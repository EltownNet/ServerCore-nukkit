package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.math.Vector3;
import net.eltown.servercore.commands.administrative.SetSpawnProtectionCommand;
import net.eltown.servercore.commands.defaults.SpawnCommand;

public class SpawnProtectionListener implements Listener {

    @EventHandler
    public void on(final PlayerMoveEvent event) {
        if (isInRadius(event.getPlayer()))
            event.getPlayer().sendActionBar("§0Du bist noch im Spawnbereich! §8[§2" + (SetSpawnProtectionCommand.spawnProtection - this.getDistance(event.getPlayer().getPosition())) + " Blöcke§8]");
    }

    @EventHandler
    public void on(final PlayerInteractEvent event) {
        if (isInRadius(event.getPlayer()) && !event.getPlayer().isOp()) event.setCancelled(true);
    }

    @EventHandler
    public void on(final BlockBreakEvent event) {
        if (isInRadius(event.getPlayer()) && !event.getPlayer().isOp()) event.setCancelled(true);
    }

    @EventHandler
    public void on(final BlockPlaceEvent event) {
        if (isInRadius(event.getPlayer()) && !event.getPlayer().isOp()) event.setCancelled(true);
    }

    @EventHandler
    public void on(final EntityDamageEvent event) {
        if (event.getEntity() instanceof Player && isInRadius((Player) event.getEntity()))
            event.setCancelled(true);
    }

    private boolean isInRadius(final Player player) {
        return SetSpawnProtectionCommand.spawnProtection >= this.getDistance(player.getPosition());
    }

    private int getDistance(final Vector3 position) {
        final Vector3 spawn = SpawnCommand.spawnLocation;

        final double dx = Math.max(spawn.x, position.x) - Math.min(spawn.x, position.x);
        final double dz = Math.max(spawn.z, position.z) - Math.min(spawn.z, position.z);

        return (int) Math.sqrt(dx * dx + dz * dz);
    }

}
