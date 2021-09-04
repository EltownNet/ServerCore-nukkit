package net.eltown.servercore.commands.feature;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.nbt.tag.CompoundTag;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.chestshop.ShopLicense;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.language.Language;

import java.util.Arrays;

public class ChestshopCommand extends PluginCommand<ServerCore> {

    public ChestshopCommand(final ServerCore owner) {
        super("chestshop", owner);
        this.setDescription("Erstelle einen neuen ChestShop");
        this.setAliases(Arrays.asList("cs", "shop").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            final Item item = player.getInventory().getItemInHand();

            if (item.getId() == 0) {
                player.sendMessage(Language.get("chestshop.create.invalid.item"));
                return true;
            }

            final ShopLicense shopLicense = this.getPlugin().getChestShopAPI().getPlayerLicense(player.getName());
            final int chestShops = this.getPlugin().getChestShopAPI().countPlayerChestShops(player.getName());

            if (chestShops < shopLicense.getMaxPossibleShops()) {
                final CustomForm form = new CustomForm.Builder("§7» §8ChestShop erstellen")
                        .addElement(new ElementLabel("§7» §fDeine Lizenz: §e" + shopLicense.getLicense().displayName() + "\n§7» §fDeine Shops: §e" + chestShops + "§f/§e" + shopLicense.getMaxPossibleShops() + "\n\n§7» §fItem: §9" + item.getName()))
                        .addElement(new ElementInput("§7» §fBitte gebe an, mit welcher Stückzahl du das Item verkaufen möchtest.", "16"))
                        .addElement(new ElementInput("§7» §fFür welchen Preis möchtest du das Item verkaufen?", "29.95"))
                        .addElement(new ElementDropdown("§7» §fIch verkaufe Items an Spieler: §eBUY\n§7» §fSpieler verkaufen Items an mich: §eSELL", Arrays.asList("BUY", "SELL"), 0))
                        .onSubmit((g, h) -> {
                            try {
                                final int amount = Integer.parseInt(h.getInputResponse(1));
                                final double price = Double.parseDouble(h.getInputResponse(2).replace(",", "."));
                                if (amount <= 0) throw new Exception("Invalid chest shop amount.");
                                if (price < 0) throw new Exception("Invalid chest shop price.");

                                final Item chestShopItem = Item.get(ItemID.SIGN, 0, 1);
                                chestShopItem.setNamedTag(new CompoundTag()
                                        .putString("shop_creator", player.getName())
                                        .putString("shop_item", SyncAPI.ItemAPI.pureItemToString(item))
                                        .putString("shop_type", h.getDropdownResponse(3).getElementContent())
                                        .putInt("shop_amount", amount)
                                        .putDouble("shop_price", price)
                                );
                                chestShopItem.setCustomName("§r§8» §eChestShop erstellen");
                                chestShopItem.setLore("§r§7Bitte platziere das Item an eine", "§r§7Kiste, um einen ChestShop zu erstellen.");
                                player.getInventory().addItem(chestShopItem);
                                player.sendMessage(Language.get("chestshop.create.info"));
                            } catch (final Exception e) {
                                player.sendMessage(Language.get("chestshop.create.invalid.input"));
                            }
                        })
                        .build();
                form.send(player);
            } else {
                player.sendMessage(Language.get("chestshop.create.too.many.shops"));
            }
        }
        return true;
    }

}
