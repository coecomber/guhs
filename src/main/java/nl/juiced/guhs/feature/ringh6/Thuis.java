package nl.juiced.guhs.feature.ringh6;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.ringknipoog.Knipogen;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
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
    /** The feast starts within this many blocks of the landing spot, this many ticks after landing. */
    public static final int FEEST_BEREIK = 40, FEEST_NA = 60;

    /** (tests: the test server has no Guhmensie) home is here. */
    @Nullable
    public static ServerLevel testLevel;
    @Nullable
    public static BlockPos testPlek;

    private static final Map<UUID, Long> GELAND = new ConcurrentHashMap<>();

    /** The level the feast is in: the Guhmensie (tests: the level they set). */
    @Nullable
    static ServerLevel level(ServerPlayer p) {
        return testLevel != null ? testLevel : p.level().getServer().getLevel(ModDimensions.GUHMENSION);
    }

    /** Where this player's home is (see the class comment): a block to stand in, or null when there is no Guhmensie. */
    @Nullable
    static BlockPos plek(ServerPlayer p) {
        if (testLevel != null && testPlek != null) {
            return testPlek;
        }
        ServerLevel level = level(p);
        if (level == null) {
            return null;
        }
        BlockPos rond = gouw(p, level);
        if (rond == null) {
            CompoundTag portalen = GuhQuests.saved(p).getCompoundOrEmpty(PORTALEN);
            String dim = level.dimension().identifier().toString();
            rond = portalen.contains(dim) ? BlockPos.of(portalen.getLongOr(dim, 0L)) : null;
        }
        if (rond == null) {
            BlockPos spawn = level.getRespawnData().pos();
            rond = GuhCompassItem.findCenter(level, ResourceKey.create(Registries.STRUCTURE, Guhs.id(Ring.STRUCTUREN.get(0))), spawn);
            if (rond == null) {
                rond = GuhCompassItem.findCenter(level, ResourceKey.create(Registries.STRUCTURE, Guhs.id("barbecueput")), spawn);
            }
            if (rond == null) {
                rond = spawn;
            }
        }
        return landing(level, rond);
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

    /** Open ground a few steps from this spot: a block with two free cells above it, not a fluid, not the portal itself. */
    static BlockPos landing(ServerLevel level, BlockPos rond) {
        BlockPos beste = null;
        for (int straal = 4; straal <= 9 && beste == null; straal++) {
            for (int i = 0; i < 16 && beste == null; i++) {
                double hoek = Math.PI * 2 * i / 16;
                int x = rond.getX() + (int) Math.round(Math.cos(hoek) * straal), z = rond.getZ() + (int) Math.round(Math.sin(hoek) * straal);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos voet = new BlockPos(x, y, z);
                if (Math.abs(y - rond.getY()) <= 8 && staan(level, voet)) {
                    beste = voet;
                }
            }
        }
        if (beste != null) {
            return beste;
        }
        return new BlockPos(rond.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, rond.getX(), rond.getZ()), rond.getZ());
    }

    private static boolean staan(ServerLevel level, BlockPos voet) {
        BlockState onder = level.getBlockState(voet.below());
        return onder.blocksMotion() && onder.getFluidState().isEmpty() && !level.getBlockState(voet).blocksMotion() && level.getFluidState(voet).isEmpty()
                && !level.getBlockState(voet.above()).blocksMotion();
    }

    /** The saved landing spot of this player (null: none yet). */
    @Nullable
    static BlockPos thuis(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        return saved.contains(THUIS) ? BlockPos.of(saved.getLongOr(THUIS, 0L)) : null;
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
        if (level == null || plek == null) {
            lijn.verder(p, 5);                                // (no Guhmensie in this world: the feast comes to wherever they are)
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

    /** (once a second, step 6) at home, near the landing spot, on the ground: the feast. */
    static void seconde(ServerPlayer p) {
        Verhaallijn lijn = RingH6Feature.LIJN;
        ServerLevel level = level(p);
        if (!Ring.aanZet(p, lijn, 6)) {
            return;
        }
        BlockPos thuis = thuis(p);
        boolean hier = level == null || (p.level() == level && (thuis == null || thuis.closerThan(p.blockPosition(), FEEST_BEREIK)));
        if (!hier) {
            if (p.tickCount % 1200 < 20) {
                GuhQuests.hint(p, "quest.guhs.ringh6.thuis.ver");
            }
            return;
        }
        Long geland = GELAND.get(p.getUUID());
        if (!Duwtje.mag(p) || !p.onGround() || (geland != null && p.level().getGameTime() - geland < FEEST_NA)) {
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

    static void vergeet(UUID speler) {
        GELAND.remove(speler);
    }

    private Thuis() {
    }
}
