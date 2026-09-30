package nl.juiced.guhs.feature.guhwaii;

import java.util.List;

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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
/** The items of Guhwai'i: the kokosnoot, kokosmelk, the ukelele, and block items with a line of lore. */
public final class GuhwaiiItems {
    private GuhwaiiItems() {
    }

    static void lore(Item item, Consumer<Component> tooltip) {
        tooltip.accept(Component.translatable(item.getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
    }

    /** A block item with one line of lore (lang: its key + ".lore"). */
    public static class LoreBlockItem extends BlockItem {
        public LoreBlockItem(Block block, Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            lore(this, tooltip);
        }
    }

    /**
     * De kokosnoot: eat it (right-click in the air: crack, slurp!), feed it to your guh (a snack), plant it on sand or
     * grass (it sprouts into a guh-palm), or hang it back under a palm frond.
     */
    public static class KokosnootItem extends Item {
        public KokosnootItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos();
            BlockState clicked = level.getBlockState(pos);
            BlockState plant = null;
            BlockPos plek = null;
            if (context.getClickedFace() == Direction.UP && GuhwaiiBlokken.eilandgrond(clicked) && level.isEmptyBlock(pos.above())) {
                plek = pos.above();
                plant = GuhwaiiFeature.PALM_KIEMPLANT.get().defaultBlockState();
            } else if (context.getClickedFace() == Direction.DOWN && clicked.is(GuhwaiiFeature.PALM_BLAD.get()) && level.isEmptyBlock(pos.below())) {
                plek = pos.below();
                plant = GuhwaiiFeature.KOKOSNOOT.get().defaultBlockState().setValue(GuhwaiiBlokken.HANGEND, true).setValue(GuhwaiiBlokken.RIJP, 0);
            }
            if (plant == null || !plant.canSurvive(level, plek)) {
                return InteractionResult.PASS;   // (then it's eaten: Item.use)
            }
            if (!level.isClientSide()) {
                level.setBlock(plek, plant, Block.UPDATE_ALL);
                level.playSound(null, plek, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.8f, 1.2f);
                level.gameEvent(context.getPlayer(), GameEvent.BLOCK_PLACE, plek);
                if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
                    context.getItemInHand().shrink(1);
                }
                if (context.getPlayer() instanceof ServerPlayer sp && plant.is(GuhwaiiFeature.PALM_KIEMPLANT.get())) {
                    GuhwaiiFeature.advancement(sp, "kiemplant");
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (!level.isClientSide() && entity instanceof ServerPlayer sp) {
                GuhwaiiFeature.advancement(sp, "kokosnoot");
            }
            return super.finishUsingItem(stack, level, entity);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            lore(this, tooltip);
        }
    }

    /** Kokosmelk: a cool drink in a bottle (a little regeneration), the bottle comes back. */
    public static class KokosmelkItem extends Item {
        public KokosmelkItem(Properties properties) {
            super(properties);
        }

        @Override
        public ItemUseAnimation getUseAnimation(ItemStack stack) {
            return ItemUseAnimation.DRINK;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack rest = super.finishUsingItem(stack, level, entity);
            if (entity instanceof ServerPlayer sp) {
                GuhwaiiFeature.advancement(sp, "kokosmelk");
            }
            if (entity instanceof Player p && p.getAbilities().instabuild) {
                return rest;
            }
            if (rest.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            if (entity instanceof Player p && !p.getInventory().add(new ItemStack(Items.GLASS_BOTTLE))) {
                p.drop(new ItemStack(Items.GLASS_BOTTLE), false);
            }
            return rest;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            lore(this, tooltip);
        }
    }

    /**
     * 626-guh's reserve-ukelele: strum it (right-click): a happy chord, music notes, and the guhs around you dance along;
     * a 626-guh strums along on his own ukelele!
     */
    public static class UkeleleItem extends Item {
        /** The chord shapes (note block pitches, a cheerful C - F - G - C). */
        private static final float[][] AKKOORDEN = {{0.749f, 0.943f, 1.122f}, {0.749f, 1.0f, 1.26f}, {0.84f, 1.059f, 1.26f}, {0.943f, 1.122f, 1.498f}};

        public UkeleleItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide() && level instanceof ServerLevel server) {
                tokkel(server, player.getX(), player.getEyeY() - 0.5, player.getZ(), server.getRandom().nextInt(AKKOORDEN.length));
                for (GuhEntity guh : server.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(8))) {
                    if (guh.emotes.current() == null && GuhEmotes.canStart(guh)) {
                        guh.emotes.start(guh.getVariant() == GuhVariant.STITCH626 ? Emote.UKELELE : Emote.DANSEN, false, GuhEmotes.Source.SELF);
                    }
                }
                if (player instanceof ServerPlayer sp) {
                    GuhwaiiFeature.advancement(sp, "ukelele_speler");
                }
            }
            player.getCooldowns().addCooldown(stack, 16);
            return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
        }

        /** One strum: three notes of a chord (guitar-ish note block) and a few music notes. */
        public static void tokkel(ServerLevel level, double x, double y, double z, int akkoord) {
            float[] a = AKKOORDEN[Math.floorMod(akkoord, AKKOORDEN.length)];
            for (float pitch : a) {
                level.playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_GUITAR.value(), SoundSource.PLAYERS, 0.6f, pitch);
            }
            level.sendParticles(ParticleTypes.NOTE, x, y + 0.6, z, 3, 0.4, 0.2, 0.4, level.getRandom().nextDouble());
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            lore(this, tooltip);
        }
    }
}
