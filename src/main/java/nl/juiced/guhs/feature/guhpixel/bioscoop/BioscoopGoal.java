package nl.juiced.guhs.feature.guhpixel.bioscoop;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.emotes.EmotesFeature;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadswoud.SleepInNestGoal;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A tamed guh goes to the film: when a projector nearby plays one and a Bioscoopstoeltje is free, it walks there, hops on
 * the cushion and watches (sitting pose, looking at the screen), with a bakje popcorn when a popcornmachine stands in the
 * cinema (flag {@link PxVlaggen#POPCORN}). At the film's cue points it reacts: it laughs, is startled (flag
 * {@link PxVlaggen#SCHRIK}: a little hop, popcorn flies), cheers, sniffs, or dozes off. After the film it claps and gets
 * down again; the film goes into its diary (once per film) and it gets a heart.
 * <p>
 * The guh does not ride anything (an own seat entity would have to be saved with it): it is put on the cushion every
 * tick. Priority 2, so a Guhhuisje resident (HuisjeGoal, 3) and a playing guh come along too; everything polite keeps
 * off through {@link GuhHooks#bezig}. Whatever happens (the seat breaks, the server stops half-way), {@link #opruimen}
 * takes the flags and the sitting pose away again.
 */
public class BioscoopGoal extends Goal {
    /** How far a guh comes from for a film (blocks from the projector). */
    public static final double KOMT_VAN = 16;
    /** The guhs that are at a film right now (in memory; {@link #opruimen} tidies up whoever is not in here). */
    static final Set<UUID> BEZOEKERS = ConcurrentHashMap.newKeySet();
    /** Persistent data: not again before (game time), and "we put this guh on a seat". */
    static final String RUST = "guhs_px_bioscoop_rust", ZIT = "guhs_px_bioscoop_zit", GEZIEN = "guhs_px_bioscoop_gezien";
    static final String NS = "guhbioscoop";

    private final GuhEntity guh;
    @Nullable
    private Voorstelling zaal;
    @Nullable
    private BlockPos stoel;
    private int wacht;
    private int loopt;
    private boolean zit, klaar;
    private int laatsteT;
    private int vertraging;
    private int schrikTot, slaapTot, naTicks;

    public BioscoopGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    private boolean vrij() {
        return guh.getType() == ModEntities.GUH.get() && guh.isTame() && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isVehicle()
                && !guh.isLeashed() && guh.mayWander() && !guh.isInWater() && guh.getTarget() == null && !Huisjes.isBinnen(guh);
    }

    @Override
    public boolean canUse() {
        if (--wacht > 0) {
            return false;
        }
        wacht = 8 + guh.getRandom().nextInt(8);
        if (!(guh.level() instanceof ServerLevel level) || !Voorstelling.iets(level) || !vrij() || GuhHooks.isBezig(guh)
                || !GuhKiezer.geclaimd(guh).isEmpty() || guh.getPersistentData().getLongOr(RUST, 0L) > level.getGameTime()) {
            return false;
        }
        long nu = level.getGameTime();
        for (Voorstelling v : Voorstelling.bij(level, guh.position(), KOMT_VAN)) {
            if (v.film.duur() - v.tijd(nu) < 100) {
                continue;       // (nearly over: not worth the walk)
            }
            BlockPos s = v.neemStoel(level, guh);
            if (s != null) {
                zaal = v;
                stoel = s;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return zaal != null && stoel != null && !klaar && !guh.isOrderedToSit() && !guh.isLeashed() && !guh.isPassenger()
                && guh.level().getBlockState(stoel).getBlock() instanceof StoeltjeBlock;
    }

    @Override
    public void start() {
        loopt = 0;
        zit = false;
        klaar = false;
        naTicks = 0;
        schrikTot = 0;
        slaapTot = 0;
        vertraging = guh.getRandom().nextInt(9);
        BEZOEKERS.add(guh.getUUID());
        GuhKiezer.claim(guh, NS);
        GuhHooks.bezig(guh, 40);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Voorstelling v = zaal;
        BlockPos s = stoel;
        if (v == null || s == null || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        GuhHooks.bezig(guh, 40);
        BlockState state = level.getBlockState(s);
        if (!(state.getBlock() instanceof StoeltjeBlock)) {
            klaar = true;
            return;
        }
        Vec3 plek = StoeltjeBlock.guhPlek(s, state);
        if (!zit) {
            if (v.afgelopen()) {
                klaar = true;
                return;
            }
            loopt++;
            double d2 = guh.distanceToSqr(plek);
            boolean stil = guh.getNavigation().isDone();
            if (d2 < 1.6 * 1.6 || loopt > 60 && stil && d2 < 6 * 6 || loopt > 160 && d2 < 12 * 12) {
                gaZitten(level, v, plek);       // (the last bit is a hop: rows of seats are hard to walk through)
                return;
            }
            if (loopt > 300) {
                klaar = true;
                return;
            }
            if (loopt % 15 == 1 || stil) {
                Direction voor = state.getValue(StoeltjeBlock.FACING);
                guh.getNavigation().moveTo(s.getX() + 0.5 + voor.getStepX(), s.getY(), s.getZ() + 0.5 + voor.getStepZ(), 1.15);
            }
            return;
        }
        // on the seat: stay put, look at the screen
        if (guh.distanceToSqr(plek) > 0.02 * 0.02) {
            guh.setPos(plek.x, plek.y, plek.z);
        }
        guh.setDeltaMovement(Vec3.ZERO);
        guh.fallDistance = 0;
        Vec3 scherm = v.doek.midden();
        float yaw = (float) (Mth.atan2(scherm.z - plek.z, scherm.x - plek.x) * Mth.RAD_TO_DEG) - 90f;
        guh.setYRot(yaw);
        guh.setYBodyRot(yaw);
        guh.setYHeadRot(yaw);
        if (!guh.isInSittingPose()) {
            guh.setInSittingPose(true);
        }
        long nu = level.getGameTime();
        if (schrikTot > 0 && --schrikTot == 0) {
            GuhHooks.zet(guh, PxVlaggen.SCHRIK, false);
        }
        if (slaapTot > 0) {
            if (--slaapTot == 0) {
                wakker();
            } else if ((guh.tickCount + guh.getId()) % 30 == 0) {
                level.sendParticles(EmotesFeature.GUH_ZZZ.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), 1, 0.12, 0.05, 0.12, 0.0);
            }
        }
        if (v.afgelopen()) {
            if (v.uitgezet()) {
                klaar = true;
                return;
            }
            if (naTicks++ == vertraging) {
                wakker();
                reageer(level, "juich");
                naDeFilm(v);
            }
            if (naTicks > 50 + vertraging) {
                klaar = true;
            }
            return;
        }
        if (GuhHooks.heeft(guh, PxVlaggen.POPCORN) != v.popcorn()) {
            GuhHooks.zet(guh, PxVlaggen.POPCORN, v.popcorn());
        }
        int t = v.tijd(nu) - vertraging;
        if (t > laatsteT) {
            for (FilmInfo.Cue cue : v.film.cues()) {
                if (cue.t() > laatsteT && cue.t() <= t) {
                    reageer(level, cue.soort());
                }
            }
            laatsteT = t;
        }
    }

    private void gaZitten(ServerLevel level, Voorstelling v, Vec3 plek) {
        zit = true;
        guh.getNavigation().stop();
        guh.setPos(plek.x, plek.y, plek.z);
        guh.setInSittingPose(true);
        guh.getPersistentData().putBoolean(ZIT, true);
        GuhHooks.zet(guh, PxVlaggen.POPCORN, v.popcorn());
        laatsteT = v.tijd(level.getGameTime()) - vertraging;     // (what was shown before it sat down it did not see)
    }

    /** One reaction to the film (the kinds of {@link FilmInfo#CUE_SOORTEN}). */
    void reageer(ServerLevel level, String soort) {
        boolean stem = guh.areSoundsEnabled();
        double x = guh.getX(), y = guh.getY() + guh.getBbHeight(), z = guh.getZ();
        switch (soort) {
            case "lach" -> {
                wakker();
                guh.triggerAnim("action", "happy");
                if (stem) {
                    guh.playSound(ModSounds.GUH_HAPPY.get(), 0.7f, guh.getVoicePitch() * (1.05f + guh.getRandom().nextFloat() * 0.25f));
                }
                level.sendParticles(ParticleTypes.NOTE, x, y + 0.3, z, 1, 0.2, 0.1, 0.2, 0.5);
            }
            case "schrik" -> {
                wakker();
                GuhHooks.zet(guh, PxVlaggen.SCHRIK, true);
                schrikTot = 16;
                if (stem) {
                    guh.playSound(ModSounds.GUH_AMBIENT.get(), 0.8f, guh.getVoicePitch() * 1.7f);
                }
                if (GuhHooks.heeft(guh, PxVlaggen.POPCORN)) {
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, BioscoopSlice.POPCORN.get()), x, y + 0.2, z, 6, 0.15, 0.1, 0.15, 0.08);
                    level.playSound(null, guh.blockPosition(), BioscoopSlice.POP.get(), SoundSource.NEUTRAL, 0.4f, 1.3f);
                }
            }
            case "juich" -> {
                wakker();
                guh.triggerAnim("action", "happy");
                if (stem) {
                    guh.playSound(ModSounds.GUH_HAPPY.get(), 0.9f, guh.getVoicePitch() * 1.25f);
                }
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 5, 0.3, 0.2, 0.3, 0);
            }
            case "snik" -> {
                wakker();
                if (stem) {
                    guh.playSound(ModSounds.GUH_AMBIENT.get(), 0.5f, guh.getVoicePitch() * 0.8f);
                }
                level.sendParticles(BandFeature.HARTJE.get(), x, y + 0.3, z, 2, 0.2, 0.1, 0.2, 0.0);
            }
            case "slaap" -> {
                GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, true);
                slaapTot = 240;
                level.sendParticles(EmotesFeature.GUH_ZZZ.get(), x, y + 0.2, z, 1, 0.12, 0.05, 0.12, 0.0);
            }
            case "wakker" -> wakker();
            default -> {
            }
        }
    }

    private void wakker() {
        if (slaapTot > 0 || GuhHooks.heeft(guh, SleepInNestGoal.OOGJES_DICHT)) {
            slaapTot = 0;
            GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, false);
        }
    }

    /** The film ran to its end with this guh in a seat: a heart, and a line in its diary the first time it saw this film. */
    private void naDeFilm(Voorstelling v) {
        if (!Band.isBandGuh(guh)) {
            return;
        }
        ServerPlayer baas = Band.eigenaarOnline(guh);
        Band.geefHartjes(guh, baas, 1, Reden.OVERIG);
        CompoundTag data = guh.getPersistentData();
        String gezien = data.getStringOr(GEZIEN, "");
        String id = v.film.id();
        if (!("," + gezien + ",").contains("," + id + ",")) {
            data.putString(GEZIEN, gezien.isEmpty() ? id : gezien + "," + id);
            Dagboek.wistJeDat(guh, v.popcorn() ? "gui.guhs.guhbioscoop.dagboek.film_popcorn" : "gui.guhs.guhbioscoop.dagboek.film", Bioscoop.naam(id));
        }
    }

    @Override
    public void stop() {
        Voorstelling v = zaal;
        BlockPos s = stoel;
        boolean zat = zit;
        zaal = null;
        stoel = null;
        zit = false;
        if (v != null && s != null) {
            v.geefStoel(s, guh.getUUID());
        }
        BEZOEKERS.remove(guh.getUUID());
        opruimen(guh);
        guh.getNavigation().stop();
        if (zat && s != null && guh.level() instanceof ServerLevel level && !guh.isPassenger()) {
            // down from the seat: in front of it when there is room
            BlockState state = level.getBlockState(s);
            Direction voor = state.getBlock() instanceof StoeltjeBlock ? state.getValue(StoeltjeBlock.FACING) : Direction.NORTH;
            Vec3 af = new Vec3(s.getX() + 0.5 + voor.getStepX(), s.getY() + 0.05, s.getZ() + 0.5 + voor.getStepZ());
            if (level.noCollision(guh, guh.getBoundingBox().move(af.subtract(guh.position())))) {
                guh.setPos(af.x, af.y, af.z);
            }
        }
        long nu = guh.level().getGameTime();
        // (a guh that could not reach its seat tries again later; after a film it is free for the next one at once)
        guh.getPersistentData().putLong(RUST, nu + (zat ? 20 : 200 + guh.getRandom().nextInt(200)));
        wacht = 10;
    }

    /** Takes everything the cinema put on a guh away again (also after a crash half-way a film: the flags are saved). */
    static void opruimen(GuhEntity guh) {
        if (GuhHooks.heeft(guh, PxVlaggen.POPCORN)) {
            GuhHooks.zet(guh, PxVlaggen.POPCORN, false);
        }
        if (GuhHooks.heeft(guh, PxVlaggen.SCHRIK)) {
            GuhHooks.zet(guh, PxVlaggen.SCHRIK, false);
        }
        CompoundTag data = guh.getPersistentData();
        if (data.getBooleanOr(ZIT, false)) {
            data.remove(ZIT);
            GuhHooks.zet(guh, SleepInNestGoal.OOGJES_DICHT, false);
            if (!guh.isOrderedToSit()) {
                guh.setInSittingPose(false);
            }
        }
        if (NS.equals(GuhKiezer.geclaimd(guh))) {
            GuhKiezer.los(guh);
            GuhHooks.bezig(guh, 0);
        }
    }

    /** (GuhHooks.tick) a guh that carries cinema marks but is not at a film (a restart half-way): tidy it up. */
    static void bewaak(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 40 != 0 || BEZOEKERS.contains(guh.getUUID())) {
            return;
        }
        if (GuhHooks.heeft(guh, PxVlaggen.POPCORN | PxVlaggen.SCHRIK) || guh.getPersistentData().getBooleanOr(ZIT, false)
                || NS.equals(GuhKiezer.geclaimd(guh))) {
            opruimen(guh);
        }
    }

    // --- for the tests --------------------------------------------------------------------------------------------------

    public boolean zit() {
        return zit;
    }

    @Nullable
    public BlockPos stoel() {
        return stoel;
    }

    @Nullable
    public static BioscoopGoal van(GuhEntity guh) {
        return guh.goalSelector.getAvailableGoals().stream().filter(w -> w.getGoal() instanceof BioscoopGoal).map(w -> (BioscoopGoal) w.getGoal())
                .findFirst().orElse(null);
    }

    public static boolean bezig(GuhEntity guh) {
        return guh.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.isRunning() && w.getGoal() instanceof BioscoopGoal);
    }
}
