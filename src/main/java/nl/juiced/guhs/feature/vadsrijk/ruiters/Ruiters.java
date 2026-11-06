package nl.juiced.guhs.feature.vadsrijk.ruiters;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;

/**
 * U-RUITERS: the manager of the riding guhs. Every route ({@link RuiterRoutes}) has N riders whose place is a pure function of the
 * game time ({@link RuiterRoute#stand}); only the riders near a player exist as entities (a {@link RuiterPluim} + its
 * {@link RuiterGuh} passenger), the rest are "virtual" and keep riding.
 * <ul>
 *   <li><b>Budget / LOD:</b> at most {@link #BUDGET} riders exist in the dimension, the ones nearest to a player first (within
 *       {@link #ZICHT} blocks flat and {@link #ZICHT_Y} up/down); the dust/feather particles only within 24 blocks (client).</li>
 *   <li><b>Exactly one per rider (idempotent):</b> one map key {@code route#k} -> one pair; the entity types are noSave (nothing is
 *       ever loaded back from disk); a join guard refuses every rider entity this manager did not just spawn; an entity that leaves
 *       the level without being removed (a hidden "ghost" in an unloading chunk) is discarded; every 5 s a sweep discards every
 *       rider entity that no pair owns. A pair is only spawned when its chunk is really entity-ticking (not in the load window).</li>
 *   <li><b>Never stuck:</b> the rider is moved kinematically every tick along the planned (walkable) polyline; y is smoothed
 *       (half steps and stairs become a ramp); if the entity is ever more than 6 blocks from where it should be (a teleport, a
 *       lag spike) it snaps to its spot; a rider whose next spot is in a chunk that isn't ticking is taken away and comes back
 *       exactly on its route later.</li>
 * </ul>
 */
public final class Ruiters {
    public static final int BUDGET = 14;
    public static final double ZICHT = 80, ZICHT_Y = 48;
    public static final String TAG = "guhs_vr_ruiter";

    static boolean aan = true;
    /** Entities this manager spawned and still owns (the join guard lets only these in). */
    static final Set<UUID> EIGEN = new HashSet<>();
    /** Rider entities that left their level without being removed: discarded on the next tick. */
    static final List<Entity> WEG = new ArrayList<>();
    static final List<Beheer> BEHEERDERS = new CopyOnWriteArrayList<>();
    /** The world's manager (the Vadsrijk dimension): every route + the dev demo routes. */
    static final List<RuiterRoute> DEMOS = new CopyOnWriteArrayList<>();
    static final Map<String, Long> DEMO_TOT = new HashMap<>();
    public static final Beheer WERELD = new Beheer(() -> {
        List<RuiterRoute> l = new ArrayList<>(RuiterRoutes.alle());
        l.addAll(DEMOS);
        return l;
    });
    private static final Map<UUID, Long> KLIK_PAUZE = new HashMap<>();
    private static int demoTeller;

    private Ruiters() {
    }

    static {
        BEHEERDERS.add(WERELD);
    }

    // =====================================================================================================================
    // ticking
    // =====================================================================================================================

    static void tick(MinecraftServer server) {
        ruimWegOp();
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        long t = sl.getGameTime();
        DEMO_TOT.entrySet().removeIf(e -> {
            if (t > e.getValue()) {
                DEMOS.removeIf(r -> r.id.equals(e.getKey()));
                return true;
            }
            return false;
        });
        if (!aan) {
            WERELD.allesWeg();
            return;
        }
        WERELD.tick(sl, kijkers(sl));
    }

    static List<Vec3> kijkers(ServerLevel sl) {
        List<Vec3> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator()) {
                l.add(p.position());
            }
        }
        return l;
    }

    static void ruimWegOp() {
        if (WEG.isEmpty()) {
            return;
        }
        for (Entity e : List.copyOf(WEG)) {
            if (!e.isRemoved()) {
                e.discard();
            }
        }
        WEG.clear();
    }

    /** Server stopped: forget everything. */
    static void wis() {
        for (Beheer b : BEHEERDERS) {
            b.paren.clear();
        }
        EIGEN.clear();
        WEG.clear();
        DEMOS.clear();
        DEMO_TOT.clear();
        KLIK_PAUZE.clear();
    }

    static boolean isRuiter(Entity e) {
        return e instanceof RuiterPluim || e instanceof RuiterGuh;
    }

    /** Join guard: only entities this manager is spawning right now may enter a level. */
    static boolean magErin(Entity e) {
        return !isRuiter(e) || EIGEN.contains(e.getUUID());
    }

    /** An entity left its level: when it isn't removed (hidden in an unloading chunk), it is discarded next tick. */
    static void verlaten(Entity e) {
        if (!isRuiter(e)) {
            return;
        }
        EIGEN.remove(e.getUUID());
        if (!e.isRemoved()) {
            WEG.add(e);
        }
    }

    /** True when some manager owns this entity in a live pair. */
    static boolean hoortBij(Entity e) {
        for (Beheer b : BEHEERDERS) {
            for (Paar p : b.paren.values()) {
                if (p.pluim == e || p.guh == e || p.verzorger == e) {
                    return true;
                }
            }
        }
        return false;
    }

    // =====================================================================================================================
    // one manager (the world has one; a gametest makes its own)
    // =====================================================================================================================

    public static final class Beheer {
        final Supplier<List<RuiterRoute>> routes;
        final Map<String, Paar> paren = new LinkedHashMap<>();
        int budget = BUDGET;

        public Beheer(Supplier<List<RuiterRoute>> routes) {
            this.routes = routes;
        }

        private record Kandidaat(RuiterRoute route, int k, RuiterRoute.Stand stand, double afstand) {
        }

        public void tick(ServerLevel sl, List<Vec3> kijkers) {
            long t = sl.getGameTime();
            List<Kandidaat> kand = new ArrayList<>();
            for (RuiterRoute r : routes.get()) {
                for (int k = 0; k < r.ruiters; k++) {
                    RuiterRoute.Stand s = r.stand(t, k);
                    double d = afstand(s.pos(), kijkers);
                    if (d < ZICHT * ZICHT) {
                        kand.add(new Kandidaat(r, k, s, d));
                    }
                }
            }
            kand.sort(Comparator.comparingDouble(Kandidaat::afstand));
            Set<String> tonen = new HashSet<>();
            for (int i = 0; i < Math.min(budget, kand.size()); i++) {
                Kandidaat c = kand.get(i);
                String key = c.route().id + "#" + c.k();
                if (toon(sl, key, c.route(), c.k(), c.stand())) {
                    tonen.add(key);
                }
            }
            for (Iterator<Map.Entry<String, Paar>> it = paren.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, Paar> e = it.next();
                if (!tonen.contains(e.getKey())) {
                    e.getValue().weg();
                    it.remove();
                }
            }
            if (t % 100 == 0) {
                veeg(sl);
            }
        }

        private static double afstand(Vec3 pos, List<Vec3> kijkers) {
            double beste = Double.MAX_VALUE;
            for (Vec3 k : kijkers) {
                if (Math.abs(k.y - pos.y) > ZICHT_Y) {
                    continue;
                }
                double dx = k.x - pos.x, dz = k.z - pos.z;
                beste = Math.min(beste, dx * dx + dz * dz);
            }
            return beste;
        }

        private boolean toon(ServerLevel sl, String key, RuiterRoute r, int k, RuiterRoute.Stand s) {
            Paar p = paren.get(key);
            if (p != null && !p.heel(sl)) {
                p.weg();
                paren.remove(key);
                p = null;
            }
            BlockPos bp = BlockPos.containing(s.pos());
            if (!sl.isPositionEntityTicking(bp) || !sl.areEntitiesLoaded(ChunkPos.pack(bp))) {
                if (p != null) {
                    p.weg();
                    paren.remove(key);
                }
                return false;
            }
            if (p == null) {
                p = maak(sl, key, r, k, s);
                if (p == null) {
                    return false;
                }
                paren.put(key, p);
            }
            p.zet(sl, s);
            return true;
        }

        /** Every pair away (the switch "uit", or a test). */
        public void allesWeg() {
            for (Paar p : paren.values()) {
                p.weg();
            }
            paren.clear();
        }

        public int aantal() {
            return paren.size();
        }

        @Nullable
        public Paar paar(String key) {
            return paren.get(key);
        }
    }

    /** The sweep: every rider entity in this level that no pair owns is discarded (ghosts, leftovers of a crash...). */
    static int veeg(ServerLevel sl) {
        int n = 0;
        List<Entity> los = new ArrayList<>();
        for (Entity e : sl.getEntities(RuiterRegistry.PLUIM.get(), e -> true)) {
            los.add(e);
        }
        for (Entity e : sl.getEntities(RuiterRegistry.GUH.get(), e -> true)) {
            los.add(e);
        }
        for (Entity e : los) {
            if (!hoortBij(e)) {
                EIGEN.remove(e.getUUID());
                e.discard();
                n++;
            }
        }
        return n;
    }

    // =====================================================================================================================
    // a pair: the Pluimguh + its rider (or, at the stable, a groomer standing next to it)
    // =====================================================================================================================

    public static final class Paar {
        final String key;
        final RuiterRoute route;
        public final RuiterPluim pluim;
        @Nullable
        public final RuiterGuh guh;
        @Nullable
        public final RuiterGuh verzorger;
        double y;
        float yaw;

        Paar(String key, RuiterRoute route, RuiterPluim pluim, @Nullable RuiterGuh guh, @Nullable RuiterGuh verzorger, double y, float yaw) {
            this.key = key;
            this.route = route;
            this.pluim = pluim;
            this.guh = guh;
            this.verzorger = verzorger;
            this.y = y;
            this.yaw = yaw;
        }

        boolean heel(ServerLevel sl) {
            return levend(pluim, sl) && (guh == null || levend(guh, sl)) && (verzorger == null || levend(verzorger, sl));
        }

        private static boolean levend(Entity e, ServerLevel sl) {
            return !e.isRemoved() && e.level() == sl;
        }

        void zet(ServerLevel sl, RuiterRoute.Stand s) {
            Vec3 doel = s.pos();
            if (pluim.position().distanceToSqr(doel.x, y, doel.z) > 36) {
                y = doel.y;   // way off (lag, a teleport): snap onto the route
            } else {
                y += Mth.clamp(doel.y - y, -0.3, 0.3);   // smooth over half steps and stairs
            }
            yaw = Mth.approachDegrees(yaw, s.yaw(), s.beweegt() ? 14f : 6f);
            pluim.setPos(doel.x, y, doel.z);
            pluim.setYRot(yaw);
            pluim.setYHeadRot(yaw);
            pluim.setYBodyRot(yaw);
            pluim.setDeltaMovement(Vec3.ZERO);
            if (guh != null && guh.getVehicle() != pluim) {
                guh.startRiding(pluim, true, false);
            }
            if (verzorger != null && sl.getGameTime() % 40 == 0) {   // being groomed: little hearts and sparkles
                sl.sendParticles(sl.getRandom().nextBoolean() ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER, pluim.getX(),
                        pluim.getY() + 1.8, pluim.getZ(), 2, 0.5, 0.3, 0.5, 0.0);
            }
        }

        void weg() {
            for (Entity e : new Entity[] {guh, verzorger, pluim}) {
                if (e != null) {
                    EIGEN.remove(e.getUUID());
                    if (!e.isRemoved()) {
                        e.discard();
                    }
                }
            }
        }
    }

    @Nullable
    static Paar maak(ServerLevel sl, String key, RuiterRoute r, int k, RuiterRoute.Stand s) {
        RandomSource rnd = RandomSource.create(key.hashCode() * 6151L + 17);   // the same look every time this rider shows up
        RuiterPluim pl = RuiterRegistry.PLUIM.get().create(sl, EntitySpawnReason.EVENT);
        if (pl == null) {
            return null;
        }
        pl.zetKleur(kleur(rnd, r.zone));
        pl.zetUiterlijk(Uiterlijk.maak("", patroon(rnd), tuig(rnd, r.zone)));
        Vec3 p = s.pos();
        float yaw = s.yaw();
        pl.snapTo(p.x, p.y, p.z, yaw, 0);
        pl.addTag(TAG);
        EIGEN.add(pl.getUUID());
        if (!sl.addFreshEntity(pl)) {
            EIGEN.remove(pl.getUUID());
            return null;
        }
        boolean geborsteld = r.modus == RuiterRoute.Modus.STAL && k == 1;
        RuiterGuh guh = maakGuh(sl, key, rnd, r, geborsteld ? "verzorger" : "ruiter");
        if (guh == null) {
            pl.discard();
            EIGEN.remove(pl.getUUID());
            return null;
        }
        RuiterGuh rijder = null, verzorger = null;
        if (geborsteld) {
            Vec3 zij = new Vec3(Math.cos(Math.toRadians(yaw)), 0, Math.sin(Math.toRadians(yaw))).scale(1.8);
            guh.snapTo(p.x + zij.x, p.y, p.z + zij.z, yaw + 90, 0);
            verzorger = guh;
        } else {
            guh.snapTo(p.x, p.y + RuiterPluim.ZADEL, p.z, yaw, 0);
            guh.setOrderedToSit(true);
            guh.setInSittingPose(true);
            rijder = guh;
        }
        EIGEN.add(guh.getUUID());
        if (!sl.addFreshEntity(guh)) {
            EIGEN.remove(guh.getUUID());
            pl.discard();
            EIGEN.remove(pl.getUUID());
            return null;
        }
        if (rijder != null) {
            rijder.startRiding(pl, true, false);
        }
        return new Paar(key, r, pl, rijder, verzorger, p.y, yaw);
    }

    // ---- looks -----------------------------------------------------------------------------------------------------------------

    private static final PluimKleur[] KLEUREN_Z3 = {PluimKleur.ROZE, PluimKleur.ROZE, PluimKleur.MINT, PluimKleur.MINT, PluimKleur.LAVENDEL,
            PluimKleur.LAVENDEL, PluimKleur.HONING, PluimKleur.HONING, PluimKleur.WOLKWIT, PluimKleur.STERRENNACHT, PluimKleur.KRISTAL,
            PluimKleur.LAMPION};
    private static final PluimKleur[] KLEUREN_Z1 = {PluimKleur.ROZE, PluimKleur.MINT, PluimKleur.LAVENDEL, PluimKleur.HONING};

    static PluimKleur kleur(RandomSource rnd, int zone) {
        PluimKleur[] k = zone == 1 ? KLEUREN_Z1 : KLEUREN_Z3;
        return k[rnd.nextInt(k.length)];   // never the golden one (that stays special)
    }

    static String patroon(RandomSource rnd) {
        String[] p = {"", "", "", "vlekjes", "streepjes", "hartjes", "sterretjes"};
        return p[rnd.nextInt(p.length)];
    }

    static String tuig(RandomSource rnd, int zone) {
        if (zone == 1) {
            return "dekje";
        }
        String[] t = {"dekje", "dekje", "bloemenkrans", "belletjes", "lampionnetjes", ""};
        return t[rnd.nextInt(t.length)];
    }

    private static final GuhVariant[] VARIANTEN = {GuhVariant.NORMAL, GuhVariant.NORMAL, GuhVariant.MINT, GuhVariant.CHOCO, GuhVariant.SNOW,
            GuhVariant.PLUISGUH, GuhVariant.BLOESEMGUH, GuhVariant.STARRY};
    private static final GuhClothes[] HOEDJES = {GuhClothes.JOCKEY_PET, GuhClothes.JOCKEY_PET, GuhClothes.STRAW_HAT, GuhClothes.ZONNEHOEDJE,
            GuhClothes.BOSWACHTERSHOED, GuhClothes.BLOESEMKRANSJE, GuhClothes.BALLONPET, GuhClothes.TUINHOEDJE, GuhClothes.PLUKMUTS};
    private static final GuhClothes[] SJAALTJES = {GuhClothes.KNUS_SJAALTJE, GuhClothes.VR_ITEM_VADSRIJKSJAAL, GuhClothes.WINTER_SCARF,
            GuhClothes.BOERDERIJ_ZAKDOEK, GuhClothes.KOORSTRIKJE};
    /** Names of the riders (lang vadsrijk.ruiters.naam.N), shown in their click line. */
    static final int NAMEN = 12;

    @Nullable
    static RuiterGuh maakGuh(ServerLevel sl, String key, RandomSource rnd, RuiterRoute r, String rol) {
        RuiterGuh g = RuiterRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        if (g == null) {
            return null;
        }
        g.sleutel = key;
        g.addTag(TAG);
        if (r.zone == 1) {   // the courier: jockey cap, a backpack full of letters
            g.setVariant(rnd.nextBoolean() ? GuhVariant.NORMAL : GuhVariant.CHOCO);
            g.wear(GuhClothes.JOCKEY_PET);
            g.wear(GuhClothes.GUH_BACKPACK);
            g.wear(GuhClothes.VR_ITEM_VADSRIJKSJAAL);
        } else if (rol.equals("verzorger")) {   // the groomer at the Pluimstal
            g.setVariant(GuhVariant.CHOCO);
            g.wear(GuhClothes.BOERDERIJ_HOEDJE);
            g.wear(GuhClothes.TUINSCHORTJE);
        } else {
            g.setVariant(VARIANTEN[rnd.nextInt(VARIANTEN.length)]);
            g.wear(HOEDJES[rnd.nextInt(HOEDJES.length)]);
            if (rnd.nextInt(3) > 0) {
                g.wear(SJAALTJES[rnd.nextInt(SJAALTJES.length)]);
            }
            if (rnd.nextInt(4) == 0) {
                g.wear(GuhClothes.JOCKEY_JASJE);
            }
        }
        int naam = r.zone == 1 ? 0 : 1 + Math.floorMod(key.hashCode(), NAMEN - 1);
        g.setCustomName(Component.translatable("vadsrijk.ruiters.naam." + naam));
        g.setCustomNameVisible(false);
        return g;
    }

    // ---- click -----------------------------------------------------------------------------------------------------------------

    /** A click on a rider or its Pluimguh: a cute line in chat (zone 1: "Ooit krijg jij er ook één!"). */
    static void klik(ServerPlayer p, Entity e) {
        long nu = p.level().getGameTime();
        Long pauze = KLIK_PAUZE.get(p.getUUID());
        if (pauze != null && nu < pauze) {
            return;
        }
        KLIK_PAUZE.put(p.getUUID(), nu + 20);
        Paar paar = null;
        for (Beheer b : BEHEERDERS) {
            for (Paar x : b.paren.values()) {
                if (x.pluim == e || x.guh == e || x.verzorger == e) {
                    paar = x;
                }
            }
        }
        if (paar == null) {
            return;
        }
        Entity spreker = paar.guh != null ? paar.guh : paar.verzorger != null ? paar.verzorger : paar.pluim;
        String regel = regel(paar, p.getRandom());
        p.sendSystemMessage(Component.translatable("vadsrijk.ruiters.zegt", spreker.getDisplayName(), Component.translatable(regel)));
        if (spreker instanceof RuiterGuh g) {
            g.getLookControl().setLookAt(p);
        }
    }

    static final int REGELS_Z1 = 3, REGELS_Z3 = 6;

    static String regel(Paar paar, RandomSource rnd) {
        if (paar.route.zone == 1) {
            return rnd.nextInt(2) == 0 ? "vadsrijk.ruiters.z1.0" : "vadsrijk.ruiters.z1." + rnd.nextInt(REGELS_Z1);
        }
        if (paar.verzorger != null) {
            return "vadsrijk.ruiters.verzorger";
        }
        if (paar.route.modus == RuiterRoute.Modus.STAL) {
            return "vadsrijk.ruiters.wacht";
        }
        if (paar.route.id.equals("z3_rijpad") || paar.route.id.equals("z3_wenteltrap")) {
            return rnd.nextBoolean() ? "vadsrijk.ruiters.trap" : "vadsrijk.ruiters.z3." + rnd.nextInt(REGELS_Z3);
        }
        return "vadsrijk.ruiters.z3." + rnd.nextInt(REGELS_Z3);
    }

    // ---- dev -------------------------------------------------------------------------------------------------------------------

    /** /guhs vadsrijk ruiters spawn: a demo rider doing small laps (r 5) round this spot for 2 minutes (its look is random). */
    static String demo(ServerPlayer p) {
        List<Vec3> pts = new ArrayList<>();
        Vec3 c = p.position();
        for (int i = 0; i <= 24; i++) {
            double a = i * Math.PI * 2 / 24;
            pts.add(new Vec3(c.x + 5 * Math.cos(a), c.y, c.z + 5 * Math.sin(a)));
        }
        String id = "demo" + (++demoTeller);
        DEMOS.add(new RuiterRoute(id, Vadsrijk.in(p) && p.getX() < 800 ? 1 : 3, RuiterRoute.Modus.RONDJE, 1, 0.2, pts));
        DEMO_TOT.put(id, p.level().getGameTime() + 20L * 120);
        return id;
    }
}
