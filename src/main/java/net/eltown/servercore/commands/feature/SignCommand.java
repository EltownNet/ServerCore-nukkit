package net.eltown.servercore.commands.feature;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.item.Item;
import cn.nukkit.level.Sound;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.language.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SignCommand extends PluginCommand<ServerCore> {

    public SignCommand(final ServerCore owner) {
        super("sign", owner);
        this.setDescription("Signiere das Item in deiner Hand und mache es damit einzigartig");
        this.setAliases(Collections.singletonList("signieren").toArray(new String[]{}));
        this.setPermission("core.command.sign");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            if (args.length >= 1) {
                final Player player = (Player) sender;
                final Item item = player.getInventory().getItemInHand();

                if (item.getId() == 0) {
                    this.getPlugin().playSound(player, Sound.NOTE_BASS);
                    player.sendMessage(Language.get("sign.invalid.item"));
                    return true;
                }


                if (item.getLore().length > 0 && Arrays.asList(item.getLore()).get(0).equals("§8---------------------------")) {
                    player.sendMessage(Language.get("sign.already.signed"));
                    return true;
                }

                final StringBuilder builder = new StringBuilder();
                for (final String arg : args) builder.append(arg).append(" ");
                final String message = builder.substring(0, builder.length() - 1);

                final List<String> lore = new ArrayList<>();
                lore.add("§8---------------------------");
                lore.add("§r§0Item signiert von §2" + player.getName() + " §0am §2" + this.getPlugin().getDate() + "§0.");
                lore.add("§r§7" + message);
                lore.add("§8---------------------------");
                lore.addAll(Arrays.asList(item.getLore()));

                item.setLore(lore.toArray(new String[0]));
                player.getInventory().setItemInHand(item);

                this.getPlugin().playSound(player, Sound.RANDOM_LEVELUP);
                player.sendMessage(Language.get("sign.item.signed"));
            } else {
                this.getPlugin().playSound(((Player) sender).getPlayer(), Sound.NOTE_BASS);
                sender.sendMessage(Language.get("sign.usage"));
            }
        }
        return true;
    }
}
