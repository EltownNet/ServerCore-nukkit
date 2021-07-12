package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentNightVision extends Enchantment {

    public EnchantmentNightVision() {
        super(CustomEnchantment.EnchantmentID.NIGHT_VISION.id(), CustomEnchantment.EnchantmentID.NIGHT_VISION.enchantment(), Rarity.UNCOMMON, EnchantmentType.ARMOR_HEAD);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
