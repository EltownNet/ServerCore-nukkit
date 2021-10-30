package net.eltown.servercore.components.roleplay.other;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
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

    public FeatureRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        serverCore.getServer().getPluginManager().registerEvents(new FeatureRoleplay.FeatureListener(this), serverCore);
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

                    if (((calendarReward.get(Calendar.DAY_OF_YEAR) + 1) == calendarNow.get(Calendar.DAY_OF_YEAR)) && (calendarReward.get(Calendar.YEAR) == calendarNow.get(Calendar.YEAR)) && !(rewardPlayer.getDay() > 14)) {
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
                    } else if (rewardPlayer.getDay() > 14) {
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

    private final List<ChainMessage> ainaraTalks = new ArrayList<>(Arrays.asList(
            new ChainMessage("Hi, §a%p§7! Naa, wie geht es dir?", 3),
            new ChainMessage("Danke, dass du mir bei meinen Aufgaben hilfst.", 2),
            new ChainMessage("Für meine Aufgaben erhälst du auch eine anständige Bezahlung!", 3),
            new ChainMessage("Schön, dich zu sehen!", 2)
    ));

    public void openAinaraByNpc(final Player player) {
        this.smallTalk(this.ainaraTalks, RoleplayID.FEATURE_LOLA.id(), player, message -> {
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

    public void openQuestNPC(final Player player, final Quest quest, final String npcPrefix, final String npcText) {
        boolean b = true;
        if (!this.serverCore.getQuestAPI().playerIsInQuest(player.getName(), quest.getNameId())) {
            this.serverCore.getQuestAPI().setQuestOnPlayer(player.getName(), quest.getNameId());
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
                        }
                    }
                }
            }
        }

    }

}
