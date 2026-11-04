package nl.juiced.guhs.feature.vadsrijk.kern.beleving;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.storage.Nbt;

/**
 * The chest ceremony (idea 28), kern's {@link BaasKist#CEREMONIE}:
 * <ul>
 *   <li>the personal boss chest falls from the sky (a block display, 16 blocks in 14 ticks) inside a light pillar; the real chest
 *   block appears where it lands (after a boss defeat: {@link Beleving#KIST_NA_OVERWINNING} ticks after it, so the victory moment
 *   plays first). In the gametest server it is placed at once. On a server stop every chest still in the air lands at once;</li>
 *   <li>opening builds up: a rising run of notes, one more per rarity step, then a burst in the colour of the best piece;</li>
 *   <li>Guhddelijk: a pink screen flash, a glitter rain and a server message "&lt;speler&gt; vond &lt;item&gt;!".</li>
 * </ul>
 */
public final class Kistceremonie implements BaasKist.Ceremonie {
    public static final int HOOGTE = 16, VAL = 14, ZUIL_NA = 60;

    /** A chest waiting to be put down (marker type, so a server stop can still run it). */
    static final class Neer implements Runnable {
        final Runnable r;
        boolean gedaan;

        Neer(Runnable r) {
            this.r = r;
        }

        @Override
        public void run() {
            if (!gedaan) {
                gedaan = true;
                r.run();
            }
        }
    }

    private static final class Val {
        final ServerLevel sl;
        final UUID eigenaar;
        final BlockPos plek;
        final Neer neer;
        final long start;
        @Nullable
        Display.BlockDisplay d;
        boolean geland;

        Val(ServerLevel sl, UUID eigenaar, BlockPos plek, Neer neer, long start) {
            this.sl = sl;
            this.eigenaar = eigenaar;
            this.plek = plek;
            this.neer = neer;
            this.start = start;
        }
    }

    private static final List<Val> VALLEN = new ArrayList<>();

    @Override
    public void val(ServerLevel sl, ServerPlayer p, BlockPos plek, List<ItemStack> buit, Runnable neerzetten) {
        Neer neer = new Neer(neerzetten);
        if (sl.getServer() instanceof net.minecraft.gametest.framework.GameTestServer) {
            neer.run();
            return;
        }
        UUID eigenaar = p.getUUID();
        // one tick later: by then the defeat hook (the victory) ran, so we know if this chest belongs to a victory moment
        Beleving.later(sl, 1, () -> {
            long nu = sl.getServer().getTickCount();
            Vec3 o = Beleving.overwinningPlek;
            boolean naOverwinning = nu - Beleving.laatsteOverwinning < 10 && o != null && o.distanceToSqr(Vec3.atCenterOf(plek)) < 64 * 64;
            long start = naOverwinning ? Beleving.laatsteOverwinning + Beleving.KIST_NA_OVERWINNING : nu + 2;
            VALLEN.add(new Val(sl, eigenaar, plek, neer, start));
        });
    }

    static void tick(MinecraftServer server) {
        if (VALLEN.isEmpty()) {
            return;
        }
        long nu = server.getTickCount();
        Iterator<Val> it = VALLEN.iterator();
        List<Runnable> landingen = new ArrayList<>();
        while (it.hasNext()) {
            Val v = it.next();
            long t = nu - v.start;
            if (t < 0) {
                continue;
            }
            ServerLevel sl = v.sl;
            double cx = v.plek.getX() + 0.5, cz = v.plek.getZ() + 0.5;
            if (t % 2 == 0 && t <= VAL + ZUIL_NA) {
                zuil(sl, cx, v.plek.getY(), cz, t > VAL);
            }
            if (!v.geland) {
                if (v.d == null) {
                    v.d = blok(sl, v.plek.getX(), v.plek.getY() + HOOGTE, v.plek.getZ());
                    sl.playSound(null, cx, v.plek.getY() + 4, cz, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.6f);
                    sl.playSound(null, cx, v.plek.getY() + 4, cz, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 0.6f, 0.7f);
                }
                double f = Math.min(1, (double) t / VAL);
                if (v.d != null) {
                    v.d.teleportTo(v.plek.getX(), v.plek.getY() + HOOGTE * (1 - f * f), v.plek.getZ());
                }
                if (t >= VAL) {
                    v.geland = true;
                    landingen.add(() -> land(v));
                }
            } else if (t > VAL + ZUIL_NA) {
                it.remove();
            }
        }
        landingen.forEach(Runnable::run);
    }

    private static void land(Val v) {
        if (v.d != null) {
            v.d.discard();
            v.d = null;
        }
        v.neer.run();
        ServerLevel sl = v.sl;
        double cx = v.plek.getX() + 0.5, cy = v.plek.getY(), cz = v.plek.getZ() + 0.5;
        sl.playSound(null, cx, cy, cz, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.35f, 1.5f);
        sl.playSound(null, cx, cy, cz, SoundEvents.SLIME_BLOCK_FALL, SoundSource.BLOCKS, 1.0f, 0.8f);
        sl.sendParticles(ParticleTypes.POOF, cx, cy + 0.1, cz, 24, 0.8, 0.05, 0.8, 0.06);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, cx, cy + 0.5, cz, 20, 0.6, 0.3, 0.6, 0.05);
        if (sl.getServer().getPlayerList().getPlayer(v.eigenaar) instanceof ServerPlayer p) {
            Beleving.schud(p, 0.35f, 6);
        }
    }

    /** The light pillar: white rods rising + a pink glow, from the floor up to the drop height. */
    private static void zuil(ServerLevel sl, double x, int y, double z, boolean geland) {
        DustParticleOptions roze = new DustParticleOptions(0xFF9AD5, 1.6f);
        for (int i = 0; i < (geland ? 6 : 10); i++) {
            double h = sl.getRandom().nextDouble() * HOOGTE;
            sl.sendParticles(ParticleTypes.END_ROD, x, y + 0.5 + h, z, 1, 0.08, 0.2, 0.08, 0.01);
            sl.sendParticles(roze, x, y + 0.5 + h, z, 1, 0.25, 0.3, 0.25, 0);
        }
    }

    @Nullable
    private static Display.BlockDisplay blok(ServerLevel sl, double x, double y, double z) {
        Display.BlockDisplay d = EntityType.BLOCK_DISPLAY.create(sl, EntitySpawnReason.TRIGGERED);
        if (d == null) {
            return null;
        }
        CompoundTag tag = new CompoundTag();
        tag.put("block_state", NbtUtils.writeBlockState(KernRegistry.BAASKIST.get().defaultBlockState()));
        tag.putInt("teleport_duration", 1);
        tag.putFloat("view_range", 4);
        CompoundTag tr = new CompoundTag();
        tr.put("translation", floats(0, 0, 0));
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("scale", floats(1, 1, 1));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tag.put("transformation", tr);
        Nbt.load(d, tag);
        d.snapTo(x, y, z, 0, 0);
        d.addTag(nl.juiced.guhs.feature.vadsrijk.kern.Getallen.TAG);   // (kern removes stray number/hologram displays by this tag)
        sl.addFreshEntity(d);
        return d;
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    /** Server stop: every chest still in the air lands now (never lose loot). */
    static void allesNeer() {
        for (Val v : VALLEN) {
            if (v.d != null) {
                v.d.discard();
            }
            v.neer.run();
        }
        VALLEN.clear();
    }

    // ---- opening ------------------------------------------------------------------------------------------------------

    /** The best rarity in the loot (pieces without a roll count as Gewoon). */
    public static Zeldzaamheid beste(List<ItemStack> buit) {
        Zeldzaamheid best = Zeldzaamheid.GEWOON;
        for (ItemStack s : buit) {
            Uitrusting u = Rollen.van(s);
            if (u != null && u.zeldzaamheid().ordinal() > best.ordinal()) {
                best = u.zeldzaamheid();
            }
        }
        return best;
    }

    /** Pure: how many build-up notes (3 for Gewoon .. 8 for Guhddelijk). */
    public static int noten(Zeldzaamheid z) {
        return 3 + z.ordinal();
    }

    @Override
    public void open(ServerLevel sl, ServerPlayer p, BlockPos pos, List<ItemStack> buit) {
        Zeldzaamheid best = beste(buit);
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.6, cz = pos.getZ() + 0.5;
        int noten = noten(best);
        for (int i = 0; i < noten; i++) {
            int stap = i;
            Beleving.later(sl, i * 3, () -> {
                float toon = Reeks.toon(stap * 2 + 1);
                sl.playSound(null, cx, cy, cz, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, toon);
                if (stap >= 3) {
                    sl.playSound(null, cx, cy, cz, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.4f, toon);
                }
                sl.sendParticles(new DustParticleOptions(best.rgb, 1.0f + stap * 0.15f), cx, cy + stap * 0.25, cz, 4 + stap * 2, 0.3, 0.2, 0.3, 0);
            });
        }
        Beleving.later(sl, noten * 3 + 2, () -> onthul(sl, p, cx, cy, cz, best, buit));
    }

    private static void onthul(ServerLevel sl, ServerPlayer p, double cx, double cy, double cz, Zeldzaamheid best, List<ItemStack> buit) {
        sl.sendParticles(new DustParticleOptions(best.rgb, 2.2f), cx, cy + 0.6, cz, 50, 0.6, 0.6, 0.6, 0.3);
        sl.sendParticles(ParticleTypes.FIREWORK, cx, cy + 0.6, cz, 10 + best.ordinal() * 8, 0.2, 0.3, 0.2, 0.15);
        switch (best) {
            case GEWOON, ONGEWOON -> sl.playSound(null, cx, cy, cz, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.8f);
            case ZELDZAAM -> {
                sl.playSound(null, cx, cy, cz, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.5f);
                sl.playSound(null, cx, cy, cz, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
            }
            case EPISCH -> {
                sl.playSound(null, cx, cy, cz, SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
                sl.playSound(null, cx, cy, cz, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
            case LEGENDARISCH -> {
                sl.playSound(null, cx, cy, cz, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.5f, 1.3f);
                sl.playSound(null, cx, cy, cz, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.2f);
            }
            case GUHDDELIJK -> guhddelijk(sl, p, cx, cy, cz, buit);
        }
    }

    private static void guhddelijk(ServerLevel sl, ServerPlayer p, double cx, double cy, double cz, List<ItemStack> buit) {
        sl.playSound(null, cx, cy, cz, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0f, 1.0f);
        sl.playSound(null, cx, cy, cz, SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 0.6f, 1.5f);
        Beleving.flits(p, Beleving.ROZE, 14);
        Beleving.schud(p, 0.4f, 8);
        // glitter rain around the player for 3 s
        DustParticleOptions roze = new DustParticleOptions(0xFF5FC8, 1.3f), wit = new DustParticleOptions(0xFFF0FA, 1.0f);
        for (int i = 0; i < 20; i++) {
            Beleving.later(sl, i * 3, () -> {
                Vec3 c = p.isRemoved() ? new Vec3(cx, cy, cz) : p.position();
                sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 5, c.z, 6, 3, 0.5, 3, 0.02);
                sl.sendParticles(roze, c.x, c.y + 4, c.z, 12, 3, 1.5, 3, 0);
                sl.sendParticles(wit, c.x, c.y + 4, c.z, 8, 3, 1.5, 3, 0);
                sl.sendParticles(ParticleTypes.FIREWORK, c.x, c.y + 5, c.z, 3, 3, 0.5, 3, 0.01);
            });
        }
        for (ItemStack s : buit) {
            Uitrusting u = Rollen.van(s);
            if (u != null && u.zeldzaamheid() == Zeldzaamheid.GUHDDELIJK) {
                sl.getServer().getPlayerList().broadcastSystemMessage(Component.translatableWithFallback("vadsrijk.kern.beleving.vond", "%s vond %s!",
                        p.getDisplayName(), s.getHoverName().copy().withStyle(st -> st.withColor(Zeldzaamheid.GUHDDELIJK.rgb).withBold(true)))
                        .withStyle(ChatFormatting.LIGHT_PURPLE), false);
            }
        }
    }
}
