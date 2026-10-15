package nl.juiced.guhs.feature.einde2.geestenwereld;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2, slice geestenwereld: De Geestenwereld (DESIGN_EINDE2 5 "De Geestenwereld"), the dimension {@code guhs:geestenwereld}.
 * One hand-made build of floating islands, always at the same spot ({@link #OORSPRONG} = template 0,0,0; see
 * {@link GeestBouw}), with its markers in the sidecar {@code data/guhs/einde2/plekken/geestenwereld.json} ({@link Plekken#van}
 * with the name {@link #NAAM}): a spot in the template is a spot in the world by a plain shift ({@link #wereld}).
 * <p>
 * Here: where things are, going there and back, and what keeps you safe: falling into the void puts you back at the
 * arrival ({@link #vangOp}), nothing hurts ({@link #doetGeenPijn}); protection of the blocks is {@link GeestBescherming}.
 * Getting there is a dev command for now ({@code /guhs einde2 geest ga}); the way in of the story calls {@link #stuur}
 * (hook fase B: mediteren). The way back: a meditatiesteen in every zone, or {@code /guhs einde2 geest terug} ({@link #terug}).
 * <p>
 * The game test server has no datapack dimensions, so a test marks a box of its own level as Geestenwereld
 * ({@link #testGebied}) with its own arrival and void line.
 */
public final class Geestenwereld {
    public static final String NAAM = "geestenwereld";
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id(NAAM));
    /** The world position of template 0,0,0 of the build (the tiles line up with the chunks). */
    public static final BlockPos OORSPRONG = new BlockPos(-304, 32, -304);
    /** The size of the build (19 x 19 tiles of 32). */
    public static final int BREEDTE = 608, HOOGTE = 160;
    /** Below this y the mist catches you and puts you back at the arrival. */
    public static final int LEEGTE_Y = 16;
    /** The zones, in the order of the design (each has the markers zone_, plek_ and meditatiesteen_ + its name). */
    public static final List<String> ZONES = List.of("meditatiecirkel", "gefluisterwoud", "koh_grot", "lotus_ruines", "top_voorgangers",
            "hemelvijver", "theepaviljoen", "fang_pad");
    /** Where you arrive (a prop marker). */
    public static final String AANKOMST = "aankomst_speler";
    /** The player's saved data: where your body stayed (dimension, position, look). */
    public static final String TERUG = "guhs_einde2_geestenwereld_terug";

    /** A box of a (test) level that counts as Geestenwereld, with its own arrival and void line. */
    public record TestGebied(ResourceKey<Level> dim, AABB doos, Vec3 aankomst, int leegteY) {
    }

    private static final List<TestGebied> TEST = new CopyOnWriteArrayList<>();

    private Geestenwereld() {
    }

    // --- where ---------------------------------------------------------------------------------------------------------------

    @Nullable
    public static ServerLevel level(@Nullable MinecraftServer server) {
        return server == null ? null : server.getLevel(DIM);
    }

    /** Is this spot in the Geestenwereld (the dimension, or a test box: any height above it)? */
    public static boolean in(Level level, BlockPos pos) {
        return in(level, pos.getX() + 0.5, pos.getZ() + 0.5);
    }

    public static boolean in(Entity e) {
        return in(e.level(), e.getX(), e.getZ());
    }

    private static boolean in(Level level, double x, double z) {
        if (level.dimension() == DIM) {
            return true;
        }
        return test(level, x, z) != null;
    }

    @Nullable
    private static TestGebied test(Level level, double x, double z) {
        for (TestGebied t : TEST) {
            if (t.dim() == level.dimension() && x >= t.doos().minX && x < t.doos().maxX && z >= t.doos().minZ && z < t.doos().maxZ) {
                return t;
            }
        }
        return null;
    }

    /** (game tests) This box of this level counts as Geestenwereld; arrival and void line of its own. */
    public static void testGebied(TestGebied gebied) {
        TEST.add(gebied);
    }

    public static void testGebiedWeg(TestGebied gebied) {
        TEST.remove(gebied);
    }

    /** The world position of a marker of the build (null: no such marker). */
    @Nullable
    public static BlockPos wereld(String plek) {
        Plekken.Plek p = Plekken.plek(NAAM, plek);
        return p == null ? null : OORSPRONG.offset(p.pos());
    }

    /** The marker at this world position (the first of the sidecar's order), or null. */
    @Nullable
    public static Plekken.Plek plekOp(BlockPos pos) {
        BlockPos lokaal = pos.subtract(OORSPRONG);
        for (Plekken.Plek p : Plekken.van(NAAM).values()) {
            if (p.pos().equals(lokaal) && !p.soort().equals(Plekken.ZONE) && !p.soort().equals(Plekken.PAD)) {
                return p;
            }
        }
        return null;
    }

    /** Where you arrive: in the dimension the arrival marker, in a test box its own. */
    public static Vec3 aankomst(Level level, double x, double z) {
        TestGebied t = level.dimension() == DIM ? null : test(level, x, z);
        if (t != null) {
            return t.aankomst();
        }
        BlockPos a = wereld(AANKOMST);
        return a == null ? Vec3.atBottomCenterOf(OORSPRONG.offset(BREEDTE / 2, 18, BREEDTE / 2)) : Vec3.atBottomCenterOf(a);
    }

    private static int leegteY(Level level, double x, double z) {
        TestGebied t = level.dimension() == DIM ? null : test(level, x, z);
        return t != null ? t.leegteY() : LEEGTE_Y;
    }

    // --- going there and back ---------------------------------------------------------------------------------------------------

    /**
     * Takes a player to a marker of the Geestenwereld ({@link #AANKOMST}, a zone's {@code plek_<zone>}, or any other marker);
     * from outside, the spot the body stays at is remembered first ({@link #TERUG}). False when the dimension or the marker
     * is not there. hook fase B: mediteren (the story calls this after its cutscene; guests within 5 blocks come along).
     */
    public static boolean stuur(ServerPlayer p, String plek) {
        ServerLevel geest = level(p.level().getServer());
        BlockPos at = wereld(plek);
        if (geest == null || at == null) {
            return false;
        }
        if (p.level().dimension() != DIM) {
            onthoud(p);
        }
        float yaw = plek.equals(AANKOMST) ? 180f : p.getYRot();
        p.teleportTo(geest, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, Set.of(), yaw, 0f, true);
        p.resetFallDistance();
        p.sendSystemMessage(Component.translatable("gui.guhs.einde2_geestenwereld.aangekomen").withStyle(ChatFormatting.LIGHT_PURPLE));
        geest.playSound(null, at, E2GeestenwereldFeature.MEDITATIE.get(), SoundSource.PLAYERS, 0.8f, 1.2f);
        return true;
    }

    /** Remembers where the body of this player stays (its dimension, spot and look). */
    public static void onthoud(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        t.putString("Dim", p.level().dimension().identifier().toString());
        t.putDouble("X", p.getX());
        t.putDouble("Y", p.getY());
        t.putDouble("Z", p.getZ());
        t.putFloat("Yaw", p.getYRot());
        t.putFloat("Pitch", p.getXRot());
        GuhQuests.saved(p).put(TERUG, t);
    }

    /**
     * Back to the body: to the remembered spot (and forgotten), or, when there is none, where the player would respawn.
     * True when the player went somewhere.
     */
    public static boolean terug(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        CompoundTag t = saved.getCompoundOrEmpty(TERUG);
        saved.remove(TERUG);
        MinecraftServer server = p.level().getServer();
        ServerLevel naar = null;
        if (t.contains("Dim")) {
            Identifier id = Identifier.tryParse(t.getStringOr("Dim", ""));
            naar = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        }
        if (naar != null) {
            p.teleportTo(naar, t.getDoubleOr("X", 0), t.getDoubleOr("Y", 64), t.getDoubleOr("Z", 0), Set.of(), t.getFloatOr("Yaw", 0f),
                    t.getFloatOr("Pitch", 0f), true);
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_geestenwereld.terug").withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            TeleportTransition bed = p.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING);
            p.teleport(bed);
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_geestenwereld.geen_terug").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        p.resetFallDistance();
        p.level().playSound(null, p.blockPosition(), E2GeestenwereldFeature.MEDITATIE.get(), SoundSource.PLAYERS, 0.7f, 0.9f);
        return true;
    }

    // --- safety ------------------------------------------------------------------------------------------------------------------

    /** Below the void line: put back at the arrival, softly. True when it happened. */
    public static boolean vangOp(ServerPlayer p) {
        if (!in(p) || p.isSpectator() || p.getY() >= leegteY(p.level(), p.getX(), p.getZ())) {
            return false;
        }
        Vec3 a = aankomst(p.level(), p.getX(), p.getZ());
        p.teleportTo((ServerLevel) p.level(), a.x, a.y, a.z, Set.of(), 180f, 0f, true);
        p.setDeltaMovement(Vec3.ZERO);
        p.resetFallDistance();
        p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_geestenwereld.leegte").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** Nothing hurts in the Geestenwereld (except what is meant to get past everything: /kill). */
    public static boolean doetGeenPijn(Entity e, net.minecraft.world.damagesource.DamageSource bron) {
        return in(e) && !bron.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY);
    }
}
