package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentDeepDiver extends Enchantment {

    public EnchantmentDeepDiver() {
        super(CustomEnchantment.EnchantmentID.DEEP_DIVER.id(), CustomEnchantment.EnchantmentID.DEEP_DIVER.enchantment(), Rarity.VERY_RARE, EnchantmentType.ARMOR_HEAD);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
