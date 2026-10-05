package nl.juiced.guhs.feature.guhpixel.reisbureau;

import java.util.function.Predicate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Reisagent-guh (NPC {@code REISBUREAU_AGENT}) behind the counter of Reisbureau "De Vadsvakantie". His questline, per
 * player ({@link Reizen#stap}):
 * <ol>
 *   <li>kennismaken: he explains the trips; you say your guh would like to go ({@link Reizen#KOFFER});</li>
 *   <li>pack the koffertje: bring {@value #KNABBELS} kaasknabbels, {@value #WOL} wool (any colour) and {@value #PAPIER}
 *   paper ({@link Reizen#PROEF});</li>
 *   <li>send your first guh on the five-minute "proefreisje om de hoek" at the counter and collect it: the Reisstempel
 *   ({@link Reizen#KLAAR}; given by Reizen when the guh is collected).</li>
 * </ol>
 * Afterwards he chats, and gives a new Reisstempel when you have none (every Reisbalie you craft uses one up).
 */
public final class Reisagent implements NpcRole {
    public static final Reisagent ROL = new Reisagent();
    public static final int KNABBELS = 4;
    public static final int WOL = 1, PAPIER = 1;
    /** Answer ids of the talking screen. */
    public static final int JA = 1, WAT = 2, OKE = 3, STEMPEL = 4;
    private static final int PRAATJES = 4;
    private static final String Q = "quest.guhs.reisbureau.";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        switch (Reizen.stap(p)) {
            case Reizen.NIEUW -> praat(p, npc, "hallo", new Object[] {}, new Praat.Optie(JA, Q + "optie.ja"), new Praat.Optie(WAT, Q + "optie.wat"));
            case Reizen.KOFFER -> {
                if (heeftAlles(p)) {
                    neem(p, s -> s.is(ModItems.KAAS_KNABBELS.get()), KNABBELS);
                    neem(p, s -> s.is(ItemTags.WOOL), WOL);
                    neem(p, s -> s.is(Items.PAPER), PAPIER);
                    Reizen.zetStap(p, Reizen.PROEF);
                    GuhAdvancements.grant(p, "reisbureau_koffer");
                    praat(p, npc, "koffer_klaar", new Object[] {}, new Praat.Optie(OKE, Q + "optie.oke"));
                } else {
                    praat(p, npc, "koffer_nog", new Object[] {KNABBELS, WOL, PAPIER});
                }
            }
            case Reizen.PROEF -> praat(p, npc, Reizen.reis(p.level().getServer(), p.getUUID()) != null ? "proef_weg" : "proef_nog", new Object[] {});
            default -> praat(p, npc, "praatje." + p.getRandom().nextInt(PRAATJES), new Object[] {}, new Praat.Optie(STEMPEL, Q + "optie.stempel"));
        }
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        int stap = Reizen.stap(p);
        if (optie == WAT && stap == Reizen.NIEUW) {
            praat(p, npc, "uitleg", new Object[] {}, new Praat.Optie(JA, Q + "optie.ja"));
        } else if (optie == JA && stap == Reizen.NIEUW) {
            Reizen.zetStap(p, Reizen.KOFFER);
            GuhAdvancements.grant(p, "reisbureau_kennis");
            praat(p, npc, "koffer", new Object[] {KNABBELS, WOL, PAPIER}, new Praat.Optie(OKE, Q + "optie.oke"));
        } else if (optie == STEMPEL && stap >= Reizen.KLAAR) {
            if (tel(p, s -> s.is(ReisbureauSlice.STEMPEL.get())) > 0) {
                praat(p, npc, "stempel_heb_je", new Object[] {});
            } else {
                Minigames.give(p, new ItemStack(ReisbureauSlice.STEMPEL.get()));
                praat(p, npc, "stempel_nieuw", new Object[] {});
            }
        } else if (optie == OKE) {
            Praat.sluit(p);
        }
    }

    private static void praat(ServerPlayer p, GuhNpcEntity npc, String key, Object[] args, Praat.Optie... opties) {
        Praat.open(p, npc, null, Q + key, args, opties);
    }

    /** The three things of the koffertje, all at once. */
    public static boolean heeftAlles(ServerPlayer p) {
        return tel(p, s -> s.is(ModItems.KAAS_KNABBELS.get())) >= KNABBELS && tel(p, s -> s.is(ItemTags.WOOL)) >= WOL && tel(p, s -> s.is(Items.PAPER)) >= PAPIER;
    }

    private static int tel(ServerPlayer p, Predicate<ItemStack> wat) {
        Inventory inv = p.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && wat.test(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static void neem(ServerPlayer p, Predicate<ItemStack> wat, int aantal) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && aantal > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && wat.test(s)) {
                int n = Math.min(aantal, s.getCount());
                s.shrink(n);
                aantal -= n;
            }
        }
    }

    private Reisagent() {
    }
}
