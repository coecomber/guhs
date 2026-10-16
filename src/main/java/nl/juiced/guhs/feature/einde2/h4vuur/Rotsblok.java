package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Duwtje;

/**
 * einde2 phase B, slice h4_vuur, step 8: Het Kokend Rotsblok (DESIGN 5, chapter 4: Zuka makes it up with Sokka). From the
 * upper station on the crater rim the gondola takes you over the boiling lake of kaassaus to the roof of the prison; then
 * you sneak down and across the yard to Suki-guh's cell while two bewaker-Mika's patrol it (actors only you see). A guard
 * who sees you (in front of him, a free line, you not crouching; or simply too close) shoves you gently back to the hall:
 * try again. At Suki's cell 626-guh opens every door (wink 4) and the escape follows ({@link Scenes#KOKEND_ROTSBLOK}).
 */
public final class Rotsblok {
    /** A guard sees you within this many blocks in front of him when you walk, and always within the second. */
    public static final double ZIET = 7.0, VOELT = 2.2, SLUIP_ZIET = 3.2;
    /** Blocks per tick a guard walks. */
    private static final double TEMPO = 0.07;

    private static final class Wacht {
        final GuhNpcEntity npc;
        final Vec3 a, b;
        boolean heen = true;

        Wacht(GuhNpcEntity npc, Vec3 a, Vec3 b) {
            this.npc = npc;
            this.a = a;
            this.b = b;
        }

        Vec3 richting() {
            Vec3 d = (heen ? b : a).subtract(npc.position());
            return new Vec3(d.x, 0, d.z).normalize();
        }
    }

    private static final Map<UUID, List<Wacht>> SLUIPEN = new ConcurrentHashMap<>();

    private Rotsblok() {
    }

    public static boolean actief(ServerPlayer p) {
        return SLUIPEN.containsKey(p.getUUID());
    }

    /** The direction of step 8 (from E2H4Vuur.regie): the gondola, then the sneak. */
    static void regie(ServerPlayer p) {
        if (Plek.bij(p, "gondel_boven", 2.5)) {
            gondel(p);
            return;
        }
        if (E2H4Vuur.VUUR.vlag(p, E2H4Vuur.V_BINNEN) && Plek.in(p, "gevangenis")) {
            start(p);
        } else if (Plek.in(p, "bovenstation") && E2H4Vuur.VUUR.eenmalig(p, "rotsblok_plan")) {
            E2H4Vuur.zeg(p, "rotsblok.plan");
        }
    }

    /** Into the gondola: a creaking ride over the lake (a fade), out on the roof of the prison. */
    private static void gondel(ServerPlayer p) {
        Vec3 onder = Plek.staan(p, "gondel_onder");
        ServerLevel g = Plek.guheinde(p);
        if (onder == null || g == null) {
            return;
        }
        Schijn.geluid(p, SoundEvents.CHAIN_PLACE, p.blockPosition(), 1.0f, 0.6f);
        E2H4Vuur.balk(p, "rotsblok.gondel");
        Later.plan(p, 30, q -> {
            Plek.breng(q, g, onder, 90f);
            E2H4Vuur.VUUR.vlag(q, E2H4Vuur.V_BINNEN, true);
            nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(q);
            E2H4Vuur.zeg(q, "rotsblok.binnen");
            E2H4Vuur.titel(q, "rotsblok.titel", "rotsblok.sub");
            // (fix2-h4) a look at where you must go: Suki-guh's cell across the yard
            net.minecraft.core.BlockPos cel = Plek.blok(q, "cel_suki");
            if (cel != null) {
                Later.plan(q, 50, r -> nl.juiced.guhs.feature.einde2.verhaal.StapHint.speelKijk(r, H4Hints.kijkSuki, cel, null));
            }
        });
    }

    /** The sneak begins: the two guards of the yard appear (for this player). */
    static void start(ServerPlayer p) {
        if (actief(p)) {
            return;
        }
        Vec3 plein = Plek.midden(p, "gevangenis_plein");
        if (plein == null) {
            return;
        }
        List<Wacht> wachten = new ArrayList<>();
        // one patrols the north half of the yard, one the south half (west to east and back)
        double y = plein.y;
        for (double dz : new double[] {-2.5, 2.5}) {
            Vec3 a = new Vec3(plein.x - 4, y, plein.z + dz), b = new Vec3(plein.x + 4, y, plein.z + dz);
            GuhNpcEntity npc = E2H4Vuur.npc(p, GuhNpcEntity.Kind.VUURNATIE_MIKA, dz < 0 ? a : b, dz < 0 ? -90f : 90f);
            if (npc != null) {
                npc.setNoGravity(true);
                Wacht w = new Wacht(npc, a, b);
                w.heen = dz < 0;
                wachten.add(w);
            }
        }
        SLUIPEN.put(p.getUUID(), wachten);
        E2H4Vuur.balk(p, "rotsblok.sluip");
    }

    static void tick(ServerPlayer p) {
        List<Wacht> wachten = SLUIPEN.get(p.getUUID());
        if (wachten == null) {
            return;
        }
        if (!Plek.in(p, "gevangenis") || Cutscenes.bezig(p)) {
            stop(p);
            return;
        }
        for (Wacht w : wachten) {
            if (w.npc.isRemoved()) {
                continue;
            }
            Vec3 doel = w.heen ? w.b : w.a;
            Vec3 d = doel.subtract(w.npc.position());
            if (d.horizontalDistance() < 0.2) {
                w.heen = !w.heen;
            } else {
                Vec3 stap = d.normalize().scale(TEMPO);
                float yaw = nl.juiced.guhs.feature.verhaal.Cutscene.yawVan(d);
                w.npc.snapTo(w.npc.getX() + stap.x, doel.y, w.npc.getZ() + stap.z, yaw, 0);
                w.npc.setYHeadRot(yaw);
                w.npc.setYBodyRot(yaw);
            }
            if (ziet(p, w)) {
                betrapt(p, w);
                return;
            }
        }
        if (Plek.bij(p, "cel_suki", 2.5)) {
            gelukt(p);
            return;
        }
        // (fix2-h4) the sneaking route lit up softly for you: over the roof, down, round the yard to the cell
        if (p.tickCount % 20 == 0) {
            for (Vec3 punt : Plek.pad(p, "inbraak_route")) {
                if (punt.distanceToSqr(p.position()) < 30 * 30) {
                    Schijn.deeltjes(p, ParticleTypes.WAX_OFF, punt.add(0, 0.3, 0), 3, 0.25, 0.0);
                }
            }
            Vec3 cel = Plek.staan(p, "cel_suki");
            if (cel != null) {
                Schijn.deeltjes(p, ParticleTypes.END_ROD, cel.add(0, 1.2, 0), 4, 0.2, 0.01);
            }
        }
    }

    /** Does this guard see the player now? */
    static boolean ziet(ServerPlayer p, Wacht w) {
        Vec3 naar = p.position().subtract(w.npc.position());
        double afstand = naar.horizontalDistance();
        if (afstand < VOELT && Math.abs(naar.y) < 2) {
            return true;
        }
        double ver = p.isShiftKeyDown() ? SLUIP_ZIET : ZIET;
        if (afstand > ver || Math.abs(naar.y) > 3) {
            return false;
        }
        Vec3 vlak = new Vec3(naar.x, 0, naar.z).normalize();
        if (vlak.dot(w.richting()) < 0.45) {
            return false;
        }
        Vec3 oog = w.npc.getEyePosition();
        return p.level().clip(new ClipContext(oog, p.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, w.npc)).getType()
                == HitResult.Type.MISS;
    }

    private static void betrapt(ServerPlayer p, Wacht w) {
        w.npc.speel("duw");
        Vec3 terug = Plek.staan(p, "rustpunt_6");
        ServerLevel g = Plek.guheinde(p);
        E2H4Vuur.balk(p, "rotsblok.betrapt");
        if (terug != null && g != null) {
            Duwtje.terug(p, g.dimension(), terug, 90f);
        }
    }

    /** At Suki's cell: 626-guh (wink 4, once per player), then the escape. */
    private static void gelukt(ServerPlayer p) {
        stop(p);
        Schijn.deeltjes(p, ParticleTypes.HAPPY_VILLAGER, p.position().add(0, 1, 0), 12, 0.6, 0.0);
        Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, p.blockPosition(), 0.8f, 1.3f);
        E2H4Vuur.titel(p, "rotsblok.titel_gevonden", null);
        Runnable ontsnap = () -> E2H4Vuur.speel(p, Scenes.KOKEND_ROTSBLOK, "gondel_onder", q -> {
            if (E2H4Vuur.VUUR.verder(q, E2H4Vuur.V_ROTSBLOK)) {
                Plek.breng(q, "rustpunt_5", 90f);
                E2H4Vuur.zeg(q, "rotsblok.gelukt");
                E2H4Vuur.zeg(q, "drakendans.hint");
            }
        });
        if (!Cutscenes.gezien(p, Scenes.KNIPOOG_STITCH.id()) && E2H4Vuur.speel(p, Scenes.KNIPOOG_STITCH, "knipoog_stitch_paneel", q -> ontsnap.run())) {
            return;
        }
        ontsnap.run();
    }

    static void stop(ServerPlayer p) {
        List<Wacht> wachten = SLUIPEN.remove(p.getUUID());
        if (wachten != null) {
            wachten.forEach(w -> w.npc.discard());
        }
    }
}
