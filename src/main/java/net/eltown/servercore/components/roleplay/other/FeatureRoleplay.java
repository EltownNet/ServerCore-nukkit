package net.eltown.servercore.components.roleplay.other;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.form.element.*;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.utils.Config;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.crates.Raffle;
import net.eltown.servercore.components.data.giftkeys.Giftkey;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.data.quests.Quest;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.data.rewards.DailyReward;
import net.eltown.servercore.components.data.rewards.RewardCalls;
import net.eltown.servercore.components.data.rewards.RewardPlayer;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.ChainMessage;
import net.eltown.servercore.components.roleplay.Cooldown;
import net.eltown.servercore.components.roleplay.RoleplayID;
import net.eltown.servercore.components.tasks.RaffleTask;
import net.eltown.servercore.components.tinyrabbit.Queue;
import net.eltown.servercore.listeners.EventListener;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class FeatureRoleplay {

    private final ServerCore serverCore;
    private final Config calendarConfig;

    private final HashMap<String, List<Integer>> cachedCalendar = new HashMap<>();
    private final HashMap<Integer, List<String>> cachedCalendarRewards = new HashMap<>();

    public FeatureRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        serverCore.getServer().getPluginManager().registerEvents(new FeatureRoleplay.FeatureListener(this), serverCore);
        this.calendarConfig = new Config(serverCore.getDataFolder() + "/components/adventskalender.yml", Config.YAML);

        for (final String d : this.calendarConfig.getSection("data").getKeys(false)) {
            this.cachedCalendar.put(d, this.calendarConfig.getIntegerList("data." + d));
        }

        this.cachedCalendarRewards.put(1, new ArrayList<>(Arrays.asList("money;50", "item;168:0:32:not", "item;357:0:16:not")));
        this.cachedCalendarRewards.put(2, new ArrayList<>(Arrays.asList("money;50", "item;403:0:1:CgAACQQAZW5jaAoBAAAAAgMAbHZsAgACAgBpZCQAAAA=")));
        this.cachedCalendarRewards.put(3, new ArrayList<>(Arrays.asList("money;50", "item;169:0:32:not")));
        this.cachedCalendarRewards.put(4, new ArrayList<>(Arrays.asList("money;50", "item;35:0:32:not", "item;357:0:16:not")));
        this.cachedCalendarRewards.put(5, new ArrayList<>(Arrays.asList("money;50", "xp;250", "item;266:0:16:not", "item;265:0:32:not")));
        this.cachedCalendarRewards.put(6, new ArrayList<>(Arrays.asList("money;50", "xp;250", "item;264:0:8:not", "item;368:0:16:not", "item;369:0:1:CgAACQQAZW5jaAoBAAAAAgMAbHZsAQACAgBpZAwAAAMKAFJlcGFpckNvc3QAAAAACgcAZGlzcGxheQkEAExvcmUIBAAAAB4Awqc4LS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tQQDCp3LCpzBJdGVtIHNpZ25pZXJ0IHZvbiDCpzJKdXN0aW5MTERWIMKnMGFtIMKnMjIxLjExLjIxIDE1OjU0wqcwLjQAwqdywqc3wqdlRGllIFNwZWt1bGF0aXVzc3RhbmdlOiBSaWVjaHQgdW5kIFNjaG1lY2t0IR4Awqc4LS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tCAQATmFtZRQAwqdlU3Bla3VsYXRpdXNzdGFuZ2UAAA==")));
        this.cachedCalendarRewards.put(7, new ArrayList<>(Arrays.asList("money;50", "item;46:0:32:not")));
        this.cachedCalendarRewards.put(8, new ArrayList<>(Arrays.asList("money;50", "item;47:0:16:not")));
        this.cachedCalendarRewards.put(9, new ArrayList<>(Arrays.asList("money;50", "item;384:0:32:not", "item;357:0:16:not")));
        this.cachedCalendarRewards.put(10, new ArrayList<>(Arrays.asList("money;50", "item;315:0:1:CgAACQQAZW5jaAoEAAAAAgMAbHZsBAACAgBpZAEAAAIDAGx2bAMAAgIAaWQAAAACAwBsdmwDAAICAGlkEQAAAgMAbHZsAgACAgBpZAUAAAMKAFJlcGFpckNvc3QAAAAACgcAZGlzcGxheQgEAE5hbWUZAMKncsKnNsKnbFdlaWhuYWNodHNtYW50ZWwAAA==")));
        this.cachedCalendarRewards.put(11, new ArrayList<>(Arrays.asList("money;50", "item;736:0:20:not")));
        this.cachedCalendarRewards.put(12, new ArrayList<>(Arrays.asList("money;50", "xp;250", "item;80:0:64:not", "item;174:0:32:not", "item;-11:0:32:not")));
        this.cachedCalendarRewards.put(13, new ArrayList<>(Arrays.asList("money;50", "item;421:0:1:not")));
        this.cachedCalendarRewards.put(14, new ArrayList<>(Arrays.asList("money;50", "item;46:0:32:not", "item;357:0:16:not")));
        this.cachedCalendarRewards.put(15, new ArrayList<>(Arrays.asList("money;50", "item;257:0:1:CgAACQQAZW5jaAoDAAAAAgMAbHZsAgACAgBpZBIAAAIDAGx2bAIAAgIAaWQPAAACAwBsdmwCAAICAGlkEQAAAwoAUmVwYWlyQ29zdAAAAAAKBwBkaXNwbGF5CAQATmFtZRgAwqdywqc1wqdsV2VpaG5hY2h0c2hhY2tlAAA=")));
        this.cachedCalendarRewards.put(16, new ArrayList<>(Arrays.asList("money;50", "item;384:0:32:not")));
        this.cachedCalendarRewards.put(17, new ArrayList<>(Arrays.asList("money;50", "item;736:0:20:not")));
        this.cachedCalendarRewards.put(18, new ArrayList<>(Arrays.asList("money;50", "item;216:0:16:not", "item;357:0:16:not")));
        this.cachedCalendarRewards.put(19, new ArrayList<>(Arrays.asList("money;50", "xp;250", "item;264:0:6:not", "item;-220:0:10:not")));
        this.cachedCalendarRewards.put(20, new ArrayList<>(Arrays.asList("money;50", "item;47:0:16:not")));
        this.cachedCalendarRewards.put(21, new ArrayList<>(Arrays.asList("money;50", "item;46:0:32:not")));
        this.cachedCalendarRewards.put(22, new ArrayList<>(Arrays.asList("money;50", "item;121:0:8:not", "item;357:0:16:not")));
        this.cachedCalendarRewards.put(23, new ArrayList<>(Arrays.asList("money;50", "item;384:0:32:not")));
        this.cachedCalendarRewards.put(24, new ArrayList<>(Arrays.asList("money;50", "xp;500", "item;752:0:2:not", "item;357:0:16:not")));
    }

    private final List<String> openQueue = new ArrayList<>();

    private final List<ChainMessage> rewardTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hi, §a%p§7! Liebst du Belohnungen auch so sehr wie ich?", 3),
            new ChainMessage("Vielleicht kannst du etwas einlösen. Schau dich um!", 3),
            new ChainMessage("Einlösen macht Spaß!", 2),
            new ChainMessage("Gutscheeeiiinnnneeee!", 2),
            new ChainMessage("Du kannst Gutscheine auch verschenken.", 2),
            new ChainMessage("Komm täglich zu mir, um Belohnungen zu erhalten!", 2)
    ));

    public void openRewardByNpc(final Player player) {
        this.smallTalk(this.rewardTalks, RoleplayID.FEATURE_LOLA.id(), player, message -> {
            if (message == null) {
                this.openReward(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fLola §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openReward(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openReward(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Lola's Rewards", "§8» §fLola §8| §7Bei mir gibt es alles an Geschenken, die für dich sind! Schau dich gerne um und löse ausstehende Sachen ein.");

        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (RewardCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_PLAYER_DATA:
                    final String[] d = delivery.getData();
                    final RewardPlayer rewardPlayer = new RewardPlayer(d[1], Integer.parseInt(d[2]), Long.parseLong(d[3]), Long.parseLong(d[4]));

                    final Calendar calendarNow = new GregorianCalendar();
                    calendarNow.setTime(new Date(System.currentTimeMillis()));
                    calendarNow.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
                    final Calendar calendarReward = new GregorianCalendar();
                    calendarReward.setTime(new Date(rewardPlayer.getLastReward()));
                    calendarReward.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

                    if ((calendarReward.get(Calendar.DAY_OF_YEAR) == calendarNow.get(Calendar.DAY_OF_YEAR)) && calendarReward.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR)) {
                        form.addButton(new ElementButton("§8» §1Tägliche Belohnung", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/rewards/daily-reward.png")), g -> {
                            final int day = rewardPlayer.getDay() + 1;
                            if (!(day > 14)) {
                                this.serverCore.getTinyRabbit().sendAndReceive(delivery1 -> {
                                    switch (RewardCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                        case CALLBACK_REWARDS:
                                            final List<DailyReward> rewardsToday = new ArrayList<>();
                                            Arrays.asList(delivery1.getData()[1].split("-:-")).forEach(p -> {
                                                final String[] l = p.split(">:<");
                                                rewardsToday.add(new DailyReward(l[0], l[1], Integer.parseInt(l[2]), Integer.parseInt(l[3]), l[4]));
                                            });
                                            final StringBuilder rewards = new StringBuilder("§8» §fLola §8| §7Deine heutige Belohnung hast du bereits schon abgeholt! Ich kann dir aber schon zeigen, was es morgen für dich gibt:\n\n");
                                            rewardsToday.forEach(p -> {
                                                rewards.append("§8» §r").append(p.getDescription()).append("\n").append("§1Chance: §f").append(p.getChance()).append(" Prozent").append("\n\n");
                                            });

                                            final SimpleForm nextDays = new SimpleForm.Builder("§7» §8Lola's Rewards", rewards.toString())
                                                    .addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/back.png")), this::openReward)
                                                    .build();
                                            nextDays.send(player);
                                            break;
                                    }
                                }, Queue.REWARDS_CALLBACK, RewardCalls.REQUEST_REWARDS.name(), String.valueOf(day));
                            } else {
                                final SimpleForm nextDays = new SimpleForm.Builder("§7» §8Lola's Rewards", "§8» §fLola §8| §7Da du deinen §914-Tage-Streak §7vollendet hast, startest du morgen wieder von vorn. Viel Glück bei den nächsten 14 Tagen!\n\n")
                                        .addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/back.png")), this::openReward)
                                        .build();
                                nextDays.send(player);
                            }
                        });
                        return;
                    }

                    if (((calendarReward.get(Calendar.DAY_OF_YEAR) + 1) == calendarNow.get(Calendar.DAY_OF_YEAR)) && (calendarReward.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR)) && !(rewardPlayer.getDay() >= 14)) {
                        this.serverCore.getTinyRabbit().sendAndReceive(delivery1 -> {
                            switch (RewardCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                case CALLBACK_REWARDS:
                                    final List<DailyReward> rewardsToday = new ArrayList<>();
                                    Arrays.asList(delivery1.getData()[1].split("-:-")).forEach(p -> {
                                        final String[] l = p.split(">:<");
                                        rewardsToday.add(new DailyReward(l[0], l[1], Integer.parseInt(l[2]), Integer.parseInt(l[3]), l[4]));
                                    });
                                    final StringBuilder rewards = new StringBuilder("§8» §fLola §8| §7Herzlichen Glückwunsch! Du bist bei Tag §9" + (rewardPlayer.getDay() + 1) + " §7angelangt. Hier unten sind deine heutigen Belohnungen aufgelistet.\n\n");
                                    rewardsToday.forEach(p -> {
                                        rewards.append("§8» §r").append(p.getDescription()).append("\n").append("§1Chance: §f").append(p.getChance()).append(" Prozent").append("\n\n");
                                    });

                                    form.addButton(new ElementButton("§8» §1Tägliche Belohnung §8[§c§l!§r§8]", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/rewards/daily-reward.png")), e -> {
                                        final ModalForm claimForm = new ModalForm.Builder("§7» §8Tägliche Belohnung abholen", rewards.toString(),
                                                "§8» §aAbholen", "§8» §cZurück")
                                                .onYes(g -> {
                                                    this.serverCore.getTinyRabbit().sendAndReceive(delivery2 -> {
                                                        if (delivery2.getKey().equalsIgnoreCase("REQUEST_PLAYTIME")) {
                                                            final long day = Long.parseLong(delivery2.getData()[2]);
                                                            final long minutes = day / 1000 / 60;
                                                            if (minutes >= 30) {
                                                                this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_ADD_STREAK.name(), player.getName());
                                                                this.givePlayerDailyReward(player, rewardsToday);
                                                            } else {
                                                                player.sendMessage(Language.get("reward.onlinetime", 30 - (int) minutes));
                                                            }
                                                        }
                                                    }, Queue.PROXY_PLAYTIME, "REQUEST_PLAYTIME", player.getName());
                                                })
                                                .onNo(this::openReward)
                                                .build();
                                        claimForm.send(player);
                                    });
                                    break;
                            }
                        }, Queue.REWARDS_CALLBACK, RewardCalls.REQUEST_REWARDS.name(), String.valueOf(rewardPlayer.getDay() + 1));
                    } else if (rewardPlayer.getDay() >= 14) {
                        this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_RESET_STREAK.name(), player.getName());
                        this.serverCore.getTinyRabbit().sendAndReceive(delivery1 -> {
                            switch (RewardCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                case CALLBACK_REWARDS:
                                    final List<DailyReward> rewardsToday = new ArrayList<>();
                                    Arrays.asList(delivery1.getData()[1].split("-:-")).forEach(p -> {
                                        final String[] l = p.split(">:<");
                                        rewardsToday.add(new DailyReward(l[0], l[1], Integer.parseInt(l[2]), Integer.parseInt(l[3]), l[4]));
                                    });
                                    final StringBuilder rewards = new StringBuilder("§8» §fLola §8| §7Da du deinen §914-Tage-Streak §7vollendet hast, startest du wieder von vorn. Viel Glück bei den nächsten 14 Tagen!\n\n");
                                    rewardsToday.forEach(p -> {
                                        rewards.append("§8» §r").append(p.getDescription()).append("\n").append("§1Chance: §f").append(p.getChance()).append(" Prozent").append("\n\n");
                                    });

                                    form.addButton(new ElementButton("§8» §1Tägliche Belohnung §8[§c§l!§r§8]", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/rewards/daily-reward.png")), e -> {
                                        final ModalForm claimForm = new ModalForm.Builder("§7» §8Tägliche Belohnung abholen", rewards.toString(),
                                                "§8» §aAbholen", "§8» §cZurück")
                                                .onYes(g -> {
                                                    this.serverCore.getTinyRabbit().sendAndReceive(delivery2 -> {
                                                        if (delivery2.getKey().equalsIgnoreCase("REQUEST_PLAYTIME")) {
                                                            final long day = Long.parseLong(delivery2.getData()[2]);
                                                            final long minutes = day / 1000 / 60;
                                                            if (minutes >= 30) {
                                                                this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_ADD_STREAK.name(), player.getName());
                                                                this.givePlayerDailyReward(player, rewardsToday);
                                                            } else {
                                                                player.sendMessage(Language.get("reward.onlinetime", 30 - (int) minutes));
                                                            }
                                                        }
                                                    }, Queue.PROXY_PLAYTIME, "REQUEST_PLAYTIME", player.getName());
                                                })
                                                .onNo(this::openReward)
                                                .build();
                                        claimForm.send(player);
                                    });
                                    break;
                            }
                        }, Queue.REWARDS_CALLBACK, RewardCalls.REQUEST_REWARDS.name(), String.valueOf(1));
                    } else {
                        this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_RESET_STREAK.name(), player.getName());
                        this.serverCore.getTinyRabbit().sendAndReceive(delivery1 -> {
                            switch (RewardCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                case CALLBACK_REWARDS:
                                    final List<DailyReward> rewardsToday = new ArrayList<>();
                                    Arrays.asList(delivery1.getData()[1].split("-:-")).forEach(p -> {
                                        final String[] l = p.split(">:<");
                                        rewardsToday.add(new DailyReward(l[0], l[1], Integer.parseInt(l[2]), Integer.parseInt(l[3]), l[4]));
                                    });
                                    final StringBuilder rewards = new StringBuilder("§8» §fLola §8| §7Du hast leider einen Tag verpasst, daher startest du wieder bei §9Tag 1§7. Komm jeden Tag vorbei, um deine Belohnungen abzuholen. Es lohnt sich!\n\n");
                                    rewardsToday.forEach(p -> {
                                        rewards.append("§8» §r").append(p.getDescription()).append("\n").append("§1Chance: §f").append(p.getChance()).append(" Prozent").append("\n\n");
                                    });

                                    form.addButton(new ElementButton("§8» §1Tägliche Belohnung §8[§c§l!§r§8]", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/rewards/daily-reward.png")), e -> {
                                        final ModalForm claimForm = new ModalForm.Builder("§7» §8Tägliche Belohnung abholen", rewards.toString(),
                                                "§8» §aAbholen", "§8» §cZurück")
                                                .onYes(g -> {
                                                    this.serverCore.getTinyRabbit().sendAndReceive(delivery2 -> {
                                                        if (delivery2.getKey().equalsIgnoreCase("REQUEST_PLAYTIME")) {
                                                            final long day = Long.parseLong(delivery2.getData()[2]);
                                                            final long minutes = day / 1000 / 60;
                                                            if (minutes >= 30) {
                                                                this.serverCore.getTinyRabbit().send(Queue.REWARDS_RECEIVE, RewardCalls.REQUEST_ADD_STREAK.name(), player.getName());
                                                                this.givePlayerDailyReward(player, rewardsToday);
                                                            } else {
                                                                player.sendMessage(Language.get("reward.onlinetime", 30 - (int) minutes));
                                                            }
                                                        }
                                                    }, Queue.PROXY_PLAYTIME, "REQUEST_PLAYTIME", player.getName());
                                                })
                                                .onNo(this::openReward)
                                                .build();
                                        claimForm.send(player);
                                    });
                                    break;
                            }
                        }, Queue.REWARDS_CALLBACK, RewardCalls.REQUEST_REWARDS.name(), String.valueOf(1));
                    }

                    break;
            }
        }, Queue.REWARDS_CALLBACK, RewardCalls.REQUEST_PLAYER_DATA.name(), player.getName());

        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
            switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NULL:
                    form.addButton(new ElementButton("§8» §1Gutscheine §8[§c0§8]", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/rewards/giftkeys.png")), e -> {
                        player.sendMessage("§8» §fLola §8| §7Oh, anscheinend hast du keine ausstehenden Gutscheine. Spieler können dir Gutscheine schenken oder du kannst welche bei Events erhalten.");
                    });
                    break;
                case CALLBACK_USER_CODES:
                    final List<String> codes = Arrays.asList(delivery.getData()[1].split(">:<"));
                    form.addButton(new ElementButton("§8» §1Gutscheine §8[§c" + codes.size() + "§8]", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/rewards/giftkeys.png")), e -> {
                        this.openPlayerGiftKeys(player, codes);
                    });
                    break;
            }
        }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_USER_CODES.name(), player.getName());

        form.addButton(new ElementButton("§8» §4Advents§fkalender", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/rewards/adventskalender.png")), e -> {
            final Calendar calendarNow = new GregorianCalendar();
            calendarNow.setTime(new Date(System.currentTimeMillis()));
            calendarNow.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));

            if (calendarNow.get(Calendar.DAY_OF_MONTH) >= 25) {
                player.sendMessage("§8» §fCore §8| §7Alle Türchen des Adventskalenders sind verschlossen.");
                return;
            }

            if (calendarNow.get(Calendar.MONTH) == Calendar.DECEMBER) {
                final SimpleForm.Builder calendar = new SimpleForm.Builder("§7» §4Advents§fkalender", "§8» §fKlicke eines der Türchen an, um deine heutige Adventbelohnung zu erhalten!\n§7Heute ist Türchen §9" + calendarNow.get(Calendar.DAY_OF_MONTH) + " §7dran.\n\n§8[§c!§8] §cStelle sicher, dass du genug Plätze in deinem Inventar frei hast.");
                if (!this.cachedCalendar.containsKey(player.getName())) this.cachedCalendar.put(player.getName(), new ArrayList<>());
                for (int i = 1; i < 25; i++) {
                    final int day = i;
                    final String status = calendarNow.get(Calendar.DAY_OF_MONTH) == day ? !this.cachedCalendar.get(player.getName()).contains(day) ? "§2§lÖffnen" : "" : "";
                    calendar.addButton(new ElementButton("§8» §4Türchen   §f§l" + day + "\n" + status), g -> {
                        if (calendarNow.get(Calendar.DAY_OF_MONTH) == day) {
                            if (!this.cachedCalendar.get(player.getName()).contains(day)) {
                                this.cachedCalendarRewards.get(day).forEach(reward -> {
                                    final String[] data = reward.split(";");
                                    switch (data[0]) {
                                        case "xp":
                                            this.serverCore.getLevelAPI().addExperience(player, Double.parseDouble(data[1]));
                                            player.sendMessage("§8» §fCore §8| §7Du hast §d" + Double.parseDouble(data[1]) + " XP-Punkte §7erhalten.");
                                            break;
                                        case "money":
                                            Economy.getAPI().addMoney(player, Double.parseDouble(data[1]));
                                            player.sendMessage("§8» §fCore §8| §7Du hast §a$" + Double.parseDouble(data[1]) + " §7erhalten.");
                                            break;
                                        case "item":
                                            final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(data[1]);
                                            player.getInventory().addItem(item);
                                            player.sendMessage("§8» §fCore §8| §7Du hast §9" + item.getCount() + "x §7das Item §9" + item.getName() + " §7erhalten.");
                                            break;
                                    }
                                });
                                player.sendMessage("§8» §fCore §8| §2Du hast das §0" + day + ". Türchen §2geöffnet!");
                                this.serverCore.playSound(player, Sound.RANDOM_LEVELUP);

                                final List<Integer> days = this.cachedCalendar.get(player.getName());
                                days.add(day);
                                this.calendarConfig.set("data." + player.getName(), days);
                                this.calendarConfig.save();
                                this.calendarConfig.reload();

                                if (day == 24 && this.cachedCalendar.get(player.getName()).size() == 24) {
                                    final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount("-206:0:1:CgAAAwoAUmVwYWlyQ29zdAAAAAAKBwBkaXNwbGF5CQQATG9yZQgEAAAAHgDCpzgtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0+AMKncsKnMEl0ZW0gc2lnbmllcnQgdm9uIMKnMkphbkxMRFYgwqcwYW0gwqcyMjEuMTEuMjEgMTA6MzPCpzAuNADCp3LCpzfCpzZOdW4ga2FubiBlbmRsaWNoIGRlciBXZWlobmFjaHRzbWFubiBrb21tZW4hHgDCpzgtLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0IBABOYW1lIQDCp3LCp2PCp2xXZWlobmFjaHRzZ2xvY2tlIMKncjIwMjEAAA==");
                                    player.getInventory().addItem(item);
                                    player.sendMessage("§8» §fCore §8| §2Du hast ein §0Spezial-Item §2erhalten, da du alle Türchen geöffnet hast!");
                                }

                                if (day == 24) player.sendMessage("§8» §fCore §8| §cFrohe Weihnachten!");
                            } else player.sendMessage("§8» §fCore §8| §7Dieses Türchen hast du bereits geöffnet.");
                        } else player.sendMessage("§8» §fCore §8| §7Dieses Türchen ist verschlossen.");
                    });
                }
                calendar.addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/back.png")), this::openReward);
                calendar.build().send(player);
            } else player.sendMessage("§8» §fCore §8| §7Dieses Feature ist nur im Dezember verfügbar.");
        });

        form.build().send(player);
    }

    private void givePlayerDailyReward(final Player player, final List<DailyReward> dailyRewards) {
        final List<DailyReward> choosenRewards = new ArrayList<>();
        dailyRewards.forEach(e -> {
            boolean is = Math.random() * 100 <= e.getChance();
            if (is) choosenRewards.add(e);
        });
        final AtomicReference<DailyReward> dailyReward = new AtomicReference<>();

        if (choosenRewards.isEmpty()) choosenRewards.add(dailyRewards.get(0));

        if (choosenRewards.size() != 1) {
            choosenRewards.forEach(e -> {
                boolean is = Math.random() * 100 <= e.getChance();
                if (is) {
                    dailyReward.set(e);
                }
            });
        } else dailyReward.set(choosenRewards.get(0));

        final String data = dailyReward.get().getData();
        final String[] dataSplit = data.split(";");
        switch (dataSplit[0]) {
            case "item":
                player.getInventory().addItem(SyncAPI.ItemAPI.pureItemFromStringWithCount(dataSplit[1]));
                break;
            case "xp":
                this.serverCore.getLevelAPI().addExperience(player, Double.parseDouble(dataSplit[1]));
                break;
            case "money":
                Economy.getAPI().addMoney(player, Double.parseDouble(dataSplit[1]));
                break;
        }
        player.sendMessage(Language.get("reward.received", dailyReward.get().getDescription(), dailyReward.get().getDay()));
    }

    public void openPlayerGiftKeys(final Player player, final List<String> codes) {
        final SimpleForm.Builder giftkeyForm = new SimpleForm.Builder("§7» §8Deine Gutscheine", "§8» §fLola §8| §7Hier siehst du deine verfügbaren Gutscheine. Klicke einen Gutschein an, um mehr Informationen zu erhalten.");
        codes.forEach(code -> this.serverCore.getTinyRabbit().sendAndReceive(delivery1 -> {
            switch (GiftkeyCalls.valueOf(delivery1.getKey().toUpperCase())) {
                case CALLBACK_KEY:
                    final String[] d = delivery1.getData()[1].split(">>");
                    final Giftkey giftkey = new Giftkey(d[0], Integer.parseInt(d[1]), Arrays.asList(d[2].split(">:<")), Arrays.asList(d[3].split(">:<")), Arrays.asList(d[4].split(">:<")));
                    giftkeyForm.addButton(new ElementButton("§8» §1" + giftkey.getKey() + "\n§8[§2Einlösbar§8]"), a -> {
                        this.openGiftKeyInformation(player, giftkey, codes);
                    });
                    break;
            }
        }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_GET_KEY.name(), code));
        giftkeyForm.addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/back.png")), e -> this.openReward(player));
        giftkeyForm.build().send(player);
    }

    public void openGiftKeyInformation(final Player player, final Giftkey nGiftkey, final List<String> codes) {
        final SimpleForm.Builder giftkeyInfoForm = new SimpleForm.Builder("§7» §8Gutschein Information", "§8» §1Gutschein: §f" + nGiftkey.getKey() + "\n" +
                "§8» §1Eingelöst: §f" + (nGiftkey.getUses().size() - 1) + "/" + nGiftkey.getMaxUses() + "\n§8» §1Belohnungen: §f" + nGiftkey.getRewards().size() + "\n\n");
        giftkeyInfoForm.addButton(new ElementButton("§8» §aJetzt einlösen", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/rewards/giftkey-redeem.png")), g -> {
            this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case CALLBACK_NULL:
                        player.sendMessage(Language.get("giftkey.invalid.key", nGiftkey.getKey()));
                        break;
                    case CALLBACK_KEY:
                        final String[] d = delivery.getData()[1].split(">>");
                        final Giftkey giftkey = new Giftkey(d[0], Integer.parseInt(d[1]), Arrays.asList(d[2].split(">:<")), Arrays.asList(d[3].split(">:<")), Arrays.asList(d[4].split(">:<")));

                        if (giftkey.getUses().contains(player.getName())) {
                            player.sendMessage(Language.get("giftkey.already.redeemed"));
                            return;
                        }

                        final ModalForm modalForm = new ModalForm.Builder("§7» §8Key einlösen", "Möchtest du diesen Key einlösen und die Belohnungen, die dahinter stecken erhalten? Jeder Key kann nur einmal von dir eingelöst werden.", "§7» §aEinlösen", "§7» §cAbbrechen")
                                .onYes(e -> {
                                    this.serverCore.playSound(player, Sound.RANDOM_LEVELUP);
                                    this.serverCore.getTinyRabbit().sendAndReceive(delivery1 -> {
                                        switch (GiftkeyCalls.valueOf(delivery1.getKey().toUpperCase())) {
                                            case CALLBACK_ALREADY_REDEEMED:
                                                player.sendMessage(Language.get("giftkey.already.redeemed"));
                                                break;
                                            case CALLBACK_NULL:
                                                player.sendMessage(Language.get("giftkey.invalid.key", giftkey.getKey()));
                                                break;
                                            case CALLBACK_REDEEMED:
                                                giftkey.getRewards().forEach(reward -> {
                                                    final String[] raw = reward.split(";");
                                                    switch (raw[0]) {
                                                        case "item":
                                                            final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(raw[1]);
                                                            player.getInventory().addItem(item);
                                                            player.sendMessage(Language.get("giftkey.reward.item", item.getName(), item.getCount()));
                                                            break;
                                                        case "money":
                                                            final double amount = Double.parseDouble(raw[1]);
                                                            Economy.getAPI().addMoney(player, amount);
                                                            player.sendMessage(Language.get("giftkey.reward.money", amount));
                                                            break;
                                                        case "levelxp":
                                                            final double xp = Double.parseDouble(raw[1]);
                                                            this.serverCore.getLevelAPI().addExperience(player, xp);
                                                            player.sendMessage(Language.get("giftkey.reward.xp", xp));
                                                            break;
                                                        case "rank":
                                                            final String rank = raw[1];
                                                            final String unit = raw[2];
                                                            final int time = Integer.parseInt(raw[3]);
                                                            final long duration = this.serverCore.getDuration(unit, time);
                                                            this.serverCore.getTinyRabbit().sendAndReceive((o -> {
                                                                switch (GroupCalls.valueOf(o.getKey().toUpperCase())) {
                                                                    case CALLBACK_GROUP_DOES_NOT_EXIST:
                                                                        player.sendMessage("Fehler: RedeemCommand :: " + raw[0] + " value: " + rank);
                                                                        break;
                                                                    case CALLBACK_PLAYER_ALREADY_IN_GROUP:
                                                                        player.sendMessage(Language.get("giftkey.reward.rank.already", rank));
                                                                        break;
                                                                    case CALLBACK_SUCCESS:
                                                                        player.sendMessage(Language.get("giftkey.reward.rank", rank));
                                                                        break;
                                                                }
                                                            }), Queue.GROUPS, GroupCalls.REQUEST_SET_GROUP.name(), player.getName(), rank, "SYSTEM/GIFTKEY", String.valueOf(duration));
                                                            break;
                                                        case "crate":
                                                            final String crate = raw[1];
                                                            final int i = Integer.parseInt(raw[2]);
                                                            this.serverCore.getCrateAPI().addCrate(player.getName(), crate, i);
                                                            player.sendMessage(Language.get("giftkey.reward.crate", this.serverCore.getFeatureRoleplay().convertToDisplay(crate), i));
                                                            break;
                                                        default:
                                                            player.sendMessage("Fehler: RedeemCommand :: " + raw[0]);
                                                            break;
                                                    }
                                                });
                                                break;
                                        }
                                    }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_REDEEM_KEY.name(), giftkey.getKey(), player.getName());
                                })
                                .onNo(e -> {
                                })
                                .build();
                        modalForm.send(g);
                        break;
                }
            }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_GET_KEY.name(), nGiftkey.getKey());
        });
        giftkeyInfoForm.addButton(new ElementButton("§8» §9Gutschein verschenken", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/rewards/giftkey-donate.png")), e -> {
            final CustomForm form = new CustomForm.Builder("§7» §8Gutschein verschenken")
                    .addElement(new ElementLabel("§8» §fLola §8| §7Du kannst Gutscheine einfach an einen anderen Spieler verschenken. Dieser wird dann in seinem Gutschein-Menü angezeigt und bei dir verschwindet der Gutschein. " +
                            "Du oder andere Spieler können den Code aber trotzdem noch einlösen."))
                    .addElement(new ElementInput("§8» §fBitte gebe einen Spielernamen an.\n§8[§c§l!§r§8] §cAchte auf die Groß- und Kleinschreibung des Namens. Der Name muss korrekt sein!", "EltownSpielerHD123"))
                    .onSubmit((g, h) -> {
                        final String target = h.getInputResponse(1);

                        if (target.isEmpty() || target.equals(player.getName())) {
                            player.sendMessage(Language.get("giftkey.invalid.target"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                            return;
                        }

                        this.serverCore.getTinyRabbit().sendAndReceive(delivery -> {
                            switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                                case CALLBACK_MARK_ADDED:
                                    player.sendMessage(Language.get("giftkey.mark.added", target));
                                    this.serverCore.playSound(player, Sound.RANDOM_LEVELUP);
                                    break;
                            }
                        }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_ADD_MARK.name(), nGiftkey.getKey(), target, player.getName());
                    })
                    .onClose(g -> {
                        this.openGiftKeyInformation(g, nGiftkey, codes);
                    })
                    .build();
            form.send(player);
        });
        if (codes != null) giftkeyInfoForm.addButton(new ElementButton("§8» §cZurück", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/back.png")), e -> this.openPlayerGiftKeys(player, codes));
        giftkeyInfoForm.build().send(player);
    }

    private final List<ChainMessage> johnTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Guten Tag, §a%p§7! Was wünschst du dir heute?", 3),
            new ChainMessage("Mit etwas Glück, bekommst du das was du möchtest!", 3),
            new ChainMessage("Also, ich habe immer Glück!", 2),
            new ChainMessage("Gewinne, Gewinne, Gewinne!", 2)
    ));

    public void openJohnByNpc(final Player player) {
        this.smallTalk(this.johnTalks, RoleplayID.FEATURE_JOHN.id(), player, message -> {
            if (message == null) {
                this.openJohn(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fJohn §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openJohn(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openJohn(final Player player) {
        final SimpleForm form = new SimpleForm.Builder("§7» §8John", "§8» §fJohn §8| §7Schön, dass du hier bist! Welche Truhe möchtest du heute kaufen? Lass dir gerne Zeit und suche dir eine aus!")
                .addButton(new ElementButton("§8» §7§lGewöhnliche §r§7Truhe\n§f§oAb: §r§f$49.95", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/crates/common-display.png")), e -> {
                    final CustomForm form1 = new CustomForm.Builder("§7» §8Angebot wählen")
                            .addElement(new ElementStepSlider("§8» §fJohn §8| §7Ich habe einige gute Angebote! Suche dir gerne eins aus, wenn du möchtest, um Rabatte zu erhalten", Arrays.asList(
                                    "\n\n§8» §f" + player.getName() + " §8| §7Danke, aber ich habe kein Interesse an einem Angebot.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §95 Truhen §7für §9$209.95 §7würde ich gerne nehmen!",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Ich habe Interesse an dem Angebot §910 Truhen §7für §9$419.95§7.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §920 Truhen §7für §9$839.95 §7klingt sehr interessant."
                            )))
                            .onSubmit((g, h) -> {
                                switch (h.getStepSliderResponse(0).getElementID()) {
                                    case 0:
                                        this.openSelectCrate(player, "common", 49.95);
                                        break;
                                    case 1:
                                        this.openBuyCrate(player, "common", 5, 209.95);
                                        break;
                                    case 2:
                                        this.openBuyCrate(player, "common", 10, 419.95);
                                        break;
                                    case 3:
                                        this.openBuyCrate(player, "common", 20, 839.95);
                                        break;
                                }
                            })
                            .build();
                    form1.send(player);
                })
                .addButton(new ElementButton("§8» §1§lUngewöhnliche §r§1Truhe\n§f§oAb: §r§f$199.95", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/crates/uncommon-display.png")), e -> {
                    final CustomForm form1 = new CustomForm.Builder("§7» §8Angebot wählen")
                            .addElement(new ElementStepSlider("§8» §fJohn §8| §7Ich habe einige gute Angebote! Suche dir gerne eins aus, wenn du möchtest, um Rabatte zu erhalten", Arrays.asList(
                                    "\n\n§8» §f" + player.getName() + " §8| §7Danke, aber ich habe kein Interesse an einem Angebot.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §95 Truhen §7für §9$919.95 §7würde ich gerne nehmen!",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Ich habe Interesse an dem Angebot §910 Truhen §7für §9$1839.95§7.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §920 Truhen §7für §9$3679.95 §7klingt sehr interessant."
                            )))
                            .onSubmit((g, h) -> {
                                switch (h.getStepSliderResponse(0).getElementID()) {
                                    case 0:
                                        this.openSelectCrate(player, "uncommon", 199.95);
                                        break;
                                    case 1:
                                        this.openBuyCrate(player, "uncommon", 5, 919.95);
                                        break;
                                    case 2:
                                        this.openBuyCrate(player, "uncommon", 10, 1839.95);
                                        break;
                                    case 3:
                                        this.openBuyCrate(player, "uncommon", 20, 3679.95);
                                        break;
                                }
                            })
                            .build();
                    form1.send(player);
                })
                .addButton(new ElementButton("§8» §5§lEpische §r§5Truhe\n§f§oAb: §r§f$399.95", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/crates/epic-display.png")), e -> {
                    final CustomForm form1 = new CustomForm.Builder("§7» §8Angebot wählen")
                            .addElement(new ElementStepSlider("§8» §fJohn §8| §7Ich habe einige gute Angebote! Suche dir gerne eins aus, wenn du möchtest, um Rabatte zu erhalten", Arrays.asList(
                                    "\n\n§8» §f" + player.getName() + " §8| §7Danke, aber ich habe kein Interesse an einem Angebot.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §93 Truhen §7für §9$949.95 §7würde ich gerne nehmen!",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Ich habe Interesse an dem Angebot §98 Truhen §7für §9$2699.95§7.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §916 Truhen §7für §9$5399.95 §7klingt sehr interessant."
                            )))
                            .onSubmit((g, h) -> {
                                switch (h.getStepSliderResponse(0).getElementID()) {
                                    case 0:
                                        this.openSelectCrate(player, "epic", 399.95);
                                        break;
                                    case 1:
                                        this.openBuyCrate(player, "epic", 3, 949.95);
                                        break;
                                    case 2:
                                        this.openBuyCrate(player, "epic", 8, 2699.95);
                                        break;
                                    case 3:
                                        this.openBuyCrate(player, "epic", 16, 5399.95);
                                        break;
                                }
                            })
                            .build();
                    form1.send(player);
                })
                .addButton(new ElementButton("§8» §g§lLegendäre §r§gTruhe\n§f§oAb: §r§f$799.95", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/crates/legendary-display.png")), e -> {
                    final CustomForm form1 = new CustomForm.Builder("§7» §8Angebot wählen")
                            .addElement(new ElementStepSlider("§8» §fJohn §8| §7Ich habe einige gute Angebote! Suche dir gerne eins aus, wenn du möchtest, um Rabatte zu erhalten", Arrays.asList(
                                    "\n\n§8» §f" + player.getName() + " §8| §7Danke, aber ich habe kein Interesse an einem Angebot.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §93 Truhen §7für §9$2149.95 §7würde ich gerne nehmen!",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Ich habe Interesse an dem Angebot §98 Truhen §7für §9$5899.95§7.",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Das Angebot §916 Truhen §7für §9$11799.95 §7klingt sehr interessant."
                            )))
                            .onSubmit((g, h) -> {
                                switch (h.getStepSliderResponse(0).getElementID()) {
                                    case 0:
                                        this.openSelectCrate(player, "legendary", 799.95);
                                        break;
                                    case 1:
                                        this.openBuyCrate(player, "legendary", 3, 2149.95);
                                        break;
                                    case 2:
                                        this.openBuyCrate(player, "legendary", 8, 5899.95);
                                        break;
                                    case 3:
                                        this.openBuyCrate(player, "legendary", 16, 11799.95);
                                        break;
                                }
                            })
                            .build();
                    form1.send(player);
                })
                .build();
        form.send(player);
    }

    public void openSelectCrate(final Player player, final String crate, final double price) {
        final CustomForm form = new CustomForm.Builder("§7» §8Stückzahl wählen")
                .addElement(new ElementSlider("§8» §fJohn §8| §7Wie oft möchtest du " + this.convertToDisplay(crate) + " §7kaufen? Eine kostet §9$" + Economy.getAPI().getMoneyFormat().format(price) + "§7", 1, 50, 1, 1))
                .onSubmit((g, h) -> {
                    final int amount = (int) h.getSliderResponse(0);
                    this.openBuyCrate(player, crate, amount, amount * price);
                })
                .build();
        form.send(player);
    }

    public void openBuyCrate(final Player player, final String crate, final int amount, final double price) {
        final ModalForm form = new ModalForm.Builder("§7» §8Glücksboxen kaufen", "§8» §fJohn §8| §7Möchtest du die §9" + amount + "x " + this.convertToDisplay(crate) + " §7für §9$" + Economy.getAPI().getMoneyFormat().format(price) + " §7kaufen?",
                "§8» §aKaufen", "§8» §cAbbrechen")
                .onYes(e -> {
                    Economy.getAPI().getMoney(player, money -> {
                        if (money >= price) {
                            Economy.getAPI().reduceMoney(player, price);
                            this.serverCore.getCrateAPI().addCrate(player.getName(), crate, amount);
                            this.serverCore.playSound(player, Sound.RANDOM_LEVELUP, 2, 3);
                            player.sendMessage(Language.get("crate.bought", this.convertToDisplay(crate), amount, price));
                        } else {
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                            player.sendMessage(Language.get("crate.not.enough.money"));
                        }
                    });
                })
                .onNo(e -> this.serverCore.playSound(player, Sound.NOTE_BASS))
                .build();
        form.send(player);
    }

    public String convertToDisplay(final String crate) {
        switch (crate) {
            case "common":
                return "§7§lGewöhnliche §r§7Truhe§r";
            case "uncommon":
                return "§1§lUngewöhnliche §r§1Truhe§r";
            case "epic":
                return "§5§lEpische §r§5Truhe§r";
            case "legendary":
                return "§g§lLegendäre §r§gTruhe§r";
            default:
                return "null";
        }
    }

    public boolean crateInUse = false;

    public void openCrate(final Player player) {
        if (this.crateInUse) {
            player.sendMessage(Language.get("crate.already.in.use"));
            this.serverCore.playSound(player, Sound.NOTE_BASS);
            return;
        }
        this.serverCore.playSound(player, Sound.RANDOM_ENDERCHESTOPEN, 2, 3);
        this.serverCore.getCrateAPI().getPlayerData(player.getName(), map -> {
            final SimpleForm form = new SimpleForm.Builder("§7» §8Glückstruhen", "§8» §fWähle eine Truhe aus, um diese zu öffnen. Du kannst dir bei John Truhen kaufen. Manchmal hat er auch spezielle Angebote!\n\n")
                    .addButton(new ElementButton("§8» §7§lGewöhnliche §r§7Truhe\n§f§oVerfügbar: §r§8[§f" + map.getOrDefault("common", 0) + "§8]", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/crates/common-display.png")), e -> {
                        if (this.crateInUse) {
                            player.sendMessage(Language.get("crate.already.in.use"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                            return;
                        }
                        if (map.getOrDefault("common", 0) > 0) {
                            this.raffleCrate(player, "common");
                        } else {
                            player.sendMessage(Language.get("crate.no.crates"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                        }
                    })
                    .addButton(new ElementButton("§8» §1§lUngewöhnliche §r§1Truhe\n§f§oVerfügbar: §r§8[§f" + map.getOrDefault("uncommon", 0) + "§8]", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/crates/uncommon-display.png")), e -> {
                        if (this.crateInUse) {
                            player.sendMessage(Language.get("crate.already.in.use"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                            return;
                        }
                        if (map.getOrDefault("uncommon", 0) > 0) {
                            this.raffleCrate(player, "uncommon");
                        } else {
                            player.sendMessage(Language.get("crate.no.crates"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                        }
                    })
                    .addButton(new ElementButton("§8» §5§lEpische §r§5Truhe\n§f§oVerfügbar: §r§8[§f" + map.getOrDefault("epic", 0) + "§8]", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/crates/epic-display.png")), e -> {
                        if (this.crateInUse) {
                            player.sendMessage(Language.get("crate.already.in.use"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                            return;
                        }
                        if (map.getOrDefault("epic", 0) > 0) {
                            this.raffleCrate(player, "epic");
                        } else {
                            player.sendMessage(Language.get("crate.no.crates"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                        }
                    })
                    .addButton(new ElementButton("§8» §g§lLegendäre §r§gTruhe\n§f§oVerfügbar: §r§8[§f" + map.getOrDefault("legendary", 0) + "§8]", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/crates/legendary-display.png")), e -> {
                        if (this.crateInUse) {
                            player.sendMessage(Language.get("crate.already.in.use"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                            return;
                        }
                        if (map.getOrDefault("legendary", 0) > 0) {
                            this.raffleCrate(player, "legendary");
                        } else {
                            player.sendMessage(Language.get("crate.no.crates"));
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                        }
                    })
                    .build();
            form.send(player);
        });
    }

    public FloatingTextParticle crateHologram = new FloatingTextParticle(new Location(59.5, 69.5, 77.5, Server.getInstance().getDefaultLevel()), "§5§lGlückstruhe");

    public void raffleCrate(final Player player, final String crate) {
        this.serverCore.getCrateAPI().removeCrate(player.getName(), crate, 1);

        this.serverCore.getCrateAPI().getCrateRewards(crate, rewards -> {
            final Raffle raffle = new Raffle(new ArrayList<>(rewards));
            Server.getInstance().getScheduler().scheduleDelayedTask(new RaffleTask(this.serverCore, raffle, player), 0, true);
            this.crateInUse = true;
        });
    }

    private final List<ChainMessage> brianTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hallo... §a%p§7! Was... kann ich für dich tun?", 3),
            new ChainMessage("Nicht jeder kann Magie anwenden!", 2),
            new ChainMessage("Verärgere niemals einen Magier!", 2),
            new ChainMessage("Meine Magie... ist nichts für schwache Nerven!", 2)
    ));

    public void openBrianByNpc(final Player player) {
        this.smallTalk(this.brianTalks, RoleplayID.FEATURE_BRIAN.id(), player, message -> {
            if (message == null) {
                this.openBrian(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fBrian §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openBrian(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openBrian(final Player player) {
        final StringBuilder builder = new StringBuilder("§8» §fBrian §8| §7Suche dir eine meiner Dienstleistungen aus und profitiere davon gegen einen günstigen Preis!");

        if (this.brianFly.containsKey(player.getName())) {
            builder.append("\n\n§8» §5Die Magie des Fliegens §7endet in: ").append("§f").append(this.serverCore.getRemainingTimeFuture(this.brianFly.get(player.getName())));
        }

        final SimpleForm form = new SimpleForm.Builder("§7» §8Lehrling Brian", builder.toString())
                .addButton(new ElementButton("§8» §5Die Magie des Fliegens", new ElementButtonImageData("url", "http://45.138.50.23:3000/img/ui/wizard/fly-display.png")), e -> {
                    final CustomForm form1 = new CustomForm.Builder("§7» §8Zahlungsart wählen")
                            .addElement(new ElementStepSlider("§8» §fBrian §8| §7Auf welche Art und Weise möchtest du mir entgegenkommen? Das hier ist kein Betrug...", Arrays.asList(
                                    "\n\n§8» §f" + player.getName() + " §8| §7Wie wäre es, wenn ich dir §9$150 §7gebe für §910 Minuten§7?",
                                    "\n\n§8» §f" + player.getName() + " §8| §924 Kohleblöcke §7finde ich sehr fair für §910 Minuten§7.",
                                    "\n\n§8» §f" + player.getName() + " §8| §9$320 §7für §930 Minuten §7ist doch fair, oder?",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Was hälst du von §913 Diamanten§7 für §930 Minuten§7?",
                                    "\n\n§8» §f" + player.getName() + " §8| §7Ich gebe dir §9$580 §7für §960 Minuten§7!",
                                    "\n\n§8» §f" + player.getName() + " §8| §922 Diamanten §7sind für §960 Minuten §7angemessen."
                            )))
                            .onSubmit((g, h) -> {
                                if (this.brianFly.containsKey(player.getName())) {
                                    this.serverCore.playSound(player, Sound.NOTE_BASS);
                                    player.sendMessage(Language.get("feature.brian.wait", this.convertToDisplayBrian("fly")));
                                    return;
                                }
                                switch (h.getStepSliderResponse(0).getElementID()) {
                                    case 0:
                                        this.openBuyBrianMoney(player, "fly", 10, 150);
                                        break;
                                    case 1:
                                        this.openBuyBrianItem(player, "fly", 10, "173:0:24:not");
                                        break;
                                    case 2:
                                        this.openBuyBrianMoney(player, "fly", 30, 320);
                                        break;
                                    case 3:
                                        this.openBuyBrianItem(player, "fly", 30, "264:0:13:not");
                                        break;
                                    case 4:
                                        this.openBuyBrianMoney(player, "fly", 60, 580);
                                        break;
                                    case 5:
                                        this.openBuyBrianItem(player, "fly", 60, "264:0:22:not");
                                        break;
                                }
                            })
                            .build();
                    form1.send(player);
                })
                .build();
        form.send(player);
    }

    public HashMap<String, Long> brianFly = new HashMap<>();

    public void openBuyBrianMoney(final Player player, final String type, final int minutes, final double price) {
        final ModalForm modalForm = new ModalForm.Builder("§7» §8Kaufbestätigung", "§8» §fBrian §8| §7Also möchtest du nun für §9" + minutes + " Minuten " + this.convertToDisplayBrian(type) + " §r§7für §9$" + Economy.getAPI().getMoneyFormat().format(price) + " §7kaufen?\n\n§7Meine Magie aktiviert sich sofort und ist nur auf dem CityBuild vorhanden!",
                "§8» §aJetzt kaufen", "§8» §cAbbrechen")
                .onYes(e -> {
                    Economy.getAPI().getMoney(player, money -> {
                        if (money >= price) {
                            Economy.getAPI().reduceMoney(player, price);
                            this.brianFly.put(player.getName(), System.currentTimeMillis() + (minutes * 60000L));
                            player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, true);
                            player.getAdventureSettings().update();
                            this.serverCore.playSound(player, Sound.RANDOM_LEVELUP, 1, 3);
                            player.sendMessage(Language.get("feature.brian.bought.money", this.convertToDisplayBrian(type), minutes, Economy.getAPI().getMoneyFormat().format(price)));
                        } else {
                            this.serverCore.playSound(player, Sound.NOTE_BASS);
                            player.sendMessage(Language.get("feature.brian.no.money"));
                        }
                    });
                })
                .onNo(this::openBrian)
                .build();
        modalForm.send(player);
    }

    public void openBuyBrianItem(final Player player, final String type, final int minutes, final String itemData) {
        final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(itemData);
        final ModalForm modalForm = new ModalForm.Builder("§7» §8Kaufbestätigung", "§8» §fBrian §8| §7Also möchtest du nun für §9" + minutes + " Minuten " + this.convertToDisplayBrian(type) + " §r§7für §9" + item.getCount() + "x " + item.getName() + " §7kaufen?\n\n§7Meine Magie aktiviert sich sofort und ist nur auf dem CityBuild vorhanden!",
                "§8» §aJetzt kaufen", "§8» §cAbbrechen")
                .onYes(e -> {
                    if (this.countInventoryItems(player, item) >= item.getCount()) {
                        player.getInventory().removeItem(item);
                        this.brianFly.put(player.getName(), System.currentTimeMillis() + (minutes * 60000L));
                        player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, true);
                        player.getAdventureSettings().update();
                        this.serverCore.playSound(player, Sound.RANDOM_LEVELUP, 1, 3);
                        player.sendMessage(Language.get("feature.brian.bought.item", this.convertToDisplayBrian(type), minutes, item.getName(), item.getCount()));
                    } else {
                        this.serverCore.playSound(player, Sound.NOTE_BASS);
                        player.sendMessage(Language.get("feature.brian.no.item"));
                    }
                })
                .onNo(this::openBrian)
                .build();
        modalForm.send(player);
    }

    public String convertToDisplayBrian(final String type) {
        switch (type) {
            case "fly":
                return "§5Die Magie des Fliegens";
            default:
                return "null";
        }
    }

    private final List<ChainMessage> ainaraTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hi, §a%p§7! Naa, wie geht es dir?", 3),
            new ChainMessage("Danke, dass du mir bei meinen Aufgaben hilfst.", 2),
            new ChainMessage("Für meine Aufgaben erhälst du auch eine anständige Bezahlung!", 3),
            new ChainMessage("Schön, dich zu sehen!", 2)
    ));

    public void openAinaraByNpc(final Player player) {
        this.smallTalk(this.ainaraTalks, RoleplayID.FEATURE_AINARA.id(), player, message -> {
            if (message == null) {
                this.openAinara(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fAinara §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openAinara(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openAinara(final Player player) {
        this.serverCore.getQuestAPI().getRandomQuestByLink("Ainara", player.getName(), quest -> {
            this.openQuestNPC(player, quest, "§8» §fAinara", " §8| §7Erledige meine Aufgabe, um eine oder mehrere tolle Belohnungen zu erhalten! Ich wäre dir sehr dankbar, denn ich habe immer etwas zu tun.");
        });
    }

    private final List<ChainMessage> mikeTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hallo, §a%p§7! Gut, dass du da bist!", 3),
            new ChainMessage("Ich habe immer so viel zu tun...", 2),
            new ChainMessage("Ich zahle sehr gut! Es lohnt sich, meine Aufgaben zu erledigen.", 3),
            new ChainMessage("Zum Glück gibt es Leute, die meine Arbeit machen!", 3)
    ));

    private final Cooldown clickCooldown = new Cooldown(3000);

    public void openMikeByNpc(final Player player) {
        if (!(this.serverCore.getLevelAPI().getLevel(player.getName()).getLevel() >= 6)) {
            if (!this.clickCooldown.hasCooldown(player.getName())) {
                this.serverCore.playSound(player, Sound.NOTE_BASS);
                player.sendMessage("§8» §fMike §8| §7Hallo! Meine Aufgaben kann ich nur erfahrenen Spielern anvertrauen... Komm wieder, wenn du §9Level 6 §7erreicht hast.");
            }
            return;
        }
        this.smallTalk(this.mikeTalks, RoleplayID.FEATURE_MIKE.id(), player, message -> {
            if (message == null) {
                this.openMike(player);
            } else {
                new ChainExecution.Builder()
                        .append(0, () -> {
                            player.sendMessage("§8» §fMike §8| §7" + message.getMessage().replace("%p", player.getName()));
                        })
                        .append(message.getSeconds(), () -> {
                            this.openMike(player);
                            this.openQueue.remove(player.getName());
                        })
                        .build().start();
            }
        });
    }

    public void openMike(final Player player) {
        this.serverCore.getQuestAPI().getRandomQuestByLink("Mike", player.getName(), quest -> {
            this.openQuestNPC(player, quest, "§8» §fMike", " §8| §7Erledige meine Aufgabe, um eine oder mehrere tolle Belohnungen zu erhalten! Ich wäre dir sehr dankbar, denn bei mir ist aktuell viel los...");
        });
    }

    public void openQuestNPC(final Player player, final Quest quest, final String npcPrefix, final String npcText) {
        boolean b = true;
        if (!this.serverCore.getQuestAPI().playerIsInQuest(player.getName(), quest.getNameId())) {
            this.serverCore.getQuestAPI().setQuestOnPlayer(player.getName(), quest.getNameId());
            this.serverCore.getHologramAPI().updateSpecialHolograms(player);
            b = false;
        }
        final QuestPlayer.QuestPlayerData questPlayerData = this.serverCore.getQuestAPI().getQuestPlayerDataFromQuestId(player.getName(), quest.getNameId());
        final int questNeedCount = questPlayerData.getRequired() - questPlayerData.getCurrent();

        if (b && questNeedCount > 0) {
            if (quest.getData().split("#")[0].equals("bring")) {
                final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(quest.getData().split("#")[1]);
                final int count = this.countInventoryItems(player, item);

                if (count > 0) {
                    int giveCount = Math.min(count, questNeedCount);
                    final ModalForm itemForm = new ModalForm.Builder(npcPrefix, npcPrefix + " §8| §7Oh! Damit du deine Aufgabe erledigst, musst du mir noch §9" + questNeedCount + "x " + item.getName()
                            + " §7geben. Möchtest du mir §9" + giveCount + " Stück §7schon geben, um in der Aufgabe weiterzumachen?",
                            "§8» §aItems abgeben", "§8» §cAbbrechen")
                            .onYes(e -> {
                                item.setCount(giveCount);
                                player.getInventory().removeItem(item);
                                player.sendMessage(Language.get("quest.progress.gave.item", item.getName(), giveCount));
                                this.serverCore.playSound(player, Sound.NOTE_PLING);
                                this.serverCore.getQuestAPI().addQuestProgress(player, quest.getNameId(), giveCount);
                            })
                            .onNo(e -> {
                                final String builder = "§8» §1Quest: §7" + quest.getDisplayName() + "§r\n" + "§8» §1Aufgabe: §7" + quest.getDescription() +
                                        "§r\n" + "§8» §1Fortschritt: §f" + questPlayerData.getCurrent() + "/" + quest.getRequired() + "\n\n" +
                                        "§8» §1Quest läuft ab in: §7" + this.serverCore.getRemainingTimeFuture(questPlayerData.getExpire()) + "\n\n";

                                final SimpleForm.Builder form = new SimpleForm.Builder(npcPrefix, npcPrefix + npcText + "\n\n" + builder);
                                form.build().send(player);
                            })
                            .build();
                    itemForm.send(player);
                    return;
                }
            }
        }

        final String builder = "§8» §1Quest: §7" + quest.getDisplayName() + "§r\n" + "§8» §1Aufgabe: §7" + quest.getDescription() +
                "§r\n" + "§8» §1Fortschritt: §f" + questPlayerData.getCurrent() + "/" + quest.getRequired() + "\n\n" +
                "§8» §1Quest läuft ab in: §7" + this.serverCore.getRemainingTimeFuture(questPlayerData.getExpire()) + "\n\n";

        final SimpleForm.Builder form = new SimpleForm.Builder(npcPrefix, npcPrefix + npcText + "\n\n" + builder);
        form.build().send(player);
    }

    private int countInventoryItems(final Player player, final Item item) {
        final AtomicInteger i = new AtomicInteger(0);
        player.getInventory().getContents().forEach((g, h) -> {
            if (h.getId() == item.getId() && h.getDamage() == item.getDamage()) {
                i.addAndGet(h.getCount());
            }
        });
        return i.get();
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
    public static class FeatureListener implements Listener {

        private final FeatureRoleplay featureRoleplay;

        @EventHandler
        public void on(final PlayerInteractEntityEvent event) {
            final Player player = event.getPlayer();
            if (event.getEntity().namedTag.exist("npc_id")) {
                final String npcId = event.getEntity().namedTag.getString("npc_id");
                if (!EventListener.inIntroduction.contains(player.getName())) {
                    if (!this.featureRoleplay.openQueue.contains(player.getName())) {
                        if (npcId.equals(RoleplayID.FEATURE_LOLA.id())) this.featureRoleplay.openRewardByNpc(player);
                        else if (npcId.equals(RoleplayID.FEATURE_AINARA.id())) this.featureRoleplay.openAinaraByNpc(player);
                        else if (npcId.equals(RoleplayID.FEATURE_JOHN.id())) this.featureRoleplay.openJohnByNpc(player);
                        else if (npcId.equals(RoleplayID.FEATURE_BRIAN.id())) this.featureRoleplay.openBrianByNpc(player);
                        else if (npcId.equals(RoleplayID.FEATURE_MIKE.id())) this.featureRoleplay.openMikeByNpc(player);
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
                        if (!this.featureRoleplay.openQueue.contains(player.getName())) {
                            if (npcId.equals(RoleplayID.FEATURE_LOLA.id())) this.featureRoleplay.openRewardByNpc(player);
                            else if (npcId.equals(RoleplayID.FEATURE_AINARA.id())) this.featureRoleplay.openAinaraByNpc(player);
                            else if (npcId.equals(RoleplayID.FEATURE_JOHN.id())) this.featureRoleplay.openJohnByNpc(player);
                            else if (npcId.equals(RoleplayID.FEATURE_BRIAN.id())) this.featureRoleplay.openBrianByNpc(player);
                            else if (npcId.equals(RoleplayID.FEATURE_MIKE.id())) this.featureRoleplay.openMikeByNpc(player);
                        }
                    }
                }
            }
        }

        @EventHandler
        public void on(final PlayerInteractEvent event) {
            final Player player = event.getPlayer();
            final Block block = event.getBlock();
            if (block.getX() == 59 && block.getY() == 68 && block.getZ() == 77 && block.getLevel().getName().equals("plots") && block.getId() == BlockID.ENDER_CHEST) {
                this.featureRoleplay.openCrate(player);
                event.setCancelled(true);
            }
        }

    }

}
