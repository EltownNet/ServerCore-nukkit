package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.inventory.CraftItemEvent;
import cn.nukkit.event.player.*;
import cn.nukkit.level.Location;
import cn.nukkit.permission.PermissionAttachment;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.economy.components.economy.event.MoneyChangeEvent;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.ScoreboardAPI;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.data.teleportation.TeleportationCalls;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.scoreboard.network.DisplayEntry;
import net.eltown.servercore.components.scoreboard.network.DisplaySlot;
import net.eltown.servercore.components.scoreboard.network.ScoreboardDisplay;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.HashMap;

@RequiredArgsConstructor
public class EventListener implements Listener {

    private final ServerCore instance;

    private final HashMap<String, PermissionAttachment> attachments = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerLocallyInitializedEvent event) {
        final Player player = event.getPlayer();
        player.setCheckMovement(false);

        /*
         * Sync
         */
        this.instance.getSyncAPI().loadPlayer(player, loaded -> {
            if (loaded) {
                /*
                 * Teleportation
                 */
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
                            } else if (d[1].startsWith("TP_NULL==&")) {
                                final String[] p = d[1].split("==&");
                                final Player target = this.instance.getServer().getPlayer(p[1]);
                                if (target != null) player.teleport(target.getLocation());
                            } else {
                                player.teleport(new Location(Double.parseDouble(d[3]), Double.parseDouble(d[4]), Double.parseDouble(d[5]), Double.parseDouble(d[6]), Double.parseDouble(d[7]), this.instance.getServer().getLevelByName(d[2])));
                                player.sendMessage(Language.get("home.teleported", d[1]));
                            }
                            break;
                        default:
                            break;
                    }
                }, Queue.TELEPORTATION_CALLBACK, TeleportationCalls.REQUEST_CACHED_DATA.name(), player.getName());

                /*
                 * Groups
                 */
                this.instance.getTinyRabbit().sendAndReceive((delivery -> {
                    switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                        case CALLBACK_FULL_GROUP_PLAYER:
                            final String prefix = delivery.getData()[3];
                            final String[] permissions = delivery.getData()[4].split("#");
                            final String[] aPermissions = delivery.getData()[6].split("#");

                            this.attachments.remove(player.getName());
                            this.attachments.put(player.getName(), player.addAttachment(this.instance));
                            final PermissionAttachment attachment = this.attachments.get(player.getName());

                            for (final String p : permissions) {
                                attachment.setPermission(p, true);
                            }

                            for (final String p : aPermissions) {
                                if (!attachment.getPermissions().containsKey(p)) attachment.setPermission(p, true);
                            }

                            player.setNameTag(prefix.replace("%p", player.getName()));
                            break;
                    }
                }), Queue.GROUPS, GroupCalls.REQUEST_FULL_GROUP_PLAYER.name(), player.getName());

                /*
                 * Level
                 */
                this.instance.getTinyRabbit().sendAndReceive(delivery -> {
                    switch (LevelCalls.valueOf(delivery.getKey().toUpperCase())) {
                        case CALLBACK_LEVEL:
                            this.instance.getLevelAPI().cachedData.put(player.getName(), new Level(
                                    delivery.getData()[1],
                                    Integer.parseInt(delivery.getData()[2]),
                                    Double.parseDouble(delivery.getData()[3])
                            ));
                            player.setScoreTag("§gLevel §l" + Integer.parseInt(delivery.getData()[2]));
                            break;
                    }
                }, Queue.LEVEL_CALLBACK, LevelCalls.REQUEST_GET_LEVEL.name(), player.getName());

                /*
                 * Scoreboard
                 */
                Economy.getAPI().getMoney(player, money -> {
                    final ScoreboardDisplay scoreboard = ScoreboardAPI.createScoreboard().addDisplay(DisplaySlot.SIDEBAR, "eltown", "§2§lEltown.net");
                    scoreboard.addLine("§1", 0);
                    scoreboard.addLine(" §8» §0Bargeld", 1);
                    final DisplayEntry economyEntry = scoreboard.addLine("   §f$" + Economy.getAPI().getMoneyFormat().format(money), 2);
                    scoreboard.addLine("§1§1", 3);
                    scoreboard.addLine(" §8» §0Level", 4);
                    final DisplayEntry levelEntry = scoreboard.addLine("   §f" + this.instance.getLevelAPI().getLevel(player.getName()).getLevel() + " §8[" + this.instance.getLevelAPI().getLevelDisplay(player) + "§8]  ", 5);
                    scoreboard.addLine("§1§1§1", 6);

                    ScoreboardAPI.setScoreboard(player, scoreboard.getScoreboard());
                    ScoreboardAPI.cachedData.put(player.getName(), scoreboard);
                    ScoreboardAPI.cachedDisplayEntries.put(player.getName() + "/economy", economyEntry);
                    ScoreboardAPI.cachedDisplayEntries.put(player.getName() + "/level", levelEntry);
                });
            }
        });
    }

    @EventHandler
    public void on(final MoneyChangeEvent event) {
        final ScoreboardDisplay scoreboardDisplay = ScoreboardAPI.cachedData.get(event.getPlayer().getName());
        final DisplayEntry displayEntry = ScoreboardAPI.cachedDisplayEntries.get(event.getPlayer().getName() + "/economy");
        scoreboardDisplay.removeEntry(displayEntry);

        final DisplayEntry economyEntry = scoreboardDisplay.addLine("   §f$" + Economy.getAPI().getMoneyFormat().format(event.getMoney()), 2);
        ScoreboardAPI.cachedDisplayEntries.put(event.getPlayer().getName() + "/economy", economyEntry);
    }

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        event.setJoinMessage("");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        event.setQuitMessage("");

        if (this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) {
            this.instance.getSyncAPI().savePlayer(event.getPlayer());

            ScoreboardAPI.cachedDisplayEntries.remove(event.getPlayer().getName() + "/economy");
            ScoreboardAPI.cachedDisplayEntries.remove(event.getPlayer().getName() + "/level");

            final Level level = this.instance.getLevelAPI().getLevel(player.getName());
            this.instance.getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_UPDATE_TO_DATABASE.name(),
                    player.getName(), String.valueOf(level.getLevel()), String.valueOf(level.getExperience()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerMoveEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerDropItemEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerCommandPreprocessEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final CraftItemEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerInteractEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerInteractEntityEvent event) {
        if (!this.instance.getSyncAPI().getLoaded().contains(event.getPlayer().getName())) event.setCancelled(true);
    }

}
