package net.eltown.servercore;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.Sound;
import cn.nukkit.network.protocol.PlaySoundPacket;
import cn.nukkit.plugin.PluginBase;
import com.google.common.net.HttpHeaders;
import lombok.Getter;
import lombok.SneakyThrows;
import net.eltown.servercore.commands.administrative.*;
import net.eltown.servercore.commands.crates.CrateCommand;
import net.eltown.servercore.commands.defaults.FixCommand;
import net.eltown.servercore.commands.defaults.PluginsCommand;
import net.eltown.servercore.commands.defaults.SpawnCommand;
import net.eltown.servercore.commands.feature.*;
import net.eltown.servercore.commands.friends.FriendCommand;
import net.eltown.servercore.commands.giftkeys.GiftkeyCommand;
import net.eltown.servercore.commands.giftkeys.RedeemCommand;
import net.eltown.servercore.commands.holograms.HologramCommand;
import net.eltown.servercore.commands.level.LevelCommand;
import net.eltown.servercore.commands.quests.QuestCommand;
import net.eltown.servercore.commands.rewards.DailyRewardCommand;
import net.eltown.servercore.commands.teleportation.*;
import net.eltown.servercore.commands.ticketsystem.TicketCommand;
import net.eltown.servercore.components.api.ServerCoreAPI;
import net.eltown.servercore.components.api.intern.*;
import net.eltown.servercore.components.enchantments.CustomEnchantment;
import net.eltown.servercore.components.entities.HumanNPC;
import net.eltown.servercore.components.entities.ModelEntity;
import net.eltown.servercore.components.forms.FormListener;
import net.eltown.servercore.components.language.Language;
import net.eltown.servercore.components.roleplay.jobs.BankRoleplay;
import net.eltown.servercore.components.roleplay.jobs.CryptoRoleplay;
import net.eltown.servercore.components.roleplay.jobs.JobRoleplay;
import net.eltown.servercore.components.roleplay.other.FeatureRoleplay;
import net.eltown.servercore.components.roleplay.government.RathausRoleplay;
import net.eltown.servercore.components.roleplay.shops.ShopRoleplay;
import net.eltown.servercore.components.tasks.BrianTask;
import net.eltown.servercore.components.tasks.HologramTask;
import net.eltown.servercore.components.tinyrabbit.TinyRabbit;
import net.eltown.servercore.listeners.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
public class ServerCore extends PluginBase {

    private TinyRabbit tinyRabbit;

    private String serverName;

    private HologramAPI hologramAPI;
    private LevelAPI levelAPI;
    private SyncAPI syncAPI;
    private GroupAPI groupAPI;
    private ChestShopAPI chestShopAPI;
    private FurnaceAPI furnaceAPI;
    private QuestAPI questAPI;
    private SettingsAPI settingsAPI;
    private CrateAPI crateAPI;

    private CustomEnchantment customEnchantment;

    private ShopRoleplay shopRoleplay;
    private JobRoleplay jobRoleplay;
    private BankRoleplay bankRoleplay;
    private RathausRoleplay rathausRoleplay;
    private FeatureRoleplay featureRoleplay;
    private CryptoRoleplay cryptoRoleplay;

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
            this.getLogger().info("??aServerCore erfolgreich initialisiert.");
        } catch (final Exception e) {
            e.printStackTrace();
            this.getLogger().error("??4Fehler beim initialisieren des ServerCores.");
        }
    }

    @Override
    public void onDisable() {
        this.getServer().getOnlinePlayers().forEach(((uuid, player) -> this.syncAPI.savePlayer(player)));
    }

    @SneakyThrows
    private void loadPlugin() {
        this.tinyRabbit = new TinyRabbit("localhost", "Core/Server/System[Main]");
        this.tinyRabbit.throwExceptions(true);
        this.serverName = this.getConfig().getString("server-name");
        Language.init(this);

        this.getServer().getPluginManager().registerEvents(new FormListener(), this);
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.getServer().getPluginManager().registerEvents(new NpcListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ChairListener(), this);
        this.getServer().getPluginManager().registerEvents(new LevelListener(this), this);
        this.getServer().getPluginManager().registerEvents(new ModelListener(this), this);
        if (this.serverName.equals("server-1")) this.getServer().getPluginManager().registerEvents(new ChestShopListener(this), this);
        this.getServer().getPluginManager().registerEvents(new FurnaceListener(this), this);
        this.getServer().getPluginManager().registerEvents(new QuestListener(this), this);
        if (this.serverName.equals("server-2") || this.serverName.equals("server-3"))
            this.getServer().getPluginManager().registerEvents(new SpawnProtectionListener(), this);

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
        this.getServer().getCommandMap().register("servercore", new GroupCommand(this));
        this.getServer().getCommandMap().register("servercore", new HealCommand(this));
        this.getServer().getCommandMap().register("servercore", new ClearCommand(this));
        this.getServer().getCommandMap().register("servercore", new GamemodeCommand(this));
        this.getServer().getCommandMap().register("servercore", new RepairCommand(this));
        this.getServer().getCommandMap().register("servercore", new SetspawnCommand(this));
        this.getServer().getCommandMap().register("servercore", new FlyCommand(this));
        this.getServer().getCommandMap().register("servercore", new ModelCommand(this));
        this.getServer().getCommandMap().register("servercore", new PrintitemCommand(this));

        this.getServer().getCommandMap().register("servercore", new CrateCommand(this));

        this.getServer().getCommandMap().register("servercore", new PluginsCommand(this));
        this.getServer().getCommandMap().register("servercore", new SpawnCommand(this));

        this.getServer().getCommandMap().register("servercore", new ChestshopCommand(this));
        this.getServer().getCommandMap().register("servercore", new SettingsCommand(this));
        this.getServer().getCommandMap().register("servercore", new SignCommand(this));
        this.getServer().getCommandMap().register("servercore", new StartCommand(this));
        this.getServer().getCommandMap().register("servercore", new VoteCommand(this));

        this.getServer().getCommandMap().register("servercore", new FriendCommand(this));

        this.getServer().getCommandMap().register("servercore", new GiftkeyCommand(this));
        this.getServer().getCommandMap().register("servercore", new RedeemCommand(this));

        this.getServer().getCommandMap().register("servercore", new HologramCommand(this));

        this.getServer().getCommandMap().register("servercore", new LevelCommand(this));

        this.getServer().getCommandMap().register("servercore", new QuestCommand(this));

        this.getServer().getCommandMap().register("servercore", new DailyRewardCommand(this));

        this.getServer().getCommandMap().register("servercore", new NpcCommand(this));

        this.getServer().getCommandMap().register("servercore", new CbCommand(this));
        this.getServer().getCommandMap().register("servercore", new FwCommand(this));
        this.getServer().getCommandMap().register("servercore", new HomeCommand(this));
        this.getServer().getCommandMap().register("servercore", new NtCommand(this));
        this.getServer().getCommandMap().register("servercore", new TeleportCommand(this));
        this.getServer().getCommandMap().register("servercore", new WarpCommand(this));
        this.getServer().getCommandMap().register("servercore", new TpaCommand(this));
        this.getServer().getCommandMap().register("servercore", new TpacceptCommand(this));

        this.getServer().getCommandMap().register("servercore", new TicketCommand(this));
        if (this.serverName.equals("server-2") || this.serverName.equals("server-3"))
            this.getServer().getCommandMap().register("servercore", new SetSpawnProtectionCommand(this));

        this.getServer().getCommandMap().register("servercore", new FixCommand(this));

        this.hologramAPI = new HologramAPI(this);
        this.levelAPI = new LevelAPI(this);
        this.syncAPI = new SyncAPI(this);
        this.groupAPI = new GroupAPI(this);
        if (this.serverName.equals("server-1")) this.chestShopAPI = new ChestShopAPI(this);
        this.furnaceAPI = new FurnaceAPI(this);
        this.questAPI = new QuestAPI(this);
        this.settingsAPI = new SettingsAPI(this);
        this.crateAPI = new CrateAPI(this);

        this.customEnchantment = new CustomEnchantment(this);

        this.shopRoleplay = new ShopRoleplay(this);
        this.jobRoleplay = new JobRoleplay(this);
        this.bankRoleplay = new BankRoleplay(this);
        if (this.getServerName().equals("server-1")) this.rathausRoleplay = new RathausRoleplay(this);
        this.featureRoleplay = new FeatureRoleplay(this);
        this.cryptoRoleplay = new CryptoRoleplay(this);

        if (this.getServerName().equals("server-1")) this.getServer().getScheduler().scheduleRepeatingTask(new BrianTask(this), 40, true);
        if (this.getServerName().equals("server-1")) this.getServer().getScheduler().scheduleRepeatingTask(new HologramTask(this), 1200, true);

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

    public String createNumberId(final int i) {
        final String chars = "1234567890";
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

    public void playSound(final Player player, final Sound sound) {
        final PlaySoundPacket packet = new PlaySoundPacket();
        packet.name = sound.getSound();
        packet.x = new Double(player.getLocation().getX()).intValue();
        packet.y = new Double(player.getLocation().getY()).intValue();
        packet.z = new Double(player.getLocation().getZ()).intValue();
        packet.volume = 1.0F;
        packet.pitch = 1.0F;
        player.dataPacket(packet);
    }

    public void playSound(final Player player, final Sound sound, final float volume, final float pitch) {
        final PlaySoundPacket packet = new PlaySoundPacket();
        packet.name = sound.getSound();
        packet.x = new Double(player.getLocation().getX()).intValue();
        packet.y = new Double(player.getLocation().getY()).intValue();
        packet.z = new Double(player.getLocation().getZ()).intValue();
        packet.volume = volume;
        packet.pitch = pitch;
        player.dataPacket(packet);
    }

    private final String getURL = "https://minecraftpocket-servers.com/api/?object=votes&element=claim&key=1UdYRD3CmGvbFu6A8Qs4qtyQZW2vsxV6WK&username=";
    private final String setURL = "https://minecraftpocket-servers.com/api/?action=post&object=votes&element=claim&key=1UdYRD3CmGvbFu6A8Qs4qtyQZW2vsxV6WK&username=";

    public void getVote(final String player, final Consumer<String> callback) {
        CompletableFuture.runAsync(() -> {
            try {
                final CloseableHttpClient httpClient = HttpClients.createDefault();
                final HttpGet request = new HttpGet(this.getURL + player.replace(" ", "%20"));

                request.addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9) Gecko/2008052906 Firefox/3.0");

                CloseableHttpResponse response = httpClient.execute(request);
                final String code = new BufferedReader(new InputStreamReader(response.getEntity().getContent())).readLine();
                callback.accept(code);

                httpClient.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

    public void setVoted(final String player) {
        CompletableFuture.runAsync(() -> {
            try {
                final CloseableHttpClient httpClient = HttpClients.createDefault();

                final HttpPost send = new HttpPost(this.setURL + player.replace(" ", "%20"));
                send.addHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9) Gecko/2008052906 Firefox/3.0");
                httpClient.execute(send);

                httpClient.close();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        });
    }

}
