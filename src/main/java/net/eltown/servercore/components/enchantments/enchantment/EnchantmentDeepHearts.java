package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentDeepHearts extends Enchantment {

    public EnchantmentDeepHearts() {
        super(CustomEnchantment.EnchantmentID.DEEP_HEARTS.id(), CustomEnchantment.EnchantmentID.DEEP_HEARTS.enchantment(), Rarity.VERY_RARE, EnchantmentType.ARMOR);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
