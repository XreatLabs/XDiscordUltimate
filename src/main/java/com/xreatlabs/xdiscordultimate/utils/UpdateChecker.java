package com.xreatlabs.xdiscordultimate.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import com.xreatlabs.xdiscordultimate.utils.SchedulerCompat;

public class UpdateChecker {
    
    private final XDiscordUltimate plugin;
    private final String currentVersion;
    private final OkHttpClient httpClient;
    
    // GitHub API URL for releases (must match the exact repository casing)
    private static final String GITHUB_API_URL = "https://api.github.com/repos/Xreatlabs/XDiscordUltimate/releases/latest";
    // Optional Spigot resource ID; set general.spigot-resource-id in config.yml to enable
    private final int spigotResourceId;
    // Optional Modrinth project slug or ID; set general.modrinth-project in config.yml to enable
    private final String modrinthProject;
    
    public UpdateChecker(XDiscordUltimate plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
        this.httpClient = new OkHttpClient();
        this.spigotResourceId = plugin.getConfig().getInt("general.spigot-resource-id", 0);
        this.modrinthProject = plugin.getConfig().getString("general.modrinth-project", "");
    }
    
    /**
     * Check for updates asynchronously
     */
    public void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                String latestVersion = getLatestVersion();
                
                // Only notify if we got a valid version AND it's actually newer
                if (latestVersion != null && !latestVersion.isEmpty() && isNewerVersion(latestVersion)) {
                    SchedulerCompat.runTask(plugin, () -> {
                        plugin.getLogger().warning("========================================");
                        plugin.getLogger().warning("A new version of XDiscordUltimate is available!");
                        plugin.getLogger().warning("Current version: " + currentVersion);
                        plugin.getLogger().warning("Latest version: " + latestVersion);
                        plugin.getLogger().warning("Download at: https://github.com/Xreatlabs/XDiscordUltimate/releases");
                        if (modrinthProject != null && !modrinthProject.isEmpty()) {
                            plugin.getLogger().warning("Or on Modrinth: https://modrinth.com/plugin/" + modrinthProject);
                        }
                        plugin.getLogger().warning("========================================");
                    });
                } else {
                    plugin.getLogger().info("XDiscordUltimate v" + currentVersion + " is up to date!");
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Update check failed: " + e.getMessage());
            }
        });
    }
    
    /**
     * Get the latest version, trying GitHub, Modrinth, then Spigot
     */
    private String getLatestVersion() {
        String version = getLatestVersionFromGitHub();
        if (version != null) {
            return version;
        }

        version = getLatestVersionFromModrinth();
        if (version != null) {
            return version;
        }

        return getLatestVersionFromSpigot();
    }
    
    /**
     * Get the latest version from GitHub
     */
    private String getLatestVersionFromGitHub() {
        try {
            Request request = new Request.Builder()
                .url(GITHUB_API_URL)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", userAgent())
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                    
                    if (jsonObject.has("tag_name")) {
                        return cleanVersion(jsonObject.get("tag_name").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            // GitHub API failed, try Modrinth next
        }
        
        return null;
    }
    
    /**
     * Get the latest version from Modrinth
     */
    private String getLatestVersionFromModrinth() {
        if (modrinthProject == null || modrinthProject.isEmpty()) {
            return null;
        }
        try {
            Request request = new Request.Builder()
                .url("https://api.modrinth.com/v2/project/" + modrinthProject + "/version")
                .header("User-Agent", userAgent())
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    com.google.gson.JsonArray versions = JsonParser.parseString(json).getAsJsonArray();
                    String fallback = null;
                    for (com.google.gson.JsonElement element : versions) {
                        JsonObject version = element.getAsJsonObject();
                        if (!version.has("version_number")) {
                            continue;
                        }
                        String number = version.get("version_number").getAsString();
                        String type = version.has("version_type") ? version.get("version_type").getAsString() : "";
                        if ("release".equalsIgnoreCase(type)) {
                            return cleanVersion(number);
                        }
                        if (fallback == null) {
                            fallback = cleanVersion(number);
                        }
                    }
                    return fallback;
                }
            }
        } catch (Exception e) {
            // Modrinth API failed, try Spigot next
        }
        
        return null;
    }
    
    private String userAgent() {
        return "XDiscordUltimate/" + currentVersion + " (github.com/XreatLabs/XDiscordUltimate)";
    }
    
    private String cleanVersion(String version) {
        if (version == null) {
            return null;
        }
        version = version.trim();
        // Remove 'v' prefix if present
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        return version;
    }
    
    /**
     * Get the latest version from Spigot (fallback)
     */
    private String getLatestVersionFromSpigot() {
        if (spigotResourceId < 1000) {
            return null;
        }
        try {
            Request request = new Request.Builder()
                .url("https://api.spigotmc.org/legacy/update.php?resource=" + spigotResourceId)
                .header("User-Agent", userAgent())
                .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String version = response.body().string().trim();
                    // Validate it looks like a version number
                    if (version.matches("\\d+\\.\\d+(\\.\\d+)?")) {
                        return version;
                    }
                }
            }
        } catch (Exception e) {
            // Spigot API also failed
        }
        
        return null;
    }
    
    /**
     * Check if a version is newer than the current version
     */
    private boolean isNewerVersion(String latestVersion) {
        if (latestVersion == null || latestVersion.isEmpty()) {
            return false;
        }
        
        try {
            Version current = new Version(currentVersion);
            Version latest = new Version(latestVersion);
            return latest.compareTo(current) > 0;
        } catch (Exception e) {
            // If version parsing fails, assume no update
            return false;
        }
    }
    
    /**
     * Version comparison class
     */
    private static class Version implements Comparable<Version> {
        private final int major;
        private final int minor;
        private final int patch;
        
        public Version(String version) {
            String[] parts = version.split("\\.");
            this.major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            this.minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            this.patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        }
        
        @Override
        public int compareTo(Version other) {
            if (this.major != other.major) {
                return Integer.compare(this.major, other.major);
            }
            if (this.minor != other.minor) {
                return Integer.compare(this.minor, other.minor);
            }
            return Integer.compare(this.patch, other.patch);
        }
    }
}