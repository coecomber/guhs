package nl.juiced.guhs.feature.golf;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.NpcRole;

/**
 * Guhgolf: a 9-hole guh minigolf course (the guh_golfbaan structure) with the Golfguh in its guh-face clubhouse. She
 * lends you a golf club and puts a guh golf ball on the tee; right-click and hold to swing (the longer, the harder, up
 * and down again), let go to hit. Golfballetjes for every hole (more for fewer strokes) buy the golf outfit for your guh.
 * See {@link GolfGame} for the game, {@link GolfBallEntity} for the ball and tools/features/golf.py for the course.
 */
public final class GolfFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    private static BlockBehaviour.Properties courseOnly(Block like) {
        return BlockBehaviour.Properties.ofFullCopy(like).strength(-1f, 3600000f).noLootTable();
    }

    /** Pink golf felt: the floor of the lanes (also a nice building block, 4 from 4 pink wool). */
    public static final DeferredBlock<Block> VILT = BLOCKS.registerSimpleBlock("golfbaan_vilt", BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_WOOL));
    public static final DeferredBlock<GolfBlocks.Afslag> AFSLAG = BLOCKS.registerBlock("golfbaan_afslag", GolfBlocks.Afslag::new,
            courseOnly(Blocks.PINK_WOOL));
    public static final DeferredBlock<GolfBlocks.Hole> HOLE = BLOCKS.registerBlock("golfbaan_hole", GolfBlocks.Hole::new,
            courseOnly(Blocks.PINK_WOOL).noOcclusion());
    public static final DeferredBlock<GolfBlocks.Molenas> MOLENAS = BLOCKS.registerBlock("golfbaan_molenas", GolfBlocks.Molenas::new,
            courseOnly(Blocks.WHITE_WOOL));
    /** A sail of the guh windmill (placed and taken away by the Golfguh as the sails turn). */
    public static final DeferredBlock<Block> WIEK = BLOCKS.registerSimpleBlock("golfbaan_wiek", courseOnly(Blocks.WHITE_WOOL));

    public static final DeferredItem<BlockItem> VILT_ITEM = ITEMS.registerSimpleBlockItem(VILT);
    /** The golf currency: earned per hole, spent in the Golfguh's shop. */
    public static final DeferredItem<Item> GOLFBALLETJE = ITEMS.registerSimpleItem("golfballetje", new Item.Properties().rarity(Rarity.UNCOMMON));
    /** The loaned club: only exists while you play (see GolfClubItem). */
    public static final DeferredItem<GolfClubItem> GOLFCLUB = ITEMS.registerItem("guhgolfclub", GolfClubItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<EntityType<?>, EntityType<GolfBallEntity>> BALL = ENTITIES.register("guh_golfbal",
            () -> EntityType.Builder.<GolfBallEntity>of(GolfBallEntity::new, MobCategory.MISC)
                    .sized(GolfBallEntity.SIZE, GolfBallEntity.SIZE).clientTrackingRange(8).updateInterval(1)
                    .build(Guhs.id("guh_golfbal").toString()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.register(GolfProtection.class);
        nl.juiced.guhs.feature.Protected.add(GolfProtection::protectedAt);
        NeoForge.EVENT_BUS.register(GolfEvents.class);
    }

    public static void payloads(PayloadRegistrar registrar) {
        GolfPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GOLFBALLETJE.get()));
        output.accept(new ItemStack(VILT_ITEM.get()));
    }

    @Nullable
    public static NpcRole role() {
        return GolfRole.INSTANCE;
    }

    private GolfFeature() {
    }
}
