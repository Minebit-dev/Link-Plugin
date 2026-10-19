package dev.minebit.link.api;

import com.google.gson.JsonObject;
import java.net.URI;

public class ResponseReader {

    private static final int MAX_TEXT = 80;

    private ResponseReader() {
    }

    public static String text(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }

        return object.get(key).getAsString();
    }

    public static String clean(String value) {
        String stripped = value.replaceAll("\\p{C}", "").trim();

        if (stripped.length() <= MAX_TEXT) {
            return stripped;
        }

        return stripped.substring(0, MAX_TEXT - 1) + "…";
    }

    public static String checkedUrl(String baseUrl, String value) {
        try {
            URI uri = URI.create(value);
            URI base = URI.create(baseUrl);

            boolean sameHost = uri.getHost() != null && uri.getHost().equalsIgnoreCase(base.getHost());
            boolean webScheme = "https".equals(uri.getScheme()) || "http".equals(uri.getScheme());

            if (!sameHost || !webScheme) {
                return "";
            }

            return uri.toString();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    public static String reason(Throwable error) {
        Throwable cause = error.getCause();

        if (cause == null) {
            cause = error;
        }

        return cause.getClass().getSimpleName();
    }

}
