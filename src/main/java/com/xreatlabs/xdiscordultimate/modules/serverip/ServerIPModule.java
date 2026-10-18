package com.xreatlabs.xdiscordultimate.modules.serverip;

import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import com.xreatlabs.xdiscordultimate.modules.Module;
import com.xreatlabs.xdiscordultimate.utils.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import com.xreatlabs.xdiscordultimate.utils.SchedulerCompat;

public class ServerIPModule extends Module implements CommandExecutor, Listener {

    private static final String PERM_SET = "xdiscord.ip.admin";
    private static final String RENDER_FILE_NAME = "address.png";

    private String currentAddress;

    public ServerIPModule(XDiscordUltimate plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "Server IP";
    }

    @Override
    public String getDescription() {
        return "Share the server address as a rendered image via /ip set <address>";
    }

    @Override
    protected void onEnable() {
        PluginCommand cmd = plugin.getCommand("ip");
        if (cmd != null) {
            cmd.setExecutor(this);
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);

        currentAddress = getConfig().getString("current-address", "");
        info("Server IP module enabled" + (currentAddress.isEmpty() ? "" : " (current: " + currentAddress + ")"));
    }

    @Override
    public List<SlashCommandData> getSlashCommands() {
        return Arrays.asList(
            Commands.slash("ip", "Show the current server address image"),
            Commands.slash("ipset", "Set the server address image (admin only)")
                .addOption(OptionType.STRING, "address", "Server address (e.g. play.example.com)", true)
        );
    }

    @Override
    public boolean handleSlashCommand(SlashCommandInteractionEvent event) {
        String name = event.getName();

        if ("ip".equals(name)) {
            event.deferReply().queue();
            String addr = currentAddress.isEmpty()
                ? plugin.getConfig().getString("server-ip.default-address", "play.example.com")
                : currentAddress;

            SchedulerCompat.runTaskAsync(plugin, () -> {
                File image = generateImage(addr);
                if (image == null) {
                    event.getHook().editOriginalEmbeds(
                        EmbedUtils.errorEmbed("Render Failed",
                            "Could not render the address image. Check the server console for details.").build()
                    ).queue();
                    return;
                }
                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedUtils.BRAND_PRIMARY)
                    .setTitle("Server Address")
                    .setDescription("**" + addr + "**")
                    .setImage("attachment://" + RENDER_FILE_NAME)
                    .setFooter("XDiscordUltimate", null)
                    .setTimestamp(Instant.now());
                event.getHook().editOriginalEmbeds(embed.build())
                    .setFiles(FileUpload.fromData(image, RENDER_FILE_NAME))
                    .queue();
            });
            return true;
        }

        if ("ipset".equals(name)) {
            event.deferReply(true).queue();

            if (!event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_CHANNEL)) {
                event.getHook().editOriginalEmbeds(
                    EmbedUtils.errorEmbed("Permission Denied",
                        "Only administrators can set the server address.").build()
                ).queue();
                return true;
            }

            String address = event.getOption("address").getAsString().trim();
            if (address.isEmpty()) {
                event.getHook().editOriginalEmbeds(
                    EmbedUtils.errorEmbed("Invalid Address", "Please provide a server address.").build()
                ).queue();
                return true;
            }

            String callerName = event.getUser().getName();
            info("Server IP set (Discord): " + address + " (by " + callerName + ")");

            SchedulerCompat.runTaskAsync(plugin, () -> {
                File image = generateImage(address);
                if (image == null) {
                    event.getHook().editOriginalEmbeds(
                        EmbedUtils.errorEmbed("Render Failed",
                            "Could not render the address image. Check the server console for details.").build()
                    ).queue();
                    return;
                }

                saveAndBroadcast(address, image, callerName);

                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedUtils.SUCCESS)
                    .setTitle("Server Address Updated")
                    .setDescription("Now showing: **" + address + "**")
                    .setImage("attachment://" + RENDER_FILE_NAME)
                    .setFooter("XDiscordUltimate", null)
                    .setTimestamp(Instant.now());
                event.getHook().editOriginalEmbeds(embed.build())
                    .setFiles(FileUpload.fromData(image, RENDER_FILE_NAME))
                    .queue();
            });
            return true;
        }

        return false;
    }

    @Override
    public boolean handleTextCommand(MessageReceivedEvent event, String command, String args) {
        if (!"ip".equals(command)) {
            return false;
        }

        String trimmed = args.trim();

        // /ip (no args) - show current
        if (trimmed.isEmpty()) {
            String addr = currentAddress.isEmpty()
                ? plugin.getConfig().getString("server-ip.default-address", "play.example.com")
                : currentAddress;

            SchedulerCompat.runTaskAsync(plugin, () -> {
                File image = generateImage(addr);
                if (image == null) {
                    event.getMessage().reply("Could not render the address image. Check the server console for details.").queue();
                    return;
                }
                EmbedBuilder embed = new EmbedBuilder()
                    .setColor(EmbedUtils.BRAND_PRIMARY)
                    .setTitle("Server Address")
                    .setDescription("**" + addr + "**")
                    .setImage("attachment://" + RENDER_FILE_NAME)
                    .setFooter("XDiscordUltimate", null)
                    .setTimestamp(Instant.now());
                event.getMessage().replyEmbeds(embed.build())
                    .addFiles(FileUpload.fromData(image, RENDER_FILE_NAME))
                    .queue();
            });
            return true;
        }

        // /ip set <address> - admin only
        String[] parts = trimmed.split("\\s+", 2);
        if (parts[0].equalsIgnoreCase("set")) {
            if (parts.length < 2) {
                event.getMessage().reply("Usage: `!ip set <address>`").queue();
                return true;
            }
            if (event.getMember() != null && !event.getMember().hasPermission(net.dv8tion.jda.api.Permission.MANAGE_CHANNEL)) {
                event.getMessage().reply("Only administrators can set the server address.").queue();
                return true;
            }

            String address = parts[1].trim();
            String callerName = event.getAuthor().getName();
            info("Server IP set (Discord text): " + address + " (by " + callerName + ")");

            SchedulerCompat.runTaskAsync(plugin, () -> {
                File image = generateImage(address);
                if (image == null) {
                    event.getMessage().reply("Could not render the address image.").queue();
                    return;
                }
                saveAndBroadcast(address, image, callerName);
                event.getMessage().reply("Server address updated to **" + address + "**").queue();
            });
            return true;
        }

        event.getMessage().reply("Usage: `!ip` to view, `!ip set <address>` to change (admin)").queue();
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            // Show current IP
            String addr = currentAddress.isEmpty()
                ? plugin.getConfig().getString("server-ip.default-address", "play.example.com")
                : currentAddress;
            if (sender != null) {
                sender.sendMessage(plugin.getMessageManager().colorize(
                    "&bServer Address: &f" + addr));
                sender.sendMessage(plugin.getMessageManager().colorize(
                    "&7Use &e/ip set <address> &7to change (admin only)"));
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                if (sender != null) {
                    sender.sendMessage(plugin.getMessageManager().colorize("&eUsage: &f/ip set <address>"));
                }
                return true;
            }

            // Permission check
            if (sender instanceof Player && !sender.hasPermission(PERM_SET)) {
                sender.sendMessage(plugin.getMessageManager().colorize(
                    "&cYou don't have permission to change the server address."));
                return true;
            }

            String address = args[1].trim();
            Player player = sender instanceof Player ? (Player) sender : null;
            String callerName = player != null ? player.getName() : "Console";

            info("Server IP set: " + address + " (by " + callerName + ")");

            final Player finalPlayer = player;
            SchedulerCompat.runTaskAsync(plugin, () -> {
                File image = generateImage(address);
                if (image == null) {
                    Runnable fail = () -> {
                        String msg = plugin.getMessageManager().colorize(
                            "&cCould not render the address image. Check the console for details.");
                        if (finalPlayer != null) finalPlayer.sendMessage(msg);
                        else if (sender != null) sender.sendMessage(msg);
                    };
                    if (finalPlayer != null) SchedulerCompat.runTask(plugin, fail);
                    else fail.run();
                    return;
                }

                saveAndBroadcast(address, image, callerName);

                Runnable ok = () -> {
                    String msg = plugin.getMessageManager().colorize(
                        "&aServer address updated to &f" + address + "&a! Check Discord.");
                    if (finalPlayer != null) finalPlayer.sendMessage(msg);
                    else if (sender != null) sender.sendMessage(msg);
                };
                if (finalPlayer != null) SchedulerCompat.runTask(plugin, ok);
                else ok.run();
            });
            return true;
        }

        if (sender != null) {
            sender.sendMessage(plugin.getMessageManager().colorize(
                "&eUsage: &f/ip &7- view address | &f/ip set <address> &7- change (admin)"));
        }
        return true;
    }

    private void saveAndBroadcast(String address, File image, String callerName) {
        currentAddress = address;
        getConfig().set("current-address", address);
        plugin.saveConfig();
        sendImageToDiscord(address, image, callerName);
    }

    private File generateImage(String address) {
        try {
            File dir = new File(plugin.getDataFolder(), "renders");
            if (!dir.exists() && !dir.mkdirs()) {
                warning("Could not create the renders folder");
                return null;
            }
            BufferedImage image = renderAddressCard(address);
            if (image == null) return null;
            File out = new File(dir, RENDER_FILE_NAME);
            javax.imageio.ImageIO.write(image, "png", out);
            return out;
        } catch (Exception e) {
            warning("Failed to render the address image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Draw the server address card locally so no external service is needed.
     */
    private BufferedImage renderAddressCard(String address) {
        int width = 1200;
        int height = 400;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Background: subtle diagonal gradient in Discord tones
            g.setPaint(new GradientPaint(0, 0, new Color(30, 31, 34), width, height, new Color(43, 45, 49)));
            g.fillRect(0, 0, width, height);

            // Blurple accent bar on the left edge
            g.setColor(new Color(88, 101, 242));
            g.fillRect(0, 0, 12, height);

            // Soft inner outline
            g.setColor(new Color(60, 62, 68));
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(24, 24, width - 49, height - 49, 24, 24);

            // Small uppercase label
            g.setFont(new Font("SansSerif", Font.BOLD, 28));
            g.setColor(new Color(148, 155, 164));
            String label = "SERVER ADDRESS";
            FontMetrics labelMetrics = g.getFontMetrics();
            g.drawString(label, (width - labelMetrics.stringWidth(label)) / 2, 140);

            // Address, shrunk to fit if needed
            g.setFont(new Font("SansSerif", Font.BOLD, 72));
            g.setColor(Color.WHITE);
            FontMetrics addressMetrics = g.getFontMetrics();
            String shown = address == null ? "" : address;
            boolean clipped = false;
            while (addressMetrics.stringWidth(shown) > width - 160 && shown.length() > 8) {
                shown = shown.substring(0, shown.length() - 2);
                clipped = true;
            }
            if (clipped) shown = shown + "...";
            g.drawString(shown, (width - addressMetrics.stringWidth(shown)) / 2, 232);

            // Underline accent below the address
            g.setColor(new Color(88, 101, 242));
            int underlineWidth = Math.max(140, addressMetrics.stringWidth(shown) + 90);
            int underlineX = (width - underlineWidth) / 2;
            g.fillRoundRect(underlineX, 262, underlineWidth, 8, 8, 8);

            // Footer hint
            g.setFont(new Font("SansSerif", Font.PLAIN, 24));
            g.setColor(new Color(110, 117, 125));
            String footer = "Copy the address and join the server";
            FontMetrics footerMetrics = g.getFontMetrics();
            g.drawString(footer, (width - footerMetrics.stringWidth(footer)) / 2, 330);
        } finally {
            g.dispose();
        }
        return image;
    }

    private void sendImageToDiscord(String address, File image, String callerName) {
        if (plugin.getDiscordManager() == null || !plugin.getDiscordManager().isReady()) {
            warning("Discord not ready, cannot send IP image for " + address);
            return;
        }
        if (image == null || !image.exists()) {
            warning("No rendered image available for " + address);
            return;
        }

        TextChannel channel = null;

        String configured = getConfig().getString("image-channel", "");
        if (configured != null && !configured.isEmpty()) {
            channel = plugin.getDiscordManager().getMainGuild()
                .getTextChannelsByName(configured, true)
                .stream().findFirst().orElse(null);
        }

        if (channel == null) {
            String channelId = plugin.getConfig().getString("discord.channels.general", "");
            if (!channelId.isEmpty() && channelId.matches("\\d+")) {
                channel = plugin.getDiscordManager().getTextChannelById(channelId);
            }
        }
        if (channel == null) {
            channel = plugin.getDiscordManager().getMainGuild()
                    .getTextChannelsByName("general", true)
                    .stream().findFirst().orElse(null);
        }
        if (channel == null) {
            warning("No channel found to send IP image");
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
            .setColor(EmbedUtils.BRAND_PRIMARY)
            .setTitle("Server Address")
            .setDescription("Requested by **" + callerName + "**\nAddress: `" + address + "`")
            .setImage("attachment://" + RENDER_FILE_NAME)
            .setFooter("XDiscordUltimate", null)
            .setTimestamp(Instant.now());

        channel.sendMessageEmbeds(embed.build())
            .addFiles(FileUpload.fromData(image, RENDER_FILE_NAME))
            .queue(
                success -> info("IP image sent to Discord: " + address),
                error -> warning("Failed to send IP image: " + error.getMessage())
            );
    }

    @Override
    protected void onDisable() {
        info("Server IP module disabled");
    }
}
