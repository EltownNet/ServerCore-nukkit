package net.eltown.servercore.components.api.intern;

import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.settings.AccountSettings;
import net.eltown.servercore.components.data.settings.SettingsCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.*;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class SettingsAPI {

    private final ServerCore instance;

    public final HashMap<String, AccountSettings> cachedSettings = new HashMap<>();
    public final List<String> hasChanges = new ArrayList<>();

    public void updateSettingsDirect(final String player, final String key, final String value) {
        final Map<String, String> map = this.cachedSettings.get(player).getSettings();
        map.remove(key);
        map.put(key, value);
        this.cachedSettings.get(player).setSettings(map);

        this.instance.getTinyRabbit().send(Queue.SETTINGS_RECEIVE, SettingsCalls.REQUEST_UPDATE_SETTINGS.name(), player, key, value);
    }

    public void updateSettings(final String player, final String key, final String value) {
        final Map<String, String> map = this.cachedSettings.get(player).getSettings();
        map.remove(key);
        map.put(key, value);
        this.cachedSettings.get(player).setSettings(map);
        if (!this.hasChanges.contains(player)) this.hasChanges.add(player);
    }

    public void updateAll(final String player) {
        final StringBuilder stringBuilder = new StringBuilder();
        this.cachedSettings.get(player).getSettings().forEach((k, v) -> {
            stringBuilder.append(k).append(":").append(v).append(">:<");
        });
        final String settings = stringBuilder.substring(0, stringBuilder.length() - 3);

        this.instance.getTinyRabbit().send(Queue.SETTINGS_RECEIVE, SettingsCalls.REQUEST_UPDATE_ALL.name(), player, settings);
    }

    public void removeEntry(final String player, final String key) {
        this.cachedSettings.get(player).getSettings().remove(key);

        this.instance.getTinyRabbit().send(Queue.SETTINGS_RECEIVE, SettingsCalls.REQUEST_REMOVE_SETTINGS.name(), player, key);
    }

    public void getEntry(final String player, final String key, final String def, final Consumer<String> value) {
        if (!this.cachedSettings.containsKey(player)) {
            this.instance.getTinyRabbit().sendAndReceive(delivery -> {
                switch (SettingsCalls.valueOf(delivery.getKey().toUpperCase())) {
                    case CALLBACK_SETTINGS:
                        if (!delivery.getData()[1].equals("null")) {
                            final Map<String, String> map = new HashMap<>();
                            final List<String> list = Arrays.asList(delivery.getData()[1].split(">:<"));
                            list.forEach(e -> {
                                map.put(e.split(":")[0], e.split(":")[1]);
                            });
                            this.instance.getSettingsAPI().cachedSettings.put(player, new AccountSettings(player, map));
                            value.accept(map.getOrDefault(key, def));
                            return;
                        } else this.instance.getSettingsAPI().cachedSettings.put(player, new AccountSettings(player, new HashMap<>()));
                        break;
                }
            }, Queue.SETTINGS_CALLBACK, SettingsCalls.REQUEST_SETTINGS.name(), player);
        }
        value.accept(this.cachedSettings.get(player).getSettings().getOrDefault(key, def));
    }

}
