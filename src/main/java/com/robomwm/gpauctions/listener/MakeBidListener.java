package com.robomwm.gpauctions.listener;

import com.robomwm.gpauctions.auction.Auction;
import com.robomwm.gpauctions.auction.Auctioneer;
import com.robomwm.usefulutil.UsefulUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

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

        if (event.getPlayer().isSneaking())
        {
            event.getPlayer().sendMessage(getInfo(event.getClickedBlock().getLocation()));
            return;
        }

        auctioneer.addBid(event.getPlayer(), event.getClickedBlock().getLocation());
    }

    private String getInfo(Location location)
    {
        Auction auction = auctioneer.getAuction(location);
        String time = UsefulUtil.formatTime(TimeUnit.MILLISECONDS.toSeconds(auction.getEndTime() - System.currentTimeMillis()));

        String bidder = "No Bidders.";
        if (!auction.getBids().isEmpty())
            bidder = "The most recent bidder is: &a" + Bukkit.getOfflinePlayer(auction.getBids().peek().getBidderUUID()).getName();

        return ChatColor.translateAlternateColorCodes('&',
                "&9----= &f[&6RealEstate Auction Info&f]&9=----\n" +
                "&bThis &aCLAIM &bis on auction for &a" + auction.getNextBidPrice() + "\n" +
                "&bThe auction closes in &a" + time + " &bhours.\n" +
                "&bThe current/previous owner is: &a" + auction.getOwnerName() + "\n" +
                bidder);
    }
}
