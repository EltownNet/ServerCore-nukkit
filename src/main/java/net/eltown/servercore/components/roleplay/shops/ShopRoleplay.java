package net.eltown.servercore.components.roleplay.shops;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import cn.nukkit.network.protocol.PlaySoundPacket;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ShopRoleplay {

    private final ServerCore serverCore;

    public ShopRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        serverCore.getServer().getPluginManager().registerEvents(new ShopListener(this), serverCore);
    }

    public final List<String> openQueue = new ArrayList<>();

    private void openItemShop(final Player player, final int[] id) {
        final CustomForm customForm = new CustomForm.Builder("§7» §8" + Item.get(id[0], id[1]).getName())
                .addElement(new ElementInput("Bitte gebe an, wie viel du von diesem Item kaufen möchtest.", "64", "64"))
                .onSubmit((g, h) -> {
                    try {
                        int i = Integer.parseInt(h.getInputResponse(0));
                        if (i <= 0) throw new Exception("Invalid item amount");

                        Economy.getShopAPI().getCurrentPrice(id, i, finalPrice -> {
                            final ModalForm modalForm = new ModalForm.Builder("§7» §8Kaufbestätigung", "Möchtest du §a" + i + "x " + Item.get(id[0], id[1]).getName() + " §ffür"
                                    + " §a$" + Economy.getAPI().getMoneyFormat().format(finalPrice) + " §fkaufen?", "§7» §aKaufen", "§7» §cAbbrechen")
                                    .onYes(l -> {
                                        if (!player.getInventory().canAddItem(Item.get(id[0], id[1], i))) {
                                            player.sendMessage(Language.get("roleplay.shop.item.inventory.full"));
                                            this.playSound(player, Sound.NOTE_BASS);
                                            return;
                                        }

                                        Economy.getAPI().getMoney(player, money -> {
                                            if (money >= finalPrice) {
                                                Economy.getAPI().reduceMoney(player, finalPrice);
                                                Economy.getShopAPI().sendBought(id, i);
                                                player.getInventory().addItem(Item.get(id[0], id[1], i));
                                                player.sendMessage(Language.get("roleplay.shop.item.bought", i, Item.get(id[0], id[1], i).getName(), Economy.getAPI().getMoneyFormat().format(finalPrice)));
                                                this.playSound(player, Sound.UI_STONECUTTER_TAKE_RESULT);
                                            } else {
                                                player.sendMessage(Language.get("roleplay.shop.item.not.enough.money"));
                                                this.playSound(player, Sound.NOTE_BASS);
                                            }
                                        });
                                    })
                                    .onNo(l -> {
                                        this.playSound(player, Sound.NOTE_BASS);
                                    })
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
                form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§2§l1x §r§f$" + Economy.getAPI().getMoneyFormat().format(price), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                    this.openItemShop(e, id);
                });
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
                form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§b§l1x §r§f$" + Economy.getAPI().getMoneyFormat().format(price), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                    this.openItemShop(e, id);
                });
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
                form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§e§l1x §r§f$" + Economy.getAPI().getMoneyFormat().format(price), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                    this.openItemShop(e, id);
                });
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
                form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§4§l1x §r§f$" + Economy.getAPI().getMoneyFormat().format(price), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                    this.openItemShop(e, id);
                });
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
                form.addButton(new ElementButton(Item.get(id[0], id[1]).getName() + "\n§5§l1x §r§f$" + Economy.getAPI().getMoneyFormat().format(price), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/shopitems/" + id[0] + "-" + id[1] + ".png")), e -> {
                    this.openItemShop(e, id);
                });
            });
        });
        form.build().send(player);
    }

    private final List<ChainMessage> blacksmithTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("", 5),
            new ChainMessage("", 3),
            new ChainMessage("", 3),
            new ChainMessage("", 3),
            new ChainMessage("", 3),
            new ChainMessage("", 3),
            new ChainMessage("", 3)
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

    private final List<int[]> blacksmithEnchantments = new ArrayList<>(Arrays.asList(
            new int[]{128, 1, 300}
    ));

    public void openBlacksmithShop(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Schmied Ben", "§7Wähle eine der aufgelisteten Kategorien aus, um fortzufahren.");
        form.addButton(new ElementButton("§7» §fBen's spezielle\n§fVerzauberungen", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/enchanted_book.png")), e -> {

        });
        form.addButton(new ElementButton("§7» §fMein Angebot", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/paper.png")), e -> {

        });
        form.addButton(new ElementButton("§7» §fReparatur Service", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/anvil.png")), e -> {

        });

        form.build().send(player);
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
                if (!this.shopRoleplay.openQueue.contains(player.getName())) {
                    if (npcId.equals(RoleplayID.SHOP_MINER.id())) this.shopRoleplay.openMiningShopByNpc(player);
                    else if (npcId.equals(RoleplayID.SHOP_NETHER.id())) this.shopRoleplay.openNetherShopByNpc(player);
                    else if (npcId.equals(RoleplayID.SHOP_EXPLORER.id())) this.shopRoleplay.openExploringShopByNpc(player);
                    else if (npcId.equals(RoleplayID.SHOP_LUMBERJACK.id())) this.shopRoleplay.openWoodShopByNpc(player);
                    else if (npcId.equals(RoleplayID.SHOP_MONSTERUNTER.id())) this.shopRoleplay.openMobdropShopByNpc(player);
                    else if (npcId.equals(RoleplayID.SHOP_BLACKSMITH.id())) this.shopRoleplay.openBlacksmithShopByNpc(player);
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
                    if (!this.shopRoleplay.openQueue.contains(player.getName())) {
                        if (npcId.equals(RoleplayID.SHOP_MINER.id())) this.shopRoleplay.openMiningShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_NETHER.id())) this.shopRoleplay.openNetherShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_EXPLORER.id())) this.shopRoleplay.openExploringShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_LUMBERJACK.id())) this.shopRoleplay.openWoodShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_MONSTERUNTER.id())) this.shopRoleplay.openMobdropShopByNpc(player);
                        else if (npcId.equals(RoleplayID.SHOP_BLACKSMITH.id())) this.shopRoleplay.openBlacksmithShopByNpc(player);
                    }
                }
            }
        }

    }

}
