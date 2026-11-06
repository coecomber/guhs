package nl.juiced.guhs.feature.vadsrijk.gilden;

import static nl.juiced.guhs.entity.GuhClothes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.vadsrijk.zone5.wereld.Z5WereldPlekken;

/**
 * U-GILDEN: the bond NPCs.
 * <ul>
 *   <li>{@code bond_klerk} "Bondsklerk Zegeltje" in Hoogvads' chancellery (POI {@code paleis_kanselarij_bureau}): founding a bond
 *   (only here: zone 5), what a bond is, the board;</li>
 *   <li>{@code bond_poortwachter} at the Bondenpoort in the Staartpark (POI {@code staartpark_prieel}): into the Bondenvallei;</li>
 *   <li>{@code bond_pleinwachter} on the valley square: back to Hoogvads, to your castle;</li>
 *   <li>{@code bond_bord_<hub>}, the Bondenbord guh in every hub town (Knuffelkamp, Zwamzoet, Kandijkom, Honingtak, Lampionhaven,
 *   Hoogvads + the valley): the bonds that look for members (V1: "rekruteren/joinen vanaf zone 1"), your bond, to your castle.
 *   Placed at runtime on a free spot round the hub's square (like t-ambacht's board guhs).</li>
 * </ul>
 */
public final class BondNpcs {
    public static final String SLEUTEL = "vr_gilden";
    static final int DAG = 0, OPRICHTEN = 1, UITLEG = 2, BORD = 3, MIJN = 4, VALLEI = 5, HOOGVADS = 6, KASTEEL = 7;
    public static final String KLERK = "bond_klerk", POORT = "bond_poortwachter", PLEIN = "bond_pleinwachter";
    private static final Map<UUID, String> PRAAT = new ConcurrentHashMap<>();
    private static final Map<String, BlockPos> GEZET = new ConcurrentHashMap<>();
    private static boolean klaar;

    /** The board guh per hub: npc id -> POI supplier (the hub's square). */
    static final Map<String, java.util.function.Supplier<BlockPos>> BORDEN = new java.util.LinkedHashMap<>();

    static {
        for (BondPaneel.Hub h : BondPaneel.HUBS) {
            BORDEN.put("bond_bord_" + h.id(), h.plek());
        }
    }

    private BondNpcs() {
    }

    static synchronized void init() {
        if (klaar) {
            return;
        }
        klaar = true;
        BlockPos klerk = Z5WereldPlekken.plek("paleis_kanselarij_bureau");
        npc(KLERK, klerk != null ? klerk : new BlockPos(2843, 233, -557), 90, GuhVariant.SNOW, BIEB_LEESBRIL, BIEB_VEST, KONING_KETTING);
        npc(POORT, Vallei.hoogvadsPoort(), 180, GuhVariant.GOLDEN, KNIGHT_HELMET, ROYAL_CAPE);
        npc(PLEIN, new BlockPos(Vallei.X0 + 4, Vallei.Y + 1, Vallei.Z0 - 18), 180, GuhVariant.GOLDEN, KNIGHT_HELMET, ROYAL_CAPE);
        npc("bond_bord_vallei", new BlockPos(Vallei.X0 - 4, Vallei.Y + 1, Vallei.Z0 - 18), 180, GuhVariant.MINT, BIEB_HOED, BIEB_VEST);
    }

    private static void npc(String id, BlockPos plek, float yaw, GuhVariant v, GuhClothes... kleren) {
        Npcs.voegToe(new NpcDef(id, plek, yaw, v, List.of(kleren), NpcDef.Gedrag.STAAN, null, 1f));
        GEZET.put(id, plek);
    }

    public static boolean isBord(String npc) {
        return npc.startsWith("bond_bord_");
    }

    @Nullable
    public static BlockPos waar(String npc) {
        return GEZET.get(npc);
    }

    // ---- placing the board guhs ------------------------------------------------------------------------------------------------------

    static void tik(MinecraftServer server) {
        if (server.getTickCount() % 40 != 21) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.players().isEmpty()) {
            return;
        }
        for (Map.Entry<String, java.util.function.Supplier<BlockPos>> e : BORDEN.entrySet()) {
            if (GEZET.containsKey(e.getKey())) {
                continue;
            }
            BlockPos a = e.getValue().get();
            if (a == null || !sl.isLoaded(a) || !spelerBij(sl, a)) {
                continue;
            }
            BlockPos plek = zoekPlek(sl, a, e.getKey());
            float yaw = (float) (Math.toDegrees(Math.atan2(a.getZ() - plek.getZ(), a.getX() - plek.getX())) - 90);
            npc(e.getKey(), plek, yaw, GuhVariant.MINT, BIEB_HOED, BIEB_VEST);
        }
    }

    private static boolean spelerBij(ServerLevel sl, BlockPos a) {
        for (ServerPlayer p : sl.players()) {
            if (p.blockPosition().distSqr(a) < 96 * 96) {
                return true;
            }
        }
        return false;
    }

    /** A free, standable floor 6..14 blocks from the square's centre, at least 3 blocks from every other NPC. */
    static BlockPos zoekPlek(ServerLevel sl, BlockPos a, String id) {
        List<BlockPos> anderen = new ArrayList<>();
        for (NpcDef d : Npcs.alle()) {
            if (d.plek().distSqr(a) < 40 * 40) {
                anderen.add(d.plek());
            }
        }
        for (int poging = 0; poging < 60; poging++) {
            double hoek = (Math.floorMod(id.hashCode() * 7, 360) + poging * 41) * Math.PI / 180, r = 6 + (poging % 9);
            BlockPos c = a.offset((int) Math.round(Math.cos(hoek) * r), 0, (int) Math.round(Math.sin(hoek) * r));
            BlockPos s = Npcs.staanPlek(sl, c);
            if (Math.abs(s.getY() - a.getY()) > 5 || !sl.getFluidState(s).isEmpty() || !sl.getBlockState(s.below()).isCollisionShapeFullBlock(sl, s.below())
                    || !sl.getBlockState(s).getCollisionShape(sl, s).isEmpty() || !sl.getBlockState(s.above()).getCollisionShape(sl, s.above()).isEmpty()) {
                continue;
            }
            boolean vrij = true;
            for (BlockPos o : anderen) {
                vrij &= o.distSqr(s) >= 9;
            }
            if (vrij) {
                return s;
            }
        }
        return Npcs.staanPlek(sl, a.offset(-3, 0, 3));
    }

    // ---- talking ------------------------------------------------------------------------------------------------------------------

    /** QuestMotor.KLIK_VOOR. */
    static boolean klik(ServerPlayer p, RijkGuh npc) {
        String id = npc.npcId();
        if (id == null || !id.startsWith("bond_")) {
            return false;
        }
        praat(p, npc, id);
        return true;
    }

    public static void praat(ServerPlayer p, @Nullable Entity npc, String id) {
        PRAAT.put(p.getUUID(), id);
        String rol = isBord(id) ? "bord" : id.substring("bond_".length());
        String naam = npc == null ? "vadsrijk.npc.naam." + id : "";
        List<Praat.Regel> regels = new ArrayList<>();
        regels.add(new Praat.Regel(npc, naam, "vadsrijk.gilden.npc." + rol + ".hallo"));
        List<Praat.Optie> o = new ArrayList<>();
        switch (rol) {
            case "klerk" -> {
                o.add(new Praat.Optie(OPRICHTEN, "vadsrijk.gilden.optie.oprichten"));
                o.add(new Praat.Optie(UITLEG, "vadsrijk.gilden.optie.uitleg"));
                o.add(new Praat.Optie(BORD, "vadsrijk.gilden.optie.bord"));
            }
            case "poortwachter" -> o.add(new Praat.Optie(VALLEI, "vadsrijk.gilden.optie.vallei"));
            case "pleinwachter" -> {
                o.add(new Praat.Optie(HOOGVADS, "vadsrijk.gilden.optie.hoogvads"));
                o.add(new Praat.Optie(KASTEEL, "vadsrijk.gilden.optie.kasteel"));
            }
            default -> {
                o.add(new Praat.Optie(BORD, "vadsrijk.gilden.optie.bord"));
                o.add(new Praat.Optie(KASTEEL, "vadsrijk.gilden.optie.kasteel"));
            }
        }
        o.add(new Praat.Optie(MIJN, "vadsrijk.gilden.optie.mijn"));
        o.add(new Praat.Optie(DAG, "vadsrijk.gilden.optie.dag"));
        Praat.scene(p, SLEUTEL, regels, o.toArray(new Praat.Optie[0]));
    }

    static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        String id = PRAAT.get(p.getUUID());
        if (id == null || optie < 0) {
            return;
        }
        String naam = spreker == null ? "vadsrijk.npc.naam." + id : "";
        switch (optie) {
            case OPRICHTEN -> {
                List<Praat.Regel> r = new ArrayList<>();
                if (Bonden.van(p) != null) {
                    r.add(new Praat.Regel(spreker, naam, "vadsrijk.gilden.npc.klerk.al"));
                } else if (!BondActies.magOprichten(p)) {
                    r.add(new Praat.Regel(spreker, naam, "vadsrijk.gilden.npc.klerk.te_vroeg", BondActies.MIN_LEVEL));
                } else {
                    r.add(new Praat.Regel(spreker, naam, "vadsrijk.gilden.npc.klerk.oprichten", nl.juiced.guhs.feature.vadsrijk.kern.Spelers.munten(BondActies.PRIJS)));
                    p.sendSystemMessage(BondPaneel.rij(Component.translatable("vadsrijk.gilden.npc.klerk.typ").withStyle(ChatFormatting.LIGHT_PURPLE),
                            BondPaneel.typ("vadsrijk.gilden.knop.oprichten", "/bond maak TAG Naam van je bond")));
                }
                Praat.scene(p, SLEUTEL, r);
            }
            case UITLEG -> {
                List<Praat.Regel> r = new ArrayList<>();
                for (int i = 1; i <= 4; i++) {
                    r.add(new Praat.Regel(spreker, naam, "vadsrijk.gilden.uitleg." + i, nl.juiced.guhs.feature.vadsrijk.kern.Spelers.munten(BondActies.PRIJS),
                            BondActies.MIN_LEVEL));
                }
                Praat.scene(p, SLEUTEL, r);
            }
            case BORD -> {
                Praat.sluit(p);
                BondPaneel.bord(p);
            }
            case MIJN -> {
                Praat.sluit(p);
                BondPaneel.toon(p);
            }
            case VALLEI -> {
                Praat.sluit(p);
                Vallei.naarVallei(p);
            }
            case HOOGVADS -> {
                Praat.sluit(p);
                Vallei.naarHoogvads(p);
            }
            case KASTEEL -> {
                Praat.sluit(p);
                BondTeleport.meld(p, BondTeleport.thuis(p, false));
            }
            default -> Praat.sluit(p);
        }
    }

    /** Is p within reach of the Bondsklerk (for {@code /bond maak})? */
    static boolean bijKlerk(ServerPlayer p) {
        BlockPos k = GEZET.get(KLERK);
        if (k != null && Vadsrijk.in(p) && p.blockPosition().distSqr(k) <= 12 * 12) {
            return true;
        }
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        RijkGuh g = sl == null ? null : Npcs.levend(sl, KLERK);
        return g != null && g.distanceTo(p) <= 12;
    }

    /** All the NPC ids (names/lines in the generator). */
    public static List<String> ids() {
        List<String> l = new ArrayList<>(List.of(KLERK, POORT, PLEIN, "bond_bord_vallei"));
        l.addAll(BORDEN.keySet());
        return l;
    }

    static void vergeet(UUID id) {
        PRAAT.remove(id);
    }
}
