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
| `support-role` | `"Support"` | Role pinged and allowed to manage tickets. |
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
| `welcome.*` | see config.yml | Embed posted inside a new ticket. |
| `buttons.*` | see config.yml | Labels and emojis for the claim and close buttons. |
| `priorities.LOW/MEDIUM/HIGH/URGENT` | see config.yml | Label, emoji, and color per priority. |
| `max-open-tickets-per-type` | unset | Optional per-type limit on top of `max-open-tickets`. |
| `types` | unset | Optional ticket types, one panel button each (max 5). |

## Commands

In-game:
- `/support` (alias `/ticket`) - opens the ticket GUI. Requires the tickets feature and a verified player. Permission: `xdiscord.support` (default true).

Discord:
- `/ticket create [subject] [priority]`
- `/ticket close [reason]`, `/ticket info`, `/ticket priority <level>`
- `/ticket adduser <user>`, `/ticket removeuser <user>`
- `/ticket edit title|description|priority|addbutton|removebutton`

## Notes

- Open tickets are persisted to `tickets.yml`; the panel message is edited in place after restarts.
- Override embed styling with `messages.embeds.ticket-panel`, `ticket-welcome`, and `ticket-closed`.
- `types` must define one to five entries; without it the panel shows a single default button.
- Transcripts are written to the plugin's `transcripts` folder and uploaded when `save-transcript` is true.
