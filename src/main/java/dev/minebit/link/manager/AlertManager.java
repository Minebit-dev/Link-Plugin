package dev.minebit.link.manager;

import dev.minebit.link.MinebitLink;
import dev.minebit.link.message.AlertMessages;
import dev.minebit.link.model.Alert;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class AlertManager {

    private static final int MAX_PENDING = 10;
    private static final String PERMISSION = "minebit.link.alerts";

    private final MinebitLink plugin;
    private final AlertMessages alertMessages;
    private final SoundManager soundManager;
    private final Deque<Alert> pending = new ArrayDeque<>();

    public void deliver(List<Alert> alerts) {
        if (!plugin.isEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> dispatch(alerts));
    }

    public List<Alert> takePending() {
        List<Alert> taken = new ArrayList<>(pending);
        pending.clear();
        return taken;
    }

    public void sendAlerts(Player player, List<Alert> alerts) {
        for (Alert alert : alerts) {
            player.sendMessage(alertMessages.application(alert));
        }

        soundManager.play(player, plugin.getSettings().alertSound());
    }

    private void dispatch(List<Alert> alerts) {
        List<Player> staff = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(PERMISSION)) {
                staff.add(player);
            }
        }

        if (staff.isEmpty()) {
            holdUntilStaffJoin(alerts);
            return;
        }

        for (Player player : staff) {
            sendAlerts(player, alerts);
        }
    }

    private void holdUntilStaffJoin(List<Alert> alerts) {
        for (Alert alert : alerts) {
            if (pending.size() >= MAX_PENDING) {
                pending.removeFirst();
            }

            pending.addLast(alert);
        }
    }

}
