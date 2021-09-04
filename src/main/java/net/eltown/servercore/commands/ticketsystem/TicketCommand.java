package net.eltown.servercore.commands.ticketsystem;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementToggle;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.data.ticketsystem.Ticket;
import net.eltown.servercore.components.data.ticketsystem.TicketCalls;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.modal.ModalForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.Queue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class TicketCommand extends PluginCommand<ServerCore> {

    public TicketCommand(ServerCore owner) {
        super("ticket", owner);
        this.setDescription("Wenn du die Hilfe eines Supporters benötigst, öffne ein Ticket");
        this.setAliases(Arrays.asList("support", "report", "hilfe").toArray(new String[]{}));
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            this.openMain(player);
        }
        return true;
    }

    private void openMain(final Player player) {
        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8TicketSystem", "§fWähle eine Option, um fortzufahren.");
        form.addButton(new ElementButton("§7» §fNeues Ticket eröffnen"), this::openCreateTicket);
        form.addButton(new ElementButton("§7» §fMeine Tickets"), this::openMyTickets);
        if (player.hasPermission("core.ticketsystem.manage")) form.addButton(new ElementButton("§7» §4Ticket Administration"), this::openTicketAdministration);
        form.build().send(player);
    }

    private void openCreateTicket(final Player player) {
        final CustomForm form = new CustomForm.Builder("§7» §8Neues Ticket eröffnen")
                .addElement(new ElementInput("Bitte gebe einen kurzen Betreff an.", "Betreff"))
                .addElement(new ElementDropdown("Wähle einen Bereich deines Anliegens aus.", Arrays.asList("Spielerbeschwerde", "Fehlermeldung", "Hilfe benötigt", "Feedback / Vorschläge", "Sonstiges"), 0))
                .addElement(new ElementInput("Verfasse eine Nachricht, welche dein Anliegen näher erklärt.", "Nachricht (ca. 80 Zeichen)"))
                .addElement(new ElementDropdown("Priorität des Tickets.", Arrays.asList("§cHoch", "§6Normal", "§eGering"), 1))
                .addElement(new ElementToggle("Dieses Ticket im Discord-Server direkt öffnen.\n§cDiese Option ist nur möglich, wenn dein Discord-Account mit deinem Ingame-Account verknüpft ist. §8[§b/auth§8]", false))
                .onSubmit((g, h) -> {
                    final String subject = h.getInputResponse(0);
                    final String section = h.getDropdownResponse(1).getElementContent();
                    final String message = h.getInputResponse(2);
                    final String priority = h.getDropdownResponse(3).getElementContent();
                    final boolean discord = h.getToggleResponse(4);

                    if (subject.isEmpty() || message.isEmpty()) {
                        player.sendMessage(Language.get("ticket.input.invalid"));
                        return;
                    }

                    if (discord) {
                        this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                            switch (TicketCalls.DiscordTicketCalls.valueOf(delivery.getKey().toUpperCase())) {
                                case CALLBACK_NO_AUTH:
                                    player.sendMessage(Language.get("ticket.discord.no.auth"));
                                    break;
                                case CALLBACK_USER_NULL:
                                    player.sendMessage(Language.get("ticket.discord.user.null"));
                                    break;
                                case CALLBACK_TOO_MANY_TICKETS:
                                    player.sendMessage(Language.get("ticket.discord.tickets.limit"));
                                    break;
                                case CALLBACK_NULL:
                                    player.sendMessage(Language.get("ticket.discord.created"));
                                    break;
                            }
                        }, Queue.DISCORD_TICKET, TicketCalls.DiscordTicketCalls.REQUEST_OPEN_TICKET.name(), player.getName(), subject, section, message);
                    } else {
                        this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
                            switch (TicketCalls.valueOf(delivery.getKey().toUpperCase())) {
                                case CALLBACK_TOO_MANY_TICKETS:
                                    player.sendMessage(Language.get("ticket.too.many"));
                                    break;
                                case CALLBACK_NULL:
                                    player.sendMessage(Language.get("ticket.created"));
                                    break;
                            }
                        }, Queue.TICKET_CALLBACK, TicketCalls.REQUEST_OPEN_TICKET.name(), player.getName(), subject, section, priority, message);
                    }
                })
                .build();
        form.send(player);
    }

    private void openMyTickets(final Player player) {
        this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
            switch (TicketCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_NO_TICKETS:
                    player.sendMessage(Language.get("ticket.no.tickets"));
                    break;
                case CALLBACK_MY_TICKETS:
                    final List<String> list = Arrays.asList(delivery.getData());
                    final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Meine Tickets", "Klicke auf eines deiner Tickets, um weitere Informationen zu erhalten.");

                    list.forEach(e -> {
                        if (!e.equals(delivery.getKey().toLowerCase())) {
                            final String[] d = e.split(">>");
                            if (d[8].equals("null")) {
                                form.addButton(new ElementButton(d[3] + "\n§f" + d[2] + " §8| " + d[5]), g -> {
                                    this.openTicket(g, d);
                                });
                            } else {
                                form.addButton(new ElementButton(d[3] + "\n§f" + d[2] + " §8| §cGeschlossen"), g -> {
                                    this.openTicket(g, d);
                                });
                            }
                        }
                    });
                    form.addButton(new ElementButton("§7» §fZurück"), this::openMain);
                    form.build().send(player);
                    break;
            }
        }, Queue.TICKET_CALLBACK, TicketCalls.REQUEST_MY_TICKETS.name(), player.getName());
    }

    private void openTicket(final Player player, final String[] data) {
        final LinkedHashMap<String, String> messages = new LinkedHashMap<>();
        for (final String v : data[6].split("~~~")) {
            final String[] p = v.split(">:<");
            messages.put(p[1], p[0]);
        }
        final Ticket ticket = new Ticket(data[0], data[1], data[2], data[3], data[4], data[5], messages, data[7], data[8]);

        final StringBuilder content = new StringBuilder();
        if (!ticket.getDateClosed().equals("null")) content.append("§cDieses Ticket ist bereits geschlossen!\n\n");
        content.append("§fTicket von: §a").append(ticket.getCreator()).append("\n");
        content.append("§fBetreff: §e").append(ticket.getSubject()).append("\n");
        content.append("§fSupporter: §b").append(ticket.getSupporter().replace("null", "Keiner")).append("\n");
        content.append("§fPriorität: §7").append(ticket.getPriority()).append("\n\n");
        content.append("§f§l- Nachrichten: -§f§r\n");
        messages.keySet().forEach(message -> {
            String prefix = "§b";
            if (messages.get(message).equals(player.getName())) prefix = "§a";
            content.append(prefix).append(messages.get(message)).append("§f:\n").append(message).append("\n\n");
        });

        final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Ticket " + ticket.getId(), content.toString());

        if (player.hasPermission("core.ticketsystem.manage") && !ticket.getCreator().equals(player.getName()) && ticket.getSupporter().equals("null") && ticket.getDateClosed().equals("null")) {
            form.addButton(new ElementButton("§7» §aTicket als Supporter annehmen"), e -> {
                this.getPlugin().getTinyRabbit().send(Queue.TICKET_RECEIVE, TicketCalls.REQUEST_TAKE_TICKET.name(), player.getName(), ticket.getId());
                player.sendMessage(Language.get("ticket.supporter.took", ticket.getId()));
            });
        }

        if (!ticket.getSupporter().equals("null") && !player.getName().equals(ticket.getCreator()) && ticket.getDateClosed().equals("null")) {
            form.addButton(new ElementButton("§7» §fNeue Nachricht verfassen"), e -> {
                final CustomForm form1 = new CustomForm.Builder("§7» §8Neue Nachricht verfassen")
                        .addElement(new ElementInput("Nachricht für Ticket: §7" + ticket.getId(), "Nachricht"))
                        .onSubmit((g, h) -> {
                            final String message = h.getInputResponse(0);

                            if (message.isEmpty() || messages.containsKey(message)) {
                                player.sendMessage(Language.get("ticket.input.invalid"));
                                return;
                            }

                            this.getPlugin().getTinyRabbit().send(Queue.TICKET_RECEIVE, TicketCalls.REQUEST_SEND_MESSAGE.name(), player.getName(), message, ticket.getId());
                            player.sendMessage(Language.get("ticket.message.sent", ticket.getId()));
                        })
                        .build();
                form1.send(player);
            });
        } else {
            if (ticket.getCreator().equals(player.getName()) && ticket.getDateClosed().equals("null")) {
                form.addButton(new ElementButton("§7» §fNeue Nachricht verfassen"), e -> {
                    final CustomForm form1 = new CustomForm.Builder("§7» §8Neue Nachricht verfassen")
                            .addElement(new ElementInput("Nachricht für Ticket: §7" + ticket.getId(), "Nachricht"))
                            .onSubmit((g, h) -> {
                                final String message = h.getInputResponse(0);

                                if (message.isEmpty() || messages.containsKey(message)) {
                                    player.sendMessage(Language.get("ticket.input.invalid"));
                                    return;
                                }

                                this.getPlugin().getTinyRabbit().send(Queue.TICKET_RECEIVE, TicketCalls.REQUEST_SEND_MESSAGE.name(), player.getName(), message, ticket.getId());
                                player.sendMessage(Language.get("ticket.message.sent", ticket.getId()));
                            })
                            .build();
                    form1.send(player);
                });
            }
        }

        if (player.hasPermission("core.ticketsystem.manage.extended") && !ticket.getCreator().equals(player.getName()) && ticket.getDateClosed().equals("null")) {
            form.addButton(new ElementButton("§7» §fPriorität ändern"), e -> {
                final CustomForm form1 = new CustomForm.Builder("§7» §8Ticket Priorität ändern")
                        .addElement(new ElementDropdown("Priorität für Ticket ändern: §7" + ticket.getId(), Arrays.asList("§4Dringend", "§cHoch", "§6Normal", "§eGering"), 2))
                        .onSubmit((g, h) -> {
                            this.getPlugin().getTinyRabbit().send(Queue.TICKET_RECEIVE, TicketCalls.REQUEST_SET_PRIORITY.name(), player.getName(), h.getDropdownResponse(0).getElementContent(), ticket.getId());
                            player.sendMessage(Language.get("ticket.priority.changed", h.getDropdownResponse(0).getElementContent(), ticket.getId()));
                        })
                        .build();
                form1.send(player);
            });
        }

        if (ticket.getCreator().equals(player.getName()) || ticket.getSupporter().equals(player.getName())) {
            if (ticket.getDateClosed().equals("null")) {
                form.addButton(new ElementButton("§7» §4Ticket schließen"), e -> {
                    final ModalForm form1 = new ModalForm.Builder("§7» §8Ticket schließen", "Möchtest du das Ticket wirklich schließen? Nach einer Schließung kann dieses nicht erneut geöffnet werden.",
                            "§7» §aTicket schließen", "§7» §cAbbrechen")
                            .onYes(g -> {
                                this.getPlugin().getTinyRabbit().send(Queue.TICKET_RECEIVE, TicketCalls.REQUEST_CLOSE_TICKET.name(), player.getName(), ticket.getId());
                                player.sendMessage(Language.get("ticket.closed", ticket.getId()));
                            })
                            .onNo(g -> {
                            })
                            .build();
                    form1.send(player);
                });
            }
        }


        if (ticket.getCreator().equals(player.getName())) {
            form.addButton(new ElementButton("§7» §fZurück"), this::openMyTickets);
        } else if (ticket.getSupporter().equals(player.getName())) {
            form.addButton(new ElementButton("§7» §fZurück"), this::openTicketAdministration);
        }

        form.build().send(player);
    }

    private void openTicketAdministration(final Player player) {
        this.getPlugin().getTinyRabbit().sendAndReceive(delivery -> {
            switch (TicketCalls.valueOf(delivery.getKey().toUpperCase())) {
                case CALLBACK_OPEN_TICKETS:
                    final List<String> openedTickets = Arrays.asList(delivery.getData());

                    this.getPlugin().getTinyRabbit().sendAndReceive(delivery1 -> {
                        switch (TicketCalls.valueOf(delivery1.getKey().toUpperCase())) {
                            case CALLBACK_MY_SUPPORT_TICKETS:
                                final List<String> mySupportTickets = Arrays.asList(delivery1.getData());

                                final SimpleForm simpleForm = new SimpleForm.Builder("§7» §8Administration", "")
                                        .addButton(new ElementButton("§7» §fOffene Tickets"), e -> {
                                            final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Offene Tickets", "Klicke auf eines der Tickets, um weitere Informationen zu erhalten.");
                                            try {
                                                openedTickets.forEach(i -> {
                                                    if (!i.equals(delivery.getKey().toLowerCase())) {
                                                        final String[] d = i.split(">>");
                                                        form.addButton(new ElementButton(d[0] + "\n§f" + d[2] + " §8| " + d[5]), g -> {
                                                            this.openTicket(g, d);
                                                        });
                                                    }
                                                });
                                                form.addButton(new ElementButton("§7» §fZurück"), this::openTicketAdministration);
                                                form.build().send(player);
                                            } catch (final Exception exception) {
                                                player.sendMessage(Language.get("ticket.no.tickets"));
                                            }
                                        })
                                        .addButton(new ElementButton("§7» §fTickets unter meiner\nBearbeitung"), e -> {
                                            final SimpleForm.Builder form = new SimpleForm.Builder("§7» §8Tickets, die ich bearbeite", "Klicke auf eines der Tickets, um weitere Informationen zu erhalten.");
                                            try {
                                                mySupportTickets.forEach(i -> {
                                                    if (!i.equals(delivery1.getKey().toLowerCase())) {
                                                        final String[] d = i.split(">>");
                                                        form.addButton(new ElementButton(d[0] + "\n§f" + d[2] + " §8| " + d[5]), g -> {
                                                            this.openTicket(g, d);
                                                        });
                                                    }
                                                });
                                                form.addButton(new ElementButton("§7» §fZurück"), this::openTicketAdministration);
                                                form.build().send(player);
                                            } catch (final Exception exception) {
                                                player.sendMessage(Language.get("ticket.no.tickets"));
                                            }
                                        })
                                        .addButton(new ElementButton("§7» §fZurück"), this::openMain)
                                        .build();
                                simpleForm.send(player);
                                break;
                        }
                    }, Queue.TICKET_CALLBACK, TicketCalls.REQUEST_MY_SUPPORT_TICKETS.name(), player.getName());
                    break;
            }
        }, Queue.TICKET_CALLBACK, TicketCalls.REQUEST_OPEN_TICKETS.name(), "null");
    }

}