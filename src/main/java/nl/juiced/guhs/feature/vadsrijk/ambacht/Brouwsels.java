package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.LinkedHashMap;
import java.util.Map;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.juiced.guhs.feature.vadsrijk.economie.Drinken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Brouwen: the brewed potions (items {@code guhs:vr_amb_drank_<id>}). The same tiers and numbers as the shop potions (economie
 * Drinken), but with Vadsigheid: up to 30 % more. They are in the SAME shared 10 s potion cooldown (no stacking, VOORSTEL §4).
 * The Honingelixer (royal jelly) is the brewer's own: Vads + energy + it washes off Plakkerig and Gesporen.
 * (Crystal cutting is a Brouwen recipe too: see {@link Recepten}.)
 */
public final class Brouwsels {
    public record Drank(String id, int tier, int vads, int energie, boolean wast) {
        public String itemId() {
            return "vr_amb_drank_" + id;
        }
    }

    public static final Map<String, Drank> DRANKEN = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<DrankItem>> ITEM = new LinkedHashMap<>();

    static {
        d("vadsdrankje", 1, Drinken.VADS, 0, false);
        d("energiedrankje", 1, 0, Drinken.ENERGIE, false);
        d("grote_vadsdrank", 2, Drinken.GROOT_VADS, 0, false);
        d("grote_energiedrank", 2, 0, Drinken.GROOT_ENERGIE, false);
        d("reuzen_vadsdrank", 3, 1500, 0, false);
        d("reuzen_energiedrank", 3, 0, 200, false);
        d("honingelixer", 3, 800, 100, true);
        // U-AMBACHT2: zone 4 (T4) and zone 5 (T5)
        d("zee_vadsdrank", 4, 1900, 0, false);
        d("zee_energiedrank", 4, 0, 250, false);
        d("wolken_vadsdrank", 5, 2400, 0, false);
        d("wolken_energiedrank", 5, 0, 300, false);
        d("hemelelixer", 5, 1100, 140, true);
    }

    private Brouwsels() {
    }

    private static void d(String id, int tier, int vads, int energie, boolean wast) {
        Drank d = new Drank(id, tier, vads, energie, wast);
        DRANKEN.put(id, d);
        ITEM.put(id, Materialen.ITEMS.registerItem(d.itemId(), p -> new DrankItem(p, d), () -> Drinken.eigenschappen(true)));
    }

    public static class DrankItem extends Item {
        public final Drank drank;

        public DrankItem(Properties p, Drank d) {
            super(p);
            this.drank = d;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (Drinken.buiten(player, "vadsrijk.economie.drankje.buiten")) {
                return InteractionResult.FAIL;
            }
            return super.use(level, player, hand);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            if (entity instanceof ServerPlayer sp && Vadsrijk.in(sp)) {
                werk(sp, Gemaakt.factor(stack));
            }
            return super.finishUsingItem(stack, level, entity);
        }

        /** What it does (also the gametest). */
        public void werk(ServerPlayer sp, double factor) {
            if (drank.vads() > 0) {
                Schade.heal(sp, sp, drank.vads() * factor);
            }
            if (drank.energie() > 0) {
                Drinken.energie(sp, drank.energie() * factor);
            }
            if (drank.wast()) {
                Statussen.wis(sp, Status.PLAKKERIG);
                Statussen.wis(sp, Status.GESPOREN);
            }
            if (sp.level() instanceof ServerLevel sl) {
                sl.sendParticles(drank.vads() > 0 ? ParticleTypes.HEART : ParticleTypes.GLOW, sp.getX(), sp.getY() + 1.4, sp.getZ(), 8, 0.35, 0.35, 0.35, 0.01);
                sl.playSound(null, sp.blockPosition(), SoundEvents.HONEY_DRINK.value(), SoundSource.PLAYERS, 0.6f, 1.2f);
            }
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display, Consumer<Component> t, TooltipFlag flag) {
            double f = Gemaakt.factor(stack);
            if (drank.vads() > 0) {
                t.accept(Component.translatable("vadsrijk.ambacht.drank.vads", Math.round(drank.vads() * f)).withStyle(ChatFormatting.GREEN));
            }
            if (drank.energie() > 0) {
                t.accept(Component.translatable("vadsrijk.ambacht.drank.energie", Math.round(drank.energie() * f)).withStyle(ChatFormatting.GREEN));
            }
            if (drank.wast()) {
                t.accept(Component.translatable("vadsrijk.ambacht.drank.wast").withStyle(ChatFormatting.GREEN));
            }
            t.accept(Component.translatable("vadsrijk.economie.drankje.wacht").withStyle(ChatFormatting.DARK_GRAY));
            Gemaakt.tooltip(stack, t);
            t.accept(Component.translatable("vadsrijk.economie.alleen_rijk").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }
}
