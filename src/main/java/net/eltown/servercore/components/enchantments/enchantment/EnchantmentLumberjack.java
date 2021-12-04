package net.eltown.servercore.components.enchantments.enchantment;

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
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.enchantments.CustomEnchantment;
import net.eltown.servercore.listeners.SpawnProtectionListener;

import java.util.*;

public class EnchantmentLumberjack extends Enchantment implements Listener {

    private final ServerCore serverCore;

    private static boolean[] isWood = new boolean[2048];
    private static boolean[] isLeaf = new boolean[2048];

    static {
        isWood[BlockID.WOOD] = true;
        isWood[BlockID.WOOD2] = true;

        isLeaf[BlockID.LEAVE] = true;
        isLeaf[BlockID.LEAVE2] = true;
    }

    public EnchantmentLumberjack(final ServerCore serverCore) {
        super(CustomEnchantment.EnchantmentID.LUMBERJACK.id(), CustomEnchantment.EnchantmentID.LUMBERJACK.enchantment(), Rarity.COMMON, EnchantmentType.DIGGER);
        this.serverCore = serverCore;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @EventHandler
    public void on(final BlockBreakEvent event) {
        if (this.serverCore.getServerName().equals("server-1")) return;

        final Item item = event.getPlayer().getInventory().getItemInHand();

        if (item.hasEnchantment(this.getId())) {
            if (SpawnProtectionListener.isInRadius(event.getBlock())) {
                event.setCancelled(true);
                return;
            }

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

                    event.getItem().setDamage(item.getDamage() + blocks.size());
                    event.getPlayer().getInventory().setItemInHand(event.getItem());

                    blocks.forEach((chop) -> {
                        chop.getLevel().setBlock(chop.getLocation(), Block.get(BlockID.AIR));
                        chop.getLevel().dropItem(chop.getLocation(), Item.get(chop.getId(), chop.getDamage(), 1));
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
