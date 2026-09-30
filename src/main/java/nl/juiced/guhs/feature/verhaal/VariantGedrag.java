package nl.juiced.guhs.feature.verhaal;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 3.0 (Guhverhalen): the behaviour of one guh variant, hooked into GuhEntity so the owner slice never edits it. Register it
 * from the owner's Feature.register with {@link VariantGedragen#zet} (common code: it runs on both sides).
 * <ul>
 *   <li>balto: BALTOGUH (fast in snow: riddenSpeed + a speed modifier on snow; speciaal = sniff the way home);</li>
 *   <li>mewtwo: MEWTWO (hovers ~1.5 blocks with a purple glow, floats over short gaps when ridden, knabbel telekinesis
 *       via tick, voerFactor 2 + the "x2" gegeten animation);</li>
 *   <li>guhwaii: STITCH626 (opKlimbaar, draagFactor 2, speciaal = the ukelele emote).</li>
 * </ul>
 */
public interface VariantGedrag {
    /** Every tick of a guh of this variant, both sides (server-only work: check {@code guh.level().isClientSide}). */
    default void tick(GuhEntity guh) {
    }

    /** Called first in GuhEntity.travel (ridden or not): true = handled (the normal travel is skipped). */
    default boolean travel(GuhEntity guh, Vec3 input) {
        return false;
    }

    /** The speed when ridden (GuhEntity.getRiddenSpeed; speed = its movement speed attribute). */
    default float riddenSpeed(GuhEntity guh, float speed) {
        return speed;
    }

    /** GuhEntity.onClimbable: true = it climbs here (626: walls and ceilings). */
    default boolean opKlimbaar(GuhEntity guh) {
        return false;
    }

    /** VOEREN hearts (and that day cap) x this: Guhtwo 2. Never less than 1. */
    default int voerFactor(GuhEntity guh) {
        return 1;
    }

    /** After it ate a snack or a knabbel from someone (server; e.g. the "x2" animation). */
    default void gegeten(GuhEntity guh, @Nullable ServerPlayer p, ItemStack snack) {
    }

    /** Chore amounts per trip x this (626 carries 2). */
    default int draagFactor(Mob guh) {
        return 1;
    }

    /** The looping animation of the controller "variant" (null: none), e.g. the Guhtwo hover. Client side. */
    @Nullable
    default RawAnimation animatie(GuhEntity guh) {
        return null;
    }

    /** The lang key of this variant's own button in the guh menu (null: no button), e.g. "gui.guhs.balto.snuffel". */
    @Nullable
    default String speciaalKnop() {
        return null;
    }

    /** That button was pressed by the owner (within 8 blocks; server, GuhActionPayload VARIANT_SPECIAAL). */
    default void speciaal(GuhEntity guh, ServerPlayer owner) {
    }
}
