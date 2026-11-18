package nl.juiced.guhs.feature.guhwinter.verhaal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.GuhwinterData;

/**
 * Guhwinter (verhaal slice): the prikbord of goede voornemens (chapter 3). After the six told theirs, you write your own on
 * a card (one per player, at most {@link #MAX} characters, cleaned by {@link #schoon}); it is pinned to the board by the
 * house for everyone to read (right-click the board: client.PrikbordScherm). You may rewrite your card during Oud &amp;
 * Nieuw. Ops remove a card with {@code /guhs winter verhaal kaartje wis <speler>}. Stored in kern's
 * {@code GuhwinterData.sectie(server, "verhaal")} under "kaartjes" (uuid -> {Naam, Tekst, Tijd}).
 */
public final class Voornemens {
    /** The longest card (characters). */
    public static final int MAX = 80;
    private static final String KAARTJES = "kaartjes";

    private Voornemens() {
    }

    static void register() {
    }

    /** The cards (live: change, then GuhwinterData.opslaan). */
    static CompoundTag kaartjes(MinecraftServer server) {
        CompoundTag s = GuhwinterData.sectie(server, "verhaal");
        if (!(s.get(KAARTJES) instanceof CompoundTag)) {
            s.put(KAARTJES, new CompoundTag());
        }
        return (CompoundTag) s.get(KAARTJES);
    }

    /**
     * A card's text as it may hang: no colour codes or control characters, no runs of spaces, trimmed, at most {@link #MAX}
     * characters (code points). Empty when nothing is left.
     */
    public static String schoon(@Nullable String in) {
        if (in == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        boolean spatie = false;
        int n = 0;
        for (int i = 0; i < in.length() && n < MAX; ) {
            int c = in.codePointAt(i);
            i += Character.charCount(c);
            if (c == '§') {
                if (i < in.length()) {
                    i += Character.charCount(in.codePointAt(i));   // (a colour code: the code letter goes too)
                }
                continue;
            }
            if (Character.isISOControl(c) && !Character.isWhitespace(c) || Character.getType(c) == Character.FORMAT || Character.getType(c) == Character.PRIVATE_USE) {
                continue;
            }
            if (Character.isWhitespace(c)) {
                spatie = b.length() > 0;
                continue;
            }
            if (spatie) {
                b.append(' ');
                n++;
                spatie = false;
                if (n >= MAX) {
                    break;
                }
            }
            b.appendCodePoint(c);
            n++;
        }
        return b.toString().strip();
    }

    /** May this player write (or rewrite) a card now: Oud &amp; Nieuw, and they heard the six's resolutions. */
    static boolean mag(ServerPlayer p) {
        return WinterVerhaal.open(p, WinterVerhaal.Hoofdstuk.OUDJAAR)
                && (WinterVerhaal.OUDJAAR.stap(p) > WinterVerhaal.O_VOORNEMENS
                || WinterVerhaal.OUDJAAR.stap(p) == WinterVerhaal.O_VOORNEMENS && WinterVerhaal.OUDJAAR.vlag(p, "gehoord"));
    }

    /** Right-click on the prikbord: every card. */
    static void openBord(ServerPlayer p) {
        stuur(p, false);
    }

    /** Straight to the writing card (after the six's talk). */
    static void openSchrijven(ServerPlayer p) {
        stuur(p, mag(p));
    }

    private static void stuur(ServerPlayer p, boolean schrijf) {
        MinecraftServer server = p.level().getServer();
        CompoundTag data = new CompoundTag();
        ListTag lijst = new ListTag();
        for (CompoundTag k : lijst(server)) {
            lijst.add(k.copy());
        }
        data.put("Kaartjes", lijst);
        data.putBoolean("Mag", mag(p));
        data.putBoolean("Schrijf", schrijf);
        data.putString("Eigen", kaartjes(server).getCompoundOrEmpty(p.getUUID().toString()).getStringOr("Tekst", ""));
        data.putInt("Max", MAX);
        PacketDistributor.sendToPlayer(p, new WinterVerhaalPayloads.Prikbord(data));
    }

    /** The cards, oldest first. */
    static List<CompoundTag> lijst(MinecraftServer server) {
        CompoundTag alle = kaartjes(server);
        List<CompoundTag> uit = new ArrayList<>();
        for (String k : alle.keySet()) {
            if (alle.get(k) instanceof CompoundTag t) {
                uit.add(t);
            }
        }
        uit.sort(Comparator.comparingLong(t -> t.getLongOr("Tijd", 0L)));
        return uit;
    }

    /** The card came back from the client. */
    static void opslaan(ServerPlayer p, String tekst) {
        if (!mag(p)) {
            p.sendSystemMessage(Component.translatable("guhwinter.verhaal.kaartje.niet").withStyle(ChatFormatting.GRAY));
            return;
        }
        String schoon = schoon(tekst);
        if (schoon.isEmpty()) {
            p.sendSystemMessage(Component.translatable("guhwinter.verhaal.kaartje.leeg").withStyle(ChatFormatting.GRAY));
            return;
        }
        MinecraftServer server = p.level().getServer();
        CompoundTag alle = kaartjes(server);
        String id = p.getUUID().toString();
        CompoundTag k = alle.getCompoundOrEmpty(id).copy();
        boolean nieuw = !alle.contains(id);
        k.putString("Naam", p.getGameProfile().name());
        k.putString("Tekst", schoon);
        if (nieuw) {
            k.putLong("Tijd", Guhwinter.nuMs(server));
        }
        alle.put(id, k);
        GuhwinterData.opslaan(server);
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 1.2f);
        p.sendSystemMessage(Component.translatable(nieuw ? "guhwinter.verhaal.kaartje.hangt" : "guhwinter.verhaal.kaartje.nieuw").withStyle(ChatFormatting.LIGHT_PURPLE));
        WinterVerhaal.OUDJAAR.verder(p, WinterVerhaal.O_VOORNEMENS);
        stuur(p, false);
    }

    /** (ops) removes the card of the player with this name; true: there was one. */
    static boolean wis(MinecraftServer server, String naam) {
        CompoundTag alle = kaartjes(server);
        for (String k : List.copyOf(alle.keySet())) {
            if (alle.get(k) instanceof CompoundTag t && t.getStringOr("Naam", "").toLowerCase(Locale.ROOT).equals(naam.toLowerCase(Locale.ROOT))) {
                alle.remove(k);
                GuhwinterData.opslaan(server);
                return true;
            }
        }
        return false;
    }
}
