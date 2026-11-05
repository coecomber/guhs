package nl.juiced.guhs.feature.vadsrijk.ambacht;

import static nl.juiced.guhs.entity.GuhClothes.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtPlekken.Werkplaats;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.economie.Post;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * The workshop guhs (A3: "werkplaatsen in de hubs met een leermeester-guh en een receptenboek-scherm") and the board guh of each
 * hub, plus the short story per ambacht (A2/C: "een verhaaltje per ambacht, met een leermeester-guh").
 * <ul>
 *   <li>Placement: by data ({@link AmbachtPlekken#WERKPLAATSEN}: hub POI ids), the exact spots found at runtime in a ring round the
 *   hub's square (free, standable floor), once a player is near; they join the npc slice's keeper ({@link Npcs#voegToe}).</li>
 *   <li>A click (npc's {@code QuestMotor.KLIK_VOOR}): a talking screen with the choices Werkplaats / Receptenboek / Verhaal / Dag
 *   (gathering masters: Waar? instead of the workshop; the board guh: Bestelbord / Ambachtsbord).</li>
 *   <li>The story: 5 steps per ambacht at ambacht level {@link #VERHAAL_LEVELS}; 3 pages each ({@code vadsrijk.ambacht.verhaal.<a>.<step>.<0-2>});
 *   rewards: munten + a bit of xp, and on the last step (lv 50) a keepsake for home by the Post (decoration only).</li>
 * </ul>
 */
public final class AmbachtNpcs {
    public static final String SLEUTEL = "vr_amb";
    public static final int[] VERHAAL_LEVELS = {1, 10, 20, 35, 50};
    static final int WERK = 1, BOEK = 2, VERHAAL = 3, DAG = 4, WAAR = 5, BESTEL = 6, RANG = 7;
    private static final Map<String, BlockPos> GEZET = new ConcurrentHashMap<>();
    /** Who a player is talking to. */
    private static final Map<UUID, String> PRAAT = new ConcurrentHashMap<>();

    private AmbachtNpcs() {
    }

    /** The workshop role word of an ambacht (NPC ids {@code amb_<rol>_z<n>}, lang {@code vadsrijk.ambacht.npc.<rol>.*}). */
    public static String rol(@Nullable Ambacht a) {
        if (a == null) {
            return "bord";
        }
        return switch (a) {
            case SMEDEN -> "smid";
            case NAAIEN -> "naai";
            case KOKEN -> "kok";
            case BROUWEN -> "brouw";
            case MIJNEN -> "mijn";
            case HOUTHAKKEN -> "hout";
            case PLUKKEN -> "pluk";
            case VISSEN -> "vis";
            case IMKEREN -> "imker";
        };
    }

    static GuhVariant variant(Werkplaats w) {
        GuhVariant[] v = {GuhVariant.CHOCO, GuhVariant.MINT, GuhVariant.NORMAL, GuhVariant.SNOW, GuhVariant.GOLDEN};
        return v[Math.floorMod(w.npc().hashCode(), v.length)];
    }

    static List<GuhClothes> kleren(@Nullable Ambacht a) {
        if (a == null) {
            return List.of(BIEB_LEESBRIL, BIEB_VEST);
        }
        return switch (a) {
            case SMEDEN -> List.of(BUILDER_HELMET, GRILL_SCHORT);
            case NAAIEN -> List.of(ZONNEHOEDJE, KNUS_SJAALTJE);
            case KOKEN -> List.of(CHEF_HAT, CHEF_JACKET);
            case BROUWEN -> List.of(WIZARD_HAT, MONOCLE);
            case MIJNEN -> List.of(KAASMIJN_HELM, KAASMIJN_OVERALL);
            case HOUTHAKKEN -> List.of(BOSWACHTERSHOED, BOSWACHTERSJAS);
            case PLUKKEN -> List.of(TUINHOEDJE, TUINSCHORTJE);
            case VISSEN -> List.of(VISSERSHOEDJE, VISVEST);
            case IMKEREN -> List.of(STRAW_HAT, TUINSCHORTJE);
        };
    }

    // ---- placing -------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 40 != 13) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || sl.players().isEmpty()) {
            return;
        }
        Map<String, List<Werkplaats>> perHub = new HashMap<>();
        for (Werkplaats w : AmbachtPlekken.WERKPLAATSEN) {
            if (!GEZET.containsKey(w.npc())) {
                perHub.computeIfAbsent(w.zone() + ":" + w.poi(), k -> new ArrayList<>()).add(w);
            }
        }
        for (List<Werkplaats> hub : perHub.values()) {
            BlockPos a = hub.get(0).anker();
            if (a == null || !PlekWereld.geladen(sl, a, 20) || !spelerBij(sl, a)) {
                continue;
            }
            plaats(sl, a, hub);
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

    /** Finds a ring of free spots round the hub square and adds the NPCs (all of this hub's workshops not placed yet). */
    static void plaats(ServerLevel sl, BlockPos a, List<Werkplaats> hub) {
        List<BlockPos> gekozen = new ArrayList<>(GEZET.values());
        int i = 0;
        for (Werkplaats w : hub) {
            BlockPos plek = null;
            for (int poging = 0; poging < 48 && plek == null; poging++) {
                double hoek = (Math.floorMod(w.npc().hashCode(), 360) + poging * 37) * Math.PI / 180, r = 7 + (poging % 10);
                BlockPos c = a.offset((int) Math.round(Math.cos(hoek) * r), 0, (int) Math.round(Math.sin(hoek) * r));
                BlockPos s = Npcs.staanPlek(sl, c);
                if (Math.abs(s.getY() - a.getY()) > 6 || !sl.getFluidState(s).isEmpty() || !sl.getBlockState(s.below()).isCollisionShapeFullBlock(sl, s.below())
                        || !sl.getBlockState(s).getCollisionShape(sl, s).isEmpty()) {
                    continue;
                }
                boolean vrij = true;
                for (BlockPos g : gekozen) {
                    vrij &= g.distSqr(s) >= 9;
                }
                if (vrij) {
                    plek = s;
                }
            }
            if (plek == null) {
                plek = Npcs.staanPlek(sl, a.offset(3 + i, 0, 3));   // (never stuck without a guh: next to the square's centre)
            }
            gekozen.add(plek);
            GEZET.put(w.npc(), plek);
            float yaw = (float) (Math.toDegrees(Math.atan2(a.getZ() - plek.getZ(), a.getX() - plek.getX())) - 90);
            Npcs.voegToe(new NpcDef(w.npc(), plek, yaw, variant(w), kleren(w.ambacht()), NpcDef.Gedrag.STAAN, "ambacht", 1f));
            i++;
        }
    }

    @Nullable
    public static BlockPos waar(String npc) {
        return GEZET.get(npc);
    }

    /** Dev: place again (after a terrain change). */
    static void vergeet() {
        GEZET.clear();
    }

    // ---- talking -------------------------------------------------------------------------------------------------------------

    /** QuestMotor.KLIK_VOOR: our NPCs. */
    static boolean klik(ServerPlayer p, RijkGuh npc) {
        Werkplaats w = AmbachtPlekken.werkplaats(npc.npcId());
        if (w == null) {
            return false;
        }
        praat(p, npc, w);
        return true;
    }

    /** Opens the talk (also the dev command, without an entity: then the pages show the narrator). */
    public static void praat(ServerPlayer p, @Nullable Entity npc, Werkplaats w) {
        PRAAT.put(p.getUUID(), w.npc());
        String r = rol(w.ambacht());
        List<Praat.Regel> regels = new ArrayList<>();
        String naam = npc == null ? "vadsrijk.npc.naam." + w.npc() : "";
        regels.add(new Praat.Regel(npc, naam, "vadsrijk.ambacht.npc." + r + ".hallo"));
        List<Praat.Optie> opties = new ArrayList<>();
        if (w.ambacht() == null) {
            opties.add(new Praat.Optie(BESTEL, "vadsrijk.ambacht.optie.bestel"));
            opties.add(new Praat.Optie(RANG, "vadsrijk.ambacht.optie.rang"));
        } else {
            opties.add(new Praat.Optie(w.ambacht().verzamel ? WAAR : WERK, w.ambacht().verzamel ? "vadsrijk.ambacht.optie.waar" : "vadsrijk.ambacht.optie.werk"));
            opties.add(new Praat.Optie(BOEK, "vadsrijk.ambacht.optie.boek"));
            opties.add(new Praat.Optie(VERHAAL, "vadsrijk.ambacht.optie.verhaal"));
        }
        opties.add(new Praat.Optie(DAG, "vadsrijk.ambacht.optie.dag"));
        Praat.scene(p, SLEUTEL, regels, opties.toArray(new Praat.Optie[0]));
    }

    static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        String id = PRAAT.get(p.getUUID());
        Werkplaats w = id == null ? null : AmbachtPlekken.werkplaats(id);
        if (w == null || optie < 0) {
            return;
        }
        switch (optie) {
            case WERK -> AmbachtSchermen.werkplaats(p, w.ambacht(), w.npc());
            case BOEK -> AmbachtSchermen.boek(p, "recepten", w.ambacht());
            case BESTEL -> AmbachtSchermen.boek(p, "bestel", null);
            case RANG -> AmbachtSchermen.boek(p, "rang", null);
            case WAAR -> waar(p, spreker, w);
            case VERHAAL -> verhaal(p, spreker, w);
            default -> Praat.sluit(p);
        }
    }

    /** The gathering masters: where the nearest spots of their ambacht are. */
    static void waar(ServerPlayer p, @Nullable Entity spreker, Werkplaats w) {
        AmbachtPlekken.Groep beste = null;
        double bd = Double.MAX_VALUE;
        int tier = AmbachtXp.tierVoor(AmbachtData.level(p, w.ambacht()));
        for (AmbachtPlekken.Groep g : AmbachtPlekken.GROEPEN) {
            BlockPos a = g.anker();
            if (g.ambacht() != w.ambacht() || a == null || g.tier() > Math.max(tier, w.ambacht() == Ambacht.IMKEREN ? 3 : 1)) {
                continue;
            }
            double d = a.distSqr(p.blockPosition()) - (g.tier() == tier ? 1e7 : 0);   // (your own tier first)
            if (d < bd) {
                bd = d;
                beste = g;
            }
        }
        List<Praat.Regel> regels = new ArrayList<>();
        String naam = spreker == null ? "vadsrijk.npc.naam." + w.npc() : "";
        regels.add(new Praat.Regel(spreker, naam, "vadsrijk.ambacht.npc." + rol(w.ambacht()) + ".uitleg"));
        if (beste != null) {
            BlockPos a = beste.anker();
            regels.add(new Praat.Regel(spreker, naam, "vadsrijk.ambacht.npc.waar", Component.translatable("vadsrijk.ambacht.groep." + beste.id()), a.getX(), a.getZ()));
        }
        Praat.scene(p, SLEUTEL, regels);
    }

    /** The next story step, when your level allows it. */
    static void verhaal(ServerPlayer p, @Nullable Entity spreker, Werkplaats w) {
        Ambacht a = w.ambacht();
        int stap = AmbachtData.verhaal(p, a);
        String naam = spreker == null ? "vadsrijk.npc.naam." + w.npc() : "";
        List<Praat.Regel> regels = new ArrayList<>();
        if (stap >= VERHAAL_LEVELS.length) {
            regels.add(new Praat.Regel(spreker, naam, "vadsrijk.ambacht.verhaal.uit"));
            Praat.scene(p, SLEUTEL, regels);
            return;
        }
        int nodig = VERHAAL_LEVELS[stap];
        if (AmbachtData.level(p, a) < nodig) {
            regels.add(new Praat.Regel(spreker, naam, "vadsrijk.ambacht.verhaal.wacht", nodig, Component.translatable(a.naamKey())));
            Praat.scene(p, SLEUTEL, regels);
            return;
        }
        String k = "vadsrijk.ambacht.verhaal." + a.id + "." + stap + ".";
        for (int i = 0; i < 3; i++) {
            regels.add(new Praat.Regel(i == 1 ? p : spreker, i == 1 ? "vadsrijk.npc.jij" : naam, k + i));
        }
        Praat.scene(p, SLEUTEL, regels);
        beloon(p, a, stap);
    }

    /** A story step's reward (also the dev command). */
    static void beloon(ServerPlayer p, Ambacht a, int stap) {
        AmbachtData.zetVerhaal(p, a, stap + 1);
        Spelers.geefMunten(p, 100L * (stap + 1));
        AmbachtData.geefXp(p, a, Math.round(AmbachtXp.xpVoor(AmbachtData.level(p, a)) * 0.15), false);
        if (stap == 0 && a.verzamel && a != Ambacht.IMKEREN) {
            for (Materialen.Materiaal m : Materialen.van(a, 1)) {
                MateriaalTas.geef(p, Materialen.stack(m.id(), 1, 3));
            }
        }
        if (stap == VERHAAL_LEVELS.length - 1) {
            ItemStack aandenken = aandenken(a);
            Post.stuur(p, aandenken);
            p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.verhaal.post", aandenken.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.verhaal.stap", Component.translatable(a.naamKey()), stap + 1, VERHAAL_LEVELS.length)
                .withStyle(ChatFormatting.GOLD));
        EcoUtil.geluid(p, net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.3f);
    }

    /** The keepsake for home at the end of a story (decoration only, A2). */
    static ItemStack aandenken(Ambacht a) {
        String post = switch (a) {
            case KOKEN -> "kokschort";
            case VISSEN -> "vissershoedje";
            case PLUKKEN -> "tuinhoedje";
            case IMKEREN -> "imkershoed";
            case MIJNEN, SMEDEN -> "mijnhelm";
            case HOUTHAKKEN -> "opperzwambeeld";
            case NAAIEN -> "klodderknuffel";
            case BROUWEN -> "mini_koning";
        };
        return Recepten.POST.get(post).get();
    }

    static void vergeetSpeler(UUID id) {
        PRAAT.remove(id);
    }

    /** All the NPC ids (lang keys in the generator). */
    public static Set<String> ids() {
        java.util.LinkedHashSet<String> s = new java.util.LinkedHashSet<>();
        for (Werkplaats w : AmbachtPlekken.WERKPLAATSEN) {
            s.add(w.npc());
        }
        return s;
    }
}
