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

package net.lapismc.spleef.util;

import net.lapismc.spleef.LapisSpleef;
import net.lapismc.spleef.arena.SpleefPlayer;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.scheduler.BukkitTask;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A util class for managing BossBar progress/timer bars
 */
public class CountdownManager {

    private final LapisSpleef plugin;
    private final List<SpleefPlayer> players = new ArrayList<>();
    BossBar bar;
    BukkitTask refreshTask;
    String text;
    Long startTime, endTime;
    private boolean isVisible = false;

    /**
     * Init the manager, needs the plugin to register tasks
     *
     * @param plugin the LapisSpleef main class
     */
    public CountdownManager(LapisSpleef plugin) {
        this.plugin = plugin;
        bar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
        plugin.tasks.addShutdownTask(this::cancelRefreshTask);
    }

    /**
     * Set the title text on the BossBar
     *
     * @param text The title of the boss bar
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * Start the countdown timer
     *
     * @param length The number of milliseconds that the boss bar should countdown
     */
    public void startCountdown(long length) {
        startTime = System.currentTimeMillis();
        endTime = startTime + length;
        setVisible(true);
    }

    /**
     * Add players to be able to see the boss bar
     *
     * @param players The players who should be added
     */
    public void addPlayers(List<SpleefPlayer> players) {
        this.players.addAll(players);
        if (isVisible) {
            for (SpleefPlayer player : players)
                bar.addPlayer(player.getBukkitPlayer());
        }
    }

    /**
     * Set the boss bar visibility
     *
     * @param visible true will display the bar to players and start refreshing it, false will remove it from view
     */
    public void setVisible(boolean visible) {
        if (visible) {
            cancelRefreshTask();
            refreshTask = Bukkit.getScheduler().runTaskTimer(plugin, this::refresh, 1, 1);
            isVisible = true;
            bar.setVisible(true);
            for (SpleefPlayer player : players) {
                bar.addPlayer(player.getBukkitPlayer());
            }
        } else {
            cancelRefreshTask();
            isVisible = false;
            bar.setVisible(false);
            bar.removeAll();
        }
    }

    private void cancelRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled())
            refreshTask.cancel();
    }

    /**
     * Refresh the boss bar with new progress and time remaining text
     */
    public void refresh() {
        //Set the text
        //Only set the text of we have text to set
        if (text != null) {
            //Don't append time remaining if it hasn't been set or if the countdown has ended
            String prettyString = endTime != null && endTime > System.currentTimeMillis() ?
                    new PrettyTime().format(new Date(endTime)) : "";
            String msg = text + prettyString;
            bar.setTitle(msg);
        }
        //Only set the progress if we have the data to do so
        if (startTime != null && endTime != null) {
            //Set the progress
            long totalTime = endTime - startTime;
            long amountPassed = System.currentTimeMillis() - startTime;
            //Calculate the progress
            double progress = (double) amountPassed / (double) totalTime;
            //Clamp the value between 0 - 1
            progress = Math.max(0, Math.min(1, progress));
            //Set the progress on the bar
            bar.setProgress(progress);
        }
    }

}
