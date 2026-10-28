package nl.juiced.guhs.feature.zingendeweide;

import java.util.Optional;
import java.util.function.IntBinaryOperator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.registry.ModEntities;

/**
 * The three structures of De Zingende Weide (structure type {@code guhs:weide_bouw}, field {@code soort}): one procedural
 * piece each, drawn chunk by chunk with {@link WeideTeken} (the same random for every chunk, so the build stays whole).
 * <ul>
 *   <li>{@code koorkom}: a bowl with rings of grass seats around a little stage with the {@link KoorkomPodium}, lanterns on
 *       the rim and three pillars with guh ears. Wild Blubs come here at dusk to sing.</li>
 *   <li>{@code poeltjes}: three bubbelsap pools stepping down, with waterfalls between them, and a bath hut where Badmeester
 *       Bubbels ({@link BadGuh}) hands out towels.</li>
 *   <li>{@code guhwand}: a great striped cliff wall with a friendly guh face in it (round eyes with a little shine, blushes,
 *       a :3 mouth), round ears on top and a bubbelsap waterfall into a pool at its foot.</li>
 * </ul>
 */
public final class WeideBouw {
    private WeideBouw() {
    }

    /** How far each build reaches from its middle (horizontally), and how high. */
    static int bereik(String soort) {
        return switch (soort) {
            case "guhwand" -> 24;
            case "poeltjes" -> 24;
            default -> 22;
        };
    }

    public static class Structuur extends Structure {
        public static final MapCodec<Structuur> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                settingsCodec(i),
                Codec.STRING.fieldOf("soort").forGetter(s -> s.soort)
        ).apply(i, Structuur::new));

        final String soort;

        public Structuur(StructureSettings settings, String soort) {
            super(settings);
            this.soort = soort;
        }

        @Override
        protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
            ChunkPos c = context.chunkPos();
            int x = c.getMiddleBlockX(), z = c.getMiddleBlockZ();
            ChunkGenerator gen = context.chunkGenerator();
            int y = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
            int bodem = gen.getFirstOccupiedHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
            if (y - bodem > 1 || y < 60) {
                return Optional.empty();      // (water here: no build)
            }
            // not on a cliff: the four corners of the build's heart within a few blocks of the middle
            int r = 10;
            for (int[] d : new int[][]{{r, r}, {-r, r}, {r, -r}, {-r, -r}}) {
                int h = gen.getFirstOccupiedHeight(x + d[0], z + d[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
                if (Math.abs(h - y) > 6) {
                    return Optional.empty();
                }
            }
            long seed = context.seed() ^ (c.pack() * 0x9E3779B97F4A7C15L) ^ soort.hashCode();
            Direction richting = Direction.from2DDataValue((int) Math.floorMod(seed >>> 7, 4L));
            BlockPos pos = new BlockPos(x, y, z);
            return Optional.of(new GenerationStub(pos, b -> b.addPiece(new Stuk(soort, x, y, z, seed, richting))));
        }

        @Override
        public StructureType<?> type() {
            return ZingendeWeideFeature.BOUW.get();
        }
    }

    public static class Stuk extends StructurePiece {
        final String soort;
        final int x, y, z;
        final long seed;
        final Direction richting;

        public Stuk(String soort, int x, int y, int z, long seed, Direction richting) {
            super(ZingendeWeideFeature.BOUW_STUK.get(), 0, doos(soort, x, y, z));
            this.soort = soort;
            this.x = x;
            this.y = y;
            this.z = z;
            this.seed = seed;
            this.richting = richting;
        }

        public Stuk(CompoundTag tag) {
            super(ZingendeWeideFeature.BOUW_STUK.get(), tag);
            this.soort = tag.getStringOr("soort", "koorkom");
            this.x = tag.getIntOr("x", 0);
            this.y = tag.getIntOr("y", 64);
            this.z = tag.getIntOr("z", 0);
            this.seed = tag.getLongOr("seed", 0L);
            this.richting = Direction.from2DDataValue(tag.getIntOr("richting", 0));
        }

        static BoundingBox doos(String soort, int x, int y, int z) {
            int r = bereik(soort);
            return new BoundingBox(x - r, y - 12, z - r, x + r, y + ("guhwand".equals(soort) ? 64 : 34), z + r);
        }

        @Override
        protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
            tag.putString("soort", soort);
            tag.putInt("x", x);
            tag.putInt("y", y);
            tag.putInt("z", z);
            tag.putLong("seed", seed);
            tag.putInt("richting", richting.get2DDataValue());
        }

        @Override
        public void postProcess(WorldGenLevel level, StructureManager structures, ChunkGenerator generator, RandomSource random, BoundingBox chunk,
                                ChunkPos chunkPos, BlockPos reference) {
            WeideTeken.Zet zet = new WeideTeken.Zet() {
                final BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();

                @Override
                public void zet(int a, int b, int c, BlockState state) {
                    p.set(a, b, c);
                    if (chunk.isInside(p)) {
                        level.setBlock(p, state, Block.UPDATE_CLIENTS);
                        if (!state.getFluidState().isEmpty()) {
                            level.scheduleTick(p.immutable(), state.getFluidState().getType(), 0);
                        }
                    }
                }

                @Override
                public BlockState get(int a, int b, int c) {
                    return level.getBlockState(p.set(a, b, c));
                }
            };
            IntBinaryOperator hoogte = (a, c) -> level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, a, c);
            bouw(soort, zet, hoogte, RandomSource.create(seed), x, y, z, richting, (pos) -> {
                if (chunk.isInside(pos)) {
                    badguh(level, pos);
                }
            });
        }
    }

    /** Draws one build (the structure's piece, or the test command straight into the world). */
    public static void bouw(String soort, WeideTeken.Zet z, IntBinaryOperator hoogte, RandomSource r, int x, int y, int zz, Direction richting,
                            java.util.function.Consumer<BlockPos> npc) {
        switch (soort) {
            case "poeltjes" -> poeltjes(z, hoogte, r, x, y, zz, richting, npc);
            case "guhwand" -> guhwand(z, hoogte, r, x, y, zz, richting);
            default -> koorkom(z, hoogte, r, x, y, zz);
        }
    }

    static void badguh(net.minecraft.world.level.ServerLevelAccessor level, BlockPos pos) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (npc == null) {
            return;
        }
        npc.setKind(ZingendeWeideFeature.BADGUH);
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180f, 0f);
        npc.setPersistenceRequired();
        level.addFreshEntityWithPassengers(npc);
    }

    // --- de Koorkom -------------------------------------------------------------------------------------------------------------
    static void koorkom(WeideTeken.Zet z, IntBinaryOperator hoogte, RandomSource r, int cx, int y0, int cz) {
        int straal = 13;
        int bodem = y0 - 4;
        BlockState planken = WeideTeken.s(ZingendeWeideFeature.PLANKEN.get()), lucht = Blocks.AIR.defaultBlockState();
        BlockState rand = WeideTeken.s(ZingendeWeideFeature.GEPOLIJSTE_ZEEGROEN_STEEN.get());
        for (int a = -straal - 2; a <= straal + 2; a++) {
            for (int b = -straal - 2; b <= straal + 2; b++) {
                double d = Math.sqrt(a * a + b * b);
                int x = cx + a, zz = cz + b;
                if (d <= straal) {
                    int ring = d < 3.5 ? 0 : Math.min(5, 1 + (int) ((d - 3.5) / 2.0));
                    int vloer = bodem + ring;
                    for (int y = vloer - 8; y < vloer; y++) {
                        z.zet(x, y, zz, WeideTeken.klei());
                    }
                    BlockState top = ring == 0 ? planken : (ring % 2 == 0 ? WeideTeken.groen() : WeideTeken.gras());
                    // the front edge of each seat: polished stone
                    if (ring > 0 && (d - 3.5) % 2.0 < 0.75) {
                        top = rand;
                    }
                    z.zet(x, vloer, zz, top);
                    for (int y = vloer + 1; y <= y0 + 12; y++) {
                        z.zet(x, y, zz, lucht);
                    }
                } else if (d <= straal + 2) {
                    // the rim: up to the bowl's top ring, grass, and the old ground below filled
                    int top = Math.max(bodem + 5, Math.min(hoogte.applyAsInt(x, zz) - 1, bodem + 7));
                    for (int y = top - 8; y < top; y++) {
                        z.zet(x, y, zz, WeideTeken.klei());
                    }
                    z.zet(x, top, zz, WeideTeken.gras());
                    for (int y = top + 1; y <= y0 + 12; y++) {
                        BlockState s = z.get(x, y, zz);
                        if (!s.isAir() && (s.canBeReplaced() || s.getFluidState().isEmpty() && !WeideTeken.grond(s))) {
                            z.zet(x, y, zz, lucht);
                        }
                    }
                }
            }
        }
        // the stage: the podium in the middle, singing flowers around it
        z.zet(cx, bodem, cz, WeideTeken.s(ZingendeWeideFeature.KOORKOM_PODIUM.get()));
        for (int i = 0; i < 8; i++) {
            double hoek = i * Math.PI / 4;
            int px = cx + (int) Math.round(Math.cos(hoek) * 2.4), pz = cz + (int) Math.round(Math.sin(hoek) * 2.4);
            Block bloem = switch (i % 3) {
                case 0 -> ZingendeWeideFeature.ZINGBLOEMETJE_ROZE.get();
                case 1 -> ZingendeWeideFeature.ZINGBLOEMETJE_GEEL.get();
                default -> ZingendeWeideFeature.ZINGBLOEMETJE_BLAUW.get();
            };
            z.zet(px, bodem, pz, WeideTeken.groen());
            z.zet(px, bodem + 1, pz, WeideTeken.s(bloem));
        }
        // lanterns on the rim
        for (int i = 0; i < 10; i++) {
            double hoek = i * Math.PI / 5 + 0.3;
            int px = cx + (int) Math.round(Math.cos(hoek) * (straal + 1)), pz = cz + (int) Math.round(Math.sin(hoek) * (straal + 1));
            int top = Math.max(bodem + 5, Math.min(hoogte.applyAsInt(px, pz) - 1, bodem + 7));
            z.zet(px, top + 1, pz, WeideTeken.s(ZingendeWeideFeature.HEK.get()));
            z.zet(px, top + 2, pz, WeideTeken.s(Blocks.LANTERN).setValue(LanternBlock.HANGING, false));
        }
        // three pillars with ears and a few crystals around
        double start = r.nextDouble() * Math.PI * 2;
        for (int i = 0; i < 3; i++) {
            double hoek = start + i * Math.PI * 2 / 3 + (r.nextDouble() - 0.5) * 0.6;
            int px = cx + (int) Math.round(Math.cos(hoek) * (straal + 5)), pz = cz + (int) Math.round(Math.sin(hoek) * (straal + 5));
            int py = Math.max(bodem + 5, hoogte.applyAsInt(px, pz));
            WeideTeken.pilaar(z, r, px, py, pz, 13 + r.nextInt(10), 1.8 + r.nextDouble() * 0.8, 0, 0, r.nextInt(3), true, false);
            WeideTeken.kristallen(z, r, px + 3, Math.max(bodem + 6, hoogte.applyAsInt(px + 3, pz)), pz);
        }
    }

    // --- de Bubbelsap-poeltjes ----------------------------------------------------------------------------------------------------
    static void poeltjes(WeideTeken.Zet z, IntBinaryOperator hoogte, RandomSource r, int cx, int y0, int cz, Direction dir,
                         java.util.function.Consumer<BlockPos> npc) {
        Direction dwars = dir.getClockWise();
        // three pools stepping down along dir: the top one on a mound
        int[][] poelen = {{-8, 3, 4}, {0, 0, 5}, {8, -3, 4}};      // {offset along dir, height, radius}
        for (int[] p : poelen) {
            int px = cx + dir.getStepX() * p[0], pz = cz + dir.getStepZ() * p[0];
            int py = y0 + p[1];
            if (p[1] > 0) {
                // the mound under the high pool
                for (int a = -p[2] - 3; a <= p[2] + 3; a++) {
                    for (int b = -p[2] - 3; b <= p[2] + 3; b++) {
                        double d = Math.sqrt(a * a + b * b);
                        if (d > p[2] + 3) {
                            continue;
                        }
                        int top = py - 1 - (int) Math.max(0, (d - p[2] - 0.5) * 1.2);
                        for (int y = y0 - 3; y <= top; y++) {
                            z.zet(px + a, y, pz + b, y == top ? WeideTeken.groen() : WeideTeken.streep(y));
                        }
                    }
                }
            }
            WeideTeken.poel(z, r, px, py, pz, p[2] + 0.4, p[2] - 0.3, 2);
        }
        // the waterfalls: a gap in the lip of the two upper pools, towards the next one
        for (int i = 0; i < 2; i++) {
            int[] p = poelen[i];
            int lx = cx + dir.getStepX() * (p[0] + p[2] + 1), lz = cz + dir.getStepZ() * (p[0] + p[2] + 1);
            int ly = y0 + p[1] - 1;
            for (int w = -1; w <= 1; w++) {
                int gx = lx + dwars.getStepX() * w, gz = lz + dwars.getStepZ() * w;
                z.zet(gx, ly, gz, WeideTeken.sap());
                z.zet(gx, ly - 1, gz, WeideTeken.streep(ly - 1));
            }
        }
        // the bath hut beside the middle pool, its open front to the pool
        int hx = cx + dwars.getStepX() * 10, hz = cz + dwars.getStepZ() * 10;
        int hy = Math.max(y0, hoogte.applyAsInt(hx, hz));
        badhuisje(z, r, hx, hy, hz, dwars.getOpposite(), npc);
        // pillars with ears and crystals on the far side
        for (int i = 0; i < 2; i++) {
            int along = i == 0 ? -11 : 12;
            int px = cx + dir.getStepX() * along - dwars.getStepX() * 9, pz = cz + dir.getStepZ() * along - dwars.getStepZ() * 9;
            int py = hoogte.applyAsInt(px, pz);
            WeideTeken.pilaar(z, r, px, py, pz, 16 + r.nextInt(12), 2.0 + r.nextDouble(), 0, 0, 2 + r.nextInt(3), true, i == 0);
        }
        WeideTeken.kristallen(z, r, cx - dwars.getStepX() * 6, y0, cz - dwars.getStepZ() * 6);
    }

    /** The bath hut: 5 x 5 of wolkjeshout, open towards the pool, a counter with towels, a lantern, Badmeester Bubbels inside. */
    static void badhuisje(WeideTeken.Zet z, RandomSource r, int x, int y0, int zz, Direction voor, java.util.function.Consumer<BlockPos> npc) {
        Direction zij = voor.getClockWise();
        BlockState planken = WeideTeken.s(ZingendeWeideFeature.PLANKEN.get()), stam = WeideTeken.s(ZingendeWeideFeature.GESTRIPT.get());
        BlockState plaat = WeideTeken.s(ZingendeWeideFeature.PLAAT.get()), lucht = Blocks.AIR.defaultBlockState();
        for (int a = -2; a <= 2; a++) {
            for (int b = -2; b <= 2; b++) {
                int px = x + zij.getStepX() * a + voor.getStepX() * b, pz = zz + zij.getStepZ() * a + voor.getStepZ() * b;
                for (int y = y0 - 4; y < y0; y++) {
                    z.zet(px, y, pz, WeideTeken.klei());
                }
                z.zet(px, y0 - 1, pz, planken);
                boolean hoek = Math.abs(a) == 2 && Math.abs(b) == 2, wand = Math.abs(a) == 2 || b == -2;
                for (int y = y0; y <= y0 + 3; y++) {
                    BlockState st = hoek ? stam : wand && b != 2 ? (y == y0 + 1 && a == 0 ? lucht : planken) : lucht;
                    if (b == 2 && !hoek) {
                        st = y == y0 ? WeideTeken.s(ZingendeWeideFeature.PLAAT.get()).setValue(SlabBlock.TYPE, SlabType.TOP) : lucht;
                    }
                    z.zet(px, y, pz, st);
                }
                z.zet(px, y0 + 4, pz, plaat);
            }
        }
        // the roof overhang to the front, ears on top, towels on the counter, a lantern
        for (int a = -2; a <= 2; a++) {
            int px = x + zij.getStepX() * a + voor.getStepX() * 3, pz = zz + zij.getStepZ() * a + voor.getStepZ() * 3;
            z.zet(px, y0 + 4, pz, plaat);
        }
        WeideTeken.oortjes(z, x, y0 + 5, zz, 2, zij.getAxis() == Direction.Axis.X);
        for (int a = -1; a <= 1; a += 2) {
            int px = x + zij.getStepX() * a + voor.getStepX() * 2, pz = zz + zij.getStepZ() * a + voor.getStepZ() * 2;
            z.zet(px, y0 + 1, pz, WeideTeken.s(a < 0 ? Blocks.LIGHT_BLUE_CARPET : Blocks.PINK_CARPET));
        }
        z.zet(x - voor.getStepX(), y0 + 3, zz - voor.getStepZ(), WeideTeken.s(Blocks.LANTERN).setValue(LanternBlock.HANGING, true));
        npc.accept(new BlockPos(x, y0, zz));
    }

    // --- de Guhwand ----------------------------------------------------------------------------------------------------------------
    static void guhwand(WeideTeken.Zet z, IntBinaryOperator hoogte, RandomSource r, int cx, int y0, int cz, Direction voor) {
        Direction langs = voor.getClockWise();
        int breed = 17, dik = 9, hoog = 44;
        BlockState lucht = Blocks.AIR.defaultBlockState();
        int[] tops = new int[2 * breed + 1];
        for (int u = -breed; u <= breed; u++) {
            double t = u / (double) breed;
            int top = y0 + (int) Math.round(hoog * (1 - 0.35 * t * t) + Math.sin(u * 0.45) * 1.5);
            tops[u + breed] = top;
            int boog = (int) Math.round(t * t * 3);                 // (a gentle curve: the ends come forward)
            for (int v = 0; v < dik; v++) {
                int vv = v - boog;
                int x = cx + langs.getStepX() * u - voor.getStepX() * vv, zz = cz + langs.getStepZ() * u - voor.getStepZ() * vv;
                // down to the ground (never floating), up to the top
                int onder = Math.min(y0 - 3, hoogte.applyAsInt(x, zz) - 1);
                for (int y = onder; y <= top; y++) {
                    z.zet(x, y, zz, y == top ? WeideTeken.groen() : WeideTeken.streep(y));
                }
                if (r.nextInt(5) == 0) {
                    WeideFeatures.plantje(z, x, top + 1, zz, r);
                }
            }
        }
        // the face on the front (the v = 0 layer, curved like the wall)
        int midden = y0 + (int) (hoog * 0.55);
        java.util.function.BiConsumer<int[], BlockState> gezicht = (uy, st) -> {
            int u = uy[0];
            double t = u / (double) breed;
            int boog = (int) Math.round(t * t * 3);
            int x = cx + langs.getStepX() * u + voor.getStepX() * boog, zz = cz + langs.getStepZ() * u + voor.getStepZ() * boog;
            z.zet(x, uy[1], zz, st);
        };
        BlockState zwart = WeideTeken.s(Blocks.BLACK_CONCRETE), wit = WeideTeken.s(Blocks.WHITE_CONCRETE), blos = WeideTeken.s(Blocks.PINK_TERRACOTTA);
        for (int oog = -1; oog <= 1; oog += 2) {
            int ou = oog * 6, oy = midden + 3;
            for (int a = -3; a <= 3; a++) {
                for (int b = -4; b <= 4; b++) {
                    if ((a / 3.3) * (a / 3.3) + (b / 4.3) * (b / 4.3) <= 1.0) {
                        gezicht.accept(new int[]{ou + a, oy + b}, zwart);
                    }
                }
            }
            // a little shine: two light spots, up and to one side
            gezicht.accept(new int[]{ou - 1, oy + 2}, wit);
            gezicht.accept(new int[]{ou - 2, oy + 2}, wit);
            gezicht.accept(new int[]{ou - 1, oy + 1}, wit);
            gezicht.accept(new int[]{ou + 1, oy - 2}, wit);
            // the blush
            for (int a = -2; a <= 2; a++) {
                for (int b = -1; b <= 0; b++) {
                    gezicht.accept(new int[]{oog * 11 + a, midden - 1 + b}, blos);
                }
            }
        }
        // the :3 mouth
        int[][] mond = {{-3, 0}, {-2, -1}, {-1, -1}, {0, 0}, {1, -1}, {2, -1}, {3, 0}};
        for (int[] m : mond) {
            gezicht.accept(new int[]{m[0], midden - 3 + m[1]}, zwart);
        }
        // ears on top, in the plane of the face
        int topMidden = tops[breed];
        WeideTeken.oortjes(z, cx - voor.getStepX() * (dik / 2), topMidden + 1, cz - voor.getStepZ() * (dik / 2), 10, langs.getAxis() == Direction.Axis.X);
        // a bubbelsap spring high up on the face, into a pool at the foot
        int wu = breed / 2 + 2;
        int wtop = tops[wu + breed] - 4;
        double t = wu / (double) breed;
        int boog = (int) Math.round(t * t * 3);
        int wx = cx + langs.getStepX() * wu + voor.getStepX() * (boog + 1), wz = cz + langs.getStepZ() * wu + voor.getStepZ() * (boog + 1);
        z.zet(wx - voor.getStepX(), wtop, wz - voor.getStepZ(), WeideTeken.sap());
        for (int y = wtop + 1; y <= wtop + 2; y++) {
            z.zet(wx - voor.getStepX(), y, wz - voor.getStepZ(), lucht);
        }
        int px = wx + voor.getStepX() * 3, pz = wz + voor.getStepZ() * 3;
        WeideTeken.poel(z, r, px, Math.min(y0, hoogte.applyAsInt(px, pz)), pz, 3.5, 3.5, 2);
        WeideTeken.kristallen(z, r, cx - langs.getStepX() * 10 + voor.getStepX() * 4, y0, cz - langs.getStepZ() * 10 + voor.getStepZ() * 4);
    }
}
