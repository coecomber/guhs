package nl.juiced.guhs.feature.erfenis.koppeling;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Everything the koppeling (village <-> real base) remembers, saved with the overworld ({@code data/guhs/erfenis_koppeling.dat};
 * a real codec, so the waiting post keeps its item components):
 * <ul>
 *   <li>per player ({@link Speler}): the letter's clock and state, the Erfhekje they last used, their Decobonnen and the
 *       Leverkist allowance ({@link LeverkistPlafond});</li>
 *   <li>per owner: the post that waits for a Leverkist ({@link #post}) and their Leverkisten in placing order ({@link #kisten});</li>
 *   <li>every Dorpszender ({@link Zender}): owner, power and whether its net ran when we last looked (also while unloaded).</li>
 * </ul>
 */
public final class KoppelingData extends SavedData {
    /** The letter: still waiting for the post, delivered (in the pockets), bought (the Erfhekje was handed out). */
    public static final int WACHT = 0, BEZORGD = 1, GEKOCHT = 2;

    public static final class Speler {
        int briefTicks;
        int briefStaat;
        @Nullable
        GlobalPos hekje;
        float hekjeYaw;
        int decobonnen;
        final Map<String, Double> tegoed;
        long tegoedTijd;

        Speler() {
            this(0, WACHT, Optional.empty(), 0f, 0, Map.of(), 0L);
        }

        Speler(int briefTicks, int briefStaat, Optional<GlobalPos> hekje, float hekjeYaw, int decobonnen, Map<String, Double> tegoed, long tegoedTijd) {
            this.briefTicks = briefTicks;
            this.briefStaat = briefStaat;
            this.hekje = hekje.orElse(null);
            this.hekjeYaw = hekjeYaw;
            this.decobonnen = decobonnen;
            this.tegoed = new HashMap<>(tegoed);
            this.tegoedTijd = tegoedTijd;
        }

        static final Codec<Speler> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("BriefTicks", 0).forGetter(s -> s.briefTicks),
                Codec.INT.optionalFieldOf("BriefStaat", WACHT).forGetter(s -> s.briefStaat),
                GlobalPos.CODEC.optionalFieldOf("Hekje").forGetter(s -> Optional.ofNullable(s.hekje)),
                Codec.FLOAT.optionalFieldOf("HekjeYaw", 0f).forGetter(s -> s.hekjeYaw),
                Codec.INT.optionalFieldOf("Decobonnen", 0).forGetter(s -> s.decobonnen),
                Codec.unboundedMap(Codec.STRING, Codec.DOUBLE).optionalFieldOf("Tegoed", Map.of()).forGetter(s -> s.tegoed),
                Codec.LONG.optionalFieldOf("TegoedTijd", 0L).forGetter(s -> s.tegoedTijd)
        ).apply(i, Speler::new));
    }

    /** A Dorpszender: where, whose, how much it sends (VK, before the loss) and whether its net ran at the last look. */
    public record Zender(GlobalPos pos, UUID eigenaar, int vermogen, boolean aan) {
        static final Codec<Zender> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("Pos").forGetter(Zender::pos),
                UUIDUtil.CODEC.fieldOf("Eigenaar").forGetter(Zender::eigenaar),
                Codec.INT.optionalFieldOf("Vermogen", 0).forGetter(Zender::vermogen),
                Codec.BOOL.optionalFieldOf("Aan", false).forGetter(Zender::aan)
        ).apply(i, Zender::new));
    }

    static final Codec<KoppelingData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Speler.CODEC).optionalFieldOf("Spelers", Map.of()).forGetter(d -> d.spelers),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, ItemStack.CODEC.listOf()).optionalFieldOf("Post", Map.of()).forGetter(d -> d.post),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, GlobalPos.CODEC.listOf()).optionalFieldOf("Kisten", Map.of()).forGetter(d -> d.kisten),
            Zender.CODEC.listOf().optionalFieldOf("Zenders", List.of()).forGetter(d -> List.copyOf(d.zenders.values()))
    ).apply(i, KoppelingData::new));

    static final SavedDataType<KoppelingData> TYPE = GuhSavedData.type("erfenis_koppeling", KoppelingData::new, CODEC);

    final Map<UUID, Speler> spelers = new HashMap<>();
    final Map<UUID, List<ItemStack>> post = new LinkedHashMap<>();
    final Map<UUID, List<GlobalPos>> kisten = new HashMap<>();
    final Map<GlobalPos, Zender> zenders = new LinkedHashMap<>();

    KoppelingData() {
    }

    private KoppelingData(Map<UUID, Speler> spelers, Map<UUID, List<ItemStack>> post, Map<UUID, List<GlobalPos>> kisten, List<Zender> zenders) {
        this.spelers.putAll(spelers);
        post.forEach((k, v) -> this.post.put(k, new ArrayList<>(v.stream().filter(s -> !s.isEmpty()).toList())));
        kisten.forEach((k, v) -> this.kisten.put(k, new ArrayList<>(v)));
        for (Zender z : zenders) {
            this.zenders.put(z.pos(), z);
        }
    }

    public static KoppelingData get(MinecraftServer server) {
        return GuhSavedData.get(server.overworld(), TYPE, "guhs_erfenis_koppeling");
    }

    Speler speler(UUID id) {
        return spelers.computeIfAbsent(id, k -> new Speler());
    }

    @Nullable
    Speler spelerAls(UUID id) {
        return spelers.get(id);
    }

    List<ItemStack> post(UUID eigenaar) {
        return post.computeIfAbsent(eigenaar, k -> new ArrayList<>());
    }

    List<GlobalPos> kisten(UUID eigenaar) {
        return kisten.computeIfAbsent(eigenaar, k -> new ArrayList<>());
    }

    /** Adds a Leverkist at the end of its owner's list (and takes it off anybody else's). */
    void voegKistToe(UUID eigenaar, GlobalPos pos) {
        for (Map.Entry<UUID, List<GlobalPos>> e : kisten.entrySet()) {
            if (!e.getKey().equals(eigenaar)) {
                e.getValue().remove(pos);
            }
        }
        List<GlobalPos> l = kisten(eigenaar);
        if (!l.contains(pos)) {
            l.add(pos);
            setDirty();
        }
    }

    void haalKistWeg(GlobalPos pos) {
        for (List<GlobalPos> l : kisten.values()) {
            if (l.remove(pos)) {
                setDirty();
            }
        }
    }

    void zetZender(Zender z) {
        if (!z.equals(zenders.put(z.pos(), z))) {
            setDirty();
        }
    }

    void haalZenderWeg(GlobalPos pos) {
        if (zenders.remove(pos) != null) {
            setDirty();
        }
    }
}
