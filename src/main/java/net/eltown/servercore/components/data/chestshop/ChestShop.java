package net.eltown.servercore.components.data.chestshop;

import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class ChestShop {

    private final Location signLocation;
    private final Location chestLocation;
    private final long id;
    private final String owner;
    private final ShopType shopType;
    private double shopPrice;
    private int shopCount;
    private Item item;
    private String bankAccount;

    public enum ShopType {

        SELL,
        BUY,
        ADMIN

    }

}
