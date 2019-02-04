package com.robomwm.gpauctions.auction;

import com.robomwm.gpauctions.GPAuctions;
import com.robomwm.usefulutil.UsefulUtil;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.PlayerData;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
        claim.ownerID = null;
        claim.clearPermissions();
        GPAuctions.debug("Set claim owner to null (admin claim) and cleared trustlist");
        PlayerData playerData = dataStore.getPlayerData(auction.getOwner());
        playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() - claim.getArea());
        GPAuctions.debug("Deducted " + claim.getArea() + " bonus blocks from player " + auction.getOwner().toString());

        auctions.put(auction.getClaimID(), auction);
        saveAuctions();
        auction.updateSign();
        plugin.getLogger().info("Auction started. " + auction.toString());

        return true;
    }

    public Bid addBid(Player player, Location location)
    {
        Claim claim = dataStore.getClaimAt(location, false, null);
        if (claim == null)
            return null;

        Auction auction = auctions.get(claim.getID());
        if (auction == null)
            return null;

        double balance = economy.getBalance(player);
        if (balance < auction.getNextBidPrice())
            return null;

        Bid bid = new Bid(player, auction.getNextBidPrice());

        if (auction.addBid(new Bid(player, auction.getNextBidPrice())))
            return bid;
        return null;
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
            playerData.setBonusClaimBlocks(playerData.getBonusClaimBlocks() + claim.getArea());
            claim.ownerID = auction.getOwner();
            plugin.getLogger().info("No winner, returning to owner " + auction.getOwner());
            return;
        }

        PlayerData winnerData = dataStore.getPlayerData(winningBid.getBidderUUID());
        winnerData.setBonusClaimBlocks(winnerData.getBonusClaimBlocks() + claim.getArea());
        claim.ownerID = winningBid.getBidderUUID();
        economy.withdrawPlayer(plugin.getServer().getOfflinePlayer(winningBid.getBidderUUID()), winningBid.getPrice());
        plugin.getLogger().info("Transferred claim to winning bid " + winningBid.toString());
    }

    /**
     * Finds a valid, winning bid in an auction.
     * @param auction
     * @return
     */
    private Bid findWinningBid(Auction auction)
    {
        for (Bid bid : auction.getBids())
        {
            OfflinePlayer player = Bukkit.getOfflinePlayer(bid.getBidderUUID());
            double balance = economy.getBalance(player);
            if (balance > bid.getPrice())
                return bid;
        }
        return null;
    }

    public static String format(double value)
    {
        return economy.format(value);
    }
}
