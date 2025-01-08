package cn.panda.pandagpt.util;

import cn.panda.pandagpt.ChatGPTIntegration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PandaLogger {

    private final ChatGPTIntegration plugin;
    private final File logFile;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public PandaLogger(ChatGPTIntegration plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        logFile = new File(dataFolder, "panda.log");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("无法创建 panda.log 文件！");
                e.printStackTrace();
            }
        }
    }

    public void logConversation(String playerName, String userMessage, String pandaResponse) {
        log("[CONVERSATION] [" + playerName + "] User: " + userMessage);
        log("[CONVERSATION] [" + playerName + "] Panda: " + pandaResponse);
    }

    public void logAction(String playerName, String action) {
        log("[ACTION] [" + playerName + "] " + action);
    }

    public void info(String message) {
        plugin.getLogger().info("[PandaGPT] " + message);
        log("[INFO] " + message);
    }

    public void warning(String message) {
        plugin.getLogger().warning("[PandaGPT] " + message);
        log("[WARNING] " + message);
    }

    public void severe(String message) {
        plugin.getLogger().severe("[PandaGPT] " + message);
        log("[SEVERE] " + message);
    }

    public void logCommandDetection(String playerName, String aiResponse, String extractedCommand) {
        log("[COMMAND_DETECTED] [" + playerName + "] AI Response: " + aiResponse + ", Extracted Command: " + extractedCommand);
    }

    public void logCommandExecutionAttempt(String playerName, String command) {
        log("[COMMAND_ATTEMPT] [" + playerName + "] Attempting to execute command: " + command);
    }

    public void logCommandExecutionSuccess(String playerName, String command) {
        log("[COMMAND_SUCCESS] [" + playerName + "] Successfully executed command: " + command);
    }

    public void logCommandExecutionFailure(String playerName, String command, String reason) {
        log("[COMMAND_FAILURE] [" + playerName + "] Failed to execute command: " + command + ", Reason: " + reason);
    }

    private void log(String message) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.write(dateFormat.format(new Date()) + " " + message);
            writer.newLine();
        } catch (IOException e) {
            plugin.getLogger().severe("写入 panda.log 文件时发生错误！");
            e.printStackTrace();
        }
    }
}