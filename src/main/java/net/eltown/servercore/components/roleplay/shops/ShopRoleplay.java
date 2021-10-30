package net.eltown.servercore.components.roleplay.shops;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.form.element.*;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.item.enchantment.Enchantment;
import cn.nukkit.level.Sound;
import cn.nukkit.network.protocol.PlaySoundPacket;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.enchantments.CustomEnchantment.EnchantmentID;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.listeners.EventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class ShopRoleplay {

    private final ServerCore serverCore;

    public ShopRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        serverCore.getServer().getPluginManager().registerEvents(new ShopListener(this), serverCore);
    }

    public final List<String> openQueue = new ArrayList<>();

    private void openItemShop(final Player player, final int[] id, final double price) {
        final SimpleForm.Builder simpleForm = new SimpleForm.Builder("§7» §8" + Item.get(id[0], id[1]).getName(), "Bitte wähle, ob du das Item kaufen oder verkaufen möchtest.");
        simpleForm.addButton(new ElementButton("§8» §aKaufen\n§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8pro Item"), o -> {
            final CustomForm customForm = new CustomForm.Builder("§7» §8" + Item.get(id[0], id[1]).getName())
                    .addElement(new ElementInput("Bitte gebe an, wie viel du von diesem Item §akaufen §fmöchtest.", "64", "64"))
                    .onSubmit((g, h) -> {
                        try {
                            final int i = Integer.parseInt(h.getInputResponse(0));
                            if (i <= 0) throw new Exception("Invalid item amount");

                            Economy.getShopAPI().getCurrentPrice(id, i, finalPrice -> {
                                finalPrice = finalPrice < i * 0.10 ? i * 0.10 : finalPrice;

                                Double finalPrice1 = finalPrice;
                                final ModalForm modalForm = new ModalForm.Builder("§7» §8Kaufbestätigung", "Möchtest du §9" + i + "x " + Item.get(id[0], id[1]).getName() + " §ffür"
                                        + " §a$" + Economy.getAPI().getMoneyFormat().format(finalPrice) + " §fkaufen?", "§7» §aKaufen", "§7» §cAbbrechen")
                                        .onYes(l -> {
                                            if (!player.getInventory().canAddItem(Item.get(id[0], id[1], i))) {
                                                player.sendMessage(Language.get("roleplay.shop.item.inventory.full"));
                                                this.playSound(player, Sound.NOTE_BASS);
                                                return;
                                            }

                                            Economy.getAPI().getMoney(player, money -> {
                                                if (money >= finalPrice1) {
                                                    Economy.getAPI().reduceMoney(player, finalPrice1);
                                                    Economy.getShopAPI().sendBought(id, i);
                                                    player.getInventory().addItem(Item.get(id[0], id[1], i));
                                                    player.sendMessage(Language.get("roleplay.shop.item.bought", i, Item.get(id[0], id[1], i).getName(), Economy.getAPI().getMoneyFormat().format(finalPrice1)));
                                                    this.playSound(player, Sound.UI_STONECUTTER_TAKE_RESULT);
                                                } else {
                                                    player.sendMessage(Language.get("roleplay.shop.item.not.enough.money"));
                                                    this.playSound(player, Sound.NOTE_BASS);
                                                }
                                            });
                                        })
                                        .onNo(l -> this.playSound(l, Sound.NOTE_BASS))
                                        .build();
                                modalForm.send(player);
                            });
                        } catch (final Exception e) {
                            player.sendMessage(Language.get("roleplay.shop.item.invalid.amount"));
                            this.playSound(player, Sound.NOTE_BASS);
                        }
                    })
                    .build();
            customForm.send(player);
        });
        if (price < 0.05) {
            simpleForm.addButton(new ElementButton("§8» §cVerkaufen\n§fKein Verkauf"), o -> {
                player.sendMessage(Language.get("roleplay.shop.item.no.sell"));
            });
        } else {
            simpleForm.addButton(new ElementButton("§8» §cVerkaufen\n§f$" + Economy.getAPI().getMoneyFormat().format(this.getSellPrice(price)) + " §8pro Item"), o -> {
                final int count = this.countInventoryItems(player, id);
                final CustomForm customForm = new CustomForm.Builder("§7» §8" + Item.get(id[0], id[1]).getName())
                        .addElement(new ElementInput("Bitte gebe an, wie viel du von diesem Item §cverkaufen §fmöchtest.", "64", "64"))
                        .addElement(new ElementToggle("Alle Items dieser Art aus meinem Inventar verkaufen: §7" + count, false))
                        .onSubmit((g, h) -> {
                            try {
                                final boolean b = h.getToggleResponse(1);
                                if (count <= 0) throw new Exception("Invalid item amount");
                                if (b) {
                                    Economy.getShopAPI().getCurrentPrice(id, count, finalPrice -> {
                                        final ModalForm modalForm = new ModalForm.Builder("§7» §8Verkaufsbestätigung", "Möchtest du §9" + count + "x " + Item.get(id[0], id[1]).getName() + " §ffür"
                                                + " §a$" + Economy.getAPI().getMoneyFormat().format(this.getSellPrice(finalPrice)) + " §fverkaufen?", "§7» §aVerkaufen", "§7» §cAbbrechen")
                                                .onYes(l -> {
                                                    final Item item = Item.get(id[0], id[1], count);
                                                    player.getInventory().removeItem(item);
                                                    Economy.getAPI().addMoney(player, this.getSellPrice(finalPrice));
                                                    Economy.getShopAPI().sendSold(id, count);
                                                    player.sendMessage(Language.get("roleplay.shop.item.sold", count, Item.get(id[0], id[1], count).getName(), Economy.getAPI().getMoneyFormat().format(this.getSellPrice(finalPrice))));
                                                    this.playSound(player, Sound.UI_STONECUTTER_TAKE_RESULT);
                                                })
                                                .onNo(l -> this.playSound(l, Sound.NOTE_BASS))
                                                .build();
                                        modalForm.send(player);
                                    });
                                } else {
                                    final int i = Integer.parseInt(h.getInputResponse(0));
                                    if (i <= 0) throw new Exception("Invalid item amount");

                                    Economy.getShopAPI().getCurrentPrice(id, i, finalPrice -> {
                                        final ModalForm modalForm = new ModalForm.Builder("§7» §8Verkaufsbestätigung", "Möchtest du §9" + i + "x " + Item.get(id[0], id[1]).getName() + " §ffür"
                                                + " §a$" + Economy.getAPI().getMoneyFormat().format(this.getSellPrice(finalPrice)) + " §fverkaufen?", "§7» §aVerkaufen", "§7» §cAbbrechen")
                                                .onYes(l -> {
                                                    if (count < i) {
                                                        player.sendMessage(Language.get("roleplay.shop.item.inventory.invalid"));
                                                        this.playSound(player, Sound.NOTE_BASS);
                                                        return;
                                                    }
                                                    final Item item = Item.get(id[0], id[1], i);
                                                    player.getInventory().removeItem(item);
                                                    Economy.getAPI().addMoney(player, this.getSellPrice(finalPrice));
                                                    Economy.getShopAPI().sendSold(id, i);
                                                    player.sendMessage(Language.get("roleplay.shop.item.sold", i, Item.get(id[0], id[1], i).getName(), Economy.getAPI().getMoneyFormat().format(this.getSellPrice(finalPrice))));
                                                    this.playSound(player, Sound.UI_STONECUTTER_TAKE_RESULT);
                                                })
                                                .onNo(l -> this.playSound(l, Sound.NOTE_BASS))
                                                .build();
                                        modalForm.send(player);
                                    });
                                }
                            } catch (final Exception e) {
                                player.sendMessage(Language.get("roleplay.shop.item.invalid.amount"));
                                this.playSound(player, Sound.NOTE_BASS);
                            }
                        })
                        .build();
                customForm.send(player);
            });
        }
        if (player.isOp()) {
            simpleForm.addButton(new ElementButton("§8» §9Preis bearbeiten"), e -> {
                final CustomForm form = new CustomForm.Builder("§7» §8" + Item.get(id[0], id[1]).getName())
                        .addElement(new ElementInput("Bitte gebe den neuen Preis für dieses Item an.", "5.95", Economy.getAPI().getMoneyFormat().format(price)))
                        .onSubmit((g, h) -> {
                            try {
                                final double newPrice = Double.parseDouble(h.getInputResponse(0).replace(",", "."));
                                Economy.getShopAPI().setPrice(id, newPrice);
                                player.sendMessage("§fDer Preis für das Item §9" + Item.get(id[0], id[1]).getName() + " §fwurde auf §9$" + Economy.getAPI().getMoneyFormat().format(newPrice) + " §fgesetzt.");
                            } catch (final Exception exception) {
                                player.sendMessage("Bitte gebe einen gültigen Preis an.");
                            }
                        })
                        .build();
                form.send(player);
            });
        }
        simpleForm.build().send(player);
    }

    private int countInventoryItems(final Player player, final int[] id) {
        final AtomicInteger i = new AtomicInteger(0);
        player.getInventory().getContents().forEach((g, h) -> {
            if (h.getId() == id[0] && h.getDamage() == id[1]) {
                i.addAndGet(h.getCount());
            }
        });
        return i.get();
    }

    private final List<int[]> woodShop = new ArrayList<>(Arrays.asList(
            new int[]{17, 0}, new int[]{17, 1}, new int[]{17, 2},
            new int[]{17, 3}, new int[]{162, 0}, new int[]{162, 1}
    ));

    private final List<ChainMessage> woodTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Guten Tag, §a%p§7! Ich habe viele hochwertige Holzsorten in meinem Sortiment. Schau dich gerne um!", 5),
            new ChainMessage("Man sagt, es gäbe bei mir das beste Holz!", 3),
            new ChainMessage("Lass dich nicht von mir stören!", 3),
            new ChainMessage("Heute gibt es tolle Angebote!", 2),
            new ChainMessage("Treue Kunden sind immer gut!", 2),
            new ChainMessage("Was ein schöner Tag zum Kaufen von gutem Holz!", 3),
            new ChainMessage("Qualitativ ist mein Holz definitiv das beste!", 3)
    ));

    public void openWoodShopByNpc(final Player player) {
        this.smallTalk(this.woodTalks, RoleplayID.SHOP_LUMBERJACK.id(), player, message -> {
            if (message == null) {
                this.openWoodShop(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fDarick §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openWoodShop(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openWoodShop(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Holzfäller Darick", "§7Wähle eines der aufgelisteten Items aus, welches du kaufen möchtest.");
        this.woodShop.forEach(id -> {
            Economy.getShopAPI().getCurrentPrice(id, 1, price -> {
                if (price < 0.05) {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§a§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §fKein Verkauf", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                } else {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§a§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §r§f$" + Economy.getAPI().getMoneyFormat().format(this.getSellPrice(price)), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                }
            });
        });
        form.build().send(player);
    }

    private final List<int[]> miningShop = new ArrayList<>(Arrays.asList(
            new int[]{15, 0}, new int[]{14, 0}, new int[]{264, 0},
            new int[]{351, 4}, new int[]{331, 0}, new int[]{263, 0}, new int[]{388, 0}
    ));

    private final List<ChainMessage> miningTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hallo, §a%p§7! Schau, wie viel tolle Erze ich anbiete. So viele hat niemand!", 5),
            new ChainMessage("Für gute Qualität muss man auch zahlen!", 3),
            new ChainMessage("Rohstoffe wie diese braucht man immer, stimmts?", 3),
            new ChainMessage("Ich habe so viel in meinem Sortiment wie noch nie!", 3),
            new ChainMessage("Eine true Kundschaft zahlt sich aus!", 3),
            new ChainMessage("Hast du gekauft, so hast du gewonnen; verkaufe, und du wirst verlieren.", 4),
            new ChainMessage("Schau dich ruhig um, ich habe Zeit!", 3)
    ));

    public void openMiningShopByNpc(final Player player) {
        this.smallTalk(this.miningTalks, RoleplayID.SHOP_MINER.id(), player, message -> {
            if (message == null) {
                this.openMiningShop(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fPatrick §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openMiningShop(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openMiningShop(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Minenarbeiter Patrick", "§7Wähle eines der aufgelisteten Items aus, welches du kaufen möchtest.");
        this.miningShop.forEach(id -> {
            Economy.getShopAPI().getCurrentPrice(id, 1, price -> {
                if (price < 0.05) {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§b§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §fKein Verkauf", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                } else {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§b§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §r§f$" + Economy.getAPI().getMoneyFormat().format(this.getSellPrice(price)), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                }
            });
        });
        form.build().send(player);
    }

    private final List<int[]> exploringShop = new ArrayList<>(Arrays.asList(
            new int[]{38, 0}, new int[]{37, 0}, new int[]{38, 3}, new int[]{175, 0}, new int[]{-216, 0},
            new int[]{38, 1}, new int[]{38, 2}, new int[]{38, 4}, new int[]{38, 5}, new int[]{38, 6}, new int[]{38, 7},
            new int[]{38, 8}, new int[]{38, 9}, new int[]{38, 10}, new int[]{32, 0}, new int[]{111, 0}, new int[]{-163, 0}
    ));

    private final List<ChainMessage> exploringTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Schön dich zu sehen, §a%p§7! Ich erkunde die Welt und verkaufe gefundene Pflanzen!", 5),
            new ChainMessage("Natur ist mein Spezialgebiet. Ich liebe die Natur.", 3),
            new ChainMessage("Ich pflücke nur ausgewählte Blumen.", 3),
            new ChainMessage("Bei mir gibt es immer tolle Angebote!", 3),
            new ChainMessage("Ich erweitere meine Angebote sehr oft!", 3),
            new ChainMessage("Lass dich nicht von mir stören! Schau dich ruhig um.", 3),
            new ChainMessage("Schau dich ruhig um, ich habe Zeit!", 3)
    ));

    public void openExploringShopByNpc(final Player player) {
        this.smallTalk(this.exploringTalks, RoleplayID.SHOP_EXPLORER.id(), player, message -> {
            if (message == null) {
                this.openExploringShop(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fMaya §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openExploringShop(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openExploringShop(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Reisende Maya", "§7Wähle eines der aufgelisteten Items aus, welches du kaufen möchtest.");
        this.exploringShop.forEach(id -> {
            Economy.getShopAPI().getCurrentPrice(id, 1, price -> {
                if (price < 0.05) {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§e§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §fKein Verkauf", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                } else {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§e§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §r§f$" + Economy.getAPI().getMoneyFormat().format(this.getSellPrice(price)), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                }
            });
        });
        form.build().send(player);
    }

    private final List<int[]> netherShop = new ArrayList<>(Arrays.asList(
            new int[]{-225, 0}, new int[]{-226, 0}, new int[]{214, 0}, new int[]{-227, 0}, new int[]{-232, 0},
            new int[]{-233, 0}, new int[]{-228, 0}, new int[]{-229, 0}, new int[]{-230, 0}, new int[]{-234, 0},
            new int[]{-236, 0}, new int[]{88, 0}, new int[]{-273, 0}, new int[]{-289, 0}, new int[]{372, 0}
    ));

    private final List<ChainMessage> netherTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Oh... Hey, §a%p§7! Ich bin immer in ganz anderen Welten unterwegs! Schau dich gerne mal um.", 5),
            new ChainMessage("Meine Waren sind nicht von Lava beschädigt!", 3),
            new ChainMessage("Ich spiele sehr oft mit dem Feuer!", 3),
            new ChainMessage("Nicht so schüchtern, kauf ruhig.", 3),
            new ChainMessage("Ich weiß wie die Hölle aussieht...", 3),
            new ChainMessage("Kaufe was du möchtest, du bist frei!", 3),
            new ChainMessage("Rot ist meine absolute Lieblingsfarbe!", 3)
    ));

    public void openNetherShopByNpc(final Player player) {
        this.smallTalk(this.netherTalks, RoleplayID.SHOP_NETHER.id(), player, message -> {
            if (message == null) {
                this.openNetherShop(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fLilly §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openNetherShop(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openNetherShop(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Netherexpertin Lilly", "§7Wähle eines der aufgelisteten Items aus, welches du kaufen möchtest.");
        this.netherShop.forEach(id -> {
            Economy.getShopAPI().getCurrentPrice(id, 1, price -> {
                if (price < 0.05) {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§4§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §fKein Verkauf", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                } else {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§4§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §r§f$" + Economy.getAPI().getMoneyFormat().format(this.getSellPrice(price)), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                }
            });
        });
        form.build().send(player);
        // 5Hf8qgiyxHtejGqzBoG85fi8KRhrY5eBGjRXAqhg
    }

    private final List<int[]> mobdropShop = new ArrayList<>(Arrays.asList(
            new int[]{289, 0}, new int[]{367, 0}, new int[]{287, 0}, new int[]{399, 0}, new int[]{368, 0},
            new int[]{341, 0}, new int[]{376, 0}, new int[]{378, 0}, new int[]{369, 0}, new int[]{288, 0},
            new int[]{334, 0}
    ));

    private final List<ChainMessage> mobdropTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hi §a%p§7! Komm her und schau dich um! Ich habe alles, was du dir von Monstern vorstellen kannst.", 5),
            new ChainMessage("Das Jagen ist kein einfacher Job!", 3),
            new ChainMessage("Schau dir meine neuen Angebote an!", 3),
            new ChainMessage("Letztens wurde ich von einer Kuh überrannt!", 3),
            new ChainMessage("Wenn etwas fehlt, sag mir bescheid!", 3),
            new ChainMessage("Geringe Preise für tolle Qualität!", 3),
            new ChainMessage("Man sagt, ich besäße besondere Kräfte...", 3)
    ));

    public void openMobdropShopByNpc(final Player player) {
        this.smallTalk(this.mobdropTalks, RoleplayID.SHOP_MONSTERUNTER.id(), player, message -> {
            if (message == null) {
                this.openMobdropShop(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fAmanda §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openMobdropShop(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openMobdropShop(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Monsterjägerin Amanda", "§7Wähle eines der aufgelisteten Items aus, welches du kaufen möchtest.");
        this.mobdropShop.forEach(id -> {
            Economy.getShopAPI().getCurrentPrice(id, 1, price -> {
                if (price < 0.05) {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§5§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §fKein Verkauf", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                } else {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§5§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §r§f$" + Economy.getAPI().getMoneyFormat().format(this.getSellPrice(price)), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                }
            });
        });
        form.build().send(player);
    }

    private final List<int[]> farmerShop = new ArrayList<>(Arrays.asList(
            new int[]{ItemID.BEETROOT, 0}, new int[]{ItemID.CARROT, 0}, new int[]{ItemID.WHEAT, 0}, new int[]{ItemID.POTATO, 0}, new int[]{ItemID.APPLE, 0}
    ));

    private final List<ChainMessage> farmerTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hallo §a%p§7! Bei mir gibt es das beste Obst und Gemüse aus der ganzen Stadt!", 5),
            new ChainMessage("Ich besitze die meisten Felder!", 3),
            new ChainMessage("Ich dünge nur mit natürlichen Mitteln!", 3),
            new ChainMessage("Bäuerin zu sein ist ein echter Knochenjob.", 3),
            new ChainMessage("Wenn etwas fehlt, sag mir bescheid!", 3),
            new ChainMessage("Geringe Preise für tolle Qualität!", 3),
            new ChainMessage("Ich ernte nur die besten Früchte!", 3)
    ));

    public void openFarmerShopByNpc(final Player player) {
        this.smallTalk(this.farmerTalks, RoleplayID.FARMER.id(), player, message -> {
            if (message == null) {
                this.openFarmerShop(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fSofi §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openFarmerShop(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openFarmerShop(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Bäuerin Sofi", "§7Wähle eines der aufgelisteten Items aus, welches du kaufen möchtest.");
        this.farmerShop.forEach(id -> {
            Economy.getShopAPI().getCurrentPrice(id, 1, price -> {
                if (price < 0.05) {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§3§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §fKein Verkauf", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                } else {
                    form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§3§l1x   §r§a+ §r§f$" + Economy.getAPI().getMoneyFormat().format(Math.max(price, 0.10)) + " §8| §c- §r§f$" + Economy.getAPI().getMoneyFormat().format(this.getSellPrice(price)), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                        this.openItemShop(e, id, price);
                    });
                }
            });
        });
        form.build().send(player);
    }

    public double getSellPrice(final double d) {
        return .23 * d;
    }

    private final List<ChainMessage> blacksmithTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hallo §a%p§7! Ich bin Ben, der Schmied! Schau dir doch mal mein Angebot an, wenn du möchtest.", 5),
            new ChainMessage("Meine Angebote sind der Hammer, oder?", 3),
            new ChainMessage("Falls du Fragen zu Verzauberungen hast, frag!", 3),
            new ChainMessage("Ich garantiere, dass jede Verzauberung funktioniert!", 3),
            new ChainMessage("Mein Stand ist vom Amt geprüft, da kannst du dir sicher sein!", 4),
            new ChainMessage("Dieser Job ist ein echter Knochenjob!", 3),
            new ChainMessage("Kauf ruhig, ich habe Zeit!", 2)
    ));

    public void openBlacksmithShopByNpc(final Player player) {
        this.smallTalk(this.blacksmithTalks, RoleplayID.SHOP_BLACKSMITH.id(), player, message -> {
            if (message == null) {
                this.openBlacksmithShop(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fBen §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openBlacksmithShop(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    private final List<double[]> blacksmithEnchantments = new ArrayList<>(Arrays.asList(
            new double[]{128, 1, 499.95}, new double[]{129, 1, 199.95}, new double[]{130, 1, 199.95}, new double[]{131, 1, 699.95}, new double[]{132, 1, 1399.95},
            new double[]{133, 1, 99.95}, new double[]{134, 1, 199.95}, new double[]{135, 1, 399.95}, new double[]{136, 1, 99.95}, new double[]{137, 1, 999.95}
    ));

    public void openBlacksmithShop(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Schmied Ben", "§8» §fBen §8| §7Aktuell biete ich nur Reparaturen an. Aber bald auch spezielle Verzauberungen, die es nur bei mir gibt!");
        /*form.addButton(new ElementButton("§7» §fBen's spezielle\n§fVerzauberungen", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/enchanted_book.png")), e -> {
            final SimpleForm.Builder enchantmentForm = new SimpleForm.Builder("§7» §8Meine Verzauberungen", "§7Wähle eines der aufgelisteten Verzauberungen aus, um fortzufahren.");
            this.blacksmithEnchantments.forEach(c -> {
                final EnchantmentID enchantmentID = this.serverCore.getCustomEnchantment().enchantmentId.get((int) c[0]);
                enchantmentForm.addButton(new ElementButton("§8" + enchantmentID.enchantment() + "\n§9Level: §f" + (int) c[1] + " §8| §f$" + c[2]), k -> {

                    final Enchantment enchantment = Enchantment.getEnchantment((int) c[0]);
                    if (enchantment.getMaxLevel() == 1) {
                        final ModalForm buyModal = new ModalForm.Builder("§7» §8Kaufbestätigung", "Möchtest du das Item in deiner Hand wirklich mit §9" + enchantmentID.enchantment() + "§f (§9Level " + (int) c[1] + "§f) verzaubern?" +
                                "\n\nDie Kosten dafür betragen §a$" + c[2] + "§f.", "§8» §aKaufen", "§8» §cAbbrechen")
                                .onYes(g -> {
                                    final Item item = player.getInventory().getItemInHand();
                                    if (this.checkCanEnchant(item, enchantment)) {
                                        Economy.getAPI().getMoney(player, money -> {
                                            if (money >= c[2]) {
                                                Economy.getAPI().reduceMoney(player, c[2]);
                                                this.serverCore.getCustomEnchantment().enchantItem(player, enchantmentID, 1);
                                                player.sendMessage(Language.get("roleplay.blacksmith.enchantment.bought", enchantmentID.enchantment(), (int) c[1], c[2]));
                                                this.playSound(player, Sound.RANDOM_ANVIL_USE);
                                            } else {
                                                player.sendMessage(Language.get("roleplay.blacksmith.enchantment.not.enough.money"));
                                                this.playSound(player, Sound.NOTE_BASS);
                                            }
                                        });
                                    } else {
                                        player.sendMessage(Language.get("roleplay.blacksmith.enchantment.invalid.tool"));
                                        this.playSound(player, Sound.NOTE_BASS);
                                    }
                                })
                                .onNo(g -> {
                                    this.openBlacksmithShop(player);
                                })
                                .build();
                        buyModal.send(player);
                    } else {
                        final CustomForm selectForm = new CustomForm.Builder("§7» §8Verzauberungslevel wählen")
                                .addElement(new ElementLabel("§7Bei dieser Verzauberung kannst du ein Level wählen. Die Kosten für das erste Level betragen §a$" + c[2] + "§7.\n" +
                                        "Die Level werden mit dem genannten Preis multipliziert."))
                                .addElement(new ElementSlider("Level", 1, enchantment.getMaxLevel(), 1, 1))
                                .onSubmit((g, h) -> {
                                    final int level = (int) h.getSliderResponse(1);
                                    final double needed = c[2] * level;
                                    final ModalForm buyModal = new ModalForm.Builder("§7» §8Kaufbestätigung", "Möchtest du das Item in deiner Hand wirklich mit §9" + enchantmentID.enchantment() + "§f (§9Level " + level + "§f) verzaubern?" +
                                            "\n\nDie Kosten dafür betragen §a$" + Economy.getAPI().getMoneyFormat().format(needed) + "§f.", "§8» §aKaufen", "§8» §cAbbrechen")
                                            .onYes(z -> {
                                                final Item item = player.getInventory().getItemInHand();
                                                if (this.checkCanEnchant(item, enchantment)) {
                                                    Economy.getAPI().getMoney(player, money -> {
                                                        if (money >= needed) {
                                                            Economy.getAPI().reduceMoney(player, needed);
                                                            this.serverCore.getCustomEnchantment().enchantItem(player, enchantmentID, level);
                                                            player.sendMessage(Language.get("roleplay.blacksmith.enchantment.bought", enchantmentID.enchantment(), level, Economy.getAPI().getMoneyFormat().format(needed)));
                                                            this.playSound(player, Sound.RANDOM_ANVIL_USE);
                                                        } else {
                                                            player.sendMessage(Language.get("roleplay.blacksmith.enchantment.not.enough.money"));
                                                            this.playSound(player, Sound.NOTE_BASS);
                                                        }
                                                    });
                                                } else {
                                                    player.sendMessage(Language.get("roleplay.blacksmith.enchantment.invalid.tool"));
                                                    this.playSound(player, Sound.NOTE_BASS);
                                                }
                                            })
                                            .onNo(z -> {
                                                this.openBlacksmithShop(player);
                                            })
                                            .build();
                                    buyModal.send(player);
                                })
                                .build();
                        selectForm.send(player);
                    }
                });
            });
            enchantmentForm.build().send(player);
        });
        form.addButton(new ElementButton("§7» §fMein Angebot", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/paper.png")), e -> {

        });*/
        form.addButton(new ElementButton("§7» §fReparatur Service", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/anvil.png")), e -> {
            final Item item = e.getInventory().getItemInHand();
            if (item.isArmor() || item.isTool() || item.isShears()) {
                if (item.getDamage() != 0) {
                    final double costs = (item.getDamage() * 1.30) + 60;
                    final ModalForm modalForm = new ModalForm.Builder("§7» §8Item reparieren", "§fLasse das Item in deiner Hand hier reparieren." +
                            "\n\n§fGrundgebühr: §a$60\n§fSchadensbehebung: §a$" + Economy.getAPI().getMoneyFormat().format(item.getDamage() * 1.30) + "\n§fBenötigte XP-Level: §a5" +
                            "\n\n§f§lZu zahlen: §r§a$" + Economy.getAPI().getMoneyFormat().format(costs) + " §fund §a5 XP-Level",
                            "§7» §aJetzt reparieren", "§7» §cAbbrechen")
                            .onYes(h -> {
                                if (h.getExperienceLevel() >= 5) {
                                    Economy.getAPI().getMoney(h, money -> {
                                        if (money >= costs) {
                                            h.setExperience(h.getExperience(), h.getExperienceLevel() - 5);
                                            Economy.getAPI().reduceMoney(h, costs);
                                            item.setDamage(0);
                                            h.getInventory().setItemInHand(item);
                                            h.sendMessage(Language.get("roleplay.blacksmith.repair.repaired", Economy.getAPI().getMoneyFormat().format(costs)));
                                            this.playSound(h, Sound.RANDOM_ANVIL_USE);
                                        } else {
                                            h.sendMessage(Language.get("roleplay.blacksmith.repair.not.enough.money"));
                                            this.playSound(h, Sound.NOTE_BASS);
                                        }
                                    });
                                } else {
                                    h.sendMessage(Language.get("roleplay.blacksmith.repair.not.enough.xp"));
                                    this.playSound(h, Sound.NOTE_BASS);
                                }
                            })
                            .onNo(this::openBlacksmithShop)
                            .build();
                    modalForm.send(e);
                } else {
                    e.sendMessage(Language.get("roleplay.blacksmith.repair.invalid.damage"));
                    this.playSound(e, Sound.NOTE_BASS);
                }
            } else {
                e.sendMessage(Language.get("roleplay.blacksmith.repair.invalid.item"));
                this.playSound(e, Sound.NOTE_BASS);
            }
        });
        form.build().send(player);
    }

    private boolean checkCanEnchant(final Item item, final Enchantment enchantment) {
        switch (enchantment.type) {
            case ARMOR:
                if (item.isArmor()) return true;
                break;
            case DIGGER:
                if (item.isTool()) return true;
                break;
            case SWORD:
                if (item.isSword()) return true;
                break;
            case ARMOR_HEAD:
                if (item.isHelmet()) return true;
                break;
            case ARMOR_TORSO:
                if (item.isChestplate()) return true;
                break;
            case ARMOR_LEGS:
                if (item.isLeggings()) return true;
                break;
            case ARMOR_FEET:
                if (item.isBoots()) return true;
                break;
            default:
                return false;
        }
        return false;
    }

    final Cooldown playerTalks = new Cooldown(TimeUnit.MINUTES.toMillis(15));
    final Cooldown talkCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(20));

    private void smallTalk(final List<ChainMessage> messages, final String npc, final Player player, final Consumer<ChainMessage> message) {
        if (this.talkCooldown.hasCooldown(npc + "//" + player.getName())) {
            message.accept(null);
            return;
        }
        if (!this.playerTalks.hasCooldown(npc + "//" + player.getName())) {
            message.accept(messages.get(0));
        } else {
            int index = ThreadLocalRandom.current().nextInt(1, messages.size());
            message.accept(messages.get(index));
        }
        this.openQueue.add(player.getName());
    }

    private void playSound(final Player player, final Sound sound) {
        final PlaySoundPacket packet = new PlaySoundPacket();
        packet.name = sound.getSound();
        packet.x = new Double(player.getLocation().getX()).intValue();
        packet.y = (new Double(player.getLocation().getY())).intValue();
        packet.z = (new Double(player.getLocation().getZ())).intValue();
        packet.volume = 1.0F;
        packet.pitch = 1.0F;
        player.dataPacket(packet);
    }


    @RequiredArgsConstructor
    public static class ShopListener implements Listener {

        private final ShopRoleplay shopRoleplay;

        @EventHandler
        public void on(final PlayerInteractEntityEvent event) {
            final Player player = event.getPlayer();
            if (event.getEntity().namedTag.exist("npc_id")) {
                final String npcId = event.getEntity().namedTag.getString("npc_id");
                if (!EventListener.inIntroduction.contains(player.getName())) {
                    if (!this.shopRoleplay.openQueue.contains(player.getName())) {
                        if (npcId.equals(RoleplayID.SHOP_MINER.id())) this.shopRoleplay.openMiningShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_NETHER.id())) this.shopRoleplay.openNetherShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_EXPLORER.id())) this.shopRoleplay.openExploringShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_LUMBERJACK.id())) this.shopRoleplay.openWoodShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_MONSTERUNTER.id())) this.shopRoleplay.openMobdropShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_BLACKSMITH.id())) this.shopRoleplay.openBlacksmithShopByNpc(player);
                        else if (npcId.equals(RoleplayID.FARMER.id())) this.shopRoleplay.openFarmerShopByNpc(player);
                    }
                }
            }
        }

        @EventHandler
        public void on(final EntityDamageByEntityEvent event) {
            final Entity entity = event.getEntity();
            if (event.getDamager() instanceof Player) {
                final Player player = (Player) event.getDamager();
                if (entity.namedTag.exist("npc_id")) {
                    final String npcId = entity.namedTag.getString("npc_id");
                    if (!EventListener.inIntroduction.contains(player.getName())) {
                        if (!this.shopRoleplay.openQueue.contains(player.getName())) {
                            if (npcId.equals(RoleplayID.SHOP_MINER.id())) this.shopRoleplay.openMiningShopByNpc(player);
                            else if (npcId.equals(RoleplayID.SHOP_NETHER.id())) this.shopRoleplay.openNetherShopByNpc(player);
                            else if (npcId.equals(RoleplayID.SHOP_EXPLORER.id())) this.shopRoleplay.openExploringShopByNpc(player);
                            else if (npcId.equals(RoleplayID.SHOP_LUMBERJACK.id())) this.shopRoleplay.openWoodShopByNpc(player);
                            else if (npcId.equals(RoleplayID.SHOP_MONSTERUNTER.id())) this.shopRoleplay.openMobdropShopByNpc(player);
                            else if (npcId.equals(RoleplayID.SHOP_BLACKSMITH.id())) this.shopRoleplay.openBlacksmithShopByNpc(player);
                            else if (npcId.equals(RoleplayID.FARMER.id())) this.shopRoleplay.openFarmerShopByNpc(player);
                        }
                    }
                }
            }
        }

    }

}
