package nl.juiced.guhs.feature.vadsrijk.zone3.systemen.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Premiebord;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Wind;

/**
 * Zone 3 systemen, the client: the Premiebord screen, and the riding client's side of the wind columns and the Pluimguh glide
 * (the riding client moves its mount, so this is where the mount is pushed):
 * <ul>
 *   <li>wind column: the mount rises at {@link Z3Wind#OMHOOG} b/t and is set down towards the landing at the top;</li>
 *   <li>Pluimzweef: on a Pluimguh in the air, hold jump to glide: the fall is capped at 0.12 b/t and you keep ~9 b/s forward.
 *   Only downwards (it never lifts you).</li>
 * </ul>
 */
public final class Z3SystemenClient {
    public static final double ZWEEF_VAL = -0.12, ZWEEF_VOORUIT = 0.45;

    private Z3SystemenClient() {
    }

    public static void init(IEventBus modBus) {
        Premiebord.ontvanger = s -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof PremieScherm j) {
                j.update(s.data(), s.items());
            } else {
                mc.setScreen(new PremieScherm(s.data(), s.items()));
            }
        };
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> tick());
    }

    private static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || !Vadsrijk.in(mc.level) || !(p.getVehicle() instanceof RijdierEntity r) || r.getControllingPassenger() != p) {
            return;
        }
        Vec3 dm = r.getDeltaMovement();
        Z3Wind.Kolom k = Z3Wind.kolom(r.getX(), r.getY(), r.getZ());
        if (k != null) {
            if (!k.boven(r.getY())) {
                r.setDeltaMovement(dm.x * 0.7, Z3Wind.OMHOOG, dm.z * 0.7);
            } else {
                Vec3 naar = new Vec3(k.lx() + 0.5 - r.getX(), 0, k.lz() + 0.5 - r.getZ());
                if (naar.lengthSqr() > 0.25) {
                    Vec3 v = naar.normalize().scale(0.4);
                    r.setDeltaMovement(v.x, 0.08, v.z);
                }
            }
            deeltjes(r);
            return;
        }
        if (r.soort() == RijdierSoort.PLUIMGUH && !r.onGround() && mc.options.keyJump.isDown() && dm.y < ZWEEF_VAL) {
            Vec3 kijk = Vec3.directionFromRotation(0, r.getYRot());
            double h = Math.sqrt(dm.x * dm.x + dm.z * dm.z);
            Vec3 hor = h < ZWEEF_VOORUIT ? new Vec3(kijk.x * ZWEEF_VOORUIT, 0, kijk.z * ZWEEF_VOORUIT) : new Vec3(dm.x, 0, dm.z);
            r.setDeltaMovement(hor.x, ZWEEF_VAL, hor.z);
            r.fallDistance = 0;
            if (r.tickCount % 3 == 0) {
                mc.level.addParticle(ParticleTypes.CHERRY_LEAVES, r.getX(), r.getY() + 0.5, r.getZ(), 0, -0.05, 0);
            }
        }
    }

    private static void deeltjes(Entity r) {
        Minecraft mc = Minecraft.getInstance();
        if (r.tickCount % 2 == 0 && mc.level != null) {
            mc.level.addParticle(ParticleTypes.CHERRY_LEAVES, r.getX() + (r.getRandom().nextDouble() - 0.5) * 2, r.getY(), r.getZ() + (r.getRandom().nextDouble() - 0.5) * 2, 0, 0.2, 0);
        }
    }
}
