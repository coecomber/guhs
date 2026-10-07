package nl.juiced.guhs.feature.ringh4;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-h4): the Spiegel van Guhladriel (block {@code guhs:ringh4_spiegel}: a silver bowl of still water on a
 * pedestal, in the green dell of the tree city) and the cutscene of the chapter (DESIGN_130 4: "look in the mirror").
 * <ul>
 *   <li>A click by a player at step 4 of the chapter plays the scene {@link #SCENE} ({@code ringh4_spiegel}), anchored on
 *       the mirror itself and turned like the copy of the city; when it is over the player is at step 5 (the step moves in
 *       the scene's {@code daarna}: whoever logs out half-way simply sees it again).</li>
 *   <li>Before that the bowl is dark; afterwards a click shows the scene again (it is in the Guhdex too).</li>
 * </ul>
 * The scene (about 55 seconds; positions are template blocks relative to the mirror, before the copy's turn): the dell at
 * night, Guhladriel fills the bowl, the visions (the Knabbelgouw eaten bare by Mika's, then the Eye of Sausron, who only
 * "had a bit of trek"), the ring is offered, Guhladriel's great temptation ("niet duister, maar ROND en VADS als de
 * dageraad!") and how she passes the test and stays small. Texts: tools/features/ring_h4_tekst.py.
 */
public final class Spiegel {
    public static final String SCENE_ID = "ringh4_spiegel";
    static final String Q = "quest.guhs.ringh4.spiegel.";
    private static final String GU = "guhladriel", SAM = "sam";
    /** Where everybody stands (the mirror block is 0,0,0; the island and the dell's floor are one layer lower). */
    private static final Vec3 BIJ_SPIEGEL = new Vec3(1.5, -1, 0.5), GUHLADRIEL = new Vec3(0.5, -1, -0.5), OP_SPIEGEL = new Vec3(0.5, 0.9, 0.5);

    public static final Cutscene SCENE = Cutscene.maak(SCENE_ID).duur(1100).bij("ring_h4").kaart(RingH4Feature.KAART).verbergEcht(14)
            .speler(new Vec3(5.5, -1, 0.5), 90)
            .npc(GU, GuhNpcEntity.Kind.GUHLADRIEL, GUHLADRIEL, 0)
            .guh(SAM, GuhVariant.SAM_GUH, new Vec3(7.5, 0, 2.5), 90)
            // 1. the dell at night: a wide look from the rim, slowly down to the bowl
            .camera(0, new Vec3(7.5, 3.6, 6.5), new Vec3(0.5, 0.0, 0.5))
            .camera(150, new Vec3(4.6, 1.0, 3.6), new Vec3(0.5, 0.2, 0.2))
            .camera(250, new Vec3(3.6, 0.6, 2.9), new Vec3(0.5, 0.3, 0.1))
            .zeg(10, "", "nacht", 70)
            .loop(Cutscene.SPELER, 30, 100, BIJ_SPIEGEL)
            .loop(SAM, 50, 120, new Vec3(4.5, -1, 1.5))
            .kijk(GU, 0, new Vec3(0.5, 0, 0.5))
            .kijk(GU, 100, new Vec3(1.5, 0.4, 0.5))
            .kijk(Cutscene.SPELER, 102, new Vec3(0.5, 0.2, -0.5))
            .zeg(90, GU, "kijk", 70)
            .animatie(GU, 92, "praat")
            .zeg(170, SAM, "snoet", 60)
            .animatie(SAM, 172, "kijk")
            // 2. she fills the bowl
            .animatie(GU, 236, "toover")
            .geluid(250, () -> SoundEvents.BOTTLE_EMPTY, 1.0f, 0.8f)
            .deeltjes(252, ParticleTypes.SPLASH, OP_SPIEGEL, 14, 0.25)
            .zeg(244, GU, "dingen", 96)
            .kijk(Cutscene.SPELER, 300, new Vec3(0.5, -0.4, 0.5))
            .animatie(Cutscene.SPELER, 320, "buk")
            // 3. the visions: straight down into the bowl
            .cameraKnip(346, new Vec3(0.5, 3.4, 1.3), new Vec3(0.5, 0.6, 0.5))
            .camera(520, new Vec3(0.5, 2.2, 1.0), new Vec3(0.5, 0.6, 0.5))
            .geluid(348, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.0f, 0.6f)
            .deeltjes(350, ParticleTypes.END_ROD, OP_SPIEGEL, 16, 0.3)
            .zeg(352, "", "gouw", 80)
            .deeltjes(400, ParticleTypes.ENCHANT, OP_SPIEGEL, 30, 0.4)
            .zeg(436, "", "kruimels", 70)
            .deeltjes(470, ParticleTypes.END_ROD, OP_SPIEGEL, 10, 0.3)
            // 4. the Eye
            .geluid(512, () -> SoundEvents.ELDER_GUARDIAN_CURSE, 0.7f, 0.7f)
            .schud(514, 0.9f, 50)
            .deeltjes(514, ParticleTypes.FLAME, OP_SPIEGEL, 40, 0.35)
            .deeltjes(530, ParticleTypes.LAVA, OP_SPIEGEL, 8, 0.2)
            .zeg(516, "", "oog", 66)
            .deeltjes(560, ParticleTypes.FLAME, OP_SPIEGEL, 30, 0.35)
            .zeg(586, "", "trek", 74)
            // 5. back in the dell: the ring is offered
            .cameraKnip(664, new Vec3(4.2, 0.4, -2.6), new Vec3(1.0, 0.1, 0.1))
            .animatie(Cutscene.SPELER, 664, "sta")
            .animatie(Cutscene.SPELER, 668, "spring")
            .animatie(SAM, 666, "schrik")
            .kijk(Cutscene.SPELER, 668, new Vec3(0.5, 0.2, -0.5))
            .zeg(672, GU, "weet", 60)
            .animatie(GU, 674, "knik")
            .zeg(736, Cutscene.SPELER, "aanbod", 56)
            .animatie(Cutscene.SPELER, 738, "zwaai")
            // 6. the temptation: low, close, she towers
            .cameraKnip(794, new Vec3(1.7, -0.7, 1.9), new Vec3(0.5, 0.5, -0.5))
            .camera(940, new Vec3(1.35, -0.75, 1.45), new Vec3(0.5, 0.7, -0.5))
            .animatie(GU, 796, "juich")
            .geluid(796, () -> SoundEvents.BEACON_ACTIVATE, 0.9f, 0.6f)
            .schud(800, 0.7f, 130)
            .deeltjes(800, ParticleTypes.END_ROD, new Vec3(0.5, 0.6, -0.5), 40, 0.7)
            .deeltjes(840, ParticleTypes.END_ROD, new Vec3(0.5, 0.6, -0.5), 40, 0.9)
            .deeltjes(890, ParticleTypes.END_ROD, new Vec3(0.5, 0.6, -0.5), 50, 1.1)
            .zeg(798, GU, "koningin", 78)
            .zeg(880, GU, "houden", 60)
            // 7. she passes the test and stays small
            .cameraKnip(946, new Vec3(3.4, 0.5, 2.4), new Vec3(0.8, 0.0, 0.0))
            .animatie(GU, 946, "schaam")
            .geluid(946, () -> SoundEvents.BEACON_DEACTIVATE, 0.7f, 1.2f)
            .zeg(952, GU, "test", 80)
            .animatie(GU, 1000, "buig")
            .zeg(1038, SAM, "bed", 46)
            .animatie(SAM, 1040, "slaap")
            .zwart(1084, 1100)
            .registreer();

    /** A player clicked the mirror at this spot. */
    static void kijk(ServerPlayer p, BlockPos pos) {
        Verhaallijn lijn = RingH4Feature.LIJN;
        if (Cutscenes.bezig(p)) {
            return;
        }
        Boomstad.Kopie kopie = Boomstad.bij(p.level(), pos);
        Rotation draai = kopie == null ? Rotation.NONE : kopie.draai();
        if (Ring.aanZet(p, lijn, 4)) {
            Cutscenes.speel(p, SCENE, pos, draai, s -> {
                if (lijn.verder(s, 4)) {
                    Ring.behaald(s, "ring_h4_spiegel");
                    GuhQuests.hint(s, "quest.guhs.ringh4.hint.gaven");
                }
            });
        } else if (lijn.aanDeBeurt(p) && lijn.stap(p) > 4) {
            // (PHASE3 R07) the mirror showed this player what it had to show: a second click does not lock them into the whole
            // scene again (55 s, not skippable). Who wants to see it again does so in the Guhdex ("Opnieuw bekijken"), where it
            // is their own choice and can be closed.
            p.sendOverlayMessage(Component.translatable(Q + "kalm").withStyle(ChatFormatting.AQUA));
        } else {
            p.sendOverlayMessage(Component.translatable(Q + (lijn.aanDeBeurt(p) ? "donker" : "eigen_snoet")).withStyle(ChatFormatting.AQUA));
        }
    }

    /** The bowl on its foot. It glows a little; it is part of a protected building and has no drops. */
    public static class Blok extends Block {
        private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 11, 15);

        public Blok(Properties properties) {
            super(properties);
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                kijk(p, pos);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            if (random.nextInt(5) == 0) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.75, pos.getZ() + 0.25 + random.nextDouble() * 0.5,
                        0, 0.01 + random.nextDouble() * 0.02, 0);
            }
            if (random.nextInt(60) == 0) {
                level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.35f,
                        0.6f + random.nextFloat() * 0.5f, false);
            }
        }
    }

    private Spiegel() {
    }
}
