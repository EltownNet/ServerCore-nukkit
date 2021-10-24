package net.eltown.servercore.components.roleplay.jobs;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.roleplay.ChainExecution;
import net.eltown.servercore.components.roleplay.RoleplayID;

import java.util.ArrayList;
import java.util.List;

public class CryptoRoleplay {

    private final ServerCore serverCore;

    public CryptoRoleplay(final ServerCore serverCore) {
        this.serverCore = serverCore;
        serverCore.getServer().getPluginManager().registerEvents(new CryptoRoleplay.CryptoListener(this), serverCore);
    }

    private final List<String> openQueue = new ArrayList<>();

    public void openBeto(final Player player) {
        new ChainExecution.Builder()
                .append(0, () -> {
                    this.openQueue.add(player.getName());
                    player.sendMessage("§8» §fBeto §8| §7Ich bin auf Eltown für die Kryptowährung zuständig. Ich habe aktuell viel zu tun, also besuche mich bald wieder, damit ich dich anständig beraten kann. Du kannst aber schon mal §e/wallet §7ausprobieren!");
                })
                .append(5, () -> {
                    this.openQueue.remove(player.getName());
                })
                .build().start();
    }

    @RequiredArgsConstructor
    public static class CryptoListener implements Listener {

        private final CryptoRoleplay cryptoRoleplay;

        @EventHandler
        public void on(final PlayerInteractEntityEvent event) {
            final Player player = event.getPlayer();
            if (event.getEntity().namedTag.exist("npc_id")) {
                final String npcId = event.getEntity().namedTag.getString("npc_id");
                if (!this.cryptoRoleplay.openQueue.contains(player.getName())) {
                    if (npcId.equals(RoleplayID.CRYPTO.id())) this.cryptoRoleplay.openBeto(player);
                }
            }
        }

        @EventHandler
        public void on(final EntityDamageByEntityEvent event) {
            if (event.getDamager() instanceof Player) {
                final Player player = (Player) event.getDamager();
                if (event.getEntity().namedTag.exist("npc_id")) {
                    final String npcId = event.getEntity().namedTag.getString("npc_id");
                    if (!this.cryptoRoleplay.openQueue.contains(player.getName())) {
                        if (npcId.equals(RoleplayID.CRYPTO.id())) this.cryptoRoleplay.openBeto(player);
                    }
                }
            }
        }
    }

}
