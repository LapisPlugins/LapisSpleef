/*
 *    Copyright 2023 Benjamin Martin
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package net.lapismc.spleef.players;

import net.lapismc.spleef.LapisSpleef;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * A class to store basic player info and methods, this is extended by Lobby and Arena players
 */
public class SpleefPlayer {

    private final LapisSpleef plugin;
    private final UUID uuid;
    private final File playerDataFile;
    private YamlConfiguration playerDataYaml;
    private ItemStack[] inventoryContents;

    public SpleefPlayer(LapisSpleef plugin, UUID uuid) {
        this.plugin = plugin;
        this.uuid = uuid;
        playerDataFile = new File(plugin.getDataFolder(), "PlayerData" + File.separator + uuid.toString() + ".yml");
        loadPlayerData();
    }

    /**
     * Get the Bukkit Player object for this SpleefPlayer
     *
     * @return the Bukkit Player object for this player
     */
    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    /**
     * Teleport the player to a location
     *
     * @param location where you want to send the player
     */
    public void teleport(Location location) {
        getPlayer().teleport(location);
    }

    /**
     * Sends the player a formatted message from the messages.yml file with the given key
     *
     * @param key The key of the message you wish to send
     */
    public void sendConfigMessage(String key) {
        String message = plugin.config.getMessage(key);
        sendMessage(message);
    }

    /**
     * Send a message to the players chat, this is a raw send, no formatting is applied to the string
     *
     * @param message The message you wish to send
     */
    public void sendMessage(String message) {
        getPlayer().sendMessage(message);
    }

    /**
     * Store the players inventory to be restored later
     * This will be stored in memory and the players data file so that it can be restored even after a server crash
     */
    public void storeInventory() {
        inventoryContents = getPlayer().getInventory().getContents();
        //save inventory to file for emergency restore should the server crash
        playerDataYaml.set("StoredInventory", inventoryContents);
        savePlayerData();
    }

    /**
     * Restore the players inventory from memory
     * This also deletes the stored inventory from memory and the player data file
     * This is to attempt to reduce the ability to use inventory storage to dupe items
     */
    public void restoreInventory() {
        //Set the players inventory contents to the stored contents
        getPlayer().getInventory().setContents(inventoryContents);
        //Delete the inventory from our file so that it cant be used to dupe items
        playerDataYaml.set("StoredInventory", null);
        savePlayerData();
    }

    /**
     * This method can be used to restore the players inventory after a server/plugin crash or if a player
     * leaves mid-game
     */
    public void restoreInventoryFromFile() {
        //Load the list of ItemStacks from the player data file
        List<ItemStack> itemList = (List<ItemStack>) playerDataYaml.getList("StoredInventory");
        //Don't proceed if it is null
        if (itemList == null)
            return;
        //Set the stored inventory contents to the loaded contents
        inventoryContents = itemList.toArray(new ItemStack[0]);
        //Use the restore inventory method to save the new inventory contents
        restoreInventory();
    }

    /**
     * Load the players data file from disk
     */
    public void loadPlayerData() {
        if (!playerDataFile.exists()) {
            try {
                playerDataFile.createNewFile();
                YamlConfiguration yaml = YamlConfiguration.loadConfiguration(playerDataFile);
                yaml.set("Username", getPlayer().getName());
                yaml.set("Stats.GamesPlayed", 0);
                yaml.set("Stats.Wins", 0);
                yaml.set("Stats.Losses", 0);
                yaml.set("Stats.Abandoned", 0);
                yaml.save(playerDataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create a player data file for " + getPlayer().getName());
                plugin.getLogger().severe(e.toString());
                plugin.getLogger().severe(e.fillInStackTrace().toString());
            }
        }
        playerDataYaml = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    /**
     * Save changes to the players data file to the disk
     */
    public void savePlayerData() {
        try {
            playerDataYaml.save(playerDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data for " + getPlayer().getName());
            plugin.getLogger().severe(e.toString());
            plugin.getLogger().severe(e.fillInStackTrace().toString());
        }
    }

}
