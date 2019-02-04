package com.robomwm.gpauctions.auction;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Created on 1/20/2019.
 *
 * @author RoboMWM
 */
public class Bid
{
    private UUID bidderUUID;
    private double price;

    public Bid(Player player, double price)
    {
        this.bidderUUID = player.getUniqueId();
        this.price = price;
    }


    public Bid(String uuid, double price)
    {
        this.bidderUUID = UUID.fromString(uuid);
        this.price = price;
    }

    public double getPrice()
    {
        return price;
    }

    public UUID getBidderUUID()
    {
        return bidderUUID;
    }

    @Override
    public String toString()
    {
        return "UUID: " + bidderUUID.toString() +
                " Price: " + price;
    }
}
