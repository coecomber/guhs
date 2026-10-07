package nl.juiced.guhs.feature.guhpixel.grap1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.mojang.math.Transformation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.emotes.EmotesFeature;
import nl.juiced.guhs.feature.guhpixel.Arena;
import nl.juiced.guhs.feature.guhpixel.ArenaSoort;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.feature.guhpixel.SessieStart;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.guhpixel.Vertrek;
import nl.juiced.guhs.feature.wereldleven.KnuffelBlock;
import nl.juiced.guhs.feature.wereldleven.WereldlevenFeature;
import nl.juiced.guhs.quest.Scorebord;

/**
 * Vadsnite: you and 99 guhs jump out of the flying Vadsbus over a small island. Everyone lands and falls asleep at once:
 * the counter "Nog wakker" races from 100 down, a harmless pink slaapwolk closes in, and when the last guh nods off you
 * are the only one awake: "#1 VADSOVERWINNING". Lie down in one of the sleeping spots yourself and it is "Iedereen slaapt.
 * Gelijkspel, njeg." (that counts too).
 * <p>
 * The 99 guhs are cheap: block display entities showing the plushie blocks (no AI, no ticking, no GeckoLib), each with a
 * second display as its parachute while it falls. They glide with the display's own interpolation (two moves each), are
 * tagged, and are removed when the game ends (and once more by the arena clean-up).
 * <p>
 * Steps: 1 get on the Vadsbus, 2 jump out, 3 land on the island, 4 be the last one awake.
 * Geometry = tools/features/guhpixel_grap1_bouw.py (vadsnite); the game tests check the template against these constants.
 */
public final class VadsniteSessie extends GrapSessie {
    public static final String ID = "vadsnite";
    public static final int STAPPEN = 4, GUHS = 99;
    public static final Vec3i MAAT = new Vec3i(49, 48, 49);
    /** Template y of the island's top layer and of the bus floor; the middle of the island (x and z). */
    public static final int EILAND_Y = 10, BUS_Y = 34, MIDDEN = 24;
    /** The hatch in the bus floor (template, x0..x1, z0..z1). */
    public static final int LUIK_X0 = 23, LUIK_X1 = 25, LUIK_Z0 = 20, LUIK_Z1 = 21;
    /** The feet ends of the four sleeping spots (their heads lie one block to the north). */
    public static final List<BlockPos> BEDDEN = List.of(new BlockPos(17, 11, 25), new BlockPos(31, 11, 25), new BlockPos(24, 11, 33), new BlockPos(24, 11, 16));
    public static final ArenaSoort ARENA = new ArenaSoort(ID, Guhs.id("guhpixel/vadsnite_eiland"), MAAT, new Vec3(24.5, BUS_Y + 1, 26.5), 180f, false,
            VadsniteSessie::herstel);
    public static final SpelSoort SPEL = new SpelSoort(ID, ARENA, 1, 1, LobbyPlek.SPEL_VADSNITE, VadsniteSessie::new);
    public static final String TAG = "guhs_vadsnite";
    /** The countdown ends and the guhs start jumping; how many jump per tick; the two glides of a guh; the helping paw. */
    static final int SPRING_START = 80, PER_TICK = 3, GLIJ = 30, DUW_NA = 20 * 30;
    /** The slaapwolk: how long it closes, from which radius to which. */
    static final int WOLK_TICKS = 20 * 15, EIND_TICKS = 170;
    static final double WOLK_VAN = 22, WOLK_TOT = 2.5;
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFF8FC8, 2.2f);
    private static final Block[] PARACHUTES = {Blocks.PINK_WOOL, Blocks.MAGENTA_WOOL, Blocks.WHITE_WOOL, Blocks.YELLOW_WOOL, Blocks.LIGHT_BLUE_WOOL};

    public enum Uitslag { BEZIG, OVERWINNING, GELIJKSPEL }

    /** One of the 99: when it jumps, where it lands, its displays. */
    private static final class Valguh {
        final int sprong;
        final Vec3 uit, half, land;
        final float yaw;
        final BlockState knuffel, parachute;
        @Nullable
        Entity guh, scherm;
        boolean slaapt;

        Valguh(int sprong, Vec3 uit, Vec3 land, float yaw, BlockState knuffel, BlockState parachute) {
            this.sprong = sprong;
            this.uit = uit;
            this.land = land;
            this.half = new Vec3((uit.x + land.x) / 2, (uit.y + land.y) / 2 + 2, (uit.z + land.z) / 2);
            this.yaw = yaw;
            this.knuffel = knuffel;
            this.parachute = parachute;
        }
    }

    private final ServerBossEvent teller = new ServerBossEvent(UUID.randomUUID(), tellerTekst(GUHS + 1), BossEvent.BossBarColor.PINK,
            BossEvent.BossBarOverlay.PROGRESS);
    private final List<Valguh> guhs = new ArrayList<>();
    private final Random rng;
    private boolean gesprongen, geland, wolkGezegd;
    private int wolkSinds = -1, getoond = GUHS + 1;
    private Uitslag uitslag = Uitslag.BEZIG;

    VadsniteSessie(SessieStart start) {
        super(start, ID);
        this.rng = new Random(start.id().getLeastSignificantBits());
    }

    private static Component tellerTekst(int n) {
        return Component.translatable("gui.guhs.vadsnite.teller", n).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    static void herstel(Arena a) {
        for (BlockPos voet : BEDDEN) {
            bedVrij(a, voet, voet.north());
        }
    }

    public Uitslag uitslag() {
        return uitslag;
    }

    /** How many are still awake: the guhs that do not sleep yet, and the player when they are not lying down. */
    public int wakker() {
        int n = 0;
        for (Valguh g : guhs) {
            if (!g.slaapt) {
                n++;
            }
        }
        ServerPlayer p = speler();
        return n + (p != null && !ligt(p) ? 1 : 0);
    }

    /** The display entities of this game that exist right now. */
    public int schermen() {
        int n = 0;
        for (Valguh g : guhs) {
            n += (g.guh != null && !g.guh.isRemoved() ? 1 : 0) + (g.scherm != null && !g.scherm.isRemoved() ? 1 : 0);
        }
        return n;
    }

    public boolean geland() {
        return geland;
    }

    @Override
    protected void begin() {
        Arena a = arena();
        ServerLevel level = level();
        List<Block> knuffels = new ArrayList<>();
        for (var b : WereldlevenFeature.KNUFFELS.values()) {
            knuffels.add(b.get());
        }
        for (int i = 0; i < GUHS; i++) {
            boolean laatste = i == GUHS - 1;
            Vec3 uit = a.wereld(new Vec3(LUIK_X0 + 0.5 + rng.nextDouble() * (LUIK_X1 - LUIK_X0), BUS_Y - 1.5, LUIK_Z0 + 0.5 + rng.nextDouble() * (LUIK_Z1 - LUIK_Z0)));
            Vec3 land = laatste ? a.wereld(new Vec3(MIDDEN + 0.5, EILAND_Y + 1, MIDDEN - 2.5)) : landplek(a);
            BlockState knuffel = knuffels.isEmpty() ? Blocks.PINK_WOOL.defaultBlockState() : knuffels.get(rng.nextInt(knuffels.size())).defaultBlockState();
            if (knuffel.getBlock() instanceof KnuffelBlock) {
                knuffel = knuffel.setValue(HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.SOUTH);
            }
            guhs.add(new Valguh(SPRING_START + i / PER_TICK, uit, land, rng.nextFloat() * 360f, knuffel,
                    PARACHUTES[rng.nextInt(PARACHUTES.length)].defaultBlockState()));
        }
        Scorebord.show(level, a.wereld(new Vec3(MIDDEN + 0.5, BUS_Y + 2.6, LUIK_Z0 + 1)), "vadsnite_luik",
                Component.translatable("sign.guhs.vadsnite.luik").withStyle(ChatFormatting.YELLOW));
        ServerPlayer p = speler();
        if (p != null) {
            teller.addPlayer(p);
            stap(p, 1);
            zeg(p, "gui.guhs.vadsnite.begin");
        }
    }

    /** A free spot on the island (air above a solid block), else near the middle. */
    private Vec3 landplek(Arena a) {
        for (int poging = 0; poging < 10; poging++) {
            double hoek = rng.nextDouble() * Math.PI * 2, straal = 2 + Math.sqrt(rng.nextDouble()) * 9.5;
            double x = MIDDEN + 0.5 + Math.cos(hoek) * straal, z = MIDDEN + 0.5 + Math.sin(hoek) * straal;
            BlockPos op = a.wereld((int) Math.floor(x), EILAND_Y + 1, (int) Math.floor(z));
            if (a.level().getBlockState(op).isAir() && a.level().getBlockState(op.above()).isAir() && a.level().getBlockState(op.below()).isSolidRender()) {
                return a.wereld(new Vec3(x, EILAND_Y + 1, z));
            }
        }
        return a.wereld(new Vec3(MIDDEN + 0.5 + rng.nextDouble() * 2, EILAND_Y + 1, MIDDEN + 0.5 + rng.nextDouble() * 2));
    }

    /**
     * The bus "flies": it is a fixed part of the arena (it carries the player and the guhs), so the sky moves instead. While
     * the player is still on board, wisps of cloud rush past the windows on both sides and under the floor, from the nose
     * (the driver sits at the high z end) to the tail.
     */
    private void vaart(Arena a) {
        var rng = level().getRandom();
        for (int i = 0; i < 4; i++) {
            double x = switch (i) {
                case 0 -> 20.5 - rng.nextDouble() * 4;
                case 1 -> 28.5 + rng.nextDouble() * 4;
                default -> 20 + rng.nextDouble() * 9;
            };
            double y = i < 2 ? BUS_Y + 0.5 + rng.nextDouble() * 4 : BUS_Y - 1 - rng.nextDouble() * 3;
            Vec3 plek = a.wereld(new Vec3(x, y, 31 + rng.nextDouble() * 4));
            // (count 0: the three numbers are the particle's own speed)
            level().sendParticles(ParticleTypes.CLOUD, plek.x, plek.y, plek.z, 0, 0, 0, -1, 0.55 + rng.nextDouble() * 0.25);
        }
    }

    @Override
    protected void tick() {
        if (klaarTick()) {
            zzz();
            return;
        }
        ServerPlayer p = speler();
        if (p == null) {
            return;
        }
        int t = ticks();
        Arena a = arena();
        aftellen(p, t);
        for (Valguh g : guhs) {
            tickGuh(g, t);
        }
        if (!gesprongen && t % 2 == 0) {
            vaart(a);
        }
        // the player: out of the bus, floating down, landed
        double busVloer = a.oorsprong().getY() + BUS_Y;
        if (!gesprongen && (p.getY() < busVloer - 0.5 || t >= SPRING_START + DUW_NA)) {
            if (p.getY() >= busVloer - 0.5) {
                Vec3 onder = a.wereld(new Vec3(MIDDEN + 0.5, BUS_Y - 2, LUIK_Z0 + 1));
                p.teleportTo(level(), onder.x, onder.y, onder.z, Set.of(), p.getYRot(), p.getXRot(), true);
                zeg(p, "gui.guhs.vadsnite.duwtje");
            }
            gesprongen = true;
            stap(p, 2);
            stap(p, 3);
        }
        if (gesprongen && !geland) {
            if (!p.hasEffect(MobEffects.SLOW_FALLING)) {
                p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 60, 0, false, false, false));   // (the parachute)
            }
            p.resetFallDistance();
            if (p.onGround() && p.getY() < a.oorsprong().getY() + EILAND_Y + 6) {
                geland = true;
                p.removeEffect(MobEffects.SLOW_FALLING);
                wolkSinds = t;
                stap(p, 4);
                zeg(p, "gui.guhs.vadsnite.geland");
            }
        }
        if (ligt(p) && gesprongen) {
            afloop(p, Uitslag.GELIJKSPEL);
            return;
        }
        if (wolkSinds >= 0) {
            int w = t - wolkSinds;
            double straal = WOLK_VAN + (WOLK_TOT - WOLK_VAN) * Math.min(1.0, w / (double) WOLK_TICKS);
            if (t % 4 == 0) {
                wolk(a, straal, t);
            }
            Vec3 midden = a.wereld(new Vec3(MIDDEN + 0.5, EILAND_Y + 1, MIDDEN + 0.5));
            if (!wolkGezegd && w > 40 && Math.hypot(p.getX() - midden.x, p.getZ() - midden.z) > straal) {
                wolkGezegd = true;
                balk(p, "gui.guhs.vadsnite.wolk");
            }
            if (w >= WOLK_TICKS && alleGeland()) {
                afloop(p, Uitslag.OVERWINNING);
                return;
            }
        }
        zzz();
        toon(wakker());
    }

    private void aftellen(ServerPlayer p, int t) {
        if (t == 20 || t == 40 || t == 60) {
            PxGeluid.titel(p, Component.literal(String.valueOf(4 - t / 20)).withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                    Component.translatable("gui.guhs.vadsnite.aftellen").withStyle(ChatFormatting.LIGHT_PURPLE), 14);
        } else if (t == SPRING_START) {
            stap(p, 2);
            PxGeluid.titel(p, Component.translatable("gui.guhs.vadsnite.springen").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable("gui.guhs.vadsnite.springen.onder").withStyle(ChatFormatting.LIGHT_PURPLE), 40);
            level().playSound(null, p.blockPosition(), Grap1Slice.VADSNITE_SPRONG.get(), SoundSource.PLAYERS, 1f, 1f);
        }
    }

    private boolean alleGeland() {
        for (Valguh g : guhs) {
            if (g.guh == null || ticks() < g.sprong + 2 + 2 * GLIJ) {
                return false;
            }
        }
        return true;
    }

    private void tickGuh(Valguh g, int t) {
        int d = t - g.sprong;
        if (d < 0 || g.slaapt) {
            return;
        }
        if (d == 0) {
            g.guh = scherm(g.knuffel, g.uit, g.yaw, 1.6f, 0f, 1.6f);
            g.scherm = scherm(g.parachute, g.uit, g.yaw, 2.2f, 2.1f, 0.45f);
        } else if (d == 2) {
            verplaats(g, g.half);
        } else if (d == 2 + GLIJ) {
            verplaats(g, g.land);
        } else if (d == 2 + 2 * GLIJ) {
            if (g.scherm != null) {
                g.scherm.discard();
                g.scherm = null;
            }
            // everyone falls asleep at once; only the very last guh keeps its eyes open until the slaapwolk is there
            if (g != guhs.get(guhs.size() - 1)) {
                slaap(g);
            }
        }
    }

    private void slaap(Valguh g) {
        g.slaapt = true;
        if (g.guh != null && !g.guh.isRemoved()) {
            g.guh.snapTo(g.land.x, g.land.y + 0.45, g.land.z, g.yaw, 80f);   // (flat on its face: asleep before it hit the grass)
        }
    }

    private static void verplaats(Valguh g, Vec3 naar) {
        if (g.guh != null) {
            g.guh.setPos(naar.x, naar.y, naar.z);
        }
        if (g.scherm != null) {
            g.scherm.setPos(naar.x, naar.y, naar.z);
        }
    }

    /** A block display (the cheap "guh" or its parachute): scaled around its own middle, lifted, gliding when moved. */
    @Nullable
    private Entity scherm(BlockState blok, Vec3 pos, float yaw, float breed, float omhoog, float hoog) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:block_display");
        tag.put("block_state", NbtUtils.writeBlockState(blok));
        tag.put("transformation", Transformation.EXTENDED_CODEC.encodeStart(NbtOps.INSTANCE, new Transformation(
                new Vector3f(-breed / 2, omhoog, -breed / 2), new Quaternionf(), new Vector3f(breed, hoog, breed), new Quaternionf())).getOrThrow());
        tag.putInt("teleport_duration", GLIJ);
        tag.putFloat("view_range", 2f);
        ListTag tags = new ListTag();
        tags.add(StringTag.valueOf(TAG));
        tag.put("Tags", tags);
        ServerLevel level = level();
        Entity e = EntityType.loadEntityRecursive(tag, level, EntitySpawnReason.LOAD, x -> {
            x.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
            return x;
        });
        if (e != null) {
            level.addFreshEntity(e);
        }
        return e;
    }

    private void wolk(Arena a, double straal, int t) {
        ServerLevel level = level();
        Vec3 midden = a.wereld(new Vec3(MIDDEN + 0.5, EILAND_Y + 1.4, MIDDEN + 0.5));
        int n = (int) Math.max(10, straal * 2.2);
        for (int i = 0; i < n; i++) {
            double hoek = (i + (t % 8) / 8.0) * Math.PI * 2 / n;
            level.sendParticles(ROZE, midden.x + Math.cos(hoek) * straal, midden.y + (i % 3) * 0.7, midden.z + Math.sin(hoek) * straal, 1, 0.15, 0.2, 0.15, 0.0);
        }
    }

    private void zzz() {
        if (ticks() % 6 != 0 || guhs.isEmpty()) {
            return;
        }
        Valguh g = guhs.get(rng.nextInt(guhs.size()));
        if (g.slaapt && g.guh != null) {
            level().sendParticles(EmotesFeature.GUH_ZZZ.get(), g.land.x, g.land.y + 1.2, g.land.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    private void toon(int n) {
        if (n != getoond) {
            getoond = n;
            teller.setName(tellerTekst(n));
            teller.setProgress(Math.max(0f, Math.min(1f, n / (float) (GUHS + 1))));
        }
    }

    private void afloop(ServerPlayer p, Uitslag hoe) {
        uitslag = hoe;
        for (Valguh g : guhs) {
            if (g.scherm != null) {
                g.scherm.discard();
                g.scherm = null;
            }
            if (!g.slaapt) {
                slaap(g);   // (in the air: asleep on the grass at once; not jumped yet: asleep in the bus)
            }
        }
        stap(p, STAPPEN);
        toon(hoe == Uitslag.OVERWINNING ? 1 : 0);
        level().playSound(null, p.blockPosition(), Grap1Slice.VADSNITE_OVERWINNING.get(), SoundSource.PLAYERS, 1f, hoe == Uitslag.OVERWINNING ? 1f : 0.8f);
        if (hoe == Uitslag.GELIJKSPEL) {
            // (a draw counts all the same, but it has its own title)
            clou(p, Component.empty());
            gelijkspelTitel(p);
            zeg(p, "gui.guhs.vadsnite.einde.gelijkspel");
        } else {
            clou(p, Component.translatable("gui.guhs.vadsnite.clou.onder"));
            zeg(p, "gui.guhs.vadsnite.einde.overwinning", GUHS);
        }
        straksKlaar(EIND_TICKS);
    }

    private static void gelijkspelTitel(ServerPlayer p) {
        PxGeluid.titel(p, Component.translatable("gui.guhs.vadsnite.gelijkspel").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Component.translatable("gui.guhs.vadsnite.gelijkspel.onder").withStyle(ChatFormatting.WHITE), 70);
    }

    /** Back in the lobby the kern shows the victory title with the reward: after a draw the draw's own title comes back. */
    @Override
    protected void beloond(ServerPlayer p, boolean eerste) {
        if (uitslag == Uitslag.GELIJKSPEL) {
            gelijkspelTitel(p);
        }
    }

    @Override
    public Vec3 terugzetPlek(ServerPlayer p) {
        // missed the island: back onto it (not back into the bus)
        return gesprongen ? arena().wereld(new Vec3(MIDDEN + 0.5, EILAND_Y + 1, MIDDEN + 2.5)) : super.terugzetPlek(p);
    }

    @Override
    public boolean magBreken(ServerPlayer p, BlockPos pos, BlockState s) {
        if (!afgelopen) {
            balk(p, "gui.guhs.vadsnite.nee.breken");
        }
        return false;
    }

    @Override
    protected void spelerWeg(ServerPlayer p, Vertrek reden) {
        super.spelerWeg(p, reden);
        p.removeEffect(MobEffects.SLOW_FALLING);
        teller.removePlayer(p);
    }

    @Override
    protected void einde() {
        teller.removeAllPlayers();
        for (Valguh g : guhs) {
            if (g.guh != null) {
                g.guh.discard();
            }
            if (g.scherm != null) {
                g.scherm.discard();
            }
        }
        guhs.clear();
    }
}
