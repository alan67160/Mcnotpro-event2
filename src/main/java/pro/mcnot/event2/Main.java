package pro.mcnot.event2;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.boss.BarColor;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.util.StringUtil;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import pro.mcnot.event2.EventController.EventStatus;
import pro.mcnot.event2.SiteController.LocType;

import java.io.File;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Main extends JavaPlugin implements CommandExecutor, Listener {

    private static Main i;
    private FileConfiguration dataCfg;
    private static LinkedHashMap<UUID, team> teamMap = new LinkedHashMap<>();
    private static LinkedHashMap<UUID, team> teamPreAllocationMap = new LinkedHashMap<>();
    private LinkedList<UUID> allocationQueue = new LinkedList<>();
    private Integer allocationQueueTeamTarget = 0;
    private org.bukkit.boss.BossBar bossbarmsg;
    private org.bukkit.boss.BossBar bossbarTimerMsg;
    private NamespacedKey bossbarmsgKey = new NamespacedKey(this, "bossbarmsg");
    private NamespacedKey bossbarTimerMsgKey = new NamespacedKey(this, "bossbarTimerMsg");
    private static EventController eventController;

    @Override
    public void onEnable() {
        // Plugin startup logic
        i = this;
        // register commands
        this.getCommand("mcnotpro").setExecutor(this);
        this.getCommand("p").setExecutor(new SpecialItemPreviewCommand());
        // register events
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new SpecialItemPreviewCommand(), this);
        // load config
        saveDefaultConfig();
        dataFileReload();
        // register placeholder
        new PlaceholderAPIExpansion(this).register();
        // init
        eventController = new EventController();
        bossbarmsg = Bukkit.createBossBar(bossbarmsgKey, "", BarColor.GREEN, org.bukkit.boss.BarStyle.SOLID);                            
        bossbarTimerMsg = Bukkit.createBossBar(bossbarTimerMsgKey, "", BarColor.GREEN, org.bukkit.boss.BarStyle.SOLID);
        // give potion effect loop
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    try {
                        p.removePotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION);
                        p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.NIGHT_VISION, 600, 255, false, false));
                        p.setStatistic(Statistic.TIME_SINCE_REST, 0);
                    } catch (Exception ignore) {}
                }
            }
        }.runTaskTimer(this, 20, 200);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    try {
                        if (p.getInventory().getHelmet() != null) {
                            if (p.getInventory().getHelmet().getType() == Material.CARVED_PUMPKIN) {
                                p.removePotionEffect(org.bukkit.potion.PotionEffectType.GLOWING);
                                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 600, 255, true, true));
                            }
                        } else {
                            p.removePotionEffect(org.bukkit.potion.PotionEffectType.GLOWING);
                        }
                    } catch (Exception ignore) {}
                }
            }
        }.runTaskTimer(this, 20, 20);
        // update gamemode and fly loop
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    try {
                        if (teamMap.containsKey(p.getUniqueId())) {
                            if (teamMap.get(p.getUniqueId()) == team.admin) continue;
                            if (teamMap.get(p.getUniqueId()) != team.spectator) {
                                if (Bukkit.getPluginManager().getPlugin("CMI") != null) {
                                    CMI cmi = CMI.getInstance();
                                    CMIUser user = cmi.getPlayerManager().getUser(p);
                                    cmi.getNMS().changeGodMode(p, false);
                                    user.setTfly(0L);
                                    if (!(p.isOnline())) {
                                        user.setHadAllowFlight(false);
                                        user.setWasFlying(false);
                                        user.setFlying(false);
                                    }
                                }
                                p.setAllowFlight(false);
                                if (eventController.getStatus() == EventStatus.STARTED) {
                                    p.setGameMode(GameMode.SURVIVAL);
                                } else {
                                    p.setGameMode(GameMode.ADVENTURE);
                                }
                                continue;
                            }
                        }
                        if (Bukkit.getPluginManager().getPlugin("CMI") != null) {
                            CMI cmi = CMI.getInstance();
                            CMIUser user = cmi.getPlayerManager().getUser(p);
                            cmi.getNMS().changeGodMode(p, true);
                            user.setTfly(0L);
                            if (!(p.isOnline())) {
                                user.setHadAllowFlight(true);
                                user.setWasFlying(true);
                                user.setFlying(true);
                            }
                            p.setFoodLevel(20);
                            p.setSaturation(20);
                            p.setAllowFlight(true);
                            p.setGameMode(GameMode.ADVENTURE);
                        }
                    } catch (Exception ignore) {}
                }
            }
        }.runTaskTimer(this, 20, 1);
        // scoreboard loop
        new BukkitRunnable() {
            @Override
            public void run() {
                ScoreboardManager sm = Bukkit.getScoreboardManager();
                // create a Set<String> that has all the team name from scoreboard
                Set<String> sbteamName = new HashSet<>();
                for (org.bukkit.scoreboard.Team sbteam : sm.getMainScoreboard().getTeams()) {
                    sbteamName.add(sbteam.getName());
                }
                // create and init all the team
                for (team t : team.values()) {
                    if (!(sbteamName.contains(t.toString()))) {
                        sm.getMainScoreboard().registerNewTeam(t.toString());
                        sm.getMainScoreboard().getTeam(t.toString()).setAllowFriendlyFire(false);
                        sm.getMainScoreboard().getTeam(t.toString()).setCanSeeFriendlyInvisibles(true);
                        sm.getMainScoreboard().getTeam(t.toString()).setOption(org.bukkit.scoreboard.Team.Option.NAME_TAG_VISIBILITY, org.bukkit.scoreboard.Team.OptionStatus.FOR_OTHER_TEAMS);
                        sm.getMainScoreboard().getTeam(t.toString()).setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.ALWAYS);
                    }
                }
                // set team color
                for (team t : team.values()) {
                    org.bukkit.scoreboard.Team sbteam = sm.getMainScoreboard().getTeam(t.toString());
                    switch (t) {
                        case admin:
                            sbteam.setColor(org.bukkit.ChatColor.GOLD);
                            break;
                        case blue:
                            if (eventController.getStatus() == EventStatus.STARTED) {
                                sbteam.setColor(org.bukkit.ChatColor.DARK_BLUE);
                            } else {
                                sbteam.setColor(org.bukkit.ChatColor.BLUE);
                            }
                            break;
                        case red:
                            if (eventController.getStatus() == EventStatus.STARTED) {
                                sbteam.setColor(org.bukkit.ChatColor.DARK_RED);
                            } else {
                                sbteam.setColor(org.bukkit.ChatColor.RED);
                            }
                            break;
                        case aqua:
                            if (eventController.getStatus() == EventStatus.STARTED) {
                                sbteam.setColor(org.bukkit.ChatColor.DARK_AQUA);
                            } else {
                                sbteam.setColor(org.bukkit.ChatColor.AQUA);
                            }
                            break;
                        case purple:
                            if (eventController.getStatus() == EventStatus.STARTED) {
                                sbteam.setColor(org.bukkit.ChatColor.DARK_PURPLE);
                            } else {
                                sbteam.setColor(org.bukkit.ChatColor.LIGHT_PURPLE);
                            }
                            break;
                        default:
                            sbteam.setColor(org.bukkit.ChatColor.GRAY);
                            break;
                    }
                }
                // make sure all player is in the right team
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.setScoreboard(sm.getMainScoreboard());
                    if (teamMap.containsKey(p.getUniqueId())) {
                        if (teamMap.get(p.getUniqueId()) == team.admin) {
                            sm.getMainScoreboard().getTeam(team.admin.toString()).addEntry(p.getName());
                        } else if (teamMap.get(p.getUniqueId()) == team.blue) {
                            sm.getMainScoreboard().getTeam(team.blue.toString()).addEntry(p.getName());
                        } else if (teamMap.get(p.getUniqueId()) == team.red) {
                            sm.getMainScoreboard().getTeam(team.red.toString()).addEntry(p.getName());
                        } else if (teamMap.get(p.getUniqueId()) == team.aqua) {
                            sm.getMainScoreboard().getTeam(team.aqua.toString()).addEntry(p.getName());
                        } else if (teamMap.get(p.getUniqueId()) == team.purple) {
                            sm.getMainScoreboard().getTeam(team.purple.toString()).addEntry(p.getName());
                        } else if (teamMap.get(p.getUniqueId()) == team.green) {
                            sm.getMainScoreboard().getTeam(team.green.toString()).addEntry(p.getName());
                        } else {
                            sm.getMainScoreboard().getTeam(team.spectator.toString()).addEntry(p.getName());
                        }
                    } else if (teamPreAllocationMap.containsKey(p.getUniqueId())) {
                        if (teamPreAllocationMap.get(p.getUniqueId()) == team.blue) {
                            sm.getMainScoreboard().getTeam(team.blue.toString()).addEntry(p.getName());
                        } else if (teamPreAllocationMap.get(p.getUniqueId()) == team.red) {
                            sm.getMainScoreboard().getTeam(team.red.toString()).addEntry(p.getName());
                        } else if (teamPreAllocationMap.get(p.getUniqueId()) == team.aqua) {
                            sm.getMainScoreboard().getTeam(team.aqua.toString()).addEntry(p.getName());
                        } else if (teamPreAllocationMap.get(p.getUniqueId()) == team.purple) {
                            sm.getMainScoreboard().getTeam(team.purple.toString()).addEntry(p.getName());
                        } else if (teamPreAllocationMap.get(p.getUniqueId()) == team.green) {
                            sm.getMainScoreboard().getTeam(team.green.toString()).addEntry(p.getName());
                        } else {
                            sm.getMainScoreboard().getTeam(team.spectator.toString()).addEntry(p.getName());
                        }
                    } else {
                        sm.getMainScoreboard().getTeam(team.spectator.toString()).addEntry(p.getName());
                    }
                }
            }
        }.runTaskTimer(this, 20, 20);
        // replace item check loop
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Item item : Bukkit.getWorld("world").getEntitiesByClass(Item.class)) {
                    ItemStack is = item.getItemStack();
                    if ((is.getType() == Material.SNOW) || (is.getType().toString().contains("CARPET"))) {
                        int amount = item.getItemStack().getAmount();
                        int stack = 0;
                        // amount divide by 16
                        stack = amount / 16;
                        // amount mod 16
                        amount = amount % 16;
                        // drop snowball
                        for (int i = 0; i < stack; i++) {
                            item.getWorld().dropItemNaturally(item.getLocation(), new ItemStack(Material.SNOWBALL, 16)).setVelocity(new Vector(0, -1, 0));
                        }
                        item.getWorld().dropItemNaturally(item.getLocation(), new ItemStack(Material.SNOWBALL, amount)).setVelocity(new Vector(0, -1, 0));
                        // drop snow or carpet
                        item.remove();
                        return;
                    }
                    if ((is.getType() == Material.SNOW_BLOCK) || (is.getType().toString().contains("CONCRETE_POWDER"))) {
                        int amount = item.getItemStack().getAmount() * 4;
                        int stack = 0;
                        // amount divide by 16
                        stack = amount / 16;
                        // amount mod 16
                        amount = amount % 16;
                        // drop snowball
                        for (int i = 0; i < stack; i++) {
                            item.getWorld().dropItemNaturally(item.getLocation(), new ItemStack(Material.SNOWBALL, 16)).setVelocity(new Vector(0, -1, 0));
                        }
                        item.getWorld().dropItemNaturally(item.getLocation(), new ItemStack(Material.SNOWBALL, amount)).setVelocity(new Vector(0, -1, 0));
                        // drop snow block
                        item.remove();
                        return;
                    }
                    if ((is.getType() == Material.POINTED_DRIPSTONE) || (is.getType() == Material.SPRUCE_BUTTON)) {
                        item.remove();
                        return;
                    }
                }
            }
        }.runTaskTimer(this, 20, 5);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Item item : Bukkit.getWorld("world").getEntitiesByClass(Item.class)) {
                    Set<String> siteName = new HashSet<>();
                    try {
                        for (String site : getConfig().getConfigurationSection("sites").getKeys(false)) {
                            siteName.add(site);
                        }
                    } catch (NullPointerException ignore) {}
                    if (!siteName.isEmpty()) {
                        for (String site : siteName) {
                            try {
                                CuboidRegion region = (new SiteController(site)).getRegion(SiteController.LocType.ACTUAL);
                                region.contract(BlockVector3.at(1, 0, 1));
                                CuboidRegion regionBelow = region.clone();
                                // shift regionBelow to bottom of region
                                regionBelow.shift(BlockVector3.at(0, -regionBelow.getHeight(), 0));
                                regionBelow.shift(BlockVector3.at(0, -1, 0));
                                if (regionBelow.contains(BukkitAdapter.asBlockVector(item.getLocation()))) {
                                    if (item.getItemStack().getType() == Material.SNOWBALL) {
                                        for (int i = 0; i < item.getItemStack().getAmount(); i++) {
                                            Location targetLoc = getRandomLoc(region, false);
                                            Snowball ent = (Snowball) item.getWorld().spawnEntity(targetLoc, EntityType.SNOWBALL);
                                            try {
                                                if (item.getThrower() != null) {
                                                    if (Bukkit.getEntity(item.getThrower()).getType() == EntityType.PLAYER) ent.setShooter((Player) Bukkit.getEntity(item.getThrower()));
                                                }
                                            } catch (Exception ignore) {}
                                            ent.setVelocity(new Vector(0, -2, 0));
                                        }
                                        item.remove();
                                        return;
                                    } else {
                                        Location targetLoc = getRandomLoc(region, true);
                                        if (item.teleport(targetLoc)) {
                                            item.setVelocity(new Vector(0, -1, 0));
                                        }
                                        return;
                                    }
                                }
                            } catch (Exception ignore) {}
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20, 5*20);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (FallingBlock fallingBlock : Bukkit.getWorld("world").getEntitiesByClass(FallingBlock.class)) {
                    fallingBlock.getWorld().dropItemNaturally(fallingBlock.getLocation(), new ItemStack(Material.SNOWBALL, 4)).setVelocity(new Vector(0, -1, 0));
                    fallingBlock.remove();
                }
            }
        }.runTaskTimer(this, 20, 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    Integer specialItemCount = 0;
                    for (ItemStack is : p.getInventory().getContents()) {
                        if (is == null) continue;
                        ItemMeta im = is.getItemMeta();
                        if (im != null) {
                            if (im.getPersistentDataContainer().has(new NamespacedKey(Main.getInstance(), "preview"), PersistentDataType.STRING)) {
                                p.getInventory().remove(is);
                            }
                            if (im.getPersistentDataContainer().has(new NamespacedKey(Main.getInstance(), "RandomId"), PersistentDataType.STRING)) {
                                if (im.getPersistentDataContainer().get(new NamespacedKey(Main.getInstance(), "RandomId"), PersistentDataType.STRING).equals("s-1667032441")) {
                                    if (im.getPersistentDataContainer().has(new NamespacedKey(Main.getInstance(), "pickUpLimit"), PersistentDataType.STRING)) {
                                        if (specialItemCount >= Integer.parseInt(im.getPersistentDataContainer().get(new NamespacedKey(Main.getInstance(), "pickUpLimit"), PersistentDataType.STRING))) {
                                            for (int i = 0; i < specialItemCount; i++) {
                                                p.getWorld().dropItemNaturally(p.getLocation(), is).setVelocity(new Vector(0, -1, 0));
                                            }
                                            p.getWorld().dropItemNaturally(p.getLocation(), is).setVelocity(new Vector(0, -1, 0));
                                            p.getInventory().remove(is);
                                            Bukkit.broadcastMessage("§6【§e系統§6】§c警告! 玩家 " + p.getName() + " 試圖利用漏洞擁有超過限制的物品數量!");
                                            Bukkit.broadcastMessage("§6【§e系統§6】§c我們都必須代表中方表示強烈譴責!");
                                        }
                                        specialItemCount++;
                                    }
                                }
                                
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20, 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!(teamMap.containsKey(p.getUniqueId()))) {
                        teamMap.put(p.getUniqueId(), team.spectator);
                    }
                    if (teamMap.get(p.getUniqueId()) == team.spectator) {
                        eventController.teleportSpectator(p.getUniqueId());
                    }
                }
            }
        }.runTaskLater(this, 30*20);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        dataFileSave();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        try {
            if (sender.hasPermission("mcnotpro.admin")) {
                if (args.length == 0) {
                    sender.sendMessage("§6【§e系統§6】§f/mcnotpro reload");
                    return true;
                }
                if (args[0].equalsIgnoreCase("reload")) {
                    reloadConfig();
                    dataFileReload();
                    eventController = new EventController();
                    sender.sendMessage("§6【§e系統§6】§f設定檔重新載入成功!");
                    return true;
                }
                if (args[0].equalsIgnoreCase("site")) {
                    if (!(checkSiteName(args[1]))) {
                        sender.sendMessage("§6【§e系統§6】§f找不到該場地!");
                        return true;
                    }
                    SiteController siteController = new SiteController(args[1]);
                    if (args[2].equalsIgnoreCase("save")) {
                        Bukkit.broadcastMessage("§6【§e系統§6】§f場地儲存中...");
                        if (siteController.save()) {
                            Bukkit.broadcastMessage("§6【§e系統§6】§a場地儲存成功!");
                            return true;
                        } else {
                            Bukkit.broadcastMessage("§6【§e系統§6】§c場地儲存失敗!");
                            return true;
                        }
                    }
                    if (args[2].equalsIgnoreCase("load")) {
                        Bukkit.broadcastMessage("§6【§e系統§6】§f場地載入中...");
                        if (siteController.load()) {
                            Bukkit.broadcastMessage("§6【§e系統§6】§a場地載入成功!");
                            return true;
                        } else {
                            Bukkit.broadcastMessage("§6【§e系統§6】§c場地載入失敗!");
                            return true;
                        }
                    }
                }
                if (args[0].equalsIgnoreCase("event")) {
                    if (args[1].equalsIgnoreCase("restoreBypass")) {
                        eventController.setSiteRestoredBypass(true);
                        sender.sendMessage("§6【§e系統§6】§f已繞過安全機制!");
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("start")) {
                        if (args.length == 3) {
                            if (args[2].equalsIgnoreCase("-confirm-")) {
                                if ((eventController.getSiteStatus(eventController.getSite()) == SiteController.SiteStatus.NOT_RESTORED) && (!(eventController.getSiteRestoredBypass()))) {
                                    sender.sendMessage("§6【§e系統§6】§c已觸發安全機制! 原因: 場地尚未還原");
                                    sender.sendMessage("§6【§e系統§6】§f使用指令 /mcnotpro event restoreBypass 繞過此安全機制");
                                    return true;
                                }
                                eventController.countdown();
                                return true;
                            }
                        }
                        // send all the info in settings
                        sender.sendMessage("§6【§e系統§6】§f=============目前設定=============");
                        sender.sendMessage("§6【§e系統§6】§f場地: " + eventController.getSite());
                        if (eventController.getSiteStatus(eventController.getSite()) == SiteController.SiteStatus.RESTORED) {
                            sender.sendMessage("§6【§e系統§6】§f場地狀態: §a已還原");
                        } else {
                            if (eventController.getSiteRestoredBypass()) {
                                sender.sendMessage("§6【§e系統§6】§f場地狀態: §c尚未還原 §a(已繞過安全機制)");
                            } else {
                                sender.sendMessage("§6【§e系統§6】§f場地狀態: §c尚未還原");
                            }
                        }
                        sender.sendMessage("§6【§e系統§6】§f遊戲時間: " + eventController.getTimeLimit() + " 秒");
                        if (eventController.getDecay()) {
                            sender.sendMessage("§6【§e系統§6】§f方塊衰變: §c開啟");
                            sender.sendMessage("§6【§e系統§6】§f方塊衰變時間: " + eventController.getDecayTime() + " tick(" + (eventController.getDecayTime()*0.05) + "秒)");
                            sender.sendMessage("§6【§e系統§6】§f方塊衰變方塊: " + eventController.getDecayBlock() + " 塊");
                        } else {
                            sender.sendMessage("§6【§e系統§6】§f方塊衰變: §a關閉");
                        }
                        if (eventController.getSpecialItemDropTime() > 0) {
                            sender.sendMessage("§6【§e系統§6】§f特殊物品掉落時間: " + eventController.getSpecialItemDropTime() + " 秒");
                        } else {
                            sender.sendMessage("§6【§e系統§6】§f特殊物品掉落時間: §c關閉");
                        }
                        Set<team> teamSet = new HashSet<>();
                        for (team t : teamPreAllocationMap.values()) {
                            if (t == team.admin) continue;
                            if (t == team.spectator) continue;
                            teamSet.add(t);
                        }
                        for (team t : teamSet) {
                            if (t == team.admin) continue;
                            if (t == team.spectator) continue;
                            List<String> playerList = new ArrayList<>();
                            for (UUID uuid : teamPreAllocationMap.keySet()) {
                                if (teamPreAllocationMap.get(uuid) == t) {
                                    if (Bukkit.getOfflinePlayer(uuid).getName() == null) continue;
                                    playerList.add(Bukkit.getOfflinePlayer(uuid).getName());
                                }
                            }
                            sender.sendMessage("§6【§e系統§6】§f" + getTeamTranslate(t) + "(" + Collections.frequency(teamPreAllocationMap.values(), t) + "人): " + playerList);
                        }
                        sender.sendMessage("§6【§e系統§6】§f================================");
                        // send clickable text
                        TextComponent button = new TextComponent();
                        button.setText("§a[確認設定並開始活動]");
                        button.setBold(true);
                        button.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/mcnotpro event start -confirm-"));
                        button.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.hover.content.Text("§a讓我們開始吧!")));
                        BaseComponent[] massage = 
                            new ComponentBuilder("§6【§e系統§6】§f")
                            .append(button)
                            .create();
                        sender.spigot().sendMessage(massage);
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("settings")) {
                        if (args[2].equalsIgnoreCase("set")) {
                            if (args[3].equalsIgnoreCase("site")) {
                                if (!(checkSiteName(args[4]))) {
                                    sender.sendMessage("§6【§e系統§6】§f找不到該場地!");
                                    return true;
                                }
                                if (eventController.setSite(args[4])) {
                                    sender.sendMessage("§6【§e系統§6】§f已成功設定場地為 " + args[4] + " !");
                                    return true;
                                } else {
                                    sender.sendMessage("§6【§e系統§6】§f設定場地失敗!");
                                    return true;
                                }
                            }
                            if (args[3].equalsIgnoreCase("timeLimit")) {
                                try {
                                    if (eventController.setTimeLimit(Long.parseLong(args[4]))) {
                                        sender.sendMessage("§6【§e系統§6】§f已成功設定遊戲時間為 " + args[4] + " 秒!");
                                        return true;
                                    } else {
                                        sender.sendMessage("§6【§e系統§6】§f設定遊戲時間失敗!");
                                        return true;
                                    }
                                } catch (NumberFormatException nfe) {
                                    sender.sendMessage("§6【§e系統§6】§f請輸入數字!");
                                }
                            }
                            if (args[3].equalsIgnoreCase("decaying")) {
                                if (args[4].equalsIgnoreCase("true")) {
                                    if (eventController.setDecay(true)) {
                                        sender.sendMessage("§6【§e系統§6】§f已成功設定方塊衰變為開啟!");
                                        return true;
                                    } else {
                                        sender.sendMessage("§6【§e系統§6】§f設定方塊衰變失敗!");
                                        return true;
                                    }
                                }
                                if (args[4].equalsIgnoreCase("false")) {
                                    if (eventController.setDecay(false)) {
                                        sender.sendMessage("§6【§e系統§6】§f已成功設定方塊衰變為關閉!");
                                        return true;
                                    } else {
                                        sender.sendMessage("§6【§e系統§6】§f設定方塊衰變失敗!");
                                        return true;
                                    }
                                }
                                sender.sendMessage("§6【§e系統§6】§f/mcnotpro event settings set decaying <true/false>");
                            }
                            if (args[3].equalsIgnoreCase("decayingTime")) {
                                try {
                                    if (eventController.setDecayTime(Integer.parseInt(args[4]))) {
                                        sender.sendMessage("§6【§e系統§6】§f已成功設定方塊衰變時間為 " + args[4] + " tick(" + (eventController.getDecayTime()*0.05) + "秒)!");
                                        return true;
                                    } else {
                                        sender.sendMessage("§6【§e系統§6】§f設定方塊衰變時間失敗!");
                                        return true;
                                    }
                                } catch (NumberFormatException nfe) {
                                    sender.sendMessage("§6【§e系統§6】§f請輸入數字!");
                                }
                            }
                            if (args[3].equalsIgnoreCase("decayingBlock")) { 
                                try {
                                    if (eventController.setDecayBlock(Integer.parseInt(args[4]))) {
                                        sender.sendMessage("§6【§e系統§6】§f已成功設定方塊衰變方塊為 " + args[4] + " 塊!");
                                        return true;
                                    } else {
                                        sender.sendMessage("§6【§e系統§6】§f設定方塊衰變方塊失敗!");
                                        return true;
                                    }
                                } catch (NumberFormatException nfe) {
                                    sender.sendMessage("§6【§e系統§6】§f請輸入數字!");
                                }
                            }
                            if (args[3].equalsIgnoreCase("specialItemDropTime")) {
                                try {
                                    if (eventController.setSpecialItemDropTime(Integer.parseInt(args[4]))) {
                                        sender.sendMessage("§6【§e系統§6】§f已成功重置特殊物品掉落時間");
                                        sender.sendMessage("§6【§e系統§6】§f已成功設定特殊物品掉落時間為 " + args[4] + " 秒!");
                                        return true;
                                    } else {
                                        sender.sendMessage("§6【§e系統§6】§f設定特殊物品掉落時間失敗!");
                                        return true;
                                    }
                                } catch (NumberFormatException nfe) {
                                    sender.sendMessage("§6【§e系統§6】§f請輸入數字!");
                                }
                            }
                        }
                        if (args[2].equalsIgnoreCase("get")) {
                            if (args[3].equalsIgnoreCase("site")) {
                                sender.sendMessage("§6【§e系統§6】§f目前場地為 " + eventController.getSite() + " !");
                                return true;
                            }
                            if (args[3].equalsIgnoreCase("timeLimit")) {
                                sender.sendMessage("§6【§e系統§6】§f目前遊戲時間為 " + eventController.getTimeLimit() + " 秒!");
                                return true;
                            }
                            if (args[3].equalsIgnoreCase("decaying")) {
                                sender.sendMessage("§6【§e系統§6】§f目前方塊衰變為 " + eventController.getDecay() + " !");
                                sender.sendMessage("§6【§e系統§6】§f目前方塊衰變時間為 " + eventController.getDecayTime() + " tick(" + (eventController.getDecayTime()*0.05) + "秒)!");
                                sender.sendMessage("§6【§e系統§6】§f目前方塊衰變方塊為 " + eventController.getDecayBlock() + " 塊!");
                                return true;
                            }
                            if (args[3].equalsIgnoreCase("specialItemDropTime")) {
                                sender.sendMessage("§6【§e系統§6】§f目前特殊物品掉落時間為 " + eventController.getSpecialItemDropTime() + " 秒!");
                                return true;
                            }
                            if (args[3].equalsIgnoreCase("all")) {
                                sender.sendMessage("§6【§e系統§6】§f目前場地為 " + eventController.getSite() + " !");
                                sender.sendMessage("§6【§e系統§6】§f目前遊戲時間為 " + eventController.getTimeLimit() + " 秒!");
                                sender.sendMessage("§6【§e系統§6】§f目前方塊衰變為 " + eventController.getDecay() + " !");
                                sender.sendMessage("§6【§e系統§6】§f目前方塊衰變時間為 " + eventController.getDecayTime() + " tick(" + (eventController.getDecayTime()*0.05) + "秒)!");
                                sender.sendMessage("§6【§e系統§6】§f目前方塊衰變方塊為 " + eventController.getDecayBlock() + " 塊!");
                                sender.sendMessage("§6【§e系統§6】§f目前特殊物品掉落時間為 " + eventController.getSpecialItemDropTime() + " 秒!");
                                return true;
                            }
                            sender.sendMessage("§6【§e系統§6】§f/mcnotpro event settings get <site/timeLimit/decaying>");
                        }
                    }
                    if (args[1].equalsIgnoreCase("forceEnd")) {
                        eventController.stop();
                        sender.sendMessage("§6【§e系統§6】§f已強制結束遊戲!");
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("forceStopCounddown")) {
                        eventController.stopCounddown();
                        sender.sendMessage("§6【§e系統§6】§f已強制結束倒數!");
                        return true;
                    }
                }
                if (args[0].equalsIgnoreCase("team")) {
                    if (args[1].equalsIgnoreCase("clear")) {
                        teamMap.clear();
                        sender.sendMessage("§6【§e系統§6】§f已清除隊伍!");
                        return true;
                    }
                    Player target = getServer().getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage("§6【§e系統§6】§f找不到該玩家!");
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("get")) {
                        if (teamMap.containsKey(target.getUniqueId())) {
                            sender.sendMessage("§6【§e系統§6】§f" + target.getName() + " 在 " + getTeamTranslate(teamMap.get(target.getUniqueId())) + " 隊!");
                            return true;
                        } else {
                            sender.sendMessage("§6【§e系統§6】§f" + target.getName() + " 在 " + getTeamTranslate(team.spectator) + " 隊!");
                            return true;
                        }
                    }
                    if (args[1].equalsIgnoreCase("set")) {
                        if (args[3].equalsIgnoreCase("blue")) {
                            teamMap.put(target.getUniqueId(), team.blue);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到" + getTeamTranslate(team.blue) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("red")) {
                            teamMap.put(target.getUniqueId(), team.red);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到" + getTeamTranslate(team.red) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("aqua")) {
                            teamMap.put(target.getUniqueId(), team.aqua);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到" + getTeamTranslate(team.aqua) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("purple")) {
                            teamMap.put(target.getUniqueId(), team.purple);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到" + getTeamTranslate(team.purple) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("green")) {
                            teamMap.put(target.getUniqueId(), team.green);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到" + getTeamTranslate(team.green) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("admin")) {
                            teamMap.put(target.getUniqueId(), team.admin);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到" + getTeamTranslate(team.admin) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("spectator")) {
                            teamMap.put(target.getUniqueId(), team.spectator);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到" + getTeamTranslate(team.spectator) + "!");
                            return true;
                        }
                    }
                    sender.sendMessage("§6【§e系統§6】§f/mcnotpro team set <player> <team>");
                    return true;
                }
                if (args[0].equalsIgnoreCase("preAllocationTeam")) {
                    if (args[1].equalsIgnoreCase("clear")) {
                        teamPreAllocationMap.clear();
                        sender.sendMessage("§6【§e系統§6】§f已清除預分組隊伍!");
                        return true;
                    }
                    Player target = getServer().getPlayer(args[2]);
                    if (target == null) {
                        sender.sendMessage("§6【§e系統§6】§f找不到該玩家!");
                        return true;
                    }
                    if (args[1].equalsIgnoreCase("get")) {
                        if (teamPreAllocationMap.containsKey(target.getUniqueId())) {
                            sender.sendMessage("§6【§e系統§6】§f" + target.getName() + " 在 " + getTeamTranslate(teamPreAllocationMap.get(target.getUniqueId())) + " 預分組隊伍中!");
                            return true;
                        } else {
                            sender.sendMessage("§6【§e系統§6】§f" + target.getName() + " 不在任何預分組隊伍中!");
                            return true;
                        }
                    }
                    if (args[1].equalsIgnoreCase("set")) {
                        if (args[3].equalsIgnoreCase("blue")) {
                            teamPreAllocationMap.put(target.getUniqueId(), team.blue);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到預分組" + getTeamTranslate(team.blue) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("red")) {
                            teamPreAllocationMap.put(target.getUniqueId(), team.red);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到預分組" + getTeamTranslate(team.red) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("aqua")) {
                            teamPreAllocationMap.put(target.getUniqueId(), team.aqua);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到預分組" + getTeamTranslate(team.aqua) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("purple")) {
                            teamPreAllocationMap.put(target.getUniqueId(), team.purple);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到預分組" + getTeamTranslate(team.purple) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("green")) {
                            teamPreAllocationMap.put(target.getUniqueId(), team.green);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到預分組" + getTeamTranslate(team.green) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("admin")) {
                            teamPreAllocationMap.put(target.getUniqueId(), team.admin);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到預分組" + getTeamTranslate(team.admin) + "!");
                            return true;
                        }
                        if (args[3].equalsIgnoreCase("spectator")) {
                            teamPreAllocationMap.put(target.getUniqueId(), team.spectator);
                            sender.sendMessage("§6【§e系統§6】§f已成功設定 " + target.getName() + " 到預分組" + getTeamTranslate(team.spectator) + "隊!");
                            return true;
                        }
                    }
                    sender.sendMessage("§6【§e系統§6】§f/mcnotpro preAllocationTeam set <player> <team>");
                    return true;
                }
                // if (args[0].equalsIgnoreCase("allocationQueue")) {
                //     if (args[1].equalsIgnoreCase("get")) {
                //         Set<String> pName = new HashSet<>();
                //         for (UUID pUUID : allocationQueue) {
                //             pName.add(Bukkit.getOfflinePlayer(pUUID).getName());
                //         }
                //         sender.sendMessage("§6【§e系統§6】§f隊列: " + String.join(", ", pName));
                //         return true;
                //     }
                //     if (args[1].equalsIgnoreCase("clear")) {
                //         allocationQueue.clear();
                //         sender.sendMessage("§6【§e系統§6】§f已清除隊列!");
                //         return true;
                //     }
                //     if (args[1].equalsIgnoreCase("setTarget")) {
                //         try {
                //             allocationQueueTeamTarget = Integer.parseInt(args[2]);
                //         } catch (NumberFormatException nfe) {
                //             sender.sendMessage("§6【§e系統§6】§c請輸入數字!");
                //         }
                //     }
                //     if (args[1].equalsIgnoreCase("run")) {
                //         if (allocationQueueTeamTarget <= 0) {
                //             sender.sendMessage("§6【§e系統§6】§c請先設定隊伍數量!");
                //             return true;
                //         }
                //         if (allocationQueue.size() % allocationQueueTeamTarget != 0) {
                //             sender.sendMessage("§6【§e系統§6】§c隊列人數沒辦法完整分成指定的隊伍數量!");
                //             LinkedList<Integer> possibleTeamTarget = new LinkedList<>();
                //             for (int i = 1; i <= team.values().length; i++) {
                //                 if (allocationQueue.size() % i == 0) {
                //                     possibleTeamTarget.add(i);
                //                 }
                //             }
                //             if (possibleTeamTarget.size() == 0) {
                //                 sender.sendMessage("§6【§e系統§6】§c無法分成任何隊伍數量!");
                //                 return true;
                //             }
                //             sender.sendMessage("§6【§e系統§6】§f可用隊伍數量: " + String.join(", ", possibleTeamTarget.stream().map(Object::toString).collect(Collectors.toList())));
                //             return true;
                //         }
                //         SecureRandom rand = new SecureRandom();
                //         LinkedHashMap<team, UUID> tempAllocationQueueTeamMap = new LinkedHashMap<>();
                //         LinkedList<team> allocationQueueTeam = new LinkedList<>();
                //         LinkedList<team> possibleTeam = new LinkedList<>();
                //         // team[] to LinkedList
                //         for (team t : team.values()) {
                //             if (t == team.admin) continue;
                //             if (t == team.spectator) continue;
                //             possibleTeam.add(t);
                //         }
                //         while ((allocationQueueTeam.size() + 1) < allocationQueueTeamTarget) {
                //             allocationQueueTeam.add(possibleTeam.get(rand.nextInt(possibleTeam.size())));
                //         }
                //         // for allocationQueueTeam
                        
                //     }
                //     if (args[1].equalsIgnoreCase("add")) {
                //         Player target = getServer().getPlayer(args[2]);
                //         if (target == null) {
                //             sender.sendMessage("§6【§e系統§6】§f找不到該玩家!");
                //             return true;
                //         }
                //         if (allocationQueue.contains(target.getUniqueId())) {
                //             sender.sendMessage("§6【§e系統§6】§f" + target.getName() + " 已在隊列中!");
                //             return true;
                //         }
                //         allocationQueue.add(target.getUniqueId());
                //         sender.sendMessage("§6【§e系統§6】§f已成功將 " + target.getName() + " 加入隊列!");
                //         return true;
                //     }
                //     if (args[1].equalsIgnoreCase("remove")) {
                //         Player target = getServer().getPlayer(args[2]);
                //         if (target == null) {
                //             sender.sendMessage("§6【§e系統§6】§f找不到該玩家!");
                //             return true;
                //         }
                //         if (!(allocationQueue.contains(target.getUniqueId()))) {
                //             sender.sendMessage("§6【§e系統§6】§f" + target.getName() + " 不在隊列中!");
                //             return true;
                //         }
                //         allocationQueue.remove(target.getUniqueId());
                //         sender.sendMessage("§6【§e系統§6】§f已成功將 " + target.getName() + " 移出隊列!");
                //         return true;
                //     }
                // }
                if (args[0].equalsIgnoreCase("specialFeather")) {
                    Player target = getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§6【§e系統§6】§f找不到該玩家!");
                        return true;
                    }
                    if (target.getSaturation() > 0) {
                        target.setSaturation(target.getSaturation() - 2);
                    } else if (target.getFoodLevel() > 0) {
                        target.setFoodLevel(target.getFoodLevel() - 2);
                    } else {
                        World w = target.getWorld();
                        Entity entity = w.spawnEntity(new Location(w, 0, 0, 0), EntityType.MINECART_COMMAND);
                        boolean gameRule = w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
                        w.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "damage " + target.getName() + " 4 minecraft:starve");
                        w.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, gameRule);
                        entity.remove();
                        return true;
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi launch " + target.getName() + " p:0.5 a:90 -s");
                    return true;
                }
                if (args[0].equalsIgnoreCase("void")) {
                    Player target = getServer().getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§6【§e系統§6】§f找不到該玩家!");
                        return true;
                    }
                    if (getTeam(target.getUniqueId()) == team.admin) return true;
                    if (getTeam(target.getUniqueId()) == team.spectator) return true;
                    SecureRandom rand = new SecureRandom();
                    World w = target.getWorld();
                    Entity entity = w.spawnEntity(new Location(w, 0, 0, 0), EntityType.MINECART_COMMAND);
                    boolean gameRule = w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK);
                    w.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
                    if ((rand.nextInt(100) <= 10) && (new SiteController(eventController.getSite()).getRegion(LocType.ACTUAL).contains(BukkitAdapter.asBlockVector(target.getLocation())))) { // 10% 
                        Bukkit.dispatchCommand(entity, "damage " + target.getName() + " 200 minecraft:bad_respawn_point");
                        // create a small explosion at target loc
                        target.getWorld().createExplosion(target.getLocation(), 1, false, false);
                    } else {
                        Bukkit.dispatchCommand(entity, "damage " + target.getName() + " 200 minecraft:outside_border");
                    }
                    w.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, gameRule);
                    entity.remove();
                    return true;
                }
            }
        } catch (IndexOutOfBoundsException e) {
            sender.sendMessage("§6【§e系統§6】§f/mcnotpro reload");
            sender.sendMessage("§6【§e系統§6】§f/mcnotpro site <siteName> <load/save>");
            sender.sendMessage("§6【§e系統§6】§f/mcnotpro team set <player> <team>");
            sender.sendMessage("§6【§e系統§6】§f/mcnotpro preAllocationTeam set <player> <team>");
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (sender.hasPermission("mcnotpro.admin")) {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], Arrays.asList(new String[]{"reload", "team", "preAllocationTeam", "site", "event"}), new ArrayList<>());
            }
            if ((args.length == 2) && (args[0].equals("site"))) {
                // create a Set<String> that has all the site name from config
                Set<String> siteName = new HashSet<>();
                try {
                    for (String site : getConfig().getConfigurationSection("sites").getKeys(false)) {
                        siteName.add(site);
                    }
                } catch (NullPointerException ignore) {}
                if (siteName.isEmpty()) return Collections.emptyList();
                return StringUtil.copyPartialMatches(args[1], siteName, new ArrayList<>());
            }
            if ((args.length == 2) && (args[0].equals("event"))) {
                return StringUtil.copyPartialMatches(args[1], Arrays.asList(new String[]{"start", "settings", "forceEnd", "forceStopCounddown"}), new ArrayList<>());
            }
            if ((args.length == 2) && ((args[0].equals("team")) || (args[0].equals("preAllocationTeam")))) {
                return StringUtil.copyPartialMatches(args[1], Arrays.asList(new String[]{"clear", "set", "get"}), new ArrayList<>());
            }
            if ((args.length == 3) && (args[0].equals("site"))) {
                return StringUtil.copyPartialMatches(args[2], Arrays.asList(new String[]{"load", "save"}), new ArrayList<>());
            }
            if ((args.length == 3) && ((args[0].equals("team")) || (args[0].equals("preAllocationTeam"))) && ((args[1].equals("set")) || (args[1].equals("get")))) {
                List<String> omlineplayer = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    omlineplayer.add(p.getName());
                }
                return StringUtil.copyPartialMatches(args[2], omlineplayer, new ArrayList<>());
            }
            if ((args.length == 3) && (args[0].equals("event")) && (args[1].equals("settings"))) {
                return StringUtil.copyPartialMatches(args[2], Arrays.asList(new String[]{"set", "get"}), new ArrayList<>());
            }
            if ((args.length == 4) && ((args[0].equals("team")) || (args[0].equals("preAllocationTeam"))) && (args[1].equals("set"))) {
                return StringUtil.copyPartialMatches(args[3], Arrays.asList(new String[]{"red", "blue", "aqua", "purple", "green", "admin", "spectator"}), new ArrayList<>());
            }
            if ((args.length == 4) && (args[0].equals("event")) && (args[1].equals("settings")) && (args[2].equals("set"))) {
                return StringUtil.copyPartialMatches(args[3], Arrays.asList(new String[]{"site", "timeLimit", "decaying", "decayingTime", "decayingBlock", "specialItemDropTime"}), new ArrayList<>());
            }
            if ((args.length == 4) && (args[0].equals("event")) && (args[1].equals("settings")) && (args[2].equals("get"))) {
                return StringUtil.copyPartialMatches(args[3], Arrays.asList(new String[]{"site", "timeLimit", "decaying", "specialItemDropTime", "all"}), new ArrayList<>());
            }
            if ((args.length == 5) && (args[0].equals("event")) && (args[1].equals("settings")) && (args[2].equals("set")) && (args[3].equals("site"))) {
                // create a Set<String> that has all the site name from config
                Set<String> siteName = new HashSet<>();
                try {
                    for (String site : getConfig().getConfigurationSection("sites").getKeys(false)) {
                        siteName.add(site);
                    }
                } catch (NullPointerException ignore) {}
                if (siteName.isEmpty()) return Collections.emptyList();
                return StringUtil.copyPartialMatches(args[4], siteName, new ArrayList<>());
            }
            if ((args.length == 5) && (args[0].equals("event")) && (args[1].equals("settings")) && (args[2].equals("set")) && (args[3].equals("decaying"))) {
                return StringUtil.copyPartialMatches(args[4], Arrays.asList(new String[]{"true", "false"}), new ArrayList<>());
            }
        } else {
            if (args.length == 1) {
                return StringUtil.copyPartialMatches(args[0], Arrays.asList(new String[]{"沒有權限的你在看什麼"}), new ArrayList<>());
            }
        }
        return Collections.emptyList();
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (e.getEntity().getType() != EntityType.PLAYER) e.setCancelled(true);
        Player p = (Player) e.getEntity();
        Location loc = e.getItem().getLocation();
        if (loc.getWorld() == null) return;
        if (!(teamMap.containsKey(p.getUniqueId()))) e.setCancelled(true);
        if (teamMap.get(p.getUniqueId()) == team.spectator) e.setCancelled(true);
        if (teamMap.get(p.getUniqueId()) == team.admin) return;
        Set<Material> blackListedItems = new HashSet<>();
        blackListedItems.add(Material.SNOW);
        blackListedItems.add(Material.SNOW_BLOCK);
        blackListedItems.add(Material.POINTED_DRIPSTONE);
        blackListedItems.add(Material.SPRUCE_BUTTON);
        if (blackListedItems.contains(e.getItem().getItemStack().getType())) {
            e.setCancelled(true);
            return;
        }
        if ((e.getItem().getItemStack().getType().toString().contains("CARPET")) || (e.getItem().getItemStack().getType().toString().contains("CONCRETE_POWDER"))) {
            e.setCancelled(true);
            return;
        }
        ItemStack is = e.getItem().getItemStack();
        ItemMeta im = is.getItemMeta();
        if (im.getPersistentDataContainer().has(new NamespacedKey(this, "pickUpLimit"), PersistentDataType.STRING)) {
            // check player inventory
            // if there has same item with same tag
            // and having 2 or more then 2
            // then cancel
            int amount = 0;
            for (ItemStack item : p.getInventory().getContents()) {
                if (item != null) {
                    if (item.getType() == is.getType()) {
                        if (item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(this, "pickUpLimit"), PersistentDataType.STRING)) {
                            amount += item.getAmount();
                        }
                    }
                }
            }
            if (amount >= Integer.parseInt(im.getPersistentDataContainer().get(new NamespacedKey(this, "pickUpLimit"), PersistentDataType.STRING))) {
                e.setCancelled(true);
                return;
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerBreakBlock(BlockBreakEvent e) {
        if (!(teamMap.containsKey(e.getPlayer().getUniqueId()))) e.setCancelled(true);
        if (teamMap.get(e.getPlayer().getUniqueId()) == team.spectator) e.setCancelled(true);
        if (teamMap.get(e.getPlayer().getUniqueId()) == team.admin) return;
        Set<Material> blackListedBlocks = new HashSet<>();
        blackListedBlocks.add(Material.SMOOTH_STONE);
        blackListedBlocks.add(Material.WHITE_CONCRETE);
        blackListedBlocks.add(Material.ORANGE_CONCRETE);
        blackListedBlocks.add(Material.MAGENTA_CONCRETE);
        blackListedBlocks.add(Material.LIGHT_BLUE_CONCRETE);
        blackListedBlocks.add(Material.YELLOW_CONCRETE);
        blackListedBlocks.add(Material.LIME_CONCRETE);
        blackListedBlocks.add(Material.PINK_CONCRETE);
        blackListedBlocks.add(Material.GRAY_CONCRETE);
        blackListedBlocks.add(Material.LIGHT_GRAY_CONCRETE);
        blackListedBlocks.add(Material.CYAN_CONCRETE);
        blackListedBlocks.add(Material.PURPLE_CONCRETE);
        blackListedBlocks.add(Material.BLUE_CONCRETE);
        blackListedBlocks.add(Material.BROWN_CONCRETE);
        blackListedBlocks.add(Material.GREEN_CONCRETE);
        blackListedBlocks.add(Material.RED_CONCRETE);
        blackListedBlocks.add(Material.BLACK_CONCRETE);
        blackListedBlocks.add(Material.BLUE_ICE);
        blackListedBlocks.add(Material.AMETHYST_BLOCK);
        blackListedBlocks.add(Material.YELLOW_WOOL);
        blackListedBlocks.add(Material.SPRUCE_SLAB);
        blackListedBlocks.add(Material.GRASS_BLOCK);
        blackListedBlocks.add(Material.STONE);
        if (blackListedBlocks.contains(e.getBlock().getType())) {
            e.setCancelled(true);
            return;
        }
        if (eventController.getStatus() != EventStatus.STARTED) {
            e.setCancelled(true);
            return;
        }
        if ((e.isCancelled()) || (e.getPlayer().getGameMode() == GameMode.CREATIVE)) return;
        if ((e.getBlock().getType() == Material.SNOW) || (e.getBlock().getType().toString().contains("CARPET"))) {
            e.setDropItems(false);
            e.setExpToDrop(0);
            int layers = 1;
            if (e.getBlock().getType() == Material.SNOW) {
                // get how many layers of snow
                layers = ((org.bukkit.block.data.type.Snow) e.getBlock().getBlockData()).getLayers();
            }
            playBlockSound(e.getBlock().getType(), e.getBlock().getLocation());
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.SNOWBALL, layers)).setVelocity(new Vector(0, -1, 0));
            e.getBlock().setType(Material.AIR);
            return;
        }
        if (e.getBlock().getType() == Material.SNOW_BLOCK) {
            e.setDropItems(false);
            e.setExpToDrop(0);
            playBlockSound(e.getBlock().getType(), e.getBlock().getLocation());
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.SNOWBALL, 4)).setVelocity(new Vector(0, -1, 0));
            e.getBlock().setType(Material.AIR);
            return;
        }
        if (e.getBlock().getType().toString().contains("CONCRETE_POWDER")) {
            e.setDropItems(false);
            e.setExpToDrop(0);
            SecureRandom rand = new SecureRandom();
            int probability = rand.nextInt(100) + 1;
            Chest chest = null;
            if (Bukkit.getWorld("world").getBlockAt(47, 65, 67).getType() == Material.CHEST) {
                chest = (Chest) Bukkit.getWorld("world").getBlockAt(47, 65, 67).getState();
            }
            switch (e.getBlock().getType()) {
                case RED_CONCRETE_POWDER:
                    if ((probability <= 5) && (chest != null)) {
                        ItemStack item = chest.getInventory().getItem(9).clone();
                        item.setAmount(1);
                        ItemMeta im =item.getItemMeta();
                        im.getPersistentDataContainer().set(new NamespacedKey(this, "pickUpLimit"), PersistentDataType.STRING, "2");
                        im.getPersistentDataContainer().set(new NamespacedKey(this, "RandomId"), PersistentDataType.STRING, "s-1667032441");
                        item.setItemMeta(im);
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), item).setVelocity(new Vector(0, -1, 0));
                    } else {
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.SNOWBALL, 4)).setVelocity(new Vector(0, -1, 0));
                    }
                    break;
                case ORANGE_CONCRETE_POWDER:
                    if ((probability <= 5) && (chest != null)) {
                        ItemStack item = chest.getInventory().getItem(10).clone();
                        item.setAmount(1);
                        ItemMeta im =item.getItemMeta();
                        im.getPersistentDataContainer().set(new NamespacedKey(this, "pickUpLimit"), PersistentDataType.STRING, "2");
                        im.getPersistentDataContainer().set(new NamespacedKey(this, "RandomId"), PersistentDataType.STRING, "s-1667032441");
                        item.setItemMeta(im);
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), item).setVelocity(new Vector(0, -1, 0));
                    } else {
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.SNOWBALL, 4)).setVelocity(new Vector(0, -1, 0));
                    }
                    break;
                case LIME_CONCRETE_POWDER:
                    if ((probability <= 5) && (chest != null)) {
                        ItemStack item = chest.getInventory().getItem(11).clone();
                        item.setAmount(1);
                        ItemMeta im =item.getItemMeta();
                        im.getPersistentDataContainer().set(new NamespacedKey(this, "pickUpLimit"), PersistentDataType.STRING, "2");
                        im.getPersistentDataContainer().set(new NamespacedKey(this, "RandomId"), PersistentDataType.STRING, "s-1667032441");
                        item.setItemMeta(im);
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), item).setVelocity(new Vector(0, -1, 0));
                    } else {
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.SNOWBALL, 4)).setVelocity(new Vector(0, -1, 0));
                    }
                    break;
                case LIGHT_BLUE_CONCRETE_POWDER:
                    if ((probability <= 5) && (chest != null)) {
                        ItemStack item = chest.getInventory().getItem(12).clone();
                        item.setAmount(1);
                        ItemMeta im =item.getItemMeta();
                        im.getPersistentDataContainer().set(new NamespacedKey(this, "pickUpLimit"), PersistentDataType.STRING, "2");
                        im.getPersistentDataContainer().set(new NamespacedKey(this, "RandomId"), PersistentDataType.STRING, "s-1667032441");
                        item.setItemMeta(im);
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), item).setVelocity(new Vector(0, -1, 0));
                    } else {
                        e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.SNOWBALL, 4)).setVelocity(new Vector(0, -1, 0));
                    }
                    break;
                default:
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), new ItemStack(Material.SNOWBALL, 4)).setVelocity(new Vector(0, -1, 0));
                    break;
            }
            playBlockSound(e.getBlock().getType(), e.getBlock().getLocation());
            e.getBlock().setType(Material.AIR);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrafting(CraftItemEvent e) {
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent e) {
        // if player not in any team
        if (!(teamMap.containsKey(e.getPlayer().getUniqueId()))) {
            teamMap.put(e.getPlayer().getUniqueId(), team.spectator);
        }
        if (teamMap.get(e.getPlayer().getUniqueId()) == team.spectator) {
            // if player in spectator team
            // teleport to spectator spawn
            eventController.teleportSpectator(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerEat(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() != Material.COOKIE) return;
        Chest chest = null;
        if (Bukkit.getWorld("world").getBlockAt(48, 65, 68).getType() == Material.CHEST) {
            chest = (Chest) Bukkit.getWorld("world").getBlockAt(48, 65, 68).getState();
        }
        if (chest == null) return;
        try {
            ItemStack item;
            // from the chest solt 0 to 26 find the first cookie
            for (int i = 0; i < 27; i++) {
                if (chest.getInventory().getItem(i).getType() == Material.COOKIE) {
                    item = chest.getInventory().getItem(i).clone();
                    item.setAmount(1);
                    e.getItem().setAmount(item.getAmount() - 1);
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            e.getPlayer().getInventory().addItem(item);
                        }
                    }.runTaskLater(this, 10);
                    break;
                }
            }
        } catch (Exception ignore) {}
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPVP(EntityDamageByEntityEvent e) {
        if ((e.getDamager().getType() == EntityType.SNOWBALL) || (e.getDamager().getType() == EntityType.EGG)) {
            if ((((org.bukkit.entity.Projectile) e.getDamager()).getShooter() instanceof Player) && (e.getEntity() instanceof Player)) {
                Player source = (Player) ((org.bukkit.entity.Projectile) e.getDamager()).getShooter();
                Player target = (Player) e.getEntity();
                if ((teamMap.containsKey(source.getUniqueId())) && (teamMap.containsKey(target.getUniqueId()))) {
                    // if they are same team cancel the event
                    if (teamMap.get(source.getUniqueId()) == teamMap.get(target.getUniqueId())) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }
            if (e.getDamager().getType() == EntityType.SNOWBALL) {
                e.setDamage(Main.getInstance().getConfig().getDouble("snowball-damage"));
            }
            if (e.getDamager().getType() == EntityType.EGG) {
                e.setDamage(Main.getInstance().getConfig().getDouble("egg-damage"));
            }
            e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, 80, 1);
        }
        if ((e.getDamager().getType() == EntityType.PLAYER) && (e.getEntity().getType() == EntityType.PLAYER)) {
            Player source = (Player) e.getDamager();
            Player target = (Player) e.getEntity();
            if ((teamMap.containsKey(source.getUniqueId())) && (teamMap.containsKey(target.getUniqueId()))) {
                // if they are same team cancel the event
                if (teamMap.get(source.getUniqueId()) == teamMap.get(target.getUniqueId())) {
                    e.setCancelled(true);
                    return;
                }
            }
            if ((source.getInventory().getItemInMainHand().getType() == org.bukkit.Material.CARVED_PUMPKIN) && (target.getInventory().getHelmet() == null)) {
                // takeaway the pumpkin and put it on target head
                ItemStack item = source.getInventory().getItemInMainHand().clone();
                item.setAmount(1);
                if (source.getInventory().getItemInMainHand().getAmount() <= 1) {
                    source.getInventory().setItemInMainHand(null);
                } else {
                    source.getInventory().getItemInMainHand().setAmount(source.getInventory().getItemInMainHand().getAmount() - 1);
                }
                target.getInventory().setHelmet(item);
            }
        }
    }

    @EventHandler
    public void onPlayerFall(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) {
            return;
        }
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            e.setCancelled(true);
        }
        if (eventController.getStatus() == EventStatus.PREPARING) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        if (e.getItemDrop().getItemStack().getType() == Material.PUFFERFISH) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (Bukkit.getEntity(e.getItemDrop().getUniqueId()) != null) {
                        for (int i = 0; i < e.getItemDrop().getItemStack().getAmount(); i++) {
                            e.getItemDrop().getWorld().spawnEntity(e.getItemDrop().getLocation(), EntityType.PUFFERFISH);
                        }
                        e.getItemDrop().remove();
                    }
                }
            }.runTaskLater(this, 20 * 5);
        }
        if (e.getItemDrop().getItemStack().getType() == Material.COOKIE) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (Bukkit.getEntity(e.getItemDrop().getUniqueId()) != null) {
                        // for item amount
                        for (int i = 0; i < e.getItemDrop().getItemStack().getAmount(); i++) {
                            AreaEffectCloud effectCloud = (AreaEffectCloud) e.getItemDrop().getWorld().spawnEntity(e.getItemDrop().getLocation(), EntityType.AREA_EFFECT_CLOUD);
                            effectCloud.setBasePotionType(PotionType.INSTANT_HEAL);
                            effectCloud.setColor(Color.fromARGB(255, 248, 36, 35));
                            effectCloud.setDuration(600);
                            effectCloud.setRadius(3);
                            effectCloud.setRadiusOnUse(-0.5f);
                            effectCloud.setRadiusPerTick(-0.005f);
                            effectCloud.setReapplicationDelay(20);
                            effectCloud.setSource(e.getPlayer());
                            effectCloud.setWaitTime(10);
                            effectCloud.setParticle(Particle.SPELL_MOB);
                        }
                        e.getItemDrop().remove();
                    }
                }
            }.runTaskLater(this, 20 * 5);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemMerge(ItemMergeEvent e) {
        if (e.getEntity().getItemStack().getType() == Material.PUFFERFISH) e.setCancelled(true);
        if (e.getEntity().getItemStack().getType() == Material.COOKIE) e.setCancelled(true);
    }

    @EventHandler
    public void onChickenLayEgg(EntityDropItemEvent e) {
        if (e.getEntity().getType() == EntityType.CHICKEN) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (getTeam(p.getUniqueId()) == team.admin) return;
        eventController.teleportSpectator(p.getUniqueId());
        teamMap.put(p.getUniqueId(), team.spectator);
        p.closeInventory();
        p.getInventory().clear();
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        // if player has team
        if (!(teamMap.containsKey(p.getUniqueId()))) {
            e.setCancelled(true);
            return;
        }
        // if player in admin team
        if (teamMap.get(p.getUniqueId()) == team.admin) return;
        // if event not started disable block place
        if (eventController.getStatus() != EventStatus.STARTED) {
            e.setCancelled(true);
            return;
        }
        // if player in spectator team
        if (teamMap.get(p.getUniqueId()) == team.spectator) {
            e.setCancelled(true);
            return;
        }
        // if blockType is not cake
        if ((!(e.getBlockPlaced().getType().toString().toUpperCase().contains("CAKE"))) && (e.getBlockPlaced().getType() != Material.FIRE)) {
            e.setCancelled(true);
            return;
        }
    }
    
    // @EventHandler
    // public void onLingeringPotionSplash(LingeringPotionSplashEvent e) {
    //     AreaEffectCloud aec = e.getAreaEffectCloud();
    //     // broadcast all the info of the lingering potion
    //     // BasePotionType
    //     Bukkit.broadcastMessage("Base potion type: " + aec.getBasePotionType().toString());
    //     // Color
    //     Bukkit.broadcastMessage("Color: A" + aec.getColor().getAlpha() + "R" + aec.getColor().getRed() + "G" + aec.getColor().getGreen() + "B" + aec.getColor().getBlue());
    //     // CustomEffects
    //     Bukkit.broadcastMessage("Custom effects: " + aec.getCustomEffects().toString());
    //     // Duration
    //     Bukkit.broadcastMessage("Duration: " + aec.getDuration());
    //     // DurationOnUse
    //     Bukkit.broadcastMessage("Duration on use: " + aec.getDurationOnUse());
    //     // Radius
    //     Bukkit.broadcastMessage("Radius: " + aec.getRadius());
    //     // RadiusOnUse
    //     Bukkit.broadcastMessage("Radius on use: " + aec.getRadiusOnUse());
    //     // RadiusPerTick
    //     Bukkit.broadcastMessage("Radius per tick: " + aec.getRadiusPerTick());
    //     // ReapplicationDelay
    //     Bukkit.broadcastMessage("Reapplication delay: " + aec.getReapplicationDelay());
    //     // Source
    //     Bukkit.broadcastMessage("Source: " + aec.getSource().toString());
    //     // WaitTime
    //     Bukkit.broadcastMessage("Wait time: " + aec.getWaitTime());
    //     // Particle
    //     Bukkit.broadcastMessage("Particle: " + aec.getParticle().toString());
    // }

    public static team getTeam(UUID uuid) {
        if (teamMap.containsKey(uuid)) {
            return teamMap.get(uuid);
        }
        return team.spectator;
    }

    public boolean checkSiteName(String siteName) {
        try {
            for (String site : getConfig().getConfigurationSection("sites").getKeys(false)) {
                if (site.equalsIgnoreCase(siteName)) return true;
            }
        } catch (Exception ignore) {}
        return false;
    }

    public void setTeam(UUID uuid, team team) {
        teamMap.put(uuid, team);
    }

    public static team getTeamPreAllocation(UUID uuid) {
        if (teamPreAllocationMap.containsKey(uuid)) {
            return teamPreAllocationMap.get(uuid);
        }
        return team.spectator;
    }

    public void setTeamPreAllocation(UUID uuid, team team) {
        teamPreAllocationMap.put(uuid, team);
    }

    public static boolean isPreAllocationTeam(UUID uuid) {
        if (teamPreAllocationMap.containsKey(uuid)) {
            if (teamPreAllocationMap.get(uuid) == team.spectator) return false;
            return true;
        }
        return false;
    }

    enum team {
        blue,
        red,
        aqua,
        purple,
        green,
        admin,
        spectator;
    }

    public String getTeamTranslate(team t) {
        switch (t) {
            case blue:
                return getConfig().getString("massage.team.blue", "藍隊");
            case red:
                return getConfig().getString("massage.team.red", "紅隊");
            case aqua:
                return getConfig().getString("massage.team.aqua", "青隊");
            case purple:
                return getConfig().getString("massage.team.purple", "紫隊");
            case green:
                return getConfig().getString("massage.team.green", "綠隊");
            case admin:
                return getConfig().getString("massage.team.admin", "管理員隊");
            case spectator:
                return getConfig().getString("massage.team.spectator", "旁觀者隊");
            default:
                return getConfig().getString("massage.team.spectator", "旁觀者隊");
        }
    }

    public File getDataFile() {
        return new File(this.getDataFolder(), "data.yml");
    }

    public FileConfiguration getDataCfg() {
        return dataCfg;
    }

    public void dataFileSave() {
        try {
            dataCfg.save(getDataFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void dataFileReload() {
        try {
            this.dataCfg = YamlConfiguration.loadConfiguration(getDataFile());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Color hex2Rgb(String colorStr) {
        return Color.fromRGB(
                Integer.valueOf( colorStr.substring( 1, 3 ), 16 ),
                Integer.valueOf( colorStr.substring( 3, 5 ), 16 ),
                Integer.valueOf( colorStr.substring( 5, 7 ), 16 ) );
    }

    public Location getRandomLoc(CuboidRegion region, Boolean HighCheck) {
        Location targetLoc;
        org.bukkit.World w = Bukkit.getWorld(region.getWorld().getName());
        int count = 0;
        while (true) {
            // get a random x and z within region
            SecureRandom random = new SecureRandom();
            BlockVector3 min = region.getPos1().withY(region.getMaximumY());
            BlockVector3 max = region.getPos2().withY(region.getMaximumY());
            int x = random.nextInt(max.getBlockX() - min.getBlockX() + 1) + min.getBlockX();
            int z = random.nextInt(max.getBlockZ() - min.getBlockZ() + 1) + min.getBlockZ();
            int y = region.getMaximumY();
            // create a random location
            targetLoc = new Location(w, x, y, z);
            count++;
            if (!(HighCheck)) break;
            if (w.getHighestBlockAt(targetLoc).getLocation().getBlockY() > region.getMinimumY()) break;
            if (count > 10) break;
        }
        return targetLoc;
    }

    public void playBlockSound(Material blockType, Location loc) {
        if (blockType.toString().contains("CONCRETE_POWDER")) {
            loc.getWorld().playSound(loc, Sound.BLOCK_SAND_BREAK, 1, 1);
            return;
        }
        if (blockType.toString().contains("CARPET")) {
            loc.getWorld().playSound(loc, Sound.BLOCK_WOOL_BREAK, 1, 1);
            return;
        }
        if ((blockType == Material.SNOW) || (blockType == Material.SNOW_BLOCK)) {
            loc.getWorld().playSound(loc, Sound.BLOCK_SNOW_BREAK, 1, 1);
            return;
        }
        loc.getWorld().playSound(loc, Sound.BLOCK_STONE_BREAK, 1, 1);
    }

    public static EventController getEventController() {
        return eventController;
    }

    public static Main getInstance() {
        return i;
    }
}
