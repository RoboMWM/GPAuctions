package com.robomwm.gpauctions.listener;

import com.robomwm.gpauctions.auction.Auctioneer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Created on 1/23/2019.
 *
 * @author RoboMWM
 */
public class MakeBidListener implements Listener
{
    private Auctioneer auctioneer;

    public MakeBidListener(Auctioneer auctioneer)
    {
        this.auctioneer = auctioneer;
    }

    @EventHandler(ignoreCancelled = true)
    private void onSignClick(PlayerInteractEvent event)
    {
        switch (event.getAction())
        {
            default:
                return;
        }
    }
}
