package nl.juiced.guhs.feature.bio.wereld;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.serialization.MapCodec;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.Bio;

/**
 * biomes3 wereld, the Bloesemmeertje: everything that lives in and around the lake. Feature {@code guhs:bloesemmeertje_leven},
 * once per chunk in the vegetation step (after the buildings; nothing of it is placed inside a building's pieces):
 * <ul>
 *   <li>the trees of {@link MeerTerrein#bomen} (all but an island's big tree, which {@link MeerVulling} places);</li>
 *   <li>on the land: the mod's pink flowers in a few loose patches and hardly any elsewhere, a tuft of roze gras;</li>
 *   <li>in sheltered corners only (land on five of the eight sides within nine blocks): guh-waterlelies in a small
 *       cluster and bloesemriet on the bank beside it;</li>
 *   <li>on the water: drijvende bloesemblaadjes under the crowns that hang over it, and in a loose streak drifting away
 *       from a tree that leans over it (out from the shore, bent to the lake's own drift direction);</li>
 *   <li>under water: seagrass in patches (2-6 deep), now and then a tuft of kaaskoraal or a reuzenschelp.</li>
 * </ul>
 * World generation's heightmaps do not see the lake's water, so nothing here asks a heightmap: every position comes from
 * the terrain model, and a block is only placed where the world agrees (air above the ground, water where the model
 * says water). All of it stays inside the chunk it is called for, except a tree's crown (at most 9 blocks out).
 * Cost: the chunk's own map is read for its own columns; what must be known about other chunks (a tree 30 blocks off
 * whose petals drift in, land around a lily corner) is asked per single column ({@link MeerTerrein#los}), never by
 * building a neighbour's map.
 * <p>
 * Also registered here, for the biome: the block {@code guhs:bloesemmeertje_riet} (Bloesemriet: a reed in the lake's own
 * colours), the sound events of the music and the ambience ({@code bloesemmeertje.muziek}, {@code bloesemmeertje.ambient})
 * and the dev command {@code /guhs bio wereld-meer tijd | kaarttijd} (what the lake costs per chunk).
 */
public class MeerLeven extends Feature<NoneFeatureConfiguration> {
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final DeferredHolder<Feature<?>, MeerLeven> LEVEN = FEATURES.register("bloesemmeertje_leven", MeerLeven::new);
    public static final DeferredBlock<Riet> RIET = BLOCKS.registerBlock("bloesemmeertje_riet", Riet::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.TALL_GRASS).mapColor(MapColor.COLOR_PINK));
    public static final DeferredItem<DoubleHighBlockItem> RIET_ITEM = ITEMS.registerItem("bloesemmeertje_riet", p -> new DoubleHighBlockItem(RIET.get(), p));
    public static final DeferredHolder<SoundEvent, SoundEvent> MUZIEK = geluid("bloesemmeertje.muziek");
    public static final DeferredHolder<SoundEvent, SoundEvent> AMBIENT = geluid("bloesemmeertje.ambient");

    /** What the lake has cost since the last {@code /guhs bio wereld-meer tijd}: nanoseconds and chunks, of {@link MeerVulling#vul} and of {@link #leef}. */
    static final LongAdder VUL_NS = new LongAdder(), VUL_N = new LongAdder(), LEEF_NS = new LongAdder(), LEEF_N = new LongAdder();

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    /** Called from {@link WereldSlice#register}. */
    public static void register(IEventBus modBus) {
        FEATURES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        NeoForge.EVENT_BUS.addListener(MeerLeven::commando);
    }

    /** Called from {@link WereldSlice#creative}. */
    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(RIET_ITEM.get()));
    }

    /** {@code /guhs bio wereld-meer tijd}: the cost per lake chunk so far (and start counting again); {@code kaarttijd x z}: the cost of a chunk map there. */
    private static void commando(RegisterCommandsEvent event) {
        var meer = Commands.literal("wereld-meer").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        meer.then(Commands.literal("tijd").executes(c -> {
            long vn = VUL_N.sumThenReset(), ln = LEEF_N.sumThenReset(), vt = VUL_NS.sumThenReset(), lt = LEEF_NS.sumThenReset();
            String tekst = String.format(Locale.ROOT, "[bio-wereld-meer] tijd: bed, water and big tree %.2f ms per chunk (%d chunks); life %.2f ms per chunk (%d chunks)",
                    vn == 0 ? 0 : vt / 1e6 / vn, vn, ln == 0 ? 0 : lt / 1e6 / ln, ln);
            c.getSource().sendSuccess(() -> Component.literal(tekst), false);
            return 1;
        }));
        meer.then(Commands.literal("kaarttijd").then(Commands.argument("x", IntegerArgumentType.integer()).then(Commands.argument("z", IntegerArgumentType.integer())
                .executes(c -> {
                    BioModel m = BioModel.van(c.getSource().getLevel().getChunkSource().randomState());
                    int cx = IntegerArgumentType.getInteger(c, "x") >> 4, cz = IntegerArgumentType.getInteger(c, "z") >> 4, n = 0, meren = 0;
                    long t0 = System.nanoTime();
                    // (20 x 20 chunk maps, each built twice in an order the model's small cache cannot hold: nearly all are built fresh)
                    for (int ronde = 0; ronde < 2; ronde++) {
                        for (int ax = -10; ax < 10; ax++) {
                            for (int az = -10; az < 10; az++) {
                                Kaart k = m.kaart(cx + ax, cz + az);
                                n++;
                                meren += !k.leeg && k.soort[136] == Kaart.MEER ? 1 : 0;
                            }
                        }
                    }
                    double ms = (System.nanoTime() - t0) / 1e6;
                    String tekst = String.format(Locale.ROOT, "[bio-wereld-meer] kaarttijd: %d chunk maps (%d in the lake biome) in %.0f ms, %.3f ms per map", n, meren, ms, ms / n);
                    c.getSource().sendSuccess(() -> Component.literal(tekst), false);
                    return 1;
                }))));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("bio").then(meer)));
    }

    /** Bloesemriet: a tall reed with soft pink plumes; it stands on anything with a firm top (sand, the Guhmensie's wool). */
    public static class Riet extends DoublePlantBlock {
        public static final MapCodec<Riet> CODEC = simpleCodec(Riet::new);

        public Riet(Properties properties) {
            super(properties);
        }

        @Override
        public MapCodec<Riet> codec() {
            return CODEC;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return super.mayPlaceOn(state, level, pos) || state.isFaceSturdy(level, pos, Direction.UP);
        }
    }

    // <meer-leven> (free to tune)
    /** Flowers per land column inside a flower patch and outside one; roze gras per land column. */
    static final float BLOEM_IN_VELD = 0.10f, BLOEM_LOS = 0.012f, GRAS = 0.015f;
    /** A corner is sheltered when at least this many of the eight directions meet land within nine blocks. */
    static final int LUW = 5;
    /** Per chunk: how many spots are tried for a lily corner and for a reed corner, the chance a chunk gets to try, the chance of a coral tuft, of a shell. */
    static final int LELIE_POGINGEN = 8, RIET_POGINGEN = 6;
    static final double LELIE_KANS = 0.8, RIET_KANS = 0.6, KORAAL_KANS = 0.2, SCHELP_KANS = 0.1;
    /** Seagrass per water column (2-6 deep) inside a seagrass patch and outside one. */
    static final float ZEEGRAS_IN_VELD = 0.11f, ZEEGRAS_LOS = 0.01f;
    /** Petals per water column under an overhanging crown; how many leaning trees send a streak out over the water. */
    static final float BLAADJES_ONDER_KROON = 0.14f;
    static final double SLIERT_KANS = 0.6;
    // </meer-leven>

    private static final String[] BLOEMEN = {"roze_guhbloem", "roze_guhbloem", "roze_guhbloem", "roze_guhbloem", "knabbelroos", "knabbelroos",
            "knabbelroos", "roze_hibiscus", "roze_hibiscus"};

    public MeerLeven() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** The petal positions (x, z packed with {@link MeerTerrein#pak}) of a tree's drifting streak; empty when it has none. Pure. */
    public static long[] sliert(BioModel m, MeerTerrein.Boom b) {
        // only a tree that leans over the water sends petals out on it
        if (b.maat() == 0 || b.leunX() == 0 && b.leunZ() == 0 || b.maat() < 3 && BioModel.kans(b.zaad(), 40) >= SLIERT_KANS) {
            return new long[0];
        }
        double[] kroon = BloesemBoom.kroon(b);
        // out from the shore, bent towards the drift of this lake (one direction per 512 blocks)
        long lh = m.hash(Math.floorDiv(b.x(), 512), Math.floorDiv(b.z(), 512), 4230);
        double drift = BioModel.kans(lh, 0) * Math.PI * 2, uit0 = Math.atan2(b.leunZ(), b.leunX());
        double hoek = Math.atan2(Math.sin(uit0) + 0.6 * Math.sin(drift), Math.cos(uit0) + 0.6 * Math.cos(drift)) + (BioModel.kans(b.zaad(), 41) - 0.5) * 0.6;
        double wx = Math.cos(hoek), wz = Math.sin(hoek), lengte = 10 + 13 * BioModel.kans(b.zaad(), 42) + (b.maat() == 3 ? 8 : 0);
        double fase = BioModel.kans(b.zaad(), 43) * 6.283;
        Random rnd = new Random(b.zaad() ^ 0x5DEECE66DL);
        int n = (int) (lengte * 4);
        long[] uit = new long[n];
        int aantal = 0;
        for (int i = 0; i < n; i++) {
            double t = i / 4.0, slinger = 2.2 * Math.sin(t * 0.21 + fase), breed = 0.3 + 1.5 * (1 - t / lengte);
            double px = kroon[0] + wx * (kroon[2] * 0.6 + t) - wz * slinger + rnd.nextGaussian() * breed;
            double pz = kroon[1] + wz * (kroon[2] * 0.6 + t) + wx * slinger + rnd.nextGaussian() * breed;
            if (rnd.nextFloat() < 0.6f) {
                uit[aantal++] = MeerTerrein.pak((int) Math.floor(px), (int) Math.floor(pz));
            }
        }
        return java.util.Arrays.copyOf(uit, aantal);
    }

    /** In how many of the eight directions land lies within nine blocks of (x, z). */
    public static int luw(BioModel m, int x, int z) {
        int n = 0;
        for (int r = 0; r < 8; r++) {
            for (int a = 3; a <= 9; a += 3) {
                if (!MeerTerrein.nat(m, x + MeerTerrein.RX[r] * a, z + MeerTerrein.RZ[r] * a)) {
                    n++;
                    break;
                }
            }
        }
        return n;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        ChunkPos chunk = ChunkPos.containing(context.origin());
        BioModel m = BioModel.van(level.getLevel().getChunkSource().randomState());
        long t0 = System.nanoTime();
        int n = leef(level, m, chunk.x(), chunk.z());
        if (n >= 0) {
            LEEF_NS.add(System.nanoTime() - t0);
            LEEF_N.increment();
        }
        return n > 0;
    }

    /** Places the life of one chunk; returns how many things were placed (-1: not a chunk of the lake biome). */
    public static int leef(WorldGenLevel level, BioModel m, int cx, int cz) {
        Kaart k = m.kaart(cx, cz);
        if (k.leeg) {
            return -1;
        }
        boolean meer = false;
        for (int o = 0; o < 256 && !meer; o++) {
            meer = k.soort[o] == Kaart.MEER && k.meng[o] >= 1f;
        }
        if (!meer) {
            return -1;
        }
        int x0 = cx << 4, z0 = cz << 4, gezet = 0;
        StructureManager gebouwen = level instanceof WorldGenRegion regio ? level.getLevel().structureManager().forWorldGenRegion(regio) : null;
        boolean bouwHier = gebouwen != null && !level.getChunk(cx, cz).getAllReferences().isEmpty();
        Random rnd = new Random(m.hash(cx, cz, 4200));
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

        // --- trees ---
        for (MeerTerrein.Boom b : MeerTerrein.bomen(m, x0, z0, x0 + 16, z0 + 16, true)) {
            int o = Kaart.index(b.x(), b.z());
            if (b.vast() || k.water[o] != Kaart.GEEN) {
                continue;
            }
            int y = k.hoogte[o] + 1;
            BlockState grond = level.getBlockState(p.set(b.x(), y - 1, b.z()));
            if (grond.isAir() || !grond.getFluidState().isEmpty() || !level.getBlockState(p.set(b.x(), y, b.z())).isAir()) {
                continue;
            }
            double[] kroon = BloesemBoom.kroon(b);
            boolean vrij = !gebouw(gebouwen, p.set(b.x(), y, b.z()));
            for (int r = 0; r < 8 && vrij; r += 2) {
                vrij = !gebouw(gebouwen, p.set((int) (kroon[0] + MeerTerrein.RX[r] * kroon[2]), y + BloesemBoom.hoogte(b), (int) (kroon[1] + MeerTerrein.RZ[r] * kroon[2])));
            }
            if (vrij) {
                gezet += BloesemBoom.bouw(level, b, y) > 0 ? 1 : 0;
            }
        }
        // the trees whose crown or streak can reach this chunk (those of the lake's own shore and the islands)
        List<MeerTerrein.Boom> nabij = MeerTerrein.bomen(m, x0 - 38, z0 - 38, x0 + 54, z0 + 54, false);
        double[][] kronen = new double[nabij.size()][];
        for (int i = 0; i < kronen.length; i++) {
            kronen[i] = BloesemBoom.kroon(nabij.get(i));
        }

        BlockState blaadjes = Bio.blok("drijvende_bloesemblaadjes", Blocks.AIR).defaultBlockState();
        BlockState gras = Bio.blok("roze_gras", Blocks.AIR).defaultBlockState();
        BlockState zeegras = Blocks.SEAGRASS.defaultBlockState();

        // --- per column: flowers on the land; petals under the crowns and seagrass in the water ---
        for (int o = 0; o < 256; o++) {
            if (k.soort[o] != Kaart.MEER || k.meng[o] < 1f) {
                continue;
            }
            int x = x0 + (o & 15), z = z0 + (o >> 4), h = k.hoogte[o];
            float kans = rnd.nextFloat(), kans2 = rnd.nextFloat();
            if (k.water[o] == Kaart.GEEN) {
                if ((k.vlag[o] & (MeerTerrein.STEEN | MeerTerrein.STRAND | Kaart.LIP | Kaart.RIVIER)) != 0 || h > MeerTerrein.WATER + 6) {
                    continue;
                }
                BlockState wat = null;
                if (kans < GRAS) {
                    wat = gras;
                } else if (kans2 < (m.ruis(BioModel.R_DETAIL, x * 0.7 + 91.5, z * 0.7 - 37.5) > 0.3 ? BLOEM_IN_VELD : BLOEM_LOS)) {
                    wat = Bio.blok(BLOEMEN[rnd.nextInt(BLOEMEN.length)], Blocks.AIR).defaultBlockState();
                }
                if (wat != null && !wat.isAir() && level.getBlockState(p.set(x, h + 1, z)).isAir() && wat.canSurvive(level, p) && !(bouwHier && gebouw(gebouwen, p))) {
                    level.setBlock(p, wat, 2);
                    gezet++;
                }
                continue;
            }
            if (k.terras[o] >= 0) {
                continue;
            }
            int diepte = k.water[o] - h;
            if (kans < BLAADJES_ONDER_KROON) {
                for (double[] kr : kronen) {
                    double dx = x + 0.5 - kr[0], dz = z + 0.5 - kr[1];
                    if (dx * dx + dz * dz < (kr[2] + 1.2) * (kr[2] + 1.2)) {
                        gezet += drijf(level, gebouwen, bouwHier, p.set(x, MeerTerrein.WATER + 1, z), blaadjes, 1);
                        break;
                    }
                }
            }
            if (diepte >= 2 && diepte <= 6 && kans2 < (m.ruis(BioModel.R_DETAIL, x * 0.9 - 17.5, z * 0.9 + 55.5) > 0.42 ? ZEEGRAS_IN_VELD : ZEEGRAS_LOS)
                    && level.getBlockState(p.set(x, h + 1, z)).is(Blocks.WATER) && level.getBlockState(p.set(x, h, z)).isFaceSturdy(level, p, Direction.UP)
                    && !(bouwHier && gebouw(gebouwen, p.set(x, h + 1, z)))) {
                level.setBlock(p.set(x, h + 1, z), zeegras, 2);
                gezet++;
            }
        }

        // --- the drifting streaks of the trees near ---
        for (MeerTerrein.Boom b : nabij) {
            long vorige = Long.MIN_VALUE;
            long[] sliert = sliert(m, b);
            java.util.Arrays.sort(sliert);
            for (long s : sliert) {
                int x = (int) (s >> 32), z = (int) s;
                if ((x >> 4) == cx && (z >> 4) == cz && water(k, x, z)) {
                    // a column that was drawn twice gets a thicker drift
                    gezet += drijf(level, gebouwen, bouwHier, p.set(x, MeerTerrein.WATER + 1, z), blaadjes, s == vorige ? 2 : 1);
                }
                vorige = s;
            }
        }

        // --- the clusters: lilies and reeds in sheltered corners, coral, shells (candidates of this chunk and its neighbours) ---
        BlockState lelie = Bio.blok("guh_waterlelie", Blocks.LILY_PAD).defaultBlockState();
        BlockState koraal = Bio.blok("kaaskoraal", Blocks.AIR).defaultBlockState();
        BlockState schelp = Bio.blok("reuzenschelp", Blocks.AIR).defaultBlockState();
        for (int ax = -1; ax <= 1; ax++) {
            for (int az = -1; az <= 1; az++) {
                int bx = cx + ax, bz = cz + az;
                long h = m.hash(bx, bz, 4210);
                Random cr = new Random(h);
                // a lily corner: the first of a few spots of that chunk that is shallow and sheltered
                if (BioModel.kans(h, 2) < LELIE_KANS) {
                    int[] hoek = luwePlek(m, bx, bz, h, 100, LELIE_POGINGEN);
                    if (hoek != null) {
                        int n = 5 + cr.nextInt(4);
                        for (int i = 0; i < n; i++) {
                            int x = hoek[0] + (int) Math.round(cr.nextGaussian() * 1.8), z = hoek[1] + (int) Math.round(cr.nextGaussian() * 1.8);
                            if ((x >> 4) == cx && (z >> 4) == cz && water(k, x, z) && MeerTerrein.WATER - k.hoogte[Kaart.index(x, z)] <= 3) {
                                gezet += drijf(level, gebouwen, bouwHier, p.set(x, MeerTerrein.WATER + 1, z), lelie, 0);
                            }
                        }
                        if (BioModel.kans(h, 3) < 0.7) {
                            gezet += riet(level, m, k, gebouwen, bouwHier, hoek[0], hoek[1], cr);
                        }
                    }
                }
                if (BioModel.kans(h, 6) < RIET_KANS) {
                    int[] hoek = luwePlek(m, bx, bz, h, 200, RIET_POGINGEN);
                    if (hoek != null) {
                        gezet += riet(level, m, k, gebouwen, bouwHier, hoek[0], hoek[1], cr);
                    }
                }
                int qx = (bx << 4) + (int) (BioModel.kans(h, 7) * 16), qz = (bz << 4) + (int) (BioModel.kans(h, 8) * 16);
                if (BioModel.kans(h, 9) < KORAAL_KANS && !koraal.isAir()) {
                    int n = 3 + cr.nextInt(4);
                    for (int i = 0; i < n; i++) {
                        int x = qx + (int) Math.round(cr.nextGaussian() * 1.3), z = qz + (int) Math.round(cr.nextGaussian() * 1.3);
                        if ((x >> 4) == cx && (z >> 4) == cz && water(k, x, z)) {
                            int bodem = k.hoogte[Kaart.index(x, z)], diepte = MeerTerrein.WATER - bodem;
                            if (diepte >= 3 && diepte <= 5) {
                                gezet += opBodem(level, gebouwen, bouwHier, p.set(x, bodem + 1, z), koraal);
                            }
                        }
                    }
                }
                int sx = (bx << 4) + (int) (BioModel.kans(h, 10) * 16), sz = (bz << 4) + (int) (BioModel.kans(h, 11) * 16);
                if (BioModel.kans(h, 12) < SCHELP_KANS && !schelp.isAir() && ax == 0 && az == 0 && water(k, sx, sz)) {
                    int bodem = k.hoogte[Kaart.index(sx, sz)], diepte = MeerTerrein.WATER - bodem;
                    if (diepte >= 2 && diepte <= 4) {
                        BlockState s = schelp;
                        if (s.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                            s = s.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.from2DDataValue((int) (BioModel.kans(h, 13) * 4)));
                        }
                        if (s.getBlock().getStateDefinition().getProperty("parel") instanceof BooleanProperty parel) {
                            s = s.setValue(parel, BioModel.kans(h, 14) < 0.3);
                        }
                        gezet += opBodem(level, gebouwen, bouwHier, p.set(sx, bodem + 1, sz), s);
                    }
                }
            }
        }
        return gezet;
    }

    /** Lake water at a column of the chunk whose map this is. */
    private static boolean water(Kaart k, int x, int z) {
        int o = Kaart.index(x, z);
        return k.water[o] == MeerTerrein.WATER && k.terras[o] < 0;
    }

    /** The first of a chunk's hashed spots that is lake water 1-2 deep in a sheltered corner ({@link #LUW}), or null. Pure; single columns only. */
    static int[] luwePlek(BioModel m, int cx, int cz, long h, int n0, int pogingen) {
        for (int i = 0; i < pogingen; i++) {
            int x = (cx << 4) + (int) (BioModel.kans(h, n0 + 2 * i) * 16), z = (cz << 4) + (int) (BioModel.kans(h, n0 + 2 * i + 1) * 16);
            int diepte = MeerTerrein.diepte(m, x, z);
            if (diepte >= 1 && diepte <= 2 && luw(m, x, z) >= LUW) {
                return new int[]{x, z};
            }
        }
        return null;
    }

    private static boolean gebouw(StructureManager gebouwen, BlockPos p) {
        return gebouwen != null && gebouwen.getStructureWithPieceAt(p, h -> true).isValid();
    }

    /** A lily (dichtheid 0) or petals on the water at p (the air block above a water source). */
    private static int drijf(WorldGenLevel level, StructureManager gebouwen, boolean bouwHier, BlockPos.MutableBlockPos p, BlockState wat, int dichtheid) {
        if (wat.isAir() || !level.getBlockState(p).isAir()) {
            return 0;
        }
        var onder = level.getFluidState(p.below());
        if (!onder.is(Fluids.WATER) || !onder.isSource() || bouwHier && gebouw(gebouwen, p)) {
            return 0;
        }
        if (dichtheid > 1 && wat.getBlock().getStateDefinition().getProperty("dichtheid") instanceof IntegerProperty ip) {
            wat = wat.setValue(ip, dichtheid);
        }
        level.setBlock(p, wat, 2);
        return 1;
    }

    /** Something waterlogged on the bed at p (which must be plain water above a firm bed). */
    private static int opBodem(WorldGenLevel level, StructureManager gebouwen, boolean bouwHier, BlockPos.MutableBlockPos p, BlockState wat) {
        if (!level.getBlockState(p).is(Blocks.WATER) || !level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP) || bouwHier && gebouw(gebouwen, p)) {
            return 0;
        }
        if (wat.hasProperty(BlockStateProperties.WATERLOGGED)) {
            wat = wat.setValue(BlockStateProperties.WATERLOGGED, true);
        }
        level.setBlock(p, wat, 2);
        return 1;
    }

    /**
     * A clump of bloesemriet on the bank nearest to the water column (wx, wz): the low bank columns within three blocks of
     * it that touch water; only those of the chunk whose map k is are placed (its neighbours place theirs).
     */
    private static int riet(WorldGenLevel level, BioModel m, Kaart k, StructureManager gebouwen, boolean bouwHier, int wx, int wz, Random cr) {
        int ox = 0, oz = 0, best = 99;
        for (int r = 0; r < 8; r++) {
            for (int a = 1; a < best && a <= 7; a++) {
                int x = wx + MeerTerrein.RX[r] * a, z = wz + MeerTerrein.RZ[r] * a;
                long kol = MeerTerrein.los(m, x, z);
                if (kol == MeerTerrein.GEEN_MEER) {
                    break;
                }
                int top = (int) (kol >> 8);
                if (top >= MeerTerrein.WATER) {
                    if (top > MeerTerrein.WATER && top <= MeerTerrein.WATER + 2 && (kol & MeerTerrein.STEEN) == 0) {
                        best = a;
                        ox = x;
                        oz = z;
                    }
                    break;
                }
            }
        }
        if (best == 99) {
            return 0;
        }
        int gezet = 0, x0 = k.cx << 4, z0 = k.cz << 4;
        BlockState onder = RIET.get().defaultBlockState().setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState boven = onder.setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                int x = ox + dx, z = oz + dz;
                // (the draw is taken for every column, so each chunk sees the same clump)
                boolean wil = cr.nextFloat() < 0.6f;
                if (!wil || dx * dx + dz * dz > 7 || x < x0 || x >= x0 + 16 || z < z0 || z >= z0 + 16) {
                    continue;
                }
                int o = Kaart.index(x, z), y = k.hoogte[o] + 1;
                if (k.terras[o] >= 0 || k.meng[o] < 1f || k.water[o] != Kaart.GEEN || (k.vlag[o] & MeerTerrein.STEEN) != 0 || y > MeerTerrein.WATER + 3) {
                    continue;
                }
                boolean aanWater = false;
                for (int r = 0; r < 8 && !aanWater; r++) {
                    int nx = x + MeerTerrein.RX[r], nz = z + MeerTerrein.RZ[r];
                    aanWater = nx >= x0 && nx < x0 + 16 && nz >= z0 && nz < z0 + 16 ? water(k, nx, nz) : MeerTerrein.nat(m, nx, nz);
                }
                if (aanWater && level.getBlockState(p.set(x, y, z)).isAir() && level.getBlockState(p.set(x, y + 1, z)).isAir()
                        && onder.canSurvive(level, p.set(x, y, z)) && !(bouwHier && gebouw(gebouwen, p))) {
                    level.setBlock(p.set(x, y, z), onder, 2);
                    level.setBlock(p.set(x, y + 1, z), boven, 2);
                    gezet++;
                }
            }
        }
        return gezet;
    }
}
