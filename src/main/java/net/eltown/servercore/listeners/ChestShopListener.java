package net.eltown.servercore.listeners;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockChest;
import cn.nukkit.block.BlockID;
import cn.nukkit.block.BlockWallSign;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.data.EntityMetadata;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.block.BlockPlaceEvent;
import cn.nukkit.event.block.SignChangeEvent;
import cn.nukkit.event.player.PlayerChunkRequestEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.DestroyBlockParticle;
import cn.nukkit.network.protocol.AddItemEntityPacket;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.chestshop.ChestShop;
import net.eltown.servercore.components.data.chestshop.ShopLicense;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.Cooldown;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class ChestShopListener implements Listener {

    private final ServerCore serverCore;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final SignChangeEvent event) {
        final Player player = event.getPlayer();

        if (this.serverCore.getChestShopAPI().cachedChestShops.get(event.getBlock().getLocation()) != null) {
            final ChestShop chestShop = this.serverCore.getChestShopAPI().cachedChestShops.get(event.getBlock().getLocation());
            if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
                event.setLine(0, "§c[§4ChestShop§c]");
                event.setLine(1, "§cKaufe: §4" + chestShop.getShopCount() + "x");
                event.setLine(2, "§c$ §4" + Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice()));
                event.setLine(3, "§4" + player.getName());
            } else {
                event.setLine(0, "§c[§4ChestShop§c]");
                event.setLine(1, "§cVerkaufe: §4" + chestShop.getShopCount() + "x");
                event.setLine(2, "§c$ §4" + Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice()));
                event.setLine(3, "§4" + player.getName());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final BlockPlaceEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        final Item item = event.getItem();

        if (item.getId() == ItemID.SIGN && item.getNamedTag() != null) {
            if (event.getBlockAgainst().getId() == BlockID.CHEST) {
                if (!this.chestIsUsed(event.getBlockAgainst())) {
                    final ShopLicense shopLicense = this.serverCore.getChestShopAPI().getPlayerLicense(player.getName());
                    final int chestShops = this.serverCore.getChestShopAPI().countPlayerChestShops(player.getName());

                    if (chestShops < shopLicense.getLicense().maxPossibleShops()) {
                        final String creator = item.getNamedTag().getString("shop_creator");
                        if (player.getName().equals(creator)) {
                            final String rawItem = item.getNamedTag().getString("shop_item");
                            final int amount = item.getNamedTag().getInt("shop_amount");
                            final double price = item.getNamedTag().getDouble("shop_price");
                            final ChestShop.ShopType type = ChestShop.ShopType.valueOf(item.getNamedTag().getString("shop_type").toUpperCase());

                            if (block instanceof BlockWallSign) {
                                final BlockEntitySign blockEntitySign = (BlockEntitySign) block.getLocation().getLevelBlock().getLevel().getBlockEntity(block.getLocation());
                                if (type == ChestShop.ShopType.BUY) {
                                    blockEntitySign.setText("§c[§4ChestShop§c]", "§cKaufe: §4" + amount + "x", "§c$ §4" + Economy.getAPI().getMoneyFormat().format(price), "§4" + player.getName());
                                } else {
                                    blockEntitySign.setText("§c[§4ChestShop§c]", "§cVerkaufe: §4" + amount + "x", "§c$ §4" + Economy.getAPI().getMoneyFormat().format(price), "§4" + player.getName());
                                }
                                blockEntitySign.scheduleUpdate();
                            }

                            this.serverCore.getChestShopAPI().cachedChestShops.put(event.getBlock().getLocation(), new ChestShop(event.getBlock().getLocation(),
                                    event.getBlockAgainst().getLocation(), -1, player.getName(), type, price, amount, SyncAPI.ItemAPI.pureItemFromString(rawItem), null));
                            player.sendMessage(Language.get("chestshop.create.bank.info"));
                        } else {
                            event.setCancelled(true);
                        }
                    } else {
                        player.sendMessage(Language.get("chestshop.create.too.many.shops"));
                        event.setCancelled(true);
                    }
                } else {
                    player.sendMessage(Language.get("chestshop.create.chest.already.used"));
                    event.setCancelled(true);
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    final Cooldown interactCooldown = new Cooldown(TimeUnit.MILLISECONDS.toMillis(250));
    final Cooldown messageCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(5));

    private final HashMap<String, String> cachedBankAccounts = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final PlayerInteractEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();

        if (event.getBlock() == null) return;

        if (block instanceof BlockWallSign) {
            if (this.serverCore.getChestShopAPI().cachedChestShops.get(block.getLocation()) != null) {
                final ChestShop chestShop = this.serverCore.getChestShopAPI().cachedChestShops.get(block.getLocation());
                if (chestShop != null && chestShop.getOwner().equals(player.getName()) && chestShop.getId() == -1) {

                    if (!this.interactCooldown.hasCooldown(player.getName())) {
                        final Item item = player.getInventory().getItemInHand();
                        if (item.getId() == ItemID.PAPER && item.getNamedTag().getString("bank_account") != null) {
                            final String account = item.getNamedTag().getString("bank_account");

                            Economy.getBankAPI().getAccount(account, bankAccount -> {
                                if (bankAccount != null) {
                                    final CustomForm form = new CustomForm.Builder("§7» §8Bankkonto-Login Verknüpfung")
                                            .addElement(new ElementLabel("§fKonto: §9" + account + "\n§fName: §9" + bankAccount.getDisplayName()))
                                            .addElement(new ElementInput("§fBitte gebe das Passwort des Kontos an, um eine neue Kontoverknüpfung zu erstellen.", "Passwort"))
                                            .onSubmit((g, h) -> {
                                                final String password = h.getInputResponse(1);

                                                if (password.equals(bankAccount.getPassword())) {
                                                    this.serverCore.getChestShopAPI().cachedChestShops.remove(block.getLocation());
                                                    this.serverCore.getChestShopAPI().createChestShop(chestShop.getSignLocation(), chestShop.getChestLocation(), player, chestShop.getShopType(),
                                                            chestShop.getShopPrice(), chestShop.getShopCount(), chestShop.getItem(), bankAccount.getAccount());

                                                    final BlockEntitySign blockEntitySign = (BlockEntitySign) chestShop.getSignLocation().getLevelBlock().getLevel().getBlockEntity(chestShop.getSignLocation());
                                                    if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
                                                        blockEntitySign.setText("§a[§2ChestShop§a]", "§0Kaufe: §2" + chestShop.getShopCount() + "x", "§f$ " + Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice()), "§2" + player.getName());
                                                    } else {
                                                        blockEntitySign.setText("§a[§2ChestShop§a]", "§0Verkaufe: §2" + chestShop.getShopCount() + "x", "§f$ " + Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice()), "§2" + player.getName());
                                                    }
                                                    blockEntitySign.scheduleUpdate();

                                                    this.serverCore.playSound(player, Sound.NOTE_HARP);

                                                    final Item displayItem = chestShop.getItem().clone();
                                                    displayItem.setCount(1);

                                                    final AddItemEntityPacket packet = new AddItemEntityPacket();
                                                    packet.entityRuntimeId = chestShop.getId();
                                                    packet.entityUniqueId = chestShop.getId();
                                                    packet.item = displayItem;
                                                    packet.x = (float) chestShop.getChestLocation().x + 0.5f;
                                                    packet.y = (float) chestShop.getChestLocation().y + 1f;
                                                    packet.z = (float) chestShop.getChestLocation().z + 0.5f;
                                                    packet.speedX = 0f;
                                                    packet.speedY = 0f;
                                                    packet.speedZ = 0f;
                                                    packet.metadata = new EntityMetadata()
                                                            .putLong(Entity.DATA_FLAGS, Entity.DATA_FLAG_IMMOBILE)
                                                            .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                                                            .putLong(Entity.DATA_SCALE, 4L);
                                                    Server.broadcastPacket(this.serverCore.getServer().getOnlinePlayers().values(), packet);
                                                    player.sendMessage(Language.get("chestshop.created"));
                                                } else {
                                                    player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                    this.serverCore.playSound(player, Sound.NOTE_BASS);
                                                }
                                            })
                                            .build();
                                    form.send(player);
                                } else {
                                    player.sendMessage(Language.get("roleplay.bank.invalid.account"));
                                    this.serverCore.playSound(player, Sound.NOTE_BASS);
                                }
                            });
                        } else {
                            player.sendMessage(Language.get("roleplay.bank.card.needed"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                        }
                    }
                    event.setCancelled(true);
                } else {
                    if (!this.interactCooldown.hasCooldown(player.getName()) && chestShop != null && chestShop.getId() != -1) {
                        final BlockEntityChest chest = (BlockEntityChest) chestShop.getChestLocation().getLevel().getBlockEntity(chestShop.getChestLocation());

                        if (player.getName().equals(chestShop.getOwner()) && event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
                            final CustomForm form = new CustomForm.Builder("§7» §8ChestShop bearbeiten")
                                    .addElement(new ElementLabel("§7» §fChestShopID: §9" + chestShop.getId()))
                                    .addElement(new ElementInput("§7» §fStückzahl bearbeiten:", "10", "" + chestShop.getShopCount()))
                                    .addElement(new ElementInput("§7» §fPreis bearbeiten:", "29.95", "" + Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice())))
                                    .addElement(new ElementToggle("§7» §fDas Item in deiner Hand zum neuen Kauf- oder Verkaufsitem setzen.", false))
                                    .onSubmit((g, h) -> {
                                        try {
                                            final int givenCount = Integer.parseInt(h.getInputResponse(1));
                                            final double givenPrice = Double.parseDouble(h.getInputResponse(2));
                                            final boolean updateItem = h.getToggleResponse(3);

                                            if (givenCount <= 0) throw new Exception("Invalid chest shop amount.");
                                            if (givenPrice < 0) throw new Exception("Invalid chest shop price.");

                                            if (givenCount != chestShop.getShopCount()) {
                                                this.serverCore.getChestShopAPI().updateAmount(chestShop, givenCount);
                                                this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                player.sendMessage(Language.get("chestshop.edit.amount"));
                                            }
                                            if (givenPrice != chestShop.getShopPrice()) {
                                                this.serverCore.getChestShopAPI().updatePrice(chestShop, givenPrice);
                                                this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                player.sendMessage(Language.get("chestshop.edit.price"));
                                            }
                                            if (updateItem) {
                                                if (!(player.getInventory().getItemInHand().getId() == 0)) {
                                                    this.serverCore.getChestShopAPI().updateItem(chestShop, SyncAPI.ItemAPI.pureItemToString(player.getInventory().getItemInHand()));
                                                    this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                    player.sendMessage(Language.get("chestshop.edit.item"));
                                                } else {
                                                    player.sendMessage(Language.get("chestshop.create.invalid.item"));
                                                    this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                                }
                                            }
                                        } catch (final Exception e) {
                                            player.sendMessage(Language.get("chestshop.create.invalid.input"));
                                            this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                        }
                                    })
                                    .build();
                            form.send(player);
                            this.serverCore.playSound(player, Sound.RANDOM_ORB);
                            event.setCancelled(true);
                            return;
                        } else if (player.getName().equals(chestShop.getOwner()) && event.getAction() == PlayerInteractEvent.Action.LEFT_CLICK_BLOCK) return;

                        final AtomicInteger count = new AtomicInteger();
                        if (chestShop.getShopType() == ChestShop.ShopType.BUY) {
                            for (final Item item : chest.getRealInventory().slots.values()) {
                                if (SyncAPI.ItemAPI.pureItemToString(item).equals(SyncAPI.ItemAPI.pureItemToString(chestShop.getItem()))) count.addAndGet(item.getCount());
                            }

                            if (count.get() >= chestShop.getShopCount()) {
                                final Item bankCard = player.getInventory().getItemInHand();
                                if (bankCard.getId() == ItemID.PAPER && bankCard.getNamedTag().getString("bank_account") != null) {
                                    final String account = bankCard.getNamedTag().getString("bank_account");

                                    Economy.getBankAPI().getAccount(account, bankAccount -> {
                                        if (bankAccount != null) {
                                            if (this.cachedBankAccounts.containsKey(player.getName())) {
                                                if (!this.cachedBankAccounts.get(player.getName()).equals(bankAccount.getAccount())) {
                                                    final CustomForm form = new CustomForm.Builder("§7» §8Bankkonto-Login")
                                                            .addElement(new ElementLabel("§fKonto: §9" + account + "\n§fName: §9" + bankAccount.getDisplayName()))
                                                            .addElement(new ElementLabel("Wenn du dich mit dieser Bankkarte anmeldest, kannst du mit dieser - bis du den Server verlässt - in ChestShops einkaufen, ohne das du das Passwort erneut angeben musst."))
                                                            .addElement(new ElementInput("§fBitte gebe das Passwort des Kontos an.", "Passwort"))
                                                            .onSubmit((g, h) -> {
                                                                final String password = h.getInputResponse(2);

                                                                if (password.equals(bankAccount.getPassword())) {
                                                                    this.cachedBankAccounts.remove(player.getName());
                                                                    this.cachedBankAccounts.put(player.getName(), bankAccount.getAccount());
                                                                    player.sendMessage(Language.get("chestshop.bank.login.success"));
                                                                    this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                                } else {
                                                                    player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                                    this.serverCore.playSound(player, Sound.NOTE_BASS);
                                                                }
                                                            })
                                                            .build();
                                                    form.send(player);
                                                    return;
                                                }
                                                if (bankAccount.getBalance() >= chestShop.getShopPrice()) {
                                                    final Item item = chestShop.getItem().clone();
                                                    item.setCount(chestShop.getShopCount());
                                                    if (player.getInventory().canAddItem(item)) {
                                                        Economy.getBankAPI().withdrawMoney(bankAccount.getAccount(), chestShop.getShopPrice());
                                                        Economy.getBankAPI().depositMoney(chestShop.getBankAccount(), chestShop.getShopPrice());

                                                        chest.getRealInventory().removeItem(item);
                                                        player.getInventory().addItem(item);

                                                        if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId()))
                                                            player.sendMessage(Language.get("chestshop.interact.bought.bank", item.getName(), chestShop.getShopCount(), Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice()), bankAccount.getDisplayName()));
                                                        this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                        chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.EMERALD_BLOCK)));
                                                    } else {
                                                        if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.space.self"));
                                                        this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                                        chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                                                    }
                                                } else {
                                                    if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.buy.no.money.bank"));
                                                    this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                                    chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                                                }
                                            } else {
                                                final CustomForm form = new CustomForm.Builder("§7» §8Bankkonto-Login")
                                                        .addElement(new ElementLabel("§fKonto: §9" + account + "\n§fName: §9" + bankAccount.getDisplayName()))
                                                        .addElement(new ElementLabel("Wenn du dich mit dieser Bankkarte anmeldest, kannst du mit dieser - bis du den Server verlässt - in ChestShops einkaufen, ohne das du das Passwort erneut angeben musst."))
                                                        .addElement(new ElementInput("§fBitte gebe das Passwort des Kontos an.", "Passwort"))
                                                        .onSubmit((g, h) -> {
                                                            final String password = h.getInputResponse(2);

                                                            if (password.equals(bankAccount.getPassword())) {
                                                                this.cachedBankAccounts.remove(player.getName());
                                                                this.cachedBankAccounts.put(player.getName(), bankAccount.getAccount());
                                                                player.sendMessage(Language.get("chestshop.bank.login.success"));
                                                                this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                            } else {
                                                                player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                                this.serverCore.playSound(player, Sound.NOTE_BASS);
                                                            }
                                                        })
                                                        .build();
                                                form.send(player);
                                            }
                                        } else {
                                            player.sendMessage(Language.get("roleplay.bank.invalid.account"));
                                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                                        }
                                    });
                                } else {
                                    Economy.getAPI().getMoney(player, money -> {
                                        if (money >= chestShop.getShopPrice()) {
                                            final Item item = chestShop.getItem().clone();
                                            item.setCount(chestShop.getShopCount());
                                            if (player.getInventory().canAddItem(item)) {
                                                Economy.getAPI().reduceMoney(player, chestShop.getShopPrice());
                                                Economy.getBankAPI().depositMoney(chestShop.getBankAccount(), chestShop.getShopPrice());

                                                chest.getRealInventory().removeItem(item);
                                                player.getInventory().addItem(item);

                                                if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.bought", item.getName(), chestShop.getShopCount(), Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice())));
                                                this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.EMERALD_BLOCK)));
                                            } else {
                                                if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.space.self"));
                                                this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                                chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                                            }
                                        } else {
                                            if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.money"));
                                            this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                            chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                                        }
                                    });
                                }
                            } else {
                                if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.out.of.stock"));
                                this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                            }
                        } else {
                            for (final Item item : player.getInventory().slots.values()) {
                                if (SyncAPI.ItemAPI.pureItemToString(item).equals(SyncAPI.ItemAPI.pureItemToString(chestShop.getItem()))) count.addAndGet(item.getCount());
                            }

                            if (count.get() >= chestShop.getShopCount()) {
                                final Item bankCard = player.getInventory().getItemInHand();
                                if (bankCard.getId() == ItemID.PAPER && bankCard.getNamedTag().getString("bank_account") != null) {
                                    final String account = bankCard.getNamedTag().getString("bank_account");

                                    Economy.getBankAPI().getAccount(account, sellAccount -> {
                                        Economy.getBankAPI().getAccount(chestShop.getBankAccount(), chestAccount -> {
                                            if (sellAccount != null && chestAccount != null) {
                                                if (this.cachedBankAccounts.containsKey(player.getName())) {
                                                    if (!this.cachedBankAccounts.get(player.getName()).equals(sellAccount.getAccount())) {
                                                        final CustomForm form = new CustomForm.Builder("§7» §8Bankkonto-Login")
                                                                .addElement(new ElementLabel("§fKonto: §9" + account + "\n§fName: §9" + sellAccount.getDisplayName()))
                                                                .addElement(new ElementLabel("Wenn du dich mit dieser Bankkarte anmeldest, kannst du mit dieser - bis du den Server verlässt - in ChestShops einkaufen, ohne das du das Passwort erneut angeben musst."))
                                                                .addElement(new ElementInput("§fBitte gebe das Passwort des Kontos an.", "Passwort"))
                                                                .onSubmit((g, h) -> {
                                                                    final String password = h.getInputResponse(2);

                                                                    if (password.equals(sellAccount.getPassword())) {
                                                                        this.cachedBankAccounts.remove(player.getName());
                                                                        this.cachedBankAccounts.put(player.getName(), sellAccount.getAccount());
                                                                        player.sendMessage(Language.get("chestshop.bank.login.success"));
                                                                        this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                                    } else {
                                                                        player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                                        this.serverCore.playSound(player, Sound.NOTE_BASS);
                                                                    }
                                                                })
                                                                .build();
                                                        form.send(player);
                                                        return;
                                                    }
                                                    if (chestAccount.getBalance() >= chestShop.getShopPrice()) {
                                                        final Item item = chestShop.getItem().clone();
                                                        item.setCount(chestShop.getShopCount());
                                                        if (chest.getRealInventory().canAddItem(item)) {
                                                            Economy.getBankAPI().withdrawMoney(chestAccount.getAccount(), chestShop.getShopPrice());
                                                            Economy.getBankAPI().depositMoney(sellAccount.getAccount(), chestShop.getShopPrice());

                                                            chest.getRealInventory().addItem(item);
                                                            player.getInventory().removeItem(item);

                                                            if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId()))
                                                                player.sendMessage(Language.get("chestshop.interact.sold.bank", item.getName(), chestShop.getShopCount(), Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice()), sellAccount.getDisplayName()));
                                                            this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                            chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.EMERALD_BLOCK)));
                                                        } else {
                                                            if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.space.self"));
                                                            this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                                            chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                                                        }
                                                    } else {
                                                        if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.buy.no.money.bank"));
                                                        this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                                        chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                                                    }
                                                } else {
                                                    final CustomForm form = new CustomForm.Builder("§7» §8Bankkonto-Login")
                                                            .addElement(new ElementLabel("§fKonto: §9" + account + "\n§fName: §9" + sellAccount.getDisplayName()))
                                                            .addElement(new ElementLabel("Wenn du dich mit dieser Bankkarte anmeldest, kannst du mit dieser - bis du den Server verlässt - in ChestShops einkaufen, ohne das du das Passwort erneut angeben musst."))
                                                            .addElement(new ElementInput("§fBitte gebe das Passwort des Kontos an.", "Passwort"))
                                                            .onSubmit((g, h) -> {
                                                                final String password = h.getInputResponse(2);

                                                                if (password.equals(sellAccount.getPassword())) {
                                                                    this.cachedBankAccounts.remove(player.getName());
                                                                    this.cachedBankAccounts.put(player.getName(), sellAccount.getAccount());
                                                                    player.sendMessage(Language.get("chestshop.bank.login.success"));
                                                                    this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                                } else {
                                                                    player.sendMessage(Language.get("roleplay.bank.invalid.password"));
                                                                    this.serverCore.playSound(player, Sound.NOTE_BASS);
                                                                }
                                                            })
                                                            .build();
                                                    form.send(player);
                                                }
                                            } else {
                                                player.sendMessage(Language.get("roleplay.bank.invalid.account"));
                                                this.serverCore.playSound(player, Sound.NOTE_BASS);
                                            }
                                        });
                                    });
                                } else {
                                    Economy.getBankAPI().getAccount(chestShop.getBankAccount(), bankAccount -> {
                                        if (bankAccount.getBalance() >= chestShop.getShopPrice()) {
                                            final Item item = chestShop.getItem().clone();
                                            item.setCount(chestShop.getShopCount());
                                            if (chest.getRealInventory().canAddItem(item)) {
                                                Economy.getBankAPI().withdrawMoney(chestShop.getBankAccount(), chestShop.getShopPrice());
                                                Economy.getAPI().addMoney(player, chestShop.getShopPrice());

                                                chest.getRealInventory().addItem(item);
                                                player.getInventory().removeItem(item);

                                                if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.sold", item.getName(), chestShop.getShopCount(), Economy.getAPI().getMoneyFormat().format(chestShop.getShopPrice())));
                                                this.serverCore.playSound(player, Sound.RANDOM_ORB);
                                                chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.EMERALD_BLOCK)));
                                            } else {
                                                if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.space.chest"));
                                                this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                                chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                                            }
                                        } else {
                                            if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.money.bank"));
                                            this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                            chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                                        }
                                    });
                                }
                            } else {
                                if (!this.messageCooldown.hasCooldown(player.getName() + "/" + chestShop.getId())) player.sendMessage(Language.get("chestshop.interact.no.items"));
                                this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                                chestShop.getChestLocation().getLevel().addParticle(new DestroyBlockParticle(chestShop.getChestLocation(), Block.get(BlockID.REDSTONE_BLOCK)));
                            }
                        }
                    }
                }
                event.setCancelled(true);
            }
        } else if (block instanceof BlockChest) {
            this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
                if (e.getChestLocation().equals(block.getLocation())) {
                    if (!e.getOwner().equals(player.getName()) && !player.isOp()) {
                        player.sendMessage(Language.get("chestshop.interact.chest"));
                        this.serverCore.playSound(player, Sound.ITEM_SHIELD_BLOCK);
                        event.setCancelled(true);
                    }
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(final BlockBreakEvent event) {
        final Player player = event.getPlayer();
        final Block block = event.getBlock();
        if (block instanceof BlockChest) {
            this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
                if (e.getChestLocation().equals(block.getLocation()) && e.getId() == -1) {
                    if (e.getOwner().equals(player.getName())) {
                        player.sendMessage(Language.get("chestshop.create.destroy"));
                    }
                    event.setCancelled(true);
                }
            });

            this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
                if (e.getChestLocation().equals(block.getLocation()) && e.getId() != -1) {
                    if (e.getOwner().equals(player.getName())) {
                        player.sendMessage(Language.get("chestshop.break.chest"));
                    }
                    event.setCancelled(true);
                }
            });

        } else if (block instanceof BlockWallSign) {
            if (this.serverCore.getChestShopAPI().cachedChestShops.get(block.getLocation()) != null && this.serverCore.getChestShopAPI().cachedChestShops.get(block.getLocation()).getId() == -1) {
                player.sendMessage(Language.get("chestshop.create.destroy"));
                event.setCancelled(true);
            } else {
                this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
                    if (e.getSignLocation().equals(block.getLocation())) {
                        if (e.getOwner().equals(player.getName()) || player.isOp()) {
                            final ModalForm form = new ModalForm.Builder("§7» §8ChestShop entfernen", "§cMöchtest du wirklich deinen ChestShop entfernen? Diese Aktion kann nicht rückgängig gemacht werden!",
                                    "§8» §aEntfernen", "§8» §cAbbrechen")
                                    .onYes(h -> {
                                        this.serverCore.getChestShopAPI().removeChestShop(e.getSignLocation(), e.getOwner(), e.getId());
                                        player.sendMessage(Language.get("chestshop.break.sign"));
                                    })
                                    .onNo(h -> {
                                    })
                                    .build();
                            form.send(player);
                        }
                        event.setCancelled(true);
                    }
                });
            }
        }
    }

    @EventHandler
    public void on(final PlayerChunkRequestEvent event) {
        final Player player = event.getPlayer();
        final int x = event.getChunkX();
        final int z = event.getChunkZ();


    }

    @EventHandler
    public void on(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        this.serverCore.getChestShopAPI().cachedChestShops.forEach((location, chestShop) -> {
            final Item displayItem = chestShop.getItem().clone();
            displayItem.setCount(1);

            final AddItemEntityPacket packet = new AddItemEntityPacket();
            packet.entityRuntimeId = chestShop.getId();
            packet.entityUniqueId = chestShop.getId();
            packet.item = displayItem;
            packet.x = (float) chestShop.getChestLocation().x + 0.5f;
            packet.y = (float) chestShop.getChestLocation().y + 1f;
            packet.z = (float) chestShop.getChestLocation().z + 0.5f;
            packet.speedX = 0f;
            packet.speedY = 0f;
            packet.speedZ = 0f;
            packet.metadata = new EntityMetadata()
                    .putLong(Entity.DATA_FLAGS, Entity.DATA_FLAG_IMMOBILE)
                    .putLong(Entity.DATA_LEAD_HOLDER_EID, -1)
                    .putLong(Entity.DATA_SCALE, 4L);
            player.dataPacket(packet);
        });
    }

    @EventHandler
    public void on(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        this.cachedBankAccounts.remove(player.getName());
    }

    private boolean chestIsUsed(final Block block) {
        final AtomicBoolean b = new AtomicBoolean(false);
        this.serverCore.getChestShopAPI().cachedChestShops.values().forEach(e -> {
            if (e.getChestLocation().equals(block.getLocation())) {
                b.set(true);
            }
        });
        return b.get();
    }

}
