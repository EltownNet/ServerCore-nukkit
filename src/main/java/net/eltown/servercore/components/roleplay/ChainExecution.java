package net.eltown.servercore.components.roleplay;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

public class ChainExecution {

    private final LinkedList<Chain> chain;

    private ChainExecution(final LinkedList<Chain> chain) {
        this.chain = chain;
    }

    public void start() {
        final Timer timer = new Timer();

        int previousTime = 0;

        for (final Chain chain : chain) {
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    chain.getTask().run();
                }
            }, (previousTime + chain.getSeconds()) * 1000L);

            previousTime += chain.getSeconds();
        }
    }

    public static class Builder {

        private final LinkedList<Chain> preChain;

        public Builder() {
            this.preChain = new LinkedList<>();
        }

        public Builder append(final int seconds, final Runnable task) {
            this.preChain.add(new Chain(seconds, task));
            return this;
        }

        public ChainExecution build() {
            return new ChainExecution(preChain);
        }

    }

    @RequiredArgsConstructor
    @Getter
    private static class Chain {

        private final int seconds;
        private final Runnable task;

    }

}