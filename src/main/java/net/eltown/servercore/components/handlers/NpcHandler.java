package net.eltown.servercore.components.handlers;

import lombok.Getter;
import net.eltown.servercore.ServerCore;

import java.util.HashSet;
import java.util.Set;

public class NpcHandler {

    @Getter
    private final Set<String> managers = new HashSet<>();
    private final ServerCore plugin;


    public NpcHandler(final ServerCore plugin) {
        this.plugin = plugin;
    }



}
