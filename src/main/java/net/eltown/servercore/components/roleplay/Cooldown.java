package net.eltown.servercore.components.roleplay;

import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
public class Cooldown {

    private long ms;
    private final Map<String, Long> cooldown = new HashMap<>();

    public boolean hasCooldown(final String key) {
        if (this.cooldown.containsKey(key)) {
            final long next = this.cooldown.get(key);

            if (next > System.currentTimeMillis()) {
                return true;
            } else {
                this.cooldown.put(key, System.currentTimeMillis() + this.ms);
                return false;
            }
        } else {
            this.cooldown.put(key, System.currentTimeMillis() + this.ms);
            return false;
        }
    }

    public boolean containsCooldown(final String key) {
        if (this.cooldown.containsKey(key)) {
            final long next = this.cooldown.get(key);

            return next > System.currentTimeMillis();
        } else return false;
    }

    public void removeCooldown(final String key) {
        this.cooldown.remove(key);
    }

    public void addCooldown(final String key, final long ms) {
        this.cooldown.put(key, System.currentTimeMillis() + ms);
    }

    public void clear() {
        this.cooldown.clear();
    }

    public void setMs(final long ms) {
        this.ms = ms;
    }

}