package net.eltown.servercore.components.data.sync;

import cn.nukkit.potion.Effect;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
@Getter
public class SyncPlayer {

    private final String invString, ecString;
    private final float health;
    private final int food, exp, level, gamemode;
    private final Set<Effect> effects;

}