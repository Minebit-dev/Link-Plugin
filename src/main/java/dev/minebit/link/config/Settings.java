package dev.minebit.link.config;

import dev.minebit.link.manager.SoundManager;
import java.net.URI;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;

public record Settings(
        String slug,
        String baseUrl,
        String linkKey,
        int refreshMinutes,
        int maxRoles,
        boolean announce,
        int announceMinutes,
        boolean announceNewRoles,
        String announceSound,
        boolean joinMessage,
        int joinDelaySeconds,
        boolean staffAlerts,
        int alertPollSeconds,
        String alertSound,
        String discordWebhook,
        boolean discordOpened,
        String discordOpenedTemplate,
        boolean discordClosed,
        String discordClosedTemplate) {

    private static final String DEFAULT_BASE = "https://minebit.dev";
    private static final String DEFAULT_SOUND = "entity.experience_orb.pickup";
    private static final Pattern SLUG = Pattern.compile("[a-z0-9-]{1,80}");
    private static final Pattern TEMPLATE = Pattern.compile("[A-Za-z0-9._-]{1,60}\\.json");
    private static final Pattern KEY =Pattern.compile("^mbl_[A-Za-z0-9_-]{20,60}$");
    private static final Pattern WEBHOOK = Pattern.compile("^https://(?:(?:canary|ptb)\\.)?discord(?:app)?\\.com/api/webhooks/\\d+/[\\w-]+$");

    public static Settings load(FileConfiguration config, Logger logger) {
        String slug = config.getString("Network-Slug", "").trim().toLowerCase(Locale.ROOT);

        if (!slug.isEmpty() && !SLUG.matcher(slug).matches()) {
            logger.warning("Network-Slug may only contain letters, numbers and dashes. Ignoring it.");
            slug = "";
        }

        String baseUrl = config.getString("Base-Url", DEFAULT_BASE).trim().replaceAll("/+$", "");

        if (!validBase(baseUrl)) {
            logger.warning("Base-Url is not a valid http or https address. Using " + DEFAULT_BASE + ".");
            baseUrl = DEFAULT_BASE;
        }

        String linkKey = config.getString("Link-Key", "").trim();

        if (!linkKey.isEmpty() && !KEY.matcher(linkKey).matches()) {
            logger.warning("Link-Key does not look right. Copy it again from your company page on Minebit. Ignoring it.");
            linkKey = "";
        }

        String announceSound = readSound(config, "Announcements.New-Roles-Sound", logger);
        String alertSound = readSound(config, "Staff-Alerts.Sound", logger);

        String discordWebhook = config.getString("Discord.Webhook-Url", "").trim();

        if (!discordWebhook.isEmpty() && !WEBHOOK.matcher(discordWebhook).matches()) {
            logger.warning("Discord.Webhook-Url is not a Discord webhook address. Ignoring it.");
            discordWebhook = "";
        }

        return new Settings(
                slug,
                baseUrl,
                linkKey,
                Math.max(1, config.getInt("Refresh-Minutes", 10)),
                Math.max(1, Math.min(10, config.getInt("Max-Roles-Shown", 5))),
                config.getBoolean("Announcements.Enabled", false),
                Math.max(1, config.getInt("Announcements.Interval-Minutes", 30)),
                config.getBoolean("Announcements.New-Roles", false),
                announceSound,
                config.getBoolean("Join-Message.Enabled", false),
                Math.max(0, config.getInt("Join-Message.Delay-Seconds", 5)),
                config.getBoolean("Staff-Alerts.Enabled", true),
                Math.max(30, config.getInt("Staff-Alerts.Poll-Seconds", 60)),
                alertSound,
                discordWebhook,
                config.getBoolean("Discord.New-Roles.Enabled", true),
                readTemplate(config, "Discord.New-Roles.Template", "new-role.json", logger),
                config.getBoolean("Discord.Closed-Roles.Enabled", false),
                readTemplate(config, "Discord.Closed-Roles.Template", "closed-role.json", logger));
    }

    private static String readTemplate(FileConfiguration config, String path, String fallback, Logger logger) {
        String name = config.getString(path, fallback).trim();

        if (TEMPLATE.matcher(name).matches()) {
            return name;
        }

        logger.warning(path + " must be a .json file name with no folders. Using " + fallback + ".");
        return fallback;
    }

    private static String readSound(FileConfiguration config, String path, Logger logger) {
        String name = config.getString(path, DEFAULT_SOUND).trim().toLowerCase(Locale.ROOT);

        if (name.isEmpty() || SoundManager.isValid(name)) {
            return name;
        }

        logger.warning(path + " is not a Minecraft sound name. Turning the sound off.");
        return "";
    }

    private static boolean validBase(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String scheme = uri.getScheme();

            return uri.getHost() != null && ("https".equals(scheme) || "http".equals(scheme));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public boolean configured() {
        return !slug.isEmpty();
    }

    public boolean hasKey() {
        return !linkKey.isEmpty();
    }

    public boolean hasWebhook() {
        return !discordWebhook.isEmpty();
    }

}
