package tizio.dev.tsp.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import tizio.dev.tsp.MainClass;
import tizio.dev.tsp.world.CraterPiece;
import tizio.dev.tsp.world.structure.CraterStructure;

public class RegisterStructures {

    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, MainClass.MODID);
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, MainClass.MODID);

    public static final RegistryObject<StructureType<CraterStructure>> CRATER_STRUCTURE_TYPE = STRUCTURE_TYPES.register("giant_crater", () -> () -> CraterStructure.CODEC);

    public static final RegistryObject<StructurePieceType> CRATER_PIECE = STRUCTURE_PIECES.register("giant_crater_piece", () -> CraterPiece::new);

    public static void register(IEventBus modBus) {
        STRUCTURE_TYPES.register(modBus);
        STRUCTURE_PIECES.register(modBus);
    }

}
