/*
 *            This file is part of  LibelulaProtectionBlocks.
 *
 *   LibelulaProtectionBlocks is free software: you can redistribute it and/or 
 *  modify it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *   LibelulaProtectionBlocks is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with  LibelulaProtectionBlocks. 
 *  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package me.libelula.pb;

import com.sk89q.worldedit.BlockVector;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class YamlUtils {

    public static ConfigurationSection getSection(Location loc) {
        ConfigurationSection result
                = new YamlConfiguration().createSection("location");
        result.set("x", loc.getBlockX());
        result.set("y", loc.getBlockY());
        result.set("z", loc.getBlockZ());
        result.set("world", loc.getWorld().getName());
        return result;
    }

    public static ConfigurationSection getSection(BlockVector bv) {
        ConfigurationSection result
                = new YamlConfiguration().createSection("blockVector");
        result.set("x", bv.getBlockX());
        result.set("y", bv.getBlockY());
        result.set("z", bv.getBlockZ());
        return result;
    }

    public static Location getLocation(ConfigurationSection cs, Server server) {
        Location loc = null;
        String worldName = cs.getString("world");
        World world = server.getWorld(worldName);
        if (world != null) {
            loc = new Location(world, cs.getInt("x"),
                    cs.getInt("y"), cs.getInt("z"));
        }
        return loc;
    }

    public static BlockVector getBlockVector(ConfigurationSection cs) {
        BlockVector bv = null;
        if (cs != null) {
            bv = new BlockVector(cs.getInt("x"), cs.getInt("y"), cs.getInt("z"));
        }
        return bv;
    }
}
