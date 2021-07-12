package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentLumberjack extends Enchantment implements Listener {

    public EnchantmentLumberjack() {
        super(CustomEnchantment.EnchantmentID.LUMBERJACK.id(), CustomEnchantment.EnchantmentID.LUMBERJACK.enchantment(), Rarity.COMMON, EnchantmentType.DIGGER);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @EventHandler
    public void on(final PlayerInteractEvent event) {

    }
}
