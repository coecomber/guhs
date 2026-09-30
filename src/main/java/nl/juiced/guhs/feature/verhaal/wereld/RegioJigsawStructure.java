package nl.juiced.guhs.feature.verhaal.wereld;

import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.feature.verhaal.VerhaalFeature;
import nl.juiced.guhs.world.BouwRuimte;
import nl.juiced.guhs.world.grond.Grond;

/**
 * Structure type {@code guhs:regio_jigsaw} (3.0): ONE building per region (Nomguh per Sneeuwguhtoendra, the Guhwai'i
 * buildings per island, the kloon-eiland per deep sea), on the spot of its {@link RegioPlek} (the placement
 * {@link RegioPiekPlacement} hands out the same chunk). One template (up to ~256 x 256: the start pool's element, no child
 * pieces), placed UNROTATED ({@link Rotation#NONE}: Nomguh's route file uses template coordinates) with its start jigsaw
 * ({@code start_jigsaw_name}) on the ground: the top block at the spot ({@code WORLD_SURFACE_WG}), or {@code vaste_y} as the
 * first free y (the sea has no water yet while structures are placed, so a sea building asks for a fixed height). 2.10 grond:
 * the start element is a {@code guhs:grond_single_pool_element} (tools/make_v2.py grond() via the regio hook: delta =
 * {@code grond_y} + 1), so the start y comes from {@link Grond#startY}. A {@code zee} spot also checks the real sea floor
 * ({@code diep} blocks under {@code water_level}) under the spot and on its ring.
 */
public class RegioJigsawStructure extends Structure implements BouwRuimte.Ruimte {
    public static final MapCodec<RegioJigsawStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
            ResourceLocation.CODEC.fieldOf("start_jigsaw_name").forGetter(s -> s.startJigsawName),
            RegioPlek.CODEC.fieldOf("plek").forGetter(s -> s.plek),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(s -> s.keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang),
            Codec.INT.optionalFieldOf("vaste_y").forGetter(s -> s.vasteY),
            Codec.INT.optionalFieldOf("grond_y", 0).forGetter(s -> s.grondY),
            Codec.INT.optionalFieldOf("reach", 0).forGetter(s -> s.reach),
            Codec.INT.optionalFieldOf("diep", 12).forGetter(s -> s.diep),
            Codec.INT.optionalFieldOf("water_level", 63).forGetter(s -> s.waterLevel)
    ).apply(i, RegioJigsawStructure::new));

    private final Holder<StructureTemplatePool> startPool;
    private final ResourceLocation startJigsawName;
    private final RegioPlek plek;
    private final int keepClear;
    private final Optional<Integer> voorrang;
    private final Optional<Integer> vasteY;
    private final int grondY;
    private final int reach;
    private final int diep;
    private final int waterLevel;

    public RegioJigsawStructure(StructureSettings settings, Holder<StructureTemplatePool> startPool, ResourceLocation startJigsawName, RegioPlek plek,
                                int keepClear, Optional<Integer> voorrang, Optional<Integer> vasteY, int grondY, int reach, int diep, int waterLevel) {
        super(settings);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.plek = plek;
        this.keepClear = keepClear;
        this.voorrang = voorrang;
        this.vasteY = vasteY;
        this.grondY = grondY;
        this.reach = reach;
        this.diep = diep;
        this.waterLevel = waterLevel;
    }

    public RegioPlek plek() {
        return plek;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        Optional<RegioPlek.Plek> spot = plek.plekVoorChunk(context.seed(), chunk.x, chunk.z);
        if (spot.isEmpty() || (spot.get().x() >> 4) != chunk.x || (spot.get().z() >> 4) != chunk.z) {
            return Optional.empty();
        }
        int x = spot.get().x(), z = spot.get().z();
        if (plek.soort() == RegioPlek.Soort.ZEE && !zeeDiep(context, x, z)) {
            return Optional.empty();
        }
        int surface = vasteY.orElseGet(() -> context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(), context.randomState()));
        return BouwRuimte.claim(context, this, onGedraaid(context, new BlockPos(x, Grond.startY(startPool, surface), z)));
    }

    /** The real sea floor under the spot and on the ring (and a middle ring) lies at least `diep` under the water level. */
    private boolean zeeDiep(GenerationContext context, int x, int z) {
        if (!diep(context, x, z)) {
            return false;
        }
        for (int r : new int[]{plek.ring() / 2, plek.ring()}) {
            for (int k = 0; k < 8 && r > 0; k++) {
                double a = Math.PI / 4 * k;
                if (!diep(context, x + (int) Math.round(Math.cos(a) * r), z + (int) Math.round(Math.sin(a) * r))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean diep(GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState())
                <= waterLevel - diep;
    }

    /**
     * Vanilla's JigsawPlacement.addPieces for one start piece, but always unrotated (vanilla picks a random rotation): the
     * start jigsaw lands on pos (its y the way vanilla does it without heightmap projection).
     */
    private Optional<GenerationStub> onGedraaid(GenerationContext context, BlockPos pos) {
        var manager = context.structureTemplateManager();
        StructurePoolElement element = startPool.value().getRandomTemplate(context.random());
        if (element == EmptyPoolElement.INSTANCE) {
            return Optional.empty();
        }
        BlockPos jig = null;
        List<StructureTemplate.StructureBlockInfo> jigsaws = element.getShuffledJigsawBlocks(manager, pos, Rotation.NONE, context.random());
        for (StructureTemplate.StructureBlockInfo info : jigsaws) {
            if (info.nbt() != null && startJigsawName.equals(ResourceLocation.tryParse(info.nbt().getString("name")))) {
                jig = info.pos();
                break;
            }
        }
        if (jig == null) {
            com.mojang.logging.LogUtils.getLogger().error("regio_jigsaw: no start jigsaw {} in {}", startJigsawName, startPool);
            return Optional.empty();
        }
        Vec3i off = jig.subtract(pos);
        BlockPos origin = pos.subtract(off);
        PoolElementStructurePiece piece = new PoolElementStructurePiece(manager, element, origin, element.getGroundLevelDelta(), Rotation.NONE,
                element.getBoundingBox(manager, origin, Rotation.NONE), LiquidSettings.IGNORE_WATERLOGGING);
        BoundingBox box = piece.getBoundingBox();
        int cx = (box.maxX() + box.minX()) / 2, cz = (box.maxZ() + box.minZ()) / 2;
        int k = origin.getY();
        int l = box.minY() + piece.getGroundLevelDelta();
        piece.move(0, k - l, 0);
        int y = k + off.getY();
        return Optional.of(new GenerationStub(new BlockPos(cx, y, cz), builder -> builder.addPiece(piece)));
    }

    @Override
    public int keepClear() {
        return keepClear;
    }

    @Override
    public int voorrang() {
        return voorrang.orElse(keepClear);
    }

    public int grondY() {
        return grondY;
    }

    public int reach() {
        return reach;
    }

    @Override
    public StructureType<?> type() {
        return VerhaalFeature.REGIO_STRUCTURE.get();
    }
}
