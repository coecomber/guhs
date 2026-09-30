package nl.juiced.guhs.feature.wereldleven;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Knus tab of the wereldleven feature (section "wereldleven"): the knuffelkast (21 plushies), the liedjesboek
 * (6 songs), the ijsjes (7 flavours) and the milestones. Also grants the shown advancements (guhs:knuffeldal/wereldleven_*).
 */
public final class WereldlevenVoortgang {
    public static final String ONDERDEEL = "wereldleven";
    // counters
    public static final String IJSJES = "wereldleven.ijsjes", KOORTJES = "wereldleven.koortjes", GRIJPEN = "wereldleven.grijpen",
            KNUFFELS = "wereldleven.knuffels", MARSHMALLOWS = "wereldleven.marshmallows", GEZWAAID = "wereldleven.gezwaaid",
            IJSCOGUH = "wereldleven.ijscoguh", DUTJES = "wereldleven.dutjes";
    // collections
    public static final String KNUFFELKAST = "knuffelkast", LIEDJESBOEK = "liedjesboek", IJSJES_BOEK = "ijsjes";

    private static Supplier<ItemStack> stack(Supplier<? extends ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    static void register() {
        String o = ONDERDEEL;
        KnusVoortgang.mijlpaal(o, "wereldleven_ijscoguh", IJSCOGUH, 1, stack(() -> WereldlevenFeature.MARSHMALLOW_KNABBEL.get(), 4), "wereldleven_ijscoguh");
        KnusVoortgang.mijlpaal(o, "wereldleven_ijsjes", IJSJES, 7, stack(() -> WereldlevenFeature.KAASIJSJES.get(Kaasijsjes.Smaak.ROZE).get(), 3));
        KnusVoortgang.mijlpaal(o, "wereldleven_gezwaaid", GEZWAAID, 10, stack(ModItems.KAAS_KNABBELS, 12));
        KnusVoortgang.mijlpaal(o, "wereldleven_dutjes", DUTJES, 5, stack(() -> nl.juiced.guhs.feature.vadswoud.VadswoudFeature.GUHNESTJE.get(), 2));
        KnusVoortgang.mijlpaal(o, "wereldleven_marshmallows", MARSHMALLOWS, 5, stack(() -> WereldlevenFeature.MARSHMALLOW_KNABBEL.get(), 8));
        KnusVoortgang.mijlpaal(o, "wereldleven_koortjes", KOORTJES, 10, () -> new ItemStack(ModItems.clothingItem(GuhClothes.KOORSTRIKJE)));
        KnusVoortgang.mijlpaal(o, "wereldleven_grijpen", GRIJPEN, 10, stack(ModItems.KERMISBON, 5));
        KnusVoortgang.mijlpaal(o, "wereldleven_knuffels", KNUFFELS, 10, stack(ModItems.GEFRITUURDE_KAASKNABBELS, 6));
        KnusVoortgang.verzameling(o, KNUFFELKAST, WereldlevenFeature.KNUFFEL_IDS, id -> {
            var b = WereldlevenFeature.knuffel(id);
            return b == null ? new ItemStack(Items.PINK_WOOL) : new ItemStack(b);
        });
        KnusVoortgang.verzameling(o, LIEDJESBOEK, Koortje.Liedje.ids(), id -> new ItemStack(WereldlevenFeature.GUH_XYLOFOON.get()));
        List<String> smaken = new ArrayList<>();
        for (Kaasijsjes.Smaak s : Kaasijsjes.Smaak.values()) {
            smaken.add(s.id());
        }
        KnusVoortgang.verzameling(o, IJSJES_BOEK, smaken, id -> {
            Kaasijsjes.Smaak s = Kaasijsjes.Smaak.byId(id);
            return s == null ? new ItemStack(Items.SNOWBALL) : new ItemStack(WereldlevenFeature.KAASIJSJES.get(s).get());
        });
    }

    /** Grants a shown advancement of the Knuffeldal tab (guhs:knuffeldal/&lt;name&gt;) and the hidden quest one of the same name. */
    public static void toon(ServerPlayer player, String name) {
        GuhAdvancements.grant(player, name);
        AdvancementHolder holder = player.level().getServer().getAdvancements().get(Guhs.id("knuffeldal/" + name));
        if (holder == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(holder);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(holder, criterion);
        }
    }

    private WereldlevenVoortgang() {
    }
}
