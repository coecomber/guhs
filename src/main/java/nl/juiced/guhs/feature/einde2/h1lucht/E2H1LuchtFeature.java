package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.balto.BaltoBlocks;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.huisje.Speelgoed;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.Halte;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Reiskaart;
import nl.juiced.guhs.feature.verhaal.Reiskaarten;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * einde2 phase B/C, slice h1_lucht (CONTRACT_E2_B 7.2): chapter 1 of the Guhvatar story (the kaasbol, the opening film, the
 * Zuidelijke Luchttempel, Zuka at the pier), the recipe of the Oog van Vadsig (a generated recipe: tools), the travel map of
 * the whole story. Wired in feature/einde2/Einde2. Resources: tools/features/einde2_h1_lucht.py (+ _tekst, _modellen); the
 * pier and Zuka's ship are in the temple's build (tools/features/einde2_bouw_lucht_bouw.py).
 * <ul>
 *   <li>{@link E2H1Lucht}: the questline and the places; {@link H1Scenes}: the eight cutscenes;</li>
 *   <li>{@link Kaasbol} + {@link KaasbolEntity}: step 0; {@link Tempel}: arriving, the hall, the stable, the door, the end
 *       and the roles of the residents; {@link Luchtbal} + {@link LuchtbalEntity}: step 3; {@link Training}: steps 4 and 5;
 *       {@link MomoJacht}: step 6; {@link Ontsnapping}: step 9;</li>
 *   <li>{@link H1Krachten}: luchtbal and luchtsprong; {@link LuchtscooterBlock}: the toy; {@link H1Commando}: the dev command.</li>
 * </ul>
 */
public final class E2H1LuchtFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Guhs.MODID);

    /** The kaasbol at the Knabbelberg (CONTRACT_E2_B 7.2; the id the skelet reserved). */
    public static final DeferredHolder<EntityType<?>, EntityType<KaasbolEntity>> KAASBOL = ENTITY_TYPES.register("kaasbol",
            () -> EntityType.Builder.<KaasbolEntity>of(KaasbolEntity::new, MobCategory.MISC).sized(1.75f, 1.75f).eyeHeight(0.9f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("kaasbol"))));
    /** The ball of the luchtbal match. */
    public static final DeferredHolder<EntityType<?>, EntityType<LuchtbalEntity>> LUCHTBAL = ENTITY_TYPES.register("einde2_h1_lucht_luchtbal",
            () -> EntityType.Builder.<LuchtbalEntity>of(LuchtbalEntity::new, MobCategory.MISC).sized(0.75f, 0.75f).eyeHeight(0.375f)
                    .clientTrackingRange(8).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("einde2_h1_lucht_luchtbal"))));

    /** The luchtscooter: a toy for guhs (the reward of chapter 1). */
    public static final DeferredBlock<LuchtscooterBlock> LUCHTSCOOTER = BLOCKS.registerBlock("luchtscooter", LuchtscooterBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.0f, 3f).sound(SoundType.WOOL).noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> LUCHTSCOOTER_ITEM = ITEMS.registerItem("luchtscooter", p -> new BaltoBlocks.LoreBlock(LUCHTSCOOTER.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());
    /** Every luchtscooter: guhs find one through it ({@link LuchtscooterBlock.Rit}). */
    public static final DeferredHolder<PoiType, PoiType> LUCHTSCOOTER_POI = POI_TYPES.register("einde2_h1_lucht_luchtscooter",
            () -> new PoiType(ImmutableSet.<BlockState>copyOf(LUCHTSCOOTER.get().getStateDefinition().getPossibleStates()), 0, 1));

    /** The source of the clothes of the chapter (text gui.guhs.kledingbron.einde2_h1_lucht). */
    public static final String KLEDINGBRON = "einde2_h1_lucht";

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        POI_TYPES.register(modBus);
        // the story (common code: both sides know the line, the scenes, the card and the map)
        E2H1Lucht.LIJN.id();
        H1Scenes.laad();
        Verteller.registreer(E2H1Lucht.KAART, 4, E2Verhaal.LUCHT);
        Reiskaarten.registreer(new Reiskaart(E2H1Lucht.REISKAART, E2Verhaal.GROEP, List.of(
                new Halte(E2Verhaal.LUCHT, 1, 60, 38, Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL),
                new Halte(E2Verhaal.WATER, 2, 204, 34, Einde2Ids.NOORDELIJKE_WATERSTAM),
                new Halte(E2Verhaal.AARDE, 3, 206, 124, Einde2Ids.BA_SING_GUH),
                new Halte(E2Verhaal.VUUR, 4, 54, 128, Einde2Ids.VUURNATIE_HOOFDSTAD),
                new Halte(E2Verhaal.FINALE, 5, 78, 112, Einde2Ids.VUURNATIE_HOOFDSTAD))));
        // the residents of the temple (CONTRACT_E2_B 4.4: every spot of zuidelijke_luchttempel but the workshop and the balloon)
        String s = Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL + "/";
        NpcRollen.zet(GuhNpcEntity.Kind.AANG_GUH, s + "aang", new Tempel.AangRol());
        NpcRollen.zet(GuhNpcEntity.Kind.GUHATSO, s + "guhatso", new Tempel.GuhatsoRol());
        NpcRollen.zet(GuhNpcEntity.Kind.ROKU_GUH, s + "roku_beeld", new Tempel.GeestRol("roku"));
        NpcRollen.zet(GuhNpcEntity.Kind.KYOSHI_GUH, s + "kyoshi_beeld", new Tempel.GeestRol("kyoshi"));
        H1Krachten.registreer();
        Schijn.registreer(Ontsnapping::vul);
        KledingBronnen.bron(GuhClothes.GUHVATAR_LUCHTNOMADE, KLEDINGBRON);
        Speelgoed.registreer(LuchtscooterBlock.SOORT);
        NeoForge.EVENT_BUS.addListener(E2H1LuchtFeature::opSpelerTick);
        NeoForge.EVENT_BUS.addListener(E2H1LuchtFeature::opUitloggen);
        NeoForge.EVENT_BUS.addListener(Tijdelijk::opJoin);
        NeoForge.EVENT_BUS.addListener(Tempel::opBlok);
        NeoForge.EVENT_BUS.addListener(Tempel::opEntity);
        NeoForge.EVENT_BUS.addListener(Tempel::opVal);
        NeoForge.EVENT_BUS.addListener(H1Commando::register);
    }

    private static void opSpelerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !GuheindeFeature.isGuheinde(p.level())) {
            if (event.getEntity() instanceof ServerPlayer p && p.tickCount % 20 == 0) {
                Kaasbol.weg(p);
            }
            return;
        }
        if (p.tickCount % 20 == 3) {
            Kaasbol.tick(p);
        }
        int stap = E2H1Lucht.LIJN.stap(p);
        if (stap < E2H1Lucht.VLUCHT || stap >= E2H1Lucht.STAPPEN) {
            return;
        }
        if (p.tickCount % 5 == 1) {
            Tempel.tick(p);
        }
        Luchtbal.tick(p);
        Training.tick(p);
        MomoJacht.tick(p);
        Ontsnapping.tick(p);
    }

    private static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        java.util.UUID u = event.getEntity().getUUID();
        Kaasbol.vergeet(u);
        Luchtbal.vergeet(u);
        Training.vergeet(u);
        MomoJacht.vergeet(u);
        Ontsnapping.vergeet(u);
        Tempel.vergeet(u);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(LUCHTSCOOTER_ITEM.get()));
    }

    private E2H1LuchtFeature() {
    }
}
