package nl.juiced.guhs.feature.slijmhuisje;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * 1.10.10: Het Slijmhuisje, a sweet little house of 3 x 3 (about 3 high) for one guh slime.
 * <ul>
 *   <li>{@link #SLIJMHUISJE} ({@link SlijmhuisjeBlock}, + 26 {@link #SLIJMHUISJE_DEEL}): the house; on vadskracht
 *       ({@code VadsGetallen.SLIJMHUISJE}) the slime that lives in it makes a guh slimeball every 45 seconds, into a bowl of
 *       16 ({@link SlijmhuisjeBlockEntity});</li>
 *   <li>{@link #OPGEPAKTE_GUH_SLIME} ({@link OpgepakteGuhSlimeItem}): sneak + right-click a guh slime with an empty hand to
 *       carry it; right-click a block to put it down, or the house to let it move in;</li>
 *   <li>the chore {@link SlijmpjesKlus "Slijmpjes verzorgen"} (on by default): a huisje guh pets the slime and brings the
 *       slimeballs home.</li>
 * </ul>
 * Resources: tools/features/slijmhuisje.py. Client: {@code client.SlijmhuisjeClient}.
 */
public final class SlijmhuisjeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** The name of the houses in the chores' scan of a home base ({@link KlusGebied#registreer}). */
    public static final String KLUS_SOORT = "slijmhuisje";

    public static final DeferredBlock<SlijmhuisjeBlock> SLIJMHUISJE = BLOCKS.registerBlock("slijmhuisje", SlijmhuisjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0f, 3f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> SLIJMHUISJE_ITEM = ITEMS.registerSimpleBlockItem(SLIJMHUISJE);
    /** The 26 other blocks of a Slijmhuisje (no item: they come and go with the kern). */
    public static final DeferredBlock<SlijmhuisjeDeelBlock> SLIJMHUISJE_DEEL = BLOCKS.registerBlock("slijmhuisje_deel", SlijmhuisjeDeelBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0f, 3f).sound(SoundType.WOOL).noOcclusion()
                    .noLootTable().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<OpgepakteGuhSlimeItem> OPGEPAKTE_GUH_SLIME = ITEMS.registerItem("opgepakte_guh_slime", OpgepakteGuhSlimeItem::new,
            () -> new Item.Properties().stacksTo(1));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SlijmhuisjeBlockEntity>> SLIJMHUISJE_BE = BLOCK_ENTITIES.register(
            "slijmhuisje", () -> new BlockEntityType<>(SlijmhuisjeBlockEntity::new, SLIJMHUISJE.get()));

    public static final SlijmpjesKlus KLUS = new SlijmpjesKlus();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(SlijmhuisjeFeature::capabilities);
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract event) -> OpgepakteGuhSlimeItem.oppakken(event));
        Klusjes.registreer(KLUS);
        KlusGebied.registreer(KLUS_SOORT, (level, pos, state) -> state.getBlock() instanceof SlijmhuisjeBlock);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.machineCapabilities(event, SLIJMHUISJE_BE.get());
        VadskrachtFeature.deelCapabilities(event, SLIJMHUISJE_DEEL.get());
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SLIJMHUISJE_ITEM.get()));
    }

    private SlijmhuisjeFeature() {
    }
}
