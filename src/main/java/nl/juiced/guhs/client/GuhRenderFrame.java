package nl.juiced.guhs.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.geckolib.renderer.base.BoneSnapshots;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.item.GuhArmorItem;

/**
 * 1.1.0 (GeckoLib 5): everything the {@link GuhRenderer} needs to draw one guh in one frame, copied from the entity while
 * it is still available ({@code addRenderData}); stored in the render state under {@link GuhRenderer#FRAME}.
 * <p>
 * GeckoLib 5 splits rendering in two: <b>extract</b> (the entity is there: read it, compute values) and <b>submit</b> (only
 * the render state is there: pose bones, draw). Features that change how a guh looks register a {@link GuhRenderer.Hook};
 * it runs at extract time with the entity and fills this frame with the things to do at submit time. <b>Only capture
 * values</b> (floats, textures, item render states) in the lambdas, never the entity itself.
 * <ul>
 *   <li>{@link #texture(Identifier, int)}: another fur texture (highest priority wins; sleeping eyes are added after);</li>
 *   <li>{@link #glow(Identifier)}: an extra full-bright pass (like the starry guh's stars);</li>
 *   <li>{@link #bones(BoneMove)}: move/rotate/scale/hide bones after the animations (was GeoBone.setRotX/setHidden...);</li>
 *   <li>{@link #pose(PoseMove)}: move the whole model after the body rotation (was applyRotations);</li>
 *   <li>{@link #pass(RenderType, int, boolean, Predicate)}: draw the model again with only some bones and another texture
 *       (was reRender with hidden bones, e.g. clothes, pyjamas);</li>
 *   <li>{@link #extra(Extra)} / {@link #layerExtra(Extra)}: submit other things (items, text...) near the guh.</li>
 * </ul>
 */
public final class GuhRenderFrame {
    /** Moves bones for this frame (after the animations and the renderer's own bone changes). */
    @FunctionalInterface
    public interface BoneMove {
        void apply(BoneSnapshots bones);
    }

    /** Changes the model's pose (after the body rotation, before the model is drawn). */
    @FunctionalInterface
    public interface PoseMove {
        void apply(PoseStack pose);
    }

    /** Submits something extra for this frame (item, text, custom geometry...). */
    @FunctionalInterface
    public interface Extra {
        void submit(PoseStack pose, SubmitNodeCollector collector, int light);
    }

    /** One extra model pass: the model again with {@code type}, only the bones for which {@code shows(name)} is true. */
    public record ModelPass(RenderType type, int colour, boolean fullBright, Predicate<String> shows) {
    }

    // --- what the guh is ---------------------------------------------------------------------------------------------
    public final GuhVariant variant;
    public final boolean saddled;
    public final GuhArmorItem.Tier armour;
    /** Worn clothes per slot (absent = nothing in that slot). */
    public final Map<GuhClothes.Slot, GuhClothes> clothes = new EnumMap<>(GuhClothes.Slot.class);
    /** Hair colour (RGB) or -1 = the hair's own colours. */
    public final int haarkleur;
    /** Asleep (SLAPEN emote or in a nest): closed-eye textures. */
    public final boolean sleeping;
    public final boolean secretNote;
    /** Baby size and the "gravity" squish of this frame. */
    public final float ageScale;
    public final float squish;

    // --- what the hooks asked for ------------------------------------------------------------------------------------
    private Identifier texture;
    private int texturePriority = Integer.MIN_VALUE;
    final List<Identifier> glows = new ArrayList<>(1);
    final List<BoneMove> boneMoves = new ArrayList<>(2);
    final List<PoseMove> poseMoves = new ArrayList<>(1);
    final List<ModelPass> passes = new ArrayList<>(2);
    final List<Extra> extras = new ArrayList<>(1);
    final List<Extra> layerExtras = new ArrayList<>(1);

    GuhRenderFrame(GuhEntity guh, float partialTick) {
        this.variant = guh.getVariant();
        this.saddled = guh.isSaddled();
        this.armour = guh.getArmorTier();
        if (guh.isWearingClothes()) {
            for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
                GuhClothes worn = guh.getClothes(slot);
                if (worn != null) {
                    clothes.put(slot, worn);
                }
            }
        }
        this.haarkleur = guh.getHaarkleur();
        this.sleeping = GuhRenderer.slaapt(guh);
        this.secretNote = guh.hasSecretNote();
        this.ageScale = guh.getAgeScale();
        this.squish = guh.getSquish(partialTick);
        this.texture = variant.texture(guh.tickCount + guh.getId());
    }

    /** Use this fur texture if nothing with a higher priority asked for another one (Pinguh 20, story variants 10). */
    public void texture(Identifier tex, int priority) {
        if (priority > texturePriority) {
            texture = tex;
            texturePriority = priority;
        }
    }

    /** The fur texture (after the hooks, before the sleeping eyes). */
    public Identifier texture() {
        return texture;
    }

    /** An extra full-bright pass with this (glowmask) texture, same bones as the main pass. */
    public void glow(Identifier glowTexture) {
        glows.add(glowTexture);
    }

    public void bones(BoneMove move) {
        boneMoves.add(move);
    }

    public void pose(PoseMove move) {
        poseMoves.add(move);
    }

    public void pass(RenderType type, int colour, boolean fullBright, Predicate<String> shows) {
        passes.add(new ModelPass(type, colour, fullBright, shows));
    }

    /** Clothes-style pass: {@code texture} (cutout, no culling) on only the bones that {@code shows}. */
    public void pass(Identifier texture, int colour, Predicate<String> shows) {
        pass(RenderTypes.entityCutout(texture), colour, false, shows);
    }

    /** Extra things in entity space: feet at 0,0,0, not rotated, not scaled (was VariantUiterlijk.extra / after render()). */
    public void extra(Extra extra) {
        extras.add(extra);
    }

    /**
     * Extra things like the 1.0.0 GuhRenderHooks layers: entity space, not rotated, but scaled with the baby size and squish
     * (the pose GeckoLib 4 gave render layers).
     */
    public void layerExtra(Extra extra) {
        layerExtras.add(extra);
    }

    @Nullable
    public GuhClothes worn(GuhClothes.Slot slot) {
        return clothes.get(slot);
    }
}
