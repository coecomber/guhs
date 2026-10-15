package nl.juiced.guhs.feature.einde2.geestwezens;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice geestmodellen: Tui and La as spirits (DESIGN_EINDE2 5, De Hemelvijver): two huge, glowing, see-through koi of
 * about six blocks that swim through the AIR. {@code Soort} 0 = Tui, white with a black spot; 1 = La, black with a white spot.
 * They circle each other like the two fish of the Geestenoase (cast_schurk's {@code CastVisEntity}): a koi that finds one of
 * the other look within {@link #ZOEK} blocks takes the midpoint of the two as its centre and swims a ring around it, half a turn
 * from the other (yin and yang); alone it circles the spot it was put. The radius is half their distance, at most
 * {@link #MAX_STRAAL}; the height sways gently around where it was put. They pass through everything (spirits) and with
 * {@code stil} they hang still.
 */
public class GeestKoiEntity extends GeestWezen {
    static final double ZOEK = 24.0, MAX_STRAAL = 9.0, ALLEEN_STRAAL = 5.0;
    /** Radians a tick along the ring, and the most it moves in one tick. */
    static final double HOEKSNELHEID = 0.018, STAP = 0.3;
    /** How far up and down it sways (blocks), and how fast (radians a tick). */
    static final double GOLF = 0.6, GOLF_SNELHEID = 0.025;

    @Nullable
    private Vec3 thuis;
    private boolean zwemt;

    public GeestKoiEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    /** The koi of the other look (its partner), or null. */
    @Nullable
    public GeestKoiEntity partner() {
        GeestKoiEntity beste = null;
        double bd = ZOEK * ZOEK;
        for (GeestKoiEntity k : level().getEntitiesOfClass(GeestKoiEntity.class, getBoundingBox().inflate(ZOEK))) {
            double d = k.distanceToSqr(this);
            if (k != this && k.soort() != soort() && d <= bd) {
                beste = k;
                bd = d;
            }
        }
        return beste;
    }

    @Nullable
    public Vec3 thuis() {
        return thuis;
    }

    @Override
    public void tick() {
        noPhysics = true;
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (thuis == null) {
            thuis = position();
        }
        zwemt = false;
        setDeltaMovement(Vec3.ZERO);
        if (stil() || isNoAi()) {
            return;
        }
        GeestKoiEntity ander = partner();
        Vec3 midden;
        double straal;
        if (ander != null) {
            midden = position().add(ander.position()).scale(0.5);
            double dx = getX() - ander.getX(), dz = getZ() - ander.getZ();
            straal = Math.min(MAX_STRAAL, Math.sqrt(dx * dx + dz * dz) / 2);
        } else {
            midden = thuis;
            straal = ALLEEN_STRAAL;
        }
        double hoek = Math.atan2(getZ() - midden.z, getX() - midden.x) + HOEKSNELHEID;
        double hoogte = thuis.y + Math.sin(level().getGameTime() * GOLF_SNELHEID + soort() * Math.PI) * GOLF;
        Vec3 doel = new Vec3(midden.x + Math.cos(hoek) * straal, hoogte, midden.z + Math.sin(hoek) * straal);
        Vec3 stap = doel.subtract(position());
        if (stap.length() > STAP) {
            stap = stap.normalize().scale(STAP);
        }
        if (stap.lengthSqr() > 1e-6) {
            setPos(getX() + stap.x, getY() + stap.y, getZ() + stap.z);
            kijkLangs(stap);
            zwemt = true;
        }
    }

    /** Tui and La have names of their own (by look). */
    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.guhs.geest_koi." + soort());
    }

    /** It swims while it moves along its ring. */
    @Override
    protected boolean isBezig() {
        return zwemt || !stil() && !isNoAi();
    }
}
