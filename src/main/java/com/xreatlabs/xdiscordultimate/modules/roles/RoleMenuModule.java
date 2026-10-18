package com.xreatlabs.xdiscordultimate.modules.roles;

import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import com.xreatlabs.xdiscordultimate.modules.Module;
import com.xreatlabs.xdiscordultimate.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bukkit.configuration.ConfigurationSection;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RoleMenuModule extends Module {

    private final Map<String, RolePanel> panels = new LinkedHashMap<>();
    private RoleMenuListener listener;

    public RoleMenuModule(XDiscordUltimate plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Role Menus";
    }

    @Override
    public String getDescription() {
        return "Self-assignable roles through Discord button menus";
    }

    @Override
    protected void onEnable() {
        loadConfiguration();
        info("Role menu module enabled - " + panels.size() + " panel(s) configured");
    }

    @Override
    public void onDiscordReady() {
        if (listener == null) {
            listener = new RoleMenuListener();
            plugin.getDiscordManager().getJDA().addEventListener(listener);
        }
    }

    @Override
    protected void onDisable() {
        if (listener != null && plugin.getDiscordManager() != null && plugin.getDiscordManager().isReady()) {
            plugin.getDiscordManager().getJDA().removeEventListener(listener);
            listener = null;
        }
        info("Role menu module disabled");
    }

    @Override
    public void reloadConfig() {
        loadConfiguration();
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isReady()) {
            onDiscordReady();
        }
    }

    @Override
    public List<SlashCommandData> getSlashCommands() {
        return Collections.singletonList(
            Commands.slash("rolemenu", "Manage self-assignable role menus")
                .addSubcommands(
                    new SubcommandData("post", "Post a role menu in its configured channel")
                        .addOption(OptionType.STRING, "panel", "Panel id from the config", true),
                    new SubcommandData("list", "List configured role menus")
                )
        );
    }

    @Override
    public boolean handleSlashCommand(SlashCommandInteractionEvent event) {
        if (!"rolemenu".equals(event.getName())) {
            return false;
        }
        Member member = event.getMember();
        if (member == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Guild Only",
                    "This command can only be used in a guild.").build())
                .setEphemeral(true).queue();
            return true;
        }

        String subcommand = event.getSubcommandName();
        if ("list".equals(subcommand)) {
            StringBuilder list = new StringBuilder();
            for (RolePanel panel : panels.values()) {
                list.append("• `").append(panel.id).append("` - ").append(panel.title)
                    .append(" (").append(panel.options.size()).append(" option(s))\n");
            }
            if (list.length() == 0) {
                list.append("No role menus configured.");
            }
            event.replyEmbeds(EmbedUtils.infoEmbed("Role Menus", list.toString()).build())
                .setEphemeral(true).queue();
            return true;
        }

        if (!member.hasPermission(net.dv8tion.jda.api.Permission.MANAGE_ROLES)) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied",
                    "You need the Manage Roles permission to do this.").build())
                .setEphemeral(true).queue();
            return true;
        }

        String panelId = event.getOption("panel").getAsString().toLowerCase(Locale.ROOT);
        RolePanel panel = panels.get(panelId);
        if (panel == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Panel Not Found",
                    "No role menu with id `" + panelId + "` is configured.").build())
                .setEphemeral(true).queue();
            return true;
        }

        Guild guild = event.getGuild();
        TextChannel channel = resolveChannel(guild, panel.channel);
        if (channel == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Channel Not Found",
                    "Could not find channel: " + panel.channel).build())
                .setEphemeral(true).queue();
            return true;
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(panel.title)
            .setDescription(panel.description)
            .setColor(plugin.getEmbedUtils().parseColor(panel.color, EmbedUtils.DISCORD))
            .setFooter("XDiscordUltimate", null)
            .setTimestamp(Instant.now());

        channel.sendMessageEmbeds(embed.build())
            .setComponents(buildRows(panel))
            .queue(
                success -> event.replyEmbeds(EmbedUtils.successEmbed("Role Menu Posted",
                        "Posted `" + panel.id + "` in " + channel.getAsMention() + ".").build())
                    .setEphemeral(true).queue(),
                error -> event.replyEmbeds(EmbedUtils.errorEmbed("Failed",
                        "Could not post the role menu: " + error.getMessage()).build())
                    .setEphemeral(true).queue());
        return true;
    }

    private List<ActionRow> buildRows(RolePanel panel) {
        List<Button> buttons = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, RoleOption> entry : panel.options.entrySet()) {
            if (count++ >= 25) break;
            RoleOption option = entry.getValue();
            Button button = createButton(option.style, "rolemenu:" + panel.id + ":" + entry.getKey(), option.label);
            if (button == null) button = Button.secondary("rolemenu:" + panel.id + ":" + entry.getKey(), option.label);
            if (option.emoji != null && !option.emoji.isEmpty()) {
                try {
                    button = button.withEmoji(Emoji.fromUnicode(option.emoji));
                } catch (Exception ignored) {
                }
            }
            buttons.add(button);
        }

        List<ActionRow> rows = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i += 5) {
            rows.add(ActionRow.of(buttons.subList(i, Math.min(i + 5, buttons.size()))));
        }
        return rows;
    }

    private Button createButton(String style, String customId, String label) {
        if (style == null) return null;
        switch (style.toUpperCase(Locale.ROOT)) {
            case "PRIMARY":
                return Button.primary(customId, label);
            case "SUCCESS":
                return Button.success(customId, label);
            case "DANGER":
                return Button.danger(customId, label);
            case "SECONDARY":
            default:
                return Button.secondary(customId, label);
        }
    }

    private TextChannel resolveChannel(Guild guild, String idOrName) {
        if (idOrName == null || idOrName.trim().isEmpty()) return null;
        String value = idOrName.trim();
        if (value.matches("\\d+")) {
            TextChannel byId = guild.getTextChannelById(value);
            if (byId != null) return byId;
        }
        return guild.getTextChannelsByName(value, true).stream().findFirst().orElse(null);
    }

    private void loadConfiguration() {
        panels.clear();
        ConfigurationSection section = getConfig() != null ? getConfig().getConfigurationSection("panels") : null;
        if (section == null) return;

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;

            String safeId = id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
            RolePanel panel = new RolePanel(
                safeId,
                entry.getString("channel", ""),
                entry.getString("title", "Role Menu"),
                entry.getString("description", "Click a button to get or remove a role."),
                entry.getString("color", ""));

            ConfigurationSection options = entry.getConfigurationSection("options");
            if (options != null) {
                for (String optionId : options.getKeys(false)) {
                    ConfigurationSection option = options.getConfigurationSection(optionId);
                    if (option == null) continue;
                    String roleName = option.getString("role", "");
                    if (roleName.isEmpty()) continue;
                    String safeOptionId = optionId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "_");
                    panel.options.put(safeOptionId, new RoleOption(
                        option.getString("label", optionId),
                        option.getString("emoji", ""),
                        option.getString("style", "SECONDARY"),
                        roleName));
                }
            }

            if (!panel.options.isEmpty()) {
                panels.put(safeId, panel);
            } else {
                warning("Role menu '" + id + "' has no valid options, skipping");
            }
        }
    }

    private Role resolveRole(Guild guild, String spec) {
        if (spec == null || spec.trim().isEmpty()) return null;
        String value = spec.trim();
        if (value.matches("\\d+")) {
            Role byId = guild.getRoleById(value);
            if (byId != null) return byId;
        }
        return guild.getRolesByName(value, true).stream().findFirst().orElse(null);
    }

    private class RoleMenuListener extends ListenerAdapter {
        @Override
        public void onButtonInteraction(ButtonInteractionEvent event) {
            String buttonId = event.getComponentId();
            if (!buttonId.startsWith("rolemenu:")) return;

            Member member = event.getMember();
            if (member == null) return;

            String[] parts = buttonId.split(":", 3);
            if (parts.length < 3) return;

            RolePanel panel = panels.get(parts[1]);
            if (panel == null) return;
            RoleOption option = panel.options.get(parts[2]);
            if (option == null) return;

            Guild guild = event.getGuild();
            Role role = resolveRole(guild, option.role);
            if (role == null) {
                event.replyEmbeds(EmbedUtils.errorEmbed("Role Missing",
                        "The role **" + option.role + "** no longer exists on this server.").build())
                    .setEphemeral(true).queue();
                return;
            }

            if (member.getRoles().contains(role)) {
                guild.removeRoleFromMember(member, role).queue(
                    success -> event.replyEmbeds(EmbedUtils.successEmbed("Role Removed",
                            "Removed **" + role.getName() + "** from you.").build())
                        .setEphemeral(true).queue(),
                    error -> event.replyEmbeds(EmbedUtils.errorEmbed("Failed",
                            "Could not remove the role: " + error.getMessage()).build())
                        .setEphemeral(true).queue());
            } else {
                guild.addRoleToMember(member, role).queue(
                    success -> event.replyEmbeds(EmbedUtils.successEmbed("Role Added",
                            "Gave you **" + role.getName() + "**.").build())
                        .setEphemeral(true).queue(),
                    error -> event.replyEmbeds(EmbedUtils.errorEmbed("Failed",
                            "Could not add the role: " + error.getMessage()).build())
                        .setEphemeral(true).queue());
            }
        }
    }

    private static class RolePanel {
        final String id;
        final String channel;
        final String title;
        final String description;
        final String color;
        final Map<String, RoleOption> options = new LinkedHashMap<>();

        RolePanel(String id, String channel, String title, String description, String color) {
            this.id = id;
            this.channel = channel;
            this.title = title;
            this.description = description;
            this.color = color;
        }
    }

    private static class RoleOption {
        final String label;
        final String emoji;
        final String style;
        final String role;

        RoleOption(String label, String emoji, String style, String role) {
            this.label = label;
            this.emoji = emoji;
            this.style = style;
            this.role = role;
        }
    }
}
