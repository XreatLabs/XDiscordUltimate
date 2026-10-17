# Messages and Localization

## How the language system works

- Language files live in `plugins/XDiscordUltimate/lang/messages_<locale>.yml`.
- On startup, the plugin extracts every bundled locale into `lang/` if the file does not already exist. Existing files are never overwritten.
- The active server default comes from `general.default-language` in config.yml.
- Every locale file is loaded with `messages_en_US.yml` as its YAML defaults. A key missing from a locale falls back to the en_US value automatically.
- If the file for the selected locale cannot be found at all, the plugin copies and uses en_US and logs a warning. If a locale file is empty, the system also falls back to en_US.
- If a key is missing everywhere, the plugin logs `Missing message: <path>` and displays `Missing message: <path>` in place of the text.

Bundled locales (selectable with `/language`):

| Locale | Language | Locale | Language |
|--------|----------|--------|----------|
| `en_US` | English | `ru_RU` | Russian |
| `de_DE` | German | `zh_CN` | Simplified Chinese |
| `fr_FR` | French | `ja_JP` | Japanese |
| `es_ES` | Spanish | `ko_KR` | Korean |
| `it_IT` | Italian | `ar_SA` | Arabic |
| `pt_BR` | Portuguese (Brazil) | `tr_TR` | Turkish |
| `pl_PL` | Polish | `nl_NL` | Dutch |
| `sv_SE` | Swedish | `cs_CZ` | Czech |
| `vi_VN` | Vietnamese | `th_TH` | Thai |
| `id_ID` | Indonesian | `hi_IN` | Hindi |

## The /language command

| Usage | Effect |
|-------|--------|
| `/language` | Shows your current locale and the list of available locales |
| `/language <locale>` | Sets your personal locale; stored in `player_languages.yml` under `players.<uuid>` |
| `/language <locale>` from console | Sets the server default (`general.default-language`) and saves config.yml |

When a player selects the default locale, their entry is removed from `player_languages.yml`. The command is aliased as `/lang` and `/locale` and requires `xdiscord.language`.

## Message file structure

Top-level sections in `messages_<locale>.yml`:

| Section | Contents |
|---------|----------|
| `general` | Prefix and reload messages |
| `errors` | Permission, verification, cooldown, ticket, filter errors |
| `success` | Success confirmations |
| `info` | Informational messages |
| `verification` | Verification prompts, lockouts, unlink messages |
| `tickets` | Ticket created/closed notifications |
| `support`, `report`, `announcement` | Command-specific messages |
| `chat` | Chat and filter messages |
| `events` | Join/leave/first join messages |
| `stats` | Stat line templates |
| `moderation` | Ban/kick/mute confirmations |
| `help` | Help header/footer and per-command lines |
| `commands` | Descriptions used by command help |
| `discord` | Discord command descriptions |
| `server-control` | Restart/stop notices |

Messages use `%placeholder%` tokens replaced by the plugin at send time, for example `%code%`, `%player%`, `%id%`, `%max%`, `%seconds%`, `%value%`. The message prefix is read from `general.prefix` in the language file.

## Formatting

Text is processed by `ColorUtils.parse`, which supports:

- Legacy color codes with `&`, for example `&a`, `&l`.
- Hex colors as `&#RRGGBB` or `<#RRGGBB>`.
- Gradient tags: `<gradient:#55FFFF:#5555FF>text</gradient>`.
- Rainbow tags: `<rainbow>text</rainbow>`.
- Formatting tags: `<bold>`/`<b>`, `<italic>`/`<i>`/`<em>`, `<underlined>`/`<u>`, `<strikethrough>`/`<st>`.

Discord embeds use hex colors (for example `#55FFFF`) in the relevant config keys.

## Customizing messages

1. Edit the locale file in `plugins/XDiscordUltimate/lang/`. The file is not regenerated after the first extraction.
2. Run `/xdiscord reload` to reload messages.
3. Keep `%placeholder%` tokens intact; removing one simply omits that value.

## Adding a new locale

Bundled locale files are extracted at startup, and only locales registered in the plugin's locale table are selectable with `/language`. To add a language that is not in the list above:

1. Copy `messages_en_US.yml` to `messages_<locale>.yml` and translate the values.
2. Register the locale in the plugin's locale table (`MessageManager.AVAILABLE_LOCALES`) and rebuild, or ask the maintainer to include it in the jar.
3. To make the custom file the server default, set `general.default-language` to that locale.

Editing an existing locale does not require rebuilding. Adding a brand new locale to the selectable list does.
