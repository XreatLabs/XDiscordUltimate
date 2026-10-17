package com.xreatlabs.xdiscordultimate.commands;

import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import com.xreatlabs.xdiscordultimate.modules.stats.PlayerStatsModule;
import com.xreatlabs.xdiscordultimate.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class StatsCommand implements CommandExecutor, TabCompleter {

    private final XDiscordUltimate plugin;

    public StatsCommand(XDiscordUltimate plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        PlayerStatsModule statsModule = plugin.getModuleManager().getModule(PlayerStatsModule.class);

        if (statsModule == null || !statsModule.isEnabled()) {
            sender.sendMessage(ColorUtils.parse("<gradient:#FF5555:#AA0000>Error!</gradient> &7Stats module is not enabled!"));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("compare")) {
            handleCompare(sender, statsModule, args);
            return true;
        }

        String targetName;

        if (args.length > 0) {
            targetName = args[0];
        } else if (sender instanceof Player) {
            targetName = sender.getName();
        } else {
            sender.sendMessage(ColorUtils.parse("<gradient:#FF5555:#FFAA00>Usage:</gradient> &e/stats <player> &7or &e/stats compare <player>"));
            return true;
        }

        PlayerStatsModule.PlayerStats stats = statsModule.getStats(targetName);
        Player viewer = sender instanceof Player ? (Player) sender : null;
        String message = statsModule.formatStatsMessage(stats, viewer);
        sender.sendMessage(message);

        return true;
    }

    private void handleCompare(CommandSender sender, PlayerStatsModule statsModule, String[] args) {
        PlayerStatsModule.PlayerStats first;
        PlayerStatsModule.PlayerStats second;

        if (args.length >= 3) {
            first = statsModule.getStats(args[1]);
            second = statsModule.getStats(args[2]);
        } else if (args.length == 2 && sender instanceof Player) {
            first = statsModule.getStats(sender.getName());
            second = statsModule.getStats(args[1]);
        } else {
            sender.sendMessage(ColorUtils.parse("<gradient:#FF5555:#FFAA00>Usage:</gradient> &e/stats compare <player> &7or &e/stats compare <player1> <player2>"));
            return;
        }

        Player viewer = sender instanceof Player ? (Player) sender : null;
        if (first == null || second == null) {
            sender.sendMessage(String.join("\n", statsModule.buildStatsLines(null, viewer)));
            return;
        }

        sender.sendMessage(String.join("\n", statsModule.buildCompareLines(first, second, viewer)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            if ("compare".startsWith(args[0].toLowerCase(Locale.ROOT))) {
                options.add("compare");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    options.add(online.getName());
                }
            }
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("compare")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    options.add(online.getName());
                }
            }
            return options;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("compare")) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT))) {
                    options.add(online.getName());
                }
            }
            return options;
        }
        return Collections.emptyList();
    }
}
