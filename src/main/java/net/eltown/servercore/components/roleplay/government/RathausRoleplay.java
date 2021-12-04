package net.eltown.servercore.components.roleplay.government;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWallSign;
import cn.nukkit.blockentity.BlockEntitySign;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.level.Level;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import lombok.*;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.chestshop.ShopLicense;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.listeners.EventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class RathausRoleplay {

    private final ServerCore serverCore;

    public RathausRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        final Level level = this.serverCore.getServer().getLevelByName("plots");
        serverCore.getServer().getPluginManager().registerEvents(new RathausRoleplay.RathausListener(this), serverCore);

        this.cachedAgencies.put(-1, new Agency(-1, "Archiv", "Frau Heegel", new Location(79, 68, 72, level), new Location(79, 68, 75, level), new Location(80, 69, 74, level), "null"));
        this.cachedAgencies.put(1, new Agency(1, "Amtsgericht", "Herr Meier", new Location(66, 72, 86, level), new Location(67, 72, 81, level), new Location(66, 73, 82, level), "null"));
        this.cachedAgencies.put(5, new Agency(5, "Steuern", "Frau Bärwald", new Location(69, 79, 86, level), new Location(67, 79, 81, level), new Location(66, 80, 82, level), "null"));
        this.cachedAgencies.put(7, new Agency(7, "Bauamt", "Herr Keppel", new Location(79, 79, 87, level), new Location(79, 79, 81, level), new Location(78, 80, 82, level), "null"));
    }

    public final HashMap<Integer, Agency> cachedAgencies = new HashMap<>();
    public final HashMap<String, Agency> cachedAppointments = new HashMap<>();

    private final List<ChainMessage> receptionTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Herzlich Willkommen im Rathaus, §a%p§7! Wie kann ich Ihnen weiterhelfen?", 4),
            new ChainMessage("Oh, wie kann ich Ihnen weiterhelfen?", 2),
            new ChainMessage("Was kann ich Ihnen Gutes tun?", 2)
    ));

    public void openReceptionByNpc(final Player player) {
        this.smallTalk(this.receptionTalks, RoleplayID.TOWNHALL_RECEPTION.id(), player, message -> {
            if (message == null) {
                this.openReception(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fHerr Kaufmann §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openReception(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    final Cooldown dateExpire = new Cooldown(TimeUnit.MINUTES.toMillis(5));

    public void openReception(final Player player) {
        final SimpleForm form = new SimpleForm.Builder("§7» §8Rezeptionist", "§8» §fHerr Kaufmann §8| §7Mit welcher Verwaltungskraft möchten Sie sprechen?")
                /*.addButton(new ElementButton("§8» §eArchiv\n§fFrau Heegel §8| §9Zimmer U1, UG", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/receptionist_archive.png")), e -> {

                })*/
                /*.addButton(new ElementButton("§8» §eAmtsgericht\n§fHerr Meier §8| §9Zimmer 1, EG", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/receptionist_court.png")), e -> {

                })*/
                .addButton(new ElementButton("§8» §eSteuern\n§fFrau Bärwald §8| §9Zimmer 5, 1. OG", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/receptionist_taxes.png")), e -> {
                    final ModalForm modalForm = new ModalForm.Builder("§7» §8Rezeptionist", "§8» §fHerr Kaufmann §8| §7Soll ich für Sie einen Termin bei §eFrau Bärwald §7buchen? " +
                            "", "§8» §aTermin buchen", "§8» §cAbbrechen")
                            .onYes(g -> {
                                if (!this.dateExpire.hasCooldown(player.getName())) {
                                    this.setAppointment(player.getName(), this.cachedAgencies.get(5));
                                    player.sendMessage("§8» §fHerr Kaufmann §8| §7Der Termin wurde gebucht. Dieser ist nun §e5 Minuten §7lang gültig. Bitte begeben Sie sich zu §eFrau Bärwald §7ins §e1. OG§7.");
                                } else player.sendMessage("§8» §fHerr Kaufmann §8| §7Sie haben Bereits einen Termin bei §eFrau Bärwald §7gebucht.");
                            })
                            .onNo(this::openReception)
                            .build();
                    modalForm.send(e);
                })
                .addButton(new ElementButton("§8» §eBauamt\n§fHerr Keppel §8| §9Zimmer 7, 1. OG", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/receptionist_building.png")), e -> {
                    final ModalForm modalForm = new ModalForm.Builder("§7» §8Rezeptionist", "§8» §fHerr Kaufmann §8| §7Soll ich für Sie einen Termin bei §eHerrn Keppel §7buchen? " +
                            "", "§8» §aTermin buchen", "§8» §cAbbrechen")
                            .onYes(g -> {
                                if (!this.dateExpire.hasCooldown(player.getName())) {
                                    this.setAppointment(player.getName(), this.cachedAgencies.get(7));
                                    player.sendMessage("§8» §fHerr Kaufmann §8| §7Der Termin wurde gebucht. Dieser ist nun §e5 Minuten §7lang gültig. Bitte begeben Sie sich zu §eHerrn Keppel §7ins §e1. OG§7.");
                                } else player.sendMessage("§8» §fHerr Kaufmann §8| §7Sie haben Bereits einen Termin bei §eHerrn Keppel §7gebucht.");
                            })
                            .onNo(this::openReception)
                            .build();
                    modalForm.send(e);
                })
                .build();
        form.send(player);
    }

    private void openCassandra(final Player player, final Agency agency) {
        final SimpleForm form = new SimpleForm.Builder(agency.getName(), "§8» §f" + player.getName() + " §8| §7Hallo, ich habe einen Termin bei Ihnen!\n\n" +
                "§8» §f" + agency.getName() + " §8| §7Guten Tag, §a" + player.getName() + "§7! Was kann ich für Sie tun?")
                .addButton(new ElementButton("§8» §eChestShop\n§8- §fLizenzen §8-", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/taxes_chestshop_license.png")), e -> {
                    final ShopLicense shopLicense = this.serverCore.getChestShopAPI().getPlayerLicense(e.getName());
                    final ShopLicense nextShopLicense = this.getNextLicense(e.getName());

                    final String nextLevel = nextShopLicense != null ? "Ich kann Ihnen eine bessere Lizenz anbieten, sofern Sie möchten." : "Da Sie bereits die beste Lizenz besitzen, " +
                            "kann ich Ihnen keine bessere anbieten.";
                    final SimpleForm.Builder licenseMain = new SimpleForm.Builder(agency.getName(), "§8» §f" + e.getName() + " §8| §7Ich habe ein Anliegen bezüglich der ChestShop-Lizenzen.\n\n" +
                            "§8» §f" + agency.getName() + " §8| §7Alles klar. Sie besitzen aktuell die §e" + shopLicense.getLicense().displayName() + "§7-Lizenz mit §e" + shopLicense.getLicense().maxPossibleShops() + "§7 " +
                            "erstellbaren Shops.\n§7" + nextLevel);
                    if (nextShopLicense != null) {
                        licenseMain.addButton(new ElementButton("§8» §9" + nextShopLicense.getLicense().displayName() + "-Lizenz\n§a$" +
                                Economy.getAPI().getMoneyFormat().format(nextShopLicense.getLicense().money()), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/taxes_chestshop_license_upgrade.png")), l -> {
                            final ModalForm buyForm = new ModalForm.Builder(agency.getName(), "§8» §f" + player.getName() + " §8| §7Das klingt interessant...\n\n" +
                                    "§8» §f" + agency.getName() + " §8| §7Möchten Sie diese Lizenz für eine einmalige Zahlung von §a$" + Economy.getAPI().getMoneyFormat().format(nextShopLicense.getLicense().money()) + "§7 kaufen?"
                                    , "§8» §aLizenz kaufen", "§8» §cAbbrechen")
                                    .onYes(k -> {
                                        Economy.getAPI().getMoney(player, money -> {
                                            if (money >= nextShopLicense.getLicense().money()) {
                                                Economy.getAPI().reduceMoney(player, nextShopLicense.getLicense().money());
                                                this.serverCore.getChestShopAPI().setLicense(player.getName(), nextShopLicense.getLicense());
                                                this.serverCore.playSound(player, Sound.RANDOM_LEVELUP, 1, 3);
                                                player.sendMessage("§8» §f" + agency.getName() + " §8| §7Sehr gut! Sie haben die Lizenz nun gekauft. Sie können direkt die Vorteile der Lizenz nutzen.");
                                            } else {
                                                this.serverCore.playSound(player, Sound.NOTE_BASS);
                                                player.sendMessage("§8» §f" + player.getName() + " §8| §7Ich habe leider nicht genug Geld dabei...");
                                            }
                                        });
                                    })
                                    .onNo(k -> this.openCassandra(player, agency))
                                    .build();
                            buyForm.send(player);
                        });
                    }
                    licenseMain.build().send(e);
                })
                .addButton(new ElementButton("§8» §cTermin beenden", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/cancel.png")), e -> {
                    if (this.cachedAgencies.get(5).getCurrentPlayer().equals(player.getName())) {
                        this.cachedAgencies.get(5).setCurrentPlayer("null:0");
                        e.teleport(agency.getOut());
                        this.serverCore.playSound(e, Sound.RANDOM_DOOR_CLOSE);
                        e.sendMessage("§8» §f" + agency.getName() + " §8| §7Auf Wiedersehen!");
                    }
                })
                .build();
        form.send(player);
    }

    public void openJack(final Player player, final Agency agency) {
        final SimpleForm form = new SimpleForm.Builder(agency.getName(), "§8» §f" + player.getName() + " §8| §7Hallo, ich habe einen Termin bei Ihnen!\n\n" +
                "§8» §f" + agency.getName() + " §8| §7Hallo, §a" + player.getName() + "§7! Was kann ich für Sie tun?")
                .addButton(new ElementButton("§8» §eGrundstücke\n§8- §fBaurechte §8-", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/building_build_permissions.png")), e -> {
                    final int plot = this.getNextPlot(player);

                    final String nextPlot = plot != 0 ? "§8» §f" + agency.getName() + " §8| §7Das klingt gut! Ich könnte Ihnen weitere Grundstücksrechte anbieten." : "§8» §f" + agency.getName() + " §8| §7Aktuell kann ich Ihnen leider kein weiteres Grundstück anbieten. Vielleicht stehen bald wieder welche zur Verfügung.";
                    final SimpleForm.Builder selectForm = new SimpleForm.Builder(agency.getName(), "§8» §f" + player.getName() + " §8| §7Ich denke aktuell über weitere Grundstücke nach...\n\n" + nextPlot + "\n\n");

                    if (plot != 0) {
                        selectForm.addButton(new ElementButton("§8» §9Weiteres Grundstück erwerben", new ElementButtonImageData("url", "")), h -> {
                            final ModalForm modalForm = new ModalForm.Builder(agency.getName(), "§8» §f" + agency.getName() + " §8| §7Möchten Sie ein weiteres Grundstück für §a$5.000 §7kaufen?\n\n§cDiese Aktion kann unter keinen Umständen rückgängig gemacht werden!",
                                    "§8» §aGrundstück kaufen", "§8» §cAbbrechen")
                                    .onYes(g -> {
                                        Economy.getAPI().getMoney(player, money -> {
                                            if (money >= 5000) {
                                                Economy.getAPI().reduceMoney(player, 5000);
                                                if (plot != 3) this.serverCore.getGroupAPI().removePlayerPermission(player.getName(), "plots.claim." + (plot - 1));
                                                this.serverCore.getGroupAPI().addPlayerPermission(player.getName(), "plots.claim." + plot);
                                                this.serverCore.playSound(player, Sound.RANDOM_LEVELUP, 1, 3);
                                                player.sendMessage("§8» §f" + agency.getName() + " §8| §7Sehr gut! Sie haben ein weiteres Grundstück gekauft.");
                                            } else {
                                                this.serverCore.playSound(player, Sound.NOTE_BASS);
                                                player.sendMessage("§8» §f" + player.getName() + " §8| §7Ich habe leider zu wenig Geld dabei...");
                                            }
                                        });
                                    })
                                    .onNo(g -> this.openJack(player, agency))
                                    .build();
                            modalForm.send(player);
                        });
                    }
                    selectForm.build().send(player);
                })
                .addButton(new ElementButton("§8» §cTermin beenden", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/cancel.png")), e -> {
                    if (this.cachedAgencies.get(7).getCurrentPlayer().equals(player.getName())) {
                        this.cachedAgencies.get(7).setCurrentPlayer("null:0");
                        e.teleport(agency.getOut());
                        this.serverCore.playSound(e, Sound.RANDOM_DOOR_CLOSE);
                        e.sendMessage("§8» §f" + agency.getName() + " §8| §7Tschüss! Bis bald.");
                    }
                })
                .build();
        form.send(player);
    }

    private int getNextPlot(final Player player) {
        if (player.hasPermission("plots.claim.3")) return 4;
        else if (player.hasPermission("plots.claim.4")) return 5;
        else if (player.hasPermission("plots.claim.5")) return 6;
        else if (player.hasPermission("plots.claim.6")) return 0;
        else return 3;
    }

    private ShopLicense getNextLicense(final String player) {
        final ShopLicense shopLicense = this.serverCore.getChestShopAPI().getPlayerLicense(player);
        switch (shopLicense.getLicense()) {
            case STANDARD:
                return new ShopLicense(player, ShopLicense.ShopLicenseType.SMALL_BUSINESS, shopLicense.getAdditionalShops());
            case SMALL_BUSINESS:
                return new ShopLicense(player, ShopLicense.ShopLicenseType.BUSINESS, shopLicense.getAdditionalShops());
            case BUSINESS:
                return new ShopLicense(player, ShopLicense.ShopLicenseType.BIG_BUSINESS, shopLicense.getAdditionalShops());
            case BIG_BUSINESS:
                return new ShopLicense(player, ShopLicense.ShopLicenseType.COMPANY, shopLicense.getAdditionalShops());
            default:
                return null;
        }
    }

    public void setAppointment(final String player, final Agency agency) {
        this.cachedAppointments.put(player, agency);
    }

    public void removeAppointment(final String player, final Agency agency) {
        this.cachedAppointments.remove(player);
    }

    public void takeAppointment(final String player, final Agency agency) {
        this.cachedAgencies.get(agency.getRoom()).setCurrentPlayer(player);
        this.dateExpire.removeCooldown(player);
        this.setDoorSignText(agency.getSign(), "§cBesetzt§r");

        this.serverCore.getServer().getScheduler().scheduleDelayedTask(() -> {
            final Agency a = this.cachedAgencies.get(agency.getRoom());
            if (!a.getCurrentPlayer().equals("null:0")) {
                final Player t = this.serverCore.getServer().getPlayer(player);
                if (t != null) {
                    t.teleport(a.getOut());
                    this.serverCore.playSound(t, Sound.RANDOM_DOOR_CLOSE);
                    t.sendMessage("§8» §f" + agency.getName() + " §8| §7Den Termin müssten wir jetzt beenden, da ich gleich einen anderen habe. Auf Wiedersehen!");
                }
            }

            this.finishAppointment(a);
            this.setDoorSignText(a.getSign(), "§2Frei§r");
        }, 3600, true);
    }

    public void finishAppointment(final Agency agency) {
        this.cachedAgencies.get(agency.getRoom()).setCurrentPlayer("null");
    }

    public void setDoorSignText(final Location sign, final String information) {
        final BlockEntitySign blockEntitySign = (BlockEntitySign) sign.getLevelBlock().getLevel().getBlockEntity(sign);
        blockEntitySign.setText(blockEntitySign.getText()[0], blockEntitySign.getText()[1], information, blockEntitySign.getText()[3]);
        blockEntitySign.scheduleUpdate();
    }

    public boolean isInAppointment(final String player, final int room) {
        return this.cachedAgencies.get(room).getCurrentPlayer().equals(player);
    }

    public HashMap<Location, Agency> getSignLocations() {
        final HashMap<Location, Agency> map = new HashMap<>();
        this.cachedAgencies.values().forEach(e -> {
            map.put(e.getSign(), e);
        });
        return map;
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

    public final List<String> openQueue = new ArrayList<>();

    @RequiredArgsConstructor
    public static class RathausListener implements Listener {

        private final RathausRoleplay rathausRoleplay;

        @EventHandler
        public void on(final PlayerInteractEntityEvent event) {
            final Player player = event.getPlayer();
            if (event.getEntity().namedTag.exist("npc_id")) {
                final String npcId = event.getEntity().namedTag.getString("npc_id");
                if (!EventListener.inIntroduction.contains(player.getName())) {
                    if (!this.rathausRoleplay.openQueue.contains(player.getName())) {
                        if (npcId.equals(RoleplayID.TOWNHALL_RECEPTION.id())) this.rathausRoleplay.openReceptionByNpc(player);
                        else if (npcId.equals(RoleplayID.TOWNHALL_TAXES.id())) {
                            if (this.rathausRoleplay.isInAppointment(player.getName(), 5)) {
                                this.rathausRoleplay.openCassandra(player, this.rathausRoleplay.cachedAgencies.get(5));
                            }
                        } else if (npcId.equals(RoleplayID.TOWNHALL_BUILDING.id())) {
                            if (this.rathausRoleplay.isInAppointment(player.getName(), 7)) {
                                this.rathausRoleplay.openJack(player, this.rathausRoleplay.cachedAgencies.get(7));
                            }
                        }
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
                        if (!this.rathausRoleplay.openQueue.contains(player.getName())) {
                            if (npcId.equals(RoleplayID.TOWNHALL_RECEPTION.id())) this.rathausRoleplay.openReceptionByNpc(player);
                            else if (npcId.equals(RoleplayID.TOWNHALL_TAXES.id())) {
                                if (this.rathausRoleplay.isInAppointment(player.getName(), 5)) {
                                    this.rathausRoleplay.openCassandra(player, this.rathausRoleplay.cachedAgencies.get(5));
                                }
                            } else if (npcId.equals(RoleplayID.TOWNHALL_BUILDING.id())) {
                                if (this.rathausRoleplay.isInAppointment(player.getName(), 7)) {
                                    this.rathausRoleplay.openJack(player, this.rathausRoleplay.cachedAgencies.get(7));
                                }
                            }
                        }
                    }
                }
            }
        }

        final Cooldown iCooldown = new Cooldown(TimeUnit.SECONDS.toMillis(1));

        @EventHandler
        public void on(final PlayerInteractEvent event) {
            final Player player = event.getPlayer();
            final Block block = event.getBlock();

            if (block == null) return;
            if (block instanceof BlockWallSign) {
                if (!this.iCooldown.hasCooldown(player.getName())) {
                    if (this.rathausRoleplay.dateExpire.containsCooldown(player.getName())) {
                        final Location location = block.getLocation();
                        this.rathausRoleplay.getSignLocations().forEach((e, g) -> {
                            if (location.equals(e)) {
                                if (g.getJob().equals(this.rathausRoleplay.cachedAppointments.get(player.getName()).getJob())) {
                                    if (g.getCurrentPlayer().equals("null")) {
                                        this.rathausRoleplay.serverCore.playSound(player, Sound.RANDOM_DOOR_OPEN);
                                        player.teleport(g.getIn());
                                        this.rathausRoleplay.removeAppointment(player.getName(), g);
                                        this.rathausRoleplay.takeAppointment(player.getName(), g);
                                        switch (g.getJob()) {
                                            case "Steuern":
                                                this.rathausRoleplay.openCassandra(player, g);
                                                break;
                                            case "Bauamt":
                                                this.rathausRoleplay.openJack(player, g);
                                                break;
                                        }
                                    } else {
                                        this.rathausRoleplay.serverCore.playSound(player, Sound.NOTE_BASS);
                                        player.sendMessage("§8» §f" + g.getName() + " §8| §7Bitte habe einen Moment Geduld. Ich bin gerade in einem Termin.");
                                    }
                                }
                            }
                        });
                    }
                }
            }
        }
    }

    @AllArgsConstructor
    @Data
    public static class Agency {

        private final int room;
        private final String job;
        private final String name;
        private final Location in;
        private final Location out;
        private final Location sign;
        private String currentPlayer;

    }

}
