package me.lokka30.levelledmobs.listeners;

import me.lokka30.levelledmobs.LevelledMobs;
import me.lokka30.levelledmobs.misc.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

/**
 * @author lokka30
 */
public class PlayerJoinListener implements Listener {

    private final LevelledMobs main;

    public PlayerJoinListener(final LevelledMobs main) {
        this.main = main;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(final PlayerJoinEvent event) {

        // Player must have permission
        if (!event.getPlayer().hasPermission("levelledmobs.compatibility-notice")) return;

        // There must be possible incompatibilities
        if (main.incompatibilitiesAmount == 0) return;

        // Must be enabled in messages cfg
        if (!main.messagesCfg.getBoolean("other.compatibility-notice.enabled")) return;

        List<String> messages = main.messagesCfg.getStringList("other.compatibility-notice.messages");
        messages = Utils.replaceAllInList(messages, "%prefix%", main.configUtils.getPrefix());
        messages = Utils.replaceAllInList(messages, "%incompatibilities%", main.incompatibilitiesAmount + "");
        messages = Utils.colorizeAllInList(messages);
        messages.forEach(event.getPlayer()::sendMessage);
    }
}
