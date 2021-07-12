package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentRunner extends Enchantment {

    public EnchantmentRunner() {
        super(CustomEnchantment.EnchantmentID.RUNNER.id(), CustomEnchantment.EnchantmentID.RUNNER.enchantment(), Rarity.COMMON, EnchantmentType.ARMOR_FEET);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
