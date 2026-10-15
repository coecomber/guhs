package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice geestmodellen: Fang, the spirit dragon of Roku-guh (DESIGN_EINDE2 5, Fang's Hemelpad): a long red dragon, see-
 * through and glowing, with the guh's face and ears; he hovers where he is. On his neck is one seat (the model's bone
 * {@code zadel}; {@link #ZADEL} is its spot): whoever phase B puts on him ({@code startRiding}) sits there.
 * <p>
 * Animations: {@code idle} (he hovers and his body winds), {@code praat}, {@code vlieg} (a pose: the body streams out, the
 * legs tucked), {@code buig} (he lowers his head: climb on).
 * <p>
 * phase B: {@link Fang#vlieg} puts a player on him and flies him along a path ({@link Vlucht}); a click of a player never
 * puts anyone on him. A Fang that was made for a flight ({@link Fang#TAG}) and has none (a server restart) takes itself away.
 */
public class FangEntity extends GeestWezen {
    /**
     * The seat on his neck in blocks, relative to his feet, +z = forwards (the model's bone {@code zadel}: x 0, y 34, z -40 in
     * model pixels; tools/features/einde2_geestmodellen_wezens.py FANG_ZADEL).
     */
    public static final Vec3 ZADEL = new Vec3(0.0, 34.0 / 16.0, 40.0 / 16.0);

    /** One flight: its rider, the smooth curve the rider's seat follows, what runs on landing. */
    static final class Vlucht {
        final UUID rijder;
        final List<Vec3> lijn;
        final double[] afstand;
        @Nullable
        final Consumer<ServerPlayer> daarna;
        double gevlogen;
        int tick;
        boolean landt;
        int weg = -1;

        Vlucht(UUID rijder, List<Vec3> lijn, @Nullable Consumer<ServerPlayer> daarna) {
            this.rijder = rijder;
            this.lijn = lijn;
            this.daarna = daarna;
            this.afstand = new double[lijn.size()];
            for (int i = 1; i < lijn.size(); i++) {
                afstand[i] = afstand[i - 1] + lijn.get(i).distanceTo(lijn.get(i - 1));
            }
        }

        double lengte() {
            return afstand[afstand.length - 1];
        }

        /** The point at this distance along the curve. */
        Vec3 op(double d) {
            if (d <= 0) {
                return lijn.get(0);
            }
            for (int i = 1; i < lijn.size(); i++) {
                if (afstand[i] >= d) {
                    double f = (d - afstand[i - 1]) / Math.max(1e-6, afstand[i] - afstand[i - 1]);
                    return lijn.get(i - 1).lerp(lijn.get(i), f);
                }
            }
            return lijn.get(lijn.size() - 1);
        }
    }

    @Nullable
    Vlucht vlucht;

    public FangEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Is he flying someone right now (server)? */
    public boolean isOpVlucht() {
        return vlucht != null && !vlucht.landt;
    }

    /** Puts the seat at this point, Fang turned along {@code richting}. */
    private void zetZadelOp(Vec3 punt, Vec3 richting) {
        float yaw = richting.horizontalDistanceSqr() > 1e-6 ? hoek(richting.x, richting.z) : getYRot();
        yaw = Mth.rotLerp(0.25f, getYRot(), yaw);                  // (he turns smoothly)
        Vec3 plek = punt.subtract(ZADEL.scale(getScale()).yRot(-yaw * Mth.DEG_TO_RAD));
        setPos(plek.x, plek.y, plek.z);
        setYRot(yaw);
        yBodyRot = yaw;
        yHeadRot = yaw;
    }

    /** Lands: the rider stands on the last point, Fang bows and is gone a little later; daarna runs once. */
    private void land(@Nullable ServerPlayer rijder) {
        Vlucht v = vlucht;
        v.landt = true;
        v.weg = tickCount + 50;
        Fang.VLIEGT.remove(v.rijder);
        zetAnimatie("idle");
        zetAnimatie("buig");
        if (rijder != null) {
            Vec3 eind = v.lijn.get(v.lijn.size() - 1);
            rijder.stopRiding();
            rijder.teleportTo(eind.x, eind.y, eind.z);
            rijder.resetFallDistance();
            if (v.daarna != null) {
                v.daarna.accept(rijder);
            }
        }
    }

    /** The flight ends at once (logout, death, another dimension): the rider at the last point, no daarna. */
    void breekAf(ServerPlayer rijder) {
        Vlucht v = vlucht;
        vlucht = null;
        if (v != null && rijder.getVehicle() == this) {
            Vec3 eind = v.lijn.get(v.lijn.size() - 1);
            rijder.stopRiding();
            if (rijder.level() == level()) {
                rijder.teleportTo(eind.x, eind.y, eind.z);
            }
            rijder.resetFallDistance();
        }
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (vlucht == null) {
            if (entityTags().contains(Fang.TAG)) {
                discard();                                           // (a leftover of a flight: never saved, never stays)
            }
            return;
        }
        Vlucht v = vlucht;
        if (v.landt) {
            if (tickCount >= v.weg) {
                vlucht = null;
                discard();
            }
            return;
        }
        ServerPlayer rijder = level().getPlayerByUUID(v.rijder) instanceof ServerPlayer sp ? sp : null;
        if (rijder == null || !rijder.isAlive()) {
            Fang.VLIEGT.remove(v.rijder);
            vlucht = null;
            discard();
            return;
        }
        if (rijder.getVehicle() != this) {
            land(rijder);                                            // (got off somehow: he lands at once)
            return;
        }
        rijder.resetFallDistance();
        v.tick++;
        if (v.tick <= Fang.OPSTIJG) {
            Vec3 start = v.op(0);
            zetZadelOp(start.add(0, 0.06 * v.tick, 0), v.op(2.0).subtract(start));
            return;
        }
        double over = v.lengte() - v.gevlogen;
        double snel = Fang.SNEL * Math.min(1.0, (v.tick - Fang.OPSTIJG) / 30.0 + 0.15) * Math.min(1.0, over / 6.0 + 0.2);
        v.gevlogen = Math.min(v.lengte(), v.gevlogen + snel);
        double hoog = Math.max(0, 1.2 - (v.tick - Fang.OPSTIJG) * 0.04);  // (the climb of the start fades out)
        Vec3 punt = v.op(v.gevlogen).add(0, hoog, 0);
        zetZadelOp(punt, v.op(v.gevlogen + 3.0).subtract(v.op(Math.max(0, v.gevlogen - 1.0))));
        if (v.gevlogen >= v.lengte() - 1e-3) {
            land(rijder);
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return vlucht == null && !entityTags().contains(Fang.TAG) && super.shouldBeSaved();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return ZADEL.scale(scale).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    /** Whoever rides him turns along with him. */
    @Override
    protected void positionRider(Entity passenger, MoveFunction callback) {
        super.positionRider(passenger, callback);
        passenger.setYBodyRot(getYRot());
    }
}
