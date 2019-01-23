package com.robomwm.gpauctions.listener;

import com.robomwm.gpauctions.auction.Auction;
import com.robomwm.gpauctions.auction.Auctioneer;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Created on 1/20/2019.
 *
 * @author RoboMWM
 */
public class CreateAuctionListener implements Listener
{
    private DataStore dataStore;
    private Auctioneer auctioneer;

    public CreateAuctionListener(Plugin plugin, Auctioneer auctioneer)
    {
        this.auctioneer = auctioneer;
        dataStore = ((GriefPrevention)(plugin.getServer().getPluginManager().getPlugin("GriefPrevention"))).dataStore;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void onSignChange(SignChangeEvent event)
    {
        if (!event.getLine(1).equalsIgnoreCase("[auction claim]"))
            return;

        double startingBid;
        try
        {
            startingBid = Double.parseDouble(event.getLine(2));
        }
        catch (NumberFormatException e)
        {
            return;
        }

        long endTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30);

        Claim claim = dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
        if (claim == null)
            return;

        if (auctioneer.addAuction(new Auction(claim, endTime, startingBid)))
        {
            event.getPlayer().sendMessage("Auction started");
        }
    }
}
