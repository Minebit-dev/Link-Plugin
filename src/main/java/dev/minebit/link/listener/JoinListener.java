package dev.minebit.link.listener;

import dev.minebit.link.MinebitLink;
import dev.minebit.link.config.Settings;
import dev.minebit.link.model.Alert;
import dev.minebit.link.model.Snapshot;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@RequiredArgsConstructor
public class JoinListener implements Listener {

    private static final long ALERT_DELAY_TICKS = 60L;

    private final MinebitLink plugin;

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        UUID id = event.getPlayer().getUniqueId();

        Bukkit.getScheduler().runTaskLater(plugin, () -> sendPendingAlerts(id), ALERT_DELAY_TICKS);

        Settings settings = plugin.getSettings();

        if (!settings.joinMessage() || !settings.configured()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> sendRoles(id), settings.joinDelaySeconds() * 20L);
    }

    private void sendPendingAlerts(UUID id) {
        Player player = Bukkit.getPlayer(id);

        if (player == null || !player.hasPermission("minebit.link.alerts")) {
            return;
        }

        List<Alert> waiting = plugin.getAlertManager().takePending();

        if (waiting.isEmpty()) {
            return;
        }

        plugin.getAlertManager().sendAlerts(player, waiting);
    }

    private void sendRoles(UUID id) {
        Player player = Bukkit.getPlayer(id);

        if (player == null || !player.hasPermission("minebit.link.see")) {
            return;
        }

        Snapshot snapshot = plugin.getRoleManager().getSnapshot();

        if (snapshot.roles().isEmpty()) {
            return;
        }

        plugin.getRoleMessages().roleList(snapshot, plugin.getSettings().maxRoles()).forEach(player::sendMessage);
    }

}
