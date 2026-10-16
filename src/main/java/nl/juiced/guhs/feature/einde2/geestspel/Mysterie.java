package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestWezen;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice geest_spel: the mysteries of the Geestenwereld (DESIGN 5 "Mysterie", "Extra mysterie en episch"):
 * <ul>
 *   <li><b>footprints</b> ({@code voetstappen_1..3} pads to {@code geheim_1..3}): glowing guh paw prints of something
 *       invisible walk ahead of you, step by step as you follow, to a little secret;</li>
 *   <li><b>the Geestenklok</b> ({@code geestenklok}): ring it, everything goes quiet, every spirit looks at you
 *       ({@link GeestScenes#KLOK} the first time), and then something small and strange happens;</li>
 *   <li><b>the misty swamp</b> ({@code moeras}, {@code moeras_visioen}): for a moment a guh you know stands between the
 *       roots, and then there is only mist;</li>
 *   <li><b>Wan-guh</b> ({@value GeestLijnen#WAN}): a hooded guh stands far away in a zone ({@code wan_1..5}) and is gone when
 *       you come near; three glimpses, then (after the whole story, or all tiles and chakras) a path appears in the mist
 *       ({@link GeestLagen#PAD_8}) to the eighth island, where {@link GeestScenes#WAN} tells where it all began;</li>
 *   <li><b>the kennisvosjes</b>: a click and the fox sniffs and shows you the way to something you have not found yet;</li>
 *   <li><b>the blooming forest</b>: after the acorn and the lanterns, more little spirits come along for you.</li>
 * </ul>
 */
public final class Mysterie {
    /** Saved: the secrets found (bit n-1 = geheim_n). */
    static final String GEHEIMEN = GeestSpel.DATA + "geheimen";
    /** Saved: the swamp vision was seen. */
    static final String MOERAS = GeestSpel.DATA + "moeras";

    /** player -> the footprint trail they follow: which pad, how far. */
    private record Spoor(int pad, int stap) {
    }

    private static final Map<UUID, Spoor> SPOOR = new ConcurrentHashMap<>();
    /** player -> game time until which the bell, the swamp and Wan rest. */
    private static final Map<UUID, Long> KLOK_RUST = new ConcurrentHashMap<>(), MOERAS_RUST = new ConcurrentHashMap<>(),
            WAN_RUST = new ConcurrentHashMap<>();
    /** player -> how long they looked at Wan-guh now (ticks), and until when the glimpse lasts. */
    private static final Map<UUID, Integer> WAN_GEKEKEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> WAN_TOT = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> VISIOEN_TOT = new ConcurrentHashMap<>();

    private Mysterie() {
    }

    private static Verhaallijn wan() {
        return GeestLijnen.wan();
    }

    static void tick(ServerPlayer p) {
        voetstappen(p);
        moeras(p);
        wanTick(p);
        eiland(p);
        bloei(p);
    }

    // =====================================================================================================================
    // footprints
    // =====================================================================================================================

    static boolean geheim(ServerPlayer p, int n) {
        return (GuhQuests.saved(p).getIntOr(GEHEIMEN, 0) & (1 << (n - 1))) != 0;
    }

    private static void voetstappen(ServerPlayer p) {
        Spoor s = SPOOR.get(p.getUUID());
        if (s == null) {
            for (int n = 1; n <= 3; n++) {
                List<Vec3> pad = GeestPlek.pad("voetstappen_" + n);
                if (!pad.isEmpty() && !geheim(p, n) && p.position().closerThan(pad.get(0), 4)) {
                    SPOOR.put(p.getUUID(), new Spoor(n, 0));
                    GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.voetstappen.begin");
                    GeestSpel.geluid(p, "geest.lantaarn_tinkel", 0.4f, 1.6f);
                    return;
                }
            }
            return;
        }
        List<Vec3> pad = GeestPlek.pad("voetstappen_" + s.pad());
        if (pad.isEmpty()) {
            SPOOR.remove(p.getUUID());
            return;
        }
        int stap = s.stap();
        // the player caught up: the prints walk on
        while (stap < pad.size() - 1 && p.position().closerThan(pad.get(stap), 3.0)) {
            stap++;
        }
        if (stap != s.stap()) {
            SPOOR.put(p.getUUID(), new Spoor(s.pad(), stap));
        }
        if (p.position().distanceToSqr(pad.get(stap)) > 40 * 40) {
            SPOOR.remove(p.getUUID());   // (lost the trail: it waits at its start)
            return;
        }
        // the next three prints ahead, left and right in turn, glowing
        for (int i = stap; i < Math.min(pad.size(), stap + 3); i++) {
            Vec3 a = pad.get(i), b = i + 1 < pad.size() ? pad.get(i + 1) : a;
            Vec3 d = b.subtract(a);
            Vec3 zij = d.lengthSqr() < 0.01 ? Vec3.ZERO : new Vec3(-d.z, 0, d.x).normalize().scale(0.22 * (i % 2 == 0 ? 1 : -1));
            Vec3 q = a.add(zij).add(0, 0.08, 0);
            p.level().sendParticles(p, ParticleTypes.GLOW, false, false, q.x, q.y, q.z, 2, 0.05, 0.0, 0.05, 0.0);
            p.level().sendParticles(p, ParticleTypes.END_ROD, false, false, q.x, q.y, q.z, 1, 0.03, 0.0, 0.03, 0.0);
        }
        if (stap >= pad.size() - 1) {
            Vec3 g = GeestPlek.midden("geheim_" + s.pad());
            if (g != null) {
                p.level().sendParticles(p, ParticleTypes.END_ROD, false, false, g.x, g.y + 1, g.z, 6, 0.3, 0.4, 0.3, 0.02);
            }
            if (g == null || p.position().closerThan(g, 2.5)) {
                vind(p, s.pad());
            }
        }
    }

    /** Secret n is found: something small, once per player. */
    static void vind(ServerPlayer p, int n) {
        SPOOR.remove(p.getUUID());
        if (geheim(p, n)) {
            return;
        }
        int alle = GuhQuests.saved(p).getIntOr(GEHEIMEN, 0) | (1 << (n - 1));
        GuhQuests.saved(p).putInt(GEHEIMEN, alle);
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.geheim." + n);
        GeestSpel.geluid(p, "stinger.opgelost", 0.7f, 1.4f);
        switch (n) {
            case 1 -> GeestSpel.geef(p, nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get(), 8);
            case 2 -> wanGlimp(p, true);
            default -> GeestSpel.geef(p, nl.juiced.guhs.registry.ModItems.GEFRITUURDE_KAASKNABBELS.get(), 3);
        }
        if (alle == 7) {
            GuhAdvancements.grant(p, "einde2_geest_spel_voetstappen");
        }
    }

    // =====================================================================================================================
    // the bell
    // =====================================================================================================================

    static boolean klik(ServerPlayer p, String prop) {
        if ("geestenklok".equals(prop)) {
            luid(p);
            return true;
        }
        if (prop.startsWith("geheim_")) {
            try {
                int n = Integer.parseInt(prop.substring("geheim_".length()));
                if (!geheim(p, n)) {
                    GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.geheim.nog");
                } else {
                    GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.geheim." + n);
                }
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if ("moeras_visioen".equals(prop) || "wan_onthulling".equals(prop)) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel." + prop);
            return true;
        }
        return false;
    }

    static void luid(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long rust = KLOK_RUST.get(p.getUUID());
        if (rust != null && nu < rust) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.klok.rust");
            return;
        }
        KLOK_RUST.put(p.getUUID(), nu + 20 * 30);
        boolean eerste = !GuhQuests.saved(p).getBooleanOr(GeestSpel.DATA + "klok", false);
        GuhQuests.saved(p).putBoolean(GeestSpel.DATA + "klok", true);
        GuhAdvancements.grant(p, "einde2_geest_spel_klok");
        if (eerste) {
            GeestScenes.speel(p, GeestScenes.KLOK, Mysterie::vreemd);
            return;
        }
        Vec3 k = GeestPlek.midden("geestenklok");
        if (k != null) {
            GeestSpel.geluid(p, k, nl.juiced.guhs.feature.einde2.audio.E2Audio.geluid("geest.klok").get(), 1.4f, 0.7f);
        }
        kijkAllemaal(p);
        p.level().getServer().execute(() -> vreemd(p));
    }

    /** Every spirit near the bell turns its head to you (a few seconds; nobody minds). */
    private static void kijkAllemaal(ServerPlayer p) {
        for (GeestWezen w : p.level().getEntitiesOfClass(GeestWezen.class, new AABB(p.blockPosition()).inflate(48))) {
            w.getLookControl().setLookAt(p, 90f, 90f);
        }
    }

    /** "Something small and strange happens." */
    static void vreemd(ServerPlayer p) {
        int keuze = p.getRandom().nextInt(6);
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.klok.vreemd." + keuze);
        switch (keuze) {
            case 0 -> {
                for (GeestWezen w : p.level().getEntitiesOfClass(GeestWezen.class, new AABB(p.blockPosition()).inflate(64))) {
                    if (w.wie().contains("lantaarn")) {
                        w.zetAnimatie("knipoog");
                    }
                }
            }
            case 1 -> GeestSpel.geluid(p, "geest.koi_zang", 1.0f, 0.8f);
            case 2 -> {
                p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0, false, false));
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 120, 0, false, false));
            }
            case 3 -> p.level().sendParticles(p, ParticleTypes.CHERRY_LEAVES, false, false, p.getX(), p.getY() + 0.5, p.getZ(), 80, 4, 0.5, 4, 0.0);
            case 4 -> wanGlimp(p, true);
            default -> p.level().sendParticles(p, ParticleTypes.SQUID_INK, false, false, p.getX(), p.getY() + 0.1, p.getZ(), 6, 0.6, 0.0, 0.6, 0.0);
        }
    }

    // =====================================================================================================================
    // the swamp
    // =====================================================================================================================

    private static void moeras(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long tot = VISIOEN_TOT.get(p.getUUID());
        if (tot != null) {
            Entity e = GeestActeurs.van(p, "visioen");
            if (e == null || nu >= tot || p.position().closerThan(e.position(), 6)) {
                VISIOEN_TOT.remove(p.getUUID());
                GeestActeurs.weg(p, "visioen", true);
            }
            return;
        }
        if (!GeestPlek.in(p, "moeras")) {
            return;
        }
        Long rust = MOERAS_RUST.get(p.getUUID());
        if (rust != null && nu < rust) {
            return;
        }
        Vec3 plek = GeestPlek.midden("moeras_visioen");
        if (plek == null) {
            return;
        }
        MOERAS_RUST.put(p.getUUID(), nu + 20 * 240);
        GuhNpcEntity.Kind wie = bekende(p);
        float yaw = (float) (Math.toDegrees(Math.atan2(p.getZ() - plek.z, p.getX() - plek.x)) - 90.0);
        if (GeestActeurs.zorg(p, "visioen", "guhs:guh_npc", plek, yaw, t -> t.putString("Kind", wie.id())) != null) {
            VISIOEN_TOT.put(p.getUUID(), nu + 70);
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.moeras.visioen");
            GeestSpel.geluid(p, "geest.maannacht", 0.4f, 0.6f);
            GuhQuests.saved(p).putBoolean(MOERAS, true);
            GuhAdvancements.grant(p, "einde2_geest_spel_moeras");
        }
    }

    /** Somebody this player knows from the story (or Aang-guh, whom everybody knows). */
    static GuhNpcEntity.Kind bekende(ServerPlayer p) {
        List<GuhNpcEntity.Kind> wie = new ArrayList<>(List.of(GuhNpcEntity.Kind.AANG_GUH));
        if (E2Verhaal.klaar(p, E2Verhaal.WATER)) {
            wie.add(GuhNpcEntity.Kind.YUE_GUH);
            wie.add(GuhNpcEntity.Kind.KATARA_GUH);
        }
        if (E2Verhaal.klaar(p, E2Verhaal.AARDE)) {
            wie.add(GuhNpcEntity.Kind.OOM_MIROH);
            wie.add(GuhNpcEntity.Kind.TOPH_GUH);
        }
        if (E2Verhaal.vlag(p, E2Verhaal.VUUR, E2Verhaal.V_ZUKA_TEAM)) {
            wie.add(GuhNpcEntity.Kind.ZUKA_TEAM);
        }
        return wie.get(p.getRandom().nextInt(wie.size()));
    }

    // =====================================================================================================================
    // Wan-guh, the path, the eighth island
    // =====================================================================================================================

    private static void wanTick(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long tot = WAN_TOT.get(p.getUUID());
        Entity e = GeestActeurs.van(p, "wan");
        if (tot != null && e != null) {
            if (nu >= tot || p.position().closerThan(e.position(), 22)) {
                verdwijn(p, e);
                return;
            }
            if (GeestSpel.kijktNaar(p, e.position().add(0, 1, 0), 10, 120)) {
                int g = WAN_GEKEKEN.merge(p.getUUID(), 10, Integer::sum);
                if (g == 20) {
                    glimpGezien(p);
                }
            }
            return;
        }
        if (tot != null) {
            WAN_TOT.remove(p.getUUID());
        }
        if (wan().klaar(p)) {
            return;
        }
        Long rust = WAN_RUST.get(p.getUUID());
        if (rust == null) {
            WAN_RUST.put(p.getUUID(), nu + 20 * 60);
            return;
        }
        if (nu < rust || p.getRandom().nextInt(6) != 0) {
            return;
        }
        wanGlimp(p, false);
    }

    /** Wan-guh shows himself far away to this player (true: even when he rests). */
    static boolean wanGlimp(ServerPlayer p, boolean altijd) {
        long nu = p.level().getGameTime();
        WAN_RUST.put(p.getUUID(), nu + 20 * (90 + p.getRandom().nextInt(90)));
        List<Vec3> ver = new ArrayList<>();
        for (String id : GeestPlek.reeks("wan_")) {
            Vec3 v = GeestPlek.midden(id);
            if (v != null && !p.position().closerThan(v, 30) && p.position().closerThan(v, 110)) {
                ver.add(v);
            }
        }
        if (ver.isEmpty()) {
            return false;
        }
        Vec3 plek = ver.get(p.getRandom().nextInt(ver.size()));
        float yaw = (float) (Math.toDegrees(Math.atan2(p.getZ() - plek.z, p.getX() - plek.x)) - 90.0);
        if (GeestActeurs.zorg(p, "wan", "guhs:wan_guh", plek, yaw, t -> t.putBoolean("Stil", true)) == null) {
            return false;
        }
        WAN_TOT.put(p.getUUID(), nu + 20 * 25);
        WAN_GEKEKEN.remove(p.getUUID());
        if (altijd) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.wan.daar");
        }
        return true;
    }

    private static void verdwijn(ServerPlayer p, Entity e) {
        if (e instanceof GeestWezen w) {
            w.zetAnimatie("verdwijn");
        }
        p.level().sendParticles(p, ParticleTypes.CLOUD, false, false, e.getX(), e.getY() + 1, e.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
        GeestActeurs.weg(p, "wan", true);
        WAN_TOT.remove(p.getUUID());
        WAN_GEKEKEN.remove(p.getUUID());
    }

    private static void glimpGezien(ServerPlayer p) {
        Verhaallijn l = wan();
        l.begin(p);
        if (l.stap(p) != GeestLijnen.W_GLIMP) {
            return;
        }
        int n = l.teller(p, "glimpen") + 1;
        l.teller(p, "glimpen", n);
        if (n >= GeestLijnen.GLIMPEN_N) {
            l.zet(p, GeestLijnen.W_PAD);
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.wan.drie");
        } else {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.wan.glimp." + Math.min(2, n));
        }
    }

    /** The path shows when the player may see it; on the island: the origin. */
    private static void eiland(ServerPlayer p) {
        Verhaallijn l = wan();
        if (l.klaar(p) || !GeestLijnen.pad8(p)) {
            return;
        }
        if (l.stap(p) == GeestLijnen.W_PAD) {
            l.zet(p, GeestLijnen.W_EILAND);
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.wan.pad");
            GeestSpel.geluid(p, "geest.maannacht", 0.8f, 0.7f);
            nl.juiced.guhs.feature.bestaand.Schijn.straks(p);
        }
        boolean op = GeestPlek.in(p, GeestToegang.EILAND_8) || GeestPlek.bij(p, "wan_onthulling", 6);
        if (op) {
            GeestScenes.speel(p, GeestScenes.WAN, speler -> {
                if (!wan().klaar(speler)) {
                    wan().zet(speler, GeestLijnen.W_KLAAR);
                    GeestSpel.geef(speler, E2GeestSpelFeature.EERSTE_KNABBEL.get(), 1);
                    GuhAdvancements.grant(speler, "einde2_geest_spel_wan");
                    GeestSpel.verteller(speler, "gui.guhs.einde2_geest_spel.wan.klaar");
                }
            });
        }
    }

    // =====================================================================================================================
    // the blooming forest: more little spirits
    // =====================================================================================================================

    /** Both side quests done: the forest blooms and more spirits come along (once a message). */
    static void bloeiCheck(ServerPlayer p) {
        if (GeestLijnen.heiBai().klaar(p) && GeestLijnen.lantaarns().klaar(p) && !GuhQuests.saved(p).getBooleanOr(GeestSpel.DATA + "bloei", false)) {
            GuhQuests.saved(p).putBoolean(GeestSpel.DATA + "bloei", true);
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.bloei");
            GuhAdvancements.grant(p, "einde2_geest_spel_bloei");
        }
    }

    private static void bloei(ServerPlayer p) {
        if (!GuhQuests.saved(p).getBooleanOr(GeestSpel.DATA + "bloei", false)) {
            return;
        }
        // three extra libelkonijnguhs flit round the Lantaarnboom and the wood, for this player
        String[] waar = {"lantaarnboom", "plek_gefluisterwoud", "eikel_plek"};
        for (int i = 0; i < waar.length; i++) {
            Vec3 v = GeestPlek.midden(waar[i]);
            String naam = "bloei_" + i;
            boolean wil = v != null && p.position().closerThan(v, 40);
            if (wil) {
                Vec3 at = v.add(2 + i, 1.5, -1 - i);
                GeestActeurs.zorg(p, naam, "guhs:libelkonijnguh", at, 0f, t -> t.putInt("Honk", 6));
            } else if (GeestActeurs.heeft(p, naam)) {
                GeestActeurs.weg(p, naam, false);
            }
        }
    }

    // =====================================================================================================================
    // clicks on creatures
    // =====================================================================================================================

    /** A GeestWezen: our Wan-guh glimpse (if he is one), or nothing. */
    static boolean klikWezen(ServerPlayer p, GeestWezen w) {
        if ("wan".equals(GeestActeurs.naam(w)) && p.getUUID().equals(GeestActeurs.eigenaar(w))) {
            verdwijn(p, w);
            return true;
        }
        return false;
    }

    /** Another entity: a kennisvosje (resident), Wan-guh (a placeholder type). */
    static boolean klikAnder(ServerPlayer p, Entity e) {
        if ("wan".equals(GeestActeurs.naam(e)) && p.getUUID().equals(GeestActeurs.eigenaar(e))) {
            verdwijn(p, e);
            return true;
        }
        if (GeestPlek.markerVan(e).startsWith("kennisvos_")) {
            vosje(p, e);
            return true;
        }
        return false;
    }

    /** The fox sniffs and shows the way to something this player has not found yet. */
    static void vosje(ServerPlayer p, Entity vos) {
        Vec3 doel = null;
        String lantaarn = Lantaarns.volgende(p);
        if (lantaarn != null && !GeestLijnen.lantaarns().klaar(p)) {
            doel = GeestPlek.midden(lantaarn);
        }
        if (doel == null) {
            for (int n = 1; n <= 3 && doel == null; n++) {
                List<Vec3> pad = GeestPlek.pad("voetstappen_" + n);
                if (!geheim(p, n) && !pad.isEmpty()) {
                    doel = pad.get(0);
                }
            }
        }
        if (doel == null) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.vosje.niets");
            return;
        }
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.vosje.snuffel");
        Vec3 van = vos.position().add(0, 0.6, 0);
        Vec3 d = doel.subtract(van);
        double lengte = Math.min(d.length(), 24);
        Vec3 stap = d.normalize();
        for (double t = 0; t < lengte; t += 0.8) {
            Vec3 q = van.add(stap.scale(t));
            p.level().sendParticles(p, ParticleTypes.WAX_ON, false, false, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
    }

    @Nullable
    static Spoor spoor(ServerPlayer p) {
        return SPOOR.get(p.getUUID());
    }

    static void vergeet(ServerPlayer p) {
        SPOOR.remove(p.getUUID());
        WAN_TOT.remove(p.getUUID());
        WAN_GEKEKEN.remove(p.getUUID());
        VISIOEN_TOT.remove(p.getUUID());
    }
}
