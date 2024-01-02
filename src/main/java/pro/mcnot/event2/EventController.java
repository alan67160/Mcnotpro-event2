package pro.mcnot.event2;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.antlr.v4.runtime.Parser.TraceListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.PufferFish;
import org.bukkit.entity.Snowman;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Containers.CMIUser;
import com.Zrips.CMI.utils.SpawnUtil;

import de.unpixelt.locale.Locale;
import de.unpixelt.locale.Translate;
import it.unimi.dsi.fastutil.Hash;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ItemTag;
import net.md_5.bungee.api.chat.TextComponent;
import pro.mcnot.event2.Main.team;
import pro.mcnot.event2.SiteController.LocType;
import pro.mcnot.event2.SiteController.SiteStatus;

public class EventController {
    
    private static Main plugin;
    private BossBar bossbarMsg;
    private BossBar bossbarTimerMsg;
    private NamespacedKey bossbarmsgKey;
    private NamespacedKey bossbarTimerMsgKey;
    private String eventID;
    private EventStatus status;
    private BukkitTask counddownTask;
    private BukkitTask timerTask;
    private SiteController siteController;
    private Long time;
    private Long timeLimit;
    private HashMap<team, Integer> scoreMap;
    private HashMap<team, Set<UUID>> teamMap;
    private HashMap<team, Integer> spawnPointMap;
    private Boolean isDecaying;
    private Integer decayTime;
    private Integer decayBlock;
    private Integer specialItemTimer;
    private Integer specialItemDropTime;
    private LinkedHashMap<String, SiteStatus> siteStatusMap;
    private Boolean siteRestoredBypass;

    public EventController() {
        plugin = Main.getInstance();
        bossbarmsgKey = new NamespacedKey(plugin, "bossbarmsg");
        bossbarTimerMsgKey = new NamespacedKey(plugin, "bossbarTimerMsg");
        bossbarMsg = Bukkit.getBossBar(bossbarmsgKey);
        bossbarTimerMsg = Bukkit.getBossBar(bossbarTimerMsgKey);
        siteStatusMap = new LinkedHashMap<>();
        siteRestoredBypass = false;
        status = EventStatus.WAITING;
        counddownTask = null;
        siteController = null;
        time = 0L;
        specialItemTimer = 0;
        specialItemDropTime = -1;
        // if event-settings.site is set
        if (plugin.getConfig().getString("event-settings.site") != null) {
            siteController = new SiteController(plugin.getConfig().getString("event-settings.site"));
        }
        // if event-settings.timeLimit is set
        if (plugin.getConfig().getLong("event-settings.timeLimit") != 0) {
            timeLimit = plugin.getConfig().getLong("event-settings.timeLimit");
        } else {
            timeLimit = 0L;
        }
        // if event-settings.isDecaying is set
        if (plugin.getConfig().getBoolean("event-settings.decay") != false) {
            isDecaying = plugin.getConfig().getBoolean("event-settings.decay");
        } else {
            isDecaying = false;
        }
        // if event-settings.decayTime is set
        if (plugin.getConfig().getInt("event-settings.decayTime") != 0) {
            decayTime = plugin.getConfig().getInt("event-settings.decayTime");
        } else {
            decayTime = 0;
        }
        // if event-settings.decayBlock is set
        if (plugin.getConfig().getInt("event-settings.decayBlock") != 0) {
            decayBlock = plugin.getConfig().getInt("event-settings.decayBlock");
        } else {
            decayBlock = 0;
        }
        // if event-settings.specialItemDropTime is set
        if (plugin.getConfig().getInt("event-settings.specialItemDropTime") > 0) {
            specialItemDropTime = plugin.getConfig().getInt("event-settings.specialItemDropTime");
        } else {
            specialItemDropTime = -1;
        }
        // if sites.<siteName>.status is set
        for (String siteName : plugin.getConfig().getConfigurationSection("sites").getKeys(false)) {
            if (plugin.getConfig().getString("sites." + siteName + ".status") != null) {
                siteStatusMap.put(siteName, SiteStatus.valueOf(plugin.getConfig().getString("sites." + siteName + ".status")));
            } else {
                siteStatusMap.put(siteName, SiteStatus.NOT_RESTORED);
            }
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                if (status == EventStatus.STARTED) {
                    bossbarTimerMsg.setVisible(true);
                    bossbarTimerMsg.setProgress((double) (timeLimit - time) / timeLimit);
                    // if time is over 50% of timeLimit then set to green
                    if ((double) (timeLimit - time) > (timeLimit * 0.5)) {
                        bossbarTimerMsg.setTitle("§a§l遊戲結束還剩下 " + (timeLimit - time) + " 秒!");
                        bossbarTimerMsg.setColor(BarColor.GREEN);
                    }
                    // if time is under 50% and over 10% of timeLimit then set to yellow
                    if (((double) (timeLimit - time) <= (timeLimit * 0.5)) && ((double) (timeLimit - time) > (timeLimit * 0.1))) {
                        bossbarTimerMsg.setTitle("§e§l遊戲結束還剩下 " + (timeLimit - time) + " 秒!");
                        bossbarTimerMsg.setColor(BarColor.YELLOW);
                    }
                    // if time is under 10% of timeLimit then set to red
                    if ((double) (timeLimit - time) <= (timeLimit * 0.1)) {
                        bossbarTimerMsg.setTitle("§c§l遊戲結束還剩下 " + (timeLimit - time) + " 秒!");
                        bossbarTimerMsg.setColor(BarColor.RED);
                    }
                    if ((double) (timeLimit - time) <= 10) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                        }
                    }
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (!(bossbarTimerMsg.getPlayers().contains(p))) {
                            bossbarTimerMsg.addPlayer(p);
                        }
                    }
                } else {
                    try {
                        bossbarTimerMsg.removeAll();
                        bossbarTimerMsg.setVisible(false);
                    } catch (Exception ignored) {}
                }
            }
        }.runTaskTimer(plugin, 0, 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Chicken chicken : Bukkit.getWorld("world").getEntitiesByClass(Chicken.class)) {
                    // if chicken is not adult
                    if (!(chicken.isAdult())) {
                        // set age to adult
                        chicken.setAgeLock(true);
                        chicken.setAdult();
                    }
                    if (chicken.getPersistentDataContainer().has(new NamespacedKey(plugin, "lived"), org.bukkit.persistence.PersistentDataType.LONG)) {
                        chicken.getPersistentDataContainer().set(new NamespacedKey(plugin, "lived"), org.bukkit.persistence.PersistentDataType.LONG, chicken.getPersistentDataContainer().get(new NamespacedKey(plugin, "lived"), org.bukkit.persistence.PersistentDataType.LONG) + 1L);
                        if ((chicken.getPersistentDataContainer().get(new NamespacedKey(plugin, "lived"), org.bukkit.persistence.PersistentDataType.LONG) % 60L) != 0) {
                            return;
                        }
                    } else {
                        chicken.getPersistentDataContainer().set(new NamespacedKey(plugin, "lived"), org.bukkit.persistence.PersistentDataType.LONG, 1L);
                    }
                    // drop a egg
                    chicken.getWorld().dropItemNaturally(chicken.getLocation(), new ItemStack(Material.EGG, 1)).setVelocity(new Vector(0, -1, 0));
                    // play sound
                    chicken.getWorld().playSound(chicken.getLocation(), Sound.ENTITY_CHICKEN_EGG, 1, 1);
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    /**
     * This method will only be called once at event is starting
     */
    public void start() {
        if (status != EventStatus.PREPARING) return;
        status = EventStatus.STARTED;
        setSiteStatus(siteController.getSiteName(), SiteStatus.NOT_RESTORED);
        // remove potion effect
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP);
        }
        // reset player health, saturation, food level
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setSaturation(20);
            p.setFoodLevel(20);
        }
        // remove boss bar
        new BukkitRunnable() {
            @Override
            public void run() {
                bossbarMsg.setVisible(false);
                bossbarMsg.removeAll();
            }
        }.runTaskLater(plugin, 3*20);
        eventID = UUID.randomUUID().toString() + "-" + (System.currentTimeMillis() / 1000L);
        plugin.dataFileReload();
        plugin.getDataCfg().set("event." + eventID + ".site", siteController.getSiteName());
        plugin.getDataCfg().set("event." + eventID + ".timeLimit", timeLimit);
        for (team t : teamMap.keySet()) {
            // Set<UUID> to List<String>
            List<String> playerUUIDList = new ArrayList<>();
            for (UUID uuid : teamMap.get(t)) {
                playerUUIDList.add(uuid.toString());
            }
            plugin.getDataCfg().set("event." + eventID + ".teamManber." + t.toString(), playerUUIDList);
        }
        plugin.dataFileSave();
        // start timer
        timerTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (time < timeLimit) {
                    timeLoop();
                } else {
                    stop();
                    this.cancel();
                    return;
                }
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    /**
     * while event is running this method will be called every second
     */
    public void timeLoop() {
        time++;
        // decay
        if (isDecaying) {
            if (time % decayTime == 0) {
                decay();
            }
        }
        // special item
        specialItemTimer++;
        if (specialItemDropTime != -1) {
            if (specialItemTimer >= specialItemDropTime) {
                specialItemTimer = 0;
                specialItemDrop();
            }
        }
        // alive player count
        LinkedHashMap<team, Integer> alivePlayerCountMap = new LinkedHashMap<>();
        Integer alivePlayerCount = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Main.getTeam(p.getUniqueId()) == team.admin) continue;
            if (Main.getTeam(p.getUniqueId()) == team.spectator) continue;
            team t = Main.getTeam(p.getUniqueId());
            if (alivePlayerCountMap.containsKey(t)) {
                alivePlayerCountMap.put(t, alivePlayerCountMap.get(t) + 1);
            } else {
                alivePlayerCountMap.put(t, 1);
            }
            alivePlayerCount++;
        }
        Integer teamCount = 0;
        for (team t : teamMap.keySet()) {
            if (t == team.admin) continue;
            if (t == team.spectator) continue;
            teamCount++;
        }
        if (teamCount != 1) {
            if (alivePlayerCountMap.keySet().size() == 1) {
                stop();
                return;
            }
        } else {
            if (alivePlayerCount < 1) {
                stop();
                return;
            }
        }
    }

    /**
     * This method will be called once at the end of the event
     */
    public void stop() {
        // only run when event is started
        if (status != EventStatus.STARTED) return;
        // stop timer
        try {
            timerTask.cancel();
        } catch (Exception ignored) {}
        timerTask = null;
        // reset timer
        time = 0L;
        specialItemTimer = 0;
        // reset status
        status = EventStatus.WAITING;
        // force all online player the clost inventory
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.closeInventory();
        }
        // remove entity
        for (Item item : Bukkit.getWorld("world").getEntitiesByClass(Item.class)) {
            item.remove();
        }
        for (Chicken chicken : Bukkit.getWorld("world").getEntitiesByClass(Chicken.class)) {
            chicken.remove();
        }
        for (Snowman snowman : Bukkit.getWorld("world").getEntitiesByClass(Snowman.class)) {
            snowman.remove();
        }
        for (PufferFish pufferfish : Bukkit.getWorld("world").getEntitiesByClass(PufferFish.class)) {
            pufferfish.remove();
        }
        // get each layer y-coordinate
        HashMap<Integer, Integer> layerYcoordinateMap = new HashMap<>();
        for (String layer : plugin.getConfig().getConfigurationSection("sites." + siteController.getSiteName() + ".Layers").getKeys(false)) {
            layerYcoordinateMap.put(Integer.parseInt(layer), plugin.getConfig().getInt("sites." + siteController.getSiteName() + ".Layers." + layer));
        }
        scoreMap = new HashMap<>();
        // for all player except admin and spectator
        for (Player p : Bukkit.getOnlinePlayers()) {
            team t = Main.getTeam(p.getUniqueId());
            if ((t != team.admin) && (t != team.spectator)) {
                // check player y-coordinate
                int y = p.getLocation().getBlockY();
                int layer = 0;
                for (int i : layerYcoordinateMap.keySet()) {
                    if (y >= layerYcoordinateMap.get(i)) {
                        layer = i;
                    }
                }
                // add score
                if (scoreMap.containsKey(t)) {
                    scoreMap.put(t, scoreMap.get(t) + layer);
                } else {
                    scoreMap.put(t, layer);
                }
            }
        }
        plugin.dataFileReload();
        // save stoping time
        plugin.getDataCfg().set("event." + eventID + ".stopingTime", System.currentTimeMillis() / 1000L);
        // save score
        for (team t : scoreMap.keySet()) {
            plugin.getDataCfg().set("event." + eventID + ".score." + t.toString(), scoreMap.get(t));
        }
        plugin.dataFileSave();
        // broadcast message
        Bukkit.broadcastMessage("§6【§e系統§6】§f遊戲結束!");
        // broadcast result
        Bukkit.broadcastMessage("§6【§e系統§6】§f結果:");
        for (team t : scoreMap.keySet()) {
            Bukkit.broadcastMessage("§6【§e系統§6】§f" + plugin.getTeamTranslate(t) + " §7: §f" + scoreMap.get(t) + " 分");
        }
        team winner = null;
        // if scoreMap only has one team
        if (scoreMap.size() == 1) {
            // broadcast winner is...
            Bukkit.broadcastMessage("§6【§e系統§6】§f贏家是: " + plugin.getTeamTranslate((team) scoreMap.keySet().toArray()[0]));
        } else { // if scoreMap has more than one team
            // get the highest score
            int highestScore = 0;
            for (team t : scoreMap.keySet()) {
                if (scoreMap.get(t) > highestScore) {
                    highestScore = scoreMap.get(t);
                }
            }
            List<team> winnerList = new ArrayList<>();
            for (team t : scoreMap.keySet()) {
                if (scoreMap.get(t) == highestScore) {
                    winnerList.add(t);
                }
            }
            // if there are more than one team in the winnerList
            if (winnerList.size() != 1) {
                // broadcast winner
                Bukkit.broadcastMessage("§6【§e系統§6】§f贏家是: §c沒有人");
            } else {
                // broadcast winner
                Bukkit.broadcastMessage("§6【§e系統§6】§f贏家是: " + plugin.getTeamTranslate(winnerList.get(0)));
                winner = winnerList.get(0);
            }
        }
        // broadcast winner team all manber name
        if (winner != null) {
            LinkedList<String> manberList = new LinkedList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (Main.getTeam(p.getUniqueId()) == winner) {
                    manberList.add(p.getName());
                }
            }
            Bukkit.broadcastMessage("§6【§e系統§6】§f" + plugin.getTeamTranslate(winner) + " §7: §f" + String.join(", ", manberList));
        }
        // set player to spectator
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Main.getTeam(p.getUniqueId()) != team.admin) {
                plugin.setTeam(p.getUniqueId(), team.spectator);
                p.getInventory().clear();
            }
        }
        // teleport player to spectator spawn point
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Main.getTeam(p.getUniqueId()) != team.spectator) continue;
            teleportSpectator(p.getUniqueId());
        }
    }
    
    public void countdown() {
        // start counddown
        if (status != EventStatus.WAITING) return;
        status = EventStatus.PREPARING;
        time = 0L;
        specialItemTimer = 0;
        // move player from pre-allocation team to team
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Main.isPreAllocationTeam(p.getUniqueId())) {
                plugin.setTeam(p.getUniqueId(), Main.getTeamPreAllocation(p.getUniqueId()));
                plugin.setTeamPreAllocation(p.getUniqueId(), team.spectator);
            }
        }
        // force all online player the clost inventory
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.closeInventory();
        }
        teamMap = new HashMap<>();
        // add player to teamMap map for logging
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (teamMap.containsKey(Main.getTeam(p.getUniqueId()))) {
                Set<UUID> playerUUIDSet = teamMap.get(Main.getTeam(p.getUniqueId()));
                playerUUIDSet.add(p.getUniqueId());
                teamMap.put(Main.getTeam(p.getUniqueId()), playerUUIDSet);
            } else {
                Set<UUID> playerUUIDSet = new HashSet<>();
                playerUUIDSet.add(p.getUniqueId());
                teamMap.put(Main.getTeam(p.getUniqueId()), playerUUIDSet);
            }
        }
        int teamCount = 0;
        // get team count
        for (team t : teamMap.keySet()) {
            if (t == team.admin) continue;
            if (t == team.spectator) continue;
            teamCount++;
        }
        if (teamCount <= 0) {
            Bukkit.broadcastMessage("§6【§e系統§6】§f隊伍數量不足! 無法繼續!");
            status = EventStatus.WAITING;
            return;
        }
        // give potion effect
        for (team t : teamMap.keySet()) {
            if (t == team.admin) continue;
            if (t == team.spectator) continue;
            for (UUID pUUID : teamMap.get(t)) {
                if (!(Bukkit.getOfflinePlayer(pUUID).isOnline())) continue;
                Player p = Bukkit.getPlayer(pUUID);
                p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP, 1200, 200, false, false));
            }
        }
        // get spawn point as a Set<Interger>
        Set<Integer> spawnPointSet = new HashSet<>();
        for (String spawnPoint : plugin.getConfig().getConfigurationSection("sites." + siteController.getSiteName() + ".SpawnPoints").getKeys(false)) {
            spawnPointSet.add(Integer.parseInt(spawnPoint));
        }
        spawnPointMap = new HashMap<>();
        Integer lastSpawnPoint = null;
        for (team t : teamMap.keySet()) {
            if (t == team.admin) continue;
            if (t == team.spectator) continue;
            // when this for loop is in odd number use random spawn point
            if (teamCount % 2 == 1) {
                int spawnPointNumber = genrtateRandomSpawnPointNumber(spawnPointSet);
                if (spawnPointNumber == -1) {
                    Bukkit.broadcastMessage("§6【§e系統§6】§f出生點數量不足! 無法繼續!");
                    status = EventStatus.WAITING;
                    return;
                }
                spawnPointMap.put(t, spawnPointNumber);
                lastSpawnPoint = spawnPointNumber;
                spawnPointSet.remove(spawnPointNumber);
            }
            // when this for loop is in even number use the farthest spawn point to lastSpawnPoint
            else {
                Location lastSpawnPointLocation = new Location(Bukkit.getWorld(plugin.getConfig().getString("sites." + siteController.getSiteName() + ".world")),
                        plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + lastSpawnPoint + ".x"),
                        plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + lastSpawnPoint + ".y"),
                        plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + lastSpawnPoint + ".z"));
                int farthestSpawnPointNumber = -1;
                double farthestDistance = 0;
                for (int spawnPointNumber : spawnPointSet) {
                    Location spawnPointLocation = new Location(Bukkit.getWorld(plugin.getConfig().getString("sites." + siteController.getSiteName() + ".world")),
                            plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointNumber + ".x"),
                            plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointNumber + ".y"),
                            plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointNumber + ".z"));
                    double distance = spawnPointLocation.distance(lastSpawnPointLocation);
                    if (distance > farthestDistance) {
                        farthestSpawnPointNumber = spawnPointNumber;
                        farthestDistance = distance;
                    }
                }
                spawnPointMap.put(t, farthestSpawnPointNumber);
                lastSpawnPoint = farthestSpawnPointNumber;
                spawnPointSet.remove(farthestSpawnPointNumber);
            }
        }
        // teleport player to spawn point
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Main.getTeam(p.getUniqueId()) != team.spectator) continue;
            teleportSpectator(p.getUniqueId());
        }
        for (team t : teamMap.keySet()) {
            if (t == team.admin) continue;
            if (t == team.spectator) continue;
            for (UUID uuid : teamMap.get(t)) {
                if (!(Bukkit.getOfflinePlayer(uuid).isOnline())) continue;
                Player p = Bukkit.getPlayer(uuid);
                Location spawnPointLocation = new Location(Bukkit.getWorld(plugin.getConfig().getString("sites." + siteController.getSiteName() + ".world")),
                        plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointMap.get(t) + ".x"),
                        plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointMap.get(t) + ".y"),
                        plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointMap.get(t) + ".z"),
                        (float) plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointMap.get(t) + ".yaw"),
                        (float) plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointMap.get(t) + ".pitch"));
                p.teleport(spawnPointLocation);
            }
        }
        // for each team spawn chicken in spawn
        for (team t : teamMap.keySet()) {
            if (t == team.admin) continue;
            if (t == team.spectator) continue;
            Location spawnPointLocation = new Location(Bukkit.getWorld(plugin.getConfig().getString("sites." + siteController.getSiteName() + ".world")),
                        plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointMap.get(t) + ".x"),
                        plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointMap.get(t) + ".y"),
                        plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpawnPoints." + spawnPointMap.get(t) + ".z"));
            Chest chest = null;
            if (Bukkit.getWorld("world").getBlockAt(49, 65, 67).getType() == Material.CHEST) {
                chest = (Chest) Bukkit.getWorld("world").getBlockAt(49, 65, 67).getState();
            }
            if (chest == null) continue;
            try {
                ItemStack item;
                for (int i = 0; i < 27; i++) {
                    if (chest.getInventory().getItem(i) != null) {
                        item = chest.getInventory().getItem(i).clone();
                        item.setAmount(1);
                        spawnPointLocation.getWorld().dropItemNaturally(spawnPointLocation, item).setVelocity(new Vector(0, -1, 0));
                    }
                }
            } catch (Exception ignore) {}
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setSaturation(5);
            p.setFoodLevel(20);
        }
        // give spawn item
        for (team t : teamMap.keySet()) {
            if (t == team.admin) continue;
            if (t == team.spectator) continue;
            for (UUID uuid : teamMap.get(t)) {
                if (!(Bukkit.getOfflinePlayer(uuid).isOnline())) continue;
                Player p = Bukkit.getPlayer(uuid);
                giveSpawnItem(p);
            }
        }
        counddownTask = new BukkitRunnable() {
            int time = 30;
            @Override
            public void run() {
                if (this.time <= 0) {
                    // broadcast message
                    Bukkit.broadcastMessage("§6【§e系統§6】§f遊戲開始!");
                    // subtitle massage
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendTitle("", "§c§l遊戲開始!", 20, 20, 20);
                    }
                    // actionbar massage
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText("§c§l遊戲開始!"));
                    }
                    // bossbar massage
                    bossbarMsg.setProgress(0);
                    bossbarMsg.setTitle("§c§l遊戲開始!");
                    bossbarMsg.setColor(BarColor.RED);
                    // play event_raid_horn sound
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1000f, 1.1f);
                    }
                    // call start event method
                    start();
                    this.cancel();
                    return;
                }
                bossbarMsg.setVisible(true);
                bossbarMsg.setProgress((double) this.time / 30);
                // edit bossbar massage
                if ((this.time > 10)) {
                    bossbarMsg.setTitle("§a遊戲將在 " + this.time + " 秒後開始!");
                    bossbarMsg.setColor(BarColor.GREEN);
                }
                if ((this.time <= 10) && (this.time > 5)) {
                    bossbarMsg.setTitle("§e遊戲將在 " + this.time + " 秒後開始!");
                    bossbarMsg.setColor(BarColor.YELLOW);
                }
                if (this.time <= 5) {
                    bossbarMsg.setTitle("§c遊戲將在 " + this.time + " 秒後開始!");
                    bossbarMsg.setColor(BarColor.RED);
                }
                if ((this.time == 30) || (this.time == 20) || (this.time == 10) || (this.time <= 5)) {
                    Bukkit.broadcastMessage("§6【§e系統§6】§f遊戲將在 " + this.time + " 秒後開始!");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        // subtitle massage
                        if ((this.time == 30) || (this.time == 20)) p.sendTitle("", "§a遊戲將在 §n" + this.time + "§a 秒後開始!", 20, 20, 20);
                        if (this.time == 10) p.sendTitle("", "§e遊戲將在 §n" + this.time + "§e 秒後開始!", 20, 20, 20);
                        if (this.time <= 5) p.sendTitle("", "§c遊戲將在 §n" + this.time + "§c 秒後開始!", 20, 20, 20);
                        // add player to bossbar
                        if (!(bossbarMsg.getPlayers().contains(p))) {
                            bossbarMsg.addPlayer(p);
                        }
                    }
                }
                // play block_note_block_pling sound
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                }
                this.time--;
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public void teleportSpectator(UUID playerUuid) {
        Location spectatorLocation = getSpectatorSpawnLoc();
        if (Bukkit.getOfflinePlayer(playerUuid).isOnline()) {
            Player p = Bukkit.getPlayer(playerUuid);
            p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setSaturation(20);
            p.setFoodLevel(20);
            p.teleport(spectatorLocation);
        } else {
            if (Bukkit.getPluginManager().getPlugin("CMI") != null) {
                CMI cmi = CMI.getInstance();
                CMIUser cmiUser = cmi.getPlayerManager().getUser(playerUuid);
                Player p = cmi.getNMS().getPlayer(cmiUser);
                try {
                    p.getInventory().clear();
                } catch (Exception ignore) {}
                try {
                    cmi.getNMS().clearPotionEffects(p);
                    cmi.getNMS().forceTeleport(p, spectatorLocation);
                    cmi.getNMS().setMiscLocation(p, spectatorLocation);
                } catch (Exception ignore) {}
                cmiUser.setLastTeleportLocation(spectatorLocation);
            }
        }
    }

    public Location getSpectatorSpawnLoc() {
        Location spectatorLocation = new Location(Bukkit.getWorld(plugin.getConfig().getString("sites." + siteController.getSiteName() + ".world")),
                    plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpectatorSpawnPoint.x"),
                    plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpectatorSpawnPoint.y"),
                    plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpectatorSpawnPoint.z"),
                    (float) plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpectatorSpawnPoint.yaw"),
                    (float) plugin.getConfig().getDouble("sites." + siteController.getSiteName() + ".SpectatorSpawnPoint.pitch"));
        return spectatorLocation;
    }

    public void giveSpawnItem(Player p) {
        if (!(p.isOnline())) return;
        Chest chest = null;
        if (Bukkit.getWorld("world").getBlockAt(48, 65, 67).getType() == Material.CHEST) {
            chest = (Chest) Bukkit.getWorld("world").getBlockAt(48, 65, 67).getState();
        }
        if (chest == null) return;
        try {
            ItemStack item;
            for (int i = 0; i < 27; i++) {
                if (chest.getInventory().getItem(i) != null) {
                    item = chest.getInventory().getItem(i).clone();
                    p.getInventory().addItem(item);
                }
            }
        } catch (Exception ignore) {}
    }

    public Integer genrtateRandomSpawnPointNumber(Set<Integer> spawnPointSet) {
        SecureRandom rand = new SecureRandom();
        if (spawnPointSet.size() == 0) return -1;
        return (int) spawnPointSet.toArray()[rand.nextInt(spawnPointSet.size())];
    }

    public enum EventStatus {
        WAITING, PREPARING, STARTED
    }

    public EventStatus getStatus() {
        return status;
    }

    public void stopCounddown() {
        if (counddownTask != null) {
            counddownTask.cancel();
            counddownTask = null;
        }
        // reset status
        status = EventStatus.WAITING;
        // reset timer
        time = 0L;
        specialItemTimer = 0;
        // hide bossbar
        bossbarMsg.setVisible(false);
        bossbarMsg.removeAll();
        // remove potion effect
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.removePotionEffect(org.bukkit.potion.PotionEffectType.JUMP);
        }
        // reset player health, saturation, food level
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setSaturation(20);
            p.setFoodLevel(20);
        }
        // remove team
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (Main.getTeam(p.getUniqueId()) != team.admin) {
                plugin.setTeam(p.getUniqueId(), team.spectator);
                p.getInventory().clear();
                teleportSpectator(p.getUniqueId());
            }
        }
    }

    public void decay() {
        // for loop decayBlock times
        for (int i = 0; i < decayBlock; i++) {
            // get random location
            Location decayLoc = plugin.getRandomLoc(siteController.getRegion(LocType.ACTUAL), true);
            decayLoc = decayLoc.getWorld().getHighestBlockAt(decayLoc).getLocation();
            // play block break sound
            plugin.playBlockSound(decayLoc.getBlock().getType(), decayLoc);
            if (decayLoc.getBlock().getType() == Material.SNOW_BLOCK) {
                decayLoc.getBlock().setType(Material.AIR);
            } else {
                decayLoc.getBlock().breakNaturally();
            }
        }
    }

    public void specialItemDrop() {
        Chest chest = null;
        if (Bukkit.getWorld("world").getBlockAt(50, 65, 67).getType() == Material.CHEST) {
            chest = (Chest) Bukkit.getWorld("world").getBlockAt(50, 65, 67).getState();
        }
        if (chest == null) return;
        LinkedList<Integer> possibleSlot = new LinkedList<>();
        try {
            for (int i = 0; i < 27; i++) {
                if (chest.getInventory().getItem(i) != null) {
                    possibleSlot.add(i);
                }
            }
        } catch (Exception ignore) {}
        if (possibleSlot.isEmpty()) return;
        Integer slot;
        // use secure random to get random slot
        SecureRandom rand = new SecureRandom();
        slot = possibleSlot.get(rand.nextInt(possibleSlot.size()));
        ItemStack item = chest.getInventory().getItem(slot);
        String itemNameRaw = null;
        // if item has custom name
        if (item.hasItemMeta()) {
            if (item.getItemMeta().hasDisplayName()) {
                itemNameRaw = item.getItemMeta().getDisplayName();
            }
        }
        if (itemNameRaw == null) {
            if (Bukkit.getPluginManager().getPlugin("Locale-API") != null) {
                itemNameRaw = Translate.getMaterial(Locale.zh_tw, item.getType());
            } else {
                itemNameRaw = item.getType().toString();
            }
        }
        // generate random item id to avoid item stack
        if ((item.getType() == Material.SLIME_BALL) || (item.getType() == Material.FIRE_CHARGE) || (item.getType() == Material.TNT) || (item.getType() == Material.FEATHER)) {
            ItemMeta im = item.getItemMeta();
            String id = "";
            rand = new SecureRandom();
            for (int i = 0; i < 10; i++) {
                id += String.valueOf(rand.nextInt(9));
            }
            im.getPersistentDataContainer().set(new NamespacedKey(plugin, "RandomId"), PersistentDataType.STRING, id);
            item.setItemMeta(im);
        }
        // get random location
        Location specialItemLoc = plugin.getRandomLoc(siteController.getRegion(LocType.ACTUAL), true);
        specialItemLoc.setY(Double.valueOf(String.valueOf(siteController.getRegion(LocType.ACTUAL).getMaximumY())));
        // drop item
        org.bukkit.entity.Item itemEnt = specialItemLoc.getWorld().dropItemNaturally(specialItemLoc, item);
        itemEnt.setGlowing(true);
        // play egg lay sound
        specialItemLoc.getWorld().playSound(specialItemLoc, Sound.ENTITY_CHICKEN_EGG, 2, 1);
        // broadcast message
        TextComponent itemName = new TextComponent(itemNameRaw);
        ItemTag itemTag = ItemTag.ofNbt(item.getItemMeta() == null ? null : item.getItemMeta().getAsString());
        net.md_5.bungee.api.chat.hover.content.Item itemHover = new net.md_5.bungee.api.chat.hover.content.Item(item.getType().getKey().toString(), item.getAmount(), itemTag);
        itemName.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, itemHover));
        itemName.setText(itemNameRaw);
        BaseComponent[] massage = 
            new ComponentBuilder("§6【§e系統§6】§f特殊物品 ")
            .append(itemName)
            .append(TextComponent.fromLegacyText(" §f已掉落!"))
            .create();
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(ChatMessageType.CHAT, massage);
        }
    }

    public Boolean setSite(String siteName) {
        if (getStatus() != EventStatus.WAITING) return false;
        siteController = new SiteController(siteName);
        // save to config
        plugin.reloadConfig();
        plugin.getConfig().set("event-settings.site", siteName);
        plugin.saveConfig();
        return true;
    }

    public Boolean setTimeLimit(Long timeLimit) {
        if (getStatus() != EventStatus.WAITING) return false;
        this.timeLimit = timeLimit;
        // save to config
        plugin.reloadConfig();
        plugin.getConfig().set("event-settings.timeLimit", timeLimit);
        plugin.saveConfig();
        return true;
    }

    public Boolean setDecay(Boolean status) {
        if (getStatus() != EventStatus.WAITING) return false;
        this.isDecaying = status;
        // save to config
        plugin.reloadConfig();
        plugin.getConfig().set("event-settings.decay", status);
        plugin.saveConfig();
        return true;
    }

    public Boolean setDecayTime(Integer decayTime) {
        if (getStatus() != EventStatus.WAITING) return false;
        this.decayTime = decayTime;
        // save to config
        plugin.reloadConfig();
        plugin.getConfig().set("event-settings.decayTime", decayTime);
        plugin.saveConfig();
        return true;
    }

    public Boolean setDecayBlock(Integer decayBlock) {
        if (getStatus() != EventStatus.WAITING) return false;
        this.decayBlock = decayBlock;
        // save to config
        plugin.reloadConfig();
        plugin.getConfig().set("event-settings.decayBlock", decayBlock);
        plugin.saveConfig();
        return true;
    }

    public Boolean setSpecialItemDropTime(Integer t) {
        this.specialItemTimer = 0;
        this.specialItemDropTime = t;
        // save to config
        plugin.reloadConfig();
        plugin.getConfig().set("event-settings.specialItemDropTime", t);
        plugin.saveConfig();
        return true;
    }

    public Boolean setSiteStatus(String siteName, SiteStatus status) {
        siteStatusMap.put(siteName, status);
        // save to config
        plugin.reloadConfig();
        plugin.getConfig().set("sites." + siteName + ".status", status.toString());
        plugin.saveConfig();
        return true;
    }

    public SiteStatus getSiteStatus(String siteName) {
        if (siteStatusMap.containsKey(siteName)) {
            return siteStatusMap.get(siteName);
        } else {
            return SiteStatus.RESTORED;
        }
    }

    public void setSiteRestoredBypass(Boolean status) {
        siteRestoredBypass = status;
    }

    public Boolean getSiteRestoredBypass() {
        return siteRestoredBypass;
    }

    public Integer getSpecialItemDropTime() {
        return specialItemDropTime;
    }

    public Integer getSpecialItemTimer() {
        return specialItemTimer;
    }

    public String getSite() {
        return siteController.getSiteName();
    }

    public Long getTimeLimit() {
        return timeLimit;
    }

    public Long getTime() {
        return time;
    }

    public Boolean getDecay() {
        return isDecaying;
    }

    public Integer getDecayTime() {
        return decayTime;
    }

    public Integer getDecayBlock() {
        return decayBlock;
    }
}
