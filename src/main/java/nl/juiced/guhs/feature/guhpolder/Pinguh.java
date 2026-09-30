package nl.juiced.guhs.feature.guhpolder;

import java.util.Locale;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.world.ModDimensions;

/**
 * De Pinguh: a guh in a penguin suit, only born in the Guhpolder. Out of the plain wild guhs born there, {@link #KANS}
 * become one (once per guh, like the Pluisguh of the Knuffeldal). Three looks ({@link Look}), decided by its UUID, so the
 * server and every client agree without extra data:
 * <ul>
 *   <li>{@link Look#KLASSIEK}: black back, head and guh ears, a white belly and a white face around its big guh eyes, an
 *       orange beak-snoet with pink blush, black flippers, orange feet;</li>
 *   <li>{@link Look#KEIZER}: the same plus yellow-orange cheek patches near the ears; a bit bigger and statelier
 *       ({@link #KEIZER_GROEI});</li>
 *   <li>{@link Look#PLUIS}: the grey fluffy chick look: every Pinguh baby, and now and then ({@link #PLUIS_PROCENT} %) a
 *       grown-up that never lost its fluff.</li>
 * </ul>
 * It waddles when it walks and belly-slides on ice ({@link GuhpolderFeature#GLIJIJS}): faster, with a trail of snow
 * glitter (drawn by client.PinguhRender). A tamed Pinguh slides along with a skating player: {@link PinguhMeeglijden}.
 * A guh with the entity tag {@link #GEEN_PINGUH} (e.g. a structure's audience that must stay a plain guh) never becomes one.
 */
public final class Pinguh {
    /** Out of the plain wild guhs born in the Guhpolder, this many become a Pinguh. */
    public static final float KANS = 0.5f;
    /** Out of 100 grown-up Pinguhs, this many keep the grey fluffy chick look. */
    public static final int PLUIS_PROCENT = 8;
    /** A keizer is this much bigger than it would have been (once, when it is grown up). */
    public static final float KEIZER_GROEI = 1.18f;
    /** Entity tag: this guh never becomes a Pinguh. */
    public static final String GEEN_PINGUH = "guhs_geen_pinguh";
    static final String CHECKED = "guhs_guhpolder_checked";
    static final String GEGROEID = "guhs_guhpolder_keizer_gegroeid";
    /** The belly-slide on ice: this much faster (on top of the ice's own slipperiness). */
    public static final double GLIJ_BOOST = 0.6;
    static final ResourceLocation GLIJ_ID = Guhs.id("guhpolder_buikglij");

    public enum Look {
        KLASSIEK, KEIZER, PLUIS;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private Pinguh() {
    }

    // --- the looks ------------------------------------------------------------------------------------------------------------

    public static Look look(GuhEntity guh) {
        return look(guh.getUUID(), guh.isBaby());
    }

    /** The look of a Pinguh with this UUID: babies are always fluffy; grown-ups klassiek or keizer (50/50), a few stay fluffy. */
    public static Look look(UUID id, boolean baby) {
        if (baby) {
            return Look.PLUIS;
        }
        long h = id.getMostSignificantBits() * 31 + id.getLeastSignificantBits();
        h ^= h >>> 29;
        h *= 0x9E3779B97F4A7C15L;
        h ^= h >>> 32;
        if (Math.floorMod(h, 100L) < PLUIS_PROCENT) {
            return Look.PLUIS;
        }
        return ((h >>> 40) & 1L) == 0 ? Look.KLASSIEK : Look.KEIZER;
    }

    // --- being born in the polder ---------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof GuhEntity guh) {
            maybePinguh(level, guh);
        }
    }

    /** Decides (once per guh) whether a new wild guh in the Guhpolder becomes a Pinguh. Returns true when it did. */
    public static boolean maybePinguh(ServerLevel level, GuhEntity guh) {
        if (guh.getClass() != GuhEntity.class || guh.getPersistentData().getBoolean(CHECKED)) {
            return false;
        }
        return decide(guh, level.dimension() == ModDimensions.GUHMENSION && inPolder(level, guh.blockPosition()));
    }

    /** The decision itself (once per guh): in the Guhpolder, a plain wild guh may become a Pinguh. */
    public static boolean decide(GuhEntity guh, boolean inPolder) {
        CompoundTag data = guh.getPersistentData();
        if (data.getBoolean(CHECKED)) {
            return false;
        }
        data.putBoolean(CHECKED, true);
        if (!inPolder || guh.isTame() || guh.hasCustomName() || guh.getVariant() != GuhVariant.NORMAL || GuhHooks.isBewoner(guh)
                || guh.getTags().contains(GEEN_PINGUH)) {
            return false;
        }
        if (guh.getRandom().nextFloat() < KANS) {
            guh.setVariant(GuhVariant.PINGUH);
            groei(guh);
            return true;
        }
        return false;
    }

    public static boolean inPolder(Level level, BlockPos pos) {
        return level.getBiome(pos).is(GuhpolderFeature.GUHPOLDER);
    }

    /** A grown-up keizer gets a bit bigger (once). */
    static void groei(GuhEntity guh) {
        if (guh.getVariant() != GuhVariant.PINGUH || guh.isBaby() || look(guh) != Look.KEIZER || guh.getPersistentData().getBoolean(GEGROEID)) {
            return;
        }
        guh.getPersistentData().putBoolean(GEGROEID, true);
        guh.setGuhScale(Math.min(GuhEntity.MAX_SCALE, Math.max(guh.getGuhScale(), 0.85f) * KEIZER_GROEI));
    }

    // --- waddling and sliding (server: the speed on ice, the glitter and the sound) ---------------------------------------------

    static void hooks() {
        GuhHooks.tick(Pinguh::tick);
    }

    /** Is this guh standing on slide ice? */
    public static boolean opIJs(GuhEntity guh) {
        return guh.onGround() && guh.level().getBlockState(guh.getOnPos()).is(GuhpolderFeature.GLIJIJS);
    }

    static void tick(GuhEntity guh) {
        if (guh.getVariant() != GuhVariant.PINGUH || (guh.tickCount + guh.getId()) % 4 != 0 || !(guh.level() instanceof ServerLevel level)) {
            return;
        }
        if (guh.tickCount % 200 == 0) {
            groei(guh);   // (a keizer chick that just grew up)
        }
        boolean ijs = opIJs(guh);
        AttributeInstance speed = guh.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            boolean heeft = speed.hasModifier(GLIJ_ID);
            if (ijs && !heeft) {
                speed.addTransientModifier(new AttributeModifier(GLIJ_ID, GLIJ_BOOST, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            } else if (!ijs && heeft) {
                speed.removeModifier(GLIJ_ID);
            }
        }
        double v = guh.getDeltaMovement().horizontalDistance();
        if (ijs && v > 0.08) {
            // a belly-slide: snow dust and frost glitter behind it, and now and then a happy "wiiie"
            level.sendParticles(ParticleTypes.SNOWFLAKE, guh.getX(), guh.getY() + 0.1, guh.getZ(), 2, 0.15, 0.02, 0.15, 0.01);
            if (level.random.nextInt(3) == 0) {
                level.sendParticles(GuhpolderFeature.GLINSTER.get(), guh.getX(), guh.getY() + 0.2, guh.getZ(), 1, 0.2, 0.05, 0.2, 0);
            }
            if (level.random.nextInt(45) == 0) {
                level.playSound(null, guh.blockPosition(), GuhpolderFeature.PINGUH_GLIJ.get(), SoundSource.NEUTRAL, 0.6f,
                        1.3f + level.random.nextFloat() * 0.3f);
            }
        }
    }
}
