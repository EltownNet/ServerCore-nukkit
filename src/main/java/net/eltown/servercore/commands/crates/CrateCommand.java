package net.eltown.servercore.commands.crates;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.*;
import cn.nukkit.item.Item;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.api.intern.SyncAPI;
import net.eltown.servercore.components.data.crates.data.CrateReward;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;

import java.util.Arrays;

public class CrateCommand extends PluginCommand<ServerCore> {

    public CrateCommand(final ServerCore owner) {
        super("crate", owner);
        this.setDescription("Crate Command");
        this.setPermission("core.command.crate");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            final CustomForm form = new CustomForm.Builder("§7» §8Crate Einstellungen")
                    .addElement(new ElementDropdown("§8» §fBitte wähle einen Crate-Typ aus, um die Gewinne zu bearbeiten.", Arrays.asList("common", "uncommon", "epic", "legendary")))
                    .onSubmit((g, h) -> {
                        final String crate = h.getDropdownResponse(0).getElementContent();
                        this.openCrateRewards(player, crate);
                    })
                    .build();
            form.send(player);
        }
        return true;
    }

    private void openCrateRewards(final Player player, final String crate) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Crate Gewinne", "§8» §fCrate: §9" + crate.toUpperCase() + "\n\n");
        form.addButton(new ElementButton("§8» §9Neuen Gewinn erstellen"), g -> {
            this.insertCrateReward(player, crate);
        });

        this.getPlugin().getCrateAPI().getCrateRewards(crate, set -> {
            set.forEach(e -> {
                form.addButton(new ElementButton("§8» " + e.getDisplayName() + "\n§8[§f" + e.getId() + "§8]"), g -> {
                    this.updateCrateReward(player, e);
                });
            });
        });
        form.build().send(player);
    }

    private void insertCrateReward(final Player player, final String crate) {
        final CustomForm form = new CustomForm.Builder("§7» §8Neuen Gewinn erstellen")
                .addElement(new ElementLabel("§8» §fCrate: §9" + crate.toUpperCase()))
                .addElement(new ElementInput("§8» §fAnzeigename des Gewinns", "16x Eichenholz"))
                .addElement(new ElementSlider("§8» §fChance, dass der Gewinn gezogen wird", 0, 100, 1, 1))
                .addElement(new ElementInput("§8» §fGewinn-Daten\n§7item;<slot>\n§7money;amount\nxp;amount\ncrate;type;amount", "money;300"))
                .onSubmit((g, h) -> {
                    final String displayName = h.getInputResponse(1);
                    final int chance = (int) h.getSliderResponse(2);
                    String data = h.getInputResponse(3);

                    if (data.startsWith("item")) {
                        final Item item = player.getInventory().slots.get(Integer.parseInt(data.split(";")[1]));
                        data = "item;" + SyncAPI.ItemAPI.pureItemToStringWithCount(item);
                    }

                    this.getPlugin().getCrateAPI().insertCrateReward(this.getPlugin().createId(10, "F-CR"), crate, displayName, chance, data);
                    player.sendMessage("§8» §fCore §8| §fNeuer Gewinn wurde erstellt.");
                })
                .build();
        form.send(player);
    }

    private void updateCrateReward(final Player player, final CrateReward crateReward) {
        final CustomForm form = new CustomForm.Builder("§7» §8Bestehenden Gewinn aktualisieren")
                .addElement(new ElementLabel("§8» §fID: §9" + crateReward.getId()))
                .addElement(new ElementLabel("§8» §fCrate: §9" + crateReward.getCrate().toUpperCase()))
                .addElement(new ElementInput("§8» §fAnzeigename des Gewinns", "16x Eichenholz", crateReward.getDisplayName()))
                .addElement(new ElementSlider("§8» §fChance, dass der Gewinn gezogen wird", 0, 100, 1, crateReward.getChance()))
                .addElement(new ElementInput("§8» §fGewinn-Daten\n§7item;<slot>\n§7money;amount\nxp;amount\ncrate;type;amount", "money;300", crateReward.getData()))
                .addElement(new ElementToggle("§8» §fItem-Gewinn aktualisieren", false))
                .addElement(new ElementToggle("§8» §cReward löschen", false))
                .onSubmit((g, h) -> {
                    final String displayName = h.getInputResponse(2);
                    final int chance = (int) h.getSliderResponse(3);
                    String data = h.getInputResponse(4);
                    final boolean b = h.getToggleResponse(5);
                    final boolean delete = h.getToggleResponse(6);

                    if (delete) {
                        this.getPlugin().getCrateAPI().deleteCrateReward(crateReward.getId());
                        player.sendMessage("§8» §fCore §8| §fDer Gewinn wurde gelöscht.");
                        return;
                    }

                    if (b && data.startsWith("item")) {
                        final Item item = player.getInventory().slots.get(Integer.parseInt(data.split(";")[1]));
                        data = "item;" + SyncAPI.ItemAPI.pureItemToStringWithCount(item);
                    }

                    this.getPlugin().getCrateAPI().updateCrateReward(crateReward.getId(), crateReward.getCrate(), displayName, chance, data);
                    player.sendMessage("§8» §fCore §8| §fGewinn wurde aktualisiert.");
                })
                .build();
        form.send(player);
    }

}
