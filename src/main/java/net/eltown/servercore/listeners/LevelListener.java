package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerFishEvent;
import cn.nukkit.event.player.PlayerLocallyInitializedEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.level.Level;
import net.eltown.servercore.components.data.level.LevelCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class LevelListener implements Listener {

    private final ServerCore instance;

    @EventHandler
    public void on(final PlayerLocallyInitializedEvent event) {
        final Player player = event.getPlayer();
        this.instance.getTinyRabbit().sendAndReceive(delivery -> {
            switch (LevelCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_LEVEL:
                    this.instance.getLevelAPI().cachedData.put(player.getName(), new Level(
                            delivery.getData()[1],
                            Integer.parseInt(delivery.getData()[2]),
                            Double.parseDouble(delivery.getData()[3])
                    ));
                    break;
            }
        }, Queue.LEVEL_CALLBACK, LevelCalls.REQUEST_GET_LEVEL.name(), player.getName());
    }

    @EventHandler
    public void on(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final Level level = this.instance.getLevelAPI().getLevel(player.getName());
        this.instance.getTinyRabbit().send(Queue.LEVEL_RECEIVE, LevelCalls.REQUEST_UPDATE_TO_DATABASE.name(),
                player.getName(), String.valueOf(level.getLevel()), String.valueOf(level.getExperience()));
    }

    private final List<ExperienceBlock> blocks = new ArrayList<>(Arrays.asList(
            new ExperienceBlock(BlockID.LOG, 0, 0.5),
            new ExperienceBlock(BlockID.LOG, 1, 0.5),
            new ExperienceBlock(BlockID.LOG, 2, 0.5),
            new ExperienceBlock(BlockID.LOG, 3, 0.5),
            new ExperienceBlock(BlockID.LOG2, 0, 0.5),
            new ExperienceBlock(BlockID.LOG2, 1, 0.5),
            new ExperienceBlock(BlockID.COAL_ORE, 0, 0.5),
            new ExperienceBlock(BlockID.IRON_ORE, 0, 0.8),
            new ExperienceBlock(BlockID.LAPIS_ORE, 0, 1.1),
            new ExperienceBlock(BlockID.GLOWING_REDSTONE_ORE, 0, 0.9),
            new ExperienceBlock(BlockID.REDSTONE_ORE, 0, 0.9),
            new ExperienceBlock(BlockID.DIAMOND_ORE, 0, 2.5),
            new ExperienceBlock(BlockID.GOLD_ORE, 0, 1.2),
            new ExperienceBlock(BlockID.EMERALD_ORE, 0, 1.5),
            new ExperienceBlock(BlockID.ANCIENT_DERBRIS, 0, 5),
            new ExperienceBlock(BlockID.GLOWSTONE, 0, 0.5),
            new ExperienceBlock(BlockID.WARPED_STEM, 0, 0.8),
            new ExperienceBlock(BlockID.CRIMSON_STEM, 0, 0.8),
            new ExperienceBlock(BlockID.NETHER_GOLD_ORE, 0, 0.4),
            new ExperienceBlock(BlockID.QUARTZ_ORE, 0, 0.4)
    ));

    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        if (!this.placed.contains(block)) {
            blocks.stream().filter(e -> e.id == block.getId() && e.meta == block.getDamage()).findFirst().ifPresent((experienceBlock) -> {
                this.instance.getLevelAPI().addExperience(event.getPlayer().getName(), experienceBlock.getExperience());
            });
        }
    }

    private final List<Block> placed = new ArrayList<>();

    @EventHandler
    public void on(final BlockPlaceEvent event) {
        if (!event.getBlock().getLocation().getLevel().getName().equals("plots")) {
            this.placed.add(event.getBlock());
        }
    }

    @EventHandler
    public void on(final PlayerFishEvent event) {

    }

    @AllArgsConstructor
    @Getter
    public static class ExperienceBlock {

        private final int id;
        private final int meta;
        private final double experience;

    }

}
