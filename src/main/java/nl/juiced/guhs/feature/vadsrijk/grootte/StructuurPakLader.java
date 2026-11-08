package nl.juiced.guhs.feature.vadsrijk.grootte;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import nl.juiced.guhs.Guhs;

/**
 * U-GROOTTE: all other guhs structures ({@code data/guhs/structure/**.nbt}, ~70 MB as vanilla NBT) ship as ONE compact pack
 * {@code data/guhs/structuren.gpak} (same format as the map pack, see {@link KaartPak}; task kaartPak). The jar leaves the .nbt
 * out. {@code mixin.StructuurPakMixin} asks this class when vanilla's resource template source is about to load a structure:
 * ONLY when its .nbt is not in the resources (so dev trees, datapack overrides and the world's own saved structures keep
 * working exactly as before) and the pack has it, the template comes from the pack.
 */
public final class StructuurPakLader {
    public static final Identifier PAK = Guhs.id("structuren.gpak");
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("guhs/kaartpak");
    private static WeakReference<ResourceManager> van = new WeakReference<>(null);
    private static @Nullable KaartPak.Pak pak;

    private StructuurPakLader() {
    }

    private static synchronized @Nullable KaartPak.Pak pak(ResourceManager rm) {
        if (van.get() != rm) {
            van = new WeakReference<>(rm);
            pak = null;
            var res = rm.getResource(PAK);
            if (res.isPresent()) {
                try (InputStream in = res.get().open()) {
                    pak = KaartPak.leesPak(in);
                    LOG.info("structure pack: {} structures ({} unique)", pak.namen.size(), pak.blobs.size());
                } catch (IOException e) {
                    LOG.error("structure pack {} unreadable", PAK, e);
                }
            }
        }
        return pak;
    }

    /** The template from the pack, or null when vanilla should handle it (not ours, the .nbt exists, or not in the pack). */
    public static @Nullable Optional<StructureTemplate> laad(ResourceManager rm, Identifier id) {
        if (!id.getNamespace().equals(Guhs.MODID)) return null;
        if (rm.getResource(id.withPath("structure/" + id.getPath() + ".nbt")).isPresent()) return null;   // the .nbt wins
        KaartPak.Pak p = pak(rm);
        Integer b = p == null ? null : p.namen.get(id.getPath());
        if (b == null) return null;
        MinecraftServer s = ServerLifecycleHooks.getCurrentServer();
        if (s == null) return null;
        try {
            return Optional.of(KaartPakLader.template(s, KaartPak.decodeer(p, b)));
        } catch (Exception e) {
            LOG.error("structure {} unreadable in the pack", id, e);
            return Optional.empty();
        }
    }

    /** The structure ids in the pack (for the template listing). */
    public static Stream<Identifier> lijst(ResourceManager rm) {
        KaartPak.Pak p = pak(rm);
        return p == null ? Stream.empty() : p.namen.keySet().stream().map(Guhs::id);
    }

    /** For the gametest: the pack's template of a structure, regardless of the .nbt. Empty = not in the pack / no pack. */
    public static Optional<StructureTemplate> uitPak(MinecraftServer s, String pad) throws IOException {
        KaartPak.Pak p = pak(s.getResourceManager());
        Integer b = p == null ? null : p.namen.get(pad);
        return b == null ? Optional.empty() : Optional.of(KaartPakLader.template(s, KaartPak.decodeer(p, b)));
    }

    public static java.util.Set<String> namen(MinecraftServer s) {
        KaartPak.Pak p = pak(s.getResourceManager());
        return p == null ? java.util.Set.of() : java.util.Set.copyOf(p.namen.keySet());
    }
}
