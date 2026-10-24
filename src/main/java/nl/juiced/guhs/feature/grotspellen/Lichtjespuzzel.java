package nl.juiced.guhs.feature.grotspellen;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * De Lichtjespuzzel with Meester Knipperlicht: one big 7x7 raster of mushroom lamps in the floor of his hall (every lamp
 * {@value #CEL}x{@value #CEL} blocks), a new puzzle every game ({@link LichtjesRaster#puzzel}), against the clock. Jump on a
 * lamp (or crouch on it): it and its four neighbours switch. Every lamp on = solved; fewer presses and more time left =
 * more points and lampjesmunten. After the game every lamp is on again (his cave stays cosy).
 */
public final class Lichtjespuzzel extends GrotSpel {
    /** A lamp of the raster is this many blocks wide. */
    public static final int CEL = 2;
    public static final int MAAT = LichtjesRaster.N * CEL;

    @Nullable
    private BlockPos hoek;
    @Nullable
    private LichtjesRaster raster;
    private int stappen, minimaal;
    private boolean wasOpGrond = true, wasGebukt;
    private int laatsteX = -1, laatsteZ = -1;

    Lichtjespuzzel(UUID npcId) {
        super(npcId);
    }

    @Override
    public Soort soort() {
        return Soort.LICHTJESPUZZEL;
    }

    @Nullable
    public LichtjesRaster raster() {
        return raster;
    }

    @Nullable
    public BlockPos hoek() {
        return hoek;
    }

    public int stappen() {
        return stappen;
    }

    public int minimaal() {
        return minimaal;
    }

    @Override
    protected boolean zoek(ServerLevel world, GuhNpcEntity npc) {
        int minX = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE, y = Integer.MIN_VALUE;
        for (BlockPos pos : inDeVloer(world, zaalLucht(world, npc), GrotSpellenFeature.PUZZELLAMP.get())) {
            {
                if (y == Integer.MIN_VALUE) {
                    y = pos.getY();
                }
                if (pos.getY() != y) {
                    continue;
                }
                minX = Math.min(minX, pos.getX());
                minZ = Math.min(minZ, pos.getZ());
                maxX = Math.max(maxX, pos.getX());
                maxZ = Math.max(maxZ, pos.getZ());
            }
        }
        if (y == Integer.MIN_VALUE || maxX - minX + 1 != MAAT || maxZ - minZ + 1 != MAAT) {
            return false;
        }
        hoek = new BlockPos(minX, y, minZ);
        return true;
    }

    @Override
    protected boolean heel(ServerLevel world) {
        return hoek != null && world.getBlockState(hoek).is(GrotSpellenFeature.PUZZELLAMP.get())
                && world.getBlockState(hoek.offset(MAAT - 1, 0, MAAT - 1)).is(GrotSpellenFeature.PUZZELLAMP.get());
    }

    @Override
    protected int speelTicks() {
        return LichtjesRaster.speelTicks(niveau);
    }

    @Override
    protected void begin(ServerLevel world, ServerPlayer p, GuhNpcEntity npc) {
        raster = LichtjesRaster.puzzel(world.getRandom(), LichtjesRaster.drukken(niveau));
        minimaal = raster.minimaal();
        stappen = 0;
        wasOpGrond = true;
        wasGebukt = true;
        laatsteX = laatsteZ = -1;
        toon(world);
        // you start at the edge of the raster nearest to the host, in the middle, looking across it
        int mid = MAAT / 2;
        BlockPos[] randen = {hoek.offset(mid, 0, -1), hoek.offset(mid, 0, MAAT), hoek.offset(-1, 0, mid), hoek.offset(MAAT, 0, mid)};
        float[] kijk = {0f, 180f, -90f, 90f};
        int beste = 0;
        for (int i = 1; i < randen.length; i++) {
            if (randen[i].distSqr(npc.blockPosition()) < randen[beste].distSqr(npc.blockPosition())) {
                beste = i;
            }
        }
        BlockPos start = randen[beste];
        p.teleportTo(world, start.getX() + 0.5, start.getY() + 1.0, start.getZ() + 0.5, Set.of(), kijk[beste], 30f, true);
    }

    /** The lamps in the floor as the raster says. */
    private void toon(ServerLevel world) {
        if (hoek == null) {
            return;
        }
        for (int x = 0; x < LichtjesRaster.N; x++) {
            for (int z = 0; z < LichtjesRaster.N; z++) {
                boolean aan = raster == null || raster.aan(x, z);
                for (int dx = 0; dx < CEL; dx++) {
                    for (int dz = 0; dz < CEL; dz++) {
                        BlockPos pos = hoek.offset(x * CEL + dx, 0, z * CEL + dz);
                        BlockState s = world.getBlockState(pos);
                        if (s.is(GrotSpellenFeature.PUZZELLAMP.get()) && s.getValue(GrotSpellenBlocks.AAN) != aan) {
                            world.setBlock(pos, s.setValue(GrotSpellenBlocks.AAN, aan), 2);
                        }
                    }
                }
            }
        }
    }

    /** The lamp under this block of the floor (cell {x, z}), or null. */
    @Nullable
    public int[] celVan(BlockPos pos) {
        if (hoek == null || pos.getY() != hoek.getY()) {
            return null;
        }
        int x = pos.getX() - hoek.getX(), z = pos.getZ() - hoek.getZ();
        if (x < 0 || z < 0 || x >= MAAT || z >= MAAT) {
            return null;
        }
        return new int[]{x / CEL, z / CEL};
    }

    @Override
    protected void speelTick(ServerLevel world, ServerPlayer p, GuhNpcEntity npc) {
        boolean opGrond = p.onGround(), gebukt = p.isShiftKeyDown();
        boolean geland = opGrond && !wasOpGrond, gedrukt = gebukt && !wasGebukt && opGrond;
        wasOpGrond = opGrond;
        wasGebukt = gebukt;
        if (!geland && !gedrukt) {
            return;
        }
        int[] cel = celVan(BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ()));
        if (cel != null) {
            druk(world, p, cel[0], cel[1]);
        }
    }

    /** Presses a lamp of the raster (it and its four neighbours switch). */
    public boolean druk(ServerLevel world, ServerPlayer p, int x, int z) {
        if (fase != Fase.SPELEN || raster == null || !raster.druk(x, z)) {
            return false;
        }
        stappen++;
        laatsteX = x;
        laatsteZ = z;
        toon(world);
        BlockPos m = hoek.offset(x * CEL + 1, 1, z * CEL + 1);
        boolean aan = raster.aan(x, z);
        world.playSound(null, m, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.9f, aan ? 1.6f : 0.9f);
        world.sendParticles(aan ? ParticleTypes.GLOW : ParticleTypes.SMOKE, m.getX(), m.getY() + 0.1, m.getZ(), 6, 0.6, 0.1, 0.6, 0.01);
        if (raster.allesAan()) {
            world.playSound(null, m, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.4f);
        } else {
            flits(p, Component.translatable("gui.guhs.lichtjespuzzel.klik", raster.aantalAan(), LichtjesRaster.N * LichtjesRaster.N)
                    .withStyle(aan ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
        }
        return true;
    }

    @Override
    protected boolean klaar() {
        return raster != null && raster.allesAan();
    }

    @Override
    protected Component balk() {
        return Component.translatable("gui.guhs.lichtjespuzzel.balk", raster == null ? 0 : raster.aantalAan(), LichtjesRaster.N * LichtjesRaster.N,
                stappen, minimaal, klok()).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    @Override
    protected int punten() {
        return LichtjesRaster.punten(klaar(), over, stappen, minimaal);
    }

    @Override
    protected int munten(boolean record) {
        return LichtjesRaster.munten(niveau, klaar(), over, speelTicks(), stappen, minimaal, raster == null ? 0 : raster.aantalAan(), record);
    }

    @Override
    protected List<Component> uitslagRegels() {
        if (klaar()) {
            return List.of(Component.translatable("quest.guhs.lichtjespuzzel.uitslag", stappen, minimaal),
                    Component.translatable("quest.guhs.lichtjespuzzel.uitslag_tijd", Math.max(0, over) / 20));
        }
        return List.of(Component.translatable("quest.guhs.lichtjespuzzel.uitslag_niet", raster == null ? 0 : raster.aantalAan(),
                LichtjesRaster.N * LichtjesRaster.N));
    }

    /** Every lamp on again. */
    @Override
    protected void opruimen(ServerLevel world) {
        raster = null;
        toon(world);
    }

    @Override
    protected List<ItemStack> leen() {
        return List.of();
    }
}
