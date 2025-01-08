package cn.panda.pandagpt;

import cn.panda.pandagpt.awareness.CommandAwareness;
import org.bukkit.plugin.java.JavaPlugin;
import cn.panda.pandagpt.commands.PandaCommand;
import cn.panda.pandagpt.listeners.ChatListener;
import cn.panda.pandagpt.util.PandaLogger;
import org.bukkit.entity.Player; // Ensure Player is imported

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatGPTIntegration extends JavaPlugin {

    private static ChatGPTIntegration instance;
    private String apiKey;
    private String geminiApiKey;
    private long cooldownTime;
    private String defaultProvider;
    private String openaiModel;
    private String geminiModel;
    private String openaiApiUrl;
    private String geminiApiUrl;
    private boolean proxyEnabled;
    private String proxyHost;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private boolean environmentAwarenessEnabled;
    private boolean commandAwarenessEnabled;
    private String personalityTemplate; // Modified: Use personalityTemplate to store the original template
    private final Map<String, Long> lastRequestTime = new ConcurrentHashMap<>();
    private final Map<String, ConversationHistory> conversationHistory = new ConcurrentHashMap<>();
    private PandaLogger pandaLogger;
    private CommandAwareness commandAwareness;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        apiKey = getConfig().getString("openai.api_key");
        geminiApiKey = getConfig().getString("gemini.api_key");
        cooldownTime = getConfig().getLong("panda.cooldown", 5000);
        defaultProvider = getConfig().getString("panda.model_selection.provider", "gemini").toLowerCase();
        openaiModel = getConfig().getString("panda.model_selection.openai.model", "gpt-3.5-turbo");
        geminiModel = getConfig().getString("panda.model_selection.gemini.model", "gemini-1.5-flash");
        openaiApiUrl = getConfig().getString("openai.api_url", "https://api.openai.com/v1/chat/completions");
        geminiApiUrl = getConfig().getString("gemini.api_url", "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s");
        proxyEnabled = getConfig().getBoolean("panda.proxy.enabled", false);
        proxyHost = getConfig().getString("panda.proxy.host", "");
        proxyPort = getConfig().getInt("panda.proxy.port", 80);
        proxyUsername = getConfig().getString("panda.proxy.username", null);
        proxyPassword = getConfig().getString("panda.proxy.password", null);
        environmentAwarenessEnabled = getConfig().getBoolean("panda.environment_awareness.enabled", true);
        commandAwarenessEnabled = getConfig().getBoolean("panda.command_awareness.enabled", true);
        personalityTemplate = getConfig().getString("panda.personality", "You are a friendly Minecraft assistant, eager to help."); // Load the original template

        pandaLogger = new PandaLogger(this);
        pandaLogger.info("Panda Logger has started.");

        commandAwareness = new CommandAwareness(this);

        boolean openaiEnabled = true;
        if (apiKey == null || apiKey.isEmpty()) {
            getLogger().severe("OpenAI API key not configured! ChatGPT functionality will be disabled.");
            if (defaultProvider.equals("openai")) {
                getLogger().warning("The default model provider is set to OpenAI, but the API key is not configured. Please check your configuration.");
                openaiEnabled = false;
            }
        }

        boolean geminiEnabled = true;
        if (geminiApiKey == null || geminiApiKey.isEmpty()) {
            getLogger().severe("Gemini API key not configured! Gemini functionality will be disabled.");
            if (defaultProvider.equals("gemini")) {
                getLogger().warning("The default model provider is set to Gemini, but the API key is not configured. Please check your configuration.");
                geminiEnabled = false;
            }
        }

        if (!openaiEnabled && !geminiEnabled) {
            getLogger().severe("No available AI model provider configuration. Plugin functionality will be limited.");
        }

        // Register commands
        getCommand("panda").setExecutor(new PandaCommand(this));

        // Register event listeners
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("ChatGPT Integration plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        pandaLogger.info("Panda Logger has stopped.");
        getLogger().info("ChatGPT Integration plugin has been disabled!");
    }

    public static ChatGPTIntegration getInstance() {
        return instance;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public long getCooldownTime() {
        return cooldownTime;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public String getOpenaiModel() {
        return openaiModel;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public Map<String, Long> getLastRequestTime() {
        return lastRequestTime;
    }

    public ConversationHistory getConversationHistory(String playerName) {
        return conversationHistory.computeIfAbsent(playerName, k -> new ConversationHistory(getConfig().getInt("panda.history_size", 5)));
    }

    public PandaLogger getPandaLogger() {
        return pandaLogger;
    }

    public String getOpenaiApiUrl() {
        return openaiApiUrl;
    }

    public String getGeminiApiUrl() {
        return geminiApiUrl;
    }

    public boolean isProxyEnabled() {
        return proxyEnabled;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public boolean isEnvironmentAwarenessEnabled() {
        return environmentAwarenessEnabled;
    }

    public boolean isCommandAwarenessEnabled() {
        return commandAwarenessEnabled;
    }

    public CommandAwareness getCommandAwareness() {
        return commandAwareness;
    }

    // New method: Get processed Personality
    public String getPersonality(Player player) {
        String personality = personalityTemplate;
        if (environmentAwarenessEnabled) {
            String playerContext = cn.panda.pandagpt.util.PlayerContext.getContext(player);
            personality = personality.replace("{{player_context}}", playerContext);
        } else {
            personality = personality.replace("{{player_context}}", ""); // If disabled, remove the placeholder
        }
        if (commandAwarenessEnabled) {
            String serverCommandContext = commandAwareness.getContext(player);
            personality = personality.replace("{{server_command_context}}", serverCommandContext);
        } else {
            personality = personality.replace("{{server_command_context}}", "");
        }
        return personality;
    }

    private String buildFullPrompt(Player player, ConversationHistory history, String currentPrompt) {
        String personality = getPersonality(player);
        StringBuilder fullPromptBuilder = new StringBuilder(personality + "\n\n");

        for (ChatMessage chatMessage : history.getMessages()) {
            fullPromptBuilder.append(chatMessage.getRole().equals("user") ? "User says: " : "Panda says: ").append(chatMessage.getContent()).append("\n");
        }
        fullPromptBuilder.append("The user's question is: ").append(currentPrompt);
        return fullPromptBuilder.toString();
    }

    public static class ConversationHistory {
        private final int maxSize;
        private final java.util.LinkedList<ChatMessage> messages = new java.util.LinkedList<>();

        public ConversationHistory(int maxSize) {
            this.maxSize = maxSize;
        }

        public void addMessage(String role, String content) {
            messages.add(new ChatMessage(role, content));
            if (messages.size() > maxSize) {
                messages.removeFirst();
            }
        }

        public java.util.List<ChatMessage> getMessages() {
            return messages;
        }
    }

    public static class ChatMessage {
        private final String role;
        private final String content;

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}
