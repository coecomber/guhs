package nl.juiced.guhs.world.grond;

import java.util.Optional;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.registry.ModStructureTypes;

/**
 * {@code guhs:grond_single_pool_element}: a normal single pool element (same fields as {@code minecraft:single_pool_element})
 * that says where its ground is: {@code ground_level_delta} = the template y of the first layer ABOVE the ground (the layer
 * you walk in).
 * <p>
 * Why (2.10, "het omgekeerde trapje"): vanilla takes template y = 1 as the ground of every piece. The Beardifier smooths the
 * terrain around a structure towards {@code minY + groundLevelDelta}, and the jigsaw placement puts {@code minY + delta} on
 * the surface. Our buildings are sunk into the ground (the template's ground is G blocks up, with foundations, ponds and
 * roots below it), so vanilla dug the land around them down to template y = 1: a ring that stepped down 3-2-1 to a moat
 * around the walls. With the real delta the terrain meets the building at its floor, with the normal smooth beard.
 * (tools/make_v2.py {@code grond()} writes these elements for every sunk structure. toString stays the vanilla
 * "Single[Left[location]]": PleinSlot reads the template from it.)
 */
public class GrondPoolElement extends SinglePoolElement {
    public static final MapCodec<GrondPoolElement> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            templateCodec(),
            processorsCodec(),
            projectionCodec(),
            overrideLiquidSettingsCodec(),
            Codec.intRange(0, 4064).fieldOf("ground_level_delta").forGetter(GrondPoolElement::groundLevelDelta)
    ).apply(i, GrondPoolElement::new));

    private final int groundLevelDelta;

    public GrondPoolElement(Either<Identifier, StructureTemplate> template, Holder<StructureProcessorList> processors,
                            StructureTemplatePool.Projection projection, Optional<LiquidSettings> overrideLiquidSettings, int groundLevelDelta) {
        super(template, processors, projection, overrideLiquidSettings);
        this.groundLevelDelta = groundLevelDelta;
    }

    public int groundLevelDelta() {
        return groundLevelDelta;
    }

    @Override
    public int getGroundLevelDelta() {
        return groundLevelDelta;
    }

    @Override
    public StructurePoolElementType<?> getType() {
        return ModStructureTypes.GROND_SINGLE_POOL_ELEMENT.get();
    }
}
