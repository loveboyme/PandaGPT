package cn.panda.pandagpt.util;

import cn.panda.pandagpt.ChatGPTIntegration;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
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

public class GeminiClient {

    private static final String DEFAULT_GEMINI_MODEL = "gemini-1.5-flash";

    public static String getResponse(String apiKey, String prompt, String modelName) throws IOException {
        ChatGPTIntegration plugin = ChatGPTIntegration.getInstance();
        String apiUrl = String.format(plugin.getGeminiApiUrl(), modelName != null ? modelName : DEFAULT_GEMINI_MODEL, apiKey);
        CloseableHttpClient httpClient = buildHttpClient(plugin);

        try {
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Content-Type", "application/json");

            StringEntity requestEntity = new StringEntity(buildJsonRequest(prompt), StandardCharsets.UTF_8);
            httpPost.setEntity(requestEntity);

            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

                if (response.getStatusLine().getStatusCode() == 200) {
                    return parseJsonResponse(responseString);
                } else {
                    System.err.println("Gemini API request failed, status code: " + response.getStatusLine().getStatusCode());
                    System.err.println("Response body: " + responseString);
                    throw new IOException("Gemini API request failed");
                }
            }
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                plugin.getLogger().severe("Error closing HTTP client: " + e.getMessage());
            }
        }
    }

    private static String buildJsonRequest(String prompt) {
        JsonObject jsonRequest = new JsonObject();
        JsonArray contentsArray = new JsonArray();
        JsonObject contentObject = new JsonObject();
        JsonArray partsArray = new JsonArray();
        JsonObject partsObject = new JsonObject();
        partsObject.addProperty("text", prompt);
        partsArray.add(partsObject);
        contentObject.add("parts", partsArray);
        contentsArray.add(contentObject);
        jsonRequest.add("contents", contentsArray);
        return jsonRequest.toString();
    }

    private static String parseJsonResponse(String jsonResponse) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(jsonResponse, JsonObject.class);

        if (jsonObject.has("candidates")) {
            JsonArray candidates = jsonObject.getAsJsonArray("candidates");
            if (candidates.size() > 0) {
                JsonObject candidate = candidates.get(0).getAsJsonObject();
                if (candidate.has("content")) {
                    JsonObject content = candidate.getAsJsonObject("content");
                    if (content.has("parts")) {
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts.size() > 0) {
                            return parts.get(0).getAsJsonObject().get("text").getAsString().trim();
                        }
                    }
                }
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
