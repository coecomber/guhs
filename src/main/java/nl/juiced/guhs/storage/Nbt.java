package nl.juiced.guhs.storage;

import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 26.1 bridges between the old CompoundTag style and ValueInput/ValueOutput (1.21.6+), so code that keeps whole
 * entities / block entities as tags (picked-up guhs, copies, gametests) stays short. Registry context is always
 * taken from the entity's level (needed for item stacks, holders, ...). Problems are ignored like 1.21.1 did.
 */
public final class Nbt {
    private Nbt() {
    }

    /** 1.21.1 {@code entity.saveWithoutId(new CompoundTag())}. */
    public static CompoundTag saveWithoutId(Entity entity) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(out);
        return out.buildResult();
    }

    /** 1.21.1 {@code entity.saveWithoutId(tag)} into an existing tag (entries are merged into {@code tag}). */
    public static CompoundTag saveWithoutId(Entity entity, CompoundTag tag) {
        tag.merge(saveWithoutId(entity));
        return tag;
    }

    /** 1.21.1 {@code entity.save(tag)} (with "id"; false for passengers, like vanilla). */
    public static boolean save(Entity entity, CompoundTag tag) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        boolean saved = entity.save(out);
        tag.merge(out.buildResult());
        return saved;
    }

    /** 1.21.1 {@code entity.load(tag)}. */
    public static void load(Entity entity, CompoundTag tag) {
        entity.load(input(entity.registryAccess(), tag));
    }

    /** A ValueInput over an old-style tag. */
    public static ValueInput input(HolderLookup.Provider registries, CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
    }

    /** Runs {@code writer} on a fresh ValueOutput and returns the written tag. */
    public static CompoundTag write(HolderLookup.Provider registries, Consumer<ValueOutput> writer) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        writer.accept(out);
        return out.buildResult();
    }

    /** The whole input as an old-style tag (for helpers that still take a CompoundTag). */
    public static CompoundTag toTag(ValueInput in) {
        return in.read(MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
    }

    /** 1.21.1 {@code blockEntity.saveWithoutMetadata(registries)} stays available; this is the load side. */
    public static void loadBlockEntity(BlockEntity be, HolderLookup.Provider registries, CompoundTag tag) {
        be.loadWithComponents(input(registries, tag));
    }
}
