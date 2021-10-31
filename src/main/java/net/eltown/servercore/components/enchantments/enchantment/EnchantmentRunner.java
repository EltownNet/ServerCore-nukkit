package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityArmorChangeEvent;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import cn.nukkit.potion.Effect;
import net.eltown.servercore.components.enchantments.CustomEnchantment;

public class EnchantmentRunner extends Enchantment implements Listener {

    public EnchantmentRunner() {
        super(CustomEnchantment.EnchantmentID.RUNNER.id(), CustomEnchantment.EnchantmentID.RUNNER.enchantment(), Rarity.COMMON, EnchantmentType.ARMOR_FEET);
    }

    @EventHandler
    public void on(final EntityArmorChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();

            // Unequip
            if (event.getOldItem().hasEnchantment(CustomEnchantment.EnchantmentID.RUNNER.id())) {
                player.removeEffect(Effect.SPEED);
            }

            // Equip
            if (event.getNewItem().hasEnchantment(CustomEnchantment.EnchantmentID.RUNNER.id())) {
                final Enchantment ec = event.getNewItem().getEnchantment(CustomEnchantment.EnchantmentID.RUNNER.id());
                player.addEffect(Effect.getEffect(Effect.SPEED).setVisible(false).setDuration(Integer.MAX_VALUE).setAmplifier(ec.getLevel() - 1));
            }
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }
}
