package nl.juiced.guhs.feature.eilanden;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Zwevende guh-eilandjes (floating guh islands): a very rare cluster of islands high in the Guhmension sky, with a
 * guh-face rock pouring kaassaus, guhbloesem trees and bridges. On the main island lives the one and only Wolkguh
 * ({@link nl.juiced.guhs.entity.GuhVariant#WOLK}, tamed with patience: {@link EilandenEvents}). A wolkenlift on the
 * ground below lifts you up (no own items needed); a second one floats you down, and the clouds catch anyone who falls.
 */
public final class EilandenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The structure (data/guhs/worldgen/structure/zwevende_eilanden.json). */
    public static final ResourceKey<Structure> ISLANDS = ResourceKey.create(Registries.STRUCTURE, Guhs.id("zwevende_eilanden"));

    /** The lift pad: makes a wolkenstroom column above it (up, or down when placed sneaking). */
    public static final DeferredBlock<WolkenliftBlock> WOLKENLIFT = BLOCKS.registerBlock("wolkenlift", WolkenliftBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.8f).sound(SoundType.WOOL).lightLevel(s -> 10)
                    .pushReaction(PushReaction.BLOCK));
    /** The invisible cloud stream of a wolkenlift (like a bubble column). */
    public static final DeferredBlock<WolkenstroomBlock> WOLKENSTROOM = BLOCKS.registerBlock("wolkenstroom", WolkenstroomBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.NONE).noCollission().noLootTable().replaceable().noOcclusion()
                    .pushReaction(PushReaction.DESTROY).isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));

    public static final DeferredItem<BlockItem> WOLKENLIFT_ITEM = ITEMS.registerSimpleBlockItem(WOLKENLIFT);
    /** Cloud candy floss from the treasure in the guh-face rock: floating down softly for a while. */
    public static final DeferredItem<Item> WOLKENSUIKERSPIN = ITEMS.registerSimpleItem("wolkensuikerspin", new Item.Properties().stacksTo(16)
            .food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3f).fast().alwaysEdible()
                    .effect(() -> new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 45, 0), 1f).build()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.register(EilandenEvents.class);
        NeoForge.EVENT_BUS.register(EilandenProtection.class);
        nl.juiced.guhs.feature.Protected.add(EilandenProtection::protectedAt);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(WOLKENLIFT_ITEM.get()));
        output.accept(new ItemStack(WOLKENSUIKERSPIN.get()));
    }

    @Nullable
    public static NpcRole role() {
        return null;
    }

    /** The floating islands structure at this spot (in its building, not just its area), or null. */
    @Nullable
    public static StructureStart islandsAt(ServerLevel level, BlockPos pos) {
        if (level.dimension() != ModDimensions.GUHMENSION) {
            return null;
        }
        Structure structure = level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ISLANDS);
        if (structure == null) {
            return null;
        }
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, structure);
        return start.isValid() ? start : null;
    }

    private EilandenFeature() {
    }
}
