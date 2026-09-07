package tizio.dev.tsp.datagen;

import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.registry.RegisterBlocks;
import tizio.dev.tsp.resources.OreProperties;
import tizio.dev.tsp.resources.blocks.GravelBlock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TSPBlockLootProvider extends BlockLootSubProvider {

    public TSPBlockLootProvider() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        Set<Block> handled = new HashSet<>();

        for (OreProperties props : RegisterBlocks.RegisterOres.ORES.keySet()) {
            Block block = RegisterBlocks.RegisterOres.ORES.get(props).get();
            handled.add(block);

            if (props.dropItem() == null) {
                this.dropSelf(block);
                continue;
            }

            Item drop = props.dropItem().get();
            LootTable.Builder table = createSilkTouchDispatchTable(block,
                    applyExplosionDecay(block, LootItem.lootTableItem(drop)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(props.minDrop(), props.maxDrop())))
                            .apply(ApplyBonusCount.addOreBonusCount(Enchantments.BLOCK_FORTUNE))));

            this.add(block, table);
        }

        for (Block block : getAllRegisteredBlocks()) {
            if (block instanceof GravelBlock) {
                handled.add(block);

                LootTable.Builder table = createSilkTouchDispatchTable(block,
                        applyExplosionCondition(block, LootItem.lootTableItem(Items.FLINT))
                                .when(LootItemRandomChanceCondition.randomChance(0.1F))
                                .otherwise(applyExplosionCondition(block, LootItem.lootTableItem(block))));

                this.add(block, table);
            }
        }

        for (Block block : getAllRegisteredBlocks()) {
            if (!handled.contains(block)) {
                this.dropSelf(block);
            }
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return getAllRegisteredBlocks();
    }

    private List<Block> getAllRegisteredBlocks() {
        return RegisterBlocks.BLOCKS.getEntries().stream()
                .map(RegistryObject::get)
                .collect(Collectors.toList());
    }
}
