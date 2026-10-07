package nl.juiced.guhs.feature.guhpixel.guhkade;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Reden;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.guhpixel.GuhKiezer;
import nl.juiced.guhs.feature.guhpixel.PxVlaggen;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Sim;
import nl.juiced.guhs.feature.guhpixel.guhkade.spel.Spel;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Tamed guhs play on the Guhkade cabinets (on every guh, through GuhHooks.doelen; priority 4, like playing with toys).
 * Now and then, when a player is around to see it, a free guh (also a huisje's resident that is outside) walks up to a
 * cabinet within {@link #BEREIK} blocks, stands in front of it and plays a whole game: the cabinet's screen shows that
 * very game ({@link KastBlockEntity#speelGuh}), it beeps, and the score goes on the cabinet's list under the guh's name.
 * How good it is: {@link GuhKunde}. A guh whose score was beaten by a player walks up, looks sad at the list, and then
 * practises: it comes back sooner, a few times, and those games count double.
 */
public class KastGoal extends Goal {
    public static final int BEREIK = 12, SPELER = 24;
    /** Tests: these guhs always go (no dice, no rest, no player needed) and play a very short game. */
    public static final Set<UUID> TEST_ALTIJD = ConcurrentHashMap.newKeySet();
    /** The guhs that are really playing now (a claim without its guh in here is a leftover: {@link #ruimOp}). */
    private static final Set<UUID> SPELEND = ConcurrentHashMap.newKeySet();

    private enum Fase { LOPEN, SPELEN, NA }

    private final GuhEntity guh;
    private int wacht;
    @Nullable
    private BlockPos kast;
    private Vec3 plek = Vec3.ZERO;
    private Fase fase = Fase.LOPEN;
    private int faseTicks, loopTicks;
    private long eind;
    /** Done with this cabinet (the goal ends; {@link #stop} lets go of the buttons). */
    private boolean gedaan;
    private int uitkomst;
    private Spel spel = Spel.FLAPPY;

    public KastGoal(GuhEntity guh) {
        this.guh = guh;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean vrij() {
        return guh.getType() == ModEntities.GUH.get() && guh.isTame() && !guh.isBaby() && !guh.isOrderedToSit() && !guh.isPassenger() && !guh.isVehicle()
                && !guh.isLeashed() && guh.mayWander() && !guh.isInWater() && guh.getTarget() == null && !Huisjes.isBinnen(guh);
    }

    @Override
    public boolean canUse() {
        if (--wacht > 0) {
            return false;
        }
        wacht = 20 + guh.getRandom().nextInt(20);          // (canUse runs every other tick: every two to four seconds)
        if (!(guh.level() instanceof ServerLevel level) || !vrij() || GuhHooks.isBezig(guh) || !GuhKiezer.geclaimd(guh).isEmpty()) {
            return false;
        }
        boolean test = TEST_ALTIJD.contains(guh.getUUID());
        long nu = level.getGameTime();
        var data = guh.getPersistentData();
        if (!test && data.getLongOr(GuhKunde.RUST, 0L) > nu) {
            return false;
        }
        boolean oefent = GuhKunde.oefen(guh) > 0;
        if (!test) {
            Player p = level.getNearestPlayer(guh, SPELER);
            if (p == null || p.isSpectator()) {
                return false;
            }
        }
        KastBlockEntity gekozen = null;
        boolean geklopt = false;
        for (KastBlockEntity be : Guhkade.Kasten.rond(level, guh.blockPosition(), BEREIK)) {
            if (!be.vrijVoor(guh.getUUID()) || !staanplekVrij(level, be)) {
                continue;
            }
            if (be.isGeklopt(guh.getUUID())) {      // (somebody beat it here: that one first)
                gekozen = be;
                geklopt = true;
                break;
            }
            if (gekozen == null || guh.getRandom().nextInt(3) == 0) {
                gekozen = be;
            }
        }
        if (gekozen == null) {
            data.putLong(GuhKunde.RUST, nu + 400);
            return false;
        }
        if (!test && !oefent && !geklopt && guh.getRandom().nextInt(3) != 0) {
            data.putLong(GuhKunde.RUST, nu + 200 + guh.getRandom().nextInt(400));
            return false;
        }
        kast = gekozen.getBlockPos();
        gedaan = false;
        spel = gekozen.spel();
        plek = staanplek(gekozen);
        gekozen.neem(guh.getUUID(), guh.getName(), KastBlockEntity.DEMO, nu + 500);      // (so no two guhs walk to the same one)
        return true;
    }

    private static Direction voorkant(KastBlockEntity be) {
        return be.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    /** Where a guh stands to play: in the middle of the block in front of the screen. */
    static Vec3 staanplek(KastBlockEntity be) {
        return Vec3.atBottomCenterOf(be.getBlockPos().relative(voorkant(be)));
    }

    private static boolean staanplekVrij(ServerLevel level, KastBlockEntity be) {
        BlockPos voor = be.getBlockPos().relative(voorkant(be));
        return level.getBlockState(voor).getCollisionShape(level, voor).isEmpty()
                && level.getBlockState(voor.above()).getCollisionShape(level, voor.above()).isEmpty();
    }

    @Override
    public boolean canContinueToUse() {
        return kast != null && !gedaan && !guh.isOrderedToSit() && !guh.isLeashed() && !guh.isPassenger();
    }

    @Override
    public void start() {
        fase = Fase.LOPEN;
        faseTicks = loopTicks = 0;
        GuhHooks.bezig(guh, 40);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Nullable
    private KastBlockEntity be() {
        return kast == null ? null : Guhkade.kast(guh.level(), kast);
    }

    @Override
    public void tick() {
        if (kast == null || gedaan || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        KastBlockEntity be = be();
        if (be == null || (fase != Fase.NA && !be.vrijVoor(guh.getUUID()))) {
            gedaan = true;              // (the cabinet is gone, or somebody else took the buttons)
            return;
        }
        GuhHooks.bezig(guh, 40);
        faseTicks++;
        long nu = level.getGameTime();
        Vec3 scherm = Vec3.atCenterOf(kast.above());
        switch (fase) {
            case LOPEN -> {
                if (loop()) {
                    if (be.isGeklopt(guh.getUUID())) {
                        // somebody beat its score here: a sad look at the list now (the emote takes over), practising later
                        be.zetGeklopt(guh.getUUID(), false);
                        kijk(scherm);
                        Guhkade.verdrietig(guh, null);
                        gedaan = true;
                    } else {
                        begin(level, be);
                    }
                } else if (faseTicks > 400) {
                    gedaan = true;
                } else if (faseTicks % 100 == 0) {
                    be.neem(guh.getUUID(), guh.getName(), KastBlockEntity.DEMO, nu + 500);
                }
            }
            case SPELEN -> {
                kijk(scherm);
                if (faseTicks % 28 == 7) {
                    level.playSound(null, kast, GuhkadeSlice.PIEP.get(), SoundSource.BLOCKS, 0.35f, 0.9f + guh.getRandom().nextFloat() * 0.5f);
                }
                if (nu >= eind) {
                    einde(level, be);
                }
            }
            case NA -> {
                if (faseTicks > 40) {
                    gedaan = true;
                }
            }
        }
    }

    private void zet(Fase f) {
        fase = f;
        faseTicks = 0;
    }

    /** Walks to the spot in front of the screen: true once it stands there. */
    private boolean loop() {
        double dx = guh.getX() - plek.x, dz = guh.getZ() - plek.z;
        double d2 = dx * dx + dz * dz;
        if (d2 < 0.45 * 0.45 && Math.abs(guh.getY() - plek.y) < 1.2) {
            guh.getNavigation().stop();
            return true;
        }
        var nav = guh.getNavigation();
        if (nav.isDone() || loopTicks % 20 == 0) {
            if (d2 < 1.5 * 1.5 || !nav.moveTo(plek.x, plek.y, plek.z, 1.0)) {
                guh.getMoveControl().setWantedPosition(plek.x, plek.y, plek.z, 1.0);    // (the last bit)
            }
        }
        loopTicks++;
        return false;
    }

    /** Stands still and looks at the screen. */
    private void kijk(Vec3 scherm) {
        guh.getNavigation().stop();
        guh.getLookControl().setLookAt(scherm.x, scherm.y, scherm.z);
        float yaw = (float) (Mth.atan2(scherm.z - guh.getZ(), scherm.x - guh.getX()) * Mth.RAD_TO_DEG) - 90f;
        guh.setYRot(yaw);
        guh.yBodyRot = yaw;
    }

    private void begin(ServerLevel level, KastBlockEntity be) {
        boolean test = TEST_ALTIJD.contains(guh.getUUID());
        int doel = GuhKunde.doel(spel, guh.getVariant(), guh.getPersonality(), GuhKunde.keren(guh, spel), guh.getRandom());
        if (test) {
            doel = Math.min(doel, 1);
        }
        long seed = guh.getRandom().nextLong();
        Sim.Uitkomst u = Sim.voorspel(spel, seed, doel);
        uitkomst = u.score();
        long nu = level.getGameTime();
        eind = nu + u.ticks() + 20;
        be.speelGuh(guh.getUUID(), guh.getName(), seed, doel, eind + 40);
        GuhHooks.zet(guh, PxVlaggen.SPEELT_KAST, true);
        GuhKiezer.claim(guh, GuhkadeSlice.NS);
        SPELEND.add(guh.getUUID());
        level.playSound(null, be.getBlockPos(), GuhkadeSlice.MUNTJE.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        zet(Fase.SPELEN);
    }

    /** The guh's game is over: its score on the list, and a little cheer (or a shrug). */
    private void einde(ServerLevel level, KastBlockEntity be) {
        einde(level, be, true);
    }

    private void einde(ServerLevel level, KastBlockEntity be, boolean juich) {
        UUID id = guh.getUUID();
        int vorig = be.scoreVan(id);
        // players on the list this guh passes now hear about it (they may want their place back)
        java.util.List<KastBlockEntity.Regel> voorbij = new java.util.ArrayList<>();
        for (KastBlockEntity.Regel r : be.regels()) {
            if (!r.guh() && !r.huis() && r.score() < uitkomst && r.score() >= vorig) {
                voorbij.add(r);
            }
        }
        int plaats = be.voegToe(id, guh.getName(), uitkomst, true);
        GuhKunde.gespeeld(guh, spel);
        stopSpelen();
        zet(Fase.NA);
        if (!juich) {
            return;
        }
        be.laatLos(id);
        if (plaats > 0) {
            guh.triggerAnim("action", "happy");
            level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.2, guh.getZ(), plaats == 1 ? 5 : 2, 0.25, 0.15, 0.25, 0);
            level.playSound(null, be.getBlockPos(), GuhkadeSlice.RECORD.get(), SoundSource.BLOCKS, 0.6f, 1.2f);
            if (!guh.emotes.start(Emote.VAHOEG, false, GuhEmotes.Source.SELF) && guh.areSoundsEnabled()) {
                guh.playSound(nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), 0.8f, guh.getVoicePitch());
            }
            Band.geefHartjes(guh, null, 1, Reden.SPEELGOED);
            for (KastBlockEntity.Regel r : voorbij) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(r.id());
                if (p != null) {
                    p.sendSystemMessage(Component.translatable("gui.guhs.guhkade.guh.voorbij", guh.getName(),
                            Component.translatable("gui.guhs.guhkade.spel." + spel.id), uitkomst).withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        } else {
            level.playSound(null, be.getBlockPos(), GuhkadeSlice.AF.get(), SoundSource.BLOCKS, 0.5f, 1.0f);
        }
    }

    private void stopSpelen() {
        GuhHooks.zet(guh, PxVlaggen.SPEELT_KAST, false);
        if (GuhkadeSlice.NS.equals(GuhKiezer.geclaimd(guh))) {
            GuhKiezer.los(guh);
        }
        SPELEND.remove(guh.getUUID());
    }

    @Override
    public void stop() {
        KastBlockEntity be = be();
        if (be != null && fase == Fase.SPELEN && !gedaan && vrij() && guh.level() instanceof ServerLevel level) {
            // something more important came up in the middle of its game (a little dance, a friend): the game counts, the
            // screen plays it to the end by itself and the cabinet is free again after that
            einde(level, be, false);
        } else if (be != null) {
            be.laatLos(guh.getUUID());
        }
        stopSpelen();
        kast = null;
        gedaan = false;
        fase = Fase.LOPEN;
        guh.getNavigation().stop();
        GuhHooks.bezig(guh, 0);
        if (guh.level() instanceof ServerLevel level) {
            // a guh that wants to practise is back soon; the others take their time (two to five minutes)
            int rust = GuhKunde.oefen(guh) > 0 ? 300 + guh.getRandom().nextInt(300) : 2400 + guh.getRandom().nextInt(3600);
            guh.getPersistentData().putLong(GuhKunde.RUST, level.getGameTime() + rust);
        }
    }

    /**
     * Every guh, now and then (GuhHooks.tick): a guh that still carries the playing flag or the Guhkade's claim although
     * no game of it is running (it was unloaded or picked up in the middle of one) gets rid of them.
     */
    static void ruimOp(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 100 != 0 || guh.level().isClientSide() || SPELEND.contains(guh.getUUID())) {
            return;
        }
        if (GuhHooks.heeft(guh, PxVlaggen.SPEELT_KAST)) {
            GuhHooks.zet(guh, PxVlaggen.SPEELT_KAST, false);
        }
        if (GuhkadeSlice.NS.equals(GuhKiezer.geclaimd(guh))) {
            GuhKiezer.los(guh);
        }
    }

    static void vergeet() {
        SPELEND.clear();
    }

    /** What it is doing (tests). */
    public String fase() {
        return kast == null ? "-" : fase.name();
    }
}
