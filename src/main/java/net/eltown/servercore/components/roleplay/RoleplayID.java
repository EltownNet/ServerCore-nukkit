package net.eltown.servercore.components.roleplay;

public enum RoleplayID {

    SHOP_MINER("servercore:shop.miner"),
    SHOP_NETHER("servercore:shop.nether"),
    SHOP_EXPLORER("servercore:shop.explorer"),
    SHOP_LUMBERJACK("servercore:shop.lumberjack"),
    SHOP_MONSTERUNTER("servercore:shop.monsterhunter"),
    SHOP_BLACKSMITH("servercore:shop.blacksmith"),

    ;

    private final String id;

    RoleplayID(final String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
