package net.eltown.servercore.components.data.quests;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class FullQuestPlayer {

    private Quest quest;
    private QuestPlayer.QuestPlayerData questPlayerData;

}
