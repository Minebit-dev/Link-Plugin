package dev.minebit.link.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.minebit.link.MinebitLink;
import dev.minebit.link.api.MinebitClient;
import dev.minebit.link.api.ResponseReader;
import dev.minebit.link.config.Settings;
import dev.minebit.link.model.Role;
import dev.minebit.link.model.Snapshot;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoleManager {

    private static final int MAX_BODY_BYTES = 262_144;
    private static final int ENDPOINT_CAP = 20;

    private final MinebitLink plugin;
    private final MinebitClient client;
    private final DiscordManager discordManager;
    private final AnnouncementManager announcementManager;

    @Getter
    private volatile Snapshot snapshot = Snapshot.EMPTY;

    private volatile boolean failing;
    private volatile boolean loaded;

    public void reset() {
        snapshot = Snapshot.EMPTY;
        failing = false;
        loaded = false;
    }

    public void refresh() {
        Settings settings = plugin.getSettings();

        if (!settings.configured()) {
            return;
        }

        HttpRequest request = client.request(settings.baseUrl() + "/api/networks/" + settings.slug() + "/jobs")
                .GET()
                .build();

        client.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> handleResponse(settings, response))
                .exceptionally(error -> {
                    logProblem(settings, "Could not reach Minebit (" + ResponseReader.reason(error) + ").", true);
                    return null;
                });
    }

    private void handleResponse(Settings settings, HttpResponse<byte[]> response) {
        if (response.statusCode() == 404) {
            logProblem(settings, "Minebit has no network called '" + settings.slug() + "'. Check Network-Slug in the config.", false);
            return;
        }

        if (response.statusCode() != 200 || response.body().length > MAX_BODY_BYTES) {
            logProblem(settings, "Minebit answered with status " + response.statusCode() + ".", true);
            return;
        }

        Snapshot parsed = parse(settings, new String(response.body(), StandardCharsets.UTF_8));

        if (parsed == null) {
            logProblem(settings, "Minebit sent a reply the plugin could not read.", true);
            return;
        }

        if (plugin.getSettings() != settings) {
            return;
        }

        Snapshot previous = snapshot;
        snapshot = parsed;

        if (loaded) {
            announceChanges(settings, previous, parsed);
        }

        loaded = true;
        failing = false;
    }

    private void announceChanges(Settings settings, Snapshot previous, Snapshot current) {
        Set<String> before = new HashSet<>();

        for (Role role : previous.roles()) {
            before.add(role.url());
        }

        Set<String> after = new HashSet<>();

        for (Role role : current.roles()) {
            after.add(role.url());
        }

        List<Role> opened = new ArrayList<>();

        for (Role role : current.roles()) {
            if (!before.contains(role.url())) {
                opened.add(role);
            }
        }

        List<Role> closed = new ArrayList<>();

        if (current.roles().size() < ENDPOINT_CAP) {
            for (Role role : previous.roles()) {
                if (!after.contains(role.url())) {
                    closed.add(role);
                }
            }
        }

        if (opened.isEmpty() && closed.isEmpty()) {
            return;
        }

        announcementManager.announceNewRoles(settings, opened);
        discordManager.send(settings, current.networkName(), current.networkUrl(), opened, closed);
    }

    private Snapshot parse(Settings settings, String body) {
        try {
            JsonObject root = JsonParser.parseString(body).getAsJsonObject();

            if (!root.has("ok") || !root.get("ok").getAsBoolean()) {
                return null;
            }

            JsonObject network = root.getAsJsonObject("network");
            String name = ResponseReader.clean(ResponseReader.text(network, "name"));
            String networkUrl = ResponseReader.checkedUrl(settings.baseUrl(), ResponseReader.text(network, "url"));

            if (name.isEmpty() || networkUrl.isEmpty()) {
                return null;
            }

            List<Role> roles = new ArrayList<>();
            JsonArray jobs = root.getAsJsonArray("jobs");

            for (JsonElement element : jobs) {
                JsonObject job = element.getAsJsonObject();
                String title = ResponseReader.clean(ResponseReader.text(job, "title"));
                String url = ResponseReader.checkedUrl(settings.baseUrl(), ResponseReader.text(job, "url"));

                if (title.isEmpty() || url.isEmpty()) {
                    continue;
                }

                roles.add(new Role(
                        title,
                        ResponseReader.clean(ResponseReader.text(job, "pay")),
                        ResponseReader.clean(ResponseReader.text(job, "category")),
                        url));
            }

            return new Snapshot(name, networkUrl, List.copyOf(roles));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void logProblem(Settings settings, String message, boolean keepsRoles) {
        if (plugin.getSettings() != settings || failing) {
            return;
        }

        failing = true;
        String keeping = "";

        if (keepsRoles) {
            keeping = " Keeping the last roles it had.";
        }

        plugin.getLogger().warning(message + keeping + " This is only logged once until it recovers.");
    }

}
