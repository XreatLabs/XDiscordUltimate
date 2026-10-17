package com.xreatlabs.xdiscordultimate.modules.moderation;

import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import com.xreatlabs.xdiscordultimate.modules.Module;
import com.xreatlabs.xdiscordultimate.utils.ColorUtils;
import com.xreatlabs.xdiscordultimate.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;

import java.awt.*;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.xreatlabs.xdiscordultimate.utils.SchedulerCompat;

public class ModerationModule extends Module implements Listener {
    
    private ModerationListener discordListener;
    
    // Configuration
    private String moderationLogChannel;
    private boolean syncBans;
    private boolean syncKicks;
    private boolean syncMutes;
    private boolean logCommands;
    private String mutedRole;
    
    // Command patterns
    private static final Pattern BAN_PATTERN = Pattern.compile("^/(ban|tempban|ban-ip)\\s+([\\w-]+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern KICK_PATTERN = Pattern.compile("^/kick\\s+([\\w-]+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MUTE_PATTERN = Pattern.compile("^/(mute|tempmute)\\s+([\\w-]+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern WARN_PATTERN = Pattern.compile("^/warn\\s+([\\w-]+)\\s*(.*)$", Pattern.CASE_INSENSITIVE);
    
    // Moderation history
    private final Map<UUID, List<ModerationAction>> playerHistory = new ConcurrentHashMap<>();

    // Synced mutes (Discord timeout / muted role -> Minecraft chat)
    private final Set<UUID> mutedPlayers = ConcurrentHashMap.newKeySet();
    private java.io.File mutesFile;
    
    public ModerationModule(XDiscordUltimate plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "Moderation";
    }
    
    @Override
    public String getDescription() {
        return "Moderation logging and synchronization between Minecraft and Discord";
    }
    
    @Override
    protected void onEnable() {
        loadConfiguration();
        loadMutes();
        loadModerationHistory();
        
        // Register Bukkit events
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        info("Moderation module enabled - waiting for Discord...");
    }
    
    @Override
    public void onDiscordReady() {
        // Register Discord listener
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isReady()) {
            discordListener = new ModerationListener();
            plugin.getDiscordManager().getJDA().addEventListener(discordListener);
            info("Moderation connected to Discord");
        }
    }
    
    @Override
    protected void onDisable() {
        // Unregister Discord listener
        if (discordListener != null && plugin.getDiscordManager() != null && plugin.getDiscordManager().isReady()) {
            plugin.getDiscordManager().getJDA().removeEventListener(discordListener);
        }
        
        // Save moderation history
        saveModerationHistory();
        saveMutes();
        
        info("Moderation module disabled");
    }
    
    private void loadConfiguration() {
        org.bukkit.configuration.ConfigurationSection config = getConfig();
        
        if (config != null) {
            moderationLogChannel = config.getString("log-channel", "mod-logs");
            syncBans = config.getBoolean("sync-bans", true);
            syncKicks = config.getBoolean("sync-kicks", false);
            syncMutes = config.getBoolean("sync-mutes", false);
            logCommands = config.getBoolean("auto-moderate", true);
            mutedRole = config.getString("muted-role", "Muted");
        } else {
            warning("Config section not found, using defaults");
            moderationLogChannel = "mod-logs";
            syncBans = true;
            syncKicks = false;
            syncMutes = false;
            logCommands = true;
            mutedRole = "Muted";
        }
        
        debug("Moderation log channel: " + moderationLogChannel);
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!logCommands || event.isCancelled()) return;
        
        String command = event.getMessage();
        Player player = event.getPlayer();
        
        // Check for moderation commands
        Matcher banMatcher = BAN_PATTERN.matcher(command);
        Matcher kickMatcher = KICK_PATTERN.matcher(command);
        Matcher muteMatcher = MUTE_PATTERN.matcher(command);
        Matcher warnMatcher = WARN_PATTERN.matcher(command);
        
        if (banMatcher.matches()) {
            handleBanCommand(player, banMatcher.group(1), banMatcher.group(2), banMatcher.group(3));
        } else if (kickMatcher.matches()) {
            handleKickCommand(player, kickMatcher.group(1), kickMatcher.group(2));
        } else if (muteMatcher.matches()) {
            handleMuteCommand(player, muteMatcher.group(1), muteMatcher.group(2), muteMatcher.group(3));
        } else if (warnMatcher.matches()) {
            handleWarnCommand(player, warnMatcher.group(1), warnMatcher.group(2));
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.isCancelled()) return;
        
        Player player = event.getPlayer();
        String reason = event.getReason();
        
        // Log kick
        logModerationAction(
            "KICK",
            player.getName(),
            player.getUniqueId().toString(),
            "Console",
            reason,
            null
        );
        
        // Sync to Discord if enabled
        if (syncKicks) {
            syncKickToDiscord(player, reason);
        }
    }
    
    private void handleBanCommand(Player moderator, String commandType, String targetName, String reason) {
        // Make variables final for lambda
        final String finalReason = reason;
        
        // Schedule check for next tick to see if ban was successful
        SchedulerCompat.runTaskLater(plugin, () -> {
            OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
            
            if (Bukkit.getBanList(BanList.Type.NAME).isBanned(targetName)) {
                // Parse duration if tempban
                Duration duration = null;
                String actualReason = finalReason;
                
                if (commandType.equalsIgnoreCase("tempban") && !finalReason.isEmpty()) {
                    String[] parts = finalReason.split(" ", 2);
                    duration = parseDuration(parts[0]);
                    if (duration != null && parts.length > 1) {
                        actualReason = parts[1];
                    }
                }
                
                // Log ban
                final String logReason = actualReason.isEmpty() ? "No reason provided" : actualReason;
                final Duration finalDuration = duration;
                
                logModerationAction(
                    commandType.toUpperCase(),
                    targetName,
                    target.getUniqueId() != null ? target.getUniqueId().toString() : "unknown",
                    moderator.getName(),
                    logReason,
                    finalDuration
                );
                
                // Sync to Discord if enabled
                if (syncBans) {
                    syncBanToDiscord(target, moderator.getName(), logReason, finalDuration);
                }
            }
        }, 1L);
    }
    
    private void handleKickCommand(Player moderator, String targetName, String reason) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) return;
        
        // Log will be handled by PlayerKickEvent
    }
    
    private void handleMuteCommand(Player moderator, String commandType, String targetName, String args) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) return;
        
        // Parse duration and reason
        Duration duration = null;
        String reason = args;
        
        if (commandType.equalsIgnoreCase("tempmute") && !args.isEmpty()) {
            String[] parts = args.split(" ", 2);
            duration = parseDuration(parts[0]);
            if (duration != null && parts.length > 1) {
                reason = parts[1];
            }
        }
        
        // Log mute
        logModerationAction(
            commandType.toUpperCase(),
            targetName,
            target.getUniqueId().toString(),
            moderator.getName(),
            reason.isEmpty() ? "No reason provided" : reason,
            duration
        );
        
        // Sync to Discord if enabled
        if (syncMutes) {
            syncMuteToDiscord(target, moderator.getName(), reason, duration);
        }
    }
    
    private void handleWarnCommand(Player moderator, String targetName, String reason) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) return;
        
        // Log warning
        logModerationAction(
            "WARN",
            targetName,
            target.getUniqueId().toString(),
            moderator.getName(),
            reason.isEmpty() ? "No reason provided" : reason,
            null
        );
        
        // Add to player history
        addToHistory(target.getUniqueId(), new ModerationAction(
            "WARN",
            moderator.getName(),
            reason,
            System.currentTimeMillis(),
            null
        ));
        
        // Send warning count
        List<ModerationAction> warnings = getPlayerWarnings(target.getUniqueId());
        target.sendMessage(ColorUtils.parse("<gradient:#FF5555:#AA0000>⚠ Warning!</gradient> &7Total warnings: &e" + warnings.size()));
    }
    
    private void syncBanToDiscord(OfflinePlayer player, String moderator, String reason, Duration duration) {
        plugin.getDatabaseManager().getDiscordId(player.getUniqueId())
            .thenAccept(discordId -> {
                if (discordId == null) return;

                plugin.getDiscordManager().getMainGuild().ban(
                    net.dv8tion.jda.api.entities.UserSnowflake.fromId(discordId), 0, TimeUnit.SECONDS
                ).reason("Minecraft Ban: " + reason + " (by " + moderator + ")")
                    .queue(
                        success -> debug("Synced ban to Discord for " + player.getName()),
                        error -> error("Failed to sync ban to Discord: " + error.getMessage())
                    );
            })
            .exceptionally(throwable -> {
                error("Failed to sync ban to Discord for " + player.getName(), throwable);
                return null;
            });
    }

    private void syncKickToDiscord(Player player, String reason) {
        plugin.getDatabaseManager().getDiscordId(player.getUniqueId())
            .thenAccept(discordId -> {
                if (discordId == null) return;

                Member member = plugin.getDiscordManager().getMainGuild().getMemberById(discordId);
                if (member != null) {
                    member.kick("Minecraft Kick: " + reason).queue(
                        success -> debug("Synced kick to Discord for " + player.getName()),
                        error -> debug("Failed to sync kick to Discord: " + error.getMessage())
                    );
                }
            })
            .exceptionally(throwable -> {
                error("Failed to sync kick to Discord for " + player.getName(), throwable);
                return null;
            });
    }

    private void syncMuteToDiscord(Player player, String moderator, String reason, Duration duration) {
        plugin.getDatabaseManager().getDiscordId(player.getUniqueId())
            .thenAccept(discordId -> {
                if (discordId == null) return;

                Member member = plugin.getDiscordManager().getMainGuild().getMemberById(discordId);
                if (member == null) return;

                if (duration != null) {
                    member.timeoutFor(duration).reason(
                        "Minecraft Mute: " + reason + " (by " + moderator + ")"
                    ).queue(
                        success -> debug("Synced timeout to Discord for " + player.getName()),
                        error -> error("Failed to sync timeout to Discord: " + error.getMessage())
                    );
                } else {
                    Role mutedDiscordRole = plugin.getDiscordManager().getMainGuild()
                        .getRolesByName(mutedRole, true).stream()
                        .findFirst().orElse(null);

                    if (mutedDiscordRole != null) {
                        plugin.getDiscordManager().getMainGuild()
                            .addRoleToMember(member, mutedDiscordRole)
                            .reason("Minecraft Mute: " + reason + " (by " + moderator + ")")
                            .queue(
                                success -> debug("Added muted role to " + player.getName()),
                                error -> error("Failed to add muted role: " + error.getMessage())
                            );
                    }
                }
            })
            .exceptionally(throwable -> {
                error("Failed to sync mute to Discord for " + player.getName(), throwable);
                return null;
            });
    }
    
    private void logModerationAction(String action, String targetName, String targetId,
                                   String moderator, String reason, Duration duration) {
        TextChannel logChannel = getLogChannel();
        if (logChannel == null) return;

        Color color = EmbedUtils.getModerationColor(action);

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(color)
            .setTitle("Moderation Action: " + action)
            .addField("Target", targetName + "\n`" + targetId + "`", true)
            .addField("Moderator", moderator, true)
            .addField("Reason", reason, false)
            .setTimestamp(Instant.now())
            .setFooter("Minecraft → Discord", null);

        if (duration != null) {
            embed.addField("Duration", formatDuration(duration), true);
        }

        logChannel.sendMessageEmbeds(embed.build()).queue();
    }
    
    private class ModerationListener extends ListenerAdapter {
        @Override
        public void onGuildBan(GuildBanEvent event) {
            if (!syncBans) return;
            
            User bannedUser = event.getUser();
            
            // Get Minecraft UUID (JDA callback is already async, so a sync DB lookup is safe)
            UUID minecraftUuid = plugin.getDatabaseManager().getMinecraftUuidSync(bannedUser.getId());

            if (minecraftUuid == null) return;

            // Check if already banned in Minecraft
            OfflinePlayer player = Bukkit.getOfflinePlayer(minecraftUuid);
            if (Bukkit.getBanList(BanList.Type.NAME).isBanned(player.getName())) {
                return; // Already banned, probably synced from Minecraft
            }

            // Ban in Minecraft
            SchedulerCompat.runTask(plugin, () -> {
                Bukkit.getBanList(BanList.Type.NAME).addBan(
                    player.getName(),
                    "Discord Ban Sync",
                    null,
                    "Discord"
                );
                
                // Kick if online
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null) {
                    onlinePlayer.kickPlayer("You have been banned from Discord");
                }
                
                // Log
                logDiscordModerationAction("BAN", bannedUser, "Discord ban synced to Minecraft");
            });
        }
        
        @Override
        public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
            // Could be kick or leave - check audit logs if needed
        }
        
        @Override
        public void onGuildMemberUpdateTimeOut(GuildMemberUpdateTimeOutEvent event) {
            if (!syncMutes) return;
            
            Member member = event.getMember();
            OffsetDateTime timeoutEnd = event.getNewTimeOutEnd();
            
            // Get Minecraft UUID (JDA callback is already async, so a sync DB lookup is safe)
            UUID minecraftUuid = plugin.getDatabaseManager().getMinecraftUuidSync(member.getId());

            if (minecraftUuid == null) return;

            Player player = Bukkit.getPlayer(minecraftUuid);
            if (player == null) return;
            
            if (timeoutEnd != null && timeoutEnd.isAfter(OffsetDateTime.now())) {
                // Muted
                mutePlayer(minecraftUuid);
                Duration duration = Duration.between(OffsetDateTime.now(), timeoutEnd);
                logDiscordModerationAction("TIMEOUT", member.getUser(),
                    "Discord timeout synced to Minecraft (Duration: " + formatDuration(duration) + ")");
            } else {
                // Unmuted
                unmutePlayer(minecraftUuid);
                logDiscordModerationAction("UNTIMEOUT", member.getUser(),
                    "Discord timeout removed");
            }
        }
        
        @Override
        public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
            if (!syncMutes) return;
            
            // Check if muted role was added
            event.getRoles().stream()
                .filter(role -> role.getName().equalsIgnoreCase(mutedRole))
                .findFirst()
                .ifPresent(role -> {
                    // Sync mute to Minecraft (JDA callback is already async, so a sync DB lookup is safe)
                    UUID minecraftUuid = plugin.getDatabaseManager().getMinecraftUuidSync(event.getMember().getId());

                    if (minecraftUuid != null) {
                        mutePlayer(minecraftUuid);
                        logDiscordModerationAction("MUTE", event.getUser(),
                            "Discord mute role synced to Minecraft");
                    }
                });
        }
        
        @Override
        public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
            if (!syncMutes) return;
            
            // Check if muted role was removed
            event.getRoles().stream()
                .filter(role -> role.getName().equalsIgnoreCase(mutedRole))
                .findFirst()
                .ifPresent(role -> {
                    // Sync unmute to Minecraft (JDA callback is already async, so a sync DB lookup is safe)
                    UUID minecraftUuid = plugin.getDatabaseManager().getMinecraftUuidSync(event.getMember().getId());

                    if (minecraftUuid != null) {
                        unmutePlayer(minecraftUuid);
                        logDiscordModerationAction("UNMUTE", event.getUser(),
                            "Discord unmute synced to Minecraft");
                    }
                });
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onAsyncChat(org.bukkit.event.player.AsyncPlayerChatEvent event) {
        if (!syncMutes) return;
        if (!mutedPlayers.contains(event.getPlayer().getUniqueId())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(ColorUtils.parse(
            "<gradient:#FF5555:#AA0000>Muted!</gradient> &7You are muted. Wait for staff to unmute you."));
    }

    @EventHandler
    public void onMutedPlayerJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (!syncMutes) return;
        if (!mutedPlayers.contains(event.getPlayer().getUniqueId())) return;
        event.getPlayer().sendMessage(ColorUtils.parse(
            "<gradient:#FF5555:#AA0000>Muted!</gradient> &7You are currently muted."));
    }

    private void mutePlayer(UUID uuid) {
        if (uuid == null) return;
        if (mutedPlayers.add(uuid)) {
            saveMutes();
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            SchedulerCompat.runTask(plugin, () -> online.sendMessage(
                ColorUtils.parse("<gradient:#FF5555:#AA0000>Muted!</gradient> &7You have been muted in Discord and Minecraft")));
        }
    }

    private void unmutePlayer(UUID uuid) {
        if (uuid == null) return;
        if (mutedPlayers.remove(uuid)) {
            saveMutes();
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null) {
            SchedulerCompat.runTask(plugin, () -> online.sendMessage(
                ColorUtils.parse("<gradient:#55FF55:#00AA00>Unmuted!</gradient> &7You have been unmuted")));
        }
    }

    public boolean isPlayerMuted(UUID uuid) {
        return mutedPlayers.contains(uuid);
    }

    private void loadMutes() {
        mutesFile = new java.io.File(plugin.getDataFolder(), "moderation-mutes.yml");
        if (!mutesFile.exists()) return;
        try {
            org.bukkit.configuration.file.YamlConfiguration data =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(mutesFile);
            mutedPlayers.clear();
            for (String id : data.getStringList("muted")) {
                try {
                    mutedPlayers.add(UUID.fromString(id));
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception e) {
            warning("Could not load moderation-mutes.yml: " + e.getMessage());
        }
    }

    private void saveMutes() {
        if (mutesFile == null) return;
        try {
            org.bukkit.configuration.file.YamlConfiguration data =
                new org.bukkit.configuration.file.YamlConfiguration();
            java.util.List<String> ids = new ArrayList<>();
            for (UUID uuid : mutedPlayers) {
                ids.add(uuid.toString());
            }
            data.set("muted", ids);
            data.save(mutesFile);
        } catch (Exception e) {
            warning("Could not save moderation-mutes.yml: " + e.getMessage());
        }
    }

    private void logDiscordModerationAction(String action, User user, String description) {
        TextChannel logChannel = getLogChannel();
        if (logChannel == null) return;

        Color color = EmbedUtils.getModerationColor(action);

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(color)
            .setTitle("Discord → Minecraft Sync: " + action)
            .addField("User", user.getAsMention() + "\n`" + user.getId() + "`", true)
            .addField("Action", description, false)
            .setTimestamp(Instant.now())
            .setFooter("Discord → Minecraft", user.getAvatarUrl());

        logChannel.sendMessageEmbeds(embed.build()).queue();
    }
    
    private TextChannel getLogChannel() {
        if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isReady()) {
            return null;
        }
        
        // Use global moderation channel if module config is empty
        String channelId = moderationLogChannel;
        if (channelId == null || channelId.isEmpty()) {
            channelId = plugin.getConfig().getString("discord.channels.moderation", "");
        }
        
        if (channelId == null || channelId.isEmpty()) {
            return null;
        }
        
        // Try by ID first
        if (channelId.matches("\\d+")) {
            TextChannel channel = plugin.getDiscordManager().getTextChannelById(channelId);
            if (channel != null) {
                return channel;
            }
        }
        
        net.dv8tion.jda.api.entities.Guild guild = plugin.getDiscordManager().getMainGuild();
        if (guild == null) return null;
        
        return guild.getTextChannelsByName(channelId, true)
            .stream()
            .findFirst()
            .orElse(null);
    }
    
    private Duration parseDuration(String durationStr) {
        try {
            Pattern pattern = Pattern.compile("(\\d+)([smhdwy])");
            Matcher matcher = pattern.matcher(durationStr.toLowerCase());
            
            if (!matcher.matches()) return null;
            
            long amount = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "s": return Duration.ofSeconds(amount);
                case "m": return Duration.ofMinutes(amount);
                case "h": return Duration.ofHours(amount);
                case "d": return Duration.ofDays(amount);
                case "w": return Duration.ofDays(amount * 7);
                case "y": return Duration.ofDays(amount * 365);
                default: return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
    
    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        
        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("d ");
        if (hours > 0) result.append(hours).append("h ");
        if (minutes > 0) result.append(minutes).append("m");
        
        return result.toString().trim();
    }
    
    private void addToHistory(UUID playerId, ModerationAction action) {
        playerHistory.computeIfAbsent(playerId, k -> new ArrayList<>()).add(action);
    }
    
    private List<ModerationAction> getPlayerWarnings(UUID playerId) {
        return playerHistory.getOrDefault(playerId, new ArrayList<>()).stream()
            .filter(action -> action.type.equals("WARN"))
            .collect(ArrayList::new, (list, action) -> list.add(action), ArrayList::addAll);
    }
    
    private void loadModerationHistory() {
        File historyFile = new File(plugin.getDataFolder(), "moderation-history.yml");
        if (!historyFile.exists()) return;
        try {
            org.bukkit.configuration.file.YamlConfiguration data =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(historyFile);
            playerHistory.clear();
            for (String key : data.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(key);
                    List<ModerationAction> actions = new ArrayList<>();
                    for (String entry : data.getStringList(key)) {
                        String[] parts = entry.split("\\|", -1);
                        if (parts.length < 5) continue;
                        long timestamp = 0L;
                        long durationSeconds = 0L;
                        try {
                            timestamp = Long.parseLong(parts[3]);
                        } catch (NumberFormatException ignored) {
                        }
                        try {
                            durationSeconds = Long.parseLong(parts[4]);
                        } catch (NumberFormatException ignored) {
                        }
                        actions.add(new ModerationAction(parts[0], parts[1], parts[2], timestamp,
                            durationSeconds > 0 ? Duration.ofSeconds(durationSeconds) : null));
                    }
                    playerHistory.put(playerId, actions);
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception e) {
            warning("Could not load moderation-history.yml: " + e.getMessage());
        }
    }

    private void saveModerationHistory() {
        try {
            org.bukkit.configuration.file.YamlConfiguration data =
                new org.bukkit.configuration.file.YamlConfiguration();
            for (Map.Entry<UUID, List<ModerationAction>> entry : playerHistory.entrySet()) {
                List<String> serialized = new ArrayList<>();
                for (ModerationAction action : entry.getValue()) {
                    serialized.add(action.type + "|"
                        + (action.moderator == null ? "" : action.moderator.replace("|", "/")) + "|"
                        + (action.reason == null ? "" : action.reason.replace("|", "/")) + "|"
                        + action.timestamp + "|"
                        + (action.duration != null ? action.duration.getSeconds() : 0L));
                }
                data.set(entry.getKey().toString(), serialized);
            }
            File historyFile = new File(plugin.getDataFolder(), "moderation-history.yml");
            data.save(historyFile);
        } catch (Exception e) {
            warning("Could not save moderation-history.yml: " + e.getMessage());
        }
    }

    /**
     * Log an auto-moderation action (chat filter, spam guard) to the configured Discord channel.
     */
    public void logAutoModerationAction(Player player, String action, String message) {
        String channelName = getConfig().getString("log-channel", "mod-logs");
        if (channelName == null || channelName.isEmpty()) return;
        com.xreatlabs.xdiscordultimate.utils.SchedulerCompat.runTask(plugin, () -> {
            try {
                net.dv8tion.jda.api.entities.Guild guild = plugin.getDiscordManager().getMainGuild();
                if (guild == null) return;
                net.dv8tion.jda.api.entities.channel.concrete.TextChannel channel =
                    guild.getTextChannelsByName(channelName, true).stream().findFirst().orElse(null);
                if (channel == null) {
                    channel = guild.getTextChannelById(channelName);
                }
                if (channel == null) return;
                net.dv8tion.jda.api.EmbedBuilder embed = new net.dv8tion.jda.api.EmbedBuilder()
                    .setTitle("Auto-Moderation: " + action)
                    .setDescription(message)
                    .setColor(com.xreatlabs.xdiscordultimate.utils.EmbedUtils.WARNING)
                    .setFooter(player.getName())
                    .setTimestamp(java.time.Instant.now());
                channel.sendMessageEmbeds(embed.build()).queue();
            } catch (Throwable ignored) {
            }
        });
    }
    
    /**
     * Get moderation history for a player
     */
    public List<ModerationAction> getPlayerHistory(UUID playerId) {
        return new ArrayList<>(playerHistory.getOrDefault(playerId, new ArrayList<>()));
    }
    
    /**
     * Check if a player is muted
     */
    public boolean isPlayerMuted(Player player) {
        if (mutedPlayers.contains(player.getUniqueId())) {
            return true;
        }

        // Synchronous lookup: this is called from sync contexts (commands/placeholders) that must
        // return a value immediately. The database lookup is bounded and does not use .join().
        String discordId = plugin.getDatabaseManager().getDiscordIdSync(player.getUniqueId());

        if (discordId == null) return false;

        Member member = plugin.getDiscordManager().getMainGuild().getMemberById(discordId);
        if (member == null) return false;

        // Check timeout
        if (member.isTimedOut()) return true;

        // Check muted role
        return member.getRoles().stream()
            .anyMatch(role -> role.getName().equalsIgnoreCase(mutedRole));
    }
    
    /**
     * Moderation action data class
     */
    public static class ModerationAction {
        public final String type;
        public final String moderator;
        public final String reason;
        public final long timestamp;
        public final Duration duration;
        
        public ModerationAction(String type, String moderator, String reason, long timestamp, Duration duration) {
            this.type = type;
            this.moderator = moderator;
            this.reason = reason;
            this.timestamp = timestamp;
            this.duration = duration;
        }
    }
}