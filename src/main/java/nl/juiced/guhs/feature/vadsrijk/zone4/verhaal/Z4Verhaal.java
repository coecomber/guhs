package nl.juiced.guhs.feature.vadsrijk.zone4.verhaal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

/**
 * The story moments of zone 4 (non-click camera scenes, kern RijkScene) and the flight to Lampionhaven:
 * <ul>
 *   <li>{@link #vlieg}: z4_hq01, to Lampionhaven: t-luchtschip's real airship line wins (binding {@code z4_luchtlijn}); else our
 *       first flight {@link #opening} (through the Wolkenval, over the island string, Hoogvads on the whale, grey spots on the
 *       Knuffeltij-strand) and later a short flight;</li>
 *   <li>{@link #mist}: z4_hq04, the soft flash through the grey gate on the Mysterie-eiland (the grey road east, the sucking pipe,
 *       the bowl castle under the Eye);</li>
 *   <li>{@link #feest}: z4_hq06, the back-home party on the Knuffeltij-strand, then {@link #wolkwal}: Moeder Wolkwal descends low
 *       over the sea (binding {@code z4_wolkwal_zakt} lets the world slice really lower her);</li>
 *   <li>{@link #cliffhanger}: z4_hq07, the first airship rises from the Opstijgpier to Hoogvads; a white-and-gold cape on the pier
 *       drops a grey fluff (the zone-5 spy). Sets {@code zone4_klaar} + {@code z5_luchtschip_open} first (binding
 *       {@code z4_cliffhanger} lets t-luchtschip fly a real first flight instead of our camera).</li>
 * </ul>
 * Two boss moments have a dialogue fallback when no boss slice binds them: {@link #wrak} (Kootje de Vliegervanger) and
 * {@link #zeeslurf} (the Grauwe Zeeslurf).
 */
public final class Z4Verhaal {
    public static final String SLEUTEL = "vr_z4_npc";
    public static final String VLAG_ZONE4 = "zone4_klaar";
    /** The flag t-luchtschip reads: the airship line Opstijgpier -> Walvisrughaven (line D) is open for this player. */
    public static final String VLAG_Z5_LUCHTSCHIP = "z5_luchtschip_open";
    static final String Z = "vadsrijk.z4verhaal.";
    static final int OPENING_TICKS = 460, VLIEG_TICKS = 160, MIST_TICKS = 300, FEEST_TICKS = 360, WOLKWAL_TICKS = 260, CLIFF_TICKS = 420;

    private record Loopt(String soort, long begin) {
    }

    private static final Map<UUID, Loopt> LOOPT = new HashMap<>();

    private Z4Verhaal() {
    }

    // ---- travel ----------------------------------------------------------------------------------------------------------------

    /** The line to Lampionhaven: after the zone-3 story and from level 50. */
    public static boolean magNaarLampionkust(ServerPlayer p) {
        return Questlog.vlag(p, "zone3_klaar") && Spelers.level(p) >= 50;
    }

    /** Has this player been to zone 4 (the travel map shows it as a real place then)? */
    public static boolean bereikt(ServerPlayer p) {
        return Questlog.status(p, Z4Haken.HQ01) != 0 && (Questlog.klaar(p, Z4Haken.HQ01) || Z4Haken.doelKlaar(p, Z4Haken.HQ01, 0));
    }

    /** Null when the line is open, else the lang key of why not (arg: the player's level). */
    public static String slot(ServerPlayer p) {
        if (bereikt(p)) {
            return null;
        }
        if (!Questlog.vlag(p, "zone3_klaar")) {
            return "vadsrijk.npc.reis.slot.lampionkust_verhaal";
        }
        return Spelers.level(p) < 50 ? "vadsrijk.npc.reis.slot.lampionkust_level" : null;
    }

    public static boolean openingGezien(ServerPlayer p) {
        return Spelers.data(p).gezien.contains("npc:z4_opening");
    }

    /** To Lampionhaven (quest action "z4_vlieg", the travel map, the airship NPCs). */
    public static void vlieg(ServerPlayer p) {
        Quest q = Quests.van(Z4Haken.HQ01);
        if (q != null && Questlog.beschikbaar(p, q)) {
            Questlog.neemAan(p, q);
        }
        if (NpcRollen.doe("z4_luchtlijn", p, "lampionhaven")) {
            return;   // (t-luchtschip: the real airship line C, Oosttaksteiger -> Lampionhaven)
        }
        if (!openingGezien(p)) {
            opening(p);
            return;
        }
        zet(p, Z4VerhaalPlekken.LANDING, 90f);
        Vec3[] k = Z4VerhaalPlekken.OPENING;
        RijkScenes.speel(p, RijkScene.maak("npc:z4_vlucht", VLIEG_TICKS).camera(0, k[2], k[3]).camera(VLIEG_TICKS, k[8], k[9])
                .titel(Z + "scene.vlucht.titel", Z + "scene.vlucht.onder", VLIEG_TICKS - 70, 70, 0xFF9AD0).bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.5f, 0.8f);
        loopt(p, "vlucht");
    }

    public static void opening(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z4_opening");
        zet(p, Z4VerhaalPlekken.LANDING, 90f);   // (under the camera: Lampionhaven's chunks load)
        Vec3[] k = Z4VerhaalPlekken.OPENING;
        int[] t = Z4VerhaalPlekken.OPENING_T;
        RijkScene.Builder b = RijkScene.maak("npc:z4_opening", OPENING_TICKS);
        for (int i = 0; i < t.length; i++) {
            b.camera(t[i], k[2 * i], k[2 * i + 1]);
        }
        b.camera(OPENING_TICKS, k[k.length - 2].add(0, -4, 4), k[k.length - 1]);
        RijkScenes.speel(p, b.titel(Z + "scene.opening.titel", Z + "scene.opening.onder", 140, 90, 0xFF9AD0).bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.7f, 0.7f);
        loopt(p, "opening");
    }

    private static void openingTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 cam = camera(Z4VerhaalPlekken.OPENING, Z4VerhaalPlekken.OPENING_T, t);
        if (t < 110) {
            deeltjes(p, sl, ParticleTypes.CLOUD, cam.add(0, -6, 0), 14, 10, 3, 0.02);   // (the Wolkenval: the cloud sea pours into the real sea)
        }
        if (t == 20) {
            zeg(p, "scene.opening.wolkenval", ChatFormatting.LIGHT_PURPLE);
        }
        if (t == 130) {
            zeg(p, "scene.opening.eilanden", ChatFormatting.GOLD);
        }
        if (t >= 120 && t < 220 && t % 6 == 0) {
            deeltjes(p, sl, new DustParticleOptions(0xFFB0D8, 1.6f), cam.add(10, -20, 0), 12, 20, 6, 0);   // (lantern glow)
        }
        if (t == 230) {
            zeg(p, "scene.opening.walvis", ChatFormatting.AQUA);
            geluid(p, SoundEvents.ELDER_GUARDIAN_AMBIENT, 1.2f, 0.5f);   // (a far, deep whale song)
        }
        if (t == 330) {
            zeg(p, "scene.opening.knuffels", ChatFormatting.GRAY);
        }
        if (t >= 320 && t < 400 && t % 4 == 0) {
            for (BlockPos b : Z4VerhaalPlekken.TIJKNUFFEL.values()) {
                deeltjes(p, sl, new DustParticleOptions(0x9A93A2, 1.5f), Vec3.atCenterOf(b), 3, 0.4, 0.3, 0);
            }
        }
    }

    // ---- the mist flash (z4_hq04) ---------------------------------------------------------------------------------------------

    public static void mist(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z4_mist");
        Vec3[] k = Z4VerhaalPlekken.MIST;
        RijkScenes.speel(p, RijkScene.maak("npc:z4_mist", MIST_TICKS).camera(0, k[0], k[1]).camera(110, k[2], k[3]).camera(MIST_TICKS, k[4], k[5])
                .titel(Z + "scene.mist.titel", Z + "scene.mist.onder", 20, 70, 0x9A93A2).bouw());
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
        geluid(p, SoundEvents.AMBIENT_BASALT_DELTAS_MOOD.value(), 1f, 0.6f);
        loopt(p, "mist");
    }

    private static void mistTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 cam = camera(Z4VerhaalPlekken.MIST, new int[] {0, 110, MIST_TICKS}, t);
        deeltjes(p, sl, ParticleTypes.WHITE_ASH, cam.add(6, 0, -2), 30, 8, 3, 0);
        deeltjes(p, sl, new DustParticleOptions(0x8A8494, 2.4f), cam.add(10, 0, -4), 10, 10, 3, 0);
        int[] regels = {30, 90, 150, 210, 270};
        for (int i = 0; i < regels.length; i++) {
            if (t == regels[i]) {
                zeg(p, "scene.mist." + i, i == 4 ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY);
            }
        }
        if (t >= 80 && t < 200 && t % 20 == 0) {
            geluid(p, SoundEvents.MINECART_RIDING, 0.4f, 0.5f);   // (cart wheels on the grey road)
        }
        if (t == 150) {
            geluid(p, SoundEvents.BEACON_AMBIENT, 1.2f, 0.5f);   // (the pipe hums and sucks)
        }
        if (t >= 205 && t < 260) {
            // the bowl castle on the horizon, and above it a moon that is not a moon (the Eye)
            Vec3 oog = Z4VerhaalPlekken.MIST[5].add(0, 40, 0);
            deeltjes(p, sl, new DustParticleOptions(0xC8C4D0, 3.0f), oog, 6, 3, 3, 0);
        }
    }

    // ---- the party + Moeder Wolkwal (z4_hq06) ---------------------------------------------------------------------------------

    public static void feest(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z4_feest");
        Vec3[] k = Z4VerhaalPlekken.FEEST_SCENE;
        RijkScenes.speel(p, RijkScene.maak("npc:z4_feest", FEEST_TICKS).camera(0, k[0], k[1]).camera(FEEST_TICKS / 2, k[2], k[3])
                .camera(FEEST_TICKS, k[4], k[5]).titel(Z + "scene.feest.titel", Z + "scene.feest.onder", 20, 80, 0xFF9AD0).bouw());
        NpcRollen.doe("z4_feest", p, "");   // (optional: the world/vfx slice lights real lanterns for this player)
        loopt(p, "feest");
    }

    private static void feestTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 f = Z4VerhaalPlekken.FEEST;
        int[] regels = {30, 100, 170, 240, 300};
        for (int i = 0; i < regels.length; i++) {
            if (t == regels[i]) {
                zeg(p, "scene.feest." + i, ChatFormatting.LIGHT_PURPLE);
            }
        }
        if (t % 5 == 0) {
            int[] kleur = {0xFF9AD0, 0xFFE26A, 0x9AF0C8};
            deeltjes(p, sl, new DustParticleOptions(kleur[(t / 5) % 3], 1.6f), f.add(0, 4 + (t % 40) / 6.0, 0), 10, 10, 2, 0);   // lanterns
        }
        if (t >= 100 && t < 240 && t % 8 == 0) {
            deeltjes(p, sl, ParticleTypes.HEART, f.add(0, 10 + (t - 100) / 10.0, 0), 4, 8, 2, 0);   // knuffels on kites
        }
        if (t >= 170 && t % 10 == 0) {
            deeltjes(p, sl, ParticleTypes.NOTE, f.add(0, 2, 0), 4, 5, 1, 0.5);
        }
        if (t == 100 || t == 240) {
            geluid(p, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1f, 1.2f);
        }
    }

    public static void wolkwal(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z4_wolkwal");
        Vec3[] k = Z4VerhaalPlekken.WOLKWAL;
        RijkScenes.speel(p, RijkScene.maak("npc:z4_wolkwal", WOLKWAL_TICKS).camera(0, k[0], k[1]).camera(WOLKWAL_TICKS, k[2], k[3])
                .titel(Z + "scene.wolkwal.titel", Z + "scene.wolkwal.onder", 60, 80, 0xBFE8FF).bouw());
        NpcRollen.doe("z4_wolkwal_zakt", p, "");   // (optional: the whale really comes down for this player)
        loopt(p, "wolkwal");
    }

    private static void wolkwalTik(ServerPlayer p, ServerLevel sl, int t) {
        int[] regels = {10, 70, 140, 210};
        for (int i = 0; i < regels.length; i++) {
            if (t == regels[i]) {
                zeg(p, "scene.wolkwal." + i, i == 3 ? ChatFormatting.GRAY : ChatFormatting.AQUA);
            }
        }
        // the shadow slides over the water; cloud wisps sink down from her belly
        Vec3 buik = Z4VerhaalPlekken.WALVIS_KOP.lerp(Z4VerhaalPlekken.WALVIS_RUG, 0.3).add(0, -40 - t / 4.0, 0);
        if (t % 3 == 0) {
            deeltjes(p, sl, ParticleTypes.CLOUD, buik, 20, 30, 6, 0.01);
        }
        if (t == 80 || t == 170) {
            geluid(p, SoundEvents.ELDER_GUARDIAN_AMBIENT, 2f, 0.45f);
        }
    }

    // ---- the cliffhanger (z4_hq07) ----------------------------------------------------------------------------------------------

    public static void cliffhanger(ServerPlayer p) {
        Z4VerhaalApi.zetZone4Klaar(p);
        Spelers.data(p).gezien.add("npc:z4_cliffhanger");
        if (NpcRollen.doe("z4_cliffhanger", p, "")) {
            return;   // (t-luchtschip: a real first flight up to Hoogvads)
        }
        Vec3[] k = Z4VerhaalPlekken.CLIFF;
        RijkScenes.speel(p, RijkScene.maak("npc:z4_cliff", CLIFF_TICKS).camera(0, k[0], k[1]).camera(120, k[2], k[3]).camera(260, k[4], k[5])
                .camera(300, k[6], k[7]).camera(CLIFF_TICKS, k[6].add(2, 0, 2), k[7]).titel(Z + "scene.cliff.titel", Z + "scene.cliff.onder", 20, 80, 0xBFE8FF)
                .bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.6f, 0.6f);
        loopt(p, "cliff");
    }

    private static void cliffTik(ServerPlayer p, ServerLevel sl, int t) {
        int[] regels = {20, 120, 220, 310, 370};
        for (int i = 0; i < regels.length; i++) {
            if (t == regels[i]) {
                zeg(p, "scene.cliff." + i, i >= 3 ? ChatFormatting.DARK_GRAY : ChatFormatting.AQUA);
            }
        }
        Vec3 cam = camera(Z4VerhaalPlekken.CLIFF, new int[] {0, 120, 260, 300}, Math.min(t, 300));
        if (t < 260 && t % 3 == 0) {
            deeltjes(p, sl, ParticleTypes.CLOUD, cam.add(0, -4, 0), 8, 6, 2, 0.02);
        }
        if (t == 140) {
            geluid(p, SoundEvents.ELDER_GUARDIAN_AMBIENT, 2f, 0.5f);
        }
        if (t >= 310 && t < 380) {
            // the white-and-gold cape on the pier: a grey fluff drifts out of its sleeve
            Vec3 c = Z4VerhaalPlekken.CAPE;
            deeltjes(p, sl, new DustParticleOptions(0xFFFFFF, 1.2f), c.add(0, 1.2, 0), 4, 0.3, 0.5, 0);
            deeltjes(p, sl, new DustParticleOptions(0xFFD54A, 1.0f), c.add(0, 1.0, 0), 2, 0.3, 0.4, 0);
            if (t % 4 == 0) {
                deeltjes(p, sl, new DustParticleOptions(0x8A8494, 0.9f), c.add(0.4, 1.0 - (t - 310) / 70.0, 0.2), 1, 0.02, 0.02, 0);
            }
        }
    }

    // ---- the boss moments without a boss slice (dialogue) ------------------------------------------------------------------------

    /** Kootje de Vliegervanger: the boss slice's fight when bound ({@code z4_vliegervanger}), else a short funny scene. */
    static void wrak(ServerPlayer p) {
        if (NpcRollen.doe("z4_vliegervanger", p, "")) {
            return;
        }
        NpcApi.meld(p, "z4_vliegervanger", "verslagen");
        List<Praat.Regel> r = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            r.add(new Praat.Regel(null, i < 3 ? Z + "wrak.naam" : "vadsrijk.npc.verteller", Z + "wrak." + i));
        }
        Praat.scene(p, SLEUTEL, r);
        geluid(p, SoundEvents.GENERIC_SPLASH, 1f, 0.8f);
    }

    /** The Grauwe Zeeslurf: the boss slice's fight when bound ({@code z4_zeeslurf}), else he is cheered up by a kind pat. */
    static void zeeslurf(ServerPlayer p) {
        if (NpcRollen.doe("z4_zeeslurf", p, "")) {
            return;
        }
        NpcApi.meld(p, "z4_zeeslurf", "opgevrolijkt");
        Praat.scene(p, SLEUTEL, List.of(new Praat.Regel(null, "vadsrijk.npc.verteller", Z + "zeeslurf.0"),
                new Praat.Regel(null, "vadsrijk.npc.verteller", Z + "zeeslurf.1"), new Praat.Regel(null, Z + "zeeslurf.naam", Z + "zeeslurf.2")));
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(p, ParticleTypes.BUBBLE_COLUMN_UP, true, true, p.getX(), p.getY() + 1, p.getZ(), 60, 2, 2, 2, 0.1);
            sl.sendParticles(p, ParticleTypes.HEART, true, true, p.getX(), p.getY() + 1.5, p.getZ(), 10, 1, 1, 1, 0.02);
        }
    }

    // ---- the ticker ------------------------------------------------------------------------------------------------------------

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
                case "opening" -> openingTik(p, sl, t);
                case "mist" -> mistTik(p, sl, t);
                case "feest" -> feestTik(p, sl, t);
                case "wolkwal" -> wolkwalTik(p, sl, t);
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
            case "opening" -> OPENING_TICKS;
            case "vlucht" -> VLIEG_TICKS;
            case "mist" -> MIST_TICKS;
            case "feest" -> FEEST_TICKS;
            case "wolkwal" -> WOLKWAL_TICKS;
            case "cliff" -> CLIFF_TICKS;
            default -> 0;
        };
    }

    static void einde(ServerPlayer p, String soort) {
        switch (soort) {
            case "opening", "vlucht" -> {
                NpcApi.meld(p, "z4", "aangekomen");
                zeg(p, "aankomst", ChatFormatting.LIGHT_PURPLE);
            }
            case "mist" -> NpcApi.meld(p, "z4", "mistflash");
            case "feest" -> {
                NpcApi.meld(p, "z4", "feest");
                wolkwal(p);
            }
            case "wolkwal" -> NpcApi.meld(p, "z4", "wolkwal");
            case "cliff" -> {
                RijkScenes.titel(p, Component.translatable(Z + "scene.cliff.vervolg").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                        Component.translatable(Z + "zone4.klaar"), 120, 0xBFE8FF);
                zeg(p, "zone4.klaar", ChatFormatting.LIGHT_PURPLE);
            }
            default -> {
            }
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------------------

    private static void loopt(ServerPlayer p, String soort) {
        LOOPT.put(p.getUUID(), new Loopt(soort, p.level().getGameTime()));
    }

    static Vec3 camera(Vec3[] k, int[] tijd, double t) {
        int n = Math.min(k.length / 2, tijd.length);
        for (int i = 0; i < n - 1; i++) {
            double a = tijd[i], b = tijd[i + 1];
            if (t <= b) {
                return k[2 * i].lerp(k[2 * i + 2], Math.max(0, (t - a) / (b - a)));
            }
        }
        return k[2 * (n - 1)];
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

    /** (tests) Run a running scene to its end at once (an end may start the next scene: that one too). */
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
