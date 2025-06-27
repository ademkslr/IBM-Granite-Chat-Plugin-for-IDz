package com.example.myidzview;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class GraniteChatClient {

    public interface TokenStatusListener {
        void onStatusChange(String statusText, boolean ok);
    }

    private String token;
    private final String apiKey;
    private final String region;
    private final String projectId;
    private String modelId;
    private final IAMTokenProvider tokenProvider;
    private TokenStatusListener statusListener;
    private final JSONArray messageHistory = new JSONArray();

    public GraniteChatClient(String apiKey, String projectId, String modelId, String region) {
        this.apiKey = apiKey;
        this.region = region;
        this.projectId = projectId;
        this.modelId = modelId;
        this.tokenProvider = new IAMTokenProvider(apiKey, region);
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public void setTokenStatusListener(TokenStatusListener listener) {
        this.statusListener = listener;
    }

    private void notifyStatus(String text, boolean success) {
        if (statusListener != null) {
            statusListener.onStatusChange(text, success);
        }
    }

    public void refreshToken() {
        this.token = fetchToken();
    }

    public String getToken() {
        return this.token;
    }

    private String fetchToken() {
        try {
            notifyStatus("Updating token...", true);
            String newToken = tokenProvider.getAccessToken();
            notifyStatus("🟢 Token is valid", true);
            return newToken;
        } catch (IOException e) {
            notifyStatus("Token error: " + e.getMessage(), false);
            e.printStackTrace();
            return null;
        }
    }

    public String sendMessage(String userMessage) {
        try {
            return sendMessageInternal(userMessage);
        } catch (IOException e) {
            e.printStackTrace();
            return "Granite Error: " + e.getMessage();
        }
    }

    private String sendMessageInternal(String userMessage) throws IOException {
        HttpURLConnection connection = setupConnection();

        // Add user message to conversation history
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messageHistory.put(userMsg);

        // Build the request
        JSONObject requestBody = new JSONObject();
        requestBody.put("messages", messageHistory);
        requestBody.put("project_id", projectId);
        requestBody.put("model_id", modelId);
        requestBody.put("temperature", 0);
        requestBody.put("max_tokens", 2000);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = connection.getResponseCode();

        if (responseCode == 401) {
            String response = readStream(connection.getErrorStream());
            if (response.contains("authentication_token_expired")) {
                this.token = fetchToken();
                return sendMessageInternal(userMessage);
            } else {
                return "Granite Error (401):\n" + response;
            }
        }

        InputStream stream = responseCode < 400
                ? connection.getInputStream()
                : connection.getErrorStream();

        String json = readStream(stream);
        String responseText = extractTextFromResponse(json);

        // Add assistant response to conversation history
        JSONObject assistantMsg = new JSONObject();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", responseText);
        messageHistory.put(assistantMsg);

        System.out.println(responseText);
        return responseText;
    }

    private HttpURLConnection setupConnection() throws IOException {
        URL url = new URL("https://" + region + ".ml.cloud.ibm.com/ml/v1/text/chat?version=2023-05-29");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + token);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);
        return connection;
    }

    private String readStream(InputStream stream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    private String extractTextFromResponse(String json) {
        JSONObject obj = new JSONObject(json);
        JSONArray choices = obj.getJSONArray("choices");
        if (choices.length() > 0) {
            System.out.println(json);
            return choices.getJSONObject(0).getJSONObject("message").getString("content");
        }
        return "(No response found)";
    }

    // Clear chat history
    public void clearHistory() {
        messageHistory.clear();
    }
}
