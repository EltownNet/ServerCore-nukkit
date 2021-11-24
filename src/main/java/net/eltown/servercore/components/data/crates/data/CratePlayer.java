package net.eltown.servercore.components.data.crates.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@AllArgsConstructor
@Data
public class CratePlayer {

    private final String player;
    private Map<String, Integer> data;

}
