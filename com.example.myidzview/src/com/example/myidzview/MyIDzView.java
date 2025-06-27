package com.example.myidzview;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.part.ViewPart;

import java.io.InputStream;

public class MyIDzView extends ViewPart {

    public static final String ID = "com.example.myidzview.MyIDzView";

    private Text inputText;
    private GraniteChatClient granite;
    private Composite chatContainer;
    private Font largerFont;
    private ScrolledComposite scrolled;

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(1, false));
        largerFont = new Font(parent.getDisplay(), "Segoe UI", 11, SWT.NONE);
        parent.addDisposeListener(e -> largerFont.dispose());

        String[] modelChoices = {
            "ibm/granite-3-3-8b-instruct",
            "ibm/granite-vision-3-2-2b",
            "ibm/granite-guardian-3-2b",
            "ibm/granite-vision-3-2-2b"
        };

        // Chat scroll area
        scrolled = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
        scrolled.setExpandHorizontal(true);
        scrolled.setExpandVertical(true);
        scrolled.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        chatContainer = new Composite(scrolled, SWT.NONE);
        chatContainer.setLayout(new GridLayout(1, false));
        scrolled.setContent(chatContainer);
        scrolled.addListener(SWT.MouseWheel, event -> {
            chatContainer.notifyListeners(SWT.MouseWheel, event);
        });

        // Input area
        Composite inputArea = new Composite(parent, SWT.NONE);
        inputArea.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        inputArea.setLayout(new GridLayout(2, false));

        inputText = new Text(inputArea, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData inputLayout = new GridData(SWT.FILL, SWT.CENTER, true, false);
        inputLayout.heightHint = 60;
        inputText.setLayoutData(inputLayout);
        inputText.setFont(largerFont);

        Button sendButton = new Button(inputArea, SWT.PUSH);
        sendButton.setText("Send");

        Button resetButton = new Button(inputArea, SWT.PUSH);
        resetButton.setText("New Chat");
        resetButton.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        resetButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                granite.clearHistory();
                for (Control child : chatContainer.getChildren()) {
                    child.dispose();
                }
                chatContainer.layout(true, true);
                addMessage("System", "🔁 Chat history cleared", null);
            }
        });

        // Token status display
        Label tokenStatusLabel = new Label(parent, SWT.NONE);
        tokenStatusLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        tokenStatusLabel.setText("Token is loading...");

        Label modelLabel = new Label(parent, SWT.NONE);
        modelLabel.setText("Granite Model:");

        Combo modelCombo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        modelCombo.setItems(modelChoices);
        modelCombo.select(0);

        // Initialize Granite client
        String apiKey = "YOUR_API_KEY";  // Replace with your API key
        String projectId = "YOUR_PROJECT_ID"; // Replace with your project ID
        String modelId = "ibm/granite-3-3-8b-instruct";
        String region = "us-south";

        granite = new GraniteChatClient(apiKey, projectId, modelId, region);

        granite.setTokenStatusListener((status, ok) -> Display.getDefault().asyncExec(() -> {
            tokenStatusLabel.setText(status);
            tokenStatusLabel.setForeground(ok
                ? parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN)
                : parent.getDisplay().getSystemColor(SWT.COLOR_RED));
        }));

        // Generate Token Button
        Button generateTokenButton = new Button(parent, SWT.PUSH);
        generateTokenButton.setText("Generate Token");
        generateTokenButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        generateTokenButton.addListener(SWT.Selection, e -> granite.refreshToken());

        // Send message logic
        Runnable sendMessage = () -> {
            if (granite.getToken() == null || granite.getToken().isEmpty()) {
                MessageBox box = new MessageBox(parent.getShell(), SWT.ICON_WARNING | SWT.OK);
                box.setText("No Active Token");
                box.setMessage("Please generate a token before using IBM Granite.");
                box.open();
                return;
            }
            String userMessage = inputText.getText().trim();
            if (!userMessage.isEmpty()) {
                inputText.setText("");
                addMessage("You", userMessage, null);
                granite.setModelId(modelCombo.getText());
                String response = granite.sendMessage(userMessage);
                addMessage("Granite", response, "/icons/robot.png");
                chatContainer.layout(true, true);
            }
        };

        sendButton.addListener(SWT.Selection, e -> sendMessage.run());
        inputText.addListener(SWT.KeyDown, e -> {
            if (e.keyCode == SWT.CR || e.keyCode == SWT.LF) {
                e.doit = false;
                sendMessage.run();
            }
        });
    }

    private void addMessage(String sender, String text, String iconPath) {
        Composite row = new Composite(chatContainer, SWT.NONE);
        row.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        row.setLayout(new GridLayout(2, false));

        Label iconLabel = new Label(row, SWT.NONE);
        if (iconPath != null) {
            InputStream iconStream = getClass().getResourceAsStream(iconPath);
            if (iconStream != null) {
                Image icon = new Image(chatContainer.getDisplay(), iconStream);
                iconLabel.setImage(icon);
                chatContainer.addDisposeListener(e -> icon.dispose());
            }
        }

        Text message = new Text(row, SWT.READ_ONLY | SWT.WRAP | SWT.MULTI);
        message.setText(sender + ":\n" + text);
        message.setFont(largerFont);
        GridData gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.widthHint = 500;
        message.setLayoutData(gd);

        // Scroll directly to Granite's response
        if ("Granite".equals(sender)) {
            Display.getDefault().asyncExec(() -> {
                chatContainer.layout(true, true);
                scrolled.setMinSize(chatContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
                scrolled.showControl(message);
            });
        }
    }

    @Override
    public void setFocus() {
        inputText.setFocus();
    }
}
