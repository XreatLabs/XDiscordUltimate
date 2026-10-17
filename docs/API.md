# Developer API

The API is exposed through the plugin instance and a lightweight event bus. No custom repository is required if you compile against the plugin jar.

## Plugin entry point

`com.xreatlabs.xdiscordultimate.XDiscordUltimate` extends `JavaPlugin`.

```java
XDiscordUltimate plugin = XDiscordUltimate.getInstance();
```

### Public methods

| Method | Returns | Description |
|--------|---------|-------------|
| `getInstance()` | `XDiscordUltimate` | Static plugin instance |
| `getConfigManager()` | `ConfigManager` | Config access, feature toggles, admin IDs |
| `getMessageManager()` | `MessageManager` | Locale-aware message lookup and sending |
| `getAdminUtils()` | `AdminUtils` | Verification checks, trusted admin helpers |
| `getEmbedUtils()` | `EmbedUtils` | Embed builders and color parsing |
| `getDatabaseManager()` | `DatabaseManager` | Async linked-account, ticket, and stats data access |
| `getModuleManager()` | `ModuleManager` | Look up modules by key, name, or class |
| `getDiscordManager()` | `DiscordManager` | JDA handle, main guild, channel lookups |
| `parsePlaceholders(String text, Player player)` | `String` | Runs text through PlaceholderAPI if present; otherwise returns it unchanged |
| `parsePlaceholders(String text, OfflinePlayer player)` | `String` | Offline-player variant |
| `isPlaceholderAPIEnabled()` | `boolean` | Whether PlaceholderAPI was detected |
| `isLuckPermsEnabled()` | `boolean` | Whether LuckPerms was detected |
| `getLuckPerms()` | `Object` | LuckPerms API instance, or null |
| `getStartTime()` | `long` | Plugin enable timestamp (millis) |
| `getHelpGUI()` | `HelpGUI` | In-game help GUI |
| `reload()` | `void` | Reloads config, messages, module configs, and activity |

Module access example:

```java
PlayerStatsModule stats = plugin.getModuleManager().getModule(PlayerStatsModule.class);
if (stats != null && stats.isEnabled()) {
    PlayerStatsModule.PlayerStats data = stats.getStats(playerName);
}
```

## Events

Events are dispatched through `XDiscordEventBus`, a reflection-based bus. It is not the Bukkit event system.

| Event | Fired when |
|-------|------------|
| `PlayerVerifiedEvent` | A Minecraft account links to a Discord account |
| `PlayerUnlinkedEvent` | A self-unlink or admin unlink removes a link |

`PlayerVerifiedEvent` getters: `getPlayerUuid()`, `getPlayerName()`, `getDiscordId()`, `getDiscordUsername()`, `getTimestamp()`.

`PlayerUnlinkedEvent` getters: `getPlayerUuid()`, `getDiscordId()`, `getTimestamp()`.

### XDiscordEventBus

| Method | Description |
|--------|-------------|
| `register(Object listener)` | Registers a listener instance (duplicates ignored) |
| `unregister(Object listener)` | Removes a listener |
| `publish(Object event)` | Delivers an event to all listeners |
| `clear()` | Removes all listeners |
| `getListenerCount()` | Number of registered listeners |

A listener is any object with a method that declares exactly one parameter of the event type. The method name does not matter, and exceptions thrown by listeners are swallowed by the bus.

```java
public class VerificationWatcher {

    public void onVerified(PlayerVerifiedEvent event) {
        // event.getPlayerUuid(), event.getDiscordId(), ...
    }

    public void onUnlinked(PlayerUnlinkedEvent event) {
        // clean up
    }
}

XDiscordEventBus.register(new VerificationWatcher());
```

Register after your plugin is enabled and unregister in `onDisable` to avoid stale references.

## PlaceholderAPI expansion

When PlaceholderAPI is installed, the plugin registers the `xdiscord` expansion (it persists across PlaceholderAPI reloads).

Global placeholders (no player required):

| Placeholder | Value |
|-------------|-------|
| `%xdiscord_bot_status%` | `Online` or `Offline` |
| `%xdiscord_guild_name%` | Main guild name, or `N/A` |
| `%xdiscord_guild_members%` | Member count |
| `%xdiscord_guild_online%` | Online member count |

Player placeholders:

| Placeholder | Value |
|-------------|-------|
| `%xdiscord_verified%` | `true` or `false` |
| `%xdiscord_linked%` | `Yes` or `No` |
| `%xdiscord_discord_name%` | Discord username, or `Not Linked` |
| `%xdiscord_discord_id%` | Discord user ID, or `N/A` |
| `%xdiscord_discord_tag%` | Username with discriminator when applicable |
| `%xdiscord_discord_avatar%` | Avatar URL |
| `%xdiscord_has_nitro%` | `true` or `false` |
| `%xdiscord_is_booster%` | `true` or `false` |

Stats placeholders (empty when the player-stats module is disabled):

| Placeholder | Value |
|-------------|-------|
| `%xdiscord_stats_<stat>%` | Current stat value, for example `%xdiscord_stats_kills%` or `%xdiscord_stats_mob_kills%` |
| `%xdiscord_stats_rank-<stat>%` | Rank for that stat, for example `%xdiscord_stats_rank-kills%` |
| `%xdiscord_stats_weekly-<stat>%` | Value gained since the weekly snapshot |
| `%xdiscord_stats_monthly-<stat>%` | Value gained since the monthly snapshot |
| `%xdiscord_stats_top_<stat>_<n>%` | Top list entry at position `n`, formatted value, for example `%xdiscord_stats_top_kills_1%` |
| `%xdiscord_stats_top_<stat>_<n>_name%` | Top list entry at position `n`, player name only |

Stat ids: `kills`, `deaths`, `kd-ratio`, `mob-kills`, `playtime`, `blocks-broken`, `blocks-placed`, `damage-dealt`, `damage-taken`, `distance-walked`, `jumps`, `fish-caught`, `items-crafted`, `advancements`. Underscores in placeholder input are treated as hyphens, so `stats_mob_kills` matches `mob-kills`.

## Database access

`DatabaseManager` returns `CompletableFuture` results for most operations, including `linkAccount`, `getDiscordId`, `getMinecraftUuid`, `isDiscordLinked`, `createTicket`, `closeTicket`, `getOpenTicketsCount`, `createReport`, `logModerationAction`, `unlinkAccount`, and `updateDiscordName`. Synchronous helpers such as `getDiscordIdSync`, `getMinecraftUuidSync`, `isDiscordLinkedSync`, and `getLinkedAccountInfoSync` exist for use inside async contexts. Do not call the synchronous variants from the main server thread.
