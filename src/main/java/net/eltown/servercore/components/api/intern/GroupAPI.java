package net.eltown.servercore.components.api.intern;

import lombok.RequiredArgsConstructor;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.groupmanager.GroupCalls;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class GroupAPI {

    private final ServerCore instance;

    public void addGroupPermission(final String group, final String key) {
        this.instance.getTinyRabbit().sendAndReceive((delivery1 -> {

        }), Queue.GROUPS, GroupCalls.REQUEST_ADD_PERMISSION.name(), group, key);
    }

    public void removeGroupPermission(final String group, final String key) {
        this.instance.getTinyRabbit().sendAndReceive((delivery1 -> {

        }), Queue.GROUPS, GroupCalls.REQUEST_REMOVE_PERMISSION.name(), group, key);
    }

    public void addPlayerPermission(final String player, final String key) {
        this.instance.getTinyRabbit().sendAndReceive((delivery1 -> {

        }), Queue.GROUPS, GroupCalls.REQUEST_ADD_PLAYER_PERMISSION.name(), player, key);
    }

    public void removePlayerPermission(final String player, final String key) {
        this.instance.getTinyRabbit().sendAndReceive((delivery1 -> {

        }), Queue.GROUPS, GroupCalls.REQUEST_REMOVE_PLAYER_PERMISSION.name(), player, key);
    }

    public void addInheritance(final String group, final String key) {
        this.instance.getTinyRabbit().sendAndReceive((delivery1 -> {

        }), Queue.GROUPS, GroupCalls.REQUEST_ADD_INHERITANCE.name(), group, key);
    }

    public void removeInheritance(final String group, final String key) {
        this.instance.getTinyRabbit().sendAndReceive((delivery1 -> {

        }), Queue.GROUPS, GroupCalls.REQUEST_REMOVE_INHERITANCE.name(), group, key);
    }

    public void setGroup(final String target, final String group, final String sender, final long duration) {
        this.instance.getTinyRabbit().sendAndReceive((delivery1 -> {

        }), Queue.GROUPS, GroupCalls.REQUEST_SET_GROUP.name(), target, group, sender, String.valueOf(duration));
    }

    public List<String> getGroups() {
        final AtomicReference<List<String>> list = new AtomicReference<>();
        this.instance.getTinyRabbit().sendAndReceive((delivery -> {
            switch (GroupCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_GROUPS:
                    list.set(Arrays.asList(delivery.getData()[1].split("#")));
                    break;
            }
        }), Queue.GROUPS, GroupCalls.REQUEST_GROUPS.name());
        return list.get();
    }
}
