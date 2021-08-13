package net.eltown.servercore.components.roleplay.jobs;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Sound;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
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

public class JobRoleplay {

    private final ServerCore serverCore;

    public JobRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        serverCore.getServer().getPluginManager().registerEvents(new JobListener(this), serverCore);
    }

    public final List<String> openQueue = new ArrayList<>();

    private final List<ChainMessage> cookTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Guten Tag, §a%p§7! Ich habe sehr leckere Gerichte im Angebot!", 5),
            new ChainMessage("Heute gibt es einen leckeren Mittagstisch!", 3),
            new ChainMessage("Lass dich nicht von mir stören!", 2),
            new ChainMessage("Mhhhh, das duftet gut!", 2),
            new ChainMessage("Treue Gäste gefallen mir!", 2),
            new ChainMessage("Was darf es heute sein?", 2)
    ));

    public void openCookByNpc(final Player player) {
        this.smallTalk(this.cookTalks, RoleplayID.SHOP_LUMBERJACK.id(), player, message -> {
            if (message == null) {
                this.openCook(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fDuke §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openCook(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openCook(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Koch Duke", "§7Wähle eines der aufgelisteten Gerichte aus, welches du bestellen möchtest.");

        form.addButton(new ElementButton("Kartoffeln und Steak mit Beilage\n§3§lMenü 1   §r§a$9.95", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/cook/01.png")), e -> {
            this.solveCook(player, 9.95, Item.get(ItemID.BAKED_POTATO, 0, 1), Item.get(ItemID.COOKED_BEEF, 0, 1), Item.get(ItemID.CARROT, 0, 1));
        });

        form.addButton(new ElementButton("Gemüseteller\n§3§lMenü 2   §r§a$6.95", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/cook/02.png")), e -> {
            this.solveCook(player, 6.95, Item.get(ItemID.BEETROOT, 0, 1), Item.get(ItemID.BAKED_POTATO, 0, 1), Item.get(ItemID.CARROT, 0, 1));
        });

        form.addButton(new ElementButton("Pilzsuppe mit Brot\n§3§lMenü 3   §r§a$7.95", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/cook/03.png")), e -> {
            this.solveCook(player, 7.95, Item.get(ItemID.MUSHROOM_STEW, 0, 1), Item.get(ItemID.BREAD, 0, 2));
        });

        form.addButton(new ElementButton("Rote Beete Suppe mit Brot\n§3§lMenü 4   §r§a$7.95", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/cook/04.png")), e -> {
            this.solveCook(player, 7.95, Item.get(ItemID.BEETROOT_SOUP, 0, 1), Item.get(ItemID.BREAD, 0, 2));
        });

        form.addButton(new ElementButton("Haseneintopf mit Brot\n§3§lMenü 5   §r§a$8.49", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/cook/05.png")), e -> {
            this.solveCook(player, 8.49, Item.get(ItemID.RABBIT_STEW, 0, 1), Item.get(ItemID.BREAD, 0, 2));
        });

        form.addButton(new ElementButton("Wasser 0,33l\n§3§lGetränk   §r§a$2.49", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/job/cook/06.png")), e -> {
            this.solveCook(player, 2.49, Item.get(ItemID.POTION, 0, 1));
        });

        form.build().send(player);
    }

    private void solveCook(final Player player, final double price, final Item... items) {
        for (final Item item : items) {
            if (!player.getInventory().canAddItem(item)) {
                player.sendMessage(Language.get("roleplay.job.cook.item.inventory.full"));
                this.serverCore.playSound(player, Sound.NOTE_BASS);
                return;
            }
        }

        Economy.getAPI().getMoney(player, money -> {
            if (money >= price) {
                Economy.getAPI().reduceMoney(player, price);
                player.getInventory().addItem(items);
                player.sendMessage(Language.get("roleplay.job.cook.item.bought", price));
                this.serverCore.playSound(player, Sound.UI_STONECUTTER_TAKE_RESULT);
            } else {
                player.sendMessage(Language.get("roleplay.job.cook.item.not.enough.money"));
                this.serverCore.playSound(player, Sound.NOTE_BASS);
            }
        });
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

    @RequiredArgsConstructor
    public static class JobListener implements Listener {

        private final JobRoleplay jobRoleplay;

        @EventHandler
        public void on(final PlayerInteractEntityEvent event) {
            final Player player = event.getPlayer();
            if (event.getEntity().namedTag.exist("npc_id")) {
                final String npcId = event.getEntity().namedTag.getString("npc_id");
                if (!this.jobRoleplay.openQueue.contains(player.getName())) {
                    if (npcId.equals(RoleplayID.COOK.id())) this.jobRoleplay.openCookByNpc(player);
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
                    if (!this.jobRoleplay.openQueue.contains(player.getName())) {
                        if (npcId.equals(RoleplayID.COOK.id())) this.jobRoleplay.openCookByNpc(player);
                    }
                }
            }
        }

    }

}
