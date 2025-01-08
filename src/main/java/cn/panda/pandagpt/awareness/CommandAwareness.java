package cn.panda.pandagpt.awareness;

import cn.panda.pandagpt.ChatGPTIntegration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandAwareness {

    private final ChatGPTIntegration plugin;
    // Regular expression to match patterns starting with a slash followed by one or more word characters
    private static final Pattern COMMAND_PATTERN = Pattern.compile("/([a-zA-Z0-9_]+(?: [a-zA-Z0-9_]+)*)");

    public CommandAwareness(ChatGPTIntegration plugin) {
        this.plugin = plugin;
    }

    public String getContext(Player player) {
        if (!plugin.isCommandAwarenessEnabled()) {
            return "";
        }

        StringBuilder contextBuilder = new StringBuilder("Server Information:\n");
        contextBuilder.append("  - Minecraft Version: ").append(Bukkit.getVersion()).append("\n");
        contextBuilder.append("  - Bukkit Version: ").append(Bukkit.getBukkitVersion()).append("\n");
        contextBuilder.append("  - Installed Plugins (Name and Version):").append("\n");
        for (Plugin pl : Bukkit.getPluginManager().getPlugins()) {
            contextBuilder.append("    - ").append(pl.getName()).append(" v").append(pl.getDescription().getVersion()).append("\n");
        }
        return contextBuilder.toString();
    }

    public boolean containsCommand(String text) {
        Matcher matcher = COMMAND_PATTERN.matcher(text);
        return matcher.find();
    }

    // Modify extractCommand method to extract the found command
    public String extractCommand(String text) {
        Matcher matcher = COMMAND_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(); // Return the entire matched command string
        }
        return null;
    }
}
