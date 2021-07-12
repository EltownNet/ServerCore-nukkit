package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentThermalProtection extends Enchantment {

    public EnchantmentThermalProtection() {
        super(CustomEnchantment.EnchantmentID.THERMAL_PROTECTION.id(), CustomEnchantment.EnchantmentID.THERMAL_PROTECTION.enchantment(), Rarity.RARE, EnchantmentType.ARMOR);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
