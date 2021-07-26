package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.item.ItemID;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.entities.ModelEntity;

@RequiredArgsConstructor
public class ModelListener implements Listener {

    private final ServerCore serverCore;

    @EventHandler
    public void on(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        if (player.isOp()) {
            if (player.getInventory().getItemInHand().getId() == ItemID.WOODEN_AXE && event.getEntity() instanceof ModelEntity) {
                player.sendMessage("ModelEntity ID: §9" + event.getEntity().getId());
            }
        }
    }

    @EventHandler
    public void on(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ModelEntity) event.setCancelled(true);
    }

    @EventHandler
    public void on(final EntityDamageEvent event) {
        if (event.getEntity() instanceof ModelEntity) event.setCancelled(true);
    }

    @EventHandler
    public void on(final EntityDamageByBlockEvent event) {
        if (event.getEntity() instanceof ModelEntity) event.setCancelled(true);
    }

}
