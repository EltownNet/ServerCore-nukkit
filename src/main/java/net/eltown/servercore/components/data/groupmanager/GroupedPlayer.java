package net.eltown.servercore.components.data.groupmanager;

import lombok.Data;

@Data
public class GroupedPlayer {

    private final String player;
    private String group;
    private long duration;

}
