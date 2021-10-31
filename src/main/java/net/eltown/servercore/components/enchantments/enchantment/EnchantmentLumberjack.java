package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import cn.nukkit.level.Level;
import cn.nukkit.level.Position;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class EnchantmentLumberjack extends Enchantment implements Listener {

    private static boolean[] isWood = new boolean[2048];
    private static boolean[] isLeaf = new boolean[2048];

    static {
        isWood[BlockID.WOOD] = true;
        isWood[BlockID.WOOD2] = true;

        isLeaf[BlockID.LEAVE] = true;
        isLeaf[BlockID.LEAVE2] = true;
    }

    public EnchantmentLumberjack() {
        super(CustomEnchantment.EnchantmentID.LUMBERJACK.id(), CustomEnchantment.EnchantmentID.LUMBERJACK.enchantment(), Rarity.COMMON, EnchantmentType.DIGGER);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @EventHandler
    public void on(final BlockBreakEvent event) {
        if (event.getPlayer().getInventory().getItemInHand().hasEnchantment(this.getId())) {
            final LinkedList<Block> blocks = new LinkedList<>();

            final Block block = event.getBlock();
            if (isWood[block.getId()]) {
                // Above + TreeCheck
                int offset = 1;

                while (isWood[this.getOffset(block, offset).getId()]) {
                    blocks.add(this.getOffset(block, offset));
                    offset++;
                }

                if (blocks.size() > 1 && this.isTree(block.getLevel(), blocks.get(blocks.size() - 1))) {
                    offset = -1;

                    // Below
                    while (isWood[this.getOffset(block, offset).getId()]) {
                        blocks.add(this.getOffset(block, offset));
                        offset--;
                    }

                    final AtomicInteger count = new AtomicInteger(0);

                    blocks.forEach((chop) -> {
                        Server.getInstance().getScheduler().scheduleDelayedTask(() -> {
                            chop.getLevel().setBlock(chop.getLocation(), Block.get(BlockID.AIR));
                            chop.getLevel().dropItem(chop.getLocation(), Item.get(chop.getId(), chop.getDamage(), 1));
                        }, 2 * count.addAndGet(1));
                    });
                }
            }
        }
    }

    private Block getOffset(final Block block, final int yOffset) {
        return block.getLevel().getBlock(block.getFloorX(), block.getFloorY() + yOffset, block.getFloorZ());
    }

    private boolean isTree(final Level level, final Position woodCrown) {
        if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX(), woodCrown.getFloorY() + 1, woodCrown.getFloorZ())]) return true;
        else if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX() + 1, woodCrown.getFloorY(), woodCrown.getFloorZ())]) return true;
        else if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX() + 1, woodCrown.getFloorY(), woodCrown.getFloorZ() - 1)]) return true;
        else if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX() + 1, woodCrown.getFloorY(), woodCrown.getFloorZ() + 1)]) return true;
        else if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX(), woodCrown.getFloorY(), woodCrown.getFloorZ())]) return true;
        else if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX(), woodCrown.getFloorY(), woodCrown.getFloorZ() + 1)]) return true;
        else if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX(), woodCrown.getFloorY(), woodCrown.getFloorZ() - 1)]) return true;
        else if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX() - 1, woodCrown.getFloorY(), woodCrown.getFloorZ() + 1)]) return true;
        else if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX() - 1, woodCrown.getFloorY(), woodCrown.getFloorZ() - 1)]) return true;
        else if (isLeaf[level.getBlockIdAt(woodCrown.getFloorX() - 1, woodCrown.getFloorY(), woodCrown.getFloorZ())]) return true;
        else return false;
    }

}
