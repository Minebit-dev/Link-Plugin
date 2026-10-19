package dev.minebit.link.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;
import lombok.Getter;

@Getter
public class MinebitClient {

    private final String userAgent;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public MinebitClient(String version) {
        this.userAgent = "MinebitLink/" + version;
    }

    public HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json");
    }

}
