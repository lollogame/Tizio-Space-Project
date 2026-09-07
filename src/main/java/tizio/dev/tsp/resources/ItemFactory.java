package tizio.dev.tsp.resources;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.registry.RegisterItems;

import java.util.function.Supplier;

public class ItemFactory {

    private final String name;
    private final Supplier<Item> itemSupplier;

    private Supplier<? extends ItemLike> nuggetSupplier = null;
    private Supplier<? extends ItemLike> blockSupplier = null;
    private Supplier<? extends ItemLike> smeltingResultSupplier = null;
    private float smeltingXp = 0.7f;
    private int smeltingTime = 200;
    private boolean canSmelt = false;

    private RegistryObject<Item> itemRegistryObject;

    public ItemFactory(String name, Supplier<Item> itemSupplier) {
        this.name = name;
        this.itemSupplier = itemSupplier;
    }

    public ItemFactory(String name) {
        this(name, () -> new Item(new Item.Properties()));
    }

    public String getName() { return name; }
    public RegistryObject<Item> getItemRegistryObject() { return itemRegistryObject; }

    public Supplier<? extends ItemLike> getNuggetSupplier() { return nuggetSupplier; }
    public Supplier<? extends ItemLike> getBlockSupplier() { return blockSupplier; }
    public Supplier<? extends ItemLike> getSmeltingResultSupplier() { return smeltingResultSupplier; }
    public float getSmeltingXp() { return smeltingXp; }
    public int getSmeltingTime() { return smeltingTime; }
    public boolean canSmelt() { return canSmelt; }

    public ItemFactory makeNuggets(Supplier<? extends ItemLike> nuggetSupplier) {
        this.nuggetSupplier = nuggetSupplier;
        return this;
    }

    public ItemFactory makeBlock(Supplier<? extends ItemLike> blockSupplier) {
        this.blockSupplier = blockSupplier;
        return this;
    }

    public ItemFactory canSmelt(Supplier<? extends ItemLike> resultSupplier, float xp, int cookingTime) {
        this.canSmelt = true;
        this.smeltingResultSupplier = resultSupplier;
        this.smeltingXp = xp;
        this.smeltingTime = cookingTime;
        return this;
    }

    public ItemFactory canSmelt(Supplier<? extends ItemLike> resultSupplier) {
        return canSmelt(resultSupplier, 0.7f, 200);
    }

    public RegistryObject<Item> build() {
        itemRegistryObject = RegisterItems.ITEMS.register(name, itemSupplier);
        RegisterItems.ITEM_BUILDERS.put(name, this);
        return itemRegistryObject;
    }
}
