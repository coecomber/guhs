package nl.juiced.guhs.feature.vadsrijk.zone5.verhaal;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The story moments of zone 5 (non-click camera scenes, kern RijkScene) and the airship to Hoogvads:
 * <ul>
 *   <li>{@link #vlieg}: to Hoogvads. The first time {@link #aankomst}: up from the Opstijgpier along the whale's head (her eye
 *       opens and smiles), over the rim, the city in gold, down to the Luchthaven; later a short flight;</li>
 *   <li>{@link #herinnering}: flash F3 (WERELDVERHAAL 7.2), Mager Mientje's escape from Grauwhoven over the Tolbrug on a lantern
 *       kite (a narrated camera scene: the playable memory needs the empire's map, later);</li>
 *   <li>{@link #ontmasker}: z5_hq07, the unmasking in the Raadszaal: a zip in a guh suit, Kootje Zilverstaart, the flight to the
 *       blowhole;</li>
 *   <li>{@link #droom}: the FALLBACK of the finale (no fight from t-z5wezens in the tree): the spy trips over his tail, the grey
 *       dream whale turns pink to the lullaby (see {@link Z5Finale});</li>
 *   <li>{@link #ontwaken}: z5_hq09, the lullaby at the eye: Moeder Wolkwal wakes, winks; then {@code zone5_klaar};</li>
 *   <li>{@link #brief}: the Grauwe brief "Aan de Ringvinger" in the traitor's desk; {@link #cliffhanger}: z5_hq10, from the east
 *       pier: the storm wall of the Stormrand, the Grauwe Vloot, De Gouden Greep and the ten rings of Hertogin Ringeling.</li>
 * </ul>
 * Binding other slices may add (a binding wins): {@code z5_luchtlijn} (arg "hoogvads": t-luchtschip's real flight).
 */
public final class Z5Verhaal {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String SLEUTEL = "vr_z5_verhaal";
    public static final String VLAG_ZONE5 = "zone5_klaar";
    /** Zone 4's end (t-z4verhaal sets it; its exact name is our guess, the same pattern as zone2_klaar/zone3_klaar). */
    public static final String VLAG_ZONE4 = "zone4_klaar";
    public static final String Z = "vadsrijk.z5verhaal.";
    static final int AANKOMST_TICKS = 440, VLIEG_TICKS = 140, ONTMASKER_TICKS = 220, HERINNERING_TICKS = 360, DROOM_TICKS = 200, ONTWAKEN_TICKS = 220,
            CLIFF_TICKS = 300;
    static final String V = "nl.juiced.guhs.feature.vadsrijk.";

    /** Level for the line: 55 by design, clamped to the kern's level cap (still 50 in this tree: Levels.MAX_GENERIEK). */
    public static final int MIN_LEVEL = Math.min(55, nl.juiced.guhs.feature.vadsrijk.kern.Levels.MAX_GENERIEK);

    private record Loopt(String soort, long begin) {
    }

    private static final Map<UUID, Loopt> LOOPT = new HashMap<>();

    private Z5Verhaal() {
    }

    // ---- travel ----------------------------------------------------------------------------------------------------------------

    /** The airship line to Hoogvads: after zone 4 (or once zone 5 started) and from level 55. */
    public static boolean magNaarHoogvads(ServerPlayer p) {
        return magZien(p) && Spelers.level(p) >= MIN_LEVEL;
    }

    /** Is Hoogvads a destination this player knows about (zone 4 done, or zone 5 already started)? */
    public static boolean magZien(ServerPlayer p) {
        return Questlog.vlag(p, VLAG_ZONE4) || Questlog.status(p, Z5Haken.HQ01) != 0 || Questlog.vlag(p, VLAG_ZONE5);
    }

    /** Null when the line is open, else the lang key of why not (QuestMotor.reisSlot "hoogvads"). */
    public static String slot(ServerPlayer p) {
        return magNaarHoogvads(p) ? null : "vadsrijk.npc.reis.slot.hoogvads";
    }

    public static boolean aankomstGezien(ServerPlayer p) {
        return Spelers.data(p).gezien.contains("npc:z5_aankomst");
    }

    /** To Hoogvads (the first time with the opening). A binding "z5_luchtlijn" (t-luchtschip) wins for the later flights. */
    public static void vlieg(ServerPlayer p) {
        Quest q = Quests.van(Z5Haken.HQ01);
        if (q != null && Questlog.beschikbaar(p, q)) {
            Questlog.neemAan(p, q);
        }
        if (!aankomstGezien(p)) {
            aankomst(p);
            return;
        }
        if (NpcRollen.doe("z5_luchtlijn", p, "hoogvads")) {
            NpcApi.meld(p, "z5", "aangekomen");
            return;
        }
        zet(p, Z5VerhaalPlekken.AANKOMST, 180f);
        RijkScenes.speel(p, RijkScene.maak("npc:z5_vlucht", VLIEG_TICKS)
                .camera(0, Z5VerhaalPlekken.AANKOMST_SCENE[4], Z5VerhaalPlekken.AANKOMST_SCENE[5])
                .camera(VLIEG_TICKS, Z5VerhaalPlekken.AANKOMST_SCENE[6], Z5VerhaalPlekken.AANKOMST_SCENE[7])
                .titel(Z + "scene.aankomst.titel", Z + "scene.aankomst.onder", 20, 80, 0xFFB8E0).bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.5f, 0.8f);
        loopt(p, "vlucht");
    }

    // ---- the first arrival ------------------------------------------------------------------------------------------------------

    public static void aankomst(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z5_aankomst");
        zet(p, Z5VerhaalPlekken.AANKOMST, 180f);   // (under the camera's end: the city chunks load while the film runs)
        Vec3[] k = Z5VerhaalPlekken.AANKOMST_SCENE;
        int[] t = Z5VerhaalPlekken.AANKOMST_T;
        RijkScene.Builder b = RijkScene.maak("npc:z5_aankomst", AANKOMST_TICKS);
        for (int i = 0; i < t.length; i++) {
            b.camera(t[i], k[2 * i], k[2 * i + 1]);
        }
        b.camera(AANKOMST_TICKS, k[k.length - 2].add(0, -6, 6), k[k.length - 1]);
        RijkScenes.speel(p, b.titel(Z + "scene.aankomst.titel", Z + "scene.aankomst.onder", 280, 100, 0xFFB8E0).bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.7f, 0.6f);
        loopt(p, "aankomst");
    }

    private static void aankomstTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 cam = camera(Z5VerhaalPlekken.AANKOMST_SCENE, Z5VerhaalPlekken.AANKOMST_T, t);
        if (t == 10) {
            zeg(p, "scene.aankomst.zee", ChatFormatting.LIGHT_PURPLE);
        }
        if (t < 140) {
            deeltjes(p, sl, ParticleTypes.CLOUD, cam.add(4, -3, -4), 12, 8, 3, 0.02);
        }
        if (t == 150) {
            geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.6f);
            zeg(p, "scene.aankomst.oog", ChatFormatting.AQUA);
        }
        if (t >= 150 && t < 250 && t % 6 == 0) {
            deeltjes(p, sl, ParticleTypes.HEART, cam.add(6, 0, -6), 2, 4, 2, 0);
        }
        if (t == 270) {
            geluid(p, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.5f, 1.2f);
            zeg(p, "scene.aankomst.stad", ChatFormatting.GOLD);
        }
        if (t >= 260 && t < 380) {
            deeltjes(p, sl, new DustParticleOptions(0xFFB8E0, 1.6f), cam.add(0, -4, -10), 14, 14, 4, 0);
            deeltjes(p, sl, ParticleTypes.CHERRY_LEAVES, cam.add(0, 2, -8), 6, 10, 3, 0);
        }
    }

    // ---- flash F3: Mager Mientje's memory (z5_hq04) ----------------------------------------------------------------------------

    public static void herinnering(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z5_herinnering");
        Vec3[] k = Z5VerhaalPlekken.HERINNERING;
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, HERINNERING_TICKS, 0, false, false));
        RijkScenes.speel(p, RijkScene.maak("npc:z5_herinnering", HERINNERING_TICKS).camera(0, k[0], k[1]).camera(HERINNERING_TICKS, k[2], k[3])
                .titel(Z + "scene.herinnering.titel", Z + "scene.herinnering.onder", 10, 90, 0xB8A890).bouw());
        loopt(p, "herinnering");
    }

    private static void herinneringTik(ServerPlayer p, ServerLevel sl, int t) {
        int[] op = {60, 120, 180, 240, 310};
        for (int i = 0; i < op.length; i++) {
            if (t == op[i]) {
                zeg(p, "scene.herinnering." + i, i == 4 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY);
            }
        }
        Vec3 cam = camera(Z5VerhaalPlekken.HERINNERING, null, t / (double) HERINNERING_TICKS);
        deeltjes(p, sl, ParticleTypes.WHITE_ASH, cam.add(0, 0, 0), 20, 10, 4, 0);
        if (t >= 180 && t < 300 && t % 20 == 0) {
            geluid(p, SoundEvents.ELYTRA_FLYING, 0.4f, 0.6f);
        }
    }

    // ---- the unmasking (z5_hq07) ------------------------------------------------------------------------------------------------

    public static void ontmasker(ServerPlayer p) {
        if (bezig(p)) {
            return;
        }
        Spelers.data(p).gezien.add("npc:z5_ontmasker");
        Vec3[] k = Z5VerhaalPlekken.ONTMASKER;
        RijkScenes.speel(p, RijkScene.maak("npc:z5_ontmasker", ONTMASKER_TICKS).camera(0, k[0], k[1]).camera(ONTMASKER_TICKS, k[2], k[3])
                .titel(Z + "scene.ontmasker.titel", Z + "scene.ontmasker.onder", 70, 80, 0xC0C0D0).bouw());
        loopt(p, "ontmasker");
    }

    private static void ontmaskerTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 zaal = Vec3.atCenterOf(Z5VerhaalPlekken.RAADSZAAL);
        int[] op = {10, 60, 110, 170};
        for (int i = 0; i < op.length; i++) {
            if (t == op[i]) {
                zeg(p, "scene.ontmasker." + i, i == 2 ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY);
            }
        }
        if (t == 60) {
            geluid(p, SoundEvents.WOOL_BREAK, 1.5f, 0.7f);
            deeltjes(p, sl, ParticleTypes.CLOUD, zaal.add(0, 1, 0), 30, 0.6, 0.6, 0.05);
        }
        if (t == 110) {
            geluid(p, SoundEvents.NOTE_BLOCK_BASS.value(), 1.2f, 0.5f);
        }
        if (t >= 170) {
            Vec3[] v = Z5VerhaalPlekken.VLUCHT_SPION;
            double f = Math.min(1, (t - 170) / 50.0);
            Vec3 w = f < 0.5 ? v[0].lerp(v[1], f * 2) : v[1].lerp(v[2], f * 2 - 1);
            deeltjes(p, sl, new DustParticleOptions(0x9AA0AA, 1.4f), w, 6, 0.3, 0.3, 0);
            if (t == 175) {
                geluid(p, SoundEvents.GLASS_BREAK, 1.2f, 1.2f);
            }
        }
    }

    // ---- the finale fallback (no fight in this tree) ------------------------------------------------------------------------------

    /** Our own camera scene for one phase of the finale; at its end the phase counts (Z5Finale.spionVerslagen / droomOpgevrolijkt). */
    public static void droom(ServerPlayer p, Z5Finale.Fase fase) {
        Vec3[] k = Z5VerhaalPlekken.DROOM;
        RijkScene.Builder b = RijkScene.maak("npc:z5_droom_" + fase.name().toLowerCase(java.util.Locale.ROOT), DROOM_TICKS).camera(0, k[0], k[1])
                .camera(DROOM_TICKS, k[2], k[3]);
        if (fase == Z5Finale.Fase.DROOM) {
            b.titel(Z + "scene.droom.titel", Z + "scene.droom.onder", 10, 80, 0x9A90A8);
        }
        RijkScenes.speel(p, b.bouw());
        loopt(p, fase == Z5Finale.Fase.SPION ? "spion" : "droom");
    }

    private static void droomTik(ServerPlayer p, ServerLevel sl, int t, boolean spion) {
        Vec3 c = Vec3.atCenterOf(Z5VerhaalPlekken.WOLKWALDROOM);
        int basis = spion ? 0 : 3;
        int[] op = {10, 80, 150};
        for (int i = 0; i < op.length; i++) {
            if (t == op[i]) {
                zeg(p, "scene.droom." + (basis + i), ChatFormatting.LIGHT_PURPLE);
            }
        }
        boolean roze = !spion && t > 150;
        deeltjes(p, sl, new DustParticleOptions(roze ? 0xFF8AD0 : 0x8A8A98, 2.6f), c.add(0, 2, 0), 24, 8, 3, 0);
        if (!spion && t >= 80 && t % 15 == 0) {
            geluid(p, SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.2f, 0.8f + (t % 45) / 60f);
        }
        if (roze && t % 8 == 0) {
            deeltjes(p, sl, ParticleTypes.HEART, c.add(0, 3, 0), 4, 6, 2, 0);
        }
    }

    // ---- waking the whale (z5_hq09) -------------------------------------------------------------------------------------------------

    public static void ontwaken(ServerPlayer p) {
        if (bezig(p)) {
            return;
        }
        Spelers.data(p).gezien.add("npc:z5_ontwaken");
        Vec3[] k = Z5VerhaalPlekken.ONTWAKEN;
        RijkScenes.speel(p, RijkScene.maak("npc:z5_ontwaken", ONTWAKEN_TICKS).camera(0, k[0], k[1]).camera(ONTWAKEN_TICKS, k[2], k[3])
                .titel(Z + "scene.ontwaken.titel", Z + "scene.ontwaken.onder", 150, 70, 0xA8D8FF).bouw());
        loopt(p, "ontwaken");
    }

    private static void ontwakenTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 oog = Z5VerhaalPlekken.ONTWAKEN[1];
        if (t % 12 == 0 && t < 120) {
            geluid(p, SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.3f, 0.6f + (t % 36) / 40f);
            deeltjes(p, sl, ParticleTypes.NOTE, Vec3.atCenterOf(Z5VerhaalPlekken.BALKON_OOG).add(0, 2, 0), 3, 1, 0.5, 0);
        }
        if (t == 20) {
            zeg(p, "scene.ontwaken.0", ChatFormatting.AQUA);
        }
        if (t == 110) {
            zeg(p, "scene.ontwaken.1", ChatFormatting.GOLD);
            geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.5f);
            for (BlockPos s : List.of(Z5VerhaalPlekken.SPUITGAT, Z5VerhaalPlekken.POI.get("z5_spuitgat_noord"), Z5VerhaalPlekken.POI.get("z5_spuitgat_zuid"))) {
                deeltjes(p, sl, ParticleTypes.CLOUD, Vec3.atCenterOf(s).add(0, 4, 0), 80, 1.5, 8, 0.15);
            }
        }
        if (t == 180) {
            zeg(p, "scene.ontwaken.2", ChatFormatting.LIGHT_PURPLE);
            deeltjes(p, sl, ParticleTypes.HEART, oog, 20, 6, 3, 0);
        }
    }

    /** The end of zone 5's main story: the flag (once), the title card, the listeners. */
    static void zone5Klaar(ServerPlayer p) {
        if (Z5VerhaalApi.zone5Klaar(p)) {
            return;
        }
        Questlog.zetVlag(p, VLAG_ZONE5, true);
        RijkScenes.titel(p, Component.translatable(Z + "zone5.klaar.titel").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Component.translatable(Z + "zone5.klaar.onder"), 80, 0xFFB8E0);
        zeg(p, "zone5.klaar", ChatFormatting.LIGHT_PURPLE);
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1.2f);
        Z5VerhaalApi.ZONE5_KLAAR.forEach(h -> h.accept(p));
    }

    // ---- the Grauwe brief + the cliffhanger (z5_hq10) -------------------------------------------------------------------------------

    /** The letter in the traitor's desk: "Aan de Ringvinger". Once. */
    static void brief(ServerPlayer p) {
        if (!Z5Haken.eenmalig(p, "grauwe_brief")) {
            return;
        }
        Questlog.geef(p, new ItemStack(Z5VerhaalRegistry.item("grauwe_brief")));
        Questlog.map(p, "z5_brieven").putBoolean("ringvinger", true);
        GuhAdvancements.grant(p, "vr_z5_grauwe_brief");
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.7f);
        Praat.scene(p, SLEUTEL, List.of(new Praat.Regel(null, "vadsrijk.npc.verteller", Z + "brief.0"),
                new Praat.Regel(null, Z + "brief.naam", Z + "brief.1"), new Praat.Regel(null, Z + "brief.naam", Z + "brief.2")));
    }

    public static void cliffhanger(ServerPlayer p) {
        if (bezig(p)) {
            return;
        }
        Spelers.data(p).gezien.add("npc:z5_cliffhanger");
        Vec3[] k = Z5VerhaalPlekken.CLIFFHANGER;
        RijkScenes.speel(p, RijkScene.maak("npc:z5_cliff", CLIFF_TICKS).camera(0, k[0], k[1]).camera(CLIFF_TICKS, k[2], k[3])
                .titel(Z + "scene.cliff.titel", Z + "scene.cliff.onder", CLIFF_TICKS - 90, 90, 0xD8C060).bouw());
        loopt(p, "cliff");
    }

    private static void cliffTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 s = Z5VerhaalPlekken.STORMRAND;
        int[] op = {20, 90, 150, 200};
        for (int i = 0; i < op.length; i++) {
            if (t == op[i]) {
                zeg(p, "scene.cliff." + i, i == 3 ? ChatFormatting.GOLD : ChatFormatting.GRAY);
            }
        }
        // the storm wall: grey mist, lightning flashes; in the flashes the masts and a golden ship with ten glints
        deeltjes(p, sl, new DustParticleOptions(0x5A5A66, 4f), s.add(-60, 10, 0), 40, 30, 25, 0);
        if (t % 40 == 5) {
            deeltjes(p, sl, ParticleTypes.ELECTRIC_SPARK, s.add(0, 30, (t % 80) - 40), 1, 0, 0, 0);
            geluid(p, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.6f, 0.6f);
        }
        if (t >= 150 && t < 200 && t % 5 == 0) {
            for (int i = 0; i < 10; i++) {
                deeltjes(p, sl, new DustParticleOptions(0xFFD24A, 1.2f), s.add(-80 + i * 1.5, -8, 6), 1, 0.1, 0.1, 0);
            }
        }
    }

    // ---- ticking -------------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || LOOPT.isEmpty()) {
            return;
        }
        long nu = sl.getGameTime();
        List<Runnable> daarna = new ArrayList<>();
        Iterator<Map.Entry<UUID, Loopt>> it = LOOPT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Loopt> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Loopt l = e.getValue();
            if (p == null || p.level() != sl) {
                it.remove();
                continue;
            }
            int t = (int) (nu - l.begin());
            switch (l.soort()) {
                case "aankomst" -> aankomstTik(p, sl, t);
                case "herinnering" -> herinneringTik(p, sl, t);
                case "ontmasker" -> ontmaskerTik(p, sl, t);
                case "spion" -> droomTik(p, sl, t, true);
                case "droom" -> droomTik(p, sl, t, false);
                case "ontwaken" -> ontwakenTik(p, sl, t);
                case "cliff" -> cliffTik(p, sl, t);
                default -> {
                }
            }
            if (t >= duur(l.soort())) {
                it.remove();
                daarna.add(() -> einde(p, l.soort()));
            }
        }
        daarna.forEach(Runnable::run);
    }

    static int duur(String soort) {
        return switch (soort) {
            case "aankomst" -> AANKOMST_TICKS;
            case "vlucht" -> VLIEG_TICKS;
            case "herinnering" -> HERINNERING_TICKS;
            case "ontmasker" -> ONTMASKER_TICKS;
            case "spion", "droom" -> DROOM_TICKS;
            case "ontwaken" -> ONTWAKEN_TICKS;
            case "cliff" -> CLIFF_TICKS;
            default -> 0;
        };
    }

    /** What happens when a scene ends (also called at once by {@link #spoel} in tests). */
    static void einde(ServerPlayer p, String soort) {
        switch (soort) {
            case "aankomst" -> {
                zeg(p, "scene.aankomst.welkom", ChatFormatting.LIGHT_PURPLE);
                NpcApi.meld(p, "z5", "aangekomen");
            }
            case "vlucht" -> NpcApi.meld(p, "z5", "aangekomen");
            case "herinnering" -> NpcApi.meld(p, "z5", "herinnering");
            case "ontmasker" -> {
                NpcApi.meld(p, "z5", "ontmaskerd");
                zeg(p, "finale.wacht", ChatFormatting.RED);
            }
            case "spion" -> Z5Finale.spionVerslagen(p);
            case "droom" -> Z5Finale.droomOpgevrolijkt(p);
            case "ontwaken" -> NpcApi.meld(p, "z5", "wolkwal_wakker");
            case "cliff" -> NpcApi.meld(p, "z5", "cliffhanger");
            default -> {
            }
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------------------

    private static void loopt(ServerPlayer p, String soort) {
        LOOPT.put(p.getUUID(), new Loopt(soort, p.level().getGameTime()));
    }

    /** The camera position at tick t (keyframe times {@code tijd}, or evenly spread over 0..1 when null and t is a fraction). */
    static Vec3 camera(Vec3[] k, int[] tijd, double t) {
        int n = k.length / 2;
        for (int i = 0; i < n - 1; i++) {
            double a = tijd == null ? i / (double) (n - 1) : tijd[i], b = tijd == null ? (i + 1) / (double) (n - 1) : tijd[i + 1];
            if (t <= b) {
                return k[2 * i].lerp(k[2 * i + 2], Math.max(0, (t - a) / (b - a)));
            }
        }
        return k[k.length - 2];
    }

    static void zeg(ServerPlayer p, String key, ChatFormatting kleur) {
        p.sendSystemMessage(Component.translatable(Z + key).withStyle(kleur, ChatFormatting.ITALIC));
    }

    static void zet(ServerPlayer p, BlockPos b, float yaw) {
        if (!(p.level() instanceof ServerLevel sl && Vadsrijk.in(sl))) {
            return;   // (only inside the Vadsrijk; tests stay put)
        }
        double y = Npcs.vloer(sl, b);
        p.teleport(new TeleportTransition(sl, new Vec3(b.getX() + 0.5, y, b.getZ() + 0.5), Vec3.ZERO, yaw, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
    }

    static void deeltjes(ServerPlayer p, ServerLevel sl, ParticleOptions soort, Vec3 v, int n, double spreid, double hoog, double snel) {
        sl.sendParticles(p, soort, true, true, v.x, v.y, v.z, n, spreid, hoog, spreid, snel);
    }

    static void geluid(ServerPlayer p, SoundEvent s, float volume, float toon) {
        geluid(p, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s), volume, toon);
    }

    static void geluid(ServerPlayer p, Holder<SoundEvent> s, float volume, float toon) {
        p.connection.send(new ClientboundSoundPacket(s, SoundSource.AMBIENT, p.getX(), p.getY(), p.getZ(), volume, toon, p.getRandom().nextLong()));
    }

    /** Another slice's static method (class#method, below feature/vadsrijk) taking (ServerPlayer) or (ServerPlayer, String). */
    static boolean reflectie(ServerPlayer p, String arg, String... namen) {
        for (String naam : namen) {
            int h = naam.indexOf('#');
            Class<?> c;
            try {
                c = Class.forName(V + naam.substring(0, h));
            } catch (ClassNotFoundException | LinkageError e) {
                continue;
            }
            for (Method m : c.getMethods()) {
                if (!m.getName().equals(naam.substring(h + 1)) || !Modifier.isStatic(m.getModifiers())) {
                    continue;
                }
                Class<?>[] t = m.getParameterTypes();
                try {
                    Object r;
                    if (t.length == 1 && t[0].isAssignableFrom(ServerPlayer.class)) {
                        r = m.invoke(null, p);
                    } else if (t.length == 2 && t[0].isAssignableFrom(ServerPlayer.class) && t[1] == String.class) {
                        r = m.invoke(null, p, arg);
                    } else {
                        continue;
                    }
                    if (!(r instanceof Boolean b) || b) {
                        return true;
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    LOG.warn("Vadsrijk z5 verhaal: adapter {} failed", naam, e);
                }
            }
        }
        return false;
    }

    /** (tests/dev) */
    static void vergeet(ServerPlayer p) {
        LOOPT.remove(p.getUUID());
    }

    static boolean bezig(ServerPlayer p) {
        return LOOPT.containsKey(p.getUUID());
    }

    /** (tests) Is this scene running for the player? */
    static boolean speelt(ServerPlayer p, String soort) {
        Loopt l = LOOPT.get(p.getUUID());
        return l != null && l.soort().equals(soort);
    }

    /** (tests) Run a running scene to its end at once (the end of a scene may start the next one: that one too). */
    static void spoel(ServerPlayer p) {
        for (int i = 0; i < 4; i++) {
            Loopt l = LOOPT.remove(p.getUUID());
            if (l == null) {
                return;
            }
            einde(p, l.soort());
        }
    }
}
