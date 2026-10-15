package nl.juiced.guhs.feature.techmachine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrowingPlantBlock;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.HugeFungusConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.HugeMushroomFeatureConfiguration;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.transfer.item.ItemResource;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.wereld.Bescherming;

/**
 * The Plantagebak's inside. One slot of saplings (anything {@link Plantagebakken#isZaailing}: saplings of every mod,
 * the sate- and worstzwammetjes and the nether fungi, mushrooms, azalea). While the bak has vadskracht:
 * <ol>
 *   <li>{@link Stand#LEEG}: it plants one sapling from its slot on the middle of the bed ({@link #plantPlek});</li>
 *   <li>{@link Stand#GROEIT}: after {@link #GROEITIJD} ticks of work (45 seconds) the sapling grows the way it would
 *       by itself: a real tree of its own kind. A kind that only grows with four in a square (dark oak) gets three more
 *       from the slot when a single one will not grow. While it grows the middle of the bed is real earth for a blink
 *       (some trees insist);</li>
 *   <li>{@link Stand#BOOM}: the tree stands, the face is surprised, and the bak waits until it is gone: chopped by hand,
 *       by an axe on the bak or by a chore guh ({@link #hak}: the WHOLE tree in one go, leaves and all; saplings that
 *       fall out go back into the slot until it holds {@link #RESERVE}). Then it starts again. "Gone" = no log of it
 *       stands any more: who chops the bottom log first leaves a floating trunk, and a sapling under that would never
 *       grow, so the bak waits (and {@link #hak} still takes the rest down).</li>
 * </ol>
 * The bak remembers exactly which blocks the tree put there ({@link #stam}, the rest of it in {@code kroon}), so
 * {@link #hak} never touches a log cabin next to it. Nothing grows where its owner may not build
 * ({@link Bescherming#magWijzigen}).
 */
public class PlantagebakBlockEntity extends TechBlockEntity {
    /** Ticks of work until the tree stands: 45 seconds ("within a minute"). */
    public static final int GROEITIJD = 45 * 20;
    /** How long it waits (in ticks of work) before it tries again when the tree would not grow. */
    public static final int OPNIEUW = 10 * 20;
    /** After a chop the bak keeps saplings that fell out until it holds this many (enough for a square of four, twice). */
    public static final int RESERVE = 8;
    /** How often (ticks) a tree whose bottom log is gone is looked at for other logs that still stand. */
    private static final int STAM_KIJK = 20;
    /** The box around the plant spot in which a tree may put its blocks: sideways, below, above. */
    private static final int STRAAL = 10, ONDER = 2, BOVEN = 44;

    public enum Stand { LEEG, GROEIT, BOOM }

    /** The blocks a tree put down: a palette of block states, and per block its spot and its palette index. */
    private record Boom(List<BlockState> palet, long[] plekken, int[] soort) {
        static final Boom GEEN = new Boom(List.of(), new long[0], new int[0]);
        static final Codec<Boom> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.listOf().fieldOf("palet").forGetter(Boom::palet),
                Codec.LONG_STREAM.xmap(LongStream::toArray, LongStream::of).fieldOf("plekken").forGetter(Boom::plekken),
                Codec.INT_STREAM.xmap(IntStream::toArray, IntStream::of).fieldOf("soort").forGetter(Boom::soort)
        ).apply(i, Boom::new));

        boolean klopt() {
            if (plekken.length != soort.length) {
                return false;
            }
            for (int s : soort) {
                if (s < 0 || s >= palet.size()) {
                    return false;
                }
            }
            return true;
        }
    }

    private Stand stand = Stand.LEEG;
    /** The last try to grow failed (no room, or a kind that wants four). */
    private boolean wilNiet;
    private Boom boom = Boom.GEEN;
    /** The block of the trunk at the plant spot (while a tree stands). */
    @Nullable
    private Block stamBlok;

    /** (server thread) the bak that is growing its sapling right now: {@link #groeit} helps the feature along. */
    @Nullable
    private static PlantagebakBlockEntity bezigMetGroeien;

    public PlantagebakBlockEntity(BlockPos pos, BlockState state) {
        super(TechmachineFeature.PLANTAGEBAK_BE.get(), pos, state, VadsGetallen.PLANTAGEBAK, 1);
    }

    @Override
    public MachineSoort soort() {
        return MachineSoort.PLANTAGEBAK;
    }

    @Override
    public int duur() {
        return GROEITIJD;
    }

    @Override
    protected int standCode() {
        return stand.ordinal() + (wilNiet ? 8 : 0);
    }

    @Override
    protected boolean past(int vak, ItemResource wat) {
        return Plantagebakken.isZaailing(wat.toStack());
    }

    // =====================================================================================================================
    // what others may ask and do (the chore slice: see Plantagebakken)
    // =====================================================================================================================

    public Stand stand() {
        return stand;
    }

    /** Does a tree stand on the bak that the bak grew (and that can be chopped with {@link #hak})? */
    public boolean heeftBoom() {
        return stand == Stand.BOOM;
    }

    /** The middle of the bed, one block up: where the sapling stands and the trunk begins. */
    public BlockPos plantPlek() {
        return worldPosition.relative(voor().getOpposite()).above();
    }

    /** A copy of the saplings in the slot. */
    public ItemStack voorraad() {
        return vakken().getResource(0).toStack(vakken().getAmountAsInt(0));
    }

    /** Puts saplings into the slot; the given stack shrinks by what went in. Returns how many went in. */
    public int plant(ItemStack stack) {
        if (!Plantagebakken.isZaailing(stack)) {
            return 0;
        }
        ItemResource soort = ItemResource.of(stack);
        ItemResource ligt = vakken().getResource(0);
        int al = vakken().getAmountAsInt(0);
        if (al > 0 && !ligt.equals(soort)) {
            return 0;
        }
        int n = Math.min(stack.getCount(), vakken().getCapacityAsInt(0, soort) - al);
        if (n <= 0) {
            return 0;
        }
        vakken().set(0, soort, al + n);
        stack.shrink(n);
        return n;
    }

    /** The logs of the standing tree that are still there, from the bottom up. */
    public List<BlockPos> stam() {
        List<BlockPos> uit = new ArrayList<>();
        if (stand == Stand.BOOM && level != null) {
            for (int i = 0; i < boom.plekken.length; i++) {
                BlockState toen = boom.palet.get(boom.soort[i]);
                BlockPos plek = BlockPos.of(boom.plekken[i]);
                if (isStam(toen) && level.getBlockState(plek).is(toen.getBlock())) {
                    uit.add(plek);
                }
            }
            uit.sort(Comparator.comparingInt(BlockPos::getY));
        }
        return uit;
    }

    private boolean isStam(BlockState state) {
        return state.is(BlockTags.LOGS) || (stamBlok != null && state.is(stamBlok));
    }

    /**
     * Chops the whole tree down in one go: every block the tree put there that is still what it was (logs, leaves, wart
     * blocks, vines...) is broken and its drops are collected. A bee nest that came with the tree comes along as an item
     * with its bees and its honey in it (what silk touch gives: it would hang in the air otherwise, and nobody is stung);
     * any other block with a block entity is left alone (nothing a tree of the game makes; never somebody's chest).
     * Saplings of the planted kind among them go back into the slot first. Returns the rest of the harvest (yours to
     * hand out). Nothing happens (an empty list) when no tree of the bak stands.
     */
    public List<ItemStack> hak(@Nullable Entity wie) {
        List<ItemStack> buit = new ArrayList<>();
        if (stand != Stand.BOOM || !(level instanceof ServerLevel server)) {
            return buit;
        }
        Boom om = boom;
        boom = Boom.GEEN;
        // from the top down (the list runs from the bottom up): what hangs from the tree goes before what it hangs from
        for (int i = om.plekken.length - 1; i >= 0; i--) {
            BlockPos plek = BlockPos.of(om.plekken[i]);
            if (!server.isLoaded(plek)) {
                continue;
            }
            BlockState toen = om.palet.get(om.soort[i]), nu = server.getBlockState(plek);
            if (nu.isAir() || !zelfde(toen, nu)) {
                continue;
            }
            if (nu.hasBlockEntity()) {
                if (nu.getBlock() instanceof BeehiveBlock) {
                    voegToe(buit, nest(server, plek, nu));
                    server.setBlock(plek, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
                continue;
            }
            for (ItemStack d : Block.getDrops(nu, server, plek, null, wie, ItemStack.EMPTY)) {
                voegToe(buit, d);
            }
            if (isStam(nu)) {
                server.levelEvent(2001, plek, Block.getId(nu));
            }
            server.setBlock(plek, nu.getFluidState().createLegacyBlock(), Block.UPDATE_ALL);
        }
        stamBlok = null;
        stand = Stand.LEEG;
        voortgang = 0;
        // saplings that fell out go back into the bak, until it holds a few again; the rest is harvest
        buit.removeIf(d -> {
            int ruimte = RESERVE - vakken().getAmountAsInt(0);
            if (ruimte > 0 && Plantagebakken.isZaailing(d)) {
                ItemStack terug = d.split(Math.min(ruimte, d.getCount()));
                plant(terug);
                d.grow(terug.getCount());   // (what did not fit after all: another kind lies in the slot)
            }
            return d.isEmpty();
        });
        setChanged();
        return buit;
    }

    /** The bee nest here as an item: the bees that are at home and the honey stay in it. */
    private static ItemStack nest(ServerLevel server, BlockPos plek, BlockState state) {
        ItemStack nest = new ItemStack(state.getBlock());
        BlockEntity be = server.getBlockEntity(plek);
        if (be != null) {
            nest.applyComponents(be.collectComponents());
        }
        nest.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(BeehiveBlock.HONEY_LEVEL, state.getValue(BeehiveBlock.HONEY_LEVEL)));
        return nest;
    }

    /** Is this still the block the tree put here? (The tip and the stalk of a hanging vine are two blocks of one plant.) */
    private static boolean zelfde(BlockState toen, BlockState nu) {
        return nu.is(toen.getBlock()) || (toen.getBlock() instanceof GrowingPlantBlock && nu.getBlock() instanceof GrowingPlantBlock);
    }

    private static void voegToe(List<ItemStack> lijst, ItemStack erbij) {
        if (erbij.isEmpty()) {
            return;
        }
        ItemStack rest = erbij.copy();
        for (ItemStack al : lijst) {
            if (ItemStack.isSameItemSameComponents(al, rest) && al.getCount() < al.getMaxStackSize()) {
                int n = Math.min(rest.getCount(), al.getMaxStackSize() - al.getCount());
                al.grow(n);
                rest.shrink(n);
                if (rest.isEmpty()) {
                    return;
                }
            }
        }
        lijst.add(rest);
    }

    // =====================================================================================================================
    // the work
    // =====================================================================================================================

    @Override
    protected boolean kanWerken() {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        kijk(server);
        return stand == Stand.GROEIT;
    }

    /** Surprised: the tree stands and waits to be chopped. */
    @Override
    protected boolean isVol() {
        return stand == Stand.BOOM;
    }

    /** Looks at the plant spot: is the tree gone, the sapling pulled out, a new sapling to plant? */
    private void kijk(ServerLevel server) {
        BlockPos plek = plantPlek();
        if (!server.isLoaded(plek)) {
            return;
        }
        BlockState daar = server.getBlockState(plek);
        if (stand == Stand.BOOM && (stamBlok == null || !daar.is(stamBlok))) {
            // the bottom log is gone (chopped by hand). While other logs of the tree still stand the bak waits: a new
            // sapling under a floating trunk "wil niet groeien", and an axe on the bak or a chore guh can still fell the
            // rest. Looked at once a second (a big tree is a few hundred blocks). No log left: the leaves that remain are
            // no longer the bak's business
            if (server.getGameTime() % STAM_KIJK != 0 || !stam().isEmpty()) {
                return;
            }
            boom = Boom.GEEN;
            stamBlok = null;
            stand = Stand.LEEG;
            setChanged();
        }
        if (stand == Stand.GROEIT && !isZaailingBlok(daar)) {
            stand = Stand.LEEG;
            voortgang = 0;
            wilNiet = false;
            setChanged();
        }
        if (stand == Stand.LEEG) {
            if (isZaailingBlok(daar)) {
                stand = Stand.GROEIT;   // (somebody planted one by hand: fine)
                voortgang = 0;
                setChanged();
            } else if (vakken().getAmountAsInt(0) > 0 && daar.canBeReplaced() && magHier(server, plek)) {
                BlockState zaailing = zaailingBlok(vakken().getResource(0).toStack());
                if (zaailing != null && zaailing.canSurvive(server, plek)) {
                    server.setBlock(plek, zaailing, Block.UPDATE_ALL);
                    vakken().set(0, vakken().getResource(0), vakken().getAmountAsInt(0) - 1);
                    server.playSound(null, plek, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 0.8f, 1f);
                    stand = Stand.GROEIT;
                    voortgang = 0;
                    wilNiet = false;
                }
            }
        }
    }

    @Nullable
    private static BlockState zaailingBlok(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item ? item.getBlock().defaultBlockState() : null;
    }

    private static boolean isZaailingBlok(BlockState state) {
        return !state.isAir() && Plantagebakken.isZaailing(state.getBlock());
    }

    @Override
    protected void werk() {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        BlockPos plek = plantPlek();
        if (voortgang % 12 == 0) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, plek.getX() + 0.5, plek.getY() + 0.4, plek.getZ() + 0.5, 2, 0.35, 0.25, 0.35, 0.0);
        }
        if (++voortgang < GROEITIJD) {
            return;
        }
        if (groei(server, plek)) {
            stand = Stand.BOOM;
            wilNiet = false;
            voortgang = 0;
            server.playSound(null, plek, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1f, 0.8f);
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, plek.getX() + 0.5, plek.getY() + 1.5, plek.getZ() + 0.5, 30, 1.2, 1.5, 1.2, 0.0);
            beloon("boom");
        } else {
            wilNiet = true;
            voortgang = GROEITIJD - OPNIEUW;
        }
        setChanged();
    }

    // =====================================================================================================================
    // growing
    // =====================================================================================================================

    /** Lets the sapling at the plant spot grow into its tree now; remembers the blocks of the tree. False: it would not. */
    private boolean groei(ServerLevel server, BlockPos plek) {
        BlockState zaailing = server.getBlockState(plek);
        if (!isZaailingBlok(zaailing) || !Bescherming.magWijzigen(server, plek, eigenaar())) {
            return false;
        }
        BlockState[] eerst = foto(server, plek);
        // Real earth under the sapling for the blink of an eye. Some trees look at the ground themselves (the
        // sneeuwguhspar, trees of other mods): on anything but earth they grow nowhere, or on the nearest earth they find
        // (a roof of grass above the bak...). The bed's middle is set without telling the neighbours (the bed does not
        // fall apart, the sapling does not notice) and put back afterwards, whatever the tree made of it.
        BlockPos onder = plek.below();
        BlockState bed = server.getBlockState(onder);
        boolean isBed = bed.getBlock() instanceof PlantagebakDeelBlock;
        int stil = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SKIP_ON_PLACE;
        boolean gelukt = false;
        if (isBed) {
            server.setBlock(onder, Blocks.DIRT.defaultBlockState(), stil);
        }
        try {
            for (int poging = 0; poging < 3 && !gelukt; poging++) {
                gelukt = groeiEen(server, plek);
            }
            if (!gelukt) {
                gelukt = groeiMetVier(server, plek, zaailing);
            }
        } finally {
            if (isBed) {
                server.setBlock(onder, bed, stil);
            }
        }
        if (gelukt) {
            boek(server, plek, eerst);
        }
        return gelukt;
    }

    /** One try, the way the sapling itself grows (bone meal without the luck). True: the sapling is something else now. */
    private boolean groeiEen(ServerLevel server, BlockPos plek) {
        BlockState state = server.getBlockState(plek);
        Block blok = state.getBlock();
        RandomSource rnd = server.getRandom();
        bezigMetGroeien = this;
        try {
            if (blok instanceof SaplingBlock zaailing) {
                if (state.getValue(SaplingBlock.STAGE) == 0) {
                    state = state.setValue(SaplingBlock.STAGE, 1);
                    server.setBlock(plek, state, Block.UPDATE_INVISIBLE);
                }
                zaailing.advanceTree(server, plek, state, rnd);
            } else if (blok instanceof MushroomBlock paddenstoel) {
                paddenstoel.growMushroom(server, plek, state, rnd);
            } else if (blok instanceof BonemealableBlock groeier) {
                groeier.performBonemeal(server, rnd, plek, state);
            }
        } finally {
            bezigMetGroeien = null;
        }
        return !server.getBlockState(plek).is(blok);
    }

    /** The three other spots of the square of four saplings: beside, behind, and beside-behind the plant spot. */
    private List<BlockPos> vierkant(BlockPos plek) {
        Direction opzij = voor().getClockWise(), achter = voor().getOpposite();
        return List.of(plek.relative(opzij), plek.relative(achter), plek.relative(opzij).relative(achter));
    }

    /**
     * Some kinds only grow with four in a square. When one will not grow and the slot holds three more: plant those next
     * to it and try again. They are only used up when the tree comes.
     */
    private boolean groeiMetVier(ServerLevel server, BlockPos plek, BlockState zaailing) {
        ItemStack voorraad = voorraad();
        if (voorraad.getCount() < 3 || !(voorraad.getItem() instanceof BlockItem item) || item.getBlock() != zaailing.getBlock()) {
            return false;
        }
        List<BlockPos> erbij = vierkant(plek);
        for (BlockPos p : erbij) {
            if (!server.getBlockState(p).canBeReplaced() || !zaailing.canSurvive(server, p) || !Bescherming.magWijzigen(server, p, eigenaar())) {
                return false;
            }
        }
        for (BlockPos p : erbij) {
            server.setBlock(p, zaailing, Block.UPDATE_ALL);
        }
        if (groeiEen(server, plek)) {
            vakken().set(0, vakken().getResource(0), vakken().getAmountAsInt(0) - 3);
            return true;
        }
        for (BlockPos p : erbij) {
            if (server.getBlockState(p).is(zaailing.getBlock())) {
                server.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return false;
    }

    /**
     * (game bus) A sapling is about to grow its feature. When it is this bak's sapling, the feature is told that the bed
     * is good ground: a huge fungus only grows on its own nylium and a huge mushroom on a few kinds of earth, the bed is
     * neither, so they get a copy of their feature that takes the bed.
     */
    static void groeit(BlockGrowFeatureEvent event) {
        PlantagebakBlockEntity bak = bezigMetGroeien;
        Holder<ConfiguredFeature<?, ?>> feature = event.getFeature();
        if (bak == null || feature == null || event.getLevel() != bak.level || !event.getPos().equals(bak.plantPlek())) {
            return;
        }
        FeatureConfiguration config = feature.value().config();
        if (config instanceof HugeFungusConfiguration zwam) {
            BlockState bed = event.getLevel().getBlockState(event.getPos().below());
            event.setFeature(met(feature.value(), new HugeFungusConfiguration(bed, zwam.stemState, zwam.hatState, zwam.decorState,
                    zwam.replaceableBlocks, true)));
        } else if (config instanceof HugeMushroomFeatureConfiguration paddenstoel) {
            event.setFeature(met(feature.value(), new HugeMushroomFeatureConfiguration(paddenstoel.capProvider(), paddenstoel.stemProvider(),
                    paddenstoel.foliageRadius(), BlockPredicate.alwaysTrue())));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Holder<ConfiguredFeature<?, ?>> met(ConfiguredFeature<?, ?> oud, FeatureConfiguration config) {
        return Holder.direct(new ConfiguredFeature(oud.feature(), config));
    }

    // --- which blocks are the tree: a picture before, and what changed after -------------------------------------------

    private static int breedte() {
        return 2 * STRAAL + 1;
    }

    private BlockPos hoek(BlockPos plek) {
        return plek.offset(-STRAAL, -ONDER, -STRAAL);
    }

    private BlockState[] foto(ServerLevel server, BlockPos plek) {
        int b = breedte(), h = ONDER + BOVEN + 1;
        BlockState[] foto = new BlockState[b * b * h];
        BlockPos hoek = hoek(plek);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < b; z++) {
                for (int x = 0; x < b; x++) {
                    p.set(hoek.getX() + x, hoek.getY() + y, hoek.getZ() + z);
                    foto[i++] = server.isLoaded(p) && !server.isOutsideBuildHeight(p) ? server.getBlockState(p) : null;
                }
            }
        }
        return foto;
    }

    /**
     * Could a tree have put a block of itself where this was? Only in air, in something that gives way (grass, snow,
     * water plants), in leaves, and where a sapling stood. What a tree does to the GROUND (a big spruce turns the grass
     * around it into podzol) is not the tree: {@link #hak} must never dig that up.
     */
    private static boolean wasVrij(BlockState toen) {
        return toen.isAir() || toen.canBeReplaced() || toen.is(BlockTags.LEAVES) || toen.is(BlockTags.REPLACEABLE_BY_TREES) || isZaailingBlok(toen);
    }

    /** Writes down every block that is something else than on the picture (and no air) where a tree could put one: that is the tree. */
    private void boek(ServerLevel server, BlockPos plek, BlockState[] eerst) {
        int b = breedte(), h = ONDER + BOVEN + 1;
        BlockPos hoek = hoek(plek);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        List<BlockState> palet = new ArrayList<>();
        List<Long> plekken = new ArrayList<>();
        List<Integer> soort = new ArrayList<>();
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < b; z++) {
                for (int x = 0; x < b; x++) {
                    BlockState toen = eerst[i++];
                    if (toen == null) {
                        continue;
                    }
                    p.set(hoek.getX() + x, hoek.getY() + y, hoek.getZ() + z);
                    BlockState nu = server.getBlockState(p);
                    if (nu == toen || nu.isAir() || nu.is(toen.getBlock()) || !wasVrij(toen)) {
                        continue;
                    }
                    int nr = palet.indexOf(nu);
                    if (nr < 0) {
                        nr = palet.size();
                        palet.add(nu);
                    }
                    plekken.add(p.asLong());
                    soort.add(nr);
                }
            }
        }
        boom = new Boom(List.copyOf(palet), plekken.stream().mapToLong(Long::longValue).toArray(), soort.stream().mapToInt(Integer::intValue).toArray());
        stamBlok = server.getBlockState(plek).getBlock();
    }

    // =====================================================================================================================
    // the hover readout, saving
    // =====================================================================================================================

    @Override
    public void vadsRegels(Consumer<Component> regels) {
        String k = "gui.guhs.techmachine.plantagebak.";
        switch (stand) {
            case LEEG -> regels.accept(Component.translatable(k + "leeg").withStyle(ChatFormatting.GRAY));
            case GROEIT -> {
                BlockState zaailing = level == null ? Blocks.AIR.defaultBlockState() : level.getBlockState(plantPlek());
                if (wilNiet) {
                    regels.accept(Component.translatable(k + "wil_niet", zaailing.getBlock().getName()).withStyle(ChatFormatting.GOLD));
                } else {
                    regels.accept(Component.translatable(k + "groeit", zaailing.getBlock().getName(), Math.max(1, (GROEITIJD - voortgang + 19) / 20))
                            .withStyle(ChatFormatting.GREEN));
                }
            }
            case BOOM -> regels.accept(Component.translatable(k + "boom").withStyle(ChatFormatting.AQUA));
        }
    }

    public boolean wilNiet() {
        return wilNiet;
    }

    @Override
    protected void opslaan(ValueOutput uit) {
        super.opslaan(uit);
        uit.putString("Stand", stand.name());
        uit.putBoolean("WilNiet", wilNiet);
        if (boom.plekken.length > 0) {
            uit.store("Boom", Boom.CODEC, boom);
        }
        if (stamBlok != null) {
            uit.store("Stam", net.minecraft.core.registries.BuiltInRegistries.BLOCK.byNameCodec(), stamBlok);
        }
    }

    @Override
    protected void laden(ValueInput in) {
        super.laden(in);
        try {
            stand = Stand.valueOf(in.getStringOr("Stand", Stand.LEEG.name()));
        } catch (IllegalArgumentException e) {
            stand = Stand.LEEG;
        }
        wilNiet = in.getBooleanOr("WilNiet", false);
        boom = in.read("Boom", Boom.CODEC).filter(Boom::klopt).orElse(Boom.GEEN);
        stamBlok = in.read("Stam", net.minecraft.core.registries.BuiltInRegistries.BLOCK.byNameCodec()).orElse(null);
    }

    /** (the list of the tree's blocks is the server's business: not sent to clients) */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.remove("Boom");
        return tag;
    }
}
