/*
 * Copyright (c) 2020. Troy Gidney
 * All rights reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 *
 * File Last Modified: 8/30/20, 2:30 AM
 * File: DCLinkListener.java
 * Project: DiscordIntegrationSpigot
 */

package me.pokerman981.DiscordIntegrationSpigot.listeners;

import me.pokerman981.DiscordIntegrationSpigot.Main;
import me.pokerman981.DiscordIntegrationSpigot.Utils;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class DCLinkListener extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromGuild()) return;
        if (!event.isFromType(ChannelType.PRIVATE)) return;

        String[] command = event.getMessage().getContentRaw().split(" ");

        if (command.length == 1 && command[0].matches("^\\d+")) {
            int pin = Integer.parseInt(event.getMessage().getContentRaw());
            String userID = event.getAuthor().getId();

            if (Main.accounts.getConfig().contains("accounts." + userID)) {
                return;
            }

            Map<String, Object> linkData = Main.linkData.getConfig()
                    .getConfigurationSection("hash")
                    .getValues(false);

            if (!linkData.containsKey(String.valueOf(pin))) {
                event.getChannel().sendMessage("Unable to find pin!").queue();
                return;
            }

            OfflinePlayer player = Bukkit.getServer().getOfflinePlayer(UUID.fromString((String) linkData.get(String.valueOf(pin))));

            String linkedMessage = ((String) Main.messages.getOrDefault("linked", "Config Error!"))
                    .replaceAll("%username%", player.getName()).replaceAll("%id%", userID);

            if (player.isOnline()) {
                Utils.msg(player.getPlayer(), linkedMessage);
            }

            Pattern pattern = Pattern.compile("(?i)" + '&' + "[0-9A-FK-OR]");
            String s = pattern.matcher(linkedMessage).replaceAll("");

            event.getChannel().sendMessage(s).queue();

            Main.accounts.getConfig().set("accounts." + userID, linkData.get(String.valueOf(pin)));
            Main.accounts.save();

            Main.linkData.getConfig().set("hash." + pin, null);
            Main.linkData.getConfig().set("linkData." + linkData.get(String.valueOf(pin)), null);
            Main.linkData.save();

            Main.rolesToAssignOnLink.forEach(roleID -> Main.guild.addRoleToMember(userID, Objects.requireNonNull(Main.guild.getRoleById(roleID))).queue());

            User user = LuckPermsProvider.get().getUserManager().getUser(UUID.fromString((String) linkData.get(String.valueOf(pin))));
            assert user != null;

            Group group = LuckPermsProvider.get().getGroupManager().getGroup(user.getPrimaryGroup());

            if (Main.donatorRolesToAssign.containsKey(group.getDisplayName())) {
                Main.guild.addRoleToMember(userID, Objects.requireNonNull(Main.guild.getRoleById((String) Main.donatorRolesToAssign.get(group.getDisplayName())))).queue();
            }

            Bukkit.getScheduler()
                    .runTask(Main.instance, () -> Main.commandsToExecuteOnLink
                            .forEach(command1 -> Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command1.replaceAll("%player%", player.getName()))));

        }

    }

}
