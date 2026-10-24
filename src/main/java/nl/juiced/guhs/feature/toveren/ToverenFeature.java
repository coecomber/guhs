package nl.juiced.guhs.feature.toveren;

import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.7.0 "Onder de Wol", rounds 32-37 (toveren): the guh way of enchanting. Resources: tools/features/toveren.py.
 * <ul>
 * <li><b>Het Glinsteraltaar</b> ({@link GlinsteraltaarBlock}): a guh enchanting table (a little altar of guhkristalsteen
 * with a glowing crystal core and a face; a fluffy lilac book floats above it). The vanilla mechanics (three offers,
 * lapis + levels, the player's enchantment seed, power from the ring positions) in {@link GlinsteraltaarMenu}, but its
 * power blocks are {@link #KNUFFELBOEKENKAST Knuffelboekenkasten} (vanilla bookshelves do not count, and a
 * Knuffelboekenkast does not count for the vanilla table) and its pool is the tag {@code #guhs:glinsteraltaar}. Little
 * hearts fly from the shelves to the altar ({@link #GLINSTERHARTJE}).</li>
 * <li><b>The guh enchantments</b> ({@link Spreuken}): data in data/guhs/enchantment; what data cannot do is Java in
 * {@link Spreuken} and {@link KaasknabbelGeurGoal}.</li>
 * <li><b>The Spreukjesboekje</b> ({@link Spreukjesboekje}): a vanilla enchanted book dressed up as a guh book (its own name
 * and model through the item components), so the anvil, grindstone, villagers and loot all treat it as the enchanted
 * book it is.</li>
 * </ul>
 */
public final class ToverenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    public static final DeferredBlock<GlinsteraltaarBlock> GLINSTERALTAAR = BLOCKS.registerBlock("glinsteraltaar", GlinsteraltaarBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).mapColor(MapColor.COLOR_PINK).sound(SoundType.AMETHYST)
                    .lightLevel(s -> 7));
    /** The power block of the Glinsteraltaar: a pink blossom-wood shelf with pastel books and a little plush guh per row. */
    public static final DeferredBlock<Block> KNUFFELBOEKENKAST = BLOCKS.registerSimpleBlock("knuffelboekenkast",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BOOKSHELF).mapColor(MapColor.COLOR_PINK));

    public static final DeferredItem<BlockItem> GLINSTERALTAAR_ITEM = ITEMS.registerSimpleBlockItem(GLINSTERALTAAR);
    public static final DeferredItem<BlockItem> KNUFFELBOEKENKAST_ITEM = ITEMS.registerSimpleBlockItem(KNUFFELBOEKENKAST);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GlinsteraltaarBlockEntity>> GLINSTERALTAAR_BE =
            BLOCK_ENTITIES.register("glinsteraltaar", () -> new BlockEntityType<>(GlinsteraltaarBlockEntity::new, GLINSTERALTAAR.get()));

    /** The little heart that flies from a Knuffelboekenkast to the altar (instead of vanilla's glyphs). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINSTERHARTJE = PARTICLES.register("glinsterhartje",
            () -> new SimpleParticleType(false));
    /** A pinch of fluff: Pluisval's 'poef' when you land softly on wool. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLUISPOEF = PARTICLES.register("pluispoef",
            () -> new SimpleParticleType(false));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        PARTICLES.register(modBus);
        Spreuken.register(NeoForge.EVENT_BUS);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GLINSTERALTAAR_ITEM.get()));
        output.accept(new ItemStack(KNUFFELBOEKENKAST_ITEM.get()));
    }

    private ToverenFeature() {
    }
}
