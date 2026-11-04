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
        npc("bakker_bram", 270, GuhVariant.NORMAL, Gedrag.STAAN, null, BAKKERSMUTSJE, BAKKERSSCHORTJE);
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
        return u != null && sl.getEntity(u) instanceof RijkGuh g && g.isAlive() ? g : null;
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
            if (!sl.isLoaded(d.plek()) || (d.id().equals("akim_kaasguh") && NpcRollen.bestaat("mika_spion"))) {
                continue;
            }
            RijkGuh g = levend(sl, d.id());
            if (g == null) {
                if (spelerBij(sl, d.plek(), 96)) {
                    zet(sl, d);
                }
            } else if (g.volgt == null && !g.rust() && g.distanceToSqr(d.plek().getX() + 0.5, g.getY(), d.plek().getZ() + 0.5) > 24 * 24) {
                BlockPos s = standplek(sl, d.plek());
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
        BlockPos s = standplek(sl, d.plek());
        g.snapTo(s.getX() + 0.5, s.getY(), s.getZ() + 0.5, d.yaw(), 0);
        g.setYHeadRot(d.yaw());
        g.setYBodyRot(d.yaw());
        g.stelIn(d);
        if (!staanOp(sl, s) && Double.isNaN(vloerOfNaN(sl, d.plek()))) {
            g.setNoGravity(true);   // (no floor built here yet: hover instead of falling into the void)
        }
        sl.addFreshEntity(g);
        LEVEND.put(d.id(), g.getUUID());
        return g;
    }

    /**
     * Where an NPC stands (fix round 2, g-ux; shared by zone 1 and 2): the spot itself when it is free with a floor; when the spot
     * lies INSIDE a block (a wall, the ground: a spot data error) first a free, lit spot right next to it (ring 1..3, same height
     * +-1), so it never drops into a dark pocket under a house; else the nearest lit floor up/down within 12 ({@link #vloer}).
     */
    public static BlockPos standplek(ServerLevel sl, BlockPos pos) {
        if (staanOp(sl, pos)) {
            return pos;
        }
        if (!sl.getBlockState(pos).getCollisionShape(sl, pos).isEmpty()) {
            for (int r = 1; r <= 3; r++) {
                for (int dy : new int[] {0, 1, -1}) {
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dz = -r; dz <= r; dz++) {
                            if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                                continue;
                            }
                            BlockPos b = pos.offset(dx, dy, dz);
                            if (staanOp(sl, b) && sl.getMaxLocalRawBrightness(b) > 0) {
                                return b;
                            }
                        }
                    }
                }
            }
        }
        double y = vloerOfNaN(sl, pos);
        return Double.isNaN(y) ? pos : new BlockPos(pos.getX(), (int) y, pos.getZ());
    }

    /** The y to stand on: the nearest floor within 12 blocks of the spot's y (the spot's own y when there is none). */
    public static double vloer(ServerLevel sl, BlockPos pos) {
        double y = vloerOfNaN(sl, pos);
        return Double.isNaN(y) ? pos.getY() : y;
    }

    private static double vloerOfNaN(ServerLevel sl, BlockPos pos) {
        for (int d = 0; d <= 12; d++) {
            for (int s : new int[] {-1, 1}) {
                BlockPos b = pos.above(s * d);
                // (integration: the map's terrain is hollowed, sealed air pockets are pitch dark; away from the exact spot only lit floors)
                if (staanOp(sl, b) && (d == 0 || sl.getMaxLocalRawBrightness(b) > 0)) {
                    return b.getY();
                }
                if (d == 0) {
                    break;
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
