package nl.juiced.guhs.feature.doolhof;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * The plan of one maze (pure logic, no world): a square of cells in the middle of the big hedge field (makkelijk 11,
 * medium 15, lastig 19 cells a side), the lookout tower's 3x3 cells in the middle left out. A random depth-first maze,
 * then a few extra openings (loops, so a Mika in a corridor never traps you for good), the start at the south edge,
 * the exit at the north edge; on makkelijk and medium a straight corridor leads from the exit to the field's gate.
 * The kaasknabbels go in cells far from the start and from each other; on lastig the dead ends get fake knabbels
 * (Mika-lokaas). Cells are (x, z) with 0 &lt;= x, z &lt; {@link #N}; x grows east, z grows south.
 */
public final class DoolhofKaart {
    /** Cells per side of the whole field (the lastig maze). */
    public static final int N = 19;
    /** The tower's cells (inclusive, both axes). */
    public static final int TOREN_VAN = 8, TOREN_TOT = 10;
    /** The column of the start, the exit and the gates. */
    public static final int MIDDEN = N / 2;

    public final Niveau niveau;
    public final int n, off;
    /** Open passages: {@code oost[x][z]} between (x, z) and (x + 1, z); {@code zuid[x][z]} between (x, z) and (x, z + 1). */
    public final boolean[][] oost = new boolean[N][N], zuid = new boolean[N][N];
    /** Cells of the maze itself / of the exit corridor (makkelijk and medium). */
    public final boolean[][] actief = new boolean[N][N], gang = new boolean[N][N];
    public final int startX, startZ, uitX, uitZ;
    public final List<int[]> knabbels = new ArrayList<>(), nep = new ArrayList<>(), doodlopend = new ArrayList<>();
    /** Steps from the start (through the maze), -1 for cells outside it. */
    public final int[][] afstand = new int[N][N];

    /** Size of the maze per level (cells a side). */
    public static int grootte(Niveau niveau) {
        return switch (niveau) {
            case MAKKELIJK -> 11;
            case MEDIUM -> 15;
            case LASTIG -> N;
        };
    }

    /** Kaasknabbels to find per level: 8 / 12 / 16. */
    public static int aantalKnabbels(Niveau niveau) {
        return switch (niveau) {
            case MAKKELIJK -> 8;
            case MEDIUM -> 12;
            case LASTIG -> 16;
        };
    }

    /** Mika's in the maze per level: 1 / 2 / 3. */
    public static int aantalMikas(Niveau niveau) {
        return niveau.ordinal() + 1;
    }

    /** Fake knabbels (only on lastig, in dead ends). */
    public static int aantalNep(Niveau niveau) {
        return niveau == Niveau.LASTIG ? 5 : 0;
    }

    public static boolean toren(int x, int z) {
        return x >= TOREN_VAN && x <= TOREN_TOT && z >= TOREN_VAN && z <= TOREN_TOT;
    }

    public DoolhofKaart(Niveau niveau, long seed) {
        this.niveau = niveau;
        this.n = grootte(niveau);
        this.off = (N - n) / 2;
        Random rng = new Random(seed);
        for (int x = off; x < off + n; x++) {
            for (int z = off; z < off + n; z++) {
                actief[x][z] = !toren(x, z);
            }
        }
        startX = MIDDEN;
        startZ = off + n - 1;
        uitX = MIDDEN;
        uitZ = off;
        for (int z = 0; z < off; z++) {
            gang[MIDDEN][z] = true;
        }
        graaf(rng);
        lussen(rng, switch (niveau) {
            case MAKKELIJK -> 0.12;
            case MEDIUM -> 0.07;
            case LASTIG -> 0.035;
        });
        meet();
        for (int x = 0; x < N; x++) {
            for (int z = 0; z < N; z++) {
                if (actief[x][z] && buren(x, z) == 1 && !(x == startX && z == startZ) && !(x == uitX && z == uitZ)) {
                    doodlopend.add(new int[] {x, z});
                }
            }
        }
        verstop(rng);
    }

    // --- building the maze ----------------------------------------------------------------------------------------------

    private static final int[][] STAP = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private void graaf(Random rng) {
        boolean[][] gezien = new boolean[N][N];
        ArrayDeque<int[]> stapel = new ArrayDeque<>();
        stapel.push(new int[] {startX, startZ});
        gezien[startX][startZ] = true;
        List<int[]> opties = new ArrayList<>(4);
        while (!stapel.isEmpty()) {
            int[] c = stapel.peek();
            opties.clear();
            for (int[] s : STAP) {
                int nx = c[0] + s[0], nz = c[1] + s[1];
                if (binnen(nx, nz) && actief[nx][nz] && !gezien[nx][nz]) {
                    opties.add(new int[] {nx, nz});
                }
            }
            if (opties.isEmpty()) {
                stapel.pop();
                continue;
            }
            int[] next = opties.get(rng.nextInt(opties.size()));
            open(c[0], c[1], next[0], next[1], true);
            gezien[next[0]][next[1]] = true;
            stapel.push(next);
        }
    }

    /** Opens some extra walls between two maze cells (loops). */
    private void lussen(Random rng, double kans) {
        List<int[]> dicht = new ArrayList<>();
        for (int x = 0; x < N; x++) {
            for (int z = 0; z < N; z++) {
                if (!actief[x][z]) {
                    continue;
                }
                if (x + 1 < N && actief[x + 1][z] && !oost[x][z]) {
                    dicht.add(new int[] {x, z, x + 1, z});
                }
                if (z + 1 < N && actief[x][z + 1] && !zuid[x][z]) {
                    dicht.add(new int[] {x, z, x, z + 1});
                }
            }
        }
        Collections.shuffle(dicht, rng);
        int aantal = (int) Math.round(dicht.size() * kans);
        for (int i = 0; i < aantal; i++) {
            int[] w = dicht.get(i);
            open(w[0], w[1], w[2], w[3], true);
        }
    }

    void open(int x1, int z1, int x2, int z2, boolean open) {
        if (x2 == x1 + 1) {
            oost[x1][z1] = open;
        } else if (x1 == x2 + 1) {
            oost[x2][z2] = open;
        } else if (z2 == z1 + 1) {
            zuid[x1][z1] = open;
        } else if (z1 == z2 + 1) {
            zuid[x2][z2] = open;
        }
    }

    public static boolean binnen(int x, int z) {
        return x >= 0 && z >= 0 && x < N && z < N;
    }

    /** Is there a passage from (x, z) one step in this direction (dx, dz) (only between maze cells)? */
    public boolean doorgang(int x, int z, int dx, int dz) {
        int nx = x + dx, nz = z + dz;
        if (!binnen(nx, nz) || !actief[x][z] || !actief[nx][nz]) {
            return false;
        }
        if (dx == 1) {
            return oost[x][z];
        } else if (dx == -1) {
            return oost[nx][nz];
        } else if (dz == 1) {
            return zuid[x][z];
        }
        return zuid[nx][nz];
    }

    /** How many ways lead out of this cell (the exit counts as one). */
    public int buren(int x, int z) {
        int b = 0;
        for (int[] s : STAP) {
            if (doorgang(x, z, s[0], s[1])) {
                b++;
            }
        }
        if (x == uitX && z == uitZ) {
            b++;
        }
        return b;
    }

    private void meet() {
        for (int[] rij : afstand) {
            java.util.Arrays.fill(rij, -1);
        }
        ArrayDeque<int[]> q = new ArrayDeque<>();
        afstand[startX][startZ] = 0;
        q.add(new int[] {startX, startZ});
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] s : STAP) {
                if (doorgang(c[0], c[1], s[0], s[1]) && afstand[c[0] + s[0]][c[1] + s[1]] < 0) {
                    afstand[c[0] + s[0]][c[1] + s[1]] = afstand[c[0]][c[1]] + 1;
                    q.add(new int[] {c[0] + s[0], c[1] + s[1]});
                }
            }
        }
    }

    /** Kaasknabbels far from the start and apart from each other; fake ones in the other dead ends (lastig). */
    private void verstop(Random rng) {
        List<int[]> cellen = new ArrayList<>();
        for (int x = 0; x < N; x++) {
            for (int z = 0; z < N; z++) {
                if (actief[x][z] && afstand[x][z] >= 3) {
                    cellen.add(new int[] {x, z});
                }
            }
        }
        Collections.shuffle(cellen, rng);
        // dead ends first (the fun spots), then the rest, farther ones a bit more likely
        int[][] punten = new int[N][N];
        for (int[] c : cellen) {
            punten[c[0]][c[1]] = score(c, rng);
        }
        cellen.sort((a, b) -> Integer.compare(punten[b[0]][b[1]], punten[a[0]][a[1]]));
        int wil = aantalKnabbels(niveau);
        for (int afstandMin = 4; afstandMin >= 0 && knabbels.size() < wil; afstandMin--) {
            for (int[] c : cellen) {
                if (knabbels.size() >= wil) {
                    break;
                }
                if (bezet(c) || !ver(c, knabbels, afstandMin)) {
                    continue;
                }
                knabbels.add(c);
            }
        }
        int nepWil = aantalNep(niveau);
        List<int[]> doden = new ArrayList<>(doodlopend);
        Collections.shuffle(doden, rng);
        for (int[] c : doden) {
            if (nep.size() >= nepWil) {
                break;
            }
            if (!bezet(c) && afstand[c[0]][c[1]] >= 3) {
                nep.add(c);
            }
        }
    }

    private int score(int[] c, Random rng) {
        int s = afstand[c[0]][c[1]] + rng.nextInt(12);
        if (buren(c[0], c[1]) == 1) {
            s += 10;
        }
        return s;
    }

    private boolean bezet(int[] c) {
        for (int[] k : knabbels) {
            if (k[0] == c[0] && k[1] == c[1]) {
                return true;
            }
        }
        for (int[] k : nep) {
            if (k[0] == c[0] && k[1] == c[1]) {
                return true;
            }
        }
        return false;
    }

    private static boolean ver(int[] c, List<int[]> andere, int min) {
        for (int[] o : andere) {
            if (Math.abs(o[0] - c[0]) + Math.abs(o[1] - c[1]) < min) {
                return false;
            }
        }
        return true;
    }

    /** Every maze cell can be reached from the start (no closed-off corners): the check of the tests. */
    public boolean alles() {
        for (int x = 0; x < N; x++) {
            for (int z = 0; z < N; z++) {
                if (actief[x][z] && afstand[x][z] < 0) {
                    return false;
                }
            }
        }
        return true;
    }

    /** All maze cells (for placing things). */
    public List<int[]> cellen() {
        List<int[]> out = new ArrayList<>();
        for (int x = 0; x < N; x++) {
            for (int z = 0; z < N; z++) {
                if (actief[x][z]) {
                    out.add(new int[] {x, z});
                }
            }
        }
        return out;
    }
}
