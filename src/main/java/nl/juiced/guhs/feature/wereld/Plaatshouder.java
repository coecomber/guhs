package nl.juiced.guhs.feature.wereld;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * bbq2 (F0 skelet): the placeholder behind a fixed entity id of CONTRACT_130 7 whose owner slice has not built it yet: an
 * invisible thing that does nothing and can't be hurt, so templates, spawn eggs and FTB tasks of other slices already load.
 * The owner replaces {@code Plaatshouder.type(...)} in its Feature class by its own entity type (same id, same field name).
 */
public class Plaatshouder extends Entity {
    public Plaatshouder(EntityType<?> type, Level level) {
        super(type, level);
    }

    /** A placeholder entity type {@code guhs:<id>} on the owner's own register. */
    public static DeferredHolder<EntityType<?>, EntityType<Plaatshouder>> type(DeferredRegister<EntityType<?>> register, String id) {
        return register.register(id, () -> EntityType.Builder.<Plaatshouder>of(Plaatshouder::new, MobCategory.MISC).sized(0.6f, 0.6f)
                .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(id))));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder entityData) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }
}
