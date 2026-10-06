package nl.juiced.guhs.feature.ringh3;

import java.util.function.Predicate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.wereld.Herstel;

/**
 * bbq2 (ring-h3): the four doors of the mine. A door is a box of blocks in the template (the west gate, the portcullis of
 * the lever hall, Gimguh's hidden door, the east gate): opening takes the blocks away, and {@link Herstel} puts the door's
 * own little template (guhs:ringh3_deur_&lt;name&gt;) back {@link #OPEN_TICKS} ticks after the last player left it, also after
 * a restart or when the chunk was unloaded in between. So a door is a shared thing that is closed again for the next player.
 * <p>
 * Who a door opens for by itself (checked a few times a second for players next to it, {@link #tick}):
 * <ul>
 *   <li>whoever stands on its INSIDE (nobody is ever locked in, and a friend who walked along gets out);</li>
 *   <li>from the outside: a player whose own story is past the riddle of that door ({@link Deur#stap}).</li>
 * </ul>
 * The riddles themselves ({@link Raadsels}) open a door for the player who solved it.
 */
public final class Deuren {
    /** A door closes this long after the last player was next to it. */
    public static final int OPEN_TICKS = 160;
    /** Within this many blocks of the middle of a door a player counts as "at the door". */
    public static final double BIJ = 4.2;

    public enum Deur {
        /** The west gate: from outside for whoever solved "zeg njeg" (step 2 and on); inside = east of it. */
        WEST("west", Plekken.DEUR_WEST, 2, l -> l.getX() > Plekken.DEUR_WEST.x1()),
        /** The portcullis between the lever hall and the well room: after the levers (step 3); inside = west of it. */
        VALHEK("valhek", Plekken.DEUR_VALHEK, 3, l -> l.getX() < Plekken.DEUR_VALHEK.x0()),
        /** Gimguh's door in the well room's west wall: after the runes (step 5); inside = in the passage behind it. */
        GEHEIM("geheim", Plekken.DEUR_GEHEIM, 5, l -> l.getX() < Plekken.DEUR_GEHEIM.x0()),
        /** The east gate: from outside only for whoever saw the bridge (step 6 and on); inside = west of it. */
        OOST("oost", Plekken.DEUR_OOST, 6, l -> l.getX() < Plekken.DEUR_OOST.x0());

        public final String naam;
        public final Plekken.Doos doos;
        /** From this step of ring_h3 on the door opens for a player who comes from outside. */
        public final int stap;
        final Predicate<BlockPos> binnen;

        Deur(String naam, Plekken.Doos doos, int stap, Predicate<BlockPos> binnen) {
            this.naam = naam;
            this.doos = doos;
            this.stap = stap;
            this.binnen = binnen;
        }

        public Identifier template() {
            return Guhs.id("ringh3_deur_" + naam);
        }

        /** Is this template position on the inside of the door? */
        public boolean binnen(BlockPos lokaal) {
            return binnen.test(lokaal);
        }
    }

    private Deuren() {
    }

    /** Is the door open in this copy (its corner block is air)? */
    public static boolean isOpen(Mijn m, Deur d) {
        return m.level().getBlockState(m.wereld(d.doos.hoek())).isAir();
    }

    /** Opens the door (stone grinds, dust falls) and asks for it to be put back; an open door just stays open longer. */
    public static void open(Mijn m, Deur d) {
        ServerLevel level = m.level();
        boolean dicht = !isOpen(m, d);
        if (dicht) {
            for (int x = d.doos.x0(); x <= d.doos.x1(); x++) {
                for (int y = d.doos.y0(); y <= d.doos.y1(); y++) {
                    for (int z = d.doos.z0(); z <= d.doos.z1(); z++) {
                        BlockPos pos = m.wereld(new BlockPos(x, y, z));
                        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0.01);
                    }
                }
            }
            Vec3 midden = m.midden(d.doos.midden());
            level.playSound(null, midden.x, midden.y, midden.z, RingH3Feature.POORT.get(), SoundSource.BLOCKS, 1.0f, d == Deur.VALHEK ? 1.3f : 0.8f);
        }
        Herstel.na(level, m.wereld(d.doos.hoek()), d.template(), m.draai(), OPEN_TICKS);
    }

    /** May this player (at this template position) have the door opened for them? */
    public static boolean mag(ServerPlayer p, Deur d, BlockPos lokaal) {
        return d.binnen(lokaal) || RingH3Feature.LIJN.stap(p) >= d.stap;
    }

    /** (a few times a second, for a player in the mine) the doors next to this player. */
    static void tick(ServerPlayer p, Mijn m, BlockPos lokaal) {
        if (p.isSpectator()) {
            return;
        }
        for (Deur d : Deur.values()) {
            BlockPos midden = d.doos.midden();
            double dx = lokaal.getX() - midden.getX(), dy = lokaal.getY() - midden.getY(), dz = lokaal.getZ() - midden.getZ();
            if (dx * dx + dz * dz > BIJ * BIJ || dy < -2 || dy > 5) {
                continue;
            }
            if (isOpen(m, d)) {
                // (it never closes on somebody: whoever is next to it keeps it open)
                Herstel.na(m.level(), m.wereld(d.doos.hoek()), d.template(), m.draai(), OPEN_TICKS);
            } else if (mag(p, d, lokaal)) {
                open(m, d);
            }
        }
    }
}
