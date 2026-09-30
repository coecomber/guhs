package nl.juiced.guhs.feature.baltoslee;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.balto.Nomguh;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.quest.Highscores;
import nl.juiced.guhs.quest.Scorebord;

/**
 * Steele-Mika at the start line of Nomguh (NPC kind STEELE_MIKA with plek "sledesprint", placed by balto): the sledesprint.
 * Once your Nomguh story is done ({@link Nomguh#verhaalKlaar}) he dares you to race him - makkelijk, medium or lastig - there,
 * around the berghut and back. He also has a little shop: winter deco for sledebelletjes (no clothes). Next to him floats the
 * board with the fastest sledgers of each level.
 */
public final class SteeleSprint implements NpcRole {
    public static final String PLEK = "sledesprint";
    /** For a test / dev Steele without a Nomguh around him: the route's anchor in his roleData. */
    public static final String ANKER = "baltoslee_anker";
    public static final int OPT_MAKKELIJK = 0, OPT_MEDIUM = 1, OPT_LASTIG = 2, OPT_WINKEL = 3, OPT_UITLEG = 4;

    /** The shop: deco for sledebelletjes (item, price). */
    public record Aanbod(java.util.function.Supplier<? extends Item> item, int prijs) {
    }

    public static List<Aanbod> aanbod() {
        return List.of(new Aanbod(BaltoSleeFeature.SLEDEBELLEN_ITEM, 4), new Aanbod(BaltoSleeFeature.LANTAARNPAAL_ITEM, 5),
                new Aanbod(BaltoSleeFeature.SNEEUWGUH_ITEM, 6), new Aanbod(BaltoSleeFeature.HONDENMAND_ITEM, 8),
                new Aanbod(BaltoSleeFeature.MINISLEE_ITEM, 10), new Aanbod(BaltoSleeFeature.BEKER_ITEM, 16));
    }

    /** May this player race (the story is done, or an op let them)? */
    public static boolean magRacen(ServerPlayer p) {
        return Nomguh.verhaalKlaar(p) || SleeRit.data(p).getBoolean("Vrij");
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, BaltoSleeFeature.BELLEN.get(), SoundSource.NEUTRAL, 0.6f, 1.3f);
        if (SleeRit.rijdt(player)) {
            GuhQuests.say(player, npc, "gui.guhs.baltoslee.steele.al_onderweg");
            return;
        }
        if (!magRacen(player)) {
            Praat.open(player, npc, null, "gui.guhs.baltoslee.steele.nog_niet", new Object[0], new Praat.Optie(OPT_WINKEL, "gui.guhs.baltoslee.optie.winkel"));
            return;
        }
        Praat.open(player, npc, null, "gui.guhs.baltoslee.steele.welkom", new Object[]{tijd(player, Niveau.MAKKELIJK), tijd(player, Niveau.MEDIUM),
                tijd(player, Niveau.LASTIG)},
                new Praat.Optie(OPT_MAKKELIJK, "gui.guhs.baltoslee.optie.makkelijk"), new Praat.Optie(OPT_MEDIUM, "gui.guhs.baltoslee.optie.medium"),
                new Praat.Optie(OPT_LASTIG, "gui.guhs.baltoslee.optie.lastig"), new Praat.Optie(OPT_WINKEL, "gui.guhs.baltoslee.optie.winkel"),
                new Praat.Optie(OPT_UITLEG, "gui.guhs.baltoslee.optie.uitleg"));
    }

    private static String tijd(ServerPlayer p, Niveau n) {
        int best = SleeRit.best(p, n);
        return best > 0 ? Highscores.tijd(best) : "-";
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
        switch (optie) {
            case OPT_MAKKELIJK, OPT_MEDIUM, OPT_LASTIG -> {
                Praat.sluit(player);
                if (magRacen(player)) {
                    start(npc, player, Niveau.of(optie));
                }
            }
            case OPT_WINKEL -> {
                Praat.sluit(player);
                npc.openShop(player);
            }
            case OPT_UITLEG -> Praat.open(player, npc, null, "gui.guhs.baltoslee.steele.uitleg", new Object[0],
                    new Praat.Optie(OPT_MAKKELIJK, "gui.guhs.baltoslee.optie.makkelijk"), new Praat.Optie(OPT_MEDIUM, "gui.guhs.baltoslee.optie.medium"),
                    new Praat.Optie(OPT_LASTIG, "gui.guhs.baltoslee.optie.lastig"));
            default -> {
            }
        }
    }

    /** Starts the race from this Steele-Mika (his Nomguh's route). */
    @Nullable
    public static SleeRit start(GuhNpcEntity npc, ServerPlayer player, Niveau niveau) {
        if (Minigames.refuse(player, npc, SleeRit.GAME_SPRINT)) {
            return null;
        }
        BlockPos anker = anker(npc);
        if (anker == null) {
            GuhQuests.say(player, npc, "gui.guhs.baltoslee.steele.geen_route");
            return null;
        }
        SleeRit rit = SleeRit.start(player, NomguhRoute.laad().in(anker), SleeRit.Modus.SPRINT, niveau, null, npc);
        if (rit == null) {
            GuhQuests.say(player, npc, "gui.guhs.baltoslee.steele.al_onderweg");
        }
        return rit;
    }

    @Nullable
    static BlockPos anker(GuhNpcEntity npc) {
        BlockPos a = npc.level() instanceof ServerLevel sl ? Nomguh.anker(sl, npc.blockPosition()) : null;
        if (a == null && npc.roleData.contains(ANKER)) {
            a = BlockPos.of(npc.roleData.getLong(ANKER));
        }
        return a;
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        for (Aanbod a : aanbod()) {
            offers.add(new MerchantOffer(new ItemCost(BaltoSleeFeature.SLEDEBELLETJE.get(), a.prijs()), Optional.empty(), new ItemStack(a.item().get()),
                    Integer.MAX_VALUE, 2, 0.0f));
        }
        return offers;
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        if (!(npc.level() instanceof ServerLevel level) || (npc.tickCount + npc.getId()) % 100 != 0) {
            return;
        }
        List<String> boards = new ArrayList<>();
        List<Component> heads = new ArrayList<>();
        for (Niveau n : Niveau.values()) {
            boards.add(SleeRit.bord(n));
            heads.add(n.naam());
        }
        Component text = Scorebord.text(level.getServer(), Component.translatable("gui.guhs.baltoslee.bord"), boards, heads, Highscores::tijd);
        Vec3 kijk = Vec3.directionFromRotation(0, npc.getYRot());
        Vec3 side = new Vec3(-kijk.z, 0, kijk.x);
        Scorebord.show(level, npc.position().add(side.scale(2.2)).add(0, 2.4, 0), "sledesprint", text);
    }

    static void bericht(ServerPlayer p, String key) {
        p.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }
}
