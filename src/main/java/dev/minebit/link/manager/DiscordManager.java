package dev.minebit.link.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import dev.minebit.link.MinebitLink;
import dev.minebit.link.api.MinebitClient;
import dev.minebit.link.config.Settings;
import dev.minebit.link.model.Role;
import java.io.File;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DiscordManager {

    private static final int MAX_MESSAGES = 5;
    private static final int MAX_ERROR_LENGTH = 300;
    private static final String TEMPLATE_FOLDER = "discord";
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{([a-z-]+)}");
    private static final Set<String> MARKDOWN_KEYS = Set.of("content", "description", "value");
    private static final Set<String> REMOVED_KEYS = Set.of("actions", "action_set_id");

    private final MinebitLink plugin;
    private final MinebitClient client;
    private final Map<String, JsonObject> templates = new ConcurrentHashMap<>();

    private volatile boolean failing;

    public void loadTemplates(Settings settings) {
        templates.clear();

        for (String name : List.of("new-role.json", "closed-role.json")) {
            File defaultFile = new File(plugin.getDataFolder(), TEMPLATE_FOLDER + File.separator + name);

            if (!defaultFile.exists()) {
                plugin.saveResource(TEMPLATE_FOLDER + "/" + name, false);
            }
        }

        loadTemplate(settings.discordOpenedTemplate());
        loadTemplate(settings.discordClosedTemplate());
    }

    public void send(Settings settings, String networkName, String networkUrl, List<Role> opened, List<Role> closed) {
        if (!settings.hasWebhook()) {
            return;
        }

        int sent = 0;

        for (Role role : opened) {
            if (!settings.discordOpened() || sent >= MAX_MESSAGES) {
                break;
            }

            post(settings, settings.discordOpenedTemplate(), values(networkName, networkUrl, role));
            sent++;
        }

        for (Role role : closed) {
            if (!settings.discordClosed() || sent >= MAX_MESSAGES) {
                break;
            }

            post(settings, settings.discordClosedTemplate(), values(networkName, networkUrl, role));
            sent++;
        }
    }

    private void loadTemplate(String name) {
        if (templates.containsKey(name)) {
            return;
        }

        File file = new File(plugin.getDataFolder(), TEMPLATE_FOLDER + File.separator + name);

        if (!file.exists()) {
            plugin.getLogger().warning("The Discord template " + name + " does not exist in plugins/MinebitLink/discord.");
            return;
        }

        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(file.toPath(), StandardCharsets.UTF_8));

            if (!parsed.isJsonObject()) {
                plugin.getLogger().warning("The Discord template " + name + " must be a JSON object.");
                return;
            }

            templates.put(name, parsed.getAsJsonObject());
        } catch (JsonParseException | IOException e) {
            plugin.getLogger().warning("The Discord template " + name + " could not be read: " + e.getMessage());
        }
    }

    private Map<String, String> values(String networkName, String networkUrl, Role role) {
        Map<String, String> values = new HashMap<>();
        values.put("network", networkName);
        values.put("network-url", networkUrl);
        values.put("title", role.title());
        values.put("pay", role.pay());
        values.put("category", role.category());
        values.put("url", role.url());
        return values;
    }

    private void post(Settings settings, String templateName, Map<String, String> values) {
        JsonObject template = templates.get(templateName);

        if (template == null) {
            return;
        }

        JsonObject body = fill("", template, values).getAsJsonObject();

        JsonObject mentions = new JsonObject();
        mentions.add("parse", new JsonArray());
        body.add("allowed_mentions", mentions);

        HttpRequest request = client.request(settings.discordWebhook() + "?with_components=true")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        client.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() / 100 == 2) {
                        failing = false;
                        return;
                    }

                    logProblem("Discord rejected " + templateName + " with status " + response.statusCode() + ": " + shorten(response.body()));
                })
                .exceptionally(error -> {
                    logProblem("Could not reach Discord.");
                    return null;
                });
    }

    private JsonElement fill(String key, JsonElement element, Map<String, String> values) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return new JsonPrimitive(replace(element.getAsString(), values, MARKDOWN_KEYS.contains(key)));
        }

        if (element.isJsonObject()) {
            JsonObject copy = new JsonObject();

            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (REMOVED_KEYS.contains(entry.getKey())) {
                    continue;
                }

                copy.add(entry.getKey(), fill(entry.getKey(), entry.getValue(), values));
            }

            return copy;
        }

        if (element.isJsonArray()) {
            JsonArray copy = new JsonArray();

            for (JsonElement item : element.getAsJsonArray()) {
                copy.add(fill(key, item, values));
            }

            return copy;
        }

        return element;
    }

    private String replace(String template, Map<String, String> values, boolean escape) {
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String value = values.get(matcher.group(1));

            if (value == null) {
                value = matcher.group();
            } else if (escape) {
                value = value.replaceAll("([\\\\*_~`|>#\\[\\]()-])", "\\\\$1");
            }

            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }

        matcher.appendTail(result);
        return result.toString();
    }

    private String shorten(String text) {
        if (text.length() <= MAX_ERROR_LENGTH) {
            return text;
        }

        return text.substring(0, MAX_ERROR_LENGTH) + "…";
    }

    private void logProblem(String message) {
        if (failing) {
            return;
        }

        failing = true;
        plugin.getLogger().warning(message + " This is only logged once until it recovers.");
    }

}
