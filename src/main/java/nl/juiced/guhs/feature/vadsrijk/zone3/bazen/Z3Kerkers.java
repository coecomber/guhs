package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.werf.Taleggio;

/**
 * The dungeon rooms (the structures come from z3-wereld; this is the mechanics):
 * <ul>
 *   <li>Kaaszeppelinwerf: De Gieterij (fondue gutters: red lanes, then a burning splash), De Bewapeningsloods (3 levers turn 3
 *       cannons; lever i turns cannon i and i+1; all aimed at the gate = they blow it open), and a safety net under the yard
 *       (fall below it = back at the entrance, no damage).</li>
 *   <li>Hart van de Boom: De Aderloop (every heartbeat the light goes out for 1.5 s: don't move in the dark), De Schimmelkwekerij
 *       (3 mould kettles: burn them out with right-clicks; they puff spores; all 3 out = the gate opens), De Ringenzaal (3 year
 *       rings; knob i turns ring i and i+1; the pink veins in one line = the gate opens).</li>
 * </ul>
 * Everything resets after 60 s with nobody in the room. Blocks only go into air (or our own), through Herstel.
 */
public final class Z3Kerkers {
    public static final String H_LOODS = "z3_werf_loods", H_KWEKERIJ = "z3_hart_kwekerij", H_RINGEN = "z3_hart_ringen";
    /** Bewapeningsloods: lever / cannon offsets from LOODS, the start stands (solution: lever presses 1, 0, 2). */
    public static final int[][] HENDELS = {{-4, 0, 0}, {0, 0, 0}, {4, 0, 0}}, KANONNEN = {{-4, 0, 5}, {0, 0, 5}, {4, 0, 5}};
    public static final int[] LOODS_START = {1, 3, 2};
    /** Ringenzaal: start stands (solution: knob presses 1, 2, 4). */
    public static final int[] RINGEN_START = {3, 5, 2};

    private static final int[] kanonStand = LOODS_START.clone(), ringStand = RINGEN_START.clone();
    private static boolean loodsOpgezet, loodsOpen, ringenOpgezet, ringenOpen, kwekerijOpen;
    private static int loodsLeeg, ringenLeeg, kwekerijLeeg, gieterijT, aderT, sporenT;
    private static final List<Z3Figurant> KANON_FIGUREN = new ArrayList<>();
    private static final List<Z3Doel> KETELS = new ArrayList<>();
    private static final Map<UUID, Vec3> ADER_STAND = new HashMap<>();
    private static final Map<UUID, Long> KLIK_CD = new HashMap<>();

    private Z3Kerkers() {
    }

    // ---- the rooms' boxes ---------------------------------------------------------------------------------------------------

    public static boolean inWerf(double x, double y, double z) {
        return x >= Z3BazenPlekken.WERF_X0 && x <= Z3BazenPlekken.WERF_X1 && z >= Z3BazenPlekken.WERF_Z0 && z <= Z3BazenPlekken.WERF_Z1;
    }

    private static boolean inBox(ServerPlayer p, BlockPos a, BlockPos b, int marge) {
        return p.getX() >= Math.min(a.getX(), b.getX()) - marge && p.getX() <= Math.max(a.getX(), b.getX()) + 1 + marge
                && p.getZ() >= Math.min(a.getZ(), b.getZ()) - marge && p.getZ() <= Math.max(a.getZ(), b.getZ()) + 1 + marge
                && p.getY() >= Math.min(a.getY(), b.getY()) - 3 && p.getY() <= Math.max(a.getY(), b.getY()) + 6;
    }

    private static boolean bij(ServerPlayer p, BlockPos c, double r) {
        return p.position().distanceToSqr(Vec3.atBottomCenterOf(c)) < r * r;
    }

    private static void kamerSchade(ServerPlayer p, double fractie, Element e, String skill) {
        Schade.doe(new Schade.Klap(null, p, Spelers.maxVads(p) * fractie, e, Schade.Soort.WAAR, false, skill));
    }

    /** A gate: a 3 x 4 plane (x -1..1) of gate blocks at this spot, only into air / our own. */
    private static void poort(ServerLevel sl, String key, BlockPos b, boolean dicht) {
        BlockState s = dicht ? Z3BazenRegistry.POORT.get().defaultBlockState() : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy < 4; dy++) {
                BlockPos q = b.offset(dx, dy, 0);
                BlockState nu = sl.getBlockState(q);
                if (nu.isAir() || nu.is(Z3BazenRegistry.POORT.get())) {
                    Herstel.zet(sl, key, q, s);
                }
            }
        }
    }

    // ---- the tick -----------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || server.getTickCount() % 2 != 0) {
            return;
        }
        List<ServerPlayer> spelers = sl.players();
        if (spelers.isEmpty()) {
            return;
        }
        werf(sl, spelers);
        hart(sl, spelers);
    }

    private static void werf(ServerLevel sl, List<ServerPlayer> spelers) {
        boolean loods = false, gieterij = false;
        for (ServerPlayer p : spelers) {
            if (p.isSpectator()) {
                continue;
            }
            // the safety net under the yard
            if (inWerf(p.getX(), p.getY(), p.getZ()) && p.getY() < Z3BazenPlekken.WERF_Y0 - 6 && p.getY() > Z3BazenPlekken.WERF_Y0 - 60) {
                BlockPos in = Z3BazenPlekken.WERF_INGANG;
                p.teleportTo(sl, in.getX() + 0.5, in.getY() + 0.1, in.getZ() + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), true);
                p.resetFallDistance();
                p.sendSystemMessage(Component.translatable("vadsrijk.z3bazen.werf.net").withStyle(ChatFormatting.AQUA));
            }
            loods |= bij(p, Z3BazenPlekken.LOODS, 24);
            gieterij |= inBox(p, Z3BazenPlekken.GIETERIJ_A, Z3BazenPlekken.GIETERIJ_B, 0);
        }
        // De Gieterij: fondue splashes along the gutters
        if (gieterij && (gieterijT += 2) >= 100) {
            gieterijT = 0;
            BlockPos a = Z3BazenPlekken.GIETERIJ_A, b = Z3BazenPlekken.GIETERIJ_B;
            int x0 = Math.min(a.getX(), b.getX()), x1 = Math.max(a.getX(), b.getX()), z0 = Math.min(a.getZ(), b.getZ()), z1 = Math.max(a.getZ(), b.getZ());
            List<Integer> banen = new ArrayList<>();
            for (int z = z0 + 2; z <= z1 - 1; z += 4) {
                banen.add(z);
            }
            java.util.Collections.shuffle(banen, new java.util.Random(sl.getGameTime()));
            int y = a.getY();
            for (int z : banen.subList(0, Math.min(banen.size(), Math.max(1, banen.size() / 2)))) {
                Vec3 va = new Vec3(x0, y, z + 0.5), vb = new Vec3(x1 + 1, y, z + 0.5);
                Telegraaf.lijn(sl, va, vb, 2.4, 40, Z3Baas.ROOD);
                Z3Scenes.loop(t -> {
                    if (t < 40) {
                        return false;
                    }
                    for (double x = va.x; x <= vb.x; x += 1.5) {
                        sl.sendParticles(ParticleTypes.LAVA, x, y + 0.3, z + 0.5, 1, 0.3, 0.2, 0.3, 0);
                        sl.sendParticles(ParticleTypes.FLAME, x, y + 0.5, z + 0.5, 2, 0.3, 0.3, 0.3, 0.02);
                    }
                    for (ServerPlayer p : sl.players()) {
                        if (Math.abs(p.getZ() - (z + 0.5)) < 1.3 && p.getX() >= va.x - 0.5 && p.getX() <= vb.x + 0.5 && Math.abs(p.getY() - y) < 2.5) {
                            kamerSchade(p, 0.2, Element.VUUR, "z3_fondue");
                            Statussen.geef(p, Status.BRANDEND, 60, 3, null);
                        }
                    }
                    sl.playSound(null, (va.x + vb.x) / 2, y, z, SoundEvents.LAVA_POP, SoundSource.BLOCKS, 2f, 0.6f);
                    return true;
                });
            }
        }
        // De Bewapeningsloods
        if (loods) {
            loodsLeeg = 0;
            if (!loodsOpgezet) {
                loodsOpzetten(sl);
            }
        } else if (loodsOpgezet && (loodsLeeg += 2) >= 1200) {
            loodsReset(sl);
        }
    }

    private static void loodsOpzetten(ServerLevel sl) {
        loodsOpgezet = true;
        loodsOpen = false;
        System.arraycopy(LOODS_START, 0, kanonStand, 0, 3);
        Herstel.terug(sl, H_LOODS);
        BlockPos c = Z3BazenPlekken.LOODS;
        for (int i = 0; i < 3; i++) {
            BlockPos h = c.offset(HENDELS[i][0], HENDELS[i][1], HENDELS[i][2]);
            BlockState s = sl.getBlockState(h);
            if (s.isAir() || s.canBeReplaced() || s.getBlock() instanceof Z3Blokken.Hendel) {
                Herstel.zet(sl, H_LOODS, h, Z3BazenRegistry.HENDEL.get().defaultBlockState().setValue(Z3Blokken.Hendel.STAND, kanonStand[i]));
            }
        }
        KANON_FIGUREN.forEach(f -> f.discard());
        KANON_FIGUREN.clear();
        for (int i = 0; i < 3; i++) {
            Z3Figurant f = Z3Figurant.maak(sl, Vec3.atBottomCenterOf(c.offset(KANONNEN[i][0], KANONNEN[i][1], KANONNEN[i][2])), kanonStand[i] * 90f,
                    "vr_z3_baas_kanon", "textures/vadsrijk/z3bazen/kanon.png", "vr_z3_baas_kanon", "idle", 1.5f);
            if (f != null) {
                KANON_FIGUREN.add(f);
            }
        }
        poort(sl, H_LOODS, Z3BazenPlekken.LOODS_POORT, true);
    }

    private static void loodsReset(ServerLevel sl) {
        loodsOpgezet = false;
        KANON_FIGUREN.forEach(f -> f.discard());
        KANON_FIGUREN.clear();
        Herstel.terug(sl, H_LOODS);
    }

    /** The cannons all aim at the gate (stand 0 = toward +z, where the gate is). */
    public static boolean loodsOpgelost() {
        return kanonStand[0] == 0 && kanonStand[1] == 0 && kanonStand[2] == 0;
    }

    public static int[] kanonStanden() {
        return kanonStand.clone();
    }

    /** A lever was pulled (the Bewapeningsloods; Taleggio's rope levers go to him). */
    static void hendel(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        Arena a = Arenas.op(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        if (a != null && Arenas.baas(sl, a.id()) instanceof Taleggio t) {
            t.touw(sl, pos, p);
            return;
        }
        BlockPos c = Z3BazenPlekken.LOODS;
        for (int i = 0; i < 3; i++) {
            if (c.offset(HENDELS[i][0], HENDELS[i][1], HENDELS[i][2]).equals(pos)) {
                Tips.toon(p, "vadsrijk.z3bazen.tip.loods");
                draaiLoods(sl, i);
                return;
            }
        }
    }

    /** Lever i turns cannon i and i+1 a quarter (public for the gametest). */
    public static void draaiLoods(ServerLevel sl, int i) {
        if (!loodsOpgezet) {
            loodsOpzetten(sl);
        }
        if (loodsOpen) {
            return;
        }
        for (int k : new int[] {i, (i + 1) % 3}) {
            kanonStand[k] = (kanonStand[k] + 1) % 4;
            if (k < KANON_FIGUREN.size()) {
                KANON_FIGUREN.get(k).setYRot(kanonStand[k] * 90f);
            }
        }
        BlockPos c = Z3BazenPlekken.LOODS;
        BlockPos h = c.offset(HENDELS[i][0], HENDELS[i][1], HENDELS[i][2]);
        if (sl.getBlockState(h).getBlock() instanceof Z3Blokken.Hendel) {
            sl.setBlock(h, sl.getBlockState(h).setValue(Z3Blokken.Hendel.STAND, kanonStand[i]), 3);
        }
        sl.playSound(null, c, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 1f, 0.8f);
        if (loodsOpgelost()) {
            loodsOpen = true;
            for (Z3Figurant f : KANON_FIGUREN) {
                f.anim("1:schiet");
                sl.sendParticles(ParticleTypes.EXPLOSION, f.getX(), f.getY() + 1, f.getZ(), 2, 0.3, 0.3, 0.3, 0);
            }
            sl.playSound(null, c, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2f, 0.9f);
            BlockPos poort = Z3BazenPlekken.LOODS_POORT;
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, poort.getX() + 0.5, poort.getY() + 2, poort.getZ() + 0.5, 1, 0, 0, 0, 0);
            poort(sl, H_LOODS, poort, false);
            for (ServerPlayer q : sl.players()) {
                if (bij(q, c, 30)) {
                    q.sendSystemMessage(Component.translatable("vadsrijk.z3bazen.werf.loods_open").withStyle(ChatFormatting.GREEN));
                }
            }
        }
    }

    // ---- Het Hart van de Boom -------------------------------------------------------------------------------------------------

    private static void hart(ServerLevel sl, List<ServerPlayer> spelers) {
        boolean ader = false, kwekerij = false, ringen = false;
        List<ServerPlayer> inAder = new ArrayList<>();
        for (ServerPlayer p : spelers) {
            if (p.isSpectator()) {
                continue;
            }
            if (inBox(p, Z3BazenPlekken.ADERLOOP_A, Z3BazenPlekken.ADERLOOP_B, 0)) {
                ader = true;
                inAder.add(p);
            }
            kwekerij |= bij(p, Z3BazenPlekken.KWEKERIJ, 22);
            ringen |= bij(p, Z3BazenPlekken.RINGEN, 22);
        }
        // De Aderloop: in the dark, stand still
        if (ader) {
            aderT += 2;
            if (aderT == 70) {
                for (ServerPlayer p : inAder) {
                    p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false));
                    Z3Vast.geluid(p, SoundEvents.WARDEN_HEARTBEAT, 2f, 0.6f);
                    ADER_STAND.put(p.getUUID(), p.position());
                    Tips.toon(p, "vadsrijk.z3bazen.tip.aderloop");
                }
            }
            if (aderT >= 100) {
                aderT = 0;
                for (ServerPlayer p : inAder) {
                    Vec3 v = ADER_STAND.remove(p.getUUID());
                    if (v != null && p.position().distanceTo(v) > 0.7) {
                        kamerSchade(p, 0.15, Element.NATUUR, "z3_aderloop");
                        p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.hart.bewogen").withStyle(ChatFormatting.RED));
                    }
                }
                ADER_STAND.clear();
            }
        }
        // De Schimmelkwekerij
        if (kwekerij) {
            kwekerijLeeg = 0;
            if (KETELS.isEmpty() && !kwekerijOpen) {
                kwekerijOpzetten(sl);
            }
            if ((sporenT += 2) >= 80) {
                sporenT = 0;
                List<Z3Doel> heel = KETELS.stream().filter(k -> !k.isRemoved() && k.vads() > 0.01).toList();
                if (!heel.isEmpty()) {
                    Z3Doel k = heel.get(sl.getRandom().nextInt(heel.size()));
                    Vec3 v = k.position();
                    Telegraaf.cirkel(sl, v, 4, 30, Z3Baas.PAARS);
                    Z3Scenes.loop(t -> {
                        if (t < 30) {
                            return false;
                        }
                        sl.sendParticles(new DustParticleOptions(0x7A8AA0, 2f), v.x, v.y + 1, v.z, 60, 2, 0.6, 2, 0.02);
                        for (ServerPlayer p : sl.players()) {
                            if (p.position().distanceToSqr(v.x, p.getY(), v.z) < 16 && Math.abs(p.getY() - v.y) < 3) {
                                kamerSchade(p, 0.1, Element.WATER, "z3_sporen");
                                Statussen.geef(p, Status.GESPOREN, 80, 3, null);
                            }
                        }
                        return true;
                    });
                }
            }
        } else if ((!KETELS.isEmpty() || kwekerijOpen) && (kwekerijLeeg += 2) >= 1200) {
            KETELS.forEach(k -> k.discard());
            KETELS.clear();
            kwekerijOpen = false;
            Herstel.terug(sl, H_KWEKERIJ);
        }
        // De Ringenzaal
        if (ringen) {
            ringenLeeg = 0;
            if (!ringenOpgezet) {
                ringenOpzetten(sl);
            }
        } else if (ringenOpgezet && (ringenLeeg += 2) >= 1200) {
            ringenOpgezet = false;
            ringenTerug(sl);
        }
    }

    private static void kwekerijOpzetten(ServerLevel sl) {
        Herstel.terug(sl, H_KWEKERIJ);
        kwekerijOpen = false;
        Z3Doel.Doelhouder kamer = new Z3Doel.Doelhouder() {
            @Override
            public boolean magRaken(Player p) {
                return true;
            }

            @Override
            public void kapot(Z3Doel d, @Nullable LivingEntity bron) {
            }

            @Override
            public void gebruikt(Z3Doel d, ServerPlayer p) {
                brand(sl, d, p);
            }

            @Override
            public boolean bestaat() {
                return true;
            }
        };
        for (int i = 0; i < Z3BazenPlekken.KWEKERIJ_KETELS.length; i++) {
            Z3Doel k = Z3BazenRegistry.KETEL.get().create(sl, EntitySpawnReason.EVENT);
            if (k != null) {
                k.koppel(kamer, i, 49, 3, Vec3.atBottomCenterOf(Z3BazenPlekken.op(Z3BazenPlekken.KWEKERIJ, Z3BazenPlekken.KWEKERIJ_KETELS[i])));
                k.zetRaakbaar(false);
                sl.addFreshEntity(k);
                KETELS.add(k);
            }
        }
        poort(sl, H_KWEKERIJ, Z3BazenPlekken.KWEKERIJ_POORT, true);
    }

    /** Right-click a kettle: burn it a third out (with the Nestlantaarn's flame, or any hand). */
    public static void brand(ServerLevel sl, Z3Doel k, ServerPlayer p) {
        long nu = sl.getGameTime();
        if (nu < KLIK_CD.getOrDefault(p.getUUID(), 0L) || k.vads() <= 0.01) {
            return;
        }
        KLIK_CD.put(p.getUUID(), nu + 20);
        Tips.toon(p, "vadsrijk.z3bazen.tip.ketel");
        k.zetVads(k.vads() - 1);
        sl.sendParticles(ParticleTypes.FLAME, k.getX(), k.getY() + 1.2, k.getZ(), 20, 0.4, 0.4, 0.4, 0.03);
        sl.playSound(null, k.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 1f, 1f);
        if (k.vads() <= 0.01) {
            Z3Baas.pluis(sl, k.position().add(0, 1, 0), 1.2, 1.4);
            k.setInvisible(true);
            p.sendOverlayMessage(Component.translatable("vadsrijk.z3bazen.hart.ketel_uit").withStyle(ChatFormatting.GREEN));
            if (KETELS.stream().allMatch(x -> x.vads() <= 0.01)) {
                kwekerijOpen = true;
                poort(sl, H_KWEKERIJ, Z3BazenPlekken.KWEKERIJ_POORT, false);
                for (ServerPlayer q : sl.players()) {
                    if (bij(q, Z3BazenPlekken.KWEKERIJ, 30)) {
                        q.sendSystemMessage(Component.translatable("vadsrijk.z3bazen.hart.kwekerij_open").withStyle(ChatFormatting.GREEN));
                    }
                }
            }
        }
    }

    public static List<Z3Doel> ketels() {
        return KETELS;
    }

    public static boolean kwekerijOpen() {
        return kwekerijOpen;
    }

    // ---- De Ringenzaal --------------------------------------------------------------------------------------------------------

    public static BlockPos knop(int i) {
        return Z3BazenPlekken.RINGEN.offset(Z3BazenPlekken.RING_STRALEN[2] + 3, 0, -2 + 2 * i);
    }

    private static BlockPos nerf(int ring, int stand) {
        double a = stand * Math.PI / 4, r = Z3BazenPlekken.RING_STRALEN[ring];
        return Z3BazenPlekken.RINGEN.offset((int) Math.round(Math.cos(a) * r), -1, (int) Math.round(Math.sin(a) * r));
    }

    private static void ringenOpzetten(ServerLevel sl) {
        ringenOpgezet = true;
        ringenOpen = false;
        System.arraycopy(RINGEN_START, 0, ringStand, 0, 3);
        ringenTerug(sl);
        for (int i = 0; i < 3; i++) {
            BlockPos k = knop(i);
            BlockState s = sl.getBlockState(k);
            if (s.isAir() || s.canBeReplaced() || s.getBlock() instanceof Z3Blokken.Ringknop) {
                Herstel.zet(sl, H_RINGEN, k, Z3BazenRegistry.RINGKNOP.get().defaultBlockState().setValue(Z3Blokken.Ringknop.STAND, ringStand[i]));
            }
            Herstel.zet(sl, H_RINGEN + "_" + i, nerf(i, ringStand[i]), Z3BazenRegistry.NERF.get().defaultBlockState());
        }
        poort(sl, H_RINGEN, Z3BazenPlekken.RINGEN_POORT, true);
    }

    public static boolean ringenOpgelost() {
        return ringStand[0] == 0 && ringStand[1] == 0 && ringStand[2] == 0;
    }

    public static int[] ringStanden() {
        return ringStand.clone();
    }

    static void ringknop(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        for (int i = 0; i < 3; i++) {
            if (knop(i).equals(pos)) {
                Tips.toon(p, "vadsrijk.z3bazen.tip.ringen");
                draaiRing(sl, i);
                return;
            }
        }
    }

    /** Knob i turns ring i and ring i+1 an eighth (public for the gametest). */
    public static void draaiRing(ServerLevel sl, int i) {
        if (!ringenOpgezet) {
            ringenOpzetten(sl);
        }
        if (ringenOpen) {
            return;
        }
        for (int k : new int[] {i, (i + 1) % 3}) {
            Herstel.terug(sl, H_RINGEN + "_" + k);
            ringStand[k] = (ringStand[k] + 1) % 8;
            Herstel.zet(sl, H_RINGEN + "_" + k, nerf(k, ringStand[k]), Z3BazenRegistry.NERF.get().defaultBlockState());
            BlockPos kb = knop(k);
            if (sl.getBlockState(kb).getBlock() instanceof Z3Blokken.Ringknop) {
                sl.setBlock(kb, sl.getBlockState(kb).setValue(Z3Blokken.Ringknop.STAND, ringStand[k]), 3);
            }
        }
        sl.playSound(null, Z3BazenPlekken.RINGEN, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 1.5f, 0.6f);
        if (ringenOpgelost()) {
            ringenOpen = true;
            Vec3 c = Vec3.atBottomCenterOf(Z3BazenPlekken.RINGEN);
            for (double s = 0; s <= Z3BazenPlekken.RING_STRALEN[2]; s += 0.5) {
                sl.sendParticles(new DustParticleOptions(Z3Baas.ROZE, 2f), c.x + s, c.y + 0.2, c.z, 3, 0.1, 0.2, 0.1, 0);
            }
            sl.playSound(null, Z3BazenPlekken.RINGEN, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 2f, 1f);
            poort(sl, H_RINGEN, Z3BazenPlekken.RINGEN_POORT, false);
            for (ServerPlayer q : sl.players()) {
                if (bij(q, Z3BazenPlekken.RINGEN, 30)) {
                    q.sendSystemMessage(Component.translatable("vadsrijk.z3bazen.hart.ringen_open").withStyle(ChatFormatting.GREEN));
                }
            }
        }
    }

    /** The puzzle rule (both rooms): knob/lever i turns piece i and i+1 one step (mod {@code stappen}). Pure: for the gametest. */
    public static int[] druk(int[] stand, int i, int stappen) {
        int[] s = stand.clone();
        s[i] = (s[i] + 1) % stappen;
        s[(i + 1) % s.length] = (s[(i + 1) % s.length] + 1) % stappen;
        return s;
    }

    /** Dev/tests: everything back to its start. */
    public static void reset(ServerLevel sl) {
        loodsReset(sl);
        KETELS.forEach(k -> k.discard());
        KETELS.clear();
        kwekerijOpen = false;
        Herstel.terug(sl, H_KWEKERIJ);
        ringenOpgezet = false;
        ringenTerug(sl);
    }

    private static void ringenTerug(ServerLevel sl) {
        Herstel.terug(sl, H_RINGEN);
        for (int i = 0; i < 3; i++) {
            Herstel.terug(sl, H_RINGEN + "_" + i);
        }
    }

    /** Is this an arena boss that belongs to a dungeon room? (for the arena list) */
    @Nullable
    static VadsrijkBaas baas(ServerLevel sl, String arena) {
        return Arenas.baas(sl, arena);
    }
}
