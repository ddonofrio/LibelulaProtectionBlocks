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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ProtectionBlock implements Comparable<ProtectionBlock> {

    private final Main plugin;
    private boolean hidden;
    private boolean fence;
    private Location location;
    private UUID uuid;
    private ItemStack is;
    private UUID playerUUID;
    private int sizeX;
    private int sizeY;
    private int sizeZ;
    private BlockVector min;
    private BlockVector max;
    private String playerName;
    private List<String> loreText;
    private String name;
    private ProtectedCuboidRegion pcr;
    private World world;
    private String pcrId;

    private final TextManager tm;

    public ProtectionBlock(Main plugin) {
        this.plugin = plugin;
        uuid = UUID.randomUUID();
        tm = plugin.tm;
    }

    @Override
    public int compareTo(ProtectionBlock o) {
        return o.getUuid().compareTo(uuid);
    }

    public void setFence(boolean active) {
        if (!active) {
            if (loreText.remove("+FENCE")) {
                setLoreText(loreText);
            }
        } else {
            loreText.add("+FENCE");
            setLoreText(loreText);
        }
        fence = active;
    }

    public void setHiden(boolean hide) {
        this.hidden = hide;
    }

    public void setLocation(Location location) {
        this.location = location;
        if (location != null) {
            setBlockVectors();
            world = location.getWorld();
            if (this.pcr == null) {
                this.pcr = plugin.getWG().getProtectedRegion(this);
            }
        } else {
            max = null;
            min = null;
            pcr = null;
            world = null;
        }
    }

    public Location getLocation() {
        return location;
    }

    public void setPcr(ProtectedCuboidRegion pcr) {
        this.pcr = pcr;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isPlaced() {
        return location != null;
    }

    public void setItemStack(ItemStack itemStack) {
        this.is = itemStack;
    }

    public ItemStack getItemStack() {
        return is;
    }

    public Material getMaterial() {
        return is.getType();
    }

    public void setPlayerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(playerUUID);
    }

    public void setSizeX(int sizeX) {
        this.sizeX = sizeX;
    }

    public void setSizeY(int sizeY) {
        this.sizeY = sizeY;
    }

    public void setSizeZ(int sizeZ) {
        this.sizeZ = sizeZ;
    }

    public int getSizeX() {
        return sizeX;
    }

    public int getSizeY() {
        return sizeY;
    }

    public int getSizeZ() {
        return sizeZ;
    }

    private void setBlockVectors() {
        int minX = location.getBlockX() - ((sizeX - 1) / 2);
        int minY = location.getBlockY() - ((sizeY - 1) / 2);
        int minZ = location.getBlockZ() - ((sizeZ - 1) / 2);
        int maxX = location.getBlockX() + ((sizeX - 1) / 2);
        int maxY = location.getBlockY() + ((sizeY - 1) / 2);
        int maxZ = location.getBlockZ() + ((sizeZ - 1) / 2);
        if (minY < 0) {
            minY = 0;
        }
        if (maxY > 255) {
            maxY = 255;
        }
        this.min = new BlockVector(minX, minY, minZ);
        this.max = new BlockVector(maxX, maxY, maxZ);

    }

    public String getRegionName() {
        String result;
        if (pcr != null) {
            result = pcr.getId();
        } else if (pcrId != null) {
            result = pcrId;
        } else {
            result = "lpb-" + location.getBlockX() + "x"
                    + location.getBlockY() + "y"
                    + location.getBlockZ() + "z";
        }
        return result;
    }

    public BlockVector getMin() {
        return min;
    }

    public BlockVector getMax() {
        return max;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public List<String> getLoreText() {
        return loreText;
    }

    public void setLoreText(List<String> loreText) {
        this.loreText = loreText;
        if (is != null) {
            ItemMeta dtMeta = is.getItemMeta();
            dtMeta.setLore(loreText);
            is.setItemMeta(dtMeta);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public ProtectedCuboidRegion getPcr() {
        return pcr;
    }

    public World getWorld() {
        return world;
    }

    public boolean isHidden() {
        return hidden;
    }

    public List<String> getInfo() {
        List<String> result = new ArrayList<>();
        if (location != null) {
            result.add(tm.getText("pb_info_header"));
            result.add(tm.getText("pb_info_name", name));
            result.add(tm.getText("pb_info_creator_text", loreText.get(0)));
            result.add(tm.getText("region_info_header", loreText.get(0)));
            result.add(tm.getText("region_info_title", getRegionName(),
                    "" + pcr.getPriority()));
            result.add(tm.getText("region_info_flags", pcr.getFlags().toString()));
            if (!pcr.getOwners().getPlayerDomain().getPlayers().isEmpty()) {
                result.add(tm.getText("region_info_owners",
                        pcr.getOwners().getPlayers()));
            } else {
                result.add(tm.getText("region_info_owners",
                        ChatColor.ITALIC + tm.getText("no_players")));
            }
            if (!pcr.getMembers().getPlayers().isEmpty()) {
                result.add(tm.getText("region_info_members",
                        pcr.getMembers().getPlayers()));
            } else {
                result.add(tm.getText("region_info_members",
                        ChatColor.ITALIC + tm.getText("no_players")));
            }
            result.add(tm.getText("region_info_bounds",
                    min.getBlockX(), min.getBlockY(), min.getBlockZ(),
                    max.getBlockX(), max.getBlockY(), max.getBlockZ()
            ));
        }
        return result;
    }

    public void drawFence() {
        int tic = 0;
        for (int X = min.getBlockX(); X <= max.getBlockX(); X++) {
            for (int Z = min.getBlockZ(); Z <= max.getBlockZ(); Z++) {
                if (X == min.getBlockX() || X == max.getBlockX()
                        || Z == min.getBlockZ() || Z == max.getBlockZ()) {
                    final Location loc = new Location(world, X, location.getBlockY(), Z);
                    final Location locMax = new Location(world, X, location.getBlockY() + 1, Z);
                    tic++;
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (plugin.pm.fenceCanReplace(loc.getBlock().getType())) {
                                loc.getBlock().setType(Material.FENCE);
                            } else if (plugin.pm.fenceCanReplace(
                                    locMax.getBlock().getType())) {
                                locMax.getBlock().setType(Material.FENCE);
                            }
                        }
                    }, tic);

                }
            }
        }
    }

    public boolean hasFence() {
        return fence;
    }

    public static boolean validateMaterial(Material mat) {
        return !(!mat.isBlock() || !mat.isSolid()
                || mat.hasGravity() || mat.isEdible()
                || mat == Material.AIR
                || mat == Material.DIRT
                || mat == Material.GRASS
                || mat == Material.ICE
                || mat == Material.SNOW_BLOCK
                || mat == Material.CACTUS
                || mat == Material.PISTON_BASE
                || mat == Material.PISTON_EXTENSION
                || mat == Material.PISTON_MOVING_PIECE
                || mat == Material.FURNACE
                || mat == Material.MYCEL
                || mat == Material.LEAVES
                || mat == Material.LEAVES_2
                || mat == Material.IRON_PLATE
                || mat == Material.GOLD_PLATE
                || mat == Material.SPONGE
                || mat == Material.TNT);
    }

    @SuppressWarnings("deprecation")
    public ConfigurationSection getConfigurationSection() {
        ConfigurationSection result
                = new YamlConfiguration().createSection(uuid.toString());
        result.set("name", name);
        result.set("hidden", hidden);
        result.set("fence", fence);
        if (location != null) {
            result.set("placed", YamlUtils.getSection(location));
        }

        if (uuid != null) {
            result.set("uuid", uuid.toString());
        }
        if (is != null) {
            result.set("item.material", is.getType().name());
            result.set("item.data", is.getData().getData());
            if (loreText != null) {
                result.set("item.lore", loreText);
            }
        }

        if (playerUUID != null) {
            result.set("owner.uuid", playerUUID.toString());
        }

        if (playerName != null) {
            result.set("owner.name", playerName);
        }

        result.set("size.X", sizeX);
        result.set("size.Y", sizeY);
        result.set("size.Z", sizeZ);

        if (min != null) {
            result.set("bounds.min", YamlUtils.getSection(min));
        }

        if (max != null) {
            result.set("bounds.max", YamlUtils.getSection(max));
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    public void load(ConfigurationSection cs) {
        name = cs.getString("name");
        hidden = cs.getBoolean("hidden");
        fence = cs.getBoolean("fence");
        String uuidString = cs.getString("uuid");
        String ownerUuidString = cs.getString("owner.uuid");
        if (ownerUuidString != null) {
            playerUUID = UUID.fromString(ownerUuidString);
        }
        playerName = cs.getString("owner.name");
        sizeX = cs.getInt("size.X");
        sizeY = cs.getInt("size.Y");
        sizeZ = cs.getInt("size.Z");
        min = YamlUtils.getBlockVector(cs.getConfigurationSection("bounds.min"));
        max = YamlUtils.getBlockVector(cs.getConfigurationSection("bounds.max"));
        if (uuidString != null) {
            uuid = UUID.fromString(uuidString);
        }
        if (cs.contains("placed")) {
            setLocation(YamlUtils.getLocation(cs.getConfigurationSection("placed"),
                    plugin.getServer()));
        }
        Material mat = Material.getMaterial(cs.getString("item.material"));
        if (mat != null) {
            ItemStack isAux = new ItemStack(mat, 1, (short) 0,
                    (byte) cs.getInt("item.data"));
            this.is = isAux;
            setLoreText(cs.getStringList("item.lore"));
            is.getItemMeta().setDisplayName("name");
        }
    }

    public void setPcrId(String pcrID) {
        this.pcrId = pcrID;
    }

    public void removeRegion() {
        if (!hidden && location != null) {
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    location.getBlock().setType(Material.AIR);
                }
            });
        }
        if (pcr != null) {
            plugin.getWG().removeRegion(this);
        }
    }
}
