package nl.juiced.guhs.feature.safari.verblijf;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.safari.kern.SafariMunten;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * Op Safari! step 2 (S5): what the gift shop (slice S2) needs to sell verblijfsborden (design 1.8/1.9): a board can be bought
 * once the species is photographed (in the Safari-dex), for its price class (gewoon 100, middel 175, zeldzaam 300
 * Safarimunten), and then not again for 7 real days ({@link SafariSpeler#BORD_COOLDOWN}). The gouden dikdik is the full-dex
 * reward: never for sale ({@link #geef} gives it).
 * <pre>
 *   for (String soort : VerblijfWinkel.teKoop()) {
 *       int prijs = VerblijfWinkel.prijs(soort);
 *       VerblijfWinkel.Reden r = VerblijfWinkel.kanKopen(speler, soort);   // OK, or why not (r.melding(...))
 *       ... button: VerblijfWinkel.koop(speler, soort) -> true when bought (the board is in the inventory)
 *   }
 * </pre>
 */
public final class VerblijfWinkel {
    public enum Reden {
        OK, GEEN_BORD, GEEN_FOTO, COOLDOWN, MUNTEN;

        /** What to tell the player when this is not OK. */
        public Component melding(ServerPlayer p, String soort) {
            Component naam = VerblijfsbordItem.naam(soort);
            return switch (this) {
                case OK -> Component.empty();
                case GEEN_BORD -> Component.translatable("gui.guhs.verblijf.koop.geen_bord");
                case GEEN_FOTO -> Component.translatable("gui.guhs.verblijf.koop.geen_foto", naam);
                case COOLDOWN -> Component.translatable("gui.guhs.verblijf.koop.cooldown", naam, tijd(SafariSpeler.bordVrijOp(p.level().getServer(), p.getUUID(), soort) - Klok.nu()));
                case MUNTEN -> Component.translatable("gui.guhs.verblijf.koop.munten", prijs(soort));
            };
        }
    }

    /** Every species whose board the shop sells, in park order (not the gouden dikdik). */
    public static List<String> teKoop() {
        List<String> uit = new ArrayList<>();
        VerblijfData.alle().values().stream().filter(d -> !d.beloning()).forEach(d -> uit.add(d.soort()));
        return uit;
    }

    public static int prijs(String soort) {
        VerblijfData.Dier d = VerblijfData.dier(soort);
        return d == null ? 0 : d.prijs();
    }

    /** "gewoon", "middel", "zeldzaam" (or "beloning"). */
    public static String klasse(String soort) {
        VerblijfData.Dier d = VerblijfData.dier(soort);
        return d == null ? "" : d.klasse();
    }

    public static Reden kanKopen(ServerPlayer p, String soort) {
        VerblijfData.Dier d = VerblijfData.dier(soort);
        if (d == null || d.beloning()) {
            return Reden.GEEN_BORD;
        }
        if (SafariSpeler.dex(p, soort).sterren() <= 0) {
            return Reden.GEEN_FOTO;
        }
        if (!SafariSpeler.kanBordKopen(p.level().getServer(), p.getUUID(), soort)) {
            return Reden.COOLDOWN;
        }
        if (!SafariMunten.kan(p, d.prijs())) {
            return Reden.MUNTEN;
        }
        return Reden.OK;
    }

    /** Buys the board (pays, starts the cooldown, gives the item); false (and a message) when it can't. */
    public static boolean koop(ServerPlayer p, String soort) {
        Reden r = kanKopen(p, soort);
        if (r != Reden.OK) {
            p.sendOverlayMessage(r.melding(p, soort).copy().withStyle(ChatFormatting.RED));
            return false;
        }
        if (!SafariMunten.neem(p, prijs(soort))) {
            return false;
        }
        SafariSpeler.bordGekocht(p.level().getServer(), p.getUUID(), soort);
        geef(p, soort);
        p.sendSystemMessage(Component.translatable("gui.guhs.verblijf.koop.gekocht", VerblijfsbordItem.naam(soort)).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Puts a board of this species in the player's inventory (or at their feet): the shop, rewards (the gouden dikdik). */
    public static void geef(ServerPlayer p, String soort) {
        ItemStack bord = VerblijfsbordItem.bord(soort);
        if (!p.getInventory().add(bord)) {
            p.drop(bord, false);
        }
    }

    /** "3 dagen" / "5 uur" for a time in ms. */
    public static Component tijd(long ms) {
        long uur = Math.max(1, (ms + Klok.UUR - 1) / Klok.UUR);
        return uur >= 24 ? Component.translatable("gui.guhs.verblijf.info.dagen", (uur + 23) / 24)
                : Component.translatable("gui.guhs.verblijf.info.uren", uur);
    }

    private VerblijfWinkel() {
    }
}
