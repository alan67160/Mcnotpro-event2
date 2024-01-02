package pro.mcnot.event2;

import org.bukkit.OfflinePlayer;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

public class PlaceholderAPIExpansion extends PlaceholderExpansion{
    
    private final Main plugin;

    public PlaceholderAPIExpansion(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getIdentifier() {
        return plugin.getDescription().getName();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if(params.equalsIgnoreCase("getTeam")) {
            Main.team team;
            if (Main.isPreAllocationTeam(player.getUniqueId())) {
                team = Main.getTeamPreAllocation(player.getUniqueId());
            } else {
                team = Main.getTeam(player.getUniqueId());
            }
            switch (team) {
                case admin:
                    return "§6";
                case blue:
                    if (Main.isPreAllocationTeam(player.getUniqueId()))
                        return "§1";
                    else
                        return "§9";
                case red:
                    if (Main.isPreAllocationTeam(player.getUniqueId()))
                        return "§4";
                    else
                        return "§c";
                case aqua:
                    if (Main.isPreAllocationTeam(player.getUniqueId()))
                        return "§3";
                    else
                        return "§b";
                case purple:
                    if (Main.isPreAllocationTeam(player.getUniqueId()))
                        return "§5";
                    else
                        return "§d";
                case green:
                    if (Main.isPreAllocationTeam(player.getUniqueId()))
                        return "§2";
                    else
                        return "§a";
                default:
                    return "§7";
            }
        }
        // unknown placeholder
        return null;
    }

}
