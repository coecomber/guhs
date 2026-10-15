package nl.juiced.guhs.feature.einde2.trap5;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.techbron.BronBlock;
import nl.juiced.guhs.feature.techmachine.TechmachineFeature;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.TestbronBlock;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The Werktuigguh's workshop in the Zuidelijke Luchttempel (CONTRACT_E2_B 7.12): the door, its scene and the six practice
 * setups.
 * <ul>
 *   <li><b>The door</b> ({@code guhs:werkplaatsdeur}, bouw_lucht's) stays a solid block in the world; for a player who
 *       finished the Guhvatar story it is walk-through ({@link E2Trap5#deurOpen}). The client knows that from
 *       {@link T5Payloads.Open}, sent whenever the answer changes.</li>
 *   <li><b>The scene</b> {@value #SCENE} (18 s): the first time such a player comes near the door, it shimmers, the
 *       Werktuigguh comes out to meet the Guhvatar, and the questline {@value E2Trap5#LIJN} begins.</li>
 *   <li><b>The practice setups</b> {@code oefen_1..6}: one working example of every project, on test sources under the bay
 *       floors, put down once per server start as soon as somebody who may enter is near ({@link #richtIn}). They are
 *       demonstrations: no owner, no projects, a click explains them.</li>
 * </ul>
 */
public final class Werkplaats {
    public static final String SCENE = "einde2_trap5_machines_werkplaats";
    public static final String S = Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL;

    /** (not saved) what each player's client was told last about the door. */
    private static final Map<UUID, Boolean> VERTELD = new ConcurrentHashMap<>();
    /** (not saved) the levels whose workshop got its practice setups since the server started. */
    private static final Set<ServerLevel> INGERICHT = ConcurrentHashMap.newKeySet();

    private Werkplaats() {
    }

    // =====================================================================================================================
    // the scene
    // =====================================================================================================================

    /**
     * The door opens (18 s, CONTRACT_E2_B 5: 15-25). Anchored at the lower middle block of the door ({@code werkplaatsdeur});
     * the workshop lies east of the door (+x), the temple's hall west of it. Positions relative to the anchor.
     */
    static Cutscene scene() {
        Vec3 buiten = new Vec3(-3.5, 0, 0.5), deur = new Vec3(0.5, 0, 0.5);
        return Cutscene.maak(SCENE).duur(380).bij(E2Trap5.LIJN).cameraOntwijkt()
                .speler(buiten.add(-1.5, 0, 0), -90f)
                .npc("werktuigguh", GuhNpcEntity.Kind.WERKTUIGGUH, new Vec3(1.5, 0, 0.5), 90f)
                // the door shimmers, gears turn behind it
                .camera(0, new Vec3(-7.5, 2.2, 4.5), deur.add(0, 1.2, 0))
                .camera(70, new Vec3(-6, 1.9, 3.2), deur.add(0, 1.2, 0))
                .deeltjes(10, ParticleTypes.END_ROD, deur.add(0, 1.5, 0), 30, 0.6)
                .geluid(10, () -> SoundEvents.BEACON_POWER_SELECT, 0.6f, 1.6f)
                .geluid(30, () -> SoundEvents.CHAIN_PLACE, 0.8f, 0.7f)
                .geluid(45, () -> SoundEvents.PISTON_CONTRACT, 0.7f, 0.8f)
                .deeltjes(55, ParticleTypes.CLOUD, deur.add(0, 1, 0), 20, 0.5)
                // he steps out through the door
                .loop("werktuigguh", 60, 110, buiten.add(1.8, 0, 0))
                .cameraKnip(112, new Vec3(-5.2, 1.6, 2.3), buiten.add(1.2, 1.0, 0))
                .kijk("werktuigguh", 112, buiten.add(-1.5, 1, 0))
                .animatie("werktuigguh", 115, "eureka")
                .zeg(120, "werktuigguh", "zin1", 60)
                .zeg(180, "werktuigguh", "zin2", 60)
                .animatie("werktuigguh", 235, "nog_niet_af")
                .zeg(240, "werktuigguh", "zin3", 60)
                .camera(240, new Vec3(-4.6, 1.7, 1.8), buiten.add(1.2, 1.0, 0))
                .animatie("werktuigguh", 295, "knutsel")
                .zeg(300, "werktuigguh", "zin4", 60)
                .loop("werktuigguh", 340, 375, new Vec3(1.5, 0, 0.5))
                .camera(380, new Vec3(-6.5, 2.4, 3.5), deur.add(0, 1.2, 0))
                .verbergEcht(10)
                .registreer();
    }

    // =====================================================================================================================
    // every 10 ticks per player
    // =====================================================================================================================

    static void tik(ServerPlayer p) {
        // the door's answer, for the client (only when it changed)
        boolean open = E2Trap5.deurOpen(p);
        Boolean was = VERTELD.put(p.getUUID(), open);
        if (was == null || was != open) {
            ModNetworking.sendTo(p, new T5Payloads.Open(open));
        }
        if (!(p.level() instanceof ServerLevel level) || level.dimension() != GuheindeFeature.GUHEINDE || !E2Trap5.open(p) || E2Trap5.lijn() == null) {
            return;
        }
        BlockPos deur = Plekken.bekend(level, S) ? Plekken.wereld(level, S, "werkplaatsdeur") : null;
        if (deur == null) {
            return;
        }
        double d = p.position().distanceTo(Vec3.atBottomCenterOf(deur));
        if (d < 64 && !INGERICHT.contains(level)) {
            richtIn(level);
        }
        if (d <= T5Getallen.DEUR_AFSTAND && !E2Trap5.lijn().begonnen(p) && !Cutscenes.bezig(p)) {
            speelScene(p, deur);
        }
    }

    /** The door scene for this player; when it is over (or seen before) the questline begins. */
    static void speelScene(ServerPlayer p, @Nullable BlockPos deur) {
        Cutscene s = Cutscene.van(SCENE);
        if (s != null && deur != null && !Cutscenes.gezien(p, SCENE)) {
            if (Cutscenes.speel(p, s, deur, Rotation.NONE, Werkplaats::begin)) {
                return;
            }
            return;   // (watching something else: next time)
        }
        begin(p);
    }

    /** The door is open for this player: the questline begins (the first talk is step 0). */
    static void begin(ServerPlayer p) {
        if (E2Trap5.lijn() != null && E2Trap5.lijn().begin(p)) {
            GidsFeature.grant(p, "guheinde/einde2_trap5_machines_werkplaats");
            p.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.einde2_trap5_machines.deur_open")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));
        }
        E2Trap5.vooruit(p);
    }

    static void vergeet(ServerPlayer p) {
        VERTELD.remove(p.getUUID());
    }

    // =====================================================================================================================
    // the practice setups
    // =====================================================================================================================

    /** Puts the six practice setups into the workshop (once per server start; a spot that holds its machine already is left). */
    static void richtIn(ServerLevel level) {
        INGERICHT.add(level);
        BlockPos o1 = Plekken.wereld(level, S, "oefen_1"), o2 = Plekken.wereld(level, S, "oefen_2"), o3 = Plekken.wereld(level, S, "oefen_3"),
                o4 = Plekken.wereld(level, S, "oefen_4"), o5 = Plekken.wereld(level, S, "oefen_5"), o6 = Plekken.wereld(level, S, "oefen_6");
        if (o1 == null || o2 == null || o3 == null || o4 == null || o5 == null || o6 == null || !level.isLoaded(o1) || !level.isLoaded(o6)) {
            INGERICHT.remove(level);   // (not loaded yet: again later)
            return;
        }
        // 1: a zender on a source, an ontvanger across the bay feeding a Vuurkorf
        bron(level, o1.offset(-2, -1, -1), 2);
        machine(level, o1.offset(-2, 0, -1), E2Trap5Feature.ZENDER.get().defaultBlockState(), Direction.SOUTH);
        bron2(level, o1.offset(2, 0, 1), E2Trap5Feature.ONTVANGER.get().defaultBlockState(), Direction.SOUTH);
        machine(level, o1.offset(2, 1, 1), E2Trap5Feature.VUURKORF.get().defaultBlockState(), Direction.SOUTH);
        // 2: a terminal
        bron(level, o2.offset(0, -1, 1), 1);
        machine(level, o2.offset(0, 0, 1), E2Trap5Feature.TERMINAL.get().defaultBlockState(), Direction.SOUTH);
        // 3: a balie with a Knutselmachine next to it
        bron(level, o3.offset(-1, -1, 1), 2);
        machine(level, o3.offset(-1, 0, 1), E2Trap5Feature.BESTELBALIE.get().defaultBlockState(), Direction.SOUTH);
        bron(level, o3.offset(1, -1, 1), 1);
        machine(level, o3.offset(1, 0, 1), TechmachineFeature.KNUTSELMACHINE.get().defaultBlockState(), Direction.SOUTH);
        // 4: two linked portal tubes
        bron(level, o4.offset(-2, -1, 1), 1);
        bron(level, o4.offset(2, -1, 1), 1);
        machine(level, o4.offset(-2, 0, 1), E2Trap5Feature.POORTBUIS.get().defaultBlockState(), Direction.EAST);
        machine(level, o4.offset(2, 0, 1), E2Trap5Feature.POORTBUIS.get().defaultBlockState(), Direction.WEST);
        // 5: two practice poorten that hop to each other
        bron(level, o5.offset(0, -1, -2), 1);
        bron(level, o5.offset(0, -1, 2), 1);
        machine(level, o5.offset(0, 0, -2), E2Trap5Feature.THUISPOORT.get().defaultBlockState(), Direction.WEST);
        machine(level, o5.offset(0, 0, 2), E2Trap5Feature.THUISPOORT.get().defaultBlockState(), Direction.WEST);
        // 6: the four element machines in a row (the blazer blows up: a little lift)
        for (int i = 0; i < 4; i++) {
            bron(level, o6.offset(0, -1, -2 + i + (i >= 2 ? 1 : 0)), 2);
        }
        machine(level, o6.offset(0, 0, -2), E2Trap5Feature.LUCHTBLAZER.get().defaultBlockState().setValue(Elementmachines.LuchtblazerBlock.OMHOOG, true),
                Direction.WEST);
        machine(level, o6.offset(0, 0, -1), E2Trap5Feature.WATERDRAGER.get().defaultBlockState(), Direction.WEST);
        machine(level, o6.offset(0, 0, 1), E2Trap5Feature.KORSTDUWER.get().defaultBlockState(), Direction.WEST);
        machine(level, o6.offset(0, 0, 2), E2Trap5Feature.VUURKORF.get().defaultBlockState(), Direction.WEST);
        // mark them all as practice (no owner, no projects, a click explains them)
        for (BlockPos o : new BlockPos[]{o1, o2, o3, o4, o5, o6}) {
            for (BlockPos p : BlockPos.betweenClosed(o.offset(-2, 0, -2), o.offset(2, 2, 2))) {
                oefen(level, p.immutable());
            }
        }
        if (level.getBlockEntity(o4.offset(-2, 0, 1)) instanceof Poortbuis.BlockEntityImpl a
                && level.getBlockEntity(o4.offset(2, 0, 1)) instanceof Poortbuis.BlockEntityImpl b) {
            a.koppel(net.minecraft.core.GlobalPos.of(level.dimension(), b.getBlockPos()));
            b.koppel(net.minecraft.core.GlobalPos.of(level.dimension(), a.getBlockPos()));
        }
        if (level.getBlockEntity(o5.offset(0, 0, -2)) instanceof Thuispoort.BlockEntityImpl a
                && level.getBlockEntity(o5.offset(0, 0, 2)) instanceof Thuispoort.BlockEntityImpl b) {
            a.oefenTwee = b.getBlockPos();
            b.oefenTwee = a.getBlockPos();
            a.setChanged();
            b.setChanged();
        }
    }

    private static void bron(ServerLevel level, BlockPos pos, int kracht) {
        BlockState s = VadskrachtFeature.TESTBRON.get().defaultBlockState().setValue(TestbronBlock.KRACHT, kracht);
        if (!level.getBlockState(pos).is(VadskrachtFeature.TESTBRON.get())) {
            level.setBlock(pos, s, Block.UPDATE_ALL);
        }
    }

    private static void machine(ServerLevel level, BlockPos pos, BlockState state, Direction facing) {
        if (!level.getBlockState(pos).is(state.getBlock())) {
            level.setBlock(pos, state.setValue(MachineBlock.FACING, facing), Block.UPDATE_ALL);
        }
    }

    private static void bron2(ServerLevel level, BlockPos pos, BlockState state, Direction facing) {
        if (!level.getBlockState(pos).is(state.getBlock())) {
            level.setBlock(pos, state.setValue(BronBlock.FACING, facing), Block.UPDATE_ALL);
        }
    }

    private static void oefen(ServerLevel level, BlockPos p) {
        switch (level.getBlockEntity(p)) {
            case Zender.BlockEntityImpl z -> z.oefen = true;
            case Ontvanger.BlockEntityImpl o -> o.oefen = true;
            case Terminal.BlockEntityImpl t -> t.oefen = true;
            case Bestelbalie.BlockEntityImpl b -> b.oefen = true;
            case Poortbuis.BlockEntityImpl b -> b.oefen = true;
            case Thuispoort.BlockEntityImpl t -> t.oefen = true;
            case Elementmachines.Luchtblazer m -> m.oefen = true;
            case Elementmachines.Waterdrager m -> m.oefen = true;
            case Elementmachines.Korstduwer m -> m.oefen = true;
            case Elementmachines.Vuurkorf m -> m.oefen = true;
            case null, default -> {
                return;
            }
        }
        level.getBlockEntity(p).setChanged();
    }

    /** (dev, tests) set up again at the next look. */
    static void vergeetInrichting() {
        INGERICHT.clear();
    }
}
