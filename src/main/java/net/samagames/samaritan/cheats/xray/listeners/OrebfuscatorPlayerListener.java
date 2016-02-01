/*
 * Copyright (C) 2011-2014 lishid.  All rights reserved.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package net.samagames.samaritan.cheats.xray.listeners;

import net.samagames.samaritan.cheats.xray.Orebfuscator;
import net.samagames.samaritan.cheats.xray.OrebfuscatorConfig;
import net.samagames.samaritan.cheats.xray.hithack.BlockHitManager;
import net.samagames.samaritan.cheats.xray.obfuscation.BlockUpdate;
import net.samagames.samaritan.cheats.xray.obfuscation.ProximityHider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

public class OrebfuscatorPlayerListener implements Listener {
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (OrebfuscatorConfig.LoginNotification) {
            if (OrebfuscatorConfig.playerBypassOp(player)) {
                Orebfuscator.message(player, "Orebfuscator bypassed because you are OP.");
            } else if (OrebfuscatorConfig.playerBypassPerms(player)) {
                Orebfuscator.message(player, "Orebfuscator bypassed because you have permission.");
            }
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        BlockHitManager.clearHistory(event.getPlayer());
        if (OrebfuscatorConfig.UseProximityHider) {
            ProximityHider.clearPlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.useInteractedBlock() == Result.DENY)
            return;

        //For using a hoe for farming
        if (event.getItem() != null &&
                event.getItem().getType() != null &&
                (event.getMaterial() == Material.DIRT || event.getMaterial() == Material.GRASS) &&
                ((event.getItem().getType() == Material.WOOD_HOE) ||
                        (event.getItem().getType() == Material.IRON_HOE) ||
                        (event.getItem().getType() == Material.GOLD_HOE) ||
                        (event.getItem().getType() == Material.DIAMOND_HOE))) {
            BlockUpdate.Update(event.getClickedBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        BlockHitManager.clearHistory(event.getPlayer());
        if (OrebfuscatorConfig.UseProximityHider) {
            ProximityHider.clearPlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (OrebfuscatorConfig.UseProximityHider) {
            ProximityHider.playerMoved(event.getPlayer(), event.getFrom());
        }
    }
}
