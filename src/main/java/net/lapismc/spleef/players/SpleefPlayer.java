package net.lapismc.spleef.players;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * A class to store basic player info and methods, this is extended by Lobby and Arena players
 */
public class SpleefPlayer {

    private final UUID uuid;

    public SpleefPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Get the Bukkit Player object for this SpleefPlayer
     *
     * @return the Bukkit Player object for this player if they are online, otherwise null
     */
    public Player getPlayer() {
        if (Bukkit.getOfflinePlayer(uuid).isOnline())
            return Bukkit.getPlayer(uuid);
        else
            return null;
    }

}
