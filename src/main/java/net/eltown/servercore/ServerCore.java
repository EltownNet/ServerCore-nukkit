package net.eltown.servercore;

import cn.nukkit.entity.Entity;
import cn.nukkit.plugin.PluginBase;
import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.servercore.commands.defaults.PluginsCommand;
import net.eltown.servercore.commands.npc.NpcCommand;
import net.eltown.servercore.commands.teleportation.HomeCommand;
import net.eltown.servercore.commands.teleportation.TpaCommand;
import net.eltown.servercore.commands.teleportation.TpacceptCommand;
import net.eltown.servercore.commands.teleportation.WarpCommand;
import net.eltown.servercore.commands.ticketsystem.TicketCommand;
import net.eltown.servercore.components.entities.HumanNPC;
import net.eltown.servercore.components.forms.FormListener;
import net.eltown.servercore.components.handlers.NpcHandler;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.tinyrabbit.TinyRabbit;
import net.eltown.servercore.listeners.ChairListener;
import net.eltown.servercore.listeners.EventListener;
import net.eltown.servercore.listeners.NpcListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

@Getter
public class ServerCore extends PluginBase {

    private TinyRabbit tinyRabbit;
    private NpcHandler npcHandler;

    private String serverName;

    @Override
    public void onLoad() {
        Entity.registerEntity(HumanNPC.class.getSimpleName(), HumanNPC.class);
    }

    @Override
    public void onEnable() {
        try {
            this.saveDefaultConfig();
            this.loadPlugin();
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    @SneakyThrows
    private void loadPlugin() {
        this.tinyRabbit = new TinyRabbit("localhost", "Core/Server");
        this.serverName = this.getConfig().getString("server-name");
        this.npcHandler = new NpcHandler(this);
        Language.init(this);

        this.getServer().getPluginManager().registerEvents(new FormListener(), this);
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new NpcListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ChairListener(), this);

        this.getServer().getCommandMap().register("servercore", new PluginsCommand(this));

        this.getServer().getCommandMap().register("servercore", new HomeCommand(this));
        this.getServer().getCommandMap().register("servercore", new WarpCommand(this));
        this.getServer().getCommandMap().register("servercore", new TpaCommand(this));
        this.getServer().getCommandMap().register("servercore", new TpacceptCommand(this));

        this.getServer().getCommandMap().register("servercore", new TicketCommand(this));

        this.getServer().getCommandMap().register("servercore", new NpcCommand(this));
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
