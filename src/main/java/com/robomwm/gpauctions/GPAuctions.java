package com.robomwm.gpauctions;

import com.robomwm.gpauctions.auction.Auction;
import com.robomwm.gpauctions.auction.Auctioneer;
import com.robomwm.gpauctions.listener.CreateAuctionListener;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created on 1/20/2019.
 *
 * @author RoboMWM
 */
public class GPAuctions extends JavaPlugin
{
    @Override
    public void onEnable()
    {
        ConfigurationSerialization.registerClass(Auction.class);
        Auctioneer auctioneer = new Auctioneer(this);
        new CreateAuctionListener(this, auctioneer);
    }
}
