package nl.juiced.guhs.feature.safari.lobby;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.safari.ParkBouwer;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Keeps exactly one guh character at every spot of {@link LobbyPlekken} (made again when missing, doubles removed), with
 * its name floating above it. Looks every 5 seconds while players are in the safari park and the park is stamped.
 */
public final class SafariNpcs {
    private static final int ELKE = 100;
    private static final double KOP_Y = 2.35;

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % ELKE != 0) {
            return;
        }
        ServerLevel level = Safari.level(server);
        if (level == null || level.players().isEmpty() || !ParkBouwer.klaar(server)) {
            return;
        }
        for (LobbyPlekken.Npc n : LobbyPlekken.get().npcs) {
            zorg(level, n);
        }
    }

    /** The character at this spot (null when its chunk is not loaded). */
    @Nullable
    public static GuhNpcEntity van(ServerLevel level, String id) {
        LobbyPlekken.Npc n = LobbyPlekken.get().npc(id);
        if (n == null) {
            return null;
        }
        Vec3 pos = Vec3.atBottomCenterOf(n.pos());
        List<GuhNpcEntity> hier = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos, pos).inflate(3, 3, 3), e -> e.getKind() == n.kind());
        return hier.isEmpty() ? null : hier.get(0);
    }

    static boolean zorg(ServerLevel level, LobbyPlekken.Npc n) {
        BlockPos blok = n.pos();
        Vec3 pos = Vec3.atBottomCenterOf(blok);
        if (!level.hasChunk(blok.getX() >> 4, blok.getZ() >> 4) || !level.areEntitiesLoaded(ChunkPos.pack(blok))) {
            return false;
        }
        List<GuhNpcEntity> hier = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos, pos).inflate(2.5, 3, 2.5), e -> e.getKind() == n.kind());
        hier.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pos)));
        for (int i = 1; i < hier.size(); i++) {
            hier.get(i).discard();
        }
        if (hier.isEmpty()) {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            if (npc == null) {
                return false;
            }
            npc.setKind(n.kind());
            npc.snapTo(pos.x, pos.y, pos.z, n.yaw(), 0f);
            npc.setYHeadRot(n.yaw());
            npc.setYBodyRot(n.yaw());
            npc.setPersistenceRequired();
            level.addFreshEntity(npc);
        } else if (hier.get(0).distanceToSqr(pos) > 0.05) {
            hier.get(0).snapTo(pos.x, pos.y, pos.z, n.yaw(), 0f);
        }
        Component kop = Component.translatable("entity.guhs.guh_npc." + n.kind().id()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        Scorebord.show(level, pos.add(0, KOP_Y * n.kind().scale, 0), "safari_npc_" + n.id(), kop);
        return true;
    }

    private SafariNpcs() {
    }
}
