package nl.juiced.guhs.feature.einde2.h2water;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.zwevers.OtterPinguinguhEntity;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * einde2 phase B/C, slice h2_water: chapter 2 of the Guhvatar story (the Noordelijke Waterstam, the Blue Spirit, the moon
 * fish, the wave) and the story's way into the Geestenwereld (DESIGN_EINDE2 5, CONTRACT_E2_B 7.3). Wired in feature/einde2/Einde2.
 * Resources: tools/features/einde2_h2_water.py (+ _tekst.py: every text; _wiki.py).
 * <ul>
 *   <li>The questline {@link #LIJN} ({@code guhvatar_water}, group "guheinde", after {@code guhvatar_lucht}): eighteen steps,
 *       the state machine {@link E2H2Water}; the narrator card {@value E2H2Water#KAART}; the thirteen scenes
 *       {@link E2H2WaterScenes}; the four story powers {@link E2H2WaterKrachten}; the roles of the characters
 *       {@link E2H2WaterRol}; the extra cast and the actors of one player {@link Toneel}; the sneak parts {@link Sluipen};
 *       the Geestenwereld {@link Geestreis}; the fleet seen per player {@link Vloot}.</li>
 *   <li>Rewards: the boemerang ({@link #BOEMERANG}, always comes back), the clothes Waterstam-parka and Blauwe Geest-masker
 *       (source {@value #KLEDINGBRON}).</li>
 *   <li>Client: the grey world, the blizzard and Koh's blank faces ({@code client/E2H2WaterClient}).</li>
 *   <li>Dev run only: {@code /guhs einde2 h2 ga|scene|wis|stand} ({@link E2H2WaterCommando}).</li>
 * </ul>
 */
public final class E2H2WaterFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** The source of the chapter's clothes in the Kleding tab. */
    public static final String KLEDINGBRON = "einde2_h2_water";

    public static final DeferredItem<BoemerangItem> BOEMERANG = ITEMS.registerItem("boemerang", BoemerangItem::new,
            () -> new Item.Properties().stacksTo(1));
    public static final DeferredHolder<EntityType<?>, EntityType<BoemerangEntity>> BOEMERANG_VLUCHT = ENTITY_TYPES.register("einde2_h2_water_boemerang",
            () -> EntityType.Builder.<BoemerangEntity>of(BoemerangEntity::new, MobCategory.MISC).sized(0.4f, 0.4f).clientTrackingRange(6).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("einde2_h2_water_boemerang"))));

    /** The questline of chapter 2 (CONTRACT_E2_B 2.1). Texts: tools/features/einde2_h2_water_tekst.py. */
    public static final Verhaallijn LIJN = Verhaallijn.maak(E2Verhaal.WATER, E2Verhaal.GROEP).stappen(E2H2Water.STAPPEN).na(E2Verhaal.LUCHT)
            .icoon("guhs:boemerang").doelregel(true)
            .beloningen(p -> List.of(
                    Verhaallijn.beloning("guhs:blauwe_geest_masker", E2H2WaterFeature.LIJN.stap(p) > E2H2Water.ONTSNAP),
                    Verhaallijn.beloning("guhs:waterstam_parka", E2H2WaterFeature.LIJN.klaar(p)),
                    Verhaallijn.beloning("guhs:boemerang", E2H2WaterFeature.LIJN.klaar(p))))
            .doel(E2H2WaterFeature::doel)
            .registreer();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        Verteller.registreer(E2H2Water.KAART, 5, LIJN.id());
        E2H2WaterScenes.registreer();
        E2H2WaterKrachten.registreer();
        E2H2WaterRol.registreer();
        KledingBronnen.bron(GuhClothes.WATERSTAM_PARKA, KLEDINGBRON);
        KledingBronnen.bron(GuhClothes.BLAUWE_GEEST_MASKER, KLEDINGBRON);
        Bewoners.BIJ_MAKEN.add(Toneel::markeer);
        OtterPinguinguhEntity.OPGESTAPT.add(Toneel::opgestapt);
        Schijn.registreer(E2H2WaterKrachten::schijnBrug);
        Schijn.registreer(Vloot::schijn);
        NeoForge.EVENT_BUS.addListener(E2H2WaterFeature::opTick);
        NeoForge.EVENT_BUS.addListener(E2H2WaterFeature::opWeg);
        NeoForge.EVENT_BUS.addListener(E2H2WaterFeature::opDimensie);
        NeoForge.EVENT_BUS.addListener(Toneel::opJoin);
        NeoForge.EVENT_BUS.addListener((LivingEvent.LivingJumpEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Geestreis.emotie(p, "spring");
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> klik(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> klik(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> klik(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.LeftClickBlock e) -> klik(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> klik(e.getEntity()));
        NeoForge.EVENT_BUS.addListener(E2H2WaterCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        E2H2WaterPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BOEMERANG.get()));
    }

    /** Where the chapter wants the player now: the Waterstam, or in the Geestenwereld Koh / the Hemelvijver. */
    private static Doel doel(ServerPlayer p, int stap) {
        if (stap == E2H2Water.KOH || stap == E2H2Water.HEMELVIJVER) {
            var pos = Geestenwereld.wereld(stap == E2H2Water.KOH ? "koh" : "plek_hemelvijver");
            if (pos != null) {
                return Doel.plek(Geestenwereld.DIM, pos, Component.translatable(stap == E2H2Water.KOH ? "quest.guhs.einde2_h2_water.doel.koh"
                        : "quest.guhs.einde2_h2_water.doel.hemelvijver"));
            }
        }
        return Doel.structuur(GuheindeFeature.GUHEINDE, E2H2Water.STRUCTUUR, Component.translatable("structure.guhs." + E2H2Water.STRUCTUUR));
    }

    private static void klik(net.minecraft.world.entity.player.Player speler) {
        if (speler instanceof ServerPlayer p && !p.level().isClientSide()) {
            Geestreis.emotie(p, "klik");
        }
    }

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide()) {
            return;
        }
        Geestreis.tick(p);
        int stap = LIJN.stap(p);
        if ((stap == E2H2Water.ONTSNAP || stap == E2H2Water.VIS_HALEN) && p.tickCount % 2 == 0 && LIJN.aanDeBeurt(p)) {
            E2H2Water.Oord o = E2H2Water.oordVan(p);
            if (o != null) {
                Sluipen.tick(p, o, stap);
            }
        }
        if ((p.tickCount + p.getId()) % 20 == 11) {
            E2H2Water.seconde(p);
        }
    }

    private static void opWeg(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Toneel.weg(p);
            Geestreis.vergeet(p);
            E2H2Water.vergeet(p.getUUID());
        }
    }

    private static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            E2H2Water.toestandVergeten(p);
            E2H2Water.speler(p).binnen = false;
        }
    }

    private E2H2WaterFeature() {
    }
}
