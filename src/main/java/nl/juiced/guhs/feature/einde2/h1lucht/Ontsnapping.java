package nl.juiced.guhs.feature.einde2.h1lucht;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.VerhaalSync;

/**
 * einde2 phase B, slice h1_lucht, step 9 (DESIGN 5 "Zuka's boog", h1): Zuka's ship has rammed the pier; run from the pier
 * over the landing terrace to Appa in the camp. Zuka ({@code ZUKA_BALLING}) stays on the head of the pier and throws
 * fireballs that only shove; four of his Vuurnatie-Mika's stand on the terrace ({@code ontsnap_wacht_1..4}) and shove you
 * when you come too close. Shoved four times: back to {@code rustpunt_5} at the root of the pier (nothing lost). The power
 * luchtbal blows fireballs away and his men aside ({@link #waai}). Reaching {@code appa_kamp}: {@link Tempel#bijAppa}.
 * <p>
 * Everything belongs to the one player: the actors only they see, the fireballs are particles sent to them alone, and the
 * ship is the overlay {@code laag_zuka_schip} shown through {@link Schijn} while the flag {@value E2H1Lucht#V_SCHIP} is on.
 */
public final class Ontsnapping {
    /** Shoves until you are put back at the pier. */
    public static final int TREFFERS = 4;
    /** The rest point kind (Rustpunten) of the escape. */
    public static final String RUSTPUNT = "einde2_h1_lucht";

    private static final class Bal {
        Vec3 pos, vel;
        int leeftijd;

        Bal(Vec3 pos, Vec3 vel) {
            this.pos = pos;
            this.vel = vel;
        }
    }

    private static final class Staat {
        GuhNpcEntity zuka;
        final List<GuhNpcEntity> wachten = new ArrayList<>();
        final List<Vec3> thuis = new ArrayList<>();
        int[] verdoofd, afkoel;
        final List<Bal> ballen = new ArrayList<>();
        int treffers;
        long volgende;
    }

    private static final Map<UUID, Staat> STATEN = new ConcurrentHashMap<>();

    private Ontsnapping() {
    }

    public static boolean bezig(Player p) {
        return STATEN.containsKey(p.getUUID());
    }

    /** After the scene zuka_steiger: Zuka and his men appear, Appa waits in the camp, the run begins. */
    static void start(ServerPlayer p) {
        ServerLevel g = E2H1Lucht.guheinde(p.level().getServer());
        if (g == null || E2H1Lucht.LIJN.stap(p) != E2H1Lucht.ZUKA) {
            return;
        }
        Vec3 zuka = E2H1Lucht.midden(g, "zuka_confrontatie"), rust = E2H1Lucht.midden(g, "rustpunt_5"), kamp = E2H1Lucht.midden(g, "appa_kamp");
        if (zuka == null || rust == null || kamp == null) {
            return;
        }
        stop(p);
        Staat st = new Staat();
        st.zuka = Tempel.acteur(g, p, GuhNpcEntity.Kind.ZUKA_BALLING, zuka, 90f);
        for (int i = 1; i <= 4; i++) {
            Vec3 w = E2H1Lucht.midden(g, "ontsnap_wacht_" + i);
            if (w != null) {
                GuhNpcEntity mika = Tempel.acteur(g, p, GuhNpcEntity.Kind.VUURNATIE_MIKA, w, -90f);
                if (mika != null) {
                    st.wachten.add(mika);
                    st.thuis.add(w);
                }
            }
        }
        st.verdoofd = new int[st.wachten.size()];
        st.afkoel = new int[st.wachten.size()];
        st.volgende = g.getGameTime() + 40;
        STATEN.put(p.getUUID(), st);
        E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_VLUCHT, true);
        E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_SCHIP, true);
        VerhaalSync.sync(p);
        Rustpunten.zet(p, RUSTPUNT, GuheindeFeature.GUHEINDE, rust, 90f);
        // Appa waits in the camp
        if (Appa.heeft(p)) {
            AppaGuhEntity appa = Appa.roep(p);
            if (appa != null && p.getVehicle() != appa) {
                appa.snapTo(kamp.x, kamp.y, kamp.z, 90f, 0f);
            }
        }
        p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "ontsnap.start").withStyle(ChatFormatting.GOLD));
    }

    /** Every tick, for a player on the run. */
    static void tick(ServerPlayer p) {
        Staat st = STATEN.get(p.getUUID());
        if (st == null) {
            // (logged out in the middle: the run starts again where it began)
            if (E2H1Lucht.LIJN.stap(p) == E2H1Lucht.ZUKA && E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_VLUCHT) && E2H1Lucht.inTempel(p)
                    && !Cutscenes.bezig(p) && p.tickCount % 20 == 0) {
                start(p);
            }
            return;
        }
        if (E2H1Lucht.LIJN.stap(p) != E2H1Lucht.ZUKA || !GuheindeFeature.isGuheinde(p.level())) {
            stop(p);
            return;
        }
        if (Cutscenes.bezig(p)) {
            return;
        }
        ServerLevel g = p.level();
        Vec3 kamp = E2H1Lucht.midden(g, "appa_kamp");
        if (kamp != null && horizontaal(p.position(), kamp) < 4.5 && Math.abs(p.getY() - kamp.y) < 3) {
            stop(p);
            Tempel.bijAppa(p);
            return;
        }
        long nu = g.getGameTime();
        Vec3 midden = p.position().add(0, 0.9, 0);
        // Zuka throws
        if (st.zuka != null && !st.zuka.isRemoved() && nu >= st.volgende && st.zuka.distanceTo(p) < 42) {
            Vec3 van = st.zuka.position().add(0, 1.3, 0);
            Vec3 doel = midden.add(p.getDeltaMovement().multiply(10, 0, 10));
            st.ballen.add(new Bal(van, doel.subtract(van).normalize().scale(0.6)));
            st.volgende = nu + 46 + p.getRandom().nextInt(22);
            st.zuka.speel("vuur");
            Schijn.geluid(p, SoundEvents.BLAZE_SHOOT, st.zuka.blockPosition(), 0.8f, 1.1f);
        }
        for (Iterator<Bal> it = st.ballen.iterator(); it.hasNext(); ) {
            Bal b = it.next();
            b.pos = b.pos.add(b.vel);
            b.leeftijd++;
            Schijn.deeltjes(p, ParticleTypes.FLAME, b.pos, 4, 0.12, 0.01);
            Schijn.deeltjes(p, ParticleTypes.SMOKE, b.pos, 1, 0.05, 0.0);
            BlockState in = g.getBlockState(BlockPos.containing(b.pos));
            if (b.leeftijd > 90 || (!in.isAir() && in.isSolidRender())) {
                Schijn.deeltjes(p, ParticleTypes.POOF, b.pos, 6, 0.2, 0.02);
                it.remove();
            } else if (b.pos.distanceTo(midden) < 1.1) {
                it.remove();
                Schijn.deeltjes(p, ParticleTypes.FLAME, b.pos, 12, 0.3, 0.05);
                if (Duwtje.mag(p)) {
                    Duwtje.duw(p, b.vel, 0.9);
                    treffer(p, st);
                }
            }
        }
        // his men: step closer when you are near, shove when you are too near; back to their spot when you are gone
        for (int i = 0; i < st.wachten.size(); i++) {
            GuhNpcEntity w = st.wachten.get(i);
            if (w == null || w.isRemoved()) {
                continue;
            }
            if (st.verdoofd[i] > 0) {
                st.verdoofd[i]--;
                if (st.verdoofd[i] % 10 == 0) {
                    Schijn.deeltjes(p, ParticleTypes.CRIT, w.position().add(0, w.getBbHeight() + 0.3, 0), 4, 0.25, 0.02);
                }
                continue;
            }
            if (st.afkoel[i] > 0) {
                st.afkoel[i]--;
            }
            double d = horizontaal(w.position(), p.position());
            Vec3 naar = d < 7 && Math.abs(w.getY() - p.getY()) < 2 ? p.position() : st.thuis.get(i);
            stap(g, w, naar, naar == st.thuis.get(i) ? 0.06 : 0.1);
            if (d < 1.6 && st.afkoel[i] == 0 && Duwtje.mag(p)) {
                Duwtje.duw(p, p.position().subtract(w.position()), 1.0);
                w.speel("duw");
                st.afkoel[i] = 30;
                treffer(p, st);
            }
        }
    }

    /** One step of a man towards a point (on the same floor; never into a block, never over an edge). */
    private static void stap(ServerLevel g, GuhNpcEntity w, Vec3 naar, double snelheid) {
        Vec3 d = new Vec3(naar.x - w.getX(), 0, naar.z - w.getZ());
        double len = d.length();
        if (len < 0.4) {
            return;
        }
        Vec3 nieuw = w.position().add(d.scale(Math.min(snelheid, len) / len));
        BlockPos voeten = BlockPos.containing(nieuw);
        if (!g.getBlockState(voeten).getCollisionShape(g, voeten).isEmpty() || !g.getBlockState(voeten.above()).getCollisionShape(g, voeten.above()).isEmpty()
                || g.getBlockState(voeten.below()).getCollisionShape(g, voeten.below()).isEmpty()) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        w.snapTo(nieuw.x, w.getY(), nieuw.z, yaw, 0f);
        w.setYHeadRot(yaw);
        w.setYBodyRot(yaw);
    }

    private static double horizontaal(Vec3 a, Vec3 b) {
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    private static void treffer(ServerPlayer p, Staat st) {
        st.treffers++;
        if (st.treffers >= TREFFERS) {
            st.treffers = 0;
            st.ballen.clear();
            for (int i = 0; i < st.wachten.size(); i++) {
                Vec3 t = st.thuis.get(i);
                st.wachten.get(i).snapTo(t.x, t.y, t.z, -90f, 0f);
            }
            Rustpunten.terug(p, RUSTPUNT);
            p.sendSystemMessage(Component.translatable(E2H1Lucht.P + "ontsnap.terug").withStyle(ChatFormatting.YELLOW));
            return;
        }
        p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "ontsnap.geraakt", st.treffers).withStyle(ChatFormatting.RED));
    }

    /** The power luchtbal during the run: fireballs in front of you are blown out, his men in front of you blown aside. */
    static boolean waai(ServerPlayer p, Vec3 richting) {
        Staat st = STATEN.get(p.getUUID());
        if (st == null) {
            return false;
        }
        Vec3 oog = p.getEyePosition();
        boolean iets = false;
        for (Iterator<Bal> it = st.ballen.iterator(); it.hasNext(); ) {
            Bal b = it.next();
            Vec3 d = b.pos.subtract(oog);
            if (d.length() < 8 && d.normalize().dot(richting) > 0.3) {
                Schijn.deeltjes(p, ParticleTypes.CLOUD, b.pos, 10, 0.3, 0.05);
                it.remove();
                iets = true;
            }
        }
        for (int i = 0; i < st.wachten.size(); i++) {
            GuhNpcEntity w = st.wachten.get(i);
            Vec3 d = w.position().subtract(p.position());
            if (d.length() < 7 && d.normalize().dot(richting) > 0.3) {
                st.verdoofd[i] = 100;
                Vec3 weg = new Vec3(richting.x, 0, richting.z).normalize();
                for (int k = 0; k < 10; k++) {
                    stap(p.level(), w, w.position().add(weg), 0.25);
                }
                Schijn.deeltjes(p, ParticleTypes.CLOUD, w.position().add(0, 1, 0), 14, 0.4, 0.05);
                iets = true;
            }
        }
        if (iets) {
            p.sendOverlayMessage(Component.translatable(E2H1Lucht.P + "ontsnap.geblazen").withStyle(ChatFormatting.AQUA));
        }
        return iets;
    }

    /** Ends the run: the actors go (the flags stay as the story leaves them). */
    static void stop(ServerPlayer p) {
        vergeet(p.getUUID());
    }

    static void vergeet(UUID speler) {
        Staat st = STATEN.remove(speler);
        if (st != null) {
            Tijdelijk.weg(st.zuka);
            st.wachten.forEach(Tijdelijk::weg);
        }
    }

    // =====================================================================================================================
    // the ship: the overlay laag_zuka_schip, shown to one player through Schijn
    // =====================================================================================================================
    /** The blocks the overlay is made of (tools/features/einde2_bouw_lucht_bouw.zuka_schip_laag). */
    private static final List<Block> SCHIPBLOKKEN = List.of(Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.RED_NETHER_BRICKS, Blocks.DARK_OAK_PLANKS,
            Blocks.IRON_BLOCK, Blocks.POLISHED_BLACKSTONE_BRICK_WALL, Blocks.RED_STAINED_GLASS, Blocks.POLISHED_BLACKSTONE, Blocks.MAGMA_BLOCK,
            Blocks.STRIPPED_DARK_OAK_LOG, Blocks.RED_WOOL, Blocks.GOLD_BLOCK, Blocks.ORANGE_CONCRETE, Blocks.RED_CONCRETE);
    private static volatile Map<BlockPos, BlockState> schip;

    /** The ship's blocks relative to zuka_schip_anker (read from its template once). */
    static Map<BlockPos, BlockState> schip(ServerLevel g) {
        Map<BlockPos, BlockState> s = schip;
        if (s != null) {
            return s;
        }
        Map<BlockPos, BlockState> nieuw = new HashMap<>();
        Plekken.Laag laag = Plekken.blad(E2H1Lucht.STRUCTUUR).lagen().get("zuka_schip");
        StructureTemplate t = laag == null ? null : g.getStructureManager().get(laag.template()).orElse(null);
        if (t != null) {
            StructurePlaceSettings zo = new StructurePlaceSettings();
            for (Block b : SCHIPBLOKKEN) {
                for (StructureTemplate.StructureBlockInfo info : t.filterBlocks(BlockPos.ZERO, zo, b)) {
                    nieuw.put(info.pos().immutable(), info.state());
                }
            }
        }
        schip = s = Map.copyOf(nieuw);
        return s;
    }

    /** Schijn.Bron: the ship at the pier for a player at step 9 while the flag is on. */
    static void vul(ServerPlayer p, Map<BlockPos, BlockState> gewenst) {
        if (!GuheindeFeature.isGuheinde(p.level()) || E2H1Lucht.LIJN.stap(p) != E2H1Lucht.ZUKA || !E2H1Lucht.LIJN.vlag(p, E2H1Lucht.V_SCHIP)) {
            return;
        }
        BlockPos anker = E2H1Lucht.plek(p.level(), "zuka_schip_anker");
        if (anker == null || !Schijn.dichtbij(p, anker)) {
            return;
        }
        for (Map.Entry<BlockPos, BlockState> e : schip(p.level()).entrySet()) {
            BlockPos pos = anker.offset(e.getKey());
            if (Schijn.dichtbij(p, pos)) {
                gewenst.put(pos, e.getValue());
            }
        }
    }

    /** The ship appears at once (the scene starts right after this). */
    static void toonSchip(ServerPlayer p) {
        Schijn.ververs(p);
    }

    /** (test) how many blocks the ship has (0: its template is not there). */
    static int schipGrootte(ServerLevel g) {
        return schip(g).size();
    }

    @Nullable
    static GuhNpcEntity zuka(ServerPlayer p) {
        Staat st = STATEN.get(p.getUUID());
        return st == null ? null : st.zuka;
    }
}
