package net.eltown.servercore.commands.giftkeys;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;

import java.util.Arrays;

public class RedeemCommand extends PluginCommand<ServerCore> {

    public RedeemCommand(ServerCore owner) {
        super("redeem", owner);
        this.setDescription("Löse einen Giftkey ein");
        this.setAliases(Arrays.asList("einlösen", "key").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {

        }
        return true;
    }
}