package nl.juiced.guhs.feature.guhpixel;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The NPCs at the lobby anchors. A slice registers (anchor, kind, heading, second line); this class keeps exactly ONE
 * NPC of that kind at the anchor (made again when it is missing, doubles removed) with two floating text lines above it:
 * the heading (default "KLIK OM TE SPELEN") and a second line that is refreshed every 5 seconds ("0 spelers · 99 guhs").
 * What a click does is the kind's NpcRole (NpcRollen.zet in the slice).
 */
public final class LobbyNpcs {
    private static final int ELKE = 100;
    private static final double KOP_Y = 2.55, REGEL_Y = 2.25;

    private record Npc(LobbyPlek plek, GuhNpcEntity.Kind kind, @Nullable Component kop, @Nullable Function<MinecraftServer, Component> onderregel) {
    }

    private static final Map<LobbyPlek, Npc> NPCS = new LinkedHashMap<>();
    private static boolean nu;

    /** One NPC per anchor (a later registration replaces an earlier one). kop null: "KLIK OM TE SPELEN"; onderregel null: none. */
    public static void registreer(LobbyPlek plek, GuhNpcEntity.Kind kind, @Nullable Component kop, @Nullable Function<MinecraftServer, Component> onderregel) {
        NPCS.put(plek, new Npc(plek, kind, kop, onderregel));
    }

    /** "0 spelers · 99 guhs": the real players in this kind of game right now, and a number of guhs. */
    public static Component spelersRegel(String soortId, int guhs) {
        return Component.translatable("gui.guhs.guhpixel.npc.spelers", Sessies.aantalSpelers(soortId), guhs).withStyle(ChatFormatting.GRAY);
    }

    /** Look again on the next tick (after the lobby was rebuilt). */
    static void meteen() {
        nu = true;
    }

    static void tick(MinecraftServer server) {
        if (NPCS.isEmpty() || (!nu && server.getTickCount() % ELKE != 0)) {
            return;
        }
        ServerLevel level = Guhpixel.level(server);
        if (level == null || level.players().isEmpty() || Lobby.herbouwBezig() || Lobby.gebouwd(server) == 0) {
            return;
        }
        nu = false;
        for (Npc n : NPCS.values()) {
            zorg(level, n);
        }
    }

    /** (Zelftest, dev) checks every anchor now; returns how many NPCs stand. */
    static int controleer(ServerLevel level) {
        int staan = 0;
        for (Npc n : NPCS.values()) {
            if (zorg(level, n)) {
                staan++;
            }
        }
        return staan;
    }

    static int aantal() {
        return NPCS.size();
    }

    private static boolean zorg(ServerLevel level, Npc n) {
        Vec3 pos = n.plek().pos();
        ChunkPos chunk = ChunkPos.containing(n.plek().blok());
        if (!level.hasChunk(chunk.x(), chunk.z()) || !level.areEntitiesLoaded(ChunkPos.pack(n.plek().blok()))) {
            return false;
        }
        List<GuhNpcEntity> hier = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos, pos).inflate(2.5, 3, 2.5), e -> e.getKind() == n.kind());
        hier.sort(Comparator.comparingDouble(e -> e.distanceToSqr(pos)));
        for (int i = 1; i < hier.size(); i++) {
            hier.get(i).discard();
        }
        // another kind at this anchor (an older registration): away with it
        for (GuhNpcEntity ander : level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos, pos).inflate(0.6, 1, 0.6), e -> e.getKind() != n.kind())) {
            ander.discard();
        }
        if (hier.isEmpty()) {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
            if (npc == null) {
                return false;
            }
            npc.setKind(n.kind());
            npc.snapTo(pos.x, pos.y, pos.z, n.plek().yaw(), 0f);
            npc.setYHeadRot(n.plek().yaw());
            npc.setYBodyRot(n.plek().yaw());
            npc.setPersistenceRequired();
            level.addFreshEntity(npc);
        } else if (hier.get(0).distanceToSqr(pos) > 0.05) {
            hier.get(0).snapTo(pos.x, pos.y, pos.z, n.plek().yaw(), 0f);
        }
        Component kop = n.kop() != null ? n.kop()
                : Component.translatable("gui.guhs.guhpixel.npc.klik").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD);
        Scorebord.show(level, pos.add(0, KOP_Y, 0), "px_kop_" + n.plek().name(), kop);
        if (n.onderregel() != null) {
            Component regel = n.onderregel().apply(level.getServer());
            if (regel != null) {
                Scorebord.show(level, pos.add(0, REGEL_Y, 0), "px_regel_" + n.plek().name(), regel);
            }
        }
        return true;
    }

    private LobbyNpcs() {
    }
}
