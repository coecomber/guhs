package nl.juiced.guhs.feature.verhaal;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.6): where a questline wants the player to go for its current step: a fixed spot,
 * or the nearest copy of a structure the player may see. {@code tekst} names it ("naar Guhvendel").
 */
public record Doel(ResourceKey<Level> dim, @Nullable BlockPos plek, @Nullable String structuur, Component tekst) {
    /** A fixed spot. */
    public static Doel plek(ResourceKey<Level> dim, BlockPos plek, Component tekst) {
        return new Doel(dim, plek, null, tekst);
    }

    /** The nearest copy of this structure (a guhs structure id without namespace) that the player may see. */
    public static Doel structuur(ResourceKey<Level> dim, String structuur, Component tekst) {
        return new Doel(dim, null, structuur, tekst);
    }
}
