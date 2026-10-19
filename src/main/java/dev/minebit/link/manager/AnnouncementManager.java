package dev.minebit.link.manager;

import dev.minebit.link.MinebitLink;
import dev.minebit.link.config.Settings;
import dev.minebit.link.message.RoleMessages;
import dev.minebit.link.model.Role;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public class AnnouncementManager {

    private static final String PERMISSION = "minebit.link.see";

    private final MinebitLink plugin;
    private final RoleMessages roleMessages;
    private final SoundManager soundManager;

    public void announceRoles() {
        if (plugin.getRoleManager().getSnapshot().roles().isEmpty()) {
            return;
        }

        List<Component> lines = roleMessages.roleList(plugin.getRoleManager().getSnapshot(), plugin.getSettings().maxRoles());

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(PERMISSION)) {
                lines.forEach(player::sendMessage);
            }
        }
    }

    public void announceNewRoles(Settings settings, List<Role> opened) {
        if (!settings.announceNewRoles() || !plugin.isEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> broadcastNewRoles(settings, opened));
    }

    private void broadcastNewRoles(Settings settings, List<Role> opened) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission(PERMISSION)) {
                continue;
            }

            for (Role role : opened) {
                player.sendMessage(roleMessages.newRole(role));
            }

            soundManager.play(player, settings.announceSound());
        }
    }

}
