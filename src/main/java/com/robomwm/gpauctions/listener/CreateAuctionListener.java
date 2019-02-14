package com.robomwm.gpauctions.listener;

import com.robomwm.gpauctions.Config;
import com.robomwm.gpauctions.GPAuctions;
import com.robomwm.gpauctions.auction.Auction;
import com.robomwm.gpauctions.auction.Auctioneer;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.DataStore;
import org.bukkit.ChatColor;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;
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
    private Set<String> signNames;

    public CreateAuctionListener(Plugin plugin, Auctioneer auctioneer, DataStore dataStore)
    {
        signNames = new HashSet<>(Config.SIGN_HEADER.asStringList());
        this.auctioneer = auctioneer;
        this.dataStore = dataStore;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event)
    {
        GPAuctions.debug("SignChangeEvent called");

        if (!signNames.contains(event.getLine(0).toLowerCase()))
            return;

        GPAuctions.debug("Line 1 matched sign");

        long endTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30);

        Claim claim = dataStore.getClaimAt(event.getBlock().getLocation(), true, null);
        if (claim == null)
            return;

        if (claim.parent != null)
        {
            GPAuctions.debug("Sign was placed in subclaim, canceling auction creation.");
            return;
        }

        GPAuctions.debug("Claim found");

        //only the claim owner can auction their claim. Check for admin claim permission for auctioning admin claims
        if (claim.ownerID == null && !event.getPlayer().hasPermission("griefprevention.adminclaims"))
            return;
        if (claim.ownerID != event.getPlayer().getUniqueId())
            return;

        double startingBid;
        try
        {
            startingBid = Double.parseDouble(event.getLine(1));
        }
        catch (NumberFormatException ignored)
        {
            startingBid = claim.getArea();
        }

        GPAuctions.debug("Set starting bid to " + startingBid);

        if (auctioneer.addAuction(new Auction(claim, endTime, startingBid, (Sign)event.getBlock().getState())))
        {
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes(
                    '&', "[&6GPAuctions&f] &bAuction has started with starting bid of &a") + startingBid);
            event.setCancelled(true);
        }
    }
}
