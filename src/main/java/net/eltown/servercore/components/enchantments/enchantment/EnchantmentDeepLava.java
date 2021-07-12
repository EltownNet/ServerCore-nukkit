package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentDeepLava extends Enchantment {

    public EnchantmentDeepLava() {
        super(CustomEnchantment.EnchantmentID.DEEP_LAVA.id(), CustomEnchantment.EnchantmentID.DEEP_LAVA.enchantment(), Rarity.VERY_RARE, EnchantmentType.ARMOR);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
