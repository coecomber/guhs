package nl.juiced.guhs.feature.bouwhulp.keet;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * Bouwguhs (design 2.2, B13): from Bouwkeet level 1 you name at most four of your own guhs near the keet as helpers. They
 * put on a builder's helmet ({@link GuhClothes#BUILDER_HELMET}; their own hat comes back when they stop), walk around the
 * project to the spots that are being built, whistle (notes) and now and then tell you how far it is ("njeg, nog 40
 * blokjes!"). Each one that is near the keet makes it build {@link BouwkeetBlockEntity#PER_GUH} blocks a second faster.
 * When the project is done they cheer.
 * <p>
 * A guh remembers its keet in its persistent data ({@link #KEET}); when the keet is gone, or no longer lists it, it takes the
 * helmet off by itself.
 */
public final class Bouwguhs {
    public static final String KEET = "guhs_bouwhulp_keet", HOOFD = "guhs_bouwhulp_hoofd";
    /** How far from the keet a bouwguh still counts (and may be named). */
    public static final int BEREIK = 32, KIES_BEREIK = 24;
    private static final int PRAAT_ELKE = 30;

    public static void register() {
        GuhHooks.doelen((guh, goals) -> goals.addGoal(4, new BouwDoel(guh)));
        GuhHooks.tick(Bouwguhs::tickGuh);
    }

    // =====================================================================================================================
    // naming and letting go
    // =====================================================================================================================

    /** The keet a guh works for, or null. */
    @Nullable
    public static BlockPos keetVan(GuhEntity guh) {
        return guh.getPersistentData().contains(KEET) ? BlockPos.of(guh.getPersistentData().getLongOr(KEET, 0L)) : null;
    }

    /** The own tame guhs of this player near the keet (what the Bouwguhs tab lists). */
    public static List<GuhEntity> kandidaten(ServerLevel level, BouwkeetBlockEntity keet, ServerPlayer p) {
        BlockPos k = keet.getBlockPos();
        return level.getEntitiesOfClass(GuhEntity.class, new AABB(k).inflate(KIES_BEREIK),
                g -> g.isAlive() && g.isTame() && p.getUUID().equals(g.getOwnerUUID()));
    }

    /**
     * Names a guh (or lets it go when it was named). False: not allowed (not the player's guh, too far, already four, the
     * keet is below level 1).
     */
    public static boolean wissel(ServerLevel level, BouwkeetBlockEntity keet, ServerPlayer p, GuhEntity guh) {
        List<UUID> lijst = new ArrayList<>(keet.bouwguhs());
        if (lijst.remove(guh.getUUID())) {
            keet.zetBouwguhs(lijst);
            laatGaan(guh);
            return true;
        }
        if (!keet.niveau().bouwguhs() || lijst.size() >= BouwkeetBlockEntity.MAX_BOUWGUHS || !p.getUUID().equals(guh.getOwnerUUID())
                || !guh.blockPosition().closerThan(keet.getBlockPos(), KIES_BEREIK)) {
            return false;
        }
        BlockPos oud = keetVan(guh);
        if (oud != null && !oud.equals(keet.getBlockPos()) && level.getBlockEntity(oud) instanceof BouwkeetBlockEntity ander) {
            List<UUID> l = new ArrayList<>(ander.bouwguhs());
            l.remove(guh.getUUID());
            ander.zetBouwguhs(l);
        }
        lijst.add(guh.getUUID());
        keet.zetBouwguhs(lijst);
        neemAan(guh, keet.getBlockPos());
        return true;
    }

    static void neemAan(GuhEntity guh, BlockPos keet) {
        if (!guh.getPersistentData().contains(HOOFD)) {
            GuhClothes hoofd = guh.getClothes(GuhClothes.Slot.HEAD);
            guh.getPersistentData().putInt(HOOFD, hoofd == null ? -1 : hoofd.ordinal());
        }
        guh.getPersistentData().putLong(KEET, keet.asLong());
        guh.wear(GuhClothes.BUILDER_HELMET);
        guh.setPersistenceRequired();
        if (guh.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, guh.getX(), guh.getY() + 1, guh.getZ(), 8, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /** The guh stops being a bouwguh: helmet off, its own hat back on. */
    public static void laatGaan(GuhEntity guh) {
        if (!guh.getPersistentData().contains(KEET)) {
            return;
        }
        guh.getPersistentData().remove(KEET);
        int hoofd = guh.getPersistentData().getIntOr(HOOFD, -1);
        guh.getPersistentData().remove(HOOFD);
        if (guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.BUILDER_HELMET) {
            GuhClothes oud = hoofd >= 0 && hoofd < GuhClothes.values().length ? GuhClothes.values()[hoofd] : null;
            if (oud != null) {
                guh.wear(oud);
            } else {
                guh.takeOff(GuhClothes.Slot.HEAD);
            }
        }
    }

    /** The keet block went away: its guhs notice it themselves ({@link #tickGuh}); nothing to keep. */
    static void keetWeg(ServerLevel level, BlockPos pos) {
        for (GuhEntity g : level.getEntitiesOfClass(GuhEntity.class, new AABB(pos).inflate(BEREIK), g -> pos.equals(keetVan(g)))) {
            laatGaan(g);
        }
    }

    // =====================================================================================================================
    // while building
    // =====================================================================================================================

    /** The keet's guhs that are here now (loaded, alive, near the keet, still its guhs). */
    static List<GuhEntity> hier(ServerLevel level, BouwkeetBlockEntity keet) {
        List<GuhEntity> uit = new ArrayList<>();
        for (UUID id : keet.bouwguhs()) {
            Entity e = level.getEntity(id);
            if (e instanceof GuhEntity g && g.isAlive() && keet.getBlockPos().equals(keetVan(g))
                    && g.blockPosition().closerThan(keet.getBlockPos(), BEREIK)) {
                uit.add(g);
            }
        }
        return uit;
    }

    static int aanwezig(ServerLevel level, BouwkeetBlockEntity keet) {
        return hier(level, keet).size();
    }

    /** Once a second per keet: notes over the working guhs, now and then a line for the owner. */
    static void tickKeet(ServerLevel level, BouwkeetBlockEntity keet, @Nullable Spook s) {
        if (s == null || s.status() != Spook.Status.BOUWT || !keet.werkNiveau().bouwguhs()) {
            return;
        }
        List<GuhEntity> guhs = hier(level, keet);
        for (GuhEntity g : guhs) {
            if (level.getRandom().nextInt(3) == 0) {
                level.sendParticles(ParticleTypes.NOTE, g.getX(), g.getY() + g.getBbHeight() + 0.4, g.getZ(), 1, 0.2, 0.1, 0.2,
                        level.getRandom().nextDouble());
                GuhHooks.bezig(g, 60);
            }
        }
        if (guhs.isEmpty() || level.getRandom().nextInt(PRAAT_ELKE) != 0) {
            return;
        }
        GuhEntity prater = guhs.get(level.getRandom().nextInt(guhs.size()));
        ServerPlayer baas = prater.getOwnerUUID() == null ? null : level.getServer().getPlayerList().getPlayer(prater.getOwnerUUID());
        if (baas != null && baas.level() == level && baas.blockPosition().closerThan(keet.getBlockPos(), 48)) {
            int regel = level.getRandom().nextInt(4);
            baas.sendOverlayMessage(Component.translatable("gui.guhs.bouwguh.praat." + regel, prater.getDisplayName(), s.nogTeGaan()));
        }
    }

    /** The project is done: the bouwguhs cheer. */
    static void juich(ServerLevel level, BouwkeetBlockEntity keet) {
        for (GuhEntity g : hier(level, keet)) {
            level.sendParticles(ParticleTypes.HEART, g.getX(), g.getY() + g.getBbHeight() + 0.3, g.getZ(), 4, 0.3, 0.2, 0.3, 0.05);
            g.getJumpControl().jump();
            level.playSound(null, g.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.6f, 1.4f + level.getRandom().nextFloat() * 0.4f);
        }
    }

    /** Every guh, now and then: a bouwguh whose keet is gone or no longer lists it lets go. */
    private static void tickGuh(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 100 != 0 || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos k = keetVan(guh);
        if (k == null || !level.isLoaded(k)) {
            return;
        }
        if (!(level.getBlockEntity(k) instanceof BouwkeetBlockEntity keet) || !keet.bouwguhs().contains(guh.getUUID())) {
            laatGaan(guh);
        }
    }

    // =====================================================================================================================
    // the goal: walk along the building spots
    // =====================================================================================================================

    static final class BouwDoel extends Goal {
        private final GuhEntity guh;
        @Nullable
        private BlockPos doel;
        private int tijd;

        BouwDoel(GuhEntity guh) {
            this.guh = guh;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private BouwkeetBlockEntity keet() {
            BlockPos k = keetVan(guh);
            if (k == null || !(guh.level() instanceof ServerLevel level) || !level.isLoaded(k)) {
                return null;
            }
            return level.getBlockEntity(k) instanceof BouwkeetBlockEntity keet && keet.bouwt() && keet.werkNiveau().bouwguhs() ? keet : null;
        }

        @Override
        public boolean canUse() {
            if (guh.tickCount % 20 != 0 || guh.isOrderedToSit() || guh.isPassenger() || keetVan(guh) == null) {
                return false;
            }
            BouwkeetBlockEntity keet = keet();
            Spook s = keet == null ? null : keet.project();
            if (s == null) {
                return false;
            }
            List<BlockPos> plekken = s.nogTeDoen(8);
            if (plekken.isEmpty()) {
                return false;
            }
            doel = plekken.get(guh.getRandom().nextInt(plekken.size()));
            return guh.blockPosition().closerThan(keet.getBlockPos(), BEREIK);
        }

        @Override
        public boolean canContinueToUse() {
            return doel != null && tijd > 0 && keet() != null && !guh.isOrderedToSit();
        }

        @Override
        public void start() {
            tijd = 60 + guh.getRandom().nextInt(60);
            if (doel != null) {
                guh.getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public void tick() {
            tijd--;
            if (doel == null) {
                return;
            }
            guh.getLookControl().setLookAt(doel.getX() + 0.5, doel.getY() + 0.5, doel.getZ() + 0.5);
            if (guh.blockPosition().closerThan(doel, 3) && tijd % 10 == 0) {
                guh.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }

        @Override
        public void stop() {
            doel = null;
            guh.getNavigation().stop();
        }
    }

    private Bouwguhs() {
    }
}
