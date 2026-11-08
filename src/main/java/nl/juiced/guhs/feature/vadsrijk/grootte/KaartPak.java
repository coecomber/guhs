package nl.juiced.guhs.feature.vadsrijk.grootte;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

/**
 * U-GROOTTE: the compact map pack of Het Vadsrijk. Pure Java (no game classes), so the build runs it right after compiling.
 * <p>The generators keep writing vanilla structure tiles ({@code data/guhs/structure/vadsrijk/tegels/t_<tx>_<tz>.nbt}, the
 * working format all the Python checkers read). {@code gradlew kaartPak} (a dependency of processResources) packs them into
 * ONE file {@code data/guhs/vadsrijk/kaart.gpak}; the jar ships only that file, not the tiles (127 MB -> a few MB).
 * <p>Per tile the pack keeps EXACTLY what vanilla's StructureTemplate.load reads: DataVersion, size, the palette compounds
 * (verbatim bytes, in the tile's own order, so the state indices are unchanged), every block (as a dense y-z-x grid of
 * state+1, 0 = no block) with its block-entity compound (verbatim bytes) and the entity list (verbatim). The grid is cut to
 * the used layers and deflated at level 9; identical tiles (open sea) are stored once. A tile that does not fit the grid
 * (duplicate/out-of-range positions, multi-palette, ...) is stored as its original gzip bytes ("raw" mode), so nothing
 * can ever change. Runtime: {@link KaartPakLader} rebuilds the same CompoundTag and loads it exactly like vanilla.
 * <p>Commands (java -cp classes ...KaartPak): {@code pak <tegels dir> <out .gpak>} and {@code controleer <tegels dir> <.gpak>}
 * (decodes every tile from the pack and compares it with its .nbt: palette, every block + block entity, size, entities).
 */
public final class KaartPak {
    public static final int MAGIE = 0x4750414B; // "GPAK"
    public static final int VERSIE = 2;   // 2: entries by name (tiles "t_<tx>_<tz>", structures their path)
    private static final Pattern NAAM = Pattern.compile("t_(-?\\d+)_(-?\\d+)\\.nbt");

    private KaartPak() {
    }

    /** One tile, as vanilla sees it. {@code raw != null}: stored as the original gzip bytes. */
    public static final class Tegel {
        public boolean heeftVersie;
        public int versie;
        public int sx, sy, sz;
        public List<byte[]> palet = new ArrayList<>();   // compound payloads (verbatim)
        public int[] rooster;                             // y-z-x, state+1, 0 = no block
        public Map<Integer, byte[]> be = new LinkedHashMap<>(); // grid index -> compound payload (verbatim)
        public byte[] entiteiten;                         // list payload (element type, count, elements), null = no key
        public byte[] raw;

        public int index(int x, int y, int z) {
            return (y * sz + z) * sx + x;
        }
    }

    /** The loaded pack: global state table + per-tile blobs. */
    public static final class Pak {
        public final List<byte[]> staten = new ArrayList<>();
        public final List<byte[]> blobs = new ArrayList<>();
        public final Map<Long, Integer> tegels = new HashMap<>();
        /** Every entry by name (a tile: "t_<tx>_<tz>"; a structure: its path under structure/, without .nbt). */
        public final Map<String, Integer> namen = new HashMap<>();

        public static long sleutel(int tx, int tz) {
            return ((long) tx << 32) | (tz & 0xFFFFFFFFL);
        }
    }

    // ------------------------------------------------------------------ reading the vanilla .nbt (skipping parser)

    private static final class Lezer {
        final byte[] d;
        int p;

        Lezer(byte[] d) {
            this.d = d;
        }

        int u1() {
            return d[p++] & 0xFF;
        }

        int i4() {
            int v = ((d[p] & 0xFF) << 24) | ((d[p + 1] & 0xFF) << 16) | ((d[p + 2] & 0xFF) << 8) | (d[p + 3] & 0xFF);
            p += 4;
            return v;
        }

        int u2() {
            int v = ((d[p] & 0xFF) << 8) | (d[p + 1] & 0xFF);
            p += 2;
            return v;
        }

        String str() {
            int n = u2();
            String s = new String(d, p, n, java.nio.charset.StandardCharsets.UTF_8);
            p += n;
            return s;
        }

        void slaOver(int t) {
            switch (t) {
                case 1 -> p += 1;
                case 2 -> p += 2;
                case 3, 5 -> p += 4;
                case 4, 6 -> p += 8;
                case 7 -> { int n = i4(); p += n; }
                case 8 -> { int n = u2(); p += n; }
                case 9 -> { int et = u1(); int n = i4(); for (int i = 0; i < n; i++) slaOver(et); }
                case 10 -> { while (true) { int tt = u1(); if (tt == 0) return; int n = u2(); p += n; slaOver(tt); } }
                case 11 -> { int n = i4(); p += 4 * n; }
                case 12 -> { int n = i4(); p += 8 * n; }
                default -> throw new IllegalStateException("nbt tag " + t);
            }
        }

        byte[] stuk(int van) {
            return Arrays.copyOfRange(d, van, p);
        }
    }

    private static byte[] gunzip(byte[] gz) throws IOException {
        try (InputStream in = new GZIPInputStream(new ByteArrayInputStream(gz), 1 << 16)) {
            return in.readAllBytes();
        }
    }

    /** Parses a vanilla structure tile. Anything the grid cannot hold exactly -> raw mode. */
    public static Tegel leesNbt(byte[] gz) throws IOException {
        Tegel t = new Tegel();
        Lezer l = new Lezer(gunzip(gz));
        if (l.u1() != 10) throw new IOException("no root compound");
        int naamLen = l.u2();
        l.p += naamLen;
        int[] bx = null, by = null, bz = null, bst = null;
        byte[][] bnbt = null;
        boolean maatOk = false, rauw = false;
        while (true) {
            int tt = l.u1();
            if (tt == 0) break;
            String k = l.str();
            switch (k) {
                case "DataVersion" -> {
                    if (tt == 3) { t.heeftVersie = true; t.versie = l.i4(); } else { rauw = true; l.slaOver(tt); }
                }
                case "size" -> {
                    if (tt == 9 && l.d[l.p] == 3) {
                        l.u1();
                        int n = l.i4();
                        if (n == 3) { t.sx = l.i4(); t.sy = l.i4(); t.sz = l.i4(); maatOk = true; } else { l.p += 4 * n; }
                    } else {
                        l.slaOver(tt);
                    }
                }
                case "palette" -> {
                    if (tt == 9 && (l.d[l.p] == 10 || l.d[l.p] == 0)) {
                        l.u1();
                        int n = l.i4();
                        for (int i = 0; i < n; i++) { int v = l.p; l.slaOver(10); t.palet.add(l.stuk(v)); }
                    } else {
                        rauw = true;
                        l.slaOver(tt);
                    }
                }
                case "palettes" -> { rauw = true; l.slaOver(tt); }
                case "blocks" -> {
                    if (tt == 9 && (l.d[l.p] == 10 || l.d[l.p] == 0)) {
                        l.u1();
                        int n = l.i4();
                        bx = new int[n]; by = new int[n]; bz = new int[n]; bst = new int[n];
                        bnbt = new byte[n][];
                        for (int i = 0; i < n; i++) {
                            boolean pos = false, st = false;
                            while (true) {
                                int bt = l.u1();
                                if (bt == 0) break;
                                String bk = l.str();
                                if (bk.equals("pos") && bt == 9 && l.d[l.p] == 3) {
                                    l.u1();
                                    int c = l.i4();
                                    if (c == 3) { bx[i] = l.i4(); by[i] = l.i4(); bz[i] = l.i4(); pos = true; } else { l.p += 4 * c; }
                                } else if (bk.equals("state") && bt == 3) {
                                    bst[i] = l.i4();
                                    st = true;
                                } else if (bk.equals("nbt") && bt == 10) {
                                    int v = l.p;
                                    l.slaOver(10);
                                    bnbt[i] = l.stuk(v);
                                } else if (bk.equals("pos") || bk.equals("state") || bk.equals("nbt")) {
                                    rauw = true;   // an odd type: vanilla's defaults would kick in
                                    l.slaOver(bt);
                                } else {
                                    l.slaOver(bt);  // ignored by vanilla too
                                }
                            }
                            if (!pos || !st) rauw = true;
                        }
                    } else {
                        rauw = true;
                        l.slaOver(tt);
                    }
                }
                case "entities" -> {
                    if (tt == 9) { int v = l.p; l.slaOver(9); t.entiteiten = l.stuk(v); } else { rauw = true; l.slaOver(tt); }
                }
                default -> l.slaOver(tt);
            }
        }
        if (!maatOk || t.sx <= 0 || t.sy <= 0 || t.sz <= 0 || (long) t.sx * t.sy * t.sz > 64L * 1024 * 1024) rauw = true;
        if (!rauw && bx != null) {
            int np = t.palet.size();
            t.rooster = new int[t.sx * t.sy * t.sz];
            for (int i = 0; i < bx.length && !rauw; i++) {
                if (bx[i] < 0 || by[i] < 0 || bz[i] < 0 || bx[i] >= t.sx || by[i] >= t.sy || bz[i] >= t.sz || bst[i] < 0 || bst[i] >= np) {
                    rauw = true;
                    break;
                }
                int ix = t.index(bx[i], by[i], bz[i]);
                if (t.rooster[ix] != 0) { rauw = true; break; }   // a duplicate position
                t.rooster[ix] = bst[i] + 1;
                if (bnbt[i] != null) t.be.put(ix, bnbt[i]);
            }
        } else if (!rauw) {
            t.rooster = new int[t.sx * t.sy * t.sz];   // no "blocks" key: an empty tile
        }
        if (rauw) {
            Tegel r = new Tegel();
            r.raw = gz;
            return r;
        }
        return t;
    }

    // ------------------------------------------------------------------ encoding

    /** Body of one tile (before deflate). {@code ids}: the global state index of each palette entry. */
    static byte[] codeer(Tegel t, int[] ids) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(1 << 16);
        DataOutputStream o = new DataOutputStream(bo);
        o.writeBoolean(t.heeftVersie);
        o.writeInt(t.versie);
        o.writeInt(t.sx);
        o.writeInt(t.sy);
        o.writeInt(t.sz);
        o.writeInt(ids.length);
        for (int id : ids) o.writeInt(id);
        int laag = t.sx * t.sz, y0 = t.sy, y1 = -1;
        for (int y = 0; y < t.sy; y++) {
            for (int i = y * laag, e = i + laag; i < e; i++) {
                if (t.rooster[i] != 0) { if (y < y0) y0 = y; y1 = y; break; }
            }
        }
        int ny = y1 < y0 ? 0 : y1 - y0 + 1;
        o.writeInt(ny == 0 ? 0 : y0);
        o.writeInt(ny);
        boolean breed = ids.length >= 255;
        o.writeBoolean(breed);
        int van = (ny == 0 ? 0 : y0) * laag, tot = van + ny * laag;
        for (int i = van; i < tot; i++) {
            int v = t.rooster[i];
            if (breed) o.writeShort(v); else o.writeByte(v);
        }
        o.writeInt(t.be.size());
        for (var e : t.be.entrySet()) {
            o.writeInt(e.getKey());
            o.writeInt(e.getValue().length);
            o.write(e.getValue());
        }
        o.writeInt(t.entiteiten == null ? -1 : t.entiteiten.length);
        if (t.entiteiten != null) o.write(t.entiteiten);
        o.flush();
        return bo.toByteArray();
    }

    static byte[] deflate(byte[] in) {
        Deflater d = new Deflater(9, true);
        d.setInput(in);
        d.finish();
        ByteArrayOutputStream bo = new ByteArrayOutputStream(Math.max(64, in.length / 8));
        byte[] buf = new byte[1 << 16];
        while (!d.finished()) {
            int n = d.deflate(buf);
            bo.write(buf, 0, n);
        }
        d.end();
        return bo.toByteArray();
    }

    static byte[] inflate(byte[] in, int van) throws IOException {
        Inflater f = new Inflater(true);
        f.setInput(in, van, in.length - van);
        ByteArrayOutputStream bo = new ByteArrayOutputStream(in.length * 8);
        byte[] buf = new byte[1 << 16];
        try {
            while (!f.finished()) {
                int n = f.inflate(buf);
                if (n == 0 && (f.needsInput() || f.needsDictionary())) throw new IOException("cut-off tile");
                bo.write(buf, 0, n);
            }
        } catch (java.util.zip.DataFormatException e) {
            throw new IOException(e);
        } finally {
            f.end();
        }
        return bo.toByteArray();
    }

    // ------------------------------------------------------------------ decoding (shared with the runtime)

    public static Pak leesPak(InputStream in) throws IOException {
        DataInputStream i = new DataInputStream(new java.io.BufferedInputStream(in, 1 << 16));
        if (i.readInt() != MAGIE) throw new IOException("no GPAK");
        int v = i.readInt();
        if (v != VERSIE) throw new IOException("GPAK version " + v);
        Pak p = new Pak();
        int ns = i.readInt();
        for (int k = 0; k < ns; k++) { byte[] b = new byte[i.readInt()]; i.readFully(b); p.staten.add(b); }
        int nb = i.readInt();
        for (int k = 0; k < nb; k++) { byte[] b = new byte[i.readInt()]; i.readFully(b); p.blobs.add(b); }
        int nt = i.readInt();
        for (int k = 0; k < nt; k++) {
            String naam = i.readUTF();
            int b = i.readInt();
            p.namen.put(naam, b);
            Matcher m = NAAM.matcher(naam + ".nbt");
            if (m.matches()) p.tegels.put(Pak.sleutel(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))), b);
        }
        return p;
    }

    /** Decodes one blob. Raw mode: returns a Tegel with {@code raw} = the original gzip bytes. */
    public static Tegel decodeer(Pak p, int blob) throws IOException {
        byte[] b = p.blobs.get(blob);
        Tegel t = new Tegel();
        if (b[0] == 1) {
            t.raw = Arrays.copyOfRange(b, 1, b.length);
            return t;
        }
        DataInputStream i = new DataInputStream(new ByteArrayInputStream(inflate(b, 1)));
        t.heeftVersie = i.readBoolean();
        t.versie = i.readInt();
        t.sx = i.readInt();
        t.sy = i.readInt();
        t.sz = i.readInt();
        int np = i.readInt();
        for (int k = 0; k < np; k++) t.palet.add(p.staten.get(i.readInt()));
        int y0 = i.readInt(), ny = i.readInt();
        boolean breed = i.readBoolean();
        int laag = t.sx * t.sz;
        t.rooster = new int[t.sx * t.sy * t.sz];
        int van = y0 * laag, tot = van + ny * laag;
        if (breed) {
            for (int k = van; k < tot; k++) t.rooster[k] = i.readUnsignedShort();
        } else {
            byte[] rij = new byte[tot - van];
            i.readFully(rij);
            for (int k = 0; k < rij.length; k++) t.rooster[van + k] = rij[k] & 0xFF;
        }
        int nbe = i.readInt();
        for (int k = 0; k < nbe; k++) {
            int ix = i.readInt();
            byte[] c = new byte[i.readInt()];
            i.readFully(c);
            t.be.put(ix, c);
        }
        int ne = i.readInt();
        if (ne >= 0) { t.entiteiten = new byte[ne]; i.readFully(t.entiteiten); }
        return t;
    }

    // ------------------------------------------------------------------ build-time commands

    /** One input: its entry name and file. */
    private record Invoer(String naam, File f) {
    }

    /**
     * Mode "tegels": the t_*.nbt of one folder (by coordinates); mode "alles": every .nbt below the folder (by path), except
     * the sub folder {@code behalve} (relative, '/' separated; the map tiles have their own pack).
     */
    private static List<Invoer> invoer(File map, boolean alles, String behalve) throws IOException {
        List<Invoer> l = new ArrayList<>();
        if (!alles) {
            for (File f : tegelBestanden(map)) l.add(new Invoer(f.getName().substring(0, f.getName().length() - 4), f));
            return l;
        }
        java.nio.file.Path wortel = map.toPath();
        try (var st = Files.walk(wortel)) {
            st.filter(q -> Files.isRegularFile(q) && q.getFileName().toString().endsWith(".nbt")).forEach(q -> {
                String rel = wortel.relativize(q).toString().replace(File.separatorChar, '/');
                if (behalve != null && !behalve.isEmpty() && rel.startsWith(behalve + "/")) return;
                l.add(new Invoer(rel.substring(0, rel.length() - 4), q.toFile()));
            });
        }
        l.sort((a, b) -> a.naam().compareTo(b.naam()));
        return l;
    }

    private static List<File> tegelBestanden(File map) {
        File[] fs = map.listFiles((d, n) -> NAAM.matcher(n).matches());
        List<File> l = new ArrayList<>(fs == null ? List.of() : Arrays.asList(fs));
        l.sort((a, b) -> {
            int[] x = coords(a), y = coords(b);
            return x[0] != y[0] ? Integer.compare(x[0], y[0]) : Integer.compare(x[1], y[1]);
        });
        return l;
    }

    private static int[] coords(File f) {
        Matcher m = NAAM.matcher(f.getName());
        if (!m.matches()) throw new IllegalArgumentException(f.getName());
        return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
    }

    private static final class Sleutel {
        final byte[] b;
        final int h;

        Sleutel(byte[] b) {
            this.b = b;
            this.h = Arrays.hashCode(b);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Sleutel s && s.h == h && Arrays.equals(s.b, b);
        }

        @Override
        public int hashCode() {
            return h;
        }
    }

    public static void pak(File map, File uit, boolean alles, String behalve) throws Exception {
        long t0 = System.currentTimeMillis();
        List<Invoer> in = invoer(map, alles, behalve);
        List<File> fs = in.stream().map(Invoer::f).toList();
        int draden = Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 1));
        ExecutorService ex = Executors.newFixedThreadPool(draden);
        try {
            // pass 1: the palettes, in a fixed order -> a deterministic global state table
            List<Future<List<byte[]>>> pal = new ArrayList<>();
            for (File f : fs) pal.add(ex.submit(() -> { Tegel t = leesNbt(Files.readAllBytes(f.toPath())); return t.raw != null ? List.<byte[]>of() : t.palet; }));
            Map<Sleutel, Integer> staatId = new LinkedHashMap<>();
            List<byte[]> staten = new ArrayList<>();
            for (var fu : pal) for (byte[] s : fu.get()) staatId.computeIfAbsent(new Sleutel(s), k -> { staten.add(s); return staten.size() - 1; });
            // pass 2: encode
            List<Future<byte[]>> blob = new ArrayList<>();
            for (File f : fs) {
                blob.add(ex.submit(() -> {
                    byte[] gz = Files.readAllBytes(f.toPath());
                    Tegel t = leesNbt(gz);
                    if (t.raw != null) {
                        byte[] r = new byte[gz.length + 1];
                        r[0] = 1;
                        System.arraycopy(gz, 0, r, 1, gz.length);
                        return r;
                    }
                    int[] ids = new int[t.palet.size()];
                    for (int k = 0; k < ids.length; k++) ids[k] = staatId.get(new Sleutel(t.palet.get(k)));
                    byte[] d = deflate(codeer(t, ids));
                    byte[] r = new byte[d.length + 1];
                    System.arraycopy(d, 0, r, 1, d.length);
                    return r;
                }));
            }
            Map<Sleutel, Integer> blobId = new HashMap<>();
            List<byte[]> blobs = new ArrayList<>();
            int[] index = new int[fs.size()];
            int rauw = 0;
            long ruw = 0;
            for (int k = 0; k < fs.size(); k++) {
                byte[] b = blob.get(k).get();
                if (b[0] == 1) rauw++;
                ruw += fs.get(k).length();
                index[k] = blobId.computeIfAbsent(new Sleutel(b), s -> { blobs.add(b); return blobs.size() - 1; });
            }
            uit.getParentFile().mkdirs();
            File tmp = new File(uit.getPath() + ".tmp");
            try (DataOutputStream o = new DataOutputStream(new java.io.BufferedOutputStream(new java.io.FileOutputStream(tmp), 1 << 16))) {
                o.writeInt(MAGIE);
                o.writeInt(VERSIE);
                o.writeInt(staten.size());
                for (byte[] s : staten) { o.writeInt(s.length); o.write(s); }
                o.writeInt(blobs.size());
                for (byte[] b : blobs) { o.writeInt(b.length); o.write(b); }
                o.writeInt(fs.size());
                for (int k = 0; k < fs.size(); k++) {
                    o.writeUTF(in.get(k).naam());
                    o.writeInt(index[k]);
                }
            }
            Files.move(tmp.toPath(), uit.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            System.out.printf("kaartPak: %d entries (%d unique, %d raw), %d states: %.1f MB -> %.2f MB (%.1fx) in %.1f s%n", fs.size(), blobs.size(),
                    rauw, staten.size(), ruw / 1e6, uit.length() / 1e6, ruw / (double) Math.max(1, uit.length()), (System.currentTimeMillis() - t0) / 1000.0);
        } finally {
            ex.shutdownNow();
        }
    }

    /** Compares every tile in the pack with its .nbt (everything vanilla's load reads). Returns the number of differences. */
    public static int controleer(File map, File pakBestand, boolean alles, String behalve) throws Exception {
        long t0 = System.currentTimeMillis();
        Pak p;
        try (InputStream in = new java.io.FileInputStream(pakBestand)) {
            p = leesPak(in);
        }
        List<Invoer> fs = invoer(map, alles, behalve);
        int fout = 0;
        if (fs.size() != p.namen.size()) {
            System.out.println("entry count differs: " + fs.size() + " .nbt vs " + p.namen.size() + " in the pack");
            fout++;
        }
        ExecutorService ex = Executors.newFixedThreadPool(Math.max(1, Math.min(8, Runtime.getRuntime().availableProcessors() - 1)));
        try {
            List<Future<String>> r = new ArrayList<>();
            for (Invoer iv : fs) {
                File f = iv.f();
                r.add(ex.submit(() -> {
                    Integer b = p.namen.get(iv.naam());
                    if (b == null) return iv.naam() + ": not in the pack";
                    byte[] gz = Files.readAllBytes(f.toPath());
                    Tegel a = leesNbt(gz), n = decodeer(p, b);
                    if (a.raw != null || n.raw != null) {
                        return Arrays.equals(gz, n.raw) ? null : f.getName() + ": raw bytes differ";
                    }
                    return verschil(a, n) == null ? null : f.getName() + ": " + verschil(a, n);
                }));
            }
            for (var fu : r) {
                String s = fu.get();
                if (s != null) { System.out.println(s); fout++; }
            }
        } finally {
            ex.shutdownNow();
        }
        System.out.printf("kaartPak controle: %d entries, %d differences (%.1f s)%n", fs.size(), fout, (System.currentTimeMillis() - t0) / 1000.0);
        return fout;
    }

    static String verschil(Tegel a, Tegel b) {
        if (a.heeftVersie != b.heeftVersie || a.versie != b.versie) return "DataVersion";
        if (a.sx != b.sx || a.sy != b.sy || a.sz != b.sz) return "size";
        if (a.palet.size() != b.palet.size()) return "palette size";
        for (int k = 0; k < a.palet.size(); k++) if (!Arrays.equals(a.palet.get(k), b.palet.get(k))) return "palette entry " + k;
        if (!Arrays.equals(a.rooster, b.rooster)) return "blocks";
        if (a.be.size() != b.be.size()) return "block entity count";
        for (var e : a.be.entrySet()) if (!Arrays.equals(e.getValue(), b.be.get(e.getKey()))) return "block entity at index " + e.getKey();
        if (!Arrays.equals(a.entiteiten, b.entiteiten)) return "entities";
        return null;
    }

    public static void main(String[] a) throws Exception {
        // pak|controleer <tegels dir> <kaart.gpak>              the map tiles (t_*.nbt of one folder)
        // pakAlles|controleerAlles <structure dir> <.gpak> [skip] every .nbt below the folder, except the sub folder skip
        // beide|controleerBeide <structure dir> <out root>     both packs: <out>/data/guhs/vadsrijk/kaart.gpak + <out>/data/guhs/structuren.gpak
        String behalve = a.length > 3 ? a[3] : null;
        if (a.length == 3 && (a[0].equals("beide") || a[0].equals("controleerBeide"))) {
            File st = new File(a[1]), tegels = new File(st, "vadsrijk/tegels");
            File kaart = new File(a[2], "data/guhs/vadsrijk/kaart.gpak"), rest = new File(a[2], "data/guhs/structuren.gpak");
            if (a[0].equals("beide")) {
                pak(tegels, kaart, false, null);
                pak(st, rest, true, "vadsrijk/tegels");
            } else if (controleer(tegels, kaart, false, null) + controleer(st, rest, true, "vadsrijk/tegels") != 0) {
                System.exit(1);
            }
            return;
        }
        if (a.length == 3 && a[0].equals("pak")) {
            pak(new File(a[1]), new File(a[2]), false, null);
        } else if (a.length == 3 && a[0].equals("controleer")) {
            if (controleer(new File(a[1]), new File(a[2]), false, null) != 0) System.exit(1);
        } else if (a.length >= 3 && a[0].equals("pakAlles")) {
            pak(new File(a[1]), new File(a[2]), true, behalve);
        } else if (a.length >= 3 && a[0].equals("controleerAlles")) {
            if (controleer(new File(a[1]), new File(a[2]), true, behalve) != 0) System.exit(1);
        } else {
            System.err.println("usage: pak|controleer <tegels dir> <kaart.gpak>  or  pakAlles|controleerAlles <structure dir> <.gpak> [skip]");
            System.exit(2);
        }
    }
}
