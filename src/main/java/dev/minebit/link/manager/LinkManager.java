package dev.minebit.link.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.minebit.link.MinebitLink;
import dev.minebit.link.api.MinebitClient;
import dev.minebit.link.api.ResponseReader;
import dev.minebit.link.config.Settings;
import dev.minebit.link.model.Alert;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LinkManager {

    private static final int MAX_BODY_BYTES = 65_536;
    private static final int EVENT_CAP = 20;

    private final MinebitLink plugin;
    private final MinebitClient client;
    private final AlertManager alertManager;

    @Getter
    private volatile boolean linked;

    private volatile boolean failing;
    private volatile long since;

    public void reset() {
        linked = false;
        failing = false;
        since = 0L;
    }

    public void ping(boolean onlineMode) {
        Settings settings = plugin.getSettings();

        if (!settings.hasKey()) {
            return;
        }

        JsonObject body = new JsonObject();
        body.addProperty("onlineMode", onlineMode);
        body.addProperty("version", plugin.getPluginMeta().getVersion());

        HttpRequest request = authorized(settings, "/api/link/ping")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        client.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> handlePing(settings, response))
                .exceptionally(error -> {
                    logProblem(settings, "Could not reach Minebit to link this server (" + ResponseReader.reason(error) + ").");
                    return null;
                });
    }

    public void pollApplications() {
        Settings settings = plugin.getSettings();

        if (!settings.hasKey() || !settings.staffAlerts()) {
            return;
        }

        String query = "";

        if (since > 0) {
            query = "?since=" + since;
        }

        HttpRequest request = authorized(settings, "/api/link/events" + query).GET().build();

        client.getHttpClient().sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(response -> handleEvents(settings, response))
                .exceptionally(error -> {
                    logProblem(settings, "Could not check Minebit for applications (" + ResponseReader.reason(error) + ").");
                    return null;
                });
    }

    private HttpRequest.Builder authorized(Settings settings, String path) {
        return client.request(settings.baseUrl() + path).header("Authorization", "Bearer " + settings.linkKey());
    }

    private void handlePing(Settings settings, HttpResponse<byte[]> response) {
        if (plugin.getSettings() != settings) {
            return;
        }

        if (response.statusCode() == 401) {
            linked = false;
            logProblem(settings, "Minebit did not accept Link-Key. Make a new key on your company page and update the config.");
            return;
        }

        if (response.statusCode() != 200) {
            logProblem(settings, "Minebit answered the link check with status " + response.statusCode() + ".");
            return;
        }

        linked = true;
        failing = false;
    }

    private void handleEvents(Settings settings, HttpResponse<byte[]> response) {
        if (plugin.getSettings() != settings) {
            return;
        }

        if (response.statusCode() == 401) {
            linked = false;
            logProblem(settings, "Minebit did not accept Link-Key. Make a new key on your company page and update the config.");
            return;
        }

        if (response.statusCode() != 200 || response.body().length > MAX_BODY_BYTES) {
            logProblem(settings, "Minebit answered the applications check with status " + response.statusCode() + ".");
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
            long now = root.get("now").getAsLong();
            long lastAt = 0L;
            List<Alert> alerts = new ArrayList<>();
            JsonArray applications = root.getAsJsonArray("applications");

            for (JsonElement element : applications) {
                JsonObject application = element.getAsJsonObject();
                lastAt = Math.max(lastAt, application.get("at").getAsLong());

                String url = ResponseReader.checkedUrl(settings.baseUrl(), ResponseReader.text(application, "url"));
                String role = ResponseReader.clean(ResponseReader.text(application, "role"));

                if (url.isEmpty() || role.isEmpty()) {
                    continue;
                }

                String applicant = ResponseReader.clean(ResponseReader.text(application, "applicant"));

                if (applicant.isEmpty()) {
                    applicant = "Someone";
                }

                alerts.add(new Alert(applicant, role, url));
            }

            since = now;

            if (applications.size() >= EVENT_CAP) {
                since = lastAt;
            }

            failing = false;

            if (alerts.isEmpty()) {
                return;
            }

            alertManager.deliver(List.copyOf(alerts));
        } catch (RuntimeException e) {
            logProblem(settings, "Minebit sent an applications reply the plugin could not read.");
        }
    }

    private void logProblem(Settings settings, String message) {
        if (plugin.getSettings() != settings || failing) {
            return;
        }

        failing = true;
        plugin.getLogger().warning(message + " This is only logged once until it recovers.");
    }

}
