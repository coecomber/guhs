package nl.juiced.guhs.feature.vadsrijk.npc;

import static nl.juiced.guhs.entity.GuhClothes.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef.Gedrag;

/**
 * All NPC guhs of zone 1 ({@link NpcDef}) and the keeper that puts them in the world: every second, for each NPC whose spot
 * is loaded and has a player within 96 blocks, it makes sure its one guh exists (code-spawned, never saved, ronde 10 /
 * contract 10: "NPCs/mobs/spawners are code"). Names/lines: tools/features/vadsrijk_npc.py.
 */
public final class Npcs {
    private static final Map<String, NpcDef> ALLE = new LinkedHashMap<>();
    /** def id -> the entity that stands for it now. */
    private static final Map<String, UUID> LEVEND = new HashMap<>();

    private Npcs() {
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, GuhClothes... kleren) {
        npc(id, yaw, v, g, rol, 1f, kleren);
    }

    private static void npc(String id, float yaw, GuhVariant v, Gedrag g, @Nullable String rol, float maat, GuhClothes... kleren) {
        BlockPos plek = NpcPlekken.NPC.containsKey(id) ? NpcPlekken.NPC.get(id) : NpcPlekken.KAMPWACHT.get(id);
        if (plek == null) {
            throw new IllegalStateException("no spot for npc " + id);
        }
        ALLE.put(id, new NpcDef(id, plek, yaw, v, List.of(kleren), g, rol, maat));
    }

    static {
        // --- Knuffelkamp ------------------------------------------------------------------------------------------------------
        npc("rekruteerder", 180, GuhVariant.NORMAL, Gedrag.STAAN, null, KNIGHT_HELMET, KNIGHT_ARMOUR, RED_BOWTIE);
        npc("kapitein", 0, GuhVariant.CHOCO, Gedrag.STAAN, "luchtschip", BALLONPET, BALLONBRIL, WINTER_SCARF);
        npc("kok", 90, GuhVariant.NORMAL, Gedrag.ZITTEN, null, 1.1f, CHEF_HAT, CHEF_JACKET);
        npc("priesteres", 180, GuhVariant.SNOW, Gedrag.STAAN, "klasse", HEMEL_AUREOOLTJE, STERRENCAPE, KNUS_SJAALTJE);
        npc("trainer_ridder", 180, GuhVariant.NORMAL, Gedrag.STAAN, "training", BESTAAND_WACHTERSHELM, BESTAAND_WACHTERSMANTEL);
        npc("trainer_jager", 180, GuhVariant.MINT, Gedrag.STAAN, "training", BOSWACHTERSHOED, BOSWACHTERSJAS);
        npc("trainer_magier", 0, GuhVariant.SNOW, Gedrag.STAAN, "training", WIZARD_HAT, WIZARD_ROBE, MONOCLE);
        npc("visser_dobber", 270, GuhVariant.CHOCO, Gedrag.ZITTEN, null, VISSERSHOEDJE, VISVEST, VIS_AAN_DE_HAAK);
        // --- Zwamzoet: order, trade, services -------------------------------------------------------------------------------
        npc("grootmeester", 180, GuhVariant.GOLDEN, Gedrag.STAAN, "ordetempel", 1.25f, STERRENKIJKERSMUTS, KONING_MANTEL, KONING_KETTING);
        npc("smid", 90, GuhVariant.CHOCO, Gedrag.STAAN, "smid", 1.15f, BUILDER_HELMET, GRILL_SCHORT, KAASMIJN_ZAKDOEK);
        npc("winkelier", 270, GuhVariant.NORMAL, Gedrag.STAAN, "winkel", SMUL_BAKKERSMUTS, SMUL_SCHORT);
        npc("wapenhandelaar", 90, GuhVariant.SNOW, Gedrag.STAAN, "wapenwinkel", KNIGHT_ARMOUR, EYEPATCH);
        npc("marktmeester", 270, GuhVariant.NORMAL, Gedrag.STAAN, "markt", KERMIS_HOED, KERMIS_JASJE, KERMIS_STRIK);
        npc("bankier", 270, GuhVariant.GOLDEN, Gedrag.STAAN, null, MONOCLE, BLACK_BOWTIE, DETECTIVE_JAS);
        npc("kleermaker", 90, GuhVariant.MINT, Gedrag.STAAN, "kleermaker", ZONNEHOEDJE, BURGEMEESTERSSJERP, KNUS_SJAALTJE);
        npc("klerk", 180, GuhVariant.NORMAL, Gedrag.STAAN, "questbord", BIEB_LEESBRIL, BIEB_VEST);
        npc("stalmeester", 270, GuhVariant.CHOCO, Gedrag.STAAN, "stal", BOERDERIJ_HOEDJE, OVERALLS, BOERDERIJ_ZAKDOEK);
        npc("havenmeester", 180, GuhVariant.NORMAL, Gedrag.STAAN, null, PIRATE_HAT, RAINCOAT);
        npc("luchtschipper", 180, GuhVariant.SNOW, Gedrag.STAAN, "luchtschip_kamp", BALLONPET, BALLONBRIL, CAMPINGMARKT_HALSDOEK);
        // the Mika-spion in disguise: only OUR fallback when the bazen slice's MikaSpion ("Akim de Kaasguh") is not in this tree
        npc("akim_kaasguh", 0, GuhVariant.CHOCO, Gedrag.STAAN, "marktguh", STRAW_HAT, SUNGLASSES, SAFETY_VEST);
        // --- Zwamzoet: citizens --------------------------------------------------------------------------------------------------
        npc("oma_truus", 90, GuhVariant.SNOW, Gedrag.SLENTER, null, THEEMUTSJE, KNUS_SJAALTJE, BIEB_LEESBRIL);
        npc("bakker_bram", 0, GuhVariant.NORMAL, Gedrag.STAAN, null, BAKKERSMUTSJE, BAKKERSSCHORTJE);
        npc("kleine_kiki", 180, GuhVariant.MINT, Gedrag.SLENTER, null, 0.75f, OORSTRIKJE_ROZE, ROMPERTJE);
        npc("dichter_daan", 180, GuhVariant.NORMAL, Gedrag.SLENTER, null, PIET_BERET, STRIPED_SWEATER);
        npc("postguh_pim", 0, GuhVariant.NORMAL, Gedrag.SLENTER, null, POLICE_CAP, CAMPINGMARKT_RUGZAK);
        npc("tuinier_tilly", 270, GuhVariant.MINT, Gedrag.SLENTER, null, TUINHOEDJE, TUINSCHORTJE);
        npc("muzikant_mo", 180, GuhVariant.CHOCO, Gedrag.STAAN, null, DISCO_KOPTELEFOONTJE, KERMIS_JASJE);
        npc("professor_pluis", 90, GuhVariant.SNOW, Gedrag.STAAN, null, DETECTIVE_VERGROOTGLAS, DOCTOR_COAT);
        npc("ridder_rammel", 270, GuhVariant.SNOW, Gedrag.SLENTER, null, 0.95f, KNIGHT_HELMET, ROYAL_CAPE);
        npc("kapster_fohnie", 90, GuhVariant.NORMAL, Gedrag.STAAN, null, KAPSEL_KRULLEN, KAPPERSCAPE);
        npc("schilder_sien", 180, GuhVariant.MINT, Gedrag.SLENTER, null, PIET_BERET, OVERALLS);
        npc("slaapkop_sjors", 90, GuhVariant.CHOCO, Gedrag.ZITTEN, null, SLAAPMUTSJE, PYJAMA_PAKJE);
        npc("koopman_kees", 0, GuhVariant.NORMAL, Gedrag.STAAN, null, CAMPINGMARKT_HOEDJE, CAMPINGMARKT_HALSDOEK, CAMPINGMARKT_RUGZAK);
        npc("weerguh_wolkje", 180, GuhVariant.SNOW, Gedrag.SLENTER, null, WOLKENMUTS, WOLKENKRAAG);
        npc("tweeling_tip", 90, GuhVariant.MINT, Gedrag.SLENTER, null, 0.8f, OORSTRIKJE_GEEL, SPELEN_SPORTSHIRTJE);
        npc("wachter_dikkie", 0, GuhVariant.NORMAL, Gedrag.STAAN, null, 1.2f, BESTAAND_WACHTERSHELM, KNIGHT_ARMOUR);
        // --- out in the zone ---------------------------------------------------------------------------------------------------
        npc("boswachter_mos", 90, GuhVariant.MINT, Gedrag.STAAN, null, BOSWACHTERSHOED, BOSWACHTERSJAS);
        npc("tweeling_tap", 0, GuhVariant.MINT, Gedrag.STAAN, null, 0.8f, OORSTRIKJE_MINT, SPELEN_SPORTSHIRTJE);
        npc("mijnwerker_mopje", 0, GuhVariant.CHOCO, Gedrag.STAAN, null, KAASMIJN_HELM, KAASMIJN_OVERALL);
        npc("tempelwachter_steeltje", 180, GuhVariant.SNOW, Gedrag.STAAN, null, 1.1f, BESTAAND_WACHTERSHELM, BESTAAND_WACHTERSMANTEL);
        // --- a ridder-guh at every campfire ---------------------------------------------------------------------------------------
        GuhVariant[] kleuren = {GuhVariant.NORMAL, GuhVariant.CHOCO, GuhVariant.SNOW, GuhVariant.MINT};
        int i = 0;
        for (String id : NpcPlekken.KAMPWACHT.keySet()) {
            npc(id, 180, kleuren[i++ % kleuren.length], Gedrag.STAAN, "kampwacht", KNIGHT_HELMET, KNIGHT_ARMOUR, WINTER_SCARF);
        }
    }

    /** Zone 2 (and later zones): add an NPC of your own (its spot in your own Plekken class); kept like zone 1's. */
    public static void voegToe(NpcDef d) {
        ALLE.put(d.id(), d);
    }

    public static java.util.Collection<NpcDef> alle() {
        return Collections.unmodifiableCollection(ALLE.values());
    }

    @Nullable
    public static NpcDef van(String id) {
        return ALLE.get(id);
    }

    /** The guh that stands for this NPC now (null: not in a loaded chunk). */
    @Nullable
    public static RijkGuh levend(ServerLevel sl, String id) {
        UUID u = LEVEND.get(id);
        if (u != null && sl.getEntity(u) instanceof RijkGuh g && g.isAlive()) {
            return g;
        }
        // U-DUBBEL: the one with this NPC's unique key (e.g. after a reload the map lost it): adopt it instead of spawning a second
        if (nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.zoek(sl, sleutel(id)) instanceof RijkGuh g && g.isAlive()) {
            LEVEND.put(id, g.getUUID());
            return g;
        }
        return null;
    }

    /** U-DUBBEL: the unique key of an NPC's guh. */
    public static String sleutel(String id) {
        return "npc:" + id;
    }

    // ---- keeping them in the world -------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 11) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.players().isEmpty()) {
            return;
        }
        for (NpcDef d : ALLE.values()) {
            // U-DUBBEL: not only "chunk loaded" but its entities loaded + visible, or a guh that is still coming back looks missing
            if (!nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, d.plek()) || (d.id().equals("akim_kaasguh") && NpcRollen.bestaat("mika_spion"))) {
                continue;
            }
            RijkGuh g = levend(sl, d.id());
            if (g == null) {
                if (spelerBij(sl, d.plek(), 96)) {
                    zet(sl, d);
                }
            } else if (g.volgt == null && !g.rust() && g.distanceToSqr(d.plek().getX() + 0.5, g.getY(), d.plek().getZ() + 0.5) > 24 * 24) {
                BlockPos s = staanPlek(sl, d.plek());
                g.snapTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, d.yaw(), 0);   // (wandered/escorted off: home)
            }
        }
    }

    private static boolean spelerBij(ServerLevel sl, BlockPos pos, double r) {
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(pos.getX() + 0.5, p.getY(), pos.getZ() + 0.5) < r * r) {
                return true;
            }
        }
        return false;
    }

    /** Spawns the guh of this NPC (any leftover of it goes first). */
    @Nullable
    public static RijkGuh zet(ServerLevel sl, NpcDef d) {
        RijkGuh oud = levend(sl, d.id());
        if (oud != null) {
            oud.discard();
        }
        RijkGuh g = NpcRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        if (g == null) {
            return null;
        }
        BlockPos s = staanPlek(sl, d.plek());
        g.snapTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, d.yaw(), 0);
        g.setYHeadRot(d.yaw());
        g.setYBodyRot(d.yaw());
        g.stelIn(d);
        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(g, sleutel(d.id()));   // U-DUBBEL
        if (!staanOp(sl, s) && Double.isNaN(vloerOfNaN(sl, d.plek()))) {
            g.setNoGravity(true);   // (no floor built here yet: hover instead of falling into the void)
        }
        sl.addFreshEntity(g);
        LEVEND.put(d.id(), g.getUUID());
        return g;
    }

    /** The y to stand on in the spot's own column: the spot's exact y, else down (max 4) first, then the nearest floor within 12
     * blocks either way (the spot's own y when there is none). */
    public static double vloer(ServerLevel sl, BlockPos pos) {
        double y = vloerOfNaN(sl, pos);
        return Double.isNaN(y) ? pos.getY() : y;
    }

    /** Fix round 2 (Bakker Bram stood on his stall's roof): where an NPC of this spot stands. The exact spot first; when that is
     * blocked (a counter, a wall), the free floor beside it at the SAME y (within 2 blocks, inside the building/stall) before
     * the column search, which tries downward before upward, so a spot never ends up on the roof above it. */
    public static BlockPos staanPlek(ServerLevel sl, BlockPos pos) {
        // g-int: ONE rule for g-kaart's staanPlek + g-ux's standplek: the exact spot first; when that cell is blocked (counter,
        // wall, ground) the nearest free LIT floor at the same y within 2 (ring 1, then ring 2: inside the stall/house, never the
        // dark pocket under it); then the column: down 1..4 first, then the nearest lit floor up/down within 12 (vloerOfNaN).
        if (staanOp(sl, pos)) {
            return pos;
        }
        boolean geblokt = !sl.getBlockState(pos).getCollisionShape(sl, pos).isEmpty()
                || !sl.getBlockState(pos.above()).getCollisionShape(sl, pos.above()).isEmpty();
        for (int r = 1; geblokt && r <= 2; r++) {
            BlockPos beste = null;
            double bd = Double.MAX_VALUE;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos b = pos.offset(dx, 0, dz);
                    double dd = dx * dx + dz * dz;
                    if (dd < bd && staanOp(sl, b) && sl.getMaxLocalRawBrightness(b) > 0) {
                        beste = b;
                        bd = dd;
                    }
                }
            }
            if (beste != null) {
                return beste;
            }
        }
        double y = vloerOfNaN(sl, pos);
        return Double.isNaN(y) ? pos : new BlockPos(pos.getX(), (int) y, pos.getZ());
    }

    private static double vloerOfNaN(ServerLevel sl, BlockPos pos) {
        if (staanOp(sl, pos)) {
            return pos.getY();
        }
        // (integration: the map's terrain is hollowed, sealed air pockets are pitch dark; away from the exact spot only lit floors)
        for (int d = 1; d <= 4; d++) {   // (fix round 2: down first, the floor of the room the spot is in)
            BlockPos b = pos.below(d);
            if (staanOp(sl, b) && sl.getMaxLocalRawBrightness(b) > 0) {
                return b.getY();
            }
        }
        for (int d = 1; d <= 12; d++) {
            for (int s : new int[] {-1, 1}) {
                if (s < 0 && d <= 4) {
                    continue;
                }
                BlockPos b = pos.above(s * d);
                if (staanOp(sl, b) && sl.getMaxLocalRawBrightness(b) > 0) {
                    return b.getY();
                }
            }
        }
        return Double.NaN;
    }

    private static boolean staanOp(ServerLevel sl, BlockPos b) {
        BlockState onder = sl.getBlockState(b.below());
        return !onder.getCollisionShape(sl, b.below()).isEmpty() && sl.getBlockState(b).getCollisionShape(sl, b).isEmpty()
                && sl.getBlockState(b.above()).getCollisionShape(sl, b.above()).isEmpty();
    }

    /** Dev: every NPC away (they come back within a second when a player is near). */
    static int wisAlles(ServerLevel sl) {
        int n = 0;
        for (String id : new ArrayList<>(LEVEND.keySet())) {
            RijkGuh g = levend(sl, id);
            if (g != null) {
                g.discard();
                n++;
            }
        }
        LEVEND.clear();
        return n;
    }
}
