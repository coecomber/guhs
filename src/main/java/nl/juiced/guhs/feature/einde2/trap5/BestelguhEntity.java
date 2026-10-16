package nl.juiced.guhs.feature.einde2.trap5;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * The Bestelguh ({@code guhs:bestelguh}): the guh with the little apron at a {@link Bestelbalie} (the model of the Kind
 * {@code BESTELGUH}). It belongs to its balie: the balie makes it, tells it where to run while an order is busy
 * ({@link #ren}) and lets it show its receipt ({@link #bonnetje}); without its balie it is gone. It is never saved (the balie
 * makes a new one after a load), can't be hurt, pushed or leashed (a GuhNpcEntity), and a click opens the order list.
 */
public class BestelguhEntity extends GuhNpcEntity {
    @Nullable
    private BlockPos balie;
    @Nullable
    private BlockPos doel;
    private int geenBalie;

    public BestelguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setKind(Kind.BESTELGUH);
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.32);
        }
    }

    void zetBalie(BlockPos balie) {
        this.balie = balie.immutable();
    }

    @Nullable
    public BlockPos balie() {
        return balie;
    }

    /** Run to this spot (next to a machine), or back to the balie (null). */
    void ren(@Nullable BlockPos naar) {
        BlockPos nieuw = naar == null ? balie : naar;
        if (nieuw == null || nieuw.equals(doel)) {
            return;
        }
        doel = nieuw;
        getNavigation().moveTo(nieuw.getX() + 0.5, nieuw.getY(), nieuw.getZ() + 0.5, 1.2);
        speel("ren");
    }

    /** The order is done: it shows its receipt. */
    void bonnetje() {
        speel("bonnetje");
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        if (balie == null || !(server.isLoaded(balie) && server.getBlockEntity(balie) instanceof Bestelbalie.BlockEntityImpl)) {
            if (++geenBalie > 40) {
                discard();
            }
            return;
        }
        geenBalie = 0;
        if (doel != null) {
            double d = distanceToSqr(Vec3.atBottomCenterOf(doel));
            if (d < 2.5) {
                getNavigation().stop();
                if (!speelt().isEmpty() && speelt().equals("ren")) {
                    speel("");
                }
                if (doel.equals(balie)) {
                    doel = null;
                }
            } else if (getNavigation().isDone() && tickCount % 20 == 0) {
                // the path is blocked (a machine behind a wall): it hops there, it is a quick guh
                if (d > 400) {
                    snapTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, getYRot(), 0);
                } else {
                    getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 1.2);
                }
            }
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sp && balie != null && level().getBlockEntity(balie) instanceof Bestelbalie.BlockEntityImpl b) {
            b.openLijst(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
