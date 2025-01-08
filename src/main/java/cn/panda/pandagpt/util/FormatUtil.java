package cn.panda.pandagpt.util;

import org.bukkit.ChatColor;

public class FormatUtil {

    public static String formatChatMessage(String message) {
        // 标题
        message = message.replaceAll("^# (.*)$", ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "[H1] $1" + ChatColor.RESET);
        message = message.replaceAll("^## (.*)$", ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "[H2] $1" + ChatColor.RESET);
        message = message.replaceAll("^### (.*)$", ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "[H3] $1" + ChatColor.RESET);

        // 粗体
        message = message.replaceAll("\\*\\*(.*?)\\*\\*", ChatColor.BOLD + "$1" + ChatColor.RESET);

        // 斜体
        message = message.replaceAll("\\*(.*?)\\*", ChatColor.ITALIC + "$1" + ChatColor.RESET);
        message = message.replaceAll("_(.*?)_", ChatColor.ITALIC + "$1" + ChatColor.RESET);

        // 删除线 (Minecraft 不原生支持，用特殊文本标识)
        message = message.replaceAll("~~(.*?)~~", "<STRIKETHROUGH>$1</STRIKETHROUGH>");

        // 无序列表
        message = message.replaceAll("^- (.*)$", ChatColor.GRAY + "• $1" + ChatColor.RESET);
        message = message.replaceAll("^\\* (.*)$", ChatColor.GRAY + "• $1" + ChatColor.RESET);

        // 有序列表
        message = message.replaceAll("^\\d+\\. (.*)$", ChatColor.GRAY + "$0" + ChatColor.RESET); // 保留原始的 "数字. "

        // 行内代码
        message = message.replaceAll("`(.*?)`", ChatColor.GRAY + "[" + ChatColor.WHITE + "$1" + ChatColor.GRAY + "]" + ChatColor.RESET);

        return message;
    }
}