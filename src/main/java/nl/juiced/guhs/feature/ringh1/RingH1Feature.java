package nl.juiced.guhs.feature.ringh1;

import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.ring.Sam;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * bbq2 (ring-h1): chapter 1 of the Knabbelring, "Een langverwacht knabbelfeest" (DESIGN_130 4). Resources:
 * tools/features/ring_h1.py (+ ring_h1_bouw, ring_h1_tex, ring_h1_tekst, ring_h1_wiki).
 * <ul>
 *   <li>{@link Gouw}: the Knabbelgouw (structure guhs:knabbelgouw: the big barbecueput with the heuvelholletjes around it,
 *       as one whole, in new chunks of the Guhmensie) and Guhdalf's camp on the free strip around a big pit that was there
 *       already; who lives there; the nearest Guhdalf.</li>
 *   <li>{@link Feest}: the questline {@link #LIJN} (meet Guhdalf, the chores, the farewell party with the ring, Sam-guh
 *       joins, the provisions, the walk to the portal). Its last step is what opens the portal lock of ring-kern.</li>
 *   <li>The narrator card {@link #KAART} and the two scenes {@link #AANKOMST} and {@link #FEEST}: written against the camp
 *       (positions relative to the block Guhdalf sits on, turned with the camp), so they fit every camp.</li>
 *   <li>{@link RingH1Blocks}: the fireworks crate, the party table, the provisions, the chimney pot.</li>
 * </ul>
 */
public final class RingH1Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);

    private static BlockBehaviour.Properties prop(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(-1f, 3_600_000f).sound(SoundType.WOOD).noLootTable().noOcclusion()
                .pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<RingH1Blocks.Vuurwerkkist> VUURWERKKIST = BLOCKS.registerBlock("ringh1_vuurwerkkist", RingH1Blocks.Vuurwerkkist::new,
            () -> prop(MapColor.COLOR_RED));
    public static final DeferredBlock<RingH1Blocks.Feesttafel> FEESTTAFEL = BLOCKS.registerBlock("ringh1_feesttafel", RingH1Blocks.Feesttafel::new,
            () -> prop(MapColor.WOOD));
    public static final DeferredBlock<RingH1Blocks.Proviand> PROVIAND = BLOCKS.registerBlock("ringh1_proviand", RingH1Blocks.Proviand::new,
            () -> prop(MapColor.PODZOL));
    public static final DeferredBlock<RingH1Blocks.Schoorsteentje> SCHOORSTEENTJE = BLOCKS.registerBlock("ringh1_schoorsteentje",
            RingH1Blocks.Schoorsteentje::new, () -> prop(MapColor.TERRACOTTA_ORANGE).sound(SoundType.STONE));

    /** The narrator card of the chapter and the ids of its two scenes. */
    public static final String KAART = "ring_h1", AANKOMST_ID = "ringh1_aankomst", FEEST_ID = "ringh1_feest";

    /**
     * The questline of chapter 1 (six steps, see {@link Feest}). Texts: tools/features/ring_h1_tekst.py. ring-kern refers to
     * this field ({@code Ring.lijn(1)}): its end opens the grill portal for the player.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("ring_h1", "knabbelring").stappen(6).icoon("minecraft:firework_rocket")
            .nodig(Feest::nodig).beloningen(Feest::beloningen).doel(Feest::doel).registreer();

    // --- the scenes: positions relative to Guhdalf's block (camp-local (10, 1, 6): he looks south, the party table is at
    // (-3, 0, 1), the open side of the camp is x -6..1, z 2..4), turned with the camp ------------------------------------
    private static final Vec3 GUHDALF = new Vec3(0.5, 0, 0.5), SPELER = new Vec3(0.5, 0, 3.5), MIDDEN = new Vec3(-1.5, 1.0, 2.0);

    /** Guhdalf has arrived: what is going on, and why there is a party first. */
    public static final Cutscene AANKOMST = Cutscene.maak(AANKOMST_ID).duur(600).bij("ring_h1").kaart(KAART).verbergEcht(14).cameraOntwijkt()
            .speler(SPELER, 180)
            .npc("guhdalf", GuhNpcEntity.Kind.GUHDALF, GUHDALF, 0)
            .guh("sam", GuhVariant.SAM_GUH, new Vec3(-5.5, 0, 3.5), 270)
            .camera(0, new Vec3(-2.0, 9.0, 12.0), MIDDEN)
            .camera(75, new Vec3(-1.5, 3.4, 8.5), new Vec3(-0.5, 1.0, 1.5))
            .zeg(10, "", "verteller", 62)
            .loop("sam", 60, 92, new Vec3(-1.5, 0, 2.5))
            .kijk("sam", 92, new Vec3(0.5, 1, 0.5))
            .cameraKnipVolgt(80, new Vec3(1.8, 1.7, 5.4), "sam")
            .zeg(84, "sam", "sam_laat", 58)
            .animatie("sam", 96, "spring")
            .cameraKnip(146, new Vec3(-2.6, 1.7, 3.6), new Vec3(0.5, 1.0, 0.5))
            .kijk("guhdalf", 146, new Vec3(-1.5, 1, 2.5))
            .zeg(150, "guhdalf", "nooit_te_laat", 82)
            .animatie("guhdalf", 150, "praat")
            .animatie("sam", 226, "lach")
            .cameraKnip(236, new Vec3(3.2, 2.1, -0.4), new Vec3(0.5, 1.0, 3.5))
            .kijk("guhdalf", 236, new Vec3(0.5, 1, 3.5))
            .zeg(238, "guhdalf", "portaal", 60)
            .animatie("guhdalf", 238, "wijs")
            .cameraKnip(300, new Vec3(-1.4, 1.6, 2.9), new Vec3(0.5, 1.1, 0.5))
            .zeg(302, "guhdalf", "ring", 82)
            .animatie("guhdalf", 302, "praat")
            .deeltjes(330, ParticleTypes.WAX_ON, new Vec3(0.5, 1.3, 1.0), 8, 0.2)
            .zeg(386, "guhdalf", "ook_ik", 36)
            .animatie("guhdalf", 386, "schaam")
            .zeg(426, "guhdalf", "berg", 70)
            .animatie("guhdalf", 426, "wijs")
            .cameraKnip(500, new Vec3(-2.0, 3.2, 8.0), new Vec3(-1.0, 1.0, 1.5))
            .zeg(502, "guhdalf", "feest", 84)
            .animatie("guhdalf", 502, "toover")
            .animatie("sam", 520, "juich")
            .deeltjes(545, ParticleTypes.FIREWORK, new Vec3(-1.5, 6.0, 2.0), 40, 0.5)
            .geluid(545, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 1.0f)
            .zwart(586, 600)
            .registreer();

    /** The farewell party: the cake, the speech, the ring, Sam-guh who is coming along whether you like it or not. */
    public static final Cutscene FEEST = Cutscene.maak(FEEST_ID).duur(820).bij("ring_h1").kaart(KAART).verbergEcht(14).cameraOntwijkt()
            .speler(new Vec3(-1.5, 0, 3.5), 180)
            .npc("guhdalf", GuhNpcEntity.Kind.GUHDALF, GUHDALF, 0)
            .guh("sam", GuhVariant.SAM_GUH, new Vec3(-4.5, 0, 2.5), 250)
            .guh("gast", GuhVariant.CHOCO, new Vec3(-5.5, 0, 4.5), 220)
            .guh("gast2", GuhVariant.MINT, new Vec3(-3.5, 0, 4.5), 180)
            .guh("gast3", GuhVariant.SNOW, new Vec3(1.5, 0, 3.5), 150)
            .guh("gast4", GuhVariant.NORMAL, new Vec3(0.5, 0, 4.5), 170)
            .zwart(0, 14)
            .camera(0, new Vec3(-2.0, 6.5, 10.5), MIDDEN)
            .camera(84, new Vec3(-6.5, 2.4, 6.5), MIDDEN)
            .zeg(10, "", "verteller", 70)
            .animatie("gast", 20, "spring").animatie("gast2", 34, "spring").animatie("gast3", 48, "spring").animatie("gast4", 62, "spring")
            .deeltjes(30, ParticleTypes.FIREWORK, new Vec3(-2.0, 6.0, 0.0), 30, 0.5)
            .geluid(30, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 0.7f, 1.1f)
            .deeltjes(66, ParticleTypes.FIREWORK, new Vec3(1.0, 6.5, 2.0), 30, 0.5)
            .geluid(66, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.7f, 1.0f)
            .cameraKnipVolgt(90, new Vec3(-2.8, 1.5, 4.4), "sam")
            .zeg(94, "sam", "sam_taart", 76)
            .animatie("sam", 94, "eet")
            .cameraKnip(176, new Vec3(-2.2, 1.8, 3.6), new Vec3(0.5, 1.1, 0.5))
            .zeg(180, "guhdalf", "speech1", 80)
            .animatie("guhdalf", 180, "praat")
            .zeg(266, "guhdalf", "speech2", 70)
            .animatie("guhdalf", 266, "buig")
            .cameraKnip(340, new Vec3(-2.0, 3.0, 8.0), new Vec3(-1.5, 1.0, 2.5))
            .zeg(342, "gast", "vahoeg", 40)
            .animatie("gast", 342, "juich").animatie("gast2", 346, "juich").animatie("gast3", 350, "juich").animatie("gast4", 354, "juich")
            .animatie("sam", 344, "juich")
            .deeltjes(350, ParticleTypes.HEART, new Vec3(-2.0, 1.6, 4.0), 10, 1.6)
            .loop(Cutscene.SPELER, 386, 414, new Vec3(-0.5, 0, 2.5))
            .kijk("guhdalf", 386, new Vec3(-0.5, 1, 2.5))
            .cameraKnip(392, new Vec3(2.6, 1.6, 3.4), new Vec3(0.0, 1.0, 1.5))
            .zeg(400, "guhdalf", "apart", 82)
            .animatie("guhdalf", 400, "wijs")
            .deeltjes(440, ParticleTypes.WAX_ON, new Vec3(0.0, 1.2, 1.0), 14, 0.2)
            .geluid(440, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f, 0.8f)
            .zeg(486, "guhdalf", "geheim", 80)
            .animatie("guhdalf", 486, "praat")
            .animatie(Cutscene.SPELER, 500, "buk")
            .animatie(Cutscene.SPELER, 520, "sta")
            .zeg(570, "guhdalf", "omdoen", 72)
            .animatie("guhdalf", 570, "schud")
            .schud(590, 0.4f, 12)
            .loop("sam", 640, 664, new Vec3(-1.5, 0, 2.5))
            .cameraKnipVolgt(646, new Vec3(-2.6, 1.5, 4.6), "sam")
            .zeg(650, "sam", "sam_mee", 82)
            .animatie("sam", 668, "juich")
            .cameraKnip(736, new Vec3(-2.0, 2.0, 9.5), new Vec3(-1.5, 5.0, 1.0))
            .zeg(738, "guhdalf", "vuurwerk", 62)
            .animatie("guhdalf", 738, "toover")
            .deeltjes(762, ParticleTypes.FIREWORK, new Vec3(-3.0, 8.0, 0.0), 60, 0.8)
            .geluid(762, () -> SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.0f)
            .deeltjes(776, ParticleTypes.FIREWORK, new Vec3(0.5, 9.0, 1.5), 60, 0.8)
            .geluid(778, () -> SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.0f, 1.0f)
            .deeltjes(790, ParticleTypes.FIREWORK, new Vec3(-1.5, 7.0, 3.0), 60, 0.8)
            .geluid(790, () -> SoundEvents.FIREWORK_ROCKET_BLAST, 1.0f, 0.9f)
            .schud(762, 0.3f, 8)
            .zwart(804, 820)
            .registreer();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        Verteller.registreer(KAART, 4, LIJN.id());
        NpcRollen.zet(GuhNpcEntity.Kind.GUHDALF, Gouw.ROL, new Feest.GuhdalfRol());
        Sam.BIJ_KLIK.add(Feest::klikSam);
        GuhHooks.klik(Feest::klikBewoner);
        GuhHooks.tick(Gouw::bewonerTick);
        Gouw.registreer();
        SuperkompasItem.voegToe("barbecue", Gouw.STRUCTUUR);
        NeoForge.EVENT_BUS.register(RingH1Events.class);
        NeoForge.EVENT_BUS.addListener(RingH1Events::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    private RingH1Feature() {
    }
}
