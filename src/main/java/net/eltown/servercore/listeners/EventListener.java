package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.level.Location;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

@RequiredArgsConstructor
public class EventListener implements Listener {

    private final ServerCore instance;

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        event.setJoinMessage("");

        this.instance.getTinyRabbit().sendAndReceive((data) -> {
            switch (TeleportationCalls.valueOf(data.getKey().toUpperCase())) {
                case CALLBACK_CACHED_DATA:
                    final String[] d = data.getData();
                    if (d[1].startsWith("WARP_NULL==&")) {
                        final String[] p = d[1].split("==&");
                        player.teleport(new Location(Double.parseDouble(d[3]), Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), this.instance.getServer().getLevelByName(d[2])));
                        player.sendMessage(Language.get("warp.teleported", p[1]));
                    } else if (d[1].startsWith("TPA_NULL==&")) {
                        final String[] p = d[1].split("==&");
                        player.teleport(new Location(Double.parseDouble(d[3]), Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), this.instance.getServer().getLevelByName(d[2])));
                        player.sendMessage(Language.get("tpa.teleported", p[1]));
                    } else {
                        player.teleport(new Location(Double.parseDouble(d[3]), Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), this.instance.getServer().getLevelByName(d[2])));
                        player.sendMessage(Language.get("home.teleported", d[1]));
                    }
                    break;
            }
        }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_CACHED_DATA.name(), player.getName());
    }

    @EventHandler
    public void on(final PlayerQuitEvent event) {
        event.setQuitMessage("");
    }

}
