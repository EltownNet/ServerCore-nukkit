package net.eltown.servercore.components.tasks;

import cn.nukkit.AdventureSettings;
import cn.nukkit.Player;
import cn.nukkit.level.Sound;
import cn.nukkit.potion.Effect;
import cn.nukkit.scheduler.Task;
import lombok.AllArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

import java.util.concurrent.atomic.AtomicReference;

@AllArgsConstructor
public class BrianTask extends Task {

    private final ServerCore serverCore;

    @Override
    public void onRun(int i) {
        final AtomicReference<String> remove = new AtomicReference<>("");
        this.serverCore.getFeatureRoleplay().brianFly.forEach((g, h) -> {
            if (h < System.currentTimeMillis()) {
                remove.set(g);
                final Player player = this.serverCore.getServer().getPlayer(g);
                if (player != null) {
                    player.getAdventureSettings().set(AdventureSettings.Type.ALLOW_FLIGHT, false);
                    player.getAdventureSettings().set(AdventureSettings.Type.FLYING, false);
                    player.getAdventureSettings().update();

                    player.addEffect(Effect.getEffect(Effect.SLOW_FALLING).setAmplifier(1).setDuration(300).setVisible(false));
                    this.serverCore.playSound(player, Sound.RANDOM_BOW);
                    player.sendMessage(Language.get("feature.brian.fly.expired"));
                }
            }
        });
        if (!remove.get().isEmpty()) this.serverCore.getFeatureRoleplay().brianFly.remove(remove.get());
    }
}
