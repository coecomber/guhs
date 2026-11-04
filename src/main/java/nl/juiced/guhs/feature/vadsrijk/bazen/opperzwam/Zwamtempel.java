package nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.bazen.Adapters;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenPlekken;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.bazen.Herstel;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * The Zwamtempel dungeon before the Opperzwam (shared world, no instances: every room resets itself 10 min after it was solved,
 * as soon as nobody stands in it):
 * <ol>
 *   <li>The temple-door ceremony: walk up to the door (level {@link BazenPlekken#TEMPEL_MIN_LEVEL}+, or {@link #MAG_NAAR_BINNEN}),
 *       a short camera scene + title card, the door slides open (wereld's WereldDeuren if present, else we slide the door blocks
 *       away with {@link Herstel}); it closes again 5 min later when nobody is near.</li>
 *   <li>Lichtzwamkamer: 5 Lichtzwammen flash in an order; light them in that order. Wrong = all out, shown again.</li>
 *   <li>Hendelkamer: 4 levers; the hint mushrooms on the far wall show which must be ON, mirrored. Step on the plate to check.</li>
 *   <li>Ridderzaal: 3 waves of Tempelzwam-ridders (+ a Sporenpriester elite in the last), gates closed until they're cheered up.</li>
 * </ol>
 * The gates between rooms are our own Zwampoort blocks (only placed in air, only removed where they are ours).
 */
public final class Zwamtempel {
    /**
     * Who may open the temple (fix round 1): level {@link BazenPlekken#TEMPEL_MIN_LEVEL}+ AND the main story is at the temple
     * (quest {@link #QUEST_TEMPEL} active or done, or the Opperzwam quest after it). Creative is no exception (dev:
     * {@code /guhs vadsrijk bazen tempel open}).
     */
    public static Predicate<ServerPlayer> MAG_NAAR_BINNEN = p -> waaromDicht(p) == null;
    public static final String QUEST_TEMPEL = "hq10_zwamtempel", QUEST_OPPERZWAM = "hq11_opperzwam";

    /** (fix round 2, g-ux) From this level a right-click on the door ALWAYS opens it, whatever the quest state. */
    public static final int TEMPEL_ALTIJD_LEVEL = 18;

    /**
     * Null when this player may open the temple door, else the lang key of the friendly "not yet" line (args: the story level
     * {@link BazenPlekken#TEMPEL_MIN_LEVEL}, the player's level, {@link #TEMPEL_ALTIJD_LEVEL}). Rule (g-ux): level 18+ always;
     * level 16+ with the temple quest (active/done) or the Opperzwam quest.
     */
    @javax.annotation.Nullable
    public static String waaromDicht(ServerPlayer p) {
        if (Spelers.level(p) >= TEMPEL_ALTIJD_LEVEL) {
            return null;
        }
        if (Spelers.level(p) < BazenPlekken.TEMPEL_MIN_LEVEL) {
            return "vadsrijk.bazen.tempel.te_laag";
        }
        boolean verhaal;
        try {
            verhaal = nl.juiced.guhs.feature.vadsrijk.npc.Questlog.actief(p, QUEST_TEMPEL) || nl.juiced.guhs.feature.vadsrijk.npc.Questlog.klaar(p, QUEST_TEMPEL)
                    || nl.juiced.guhs.feature.vadsrijk.npc.Questlog.actief(p, QUEST_OPPERZWAM) || nl.juiced.guhs.feature.vadsrijk.npc.Questlog.klaar(p, QUEST_OPPERZWAM);
        } catch (RuntimeException | LinkageError e) {
            verhaal = true;   // (no quest system: the level is enough)
        }
        return verhaal ? null : "vadsrijk.bazen.tempel.verhaal";
    }

    /** Does this block belong to the temple entrance (the moving door, or anything openable right around it)? */
    public static boolean bijDeur(BlockPos pos) {
        BlockPos a = BazenPlekken.TEMPELDEUR_MIN, b = BazenPlekken.TEMPELDEUR_MAX;
        return pos.getX() >= Math.min(a.getX(), b.getX()) - 3 && pos.getX() <= Math.max(a.getX(), b.getX()) + 3 && pos.getY() >= Math.min(a.getY(), b.getY()) - 1
                && pos.getY() <= Math.max(a.getY(), b.getY()) + 1 && pos.getZ() >= Math.min(a.getZ(), b.getZ()) - 4 && pos.getZ() <= Math.max(a.getZ(), b.getZ()) + 4;
    }

    /**
     * A right-click at the temple entrance (BazenRegistry listens before wereld's door click): below the gate a friendly line
     * and nothing opens (also no vanilla door/gate/trapdoor there); allowed = the ceremony. Returns true when handled.
     */
    public static boolean klikDeur(ServerPlayer p, BlockPos pos) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl) || !bijDeur(pos)) {
            return false;
        }
        String waarom = waaromDicht(p);
        if (waarom != null) {
            p.sendOverlayMessage(dichtTekst(p, waarom));
            sl.playSound(null, pos, SoundEvents.WOODEN_DOOR_CLOSE, SoundSource.BLOCKS, 0.6f, 0.5f);
            return true;
        }
        BlockState st = sl.getBlockState(pos);
        boolean deurBlok = st.getBlock() instanceof net.minecraft.world.level.block.DoorBlock || st.getBlock() instanceof net.minecraft.world.level.block.TrapDoorBlock
                || st.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock;
        if (deurBlok) {
            return true;   // (vanilla doors in the doorway never open: the temple door is the way in)
        }
        try {
            nl.juiced.guhs.feature.vadsrijk.wereld.WereldDeuren.ontgrendel("zwamtempel", p);
        } catch (RuntimeException | LinkageError e) {
            // (no wereld doors)
        }
        if (deurOpenTot < 0 && deurAnim < 0) {
            ceremonie(sl, sl.getGameTime());
        } else {
            meldDeurOpen(p);   // (already open: the quest step "click the big door" still counts)
        }
        return true;
    }

    /** The line below the gate: what is needed (gold so it reads as advice, not as an error). */
    static Component dichtTekst(ServerPlayer p, String key) {
        return Component.translatable(key, BazenPlekken.TEMPEL_MIN_LEVEL, Spelers.level(p), TEMPEL_ALTIJD_LEVEL).withStyle(ChatFormatting.GOLD);
    }

    /** Tells the quest system this player opened (or walked through) the temple door: hq11's first step. */
    static void meldDeurOpen(ServerPlayer p) {
        try {
            nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.meld(p, "zwamtempel", "deur");
        } catch (RuntimeException | LinkageError e) {
            // (no quest system)
        }
    }
    public static final int RESET_TICKS = 20 * 60 * 10, DEUR_OPEN_TICKS = 20 * 60 * 5;
    private static final String H_DEUR = "tempel_deur";

    // door
    private static long deurOpenTot = -1, deurDichtBericht;
    private static int deurAnim = -1;
    // room 1
    private static int[] volgorde = {0, 1, 2, 3, 4};
    private static int stap, toonT = -1;
    private static long k1Klaar = -1;
    // room 2
    private static boolean[] patroon = {true, false, true, true};
    private static long k2Klaar = -1;
    private static boolean k2Plaat;
    // room 3
    private static int golf;
    private static final List<UUID> wezens = new ArrayList<>();
    private static long k3Klaar = -1, k3Leeg, golfOp = -1;
    private static boolean opgezet;

    private Zwamtempel() {
    }

    static boolean binnen(Player p, BlockPos a, BlockPos b) {
        return p.getX() >= Math.min(a.getX(), b.getX()) && p.getX() <= Math.max(a.getX(), b.getX()) + 1 && p.getZ() >= Math.min(a.getZ(), b.getZ())
                && p.getZ() <= Math.max(a.getZ(), b.getZ()) + 1 && p.getY() >= Math.min(a.getY(), b.getY()) - 1 && p.getY() <= Math.max(a.getY(), b.getY()) + 2;
    }

    static List<ServerPlayer> in(ServerLevel sl, BlockPos a, BlockPos b) {
        List<ServerPlayer> l = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && binnen(p, a, b)) {
                l.add(p);
            }
        }
        return l;
    }

    private static void zeg(List<ServerPlayer> l, String key, ChatFormatting k, Object... args) {
        for (ServerPlayer p : l) {
            p.sendOverlayMessage(Component.translatable(key, args).withStyle(k));
        }
    }

    // ---- gates --------------------------------------------------------------------------------------------------------------

    public static void poortDicht(ServerLevel sl, BlockPos a, BlockPos b) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            if (sl.getBlockState(p).isAir() && sl.getEntitiesOfClass(Player.class, new AABB(p)).isEmpty()) {
                sl.setBlock(p, BazenRegistry.ZWAMPOORT.get().defaultBlockState(), 3);
            }
        }
    }

    public static void poortOpen(ServerLevel sl, BlockPos a, BlockPos b) {
        for (BlockPos p : BlockPos.betweenClosed(a, b)) {
            if (sl.getBlockState(p).is(BazenRegistry.ZWAMPOORT.get())) {
                sl.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                if (sl.getRandom().nextInt(3) == 0) {
                    sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0);
                }
            }
        }
        BlockPos m = BlockPos.containing((a.getX() + b.getX()) / 2.0, a.getY(), (a.getZ() + b.getZ()) / 2.0);
        sl.playSound(null, m, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.5f, 0.6f);
    }

    // ---- the tick -----------------------------------------------------------------------------------------------------------

    public static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.getGameTime() % 5 != 0 || !sl.isLoaded(BazenPlekken.TEMPELDEUR_VOOR) || !sl.isLoaded(BazenPlekken.OZ_CENTRUM)) {
            return;
        }
        Vec3 d = Vec3.atCenterOf(BazenPlekken.TEMPELDEUR_VOOR);
        boolean iemand = false;
        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceToSqr(d) < 160 * 160) {
                iemand = true;
                break;
            }
        }
        if (!iemand) {
            return;
        }
        long nu = sl.getGameTime();
        if (!opgezet) {
            opgezet = true;
            k1Reset(sl);
            k2Reset(sl);
            poortDicht(sl, BazenPlekken.K3_POORT_MIN, BazenPlekken.K3_POORT_MAX);
        }
        deur(sl, nu);
        kamer1(sl, nu);
        kamer2(sl, nu);
        kamer3(sl, nu);
    }

    // ---- the door ceremony --------------------------------------------------------------------------------------------------

    private static void deur(ServerLevel sl, long nu) {
        Vec3 voor = Vec3.atBottomCenterOf(BazenPlekken.TEMPELDEUR_VOOR);
        if (deurAnim >= 0) {
            deurAnim += 5;
            if (deurAnim >= 40) {
                schuif(sl, deurAnim - 40);
            }
            if (deurAnim > 100) {
                deurAnim = -1;
            }
            return;
        }
        if (deurOpenTot > 0) {
            if (nu > deurOpenTot) {
                boolean dichtbij = sl.players().stream().anyMatch(p -> p.position().distanceToSqr(voor) < 12 * 12);
                if (!dichtbij) {
                    deurOpenTot = -1;
                    if (!Adapters.deurDicht(sl, "zwamtempel")) {
                        Herstel.terug(sl, H_DEUR);
                    }
                }
            }
            return;
        }
        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceToSqr(voor) > 6 * 6) {
                continue;
            }
            if (!MAG_NAAR_BINNEN.test(p)) {
                if (nu > deurDichtBericht) {
                    deurDichtBericht = nu + 100;
                    String waarom = waaromDicht(p);
                    p.sendOverlayMessage(dichtTekst(p, waarom == null ? "vadsrijk.bazen.tempel.te_laag" : waarom));
                }
                continue;
            }
            ceremonie(sl, nu);
            return;
        }
    }

    /** Opens the temple door with its scene for everyone near (public for the dev command). */
    public static void ceremonie(ServerLevel sl, long nu) {
        Vec3 voor = Vec3.atBottomCenterOf(BazenPlekken.TEMPELDEUR_VOOR);
        BlockPos a = BazenPlekken.TEMPELDEUR_MIN, b = BazenPlekken.TEMPELDEUR_MAX;
        Vec3 deurMidden = new Vec3((a.getX() + b.getX()) / 2.0 + 0.5, (a.getY() + b.getY()) / 2.0, a.getZ() + 0.5);
        Vec3 richting = voor.subtract(deurMidden).multiply(1, 0, 1).normalize();
        RijkScene s = RijkScene.maak("zwamtempel_deur", 110)
                .camera(0, deurMidden.add(richting.scale(26)).add(0, -6, 0), deurMidden.add(0, 4, 0))
                .camera(50, deurMidden.add(richting.scale(18)).add(0, -7, 0), deurMidden.add(0, 6, 0))
                .camera(110, deurMidden.add(richting.scale(9)).add(0, -8, 0), deurMidden)
                .titel("vadsrijk.bazen.zwamtempel", "vadsrijk.bazen.zwamtempel.onder", 50, 55, 0xE85696)
                .bouw();
        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceToSqr(voor) < 24 * 24) {
                RijkScenes.speel(p, s);
                p.sendOverlayMessage(Component.translatable("vadsrijk.bazen.tempel.open").withStyle(ChatFormatting.LIGHT_PURPLE));
                meldDeurOpen(p);
            }
        }
        sl.playSound(null, BlockPos.containing(deurMidden), SoundEvents.WARDEN_EMERGE, SoundSource.BLOCKS, 2f, 1.4f);
        deurOpenTot = nu + DEUR_OPEN_TICKS;
        deurAnim = Adapters.deurOpen(sl, "zwamtempel") ? -1 : 0;
    }

    /** Fallback door: columns slide away from the middle outwards (t = ticks since it started moving). */
    private static void schuif(ServerLevel sl, int t) {
        BlockPos a = BazenPlekken.TEMPELDEUR_MIN, b = BazenPlekken.TEMPELDEUR_MAX;
        int midden = (a.getX() + b.getX()) / 2, half = (b.getX() - a.getX()) / 2 + 1;
        int tot = Math.min(half, t / 5 + 1);
        for (int dx = 0; dx < tot; dx++) {
            for (int x : new int[] {midden - dx, midden + dx}) {
                for (int y = a.getY(); y <= b.getY(); y++) {
                    BlockPos p = new BlockPos(x, y, a.getZ());
                    BlockState s = sl.getBlockState(p);
                    if (!s.isAir() && s.getDestroySpeed(sl, p) >= 0) {
                        Herstel.zet(sl, H_DEUR, p, Blocks.AIR.defaultBlockState());
                        if (y % 3 == 0) {
                            sl.sendParticles(ParticleTypes.CLOUD, x + 0.5, y + 0.5, a.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
                        }
                    }
                }
            }
        }
        sl.playSound(null, new BlockPos(midden, a.getY(), a.getZ()), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1f, 0.4f);
    }

    // ---- room 1: the Lichtzwamkamer -----------------------------------------------------------------------------------------

    private static void k1Reset(ServerLevel sl) {
        List<Integer> l = new ArrayList<>(List.of(0, 1, 2, 3, 4));
        java.util.Collections.shuffle(l, new java.util.Random(sl.getRandom().nextLong()));
        volgorde = l.stream().mapToInt(Integer::intValue).toArray();
        stap = 0;
        toonT = -1;
        k1Klaar = -1;
        for (BlockPos p : BazenPlekken.K1_ZWAMMEN) {
            if (sl.getBlockState(p).canBeReplaced() || sl.getBlockState(p).getBlock() instanceof LichtzwamBlok) {
                sl.setBlock(p, BazenRegistry.LICHTZWAM.get().defaultBlockState(), 3);
            }
        }
        poortDicht(sl, BazenPlekken.K1_POORT_MIN, BazenPlekken.K1_POORT_MAX);
    }

    private static void kamer1(ServerLevel sl, long nu) {
        List<ServerPlayer> l = in(sl, BazenPlekken.K1_MIN, BazenPlekken.K1_MAX);
        if (nu % 20 == 0) {
            l.forEach(Zwamtempel::meldDeurOpen);   // (inside = through the door, however you got in)
        }
        if (k1Klaar > 0) {
            if (nu - k1Klaar > RESET_TICKS && l.isEmpty() && in(sl, BazenPlekken.K2_MIN, BazenPlekken.K2_MAX).isEmpty()) {
                k1Reset(sl);
            }
            return;
        }
        if (toonT < 0) {
            if (l.isEmpty()) {
                if (stap == 0 && toonT == -2) {
                    toonT = -1;   // the next group gets the show again
                }
                return;
            }
            if (toonT == -1) {
                toonT = 0;
                zeg(l, "vadsrijk.bazen.tempel.kamer1", ChatFormatting.LIGHT_PURPLE);
            }
            return;
        }
        toonT += 5;
        int i = (toonT - 20) / 20;
        if (toonT >= 20 && i < 5 && (toonT - 20) % 20 == 0) {
            LichtzwamBlok.zet(sl, BazenPlekken.K1_ZWAMMEN.get(volgorde[i]), true);
        }
        if (toonT >= 20 && (toonT - 20) % 20 == 15 && i < 5) {
            LichtzwamBlok.zet(sl, BazenPlekken.K1_ZWAMMEN.get(volgorde[i]), false);
        }
        if (toonT > 20 + 5 * 20 + 10) {
            toonT = -2;   // shown; waiting for the players (-2 = not showing, but don't show again on its own)
        }
    }

    public static void lichtzwamAangestoken(ServerLevel sl, BlockPos pos, ServerPlayer p) {
        int i = BazenPlekken.K1_ZWAMMEN.indexOf(pos);
        if (i < 0 || k1Klaar > 0) {
            return;
        }
        if (toonT >= 0) {
            LichtzwamBlok.zet(sl, pos, false);
            return;
        }
        List<ServerPlayer> l = in(sl, BazenPlekken.K1_MIN, BazenPlekken.K1_MAX);
        if (volgorde[stap] == i) {
            stap++;
            if (stap == 5) {
                k1Klaar = sl.getGameTime();
                poortOpen(sl, BazenPlekken.K1_POORT_MIN, BazenPlekken.K1_POORT_MAX);
                zeg(l, "vadsrijk.bazen.tempel.kamer1_goed", ChatFormatting.GREEN);
                sl.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1f, 1.2f);
            }
            return;
        }
        for (BlockPos z : BazenPlekken.K1_ZWAMMEN) {
            LichtzwamBlok.zet(sl, z, false);
        }
        sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 60, 2, 1, 2, 0.02);
        sl.playSound(null, pos, SoundEvents.PUFFER_FISH_BLOW_OUT, SoundSource.BLOCKS, 1f, 0.6f);
        zeg(l, "vadsrijk.bazen.tempel.kamer1_fout", ChatFormatting.RED);
        stap = 0;
        toonT = -20;   // shows again in a moment
        BazenRegistry.later(sl.getServer(), 60, () -> {
            if (k1Klaar < 0 && stap == 0) {
                toonT = 0;
            }
        });
    }

    // ---- room 2: the Hendelkamer --------------------------------------------------------------------------------------------

    private static void k2Reset(ServerLevel sl) {
        var r = sl.getRandom();
        int n;
        do {
            n = 0;
            for (int i = 0; i < 4; i++) {
                patroon[i] = r.nextBoolean();
                n += patroon[i] ? 1 : 0;
            }
        } while (n == 0 || n == 4);
        k2Klaar = -1;
        for (int i = 0; i < 4; i++) {
            BlockPos h = BazenPlekken.K2_HENDELS.get(i);
            if (sl.getBlockState(h).canBeReplaced() || sl.getBlockState(h).is(Blocks.LEVER)) {
                sl.setBlock(h, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR).setValue(LeverBlock.FACING, Direction.NORTH), 3);
            }
            BlockPos z = BazenPlekken.K2_HINTS.get(i);
            if (sl.getBlockState(z).canBeReplaced() || sl.getBlockState(z).getBlock() instanceof LichtzwamBlok) {
                sl.setBlock(z, BazenRegistry.LICHTZWAM.get().defaultBlockState().setValue(LichtzwamBlok.LIT, patroon[i]), 3);
            }
        }
        if (sl.getBlockState(BazenPlekken.K2_PLAAT).canBeReplaced()) {
            sl.setBlock(BazenPlekken.K2_PLAAT, Blocks.STONE_PRESSURE_PLATE.defaultBlockState(), 3);
        }
        poortDicht(sl, BazenPlekken.K2_POORT_MIN, BazenPlekken.K2_POORT_MAX);
    }

    private static void kamer2(ServerLevel sl, long nu) {
        List<ServerPlayer> l = in(sl, BazenPlekken.K2_MIN, BazenPlekken.K2_MAX);
        if (k2Klaar > 0) {
            if (nu - k2Klaar > RESET_TICKS && l.isEmpty() && in(sl, BazenPlekken.K3_MIN, BazenPlekken.K3_MAX).isEmpty() && golf == 0) {
                k2Reset(sl);
            }
            return;
        }
        BlockState plaat = sl.getBlockState(BazenPlekken.K2_PLAAT);
        boolean op = plaat.hasProperty(PressurePlateBlock.POWERED) && plaat.getValue(PressurePlateBlock.POWERED);
        if (op && !k2Plaat) {
            boolean goed = true;
            for (int i = 0; i < 4; i++) {
                BlockState h = sl.getBlockState(BazenPlekken.K2_HENDELS.get(i));
                boolean aan = h.hasProperty(LeverBlock.POWERED) && h.getValue(LeverBlock.POWERED);
                goed &= aan == patroon[i];
            }
            if (goed) {
                k2Klaar = nu;
                poortOpen(sl, BazenPlekken.K2_POORT_MIN, BazenPlekken.K2_POORT_MAX);
                zeg(l, "vadsrijk.bazen.tempel.kamer2_goed", ChatFormatting.GREEN);
            } else {
                for (BlockPos h : BazenPlekken.K2_HENDELS) {
                    BlockState s = sl.getBlockState(h);
                    if (s.hasProperty(LeverBlock.POWERED) && s.getValue(LeverBlock.POWERED)) {
                        sl.setBlock(h, s.setValue(LeverBlock.POWERED, false), 3);
                    }
                }
                sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, BazenPlekken.K2_PLAAT.getX() + 0.5, BazenPlekken.K2_PLAAT.getY() + 1,
                        BazenPlekken.K2_PLAAT.getZ() + 0.5, 50, 2, 1, 2, 0.02);
                zeg(l, "vadsrijk.bazen.tempel.kamer2_fout", ChatFormatting.RED);
            }
        }
        k2Plaat = op;
        if (!l.isEmpty() && nu % 400 == 0) {
            zeg(l, "vadsrijk.bazen.tempel.kamer2", ChatFormatting.LIGHT_PURPLE);
        }
    }

    // ---- room 3: the Ridderzaal ---------------------------------------------------------------------------------------------

    private static void kamer3(ServerLevel sl, long nu) {
        List<ServerPlayer> l = in(sl, BazenPlekken.K3_MIN, BazenPlekken.K3_MAX);
        if (k3Klaar > 0) {
            if (nu - k3Klaar > RESET_TICKS && l.isEmpty() && in(sl, BazenPlekken.K2_MIN, BazenPlekken.K2_MAX).isEmpty()) {
                k3Klaar = -1;
                poortDicht(sl, BazenPlekken.K3_POORT_MIN, BazenPlekken.K3_POORT_MAX);
            }
            return;
        }
        if (golf == 0) {
            boolean diep = l.stream().anyMatch(p -> p.getZ() < BazenPlekken.K3_MAX.getZ() - 4);
            if (diep) {
                poortDicht(sl, BazenPlekken.K2_POORT_MIN, BazenPlekken.K2_POORT_MAX);
                golf(sl, 1, l);
            }
            return;
        }
        if (l.isEmpty()) {
            if (k3Leeg == 0) {
                k3Leeg = nu;
            } else if (nu - k3Leeg > 600) {
                for (UUID id : wezens) {
                    if (sl.getEntity(id) != null) {
                        sl.getEntity(id).discard();
                    }
                }
                wezens.clear();
                golf = 0;
                golfOp = -1;
                if (k2Klaar > 0) {
                    poortOpen(sl, BazenPlekken.K2_POORT_MIN, BazenPlekken.K2_POORT_MAX);
                }
            }
            return;
        }
        k3Leeg = 0;
        boolean levend = wezens.stream().anyMatch(id -> sl.getEntity(id) instanceof VadsrijkMob m && !m.geplofd() && !m.isRemoved());
        if (levend) {
            return;
        }
        if (golfOp < 0) {
            golfOp = nu + 40;
        } else if (nu >= golfOp) {
            golfOp = -1;
            if (golf >= 3) {
                golf = 0;
                wezens.clear();
                k3Klaar = nu;
                poortOpen(sl, BazenPlekken.K3_POORT_MIN, BazenPlekken.K3_POORT_MAX);
                if (k2Klaar > 0) {
                    poortOpen(sl, BazenPlekken.K2_POORT_MIN, BazenPlekken.K2_POORT_MAX);
                }
                zeg(l, "vadsrijk.bazen.tempel.ridders_klaar", ChatFormatting.GOLD);
                sl.playSound(null, BazenPlekken.K3_POORT_MIN, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.5f, 0.8f);
            } else {
                golf(sl, golf + 1, l);
            }
        }
    }

    private static void golf(ServerLevel sl, int n, List<ServerPlayer> l) {
        golf = n;
        wezens.clear();
        int ridders = n == 1 ? 3 : n == 2 ? 4 : 2;
        List<BlockPos> s = BazenPlekken.K3_SPAWNS;
        for (int i = 0; i < ridders; i++) {
            VadsrijkMob m = Adapters.spawn(sl, Adapters.RIDDER, s.get(i % s.size()), 18 + sl.getRandom().nextInt(2), false);
            if (m != null) {
                wezens.add(m.getUUID());
            }
        }
        if (n == 3) {
            VadsrijkMob m = Adapters.spawn(sl, Adapters.PRIESTER, s.get(1), 19, true);
            if (m != null) {
                wezens.add(m.getUUID());
            }
        }
        for (BlockPos p : s) {
            sl.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, p.getX() + 0.5, p.getY() + 1, p.getZ() + 0.5, 30, 0.8, 1, 0.8, 0.02);
        }
        zeg(l, "vadsrijk.bazen.tempel.ridders", ChatFormatting.LIGHT_PURPLE, n);
    }

    /** Dev: every room and the door back to the start. */
    public static void resetAlles(ServerLevel sl) {
        for (UUID id : wezens) {
            if (sl.getEntity(id) != null) {
                sl.getEntity(id).discard();
            }
        }
        wezens.clear();
        golf = 0;
        golfOp = -1;
        k3Klaar = -1;
        k1Reset(sl);
        k2Reset(sl);
        poortDicht(sl, BazenPlekken.K3_POORT_MIN, BazenPlekken.K3_POORT_MAX);
        deurOpenTot = -1;
        deurAnim = -1;
        if (!Adapters.deurDicht(sl, "zwamtempel")) {
            Herstel.terug(sl, H_DEUR);
        }
        opgezet = true;
    }

    /** The hint for testers: the current order and pattern. */
    public static String oplossing() {
        StringBuilder b = new StringBuilder("Lichtzwamkamer: ");
        for (int v : volgorde) {
            b.append(v + 1).append(' ');
        }
        b.append("| Hendels aan: ");
        for (int i = 0; i < 4; i++) {
            b.append(patroon[i] ? "1" : "0");
        }
        return b.toString();
    }
}
