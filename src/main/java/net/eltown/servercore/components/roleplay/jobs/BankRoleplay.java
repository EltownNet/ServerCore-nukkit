package net.eltown.servercore.components.roleplay.jobs;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.nbt.tag.CompoundTag;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.economy.components.bank.data.BankAccount;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.entities.ModelEntity;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.RoleplayID;

public class BankRoleplay {

    private final ServerCore serverCore;

    public BankRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        serverCore.getServer().getPluginManager().registerEvents(new BankListener(this), serverCore);
    }

    public void openLogin(final Player player) {
        final Item item = player.getInventory().getItemInHand();
        if (item.getId() == ItemID.PAPER && item.getNamedTag().getString("bank_account") != null) {
            final String account = item.getNamedTag().getString("bank_account");

            final BankAccount bankAccount = Economy.getBankAPI().getAccount(account);
            if (bankAccount != null) {
                final CustomForm form = new CustomForm.Builder("§7» §8Bankkonto-Login")
                        .addElement(new ElementLabel("§fKonto: §9" + account))
                        .addElement(new ElementInput("§fBitte gebe das Passwort des Kontos an.", "Passwort"))
                        .onSubmit((g, h) -> {
                            final String password = h.getInputResponse(1);

                            if (password.equals(bankAccount.getPassword())) {
                                this.openBankAccount(player, bankAccount.getAccount());
                                this.serverCore.playSound(player, Sound.NOTE_PLING);
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
        } else {
            player.sendMessage(Language.get("roleplay.bank.card.needed"));
            this.serverCore.playSound(player, Sound.NOTE_BASS);
        }
    }

    public void openBankAccount(final Player player, final String account) {
        final BankAccount bankAccount = Economy.getBankAPI().getAccount(account);
        final SimpleForm form = new SimpleForm.Builder("§7» §8Bankkonto", "§fKonto: §9" + bankAccount.getAccount() + "\n§fInhaber: §9" + bankAccount.getOwner() + "\n\n§fGuthaben: §a$" + Economy.getAPI().getMoneyFormat().format(bankAccount.getBalance()) + "\n")
                .addButton(new ElementButton("§8» §3Guthaben einzahlen"), e -> this.openDepositMenu(e, bankAccount.getAccount()))
                .addButton(new ElementButton("§8» §3Guthaben abheben"), e -> this.openWithdrawMenu(e, bankAccount.getAccount()))
                .build();
        form.send(player);
    }

    public void openDepositMenu(final Player player, final String account) {
        final BankAccount bankAccount = Economy.getBankAPI().getAccount(account);

        final CustomForm form = new CustomForm.Builder("§7» §8Guthaben einzahlen")
                .addElement(new ElementLabel("§fKonto: §9" + bankAccount.getAccount() + "\n§fInhaber: §9" + bankAccount.getOwner() + "\n\n§fGuthaben: §a$" + Economy.getAPI().getMoneyFormat().format(bankAccount.getBalance()) + "\n"))
                .addElement(new ElementInput("Bitte gebe an, wie viel Geld du auf dieses Konto einzahlen möchtest.", "3.99"))
                .onSubmit((g, h) -> {
                    try {
                        final double amount = Double.parseDouble(h.getInputResponse(1));
                        if (amount <= 0) throw new Exception("Invalid bank interact amount");

                        Economy.getAPI().getMoney(player, money -> {
                            if (money >= amount) {
                                Economy.getAPI().reduceMoney(player, amount);
                                Economy.getBankAPI().depositMoney(account, amount);
                                player.sendMessage(Language.get("roleplay.bank.deposit.success", Economy.getAPI().getMoneyFormat().format(amount), Economy.getAPI().getMoneyFormat().format(bankAccount.getBalance() + amount)));
                                this.serverCore.playSound(player, Sound.NOTE_PLING);
                            } else {
                                player.sendMessage(Language.get("roleplay.bank.no.money"));
                                this.serverCore.playSound(player, Sound.NOTE_BASS);
                            }
                        });
                    } catch (final Exception e) {
                        player.sendMessage(Language.get("roleplay.bank.invalid.input"));
                        this.serverCore.playSound(player, Sound.NOTE_BASS);
                    }
                })
                .build();
        form.send(player);
    }

    public void openWithdrawMenu(final Player player, final String account) {
        final BankAccount bankAccount = Economy.getBankAPI().getAccount(account);

        final CustomForm form = new CustomForm.Builder("§7» §8Guthaben abheben")
                .addElement(new ElementLabel("§fKonto: §9" + bankAccount.getAccount() + "\n§fInhaber: §9" + bankAccount.getOwner() + "\n\n§fGuthaben: §a$" + Economy.getAPI().getMoneyFormat().format(bankAccount.getBalance()) + "\n"))
                .addElement(new ElementInput("Bitte gebe an, wie viel Geld du von diesem Konto abheben möchtest.", "3.99"))
                .onSubmit((g, h) -> {
                    try {
                        final double amount = Double.parseDouble(h.getInputResponse(1));
                        if (amount <= 0) throw new Exception("Invalid bank interact amount");

                        if (bankAccount.getBalance() >= amount) {
                            Economy.getAPI().addMoney(player, amount);
                            Economy.getBankAPI().withdrawMoney(account, amount);
                            player.sendMessage(Language.get("roleplay.bank.withdraw.success", Economy.getAPI().getMoneyFormat().format(amount), Economy.getAPI().getMoneyFormat().format(bankAccount.getBalance() - amount)));
                            this.serverCore.playSound(player, Sound.NOTE_PLING);
                        }
                    } catch (final Exception e) {
                        player.sendMessage(Language.get("roleplay.bank.invalid.input"));
                        this.serverCore.playSound(player, Sound.NOTE_BASS);
                    }
                })
                .build();
        form.send(player);
    }

    public void openBankManager(final Player player) {
        final SimpleForm form = new SimpleForm.Builder("§7» §8Banker Chris", "")
                .addButton(new ElementButton("§8» §fNeues Konto eröffnen"), this::openCreateBankAccount)
                .addButton(new ElementButton("§8» §fBankkonto Beratung"), e -> {})
                .build();
        form.send(player);
    }

    public void openCreateBankAccount(final Player player) {
        final ModalForm form = new ModalForm.Builder("§7» §8Neues Bankkonto eröffnen", "Möchtest du wirklich ein neues Bankkonto eröffnen?\n\n§fVerwaltungsgebühr: §a$120\n§fBankkarte: §a$35\n\n§f§lZu zahlen: §r§a$155",
                "§8» §aErstellen", "§8» §cAbbrechen")
                .onYes(e -> Economy.getAPI().getMoney(player, money -> {
                    if (money >= 155) {
                        Economy.getAPI().reduceMoney(player, 155);
                        Economy.getBankAPI().createBankAccount(player.getName(), "A", (password, account) -> {
                            player.sendMessage(Language.get("roleplay.bank.account.created", password));
                            this.serverCore.playSound(player, Sound.NOTE_PLING);
                            this.giveBankCard(player, account);
                        });
                    } else {
                        player.sendMessage(Language.get("roleplay.bank.no.money"));
                        this.serverCore.playSound(player, Sound.NOTE_BASS);
                    }
                }))
                .onNo(this::openBankManager)
                .build();
        form.send(player);
    }

    public void giveBankCard(final Player player, final String account) {
        final Item item = Item.get(ItemID.PAPER, 0, 1);
        item.setNamedTag(new CompoundTag().putString("bank_account", account));
        item.setCustomName("§r§8» §bBankkarte");
        item.setLore("§r§7" + account);
        player.getInventory().addItem(item);
    }

    @RequiredArgsConstructor
    public static class BankListener implements Listener {

        private final BankRoleplay bankRoleplay;

        @EventHandler
        public void on(final PlayerInteractEntityEvent event) {
            final Player player = event.getPlayer();
            if (event.getEntity() instanceof ModelEntity) {
                if (event.getEntity().getName().equals("Bankautomat")) this.bankRoleplay.openLogin(player);
            } else if (event.getEntity().namedTag.exist("npc_id")) {
                final String npcId = event.getEntity().namedTag.getString("npc_id");
                if (npcId.equals(RoleplayID.BANKER.id())) this.bankRoleplay.openBankManager(player);
            }
        }

        @EventHandler
        public void on(final EntityDamageByEntityEvent event) {
            final Entity entity = event.getEntity();
            if (event.getDamager() instanceof Player) {
                final Player player = (Player) event.getDamager();
                if (entity instanceof ModelEntity) {
                    if (entity.getName().equals("Bankautomat")) this.bankRoleplay.openLogin(player);
                } else if (event.getEntity().namedTag.exist("npc_id")) {
                    final String npcId = event.getEntity().namedTag.getString("npc_id");
                    if (npcId.equals(RoleplayID.BANKER.id())) this.bankRoleplay.openBankManager(player);
                }
            }
        }
    }

}