package nl.juiced.guhs.feature.spiesburcht;

import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.core.UUIDUtil;
/**
 * The greedy Nether-Mikas (guhs:nether_mika) of the Barbecuether and the Nether, like piglins with their gold:
 * <ul>
 *   <li>they trade a vahoege vads ingot (give it, or throw it near one) for something from their loot
 *       (loot table guhs:gameplay/nether_mika_ruil): they hold it up, sniff it ({@link #ADMIRE_TICKS} ticks) and throw
 *       you a present;</li>
 *   <li>they only leave you alone while you wear a piece of vahoege vads armour, and never once you've hit one of
 *       them: then all Nether-Mikas around are angry at you for {@link #ANGRY_TICKS} ticks.</li>
 * </ul>
 * Everything is added to the existing entity with events and goals (the registration in ModEntities stays as it is).
 */
public final class NetherMikaRuil {
    public static final ResourceKey<LootTable> RUIL_LOOT = ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("gameplay/nether_mika_ruil"));
    public static final int ADMIRE_TICKS = 110;
    public static final int ANGRY_TICKS = 600;
    public static final int ALERT_RANGE = 16;
    static final String ADMIRE_UNTIL = "guhs_ruil_tot", PARTNER = "guhs_ruil_speler", ANGRY_AT = "guhs_boos_op", ANGRY_UNTIL = "guhs_boos_tot";

    public static void register() {
        NeoForge.EVENT_BUS.addListener(NetherMikaRuil::onJoin);
        NeoForge.EVENT_BUS.addListener(NetherMikaRuil::onInteract);
        NeoForge.EVENT_BUS.addListener(NetherMikaRuil::onHurt);
        NeoForge.EVENT_BUS.addListener(NetherMikaRuil::onChangeTarget);
    }

    public static boolean isNetherMika(@Nullable Entity entity) {
        return entity instanceof MikaEntity && entity.getType() == ModEntities.NETHER_MIKA.get();
    }

    // --- who a Nether-Mika is angry at -------------------------------------------------------------------------------------

    /** What counts as vads gear for a Nether-Mika (the vahoege vads armour; data/guhs/tags/item/vads_uitrusting.json). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> VADS_UITRUSTING =
            net.minecraft.tags.TagKey.create(Registries.ITEM, Guhs.id("vads_uitrusting"));

    /** Wearing at least one piece of vahoege vads armour. */
    public static boolean wearsVads(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.is(VADS_UITRUSTING)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAngryAt(MikaEntity mika, Player player) {
        CompoundTag data = mika.getPersistentData();
        return data.read(ANGRY_AT, UUIDUtil.CODEC).isPresent() && data.read(ANGRY_AT, UUIDUtil.CODEC).orElseThrow().equals(player.getUUID()) && data.getLongOr(ANGRY_UNTIL, 0L) > mika.level().getGameTime();
    }

    /** Does this Nether-Mika go after this player? */
    public static boolean isHostileTo(MikaEntity mika, Player player) {
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return false;
        }
        return isAngryAt(mika, player) || !wearsVads(player);
    }

    public static void makeAngry(MikaEntity mika, Player player) {
        CompoundTag data = mika.getPersistentData();
        data.store(ANGRY_AT, UUIDUtil.CODEC, player.getUUID());
        data.putLong(ANGRY_UNTIL, mika.level().getGameTime() + ANGRY_TICKS);
        stopAdmiring(mika, false);
        mika.setTarget(player);
    }

    // --- events -------------------------------------------------------------------------------------------------------------

    /** Nether-Mikas get their greed: they only target players without vads (or who hit them), and like ingots. */
    static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !isNetherMika(event.getEntity())) {
            return;
        }
        MikaEntity mika = (MikaEntity) event.getEntity();
        mika.targetSelector.removeAllGoals(g -> g instanceof NearestAttackableTargetGoal<?>);
        mika.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mika, Player.class, 10, true, false, p -> isHostileTo(mika, (Player) p)));
        mika.goalSelector.addGoal(1, new AdmireGoal(mika));
        mika.goalSelector.addGoal(3, new FetchIngotGoal(mika));
        mika.setDropChance(EquipmentSlot.MAINHAND, 1.0f);
    }

    /** Right-click a Nether-Mika with a vahoege vads ingot: a deal! */
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (!isNetherMika(event.getTarget()) || !event.getItemStack().is(ModItems.VAHOEGE_VADS_INGOT.get())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer player) {
            offer((MikaEntity) event.getTarget(), player, event.getItemStack());
        }
    }

    /** A player offers an ingot from this stack. True: the Mika took it. */
    public static boolean offer(MikaEntity mika, ServerPlayer player, ItemStack stack) {
        if (isAngryAt(mika, player)) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.nether_mika.boos").withStyle(ChatFormatting.RED));
            return false;
        }
        if (isAdmiring(mika)) {
            player.sendOverlayMessage(Component.translatable("quest.guhs.nether_mika.bezig").withStyle(ChatFormatting.GOLD));
            return false;
        }
        ItemStack ingot = stack.copyWithCount(1);
        stack.consume(1, player);
        startAdmiring(mika, ingot, player.getUUID());
        player.sendOverlayMessage(Component.translatable("quest.guhs.nether_mika.hmm").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Hit a Nether-Mika and every Nether-Mika around is angry at you (vads or no vads). */
    static void onHurt(LivingIncomingDamageEvent event) {
        if (isNetherMika(event.getEntity()) && event.getSource().getEntity() instanceof Player player
                && event.getEntity().level() instanceof ServerLevel level) {
            MikaEntity hurt = (MikaEntity) event.getEntity();
            for (MikaEntity other : level.getEntitiesOfClass(MikaEntity.class, hurt.getBoundingBox().inflate(ALERT_RANGE), NetherMikaRuil::isNetherMika)) {
                makeAngry(other, player);
            }
        }
    }

    /** A Nether-Mika doesn't go after someone wearing vads (unless it's angry at them). */
    static void onChangeTarget(LivingChangeTargetEvent event) {
        if (isNetherMika(event.getEntity()) && event.getNewAboutToBeSetTarget() instanceof Player player
                && !isHostileTo((MikaEntity) event.getEntity(), player)) {
            event.setCanceled(true);
        }
    }

    // --- admiring and trading ----------------------------------------------------------------------------------------------

    public static boolean isAdmiring(MikaEntity mika) {
        return mika.getPersistentData().getLongOr(ADMIRE_UNTIL, 0L) > 0;
    }

    static void startAdmiring(MikaEntity mika, ItemStack ingot, @Nullable UUID partner) {
        mika.setItemSlot(EquipmentSlot.MAINHAND, ingot);
        mika.setDropChance(EquipmentSlot.MAINHAND, 1.0f);
        CompoundTag data = mika.getPersistentData();
        data.putLong(ADMIRE_UNTIL, mika.level().getGameTime() + ADMIRE_TICKS);
        if (partner != null) {
            data.store(PARTNER, UUIDUtil.CODEC, partner);
        } else {
            data.remove(PARTNER);
        }
        mika.setTarget(null);
        mika.getNavigation().stop();
        mika.playSound(SoundEvents.PIGLIN_ADMIRING_ITEM, 1.0f, 1.3f);
    }

    /** Done sniffing (or interrupted): with {@code trade} it throws its present. */
    static void stopAdmiring(MikaEntity mika, boolean trade) {
        CompoundTag data = mika.getPersistentData();
        if (!isAdmiring(mika)) {
            return;
        }
        data.remove(ADMIRE_UNTIL);
        ItemStack held = mika.getItemBySlot(EquipmentSlot.MAINHAND);
        mika.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        if (!(mika.level() instanceof ServerLevel level)) {
            return;
        }
        Player partner = data.read(PARTNER, UUIDUtil.CODEC).isPresent() ? level.getPlayerByUUID(data.read(PARTNER, UUIDUtil.CODEC).orElseThrow()) : null;
        data.remove(PARTNER);
        if (!trade) {
            if (!held.isEmpty()) {
                BehaviorUtils.throwItem(mika, held, mika.position().add(0, 1, 0));   // (interrupted: it drops the ingot)
            }
            return;
        }
        List<ItemStack> loot = barter(level, mika);
        Vec3 to = partner != null && partner.distanceToSqr(mika) < 16 * 16 ? partner.position() : mika.position().add(
                mika.getRandom().nextInt(7) - 3, 0, mika.getRandom().nextInt(7) - 3);
        for (ItemStack stack : loot) {
            BehaviorUtils.throwItem(mika, stack, to.add(0, 1, 0));
        }
        mika.playSound(SoundEvents.PIGLIN_CELEBRATE, 1.0f, 1.3f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mika.getX(), mika.getY() + 1.0, mika.getZ(), 6, 0.3, 0.3, 0.3, 0.0);
        if (partner instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("quest.guhs.nether_mika.ruil").withStyle(ChatFormatting.GOLD));
            GuhAdvancements.grant(sp, "nether_mika_ruil");
            SpiesburchtStats.award(sp, "barbecuether/ruilen");
        }
    }

    /** What a Nether-Mika gives for one vahoege vads ingot. */
    public static List<ItemStack> barter(ServerLevel level, MikaEntity mika) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(RUIL_LOOT);
        return table.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.THIS_ENTITY, mika)
                .create(LootContextParamSets.PIGLIN_BARTER));
    }

    /** While it's sniffing an ingot the Mika stands still and does nothing else (not even chase you). */
    static class AdmireGoal extends Goal {
        private final MikaEntity mika;

        AdmireGoal(MikaEntity mika) {
            this.mika = mika;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return isAdmiring(mika);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            mika.getNavigation().stop();
        }

        @Override
        public void tick() {
            mika.getNavigation().stop();
            mika.setXRot(35f);
            if (mika.level() instanceof ServerLevel level) {
                if (mika.tickCount % 6 == 0) {
                    level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, mika.getMainHandItem().isEmpty()
                            ? new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get()) : mika.getMainHandItem()),
                            mika.getX(), mika.getY() + mika.getBbHeight() + 0.2, mika.getZ(), 2, 0.15, 0.1, 0.15, 0.02);
                }
                if (mika.getPersistentData().getLongOr(ADMIRE_UNTIL, 0L) <= level.getGameTime()) {
                    stopAdmiring(mika, true);
                }
            }
        }
    }

    /** An ingot on the ground (thrown for it): it walks over and picks it up. */
    static class FetchIngotGoal extends Goal {
        private final MikaEntity mika;
        @Nullable
        private ItemEntity ingot;

        FetchIngotGoal(MikaEntity mika) {
            this.mika = mika;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (isAdmiring(mika) || mika.getTarget() != null) {
                return false;
            }
            List<ItemEntity> items = mika.level().getEntitiesOfClass(ItemEntity.class, mika.getBoundingBox().inflate(9, 3, 9),
                    e -> e.isAlive() && e.getItem().is(ModItems.VAHOEGE_VADS_INGOT.get()) && !e.hasPickUpDelay());
            ingot = items.stream().min((a, b) -> Double.compare(a.distanceToSqr(mika), b.distanceToSqr(mika))).orElse(null);
            return ingot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return ingot != null && ingot.isAlive() && !isAdmiring(mika) && mika.getTarget() == null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (ingot == null) {
                return;
            }
            if (mika.distanceToSqr(ingot) < 2.5) {
                ItemStack stack = ingot.getItem();
                ItemStack one = stack.split(1);
                if (stack.isEmpty()) {
                    ingot.discard();
                } else {
                    ingot.setItem(stack);
                }
                Entity thrower = ingot.getOwner();
                startAdmiring(mika, one, thrower instanceof Player p ? p.getUUID() : null);
                ingot = null;
            } else {
                mika.getNavigation().moveTo(ingot, 1.1);
            }
        }
    }

    private NetherMikaRuil() {
    }
}
