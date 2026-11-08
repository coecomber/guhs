package nl.juiced.guhs.feature.vadsrijk.zone6.verhaal;

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
import nl.juiced.guhs.feature.vadsrijk.economie.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The story moments of zone 6 (non-click camera scenes, kern RijkScene; all data-driven: {@link #DEFS}) plus the travel and the
 * rewards that live in other slices:
 * <ul>
 *   <li>{@link #wolkwaltocht}: right after zone 5 (z5_hq10 handed in): the EPIC film of Moeder Wolkwal swimming east with all of
 *       Hoogvads to the pink cliffs of the Zijdeland. ONLY a film: in the world Hoogvads stays where it is (user decision Z6-6). At
 *       its end the flag {@link #VLAG_LIJN_E} opens airship line E (Z6Lijn) and z6_hq01 starts;</li>
 *   <li>{@link #vlieg} / {@link #aankomst}: to Rozenkloof (the travel map / the havenmeesters; the first time the arrival film);
 *       a landing of line E at Rozenkloof counts too ({@link Z6Lijn});</li>
 *   <li>{@link #visioen}: Moeder Bron's vision after each beacon (0 = the tutorial at Twinkel's telescope; 1..5 stronger each time);</li>
 *   <li>zoutkolos (hq07), kerker (hq08 fallback when no dungeon is in the tree), ontmasker + ontsnapt (hq09), luchtspiegeling,
 *       bron + toren (hq10: Ringeling flees upward, the story ends at the foot of the tower), and after the RAID: ringeling (she
 *       is cheered up, {@link #ringeling}) followed by flash F4 (the Keizer loses her; the Oog turns to Hoogvads).</li>
 * </ul>
 */
public final class Z6Verhaal {
    private static final Logger LOG = LogUtils.getLogger();
    public static final String SLEUTEL = "vr_z6_verhaal";
    public static final String Z = "vadsrijk.z6verhaal.";
    /** Set when the great swim is done: line E (Hoogvads - Rozenkloof) is open, z6_hq01 starts. */
    public static final String VLAG_LIJN_E = "z6_lijn_e_open";
    /** The end of the zone-6 story (z6_hq10): the tower's door is "sealed", the raid opens. */
    public static final String VLAG_ZONE6 = "zone6_klaar";
    /** After the first raid clear: Ringeling is cheered up and an ally in Saffraanpoort. */
    public static final String VLAG_RINGELING = "z6_ringeling_vriend";
    /** Rewards that wait for their slice (the species / the looks are not in the tree yet): retried every 5 s. */
    static final String VLAG_BULTGUH = "z6_bultguh_verdiend", VLAG_TAPIJT = "z6_tapijt_verdiend";
    static final String V = "nl.juiced.guhs.feature.vadsrijk.";
    /** Level for line E (and the Zijdeland on the travel map). */
    public static final int MIN_LEVEL = 60;

    /** A scene: duration, the text lines (lang keys under Z) and when they come, title timing, colours, darkness, its particles. */
    record Def(String id, int ticks, String[] regels, int[] op, int titelOp, int titelKleur, ChatFormatting kleur, boolean donker, ParticleOptions deeltje) {
    }

    static final Map<String, Def> DEFS = new HashMap<>();

    private static void def(String id, int ticks, int regels, int titelOp, int titelKleur, ChatFormatting kleur, boolean donker, ParticleOptions d) {
        String[] r = new String[regels];
        int[] op = new int[regels];
        for (int i = 0; i < regels; i++) {
            r[i] = "scene." + id + "." + i;
            op[i] = 15 + i * Math.max(30, (ticks - 60) / Math.max(1, regels));
        }
        DEFS.put(id, new Def(id, ticks, r, op, titelOp, titelKleur, kleur, donker, d));
    }

    static {
        def("wolkwaltocht", 520, 5, 400, 0xFFB8E0, ChatFormatting.LIGHT_PURPLE, false, ParticleTypes.CLOUD);
        def("aankomst", 360, 4, 250, 0xFFC8A0, ChatFormatting.GOLD, false, ParticleTypes.CHERRY_LEAVES);
        def("zoutkolos", 260, 4, 30, 0xC8C8D8, ChatFormatting.GRAY, false, ParticleTypes.WHITE_ASH);
        def("ontmasker", 320, 6, 160, 0xC0E0FF, ChatFormatting.AQUA, false, ParticleTypes.END_ROD);
        def("ontsnapt", 220, 3, 120, 0x9A90A8, ChatFormatting.GRAY, false, ParticleTypes.WHITE_ASH);
        def("luchtspiegeling", 220, 3, 20, 0xFFE0B0, ChatFormatting.YELLOW, false, ParticleTypes.END_ROD);
        def("bron", 260, 4, 170, 0x80D8FF, ChatFormatting.AQUA, false, ParticleTypes.SPLASH);
        def("toren", 360, 6, 40, 0x9A86B0, ChatFormatting.GRAY, false, ParticleTypes.WHITE_ASH);
        def("ringeling", 320, 5, 200, 0xFFB8E0, ChatFormatting.LIGHT_PURPLE, false, ParticleTypes.HEART);
        def("f4", 380, 6, 300, 0x8A8A9A, ChatFormatting.GRAY, true, ParticleTypes.WHITE_ASH);
        DEFS.put("kerker", new Def("kerker", 200, new String[] {"kerker.binnen", "kompas.fallback"}, new int[] {15, 90}, 150, 0xFFE070,
                ChatFormatting.GOLD, false, ParticleTypes.END_ROD));
        for (int n = 0; n <= 5; n++) {
            DEFS.put("visioen" + n, new Def("visioen" + n, 200, new String[] {"visioen." + n + ".0", "visioen." + n + ".1", "visioen." + n + ".2"},
                    new int[] {10, 70, 130}, 20, 0x80D8FF, ChatFormatting.AQUA, false, ParticleTypes.SPLASH));
        }
    }

    private record Loopt(String soort, long begin, Vec3 bron) {
    }

    private static final Map<UUID, Loopt> LOOPT = new HashMap<>();

    private Z6Verhaal() {
    }

    // ---- the gate ---------------------------------------------------------------------------------------------------------------

    /** Does this player know about the Zijdeland (the swim was seen, or zone 6 already started)? */
    public static boolean magZien(ServerPlayer p) {
        return Questlog.vlag(p, VLAG_LIJN_E) || Questlog.status(p, Z6Haken.HQ01) != 0;
    }

    public static boolean magNaarZijdeland(ServerPlayer p) {
        return magZien(p) && Spelers.level(p) >= MIN_LEVEL;
    }

    /** Null when line E is open, else the lang key of why not (QuestMotor.reisSlot "zijdeland", Z6Lijn's SLOT). */
    public static String slot(ServerPlayer p) {
        return magNaarZijdeland(p) ? null : "vadsrijk.npc.reis.slot.zijdeland";
    }

    public static boolean aankomstGezien(ServerPlayer p) {
        return Spelers.data(p).gezien.contains("npc:z6_aankomst");
    }

    // ---- the great swim (after zone 5) -------------------------------------------------------------------------------------------

    public static void wolkwaltocht(ServerPlayer p) {
        if (bezig(p)) {
            return;
        }
        speel(p, "wolkwaltocht", null);
        geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, 0.4f);
    }

    /** Line E opens for this player and z6_hq01 starts (the end of the swim; also the dev command). */
    static void lijnOpen(ServerPlayer p) {
        Questlog.zetVlag(p, VLAG_LIJN_E, true);
        Spelers.data(p).gezien.add("npc:z6_wolkwaltocht");
        Z6Haken.start(p);
        NpcApi.meld(p, "z6", "wolkwaltocht");
    }

    // ---- travel ------------------------------------------------------------------------------------------------------------------

    /** To Rozenkloof (the travel map / a havenmeester; the gate is checked before). The first time: the arrival film. */
    public static void vlieg(ServerPlayer p) {
        Z6Haken.start(p);
        if (!aankomstGezien(p)) {
            aankomst(p);
            return;
        }
        zet(p, Z6VerhaalPlekken.AANKOMST, 180f);
        Vec3[] k = Z6VerhaalPlekken.CAMERA.get("aankomst");
        RijkScenes.speel(p, RijkScene.maak("npc:z6_vlucht", 120).camera(0, k[2].add(0, 20, 30), k[3]).camera(120, k[2], k[3])
                .titel(Z + "scene.aankomst.titel", Z + "scene.aankomst.onder", 20, 70, 0xFFC8A0).bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.5f, 0.8f);
        NpcApi.meld(p, "z6", "aangekomen");
    }

    /** The first arrival in Rozenkloof (once). */
    public static void aankomst(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z6_aankomst");
        zet(p, Z6VerhaalPlekken.AANKOMST, 180f);
        speel(p, "aankomst", null);
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.7f, 0.6f);
    }

    // ---- the visions of Moeder Bron ------------------------------------------------------------------------------------------------

    /** Moeder Bron's vision after the n-th beacon (0 = the tutorial at the telescope), at this spot. */
    public static void visioen(ServerPlayer p, int n, BlockPos waar) {
        int k = Math.max(0, Math.min(5, n));
        if (bezig(p)) {
            vergeet(p);
        }
        speel(p, "visioen" + k, waar);
        geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8f + 0.2f * k, 0.6f + 0.1f * k);
    }

    // ---- ringeling + F4 (after the raid) ------------------------------------------------------------------------------------------

    /**
     * Ringeling is cheered up (the raid's end, u-z6raid: {@code Z6VerhaalApi.ringelingOpgevrolijkt}). The flag is set at once; the
     * scene "ringeling" plays now when nothing else runs (else at the next quiet moment: Z6Haken's ticker), then flash F4.
     */
    static void ringeling(ServerPlayer p) {
        Questlog.zetVlag(p, VLAG_RINGELING, true);
        if (!bezig(p) && !RijkScenes.bezig(p) && Vadsrijk.in(p)) {
            ringelingScene(p);
        }
    }

    /** u-z6raid's entry point (called by reflection on Ringeling's defeat): the same as {@link Z6VerhaalApi#ringelingOpgevrolijkt}. */
    public static void ringelingOpgevrolijkt(ServerPlayer p) {
        Z6VerhaalApi.ringelingOpgevrolijkt(p);
    }

    static void ringelingScene(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z6_ringeling");
        speel(p, "ringeling", null);
    }

    /** Flash F4 (WERELDVERHAAL 7.2, adapted to decision Z6-6): the Keizer loses the Ringvinger; the Oog turns to Hoogvads. */
    public static void f4(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z6_f4");
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DEFS.get("f4").ticks(), 0, false, false));
        speel(p, "f4", null);
    }

    // ---- the end of the story -----------------------------------------------------------------------------------------------------

    /** z6_hq10 handed in: the flag (once), the title card, the knight title, the listeners. The raid is open from now on. */
    static void zone6Klaar(ServerPlayer p) {
        if (Questlog.vlag(p, VLAG_ZONE6)) {
            return;
        }
        Questlog.zetVlag(p, VLAG_ZONE6, true);
        RijkScenes.titel(p, Component.translatable(Z + "zone6.klaar.titel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable(Z + "zone6.klaar.onder"), 80, 0xFFC8A0);
        zeg(p, "zone6.klaar", ChatFormatting.GOLD);
        Cosmetica.geefTitel(p, "vr_ridder_van_het_zijdeland");
        huisdier(p, "bobbeltje");   // (the baby bultguh follows you home: u-z6wezens' pet)
        p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1.2f);
        Z6VerhaalApi.ZONE6_KLAAR.forEach(h -> h.accept(p));
    }

    // ---- rewards that live in other slices ---------------------------------------------------------------------------------------

    /** hq02 at Stalmeester Hobbel: the bultguh (Rijdieren "bultguh", u-z6wezens' species). Waits when the species is not in the tree. */
    static void bultguh(ServerPlayer p) {
        if (reflectie(p, "naturel", "zone6.bultguh.Bultguh#geef")) {   // (u-z6wezens: the coat "naturel", owned + worn)
            Questlog.zetVlag(p, VLAG_BULTGUH, false);
        } else if (RijdierSoort.van("bultguh") != null) {
            Rijdieren.geef(p, "bultguh");
            Questlog.zetVlag(p, VLAG_BULTGUH, false);
        } else {
            Questlog.zetVlag(p, VLAG_BULTGUH, true);
            zeg(p, "beloning.wacht.bultguh", ChatFormatting.GRAY);
        }
        NpcApi.meld(p, "z6", "bultguh");
    }

    /** hq06 at Knoopje: the flying carpet (u-tapijt's {@code tapijt.Tapijt.geef}, by reflection). Waits when it is not in the tree. */
    static void tapijt(ServerPlayer p) {
        if (reflectie(p, "", "tapijt.Tapijt#geef") || Rijdieren.geef(p, "tapijt") || RijdierSoort.van("tapijt") != null && Rijdieren.heeft(p, RijdierSoort.van("tapijt"))) {
            Questlog.zetVlag(p, VLAG_TAPIJT, false);
            return;
        }
        Questlog.zetVlag(p, VLAG_TAPIJT, true);
        zeg(p, "beloning.wacht.tapijt", ChatFormatting.GRAY);
    }

    /** A carpet look with {@code Bron.api(id)} (u-tapijt: "z6_kwastjes", "z6_eerste_karavaan"). Waits (a flag) when no such look exists. */
    static void uiterlijk(ServerPlayer p, String id) {
        boolean gegeven = false;
        for (Uiterlijk.Variant v : Uiterlijk.alle()) {
            if (v.bron().soort().equals("api") && v.bron().sleutel().equals(id)) {
                Aankleden.geef(p, v, true);
                gegeven = true;
            }
        }
        Questlog.zetVlag(p, "z6_uiterlijk_" + id, !gegeven);
        if (!gegeven) {
            zeg(p, "beloning.wacht.uiterlijk", ChatFormatting.GRAY);
        }
    }

    /** A quest pet of u-z6wezens (springpiepje, dadelaapje, bobbeltje): waits with a flag while the pet is not in the tree. */
    static void huisdier(ServerPlayer p, String id) {
        boolean ok = nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren.geef(p, id);
        Questlog.zetVlag(p, "z6_huisdier_" + id, !ok && !nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren.heeft(p, id));
    }

    /** Every 5 s: rewards that waited for their slice. */
    static void beloningen(ServerPlayer p) {
        if (Questlog.vlag(p, VLAG_BULTGUH) && (bestaat("zone6.bultguh.Bultguh") || RijdierSoort.van("bultguh") != null)) {
            if (!reflectie(p, "naturel", "zone6.bultguh.Bultguh#geef")) {
                Rijdieren.geef(p, "bultguh");
            }
            Questlog.zetVlag(p, VLAG_BULTGUH, false);
        }
        for (String dier : List.of("springpiepje", "dadelaapje", "bobbeltje")) {
            if (Questlog.vlag(p, "z6_huisdier_" + dier)) {
                huisdier(p, dier);
            }
        }
        if (Questlog.vlag(p, VLAG_TAPIJT) && RijdierSoort.van("tapijt") != null) {
            tapijt(p);
        }
        for (String id : List.of("z6_kwastjes", "z6_eerste_karavaan")) {
            if (Questlog.vlag(p, "z6_uiterlijk_" + id)) {
                for (Uiterlijk.Variant v : Uiterlijk.alle()) {
                    if (v.bron().soort().equals("api") && v.bron().sleutel().equals(id)) {
                        uiterlijk(p, id);
                        break;
                    }
                }
            }
        }
    }

    // ---- the Grauwe brief (hq07) ---------------------------------------------------------------------------------------------------

    static void brief(ServerPlayer p) {
        if (!Z6Haken.eenmalig(p, "grauwe_brief")) {
            return;
        }
        GuhAdvancements.grant(p, "vr_z6_grauwe_brief");
        Questlog.map(p, "z6_brieven").putBoolean("ringvinger", true);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.7f);
        Praat.scene(p, SLEUTEL, List.of(new Praat.Regel(null, "vadsrijk.npc.verteller", Z + "brief.0"),
                new Praat.Regel(null, Z + "brief.naam", Z + "brief.1"), new Praat.Regel(null, Z + "brief.naam", Z + "brief.2")));
    }

    // ---- the scene runner ---------------------------------------------------------------------------------------------------------

    /** Plays scene {@code soort} (at {@code waar} for a vision; else its CAMERA keyframes). */
    static void speel(ServerPlayer p, String soort, BlockPos waar) {
        Def d = DEFS.get(soort);
        if (d == null) {
            return;
        }
        Vec3[] k = soort.startsWith("visioen") ? Z6VerhaalPlekken.visioen(waar == null ? p.blockPosition() : waar)
                : Z6VerhaalPlekken.CAMERA.getOrDefault(soort, Z6VerhaalPlekken.visioen(p.blockPosition()));
        String titel = soort.startsWith("visioen") ? "visioen." + soort.substring(7) : "scene." + soort;
        RijkScenes.speel(p, RijkScene.maak("npc:z6_" + soort, d.ticks()).camera(0, k[0], k[1]).camera(d.ticks(), k[2], k[3])
                .titel(Z + titel + ".titel", Z + titel + ".onder", d.titelOp(), 80, d.titelKleur()).bouw());
        if (d.donker()) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, d.ticks(), 0, false, false));
        }
        LOOPT.put(p.getUUID(), new Loopt(soort, p.level().getGameTime(), k[1]));
    }

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
            Def d = DEFS.get(l.soort());
            int t = (int) (nu - l.begin());
            for (int i = 0; i < d.regels().length; i++) {
                if (t == d.op()[i]) {
                    zeg(p, d.regels()[i], d.kleur());
                }
            }
            if (t % 4 == 0) {
                int sterk = l.soort().startsWith("visioen") ? 4 + 6 * (l.soort().charAt(7) - '0') : 16;
                deeltjes(p, sl, d.deeltje(), l.bron().add(0, 2, 0), sterk, 4, 2, 0.02);
                if (l.soort().startsWith("visioen") || l.soort().equals("bron")) {
                    deeltjes(p, sl, new DustParticleOptions(0x80D8FF, 1.6f), l.bron().add(0, 1, 0), sterk, 2, 2, 0);
                }
            }
            if (l.soort().equals("wolkwaltocht") && t % 30 == 0) {
                geluid(p, SoundEvents.ELYTRA_FLYING, 0.4f, 0.5f);
            }
            if (t >= d.ticks()) {
                it.remove();
                daarna.add(() -> einde(p, l.soort()));
            }
        }
        daarna.forEach(Runnable::run);
    }

    /** What happens when a scene ends (also called at once by {@link #spoel} in tests). */
    static void einde(ServerPlayer p, String soort) {
        switch (soort) {
            case "wolkwaltocht" -> lijnOpen(p);
            case "aankomst" -> NpcApi.meld(p, "z6", "aangekomen");
            case "zoutkolos" -> {
                NpcApi.meld(p, "z6", "zoutkolos");
                reflectie(p, "zoutkolos", "zone6.wezens.baas.Z6Bazen#start");   // (u-z6wezens: he really wakes; dodging is fine)
            }
            case "kerker" -> kompas(p);
            case "ontmasker" -> NpcApi.meld(p, "z6", "ontmaskerd");
            case "ontsnapt" -> NpcApi.meld(p, "z6", "ontsnapt");
            case "bron" -> NpcApi.meld(p, "z6_bron", "opgevrolijkt");
            case "toren" -> {
                NpcApi.meld(p, "z6", "toren");
                zeg(p, "toren.dicht", ChatFormatting.GRAY);
            }
            case "ringeling" -> {
                if (Z6Haken.eenmalig(p, "ring")) {
                    Questlog.geef(p, new ItemStack(Z6VerhaalRegistry.item("ring")));
                    Cosmetica.geefTitel(p, "vr_vriend_van_ringeling");
                    GuhAdvancements.grant(p, "vr_z6_ringeling");
                }
                f4(p);
            }
            case "f4" -> {
                zeg(p, "epiloog.0", ChatFormatting.LIGHT_PURPLE);
                zeg(p, "epiloog.1", ChatFormatting.LIGHT_PURPLE);
            }
            default -> {
            }
        }
    }

    /** hq08: the Sterrenkompas (the dungeon's end, or the fallback scene). */
    static void kompas(ServerPlayer p) {
        if (Z6Haken.eenmalig(p, "kompas")) {
            Questlog.geef(p, new ItemStack(Z6VerhaalRegistry.item("sterrenkompas")));
        }
        NpcApi.meld(p, "z6_sterrenwacht", "kompas");   // (u-z6wezens melds the same at the dungeon's end)
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------------------

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
        Holder<SoundEvent> h = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s);
        p.connection.send(new ClientboundSoundPacket(h, SoundSource.AMBIENT, p.getX(), p.getY(), p.getZ(), volume, toon, p.getRandom().nextLong()));
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
                    if (t.length == 1 && t[0].isAssignableFrom(ServerPlayer.class)) {
                        m.invoke(null, p);
                    } else if (t.length == 2 && t[0].isAssignableFrom(ServerPlayer.class) && t[1] == String.class) {
                        m.invoke(null, p, arg);
                    } else {
                        continue;
                    }
                    return true;
                } catch (ReflectiveOperationException | RuntimeException e) {
                    LOG.warn("Vadsrijk z6 verhaal: adapter {} failed", naam, e);
                }
            }
        }
        return false;
    }

    /** Does another slice's class exist (below feature/vadsrijk)? */
    static boolean bestaat(String klasse) {
        try {
            Class.forName(V + klasse);
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
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

    /** (tests) Run a running scene to its end at once (the end may start the next one: that one too). */
    static void spoel(ServerPlayer p) {
        for (int i = 0; i < 4; i++) {
            Loopt l = LOOPT.remove(p.getUUID());
            if (l == null) {
                return;
            }
            einde(p, l.soort());
        }
    }

    /** (dev) start hq01 by hand when it is available. */
    static void startHq01(ServerPlayer p) {
        Quest q = Quests.van(Z6Haken.HQ01);
        if (q != null && Questlog.beschikbaar(p, q)) {
            Questlog.neemAan(p, q);
        }
    }
}
