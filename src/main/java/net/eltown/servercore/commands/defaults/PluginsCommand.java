package net.eltown.servercore.commands.defaults;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.plugin.PluginDescription;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.simple.SimpleForm;

import java.util.Arrays;

public class PluginsCommand extends PluginCommand<ServerCore> {

    public PluginsCommand(ServerCore owner) {
        super("plugins", owner);
        this.setDescription("Eine Auflistung der Plugins auf diesem Server");
        this.setAliases(Arrays.asList("pl", "plugin").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            this.openPlugins(player);
        }
        return true;
    }

    private void openPlugins(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Plugins", "Hier sind alle Plugins dieses Servers aufgelistet. Klicke auf eines, um näheres zu erfahren.\n§aAnzahl: §f" + this.getPlugin().getServer().getPluginManager().getPlugins().values().size());
        this.getPlugin().getServer().getPluginManager().getPlugins().values().forEach(e -> {
            form.addButton(new ElementButton("§8» §a" + e.getDescription().getName() + "\n§f" + e.getDescription().getVersion()), g -> {
                final SimpleForm form1 = new SimpleForm.Builder("§7» §8" + e.getDescription().getName(), this.pluginDescription(e.getDescription()))
                        .addButton(new ElementButton("§7» §cZurück"), this::openPlugins)
                        .build();
                form1.send(g);
            });
        });
        form.build().send(player);
    }

    private String pluginDescription(final PluginDescription plugin) {
        if (plugin.getAuthors().size() != 0) {
            final StringBuilder authorList = new StringBuilder();
            plugin.getAuthors().forEach(e -> {
                authorList.append(e).append(",").append(" ");
            });
            final String authors = authorList.substring(0, authorList.length() - 2);

            if (plugin.getAuthors().size() == 1) {
                return "§fPlugin: §a" + plugin.getName() + "\n§fVersion: §a" + plugin.getVersion() + "\n§fAutor: §a" + authors + "\n§fBeschreibung: §a" + plugin.getDescription();
            } else {
                return "§fPlugin: §a" + plugin.getName() + "\n§fVersion: §a" + plugin.getVersion() + "\n§fAutoren: §a" + authors + "\n§fBeschreibung: §a" + plugin.getDescription();
            }
        } else {
            return "§fPlugin: §a" + plugin.getName() + "\n§fVersion: §a" + plugin.getVersion() + "\n§fBeschreibung: §a" + plugin.getDescription();
        }
    }

}
