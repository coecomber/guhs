package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;

/** Quest rewards: xp (current class), Vadsmunten, items, and gear rolled by kern (the first class weapon, a piece of loot). */
public final class Beloningen {
    private Beloningen() {
    }

    static void geef(ServerPlayer p, Quest q) {
        Quest.Beloning b = q.beloning();
        if (b.xp() > 0) {
            Spelers.geefXp(p, b.xp(), "quest");
        }
        List<ItemStack> buit = new ArrayList<>();
        if (b.wapen()) {
            BasisType t = wapenVoor(p);
            if (t != null) {
                buit.add(Rollen.stack(Rollen.rol(t, Math.max(1, Spelers.level(p)), Zeldzaamheid.ONGEWOON, p.getRandom())));
            }
        }
        if (!b.loot().isEmpty()) {
            BasisType t = stukVoor(p, nl.juiced.guhs.feature.vadsrijk.voltooiing.ZoneBoeken.zoneVan(q.id()));
            if (t != null) {
                buit.add(Rollen.stack(Rollen.rol(t, Math.max(1, Spelers.level(p)), Zeldzaamheid.van(b.loot()), p.getRandom())));
            }
        }
        if (!buit.isEmpty() || b.munten() > 0) {
            // (kern's loot drop: the rarity beam, personal pick-up; the coins go straight into the purse)
            LootBundel.laatVallen(p, p.position().add(p.getLookAngle().multiply(1.5, 0, 1.5)).add(0, 1, 0), new LootTabel.Resultaat(buit, b.munten()), 1);
            if (!buit.isEmpty()) {
                Tips.toon(p, "vadsrijk.npc.tip.loot");
            }
        }
        for (Quest.Beloning.Ding d : b.items()) {
            Questlog.geef(p, new ItemStack(Questlog.item(d.item()), d.n()));
        }
    }

    /** A weapon base type of the player's class (the first one registered: the class's starter weapon). */
    @Nullable
    static BasisType wapenVoor(ServerPlayer p) {
        String k = Spelers.data(p).klasse;
        for (BasisType t : BasisTypes.alle()) {
            if (t.slot() == Slot.WAPEN && k.equals(t.klasse())) {
                return t;
            }
        }
        for (BasisType t : BasisTypes.alle()) {
            if (t.slot() == Slot.WAPEN) {
                return t;
            }
        }
        return null;
    }

    /** A random piece the player can use: a weapon of their class or any armour. */
    @Nullable
    static BasisType stukVoor(ServerPlayer p, int zone) {
        return nl.juiced.guhs.feature.vadsrijk.kern.loot.ZonePools.stuk(zone, Spelers.data(p).klasse, p.getRandom());   // 1101-balans: only this zone's pool
    }
}
