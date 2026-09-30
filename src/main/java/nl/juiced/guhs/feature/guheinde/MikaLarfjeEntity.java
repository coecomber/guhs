package nl.juiced.guhs.feature.guheinde;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.registry.ModItems;

/**
 * A Mika-larfje: a tiny Mika (the silverfish of the Knabbelkelder) that lives in aangevreten kaaskorststenen. It doesn't
 * hurt, it steals: every time it gets you, a kaasknabbel is gone (it gives them back when you squash it).
 */
public class MikaLarfjeEntity extends MikaEntity {
    public static final float LARFJE_HEALTH = 8f;
    public static final float LARFJE_SCALE = 0.4f;
    private int gestolen;

    public MikaLarfjeEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 3;
        this.setCustomName(Component.translatable("entity.guhs.mika_larfje"));
    }

    public static AttributeSupplier.Builder createLarfjeAttributes() {
        return MikaEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, LARFJE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.SCALE, LARFJE_SCALE);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnType, @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, data);
        this.getAttribute(Attributes.SCALE).setBaseValue(LARFJE_SCALE); // (a Mika gets a random size: a larfje stays tiny)
        this.refreshDimensions();
        return result;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        this.swing(this.getUsedItemHand());
        if (target instanceof ServerPlayer player) {
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (stack.is(ModItems.KAAS_KNABBELS.get())) {
                    stack.shrink(1);
                    gestolen++;
                    player.sendOverlayMessage(Component.translatable("gui.guhs.guheinde.larfje.roof").withStyle(ChatFormatting.DARK_PURPLE));
                    break;
                }
            }
            player.knockback(0.4, Math.sin(this.getYRot() * Math.PI / 180), -Math.cos(this.getYRot() * Math.PI / 180));
            player.hurtMarked = true;
        }
        return true;
    }

    public int gestolen() {
        return gestolen;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (gestolen > 0) {
            this.spawnAtLocation(new ItemStack(ModItems.KAAS_KNABBELS.get(), gestolen));
        }
    }

    @Override
    public float getVoicePitch() {
        return 1.8f + this.random.nextFloat() * 0.3f;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Gestolen", gestolen);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        gestolen = tag.getIntOr("Gestolen", 0);
    }
}
