package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentMagnet extends Enchantment {

    public EnchantmentMagnet() {
        super(CustomEnchantment.EnchantmentID.MAGNET.id(), CustomEnchantment.EnchantmentID.MAGNET.enchantment(), Rarity.UNCOMMON, EnchantmentType.DIGGER);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getMaxLevel() {
        return 2;
    }
}
