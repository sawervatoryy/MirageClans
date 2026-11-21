package org.github.sawervatoryy.mirageClans;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.github.sawervatoryy.mirageClans.command.ClanChatCommand;
import org.github.sawervatoryy.mirageClans.command.ClanCommand;
import org.github.sawervatoryy.mirageClans.config.ConfigManager;
import org.github.sawervatoryy.mirageClans.config.Lang;
import org.github.sawervatoryy.mirageClans.config.MenuConfig;
import org.github.sawervatoryy.mirageClans.config.Settings;
import org.github.sawervatoryy.mirageClans.gui.GuiListener;
import org.github.sawervatoryy.mirageClans.gui.GuiManager;
import org.github.sawervatoryy.mirageClans.hook.PapiExpansion;
import org.github.sawervatoryy.mirageClans.hook.PointsHook;
import org.github.sawervatoryy.mirageClans.hook.VaultHook;
import org.github.sawervatoryy.mirageClans.listener.ChatListener;
import org.github.sawervatoryy.mirageClans.listener.FriendlyFireListener;
import org.github.sawervatoryy.mirageClans.listener.PlayerListener;
import org.github.sawervatoryy.mirageClans.listener.QuestListener;
import org.github.sawervatoryy.mirageClans.manager.ChatManager;
import org.github.sawervatoryy.mirageClans.manager.ClanManager;
import org.github.sawervatoryy.mirageClans.manager.GlowManager;
import org.github.sawervatoryy.mirageClans.manager.HomeManager;
import org.github.sawervatoryy.mirageClans.manager.InviteManager;
import org.github.sawervatoryy.mirageClans.manager.LevelManager;
import org.github.sawervatoryy.mirageClans.manager.PromptManager;
import org.github.sawervatoryy.mirageClans.manager.QuestManager;
import org.github.sawervatoryy.mirageClans.manager.StorageManager;
import org.github.sawervatoryy.mirageClans.manager.TreasuryManager;
import org.github.sawervatoryy.mirageClans.storage.ClanRepository;
import org.github.sawervatoryy.mirageClans.storage.Database;

import java.util.logging.Level;

/**
 * MirageClans — клановая система для Paper 1.21.4.
 * Данные кланов грузятся из БД асинхронно; до окончания загрузки команды отвечают «загрузка».
 */
public final class MirageClans extends JavaPlugin {

    private ConfigManager configs;
    private final Settings settings = new Settings();
    private final Lang lang = new Lang();
    private MenuConfig menus;

    private Database database;
    private ClanRepository repository;

    private ClanManager clans;
    private TreasuryManager treasury;
    private LevelManager levels;
    private StorageManager storage;
    private HomeManager homes;
    private QuestManager quests;
    private ChatManager chat;
    private InviteManager invites;
    private PromptManager prompts;
    private GlowManager glow;
    private GuiManager gui;
    private VaultHook vault;
    private PointsHook points;

    private volatile boolean ready;
    private int flushTaskId = -1;

    @Override
    public void onEnable() {
        configs = new ConfigManager(this);
        configs.ensureFiles();
        menus = new MenuConfig(this);
        loadConfigs();

        vault = new VaultHook();
        points = new PointsHook();

        try {
            database = new Database(this, settings.cfg());
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Не удалось подключиться к БД — плагин отключён", ex);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        repository = new ClanRepository(this, database);

        clans = new ClanManager(this);
        treasury = new TreasuryManager(this);
        levels = new LevelManager(this);
        storage = new StorageManager(this);
        homes = new HomeManager(this);
        quests = new QuestManager(this);
        quests.load(configs.load("quests.yml"));
        chat = new ChatManager(this);
        invites = new InviteManager(this);
        prompts = new PromptManager(this);
        glow = new GlowManager(this);
        gui = new GuiManager(this);

        registerCommands();
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new GuiListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new FriendlyFireListener(this), this);
        pm.registerEvents(new QuestListener(this), this);

        if (pm.isPluginEnabled("PlaceholderAPI")) {
            new PapiExpansion(this).register();
        }

        repository.init()
                .thenCompose(v -> repository.loadAll())
                .whenComplete((list, err) -> Bukkit.getScheduler().runTask(this, () -> {
                    if (err != null) {
                        getLogger().log(Level.SEVERE, "Ошибка загрузки данных кланов", err);
                        Bukkit.getPluginManager().disablePlugin(this);
                        return;
                    }
                    clans.registerLoaded(list);
                    repository.pruneHistory(settings.integer("database.history-keep-days"));
                    storage.start();
                    quests.start();
                    glow.start();
                    flushTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> clans.flushDirty(), 600L, 600L).getTaskId();
                    ready = true;
                    getLogger().info("Загружено кланов: " + list.size());
                }));
    }

    @Override
    public void onDisable() {
        ready = false;
        if (flushTaskId != -1) {
            Bukkit.getScheduler().cancelTask(flushTaskId);
        }
        if (storage != null) {
            storage.stop();
            storage.saveAll();
        }
        if (quests != null) {
            quests.stop();
        }
        if (glow != null) {
            glow.stop();
        }
        if (clans != null) {
            clans.saveAll();
        }
        if (repository != null) {
            repository.shutdown();
        }
        if (database != null) {
            database.close();
        }
    }

    private void registerCommands() {
        PluginCommand clan = getCommand("clan");
        if (clan != null) {
            ClanCommand exec = new ClanCommand(this);
            clan.setExecutor(exec);
            clan.setTabCompleter(exec);
        }
        PluginCommand cc = getCommand("cc");
        if (cc != null) {
            cc.setExecutor(new ClanChatCommand(this));
        }
    }

    private void loadConfigs() {
        settings.load(configs.load("config.yml"));
        lang.load(configs.load("lang.yml"));
        menus.load(configs.load("menus.yml"));
    }

    /** Перезагрузка конфигов (БД не переподключается). */
    public void reloadAll() {
        loadConfigs();
        if (quests != null) {
            quests.load(configs.load("quests.yml"));
        }
    }

    public boolean isReady() {
        return ready;
    }

    public Settings settings() {
        return settings;
    }

    public Lang lang() {
        return lang;
    }

    public MenuConfig menus() {
        return menus;
    }

    public ClanRepository repository() {
        return repository;
    }

    public ClanManager clans() {
        return clans;
    }

    public TreasuryManager treasury() {
        return treasury;
    }

    public LevelManager levels() {
        return levels;
    }

    public StorageManager storage() {
        return storage;
    }

    public HomeManager homes() {
        return homes;
    }

    public QuestManager quests() {
        return quests;
    }

    public ChatManager chat() {
        return chat;
    }

    public InviteManager invites() {
        return invites;
    }

    public PromptManager prompts() {
        return prompts;
    }

    public GlowManager glow() {
        return glow;
    }

    public GuiManager gui() {
        return gui;
    }

    public VaultHook vault() {
        return vault;
    }

    public PointsHook points() {
        return points;
    }
}
