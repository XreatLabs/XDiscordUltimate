package com.xreatlabs.xdiscordultimate.integrations;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

/**
 * Lightweight Vault economy bridge used for one-off rewards
 * (verification bonus, boost rewards, event prizes).
 */
public final class VaultRewards {

    private static boolean resolved;
    private static Object economy;
    private static Method depositMethod;
    private static Method nameMethod;

    private VaultRewards() {
    }

    public static synchronized boolean isAvailable() {
        if (!resolved) {
            resolve();
        }
        return economy != null;
    }

    public static synchronized boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) return false;
        if (!isAvailable()) return false;
        try {
            depositMethod.invoke(economy, player, amount);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static synchronized String providerName() {
        if (!isAvailable() || nameMethod == null) return "Vault";
        try {
            return String.valueOf(nameMethod.invoke(economy));
        } catch (Exception e) {
            return "Vault";
        }
    }

    private static void resolve() {
        resolved = true;
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return;
        }
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> registration = Bukkit.getServicesManager().getRegistration(economyClass);
            if (registration == null || registration.getProvider() == null) {
                return;
            }
            economy = registration.getProvider();
            depositMethod = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            nameMethod = economyClass.getMethod("getName");
        } catch (Throwable e) {
            economy = null;
        }
    }
}
