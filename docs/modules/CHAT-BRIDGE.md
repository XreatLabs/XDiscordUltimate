# Chat Bridge

Two-way chat relay between Minecraft and Discord. Also posts player join, leave, and death embeds, plus server start/stop messages.

## Configuration

Keys under `features.chat-bridge`:

| Key | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables the module. |
| `chat-channel-id` | `""` | Discord channel ID. Takes priority over the name. |
| `chat-channel` | `"minecraft-chat"` | Channel name used when no ID is configured. |
| `minecraft-to-discord` | `true` | Send Minecraft chat to Discord. |
| `discord-to-minecraft` | `true` | Show Discord messages in Minecraft chat. |
| `filter-bot-messages` | `true` | Ignore other bot messages so relays do not loop. |
| `relay-attachments` | `true` | Relay Discord attachments into Minecraft. |
| `show-attachment-links` | `true` | Include attachment URLs in relayed messages. |
| `skin-provider` | `"crafthead"` | Avatar provider used in player embeds. |
| `auto-create-webhook` | `true` | Create a webhook automatically when needed. |
| `use-webhook` | `false` | Route chat through a webhook instead of the bot. |
| `webhook-url` | `""` | Webhook URL used when `use-webhook` is true. |
| `minecraft-format` | `"**%player%**: %message%"` | Format for Minecraft messages posted to Discord. |
| `discord-format` | `"&7[&9Discord&7] &b%user%&7: &f%message%"` | Format for Discord messages shown in Minecraft. |
| `show-mentions` | `true` | Show Discord mentions in Minecraft chat. |
| `play-mention-sound` | `true` | Play a sound when the player is mentioned. |
| `convert-emojis` | `true` | Convert Discord emojis for Minecraft. |
| `strip-discord-formatting` | `false` | Remove Discord markdown before relaying. |
| `show-typing-indicator` | `true` | Show typing-state hints. |
| `show-player-prefix` | `true` | Include the Minecraft prefix. |
| `show-player-suffix` | `false` | Include the Minecraft suffix. |
| `discord-username-format` | `"%prefix%%player%"` | Name format used in Discord. |

Fallback channel keys: `discord.channels.chat` is used when the feature channel is empty, and `discord.channels.events` is where join, leave, and death embeds are posted (it falls back to the chat channel).

Player event texts come from `messages.join-message`, `messages.leave-message`, and `messages.death-message`.

## Commands

None. Chat and player events are relayed automatically.

## Permissions

None.

## Notes

- Prefer `chat-channel-id` over the channel name; names break when a channel is renamed.
- Join, leave, and death embeds include the player avatar and use Discord shortcodes such as `:arrow_right:` in the default messages.
- `use-webhook` requires a valid `webhook-url`; leave `auto-create-webhook` enabled to let the bot manage one.
- Players do not need a command or prefix to chat; relayed Discord messages appear using `discord-format`.
