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
import cn.nukkit.level.Location;
import cn.nukkit.math.BlockFace;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

import java.util.ArrayList;
import java.util.List;

public class EnchantmentDrill extends Enchantment implements Listener {

    public EnchantmentDrill() {
        super(CustomEnchantment.EnchantmentID.DRILL.id(), CustomEnchantment.EnchantmentID.DRILL.enchantment(), Rarity.RARE, EnchantmentType.DIGGER);
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
        if (item.hasEnchantment(this.getId())) {
            BlockFace blockFace = player.getDirection();
            if (player.getPitch() <= -40) blockFace = BlockFace.UP;
            if (player.getPitch() >= 40) blockFace = BlockFace.DOWN;

            this.breakBlocks(this.getBlocks(block, blockFace.name().toUpperCase()));
        }
    }

    private void breakBlocks(final List<Block> blocks) {
        blocks.forEach(e -> {
            if (e.getId() == BlockID.STONE) {
                e.getLevel().setBlock(e.getLocation(), Block.get(BlockID.AIR));
                e.getLevel().dropItem(e.getLocation(), Item.get(BlockID.COBBLESTONE));
            } else if (e.getId() == BlockID.NETHERRACK) {
                e.getLevel().setBlock(e.getLocation(), Block.get(BlockID.AIR));
                e.getLevel().dropItem(e.getLocation(), Item.get(BlockID.NETHERRACK));
            }
        });
    }

    private List<Block> getBlocks(final Block block, final String face) {
        final List<Block> blocks = new ArrayList<>();
        final Location location = block.getLocation();
        switch (face) {
            case "UP":
            case "DOWN":
                blocks.add(block);
                blocks.add(location.clone().add(0.0D, 0.0D, 1.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, 0.0D, -1.0D).getLevelBlock());
                blocks.add(location.clone().add(1.0D, 0.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(1.0D, 0.0D, 1.0D).getLevelBlock());
                blocks.add(location.clone().add(1.0D, 0.0D, -1.0D).getLevelBlock());
                blocks.add(location.clone().add(-1.0D, 0.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(-1.0D, 0.0D, 1.0D).getLevelBlock());
                blocks.add(location.clone().add(-1.0D, 0.0D, -1.0D).getLevelBlock());
                break;
            case "EAST":
            case "WEST":
                blocks.add(block);
                blocks.add(location.clone().add(0.0D, 0.0D, 1.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, 0.0D, -1.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, 1.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, 1.0D, 1.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, 1.0D, -1.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, -1.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, -1.0D, 1.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, -1.0D, -1.0D).getLevelBlock());
                break;
            case "NORTH":
            case "SOUTH":
                blocks.add(block);
                blocks.add(location.clone().add(1.0D, 0.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(-1.0D, 0.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, 1.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(1.0D, 1.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(-1.0D, 1.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(0.0D, -1.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(1.0D, -1.0D, 0.0D).getLevelBlock());
                blocks.add(location.clone().add(-1.0D, -1.0D, 0.0D).getLevelBlock());
                break;
        }
        return blocks;
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
