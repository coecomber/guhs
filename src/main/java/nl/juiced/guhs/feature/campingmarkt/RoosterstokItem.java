package nl.juiced.guhs.feature.campingmarkt;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.kamperen.KampvuurMarshmallow;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The Roosterstok of the Kampbaas-guh (bbq2, camping-markt): roasting a marshmallow is a little game of timing. Hold
 * right-click on a burning camp fire (with a marshmallow in your pockets): the marshmallow on the stick goes from cold to
 * warm to golden brown to burnt, and what you get depends on when you let go ({@link #uitkomst}):
 * <ul>
 *   <li>too early: still cold, nothing happened;</li>
 *   <li>golden brown (between {@link #GOUD_VAN} and {@link #GOUD_TOT} ticks): you eat it, warm and sticky; it counts for the
 *       Kampbaas-guh's questline and for the Knus tab;</li>
 *   <li>too late: burnt to a crisp. Nothing is lost (failing costs nothing): you wipe the stick and try again.</li>
 * </ul>
 * Works over the big camp fire of the Grillcamping and over any lit vanilla camp fire, at any time of day.
 */
public class RoosterstokItem extends Item {
    /** Ticks of holding: warm from here, golden brown from/to here, and it is taken out of the fire for you here. */
    public static final int WARM = 20, GOUD_VAN = 45, GOUD_TOT = 75, MAX = 120;
    /** (player persistent data) the fire a player is roasting over. */
    static final String VUUR = "guhs_campingmarkt_vuur";

    /** What a marshmallow is after this many ticks over the fire. */
    public enum Uitkomst { KOUD, GOUDBRUIN, VERKOOLD }

    public RoosterstokItem(Properties properties) {
        super(properties);
    }

    public static Uitkomst uitkomst(int ticks) {
        return ticks < GOUD_VAN ? Uitkomst.KOUD : ticks <= GOUD_TOT ? Uitkomst.GOUDBRUIN : Uitkomst.VERKOOLD;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    /** A click that was not on the big camp fire itself: over a lit camp fire the player looks at, roasting starts too. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockPos vuur = Kampvuur.inZicht(player);
        if (player instanceof ServerPlayer p) {
            if (vuur == null) {
                Kamperen.meld(p, "stok.geen_vuur", ChatFormatting.LIGHT_PURPLE);
                return InteractionResult.FAIL;
            }
            return begin(p, hand, vuur) ? InteractionResult.CONSUME : InteractionResult.FAIL;
        }
        if (vuur == null || !heeftMarshmallow(player)) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** Does this player carry a marshmallow (the item tag guhs:knus/marshmallow)? Both sides. */
    public static boolean heeftMarshmallow(Player player) {
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(KnusTags.MARSHMALLOW)) {
                return true;
            }
        }
        return false;
    }

    /** Starts roasting over this fire (the player needs a marshmallow). True when it started. */
    public static boolean begin(ServerPlayer p, InteractionHand hand, BlockPos vuur) {
        if (Kamperen.marshmallows(p) == 0) {
            Kamperen.meld(p, "stok.geen_marshmallow", ChatFormatting.LIGHT_PURPLE);
            p.stopUsingItem();
            return false;
        }
        p.getPersistentData().putLong(VUUR, vuur.asLong());
        p.startUsingItem(hand);
        return true;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide() || !(user instanceof ServerPlayer p)) {
            return;
        }
        int t = getUseDuration(stack, user) - remaining;
        BlockPos vuur = BlockPos.of(p.getPersistentData().getLongOr(VUUR, 0L));
        if (!Kampvuur.brandt(level, vuur) || vuur.distToCenterSqr(p.position()) > 36) {
            p.stopUsingItem();
            Kamperen.meld(p, "stok.vuur_weg", ChatFormatting.LIGHT_PURPLE);
            return;
        }
        if (t >= MAX) {
            p.stopUsingItem();
            rooster(p, t);
            return;
        }
        if (t == GOUD_VAN) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.7f, 1.6f);
        } else if (t == GOUD_TOT + 1) {
            p.level().playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.35f, 1.5f);
        }
        if (t % 5 == 0) {
            String fase = t < WARM ? "koud" : t < GOUD_VAN ? "warm" : t <= GOUD_TOT ? "goud" : "zwart";
            ChatFormatting kleur = t < WARM ? ChatFormatting.WHITE : t < GOUD_VAN ? ChatFormatting.YELLOW : t <= GOUD_TOT ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY;
            Kamperen.meld(p, "stok.bezig." + fase, kleur);
            ((ServerLevel) level).sendParticles(t > GOUD_TOT ? ParticleTypes.SMOKE : ParticleTypes.SMALL_FLAME, vuur.getX() + 0.5, vuur.getY() + 1.0, vuur.getZ() + 0.5,
                    2, 0.15, 0.1, 0.15, 0.01);
        }
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!level.isClientSide() && entity instanceof ServerPlayer p) {
            rooster(p, getUseDuration(stack, entity) - timeLeft);
        }
        return false;
    }

    /**
     * The player takes the marshmallow out of the fire after this many ticks (public for the tests). Golden brown: one
     * marshmallow of their pockets is eaten and counted. Returns what it became.
     */
    public static Uitkomst rooster(ServerPlayer p, int ticks) {
        ServerLevel level = p.level();
        Uitkomst u = uitkomst(ticks);
        switch (u) {
            case KOUD -> Kamperen.meld(p, ticks < WARM ? "stok.koud" : "stok.bijna", ChatFormatting.LIGHT_PURPLE);
            case VERKOOLD -> {
                Kamperen.meld(p, "stok.verkoold", ChatFormatting.DARK_GRAY);
                level.sendParticles(ParticleTypes.LARGE_SMOKE, p.getX(), p.getEyeY() - 0.2, p.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
                level.playSound(null, p.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            case GOUDBRUIN -> {
                ItemStack mm = ItemStack.EMPTY;
                for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
                    if (s.is(KnusTags.MARSHMALLOW)) {
                        mm = s;
                        break;
                    }
                }
                if (mm.isEmpty()) {
                    Kamperen.meld(p, "stok.geen_marshmallow", ChatFormatting.LIGHT_PURPLE);
                    return Uitkomst.KOUD;
                }
                if (!p.getAbilities().instabuild) {
                    mm.shrink(1);
                }
                p.getFoodData().eat(4, 0.8f);
                level.sendParticles(ParticleTypes.HEART, p.getX(), p.getEyeY() + 0.5, p.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
                level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
                KnusVoortgang.tel(p, KampvuurMarshmallow.GEROOSTERD, 1);
                GuhAdvancements.grant(p, "camping_markt_goudbruin");
                Verhaallijn lijn = CampingmarktFeature.CAMPING;
                if (lijn.stap(p) == 4 && lijn.teller(p, Kamperen.GEROOSTERD) < Kamperen.MARSHMALLOWS) {
                    int n = lijn.teller(p, Kamperen.GEROOSTERD) + 1;
                    lijn.teller(p, Kamperen.GEROOSTERD, n);
                    if (n >= Kamperen.MARSHMALLOWS) {
                        Kamperen.meld(p, "stok.goud_klaar", ChatFormatting.GOLD);
                        GuhQuests.hint(p, "quest.guhs.campingmarkt.hint.kampbaas");
                        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
                    } else {
                        Kamperen.meld(p, "stok.goud_n", ChatFormatting.GOLD, n, Kamperen.MARSHMALLOWS);
                    }
                } else {
                    Kamperen.meld(p, "stok.goud", ChatFormatting.GOLD);
                }
            }
        }
        return u;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable(getDescriptionId() + ".lore2").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
