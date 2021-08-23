package net.eltown.servercore.components.enchantments.enchantment;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityArmorChangeEvent;
import cn.nukkit.event.player.PlayerMoveEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.item.enchantment.EnchantmentType;
import cn.nukkit.potion.Effect;
import net.eltown.servercore.components.enchantments.CustomEnchantment;
import net.eltown.servercore.components.roleplay.Cooldown;

public class EnchantmentNightVision extends Enchantment implements Listener {

    public EnchantmentNightVision() {
        super(CustomEnchantment.EnchantmentID.NIGHT_VISION.id(), CustomEnchantment.EnchantmentID.NIGHT_VISION.enchantment(), Rarity.UNCOMMON, EnchantmentType.ARMOR_HEAD);
    }

    @EventHandler
    public void on(final EntityArmorChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();

            // Unequip
            if (event.getOldItem().hasEnchantment(CustomEnchantment.EnchantmentID.NIGHT_VISION.id())) {
                player.removeEffect(Effect.NIGHT_VISION);
            }

            // Equip
            if (event.getNewItem().hasEnchantment(CustomEnchantment.EnchantmentID.NIGHT_VISION.id())) {
                player.addEffect(Effect.getEffect(Effect.NIGHT_VISION).setVisible(false).setDuration(Integer.MAX_VALUE));
            }
        }
    }

    @Override
    public String getName() {
        return this.name;
    }
}
