package nl.juiced.guhs.feature.guhpixel.grap2;

import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
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
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * The Guhmon-gevecht (a Cobblemon parody): Gymleider Dutjes in the lobby, ONE gym, ONE battle. You pick one of your tamed
 * guhs (a copy appears in the gym; the real guh stays home) or the gym's leenguh, and the two guhs battle with SLEEP bars:
 * whoever falls asleep first wins ({@link GuhmonGevecht}). Losing costs nothing: just try again.
 * <p>
 * Keepsakes: the Trainerspet (an outfit unlock), the Badgedoosje (a decoration block) and three gymbadges that go into it:
 * the Dutjesbadge (win), the Njegbadge (win after your Njeg made a Dutje of the other guh fail) and the Knabbelbadge (fall
 * asleep with a full belly). Everything is per player; the numbers of the gym are checked by guhpixel_grap2_bouw.py.
 */
public final class Guhmon {
    public static final String ID = "guhmon";
    /** The gym: template size and the fixed spots (template coordinates; a copy of GYM in tools/features/guhpixel_grap2_bouw.py). */
    public static final Vec3i MAAT = new Vec3i(27, 12, 39);
    public static final Vec3 START = new Vec3(13.5, 1, 35.5), VAK = new Vec3(13.5, 1, 29.5), MIJN = new Vec3(13.5, 1, 25.5),
            TEGEN = new Vec3(13.5, 1, 13.5), LEIDER = new Vec3(13.5, 2, 7.5), TEGEN_WACHT = new Vec3(15.5, 2, 7.5);
    public static final Vec3[] PUBLIEK = {new Vec3(3.5, 2, 14.5), new Vec3(3.5, 2, 19.5), new Vec3(3.5, 2, 24.5),
            new Vec3(23.5, 2, 14.5), new Vec3(23.5, 2, 19.5), new Vec3(23.5, 2, 24.5)};

    public static final ArenaSoort ARENA = new ArenaSoort(ID, Guhs.id("guhpixel/guhmon_gym"), MAAT, START, 180f);
    public static final SpelSoort SPEL = new SpelSoort(ID, ARENA, 1, 1, LobbyPlek.SPEL_GUHMON, GuhmonSessie::new);

    /** The id the picker sends for the gym's own guh. */
    public static final String LEENGUH = "leenguh";
    public static final UUID LEENGUH_ID = UUID.nameUUIDFromBytes("guhs:guhmon_leenguh".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    /** The three gymbadges (bits of the saved mask; the block state of the Badgedoosje has one property per badge). */
    public enum Badge {
        DUTJES, NJEG, KNABBEL;

        public int bit() {
            return 1 << ordinal();
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public Item item() {
            return Grap2Slice.BADGES.get(ordinal()).get();
        }
    }

    private static final String DATA = "grap2", BADGES = "GuhmonBadges", GEWONNEN = "GuhmonGewonnen", VERLOREN = "GuhmonVerloren";
    private static final int UITDAGEN = 1, UITLEG = 2, KWIJT = 3;

    // --- saved progress ----------------------------------------------------------------------------------------------------

    public static int badges(ServerPlayer p) {
        return PxData.deel(p, DATA).getIntOr(BADGES, 0);
    }

    public static boolean heeft(ServerPlayer p, Badge b) {
        return (badges(p) & b.bit()) != 0;
    }

    public static int gewonnen(ServerPlayer p) {
        return PxData.deel(p, DATA).getIntOr(GEWONNEN, 0);
    }

    public static int verloren(ServerPlayer p) {
        return PxData.deel(p, DATA).getIntOr(VERLOREN, 0);
    }

    /** (Dev, tests) sets the badge mask without giving items. */
    public static void zetBadges(ServerPlayer p, int mask) {
        PxData.deel(p, DATA).putInt(BADGES, mask & 7);
        PxData.vuil(p.level().getServer());
    }

    static void tel(ServerPlayer p, boolean winst) {
        CompoundTag d = PxData.deel(p, DATA);
        String k = winst ? GEWONNEN : VERLOREN;
        d.putInt(k, d.getIntOr(k, 0) + 1);
        PxData.vuil(p.level().getServer());
    }

    /** The player earned this badge; true (and the badge item is given) the first time. */
    static boolean verdien(ServerPlayer p, Badge b) {
        CompoundTag d = PxData.deel(p, DATA);
        int mask = d.getIntOr(BADGES, 0);
        if ((mask & b.bit()) != 0) {
            return false;
        }
        d.putInt(BADGES, mask | b.bit());
        PxData.vuil(p.level().getServer());
        Aandenken.item(p, new ItemStack(b.item()));
        if ((mask | b.bit()) == 7) {
            GuhAdvancements.grant(p, "guhmon_badges");
        }
        return true;
    }

    /** The keepsake of the first win: the Badgedoosje and the Trainerspet (the Dutjesbadge comes from {@link #verdien}). */
    static void aandenken(ServerPlayer p) {
        Aandenken.item(p, new ItemStack(Grap2Slice.BADGEDOOS_ITEM.get()));
        Aandenken.kleding(p, GuhClothes.GUHMON_TRAINERSPET);
    }

    // --- the gym leader: in the lobby, and the same guh in the gym -----------------------------------------------------------

    public static final NpcRole ROL = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            if (Sessies.van(player) instanceof GuhmonSessie s) {
                s.daagUit(player);
                return;
            }
            boolean klaar = Grappen.isKlaar(player, ID);
            Praat.Optie uitdagen = new Praat.Optie(UITDAGEN, klaar ? "quest.guhs.guhmon.optie.opnieuw" : "quest.guhs.guhmon.optie.uitdagen");
            Praat.Optie uitleg = new Praat.Optie(UITLEG, "quest.guhs.guhmon.optie.uitleg");
            if (klaar) {
                Praat.open(player, npc, null, "quest.guhs.guhmon.lobby.weer", new Object[0], uitdagen, uitleg, new Praat.Optie(KWIJT, "quest.guhs.guhmon.optie.kwijt"));
            } else {
                Praat.open(player, npc, null, "quest.guhs.guhmon.lobby.hallo", new Object[0], uitdagen, uitleg);
            }
        }

        @Override
        public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
            if (Sessies.van(player) != null) {
                return;
            }
            switch (optie) {
                case UITDAGEN -> {
                    Praat.sluit(player);
                    Sessies.start(SPEL, List.of(player), new CompoundTag());
                }
                case UITLEG -> Praat.open(player, npc, null, "quest.guhs.guhmon.lobby.uitleg", new Object[0],
                        new Praat.Optie(UITDAGEN, "quest.guhs.guhmon.optie.uitdagen"));
                case KWIJT -> {
                    Praat.sluit(player);
                    kwijt(player);
                }
                default -> {
                }
            }
        }
    };

    /** "Kwijt, njeg": the Badgedoosje and every earned badge again, but only what the player does not carry. */
    static void kwijt(ServerPlayer p) {
        if (!Grappen.isKlaar(p, ID)) {
            return;
        }
        boolean iets = Aandenken.opnieuw(p, Grap2Slice.BADGEDOOS_ITEM.get());
        for (Badge b : Badge.values()) {
            if (heeft(p, b) && !p.getInventory().hasAnyMatching(s -> s.is(b.item()))) {
                nl.juiced.guhs.feature.Minigames.give(p, new ItemStack(b.item()));
                iets = true;
            }
        }
        if (!KledingUnlocks.heeft(p, GuhClothes.GUHMON_TRAINERSPET)) {
            iets |= Aandenken.kleding(p, GuhClothes.GUHMON_TRAINERSPET);
        }
        p.sendSystemMessage(Component.translatable(iets ? "quest.guhs.guhmon.kwijt.hier" : "quest.guhs.guhmon.kwijt.niets").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** (Grap2Payloads.GuhmonDoe) what the player did in the picker or the battle screen; everything is checked by the session. */
    static void doe(ServerPlayer p, int actie, String arg) {
        Sessie s = Sessies.van(p);
        if (!(s instanceof GuhmonSessie g)) {
            return;
        }
        switch (actie) {
            case Grap2Payloads.KIES -> g.kies(p, arg);
            case Grap2Payloads.ZET -> {
                try {
                    g.zet(p, Integer.parseInt(arg));
                } catch (NumberFormatException e) {
                    // (not a move: ignored)
                }
            }
            case Grap2Payloads.OPNIEUW -> g.opnieuw(p);
            case Grap2Payloads.VERDER -> g.verder(p);
            default -> {
            }
        }
    }

    private Guhmon() {
    }
}
