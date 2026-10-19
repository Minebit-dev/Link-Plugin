package dev.minebit.link;

import co.aikar.commands.PaperCommandManager;
import dev.minebit.link.api.MinebitClient;
import dev.minebit.link.command.HiringCommand;
import dev.minebit.link.config.Settings;
import dev.minebit.link.hook.MinebitExpansion;
import dev.minebit.link.listener.JoinListener;
import dev.minebit.link.manager.AlertManager;
import dev.minebit.link.manager.AnnouncementManager;
import dev.minebit.link.manager.DiscordManager;
import dev.minebit.link.manager.LinkManager;
import dev.minebit.link.manager.RoleManager;
import dev.minebit.link.manager.SoundManager;
import dev.minebit.link.message.AlertMessages;
import dev.minebit.link.message.CommandMessages;
import dev.minebit.link.message.RoleMessages;
import java.io.File;
import lombok.Getter;
import net.bitbylogic.utils.message.MessageUtil;
import net.bitbylogic.utils.message.messages.Messages;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

@Getter
public class MinebitLink extends JavaPlugin {

    private final RoleMessages roleMessages = new RoleMessages();
    private final AlertMessages alertMessages = new AlertMessages();
    private final CommandMessages commandMessages = new CommandMessages();

    private volatile Settings settings;

    private File messagesFolder;
    private RoleManager roleManager;
    private LinkManager linkManager;
    private AlertManager alertManager;
    private AnnouncementManager announcementManager;
    private DiscordManager discordManager;

    private BukkitTask refreshTask;
    private BukkitTask alertTask;
    private BukkitTask announceTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = Settings.load(getConfig(), getLogger());

        MessageUtil.init();
        Messages.registerGroup(roleMessages, alertMessages, commandMessages);
        messagesFolder = new File(getDataFolder(), "messages");
        messagesFolder.mkdirs();
        Messages.initialize(messagesFolder);

        MinebitClient client = new MinebitClient(getPluginMeta().getVersion());
        discordManager = new DiscordManager(this, client);
        discordManager.loadTemplates(settings);
        SoundManager soundManager = new SoundManager();
        alertManager = new AlertManager(this, alertMessages, soundManager);
        announcementManager = new AnnouncementManager(this, roleMessages, soundManager);
        roleManager = new RoleManager(this, client, discordManager, announcementManager);
        linkManager = new LinkManager(this, client, alertManager);

        PaperCommandManager commandManager = new PaperCommandManager(this);
        commandManager.registerCommand(new HiringCommand(this, roleMessages, commandMessages));
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new MinebitExpansion(this).register();
        }

        if (!settings.configured()) {
            getLogger().warning("Set Network-Slug in config.yml, then run /hiring reload.");
        }

        schedule();
    }

    @Override
    public void onDisable() {
        cancelTasks();
    }

    public void reload() {
        reloadConfig();
        settings = Settings.load(getConfig(), getLogger());
        Messages.reload(messagesFolder);
        discordManager.loadTemplates(settings);
        roleManager.reset();
        linkManager.reset();
        cancelTasks();
        schedule();
    }

    private void schedule() {
        Settings current = settings;
        boolean onlineMode = getServer().getOnlineMode();

        if (current.configured() || current.hasKey()) {
            long refreshTicks = current.refreshMinutes() * 60L * 20L;

            refreshTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                roleManager.refresh();
                linkManager.ping(onlineMode);
            }, 20L, refreshTicks);
        }

        if (current.hasKey() && current.staffAlerts()) {
            long pollTicks = current.alertPollSeconds() * 20L;

            alertTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, linkManager::pollApplications, 100L, pollTicks);
        }

        if (current.configured() && current.announce()) {
            long announceTicks = current.announceMinutes() * 60L * 20L;

            announceTask = Bukkit.getScheduler().runTaskTimer(this, announcementManager::announceRoles, announceTicks, announceTicks);
        }
    }

    private void cancelTasks() {
        if (refreshTask != null) {
            refreshTask.cancel();
            refreshTask = null;
        }

        if (alertTask != null) {
            alertTask.cancel();
            alertTask = null;
        }

        if (announceTask != null) {
            announceTask.cancel();
            announceTask = null;
        }
    }

}
