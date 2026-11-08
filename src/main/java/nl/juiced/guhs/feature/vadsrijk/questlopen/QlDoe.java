package nl.juiced.guhs.feature.vadsrijk.questlopen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcHaken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPlekken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRegistry;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestPlek;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;
import nl.juiced.guhs.feature.vadsrijk.questrobot.QuestRobot;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * U-QUESTLOPEN: completes ONE step through the real hooks. This is the driver of U-QUESTROBOT's {@code QuestRobot} (same hook per
 * objective type, copied here because the robot's methods are private and work on a whole run; see NOTITIES_U_QUESTLOPEN), but
 * for the REAL player, in three tiers:
 * <ol>
 *   <li>{@link #echt}: what the player would do: click the live NPC (the real dialogue opens; taking/handing in/talking counts
 *       when it closes and the quest's own actions run: scenes, flights, rewards), stand on the spot (the zone triggers: story
 *       scenes, flashes, arrival films), enter the area, light the campfire;</li>
 *   <li>{@link #haak}: when that did not count: the hook the game itself calls for it (NpcApi.meld, Haken.VERSLAGEN with a mob of
 *       the right kind in the right area, the escort's arrival, the hand-in + actie_klaar...);</li>
 *   <li>{@link #dwing}: the counter itself (Questlog.tel / neemAan / voltooi), so the line never gets stuck.</li>
 * </ol>
 * {@code stil}: for "Spring naar" (no dialogue stays open: the click's pending action runs at once, like the robot).
 */
public final class QlDoe {
    private QlDoe() {
    }

    // ---- tier 1: the real thing ----------------------------------------------------------------------------------------------------

    public static void echt(ServerPlayer p, QlStap s, boolean stil) {
        Quest q = s.q();
        if (q == null) {
            return;
        }
        switch (s.soort()) {
            case OPENING -> {
                if (!stil) {
                    nl.juiced.guhs.feature.vadsrijk.npc.Verhaal.opening(p);
                } else {
                    Spelers.data(p).gezien.add("npc:opening");
                }
            }
            case NEEM -> {
                if (Questlog.status(p, q.id()) != 0) {
                    return;
                }
                RijkGuh g = npc(p, q.gever());
                if (g != null) {
                    Praat.vergeet(p);
                    QuestMotor.vergeet(p);
                    QuestMotor.klik(p, g);
                    if (stil) {
                        QuestMotor.kiesVoorTest(p, QuestMotor.AANNEMEN_OPTIE);
                        QuestMotor.afronden(p);
                        spoel(p);
                    }
                }
            }
            case LEVER -> {
                zorgActief(p, q);
                if (!Questlog.allesKlaar(p, q)) {
                    return;
                }
                klik(p, q.inleveren(), stil);
            }
            case DOEL -> {
                zorgActief(p, q);
                doel(p, q, s.doel(), stil);
            }
        }
    }

    private static void doel(ServerPlayer p, Quest q, int i, boolean stil) {
        Quest.Doel d = q.doelen().get(i);
        switch (d.type()) {
            case PRAAT -> klik(p, d.doel(), stil);
            case GEBIED, GEHEIM -> {
                if (d.doel().startsWith("poi:")) {
                    BlockPos b = QlPlek.poi(d.doel().substring(4));
                    if (b != null) {
                        opPlek(p, b, stil);
                    }
                } else if (d.type() == Quest.Doel.Type.GEBIED) {
                    String g = d.doel().split("\\|")[0];
                    Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, g));
                } else {
                    geheim(p, q, i, d);
                }
            }
            case KAMPVUUR -> {
                BlockPos b = d.doel().equals("*") ? NpcPlekken.KAMPPLEIN : NpcPlekken.KAMPVUUR.get(d.doel());
                if (b != null) {
                    Haken.KAMPVUUR.forEach(h -> h.accept(p, b));
                }
            }
            case ESCORTE -> {
                RijkGuh g = npc(p, d.doel());
                if (g != null && !p.getUUID().equals(g.volgt())) {
                    QuestMotor.klik(p, g);   // (starts following)
                    if (stil) {
                        QuestMotor.afronden(p);
                        Praat.vergeet(p);
                    }
                }
            }
            case POP -> {
                for (int k = 0; k < d.n() && !Questlog.doelKlaar(p, q, i); k++) {
                    Haken.BLOK_GERAAKT.forEach(h -> h.accept(p, NpcPlekken.TRAINING));
                }
            }
            case KLASSE -> NpcApi.meld(p, "klasse", Spelers.data(p).klasse);
            case LEVEL -> zetLevel(p, q.klasse(), d.n());
            case MELD -> {
                String plek = d.plek();
                if (plek.startsWith("npc:")) {
                    klik(p, plek.substring(4), stil);
                } else if (!plek.isEmpty()) {
                    BlockPos b = QuestPlek.pos(QlPlek.level(p), plek);
                    if (b != null) {
                        opPlek(p, b, stil);
                    }
                } else if (d.doel().startsWith("quiz:")) {
                    NpcApi.meld(p, "quiz", d.doel().substring(5));
                }
            }
            default -> {
                // OPVROLIJK / ELITE / BAAS / VERZAMEL: a fight: tier 2 (the boss flow shows a real boss first, see QlBaas)
            }
        }
    }

    // ---- tier 2: the hook the game calls -------------------------------------------------------------------------------------------

    public static void haak(ServerPlayer p, QlStap s) {
        Quest q = s.q();
        if (q == null) {
            return;
        }
        switch (s.soort()) {
            case OPENING -> Spelers.data(p).gezien.add("npc:opening");
            case NEEM -> {
                if (Questlog.status(p, q.id()) == 0) {
                    neemAan(p, q);
                }
            }
            case LEVER -> {
                zorgActief(p, q);
                if (Questlog.actief(p, q.id())) {
                    for (int i = 0; i < q.doelen().size(); i++) {
                        if (!Questlog.doelKlaar(p, q, i)) {
                            dwingDoel(p, q, i);
                        }
                    }
                    Questlog.voltooi(p, q);
                    QuestMotor.doeActie(p, q.actieKlaar(), npc(p, q.inleveren()));
                }
            }
            case DOEL -> {
                zorgActief(p, q);
                int i = s.doel();
                Quest.Doel d = q.doelen().get(i);
                if (Questlog.doelKlaar(p, q, i)) {
                    return;
                }
                switch (d.type()) {
                    case OPVROLIJK, ELITE, BAAS -> {
                        VadsrijkMob mob = mob(p, d.doel(), d.type() == Quest.Doel.Type.BAAS, d.type() == Quest.Doel.Type.ELITE);
                        for (int k = 0; mob != null && k < d.n() && !Questlog.doelKlaar(p, q, i) && Questlog.actief(p, q.id()); k++) {
                            verslagen(p, mob);
                        }
                    }
                    case VERZAMEL -> {
                        if (d.kans() > 0) {
                            VadsrijkMob mob = mob(p, d.bron().isEmpty() ? "*" : d.bron(), false, false);
                            int max = Math.max(60, (int) Math.ceil(d.n() / d.kans() * 6));
                            for (int k = 0; mob != null && k < max && !Questlog.doelKlaar(p, q, i) && Questlog.actief(p, q.id()); k++) {
                                verslagen(p, mob);
                            }
                        }
                    }
                    case ESCORTE -> {
                        RijkGuh g = Npcs.levend(QlPlek.level(p), d.doel());
                        if (g != null && p.getUUID().equals(g.volgt())) {
                            g.volg(null);
                        }
                        Questlog.tel(p, Quest.Doel.Type.ESCORTE, x -> x == d, 1);
                    }
                    case MELD -> {
                        if (!d.plek().isEmpty() && !d.plek().startsWith("npc:")) {
                            BlockPos b = QuestPlek.pos(QlPlek.level(p), d.plek());
                            if (b != null) {
                                plekHaken(p, b);
                            }
                        }
                        int c = d.doel().indexOf(':');
                        String soort = c < 0 ? d.doel() : d.doel().substring(0, c), wat = c < 0 ? "" : d.doel().substring(c + 1);
                        for (int k = 0; k < d.n() && !Questlog.doelKlaar(p, q, i); k++) {
                            NpcApi.meld(p, soort, wat);
                        }
                    }
                    case GEBIED, GEHEIM -> {
                        if (d.doel().startsWith("poi:")) {
                            BlockPos b = QlPlek.poi(d.doel().substring(4));
                            if (b != null) {
                                plekHaken(p, b);
                            }
                        }
                    }
                    case PRAAT -> Questlog.tel(p, Quest.Doel.Type.PRAAT, x -> x == d, 1);
                    default -> {
                    }
                }
            }
        }
    }

    // ---- tier 3: the counter -------------------------------------------------------------------------------------------------------

    public static void dwing(ServerPlayer p, QlStap s) {
        Quest q = s.q();
        if (q == null) {
            return;
        }
        switch (s.soort()) {
            case OPENING -> Spelers.data(p).gezien.add("npc:opening");
            case NEEM -> {
                if (Questlog.status(p, q.id()) == 0) {
                    zetLevel(p, q.klasse(), q.lv());
                    neemAan(p, q);
                }
            }
            case DOEL -> {
                zorgActief(p, q);
                dwingDoel(p, q, s.doel());
            }
            case LEVER -> haak(p, s);
        }
    }

    static void dwingDoel(ServerPlayer p, Quest q, int i) {
        Quest.Doel d = q.doelen().get(i);
        if (Questlog.doelKlaar(p, q, i)) {
            return;
        }
        switch (d.type()) {
            case VERZAMEL -> Questlog.geef(p, new ItemStack(Questlog.item(d.doel()), d.n() - Questlog.waarde(p, q, i)));
            case LEVEL -> zetLevel(p, q.klasse(), d.n());
            case GEHEIM -> {
                String map = switch (d.doel()) {
                    case "knuffel" -> "knuffels";
                    case "aanwijzing" -> "aanwijzingen";
                    case "schat" -> "schatten";
                    default -> d.doel().startsWith("teller:") ? d.doel().substring(7) : null;
                };
                if (map != null) {
                    CompoundTag t = Questlog.map(p, map);
                    for (int k = 0; t.size() < d.n() && k < d.n() + 64; k++) {
                        t.putBoolean("questlopen_" + k, true);
                    }
                    Questlog.kijkLive(p);
                } else {
                    Questlog.tel(p, d.type(), x -> x == d, d.n());
                }
            }
            default -> Questlog.tel(p, d.type(), x -> x == d, d.n());
        }
    }

    // ---- helpers -------------------------------------------------------------------------------------------------------------------

    /** A quest whose step comes up but that was never started (an auto quest whose start was missed): take it now. */
    static void zorgActief(ServerPlayer p, Quest q) {
        if (Questlog.status(p, q.id()) == 0) {
            zetLevel(p, q.klasse(), q.lv());
            neemAan(p, q);
        }
    }

    /** Taking a quest as the "yes" answer does it: accept, the escort starts following, actie_aanname runs. */
    static void neemAan(ServerPlayer p, Quest q) {
        Questlog.neemAan(p, q);
        RijkGuh gever = npc(p, q.gever());
        for (Quest.Doel d : q.doelen()) {
            if (d.type() == Quest.Doel.Type.ESCORTE && gever != null && gever.isAddedToLevel() && gever.npcId().equals(d.doel())) {
                gever.volg(p);
            }
        }
        if (!q.actieAanname().isEmpty()) {
            QuestMotor.doeActie(p, q.actieAanname(), gever);
        }
    }

    static void klik(ServerPlayer p, String npc, boolean stil) {
        RijkGuh g = npc(p, npc);
        if (g == null) {
            return;
        }
        Praat.vergeet(p);
        QuestMotor.vergeet(p);
        QuestMotor.klik(p, g);
        if (stil) {
            QuestMotor.afronden(p);
            spoel(p);
        }
    }

    /** The live NPC guh, or a stand-in made from its definition (not added to the world, like the robot's). */
    @Nullable
    static RijkGuh npc(ServerPlayer p, String id) {
        NpcDef d = Npcs.van(id);
        ServerLevel sl = QlPlek.level(p);
        if (d == null || sl == null) {
            return null;
        }
        RijkGuh echt = Npcs.levend(sl, id);
        if (echt != null) {
            return echt;
        }
        RijkGuh g = NpcRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
        if (g != null) {
            g.stelIn(d);
            g.snapTo(p.getX() + 1, p.getY(), p.getZ());
        }
        return g;
    }

    /** Stand on exactly this spot for the quest tickers (npc poi check + the zones' story triggers); the real player stays put. */
    static void opPlek(ServerPlayer p, BlockPos b, boolean stil) {
        plekHaken(p, b);
        if (stil) {
            spoel(p);
        }
    }

    static void plekHaken(ServerPlayer p, BlockPos b) {
        net.minecraft.world.phys.Vec3 was = p.position();
        p.setPos(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        try {
            NpcHaken.robotPlekken(p);
            ServerLevel sl = QlPlek.level(p);
            for (var h : QuestRobot.PLEK_HAKEN) {
                try {
                    h.accept(p, sl);
                } catch (RuntimeException ex) {
                    QuestLoper.LOG.warn("questlopen: plek-haak at {}", b, ex);
                }
            }
        } finally {
            p.setPos(was.x, was.y, was.z);
        }
    }

    /** The zones' "finish the scene that runs now" (U-QUESTROBOT's SPOEL list): only for "Spring naar". */
    static void spoel(ServerPlayer p) {
        for (var s : QuestRobot.SPOEL) {
            try {
                s.accept(p);
            } catch (RuntimeException ex) {
                QuestLoper.LOG.warn("questlopen: spoel", ex);
            }
        }
    }

    private static void geheim(ServerPlayer p, Quest q, int i, Quest.Doel d) {
        Map<String, BlockPos> m = switch (d.doel()) {
            case "knuffel" -> NpcPlekken.KNUFFEL;
            case "aanwijzing" -> NpcPlekken.AANWIJZING;
            case "schat" -> NpcPlekken.SCHAT;
            default -> null;
        };
        String soort = switch (d.doel()) {
            case "knuffel" -> "knuffels";
            case "aanwijzing" -> "aanwijzingen";
            default -> "schatten";
        };
        if (m == null) {
            return;   // (a teller: tier 3 fills it)
        }
        ServerLevel sl = QlPlek.level(p);
        for (var e : m.entrySet()) {
            if (Questlog.doelKlaar(p, q, i)) {
                break;
            }
            NpcHaken.robotVind(p, sl, soort, e.getKey(), e.getValue());
            Praat.vergeet(p);
        }
    }

    /** A mob of the kind an objective wants ("*", "gebied:a|b" or an entity id), standing in the right area (not added). */
    @Nullable
    static VadsrijkMob mob(ServerPlayer p, String doel, boolean baas, boolean elite) {
        BlockPos plek = null;
        Entity e;
        if (doel.equals("*") || doel.startsWith("gebied:")) {
            if (doel.startsWith("gebied:")) {
                for (String g : doel.substring(7).split("\\|")) {
                    if (plek == null) {
                        plek = plekIn(g, baas);
                    }
                }
            }
            e = baas ? baasType(p) == null ? null : baasType(p).create(p.level(), EntitySpawnReason.EVENT)
                    : KernRegistry.PROEFKLODDER.get().create(p.level(), EntitySpawnReason.EVENT);
        } else {
            Identifier id = Identifier.tryParse(doel.contains(":") ? doel : "guhs:" + doel);
            if (id == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(id)) {
                return null;
            }
            e = BuiltInRegistries.ENTITY_TYPE.getValue(id).create(p.level(), EntitySpawnReason.EVENT);
        }
        if (!(e instanceof VadsrijkMob vm)) {
            return null;
        }
        vm.stelIn(Math.max(1, Spelers.level(p)), null, elite, Set.of());
        BlockPos b = plek != null ? plek : p.blockPosition();
        vm.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5);
        return vm;
    }

    @Nullable
    private static EntityType<?> eenBaas;

    @Nullable
    private static EntityType<?> baasType(ServerPlayer p) {
        if (eenBaas == null) {
            for (EntityType<?> t : BuiltInRegistries.ENTITY_TYPE) {
                if (!BuiltInRegistries.ENTITY_TYPE.getKey(t).getNamespace().equals("guhs")) {
                    continue;
                }
                try {
                    Entity e = t.create(p.level(), EntitySpawnReason.EVENT);
                    if (e instanceof VadsrijkBaas) {
                        eenBaas = t;
                    }
                    if (e != null) {
                        e.discard();
                    }
                    if (eenBaas != null) {
                        break;
                    }
                } catch (RuntimeException ex) {
                    // (some entities cannot be made outside the world)
                }
            }
        }
        return eenBaas;
    }

    @Nullable
    private static BlockPos plekIn(String g, boolean baas) {
        List<BlockPos> kandidaten = new ArrayList<>();
        if (baas) {
            kandidaten.add(QuestPlek.BAAS.get(g));
            kandidaten.add(NpcPlekken.BAAS.get(g));
        }
        kandidaten.add(QuestPlek.GEBIED.get(g));
        kandidaten.add(NpcPlekken.GEBIED_DOEL.get(g));
        kandidaten.add(NpcPlekken.KAMPVUUR.get(g));
        for (BlockPos b : kandidaten) {
            if (b == null) {
                continue;
            }
            for (int dy : new int[] {0, 1, 2, 4, -2, 8}) {
                Gebieden.Gebied x = Gebieden.op(b.getX() + 0.5, b.getY() + dy, b.getZ() + 0.5);
                if (x != null && x.id().equals(g)) {
                    return b.above(dy);
                }
            }
        }
        return null;
    }

    static void verslagen(ServerPlayer p, VadsrijkMob mob) {
        Haken.Verslagen v = new Haken.Verslagen(mob, Set.of(p));
        for (var h : Haken.VERSLAGEN) {
            try {
                h.accept(v);
            } catch (RuntimeException ex) {
                QuestLoper.LOG.warn("questlopen: VERSLAGEN listener", ex);
            }
        }
    }

    /** At least this level (class quest: that class's level + the generic one; else generic + your current class). */
    public static void zetLevel(ServerPlayer p, @Nullable String klasse, int lv) {
        lv = Math.max(1, Math.min(Levels.MAX, lv));
        SpelerData d = Spelers.data(p);
        if (klasse != null && d.level(klasse) < lv) {
            Spelers.zetKlasseLevel(p, klasse, lv);
        }
        if (d.level() < lv) {
            Spelers.zetGeneriek(p, lv);
        }
        if (d.klasseLevel() < lv) {
            Spelers.zetKlasseLevel(p, d.klasse, lv);
        }
        Questlog.kijkLive(p);
    }

    /** True when an item id exists (a VERZAMEL objective that names a missing item can only be forced). */
    static boolean bestaat(String item) {
        return Questlog.item(item) != Items.AIR;
    }
}
