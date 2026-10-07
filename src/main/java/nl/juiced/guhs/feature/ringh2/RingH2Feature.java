package nl.juiced.guhs.feature.ringh2;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * bbq2 (ring-h2): chapter 2 of the Knabbelring, "De Raad van Guhrond" (DESIGN_130 4). Resources: tools/features/ring_h2.py
 * (+ ring_h2_bouw.py: the template of Guhvendel).
 * <ul>
 *   <li>The structure guhs:guhvendel: an elf house with three kaassaus waterfalls in a rock cirque of the Worstenwoud,
 *       exactly once per world (the first halte of the story chain in the Barbecuether), behind Guhdalfs sluier until the
 *       player finished chapter 1, protected for everybody, in the Superkompas once it is open.</li>
 *   <li>The questline {@link #LIJN} (six steps, per player): the walk there, Guhrond's welcome, meeting the fellowship,
 *       the council bell, volunteering, the farewell: {@link Guhvendel}; who says what: {@link GuhvendelRol}.</li>
 *   <li>The narrator card "ring_h2" at the start of the chapter and the two cutscenes {@link RingH2Scenes}.</li>
 *   <li>The cast only exists in its own scenes: every character of the template is marked with the steps it is there for
 *       (ring-kern's Zicht), and after the chapter only Guhrond lives here.</li>
 * </ul>
 * No blocks, items, entities or payloads of its own: the characters, the ring and the Rustvuurtje are ring-kern's.
 */
public final class RingH2Feature {
    /** How many blocks of smoke and protection lie around the template's box. */
    public static final int RAND = 6;

    /**
     * The questline of this chapter (the field ring-kern refers to: {@code Ring.lijn(2)}). Steps: {@link Guhvendel#REIS} ..
     * {@link Guhvendel#VERTREK}. Texts: tools/features/ring_h2.py.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_h2", Ring.GROEP).stappen(Guhvendel.STAPPEN).na("ring_h1").icoon("minecraft:bell")
            .nodig((p, stap) -> stap == Guhvendel.KENNIS
                    ? List.of(Verhaallijn.nodig("guhs:kaas_knabbels", "gui.guhs.verhalen.ring_h2.nodig.kennis", Guhvendel.ontmoet(p), Guhvendel.GEZELSCHAP.size()))
                    : List.of())
            .beloningen(p -> List.of(
                    Verhaallijn.beloning("guhs:ring_stoofpotje", "gui.guhs.verhalen.ring_h2.beloning.proviand", Guhvendel.lijn().stap(p) > Guhvendel.MELDEN),
                    Verhaallijn.beloning("guhs:knabbelring", "gui.guhs.verhalen.ring_h2.beloning.genootschap", Guhvendel.lijn().klaar(p))))
            .doel((p, stap) -> Ring.doel(2))
            .registreer();

    public static void register(IEventBus modBus) {
        // hidden and protected until the player's story gets here, then open for ever (also in the Superkompas)
        Ring.sluier(Guhvendel.STRUCTUUR, RAND, 2);
        SuperkompasItem.voegToe("barbecue", Guhvendel.STRUCTUUR);
        // the story
        Verteller.registreer(Guhvendel.KAART, 4, LIJN.id());
        RingH2Scenes.registreer();
        for (GuhNpcEntity.Kind kind : new GuhNpcEntity.Kind[]{GuhNpcEntity.Kind.GUHROND, GuhNpcEntity.Kind.GUHDALF, GuhNpcEntity.Kind.ARAGUH,
                GuhNpcEntity.Kind.LEGUHLAS, GuhNpcEntity.Kind.GIMGUH, GuhNpcEntity.Kind.BOROMIKA, GuhNpcEntity.Kind.MERRIE, GuhNpcEntity.Kind.PIPPGUH}) {
            NpcRollen.zet(kind, Guhvendel.PLEK, new GuhvendelRol(kind));
        }
        Guhvendel.registreer();
        NeoForge.EVENT_BUS.addListener(Guhvendel::opTick);
        NeoForge.EVENT_BUS.addListener(Guhvendel::opKlik);
        NeoForge.EVENT_BUS.addListener(RingH2Commands::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private RingH2Feature() {
    }
}
