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

/**
 * This enumeration is used to store the current state of the game for any arena
 */
public enum GameState {

    /**
     * Disabled arenas are not joinable, this is the state that a new and not yet configured arena will begin in
     */
    disabled,
    /**
     * Waiting arenas are joinable, but are waiting for a minimum number of players or players to ready up before starting
     */
    waiting,
    /**
     * Starting arenas are non-joinable but also cannot break blocks, this is a temporary state that will transition to playing
     */
    starting,
    /**
     * Playing arenas are currently in game, and as such new players can only spectate until the next game
     */
    playing,
    /**
     * Arenas in the ended state are simply allowing the winner to bask in their glory before the waiting state begins again
     */
    ended

}
