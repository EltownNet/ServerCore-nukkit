package net.eltown.servercore.components.api.intern;

import cn.nukkit.Player;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import cn.nukkit.potion.Effect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.sync.SyncCalls;
import net.eltown.servercore.components.data.sync.SyncPlayer;
import net.eltown.servercore.components.language.Language;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class SyncAPI {

    private final ServerCore instance;

    public void savePlayer(final String player, final String invString, final String ecString, final String health, final int food, final int level, final int exp, final int gamemode, final List<String> effects) {
        this.instance.getTinyRabbit().send("playersyncReceive", SyncCalls.REQUEST_SETSYNC.name(), player, invString, ecString, health, "" + food, "" + level, "" + exp, effects.size() > 0 ? String.join("#", effects) : "empty", "" + gamemode);
    }

    public void getPlayer(final Player player, final Consumer<SyncPlayer> callback) {
        CompletableFuture.runAsync(() -> {
            this.instance.getTinyRabbit().sendAndReceive((delivery -> {
                switch (SyncCalls.valueOf(delivery.getKey())) {
                    case GOT_NOSYNC:
                        this.instance.getServer().getScheduler().scheduleDelayedTask(() -> this.getPlayer(player, callback), 5);
                        break;
                    case GOT_SYNC:
                        this.instance.getTinyRabbit().send(SyncCalls.REQUEST_SETNOSYNC.name(), player.getName());
                        final Set<Effect> effects = new HashSet<>();
                        if (!delivery.getData()[7].equalsIgnoreCase("empty")) {
                            final String[] splittedEffects = delivery.getData()[7].split("#");

                            for (final String str : splittedEffects) {
                                final String[] effectSplit = str.split(":");
                                effects.add(Effect.getEffect(Integer.parseInt(effectSplit[0]))
                                        .setAmplifier(Integer.parseInt(effectSplit[1]))
                                        .setDuration(Integer.parseInt(effectSplit[2]))
                                );
                            }
                        }
                        callback.accept(new SyncPlayer(delivery.getData()[1], delivery.getData()[2], Float.parseFloat(delivery.getData()[3]), Integer.parseInt(delivery.getData()[4]), Integer.parseInt(delivery.getData()[5]), Integer.parseInt(delivery.getData()[6]), Integer.parseInt(delivery.getData()[8]), effects));
                        break;
                }
            }), "playersync", SyncCalls.REQUEST_SYNC.name(), player.getName());
        });
    }

    @Getter
    private final ArrayList<String> loaded = new ArrayList<>();

    public void savePlayerAsync(final Player player) {
        CompletableFuture.runAsync(() -> {
            try {
                this.savePlayer(player);
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    public void savePlayer(final Player player) {
        if (!this.loaded.contains(player.getName())) return;
        String inv = "empty";
        String ec = "empty";
        if (player.getEnderChestInventory().getContents().size() > 0)
            ec = ItemAPI.invToString(player.getEnderChestInventory());
        if (player.getInventory().getContents().size() > 0) inv = ItemAPI.invToString(player.getInventory());
        final ArrayList<String> effects = new ArrayList<>();
        player.getEffects().forEach((id, effect) -> {
            effects.add(effect.getId() + ":" + effect.getAmplifier() + ":" + effect.getDuration());
        });

        this.savePlayer(player.getName(), inv, ec, player.getHealth() + "", player.getFoodData().getLevel(), player.getExperienceLevel(), player.getExperience(), player.getGamemode(), effects);
    }

    public void loadPlayer(final Player player, final Consumer<Boolean> b) {
        this.loaded.remove(player.getName());

        player.getInventory().clearAll();
        player.getEnderChestInventory().clearAll();
        player.setExperience(0, 0);
        player.getEffects().clear();

        player.sendMessage(Language.get("sync.data.loading"));
        this.instance.playSound(player, Sound.RANDOM_ORB);

        this.getPlayer(player, (syncPlayer -> {
            player.getInventory().setContents(ItemAPI.invFromString(syncPlayer.getInvString()));
            player.getEnderChestInventory().setContents(ItemAPI.invFromString(syncPlayer.getEcString()));
            player.setHealth(syncPlayer.getHealth());
            player.getFoodData().setLevel(syncPlayer.getFood());
            player.setExperience(syncPlayer.getExp(), syncPlayer.getLevel());
            player.setGamemode(syncPlayer.getGamemode());
            syncPlayer.getEffects().forEach(player::addEffect);

            loaded.add(player.getName());
            player.sendMessage(Language.get("sync.data.loaded"));
            this.instance.playSound(player, Sound.RANDOM_LEVELUP);
            b.accept(true);
        }));
    }

    public static class ItemAPI {

        public static String invToString(Inventory inventory) {
            StringBuilder stringInv = new StringBuilder();
            inventory.getContents().forEach(((integer, item) -> stringInv.append(itemToString(integer, item)).append(";")));
            return stringInv.substring(0, stringInv.toString().length() - 1);
        }

        public static Map<Integer, Item> invFromString(String string) {
            if (!string.equalsIgnoreCase("empty")) {
                String[] itemStrings = string.split(";");
                final Map<Integer, Item> loadedInv = new HashMap<>();
                for (String str : itemStrings) {
                    ItemWithSlot its = itemFromString(str);
                    loadedInv.put(its.getSlot(), its.getItem());
                }
                return loadedInv;
            } else return new HashMap<>();
        }

        // itemInfo
        // slot:id:damage:count:CompoundTag(base64)

        public static String itemToString(int slot, Item item) {
            return slot + ":" + item.getId() + ":" + item.getDamage() + ":" + item.getCount() + ":" +
                    (item.hasCompoundTag() ? bytesToBase64(item.getCompoundTag()) : "not");
        }

        private static String bytesToBase64(byte[] src) {
            if (src == null || src.length <= 0) return "not";
            return Base64.getEncoder().encodeToString(src);
        }

        // itemInfo
        // slot:id:damage:count:CompoundTag(base64)

        public static ItemWithSlot itemFromString(String itemString) throws NumberFormatException {
            String[] info = itemString.split(":");
            int slot = Integer.parseInt(info[0]);
            Item item = Item.get(
                    Integer.parseInt(info[1]),
                    Integer.parseInt(info[2]),
                    Integer.parseInt(info[3])
            );
            if (!info[4].equals("not")) item.setCompoundTag(base64ToBytes(info[4]));
            return new ItemWithSlot(slot, item);
        }

        private static byte[] base64ToBytes(String hexString) {
            if (hexString == null || hexString.equals("")) return null;
            return Base64.getDecoder().decode(hexString);
        }

        @RequiredArgsConstructor
        @Getter
        public static class ItemWithSlot {

            private final Integer slot;
            private final Item item;

        }

    }
}
