package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.registry.ModItems;

/**
 * What goes back to the rest of the mod (design 9: "alle vier": clothes, trophies + deco, titles; the Guhdex pages are another
 * slice's). Cosmetic ONLY: no power ever leaves the Vadsrijk.
 * <ul>
 *   <li>Guh clothes ({@link #KLEDING}, the existing GuhClothes system on existing bones): bought in the cosmetica shop, they arrive
 *       as the normal clothing items in the {@link Post} at home;</li>
 *   <li>trophy/deco blocks ({@link EcoSpul#DECO}): the same way, by post;</li>
 *   <li>titles ({@link #TITELS}): a flag in {@code SpelerData.extra("economie")}, shown in the normal world by the titles feature.
 *       Two are bought, three are earned (Sterrensmid: a +10 piece; Rijdiermeester: a mount at lv 10; Marktkoopman: 10 market sales).</li>
 * </ul>
 * Never through the inventory swap itself: post is kept in the player attachment and handed out outside the dimension.
 */
public final class Cosmetica {
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.VR_ITEM_RIDDERHELMPJE, GuhClothes.VR_ITEM_ZWAMMENHOEDJE,
            GuhClothes.VR_ITEM_KLODDERKROONTJE, GuhClothes.VR_ITEM_RIDDERCAPE, GuhClothes.VR_ITEM_VADSRIJKSJAAL);

    public static final String RIDDER = "vr_ridder_van_zwamzoet", BARON = "vr_vadsmuntenbaron", STERRENSMID = "vr_sterrensmid",
            RIJDIERMEESTER = "vr_rijdiermeester", MARKTKOOPMAN = "vr_marktkoopman";
    public static final List<String> TITELS = List.of(RIDDER, BARON, STERRENSMID, RIJDIERMEESTER, MARKTKOOPMAN);
    /** Market sales for Marktkoopman. */
    public static final int VERKOCHT_VOOR_TITEL = 10;

    private Cosmetica() {
    }

    static void registreer() {
        Titels.registreer(new Titels.Titel(RIDDER, "gui.guhs.titels.naam." + RIDDER, ChatFormatting.LIGHT_PURPLE, "guhs:vr_item_ridderhelmpje",
                p -> heeftTitel(p, RIDDER)));
        Titels.registreer(new Titels.Titel(BARON, "gui.guhs.titels.naam." + BARON, ChatFormatting.GOLD, "guhs:vr_item_munt_goud", p -> heeftTitel(p, BARON)));
        Titels.registreer(new Titels.Titel(STERRENSMID, "gui.guhs.titels.naam." + STERRENSMID, ChatFormatting.YELLOW, "guhs:vr_item_verbetersteentje",
                p -> heeftTitel(p, STERRENSMID)));
        Titels.registreer(new Titels.Titel(RIJDIERMEESTER, "gui.guhs.titels.naam." + RIJDIERMEESTER, ChatFormatting.AQUA, "guhs:vr_item_rijfluitje",
                p -> heeftTitel(p, RIJDIERMEESTER)));
        Titels.registreer(new Titels.Titel(MARKTKOOPMAN, "gui.guhs.titels.naam." + MARKTKOOPMAN, ChatFormatting.GREEN, "guhs:vr_item_munt_zilver",
                p -> heeftTitel(p, MARKTKOOPMAN)));
        for (GuhClothes c : KLEDING) {
            KledingBronnen.bron(c, "vadsrijk", Component.translatable("vadsrijk.economie.kledingprijs"));
        }
    }

    // ---- titles -----------------------------------------------------------------------------------------------------------

    public static boolean heeftTitel(ServerPlayer p, String id) {
        ListTag l = Spelers.data(p).extra("economie").getListOrEmpty("titels");
        for (int i = 0; i < l.size(); i++) {
            if (l.getString(i).orElse("").equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Gives a title (true when new). The titles feature announces it on its next check. */
    public static boolean geefTitel(ServerPlayer p, String id) {
        if (heeftTitel(p, id)) {
            return false;
        }
        CompoundTag e = Spelers.data(p).extra("economie");
        ListTag l = e.getListOrEmpty("titels").copy();
        l.add(StringTag.valueOf(id));
        e.put("titels", l);
        EcoUtil.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        EcoUtil.chat(p, "vadsrijk.economie.titel.nieuw", Component.translatable("gui.guhs.titels.naam." + id).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** The shop item for a clothing piece. */
    public static ItemStack kledingItem(GuhClothes c) {
        return new ItemStack(ModItems.clothingItem(c));
    }

    /** True while the player is outside the Vadsrijk with their normal inventory live (post may be handed out). */
    static boolean thuis(ServerPlayer p) {
        return !Vadsrijk.in(p) && !Spelers.data(p).binnen;
    }
}
