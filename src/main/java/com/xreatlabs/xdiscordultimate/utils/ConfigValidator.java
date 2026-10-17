package com.xreatlabs.xdiscordultimate.utils;

import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Warns about common configuration mistakes on startup.
 * Never blocks the plugin, only logs.
 */
public final class ConfigValidator {

    private ConfigValidator() {
    }

    public static List<String> validate(XDiscordUltimate plugin) {
        FileConfiguration config = plugin.getConfig();
        List<String> warnings = new ArrayList<>();

        String guildId = plugin.getConfigManager().getGuildId();
        if (guildId == null || guildId.isEmpty() || !guildId.matches("\\d{17,20}")) {
            warnings.add("discord.guild-id is missing or not a valid Discord server ID");
        }

        String token = config.getString("discord.bot-token", "");
        if (token == null || token.isEmpty() || token.equalsIgnoreCase("YOUR_BOT_TOKEN")
            || token.equalsIgnoreCase("YOUR_BOT_TOKEN_HERE")) {
            warnings.add("discord.bot-token is not set - Discord features will stay offline");
        }

        if (isEnabled(config, "tickets")) {
            if (isBlank(config.getString("features.tickets.ticket-panel-channel"))) {
                warnings.add("features.tickets is enabled but ticket-panel-channel is empty");
            }
            int maxOpen = config.getInt("features.tickets.max-open-tickets", 3);
            if (maxOpen < 1) {
                warnings.add("features.tickets.max-open-tickets must be at least 1");
            }
            if (config.getInt("features.tickets.auto-close-hours", 48) < 0) {
                warnings.add("features.tickets.auto-close-hours cannot be negative");
            }
        }

        if (isEnabled(config, "chat-bridge")) {
            if (isBlank(config.getString("features.chat-bridge.chat-channel-id"))
                && isBlank(config.getString("features.chat-bridge.channel"))) {
                warnings.add("features.chat-bridge is enabled but chat-channel-id is empty");
            }
        }

        if (isEnabled(config, "verification")) {
            if (isBlank(config.getString("features.verification.verified-role"))) {
                warnings.add("features.verification is enabled but verified-role is empty");
            }
        }

        if (isEnabled(config, "tickets") || isEnabled(config, "player-stats")) {
            String language = config.getString("general.language", "en_US");
            File langFile = new File(plugin.getDataFolder(), "lang/messages_" + language + ".yml");
            if (!langFile.exists()) {
                warnings.add("language file lang/messages_" + language + ".yml is missing - falling back to en_US");
            }
        }

        int metricsId = config.getInt("advanced.metrics.bstats-id", 34781);
        if (config.getBoolean("advanced.metrics.enabled", true) && metricsId == 12345) {
            warnings.add("advanced.metrics.bstats-id is still the placeholder 12345 - replace it with your bStats plugin ID");
        }

        String embedColor = config.getString("messages.embed-color", "");
        if (!isBlank(embedColor) && !embedColor.matches("(?i)(#[0-9a-f]{3,6}|0x[0-9a-f]{6}|[0-9]{1,3},\\s*[0-9]{1,3},\\s*[0-9]{1,3}|[0-9a-f]{6})")) {
            warnings.add("messages.embed-color does not look like a valid color: " + embedColor);
        }

        return warnings;
    }

    private static boolean isEnabled(FileConfiguration config, String feature) {
        return config.getBoolean("features." + feature + ".enabled", false);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }
}
