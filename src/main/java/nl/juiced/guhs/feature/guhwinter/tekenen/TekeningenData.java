package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Every Guhwinter drawing (SavedData {@code guhs:guhwinter_tekeningen} in the overworld), plus the kerstboom bookkeeping:
 * which drawing each player hung in the tree (one per player), who already got a Kerstalbum, and which album drawings each
 * player already took as a 2x2 drawing (once per drawing per player).
 */
public final class TekeningenData extends SavedData {
    public static final Codec<TekeningenData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Tekening.CODEC.listOf().optionalFieldOf("tekeningen", List.of()).forGetter(d -> List.copyOf(d.tekeningen.values())),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC).optionalFieldOf("kerstboom", Map.of()).forGetter(d -> d.kerstboom),
            UUIDUtil.STRING_CODEC.listOf().optionalFieldOf("album_gekregen", List.of()).forGetter(d -> List.copyOf(d.albumGekregen)),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC.listOf()).optionalFieldOf("album_gepakt", Map.of())
                    .forGetter(TekeningenData::gepaktLijsten)
    ).apply(i, TekeningenData::new));
    public static final SavedDataType<TekeningenData> TYPE = GuhSavedData.type("guhwinter_tekeningen", TekeningenData::new, CODEC);

    private final Map<UUID, Tekening> tekeningen = new LinkedHashMap<>();
    /** player -> their kerstboom drawing. */
    private final Map<UUID, UUID> kerstboom = new LinkedHashMap<>();
    private final Set<UUID> albumGekregen = new LinkedHashSet<>();
    /** player -> the album drawings they took. */
    private final Map<UUID, Set<UUID>> albumGepakt = new HashMap<>();

    public TekeningenData() {
    }

    private TekeningenData(List<Tekening> lijst, Map<UUID, UUID> boom, List<UUID> gekregen, Map<UUID, List<UUID>> gepakt) {
        lijst.forEach(t -> tekeningen.put(t.id(), t));
        kerstboom.putAll(boom);
        albumGekregen.addAll(gekregen);
        gepakt.forEach((k, v) -> albumGepakt.put(k, new LinkedHashSet<>(v)));
    }

    public static TekeningenData get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_guhwinter_tekeningen");
    }

    private Map<UUID, List<UUID>> gepaktLijsten() {
        Map<UUID, List<UUID>> m = new LinkedHashMap<>();
        albumGepakt.forEach((k, v) -> m.put(k, List.copyOf(v)));
        return m;
    }

    // ------------------------------------------------------------------------------------------------ drawings

    @Nullable
    public Tekening get(UUID id) {
        return tekeningen.get(id);
    }

    public void zet(Tekening t) {
        tekeningen.put(t.id(), t);
        setDirty();
    }

    public int aantal() {
        return tekeningen.size();
    }

    public List<Tekening> alle() {
        return List.copyOf(tekeningen.values());
    }

    // ------------------------------------------------------------------------------------------------ kerstboom

    @Nullable
    public UUID kerstboomVan(UUID speler) {
        return kerstboom.get(speler);
    }

    public void zetKerstboom(UUID speler, UUID tekening) {
        if (!tekening.equals(kerstboom.put(speler, tekening))) {
            setDirty();
        }
    }

    public boolean isKerstboom(UUID tekening) {
        return kerstboom.containsValue(tekening);
    }

    /** Every drawing in the kerstboom, oldest first (the album's order). */
    public List<Tekening> kerstboomTekeningen() {
        List<Tekening> l = new ArrayList<>();
        for (UUID id : kerstboom.values()) {
            Tekening t = tekeningen.get(id);
            if (t != null) {
                l.add(t);
            }
        }
        l.sort(Comparator.comparingLong(Tekening::gemaakt).thenComparing(Tekening::makerNaam));
        return l;
    }

    // ------------------------------------------------------------------------------------------------ album

    public boolean albumGekregen(UUID speler) {
        return albumGekregen.contains(speler);
    }

    public void markeerAlbum(UUID speler) {
        if (albumGekregen.add(speler)) {
            setDirty();
        }
    }

    public boolean gepakt(UUID speler, UUID tekening) {
        Set<UUID> s = albumGepakt.get(speler);
        return s != null && s.contains(tekening);
    }

    /** Marks it taken; false when it already was. */
    public boolean pak(UUID speler, UUID tekening) {
        if (albumGepakt.computeIfAbsent(speler, k -> new HashSet<>()).add(tekening)) {
            setDirty();
            return true;
        }
        return false;
    }
}
