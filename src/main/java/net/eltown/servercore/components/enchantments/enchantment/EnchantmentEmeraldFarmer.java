package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentEmeraldFarmer extends Enchantment {

    public EnchantmentEmeraldFarmer() {
        super(CustomEnchantment.EnchantmentID.EMERALD_FARMER.id(), CustomEnchantment.EnchantmentID.EMERALD_FARMER.enchantment(), Rarity.COMMON, EnchantmentType.DIGGER);
    }

    @Override
    public String getName() {
        return this.name;
    }
}
