package nl.juiced.guhs.feature.safari.lobby;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.safari.ParkBouwer;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The kijk-guhs (test round 3, design ronde 44): on every lookout of the park (a hide, a viewing hut, a tower, the
 * underwater dome) one visitor guh in safari clothes with binoculars sits on a guh chair and looks out, now and then
 * turning its head a little (looking through the binoculars). It can not be picked up or tamed; a click gives a funny
 * line about what it sees, sometimes with a hint about a rare animal.
 * The chairs come from {@link Beleving#stoelen} (the viewpoints of the river pipeline, or the lookout chairs found by the
 * generator). Exactly one kijk-guh per chair is kept, checked every 10 seconds while players are in the park.
 */
public final class Kijkguhs {
    public static final String TAG = "guhs_safari_kijkguh";
    private static final int ELKE = 200, KIJK_ELKE = 70;
    /** How far above the chair block the sitting guh is put (the chair's seat). */
    static final double ZIT_Y = 0.45;
    static final int LIJNEN = 6, HINTS = 3;

    static void tick(MinecraftServer server) {
        ServerLevel level = Safari.level(server);
        if (level == null || level.players().isEmpty() || !ParkBouwer.klaar(server)) {
            return;
        }
        List<Beleving.Stoel> stoelen = Beleving.get().stoelen;
        if (server.getTickCount() % ELKE == 41) {
            for (Beleving.Stoel s : stoelen) {
                zorg(level, s);
            }
        }
        if (server.getTickCount() % KIJK_ELKE == 17) {
            for (Beleving.Stoel s : stoelen) {
                kijk(level, s);
            }
        }
    }

    static Vec3 zitPlek(Beleving.Stoel s) {
        return new Vec3(s.pos().getX() + 0.5, s.pos().getY() + ZIT_Y, s.pos().getZ() + 0.5);
    }

    private static List<GuhEntity> hier(ServerLevel level, Beleving.Stoel s) {
        Vec3 p = zitPlek(s);
        return level.getEntitiesOfClass(GuhEntity.class, new AABB(p, p).inflate(1.5), g -> g.entityTags().contains(TAG));
    }

    static void zorg(ServerLevel level, Beleving.Stoel s) {
        if (!level.hasChunk(s.pos().getX() >> 4, s.pos().getZ() >> 4) || !level.areEntitiesLoaded(ChunkPos.pack(s.pos()))) {
            return;
        }
        Vec3 p = zitPlek(s);
        List<GuhEntity> er = new ArrayList<>(hier(level, s));
        er.sort(Comparator.comparingDouble(g -> g.distanceToSqr(p)));
        for (int i = 1; i < er.size(); i++) {
            er.get(i).discard();
        }
        if (er.isEmpty()) {
            GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (guh == null) {
                return;
            }
            guh.setVariant(GuhVariant.roll(level.getRandom()));
            guh.wear(GuhClothes.SAFARI_HOEDJE);
            guh.wear(GuhClothes.SAFARI_VESTJE);
            guh.wear(GuhClothes.SAFARI_VERREKIJKER);
            guh.addTag(TAG);
            guh.setPersistenceRequired();
            guh.setNoAi(true);
            guh.setNoGravity(true);
            guh.setInvulnerable(true);
            guh.setOrderedToSit(true);
            guh.setInSittingPose(true);
            guh.snapTo(p.x, p.y, p.z, s.yaw(), 0f);
            guh.setYHeadRot(s.yaw());
            guh.setYBodyRot(s.yaw());
            level.addFreshEntity(guh);
        } else if (er.get(0).distanceToSqr(p) > 0.05) {
            er.get(0).snapTo(p.x, p.y, p.z, s.yaw(), 0f);
        }
    }

    /** Looking through the binoculars: a small turn of the head and a slightly raised look, around the chair's view. */
    private static void kijk(ServerLevel level, Beleving.Stoel s) {
        for (GuhEntity g : hier(level, s)) {
            float yaw = s.yaw() + (g.getRandom().nextFloat() - 0.5f) * 50f;
            g.setYHeadRot(yaw);
            g.setXRot(-4f - g.getRandom().nextFloat() * 10f);
            g.setYBodyRot(s.yaw());
            g.setYRot(s.yaw());
        }
    }

    static boolean isKijkguh(Entity e) {
        return e instanceof GuhEntity && e.entityTags().contains(TAG);
    }

    /** A click on a kijk-guh: what it sees right now (an animal living in front of its window), or a rare-animal hint. */
    static void opInteractie(PlayerInteractEvent.EntityInteract event) {
        if (!isKijkguh(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        Entity guh = event.getTarget();
        int r = Math.floorMod(guh.getUUID().hashCode() + (int) (p.level().getGameTime() / 60), 8);
        ParkData d = ParkData.get();
        if (r < 2) {
            ParkData.Plek zeldzaam = zeldzaam(d, guh.getUUID().hashCode() + (int) (p.level().getGameTime() / 1200));
            if (zeldzaam != null) {
                p.sendSystemMessage(Component.translatable("gui.guhs.safari.kijkguh.hint." + (r % HINTS),
                        Component.translatable("entity.guhs.safari." + zeldzaam.soort()), Component.translatable("biome.guhs.safari_" + zeldzaam.zone()))
                        .withStyle(ChatFormatting.YELLOW));
                return;
            }
        }
        String soort = inBeeld(d, guh.position(), guh.getYRot());
        if (soort != null) {
            p.sendSystemMessage(Component.translatable("gui.guhs.safari.kijkguh.ziet." + (r % LIJNEN), Component.translatable("entity.guhs.safari." + soort))
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            p.sendSystemMessage(Component.translatable("gui.guhs.safari.kijkguh.niks").withStyle(ChatFormatting.YELLOW));
        }
    }

    static void opInteractieSpecifiek(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isKijkguh(event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    /** The nearest home in front of this window (within 60 degrees of the view, at most 56 blocks away): its species. */
    static String inBeeld(ParkData d, Vec3 van, float yaw) {
        ParkData.Plek beste = null;
        double besteAfstand = Double.MAX_VALUE;
        for (ParkData.Plek p : d.plekken) {
            double dx = p.centrum().getX() + 0.5 - van.x, dz = p.centrum().getZ() + 0.5 - van.z;
            double afstand = Math.sqrt(dx * dx + dz * dz);
            if (afstand > 56) {
                continue;
            }
            float richting = (float) Math.toDegrees(Math.atan2(-dx, dz));
            if (Math.abs(Mth.wrapDegrees(richting - yaw)) > 60) {
                continue;
            }
            if (afstand < besteAfstand) {
                besteAfstand = afstand;
                beste = p;
            }
        }
        return beste == null ? null : beste.soort();
    }

    /** A rare animal's home (a hider or one with a small territory), chosen by the seed. */
    static ParkData.Plek zeldzaam(ParkData d, int seed) {
        List<ParkData.Plek> uit = new ArrayList<>();
        for (ParkData.Plek p : d.plekken) {
            ParkData.Soort s = d.soort(p.soort());
            if (s != null && (s.heeft("territorium") || s.heeft("verstopper"))) {
                uit.add(p);
            }
        }
        return uit.isEmpty() ? null : uit.get(Math.floorMod(seed, uit.size()));
    }

    private Kijkguhs() {
    }
}
