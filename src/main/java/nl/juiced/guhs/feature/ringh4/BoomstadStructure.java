package nl.juiced.guhs.feature.ringh4;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import nl.juiced.guhs.feature.spiesburcht.BurchtStructure;
import nl.juiced.guhs.world.BouwRuimte;

/**
 * bbq2 (ring-h4): the placement of the tree city of Guhladriel ({@code guhs:guhladriel_boomstad}, type
 * {@code guhs:ringh4_boomstad}). It is a {@link BurchtStructure} (one build in tiles that turn together around the anchor:
 * {@code Kopieen} and {@code Bezetting} treat it as one), but it does not stand in the sauce sea or on a bridge height: it
 * lies on a cave floor, and what must be right are its two ENDS, the gate in the west and the way on at the landing in the
 * east, almost 190 blocks apart:
 * <ul>
 *   <li>the gate's column has a dry cave floor above the sauce sea with room over it, low enough for the whole build (the
 *       crown of the great tree) to fit under the top of the world: template layer 0 comes in the first layer of air there,
 *       so the path at the gate starts one step above the cave floor;</li>
 *   <li>the column of the way on has a floor within {@link #VERSCHIL} blocks of that height (so nobody arrives at the far
 *       bank in front of a wall or over a pit);</li>
 *   <li>all four turns are tried, the first one that fits is taken.</li>
 * </ul>
 * Everything in between is the build's own business: it carves its dome and its gorge and lays its own ground
 * (tools/features/ring_h4_bouw.py), and {@code terrain_adaptation: beard_thin} lets the cave floor meet its edges.
 * <p>
 * The city belongs in the Satébos, but the story needs it to exist in every world: its biome tag also holds the other
 * dry biomes, and outside the preferred ones ({@code voorkeur}) only one start chunk in {@code anders} is accepted. So the
 * search for the one guaranteed copy lands in a Satébos whenever the ring around the mine has one, and still finds a spot
 * when it has none.
 */
public class BoomstadStructure extends BurchtStructure {
    /** How far the floor at the way on may differ from the floor at the gate. */
    public static final int VERSCHIL = 5;

    public static final MapCodec<BoomstadStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Identifier.CODEC.fieldOf("templates").forGetter(s -> s.templates),
            Codec.INT.fieldOf("tiles_x").forGetter(s -> s.tilesX),
            Codec.INT.fieldOf("tiles_z").forGetter(s -> s.tilesZ),
            Codec.INT.fieldOf("tile_size").forGetter(s -> s.tileSize),
            BlockPos.CODEC.fieldOf("anchor").forGetter(BurchtStructure::anchor),
            BlockPos.CODEC.fieldOf("poort").forGetter(s -> s.poort),
            BlockPos.CODEC.fieldOf("uitgang").forGetter(s -> s.uitgang),
            Codec.INT.fieldOf("hoogte").forGetter(s -> s.hoogte),
            TagKey.codec(Registries.BIOME).fieldOf("voorkeur").forGetter(s -> s.voorkeur),
            Codec.INT.optionalFieldOf("anders", 12).forGetter(s -> s.anders),
            Codec.INT.optionalFieldOf("keep_clear", 0).forGetter(BurchtStructure::keepClear),
            Codec.INT.optionalFieldOf("voorrang").forGetter(s -> s.voorrang)
    ).apply(i, BoomstadStructure::new));

    private final Identifier templates;
    private final int tilesX, tilesZ, tileSize;
    /** Template coordinates (of the whole build) of the gate and of the way on at the landing. */
    private final BlockPos poort, uitgang;
    /** The height of the build (layers). */
    private final int hoogte;
    private final TagKey<Biome> voorkeur;
    private final int anders;
    private final Optional<Integer> voorrang;

    public BoomstadStructure(StructureSettings settings, Identifier templates, int tilesX, int tilesZ, int tileSize, BlockPos anchor, BlockPos poort,
                             BlockPos uitgang, int hoogte, TagKey<Biome> voorkeur, int anders, int keepClear, Optional<Integer> voorrang) {
        super(settings, templates, tilesX, tilesZ, tileSize, anchor, "paleis", 0, 0, 0, keepClear, voorrang);
        this.templates = templates;
        this.tilesX = tilesX;
        this.tilesZ = tilesZ;
        this.tileSize = tileSize;
        this.poort = poort;
        this.uitgang = uitgang;
        this.hoogte = hoogte;
        this.voorkeur = voorkeur;
        this.anders = Math.max(1, anders);
        this.voorrang = voorrang;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(x >> 2, 48 >> 2, z >> 2, context.randomState().sampler());
        if (!biome.is(voorkeur) && Math.floorMod((x >> 4) * 73428767 ^ (z >> 4) * 912931, anders) != 0) {
            return Optional.empty();
        }
        Rotation eerste = Rotation.getRandom(context.random());
        for (int k = 0; k < 4; k++) {
            Rotation draai = Rotation.values()[(eerste.ordinal() + k) % 4];
            int y = vloer(context, x, z, draai);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            BlockPos at = new BlockPos(x, y, z);
            StructureTemplateManager manager = context.structureTemplateManager();
            List<Piece> pieces = new ArrayList<>();
            for (int i = 0; i < tilesX; i++) {
                for (int j = 0; j < tilesZ; j++) {
                    Identifier id = tile(i, j);
                    if (manager.get(id).isEmpty()) {
                        continue;
                    }
                    BlockPos offset = new BlockPos(i * tileSize, 0, j * tileSize);
                    pieces.add(new Piece(manager, id, at.subtract(anchor()).offset(offset), draai, anchor().subtract(offset)));
                }
            }
            if (pieces.isEmpty()) {
                return Optional.empty();
            }
            return BouwRuimte.claim(context, this, Optional.of(new GenerationStub(at, builder -> pieces.forEach(builder::addPiece))));
        }
        return Optional.empty();
    }

    /** The world y of template layer 0 for this turn, or MIN_VALUE when the two ends don't fit. */
    private int vloer(GenerationContext context, int x, int z, Rotation draai) {
        BlockPos p = StructureTemplate.transform(poort, Mirror.NONE, draai, anchor()).subtract(anchor());
        BlockPos u = StructureTemplate.transform(uitgang, Mirror.NONE, draai, anchor()).subtract(anchor());
        NoiseColumn bijPoort = context.chunkGenerator().getBaseColumn(x + p.getX(), z + p.getZ(), context.heightAccessor(), context.randomState());
        int laag = context.chunkGenerator().getSeaLevel() + 2;
        int hoog = context.heightAccessor().getMaxY() - hoogte - 2;
        for (int y = laag; y <= hoog; y++) {
            if (isVloer(bijPoort, y) && vrij(bijPoort, y, 5)) {
                NoiseColumn bijUitgang = context.chunkGenerator().getBaseColumn(x + u.getX(), z + u.getZ(), context.heightAccessor(), context.randomState());
                for (int d = 0; d <= VERSCHIL; d++) {
                    for (int s : new int[]{y + d, y - d}) {
                        if (s >= laag - 1 && isVloer(bijUitgang, s) && vrij(bijUitgang, s, 4)) {
                            return y;
                        }
                    }
                }
                return Integer.MIN_VALUE;   // (the lowest floor at the gate is the one that counts: no climbing to a higher gallery)
            }
        }
        return Integer.MIN_VALUE;
    }

    /** The first layer of air over dry, solid ground? */
    private static boolean isVloer(NoiseColumn column, int y) {
        BlockState onder = column.getBlock(y - 1);
        return column.getBlock(y).isAir() && !onder.isAir() && onder.getFluidState().isEmpty();
    }

    private static boolean vrij(NoiseColumn column, int y, int ruimte) {
        for (int i = 0; i < ruimte; i++) {
            if (!column.getBlock(y + i).isAir()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public StructureType<?> type() {
        return RingH4Feature.BOOMSTAD_TYPE.get();
    }
}
