package invtweaks;

import org.apache.commons.lang3.ObjectUtils;

import invtweaks.api.IItemTreeItem;

/**
 * Representation of an item in the item tree.
 *
 * @author Jimeo Wan
 */
public class InvTweaksItemTreeItem implements IItemTreeItem {

    private String name;
    private String id;
    private int damageMin;
    private int damageMax;
    private int order;

    /**
     * @param name   The item name
     * @param id     The item ID
     * @param damage The item variant or InvTweaksConst.DAMAGE_WILDCARD
     * @param order  The item order while sorting
     */
    public InvTweaksItemTreeItem(String name, String id, int damage, int order) {
        this.name = name;
        this.id = InvTweaksObfuscation.getNamespacedID(id);
        this.damageMin = damage;
        this.damageMax = damage;
        this.order = order;
    }

    /**
     * @param name      The item name
     * @param id        The item ID
     * @param damageMin The lowest value of the item variant or InvTweaksConst.DAMAGE_WILDCARD
     * @param damageMax The highest value of the item variant or InvTweaksConst.DAMAGE_WILDCARD
     * @param order     The item order while sorting
     */
    public InvTweaksItemTreeItem(String name, String id, int damageMin, int damageMax, int order) {
        this.name = name;
        this.id = InvTweaksObfuscation.getNamespacedID(id);
        this.damageMin = damageMin;
        this.damageMax = damageMax;
        this.order = order;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getDamage() {
        // Not an ideal solution, but handles DAMAGE_WILDCARD cases nicely
        return damageMin;
    }

    @Override
    public boolean matchesDamage(int damage) {
        if (damage == InvTweaksConst.DAMAGE_WILDCARD || this.damageMin == InvTweaksConst.DAMAGE_WILDCARD
                || this.damageMax == InvTweaksConst.DAMAGE_WILDCARD) {
            return true;
        }
        return damage >= this.damageMin && damage <= this.damageMax;
    }

    @Override
    public int getOrder() {
        return order;
    }

    /**
     * Warning: the item equality is not reflective. They are equal if "o" matches the item constraints (the opposite
     * can be false).
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof IItemTreeItem)) {
            return false;
        }
        IItemTreeItem item = (IItemTreeItem) o;
        return ObjectUtils.equals(id, item.getId()) && (damageMin == InvTweaksConst.DAMAGE_WILDCARD
                || (damageMin <= item.getDamage() && damageMax >= item.getDamage()));
    }

    public String toString() {
        return name;
    }

    @Override
    public int compareTo(IItemTreeItem item) {
        return item.getOrder() - getOrder();
    }

}
