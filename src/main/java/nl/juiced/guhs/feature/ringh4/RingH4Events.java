package nl.juiced.guhs.feature.ringh4;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;

/**
 * bbq2 (ring-h4): what the tree city does by itself for a player whose own story is in chapter 4 (registered by
 * {@link RingH4Feature}).
 * <ul>
 *   <li>Once a second, for a player in the Guhbarbecuether whose chapter 4 is open and not done: the copy around them is
 *       looked up, and the steps that are a place react: step 0 at the gate (the narrator card, then "talk to Leguhlas"),
 *       step 3 at the Rustvuurtje of the guest flet (a night's rest), step 6 at the landing (whoever got there without the
 *       boat is done too: the boat is the way, never a lock).</li>
 *   <li>Guhladriel's blessing: nobody takes fall damage in the tree city (stairs around trunks, rope bridges, flets 25
 *       blocks up: DESIGN_130 0, nothing new hurts a player).</li>
 *   <li>Nobody gets out of a sailing elf boat; whoever logs out in one stands on the jetty again.</li>
 * </ul>
 */
public final class RingH4Events {
    static final String Q = "quest.guhs.ringh4.";
    /** How near a "place" step counts as reached (blocks). */
    public static final double POORT_BEREIK = 9, VUUR_BEREIK = 4.5, AANLEG_BEREIK = 7;
    /** (not saved) the last time a player's copy was looked up: once every 2 seconds is enough. */
    private static final Map<UUID, Long> GEKEKEN = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % 20 == 0) {
            tik(p, false);
        }
    }

    /** The once-a-second upkeep (public: game tests call it, mock players are not ticked). {@code nu}: skip the 2-second rest. */
    public static void tik(ServerPlayer p, boolean nu) {
        Verhaallijn lijn = RingH4Feature.LIJN;
        if (p.isSpectator() || !lijn.aanDeBeurt(p) || lijn.klaar(p) || !(Ring.OVERAL || p.level().dimension() == BarbecuetherFeature.BARBECUETHER)) {
            return;
        }
        int stap = lijn.stap(p);
        if (stap != 0 && stap != 3 && stap != 6) {
            return;
        }
        long tijd = p.level().getGameTime();
        Long vorige = GEKEKEN.get(p.getUUID());
        if (!nu && vorige != null && tijd - vorige < 40 && tijd >= vorige) {
            return;
        }
        GEKEKEN.put(p.getUUID(), tijd);
        Boomstad.Kopie kopie = Boomstad.bij(p.level(), p.blockPosition());
        if (kopie == null) {
            return;
        }
        Vec3 hier = p.position();
        switch (stap) {
            case 0 -> {
                if (hier.distanceTo(kopie.punt("poort")) <= POORT_BEREIK || hier.distanceTo(kopie.punt("leguhlas_poort")) <= POORT_BEREIK) {
                    aankomst(p);
                }
            }
            case 3 -> {
                if (hier.distanceTo(kopie.punt("gast_vuur")) <= VUUR_BEREIK && Duwtje.mag(p)) {
                    rust(p);
                }
            }
            default -> {
                if (!p.isPassenger() && hier.distanceTo(kopie.punt("aanleg")) <= AANLEG_BEREIK && Duwtje.mag(p)) {
                    klaar(p, false);
                }
            }
        }
    }

    /** Step 0 -> 1: the player walks up to the gate of the tree city: the narrator card of the chapter, then Leguhlas. */
    static boolean aankomst(ServerPlayer p) {
        Verhaallijn lijn = RingH4Feature.LIJN;
        if (!Ring.aanZet(p, lijn, 0) || Cutscenes.bezig(p)) {
            return false;
        }
        lijn.begin(p);
        return Verteller.toon(p, RingH4Feature.KAART, s -> {
            if (lijn.verder(s, 0)) {
                s.sendSystemMessage(Component.translatable(Q + "aankomst").withStyle(ChatFormatting.GOLD));
            }
        });
    }

    /** Step 3 -> 4: a night on the guest flet. Guhladriel walks to the dell with the mirror. */
    static void rust(ServerPlayer p) {
        Verhaallijn lijn = RingH4Feature.LIJN;
        if (!lijn.verder(p, 3)) {
            return;
        }
        ServerLevel level = p.level();
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 70, 0, false, false));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1, false, false));
        p.getFoodData().eat(6, 0.6f);
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 0.6f);
        level.sendParticles(p, ParticleTypes.END_ROD, false, false, p.getX(), p.getY() + 1.2, p.getZ(), 14, 0.8, 0.6, 0.8, 0.01);
        p.sendSystemMessage(Component.translatable(Q + "rust.0").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        p.sendSystemMessage(Component.translatable(Q + "rust.1").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        p.sendSystemMessage(Component.translatable(Q + "rust.2").withStyle(ChatFormatting.GOLD));
        Ring.behaald(p, "ring_h4_gerust");
    }

    /** Step 6 -> done: on the far bank. {@code metBoot}: Leguhlas says goodbye from the boat; else Sam-guh shakes himself dry. */
    static void klaar(ServerPlayer p, boolean metBoot) {
        Verhaallijn lijn = RingH4Feature.LIJN;
        if (!lijn.verder(p, 6)) {
            return;
        }
        ServerLevel level = p.level();
        if (metBoot) {
            Vaart.zeg(p, 0, Vaart.T + "afscheid.0");
            Vaart.zeg(p, 1, Vaart.T + "afscheid.1");
        } else {
            Vaart.zeg(p, 2, Vaart.T + "gezwommen");
        }
        p.sendSystemMessage(Component.translatable(Q + "klaar").withStyle(ChatFormatting.GOLD));
        Ring.rustpunt(p, p.position(), p.getYRot());
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.2f);
        level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, p.getX(), p.getY() + 1.0, p.getZ(), 12, 0.6, 0.5, 0.6, 0);
        Ring.behaald(p, "ring_h4_klaar");
        Sam.kom(p);
    }

    /** Guhladriel's blessing: you land like a leaf. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && event.getDistance() > 3 && Boomstad.binnen(p.level(), p.blockPosition())) {
            event.setDamageMultiplier(0f);
            if (event.getDistance() > 6) {
                p.sendOverlayMessage(Component.translatable(Q + "zegen").withStyle(ChatFormatting.AQUA));
            }
        }
    }

    /** Nobody gets out of a sailing elf boat ("Blijf zitten, njeg"); before it leaves you may. */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof ElfenbootjeEntity boot && boot.vaart()
                && !boot.uitstappen && !boot.isRemoved() && event.getEntityMounting() instanceof ServerPlayer p && p.isAlive() && !p.isRemoved()
                && p.isShiftKeyDown()) {
            event.setCanceled(true);
            p.sendOverlayMessage(Component.translatable(Vaart.T + "blijf_zitten").withStyle(ChatFormatting.AQUA));
        }
    }

    /** Logged out in a boat: the trip ends for everybody in it, back on the jetty (a trip is never saved). */
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        GEKEKEN.remove(event.getEntity().getUUID());
        Boomstad.vergeet(event.getEntity().getUUID());
        if (event.getEntity().getVehicle() instanceof ElfenbootjeEntity boot && boot.soort() == ElfenbootjeEntity.RIT) {
            boot.eindig(false);
        }
    }

    private RingH4Events() {
    }
}
