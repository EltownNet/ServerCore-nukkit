package net.eltown.servercore.components.api.intern;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.BlockID;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.network.protocol.AddItemEntityPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.chestshop.ChestShop;
import net.eltown.servercore.components.data.chestshop.ChestshopCalls;
import net.eltown.servercore.components.data.chestshop.ShopLicense;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ChestShopAPI {

    private final ServerCore instance;

    public HashMap<Location, ChestShop> cachedChestShops = new HashMap<>();
    private final HashMap<String, ShopLicense> cachedLicenses = new HashMap<>();

    public ChestShopAPI(final ServerCore instance) {
        this.instance = instance;

        this.instance.getTinyRabbit().sendAndReceive(delivery -> {
            final String[] d = delivery.getData();
            if (ChestshopCalls.valueOf(delivery.getKey().toUpperCase()) == ChestshopCalls.CALLBACK_LOAD_DATA) {
                final List<String> chestShops = Arrays.asList(d[1].split("-;-"));
                chestShops.forEach(e -> {
                    final String[] c = e.split("#");
                    final Location sign = new Location(Double.parseDouble(c[1]), Double.parseDouble(c[2]), Double.parseDouble(c[3]), this.instance.getServer().getLevelByName(c[7]));
                    final Location chest = new Location(Double.parseDouble(c[4]), Double.parseDouble(c[5]), Double.parseDouble(c[6]), this.instance.getServer().getLevelByName(c[7]));

                    this.cachedChestShops.put(sign, new ChestShop(
                            sign,
                            chest,
                            Long.parseLong(c[0]),
                            c[8],
                            ChestShop.ShopType.valueOf(c[9].toUpperCase()),
                            Double.parseDouble(c[12]),
                            Integer.parseInt(c[10]),
                            SyncAPI.ItemAPI.pureItemFromString(c[11]),
                            c[13]
                    ));
                });

                final List<String> licenses = Arrays.asList(d[2].split("-;-"));
                licenses.forEach(e -> {
                    final String[] c = e.split("#");
                    this.cachedLicenses.put(c[0], new ShopLicense(c[0], ShopLicense.ShopLicenseType.valueOf(c[1].toUpperCase()), Integer.parseInt(c[2])));
                });
            }
        }, Queue.CHESTSHOP_CALLBACK, ChestshopCalls.REQUEST_LOAD_DATA.name());

        final List<ChestShop> toRemove = new ArrayList<>();
        this.cachedChestShops.values().forEach(e -> {
            if (e.getSignLocation().getLevel().getBlock(e.getSignLocation()).getId() != BlockID.WALL_SIGN || e.getChestLocation().getLevel().getBlock(e.getChestLocation()).getId() != BlockID.CHEST) {
                toRemove.add(e);
            }
        });
        toRemove.forEach(e -> this.removeChestShop(e.getSignLocation(), e.getId()));

        this.instance.getServer().getScheduler().scheduleDelayedRepeatingTask(() -> {
            this.cachedChestShops.forEach((location, chestShop) -> {
                final RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
                removeEntityPacket.eid = chestShop.getId();
                this.instance.getServer().getOnlinePlayers().values().forEach(e -> e.dataPacket(removeEntityPacket));

                final Item displayItem = chestShop.getItem().clone();
                displayItem.setCount(1);

                final AddItemEntityPacket addItemEntityPacket = new AddItemEntityPacket();
                addItemEntityPacket.entityRuntimeId = chestShop.getId();
                addItemEntityPacket.entityUniqueId = chestShop.getId();
                addItemEntityPacket.item = displayItem;
                addItemEntityPacket.x = (float) chestShop.getChestLocation().x + 0.5f;
                addItemEntityPacket.y = (float) chestShop.getChestLocation().y + 1f;
                addItemEntityPacket.z = (float) chestShop.getChestLocation().z + 0.5f;
                addItemEntityPacket.speedX = 0f;
                addItemEntityPacket.speedY = 0f;
                addItemEntityPacket.speedZ = 0f;
                addItemEntityPacket.metadata = new EntityMetadata()
                        .putLong(Entity.DATA_FLAGS, Entity.DATA_FLAG_IMMOBILE)
                        .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                        .putLong(Entity.DATA_SCALE, 4L);
                this.instance.getServer().getOnlinePlayers().values().forEach(e -> e.dataPacket(addItemEntityPacket));
            });
        }, 500, 5000);
    }

    public void createChestShop(final Location signLocation, final Location chestLocation, final Player owner, final ChestShop.ShopType shopType, final double price, final int sellAmount, final Item item, final String bankAccount) {
        final long id = 1095216660480L + ThreadLocalRandom.current().nextLong(0L, 2147483647L);
        this.cachedChestShops.put(signLocation, new ChestShop(signLocation, chestLocation, id, owner.getName(), shopType, price, sellAmount, item, bankAccount));

        this.instance.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_CREATE_CHESTSHOP.name(),
                String.valueOf(id),
                String.valueOf(signLocation.getX()),
                String.valueOf(signLocation.getY()),
                String.valueOf(signLocation.getZ()),
                String.valueOf(chestLocation.getX()),
                String.valueOf(chestLocation.getY()),
                String.valueOf(chestLocation.getZ()),
                signLocation.getLevel().getName(),
                owner.getName(),
                shopType.name().toUpperCase(),
                String.valueOf(sellAmount),
                SyncAPI.ItemAPI.pureItemToString(item),
                String.valueOf(price),
                bankAccount
        );
    }

    public void updateAmount(final ChestShop chestShop, final int update) {
        this.cachedChestShops.get(chestShop.getSignLocation()).setShopCount(update);

        this.instance.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_UPDATE_AMOUNT.name(), String.valueOf(chestShop.getId()), String.valueOf(update));

        final BlockEntitySign blockEntitySign = (BlockEntitySign) chestShop.getSignLocation().getLevelBlock().getLevel().getBlockEntity(chestShop.getSignLocation());
        if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
            blockEntitySign.setText("§a[§2ChestShop§a]", "§0Kaufe: §2" + update + "x", "§f$ " + Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice()), "§2" + chestShop.getOwner());
        } else {
            blockEntitySign.setText("§a[§2ChestShop§a]", "§0Verkaufe: §2" + update + "x", "§f$ " + Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice()), "§2" + chestShop.getOwner());
        }
        blockEntitySign.scheduleUpdate();
    }

    public void updatePrice(final ChestShop chestShop, final double price) {
        this.cachedChestShops.get(chestShop.getSignLocation()).setShopPrice(price);

        this.instance.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_UPDATE_PRICE.name(), String.valueOf(chestShop.getId()), String.valueOf(price));

        final BlockEntitySign blockEntitySign = (BlockEntitySign) chestShop.getSignLocation().getLevelBlock().getLevel().getBlockEntity(chestShop.getSignLocation());
        if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
            blockEntitySign.setText("§a[§2ChestShop§a]", "§0Kaufe: §2" + chestShop.getShopCount() + "x", "§f$ " + Economy.getAPI().getMoneyFormat().format(price), "§2" + chestShop.getOwner());
        } else {
            blockEntitySign.setText("§a[§2ChestShop§a]", "§0Verkaufe: §2" + chestShop.getShopCount() + "x", "§f$ " + Economy.getAPI().getMoneyFormat().format(price), "§2" + chestShop.getOwner());
        }
        blockEntitySign.scheduleUpdate();
    }

    public void updateItem(final ChestShop chestShop, final String item) {
        this.cachedChestShops.get(chestShop.getSignLocation()).setItem(SyncAPI.ItemAPI.pureItemFromString(item));

        this.instance.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_UPDATE_ITEM.name(), String.valueOf(chestShop.getId()), item);

        final RemoveEntityPacket removeEntityPacket = new RemoveEntityPacket();
        removeEntityPacket.eid = chestShop.getId();
        Server.broadcastPacket(this.instance.getServer().getOnlinePlayers().values(), removeEntityPacket);

        final Item displayItem = chestShop.getItem().clone();
        displayItem.setCount(1);

        final AddItemEntityPacket addItemEntityPacket = new AddItemEntityPacket();
        addItemEntityPacket.entityRuntimeId = chestShop.getId();
        addItemEntityPacket.entityUniqueId = chestShop.getId();
        addItemEntityPacket.item = displayItem;
        addItemEntityPacket.x = (float) chestShop.getChestLocation().x + 0.5f;
        addItemEntityPacket.y = (float) chestShop.getChestLocation().y + 1f;
        addItemEntityPacket.z = (float) chestShop.getChestLocation().z + 0.5f;
        addItemEntityPacket.speedX = 0f;
        addItemEntityPacket.speedY = 0f;
        addItemEntityPacket.speedZ = 0f;
        addItemEntityPacket.metadata = new EntityMetadata()
                .putLong(Entity.DATA_FLAGS, Entity.DATA_FLAG_IMMOBILE)
                .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                .putLong(Entity.DATA_SCALE, 4L);
        this.instance.getServer().getOnlinePlayers().values().forEach(e -> e.dataPacket(addItemEntityPacket));
    }

    public void removeChestShop(final Location signLocation, final long id) {
        this.instance.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_REMOVE_SHOP.name(), String.valueOf(id));

        final RemoveEntityPacket packet = new RemoveEntityPacket();
        packet.eid = id;
        this.instance.getServer().getOnlinePlayers().values().forEach(e -> e.dataPacket(packet));

        this.cachedChestShops.remove(signLocation);
    }

    public int countPlayerChestShops(final String player) {
        final AtomicInteger atomicInteger = new AtomicInteger(0);
        this.cachedChestShops.values().forEach(e -> {
            if (e.getOwner().equals(player)) atomicInteger.addAndGet(1);
        });
        return atomicInteger.get();
    }

    public ShopLicense getPlayerLicense(final String player) {
        final ShopLicense shopLicense = this.cachedLicenses.get(player);
        if (shopLicense != null) {
            return shopLicense;
        } else return new ShopLicense(player, ShopLicense.ShopLicenseType.STANDARD, 0);
    }

    public void setLicense(final String player, final ShopLicense.ShopLicenseType licenseType) {
        this.cachedLicenses.get(player).setLicense(licenseType);
        this.instance.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_SET_LICENSE.name(), player, licenseType.name().toUpperCase());
    }

    public void setAdditionalShops(final String player, final int additionalShops) {
        this.cachedLicenses.get(player).setAdditionalShops(additionalShops);
        this.instance.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_SET_ADDITIONAL_SHOPS.name(), player, String.valueOf(additionalShops));
    }

    public void addAdditionalShops(final String player, final int additionalShops) {
        this.cachedLicenses.get(player).setAdditionalShops(this.cachedLicenses.get(player).getAdditionalShops() + additionalShops);
        this.instance.getTinyRabbit().send(Queue.CHESTSHOP_RECEIVE, ChestshopCalls.REQUEST_ADD_ADDITIONAL_SHOPS.name(), player, String.valueOf(additionalShops));
    }

}
