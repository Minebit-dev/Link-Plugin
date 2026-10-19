package dev.minebit.link.manager;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {

    private static final float VOLUME = 1.0f;
    private static final float PITCH = 1.2f;

    public static boolean isValid(String name) {
        return find(name) != null;
    }

    public void play(Player player, String name) {
        if (name.isEmpty()) {
            return;
        }

        Sound sound = find(name);

        if (sound == null) {
            return;
        }

        player.playSound(player, sound, VOLUME, PITCH);
    }

    private static Sound find(String name) {
        NamespacedKey key = NamespacedKey.fromString(name);

        if (key == null) {
            return null;
        }

        return Registry.SOUNDS.get(key);
    }

}
