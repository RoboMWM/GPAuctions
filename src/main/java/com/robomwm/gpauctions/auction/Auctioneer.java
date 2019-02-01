package com.robomwm.gpauctions.auction;

import com.robomwm.gpauctions.GPAuctions;
import com.robomwm.usefulutil.UsefulUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
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

    public Auctioneer(Plugin plugin)
    {
        this.plugin = plugin;
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
                for (Auction auction : auctions.values())
                {
                    //todo: check if ended
                    // transfer/delete claim
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
        plugin.getLogger().info("Auction started. " + auction.toString());
        auctions.put(auction.getClaimID(), auction);
        saveAuctions();
        return true;
    }

    public void saveAuctions()
    {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Auction auction : auctions.values())
            yaml.set(String.valueOf(auction.getClaimID()), auction);
        UsefulUtil.saveStringToFile(plugin, file, yaml.saveToString());
        GPAuctions.debug("Saved auctions to file.");
    }
}
