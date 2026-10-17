package nl.juiced.guhs.feature.einde2.spd1stad;

import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.sponsguh.Cast;
import nl.juiced.guhs.feature.einde2.sponsguh.SponsguhWezen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpStappen;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.einde2.verhaal.StapHint;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;

/**
 * einde2 "Sponsguh" (CONTRACT_E2_C 7.4), slice sp_d1_stad: part 1 steps 5-6 of the Sponsguh story: jellyfishing with Patguh
 * (wink 7, {@link Kwallen}) and the boating exam with Sponsguh panicking beside you (wink 5, {@link Examen}). Wired in
 * feature/einde2/Einde2 with the marker {@code <einde2_sp_d1_stad/>}. Resources: tools/features/einde2_sp_d1_stad.py
 * (+ _tekst.py: every text; _wiki.py; _plekken.py adds nothing to the builds).
 * <ul>
 *   <li>The steps {@value SpVerhaal#R_KWALLEN} and {@value SpVerhaal#R_EXAMEN} of {@link SpVerhaal#RECEPT_LIJN} are real here
 *       ({@code SpStappen.echt}); each has two halves (sleutels {@code 5}/{@code 5_b}, {@code 6}/{@code 6_b}), a {@link Doel},
 *       a {@link StapHint} (light column, "Verder met het verhaal" spot) and a {@code ga} start; the step's own look scene and
 *       hint line come when each half starts ({@link D1Stad#hint}).</li>
 *   <li>The scenes: {@link D1StadScenes}. The item: Patguh's kwallennetje ({@link #KWALLENNET}); the story vehicle: juf
 *       Pufguh's lesbootje ({@link #LESBOOT}).</li>
 *   <li>Clicks ({@code SponsguhWezen.BIJ_KLIK}, CONTRACT_E2_C 4.3): only while the clicking player is at step 5 or 6: Patguh,
 *       juf Pufguh and Sponsguh answer (and start the step's scene). Visibility: the resident juf Pufguh is never hidden; her
 *       scenes hide her near their anchor ({@code verbergEcht}).</li>
 *   <li>Dev run only: {@code /guhs einde2 sponsguh d1stad scene|spel|kwal|stand} ({@link D1StadCommando}).</li>
 * </ul>
 */
public final class E2SpD1StadFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** Patguh's net (given at the scene patguh, kept). */
    public static final DeferredItem<KwallennetItem> KWALLENNET = ITEMS.registerItem("einde2_sp_d1_stad_kwallennet", KwallennetItem::new,
            () -> new Item.Properties().stacksTo(1));
    /** Juf Pufguh's lesbootje (story vehicle of the exam; no item). */
    public static final DeferredHolder<EntityType<?>, EntityType<LesbootEntity>> LESBOOT = ENTITY_TYPES.register("einde2_sp_d1_stad_lesboot",
            () -> EntityType.Builder.<LesbootEntity>of(LesbootEntity::new, MobCategory.MISC).sized(1.6f, 0.9f).eyeHeight(0.5f).fireImmune()
                    .clientTrackingRange(10).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("einde2_sp_d1_stad_lesboot"))));

    private E2SpD1StadFeature() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        D1StadScenes.registreer();
        String lijn = D1Stad.LIJN;
        for (int stap : new int[] {D1Stad.KWALLEN, D1Stad.EXAMEN}) {
            SpStappen.echt(lijn, stap);
            SpStappen.sleutel(lijn, stap, D1Stad::sleutel);
            SpStappen.doel(lijn, stap, E2SpD1StadFeature::doel);
        }
        SpStappen.ga(lijn, D1Stad.KWALLEN, p -> naar(p, "patguh", 0.0, -5.0, 0f));
        SpStappen.ga(lijn, D1Stad.EXAMEN, p -> naar(p, "pufguh", 4.5, -0.5, 90f));
        // the hints: the target follows the half of the step the player is in; "Verder" puts them at its start
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> D1Stad.wereld(level, doelMarker(p, D1Stad.KWALLEN))))
                .baken().terug(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> terug(p, level, D1Stad.KWALLEN)))
                .zet(lijn, D1Stad.KWALLEN);
        StapHint.naar(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> D1Stad.wereld(level, doelMarker(p, D1Stad.EXAMEN))))
                .baken().terug(StapHint.plek(GuheindeFeature.GUHEINDE, (p, level) -> terug(p, level, D1Stad.EXAMEN)))
                .zet(lijn, D1Stad.EXAMEN);
        SpVerhaal.RECEPT_LIJN.opStap(E2SpD1StadFeature::opStap);
        SponsguhWezen.BIJ_KLIK.add(E2SpD1StadFeature::klik);
        LesbootEntity.BOTSING = Examen::bots;
        NeoForge.EVENT_BUS.addListener(E2SpD1StadFeature::opTick);
        NeoForge.EVENT_BUS.addListener(E2SpD1StadFeature::opWeg);
        NeoForge.EVENT_BUS.addListener(E2SpD1StadFeature::opDimensie);
        NeoForge.EVENT_BUS.addListener(E2SpD1StadFeature::opJoin);
        NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && Kwallen.tik(p, e.getTarget())) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.EntityInteract e) -> {
            if (e.getEntity() instanceof ServerPlayer p && Kwallen.tik(p, e.getTarget())) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
            }
        });
        NeoForge.EVENT_BUS.addListener(D1StadCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KWALLENNET.get()));
    }

    // =====================================================================================================================
    // the steps
    // =====================================================================================================================

    /** The marker each half of the two steps points at. */
    static String doelMarker(ServerPlayer p, int stap) {
        boolean b = D1Stad.tweedeHelft(p, stap);
        return stap == D1Stad.KWALLEN ? (b ? "kwallen_start" : "patguh") : (b ? "examen_start" : "pufguh");
    }

    /** Where "Verder met het verhaal" puts the player: in front of Patguh / juf Pufguh, at the field, beside the boat. */
    @Nullable
    static BlockPos terug(ServerPlayer p, ServerLevel level, int stap) {
        boolean b = D1Stad.tweedeHelft(p, stap);
        String m = doelMarker(p, stap);
        BlockPos w = D1Stad.wereld(level, m);
        if (w == null) {
            return null;
        }
        return stap == D1Stad.KWALLEN ? (b ? w.offset(1, 0, -3) : w.offset(0, 0, -5)) : (b ? w.offset(-2, 0, 3) : w.offset(4, 0, 0));
    }

    private static Doel doel(ServerPlayer p, int stap) {
        ServerLevel g = D1Stad.guheinde(p.level().getServer());
        BlockPos w = D1Stad.wereld(g, doelMarker(p, stap));
        if (w == null) {
            return Doel.structuur(GuheindeFeature.GUHEINDE, D1Stad.STRUCTUUR, Component.translatable("structure.guhs." + D1Stad.STRUCTUUR));
        }
        return Doel.plek(GuheindeFeature.GUHEINDE, w, Component.translatable("gui.guhs.verhalen." + D1Stad.LIJN + ".waar." + D1Stad.sleutel(p, stap)));
    }

    /** A step of part 1 changed: the hint of the new step (its look scene), the actors of the old one go. */
    private static void opStap(ServerPlayer p, int oud, int nieuw) {
        ServerLevel g = D1Stad.guheinde(p.level().getServer());
        if (oud == D1Stad.KWALLEN || nieuw < oud) {
            Kwallen.ruimOp(p, g, true);
        }
        if (oud == D1Stad.EXAMEN || nieuw < oud) {
            Examen.ruimOp(p, g);
        }
        if (nieuw == D1Stad.KWALLEN) {
            D1Stad.hint(p, nieuw, D1StadScenes.KIJK_PATGUH, "patguh", "hint.patguh", 40);
        } else if (nieuw == D1Stad.EXAMEN) {
            D1Stad.hint(p, nieuw, D1StadScenes.KIJK_PUFGUH, "pufguh", "hint.pufguh", 40);
        }
    }

    /** (ga, dev) puts the player in front of a marker of Bikinibodem, looking at it. */
    static void naar(ServerPlayer p, String marker, double dx, double dz, float yaw) {
        ServerLevel g = D1Stad.guheinde(p.level().getServer());
        Vec3 m = D1Stad.bij(g, marker, dx, 0.1, dz);
        if (m != null) {
            p.teleportTo(g, m.x, m.y, m.z, Set.of(), yaw, 10f, true);
            p.resetFallDistance();
        }
    }

    // =====================================================================================================================
    // events
    // =====================================================================================================================

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.level().isClientSide() || !GuheindeFeature.isGuheinde(p.level())) {
            return;
        }
        long nu = p.level().getServer().getTickCount();
        int stap = SpVerhaal.RECEPT_LIJN.stap(p);
        if (stap == D1Stad.KWALLEN && (nu + p.getId()) % 10 == 0 && D1Stad.aanZet(p, stap)) {
            Kwallen.tick(p, nu);
        } else if (stap == D1Stad.EXAMEN && nu % 2 == 0 && D1Stad.aanZet(p, stap)) {
            Examen.tick(p, nu);
        }
    }

    private static void opWeg(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ServerLevel g = D1Stad.guheinde(p.level().getServer());
            Kwallen.ruimOp(p, g, true);
            Examen.ruimOp(p, g);
            Kwallen.vergeet(p.getUUID());
            Examen.vergeet(p.getUUID());
        }
    }

    private static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            ServerLevel g = D1Stad.guheinde(p.level().getServer());
            Kwallen.ruimOp(p, g, true);
            Examen.ruimOp(p, g);
        }
    }

    /** An actor of one player is never kept: one that is loaded again is thrown away. */
    private static void opJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.loadedFromDisk() && event.getEntity().entityTags().contains(D1Stad.EIGEN)) {
            event.setCanceled(true);
        }
    }

    /**
     * The clicks of this slice (CONTRACT_E2_C 4.3): only for a player whose current step of part 1 is 5 or 6. Patguh / juf
     * Pufguh start the step's scene (first half) or say what to do (second half); Sponsguh cheers you on.
     */
    static boolean klik(SponsguhWezen w, ServerPlayer p) {
        int stap = SpVerhaal.RECEPT_LIJN.stap(p);
        if ((stap != D1Stad.KWALLEN && stap != D1Stad.EXAMEN) || !D1Stad.aanZet(p, stap) || Cutscenes.bezig(p)) {
            return false;
        }
        Cast c = w.cast();
        boolean tweede = D1Stad.tweedeHelft(p, stap);
        if (stap == D1Stad.KWALLEN) {
            if (c == Cast.PATGUH) {
                if (!tweede) {
                    Kwallen.begin(p);
                } else {
                    w.zetAnimatie("praat");
                    D1Stad.zeg(p, w, "klik.patguh_veld", Kwallen.NODIG - Kwallen.gevangen(p));
                }
                return true;
            }
            if (c == Cast.SPONSGUH) {
                w.zetAnimatie("praat");
                D1Stad.zeg(p, w, "klik.sponsguh_kwallen");
                return true;
            }
            return false;
        }
        if (c == Cast.JUF_PUFGUH) {
            if (!tweede) {
                Examen.begin(p);
            } else {
                w.zetAnimatie("praat");
                D1Stad.zeg(p, w, "klik.pufguh_boot");
            }
            return true;
        }
        if (c == Cast.SPONSGUH) {
            w.zetAnimatie(tweede ? "juich" : "praat");
            D1Stad.zeg(p, w, tweede ? "klik.sponsguh_boot" : "klik.sponsguh_examen");
            return true;
        }
        if (c == Cast.PATGUH) {
            w.zetAnimatie("praat");
            D1Stad.zeg(p, w, "klik.patguh_examen");
            return true;
        }
        return false;
    }
}
