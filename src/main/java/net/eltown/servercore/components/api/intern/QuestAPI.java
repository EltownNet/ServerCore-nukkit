package net.eltown.servercore.components.api.intern;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import lombok.RequiredArgsConstructor;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.giftkeys.GiftkeyCalls;
import net.eltown.servercore.components.data.quests.FullQuestPlayer;
import net.eltown.servercore.components.data.quests.Quest;
import net.eltown.servercore.components.data.quests.QuestCalls;
import net.eltown.servercore.components.data.quests.QuestPlayer;
import net.eltown.servercore.components.event.QuestCompleteEvent;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class QuestAPI {

    private final ServerCore instance;

    public final HashMap<String, QuestPlayer> cachedQuestPlayer = new HashMap<>();
    private final HashMap<String, Quest> cachedQuests = new HashMap<>();

    public void getQuest(final String questNameId, final Consumer<Quest> quest) {
        if (!this.cachedQuests.containsKey(questNameId)) {
            this.instance.getTinyRabbit().sendAndReceive(delivery -> {
                final String[] d = delivery.getData();
                switch (QuestCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case CALLBACK_QUEST_DATA:
                        this.cachedQuests.put(d[1], new Quest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]));
                        quest.accept(new Quest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]));
                        break;
                    case CALLBACK_NULL:
                        quest.accept(null);
                        break;
                }
            }, Queue.QUESTS_CALLBACK, QuestCalls.REQUEST_QUEST_DATA.name(), questNameId);
        } else quest.accept(this.cachedQuests.get(questNameId));
    }

    public void getRandomQuestByLink(final String link, final String player, final Consumer<Quest> quest) {
        this.checkIfQuestIsExpired(player);
        this.instance.getTinyRabbit().sendAndReceive(delivery -> {
            final String[] d = delivery.getData();
            switch (QuestCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_RANDOM_QUEST_DATA_BY_LINK:
                    if (!this.cachedQuests.containsKey(d[1])) this.cachedQuests.put(d[1], new Quest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]));

                    final AtomicReference<Quest> questReference = new AtomicReference<>(null);
                    this.cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
                        this.getQuest(e.getQuestNameId(), f -> {
                            if (f.getLink().equals(link)) {
                                questReference.set(f);
                            }
                        });
                    });

                    if (questReference.get() == null) {
                        questReference.set(new Quest(d[1], d[2], d[3], d[4], Integer.parseInt(d[5]), Long.parseLong(d[6]), d[7], d[8]));
                    }

                    quest.accept(questReference.get());
                    break;
                case CALLBACK_NULL:
                    quest.accept(null);
                    break;
            }
        }, Queue.QUESTS_CALLBACK, QuestCalls.REQUEST_RANDOM_QUEST_DATA_BY_LINK.name(), link);
    }

    public void updateQuest(final String questNameId, final String displayName, final String description, final String data, final int required, final long expire, final String rewardData, final String link) {
        this.cachedQuests.remove(questNameId);
        this.cachedQuests.put(questNameId, new Quest(questNameId, displayName, description, data, required, expire, rewardData, link));

        this.instance.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_QUEST.name(), questNameId, displayName, description, data, String.valueOf(required), String.valueOf(expire), rewardData, link);
    }

    public void createQuest(final String questNameId, final String displayName, final String description, final String data, final int required, final long expire, final String rewardData, final String link) {
        this.cachedQuests.remove(questNameId);
        this.cachedQuests.put(questNameId, new Quest(questNameId, displayName, description, data, required, expire, rewardData, link));

        this.instance.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_CREATE_QUEST.name(), questNameId, displayName, description, data, String.valueOf(required), String.valueOf(expire), rewardData, link);
    }

    public boolean playerIsInQuest(final String player, final String questNameId) {
        this.checkIfQuestIsExpired(player);
        final QuestPlayer.QuestPlayerData questPlayerData = this.getQuestPlayerDataFromQuestId(player, questNameId);
        if (questPlayerData == null) return false;
        return questPlayerData.getQuestNameId().equals(questNameId);
    }

    public Set<FullQuestPlayer> getActivePlayerQuests(final String player) {
        final Set<FullQuestPlayer> quests = new HashSet<>();

        this.checkIfQuestIsExpired(player);
        for (QuestPlayer.QuestPlayerData questPlayerData : this.cachedQuestPlayer.get(player).getQuestPlayerData()) {
            this.getQuest(questPlayerData.getQuestNameId(), quest -> {
                quests.add(new FullQuestPlayer(quest, questPlayerData));
            });
        }

        return quests;
    }

    public void setQuestOnPlayer(final String player, final String questNameId) {
        this.instance.getQuestAPI().cachedQuestPlayer.get(player).getQuestPlayerData().forEach(questPlayerData -> {
            this.instance.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_PLAYER_DATA.name(), player, questPlayerData.getQuestNameId(), String.valueOf(questPlayerData.getCurrent()));
        });

        this.getQuest(questNameId, quest -> {
            final List<QuestPlayer.QuestPlayerData> playerData = this.cachedQuestPlayer.get(player).getQuestPlayerData();
            playerData.add(new QuestPlayer.QuestPlayerData(quest.getNameId(), (System.currentTimeMillis() + quest.getExpire()), quest.getRequired(), 0));
            this.cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

            this.instance.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_SET_PLAYER_QUEST.name(), player, questNameId);
        });
    }

    public void removeQuestFromPlayer(final String player, final String questNameId) {
        final QuestPlayer.QuestPlayerData questPlayerData = this.getQuestPlayerDataFromQuestId(player, questNameId);
        final List<QuestPlayer.QuestPlayerData> playerData = this.cachedQuestPlayer.get(player).getQuestPlayerData();
        playerData.remove(questPlayerData);
        this.cachedQuestPlayer.get(player).setQuestPlayerData(playerData);

        this.instance.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_REMOVE_PLAYER_QUEST.name(), player, questNameId);
    }

    public void addQuestProgress(final Player player, final String questNameId, final int progress) {
        this.cachedQuestPlayer.get(player.getName()).getQuestPlayerData().forEach(e -> {
            if (e.getQuestNameId().equals(questNameId)) {
                if (!(e.getCurrent() >= e.getRequired())) {
                    e.setCurrent(e.getCurrent() + progress);
                    this.checkForQuestEnding(player, questNameId, e);
                }
            }
        });
    }

    public void checkForQuestEnding(final Player player, final String questNameId, final QuestPlayer.QuestPlayerData questPlayerData) {
        this.getQuest(questNameId, quest -> {
            if (quest.getRequired() <= questPlayerData.getCurrent()) {
                player.sendMessage(" ");
                player.sendMessage(Language.get("quest.completed", quest.getDisplayName()));
                this.instance.playSound(player, Sound.RANDOM_LEVELUP);
                final List<String> rewards = new ArrayList<>(Arrays.asList(quest.getRewardData().split("-#-")));
                rewards.forEach(e -> {
                    final String[] reward = e.split("#");

                    switch (reward[0]) {
                        case "xp":
                            this.instance.getLevelAPI().addExperience(player, Double.parseDouble(reward[1]));
                            player.sendMessage(Language.get("quest.reward.xp", reward[1]));
                            break;
                        case "money":
                            Economy.getAPI().addMoney(player, Double.parseDouble(reward[1]));
                            player.sendMessage(Language.get("quest.reward.money", reward[1]));
                            break;
                        case "item":
                            final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(reward[1]);
                            player.getInventory().addItem(item);
                            player.sendMessage(Language.get("quest.reward.item", item.getName(), item.getCount()));
                            break;
                        case "gutschein":
                            this.instance.getTinyRabbit().sendAndReceive(delivery -> {
                                switch (GiftkeyCalls.valueOf(delivery.getKey().toUpperCase())) {
                                    case CALLBACK_NULL:
                                        player.sendMessage(Language.get("quest.reward.giftkey", delivery.getData()[1]));
                                        break;
                                }
                            }, Queue.GIFTKEYS_CALLBACK, GiftkeyCalls.REQUEST_CREATE_KEY.name(), String.valueOf(1), reward[1], player.getName());
                            break;
                        case "permission":
                            this.instance.getGroupAPI().addPlayerPermission(player.getName(), reward[1]);
                            player.sendMessage(Language.get("quest.reward.permission", reward[2]));
                            break;
                        case "crate":
                            this.instance.getCrateAPI().addCrate(player.getName(), reward[1], Integer.parseInt(reward[2]));
                            player.sendMessage(Language.get("quest.reward.crate", this.instance.getFeatureRoleplay().convertToDisplay(reward[1]), Integer.parseInt(reward[2])));
                            break;
                    }
                });
                player.sendMessage(" ");

                this.instance.getServer().getPluginManager().callEvent(new QuestCompleteEvent(player, quest, questPlayerData));
                this.instance.getTinyRabbit().send(Queue.QUESTS_RECEIVE, QuestCalls.REQUEST_UPDATE_PLAYER_DATA.name(), player.getName(), questPlayerData.getQuestNameId(), String.valueOf(questPlayerData.getCurrent()));
            }
        });
    }

    public QuestPlayer.QuestPlayerData getQuestPlayerDataFromQuestId(final String player, final String questNameId) {
        final AtomicReference<QuestPlayer.QuestPlayerData> questPlayerData = new AtomicReference<>();

        if (this.cachedQuestPlayer.get(player) == null || this.cachedQuestPlayer.get(player).getQuestPlayerData().isEmpty()) return null;

        this.cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
            if (e.getQuestNameId().equals(questNameId)) questPlayerData.set(e);
        });

        return questPlayerData.get();
    }

    public void checkIfQuestIsExpired(final String player) {
        final List<String> list = new ArrayList<>();
        if (!this.cachedQuestPlayer.get(player).getQuestPlayerData().isEmpty()) {
            this.cachedQuestPlayer.get(player).getQuestPlayerData().forEach(e -> {
                if (e.getExpire() < System.currentTimeMillis()) list.add(e.getQuestNameId());
            });
        }

        if (!list.isEmpty()) {
            list.forEach(e -> this.removeQuestFromPlayer(player, e));
        }
    }

}
