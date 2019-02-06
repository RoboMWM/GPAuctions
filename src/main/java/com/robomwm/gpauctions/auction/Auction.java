package com.robomwm.gpauctions.auction;

import com.robomwm.gpauctions.Config;
import com.robomwm.gpauctions.GPAuctions;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;

/**
 * Created on 1/20/2019.
 *
 * @author RoboMWM
 */
public class Auction implements ConfigurationSerializable
{
    private long claimID;
    private long endTime;
    private double startingBid;
    private UUID owner;
    private Stack<Bid> bids = new Stack<>();
    private Sign sign;

    public Auction(Claim claim, long endTime, double startingBid, Sign sign)
    {
        this.claimID = claim.getID();
        this.owner = claim.ownerID;
        this.endTime = endTime;
        this.startingBid = startingBid;
        this.sign = sign;
    }

    public Auction(Map<String, Object> map)
    {
        this.claimID = (int)map.get("claimID");
        String ownerUUID = (String)map.get("owner");
        if (ownerUUID != null)
            this.owner = UUID.fromString(ownerUUID);
        this.sign = (Sign)((Location)map.get("signLocation")).getBlock().getState();
        this.endTime = (long)map.get("endTime");
        this.startingBid = (double)map.get("startingBid");
        for (String unformattedBid : (List<String>)map.get("bids"))
        {
            String[] bid = unformattedBid.split(",");
            bids.push(new Bid(bid[0], Double.parseDouble(bid[1])));
        }
    }

    public void updateSign()
    {
        sign.setLine(0, "Real Estate");
        sign.setLine(1, ChatColor.DARK_GREEN + "Auction");
        String name = "";
        if (owner != null && Bukkit.getOfflinePlayer(owner) != null)
            name = Bukkit.getOfflinePlayer(owner).getName();
        sign.setLine(2, name);
        sign.setLine(3, Auctioneer.format(getNextBidPrice()));
        sign.update(false, false);
    }

    public double getNextBidPrice()
    {
        if (bids.isEmpty())
            return startingBid;
        else
            return bids.peek().getPrice() * Config.BID_PERCENTAGE.asDouble();
    }

    public UUID getOwner()
    {
        return owner;
    }

    public long getClaimID()
    {
        return claimID;
    }

    public boolean addBid(Bid bid)
    {
        if (isEnded())
            return false;
        if (bids.isEmpty() && bid.getPrice() < startingBid)
            return false;
        if (bid.getPrice() < bids.peek().getPrice())
            return false;

        bids.push(bid);
        updateSign();
        GPAuctions.debug("Added bid " + bid.toString() + " to auction " + this.toString());
        return true;
    }

    public boolean isEnded()
    {
        return endTime < System.currentTimeMillis();
    }

    private boolean cancelBid(UUID playerUUID)
    {
        throw new NotImplementedException();
    }

    public Stack<Bid> getBids()
    {
        return bids;
    }

    @Override
    public Map<String, Object> serialize()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("claimID", claimID);
        if (owner != null)
            map.put("owner", owner.toString());
        map.put("signLocation", sign.getLocation());
        map.put("endTime", endTime);
        map.put("startingBid", startingBid);
        List<String> bidsList = new ArrayList<>();
        for (Bid bid : bids)
            bidsList.add(bid.getBidderUUID() + "," + bid.getPrice());
        Collections.reverse(bids);
        map.put("bids", bidsList);
        return map;
    }

    @Override
    public String toString()
    {
        return "Claim ID: " + claimID +
                " endTime: " + endTime +
                " startingBid: " + startingBid;
    }
}
