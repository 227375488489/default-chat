package dev.defaultchat;

import dev.defaultchat.chat.ChatService;
import dev.defaultchat.command.ChatCommand;
import dev.defaultchat.command.GlobalChatCommand;
import dev.defaultchat.config.Messages;
import dev.defaultchat.config.PluginConfig;
import dev.defaultchat.listener.ChatListener;
import dev.defaultchat.listener.PlayerListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public final class DefaultChatPlugin extends JavaPlugin {

    private PluginConfig config;
    private Messages messages;
    private ChatService chat;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!loadConfig()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.chat = new ChatService(this);
        this.chat.start();

        getServer().getPluginManager().registerEvents(new ChatListener(this, chat), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(chat), this);
        register("chat", new ChatCommand(this));
        register("globalchat", new GlobalChatCommand(this));

        long interval = Math.max(30, config.saveIntervalSeconds());
        Bukkit.getAsyncScheduler().runAtFixedRate(this, task -> chat.saveSettings(), interval, interval, TimeUnit.SECONDS);

        getLogger().info("DefaultChat enabled (language: " + config.language() + ").");
    }

    @Override
    public void onDisable() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(this);
        Bukkit.getAsyncScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        if (chat != null) {
            chat.shutdown();
        }
    }

    /**
     * Reloads config.yml and the language file. Returns false when the new configuration is invalid,
     * in which case the previously loaded values stay active.
     */
    public boolean reloadPlugin() {
        if (!loadConfig()) {
            return false;
        }
        if (chat != null) {
            chat.reload();
        }
        return true;
    }

    private boolean loadConfig() {
        try {
            reloadConfig();
            PluginConfig loaded = PluginConfig.load(getConfig());
            Messages loadedMessages = Messages.load(this, loaded.language());
            this.config = loaded;
            this.messages = loadedMessages;
            return true;
        } catch (RuntimeException e) {
            getLogger().log(Level.SEVERE, "Could not load the configuration: " + e.getMessage(), e);
            return false;
        }
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Command /" + name + " is missing from plugin.yml.");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter completer) {
            command.setTabCompleter(completer);
        }
    }

    public PluginConfig config() {
        return config;
    }

    public Messages messages() {
        return messages;
    }

    public ChatService chat() {
        return chat;
    }
}
