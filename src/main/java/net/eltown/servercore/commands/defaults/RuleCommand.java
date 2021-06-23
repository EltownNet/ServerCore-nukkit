package net.eltown.servercore.commands.defaults;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;

public class RuleCommand extends PluginCommand<ServerCore> {

    public RuleCommand(ServerCore owner) {
        super("rules", owner);
        this.setDescription("Server Regeln");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {

        }
        return true;
    }
}
