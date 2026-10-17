package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * einde2, slice geestmodellen: Koh de Gezichtenrover (DESIGN_EINDE2 5, Koh's Grot): a giant centipede spirit of about 12 blocks,
 * the front of his body reared up, and on his head the face he wears now ({@code Soort}, one texture each):
 * {@link #MASKER} a pale mask, {@link #GUH} a guh's face, {@link #MIKA} a Mika's face, {@link #LACH} a laughing face,
 * {@link #VERDRIET} a sad face, {@link #BLANCO} a blank face. He stays in his cave (no strolling) and watches whoever comes in.
 * <p>
 * Animations: {@code idle} (the front sways, the legs ripple), {@code praat}, {@code kruip} (a pose: flat on the floor, the
 * body winds), {@code wissel} (the head whips round and the face changes: {@link #wissel}), {@code steel} (he lunges at you
 * and freezes, nose to nose).
 * <p>
 * hook fase B: Koh's test ("toon geen emotie": no moving, jumping or clicking while he talks; failing = your face is gone
 * for a moment and you are back at the entrance, never any harm) and the riddle round for the mask are the story's. It uses
 * {@link #wissel}, {@code zetAnimatie("steel")}, {@link GeestWezen#BIJ_KLIK} and the faces; nothing of it lives here.
 */
public class KohEntity extends GeestWezen {
    public static final int MASKER = 0, GUH = 1, MIKA = 2, LACH = 3, VERDRIET = 4, BLANCO = 5;
    /** Ticks into "wissel" at which the new face shows (the head is turned away then). */
    static final int WISSEL_MIDDEN = 9;

    /** Server: the face that comes at {@link #wisselOp} (-1: none waiting). */
    private int nieuwGezicht = -1;
    private long wisselOp;
    /** einde2 fix3-h2: server, during a test: he faces this player squarely and looks nowhere else (null: free). */
    @Nullable
    private UUID strak;

    public KohEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Plays "wissel" and puts on this face halfway through (while the head is turned away). */
    public void wissel(int gezicht) {
        if (level().isClientSide()) {
            return;
        }
        zetAnimatie("wissel");
        nieuwGezicht = Math.floorMod(gezicht, wezen.soorten);
        wisselOp = level().getGameTime() + WISSEL_MIDDEN;
    }

    /**
     * einde2 fix3-h2 (playtest 2: "tijdens de test was hij 90 graden gedraaid ... ik zag zijn veranderde hoofden helemaal niet"):
     * while somebody takes his test he stands still and faces that player, body and head, every tick; no looking round.
     * {@code null} lets him go again.
     */
    public void kijkStrak(@Nullable Entity wie) {
        strak = wie == null ? null : wie.getUUID();
        if (wie != null) {
            zetStil(true);
            richtOp(wie);
        }
    }

    /** Is he facing somebody for a test now? */
    public boolean kijktStrak() {
        return strak != null;
    }

    private void richtOp(Entity wie) {
        double dx = wie.getX() - getX(), dz = wie.getZ() - getZ();
        if (dx * dx + dz * dz < 1.0e-4) {
            return;
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        setYRot(yaw);
        yRotO = yaw;
        setYBodyRot(yaw);
        yBodyRotO = yaw;
        setYHeadRot(yaw);
        yHeadRotO = yaw;
        setXRot(0f);
        getNavigation().stop();
    }

    @Override
    protected boolean kijktNaarSpelers() {
        return strak == null && super.kijktNaarSpelers();
    }

    /** The face that he wears now (the same as {@link #soort()}). */
    public int gezicht() {
        return soort();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && strak != null) {
            Entity wie = level() instanceof net.minecraft.server.level.ServerLevel sl ? sl.getEntity(strak) : null;
            if (wie == null || !wie.isAlive() || wie.distanceToSqr(this) > 48 * 48) {
                strak = null;
            } else {
                richtOp(wie);
            }
        }
        if (!level().isClientSide() && nieuwGezicht >= 0 && level().getGameTime() >= wisselOp) {
            zetSoort(nieuwGezicht);
            nieuwGezicht = -1;
        }
    }

    /** A new Koh wears his own pale mask, not a random face. */
    @Override
    protected boolean soortGezet() {
        if (!super.soortGezet() && !level().isClientSide()) {
            super.zetSoort(MASKER);
        }
        return true;
    }
}
