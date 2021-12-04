package net.eltown.servercore.components.enchantments;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.enchantments.enchantment.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class CustomEnchantment {

    private final ServerCore instance;

    public final HashMap<Integer, EnchantmentID> enchantmentId = new HashMap<>();

    public CustomEnchantment(final ServerCore serverCore) {
        this.instance = serverCore;

        final Enchantment[] enchantments = new Enchantment[512];
        final Enchantment[] legacy;
        final int count;
        byte id;
        for (count = (legacy = Enchantment.getEnchantments()).length, id = 0; id < count; ) {
            final Enchantment enchantment;
            if ((enchantment = legacy[id]).getId() >= 0 && enchantment.getId() <= 100)
                enchantments[enchantment.getId()] = enchantment;
            id++;
        }

        /*
         * Register custom enchantments
         */
        enchantments[EnchantmentID.COLD_PROTECTION.id] = new EnchantmentColdProtection();
        this.enchantmentId.put(EnchantmentID.COLD_PROTECTION.id, EnchantmentID.COLD_PROTECTION);

        enchantments[EnchantmentID.DEEP_DEATH.id] = new EnchantmentDeepDeath();
        this.enchantmentId.put(EnchantmentID.DEEP_DEATH.id, EnchantmentID.DEEP_DEATH);

        enchantments[EnchantmentID.DEEP_DIVER.id] = new EnchantmentDeepDiver();
        this.enchantmentId.put(EnchantmentID.DEEP_DIVER.id, EnchantmentID.DEEP_DIVER);

        enchantments[EnchantmentID.DEEP_EXPERIENCE.id] = new EnchantmentDeepExperience();
        this.enchantmentId.put(EnchantmentID.DEEP_EXPERIENCE.id, EnchantmentID.DEEP_EXPERIENCE);

        enchantments[EnchantmentID.DEEP_HEARTS.id] = new EnchantmentDeepHearts();
        this.enchantmentId.put(EnchantmentID.DEEP_HEARTS.id, EnchantmentID.DEEP_HEARTS);

        enchantments[EnchantmentID.DEEP_LAVA.id] = new EnchantmentDeepLava();
        this.enchantmentId.put(EnchantmentID.DEEP_LAVA.id, EnchantmentID.DEEP_LAVA);

        enchantments[EnchantmentID.DRILL.id] = new EnchantmentDrill();
        this.enchantmentId.put(EnchantmentID.DRILL.id, EnchantmentID.DRILL);
        serverCore.getServer().getPluginManager().registerEvents(new EnchantmentDrill(), serverCore);

        enchantments[EnchantmentID.EMERALD_FARMER.id] = new EnchantmentEmeraldFarmer(serverCore);
        this.enchantmentId.put(EnchantmentID.EMERALD_FARMER.id, EnchantmentID.EMERALD_FARMER);
        serverCore.getServer().getPluginManager().registerEvents(new EnchantmentEmeraldFarmer(serverCore), serverCore);

        enchantments[EnchantmentID.EXPERIENCE.id] = new EnchantmentExperience();
        this.enchantmentId.put(EnchantmentID.EXPERIENCE.id, EnchantmentID.EXPERIENCE);

        enchantments[EnchantmentID.LUMBERJACK.id] = new EnchantmentLumberjack(serverCore);
        this.enchantmentId.put(EnchantmentID.LUMBERJACK.id, EnchantmentID.LUMBERJACK);
        serverCore.getServer().getPluginManager().registerEvents(new EnchantmentLumberjack(serverCore), serverCore);

        enchantments[EnchantmentID.MAGNET.id] = new EnchantmentMagnet();
        this.enchantmentId.put(EnchantmentID.MAGNET.id, EnchantmentID.MAGNET);

        enchantments[EnchantmentID.NIGHT_VISION.id] = new EnchantmentNightVision();
        this.enchantmentId.put(EnchantmentID.NIGHT_VISION.id, EnchantmentID.NIGHT_VISION);
        serverCore.getServer().getPluginManager().registerEvents(new EnchantmentNightVision(), serverCore);

        enchantments[EnchantmentID.RUNNER.id] = new EnchantmentRunner();
        this.enchantmentId.put(EnchantmentID.RUNNER.id, EnchantmentID.RUNNER);
        serverCore.getServer().getPluginManager().registerEvents(new EnchantmentRunner(), serverCore);

        enchantments[EnchantmentID.THERMAL_PROTECTION.id] = new EnchantmentThermalProtection();
        this.enchantmentId.put(EnchantmentID.THERMAL_PROTECTION.id, EnchantmentID.THERMAL_PROTECTION);

        enchantments[EnchantmentID.VEIN_MINING.id] = new EnchantmentVeinMining();
        this.enchantmentId.put(EnchantmentID.VEIN_MINING.id, EnchantmentID.VEIN_MINING);
        serverCore.getServer().getPluginManager().registerEvents(new EnchantmentVeinMining(), serverCore);

        try {
            final Field[] arrayOfField;
            (arrayOfField = Class.forName("cn.nukkit.item.enchantment.Enchantment").getDeclaredFields())[1].setAccessible(true);
            arrayOfField[1].set(null, enchantments);
        } catch (final Exception exception) {
            serverCore.getLogger().error("Es trat bei der Registrierung der Custom Enchantments ein Fehler auf.", exception);
        }
    }

    public void enchantItem(final Player player, final EnchantmentID enchantmentID, int level) {
        final Item item = player.getInventory().getItemInHand();
        final Enchantment enchantment = Enchantment.getEnchantment(enchantmentID.id);
        if (enchantment != null && !enchantment.getName().equals("%enchantment.unknown")) {
            if (level > enchantment.getMaxLevel()) level = enchantment.getMaxLevel();
            enchantment.setLevel(level);
            item.addEnchantment(enchantment);

            final List<String> lore;
            for (final String value : lore = new LinkedList<>(Arrays.asList(item.getLore()))) {
                final String s;
                if ((s = value).equals("§r" + enchantmentID.color + enchantmentID.enchantment + getLevelString(level))) {
                    lore.remove(s);
                    continue;
                }
                if (s.startsWith("§r" + enchantmentID.color + enchantmentID.enchantment))
                    lore.remove(s);
            }
            lore.add("§r" + enchantmentID.color + enchantmentID.enchantment + getLevelString(level));
            item.setLore(lore.toArray(new String[0]));
            player.getInventory().setItemInHand(item);
        }
    }

    public void enchantItem(final Player player, final int enchantmentID, int level) {
        final Item item = player.getInventory().getItemInHand();
        final Enchantment enchantment = Enchantment.getEnchantment(enchantmentID);
        if (enchantment != null && !enchantment.getName().equals("%enchantment.unknown")) {
            if (level > enchantment.getMaxLevel()) level = enchantment.getMaxLevel();
            enchantment.setLevel(level);
            item.addEnchantment(enchantment);
            player.getInventory().setItemInHand(item);
        }
    }

    public String getLevelString(final int lvl) {
        switch (lvl) {
            case 1:
                return " I";
            case 2:
                return " II";
            case 3:
                return " III";
            case 4:
                return " IV";
            case 5:
                return " V";
            case 6:
                return " VI";
            case 7:
                return " VII";
            case 8:
                return " VIII";
            case 9:
                return " IX";
            case 10:
                return " X";
        }
        return " ".concat(String.valueOf(lvl));
    }


    public enum EnchantmentID {

        // common: §a  uncommon: §e  rare: §b  mythic: §5 legendary: §6
        LUMBERJACK("Holzfäller", "§a", 128),
        THERMAL_PROTECTION("Wärmeschutz", "§b", 129),
        COLD_PROTECTION("Kälteschutz", "§b", 130),
        MAGNET("Magnetfeld", "§e", 131),
        DRILL("Bohrer", "§b", 132),
        EMERALD_FARMER("Smaragdfarmer", "§a", 133),
        EXPERIENCE("Erfahrung", "§e", 134),
        NIGHT_VISION("Nachtsicht", "§e", 135),
        RUNNER("Läufer", "§a", 136),
        VEIN_MINING("Aderabbau", "§b", 137),

        DEEP_DEATH("Schutz des Todes", "§6", 192),
        DEEP_EXPERIENCE("Verlorene Weisheiten", "§5", 193),
        DEEP_HEARTS("Herzübertaktung", "§6", 194),
        DEEP_LAVA("Lavaschwimmer", "§5", 195),
        DEEP_DIVER("Profitaucher", "§5", 196),

        ;

        private final String enchantment;
        private final String color;
        private final int id;

        EnchantmentID(final String name, final String color, final int id) {
            this.enchantment = name;
            this.color = color;
            this.id = id;
        }

        public int id() {
            return id;
        }

        public String color() {
            return color;
        }

        public String enchantment() {
            return enchantment;
        }

    }

}
