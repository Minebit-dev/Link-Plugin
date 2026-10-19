# Minebit Link

A Paper plugin that connects your server to [Minebit](https://minebit.dev). It shows your open roles to your players, tells your staff in game when someone applies, posts new roles to Discord, and marks your network as Server linked.

## What it does

- `/hiring` (also `/jobs`, `/apply` and `/minebit`) lists your open roles. Each line is clickable and opens the role on Minebit.
- An optional message sent to everyone online at an interval, and an optional one a few seconds after a player joins. Both are off by default.
- With a `Link-Key`: your network page shows a Server linked badge, and staff with `minebit.link.alerts` get an in-game message and a sound when someone applies. Alerts that come in while no staff are online are shown to the first staff member who joins.
- With a Discord webhook: new roles (and, if you turn it on, closed roles) are posted to a channel.
- PlaceholderAPI values, if PlaceholderAPI is installed:
  - `%minebit_open_roles%` the number of open roles
  - `%minebit_network%` your network's name
  - `%minebit_top_role%` the title of your newest role

## Privacy

Nothing about your players is sent to Minebit. Without a key the plugin only reads your public roles. With a key it also tells Minebit that it is running, its version and whether the server is in online mode, and it asks for new applications. Minebit sends back only the applicant's display name and the role, never their message, email or profile. The Discord webhook only ever receives public role details.

## Setup

1. Put the jar in `plugins/` and start the server once.
2. Open `plugins/MinebitLink/config.yml` and set `Network-Slug` to the last part of your network address, for example `demo-network` for `https://minebit.dev/networks/demo-network`.
3. Optional: on Minebit go to Dashboard > Company > Minebit Link, make a key, and paste it into `Link-Key`. Keep it private.
4. Optional: paste a Discord webhook address into `Discord.Webhook-Url`. Make one in the channel's settings under Integrations.
5. Run `/hiring reload`, then `/hiring status` to check everything.
6. Give your application handlers the `minebit.link.alerts` permission with your permissions plugin.

## Configuration

`plugins/MinebitLink/config.yml`:

| Key | Default | What it does |
| --- | --- | --- |
| `Network-Slug` | empty | Your network's address on Minebit |
| `Base-Url` | `https://minebit.dev` | Only change this to test against another Minebit site |
| `Link-Key` | empty | The key from your company page |
| `Refresh-Minutes` | `10` | How often roles are checked |
| `Max-Roles-Shown` | `5` | The most roles listed at once, up to 10 |
| `Announcements.Enabled` | `false` | Send the role list to everyone online at an interval |
| `Announcements.Interval-Minutes` | `30` | Minutes between announcements |
| `Announcements.New-Roles` | `false` | Tell everyone online when a new role opens |
| `Announcements.New-Roles-Sound` | `block.note_block.chime` | Sound played with that message, empty for none |
| `Join-Message.Enabled` | `false` | Send the role list to a player after they join |
| `Join-Message.Delay-Seconds` | `5` | Seconds after joining |
| `Staff-Alerts.Enabled` | `true` | Alert staff to new applications, needs `Link-Key` |
| `Staff-Alerts.Poll-Seconds` | `60` | How often applications are checked, at least 30 |
| `Staff-Alerts.Sound` | `entity.experience_orb.pickup` | Sound played with an alert, empty for none |
| `Discord.Webhook-Url` | empty | Where roles are posted |
| `Discord.New-Roles.Enabled` | `true` | Post when a role opens |
| `Discord.New-Roles.Template` | `new-role.json` | The file in `discord/` used for it |
| `Discord.Closed-Roles.Enabled` | `false` | Post when a role closes |
| `Discord.Closed-Roles.Template` | `closed-role.json` | The file in `discord/` used for it |

## Discord messages

The messages posted to Discord are JSON files in `plugins/MinebitLink/discord/`, so they can look however you like. `new-role.json` is posted when a role opens and `closed-role.json` when one closes. They are created the first time the plugin runs and are never overwritten.

To design your own, build the message in [message.style](https://message.style/app/editor/json), copy its JSON, and save it over the file. Anything Discord accepts in a webhook works, including the newer container, section, text, separator and link button components. The default files use them. Use `Discord.New-Roles.Template` and `Discord.Closed-Roles.Template` to point at a different file name, and `Enabled` under each to turn it off.

These placeholders are filled in anywhere in the JSON: `{network}`, `{network-url}`, `{title}`, `{pay}`, `{category}` and `{url}`. In text that Discord formats (`content`, `description` and field `value`), formatting characters inside a role's details are escaped so a role can not change how your message looks. Mentions never ping anyone, and message.style's `actions` entries are removed. At most 5 messages are posted at a time. If Discord rejects a template, the console shows why.

## Messages

Every message, including the hover text, is in `plugins/MinebitLink/messages/en.yml` with MiniMessage formatting. `<smallcaps>` turns text into small caps. `/hiring reload` applies changes to both files.

## Permissions

| Permission | Default | What it allows |
| --- | --- | --- |
| `minebit.link.use` | everyone | Use `/hiring` |
| `minebit.link.see` | everyone | Receive the announcements and the join message |
| `minebit.link.admin` | op | Use `/hiring reload` and `/hiring status` |
| `minebit.link.alerts` | nobody | Get an alert when someone applies. Grant it explicitly, ops do not get it |

## About the Server linked badge

The key proves that whoever runs the server also controls the Minebit account. It does not prove who a server's players are, and it works on offline-mode servers too, which Minebit records. It is separate from the Verified employer badge, which Minebit checks by hand.

## Building

Needs Java 21 and Maven.

```
mvn package
```

The jar is written to `target/minebit-link-1.0.0.jar`.

## The endpoints

- `GET /api/networks/<slug>/jobs` returns the network's name and up to 20 live roles (title, kind, category, platform, pay, link). It is public, cached for a minute, and returns 404 for an unknown or banned network.
- `POST /api/link/ping` and `GET /api/link/events?since=<ms>` need `Authorization: Bearer <link-key>`. The first marks the network as linked, the second returns new applications.
