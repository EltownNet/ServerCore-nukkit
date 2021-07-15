package net.eltown.servercore.commands.level;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.level.Level;

public class LevelCommand extends PluginCommand<ServerCore> {

    public LevelCommand(final ServerCore owner) {
        super("level", owner);
        this.setDescription("Lasse dir deinen Level-Fortschritt anzeigen");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            final Level level = this.getPlugin().getLevelAPI().getLevel(player.getName());
            player.sendMessage("Level: " + level.getLevel() + " XP: " + level.getExperience());
        }
        return true;
    }
}
