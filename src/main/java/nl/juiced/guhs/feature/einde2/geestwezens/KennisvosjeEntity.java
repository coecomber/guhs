package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * einde2 phase B, slice geest_modellen: a kennisvosje (DESIGN_EINDE2 5 "Nieuwe wezens"): a spirit fox with round guh ears,
 * family of Wan Shi Guh's foxes, a scroll on its back. Soort 0 silver-blue, 1 the rare golden one (geest_spel's sighting).
 * It strolls round its Honk; once someone gives it a goal ({@link #zetDoel}) it LEADS: when a player is within
 * {@link #VOLG} blocks it walks there, waits and looks back ({@code snuffel}) when the player falls behind more than
 * {@link #ACHTER} blocks, and at the goal it points ({@code wijs}), tells {@link #BIJ_DOEL} once (the nearest player within
 * {@link #BIJ} blocks), waits there {@link #WACHT} ticks and goes home. The goal is saved (NBT {@code DoelX/Y/Z}).
 * <p>
 * hook fase B: geest_spel decides the goals (the secrets) and what happens there ({@link #BIJ_DOEL}).
 */
public class KennisvosjeEntity extends GeestWezen {
    static final double VOLG = 10.0, ACHTER = 12.0, BIJ = 6.0, SNEL = 0.8;
    static final int WACHT = 200;
    /** Told when a fox reaches its goal with a player near (the first of this list that cares does its thing). */
    public static final List<BiConsumer<KennisvosjeEntity, ServerPlayer>> BIJ_DOEL = new CopyOnWriteArrayList<>();

    @Nullable
    private BlockPos doel;
    private int aangekomen = -1;
    private int snuffelOver;

    public KennisvosjeEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Lead the next player who comes close to this spot (null: stop leading). */
    public void zetDoel(@Nullable BlockPos pos) {
        doel = pos == null ? null : pos.immutable();
        aangekomen = -1;
        getNavigation().stop();
    }

    @Nullable
    public BlockPos doel() {
        return doel;
    }

    /** Is it standing at its goal right now? */
    public boolean isAangekomen() {
        return aangekomen >= 0;
    }

    @Override
    protected boolean magLopen() {
        return doel == null && super.magLopen();                     // (leading: no strolling)
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || doel == null || isNoAi() || stil()) {
            return;
        }
        Vec3 bij = Vec3.atBottomCenterOf(doel);
        Player speler = level().getNearestPlayer(this, VOLG + ACHTER);
        if (aangekomen >= 0) {
            if (++aangekomen > WACHT) {
                zetDoel(null);                                       // (it walks home along its Honk)
            }
            return;
        }
        if (position().distanceToSqr(bij) < 2.25) {
            getNavigation().stop();
            aangekomen = 0;
            zetAnimatie("wijs");
            Player bijDoel = level().getNearestPlayer(this, BIJ);
            if (bijDoel instanceof ServerPlayer sp && !sp.isSpectator()) {
                for (BiConsumer<KennisvosjeEntity, ServerPlayer> l : BIJ_DOEL) {
                    l.accept(this, sp);
                }
            }
            return;
        }
        if (speler == null || speler.isSpectator()) {
            getNavigation().stop();                                  // (nobody to lead: it waits)
            return;
        }
        if (distanceTo(speler) > ACHTER) {
            getNavigation().stop();
            getLookControl().setLookAt(speler);
            if (--snuffelOver <= 0) {
                zetAnimatie("snuffel");                              // (it waits and looks back)
                snuffelOver = 60;
            }
            return;
        }
        if (getNavigation().isDone() || tickCount % 20 == 0) {
            getNavigation().moveTo(bij.x, bij.y, bij.z, SNEL);
        }
    }

    @Override
    protected boolean isBezig() {
        return doel != null && !getNavigation().isDone();
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (doel != null) {
            tag.putInt("DoelX", doel.getX());
            tag.putInt("DoelY", doel.getY());
            tag.putInt("DoelZ", doel.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        int y = tag.getIntOr("DoelY", Integer.MIN_VALUE);
        doel = y == Integer.MIN_VALUE ? null : new BlockPos(tag.getIntOr("DoelX", 0), y, tag.getIntOr("DoelZ", 0));
    }
}
