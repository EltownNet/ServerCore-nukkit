package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentVeinMining extends Enchantment {

    public EnchantmentVeinMining() {
        super(CustomEnchantment.EnchantmentID.VEIN_MINING.id(), CustomEnchantment.EnchantmentID.VEIN_MINING.enchantment(), Rarity.RARE, EnchantmentType.DIGGER);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
