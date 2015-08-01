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
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ProtectionStonesImporter {

    private final Main plugin;
    private final Plugin oldPs;
    private final File migrationTest;
    private static final String regionIdRegexp
            = "^ps(-?[0-9]+)x(-?[0-9]+)y(-?[0-9]+)z";
    private final Pattern regex;

    public ProtectionStonesImporter(Main plugin) {
        this.plugin = plugin;
        oldPs = plugin.getServer().getPluginManager().getPlugin("ProtectionStones");
        migrationTest = new File(plugin.getDataFolder(), "migration-done");
        regex = Pattern.compile("-?\\d+");
    }

    public boolean isOldPsActive() {
        boolean result = false;
        if (oldPs != null) {
            result = oldPs.isEnabled();
        }
        return result;
    }

    public void disableOldPs() {
        plugin.getPluginLoader().disablePlugin(oldPs);
    }

    public boolean isImportNeeded() {
        return !migrationTest.exists();
    }

    @SuppressWarnings("deprecation")
    public void importFromOldPS() {
        TreeMap<Integer, Material> materials = new TreeMap<>();
        File psConfigFile = new File("plugins/ProtectionStones/config.yml");
        if (psConfigFile.exists()) {
            FileConfiguration psConfig
                    = YamlConfiguration.loadConfiguration(psConfigFile);
            for (String blockLine : psConfig.getStringList("Blocks")) {
                try {
                    Material material = Material.getMaterial(blockLine.split(" ")[0]);
                    int size = Integer.parseInt(blockLine.split(" ")[1]);
                    materials.put(size, material);
                } catch (Exception ex) {
                    plugin.logTranslated("error_importing_block_list", ex.getMessage());
                }
            }
            for (World world : plugin.getWG().getRegionManagers().keySet()) {
                plugin.logTranslated("importing_from_world", world.getName());
                for (String regionId : plugin.getWG().getRegionsIDs(world)) {
                    if (regionId.matches(regionIdRegexp)) {
                        Matcher m = regex.matcher(regionId);
                        m.find();
                        int x = Integer.parseInt(m.group());
                        m.find();
                        int y = Integer.parseInt(m.group());
                        m.find();
                        int z = Integer.parseInt(m.group());
                        Location psLocation = new Location(world, x, y, z);

                        ProtectionBlock pb = new ProtectionBlock(plugin);

                        ProtectedCuboidRegion pcr = (ProtectedCuboidRegion) plugin.getWG().getPcr(world, regionId);
                        
                        int size = (pcr.getMaximumPoint().getBlockX()
                                - pcr.getMinimumPoint().getBlockX()) + 1;
                        Material mat = materials.get(size);
                        if (mat == null) {
                            mat = Material.COAL_ORE;
                        }
                        ItemStack is = new ItemStack(mat, 1);
                        UUID playerUid = null;
                        String playerName = null;
                        try {
                            playerUid = pcr.getOwners().getUniqueIds().iterator().next();
                        } catch (NoSuchElementException e) {
                            playerName = pcr.getOwners().getPlayers().iterator().next();
                        }
                        if (playerUid != null) {
                            pb.setPlayerUUID(playerUid);
                            pb.setPlayerName(Bukkit.getPlayer(playerUid).getName());
                        } else {
                            pb.setPlayerName(playerName);
                            pb.setPlayerUUID(Bukkit.getOfflinePlayer(playerName).getUniqueId());
                        }
                        pb.setItemStack(is);
                        pb.setPcr(pcr);
                        pb.setSizeX(size);
                        pb.setSizeY(size);
                        pb.setSizeZ(size);
                        List<String> loreText = new ArrayList<>();
                        loreText.add(plugin.tm.getText("created_by", "ProtectionBlocks"));
                        loreText.add(pb.getUuid().toString().substring(0, 18));
                        loreText.add(pb.getUuid().toString().substring(19));
                        pb.setName(plugin.tm.getText("protection_block_name",
                                Integer.toString(size),
                                Integer.toString(size),
                                Integer.toString(size)));
                        pb.setLoreText(loreText);
                        pb.setLocation(psLocation);
                        pb.setHiden(!psLocation.getBlock().getType().equals(mat));
                        plugin.pm.addPlacedPb(pb);
                        plugin.logTranslated("imported_ps_block", regionId);
                    }
                }
            }
            try {
                migrationTest.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(ProtectionStonesImporter.class.getName()).log(Level.SEVERE, null, ex);
            }
            plugin.logTranslated("import_finished");
        }
    }
}
