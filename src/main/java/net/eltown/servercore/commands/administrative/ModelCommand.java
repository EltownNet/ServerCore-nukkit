package net.eltown.servercore.commands.administrative;

import cn.nukkit.Player;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.PluginCommand;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.entity.data.Skin;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import cn.nukkit.form.element.ElementToggle;
import cn.nukkit.level.Location;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.FloatTag;
import cn.nukkit.nbt.tag.ListTag;
import net.eltown.servercore.ServerCore;
import net.eltown.servercore.components.entities.ModelEntity;
import net.eltown.servercore.components.forms.custom.CustomForm;
import net.eltown.servercore.components.forms.simple.SimpleForm;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;

public class ModelCommand extends PluginCommand<ServerCore> {

    public ModelCommand(final ServerCore owner) {
        super("model", owner);
        this.setDescription("Model Command");
        this.setPermission("core.command.model");
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!sender.hasPermission(this.getPermission())) return true;
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            if (args.length == 4) {
                try {
                    this.spawnEntity(player.getLocation(), args[0], args[1], args[2], Float.parseFloat(args[3]), "ModelEntity");
                    player.sendMessage("§bDas Model wurde gespawnt.");
                } catch (final Exception e) {
                    player.sendMessage("§cFehler beim erstellen. Bitte überprüfe deine Angaben.");
                }
            } else if (args.length == 1) {
                if (args[0].equalsIgnoreCase("create")) {
                    final CustomForm form = new CustomForm.Builder("ModelEntity erstellen")
                            .addElement(new ElementLabel("Der Identifier ist auch der Name der .png-Datei und der .geo.json-Datei."))
                            .addElement(new ElementInput("Geometry Identifier", "model"))
                            .addElement(new ElementInput("Entity Displayname", "ModelEntity"))
                            .addElement(new ElementInput("Entity Scale", "1.0", "1.0"))
                            .onSubmit((g, h) -> {
                                try {
                                    final String identifier = h.getInputResponse(1);
                                    final String displayName = h.getInputResponse(2);
                                    final float scale = Float.parseFloat(h.getInputResponse(3));

                                    this.spawnEntity(player.getLocation(), identifier, identifier + ".png", identifier + ".geo.json", scale, displayName);
                                    player.sendMessage("§bDas Model wurde gespawnt.");
                                } catch (final Exception e ) {
                                    player.sendMessage("§cFehler beim erstellen. Bitte überprüfe deine Angaben.");
                                }
                            })
                            .build();
                    form.send(player);
                } else player.sendMessage("§b/model create\n§b/model edit <entityId>");
            } else if (args.length == 2) {
                if (args[0].equalsIgnoreCase("edit")) {
                    try {
                        final long id = Long.parseLong(args[1]);
                        final Entity entity = player.getLevel().getEntity(id);

                        if (!(entity instanceof ModelEntity)) {
                            player.sendMessage("§cDies ist keine ModelEntity!");
                            return true;
                        }
                        final CustomForm form = new CustomForm.Builder("ModelEntity " + entity.getId())
                                .addElement(new ElementInput("§8» §7Anzeigename ändern", "Name", entity.getNameTag()))
                                .addElement(new ElementToggle("§8» §7An meine Position teleportieren", false))
                                .addElement(new ElementToggle("§8» §7Endgültig entfernen", false))
                                .onSubmit((g, h) -> {
                                    final String name = h.getInputResponse(0);
                                    if (!name.equals(entity.getNameTag())) {
                                        entity.setNameTag(name);
                                    }

                                    final boolean teleport = h.getToggleResponse(2);
                                    if (teleport) {
                                        entity.teleport(player.getLocation());
                                    }

                                    final boolean delete = h.getToggleResponse(3);
                                    if (delete) {
                                        entity.close();
                                    }

                                    player.sendMessage("Die Einstellungen von ModelEntity §9[" + entity.getId() + "] §fwurden gespeichert.");
                                })
                                .build();
                        form.send(player);
                    } catch (final Exception e) {
                        player.sendMessage("§cFehler beim editieren.");
                    }
                } else player.sendMessage("§b/model create\n§b/model edit <entityId>");
            } else player.sendMessage("§b/model create\n§b/model edit <entityId>");
        }
        return true;
    }

    private EntityHuman spawnEntity(final Location location, final String name, final String skinURI, final String geometryDataURI, final float scale, final String displayName) {
        try {
            final File skinFile = new File(this.getPlugin().getDataFolder() + "/models/" + skinURI);
            final File geometryFile = new File(this.getPlugin().getDataFolder() + "/models/" + geometryDataURI);

            final Skin skin = new Skin();
            final BufferedImage skinData = ImageIO.read(skinFile);
            final String geometry = new String(Files.readAllBytes(geometryFile.toPath()));

            skin.setGeometryData(geometry);
            skin.setGeometryName("geometry." + name);
            skin.setSkinData(skinData);
            skin.setSkinId(name);
            skin.setTrusted(true);
            skin.setPremium(true);

            final EntityHuman entityHuman = (EntityHuman) Entity.createEntity(ModelEntity.class.getSimpleName(), location.getChunk(), this.nbt(location, skin, name, scale, displayName));
            entityHuman.setImmobile(false);
            entityHuman.setMovementSpeed(1.0f);
            entityHuman.setScale(scale);
            entityHuman.setNameTagVisible(false);
            entityHuman.setNameTagAlwaysVisible(false);

            entityHuman.spawnToAll();

            return entityHuman;
        } catch (final Exception ignored) {

        }
        return null;
    }

    private CompoundTag nbt(final Location location, final Skin skin, final String name, final float scale, final String displayName) {
        final CompoundTag nbt = new CompoundTag()
                .putList(new ListTag<>("Pos")
                        .add(new DoubleTag("", location.x))
                        .add(new DoubleTag("", location.y))
                        .add(new DoubleTag("", location.z)))
                .putList(new ListTag<DoubleTag>("Motion")
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0))
                        .add(new DoubleTag("", 0)))
                .putList(new ListTag<FloatTag>("Rotation")
                        .add(new FloatTag("", (float) location.getYaw()))
                        .add(new FloatTag("", (float) location.getPitch())))
                .putBoolean("Invulnerable", true)
                .putString("NameTag", displayName)
                .putString("name", name)
                .putBoolean("Invulnerable", true)
                .putBoolean("npc", true).putFloat("scale", scale);

        final CompoundTag skinTag = new CompoundTag().putString("ModelId", name)
                .putByteArray("Data", skin.getSkinData().data)
                .putString("GeometryName", "geometry." + name)
                .putByteArray("GeometryData", skin.getGeometryData().getBytes())
                .putBoolean("IsTrustedSkin", skin.isTrusted());

        nbt.putCompound("Skin", skinTag);

        return nbt;
    }
}
