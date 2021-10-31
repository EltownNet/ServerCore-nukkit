package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentVeinMining extends Enchantment implements Listener {

    public EnchantmentVeinMining() {
        super(CustomEnchantment.EnchantmentID.VEIN_MINING.id(), CustomEnchantment.EnchantmentID.VEIN_MINING.enchantment(), Rarity.RARE, EnchantmentType.DIGGER);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Item item = player.getInventory().getItemInHand();
        final Block block = event.getBlock();
        if (item.hasEnchantment(this.getId()) && (this.oreToDropItem(block).getId() != BlockID.AIR)) {
            this.checkAndBreak(player, block, event.getDrops());
        }
    }

    private void checkAndBreak(final Player player, final Block block, final Item[] drops) {
        final Block north = block.north();
        final Block east = block.east();
        final Block south = block.south();
        final Block west = block.west();
        final Block down = block.down();
        final Block up = block.up();

        if (block.getId() == BlockID.AIR) return;

        if (north.getId() == block.getId()) {
            north.getLevel().setBlock(north.getLocation(), Block.get(BlockID.AIR));
            this.checkAndBreak(player, north, drops);
            for (final Item drop : drops) {
                north.getLevel().dropItem(north.getLocation(), drop);
            }
        }
        if (east.getId() == block.getId()) {
            east.getLevel().setBlock(east.getLocation(), Block.get(BlockID.AIR));
            this.checkAndBreak(player, east, drops);
            for (final Item drop : drops) {
                east.getLevel().dropItem(east.getLocation(), drop);
            }
        }
        if (south.getId() == block.getId()) {
            south.getLevel().setBlock(south.getLocation(), Block.get(BlockID.AIR));
            this.checkAndBreak(player, south, drops);
            for (final Item drop : drops) {
                south.getLevel().dropItem(south.getLocation(), drop);
            }
        }
        if (west.getId() == block.getId()) {
            west.getLevel().setBlock(west.getLocation(), Block.get(BlockID.AIR));
            this.checkAndBreak(player, west, drops);
            for (final Item drop : drops) {
                west.getLevel().dropItem(west.getLocation(), drop);
            }
        }
        if (down.getId() == block.getId()) {
            down.getLevel().setBlock(down.getLocation(), Block.get(BlockID.AIR));
            this.checkAndBreak(player, down, drops);
            for (final Item drop : drops) {
                down.getLevel().dropItem(down.getLocation(), drop);
            }
        }
        if (up.getId() == block.getId()) {
            up.getLevel().setBlock(up.getLocation(), Block.get(BlockID.AIR));
            this.checkAndBreak(player, up, drops);
            for (final Item drop : drops) {
                up.getLevel().dropItem(up.getLocation(), drop);
            }
        }
    }

    private Item oreToDropItem(final Block block) {
        switch (block.getId()) {
            case BlockID
                    .COAL_ORE:
                return Item.get(ItemID.COAL);
            case BlockID
                    .IRON_ORE:
                return Item.get(BlockID.IRON_ORE);
            case BlockID
                    .GOLD_ORE:
                return Item.get(BlockID.GOLD_ORE);
            case BlockID
                    .DIAMOND_ORE:
                return Item.get(ItemID.DIAMOND);
            case BlockID
                    .REDSTONE_ORE:
            case BlockID
                    .GLOWING_REDSTONE_ORE:
                return Item.get(ItemID.REDSTONE_DUST);
            case BlockID
                    .LAPIS_ORE:
                return Item.get(ItemID.DYE, 4);
            case BlockID.
                    QUARTZ_ORE:
                return Item.get(ItemID.NETHER_QUARTZ);
            case BlockID.
                    NETHER_GOLD_ORE:
                return Item.get(ItemID.GOLD_NUGGET);
            default:
                return Item.get(BlockID.AIR);
        }
    }
}
