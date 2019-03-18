package com.robomwm.gpauctions.auction;

import com.robomwm.gpauctions.GPAuctions;
import com.robomwm.usefulutil.UsefulUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created on 1/20/2019.
 *
 * Auction manager
 *
 * @author RoboMWM
 */
public class Auctioneer
{
    private Plugin plugin;
    private File file;
    private Map<Long, Auction> auctions = new HashMap<>();
    private DataStore dataStore;
    private static Economy economy;

    public Auctioneer(Plugin plugin, DataStore dataStore)
    {
        this.plugin = plugin;
        this.dataStore = dataStore;
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = rsp.getProvider();
        file = new File(plugin.getDataFolder() + File.separator + "auctions.data");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (String key : yaml.getKeys(false))
        {
            Auction auction = (Auction)yaml.get(key);
            auctions.put(auction.getClaimID(), auction);
        }

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                Iterator<Auction> auctionIterator = auctions.values().iterator();

                while (auctionIterator.hasNext())
                {
                    Auction auction = auctionIterator.next();

                    if (auction.isEnded())
                    {
                        endAuction(auction);
                        auctionIterator.remove();
                    }
                }
            }
        }.runTaskTimer(plugin, 1200L, 1200L);
    }

    public boolean addAuction(Auction auction)
    {
        GPAuctions.debug("addAuction called");
        if (auctions.containsKey(auction.getClaimID()))
            return false;

        GPAuctions.debug("Pre-existing auction does not exist.");

        Claim claim = dataStore.getClaim(auction.getClaimID());
        claim.clearPermissions();
        dataStore.changeClaimOwner(claim, null);
        GPAuctions.debug("Set claim owner to null (admin claim) and cleared trustlist");

        if (auction.getOwner() != null)
        {
            PlayerData playerData = dataStore.getPlayerData(auction.getOwner());
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() - claim.getArea());
            GPAuctions.debug("Deducted " + claim.getArea() + " bonus blocks from player " + auction.getOwner().toString());
        }

        auctions.put(auction.getClaimID(), auction);
        saveAuctions();
        auction.updateSign();
        plugin.getLogger().info("Adding auction with ID " + auction.getClaimID());
        plugin.getLogger().info("Auction started. " + auction.toString());

        Location location = auction.getSign().getLocation();

        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), ChatColor.translateAlternateColorCodes('&',
                "[&6GPAuctions&f] &bAn auction has been created at &a" +
                        location.getBlockX() + ", " + location.getBlockZ() +
                        "&b. Bidding will start at &a" + auction.getNextBidPrice() + "&b."));

        return true;
    }

    public Auction getAuction(Location location)
    {
        Claim claim = dataStore.getClaimAt(location, true, null);
        if (claim == null)
            return null;
        return getAuction(claim);
    }

    public Auction getAuction(Claim claim)
    {
        return getAuction(claim.getID());
    }

    public Auction getAuction(long claimID)
    {
        return auctions.get(claimID);
    }

    public boolean cancelAuction(Auction auction)
    {
        auction = auctions.remove(auction.getClaimID());
        if (auction == null)
            return false;
        auction.cancelSign();

        Claim claim = dataStore.getClaim(auction.getClaimID());
        dataStore.changeClaimOwner(claim, auction.getOwner());

        return true;
    }

    private void saveAuctions()
    {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Auction auction : auctions.values())
            yaml.set(String.valueOf(auction.getClaimID()), auction);
        UsefulUtil.saveStringToFile(plugin, file, yaml.saveToString());
        GPAuctions.debug("Saved auctions to file.");
    }

    private void endAuction(Auction auction)
    {
        plugin.getLogger().info("Auction " + auction.toString() + " ended.");
        Claim claim = dataStore.getClaim(auction.getClaimID());
        PlayerData playerData = dataStore.getPlayerData(auction.getOwner());
        Bid winningBid = findWinningBid(auction);

        //No winner, return to owner
        if (winningBid == null)
        {
            if (auction.getOwner() == null)
            {
                plugin.getLogger().info("No winner. Auction canceled (nothing to do since originally was admin claim.");
                return;
            }
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + claim.getArea());
            dataStore.changeClaimOwner(claim, auction.getOwner());
            plugin.getLogger().info("No winner, returning to owner " + auction.getOwner());

            return;
        }

        PlayerData winnerData = dataStore.getPlayerData(winningBid.getBidderUUID());
        winnerData.setBonusClaimBlocks(winnerData.getBonusClaimBlocks() + claim.getArea());
        dataStore.changeClaimOwner(claim, winningBid.getBidderUUID());
        economy.withdrawPlayer(plugin.getServer().getOfflinePlayer(winningBid.getBidderUUID()), winningBid.getPrice());
        plugin.getLogger().info("Transferred claim to winning bid " + winningBid.toString());

        String location = GPAuctions.smallFriendlyCoordinate(auction.getSign().getLocation());
        String buyer = plugin.getServer().getOfflinePlayer(winningBid.getBidderUUID()).getName();

        GPAuctions.lazyCmdDispatcher("broadcast [&6GPAuctions&f] &b The auction at &a" + location +
                " &b has closed. The auction winner is &a" + buyer +
                " &bwith a final bid of &a" + winningBid.getPrice() +
                "&b.");

        GPAuctions.lazyCmdDispatcher("mail send " + buyer +
                " [&6GPAuctions&f] &bYou have won the Auction! &a" + winningBid.getPrice() +
                " &bhas been deducted from your account balance. Your new property at &a" + location +
                "&b has been transferred into your care.");

        if (auction.getOwner() == null)
            return;

        String seller = plugin.getServer().getOfflinePlayer(auction.getOwner()).getName();

        GPAuctions.lazyCmdDispatcher("mail send " + seller +
                " [&6GPAuctions&f] &a" + buyer +
                " &bhas won your auction at &a" + location +
                " &b. The final bid was &a" + winningBid.getPrice() +
                "&b.");
    }

    /**
     * Finds a valid, winning bid in an auction.
     * @param auction
     * @return
     */
    private Bid findWinningBid(Auction auction)
    {
        Set<String> playersMessaged = new HashSet<>();

        for (Bid bid : auction.getBids())
        {
            OfflinePlayer player = Bukkit.getOfflinePlayer(bid.getBidderUUID());
            double balance = economy.getBalance(player);
            if (balance > bid.getPrice())
                return bid;

            String bidder = plugin.getServer().getOfflinePlayer(bid.getBidderUUID()).getName();
            if (playersMessaged.add(bidder))
                GPAuctions.lazyCmdDispatcher("mail send " + bidder +
                        " [&6GPAuctions&f] &bUh oh! You are too broke to pay! Your bid of " + bid.getPrice() +
                        " at " + GPAuctions.smallFriendlyCoordinate(auction.getSign().getLocation()) +
                        " has been rejected.");
        }
        return null;
    }

    public static String format(double value)
    {
        return economy.format(value);
    }
}
