package net.eltown.servercore.components.roleplay;

public enum RoleplayID {

    SHOP_MINER("servercore:shop.miner"),
    SHOP_NETHER("servercore:shop.nether"),
    SHOP_EXPLORER("servercore:shop.explorer"),
    SHOP_LUMBERJACK("servercore:shop.lumberjack"),
    SHOP_MONSTERUNTER("servercore:shop.monsterhunter"),
    SHOP_BLACKSMITH("servercore:shop.blacksmith"),

    BANKER("servercore:job.banker"),
    CRYPTO("servercore:job.crypto"),
    COOK("servercore:job.cook"),
    FARMER("servercore:job.farmer"),

    TOWNHALL_RECEPTION("servercore:townhall_reception"),
    TOWNHALL_TAXES("servercore:cassandra"),

    FEATURE_LOLA("servercore:lola"),
    FEATURE_AINARA("servercore:ainara"),

    ;

    private final String id;

    RoleplayID(final String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
