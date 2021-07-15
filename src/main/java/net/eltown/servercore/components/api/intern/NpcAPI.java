package net.eltown.servercore.components.api.intern;

import lombok.Getter;
import net.eltown.servercore.ServerCore;

import java.util.HashSet;
import java.util.Set;

public class NpcAPI {

    @Getter
    private final Set<String> managers = new HashSet<>();
    private final ServerCore plugin;


    public NpcAPI(final ServerCore plugin) {
        this.plugin = plugin;
    }

}
