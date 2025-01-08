package cn.panda.pandagpt.util;

import cn.panda.pandagpt.ChatGPTIntegration;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ChatGPTClient {

    private static final String MODEL = "gpt-3.5-turbo";

    public static String getResponse(String apiKey, String prompt) throws IOException {
        ChatGPTIntegration plugin = ChatGPTIntegration.getInstance();
        String apiUrl = plugin.getOpenaiApiUrl();
        CloseableHttpClient httpClient = buildHttpClient(plugin);

        try {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Authorization", "Bearer " + apiKey);

            StringEntity requestEntity = new StringEntity(buildJsonRequest(prompt), StandardCharsets.UTF_8);
            httpPost.setEntity(requestEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

                if (response.getStatusLine().getStatusCode() == 200) {
                    return parseJsonResponse(responseString);
                } else {
                    System.err.println("ChatGPT API request failed, status code: " + response.getStatusLine().getStatusCode());
                    System.err.println("Response body: " + responseString);
                    throw new IOException("ChatGPT API request failed");
                }
            }
        }finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                plugin.getLogger().severe("Error closing HTTP client: " + e.getMessage());
            }
        }
    }

    private static String buildJsonRequest(String prompt) {
        JsonObject jsonRequest = new JsonObject();
        jsonRequest.addProperty("model", MODEL);

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        List<JsonObject> messages = new ArrayList<>();
        messages.add(message);

        jsonRequest.add("messages", new Gson().toJsonTree(messages));

        return jsonRequest.toString();
    }

    private static String parseJsonResponse(String jsonResponse) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);

        if (jsonObject.has("choices")) {
            JsonObject choice = jsonObject.getAsJsonArray("choices").get(0).getAsJsonObject();
            if (choice.has("message")) {
                return choice.getAsJsonObject("message").get("content").getAsString().trim();
            }
        }
        return "No suitable response found.";
    }

    private static CloseableHttpClient buildHttpClient(ChatGPTIntegration plugin) {
        if (plugin.isProxyEnabled()) {
            HttpHost proxy = new HttpHost(plugin.getProxyHost(), plugin.getProxyPort());

            if (plugin.getProxyUsername() != null && !plugin.getProxyUsername().isEmpty()) {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                Credentials credentials = new UsernamePasswordCredentials(plugin.getProxyUsername(), plugin.getProxyPassword());
                credsProvider.setCredentials(new AuthScope(proxy.getHostName(), proxy.getPort()), credentials);
                return HttpClients.custom()
                        .setProxy(proxy)
                        .setDefaultCredentialsProvider(credsProvider)
                        .build();
            }
            return HttpClients.custom()
                    .setProxy(proxy)
                    .build();
        } else {
            return HttpClients.createDefault();
        }
    }
}
