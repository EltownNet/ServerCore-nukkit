package net.eltown.servercore.commands.feature;

import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;

import java.util.Arrays;

public class SettingsCommand extends PluginCommand<ServerCore> {

    public SettingsCommand(final ServerCore owner) {
        super("settings", owner);
        this.setDescription("Verwalte deine Account-Einstellungen");
        this.setAliases(Arrays.asList("st", "einstellungen").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return true;
    }
}
