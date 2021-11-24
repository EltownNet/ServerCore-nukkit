package net.eltown.servercore.components.data.crates.data;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class CrateReward {

    private final String id;
    private String crate;
    private String displayName;
    private int chance;
    private String data;

}
