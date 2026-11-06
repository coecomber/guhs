package nl.juiced.guhs.feature.vadsrijk.grootte;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;

/**
 * U-GROOTTE: reads the Vadsrijk map tiles from the compact pack {@code data/guhs/vadsrijk/kaart.gpak} (see {@link KaartPak}).
 * A tile is rebuilt into the same CompoundTag the vanilla .nbt held and loaded exactly like vanilla's TemplateSource does
 * (DataFixTypes.STRUCTURE + StructureTemplate.load with the server's block lookup), so placement is unchanged.
 * <p>No pack (an old build / a dev tree without the kaartPak task): {@link #tegel} returns {@code null} and the caller falls
 * back to the structure manager (.nbt). Tiles are cached here until {@link #vergeet} (KaartFeature forgets a tile as soon as its
 * 16 chunks are placed).
 */
public final class KaartPakLader {
    public static final Identifier PAK = Guhs.id("vadsrijk/kaart.gpak");
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("guhs/kaartpak");
    private static final Map<Long, Optional<StructureTemplate>> CACHE = new ConcurrentHashMap<>();
    private static WeakReference<MinecraftServer> van = new WeakReference<>(null);
    private static KaartPak.Pak pak;
    private static boolean geenPak;

    private KaartPakLader() {
    }

    /** The pack of this server (loaded once per server). null = no pack in the resources. */
    private static synchronized KaartPak.Pak pak(MinecraftServer s) {
        if (van.get() != s) {
            van = new WeakReference<>(s);
            CACHE.clear();
            pak = null;
            geenPak = false;
            var res = s.getResourceManager().getResource(PAK);
            if (res.isEmpty()) {
                geenPak = true;
                LOG.info("no {}: map tiles come from the .nbt structures", PAK);
            } else {
                long t0 = System.nanoTime();
                try (InputStream in = res.get().open()) {
                    pak = KaartPak.leesPak(in);
                    LOG.info("map pack: {} tiles, {} unique, {} states ({} ms)", pak.tegels.size(), pak.blobs.size(), pak.staten.size(),
                            (System.nanoTime() - t0) / 1_000_000);
                } catch (IOException e) {
                    LOG.error("map pack {} unreadable: falling back to the .nbt tiles", PAK, e);
                    pak = null;
                    geenPak = true;
                }
            }
        }
        return geenPak ? null : pak;
    }

    /** The tile (cached), {@code Optional.empty()} = no such tile, {@code null} = no pack (use the .nbt). */
    public static Optional<StructureTemplate> tegel(MinecraftServer s, int tx, int tz) {
        KaartPak.Pak p = pak(s);
        if (p == null) return null;
        long k = KaartPak.Pak.sleutel(tx, tz);
        Integer blob = p.tegels.get(k);
        if (blob == null) return Optional.empty();
        return CACHE.computeIfAbsent(k, kk -> {
            try {
                return Optional.of(bouw(s, p, blob));
            } catch (Exception e) {
                LOG.error("map tile {},{} unreadable", tx, tz, e);
                return Optional.empty();
            }
        });
    }

    /** Is there a tile here? (no decoding; null = no pack) */
    public static Boolean heeft(MinecraftServer s, int tx, int tz) {
        KaartPak.Pak p = pak(s);
        return p == null ? null : p.tegels.containsKey(KaartPak.Pak.sleutel(tx, tz));
    }

    /** All tile keys of the pack ({@link KaartPak.Pak#sleutel}); empty without a pack. */
    public static java.util.Set<Long> sleutels(MinecraftServer s) {
        KaartPak.Pak p = pak(s);
        return p == null ? java.util.Set.of() : java.util.Set.copyOf(p.tegels.keySet());
    }

    /** Number of distinct tiles stored (identical tiles once); 0 without a pack. */
    public static int uniek(MinecraftServer s) {
        KaartPak.Pak p = pak(s);
        return p == null ? 0 : p.blobs.size();
    }

    public static void vergeet(int tx, int tz) {
        CACHE.remove(KaartPak.Pak.sleutel(tx, tz));
    }

    /** The CompoundTag a vanilla .nbt tile would give: (re)built from the pack. */
    public static CompoundTag alsNbt(KaartPak.Tegel t) throws IOException {
        if (t.raw != null) {
            return NbtIo.readCompressed(new ByteArrayInputStream(t.raw), NbtAccounter.unlimitedHeap());
        }
        CompoundTag tag = new CompoundTag();
        if (t.heeftVersie) tag.putInt("DataVersion", t.versie);
        tag.put("size", ints(t.sx, t.sy, t.sz));
        ListTag pal = new ListTag(t.palet.size());
        for (byte[] b : t.palet) pal.add(compound(b));
        tag.put("palette", pal);
        ListTag blokken = new ListTag();
        int[] r = t.rooster;
        int laag = t.sx * t.sz;
        for (int i = 0; i < r.length; i++) {
            if (r[i] == 0) continue;
            int y = i / laag, rest = i - y * laag, z = rest / t.sx, x = rest - z * t.sx;
            CompoundTag b = new CompoundTag();
            b.put("pos", ints(x, y, z));
            b.putInt("state", r[i] - 1);
            byte[] be = t.be.get(i);
            if (be != null) b.put("nbt", compound(be));
            blokken.add(b);
        }
        tag.put("blocks", blokken);
        if (t.entiteiten != null) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream(t.entiteiten.length + 16);
            DataOutputStream o = new DataOutputStream(bo);
            o.writeByte(9);
            o.writeUTF("entities");
            o.write(t.entiteiten);
            o.writeByte(0);
            CompoundTag e = compound(bo.toByteArray());
            tag.put("entities", e.get("entities"));
        }
        return tag;
    }

    private static StructureTemplate bouw(MinecraftServer s, KaartPak.Pak p, int blob) throws IOException {
        return template(s, KaartPak.decodeer(p, blob));
    }

    /** A decoded pack entry as a StructureTemplate, loaded exactly like vanilla's TemplateSource.readStructure. */
    static StructureTemplate template(MinecraftServer s, KaartPak.Tegel t) throws IOException {
        CompoundTag tag = alsNbt(t);
        // exactly vanilla's TemplateSource.readStructure (+ the server's block lookup, as in MinecraftServer's template manager)
        HolderGetter<Block> blokken = s.registryAccess().lookupOrThrow(Registries.BLOCK).filterFeatures(s.getWorldData().enabledFeatures());
        StructureTemplate st = new StructureTemplate();
        int versie = NbtUtils.getDataVersion(tag, 500);
        st.load(blokken, DataFixTypes.STRUCTURE.updateToCurrentVersion(s.getFixerUpper(), tag, versie));
        return st;
    }

    private static ListTag ints(int a, int b, int c) {
        ListTag l = new ListTag(3);
        l.add(IntTag.valueOf(a));
        l.add(IntTag.valueOf(b));
        l.add(IntTag.valueOf(c));
        return l;
    }

    /** A compound payload (or a full compound body) as a fresh CompoundTag. */
    private static CompoundTag compound(byte[] payload) throws IOException {
        byte[] b = new byte[payload.length + 3];
        b[0] = 10;   // tag type compound, name "" (2 zero bytes), then the payload
        System.arraycopy(payload, 0, b, 3, payload.length);
        return NbtIo.read(new DataInputStream(new ByteArrayInputStream(b)));
    }
}
