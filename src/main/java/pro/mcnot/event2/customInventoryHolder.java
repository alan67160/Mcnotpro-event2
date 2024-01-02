package pro.mcnot.event2;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class customInventoryHolder implements InventoryHolder {
    
    private final Inventory inventory;

    public customInventoryHolder(String name) {
        if (name == null) {
            this.inventory = Bukkit.createInventory(this, 27);
        } else {
            this.inventory = Bukkit.createInventory(this, 27, name);
        }
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

}
