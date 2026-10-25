package nl.juiced.guhs.feature.mijnspellen.kegelgrot;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.techbeurs.VadslampBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * 1.7.0 (ronde 45-47): the Kegelzetter, the pin setter of a Kegelgrot lane (tools/features/mijnspellen_bouw.py
 * kegelgrot_kracht, the same numbers, checked there). A bridge over the end of the lanes with a Guhrad on it; per lane a
 * Vadslamp on the bridge ({@link #U}, {@link #DY} + 1 above the foul line block, v -1..1) and a sign on the bridge's front
 * ({@link #BORD_U}, {@link #DY}, v 0). Decoration that runs on vadskracht, nothing more:
 * <ul>
 *   <li>with vadskracht (its lamp's net runs) a fresh rack comes with a little show (sparks from the setter down to the
 *       pins, a clack); without it the sign says the setter takes a break and De Guhde sets them by hand;</li>
 *   <li>the pins are ALWAYS set ({@link KegelGame} never waits for it): the game never breaks on a stopped wheel, a broken
 *       lamp or an older Kegelgrot without a setter ({@link #kracht} null: nothing shown).</li>
 * </ul>
 */
public final class Kegelzetter {
    /** In a lane's frame from its foul line block: the bridge's row, the sign's row, the bridge's height. */
    public static final int U = 15, BORD_U = 14, DY = 3;
    static final String AAN = "sign.guhs.kegelen.zetter_aan_", UIT = "sign.guhs.kegelen.zetter_uit_";

    /** The setter's Vadslamp of the lane whose foul line block is {@code begin} (facing f), or null when it has none. */
    @Nullable
    public static BlockPos lamp(ServerLevel level, BlockPos begin, Direction f) {
        Direction r = f.getClockWise();
        for (int v = -1; v <= 1; v++) {
            BlockPos p = begin.relative(f, U).relative(r, v).above(DY + 1);
            if (level.isLoaded(p) && level.getBlockState(p).getBlock() instanceof VadslampBlock) {
                return p;
            }
        }
        return null;
    }

    /** Does this lane's setter have vadskracht? null: the lane has no setter. */
    @Nullable
    public static Boolean kracht(ServerLevel level, BlockPos begin, Direction f) {
        BlockPos lamp = lamp(level, begin, f);
        if (lamp == null) {
            return null;
        }
        return level.getBlockEntity(lamp) instanceof MachineBlockEntity m && m.heeftKracht();
    }

    /** The lane's setter sign. */
    public static BlockPos bord(BlockPos begin, Direction f) {
        return begin.relative(f, BORD_U).above(DY);
    }

    /** Shows on the lane's sign whether the setter works (aan) or takes a break; only a Kegelzetter sign is touched. */
    public static void toon(ServerLevel level, BlockPos begin, Direction f, boolean aan) {
        BlockPos p = bord(begin, f);
        if (!level.isLoaded(p) || !(level.getBlockEntity(p) instanceof SignBlockEntity sign)) {
            return;
        }
        String nu = sleutel(sign);
        String wil = (aan ? AAN : UIT) + "0";
        if (nu == null || !(nu.startsWith(AAN) || nu.startsWith(UIT)) || nu.equals(wil)) {
            return;
        }
        SignText tekst = sign.getFrontText();
        for (int i = 0; i < 4; i++) {
            tekst = tekst.setMessage(i, Component.translatable((aan ? AAN : UIT) + i));
        }
        sign.setText(tekst, true);
        sign.setChanged();
        level.sendBlockUpdated(p, sign.getBlockState(), sign.getBlockState(), 3);
    }

    /** The translation key of the sign's first front line (null when it isn't one). */
    @Nullable
    public static String sleutel(SignBlockEntity sign) {
        return sign.getFrontText().getMessage(0, false).getContents() instanceof TranslatableContents t ? t.getKey() : null;
    }

    /** A fresh rack with vadskracht: sparks from the setter's lamp down to the pin deck, a clack and a whirr. */
    static void show(ServerLevel level, BlockPos lamp, Vec3 dek) {
        Vec3 van = Vec3.atCenterOf(lamp);
        for (int i = 0; i <= 6; i++) {
            Vec3 p = van.lerp(dek, i / 6.0);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 2, 0.15, 0.1, 0.15, 0.02);
        }
        level.sendParticles(ParticleTypes.END_ROD, dek.x, dek.y + 0.3, dek.z, 6, 0.5, 0.2, 0.5, 0.01);
        level.playSound(null, lamp, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.35f, 1.6f);
    }

    private Kegelzetter() {
    }
}
