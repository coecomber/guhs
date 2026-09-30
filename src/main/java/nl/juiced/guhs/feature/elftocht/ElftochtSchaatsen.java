package nl.juiced.guhs.feature.elftocht;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.Guhs;

/**
 * Skating (both sides, every player tick): with guh-schaatsen in either hand, on ice (tag guhs:elftocht/schaatsijs) you
 * get {@link #SNELHEID} x your walking speed on top (the ice keeps you gliding: vanilla's slippery ice does the rest).
 * While jumping over a crack you keep the speed for {@link #SPRONG} ticks. Off the ice you walk as usual. The client
 * adds the skater's sway, the blades' hiss and the ice dust (client.SchaatsEffecten).
 */
public final class ElftochtSchaatsen {
    public static final ResourceLocation MODIFIER = Guhs.id("elftocht_schaatsen");
    /** Extra walking speed on ice (x the base speed): walking ~6.4, sprinting ~8.3 blocks per second on the canal. */
    public static final double SNELHEID = 0.5;
    /** Ticks you keep the skating speed in the air (a jump) after touching ice. */
    public static final int SPRONG = 12;
    private static final String LAATST = "guhs_elftocht_ijs";

    public static boolean houdtSchaatsen(Player player) {
        return player.getMainHandItem().is(ElftochtFeature.SCHAATSEN.get()) || player.getOffhandItem().is(ElftochtFeature.SCHAATSEN.get());
    }

    /** Standing on skating ice right now? */
    public static boolean opIJs(Player player) {
        return player.onGround() && player.getBlockStateOn().is(ElftochtFeature.SCHAATSIJS);
    }

    /** Skating: skates in hand, on the ice (or just jumped off it), not flying, riding or swimming. */
    public static boolean schaatst(Player player) {
        if (!houdtSchaatsen(player) || player.isPassenger() || player.getAbilities().flying || player.isInWater() || player.isFallFlying()) {
            return false;
        }
        if (opIJs(player)) {
            return true;
        }
        var data = player.getPersistentData();
        return !player.onGround() && data.contains(LAATST) && player.tickCount - data.getInt(LAATST) <= SPRONG;
    }

    public static void tick(Player player) {
        if (opIJs(player)) {
            player.getPersistentData().putInt(LAATST, player.tickCount);
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean nu = schaatst(player);
        boolean heeft = speed.hasModifier(MODIFIER);
        if (nu && !heeft) {
            speed.addTransientModifier(new AttributeModifier(MODIFIER, SNELHEID, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else if (!nu && heeft) {
            speed.removeModifier(MODIFIER);
        }
    }

    private ElftochtSchaatsen() {
    }
}
