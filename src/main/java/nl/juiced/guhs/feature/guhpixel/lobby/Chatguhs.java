package nl.juiced.guhs.feature.guhpixel.lobby;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.feature.guhpixel.Rang;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The lobby guhs (NPC kind LOBBY_CHATGUH): six little guhs with gamer names who sit around the plaza and in the AFK-hoek
 * ({@link LobbyKaart}) and "chat" now and then, like the regulars of any minigame server: "gg", "iemand party?", "lag!!",
 * "afk (slaap)". One line per {@link #MIN_WACHT}..{@link #MAX_WACHT} ticks (30 to 60 seconds), only sent to the players
 * who are on the plaza (in the lobby, not in a game), never the same line twice in a row. Click one: it says something
 * to you alone.
 */
public final class Chatguhs implements NpcRole {
    public static final int NAMEN = 6, REGELS = 22, MIN_WACHT = 600, MAX_WACHT = 1200;
    static final Chatguhs ROL = new Chatguhs();
    private static final String NAAM = "LobbyNaam";
    /** The ranks the lobby guhs show off with (they have been here a while). */
    private static final Rang[] RANGEN = {Rang.GUH, Rang.GUH, Rang.VADS, Rang.VADS, Rang.VADS_PLUS, Rang.MVG, Rang.MVG_PLUS_PLUS};

    private static int wacht = MIN_WACHT;
    private static int vorige = -1;

    static Component naam(int i) {
        return Component.translatable("gui.guhs.lobby.chat.naam." + Math.floorMod(i, NAMEN));
    }

    /** How long until the next line: 30 to 60 seconds. */
    static int volgendeWacht(RandomSource random) {
        return MIN_WACHT + random.nextInt(MAX_WACHT - MIN_WACHT + 1);
    }

    /** Picks the next line: any but the previous one. */
    static int volgendeRegel(RandomSource random) {
        int r = random.nextInt(REGELS - 1);
        if (r >= vorige && vorige >= 0) {
            r++;
        }
        vorige = r;
        return r;
    }

    /** "[VADS] Knabbel2009: gg", the way a chat line of a ranked player looks. */
    static Component regel(int wie, int regel) {
        Rang rang = RANGEN[Math.floorMod(wie * 3 + 1, RANGEN.length)];
        MutableComponent t = Component.empty().append(rang.naam()).append(" ").append(naam(wie).copy().withStyle(ChatFormatting.WHITE));
        return t.append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(Component.translatable("gui.guhs.lobby.chat.regel." + Math.floorMod(regel, REGELS)).withStyle(ChatFormatting.GRAY));
    }

    /** The players who hear the lobby chat: on the plaza, not in a game. */
    static List<ServerPlayer> publiek(ServerLevel level) {
        List<ServerPlayer> uit = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (Guhpixel.in(p) && Sessies.van(p) == null && !p.isSpectator()) {
                uit.add(p);
            }
        }
        return uit;
    }

    /** Every server tick while somebody is in the dimension: counts down to the next line. */
    static void tick(ServerLevel level, BlockPos oorsprong) {
        if (--wacht > 0) {
            return;
        }
        RandomSource random = level.getRandom();
        wacht = volgendeWacht(random);
        zeg(level, oorsprong, random.nextInt(NAMEN), volgendeRegel(random));
    }

    /** One lobby guh says one line to everybody on the plaza. Returns how many players got it. */
    static int zeg(ServerLevel level, BlockPos oorsprong, int wie, int regel) {
        List<ServerPlayer> publiek = publiek(level);
        if (publiek.isEmpty()) {
            return 0;
        }
        Component tekst = regel(wie, regel);
        for (ServerPlayer p : publiek) {
            p.sendSystemMessage(tekst);
            PxGeluid.speel(p, LobbySlice.CHAT_GELUID.get(), SoundSource.NEUTRAL, 0.35f, 1.6f);
        }
        List<LobbyKaart.Plek> plekken = LobbyKaart.van(level.getServer()).chatguhs();
        if (wie < plekken.size()) {
            Vec3 pos = plek(oorsprong, plekken.get(wie));
            level.sendParticles(ParticleTypes.NOTE, pos.x, pos.y + 1.2, pos.z, 1, 0, 0, 0, 0.5);
        }
        return publiek.size();
    }

    private static Vec3 plek(BlockPos oorsprong, LobbyKaart.Plek plek) {
        return LobbyBorden.plek(oorsprong, plek.pos().getX() + 0.5, plek.pos().getY(), plek.pos().getZ() + 0.5);
    }

    /** Keeps one lobby guh with its own name on every spot (made again when it is missing). Returns how many sit. */
    static int zorg(ServerLevel level, BlockPos oorsprong) {
        List<LobbyKaart.Plek> plekken = LobbyKaart.van(level.getServer()).chatguhs();
        int zitten = 0;
        for (int i = 0; i < plekken.size(); i++) {
            Vec3 pos = plek(oorsprong, plekken.get(i));
            if (!LobbySlice.geladen(level, pos)) {
                continue;
            }
            float yaw = plekken.get(i).yaw();
            List<GuhNpcEntity> hier = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos, pos).inflate(1.2, 2, 1.2),
                    e -> e.getKind() == GuhNpcEntity.Kind.LOBBY_CHATGUH);
            for (int k = 1; k < hier.size(); k++) {
                hier.get(k).discard();
            }
            GuhNpcEntity npc;
            if (hier.isEmpty()) {
                npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
                if (npc == null) {
                    continue;
                }
                npc.setKind(GuhNpcEntity.Kind.LOBBY_CHATGUH);
                npc.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
                npc.setYHeadRot(yaw);
                npc.setYBodyRot(yaw);
                npc.setPersistenceRequired();
                level.addFreshEntity(npc);
            } else {
                npc = hier.get(0);
            }
            // (loading an NPC gives it the name of its kind again: put the gamer name back)
            if (npc.roleData.getIntOr(NAAM, -1) != i || npc.getCustomName() == null || !npc.getCustomName().equals(naam(i))) {
                npc.roleData.putInt(NAAM, i);
                npc.setCustomName(naam(i));
            }
            zitten++;
        }
        return zitten;
    }

    // --- clicked ---------------------------------------------------------------------------------------------------------------

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        int regel = Math.floorMod(npc.getId() * 7 + p.tickCount / 40, REGELS);
        GuhQuests.say(p, npc, "gui.guhs.lobby.chat.regel." + regel);
        PxGeluid.speel(p, LobbySlice.CHAT_GELUID.get(), SoundSource.NEUTRAL, 0.5f, 1.3f);
    }

    /** (Dev, tests) the next line comes at the next tick. */
    static void nu() {
        wacht = 0;
    }

    private Chatguhs() {
    }
}
