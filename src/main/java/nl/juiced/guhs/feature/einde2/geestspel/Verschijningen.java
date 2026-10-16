package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2 phase B, slice geest_spel: what happens now and then (DESIGN 5 "Zeldzame verschijningen", "Extra mysterie"):
 * <ul>
 *   <li>three rare sightings of ONE player ({@value GeestLijnen#VERSCHIJNINGEN}): a white heron gliding far off, a golden
 *       kennisvosje on an island, Koh crawling through the mist. Every half minute a small chance (growing while nothing
 *       comes); you must really LOOK at it before it is gone; each gives a keepsake;</li>
 *   <li>the <b>moon night</b> (the fourth sighting, for everyone in the Geestenwereld): Tui comes down through the mist, the
 *       light turns silver, the Hemelvijver glows; who goes there sees {@link GeestScenes#MAAN} once;</li>
 *   <li>the <b>spirit tide</b> (for everyone): the mist turns deep blue, the lanterns glow brighter, and far away giant
 *       spirit silhouettes drift ({@code geestreus_1..4}); nobody explains them.</li>
 * </ul>
 * The two world moods reach the clients as {@link GeestSpelPayloads.Sfeer} (the fog colour).
 */
public final class Verschijningen {
    /** How long the world moods last, and how often they get a chance (ticks), with the chance per try. */
    public static final int MAAN_DUUR = 20 * 150, TIJ_DUUR = 20 * 100, PROBEER = 20 * 60;
    static final int MAAN_KANS = 40, TIJ_KANS = 12;

    private record Zicht(String soort, long tot, Vec3 van, Vec3 naar, long start) {
    }

    /** player -> the sighting showing now. */
    private static final Map<UUID, Zicht> NU = new ConcurrentHashMap<>();
    /** player -> how many tries brought nothing (the chance grows). */
    private static final Map<UUID, Integer> PECH = new ConcurrentHashMap<>();
    /** player -> how long they have looked at the sighting (ticks). */
    private static final Map<UUID, Integer> GEKEKEN = new ConcurrentHashMap<>();

    /** The world moods of the Geestenwereld: until when (game time; 0 = not now). */
    private static volatile long maanTot, tijTot;
    private static final List<Entity> REUZEN = new ArrayList<>();

    private Verschijningen() {
    }

    private static Verhaallijn lijn() {
        return GeestLijnen.verschijningen();
    }

    public static boolean maannacht(ServerLevel level) {
        return level.getGameTime() < maanTot;
    }

    public static boolean geestentij(ServerLevel level) {
        return level.getGameTime() < tijTot;
    }

    /** The keepsake of a sighting. */
    static Item aandenken(String soort) {
        return switch (soort) {
            case "reiger" -> E2GeestSpelFeature.VEER.get();
            case "vosje" -> E2GeestSpelFeature.GOUDEN_ROL.get();
            case "koh" -> E2GeestSpelFeature.KOH_SCHUB.get();
            default -> E2GeestSpelFeature.MAANDRUPPEL.get();
        };
    }

    /** This player saw this sighting: the flag, the step, the keepsake (once each). */
    static void gezien(ServerPlayer p, String soort) {
        if (lijn().vlag(p, soort)) {
            return;
        }
        lijn().vlag(p, soort, true);
        int n = 0;
        for (String s : GeestLijnen.ZELDZAAM) {
            if (lijn().vlag(p, s)) {
                n++;
            }
        }
        lijn().zet(p, n);
        GeestSpel.geef(p, aandenken(soort), 1);
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.zicht." + soort + ".gezien");
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.zicht.teller", n, GeestLijnen.VERSCHIJNINGEN_N);
        GeestSpel.geluid(p, "geest.maannacht", 0.6f, 1.4f);
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
    }

    // =====================================================================================================================
    // the sightings of one player
    // =====================================================================================================================

    /** Every 10 ticks in the Geestenwereld. */
    static void tick(ServerPlayer p) {
        Zicht z = NU.get(p.getUUID());
        long nu = p.level().getGameTime();
        if (z != null) {
            beweeg(p, z, nu);
            return;
        }
        if (maannacht(p.level()) && !lijn().vlag(p, "maan") && GeestToegang.zoneOpen(p, "hemelvijver")) {
            Vec3 v = GeestPlek.midden(GeestScenes.ANKER_MAAN);
            if (v != null && p.position().closerThan(v, 40)) {
                GeestScenes.speel(p, GeestScenes.MAAN, speler -> gezien(speler, "maan"));
                return;
            }
        }
        if ((p.tickCount + p.getId()) % 600 >= 10) {
            return;
        }
        List<String> open = new ArrayList<>();
        for (String s : List.of("reiger", "vosje", "koh")) {
            if (!lijn().vlag(p, s)) {
                open.add(s);
            }
        }
        if (open.isEmpty()) {
            return;
        }
        int pech = PECH.getOrDefault(p.getUUID(), 0);
        int kans = Math.max(3, 14 - pech);   // 1 in 14 at first, at worst 1 in 3
        if (p.getRandom().nextInt(kans) != 0) {
            PECH.put(p.getUUID(), pech + 1);
            return;
        }
        PECH.remove(p.getUUID());
        verschijn(p, open.get(p.getRandom().nextInt(open.size())));
    }

    /** Lets a sighting appear for this player now (dev: any; normally one they did not see yet). */
    static boolean verschijn(ServerPlayer p, String soort) {
        weg(p);
        long nu = p.level().getGameTime();
        double kijk = Math.toRadians(p.getYRot() + (p.getRandom().nextDouble() - 0.5) * 90);
        Vec3 voor = new Vec3(-Math.sin(kijk), 0, Math.cos(kijk));
        Vec3 opzij = new Vec3(-voor.z, 0, voor.x);
        Vec3 van, naar;
        String type;
        java.util.function.Consumer<net.minecraft.nbt.CompoundTag> nbt;
        switch (soort) {
            case "reiger" -> {
                // high up and far, gliding across your view
                Vec3 m = p.position().add(voor.scale(34)).add(0, 14, 0);
                van = m.add(opzij.scale(-30));
                naar = m.add(opzij.scale(30));
                type = "guhs:mistreiger";
                nbt = t -> {
                    t.putInt("Soort", 1);
                    t.putBoolean("Stil", true);
                    t.putString("Houding", "vlieg");
                };
            }
            case "vosje" -> {
                Vec3 eiland = eiland(p);
                if (eiland == null) {
                    return false;
                }
                van = eiland;
                naar = eiland;
                type = "guhs:kennisvosje";
                nbt = t -> {
                    t.putInt("Soort", 1);
                    t.putBoolean("Gouden", true);
                    t.putBoolean("Stil", true);
                };
            }
            default -> {
                // Koh crawls through the mist under the islands' edge, slowly, past you
                Vec3 m = p.position().add(voor.scale(30)).add(0, -4, 0);
                van = m.add(opzij.scale(-24));
                naar = m.add(opzij.scale(24));
                type = "guhs:koh";
                nbt = t -> {
                    t.putInt("Soort", 0);
                    t.putBoolean("Stil", true);
                    t.putString("Houding", "kruip");
                };
            }
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(naar.z - van.z, naar.x - van.x)) - 90.0);
        Entity e = GeestActeurs.zorg(p, "zicht", type, van, soort.equals("vosje") ? p.getYRot() + 180f : yaw, nbt);
        if (e == null) {
            return false;
        }
        e.setNoGravity(true);
        NU.put(p.getUUID(), new Zicht(soort, nu + (soort.equals("vosje") ? 20 * 25 : 20 * 18), van, naar, nu));
        GEKEKEN.remove(p.getUUID());
        GeestSpel.geluid(p, soort.equals("koh") ? "geest.koh_kraak" : "geest.lantaarn_tinkel", 0.5f, 0.6f);
        return true;
    }

    /** A spot on another island 24..64 blocks away (a resident marker: there is ground there), or null. */
    @Nullable
    private static Vec3 eiland(ServerPlayer p) {
        List<Vec3> kandidaten = new ArrayList<>();
        for (String prefix : List.of("kennisvos_", "libelkonijnguh_", "bezienswaardigheid_", "plek_")) {
            for (String id : GeestPlek.alleMet(prefix)) {
                Vec3 v = GeestPlek.midden(id);
                if (v != null && !p.position().closerThan(v, 24) && p.position().closerThan(v, 64)) {
                    kandidaten.add(v);
                }
            }
        }
        return kandidaten.isEmpty() ? null : kandidaten.get(p.getRandom().nextInt(kandidaten.size()));
    }

    /** The sighting glides on; a long enough look counts; at its end (or when you come close) it is gone. */
    private static void beweeg(ServerPlayer p, Zicht z, long nu) {
        Entity e = GeestActeurs.van(p, "zicht");
        if (e == null || nu >= z.tot()) {
            weg(p);
            return;
        }
        double f = Math.min(1.0, (nu - z.start()) / (double) (z.tot() - z.start()));
        Vec3 waar = z.van().add(z.naar().subtract(z.van()).scale(f));
        if (!z.van().equals(z.naar())) {
            e.setPos(waar);
        }
        if (p.position().closerThan(e.position(), 9)) {
            if (z.soort().equals("vosje") && !lijn().vlag(p, "vosje")) {
                gezien(p, "vosje");
            }
            p.level().sendParticles(p, ParticleTypes.END_ROD, false, false, e.getX(), e.getY() + 0.6, e.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
            weg(p);
            return;
        }
        if (z.soort().equals("vosje")) {
            p.level().sendParticles(p, ParticleTypes.WAX_ON, false, false, e.getX(), e.getY() + 0.5, e.getZ(), 3, 0.3, 0.3, 0.3, 0.01);
        }
        if (GeestSpel.kijktNaar(p, e.position().add(0, e.getBbHeight() / 2, 0), 14, 96)) {
            int g = GEKEKEN.merge(p.getUUID(), 10, Integer::sum);
            if (g >= 20 && !lijn().vlag(p, z.soort())) {
                gezien(p, z.soort());
            }
        }
    }

    /** A click on the sighting (only the fox can be reached): seen. */
    static boolean klik(ServerPlayer p, Entity e) {
        if (!"zicht".equals(GeestActeurs.naam(e)) || !p.getUUID().equals(GeestActeurs.eigenaar(e))) {
            return false;
        }
        Zicht z = NU.get(p.getUUID());
        if (z != null) {
            gezien(p, z.soort());
        }
        weg(p);
        return true;
    }

    static void weg(ServerPlayer p) {
        NU.remove(p.getUUID());
        GEKEKEN.remove(p.getUUID());
        GeestActeurs.weg(p, "zicht", true);
    }

    static void vergeet(ServerPlayer p) {
        NU.remove(p.getUUID());
        GEKEKEN.remove(p.getUUID());
        PECH.remove(p.getUUID());
    }

    // =====================================================================================================================
    // the world moods (the moon night, the spirit tide)
    // =====================================================================================================================

    static void levelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Geestenwereld.DIM) {
            return;
        }
        long nu = level.getGameTime();
        if (nu == tijTot) {
            eindeTij(level);
        }
        if (nu == maanTot) {
            sfeer(level);
        }
        if (maannacht(level) && nu % 20 == 0) {
            Vec3 v = GeestPlek.midden("vijver") != null ? GeestPlek.midden("vijver") : GeestPlek.midden(GeestScenes.ANKER_MAAN);
            var d = GeestPlek.doos("vijver");
            Vec3 m = d != null ? d.getCenter() : v;
            if (m != null) {
                level.sendParticles(ParticleTypes.END_ROD, m.x, m.y + 1, m.z, 12, 10, 1, 10, 0.01);
                level.sendParticles(ParticleTypes.GLOW, m.x, m.y + 0.5, m.z, 8, 10, 0.5, 10, 0.0);
            }
        }
        if (nu % PROBEER != 41 || level.players().isEmpty()) {
            return;
        }
        if (!maannacht(level) && !geestentij(level) && level.getRandom().nextInt(MAAN_KANS) == 0) {
            begin(level, true);
        } else if (!maannacht(level) && !geestentij(level) && level.getRandom().nextInt(TIJ_KANS) == 0) {
            begin(level, false);
        }
    }

    /** Begins a moon night ({@code maan}) or a spirit tide for everyone in the Geestenwereld. */
    static void begin(ServerLevel level, boolean maan) {
        long nu = level.getGameTime();
        if (maan) {
            maanTot = nu + MAAN_DUUR;
            for (ServerPlayer q : level.players()) {
                GeestSpel.verteller(q, "gui.guhs.einde2_geest_spel.maannacht.begin");
                GeestSpel.geluid(q, "geest.maannacht", 1.0f, 1.0f);
            }
        } else {
            tijTot = nu + TIJ_DUUR;
            for (ServerPlayer q : level.players()) {
                GeestSpel.verteller(q, "gui.guhs.einde2_geest_spel.geestentij.begin");
                GeestSpel.geluid(q, "geest.geestentij", 1.0f, 1.0f);
                GuhAdvancements.grant(q, "einde2_geest_spel_geestentij");
            }
            reuzen(level);
        }
        sfeer(level);
    }

    /** Tells every player in the Geestenwereld the mood now (also one who just came in: {@link #sfeer(ServerPlayer)}). */
    static void sfeer(ServerLevel level) {
        for (ServerPlayer q : level.players()) {
            sfeer(q);
        }
    }

    static void sfeer(ServerPlayer q) {
        ServerLevel level = q.level();
        boolean hier = level.dimension() == Geestenwereld.DIM;
        ModNetworking.sendTo(q, new GeestSpelPayloads.Sfeer(hier && geestentij(level), hier && maannacht(level)));
    }

    /** The giants far away during the tide (shared: they are weather, not anybody's story). */
    private static void reuzen(ServerLevel level) {
        eindeTij(level);
        for (int i = 1; i <= 4; i++) {
            Vec3 v = GeestPlek.midden("geestreus_" + i);
            if (v == null) {
                continue;
            }
            net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
            t.putString("id", "guhs:geestreus");
            t.putInt("Soort", i - 1);
            t.putBoolean("Stil", true);
            t.putBoolean("NoAI", true);
            Entity e = net.minecraft.world.entity.EntityType.loadEntityRecursive(t, level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED, x -> x);
            if (e != null) {
                e.snapTo(v.x, v.y, v.z, level.getRandom().nextFloat() * 360f, 0f);
                e.setInvulnerable(true);
                e.addTag(GeestActeurs.TAG);
                if (level.addFreshEntity(e)) {
                    REUZEN.add(e);
                }
            }
        }
    }

    private static void eindeTij(ServerLevel level) {
        for (Entity e : REUZEN) {
            if (!e.isRemoved()) {
                e.discard();
            }
        }
        REUZEN.clear();
        sfeer(level);
    }

    /** (dev) stops both moods. */
    static void stopSfeer(ServerLevel level) {
        maanTot = 0;
        tijTot = 0;
        eindeTij(level);
    }
}
