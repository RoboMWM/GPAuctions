package com.robomwm.gpauctions.command;

import com.robomwm.gpauctions.auction.Auction;
import com.robomwm.gpauctions.auction.Auctioneer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Created on 2/14/2019.
 *
 * @author RoboMWM
 */
public class CancelCommand implements CommandExecutor
{
    private Auctioneer auctioneer;

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        Player player = (Player)sender;
        Auction auction = auctioneer.getAuction(player.getLocation());
        if (auction == null)
        {
            sender.sendMessage("No auction is happening here.");
            return false;
        }

        if (hasPermission(auction.getOwner(), player))
        {
            player.sendMessage("This ain't your auction to cancel.");
            return true;
        }

        if (auctioneer.cancelAuction(auction))
            player.sendMessage("Successfully canceled auction.");

        return true;
    }

    public boolean hasPermission(UUID owner, Player player)
    {
        if (owner == player.getUniqueId())
            return true;
        return owner == null && player.hasPermission("griefprevention.adminclaims");
    }
}
