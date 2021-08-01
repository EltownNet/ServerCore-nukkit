package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.entity.Entity;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.nbt.tag.CompoundTag;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.entities.HumanNPC;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.language.Language;

public class NpcCommand extends PluginCommand<ServerCore> {

    public NpcCommand(final ServerCore owner) {
        super("npc", owner);
        this.setDescription("Npc Command");
        this.setPermission("core.command.npc");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender.isPlayer() && sender.isOp()) {
            final Player player = (Player) sender;
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("create")) this.createForm(player);
                else player.sendMessage("§b/npc create\n§b/npc edit <entityId>");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("edit")) {
                    try {
                        final long id = Long.parseLong(args[1]);
                        final Entity entity = player.getLevel().getEntity(id);

                        if (!(entity instanceof HumanNPC)) {
                            player.sendMessage("§cDies ist kein HumanNPC!");
                            return true;
                        }

                        this.editForm(player, entity);
                    } catch (final Exception e) {
                        player.sendMessage("§cFehler beim editieren.");
                    }
                } else player.sendMessage("§b/npc create\n§b/npc edit <entityId>");
            } else player.sendMessage("§b/npc create\n§b/npc edit <entityId>");
        }
        return false;
    }

    private void createForm(final Player player) {
        new CustomForm.Builder("§8» §fNPC erstellen")
                .addElement(new ElementInput("NPC ID", "servercore:testnpc"))
                .addElement(new ElementInput("NPC Name", "Karl"))
                .onSubmit((p, f) -> {
                    final String id = f.getInputResponse(0);
                    final String name = f.getInputResponse(1);

                    if (id.isEmpty()) {
                        p.sendMessage(Language.get("npc.missing.id"));
                        return;
                    }

                    if (name.isEmpty()) {
                        p.sendMessage(Language.get("npc.missing.name"));
                        return;
                    }

                    final CompoundTag tag = HumanNPC.createNBT(p, name, id);
                    Entity entity = Entity.createEntity(HumanNPC.class.getSimpleName(), p.chunk, tag);
                    entity.spawnToAll();
                    p.sendMessage(Language.get("npc.created"));
                })
                .build().send(player);
    }

    private void editForm(final Player player, final Entity entity) {
        final HumanNPC npc = (HumanNPC) entity;
        final CustomForm form = new CustomForm.Builder("HumanNPC " + entity.getId())
                .addElement(new ElementInput("§8» §7Anzeigename ändern", "Name", npc.getNameTag()))
                .addElement(new ElementInput("§8» §7ID ändern", "ID", npc.getNpcID()))
                .addElement(new ElementToggle("§8» §7An meine Position teleportieren", false))
                .addElement(new ElementToggle("§8» §7Endgültig entfernen", false))
                .onSubmit((g, h) -> {
                    final String name = h.getInputResponse(0);
                    if (!name.equals(npc.getNameTag())) {
                        npc.setNameTag(name);
                    }

                    final String id = h.getInputResponse(1);
                    if (!id.equals(npc.getNpcID())) {
                        npc.setID(id);
                    }

                    final boolean teleport = h.getToggleResponse(2);
                    if (teleport) {
                        npc.teleport(player.getLocation());
                    }

                    final boolean delete = h.getToggleResponse(3);
                    if (delete) {
                        entity.close();
                    }

                    player.sendMessage("Die Einstellungen von HumanNPC §9[" + entity.getId() + "] §fwurden gespeichert.");
                })
                .build();
        form.send(player);
    }
}
