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

    /** Takes a ghost away (cancelled, or the last block stands). Its crafted credit goes back (1.10.10). */
    public void weg(ServerLevel level, Spook s) {
        if (spoken.remove(s.id) != null) {
            geefTegoedTerug(level, s);
            pof.remove(s.id);
            setDirty();
            SpookPayloads.weg(level, s.id);
        }
    }

    // =====================================================================================================================
    // building
    // =====================================================================================================================

    /**
     * The answer to "Bouw!" ({@code tekort}: what is missing after crafting; {@code gemaakt}: what the keet crafts). With a
     * keet and something missing (1.10.10): {@code dekking} = the percentage of block spots that can be built, {@code open}
     * = how many would stay open (air), {@code teWeinig} = below {@link #MIN_DEKKING}: refused. Elsewhere dekking is -1.
     */
    public record Bouwen(boolean begonnen, Map<Item, Integer> tekort, boolean pof, List<Werkplan.Gemaakt> gemaakt, int dekking, int open,
                         boolean teWeinig) {
        public Bouwen(boolean begonnen, Map<Item, Integer> tekort, boolean pof) {
            this(begonnen, tekort, pof, List.of());
        }

        public Bouwen(boolean begonnen, Map<Item, Integer> tekort, boolean pof, List<Werkplan.Gemaakt> gemaakt) {
            this(begonnen, tekort, pof, gemaakt, -1, 0, false);
        }
    }

    /**
     * 1.10.10 (the user's 80% rule): a keet starts a build with something missing only when at least this percentage of
     * its block spots can be built (crafting and the water rule counted); the missing spots then stay open (air) and the
     * build ends as done.
     */
    public static final int MIN_DEKKING = 80;

    /** How much of a build can be made: {@code procent} of {@code totaal} block spots; {@code open} spots stay air. */
    public record Dekking(int procent, int totaal, int open) {
        public boolean genoeg() {
            return procent >= MIN_DEKKING;
        }
    }

    /**
     * The coverage of the rest of this ghost with this plan. Every spot that still differs and is not air counts (free
     * ones such as door tops as covered). Per item the missing amount is spread over the spots that use it (a double slab
     * is 2 slabs for 1 spot); the water of a keet is all-or-nothing (the infinite source).
     */
    public Dekking dekking(ServerLevel level, Spook s, Werkplan plan) {
        Map<Item, Integer> plekken = new HashMap<>(), stuks = new HashMap<>();
        int totaal = 0;
        boolean keetWater = oneindigWater(level, s);
        for (Map.Entry<BlockPos, BlockState> e : s.volgorde()) {
            if (e.getValue().isAir() || level.isLoaded(e.getKey()) && level.getBlockState(e.getKey()) == e.getValue()) {
                continue;
            }
            Materiaal.Kost k = Materiaal.kost(e.getValue());
            if (k == null) {
                continue;
            }
            totaal++;
            if (k.oneindig() && keetWater) {
                plekken.merge(k.item(), 1, Integer::sum);
            } else if (k.aantal() > 0) {
                plekken.merge(k.item(), 1, Integer::sum);
                stuks.merge(k.item(), k.aantal(), Integer::sum);
                if (k.heeftExtra()) {
                    plekken.merge(k.extra(), 1, Integer::sum);
                    stuks.merge(k.extra(), 1, Integer::sum);
                }
            }
        }
        long open = 0;
        for (Map.Entry<Item, Integer> t : plan.tekort().entrySet()) {
            int p = plekken.getOrDefault(t.getKey(), 0), n = stuks.getOrDefault(t.getKey(), 0);
            if (p == 0) {
                continue;
            }
            open += n == 0 ? p : Math.min(p, ((long) t.getValue() * p + n - 1) / n);   // (n == 0: the water buckets)
        }
        int o = (int) Math.min(totaal, open);
        int procent = totaal == 0 ? 100 : (int) Math.floor((totaal - o) * 100.0 / totaal);
        return new Dekking(procent, totaal, o);
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
        Werkplan plan = Werkplan.maak(level, nodig, bronnen);   // 1.10.10: what is not there may be crafted
        Map<Item, Integer> tekort = plan.tekort();
        // 1.10.10, the 80% rule (a keet): enough there -> ask, then build and leave the rest open (air); too little -> no
        Dekking d = !tekort.isEmpty() && !bronnen.gratis() && bouwer != null && bouwer.tachtigProcent() ? dekking(level, s, plan) : null;
        if (d != null && (!d.genoeg() || !alsnog)) {
            return new Bouwen(false, tekort, false, plan.gemaakt(), d.procent(), d.open(), !d.genoeg());
        }
        if (!tekort.isEmpty() && !alsnog) {
            return new Bouwen(false, tekort, false, plan.gemaakt());
        }
        if (!plan.stappen().isEmpty()) {
            plan.voerUit(bronnen, s.tegoed);
            p.sendSystemMessage(Werkplan.bericht(plan.gemaakt()));
        }
        s.volgende = 0;
        s.mist.clear();
        s.openLaten = d != null;
        s.opengelaten = 0;
        s.status = Spook.Status.BOUWT;
        s.bouwer = p.getUUID();
        BouwActie actie = new BouwActie(UUID.randomUUID(), p.getUUID(), s.id);
        s.actie = actie.id;
        ArrayDeque<BouwActie> lijst = acties.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>());
        lijst.addLast(actie);
        while (lijst.size() > MAX_ACTIES) {
            sluit(level, lijst.removeFirst());
        }
        boolean poffen = (tekort.isEmpty() || s.openLaten) && bouwer != null && bouwer.pof(level, s);
        if (poffen) {
            pof.add(s.id);
            Feestje.pof(level, s);
        }
        veranderd(level, s);
        return new Bouwen(true, tekort, poffen, plan.gemaakt(), d == null ? -1 : d.procent(), d == null ? 0 : d.open(), false);
    }

    /**
     * The items still needed: every spot that differs from the ghost now (whatever is built already costs nothing). With a
     * keet, water still to place asks for {@link Materiaal#WATER_EMMERS} water buckets (there, not used up; 1.10.10).
     */
    public Map<Item, Integer> nogNodig(ServerLevel level, Spook s) {
        Map<Item, Integer> uit = new LinkedHashMap<>();
        boolean water = false;
        for (Map.Entry<BlockPos, BlockState> e : s.volgorde()) {
            if (level.isLoaded(e.getKey()) && level.getBlockState(e.getKey()) == e.getValue()) {
                continue;
            }
            Materiaal.Kost k = Materiaal.kost(e.getValue());
            if (k != null) {
                k.telOp(uit);
                water |= k.oneindig();
            }
        }
        if (water && oneindigWater(level, s)) {
            uit.merge(net.minecraft.world.item.Items.WATER_BUCKET, Materiaal.WATER_EMMERS, Math::max);
        }
        return uit;
    }

    /** Does this ghost's water need the infinite source (its builder is a keet)? */
    boolean oneindigWater(ServerLevel level, Spook s) {
        Bouwer b = bouwer(level, s);
        return b != null && b.oneindigWater();
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

    /** The places of this build, its crafted credit in front (1.10.10). */
    Materiaal.Bronnen bronnen(ServerLevel level, Spook s, @Nullable Bouwer bouwer, @Nullable ServerPlayer p) {
        return (bouwer != null ? bouwer.bronnen(level, s, p) : Materiaal.bronnen(level, p, s.bank, null)).metTegoed(s.tegoed);
    }

    /** The crafted credit goes back: inventory, bank, the keet's chests; what has no place falls at the keet/ghost. */
    private void geefTegoedTerug(ServerLevel level, Spook s) {
        if (s.tegoed.isEmpty()) {
            return;
        }
        UUID wie = s.bouwer != null ? s.bouwer : s.eigenaar;
        ServerPlayer p = level.getServer().getPlayerList().getPlayer(wie);
        Bouwer b = bouwer(level, s);
        Materiaal.Bronnen terug = b != null ? b.bronnen(level, s, p) : Materiaal.bronnen(level, p, s.bank, null);
        BlockPos waar = s.bouwerPos != null ? s.bouwerPos : p != null ? p.blockPosition() : s.oorsprong;
        for (ItemStack st : BouwActie.stapels(s.tegoed)) {
            ItemStack rest = terug.gratis() ? st : terug.terug(st);
            while (!rest.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, waar.getX() + 0.5, waar.getY() + 1.5, waar.getZ() + 0.5, rest.split(rest.getMaxStackSize()));
            }
        }
        s.tegoed.clear();
        setDirty();
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
        Boolean bron = null;   // 1.10.10: the infinite water source, asked once per tick (only when there is water)
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
            if (doel.getBlock() instanceof nl.juiced.guhs.block.SleeRailPartBlock) {
                continue;   // 1.10.10: a sled rail part is laid by its anchor (a part without its anchor breaks)
            }
            if (nu == doel || !magBouwen(level, pos, nu)) {
                continue;
            }
            if (bouwer != null && buiten(s.bouwerPos, bouwer.bereik(level), pos)) {
                s.mist.add(pos.asLong());   // test round 5: out of the keet's reach, stays a ghost (drawn red)
                continue;
            }
            Materiaal.Kost kost = Materiaal.kost(doel);
            if (kost == null) {
                continue;
            }
            if (kost.oneindig() && !bronnen.gratis() && bouwer != null && bouwer.oneindigWater()) {
                if (bron == null) {
                    bron = bronnen.tel(kost.item()) >= Materiaal.WATER_EMMERS;
                }
                if (!bron) {
                    mist(level, s, pos, nu, actie);   // no infinite source: the water stays a ghost (80% rule: open)
                    continue;
                }
            }
            boolean betaald = false;
            if (kost.aantal() > 0 && !bronnen.gratis()) {
                int kreeg = bronnen.neem(kost.item(), kost.aantal());
                // 1.10.10: a potted plant also takes the plant
                if (kreeg < kost.aantal() || kost.heeftExtra() && bronnen.neem(kost.extra(), 1) < 1) {
                    if (kreeg > 0) {
                        Materiaal.terug(level, bronnen, new ItemStack(kost.item(), kreeg), pos);
                    }
                    mist(level, s, pos, nu, actie);
                    continue;
                }
                betaald = true;
            }
            level.setBlock(pos, doel, Block.UPDATE_ALL);
            if (actie != null) {
                actie.voegToe(pos, nu, doel, betaald);
            }
            if (doel.getBlock() instanceof nl.juiced.guhs.block.SleeRailBlock) {
                s.gedaan += sleeParts(level, pos, doel, actie);
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

    /**
     * 1.10.10: a sled rail anchor was placed: lay its parts right after it, like SleeRailItem.place (computed from the
     * anchor's facing and shape, so a turned or mirrored ghost lays them right too). Spots that may not be built are left
     * out. Returns how many parts were laid.
     */
    private static int sleeParts(ServerLevel level, BlockPos anker, BlockState staat, @Nullable BouwActie actie) {
        nl.juiced.guhs.block.SleeRailPartBlock part = nl.juiced.guhs.registry.ModBlocks.SLEE_RAIL_PART.get();
        int n = 0;
        for (BlockPos p : new nl.juiced.guhs.slee.SleePath.Placement(anker, staat.getValue(nl.juiced.guhs.block.SleeRailBlock.FACING),
                staat.getValue(nl.juiced.guhs.block.SleeRailBlock.SHAPE)).blocks()) {
            if (p.equals(anker) || !level.isLoaded(p)) {
                continue;
            }
            BlockState nu = level.getBlockState(p), wil = part.forOffset(p.subtract(anker));
            if (nu == wil || !magBouwen(level, p, nu)) {
                continue;
            }
            level.setBlock(p, wil, Block.UPDATE_ALL);
            if (actie != null) {
                actie.voegToe(p, nu, wil, false);
            }
            n++;
        }
        return n;
    }

    /**
     * The material of a spot is missing: it stays a ghost ("alsnog bouwen"), or, in a build started by the 80% rule
     * (1.10.10), it stays open: air (what stood there goes into the undo action, like a dug-out spot).
     */
    private static void mist(ServerLevel level, Spook s, BlockPos pos, BlockState nu, @Nullable BouwActie actie) {
        if (!s.openLaten) {
            s.mist.add(pos.asLong());
            return;
        }
        if (!nu.isAir()) {
            BlockState lucht = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            level.setBlock(pos, lucht, Block.UPDATE_ALL);
            if (actie != null) {
                actie.voegToe(pos, nu, lucht, false);
            }
        }
        s.opengelaten++;
    }

    /** Test round 5: is this spot further than {@code bereik} from the builder's block (in x or in z)? */
    public static boolean buiten(@Nullable BlockPos bouwerPos, int bereik, BlockPos pos) {
        return bouwerPos != null && bereik < Integer.MAX_VALUE
                && (Math.abs(pos.getX() - bouwerPos.getX()) > bereik || Math.abs(pos.getZ() - bouwerPos.getZ()) > bereik);
    }

    /** How many blocks of this ghost lie out of its builder's reach. */
    public static int buiten(ServerLevel level, Spook s, @Nullable Bouwer bouwer) {
        if (bouwer == null || s.bouwerPos == null) {
            return 0;
        }
        int bereik = bouwer.bereik(level), n = 0;
        for (Map.Entry<BlockPos, BlockState> e : s.volgorde()) {
            if (!e.getValue().isAir() && buiten(s.bouwerPos, bereik, e.getKey())) {
                n++;
            }
        }
        return n;
    }

    /** (The builder of a ghost: for the menus.) */
    @Nullable
    public Bouwer bouwerVan(ServerLevel level, Spook s) {
        return bouwer(level, s);
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
            if (p != null && s.opengelaten > 0) {
                p.sendSystemMessage(Component.translatable("gui.guhs.bouwhulp.spook.open", s.opengelaten).withStyle(net.minecraft.ChatFormatting.GOLD));
            }
            s.openLaten = false;
            Feestje.klaar(level, s, p);
            if (bouwer != null) {
                bouwer.gestopt(level, s, true);
            }
            weg(level, s);
        } else {
            s.status = Spook.Status.WACHT;
            int ver = bouwer == null ? 0 : buiten(level, s, bouwer);
            if (p != null && ver > 0) {
                p.sendSystemMessage(Component.translatable("gui.guhs.bouwhulp.bereik.buiten.klaar", ver, bouwer.bereik(level))
                        .withStyle(net.minecraft.ChatFormatting.GOLD));
                p.sendSystemMessage(Component.translatable("gui.guhs.bouwhulp.bereik.tip").withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if (p != null && s.mist.size() > ver) {
                p.sendSystemMessage(Component.translatable("gui.guhs.bouwhulp.spook.mist", s.mist.size() - ver));
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
                if (x.betaald() && k != null) {
                    for (ItemStack st : k.stapels()) {
                        Materiaal.terug(level, bronnen, st, p.blockPosition());
                    }
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
        if (k != null) {
            for (ItemStack st : k.stapels()) {
                Materiaal.terug(level, bronnen, st, p.blockPosition());
            }
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
