package net.eltown.servercore.components.data.rewards;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class RewardPlayer {

    private final String player;
    private final int day;
    private final long lastReward;
    private final long onlineTime;

}
