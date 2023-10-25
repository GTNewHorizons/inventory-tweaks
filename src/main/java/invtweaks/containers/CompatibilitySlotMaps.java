package invtweaks.containers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;

import invtweaks.api.container.ContainerSection;

@SuppressWarnings({ "unchecked", "unused" })
public class CompatibilitySlotMaps {

    public static Map<ContainerSection, List<Slot>> ee3PortableCraftingSlots(Container container) {
        Map<ContainerSection, List<Slot>> slotRefs = new HashMap<>();

        slotRefs.put(ContainerSection.CRAFTING_OUT, container.inventorySlots.subList(0, 1));
        slotRefs.put(ContainerSection.CRAFTING_IN, container.inventorySlots.subList(1, 10));
        return slotRefs;
    }

    public static Map<ContainerSection, List<Slot>> galacticraftPlayerSlots(Container container) {
        Map<ContainerSection, List<Slot>> slotRefs = new HashMap<>();

        slotRefs.put(ContainerSection.CRAFTING_OUT, container.inventorySlots.subList(0, 1));
        slotRefs.put(ContainerSection.CRAFTING_IN, container.inventorySlots.subList(1, 5));
        slotRefs.put(ContainerSection.ARMOR, container.inventorySlots.subList(5, 9));
        slotRefs.put(ContainerSection.INVENTORY, container.inventorySlots.subList(9, 45));
        slotRefs.put(ContainerSection.INVENTORY_NOT_HOTBAR, container.inventorySlots.subList(9, 36));
        slotRefs.put(ContainerSection.INVENTORY_HOTBAR, container.inventorySlots.subList(36, 45));

        return slotRefs;
    }
}
