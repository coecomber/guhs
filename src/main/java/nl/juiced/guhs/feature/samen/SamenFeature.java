package nl.juiced.guhs.feature.samen;

import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.Vriendjes;
import nl.juiced.guhs.feature.guhpolder.PinguhMeeglijden;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.race.RaceBaan;

/**
 * Samen spelen, samen knuffelen (2.10 "Lieve vadsjes van elkaar", slice samen). Your own guh is really WITH you:
 * <ul>
 *   <li>{@link SamenSpel}: at every minigame your guh stands by and cheers at a good throw or time, is lovingly sad at a
 *       miss ("ooh njeg...") and dances at a record; playing together gives hearts (MINIGAME, RECORD), and so does
 *       travelling together (REIZEN).</li>
 *   <li>{@link SamenMee}: where it can, it really joins: it rides along behind you in the race / circuit kart, skates
 *       next to you on the Elf-Guhjestocht and swims along in the Knuffelbad.</li>
 *   <li>{@link SamenReacties}: it waits at your respawn and comforts you, does a welcome-back dance after you were away a
 *       long time, cuddles against you in a thunderstorm and waves when you go to sleep. The bff-knuffel: a hug with a
 *       big heart above you both.</li>
 *   <li>{@link SamenVriendjes}: tamed guhs that spend time together become friends (and besties): they walk together,
 *       cuddle, sleep together in their huisje and play together on the wip and the schommel.</li>
 *   <li>{@link SamenBeloning}: what the hartjes levels unlock: a clothing piece and an emote per level (the zielsguh
 *       also gets the gouden hartjes-halsbandje), the emote lock of the three band emotes, and (client) the sparkling
 *       pink heart next to a zielsguh's name.</li>
 * </ul>
 * Resources (particles, sounds, clothes, texts, advancements, FTB): tools/features/samen.py.
 */
public final class SamenFeature {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** The sparkling pink heart of a zielsguh (next to its name, and now and then floating up). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ZIELSHARTJE = PARTICLES.register("samen_zielshartje",
            () -> new SimpleParticleType(false));
    /** The big heart above a guh and its player during the bff-knuffel. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BFF_HART = PARTICLES.register("samen_bff_hart",
            () -> new SimpleParticleType(true));

    public static final DeferredHolder<SoundEvent, SoundEvent> JUICH = geluid("samen.juich");
    public static final DeferredHolder<SoundEvent, SoundEvent> OOH = geluid("samen.ooh");
    public static final DeferredHolder<SoundEvent, SoundEvent> WELKOM = geluid("samen.welkom");
    public static final DeferredHolder<SoundEvent, SoundEvent> BFF = geluid("samen.bff");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id)));
    }

    public static void register(IEventBus modBus) {
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        // the clothes of the hartjes levels (source "band": "Hartjes met je guh")
        for (GuhClothes c : SamenBeloning.alleKleding()) {
            KledingBronnen.bron(c, "band");
        }
        // guh behaviour
        GuhHooks.doelen((guh, goals) -> {
            goals.addGoal(3, new SamenMee.ZwemGoal(guh));
            goals.addGoal(4, new SamenReacties.ReactieGoal(guh));
            goals.addGoal(4, new SamenVriendjes.VriendjesGoal(guh));
        });
        GuhHooks.tick(SamenFeature::tick);
        // the band bus
        Band.opMoment(SamenSpel::moment);
        Band.opMoment(SamenVriendjes::moment);
        Band.opMoment(SamenReacties::moment);
        Band.opNiveau(SamenBeloning::nieuwNiveau);
        Vriendjes.opNieuw(SamenVriendjes::nieuw);
        // joining in: the race / circuit kart, skating along on the Elf-Guhjestocht
        RaceBaan.luister(SamenMee.RACE);
        PinguhMeeglijden.ookMee(SamenMee::magMeeSchaatsen);
        // game events
        NeoForge.EVENT_BUS.register(SamenEvents.class);
    }

    /** Every tick of every guh (server): spread per guh inside. */
    private static void tick(GuhEntity guh) {
        if (!Band.isBandGuh(guh)) {
            SamenMee.opruimen(guh);
            return;
        }
        SamenMee.tick(guh);
        SamenReacties.tick(guh);
        SamenVriendjes.tick(guh);
    }

    public static void payloads(PayloadRegistrar registrar) {
        SamenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private SamenFeature() {
    }
}
