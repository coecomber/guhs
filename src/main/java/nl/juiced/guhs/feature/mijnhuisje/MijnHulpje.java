package nl.juiced.guhs.feature.mijnhuisje;

import java.util.function.Predicate;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.onderwol.OnderWolFeature;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 (ronde 37-40): the little quest "Mijnguh-hulpje" of the Mijnguh. He asks for {@value #VILT} viltsteen, a guhklei and
 * a Vahoegiet; for them you get the Mijn-guhhuisje and a Mijnlampje for your guh (the pink miner's helmet with a lamp of
 * feature/mijnhulp, the "mijnwerkershelmpje"). Afterwards he makes you another huisje whenever you bring the same again
 * with the Vahoegiet in your hand (without the helmet).
 * <p>
 * Any Mijnguh can give it: {@link #praat} is called at the start of his talk (the kaasmijn's Mijnguh now; the Mijnguh of
 * the underground mijnguhdorp only has to call it too). Saved per player: {@value #KEY} (0 not asked, 1 asked, 2 done).
 */
public final class MijnHulpje {
    public static final String KEY = "guhs_mijnhuisje_hulpje";
    public static final int NIET = 0, GEVRAAGD = 1, KLAAR = 2;
    public static final int VILT = 16;

    private MijnHulpje() {
    }

    public static int stand(ServerPlayer p) {
        return GuhQuests.saved(p).getIntOr(KEY, NIET);
    }

    static void zet(ServerPlayer p, int stand) {
        GuhQuests.saved(p).putInt(KEY, stand);
    }

    /** The Mijnguh is spoken to. True: the quest handled the talk (he said his line; the shop is opened when it fits). */
    public static boolean praat(GuhNpcEntity npc, ServerPlayer p) {
        switch (stand(p)) {
            case NIET -> {
                GuhQuests.say(p, npc, "quest.guhs.mijnhulpje.vraag");
                GuhQuests.say(p, npc, "quest.guhs.mijnhulpje.vraag.2", VILT);
                zet(p, GEVRAAGD);
                return true;
            }
            case GEVRAAGD -> {
                if (!heeftAlles(p)) {
                    GuhQuests.say(p, npc, "quest.guhs.mijnhulpje.nog_niet", tel(p, OnderWolFeature.VILTSTEEN.get().asItem()), VILT,
                            tel(p, OnderWolFeature.GUHKLEI.get().asItem()), tel(p, OnderWolFeature.VAHOEGIET.get()));
                    npc.openShop(p);
                    return true;
                }
                neem(p);
                Minigames.give(p, new ItemStack(MijnhuisjeFeature.ITEM.get()));
                Minigames.give(p, new ItemStack(ModItems.clothingItem(GuhClothes.MIJNLAMPJE)));
                zet(p, KLAAR);
                GuhQuests.say(p, npc, "quest.guhs.mijnhulpje.klaar");
                GuhQuests.say(p, npc, "quest.guhs.mijnhulpje.klaar.2");
                npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6f, 1.4f);
                return true;
            }
            default -> {
                if (p.getMainHandItem().is(OnderWolFeature.VAHOEGIET.get()) && heeftAlles(p)) {
                    neem(p);
                    Minigames.give(p, new ItemStack(MijnhuisjeFeature.ITEM.get()));
                    GuhQuests.say(p, npc, "quest.guhs.mijnhulpje.nog_een");
                    return true;
                }
                return false;
            }
        }
    }

    public static boolean heeftAlles(ServerPlayer p) {
        return tel(p, OnderWolFeature.VILTSTEEN.get().asItem()) >= VILT && tel(p, OnderWolFeature.GUHKLEI.get().asItem()) >= 1
                && tel(p, OnderWolFeature.VAHOEGIET.get()) >= 1;
    }

    private static int tel(ServerPlayer p, Item item) {
        return p.getInventory().countItem(item);
    }

    private static void neem(ServerPlayer p) {
        haal(p.getInventory(), s -> s.is(OnderWolFeature.VILTSTEEN.get().asItem()), VILT);
        haal(p.getInventory(), s -> s.is(OnderWolFeature.GUHKLEI.get().asItem()), 1);
        haal(p.getInventory(), s -> s.is(OnderWolFeature.VAHOEGIET.get()), 1);
    }

    private static void haal(Inventory inv, Predicate<ItemStack> wat, int n) {
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (wat.test(s)) {
                int weg = Math.min(n, s.getCount());
                s.shrink(weg);
                n -= weg;
            }
        }
        inv.setChanged();
    }
}
