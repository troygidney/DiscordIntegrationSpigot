/*
 * Copyright (c) 2020. Troy Gidney
 * All rights reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 * File Last Modified: 8/30/20, 2:30 AM
 * File: Main.java
 * Project: DiscordIntegrationSpigot
 */

package me.pokerman981.DiscordIntegrationSpigot;

import me.pokerman981.DiscordIntegrationSpigot.commands.MCDiscordCommand;
import me.pokerman981.DiscordIntegrationSpigot.listeners.DCGuildLeaveListener;
import me.pokerman981.DiscordIntegrationSpigot.listeners.DCLinkListener;
import me.pokerman981.DiscordIntegrationSpigot.listeners.DCMessageListener;
import me.pokerman981.DiscordIntegrationSpigot.listeners.MCDeluxeChatListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main extends JavaPlugin {

    public static Main instance;
    public static ConfigAccessor config, linkData, accounts;

    public static JDA jda;
    public static List<TextChannel> textChannels = new ArrayList<>();
    public static Map<String, Object> messages;
    public static List<String> rolesToAssignOnLink;
    public static Map<String, Object> donatorRolesToAssign;
    public static List<String> commandsToExecuteOnLink;
    public static List<String> commandsToExecuteOnUnLink;
    public static Guild guild;

    @Override
    public void onEnable() {
        Main.instance = this;

        loadConfigurationFiles();
        Bukkit.getLogger().info("[DiscordBotSpigot] Loaded configuration files.");

        loadConfigurationValues();
        Bukkit.getLogger().info("[DiscordBotSpigot] Loaded configuration values.");

        registerCommands();
        registerListeners();
        Bukkit.getLogger().info("[DiscordBotSpigot] Registered Listeners & Commands.");

        try {
            loadDiscordBot();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
        }
        Bukkit.getLogger().info("[DiscordBotSpigot] Loaded Discord Bot.");

        loadChannels();
    }

    private void loadChannels() { // TODO Make this more dynamic
        String guild = Validate.notNull(Main.config.getConfig().getConfigurationSection("guilds").getString("guild"), "Unable to find guild id"),
                global = Validate.notNull(Main.config.getConfig().getConfigurationSection("channels").getString("global-channel"), "Unable to find global channel id"),
                staff = Validate.notNull(Main.config.getConfig().getConfigurationSection("channels").getString("staff-channel"), "Unable to find staff channel id");

        Guild loadGuild = Validate.notNull(Main.jda.getGuildById(guild), "Failed to find specified guild");
        TextChannel loadGlobalChannel = (TextChannel) Validate.notNull(Main.jda.getGuildChannelById(ChannelType.TEXT, global), "Failed to find specified global channel");
        TextChannel loadStaffChannel = (TextChannel) Validate.notNull(Main.jda.getGuildChannelById(ChannelType.TEXT, staff), "Failed to find specified staff channel");

        Bukkit.getLogger().info("[DiscordBotSpigot] Loaded guild " + loadGuild.getName());
        Bukkit.getLogger().info("[DiscordBotSpigot] Loaded global channel: " + loadGlobalChannel.getName() + " (" + global + ")");
        Bukkit.getLogger().info("[DiscordBotSpigot] Loaded staff channel: " + loadStaffChannel.getName() + " (" + staff + ")");

        Main.guild = loadGuild;
        Main.textChannels.add(loadGlobalChannel);
        Main.textChannels.add(loadStaffChannel);
    }

    private void loadDiscordBot() throws LoginException, InterruptedException {
        String token = Main.config.getConfig().getString("token");
        Validate.notNull(token);

        Main.jda = JDABuilder.createDefault(token)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setEnabledIntents(Arrays.stream(GatewayIntent.values()).collect(Collectors.toList()))
                .addEventListeners(
                        new DCMessageListener(),
                        new DCLinkListener(),
                        new DCGuildLeaveListener())
                .build().awaitStatus(JDA.Status.CONNECTED);
    }

    private void registerListeners() {
        Bukkit.getServer().getPluginManager().registerEvents(new MCDeluxeChatListener(), instance);
    }

    private void registerCommands() {
        this.getCommand("discord").setExecutor(new MCDiscordCommand());
    }

    private void loadConfigurationFiles() {
        Main.config = new ConfigAccessor(Main.instance, "config.yml");
        Main.linkData = new ConfigAccessor(Main.instance, "linkData.yml");
        Main.accounts = new ConfigAccessor(Main.instance, "accounts.yml");

        Main.config.saveDefaultConfig();
        Main.linkData.saveDefaultConfig();
        Main.accounts.saveDefaultConfig();
    }

    private void loadConfigurationValues() {
        messages = Validate.notNull(Main.config.getConfig().getConfigurationSection("messages").getValues(false));
        rolesToAssignOnLink = Validate.notNull(Main.config.getConfig().getStringList("roles-to-assign-on-link"));
        donatorRolesToAssign = Validate.notNull(Main.config.getConfig().getConfigurationSection("donator-roles-to-assign").getValues(false));
        commandsToExecuteOnLink = Validate.notNull(Main.config.getConfig().getStringList("commands-to-execute-on-link"));
        commandsToExecuteOnUnLink = Validate.notNull(Main.config.getConfig().getStringList("commands-to-execute-on-unlink"));
    }

    @Override
    public void onDisable() {
        Main.jda.getHttpClient().connectionPool().evictAll();
        Main.jda.getHttpClient().dispatcher().executorService().shutdown();
        Main.jda.shutdown();

        Bukkit.getLogger().info("[DiscordBotSpigot] Shutting down discord bot");
    }

}

