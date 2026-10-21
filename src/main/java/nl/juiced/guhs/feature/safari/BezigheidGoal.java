package nl.juiced.guhs.feature.safari;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.safari.gedrag.Kijkplekken;

/**
 * Round 3 (design ch. 5, ronde 40): what a park animal is busy with. Every 10-40 seconds it picks an activity (about 60%
 * active, 40% rest): stroll at home, visit a favourite spot in view of a viewpoint (so you almost always see animals from
 * the huts and towers), one of its species' own behaviours ({@code acties} in soorten.json), eat, play, look around, or
 * rest (lying down; now and then a nap with its eyes shut). Herd animals keep together: when one runs or strolls off, the
 * others of its home follow to the same spot.
 * <p>
 * Behaviours ({@code acties}): {@code rust}, {@code anim:<a>} (its own animation), {@code spuit} (trunk up, a shower),
 * {@code kudde_ren} (the herd runs to another spot), {@code spring} (hops), {@code buikglij} (a belly slide over snow or
 * ice), {@code glijbaan} (up the steps and down the slide: points {@code glijbaan}, bottom to top), {@code zwem},
 * {@code drink}, {@code draag} (a branch to the dam: points {@code beverdam}), {@code burcht} (a while in the lodge:
 * {@code beverburcht}), {@code tunnel} (through the badger tunnel: points {@code dassentunnel}, top to room).
 * <p>
 * Cheap: nothing happens without a player within 48 blocks (the scheduler then just waits), and only the activity's own
 * work runs per tick. Sleeping at night stays {@link SafariDier}'s.
 */
public final class BezigheidGoal extends Goal {
    public enum Soort { SCHARREL, FAVORIET, GEDRAG, EET, SPEEL, KIJK, RUST }

    /** Rest-like activities (the other ones count as "active"). */
    public static boolean rustig(Soort s) {
        return s == Soort.RUST || s == Soort.KIJK;
    }

    /** One draw of the next activity (pure: the weights of design ronde 40). */
    public static Soort kies(RandomSource r, boolean favorieten, boolean gedrag, boolean boom) {
        int scharrel = boom ? 6 : 22, fav = favorieten ? 30 : 0, ged = gedrag ? 18 : 0;
        int eet = 7, speel = 4, kijk = 10, rust = boom ? 30 : 20;
        int som = scharrel + fav + ged + eet + speel + kijk + rust;
        int n = r.nextInt(som);
        if ((n -= scharrel) < 0) {
            return Soort.SCHARREL;
        }
        if ((n -= fav) < 0) {
            return Soort.FAVORIET;
        }
        if ((n -= ged) < 0) {
            return Soort.GEDRAG;
        }
        if ((n -= eet) < 0) {
            return Soort.EET;
        }
        if ((n -= speel) < 0) {
            return Soort.SPEEL;
        }
        if ((n -= kijk) < 0) {
            return Soort.KIJK;
        }
        return Soort.RUST;
    }

    /** A herd's current destination: key = level + home, until = game time. */
    private record Kudde(BlockPos doel, long tot, boolean ren) {
    }

    private static final Map<String, Kudde> KUDDES = new HashMap<>();

    private final SafariDier dier;
    @Nullable
    private Soort nu;
    @Nullable
    private Soort geforceerd;
    private int duur, pauze = 40;
    /** The behaviour being done (GEDRAG) and its stage. */
    private String actie = "";
    private int fase, vast;
    private final List<BlockPos> pad = new ArrayList<>();
    private int padI;
    @Nullable
    private BlockPos doel;
    private double snelheid = 1.0;

    BezigheidGoal(SafariDier dier) {
        this.dier = dier;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Nullable
    public Soort nu() {
        return nu;
    }

    /** (Tests) the next activity will be this one, at once. */
    public void forceer(Soort s) {
        geforceerd = s;
        pauze = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public boolean canUse() {
        ParkData.Soort info = dier.info();
        if (!dier.geregeld || dier.slaapt() || info == null || dier.hangt() || !(dier.level() instanceof ServerLevel level)) {
            return false;
        }
        if (pauze > 0) {
            pauze--;
            return false;
        }
        if (!SafariDier.ALTIJD_ACTIEF && !level.hasNearbyAlivePlayer(dier.getX(), dier.getY(), dier.getZ(), 48)) {
            pauze = 60;          // nobody to see it: no pathfinding (about 450 animals in the park)
            return false;
        }
        return begin(level, info);
    }

    @Override
    public boolean canContinueToUse() {
        return nu != null && duur > 0 && !dier.slaapt() || nu == Soort.RUST && dier.dutTot > 0 && duur > 0;
    }

    @Override
    public void stop() {
        if (nu != null && nu != Soort.RUST || dier.dutTot == 0) {
            dier.stopActie();
        }
        nu = null;
        actie = "";
        dier.bezoek = null;
        dier.getNavigation().stop();
        pauze = 20 + dier.getRandom().nextInt(60);
    }

    // =====================================================================================================================
    // choosing
    // =====================================================================================================================

    private boolean begin(ServerLevel level, ParkData.Soort info) {
        RandomSource r = dier.getRandom();
        // (the favourites are park coordinates: not for animals elsewhere, such as the home animals; tests excepted)
        List<Kijkplekken.Favoriet> favs = Safari.echt(level) || SafariDier.ALTIJD_ACTIEF ? favorieten(level) : List.of();
        List<String> acties = info.acties().stream().filter(a -> !a.equals("rust")).toList();
        Soort s = geforceerd != null ? geforceerd : kies(r, !favs.isEmpty(), !acties.isEmpty(), info.heeft("boom"));
        geforceerd = null;
        if (s == Soort.FAVORIET && favs.isEmpty()) {
            s = Soort.SCHARREL;
        } else if (s == Soort.GEDRAG && acties.isEmpty()) {
            s = Soort.RUST;
        }
        // a herd that is on the move takes the others along
        if (info.heeft("kudde") && s != Soort.FAVORIET) {
            Kudde k = KUDDES.get(kuddeSleutel(level));
            if (k != null && k.tot() > level.getGameTime() && k.doel().distSqr(dier.blockPosition()) > 9) {
                return loop(level, Soort.SCHARREL, rond(level, k.doel(), 3), k.ren() ? 1.7 : 1.0, k.ren() ? "ren" : "", 260);
            }
        }
        nu = s;
        actie = "";
        fase = 0;
        vast = 0;
        pad.clear();
        switch (s) {
            case SCHARREL -> {
                BlockPos d = dier.kiesDoel(level);
                if (d == null) {
                    return rust(info, 100);
                }
                if (info.heeft("kudde")) {
                    KUDDES.put(kuddeSleutel(level), new Kudde(d, level.getGameTime() + 200, false));
                }
                return loop(level, Soort.SCHARREL, d, 1.0, "", 300);
            }
            case FAVORIET -> {
                Kijkplekken.Favoriet f = favs.get(r.nextInt(favs.size()));
                BlockPos d = rond(level, f.pos(), Math.max(1, f.straal()));
                dier.bezoek = f.pos();
                dier.bezoekStraal = f.straal() + 3;
                fase = 0;
                return loop(level, Soort.FAVORIET, d, 1.0, "", 900);
            }
            case GEDRAG -> {
                return gedrag(level, info, acties.get(r.nextInt(acties.size())));
            }
            case EET -> {
                dier.getNavigation().stop();
                duur = 80 + r.nextInt(100);
                dier.zetActie("eet", duur);
                return true;
            }
            case SPEEL -> {
                dier.getNavigation().stop();
                duur = 40;
                dier.zetActie(heeft(info, "blij") ? "blij" : "eet", duur);
                if (dier.onGround()) {
                    dier.getJumpControl().jump();
                }
                return true;
            }
            case KIJK -> {
                dier.getNavigation().stop();
                duur = 100 + r.nextInt(120);
                if (!info.idleMix().isEmpty()) {
                    dier.zetActie(info.idleMix().keySet().iterator().next(), duur);
                }
                return true;
            }
            default -> {
                return rust(info, 300 + r.nextInt(500));
            }
        }
    }

    private boolean rust(ParkData.Soort info, int ticks) {
        nu = Soort.RUST;
        dier.getNavigation().stop();
        duur = ticks;
        if (heeft(info, "rust")) {
            dier.zetActie("rust", duur);
        }
        if (dier.getRandom().nextInt(4) == 0 && !dier.level().isDarkOutside()) {
            dier.dutTot = duur;            // a nap: eyes shut (SafariDier sleeps while dutTot > 0)
        }
        return true;
    }

    private boolean loop(ServerLevel level, Soort s, @Nullable BlockPos d, double speed, String anim, int max) {
        if (d == null) {
            return rust(dier.info(), 100);
        }
        nu = s;
        doel = d;
        snelheid = speed;
        duur = max;
        if (!anim.isEmpty() && heeft(dier.info(), anim)) {
            dier.zetActie(anim, max);
        }
        return dier.getNavigation().moveTo(d.getX() + 0.5, d.getY(), d.getZ() + 0.5, speed) || s == Soort.FAVORIET;
    }

    private boolean gedrag(ServerLevel level, ParkData.Soort info, String a) {
        RandomSource r = dier.getRandom();
        nu = Soort.GEDRAG;
        actie = a;
        if (a.startsWith("anim:")) {
            String anim = a.substring(5);
            dier.getNavigation().stop();
            duur = 60 + r.nextInt(100);
            dier.zetActie(anim, duur);
            return true;
        }
        switch (a) {
            case "spuit" -> {
                dier.getNavigation().stop();
                duur = 40;
                if (heeft(info, "spuit")) {
                    dier.zetActie("spuit", duur);
                }
                return true;
            }
            case "kudde_ren" -> {
                BlockPos d = ver(level);
                if (d != null) {
                    KUDDES.put(kuddeSleutel(level), new Kudde(d, level.getGameTime() + 160, true));
                }
                return loop(level, Soort.GEDRAG, d, 1.7, "ren", 220);
            }
            case "spring" -> {
                return loop(level, Soort.GEDRAG, dier.kiesDoel(level), 1.2, "", 200);
            }
            case "buikglij" -> {
                if (!opIjs(level, dier.blockPosition())) {
                    return rust(info, 120);
                }
                dier.getNavigation().stop();
                dier.setYRot(r.nextFloat() * 360f);
                duur = 30 + r.nextInt(20);
                dier.zetActie("buikglij", duur);
                return true;
            }
            case "glijbaan" -> {
                List<BlockPos> p = bereikbaar(level, Kijkplekken.get().punten("glijbaan"));
                if (p.size() < 2) {
                    return gedrag(level, info, "buikglij");
                }
                pad.addAll(p);
                padI = 0;
                fase = 0;
                return pad(level, 1.0, "", 600, pad.get(pad.size() - 1));
            }
            case "zwem" -> {
                BlockPos d = waterDoel(level);
                return loop(level, Soort.GEDRAG, d, 1.0, "", 300);
            }
            case "drink" -> {
                BlockPos d = oever(level);
                fase = 0;
                return loop(level, Soort.GEDRAG, d, 1.0, "", 400);
            }
            case "draag" -> {
                BlockPos d = dichtste(level, "beverdam");
                if (d == null) {
                    return loop(level, Soort.GEDRAG, dier.kiesDoel(level), 1.0, "draag", 300);
                }
                dier.bezoek = d;
                dier.bezoekStraal = 4;
                return loop(level, Soort.GEDRAG, d, 1.0, "draag", 500);
            }
            case "burcht" -> {
                BlockPos d = dichtste(level, "beverburcht");
                if (d == null) {
                    return gedrag(level, info, "zwem");
                }
                dier.bezoek = d;
                dier.bezoekStraal = 4;
                fase = 0;
                return loop(level, Soort.GEDRAG, d, 1.0, "", 700);
            }
            case "tunnel" -> {
                List<BlockPos> p = bereikbaar(level, Kijkplekken.get().punten("dassentunnel"));
                if (p.size() < 2) {
                    return rust(info, 200);
                }
                // from whichever end it is nearer to, to the other end
                BlockPos eerst = p.get(0), laatst = p.get(p.size() - 1);
                if (dier.blockPosition().distSqr(laatst) < dier.blockPosition().distSqr(eerst)) {
                    p = new ArrayList<>(p);
                    java.util.Collections.reverse(p);
                }
                pad.addAll(p);
                padI = 0;
                fase = 0;
                return pad(level, 1.0, "", 700, pad.get(pad.size() - 1));
            }
            default -> {
                return rust(info, 200);
            }
        }
    }

    private boolean pad(ServerLevel level, double speed, String anim, int max, BlockPos eind) {
        dier.bezoek = eind;
        dier.bezoekStraal = 4;
        for (BlockPos p : pad) {
            dier.bezoekStraal = Math.max(dier.bezoekStraal, (int) Math.ceil(Math.sqrt(p.distSqr(eind))) + 2);
        }
        return loop(level, Soort.GEDRAG, pad.get(0), speed, anim, max);
    }

    // =====================================================================================================================
    // running
    // =====================================================================================================================

    @Override
    public void tick() {
        if (!(dier.level() instanceof ServerLevel level) || nu == null) {
            return;
        }
        duur--;
        boolean klaar = dier.getNavigation().isDone();
        switch (nu) {
            case SCHARREL -> {
                if (klaar) {
                    duur = Math.min(duur, 0);
                }
            }
            case FAVORIET -> {
                if (fase == 0 && klaar && !dichtbij(doel, 2.5) && dier.bezoek != null && ++vast <= 3) {
                    // no path to the chosen spot: straight to the favourite itself
                    BlockPos f = dier.bezoek;
                    doel = f;
                    dier.getNavigation().moveTo(f.getX() + 0.5, f.getY(), f.getZ() + 0.5, snelheid);
                    klaar = false;
                }
                if (fase == 0 && (klaar || dichtbij(doel, 2.5))) {
                    // arrived: stay a while where you are seen (eat, look around, rest)
                    fase = 1;
                    duur = 200 + dier.getRandom().nextInt(300);
                }
                if (fase == 1 && dier.tickCount % 80 == 0 && dier.actie().isEmpty()) {
                    ParkData.Soort info = dier.info();
                    String a = dier.getRandom().nextBoolean() ? "eet" : !info.idleMix().isEmpty() ? info.idleMix().keySet().iterator().next() : "blij";
                    if (heeft(info, a)) {
                        dier.zetActie(a, 50);
                    }
                }
            }
            case GEDRAG -> gedragTick(level, klaar);
            default -> {
            }
        }
    }

    private void gedragTick(ServerLevel level, boolean klaar) {
        switch (actie) {
            case "spuit" -> {
                if (duur % 4 == 0 && duur < 32) {
                    Vec3 kop = dier.position().add(dier.getLookAngle().scale(dier.getBbWidth() * 0.6)).add(0, dier.getBbHeight() + 0.4, 0);
                    level.sendParticles(ParticleTypes.SPLASH, kop.x, kop.y, kop.z, 8, 0.4, 0.3, 0.4, 0.2);
                    level.sendParticles(ParticleTypes.FALLING_WATER, kop.x, kop.y + 0.6, kop.z, 4, 0.5, 0.2, 0.5, 0.0);
                }
            }
            case "spring" -> {
                if (!klaar && dier.onGround() && dier.tickCount % 12 == 0) {
                    dier.getJumpControl().jump();
                }
                if (klaar) {
                    duur = 0;
                }
            }
            case "buikglij" -> glij(level);
            case "glijbaan" -> {
                if (fase == 0) {
                    // walking up the steps, point by point; at the top: slide down to the first point
                    if (volgPad(level, 1.0)) {
                        fase = 1;
                        dier.zetActie("buikglij", 120);
                        doel = pad.get(0);
                        dier.getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 2.0);
                    }
                } else {
                    if (dier.isInWater() && dier.tickCount % 6 == 0) {
                        level.sendParticles(ParticleTypes.SPLASH, dier.getX(), dier.getY() + 0.3, dier.getZ(), 6, 0.3, 0.1, 0.3, 0.1);
                    }
                    if (dier.getNavigation().isDone() || dichtbij(doel, 1.5)) {
                        dier.stopActie();
                        duur = 0;
                    }
                }
            }
            case "tunnel" -> {
                if (fase == 0 && volgPad(level, 1.0)) {
                    fase = 1;               // at the end (the room or the top): stay a while
                    duur = 200 + dier.getRandom().nextInt(250);
                    dier.getNavigation().stop();
                }
            }
            case "drink" -> {
                if (fase == 0 && klaar) {
                    fase = 1;
                    duur = 80;
                    dier.zetActie("eet", 80);
                }
            }
            case "burcht" -> {
                if (fase == 0 && (klaar || dichtbij(doel, 2))) {
                    fase = 1;
                    duur = 200 + dier.getRandom().nextInt(200);
                    dier.getNavigation().stop();
                }
            }
            case "draag" -> {
                if (klaar) {
                    dier.stopActie();
                    dier.zetActie("eet", 40);
                    duur = Math.min(duur, 40);
                }
            }
            case "zwem", "kudde_ren" -> {
                if (klaar) {
                    duur = 0;
                }
            }
            default -> {
            }
        }
    }

    /** Follows {@link #pad}; true when the last point is reached. Gets stuck in a narrow tunnel: hops to the next point. */
    private boolean volgPad(ServerLevel level, double speed) {
        if (padI >= pad.size()) {
            return true;
        }
        BlockPos p = pad.get(padI);
        if (dichtbij(p, 1.4)) {
            padI++;
            vast = 0;
            if (padI >= pad.size()) {
                return true;
            }
            p = pad.get(padI);
            dier.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, speed);
            return false;
        }
        if (dier.getNavigation().isDone()) {
            if (++vast > 30) {
                dier.teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                vast = 0;
            } else {
                dier.getNavigation().moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, speed);
            }
        }
        return false;
    }

    /** The belly slide: a push along its look over snow and ice, a splash when it reaches the water. */
    private void glij(ServerLevel level) {
        Vec3 kijk = Vec3.directionFromRotation(0, dier.getYRot());
        BlockPos voor = BlockPos.containing(dier.position().add(kijk.scale(0.9)));
        boolean muur = !level.getBlockState(voor).getCollisionShape(level, voor).isEmpty();
        boolean binnen = dier.zoneIdx() < 0 || !Safari.echt(level) || ParkData.get().zone(voor.getX(), voor.getZ()) == dier.zoneIdx();
        if (muur || !binnen || Math.hypot(voor.getX() - dier.thuis().getX(), voor.getZ() - dier.thuis().getZ()) > dier.straalThuis()) {
            dier.setYRot(dier.getYRot() + 120 + dier.getRandom().nextInt(120));
            return;
        }
        Vec3 v = dier.getDeltaMovement();
        dier.setDeltaMovement(kijk.x * 0.28, v.y, kijk.z * 0.28);
        if (dier.isInWater() && dier.tickCount % 5 == 0) {
            level.sendParticles(ParticleTypes.SPLASH, dier.getX(), dier.getY() + 0.3, dier.getZ(), 6, 0.3, 0.1, 0.3, 0.1);
        } else if (dier.tickCount % 4 == 0) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, dier.getX(), dier.getY() + 0.1, dier.getZ(), 2, 0.2, 0.05, 0.2, 0.01);
        }
    }

    // =====================================================================================================================
    // places
    // =====================================================================================================================

    /** The favourite spots it may visit: for its species (or any), in its zone, not too far from home. */
    private List<Kijkplekken.Favoriet> favorieten(ServerLevel level) {
        List<Kijkplekken.Favoriet> uit = new ArrayList<>();
        String soort = dier.soort();
        BlockPos thuis = dier.thuis();
        int reik = dier.straalThuis() + 20;
        for (Kijkplekken.Kijkplek k : Kijkplekken.get().kijkplekken) {
            for (Kijkplekken.Favoriet f : k.favorieten()) {
                if (!f.voor(soort)) {
                    continue;
                }
                double d = Math.hypot(f.pos().getX() - thuis.getX(), f.pos().getZ() - thuis.getZ());
                if (d > reik) {
                    continue;
                }
                if (dier.zoneIdx() >= 0 && Safari.echt(level) && ParkData.get().zone(f.pos().getX(), f.pos().getZ()) != dier.zoneIdx()) {
                    continue;
                }
                uit.add(f);
            }
        }
        return uit;
    }

    /** The points of a list that lie within reach of its home (and in its zone). */
    private List<BlockPos> bereikbaar(ServerLevel level, List<BlockPos> punten) {
        List<BlockPos> uit = new ArrayList<>();
        if (punten.isEmpty()) {
            return uit;
        }
        BlockPos thuis = dier.thuis();
        for (BlockPos p : punten) {
            if (Math.hypot(p.getX() - thuis.getX(), p.getZ() - thuis.getZ()) > dier.straalThuis() + 40) {
                return List.of();             // a list of another place (another penguin slide or tunnel)
            }
            if (dier.zoneIdx() >= 0 && Safari.echt(level) && ParkData.get().zone(p.getX(), p.getZ()) != dier.zoneIdx()) {
                return List.of();
            }
            uit.add(p);
        }
        return uit;
    }

    @Nullable
    private BlockPos dichtste(ServerLevel level, String naam) {
        BlockPos beste = null;
        double b = Double.MAX_VALUE;
        for (BlockPos p : Kijkplekken.get().punten(naam)) {
            double d = p.distSqr(dier.thuis());
            if (d < b && Math.sqrt(d) <= dier.straalThuis() + 40
                    && (dier.zoneIdx() < 0 || !Safari.echt(level) || ParkData.get().zone(p.getX(), p.getZ()) == dier.zoneIdx())) {
                b = d;
                beste = p;
            }
        }
        return beste;
    }

    /** A standing spot near {@code p} (within r), where this animal may stand. */
    @Nullable
    private BlockPos rond(ServerLevel level, BlockPos p, int r) {
        RandomSource rnd = dier.getRandom();
        for (int i = 0; i < 6; i++) {
            int x = p.getX() + rnd.nextInt(2 * r + 1) - r, z = p.getZ() + rnd.nextInt(2 * r + 1) - r;
            BlockPos q = dier.magStaan(level, x, z);
            if (q != null && Math.abs(q.getY() - p.getY()) <= 3) {
                return q;           // (not on a roof or under an overhang above the spot)
            }
        }
        BlockPos q = dier.magStaan(level, p.getX(), p.getZ());
        return q != null && Math.abs(q.getY() - p.getY()) <= 3 ? q : p;
    }

    /** A spot far across its home (for the herd run). */
    @Nullable
    private BlockPos ver(ServerLevel level) {
        BlockPos beste = null;
        double b = -1;
        for (int i = 0; i < 6; i++) {
            BlockPos d = dier.kiesDoel(level);
            if (d != null && d.distSqr(dier.blockPosition()) > b) {
                b = d.distSqr(dier.blockPosition());
                beste = d;
            }
        }
        return beste;
    }

    /** Water in its home (it swims there). */
    @Nullable
    private BlockPos waterDoel(ServerLevel level) {
        RandomSource r = dier.getRandom();
        BlockPos thuis = dier.thuis();
        int s = dier.straalThuis();
        for (int i = 0; i < 12; i++) {
            double hoek = r.nextDouble() * Mth.TWO_PI, d = Math.sqrt(r.nextDouble()) * s;
            int x = thuis.getX() + (int) Math.round(Math.cos(hoek) * d), z = thuis.getZ() + (int) Math.round(Math.sin(hoek) * d);
            BlockPos p = dier.grond(level, x, z);
            if (!level.getFluidState(p.below()).isEmpty() || !level.getFluidState(p).isEmpty()) {
                if (dier.zoneIdx() < 0 || !Safari.echt(level) || ParkData.get().zone(x, z) == dier.zoneIdx()) {
                    return p;
                }
            }
        }
        return dier.kiesDoel(level);
    }

    /** A dry spot at the water's edge in its home (it drinks there). */
    @Nullable
    private BlockPos oever(ServerLevel level) {
        RandomSource r = dier.getRandom();
        BlockPos thuis = dier.thuis();
        int s = dier.straalThuis();
        for (int i = 0; i < 16; i++) {
            double hoek = r.nextDouble() * Mth.TWO_PI, d = Math.sqrt(r.nextDouble()) * s;
            int x = thuis.getX() + (int) Math.round(Math.cos(hoek) * d), z = thuis.getZ() + (int) Math.round(Math.sin(hoek) * d);
            BlockPos p = dier.magStaan(level, x, z);
            if (p == null || !level.getFluidState(p.below()).isEmpty()) {
                continue;
            }
            for (BlockPos n : new BlockPos[]{p.north(), p.south(), p.east(), p.west()}) {
                if (!level.getFluidState(n.below()).isEmpty() || !level.getFluidState(n).isEmpty()) {
                    return p;
                }
            }
        }
        return dier.kiesDoel(level);
    }

    private static boolean opIjs(ServerLevel level, BlockPos p) {
        BlockState onder = level.getBlockState(p.below());
        BlockState hier = level.getBlockState(p);
        return onder.is(BlockTags.ICE) || onder.is(BlockTags.SNOW) || onder.is(Blocks.SNOW_BLOCK) || hier.is(Blocks.SNOW);
    }

    private boolean dichtbij(@Nullable BlockPos p, double r) {
        return p != null && dier.distanceToSqr(p.getX() + 0.5, p.getY(), p.getZ() + 0.5) < r * r + 1;
    }

    private String kuddeSleutel(ServerLevel level) {
        return level.dimension().identifier() + "#" + dier.plek() + "#" + dier.thuis().asLong();
    }

    private static boolean heeft(@Nullable ParkData.Soort info, String anim) {
        return info != null && info.animaties().contains(anim);
    }
}
