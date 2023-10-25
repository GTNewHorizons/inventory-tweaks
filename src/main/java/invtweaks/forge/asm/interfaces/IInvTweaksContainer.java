package invtweaks.forge.asm.interfaces;

import java.util.List;
import java.util.Map;

import net.minecraft.inventory.Slot;

import invtweaks.api.container.ContainerSection;

public interface IInvTweaksContainer {

    boolean invtweaks$validChest();

    boolean invtweaks$largeChest();

    boolean invtweaks$validInventory();

    boolean invtweaks$showButtons();

    int invtweaks$rowSize();

    Map<ContainerSection, List<Slot>> invtweaks$slotMap();

}
