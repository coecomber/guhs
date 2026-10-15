package nl.juiced.guhs.feature.einde2.trap5;

import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Einde2Haken;
import nl.juiced.guhs.feature.guhpad.GroteVerhalen;
import nl.juiced.guhs.feature.kaasmijn.LoreItem;
import nl.juiced.guhs.feature.techquest.TechquestBlocks;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * einde2 phase C, slice trap5_machines (CONTRACT_E2_B 7.12, DESIGN 6): tech tier 5, everything only after the finale of the
 * Guhvatar story.
 * <ul>
 *   <li>The Werktuigguh's workshop in the Zuidelijke Luchttempel opens ({@link Werkplaats}: the door, its scene, six
 *       practice setups) and his questline of projects ({@link E2Trap5}, {@link WerktuigguhRol}).</li>
 *   <li>The machines, all on main's vadskracht with a snoet: {@link Zender} + {@link Ontvanger}, the {@link Terminal}, the
 *       {@link Bestelbalie} with its {@link BestelguhEntity}, the {@link Poortbuis}, the {@link Thuispoort}, the four
 *       {@link Elementmachines}.</li>
 *   <li>The two new sources: the {@link Enderguhrad} and the {@link Komeetkern} (picked up once at the capital).</li>
 * </ul>
 * The recipes need the Bouwplan of the Werktuigguh (it stays in the grid, like the recipe cards of the Oude Guhrad-centrale)
 * and the materials of the Guheinde (kristalkaas, flitskruimel, doosschil, knabbelkoor). Tier 5 is not the end of the
 * technology. Resources: tools/features/einde2_trap5_machines.py (+ _tekst, _wiki). Dev: {@link E2Trap5Commando}.
 */
public final class E2Trap5Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final String BOUWPLAN_ID = "einde2_trap5_machines_bouwplan", DIPLOMA_ID = "einde2_trap5_machines_diploma";

    private static BlockBehaviour.Properties machine(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(2.5f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK).noOcclusion();
    }

    private static <B extends Block> DeferredItem<Item> item(DeferredBlock<B> blok, int vk, Rarity zeldzaam) {
        return ITEMS.registerItem(blok.getId().getPath(), p -> new T5Machine.Item(blok.get(), p, vk),
                () -> new Item.Properties().useBlockDescriptionPrefix().rarity(zeldzaam));
    }

    private static <B extends Block> DeferredBlock<B> blok(String id, Function<BlockBehaviour.Properties, B> maak, MapColor kleur) {
        return BLOCKS.registerBlock(id, maak, () -> machine(kleur));
    }

    // --- the blocks (fixed ids of CONTRACT_E2_B 1.3) ---
    public static final DeferredBlock<Zender.Block> ZENDER = blok("vadszender", Zender.Block::new, MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredItem<Item> ZENDER_ITEM = item(ZENDER, T5Getallen.ZENDER, Rarity.UNCOMMON);
    public static final DeferredBlock<Ontvanger.Block> ONTVANGER = blok("vadsontvanger", Ontvanger.Block::new, MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredItem<Item> ONTVANGER_ITEM = item(ONTVANGER, 0, Rarity.UNCOMMON);
    public static final DeferredBlock<Terminal.Block> TERMINAL = blok("guhterminal", Terminal.Block::new, MapColor.COLOR_PINK);
    public static final DeferredItem<Item> TERMINAL_ITEM = item(TERMINAL, T5Getallen.TERMINAL, Rarity.UNCOMMON);
    public static final DeferredBlock<Bestelbalie.Block> BESTELBALIE = blok("bestelbalie", Bestelbalie.Block::new, MapColor.WOOD);
    public static final DeferredItem<Item> BESTELBALIE_ITEM = item(BESTELBALIE, T5Getallen.BESTELBALIE, Rarity.UNCOMMON);
    public static final DeferredBlock<Poortbuis.Block> POORTBUIS = blok("knabbelpoort_buis", Poortbuis.Block::new, MapColor.COLOR_PURPLE);
    public static final DeferredItem<Item> POORTBUIS_ITEM = item(POORTBUIS, T5Getallen.POORTBUIS, Rarity.UNCOMMON);
    public static final DeferredBlock<Thuispoort.Block> THUISPOORT = BLOCKS.registerBlock("thuispoort", Thuispoort.Block::new,
            () -> machine(MapColor.COLOR_PURPLE).lightLevel(s -> 7));
    public static final DeferredItem<Item> THUISPOORT_ITEM = item(THUISPOORT, 0, Rarity.RARE);
    public static final DeferredBlock<Enderguhrad.Kern> ENDERGUHRAD = blok("enderguhrad", Enderguhrad.Kern::new, MapColor.COLOR_MAGENTA);
    public static final DeferredItem<Item> ENDERGUHRAD_ITEM = item(ENDERGUHRAD, -T5Getallen.ENDERGUHRAD, Rarity.RARE);
    public static final DeferredBlock<Komeetkern.Block> KOMEETKERN = BLOCKS.registerBlock("komeetkern", Komeetkern.Block::new,
            () -> machine(MapColor.COLOR_ORANGE).lightLevel(s -> 15));
    public static final DeferredItem<Item> KOMEETKERN_ITEM = item(KOMEETKERN, -T5Getallen.KOMEETKERN, Rarity.EPIC);
    public static final DeferredBlock<Elementmachines.LuchtblazerBlock> LUCHTBLAZER = blok("luchtblazer", Elementmachines.LuchtblazerBlock::new,
            MapColor.COLOR_LIGHT_GRAY);
    public static final DeferredItem<Item> LUCHTBLAZER_ITEM = item(LUCHTBLAZER, T5Getallen.LUCHTBLAZER, Rarity.COMMON);
    public static final DeferredBlock<Elementmachines.WaterdragerBlock> WATERDRAGER = blok("waterdrager", Elementmachines.WaterdragerBlock::new,
            MapColor.COLOR_BLUE);
    public static final DeferredItem<Item> WATERDRAGER_ITEM = item(WATERDRAGER, T5Getallen.WATERDRAGER, Rarity.COMMON);
    public static final DeferredBlock<Elementmachines.KorstduwerBlock> KORSTDUWER = blok("korstduwer", Elementmachines.KorstduwerBlock::new,
            MapColor.COLOR_BROWN);
    public static final DeferredItem<Item> KORSTDUWER_ITEM = item(KORSTDUWER, T5Getallen.KORSTDUWER, Rarity.COMMON);
    public static final DeferredBlock<Elementmachines.VuurkorfBlock> VUURKORF = BLOCKS.registerBlock("vuurkorf", Elementmachines.VuurkorfBlock::new,
            () -> machine(MapColor.COLOR_RED).lightLevel(s -> 10));
    public static final DeferredItem<Item> VUURKORF_ITEM = item(VUURKORF, T5Getallen.VUURKORF, Rarity.COMMON);

    // --- items ---
    /** The Werktuigguh's Bouwplan: in every tier-5 recipe, and it stays in the grid. */
    public static final DeferredItem<TechquestBlocks.Receptkaart> BOUWPLAN = ITEMS.registerItem(BOUWPLAN_ID, TechquestBlocks.Receptkaart::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    /** The diploma at the end of the line (a keepsake). */
    public static final DeferredItem<LoreItem> DIPLOMA = ITEMS.registerItem(DIPLOMA_ID, LoreItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    // --- the Bestelguh ---
    public static final DeferredHolder<EntityType<?>, EntityType<BestelguhEntity>> BESTELGUH = ENTITY_TYPES.register("bestelguh",
            () -> EntityType.Builder.of(BestelguhEntity::new, MobCategory.MISC).sized(0.9f, 1.9f).eyeHeight(1.5f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("bestelguh"))));

    // --- block entities ---
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Zender.BlockEntityImpl>> ZENDER_BE = BLOCK_ENTITIES.register("vadszender",
            () -> new BlockEntityType<>(Zender.BlockEntityImpl::new, ZENDER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Ontvanger.BlockEntityImpl>> ONTVANGER_BE = BLOCK_ENTITIES.register("vadsontvanger",
            () -> new BlockEntityType<>(Ontvanger.BlockEntityImpl::new, ONTVANGER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Terminal.BlockEntityImpl>> TERMINAL_BE = BLOCK_ENTITIES.register("guhterminal",
            () -> new BlockEntityType<>(Terminal.BlockEntityImpl::new, TERMINAL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Bestelbalie.BlockEntityImpl>> BESTELBALIE_BE = BLOCK_ENTITIES.register(
            "bestelbalie", () -> new BlockEntityType<>(Bestelbalie.BlockEntityImpl::new, BESTELBALIE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Poortbuis.BlockEntityImpl>> POORTBUIS_BE = BLOCK_ENTITIES.register(
            "knabbelpoort_buis", () -> new BlockEntityType<>(Poortbuis.BlockEntityImpl::new, POORTBUIS.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Thuispoort.BlockEntityImpl>> THUISPOORT_BE = BLOCK_ENTITIES.register(
            "thuispoort", () -> new BlockEntityType<>(Thuispoort.BlockEntityImpl::new, THUISPOORT.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Enderguhrad.BlockEntityImpl>> ENDERGUHRAD_BE = BLOCK_ENTITIES.register(
            "enderguhrad", () -> new BlockEntityType<>(Enderguhrad.BlockEntityImpl::new, ENDERGUHRAD.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Komeetkern.BlockEntityImpl>> KOMEETKERN_BE = BLOCK_ENTITIES.register(
            "komeetkern", () -> new BlockEntityType<>(Komeetkern.BlockEntityImpl::new, KOMEETKERN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Elementmachines.Luchtblazer>> LUCHTBLAZER_BE = BLOCK_ENTITIES.register(
            "luchtblazer", () -> new BlockEntityType<>(Elementmachines.Luchtblazer::new, LUCHTBLAZER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Elementmachines.Waterdrager>> WATERDRAGER_BE = BLOCK_ENTITIES.register(
            "waterdrager", () -> new BlockEntityType<>(Elementmachines.Waterdrager::new, WATERDRAGER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Elementmachines.Korstduwer>> KORSTDUWER_BE = BLOCK_ENTITIES.register(
            "korstduwer", () -> new BlockEntityType<>(Elementmachines.Korstduwer::new, KORSTDUWER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<Elementmachines.Vuurkorf>> VUURKORF_BE = BLOCK_ENTITIES.register(
            "vuurkorf", () -> new BlockEntityType<>(Elementmachines.Vuurkorf::new, VUURKORF.get()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(E2Trap5Feature::capabilities);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(BESTELGUH.get(), GuhNpcEntity.createAttributes().build()));

        E2Trap5.registreerLijn();
        GroteVerhalen.zetLijn(E2Trap5.LIJN, GroteVerhalen.Wereld.GUHEINDE);
        Werkplaats.scene();
        NpcRollen.zet(GuhNpcEntity.Kind.WERKTUIGGUH, "zuidelijke_luchttempel/werktuigguh", new WerktuigguhRol());
        Einde2Haken.WERKPLAATS_OPEN = E2Trap5::deurOpen;
        Schijn.registreer(Komeetkern::schijn);

        NeoForge.EVENT_BUS.addListener(E2Trap5Feature::opTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                Werkplaats.vergeet(p);
                Poortbuis.KOPPELT.remove(p.getUUID());
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer p) {
                Werkplaats.vergeet(p);   // (the door's answer is sent again at the next look)
            }
        });
        NeoForge.EVENT_BUS.addListener(E2Trap5Commando::register);
    }

    /** Every 10 ticks per player: the door (and its scene), the practice setups, the comet shard. */
    private static void opTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 10 != 4) {
            return;
        }
        for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
            Werkplaats.tik(p);
            Komeetkern.kijk(p);
        }
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.machineCapabilities(event, ZENDER_BE.get());
        VadskrachtFeature.knoopCapability(event, ONTVANGER_BE.get());
        VadskrachtFeature.machineCapabilities(event, TERMINAL_BE.get());
        VadskrachtFeature.machineCapabilities(event, BESTELBALIE_BE.get());
        VadskrachtFeature.machineCapabilities(event, POORTBUIS_BE.get());
        VadskrachtFeature.machineCapabilities(event, THUISPOORT_BE.get());
        VadskrachtFeature.knoopCapability(event, ENDERGUHRAD_BE.get());
        VadskrachtFeature.knoopCapability(event, KOMEETKERN_BE.get());
        VadskrachtFeature.machineCapabilities(event, LUCHTBLAZER_BE.get());
        VadskrachtFeature.machineCapabilities(event, WATERDRAGER_BE.get());
        VadskrachtFeature.machineCapabilities(event, KORSTDUWER_BE.get());
        VadskrachtFeature.machineCapabilities(event, VUURKORF_BE.get());
    }

    public static void payloads(PayloadRegistrar registrar) {
        T5Payloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<?> i : new DeferredItem<?>[]{ZENDER_ITEM, ONTVANGER_ITEM, TERMINAL_ITEM, BESTELBALIE_ITEM, POORTBUIS_ITEM, THUISPOORT_ITEM,
                LUCHTBLAZER_ITEM, WATERDRAGER_ITEM, KORSTDUWER_ITEM, VUURKORF_ITEM, ENDERGUHRAD_ITEM, KOMEETKERN_ITEM, BOUWPLAN, DIPLOMA}) {
            output.accept(new ItemStack(i.get()));
        }
    }

    private E2Trap5Feature() {
    }
}
