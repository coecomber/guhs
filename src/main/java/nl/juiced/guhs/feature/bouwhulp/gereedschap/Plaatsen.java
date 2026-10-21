package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;
import nl.juiced.guhs.feature.bouwhulp.kern.Spook;
import nl.juiced.guhs.feature.bouwhulp.kern.Spookbouw;
import nl.juiced.guhs.feature.bouwhulp.kern.Spoken;

/** Everything F9 makes goes through a ghost (design 2.1): helpers to put one down, by a clicked face or at a world spot. */
public final class Plaatsen {
    /**
     * Where an ontwerp goes when you click a block's face: its footprint centred on the block in front of the face, its
     * bottom there (hanging down from a ceiling).
     */
    public static BlockPos oorsprong(Ontwerp o, BlockPos geklikt, Direction kant) {
        return oorsprong(o.grootte(), geklikt, kant);
    }

    public static BlockPos oorsprong(Vec3i g, BlockPos geklikt, Direction kant) {
        BlockPos voor = geklikt.relative(kant);
        int y = kant == Direction.DOWN ? voor.getY() - g.getY() + 1 : voor.getY();
        return new BlockPos(voor.getX() - (g.getX() - 1) / 2, y, voor.getZ() - (g.getZ() - 1) / 2);
    }

    /** The rotation that makes an ontwerp's front (south) face the player. */
    public static Rotation naarSpeler(ServerPlayer p) {
        return switch (p.getDirection()) {
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.CLOCKWISE_90;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** A ghost of this ontwerp with its control panel (null: it could not: the player was told why). */
    @Nullable
    public static Spook spook(ServerPlayer p, Ontwerp o, BlockPos oorsprong, Rotation draai, String naam) {
        Spook s = Spookbouw.start(p, o, oorsprong, draai, Mirror.NONE);
        if (s != null) {
            Spookbouw.zetNaam(p.level(), s, naam);
            Spookbouw.toonBediening(p, s);
        }
        return s;
    }

    /**
     * Blocks at world spots (a kwast's strokes): a ghost that starts building at once. When something is missing it stays a
     * ghost and its panel opens (so the player sees the shortage and may "alsnog bouwen").
     */
    @Nullable
    public static Spook meteen(ServerPlayer p, Map<BlockPos, BlockState> wereld, String naam) {
        if (wereld.isEmpty()) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.kwast.niets"));
            return null;
        }
        int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE, z = Integer.MAX_VALUE;
        for (BlockPos b : wereld.keySet()) {
            x = Math.min(x, b.getX());
            y = Math.min(y, b.getY());
            z = Math.min(z, b.getZ());
        }
        Spook s = Spookbouw.start(p, Ontwerp.van(wereld), new BlockPos(x, y, z), Rotation.NONE, Mirror.NONE);
        if (s == null) {
            return null;
        }
        Spookbouw.zetNaam(p.level(), s, naam);
        Spoken.Bouwen b = Spookbouw.bouw(p, s, false);
        if (!b.begonnen()) {
            Spookbouw.toonBediening(p, s);
        }
        return s;
    }

    /** A ghost at a world spot (the egaliseerder, the tunnelboor, the verfmachine): placed, with its panel. */
    @Nullable
    public static Spook opWereld(ServerPlayer p, Map<BlockPos, BlockState> wereld, String naam) {
        if (wereld.isEmpty()) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.kwast.niets"));
            return null;
        }
        int x = Integer.MAX_VALUE, y = Integer.MAX_VALUE, z = Integer.MAX_VALUE;
        for (BlockPos b : wereld.keySet()) {
            x = Math.min(x, b.getX());
            y = Math.min(y, b.getY());
            z = Math.min(z, b.getZ());
        }
        return spook(p, Ontwerp.van(wereld), new BlockPos(x, y, z), Rotation.NONE, naam);
    }

    private Plaatsen() {
    }
}
