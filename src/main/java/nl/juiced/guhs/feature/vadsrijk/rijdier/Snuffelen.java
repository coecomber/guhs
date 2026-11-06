package nl.juiced.guhs.feature.vadsrijk.rijdier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEntity;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;

/**
 * T-RIJDIER: the sniffing of a Snuffelaar pet ({@link Maatjes#SCHERP_OOG}). Every few seconds out of combat it looks for the nearest
 * spot that is still a secret for you, and shows the way with a trail of golden sparkles (only you see them) and a sparkle on the
 * spot itself; the first time per spot a little line ("Kruinroofje ruikt iets!").
 * <p>Spots come from:
 * <ul>
 *   <li>the Verkenningsboek ({@code voltooiing}): every not-yet-done entry marked secret with a place (hidden knuffels, clues,
 *       treasures, secret bosses... of every zone that registered a page);</li>
 *   <li>{@link #BRONNEN}: other slices add their own (e.g. gathering spots of the professions, puzzle pieces):
 *       {@code (player, radius) -> list of BlockPos}.</li>
 * </ul>
 */
public final class Snuffelen {
    /** Extra sniffable spots (other slices): (player, radius) -> positions near the player that are still worth finding. */
    public static final List<BiFunction<ServerPlayer, Double, List<BlockPos>>> BRONNEN = new CopyOnWriteArrayList<>();

    private Snuffelen() {
    }

    /** The nearest sniffable spot within r blocks, or null. */
    @Nullable
    public static BlockPos dichtstbij(ServerPlayer p, double r) {
        BlockPos beste = null;
        double bd = r * r;
        for (BlockPos b : plekken(p, r)) {
            double d = b.distToCenterSqr(p.position());
            if (d <= bd) {
                bd = d;
                beste = b;
            }
        }
        return beste;
    }

    /** Every sniffable spot within r blocks (secrets not yet found + the extra sources). */
    public static List<BlockPos> plekken(ServerPlayer p, double r) {
        List<BlockPos> l = new ArrayList<>();
        double r2 = r * r;
        for (Voltooiing.ZoneBoek z : Voltooiing.zones()) {
            for (Voltooiing.Categorie c : Voltooiing.categorieen(z)) {
                for (Voltooiing.Ding d : c.dingen()) {
                    if (d.geheim() && d.plek() != null && d.plek().distToCenterSqr(p.position()) <= r2
                            && Voltooiing.staat(p, d) != Voltooiing.Staat.KLAAR) {
                        l.add(d.plek());
                    }
                }
            }
        }
        for (BiFunction<ServerPlayer, Double, List<BlockPos>> f : BRONNEN) {
            try {
                List<BlockPos> x = f.apply(p, r);
                if (x != null) {
                    l.addAll(x);
                }
            } catch (RuntimeException e) {
                // (a broken source never stops the sniffing)
            }
        }
        return l;
    }

    /** One sniff: the trail to the nearest spot. Returns the spot (or null). {@code gemeld}: spots already announced. */
    @Nullable
    static BlockPos snuffel(ServerPlayer p, HuisdierEntity h, double r, Set<Long> gemeld) {
        BlockPos b = dichtstbij(p, r);
        if (b == null || !(p.level() instanceof ServerLevel sl)) {
            return null;
        }
        Vec3 a = h.position().add(0, 0.4, 0), doel = Vec3.atCenterOf(b).add(0, 0.6, 0);
        double lengte = a.distanceTo(doel);
        int stappen = (int) Math.min(40, Math.max(4, lengte * 1.5));
        DustParticleOptions goud = new DustParticleOptions(0xFFD84A, 0.8f);
        for (int i = 1; i <= stappen; i++) {
            Vec3 v = a.lerp(doel, i / (double) stappen);
            sl.sendParticles(p, goud, true, false, v.x, v.y + Math.sin(i * 0.6) * 0.15, v.z, 1, 0.03, 0.03, 0.03, 0);
        }
        sl.sendParticles(p, ParticleTypes.WAX_ON, true, false, doel.x, doel.y, doel.z, 10, 0.3, 0.4, 0.3, 0.05);
        if (gemeld.add(b.asLong())) {
            Huisdier hd = Huisdieren.actief(p);
            p.sendOverlayMessage(Component.translatable("vadsrijk.huisdier.snuffel", hd == null ? Component.empty() : Component.translatable(hd.naamKey()))
                    .withStyle(ChatFormatting.GOLD));
            sl.playSound(null, h.blockPosition(), SoundEvents.FOX_SNIFF, SoundSource.NEUTRAL, 0.7f, 1.6f);
        }
        return b;
    }
}
