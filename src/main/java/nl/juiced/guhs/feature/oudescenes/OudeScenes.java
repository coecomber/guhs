package nl.juiced.guhs.feature.oudescenes;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.barbecuether.Grillguh;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.hemel.HemelQuest;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;
import nl.juiced.guhs.feature.timmerguh.TimmerguhVoortgang;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.VerhaalPayloads;
import nl.juiced.guhs.feature.verhaal.VerhaalSync;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (oude-scenes, DESIGN_VERHALENPAD B): one camera scene for each older story, played with the verhaal engine at the
 * story's own big moment:
 * <ul>
 *   <li>{@link #BALTO}: the dieptepunt of the medicine ride (BaltoVerhaal.opMoment): the white wolf-guh;</li>
 *   <li>{@link #MEWTWO}: the professor has all six lab notes (MewtwoVerhaal.notitiesKlaar): what he remembers of day 45;</li>
 *   <li>{@link #OHANA}: back at Lilo-guh after 626-guh got his three lovely things (Ohana.ohana);</li>
 *   <li>{@link #HEMEL}: the wolkenhoeder has all three things (Wolkenhoeder.wordtWakker): the Knuffelhart starts to beat;</li>
 *   <li>{@link #GRILL}: the Grillguh's pit burns again (Grillguh.complete);</li>
 *   <li>{@link #TIMMER}: the last tuft of the Timmerguh's roof is laid (Timmerguh.dakAf).</li>
 * </ul>
 * The old feature calls {@link #speel} FIRST and does what it always did in the scene's {@code daarna}: so a scene that is
 * broken off (a logout) changes nothing and simply plays again at the next try. Each scene plays once per player
 * ({@code Cutscenes.gezien}); after that the story goes as it always went. The Guhdex replays it (the scene is registered
 * with {@code .bij(<the story's questline>)}).
 * <p>
 * A player who was already past a moment when this was added never gets the scene forced on them: {@link #bijwerken} puts
 * it in their Guhdex as watched, so they can look at it there (as a picture book, or in the world when they are near the
 * building), and nothing else happens.
 * <p>
 * Where a scene plays: the copy of its structure at the spot ({@link Bezetting#start}, {@link Kopieen}): the scene's anchor
 * block and the copy's rotation. Before it plays, every camera position is tried against the real blocks ({@link #vrij}):
 * a player may have built in the picture. No free camera = no scene (the story goes on as before).
 * Resources: tools/features/oude_scenes.py.
 */
public final class OudeScenes {
    public static final OudeScene BALTO = new OudeScene("balto", Scenes.BALTO, Scenes.BALTO_STRUCTUUR, Scenes.BALTO_STUK, Scenes.BALTO_ANKER,
            Scenes.BALTO_EFFECTEN, p -> BaltoVerhaal.stap(p) > BaltoVerhaal.TERUG);
    public static final OudeScene MEWTWO = new OudeScene("mewtwo", Scenes.MEWTWO, Scenes.MEWTWO_STRUCTUUR, Scenes.MEWTWO_STUK, Scenes.MEWTWO_ANKER,
            Scenes.MEWTWO_EFFECTEN, p -> MewtwoVoortgang.stap(p) > MewtwoVoortgang.NOTITIES);
    public static final OudeScene OHANA = new OudeScene("ohana", Scenes.OHANA, Scenes.OHANA_STRUCTUUR, Scenes.OHANA_STUK, Scenes.OHANA_ANKER,
            Scenes.OHANA_EFFECTEN, p -> Ohana.stap(p) > Ohana.OHANA);
    public static final OudeScene HEMEL = new OudeScene("hemel", Scenes.HEMEL, Scenes.HEMEL_STRUCTUUR, Scenes.HEMEL_STUK, Scenes.HEMEL_ANKER,
            Scenes.HEMEL_EFFECTEN, HemelQuest::klopt);
    public static final OudeScene GRILL = new OudeScene("grill", Scenes.GRILL, Scenes.GRILL_STRUCTUUR, Scenes.GRILL_STUK, Scenes.GRILL_ANKER,
            Scenes.GRILL_EFFECTEN, p -> Grillguh.step(p) >= Grillguh.DONE);
    public static final OudeScene TIMMER = new OudeScene("timmer", Scenes.TIMMER, Scenes.TIMMER_STRUCTUUR, Scenes.TIMMER_STUK, Scenes.TIMMER_ANKER,
            Scenes.TIMMER_EFFECTEN, p -> TimmerguhVoortgang.stap(p) > TimmerguhVoortgang.DAK);
    public static final List<OudeScene> ALLE = List.of(BALTO, MEWTWO, OHANA, HEMEL, GRILL, TIMMER);

    /** How often (ticks) a player's Guhdex is brought up to date ({@link #bijwerken}). */
    static final int BIJWERK_TICKS = 200;
    /** A camera position is tried every this many ticks of the script. */
    private static final int PROEF_STAP = 10;
    /** (game tests) mock players that do get the scenes (a mock player "watches" two ticks). */
    private static final Set<UUID> PROEF = ConcurrentHashMap.newKeySet();

    /** Where a scene plays: its anchor block in the world and how its building is turned. */
    public record Plek(BlockPos anker, Rotation draai) {
    }

    @Nullable
    public static OudeScene van(@Nullable String naam) {
        for (OudeScene s : ALLE) {
            if (s.naam().equals(naam) || s.id().equals(naam)) {
                return s;
            }
        }
        return null;
    }

    /** The scene of ours that this script is (null: somebody else's). */
    @Nullable
    public static OudeScene van(@Nullable Cutscene scene) {
        for (OudeScene s : ALLE) {
            if (s.scene() == scene) {
                return s;
            }
        }
        return null;
    }

    /**
     * The story is at this scene's moment for this player, at {@code bij} (the spot of the story: the NPC, the heart, the
     * sled). True: the scene plays now, for the first time, and {@code daarna} runs when it was watched to the end (do
     * there what the story does next). False: nothing happens here (they saw it before, they are watching something else,
     * or no camera of it is free): go on with the story as always.
     */
    public static boolean speel(ServerPlayer p, OudeScene s, BlockPos bij, @Nullable Consumer<ServerPlayer> daarna) {
        if (!p.isAlive() || Cutscenes.gezien(p, s.id()) || Cutscenes.bezig(p) || !kanKijken(p)) {
            return false;
        }
        Plek plek = plek(p.level(), s, bij);
        return plek != null && Cutscenes.speel(p, s.scene(), plek.anker(), plek.draai(), daarna);
    }

    /** Can this player's game show a scene? (Not the mock players of the game tests, unless a test asks for it.) */
    private static boolean kanKijken(ServerPlayer p) {
        if (PROEF.contains(p.getUUID())) {
            return true;
        }
        return !(p instanceof net.neoforged.neoforge.common.util.FakePlayer) && p.connection != null && p.connection.hasChannel(VerhaalPayloads.Speel.TYPE);
    }

    /** (game tests) this mock player gets the scenes as a real player would (or not any more). */
    public static void proef(ServerPlayer p, boolean aan) {
        if (aan) {
            PROEF.add(p.getUUID());
        } else {
            PROEF.remove(p.getUUID());
        }
    }

    // =====================================================================================================================
    // where
    // =====================================================================================================================

    /**
     * Where this scene plays for something that happens at {@code bij}: at the copy of its structure there (its anchor
     * block, its rotation). Without a copy (an NPC an operator put somewhere, a test room): anchored on {@code bij}
     * itself, turned so that the cameras are free. Null: no way to hang every camera in free air.
     */
    @Nullable
    public static Plek plek(ServerLevel level, OudeScene s, BlockPos bij) {
        Plek kopie = kopie(level, s, bij);
        if (kopie != null) {
            return vrij(level, s.scene(), kopie.anker(), kopie.draai()) ? kopie : null;
        }
        for (Rotation draai : Rotation.values()) {
            if (vrij(level, s.scene(), bij, draai)) {
                return new Plek(bij, draai);
            }
        }
        return null;
    }

    /** The scene's anchor and rotation in the copy of its structure at this spot (null: there is no such copy here). */
    @Nullable
    public static Plek kopie(ServerLevel level, OudeScene s, BlockPos bij) {
        StructureStart start = Bezetting.start(level, s.structuur(), bij);
        BlockPos anker = start == null ? null : Kopieen.wereld(start, s.stuk(), s.anker());
        return anker == null ? null : new Plek(anker, Kopieen.draai(start, s.stuk()));
    }

    /**
     * Does every camera position of the scene hang in free air here (no full block but a barrier, no fluid)? Tried at every keyframe and
     * every {@value #PROEF_STAP} ticks of the glides between them; chunks that are not loaded count as free.
     */
    public static boolean vrij(ServerLevel level, Cutscene scene, BlockPos anker, Rotation draai) {
        return geblokkeerd(level, scene, anker, draai) == null;
    }

    /** The first camera position of the scene that does not hang in free air here ("t=40 at 1, 64, 2: minecraft:stone"), or null. */
    @Nullable
    public static String geblokkeerd(ServerLevel level, Cutscene scene, BlockPos anker, Rotation draai) {
        for (int t = 0; t <= scene.duur(); t += PROEF_STAP) {
            String blok = geblokkeerd(level, scene, anker, draai, t);
            if (blok != null) {
                return blok;
            }
        }
        for (Cutscene.CameraPunt punt : scene.camera()) {
            String blok = geblokkeerd(level, scene, anker, draai, punt.t());
            if (blok != null) {
                return blok;
            }
        }
        return null;
    }

    @Nullable
    private static String geblokkeerd(ServerLevel level, Cutscene scene, BlockPos anker, Rotation draai, int t) {
        Vec3[] cam = scene.cameraOp(t);
        if (cam == null) {
            return null;
        }
        BlockPos pos = BlockPos.containing(Cutscene.wereld(anker, draai, cam[0]));
        if (!level.isLoaded(pos)) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        // (a barrier is no wall for a camera: it is invisible, from inside too)
        return state.isCollisionShapeFullBlock(level, pos) && !state.is(net.minecraft.world.level.block.Blocks.BARRIER) || !state.getFluidState().isEmpty()
                ? "t=" + t + " at " + pos.toShortString() + ": " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()) : null;
    }

    // =====================================================================================================================
    // players who are past a moment already
    // =====================================================================================================================

    private static String sleutel(OudeScene s) {
        return "guhs_scene_" + s.id();          // (the verhaal engine's own key of a scene: Cutscenes.gezien reads it)
    }

    /**
     * For every scene whose moment this player has behind them without having seen it (they were further when this was
     * added, an operator set their step, the scene could not play): it counts as watched, so the Guhdex offers it. And for
     * a watched scene that has no place yet: when the player is at a copy of its building, a replay plays there from now
     * on (until then: the picture book). Returns how many scenes changed.
     */
    public static int bijwerken(ServerPlayer p) {
        int n = 0;
        CompoundTag bewaard = GuhQuests.saved(p);
        for (OudeScene s : ALLE) {
            CompoundTag t = bewaard.getCompoundOrEmpty(sleutel(s));
            boolean gezien = t.getBooleanOr("Gezien", false);
            if (!gezien && !s.voorbij().test(p)) {
                continue;
            }
            boolean anders = false;
            t = t.copy();
            if (!gezien) {
                t.putBoolean("Gezien", true);
                anders = true;
            }
            if (!t.contains("Dim")) {
                Plek plek = kopie(p.level(), s, p.blockPosition());
                if (plek != null) {
                    t.putString("Dim", p.level().dimension().identifier().toString());
                    t.putLong("Pos", plek.anker().asLong());
                    t.putInt("Draai", plek.draai().ordinal());
                    anders = true;
                }
            }
            if (anders) {
                bewaard.put(sleutel(s), t);
                n++;
            }
        }
        if (n > 0) {
            VerhaalSync.sync(p);
        }
        return n;
    }

    /** (dev command, tests) forgets that this player saw the scene. */
    public static void vergeet(ServerPlayer p, OudeScene s) {
        Cutscenes.vergeet(p, s.id());
    }

    // --- events (registered by OudeScenesFeature) -----------------------------------------------------------------------

    static void opTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % BIJWERK_TICKS == 0 && !Cutscenes.bezig(p)) {
            bijwerken(p);
        }
    }

    static void opLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            bijwerken(p);
        }
    }

    static void opLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        PROEF.remove(event.getEntity().getUUID());
    }

    private OudeScenes() {
    }
}
