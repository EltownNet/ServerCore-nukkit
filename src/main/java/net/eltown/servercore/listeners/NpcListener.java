package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.math.Vector2;
import cn.nukkit.network.protocol.MovePlayerPacket;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.entities.HumanNPC;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.language.Language;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
public class NpcListener implements Listener {

    @Getter
    private final ServerCore plugin;

    @EventHandler
    public void on(final PlayerInteractEntityEvent event) {
        final Player player = event.getPlayer();
        if (this.getPlugin().getNpcHandler().getManagers().contains(player.getName())) {
            if (event.getEntity() instanceof HumanNPC) {

                final HumanNPC npc = (HumanNPC) event.getEntity();
                this.getPlugin().getNpcHandler().getManagers().remove(player.getName());

                new CustomForm.Builder("§8» §fNPC bearbeiten")
                        .addElement(new ElementInput("NPC ID", npc.getNpcID(), npc.getNpcID()))
                        .addElement(new ElementInput("NPC Name", npc.getNameTag(), npc.getNameTag()))
                        .addElement(new ElementToggle("NPC löschen?", false))
                        .onSubmit((p, f) -> {
                            final boolean delete = f.getToggleResponse(2);
                            if (delete) {
                                npc.close();
                                return;
                            }

                            final String id = f.getInputResponse(0);
                            final String name = f.getInputResponse(1);

                            if (id.isEmpty()) {
                                p.sendMessage(Language.get("npc.missing.id"));
                                return;
                            }

                            if (name.isEmpty()) {
                                p.sendMessage(Language.get("npc.missing.name"));
                                return;
                            }

                            npc.setID(id);
                            npc.setNameTag(name);

                            p.sendMessage(Language.get("npc.updated"));
                        }).build().send(player);

            } else player.sendMessage(Language.get("npc.no"));

        }
    }

    @EventHandler
    public void on(final EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof HumanNPC) event.setCancelled(true);
    }

    @EventHandler
    public void on(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();

        for (Entity e : player.getLevel().getNearbyEntities(player.getBoundingBox().clone().expand(16, 16, 16), player)) {
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
