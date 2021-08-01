package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.item.ItemID;
import cn.nukkit.math.Vector2;
import cn.nukkit.network.protocol.MovePlayerPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.entities.HumanNPC;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class NpcListener implements Listener {

    @Getter
    private final ServerCore plugin;

    private final Map<String, Long> lastUpdate = new HashMap<>();

    @EventHandler
    public void on(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        if (player.isOp()) {
            if (player.getInventory().getItemInHand().getId() == ItemID.WOODEN_AXE && event.getEntity() instanceof HumanNPC) {
                player.sendMessage("HumanNPC ID: §9" + event.getEntity().getId());
            }
        }
    }

    @EventHandler
    public void on(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof HumanNPC) event.setCancelled(true);
    }

    @EventHandler
    public void on(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();

        if (lastUpdate.containsKey(player.getName())) {
            if (lastUpdate.get(player.getName()) + 300 > System.currentTimeMillis()) {
                return;
            } else lastUpdate.put(player.getName(), System.currentTimeMillis());
        } else lastUpdate.put(player.getName(), System.currentTimeMillis());

        for (Entity e : player.getLevel().getNearbyEntities(player.getBoundingBox().clone().expand(12, 12, 12), player)) {
            // check if entity is a npc that can rotate
            if (!(e instanceof HumanNPC))
                continue;

            // calculate yaw using player position and npc position
            double xdiff = player.x - e.x;
            double zdiff = player.z - e.z;
            double angle = Math.atan2(zdiff, xdiff);
            double yaw = ((angle * 180) / Math.PI) - 90;

            // calculate pitch using player position and npc position
            double ydiff = player.y - e.y;
            Vector2 v = new Vector2(e.x, e.z);
            double dist = v.distance(player.x, player.z);
            angle = Math.atan2(dist, ydiff);
            double pitch = ((angle * 180) / Math.PI) - 90;

            // send the move packets
            // possible to send the move player packet
            MovePlayerPacket pk = new MovePlayerPacket();
            pk.eid = e.getId();
            pk.x = (float) e.x;
            pk.y = (float) e.y + e.getEyeHeight();
            pk.z = (float) e.z;
            pk.yaw = (float) yaw;
            pk.pitch = (float) pitch;
            pk.headYaw = (float) yaw;
            pk.onGround = e.onGround;
            player.dataPacket(pk);
        }
    }

}
