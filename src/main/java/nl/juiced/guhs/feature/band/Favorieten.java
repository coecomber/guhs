package nl.juiced.guhs.feature.band;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.disco.DiscoLiedje;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.wereldleven.Koortje;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;

/**
 * The secret favourites of every band guh (2.10): the data side. Each guh gets one favourite per {@link FavorietSoort},
 * made once, deterministic from {@link #SALT} and its band id (VRIEND only once the owner has a second guh). Which are
 * discovered is stored too. Finding them (hints, heart explosions, the happy buff) is the favorietjes feature.
 */
public final class Favorieten {
    public static final long SALT = 20210201L;
    /** The colours a guh can love (lang gui.guhs.favoriet.kleur.&lt;id&gt;); favorietjes fills the clothes -&gt; colour table. */
    public static final List<String> KLEUREN = List.of("roze", "rood", "oranje", "geel", "groen", "mint", "blauw", "paars", "wit", "zwart",
            "bruin", "goud");
    /** The toys (speelgoed; lang gui.guhs.speeltje.&lt;id&gt;). */
    public static final List<String> SPEELTJES = List.of("knabbelbal", "glijbaantje", "tunnel", "wip_schommel");
    /** How many emotes existed before 2.10 (the favourite emote is one of those). */
    public static final int OUDE_EMOTES = 10;

    public static final TagKey<Item> FAVORIET_ETEN = TagKey.create(Registries.ITEM, Guhs.id("band/favoriet_eten"));
    public static final TagKey<Biome> FAVORIETE_PLEKKEN = TagKey.create(Registries.BIOME, Guhs.id("band/favoriete_plekken"));

    private static final Map<GuhClothes, String> KLEUR_VAN = new HashMap<>();

    private Favorieten() {
    }

    /** Everything a favourite of this kind can be (sorted, so the choice is stable). VRIEND: empty (see {@link #van}). */
    public static List<String> kandidaten(MinecraftServer s, FavorietSoort soort) {
        List<String> out = new ArrayList<>();
        switch (soort) {
            case ETEN -> BuiltInRegistries.ITEM.getTagOrEmpty(FAVORIET_ETEN)
                    .forEach(h -> out.add(BuiltInRegistries.ITEM.getKey(h.value()).toString()));
            case PLEK -> s.registryAccess().registryOrThrow(Registries.BIOME).getTagOrEmpty(FAVORIETE_PLEKKEN)
                    .forEach(h -> h.unwrapKey().ifPresent(k -> out.add(k.location().toString())));
            case KNUFFEL -> out.addAll(WereldlevenFeature.KNUFFEL_IDS);
            case LIEDJE -> {
                for (Koortje.Liedje l : Koortje.Liedje.values()) {
                    out.add("koortje:" + l.id());
                }
                for (DiscoLiedje l : DiscoLiedje.values()) {
                    out.add("disco:" + l.id);
                }
            }
            case SPEELTJE -> out.addAll(SPEELTJES);
            case EMOTE -> {
                for (int i = 0; i < OUDE_EMOTES && i < Emote.values().length; i++) {
                    out.add(Emote.values()[i].id());
                }
            }
            case KLEUR -> out.addAll(KLEUREN);
            case VRIEND -> {
            }
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    /** All favourites of this guh (made the first time; VRIEND once the owner has another guh). Empty for other mobs. */
    public static Map<FavorietSoort, String> van(Mob guh) {
        BandData.Rec r = Band.rec(guh);
        if (r == null) {
            return Map.of();
        }
        MinecraftServer s = guh.getServer();
        boolean veranderd = false;
        for (FavorietSoort soort : FavorietSoort.values()) {
            if (r.fav.containsKey(soort)) {
                continue;
            }
            List<String> k = soort == FavorietSoort.VRIEND ? vriendKandidaten(s, Band.eigenaar(guh), r.id) : kandidaten(s, soort);
            if (!k.isEmpty()) {
                r.fav.put(soort, k.get(rng(r.id, soort).nextInt(k.size())));
                veranderd = true;
            }
        }
        if (veranderd) {
            BandData.get(s).setDirty();
        }
        return r.fav.isEmpty() ? Map.of() : new EnumMap<>(r.fav);
    }

    private static List<String> vriendKandidaten(MinecraftServer s, UUID eigenaar, UUID self) {
        List<String> out = new ArrayList<>();
        for (BandData.Rec o : BandData.get(s).guhsVan(eigenaar)) {
            if (!o.id.equals(self)) {
                out.add(o.id.toString());
            }
        }
        out.sort(Comparator.naturalOrder());
        return out;
    }

    private static Random rng(UUID id, FavorietSoort soort) {
        return new Random(SALT ^ id.getMostSignificantBits() * 31 ^ id.getLeastSignificantBits() ^ (soort.ordinal() + 1L) * 0x9E3779B97F4A7C15L);
    }

    @Nullable
    public static String waarde(Mob guh, FavorietSoort soort) {
        return van(guh).get(soort);
    }

    public static boolean ontdekt(Mob guh, FavorietSoort soort) {
        BandData.Rec r = Band.rec(guh);
        return r != null && r.ontdekt.contains(soort);
    }

    /** Marks a favourite as discovered: true when it is new (saved; no effects: those are favorietjes'). */
    public static boolean ontdek(Mob guh, @Nullable ServerPlayer speler, FavorietSoort soort) {
        BandData.Rec r = Band.rec(guh);
        if (r == null || r.ontdekt.contains(soort)) {
            return false;
        }
        van(guh);
        r.ontdekt.add(soort);
        BandData.get(guh.getServer()).setDirty();
        return true;
    }

    public static Set<FavorietSoort> ontdekt(MinecraftServer s, UUID eigenaar, UUID bandId) {
        BandData.Rec r = BandData.get(s).vind(eigenaar, bandId);
        return r == null || r.ontdekt.isEmpty() ? EnumSet.noneOf(FavorietSoort.class) : EnumSet.copyOf(r.ontdekt);
    }

    /** A favourite's name: item/biome/knuffel/emote/song/toy/colour name, VRIEND = that guh's name. */
    public static Component naam(FavorietSoort soort, String waarde) {
        switch (soort) {
            case ETEN -> {
                ResourceLocation id = ResourceLocation.tryParse(waarde);
                Item item = id == null ? null : BuiltInRegistries.ITEM.getOptional(id).orElse(null);
                return item == null ? Component.literal(waarde) : Component.translatable(item.getDescriptionId());
            }
            case PLEK -> {
                ResourceLocation id = ResourceLocation.tryParse(waarde);
                return id == null ? Component.literal(waarde) : Component.translatable("biome." + id.getNamespace() + "." + id.getPath());
            }
            case KNUFFEL -> {
                return Component.translatable("block.guhs.knuffel_" + waarde);
            }
            case LIEDJE -> {
                if (waarde.startsWith("koortje:")) {
                    return Component.translatable("gui.guhs.knus.liedjesboek." + waarde.substring(8));
                }
                return Component.translatable("gui.guhs.disco.lied." + waarde.substring(waarde.indexOf(':') + 1));
            }
            case SPEELTJE -> {
                return Component.translatable("gui.guhs.speeltje." + waarde);
            }
            case EMOTE -> {
                return Component.translatable("emote.guhs." + waarde);
            }
            case KLEUR -> {
                return Component.translatable("gui.guhs.favoriet.kleur." + waarde);
            }
            case VRIEND -> {
                MinecraftServer s = Band.server();
                try {
                    BandData.Rec r = s == null ? null : BandData.get(s).vindOveral(UUID.fromString(waarde));
                    return Component.literal(r == null || r.naam.isEmpty() ? "Guh" : r.naam);
                } catch (IllegalArgumentException e) {
                    return Component.literal("Guh");
                }
            }
        }
        return Component.literal(waarde);
    }

    /** favorietjes fills the clothes -&gt; colour table (for the KLEUR favourite). */
    public static void kleur(GuhClothes stuk, String kleurId) {
        KLEUR_VAN.put(stuk, kleurId);
    }

    @Nullable
    public static String kleur(GuhClothes stuk) {
        return KLEUR_VAN.get(stuk);
    }
}
