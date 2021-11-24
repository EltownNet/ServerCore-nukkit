package net.eltown.servercore.components.api.intern;

import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.crates.CratesCalls;
import net.eltown.servercore.components.data.crates.data.CrateReward;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class CrateAPI {

    private final ServerCore instance;

    public void addCrate(final String player, final String crate, final int i) {
        this.instance.getTinyRabbit().send(Queue.CRATES_RECEIVE, CratesCalls.REQUEST_ADD_CRATE.name(), player, crate, String.valueOf(i));
    }

    public void removeCrate(final String player, final String crate, final int i) {
        this.instance.getTinyRabbit().send(Queue.CRATES_RECEIVE, CratesCalls.REQUEST_REMOVE_CRATE.name(), player, crate, String.valueOf(i));
    }

    public void insertCrateReward(final String id, final String crate, final String displayName, final int chance, final String data) {
        this.instance.getTinyRabbit().send(Queue.CRATES_RECEIVE, CratesCalls.REQUEST_INSERT_REWARD_DATA.name(), id, crate, displayName, String.valueOf(chance), data);
    }

    public void updateCrateReward(final String id, final String crate, final String displayName, final int chance, final String data) {
        this.instance.getTinyRabbit().send(Queue.CRATES_RECEIVE, CratesCalls.REQUEST_UPDATE_REWARD.name(), id, crate, displayName, String.valueOf(chance), data);
    }

    public void deleteCrateReward(final String id) {
        this.instance.getTinyRabbit().send(Queue.CRATES_RECEIVE, CratesCalls.REQUEST_DELETE_REWARD.name(), id);
    }

    public void getCrateReward(final String id, final Consumer<CrateReward> consumer) {
        this.instance.getTinyRabbit().sendAndReceive(delivery -> {
            final String[] d = delivery.getData();
            switch (CratesCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NULL:
                    consumer.accept(null);
                    break;
                case CALLBACK_GET_REWARD_DATA:
                    consumer.accept(new CrateReward(d[1], d[2], d[3], Integer.parseInt(d[4]), d[5]));
                    break;
            }
        }, Queue.CRATES_CALLBACK, CratesCalls.REQUEST_GET_REWARD_DATA.name(), id);
    }

    public void getCrateRewards(final String crate, final Consumer<Set<CrateReward>> consumer) {
        this.instance.getTinyRabbit().sendAndReceive(delivery -> {
            final String[] d = delivery.getData();
            switch (CratesCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_GET_CRATE_REWARDS:
                    final Set<CrateReward> rewards = new HashSet<>();
                    if (!d[1].equals("null")) {
                        final String[] rawData = d[1].split(">#<");
                        for (final String s : rawData) {
                            final String[] c = s.split(">:<");
                            rewards.add(new CrateReward(c[0], c[1], c[2], Integer.parseInt(c[3]), c[4]));
                        }
                    }
                    consumer.accept(rewards);
                    break;
            }
        }, Queue.CRATES_CALLBACK, CratesCalls.REQUEST_GET_CRATE_REWARDS.name(), crate);
    }

    public void getPlayerData(final String player, final Consumer<Map<String, Integer>> consumer) {
        this.instance.getTinyRabbit().sendAndReceive(delivery -> {
            final String[] d = delivery.getData();
            switch (CratesCalls.valueOf(delivery.getKey())) {
                case CALLBACK_PLAYER_DATA:
                    final Map<String, Integer> map = new HashMap<>();
                    if (!d[1].equals("null")) {
                        final String[] rawData = d[1].split("#");
                        for (final String s : rawData) {
                            final String[] c = s.split(":");
                            map.put(c[0], Integer.parseInt(c[1]));
                        }
                    }
                    consumer.accept(map);
                    break;
            }
        }, Queue.CRATES_CALLBACK, CratesCalls.REQUEST_PLAYER_DATA.name(), player);
    }

}
