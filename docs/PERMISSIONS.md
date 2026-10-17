# Permissions

All permission nodes are defined in `plugin.yml`. Defaults use the standard Bukkit values: `true` means everyone, `op` means server operators, `false` means nobody by default.

## Permission table

| Permission | Description | Default |
|------------|-------------|---------|
| `xdiscord.*` | Grants all XDiscordUltimate permissions (wildcard, includes all nodes below) | op |
| `xdiscord.admin` | Administrative commands, including `/xdiscord`, `/welcomedm` | op |
| `xdiscord.announce` | Use `/announce` | op |
| `xdiscord.console` | Use `/discordconsole` | false |
| `xdiscord.embed` | Send custom embeds with `/embed` | op |
| `xdiscord.verify` | Use `/verify` | true |
| `xdiscord.unlink` | Unlink your own Discord account with `/unlink` | true |
| `xdiscord.support` | Create support tickets with `/support` | true |
| `xdiscord.report` | Report players with `/report` | true |
| `xdiscord.bypass.verification` | Bypass the Discord verification requirement | false |
| `xdiscord.ip` | View the server address with `/ip` | true |
| `xdiscord.ip.admin` | Set the server address with `/ip set <address>` | op |
| `xdiscord.whitelist` | Request whitelist access with `/whitelist` | true |
| `xdiscord.bypass.filter` | Bypass the chat filter | false |
| `xdiscord.language` | Change your language preference with `/language` | true |

## Notes

- `xdiscord.*` is defined with children set to `true` for: `xdiscord.admin`, `xdiscord.announce`, `xdiscord.console`, `xdiscord.verify`, `xdiscord.unlink`, `xdiscord.support`, `xdiscord.report`, `xdiscord.embed`, `xdiscord.ip`, `xdiscord.ip.admin`, `xdiscord.whitelist`, `xdiscord.bypass.verification`, `xdiscord.bypass.filter`, and `xdiscord.language`.
- `/stats`, `/discord`, and `/help` do not require permissions in plugin.yml.
- `/report` additionally requires a linked (verified) Discord account when the moderation module is active, regardless of permission.
- `/support` requires a linked Discord account to create a ticket.
- Discord-side permissions are separate: the bot uses Discord roles and the `adminIDs` list, not Bukkit permissions. For example, `/ipset` requires the Discord Manage Channel permission, `/rolemenu post` requires Manage Roles, and bot console commands require a trusted `adminIDs` entry.
- `/ip set` is guarded by `xdiscord.ip.admin`, while the base `ip` command permission covers viewing.

## Granting permissions

Assign nodes with your permissions plugin. For example, with LuckPerms:

```
/lp group moderator permission set xdiscord.report true
/lp group moderator permission set xdiscord.support true
/lp group admin permission set xdiscord.admin true
```

Operators receive every node defaulting to `op` (`xdiscord.*` includes the whole set). Nodes defaulting to `true` are available to all players unless negated by a permissions plugin.
