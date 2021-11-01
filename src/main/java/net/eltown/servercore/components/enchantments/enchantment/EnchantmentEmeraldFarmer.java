package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentEmeraldFarmer extends Enchantment implements Listener {

    private final ServerCore serverCore;

    public EnchantmentEmeraldFarmer(final ServerCore serverCore) {
        super(CustomEnchantment.EnchantmentID.EMERALD_FARMER.id(), CustomEnchantment.EnchantmentID.EMERALD_FARMER.enchantment(), Rarity.COMMON, EnchantmentType.DIGGER);
        this.serverCore = serverCore;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @EventHandler
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final Item item = player.getInventory().getItemInHand();
        if (block.getId() == BlockID.EMERALD_ORE) {
            if (item.hasEnchantment(this.getId())) {
                if (!block.getLevel().getName().equals("plots")) {
                    this.serverCore.getLevelAPI().addExperience(player, 30);
                }
            } else {
                player.sendActionBar("§fDamit du dies abbauen kannst, benötigst du die Verzauberung §9Smaragdfarmer§f.");
                event.setCancelled(true);
            }
        }
    }

}
