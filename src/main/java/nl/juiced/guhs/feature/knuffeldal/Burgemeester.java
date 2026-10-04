package nl.juiced.guhs.feature.knuffeldal;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.feature.knus.Seizoen;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Burgemeester Vadsema, on the town hall steps of the Knuffeldal plein: the Grote Knusfeest.
 * <ol>
 *   <li>First visit: the Knusfeest is coming, but nothing is ready, njeg! He asks all six feesttaakjes
 *   ({@link Knusfeest#nieuweRonde}, round 0) and gives you the knusfeestlijstje.</li>
 *   <li>Every visit: he takes the feest-items you carry ({@code #guhs:knus/<task>}) and ticks them off
 *   ({@code GEBRACHT}); an item a Kruimel-Mika stole long ago he has found back himself (so nobody gets stuck).</li>
 *   <li>All six there: the feast! ({@link KnusfeestEvenement}: the feestbuffet, all your tamed guhs come to eat; after it
 *   the knus_oorkonde, the title Knuffelburgemeester and the burgemeesterssjerp.)</li>
 *   <li>After that, once per season: the seasonal Knusfeest (2-3 random tasks again; a smaller feast).</li>
 * </ol>
 * The talking screen (guhs:knuffeldal_open) shows the list; its button "Geven" hands in what you carry.
 */
public final class Burgemeester implements NpcRole {
    /** The option "Geven" (hand in what you carry) and "Feest!" (start the feast when everything is there). */
    public static final int GEVEN = 1, FEEST = 2;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.8f);
        GuhAdvancements.grant(player, "knuffeldal_burgemeester");
        KnuffeldalAdvancements.toon(player, "burgemeester");
        KnuffeldalEvents.vriendje(player, "burgemeester");
        String begroeting = isKnuffelburgemeester(player) ? "quest.guhs.burgemeester.hoi_collega" : "quest.guhs.burgemeester.hoi";
        if (!Knusfeest.rondeBezig(player) && !Knusfeest.isKlaar(player)) {
            // the very first time: the Grote Knusfeest
            GuhQuests.say(player, npc, "quest.guhs.burgemeester.start");
            Knusfeest.nieuweRonde(player, 0, EnumSet.allOf(Feesttaak.class));
            KnusVoortgang.hoogste(player, KnuffeldalVoortgang.KNUSFEEST, 1);
            geefLijstje(player);
            scherm(npc, player, "quest.guhs.burgemeester.lijst");
            return;
        }
        if (!Knusfeest.rondeBezig(player)) {
            long nu = Seizoen.nummer(player.level()) + 1;
            if (Knusfeest.laatsteSeizoensfeest(player) < nu && Knusfeest.ronde(player) != nu) {
                // a new season: the seasonal Knusfeest
                Set<Feesttaak> taken = seizoensTaken(player, nu);
                Knusfeest.nieuweRonde(player, nu, taken);
                GuhQuests.say(player, npc, "quest.guhs.burgemeester.seizoen." + Seizoen.huidig(player.level()).id());
                geefLijstje(player);
                scherm(npc, player, "quest.guhs.burgemeester.lijst");
            } else {
                GuhQuests.say(player, npc, "quest.guhs.burgemeester.klaar." + npc.getRandom().nextInt(3));
            }
            return;
        }
        int gebracht = lever(player);
        if (Knusfeest.alleGebracht(player)) {
            GuhQuests.say(player, npc, Knusfeest.ronde(player) == 0 ? "quest.guhs.burgemeester.alles" : "quest.guhs.burgemeester.alles_seizoen");
            feest(npc, player);
            return;
        }
        GuhQuests.say(player, npc, gebracht > 0 ? "quest.guhs.burgemeester.dank" : begroeting);
        scherm(npc, player, gebracht > 0 ? "quest.guhs.burgemeester.dank" : "quest.guhs.burgemeester.lijst");
    }

    /** A button in the screen. */
    static void actie(GuhNpcEntity npc, ServerPlayer player, int actie) {
        if (!Knusfeest.rondeBezig(player)) {
            return;
        }
        if (actie == GEVEN) {
            int n = lever(player);
            GuhQuests.say(player, npc, n > 0 ? "quest.guhs.burgemeester.dank" : "quest.guhs.burgemeester.niets");
        }
        if (Knusfeest.alleGebracht(player)) {
            feest(npc, player);
            sluit(npc, player);
        } else {
            scherm(npc, player, "quest.guhs.burgemeester.lijst");
        }
    }

    /**
     * Hands in every feest-item the player carries for an open task; a stolen one that's been gone too long counts as
     * found by the Burgemeester himself. Returns how many were handed in.
     */
    public static int lever(ServerPlayer player) {
        int n = 0;
        for (Feesttaak taak : Feesttaak.values()) {
            Knusfeest.Stap stap = Knusfeest.stap(player, taak);
            if (stap == null || stap == Knusfeest.Stap.GEBRACHT) {
                continue;
            }
            if (stap == Knusfeest.Stap.GESTOLEN) {
                if (player.level().getGameTime() - Knusfeest.gestolenOp(player, taak) > KruimelMikaEntity.TERUG_NA && KruimelMikaEntity.heeftGeen(player)) {
                    player.sendSystemMessage(Component.translatable("quest.guhs.burgemeester.teruggevonden", taak.naam()).withStyle(ChatFormatting.GREEN));
                    gebracht(player, taak);
                    n++;
                }
                continue;
            }
            ItemStack item = KruimelMikaEntity.neem(player, taak);
            if (!item.isEmpty()) {
                gebracht(player, taak);
                n++;
            } else if (stap == Knusfeest.Stap.GEMAAKT || stap == Knusfeest.Stap.TERUGGEVONDEN) {
                // made, but not in the pockets (lost? left at home?): asked again, so it can always be made anew (1.2.7)
                Knusfeest.zet(player, taak, Knusfeest.Stap.GEVRAAGD);
                player.sendSystemMessage(Component.translatable("quest.guhs.burgemeester.kwijt", taak.naam()).withStyle(ChatFormatting.GOLD));
            }
        }
        if (n > 0) {
            player.level().playSound(null, player.blockPosition(), KnuffeldalFeature.FEESTBEL.get(), SoundSource.NEUTRAL, 1f, 1.2f);
        }
        return n;
    }

    /** A task delivered (the item is already taken): it counts. */
    public static void gebracht(ServerPlayer player, Feesttaak taak) {
        Knusfeest.zet(player, taak, Knusfeest.Stap.GEBRACHT);
        KnusVoortgang.tel(player, KnuffeldalVoortgang.TAAKJES, 1);
        GuhAdvancements.grant(player, "knuffeldal_taakje_gebracht");
        // 1.2.7: an item that was ready before the Burgemeester asked (a feestboeket made earlier...) still counts as made
        GuhAdvancements.grant(player, "knusfeest_" + taak.id() + "_gemaakt");
    }

    /** Everything is there: the feast at the feestbuffet (the Grote Knusfeest's finale, or the seasonal one). */
    static void feest(GuhNpcEntity npc, ServerPlayer player) {
        var bezig = nl.juiced.guhs.feature.evenementen.Evenementen.eventOf(player);
        if (bezig != null) {
            if (!(bezig instanceof KnusfeestEvenement)) {
                // 1.2.7: in another event (a kaasregen, a parade...): say so, the feast starts when you come back after it
                GuhQuests.say(player, npc, "quest.guhs.burgemeester.feest_wacht");
            }
            return;   // (already feasting)
        }
        var event = nl.juiced.guhs.feature.evenementen.Evenementen.start(nl.juiced.guhs.feature.evenementen.EvenementType.KNUSFEEST, player);
        if (event == null) {
            Feestbuffet.gevierd(player, false);   // (no room for a feast: the rewards all the same)
        }
    }

    /** The seasonal tasks: 2 or 3, chosen by the season (and the player), so they stay the same all season. */
    static Set<Feesttaak> seizoensTaken(ServerPlayer player, long nummer) {
        RandomSource random = RandomSource.create(nummer * 31 + player.getUUID().getLeastSignificantBits());
        List<Feesttaak> all = new ArrayList<>(List.of(Feesttaak.values()));
        net.minecraft.util.Util.shuffle(all, random);
        int n = 2 + random.nextInt(2);
        return EnumSet.copyOf(all.subList(0, n));
    }

    static void geefLijstje(ServerPlayer player) {
        if (!player.getInventory().hasAnyMatching(s -> s.is(KnuffeldalFeature.KNUSFEESTLIJSTJE.get()))) {
            Minigames.give(player, new ItemStack(KnuffeldalFeature.KNUSFEESTLIJSTJE.get()));
        }
    }

    /** The lijstje (right-click it): the tasks and their steps, in the chat. */
    static void toonLijstje(ServerPlayer player) {
        if (!Knusfeest.rondeBezig(player)) {
            player.sendSystemMessage(Component.translatable(Knusfeest.isKlaar(player) ? "gui.guhs.knusfeest.lijst_klaar" : "gui.guhs.knusfeest.lijst_leeg")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        player.sendSystemMessage(Component.translatable("gui.guhs.knusfeest.lijst_kop").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
        for (Component line : KnuffeldalItems.lijstje(player)) {
            player.sendSystemMessage(line);
        }
    }

    /** The talking screen with the list of this round. */
    static void scherm(GuhNpcEntity npc, ServerPlayer player, String tekst) {
        CompoundTag data = new CompoundTag();
        data.putString("Scherm", "burgemeester");
        data.putString("Tekst", tekst);
        data.putLong("Ronde", Knusfeest.ronde(player));
        ListTag taken = new ListTag();
        for (Feesttaak taak : Knusfeest.lijst(Knusfeest.taken(player))) {
            CompoundTag t = new CompoundTag();
            t.putString("Id", taak.id());
            Knusfeest.Stap stap = Knusfeest.stap(player, taak);
            t.putString("Stap", stap == null ? "" : stap.name().toLowerCase(java.util.Locale.ROOT));
            t.putBoolean("Bij", !KruimelMikaEntity.neemNiet(player, taak).isEmpty());
            taken.add(t);
        }
        data.put("Taken", taken);
        ListTag opties = new ListTag();
        CompoundTag geven = new CompoundTag();
        geven.putInt("Id", GEVEN);
        geven.putString("Tekst", "quest.guhs.burgemeester.optie.geven");
        opties.add(geven);
        data.put("Opties", opties);
        ModNetworking.sendTo(player, new KnuffeldalPayloads.Open(npc.getId(), data));
    }

    static void sluit(GuhNpcEntity npc, ServerPlayer player) {
        CompoundTag data = new CompoundTag();
        data.putBoolean("Sluit", true);
        ModNetworking.sendTo(player, new KnuffeldalPayloads.Open(npc.getId(), data));
    }

    /** The title of the Grote Knusfeest's finale. */
    public static boolean isKnuffelburgemeester(ServerPlayer player) {
        return GuhQuests.saved(player).getBooleanOr(Feestbuffet.TITEL, false);
    }
}
