package pro.mcnot.event2;

import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public class SpecialItemPreviewCommand implements TabExecutor, Listener {
    
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        // check if the sender is a player
        if (!(sender instanceof org.bukkit.entity.Player)) {
            sender.sendMessage("Only players can perform this command!");
            return true;
        }
        Player p = (Player) sender;
        // clone the chest inventory to ui
        Chest chest = null;
        if (Bukkit.getWorld("world").getBlockAt(50, 65, 67).getType() == Material.CHEST) {
            chest = (Chest) Bukkit.getWorld("world").getBlockAt(50, 65, 67).getState();
        }
        if (chest == null) return true;
        // create a chest UI
        Inventory ui = new customInventoryHolder(chest.getCustomName() + " - 預覽").getInventory();
        try {
            for (int i = 0; i < 27; i++) {
                if (chest.getInventory().getItem(i) != null) {
                    ItemStack is = chest.getInventory().getItem(i).clone();
                    ItemMeta im = is.getItemMeta();
                    im.getPersistentDataContainer().set(new NamespacedKey(Main.getInstance(), "preview"), PersistentDataType.STRING, "");
                    is.setItemMeta(im);
                    ui.setItem(i, is);
                }
            }
        } catch (Exception ignore) {}
        p.openInventory(ui);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }

    @EventHandler
    public void onClickingUI(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof customInventoryHolder) {
            e.setCancelled(true);
        }
    }

}
