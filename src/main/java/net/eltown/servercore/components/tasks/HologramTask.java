package net.eltown.servercore.components.tasks;

import cn.nukkit.scheduler.Task;
import lombok.AllArgsConstructor;
import net.eltown.servercore.ServerCore;

@AllArgsConstructor
public class HologramTask extends Task {

    private final ServerCore serverCore;

    @Override
    public void onRun(int i) {
        this.serverCore.getServer().getOnlinePlayers().values().forEach(player -> this.serverCore.getHologramAPI().updateSpecialHolograms(player));
    }
}
