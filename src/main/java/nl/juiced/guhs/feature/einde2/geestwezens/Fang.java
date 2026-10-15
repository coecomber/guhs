package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.feature.ring.Zicht;

/**
 * einde2 phase B (CONTRACT_E2_B 3), owner: slice geest_modellen. Fang's flight (DESIGN_EINDE2 5 "Fang's Hemelpad"): the spirit
 * dragon of Roku-guh appears beside the player, the player sits on his neck seat ({@code FangEntity.ZADEL}) and Fang flies a
 * smooth curve through the points (the player's seat passes through every point; the first leg starts where the player
 * stands), lands at the last one, bows, and the player stands there; then {@code daarna} runs once.
 * <ul>
 *   <li>The player cannot get off on the way (sneaking does nothing but a small "Fang houdt je vast"); nothing can hurt them.</li>
 *   <li>Fang is a story actor for that player alone ({@code Zicht.alleenVoor}) and is never saved: a leftover takes itself
 *       away.</li>
 *   <li>Logging out, dying or changing dimension ends the flight at once (the player is put at the last point; {@code daarna}
 *       does NOT run then: the story asks its own state on the next visit).</li>
 *   <li>False (nothing happened): an empty path, a dead or spectating player, or a flight of this player already going.</li>
 * </ul>
 * Speed: {@link #SNEL} blocks a tick (12 blocks a second) after a short climb; Fang first hovers {@link #OPSTIJG} ticks.
 */
public final class Fang {
    public static final String TAG = "guhs_einde2_geestmodellen_fangvlucht";
    public static final double SNEL = 0.6;
    public static final int OPSTIJG = 20, LAND = 30;
    static final Map<UUID, FangEntity> VLIEGT = new ConcurrentHashMap<>();

    private Fang() {
    }

    /** Flies p along the points (in p's dimension); {@code daarna} on arrival. False: could not start. */
    public static boolean vlieg(ServerPlayer p, List<Vec3> pad, @Nullable Consumer<ServerPlayer> daarna) {
        if (pad.isEmpty() || !p.isAlive() || p.isSpectator() || vliegt(p)) {
            return false;
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        List<Vec3> punten = new ArrayList<>();
        punten.add(p.position());
        punten.addAll(pad);
        List<Vec3> lijn = glad(punten);
        ServerLevel level = (ServerLevel) p.level();
        FangEntity fang = E2GeestWezensFeature.FANG.get().create(level, EntitySpawnReason.TRIGGERED);
        if (fang == null) {
            return false;
        }
        Vec3 richting = lijn.size() > 1 ? lijn.get(Math.min(lijn.size() - 1, 4)).subtract(lijn.get(0)) : p.getLookAngle();
        float yaw = GeestWezen.hoek(richting.x, richting.z);
        Vec3 plek = lijn.get(0).subtract(FangEntity.ZADEL.yRot(-yaw * (float) (Math.PI / 180.0)));
        fang.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        fang.yBodyRot = yaw;
        fang.yHeadRot = yaw;
        fang.addTag(TAG);
        Zicht.alleenVoor(fang, p.getUUID());
        level.addFreshEntity(fang);
        fang.vlucht = new FangEntity.Vlucht(p.getUUID(), lijn, daarna);
        if (!p.startRiding(fang, true, true)) {
            fang.discard();
            return false;
        }
        p.resetFallDistance();
        fang.zetAnimatie("vlieg");
        VLIEGT.put(p.getUUID(), fang);
        return true;
    }

    /** Is p on one of Fang's flights right now? */
    public static boolean vliegt(ServerPlayer p) {
        FangEntity f = VLIEGT.get(p.getUUID());
        return f != null && f.isAlive() && f.vlucht != null;
    }

    /** A smooth curve (Catmull-Rom) through the points, about every quarter block. */
    static List<Vec3> glad(List<Vec3> punten) {
        List<Vec3> uit = new ArrayList<>();
        if (punten.size() == 1) {
            uit.add(punten.get(0));
            return uit;
        }
        for (int i = 0; i < punten.size() - 1; i++) {
            Vec3 p0 = punten.get(Math.max(0, i - 1)), p1 = punten.get(i), p2 = punten.get(i + 1), p3 = punten.get(Math.min(punten.size() - 1, i + 2));
            int n = Math.max(1, (int) Math.ceil(p1.distanceTo(p2) * 4));
            for (int k = 0; k < n; k++) {
                double t = k / (double) n, t2 = t * t, t3 = t2 * t;
                uit.add(p1.scale(2).add(p2.subtract(p0).scale(t)).add(p0.scale(2).subtract(p1.scale(5)).add(p2.scale(4)).subtract(p3).scale(t2))
                        .add(p0.scale(-1).add(p1.scale(3)).subtract(p2.scale(3)).add(p3).scale(t3)).scale(0.5));
            }
        }
        uit.add(punten.get(punten.size() - 1));
        return uit;
    }

    // --- events (registered in E2GeestWezensFeature) ------------------------------------------------------------------------

    /** No getting off halfway. */
    static void opAfstappen(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof FangEntity fang
                && fang.vlucht != null && !fang.vlucht.landt && event.getEntityMounting() instanceof ServerPlayer p && p.isAlive()
                && !p.isRemoved() && p.isShiftKeyDown()) {
            event.setCanceled(true);
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_geestmodellen.fang.vast").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Logging out, dying, another dimension: the flight ends at once, the player at the last point. */
    static void spelerWeg(ServerPlayer p) {
        FangEntity fang = VLIEGT.remove(p.getUUID());
        if (fang != null && fang.vlucht != null) {
            fang.breekAf(p);
        }
    }

    static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            spelerWeg(p);
        }
    }

    static void opDimensie(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            spelerWeg(p);
        }
    }

    static void opDood(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            spelerWeg(p);
        }
    }
}
