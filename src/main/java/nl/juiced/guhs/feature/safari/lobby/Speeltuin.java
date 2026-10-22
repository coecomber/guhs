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
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
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
 * <p>
 * Test round 5: more visitors ({@link #SPELERS}), and they play far more than guhs elsewhere: an idle visitor looks for a
 * toy every {@link #KIJK} ticks, so you always see the toys in use. The safari animal toys of the giftshop (speelbal,
 * schommelband, krabboom, glijbaantje) stand there too: a guh runs to one and plays its own little game there (rolling,
 * dancing, a vahoeg) for a while.
 */
public final class Speeltuin {
    public static final String TAG = "guhs_safari_speler";
    static final int SPELERS = 10;
    private static final int ELKE = 200;
    /** Test round 5: how often an idle visitor guh looks for a toy (each time: 3 in 4 it starts to play). */
    private static final int KIJK = 40;
    /** The safari animal toys found in the playground at the last upkeep. */
    private static final List<BlockPos> DIERSPEELGOED = new java.util.ArrayList<>();
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
        boolean onderhoud = server.getTickCount() % ELKE == 29, kijk = server.getTickCount() % KIJK == 13;
        if (!onderhoud && !kijk || level.players().isEmpty() || !ParkBouwer.klaar(server)) {
            return;
        }
        BlockPos c = b.speeltuin;
        if (!level.hasChunk(c.getX() >> 4, c.getZ() >> 4) || !level.areEntitiesLoaded(ChunkPos.pack(c))) {
            return;
        }
        if (onderhoud) {
            for (Beleving.Speelgoed t : b.speelgoed) {
                zorg(level, t);
            }
            zoekDierspeelgoed(level, c, b.speeltuinStraal + 2);
        }
        List<GuhEntity> er = level.getEntitiesOfClass(GuhEntity.class, new AABB(c).inflate(b.speeltuinStraal + 20, 12, b.speeltuinStraal + 20),
                g -> g.entityTags().contains(TAG));
        for (GuhEntity g : er) {
            if (g.distanceToSqr(c.getX() + 0.5, g.getY(), c.getZ() + 0.5) > (double) (b.speeltuinStraal + 4) * (b.speeltuinStraal + 4)) {
                stop(g);
                BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c);
                g.teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            } else if (kijk && !SPEELT.containsKey(g.getUUID()) && !g.isPassenger() && g.getRandom().nextInt(4) != 0) {
                KlusTaak taak = !DIERSPEELGOED.isEmpty() && g.getRandom().nextInt(3) == 0 ? null
                        : Speelgoed.willekeurig(level, g, c, b.speeltuinStraal + 2);
                if (taak == null && !DIERSPEELGOED.isEmpty()) {
                    taak = new DierSpeeltje(g, DIERSPEELGOED.get(g.getRandom().nextInt(DIERSPEELGOED.size())), level);
                }
                if (taak != null) {
                    SPEELT.put(g.getUUID(), taak);
                    GuhHooks.bezig(g, 40);
                }
            }
        }
        if (!onderhoud) {
            return;
        }
        for (int i = er.size(); i < SPELERS; i++) {
            Bezoekers.maak(level, level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, c), TAG);
        }
    }

    /** Test round 5: the safari animal toys (verblijf Speeltje blocks) standing in the playground. */
    private static void zoekDierspeelgoed(ServerLevel level, BlockPos c, int straal) {
        DIERSPEELGOED.clear();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -straal; dx <= straal; dx++) {
            for (int dz = -straal; dz <= straal; dz++) {
                for (int dy = -2; dy <= 4; dy++) {
                    p.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (level.getBlockState(p).getBlock() instanceof nl.juiced.guhs.feature.safari.verblijf.VerblijfBlokken.Speeltje) {
                        DIERSPEELGOED.add(p.immutable());
                    }
                }
            }
        }
    }

    /**
     * Test round 5: a visitor guh plays at a safari animal toy: it runs there and does a little game for a few seconds (a
     * ball or slide: rolling; the tyre swing: dancing; the scratching post: a vahoeg), then goes off again.
     */
    static final class DierSpeeltje implements KlusTaak {
        private final GuhEntity g;
        private final BlockPos toy;
        private final Emote emote;
        private int gespeeld = -1;

        DierSpeeltje(GuhEntity g, BlockPos toy, ServerLevel level) {
            this.g = g;
            this.toy = toy;
            String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(level.getBlockState(toy).getBlock()).getPath();
            this.emote = id.contains("schommel") ? Emote.DANSEN : id.contains("krab") ? Emote.VAHOEG : Emote.ROLLEN;
        }

        @Override
        public boolean tick() {
            if (gespeeld >= 0) {
                return ++gespeeld < 120;
            }
            if (g.distanceToSqr(toy.getX() + 0.5, g.getY(), toy.getZ() + 0.5) < 3.2) {
                g.getNavigation().stop();
                g.getLookControl().setLookAt(toy.getX() + 0.5, toy.getY() + 0.5, toy.getZ() + 0.5);
                g.emotes.start(emote, true, GuhEmotes.Source.SELF);
                gespeeld = 0;
                return true;
            }
            if (g.getNavigation().isDone()) {
                g.getNavigation().moveTo(toy.getX() + 0.5, toy.getY(), toy.getZ() + 1.5, 1.0);
            }
            return true;
        }

        @Override
        public void stop() {
            g.emotes.stop();
        }

        @Override
        public int maxTicks() {
            return 400;
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
