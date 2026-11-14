package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;

/**
 * A player's quest state, kept in {@code SpelerData.extra("npc")} (CONTRACT_VADSRIJK 3/9):
 * <pre>
 *   q            {questId: {s: 1 active / 2 done, v: [progress per objective]}}
 *   zone1_klaar  true once the zone-1 story is done (wereld recolours the scars)
 *   knuffels / aanwijzingen / schatten   {id: true} (secrets found)
 *   wezens       {entity type id: times cheered up} (the bestiary page of the questlog)
 *   gezien       {flag: true} (story moments)
 * </pre>
 * Some objectives are counted live instead of stored: VERZAMEL (items in the inventory), LEVEL, GEHEIM counters.
 */
public final class Questlog {
    public static final String SLICE = "npc";
    public static final int ACTIEF = 1, KLAAR = 2;

    private Questlog() {
    }

    public static CompoundTag data(ServerPlayer p) {
        return Spelers.data(p).extra(SLICE);
    }

    static CompoundTag quests(ServerPlayer p) {
        CompoundTag d = data(p);
        if (!(d.get("q") instanceof CompoundTag q)) {
            CompoundTag n = new CompoundTag();
            d.put("q", n);
            return n;
        }
        return q;
    }

    public static CompoundTag map(ServerPlayer p, String naam) {
        CompoundTag d = data(p);
        if (!(d.get(naam) instanceof CompoundTag t)) {
            CompoundTag n = new CompoundTag();
            d.put(naam, n);
            return n;
        }
        return t;
    }

    public static int status(ServerPlayer p, String quest) {
        int s = quests(p).getCompoundOrEmpty(quest).getIntOr("s", 0);
        if (s == 0) {   // 1.10.1: a tutorial step inserted later counts as done for who already started the step after it
            String na = INGEVOEGD.get(quest);
            if (na != null && quests(p).getCompoundOrEmpty(na).getIntOr("s", 0) != 0) {
                return KLAAR;
            }
        }
        return s;
    }

    /**
     * 1.10.1: main steps inserted into an existing chain -> the step that used to follow. Players who already started that next
     * step (or got further) never see the new one: it counts as done for them (nothing is written into their data).
     */
    public static final Map<String, String> INGEVOEGD = Map.of("hq03c_opruimen", "hq04_kampvuur");

    public static boolean klaar(ServerPlayer p, String quest) {
        return status(p, quest) == KLAAR;
    }

    public static boolean actief(ServerPlayer p, String quest) {
        return status(p, quest) == ACTIEF;
    }

    static int[] v(ServerPlayer p, Quest q) {
        int[] v = quests(p).getCompoundOrEmpty(q.id()).getIntArray("v").orElse(new int[0]);
        if (v.length != q.doelen().size()) {
            int[] n = new int[q.doelen().size()];
            System.arraycopy(v, 0, n, 0, Math.min(v.length, n.length));
            v = n;
        }
        return v;
    }

    static void zetV(ServerPlayer p, Quest q, int[] v) {
        CompoundTag t = quests(p).getCompoundOrEmpty(q.id()).copy();
        t.putIntArray("v", v);
        quests(p).put(q.id(), t);
    }

    public static boolean vlag(ServerPlayer p, String naam) {
        return data(p).getBooleanOr(naam, false);
    }

    public static void zetVlag(ServerPlayer p, String naam, boolean aan) {
        data(p).putBoolean(naam, aan);
    }

    /** The active quests, in questlog order. */
    public static List<Quest> actieve(ServerPlayer p) {
        List<Quest> l = new ArrayList<>();
        for (Quest q : Quests.alle().values()) {
            if (actief(p, q.id())) {
                l.add(q);
            }
        }
        return l;
    }

    /** Could this NPC/the story give this quest now? (not taken yet, level, previous quests, class) */
    public static boolean beschikbaar(ServerPlayer p, Quest q) {
        // g-klassen: a class quest (job advancement) looks at that CLASS level, any other quest at the generic level; you are
        // always all 3 classes, so every class's quest can be offered (no 'current class' check any more)
        if (status(p, q.id()) != 0 || lvVoor(p, q) < q.lv()) {
            return false;
        }
        if (nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.questDicht(p, q)) {   // X-ZONE1: a quest of a closed zone
            return false;
        }
        for (String na : q.na()) {
            if (na.equals(RijdierQuests.POORT)) {   // (fix round 2: the mount return quests, once you own any mount)
                if (!RijdierQuests.poortOpen(p)) {
                    return false;
                }
                continue;
            }
            if (!klaar(p, na)) {
                return false;
            }
        }
        return q.klasse() == null || nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen.van(q.klasse()) != null;
    }

    /** g-klassen: the level a quest compares with: its class's level for a class quest, else the generic level. */
    public static int lvVoor(ServerPlayer p, Quest q) {
        return q.klasse() != null ? Spelers.level(p, q.klasse()) : Spelers.level(p);
    }

    // ---- objectives -----------------------------------------------------------------------------------------------------------

    /** The progress of objective i now (live objectives counted fresh). */
    public static int waarde(ServerPlayer p, Quest q, int i) {
        Quest.Doel d = q.doelen().get(i);
        return switch (d.type()) {
            case VERZAMEL -> Math.min(d.n(), telItems(p, item(d.doel())));
            case LEVEL -> Math.min(d.n(), lvVoor(p, q));
            case GEHEIM -> switch (d.doel()) {
                case "knuffel" -> Math.min(d.n(), map(p, "knuffels").size());
                case "aanwijzing" -> Math.min(d.n(), map(p, "aanwijzingen").size());
                case "schat" -> Math.min(d.n(), map(p, "schatten").size());
                default -> d.doel().startsWith("teller:") ? Math.min(d.n(), map(p, d.doel().substring(7)).size()) : Math.min(d.n(), v(p, q)[i]);
            };
            default -> Math.min(d.n(), v(p, q)[i]);
        };
    }

    public static boolean doelKlaar(ServerPlayer p, Quest q, int i) {
        return waarde(p, q, i) >= q.doelen().get(i).n();
    }

    public static boolean allesKlaar(ServerPlayer p, Quest q) {
        for (int i = 0; i < q.doelen().size(); i++) {
            if (!doelKlaar(p, q, i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Count +n on every matching stored objective of every active quest. Returns how many objectives moved. Tells the player
     * (overlay), and when a quest becomes complete, who to go back to.
     */
    public static int tel(ServerPlayer p, Quest.Doel.Type type, Predicate<Quest.Doel> past, int n) {
        int bewogen = 0;
        for (Quest q : actieve(p)) {
            boolean wasKlaar = allesKlaar(p, q);
            int[] v = v(p, q);
            boolean anders = false;
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if (d.type() == type && v[i] < d.n() && past.test(d)) {
                    v[i] = Math.min(d.n(), v[i] + n);
                    anders = true;
                    bewogen++;
                    p.sendOverlayMessage(Component.translatable("vadsrijk.npc.voortgang", Component.translatable(q.doelKey(i)), v[i], d.n())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            if (anders) {
                zetV(p, q, v);
                if (!wasKlaar && allesKlaar(p, q)) {
                    meldKlaar(p, q);
                }
            }
        }
        return bewogen;
    }

    /** A live objective (items, level, secrets) may have become complete: tell the player once per quest. */
    public static void kijkLive(ServerPlayer p) {
        for (Quest q : actieve(p)) {
            CompoundTag t = quests(p).getCompoundOrEmpty(q.id());
            boolean nu = allesKlaar(p, q);
            if (nu && !t.getBooleanOr("gemeld", false)) {
                meldKlaar(p, q);
            } else if (!nu && t.getBooleanOr("gemeld", false)) {
                CompoundTag c = t.copy();
                c.putBoolean("gemeld", false);
                quests(p).put(q.id(), c);
            }
        }
    }

    private static void meldKlaar(ServerPlayer p, Quest q) {
        CompoundTag t = quests(p).getCompoundOrEmpty(q.id()).copy();
        t.putBoolean("gemeld", true);
        quests(p).put(q.id(), t);
        NpcDef d = Npcs.van(q.inleveren());
        Component wie = d == null ? Component.literal("?") : Component.translatable(d.naamKey());
        p.sendSystemMessage(Component.translatable("vadsrijk.npc.quest_klaar", Component.translatable(q.titelKey()), wie).withStyle(ChatFormatting.GOLD));
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
        NpcMarkers.vies(p);
    }

    // ---- accepting and finishing ----------------------------------------------------------------------------------------------

    public static void neemAan(ServerPlayer p, Quest q) {
        CompoundTag t = new CompoundTag();
        t.putInt("s", ACTIEF);
        t.putIntArray("v", new int[q.doelen().size()]);
        quests(p).put(q.id(), t);
        for (Quest.Beloning.Ding d : q.startItems()) {
            geef(p, new ItemStack(item(d.item()), d.n()));
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.npc.quest_nieuw", Component.translatable(q.titelKey()))
                .withStyle(q.hoofd() ? ChatFormatting.GOLD : ChatFormatting.YELLOW));
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.1f);
        Tips.toon(p, "vadsrijk.npc.tip.quests");
        // a campfire objective whose area already holds your checkpoint counts at once (kern only reports a NEW checkpoint)
        net.minecraft.core.BlockPos cp = Spelers.data(p).checkpoint;
        var g = cp == null ? null : nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.op(cp.getX(), cp.getY(), cp.getZ());
        if (g != null) {
            tel(p, Quest.Doel.Type.KAMPVUUR, d -> d.doel().equals(g.id()), 1);
        }
        // the same for "go to area x" while you already stand in it (kern only reports ENTERING an area)
        var hier = nl.juiced.guhs.feature.vadsrijk.kern.Gebieden.op(p.getX(), p.getY(), p.getZ());
        if (hier != null && Vadsrijk.in(p)) {
            tel(p, Quest.Doel.Type.GEBIED, d -> !d.doel().startsWith("poi:") && List.of(d.doel().split("\\|")).contains(hier.id()), 1);
        }
        NpcFtb.aangenomen(p, q);
        NpcMarkers.vies(p);
        kijkLive(p);
    }

    /** Hands in: takes the collected items, gives the reward, marks it done. The caller shows the dialogue and the action. */
    public static void voltooi(ServerPlayer p, Quest q) {
        for (int i = 0; i < q.doelen().size(); i++) {
            Quest.Doel d = q.doelen().get(i);
            if (d.type() == Quest.Doel.Type.VERZAMEL) {
                neemItems(p, item(d.doel()), d.n());
            }
        }
        CompoundTag t = new CompoundTag();
        t.putInt("s", KLAAR);
        quests(p).put(q.id(), t);
        nl.juiced.guhs.feature.vadsrijk.zonepoort.ZonePoort.questKlaar(p, q.id());   // 1.10.2: a finished quest of zone N = zone N entered (level cap)
        Beloningen.geef(p, q);
        p.sendSystemMessage(Component.translatable("vadsrijk.npc.quest_af", Component.translatable(q.titelKey())).withStyle(ChatFormatting.GOLD));
        NpcFtb.klaar(p, q);
        NpcMarkers.vies(p);
    }

    /** Dev/tests: forget one quest. */
    public static void vergeet(ServerPlayer p, String quest) {
        quests(p).remove(quest);
        NpcMarkers.vies(p);
    }

    /** Dev: forget everything of the npc slice for this player. */
    public static void wisAlles(ServerPlayer p) {
        Spelers.data(p).extra.remove(SLICE);
        NpcMarkers.vies(p);
    }

    // ---- items -------------------------------------------------------------------------------------------------------------------

    public static Item item(String id) {
        Identifier r = Identifier.tryParse(id.contains(":") ? id : "guhs:" + id);
        Item i = r == null ? null : BuiltInRegistries.ITEM.getValue(r);
        return i == null ? Items.AIR : i;
    }

    static int telItems(ServerPlayer p, Item item) {
        if (item == Items.AIR) {
            return 0;
        }
        int n = 0;
        for (int s = 0; s < p.getInventory().getContainerSize(); s++) {
            ItemStack st = p.getInventory().getItem(s);
            if (st.is(item)) {
                n += st.getCount();
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, n + nl.juiced.guhs.feature.vadsrijk.opslag.Materiaaltas.tel(p, item));   // T-OPSLAG: + the materiaaltas
    }

    static void neemItems(ServerPlayer p, Item item, int n) {
        for (int s = 0; s < p.getInventory().getContainerSize() && n > 0; s++) {
            ItemStack st = p.getInventory().getItem(s);
            if (st.is(item)) {
                int weg = Math.min(n, st.getCount());
                st.shrink(weg);
                n -= weg;
            }
        }
        if (n > 0) {
            nl.juiced.guhs.feature.vadsrijk.opslag.Materiaaltas.neem(p, item, n);   // T-OPSLAG: the rest from the materiaaltas
        }
    }

    public static void geef(ServerPlayer p, ItemStack st) {
        if (st.isEmpty()) {
            return;
        }
        if (!p.getInventory().add(st)) {
            p.drop(st, false);
        }
    }

    /** The class whose level a quest looks at (its own class, else the current one). */
    static String klasseVan(ServerPlayer p, @Nullable Quest q) {
        String k = q == null || q.klasse() == null ? Spelers.data(p).klasse : q.klasse();
        return Klassen.van(k) == null ? Klassen.RIDDER : k;
    }
}
