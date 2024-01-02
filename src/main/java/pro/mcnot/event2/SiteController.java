package pro.mcnot.event2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Set;

import org.bukkit.Bukkit;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class SiteController {
    
    private static final Main plugin = Main.getInstance();
    private String siteName;

    public SiteController (String siteName) {
        this.siteName = siteName;
    }

    public Boolean save() {
        if (verifySiteData(siteName) == false) return false;
        org.bukkit.World world = plugin.getServer().getWorld("world");
        BukkitWorld weWorld = new BukkitWorld(world);
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld);) {
            CuboidRegion region = getRegion(LocType.BACKUP);
            BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
            clipboard.setOrigin(region.getPos1());
            ForwardExtentCopy forwardExtentCopy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            Operations.complete(forwardExtentCopy);
            File file = new File(Main.getInstance().getDataFolder(), siteName + ".schem");
            try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(file))) {
                writer.write(clipboard);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean load() {
        if (verifySiteData(siteName) == false) return true;
        CuboidRegion region = getRegion(LocType.ACTUAL);
        BukkitWorld weWorld = new BukkitWorld(Bukkit.getWorld(region.getWorld().getName()));
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld);) {
            editSession.setBlocks((Region) region, (BlockStateHolder)BlockTypes.AIR.getDefaultState());
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld);) {
            Clipboard clipboard;
            File file = new File(Main.getInstance().getDataFolder(), siteName + ".schem");
            
            ClipboardFormat format = ClipboardFormats.findByFile(file);
            try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
                clipboard = reader.read();
                Operation operation = new ClipboardHolder(clipboard)
                         .createPaste(editSession)
                         .to(region.getPos1())
                         // configure here
                         .ignoreAirBlocks(true)
                         .build();
                Operations.complete(operation);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
            Main.getEventController().setSiteStatus(siteName, SiteStatus.RESTORED);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Boolean verifySiteData(String siteName) {
        // check siteName exists
        if (!(plugin.getConfig().getConfigurationSection("sites").getKeys(false).contains(siteName))) {
            Bukkit.broadcast("Error: site " + siteName + " does not exist in config.yml", "mcnotpro.admin");
            return false;
        }
        // check siteName has world, backupLoc, and actualLoc
        Set<String> siteSettings = plugin.getConfig().getConfigurationSection("sites." + siteName).getKeys(false);
        if (!(siteSettings.contains("world") || siteSettings.contains("backupLoc") || siteSettings.contains("actualLoc"))) {
            Bukkit.broadcast("Error: site " + siteName + " is missing world, backupLoc, or actualLoc in config.yml", "mcnotpro.admin");
            return false;
        }
        // check backupLoc has pos1 and pos2
        Set<String> backupLocSettings = plugin.getConfig().getConfigurationSection("sites." + siteName + ".backupLoc").getKeys(false);
        if (!(backupLocSettings.contains("pos1") || backupLocSettings.contains("pos2"))) {
            Bukkit.broadcast("Error: site " + siteName + " is missing pos1 or pos2 in config.yml", "mcnotpro.admin");
            return false;
        }
        // check actualLoc has pos1 and pos2
        Set<String> actualLocSettings = plugin.getConfig().getConfigurationSection("sites." + siteName + ".actualLoc").getKeys(false);
        if (!(actualLocSettings.contains("pos1") || actualLocSettings.contains("pos2"))) {
            Bukkit.broadcast("Error: site " + siteName + " is missing pos1 or pos2 in config.yml", "mcnotpro.admin");
            return false;
        }
        return true;
    }

    public String getSiteName() {
        return siteName;
    }

    public CuboidRegion getRegion(LocType type) {
        org.bukkit.World world;
        BukkitWorld weWorld;
        BlockVector3 min;
        BlockVector3 max;
        CuboidRegion region;
        try {
            world = plugin.getServer().getWorld(plugin.getConfig().getString("sites." + siteName + ".world"));
            weWorld = new BukkitWorld(world);
            String locKey = "sites." + siteName + "." + type.toString().toLowerCase() + "Loc";
            BlockVector3 loc1 = BlockVector3.at(plugin.getConfig().getDouble(locKey + ".pos1.x"), plugin.getConfig().getDouble(locKey + ".pos1.y"), plugin.getConfig().getDouble(locKey + ".pos1.z"));
            BlockVector3 loc2 = BlockVector3.at(plugin.getConfig().getDouble(locKey + ".pos2.x"), plugin.getConfig().getDouble(locKey + ".pos2.y"), plugin.getConfig().getDouble(locKey + ".pos2.z"));
            if(loc1.getBlockY()<loc2.getBlockY()) {
                min = loc1;
                max = loc2;
            }else {
                min = loc2;
                max = loc1;
            }
            region = new CuboidRegion(weWorld, min, max);
            return region;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public enum LocType {
        ACTUAL,
        BACKUP
    }

    public enum SiteStatus {
        RESTORED,
        NOT_RESTORED
    }
}
