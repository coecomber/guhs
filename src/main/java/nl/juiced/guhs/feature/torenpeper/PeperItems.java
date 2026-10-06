package nl.juiced.guhs.feature.torenpeper;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.spiesburcht.GuhdrankjeItem;
import nl.juiced.guhs.quest.GuhAdvancements;

/** The small item classes of bbq2 (toren-peper): the seeds, the three peppers, the two drinks, and the Peperadem effect. */
public final class PeperItems {
    private PeperItems() {
    }

    /** An item with a grey lore line ({@code item.guhs.<id>.lore}). */
    public static class Lore extends Item {
        public Lore(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** Peperzaadjes: the seed of the {@link PeperplantBlock}, with the three grounds in its tooltip. */
    public static class Zaadjes extends BlockItem {
        public Zaadjes(Block plant, Properties properties) {
            super(plant, properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.torenpeper_peperzaadjes.lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("item.guhs.torenpeper_peperzaadjes.grond.groen").withStyle(ChatFormatting.GREEN));
            tooltip.accept(Component.translatable("item.guhs.torenpeper_peperzaadjes.grond.rood").withStyle(ChatFormatting.RED));
            tooltip.accept(Component.translatable("item.guhs.torenpeper_peperzaadjes.grond.roze").withStyle(ChatFormatting.LIGHT_PURPLE));
            tooltip.accept(Component.translatable("item.guhs.torenpeper_peperzaadjes.glas").withStyle(ChatFormatting.AQUA));
        }
    }

    /** A pepper. The red one bites back when you eat it raw: a sprint, a puff of smoke and a red face. Never any harm. */
    public static class Peper extends Lore {
        /** How long the sprint of a raw Vahoegpeper lasts. */
        public static final int HEET_TICKS = 200;
        public final PeperSoort soort;

        public Peper(PeperSoort soort, Properties properties) {
            super(properties);
            this.soort = soort;
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack rest = super.finishUsingItem(stack, level, entity);
            if (level instanceof ServerLevel server) {
                Vec3 mond = entity.getEyePosition().add(entity.getLookAngle().scale(0.3));
                if (soort == PeperSoort.ROOD) {
                    entity.addEffect(new MobEffectInstance(MobEffects.SPEED, HEET_TICKS, 1));
                    server.playSound(null, entity.blockPosition(), TorenpeperFeature.HEET.get(), SoundSource.PLAYERS, 1f, 1f);
                    server.sendParticles(ParticleTypes.SMOKE, mond.x, mond.y + 0.2, mond.z, 14, 0.25, 0.15, 0.25, 0.02);
                    server.sendParticles(ParticleTypes.SMALL_FLAME, mond.x, mond.y - 0.1, mond.z, 4, 0.1, 0.05, 0.1, 0.01);
                    if (entity instanceof ServerPlayer p) {
                        p.sendOverlayMessage(Component.translatable("gui.guhs.torenpeper.heet").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                        GuhAdvancements.grant(p, "toren_peper_heet");
                        GidsFeature.grant(p, "barbecuether/toren_peper_heet");
                    }
                } else if (soort == PeperSoort.ROZE) {
                    server.sendParticles(ParticleTypes.HEART, mond.x, mond.y + 0.3, mond.z, 2, 0.2, 0.1, 0.2, 0.0);
                }
            }
            return rest;
        }
    }

    /** One of the two pepper drinks: a Guhdrankje that also tells the Peperteler-guh's questline it was tasted. */
    public static class Drankje extends GuhdrankjeItem {
        public Drankje(Brouwsel brouwsel, Properties properties) {
            super(brouwsel, properties);
        }

        @Override
        public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
            ItemStack rest = super.finishUsingItem(stack, level, entity);
            if (entity instanceof ServerPlayer p) {
                Pepertuin.geproefd(p);
            }
            return rest;
        }
    }

    /** Peperadem: a little flame with every breath, and the cold can't get you (freezing is taken away). Sets nothing on fire. */
    public static class Peperadem extends MobEffect {
        public Peperadem(MobEffectCategory category, int colour) {
            super(category, colour);
        }

        @Override
        public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
            entity.setTicksFrozen(0);
            Vec3 mond = entity.getEyePosition().add(entity.getLookAngle().scale(0.4));
            level.sendParticles(ParticleTypes.SMALL_FLAME, mond.x, mond.y - 0.15, mond.z, 2, 0.05, 0.03, 0.05, 0.01);
            level.sendParticles(ParticleTypes.SMOKE, mond.x, mond.y, mond.z, 1, 0.05, 0.05, 0.05, 0.005);
            return true;
        }

        @Override
        public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
            return tickCount % 30 == 0;
        }
    }
}
