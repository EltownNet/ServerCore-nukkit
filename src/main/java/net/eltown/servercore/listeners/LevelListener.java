package net.eltown.servercore.listeners;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.player.PlayerFishEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class LevelListener implements Listener {

    private final ServerCore instance;

    private final List<ExperienceBlock> blocks = new ArrayList<>(Arrays.asList(
            new ExperienceBlock(BlockID.LOG, 0, 1.5),
            new ExperienceBlock(BlockID.LOG, 1, 1.5),
            new ExperienceBlock(BlockID.LOG, 2, 1.5),
            new ExperienceBlock(BlockID.LOG, 3, 1.5),
            new ExperienceBlock(BlockID.LOG2, 0, 1.5),
            new ExperienceBlock(BlockID.LOG2, 1, 1.5),
            new ExperienceBlock(BlockID.COAL_ORE, 0, 1.7),
            new ExperienceBlock(BlockID.IRON_ORE, 0, 3.5),
            new ExperienceBlock(BlockID.LAPIS_ORE, 0, 3.2),
            new ExperienceBlock(BlockID.GLOWING_REDSTONE_ORE, 0, 3.2),
            new ExperienceBlock(BlockID.REDSTONE_ORE, 0, 3.2),
            new ExperienceBlock(BlockID.DIAMOND_ORE, 0, 8.5),
            new ExperienceBlock(BlockID.GOLD_ORE, 0, 4.3),
            new ExperienceBlock(BlockID.ANCIENT_DERBRIS, 0, 20),
            new ExperienceBlock(BlockID.GLOWSTONE, 0, 1.9),
            new ExperienceBlock(BlockID.WARPED_STEM, 0, 1.5),
            new ExperienceBlock(BlockID.CRIMSON_STEM, 0, 1.5),
            new ExperienceBlock(BlockID.NETHER_GOLD_ORE, 0, 3.2),
            new ExperienceBlock(BlockID.QUARTZ_ORE, 0, 3.5)
    ));

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final BlockBreakEvent event) {
        final Block block = event.getBlock();
        final Item item = event.getPlayer().getInventory().getItemInHand();
        if (!SpawnProtectionListener.isInRadius(event.getPlayer()) && !SpawnProtectionListener.isInRadius(block.getLocation())) {
            if (!event.getBlock().getLocation().getLevel().getName().equals("plots")) {
                if (!this.placed.contains(block)) {
                    this.blocks.stream().filter(e -> e.id == block.getId() && e.meta == block.getDamage()).findFirst().ifPresent((experienceBlock) -> {
                        if (item.hasEnchantment(CustomEnchantment.EnchantmentID.EXPERIENCE.id())) {
                            final Enchantment enchantment = item.getEnchantment(CustomEnchantment.EnchantmentID.EXPERIENCE.id());
                            double experience = experienceBlock.getExperience();
                            if (enchantment.getLevel() == 1) experience = experience * 1.25;
                            else if (enchantment.getLevel() == 2) experience = experience * 1.60;
                            else if (enchantment.getLevel() == 3) experience = experience * 2;
                            this.instance.getLevelAPI().addExperience(event.getPlayer(), experience);
                        } else {
                            this.instance.getLevelAPI().addExperience(event.getPlayer(), experienceBlock.getExperience());
                        }
                    });
                }
            }
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
        this.instance.getLevelAPI().addExperience(event.getPlayer(), 5);
    }

    @AllArgsConstructor
    @Getter
    public static class ExperienceBlock {

        private final int id;
        private final int meta;
        private final double experience;

    }

}
