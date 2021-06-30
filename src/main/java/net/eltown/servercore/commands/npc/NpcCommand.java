package net.eltown.servercore.commands.npc;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.entity.Entity;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.nbt.tag.CompoundTag;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.entities.HumanNPC;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;
import net.eltown.servercore.components.language.Language;

public class NpcCommand extends PluginCommand<ServerCore> {

    public NpcCommand(final ServerCore owner) {
        super("npc", owner);
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (sender.isPlayer() && sender.isOp()) {
            final Player player = (Player) sender;
            this.openMain(player);
        }
        return false;
    }

    private void openMain(final Player player) {
        new SimpleForm.Builder("§8» §fNPC", "Hier kannst du NPCs erstellen und verwalten.")
                .addButton(new ElementButton("§8» §fNPC erstellen"), this::createForm)
                .addButton(new ElementButton("§8» §fNPC bearbeiten"), (p) -> {
                    this.getPlugin().getNpcHandler().getManagers().add(p.getName());
                    p.sendMessage(Language.get("npc.click"));
                })
                .build().send(player);
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
}
