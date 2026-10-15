package nl.juiced.guhs.feature.einde2.h2water;

import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.bouwwater.E2BouwWaterFeature;
import nl.juiced.guhs.feature.einde2.zij.Krachten;
import nl.juiced.guhs.feature.verhaal.Duwtje;

/**
 * einde2 h2_water: the four story powers of chapter 2 (CONTRACT_E2_B 7.1: {@code Krachten.registreer}, {@code kan} = inside
 * the Waterstam only, from the step at which Pakguh teaches them). They are used with the one power key of the slice zij
 * (tap, or the element wheel), or in a dev run with {@code /guhs einde2 verhaal kracht <id>}.
 * <ul>
 *   <li>{@value #ZWEEP}: a whip of water straight ahead (9 blocks): hits the training targets ({@code zweep_doel_*}) and gives a
 *       Mika a little shove; never any harm.</li>
 *   <li>{@value #GOLF}: a wave rolls away from you; in the wave pool it counts for the training.</li>
 *   <li>{@value #IJSBRUG}: at the end of the jetty, looking at the islet: an ice bridge over the gap, for you alone.</li>
 *   <li>{@value #STROOM}: a current pushes your gondola (or yourself in the water) ahead.</li>
 * </ul>
 */
public final class E2H2WaterKrachten {
    public static final String ZWEEP = "einde2_h2_water_waterzweep", GOLF = "einde2_h2_water_watergolf", IJSBRUG = "einde2_h2_water_ijsbrug",
            STROOM = "einde2_h2_water_waterstroom";
    /** How far the whip reaches and how narrow its aim is (degrees off the look). */
    public static final double ZWEEP_BEREIK = 9.5, ZWEEP_HOEK = 18;

    private E2H2WaterKrachten() {
    }

    /** Registers the four powers (E2H2WaterFeature.register). */
    static void registreer() {
        Krachten.registreer(new Krachten.Kracht(ZWEEP, Einde2Ids.Element.WATER, "minecraft:water_bucket", 12,
                (p, voet) -> mag(p, E2H2Water.ZWEEP), (p, plek) -> zweep(p)));
        Krachten.registreer(new Krachten.Kracht(GOLF, Einde2Ids.Element.WATER, "minecraft:heart_of_the_sea", 40,
                (p, voet) -> mag(p, E2H2Water.GOLFOEFENING), (p, plek) -> golf(p)));
        Krachten.registreer(new Krachten.Kracht(IJSBRUG, Einde2Ids.Element.WATER, "minecraft:packed_ice", 40,
                (p, voet) -> mag(p, E2H2Water.IJSBRUG), (p, plek) -> ijsbrug(p)));
        Krachten.registreer(new Krachten.Kracht(STROOM, Einde2Ids.Element.WATER, "minecraft:oak_boat", 16,
                (p, voet) -> mag(p, E2H2Water.GONDEL), (p, plek) -> stroom(p)));
    }

    /** Inside the Waterstam, and the chapter has come this far (or is done). */
    static boolean mag(ServerPlayer p, int vanStap) {
        return E2H2Water.lijn().aanDeBeurt(p) && E2H2Water.lijn().stap(p) >= vanStap && E2H2Water.binnen(p);
    }

    // --- the whip ----------------------------------------------------------------------------------------------------------------

    static void zweep(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 oog = p.getEyePosition(), kijk = p.getLookAngle();
        for (int i = 1; i <= 18; i++) {
            Vec3 q = oog.add(kijk.scale(i * 0.5)).add(0, Math.sin(i * 0.7) * 0.15, 0);
            level.sendParticles(ParticleTypes.SPLASH, q.x, q.y, q.z, 3, 0.05, 0.05, 0.05, 0.02);
            level.sendParticles(ParticleTypes.BUBBLE_POP, q.x, q.y, q.z, 1, 0.02, 0.02, 0.02, 0.0);
        }
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.7f, 1.4f);
        // a Mika in front of you gets a gentle shove (no harm, never a guh)
        for (Entity e : level.getEntities(p, p.getBoundingBox().expandTowards(kijk.scale(ZWEEP_BEREIK)).inflate(1.0))) {
            if (e instanceof LivingEntity le && !(e instanceof Player) && (e instanceof nl.juiced.guhs.entity.MikaEntity) && geraakt(oog, kijk, e.position().add(0, 0.6, 0))) {
                Vec3 d = new Vec3(e.getX() - p.getX(), 0, e.getZ() - p.getZ()).normalize().scale(0.8);
                le.push(d.x, 0.25, d.z);
                le.hurtMarked = true;
            }
        }
        E2H2Water.Oord o = E2H2Water.oordVan(p);
        if (o == null || !E2H2Water.aanZet(p, E2H2Water.ZWEEP)) {
            return;
        }
        for (int nr = 1; nr <= E2H2Water.DOELEN; nr++) {
            BlockPos doel = o.wereld("zweep_doel_" + nr);
            if (doel != null && geraakt(oog, kijk, Vec3.atCenterOf(doel))) {
                level.sendParticles(ParticleTypes.SPLASH, doel.getX() + 0.5, doel.getY() + 0.7, doel.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.1);
                level.playSound(null, doel, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 0.6f + nr * 0.15f);
                E2H2Water.doelGeraakt(p, nr);
            }
        }
    }

    /** Is a spot within the whip's reach and its narrow aim (from the eyes along the look)? */
    public static boolean geraakt(Vec3 oog, Vec3 kijk, Vec3 doel) {
        Vec3 d = doel.subtract(oog);
        double l = d.length();
        if (l > ZWEEP_BEREIK || l < 0.3) {
            return l < 0.3;
        }
        double cos = d.dot(kijk) / (l * kijk.length());
        return cos >= Math.cos(Math.toRadians(ZWEEP_HOEK));
    }

    /** The training targets that are still to be hit glow a little (for this player only). */
    static void toonDoelen(ServerPlayer p, E2H2Water.Oord o) {
        for (int nr = 1; nr <= E2H2Water.DOELEN; nr++) {
            BlockPos doel = o.wereld("zweep_doel_" + nr);
            if (doel != null && !E2H2Water.lijn().vlag(p, E2H2Water.V_DOEL + nr) && doel.distToCenterSqr(p.position()) < 40 * 40) {
                Schijn.deeltjes(p, ParticleTypes.DRIPPING_WATER, Vec3.atCenterOf(doel).add(0, 0.8, 0), 3, 0.3, 0.0);
            }
        }
    }

    // --- the wave ------------------------------------------------------------------------------------------------------------------

    static void golf(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 kijk = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z);
        if (kijk.lengthSqr() < 1e-4) {
            kijk = new Vec3(0, 0, 1);
        }
        kijk = kijk.normalize();
        Vec3 dwars = new Vec3(-kijk.z, 0, kijk.x);
        for (int i = 1; i <= 10; i++) {
            for (int j = -3; j <= 3; j++) {
                Vec3 q = p.position().add(kijk.scale(i * 1.2)).add(dwars.scale(j * 0.8)).add(0, 0.3 + Math.max(0, 1.2 - Math.abs(j) * 0.3), 0);
                level.sendParticles(ParticleTypes.SPLASH, q.x, q.y, q.z, 4, 0.2, 0.1, 0.2, 0.05);
                if (i % 3 == 0) {
                    level.sendParticles(ParticleTypes.BUBBLE, q.x, q.y - 0.3, q.z, 2, 0.2, 0.1, 0.2, 0.05);
                }
            }
        }
        level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 1.0f, 0.7f);
        for (Entity e : level.getEntities(p, p.getBoundingBox().inflate(8))) {
            if (e instanceof nl.juiced.guhs.entity.MikaEntity m) {
                Vec3 d = new Vec3(m.getX() - p.getX(), 0, m.getZ() - p.getZ()).normalize().scale(0.9);
                m.push(d.x, 0.2, d.z);
                m.hurtMarked = true;
            }
        }
        E2H2Water.Oord o = E2H2Water.oordVan(p);
        if (o != null && E2H2Water.aanZet(p, E2H2Water.GOLFOEFENING) && o.in(p.position(), "golf_bassin", 3)) {
            E2H2Water.golfGemaakt(p);
        }
    }

    // --- the ice bridge -------------------------------------------------------------------------------------------------------------

    static void ijsbrug(ServerPlayer p) {
        E2H2Water.Oord o = E2H2Water.oordVan(p);
        BlockPos a = o == null ? null : o.wereld("ijsbrug_a"), b = o == null ? null : o.wereld("ijsbrug_b");
        ServerLevel level = p.level();
        if (a == null || b == null || Vec3.atBottomCenterOf(a.above()).distanceToSqr(p.position()) > 4.5 * 4.5) {
            // anywhere else: a little frost at your feet, nothing more
            level.sendParticles(ParticleTypes.SNOWFLAKE, p.getX(), p.getY() + 0.2, p.getZ(), 30, 1.0, 0.1, 1.0, 0.02);
            E2H2Water.balk(p, "quest.guhs.einde2_h2_water.ijsbrug.hier_niet");
            return;
        }
        Vec3 naar = Vec3.atCenterOf(b).subtract(p.getEyePosition());
        Vec3 kijk = p.getLookAngle();
        double cos = new Vec3(naar.x, 0, naar.z).normalize().dot(new Vec3(kijk.x, 0, kijk.z).normalize());
        if (cos < Math.cos(Math.toRadians(40))) {
            E2H2Water.balk(p, "quest.guhs.einde2_h2_water.ijsbrug.kijk");
            return;
        }
        for (BlockPos q : brug(a, b)) {
            level.sendParticles(ParticleTypes.SNOWFLAKE, q.getX() + 0.5, q.getY() + 1.0, q.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.02);
        }
        level.playSound(null, a, SoundEvents.GLASS_PLACE, SoundSource.PLAYERS, 1.0f, 0.8f);
        E2H2Water.brugGemaakt(p);
    }

    /** The cells of the bridge: three wide between the end of the jetty and the rim of the islet, at their height. */
    public static java.util.List<BlockPos> brug(BlockPos a, BlockPos b) {
        java.util.List<BlockPos> uit = new java.util.ArrayList<>();
        int dx = Integer.signum(b.getX() - a.getX()), dz = Integer.signum(b.getZ() - a.getZ());
        boolean langsX = Math.abs(b.getX() - a.getX()) >= Math.abs(b.getZ() - a.getZ());
        int n = Math.max(Math.abs(b.getX() - a.getX()), Math.abs(b.getZ() - a.getZ()));
        for (int i = 1; i < n; i++) {
            BlockPos m = langsX ? new BlockPos(a.getX() + dx * i, a.getY(), a.getZ()) : new BlockPos(a.getX(), a.getY(), a.getZ() + dz * i);
            for (int w = -1; w <= 1; w++) {
                uit.add(langsX ? m.offset(0, 0, w) : m.offset(w, 0, 0));
            }
        }
        return uit;
    }

    /** Schijn: the ice bridge of a player who made it, while it stands. */
    static void schijnBrug(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        if (!E2H2Water.brugStaat(p)) {
            return;
        }
        E2H2Water.Oord o = E2H2Water.oordVan(p);
        BlockPos a = o == null ? null : o.wereld("ijsbrug_a"), b = o == null ? null : o.wereld("ijsbrug_b");
        if (a == null || b == null) {
            return;
        }
        BlockState ijs = E2BouwWaterFeature.IJSSTEEN.get().defaultBlockState();
        for (BlockPos q : brug(a, b)) {
            if (Schijn.dichtbij(p, q) && !p.level().getBlockState(q).isSolid()) {
                gewenst.put(q, ijs);
            }
        }
    }

    // --- the current -----------------------------------------------------------------------------------------------------------------

    static void stroom(ServerPlayer p) {
        Entity voertuig = p.getVehicle();
        Vec3 kijk = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z);
        if (kijk.lengthSqr() < 1e-4) {
            return;
        }
        kijk = kijk.normalize();
        ServerLevel level = p.level();
        Entity wie = Toneel.isGondel(p, voertuig) ? voertuig : (p.isInWater() ? p : null);
        if (wie == null) {
            level.sendParticles(ParticleTypes.SPLASH, p.getX(), p.getY() + 0.5, p.getZ(), 20, 0.5, 0.2, 0.5, 0.1);
            E2H2Water.balk(p, "quest.guhs.einde2_h2_water.stroom.geen_water");
            return;
        }
        double kracht = wie == p ? 0.9 : 1.25;
        wie.setDeltaMovement(wie.getDeltaMovement().add(kijk.scale(kracht)).multiply(1, 0, 1).add(0, 0.05, 0));
        wie.hurtMarked = true;
        p.hurtMarked = true;
        Vec3 achter = wie.position().subtract(kijk.scale(1.5));
        level.sendParticles(ParticleTypes.SPLASH, achter.x, achter.y + 0.4, achter.z, 25, 0.6, 0.2, 0.6, 0.15);
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, achter.x, achter.y, achter.z, 8, 0.5, 0.1, 0.5, 0.1);
        level.playSound(null, wie.blockPosition(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 1.0f, 0.8f);
        if (!Duwtje.mag(p)) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-kijk.x, kijk.z));
        if (wie != p) {
            wie.setYRot(Mth.approachDegrees(wie.getYRot(), yaw, 30f));
        }
    }
}
