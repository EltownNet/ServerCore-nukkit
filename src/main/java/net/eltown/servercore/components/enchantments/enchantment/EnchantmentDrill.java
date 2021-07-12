package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentDrill extends Enchantment {

    public EnchantmentDrill() {
        super(CustomEnchantment.EnchantmentID.DRILL.id(), CustomEnchantment.EnchantmentID.DRILL.enchantment(), Rarity.RARE, EnchantmentType.DIGGER);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
