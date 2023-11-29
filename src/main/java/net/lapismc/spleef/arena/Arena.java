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

package net.lapismc.spleef.arena;

import net.lapismc.lapiscore.utils.LapisItemBuilder;
import net.lapismc.spleef.LapisSpleef;
import net.lapismc.spleef.util.CountdownManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * This class is used to represent the arena that the spleef game will take place in
 * It handles most of the gameplay logic
 */
public class Arena implements Listener {

    private final LapisSpleef plugin;
    //List of floors in this arena
    private List<Floor> floors = new ArrayList<>();
    //The current state of the game in the arena
    GameState gameState;
    //List of players in arena
    List<SpleefPlayer> players = new ArrayList<>();
    //The name of the arena, must be unique
    private String name;
    //Where players should spawn when sent to the lobby
    private Location lobbySpawn;
    //Where players should spawn when they spectate
    private Location spectateLocation;
    //Height of the bottom of the arena where players should be eliminated
    private int eliminationHeight;
    //Countdown manager for displaying boss bars to players
    private final CountdownManager countdownManager;

    /**
     * Initialize an arena
     *
     * @param plugin The LapisSpleef main class
     * @param name   The unique name of this arena
     */
    public Arena(LapisSpleef plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        countdownManager = new CountdownManager(plugin);
    }

    /**
     * Trigger the start of the game, will:
     * Set the game state to playing
     * Teleport the players into the arena on the 0th floor
     * Give the players the load outs that they need
     * Start a count-down for game start
     */
    public void startGame() {
        //TODO: Implement below comments
        gameState = GameState.starting;
        //Sort the floors before we try to grab the highest
        sortFloors();
        //Get the spawn points from the highest floor
        Queue<Location> spawnPoints = new PriorityQueue<>(floors.get(0).generateSpawnPoints(players.size()));
        for (SpleefPlayer player : players) {
            //Teleport into game arena
            //Evenly distribute the players by spreading them along the list of blocks evenly
            player.teleport(spawnPoints.poll());
            //Give players the tools they need
            player.getBukkitPlayer().getInventory().addItem(new LapisItemBuilder(Material.IRON_SHOVEL)
                    .setName(ChatColor.AQUA + "The Shovel of Destiny")
                    .addLore("This shovel will bring you:", "Fun", "Pain", "Falling").build());
        }
        //Start a 3-second count-down so that players can see where they are and get ready before block breaks are enabled
        countdownManager.addPlayers(players);
        countdownManager.startCountdown(1000 * 3);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            gameState = GameState.playing;
            sendTitle(plugin.config.getMessage("Game.Begin"), "");
        }, 20 * 3);
    }

    /**
     * Add a player to the arena
     * This method handles sending them to spectator areas or the lobby depending on game state as well as storing their inventories
     *
     * @param player The player being added
     */
    public void addPlayer(SpleefPlayer player) {
        //Don't add players if the arena is disabled
        if (gameState.equals(GameState.disabled))
            return;
        //Add the player to the arenas list of players
        players.add(player);
        //Store the players inventory, this will be restored when they leave the arena
        player.storeInventory();

        if (gameState.equals(GameState.playing)) {
            //Send the player to the spectator location
            sendToSpectate(player);
        }
        if (gameState.equals(GameState.waiting)) {
            //Add the player to the lobby
            sendToLobby(player);
        }
    }

    /**
     * When a player joins a waiting game, or a game ends, players should be sent to the lobby using this method
     *
     * @param player The player to be sent to this arenas lobby, this player must be a member of the arena
     */
    public void sendToLobby(SpleefPlayer player) {
        if (!players.contains(player)) {
            plugin.getLogger().warning("Player " + player.getBukkitPlayer().getName() + " was sent to the lobby of arena "
                    + name + ", but they aren't in that arena. This error should be reported.");
            return;
        }
        //Teleport to lobby location
        player.teleport(lobbySpawn);
        //Tell the player that they are in the lobby and should ready up once they are ready
        player.sendConfigMessage("Lobby.Join");
        //Show the ready progress bar/timer or whatever to the player

    }

    /**
     * Send a player to the spectate area of this arena
     * This method is used when a player is eliminated or joins an arena in progress
     *
     * @param player The player who will spectate
     */
    public void sendToSpectate(SpleefPlayer player) {
        //Make sure this player is a member of this arena
        if (!players.contains(player)) {
            plugin.getLogger().warning("Player " + player.getBukkitPlayer().getName() + " was sent to spectate the arena "
                    + name + ", but they aren't in that arena. This error should be reported.");
            return;
        }
        //Teleport to spectate location
        player.teleport(spectateLocation);
        //Tell the player that the game is in progress, so they are now spectating
        player.sendConfigMessage("Spectate.GameInProgress");
    }

    /**
     * Used to detect block breakage in this arena
     *
     * @param e The block break event being fired
     */
    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        //Check if it's an arena player, if it isn't we return and ignore the event
        if (!players.contains(plugin.getSpleefPlayer(e.getPlayer().getUniqueId())))
            return;
        //Check if it's a floor block
        boolean isFloor = false;
        Floor floor = null;
        for (Floor f : floors) {
            if (f.isBlockOnFloor(e.getBlock())) {
                isFloor = true;
                floor = f;
            }
        }
        //If it isn't a floor block, we cancel the event since players shouldn't be able to damage the arena
        if (!isFloor) {
            e.setCancelled(true);
            return;
        }
        //Only allow block breaking if the game is in the playing state, otherwise cancel the event
        if (gameState.equals(GameState.playing)) {
            //Break the floor block
            floor.breakBlock(e.getBlock());
            //Check if it's a tool being used

            //Apply tool abilities

        } else {
            e.setCancelled(true);
        }
    }

    /**
     * Send a title to all Players in the arena
     *
     * @param title    The title to be shown
     * @param subtitle The subtitle to be shown
     */
    public void sendTitle(String title, String subtitle) {
        sendTitle(title, subtitle, 10, 20, 10);
    }

    /**
     * Send a title to all Players in the arena
     *
     * @param title    The title to be shown
     * @param subtitle The subtitle to be shown
     * @param fadeIn   Ticks to fade in for
     * @param stay     Ticks to stay for
     * @param fadeOut  Ticks to fade out for
     */
    public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
        for (SpleefPlayer player : players) {
            player.getBukkitPlayer().sendTitle(title, subtitle, fadeIn, stay, fadeOut);
        }
    }

    /**
     * Get this arenas name
     *
     * @return the name of this arena
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name of this arena
     * This must be unique among the current arena names
     *
     * @param name The new name for this arena
     */
    public void setName(String name) {
        //TODO: Check its unique
        this.name = name;
    }

    /**
     * Add a floor to this arena, new floors are added to the bottom by default
     *
     * @param floor The floor object to add
     */
    public void addFloor(Floor floor) {
        floors.add(floor);
    }

    /**
     * Remove a floor from this arena
     *
     * @param floor The floor you wish to remove
     */
    public void removeFloor(Floor floor) {
        floors.remove(floor);
    }

    /**
     * Get the current list of floors stored for this arena
     *
     * @return a list of current floors
     */
    public List<Floor> getFloors() {
        return floors;
    }

    /**
     * Sorts the floors from highest Y value at index 0 in the floors list to the lowest value in the last index
     */
    public void sortFloors() {
        List<Floor> sortedList = new ArrayList<>();
        int size = floors.size();
        while (sortedList.size() != size) {
            Floor highest = null;
            for (Floor f : floors) {
                int y = f.getYLevel();
                if (highest == null || highest.getYLevel() < y) {
                    highest = f;
                }
            }
            sortedList.add(highest);
            floors.remove(highest);
        }
        floors = sortedList;
    }

    /**
     * Get the lobby spawn location, this is where players will spawn if they join while the game is in the Waiting game state
     *
     * @return the location of the lobby spawn point
     */
    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    /**
     * Set where players will spawn when they join the arena in the Waiting game state
     *
     * @param location The new lobby spawn location
     */
    public void setLobbySpawn(Location location) {
        lobbySpawn = location;
    }

    /**
     * Get the spectate spawn location
     *
     * @return the location where players should spawn to spectate
     */
    public Location getSpectateLocation() {
        return spectateLocation;
    }

    /**
     * Set the spectate spawn location, this is where players will be sent when they join a game in progress or are eliminated
     *
     * @param location The new spectate spawn location
     */
    public void setSpectateLocation(Location location) {
        spectateLocation = location;
    }

    /**
     * Get the Y value for the elimination height
     *
     * @return the Y value for elimination
     */
    public int getEliminationHeight() {
        return eliminationHeight;
    }

    /**
     * Set the height at which players will be eliminated, that is if a players Y component of their location is ever less than this value,
     * they will be eliminated. This should be below the lowest floor.
     *
     * @param height The new elimination height
     */
    public void setEliminationHeight(int height) {
        eliminationHeight = height;
    }

    /**
     * Load the arenas details from a config file
     * This should mainly be used when loading an exising arena
     */
    public void loadFromConfig() {
        //TODO: load from config
    }

    /**
     * Save arena data to config file
     * This should be used after edit the arenas details such that it can be loaded next time
     */
    public void saveToConfig() {
        //TODO: save to config
    }

}
