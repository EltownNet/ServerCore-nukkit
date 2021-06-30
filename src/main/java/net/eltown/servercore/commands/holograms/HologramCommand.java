package net.eltown.servercore.commands.holograms;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.*;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

public class HologramCommand extends PluginCommand<ServerCore> {

    public HologramCommand(final ServerCore owner) {
        super("hologram", owner);
        this.setDescription("Hologram command");
        this.setPermission("core.commands.hologram");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            this.openMain((Player) sender);
        }
        return true;
    }

    private void openMain(final Player player) {
        final SimpleForm form = new SimpleForm.Builder("§7» §8Hologramme", "")
                .addButton(new ElementButton("§7» §fHologramm erstellen"), this::createHologram)
                .addButton(new ElementButton("§7» §fHologramm bearbeiten"), this::hologramSettings)
                .build();
        form.send(player);
    }

    private void createHologram(final Player player) {
        final CustomForm form = new CustomForm.Builder("§7» §8Hologramm erstellen")
                .addElement(new ElementStepSlider("Bitte wähle aus, wie viele Zeilen das Hologramm haben soll.", Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"), 2))
                .onSubmit((i, o) -> {
                    final CustomForm.Builder form1 = new CustomForm.Builder("§7» §8Hologramm erstellen");
                    form1.addElement(new ElementInput("Bitte gebe dem Hologramm einen einmaligen Namen.", "Name"));
                    form1.addElement(new ElementInput("Bitte gebe dem Hologramm einen Titel und (eine) Zeile(n).", "Titel"));
                    for (int x = 0; x < Integer.parseInt(o.getStepSliderResponse(0).getElementContent()); x++) {
                        form1.addElement(new ElementInput("", "Zeile " + (x + 1)));
                    }
                    form1.onSubmit((g, h) -> {
                        final String name = h.getInputResponse(0);

                        if (name.isEmpty() || this.getPlugin().getHologramHandler().hologramExists(name)) {
                            player.sendMessage(Language.get("holograms.invalid.input"));
                            return;
                        }

                        final LinkedList<String> lines = new LinkedList<>();
                        for (int x = 0; x < Integer.parseInt(o.getStepSliderResponse(0).getElementContent()); x++) {
                            lines.add((x + 1) + ">:<" + h.getInputResponse(2 + x));
                        }
                        this.getPlugin().getHologramHandler().createHologram(player, name, h.getInputResponse(1), lines);
                        player.sendMessage(Language.get("holograms.created", name));
                    });
                    form1.build().send(player);
                })
                .build();
        form.send(player);

    }

    private void hologramSettings(final Player player) {
        if (new ArrayList<>(this.getPlugin().getHologramHandler().particles.keySet()).size() == 0) {
            player.sendMessage(Language.get("holograms.no.holograms"));
            return;
        }
        final CustomForm form = new CustomForm.Builder("§7» §8Hologramm bearbeiten")
                .addElement(new ElementDropdown("Bitte wähle ein Hologramm aus, welches du bearbeiten möchtest.", new ArrayList<>(this.getPlugin().getHologramHandler().particles.keySet())))
                .onSubmit((g, h) -> {
                    final String name = h.getDropdownResponse(0).getElementContent();

                    final SimpleForm form1 = new SimpleForm.Builder("§7» §8Hologramm bearbeiten", "")
                            .addButton(new ElementButton("§7» §fZeile hinzufügen"), d -> {
                                final CustomForm form2 = new CustomForm.Builder("§7» §8Zeile hinzufügen")
                                        .addElement(new ElementInput("Bitte gebe der neuen Zeile einen Text.", "Neue Zeile"))
                                        .onSubmit((b, n) -> {
                                            final String line = n.getInputResponse(0);
                                            if (!line.isEmpty()) {
                                                this.getPlugin().getHologramHandler().addHologramLine(name, line);
                                                player.sendMessage(Language.get("holograms.line.added"));
                                            }
                                        })
                                        .build();
                                form2.send(g);
                            })
                            .addButton(new ElementButton("§7» §fLetzte Zeile entfernen"), d -> {
                                this.getPlugin().getHologramHandler().removeLastLine(name);
                                player.sendMessage(Language.get("holograms.line.removed"));
                            })
                            .addButton(new ElementButton("§7» §fErweitert"), d -> {
                                final CustomForm form2 = new CustomForm.Builder("§7» §8Erweitert")
                                        .addElement(new ElementToggle("Soll die Position des Hologramms auf deine aktualisiert werden?", false))
                                        .addElement(new ElementToggle("Soll das Hologramm endgültig gelöscht werden?", false))
                                        .onSubmit((b, n) -> {
                                            final boolean pos = n.getToggleResponse(0);
                                            final boolean del = n.getToggleResponse(1);
                                            if (!del) {
                                                if (pos) {
                                                    this.getPlugin().getHologramHandler().moveHologram(name, player.getLocation());
                                                    player.sendMessage(Language.get("holograms.moved"));
                                                }
                                            } else {
                                                this.getPlugin().getHologramHandler().deleteHologram(name);
                                                player.sendMessage(Language.get("holograms.deleted"));
                                            }
                                        })
                                        .build();
                                form2.send(g);
                            })
                            .build();
                    form1.send(player);
                })
                .build();
        form.send(player);
    }

}
