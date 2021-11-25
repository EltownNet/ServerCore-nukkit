package net.eltown.servercore.components.tasks;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityFirework;
import cn.nukkit.item.Item;
import cn.nukkit.level.Location;
import cn.nukkit.level.Sound;
import cn.nukkit.level.particle.FloatingTextParticle;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.EntityEventPacket;
import cn.nukkit.network.protocol.LevelSoundEventPacket;
import cn.nukkit.scheduler.Task;
import net.eltown.economy.Economy;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.CoreCalls;
import net.eltown.servercore.components.data.crates.Raffle;
import net.eltown.servercore.components.data.crates.data.CrateReward;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.concurrent.ThreadLocalRandom;

public class RaffleTask extends Task {

    private final ServerCore serverCore;
    private final Raffle raffle;
    private final Player player;
    private final int spins;
    private final FloatingTextParticle hologram;
    private int done = 0;

    public RaffleTask(final ServerCore serverCore, final Raffle raffle, final Player player) {
        this.serverCore = serverCore;
        this.raffle = raffle;
        this.player = player;
        this.spins = ThreadLocalRandom.current().nextInt(100) + 50;
        this.hologram = this.serverCore.getFeatureRoleplay().crateHologram;
    }

    @Override
    public void onRun(int i) {
        final int add = Math.max(this.done - (this.spins - 20), 1);
        final String display = this.raffle.getNextRaffleDisplay();

        if ((this.spins - 1) <= this.done) {
            final CrateReward reward = this.raffle.getFinalReward();

            this.hologram.setTitle(reward.getDisplayName());
            this.player.getLevel().addSound(this.hologram.add(0), Sound.RANDOM_CLICK);

            Server.getInstance().getScheduler().scheduleDelayedTask(() -> {
                this.hologram.setTitle("§2§l> §r" + reward.getDisplayName() + " §r§2§l<");
                this.player.getLevel().addSound(this.hologram.add(0), Sound.NOTE_PLING, 1, 3);
                Server.getInstance().getScheduler().scheduleDelayedTask(() -> {
                    final CompoundTag nbt = Entity.getDefaultNBT(this.hologram.add(0));
                    nbt.putCompound("FireworkItem", NBTIO.putItemHelper(SyncAPI.ItemAPI.pureItemFromStringWithCount("401:0:1:CgAACgkARmlyZXdvcmtzAQYARmxpZ2h0AQkKAEV4cGxvc2lvbnMKAQAAAAcMAEZpcmV3b3JrRmFkZQAAAAAHDQBGaXJld29ya0NvbG9yAQAAAAUBDABGaXJld29ya1R5cGUAAQ0ARmlyZXdvcmtUcmFpbAABDwBGaXJld29ya0ZsaWNrZXIAAAAA")));

                    final EntityFirework firework = new EntityFirework(this.player.getChunk(), nbt);
                    firework.spawnToAll();

                    final EntityEventPacket pk = new EntityEventPacket();
                    pk.data = 0;
                    pk.event = EntityEventPacket.FIREWORK_EXPLOSION;
                    pk.eid = firework.getId();

                    firework.kill();

                    this.player.getLevel().addLevelSoundEvent(this.hologram.add(0), LevelSoundEventPacket.SOUND_LARGE_BLAST, -1, 72);
                    Server.broadcastPacket(this.serverCore.getServer().getOnlinePlayers().values(), pk);
                }, 20, true);

                if (reward.getChance() <= 20) {
                    this.serverCore.getTinyRabbit().send(Queue.CORE_RECEIVE, CoreCalls.REQUEST_BROADCAST_PROXY_MESSAGE.name(), Language.get("crate.reward.broadcast", this.player.getName(), reward.getDisplayName(), this.serverCore.getFeatureRoleplay().convertToDisplay(reward.getCrate())));
                    this.player.getLevel().addSound(this.hologram.add(0), Sound.RANDOM_LEVELUP, 1, 3);
                }

                final String[] rewardData = reward.getData().split(";");
                switch (rewardData[0]) {
                    case "item":
                        final Item item = SyncAPI.ItemAPI.pureItemFromStringWithCount(rewardData[1]);
                        this.player.getInventory().addItem(item);
                        this.player.sendMessage(Language.get("crate.reward.item", item.getName(), item.getCount()));
                        break;
                    case "money":
                        Economy.getAPI().addMoney(this.player, Double.parseDouble(rewardData[1]));
                        this.player.sendMessage(Language.get("crate.reward.money", Double.parseDouble(rewardData[1])));
                        break;
                    case "xp":
                        this.serverCore.getLevelAPI().addExperience(this.player, Double.parseDouble(rewardData[1]));
                        this.player.sendMessage(Language.get("crate.reward.xp", Double.parseDouble(rewardData[1])));
                        break;
                    case "crate":
                        this.serverCore.getCrateAPI().addCrate(this.player.getName(), rewardData[1], Integer.parseInt(rewardData[2]));
                        this.player.sendMessage(Language.get("crate.reward.crate", this.serverCore.getFeatureRoleplay().convertToDisplay(rewardData[1]), rewardData[2]));
                        break;
                }

                Server.getInstance().getScheduler().scheduleDelayedTask(() -> {
                    this.hologram.setTitle("§5§lGlückstruhe");
                    this.serverCore.getFeatureRoleplay().crateInUse = false;
                }, 80, true);
            }, (add * 25) / 20, true);
            return;
        }

        this.hologram.setTitle(display);
        this.player.getLevel().addSound(this.hologram.add(0), Sound.RANDOM_CLICK);

        this.done++;
        Server.getInstance().getScheduler().scheduleDelayedTask(this, (add * 25) / 20, true);

    }


}
