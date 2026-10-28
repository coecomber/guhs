package nl.juiced.guhs.feature.dromen;

import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The Wekkerguh on the Droomplein: talk to it and you wake up (after a little joke). Exactly one stands on its spot of the
 * plein ({@link DroomEilanden.Opzet#wekkerguh()}), kept there once a second while somebody dreams.
 * <p>
 * Its look comes from part C (droomwezens): until then it is a plain guh named "Wekkerguh" that stands still. Part C
 * replaces {@link #MAKER} with its own NPC; everything else (the tag, the click, the keeping) stays here.
 */
public final class Wekkerguh {
    static final String TAG = "guhs_wekkerguh";
    private static final int GRAPJES = 5;

    /** Makes a new Wekkerguh (part C: its own NPC model). It gets the tag, the spot and the keeping from here. */
    public static Function<ServerLevel, Entity> MAKER = level -> {
        var g = ModEntities.GUH.get().create(level, EntitySpawnReason.STRUCTURE);
        if (g != null) {
            g.setCustomName(Component.translatable("entity.guhs.wekkerguh"));
            g.setCustomNameVisible(true);
        }
        return g;
    };

    private Wekkerguh() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Wekkerguh::onKlik);
        NeoForge.EVENT_BUS.addListener(Wekkerguh::tick);
    }

    public static boolean is(@Nullable Entity e) {
        return e != null && e.entityTags().contains(TAG);
    }

    private static void onKlik(PlayerInteractEvent.EntityInteract event) {
        if (!is(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer p) {
            p.sendSystemMessage(Component.translatable("gui.guhs.dromen.wekkerguh.grap" + p.getRandom().nextInt(GRAPJES))
                    .withStyle(ChatFormatting.GOLD));
            DroomReis.wakker(p);
        }
    }

    private static void tick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 11) {
            return;
        }
        ServerLevel level = event.getServer().getLevel(DromenFeature.DIM);
        if (level != null && !level.players().isEmpty()) {
            zorg(level);
        }
    }

    /** Exactly one Wekkerguh on its spot (when the plein has one, and its chunk is loaded). */
    static void zorg(ServerLevel level) {
        DroomEilanden.Punt punt = DroomEilanden.opzet(DroomEilanden.PLEIN).wekkerguh();
        if (punt == null) {
            return;
        }
        Vec3 plek = DroomEilanden.wereld(DroomEilanden.PLEIN, punt.plek());
        if (!level.isLoaded(net.minecraft.core.BlockPos.containing(plek))) {
            return;
        }
        List<Entity> er = level.getEntities((Entity) null, new AABB(plek, plek).inflate(32), Wekkerguh::is);
        Entity goed = null;
        for (Entity e : er) {
            if (goed == null && e.isAlive()) {
                goed = e;
            } else {
                e.discard();
            }
        }
        if (goed == null) {
            goed = MAKER.apply(level);
            if (goed == null) {
                return;
            }
            goed.addTag(TAG);
            goed.snapTo(plek.x, plek.y, plek.z, punt.yaw(), 0f);
            goed.setInvulnerable(true);
            if (goed instanceof Mob m) {
                m.setNoAi(true);
                m.setPersistenceRequired();
                m.setYHeadRot(punt.yaw());
            }
            level.addFreshEntity(goed);
        } else if (goed.position().distanceToSqr(plek) > 0.25) {
            goed.snapTo(plek.x, plek.y, plek.z, punt.yaw(), 0f);
        }
    }
}
