package nl.juiced.guhs.feature.safari;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.safari.lobby.LobbyPlekken;

/**
 * The park is a safe place for players (test round 3): in the safari dimension a player takes no damage of any kind
 * (falling, drowning, an animal, starving...; only /kill and the void still go through), does not get hungry (food and
 * saturation never go down; what you eat counts), and whoever falls out of the world is put back on the village square.
 */
public final class SafariVeilig {
    /** Server: per player in the park the food level and saturation it keeps (the highest since entering). */
    private static final Map<UUID, float[]> ETEN = new HashMap<>();

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SafariVeilig::opSchade);
        NeoForge.EVENT_BUS.addListener(SafariVeilig::opTick);
    }

    /** Does the park stop this damage (pure: in the park or not, a player or not, the kind of damage)? */
    public static boolean tegenhouden(boolean inPark, Entity wie, DamageSource bron) {
        return inPark && wie instanceof Player && !bron.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    private static void opSchade(LivingIncomingDamageEvent event) {
        Entity e = event.getEntity();
        if (tegenhouden(Safari.in(e), e, event.getSource())) {
            event.setCanceled(true);
            e.resetFallDistance();
        }
    }

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 5 != 0) {
            return;
        }
        if (!Safari.in(p)) {
            ETEN.remove(p.getUUID());
            return;
        }
        var food = p.getFoodData();
        float[] vloer = ETEN.computeIfAbsent(p.getUUID(), id -> new float[] {food.getFoodLevel(), food.getSaturationLevel()});
        vloer[0] = Math.max(vloer[0], food.getFoodLevel());
        vloer[1] = Math.max(vloer[1], food.getSaturationLevel());
        food.setFoodLevel((int) vloer[0]);
        food.setSaturation(Math.min(vloer[1], vloer[0]));
        p.setAirSupply(p.getMaxAirSupply());
        if (p.getY() < p.level().getMinY() + 4 && p.level() instanceof ServerLevel level) {
            BlockPos c = LobbyPlekken.get().plein;
            BlockPos g = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c);
            p.teleportTo(g.getX() + 0.5, g.getY(), g.getZ() + 0.5);
            p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            p.resetFallDistance();
        }
    }

    private SafariVeilig() {
    }
}
