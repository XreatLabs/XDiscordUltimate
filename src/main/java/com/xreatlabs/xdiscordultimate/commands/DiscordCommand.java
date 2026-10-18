package com.xreatlabs.xdiscordultimate.commands;

import com.xreatlabs.xdiscordultimate.XDiscordUltimate;
import com.xreatlabs.xdiscordultimate.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapPalette;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Opens a small menu with the server Discord logo. Clicking the logo opens
 * the invite. Also used for /invite and /disc.
 */
public class DiscordCommand implements CommandExecutor, Listener {

    private static final int MENU_SIZE = 27;
    private static final int LOGO_SLOT = 13;
    private static final int LOGO_SIZE = 128;
    private static final int BRAND_RED = 88;
    private static final int BRAND_GREEN = 101;
    private static final int BRAND_BLUE = 242;

    private final XDiscordUltimate plugin;
    private final byte[] logoPixels = new byte[LOGO_SIZE * LOGO_SIZE];
    private MapView logoView;

    public DiscordCommand(XDiscordUltimate plugin) {
        this.plugin = plugin;
        loadLogoPixels();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String inviteUrl = plugin.getConfig().getString("discord.invite-url", "");
        String guildName = plugin.getConfig().getString("discord.guild-name", "Discord Server");

        if (inviteUrl == null || inviteUrl.isEmpty() || !inviteUrl.startsWith("http")) {
            sender.sendMessage(ColorUtils.parse("<gradient:#FF5555:#FFAA00>Discord invite not configured!</gradient> &7Ask an admin to set &ediscord.invite-url&7 in config.yml."));
            return true;
        }

        if (sender instanceof Player) {
            openMenu((Player) sender, inviteUrl, guildName);
            return true;
        }

        sender.sendMessage(ColorUtils.parse("<gradient:#5865F2:#8B5CF6>━━━ Discord ━━━</gradient>"));
        sender.sendMessage(ColorUtils.parse("&7Join our Discord: &f" + guildName));
        sender.sendMessage(ColorUtils.parse("&e" + inviteUrl));
        return true;
    }

    private void openMenu(Player player, String inviteUrl, String guildName) {
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, MENU_SIZE, ColorUtils.parse("<#5865F2>Discord"));
        holder.setInventory(inventory);

        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int slot = 0; slot < MENU_SIZE; slot++) {
            inventory.setItem(slot, filler);
        }

        ItemStack logo = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) logo.getItemMeta();
        if (mapMeta != null) {
            mapMeta.setMapView(getLogoView(player));
            mapMeta.setDisplayName(ColorUtils.parse("&f&l" + guildName));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.parse("&7Click the logo to join our Discord"));
            lore.add(ChatColor.WHITE + inviteUrl);
            mapMeta.setLore(lore);
            logo.setItemMeta(mapMeta);
        }
        inventory.setItem(LOGO_SLOT, logo);

        player.sendMap(getLogoView(player));
        player.openInventory(inventory);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    /**
     * Build a single shared map view that draws the bundled Discord logo.
     */
    private MapView getLogoView(Player player) {
        if (logoView != null) return logoView;
        MapView view = Bukkit.createMap(player.getWorld());
        view.setScale(MapView.Scale.CLOSEST);
        view.setTrackingPosition(false);
        for (MapRenderer renderer : new ArrayList<>(view.getRenderers())) {
            view.removeRenderer(renderer);
        }
        view.addRenderer(new MapRenderer() {
            @Override
            public void render(MapView mapView, MapCanvas canvas, Player renderPlayer) {
                for (int y = 0; y < LOGO_SIZE; y++) {
                    for (int x = 0; x < LOGO_SIZE; x++) {
                        canvas.setPixel(x, y, logoPixels[y * LOGO_SIZE + x]);
                    }
                }
            }
        });
        logoView = view;
        return view;
    }

    /**
     * Convert the bundled PNG into map palette bytes. Falls back to a solid
     * brand colored square when the resource is missing.
     */
    private void loadLogoPixels() {
        Arrays.fill(logoPixels, MapPalette.matchColor(BRAND_RED, BRAND_GREEN, BRAND_BLUE));
        try (InputStream in = plugin.getResource("discord-logo.png")) {
            if (in == null) return;
            BufferedImage image = ImageIO.read(in);
            if (image == null) return;
            int width = image.getWidth();
            int height = image.getHeight();
            for (int y = 0; y < LOGO_SIZE; y++) {
                for (int x = 0; x < LOGO_SIZE; x++) {
                    int argb = image.getRGB(x * width / LOGO_SIZE, y * height / LOGO_SIZE);
                    int alpha = (argb >> 24) & 0xFF;
                    int red = (argb >> 16) & 0xFF;
                    int green = (argb >> 8) & 0xFF;
                    int blue = argb & 0xFF;
                    if (alpha < 255) {
                        red = (red * alpha + BRAND_RED * (255 - alpha)) / 255;
                        green = (green * alpha + BRAND_GREEN * (255 - alpha)) / 255;
                        blue = (blue * alpha + BRAND_BLUE * (255 - alpha)) / 255;
                    }
                    logoPixels[y * LOGO_SIZE + x] = MapPalette.matchColor(red, green, blue);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not load discord-logo.png: " + e.getMessage());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof MenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.FILLED_MAP) return;

        Player player = (Player) event.getWhoClicked();
        player.closeInventory();
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.4f);
        plugin.getMessageManager().sendClickableLink(player, "&7Click to join our Discord", plugin.getConfig().getString("discord.invite-url", ""));
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private static class MenuHolder implements InventoryHolder {

        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }
}
