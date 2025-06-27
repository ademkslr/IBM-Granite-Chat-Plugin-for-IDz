# 🧠 IBM Granite Chat Plugin for IDz

This Eclipse/IDz plugin integrates IBM Granite (Watsonx) into your developer workflow. It allows you to chat with an AI assistant directly inside IBM Developer for z/OS (IDz), using a custom view.

---

## ✨ Features

- Chat with IBM's Granite models from inside IDz
- Automatically highlights assistant replies with icons
- Remembers chat history during the session
- Switch between Granite models (e.g., `granite-3-3-8b-instruct`, `guardian`, etc.)
- Manual token generation with visual status indicator
- Reset chat history anytime
- Clean, resizable UI with scroll-to-latest functionality

---

## 🔧 Requirements

- Eclipse-based IDE (tested with IDz)
- Java 11 or higher
- IBM Cloud API Key with access to Watsonx.ai
- Internet connection

---

## 🚀 Getting Started

### 1. Clone the Repository

bash
git clone https://github.com/your-username/granite-idz-plugin.git

### 2. Import in Eclipse or IDz
Go to File → Import → Existing Projects into Workspace

Select the cloned folder

### 3. Configure API Credentials
In MyIDzView.java, set your IBM Cloud credentials:


String apiKey = "YOUR_API_KEY";
String projectId = "YOUR_PROJECT_ID";
String region = "us-south"; // or "eu-de"


### 4. Run the View
Launch your Eclipse runtime application

Open the custom view:

Window → Show View → Other... → IBM Granite AI Assitant

![Screenshot 2025-06-27 221010](https://github.com/user-attachments/assets/732ca673-67cd-4570-93b6-4c9a3dd6324d)



