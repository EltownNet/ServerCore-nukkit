package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;

@RequiredArgsConstructor
public class HologramListener implements Listener {

    private final ServerCore serverCore;

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.serverCore.getHologramAPI().particles.values().forEach(e -> {
            player.getLevel().addParticle(e);
        });
    }

}
