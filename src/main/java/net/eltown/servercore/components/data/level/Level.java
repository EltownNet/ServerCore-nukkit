package net.eltown.servercore.components.data.level;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class Level {

    private final String player;
    private int level;
    private double experience;

}
