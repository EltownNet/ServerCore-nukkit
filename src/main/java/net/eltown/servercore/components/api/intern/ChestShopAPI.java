package net.eltown.servercore.components.api.intern;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.network.protocol.AddItemEntityPacket;
import cn.nukkit.network.protocol.RemoveEntityPacket;
import cn.nukkit.utils.Config;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.chestshop.ChestShop;
import net.eltown.servercore.components.data.chestshop.ShopLicense;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ChestShopAPI {

    private final ServerCore instance;
    private final Config config;

    public ChestShopAPI(final ServerCore instance) {
        this.instance = instance;
        this.config = new Config(instance.getDataFolder() + "/components/chestshop.yml", Config.YAML);

        for (final String owner : this.config.getSection("chestshop").getKeys(false)) {
            for (final String id : this.config.getSection("chestshop." + owner).getKeys(false)) {
                final String path = "chestshop." + owner + "." + id + ".";
                this.cachedChestShops.put(new Location(
                                this.config.getDouble(path + "sign.x"),
                                this.config.getDouble(path + "sign.y"),
                                this.config.getDouble(path + "sign.z"),
                                this.instance.getServer().getLevelByName(this.config.getString(path + "sign.level"))
                        ), new ChestShop(new Location(
                                this.config.getDouble(path + "sign.x"),
                                this.config.getDouble(path + "sign.y"),
                                this.config.getDouble(path + "sign.z"),
                                this.instance.getServer().getLevelByName(this.config.getString(path + "sign.level"))
                        ), new Location(
                                this.config.getDouble(path + "chest.x"),
                                this.config.getDouble(path + "chest.y"),
                                this.config.getDouble(path + "chest.z"),
                                this.instance.getServer().getLevelByName(this.config.getString(path + "chest.level"))
                        ), Long.parseLong(id), owner, ChestShop.ShopType.valueOf(this.config.getString(path + "type").toUpperCase()),
                                this.config.getDouble(path + "price"), this.config.getInt(path + "amount"), SyncAPI.ItemAPI.pureItemFromString(this.config.getString(path + "item")),
                                this.config.getString(path + "bank")
                        )
                );
            }
        }

        for (final String licenseOwner : this.config.getSection("licenses").getKeys(false)) {
            final ShopLicense.ShopLicenseType licenseType = ShopLicense.ShopLicenseType.valueOf(this.config.getString("licenses." + licenseOwner + ".license"));
            final int maxPossibleShops = this.config.getInt("licenses." + licenseOwner + ".shops");

            this.cachedLicenses.put(licenseOwner, new ShopLicense(licenseOwner, licenseType));
        }

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

    public HashMap<Location, ChestShop> cachedChestShops = new HashMap<>();
    private final HashMap<String, ShopLicense> cachedLicenses = new HashMap<>();

    public void createChestShop(final Location signLocation, final Location chestLocation, final Player owner, final ChestShop.ShopType shopType, final double price, final int sellAmount, final Item item, final String bankAccount) {
        final String path = "chestshop." + owner.getName() + "." + this.instance.createNumberId(15) + ".";

        this.config.set(path + "sign.x", signLocation.getX());
        this.config.set(path + "sign.y", signLocation.getY());
        this.config.set(path + "sign.z", signLocation.getZ());
        this.config.set(path + "sign.level", signLocation.getLevel().getName());

        this.config.set(path + "chest.x", chestLocation.getX());
        this.config.set(path + "chest.y", chestLocation.getY());
        this.config.set(path + "chest.z", chestLocation.getZ());
        this.config.set(path + "chest.level", chestLocation.getLevel().getName());

        this.config.set(path + "type", shopType.name().toUpperCase());
        this.config.set(path + "price", price);
        this.config.set(path + "amount", sellAmount);
        this.config.set(path + "item", SyncAPI.ItemAPI.pureItemToString(item));
        this.config.set(path + "bank", bankAccount);

        this.config.save();
        this.config.reload();

        this.cachedChestShops.put(signLocation, new ChestShop(signLocation, chestLocation, Long.parseLong(path.split("\\.")[2]), owner.getName(), shopType, price, sellAmount, item, bankAccount));
    }

    public void updateAmount(final ChestShop chestShop, final int update) {
        this.cachedChestShops.get(chestShop.getSignLocation()).setShopCount(update);

        this.config.set("chestshop." + chestShop.getOwner() + "." + chestShop.getId() + ".amount", update);
        this.config.save();
        this.config.reload();

        final BlockEntitySign blockEntitySign = (BlockEntitySign) chestShop.getSignLocation().getLevelBlock().getLevel().getBlockEntity(chestShop.getSignLocation());
        if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
            blockEntitySign.setText("§a[§2ChestShop§a]", "§0Kaufe: §2" + update + "x", "§f$ " + chestShop.getShopPrice(), "§2" + chestShop.getOwner());
        } else {
            blockEntitySign.setText("§a[§2ChestShop§a]", "§0Verkaufe: §2" + update + "x", "§f$ " + chestShop.getShopPrice(), "§2" + chestShop.getOwner());
        }
        blockEntitySign.scheduleUpdate();
    }

    public void updatePrice(final ChestShop chestShop, final double price) {
        this.cachedChestShops.get(chestShop.getSignLocation()).setShopPrice(price);

        this.config.set("chestshop." + chestShop.getOwner() + "." + chestShop.getId() + ".price", price);
        this.config.save();
        this.config.reload();

        final BlockEntitySign blockEntitySign = (BlockEntitySign) chestShop.getSignLocation().getLevelBlock().getLevel().getBlockEntity(chestShop.getSignLocation());
        if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
            blockEntitySign.setText("§a[§2ChestShop§a]", "§0Kaufe: §2" + chestShop.getShopCount() + "x", "§f$ " + price, "§2" + chestShop.getOwner());
        } else {
            blockEntitySign.setText("§a[§2ChestShop§a]", "§0Verkaufe: §2" + chestShop.getShopCount() + "x", "§f$ " + price, "§2" + chestShop.getOwner());
        }
        blockEntitySign.scheduleUpdate();
    }

    public void updateItem(final ChestShop chestShop, final String item) {
        this.cachedChestShops.get(chestShop.getSignLocation()).setItem(SyncAPI.ItemAPI.pureItemFromString(item));

        this.config.set("chestshop." + chestShop.getOwner() + "." + chestShop.getId() + ".item", item);
        this.config.save();
        this.config.reload();

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

    public void removeChestShop(final Location signLocation, final String owner, final long id) {
        final Map<String, Object> map = this.config.getSection("chestshop." + owner).getAllMap();
        map.remove(String.valueOf(id));
        this.config.set("chestshop." + owner, map);
        this.config.save();
        this.config.reload();

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
        } else return new ShopLicense(player, ShopLicense.ShopLicenseType.STANDARD);
    }

    public void setLicense(final String player, final ShopLicense.ShopLicenseType licenseType, final int maxPossibleShops) {
        this.config.set("licenses." + player + ".license", licenseType.name().toUpperCase());
        this.config.set("licenses." + player + ".shops", maxPossibleShops);
        this.config.save();
        this.config.reload();

        this.cachedLicenses.remove(player);
        this.cachedLicenses.put(player, new ShopLicense(player, licenseType));
    }

}
