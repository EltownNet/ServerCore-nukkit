package net.eltown.servercore.components.roleplay.rathaus;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockWallSign;
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
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
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

        this.cachedAdministrationData.put(5, new AdministrationData(5, "Steuern", "Frau Bärwald", new Location(69, 79, 86, level), new Location(67, 79, 81, level)));
        this.cachedAdministrationSign.put(new Location(66, 80, 82, level), this.cachedAdministrationData.get(5));
        this.runningDatesData.put(this.cachedAdministrationData.get(5), "null");
    }

    private final List<ChainMessage> receptionTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Herzlich Willkommen im Rathaus, §a%p§7! Wie kann ich Ihnen weiterhelfen?", 4),
            new ChainMessage("Oh, wie kann ich Ihnen weiterhelfen?", 2),
            new ChainMessage("Was kann ich Ihnen Gutes tun?", 2)
    ));

    public void openReceptionByNpc(final Player player) {
        this.smallTalk(this.receptionTalks, RoleplayID.COOK.id(), player, message -> {
            if (message == null) {
                this.openReception(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fDavid §8| §7" + message.getMessage().replace("%p", player.getName()));
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
        final SimpleForm form = new SimpleForm.Builder("§7» §8Rezeptionist David", "§8» §fDavid §8| §7Mit welcher Verwaltungskraft möchten Sie sprechen?")
                .addButton(new ElementButton("§8» §eSteuern\n§fFrau Bärwald §8| §9Zimmer 5, 1. OG", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/receptionist_taxes.png")), e -> {
                    final ModalForm modalForm = new ModalForm.Builder("§7» §8Rezeptionist David", "§8» §fDavid §8| §7Soll ich für Sie einen Termin bei §eFrau Bärwald §7buchen? " +
                            "", "§8» §aTermin buchen", "§8» §cAbbrechen")
                            .onYes(g -> {
                                if (!this.dateExpire.hasCooldown(player.getName())) {
                                    this.cachedDateData.put(player.getName(), this.cachedAdministrationData.get(5));
                                    player.sendMessage("§8» §fDavid §8| §7Der Termin wurde gebucht. Dieser ist nun §e5 Minuten §7lang gültig. Bitte begeben Sie sich zu §eFrau Bärwald §7ins §e1. OG§7.");
                                } else player.sendMessage("§8» §fDavid §8| §7Sie haben Bereits einen Termin bei §eFrau Bärwald §7gebucht.");
                            })
                            .onNo(g -> {

                            })
                            .build();
                    modalForm.send(e);
                })
                .build();
        form.send(player);
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
                            if (this.rathausRoleplay.runningDatesData.get(this.rathausRoleplay.cachedAdministrationData.get(5)).equals(player.getName())) {
                                this.openCassandra(player, this.rathausRoleplay.cachedDateData.get(player.getName()));
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
                                if (this.rathausRoleplay.runningDatesData.get(this.rathausRoleplay.cachedAdministrationData.get(5)).equals(player.getName())) {
                                    this.openCassandra(player, this.rathausRoleplay.cachedDateData.get(player.getName()));
                                }
                            }
                        }
                    }
                }
            }
        }

        @EventHandler
        public void on(final PlayerInteractEvent event) {
            final Player player = event.getPlayer();
            final Block block = event.getBlock();

            if (block == null) return;
            if (block instanceof BlockWallSign) {
                if (this.rathausRoleplay.dateExpire.containsCooldown(player.getName())) {
                    final Location location = block.getLocation();
                    this.rathausRoleplay.cachedAdministrationSign.forEach((e, g) -> {
                        if (location.equals(e)) {
                            if (g.getJob().equals(this.rathausRoleplay.cachedDateData.get(player.getName()).getJob())) {
                                if (this.rathausRoleplay.runningDatesData.get(this.rathausRoleplay.cachedAdministrationData.get(g.getRoom())).equals("null")) {
                                    player.teleport(g.getIn());
                                    this.rathausRoleplay.runningDatesData.replace(this.rathausRoleplay.cachedAdministrationData.get(5), "null", player.getName());
                                    switch (g.getJob()) {
                                        case "Steuern":
                                            this.openCassandra(player, g);
                                            break;
                                    }
                                } else {
                                    player.sendMessage("Spieler hat bereits einen Termin. Bitte warten.");
                                }
                            }
                        }
                    });
                }
            }
        }

        private void openCassandra(final Player player, final AdministrationData administrationData) {
            final SimpleForm form = new SimpleForm.Builder(administrationData.getName(), "§8» §f" + player.getName() + " §8| §7Hallo, ich habe einen Termin bei Ihnen!\n\n" +
                    "§8» §fCassandra §8| §7Guten Tag, §a" + player.getName() + "§7! Was kann ich für Sie tun?")
                    .addButton(new ElementButton("§8» §eChestShop\n§8- §fLizenzen §8-", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/taxes_chestshop_license.png")), e -> {
                        final ShopLicense shopLicense = this.rathausRoleplay.serverCore.getChestShopAPI().getPlayerLicense(e.getName());
                        final ShopLicense nextShopLicense = this.getNextLicense(e.getName());

                        final String nextLevel = nextShopLicense != null ? "Ich kann Ihnen eine bessere Lizenz anbieten, sofern Sie möchten." : "Da Sie bereits die beste Lizenz besitzen, " +
                                "kann ich Ihnen keine bessere anbieten.";
                        final SimpleForm.Builder licenseMain = new SimpleForm.Builder(administrationData.getName(), "§8» §f" + e.getName() + " §8| §7Ich habe ein Anliegen bezüglich der ChestShop-Lizenzen.\n\n" +
                                "§8» §fCassandra §8| §7Alles klar. Sie besitzen aktuell die §e" + shopLicense.getLicense().displayName() + "§7-Lizenz mit §e" + shopLicense.getLicense().maxPossibleShops() + "§7 " +
                                "erstellbaren Shops.\n§7" + nextLevel);
                        if (nextShopLicense != null) {
                            licenseMain.addButton(new ElementButton("§8» §9" + nextShopLicense.getLicense().displayName() + "-Lizenz\n§a$" +
                                    Economy.getAPI().getMoneyFormat().format(nextShopLicense.getLicense().money()), new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/rathaus/taxes_chestshop_license_upgrade.png")), l -> {
                                final ModalForm buyForm = new ModalForm.Builder("", "§8» §f" + player.getName() + " §8| §7Das klingt interessant...\n\n" +
                                        "§8» §fCassandra §8| §7Möchten Sie diese Lizenz für eine einmalige Zahlung von §a$" + Economy.getAPI().getMoneyFormat().format(nextShopLicense.getLicense().money()) + "§7 kaufen?"
                                        , "§8» §aLizenz kaufen", "§8» §cAbbrechen")
                                        .onYes(k -> {
                                            Economy.getAPI().getMoney(player, money -> {
                                                if (money >= nextShopLicense.getLicense().money()) {
                                                    Economy.getAPI().reduceMoney(player, nextShopLicense.getLicense().money());
                                                    this.rathausRoleplay.serverCore.getChestShopAPI().setLicense(player.getName(), nextShopLicense.getLicense(), 0);
                                                    player.sendMessage("§8» §fCassandra §8| §7Sehr gut! Sie haben die Lizenz nun gekauft. Sie können direkt die Vorteile der Lizenz nutzen.");
                                                } else {
                                                    player.sendMessage("§8» §f" + player.getName() + " §8| §7Ich habe leider nicht genug Geld dabei...");
                                                }
                                            });
                                        })
                                        .onNo(k -> {

                                        })
                                        .build();
                                buyForm.send(player);
                            });
                        }
                        licenseMain.build().send(e);
                    })
                    .addButton(new ElementButton("§8» §cTermin beenden", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/cancel.png")), e -> {
                        this.rathausRoleplay.cachedDateData.remove(e.getName());
                        this.rathausRoleplay.dateExpire.removeCooldown(e.getName());
                        this.rathausRoleplay.runningDatesData.replace(this.rathausRoleplay.cachedAdministrationData.get(5), player.getName(), "null");
                        e.teleport(administrationData.getOut());
                        e.sendMessage("Termin beendet.");
                    })
                    .build();
            form.send(player);
        }

        private ShopLicense getNextLicense(final String player) {
            final ShopLicense shopLicense = this.rathausRoleplay.serverCore.getChestShopAPI().getPlayerLicense(player);
            switch (shopLicense.getLicense()) {
                case STANDARD:
                    return new ShopLicense(player, ShopLicense.ShopLicenseType.SMALL_BUSINESS);
                case SMALL_BUSINESS:
                    return new ShopLicense(player, ShopLicense.ShopLicenseType.BUSINESS);
                case BUSINESS:
                    return new ShopLicense(player, ShopLicense.ShopLicenseType.BIG_BUSINESS);
                case BIG_BUSINESS:
                    return new ShopLicense(player, ShopLicense.ShopLicenseType.COMPANY);
                default:
                    return null;
            }
        }
    }

    public final HashMap<Integer, AdministrationData> cachedAdministrationData = new HashMap<>();
    public final HashMap<Location, AdministrationData> cachedAdministrationSign = new HashMap<>();
    public final HashMap<String, AdministrationData> cachedDateData = new HashMap<>();
    public final HashMap<AdministrationData, String> runningDatesData = new HashMap<>();

    @AllArgsConstructor
    @Getter
    public static class AdministrationData {

        private final int room;
        private final String job;
        private final String name;
        private final Location in;
        private final Location out;

    }

}
