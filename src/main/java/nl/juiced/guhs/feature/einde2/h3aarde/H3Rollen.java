package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhNpcEntity.Kind;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.castschurk.E2CastSchurkFeature;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.kleding.KledingOntgrendel;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Who says what in Ba Sing Guh (CONTRACT_E2_B 4.4: every {@code ba_sing_guh/*} spot is this chapter's): the roles of the
 * residents on their markers and of the chapter's own actors, and which residents only exist for some steps
 * ({@link #bijMaken}: Miroh and Zuka leave the tea house, Long Feng-Mika the palace, the guards of the base step aside for
 * your own). Story talks are the talking screen ({@link Praat}); small talk is a line in the chat. Texts:
 * quest.guhs.einde2_h3_aarde.*.
 */
public final class H3Rollen {
    static final String LOKET = "einde2_h3_aarde_loket", TOPH = "einde2_h3_aarde_toph", PAI_SHO = "einde2_h3_aarde_pai_sho",
            PAI_SHO_KLAAR = "einde2_h3_aarde_pai_sho_klaar", JOO_DEE = "einde2_h3_aarde_joo_dee", KONING = "einde2_h3_aarde_koning",
            TOPH_MEE = "einde2_h3_aarde_toph_mee", KOOL = "einde2_h3_aarde_kool";
    /** How many answers each question of the passport desk has. */
    static final int LOKET_VRAGEN = 3, LOKET_OPTIES = 3;
    /** A step no player ever has: a resident with this range is never seen. */
    static final int NOOIT = 900;

    private H3Rollen() {
    }

    private static String plek(String marker) {
        return E2H3Aarde.STRUCTUUR + "/" + marker;
    }

    private static void rol(Kind kind, String marker, NpcRole rol) {
        NpcRollen.zet(kind, plek(marker), rol);
    }

    static void registreer() {
        rol(Kind.LOKETGUH, "loketguh", H3Rollen::loketguh);
        rol(Kind.TOPH_GUH, "toph", H3Rollen::toph);
        rol(Kind.TOPH_GUH, "team_toph", H3Rollen::tophKamp);
        rol(Kind.KEI_GUH, "kei", (npc, p) -> klets(npc, p, "kei", 3, "brul"));
        rol(Kind.AARDROMMEL_OMROEPER, "omroeper", (npc, p) -> klets(npc, p, "omroeper", 3, "roep"));
        rol(Kind.OOM_MIROH, "miroh", H3Rollen::miroh);
        rol(Kind.OOM_MIROH, "lu_ten_miroh", (npc, p) -> zeg(npc, p, "miroh.heuvel", "idle"));
        rol(Kind.OOM_MIROH, "miroh_thee", H3Rollen::mirohThee);
        rol(Kind.ZUKA, "zuka_ober", H3Rollen::zuka);
        rol(Kind.KOOLMAN, "koolman", H3Rollen::koolman);
        rol(Kind.JOO_DEE_GUH, "joo_dee", H3Rollen::jooDee);
        rol(Kind.AARDEKONING_GUH, "aardekoning", H3Rollen::koning);
        rol(Kind.LONG_FENG_MIKA, "long_feng", (npc, p) -> klets(npc, p, "long_feng", 3, "fluister"));
        rol(Kind.AZULA_MIKA, "azula", (npc, p) -> zeg(npc, p, "azula", "grijns"));
        for (int i = 1; i <= 6; i++) {
            rol(Kind.DAI_LI_MIKA, "dai_li_" + i, (npc, p) -> klets(npc, p, "dai_li", 4, "praat"));
        }
        rol(Kind.DAI_LI_MIKA, "volger", (npc, p) -> klets(npc, p, "volger", 3, "praat"));
        rol(Kind.DAI_LI_MIKA, "wacht", (npc, p) -> klets(npc, p, "dai_li", 4, "praat"));
        for (int i = 1; i <= 3; i++) {
            rol(Kind.BA_SING_GUH_BURGER, "klant_" + i, (npc, p) -> klets(npc, p, "klant", 4, "praat"));
        }
        for (Kind k : List.of(Kind.AANG_GUH, Kind.KATARA_GUH, Kind.SOKKA_GUH, Kind.TOPH_GUH, Kind.ZUKA_TEAM, Kind.ZUKA_VUURHEER)) {
            NpcRollen.zet(k, H3Theebezoek.PLEK, (NpcRole) (npc, p) -> zeg(npc, p, "theebezoek." + k.id(), "praat"));
        }
        Praat.luister(LOKET, H3Rollen::loketAntwoord);
        Praat.luister(TOPH, (p, spreker, optie) -> {
            if (optie < 0 && E2H3Aarde.stap(p) == E2H3Aarde.BLINDDOEK && !E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_BAAN)) {
                E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_BAAN, true);
                nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
                GuhQuests.hint(p, "gui.guhs.verhalen.guhvatar_aarde.kort.3_baan");
                H3Hints.baan(p);
            }
        });
        Praat.luister(PAI_SHO, (p, spreker, optie) -> {
            if (optie == 0 && E2H3Aarde.stap(p) == E2H3Aarde.PAI_SHO) {
                PaiShoSpel.metMiroh(p);
            }
        });
        Praat.luister(PAI_SHO_KLAAR, (p, spreker, optie) -> {
            if (optie < 0 && E2H3Aarde.stap(p) == E2H3Aarde.PAI_SHO) {
                PaiShoSpel.prijs(p);
                E2H3Aarde.LIJN.verder(p, E2H3Aarde.PAI_SHO);
            }
        });
        Praat.luister(JOO_DEE, H3Rollen::jooDeeAntwoord);
        Praat.luister(KONING, (p, spreker, optie) -> {
            if (optie < 0 && E2H3Aarde.stap(p) == E2H3Aarde.KONING) {
                E2H3Aarde.LIJN.verder(p, E2H3Aarde.KONING);
            }
        });
        Praat.luister(TOPH_MEE, (p, spreker, optie) -> {
            if (optie < 0 && E2H3Aarde.stap(p) == E2H3Aarde.TOPH) {
                klaar(p);
            }
        });
        Praat.luister(KOOL, (p, spreker, optie) -> {
            if (optie < 0 && H3Kool.kapot(p) >= 3 && !E2H3Aarde.LIJN.vlag(p, "koolkraam")) {
                E2H3Aarde.LIJN.vlag(p, "koolkraam", true);
                Minigames.give(p, new ItemStack(E2H3AardeFeature.KOOLKRAAM_ITEM.get()));
            }
        });
        Bewoners.BIJ_MAKEN.add(H3Rollen::bijMaken);
    }

    // =====================================================================================================================
    // who is there for whom
    // =====================================================================================================================

    /**
     * A resident of Ba Sing Guh was just made: some of them only exist for players at some steps of the chapter (Zicht:
     * the step range of {@code guhvatar_aarde}). The others stand there for everybody.
     */
    static void bijMaken(Entity e, String plek) {
        if (!plek.startsWith(E2H3Aarde.STRUCTUUR + "/")) {
            return;
        }
        String m = plek.substring(E2H3Aarde.STRUCTUUR.length() + 1);
        String lijn = E2H3Aarde.LIJN.id();
        switch (m) {
            case "miroh" -> Zicht.alleenBij(e, lijn, 0, E2H3Aarde.LU_TEN - 1);       // (under the tree, then caught in the fall; after the finale: his own reopened tea house)
            case "zuka_ober" -> Zicht.alleenBij(e, lijn, 0, E2H3Aarde.LU_TEN);       // (fix3-h3: at VAL the Dai Li hold him, with Katara)
            case "lu_ten_miroh" -> Zicht.alleenBij(e, lijn, NOOIT, NOOIT);          // (fix3-h3, playtest 2 item 7: H3Personen's own Miroh is the one under the tree, only at LU_TEN)
            case "long_feng" -> Zicht.alleenBij(e, lijn, 0, E2H3Aarde.KONING);       // (unmasked in the base)
            case "azula" -> Zicht.alleenBij(e, lijn, E2H3Aarde.VAL, E2H3Aarde.VAL);  // (only when she waits in the crystal chamber)
            case "toph" -> Zicht.alleenBij(e, lijn, 0, E2H3Aarde.VAL);               // (then at the camp, then in your team)
            default -> {
                if (m.startsWith("sluip_wacht_")) {
                    Zicht.alleenBij(e, lijn, 0, E2H3Aarde.KONING);                   // (your own guards during the sneak; after it the base is empty)
                }
            }
        }
    }

    /**
     * fix3-h3: a resident of Ba Sing Guh that was made before (saved with the chunk) gets the rules of now when it comes back
     * into the world (playtest 2 item 7: the old Miroh under the tree was saved for everybody).
     */
    static void opBinnenkomst(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof GuhNpcEntity npc && npc.getPersistentData().contains(Bewoners.TAG)) {
            String plek = npc.roleData.getStringOr(NpcRollen.PLEK, "");
            if (plek.startsWith(E2H3Aarde.STRUCTUUR + "/")) {
                bijMaken(npc, plek);
            }
        }
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    private static void zeg(GuhNpcEntity npc, ServerPlayer p, String key, String animatie) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1f);
        npc.speel(animatie);
        E2H3Aarde.zeg(p, npc, key);
    }

    /** Small talk: one of n lines, in turn (the counter of the line per character). */
    private static void klets(GuhNpcEntity npc, ServerPlayer p, String wie, int n, String animatie) {
        int i = E2H3Aarde.LIJN.teller(p, "klets_" + wie);
        E2H3Aarde.LIJN.teller(p, "klets_" + wie, i + 1);
        zeg(npc, p, wie + "." + (i % n), animatie);
    }

    private static Praat.Regel regel(@Nullable Entity spreker, String key) {
        return new Praat.Regel(spreker, "", "quest.guhs.einde2_h3_aarde." + key);
    }

    /** A page said by someone who is not there as an entity (the narrator, a team member): their name above it. */
    private static Praat.Regel van(String naamKey, String key) {
        return new Praat.Regel(null, naamKey, "quest.guhs.einde2_h3_aarde." + key);
    }

    private static final String VERTELLER = "gui.guhs.einde2_h3_aarde.verteller";

    @Nullable
    private static Entity bij(ServerPlayer p, Class<? extends Entity> soort, java.util.function.Predicate<Entity> wie, double r) {
        for (Entity e : p.level().getEntitiesOfClass(soort, new AABB(p.blockPosition()).inflate(r), wie::test)) {
            return e;
        }
        return null;
    }

    // =====================================================================================================================
    // the passport desk
    // =====================================================================================================================

    private static void loketguh(GuhNpcEntity npc, ServerPlayer p) {
        int stap = E2H3Aarde.stap(p);
        if (stap == E2H3Aarde.AANKOMST) {
            zeg(npc, p, "loket.wacht", "nee");
        } else if (stap == E2H3Aarde.LOKET) {
            E2H3Aarde.LIJN.teller(p, "loket", 0);
            vraag(p, npc, 0);
        } else {
            klets(npc, p, "loket.na", 3, "stempel");
        }
    }

    private static void vraag(ServerPlayer p, Entity loketguh, int nr) {
        if (loketguh instanceof GuhNpcEntity npc) {
            npc.speel(nr == 0 ? "stempel" : "praat");
        }
        Praat.Optie[] opties = new Praat.Optie[LOKET_OPTIES];
        for (int i = 0; i < LOKET_OPTIES; i++) {
            opties[i] = new Praat.Optie(i, "quest.guhs.einde2_h3_aarde.loket.vraag." + nr + "." + i);
        }
        Praat.open(p, loketguh, LOKET, "quest.guhs.einde2_h3_aarde.loket.vraag." + nr, new Object[] {p.getName()}, opties);
    }

    private static void loketAntwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (E2H3Aarde.stap(p) != E2H3Aarde.LOKET || optie < 0) {
            return;
        }
        int nr = E2H3Aarde.LIJN.teller(p, "loket");
        if (spreker != null) {
            E2H3Aarde.zeg(p, spreker, "loket.antwoord." + nr + "." + optie);
        }
        if (nr + 1 < LOKET_VRAGEN && spreker != null) {
            E2H3Aarde.LIJN.teller(p, "loket", nr + 1);
            vraag(p, spreker, nr + 1);
            return;
        }
        Praat.sluit(p);
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        if (stad != null) {
            H3Verloop.scene(p, stad, H3Scenes.LOKET, E2H3Aarde.LOKET, () -> H3Schijn.ververs(p));
        }
    }

    // =====================================================================================================================
    // Toph-guh
    // =====================================================================================================================

    private static void toph(GuhNpcEntity npc, ServerPlayer p) {
        int stap = E2H3Aarde.stap(p);
        if (stap < E2H3Aarde.AARDROMMEL) {
            zeg(npc, p, "toph.vroeg", "idle");
        } else if (stap == E2H3Aarde.AARDROMMEL) {
            zeg(npc, p, "toph.ring", "stamp");
        } else if (stap == E2H3Aarde.BLINDDOEK && !E2H3Aarde.LIJN.vlag(p, E2H3Aarde.V_BAAN)) {
            npc.speel("praat");
            Praat.scene(p, TOPH, List.of(regel(npc, "toph.les.0"), regel(npc, "toph.les.1"), regel(npc, "toph.les.2"), regel(npc, "toph.les.3")));
        } else if (stap == E2H3Aarde.BLINDDOEK) {
            E2H3Aarde.zeg(p, npc, "toph.bezig", Integer.bitCount(H3Blinddoek.gevonden(p)), H3Blinddoek.DOELEN);
        } else {
            klets(npc, p, "toph.na", 4, "lach");
        }
    }

    /** All six stones found: Toph's word, earth bending learnt, the step moves on. */
    static void tophGeleerd(ServerPlayer p) {
        Entity toph = bij(p, GuhNpcEntity.class, e -> ((GuhNpcEntity) e).getKind() == Kind.TOPH_GUH, 48);
        if (toph != null) {
            E2H3Aarde.zeg(p, toph, "toph.geleerd");
        } else {
            E2H3Aarde.verteller(p, "toph.geleerd");
        }
        p.sendSystemMessage(Component.translatable("quest.guhs.einde2_h3_aarde.aarde_geleerd").withStyle(ChatFormatting.GOLD));
        E2H3Aarde.LIJN.verder(p, E2H3Aarde.BLINDDOEK);
    }

    /** At the camp after the fall: Toph joins the team, the chapter is done, Appa flies on. */
    private static void tophKamp(GuhNpcEntity npc, ServerPlayer p) {
        if (E2H3Aarde.stap(p) != E2H3Aarde.TOPH) {
            zeg(npc, p, "toph.na.0", "lach");
            return;
        }
        npc.speel("praat");
        Praat.scene(p, TOPH_MEE, List.of(regel(npc, "toph.mee.0"), van("entity.guhs.guh_npc.katara_guh", "toph.mee.1"), regel(npc, "toph.mee.2"),
                van("entity.guhs.guh_npc.sokka_guh", "toph.mee.3"), regel(npc, "toph.mee.4"), van("entity.guhs.guh_npc.aang_guh", "toph.mee.5")));
    }

    /** The end of the chapter: the clothes, the step, the flight to the Fire Nation. */
    static void klaar(ServerPlayer p) {
        for (GuhClothes c : E2H3Aarde.KLEDING) {
            KledingOntgrendel.ontgrendel(p, c);
        }
        if (!E2H3Aarde.LIJN.verder(p, E2H3Aarde.TOPH)) {
            return;
        }
        H3Acteurs.weg(p, "toph_kamp");
        p.sendSystemMessage(Component.translatable("quest.guhs.einde2_h3_aarde.klaar").withStyle(ChatFormatting.GOLD));
        E2Reizen.vlieg(p, Einde2Ids.VUURNATIE_HOOFDSTAD, E2Reizen.Vlucht.H3_H4, null);
    }

    // =====================================================================================================================
    // Oom Miroh, Zuka
    // =====================================================================================================================

    private static void miroh(GuhNpcEntity npc, ServerPlayer p) {
        int stap = E2H3Aarde.stap(p);
        if (stap < E2H3Aarde.THEEHUIS) {
            zeg(npc, p, "miroh.vroeg", "lach");
        } else if (stap == E2H3Aarde.THEEHUIS) {
            zeg(npc, p, "miroh.welkom", "lach");
        } else if (stap == E2H3Aarde.THEE) {
            npc.speel("thee");
            Praat.open(p, npc, null, "quest.guhs.einde2_h3_aarde.miroh.thee.uitleg", new Object[0]);
        } else if (stap == E2H3Aarde.PAI_SHO) {
            npc.speel("pai_sho");
            Praat.open(p, npc, PAI_SHO, "quest.guhs.einde2_h3_aarde.miroh.pai_sho.vraag", new Object[0],
                    new Praat.Optie(0, "quest.guhs.einde2_h3_aarde.miroh.pai_sho.ja"), new Praat.Optie(1, "quest.guhs.einde2_h3_aarde.miroh.pai_sho.straks"));
        } else {
            klets(npc, p, "miroh.wijs", 6, "wijsheid");
        }
    }

    /** The game with Miroh is over (wie = the winner, 'a' = you): his word about hope, his table goes with you. */
    static void paiShoKlaar(ServerPlayer p, char wie) {
        Entity miroh = PaiShoSpel.miroh(p);
        String uitslag = wie == PaiSho.A ? "gewonnen" : wie == PaiSho.B ? "verloren" : "gelijk";
        Praat.scene(p, PAI_SHO_KLAAR, List.of(regel(miroh, "miroh.pai_sho." + uitslag), regel(miroh, "miroh.pai_sho.hoop"),
                regel(miroh, "miroh.pai_sho.tafel")));
    }

    /** After the whole story: Miroh in his reopened tea house; a cup of tea any time. */
    private static void mirohThee(GuhNpcEntity npc, ServerPlayer p) {
        npc.speel("thee");
        if (E2H3Aarde.LIJN.eenmalig(p, "miroh_heropend")) {
            Praat.open(p, npc, null, "quest.guhs.einde2_h3_aarde.miroh.heropend", new Object[0]);
        } else {
            E2H3Aarde.zeg(p, npc, "miroh.na." + p.getRandom().nextInt(4));
        }
        if (npc.level() instanceof ServerLevel level) {
            H3Blokken.Theeset.drink(p, level, npc.blockPosition());
        }
    }

    private static void zuka(GuhNpcEntity npc, ServerPlayer p) {
        int stap = E2H3Aarde.stap(p);
        if (stap < E2H3Aarde.THEEHUIS) {
            zeg(npc, p, "zuka.vroeg", "zucht");
        } else if (stap == E2H3Aarde.LU_TEN) {
            npc.speel("zucht");
            Praat.open(p, npc, null, "quest.guhs.einde2_h3_aarde.zuka.lu_ten", new Object[0]);
        } else if (stap == E2H3Aarde.VAL) {
            npc.speel("praat");
            Praat.open(p, npc, null, "quest.guhs.einde2_h3_aarde.zuka.val", new Object[0]);
        } else {
            klets(npc, p, "zuka.ober", 5, "zucht");
        }
    }

    // =====================================================================================================================
    // the koolman
    // =====================================================================================================================

    private static void koolman(GuhNpcEntity npc, ServerPlayer p) {
        int kapot = H3Kool.kapot(p);
        if (kapot >= 3 && !E2H3Aarde.LIJN.vlag(p, "koolkraam") && !npc.entityTags().contains(H3Acteurs.TAG)) {
            npc.speel("poets_kool");
            Praat.scene(p, KOOL, List.of(regel(npc, "kool.geef.0"), regel(npc, "kool.geef.1")));
        } else if (kapot == 0) {
            klets(npc, p, "kool.hallo", 3, "poets_kool");
        } else if (kapot < 3) {
            zeg(npc, p, "kool.boos." + kapot, "mijn_kolen");
        } else {
            klets(npc, p, "kool.na", 3, "poets_kool");
        }
    }

    // =====================================================================================================================
    // Joo Dee-guh, the Aardekoning-guh
    // =====================================================================================================================

    private static void jooDee(GuhNpcEntity npc, ServerPlayer p) {
        int stap = E2H3Aarde.stap(p);
        if (stap < E2H3Aarde.JOO_DEE) {
            zeg(npc, p, "joo_dee.welkom", "glimlach");
        } else if (stap == E2H3Aarde.JOO_DEE) {
            npc.speel("glimlach");
            Praat.open(p, npc, JOO_DEE, "quest.guhs.einde2_h3_aarde.joo_dee.vraag", new Object[0],
                    new Praat.Optie(0, "quest.guhs.einde2_h3_aarde.joo_dee.vraag.0"), new Praat.Optie(1, "quest.guhs.einde2_h3_aarde.joo_dee.vraag.1"),
                    new Praat.Optie(2, "quest.guhs.einde2_h3_aarde.joo_dee.vraag.2"));
        } else {
            klets(npc, p, "joo_dee.na", 3, "glimlach");
        }
    }

    private static void jooDeeAntwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (optie < 0 || E2H3Aarde.stap(p) != E2H3Aarde.JOO_DEE) {
            return;
        }
        Praat.sluit(p);
        if (spreker instanceof GuhNpcEntity npc) {
            npc.speel("herstart");
            E2H3Aarde.zeg(p, npc, "joo_dee.antwoord." + optie);
            E2H3Aarde.zeg(p, npc, "joo_dee.knabbeltekort");
        }
        E2H3Aarde.verteller(p, "joo_dee.volgers");
        E2H3Aarde.LIJN.verder(p, E2H3Aarde.JOO_DEE);
    }

    private static void koning(GuhNpcEntity npc, ServerPlayer p) {
        int stap = E2H3Aarde.stap(p);
        if (stap == E2H3Aarde.KONING) {
            npc.speel("klap");
            Entity longFeng = bij(p, GuhNpcEntity.class, e -> ((GuhNpcEntity) e).getKind() == Kind.LONG_FENG_MIKA, 16);
            Entity bosco = bij(p, Entity.class, e -> e.getType() == E2CastSchurkFeature.BOSCO.get(), 24);
            Praat.scene(p, KONING, List.of(regel(npc, "koning.0"), van(VERTELLER, "koning.1"), regel(longFeng, "koning.2"), van(VERTELLER, "koning.3"),
                    regel(npc, "koning.4"), regel(bosco != null ? bosco : npc, "koning.5"), van(VERTELLER, "koning.6")));
        } else if (stap > E2H3Aarde.LAOGAI && E2H3Aarde.LIJN.eenmalig(p, "koning_waarheid")) {
            npc.speel("bril");
            Praat.open(p, npc, null, "quest.guhs.einde2_h3_aarde.koning.waarheid", new Object[0]);
        } else {
            klets(npc, p, stap < E2H3Aarde.KONING ? "koning.vroeg" : "koning.na", 3, "praat");
        }
    }

    /** (dev, tests) every name a role can say (for the lang check of the slice). */
    static List<String> sleutels() {
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < LOKET_VRAGEN; i++) {
            uit.add("loket.vraag." + i);
            for (int j = 0; j < LOKET_OPTIES; j++) {
                uit.add("loket.vraag." + i + "." + j);
                uit.add("loket.antwoord." + i + "." + j);
            }
        }
        return uit;
    }

    /** A spot of the copy of this player's city, in the world (null: none). */
    @Nullable
    static BlockPos waar(ServerPlayer p, String marker) {
        return H3Verloop.pos(p, marker);
    }

    /** Is the player's element of this chapter learnt (for zij's side gates, through E2Verhaal)? */
    static boolean geleerd(ServerPlayer p) {
        return E2Verhaal.elementGeleerd(p, Einde2Ids.Element.AARDE);
    }
}
