package com.robomwm.gpauctions.listener;

import com.robomwm.gpauctions.GPAuctions;
import com.robomwm.gpauctions.auction.Auction;
import com.robomwm.gpauctions.auction.Auctioneer;
import com.robomwm.gpauctions.auction.Bid;
import com.robomwm.usefulutil.UsefulUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.concurrent.TimeUnit;

/**
 * Created on 1/23/2019.
 *
 * @author RoboMWM
 */
public class MakeBidListener implements Listener
{
    private Economy economy;
    private Auctioneer auctioneer;

    public MakeBidListener(Plugin plugin, Auctioneer auctioneer)
    {
        this.auctioneer = auctioneer;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = rsp.getProvider();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    //Cancel auction if its sign is broken
    @EventHandler(ignoreCancelled = true)
    private void onSignBreak(BlockBreakEvent event)
    {
        if (!isAuctionSign(event.getBlock()))
            return;

        Auction auction = auctioneer.getAuction(event.getBlock().getLocation());

        if (auction != null && auction.getSign().getLocation().equals(event.getBlock().getLocation()))
            auctioneer.cancelAuction(auction);
    }

    @EventHandler(ignoreCancelled = true)
    private void onSignClick(PlayerInteractEvent event)
    {
        Player player = event.getPlayer();

        switch (event.getAction())
        {
            case RIGHT_CLICK_BLOCK:
                break;
            default:
                return;
        }

        if (!isAuctionSign(event.getClickedBlock()))
            return;

        Auction auction = auctioneer.getAuction(event.getClickedBlock().getLocation());
        if (auction == null || !auction.getSign().getLocation().equals(event.getClickedBlock().getLocation()))
        {
            GPAuctions.debug("No auction found or incorrect sign used at location " + event.getClickedBlock().getLocation());
            return;
        }

        if (player.isSneaking())
        {
            String infoMessage = getInfo(auction);
            player.sendMessage(infoMessage);
            return;
        }

        Bid bid = new Bid(player, auction.getNextBidPrice());
        if (bid.getPrice() > economy.getBalance(player))
        {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "[&6GPAuctions&f] &bYou have insufficient funds to place a bid at this time."));
            return;
        }

        if (auction.addBid(bid))
        {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "[&6GPAuctions&f] &bYour bid of &a" + bid.getPrice() +
                            " &bhas been accepted. " +
                            "If you are the Auction winner payment will be due in &a" +
                            auctioneer.getAuction(event.getClickedBlock().getLocation()).getEndTimeString() +
                            "&b."
            ));
            auctioneer.saveAuctions();
        }
        else
            player.sendMessage("[&6GPAuctions&f] Error occurred when attempting to place a bid. (Auction has ended, likely.)");
    }

    private boolean isAuctionSign(Block block)
    {
        if (block.getType() != Material.SIGN)
            return false;
        GPAuctions.debug("Sign clicked");
        if (!((Sign)block.getState()).getLine(0).equalsIgnoreCase("Real Estate"))
            return false;
        GPAuctions.debug("Is a real estate-labeled sign");
        return true;
    }

    private String getInfo(Auction auction)
    {
        String time = UsefulUtil.formatTime(TimeUnit.MILLISECONDS.toSeconds(auction.getEndTime() - System.currentTimeMillis()));

        String bidder = "No Bidders.";
        if (!auction.getBids().isEmpty())
            bidder = "The most recent bidder is: &a" + Bukkit.getOfflinePlayer(auction.getHighestBid().getBidderUUID()).getName();

        return ChatColor.translateAlternateColorCodes('&',
                "&9----= &f[&6RealEstate Auction Info&f]&9=----\n" +
                "&bThis &aCLAIM &bis on auction for &a" + auction.getNextBidPrice() + "\n" +
                "&bThe auction closes in &a" + time + "&b.\n" +
                "&bThe current/previous owner is: &a" + auction.getOwnerName() + "\n" +
                bidder);
    }
}
