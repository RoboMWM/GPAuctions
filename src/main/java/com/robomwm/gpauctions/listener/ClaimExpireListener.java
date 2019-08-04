package com.robomwm.gpauctions.listener;

import com.robomwm.gpauctions.Config;
import com.robomwm.gpauctions.auction.Auction;
import com.robomwm.gpauctions.auction.Auctioneer;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.events.ClaimExpirationEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Created on 3/19/2019.
 *
 * @author RoboMWM
 */
public class ClaimExpireListener implements Listener
{
    private Auctioneer auctioneer;

    public ClaimExpireListener(Plugin plugin, Auctioneer auctioneer)
    {
        this.auctioneer = auctioneer;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    private void onExpire(ClaimExpirationEvent event)
    {
        event.setCancelled(true);

        if (auctioneer.getAuction(event.getClaim()) != null)
            return;

        Claim claim = event.getClaim();
        double startingBid = claim.getArea() * Config.DEFAULT_STARTING_PRICE_PER_CLAIMBLOCK.asInt();
        long endTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(Config.DEFAULT_AUCTION_DURATION_HOURS.asLong());        Sign sign = placeSign(claim);

        Auction auction = new Auction(claim, endTime, startingBid, sign);
        auctioneer.addAuction(auction);
    }

    private Sign placeSign(Claim claim)
    {
        Location location = claim.getLesserBoundaryCorner();
        Sign sign = null;

        while (sign == null)
        {
            //find highest block
            int y = location.getWorld().getHighestBlockYAt(location);

            //if highest block is at ceiling limit, try again along either x or z coordinate
            if (y >= 255)
            {
                if (ThreadLocalRandom.current().nextBoolean())
                    location.add(1, 0, 0);
                else
                    location.add(0, 0, 1);
                continue;
            }

            location.setY(y);
            location.getBlock().setType(Material.OAK_SIGN);
            sign = (Sign)location.getBlock().getState();
        }

        return sign;
    }
}
