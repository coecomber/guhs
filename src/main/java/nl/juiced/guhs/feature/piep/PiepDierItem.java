package nl.juiced.guhs.feature.piep;

import java.util.List;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * A piep-maatje you picked up (sneak + right-click it with an empty hand, or "Oppakken" in its menu; owner only): the whole
 * creature (name, owner, health, age, its menu settings, its rest timer...) rides along in the item's custom data, like the
 * vanilla bucket of axolotl. Right-click a block with it: it hops down there, exactly as it was. Used for
 * {@code guhs:poepschilly_item} and {@code guhs:schilly_item}; the {@link MuisjeItem pieppiepmuisje_item} adds the shoulder.
 */
public class PiepDierItem extends Item {
    private final Supplier<? extends EntityType<? extends TamableAnimal>> type;

    public PiepDierItem(Supplier<? extends EntityType<? extends TamableAnimal>> type, Properties properties) {
        super(properties);
        this.type = type;
    }

    public EntityType<? extends TamableAnimal> type() {
        return type.get();
    }

    // --- picking up --------------------------------------------------------------------------------------------------------------

    /** The item holding this creature (it stays in the world: the caller removes it). */
    public static ItemStack van(TamableAnimal dier, Item item) {
        nl.juiced.guhs.feature.band.Band.id(dier);   // 2.10: its own band id travels along (its entity UUID doesn't)
        CompoundTag tag = new CompoundTag();
        dier.saveWithoutId(tag);
        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("leash");
        tag.remove("Leash");
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (dier.hasCustomName()) {
            stack.set(DataComponents.CUSTOM_NAME, dier.getCustomName());
        }
        return stack;
    }

    /**
     * The owner picks this creature up (into the pockets, or at their feet when they are full). Returns false (and says why)
     * when it is not theirs or busy.
     */
    public static boolean pakOp(PiepMaatje maatje, ServerPlayer player) {
        TamableAnimal dier = maatje.dier();
        if (!dier.isAlive() || !dier.isTame() || !dier.isOwnedBy(player)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.piep.niet_jouw_maatje").withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (maatje.isBezig()) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.piep.even_bezig", dier.getDisplayName()).withStyle(ChatFormatting.GRAY));
            return false;
        }
        if (dier.isLeashed()) {
            dier.dropLeash(true, !player.hasInfiniteMaterials());
        }
        dier.stopRiding();
        ItemStack stack = van(dier, maatje.oppakItem());
        dier.level().playSound(null, dier.blockPosition(), geluid(dier), SoundSource.NEUTRAL, 0.8f, 1.5f);
        dier.discard();
        nl.juiced.guhs.feature.band.GuhVolger.inZakken(stack, player);   // 2.10: "waar is mijn guh"
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.piep.opgepakt." + maatje.soort(), dier.getDisplayName())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // --- putting down ------------------------------------------------------------------------------------------------------------

    /** A fresh creature from an item (not yet in the world); an item without data gives a new, wild one. */
    @Nullable
    public static <T extends TamableAnimal> T naar(ItemStack stack, Level level, EntityType<T> type) {
        T dier = type.create(level, EntitySpawnReason.TRIGGERED);
        if (dier == null) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            dier.load(data.copyTag());
        }
        if (stack.has(DataComponents.CUSTOM_NAME)) {
            dier.setCustomName(stack.getHoverName());
        }
        dier.setPersistenceRequired();
        return dier;
    }

    /** Puts the creature of this item into the world at this spot. */
    @Nullable
    public static <T extends TamableAnimal> T zetNeer(ItemStack stack, ServerLevel level, Vec3 at, float yaw, EntityType<T> type) {
        T dier = naar(stack, level, type);
        if (dier == null) {
            return null;
        }
        dier.snapTo(at.x, at.y, at.z, yaw, 0);
        dier.setYHeadRot(yaw);
        dier.setYBodyRot(yaw);
        dier.setDeltaMovement(Vec3.ZERO);
        dier.fallDistance = 0;
        level.addFreshEntity(dier);
        level.playSound(null, dier.blockPosition(), geluid(dier), SoundSource.NEUTRAL, 0.8f, 1.3f);
        return dier;
    }

    /** 3.0: the maatje's own pick-up sound (PiepMaatje.oppakGeluid). */
    static SoundEvent geluid(TamableAnimal dier) {
        return dier instanceof PiepMaatje m ? m.oppakGeluid() : PiepFeature.SCHILLY_PLOP.get();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        TamableAnimal dier = zetNeer(stack, level, new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5),
                player == null ? 0 : player.getYRot() + 180, type());
        if (dier == null) {
            return InteractionResult.FAIL;
        }
        stack.shrink(1);
        if (player instanceof ServerPlayer sp && dier instanceof PiepMaatje m) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.piep.neergezet." + m.soort(), dier.getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.CONSUME;
    }

    /** 2.10: "waar is mijn guh": in someone's pockets (checked every 5 seconds). */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (!level.isClientSide() && entity instanceof Player holder && (level.getGameTime() + slot) % 100 == 0) {
            nl.juiced.guhs.feature.band.GuhVolger.inZakken(stack, holder);
        }
    }

    /** 2.10: "waar is mijn guh": dropped on the ground. */
    @Override
    public boolean onEntityItemUpdate(ItemStack stack, net.minecraft.world.entity.item.ItemEntity entity) {
        if (!entity.level().isClientSide() && entity.tickCount % 100 == 1) {
            nl.juiced.guhs.feature.band.GuhVolger.item(stack, nl.juiced.guhs.feature.band.PlekSoort.ITEM_GROND, entity.level().dimension(), entity.blockPosition(), "", entity.level().getGameTime());
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
    }
}
