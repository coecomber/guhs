package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.taal.Tekst;

/**
 * The Guhpixel shop: a registry of offers ({@link #aanbod}, registered by the slices from their register) and the server
 * side of the shop screen (client.WinkelScherm; the Verkoper-guh's role calls {@link #open}). Everything is paid with
 * {@link Muntjes}. Bought things are real items ({@link #geef}) or outfit unlocks ({@link #ontgrendel}, {@code max(1)}).
 * A buy is validated here: the offer exists, the player is in guhpixel and not in a game, the maximum, the requirement,
 * the price (asked again on the server), and the delivery (false = nothing is charged).
 */
public final class Winkel {
    /** The groups in screen order (titles gui.guhs.guhpixel.winkel.groep.&lt;groep&gt;). */
    public static final List<String> GROEPEN = List.of("guhkade", "guhkantoor", "guhbioscoop", "among_pakjes", "among_hoedjes", "among_deco");

    /** One offer. */
    public record Aanbod(String id, String groep, Supplier<ItemStack> icoon, Component naam, Component uitleg,
                         ToIntBiFunction<ServerPlayer, Integer> prijs, int max, Predicate<ServerPlayer> eis, @Nullable Component eisTekst,
                         BiPredicate<ServerPlayer, Aanbod> lever) {
    }

    private static final Map<String, Aanbod> AANBOD = new LinkedHashMap<>();
    private static final String GEKOCHT = "Gekocht";

    public enum Uitkomst { OK, ONBEKEND, NIET_HIER, MAX, EIS, TE_DUUR, MISLUKT }

    /** Starts an offer: {@code Winkel.aanbod("id").groep(..).icoon(..).naam(..).uitleg(..).prijs(..).lever(..).registreer()}. */
    public static Bouwer aanbod(String id) {
        return new Bouwer(id);
    }

    public static final class Bouwer {
        private final String id;
        private String groep = GROEPEN.get(0);
        private Supplier<ItemStack> icoon = () -> new ItemStack(Items.PAPER);
        private Component naam, uitleg = Component.empty();
        private ToIntBiFunction<ServerPlayer, Integer> prijs = (p, n) -> 0;
        private int max;
        private Predicate<ServerPlayer> eis = p -> true;
        private Component eisTekst;
        private BiPredicate<ServerPlayer, Aanbod> lever = (p, a) -> false;

        private Bouwer(String id) {
            this.id = id;
            this.naam = Component.literal(id);
        }

        public Bouwer groep(String groep) {
            this.groep = groep;
            return this;
        }

        public Bouwer icoon(Supplier<ItemStack> icoon) {
            this.icoon = icoon;
            return this;
        }

        public Bouwer naam(Component naam) {
            this.naam = naam;
            return this;
        }

        public Bouwer uitleg(Component uitleg) {
            this.uitleg = uitleg;
            return this;
        }

        /** (player, how many they already bought) -> price. */
        public Bouwer prijs(ToIntBiFunction<ServerPlayer, Integer> prijs) {
            this.prijs = prijs;
            return this;
        }

        public Bouwer prijs(int vast) {
            this.prijs = (p, n) -> vast;
            return this;
        }

        /** 0 = unlimited; 1 = once (outfits). */
        public Bouwer max(int max) {
            this.max = max;
            return this;
        }

        /** A requirement, and the text shown while it is not met. */
        public Bouwer eis(Predicate<ServerPlayer> eis, Component tekst) {
            this.eis = eis;
            this.eisTekst = tekst;
            return this;
        }

        /** Delivers the thing; false = nothing is charged. */
        public Bouwer lever(BiPredicate<ServerPlayer, Aanbod> lever) {
            this.lever = lever;
            return this;
        }

        public Aanbod registreer() {
            Aanbod a = new Aanbod(id, groep, icoon, naam, uitleg, prijs, max, eis, eisTekst, lever);
            AANBOD.put(id, a);
            return a;
        }
    }

    @Nullable
    public static Aanbod van(String id) {
        return AANBOD.get(id);
    }

    /** Every offer, by group in screen order (unknown groups last), then in order of registration. */
    public static List<Aanbod> alle() {
        List<Aanbod> out = new ArrayList<>();
        for (String g : GROEPEN) {
            AANBOD.values().stream().filter(a -> a.groep().equals(g)).forEach(out::add);
        }
        AANBOD.values().stream().filter(a -> !GROEPEN.contains(a.groep())).forEach(out::add);
        return out;
    }

    /** How many of this offer the player bought. */
    public static int gekocht(ServerPlayer p, String id) {
        return Muntjes.data(p).getCompoundOrEmpty(GEKOCHT).getIntOr(id, 0);
    }

    public static int prijs(ServerPlayer p, Aanbod a) {
        return Math.max(0, a.prijs().applyAsInt(p, gekocht(p, a.id())));
    }

    /** A bought item: into the pockets, never lost. Always true. */
    public static boolean geef(ServerPlayer p, ItemStack s) {
        Minigames.give(p, s);
        return true;
    }

    /** A bought outfit; false when the player already had it. */
    public static boolean ontgrendel(ServerPlayer p, GuhClothes c) {
        return KledingUnlocks.ontgrendel(p, c);
    }

    /** Why the player cannot buy this now (OK: they can). */
    public static Uitkomst kan(ServerPlayer p, @Nullable Aanbod a) {
        if (a == null) {
            return Uitkomst.ONBEKEND;
        }
        if (!Toegang.heeft(p) || !Guhpixel.in(p) || Sessies.van(p) != null) {
            return Uitkomst.NIET_HIER;
        }
        if (a.max() > 0 && gekocht(p, a.id()) >= a.max()) {
            return Uitkomst.MAX;
        }
        if (!a.eis().test(p)) {
            return Uitkomst.EIS;
        }
        if (Muntjes.saldo(p) < prijs(p, a)) {
            return Uitkomst.TE_DUUR;
        }
        return Uitkomst.OK;
    }

    /** The player buys an offer (server side, everything checked). */
    public static Uitkomst koop(ServerPlayer p, String id) {
        Aanbod a = AANBOD.get(id);
        Uitkomst u = kan(p, a);
        if (u != Uitkomst.OK) {
            return u;
        }
        int prijs = prijs(p, a);
        if (!a.lever().test(p, a)) {
            return Uitkomst.MISLUKT;
        }
        Muntjes.betaal(p, prijs);
        CompoundTag gekocht = PxData.sub(Muntjes.data(p), GEKOCHT);
        gekocht.putInt(id, gekocht.getIntOr(id, 0) + 1);
        PxData.vuil(p.level().getServer());
        PxGeluid.speel(p, GuhpixelFeature.MUNTJE.get(), SoundSource.PLAYERS, 0.8f, 0.8f);
        return Uitkomst.OK;
    }

    /** Opens the shop screen (or refreshes it). */
    public static void open(ServerPlayer p) {
        ModNetworking.sendTo(p, new GuhpixelPayloads.WinkelOpen(stand(p, null)));
    }

    /** What the screen shows: the saldo and every offer with its price for this player. */
    static CompoundTag stand(ServerPlayer p, @Nullable Component melding) {
        CompoundTag t = new CompoundTag();
        t.putInt("Saldo", Muntjes.saldo(p));
        if (melding != null) {
            Tekst.put(t, "Melding", melding);
        }
        ListTag lijst = new ListTag();
        for (Aanbod a : alle()) {
            CompoundTag e = new CompoundTag();
            e.putString("Id", a.id());
            e.putString("Groep", a.groep());
            Tekst.put(e, "Naam", a.naam());
            Tekst.put(e, "Uitleg", a.uitleg());
            e.put("Icoon", Nbt.saveStack(p.registryAccess(), a.icoon().get()));
            e.putInt("Prijs", prijs(p, a));
            e.putInt("Gekocht", gekocht(p, a.id()));
            e.putInt("Max", a.max());
            Uitkomst u = kan(p, a);
            e.putInt("Kan", u.ordinal());
            if (u == Uitkomst.EIS && a.eisTekst() != null) {
                Tekst.put(e, "Eis", a.eisTekst());
            }
            lijst.add(e);
        }
        t.put("Aanbod", lijst);
        return t;
    }

    /** The screen's buy button. */
    static void opKoop(ServerPlayer p, String id) {
        Uitkomst u = koop(p, id);
        Aanbod a = AANBOD.get(id);
        Component melding = switch (u) {
            case OK -> Component.translatable("gui.guhs.guhpixel.winkel.gekocht", a.naam()).withStyle(ChatFormatting.GREEN);
            case EIS -> a != null && a.eisTekst() != null ? a.eisTekst().copy().withStyle(ChatFormatting.GOLD)
                    : Component.translatable("gui.guhs.guhpixel.winkel.nee.eis").withStyle(ChatFormatting.GOLD);
            default -> Component.translatable("gui.guhs.guhpixel.winkel.nee." + u.name().toLowerCase(java.util.Locale.ROOT)).withStyle(ChatFormatting.GOLD);
        };
        ModNetworking.sendTo(p, new GuhpixelPayloads.WinkelOpen(stand(p, melding)));
    }

    /** (Tests) removes an offer. */
    static void vergeet(String id) {
        AANBOD.remove(id);
    }

    private Winkel() {
    }
}
