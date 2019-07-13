package com.robomwm.gpauctions.listener;

import com.robomwm.gpauctions.auction.Auction;
import com.robomwm.gpauctions.auction.Auctioneer;
import me.SuperPyroManiac.GPR.GPRealEstate;
import me.SuperPyroManiac.GPR.events.GPRSaleEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

/**
 * Created on 7/13/2019.
 *
 * Cancel pending auction if claim is sold via GPRealEstate
 *
 * @author RoboMWM
 */
public class ClaimSaleListener implements Listener
{
    private Auctioneer auctioneer;

    public ClaimSaleListener(Plugin plugin, Auctioneer auctioneer)
    {
        GPRealEstate gpRealEstate = (GPRealEstate)plugin.getServer().getPluginManager().getPlugin("GPRealEstate");
        if (gpRealEstate == null || !gpRealEstate.isEnabled())
            return;
        this.auctioneer = auctioneer;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSaleEvent(GPRSaleEvent event)
    {
        Auction auction = auctioneer.getAuction(event.getClaim());
        if (auction == null)
            return;

        auctioneer.cancelAuction(auction);
    }

}
