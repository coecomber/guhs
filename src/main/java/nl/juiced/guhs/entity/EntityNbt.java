package nl.juiced.guhs.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * 26.1 port: entities save to {@code ValueOutput} / load from {@code ValueInput} now. These helpers give the old
 * CompoundTag style back for the places that keep a whole entity in an item or tag (picked up guhs, Reisguhs, gametests).
 * <pre>
 * 1.21.1: CompoundTag t = new CompoundTag(); guh.saveWithoutId(t);     ->  CompoundTag t = EntityNbt.save(guh);
 * 1.21.1: guh.save(t) (with "id")                                          ->  CompoundTag t = EntityNbt.saveWithId(guh);
 * 1.21.1: guh.load(t)                                                      ->  EntityNbt.load(guh, t);
 * inside addAdditionalSaveData(ValueOutput out): old helper(CompoundTag t)  ->  CompoundTag t = new CompoundTag(); helper(t); out.store(t);
 * inside readAdditionalSaveData(ValueInput in): old helper(CompoundTag t)    ->  helper(EntityNbt.whole(in))
 * </pre>
 */
public final class EntityNbt {
    private EntityNbt() {
    }

    /** Like 1.21.1 {@code saveWithoutId(new CompoundTag())}. */
    public static CompoundTag save(Entity entity) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(out);
        return out.buildResult();
    }

    /** Like 1.21.1 {@code save(tag)}: with the "id" (empty tag if the entity may not be saved, e.g. a passenger). */
    public static CompoundTag saveWithId(Entity entity) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.save(out);
        return out.buildResult();
    }

    /** Like 1.21.1 {@code load(tag)}. */
    public static void load(Entity entity, CompoundTag tag) {
        entity.load(TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag));
    }

    /** Only the subclass data (1.21.1 {@code addAdditionalSaveData(tag)}); needs access to the protected method. */
    public static CompoundTag saveAdditional(Entity entity, java.util.function.BiConsumer<Entity, net.minecraft.world.level.storage.ValueOutput> saver) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        saver.accept(entity, out);
        return out.buildResult();
    }

    /** A ValueInput view of a tag (to call a {@code readAdditionalSaveData(ValueInput)} with an old-style tag). */
    public static net.minecraft.world.level.storage.ValueInput input(Entity entity, CompoundTag tag) {
        return TagValueInput.create(ProblemReporter.DISCARDING, entity.registryAccess(), tag);
    }

    /** The whole input as an old-style tag (to hand it to 1.21.1 helper code that reads a CompoundTag). */
    public static CompoundTag whole(net.minecraft.world.level.storage.ValueInput input) {
        return input.read(com.mojang.serialization.MapCodec.assumeMapUnsafe(CompoundTag.CODEC)).orElseGet(CompoundTag::new);
    }

    /** A fresh ValueOutput (call {@code buildResult()} on it for the tag). */
    public static TagValueOutput output(Entity entity) {
        return TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
    }
}
