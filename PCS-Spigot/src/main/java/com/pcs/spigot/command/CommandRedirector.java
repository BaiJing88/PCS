/*
 * Copyright (c) 2026 Bai_Jing88 (QQ: 1782307393)
 * PCS (Player Credit System) - Minecraft Cross-Server Player Management
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Any derivative work must also be open source and licensed under
 * the same AGPL v3 license. Commercial use is prohibited without
 * explicit permission from the author.
 */

package com.pcs.spigot.command;

import com.pcs.spigot.PCSSpigotPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

/**
 * 命令重定向器 - 将独立命令重定向到主命令
 * 例如: /credit -> /pcs credit
 */
public class CommandRedirector implements CommandExecutor, TabCompleter {

    private final PCSSpigotPlugin plugin;
    private final String subCommand;
    private final String permission;

    public CommandRedirector(PCSSpigotPlugin plugin, String subCommand, String permission) {
        this.plugin = plugin;
        this.subCommand = subCommand;
        this.permission = permission;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 检查权限
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage("§c你没有权限使用此命令！");
            return true;
        }

        // 构建重定向的命令
        String[] redirectedArgs = new String[args.length + 1];
        redirectedArgs[0] = subCommand;
        System.arraycopy(args, 0, redirectedArgs, 1, args.length);

        // 调用主命令处理器
        return plugin.getCommand("pcs").execute(sender, label, redirectedArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(permission)) {
            return new ArrayList<>();
        }

        // 复用PCSCommand的Tab补全逻辑
        PCSCommand pcsCommand = new PCSCommand(plugin);
        return pcsCommand.onTabComplete(sender, command, alias, prependSubCommand(args));
    }

    private String[] prependSubCommand(String[] args) {
        String[] result = new String[args.length + 1];
        result[0] = subCommand;
        System.arraycopy(args, 0, result, 1, args.length);
        return result;
    }
}
