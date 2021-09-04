package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockSlab;
import cn.nukkit.block.BlockStairs;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.network.protocol.AddEntityPacket;
import cn.nukkit.network.protocol.MoveEntityAbsolutePacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import cn.nukkit.network.protocol.SetEntityLinkPacket;
import net.eltown.servercore.components.language.Language;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ChairListener implements Listener {

    private final Map<String, Long> onChair = new HashMap<>();
    private final Map<String, Long> doubleTap = new HashMap<>();
    private final Map<String, Long> tagblock = new HashMap<>();
    private final Map<String, int[]> posXZ = new HashMap<>();
    private final Map<String, Long> cooldown = new HashMap<>();
    private Map<String, Object> messages;
    private final int[] faces = new int[]{90, 270, 180, 0, 90, 270, 180, 0};

    @EventHandler
    public void on(final PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking() || event.getAction() != PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        String name = player.getName().toLowerCase();
        Block block = event.getBlock();

        if (!this.onChair.containsKey(name)) {
            if (block instanceof BlockStairs || block instanceof BlockSlab) {
                if ((block.getDamage() & 4) != 0 || block.up().isSolid()) return;

                if (!this.doubleTap.containsKey(name)) {
                    this.doubleTap.put(name, System.currentTimeMillis());
                    player.sendActionBar(Language.getNoPrefix("chair.tap"));
                    return;
                }

                if (System.currentTimeMillis() - this.doubleTap.get(name) < 1000) {
                    AddEntityPacket addTagblockPacket = new AddEntityPacket();
                    long eid = Entity.entityCount++;
                    this.tagblock.put(name, eid);
                    addTagblockPacket.entityRuntimeId = eid;
                    addTagblockPacket.entityUniqueId = eid;
                    addTagblockPacket.speedX = 0;
                    addTagblockPacket.speedY = 0;
                    addTagblockPacket.speedZ = 0;
                    addTagblockPacket.pitch = 0;
                    addTagblockPacket.yaw = 0;
                    addTagblockPacket.x = (float) (block.getX() + 0.5);
                    addTagblockPacket.y = (float) (block.getY() + 0.3);
                    addTagblockPacket.z = (float) (block.getZ() + 0.5);
                    addTagblockPacket.type = 84;

                    long flags = 0;
                    flags |= 1 << Entity.DATA_FLAG_CAN_SHOW_NAMETAG;
                    flags |= 1 << Entity.DATA_FLAG_INVISIBLE;
                    flags |= 1 << Entity.DATA_FLAG_NO_AI;
                    flags |= 1 << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG;

                    addTagblockPacket.metadata = new EntityMetadata()
                            .putLong(Entity.DATA_FLAGS, flags)
                            .putShort(Entity.DATA_AIR, 400)
                            .putShort(Entity.DATA_MAX_AIR, 400)
                            .putString(Entity.DATA_NAMETAG, "§7Du sitzt.")
                            .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                            .putFloat(Entity.DATA_SCALE, 0.0001f);

                    MoveEntityAbsolutePacket moveTagblockPacket = new MoveEntityAbsolutePacket();
                    moveTagblockPacket.eid = eid;
                    moveTagblockPacket.x = (float) (block.getX() + 0.5);
                    moveTagblockPacket.y = (float) (block.getY() + 0.7);
                    moveTagblockPacket.z = (float) (block.getZ() + 0.5);

                    AddEntityPacket addEntityPacket = new AddEntityPacket();
                    eid = Entity.entityCount++;
                    this.onChair.put(name, eid);
                    addEntityPacket.entityRuntimeId = eid;
                    addEntityPacket.entityUniqueId = eid;
                    addEntityPacket.speedX = 0;
                    addEntityPacket.speedY = 0;
                    addEntityPacket.speedZ = 0;
                    addEntityPacket.pitch = 0;
                    if (event.getBlock() instanceof BlockSlab) {
                        addEntityPacket.yaw = faces[0];
                    } else addEntityPacket.yaw = faces[event.getBlock().getDamage()];
                    addEntityPacket.x = (float) (block.getX() + 0.5);
                    addEntityPacket.y = (float) (block.getY() + 1.6);
                    addEntityPacket.z = (float) (block.getZ() + 0.5);
                    addEntityPacket.type = 84;

                    flags = 0;
                    flags |= 1 << Entity.DATA_FLAG_CAN_SHOW_NAMETAG;
                    flags |= 1 << Entity.DATA_FLAG_INVISIBLE;
                    flags |= 1 << Entity.DATA_FLAG_NO_AI;
                    flags |= 1 << Entity.DATA_FLAG_ALWAYS_SHOW_NAMETAG;

                    addEntityPacket.metadata = new EntityMetadata()
                            .putLong(Entity.DATA_FLAGS, flags)
                            .putShort(Entity.DATA_AIR, 400)
                            .putShort(Entity.DATA_MAX_AIR, 400)
                            .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                            .putFloat(Entity.DATA_SCALE, 0.0001f);

                    MoveEntityAbsolutePacket moveEntityPacket = new MoveEntityAbsolutePacket();
                    moveEntityPacket.eid = eid;
                    moveEntityPacket.x = (float) (block.getX() + 0.5);
                    moveEntityPacket.y = (float) (block.getY() + 1.6);
                    moveEntityPacket.z = (float) (block.getZ() + 0.5);
                    if (event.getBlock() instanceof BlockSlab) {
                        moveEntityPacket.yaw = faces[0];
                        moveEntityPacket.headYaw = faces[0];
                    } else {
                        moveEntityPacket.yaw = faces[event.getBlock().getDamage()];
                        moveEntityPacket.headYaw = faces[event.getBlock().getDamage()];
                    }
                    moveEntityPacket.pitch = 0;

                    SetEntityLinkPacket setEntityLinkPacket = new SetEntityLinkPacket();
                    setEntityLinkPacket.vehicleUniqueId = eid;
                    setEntityLinkPacket.riderUniqueId = player.getId();
                    setEntityLinkPacket.type = SetEntityLinkPacket.TYPE_PASSENGER;

                    Server.getInstance().getOnlinePlayers().values().forEach((target) -> {
                        target.dataPacket(addEntityPacket);
                        target.dataPacket(moveEntityPacket);
                        target.dataPacket(addTagblockPacket);
                        target.dataPacket(moveTagblockPacket);
                        target.dataPacket(setEntityLinkPacket);
                    });

                    Server.getInstance().getScheduler().scheduleDelayedTask(() -> {
                        this.posXZ.put(name, new int[]{player.getFloorX(), player.getFloorZ()});
                    }, 5);

                    player.setDataFlag(Entity.DATA_FLAGS, Entity.DATA_FLAG_RIDING, true);
                    this.doubleTap.remove(name);
                } else {
                    this.doubleTap.put(name, System.currentTimeMillis());
                    player.sendActionBar(Language.getNoPrefix("chair.tap"));
                }
            }
        } else {
            RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
            removeEntityPacket.eid = this.onChair.remove(name);
            RemoveEntityPacket removeTagblockPacket = new RemoveEntityPacket();
            removeTagblockPacket.eid = this.tagblock.remove(name);
            Server.getInstance().getOnlinePlayers().values().forEach((p) -> {
                p.dataPacket(removeEntityPacket);
                p.dataPacket(removeTagblockPacket);
            });
        }
    }

    @EventHandler
    public void on(final PlayerMoveEvent event) {
        CompletableFuture.runAsync(() -> {
            final Player player = event.getPlayer();
            final String name = event.getPlayer().getName().toLowerCase();

            if (this.onChair.containsKey(name)) {
                if (this.posXZ.containsKey(name)) {

                    final int[] poses = this.posXZ.get(name);

                    if (poses[0] != player.getFloorX() && poses[1] != player.getFloorZ()) {

                        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
                        removeEntityPacket.eid = this.onChair.remove(name);
                        RemoveEntityPacket removeTagblockPacket = new RemoveEntityPacket();
                        removeTagblockPacket.eid = this.tagblock.remove(name);
                        Server.getInstance().getOnlinePlayers().values().forEach((p) -> {
                            p.dataPacket(removeEntityPacket);
                            p.dataPacket(removeTagblockPacket);
                        });

                        this.posXZ.remove(name);
                    }
                }
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        String name = event.getPlayer().getName().toLowerCase();
        if (!this.onChair.containsKey(name)) {
            return;
        }
        RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.eid = this.onChair.remove(name);
        RemoveEntityPacket removeTagblockPacket = new RemoveEntityPacket();
        removeTagblockPacket.eid = this.tagblock.remove(name);
        Server.getInstance().getOnlinePlayers().values().forEach((p) -> {
            p.dataPacket(removeEntityPacket);
            p.dataPacket(removeTagblockPacket);
        });
    }

}
