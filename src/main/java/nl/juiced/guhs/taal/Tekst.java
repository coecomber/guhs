package nl.juiced.guhs.taal;

import java.util.Optional;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 1.2.0: texts that are saved or sent (names, places, diary lines) are Components, not Strings resolved on the server
 * (the server resolves in en_us, the player may read Dutch: client/GuhsTaal). They are stored with
 * ComponentSerialization.CODEC: a plain literal is a plain string tag, so data saved before 1.2.0 (a String) still loads,
 * as a literal (old worlds keep their Dutch).
 */
public final class Tekst {
    public static void put(CompoundTag tag, String key, Component text) {
        tag.store(key, ComponentSerialization.CODEC, text);
    }

    public static Component get(CompoundTag tag, String key) {
        return tag.read(key, ComponentSerialization.CODEC).orElse(Component.empty());
    }

    public static void put(ValueOutput out, String key, Component text) {
        out.store(key, ComponentSerialization.CODEC, text);
    }

    public static Component get(ValueInput in, String key) {
        return in.read(key, ComponentSerialization.CODEC).orElse(Component.empty());
    }

    public static Optional<Component> read(CompoundTag tag, String key) {
        return tag.read(key, ComponentSerialization.CODEC);
    }

    /** A plain literal text (no translate key anywhere): a player's own name for something, or old saved data. */
    public static boolean literal(Component c) {
        return c.getContents() instanceof PlainTextContents && c.getSiblings().isEmpty();
    }

    /** Is there no text at all? */
    public static boolean empty(Component c) {
        return literal(c) && ((PlainTextContents) c.getContents()).text().isEmpty();
    }

    private Tekst() {
    }
}
