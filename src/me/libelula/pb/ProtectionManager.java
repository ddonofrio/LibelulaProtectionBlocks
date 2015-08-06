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

import com.sk89q.worldguard.protection.flags.DefaultFlag;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

/**
 *
 * @author Diego D'Onofrio <ddonofrio@member.fsf.org>
 */
public class ProtectionManager {

    private class LocationComparator implements Comparator<Location> {

        @Override
        public int compare(Location o1, Location o2) {
            int resp;
            resp = o1.getWorld().getUID().compareTo(o2.getWorld().getUID());
            if (resp == 0) {
                resp = o1.getBlockX() - o2.getBlockX();
                if (resp == 0) {
                    resp = o1.getBlockY() - o2.getBlockY();
                    if (resp == 0) {
                        resp = o1.getBlockZ() - o2.getBlockZ();
                    }
                }
            }
            return resp;
        }
    }

    private final Main plugin;
    private final TextManager tm;
    private final TreeSet<Material> materialsCache;
    private final TreeMap<UUID, ProtectionBlock> uuidsCache;
    private final TreeSet<ProtectionBlock> createdBlocks;
    private final TreeMap<Location, ProtectionBlock> placedBlocks;
    private final TreeMap<UUID, TreeSet<ProtectionBlock>> playersBlocks;
    private final ReentrantLock _pb_mutex;
    private final ItemStack air;
    private final TreeMap<String, Integer> permissions;
    private final TreeSet<Material> fenceReplaces;
    private final File pbFile;
    private final TreeSet<String> configurableFlags;

    public ProtectionManager(Main plugin) {
        this.plugin = plugin;
        tm = plugin.tm;
        materialsCache = new TreeSet<>();
        fenceReplaces = new TreeSet<>();
        createdBlocks = new TreeSet<>();
        placedBlocks = new TreeMap<>(new LocationComparator());
        playersBlocks = new TreeMap<>();
        permissions = new TreeMap<>();
        uuidsCache = new TreeMap<>();
        configurableFlags = new TreeSet<>();
        _pb_mutex = new ReentrantLock();
        air = new ItemStack(Material.AIR);
        pbFile = new File(plugin.getDataFolder(), "pb.yml");
    }

    public void initialize() {
        load();
    }

    public void addFenceFlag(Player player) {
        ItemStack itemInHand = player.getItemInHand();
        ProtectionBlock pb = getPB(itemInHand);
        if (pb != null) {
            if (pb.hasFence()) {
                plugin.sendMessage(player,
                        ChatColor.RED + tm.getText("already_fence"));
            } else {
                pb.setFence(true);
                player.setItemInHand(pb.getItemStack());
            }
        } else {
            plugin.sendMessage(player, ChatColor.RED + tm.getText("block_not_pb"));
        }
    }

    public boolean createProtectionBlock(Player player,
            int maxX, int maxY, int maxZ) {
        boolean result = true;
        ItemStack itemInHand = player.getItemInHand();
        if (ProtectionBlock.validateMaterial(itemInHand.getType())) {
            if (itemInHand.getAmount() != 1) {
                plugin.sendMessage(player, ChatColor.RED + tm.getText("only_one_solid_block"));
            } else {
                if (isPB(itemInHand)) {
                    plugin.sendMessage(player, ChatColor.RED + tm.getText("block_already_pb"));
                } else {
                    ProtectionBlock pb = generateBlock(itemInHand.getType(),
                            itemInHand.getData(), player.getName(), maxX, maxY, maxZ);
                    player.setItemInHand(pb.getItemStack());
                    plugin.sendMessage(player, tm.getText("protection_block_created"));
                }
            }
        } else {
            plugin.sendMessage(player, ChatColor.RED + tm.getText("not_proper_material"));
            plugin.sendMessage(player, tm.getText("must_be_solid_blocks"));
            result = false;
        }

        return result;
    }

    @SuppressWarnings("deprecation")
    public ProtectionBlock generateBlock(Material material, MaterialData materialData,
            String ownerName, int maxX, int maxY, int maxZ) {
        ProtectionBlock pb = new ProtectionBlock(plugin);
        ItemStack is;
        if (materialData != null) {
            is = new ItemStack(material, 1, (short) 0, materialData.getData());
        } else {
            is = new ItemStack(material, 1);
        }
        is.setData(materialData);
        ItemMeta dtMeta = is.getItemMeta();
        dtMeta.setDisplayName(tm.getText("protection_block_name",
                Integer.toString(maxX),
                Integer.toString(maxY),
                Integer.toString(maxZ)));
        List<String> loreText = new ArrayList<>();
        loreText.add(tm.getText("created_by", ownerName));
        loreText.add(pb.getUuid().toString().substring(0, 18));
        loreText.add(pb.getUuid().toString().substring(19));
        pb.setName(dtMeta.getDisplayName());
        pb.setLoreText(loreText);
        dtMeta.setLore(loreText);
        is.setItemMeta(dtMeta);
        pb.setItemStack(is);
        pb.setSizeX(maxX);
        pb.setSizeY(maxY);
        pb.setSizeZ(maxZ);
        _pb_mutex.lock();
        try {
            materialsCache.add(pb.getMaterial());
            createdBlocks.add(pb);
            uuidsCache.put(pb.getUuid(), pb);
        } finally {
            _pb_mutex.unlock();
        }

        return pb;
    }

    private void revertPlacedPb(final ProtectionBlock pb, final BlockPlaceEvent e) {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                e.getPlayer().setItemInHand(pb.getItemStack());
                e.getBlock().setType(Material.AIR);
                pb.setLocation(null);
            }
        });
    }

    public void placePb(final BlockPlaceEvent e) {
        e.getPlayer().setItemInHand(air);
        final List<String> lore = e.getItemInHand().getItemMeta().getLore();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                String uuidString = lore.get(1).concat("-")
                        .concat(lore.get(2));
                ProtectionBlock pb = getPb(UUID.fromString(uuidString));
                pb.setPlayerUUID(e.getPlayer().getUniqueId());
                pb.setPlayerName(e.getPlayer().getName());
                pb.setLocation(e.getBlock().getLocation());
                if (plugin.getWG().overlapsUnownedRegion(pb.getPcr(),
                        e.getPlayer())) {
                    plugin.sendMessage(e.getPlayer(), ChatColor.RED
                            + tm.getText("overlaps"));
                    revertPlacedPb(pb, e);
                } else if (!e.getPlayer().hasPermission("pb.place")) {
                    plugin.sendMessage(e.getPlayer(), ChatColor.RED
                            + tm.getText("not_permission_active_pb"));
                    revertPlacedPb(pb, e);
                } else if (!e.getPlayer().hasPermission("pb.protection.unlimited")
                        && playersBlocks.get(e.getPlayer().getUniqueId()) != null) {
                    int playerBlocks = playersBlocks.get(e.getPlayer().getUniqueId()).size();
                    if (getMaxProtections(e.getPlayer()) > playerBlocks) {
                        placePb(pb, e);
                    } else {
                        plugin.sendMessage(e.getPlayer(), ChatColor.RED
                                + tm.getText("over_pb_limit"));
                        revertPlacedPb(pb, e);
                    }
                } else {
                    placePb(pb, e);
                }
            }
        });
    }

    private void placePb(ProtectionBlock pb, BlockPlaceEvent e) {
        ajustPriority(pb);
        _pb_mutex.lock();
        try {
            createdBlocks.remove(pb);
            placedBlocks.put(e.getBlock().getLocation(), pb);
            TreeSet<ProtectionBlock> pbs
                    = playersBlocks.get(e.getPlayer().getUniqueId());
            if (pbs == null) {
                pbs = new TreeSet<>();
            }
            pbs.add(pb);
            _pb_mutex.lock();
            try {
                playersBlocks.remove(e.getPlayer().getUniqueId());
                playersBlocks.put(e.getPlayer().getUniqueId(), pbs);
            } finally {
                _pb_mutex.unlock();
            }
            generateWgRegion(pb);
        } finally {
            _pb_mutex.unlock();
        }
        if (pb.hasFence()) {
            pb.drawFence();
            pb.setFence(false);
        }
    }

    @SuppressWarnings("deprecation")
    public void breakPb(final BlockBreakEvent e) {
        e.setExpToDrop(0);

        final ProtectionBlock pb = placedBlocks.get(e.getBlock().getLocation());
        Player player = e.getPlayer();
        if (!pb.getPcr().getOwners().contains(plugin.getWG().wrapPlayer(player))
                && !e.getPlayer().hasPermission("pb.break.others")) {
            plugin.sendMessage(player, ChatColor.RED
                    + tm.getText("not_owned_by_you"));
            Bukkit.getScheduler().runTask(plugin, new Runnable() {
                @Override
                public void run() {
                    e.getBlock().setType(pb.getMaterial());
                    e.getBlock().setData(pb.getItemStack().getData().getData());
                }
            });
        } else {
            HashMap<Integer, ItemStack> remaining
                    = e.getPlayer().getInventory().addItem(pb.getItemStack());
            if (remaining.size() > 0) {
                plugin.sendMessage(player, ChatColor.RED
                        + tm.getText("not_inventory_space"));
                Bukkit.getScheduler().runTask(plugin, new Runnable() {
                    @Override
                    public void run() {
                        e.getBlock().setType(pb.getMaterial());
                        e.getBlock().setData(pb.getItemStack().getData().getData());
                    }
                });
            } else {
                removePb(pb);
            }
        }
    }

    public void removePb(ProtectionBlock pb) {
        _pb_mutex.lock();
        try {
            if (pb.getLocation() != null) {
                plugin.getWG().removeRegion(pb);
                placedBlocks.remove(pb.getLocation());
            }
            if (pb.getPlayerUUID() != null) {
                playersBlocks.get(pb.getPlayerUUID()).remove(pb);
            }
            createdBlocks.add(pb);
        } finally {
            _pb_mutex.unlock();
        }
    }

    public boolean cancelDrop(Location loc) {
        ProtectionBlock pb = placedBlocks.get(loc);
        if (pb == null) {
            return false;
        } else {
            return !pb.isHidden();
        }
    }

    private void generateWgRegion(final ProtectionBlock pb) {
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getWG().createRegion(pb);
            }
        });
    }

    public boolean isPB(Block block) {
        boolean result = false;
        if (!materialsCache.contains(block.getType())) {
            result = false;
        } else {
            if (placedBlocks.containsKey(block.getLocation())) {
                result = true;
            }
        }
        return result;

    }

    public ProtectionBlock getPB(ItemStack is) {
        ProtectionBlock result = null;
        if (materialsCache.contains(is.getType())) {
            List<String> lore = is.getItemMeta().getLore();
            if (lore != null && lore.size() >= 3) {
                String uuidString = lore.get(1).concat("-").concat(lore.get(2));
                result = uuidsCache.get(UUID.fromString(uuidString));
            }
        }
        return result;
    }

    public boolean isPB(ItemStack is) {
        boolean result = false;
        if (!materialsCache.contains(is.getType())) {
            result = false;
        } else {
            List<String> lore = is.getItemMeta().getLore();
            if (lore != null && lore.size() >= 3) {
                String uuidString = lore.get(1).concat("-").concat(lore.get(2));
                result = uuidsCache.containsKey(UUID.fromString(uuidString));
            }
        }
        return result;
    }

    public ProtectionBlock getPb(UUID uuid) {
        return uuidsCache.get(uuid);
    }

    public void ajustPriority(ProtectionBlock pb) {
        _pb_mutex.lock();
        try {
            for (ProtectionBlock oPb : placedBlocks.values()) {
                if (pb.equals(oPb) || oPb.getWorld() == null
                        || !oPb.getWorld().getUID().equals(pb.getWorld().getUID())) {

                    continue;
                }
                if (oPb.getMin().getBlockX() <= pb.getLocation().getBlockX()
                        && oPb.getMin().getBlockY() <= pb.getLocation().getBlockY()
                        && oPb.getMin().getBlockZ() <= pb.getLocation().getBlockZ()
                        && oPb.getMax().getBlockX() >= pb.getLocation().getBlockX()
                        && oPb.getMax().getBlockY() >= pb.getLocation().getBlockY()
                        && oPb.getMax().getBlockZ() >= pb.getLocation().getBlockZ()) {
                    if (pb.getPcr().getPriority() <= oPb.getPcr().getPriority()) {
                        pb.getPcr().setPriority(oPb.getPcr().getPriority() + 1);
                    }
                }
            }
        } finally {
            _pb_mutex.unlock();
        }
    }

    public void addPlayer(final Player player, final String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            ProtectionBlock pb = null;

            @Override
            public void run() {
                _pb_mutex.lock();
                try {
                    for (ProtectionBlock pbO : placedBlocks.values()) {
                        if (pbO.getPlayerUUID().equals(player.getUniqueId())
                                || player.hasPermission("pb.addmember.others")) {
                            if (pbO.getPcr().contains(player.getLocation().getBlockX(),
                                    player.getLocation().getBlockY(),
                                    player.getLocation().getBlockZ())) {
                                pb = pbO;
                                break;
                            }
                        }
                    }
                } finally {
                    _pb_mutex.unlock();
                }
                if (pb == null) {
                    plugin.sendMessage(player, ChatColor.RED
                            + tm.getText("not_in_your_parea"));
                } else {
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getWG().addMemberPlayer(pb.getPcr(), playerName);
                            plugin.sendMessage(player,
                                    tm.getText("player_member_added", playerName));
                        }
                    });
                }
            }
        });
    }

    public void delPlayer(final Player player, final String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            ProtectionBlock pb = null;

            @Override
            public void run() {
                _pb_mutex.lock();
                try {
                    for (ProtectionBlock pbO : placedBlocks.values()) {
                        if (pbO.getPlayerUUID().equals(player.getUniqueId())
                                || player.hasPermission("pb.addmember.others")) {
                            if (pbO.getPcr().contains(player.getLocation().getBlockX(),
                                    player.getLocation().getBlockY(),
                                    player.getLocation().getBlockZ())) {
                                pb = pbO;
                                break;
                            }
                        }
                    }
                } finally {
                    _pb_mutex.unlock();
                }
                if (pb == null) {
                    plugin.sendMessage(player, ChatColor.RED
                            + tm.getText("not_in_your_parea"));
                } else {
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (plugin.getWG().delMemberPlayer(pb.getPcr(), playerName)) {
                                plugin.sendMessage(player,
                                        tm.getText("player_member_removed", playerName));
                            } else {
                                plugin.sendMessage(player,
                                        tm.getText("player_member_not_a_member", playerName));
                            }
                        }
                    });
                }
            }
        });
    }

    public int getMaxProtections(Player player) {
        int result = 1;
        if (player.hasPermission("pb.protection.multiple")) {
            for (String permName : permissions.keySet()) {
                if (player.hasPermission(permName)) {
                    int permValue = permissions.get(permName);
                    if (result < permValue) {
                        result = permValue;
                    }
                }
            }
        }
        return result;
    }

    public Set<Location> getPbLocations() {
        return this.placedBlocks.keySet();
    }

    public boolean isHidden(Location loc) {
        boolean result = true;
        ProtectionBlock pb = placedBlocks.get(loc);
        if (pb != null) {
            result = pb.isHidden();
        }
        return result;
    }

    public void hide(final Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            ProtectionBlock pb = null;

            @Override
            public void run() {
                _pb_mutex.lock();
                try {
                    for (ProtectionBlock pbO : placedBlocks.values()) {
                        if (pbO.getPlayerUUID().equals(player.getUniqueId())
                                || player.hasPermission("pb.hide.others")) {
                            if (pbO.getPcr().contains(player.getLocation().getBlockX(),
                                    player.getLocation().getBlockY(),
                                    player.getLocation().getBlockZ())) {
                                pb = pbO;
                                break;
                            }
                        }
                    }
                } finally {
                    _pb_mutex.unlock();
                }
                if (pb == null) {
                    plugin.sendMessage(player, ChatColor.RED
                            + tm.getText("not_in_your_parea"));
                } else {
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (pb.isHidden()) {
                                plugin.sendMessage(player, ChatColor.RED
                                        + tm.getText("pb_is_already_hidden"));
                            } else {
                                pb.setHiden(true);
                                pb.getLocation().getBlock().setType(Material.AIR);
                            }
                        }
                    });
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    public void unhide(final Player player, final boolean force) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            ProtectionBlock pb = null;

            @Override
            public void run() {
                _pb_mutex.lock();
                try {
                    for (ProtectionBlock pbO : placedBlocks.values()) {
                        if (pbO.getPlayerUUID().equals(player.getUniqueId())
                                || player.hasPermission("pb.hide.others")) {
                            if (pbO.getPcr().contains(player.getLocation().getBlockX(),
                                    player.getLocation().getBlockY(),
                                    player.getLocation().getBlockZ())) {
                                pb = pbO;
                                break;
                            }
                        }
                    }
                } finally {
                    _pb_mutex.unlock();
                }
                if (pb == null) {
                    plugin.sendMessage(player, ChatColor.RED
                            + tm.getText("not_in_your_parea"));
                } else {
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (!pb.isHidden() && !force) {
                                plugin.sendMessage(player, ChatColor.RED
                                        + tm.getText("pb_is_already_visible"));
                                if (player.hasPermission("pb.unhide.force")) {
                                    plugin.sendMessage(player,
                                            tm.getText("use_force_modifier"));
                                }
                            } else {
                                pb.setHiden(false);
                                pb.getLocation().getBlock().setTypeIdAndData(pb.getMaterial().getId(),
                                        pb.getItemStack().getData().getData(), false);
                            }
                        }
                    });
                }
            }
        });
    }

    public void showInfo(final Player player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            ProtectionBlock pb = null;

            @Override
            public void run() {
                _pb_mutex.lock();
                try {
                    for (ProtectionBlock pbO : placedBlocks.values()) {
                        if (pbO.getPlayerUUID().equals(player.getUniqueId())
                                || player.hasPermission("pb.info.others")) {
                            if (pbO.getPcr().contains(player.getLocation().getBlockX(),
                                    player.getLocation().getBlockY(),
                                    player.getLocation().getBlockZ())) {
                                pb = pbO;
                                break;
                            }
                        }
                    }
                } finally {
                    _pb_mutex.unlock();
                }
                if (pb == null) {
                    plugin.sendMessage(player, ChatColor.RED
                            + tm.getText("not_in_your_parea"));
                } else {
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.sendMessage(player, pb.getInfo());
                        }
                    });
                }
            }
        });
    }

    public boolean fenceCanReplace(Material material) {
        return fenceReplaces.contains(material);
    }

    public void save() {
        YamlConfiguration yc = new YamlConfiguration();
        _pb_mutex.lock();
        try {
            for (ProtectionBlock pb : createdBlocks) {
                yc.set("created." + pb.getUuid().toString(), pb.getConfigurationSection());
            }
            for (ProtectionBlock pb : placedBlocks.values()) {
                yc.set("placed." + pb.getRegionName(), pb.getConfigurationSection());
            }
        } finally {
            _pb_mutex.unlock();
        }
        try {
            yc.save(pbFile);
        } catch (IOException ex) {
            plugin.alert(tm.getText("error_saving", ex.getMessage()));
        }
    }

    public void load() {

        permissions.clear();
        fenceReplaces.clear();
        ConfigurationSection cs = plugin.getConfig()
                .getConfigurationSection("protection-multiple");
        if (cs != null) {
            for (String key : cs.getKeys(false)) {
                permissions.put("pb.protection.multiple." + key, cs.getInt(key));
            }

            for (String materialName : plugin.getConfig()
                    .getStringList("flags.fence.replace-materials")) {
                Material mat = Material.getMaterial(materialName);
                if (mat == null) {
                    plugin.alert(tm.getText("fence_flag_invalid_material", materialName));
                } else {
                    fenceReplaces.add(mat);
                }
            }

            configurableFlags.clear();
            for (String defaultFlagName : plugin.getConfig()
                    .getStringList("player.configurable-flags")) {
                configurableFlags.add(defaultFlagName.toLowerCase());
            }

            if (pbFile.exists()) {
                try {
                    YamlConfiguration yc = new YamlConfiguration();
                    yc.load(pbFile);
                    _pb_mutex.lock();
                    try {
                        materialsCache.clear();
                        uuidsCache.clear();
                        createdBlocks.clear();
                        placedBlocks.clear();
                        playersBlocks.clear();
                        if (yc.contains("created")) {
                            for (String uuidString : yc.getConfigurationSection("created")
                                    .getKeys(false)) {
                                ProtectionBlock pb = new ProtectionBlock(plugin);
                                pb.load(yc.getConfigurationSection("created." + uuidString));
                                materialsCache.add(pb.getMaterial());
                                uuidsCache.put(pb.getUuid(), pb);
                                createdBlocks.add(pb);
                                if (pb.getPlayerUUID() != null) {
                                    TreeSet<ProtectionBlock> playerPbs = playersBlocks.get(pb.getPlayerUUID());
                                    if (playerPbs == null) {
                                        playerPbs = new TreeSet<>();
                                    }
                                    playerPbs.add(pb);
                                    playersBlocks.remove(pb.getPlayerUUID());
                                    playersBlocks.put(pb.getPlayerUUID(), playerPbs);
                                }
                            }
                        }
                        if (yc.contains("placed")) {
                            for (String pbLocationString : yc.getConfigurationSection("placed")
                                    .getKeys(false)) {
                                ProtectionBlock pb = new ProtectionBlock(plugin);
                                pb.setPcrId(pbLocationString);
                                pb.load(yc.getConfigurationSection("placed." + pbLocationString));
                                materialsCache.add(pb.getMaterial());
                                uuidsCache.put(pb.getUuid(), pb);
                                placedBlocks.put(pb.getLocation(), pb);
                                TreeSet<ProtectionBlock> playerPbs = playersBlocks.get(pb.getPlayerUUID());
                                if (playerPbs == null) {
                                    playerPbs = new TreeSet<>();
                                }
                                playerPbs.add(pb);
                                playersBlocks.remove(pb.getPlayerUUID());
                                playersBlocks.put(pb.getPlayerUUID(), playerPbs);

                            }
                        }

                    } finally {
                        _pb_mutex.unlock();
                    }

                } catch (IOException | InvalidConfigurationException ex) {
                    plugin.alert(tm.getText("error_loading", ex.getMessage()));
                }
            }
        }
    }

    public TreeSet<String> getConfigurableFlags() {
        return configurableFlags;
    }

    public void setFlag(final Player player, final String flagName, final String value) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            ProtectionBlock pb = null;

            @Override
            public void run() {
                _pb_mutex.lock();
                try {
                    for (ProtectionBlock pbO : placedBlocks.values()) {
                        if (pbO.getPlayerUUID().equals(player.getUniqueId())
                                || player.hasPermission("pb.pb.modifyflags.others")) {
                            if (pbO.getPcr().contains(player.getLocation().getBlockX(),
                                    player.getLocation().getBlockY(),
                                    player.getLocation().getBlockZ())) {
                                pb = pbO;
                                break;
                            }
                        }
                    }
                } finally {
                    _pb_mutex.unlock();
                }
                if (pb == null) {
                    plugin.sendMessage(player, ChatColor.RED
                            + tm.getText("not_in_your_parea"));
                } else {
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            plugin.getWG().setFlag(pb.getPcr(), pb.getWorld(),
                                    DefaultFlag.fuzzyMatchFlag(flagName), value);
                        }
                    });
                }
            }
        });
    }

    public void addPlacedPb(ProtectionBlock pb) {
        _pb_mutex.lock();
        try {
            materialsCache.add(pb.getMaterial());
            uuidsCache.put(pb.getUuid(), pb);
            placedBlocks.put(pb.getLocation(), pb);
            TreeSet<ProtectionBlock> playerPbs = playersBlocks.get(pb.getPlayerUUID());
            if (playerPbs == null) {
                playerPbs = new TreeSet<>();
            }
            playerPbs.add(pb);
            playersBlocks.remove(pb.getPlayerUUID());
            playersBlocks.put(pb.getPlayerUUID(), playerPbs);
        } finally {
            _pb_mutex.unlock();
        }
    }

    public TreeMap<UUID, TreeSet<ProtectionBlock>> getPlayersBlocks() {
        return playersBlocks;
    }

    @SuppressWarnings("deprecation")
    public void removeAllPS(final CommandSender cs, final String playerName) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            ProtectionBlock pb = null;

            @Override
            public void run() {

                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                if (player == null) {
                    plugin.sendMessage(cs, ChatColor.RED
                            + tm.getText("never_played", playerName));
                } else {
                    if (getPbs(player) != null) {
                    plugin.sendMessage(cs, 
                            tm.getText("removing_pbs", playerName));
                        
                        TreeSet<ProtectionBlock> pbs = new TreeSet<>();
                        pbs.addAll(getPbs(player));
                        if (pbs.isEmpty()) {
                            plugin.sendMessage(cs, ChatColor.RED
                                    + tm.getText("has_no_pbs", playerName));
                        } else {
                            for (ProtectionBlock pbL : pbs) {
                                removePb(pbL);
                                if (pbL.isPlaced()) {
                                    pbL.removeRegion();
                                }
                            }
                            plugin.sendMessage(cs, tm.getText("player_pbs_deleted",
                                    pbs.size() + "", playerName));
                        }
                    }
                }
            }
        });

    }

    public TreeSet<ProtectionBlock> getPbs(Player player) {
        return playersBlocks.get(player.getUniqueId());
    }

    public TreeSet<ProtectionBlock> getPbs(OfflinePlayer player) {
        return playersBlocks.get(player.getUniqueId());
    }
}
