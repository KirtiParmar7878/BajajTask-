# Webhook Client – Bajaj Task

This is a Spring Boot application that *runs automatically on startup*.  
It connects to the Bajaj generateWebhook endpoint, retrieves a webhook URL and access token, determines which SQL question to solve based on regNo, and submits the solution query to the webhook.

---

## 🚀 How it works

1. On startup, the app:
   - Reads your details (name, regNo, email) from application.properties.
   - Calls generateWebhook API.
   - Gets back a webhook URL and accessToken.
   - Decides which SQL query to use:
     - **Odd last 2 digits of regNo → Question 1**
     - *Even last 2 digits → Question 2*
   - Saves the query to final-query.sql.
   - Submits the query to the returned webhook with Authorization header.

---

## 🛠 Setup & Run

### Prerequisites
- JDK 17+
- Maven
- Git

### Clone repo
```bash
git clone https://github.com/<your-username>/<your-repo>.git
cd <your-repo>
