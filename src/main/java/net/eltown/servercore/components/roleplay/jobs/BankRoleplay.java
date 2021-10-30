package net.eltown.servercore.components.roleplay.jobs;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.form.element.*;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import cn.nukkit.nbt.tag.CompoundTag;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.economy.components.bank.data.BankAccount;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.bank.BankCalls;
import net.eltown.servercore.components.entities.ModelEntity;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.List;

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

            Economy.getBankAPI().getAccount(account, bankAccount -> {
                if (bankAccount != null) {
                    final CustomForm form = new CustomForm.Builder("§7» §8Bankkonto-Login")
                            .addElement(new ElementLabel("§fKonto: §9" + account + "\n§fName: §9" + bankAccount.getDisplayName()))
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
            });
        } else {
            player.sendMessage(Language.get("roleplay.bank.card.needed"));
            this.serverCore.playSound(player, Sound.NOTE_BASS);
        }
    }

    public void openBankAccount(final Player player, final String account) {
        Economy.getBankAPI().getAccount(account, bankAccount -> {
            final SimpleForm form = new SimpleForm.Builder("§7» §8Bankkonto§1", "§fKonto: §9" + account + "\n§ffName: §9" + bankAccount.getDisplayName() + "\n§fInhaber: §9" + bankAccount.getOwner() + "\n\n§fGuthaben: §a$" + Economy.getAPI().getMoneyFormat().format(bankAccount.getBalance()) + "\n")
                    .addButton(new ElementButton("§8» §3Guthaben einzahlen", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/banker/deposit.png")), e -> this.openDepositMenu(e, bankAccount.getAccount()))
                    .addButton(new ElementButton("§8» §3Guthaben abheben", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/banker/withdraw.png")), e -> this.openWithdrawMenu(e, bankAccount.getAccount()))
                    .build();
            form.send(player);
        });
    }

    public void openDepositMenu(final Player player, final String account) {
        Economy.getBankAPI().getAccount(account, bankAccount -> {
            Economy.getAPI().getMoney(player, money -> {
                final CustomForm form = new CustomForm.Builder("§7» §8Guthaben einzahlen")
                        .addElement(new ElementLabel("§fKonto: §9" + account + "\n§fName: §9" + bankAccount.getDisplayName() + "\n§fInhaber: §9" + bankAccount.getOwner() + "\n\n§fGuthaben: §a$" + Economy.getAPI().getMoneyFormat().format(bankAccount.getBalance()) + "\n§fBargeld: §a$" + Economy.getAPI().getMoneyFormat().format(money) + "\n"))
                        .addElement(new ElementInput("Bitte gebe an, wie viel Geld du auf dieses Konto einzahlen möchtest.", "3.99"))
                        .onSubmit((g, h) -> {
                            try {
                                final double amount = Double.parseDouble(h.getInputResponse(1));
                                if (amount <= 0) throw new Exception("Invalid bank interact amount");

                                if (money >= amount) {
                                    Economy.getAPI().reduceMoney(player, amount);
                                    Economy.getBankAPI().depositMoney(account, amount);
                                    Economy.getBankAPI().insertBankLog(account, "Geld eingezahlt", "§7" + player.getName() + " hat $" + amount + " eingezahlt.");
                                    player.sendMessage(Language.get("roleplay.bank.deposit.success", Economy.getAPI().getMoneyFormat().format(amount), Economy.getAPI().getMoneyFormat().format(bankAccount.getBalance() + amount)));
                                    this.serverCore.playSound(player, Sound.NOTE_PLING);
                                } else {
                                    player.sendMessage(Language.get("roleplay.bank.no.money"));
                                    this.serverCore.playSound(player, Sound.NOTE_BASS);
                                }
                            } catch (final Exception e) {
                                player.sendMessage(Language.get("roleplay.bank.invalid.input"));
                                this.serverCore.playSound(player, Sound.NOTE_BASS);
                            }
                        })
                        .build();
                form.send(player);
            });
        });
    }

    public void openWithdrawMenu(final Player player, final String account) {
        Economy.getBankAPI().getAccount(account, bankAccount -> {
            final CustomForm form = new CustomForm.Builder("§7» §8Guthaben abheben")
                    .addElement(new ElementLabel("§fKonto: §9" + account + "\n§fName: §9" + bankAccount.getDisplayName() + "\n§fInhaber: §9" + bankAccount.getOwner() + "\n\n§fGuthaben: §a$" + Economy.getAPI().getMoneyFormat().format(bankAccount.getBalance()) + "\n"))
                    .addElement(new ElementInput("Bitte gebe an, wie viel Geld du von diesem Konto abheben möchtest.", "3.99"))
                    .onSubmit((g, h) -> {
                        try {
                            final double amount = Double.parseDouble(h.getInputResponse(1));
                            if (amount <= 0) throw new Exception("Invalid bank interact amount");

                            if (bankAccount.getBalance() >= amount) {
                                Economy.getAPI().addMoney(player, amount);
                                Economy.getBankAPI().withdrawMoney(account, amount);
                                Economy.getBankAPI().insertBankLog(account, "Geld abgehoben", "§7" + player.getName() + " hat $" + amount + " abgehoben.");
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
        });
    }

    public void openBankManager(final Player player) {
        final SimpleForm form = new SimpleForm.Builder("§7» §8Banker Chris", "")
                .addButton(new ElementButton("§8» §fNeues Konto eröffnen", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/banker/create_account.png")), this::openCreateBankAccount)
                .addButton(new ElementButton("§8» §fBankkonto Beratung", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/banker/manage-account.png")), this::openBankManagerService)
                .addButton(new ElementButton("§8» §fBankkarte verloren", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/banker/forgot-password.png")), this::openNewBankCard)
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

    public void openBankManagerService(final Player player) {
        final Item item = player.getInventory().getItemInHand();
        if (item.getId() == ItemID.PAPER && item.getNamedTag().getString("bank_account") != null) {
            final String account = item.getNamedTag().getString("bank_account");

            Economy.getBankAPI().getAccount(account, bankAccount -> {
                if (bankAccount != null) {
                    if (bankAccount.getOwner().equals(player.getName())) {
                        final SimpleForm select = new SimpleForm.Builder("§7» §8Service wählen", "Bitte suche dir eines der folgenden Aktionen aus, um fortzufahren.\n\n§fKonto: §9" + bankAccount.getAccount() + "\n§fName: §9" + bankAccount.getDisplayName())
                                .addButton(new ElementButton("§8» §9Kontoaktivität", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/banker/manage-account-log.png")), e -> this.openBankManagerAccountLog(player, bankAccount))
                                .addButton(new ElementButton("§8» §9Bankkarte", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/banker/manage-account.png")), e -> this.openBankManagerAccountCard(player, bankAccount))
                                .addButton(new ElementButton("§8» §9Einstellungen", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/banker/manage-account-settings.png")), e -> this.openBankManagerAccountSettings(player, bankAccount))
                                .build();
                        select.send(player);
                    } else {
                        player.sendMessage(Language.get("roleplay.bank.invalid.user"));
                        this.serverCore.playSound(player, Sound.NOTE_BASS);
                    }
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

    public void openBankManagerAccountLog(final Player player, final BankAccount bankAccount) {
        final SimpleForm.Builder logs = new SimpleForm.Builder("§7» §8Kontoaktivität", "§fKonto: §9" + bankAccount.getAccount() + "\n§fName: §9" + bankAccount.getDisplayName());

        bankAccount.getBankLogs().forEach(e -> {
            logs.addButton(new ElementButton(e.getTitle() + "\n§7" + e.getDate()), g -> {
                final SimpleForm details = new SimpleForm.Builder("§7» §8Kontoaktivität", "§fKonto: §9" + bankAccount.getAccount() + "\n§fName: §9" + bankAccount.getDisplayName() + "\n\n" +
                        "§fLogID: §1" + e.getLogId() + "\n" + "§fAktion: §1" + e.getTitle() + "\n" + "§fBeschreibung: §1" + e.getDetails() + "\n" + "§fDatum: §1" + e.getDate() + "\n\n")
                        .addButton(new ElementButton("§8» §cZurück"), h -> this.openBankManagerAccountLog(player, bankAccount))
                        .build();
                details.send(g);
            });
        });

        logs.addButton(new ElementButton("§8» §cZurück"), this::openBankManagerService);

        logs.build().send(player);
    }

    public void openBankManagerAccountCard(final Player player, final BankAccount bankAccount) {
        final ModalForm form = new ModalForm.Builder("§7» §8Neue Bankkarte", "§fMöchtest du für das Konto §9" + bankAccount.getAccount() + " §feine neue Bankkarte beanspruchen?" +
                "\n\n§fVerwaltungsgebühr: §a$20\n§fBankkarte: §a$35\n\n§f§lZu zahlen: §r§a$55", "§8» §aBeanspruchen", "§8» §cZurück")
                .onYes(e -> Economy.getAPI().getMoney(player, money -> {
                    if (money >= 55) {
                        Economy.getAPI().reduceMoney(player, 55);
                        this.giveBankCard(player, bankAccount.getAccount());
                        Economy.getBankAPI().insertBankLog(bankAccount.getAccount(), "Neue Karte eingerichtet", "§7" + player.getName() + " hat für dieses Konto eine neue Karte einrichten lassen.");
                        player.sendMessage(Language.get("roleplay.bank.card.received"));
                        this.serverCore.playSound(player, Sound.NOTE_PLING);
                    } else {
                        player.sendMessage(Language.get("roleplay.bank.no.money"));
                        this.serverCore.playSound(player, Sound.NOTE_BASS);
                    }
                }))
                .onNo(this::openBankManagerService)
                .build();
        form.send(player);
    }

    public void openBankManagerAccountSettings(final Player player, final BankAccount bankAccount) {
        final CustomForm form = new CustomForm.Builder("§7» §8Kontoeinstellungen")
                .addElement(new ElementInput("Anzeigename des Kontos:", bankAccount.getAccount(), bankAccount.getDisplayName()))
                .addElement(new ElementInput("Passwort des Kontos:", bankAccount.getPassword(), bankAccount.getPassword()))
                .addElement(new ElementToggle("Möchtest du das Passwort ändern? §cDas Ändern des Passworts wird sofort übernommen. Das Konto wird allerdings nicht aus aktiven Verknüpfungen getrennt.", false))
                .onSubmit((g, h) -> {
                    final String displayName = h.getInputResponse(0);
                    final String password = h.getInputResponse(1);
                    final boolean change = h.getToggleResponse(2);

                    if (!bankAccount.getDisplayName().equals(displayName)) {
                        Economy.getBankAPI().changeDisplayName(bankAccount.getAccount(), displayName);
                        Economy.getBankAPI().insertBankLog(bankAccount.getAccount(), "Kontoname geändert", "§7" + player.getName() + " hat den Anzeigename des Kontos von " + bankAccount.getDisplayName() + " zu " + displayName + " geändert.");
                    }

                    if (change) {
                        if (!bankAccount.getPassword().equals(password)) {
                            try {
                                if (!(password.length() == 4)) throw new Exception("Password too long");
                                final int check = Integer.parseInt(password);
                                Economy.getBankAPI().changePassword(bankAccount.getAccount(), password);
                                Economy.getBankAPI().insertBankLog(bankAccount.getAccount(), "Passwort geändert", "§7" + player.getName() + " hat das Passwort des Kontos von " + bankAccount.getPassword() + " zu " + password + " geändert.");
                            } catch (final Exception e) {
                                player.sendMessage(Language.get("roleplay.bank.settings.invalid.password"));
                                this.serverCore.playSound(player, Sound.NOTE_BASS);
                                return;
                            }
                        }
                    }

                    player.sendMessage(Language.get("roleplay.bank.settings.updated"));
                    this.serverCore.playSound(player, Sound.NOTE_PLING);
                })
                .build();
        form.send(player);
    }

    public void openNewBankCard(final Player player) {
        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (BankCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_BANKACCOUNTS_BY_PLAYER:
                    final List<String> list = Arrays.asList(delivery.getData()[1].split("#"));

                    final CustomForm form = new CustomForm.Builder("§7» §8Bankkarte verloren")
                            .addElement(new ElementDropdown("Bitte wähle eines deiner Konten aus, um eine neue Bankkarte zu beantragen.", list))
                            .onSubmit((g, h) -> {
                                final String account = h.getDropdownResponse(0).getElementContent();
                                final ModalForm form1 = new ModalForm.Builder("§7» §8Neue Bankkarte", "§fMöchtest du für das Konto §9" + account + " §feine neue Bankkarte beanspruchen?" +
                                        "\n\n§fVerwaltungsgebühr: §a$20\n§fBankkarte: §a$35\n\n§f§lZu zahlen: §r§a$55", "§8» §aBeanspruchen", "§8» §cZurück")
                                        .onYes(e -> Economy.getAPI().getMoney(player, money -> {
                                            if (money >= 55) {
                                                Economy.getAPI().reduceMoney(player, 55);
                                                this.giveBankCard(player, account);
                                                Economy.getBankAPI().insertBankLog(account, "Neue Karte eingerichtet", "§7" + player.getName() + " hat für dieses Konto eine neue Karte einrichten lassen.");
                                                player.sendMessage(Language.get("roleplay.bank.card.received"));
                                                this.serverCore.playSound(player, Sound.NOTE_PLING);
                                            } else {
                                                player.sendMessage(Language.get("roleplay.bank.no.money"));
                                                this.serverCore.playSound(player, Sound.NOTE_BASS);
                                            }
                                        }))
                                        .onNo(this::openBankManager)
                                        .build();
                                form1.send(player);
                            })
                            .build();
                    form.send(player);
                    break;
            }
        }, Queue.BANK_CALLBACK, BankCalls.REQUEST_BANKACCOUNTS_BY_PLAYER.name(), player.getName());
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
