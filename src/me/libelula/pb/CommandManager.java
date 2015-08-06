/*
 *            This file is part of LibelulaProtectionBlocks.
 *
 *  LibelulaProtectionBlocks is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  LibelulaProtectionBlocks is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with LibelulaProtectionBlocks. 
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package me.libelula.pb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class CommandManager implements CommandExecutor {

    private final Main plugin;
    private final TextManager tm;

    public CommandManager(Main plugin) {
        this.plugin = plugin;
        tm = plugin.tm;
    }

    public void initialize() throws IOException {
        plugin.saveResource("plugin.yml", true);
        File pluginFile = new File(plugin.getDataFolder(), "plugin.yml");
        YamlConfiguration pluginYml = new YamlConfiguration();
        try {
            pluginYml.load(pluginFile);
        } catch (FileNotFoundException | InvalidConfigurationException ex) {
            // it will never happens.
        }
        for (String commandName : pluginYml.getConfigurationSection("commands")
                .getKeys(false)) {
            plugin.getCommand(commandName).setExecutor(this);
        }
        pluginFile.delete();
    }

    @Override
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] args) {
        Player player;
        String toLowerSubCommand;
        if (cs instanceof Player) {
            player = (Player) cs;
        } else {
            player = null;
        }
        // There is only one command so we assume it is /ps or it aliases.
        if (args.length >= 1) {
            toLowerSubCommand = args[0].toLowerCase();
            switch (toLowerSubCommand) {
                case "help":
                    showHelp(cs, toLowerSubCommand);
                    break;
                case "add":
                    if (args.length < 2) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        if (player == null) {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("ingame_command"));
                        } else {
                            plugin.pm.addPlayer(player, args[1]);
                        }
                    }
                    break;
                case "del":
                case "remove":
                    if (args.length < 2) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        if (player == null) {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("ingame_command"));
                        } else {
                            plugin.pm.delPlayer(player, args[1]);
                        }
                    }
                    break;
                case "hide":
                    if (args.length != 1) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        if (player == null) {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("ingame_command"));
                        } else {
                            if (player.hasPermission("pb.hide")) {
                                plugin.pm.hide(player);
                            } else {
                                plugin.sendMessage(cs, ChatColor.RED
                                        + tm.getText("not_permission_this_command"));
                            }
                        }
                    }
                    break;
                case "unhide":
                    if (args.length != 1 && (args.length != 2)) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        if (player == null) {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("ingame_command"));
                        } else {
                            if (args.length == 2 && !args[1]
                                    .equalsIgnoreCase("force")) {
                                showHelp(cs, toLowerSubCommand);
                            } else if (args.length == 1) {
                                plugin.pm.unhide(player, false);
                            } else {
                                if (player.hasPermission("pb.unhide.force")) {
                                    plugin.pm.unhide(player, true);
                                } else {
                                    plugin.sendMessage(cs, ChatColor.RED
                                            + tm.getText("not_permission_this_command"));
                                }
                            }
                        }
                    }
                    break;
                case "flag":
                    switch (args.length) {
                        case 1:
                            showHelp(cs, toLowerSubCommand);
                            break;
                        case 2:
                            if (!args[1].equalsIgnoreCase("list")) {
                                showHelp(cs, "flag");
                            } else {
                                plugin.sendMessage(cs,
                                        tm.getText("configurable-flags",
                                                plugin.pm.getConfigurableFlags().toString()));
                            }
                            break;
                        default:
                            switch (args[1].toLowerCase()) {
                                case "list":
                                    showHelp(cs, "flag-list");
                                    break;
                                case "del":
                                case "remove":
                                    if (plugin.pm.getConfigurableFlags().contains(args[2])) {
                                        plugin.pm.setFlag(player, args[2], "");
                                        plugin.sendMessage(cs, tm.getText("flag_removed",
                                                args[2].toUpperCase()));
                                    } else {
                                        plugin.sendMessage(cs, ChatColor.RED
                                                + tm.getText("not_configurable_flag"));
                                    }
                                    break;
                                default:
                                    if (player == null) {
                                        plugin.sendMessage(cs, ChatColor.RED
                                                + tm.getText("ingame_command"));
                                    } else {
                                        if (plugin.pm.getConfigurableFlags().contains(args[1])) {
                                            String flagValue = "";
                                            for (int i = 2; i < args.length; i++) {
                                                flagValue = flagValue.concat(args[i] + " ");
                                            }
                                            flagValue = flagValue.substring(0, flagValue.length() - 1);
                                            plugin.pm.setFlag(player, args[1], flagValue);
                                        } else {
                                            plugin.sendMessage(cs, ChatColor.RED
                                                    + tm.getText("not_configurable_flag"));
                                        }
                                    }
                            }

                    }
                    break;
                case "info":
                    if (args.length != 1) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        if (player == null) {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("ingame_command"));
                        } else {
                            plugin.pm.showInfo(player);
                        }
                    }
                    break;
                case "reload":
                    if (args.length != 1) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        plugin.reloadLocalConfig();
                    }
                    break;
                case "create":
                    if (args.length != 2 && args.length != 4) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        if (player == null) {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("ingame_command"));
                        } else if (!player.hasPermission("pb.create")) {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("not_permission_this_command"));
                        } else {
                            int X = 0;
                            int Y = 0;
                            int Z = 0;
                            if (args.length == 2) {
                                try {
                                    X = Integer.parseInt(args[1]);
                                    if ((X & 1) != 0) {
                                        Y = X;
                                        Z = X;
                                        plugin.pm.createProtectionBlock(player, X, Y, Z);
                                    } else {
                                        plugin.sendMessage(cs, ChatColor.RED
                                                + tm.getText("protections_not_an_odd"));
                                    }
                                } catch (NumberFormatException ex) {
                                    plugin.sendMessage(cs, ChatColor.RED
                                            + tm.getText("not_a_number", args[1]));
                                }
                            } else {
                                try {
                                    X = Integer.parseInt(args[1]);
                                } catch (NumberFormatException ex) {
                                    plugin.sendMessage(cs, ChatColor.RED
                                            + tm.getText("not_a_number", args[1]));
                                }
                                try {
                                    Y = Integer.parseInt(args[2]);
                                } catch (NumberFormatException ex) {
                                    plugin.sendMessage(cs, ChatColor.RED
                                            + tm.getText("not_a_number", args[2]));
                                }
                                try {
                                    Z = Integer.parseInt(args[3]);
                                } catch (NumberFormatException ex) {
                                    plugin.sendMessage(cs, ChatColor.RED
                                            + tm.getText("not_a_number", args[3]));
                                }
                                if (X != 0 && Y != 0 && Z != 0) {
                                    if ((X & 1) == 0 || (Y & 1) == 0 || (Z & 1) == 0) {
                                        plugin.sendMessage(cs, ChatColor.RED
                                                + tm.getText("protections_not_an_odd"));
                                    } else {
                                        plugin.pm.createProtectionBlock(player, X, Y, Z);
                                    }
                                }
                            }
                        }
                    }
                    break;
                case "version":
                    if (args.length != 1) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        if (cs.hasPermission("pb.version")) {
                            plugin.sendMessage(cs, plugin.getDescription().getFullName());
                            plugin.sendMessage(cs, "Created by "
                                    + plugin.getDescription().getAuthors().toString());
                        } else {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("not_permission_this_command"));
                        }
                    }
                    break;
                case "remove-all-ps":
                    if (args.length != 2) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        if (cs.hasPermission("pb.remove.all")) {
                            plugin.pm.removeAllPS(cs, args[1]);
                        } else {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("not_permission_this_command"));                            
                        }
                    }
                    break;
                case "+fence":
                    if (args.length != 1) {
                        showHelp(cs, toLowerSubCommand);
                    } else {
                        if (player == null) {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("ingame_command"));
                        } else {
                            if (player.hasPermission("pb.create")) {
                                plugin.pm.addFenceFlag(player);
                            } else {
                                plugin.sendMessage(cs, ChatColor.RED
                                        + tm.getText("not_permission_this_command"));
                            }
                        }
                    }
                    break;
                default:
                    showHelp(cs, null);
                    break;

            }
        } else {
            showHelp(cs, null);
        }
        return true;
    }

    public void showHelp(CommandSender cs, String subcommand) {
        Player player = null;
        if (cs instanceof Player) {
            player = (Player) cs;
        }
        if (subcommand == null) {
            subcommand = "_empty_";
        }
        switch (subcommand) {
            case "_empty_":
            case "help":
                plugin.sendMessage(cs, getPsCommandsText(player));
                break;
            case "add":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_add"));
                break;
            case "del":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_del"));
                break;
            case "hide":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_hide"));
                break;
            case "unhide":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_unhide"));
                break;
            case "flag":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_flag"));                
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_flag_remove"));
            // no break
            case "flag-list":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_flag_list"));
                break;
            case "info":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_info"));
                break;
            case "create":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_create"));
                break;
            case "reload":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_reload"));
                break;
            case "version":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_version"));
                break;
            case "remove-all-ps":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_remove_all"));
                break;
            case "+fence":
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("help_ps_plus_fence"));
                break;
            default:
                plugin.sendMessage(cs, ChatColor.RED
                        + tm.getText("unknown_command"));
                break;
        }

    }

    private List<String> getPsCommandsText(Player player) {
        List<String> result = new ArrayList<>();
        result.add(tm.getText("help_ps_help"));
        result.add(tm.getText("help_ps_add"));
        result.add(tm.getText("help_ps_del"));
        result.add(tm.getText("help_ps_hide"));
        result.add(tm.getText("help_ps_unhide"));
        result.add(tm.getText("help_ps_flag_list"));
        result.add(tm.getText("help_ps_flag"));
        result.add(tm.getText("help_ps_flag_remove"));
        result.add(tm.getText("help_ps_info"));
        if (player == null || player.hasPermission("pb.reload")) {
            result.add(tm.getText("help_ps_reload"));
        }
        if (player == null || player.hasPermission("pb.create")) {
            result.add(tm.getText("help_ps_create"));
        }
        result.add(tm.getText("help_ps_version"));
        if (player == null || player.hasPermission("pb.remove.all")) {
            result.add(tm.getText("help_ps_remove_all"));
        }
        if (player == null || player.hasPermission("pb.modifyflags")) {
            result.add(tm.getText("help_ps_plus_fence"));
        }
        return result;
    }

}
