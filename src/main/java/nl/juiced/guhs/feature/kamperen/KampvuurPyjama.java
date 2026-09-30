package nl.juiced.guhs.feature.kamperen;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * Pyjamas by the campfire: at night every guh near a burning campfire wears its pyjama and nightcap (the flag
 * GuhHooks.PYJAMA, drawn by client.KamperenClient with the pyjama_pakje and slaapmutsje textures); in the morning
 * (or away from the fire) it's off again. Checked every {@value #ELKE} ticks per guh, spread over the guhs.
 */
public final class KampvuurPyjama {
    public static final int ELKE = 40;
    /** How far from a campfire a guh puts on its pyjama. */
    public static final int AFSTAND = 7;
    /** Guhs for which it is always night (the game tests: the test level's clock is shared). */
    public static final Set<UUID> TEST_NACHT = ConcurrentHashMap.newKeySet();

    private KampvuurPyjama() {
    }

    static void tick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % ELKE != 0) {
            return;
        }
        werk(guh, isNacht(guh));
    }

    /** Puts the pyjama on or off (nacht: is it night for this guh). Returns whether it wears one now. */
    public static boolean werk(GuhEntity guh, boolean nacht) {
        boolean aan = nacht && kampvuurBij(guh.level(), guh.blockPosition(), AFSTAND) != null;
        if (GuhHooks.heeft(guh, GuhHooks.PYJAMA) != aan) {
            GuhHooks.zet(guh, GuhHooks.PYJAMA, aan);
        }
        return aan;
    }

    /** Night (story and pyjama time): from the evening until the morning, in a world with a day and a night. */
    public static boolean isNacht(GuhEntity guh) {
        if (TEST_NACHT.contains(guh.getUUID())) {
            return true;
        }
        Level level = guh.level();
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        long t = nl.juiced.guhs.world.GuhTime.timeOfDay(level);
        return t >= 12500 && t < 23300;
    }

    /** A burning campfire within this many blocks (2 up or down), or null. */
    @Nullable
    public static BlockPos kampvuurBij(Level level, BlockPos at, int r) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-r, -2, -r), at.offset(r, 2, r))) {
            BlockState s = level.getBlockState(p);
            if (s.is(BlockTags.CAMPFIRES) && CampfireBlock.isLitCampfire(s)) {
                return p.immutable();
            }
        }
        return null;
    }
}
