package nl.juiced.guhs.feature.guhpixel.grap2;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.guhpixel.Aandenken;
import nl.juiced.guhs.feature.guhpixel.ArenaSoort;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.network.ModNetworking;

/**
 * Boer zoekt Guh (a TV parody, nothing more: no friendship system): Presentatrice Guhvon in the lobby, a TV studio with a
 * farm set next to it. The show: the intro, three letters for Boer Guhrrit from the mailbox (every candidate mostly loves
 * sleeping), the logeerweek (everybody sleeps, you tuck them in), the choice (he cannot choose: they all become friends and
 * nap together), the credits. Keepsakes: the outfit Strohoed met overall and the decoration Ingelijste brief.
 * The numbers of the studio are checked by guhpixel_grap2_bouw.py.
 */
public final class Bzg {
    public static final String ID = "bzg";
    /** The studio: template size and the fixed spots (template coordinates; a copy of STUDIO in tools/features/guhpixel_grap2_bouw.py). */
    public static final Vec3i MAAT = new Vec3i(41, 12, 27);
    public static final Vec3 START = new Vec3(10.5, 1, 22.5), GUHVON = new Vec3(10.5, 1, 8.5), BOER = new Vec3(30.5, 1, 8.5);
    public static final BlockPos BRIEVENBUS = new BlockPos(5, 1, 14);
    /** The three candidates: on the studio sofa, in the hay of the logeerweek, and around the farmer at the end. */
    public static final Vec3[] BANK = {new Vec3(13.5, 1, 5.5), new Vec3(15.5, 1, 5.5), new Vec3(17.5, 1, 5.5)};
    public static final Vec3[] HOOI = {new Vec3(26.5, 1, 13.5), new Vec3(30.5, 1, 15.5), new Vec3(34.5, 1, 13.5)};
    public static final Vec3[] SAMEN = {new Vec3(29, 1, 9.8), new Vec3(30.5, 1, 10.3), new Vec3(32, 1, 9.8)};
    public static final int KANDIDATEN = 3, BRIEVEN = 3, AFTITELING_REGELS = 22;
    /** The candidates' looks (variant ids) in letter order: Tukkie, Dommelien, Snurkbert. */
    static final String[] SOORTEN = {"snow", "choco", "golden"};

    public static final ArenaSoort ARENA = new ArenaSoort(ID, Guhs.id("guhpixel/bzg_studio"), MAAT, START, 180f);
    public static final SpelSoort SPEL = new SpelSoort(ID, ARENA, 1, 1, LobbyPlek.SPEL_BZG, BzgSessie::new);

    private static final String DATA = "grap2", INGESTOPT = "BzgIngestopt";
    private static final int MEEDOEN = 1, UITLEG = 2, KWIJT = 3;

    /** How many sleepers this player ever tucked in (a silly stat for the Guhdex). */
    public static int ingestopt(ServerPlayer p) {
        return PxData.deel(p, DATA).getIntOr(INGESTOPT, 0);
    }

    static void telIngestopt(ServerPlayer p) {
        CompoundTag d = PxData.deel(p, DATA);
        d.putInt(INGESTOPT, d.getIntOr(INGESTOPT, 0) + 1);
        PxData.vuil(p.level().getServer());
    }

    static Component kandidaat(int i) {
        return Component.translatable("gui.guhs.bzg.kandidaat." + (i + 1));
    }

    /** The keepsakes of the first time: the outfit (hat + overall) and the framed letter. */
    static void aandenken(ServerPlayer p) {
        Aandenken.kleding(p, GuhClothes.BZG_STROHOED, GuhClothes.BZG_OVERALL);
        Aandenken.item(p, new ItemStack(Grap2Slice.INGELIJSTE_BRIEF_ITEM.get()));
    }

    // --- Presentatrice Guhvon: in the lobby, and the same guh in the studio --------------------------------------------------

    public static final NpcRole GUHVON_ROL = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            if (Sessies.van(player) instanceof BzgSessie s) {
                s.praatGuhvon(player);
                return;
            }
            boolean klaar = Grappen.isKlaar(player, ID);
            Praat.Optie mee = new Praat.Optie(MEEDOEN, klaar ? "quest.guhs.bzg.optie.opnieuw" : "quest.guhs.bzg.optie.meedoen");
            Praat.Optie uitleg = new Praat.Optie(UITLEG, "quest.guhs.bzg.optie.uitleg");
            if (klaar) {
                Praat.open(player, npc, null, "quest.guhs.bzg.lobby.weer", new Object[0], mee, uitleg, new Praat.Optie(KWIJT, "quest.guhs.bzg.optie.kwijt"));
            } else {
                Praat.open(player, npc, null, "quest.guhs.bzg.lobby.hallo", new Object[0], mee, uitleg);
            }
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
            if (Sessies.van(player) != null) {
                return;
            }
            switch (optie) {
                case MEEDOEN -> {
                    Praat.sluit(player);
                    Sessies.start(SPEL, List.of(player), new CompoundTag());
                }
                case UITLEG -> Praat.open(player, npc, null, "quest.guhs.bzg.lobby.uitleg", new Object[0], new Praat.Optie(MEEDOEN, "quest.guhs.bzg.optie.meedoen"));
                case KWIJT -> {
                    Praat.sluit(player);
                    kwijt(player);
                }
                default -> {
                }
            }
        }
    };

    /** Boer Guhrrit only exists on the farm set of a running show. */
    public static final NpcRole BOER_ROL = (npc, player) -> {
        if (Sessies.van(player) instanceof BzgSessie s) {
            s.praatBoer(player);
        } else {
            nl.juiced.guhs.quest.GuhQuests.say(player, npc, "quest.guhs.bzg.boer.los");
        }
    };

    static void kwijt(ServerPlayer p) {
        if (!Grappen.isKlaar(p, ID)) {
            return;
        }
        boolean iets = Aandenken.opnieuw(p, Grap2Slice.INGELIJSTE_BRIEF_ITEM.get());
        if (!KledingUnlocks.heeft(p, GuhClothes.BZG_STROHOED) || !KledingUnlocks.heeft(p, GuhClothes.BZG_OVERALL)) {
            iets |= Aandenken.kleding(p, GuhClothes.BZG_STROHOED, GuhClothes.BZG_OVERALL);
        }
        p.sendSystemMessage(Component.translatable(iets ? "quest.guhs.bzg.kwijt.hier" : "quest.guhs.bzg.kwijt.niets").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // --- the mailbox and the framed letter ------------------------------------------------------------------------------------

    /** A click on a Brievenbus: in the show the three letters, anywhere else a little joke. */
    static void brievenbus(ServerPlayer p, BlockPos pos) {
        if (Sessies.van(p) instanceof BzgSessie s) {
            s.brievenbus(p, pos);
            return;
        }
        int n = 1 + Math.floorMod(p.getRandom().nextInt(), 4);
        p.sendOverlayMessage(Component.translatable("gui.guhs.bzg.brievenbus.leeg." + n).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** A click on an Ingelijste brief: the thank-you letter of Boer Guhrrit, to read. */
    static void leesIngelijst(ServerPlayer p) {
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "brief");
        ModNetworking.sendTo(p, new Grap2Payloads.BzgScherm(data));
    }

    /** (Grap2Payloads.BzgDoe) what the player did in the letter screen or the credits; the session checks everything. */
    static void doe(ServerPlayer p, int actie, int arg) {
        if (!(Sessies.van(p) instanceof BzgSessie s)) {
            return;
        }
        switch (actie) {
            case Grap2Payloads.GELEZEN -> s.gelezen(p, arg);
            case Grap2Payloads.BRIEVEN_DICHT -> s.brievenDicht(p);
            case Grap2Payloads.AFTITELING_KLAAR -> s.aftitelingKlaar(p);
            default -> {
            }
        }
    }

    private Bzg() {
    }
}
