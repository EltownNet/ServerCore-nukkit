package net.eltown.servercore.components.data.groupmanager;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class GroupedPlayer {

    private final String player;
    private String group;
    private long duration;
    private List<String> permissions;

}
