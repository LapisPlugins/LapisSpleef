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

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class Floor {

    //The arena that this floor belongs too
    private final Arena arena;
    //The block contained in this floor
    private final List<Block> blocks;
    //The material that the blocks of the floor are made from
    private final Material material;

    /**
     * @param arena    The arena that this floor belongs too
     * @param blocks   The blocks that are contained by this floor
     * @param material The material that the floor should regenerate too
     */
    public Floor(Arena arena, List<Block> blocks, Material material) {
        this.arena = arena;
        this.blocks = blocks;
        this.material = material;
    }

    /**
     * Check if a block is contained on this floor
     *
     * @param b The block you wish to check
     * @return True if the block is on this floor, otherwise false
     */
    public boolean isBlockOnFloor(Block b) {
        return blocks.contains(b);
    }

    /**
     * Break a block on this floor
     * Spawns particles and plays sound like it was naturally broken
     *
     * @param b The block you wish to break
     */
    public void breakBlock(Block b) {
        //Don't break it if it isn't on our floor
        if (!isBlockOnFloor(b))
            return;
        //Set block to air
        b.setType(Material.AIR);
        //Display the block break particles
        b.getWorld().spawnParticle(Particle.BLOCK_CRACK, b.getLocation().add(.5, .5, .5), 1, 1, .1, .1, .1, material.createBlockData());
        //Play block break sound
        b.getWorld().playSound(b.getLocation(), material.createBlockData().getSoundGroup().getBreakSound(), 1, 1);
    }

    /**
     * Set all block on this floor back to the default material
     */
    public void regenerateFloor() {
        for (Block b : blocks) {
            //Set each block back to the original material
            b.setType(material);
            //TODO: might be cool to spawn particles in here
        }
    }

    /**
     * Generate evenly spaced spawn points along the blocks of this floor
     *
     * @param numberOfPlayers The number of players who need spawn points
     * @return a list of locations evenly spaced over the entirety of the blocks in the floor
     */
    public List<Location> generateSpawnPoints(int numberOfPlayers) {
        List<Location> spawnPoints = new ArrayList<>();
        //Calculate the number of blocks between players
        int gap = blocks.size() / numberOfPlayers;
        //Calculate a spawn point per player
        for (int i = 1; i < numberOfPlayers; i++) {
            //Get the block spaced for this player by multiplying the gap by this players index
            //Then add 1 in the y-axis to get a location above the block
            spawnPoints.add(blocks.get(gap * i).getLocation().add(0, 1, 0));
        }
        return spawnPoints;
    }

}
