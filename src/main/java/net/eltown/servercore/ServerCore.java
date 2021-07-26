package net.eltown.servercore;

import cn.nukkit.entity.Entity;
import cn.nukkit.plugin.PluginBase;
import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.servercore.commands.administrative.*;
import net.eltown.servercore.commands.defaults.PluginsCommand;
import net.eltown.servercore.commands.giftkeys.GiftkeyCommand;
import net.eltown.servercore.commands.giftkeys.RedeemCommand;
import net.eltown.servercore.commands.level.LevelCommand;
import net.eltown.servercore.commands.npc.NpcCommand;
import net.eltown.servercore.commands.holograms.HologramCommand;
import net.eltown.servercore.commands.teleportation.*;
import net.eltown.servercore.commands.ticketsystem.TicketCommand;
import net.eltown.servercore.components.api.ServerCoreAPI;
import net.eltown.servercore.components.api.intern.LevelAPI;
import net.eltown.servercore.components.enchantments.CustomEnchantment;
import net.eltown.servercore.components.entities.HumanNPC;
import net.eltown.servercore.components.entities.ModelEntity;
import net.eltown.servercore.components.forms.FormListener;
import net.eltown.servercore.components.api.intern.HologramAPI;
import net.eltown.servercore.components.api.intern.NpcAPI;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.jobs.JobRoleplay;
import net.eltown.servercore.components.roleplay.shops.ShopRoleplay;
import net.eltown.servercore.components.tinyrabbit.TinyRabbit;
import net.eltown.servercore.listeners.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

@Getter
public class ServerCore extends PluginBase {

    private TinyRabbit tinyRabbit;

    private String serverName;

    private HologramAPI hologramAPI;
    private NpcAPI npcAPI;
    private LevelAPI levelAPI;

    private CustomEnchantment customEnchantment;

    private ShopRoleplay shopRoleplay;
    private JobRoleplay jobRoleplay;

    @Override
    public void onLoad() {
        Entity.registerEntity(HumanNPC.class.getSimpleName(), HumanNPC.class);
        Entity.registerEntity("ModelEntity", ModelEntity.class);
    }

    @Override
    public void onEnable() {
        try {
            this.saveDefaultConfig();
            this.loadPlugin();
            this.getLogger().info("§aServerCore erfolgreich initialisiert.");
        } catch (final Exception e) {
            e.printStackTrace();
            this.getLogger().error("§4Fehler beim initialisieren des ServerCores.");
        }
    }

    @SneakyThrows
    private void loadPlugin() {
        this.tinyRabbit = new TinyRabbit("localhost", "Core/Server");
        this.tinyRabbit.throwExceptions(true);
        this.serverName = this.getConfig().getString("server-name");
        Language.init(this);

        this.getServer().getPluginManager().registerEvents(new FormListener(), this);
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new NpcListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ChairListener(), this);
        this.getServer().getPluginManager().registerEvents(new HologramListener(this), this);
        this.getServer().getPluginManager().registerEvents(new LevelListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ModelListener(this), this);

        this.getServer().getCommandMap().register("servercore", new EnchantCommand(this));
        this.getServer().getCommandMap().register("servercore", new SpeedCommand(this));
        this.getServer().getCommandMap().register("servercore", new IdCommand(this));
        this.getServer().getCommandMap().register("servercore", new BanCommand(this));
        this.getServer().getCommandMap().register("servercore", new BanlogCommand(this));
        this.getServer().getCommandMap().register("servercore", new UnbanCommand(this));
        this.getServer().getCommandMap().register("servercore", new CheckbanCommand(this));
        this.getServer().getCommandMap().register("servercore", new GetbanCommand(this));
        this.getServer().getCommandMap().register("servercore", new UnbanlogCommand(this));
        this.getServer().getCommandMap().register("servercore", new MuteCommand(this));
        this.getServer().getCommandMap().register("servercore", new MutelogCommand(this));
        this.getServer().getCommandMap().register("servercore", new UnmuteCommand(this));
        this.getServer().getCommandMap().register("servercore", new CheckmuteCommand(this));
        this.getServer().getCommandMap().register("servercore", new GetmuteCommand(this));
        this.getServer().getCommandMap().register("servercore", new UnmutelogCommand(this));
        this.getServer().getCommandMap().register("servercore", new SetgroupCommand(this));
        this.getServer().getCommandMap().register("servercore", new CreategroupCommand(this));
        this.getServer().getCommandMap().register("servercore", new GroupmetaCommand(this));
        this.getServer().getCommandMap().register("servercore", new HealCommand(this));
        this.getServer().getCommandMap().register("servercore", new ClearCommand(this));
        this.getServer().getCommandMap().register("servercore", new GamemodeCommand(this));
        this.getServer().getCommandMap().register("servercore", new RepairCommand(this));
        this.getServer().getCommandMap().register("servercore", new FlyCommand(this));
        this.getServer().getCommandMap().register("servercore", new ModelCommand(this));

        this.getServer().getCommandMap().register("servercore", new PluginsCommand(this));

        this.getServer().getCommandMap().register("servercore", new GiftkeyCommand(this));
        this.getServer().getCommandMap().register("servercore", new RedeemCommand(this));

        this.getServer().getCommandMap().register("servercore", new HologramCommand(this));

        this.getServer().getCommandMap().register("servercore", new LevelCommand(this));

        this.getServer().getCommandMap().register("servercore", new NpcCommand(this));

        this.getServer().getCommandMap().register("servercore", new HomeCommand(this));
        this.getServer().getCommandMap().register("servercore", new TeleportCommand(this));
        this.getServer().getCommandMap().register("servercore", new WarpCommand(this));
        this.getServer().getCommandMap().register("servercore", new TpaCommand(this));
        this.getServer().getCommandMap().register("servercore", new TpacceptCommand(this));

        this.getServer().getCommandMap().register("servercore", new TicketCommand(this));

        this.saveResource("models");

        this.hologramAPI = new HologramAPI(this);
        this.npcAPI = new NpcAPI(this);
        this.levelAPI = new LevelAPI(this);

        this.customEnchantment = new CustomEnchantment(this);

        this.shopRoleplay = new ShopRoleplay(this);
        this.jobRoleplay = new JobRoleplay(this);

        new ServerCoreAPI(this);
    }

    public String createId(final int i) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        final StringBuilder stringBuilder = new StringBuilder();
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

    public String createId(final int i, final String prefix) {
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        final StringBuilder stringBuilder = new StringBuilder(prefix + "-");
        final Random rnd = new Random();
        while (stringBuilder.length() < i) {
            int index = (int) (rnd.nextFloat() * chars.length());
            stringBuilder.append(chars.charAt(index));
        }
        return stringBuilder.toString();
    }

    public String getDate() {
        Date now = new Date();
        DateFormat dateFormat = new SimpleDateFormat("dd.MM.yy HH:mm");
        return dateFormat.format(now);
    }

    public long getDuration(final String unit, final int time) {
        long duration = System.currentTimeMillis();
        switch (unit) {
            case "m":
                duration = duration + time * 60000L;
                break;
            case "h":
                duration = duration + time * 3600000L;
                break;
            case "d":
                duration = duration + time * 86400000L;
                break;
            case "W":
                duration = duration + time * 604800000L;
                break;
            case "M":
                duration = duration + (long) (time * 2.6280E+9);
                break;
            case "Permanent":
                duration = -1;
                break;
        }
        return duration;
    }

    public String getRemainingTimeFuture(final long duration) {
        if (duration == -1L) {
            return "Permanent";
        } else {
            final SimpleDateFormat today = new SimpleDateFormat("dd.MM.yyyy");
            today.format(System.currentTimeMillis());
            final SimpleDateFormat future = new SimpleDateFormat("dd.MM.yyyy");
            future.format(duration);
            final long time = future.getCalendar().getTimeInMillis() - today.getCalendar().getTimeInMillis();
            final int days = (int) (time / 86400000L);
            final int hours = (int) (time / 3600000L % 24L);
            final int minutes = (int) (time / 60000L % 60L);
            String day = "Tage";
            if (days == 1) {
                day = "Tag";
            }

            String hour = "Stunden";
            if (hours == 1) {
                hour = "Stunde";
            }

            String minute = "Minuten";
            if (minutes == 1) {
                minute = "Minute";
            }

            if (minutes < 1 && days == 0 && hours == 0) {
                return "Wenige Augenblicke";
            } else if (hours == 0 && days == 0) {
                return minutes + " " + minute;
            } else {
                return days == 0 ? hours + " " + hour + " " + minutes + " " + minute : days + " " + day + " " + hours + " " + hour + " " + minutes + " " + minute;
            }
        }
    }

}
