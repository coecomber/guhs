package nl.juiced.guhs.feature.mijnhuisje;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.band.PlekSoort;
import nl.juiced.guhs.feature.bank.BankAdressen;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.BankSleutelItem;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.BankStorage;

/**
 * 1.7.0 (ronde 37-40): the Mijn-guhhuisje. 0-4 of the owner's tamed guhs live in it ({@link #trekIn}) and slowly mine out
 * the {@link MijnGebied} under it, one block at a time:
 * <ul>
 *   <li><b>Speed</b>: every tick each working guh (loaded, within {@value #BEREIK} blocks, not sitting, not a baby) adds
 *       {@value #VRIJ} point, or {@value #GEVOERD} while it is fed (kaasknabbels in the first slot, one per
 *       {@value #BLOKKEN_PER_KNABBEL} blocks); every {@value #PUNTEN_PER_BLOK} points one block is mined. So per guh one
 *       block per 20 s for free, one per 5 s with kaasknabbels.</li>
 *   <li><b>Safety</b> ({@link #magMijnen}): only natural terrain (block tag {@code guhs:mijnhuisje_mijnbaar}), nothing with a
 *       block entity, nothing unbreakable, nothing of a structure or a protected guh building, never a block next to a
 *       fluid (it stays as a seal), never one with sand or gravel on top that is not mined itself, never below y
 *       {@value MijnGebied#MIN_Y}. The huisje's own column stays (the pilaar), a ladder goes down along it and a lampje glows
 *       in it every few layers and along the walls.</li>
 *   <li><b>Where it goes</b> ({@link #lever}): plain ground blocks into the bag of a resident with a Rugzakzadel (endless),
 *       else into the Bank Guh of the Banksleutel in the second slot, else into a chest touching the huisje; everything
 *       else into the chest first, then the bank. What fits nowhere waits inside ({@link #wacht}) and the work pauses
 *       until there is room. Every few seconds the Rugzak goes to the bank.</li>
 *   <li><b>Load</b>: at most one block per tick (and only when the points are there), a search of at most
 *       {@value #SCAN} positions, the area's chunks must be loaded.</li>
 * </ul>
 */
public class MijnhuisjeBlockEntity extends BlockEntity implements MenuProvider {
    public static final int VAK_KNABBELS = 0, VAK_SLEUTEL = 1, VAKKEN = 2;
    public static final int PUNTEN_PER_BLOK = 400, VRIJ = 1, GEVOERD = 4, BLOKKEN_PER_KNABBEL = 20;
    public static final int MAX_BEWONERS = 4;
    /** A resident further away than this doesn't work; further than {@link #WEG} it moves out (taken along, called away). */
    public static final int BEREIK = 48, WEG = 96;
    /** At most this many positions are looked at per tick. */
    static final int SCAN = 512;
    /** How long "laat zien" shows the outline (ticks). */
    static final int TOON_TICKS = 200;
    /** The guh's persistent data: where its Mijn-guhhuisje stands, in which dimension, and its own "teleport to me" setting. */
    public static final String THUIS = "guhs_mijnhuisje", DIM = "guhs_mijnhuisje_dim", TP_OUD = "guhs_mijnhuisje_tp";
    /** (tests) the lowest y instead of {@link MijnGebied#MIN_Y} (the test world lies below y 10). */
    @Nullable
    public static Integer testMinY;

    /** The status shown in the menu. */
    public static final int GEEN_GUHS = 0, BEZIG = 1, VOL = 2, KLAAR = 3, NIEMAND = 4, UIT = 5, GEEN_OPSLAG = 6, WACHT = 7, TE_DIEP = 8;

    private static final ItemStack GEREEDSCHAP = new ItemStack(Items.NETHERITE_PICKAXE);
    private static final int ROZE = 0xFF6FB5;

    final SimpleContainer vakken = new SimpleContainer(VAKKEN) {
        @Override
        public void setChanged() {
            super.setChanged();
            MijnhuisjeBlockEntity.this.setChanged();
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return magInVak(slot, stack);
        }
    };
    /** What was mined but fits nowhere yet. */
    final NonNullList<ItemStack> wacht = NonNullList.withSize(27, ItemStack.EMPTY);

    @Nullable
    private UUID eigenaar;
    private String eigenaarNaam = "";
    private final List<UUID> bewoners = new ArrayList<>();
    private final Map<UUID, String> namen = new LinkedHashMap<>();
    private int rx = 5, rz = 5, bodem = Integer.MIN_VALUE;
    private boolean aan = true;
    /** Where the work is: layer and index ({@link MijnGebied#plek}); done when the whole area is mined. */
    private int laagY = Integer.MIN_VALUE, index;
    private boolean klaar;
    private int punten, portie, gemijnd;
    private long toonTot;

    // (not saved)
    private int tik, status, werkers;
    private final List<GuhEntity> rugzakGuhs = new ArrayList<>();
    @Nullable
    private BlockPos werkplek;
    private long werkplekTijd, wachtTot;
    /** (every second) is there a chest touching it, a bank behind the key? */
    private boolean heeftKist, heeftBank;

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int i) {
            return switch (i) {
                case 0 -> rx;
                case 1 -> rz;
                case 2 -> gebied().bodem();
                case 3 -> status;
                case 4 -> bewoners.size();
                case 5 -> werkers;
                case 6 -> Math.min(gemijnd, Short.MAX_VALUE);
                case 7 -> (int) Math.min(rugzakTotaal(), Short.MAX_VALUE);
                case 8 -> gebied().boven();
                case 9 -> (heeftKist ? 1 : 0) | (heeftBank ? 2 : 0) | (!rugzakGuhs.isEmpty() ? 4 : 0) | (aan ? 8 : 0) | (gevoerd() ? 16 : 0);
                case 10 -> minY();
                case 11 -> laagY == Integer.MIN_VALUE ? gebied().boven() : laagY;
                default -> 0;
            };
        }

        @Override
        public void set(int i, int value) {
        }

        @Override
        public int getCount() {
            return MijnhuisjeMenu.DATA;
        }
    };

    public MijnhuisjeBlockEntity(BlockPos pos, BlockState state) {
        super(MijnhuisjeFeature.BE.get(), pos, state);
    }

    // =====================================================================================================================
    // settings
    // =====================================================================================================================

    public Direction voor() {
        BlockState s = getBlockState();
        return s.hasProperty(MijnhuisjeBlock.FACING) ? s.getValue(MijnhuisjeBlock.FACING) : Direction.NORTH;
    }

    static int minY() {
        return testMinY != null ? testMinY : MijnGebied.MIN_Y;
    }

    public MijnGebied gebied() {
        int b = bodem == Integer.MIN_VALUE ? worldPosition.getY() - 13 : bodem;
        return new MijnGebied(worldPosition, voor(), rx, rz, b, minY());
    }

    /** New settings: the work starts again at the top (what is mined already is passed quickly). */
    public void stel(int rx, int rz, int bodem) {
        int r1 = Math.clamp(rx, 0, MijnGebied.MAX_ZIJ), r2 = Math.clamp(rz, 0, MijnGebied.MAX_ZIJ);
        int b = Math.clamp(bodem, minY(), Math.max(minY(), worldPosition.getY() - 1));
        if (r1 != this.rx || r2 != this.rz || b != this.bodem) {
            this.rx = r1;
            this.rz = r2;
            this.bodem = b;
            opnieuw();
        }
    }

    void opnieuw() {
        laagY = Integer.MIN_VALUE;
        index = 0;
        klaar = false;
        setChanged();
    }

    public boolean aan() {
        return aan;
    }

    public void zetAan(boolean aan) {
        this.aan = aan;
        setChanged();
    }

    @Nullable
    public UUID eigenaar() {
        return eigenaar;
    }

    public String eigenaarNaam() {
        return eigenaarNaam;
    }

    void zetEigenaar(Player p) {
        eigenaar = p.getUUID();
        eigenaarNaam = p.getGameProfile().name();
        setChanged();
    }

    /** The owner, or an op (an unknown owner: anybody, the first one claims it). */
    public boolean mag(Player p) {
        return eigenaar == null || eigenaar.equals(p.getUUID()) || p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    public int status() {
        return status;
    }

    public int gemijnd() {
        return gemijnd;
    }

    public boolean klaar() {
        return klaar;
    }

    @Nullable
    public BlockPos werkplek() {
        return werkplek != null && level != null && level.getGameTime() - werkplekTijd < 600 ? werkplek : null;
    }

    static boolean magInVak(int slot, ItemStack stack) {
        return slot == VAK_KNABBELS ? stack.is(ModItems.KAAS_KNABBELS.get()) : stack.is(BankFeature.BANK_SLEUTEL.get());
    }

    // =====================================================================================================================
    // residents
    // =====================================================================================================================

    public List<UUID> bewoners() {
        return List.copyOf(bewoners);
    }

    public String naamVan(UUID id) {
        return namen.getOrDefault(id, "Guh");
    }

    public boolean isVol() {
        return bewoners.size() >= MAX_BEWONERS;
    }

    /** Can this guh move in here (one of the owner's tamed guhs, not grown-up is fine, room left)? */
    public boolean kanErbij(Entity e) {
        return e instanceof GuhEntity g && Band.isBandGuh(g) && eigenaar != null && eigenaar.equals(g.getOwnerUUID())
                && (!isVol() || bewoners.contains(Band.id(g)));
    }

    /** Moves one of the owner's guhs in (out of a Guhhuisje or another Mijn-guhhuisje). False: not theirs, or full. */
    public boolean trekIn(Entity e) {
        if (!(level instanceof ServerLevel sl) || !kanErbij(e) || !(e instanceof GuhEntity g)) {
            return false;
        }
        UUID id = Band.id(g);
        if (bewoners.contains(id)) {
            return true;
        }
        if (Huisjes.isBewoner(g)) {
            Huisjes.trekUit(g);
        }
        MijnhuisjeBlockEntity oud = thuisVan(g);
        if (oud != null && oud != this) {
            oud.vergeet(id);
        }
        CompoundTag p = g.getPersistentData();
        if (!p.contains(THUIS)) {
            p.putBoolean(TP_OUD, g.isTeleportEnabled());
        }
        p.putLong(THUIS, worldPosition.asLong());
        p.putString(DIM, sl.dimension().identifier().toString());
        g.setTeleportEnabled(false);               // (it lives here now: it doesn't pop over to its owner; back on when it moves out)
        bewoners.add(id);
        namen.put(id, g.getName().getString());
        GuhVolger.zet(g, PlekSoort.HUISJE, Component.translatable("block.guhs.mijn_guhhuisje"));
        sl.playSound(null, worldPosition, nl.juiced.guhs.feature.huisje.HuisjeFeature.DEUR_GELUID.get(), SoundSource.NEUTRAL, 0.8f, 1.1f);
        setChanged();
        return true;
    }

    /** A resident moves out (by band id; a loaded one is set free at once, an unloaded one when it is seen next). */
    public void trekUit(UUID id) {
        if (level instanceof ServerLevel sl && sl.getEntity(id) instanceof GuhEntity g && isHier(g)) {
            ontruim(g);
        }
        vergeet(id);
    }

    private void vergeet(UUID id) {
        bewoners.remove(id);
        namen.remove(id);
        setChanged();
    }

    /** The guh no longer lives in a Mijn-guhhuisje: its own teleport setting back, "Waar is mijn guh?" says the world. */
    public static void ontruim(Entity e) {
        CompoundTag p = e.getPersistentData();
        if (!p.contains(THUIS)) {
            return;
        }
        boolean tp = p.getBooleanOr(TP_OUD, true);
        p.remove(THUIS);
        p.remove(DIM);
        p.remove(TP_OUD);
        if (e instanceof GuhEntity g) {
            g.setTeleportEnabled(tp);
            GuhVolger.zet(g, PlekSoort.WERELD, "");
        }
    }

    /** Lives in a Mijn-guhhuisje (its persistent data says so). */
    public static boolean isBewoner(Entity e) {
        return e.getPersistentData().contains(THUIS);
    }

    /** Does this guh's data point at this very huisje? */
    boolean isHier(Entity e) {
        CompoundTag p = e.getPersistentData();
        return level != null && p.getLongOr(THUIS, Long.MIN_VALUE) == worldPosition.asLong()
                && level.dimension().identifier().toString().equals(p.getStringOr(DIM, ""));
    }

    /** The loaded Mijn-guhhuisje this guh lives in (it lists the guh), or null (also when its chunk isn't loaded). */
    @Nullable
    public static MijnhuisjeBlockEntity thuisVan(Entity e) {
        CompoundTag p = e.getPersistentData();
        if (!p.contains(THUIS) || !(e.level() instanceof ServerLevel sl)) {
            return null;
        }
        Identifier dim = Identifier.tryParse(p.getStringOr(DIM, ""));
        if (dim == null || !dim.equals(sl.dimension().identifier())) {
            return null;
        }
        BlockPos pos = BlockPos.of(p.getLongOr(THUIS, 0L));
        if (!sl.isLoaded(pos)) {
            return null;
        }
        return sl.getBlockEntity(pos) instanceof MijnhuisjeBlockEntity be && be.bewoners.contains(Band.id(e)) ? be : null;
    }

    /** Is this guh's home known to be gone (its chunk is loaded and nothing there lists it)? */
    static boolean thuisWeg(Entity e) {
        CompoundTag p = e.getPersistentData();
        if (!p.contains(THUIS) || !(e.level() instanceof ServerLevel sl)) {
            return false;
        }
        Identifier dim = Identifier.tryParse(p.getStringOr(DIM, ""));
        if (dim == null || !dim.equals(sl.dimension().identifier())) {
            return false;
        }
        BlockPos pos = BlockPos.of(p.getLongOr(THUIS, 0L));
        return sl.isLoaded(pos) && thuisVan(e) == null;
    }

    /** Was this resident taken far from its home (another dimension, or further than {@link #WEG} blocks)? */
    static boolean teVer(Entity e) {
        CompoundTag p = e.getPersistentData();
        if (!p.contains(THUIS)) {
            return false;
        }
        if (!e.level().dimension().identifier().toString().equals(p.getStringOr(DIM, ""))) {
            return true;
        }
        return e.distanceToSqr(BlockPos.of(p.getLongOr(THUIS, 0L)).getCenter()) > (double) WEG * WEG;
    }

    /** Every second: who is here to work, who moved away, who wears a Rugzakzadel. */
    private void verversWerkers(ServerLevel sl) {
        werkers = 0;
        rugzakGuhs.clear();
        Vec3 m = Vec3.atCenterOf(worldPosition);
        for (UUID id : List.copyOf(bewoners)) {
            Entity e = sl.getEntity(id);
            if (!(e instanceof GuhEntity g) || !g.isAlive()) {
                continue;
            }
            if (!isHier(g) || Huisjes.isBewoner(g)) {
                vergeet(id);                               // (it moved: another huisje has it now)
                continue;
            }
            namen.put(id, g.getName().getString());
            double d = g.distanceToSqr(m);
            if (d > (double) WEG * WEG) {
                ontruim(g);                                // (taken far away, or called with "Roep naar mij")
                vergeet(id);
                continue;
            }
            if (Rugzak.draagt(g)) {
                rugzakGuhs.add(g);
            }
            if (d <= (double) BEREIK * BEREIK && !g.isOrderedToSit() && !g.isBaby() && !g.isPassenger()) {
                werkers++;
            }
        }
    }

    // =====================================================================================================================
    // the work
    // =====================================================================================================================

    public static void serverTick(Level level, BlockPos pos, BlockState state, MijnhuisjeBlockEntity be) {
        if (level instanceof ServerLevel sl) {
            be.tick(sl);
        }
    }

    void tick(ServerLevel sl) {
        tik++;
        if (tik % 20 == 1) {
            verversWerkers(sl);
            heeftKist = kist() != null;
            heeftBank = bank() != null;
        }
        if (toonTot > sl.getGameTime() && tik % 10 == 0) {
            toon(sl);
        }
        if (tik % 100 == 50 && !rugzakGuhs.isEmpty()) {
            BankStorage bank = bank();
            if (bank != null) {
                for (GuhEntity g : rugzakGuhs) {
                    Rugzak.naarBank(g, bank);
                }
            }
        }
        status = werkStatus(sl);
        if (status != BEZIG) {
            punten = Math.min(punten, PUNTEN_PER_BLOK);
            return;
        }
        punten = Math.min(PUNTEN_PER_BLOK, punten + werkers * (gevoerd() ? GEVOERD : VRIJ));
        if (punten >= PUNTEN_PER_BLOK) {
            BlockPos p = volgende(sl);
            if (p != null) {
                mijn(sl, p);
                punten -= PUNTEN_PER_BLOK;
            }
        }
    }

    /** (tests) one tick with the points for a block already there: what a working guh does every 20 s, at once. */
    void testStap(ServerLevel sl) {
        punten = PUNTEN_PER_BLOK;
        tick(sl);
    }

    /** (tests) the work points gathered so far. */
    int punten() {
        return punten;
    }

    private int werkStatus(ServerLevel sl) {
        if (!aan) {
            return UIT;
        }
        if (bewoners.isEmpty()) {
            return GEEN_GUHS;
        }
        if (gebied().leeg()) {
            return TE_DIEP;
        }
        if (klaar) {
            return KLAAR;
        }
        if (werkers == 0) {
            return NIEMAND;
        }
        if (!wachtLeeg()) {
            if (tik % 20 != 7 || !leegWacht(sl)) {
                return VOL;
            }
        }
        if (!heeftKist && !heeftBank && rugzakGuhs.isEmpty()) {
            return GEEN_OPSLAG;
        }
        return sl.getGameTime() < wachtTot ? WACHT : BEZIG;
    }

    /** Fed: a knabbel portion is going, or there are kaasknabbels in the slot. */
    public boolean gevoerd() {
        return portie > 0 || vakken.getItem(VAK_KNABBELS).is(ModItems.KAAS_KNABBELS.get());
    }

    /**
     * The next block to mine (the cursor moves past it), or null: none found within {@link #SCAN} positions this tick, a
     * chunk that isn't loaded (status WACHT), or the area is done. Ladders in empty spots of the ladder column and the
     * pilaar's lampjes are put on the way (they cost no work).
     */
    @Nullable
    BlockPos volgende(ServerLevel sl) {
        MijnGebied g = gebied();
        if (laagY == Integer.MIN_VALUE || laagY > g.boven()) {
            laagY = g.boven();
            index = 0;
        }
        for (int n = 0; n < SCAN; n++) {
            if (laagY < g.bodem()) {
                klaar = true;
                setChanged();
                return null;
            }
            if (index >= g.perLaag()) {
                laagY--;
                index = 0;
                continue;
            }
            BlockPos p = g.plek(laagY, index);
            if (!sl.isLoaded(p)) {
                wachtTot = sl.getGameTime() + 40;          // (that part of the area isn't loaded: a moment later)
                return null;
            }
            if (index == 0 && g.isLampLaag(laagY)) {
                pilaarLampje(sl, g, laagY);
            }
            index++;
            if (g.isPilaar(p)) {
                continue;
            }
            BlockState s = sl.getBlockState(p);
            if (g.isLadder(p) && (s.isAir() || s.canBeReplaced() && s.getFluidState().isEmpty())) {
                if (geenVloeistofErnaast(sl, p)) {
                    zetLadder(sl, g, p);
                }
                continue;
            }
            if (magMijnen(sl, p, s)) {
                return p;
            }
        }
        return null;
    }

    /** May a Mijn-guhhuisje mine this block (see the class comment: safety)? */
    public static boolean magMijnen(ServerLevel level, BlockPos p, BlockState s) {
        if (s.isAir() || !s.is(MijnhuisjeFeature.MIJNBAAR) || !s.getFluidState().isEmpty() || s.getDestroySpeed(level, p) < 0
                || s.hasBlockEntity() || p.getY() < minY()) {
            return false;
        }
        if (!geenVloeistofErnaast(level, p)) {
            return false;                                  // (it stays: it holds the water or lava back)
        }
        if (level.getBlockState(p.above()).getBlock() instanceof FallingBlock) {
            return false;                                  // (sand or gravel on top that is not mined itself would come down)
        }
        if (Protected.at(level, p) || level.structureManager().getStructureWithPieceAt(p, h -> true).isValid()) {
            return false;
        }
        return true;
    }

    static boolean geenVloeistofErnaast(Level level, BlockPos p) {
        for (Direction d : Direction.values()) {
            if (d != Direction.DOWN && !level.getFluidState(p.relative(d)).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Mines it: break effects, the drops on their way, a ladder or a lampje where they belong. */
    void mijn(ServerLevel sl, BlockPos p) {
        MijnGebied g = gebied();
        BlockState s = sl.getBlockState(p);
        List<ItemStack> drops = Block.getDrops(s, sl, p, null, null, GEREEDSCHAP);
        sl.levelEvent(2001, p, Block.getId(s));
        if (g.isLadder(p)) {
            sl.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            zetLadder(sl, g, p);
        } else if (g.isRandLampje(p)) {
            sl.setBlock(p, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), Block.UPDATE_ALL);
        } else {
            sl.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        for (ItemStack d : drops) {
            lever(sl, d);
        }
        gemijnd++;
        werkplek = p.immutable();
        werkplekTijd = sl.getGameTime();
        if (gevoerd()) {
            if (portie <= 0) {
                vakken.removeItem(VAK_KNABBELS, 1);
                portie = BLOKKEN_PER_KNABBEL;
            }
            portie--;
        }
        if (sl.getRandom().nextInt(6) == 0) {
            sl.sendParticles(nl.juiced.guhs.feature.band.BandFeature.HARTJE.get(), p.getX() + 0.5, p.getY() + 0.8, p.getZ() + 0.5, 1, 0.2, 0.1, 0.2, 0.01);
        }
        setChanged();
    }

    /** A ladder on the pilaar (the pilaar gets a block of pink wool where it is open, so the ladder has something to hang on). */
    private static void zetLadder(ServerLevel sl, MijnGebied g, BlockPos p) {
        BlockPos pilaar = new BlockPos(g.huisje().getX(), p.getY(), g.huisje().getZ());
        BlockState ps = sl.getBlockState(pilaar);
        if ((ps.isAir() || ps.canBeReplaced()) && ps.getFluidState().isEmpty()) {
            sl.setBlock(pilaar, Blocks.PINK_WOOL.defaultBlockState(), Block.UPDATE_ALL);
        }
        BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, g.voor());
        if (ladder.canSurvive(sl, p)) {
            sl.setBlock(p, ladder, Block.UPDATE_ALL);
        }
    }

    /** A lampje in the pilaar on this layer: its natural block makes way for a pink froglight (and goes the usual way). */
    private void pilaarLampje(ServerLevel sl, MijnGebied g, int y) {
        BlockPos p = new BlockPos(g.huisje().getX(), y, g.huisje().getZ());
        BlockState s = sl.getBlockState(p);
        if (s.isAir() || magMijnen(sl, p, s)) {
            if (!s.isAir()) {
                for (ItemStack d : Block.getDrops(s, sl, p, null, null, GEREEDSCHAP)) {
                    lever(sl, d);
                }
            }
            sl.setBlock(p, Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // =====================================================================================================================
    // where it goes
    // =====================================================================================================================

    /** Delivers a mined stack (see the class comment); what fits nowhere waits inside. */
    void lever(ServerLevel sl, ItemStack stack) {
        ItemStack rest = route(stack);
        if (!rest.isEmpty()) {
            inWacht(sl, rest);
        }
    }

    private ItemStack route(ItemStack stack) {
        ItemStack rest = stack.copy();
        if (Rugzak.isGrond(rest)) {
            GuhEntity r = rugzakGuh();
            if (r != null && Rugzak.stop(r, rest)) {
                return ItemStack.EMPTY;
            }
            rest = naarBank(rest);
            return naarKist(rest);
        }
        rest = naarKist(rest);
        return naarBank(rest);
    }

    @Nullable
    private GuhEntity rugzakGuh() {
        for (GuhEntity g : rugzakGuhs) {
            if (Rugzak.draagt(g)) {
                return g;
            }
        }
        return null;
    }

    private ItemStack naarBank(ItemStack stack) {
        BankStorage bank = stack.isEmpty() ? null : bank();
        return bank == null ? stack : bank.insert(stack);
    }

    private ItemStack naarKist(ItemStack stack) {
        IItemHandler kist = stack.isEmpty() ? null : kist();
        return kist == null ? stack : ItemHandlerHelper.insertItemStacked(kist, stack, false);
    }

    /** The Bank Guh of the Banksleutel in the second slot (its chunk is woken for a moment if it slept), or null. */
    @Nullable
    public BankStorage bank() {
        ItemStack sleutel = vakken.getItem(VAK_SLEUTEL);
        if (level == null || level.getServer() == null || !sleutel.is(BankFeature.BANK_SLEUTEL.get())) {
            return null;
        }
        UUID id = BankSleutelItem.bank(sleutel);
        BankGuhBlockEntity be = id == null ? null : BankAdressen.zoek(level.getServer(), id, true);
        return be == null ? null : be.getStorage();
    }

    /** A chest (any block with an item handler, not a Bank Guh) touching the huisje, or null. */
    @Nullable
    public IItemHandler kist() {
        if (level == null) {
            return null;
        }
        for (Direction d : Direction.values()) {
            BlockPos n = worldPosition.relative(d);
            if (!level.isLoaded(n)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(n);
            if (be == null || be instanceof BankGuhBlockEntity || be instanceof MijnhuisjeBlockEntity) {
                continue;
            }
            var res = level.getCapability(Capabilities.Item.BLOCK, n, d.getOpposite());
            if (res != null) {
                return IItemHandler.of(res);
            }
        }
        return null;
    }

    private boolean wachtLeeg() {
        for (ItemStack s : wacht) {
            if (!s.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Tries to deliver what waits; true when nothing waits any more. */
    private boolean leegWacht(ServerLevel sl) {
        boolean leeg = true;
        for (int i = 0; i < wacht.size(); i++) {
            ItemStack s = wacht.get(i);
            if (!s.isEmpty()) {
                ItemStack rest = route(s);
                wacht.set(i, rest);
                leeg &= rest.isEmpty();
            }
        }
        setChanged();
        return leeg;
    }

    private void inWacht(ServerLevel sl, ItemStack stack) {
        for (int i = 0; i < wacht.size() && !stack.isEmpty(); i++) {
            ItemStack w = wacht.get(i);
            if (w.isEmpty()) {
                wacht.set(i, stack.copy());
                stack = ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameComponents(w, stack) && w.getCount() < w.getMaxStackSize()) {
                int n = Math.min(stack.getCount(), w.getMaxStackSize() - w.getCount());
                w.grow(n);
                stack.shrink(n);
            }
        }
        if (!stack.isEmpty()) {                           // (never lost: out of the door)
            BlockPos d = worldPosition.relative(voor());
            Containers.dropItemStack(sl, d.getX() + 0.5, d.getY() + 0.2, d.getZ() + 0.5, stack);
        }
        setChanged();
    }

    /** What all Rugzakzadels of the residents hold together. */
    public long rugzakTotaal() {
        long n = 0;
        for (GuhEntity g : rugzakGuhs) {
            n += Rugzak.totaal(g);
        }
        return n;
    }

    /** "Rugzak legen": the bags of the residents into the chest (as much as fits). */
    public long rugzakNaarKist() {
        IItemHandler kist = kist();
        long n = 0;
        if (kist != null) {
            for (GuhEntity g : rugzakGuhs) {
                n += Rugzak.naarKist(g, kist, 54);
            }
        }
        return n;
    }

    // =====================================================================================================================
    // "laat zien"
    // =====================================================================================================================

    public void laatZien() {
        if (level != null) {
            toonTot = level.getGameTime() + TOON_TICKS;
            if (level instanceof ServerLevel sl) {
                toon(sl);
            }
        }
    }

    public boolean toont() {
        return level != null && toonTot > level.getGameTime();
    }

    private void toon(ServerLevel sl) {
        MijnGebied g = gebied();
        DustParticleOptions roze = new DustParticleOptions(ROZE, 1.5f);
        for (ServerPlayer p : sl.players()) {
            if (p.blockPosition().distSqr(worldPosition) > 96 * 96) {
                continue;
            }
            for (Vec3 v : g.rand(2)) {
                sl.sendParticles(p, roze, true, false, v.x, v.y, v.z, 1, 0, 0, 0, 0);
            }
        }
    }

    // =====================================================================================================================
    // the menu, saving
    // =====================================================================================================================

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.guhs.mijn_guhhuisje");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MijnhuisjeMenu(id, inventory, this);
    }

    /** The resident names for the menu, in order. */
    public List<String> namen() {
        List<String> uit = new ArrayList<>();
        for (UUID id : bewoners) {
            uit.add(naamVan(id));
        }
        return uit;
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (eigenaar != null) {
            tag.store("Eigenaar", UUIDUtil.CODEC, eigenaar);
        }
        tag.putString("EigenaarNaam", eigenaarNaam);
        CompoundTag m = new CompoundTag();
        m.putInt("Rx", rx);
        m.putInt("Rz", rz);
        m.putInt("Bodem", bodem);
        m.putBoolean("Aan", aan);
        m.putInt("LaagY", laagY);
        m.putInt("Index", index);
        m.putBoolean("Klaar", klaar);
        m.putInt("Punten", punten);
        m.putInt("Portie", portie);
        m.putInt("Gemijnd", gemijnd);
        net.minecraft.nbt.ListTag lijst = new net.minecraft.nbt.ListTag();
        for (UUID id : bewoners) {
            CompoundTag b = new CompoundTag();
            b.store("Id", UUIDUtil.CODEC, id);
            b.putString("Naam", naamVan(id));
            lijst.add(b);
        }
        m.put("Bewoners", lijst);
        tag.store("Mijn", CompoundTag.CODEC, m);
        ContainerHelper.saveAllItems(tag.child("Vakken"), vakken.getItems());
        ContainerHelper.saveAllItems(tag.child("Wacht"), wacht);
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        eigenaar = tag.read("Eigenaar", UUIDUtil.CODEC).orElse(null);
        eigenaarNaam = tag.getStringOr("EigenaarNaam", "");
        CompoundTag m = tag.read("Mijn", CompoundTag.CODEC).orElse(new CompoundTag());
        rx = m.getIntOr("Rx", 5);
        rz = m.getIntOr("Rz", 5);
        bodem = m.getIntOr("Bodem", Integer.MIN_VALUE);
        aan = m.getBooleanOr("Aan", true);
        laagY = m.getIntOr("LaagY", Integer.MIN_VALUE);
        index = m.getIntOr("Index", 0);
        klaar = m.getBooleanOr("Klaar", false);
        punten = m.getIntOr("Punten", 0);
        portie = m.getIntOr("Portie", 0);
        gemijnd = m.getIntOr("Gemijnd", 0);
        bewoners.clear();
        namen.clear();
        net.minecraft.nbt.ListTag lijst = m.getListOrEmpty("Bewoners");
        for (int i = 0; i < lijst.size(); i++) {
            CompoundTag b = lijst.getCompoundOrEmpty(i);
            b.read("Id", UUIDUtil.CODEC).ifPresent(id -> {
                bewoners.add(id);
                namen.put(id, b.getStringOr("Naam", "Guh"));
            });
        }
        vakken.clearContent();
        ContainerHelper.loadAllItems(tag.childOrEmpty("Vakken"), vakken.getItems());
        for (int i = 0; i < wacht.size(); i++) {
            wacht.set(i, ItemStack.EMPTY);
        }
        ContainerHelper.loadAllItems(tag.childOrEmpty("Wacht"), wacht);
    }

    /** Broken or replaced: its things drop, its residents are free again. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel sl) {
            Containers.dropContents(sl, pos, vakken);
            for (ItemStack s : wacht) {
                if (!s.isEmpty()) {
                    Containers.dropItemStack(sl, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s);
                }
            }
            for (UUID id : List.copyOf(bewoners)) {
                if (sl.getEntity(id) instanceof GuhEntity g && isHier(g)) {
                    ontruim(g);
                }
            }
            bewoners.clear();
        }
    }
}
