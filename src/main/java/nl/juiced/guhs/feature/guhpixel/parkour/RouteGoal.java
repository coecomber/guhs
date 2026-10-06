package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.speelgoed.SpeelTaak;
import nl.juiced.guhs.feature.speelgoed.ZitjeEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * A guh that was put on a Guh-parkour runs it (priority 3, before playing, following and wandering): to the Startpaaltje,
 * then piece after piece in order (walks to it, USES it: {@link RouteStukken}), to the Finishpaaltje, a little cheer,
 * and again, until it is taken out. The lap time runs from leaving the Startpaaltje to touching the Finishpaaltje (without
 * one: to the end of the last piece). Sitting on command pauses it; an emote or a fright interrupts it and it then goes on
 * with the same piece (the clock keeps running). A piece it cannot get to is skipped and that lap does not count.
 * <p>
 * Which post it belongs to is persistent data of the guh ({@link #PAAL}), so it goes on after a chunk reload; when the post
 * is gone or no longer lists it, it frees itself ({@link #vrij}).
 */
public class RouteGoal extends Goal {
    /** Guh persistent data: the position (as a long) of its Startpaaltje. */
    public static final String PAAL = "guhs_px_parkour_paal";
    /** Guh persistent data: the dimension of that post (posts set before this existed have none: any dimension). */
    public static final String PAAL_DIM = "guhs_px_parkour_dim";
    /**
     * A guh that is this long (ticks) away from its post (far from it, or in another dimension) is off the route: it would
     * otherwise keep its claim for ever, because an unloaded post cannot tell it that it was taken off or broken, and a
     * claimed guh cannot go on holiday, to the Guhkantoor, the Guhkade or the Guhbioscoop.
     */
    static final int WEG_TICKS = 20 * 60 * 5;
    private static final Identifier VER_PAD = Guhs.id("guhparkour_pad");
    /** A guh further than this from its post (its owner took it along) just waits until it is back. */
    private static final int MAX_AFSTAND = Routes.BEREIK + 24;
    private static final double SNELHEID = 1.2;

    enum Fase { NAAR_START, NAAR_STUK, STUK, NAAR_FINISH, PAUZE }

    private final GuhEntity guh;
    private Fase fase = Fase.NAAR_START;
    private int index, gedaan, faseTicks, wacht;
    private long rondeStart;
    private boolean geldig;
    /** The game time at which the guh was first seen away from its post (-1: it is not away). */
    private long wegSinds = -1;
    @Nullable
    private RouteStukken.Stap stap;
    @Nullable
    private Loop loop;

    public RouteGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    /** Takes a guh off its route: free again (also called by the guh itself when its post is gone). */
    public static void vrij(GuhEntity guh) {
        guh.getPersistentData().remove(PAAL);
        guh.getPersistentData().remove(PAAL_DIM);
        if (ParkourSlice.NS.equals(GuhKiezer.geclaimd(guh))) {
            GuhKiezer.los(guh);
        }
        GuhHooks.zet(guh, PxVlaggen.OP_ROUTE, false);
    }

    /** The post this guh runs for (null: none, or not loaded). A loaded post that no longer lists the guh frees it. */
    @Nullable
    public static StartpaalBlockEntity paal(GuhEntity guh) {
        var data = guh.getPersistentData();
        if (!data.contains(PAAL) || !(guh.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos pos = BlockPos.of(data.getLongOr(PAAL, 0L));
        if (andereDimensie(guh, level) || !level.isLoaded(pos)) {
            return null;
        }
        if (level.getBlockEntity(pos) instanceof StartpaalBlockEntity paal && paal.heeftGuh(guh.getUUID())) {
            return paal;
        }
        vrij(guh);
        return null;
    }

    private static boolean andereDimensie(GuhEntity guh, ServerLevel level) {
        String dim = guh.getPersistentData().getStringOr(PAAL_DIM, "");
        return !dim.isEmpty() && !dim.equals(level.dimension().identifier().toString());
    }

    /**
     * Is this guh away from its post: in another dimension, or further than a waiting guh ever is while the post is not
     * loaded? (A guh near an unloaded post just waits: the chunk border can run between them.)
     */
    static boolean isWeg(GuhEntity guh) {
        var data = guh.getPersistentData();
        if (!data.contains(PAAL) || !(guh.level() instanceof ServerLevel level)) {
            return false;
        }
        if (andereDimensie(guh, level)) {
            return true;
        }
        BlockPos pos = BlockPos.of(data.getLongOr(PAAL, 0L));
        return !level.isLoaded(pos) && !guh.blockPosition().closerThan(pos, MAX_AFSTAND);
    }

    /** Called while the guh has a post but cannot run: frees it when it has been away for {@link #WEG_TICKS}. True = freed. */
    boolean bewaakWeg(long nu) {
        if (!isWeg(guh)) {
            wegSinds = -1;
            return false;
        }
        if (wegSinds < 0) {
            wegSinds = nu;
            return false;
        }
        if (nu - wegSinds < WEG_TICKS) {
            return false;
        }
        wegSinds = -1;
        vrij(guh);
        return true;
    }

    private boolean kan(StartpaalBlockEntity paal) {
        return guh.getType() == ModEntities.GUH.get() && guh.isTame() && !guh.isOrderedToSit() && !guh.isLeashed() && !guh.isVehicle()
                && (!guh.isPassenger() || guh.getVehicle() instanceof ZitjeEntity) && !paal.stukken().isEmpty()
                && guh.blockPosition().closerThan(paal.getBlockPos(), MAX_AFSTAND);
    }

    @Override
    public boolean canUse() {
        if (!guh.getPersistentData().contains(PAAL)) {
            return false;
        }
        if (wacht > 0) {
            wacht--;
            return false;
        }
        StartpaalBlockEntity paal = paal(guh);
        if (paal == null || !kan(paal)) {
            wacht = 10;
            if (paal == null) {
                bewaakWeg(guh.level().getGameTime());
            } else {
                wegSinds = -1;
            }
            return false;
        }
        wegSinds = -1;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        StartpaalBlockEntity paal = paal(guh);
        return paal != null && kan(paal);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        GuhHooks.bezig(guh, 40);
        verPad(true);
        if (fase == Fase.STUK) {
            fase = Fase.NAAR_STUK;      // (interrupted halfway a piece: walk up to it again)
        }
        faseTicks = 0;
        loop = null;
    }

    @Override
    public void stop() {
        if (stap != null) {
            boolean klaar = stap.gedaan();
            eindigStap();
            if (klaar) {
                index++;
                gedaan++;
            }
        }
        if (fase == Fase.STUK) {
            fase = Fase.NAAR_STUK;
        }
        loop = null;
        guh.getNavigation().stop();
        verPad(false);
        GuhHooks.zet(guh, PxVlaggen.OP_ROUTE, false);
        if (!guh.isPassenger()) {
            BandVlaggen.zet(guh, BandVlaggen.SPEELT, false);
            GuhHooks.bezig(guh, 0);
        }
        if (!guh.getPersistentData().contains(PAAL)) {     // (taken out: next time from the start)
            herstart();
        }
    }

    private void herstart() {
        fase = Fase.NAAR_START;
        index = 0;
        gedaan = 0;
        rondeStart = 0;
    }

    /** A longer path search: the pieces of a route may lie up to twice its reach apart. */
    private void verPad(boolean aan) {
        AttributeInstance range = guh.getAttribute(Attributes.FOLLOW_RANGE);
        if (range != null) {
            range.removeModifier(VER_PAD);
            if (aan) {
                range.addTransientModifier(new AttributeModifier(VER_PAD, 2.0 * Routes.BEREIK, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        guh.getNavigation().setMaxVisitedNodesMultiplier(aan ? 4f : 1f);
    }

    private void eindigStap() {
        RouteStukken.Stap s = stap;
        stap = null;
        if (s != null) {
            try {
                s.stop();
            } catch (RuntimeException e) {
                LogUtils.getLogger().warn("Guh-parkour: stopping a piece failed", e);
            }
        }
    }

    @Override
    public void tick() {
        StartpaalBlockEntity paal = paal(guh);
        if (paal == null || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        GuhHooks.bezig(guh, 40);
        if (!GuhHooks.heeft(guh, PxVlaggen.OP_ROUTE)) {
            GuhHooks.zet(guh, PxVlaggen.OP_ROUTE, true);
        }
        if (!BandVlaggen.heeft(guh, BandVlaggen.SPEELT)) {
            BandVlaggen.zet(guh, BandVlaggen.SPEELT, true);
        }
        List<BlockPos> stukken = paal.stukken();
        faseTicks++;
        switch (fase) {
            case NAAR_START -> {
                Vec3 doel = Vec3.atBottomCenterOf(paal.getBlockPos());
                if (loopt(level, doel, 1.5) || vast() || faseTicks > 600) {
                    rondeStart = level.getGameTime();
                    geldig = true;
                    index = 0;
                    gedaan = 0;
                    naar(Fase.NAAR_STUK);
                    level.playSound(null, paal.getBlockPos(), ParkourSlice.START.get(), SoundSource.NEUTRAL, 0.6f, guh.getVoicePitch());
                    level.sendParticles(ParticleTypes.CLOUD, guh.getX(), guh.getY() + 0.1, guh.getZ(), 4, 0.2, 0.02, 0.2, 0.02);
                }
            }
            case NAAR_STUK -> {
                if (index >= stukken.size()) {
                    rondeKlaar(paal, level);
                    return;
                }
                BlockPos pos = stukken.get(index);
                Routes.Soort soort = Routes.soort(level, pos);
                if (soort == Routes.Soort.FINISH) {
                    naar(Fase.NAAR_FINISH);
                    return;
                }
                if (stap == null) {
                    stap = RouteStukken.maak(guh, level, pos);
                    if (stap == null) {            // (the piece is gone: on to the next)
                        index++;
                        naar(Fase.NAAR_STUK);
                        return;
                    }
                }
                if (loopt(level, stap.instap(), stap.dichtbij())) {
                    paal.looptWeer(pos);
                    stap.begin();
                    naar(Fase.STUK);
                } else if (vast() || faseTicks > 500) {
                    paal.hapert(pos);
                    geldig = false;
                    eindigStap();
                    index++;
                    naar(Fase.NAAR_STUK);
                }
            }
            case STUK -> {
                boolean verder;
                try {
                    verder = stap != null && stap.tick();
                } catch (RuntimeException e) {
                    LogUtils.getLogger().warn("Guh-parkour: a piece failed", e);
                    verder = false;
                }
                if (!verder || (stap != null && faseTicks > stap.maxTicks())) {
                    eindigStap();
                    index++;
                    gedaan++;
                    naar(Fase.NAAR_STUK);
                }
            }
            case NAAR_FINISH -> {
                BlockPos pos = index < stukken.size() ? stukken.get(index) : paal.getBlockPos();
                if (loopt(level, Vec3.atBottomCenterOf(pos), 1.3)) {
                    paal.looptWeer(pos);
                    rondeKlaar(paal, level);
                } else if (vast() || faseTicks > 500) {
                    paal.hapert(pos);
                    geldig = false;
                    rondeKlaar(paal, level);
                }
            }
            case PAUZE -> {
                guh.getNavigation().stop();
                if (faseTicks > 30) {
                    naar(Fase.NAAR_START);
                }
            }
        }
    }

    private void naar(Fase nieuw) {
        fase = nieuw;
        faseTicks = 0;
        loop = null;
    }

    private void rondeKlaar(StartpaalBlockEntity paal, ServerLevel level) {
        if (geldig && gedaan > 0 && rondeStart > 0) {
            paal.rondje(guh, (int) Math.min(Integer.MAX_VALUE, level.getGameTime() - rondeStart));
        }
        herstart();
        naar(Fase.PAUZE);
    }

    // --- walking -----------------------------------------------------------------------------------------------------------------

    /** The shared walking of the toys (re-paths, notices when it gets nowhere). */
    private static final class Loop extends SpeelTaak {
        private final Vec3 doel;
        private final double dichtbij;
        private boolean er;

        Loop(Mob mob, ServerLevel level, Vec3 doel, double dichtbij) {
            super(mob, level);
            this.doel = doel;
            this.dichtbij = dichtbij;
        }

        @Override
        protected boolean speel() {
            er = loopNaar(doel, SNELHEID, dichtbij);
            return !er;
        }

        boolean isVast() {
            return vast();
        }
    }

    private boolean loopt(ServerLevel level, Vec3 doel, double dichtbij) {
        if (loop == null || loop.doel.distanceToSqr(doel) > 0.01) {
            loop = new Loop(guh, level, doel, dichtbij);
        }
        loop.tick();
        return loop.er;
    }

    private boolean vast() {
        return loop != null && loop.isVast();
    }

    // --- for tests and the dev command ---------------------------------------------------------------------------------------------

    Fase fase() {
        return fase;
    }

    int index() {
        return index;
    }
}
