package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.Drinken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Zone-3 items of the systemen slice: the Gouden Veer (right-click = onto your feather counter), the Vahoegzegel (the key item for
 * the Vahoeg altar at a dungeon door), the Premiekist (the optional weekly chest) and the tier-3 potions.
 */
public final class Z3Spul {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredItem<GoudenVeer> GOUDEN_VEER = ITEMS.registerItem("vr_z3_gouden_veer", GoudenVeer::new,
            () -> new Item.Properties().stacksTo(64).rarity(Rarity.EPIC));
    public static final DeferredItem<Uitleg> VAHOEGZEGEL = ITEMS.registerItem("vr_z3_vahoegzegel", Uitleg::new,
            () -> new Item.Properties().stacksTo(16).rarity(Rarity.RARE));
    public static final DeferredItem<Premiekist> PREMIEKIST = ITEMS.registerItem("vr_z3_premiekist", Premiekist::new,
            () -> new Item.Properties().stacksTo(16).rarity(Rarity.EPIC));

    /** Tier-3 potions (lv 40+ shop stock, flat numbers like the other tiers). */
    public static final int REUZEN_VADS = 1500, REUZEN_ENERGIE = 200, THEE_VADS = 400, THEE_ENERGIE = 60;

    public enum Soort {
        REUZEN_VADS, REUZEN_ENERGIE, BLOESEMTHEE
    }

    public static final DeferredItem<Drank> REUZEN_VADSDRANK = drank("vr_z3_reuzen_vadsdrank", Soort.REUZEN_VADS);
    public static final DeferredItem<Drank> REUZEN_ENERGIEDRANK = drank("vr_z3_reuzen_energiedrank", Soort.REUZEN_ENERGIE);
    public static final DeferredItem<Drank> BLOESEMTHEE = drank("vr_z3_bloesemthee", Soort.BLOESEMTHEE);

    private Z3Spul() {
    }

    private static DeferredItem<Drank> drank(String id, Soort s) {
        return ITEMS.registerItem(id, p -> new Drank(p, s), () -> Drinken.eigenschappen(true));
    }

    /** An item with a grey explanation line ({@code <description id>.uitleg}). */
    public static class Uitleg extends Item {
        public Uitleg(Properties p) {
            super(p);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            t.accept(Component.translatable(getDescriptionId() + ".uitleg").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Right-click: onto your Gouden Veren counter (sneak: the whole stack). */
    public static class GoudenVeer extends Uitleg {
        public GoudenVeer(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            ItemStack s = player.getItemInHand(hand);
            if (player instanceof ServerPlayer sp) {
                int n = sp.isShiftKeyDown() ? s.getCount() : 1;
                s.shrink(n);
                GoudenVeren.geef(sp, n);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** The weekly bounty chest: right-click = open ({@link Premies#openKist}). */
    public static class Premiekist extends Uitleg {
        public Premiekist(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                ItemStack s = player.getItemInHand(hand);
                s.shrink(1);
                Premies.openKist(sp);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** A zone-3 potion. Only works in the Vadsrijk. */
    public static class Drank extends Item {
        public final Soort soort;

        public Drank(Properties p, Soort soort) {
            super(p);
            this.soort = soort;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (Drinken.buiten(player, "vadsrijk.economie.drankje.buiten")) {
                return InteractionResult.FAIL;
            }
            return super.use(level, player, hand);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, net.minecraft.world.entity.LivingEntity entity) {
            if (entity instanceof ServerPlayer sp && Vadsrijk.in(sp)) {
                werk(sp);
            }
            return super.finishUsingItem(stack, level, entity);
        }

        /** What it does (also called by the gametest). */
        public void werk(ServerPlayer sp) {
            switch (soort) {
                case REUZEN_VADS -> Schade.heal(sp, sp, REUZEN_VADS);
                case REUZEN_ENERGIE -> Drinken.energie(sp, REUZEN_ENERGIE);
                case BLOESEMTHEE -> {
                    Schade.heal(sp, sp, THEE_VADS);
                    Drinken.energie(sp, THEE_ENERGIE);
                    Statussen.wis(sp, Status.PLAKKERIG);
                    Statussen.wis(sp, Status.GESPOREN);
                }
            }
            if (sp.level() instanceof ServerLevel sl) {
                sl.sendParticles(soort == Soort.REUZEN_ENERGIE ? ParticleTypes.GLOW : soort == Soort.BLOESEMTHEE ? ParticleTypes.CHERRY_LEAVES : ParticleTypes.HEART,
                        sp.getX(), sp.getY() + 1.4, sp.getZ(), 10, 0.35, 0.35, 0.35, 0.01);
                sl.playSound(null, sp.blockPosition(), SoundEvents.HONEY_DRINK.value(), SoundSource.PLAYERS, 0.6f, 1.1f);
            }
            sp.sendOverlayMessage(Component.translatable(getDescriptionId() + ".op").withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".uitleg").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("vadsrijk.economie.drankje.wacht").withStyle(ChatFormatting.DARK_GRAY));
            tooltip.accept(Component.translatable("vadsrijk.economie.alleen_rijk").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
