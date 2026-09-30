package nl.juiced.guhs.feature.mewtwo;

import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.VariantGedrag;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.VerhaalVlaggen;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Guhtwo (GuhVariant MEWTWO, also its story copy):
 * <ul>
 *   <li>it hovers a little above the ground in a soft purple glow (the flag ZWEEFT: the client lifts the model, draws the
 *       glow and sparkles; see client.MewtwoUiterlijk);</li>
 *   <li>ridden, it floats over short gaps: for {@link #ZWEEF_TICKS} ticks after leaving the ground it doesn't fall (about 4
 *       blocks at a ridden guh's speed), then it sinks down gently;</li>
 *   <li>knabbel telekinesis (tamed ones, not the story copy; on/off with the button in its guh menu): items within
 *       {@link #TELEKINESE} blocks float to it, and on to its owner when the owner is close ({@link #telekinese});</li>
 *   <li>it eats x2: VOEREN hearts x2 (voerFactor; the day cap too), a funny double chomp with an "x2" and double heart puffs
 *       ({@link #gegeten}); no extra healing.</li>
 * </ul>
 */
public class MewtwoGedrag implements VariantGedrag {
    /** How far the knabbel telekinesis reaches (blocks). */
    public static final double TELEKINESE = 8.0;
    /** The owner within this many blocks: the items float on to them. */
    public static final double NAAR_BAAS = 12.0;
    /** Ridden: this many ticks in the air without falling (then it sinks). */
    public static final int ZWEEF_TICKS = 16;
    /** Persistent key: the telekinesis is switched off. */
    public static final String UIT = "guhs_mewtwo_telekinese_uit";
    /** Persistent key on an item that is floating (for the sound and the advancement only once). */
    private static final String ZWEEFT_ITEM = "guhs_mewtwo_zweeft";

    private static final Map<GuhEntity, int[]> LUCHT = java.util.Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<GuhEntity, Long> TWEEDE_HAP = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    @Override
    public void tick(GuhEntity guh) {
        if (guh.level().isClientSide) {
            // purple sparkles drifting from under the floating guh
            if (GuhHooks.heeft(guh, VerhaalVlaggen.ZWEEFT) && guh.getRandom().nextInt(guh.isVehicle() ? 12 : 6) == 0 && !guh.isInvisible()) {
                double r = guh.getBbWidth() * 0.6;
                guh.level().addParticle(MewtwoFeature.GLOED.get(), guh.getX() + (guh.getRandom().nextDouble() - 0.5) * 2 * r, guh.getY() + 0.15,
                        guh.getZ() + (guh.getRandom().nextDouble() - 0.5) * 2 * r, 0, 0.015, 0);
            }
            return;
        }
        if ((guh.tickCount + guh.getId()) % 20 == 0 && !GuhHooks.heeft(guh, VerhaalVlaggen.ZWEEFT)) {
            GuhHooks.zet(guh, VerhaalVlaggen.ZWEEFT, true);
        }
        Long hap = TWEEDE_HAP.get(guh);
        if (hap != null && guh.level().getGameTime() >= hap) {
            TWEEDE_HAP.remove(guh);
            hap(guh, 1.45f);
        }
        if ((guh.tickCount + guh.getId()) % 2 == 0 && magTelekinese(guh)) {
            telekinese(guh);
        }
    }

    /** A tamed Guhtwo (not the story copy) with its telekinesis on, not inside a huisje. */
    public static boolean magTelekinese(GuhEntity guh) {
        return guh.isTame() && guh.getOwnerUUID() != null && !VerhaalGuhs.isKopie(guh) && !guh.getPersistentData().getBoolean(UIT)
                && !Huisjes.isBinnen(guh) && guh.isAlive();
    }

    /**
     * One telekinesis step: every item within {@link #TELEKINESE} blocks (not freshly dropped, not a loaned item) floats to
     * the Guhtwo, with a purple trail; when its owner is within {@link #NAAR_BAAS} blocks it floats on to the owner (who
     * picks it up). Returns how many items it moves.
     */
    public static int telekinese(GuhEntity guh) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return 0;
        }
        LivingEntity baas = guh.getOwner();
        boolean naarBaas = baas instanceof Player p && !p.isSpectator() && baas.distanceTo(guh) <= NAAR_BAAS && baas.level() == guh.level();
        Vec3 bij = guh.position().add(0, guh.getBbHeight() * 0.8 + 0.4, 0);
        int n = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, guh.getBoundingBox().inflate(TELEKINESE), ItemEntity::isAlive)) {
            if (n >= 24 || item.distanceTo(guh) > TELEKINESE || item.hasPickUpDelay() || Features.isLoaned(item.getItem())) {
                continue;
            }
            Vec3 naar = naarBaas ? baas.position().add(0, 0.6, 0) : bij;
            Vec3 d = naar.subtract(item.position());
            double len = d.length();
            if (!naarBaas && len < 1.2) {
                // it hovers next to the Guhtwo, bobbing a little
                item.setDeltaMovement(d.scale(0.1).add(0, 0.04 + Math.sin((level.getGameTime() + item.getId()) * 0.2) * 0.01, 0));
            } else {
                double speed = Math.min(0.32, 0.12 + len * 0.03);
                item.setDeltaMovement(d.scale(speed / Math.max(len, 1e-3)).add(0, 0.045, 0));
            }
            item.hasImpulse = true;
            if (!item.getPersistentData().getBoolean(ZWEEFT_ITEM)) {
                item.getPersistentData().putBoolean(ZWEEFT_ITEM, true);
                level.playSound(null, item.getX(), item.getY(), item.getZ(), MewtwoFeature.TELEKINESE.get(), SoundSource.NEUTRAL, 0.6f,
                        1.4f + level.random.nextFloat() * 0.3f);
                if (baas instanceof ServerPlayer sp && item.getItem().is(ModItems.KAAS_KNABBELS.get())) {
                    MewtwoVerhaal.adv(sp, "mewtwo_telekinese");
                }
            }
            if ((level.getGameTime() + item.getId()) % 5 == 0) {
                level.sendParticles(MewtwoFeature.GLOED.get(), item.getX(), item.getY() + 0.25, item.getZ(), 1, 0.08, 0.08, 0.08, 0.0);
            }
            n++;
        }
        return n;
    }

    @Override
    public boolean travel(GuhEntity guh, Vec3 input) {
        if (!(guh.getControllingPassenger() instanceof Player)) {
            LUCHT.remove(guh);
            return false;
        }
        int[] t = LUCHT.computeIfAbsent(guh, g -> new int[1]);
        if (guh.onGround() || guh.isInWater() || guh.onClimbable()) {
            t[0] = 0;
            return false;
        }
        t[0]++;
        Vec3 v = guh.getDeltaMovement();
        if (v.y < 0 && t[0] <= ZWEEF_TICKS) {
            guh.setDeltaMovement(v.x, 0.0, v.z);            // floating over the gap
            guh.fallDistance = 0;
        } else if (v.y < -0.12 && t[0] <= ZWEEF_TICKS + 30) {
            guh.setDeltaMovement(v.x, -0.12, v.z);          // then it sinks down gently
            guh.fallDistance = 0;
        }
        return false;
    }

    /** (tests) the ticks this guh has been floating in the air while ridden. */
    public static int luchtTicks(GuhEntity guh) {
        int[] t = LUCHT.get(guh);
        return t == null ? 0 : t[0];
    }

    @Override
    public int voerFactor(GuhEntity guh) {
        return 2;
    }

    @Override
    public void gegeten(GuhEntity guh, @Nullable ServerPlayer p, ItemStack snack) {
        if (!(guh.level() instanceof ServerLevel level)) {
            return;
        }
        hap(guh, 1.25f);
        TWEEDE_HAP.put(guh, level.getGameTime() + 7);
        level.sendParticles(MewtwoFeature.X2.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.9, guh.getZ(), 1, 0, 0, 0, 0);
        PacketDistributor.sendToPlayersTrackingEntity(guh, new MewtwoPayloads.X2(guh.getId()));
        if (p != null && guh.isOwnedBy(p)) {
            MewtwoVerhaal.adv(p, "mewtwo_x2");
        }
    }

    /** One bite of the double chomp: a munch and a puff of hearts. */
    private static void hap(GuhEntity guh, float pitch) {
        if (guh.level() instanceof ServerLevel level) {
            level.playSound(null, guh, MewtwoFeature.X2_SMUL.get(), SoundSource.NEUTRAL, 0.9f, pitch);
            level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.4, guh.getZ(), 5, 0.35, 0.2, 0.35, 0.02);
        }
    }

    @Override
    public String speciaalKnop() {
        return "gui.guhs.mewtwo.telekinese";
    }

    @Override
    public void speciaal(GuhEntity guh, ServerPlayer owner) {
        boolean uit = !guh.getPersistentData().getBoolean(UIT);
        guh.getPersistentData().putBoolean(UIT, uit);
        owner.displayClientMessage(Component.translatable(uit ? "gui.guhs.mewtwo.telekinese.uit" : "gui.guhs.mewtwo.telekinese.aan",
                guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
        if (guh.level() instanceof ServerLevel level) {
            level.playSound(null, guh, MewtwoFeature.TELEKINESE.get(), SoundSource.NEUTRAL, 0.8f, uit ? 0.8f : 1.5f);
            level.sendParticles(MewtwoFeature.GLOED.get(), guh.getX(), guh.getY() + 0.8, guh.getZ(), uit ? 4 : 16, 0.5, 0.4, 0.5, 0.04);
        }
    }
}
