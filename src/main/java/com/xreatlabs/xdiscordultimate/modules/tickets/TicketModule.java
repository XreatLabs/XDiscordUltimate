package com.xreatlabs.xdiscordultimate.modules.tickets;
import net.dv8tion.jda.api.entities.emoji.Emoji;

import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import com.xreatlabs.xdiscordultimate.modules.Module;
import com.xreatlabs.xdiscordultimate.modules.DependsOn;
import com.xreatlabs.xdiscordultimate.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.GenericComponentInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import com.xreatlabs.xdiscordultimate.utils.SchedulerCompat;

@DependsOn({"verification"})
public class TicketModule extends Module implements org.bukkit.event.Listener {

    public enum TicketPriority {
        LOW("LOW", "🟢", new Color(0x4CAF50)),
        MEDIUM("MEDIUM", "🟡", new Color(0xFFC107)),
        HIGH("HIGH", "🟠", new Color(0xFF9800)),
        URGENT("URGENT", "🔴", new Color(0xE53935));

        // Configurable display values
        public String label;
        public String emoji;
        public Color color;

        TicketPriority(String label, String emoji, Color color) {
            this.label = label;
            this.emoji = emoji;
            this.color = color;
        }

        public static TicketPriority fromString(String value) {
            if (value == null) return MEDIUM;
            switch (value.toUpperCase().trim()) {
                case "LOW": return LOW;
                case "HIGH": return HIGH;
                case "URGENT":
                case "CRITICAL": return URGENT;
                case "MEDIUM":
                default: return MEDIUM;
            }
        }
    }

    private TicketListener listener;
    private String ticketCategoryName;
    private String ticketChannelName;
    private String transcriptChannelName;
    private String supportRoleName;
    private int maxOpenTickets;
    private boolean autoCloseInactive;
    private int inactiveHours;
    private boolean autoCloseWarningEnabled;
    private int autoCloseWarningMinutes;
    private boolean playerCanClose;
    private boolean saveTranscript;
    private int closeDeleteDelaySeconds;
    private String channelNameFormat;
    private String channelTopicFormat;

    // Panel customization
    private String panelTitle;
    private String panelDescription;
    private String panelColorHex;
    private String panelFooter;
    private String panelImage;
    private String panelThumbnail;
    private String panelButtonLabel;
    private String panelButtonEmoji;
    private String panelButtonStyle;
    private String panelMode;
    private String panelSelectPlaceholder;
    private final Map<String, PanelConfig> panels = new LinkedHashMap<>();
    private final Map<String, String> panelChannelIds = new ConcurrentHashMap<>();
    private final Map<String, String> panelMessageIds = new ConcurrentHashMap<>();

    // Welcome embed customization
    private String welcomeTitle;
    private String welcomeDescription;
    private String welcomeColorHex;
    private String welcomeFooter;
    private String welcomeThumbnail;
    private boolean welcomeShowInfoField;

    // Ticket action buttons
    private String closeButtonLabel;
    private String closeButtonEmoji;
    private String claimButtonLabel;
    private String claimButtonEmoji;

    // Creation limits, feedback and transcripts
    private int creationCooldownSeconds;
    private final Map<String, Integer> maxOpenPerType = new HashMap<>();
    private boolean feedbackEnabled;
    private String transcriptFormat;
    private final Map<String, Long> creationCooldowns = new ConcurrentHashMap<>();

    // Ticket types (ordered, first one is the default)
    private final Map<String, TicketType> ticketTypes = new LinkedHashMap<>();

    // Panel persistence (tickets.yml)
    private File dataFile;
    private FileConfiguration data;
    private String panelChannelId = "";
    private String panelMessageId = "";

    // GUI state
    private final Map<java.util.UUID, List<TicketType>> openGuis = new ConcurrentHashMap<>();

    // Active tickets
    private final Map<String, Ticket> activeTickets = new ConcurrentHashMap<>();
    private final Map<String, String> channelToTicket = new ConcurrentHashMap<>();
    private final Map<String, Integer> userTicketCount = new ConcurrentHashMap<>();
    
    public TicketModule(XDiscordUltimate plugin) {
        super(plugin);
    }
    
    @Override
    public String getName() {
        return "Tickets";
    }
    
    @Override
    public String getDescription() {
        return "Support ticket system with Discord integration";
    }
    
    @Override
    protected void onEnable() {
        loadConfiguration();
        backupTicketData();
        loadData();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        info("Ticket module enabled - waiting for Discord...");
    }

    private void backupTicketData() {
        File source = new File(plugin.getDataFolder(), "tickets.yml");
        if (!source.exists()) return;
        try {
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists() && !backupDir.mkdirs()) return;
            String stamp = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
            File backup = new File(backupDir, "tickets-" + stamp + ".yml");
            java.nio.file.Files.copy(source.toPath(), backup.toPath());

            File[] existing = backupDir.listFiles((dir, name) -> name.startsWith("tickets-") && name.endsWith(".yml"));
            if (existing != null && existing.length > 5) {
                java.util.Arrays.sort(existing, java.util.Comparator.comparingLong(File::lastModified));
                for (int i = 0; i < existing.length - 5; i++) {
                    //noinspection ResultOfMethodCallIgnored
                    existing[i].delete();
                }
            }
        } catch (Exception e) {
            warning("Could not back up tickets.yml: " + e.getMessage());
        }
    }
    
    @Override
    public void onDiscordReady() {
        // Register Discord listener
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isReady()) {
            listener = new TicketListener();
            plugin.getDiscordManager().getJDA().addEventListener(listener);

            // Restore tickets that survived a restart
            restoreActiveTickets();

            // Create or update the ticket panel
            createTicketPanel();
            
            // Start auto-close task
            if (autoCloseInactive) {
                startAutoCloseTask();
            }
            info("Ticket system connected to Discord");
        }
    }

    @Override
    public void reloadConfig() {
        loadConfiguration();
        if (plugin.getDiscordManager() != null && plugin.getDiscordManager().isReady()) {
            createTicketPanel();
        }
    }

    @Override
    public List<SlashCommandData> getSlashCommands() {
        return Collections.singletonList(
            Commands.slash("ticket", "Support ticket management")
                .addSubcommands(
                    new SubcommandData("create", "Create a new support ticket")
                        .addOption(OptionType.STRING, "subject", "Ticket subject", false)
                        .addOption(OptionType.STRING, "priority", "Priority: LOW, MEDIUM, HIGH, URGENT", false),
                    new SubcommandData("close", "Close the current ticket channel")
                        .addOption(OptionType.STRING, "reason", "Reason for closing", false),
                    new SubcommandData("adduser", "Add a member to the current ticket")
                        .addOption(OptionType.USER, "user", "Member to add", true),
                    new SubcommandData("removeuser", "Remove a member from the current ticket")
                        .addOption(OptionType.USER, "user", "Member to remove", true),
                    new SubcommandData("info", "Show your open tickets and active ticket count"),
                    new SubcommandData("panel", "Post or refresh a ticket panel")
                        .addOption(OptionType.STRING, "id", "Panel id from config (empty for all panels)", false),
                    new SubcommandData("priority", "Change the priority of the current ticket")
                        .addOption(OptionType.STRING, "level", "New priority: LOW, MEDIUM, HIGH, URGENT", true)
                )
                .addSubcommandGroups(
                    new net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData("edit", "Edit the current ticket")
                        .addSubcommands(
                            new SubcommandData("title", "Edit the ticket title/subject")
                                .addOption(OptionType.STRING, "title", "New ticket title", true),
                            new SubcommandData("description", "Set or update the ticket description")
                                .addOption(OptionType.STRING, "description", "New description text", true),
                            new SubcommandData("priority", "Edit the ticket priority")
                                .addOption(OptionType.STRING, "level", "New priority: LOW, MEDIUM, HIGH, URGENT", true),
                            new SubcommandData("addbutton", "Add a custom action button to the ticket")
                                .addOption(OptionType.STRING, "label", "Button label", true)
                                .addOption(OptionType.STRING, "style", "Button style: PRIMARY, SECONDARY, SUCCESS, DANGER, LINK", true)
                                .addOption(OptionType.STRING, "target", "URL (for LINK) or custom id", false),
                            new SubcommandData("removebutton", "Remove a button by label")
                                .addOption(OptionType.STRING, "label", "Exact label of the button to remove", true)
                        )
                )
        );
    }

    @Override
    public boolean handleSlashCommand(SlashCommandInteractionEvent event) {
        if (!"ticket".equals(event.getName())) {
            return false;
        }

        String subcommand = event.getSubcommandName();
        Member member = event.getMember();
        if (member == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Guild Only",
                    "This command can only be used in a guild.").build())
                .setEphemeral(true).queue();
            return true;
        }

        // Subcommand group: /ticket edit <title|description|priority|addbutton|removebutton>
        if ("edit".equals(event.getSubcommandGroup())) {
            handleSlashTicketEdit(event, member, subcommand);
            return true;
        }

        if ("panel".equals(subcommand)) {
            handleSlashPanel(event, member);
            return true;
        }

        if ("close".equals(subcommand)) {
            handleSlashCloseTicket(event, member);
            return true;
        }

        if ("adduser".equals(subcommand)) {
            handleSlashParticipant(event, member, true);
            return true;
        }

        if ("removeuser".equals(subcommand)) {
            handleSlashParticipant(event, member, false);
            return true;
        }

        if ("info".equals(subcommand)) {
            handleSlashTicketInfo(event, member);
            return true;
        }

        if ("priority".equals(subcommand)) {
            handleSlashTicketPriority(event, member);
            return true;
        }

        handleSlashCreateTicket(event, member);
        return true;
    }

    private void handleSlashPanel(SlashCommandInteractionEvent event, Member member) {
        if (!member.hasPermission(Permission.MANAGE_SERVER)
            && !member.hasPermission(Permission.MANAGE_CHANNEL)) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied",
                    "You need the Manage Server permission to post ticket panels.").build())
                .setEphemeral(true).queue();
            return;
        }
        String requested = event.getOption("id") != null ? event.getOption("id").getAsString().trim() : "";
        int posted = 0;
        for (PanelConfig panel : panels.values()) {
            if (!requested.isEmpty() && !panel.id.equalsIgnoreCase(requested)) continue;
            postOrUpdatePanel(event.getGuild(), panel);
            posted++;
        }
        if (posted == 0) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Unknown Panel",
                    "No panel matches '" + requested + "'. Check the panels section in config.yml.").build())
                .setEphemeral(true).queue();
            return;
        }
        event.replyEmbeds(EmbedUtils.successEmbed("Panels Updated",
                requested.isEmpty() ? "Reposting all ticket panels." : "Reposting the '" + requested + "' panel.").build())
            .setEphemeral(true).queue();
    }

    private void handleSlashTicketEdit(SlashCommandInteractionEvent event, Member member, String editType) {
        Ticket ticket = resolveTicketFromEvent(event);
        if (ticket == null) return;
        if (!checkEditPermission(event, member, ticket)) return;

        switch (editType == null ? "" : editType.toLowerCase()) {
            case "title":
                handleEditTitle(event, member, ticket);
                break;
            case "description":
                handleEditDescription(event, member, ticket);
                break;
            case "priority":
                handleEditPriority(event, member, ticket);
                break;
            case "addbutton":
                handleEditAddButton(event, member, ticket);
                break;
            case "removebutton":
                handleEditRemoveButton(event, member, ticket);
                break;
            default:
                event.replyEmbeds(EmbedUtils.errorEmbed("Unknown Edit",
                        "Unknown ticket edit operation.").build())
                    .setEphemeral(true).queue();
        }
    }

    private void handleEditTitle(SlashCommandInteractionEvent event, Member member, Ticket ticket) {
        String newTitle = event.getOption("title").getAsString().trim();
        if (newTitle.isEmpty() || newTitle.length() > 100) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Invalid Title",
                    "Title must be between 1 and 100 characters.").build())
                .setEphemeral(true).queue();
            return;
        }
        String oldTitle = ticket.subject;
        ticket.subject = newTitle;
        persistSubjectChange(ticket, newTitle);
        refreshWelcomeMessage(ticket);
        updateChannelTopic(ticket);
        event.replyEmbeds(EmbedUtils.successEmbed("Title Updated",
                "Ticket title changed from **" + oldTitle + "** to **" + newTitle + "**.").build())
            .queue();
        info("Ticket #" + ticket.id + " title changed from '" + oldTitle + "' to '" + newTitle
            + "' by " + member.getEffectiveName());
        logTicketAction(ticket.id, "title-edited", member.getEffectiveName());
        saveActiveTickets();
    }

    private void handleEditDescription(SlashCommandInteractionEvent event, Member member, Ticket ticket) {
        String newDescription = event.getOption("description").getAsString().trim();
        if (newDescription.isEmpty() || newDescription.length() > 1000) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Invalid Description",
                    "Description must be between 1 and 1000 characters.").build())
                .setEphemeral(true).queue();
            return;
        }
        ticket.description = newDescription;
        refreshWelcomeMessage(ticket);
        event.replyEmbeds(EmbedUtils.successEmbed("Description Updated",
                "Ticket description has been updated.").build()).queue();
        info("Ticket #" + ticket.id + " description updated by " + member.getEffectiveName());
        logTicketAction(ticket.id, "description-edited", member.getEffectiveName());
    }

    private void handleEditPriority(SlashCommandInteractionEvent event, Member member, Ticket ticket) {
        String levelStr = event.getOption("level").getAsString();
        TicketPriority newPriority = TicketPriority.fromString(levelStr);
        TicketPriority oldPriority = ticket.priority != null ? ticket.priority : TicketPriority.MEDIUM;
        ticket.priority = newPriority;
        refreshWelcomeMessage(ticket);
        updateChannelTopic(ticket);

        EmbedBuilder reply = EmbedUtils.successEmbed("Priority Updated",
                "Ticket #" + ticket.id + " priority changed from " + oldPriority.label + " to " + newPriority.label + ".")
            .setColor(newPriority.color);
        event.replyEmbeds(reply.build()).queue();

        TextChannel channel = event.getGuild().getTextChannelById(ticket.channelId);
        if (channel != null) {
            EmbedBuilder broadcast = EmbedUtils.infoEmbed("Priority Updated",
                    "Ticket #" + ticket.id + " is now " + newPriority.label)
                .setColor(newPriority.color);
            channel.sendMessageEmbeds(broadcast.build()).queue();
        }
        info("Ticket #" + ticket.id + " priority changed from " + oldPriority.label
            + " to " + newPriority.label + " by " + member.getEffectiveName());
        logTicketAction(ticket.id, "priority-edited", member.getEffectiveName());
        saveActiveTickets();
    }

    private void handleEditAddButton(SlashCommandInteractionEvent event, Member member, Ticket ticket) {
        String label = event.getOption("label").getAsString().trim();
        String styleStr = event.getOption("style").getAsString().trim().toUpperCase();
        String target = event.getOption("target") != null ? event.getOption("target").getAsString() : null;

        if (label.isEmpty() || label.length() > 80) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Invalid Label",
                    "Button label must be between 1 and 80 characters.").build())
                .setEphemeral(true).queue();
            return;
        }
        // Reject duplicate labels
        if (ticket.welcomeButtons.stream().anyMatch(b -> label.equals(b.getLabel()))) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Duplicate Button",
                    "A button with that label already exists.").build())
                .setEphemeral(true).queue();
            return;
        }

        Button button;
        try {
            switch (styleStr) {
                case "LINK":
                    if (target == null || target.isEmpty()) {
                        event.replyEmbeds(EmbedUtils.errorEmbed("Missing URL",
                                "LINK style requires a URL target.").build())
                            .setEphemeral(true).queue();
                        return;
                    }
                    if (!target.startsWith("http://") && !target.startsWith("https://")) {
                        event.replyEmbeds(EmbedUtils.errorEmbed("Invalid URL",
                                "Link buttons require an http(s) URL.").build())
                            .setEphemeral(true).queue();
                        return;
                    }
                    button = Button.link(target, label);
                    break;
                case "PRIMARY":
                    button = Button.primary(sanitizeCustomId(target, label), label);
                    break;
                case "SECONDARY":
                    button = Button.secondary(sanitizeCustomId(target, label), label);
                    break;
                case "SUCCESS":
                    button = Button.success(sanitizeCustomId(target, label), label);
                    break;
                case "DANGER":
                    button = Button.danger(sanitizeCustomId(target, label), label);
                    break;
                default:
                    event.replyEmbeds(EmbedUtils.errorEmbed("Invalid Style",
                            "Style must be PRIMARY, SECONDARY, SUCCESS, DANGER, or LINK.").build())
                        .setEphemeral(true).queue();
                    return;
            }
        } catch (IllegalArgumentException e) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Invalid Button",
                    "Could not create button: " + e.getMessage()).build())
                .setEphemeral(true).queue();
            return;
        }

        // Discord allows at most 5 buttons in a row
        if (ticket.welcomeButtons.size() >= 5) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Button Limit Reached",
                    "A ticket can have at most 5 buttons. Remove one first.").build())
                .setEphemeral(true).queue();
            return;
        }

        ticket.welcomeButtons.add(button);
        refreshWelcomeMessage(ticket);
        event.replyEmbeds(EmbedUtils.successEmbed("Button Added",
                "Button **" + label + "** (" + styleStr + ") added to the ticket.").build()).queue();
        info("Ticket #" + ticket.id + " button added '" + label + "' (" + styleStr + ") by "
            + member.getEffectiveName());
        logTicketAction(ticket.id, "button-added", member.getEffectiveName());
    }

    private void handleEditRemoveButton(SlashCommandInteractionEvent event, Member member, Ticket ticket) {
        String label = event.getOption("label").getAsString().trim();
        boolean removed = ticket.welcomeButtons.removeIf(b -> label.equals(b.getLabel()));
        if (!removed) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Button Not Found",
                    "No button with label **" + label + "** exists on this ticket.").build())
                .setEphemeral(true).queue();
            return;
        }
        refreshWelcomeMessage(ticket);
        event.replyEmbeds(EmbedUtils.successEmbed("Button Removed",
                "Button **" + label + "** removed from the ticket.").build()).queue();
        info("Ticket #" + ticket.id + " button removed '" + label + "' by " + member.getEffectiveName());
        logTicketAction(ticket.id, "button-removed", member.getEffectiveName());
    }

    private String sanitizeCustomId(String target, String label) {
        if (target != null && !target.isEmpty()) {
            String trimmed = target.length() > 100 ? target.substring(0, 100) : target;
            return trimmed.replaceAll("[^a-zA-Z0-9_:-]", "_");
        }
        return ("ticket_" + ticketSafeId() + "_" + label)
            .replaceAll("[^a-zA-Z0-9_:-]", "_")
            .substring(0, Math.min(100, label.length() + 16));
    }

    private String ticketSafeId() {
        return String.valueOf(System.currentTimeMillis());
    }

    private void updateChannelTopic(Ticket ticket) {
        if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isReady()) return;
        Guild guild = plugin.getDiscordManager().getMainGuild();
        if (guild == null) return;
        TextChannel channel = guild.getTextChannelById(ticket.channelId);
        if (channel == null) return;
        TicketPriority priority = ticket.priority != null ? ticket.priority : TicketPriority.MEDIUM;
        TicketType type = ticket.type != null ? ticket.type : defaultTicketType();
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%id%", ticket.id);
        placeholders.put("%type%", type.label);
        placeholders.put("%minecraft%", ticket.minecraftName);
        placeholders.put("%user%", "<@" + ticket.userId + ">");
        placeholders.put("%subject%", ticket.subject);
        placeholders.put("%priority%", priority.label);
        String topic = applyPlaceholders(channelTopicFormat, placeholders);
        if (ticket.claimedBy != null) {
            topic = topic + " | Claimed by " + ticket.claimedBy;
        }
        if (topic.length() > 1024) topic = topic.substring(0, 1024);
        channel.getManager().setTopic(topic).queue(
            success -> debug("Updated channel topic for ticket #" + ticket.id),
            error -> warning("Failed to update topic for ticket #" + ticket.id + ": " + error.getMessage())
        );
    }

    private void handleSlashTicketInfo(SlashCommandInteractionEvent event, Member member) {
        event.deferReply(true).queue();
        plugin.getDatabaseManager().getMinecraftUuid(member.getId()).thenAccept(uuid -> {
            if (uuid == null) {
                event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Ticket Info",
                    "You are not verified, so you have no tickets.").build()).queue();
                return;
            }
            plugin.getDatabaseManager().getOpenTicketsCount(uuid).thenAccept(count -> {
                int active = (int) activeTickets.values().stream()
                    .filter(t -> member.getId().equals(t.userId)).count();
                StringBuilder myTickets = new StringBuilder();
                for (Ticket t : activeTickets.values()) {
                    if (!member.getId().equals(t.userId)) continue;
                    myTickets.append("• #").append(t.id)
                        .append(" [").append(t.priority != null ? t.priority.label : "MEDIUM").append("]")
                        .append(" - ").append(t.subject).append("\n");
                }
                EmbedBuilder embed = EmbedUtils.infoEmbed("Ticket Information",
                    "Active server tickets: **" + activeTickets.size() + "**\n"
                        + "Your open tickets: **" + count + "**")
                    .addField("Your Active Channels", myTickets.length() == 0
                        ? "None currently open." : myTickets.toString(), false);
                event.getHook().sendMessageEmbeds(embed.build()).queue();
            });
        });
    }

    private void handleSlashTicketPriority(SlashCommandInteractionEvent event, Member member) {
        String channelId = event.getChannel().getId();
        String ticketId = channelToTicket.get(channelId);
        if (ticketId == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Not a Ticket Channel",
                    "This command can only be used inside an active ticket channel.").build())
                .setEphemeral(true).queue();
            return;
        }
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Ticket Not Found",
                    "This ticket is no longer active.").build())
                .setEphemeral(true).queue();
            return;
        }
        String priorityRoleName = ticket.type != null ? ticket.type.supportRole : supportRoleName;
        boolean isSupport = member.hasPermission(Permission.MANAGE_CHANNEL)
            || member.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(priorityRoleName));
        if (!member.getId().equals(ticket.userId) && !isSupport) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied",
                    "Only the ticket creator or support staff may change this ticket's priority.").build())
                .setEphemeral(true).queue();
            return;
        }
        String levelStr = event.getOption("level").getAsString();
        TicketPriority priority = TicketPriority.fromString(levelStr);
        ticket.priority = priority;
        saveActiveTickets();

        EmbedBuilder reply = EmbedUtils.successEmbed("Priority Updated",
                "Ticket #" + ticket.id + " priority is now " + priority.label + ".")
            .setColor(priority.color);
        event.replyEmbeds(reply.build()).queue();
        info("Ticket #" + ticket.id + " priority changed to " + priority.label + " by " + member.getEffectiveName());

        TextChannel channel = event.getGuild().getTextChannelById(channelId);
        if (channel != null) {
            EmbedBuilder embed = EmbedUtils.infoEmbed("Priority Updated",
                "Ticket #" + ticket.id + " is now " + priority.label)
                .setColor(priority.color);
            channel.sendMessageEmbeds(embed.build()).queue();
        }
    }

    @Override
    public boolean handleTextCommand(MessageReceivedEvent event, String command, String args) {
        if (!"ticket".equals(command)) {
            return false;
        }

        Member member = event.getMember();
        if (member == null) {
            event.getMessage().reply("This command can only be used in a guild.").queue();
            return true;
        }

        String trimmed = args.trim();
        String subcommand = trimmed.isEmpty() ? "create" : trimmed.split(" ", 2)[0].toLowerCase();
        String subArgs = trimmed.isEmpty() ? "" : (trimmed.split(" ", 2).length > 1 ? trimmed.split(" ", 2)[1] : "");

        if ("close".equals(subcommand)) {
            handleTextCloseTicket(event, member, subArgs.trim());
            return true;
        }

        if ("create".equals(subcommand)) {
            handleTextCreateTicket(event, member, subArgs.trim());
            return true;
        }

        event.getMessage().reply("Usage: `!ticket create [subject]` or `!ticket close`").queue();
        return true;
    }

    private void handleTextCreateTicket(MessageReceivedEvent event, Member member, String subject) {
        String userId = member.getId();
        int currentTickets = userTicketCount.getOrDefault(userId, 0);
        if (currentTickets >= maxOpenTickets) {
            event.getMessage().reply("You have reached the maximum number of open tickets (" + maxOpenTickets + ")").queue();
            return;
        }
        String limitError = checkTicketLimits(userId, defaultTicketType().id);
        if (limitError != null) {
            event.getMessage().reply(formatLimitError(limitError)).queue();
            return;
        }

        final String ticketSubject = subject.isEmpty() ? "General Support" : subject;

        plugin.getDatabaseManager().getMinecraftName(userId)
            .thenAccept(minecraftName -> {
                if (minecraftName == null) {
                    event.getMessage().reply("You must verify your Minecraft account first! Use `/verify` in-game.").queue();
                    return;
                }
                event.getMessage().reply("Creating your ticket...").queue();
                createDiscordTicket(userId, minecraftName, ticketSubject);
            })
            .exceptionally(throwable -> {
                event.getMessage().reply("An error occurred while checking your verification status.").queue();
                return null;
            });
    }

    private void handleTextCloseTicket(MessageReceivedEvent event, Member member, String reason) {
        String channelId = event.getChannel().getId();
        String ticketId = channelToTicket.get(channelId);

        if (ticketId == null) {
            event.getMessage().reply("This is not a valid ticket channel!").queue();
            return;
        }

        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            event.getMessage().reply("Ticket not found.").queue();
            return;
        }

        if (!canMemberClose(member, ticket)) {
            event.getMessage().reply("You don't have permission to close this ticket!").queue();
            return;
        }

        event.getMessage().reply("Closing ticket...").queue();
        closeTicket(ticketId, member.getEffectiveName(), reason.isEmpty() ? null : reason);
    }

    private boolean canMemberClose(Member member, Ticket ticket) {
        if (member.hasPermission(Permission.MANAGE_CHANNEL)) return true;
        String roleName = ticket.type != null ? ticket.type.supportRole : supportRoleName;
        if (member.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(roleName))) {
            return true;
        }
        return playerCanClose && member.getId().equals(ticket.userId);
    }

    private void handleSlashCreateTicket(SlashCommandInteractionEvent event, Member member) {
        String userId = member.getId();
        int currentTickets = userTicketCount.getOrDefault(userId, 0);
        if (currentTickets >= maxOpenTickets) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Limit Reached",
                    "You have reached the maximum number of open tickets (" + maxOpenTickets + ").").build())
                .setEphemeral(true).queue();
            return;
        }
        String limitError = checkTicketLimits(userId, defaultTicketType().id);
        if (limitError != null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Limit Reached",
                    formatLimitError(limitError)).build())
                .setEphemeral(true).queue();
            return;
        }

        final TicketPriority priority = event.getOption("priority") != null
            ? TicketPriority.fromString(event.getOption("priority").getAsString())
            : TicketPriority.MEDIUM;

        plugin.getDatabaseManager().getMinecraftName(userId)
            .thenAccept(minecraftName -> {
                if (minecraftName == null) {
                    event.replyEmbeds(EmbedUtils.warningEmbed("Verification Required",
                            "You must verify your Minecraft account first. Use `/verify` in-game.").build())
                        .setEphemeral(true).queue();
                    return;
                }

                String subject = event.getOption("subject") != null
                    ? event.getOption("subject").getAsString()
                    : "General Support";
                event.replyEmbeds(EmbedUtils.infoEmbed("Creating Ticket",
                        "Your support ticket is being created...").build())
                    .setEphemeral(true).queue();
                createDiscordTicket(userId, minecraftName, subject, priority);
            })
            .exceptionally(throwable -> {
                plugin.getLogger().severe("Error checking verification status: " + throwable.getMessage());
                event.replyEmbeds(EmbedUtils.errorEmbed("Verification Check Failed",
                        "An error occurred while checking your verification status.").build())
                    .setEphemeral(true).queue();
                return null;
            });
    }

    private void handleSlashCloseTicket(SlashCommandInteractionEvent event, Member member) {
        String channelId = event.getChannel().getId();
        String ticketId = channelToTicket.get(channelId);

        if (ticketId == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Not a Ticket Channel",
                    "This command can only be used inside an active ticket channel.").build())
                .setEphemeral(true).queue();
            return;
        }

        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Ticket Not Found",
                    "This ticket is no longer active.").build())
                .setEphemeral(true).queue();
            return;
        }

        if (!canMemberClose(member, ticket)) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied",
                    "Only the ticket creator or support staff may close this ticket.").build())
                .setEphemeral(true).queue();
            return;
        }

        String reason = event.getOption("reason") != null
            ? event.getOption("reason").getAsString().trim()
            : null;
        event.replyEmbeds(EmbedUtils.infoEmbed("Closing Ticket",
                "This ticket will be closed shortly.").build()).queue();
        closeTicket(ticketId, member.getEffectiveName(), reason == null || reason.isEmpty() ? null : reason);
    }

    private void handleSlashParticipant(SlashCommandInteractionEvent event, Member member, boolean add) {
        Ticket ticket = resolveTicketFromEvent(event);
        if (ticket == null) return;
        if (!checkEditPermission(event, member, ticket)) return;

        Member target = event.getOption("user").getAsMember();
        if (target == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Member Not Found",
                    "That member could not be found in this server.").build())
                .setEphemeral(true).queue();
            return;
        }
        if (target.getId().equals(ticket.userId)) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Invalid Member",
                    "The ticket creator is already part of this ticket.").build())
                .setEphemeral(true).queue();
            return;
        }

        TextChannel channel = event.getGuild().getTextChannelById(ticket.channelId);
        if (channel == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Ticket Not Found",
                    "This ticket is no longer active.").build())
                .setEphemeral(true).queue();
            return;
        }

        if (add) {
            if (ticket.participants.containsKey(target.getId())) {
                event.replyEmbeds(EmbedUtils.warningEmbed("Already Added",
                        target.getAsMention() + " is already part of this ticket.").build())
                    .setEphemeral(true).queue();
                return;
            }
            ticket.participants.put(target.getId(), target.getEffectiveName());
            channel.upsertPermissionOverride(target)
                .setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND)
                .queue();
            event.replyEmbeds(EmbedUtils.successEmbed("Member Added",
                    target.getAsMention() + " was added to the ticket.").build()).queue();
            channel.sendMessage(target.getAsMention() + " was added to this ticket by "
                + member.getEffectiveName()).queue();
            logTicketAction(ticket.id, "participant-added", member.getEffectiveName());
        } else {
            if (ticket.participants.remove(target.getId()) == null) {
                event.replyEmbeds(EmbedUtils.errorEmbed("Not A Participant",
                        target.getAsMention() + " is not part of this ticket.").build())
                    .setEphemeral(true).queue();
                return;
            }
            channel.upsertPermissionOverride(target)
                .setDenied(Permission.VIEW_CHANNEL)
                .queue();
            event.replyEmbeds(EmbedUtils.successEmbed("Member Removed",
                    target.getAsMention() + " was removed from the ticket.").build()).queue();
            logTicketAction(ticket.id, "participant-removed", member.getEffectiveName());
        }
        refreshWelcomeMessage(ticket);
        saveActiveTickets();
    }

    private String checkTicketLimits(String userId, String typeId) {
        if (creationCooldownSeconds > 0) {
            Long last = creationCooldowns.get(userId);
            if (last != null) {
                long remaining = (last + creationCooldownSeconds * 1000L) - System.currentTimeMillis();
                if (remaining > 0) {
                    return "cooldown:" + ((remaining + 999) / 1000);
                }
            }
        }
        int perTypeLimit = typeId != null ? maxOpenPerType.getOrDefault(typeId, 0) : 0;
        if (perTypeLimit > 0) {
            long count = activeTickets.values().stream()
                .filter(t -> userId.equals(t.userId))
                .filter(t -> t.type != null && typeId.equals(t.type.id))
                .count();
            if (count >= perTypeLimit) {
                return "type:" + typeId + ":" + perTypeLimit;
            }
        }
        return null;
    }

    private String formatLimitError(String limitError) {
        if (limitError.startsWith("cooldown:")) {
            return "Please wait " + limitError.substring("cooldown:".length())
                + " more second(s) before creating another ticket.";
        }
        String[] parts = limitError.split(":", 3);
        String label = resolveTicketType(parts[1]).label;
        return "You already have the maximum number of open '" + label + "' tickets (" + parts[2] + ").";
    }

    @Override
    protected void onDisable() {
        // Unregister Discord listener
        if (listener != null && plugin.getDiscordManager() != null && plugin.getDiscordManager().isReady()) {
            plugin.getDiscordManager().getJDA().removeEventListener(listener);
        }
        org.bukkit.event.inventory.InventoryClickEvent.getHandlerList().unregister(this);
        org.bukkit.event.inventory.InventoryCloseEvent.getHandlerList().unregister(this);
        openGuis.clear();
        
        // Save active tickets
        saveActiveTickets();
        
        info("Ticket module disabled");
    }
    
    private void loadConfiguration() {
        org.bukkit.configuration.ConfigurationSection config = getConfig();

        if (config != null) {
            ticketCategoryName = config.getString("ticket-category", "Support Tickets");
            ticketChannelName = config.getString("ticket-panel-channel", config.getString("transcript-channel", "ticket-logs"));
            transcriptChannelName = config.getString("transcript-channel", "ticket-logs");
            supportRoleName = config.getString("support-role", "Support");
            maxOpenTickets = config.getInt("max-open-tickets", 3);
            autoCloseInactive = config.getInt("auto-close-hours", 48) > 0;
            inactiveHours = config.getInt("auto-close-hours", 48);
            autoCloseWarningEnabled = config.getBoolean("auto-close-warning-enabled", true);
            // Default: warn 1 hour before close
            autoCloseWarningMinutes = config.getInt("auto-close-warning-minutes", 60);
            playerCanClose = config.getBoolean("player-close", true);
            saveTranscript = config.getBoolean("save-transcript", true);
            closeDeleteDelaySeconds = config.getInt("close-delete-delay-seconds", 10);
            channelNameFormat = config.getString("channel-name-format", "ticket-%id%");
            channelTopicFormat = config.getString("channel-topic-format",
                "Support ticket for %user% | MC: %minecraft% | Priority: %priority% | Type: %type%");

            panelTitle = config.getString("panel.title", "Support Tickets");
            panelDescription = config.getString("panel.description",
                "Create a ticket for help from staff.\n\n"
                    + "• Max %max_tickets% open tickets per user\n"
                    + "• Tickets close after %auto_close_hours% hours of inactivity\n"
                    + "• Be respectful to support staff");
            panelColorHex = config.getString("panel.color", "");
            panelFooter = config.getString("panel.footer", "XDiscordUltimate Tickets");
            panelImage = config.getString("panel.image", "");
            panelThumbnail = config.getString("panel.thumbnail", "");
            panelButtonLabel = config.getString("panel.button-label", "Create Ticket");
            panelButtonEmoji = config.getString("panel.button-emoji", "🎫");
            panelButtonStyle = config.getString("panel.button-style", "PRIMARY");
            panelMode = config.getString("panel.mode", "buttons");
            panelSelectPlaceholder = config.getString("panel.select-placeholder", "Select a ticket type");

            welcomeTitle = config.getString("welcome.title", "Ticket #%id% - %priority%");
            welcomeDescription = config.getString("welcome.description",
                "Welcome %user%!\n\n"
                    + "**Type:** %type%\n"
                    + "**Subject:** %subject%\n"
                    + "**Minecraft:** %minecraft%\n"
                    + "**Priority:** %priority%\n"
                    + "%description%\n"
                    + "A support team member will be with you shortly.\n"
                    + "Please describe your issue in detail.");
            welcomeColorHex = config.getString("welcome.color", "");
            welcomeFooter = config.getString("welcome.footer", "XDiscordUltimate Tickets");
            welcomeThumbnail = config.getString("welcome.thumbnail", "https://minotar.net/helm/%minecraft%/64.png");
            welcomeShowInfoField = config.getBoolean("welcome.show-info-field", true);

            closeButtonLabel = config.getString("buttons.close-label", "Close Ticket");
            closeButtonEmoji = config.getString("buttons.close-emoji", "🔒");
            claimButtonLabel = config.getString("buttons.claim-label", "Claim Ticket");
            claimButtonEmoji = config.getString("buttons.claim-emoji", "✋");

            creationCooldownSeconds = config.getInt("creation-cooldown-seconds", 60);
            feedbackEnabled = config.getBoolean("feedback-enabled", true);
            transcriptFormat = config.getString("transcript-format", "text");
            maxOpenPerType.clear();
            ConfigurationSection perType = config.getConfigurationSection("max-open-tickets-per-type");
            if (perType != null) {
                for (String id : perType.getKeys(false)) {
                    maxOpenPerType.put(id.toLowerCase(), perType.getInt(id, 0));
                }
            }

            loadPriorityOverrides(config.getConfigurationSection("priorities"));
            loadTicketTypes(config.getConfigurationSection("types"));
            loadPanels(config);
        } else {
            warning("Config section not found, using defaults");
            ticketCategoryName = "Support Tickets";
            ticketChannelName = "ticket-logs";
            transcriptChannelName = "ticket-logs";
            supportRoleName = "Support";
            maxOpenTickets = 3;
            autoCloseInactive = true;
            inactiveHours = 48;
            autoCloseWarningEnabled = true;
            autoCloseWarningMinutes = 60;
            playerCanClose = true;
            saveTranscript = true;
            closeDeleteDelaySeconds = 10;
            channelNameFormat = "ticket-%id%";
            channelTopicFormat = "Support ticket for %user% | MC: %minecraft% | Priority: %priority% | Type: %type%";

            panelTitle = "Support Tickets";
            panelDescription = "Create a ticket for help from staff.";
            panelColorHex = "";
            panelFooter = "XDiscordUltimate Tickets";
            panelImage = "";
            panelThumbnail = "";
            panelButtonLabel = "Create Ticket";
            panelButtonEmoji = "🎫";
            panelButtonStyle = "PRIMARY";

            welcomeTitle = "Ticket #%id% - %priority%";
            welcomeDescription = "Welcome %user%!\n\n**Subject:** %subject%\n**Minecraft:** %minecraft%";
            welcomeColorHex = "";
            welcomeFooter = "XDiscordUltimate Tickets";
            welcomeThumbnail = "https://minotar.net/helm/%minecraft%/64.png";
            welcomeShowInfoField = true;

            closeButtonLabel = "Close Ticket";
            closeButtonEmoji = "🔒";
            claimButtonLabel = "Claim Ticket";
            claimButtonEmoji = "✋";
            creationCooldownSeconds = 60;
            feedbackEnabled = true;
            transcriptFormat = "text";
            panelMode = "buttons";
            panelSelectPlaceholder = "Select a ticket type";
            maxOpenPerType.clear();
            loadPriorityOverrides(null);
            loadTicketTypes(null);
            loadPanels(null);
        }

        debug("Ticket category: " + ticketCategoryName + ", types: " + ticketTypes.keySet());
    }

    private void loadPriorityOverrides(ConfigurationSection section) {
        // Reset to defaults before applying overrides
        TicketPriority.LOW.label = "LOW";
        TicketPriority.LOW.emoji = "🟢";
        TicketPriority.LOW.color = new Color(0x4CAF50);
        TicketPriority.MEDIUM.label = "MEDIUM";
        TicketPriority.MEDIUM.emoji = "🟡";
        TicketPriority.MEDIUM.color = new Color(0xFFC107);
        TicketPriority.HIGH.label = "HIGH";
        TicketPriority.HIGH.emoji = "🟠";
        TicketPriority.HIGH.color = new Color(0xFF9800);
        TicketPriority.URGENT.label = "URGENT";
        TicketPriority.URGENT.emoji = "🔴";
        TicketPriority.URGENT.color = new Color(0xE53935);

        if (section == null) return;
        for (TicketPriority priority : TicketPriority.values()) {
            ConfigurationSection entry = section.getConfigurationSection(priority.name());
            if (entry == null) continue;
            priority.label = entry.getString("label", priority.label);
            priority.emoji = entry.getString("emoji", priority.emoji);
            priority.color = parseColor(entry.getString("color", ""), priority.color);
        }
    }

    private void loadTicketTypes(ConfigurationSection section) {
        ticketTypes.clear();
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection entry = section.getConfigurationSection(id);
                if (entry == null) continue;
                String safeId = id.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
                TicketType type = new TicketType(safeId, entry.getString("label", id));
                type.emoji = entry.getString("emoji", panelButtonEmoji);
                type.style = entry.getString("style", panelButtonStyle);
                type.category = entry.getString("category", ticketCategoryName);
                type.supportRole = entry.getString("support-role", supportRoleName);
                type.channelNameFormat = entry.getString("channel-name-format", channelNameFormat);
                type.subject = entry.getString("subject", entry.getString("label", "General Support"));
                type.welcomeTitle = entry.getString("welcome-title", null);
                type.welcomeDescription = entry.getString("welcome-description", null);
                type.color = entry.getString("color", null);
                type.guiMaterial = entry.getString("gui-material", "PAPER");
                type.optionDescription = entry.getString("description", "");
                type.pingRole = entry.getString("ping-role", "");
                type.transcriptChannel = entry.getString("transcript-channel", "");
                type.autoCloseHours = entry.getInt("auto-close-hours", -1);
                for (String question : entry.getStringList("questions")) {
                    if (question != null && !question.trim().isEmpty() && type.questions.size() < 5) {
                        type.questions.add(question.trim());
                    }
                }
                ticketTypes.put(safeId, type);
            }
        }
        if (ticketTypes.isEmpty()) {
            TicketType type = new TicketType("default", panelButtonLabel);
            type.emoji = panelButtonEmoji;
            type.style = panelButtonStyle;
            type.category = ticketCategoryName;
            type.supportRole = supportRoleName;
            type.channelNameFormat = channelNameFormat;
            type.subject = "General Support";
            ticketTypes.put("default", type);
        }
    }

    private void loadPanels(ConfigurationSection ticketsRoot) {
        panels.clear();
        ConfigurationSection section = ticketsRoot != null
            ? ticketsRoot.getConfigurationSection("panels") : null;

        if (section == null || section.getKeys(false).isEmpty()) {
            PanelConfig panel = new PanelConfig("default");
            panel.title = panelTitle;
            panel.description = panelDescription;
            panel.color = panelColorHex;
            panel.image = panelImage;
            panel.thumbnail = panelThumbnail;
            panel.footer = panelFooter;
            panel.mode = panelMode;
            panel.selectPlaceholder = panelSelectPlaceholder;
            panels.put("default", panel);
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;
            String safeId = id.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
            PanelConfig panel = new PanelConfig(safeId);
            panel.channelSpec = entry.getString("channel", "");
            panel.mode = entry.getString("mode", panelMode);
            panel.title = entry.getString("title", panelTitle);
            panel.description = entry.getString("description", panelDescription);
            panel.color = entry.getString("color", panelColorHex);
            panel.image = entry.getString("image", panelImage);
            panel.thumbnail = entry.getString("thumbnail", panelThumbnail);
            panel.footer = entry.getString("footer", panelFooter);
            panel.selectPlaceholder = entry.getString("select-placeholder", panelSelectPlaceholder);
            panel.typeIds.addAll(entry.getStringList("types"));
            panels.put(safeId, panel);
        }
    }

    private TicketType defaultTicketType() {
        return ticketTypes.values().iterator().next();
    }

    private TicketType resolveTicketType(String id) {
        if (id != null) {
            TicketType type = ticketTypes.get(id);
            if (type != null) return type;
        }
        return defaultTicketType();
    }

    private Color parseColor(String hex, Color fallback) {
        return plugin.getEmbedUtils().parseColor(hex, fallback);
    }

    /**
     * Create a new ticket
     */
    public void createTicket(Player player, String subject) {
        createTicket(player, subject, TicketPriority.MEDIUM);
    }

    public void createTicket(Player player, String subject, TicketPriority priority) {
        createTicket(player, subject, priority, (TicketType) null);
    }

    public void createTicket(Player player, String subject, TicketPriority priority, String typeId) {
        createTicket(player, subject, priority, resolveTicketType(typeId));
    }

    public void createTicket(Player player, String subject, TicketPriority priority, TicketType type) {
        plugin.getDatabaseManager().getDiscordId(player.getUniqueId())
            .thenAccept(userId -> {
                if (userId == null) {
                    SchedulerCompat.runTask(plugin, () ->
                        plugin.getMessageManager().sendError(player, "errors.discord-not-linked"));
                    return;
                }

                // Check ticket limit
                int currentTickets = userTicketCount.getOrDefault(userId, 0);
                if (currentTickets >= maxOpenTickets) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("%max%", String.valueOf(maxOpenTickets));
                    SchedulerCompat.runTask(plugin, () ->
                        plugin.getMessageManager().sendError(player, "errors.max-tickets", placeholders));
                    return;
                }
                TicketType finalType = type != null ? type : defaultTicketType();
                String limitError = checkTicketLimits(userId, finalType.id);
                if (limitError != null) {
                    SchedulerCompat.runTask(plugin, () ->
                        plugin.getMessageManager().sendRawMessage(player, "&c" + formatLimitError(limitError)));
                    return;
                }

                // Create ticket in Discord
                createDiscordTicket(userId, player.getName(), subject, priority, finalType);

                SchedulerCompat.runTask(plugin, () ->
                    plugin.getMessageManager().sendSuccess(player, "success.ticket-created"));
            })
            .exceptionally(throwable -> {
                SchedulerCompat.runTask(plugin, () ->
                    plugin.getMessageManager().sendError(player, "verification.verification-failed"));
                plugin.getLogger().warning("Failed to create ticket for " + player.getName() + ": " + throwable.getMessage());
                return null;
            });
    }
    
    private void createTicketPanel() {
        Guild guild = plugin.getDiscordManager().getMainGuild();
        if (guild == null) return;

        for (PanelConfig panel : panels.values()) {
            try {
                postOrUpdatePanel(guild, panel);
            } catch (Exception e) {
                warning("Failed to set up the '" + panel.id + "' ticket panel: " + e.getMessage());
            }
        }
    }

    private void postOrUpdatePanel(Guild guild, PanelConfig panel) {
        TextChannel panelChannel = resolvePanelChannel(guild, panel);
        if (panelChannel == null) {
            warning("Could not find or create the panel channel for '" + panel.id + "'");
            return;
        }

        String storedChannel = panelChannelIds.getOrDefault(panel.id, "");
        String storedMessage = panelMessageIds.getOrDefault(panel.id, "");
        if ("default".equals(panel.id)) {
            if (storedChannel.isEmpty()) storedChannel = panelChannelId;
            if (storedMessage.isEmpty()) storedMessage = panelMessageId;
        }

        // Remove the panel from the old channel if it moved
        if (!storedMessage.isEmpty() && !storedChannel.isEmpty()
            && !storedChannel.equals(panelChannel.getId())) {
            TextChannel oldChannel = guild.getTextChannelById(storedChannel);
            if (oldChannel != null) {
                oldChannel.deleteMessageById(storedMessage).queue(
                    success -> debug("Removed old '" + panel.id + "' ticket panel"),
                    failure -> {});
            }
            storedMessage = "";
        }

        final String panelMessage = storedMessage;
        final TextChannel target = panelChannel;
        if (!panelMessage.isEmpty()) {
            // Update the existing panel
            target.retrieveMessageById(panelMessage).queue(
                message -> message.editMessageEmbeds(buildPanelEmbed(panel).build())
                    .setActionRow(buildPanelActions(panel))
                    .queue(
                        success -> debug("Updated '" + panel.id + "' ticket panel in #" + target.getName()),
                        failure -> {
                            warning("Failed to update ticket panel: " + failure.getMessage());
                            sendNewPanel(target, panel);
                        }),
                failure -> sendNewPanel(target, panel));
        } else {
            // Remove old panels before posting a fresh one
            target.getHistory().retrievePast(20).queue(
                messages -> {
                    for (Message message : messages) {
                        if (message.getAuthor().isBot() && hasCreateTicketComponent(message)) {
                            message.delete().queue(success -> {}, failure -> {});
                        }
                    }
                    sendNewPanel(target, panel);
                },
                failure -> sendNewPanel(target, panel));
        }
    }

    private boolean hasCreateTicketComponent(Message message) {
        try {
            for (Button button : message.getButtons()) {
                String id = button.getId();
                if (id != null && id.startsWith("create_ticket")) {
                    return true;
                }
            }
            for (LayoutComponent component : message.getComponents()) {
                if (!(component instanceof ActionRow)) continue;
                for (ItemComponent item : ((ActionRow) component).getComponents()) {
                    if (item instanceof SelectMenu
                        && ((SelectMenu) item).getId().startsWith("create_ticket")) {
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void sendNewPanel(TextChannel channel, PanelConfig panel) {
        channel.sendMessageEmbeds(buildPanelEmbed(panel).build())
            .setActionRow(buildPanelActions(panel))
            .queue(
                sent -> {
                    panelChannelIds.put(panel.id, channel.getId());
                    panelMessageIds.put(panel.id, sent.getId());
                    if ("default".equals(panel.id)) {
                        panelChannelId = channel.getId();
                        panelMessageId = sent.getId();
                    }
                    saveData();
                    debug("Posted '" + panel.id + "' ticket panel in #" + channel.getName());
                },
                failure -> warning("Failed to send ticket panel: " + failure.getMessage()));
    }

    private EmbedBuilder buildPanelEmbed(PanelConfig panel) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%max_tickets%", String.valueOf(maxOpenTickets));
        placeholders.put("%auto_close_hours%", String.valueOf(inactiveHours));
        placeholders.put("%support_role%", supportRoleName);
        placeholders.put("%category%", ticketCategoryName);
        placeholders.put("%enabled%", String.valueOf(autoCloseInactive));

        EmbedBuilder embed = new EmbedBuilder()
            .setTitle(applyPlaceholders(panel.title, placeholders))
            .setDescription(applyPlaceholders(panel.description, placeholders))
            .setTimestamp(Instant.now());

        String footer = applyPlaceholders(panel.footer, placeholders);
        if (footer != null && !footer.isEmpty()) {
            embed.setFooter(footer, null);
        }

        Color color = parseColor(panel.color, EmbedUtils.BRAND_PRIMARY);
        embed.setColor(color);
        try {
            if (panel.image != null && !panel.image.isEmpty()) embed.setImage(panel.image);
            if (panel.thumbnail != null && !panel.thumbnail.isEmpty()) embed.setThumbnail(panel.thumbnail);
        } catch (IllegalArgumentException ignored) {
        }
        plugin.getEmbedUtils().applyStyle("ticket-panel", embed);
        return embed;
    }

    private List<ItemComponent> buildPanelActions(PanelConfig panel) {
        List<ItemComponent> row = new ArrayList<>();
        if ("dropdown".equalsIgnoreCase(panel.mode)) {
            row.add(buildPanelMenu(panel));
        } else {
            row.addAll(buildPanelButtons(panel));
        }
        return row;
    }

    private ItemComponent buildPanelMenu(PanelConfig panel) {
        String placeholder = panel.selectPlaceholder != null && !panel.selectPlaceholder.isEmpty()
            ? panel.selectPlaceholder : "Select a ticket type";
        StringSelectMenu.Builder menu = StringSelectMenu
            .create("create_ticket_menu:" + panel.id)
            .setPlaceholder(placeholder)
            .setRequiredRange(1, 1);

        int added = 0;
        for (TicketType type : resolvePanelTypes(panel)) {
            if (added >= 25) break;
            SelectOption option = SelectOption.of(type.id, type.label);
            if (type.optionDescription != null && !type.optionDescription.isEmpty()) {
                option = option.withDescription(type.optionDescription);
            }
            if (type.emoji != null && !type.emoji.isEmpty()) {
                try {
                    option = option.withEmoji(Emoji.fromUnicode(type.emoji));
                } catch (Exception ignored) {
                }
            }
            menu.addOptions(option);
            added++;
        }
        if (added == 0) {
            TicketType fallback = defaultTicketType();
            menu.addOptions(SelectOption.of(fallback.id, fallback.label));
        }
        return menu.build();
    }

    private List<Button> buildPanelButtons(PanelConfig panel) {
        List<Button> buttons = new ArrayList<>();
        for (TicketType type : resolvePanelTypes(panel)) {
            if (buttons.size() >= 5) break;
            Button button = createButton(type.style, "create_ticket:" + type.id, type.label);
            if (button == null) button = Button.primary("create_ticket:" + type.id, type.label);
            if (type.emoji != null && !type.emoji.isEmpty()) {
                try {
                    button = button.withEmoji(Emoji.fromUnicode(type.emoji));
                } catch (Exception ignored) {
                }
            }
            buttons.add(button);
        }
        if (buttons.isEmpty()) {
            buttons.add(Button.primary("create_ticket", panelButtonLabel));
        }
        return buttons;
    }

    private List<TicketType> resolvePanelTypes(PanelConfig panel) {
        List<TicketType> result = new ArrayList<>();
        if (panel.typeIds.isEmpty()) {
            result.addAll(ticketTypes.values());
            return result;
        }
        for (String typeId : panel.typeIds) {
            TicketType type = ticketTypes.get(typeId.toLowerCase().replaceAll("[^a-z0-9_-]", "_"));
            if (type != null) {
                result.add(type);
            }
        }
        return result;
    }

    private Button createButton(String style, String customId, String label) {
        if (style == null) return null;
        switch (style.toUpperCase()) {
            case "PRIMARY":
                return Button.primary(customId, label);
            case "SECONDARY":
                return Button.secondary(customId, label);
            case "SUCCESS":
                return Button.success(customId, label);
            case "DANGER":
                return Button.danger(customId, label);
            case "LINK":
                if (customId.startsWith("http://") || customId.startsWith("https://")) {
                    return Button.link(customId, label);
                }
                return null;
            default:
                return null;
        }
    }

    private TextChannel resolvePanelChannel(Guild guild, PanelConfig panel) {
        if (panel.channelSpec != null && !panel.channelSpec.isEmpty()) {
            TextChannel channel = resolveTextChannel(guild, panel.channelSpec);
            if (channel != null) return channel;
        }
        return resolvePanelChannel(guild);
    }

    private TextChannel resolvePanelChannel(Guild guild) {
        // 1) Global tickets channel from discord.channels.tickets (ID or name)
        String configured = plugin.getConfig().getString("discord.channels.tickets", "");
        TextChannel channel = resolveTextChannel(guild, configured);
        if (channel != null) return channel;

        // 2) ticket-panel-channel (ID or name)
        channel = resolveTextChannel(guild, ticketChannelName);
        if (channel != null) return channel;

        // 3) Create it in the ticket category
        try {
            Category category = getOrCreateCategory(guild, ticketCategoryName);
            return guild.createTextChannel(ticketChannelName)
                .setParent(category)
                .setTopic("Create a support ticket")
                .complete();
        } catch (Exception e) {
            warning("Failed to create ticket panel channel: " + e.getMessage());
            return null;
        }
    }

    private TextChannel resolveTextChannel(Guild guild, String idOrName) {
        if (idOrName == null || idOrName.trim().isEmpty()) return null;
        String value = idOrName.trim();
        if (value.matches("\\d+")) {
            TextChannel byId = guild.getTextChannelById(value);
            if (byId != null) return byId;
            byId = plugin.getDiscordManager().getTextChannelById(value);
            if (byId != null) return byId;
        }
        return guild.getTextChannelsByName(value, true).stream().findFirst().orElse(null);
    }

    private Role resolveRole(Guild guild, String idOrName) {
        if (idOrName == null || idOrName.trim().isEmpty()) return null;
        String value = idOrName.trim();
        if (value.matches("\\d+")) {
            Role byId = guild.getRoleById(value);
            if (byId != null) return byId;
        }
        return guild.getRolesByName(value, true).stream().findFirst().orElse(null);
    }
    
    private void createDiscordTicket(String userId, String minecraftName, String subject) {
        createDiscordTicket(userId, minecraftName, subject, TicketPriority.MEDIUM, defaultTicketType());
    }

    private void createDiscordTicket(String userId, String minecraftName, String subject, TicketPriority priority) {
        createDiscordTicket(userId, minecraftName, subject, priority, defaultTicketType());
    }

    private void createDiscordTicket(String userId, String minecraftName, String subject,
                                     TicketPriority priority, TicketType type) {
        createDiscordTicket(userId, minecraftName, subject, priority, type, null);
    }

    private void createDiscordTicket(String userId, String minecraftName, String subject,
                                     TicketPriority priority, TicketType type, List<String> answers) {
        Guild guild = plugin.getDiscordManager().getMainGuild();
        if (guild == null) return;

        Member member = guild.getMemberById(userId);
        if (member == null) return;

        final TicketPriority finalPriority = priority != null ? priority : TicketPriority.MEDIUM;
        final TicketType finalType = type != null ? type : defaultTicketType();

        // Get Minecraft UUID for database operations
        plugin.getDatabaseManager().getMinecraftUuid(userId)
            .thenAccept(minecraftUuid -> {
                if (minecraftUuid == null) {
                    // User not properly verified
                    return;
                }

                // Check if user already has too many open tickets
                plugin.getDatabaseManager().getOpenTicketsCount(minecraftUuid)
                    .thenAccept(openCount -> {
                        if (openCount >= maxOpenTickets || checkTicketLimits(userId, finalType.id) != null) {
                            return; // Already handled in button interaction
                        }

                        // Create ticket in database first
                        plugin.getDatabaseManager().createTicket(minecraftUuid, subject)
                            .thenAccept(dbTicketId -> {
                                if (dbTicketId <= 0) {
                                    plugin.getLogger().severe("Failed to create ticket in database");
                                    return;
                                }
                                
                                // Create Discord channel
                                createDiscordChannel(guild, member, userId, minecraftName, subject,
                                    dbTicketId, finalPriority, finalType, answers);
                            });
                    });
            });
    }

    private void createDiscordChannel(Guild guild, Member member, String userId, String minecraftName,
                                       String subject, int dbTicketId, TicketPriority priority, TicketType type,
                                       List<String> answers) {
        TicketPriority finalPriority = priority != null ? priority : TicketPriority.MEDIUM;
        TicketType finalType = type != null ? type : defaultTicketType();

        Map<String, String> channelPlaceholders = new HashMap<>();
        channelPlaceholders.put("%id%", String.valueOf(dbTicketId));
        channelPlaceholders.put("%type%", finalType.label);
        channelPlaceholders.put("%minecraft%", minecraftName);
        channelPlaceholders.put("%user%", member.getEffectiveName());
        channelPlaceholders.put("%subject%", subject);
        channelPlaceholders.put("%priority%", finalPriority.label);

        String channelName = applyPlaceholders(finalType.channelNameFormat, channelPlaceholders);
        if (channelName.length() > 100) channelName = channelName.substring(0, 100);

        // Create ticket channel
        Category category = getOrCreateCategory(guild, finalType.category);
        TextChannel ticketChannel = guild.createTextChannel(channelName)
            .setParent(category)
            .setTopic(applyPlaceholders(channelTopicFormat, channelPlaceholders))
            .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
            .addPermissionOverride(member, EnumSet.of(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), null)
            .complete();

        // Add support role permissions
        Role supportRole = guild.getRolesByName(finalType.supportRole, true).stream().findFirst().orElse(null);
        if (supportRole != null) {
            ticketChannel.upsertPermissionOverride(supportRole)
                .setAllowed(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_MANAGE)
                .queue();
        }

        // Update database with channel ID
        plugin.getDatabaseManager().updateTicketChannel(dbTicketId, ticketChannel.getId());

        // Create ticket object
        Ticket ticket = new Ticket(String.valueOf(dbTicketId), userId, minecraftName, subject, ticketChannel.getId());
        ticket.dbId = dbTicketId;
        ticket.priority = finalPriority;
        ticket.type = finalType;
        if (answers != null) {
            ticket.answers.addAll(answers);
        }
        activeTickets.put(String.valueOf(dbTicketId), ticket);
        channelToTicket.put(ticketChannel.getId(), String.valueOf(dbTicketId));
        userTicketCount.merge(userId, 1, Integer::sum);
        creationCooldowns.put(userId, System.currentTimeMillis());

        // Send welcome message
        EmbedBuilder welcomeEmbed = buildWelcomeEmbed(ticket);
        ticket.welcomeButtons.add(buildActionButton("DANGER", "close_ticket", closeButtonLabel, closeButtonEmoji));
        ticket.welcomeButtons.add(buildActionButton("SECONDARY", "claim_ticket", claimButtonLabel, claimButtonEmoji));

        ticketChannel.sendMessageEmbeds(welcomeEmbed.build())
            .setActionRow(new ArrayList<>(ticket.welcomeButtons))
            .queue(sentMessage -> {
                ticket.welcomeMessageId = sentMessage.getId();
                saveActiveTickets();
                debug("Captured welcome message id " + ticket.welcomeMessageId + " for ticket #" + ticket.id);
            });

        // Notify support role
        if (supportRole != null) {
            ticketChannel.sendMessage(supportRole.getAsMention() + " - New ticket created! (" + finalPriority.label + ")").queue();
        }
        if (finalType.pingRole != null && !finalType.pingRole.isEmpty()
            && (supportRole == null || !finalType.pingRole.equalsIgnoreCase(supportRole.getName()))) {
            Role pingRole = resolveRole(guild, finalType.pingRole);
            if (pingRole != null) {
                ticketChannel.sendMessage(pingRole.getAsMention() + " - New " + finalType.label + " ticket").queue();
            }
        }

        // Log ticket creation
        logTicketAction(String.valueOf(dbTicketId), "created", member.getEffectiveName());
        saveActiveTickets();

        // Notify player in-game if online
        plugin.getDatabaseManager().getMinecraftUuid(userId)
            .thenAccept(uuid -> {
                if (uuid != null) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("%id%", String.valueOf(dbTicketId));
                        placeholders.put("%type%", finalType.label);
                        placeholders.put("%priority%", finalPriority.label);
                        String ticketUrl = guild.getId() + "/" + ticketChannel.getId();
                        String link = "https://discord.com/channels/" + ticketUrl;
                        SchedulerCompat.runTask(plugin, () -> {
                            plugin.getMessageManager().sendMessage(player, "tickets.created", placeholders);
                            plugin.getMessageManager().sendClickableLink(player,
                                "&7Open your ticket in Discord: &b" + link, link);
                        });
                    }
                }
            });
    }

    public void openTicketGui(Player player) {
        List<TicketType> types = new ArrayList<>(ticketTypes.values());
        int limit = Math.min(types.size(), 5);

        org.bukkit.inventory.Inventory inventory = Bukkit.createInventory(null, 27,
            org.bukkit.ChatColor.translateAlternateColorCodes('&', "&8Create a ticket"));
        int[] slots = {10, 11, 12, 13, 14};

        for (int i = 0; i < limit; i++) {
            TicketType type = types.get(i);
            org.bukkit.Material material = org.bukkit.Material.matchMaterial(type.guiMaterial);
            if (material == null || !material.isItem()) {
                material = org.bukkit.Material.PAPER;
            }
            org.bukkit.inventory.ItemStack item = new org.bukkit.inventory.ItemStack(material);
            org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f" + type.emoji + " " + type.label));
                meta.setLore(Collections.singletonList(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&7Click to create this ticket")));
                item.setItemMeta(meta);
            }
            inventory.setItem(slots[i], item);
        }

        openGuis.put(player.getUniqueId(), types.subList(0, limit));
        player.openInventory(inventory);
    }

    @org.bukkit.event.EventHandler
    public void onGuiClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        List<TicketType> types = openGuis.get(player.getUniqueId());
        if (types == null) return;
        if (event.getClickedInventory() == null
            || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }
        event.setCancelled(true);

        int[] slots = {10, 11, 12, 13, 14};
        int index = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == event.getRawSlot()) {
                index = i;
                break;
            }
        }
        if (index < 0 || index >= types.size()) return;

        TicketType type = types.get(index);
        openGuis.remove(player.getUniqueId());
        player.closeInventory();
        createTicket(player, type.subject, TicketPriority.MEDIUM, type);
    }

    @org.bukkit.event.EventHandler
    public void onGuiClose(org.bukkit.event.inventory.InventoryCloseEvent event) {
        openGuis.remove(event.getPlayer().getUniqueId());
    }

    private Button buildActionButton(String style, String customId, String label, String emoji) {
        Button button = createButton(style, customId, label);
        if (button == null) button = Button.secondary(customId, label);
        if (emoji != null && !emoji.isEmpty()) {
            try {
                button = button.withEmoji(Emoji.fromUnicode(emoji));
            } catch (Exception ignored) {
            }
        }
        return button;
    }
    
    private class TicketListener extends ListenerAdapter {
        @Override
        public void onButtonInteraction(ButtonInteractionEvent event) {
            String buttonId = event.getComponentId();
            Member member = event.getMember();
            
            if (member == null) return;

            if (buttonId.startsWith("create_ticket")) {
                String typeId = buttonId.contains(":")
                    ? buttonId.substring(buttonId.indexOf(':') + 1)
                    : null;
                handleCreateTicket(event, typeId, null);
            } else if (buttonId.equals("close_ticket")) {
                handleCloseTicket(event);
            } else if (buttonId.equals("claim_ticket")) {
                handleClaimTicket(event);
            } else if (buttonId.startsWith("feedback:")) {
                handleFeedback(event, buttonId);
            }
        }
        
        @Override
        public void onStringSelectInteraction(StringSelectInteractionEvent event) {
            String componentId = event.getComponentId();
            if (!componentId.startsWith("create_ticket_menu")) return;
            if (event.getMember() == null || event.getValues().isEmpty()) return;
            handleCreateTicket(event, event.getValues().get(0), null);
        }

        @Override
        public void onModalInteraction(ModalInteractionEvent event) {
            String modalId = event.getModalId();
            if (!modalId.startsWith("ticket_questions")) return;
            if (event.getMember() == null) return;
            handleModalAnswers(event);
        }

        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (event.getAuthor().isBot()) return;
            
            String channelId = event.getChannel().getId();
            String ticketId = channelToTicket.get(channelId);
            
            if (ticketId != null) {
                Ticket ticket = activeTickets.get(ticketId);
                if (ticket != null) {
                    ticket.lastActivity = System.currentTimeMillis();
                    
                    // Log message to transcript
                    ticket.addMessage(event.getAuthor().getName(), event.getMessage().getContentRaw());
                }
            }
        }
    }
    
    private void handleCreateTicket(GenericComponentInteractionCreateEvent event, String typeId, List<String> answers) {
        Member member = event.getMember();
        String userId = member.getId();
        final TicketType type = resolveTicketType(typeId);

        // Check ticket limit
        int currentTickets = userTicketCount.getOrDefault(userId, 0);
        if (currentTickets >= maxOpenTickets) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Limit Reached",
                    "You have reached the maximum number of open tickets (" + maxOpenTickets + ").").build())
                .setEphemeral(true).queue();
            return;
        }
        String limitError = checkTicketLimits(userId, type.id);
        if (limitError != null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Limit Reached",
                    formatLimitError(limitError)).build())
                .setEphemeral(true).queue();
            return;
        }

        if (answers == null && !type.questions.isEmpty()) {
            openQuestionsModal(event, type);
            return;
        }

        // Check if verified and get Minecraft name
        plugin.getDatabaseManager().getMinecraftName(userId)
            .thenAccept(minecraftName -> {
                if (minecraftName == null) {
                    event.replyEmbeds(EmbedUtils.warningEmbed("Verification Required",
                            "You must verify your Minecraft account first. Use `/verify` in-game.").build())
                        .setEphemeral(true).queue();
                    return;
                }

                event.replyEmbeds(EmbedUtils.infoEmbed("Creating Ticket",
                        "Your support ticket is being created...").build())
                    .setEphemeral(true).queue();
                createDiscordTicket(userId, minecraftName, type.subject, TicketPriority.MEDIUM, type, answers);
            })
            .exceptionally(throwable -> {
                plugin.getLogger().severe("Error checking verification status: " + throwable.getMessage());
                event.replyEmbeds(EmbedUtils.errorEmbed("Verification Check Failed",
                        "An error occurred while checking your verification status.").build())
                    .setEphemeral(true).queue();
                return null;
            });
    }

    private void openQuestionsModal(GenericComponentInteractionCreateEvent event, TicketType type) {
        Modal.Builder modal = Modal.create("ticket_questions:" + type.id, "New " + type.label + " Ticket");
        int index = 0;
        for (String question : type.questions) {
            String placeholder = question.length() > 100 ? question.substring(0, 97) + "..." : question;
            TextInput input = TextInput
                .create("q" + index, String.valueOf(index + 1) + ". " + shortLabel(question),
                    TextInputStyle.PARAGRAPH)
                .setPlaceholder(placeholder)
                .setRequired(false)
                .setMaxLength(500)
                .build();
            modal.addActionRow(input);
            index++;
        }
        try {
            event.replyModal(modal.build()).queue();
        } catch (Exception e) {
            debug("Could not open the ticket questions modal: " + e.getMessage());
        }
    }

    private String shortLabel(String question) {
        if (question.length() <= 42) return question;
        return question.substring(0, 39) + "...";
    }

    private void handleModalAnswers(ModalInteractionEvent event) {
        String modalId = event.getModalId();
        String typeId = modalId.contains(":") ? modalId.substring(modalId.indexOf(':') + 1) : null;
        final TicketType type = resolveTicketType(typeId);
        Member member = event.getMember();
        if (member == null) return;
        String userId = member.getId();

        List<String> answers = new ArrayList<>();
        for (ModalMapping mapping : event.getValues()) {
            if (mapping.getId() != null && mapping.getId().startsWith("q")) {
                answers.add(mapping.getAsString());
            }
        }

        event.deferReply(true).queue();

        int currentTickets = userTicketCount.getOrDefault(userId, 0);
        if (currentTickets >= maxOpenTickets) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Limit Reached",
                    "You have reached the maximum number of open tickets (" + maxOpenTickets + ").").build()).queue();
            return;
        }
        String limitError = checkTicketLimits(userId, type.id);
        if (limitError != null) {
            event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Limit Reached",
                    formatLimitError(limitError)).build()).queue();
            return;
        }

        plugin.getDatabaseManager().getMinecraftName(userId)
            .thenAccept(minecraftName -> {
                if (minecraftName == null) {
                    event.getHook().sendMessageEmbeds(EmbedUtils.warningEmbed("Verification Required",
                            "You must verify your Minecraft account first. Use `/verify` in-game.").build()).queue();
                    return;
                }
                event.getHook().sendMessageEmbeds(EmbedUtils.infoEmbed("Creating Ticket",
                        "Your support ticket is being created...").build()).queue();
                createDiscordTicket(userId, minecraftName, type.subject, TicketPriority.MEDIUM, type, answers);
            })
            .exceptionally(throwable -> {
                plugin.getLogger().severe("Error checking verification status: " + throwable.getMessage());
                event.getHook().sendMessageEmbeds(EmbedUtils.errorEmbed("Verification Check Failed",
                        "An error occurred while checking your verification status.").build()).queue();
                return null;
            });
    }

    private void handleCloseTicket(ButtonInteractionEvent event) {
        String channelId = event.getChannel().getId();
        String ticketId = channelToTicket.get(channelId);

        if (ticketId == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Not a Ticket Channel",
                    "This button can only be used inside an active ticket channel.").build())
                .setEphemeral(true).queue();
            return;
        }

        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) return;

        // Check permissions
        Member member = event.getMember();
        if (!canMemberClose(member, ticket)) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied",
                    "Only the ticket creator or support staff may close this ticket.").build())
                .setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(EmbedUtils.infoEmbed("Closing Ticket",
                "This ticket will be closed shortly.").build()).queue();
        closeTicket(ticketId, member.getEffectiveName());
    }

    private void handleClaimTicket(ButtonInteractionEvent event) {
        Member member = event.getMember();

        String channelId = event.getChannel().getId();
        String ticketId = channelToTicket.get(channelId);

        if (ticketId == null) return;

        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) return;

        // Check if member has support role
        String roleName = ticket.type != null ? ticket.type.supportRole : supportRoleName;
        boolean isSupport = member.getRoles().stream()
            .anyMatch(role -> role.getName().equalsIgnoreCase(roleName));

        if (!isSupport) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied",
                    "Only support staff can claim tickets.").build())
                .setEphemeral(true).queue();
            return;
        }

        if (ticket.claimedBy != null) {
            event.replyEmbeds(EmbedUtils.warningEmbed("Already Claimed",
                    "This ticket is already claimed by " + ticket.claimedBy + ".").build())
                .setEphemeral(true).queue();
            return;
        }

        ticket.claimedBy = member.getEffectiveName();
        ticket.claimedById = member.getId();

        event.replyEmbeds(EmbedUtils.successEmbed("Ticket Claimed",
                "You have claimed this ticket.").build()).queue();

        // Update channel topic and welcome embed to include the claim
        updateChannelTopic(ticket);
        refreshWelcomeMessage(ticket);

        logTicketAction(ticketId, "claimed", member.getEffectiveName());
        saveActiveTickets();
    }
    
    private void closeTicket(String ticketId, String closedBy) {
        closeTicket(ticketId, closedBy, null);
    }

    private void closeTicket(String ticketId, String closedBy, String reason) {
        Ticket ticket = activeTickets.remove(ticketId);
        if (ticket == null) return;
        ticket.closeReason = reason;

        channelToTicket.remove(ticket.channelId);
        userTicketCount.merge(ticket.userId, -1, Integer::sum);
        saveActiveTickets();

        // Close ticket in database
        if (ticket.dbId > 0) {
            plugin.getDatabaseManager().closeTicket(ticket.dbId);
        }

        Guild guild = plugin.getDiscordManager().getMainGuild();
        TextChannel channel = guild != null ? guild.getTextChannelById(ticket.channelId) : null;

        if (saveTranscript) {
            // Generate transcript and archive it
            String transcript = ticket.generateTranscript();
            String archive = "html".equalsIgnoreCase(transcriptFormat)
                ? ticket.generateHtmlTranscript() : transcript;

            // Send transcript as a file attachment to the transcript channel
            sendTranscriptAttachment(ticket, archive, closedBy);

            // Also save the transcript to disk
            saveTranscript(ticket, archive);
        }

        if (channel != null) {
            // Send closing message
            EmbedBuilder closeEmbed = new EmbedBuilder()
                .setColor(EmbedUtils.BRAND_SECONDARY)
                .setTitle("Ticket Closed")
                .setDescription("This ticket has been closed by **" + closedBy + "**."
                    + (reason != null && !reason.isEmpty() ? "\n**Reason:** " + reason : ""))
                .addField("Ticket ID", ticket.id, true)
                .addField("Priority", ticket.priority != null ? ticket.priority.label : "MEDIUM", true)
                .addField("Duration", formatDuration(System.currentTimeMillis() - ticket.createdAt), true)
                .addField("Claimed By", ticket.claimedBy != null ? ticket.claimedBy : "Nobody", true)
                .setFooter("XDiscordUltimate Tickets", null)
                .setTimestamp(Instant.now());
            plugin.getEmbedUtils().applyStyle("ticket-closed", closeEmbed);

            channel.sendMessageEmbeds(closeEmbed.build()).queue();

            // Delete the channel after the configured delay
            if (closeDeleteDelaySeconds > 0) {
                channel.delete().queueAfter(closeDeleteDelaySeconds, TimeUnit.SECONDS);
            }
        }

        if (feedbackEnabled) {
            sendFeedbackPrompt(ticket, reason);
        }

        // Notify player in-game if online
        plugin.getDatabaseManager().getMinecraftUuid(ticket.userId)
            .thenAccept(uuid -> {
                if (uuid != null) {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        Map<String, String> placeholders = new HashMap<>();
                        placeholders.put("%id%", ticket.id);
                        placeholders.put("%by%", closedBy);
                        placeholders.put("%handled_by%", ticket.claimedBy != null ? ticket.claimedBy : "Nobody");
                        SchedulerCompat.runTask(plugin, () -> {
                            plugin.getMessageManager().sendMessage(player, "tickets.closed", placeholders);
                            if (ticket.claimedBy != null) {
                                plugin.getMessageManager().sendMessage(player, "tickets.handled-by", placeholders);
                            }
                        });
                    }
                }
            });

        logTicketAction(ticketId, "closed", closedBy);
    }

    private void sendFeedbackPrompt(Ticket ticket, String reason) {
        Guild guild = plugin.getDiscordManager() != null ? plugin.getDiscordManager().getMainGuild() : null;
        if (guild == null) return;
        Member creator = guild.getMemberById(ticket.userId);
        if (creator == null) return;

        EmbedBuilder embed = EmbedUtils.infoEmbed("Ticket Feedback",
            "Ticket #" + ticket.id + " was closed."
                + (reason != null && !reason.isEmpty() ? "\n**Reason:** " + reason : "")
                + "\n\nHow was the support you received? Rate it from 1 (poor) to 5 (great).");

        List<Button> stars = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            stars.add(Button.secondary("feedback:" + ticket.id + ":" + i, String.valueOf(i)));
        }

        creator.getUser().openPrivateChannel().queue(
            privateChannel -> privateChannel.sendMessageEmbeds(embed.build())
                .setActionRow(stars)
                .queue(
                    success -> debug("Sent feedback prompt for ticket #" + ticket.id),
                    error -> debug("Failed to send feedback prompt: " + error.getMessage())),
            error -> debug("Could not open DM for feedback prompt: " + error.getMessage()));
    }

    private void handleFeedback(ButtonInteractionEvent event, String buttonId) {
        String[] parts = buttonId.split(":", 3);
        if (parts.length < 3) return;
        int stars;
        try {
            stars = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return;
        }
        if (stars < 1 || stars > 5) return;

        String ticketId = parts[1];
        String base = "feedback." + ticketId + ".";
        data.set(base + "user-id", event.getUser().getId());
        data.set(base + "stars", stars);
        data.set(base + "closed-at", System.currentTimeMillis());
        saveData();

        event.replyEmbeds(EmbedUtils.successEmbed("Thanks!",
                "You rated ticket #" + ticketId + " " + stars + "/5.").build())
            .setEphemeral(true).queue();
        info("Ticket #" + ticketId + " rated " + stars + "/5 by " + event.getUser().getName());
    }

    /**
     * Send the transcript as a .txt file attachment to the configured transcript channel.
     */
    private void sendTranscriptAttachment(Ticket ticket, String transcript, String closedBy) {
        if (plugin.getDiscordManager() == null
            || !plugin.getDiscordManager().isReady()
            || plugin.getDiscordManager().getMainGuild() == null) {
            return;
        }

        Guild guild = plugin.getDiscordManager().getMainGuild();
        String targetTranscript = ticket.type != null && ticket.type.transcriptChannel != null
            && !ticket.type.transcriptChannel.isEmpty()
            ? ticket.type.transcriptChannel : transcriptChannelName;
        TextChannel transcriptChannel = resolveTextChannel(guild, targetTranscript);

        if (transcriptChannel == null) {
            debug("Transcript channel '" + transcriptChannelName + "' not found - skipping attachment upload");
            return;
        }

        // Write transcript to a temporary file for upload
        File tempFile = null;
        try {
            File transcriptsDir = new File(plugin.getDataFolder(), "transcripts");
            if (!transcriptsDir.exists() && !transcriptsDir.mkdirs()) {
                warning("Failed to create transcripts directory for upload");
                return;
            }
            String extension = "html".equalsIgnoreCase(transcriptFormat) ? ".html" : ".txt";
            String fileName = "ticket_" + ticket.id + "_" + System.currentTimeMillis() + extension;
            tempFile = new File(transcriptsDir, fileName);
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(transcript);
            }

            EmbedBuilder summary = new EmbedBuilder()
                .setColor(EmbedUtils.BRAND_PRIMARY)
                .setTitle("Ticket #" + ticket.id + " Transcript")
                .setDescription("Closed by: **" + closedBy + "**")
                .addField("Subject", ticket.subject, false)
                .addField("Minecraft", ticket.minecraftName, true)
                .addField("Priority", ticket.priority != null ? ticket.priority.label : "MEDIUM", true)
                .addField("Claimed By", ticket.claimedBy != null ? ticket.claimedBy : "Nobody", true)
                .setFooter("XDiscordUltimate Tickets", null)
                .setTimestamp(Instant.now());

            transcriptChannel.sendMessageEmbeds(summary.build())
                .addFiles(net.dv8tion.jda.api.utils.FileUpload.fromData(tempFile))
                .queue(
                    success -> info("Uploaded transcript attachment for ticket #" + ticket.id),
                    error -> warning("Failed to upload transcript for ticket #" + ticket.id + ": " + error.getMessage())
                );
        } catch (Exception e) {
            warning("Failed to send transcript attachment for ticket #" + ticket.id + ": " + e.getMessage());
        }
    }

    private void startAutoCloseTask() {
        SchedulerCompat.runTaskTimerAsync(plugin, () -> {
            long warningMillis = (long) autoCloseWarningMinutes * 60 * 1000L;
            long currentTime = System.currentTimeMillis();

            for (Ticket ticket : activeTickets.values()) {
                int hours = ticket.type != null && ticket.type.autoCloseHours > 0
                    ? ticket.type.autoCloseHours : inactiveHours;
                long inactiveMillis = hours * 60 * 60 * 1000L;
                long inactiveFor = currentTime - ticket.lastActivity;
                if (inactiveFor > inactiveMillis) {
                    final String tid = ticket.id;
                    SchedulerCompat.runTask(plugin, () ->
                        closeTicket(tid, "System (Inactivity)", "Inactivity"));
                } else if (autoCloseWarningEnabled
                    && !ticket.autoCloseWarningSent
                    && inactiveFor > (inactiveMillis - warningMillis)) {
                    ticket.autoCloseWarningSent = true;
                    sendAutoCloseWarning(ticket, hours, autoCloseWarningMinutes);
                }
            }
        }, 20L * 60 * 30, 20L * 60 * 30); // Check every 30 minutes
    }

    private void sendAutoCloseWarning(Ticket ticket, int inactiveHours, int warningMinutes) {
        if (plugin.getDiscordManager() == null
            || !plugin.getDiscordManager().isReady()
            || plugin.getDiscordManager().getMainGuild() == null) {
            return;
        }
        TextChannel channel = plugin.getDiscordManager().getMainGuild()
            .getTextChannelById(ticket.channelId);
        if (channel == null) return;

        EmbedBuilder embed = EmbedUtils.warningEmbed("Auto-close Warning",
            "This ticket will be automatically closed in approximately **" + warningMinutes
                + " minute(s)** due to inactivity.")
            .addField("Ticket", "#" + ticket.id, true)
            .addField("Inactive Limit", inactiveHours + " hours", true)
            .setFooter("Reply in this channel to reset the inactivity timer", null);

        channel.sendMessageEmbeds(embed.build()).queue();
        info("Sent auto-close warning for ticket #" + ticket.id);
    }
    
    private Category getOrCreateCategory(Guild guild, String name) {
        String categoryName = name == null || name.isEmpty() ? ticketCategoryName : name;
        return guild.getCategoriesByName(categoryName, true).stream()
            .findFirst()
            .orElseGet(() -> guild.createCategory(categoryName).complete());
    }

    private String generateTicketId() {
        return String.format("%04d", new Random().nextInt(10000));
    }

    private String applyPlaceholders(String template, Map<String, String> placeholders) {
        if (template == null) return "";
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }
    
    private void logTicketAction(String ticketId, String action, String user) {
        info("Ticket #" + ticketId + " " + action + " by " + user);
    }
    
    private void saveTranscript(Ticket ticket, String transcript) {
        try {
            // Create transcripts directory if it doesn't exist
            File transcriptsDir = new File(plugin.getDataFolder(), "transcripts");
            if (!transcriptsDir.exists()) {
                transcriptsDir.mkdirs();
            }
            
            // Save transcript to file
            String extension = "html".equalsIgnoreCase(transcriptFormat) ? ".html" : ".txt";
            String fileName = "ticket_" + ticket.id + "_" + System.currentTimeMillis() + extension;
            File transcriptFile = new File(transcriptsDir, fileName);
            
            try (java.io.FileWriter writer = new java.io.FileWriter(transcriptFile)) {
                writer.write(transcript);
            }
            
            debug("Saved transcript for ticket #" + ticket.id + " to " + fileName);
            
            // Also save messages to database
            if (ticket.dbId > 0) {
                for (TicketMessage msg : ticket.messages) {
                    plugin.getDatabaseManager().addTicketMessage(
                        ticket.dbId,
                        null, // We don't have UUID for Discord messages
                        "DISCORD",
                        msg.author + ": " + msg.content
                    );
                }
            }
            
        } catch (Exception e) {
            error("Failed to save transcript for ticket #" + ticket.id, e);
        }
    }
    
    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "tickets.yml");
        if (!dataFile.exists()) {
            try {
                if (!dataFile.getParentFile().exists()) {
                    dataFile.getParentFile().mkdirs();
                }
                dataFile.createNewFile();
            } catch (Exception e) {
                warning("Could not create tickets.yml: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        panelChannelId = data.getString("panel.channel-id", "");
        panelMessageId = data.getString("panel.message-id", "");
        panelChannelIds.clear();
        panelMessageIds.clear();
        ConfigurationSection panelSection = data.getConfigurationSection("panels");
        if (panelSection != null) {
            for (String id : panelSection.getKeys(false)) {
                panelChannelIds.put(id, panelSection.getString(id + ".channel-id", ""));
                panelMessageIds.put(id, panelSection.getString(id + ".message-id", ""));
            }
        }
    }

    private void saveData() {
        if (data == null || dataFile == null) return;
        try {
            data.save(dataFile);
        } catch (Exception e) {
            warning("Could not save tickets.yml: " + e.getMessage());
        }
    }

    private void saveActiveTickets() {
        if (data == null) return;

        data.set("active", null);
        for (Ticket ticket : activeTickets.values()) {
            String base = "active." + ticket.id + ".";
            data.set(base + "user-id", ticket.userId);
            data.set(base + "minecraft", ticket.minecraftName);
            data.set(base + "subject", ticket.subject);
            data.set(base + "channel-id", ticket.channelId);
            data.set(base + "message-id", ticket.welcomeMessageId);
            data.set(base + "priority", ticket.priority != null ? ticket.priority.name() : "MEDIUM");
            data.set(base + "type", ticket.type != null ? ticket.type.id : "default");
            data.set(base + "claimed-by", ticket.claimedBy);
            data.set(base + "claimed-by-id", ticket.claimedById);
            data.set(base + "created-at", ticket.createdAt);
            data.set(base + "last-activity", ticket.lastActivity);
            List<String> participantEntries = new ArrayList<>();
            for (Map.Entry<String, String> participant : ticket.participants.entrySet()) {
                participantEntries.add(participant.getKey() + "|" + participant.getValue());
            }
            data.set(base + "participants", participantEntries);
        }
        data.set("panel.channel-id", panelChannelId);
        data.set("panel.message-id", panelMessageId);
        data.set("panels", null);
        for (Map.Entry<String, String> entry : panelMessageIds.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) continue;
            String base = "panels." + entry.getKey() + ".";
            data.set(base + "channel-id", panelChannelIds.getOrDefault(entry.getKey(), ""));
            data.set(base + "message-id", entry.getValue());
        }
        saveData();
    }

    /**
     * Restore tickets that survived a server restart.
     */
    private void restoreActiveTickets() {
        if (data == null) return;
        Guild guild = plugin.getDiscordManager().getMainGuild();
        if (guild == null) return;
        ConfigurationSection section = data.getConfigurationSection("active");
        if (section == null) return;

        int restored = 0;
        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) continue;

            String channelId = entry.getString("channel-id", "");
            if (channelId.isEmpty() || guild.getTextChannelById(channelId) == null) {
                continue;
            }

            String userId = entry.getString("user-id", "");
            String minecraftName = entry.getString("minecraft", "Unknown");
            String subject = entry.getString("subject", "General Support");

            Ticket ticket = new Ticket(id, userId, minecraftName, subject, channelId);
            try {
                ticket.dbId = Integer.parseInt(id);
            } catch (NumberFormatException ignored) {
                ticket.dbId = -1;
            }
            ticket.welcomeMessageId = entry.getString("message-id", null);
            ticket.priority = TicketPriority.fromString(entry.getString("priority", "MEDIUM"));
            ticket.type = resolveTicketType(entry.getString("type", null));
            ticket.claimedBy = entry.getString("claimed-by", null);
            ticket.claimedById = entry.getString("claimed-by-id", null);
            ticket.createdAt = entry.getLong("created-at", System.currentTimeMillis());
            ticket.lastActivity = entry.getLong("last-activity", System.currentTimeMillis());
            for (String participantEntry : entry.getStringList("participants")) {
                String[] parts = participantEntry.split("\\|", 2);
                if (parts.length == 2 && !parts[0].isEmpty()) {
                    ticket.participants.put(parts[0], parts[1]);
                }
            }
            ticket.welcomeButtons.add(buildActionButton("DANGER", "close_ticket", closeButtonLabel, closeButtonEmoji));
            ticket.welcomeButtons.add(buildActionButton("SECONDARY", "claim_ticket", claimButtonLabel, claimButtonEmoji));

            activeTickets.put(id, ticket);
            channelToTicket.put(channelId, id);
            userTicketCount.merge(userId, 1, Integer::sum);
            restored++;
        }

        if (restored > 0) {
            info("Restored " + restored + " active ticket(s)");
        }
        saveActiveTickets();
    }

    private String formatDuration(long millis) {
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        return String.format("%d hours, %d minutes", hours, minutes);
    }

    /**
     * Build the welcome embed for a ticket. Used for initial send and in-place edits.
     */
    private EmbedBuilder buildWelcomeEmbed(Ticket ticket) {
        TicketPriority priority = ticket.priority != null ? ticket.priority : TicketPriority.MEDIUM;
        TicketType type = ticket.type != null ? ticket.type : defaultTicketType();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("%id%", ticket.id);
        placeholders.put("%user%", "<@" + ticket.userId + ">");
        placeholders.put("%minecraft%", ticket.minecraftName);
        placeholders.put("%subject%", ticket.subject);
        placeholders.put("%priority%", priority.label);
        placeholders.put("%priority_emoji%", priority.emoji);
        placeholders.put("%type%", type.label);
        placeholders.put("%claimed_by%", ticket.claimedBy != null ? ticket.claimedBy : "Nobody");
        placeholders.put("%created%", "<t:" + (ticket.createdAt / 1000) + ":R>");
        placeholders.put("%description%", ticket.description != null && !ticket.description.isEmpty()
            ? "**Description:**\n" + ticket.description : "");

        String titleTemplate = type.welcomeTitle != null && !type.welcomeTitle.isEmpty()
            ? type.welcomeTitle : welcomeTitle;
        String descriptionTemplate = type.welcomeDescription != null && !type.welcomeDescription.isEmpty()
            ? type.welcomeDescription : welcomeDescription;

        Color color = priority.color;
        if (type.color != null && !type.color.isEmpty()) {
            color = parseColor(type.color, color);
        } else {
            color = parseColor(welcomeColorHex, color);
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(color)
            .setTitle(applyPlaceholders(titleTemplate, placeholders))
            .setDescription(applyPlaceholders(descriptionTemplate, placeholders))
            .setTimestamp(Instant.now());

        String footer = applyPlaceholders(welcomeFooter, placeholders);
        if (!footer.isEmpty()) {
            embed.setFooter(footer, null);
        }

        String thumbnail = applyPlaceholders(welcomeThumbnail, placeholders);
        if (!thumbnail.isEmpty()) {
            try {
                embed.setThumbnail(thumbnail);
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (welcomeShowInfoField) {
            StringBuilder info = new StringBuilder();
            info.append("Created: ").append(placeholders.get("%created%")).append("\n");
            info.append("Status: ").append(ticket.claimedBy != null
                ? "Claimed by " + ticket.claimedBy : "Open").append("\n");
            info.append("Priority: ").append(priority.emoji).append(" ").append(priority.label);
            if (!ticket.participants.isEmpty()) {
                List<String> mentions = new ArrayList<>();
                for (String id : ticket.participants.keySet()) {
                    mentions.add("<@" + id + ">");
                }
                info.append("\nParticipants: ").append(String.join(", ", mentions));
            }
            embed.addField("Ticket Information", info.toString(), false);
        }

        if (!ticket.answers.isEmpty() && ticket.type != null) {
            StringBuilder form = new StringBuilder();
            List<String> questions = ticket.type.questions;
            for (int i = 0; i < ticket.answers.size(); i++) {
                String question = i < questions.size() ? questions.get(i) : "Question " + (i + 1);
                if (form.length() > 0) form.append("\n\n");
                form.append("**").append(question).append("**\n").append(ticket.answers.get(i));
            }
            String value = form.toString();
            if (value.length() > 1024) value = value.substring(0, 1021) + "...";
            embed.addField("Form Answers", value, false);
        }
        plugin.getEmbedUtils().applyStyle("ticket-welcome", embed);
        return embed;
    }

    /**
     * Re-render and in-place edit the welcome message for a ticket.
     */
    private void refreshWelcomeMessage(Ticket ticket) {
        if (ticket.welcomeMessageId == null) {
            debug("Cannot refresh welcome message for ticket #" + ticket.id + " - no message id captured yet");
            return;
        }
        Guild guild = plugin.getDiscordManager() != null ? plugin.getDiscordManager().getMainGuild() : null;
        if (guild == null) return;
        TextChannel channel = guild.getTextChannelById(ticket.channelId);
        if (channel == null) return;

        try {
            channel.editMessageEmbedsById(ticket.welcomeMessageId, buildWelcomeEmbed(ticket).build())
                .setActionRow(new ArrayList<>(ticket.welcomeButtons))
                .queue(
                    success -> debug("Refreshed welcome embed for ticket #" + ticket.id),
                    error -> warning("Failed to refresh welcome embed for ticket #" + ticket.id
                        + ": " + error.getMessage())
                );
        } catch (Exception e) {
            warning("Exception while refreshing welcome embed for ticket #" + ticket.id + ": " + e.getMessage());
        }
    }

    /**
     * Resolve a ticket from a slash command event's channel.
     * Returns null and sends an ephemeral error embed if the channel is not a ticket channel.
     */
    private Ticket resolveTicketFromEvent(SlashCommandInteractionEvent event) {
        String channelId = event.getChannel().getId();
        String ticketId = channelToTicket.get(channelId);
        if (ticketId == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Not a Ticket Channel",
                    "This command can only be used inside an active ticket channel.").build())
                .setEphemeral(true).queue();
            return null;
        }
        Ticket ticket = activeTickets.get(ticketId);
        if (ticket == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Ticket Not Found",
                    "This ticket is no longer active.").build())
                .setEphemeral(true).queue();
            return null;
        }
        return ticket;
    }

    /**
     * Permission gate for ticket edits. Returns true if the member may edit the ticket.
     * Sends an ephemeral error embed and returns false otherwise.
     */
    private boolean checkEditPermission(SlashCommandInteractionEvent event, Member member, Ticket ticket) {
        if (member == null) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied",
                    "This command can only be used in a guild.").build())
                .setEphemeral(true).queue();
            return false;
        }
        boolean isCreator = member.getId().equals(ticket.userId);
        String roleName = ticket.type != null ? ticket.type.supportRole : supportRoleName;
        boolean isSupport = member.hasPermission(Permission.MANAGE_CHANNEL)
            || member.getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase(roleName));
        if (!isCreator && !isSupport) {
            event.replyEmbeds(EmbedUtils.errorEmbed("Permission Denied",
                    "Only the ticket creator or support staff may edit this ticket.").build())
                .setEphemeral(true).queue();
            return false;
        }
        return true;
    }

    /**
     * Persist subject changes to the database when supported.
     */
    private void persistSubjectChange(Ticket ticket, String newSubject) {
        if (ticket.dbId <= 0) return;
        plugin.getDatabaseManager().executeAsync(() -> {
            String sql = "UPDATE tickets SET subject = ? WHERE id = ?";
            try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection();
                 java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, newSubject);
                stmt.setInt(2, ticket.dbId);
                stmt.executeUpdate();
            }
            return null;
        });
    }
    
    /**
     * Ticket data class
     */
    private static class Ticket {
        final String id;
        final String userId;
        final String minecraftName;
        String subject;
        String description;
        final String channelId;
        String welcomeMessageId;
        final List<Button> welcomeButtons;
        long createdAt;
        long lastActivity;
        String claimedBy;
        String claimedById;
        String closeReason;
        final Map<String, String> participants;
        TicketPriority priority;
        TicketType type;
        boolean autoCloseWarningSent;
        final List<String> answers;
        final List<TicketMessage> messages;
        int dbId; // Database ticket ID

        Ticket(String id, String userId, String minecraftName, String subject, String channelId) {
            this.id = id;
            this.userId = userId;
            this.minecraftName = minecraftName;
            this.subject = subject;
            this.description = "";
            this.channelId = channelId;
            this.welcomeMessageId = null;
            this.welcomeButtons = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
            this.lastActivity = createdAt;
            this.closeReason = null;
            this.participants = new LinkedHashMap<>();
            this.priority = TicketPriority.MEDIUM;
            this.type = null;
            this.autoCloseWarningSent = false;
            this.answers = new ArrayList<>();
            this.messages = new ArrayList<>();
            this.dbId = -1;
        }

        void addMessage(String author, String content) {
            messages.add(new TicketMessage(author, content, System.currentTimeMillis()));
        }

        String generateTranscript() {
            StringBuilder transcript = new StringBuilder();
            transcript.append("Ticket #").append(id).append(" - ").append(subject).append("\n");
            transcript.append("User: ").append(minecraftName).append("\n");
            transcript.append("Priority: ").append(priority != null ? priority.label : "MEDIUM").append("\n");
            transcript.append("Created: ").append(new Date(createdAt)).append("\n");
            transcript.append("Claimed by: ").append(claimedBy != null ? claimedBy : "None").append("\n");
            if (!participants.isEmpty()) {
                transcript.append("Participants: ").append(String.join(", ", participants.values())).append("\n");
            }
            if (closeReason != null && !closeReason.isEmpty()) {
                transcript.append("Close reason: ").append(closeReason).append("\n");
            }
            transcript.append("\n--- Messages ---\n\n");

            for (TicketMessage msg : messages) {
                transcript.append("[").append(new Date(msg.timestamp)).append("] ");
                transcript.append(msg.author).append(": ").append(msg.content).append("\n");
            }

            return transcript.toString();
        }

        String generateHtmlTranscript() {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"utf-8\">\n");
            html.append("<title>Ticket #").append(escapeHtml(id)).append("</title>\n");
            html.append("<style>")
                .append("body{font-family:Arial,Helvetica,sans-serif;background:#313338;color:#dbdee1;margin:24px;}")
                .append("h1{font-size:20px;}h2{font-size:16px;}")
                .append(".meta{padding:12px;margin:16px 0;background:#2b2d31;border-radius:8px;line-height:1.6;}")
                .append(".msg{padding:8px 12px;margin:6px 0;background:#2b2d31;border-radius:6px;}")
                .append(".author{color:#5865f2;font-weight:bold;}")
                .append(".time{color:#949ba4;font-size:12px;margin-left:8px;}")
                .append("pre{white-space:pre-wrap;margin:4px 0 0;font-family:inherit;}")
                .append("</style>\n</head>\n<body>\n");

            html.append("<h1>Ticket #").append(escapeHtml(id)).append(" - ")
                .append(escapeHtml(subject)).append("</h1>\n");
            html.append("<div class=\"meta\">");
            html.append("<div><b>User:</b> ").append(escapeHtml(minecraftName)).append("</div>");
            html.append("<div><b>Priority:</b> ")
                .append(escapeHtml(priority != null ? priority.label : "MEDIUM")).append("</div>");
            html.append("<div><b>Created:</b> ").append(new Date(createdAt)).append("</div>");
            html.append("<div><b>Claimed by:</b> ")
                .append(escapeHtml(claimedBy != null ? claimedBy : "None")).append("</div>");
            if (!participants.isEmpty()) {
                html.append("<div><b>Participants:</b> ")
                    .append(escapeHtml(String.join(", ", participants.values()))).append("</div>");
            }
            if (closeReason != null && !closeReason.isEmpty()) {
                html.append("<div><b>Close reason:</b> ").append(escapeHtml(closeReason)).append("</div>");
            }
            html.append("</div>\n<h2>Messages</h2>\n");

            for (TicketMessage msg : messages) {
                html.append("<div class=\"msg\"><span class=\"author\">").append(escapeHtml(msg.author))
                    .append("</span><span class=\"time\">").append(new Date(msg.timestamp)).append("</span>")
                    .append("<pre>").append(escapeHtml(msg.content)).append("</pre></div>\n");
            }

            html.append("</body>\n</html>\n");
            return html.toString();
        }

        private String escapeHtml(String value) {
            if (value == null) return "";
            return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
        }
    }
    
    private static class TicketMessage {
        final String author;
        final String content;
        final long timestamp;
        
        TicketMessage(String author, String content, long timestamp) {
            this.author = author;
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    /**
     * A configurable ticket type shown as a button on the panel.
     */
    public static class TicketType {
        final String id;
        String label;
        String emoji;
        String style;
        String category;
        String supportRole;
        String channelNameFormat;
        String subject;
        String welcomeTitle;
        String welcomeDescription;
        String color;
        String guiMaterial;
        String optionDescription;
        String pingRole;
        String transcriptChannel;
        int autoCloseHours;
        final List<String> questions;

        TicketType(String id, String label) {
            this.id = id;
            this.label = label;
            this.emoji = "";
            this.style = "PRIMARY";
            this.category = "";
            this.supportRole = "";
            this.channelNameFormat = "";
            this.subject = "";
            this.color = null;
            this.guiMaterial = "PAPER";
            this.optionDescription = "";
            this.pingRole = "";
            this.transcriptChannel = "";
            this.autoCloseHours = -1;
            this.questions = new ArrayList<>();
        }
    }

    private static class PanelConfig {
        final String id;
        String channelSpec = "";
        String mode = "buttons";
        String title = "";
        String description = "";
        String color = "";
        String image = "";
        String thumbnail = "";
        String footer = "";
        String selectPlaceholder = "";
        final List<String> typeIds = new ArrayList<>();

        PanelConfig(String id) {
            this.id = id;
        }
    }
}