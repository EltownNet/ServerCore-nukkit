package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentColdProtection extends Enchantment {

    public EnchantmentColdProtection() {
        super(CustomEnchantment.EnchantmentID.COLD_PROTECTION.id(), CustomEnchantment.EnchantmentID.COLD_PROTECTION.enchantment(), Rarity.RARE, EnchantmentType.ARMOR);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
