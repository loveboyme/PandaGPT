package cn.panda.pandagpt.listeners;

import cn.panda.pandagpt.ChatGPTIntegration;
import cn.panda.pandagpt.util.ChatGPTClient;
import cn.panda.pandagpt.util.FormatUtil;
import cn.panda.pandagpt.util.GeminiClient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final ChatGPTIntegration plugin;

    public ChatListener(ChatGPTIntegration plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        String playerName = player.getName();

        String triggerWord = plugin.getConfig().getString("panda.trigger_word", "panda");
        Pattern pattern = Pattern.compile("\\b" + Pattern.quote(triggerWord) + "\\b", Pattern.CASE_INSENSITIVE);

        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            String prompt = message.substring(matcher.end()).trim();

            if (plugin.getLastRequestTime().containsKey(player.getName()) &&
                    System.currentTimeMillis() - plugin.getLastRequestTime().get(player.getName()) < plugin.getCooldownTime()) {
                player.sendMessage(ChatColor.YELLOW + "[Panda] Please wait before asking again.");
                return;
            }

            plugin.getLastRequestTime().put(playerName, System.currentTimeMillis());
            plugin.getPandaLogger().logAction(playerName, "Mentioned Panda in chat: " + prompt);
            event.setCancelled(plugin.getConfig().getBoolean("panda.cancel_chat_message", true));
            askPanda(player, prompt);
        }
    }

    private void askPanda(final Player player, final String prompt) {
        player.sendMessage(ChatColor.AQUA + "[Panda] " + ChatColor.GRAY + "Thinking... (Using " + plugin.getDefaultProvider() + ")");

        final ChatGPTIntegration.ConversationHistory history = plugin.getConversationHistory(player.getName());

        new Thread(() -> {
            try {
                String fullPrompt = buildFullPrompt(player, history, prompt);

                String response;
                String defaultProvider = plugin.getDefaultProvider();
                if (defaultProvider.equalsIgnoreCase("openai")) {
                    response = ChatGPTClient.getResponse(plugin.getApiKey(), fullPrompt);
                } else if (defaultProvider.equalsIgnoreCase("gemini")) {
                    response = GeminiClient.getResponse(plugin.getGeminiApiKey(), fullPrompt, plugin.getGeminiModel());
                } else {
                    response = ChatColor.RED + "Unknown model provider: " + defaultProvider;
                }

                String formattedResponse = FormatUtil.formatChatMessage(response);

                plugin.getServer().getPlayer(player.getName()).sendMessage(ChatColor.AQUA + "[Panda] " + ChatColor.WHITE + formattedResponse);
                history.addMessage("user", prompt);
                history.addMessage("assistant", response);

                plugin.getPandaLogger().logConversation(player.getName(), prompt, response);

                // Check for commands and execute if present
                if (plugin.getCommandAwareness().containsCommand(response)) {
                    String command = plugin.getCommandAwareness().extractCommand(response);
                    if (command != null) {
                        plugin.getPandaLogger().logCommandDetection(player.getName(), response, command);
                        final String finalCommand = command;
                        Bukkit.getServer().getScheduler().runTask(plugin, () -> {
                            plugin.getPandaLogger().logCommandExecutionAttempt(player.getName(), finalCommand);
                            boolean success = player.performCommand(finalCommand.substring(1));
                            if (success) {
                                plugin.getPandaLogger().logCommandExecutionSuccess(player.getName(), finalCommand);
                            } else {
                                plugin.getPandaLogger().logCommandExecutionFailure(player.getName(), finalCommand, "Insufficient permissions or command error");
                                player.sendMessage(ChatColor.RED + "[Panda] Command execution failed. Please check permissions or command syntax.");
                            }
                        });
                    } else {
                        plugin.getPandaLogger().warning("The AI's response contains a potential command, but a valid command could not be extracted.");
                    }
                }

            } catch (Exception e) {
                plugin.getServer().getPlayer(player.getName()).sendMessage(ChatColor.RED + "An error occurred while communicating with Panda.");
                plugin.getPandaLogger().severe("Exception while processing chat message: " + e.getMessage());
            }
        }).start();
    }

    private String buildFullPrompt(Player player, ChatGPTIntegration.ConversationHistory history, String currentPrompt) {
        String personality = plugin.getPersonality(player);
        StringBuilder fullPromptBuilder = new StringBuilder(personality + "\n\n");

        for (ChatGPTIntegration.ChatMessage chatMessage : history.getMessages()) {
            fullPromptBuilder.append(chatMessage.getRole().equals("user") ? "User: " : "Panda: ").append(chatMessage.getContent()).append("\n");
        }
        fullPromptBuilder.append("The user's question is: ").append(currentPrompt);
        return fullPromptBuilder.toString();
    }
}
