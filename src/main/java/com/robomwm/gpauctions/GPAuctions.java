package com.robomwm.gpauctions;

import com.robomwm.gpauctions.auction.Auction;
import com.robomwm.gpauctions.auction.Auctioneer;
import com.robomwm.gpauctions.command.CancelCommand;
import com.robomwm.gpauctions.listener.ClaimExpireListener;
import com.robomwm.gpauctions.listener.CreateAuctionListener;
import com.robomwm.gpauctions.listener.MakeBidListener;
import me.ryanhamshire.GriefPrevention.DataStore;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.Plugin;
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
        Config.initialize(this);

        DataStore dataStore = ((GriefPrevention)(this.getServer().getPluginManager().getPlugin("GriefPrevention"))).dataStore;
        
        ConfigurationSerialization.registerClass(Auction.class);
        Auctioneer auctioneer = new Auctioneer(this, dataStore);
        new CreateAuctionListener(this, auctioneer, dataStore);
        new MakeBidListener(this, auctioneer);
        new ClaimExpireListener(this, auctioneer);
        getCommand("gpacancel").setExecutor(new CancelCommand(auctioneer));
    }

    public static void debug(Object object)
    {
        System.out.println("[GPAuctions] " + object);
    }

    public static String smallFriendlyCoordinate(Location location)
    {
        return location.getBlockX() + ", " + location.getBlockZ();
    }

    public static void lazyCmdDispatcher(String command)
    {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), ChatColor.translateAlternateColorCodes('&', command));
    }
}
