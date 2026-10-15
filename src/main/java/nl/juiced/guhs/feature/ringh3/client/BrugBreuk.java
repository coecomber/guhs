package nl.juiced.guhs.feature.ringh3.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.feature.ringh3.Plekken;
import nl.juiced.guhs.feature.ringh3.Scenes;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.client.CutsceneSpeler;

/**
 * bbq2 (ring-h3): the Brug van Knabbel-dum breaks in THIS game only. The stones of the span ({@link Plekken#BRUG_KAPOT}) are
 * taken out of this client's level (the server's bridge never changes: it is a shared thing):
 * <ul>
 *   <li>at the tick of the bridge scene where it happens ({@link Scenes#BRUG_BREEKT}): where the bridge is in the world is
 *       read from two of the scene's own actors (the Barbecuerog and Guhdalf stand on it), so it also works for a replay from
 *       the Guhdex;</li>
 *   <li>and when the server says so ({@code guhs:ringh3_brug}: after the scene the bridge stays broken for that player for a
 *       minute, then "whole again").</li>
 * </ul>
 * While it is broken the stones are taken out again once a second (the server may show the real ones again after a chunk
 * reload). A break that no server message confirms (a replay) heals by itself a few seconds after the scene.
 * <p>
 * When it breaks before your eyes the stones FALL: the span gives way under his feet first and then outwards to both ends
 * ({@link #STRAKS}), and every stone becomes a falling block of this game alone ({@link #PUIN}: the server never hears of
 * them, they place nothing and drop nothing) that tumbles into the chasm and goes up in dust where it lands.
 */
public final class BrugBreuk {
    /** The stones that are gone, with what they were. */
    private static final Map<BlockPos, BlockState> WEG = new HashMap<>();
    @Nullable
    private static ClientLevel waar;
    private static boolean server, sceneBrak;
    private static int heelOver = -1, tik;
    /** The stones on their way down (entities of this client only; ids far below the cutscene actors' own). */
    private static final List<FallingBlockEntity> PUIN = new ArrayList<>();
    /** The stones that are about to let go: {where, what, ticks until it does}. */
    private static final List<Object[]> STRAKS = new ArrayList<>();
    private static int puinId = -5_000_000;
    /** A falling stone lives at most this many ticks (the chasm is ten deep: a second and a half). */
    private static final int PUIN_TICKS = 70;
    /** After a scene that the server does not follow up, the bridge is whole again after this many ticks. */
    private static final int NA_SCENE = 100;

    private BrugBreuk() {
    }

    /** The server: your bridge is broken (nul = the world position of the template block (0, 0, 0), draai = its rotation) / whole. */
    public static void vanServer(BlockPos nul, int draai, boolean kapot) {
        server = kapot;
        if (kapot) {
            heelOver = -1;
            breek(nul, Rotation.values()[Math.floorMod(draai, Rotation.values().length)], WEG.isEmpty());
        } else {
            heel(true);
        }
    }

    static void wis() {
        WEG.clear();
        PUIN.clear();
        STRAKS.clear();
        waar = null;
        server = false;
        sceneBrak = false;
        heelOver = -1;
    }

    static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || (waar != null && waar != level)) {
            wis();
            return;
        }
        Cutscene scene = CutsceneSpeler.scene();
        if (scene == Scenes.BRUG) {
            if (CutsceneSpeler.tijd() == Scenes.BRUG_BREEKT && !sceneBrak) {
                sceneBrak = true;
                heelOver = -1;
                breekInScene(level);
            } else if (CutsceneSpeler.tijd() < Scenes.BRUG_BREEKT - 5 && sceneBrak) {
                sceneBrak = false;
            }
        } else if (sceneBrak) {
            // the scene is over: the server says within a moment whether the bridge stays broken; a replay heals by itself
            sceneBrak = false;
            if (!server) {
                heelOver = NA_SCENE;
            }
        }
        if (heelOver > 0 && --heelOver == 0) {
            heelOver = -1;
            if (!server) {
                heel(true);
            }
        }
        straks(level);
        puinTick(level);
        if (!WEG.isEmpty() && STRAKS.isEmpty() && ++tik % 20 == 0) {
            for (BlockPos pos : WEG.keySet()) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 19);
                }
            }
        }
    }

    /** In the scene: where the copy is comes from the scene's own actors ({@link SceneVuur#anker}), then break. */
    private static void breekInScene(ClientLevel level) {
        BlockPos anker = SceneVuur.anker();
        if (anker == null) {
            return;
        }
        Rotation draai = SceneVuur.draai();
        breek(anker.subtract(StructureTemplate.transform(Plekken.BRUG_ANKER, Mirror.NONE, draai, BlockPos.ZERO)), draai, true);
    }

    private static void breek(BlockPos nul, Rotation draai, boolean stof) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        waar = level;
        RandomSource r = level.getRandom();
        Plekken.Doos d = Plekken.BRUG_KAPOT;
        for (int x = d.x0(); x <= d.x1(); x++) {
            for (int y = d.y0(); y <= d.y1(); y++) {
                for (int z = d.z0(); z <= d.z1(); z++) {
                    BlockPos pos = nul.offset(StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, draai, BlockPos.ZERO));
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir()) {
                        continue;
                    }
                    WEG.putIfAbsent(pos.immutable(), state);
                    if (stof) {
                        // (under his feet first, then outwards to both ends: the span gives way, it does not blink out)
                        STRAKS.add(new Object[] {pos.immutable(), state, Math.abs(x - (d.x0() + d.x1()) / 2) * 2 + r.nextInt(2)});
                        continue;
                    }
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 19);
                }
            }
        }
    }

    /** (every tick) the stones whose moment has come let go: each becomes a falling stone, splinters fly, dust rolls off it. */
    private static void straks(ClientLevel level) {
        RandomSource r = level.getRandom();
        for (Iterator<Object[]> it = STRAKS.iterator(); it.hasNext(); ) {
            Object[] s = it.next();
            int over = (Integer) s[2] - 1;
            s[2] = over;
            if (over >= 0) {
                continue;
            }
            it.remove();
            BlockPos pos = (BlockPos) s[0];
            BlockState state = (BlockState) s[1];
            if (level.getBlockState(pos).isAir()) {
                continue;
            }
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 19);
            val(level, pos, state, r);
            for (int i = 0; i < 3; i++) {
                level.addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), true, pos.getX() + r.nextDouble(),
                        pos.getY() + r.nextDouble(), pos.getZ() + r.nextDouble(), (r.nextDouble() - 0.5) * 0.3, -0.2 - r.nextDouble() * 0.4,
                        (r.nextDouble() - 0.5) * 0.3);
            }
            if (r.nextInt(3) == 0) {
                SceneVuur.deeltje(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, (r.nextDouble() - 0.5) * 0.08,
                        0.01 + r.nextDouble() * 0.05, (r.nextDouble() - 0.5) * 0.08, 0.8f + r.nextFloat() * 0.7f, 44 + r.nextInt(30), 0x4A4038);
            }
        }
    }

    /** One stone of the span lets go (the place it had is air already). */
    private static void val(ClientLevel level, BlockPos pos, BlockState state, RandomSource r) {
        if (state.getRenderShape() != RenderShape.MODEL || PUIN.size() >= 96) {
            return;
        }
        // (on a client this makes the entity and nothing else: a client level takes no "fresh" entities, it is added below
        // under an id of our own)
        FallingBlockEntity steen = FallingBlockEntity.fall(level, pos, state);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 19);
        steen.dropItem = false;
        steen.setId(puinId--);
        steen.setDeltaMovement((r.nextDouble() - 0.5) * 0.22, 0.06 - r.nextDouble() * 0.3, (r.nextDouble() - 0.5) * 0.3);
        level.addEntity(steen);
        PUIN.add(steen);
    }

    /** (every tick) a stone that landed, or fell long enough, goes up in dust. */
    private static void puinTick(ClientLevel level) {
        for (Iterator<FallingBlockEntity> it = PUIN.iterator(); it.hasNext(); ) {
            FallingBlockEntity steen = it.next();
            if (steen.isRemoved() || steen.level() != level) {
                it.remove();
            } else if (steen.onGround() || steen.tickCount > PUIN_TICKS) {
                RandomSource r = level.getRandom();
                for (int i = 0; i < 4; i++) {
                    level.addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, steen.getBlockState()), true, steen.getX() + r.nextDouble() - 0.5,
                            steen.getY() + r.nextDouble(), steen.getZ() + r.nextDouble() - 0.5, (r.nextDouble() - 0.5) * 0.4, 0.2 + r.nextDouble() * 0.3,
                            (r.nextDouble() - 0.5) * 0.4);
                }
                level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, true, steen.getX(), steen.getY() + 0.4, steen.getZ(), 0, 0.04, 0);
                level.removeEntity(steen.getId(), Entity.RemovalReason.DISCARDED);
                it.remove();
            }
        }
    }

    private static void weg(ClientLevel level) {
        for (FallingBlockEntity steen : PUIN) {
            if (!steen.isRemoved() && steen.level() == level) {
                level.removeEntity(steen.getId(), Entity.RemovalReason.DISCARDED);
            }
        }
        PUIN.clear();
    }

    private static void heel(boolean puf) {
        ClientLevel level = Minecraft.getInstance().level;
        STRAKS.clear();
        if (level != null && level == waar) {
            weg(level);
            for (Map.Entry<BlockPos, BlockState> e : WEG.entrySet()) {
                if (level.getBlockState(e.getKey()).isAir()) {
                    level.setBlock(e.getKey(), e.getValue(), 19);
                    if (puf) {
                        level.addParticle(ParticleTypes.POOF, e.getKey().getX() + 0.5, e.getKey().getY() + 0.6, e.getKey().getZ() + 0.5, 0, 0.02, 0);
                    }
                }
            }
        }
        WEG.clear();
        waar = null;
    }
}
