package nl.juiced.guhs.feature.safari.lobby;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.huisje.Speelgoed;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.safari.ParkBouwer;
import nl.juiced.guhs.feature.safari.Safari;
import nl.juiced.guhs.feature.speelgoed.KnabbelbalEntity;
import nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature;
import nl.juiced.guhs.feature.speelgoed.ToestelBlock;

/**
 * The guh-speeltuin of the lobby (test round 3, design ronde 45; bigger in round 4): a fenced sand square with
 * guh-schommels, a guh-glijbaantje and guh-wippen. The toys are multi-blocks with a block entity, so the game places them
 * (not the tiles): whenever one is missing it is built again; the same for the knabbelbal (an entity). The plain-block
 * toys (pluizige tunnel, bellenblaas, luchtscooter, hapluikje) are in the tiles. Visitor guhs in safari clothes live
 * there and play with them now and then (the toys' own sessions, as in a guhhuisje); your own tamed guhs play there by
 * themselves when you are near (the speelgoed slice's SpeelGoal).
 */
public final class Speeltuin {
    public static final String TAG = "guhs_safari_speler";
    static final int SPELERS = 6;
    private static final int ELKE = 200;
    /** Server: the toy session each playing visitor guh is in. */
    private static final Map<UUID, KlusTaak> SPEELT = new HashMap<>();
    private static final Map<UUID, Integer> DUUR = new HashMap<>();

    static void tick(MinecraftServer server) {
        ServerLevel level = Safari.level(server);
        Beleving b = Beleving.get();
        if (level == null || b.speeltuin == null) {
            return;
        }
        speel(level);
        if (server.getTickCount() % ELKE != 29 || level.players().isEmpty() || !ParkBouwer.klaar(server)) {
            return;
        }
        BlockPos c = b.speeltuin;
        if (!level.hasChunk(c.getX() >> 4, c.getZ() >> 4) || !level.areEntitiesLoaded(ChunkPos.pack(c))) {
            return;
        }
        for (Beleving.Speelgoed t : b.speelgoed) {
            zorg(level, t);
        }
        List<GuhEntity> er = level.getEntitiesOfClass(GuhEntity.class, new AABB(c).inflate(b.speeltuinStraal + 20, 12, b.speeltuinStraal + 20),
                g -> g.entityTags().contains(TAG));
        for (GuhEntity g : er) {
            if (g.distanceToSqr(c.getX() + 0.5, g.getY(), c.getZ() + 0.5) > (double) (b.speeltuinStraal + 4) * (b.speeltuinStraal + 4)) {
                stop(g);
                BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c);
                g.teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            } else if (!SPEELT.containsKey(g.getUUID()) && !g.isPassenger() && g.getRandom().nextInt(2) == 0) {
                KlusTaak taak = Speelgoed.willekeurig(level, g, c, b.speeltuinStraal + 2);
                if (taak != null) {
                    SPEELT.put(g.getUUID(), taak);
                    GuhHooks.bezig(g, 40);
                }
            }
        }
        for (int i = er.size(); i < SPELERS; i++) {
            Bezoekers.maak(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c), TAG);
        }
    }

    /** The toy at this spot, built again when it is gone (its spot and parts free). */
    static void zorg(ServerLevel level, Beleving.Speelgoed t) {
        if (t.soort().equals("knabbelbal")) {
            if (level.getEntitiesOfClass(KnabbelbalEntity.class, new AABB(t.pos()).inflate(Beleving.get().speeltuinStraal + 2, 4,
                    Beleving.get().speeltuinStraal + 2)).isEmpty()) {
                KnabbelbalEntity.maak(level, Vec3.atBottomCenterOf(t.pos()), true);
            }
            return;
        }
        Block blok = switch (t.soort()) {
            case "schommel" -> SpeelgoedFeature.SCHOMMEL.get();
            case "glijbaantje" -> SpeelgoedFeature.GLIJBAANTJE.get();
            case "wip" -> SpeelgoedFeature.WIP.get();
            default -> null;
        };
        if (blok == null) {
            return;
        }
        if (level.getBlockState(t.pos()).is(blok) || !(blok instanceof ToestelBlock toestel)) {
            return;
        }
        for (BlockPos p : toestel.blokken(t.pos(), t.facing())) {
            if (!level.getBlockState(p).canBeReplaced()) {
                return;
            }
        }
        ToestelBlock.bouw(level, t.pos(), toestel, t.facing());
    }

    /** Every tick: the toy sessions of the playing visitor guhs. */
    private static void speel(ServerLevel level) {
        if (SPEELT.isEmpty()) {
            return;
        }
        SPEELT.entrySet().removeIf(e -> {
            Entity en = level.getEntity(e.getKey());
            if (!(en instanceof GuhEntity g) || g.isRemoved()) {
                return true;
            }
            GuhHooks.bezig(g, 40);
            int n = DUUR.merge(e.getKey(), 1, Integer::sum);
            boolean verder;
            try {
                verder = n <= e.getValue().maxTicks() && e.getValue().tick();
            } catch (RuntimeException ex) {
                verder = false;
            }
            if (!verder) {
                e.getValue().stop();
                DUUR.remove(e.getKey());
            }
            return !verder;
        });
    }

    private static void stop(GuhEntity g) {
        DUUR.remove(g.getUUID());
        KlusTaak t = SPEELT.remove(g.getUUID());
        if (t != null) {
            t.stop();
        }
    }

    private Speeltuin() {
    }
}
