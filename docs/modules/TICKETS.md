# Tickets

Support ticket system: a panel message in Discord, one private channel per ticket, claiming, priorities, transcripts, and optional ticket types. Players can also open tickets from in-game with `/support`.

## Configuration

Keys under `features.tickets`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `ticket-category` | `"Support Tickets"` | Category where ticket channels are created. |
| `ticket-panel-channel` | `"ticket-logs"` | Channel holding the creation panel. |
| `transcript-channel` | `"ticket-logs"` | Channel where transcripts are uploaded. |
| `support-role` | `"Support"` | Role ID (preferred) or name; pinged and allowed to manage tickets. |
| `max-open-tickets` | `3` | Open tickets allowed per user. |
| `auto-close-hours` | `48` | Close inactive tickets after N hours. `0` disables. |
| `auto-close-warning-enabled` | `true` | Warn before auto-closing. |
| `auto-close-warning-minutes` | `60` | Minutes before closing to send the warning. |
| `player-close` | `true` | Allow creators to close their own tickets. |
| `save-transcript` | `true` | Save and upload transcripts. |
| `close-delete-delay-seconds` | `10` | Delay before deleting a closed channel. `0` keeps it. |
| `creation-cooldown-seconds` | `60` | Cooldown between ticket creations by one user. `0` disables. |
| `feedback-enabled` | `true` | DM a 1-5 rating prompt after closing. |
| `transcript-format` | `text` | `text` or `html`. |
| `channel-name-format` | `"ticket-%id%"` | Ticket channel name format. |
| `channel-topic-format` | topic template | Supports `%id%`, `%user%`, `%minecraft%`, `%subject%`, `%priority%`, `%type%`. |
| `panel.*` | see config.yml | Panel title, description, color, footer, image, thumbnail, and button options. |
| `panel.mode` | `buttons` | `buttons` (one per type, max 5) or `dropdown` (select menu, up to 25 types). |
| `panel.select-placeholder` | `"Select a ticket type"` | Placeholder for dropdown panels. |
| `welcome.*` | see config.yml | Embed posted inside a new ticket. |
| `buttons.*` | see config.yml | Labels and emojis for the claim and close buttons. |
| `priorities.LOW/MEDIUM/HIGH/URGENT` | see config.yml | Label, emoji, and color per priority. |
| `max-open-tickets-per-type` | unset | Optional per-type limit on top of `max-open-tickets`. |
| `types` | unset | Optional ticket types, one panel entry each. |
| `panels` | unset | Optional multiple panels; see below. |

### Ticket types

Each entry under `types` takes the same base keys as before (`label`, `emoji`, `style`, `subject`, `category`, `support-role`, `channel-name-format`, `gui-material`, `welcome-title`, `welcome-description`, `color`) plus:

| Key | Description |
| --- | --- |
| `description` | Option text shown under the type in a dropdown panel. |
| `ping-role` | Extra role pinged when a ticket of this type opens (name or ID). |
| `transcript-channel` | Transcript upload channel override for this type (name or ID). |
| `auto-close-hours` | Auto-close override in hours; `0` uses the global value. |
| `questions` | Up to 5 questions asked in a popup before the ticket is created; answers are added to the welcome embed. |

### Multiple panels

When `panels` is set, each entry posts its own panel message. Supported keys: `channel`, `mode`, `title`, `description`, `color`, `image`, `thumbnail`, `footer`, `select-placeholder`, and `types` (a subset of the configured type ids). Unset keys fall back to the single `panel` section. Panel message ids are stored per panel in `tickets.yml`.

## Commands

In-game:
- `/support` (alias `/ticket`) - opens the ticket GUI. Requires the tickets feature and a verified player. Permission: `xdiscord.support` (default true).

Discord:
- `/ticket create [subject] [priority]`
- `/ticket close [reason]`, `/ticket info`, `/ticket priority <level>`
- `/ticket adduser <user>`, `/ticket removeuser <user>`
- `/ticket edit title|description|priority|addbutton|removebutton`
- `/ticket panel [id]` - reposts one panel from `panels` or all of them. Requires Manage Server.

## Notes

- Open tickets are persisted to `tickets.yml`; panel messages are edited in place after restarts.
- If `tickets.yml` loses an open ticket, the module rebuilds it from the tickets database at startup and then when a ticket command is used in that channel.
- Override embed styling with `messages.embeds.ticket-panel`, `ticket-welcome`, and `ticket-closed`.
- Without `types` the panel shows a single default button; button panels show at most 5 types, dropdown panels at most 25.
- Transcripts are written to the plugin's `transcripts` folder and uploaded when `save-transcript` is true.
