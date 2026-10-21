package nl.juiced.guhs.feature.bouwhulp.kern;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * Every ghost of one dimension, the undo lists of its players and what is still owed to players who were offline
 * (SavedData {@code guhs:bouwhulp_spoken} per dimension). Its {@link #tick} builds: per ghost that is building, as many
 * blocks as its tempo allows (from its {@link Bouwer} or its own {@link Spook#tempo}), from the bottom up.
 * <p>
 * Building a spot: an equal block is skipped for free; a protected spot ({@code Protected}/{@code Bescherming}), a block
 * with a block entity (a chest, a machine) or an unbreakable block is never touched (the spot is dropped); a missing
 * item leaves the spot a ghost ("alsnog bouwen"). What stood in the way is kept in the undo action ({@link BouwActie}).
 */
public final class Spoken extends SavedData {
    public static final SavedDataType<Spoken> TYPE = GuhSavedData.tagType("bouwhulp_spoken", Spoken::new, Spoken::laad, Spoken::opslaan);
    /** Undo: the last 5 actions per player (design 2.1). */
    public static final int MAX_ACTIES = 5;
    /** Open ghosts per player (an own choice, BOUWHULP_KEUZES.md). */
    public static final int MAX_SPOKEN = 10;
    /** At most this many spots are looked at per ghost per tick (equal blocks are free, but not endless). */
    public static final int MAX_KIJK = 4096;
    /** Pof!: this many blocks per tick (a 64-box takes a few ticks; it still looks like one puff). */
    public static final int POF_PER_TICK = 20000;

    final Map<UUID, Spook> spoken = new LinkedHashMap<>();
    /** Per player, oldest first. */
    final Map<UUID, ArrayDeque<BouwActie>> acties = new HashMap<>();
    /** Items owed to players who were offline when their action fell off the undo list. */
    final Map<UUID, Map<Item, Integer>> tegoed = new HashMap<>();
    /** Ghosts in Pof! mode (not saved: a restart finishes them block by block). */
    private final java.util.Set<UUID> pof = new java.util.HashSet<>();

    public Spoken() {
    }

    public static Spoken van(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    // =====================================================================================================================
    // the ghosts
    // =====================================================================================================================

    @Nullable
    public Spook get(UUID id) {
        return spoken.get(id);
    }

    public Collection<Spook> alle() {
        return spoken.values();
    }

    /** The ghosts this player owns or helps with. */
    public List<Spook> van(UUID speler) {
        List<Spook> uit = new ArrayList<>();
        for (Spook s : spoken.values()) {
            if (s.eigenaar.equals(speler) || s.helpers.contains(speler)) {
                uit.add(s);
            }
        }
        return uit;
    }

    public int aantalVan(UUID eigenaar) {
        int n = 0;
        for (Spook s : spoken.values()) {
            if (s.eigenaar.equals(eigenaar)) {
                n++;
            }
        }
        return n;
    }

    Spook voegToe(ServerLevel level, ServerPlayer eigenaar, Ontwerp ontwerp, BlockPos oorsprong) {
        Spook s = new Spook(UUID.randomUUID(), eigenaar.getUUID(), eigenaar.getGameProfile().name(), ontwerp, oorsprong);
        spoken.put(s.id, s);
        veranderd(level, s);
        return s;
    }

    /** It changed (moved, turned, another status): save and tell the clients. */
    public void veranderd(ServerLevel level, Spook s) {
        setDirty();
        SpookPayloads.stuur(level, s);
    }

    /** Takes a ghost away (cancelled, or the last block stands). */
    public void weg(ServerLevel level, Spook s) {
        if (spoken.remove(s.id) != null) {
            pof.remove(s.id);
            setDirty();
            SpookPayloads.weg(level, s.id);
        }
    }

    // =====================================================================================================================
    // building
    // =====================================================================================================================

    /** The answer to "Bouw!". */
    public record Bouwen(boolean begonnen, Map<Item, Integer> tekort, boolean pof) {
    }

    /**
     * "Bouw!": checks the material of every spot that still differs from the ghost. Something missing and not
     * {@code alsnog}: nothing happens and the shortage comes back (the screen asks "alsnog bouwen?"). Otherwise it starts
     * (again), with a new undo action of this player.
     */
    public Bouwen bouw(ServerLevel level, ServerPlayer p, Spook s, boolean alsnog) {
        if (s.status == Spook.Status.BOUWT) {
            return new Bouwen(true, Map.of(), pof.contains(s.id));
        }
        Bouwer bouwer = bouwer(level, s);
        Materiaal.Bronnen bronnen = bronnen(level, s, bouwer, p);
        Map<Item, Integer> nodig = nogNodig(level, s);
        Map<Item, Integer> tekort = bronnen.tekort(nodig);
        if (!tekort.isEmpty() && !alsnog) {
            return new Bouwen(false, tekort, false);
        }
        s.volgende = 0;
        s.mist.clear();
        s.status = Spook.Status.BOUWT;
        s.bouwer = p.getUUID();
        BouwActie actie = new BouwActie(UUID.randomUUID(), p.getUUID(), s.id);
        s.actie = actie.id;
        ArrayDeque<BouwActie> lijst = acties.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        lijst.addLast(actie);
        while (lijst.size() > MAX_ACTIES) {
            sluit(level, lijst.removeFirst());
        }
        boolean poffen = tekort.isEmpty() && bouwer != null && bouwer.pof(level, s);
        if (poffen) {
            pof.add(s.id);
            Feestje.pof(level, s);
        }
        veranderd(level, s);
        return new Bouwen(true, tekort, poffen);
    }

    /** The items still needed: every spot that differs from the ghost now (whatever is built already costs nothing). */
    public Map<Item, Integer> nogNodig(ServerLevel level, Spook s) {
        Map<Item, Integer> uit = new LinkedHashMap<>();
        for (Map.Entry<BlockPos, BlockState> e : s.volgorde()) {
            if (level.isLoaded(e.getKey()) && level.getBlockState(e.getKey()) == e.getValue()) {
                continue;
            }
            Materiaal.Kost k = Materiaal.kost(e.getValue());
            if (k != null && k.aantal() > 0) {
                uit.merge(k.item(), k.aantal(), Integer::sum);
            }
        }
        return uit;
    }

    /** Stops building (the ghost stays; what stands, stands). */
    public void pauzeer(ServerLevel level, Spook s) {
        if (s.status == Spook.Status.BOUWT) {
            s.status = Spook.Status.WACHT;
            pof.remove(s.id);
            veranderd(level, s);
        }
    }

    @Nullable
    Bouwer bouwer(ServerLevel level, Spook s) {
        if (s.bouwerPos == null || !level.isLoaded(s.bouwerPos)) {
            return null;
        }
        return level.getBlockEntity(s.bouwerPos) instanceof Bouwer b ? b : null;
    }

    Materiaal.Bronnen bronnen(ServerLevel level, Spook s, @Nullable Bouwer bouwer, @Nullable ServerPlayer p) {
        return bouwer != null ? bouwer.bronnen(level, s, p) : Materiaal.bronnen(level, p, s.bank, null);
    }

    @Nullable
    private BouwActie actie(Spook s) {
        if (s.actie == null || s.bouwer == null) {
            return null;
        }
        ArrayDeque<BouwActie> lijst = acties.get(s.bouwer);
        if (lijst != null) {
            for (BouwActie a : lijst) {
                if (a.id.equals(s.actie)) {
                    return a;
                }
            }
        }
        return null;
    }

    public void tick(ServerLevel level) {
        if (spoken.isEmpty()) {
            return;
        }
        boolean meld = level.getGameTime() % 20 == 0;
        for (Spook s : new ArrayList<>(spoken.values())) {
            if (s.status != Spook.Status.BOUWT) {
                continue;
            }
            if (s.bouwerPos != null && !level.isLoaded(s.bouwerPos)) {
                continue;   // the keet sleeps: so does its project
            }
            Bouwer bouwer = bouwer(level, s);
            if (s.bouwerPos != null && bouwer == null) {
                pauzeer(level, s);   // the keet is gone
                continue;
            }
            boolean poffen = pof.contains(s.id);
            double tempo = bouwer != null ? bouwer.tempo(level, s) : s.tempo;
            if (!poffen) {
                if (tempo <= 0) {
                    continue;
                }
                s.opgespaard = Math.min(s.opgespaard + tempo / 20.0, Math.max(1, tempo));
                if (s.opgespaard < 1) {
                    continue;
                }
            }
            ServerPlayer p = s.bouwer == null ? null : level.getServer().getPlayerList().getPlayer(s.bouwer);
            Materiaal.Bronnen bronnen = bronnen(level, s, bouwer, p);
            BouwActie actie = actie(s);
            bouwStukje(level, s, bouwer, bronnen, actie, poffen);
            if (meld) {
                SpookPayloads.voortgang(level, s);
            }
        }
    }

    private void bouwStukje(ServerLevel level, Spook s, @Nullable Bouwer bouwer, Materiaal.Bronnen bronnen, @Nullable BouwActie actie, boolean poffen) {
        List<Map.Entry<BlockPos, BlockState>> v = s.volgorde();
        int gekeken = 0, geplaatst = 0;
        int budget = poffen ? POF_PER_TICK : (int) s.opgespaard;
        while (s.volgende < v.size() && geplaatst < budget && gekeken < (poffen ? POF_PER_TICK * 2 : MAX_KIJK)) {
            Map.Entry<BlockPos, BlockState> e = v.get(s.volgende);
            BlockPos pos = e.getKey();
            if (!level.isLoaded(pos)) {
                break;   // wait for the chunk
            }
            s.volgende++;
            gekeken++;
            BlockState doel = e.getValue(), nu = level.getBlockState(pos);
            if (nu == doel || !magBouwen(level, pos, nu)) {
                continue;
            }
            Materiaal.Kost kost = Materiaal.kost(doel);
            if (kost == null) {
                continue;
            }
            boolean betaald = false;
            if (kost.aantal() > 0 && !bronnen.gratis()) {
                int kreeg = bronnen.neem(kost.item(), kost.aantal());
                if (kreeg < kost.aantal()) {
                    if (kreeg > 0) {
                        Materiaal.terug(level, bronnen, new ItemStack(kost.item(), kreeg), pos);
                    }
                    s.mist.add(pos.asLong());
                    continue;
                }
                betaald = true;
            }
            level.setBlock(pos, doel, Block.UPDATE_ALL);
            if (actie != null) {
                actie.voegToe(pos, nu, doel, betaald);
            }
            s.gedaan++;
            geplaatst++;
            if (bouwer != null) {
                bouwer.geplaatst(level, s, pos);
            }
        }
        s.opgespaard -= geplaatst;
        if (geplaatst > 0 || gekeken > 0) {
            setDirty();
        }
        if (s.volgende >= v.size()) {
            klaar(level, s, bouwer);
        }
    }

    /** May a builder change this spot (not protected, not a block entity, not unbreakable, inside the world)? */
    public static boolean magBouwen(ServerLevel level, BlockPos pos, BlockState nu) {
        if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        if (nu.hasBlockEntity() || nu.getDestroySpeed(level, pos) < 0) {
            return false;
        }
        return !Protected.at(level, pos) && !Bescherming.beschermd(level, pos);
    }

    private void klaar(ServerLevel level, Spook s, @Nullable Bouwer bouwer) {
        boolean helemaal = s.mist.isEmpty();
        pof.remove(s.id);
        ServerPlayer p = s.bouwer == null ? null : level.getServer().getPlayerList().getPlayer(s.bouwer);
        if (helemaal) {
            Feestje.klaar(level, s, p);
            if (bouwer != null) {
                bouwer.gestopt(level, s, true);
            }
            weg(level, s);
        } else {
            s.status = Spook.Status.WACHT;
            if (p != null) {
                p.sendSystemMessage(Component.translatable("gui.guhs.bouwhulp.spook.mist", s.mist.size()));
            }
            if (bouwer != null) {
                bouwer.gestopt(level, s, false);
            }
            veranderd(level, s);
        }
    }

    // =====================================================================================================================
    // undo
    // =====================================================================================================================

    /** How many actions this player can undo here. */
    public int ongedaanTeMaken(UUID speler) {
        ArrayDeque<BouwActie> lijst = acties.get(speler);
        return lijst == null ? 0 : lijst.size();
    }

    /**
     * Undoes the player's last action: every block that is still what was built goes back to what stood there (from the
     * action, free) and the paid item comes back; a block the player changed since stays, and its old block's item comes
     * back instead. A ghost that is still building that action stops (it stays a ghost). False: nothing to undo.
     */
    public boolean ongedaan(ServerLevel level, ServerPlayer p) {
        ArrayDeque<BouwActie> lijst = acties.get(p.getUUID());
        if (lijst == null || lijst.isEmpty()) {
            return false;
        }
        BouwActie a = lijst.removeLast();
        Spook s = spoken.get(a.spook);
        if (s != null && a.id.equals(s.actie)) {
            s.actie = null;
            s.status = Spook.Status.SPOOK;
            s.volgende = 0;
            s.mist.clear();
            s.gedaan = 0;
            pof.remove(s.id);
            veranderd(level, s);
        }
        Materiaal.Bronnen bronnen = Materiaal.bronnen(level, p, s == null ? null : s.bank, null);
        if (s != null) {
            Bouwer b = bouwer(level, s);
            if (b != null) {
                bronnen = b.bronnen(level, s, p);
            }
        }
        List<BouwActie.Wijziging> w = a.wijzigingen;
        for (int i = w.size() - 1; i >= 0; i--) {
            BouwActie.Wijziging x = w.get(i);
            if (!level.isLoaded(x.pos())) {
                geefOud(level, bronnen, x, p);
                continue;
            }
            if (level.getBlockState(x.pos()) == x.nieuw()) {
                level.setBlock(x.pos(), x.oud(), Block.UPDATE_ALL);
                Materiaal.Kost k = Materiaal.kost(x.nieuw());
                if (x.betaald() && k != null && k.aantal() > 0) {
                    Materiaal.terug(level, bronnen, new ItemStack(k.item(), k.aantal()), p.blockPosition());
                }
            } else {
                geefOud(level, bronnen, x, p);
            }
        }
        setDirty();
        return true;
    }

    private static void geefOud(ServerLevel level, Materiaal.Bronnen bronnen, BouwActie.Wijziging x, ServerPlayer p) {
        Materiaal.Kost k = Materiaal.kost(x.oud());
        if (k != null && k.aantal() > 0) {
            Materiaal.terug(level, bronnen, new ItemStack(k.item(), k.aantal()), p.blockPosition());
        }
    }

    /** An action fell off the undo list: the blocks that were in the way go to the player now (or later, when offline). */
    private void sluit(ServerLevel level, BouwActie a) {
        Map<Item, Integer> items = a.bewaard();
        if (items.isEmpty()) {
            return;
        }
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(a.speler);
        if (p == null) {
            Map<Item, Integer> t = tegoed.computeIfAbsent(a.speler, k -> new LinkedHashMap<>());
            items.forEach((item, n) -> t.merge(item, n, Integer::sum));
            setDirty();
            return;
        }
        Materiaal.Bronnen bronnen = Materiaal.bronnen(level, p, null, null);
        for (ItemStack st : BouwActie.stapels(items)) {
            Materiaal.terug(level, bronnen, st, p.blockPosition());
        }
    }

    /** Hands out what was owed to this player (at login). */
    public void geefTegoed(ServerLevel level, ServerPlayer p) {
        Map<Item, Integer> t = tegoed.remove(p.getUUID());
        if (t == null) {
            return;
        }
        Materiaal.Bronnen bronnen = Materiaal.bronnen(level, p, null, null);
        for (ItemStack st : BouwActie.stapels(t)) {
            Materiaal.terug(level, bronnen, st, p.blockPosition());
        }
        setDirty();
    }

    // =====================================================================================================================
    // saving
    // =====================================================================================================================

    private CompoundTag opslaan() {
        CompoundTag t = new CompoundTag();
        ListTag s = new ListTag();
        spoken.values().forEach(x -> s.add(x.opslaan()));
        t.put("Spoken", s);
        ListTag a = new ListTag();
        acties.values().forEach(l -> l.forEach(x -> a.add(x.opslaan())));
        t.put("Acties", a);
        ListTag g = new ListTag();
        tegoed.forEach((speler, items) -> {
            CompoundTag c = new CompoundTag();
            c.putString("Speler", speler.toString());
            CompoundTag i = new CompoundTag();
            items.forEach((item, n) -> i.putInt(BuiltInRegistries.ITEM.getKey(item).toString(), n));
            c.put("Items", i);
            g.add(c);
        });
        t.put("Tegoed", g);
        return t;
    }

    private static Spoken laad(CompoundTag t) {
        Spoken d = new Spoken();
        ListTag s = t.getListOrEmpty("Spoken");
        for (int i = 0; i < s.size(); i++) {
            Spook x = Spook.laad(s.getCompoundOrEmpty(i));
            d.spoken.put(x.id, x);
        }
        ListTag a = t.getListOrEmpty("Acties");
        for (int i = 0; i < a.size(); i++) {
            BouwActie x = BouwActie.laad(a.getCompoundOrEmpty(i));
            d.acties.computeIfAbsent(x.speler, k -> new ArrayDeque<>()).addLast(x);
        }
        ListTag g = t.getListOrEmpty("Tegoed");
        for (int i = 0; i < g.size(); i++) {
            CompoundTag c = g.getCompoundOrEmpty(i);
            try {
                UUID speler = UUID.fromString(c.getStringOr("Speler", ""));
                Map<Item, Integer> items = new LinkedHashMap<>();
                CompoundTag it = c.getCompoundOrEmpty("Items");
                for (String k : it.keySet()) {
                    BuiltInRegistries.ITEM.getOptional(Identifier.parse(k)).ifPresent(item -> items.put(item, it.getIntOr(k, 0)));
                }
                d.tegoed.put(speler, items);
            } catch (IllegalArgumentException ignored) {
                // (a broken line)
            }
        }
        return d;
    }
}
