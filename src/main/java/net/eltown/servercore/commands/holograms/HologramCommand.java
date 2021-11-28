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
                .addButton(new ElementButton("§8» §fHologramm erstellen"), this::createHologram)
                .addButton(new ElementButton("§8» §fHologramm bearbeiten"), this::hologramSettings)
                .build();
        form.send(player);
    }

    private void createHologram(final Player player) {
        final CustomForm form = new CustomForm.Builder("§7» §8Hologramm erstellen")
                .addElement(new ElementSlider("Bitte wähle aus, wie viele Zeilen das Hologramm haben soll", 0, 12, 1, 0))
                .onSubmit((i, o) -> {
                    final CustomForm.Builder form1 = new CustomForm.Builder("§7» §8Hologramm erstellen");
                    form1.addElement(new ElementInput("Bitte gebe dem Hologramm einen einmaligen Namen.", "Name"));
                    form1.addElement(new ElementInput("Bitte gebe dem Hologramm einen Titel und fülle die Zeilen aus.", "Titel"));
                    if (o.getSliderResponse(0) != 0) {
                        for (int x = 0; x < o.getSliderResponse(0); x++) {
                            form1.addElement(new ElementInput("", "Zeile " + (x + 1)));
                        }
                    }
                    form1.onSubmit((g, h) -> {
                        final String name = h.getInputResponse(0);

                        if (name.isEmpty() || this.getPlugin().getHologramAPI().hologramExists(name)) {
                            player.sendMessage(Language.get("holograms.invalid.input"));
                            return;
                        }

                        final LinkedList<String> lines = new LinkedList<>();
                        if (o.getSliderResponse(0) != 0) {
                            for (int x = 0; x < o.getSliderResponse(0); x++) {
                                lines.add(h.getInputResponse(2 + x));
                            }
                        }
                        this.getPlugin().getHologramAPI().createHologram(player, name, h.getInputResponse(1), lines);
                        player.sendMessage(Language.get("holograms.created", name));
                    });
                    form1.build().send(player);
                })
                .build();
        form.send(player);

    }

    private void hologramSettings(final Player player) {
        if (new ArrayList<>(this.getPlugin().getHologramAPI().particles.keySet()).size() == 0) {
            player.sendMessage(Language.get("holograms.no.holograms"));
            return;
        }
        final CustomForm form = new CustomForm.Builder("§8» §fHologramm bearbeiten")
                .addElement(new ElementDropdown("Bitte wähle ein Hologramm aus, welches du bearbeiten möchtest.", new ArrayList<>(this.getPlugin().getHologramAPI().particles.keySet())))
                .onSubmit((g, h) -> {
                    final String name = h.getDropdownResponse(0).getElementContent();

                    final SimpleForm form1 = new SimpleForm.Builder("§7» §8Hologramm bearbeiten", "§8» §fHologramm: §9" + name)
                            .addButton(new ElementButton("§8» §fZeile hinzufügen"), d -> {
                                final CustomForm form2 = new CustomForm.Builder("§7» §8Zeile hinzufügen")
                                        .addElement(new ElementInput("Bitte gebe der neuen Zeile einen Text.", "Neue Zeile"))
                                        .onSubmit((b, n) -> {
                                            final String line = n.getInputResponse(0);
                                            if (!line.isEmpty()) {
                                                this.getPlugin().getHologramAPI().addHologramLine(name, line);
                                                player.sendMessage(Language.get("holograms.line.added"));
                                            }
                                        })
                                        .build();
                                form2.send(g);
                            })
                            .addButton(new ElementButton("§8» §fZeile entfernen"), d -> {
                                final CustomForm form2 = new CustomForm.Builder("§7» §8Zeile entfernen")
                                        .addElement(new ElementSlider("Welche Zeile soll entfernt werden?", 1, this.getPlugin().getHologramAPI().getHologramLines(name), 1, 1))
                                        .onSubmit((b, n) -> {
                                            this.getPlugin().getHologramAPI().removeLine(name, (int) n.getSliderResponse(0));
                                            player.sendMessage(Language.get("holograms.line.removed"));
                                        })
                                        .build();
                                form2.send(g);
                            })
                            .addButton(new ElementButton("§8» §fTitel ändern"), d -> {
                                final CustomForm form2 = new CustomForm.Builder("§7» §8Titel ändern")
                                        .addElement(new ElementInput("Gebe einen neuen Titel an.", "Titel", this.getPlugin().getHologramAPI().particles.get(name).getTitle()))
                                        .onSubmit((b, n) -> {
                                            this.getPlugin().getHologramAPI().setTitle(name, n.getInputResponse(0));
                                            player.sendMessage(Language.get("holograms.title.edited"));
                                        })
                                        .build();
                                form2.send(g);
                            })
                            .addButton(new ElementButton("§8» §fErweitert"), d -> {
                                final CustomForm form2 = new CustomForm.Builder("§7» §8Erweitert")
                                        .addElement(new ElementToggle("Soll die Position des Hologramms auf deine aktualisiert werden?", false))
                                        .addElement(new ElementToggle("Soll das Hologramm endgültig gelöscht werden?", false))
                                        .onSubmit((b, n) -> {
                                            final boolean pos = n.getToggleResponse(0);
                                            final boolean del = n.getToggleResponse(1);
                                            if (!del) {
                                                if (pos) {
                                                    this.getPlugin().getHologramAPI().moveHologram(name, player.getLocation());
                                                    player.sendMessage(Language.get("holograms.moved"));
                                                }
                                            } else {
                                                this.getPlugin().getHologramAPI().deleteHologram(name);
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
