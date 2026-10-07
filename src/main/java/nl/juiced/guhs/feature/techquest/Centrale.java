package nl.juiced.guhs.feature.techquest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.block.GuhWheelBlock;
import nl.juiced.guhs.block.GuhWheelPartBlock;
import nl.juiced.guhs.block.entity.GuhWheelBlockEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.guhoven.GuhovenFeature;
import nl.juiced.guhs.feature.techbron.TechbronFeature;
import nl.juiced.guhs.feature.techbuis.BuisFilter;
import nl.juiced.guhs.feature.techbuis.BuisStukBlock;
import nl.juiced.guhs.feature.techbuis.FilterBlockEntity;
import nl.juiced.guhs.feature.techbuis.TechbuisFeature;
import nl.juiced.guhs.feature.techmachine.TechmachineFeature;
import nl.juiced.guhs.feature.techsaus.SausvatBlockEntity;
import nl.juiced.guhs.feature.techsaus.TechsausFeature;
import nl.juiced.guhs.feature.vadskracht.Sauzen;
import nl.juiced.guhs.feature.vadskracht.VadsKracht;
import nl.juiced.guhs.feature.vadskracht.VadsNet;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModFluids;

/**
 * bbq2 (tech-quests): the practice hall of the Oude Guhrad-centrale.
 * <p>
 * Five old Guhraden stand against the north wall, each with an old guh in it that nobody can take out (it has no owner),
 * and in front of each a practice setup that is broken in one way ({@link Opstelling}). The Uitvinder-guh's questline
 * ({@link TechquestFeature#TECHNIEK}) sends every player past them, one by one. A setup is mended with the real thing a
 * player would do at home: lay Guhdraad, cut a wire, turn a Richtingstuk around, set a Filterstuk right, lay a Sausslang.
 * <p>
 * <b>Everything of the setups is put down by this class, not by the template.</b> A structure start is turned at random
 * and the Guhrad, Guhdraad and the Guh Oven do not turn with a template; so {@link #tik} furnishes a copy the first time
 * a player comes near (turned the way the copy is), keeps the fixed parts whole, and puts a setup back in its broken state
 * once nobody has stood near it for {@link #RUST} seconds. Nothing is saved: what a setup looks like is all there is.
 * <p>
 * <b>Per player.</b> The step is the player's own (the Verhaallijn). A setup that works counts for every player at that
 * step who stands within {@link #KIJK} blocks of it, so friends do it together and nobody waits for anybody; the next
 * player finds it broken again.
 * <p>
 * The template coordinates below are THE SAME NUMBERS as tools/features/tech_quests_bouw.py PLEKKEN (the generator's
 * self-check compares the two files).
 */
public final class Centrale {
    public static final String STRUCTUUR = "oude_guhrad_centrale";
    /**
     * How near a player must be for a setup to count for them (in its own bay: the bays are six blocks apart, so a setup
     * never counts for somebody who stands at the next one), and how near somebody must be for it to be left alone.
     */
    public static final int KIJK = 5, DICHTBIJ = 10;
    /** Checks (seconds) without anybody near before a mended setup is broken again. */
    public static final int RUST = 5;
    /** What the barrels of the tube setups hold: cardboard knabbels, and crumpled paper for the filter to refuse. */
    public static final int OEFENKNABBELS = 16, OEFENROMMEL = 8;

    // --- template coordinates (tech_quests_bouw.PLEKKEN) -----------------------------------------------------------------
    static final BlockPos[] RADEREN = {pos(11, 4, 10), pos(17, 4, 10), pos(23, 4, 10), pos(29, 4, 10), pos(35, 4, 10)};
    static final BlockPos[] S1_DRAAD = {pos(11, 4, 11), pos(11, 4, 13)};
    static final BlockPos[] S1_GAT = {pos(11, 4, 12)};
    static final BlockPos[] S1_OVEN = {pos(11, 4, 14)};
    static final BlockPos[] S2_DRAAD = {pos(17, 4, 11), pos(17, 4, 12), pos(15, 4, 13), pos(17, 4, 13), pos(19, 4, 13)};
    static final BlockPos[] S2_KNIP = {pos(16, 4, 13), pos(18, 4, 13)};
    static final BlockPos[] S2_OVENS = {pos(15, 4, 14), pos(19, 4, 14)};
    static final BlockPos[] S2_MOLEN = {pos(17, 4, 14)};
    static final BlockPos[] S3_BATTERIJ = {pos(23, 4, 11)};
    static final BlockPos[] S3_VAT_A = {pos(21, 4, 14)};
    static final BlockPos[] S3_STUK = {pos(22, 4, 14)};
    static final BlockPos[] S3_BUIS = {pos(23, 4, 14), pos(24, 4, 14)};
    static final BlockPos[] S3_VAT_B = {pos(25, 4, 14)};
    static final BlockPos[] S4_DRAAD = {pos(29, 4, 11), pos(29, 4, 12), pos(29, 4, 13), pos(28, 4, 13)};
    static final BlockPos[] S4_VAT_A = {pos(27, 4, 14)};
    static final BlockPos[] S4_FILTER = {pos(28, 4, 14)};
    static final BlockPos[] S4_BUIS = {pos(29, 4, 14), pos(30, 4, 14)};
    static final BlockPos[] S4_VAT_B = {pos(31, 4, 14)};
    static final BlockPos[] S5_DRAAD = {pos(35, 4, 11)};
    static final BlockPos[] S5_BRON = {pos(35, 3, 12)};
    static final BlockPos[] S5_POMP = {pos(35, 4, 12)};
    static final BlockPos[] S5_SLANG = {pos(35, 4, 13)};
    static final BlockPos[] S5_GAT = {pos(35, 4, 14)};
    static final BlockPos[] S5_SAUSVAT = {pos(35, 4, 15)};
    static final BlockPos[] TEKENTAFEL = {pos(8, 4, 22)};
    static final BlockPos[] KERN = {pos(28, 5, 24)};
    static final BlockPos[] UITVINDER = {pos(14, 4, 24)};
    /** The tops of the two chimneys (where the smoke comes out) and the two far corners of the hall. */
    static final BlockPos[] SCHOORSTENEN = {pos(10, 32, 5), pos(36, 32, 5)};
    static final BlockPos HOEK_NW = pos(6, 4, 9), HOEK_ZO = pos(40, 4, 31);
    /** The old guhs in the five wheels (their names are on the signs over them). */
    private static final GuhVariant[] OUDJES = {GuhVariant.NORMAL, GuhVariant.CHOCO, GuhVariant.MINT, GuhVariant.SNOW, GuhVariant.GOLDEN};

    private static BlockPos pos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }

    private Centrale() {
    }

    // =====================================================================================================================
    // a copy of the building
    // =====================================================================================================================

    /**
     * One copy: where template coordinates lie in the world, how the copy is turned, and whether the whole building stands
     * there ({@code heel}; false: only the practice hall, as in the game tests, so nothing is put down outside it).
     */
    public record Kopie(ServerLevel level, String sleutel, Function<BlockPos, BlockPos> plek, Rotation draai, boolean heel) {
        public BlockPos w(BlockPos lokaal) {
            return plek.apply(lokaal);
        }

        /** A direction of the template, in the world. */
        public Direction kant(Direction d) {
            return draai.rotate(d);
        }

        boolean geladen() {
            return level.isLoaded(w(RADEREN[0])) && level.isLoaded(w(RADEREN[4])) && level.isLoaded(w(S1_OVEN[0])) && level.isLoaded(w(S5_SAUSVAT[0]))
                    && (!heel || level.isLoaded(w(HOEK_NW)) && level.isLoaded(w(HOEK_ZO)));
        }
    }

    /** The copy of a generated structure start (null: it has no piece). */
    @Nullable
    public static Kopie van(ServerLevel level, StructureStart start) {
        if (Kopieen.wereld(start, null, BlockPos.ZERO) == null) {
            return null;
        }
        return new Kopie(level, level.dimension().identifier() + "@" + start.getChunkPos().pack(), lokaal -> Kopieen.wereld(start, null, lokaal),
                Kopieen.draai(start, null), true);
    }

    /** (tests, the dev command) a copy whose template corner (0, 0, 0) lies at {@code hoek}, turned {@code draai}. */
    public static Kopie proef(ServerLevel level, BlockPos hoek, Rotation draai, boolean heel) {
        return new Kopie(level, "proef@" + hoek.asLong() + "@" + draai, lokaal -> hoek.offset(StructureTemplate.transform(lokaal, Mirror.NONE, draai, BlockPos.ZERO)),
                draai, heel);
    }

    /** The copy this spot belongs to (a piece within 48 blocks), or null. */
    @Nullable
    public static Kopie bij(ServerLevel level, BlockPos pos) {
        for (Kopie k : PROEF.values()) {
            if (k.level() == level && k.w(RADEREN[2]).closerThan(pos, 64)) {
                return k;
            }
        }
        StructureStart start = Bezetting.start(level, STRUCTUUR, pos);
        return start == null ? null : van(level, start);
    }

    /** (tests, the dev command) copies that are not generated structures: they are ticked like the real ones. */
    private static final Map<String, Kopie> PROEF = new ConcurrentHashMap<>();

    public static void proefAan(Kopie k) {
        PROEF.put(k.sleutel(), k);
    }

    public static void proefUit(Kopie k) {
        PROEF.remove(k.sleutel());
    }

    // =====================================================================================================================
    // the five setups
    // =====================================================================================================================

    /** A practice setup: the step of the questline it belongs to and its middle (for "who stands near"). */
    public enum Opstelling {
        DRAAD(1, pos(11, 4, 13)), TE_ZWAAR(2, pos(17, 4, 13)), BUIS(4, pos(23, 4, 14)), FILTER(5, pos(29, 4, 14)), SAUS(6, pos(35, 4, 14));

        public final int stap;
        public final BlockPos midden;

        Opstelling(int stap, BlockPos midden) {
            this.stap = stap;
            this.midden = midden;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        @Nullable
        public static Opstelling vanStap(int stap) {
            for (Opstelling o : values()) {
                if (o.stap == stap) {
                    return o;
                }
            }
            return null;
        }
    }

    /** What is remembered of a copy while the server runs (nothing is saved). */
    private static final class Stand {
        final boolean[] ingericht = new boolean[Opstelling.values().length];
        final int[] leeg = new int[Opstelling.values().length];
    }

    private static final Map<String, Stand> STANDEN = new ConcurrentHashMap<>();

    // --- putting things down -------------------------------------------------------------------------------------------------

    /** Puts this state here unless it is there already (true: something changed). */
    private static boolean zet(Kopie k, BlockPos lokaal, BlockState state) {
        BlockPos pos = k.w(lokaal);
        if (k.level().getBlockState(pos) == state) {
            return false;
        }
        k.level().setBlock(pos, state, Block.UPDATE_ALL);
        return true;
    }

    /** Puts this block here unless a block of that kind is there already (whatever its state: tubes, wire and hoses shape themselves). */
    private static boolean zetSoort(Kopie k, BlockPos lokaal, Block block) {
        BlockPos pos = k.w(lokaal);
        if (k.level().getBlockState(pos).is(block)) {
            return false;
        }
        k.level().setBlock(pos, Block.updateFromNeighbourShapes(block.defaultBlockState(), k.level(), pos), Block.UPDATE_ALL);
        return true;
    }

    private static void weg(Kopie k, BlockPos lokaal, Block block) {
        BlockPos pos = k.w(lokaal);
        if (k.level().getBlockState(pos).is(block)) {
            k.level().setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    private static void draad(Kopie k, BlockPos... lokaal) {
        for (BlockPos p : lokaal) {
            zetSoort(k, p, ModBlocks.GUH_WIRE.get());
        }
    }

    /** A block with a horizontal FACING (a machine, the Guh Oven, a battery, a vat), looking this way; it keeps whatever else its state says. */
    private static void gericht(Kopie k, BlockPos lokaal, Block block, Direction kant) {
        BlockState er = k.level().getBlockState(k.w(lokaal));
        Direction wil = k.kant(kant);
        if (!er.is(block) || er.getValue(HorizontalDirectionalBlock.FACING) != wil) {
            zet(k, lokaal, block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, wil));
        }
    }

    /** A Guhrad with an old guh in it: the kern, its eight other blocks, the guh. Nobody owns the guh, so nobody takes it out. */
    private static void rad(Kopie k, BlockPos lokaal, GuhVariant variant) {
        ServerLevel level = k.level();
        BlockPos pos = k.w(lokaal);
        Direction facing = k.kant(Direction.SOUTH);
        BlockState kern = ModBlocks.GUH_WHEEL.get().defaultBlockState().setValue(GuhWheelBlock.FACING, facing).setValue(GuhWheelBlock.RUNNING, true);
        if (level.getBlockState(pos) != kern) {
            level.setBlock(pos, kern, Block.UPDATE_ALL);
        }
        Direction along = GuhWheelPartBlock.along(facing);
        for (BlockPos part : GuhWheelBlock.partPositions(pos, facing)) {
            int side = part.get(along.getAxis()) - pos.get(along.getAxis());
            side = along.getAxisDirection() == Direction.AxisDirection.POSITIVE ? side : -side;
            BlockState deel = ModBlocks.GUH_WHEEL_PART.get().defaultBlockState().setValue(GuhWheelPartBlock.FACING, facing)
                    .setValue(GuhWheelPartBlock.SIDE, side + 1).setValue(GuhWheelPartBlock.HEIGHT, part.getY() - pos.getY());
            if (level.getBlockState(part) != deel) {
                level.setBlock(part, deel, Block.UPDATE_ALL);
            }
        }
        if (level.getBlockEntity(pos) instanceof GuhWheelBlockEntity wiel && !wiel.hasGuh()) {
            CompoundTag guh = new CompoundTag();
            guh.putString("id", "guhs:guh");
            guh.putString("Variant", variant.id());
            wiel.insert(guh);
        }
    }

    // --- the barrels of the tube setups ------------------------------------------------------------------------------------

    @Nullable
    private static Container vat(Kopie k, BlockPos lokaal) {
        if (!k.level().getBlockState(k.w(lokaal)).is(Blocks.BARREL)) {
            zet(k, lokaal, Blocks.BARREL.defaultBlockState());
        }
        return k.level().getBlockEntity(k.w(lokaal)) instanceof Container c ? c : null;
    }

    private static int tel(Container c, Item item) {
        int n = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (c.getItem(i).is(item)) {
                n += c.getItem(i).getCount();
            }
        }
        return n;
    }

    /** Everything out of {@code van} into {@code naar} (what doesn't fit stays). */
    private static void giet(Container van, Container naar) {
        for (int i = 0; i < van.getContainerSize(); i++) {
            ItemStack stack = van.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            for (int j = 0; j < naar.getContainerSize() && !stack.isEmpty(); j++) {
                ItemStack daar = naar.getItem(j);
                if (daar.isEmpty()) {
                    naar.setItem(j, stack.copy());
                    stack = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(daar, stack) && daar.getCount() < daar.getMaxStackSize()) {
                    int n = Math.min(stack.getCount(), daar.getMaxStackSize() - daar.getCount());
                    daar.grow(n);
                    stack.shrink(n);
                }
            }
            van.setItem(i, stack);
        }
        van.setChanged();
        naar.setChanged();
    }

    /** Fills the barrel up to {@code n} of this practice item (never more: what players took is made up, nothing piles up). */
    private static void vulAan(Container c, Item item, int n) {
        int mist = n - tel(c, item);
        for (int i = 0; i < c.getContainerSize() && mist > 0; i++) {
            if (c.getItem(i).isEmpty()) {
                int erbij = Math.min(mist, 64);
                c.setItem(i, new ItemStack(item, erbij));
                mist -= erbij;
            }
        }
        c.setChanged();
    }

    // --- per setup: the fixed parts, the broken state, "does it work" ----------------------------------------------------------

    /** The parts of a setup that never change: put back whenever they are missing. */
    static void vast(Kopie k, Opstelling o) {
        switch (o) {
            case DRAAD -> {
                draad(k, S1_DRAAD);
                gericht(k, S1_OVEN[0], GuhovenFeature.GUH_OVEN.get(), Direction.SOUTH);
            }
            case TE_ZWAAR -> {
                draad(k, S2_DRAAD);
                for (BlockPos p : S2_OVENS) {
                    gericht(k, p, GuhovenFeature.GUH_OVEN.get(), Direction.SOUTH);
                }
                gericht(k, S2_MOLEN[0], TechmachineFeature.VADSMOLEN.get(), Direction.SOUTH);
            }
            case BUIS -> {
                gericht(k, S3_BATTERIJ[0], TechbronFeature.KNABBELBATTERIJ.get(), Direction.SOUTH);
                vat(k, S3_VAT_A[0]);
                vat(k, S3_VAT_B[0]);
                if (!k.level().getBlockState(k.w(S3_STUK[0])).is(TechbuisFeature.KNABBELBUIS_RICHTING.get())) {
                    zet(k, S3_STUK[0], TechbuisFeature.KNABBELBUIS_RICHTING.get().defaultBlockState().setValue(BuisStukBlock.FACING, k.kant(Direction.WEST)));
                }
                for (BlockPos p : S3_BUIS) {
                    zetSoort(k, p, TechbuisFeature.KNABBELBUIS.get());
                }
            }
            case FILTER -> {
                draad(k, S4_DRAAD);
                vat(k, S4_VAT_A[0]);
                vat(k, S4_VAT_B[0]);
                zet(k, S4_FILTER[0], filterState(k));
                for (BlockPos p : S4_BUIS) {
                    zetSoort(k, p, TechbuisFeature.KNABBELBUIS.get());
                }
            }
            case SAUS -> {
                draad(k, S5_DRAAD);
                BlockPos bron = k.w(S5_BRON[0]);
                if (!k.level().getFluidState(bron).isSource()) {
                    k.level().setBlock(bron, ModFluids.KAAS_SAUS.get().defaultFluidState().createLegacyBlock(), Block.UPDATE_ALL);
                }
                gericht(k, S5_POMP[0], TechsausFeature.SAUSPOMP.get(), Direction.SOUTH);
                zetSoort(k, S5_SLANG[0], TechsausFeature.SAUSSLANG.get());
                gericht(k, S5_SAUSVAT[0], TechsausFeature.SAUSVAT.get(), Direction.SOUTH);
            }
        }
    }

    private static BlockState filterState(Kopie k) {
        BlockState er = k.level().getBlockState(k.w(S4_FILTER[0]));
        BlockState wil = TechbuisFeature.KNABBELBUIS_FILTER.get().defaultBlockState().setValue(BuisStukBlock.FACING, k.kant(Direction.EAST));
        // (its face follows the vadskracht: keep whatever it shows now)
        return er.is(wil.getBlock()) && er.getValue(BuisStukBlock.FACING) == wil.getValue(BuisStukBlock.FACING) ? er : wil;
    }

    /** Breaks a setup the way a new player must find it. */
    static void kapot(Kopie k, Opstelling o) {
        ServerLevel level = k.level();
        switch (o) {
            case DRAAD -> weg(k, S1_GAT[0], ModBlocks.GUH_WIRE.get());
            case TE_ZWAAR -> draad(k, S2_KNIP);
            case BUIS -> {
                // the piece points the wrong way (into the full barrel), and the knabbels are back where they started
                zet(k, S3_STUK[0], TechbuisFeature.KNABBELBUIS_RICHTING.get().defaultBlockState().setValue(BuisStukBlock.FACING, k.kant(Direction.WEST)));
                Container a = vat(k, S3_VAT_A[0]), b = vat(k, S3_VAT_B[0]);
                if (a != null && b != null) {
                    giet(b, a);
                    vulAan(a, TechquestFeature.OEFENKNABBEL.get(), OEFENKNABBELS);
                }
            }
            case FILTER -> {
                // the list knows the knabbel, but the filter stands on "alles behalve": only the paper rolls through
                if (level.getBlockEntity(k.w(S4_FILTER[0])) instanceof FilterBlockEntity filter) {
                    filter.filter().items().clearContent();
                    filter.filter().voegToe(new ItemStack(TechquestFeature.OEFENKNABBEL.get()));
                    filter.filter().zetBehalve(true);
                    filter.filter().zetPrecies(false);
                    filter.setChanged();
                }
                Container a = vat(k, S4_VAT_A[0]), b = vat(k, S4_VAT_B[0]);
                if (a != null && b != null) {
                    giet(b, a);
                    vulAan(a, TechquestFeature.OEFENKNABBEL.get(), OEFENKNABBELS / 2);
                    vulAan(a, TechquestFeature.OEFENROMMEL.get(), OEFENROMMEL);
                }
            }
            case SAUS -> {
                weg(k, S5_GAT[0], TechsausFeature.SAUSSLANG.get());
                if (level.getBlockEntity(k.w(S5_SAUSVAT[0])) instanceof SausvatBlockEntity sausvat && !sausvat.tank().isLeeg()) {
                    sausvat.tank().zet(Sauzen.kaassaus(), 0);
                }
            }
        }
    }

    /**
     * Setup 4 works again: the paper that slipped through while the filter stood wrong goes back to the first barrel (where
     * the mended filter now leaves it), so the second barrel ends up with knabbels only.
     */
    private static void papierTerug(Kopie k) {
        if (k.level().getBlockEntity(k.w(S4_VAT_A[0])) instanceof Container a && k.level().getBlockEntity(k.w(S4_VAT_B[0])) instanceof Container b) {
            for (int i = 0; i < b.getContainerSize(); i++) {
                ItemStack stack = b.getItem(i);
                if (!stack.is(TechquestFeature.OEFENROMMEL.get())) {
                    continue;
                }
                for (int j = 0; j < a.getContainerSize() && !stack.isEmpty(); j++) {
                    ItemStack daar = a.getItem(j);
                    if (daar.isEmpty()) {
                        a.setItem(j, stack.copy());
                        stack = ItemStack.EMPTY;
                    } else if (ItemStack.isSameItemSameComponents(daar, stack) && daar.getCount() < daar.getMaxStackSize()) {
                        int n = Math.min(stack.getCount(), daar.getMaxStackSize() - daar.getCount());
                        daar.grow(n);
                        stack.shrink(n);
                    }
                }
                b.setItem(i, stack);
            }
        }
    }

    /** Is this setup mended (does it do what it should)? */
    public static boolean werkt(Kopie k, Opstelling o) {
        ServerLevel level = k.level();
        switch (o) {
            case DRAAD -> {
                return level.getBlockState(k.w(S1_GAT[0])).is(ModBlocks.GUH_WIRE.get()) && draait(level, k.w(S1_OVEN[0]));
            }
            case TE_ZWAAR -> {
                return draait(level, k.w(RADEREN[1]));
            }
            case BUIS -> {
                BlockState stuk = level.getBlockState(k.w(S3_STUK[0]));
                return stuk.is(TechbuisFeature.KNABBELBUIS_RICHTING.get()) && stuk.getValue(BuisStukBlock.FACING) == k.kant(Direction.EAST);
            }
            case FILTER -> {
                if (!(level.getBlockEntity(k.w(S4_FILTER[0])) instanceof FilterBlockEntity filter)) {
                    return false;
                }
                BuisFilter f = filter.filter();
                return !f.isLeeg() && !f.behalve() && f.past(new ItemStack(TechquestFeature.OEFENKNABBEL.get()))
                        && !f.past(new ItemStack(TechquestFeature.OEFENROMMEL.get()));
            }
            case SAUS -> {
                return level.getBlockState(k.w(S5_GAT[0])).is(TechsausFeature.SAUSSLANG.get())
                        && level.getBlockEntity(k.w(S5_SAUSVAT[0])) instanceof SausvatBlockEntity sausvat && sausvat.tank().inhoud() >= Sauzen.EMMER;
            }
        }
        return false;
    }

    /** Does the vadskracht net of this block run, with something on it that asks for vadskracht? */
    private static boolean draait(ServerLevel level, BlockPos pos) {
        VadsNet net = VadsKracht.net(level, pos);
        return net.draait() && net.vraag() > 0 && net.aanbod() > 0;
    }

    // =====================================================================================================================
    // the tick
    // =====================================================================================================================

    static void opTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.players().isEmpty()) {
                continue;
            }
            Map<String, Kopie> kopieen = new HashMap<>();
            Map<String, List<ServerPlayer>> spelers = new HashMap<>();
            for (ServerPlayer p : level.players()) {
                Kopie k = bij(level, p.blockPosition());
                if (k != null) {
                    kopieen.putIfAbsent(k.sleutel(), k);
                    spelers.computeIfAbsent(k.sleutel(), s -> new ArrayList<>()).add(p);
                }
            }
            for (Kopie k : kopieen.values()) {
                tik(k, spelers.get(k.sleutel()));
            }
        }
    }

    /** One look at a copy (once a second while players are around it): furnish, mend-check, break again, smoke. */
    public static void tik(Kopie k, List<ServerPlayer> spelers) {
        if (!k.geladen()) {
            return;
        }
        ServerLevel level = k.level();
        Stand stand = STANDEN.computeIfAbsent(k.sleutel(), s -> new Stand());
        for (int i = 0; i < RADEREN.length; i++) {
            rad(k, RADEREN[i], OUDJES[i]);
        }
        if (k.heel()) {
            gericht(k, TEKENTAFEL[0], TechmachineFeature.TEKENTAFEL.get(), Direction.EAST);
        }
        for (Opstelling o : Opstelling.values()) {
            int i = o.ordinal();
            BlockPos midden = k.w(o.midden);
            boolean iemand = false;
            for (ServerPlayer p : spelers) {
                if (!p.isSpectator() && p.blockPosition().closerThan(midden, DICHTBIJ)) {
                    iemand = true;
                    break;
                }
            }
            vast(k, o);
            if (!stand.ingericht[i]) {
                kapot(k, o);
                stand.ingericht[i] = true;
                stand.leeg[i] = 0;
            } else if (iemand) {
                stand.leeg[i] = 0;
            } else if (++stand.leeg[i] >= RUST) {
                kapot(k, o);
            }
            if (iemand && werkt(k, o)) {
                if (o == Opstelling.FILTER) {
                    papierTerug(k);
                }
                for (ServerPlayer p : spelers) {
                    if (!p.isSpectator() && p.blockPosition().closerThan(midden, KIJK) && TechquestFeature.TECHNIEK.stap(p) == o.stap) {
                        gemaakt(p, o, midden);
                    }
                }
            }
        }
        // the chimneys smoke as long as the old guhs run
        if (k.heel() && level.getGameTime() % 40 == 0) {
            for (BlockPos s : SCHOORSTENEN) {
                BlockPos top = k.w(s);
                level.sendParticles(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, false, top.getX() + 0.5, top.getY() + 0.4, top.getZ() + 0.5, 0, 0.0, 0.07, 0.0, 1.0);
            }
        }
    }

    /** This player mended (or helped mend) the setup of their step: one step on. */
    private static void gemaakt(ServerPlayer p, Opstelling o, BlockPos midden) {
        Verhaallijn lijn = TechquestFeature.TECHNIEK;
        if (!lijn.verder(p, o.stap)) {
            return;
        }
        ServerLevel level = p.level();
        p.sendOverlayMessage(Component.translatable("gui.guhs.techquest.opstelling." + o.id() + ".gemaakt").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.translatable("gui.guhs.techquest.opstelling." + o.id() + ".verder").withStyle(ChatFormatting.GOLD));
        if (o == Opstelling.DRAAD) {
            p.sendSystemMessage(Component.translatable("gui.guhs.techquest.opstelling.stuk").withStyle(ChatFormatting.GRAY));
        }
        level.playSound(null, midden, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, midden.getX() + 0.5, midden.getY() + 1.0, midden.getZ() + 0.5, 18, 1.2, 0.6, 1.2, 0.02);
    }

    // =====================================================================================================================
    // what a player may do in the hall
    // =====================================================================================================================

    /**
     * Laying Guhdraad in the gap of setup 1, or a Sausslang in the gap of setup 5: the building is protected
     * ({@link Bescherming} refuses every block item), so this puts the piece down itself. Any player may (also one who is
     * not at that step: a friend helps), the setup breaks again by itself.
     */
    static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        ItemStack stack = event.getItemStack();
        boolean draad = stack.is(ModBlocks.GUH_WIRE.get().asItem()), slang = stack.is(TechsausFeature.SAUSSLANG_ITEM.get());
        if (!draad && !slang) {
            return;
        }
        BlockPos geklikt = event.getPos();
        BlockPos doel = level.getBlockState(geklikt).canBeReplaced() || event.getFace() == null ? geklikt : geklikt.relative(event.getFace());
        if (!STRUCTUUR.equals(Bescherming.structuurBij(level, doel)) && PROEF.isEmpty()) {
            return;
        }
        Kopie k = bij(level, doel);
        if (k == null) {
            return;
        }
        BlockPos gat = k.w(draad ? S1_GAT[0] : S5_GAT[0]);
        if (!doel.equals(gat)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!level.getBlockState(gat).canBeReplaced()) {
            return;
        }
        Block block = draad ? ModBlocks.GUH_WIRE.get() : TechsausFeature.SAUSSLANG.get();
        level.setBlock(gat, Block.updateFromNeighbourShapes(block.defaultBlockState(), level, gat), Block.UPDATE_ALL);
        if (!p.getAbilities().instabuild) {
            stack.shrink(1);
        }
        level.playSound(null, gat, block.defaultBlockState().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1f, 1f);
    }

    /**
     * The blocks of the hall a player may break (the exception to {@link Bescherming}): the two wires of setup 2 that
     * may be cut, and the piece somebody laid in a gap (so it can be taken out again).
     */
    static boolean magBreken(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        BlockState state = level.getBlockState(pos);
        boolean draad = state.is(ModBlocks.GUH_WIRE.get()), slang = state.is(TechsausFeature.SAUSSLANG.get());
        if (!draad && !slang) {
            return false;
        }
        Kopie k = bij(level, pos);
        if (k == null) {
            return false;
        }
        if (slang) {
            return k.w(S5_GAT[0]).equals(pos);
        }
        for (BlockPos knip : S2_KNIP) {
            if (k.w(knip).equals(pos)) {
                return true;
            }
        }
        return k.w(S1_GAT[0]).equals(pos);
    }

    /** (the dev command, tests) furnishes a copy at once, every setup broken. */
    public static void richtIn(Kopie k) {
        STANDEN.remove(k.sleutel());
        tik(k, List.of());
    }
}
