package com.robomwm.gpauctions;

import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created on 2/4/2019.
 *
 * @author RoboMWM
 */
public enum Config
{
    BID_PERCENTAGE(0.1D),
    SIGN_HEADER(Arrays.asList("[auction claim]", "[ac]"));

    public static void initialize(Plugin plugin)
    {
        for (Config node : Config.values())
        {
            plugin.getConfig().addDefault(node.name().toLowerCase(), node.getValue());
        }

        plugin.getConfig().options().copyDefaults(true);
        plugin.saveConfig();

        for (Map.Entry<String, Object> entry : plugin.getConfig().getValues(false).entrySet())
        {
            try
            {
                Config.valueOf(entry.getKey().toUpperCase()).value = entry.getValue();
            }
            catch (NullPointerException | IllegalArgumentException e)
            {
                plugin.getLogger().warning("Invalid config option " + entry.getKey() + " was ignored. Consider removing this node from your config.yml");
            }
        }
    }

    private Object value;

    Config(Object value)
    {
        this.value = value;
    }

    public Object getValue()
    {
        return value;
    }

    public double asDouble()
    {
        return (double)value;
    }

    public List<String> asStringList()
    {
        return (List<String>)value;
    }
}
