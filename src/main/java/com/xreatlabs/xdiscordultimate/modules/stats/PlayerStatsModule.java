package com.xreatlabs.xdiscordultimate.modules.stats;

import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import com.xreatlabs.xdiscordultimate.modules.Module;
import com.xreatlabs.xdiscordultimate.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.xreatlabs.xdiscordultimate.utils.SchedulerCompat;

public class PlayerStatsModule extends Module implements Listener {

    // Order here controls display order, ids match the config and lang keys
    public static final List<String> STAT_ORDER = Collections.unmodifiableList(List.of(
        "kills", "deaths", "kd-ratio", "mob-kills", "playtime",
        "blocks-broken", "blocks-placed", "damage-dealt", "damage-taken",
        "distance-walked", "jumps", "fish-caught", "items-crafted", "advancements"
    ));

    private static final Map<String, String> DEFAULT_LABELS = Map.ofEntries(
        Map.entry("kills", "⚔️ Kills"),
        Map.entry("deaths", "💀 Deaths"),
        Map.entry("kd-ratio", "📈 K/D Ratio"),
        Map.entry("mob-kills", "🐾 Mob Kills"),
        Map.entry("playtime", "⏱️ Playtime"),
        Map.entry("blocks-broken", "⛏️ Blocks Broken"),
        Map.entry("blocks-placed", "🧱 Blocks Placed"),
        Map.entry("damage-dealt", "🗡️ Damage Dealt"),
        Map.entry("damage-taken", "🛡️ Damage Taken"),
        Map.entry("distance-walked", "🚶 Distance Walked"),
        Map.entry("jumps", "🦘 Jumps"),
        Map.entry("fish-caught", "🎣 Fish Caught"),
        Map.entry("items-crafted", "🔨 Items Crafted"),
        Map.entry("advancements", "🏆 Advancements")
    );

    private static final long CACHE_TTL_MS = 5_000L;

    private final Map<String, Boolean> statEnabled = new LinkedHashMap<>();
    private final Map<String, String> statLabels = new LinkedHashMap<>();
    private final Map<UUID, CachedStats> statsCache = new java.util.concurrent.ConcurrentHashMap<>();

    private volatile Map<String, List<TopEntry>> topLists = new HashMap<>();
    private volatile Map<UUID, Map<String, Integer>> playerRanks = new HashMap<>();
    private org.bukkit.scheduler.BukkitTask topTask;
    private File snapshotsFile;
    private org.bukkit.configuration.file.YamlConfiguration snapshots;

    private boolean usePlaceholderApi;
    private boolean thousandsSeparator;
    private int kdDecimals;
    private String playtimeFormat;
    private boolean showRank;
    private String rankFormat;
    private int topCacheMinutes;
    private boolean snapshotsAutoReset;
    private boolean tabEnabled;
    private String tabHeader;
    private String tabFooter;
    private int tabUpdateSeconds;
    private org.bukkit.scheduler.BukkitTask tabTask;

    private String embedTitle;
    private String embedColor;
    private String embedFooter;
    private String embedThumbnail;
    private boolean embedInline;
    private boolean embedTimestamp;

    public PlayerStatsModule(XDiscordUltimate plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Player Stats";
    }

    @Override
    public String getDescription() {
        return "Track and display player statistics";
    }

    @Override
    protected void onEnable() {
        loadSettings();
        loadSnapshots();
        startTopRefresh();
        startTabTask();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        info("Player stats module enabled");
    }

    @Override
    protected void onDisable() {
        if (topTask != null) {
            topTask.cancel();
            topTask = null;
        }
        if (tabTask != null) {
            tabTask.cancel();
            tabTask = null;
        }
        PlayerDeathEvent.getHandlerList().unregister(this);
        PlayerJoinEvent.getHandlerList().unregister(this);
        info("Player stats module disabled");
    }

    @Override
    public void reloadConfig() {
        loadSettings();
        startTopRefresh();
        startTabTask();
    }

    private void startTabTask() {
        if (tabTask != null) {
            tabTask.cancel();
            tabTask = null;
        }
        if (!tabEnabled || (isBlank(tabHeader) && isBlank(tabFooter))) {
            return;
        }
        tabTask = SchedulerCompat.runTaskTimer(plugin, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) {
                applyTab(online);
            }
        }, 40L, tabUpdateSeconds * 20L);
    }

    private void applyTab(Player player) {
        try {
            player.setPlayerListHeaderFooter(
                buildTabText(tabHeader, player), buildTabText(tabFooter, player));
        } catch (Exception ignored) {
        }
    }

    private String buildTabText(String template, Player player) {
        if (template == null || template.isEmpty()) return "";
        PlayerStats stats = getStats(player);
        String result = template.replace("%player%", player.getName());
        if (stats != null) {
            for (String id : STAT_ORDER) {
                result = result.replace("%" + id + "%", formatStatValue(id, stats));
            }
            int rank = getRank(player.getUniqueId(), "kills");
            result = result.replace("%rank%", rank > 0 ? String.valueOf(rank) : "-");
        }
        return plugin.getMessageManager().colorize(result);
    }

    private boolean isBlank(String value) {
        return value == null || value.isEmpty();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!tabEnabled) return;
        SchedulerCompat.runTaskLater(plugin, () -> applyTab(event.getPlayer()), 20L);
    }

    private void loadSettings() {
        ConfigurationSection config = getConfig();

        usePlaceholderApi = config == null || config.getBoolean("use-placeholderapi", true);
        thousandsSeparator = config == null || config.getBoolean("thousands-separator", true);
        kdDecimals = config != null ? Math.max(0, Math.min(6, config.getInt("kd-decimals", 2))) : 2;
        playtimeFormat = config != null ? config.getString("playtime-format", "auto") : "auto";
        showRank = config == null || config.getBoolean("show-rank", true);
        rankFormat = config != null ? config.getString("rank-format", "%value% (#%rank%)") : "%value% (#%rank%)";
        topCacheMinutes = config != null ? Math.max(1, config.getInt("top-cache-minutes", 10)) : 10;
        snapshotsAutoReset = config == null || config.getBoolean("snapshots.auto-reset", true);
        tabEnabled = config != null && config.getBoolean("tab.enabled", false);
        tabHeader = config != null ? config.getString("tab.header", "") : "";
        tabFooter = config != null ? config.getString("tab.footer", "") : "";
        tabUpdateSeconds = config != null ? Math.max(5, config.getInt("tab.update-seconds", 30)) : 30;

        embedTitle = config != null ? config.getString("embed.title", "📊 %player%'s Stats") : "📊 %player%'s Stats";
        embedColor = config != null ? config.getString("embed.color", "#00AAFF") : "#00AAFF";
        embedFooter = config != null ? config.getString("embed.footer", "XDiscordUltimate • Player Stats") : "XDiscordUltimate • Player Stats";
        embedThumbnail = config != null ? config.getString("embed.thumbnail", "https://minotar.net/helm/%uuid%/64.png") : "https://minotar.net/helm/%uuid%/64.png";
        embedInline = config == null || config.getBoolean("embed.inline-fields", true);
        embedTimestamp = config == null || config.getBoolean("embed.timestamp", true);

        ConfigurationSection lines = config != null ? config.getConfigurationSection("lines") : null;
        statEnabled.clear();
        statLabels.clear();
        for (String id : STAT_ORDER) {
            boolean enabled = true;
            String label = DEFAULT_LABELS.getOrDefault(id, id);

            if (lines != null) {
                Object raw = lines.get(id);
                if (raw instanceof Boolean) {
                    enabled = (Boolean) raw;
                } else if (lines.isConfigurationSection(id)) {
                    ConfigurationSection entry = lines.getConfigurationSection(id);
                    enabled = entry.getBoolean("enabled", true);
                    label = entry.getString("label", label);
                }
            }

            statEnabled.put(id, enabled);
            statLabels.put(id, label);
        }
    }

    public boolean isStatEnabled(String id) {
        return statEnabled.getOrDefault(id, true);
    }

    public String getStatLabel(String id) {
        return statLabels.getOrDefault(id, id);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null && killer != victim) {
            debug(killer.getName() + " killed " + victim.getName());
        }
    }

    public PlayerStats getStats(OfflinePlayer player) {
        if (player == null) return null;
        if (!player.hasPlayedBefore() && !player.isOnline()) return null;

        UUID uuid = player.getUniqueId();
        if (uuid != null) {
            CachedStats cached = statsCache.get(uuid);
            if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
                return cached.stats;
            }
        }

        PlayerStats stats = collectStats(player);
        if (uuid != null && stats != null) {
            statsCache.put(uuid, new CachedStats(stats, System.currentTimeMillis() + CACHE_TTL_MS));
        }
        return stats;
    }

    private PlayerStats collectStats(OfflinePlayer player) {
        int kills = getStatistic(player, Statistic.PLAYER_KILLS);
        int deaths = getStatistic(player, Statistic.DEATHS);
        int mobKills = getStatistic(player, Statistic.MOB_KILLS);
        long playtimeTicks = getStatistic(player, Statistic.PLAY_ONE_MINUTE);
        long playtimeMinutes = playtimeTicks / 20 / 60;

        Map<String, Long> values = new LinkedHashMap<>();
        values.put("kills", (long) kills);
        values.put("deaths", (long) deaths);
        values.put("mob-kills", (long) mobKills);
        values.put("playtime", playtimeMinutes);
        values.put("blocks-broken", (long) getStatistic(player, Statistic.MINE_BLOCK));
        values.put("blocks-placed", (long) getStatistic(player, Statistic.USE_ITEM));
        values.put("damage-dealt", getStatistic(player, Statistic.DAMAGE_DEALT) / 10L);
        values.put("damage-taken", getStatistic(player, Statistic.DAMAGE_TAKEN) / 10L);
        values.put("distance-walked", (long) getStatistic(player, Statistic.WALK_ONE_CM));
        values.put("jumps", (long) getStatistic(player, Statistic.JUMP));
        values.put("fish-caught", (long) getStatistic(player, Statistic.FISH_CAUGHT));
        values.put("items-crafted", (long) getStatistic(player, Statistic.CRAFT_ITEM));
        values.put("advancements", countAdvancements(player));

        return new PlayerStats(player.getName(), player.getUniqueId(), kills, deaths, mobKills,
            playtimeMinutes, values);
    }

    private static class CachedStats {
        final PlayerStats stats;
        final long expiresAt;

        CachedStats(PlayerStats stats, long expiresAt) {
            this.stats = stats;
            this.expiresAt = expiresAt;
        }
    }

    public PlayerStats getStats(String playerName) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
        if (player == null || !player.hasPlayedBefore()) {
            return null;
        }
        return getStats(player);
    }

    public PlayerStats getStats(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player == null || !player.hasPlayedBefore()) {
            return null;
        }
        return getStats(player);
    }

    private int getStatistic(OfflinePlayer player, Statistic statistic) {
        try {
            return player.getStatistic(statistic);
        } catch (Exception e) {
            return 0;
        }
    }

    private long countAdvancements(OfflinePlayer player) {
        Player online = player.getPlayer();
        if (online == null) return -1;

        try {
            int count = 0;
            Iterator<Advancement> iterator = Bukkit.advancementIterator();
            while (iterator.hasNext()) {
                if (online.getAdvancementProgress(iterator.next()).isDone()) {
                    count++;
                }
            }
            return count;
        } catch (Exception e) {
            return -1;
        }
    }

    public String formatStatValue(String id, PlayerStats stats) {
        if (stats == null) return "N/A";

        switch (id) {
            case "kd-ratio":
                return String.format(Locale.US, "%." + kdDecimals + "f", stats.kdRatio);
            case "playtime":
                return formatPlaytime(stats.playtimeMinutes);
            case "distance-walked":
                return formatDistance(stats.value("distance-walked"));
            case "advancements":
                long advancements = stats.value("advancements");
                return advancements < 0 ? "N/A" : formatNumber(advancements);
            default:
                return formatNumber(stats.value(id));
        }
    }

    public String formatPlaytime(long minutes) {
        String mode = playtimeFormat == null ? "auto" : playtimeFormat.toLowerCase(Locale.ROOT);
        switch (mode) {
            case "clock": {
                long hours = minutes / 60;
                long mins = minutes % 60;
                return String.format(Locale.US, "%d:%02d", hours, mins);
            }
            case "hours":
                return String.format(Locale.US, "%.2fh", minutes / 60.0);
            case "minutes":
                return minutes + "m";
            case "auto":
            default: {
                long hours = minutes / 60;
                long days = hours / 24;
                hours = hours % 24;
                long mins = minutes % 60;

                if (days > 0) {
                    return days + "d " + hours + "h " + mins + "m";
                } else if (hours > 0) {
                    return hours + "h " + mins + "m";
                }
                return mins + "m";
            }
        }
    }

    private String formatDistance(long centimeters) {
        if (centimeters >= 100_000_000L) {
            return String.format(Locale.US, "%.2f km", centimeters / 100_000.0);
        } else if (centimeters >= 100L) {
            return String.format(Locale.US, "%.1f m", centimeters / 100.0);
        }
        return centimeters + " cm";
    }

    private String formatNumber(long value) {
        if (thousandsSeparator) {
            return String.format(Locale.US, "%,d", value);
        }
        return String.valueOf(value);
    }

    private void startTopRefresh() {
        if (topTask != null) {
            topTask.cancel();
        }
        long interval = topCacheMinutes * 60L * 20L;
        topTask = SchedulerCompat.runTaskTimerAsync(plugin, () -> {
            refreshTopCache();
            maybeAutoResetSnapshots();
        }, 100L, interval);
    }

    /**
     * Rebuild the leaderboard and rank caches from all known players.
     */
    private void refreshTopCache() {
        try {
            Map<String, List<TopEntry>> lists = new HashMap<>();
            Map<UUID, Map<String, Integer>> ranks = new HashMap<>();

            for (String id : STAT_ORDER) {
                if ("advancements".equals(id)) continue;
                lists.put(id, new ArrayList<>());
            }

            for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
                if (offline == null || offline.getUniqueId() == null) continue;
                if (!offline.hasPlayedBefore() && !offline.isOnline()) continue;
                PlayerStats stats = collectStats(offline);
                if (stats == null) continue;
                for (String id : lists.keySet()) {
                    double value = "kd-ratio".equals(id) ? stats.kdRatio : stats.value(id);
                    lists.get(id).add(new TopEntry(stats.uuid, stats.playerName, value, formatStatValue(id, stats)));
                }
            }

            for (Map.Entry<String, List<TopEntry>> entry : lists.entrySet()) {
                List<TopEntry> list = entry.getValue();
                list.sort((a, b) -> Double.compare(b.value, a.value));
                for (int i = 0; i < list.size(); i++) {
                    TopEntry top = list.get(i);
                    ranks.computeIfAbsent(top.uuid, k -> new HashMap<>()).put(entry.getKey(), i + 1);
                }
                if (list.size() > 50) {
                    entry.setValue(new ArrayList<>(list.subList(0, 50)));
                }
            }

            topLists = lists;
            playerRanks = ranks;
            debug("Refreshed stats leaderboard cache");
        } catch (Exception e) {
            warning("Failed to refresh stats leaderboard cache: " + e.getMessage());
        }
    }

    public int getRank(UUID uuid, String statId) {
        if (uuid == null) return -1;
        Map<String, Integer> ranks = playerRanks.get(uuid);
        if (ranks == null) return -1;
        Integer rank = ranks.get(statId);
        return rank != null ? rank : -1;
    }

    public String getTopPlaceholder(String statId, int position, boolean nameOnly) {
        if (!STAT_ORDER.contains(statId) || !isStatEnabled(statId)) return "";
        List<TopEntry> list = topLists.get(statId);
        if (list == null || position < 1 || position > list.size()) return "";
        TopEntry entry = list.get(position - 1);
        return nameOnly ? entry.name : entry.display;
    }

    private void loadSnapshots() {
        snapshotsFile = new File(plugin.getDataFolder(), "stats-snapshots.yml");
        if (!snapshotsFile.exists()) {
            try {
                if (!snapshotsFile.getParentFile().exists()) {
                    snapshotsFile.getParentFile().mkdirs();
                }
                snapshotsFile.createNewFile();
            } catch (Exception e) {
                warning("Could not create stats-snapshots.yml: " + e.getMessage());
            }
        }
        snapshots = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(snapshotsFile);
    }

    private void saveSnapshots() {
        if (snapshots == null || snapshotsFile == null) return;
        try {
            snapshots.save(snapshotsFile);
        } catch (Exception e) {
            warning("Could not save stats-snapshots.yml: " + e.getMessage());
        }
    }

    /**
     * Store the current values of every known player as a baseline for deltas.
     */
    public void createSnapshot(String period) {
        if (snapshots == null) return;
        int players = 0;
        for (OfflinePlayer offline : Bukkit.getOfflinePlayers()) {
            if (offline == null || offline.getUniqueId() == null) continue;
            if (!offline.hasPlayedBefore() && !offline.isOnline()) continue;
            PlayerStats stats = collectStats(offline);
            if (stats == null) continue;
            String base = "players." + offline.getUniqueId() + "." + period + ".";
            for (String id : STAT_ORDER) {
                if ("kd-ratio".equals(id)) continue;
                snapshots.set(base + id, stats.value(id));
            }
            snapshots.set(base + "at", System.currentTimeMillis());
            players++;
        }
        snapshots.set("meta." + period + ".at", System.currentTimeMillis());
        saveSnapshots();
        info("Created " + period + " stats snapshot for " + players + " player(s)");
    }

    private long getSnapshotValue(UUID uuid, String period, String statId) {
        if (snapshots == null || uuid == null) return 0L;
        return snapshots.getLong("players." + uuid + "." + period + "." + statId, 0L);
    }

    private void maybeAutoResetSnapshots() {
        if (!snapshotsAutoReset || snapshots == null) return;
        long week = 7L * 24 * 60 * 60 * 1000;
        long month = 30L * 24 * 60 * 60 * 1000;
        long now = System.currentTimeMillis();
        if (now - snapshots.getLong("meta.weekly.at", 0L) > week) {
            createSnapshot("weekly");
        }
        if (now - snapshots.getLong("meta.monthly.at", 0L) > month) {
            createSnapshot("monthly");
        }
    }

    public EmbedBuilder buildStatsEmbed(PlayerStats stats) {
        if (stats == null) {
            return EmbedUtils.errorEmbed("Player Not Found", "Could not find stats for this player.");
        }

        Map<String, String> placeholders = buildPlaceholders(stats, null);

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(applyPlaceholders(embedTitle, placeholders, stats))
            .setColor(plugin.getEmbedUtils().parseColor(embedColor));

        if (embedFooter != null && !embedFooter.isEmpty()) {
            embed.setFooter(applyPlaceholders(embedFooter, placeholders, stats), null);
        }
        if (embedTimestamp) {
            embed.setTimestamp(java.time.Instant.now());
        }

        String thumbnail = applyPlaceholders(embedThumbnail, placeholders, stats);
        if (thumbnail != null && thumbnail.startsWith("http")) {
            try {
                embed.setThumbnail(thumbnail);
            } catch (IllegalArgumentException ignored) {
            }
        }

        plugin.getEmbedUtils().applyStyle("stats", embed);

        int fields = 0;
        for (String id : STAT_ORDER) {
            if (!isStatEnabled(id)) continue;
            if (fields >= 25) break; // Discord embed field limit
            String value = formatStatValue(id, stats);
            if (showRank && stats.uuid != null) {
                int rank = getRank(stats.uuid, id);
                if (rank > 0 && rankFormat != null && !rankFormat.isEmpty()) {
                    value = rankFormat
                        .replace("%value%", value)
                        .replace("%rank%", String.valueOf(rank));
                }
            }
            embed.addField(getStatLabel(id), value, embedInline);
            fields++;
        }

        return embed;
    }

    public String formatStatsMessage(PlayerStats stats) {
        return String.join("\n", buildStatsLines(stats, null));
    }

    public String formatStatsMessage(PlayerStats stats, Player viewer) {
        return String.join("\n", buildStatsLines(stats, viewer));
    }

    public List<String> buildStatsLines(PlayerStats stats, Player viewer) {
        if (stats == null) {
            String notFound = viewer != null
                ? plugin.getMessageManager().getMessage(viewer.getUniqueId(), "stats.not-found")
                : plugin.getMessageManager().getMessage("stats.not-found");
            return Collections.singletonList(notFound);
        }

        List<String> lines = new ArrayList<>();
        lines.add(resolveStatsMessage(viewer, "stats.header", stats, null));

        for (String id : STAT_ORDER) {
            if (!isStatEnabled(id)) continue;
            lines.add(resolveStatsMessage(viewer, "stats." + id, stats, formatStatValue(id, stats)));
        }

        String footerPath = "stats.footer";
        String footer = resolveStatsMessage(viewer, footerPath, stats, null);
        if (footer != null && !footer.isEmpty() && !footer.contains("Missing message")) {
            lines.add(footer);
        }

        return lines;
    }

    private String resolveStatsMessage(Player viewer, String path, PlayerStats stats, String value) {
        Map<String, String> placeholders = buildPlaceholders(stats, value);
        String message;
        if (viewer != null && viewer.isOnline()) {
            message = plugin.getMessageManager().getMessage(viewer.getUniqueId(), path, placeholders);
        } else {
            message = plugin.getMessageManager().getMessage(path, placeholders);
        }
        return applyPlaceholderApi(message, stats);
    }

    private Map<String, String> buildPlaceholders(PlayerStats stats, String value) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%player%", stats.playerName == null ? "" : stats.playerName);
        placeholders.put("%uuid%", stats.uuid == null ? "" : stats.uuid.toString());
        if (value != null) {
            placeholders.put("%value%", value);
        }
        return placeholders;
    }

    private String applyPlaceholders(String template, Map<String, String> placeholders, PlayerStats stats) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return applyPlaceholderApi(result, stats);
    }

    private String applyPlaceholderApi(String message, PlayerStats stats) {
        if (!usePlaceholderApi || message == null || stats == null || stats.uuid == null) {
            return message;
        }

        try {
            Player online = Bukkit.getPlayer(stats.uuid);
            if (online != null && online.isOnline()) {
                return plugin.parsePlaceholders(message, online);
            }
            return plugin.parsePlaceholders(message, Bukkit.getOfflinePlayer(stats.uuid));
        } catch (Exception ignored) {
        }
        return message;
    }

    public String getStatPlaceholder(OfflinePlayer player, String statId) {
        if (player == null || statId == null) return "";

        if (statId.startsWith("rank-")) {
            String id = statId.substring("rank-".length());
            if (!STAT_ORDER.contains(id) || !isStatEnabled(id)) return "";
            int rank = getRank(player.getUniqueId(), id);
            return rank > 0 ? String.valueOf(rank) : "";
        }

        String period = null;
        if (statId.startsWith("weekly-")) {
            period = "weekly";
            statId = statId.substring("weekly-".length());
        } else if (statId.startsWith("monthly-")) {
            period = "monthly";
            statId = statId.substring("monthly-".length());
        }

        if (!STAT_ORDER.contains(statId)) return "";
        if (!isStatEnabled(statId)) return "";

        PlayerStats stats = getStats(player);
        if (stats == null) return "";

        if (period != null) {
            if ("kd-ratio".equals(statId)) return "N/A";
            long delta = Math.max(0L, stats.value(statId) - getSnapshotValue(player.getUniqueId(), period, statId));
            return formatDelta(statId, delta);
        }
        return formatStatValue(statId, stats);
    }

    private String formatDelta(String id, long delta) {
        switch (id) {
            case "playtime":
                return formatPlaytime(delta);
            case "distance-walked":
                return formatDistance(delta);
            default:
                return formatNumber(delta);
        }
    }

    /**
     * Format two players side by side for /stats compare.
     */
    public List<String> buildCompareLines(PlayerStats first, PlayerStats second, Player viewer) {
        List<String> lines = new ArrayList<>();
        lines.add(compareLine(viewer, "stats.compare-header", "%player%", first.playerName,
            "%player2%", second.playerName, null, null));
        for (String id : STAT_ORDER) {
            if (!isStatEnabled(id)) continue;
            lines.add(compareLine(viewer, "stats.compare-line", "%stat%", getStatLabel(id),
                "%value%", formatStatValue(id, first), "%value2%", formatStatValue(id, second)));
        }
        return lines;
    }

    private String compareLine(Player viewer, String path, String key1, String value1,
                               String key2, String value2, String key3, String value3) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put(key1, value1 == null ? "" : value1);
        placeholders.put(key2, value2 == null ? "" : value2);
        if (key3 != null) placeholders.put(key3, value3 == null ? "" : value3);
        if (viewer != null && viewer.isOnline()) {
            return plugin.getMessageManager().getMessage(viewer.getUniqueId(), path, placeholders);
        }
        return plugin.getMessageManager().getMessage(path, placeholders);
    }

    private static class TopEntry {
        final UUID uuid;
        final String name;
        final double value;
        final String display;

        TopEntry(UUID uuid, String name, double value, String display) {
            this.uuid = uuid;
            this.name = name;
            this.value = value;
            this.display = display;
        }
    }

    public static class PlayerStats {
        public final String playerName;
        public final UUID uuid;
        public final int kills;
        public final int deaths;
        public final int mobKills;
        public final long playtimeMinutes;
        public final double kdRatio;
        private final Map<String, Long> values;

        public PlayerStats(String playerName, UUID uuid, int kills, int deaths, int mobKills,
                           long playtimeMinutes, Map<String, Long> values) {
            this.playerName = playerName;
            this.uuid = uuid;
            this.kills = kills;
            this.deaths = deaths;
            this.mobKills = mobKills;
            this.playtimeMinutes = playtimeMinutes;
            this.kdRatio = deaths > 0 ? (double) kills / deaths : kills;
            this.values = values;
        }

        public long value(String id) {
            return values != null ? values.getOrDefault(id, 0L) : 0L;
        }

        public Map<String, Long> getValues() {
            return Collections.unmodifiableMap(values);
        }
    }
}
