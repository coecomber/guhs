package nl.juiced.guhs.feature.bio.wereld;

import java.util.Locale;
import java.util.Optional;

import com.mojang.serialization.Codec;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

/**
 * biomes3 wereld: the spots of the terrain a building can ask for ({@code guhs:bio_plek}, {@link BioPlekStructure}).
 * Everything is read from the terrain model, so a spot is known before a single block of the chunk exists.
 * <p>
 * {@link #zoek} looks for a spot of one kind INSIDE one chunk and returns the one nearest to the chunk's middle, or
 * nothing. A spot is a column, the y of its ground's top block (for {@code over_rivier}: of the banks; for
 * {@code lucht}: the asked height above the meadow), and the direction the building looks ("kijk"):
 * <table>
 *   <caption>The kinds</caption>
 *   <tr><td>terras</td><td>flat terrace ground of the Klaterdal, no river, rock face or rim within 6 blocks; looks to the river if one is within 20 blocks, else down the valley</td></tr>
 *   <tr><td>oever</td><td>the bank right beside the river (dry, on the terrace, two blocks of terrace behind it); looks at the water</td></tr>
 *   <tr><td>over_rivier</td><td>the middle of the river where it is at most 9 wide with level banks and runs on for 3 blocks both ways; y = the banks; looks ALONG the river, so the building's left-right axis lies across it</td></tr>
 *   <tr><td>waterval</td><td>the bank at the foot of a fall of 4 blocks or more (on the lower terrace, at most 6 from the falling water); looks at the fall</td></tr>
 *   <tr><td>rots</td><td>the bank at the TOP of a tall fall (10 blocks or more within 6 blocks of the edge); looks out over the fall</td></tr>
 *   <tr><td>meer_oever</td><td>the shore of the lake (dry, at most 2 above the water, open water for 6 blocks in front); looks at the water</td></tr>
 *   <tr><td>meer_eiland</td><td>the flat ground of a large lake island with 4 blocks of it all around; looks to the nearest water</td></tr>
 *   <tr><td>meer_boom</td><td>3 blocks from the big tree of a large lake island, on the island's inner side; looks at the tree</td></tr>
 *   <tr><td>weide</td><td>the middle of the chunk, on the meadow of the Wolkenweide</td></tr>
 *   <tr><td>lucht</td><td>the middle of the chunk, {@code hoogte} blocks above the meadow; the air is kept free by {@link Luchtruim}</td></tr>
 *   <tr><td>zweefeiland</td><td>the middle of a natural floating island (round or elongated, at least 5 wide each way), on its top</td></tr>
 * </table>
 */
public final class BioPlekken {
    public enum Soort implements StringRepresentable {
        TERRAS, OEVER, OVER_RIVIER, WATERVAL, ROTS, MEER_OEVER, MEER_EILAND, MEER_BOOM, WEIDE, LUCHT, ZWEEFEILAND;

        public static final Codec<Soort> CODEC = StringRepresentable.fromEnum(Soort::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** A spot: the column, the y of the ground's top block (or of the asked height), the way the building looks. */
    public record Plek(int x, int y, int z, Direction kijk) {
    }

    private static final Direction[] RICHTINGEN = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** The spot of this kind in chunk (cx, cz) nearest to its middle. hoogte: only for {@link Soort#LUCHT}. */
    public static Optional<Plek> zoek(BioModel m, Soort soort, int cx, int cz, int hoogte) {
        Kaart k = m.kaart(cx, cz);
        if (k.leeg) {
            return Optional.empty();
        }
        int x0 = cx << 4, z0 = cz << 4, mx = x0 + 8, mz = z0 + 8;
        Direction willekeurig = RICHTINGEN[(int) (BioModel.kans(m.hash(cx, cz, 7401), 0) * 4)];
        switch (soort) {
            case WEIDE, LUCHT -> {
                int o = Kaart.index(mx, mz);
                if (k.soort[o] != Kaart.WEIDE || k.meng[o] < 1f || soort == Soort.WEIDE && k.water[o] != Kaart.GEEN) { // biomes3 wereld-wolk: not in the meadow's pond
                    return Optional.empty();
                }
                return Optional.of(new Plek(mx, k.hoogte[o] + (soort == Soort.LUCHT ? hoogte : 0), mz, willekeurig));
            }
            case ZWEEFEILAND -> {
                for (WolkTerrein.Eiland ei : WolkTerrein.cel(m, Math.floorDiv(mx, WolkTerrein.CEL), Math.floorDiv(mz, WolkTerrein.CEL))) {
                    if ((ei.x >> 4) == cx && (ei.z >> 4) == cz && (ei.vorm == WolkTerrein.ROND || ei.vorm == WolkTerrein.LANG)
                            && Math.min(ei.rx, ei.rz) >= 5 && m.meng(ei.x, ei.z) >= 1f) {
                        return Optional.of(new Plek(ei.x, ei.top, ei.z, willekeurig));
                    }
                }
                return Optional.empty();
            }
            case MEER_BOOM -> {
                for (MeerTerrein.Eiland ei : MeerTerrein.bij(m, x0, z0, x0 + 16, z0 + 16)) {
                    if (!ei.groot()) {
                        continue;
                    }
                    // from the tree towards the island's middle, along the axis that is longest
                    int dx = ei.x() - ei.boomX(), dz = ei.z() - ei.boomZ();
                    Direction naar = Math.abs(dx) >= Math.abs(dz) ? (dx >= 0 ? Direction.EAST : Direction.WEST) : (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
                    int x = ei.boomX() + naar.getStepX() * 3, z = ei.boomZ() + naar.getStepZ() * 3;
                    if ((x >> 4) == cx && (z >> 4) == cz && m.droog(x, z) && (m.vlag(x, z) & Kaart.GROOT) != 0) {
                        return Optional.of(new Plek(x, m.hoogte(x, z), z, naar.getOpposite()));
                    }
                }
                return Optional.empty();
            }
            default -> {
            }
        }
        Plek beste = null;
        int besteAfstand = Integer.MAX_VALUE;
        for (int o = 0; o < 256; o++) {
            if (k.meng[o] < 1f || k.soort[o] == Kaart.WEIDE) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4);
            int afstand = (x - mx) * (x - mx) + (z - mz) * (z - mz);
            if (afstand >= besteAfstand) {
                continue;
            }
            Plek p = switch (soort) {
                case TERRAS -> terras(m, k, o, x, z, willekeurig);
                case OEVER -> oever(m, k, o, x, z);
                case OVER_RIVIER -> overRivier(m, k, o, x, z, willekeurig);
                case WATERVAL -> waterval(m, k, o, x, z, 4, false);
                case ROTS -> waterval(m, k, o, x, z, 10, true);
                case MEER_OEVER -> meerOever(m, k, o, x, z);
                case MEER_EILAND -> meerEiland(m, k, o, x, z);
                default -> null;
            };
            if (p != null) {
                beste = p;
                besteAfstand = afstand;
            }
        }
        return Optional.ofNullable(beste);
    }

    private static boolean rivierwater(BioModel m, int x, int z) {
        return m.water(x, z) != Kaart.GEEN && (m.vlag(x, z) & Kaart.RIVIER) != 0;
    }

    private static Plek terras(BioModel m, Kaart k, int o, int x, int z, Direction anders) {
        if (k.terras[o] < 0 || k.water[o] != Kaart.GEEN || k.vlag[o] != 0 || ((x | z) & 1) != 0) {
            return null;
        }
        int h = k.hoogte[o];
        for (int dx = -6; dx <= 6; dx += 2) {
            for (int dz = -6; dz <= 6; dz += 2) {
                if (m.hoogte(x + dx, z + dz) != h || !m.droog(x + dx, z + dz)) {
                    return null;
                }
            }
        }
        // to the river if it is near, else down the valley
        Direction kijk = null;
        int best = 21;
        for (Direction d : RICHTINGEN) {
            for (int a = 7; a < best; a++) {
                if (m.water(x + d.getStepX() * a, z + d.getStepZ() * a) != Kaart.GEEN) {
                    best = a;
                    kijk = d;
                    break;
                }
            }
        }
        if (kijk == null) {
            for (Direction d : RICHTINGEN) {
                int ver = m.hoogte(x + d.getStepX() * 24, z + d.getStepZ() * 24);
                if (m.meng(x + d.getStepX() * 24, z + d.getStepZ() * 24) >= 1f && ver < h) {
                    kijk = d;
                    break;
                }
            }
        }
        return new Plek(x, h, z, kijk == null ? anders : kijk);
    }

    private static Plek oever(BioModel m, Kaart k, int o, int x, int z) {
        if (k.terras[o] < 0 || k.water[o] != Kaart.GEEN || k.vlag[o] != 0) {
            return null;
        }
        int h = k.hoogte[o];
        for (Direction d : RICHTINGEN) {
            int sx = d.getStepX(), sz = d.getStepZ();
            if (!rivierwater(m, x + sx, z + sz) || m.water(x + sx, z + sz) != h - 1 || !rivierwater(m, x + 2 * sx, z + 2 * sz)) {
                continue;
            }
            boolean vlak = true;
            for (int a = 1; a <= 2 && vlak; a++) {
                for (int b = -1; b <= 1 && vlak; b++) {
                    int px = x - sx * a + sz * b, pz = z - sz * a + sx * b;
                    vlak = m.droog(px, pz) && m.hoogte(px, pz) == h;
                }
            }
            if (vlak && m.droog(x + sz, z + sx) && m.droog(x - sz, z - sx)) {
                return new Plek(x, h, z, d);
            }
        }
        return null;
    }

    private static Plek overRivier(BioModel m, Kaart k, int o, int x, int z, Direction anders) {
        if (k.terras[o] < 0 || k.water[o] == Kaart.GEEN || (k.vlag[o] & (Kaart.RIVIER | Kaart.VAL)) != Kaart.RIVIER) {
            return null;
        }
        int w = k.water[o];
        for (int as = 0; as < 2; as++) {
            int sx = as == 0 ? 1 : 0, sz = 1 - sx;
            // across: the same water to both sides, then a dry bank one above the water
            int links = 0, rechts = 0;
            while (links < 6 && m.water(x - sx * (links + 1), z - sz * (links + 1)) == w) {
                links++;
            }
            while (rechts < 6 && m.water(x + sx * (rechts + 1), z + sz * (rechts + 1)) == w) {
                rechts++;
            }
            if (links + rechts + 1 > 9 || Math.abs(links - rechts) > 1) {
                continue;
            }
            int lx = x - sx * (links + 1), lz = z - sz * (links + 1), rx = x + sx * (rechts + 1), rz = z + sz * (rechts + 1);
            if (!m.droog(lx, lz) || !m.droog(rx, rz) || m.hoogte(lx, lz) != w + 1 || m.hoogte(rx, rz) != w + 1) {
                continue;
            }
            // along: the river runs on for 3 blocks both ways
            boolean door = true;
            for (int a = 1; a <= 3 && door; a++) {
                door = m.water(x + sz * a, z + sx * a) == w && m.water(x - sz * a, z - sx * a) == w;
            }
            if (door) {
                boolean om = anders == Direction.SOUTH || anders == Direction.WEST;
                return new Plek(x, w + 1, z, as == 0 ? (om ? Direction.SOUTH : Direction.NORTH) : (om ? Direction.WEST : Direction.EAST));
            }
        }
        return null;
    }

    /** The drop of the water at a {@link Kaart#VAL} column in a direction: down to the lowest water within 6 columns. */
    static int val(BioModel m, int x, int z, Direction d) {
        int w = m.water(x, z), laag = w;
        for (int a = 1; a <= 6; a++) {
            int w2 = m.water(x + d.getStepX() * a, z + d.getStepZ() * a);
            if (w2 == Kaart.GEEN || w2 > laag) {
                break;
            }
            laag = w2;
        }
        return w - laag;
    }

    /** The bank beside a fall of at least min blocks: at its foot (looking at it), or with boven at its top (looking out over it). */
    private static Plek waterval(BioModel m, Kaart k, int o, int x, int z, int min, boolean boven) {
        if (k.terras[o] < 0 || k.water[o] != Kaart.GEEN || k.vlag[o] != 0) {
            return null;
        }
        int h = k.hoogte[o];
        // walk sideways from this bank into the river; the fall must be in the row right behind (foot) or the row itself (top)
        for (Direction zij : RICHTINGEN) {
            int sx = zij.getStepX(), sz = zij.getStepZ();
            if (!rivierwater(m, x + sx, z + sz) || m.water(x + sx, z + sz) != h - 1) {
                continue;
            }
            for (Direction d : new Direction[]{zij.getClockWise(), zij.getCounterClockWise()}) {
                for (int a = 1; a <= 6; a++) {
                    int px = x + sx * a, pz = z + sz * a;
                    if (m.water(px, pz) != h - 1) {
                        break;
                    }
                    if (boven) {
                        // d = the way the water falls
                        if ((m.vlag(px, pz) & Kaart.VAL) != 0 && val(m, px, pz, d) >= min) {
                            return new Plek(x, h, z, d);
                        }
                    } else {
                        // d = towards the fall: the column behind this one is the higher bed
                        int qx = px + d.getStepX(), qz = pz + d.getStepZ();
                        if ((m.vlag(qx, qz) & Kaart.VAL) != 0 && m.water(qx, qz) - (h - 1) >= min) {
                            return new Plek(x, h, z, d);
                        }
                    }
                }
            }
        }
        return null;
    }

    private static Plek meerOever(BioModel m, Kaart k, int o, int x, int z) {
        if (k.water[o] != Kaart.GEEN || (k.vlag[o] & (Kaart.LIP | Kaart.EILAND)) != 0 || k.hoogte[o] > MeerTerrein.WATER + 2
                || k.hoogte[o] <= MeerTerrein.WATER) {
            return null;
        }
        for (Direction d : RICHTINGEN) {
            int sx = d.getStepX(), sz = d.getStepZ();
            boolean open = true;
            for (int a = 2; a <= 8 && open; a += 2) {
                int px = x + sx * a, pz = z + sz * a;
                open = m.water(px, pz) == MeerTerrein.WATER && (m.vlag(px, pz) & Kaart.RIVIER) == 0;
            }
            if (open && m.droog(x - sx, z - sz) && m.droog(x - 2 * sx, z - 2 * sz) && m.droog(x + sz, z + sx) && m.droog(x - sz, z - sx)) {
                return new Plek(x, k.hoogte[o], z, d);
            }
        }
        return null;
    }

    private static Plek meerEiland(BioModel m, Kaart k, int o, int x, int z) {
        if ((k.vlag[o] & Kaart.GROOT) == 0 || k.hoogte[o] != MeerTerrein.WATER + 2) {
            return null;
        }
        for (int dx = -4; dx <= 4; dx += 2) {
            for (int dz = -4; dz <= 4; dz += 2) {
                if ((m.vlag(x + dx, z + dz) & Kaart.GROOT) == 0 || m.hoogte(x + dx, z + dz) != MeerTerrein.WATER + 2) {
                    return null;
                }
            }
        }
        Direction kijk = Direction.NORTH;
        int best = 99;
        for (Direction d : RICHTINGEN) {
            for (int a = 5; a < best; a++) {
                if (m.water(x + d.getStepX() * a, z + d.getStepZ() * a) != Kaart.GEEN) {
                    best = a;
                    kijk = d;
                    break;
                }
            }
        }
        return new Plek(x, k.hoogte[o], z, kijk);
    }

    private BioPlekken() {
    }
}
