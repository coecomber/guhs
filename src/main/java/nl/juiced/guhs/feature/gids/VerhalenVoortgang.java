package nl.juiced.guhs.feature.gids;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.balto.BaltoFeature;
import nl.juiced.guhs.feature.balto.BaltoVerhaal;
import nl.juiced.guhs.feature.band.BandFeature;
import nl.juiced.guhs.feature.barbecuether.Grillguh;
import nl.juiced.guhs.feature.beroepen.BeroepenVoortgang;
import nl.juiced.guhs.feature.guheinde.GuheindeEvents;
import nl.juiced.guhs.feature.guheinde.GuheindeGevecht;
import nl.juiced.guhs.feature.guhwaii.Ohana;
import nl.juiced.guhs.feature.hemel.HemelQuest;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.mewtwo.MewtwoFeature;
import nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang;
import nl.juiced.guhs.feature.timmerguh.Timmerguh;
import nl.juiced.guhs.feature.timmerguh.TimmerguhVoortgang;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;

/**
 * The Guhdex tab "Verhalen" (server side): for every questline with per-player steps, where this player is
 * ({@link VerhaalStand}). Everything is read from the player's own progress (GuhQuests.saved / GuhWorldData per player
 * UUID / advancements), never from a structure, so it is the same at every Nomguh, every Knuffeldal, every chapel...
 * <p>
 * The texts are lang keys {@code gui.guhs.verhalen.<id>.(naam|uitleg|stap.<i>|nu.<sleutel>|waar.<sleutel>)}, where the
 * sleutel is the step number, "klaar", or a variant of a step (e.g. "3_bewoner": the guh lives in the huisje, now go and
 * tell the Timmerguh). {@link #sleutels} lists every sleutel per questline (the game test checks each exists in the lang).
 * Resources: tools/features/gids_verhalen.py.
 * <p>
 * bbq2: besides the hard-coded lines below, every registered {@link nl.juiced.guhs.feature.verhaal.Verhaallijn} is a
 * questline here by itself (shown first, group by group: {@link nl.juiced.guhs.feature.verhaal.Verhaallijnen}); its step
 * count is added to {@link #STAPPEN} when it registers.
 */
public final class VerhalenVoortgang {
    /** The questlines of 3.0 (Guhverhalen): shown first, under their own heading. */
    public static final List<String> NIEUW = List.of("timmerguh", "balto", "mewtwo", "hemel", "guhwaii");
    /** The older adventures with steps. */
    public static final List<String> OUD = List.of("vadsig", "slee", "knusfeest", "guheinde", "grillguh",
            "beroep_brandweer", "beroep_politie", "beroep_apotheek", "beroep_bouw");

    /** Every sleutel of nu/waar per questline (besides "0".."stappen-1" and "klaar"). */
    public static final java.util.Map<String, List<String>> EXTRA = java.util.Map.of(
            "timmerguh", List.of("3_bewoner", "4_knus"),
            "balto", List.of("klaar_tem"),
            "mewtwo", List.of("2_inbouwen", "klaar_tem"),
            "guhwaii", List.of("klaar_tem"),
            "knusfeest", List.of());
    /** How many steps each questline has (bbq2: a registered Verhaallijn adds itself, see Verhaallijnen.voegToe). */
    public static final java.util.Map<String, Integer> STAPPEN = new java.util.concurrent.ConcurrentHashMap<>(java.util.Map.ofEntries(
            java.util.Map.entry("timmerguh", 5), java.util.Map.entry("balto", 7), java.util.Map.entry("mewtwo", 4),
            java.util.Map.entry("hemel", 2), java.util.Map.entry("guhwaii", 6), java.util.Map.entry("vadsig", 4),
            java.util.Map.entry("slee", 2), java.util.Map.entry("knusfeest", 3), java.util.Map.entry("guheinde", 8),
            java.util.Map.entry("grillguh", 4), java.util.Map.entry("beroep_brandweer", 3), java.util.Map.entry("beroep_politie", 3),
            java.util.Map.entry("beroep_apotheek", 3), java.util.Map.entry("beroep_bouw", 3)));

    /** Every sleutel of this questline (the test checks nu.&lt;s&gt; and waar.&lt;s&gt; exist for each). */
    public static List<String> sleutels(String id) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < STAPPEN.get(id); i++) {
            out.add(String.valueOf(i));
        }
        out.add("klaar");
        out.addAll(EXTRA.getOrDefault(id, List.of()));
        nl.juiced.guhs.feature.verhaal.Verhaallijn lijn = nl.juiced.guhs.feature.verhaal.Verhaallijnen.van(id);
        if (lijn != null) {
            out.addAll(lijn.extraSleutels());
        }
        return out;
    }

    /** All questlines of this player, in display order. */
    public static List<VerhaalStand> alle(ServerPlayer p) {
        List<VerhaalStand> out = new ArrayList<>();
        for (nl.juiced.guhs.feature.verhaal.Verhaallijn l : nl.juiced.guhs.feature.verhaal.Verhaallijnen.alle()) {
            out.add(l.stand(p));   // (bbq2: the registered lines first)
        }
        out.add(timmerguh(p));
        out.add(balto(p));
        out.add(mewtwo(p));
        out.add(hemel(p));
        out.add(guhwaii(p));
        out.add(vadsig(p));
        out.add(slee(p));
        out.add(knusfeest(p));
        out.add(guheinde(p));
        out.add(grillguh(p));
        for (BeroepenVoortgang.Beroep b : BeroepenVoortgang.Beroep.values()) {
            out.add(beroep(p, b));
        }
        return out;
    }

    /** One questline by id (for tests), or null. */
    public static VerhaalStand van(ServerPlayer p, String id) {
        for (VerhaalStand v : alle(p)) {
            if (v.id().equals(id)) {
                return v;
            }
        }
        return null;
    }

    // =====================================================================================================================
    // 3.0: the Guhverhalen
    // =====================================================================================================================

    static VerhaalStand timmerguh(ServerPlayer p) {
        int stap = TimmerguhVoortgang.stap(p);
        String sleutel = String.valueOf(stap);
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (stap == TimmerguhVoortgang.MATERIAAL) {
            nodig.add(new VerhaalStand.Nodig("minecraft:oak_planks", Component.translatable("gui.guhs.verhalen.ding.planken"),
                    tel(p, ItemTags.PLANKS), Timmerguh.PLANKEN));
            nodig.add(new VerhaalStand.Nodig("minecraft:pink_wool", Component.translatable("gui.guhs.verhalen.ding.roze_wol"),
                    Math.min(Timmerguh.WOL, GuhQuests.count(p, Items.PINK_WOOL)), Timmerguh.WOL));
        } else if (stap == TimmerguhVoortgang.DAK) {
            nodig.add(nodig("guhs:timmerguh_dakpluisje", GuhQuests.count(p, item("guhs:timmerguh_dakpluisje")), 1));
        } else if (stap == TimmerguhVoortgang.BEWONER && Timmerguh.heeftBewoner(p)) {
            sleutel = "3_bewoner";
        } else if (stap == TimmerguhVoortgang.KLAAR && Timmerguh.isKnus(p)) {
            sleutel = "4_knus";
        }
        List<VerhaalStand.Beloning> bel = List.of(
                beloning("guhs:guhhuisje_klein", stap >= TimmerguhVoortgang.BEWONER),
                beloning("guhs:timmerguh_bouwboekje", stap >= TimmerguhVoortgang.KLAAR),
                beloning(kleding(GuhClothes.TIMMER_HELMPJE), stap >= TimmerguhVoortgang.KLAAR),
                beloning(kleding(GuhClothes.TIMMER_GEREEDSCHAPSRIEM), stap >= TimmerguhVoortgang.KNUS));
        boolean begonnen = stap > 0 || GidsFeature.heeft(p, "verhalen/timmerguh_bouwplaats");
        return stand("timmerguh", "guhs:timmerguh_bouwboekje", stap, begonnen, sleutel, nodig, bel);
    }

    static VerhaalStand balto(ServerPlayer p) {
        int stap = BaltoVerhaal.stap(p);
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (stap == BaltoVerhaal.AANGEKOMEN) {
            nodig.add(nodig("guhs:nomguh_medicijnkist", GuhQuests.count(p, BaltoFeature.MEDICIJNKIST_ITEM.get()), 1));
        }
        boolean klaar = stap >= BaltoVerhaal.KLAAR;
        String sleutel = klaar && VerhaalGuhs.magTemmen(p, VerhaalGuh.BALTOGUH) ? "klaar_tem" : String.valueOf(stap);
        List<VerhaalStand.Beloning> bel = new ArrayList<>();
        bel.add(beloning("guhs:sneeuwslee", klaar));
        bel.add(beloning("guhs:baltoguh_beeldje", klaar));
        bel.add(new VerhaalStand.Beloning(itemId(ModItems.clothingItem(BaltoFeature.KLEDING.get(0))),
                Component.translatable("gui.guhs.verhalen.beloning.kleertjes", String.valueOf(BaltoFeature.KLEDING.size())), klaar));
        bel.add(new VerhaalStand.Beloning("guhs:baltoguh_beeldje", Component.translatable("gui.guhs.verhalen.beloning.held_van_nomguh"), BaltoVerhaal.isHeld(p)));
        bel.add(new VerhaalStand.Beloning("guhs:sneeuwslee", Component.translatable("gui.guhs.verhalen.beloning.sledesprint"), klaar));
        bel.add(getemd(p, VerhaalGuh.BALTOGUH));
        return stand("balto", "guhs:baltoguh_beeldje", stap, stap > 0, sleutel, nodig, bel);
    }

    static VerhaalStand mewtwo(ServerPlayer p) {
        int stap = MewtwoVoortgang.stap(p);
        String sleutel = String.valueOf(stap);
        List<Object> args = new ArrayList<>();
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (stap == MewtwoVoortgang.NOTITIES) {
            nodig.add(nodig("guhs:mewtwo_labnotitie", MewtwoVoortgang.aantalNotities(p), MewtwoFeature.NOTITIES));
            for (int n = 1; n <= MewtwoFeature.NOTITIES; n++) {
                if (!MewtwoVoortgang.heeftNotitie(p, n)) {
                    args.add(Component.translatable("gui.guhs.mewtwo.plek.notitie." + n));
                    break;
                }
            }
            if (args.isEmpty()) {
                args.add(Component.translatable("gui.guhs.verhalen.mewtwo.alle_notities"));
            }
        } else if (stap == MewtwoVoortgang.ONDERDELEN) {
            nodig.add(nodig("guhs:mewtwo_tankonderdeel", MewtwoVoortgang.aantalIngebouwd(p), MewtwoFeature.ONDERDELEN));
            for (int n = 1; n <= MewtwoFeature.ONDERDELEN; n++) {
                if (!MewtwoVoortgang.heeftOnderdeel(p, n) && !MewtwoVoortgang.isIngebouwd(p, n)) {
                    args.add(Component.translatable("gui.guhs.mewtwo.plek.onderdeel." + n));
                    break;
                }
            }
            if (args.isEmpty()) {
                sleutel = "2_inbouwen";
            }
        } else if (stap == MewtwoVoortgang.MAALTIJD) {
            Item knabbels = item("guhs:kaas_knabbels");
            nodig.add(nodig("guhs:kaas_knabbels", Math.min(MewtwoFeature.PORTIE_KNABBELS, MewtwoVoortgang.knabbels(p) + GuhQuests.count(p, knabbels)),
                    MewtwoFeature.PORTIE_KNABBELS));
            Item snack = eerste(BandFeature.SNACKS, Items.COOKIE);
            nodig.add(new VerhaalStand.Nodig(itemId(snack), Component.translatable("gui.guhs.verhalen.ding.snacks"),
                    Math.min(MewtwoFeature.PORTIE_SNACKS, MewtwoVoortgang.snacks(p) + tel(p, BandFeature.SNACKS)), MewtwoFeature.PORTIE_SNACKS));
        }
        boolean klaar = stap >= MewtwoVoortgang.KLAAR;
        if (klaar && VerhaalGuhs.magTemmen(p, VerhaalGuh.MEWTWO)) {
            sleutel = "klaar_tem";
        }
        List<VerhaalStand.Beloning> bel = List.of(
                beloning(kleding(GuhClothes.MEWTWO_TRAINERPETJE), stap >= MewtwoVoortgang.ONDERDELEN),
                beloning(kleding(GuhClothes.MEWTWO_TRAINERPAKJE), stap >= MewtwoVoortgang.MAALTIJD),
                beloning(kleding(GuhClothes.MEWTWO_STAARTJE), klaar),
                beloning(kleding(GuhClothes.MEW_BALLONNETJE), klaar),
                getemd(p, VerhaalGuh.MEWTWO));
        return stand("mewtwo", "guhs:mewtwo_labnotitie", stap, stap > 0, sleutel, nodig, bel, args.toArray());
    }

    static VerhaalStand hemel(ServerPlayer p) {
        boolean klopt = HemelQuest.klopt(p);
        int stap = klopt ? 2 : Math.min(1, HemelQuest.stap(p));
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (stap == 1) {
            for (HemelQuest.Ding d : HemelQuest.Ding.values()) {
                int heb = HemelQuest.heeft(p, d) ? 1 : Math.min(1, GuhQuests.count(p, d.item()));
                nodig.add(new VerhaalStand.Nodig(itemId(d.item()), Component.translatable(d.item().getDescriptionId()), heb, 1));
            }
        }
        List<VerhaalStand.Beloning> bel = List.of(
                beloning(kleding(GuhClothes.HEMEL_AUREOOLTJE), klopt),
                beloning(kleding(GuhClothes.HEMEL_WOLKENVLEUGELTJES), klopt),
                new VerhaalStand.Beloning("guhs:pluisveertje", Component.translatable("gui.guhs.verhalen.beloning.wolkjes", String.valueOf(HemelQuest.terug(p))), klopt));
        boolean begonnen = stap > 0 || GidsFeature.heeft(p, "verhalen/hemel_kapelletje");
        return stand("hemel", kleding(GuhClothes.HEMEL_AUREOOLTJE), stap, begonnen, String.valueOf(stap), nodig, bel);
    }

    static VerhaalStand guhwaii(ServerPlayer p) {
        int stap = Ohana.stap(p);
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (stap == Ohana.OPRUIMEN) {
            nodig.add(nodig("guhs:guhwaii_rommeltje", Ohana.rommel(p), Ohana.ROMMEL_NODIG));
        } else if (stap == Ohana.LIEF) {
            int gaven = Ohana.gaven(p);
            nodig.add(gave(p, "guhs:kokosnoot", gaven, Ohana.GAVE_KOKOS, 1));
            nodig.add(gave(p, "guhs:roze_hibiscus", gaven, Ohana.GAVE_BLOEM, 1));
            nodig.add(gave(p, "guhs:kaas_knabbels", gaven, Ohana.GAVE_KNABBELS, Ohana.KNABBELS));
        }
        boolean klaar = stap >= Ohana.KLAAR;
        String sleutel = klaar && VerhaalGuhs.magTemmen(p, VerhaalGuh.STITCH626) ? "klaar_tem" : String.valueOf(stap);
        List<VerhaalStand.Beloning> bel = List.of(
                beloning("guhs:vadsigheid_poster", klaar),
                // (the ukelele's own model draws nothing in a GUI slot: a coconut shows it)
                new VerhaalStand.Beloning("guhs:kokosnoot", Component.translatable(item("guhs:guhwaii_ukelele").getDescriptionId()), klaar),
                new VerhaalStand.Beloning(kleding(GuhClothes.GUHWAII_HULAROKJE), Component.translatable("gui.guhs.verhalen.beloning.kleertjes", "4"), klaar),
                getemd(p, VerhaalGuh.STITCH626));
        return stand("guhwaii", "guhs:kokosnoot", stap, stap > 0, sleutel, nodig, bel);
    }

    // =====================================================================================================================
    // the older adventures
    // =====================================================================================================================

    static VerhaalStand vadsig(ServerPlayer p) {
        GuhWorldData.PlayerData d = GuhWorldData.get(p.level().getServer()).player(p.getUUID());
        int stap = Math.min(4, d.maagQuest);
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (stap == 1) {
            nodig.add(new VerhaalStand.Nodig("guhs:mika_spoorkompas", Component.translatable("gui.guhs.verhalen.ding.rps"),
                    Math.min(GuhQuests.RPS_WINS_NEEDED, d.rpsStreak), GuhQuests.RPS_WINS_NEEDED));
        } else if (stap == 3) {
            nodig.add(nodig("guhs:verloren_guh_taart", GuhQuests.count(p, item("guhs:verloren_guh_taart")), 1));
            nodig.add(nodig("guhs:guh_ballon", GuhQuests.count(p, item("guhs:guh_ballon")), GuhQuests.BALLOONS_WANTED));
        }
        List<VerhaalStand.Beloning> bel = List.of(
                new VerhaalStand.Beloning("guhs:taartkruimels", Component.translatable("gui.guhs.verhalen.beloning.guhbert"), stap >= 2),
                beloning("guhs:guh_buikfluitje", stap >= 4),
                new VerhaalStand.Beloning("guhs:guh_buikfluitje", Component.translatable("gui.guhs.verhalen.beloning.maag"), stap >= 4));
        return stand("vadsig", "guhs:verloren_guh_taart", stap, stap > 0, String.valueOf(stap), nodig, bel);
    }

    static VerhaalStand slee(ServerPlayer p) {
        int stap = Math.min(2, GuhWorldData.get(p.level().getServer()).player(p.getUUID()).sledQuest);
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (stap == 1) {
            for (String id : List.of("guhs:sleeglijder", "guhs:guh_belletje", "guhs:roze_lint")) {
                nodig.add(nodig(id, GuhQuests.count(p, item(id)), 1));
            }
        }
        List<VerhaalStand.Beloning> bel = List.of(beloning("guhs:guh_slee", stap >= 2), beloning("guhs:sleebouwersboek", stap >= 2));
        return stand("slee", "guhs:guh_slee", stap, stap > 0, String.valueOf(stap), nodig, bel);
    }

    /** The item each Knusfeest task brings (its icon). */
    static String feestItem(Feesttaak t) {
        return switch (t) {
            case FEESTTAART -> "guhs:feesttaart";
            case THEESERVIES -> "guhs:feest_theeservies";
            case FEESTKAPSELS -> "guhs:feestkapselset";
            case FEESTSLINGERS -> "guhs:feestslingers";
            case FEESTBLOEMEN -> "guhs:feestboeket";
            case STERRENLANTAARNS -> "guhs:sterrenlantaarn";
        };
    }

    static VerhaalStand knusfeest(ServerPlayer p) {
        boolean klaar = Knusfeest.isKlaar(p);
        boolean groteRonde = !klaar && Knusfeest.ronde(p) == 0 && !Knusfeest.taken(p).isEmpty();
        int stap = klaar ? 3 : groteRonde ? (Knusfeest.alleGebracht(p) ? 2 : 1) : 0;
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        List<Object> args = new ArrayList<>();
        if (stap == 1) {
            for (Feesttaak t : Knusfeest.lijst(Knusfeest.taken(p))) {
                boolean gebracht = Knusfeest.stap(p, t) == Knusfeest.Stap.GEBRACHT;
                boolean bij = GuhQuests.count(p, item(feestItem(t))) > 0;
                nodig.add(new VerhaalStand.Nodig(feestItem(t), t.naam(), gebracht || bij ? 1 : 0, 1));
                if (args.isEmpty() && !gebracht && !bij) {
                    args.add(Component.translatable("gui.guhs.knusfeest.waar." + t.id()));
                }
            }
            if (args.isEmpty()) {
                args.add(Component.translatable("gui.guhs.verhalen.knusfeest.alles_bij_je"));
            }
        }
        List<VerhaalStand.Beloning> bel = List.of(
                beloning("guhs:knus_oorkonde", klaar),
                beloning(kleding(GuhClothes.BURGEMEESTERSSJERP), klaar),
                new VerhaalStand.Beloning("guhs:knus_oorkonde", Component.translatable("gui.guhs.verhalen.beloning.knuffelburgemeester"), klaar));
        boolean begonnen = stap > 0 || Knusfeest.ronde(p) >= 0;
        return stand("knusfeest", "guhs:knusfeestlijstje", stap, begonnen, String.valueOf(stap), nodig, bel, args.toArray());
    }

    static VerhaalStand guheinde(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        int koning = saved.getIntOr(GuheindeEvents.KONING, 0);
        int wins = saved.getIntOr(GuheindeGevecht.WINS, 0);
        boolean[] gedaan = {
                koning >= 1 || GidsFeature.heeft(p, "guheinde/guheinde_opdracht"),
                GidsFeature.heeft(p, "guheinde/root") || GuhQuests.count(p, item("guhs:mika_traan")) > 0,
                GidsFeature.heeft(p, "guheinde/guheinde_oog") || GuhQuests.count(p, item("guhs:oog_van_vadsig")) > 0,
                GidsFeature.heeft(p, "guheinde/find_knabbelkelder"),
                GidsFeature.heeft(p, "guheinde/guheinde_portaal"),
                GidsFeature.heeft(p, "guheinde/guheinde_binnen"),
                wins > 0,
                koning >= 2 || GidsFeature.heeft(p, "guheinde/guheinde_ridder")};
        int stap = 0;
        for (int i = gedaan.length - 1; i >= 0; i--) {
            if (gedaan[i]) {
                stap = i + 1;   // (a later step done: the ones before it count as done too)
                break;
            }
        }
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (stap == 1) {
            nodig.add(nodig("guhs:mika_traan", GuhQuests.count(p, item("guhs:mika_traan")), 1));
        } else if (stap == 2) {
            for (String id : List.of("guhs:guh_kristal", "guhs:kaas_knabbels", "guhs:mika_traan")) {
                nodig.add(nodig(id, GuhQuests.count(p, item(id)), 1));
            }
        } else if (stap == 4) {
            nodig.add(nodig("guhs:oog_van_vadsig", GuhQuests.count(p, item("guhs:oog_van_vadsig")), 12));
        }
        List<VerhaalStand.Beloning> bel = List.of(
                beloning("guhs:knabbelkroon", wins > 0),
                beloning("guhs:opper_mikatrofee", wins > 0),
                new VerhaalStand.Beloning("guhs:enderguh_ei", Component.translatable("gui.guhs.verhalen.beloning.enderguh"), wins > 0),
                new VerhaalStand.Beloning("guhs:knabbelkroon", Component.translatable("gui.guhs.verhalen.beloning.ridder"), koning >= 2));
        return stand("guheinde", "guhs:oog_van_vadsig", stap, stap > 0, String.valueOf(stap), nodig, bel);
    }

    static VerhaalStand grillguh(ServerPlayer p) {
        int stap = Math.min(Grillguh.DONE, Grillguh.step(p));
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (stap == Grillguh.FRAME) {
            nodig.add(nodig("guhs:aanmaakblokje", GuhQuests.count(p, item("guhs:aanmaakblokje")), 1));
        } else if (stap == Grillguh.BLOKJES) {
            nodig.add(nodig("guhs:aanmaakblokje", GuhQuests.count(p, item("guhs:aanmaakblokje")), 1));
        }
        List<VerhaalStand.Beloning> bel = List.of(
                beloning("guhs:grillguh_recept", stap >= Grillguh.DONE),
                new VerhaalStand.Beloning("guhs:grillkool", Component.translatable("gui.guhs.verhalen.beloning.grillwinkel"), stap >= Grillguh.DONE));
        return stand("grillguh", "guhs:grillguh_recept", stap, stap > 0, String.valueOf(stap), nodig, bel);
    }

    static VerhaalStand beroep(ServerPlayer p, BeroepenVoortgang.Beroep b) {
        boolean klaar = BeroepenVoortgang.klaar(p, b);
        int stap = klaar ? 3 : Math.min(2, BeroepenVoortgang.stap(p, b));
        List<VerhaalStand.Nodig> nodig = new ArrayList<>();
        if (!klaar && stap == 1 && b == BeroepenVoortgang.Beroep.APOTHEEK) {
            nodig.add(nodig("guhs:snotkruidje", GuhQuests.count(p, item("guhs:snotkruidje")), 3));
            nodig.add(nodig("guhs:kaasmelkdrankje", GuhQuests.count(p, item("guhs:kaasmelkdrankje")), 1));
        } else if (!klaar && stap == 1 && b == BeroepenVoortgang.Beroep.BOUW) {
            nodig.add(new VerhaalStand.Nodig("minecraft:oak_planks", Component.translatable("gui.guhs.verhalen.ding.planken"), tel(p, ItemTags.PLANKS), 16));
            nodig.add(nodig("guhs:kaas_knabbels", GuhQuests.count(p, item("guhs:kaas_knabbels")), 8));
        }
        String id = "beroep_" + b.id();
        List<VerhaalStand.Beloning> bel = List.of(beloning(kleding(b.kleding().get(0)), klaar), beloning(kleding(b.kleding().get(1)), klaar));
        return stand(id, kleding(b.kleding().get(0)), stap, stap > 0, klaar ? "klaar" : String.valueOf(stap), nodig, bel);
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** A questline's state from its parts (bbq2: public, Verhaallijn.stand uses it). */
    public static VerhaalStand stand(String id, String icoon, int stap, boolean begonnen, String sleutel, List<VerhaalStand.Nodig> nodig,
                                     List<VerhaalStand.Beloning> bel, Object... args) {
        int stappen = STAPPEN.get(id);
        stap = Math.max(0, Math.min(stap, stappen));
        VerhaalStand.Status status = stap >= stappen ? VerhaalStand.Status.KLAAR : stap == 0 && !begonnen ? VerhaalStand.Status.NIET_BEGONNEN
                : VerhaalStand.Status.BEZIG;
        if (status == VerhaalStand.Status.KLAAR && !sleutel.startsWith("klaar")) {
            sleutel = "klaar";
        }
        String base = "gui.guhs.verhalen." + id;
        return new VerhaalStand(id, icoon, status, stap, stappen, Component.translatable(base + ".nu." + sleutel, args),
                Component.translatable(base + ".waar." + sleutel), nodig, bel);
    }

    static Item item(String id) {
        return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
    }

    static String itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    static String kleding(GuhClothes c) {
        return itemId(ModItems.clothingItem(c));
    }

    private static VerhaalStand.Nodig nodig(String id, int heb, int n) {
        return new VerhaalStand.Nodig(id, Component.translatable(item(id).getDescriptionId()), Math.min(heb, n), n);
    }

    private static VerhaalStand.Nodig gave(ServerPlayer p, String id, int gaven, int bit, int n) {
        int heb = (gaven & bit) != 0 ? n : GuhQuests.count(p, item(id));
        return nodig(id, heb, n);
    }

    private static VerhaalStand.Beloning beloning(String id, boolean binnen) {
        return new VerhaalStand.Beloning(id, Component.translatable(item(id).getDescriptionId()), binnen);
    }

    private static VerhaalStand.Beloning getemd(ServerPlayer p, VerhaalGuh g) {
        return new VerhaalStand.Beloning("guhs:kaas_knabbels", Component.translatable("gui.guhs.verhalen.beloning.getemd",
                Component.translatable("entity.guhs.guh." + g.variant().id())), VerhaalGuhs.heeftGetemd(p, g));
    }

    /** How many items of a tag the player carries (main inventory). */
    static int tel(ServerPlayer p, TagKey<Item> tag) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(tag)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static Item eerste(TagKey<Item> tag, Item anders) {
        return BuiltInRegistries.ITEM.get(tag).flatMap(t -> t.stream().findFirst()).map(h -> h.value()).orElse(anders);
    }

    /** All ids, in display order. */
    public static List<String> ids() {
        List<String> out = new ArrayList<>();
        for (nl.juiced.guhs.feature.verhaal.Verhaallijn l : nl.juiced.guhs.feature.verhaal.Verhaallijnen.alle()) {
            out.add(l.id());
        }
        out.addAll(NIEUW);
        out.addAll(OUD);
        return out;
    }

    private VerhalenVoortgang() {
    }
}
