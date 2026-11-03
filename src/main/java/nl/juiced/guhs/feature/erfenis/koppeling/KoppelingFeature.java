package nl.juiced.guhs.feature.erfenis.koppeling;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.trap5.T5Machine;
import nl.juiced.guhs.feature.kaasmijn.LoreItem;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;

/**
 * Guhs 1.9 "Een opknapper!", slice KOPPELING: everything between the village (De Erfenis) and the player's real base.
 * <ul>
 *   <li>the entry: {@link Makelaarsbrief} (the Postguh brings it after half an hour of play since your first Guhmension visit;
 *       buy the opknapper for one kaasknabbel = the Erfhekje + moment {@code brief_gelezen});</li>
 *   <li>{@link ErfhekjeBlock} the gate to your erfenis (back = in front of the hekje you used);</li>
 *   <li>{@link Leverkist} + {@link Leverkisten} + {@link LeverkistPlafond}: the village's post home
 *       ({@code erfenis.LeverkistPost});</li>
 *   <li>{@link Dorpszender}: vadskracht from your net to the village's Vadsmast ({@code erfenis.ErfenisVadskracht});</li>
 *   <li>items: glasscherf (half-product: smelts to glass), the Guhldenzakjes and the Decobon ({@link Dorpsbeloningen}).</li>
 * </ul>
 * Saved: {@link KoppelingData}. Dev: {@link KoppelingCommando}. Resources: tools/features/erfenis_koppeling.py.
 */
public final class KoppelingFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    // --- blocks ---
    public static final DeferredBlock<ErfhekjeBlock> ERFHEKJE = BLOCKS.registerBlock("erfhekje", ErfhekjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.CHERRY_WOOD).noOcclusion()
                    .lightLevel(s -> s.getValue(ErfhekjeBlock.HALF) == DoubleBlockHalf.LOWER ? 11 : 9).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<Item> ERFHEKJE_ITEM = ITEMS.registerItem("erfhekje", p -> new DoubleHighBlockItem(ERFHEKJE.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON));

    public static final DeferredBlock<Leverkist.Block> LEVERKIST = BLOCKS.registerBlock("leverkist", Leverkist.Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<Item> LEVERKIST_ITEM = ITEMS.registerItem("leverkist", p -> new LoreItem.Block(LEVERKIST.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());

    public static final DeferredBlock<Dorpszender.Block> DORPSZENDER = BLOCKS.registerBlock("dorpszender", Dorpszender.Block::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(2.5f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .pushReaction(PushReaction.BLOCK).noOcclusion());
    public static final DeferredItem<Item> DORPSZENDER_ITEM = ITEMS.registerItem("dorpszender", p -> new T5Machine.Item(DORPSZENDER.get(), p, 0),
            () -> new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.UNCOMMON));

    // --- items ---
    public static final DeferredItem<Makelaarsbrief.BriefItem> MAKELAARSBRIEF = ITEMS.registerItem("makelaarsbrief", Makelaarsbrief.BriefItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<LoreItem> GLASSCHERF = ITEMS.registerItem("glasscherf", LoreItem::new, Item.Properties::new);
    public static final DeferredItem<Dorpsbeloningen.Guhldenzakje> GUHLDENZAKJE_KLEIN = ITEMS.registerItem("guhldenzakje_klein",
            p -> new Dorpsbeloningen.Guhldenzakje(p, 100), Item.Properties::new);
    public static final DeferredItem<Dorpsbeloningen.Guhldenzakje> GUHLDENZAKJE = ITEMS.registerItem("guhldenzakje",
            p -> new Dorpsbeloningen.Guhldenzakje(p, 500), Item.Properties::new);
    public static final DeferredItem<Dorpsbeloningen.Guhldenzakje> GUHLDENZAKJE_GROOT = ITEMS.registerItem("guhldenzakje_groot",
            p -> new Dorpsbeloningen.Guhldenzakje(p, 2000), () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Dorpsbeloningen.Decobon> DECOBON = ITEMS.registerItem("decobon", Dorpsbeloningen.Decobon::new,
            Item.Properties::new);

    // --- block entities ---
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Leverkist.BlockEntityImpl>> LEVERKIST_BE = BLOCK_ENTITIES.register("leverkist",
            () -> new BlockEntityType<>(Leverkist.BlockEntityImpl::new, LEVERKIST.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Dorpszender.BlockEntityImpl>> DORPSZENDER_BE = BLOCK_ENTITIES.register(
            "dorpszender", () -> new BlockEntityType<>(Dorpszender.BlockEntityImpl::new, DORPSZENDER.get()));

    private KoppelingFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(KoppelingFeature::capabilities);
        modBus.addListener((RegisterPayloadHandlersEvent event) -> KoppelingPayloads.register(event.registrar("1")));
        NeoForge.EVENT_BUS.addListener(KoppelingCommando::register);
        NeoForge.EVENT_BUS.addListener(Makelaarsbrief::tick);
        NeoForge.EVENT_BUS.addListener(Makelaarsbrief::opJoin);
        NeoForge.EVENT_BUS.addListener(ErfhekjeBlock::tick);
        NeoForge.EVENT_BUS.addListener(Leverkisten::tick);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, LEVERKIST_BE.get(), (kist, kant) -> VanillaContainerWrapper.of(kist));
        event.registerBlockEntity(VadsKracht.KNOOP, DORPSZENDER_BE.get(), (be, ctx) -> be);
    }
}
