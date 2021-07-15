package net.eltown.servercore.components.roleplay.jobs;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;

public class JobRoleplay {

    public JobRoleplay(final ServerCore serverCore) {
        serverCore.getServer().getPluginManager().registerEvents(new JobListener(this), serverCore);
    }


    @RequiredArgsConstructor
    public static class JobListener implements Listener {

        private final JobRoleplay jobRoleplay;

        @EventHandler
        public void on(final PlayerInteractEvent event) {

        }

    }

}
