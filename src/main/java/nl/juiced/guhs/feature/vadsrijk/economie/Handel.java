package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Direct trade between two players in the Vadsrijk (design 9), with a two-sided confirm: both sides put items/munten in, both press
 * "Klaar", then both press "Ruilen!". Any change to an offer resets both. Start: sneak + right-click another player (empty hand), or
 * {@code /guhs vadsrijk eco handel <speler>}; the other accepts with the chat link.
 * <p>Exploit rules: NOTHING moves until the very end. An offer only points at bag slots plus a copy of what was in them; at the
 * final confirm ({@link #voerUit}) both sides are re-validated in one server-thread call (online, both in the Vadsrijk, close,
 * every slot still holds exactly the offered stack, not gebonden, enough munten, enough empty slots on both sides) and only then
 * are all items and munten swapped together. Any failure: nothing happens and both must confirm again.
 */
public final class Handel {
    public static final int MAX_ITEMS = 9;
    public static final double AFSTAND = 12;
    /** Ticks a request stays valid. */
    public static final long VERZOEK_TICKS = 20 * 60;

    public static final class Kant {
        final UUID speler;
        final List<Integer> slots = new ArrayList<>();
        final List<ItemStack> kopie = new ArrayList<>();
        long munten;
        boolean klaar, bevestigd;

        Kant(UUID speler) {
            this.speler = speler;
        }
    }

    public static final class Sessie {
        final Kant a, b;

        Sessie(UUID a, UUID b) {
            this.a = new Kant(a);
            this.b = new Kant(b);
        }

        Kant van(UUID id) {
            return a.speler.equals(id) ? a : b;
        }

        Kant ander(UUID id) {
            return a.speler.equals(id) ? b : a;
        }

        void reset() {
            a.klaar = b.klaar = a.bevestigd = b.bevestigd = false;
        }
    }

    private record Verzoek(UUID van, long tijd) {
    }

    private static final Map<UUID, Sessie> SESSIES = new ConcurrentHashMap<>();
    /** Requests by the asked player's id. */
    private static final Map<UUID, Verzoek> VERZOEKEN = new ConcurrentHashMap<>();

    private Handel() {
    }

    @Nullable
    public static Sessie sessie(ServerPlayer p) {
        return SESSIES.get(p.getUUID());
    }

    // ---- requests -------------------------------------------------------------------------------------------------------------

    public static void vraag(ServerPlayer p, ServerPlayer doel) {
        if (p == doel || !Vadsrijk.in(p) || !Vadsrijk.in(doel)) {
            EcoUtil.zeg(p, "vadsrijk.economie.handel.alleen_rijk");
            return;
        }
        if (SESSIES.containsKey(p.getUUID()) || SESSIES.containsKey(doel.getUUID())) {
            EcoUtil.zeg(p, "vadsrijk.economie.handel.bezig");
            return;
        }
        // asked back by the one you are asking: start right away
        Verzoek terug = VERZOEKEN.get(p.getUUID());
        if (terug != null && terug.van().equals(doel.getUUID()) && p.level().getGameTime() - terug.tijd() < VERZOEK_TICKS) {
            accepteer(p);
            return;
        }
        VERZOEKEN.put(doel.getUUID(), new Verzoek(p.getUUID(), p.level().getGameTime()));
        EcoUtil.zeg(p, "vadsrijk.economie.handel.gevraagd", doel.getGameProfile().name());
        doel.sendSystemMessage(Component.translatable("vadsrijk.economie.handel.vraag", p.getGameProfile().name()).withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(" ").append(Component.translatable("vadsrijk.economie.handel.ja").withStyle(s -> s.withColor(ChatFormatting.GREEN)
                        .withBold(true).withClickEvent(new ClickEvent.RunCommand("/guhs vadsrijk eco handel accepteer")))));
        EcoUtil.geluid(doel, SoundEvents.NOTE_BLOCK_BELL.value(), 0.8f, 1.6f);
    }

    public static void accepteer(ServerPlayer p) {
        Verzoek v = VERZOEKEN.remove(p.getUUID());
        MinecraftServer server = p.level().getServer();
        ServerPlayer van = v == null ? null : server.getPlayerList().getPlayer(v.van());
        if (v == null || van == null || p.level().getGameTime() - v.tijd() > VERZOEK_TICKS) {
            EcoUtil.zeg(p, "vadsrijk.economie.handel.geen_verzoek");
            return;
        }
        if (!dichtbij(p, van)) {
            EcoUtil.zeg(p, "vadsrijk.economie.handel.te_ver");
            return;
        }
        if (SESSIES.containsKey(p.getUUID()) || SESSIES.containsKey(van.getUUID())) {
            EcoUtil.zeg(p, "vadsrijk.economie.handel.bezig");
            return;
        }
        start(van, p);
    }

    /** Starts a session between two players (no checks; the gametests use it directly). */
    public static Sessie start(ServerPlayer a, ServerPlayer b) {
        Sessie s = new Sessie(a.getUUID(), b.getUUID());
        SESSIES.put(a.getUUID(), s);
        SESSIES.put(b.getUUID(), s);
        stuur(a);
        stuur(b);
        return s;
    }

    /** Gametests only: mock players are not in the dimension. */
    static boolean alleenInRijk = true;

    static boolean dichtbij(ServerPlayer a, ServerPlayer b) {
        return (!alleenInRijk || Vadsrijk.in(a) && Vadsrijk.in(b)) && a.level() == b.level() && a.distanceToSqr(b) <= AFSTAND * AFSTAND;
    }

    // ---- changing an offer --------------------------------------------------------------------------------------------------

    /** Adds bag slot {@code slot} to p's offer. Returns "" or a message key. */
    public static String leg(ServerPlayer p, int slot) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null || slot < 0 || slot >= EcoUtil.TAS) {
            return "";
        }
        Kant k = s.van(p.getUUID());
        ItemStack st = p.getInventory().getItem(slot);
        if (st.isEmpty() || k.slots.contains(slot) || k.slots.size() >= MAX_ITEMS) {
            return "";
        }
        if (!EcoUtil.verhandelbaar(st)) {
            return "vadsrijk.economie.handel.gebonden";
        }
        k.slots.add(slot);
        k.kopie.add(st.copy());
        s.reset();
        return "";
    }

    public static void pak(ServerPlayer p, int index) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null) {
            return;
        }
        Kant k = s.van(p.getUUID());
        if (index >= 0 && index < k.slots.size()) {
            k.slots.remove(index);
            k.kopie.remove(index);
            s.reset();
        }
    }

    public static void munten(ServerPlayer p, long n) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null) {
            return;
        }
        Kant k = s.van(p.getUUID());
        long nieuw = Math.max(0, Math.min(n, Spelers.data(p).munten));
        if (nieuw != k.munten) {
            k.munten = nieuw;
            s.reset();
        }
    }

    public static void klaar(ServerPlayer p) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s != null) {
            Kant k = s.van(p.getUUID());
            k.klaar = !k.klaar;
            if (!k.klaar) {
                s.a.bevestigd = s.b.bevestigd = false;
            }
        }
    }

    /** The final confirm: when both confirmed, the swap happens (re-validated). Returns "" or a message key. */
    public static String bevestig(ServerPlayer p) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null || !s.a.klaar || !s.b.klaar) {
            return "vadsrijk.economie.handel.eerst_klaar";
        }
        s.van(p.getUUID()).bevestigd = true;
        if (s.a.bevestigd && s.b.bevestigd) {
            return voerUit(p.level().getServer(), s);
        }
        return "";
    }

    // ---- the swap ------------------------------------------------------------------------------------------------------------

    private static String controleer(ServerPlayer p, Kant mijn, Kant hun) {
        for (int i = 0; i < mijn.slots.size(); i++) {
            ItemStack nu = p.getInventory().getItem(mijn.slots.get(i));
            if (!ItemStack.matches(nu, mijn.kopie.get(i)) || !EcoUtil.verhandelbaar(nu)) {
                return "vadsrijk.economie.handel.veranderd";
            }
        }
        if (Spelers.data(p).munten < mijn.munten) {
            return "vadsrijk.economie.handel.te_weinig";
        }
        if (EcoUtil.vrij(p.getInventory()) + mijn.slots.size() < hun.slots.size()) {
            return "vadsrijk.economie.handel.tas_vol";
        }
        return "";
    }

    static String voerUit(MinecraftServer server, Sessie s) {
        ServerPlayer a = server.getPlayerList().getPlayer(s.a.speler), b = server.getPlayerList().getPlayer(s.b.speler);
        if (a == null || b == null) {
            stop(server, s, "vadsrijk.economie.handel.geannuleerd");
            return "vadsrijk.economie.handel.geannuleerd";
        }
        String fout = !dichtbij(a, b) ? "vadsrijk.economie.handel.te_ver" : controleer(a, s.a, s.b);
        if (fout.isEmpty()) {
            fout = controleer(b, s.b, s.a);
        }
        if (!fout.isEmpty()) {
            s.reset();
            EcoUtil.zeg(a, fout);
            EcoUtil.zeg(b, fout);
            stuur(a);
            stuur(b);
            return fout;
        }
        // everything checked: now move it all, in this one call
        List<ItemStack> vanA = haalUit(a, s.a), vanB = haalUit(b, s.b);
        if (s.a.munten > 0) {
            Spelers.neemMunten(a, s.a.munten);
            Spelers.geefMunten(b, s.a.munten);
        }
        if (s.b.munten > 0) {
            Spelers.neemMunten(b, s.b.munten);
            Spelers.geefMunten(a, s.b.munten);
        }
        for (ItemStack st : vanA) {
            if (!EcoUtil.inLegeSlot(b.getInventory(), st)) {
                EcoUtil.geefOfBewaar(b, st);   // (cannot happen after the space check; never lost anyway)
            }
        }
        for (ItemStack st : vanB) {
            if (!EcoUtil.inLegeSlot(a.getInventory(), st)) {
                EcoUtil.geefOfBewaar(a, st);
            }
        }
        a.getInventory().setChanged();
        b.getInventory().setChanged();
        SESSIES.remove(s.a.speler, s);
        SESSIES.remove(s.b.speler, s);
        for (ServerPlayer x : new ServerPlayer[] {a, b}) {
            EcoPayloads.sluit(x);
            EcoUtil.geluid(x, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.8f);
            EcoUtil.chat(x, "vadsrijk.economie.handel.gelukt");
        }
        return "";
    }

    private static List<ItemStack> haalUit(ServerPlayer p, Kant k) {
        List<ItemStack> uit = new ArrayList<>();
        for (int slot : k.slots) {
            uit.add(p.getInventory().getItem(slot).copy());
            p.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        return uit;
    }

    /** Ends p's session (closing, logging out, leaving the dimension). */
    public static void annuleer(ServerPlayer p, String reden) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s != null) {
            stop(p.level().getServer(), s, reden);
        }
        VERZOEKEN.remove(p.getUUID());
    }

    private static void stop(MinecraftServer server, Sessie s, String reden) {
        SESSIES.remove(s.a.speler, s);
        SESSIES.remove(s.b.speler, s);
        for (UUID id : new UUID[] {s.a.speler, s.b.speler}) {
            ServerPlayer x = server.getPlayerList().getPlayer(id);
            if (x != null) {
                EcoPayloads.sluit(x);
                EcoUtil.zeg(x, reden);
            }
        }
    }

    static void vergeet(UUID id) {
        VERZOEKEN.remove(id);
    }

    // ---- the screen ------------------------------------------------------------------------------------------------------------

    static void stuur(ServerPlayer p) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null) {
            return;
        }
        Kant mijn = s.van(p.getUUID()), hun = s.ander(p.getUUID());
        ServerPlayer ander = p.level().getServer().getPlayerList().getPlayer(hun.speler);
        CompoundTag d = new CompoundTag();
        List<ItemStack> items = new ArrayList<>();
        d.putString("ander", ander == null ? "?" : ander.getGameProfile().name());
        d.put("mijn", lijst(mijn.kopie, items));
        d.put("hun", lijst(hun.kopie, items));
        d.putLong("mijnMunten", mijn.munten);
        d.putLong("hunMunten", hun.munten);
        d.putBoolean("mijnKlaar", mijn.klaar);
        d.putBoolean("hunKlaar", hun.klaar);
        d.putBoolean("mijnBevestigd", mijn.bevestigd);
        d.putBoolean("hunBevestigd", hun.bevestigd);
        d.putLong("munten", Spelers.data(p).munten);
        ListTag tas = new ListTag();
        for (int i = 0; i < EcoUtil.TAS; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!st.isEmpty() && !mijn.slots.contains(i)) {
                CompoundTag t = new CompoundTag();
                t.putInt("slot", i);
                t.putBoolean("gebonden", !EcoUtil.verhandelbaar(st));
                t.putInt("item", items.size());
                items.add(st.copy());
                tas.add(t);
            }
        }
        d.put("tas", tas);
        EcoPayloads.open(p, EcoPayloads.HANDEL, "", d, items);
    }

    private static ListTag lijst(List<ItemStack> l, List<ItemStack> items) {
        ListTag t = new ListTag();
        for (ItemStack s : l) {
            CompoundTag c = new CompoundTag();
            c.putInt("item", items.size());
            items.add(s.copy());
            t.add(c);
        }
        return t;
    }

    static void doe(ServerPlayer p, String actie, CompoundTag a) {
        Sessie s = SESSIES.get(p.getUUID());
        if (s == null) {
            EcoPayloads.sluit(p);
            return;
        }
        String melding = switch (actie) {
            case "leg" -> leg(p, a.getIntOr("slot", -1));
            case "pak" -> {
                pak(p, a.getIntOr("index", -1));
                yield "";
            }
            case "munten" -> {
                munten(p, a.getLongOr("n", 0));
                yield "";
            }
            case "klaar" -> {
                klaar(p);
                yield "";
            }
            case "bevestig" -> bevestig(p);
            case "annuleer" -> {
                annuleer(p, "vadsrijk.economie.handel.geannuleerd");
                yield "";
            }
            default -> "";
        };
        if (!melding.isEmpty()) {
            EcoUtil.zeg(p, melding);
        }
        if (SESSIES.get(p.getUUID()) == s) {
            stuur(p);
            ServerPlayer ander = p.level().getServer().getPlayerList().getPlayer(s.ander(p.getUUID()).speler);
            if (ander != null) {
                stuur(ander);
            }
        }
    }
}
