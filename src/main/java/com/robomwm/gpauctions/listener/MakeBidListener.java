package com.robomwm.gpauctions.listener;

import com.robomwm.gpauctions.auction.Auctioneer;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

/**
 * Created on 1/23/2019.
 *
 * @author RoboMWM
 */
public class MakeBidListener implements Listener
{
    private Auctioneer auctioneer;

    public MakeBidListener(Plugin plugin, Auctioneer auctioneer)
    {
        this.auctioneer = auctioneer;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    private void onSignClick(PlayerInteractEvent event)
    {
        switch (event.getAction())
        {
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        if (event.getClickedBlock().getType() != Material.SIGN)
            return;
        if (!((Sign)event.getClickedBlock().getState()).getLine(0).equalsIgnoreCase("Real Estate"))
            return;

        auctioneer.addBid(event.getPlayer(), event.getClickedBlock().getLocation());
    }
}
