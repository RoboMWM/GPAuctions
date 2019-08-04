package com.robomwm.gpauctions.auction;

import com.robomwm.gpauctions.GPAuctions;
import com.robomwm.usefulutils.FileUtils;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import java.util.concurrent.TimeUnit;

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
    private GriefPrevention gp;
    private DataStore dataStore;
    private static Economy economy;

    public Auctioneer(Plugin plugin, GriefPrevention gp, DataStore dataStore)
    {
        this.plugin = plugin;
        this.gp = gp;
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
                        saveAuctions();
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
            dataStore.savePlayerData(auction.getOwner(), playerData);
        }

        auctions.put(auction.getClaimID(), auction);
        saveAuctions();
        auction.updateSign();
        plugin.getLogger().info("Adding auction with ID " + auction.getClaimID());
        plugin.getLogger().info("Auction started. " + auction.toString());

        Location location = auction.getSign().getLocation();

        GPAuctions.lazyCmdDispatcher("broadcast &f[&6GPAuctions&f] &bAn auction has been created at &a" +
                        GPAuctions.smallFriendlyCoordinate(location) +
                "&b. Bidding will start at &a" + auction.getNextBidPrice() +
                "&b.");

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
        saveAuctions();
        auction.endSign("Canceled");

        Claim claim = dataStore.getClaim(auction.getClaimID());
        dataStore.changeClaimOwner(claim, auction.getOwner());

        if (auction.getOwner() == null)
            return true;

        GPAuctions.debug("Refunding claimblocks");
        PlayerData playerData = dataStore.getPlayerData(auction.getOwner());

        playerData
                .setBonusClaimBlocks(
                        playerData.getBonusClaimBlocks() +
                                claim.getArea());
        dataStore.savePlayerData(auction.getOwner(), playerData);

        return true;
    }

    public void saveAuctions()
    {
        YamlConfiguration yaml = new YamlConfiguration();
        GPAuctions.debug("Starting to save auctions");
        for (Auction auction : auctions.values())
            yaml.set(String.valueOf(auction.getClaimID()), auction);
        FileUtils.saveStringToFile(plugin, file, yaml.saveToString());
        GPAuctions.debug("Saved auctions to file.");
    }

    private void endAuction(Auction auction)
    {
        plugin.getLogger().info("Auction " + auction.toString() + " ended.");
        Claim claim = dataStore.getClaim(auction.getClaimID());
        if (claim == null)
        {
            plugin.getLogger().warning("Claim does not exist! Canceling auction instead...");
            auction.endSign("Canceled");
            return;
        }
        auction.endSign("Closed");
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

            GPAuctions.debug(playerData + "," + claim);

            OfflinePlayer owner = plugin.getServer().getOfflinePlayer(auction.getOwner());

            if (isExpiredPlayer(owner))
            {
                dataStore.deleteClaim(claim, true);
                GPAuctions.lazyCmdDispatcher("mail send " + owner.getName() +
                        " &f[&6GPAuctions&f] &bYour auction at &a" +
                        GPAuctions.smallFriendlyCoordinate(auction.getSign().getLocation()) +
                        "&b has closed without bidders. The claim has expired due to inactivity.");
                GPAuctions.debug("Removing auction sign.");
                auction.getSign().getLocation().getBlock().setType(Material.AIR);
                return;
            }

            playerData
                    .setBonusClaimBlocks(
                            playerData.getBonusClaimBlocks() +
                                    claim.getArea());
            dataStore.savePlayerData(auction.getOwner(), playerData);
            dataStore.changeClaimOwner(claim, auction.getOwner());
            plugin.getLogger().info("No winner, returning to owner " + auction.getOwner());
            GPAuctions.lazyCmdDispatcher("mail send " + owner.getName() +
                    " &f[&6GPAuctions&f] &bYour auction has closed without bidders. The claim has been returned to you.");

            return;
        }

        PlayerData winnerData = dataStore.getPlayerData(winningBid.getBidderUUID());
        winnerData.setBonusClaimBlocks(winnerData.getBonusClaimBlocks() + claim.getArea());
        dataStore.savePlayerData(winningBid.getBidderUUID(), winnerData);
        dataStore.changeClaimOwner(claim, winningBid.getBidderUUID());
        economy.withdrawPlayer(plugin.getServer().getOfflinePlayer(winningBid.getBidderUUID()), winningBid.getPrice());
        plugin.getLogger().info("Transferred claim to winning bid " + winningBid.toString());

        String location = GPAuctions.smallFriendlyCoordinate(auction.getSign().getLocation());
        String buyer = plugin.getServer().getOfflinePlayer(winningBid.getBidderUUID()).getName();

        GPAuctions.lazyCmdDispatcher("broadcast &f[&6GPAuctions&f] &bThe auction at &a" + location +
                " &bhas closed. The auction winner is &a" + buyer +
                " &bwith a final bid of &a" + winningBid.getPrice() +
                "&b.");

        GPAuctions.lazyCmdDispatcher("mail send " + buyer +
                " &f[&6GPAuctions&f] &bYou have won the Auction! &a" + winningBid.getPrice() +
                " &bhas been deducted from your account balance. Your new property at &a" + location +
                " &bhas been transferred into your care.");

        if (auction.getOwner() == null)
            return;

        OfflinePlayer owner = plugin.getServer().getOfflinePlayer(auction.getOwner());
        if (isExpiredPlayer(owner))
        {
            GPAuctions.debug("Removing auction sign.");
            auction.getSign().getLocation().getBlock().setType(Material.AIR);
        }

        economy.depositPlayer(plugin.getServer().getOfflinePlayer(auction.getOwner()), winningBid.getPrice());

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

    public boolean isExpiredPlayer(OfflinePlayer player)
    {
        long lastPlayed = player.getLastSeen(); //getLastPlayed is deprecated. (Maybe by Paper?) getLastSeen is a paper-only method.
        long inactivityThresholdAsOfNow = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(gp.config_claims_expirationDays);

        //if player's last login was before the configured inactivity date, this is an expired player
        return lastPlayed < inactivityThresholdAsOfNow;
    }
}
