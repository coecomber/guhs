package nl.juiced.guhs.feature.balto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VariantGedrag;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;

/**
 * The tamed Baltoguh (VariantGedrag of GuhVariant.BALTOGUH):
 * <ul>
 *   <li>fast in the snow: on snow, snowy grass, ice or the Nomguh track he walks {@link #SNEEUW_EXTRA} faster (a transient
 *       speed modifier) and, ridden, runs {@link #RIJ_EXTRA} x on top of that; little snow puffs fly from his paws;</li>
 *   <li>"Snuffel!" (the guh-menu button): he sniffs the way to your last huisje (his own home first), or else your spawn
 *       point: nose down, a sniff, and a trail of glowing paw prints running from him towards it (only you see it), plus a
 *       line with the distance and the direction. The SNUFFELT flag shows the pose on the client
 *       (client.BaltoClient).</li>
 * </ul>
 */
public class BaltoGedrag implements VariantGedrag {
    public static final ResourceLocation SNEEUW_MODIFIER = Guhs.id("balto_sneeuwsnel");
    /** Extra walking speed on snow (x the base speed). */
    public static final double SNEEUW_EXTRA = 0.45;
    /** Ridden on snow: this factor on top. */
    public static final float RIJ_EXTRA = 1.3f;
    /** How long the sniffing (pose + trail) lasts. */
    public static final int SNUFFEL_TICKS = 120;
    /** The trail of paw prints reaches this far from him. */
    public static final int SPOOR_LENGTE = 26;

    /** A running sniff: the owner, the target, until when. */
    record Spoor(UUID eigenaar, Vec3 doel, long tot) {
    }

    private static final Map<UUID, Spoor> SPOREN = new ConcurrentHashMap<>();

    // =================================================================================================================
    // fast in the snow
    // =================================================================================================================

    /** Is this entity on (or in) snow, snowy ground or ice? */
    public static boolean opSneeuw(Entity e) {
        BlockState on = e.getBlockStateOn();
        BlockState in = e.level().getBlockState(e.blockPosition());
        return on.is(BlockTags.SNOW) || in.is(Blocks.SNOW) || on.is(BlockTags.ICE) || on.is(BaltoFeature.SNEEUWSPOOR.get())
                || on.is(BaltoFeature.SNEEUWDAK.get()) || on.hasProperty(SnowyDirtBlock.SNOWY) && on.getValue(SnowyDirtBlock.SNOWY)
                || on.is(nl.juiced.guhs.feature.guhpolder.GuhpolderFeature.RIJPGRAS.get());
    }

    @Override
    public float riddenSpeed(GuhEntity guh, float speed) {
        return opSneeuw(guh) ? speed * RIJ_EXTRA : speed;
    }

    @Override
    public void tick(GuhEntity guh) {
        Level level = guh.level();
        boolean sneeuw = (guh.tickCount + guh.getId()) % 5 == 0 ? opSneeuw(guh) : false;
        if (level.isClientSide) {
            // snow puffs from his paws when he runs through snow
            if (guh.tickCount % 3 == 0 && guh.onGround() && guh.getDeltaMovement().horizontalDistanceSqr() > 0.004 && opSneeuw(guh)) {
                level.addParticle(ParticleTypes.SNOWFLAKE, guh.getX() + (guh.getRandom().nextDouble() - 0.5) * 0.6, guh.getY() + 0.1,
                        guh.getZ() + (guh.getRandom().nextDouble() - 0.5) * 0.6, -guh.getDeltaMovement().x * 0.5, 0.06, -guh.getDeltaMovement().z * 0.5);
            }
            return;
        }
        if ((guh.tickCount + guh.getId()) % 5 == 0) {
            snelheid(guh, sneeuw);
        }
        if ((guh.tickCount + guh.getId()) % 20 == 0) {
            rit(guh);
        }
        spoor(guh);
    }

    /** Ridden fast over the snow for ~10 seconds (not necessarily in one go): the advancement "Zo snel als de wind". */
    static void rit(GuhEntity guh) {
        var data = guh.getPersistentData();
        double lx = data.getDouble("guhs_balto_rit_x"), lz = data.getDouble("guhs_balto_rit_z");
        data.putDouble("guhs_balto_rit_x", guh.getX());
        data.putDouble("guhs_balto_rit_z", guh.getZ());
        if (!(guh.getControllingPassenger() instanceof ServerPlayer rider) || !opSneeuw(guh)
                || Mth.square(guh.getX() - lx) + Mth.square(guh.getZ() - lz) < 9) {
            return;
        }
        int n = data.getInt("guhs_balto_rit") + 1;
        data.putInt("guhs_balto_rit", n);
        if (n >= 10) {
            BaltoVerhaal.grant(rider, "balto_sneeuwsnel");
        }
    }

    /** Adds or removes the snow speed modifier. */
    static void snelheid(GuhEntity guh, boolean sneeuw) {
        AttributeInstance speed = guh.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean heeft = speed.hasModifier(SNEEUW_MODIFIER);
        if (sneeuw && !heeft) {
            speed.addTransientModifier(new AttributeModifier(SNEEUW_MODIFIER, SNEEUW_EXTRA, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!sneeuw && heeft) {
            speed.removeModifier(SNEEUW_MODIFIER);
        }
    }

    // =================================================================================================================
    // "Snuffel!": the way home
    // =================================================================================================================

    @Nullable
    @Override
    public String speciaalKnop() {
        return "gui.guhs.balto.snuffel";
    }

    @Override
    public void speciaal(GuhEntity guh, ServerPlayer owner) {
        snuffel(guh, owner);
    }

    /** Where Baltoguh smells home for this owner: [target, kind ("huisje"/"spawn"/"andere_wereld")], or null. */
    @Nullable
    static Object[] doel(GuhEntity guh, ServerPlayer owner) {
        ServerLevel level = (ServerLevel) guh.level();
        Huisje thuis = Huisjes.thuisVan(guh);
        if (thuis == null) {
            List<Huisje> eigen = Huisjes.vanEigenaar(level.getServer(), owner.getUUID());
            for (int i = eigen.size() - 1; i >= 0 && thuis == null; i--) {
                if (eigen.get(i).dim() == level.dimension()) {
                    thuis = eigen.get(i);
                }
            }
            if (thuis == null && !eigen.isEmpty()) {
                return new Object[]{null, "andere_wereld"};
            }
        } else if (thuis.dim() != level.dimension()) {
            return new Object[]{null, "andere_wereld"};
        }
        if (thuis != null) {
            return new Object[]{thuis.midden(), "huisje"};
        }
        ResourceKey<Level> dim = owner.getRespawnDimension();
        BlockPos spawn = owner.getRespawnPosition();
        if (spawn != null && dim == level.dimension()) {
            return new Object[]{Vec3.atBottomCenterOf(spawn), "spawn"};
        }
        if (level.dimension() == Level.OVERWORLD) {
            return new Object[]{Vec3.atBottomCenterOf(level.getSharedSpawnPos()), "spawn"};
        }
        return new Object[]{null, "andere_wereld"};
    }

    /** Sniff: the pose, the sound, the line and the trail. Returns the target (null: nothing to smell in this world). */
    @Nullable
    public static Vec3 snuffel(GuhEntity guh, ServerPlayer owner) {
        ServerLevel level = (ServerLevel) guh.level();
        GuhHooks.zet(guh, VerhaalVlaggen.SNUFFELT, true);
        guh.getNavigation().stop();
        GuhHooks.bezig(guh, 50);
        level.playSound(null, guh.blockPosition(), BaltoFeature.SNUIF.get(), SoundSource.NEUTRAL, 1f, 1f);
        Object[] d = doel(guh, owner);
        Vec3 doel = d == null ? null : (Vec3) d[0];
        SPOREN.put(guh.getUUID(), new Spoor(owner.getUUID(), doel, level.getGameTime() + SNUFFEL_TICKS));
        if (doel == null) {
            owner.displayClientMessage(Component.translatable("gui.guhs.balto.snuffel.andere_wereld", guh.getDisplayName())
                    .withStyle(ChatFormatting.AQUA), false);
            return null;
        }
        double dx = doel.x - guh.getX(), dz = doel.z - guh.getZ();
        int afstand = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
        String soort = (String) d[1];
        if (afstand < 6) {
            owner.displayClientMessage(Component.translatable("gui.guhs.balto.snuffel.al_thuis." + soort, guh.getDisplayName())
                    .withStyle(ChatFormatting.AQUA), false);
        } else {
            owner.displayClientMessage(Component.translatable("gui.guhs.balto.snuffel." + soort, guh.getDisplayName(), afstand,
                    Component.translatable("gui.guhs.balto.richting." + richting(dx, dz))).withStyle(ChatFormatting.AQUA), false);
        }
        BaltoVerhaal.grant(owner, "balto_snuffel");
        return doel;
    }

    /** "noorden", "noordoosten", ... for a direction (x east, z south). */
    static String richting(double dx, double dz) {
        double hoek = Math.toDegrees(Math.atan2(dx, -dz));      // 0 = north, 90 = east
        int i = Math.floorMod((int) Math.round(hoek / 45.0), 8);
        return new String[]{"noorden", "noordoosten", "oosten", "zuidoosten", "zuiden", "zuidwesten", "westen", "noordwesten"}[i];
    }

    /** While sniffing: every 3 ticks the next paw print of the trail lights up (a wave running towards home). */
    static void spoor(GuhEntity guh) {
        Spoor s = SPOREN.get(guh.getUUID());
        if (s == null) {
            return;
        }
        ServerLevel level = (ServerLevel) guh.level();
        long nu = level.getGameTime();
        if (nu > s.tot()) {
            SPOREN.remove(guh.getUUID());
            GuhHooks.zet(guh, VerhaalVlaggen.SNUFFELT, false);
            return;
        }
        if (s.doel() == null || nu % 3 != 0 || !(level.getPlayerByUUID(s.eigenaar()) instanceof ServerPlayer owner)) {
            return;
        }
        Vec3 van = guh.position();
        Vec3 dir = new Vec3(s.doel().x - van.x, 0, s.doel().z - van.z);
        double len = dir.length();
        if (len < 1) {
            return;
        }
        dir = dir.scale(1 / len);
        int stappen = (int) Math.min(SPOOR_LENGTE, len);
        int k = (int) ((nu / 3) % Math.max(1, stappen / 2));
        for (int j = 0; j < 3; j++) {
            double afstand = 1.5 + (k * 2 + j * 0.7) % Math.max(2, stappen);
            // the paw prints zigzag a little, left - right, like real footsteps
            double zij = ((k + j) % 2 == 0 ? 0.35 : -0.35);
            double x = van.x + dir.x * afstand - dir.z * zij, z = van.z + dir.z * afstand + dir.x * zij;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
            level.sendParticles(owner, BaltoFeature.SNUFFEL.get(), true, x, y + 0.08, z, 1, 0, 0, 0, 0);
        }
    }

    /** (tests) is this guh sniffing right now? */
    static boolean snuffelt(GuhEntity guh) {
        return SPOREN.containsKey(guh.getUUID());
    }
}
