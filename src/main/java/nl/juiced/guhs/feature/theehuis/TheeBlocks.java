package nl.juiced.guhs.feature.theehuis;

import java.util.List;
import java.util.Locale;

import com.mojang.serialization.MapCodec;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The blocks, items and the effect of the Knabbelthee-huisje (registered in {@link TheehuisFeature}). */
public final class TheeBlocks {
    /** The four teas (the Knus collection "theesoorten"; item ids = entry ids). */
    public enum Soort {
        KNABBELTHEE, KAASMELKTHEE, THEEKRUIDTHEE, GUHBLOEMENTHEE;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Anything but plain knabbelthee is a special tea (more gezelligheid when you pour it). */
        public boolean bijzonder() {
            return this != KNABBELTHEE;
        }
    }

    /**
     * guhs:theetafel: a round tea table with a flowery cloth; during a theekransje it's laid (cups, the teapot, cakes).
     * The guhs sit on the guh_stoelen around it.
     */
    public static class Theetafel extends Block {
        public static final BooleanProperty GEDEKT = BooleanProperty.create("gedekt");
        private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 13, 0, 16, 16, 16), Block.box(6, 0, 6, 10, 13, 10), Block.box(3, 0, 3, 13, 1.5, 13));

        public Theetafel(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(GEDEKT, false));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(GEDEKT);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }
    }

    /**
     * guhs:theepotje: a little teapot with a guh face on its lid. Put something tasty in it (right-click) and you get two
     * cups of tea: kaasknabbels make knabbelthee, and from #guhs:knus/kaasmelk, #theekruid and #guhbloem the other teas.
     */
    public static class Theepotje extends HorizontalDirectionalBlock {
        public static final MapCodec<Theepotje> CODEC = simpleCodec(Theepotje::new);
        private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 10, 13);
        public static final int KOPJES = 2;

        public Theepotje(Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return SHAPE;
        }

        @Override
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                                  BlockHitResult hit) {
            Soort soort = thee(stack);
            if (soort == null) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (!level.isClientSide() && player instanceof ServerPlayer sp) {
                zet(sp, (ServerLevel) level, pos, stack, soort);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.theehuis.theepotje.leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) == 0) {
                Direction d = state.getValue(FACING);
                level.addParticle(TheehuisFeature.THEESTOOM.get(), pos.getX() + 0.5 + d.getStepX() * 0.35, pos.getY() + 0.65,
                        pos.getZ() + 0.5 + d.getStepZ() * 0.35, 0, 0.02, 0);
            }
        }
    }

    /** Which tea this ingredient makes in a theepotje (null: none). */
    public static Soort thee(ItemStack stack) {
        if (stack.is(ModItems.KAAS_KNABBELS.get())) {
            return Soort.KNABBELTHEE;
        } else if (stack.is(KnusTags.KAASMELK)) {
            return Soort.KAASMELKTHEE;
        } else if (stack.is(KnusTags.THEEKRUID)) {
            return Soort.THEEKRUIDTHEE;
        } else if (stack.is(KnusTags.GUHBLOEM)) {
            return Soort.GUHBLOEMENTHEE;
        }
        return null;
    }

    /** Makes tea: one ingredient in, two cups out (the kind goes into the theesoorten collection). */
    public static void zet(ServerPlayer player, ServerLevel level, BlockPos pos, ItemStack stack, Soort soort) {
        if (!player.getAbilities().instabuild) {
            ItemStack rest = stack.getCraftingRemainingItem();
            stack.shrink(1);
            if (!rest.isEmpty()) {
                Minigames.give(player, rest);          // (the empty bottle of the kaasmelk)
            }
        }
        Minigames.give(player, new ItemStack(TheehuisFeature.thee(soort), Theepotje.KOPJES));
        level.playSound(null, pos, TheehuisFeature.INSCHENKEN.get(), SoundSource.BLOCKS, 1f, 1.0f + level.getRandom().nextFloat() * 0.2f);
        level.sendParticles(TheehuisFeature.THEESTOOM.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.15, 0.1, 0.15, 0.01);
        KnusVoortgang.ontdek(player, TheehuisVoortgang.THEESOORTEN, soort.id());
        KnusVoortgang.hoogste(player, TheehuisVoortgang.SOORTEN, KnusVoortgang.ontdekt(player, TheehuisVoortgang.THEESOORTEN).size());
        KnusVoortgang.tel(player, TheehuisVoortgang.GEZET, Theepotje.KOPJES);
        GuhAdvancements.grant(player, "theehuis_thee_gezet");
        player.sendOverlayMessage(Component.translatable("gui.guhs.theehuis.gezet", Theepotje.KOPJES,
                Component.translatable("item.guhs." + soort.id())).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** A cup of tea: you drink it (a little food, and each kind does something knus). */
    public static class Thee extends Item {
        public final Soort soort;

        public Thee(Soort soort, Properties properties) {
            super(properties.food(new FoodProperties.Builder().nutrition(soort == Soort.KNABBELTHEE ? 3 : 2).saturationModifier(0.5f).alwaysEdible().build()));
            this.soort = soort;
        }

        @Override
        public ItemUseAnimation getUseAnimation(ItemStack stack) {
            return ItemUseAnimation.DRINK;
        }

        @Override
        public SoundEvent getDrinkingSound() {
            return SoundEvents.GENERIC_DRINK;
        }

        @Override
        public SoundEvent getEatingSound() {
            return SoundEvents.GENERIC_DRINK;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!level.isClientSide()) {
                switch (soort) {
                    case KNABBELTHEE -> entity.addEffect(new MobEffectInstance(TheehuisFeature.GEZELLIG, 20 * 30));
                    case KAASMELKTHEE -> entity.getActiveEffects().stream().filter(e -> !e.getEffect().value().isBeneficial()).map(MobEffectInstance::getEffect)
                            .toList().forEach(entity::removeEffect);
                    case THEEKRUIDTHEE -> entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 8));
                    case GUHBLOEMENTHEE -> entity.addEffect(new MobEffectInstance(TheehuisFeature.GEZELLIG, 20 * 90));
                }
                if (entity instanceof ServerPlayer player) {
                    KnusVoortgang.ontdek(player, TheehuisVoortgang.THEESOORTEN, soort.id());
                    KnusVoortgang.hoogste(player, TheehuisVoortgang.SOORTEN, KnusVoortgang.ontdekt(player, TheehuisVoortgang.THEESOORTEN).size());
                }
            }
            return super.finishUsingItem(stack, level, entity);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** An item with a line of Dutch lore (lang &lt;item&gt;.lore). */
    public static class Lore extends Item {
        public Lore(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** A block item with a line of lore. */
    public static class LoreBlock extends net.minecraft.world.item.BlockItem {
        public LoreBlock(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * guhs:gezellig: from a gezellige theekransje (and a cup of knabbelthee or guhbloementhee). You slowly heal and little
     * hearts float around you. Guhs love it.
     */
    public static class Gezellig extends MobEffect {
        public Gezellig() {
            super(MobEffectCategory.BENEFICIAL, 0xFF8FC8);
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
            return duration % 80 == 0;
        }

        @Override
        public boolean applyEffectTick(LivingEntity entity, int amplifier) {
            if (entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(1.0f + amplifier);
            }
            if (entity.level() instanceof ServerLevel server) {
                server.sendParticles(TheehuisFeature.GEZELLIG_HARTJE.get(), entity.getX(), entity.getY() + entity.getBbHeight() + 0.2, entity.getZ(),
                        2, 0.3, 0.1, 0.3, 0.01);
            }
            return true;
        }
    }

    static void hartjes(ServerLevel level, double x, double y, double z, int n) {
        level.sendParticles(ParticleTypes.HEART, x, y, z, n, 0.3, 0.2, 0.3, 0.01);
    }

    private TheeBlocks() {
    }
}
