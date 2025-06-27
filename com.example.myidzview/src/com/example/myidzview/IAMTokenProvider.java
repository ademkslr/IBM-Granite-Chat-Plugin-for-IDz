package com.example.myidzview;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class IAMTokenProvider {
    private final String apiKey;
    private final String region;

    public IAMTokenProvider(String apiKey, String region) {
        this.apiKey = apiKey;
        this.region = region;
    }

    public String getAccessToken() throws IOException {
    	URL url = new URL("https://iam.cloud.ibm.com/identity/token");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Accept", "application/json");

        String body = "grant_type=urn:ibm:params:oauth:grant-type:apikey&apikey=" + apiKey;
        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = reader.lines().collect(Collectors.joining());
            return new JSONObject(response).getString("access_token");
        }
    }
}