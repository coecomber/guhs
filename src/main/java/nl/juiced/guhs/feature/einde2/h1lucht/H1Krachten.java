package nl.juiced.guhs.feature.einde2.h1lucht;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.zij.Krachten;

/**
 * einde2 phase B, slice h1_lucht: the two story powers of air (CONTRACT_E2_B 7.2), only inside the Zuidelijke Luchttempel
 * and from the training on (step 3; after the chapter too). They come in the element wheel of {@code zij} with the four
 * lasting powers; a dev run can use them without the key ({@code /guhs einde2 verhaal kracht <id>}).
 * <ul>
 *   <li>{@value #LUCHTBAL}: a gust where you look: the luchtbal ball flies, Zuka's fireballs blow out, his men stumble aside,
 *       a creature in front of you gets a soft push (never a player, never damage);</li>
 *   <li>{@value #LUCHTSPRONG}: a big jump on the wind.</li>
 * </ul>
 * Their names: {@code gui.guhs.einde2_h1_lucht.kracht.<luchtbal|luchtsprong>}.
 */
public final class H1Krachten {
    public static final String LUCHTBAL = "einde2_h1_lucht_luchtbal", LUCHTSPRONG = "einde2_h1_lucht_luchtsprong";

    private H1Krachten() {
    }

    static void registreer() {
        Krachten.registreer(new Krachten.Kracht(LUCHTBAL, Einde2Ids.Element.LUCHT, "guhs:luchtscooter", 16, H1Krachten::kan, H1Krachten::luchtbal));
        Krachten.registreer(new Krachten.Kracht(LUCHTSPRONG, Einde2Ids.Element.LUCHT, "minecraft:feather", 40, H1Krachten::kan, H1Krachten::luchtsprong));
    }

    /** Inside the temple, from the training on. */
    static boolean kan(ServerPlayer p, BlockPos voeten) {
        return E2H1Lucht.LIJN.stap(p) >= E2H1Lucht.LUCHTBAL && E2H1Lucht.inTempel(p, voeten);
    }

    static void luchtbal(ServerPlayer p, Vec3 plek) {
        ServerLevel g = p.level();
        Vec3 r = p.getLookAngle();
        Vec3 oog = p.getEyePosition();
        for (int i = 1; i <= 6; i++) {
            Vec3 q = oog.add(r.scale(i * 1.2));
            g.sendParticles(ParticleTypes.CLOUD, q.x, q.y, q.z, 3, 0.15 * i / 2, 0.15 * i / 2, 0.15 * i / 2, 0.02);
        }
        g.sendParticles(ParticleTypes.GUST, oog.x + r.x * 2, oog.y + r.y * 2, oog.z + r.z * 2, 1, 0, 0, 0, 0);
        g.playSound(null, p, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        boolean iets = Luchtbal.waai(p, r) | Ontsnapping.waai(p, r);
        if (!iets) {
            AABB voor = new AABB(oog, oog.add(r.scale(6))).inflate(1.5);
            for (LivingEntity e : g.getEntitiesOfClass(LivingEntity.class, voor, e -> !(e instanceof Player) && !(e instanceof nl.juiced.guhs.entity.GuhNpcEntity) && !Tijdelijk.is(e) && e.isAlive() && !e.isInvulnerable())) {
                e.push(r.x * 0.8, 0.25, r.z * 0.8);
                e.hurtMarked = true;
            }
        }
    }

    static void luchtsprong(ServerPlayer p, Vec3 plek) {
        Vec3 r = p.getLookAngle();
        Vec3 vlak = new Vec3(r.x, 0, r.z);
        vlak = vlak.lengthSqr() < 1e-4 ? Vec3.ZERO : vlak.normalize();
        p.setDeltaMovement(vlak.x * 0.7, 1.05, vlak.z * 0.7);
        p.hurtMarked = true;
        p.resetFallDistance();
        p.level().sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 0.1, p.getZ(), 16, 0.4, 0.05, 0.4, 0.04);
        p.level().playSound(null, p, SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.6f, 1.6f);
    }
}
