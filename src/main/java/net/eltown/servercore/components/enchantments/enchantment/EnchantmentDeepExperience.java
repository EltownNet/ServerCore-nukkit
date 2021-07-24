package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentDeepExperience extends Enchantment {

    public EnchantmentDeepExperience() {
        super(CustomEnchantment.EnchantmentID.DEEP_EXPERIENCE.id(), CustomEnchantment.EnchantmentID.DEEP_EXPERIENCE.enchantment(), Rarity.VERY_RARE, EnchantmentType.DIGGER);
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
