package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.inventory.CraftItemEvent;
import cn.nukkit.event.player.PlayerCommandPreprocessEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.Position;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.quests.QuestCalls;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class QuestListener implements Listener {

    private final ServerCore instance;

    private final List<Block> placed = new ArrayList<>();

    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        if (!SpawnProtectionListener.isInRadius(event.getPlayer())) {
            if (!block.getLocation().getLevel().getName().equals("plots")) {
                if (!this.placed.contains(block)) {
                    final QuestPlayer questPlayer = this.instance.getQuestAPI().cachedQuestPlayer.get(player.getName());
                    questPlayer.getQuestPlayerData().forEach(playerData -> this.instance.getQuestAPI().getQuest(playerData.getQuestNameId(), quest -> {
                        if (quest.getData().startsWith("collect")) {
                            final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(quest.getData().split("#")[1]);
                            if (block.getId() == item.getId() && block.getDamage() == item.getDamage()) {
                                this.instance.getQuestAPI().addQuestProgress(player, quest.getNameId(), 1);
                            }
                        }
                    }));
                }
            }
        }
    }

    @EventHandler
    public void on(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (!SpawnProtectionListener.isInRadius(event.getPlayer())) {
            if (!block.getLocation().getLevel().getName().equals("plots")) {
                this.placed.add(event.getBlock());
            }

            final QuestPlayer questPlayer = this.instance.getQuestAPI().cachedQuestPlayer.get(player.getName());
            questPlayer.getQuestPlayerData().forEach(playerData -> this.instance.getQuestAPI().getQuest(playerData.getQuestNameId(), quest -> {
                if (quest.getData().startsWith("place")) {
                    final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(quest.getData().split("#")[1]);
                    if (block.getId() == item.getId() && block.getDamage() == item.getDamage()) {
                        this.instance.getQuestAPI().addQuestProgress(player, quest.getNameId(), 1);
                    }
                }
            }));
        }
    }

    @EventHandler
    public void on(final PlayerMoveEvent event) {
        final Player player = event.getPlayer();
        final QuestPlayer questPlayer = this.instance.getQuestAPI().cachedQuestPlayer.get(player.getName());
        if (questPlayer == null) return;
        questPlayer.getQuestPlayerData().forEach(playerData -> this.instance.getQuestAPI().getQuest(playerData.getQuestNameId(), quest -> {
            if (quest.getData().startsWith("explore")) {
                final String[] d = quest.getData().split("#");
                final String[] d1 = d[1].split(">");
                final String[] d2 = d[2].split(">");
                final Location pos1 = new Location(Double.parseDouble(d1[0]), Double.parseDouble(d1[1]), Double.parseDouble(d1[2]), this.instance.getServer().getLevelByName(d1[3]));
                final Location pos2 = new Location(Double.parseDouble(d2[0]), Double.parseDouble(d2[1]), Double.parseDouble(d2[2]), this.instance.getServer().getLevelByName(d2[3]));

                if (this.isInArea(player.getPosition(), pos1, pos2)) {
                    this.instance.getQuestAPI().addQuestProgress(player, quest.getNameId(), 1);
                }
            }
        }));
    }

    @EventHandler
    public void on(final CraftItemEvent event) {
        final Player player = event.getPlayer();
        final Item output = event.getTransaction().getPrimaryOutput();
        final QuestPlayer questPlayer = this.instance.getQuestAPI().cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(playerData -> this.instance.getQuestAPI().getQuest(playerData.getQuestNameId(), quest -> {
            if (quest.getData().startsWith("craft")) {
                final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(quest.getData().split("#")[1]);
                if (output.getId() == item.getId() && output.getDamage() == item.getDamage()) {
                    this.instance.getQuestAPI().addQuestProgress(player, quest.getNameId(), output.getCount());
                }
            }
        }));
    }

    @EventHandler
    public void on(final PlayerCommandPreprocessEvent event) {
        final Player player = event.getPlayer();
        final String command = event.getMessage().replace("/", "");
        final QuestPlayer questPlayer = this.instance.getQuestAPI().cachedQuestPlayer.get(player.getName());
        questPlayer.getQuestPlayerData().forEach(playerData -> this.instance.getQuestAPI().getQuest(playerData.getQuestNameId(), quest -> {
            if (quest.getData().startsWith("execute")) {
                if (command.equalsIgnoreCase(quest.getData().split("#")[1])) {
                    this.instance.getQuestAPI().addQuestProgress(player, quest.getNameId(), 1);
                }
            }
        }));
    }

    @EventHandler
    public void on(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        this.instance.getQuestAPI().checkIfQuestIsExpired(player.getName());
        this.instance.getQuestAPI().cachedQuestPlayer.get(player.getName()).getQuestPlayerData().forEach(questPlayerData -> {
            this.instance.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_PLAYER_DATA.name(), player.getName(), questPlayerData.getQuestNameId(), String.valueOf(questPlayerData.getCurrent()));
        });
    }

    public boolean isInArea(final Position location, final Location pos1, final Location pos2) {
        if (!location.getLevel().getName().equals(pos1.getLevel().getName())) return false;
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        return location.getX() >= minX && location.getX() <= maxX && location.getY() >= minY && location.getY() <= maxY && location.getZ() >= minZ && location.getZ() <= maxZ;
    }
}
