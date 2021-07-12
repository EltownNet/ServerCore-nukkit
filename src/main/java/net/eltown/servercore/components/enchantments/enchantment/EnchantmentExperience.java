package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentExperience extends Enchantment {

    public EnchantmentExperience() {
        super(CustomEnchantment.EnchantmentID.EXPERIENCE.id(), CustomEnchantment.EnchantmentID.EXPERIENCE.enchantment(), Rarity.UNCOMMON, EnchantmentType.DIGGER);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
