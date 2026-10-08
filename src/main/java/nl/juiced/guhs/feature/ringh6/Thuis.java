package nl.juiced.guhs.feature.ringh6;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ringknipoog.Knipogen;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.item.GuhCompassItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.world.ModDimensions;

/**
 * bbq2 (ring-h6): home. After the flight the Rookguhs put the player down in the Guhmensie ({@link #breng}), and there the
 * feast of the Knabbelgouw plays ({@link #seconde}: step 6 of the chapter), which ends the story.
 * <p>
 * Where "home" is ({@link #plek}), the first that exists: this player's own Gouw ({@link #gouw}: Guhdalf's camp where their
 * chapter 1 played, which chapter 1 wrote in their saved data), else the grill portal they last left the Guhmensie through
 * (the verhaal engine remembers it: that is the big barbecueput with the Knabbelgouw, or with Guhdalf's cart in a world that
 * was generated before the update), else the nearest Knabbelgouw, else the nearest barbecueput, else the world's spawn. The
 * player lands on open ground a few steps from it. The spot is saved ({@link #THUIS}), it is what the compass points at
 * for step 6 ("Mijn verhaal"), and the feast starts once the player stands near it; so somebody who logged out in the air
 * or wandered off still gets their party.
 * <p>
 * 1.4.1: the spot is chosen in a world the player is not in yet (they are still on the mountain), so its chunks are not
 * loaded: {@link #landing} loads them first ({@link #laad}; {@link #warm} asks for them in the background while the flight
 * scene plays, so that is no wait) and only ever answers a cell with solid ground under it. Before, the height of an
 * unloaded column read as the bottom of the world: the player was put down there and that spot was saved, so the feast
 * (which asked for 40 blocks in 3D) could never start. Now "near" is measured sideways, the player's own Gouw counts as
 * near too, and a saved spot that is no place to stand is chosen again ({@link #herstel}): whoever was put in the void by
 * 1.4.0 gets their party by walking to their Guhdalf. A player who was just put down can't be hurt for a moment
 * ({@link #netGeland}).
 * <p>
 * The feast is a cutscene ({@link Finale#FEEST}): its actors only exist in the viewer's game, so it needs no building and
 * disturbs nobody's base: it is anchored on the flattest spot near the player ({@link #feestplek}).
 */
public final class Thuis {
    /** Player saved data: where the Rookguhs put this player down (block position, in the Guhmensie). */
    public static final String THUIS = "guhs_ringh6_thuis";
    /** The key of the verhaal engine's portal memory in the player's saved data (feature/verhaal/Doelen). */
    private static final String PORTALEN = "guhs_verhaal_portalen";
    /**
     * Chapter 1's keys in the player's saved data (feature/ringh1/Feest: the block Guhdalf sits on in this player's Gouw, and
     * the id of its dimension). Read by name only: this chapter does not need chapter 1's classes, and a player without
     * them (the keys are written when they meet Guhdalf) simply gets the next choice.
     */
    private static final String GOUW = "guhs_ringh1_thuis", GOUW_DIM = "guhs_ringh1_thuis_dim";
    /** The feast starts within this many blocks (sideways) of the landing spot, this many ticks after landing. */
    public static final int FEEST_BEREIK = 40, FEEST_NA = 60;
    /** How far around home a place to stand is looked for (blocks sideways; up and down). */
    public static final int ZOEK = 12, ZOEK_HOOGTE = 8;
    /** A player who was just put down can't be hurt for this many ticks. */
    public static final int AANKOMST = 200;
    private static final Logger LOGGER = LogUtils.getLogger();

    /** (tests: the test server has no Guhmensie) home is here. */
    @Nullable
    public static ServerLevel testLevel;
    @Nullable
    public static BlockPos testPlek;

    /** (not saved) when the Rookguhs put this player down (game time). */
    private static final Map<UUID, Long> GELAND = new ConcurrentHashMap<>();
    /** (not saved) the chunk that is being loaded in the background for this player's flight home. */
    private static final Map<UUID, ChunkPos> WARM = new ConcurrentHashMap<>();

    /** The level the feast is in: the Guhmensie (tests: the level they set). */
    @Nullable
    static ServerLevel level(ServerPlayer p) {
        return testLevel != null ? testLevel : p.level().getServer().getLevel(ModDimensions.GUHMENSION);
    }

    // =====================================================================================================================
    // where home is
    // =====================================================================================================================

    /**
     * Where this player's home is (see the class comment): a block to stand in, with solid ground under it, or null when
     * there is no Guhmensie (or no place to stand near any of the choices). Loads the chunks it looks at (server thread).
     */
    @Nullable
    static BlockPos plek(ServerPlayer p) {
        if (testLevel != null && testPlek != null) {
            return testPlek;
        }
        ServerLevel level = level(p);
        if (level == null) {
            return null;
        }
        BlockPos rond = bekend(p, level);
        BlockPos plek = rond == null ? null : landing(level, rond);
        if (plek != null) {
            return plek;
        }
        // (nothing known, or no place to stand there: the nearest Knabbelgouw, the nearest barbecueput, the world's spawn)
        BlockPos spawn = level.getRespawnData().pos();
        for (String structuur : new String[]{Ring.STRUCTUREN.get(0), "barbecueput"}) {
            BlockPos midden = GuhCompassItem.findCenter(level, ResourceKey.create(Registries.STRUCTURE, Guhs.id(structuur)), spawn);
            plek = midden == null ? null : landing(level, midden);
            if (plek != null) {
                return plek;
            }
        }
        return landing(level, spawn);
    }

    /**
     * The home this player's own data knows without searching: their Gouw, else the grill portal they last left the
     * Guhmensie through (null: neither).
     */
    @Nullable
    static BlockPos bekend(ServerPlayer p, ServerLevel level) {
        BlockPos gouw = gouw(p, level);
        if (gouw != null) {
            return gouw;
        }
        CompoundTag portalen = GuhQuests.saved(p).getCompoundOrEmpty(PORTALEN);
        String dim = level.dimension().identifier().toString();
        return portalen.contains(dim) ? BlockPos.of(portalen.getLongOr(dim, 0L)) : null;
    }

    /** This player's own Gouw: where Guhdalf sat when their story began, when that is in this level (null: not known). */
    @Nullable
    static BlockPos gouw(ServerPlayer p, ServerLevel level) {
        CompoundTag saved = GuhQuests.saved(p);
        if (!saved.contains(GOUW) || !level.dimension().identifier().toString().equals(saved.getStringOr(GOUW_DIM, ""))) {
            return null;
        }
        return BlockPos.of(saved.getLongOr(GOUW, 0L));
    }

    /**
     * (once a second while the Rookguhs are on their way: the flight scene) asks for the chunks of this player's home in
     * the background (a {@link RingH6Feature#THUIS_TICKET}; nothing waits for it), so {@link #breng} finds them loaded.
     */
    static void warm(ServerPlayer p) {
        ServerLevel level = testLevel != null ? null : level(p);
        if (level == null) {
            return;
        }
        ChunkPos chunk = WARM.get(p.getUUID());
        if (chunk == null) {
            BlockPos rond = bekend(p, level);
            if (rond == null) {
                return;
            }
            chunk = ChunkPos.containing(rond);
            WARM.put(p.getUUID(), chunk);
        }
        level.getChunkSource().addTicketWithRadius(RingH6Feature.THUIS_TICKET.get(), chunk, 1);
    }

    /**
     * Loads (the very first time: generates) the chunks within {@code straal} blocks of this column NOW, so their heights and
     * blocks can be read: the height of a column whose chunk is not loaded reads as the bottom of the world. Server thread
     * only (as GuhPortalForcer and the Bank Guh do it).
     */
    private static void laad(ServerLevel level, BlockPos rond, int straal) {
        for (int cx = (rond.getX() - straal) >> 4; cx <= (rond.getX() + straal) >> 4; cx++) {
            for (int cz = (rond.getZ() - straal) >> 4; cz <= (rond.getZ() + straal) >> 4; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }

    /**
     * A place to stand near this spot: a cell with a solid block under it and room for a player, no fluid ({@link #staan}).
     * The first that exists: open ground under the sky 4-9 steps away at about this height (what the Rookguhs always
     * chose); the nearest place to stand within {@link #ZOEK} blocks at about this height (under a roof, on a ledge); open
     * ground under the sky 4-9 steps away at any height (for a spot whose own height says nothing). Null: there is none.
     * Never a spot without ground. The chunks around the spot are loaded first.
     */
    @Nullable
    static BlockPos landing(ServerLevel level, BlockPos rond) {
        laad(level, rond, ZOEK);
        BlockPos hemel = onderDeHemel(level, rond, true);
        if (hemel != null) {
            return hemel;
        }
        BlockPos beste = null;
        double besteD = Double.MAX_VALUE;
        for (int dx = -ZOEK; dx <= ZOEK; dx++) {
            for (int dz = -ZOEK; dz <= ZOEK; dz++) {
                if (dx * dx + dz * dz < 4) {
                    continue;                                 // (not on top of Guhdalf, not in the portal)
                }
                for (int dy = ZOEK_HOOGTE; dy >= -ZOEK_HOOGTE; dy--) {
                    double d = dx * dx + dz * dz + 4.0 * dy * dy;
                    BlockPos voet = rond.offset(dx, dy, dz);
                    if (d < besteD && staan(level, voet)) {
                        beste = voet;
                        besteD = d;
                    }
                }
            }
        }
        return beste != null ? beste : onderDeHemel(level, rond, false);
    }

    /** Open ground under the sky 4-9 steps from this spot ({@code opHoogte}: within 8 blocks of its height), or null. */
    @Nullable
    private static BlockPos onderDeHemel(ServerLevel level, BlockPos rond, boolean opHoogte) {
        for (int straal = 4; straal <= 9; straal++) {
            for (int i = 0; i < 16; i++) {
                double hoek = Math.PI * 2 * i / 16;
                int x = rond.getX() + (int) Math.round(Math.cos(hoek) * straal), z = rond.getZ() + (int) Math.round(Math.sin(hoek) * straal);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos voet = new BlockPos(x, y, z);
                if ((!opHoogte || Math.abs(y - rond.getY()) <= 8) && staan(level, voet)) {
                    return voet;
                }
            }
        }
        return null;
    }

    /** Is this a cell to stand in: inside the world, a solid block under it, room for a player, no fluid? */
    static boolean staan(ServerLevel level, BlockPos voet) {
        if (voet.getY() <= level.getMinY() || voet.getY() + 1 > level.getMaxY()) {
            return false;
        }
        BlockState onder = level.getBlockState(voet.below());
        return onder.blocksMotion() && onder.getFluidState().isEmpty() && !level.getBlockState(voet).blocksMotion() && level.getFluidState(voet).isEmpty()
                && !level.getBlockState(voet.above()).blocksMotion() && level.getFluidState(voet.above()).isEmpty();
    }

    // =====================================================================================================================
    // the saved spot
    // =====================================================================================================================

    /** The saved landing spot of this player (null: none yet). */
    @Nullable
    static BlockPos thuis(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        return saved.contains(THUIS) ? BlockPos.of(saved.getLongOr(THUIS, 0L)) : null;
    }

    /**
     * (step 6) The saved landing spot, made good when it is no place to stand: 1.4.0 saved the bottom of the world for
     * whoever flew home to chunks that were not loaded. A spot outside the world is chosen again at once (wherever the
     * player is: the compass points at it), any other spot only when its chunk is loaded anyway and nobody can stand
     * there any more. Chosen again = the same choice as the flight makes ({@link #plek}), else a place to stand around the
     * spot's own column, else no spot at all (then the feast is wherever they stand in the Guhmensie). Null: no spot.
     */
    @Nullable
    static BlockPos herstel(ServerPlayer p, ServerLevel level) {
        BlockPos thuis = thuis(p);
        if (thuis == null || (testLevel != null && testPlek != null)) {
            return thuis;
        }
        boolean buiten = thuis.getY() <= level.getMinY() || thuis.getY() + 1 > level.getMaxY();
        if (!buiten && (!level.isLoaded(thuis) || staan(level, thuis))) {
            return thuis;
        }
        BlockPos beter = plek(p);
        if (beter == null) {
            beter = landing(level, thuis);
        }
        CompoundTag saved = GuhQuests.saved(p);
        if (beter == null) {
            saved.remove(THUIS);
        } else {
            saved.putLong(THUIS, beter.asLong());
        }
        LOGGER.info("Guhs: the home of {} for the feast of the Knabbelring was no place to stand ({}): now {}", p.getGameProfile().name(), thuis.toShortString(),
                beter == null ? "wherever they stand in the Guhmensie" : beter.toShortString());
        return beter;
    }

    /** What the compass and Sam-guh point at for step 6: the landing spot, or (none yet) the Knabbelgouw. */
    static Doel doel(ServerPlayer p) {
        BlockPos thuis = thuis(p);
        ServerLevel level = level(p);
        if (thuis == null || level == null) {
            return Ring.doel(1);
        }
        return Doel.plek(level.dimension(), thuis, Component.translatable("gui.guhs.ringh6.doel.thuis"));
    }

    // =====================================================================================================================
    // the flight home, the feast
    // =====================================================================================================================

    /**
     * (the daarna of the flight scene) Step 5 -> 6: the player is put down at home, in the Guhmensie. Nothing is lost on the
     * way; Sam-guh turns up next to them by himself.
     */
    static void breng(ServerPlayer p) {
        Verhaallijn lijn = RingH6Feature.LIJN;
        if (!Ring.aanZet(p, lijn, 5) || !lijn.vlag(p, Finale.GEFRITUURD)) {
            return;
        }
        ServerLevel level = level(p);
        BlockPos plek = plek(p);
        WARM.remove(p.getUUID());
        if (level == null || plek == null) {
            // (no Guhmensie in this world, or nowhere to put them down: nobody is ever put down without ground; the feast
            // comes to wherever they stand, in the Guhmensie when there is one)
            GuhQuests.saved(p).remove(THUIS);
            lijn.verder(p, 5);
            if (level != null) {
                GuhQuests.hint(p, "quest.guhs.ringh6.thuis.ver");
            }
            return;
        }
        GuhQuests.saved(p).putLong(THUIS, plek.asLong());
        lijn.verder(p, 5);
        if (p.isPassenger()) {
            p.stopRiding();
        }
        Duwtje.terug(p, level.dimension(), Vec3.atBottomCenterOf(plek), p.getYRot());
        GELAND.put(p.getUUID(), level.getGameTime());
        p.sendSystemMessage(Component.translatable("quest.guhs.ringh6.thuis").withStyle(ChatFormatting.GOLD));
    }

    /** Was this player put down at home a moment ago ({@link #AANKOMST} ticks: nothing hurts them while they arrive)? */
    static boolean netGeland(ServerPlayer p) {
        Long geland = GELAND.get(p.getUUID());
        if (geland == null) {
            return false;
        }
        long sinds = p.level().getGameTime() - geland;
        return sinds >= 0 && sinds < AANKOMST;
    }

    /** Is this player within {@link #FEEST_BEREIK} blocks of this spot, measured sideways (the height does not count)? */
    static boolean dichtbij(ServerPlayer p, BlockPos plek) {
        double dx = p.getX() - (plek.getX() + 0.5), dz = p.getZ() - (plek.getZ() + 0.5);
        return dx * dx + dz * dz <= (double) FEEST_BEREIK * FEEST_BEREIK;
    }

    /**
     * (once a second, step 6) at home, near the landing spot or their own Gouw, on the ground: the feast. Nothing here is
     * kept in the player entity: whoever died, logged out or walked off between the flight and the feast gets it the
     * moment they stand there again.
     */
    static void seconde(ServerPlayer p) {
        Verhaallijn lijn = RingH6Feature.LIJN;
        ServerLevel level = level(p);
        if (!Ring.aanZet(p, lijn, 6)) {
            return;
        }
        BlockPos thuis = level == null ? null : herstel(p, level);
        if (thuis != null && p.level() == level && netGeland(p) && p.getY() < level.getMinY()) {
            // (just put down and under the world all the same: back on the spot, before anything can happen)
            Duwtje.terug(p, level.dimension(), Vec3.atBottomCenterOf(thuis), p.getYRot());
            return;
        }
        BlockPos gouw = level == null ? null : gouw(p, level);
        boolean hier = level == null || (p.level() == level && (thuis == null || dichtbij(p, thuis) || (gouw != null && dichtbij(p, gouw))));
        if (!hier) {
            if (p.tickCount % 1200 < 20) {
                GuhQuests.hint(p, "quest.guhs.ringh6.thuis.ver");
            }
            return;
        }
        Long geland = GELAND.get(p.getUUID());
        if (!Duwtje.mag(p) || !p.onGround() || (geland != null && p.level().getGameTime() - geland >= 0 && p.level().getGameTime() - geland < FEEST_NA)) {
            return;
        }
        Sam.roep(p);
        BlockPos anker = feestplek(p.level(), p.blockPosition());
        // (ring-knipogen: the first time a late guest shuffles in right after the feast)
        Knipogen.speel(p, Finale.FEEST, Knipogen.SJOKKEL, anker, Rotation.NONE, Thuis::klaar);
    }

    /** The feast is over: the questline is done; ring-kern gives the rewards of the whole story when its last step falls. */
    static void klaar(ServerPlayer p) {
        Verhaallijn lijn = RingH6Feature.LIJN;
        if (!lijn.verder(p, 6)) {
            return;
        }
        GELAND.remove(p.getUUID());
        p.sendSystemMessage(Component.translatable("quest.guhs.ringh6.feest.klaar").withStyle(ChatFormatting.GOLD));
    }

    /**
     * Where the feast is played: the middle of the flattest patch of open ground (11 x 11) within 16 blocks of this spot
     * (the cutscene's actors stand within 5 blocks of its anchor, at the anchor's height). When nothing near is flat, the
     * spot itself.
     */
    static BlockPos feestplek(ServerLevel level, BlockPos rond) {
        BlockPos beste = rond;
        int besteScore = -1;
        for (int dx = -16; dx <= 16; dx += 4) {
            for (int dz = -16; dz <= 16; dz += 4) {
                int x = rond.getX() + dx, z = rond.getZ() + dz;
                BlockPos midden = oppervlak(level, x, z, rond.getY());
                if (midden == null) {
                    continue;
                }
                int score = 0;
                for (int ax = -5; ax <= 5; ax += 2) {
                    for (int az = -5; az <= 5; az += 2) {
                        BlockPos q = oppervlak(level, x + ax, z + az, midden.getY());
                        if (q != null && Math.abs(q.getY() - midden.getY()) <= 1 && !level.getBlockState(q.above(2)).blocksMotion()) {
                            score += q.getY() == midden.getY() ? 2 : 1;
                        }
                    }
                }
                score -= (Math.abs(dx) + Math.abs(dz)) / 8;       // (a little closer is a little better)
                if (score > besteScore) {
                    besteScore = score;
                    beste = midden;
                }
            }
        }
        return beste;
    }

    /** The cell to stand in at this column, within 6 blocks of height {@code y} (null: none: a hole, a wall, water). */
    @Nullable
    private static BlockPos oppervlak(ServerLevel level, int x, int z, int y) {
        for (int dy = 6; dy >= -6; dy--) {
            BlockPos voet = new BlockPos(x, y + dy, z);
            if (staan(level, voet)) {
                return voet;
            }
        }
        return null;
    }

    /** (a logout) nothing of this player's flight is kept. */
    static void vergeet(UUID speler) {
        GELAND.remove(speler);
        WARM.remove(speler);
    }

    private Thuis() {
    }
}
