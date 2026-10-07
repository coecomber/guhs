package nl.juiced.guhs.feature.snuffel;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * Het Snuffeleiland, the KERN (DESIGN_VERHALENPAD C): everything the story of the dog island stands on, without the
 * island's own build and without the dock.
 * <ul>
 *   <li>the dimension {@code guhs:snuffeleiland} (a flat sea) with ONE fixed island ({@link Eiland}; the kern ships a small
 *   test island, the island slice replaces its data and templates);</li>
 *   <li>the DOG FORM of every player there ({@link Hondvorm}), their own inventory safe in {@link SnuffelKluis}, the
 *   island's rules ({@link SnuffelEvents}), travel there and exactly back home ({@link Reis});</li>
 *   <li>the choice of dog and companion ({@link Keuze}), the residents ({@link BewonerEntity}, {@link Bewoners}), the
 *   companion only its own player sees ({@link MaatjeEntity}, {@link Maatjes});</li>
 *   <li>sniffing and digging ({@link Snuffelen}), scents and their sources ({@link Geuren}, {@link Geurbronnen}), the five
 *   ranks ({@link Rang}), good deeds ({@link Daden}), the tree and its growth scene ({@link Boom}), exams
 *   ({@link Examen});</li>
 *   <li>the Guhstation ({@link GuhstationBlock}), the memory card ({@link GeheugenkaartItem}) and the questline
 *   {@code snuffeleiland} ({@link #LIJN}) as a frame of nine steps that the dock and the village slices fill in.</li>
 * </ul>
 * For the slices that build on this: {@link Snuffel} and W/reports/slice_snuffel-kern.md. Resources:
 * tools/features/snuffel.py (models: snuffel_modellen.py, the test island: snuffel_bouw.py).
 */
public final class SnuffelFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- the Guhstation, the memory card, the tree's gift (fixed ids of DESIGN_VERHALENPAD) ------------------------------------
    public static final DeferredBlock<GuhstationBlock> GUHSTATION = BLOCKS.registerBlock("guhstation", GuhstationBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(1.0f, 6.0f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> GUHSTATION_ITEM = ITEMS.registerItem("guhstation",
            p -> new BlockItem(GUHSTATION.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("block.guhs.guhstation.lore").withStyle(ChatFormatting.GRAY));
                    tooltip.accept(Component.translatable("block.guhs.guhstation.tooltip").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }, () -> new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<GeheugenkaartItem> SNUFFEL_GEHEUGENKAART = ITEMS.registerItem("snuffel_geheugenkaart", GeheugenkaartItem::new,
            () -> new Item.Properties().stacksTo(1));
    /** What the tree gives at the end of the first series: a keepsake. */
    public static final DeferredItem<Item> SNUFFEL_BLOESEMTAKJE = ITEMS.registerItem("snuffel_bloesemtakje", p -> new Item(p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.snuffel_bloesemtakje.lore").withStyle(ChatFormatting.GRAY));
        }
    }, () -> new Item.Properties().rarity(Rarity.UNCOMMON));

    // --- entities ---------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<BewonerEntity>> SNUFFEL_BEWONER = ENTITY_TYPES.register("snuffel_bewoner",
            () -> EntityType.Builder.of(BewonerEntity::new, MobCategory.MISC).sized(0.6f, 0.85f).eyeHeight(0.65f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("snuffel_bewoner"))));
    public static final DeferredHolder<EntityType<?>, EntityType<HondEntity>> SNUFFEL_HOND = ENTITY_TYPES.register("snuffel_hond",
            () -> EntityType.Builder.of(HondEntity::new, MobCategory.MISC).sized(0.6f, 0.85f).eyeHeight(0.65f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("snuffel_hond"))));
    public static final DeferredHolder<EntityType<?>, EntityType<MaatjeEntity>> SNUFFEL_MAATJE = ENTITY_TYPES.register("snuffel_maatje",
            () -> EntityType.Builder.of(MaatjeEntity::new, MobCategory.MISC).sized(0.4f, 0.5f).eyeHeight(0.3f).clientTrackingRange(8).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("snuffel_maatje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<BoompjeEntity>> SNUFFEL_BOOMPJE = ENTITY_TYPES.register("snuffel_boompje",
            () -> EntityType.Builder.of(BoompjeEntity::new, MobCategory.MISC).sized(0.9f, 1.5f).eyeHeight(0.8f).clientTrackingRange(10).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("snuffel_boompje"))));

    // --- sounds (vanilla and guh sounds, pitched, in sounds.json; no music) -----------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> SNUF_GELUID = geluid("snuffel.snuf");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLAF_GELUID = geluid("snuffel.blaf");
    /** The "njeg" in a bark (a guh's happy squeak, high). */
    public static final DeferredHolder<SoundEvent, SoundEvent> NJEG_GELUID = geluid("snuffel.njeg");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRAAF_GELUID = geluid("snuffel.graaf");
    public static final DeferredHolder<SoundEvent, SoundEvent> KWISPEL_GELUID = geluid("snuffel.kwispel");
    public static final DeferredHolder<SoundEvent, SoundEvent> GEVONDEN_GELUID = geluid("snuffel.gevonden");
    public static final DeferredHolder<SoundEvent, SoundEvent> GELEERD_GELUID = geluid("snuffel.geleerd");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROEI_GELUID = geluid("snuffel.groei");
    public static final DeferredHolder<SoundEvent, SoundEvent> REIS_GELUID = geluid("snuffel.reis");
    public static final DeferredHolder<SoundEvent, SoundEvent> GUHSTATION_GELUID = geluid("snuffel.guhstation");
    /** The companion's giggle (for the scene in which it appears, and whenever it is up to something). */
    public static final DeferredHolder<SoundEvent, SoundEvent> MAATJE_GELUID = geluid("snuffel.maatje");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    // --- the questline: a frame of nine steps (per player) ----------------------------------------------------------------------
    /**
     * The steps of the first series, in the order of the story. The dock slice (snuffelsteiger) moves a player through
     * {@link #STAP_STEIGER} and {@link #STAP_UITVAREN}; the village slice (snuffeldorp) through the rest; the last step ends
     * with {@link Snuffel#rondAf}. Move with {@code LIJN.verder(p, STAP_X)} (only from exactly that step).
     */
    public static final int STAP_STEIGER = 0, STAP_UITVAREN = 1, STAP_STRAND = 2, STAP_DOKTER = 3, STAP_LES = 4, STAP_MAATJE = 5, STAP_DADEN = 6,
            STAP_EXAMEN = 7, STAP_SPOOR = 8, STAPPEN = 9;
    /** How many good deeds the step {@link #STAP_DADEN} asks (one per growth step of the tree). */
    public static final int DADEN_NODIG = Boom.MAX;

    public static final Verhaallijn LIJN = Verhaallijn.maak("snuffeleiland", "snuffeleiland").stappen(STAPPEN).icoon("guhs:guhstation").doelregel(true)
            .sleutel(Snuffel::sleutelVan).extraSleutels(Snuffel.SLEUTELS)
            .nodig((p, stap) -> stap == STAP_DADEN
                    ? List.of(Verhaallijn.nodig("minecraft:oak_sapling", "gui.guhs.snuffel.nodig.daden", Daden.aantal(p), DADEN_NODIG)) : List.of())
            .beloningen(Snuffel::beloningen)
            .doel(Snuffel::doelVan)
            .registreer();

    private SnuffelFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(SNUFFEL_BEWONER.get(), SnuffelHond.createAttributes().build());
            event.put(SNUFFEL_HOND.get(), SnuffelHond.createAttributes().build());
        });
        // a dog is "in a game": one game at a time, no /lobby from the island, nobody gets hurt
        Minigames.registerGame(Hondvorm.SPEL, Hondvorm::actief);
        // no fire, no flowing fluids changing the island
        Protected.add(Eiland::in);
        Boom.init();
        Bewoners.init();
        Examen.init();
        NeoForge.EVENT_BUS.register(SnuffelEvents.class);
        NeoForge.EVENT_BUS.addListener(SnuffelCommando::registreer);
    }

    public static void payloads(PayloadRegistrar registrar) {
        SnuffelPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GUHSTATION_ITEM.get()));
        output.accept(new ItemStack(SNUFFEL_BLOESEMTAKJE.get()));
    }
}
