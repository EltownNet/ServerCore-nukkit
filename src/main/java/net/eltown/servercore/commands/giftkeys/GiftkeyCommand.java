package net.eltown.servercore.commands.giftkeys;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;

public class GiftkeyCommand extends PluginCommand<ServerCore> {

    public GiftkeyCommand(ServerCore owner) {
        super("giftkey", owner);
        this.setDescription("Erstelle und verwalte Giftkeys");
        this.setPermission("core.commands.giftkey");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {

        }
        return true;
    }
}