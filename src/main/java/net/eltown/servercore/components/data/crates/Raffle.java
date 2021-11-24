package net.eltown.servercore.components.data.crates;

import net.eltown.servercore.components.data.crates.data.CrateReward;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class Raffle {

    private final List<CrateReward> possibleRewards;
    private List<CrateReward> raffleRewards;
    private CrateReward reward;

    private CrateReward lastDisplay;

    public Raffle(final List<CrateReward> possibleRewards) {
        this.possibleRewards = possibleRewards;
        this.pickReward();
    }

    private void pickReward() {
        final ArrayList<CrateReward> rewards = new ArrayList<>();

        this.possibleRewards.forEach(r -> {
            if (r.getChance() > ThreadLocalRandom.current().nextInt(101)) rewards.add(r);
        });
        reward = rewards.get(ThreadLocalRandom.current().nextInt(rewards.size()));
        this.raffleRewards = rewards;
    }

    public CrateReward getFinalReward() {
        return this.reward;
    }

    public String getNextRaffleDisplay() {
        CrateReward display = this.possibleRewards.get(ThreadLocalRandom.current().nextInt(this.possibleRewards.size()));
        while (display == lastDisplay) {
            display = this.possibleRewards.get(ThreadLocalRandom.current().nextInt(this.possibleRewards.size()));
        }
        this.lastDisplay = display;
        return display.getDisplayName();
    }

}
