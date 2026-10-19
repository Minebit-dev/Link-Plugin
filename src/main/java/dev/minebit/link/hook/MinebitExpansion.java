package dev.minebit.link.hook;

import dev.minebit.link.MinebitLink;
import dev.minebit.link.model.Snapshot;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class MinebitExpansion extends PlaceholderExpansion {

    private final MinebitLink plugin;

    @Override
    public @NotNull String getIdentifier() {
        return "minebit";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Minebit";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        Snapshot snapshot = plugin.getRoleManager().getSnapshot();
        String id = params.toLowerCase(Locale.ROOT);

        if (id.equals("open_roles")) {
            return String.valueOf(snapshot.roles().size());
        }

        if (id.equals("network")) {
            return snapshot.networkName();
        }

        if (id.equals("top_role")) {
            if (snapshot.roles().isEmpty()) {
                return "";
            }

            return snapshot.roles().getFirst().title();
        }

        return null;
    }

}
