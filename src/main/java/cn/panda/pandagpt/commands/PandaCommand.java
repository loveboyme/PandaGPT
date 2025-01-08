package cn.panda.pandagpt.commands;

import cn.panda.pandagpt.ChatGPTIntegration;
import cn.panda.pandagpt.util.ChatGPTClient;
import cn.panda.pandagpt.util.FormatUtil;
import cn.panda.pandagpt.util.GeminiClient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PandaCommand implements CommandExecutor {

    private final ChatGPTIntegration plugin;

    public PandaCommand(ChatGPTIntegration plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by players.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /panda <your question>");
            return true;
        }

        Player player = (Player) sender;
        String prompt = String.join(" ", args);

        plugin.getPandaLogger().logAction(player.getName(), "Used /panda command: " + prompt);
        askPanda(player, prompt);
        return true;
    }

    private void askPanda(final Player player, final String prompt) {
        if (plugin.getLastRequestTime().containsKey(player.getName()) &&
                System.currentTimeMillis() - plugin.getLastRequestTime().get(player.getName()) < plugin.getCooldownTime()) {
            player.sendMessage(ChatColor.YELLOW + "[Panda] Please wait before asking again.");
            return;
        }

        plugin.getLastRequestTime().put(player.getName(), System.currentTimeMillis());
        String defaultProvider = plugin.getDefaultProvider();
        player.sendMessage(ChatColor.AQUA + "[Panda] " + ChatColor.GRAY + "Thinking... (Using " + defaultProvider + ")");

        final ChatGPTIntegration.ConversationHistory history = plugin.getConversationHistory(player.getName());

        new Thread(() -> {
            try {
                String fullPrompt = buildFullPrompt(player, history, prompt);

                String response;
                if (defaultProvider.equalsIgnoreCase("openai")) {
                    response = ChatGPTClient.getResponse(plugin.getApiKey(), fullPrompt);
                } else if (defaultProvider.equalsIgnoreCase("gemini")) {
                    response = GeminiClient.getResponse(plugin.getGeminiApiKey(), fullPrompt, plugin.getGeminiModel());
                } else {
                    response = ChatColor.RED + "Unknown model provider: " + defaultProvider;
                }

                String formattedResponse = FormatUtil.formatChatMessage(response);

                player.sendMessage(ChatColor.AQUA + "[Panda] " + ChatColor.WHITE + formattedResponse);
                history.addMessage("user", prompt);
                history.addMessage("assistant", response);

                plugin.getPandaLogger().logConversation(player.getName(), prompt, response);

                // Check if a command is included in the response and execute it
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
                player.sendMessage(ChatColor.RED + "An error occurred while communicating with Panda.");
                plugin.getPandaLogger().severe("Exception while processing command request: " + e.getMessage());
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
