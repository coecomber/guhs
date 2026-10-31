package nl.juiced.guhs.feature.maagwand;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * What the maagwand does while eating: too vadsig food ends in a BOERP (the item stays), training food counts, and the
 * bonuses of the layers: 2 eats faster, 3 fills more, 4 is immune to hunger cramps, 5 burps mobs away and wears the Vadsaura.
 * Bonuses add up.
 */
final class MaagwandEten {
    /** How many ticks before the end of the bite the stomach protests: late enough to really try, before vanilla finishes. */
    static final int STOP_BIJ = 2;
    /** Ticks the food stays out of reach after a BOERP, so holding the mouse button doesn't spam. */
    static final int ADEMPAUZE = 30;
    /** Ticks between two burps that push mobs away, and how far they reach. */
    static final int BOER_PAUZE = 200;
    static final double BOER_BEREIK = 5;
    /** The saturation the Frituurmaag adds: this part of the food's own. */
    static final float EXTRA_VERZADIGING = 0.25f;
    private static final int AURA_ELKE = 40;
    private static final String BOER_TICK = "guhs_maagwand_boer";
    private static final DustParticleOptions AURA = new DustParticleOptions(0xF6C446, 1.0f);

    private MaagwandEten() {
    }

    static int laagVan(Player p) {
        return p instanceof ServerPlayer sp ? Maagwand.laag(sp) : Maagwand.klantLaag;
    }

    static void opStart(LivingEntityUseItemEvent.Start event) {
        if (event.getEntity() instanceof Player p && event.getItem().has(DataComponents.FOOD) && laagVan(p) >= 2) {
            event.setDuration(Math.max(1, event.getDuration() * 3 / 4));
        }
    }

    static void opTick(LivingEntityUseItemEvent.Tick event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getDuration() <= STOP_BIJ && !Maagwand.magEten(p, event.getItem())) {
            p.stopUsingItem();
            boer(p, event.getItem());
        }
    }

    static void opKlaar(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        ItemStack gegeten = event.getItem();
        Maagwand.telTrainvoer(p, gegeten.getItem());
        FoodProperties voedsel = gegeten.get(DataComponents.FOOD);
        if (voedsel == null) {
            return;
        }
        int laag = Maagwand.laag(p);
        if (laag >= 3) {
            FoodData data = p.getFoodData();
            data.setSaturation(Math.min(data.getSaturationLevel() + voedsel.saturation() * EXTRA_VERZADIGING, data.getFoodLevel()));
        }
        if (laag >= 5) {
            duwBoer(p);
        }
    }

    static void opEffect(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getEffectInstance().getEffect().equals(MobEffects.HUNGER)
                && Maagwand.laag(p) >= 4) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    static void opSpelerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && p.tickCount % AURA_ELKE == 0 && Maagwand.laag(p) >= 5 && !p.isSpectator()) {
            p.level().sendParticles(AURA, p.getX(), p.getY() + 1, p.getZ(), 4, 0.5, 0.6, 0.5, 0);
        }
    }

    static void opLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Maagwand.stuur(p);
        }
    }

    /** The stomach says no: burp, the message, and a short breather. Nothing is lost. */
    static void boer(ServerPlayer p, ItemStack stack) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1f, 0.6f);
        p.getCooldowns().addCooldown(stack, ADEMPAUZE);
        p.sendOverlayMessage(Component.translatable("gui.guhs.maagwand.boerp"));
        Maagwand.Stap stap = Maagwand.stapNaar(Maagwand.laag(p) + 1);
        Component namen = stap == null ? null : Maagwand.trainvoerNamen(stap);
        if (namen != null) {
            p.sendSystemMessage(Component.translatable("gui.guhs.maagwand.eerst", stap.doel() - Maagwand.teller(p), namen,
                    Component.translatable("gui.guhs.maagwand.laag." + Maagwand.nodigeLaag(stack.getItem()))).withStyle(ChatFormatting.LIGHT_PURPLE));
        } else {
            p.sendSystemMessage(Component.translatable("gui.guhs.maagwand.eerst_leeg").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** A placed food block asks first (the Roze guhkoek, eaten straight from its tray). */
    static boolean blokMag(ServerPlayer p, ItemStack stack) {
        if (Maagwand.magEten(p, stack)) {
            return true;
        }
        boer(p, stack);
        return false;
    }

    static void gestegen(ServerPlayer p, int laag) {
        ServerLevel level = p.level();
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 0.7f);
        level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.5, 0.5, 0.5, 0.05);
        p.sendSystemMessage(Component.translatable("gui.guhs.maagwand.gestegen", Component.translatable("gui.guhs.maagwand.laag." + laag),
                Component.translatable("gui.guhs.maagwand.bonus." + laag)).withStyle(ChatFormatting.GOLD));
    }

    /** The burp of the Onverwoestbare Vadsmaag: no harm done, but enemies are blown back. */
    static void duwBoer(ServerPlayer p) {
        long nu = p.level().getGameTime();
        if (nu - p.getPersistentData().getLongOr(BOER_TICK, -BOER_PAUZE) < BOER_PAUZE) {
            return;
        }
        p.getPersistentData().putLong(BOER_TICK, nu);
        ServerLevel level = p.level();
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 1.2f, 0.5f);
        level.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY() + 1.2, p.getZ(), 12, 0.6, 0.4, 0.6, 0.08);
        for (Mob mob : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(BOER_BEREIK), m -> m instanceof Enemy)) {
            Vec3 weg = p.position().subtract(mob.position());
            mob.knockback(1.2, weg.x, weg.z);
        }
    }
}
