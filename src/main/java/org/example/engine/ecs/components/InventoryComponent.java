package org.example.engine.ecs.components;

import org.example.engine.ecs.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map; /**
 * Component for inventory management
 */
public class InventoryComponent extends Component {
    private static class InventoryItem {
        String itemId;
        String itemName;
        int quantity;
        Map<String, String> properties = new HashMap<>();

        public InventoryItem(String itemId, String itemName, int quantity) {
            this.itemId = itemId;
            this.itemName = itemName;
            this.quantity = quantity;
        }
    }

    private final List<InventoryItem> items = new ArrayList<>();
    private int maxItems = 20;

    public InventoryComponent() {
    }

    public InventoryComponent(int maxItems) {
        this.maxItems = maxItems;
    }

    public boolean addItem(String itemId, String itemName, int quantity) {
        // Check if we already have this item
        for (InventoryItem item : items) {
            if (item.itemId.equals(itemId)) {
                item.quantity += quantity;
                return true;
            }
        }

        // Check if we have space for a new item
        if (items.size() >= maxItems) {
            return false;
        }

        // Add new item
        items.add(new InventoryItem(itemId, itemName, quantity));
        return true;
    }

    public boolean removeItem(String itemId, int quantity) {
        for (int i = 0; i < items.size(); i++) {
            InventoryItem item = items.get(i);
            if (item.itemId.equals(itemId)) {
                if (item.quantity <= quantity) {
                    items.remove(i);
                } else {
                    item.quantity -= quantity;
                }
                return true;
            }
        }
        return false;
    }

    public int getItemQuantity(String itemId) {
        for (InventoryItem item : items) {
            if (item.itemId.equals(itemId)) {
                return item.quantity;
            }
        }
        return 0;
    }

    public boolean hasItem(String itemId) {
        return getItemQuantity(itemId) > 0;
    }

    public List<Map<String, Object>> getItemList() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (InventoryItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.itemId);
            itemMap.put("name", item.itemName);
            itemMap.put("quantity", item.quantity);
            result.add(itemMap);
        }
        return result;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public void setMaxItems(int maxItems) {
        this.maxItems = maxItems;
    }

    public int getItemCount() {
        return items.size();
    }
}
