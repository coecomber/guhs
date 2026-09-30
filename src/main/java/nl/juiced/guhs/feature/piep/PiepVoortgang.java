package nl.juiced.guhs.feature.piep;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.feature.knus.Knus;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * Piep's Guhdex section (Knus tab, section {@value #SECTIE}, after the 2.8 ones): counters and milestones for the muisjes
 * (geaaid, getamed, gevonden bij verstoppertje), Poepschilly's poetsbeurten, the nest, and the collection "piepboek" (one
 * page per creature, with its story). Per-player Piep data (the recipe) lives in {@link GuhQuests#saved} compound {@value #KEY}.
 */
public final class PiepVoortgang {
    public static final String SECTIE = "piep";
    public static final String KEY = "guhs_piep";
    public static final String PIEPBOEK = "piepboek";
    /** The pages of the piepboek, in order. */
    public static final List<String> PAGINAS = List.of("pieppiepmuisje", "poepschilly", "schilly", "roze_guh_koek", "boze_kaasknabbel", "boze_oppernabbel");

    // counters (KnusVoortgang.tel)
    public static final String GEAAID = "piep.muisje_geaaid", GETAMED = "piep.muisje_getamed", GEVONDEN = "piep.muisje_gevonden",
            POETSBEURTEN = "piep.poetsbeurten", KNABBELS = "piep.knabbels_verslagen", NEST = "piep.nest_gewonnen",
            KOEKJES = "piep.koekjes", BESTIES = "piep.bestie_momenten", BEEF = "piep.beef_bijgelegd";

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        KnusVoortgang.onderdeel(SECTIE, Knus.icoon("roze_guh_koek", Items.CAKE));
        String s = SECTIE;
        KnusVoortgang.mijlpaal(s, "piep_aaien", GEAAID, 10, stack(ModItems.KAAS_KNABBELS, 8), "piep_muisje_geaaid_10");
        KnusVoortgang.mijlpaal(s, "piep_temmen", GETAMED, 1, stack(ModItems.KAAS_KNABBELS, 8), "piep_muisje_getamed");
        KnusVoortgang.mijlpaal(s, "piep_muizenfamilie", GETAMED, 5, stack(PiepFeature.ROZE_GUH_KOEK_ITEM, 1));
        KnusVoortgang.mijlpaal(s, "piep_verstoppertje", GEVONDEN, 3, stack(ModItems.KAAS_KNABBELS, 16), "piep_verstoppertje_3");
        KnusVoortgang.mijlpaal(s, "piep_verstopkampioen", GEVONDEN, 15, stack(PiepFeature.ROZE_GUH_KOEK_ITEM, 2));
        KnusVoortgang.mijlpaal(s, "piep_poetsen", POETSBEURTEN, 1, stack(ModItems.KAAS_KNABBELS, 8), "piep_poetsbeurt");
        KnusVoortgang.mijlpaal(s, "piep_poetsmeester", POETSBEURTEN, 10, stack(PiepFeature.ROZE_GUH_KOEK_ITEM, 2));
        KnusVoortgang.mijlpaal(s, "piep_besties", BESTIES, 5, stack(ModItems.KAAS_KNABBELS, 12));
        KnusVoortgang.mijlpaal(s, "piep_beef", BEEF, 3, stack(PiepFeature.ROZE_GUH_KOEK_ITEM, 1));
        KnusVoortgang.mijlpaal(s, "piep_knabbels", KNABBELS, 20, stack(ModItems.KAAS_KNABBELS, 16));
        KnusVoortgang.mijlpaal(s, "piep_nest", NEST, 1, stack(ModItems.KAAS_KNABBELS, 32), "piep_nest_gewonnen");
        KnusVoortgang.mijlpaal(s, "piep_koekjes", KOEKJES, 5, stack(ModItems.KAAS_KNABBELS, 16));
        KnusVoortgang.verzameling(s, PIEPBOEK, PAGINAS, id -> switch (id) {
            case "pieppiepmuisje" -> new ItemStack(PiepFeature.PIEPPIEPMUISJE_SPAWN_EGG.get());
            case "poepschilly" -> new ItemStack(PiepFeature.POEPSCHILLY_SPAWN_EGG.get());
            case "schilly" -> new ItemStack(PiepFeature.SCHILLY_SPAWN_EGG.get());
            case "roze_guh_koek" -> new ItemStack(PiepFeature.ROZE_GUH_KOEK_ITEM.get());
            case "boze_kaasknabbel" -> new ItemStack(PiepFeature.BOZE_KAASKNABBEL_SPAWN_EGG.get());
            default -> new ItemStack(PiepFeature.BOZE_OPPERNABBEL_SPAWN_EGG.get());
        });
    }

    /** The player's own Piep data (a live compound in their saved data). */
    public static CompoundTag data(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains(KEY, Tag.TAG_COMPOUND)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompound(KEY);
    }

    /** Counts one (and grants a hidden quest advancement guhs:quest/&lt;adv&gt;, if given). */
    public static void tel(ServerPlayer player, String teller, int erbij, String... adv) {
        KnusVoortgang.tel(player, teller, erbij);
        for (String a : adv) {
            GuhAdvancements.grant(player, a);
        }
    }

    /** Fills in a page of the piepboek (the first meeting). */
    public static void pagina(ServerPlayer player, String id) {
        KnusVoortgang.ontdek(player, PIEPBOEK, id);
    }

    private PiepVoortgang() {
    }
}
