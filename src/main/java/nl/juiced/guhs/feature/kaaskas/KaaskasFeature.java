package nl.juiced.guhs.feature.kaaskas;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.techsaus.SausMachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * 1.8.0 (vadskracht, De Fabriekshal): de Kaaskas and de Guh-sorteerband.
 * <ul>
 *   <li>{@link KaaskasBlock}: the kern of a glass greenhouse; on vadskracht + kaassaus the crops around it grow much faster
 *       (a full kas: a few thousand knabbels an hour). The new crop {@link PluisbolletjeBlock} (pluisbolletje, gives pluis)
 *       grows best in it. The guh chore {@link KassierKlus} picks the ripe kas empty and plants it again.</li>
 *   <li>{@link SorteerbandBlock}: a belt that carries things to its end, where a guh with the chore {@link SorteerderKlus}
 *       sorts them into the chest that already holds that kind.</li>
 * </ul>
 * Resources and texts: tools/features/kaaskas.py. {@link #PLUIS} is TEMPORARY: the pluis item of the vadskracht "fabriek"
 * branch has the same id ({@code guhs:pluis}); at the merge only one of the two may stay.
 */
public final class KaaskasFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<KaaskasBlock> KAASKAS = BLOCKS.registerBlock("kaaskas", KaaskasBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).strength(3.0f, 6f).noOcclusion()
                    .sound(SoundType.GLASS).isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                    .isViewBlocking((s, l, p) -> false).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> KAASKAS_ITEM = ITEMS.registerSimpleBlockItem(KAASKAS);

    public static final DeferredBlock<PluisbolletjeBlock> PLUISBOLLETJE = BLOCKS.registerBlock("pluisbolletje_plant", PluisbolletjeBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.WHEAT).mapColor(MapColor.SNOW));
    public static final DeferredItem<BlockItem> PLUIS_ZAAD = ITEMS.registerItem("pluisbolletje_zaad",
            p -> new BlockItem(PLUISBOLLETJE.get(), p), () -> new Item.Properties().useItemDescriptionPrefix());
    /** TEMPORARY (see the class comment): pluis, until the fabriek branch's pluis is merged. */
    public static final DeferredItem<Item> PLUIS = ITEMS.registerSimpleItem("pluis");

    public static final DeferredBlock<SorteerbandBlock> SORTEERBAND = BLOCKS.registerBlock("sorteerband", SorteerbandBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f, 3f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> SORTEERBAND_ITEM = ITEMS.registerSimpleBlockItem(SORTEERBAND);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KaaskasBlockEntity>> KAASKAS_BE = BLOCK_ENTITIES.register("kaaskas",
            () -> new BlockEntityType<>(KaaskasBlockEntity::new, KAASKAS.get()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(KaaskasFeature::capabilities);
        KlusGebied.registreer(KassierKlus.KAS, (level, pos, state) -> state.getBlock() instanceof KaaskasBlock);
        KlusGebied.registreer(SorteerderKlus.BANDEIND, (level, pos, state) -> state.getBlock() instanceof SorteerbandBlock
                && SorteerbandBlock.isEind(level, pos));
        Klusjes.registreer(new KassierKlus());
        Klusjes.registreer(new SorteerderKlus());
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.machineCapabilities(event, KAASKAS_BE.get());
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, KAASKAS_BE.get(), SausMachineBlockEntity::sausHandler);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KAASKAS_ITEM.get()));
        output.accept(new ItemStack(PLUIS_ZAAD.get()));
        output.accept(new ItemStack(PLUIS.get()));
        output.accept(new ItemStack(SORTEERBAND_ITEM.get()));
    }

    private KaaskasFeature() {
    }
}
