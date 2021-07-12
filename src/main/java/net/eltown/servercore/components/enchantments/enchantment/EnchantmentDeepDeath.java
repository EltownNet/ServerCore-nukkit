package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentDeepDeath extends Enchantment {

    public EnchantmentDeepDeath() {
        super(CustomEnchantment.EnchantmentID.DEEP_DEATH.id(), CustomEnchantment.EnchantmentID.DEEP_DEATH.enchantment(), Rarity.VERY_RARE, EnchantmentType.ARMOR_TORSO);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
