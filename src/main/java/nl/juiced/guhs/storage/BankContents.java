package nl.juiced.guhs.storage;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * An immutable snapshot of what's inside a Bank Guh: item (count 1) + how many (a long, so practically infinite).
 * Used as the item data component (so a broken Bank Guh keeps its contents) and to sync the list to the screen.
 */
public record BankContents(List<Entry> entries) {
    public static final BankContents EMPTY = new BankContents(List.of());

    public record Entry(ItemStack item, long count) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(Entry::item),
                Codec.LONG.fieldOf("count").forGetter(Entry::count)
        ).apply(i, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC, Entry::item,
                ByteBufCodecs.VAR_LONG, Entry::count,
                Entry::new);
    }

    public static final Codec<BankContents> CODEC = Entry.CODEC.listOf().xmap(BankContents::new, BankContents::entries);
    public static final StreamCodec<RegistryFriendlyByteBuf, BankContents> STREAM_CODEC =
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(BankContents::new, BankContents::entries);

    public long totalItems() {
        return entries.stream().mapToLong(Entry::count).sum();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
