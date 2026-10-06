package nl.juiced.guhs.feature.techbezorg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The Stepstation's block entity (bbq2): the brain of one delivery round. It is a guh machine of
 * {@link VadsGetallen#STEPSTATION} VK. It owns:
 * <ul>
 *   <li><b>the backpack</b> ({@link #vakken()}, {@link Bezorgnet#RUGZAK} stacks). The Bezorgguhtje carries it on its back,
 *       but the items are kept HERE, so whatever happens to the guhtje nothing is lost; the items fall out when the station
 *       is broken. Pipes and hoppers cannot reach it; a player can while the guhtje is home (or whistled to them).</li>
 *   <li><b>the round</b>: the Haltepaaltjes that belong to it, in order ({@link #haltes()}, at most
 *       {@link Bezorgnet#MAX_HALTES}, each within {@link Bezorgnet#BEREIK} blocks).</li>
 *   <li><b>its Bezorgguhtje</b> ({@link BezorgguhtjeEntity}): made when the station appears, found again by its UUID after a
 *       restart, replaced when it is really gone (missing with its chunk loaded for {@link #VERMIST_TICKS}, or out of reach in
 *       an unloaded chunk for {@link #WEG_TICKS}; the old one poofs when it shows up again), and it disappears with the
 *       station.</li>
 * </ul>
 * The round ({@link #altijd}, every server tick): from its dock next to the station the guhtje rides to the next stop that
 * has work ({@link #nuttig}), rummages for {@link #LAAD_TICKS} and moves the items ({@link #wissel}), and so on; after the last
 * stop it rides home and rests. At an "ophalen" stop it takes only what an "afleveren" stop of this round asks for (its
 * filter) and can hold right now, so the backpack does not fill up with things nobody wants. A stop that is not loaded, out
 * of reach or without a chest is skipped. When it does not get closer to where it is going for {@link #VAST_TICKS} it hops
 * there ({@link BezorgguhtjeEntity#hop}): no wall, hole or missing path ever stops a delivery. Without vadskracht it rides
 * home and sleeps. The face: asleep without vadskracht, surprised while the backpack does not get empty, happy otherwise.
 * Resources and texts: tools/features/tech_bezorg.py.
 */
public class StepstationBlockEntity extends MachineBlockEntity implements MenuProvider {
    /** What the guhtje is doing. */
    public enum Fase {
        SLAAPT, RUST, RIJDT, LAADT, NAAR_HUIS, NAAR_SPELER, BIJ_SPELER;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** Ticks: resting at home between rounds, rummaging at a stop, no progress before it hops, the longest ride. */
    public static final int RUST_TICKS = 40, LAAD_TICKS = 24, VAST_TICKS = 80, MAX_RIT_TICKS = 20 * 45;
    /** Ticks: missing while its chunk is loaded / away in an unloaded chunk before a new guhtje is made. */
    public static final int VERMIST_TICKS = 60, WEG_TICKS = 20 * 30;
    /** Ticks: how long it tries to reach a whistling player, and how long it waits there without its backpack being opened. */
    public static final int ROEP_TICKS = 20 * 40, WACHT_BIJ_SPELER = 20 * 20;
    private static final int VEEL = Bezorgnet.RUGZAK * 64;

    private final List<BlockPos> haltes = new ArrayList<>();
    private Fase fase = Fase.SLAAPT;
    private int doel = -1;
    private int wacht;
    @Nullable
    private UUID koerierId;
    @Nullable
    private BlockPos laatstePlek;
    private int vermist, weg;
    @Nullable
    private UUID roeper;
    private int roepTicks;
    /** The backpack did not get empty in the last round. */
    private boolean vast;
    /** Something was delivered in this round. */
    private boolean geleverd;
    /** Items delivered in total. */
    private long gebracht;
    private boolean thuis = true;
    private boolean begonnen;
    // the ride that is going on
    @Nullable
    private BlockPos ritDoel;
    private int rit, stil;
    private double beste;
    @Nullable
    private BlockPos dok;
    private long dokTik = Long.MIN_VALUE;

    public StepstationBlockEntity(BlockPos pos, BlockState state) {
        super(TechbezorgFeature.STEPSTATION_BE.get(), pos, state, VadsGetallen.STEPSTATION, Bezorgnet.RUGZAK);
    }

    // =====================================================================================================================
    // the machine
    // =====================================================================================================================

    /** On the road (the synced "bezig" of the machine). */
    @Override
    protected boolean kanWerken() {
        return fase == Fase.RIJDT || fase == Fase.LAADT || fase == Fase.NAAR_HUIS || fase == Fase.NAAR_SPELER;
    }

    @Override
    protected boolean isVol() {
        return vast;
    }

    /** (The round is run by {@link #altijd}, which also runs without vadskracht.) */
    @Override
    protected void werk() {
    }

    /** Pipes and hoppers stay out of the backpack. */
    @Override
    protected boolean isInvoer(int vak) {
        return false;
    }

    @Override
    protected boolean isUitvoer(int vak) {
        return false;
    }

    /** Only the little the client needs (the screens get their numbers from the menus). */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Bezig", bezig());
        return tag;
    }

    // =====================================================================================================================
    // the stops
    // =====================================================================================================================

    /** The stops of this station, in the order of the round (positions). */
    public List<BlockPos> haltes() {
        return Collections.unmodifiableList(haltes);
    }

    /** Which stop of the round this pole is (0-based), or -1. */
    public int nummer(BlockPos halte) {
        return haltes.indexOf(halte);
    }

    /**
     * Adds this pole as the last stop of the round (it leaves the station it belonged to). False: no room, or too far.
     * True also when it already was a stop here.
     */
    public boolean koppel(HaltepaaltjeBlockEntity halte) {
        BlockPos pos = halte.getBlockPos();
        if (haltes.contains(pos)) {
            if (!worldPosition.equals(halte.station())) {
                halte.zetStation(worldPosition);
            }
            return true;
        }
        if (haltes.size() >= Bezorgnet.MAX_HALTES || !Bezorgnet.binnenBereik(worldPosition, pos) || halte.getLevel() != level) {
            return false;
        }
        StepstationBlockEntity oud = halte.thuis();
        if (oud != null && oud != this) {
            oud.vergeet(pos);
        }
        haltes.add(pos.immutable());
        halte.zetStation(worldPosition);
        rondeVeranderd();
        return true;
    }

    /** Takes this pole out of the round and tells the pole (when it is loaded). */
    public void ontkoppel(BlockPos halte) {
        if (level != null && level.isLoaded(halte) && level.getBlockEntity(halte) instanceof HaltepaaltjeBlockEntity h
                && worldPosition.equals(h.station())) {
            h.zetStation(null);
        }
        vergeet(halte);
    }

    /** Takes this pole out of the round (the pole itself is gone or went to another station). */
    void vergeet(BlockPos halte) {
        if (haltes.remove(halte)) {
            rondeVeranderd();
        }
    }

    /** Moves a stop one place earlier (-1) or later (+1) in the round. */
    public boolean schuif(int index, int stap) {
        int naar = index + stap;
        if (index < 0 || index >= haltes.size() || naar < 0 || naar >= haltes.size()) {
            return false;
        }
        Collections.swap(haltes, index, naar);
        rondeVeranderd();
        return true;
    }

    /** The round changed: a ride to a stop is broken off (the guhtje goes home and starts again). */
    private void rondeVeranderd() {
        if (fase == Fase.RIJDT || fase == Fase.LAADT) {
            zetFase(Fase.NAAR_HUIS);
        }
        doel = -1;
        setChanged();
    }

    /** The pole of stop i, when it can be served right now: loaded, ticking, within reach, still ours. */
    @Nullable
    HaltepaaltjeBlockEntity halte(ServerLevel sl, int i) {
        if (i < 0 || i >= haltes.size()) {
            return null;
        }
        BlockPos pos = haltes.get(i);
        if (!sl.isLoaded(pos) || !sl.isPositionEntityTicking(pos) || !Bezorgnet.binnenBereik(worldPosition, pos)) {
            return null;
        }
        return sl.getBlockEntity(pos) instanceof HaltepaaltjeBlockEntity h && worldPosition.equals(h.station()) ? h : null;
    }

    /** Is there a stop that takes things (other than this pole)? */
    public boolean heeftOphaalHalte(@Nullable BlockPos behalve) {
        return heeftHalte(true, behalve);
    }

    /** Is there a stop that brings things (other than this pole)? */
    public boolean heeftAfleverHalte(@Nullable BlockPos behalve) {
        return heeftHalte(false, behalve);
    }

    private boolean heeftHalte(boolean ophalen, @Nullable BlockPos behalve) {
        if (level == null) {
            return false;
        }
        for (BlockPos pos : haltes) {
            if (!pos.equals(behalve) && level.isLoaded(pos) && level.getBlockEntity(pos) instanceof HaltepaaltjeBlockEntity h && h.ophalen() == ophalen) {
                return true;
            }
        }
        return false;
    }

    /** Drops the stops whose pole is gone or went elsewhere (only where the chunk is loaded: an unloaded stop is just skipped). */
    private void controleerHaltes(ServerLevel sl) {
        boolean weg = haltes.removeIf(pos -> sl.isLoaded(pos)
                && !(sl.getBlockEntity(pos) instanceof HaltepaaltjeBlockEntity h && worldPosition.equals(h.station())));
        if (weg) {
            rondeVeranderd();
        }
    }

    /**
     * Takes in the poles around it that say they are ours but are not in the list (after a mishap), and with {@code losse}
     * also the loose poles within reach (no station, or a station that is gone) while there is room, nearest first: that is
     * what a Stepstation does when a player places it, so poles that stood there first, or whose station was moved, just
     * work. Returns how many joined.
     */
    public int adopteer(ServerLevel sl, boolean losse) {
        int erbij = 0;
        for (HaltepaaltjeBlockEntity halte : Bezorgnet.haltes(sl, worldPosition)) {
            boolean vanOns = worldPosition.equals(halte.station());
            if ((vanOns && !haltes.contains(halte.getBlockPos())) || (losse && !vanOns && halte.isLos())) {
                if (koppel(halte)) {
                    erbij++;
                }
            }
        }
        return erbij;
    }

    /** Placed by a player (the owner is set by MachineBlock): the loose poles join, and the player hears about it. */
    void geplaatst(ServerPlayer player) {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        begonnen = true;
        int erbij = adopteer(sl, true);
        GuhAdvancements.grant(player, "tech_bezorg_station");
        GidsFeature.grant(player, "techniek/tech_bezorg_station");
        player.sendOverlayMessage((erbij > 0 ? Component.translatable("gui.guhs.techbezorg.station.gekoppeld", erbij)
                : Component.translatable("gui.guhs.techbezorg.station.nieuw")).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // the Bezorgguhtje
    // =====================================================================================================================

    /** Is this the guhtje of this station? */
    public boolean isKoerier(BezorgguhtjeEntity guhtje) {
        return guhtje.getUUID().equals(koerierId);
    }

    /** Its guhtje, when it is in a loaded chunk right now. */
    @Nullable
    public BezorgguhtjeEntity koerier() {
        if (koerierId == null || !(level instanceof ServerLevel sl)) {
            return null;
        }
        return sl.getEntity(koerierId) instanceof BezorgguhtjeEntity k && k.isAlive() && worldPosition.equals(k.station()) ? k : null;
    }

    /** No guhtje in sight: wait a little (it may still be loading, or be in a chunk that is not loaded), then make a new one. */
    private void zoekOfMaak(ServerLevel sl) {
        thuis = true;
        if (koerierId == null) {
            maak(sl);
            return;
        }
        boolean daarGeladen = laatstePlek == null || (sl.isLoaded(laatstePlek) && sl.areEntitiesLoaded(ChunkPos.pack(laatstePlek)));
        if (daarGeladen) {
            weg = 0;
            if (++vermist >= VERMIST_TICKS) {
                maak(sl);
            }
        } else if (++weg >= WEG_TICKS) {
            maak(sl);   // (the old one poofs when its chunk loads: this station does not know it any more)
        }
    }

    /** A new guhtje at the dock (the one before it, if any, is no longer ours). */
    private void maak(ServerLevel sl) {
        BezorgguhtjeEntity k = TechbezorgFeature.BEZORGGUHTJE.get().create(sl, EntitySpawnReason.TRIGGERED);
        if (k == null) {
            return;
        }
        BlockPos plek = dok(sl);
        Direction voor = getBlockState().getValue(MachineBlock.FACING);
        k.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, voor.toYRot(), 0);
        k.setYHeadRot(voor.toYRot());
        k.zetStation(worldPosition);
        k.zetLading(gevuld());
        koerierId = k.getUUID();
        sl.addFreshEntity(k);
        sl.sendParticles(ParticleTypes.POOF, k.getX(), k.getY() + 0.4, k.getZ(), 8, 0.2, 0.25, 0.2, 0.02);
        vermist = 0;
        weg = 0;
        laatstePlek = plek;
        ritDoel = null;
        doel = -1;
        zetFase(heeftKracht() ? Fase.RUST : Fase.SLAAPT);
        wacht = RUST_TICKS;
        setChanged();
    }

    /**
     * Where the guhtje parks: in front of the station, else at a side, else behind it, else on top (a free block with
     * something to stand on). Looked up again every second.
     */
    public BlockPos dok(ServerLevel sl) {
        if (dok != null && sl.getGameTime() - dokTik < 20 && sl.getGameTime() >= dokTik) {
            return dok;
        }
        Direction voor = getBlockState().getValue(MachineBlock.FACING);
        BlockPos gekozen = worldPosition.above();
        for (Direction kant : new Direction[] {voor, voor.getClockWise(), voor.getCounterClockWise(), voor.getOpposite()}) {
            BlockPos plek = worldPosition.relative(kant);
            if (vrij(sl, plek)) {
                gekozen = plek;
                break;
            }
        }
        dok = gekozen;
        dokTik = sl.getGameTime();
        return gekozen;
    }

    private static boolean vrij(ServerLevel sl, BlockPos plek) {
        BlockState hier = sl.getBlockState(plek);
        BlockPos onder = plek.below();
        return hier.getCollisionShape(sl, plek).isEmpty() && hier.getFluidState().isEmpty()
                && !sl.getBlockState(onder).getCollisionShape(sl, onder).isEmpty();
    }

    // =====================================================================================================================
    // every tick
    // =====================================================================================================================

    /** The station's own tick (after the machine's): looks after the guhtje and runs the round. */
    void altijd() {
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        if (!begonnen) {
            begonnen = true;
            adopteer(sl, false);
        }
        if (sl.getGameTime() % 40 == 0) {
            controleerHaltes(sl);
        }
        BezorgguhtjeEntity k = koerier();
        if (k == null) {
            zoekOfMaak(sl);
            return;
        }
        vermist = 0;
        weg = 0;
        laatstePlek = k.blockPosition();
        k.zetLading(gevuld());
        if (vast && rugzakLeeg()) {
            vast = false;
        }
        BlockPos huis = dok(sl);
        if (!heeftKracht()) {
            // no vadskracht: home, and to sleep
            if (fase != Fase.SLAAPT) {
                zetFase(Fase.SLAAPT);
            }
            thuis = k.isBij(huis);
            if (thuis) {
                k.sta();
                k.zetSlaapt(true);
            } else {
                k.zetSlaapt(false);
                rijd(k, huis);
            }
            return;
        }
        k.zetSlaapt(false);
        switch (fase) {
            case SLAAPT -> {
                zetFase(Fase.RUST);
                wacht = 20;
                k.speel("blij");
            }
            case RUST -> {
                thuis = k.isBij(huis);
                if (!thuis) {
                    rijd(k, huis);
                    break;
                }
                k.sta();
                if (--wacht > 0) {
                    break;
                }
                int eerste = volgende(sl, -1);
                if (eerste >= 0) {
                    doel = eerste;
                    geleverd = false;
                    thuis = false;
                    zetFase(Fase.RIJDT);
                    k.bel();
                } else {
                    wacht = RUST_TICKS;
                }
            }
            case RIJDT -> {
                thuis = false;
                HaltepaaltjeBlockEntity h = halte(sl, doel);
                if (h == null) {
                    verder(sl);
                } else if (k.isBij(h.getBlockPos())) {
                    k.sta();
                    k.getLookControl().setLookAt(h.kist().getX() + 0.5, h.kist().getY() + 0.5, h.kist().getZ() + 0.5);
                    k.speel("laad");
                    k.playSound(TechbezorgFeature.RITS.get(), 0.7f, 1f);
                    wacht = LAAD_TICKS;
                    zetFase(Fase.LAADT);
                } else {
                    rijd(k, h.getBlockPos());
                }
            }
            case LAADT -> {
                k.sta();
                if (--wacht == LAAD_TICKS / 2) {
                    wissel(sl, doel);
                }
                if (wacht <= 0) {
                    verder(sl);
                }
            }
            case NAAR_HUIS -> {
                if (k.isBij(huis)) {
                    k.sta();
                    thuis = true;
                    vast = !rugzakLeeg();
                    wacht = geleverd ? RUST_TICKS : 2 * RUST_TICKS;
                    zetFase(Fase.RUST);
                } else {
                    thuis = false;
                    rijd(k, huis);
                }
            }
            case NAAR_SPELER -> {
                thuis = false;
                ServerPlayer speler = roeper(sl);
                if (speler == null || ++roepTicks > ROEP_TICKS) {
                    naarHuis();
                } else if (k.distanceToSqr(speler) <= 2.5 * 2.5) {
                    k.sta();
                    k.speel("zwaai");
                    k.bel();
                    wacht = WACHT_BIJ_SPELER;
                    zetFase(Fase.BIJ_SPELER);
                    speler.openMenu(this, worldPosition);
                } else {
                    rijd(k, speler.blockPosition());
                }
            }
            case BIJ_SPELER -> {
                ServerPlayer speler = roeper(sl);
                k.sta();
                boolean kijkt = speler != null && speler.containerMenu instanceof StepstationMenu menu && menu.station() == this;
                if (speler != null) {
                    k.getLookControl().setLookAt(speler, 30f, 30f);
                }
                wacht--;
                if (speler == null || k.distanceToSqr(speler) > 8 * 8 || (!kijkt && (wacht <= 0 || wacht < WACHT_BIJ_SPELER - 40))) {
                    naarHuis();
                }
            }
        }
    }

    private void zetFase(Fase nieuw) {
        if (fase != nieuw) {
            fase = nieuw;
            ritDoel = null;
            setChanged();
        }
    }

    /** After stop {@link #doel}: on to the next stop with work, or home. */
    private void verder(ServerLevel sl) {
        int next = volgende(sl, doel);
        if (next >= 0) {
            doel = next;
            zetFase(Fase.RIJDT);
            ritDoel = null;
        } else {
            doel = -1;
            zetFase(Fase.NAAR_HUIS);
        }
    }

    /** Keeps the guhtje riding to this block, and makes it hop there when it does not get any closer (or takes far too long). */
    private void rijd(BezorgguhtjeEntity k, BlockPos naar) {
        if (!naar.equals(ritDoel)) {
            ritDoel = naar.immutable();
            rit = 0;
            stil = 0;
            beste = Double.MAX_VALUE;
        }
        rit++;
        double afstand = k.afstand(naar);
        if (afstand < beste - 0.5) {
            beste = afstand;
            stil = 0;
        } else {
            stil++;
        }
        if (stil >= VAST_TICKS || rit >= MAX_RIT_TICKS) {
            k.hop(naar);
            ritDoel = null;
            return;
        }
        k.rijdNaar(naar);
    }

    // =====================================================================================================================
    // the work at the stops
    // =====================================================================================================================

    /** The first stop after stop {@code na} where there is something to do right now, or -1. */
    int volgende(ServerLevel sl, int na) {
        for (int i = Math.max(0, na + 1); i < haltes.size(); i++) {
            if (nuttig(sl, i)) {
                return i;
            }
        }
        return -1;
    }

    /** Is there work at stop i: something to take that somebody wants and that fits, or something in the backpack that fits there? */
    boolean nuttig(ServerLevel sl, int i) {
        HaltepaaltjeBlockEntity h = halte(sl, i);
        if (h == null) {
            return false;
        }
        return h.ophalen() ? haalOp(sl, h, false) > 0 : kanAfleveren(h);
    }

    /** Moves the items at stop i: out of the chest into the backpack, or out of the backpack into the chest. */
    void wissel(ServerLevel sl, int i) {
        HaltepaaltjeBlockEntity h = halte(sl, i);
        if (h == null) {
            return;
        }
        if (h.ophalen()) {
            haalOp(sl, h, true);
            return;
        }
        ResourceHandler<ItemResource> in = h.inKant();
        if (in == null) {
            return;
        }
        int n = Kisten.verplaats(vakken(), in, h::past, VEEL);
        if (n > 0) {
            geleverd = true;
            gebracht += n;
            setChanged();
            sl.playSound(null, h.getBlockPos(), TechbezorgFeature.RITS.get(), SoundSource.BLOCKS, 0.6f, 1.3f);
            beloon(sl);
        }
    }

    /**
     * Takes at an "ophalen" stop what its filter allows, what the "afleveren" stops of this round can hold right now
     * ({@link #gevraagd}, minus what is in the backpack already) and what fits in the backpack. Not {@code echt}: only asks
     * whether there is anything (returns 1 or 0). Returns how many items moved.
     */
    private int haalOp(ServerLevel sl, HaltepaaltjeBlockEntity h, boolean echt) {
        ResourceHandler<ItemResource> uit = h.uitKant();
        if (uit == null) {
            return 0;
        }
        int totaal = 0;
        Set<ItemResource> gezien = new HashSet<>();
        for (int vak = 0; vak < uit.size(); vak++) {
            ItemResource soort = uit.getResource(vak);
            if (soort.isEmpty() || !gezien.add(soort)) {
                continue;
            }
            ItemStack een = soort.toStack(1);
            if (!h.past(een)) {
                continue;
            }
            Predicate<ItemStack> deze = stack -> ItemStack.isSameItemSameComponents(stack, een);
            int wil = gevraagd(sl, een) - (int) Math.min(VEEL, Kisten.tel(vakken(), deze));
            if (wil <= 0) {
                continue;
            }
            if (!echt) {
                if (!Kisten.neem(uit, deze, 1, true).isEmpty() && Kisten.past(vakken(), een)) {
                    return 1;
                }
                continue;
            }
            totaal += Kisten.verplaats(uit, vakken(), deze, wil);
        }
        if (totaal > 0) {
            setChanged();
        }
        return totaal;
    }

    /** How many of this item the "afleveren" stops of this round can take in right now (at most a full backpack). */
    private int gevraagd(ServerLevel sl, ItemStack een) {
        int n = 0;
        for (int i = 0; i < haltes.size() && n < VEEL; i++) {
            HaltepaaltjeBlockEntity h = halte(sl, i);
            if (h == null || h.ophalen() || !h.past(een)) {
                continue;
            }
            ResourceHandler<ItemResource> in = h.inKant();
            if (in != null) {
                n += VEEL - Kisten.stop(in, een.copyWithCount(VEEL), true).getCount();
            }
        }
        return Math.min(n, VEEL);
    }

    /** Is there something in the backpack that this "afleveren" stop wants and has room for? */
    private boolean kanAfleveren(HaltepaaltjeBlockEntity h) {
        ResourceHandler<ItemResource> in = h.inKant();
        if (in == null) {
            return false;
        }
        for (ItemStack stack : vakken().copyToList()) {
            if (!stack.isEmpty() && h.past(stack) && Kisten.past(in, stack.copyWithCount(1))) {
                return true;
            }
        }
        return false;
    }

    /** The owner (when online) gets the advancements of a first delivery. */
    private void beloon(ServerLevel sl) {
        if (eigenaar() != null && sl.getPlayerByUUID(eigenaar()) instanceof ServerPlayer speler) {
            GuhAdvancements.grant(speler, "tech_bezorg_bezorgd");
            GidsFeature.grant(speler, "techniek/tech_bezorg_bezorgd");
        }
    }

    /** How many stacks are in the backpack. */
    public int gevuld() {
        int n = 0;
        for (int i = 0; i < vakken().size(); i++) {
            if (!vakken().getResource(i).isEmpty()) {
                n++;
            }
        }
        return n;
    }

    public boolean rugzakLeeg() {
        return gevuld() == 0;
    }

    /** Items delivered by this station in total. */
    public long gebracht() {
        return gebracht;
    }

    // =====================================================================================================================
    // the whistle, the screen
    // =====================================================================================================================

    public Fase fase() {
        return fase;
    }

    /** The stop it is riding to or busy at (0-based), or -1. */
    public int doel() {
        return fase == Fase.RIJDT || fase == Fase.LAADT ? doel : -1;
    }

    /** May a player reach into the backpack now: the guhtje is home, or it was whistled and stands next to them. */
    public boolean rugzakOpen() {
        return fase == Fase.BIJ_SPELER || (thuis && (fase == Fase.RUST || fase == Fase.SLAAPT));
    }

    /**
     * The whistle: the guhtje breaks off what it does and rides to this player with its backpack (it opens when it arrives).
     * False: it sleeps (no vadskracht) or is not around.
     */
    public boolean roep(ServerPlayer speler) {
        if (!heeftKracht() || koerier() == null) {
            return false;
        }
        roeper = speler.getUUID();
        roepTicks = 0;
        doel = -1;
        zetFase(Fase.NAAR_SPELER);
        ritDoel = null;
        return true;
    }

    /** Home, and a fresh round (the button "Naar huis!", the whistle while sneaking). */
    public void naarHuis() {
        roeper = null;
        doel = -1;
        if (fase != Fase.SLAAPT && fase != Fase.RUST) {
            zetFase(Fase.NAAR_HUIS);
        }
    }

    @Nullable
    private ServerPlayer roeper(ServerLevel sl) {
        if (roeper == null || !(sl.getPlayerByUUID(roeper) instanceof ServerPlayer speler)) {
            return null;
        }
        return speler.isAlive() && !speler.isSpectator() && Bezorgnet.binnenBereik(worldPosition, speler.blockPosition()) ? speler : null;
    }

    /** One line: what the guhtje is doing. */
    public Component standTekst() {
        if (koerier() == null) {
            return Component.translatable("gui.guhs.techbezorg.stand.zoek");
        }
        return switch (fase) {
            case RIJDT -> Component.translatable("gui.guhs.techbezorg.stand.rijdt", doel + 1, haltes.size());
            case LAADT -> Component.translatable("gui.guhs.techbezorg.stand.laadt", doel + 1);
            default -> Component.translatable("gui.guhs.techbezorg.stand." + fase.id());
        };
    }

    /** The hover readout: what the guhtje does, the backpack, and what is wrong. */
    @Override
    public void vadsRegels(Consumer<Component> regels) {
        regels.accept(standTekst());
        regels.accept(Component.translatable("gui.guhs.techbezorg.hover.rugzak", gevuld()));
        if (haltes.isEmpty()) {
            regels.accept(Component.translatable("gui.guhs.techbezorg.hover.geen_haltes").withStyle(ChatFormatting.YELLOW));
        } else if (!heeftAfleverHalte(null)) {
            regels.accept(Component.translatable("gui.guhs.techbezorg.hover.geen_aflever").withStyle(ChatFormatting.YELLOW));
        }
        if (vast) {
            regels.accept(Component.translatable("gui.guhs.techbezorg.hover.vol").withStyle(ChatFormatting.YELLOW));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.guhs.stepstation");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new StepstationMenu(id, inventory, this);
    }

    // =====================================================================================================================
    // loading, removal, saving
    // =====================================================================================================================

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            Bezorgnet.stationErbij(level, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            Bezorgnet.stationWeg(level, worldPosition);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide()) {
            Bezorgnet.stationErbij(level, worldPosition);
        }
    }

    /**
     * Broken: the backpack's items fall out (the machine does that), the guhtje poofs away, and the poles are loose again
     * (they wait for the next Stepstation a player places within reach, or for "Ander station").
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (!(level instanceof ServerLevel sl)) {
            return;
        }
        Bezorgnet.stationWeg(sl, pos);
        BezorgguhtjeEntity k = koerier();
        if (k != null) {
            k.verdwijn();
        }
        koerierId = null;
        List<BlockPos> los = new ArrayList<>(haltes);
        haltes.clear();
        for (BlockPos halte : los) {
            if (sl.isLoaded(halte) && sl.getBlockEntity(halte) instanceof HaltepaaltjeBlockEntity h && pos.equals(h.station())) {
                h.zetStation(null);
            }
        }
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        uit.store("Haltes", BlockPos.CODEC.listOf(), new ArrayList<>(haltes));
        uit.putString("Fase", fase.id());
        uit.putInt("Doel", doel);
        uit.putInt("Wacht", wacht);
        uit.storeNullable("Koerier", UUIDUtil.CODEC, koerierId);
        uit.storeNullable("LaatstePlek", BlockPos.CODEC, laatstePlek);
        uit.storeNullable("Roeper", UUIDUtil.CODEC, roeper);
        uit.putBoolean("Vast", vast);
        uit.putBoolean("Geleverd", geleverd);
        uit.putBoolean("Thuis", thuis);
        uit.putLong("Gebracht", gebracht);
    }

    @Override
    protected void laden(ValueInput in) {
        haltes.clear();
        in.read("Haltes", BlockPos.CODEC.listOf()).ifPresent(lijst -> {
            for (BlockPos pos : lijst) {
                if (haltes.size() < Bezorgnet.MAX_HALTES && !haltes.contains(pos)) {
                    haltes.add(pos.immutable());
                }
            }
        });
        String naam = in.getStringOr("Fase", Fase.SLAAPT.id());
        fase = Fase.SLAAPT;
        for (Fase f : Fase.values()) {
            if (f.id().equals(naam)) {
                fase = f;
            }
        }
        doel = in.getIntOr("Doel", -1);
        wacht = in.getIntOr("Wacht", 0);
        koerierId = in.read("Koerier", UUIDUtil.CODEC).orElse(null);
        laatstePlek = in.read("LaatstePlek", BlockPos.CODEC).orElse(null);
        roeper = in.read("Roeper", UUIDUtil.CODEC).orElse(null);
        vast = in.getBooleanOr("Vast", false);
        geleverd = in.getBooleanOr("Geleverd", false);
        thuis = in.getBooleanOr("Thuis", true);
        gebracht = in.getLongOr("Gebracht", 0L);
    }
}
