package nl.juiced.guhs.feature.einde2.mikas;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;

/**
 * einde2, slice mikas: the Kaasmijten that pop out of a knabbelkoorvrucht. The biomes slice calls {@link #uitVrucht} when
 * a fruit is eaten; nothing else makes a Kaasmijt come by itself.
 */
public final class Kaasmijten {
    /** One fruit in this many holds a Kaasmijt. */
    public static final int KANS = 8;

    /**
     * Called by the biomes slice when a knabbelkoorvrucht is eaten at this spot. Rolls the chance itself (about 1 in 8).
     *
     * @return true when a Kaasmijt popped out
     */
    public static boolean uitVrucht(ServerLevel level, Vec3 plek, @Nullable ServerPlayer eter) {
        if (level.getRandom().nextInt(KANS) != 0) {
            return false;
        }
        KaasmijtEntity mijt = zet(level, plek);
        if (mijt == null) {
            return false;
        }
        level.playSound(null, plek.x, plek.y, plek.z, E2MikasFeature.MIJT_PLOP.get(), SoundSource.NEUTRAL, 0.8f, 1.2f + level.getRandom().nextFloat() * 0.3f);
        level.sendParticles(E2MikasFeature.FLITS.get(), plek.x, plek.y + 0.3, plek.z, 10, 0.2, 0.2, 0.2, 0.02);
        if (eter != null) {
            eter.sendOverlayMessage(Component.translatable("gui.guhs.einde2_mikas.mijt_uit_vrucht").withStyle(ChatFormatting.LIGHT_PURPLE));
            GuhDex.zie(eter, GuhVariant.KAASMIJT);
        }
        return true;
    }

    /** Puts a Kaasmijt down at this spot (it lives for {@link KaasmijtEntity#LEVEN} ticks). Null when the spot is no good. */
    @Nullable
    public static KaasmijtEntity zet(ServerLevel level, Vec3 plek) {
        KaasmijtEntity mijt = E2MikasFeature.KAASMIJT.get().create(level, EntitySpawnReason.TRIGGERED);
        if (mijt == null) {
            return null;
        }
        mijt.snapTo(plek.x, plek.y, plek.z, level.getRandom().nextFloat() * 360f, 0f);
        return level.addFreshEntity(mijt) ? mijt : null;
    }

    private Kaasmijten() {
    }
}
