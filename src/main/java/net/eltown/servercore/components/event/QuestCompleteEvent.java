package net.eltown.servercore.components.event;

import cn.nukkit.Player;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import lombok.Getter;
import net.eltown.servercore.components.data.quests.Quest;
import net.eltown.servercore.components.data.quests.QuestPlayer;

@Getter
public class QuestCompleteEvent extends PlayerEvent {

    private final Quest quest;
    private final QuestPlayer.QuestPlayerData questPlayerData;
    private static final HandlerList handlers = new HandlerList();

    public QuestCompleteEvent(final Player player, final Quest quest, final QuestPlayer.QuestPlayerData questPlayerData) {
        this.player = player;
        this.quest = quest;
        this.questPlayerData = questPlayerData;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

}
