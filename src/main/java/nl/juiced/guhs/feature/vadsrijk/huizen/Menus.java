package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Huis;
import nl.juiced.guhs.feature.vadsrijk.huizen.WijkPlan.Kavel;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.registry.ModBlocks;

/**
 * U-HUIZEN: every talking menu of the housing (the Vadsrijk talking screen, sleutel {@value #SLEUTEL}):
 * <ul>
 *   <li>the Kwartiermeester in every hub town: to your house / to the ward (choose a plot) / visit a friend / how it works;</li>
 *   <li>the "te koop" sign of a free plot: pick a size, then a look, then it is yours;</li>
 *   <li>the mailbox of a house: owner = post, door, friends, bigger house, other look, sell; others = whose house this is;</li>
 *   <li>the notice board on the square: who lives where, how many plots are free;</li>
 *   <li>Meubelmeester Mollie at the market: Guhs furniture for Vadsmunten (straight into your home inventory).</li>
 * </ul>
 */
public final class Menus {
    public static final String SLEUTEL = "vr_kk";
    static final int DAG = 0, NAAR_HUIS = 1, NAAR_WIJK = 2, BEZOEK = 3, UITLEG = 4, TERUG = 5;
    static final int KOOP_SOORT = 10, KOOP_VARIANT = 20;
    static final int POST = 30, DEUR = 31, VRIENDEN = 32, GROTER = 33, UITERLIJK = 34, VERKOOP = 35, VERKOOP_JA = 36, VRIEND_HIER = 37,
            VRIEND_INRICHTEN = 38, VRIEND_WEG = 39, NAAR_STAD = 40;
    static final int UITERLIJK_KIES = 50, LIJST = 100, MOLLIE_KOOP = 200, MOLLIE_MEER = 199;
    static final int PER_PAGINA = 6;

    /** What a player is choosing now (the plot of the sign, the size, the list behind the numbered options). */
    private static final class Staat {
        String wat = "";
        int kavel = -1, soort = -1, pagina;
        final List<Integer> lijst = new ArrayList<>();
    }

    private static final Map<UUID, Staat> STAAT = new ConcurrentHashMap<>();

    /** Mollie's wares: the Guhs furniture and deco (block items), price in brons. */
    record Waar(Supplier<ItemStack> item, long prijs) {
    }

    static final List<Waar> WAREN = new ArrayList<>();

    static {
        WAREN.add(new Waar(() -> new ItemStack(ModBlocks.GUH_STOEL.get()), 40));
        WAREN.add(new Waar(() -> new ItemStack(ModBlocks.GUH_TAFEL.get()), 60));
        WAREN.add(new Waar(() -> new ItemStack(ModBlocks.GUH_BANK.get()), 80));
        WAREN.add(new Waar(() -> new ItemStack(ModBlocks.ZITZAKKEN.get(DyeColor.PINK).get()), 50));
        WAREN.add(new Waar(() -> new ItemStack(ModBlocks.KUSSENS.get(DyeColor.MAGENTA).get()), 25));
        WAREN.add(new Waar(() -> new ItemStack(ModBlocks.VLAGGETJES.get()), 30));
        WAREN.add(new Waar(() -> new ItemStack(ModBlocks.GUH_TAART.get()), 35));
        WAREN.add(new Waar(() -> new ItemStack(ModBlocks.ROZE_GUHBLOEM.get(), 4), 20));
        WAREN.add(new Waar(() -> new ItemStack(ModBlocks.GUHOORTJES.get(), 4), 20));
        WAREN.add(new Waar(() -> new ItemStack(Items.LANTERN, 4), 30));
        WAREN.add(new Waar(() -> new ItemStack(Items.PINK_CARPET, 8), 20));
        WAREN.add(new Waar(() -> new ItemStack(Items.PINK_BED), 60));
        WAREN.add(new Waar(() -> new ItemStack(Items.FLOWER_POT, 4), 15));
        WAREN.add(new Waar(() -> new ItemStack(Items.CHERRY_PLANKS, 32), 30));
        WAREN.add(new Waar(() -> new ItemStack(Items.PINK_STAINED_GLASS_PANE, 16), 30));
        WAREN.add(new Waar(() -> new ItemStack(Items.BOOKSHELF, 4), 40));
        WAREN.add(new Waar(() -> new ItemStack(EcoSpul.VADSRIJK_VAANDEL.get()), 300));
    }

    private Menus() {
    }

    static Staat staat(ServerPlayer p) {
        return STAAT.computeIfAbsent(p.getUUID(), x -> new Staat());
    }

    static void vergeet(UUID id) {
        STAAT.remove(id);
    }

    private static Praat.Regel regel(@Nullable Entity spreker, String naamKey, String tekst, Object... args) {
        return new Praat.Regel(spreker, naamKey, tekst, args);
    }

    // ---- the Kwartiermeester (hub towns) -----------------------------------------------------------------------------------------

    public static void poort(ServerPlayer p, @Nullable Entity npc) {
        HuizenData d = Kwartier.data(p.level().getServer());
        Staat s = staat(p);
        s.wat = "poort";
        List<Praat.Optie> o = new ArrayList<>();
        Integer eigen = d.sleutelVan(p.getUUID());
        if (eigen != null) {
            o.add(new Praat.Optie(NAAR_HUIS, "vadsrijk.huizen.optie.naar_huis"));
        }
        o.add(new Praat.Optie(NAAR_WIJK, eigen == null ? "vadsrijk.huizen.optie.naar_wijk_kies" : "vadsrijk.huizen.optie.naar_wijk"));
        o.add(new Praat.Optie(BEZOEK, "vadsrijk.huizen.optie.bezoek"));
        o.add(new Praat.Optie(UITLEG, "vadsrijk.huizen.optie.uitleg"));
        o.add(new Praat.Optie(DAG, "vadsrijk.huizen.optie.dag"));
        String naam = npc == null ? "vadsrijk.npc.naam.kk_poort" : "";
        Praat.scene(p, SLEUTEL, List.of(regel(npc, naam, eigen == null ? "vadsrijk.huizen.poort.hallo" : "vadsrijk.huizen.poort.hallo_eigen")),
                o.toArray(new Praat.Optie[0]));
    }

    static void bezoekLijst(ServerPlayer p, @Nullable Entity spreker) {
        MinecraftServer server = p.level().getServer();
        HuizenData d = Kwartier.data(server);
        Staat s = staat(p);
        s.wat = "bezoek";
        s.lijst.clear();
        for (Integer k : d.vriendVan(p.getUUID())) {
            s.lijst.add(k);
        }
        for (ServerPlayer o : server.getPlayerList().getPlayers()) {
            Integer k = d.sleutelVan(o.getUUID());
            if (k != null && !o.getUUID().equals(p.getUUID()) && !s.lijst.contains(k)) {
                s.lijst.add(k);
            }
        }
        List<Praat.Optie> o = new ArrayList<>();
        for (int i = 0; i < s.lijst.size() && i < PER_PAGINA; i++) {
            Huis h = d.huis(s.lijst.get(i));
            o.add(new Praat.Optie(LIJST + i, h == null ? "?" : h.naam));
        }
        o.add(new Praat.Optie(DAG, "vadsrijk.huizen.optie.dag"));
        Praat.scene(p, SLEUTEL, List.of(regel(spreker, spreker == null ? "vadsrijk.npc.naam.kk_poort" : "",
                s.lijst.isEmpty() ? "vadsrijk.huizen.bezoek.niemand" : "vadsrijk.huizen.bezoek.wie")), o.toArray(new Praat.Optie[0]));
    }

    // ---- the "te koop" sign -----------------------------------------------------------------------------------------------------

    static void teKoop(ServerPlayer p, Kavel k) {
        HuizenData d = Kwartier.data(p.level().getServer());
        Staat s = staat(p);
        s.wat = "koop";
        s.kavel = k.sleutel();
        if (d.huisVan(p.getUUID()) != null) {
            Praat.scene(p, SLEUTEL, List.of(regel(null, "vadsrijk.huizen.naam.makelaar", "vadsrijk.huizen.koop.al_een_uitleg")));
            return;
        }
        List<Praat.Optie> o = new ArrayList<>();
        MutableComponent prijzen = Component.empty();
        for (HuisSoort hs : HuisSoort.values()) {
            o.add(new Praat.Optie(KOOP_SOORT + hs.ordinal(), hs.naamKey()));
            prijzen.append(Component.translatable("vadsrijk.huizen.koop.regel", Component.translatable(hs.naamKey()), Spelers.munten(hs.prijs), hs.level,
                    hs.maxDeco)).append("\n");
        }
        o.add(new Praat.Optie(DAG, "vadsrijk.huizen.optie.dag"));
        Praat.scene(p, SLEUTEL, List.of(regel(null, "vadsrijk.huizen.naam.makelaar", "vadsrijk.huizen.koop.hallo", k.nr() + 1, k.wijk() + 1, prijzen,
                Spelers.munten(Spelers.data(p).munten), Spelers.level(p))), o.toArray(new Praat.Optie[0]));
    }

    static void kiesVariant(ServerPlayer p, HuisSoort hs) {
        Staat s = staat(p);
        s.soort = hs.ordinal();
        List<Praat.Optie> o = new ArrayList<>();
        for (int i = 0; i < Ontwerp.PALETTEN.size(); i++) {
            o.add(new Praat.Optie(KOOP_VARIANT + i, Ontwerp.palet(i).naamKey()));
        }
        o.add(new Praat.Optie(DAG, "vadsrijk.huizen.optie.dag"));
        Praat.scene(p, SLEUTEL, List.of(regel(null, "vadsrijk.huizen.naam.makelaar", "vadsrijk.huizen.koop.variant", Component.translatable(hs.naamKey()))),
                o.toArray(new Praat.Optie[0]));
    }

    // ---- the mailbox --------------------------------------------------------------------------------------------------------------

    static void post(ServerPlayer p, Kavel k, Huis h) {
        Staat s = staat(p);
        s.wat = "post";
        s.kavel = k.sleutel();
        if (!h.eigenaar.equals(p.getUUID())) {
            Praat.scene(p, SLEUTEL, List.of(regel(null, "vadsrijk.huizen.naam.brievenbus", "vadsrijk.huizen.post.van", h.naam,
                    Component.translatable(h.soort.naamKey()), Component.translatable(h.deur.key()),
                    Component.translatable(Rechten.rol(h, p.getUUID()) == Rechten.Rol.VRIEND ? "vadsrijk.huizen.rol.vriend" : "vadsrijk.huizen.rol.bezoeker"))));
            return;
        }
        Figuranten.bijwerkenVan(p, h);
        List<Praat.Optie> o = new ArrayList<>();
        o.add(new Praat.Optie(POST, "vadsrijk.huizen.optie.post"));
        o.add(new Praat.Optie(DEUR, "vadsrijk.huizen.optie.deur"));
        o.add(new Praat.Optie(VRIENDEN, "vadsrijk.huizen.optie.vrienden"));
        if (h.soort.groter() != null) {
            o.add(new Praat.Optie(GROTER, "vadsrijk.huizen.optie.groter"));
        }
        o.add(new Praat.Optie(UITERLIJK, "vadsrijk.huizen.optie.uiterlijk"));
        o.add(new Praat.Optie(VERKOOP, "vadsrijk.huizen.optie.verkoop"));
        o.add(new Praat.Optie(NAAR_STAD, "vadsrijk.huizen.optie.naar_stad"));
        o.add(new Praat.Optie(DAG, "vadsrijk.huizen.optie.dag"));
        Object groter = h.soort.groter() == null ? Component.literal("-")
                : Component.translatable("vadsrijk.huizen.post.groter", Component.translatable(h.soort.groter().naamKey()),
                Spelers.munten(h.soort.groter().prijs - h.soort.prijs), h.soort.groter().level);
        Praat.scene(p, SLEUTEL, List.of(regel(null, "vadsrijk.huizen.naam.brievenbus", "vadsrijk.huizen.post.eigen", Kwartier.postAantal(p),
                Component.translatable(h.deur.key()), h.vrienden.size(), h.geplaatst.size(), h.soort.maxDeco, RustXp.pot(p), groter)),
                o.toArray(new Praat.Optie[0]));
    }

    static void vriendenMenu(ServerPlayer p, Huis h) {
        Staat s = staat(p);
        s.wat = "vrienden";
        List<Praat.Optie> o = new ArrayList<>();
        o.add(new Praat.Optie(VRIEND_HIER, "vadsrijk.huizen.optie.vriend_hier"));
        o.add(new Praat.Optie(VRIEND_INRICHTEN, h.vriendenInrichten ? "vadsrijk.huizen.optie.inrichten_uit" : "vadsrijk.huizen.optie.inrichten_aan"));
        if (!h.vrienden.isEmpty()) {
            o.add(new Praat.Optie(VRIEND_WEG, "vadsrijk.huizen.optie.vrienden_weg"));
        }
        o.add(new Praat.Optie(DAG, "vadsrijk.huizen.optie.dag"));
        String namen = h.vrienden.isEmpty() ? "-" : String.join(", ", h.vrienden.values());
        Praat.scene(p, SLEUTEL, List.of(regel(null, "vadsrijk.huizen.naam.brievenbus", "vadsrijk.huizen.vrienden.lijst", namen,
                Component.translatable(h.vriendenInrichten ? "vadsrijk.huizen.ja" : "vadsrijk.huizen.nee"))), o.toArray(new Praat.Optie[0]));
    }

    // ---- the notice board ---------------------------------------------------------------------------------------------------------

    static void prikbord(ServerPlayer p, int wijk) {
        HuizenData d = Kwartier.data(p.level().getServer());
        MutableComponent bewoners = Component.empty();
        int n = 0;
        for (int i = 0; i < WijkPlan.KAVELS; i++) {
            Huis h = d.huis(wijk * WijkPlan.KAVELS + i);
            if (h != null) {
                bewoners.append(Component.translatable("vadsrijk.huizen.prikbord.regel", i + 1, h.naam, Component.translatable(h.soort.naamKey()))).append("\n");
                n++;
            }
        }
        if (n == 0) {
            bewoners.append(Component.translatable("vadsrijk.huizen.prikbord.leeg"));
        }
        List<Praat.Regel> r = new ArrayList<>();
        r.add(regel(null, "vadsrijk.huizen.naam.prikbord", "vadsrijk.huizen.prikbord.kop", wijk + 1, WijkPlan.KAVELS - n, d.wijken));
        r.add(regel(null, "vadsrijk.huizen.naam.prikbord", "vadsrijk.huizen.prikbord.bewoners", bewoners));
        r.add(regel(null, "vadsrijk.huizen.naam.prikbord", "vadsrijk.huizen.prikbord.tip"));
        Praat.scene(p, SLEUTEL, r);
    }

    // ---- Mollie -------------------------------------------------------------------------------------------------------------------

    static void mollie(ServerPlayer p, @Nullable Entity npc, int pagina) {
        Staat s = staat(p);
        s.wat = "mollie";
        int aantal = (WAREN.size() + PER_PAGINA - 1) / PER_PAGINA;
        s.pagina = Math.floorMod(pagina, aantal);
        List<Praat.Optie> o = new ArrayList<>();
        MutableComponent prijzen = Component.empty();
        for (int i = s.pagina * PER_PAGINA; i < Math.min(WAREN.size(), (s.pagina + 1) * PER_PAGINA); i++) {
            ItemStack it = WAREN.get(i).item().get();
            o.add(new Praat.Optie(MOLLIE_KOOP + i, it.getItem().getDescriptionId()));
            prijzen.append(Component.translatable("vadsrijk.huizen.mollie.regel", it.getCount(), it.getHoverName(), Spelers.munten(WAREN.get(i).prijs()))).append("\n");
        }
        if (aantal > 1) {
            o.add(new Praat.Optie(MOLLIE_MEER, "vadsrijk.huizen.optie.meer"));
        }
        o.add(new Praat.Optie(DAG, "vadsrijk.huizen.optie.dag"));
        String naam = npc == null ? "vadsrijk.npc.naam." + Figuranten.MOLLIE : "";
        Praat.scene(p, SLEUTEL, List.of(regel(npc, naam, "vadsrijk.huizen.mollie.hallo", prijzen, Spelers.munten(Spelers.data(p).munten))),
                o.toArray(new Praat.Optie[0]));
    }

    /** Buys ware {@code i} from Mollie. Returns false (with a message) when it can't. */
    static boolean koopBijMollie(ServerPlayer p, int i) {
        if (i < 0 || i >= WAREN.size()) {
            return false;
        }
        if (Regels.nogNietGewisseld(p) || !Kwartier.in(p)) {
            return false;
        }
        Waar w = WAREN.get(i);
        if (!Spelers.neemMunten(p, w.prijs())) {
            EcoUtil.zeg(p, "vadsrijk.huizen.koop.munten", Spelers.munten(w.prijs()));
            return false;
        }
        ItemStack it = w.item().get();
        if (!p.getInventory().add(it)) {
            p.drop(it, false);
        }
        EcoUtil.geluid(p, SoundEvents.VILLAGER_YES, 0.6f, 1.6f);
        return true;
    }

    // ---- the answers --------------------------------------------------------------------------------------------------------------

    static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        Staat s = STAAT.get(p.getUUID());
        if (s == null || optie < 0) {
            return;
        }
        MinecraftServer server = p.level().getServer();
        HuizenData d = Kwartier.data(server);
        Integer eigen = d.sleutelVan(p.getUUID());
        Huis h = eigen == null ? null : d.huis(eigen);
        switch (optie) {
            case DAG -> Praat.sluit(p);
            case NAAR_HUIS -> {
                Praat.sluit(p);
                if (eigen != null) {
                    Kwartier.naarHuis(p, eigen);
                }
            }
            case NAAR_WIJK -> {
                Praat.sluit(p);
                Kwartier.naarWijk(p, eigen != null ? eigen / WijkPlan.KAVELS : d.wijkMetPlek());
            }
            case BEZOEK -> bezoekLijst(p, spreker);
            case UITLEG -> Praat.scene(p, SLEUTEL, List.of(regel(spreker, spreker == null ? "vadsrijk.npc.naam.kk_poort" : "", "vadsrijk.huizen.uitleg.1"),
                    regel(spreker, spreker == null ? "vadsrijk.npc.naam.kk_poort" : "", "vadsrijk.huizen.uitleg.2"),
                    regel(spreker, spreker == null ? "vadsrijk.npc.naam.kk_poort" : "", "vadsrijk.huizen.uitleg.3")));
            case NAAR_STAD -> {
                Praat.sluit(p);
                Kwartier.terug(p);
            }
            case POST -> {
                int n = Kwartier.haalPost(p);
                Praat.sluit(p);
                p.sendSystemMessage(Component.translatable(n > 0 ? "vadsrijk.huizen.post.gehaald" : "vadsrijk.huizen.post.leeg", n).withStyle(ChatFormatting.GOLD));
            }
            case DEUR -> {
                if (h != null) {
                    h.deur = h.deur.volgende();
                    d.setDirty();
                    ServerLevel l = Kwartier.level(server);
                    if (l != null && h.deur != HuizenData.Deur.OPEN) {
                        Regels.deurDicht(l, WijkPlan.kavel(eigen), h);
                    }
                    p.sendOverlayMessage(Component.translatable("vadsrijk.huizen.deur.nu", Component.translatable(h.deur.key())).withStyle(ChatFormatting.LIGHT_PURPLE));
                    post(p, WijkPlan.kavel(eigen), h);
                }
            }
            case VRIENDEN -> {
                if (h != null) {
                    vriendenMenu(p, h);
                }
            }
            case VRIEND_HIER -> {
                if (h != null) {
                    int n = 0;
                    for (ServerPlayer o : ((ServerLevel) p.level()).players()) {
                        if (!o.getUUID().equals(p.getUUID()) && o.distanceTo(p) < 10 && voegVriendToe(d, h, o)) {
                            n++;
                            o.sendSystemMessage(Component.translatable("vadsrijk.huizen.vrienden.jij", h.naam).withStyle(ChatFormatting.LIGHT_PURPLE));
                        }
                    }
                    p.sendOverlayMessage(Component.translatable("vadsrijk.huizen.vrienden.erbij", n).withStyle(ChatFormatting.LIGHT_PURPLE));
                    vriendenMenu(p, h);
                }
            }
            case VRIEND_INRICHTEN -> {
                if (h != null) {
                    h.vriendenInrichten = !h.vriendenInrichten;
                    d.setDirty();
                    vriendenMenu(p, h);
                }
            }
            case VRIEND_WEG -> {
                if (h != null) {
                    h.vrienden.clear();
                    d.setDirty();
                    vriendenMenu(p, h);
                }
            }
            case GROTER -> {
                Praat.sluit(p);
                Kwartier.vergroot(p);
            }
            case UITERLIJK -> {
                List<Praat.Optie> o = new ArrayList<>();
                for (int i = 0; i < Ontwerp.PALETTEN.size(); i++) {
                    o.add(new Praat.Optie(UITERLIJK_KIES + i, Ontwerp.palet(i).naamKey()));
                }
                o.add(new Praat.Optie(DAG, "vadsrijk.huizen.optie.dag"));
                Praat.scene(p, SLEUTEL, List.of(regel(null, "vadsrijk.huizen.naam.brievenbus", "vadsrijk.huizen.post.uiterlijk")), o.toArray(new Praat.Optie[0]));
            }
            case VERKOOP -> Praat.scene(p, SLEUTEL, List.of(regel(null, "vadsrijk.huizen.naam.brievenbus", "vadsrijk.huizen.post.verkoop_zeker",
                    Spelers.munten(h == null ? 0 : h.betaald / 2))), new Praat.Optie(VERKOOP_JA, "vadsrijk.huizen.optie.verkoop_ja"),
                    new Praat.Optie(DAG, "vadsrijk.huizen.optie.nee"));
            case VERKOOP_JA -> {
                Praat.sluit(p);
                Kwartier.verkoop(p);
            }
            case MOLLIE_MEER -> mollie(p, spreker, s.pagina + 1);
            default -> {
                if (optie >= KOOP_SOORT && optie < KOOP_SOORT + HuisSoort.values().length && s.wat.equals("koop")) {
                    kiesVariant(p, HuisSoort.values()[optie - KOOP_SOORT]);
                } else if (optie >= KOOP_VARIANT && optie < KOOP_VARIANT + Ontwerp.PALETTEN.size() && s.wat.equals("koop") && s.soort >= 0 && s.kavel >= 0) {
                    Praat.sluit(p);
                    Kwartier.koop(p, s.kavel, HuisSoort.values()[s.soort], optie - KOOP_VARIANT, false);
                } else if (optie >= UITERLIJK_KIES && optie < UITERLIJK_KIES + Ontwerp.PALETTEN.size()) {
                    Praat.sluit(p);
                    Kwartier.uiterlijk(p, optie - UITERLIJK_KIES);
                } else if (optie >= LIJST && optie < LIJST + PER_PAGINA && s.wat.equals("bezoek") && optie - LIJST < s.lijst.size()) {
                    Praat.sluit(p);
                    Kwartier.naarHuis(p, s.lijst.get(optie - LIJST));
                } else if (optie >= MOLLIE_KOOP && optie < MOLLIE_KOOP + WAREN.size()) {
                    koopBijMollie(p, optie - MOLLIE_KOOP);
                    mollie(p, spreker, s.pagina);
                }
            }
        }
    }

    /** Adds a friend (true when new). */
    static boolean voegVriendToe(HuizenData d, Huis h, ServerPlayer vriend) {
        if (h.eigenaar.equals(vriend.getUUID()) || h.vrienden.containsKey(vriend.getUUID())) {
            return false;
        }
        h.vrienden.put(vriend.getUUID(), vriend.getGameProfile().name());
        d.setDirty();
        return true;
    }

    /** QuestMotor.KLIK_VOOR: the Kwartiermeesters and Mollie. */
    static boolean klik(ServerPlayer p, RijkGuh npc) {
        String id = npc.npcId();
        if (id.startsWith(Poorten.PREFIX)) {
            poort(p, npc);
            return true;
        }
        if (id.equals(Figuranten.MOLLIE)) {
            mollie(p, npc, 0);
            return true;
        }
        return false;
    }
}
