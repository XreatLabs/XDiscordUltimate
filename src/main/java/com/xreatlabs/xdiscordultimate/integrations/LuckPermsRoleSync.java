package com.xreatlabs.xdiscordultimate.integrations;

import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.Map;

/**
 * Mirrors LuckPerms primary groups onto Discord roles.
 * Mappings live under integrations.luckperms-role-sync.mappings.
 */
public final class LuckPermsRoleSync {

    private final XDiscordUltimate plugin;
    private final LuckPerms luckPerms;
    private final Map<String, String> mappings = new HashMap<>();
    private net.luckperms.api.event.EventSubscription<UserDataRecalculateEvent> subscription;

    private LuckPermsRoleSync(XDiscordUltimate plugin, LuckPerms luckPerms) {
        this.plugin = plugin;
        this.luckPerms = luckPerms;
    }

    public static LuckPermsRoleSync start(XDiscordUltimate plugin) {
        Object raw = plugin.getLuckPerms();
        if (!(raw instanceof LuckPerms)) {
            plugin.getLogger().info("LuckPerms not available - role sync disabled");
            return null;
        }

        LuckPermsRoleSync sync = new LuckPermsRoleSync(plugin, (LuckPerms) raw);
        ConfigurationSection section = plugin.getConfig()
            .getConfigurationSection("integrations.luckperms-role-sync.mappings");
        if (section != null) {
            for (String group : section.getKeys(false)) {
                String role = section.getString(group);
                if (role != null && !role.isEmpty()) {
                    sync.mappings.put(group.toLowerCase(), role);
                }
            }
        }

        if (sync.mappings.isEmpty()) {
            plugin.getLogger().info("LuckPerms role sync enabled but no mappings are configured");
            return null;
        }

        try {
            sync.subscription = sync.luckPerms.getEventBus()
                .subscribe(plugin, UserDataRecalculateEvent.class, sync::onRecalculate);
            plugin.getLogger().info("LuckPerms role sync enabled for "
                + sync.mappings.size() + " group mapping(s)");
            return sync;
        } catch (Throwable e) {
            plugin.getLogger().warning("Could not subscribe to LuckPerms events: " + e.getMessage());
            return null;
        }
    }

    public void stop() {
        try {
            if (subscription != null) {
                subscription.close();
            }
        } catch (Throwable ignored) {
        }
    }

    private void onRecalculate(UserDataRecalculateEvent event) {
        String group = event.getUser().getPrimaryGroup();
        if (group == null) return;
        java.util.UUID minecraftId = event.getUser().getUniqueId();

        plugin.getDatabaseManager().getDiscordId(minecraftId).thenAccept(discordId -> {
            if (discordId == null || discordId.isEmpty()) return;
            applyRoles(discordId, group.toLowerCase());
        });
    }

    private void applyRoles(String discordId, String group) {
        if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isReady()) return;
        Guild guild = resolveGuild();
        if (guild == null) return;

        Member member = guild.getMemberById(discordId);
        if (member == null) return;

        String wantedSpec = mappings.get(group);
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            Role role = resolveRole(guild, entry.getValue());
            if (role == null) continue;

            boolean shouldHave = entry.getKey().equals(group) && wantedSpec != null;
            boolean hasRole = member.getRoles().stream().anyMatch(r -> r.getId().equals(role.getId()));

            if (shouldHave && !hasRole) {
                guild.addRoleToMember(member, role).queue(
                    success -> plugin.getLogger().info("LuckPerms sync: gave " + role.getName() + " to " + member.getUser().getName()),
                    error -> plugin.getLogger().warning("LuckPerms sync: failed to add role: " + error.getMessage()));
            } else if (!shouldHave && hasRole) {
                guild.removeRoleFromMember(member, role).queue(
                    success -> plugin.getLogger().info("LuckPerms sync: removed " + role.getName() + " from " + member.getUser().getName()),
                    error -> plugin.getLogger().warning("LuckPerms sync: failed to remove role: " + error.getMessage()));
            }
        }
    }

    private Guild resolveGuild() {
        String guildId = plugin.getConfigManager().getGuildId();
        if (guildId == null || guildId.isEmpty()) return null;
        return plugin.getDiscordManager().getJDA().getGuildById(guildId);
    }

    private Role resolveRole(Guild guild, String spec) {
        if (spec == null || spec.isEmpty()) return null;
        if (spec.matches("\\d+")) {
            Role byId = guild.getRoleById(spec);
            if (byId != null) return byId;
        }
        return guild.getRolesByName(spec, true).stream().findFirst().orElse(null);
    }
}
