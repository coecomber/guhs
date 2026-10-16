package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.einde2.geestenwereld.GeestBlokken;
import nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld;
import nl.juiced.guhs.feature.einde2.geestenwereld.Lotuspuzzel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * einde2 phase B, slice geest_spel: the line {@value GeestLijnen#WITTE_LOTUS} (DESIGN 5 "De Witte Lotus, vijf vleugels"):
 * <ol start="0">
 *   <li>the gate: lay the nine puzzle tiles as a lotus ({@link Lotuspuzzel#OPGELOST}); until then the doorway sends you
 *       back. The puzzle is shared: whoever solves it opens it for everyone of this step who stands by; it turns back only
 *       when a newcomer of this step arrives while nobody else is at it, so the next visitor gets to lay it too (fix round 1:
 *       no timer any more, see {@link #AANKOMST});</li>
 *   <li>the court with the Pai Sho board (zone {@code lotus_hof});</li>
 *   <li>the five trials ({@link Proeven}), a tile each;</li>
 *   <li>the old members: the founder and three members tell the history of the order; with all five tiles the founder
 *       gives the White Lotus tile;</li>
 *   <li>the six tiles on the pedestals of the sanctuary (each shows its tile, for that player only);</li>
 *   <li>the Oude Reigerguh wakes ({@link GeestScenes#REIGER}); the Lotusarchief opens (zone {@code lotusarchief});</li>
 * </ol>
 * then the archive: a book of lore. The tiles are keepsakes: they are never used up, and a lost tile costs nothing (the
 * story remembers what you earned).
 */
public final class WitteLotus {
    /** The founder and the three members (npc markers). */
    public static final List<String> LEDEN = List.of("lid_1", "lid_2", "lid_3");
    public static final String STICHTER = "lid_stichter";
    private static final String SLEUTEL_LID = "einde2_geest_spel_lid";

    /** The players who were told the gate is closed a moment ago (no spam). */
    private static final Set<UUID> GEZEGD = ConcurrentHashMap.newKeySet();

    private WitteLotus() {
    }

    static void register() {
        Lotuspuzzel.OPGELOST.add(WitteLotus::opgelost);
        NpcRollen.zet(GuhNpcEntity.Kind.LOTUS_STICHTER_ECHO, Geestenwereld.NAAM + "/" + STICHTER, new LidRol(STICHTER));
        for (String lid : LEDEN) {
            NpcRollen.zet(GuhNpcEntity.Kind.LOTUS_LID_ECHO, Geestenwereld.NAAM + "/" + lid, new LidRol(lid));
        }
        Praat.luister(SLEUTEL_LID, (p, spreker, optie) -> {
        });
    }

    private static Verhaallijn lijn() {
        return GeestLijnen.witteLotus();
    }

    private static boolean magLotus(ServerPlayer p) {
        return GeestToegang.zoneOpen(p, "lotus_ruines");
    }

    // =====================================================================================================================
    // the gate
    // =====================================================================================================================

    /** The puzzle was solved by p: the gate opens for p and everyone of step 0 who stands by. */
    private static void opgelost(ServerPlayer p, ServerLevel level) {
        for (ServerPlayer q : level.players()) {
            if ((q == p || q.distanceToSqr(p) <= 16 * 16) && magLotus(q) && lijn().stap(q) == GeestLijnen.L_POORT) {
                lijn().zet(q, GeestLijnen.L_HOF);
                BIJ_PUZZEL.remove(q.getUUID());
                GETELD.remove(q.getUUID());
                GeestSpel.titel(q, "gui.guhs.einde2_geest_spel.lotus.poort_titel", "gui.guhs.einde2_geest_spel.lotus.poort_sub");
                GeestSpel.verteller(q, "gui.guhs.einde2_geest_spel.lotus.poort_open");
                GeestSpel.geluid(q, "stinger.opgelost", 0.9f, 1.0f);
                GeestSpel.geluid(q, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.2f);
            }
        }
    }

    /** Is the puzzle lying right in the world now? */
    static boolean puzzelGoed(ServerLevel level) {
        return Lotuspuzzel.opgelost(n -> {
            BlockPos at = Geestenwereld.wereld("tegel_" + n);
            return at == null ? null : level.getBlockState(at);
        });
    }

    /**
     * fix round 1 (fix2-geestzij): the shared puzzle is forgiving. It never turns back on a timer any more (the old rule
     * turned it back half a minute after nobody stood near, so a player who walked off to read a stone could find it
     * scrambled). Instead it turns back only when a player of step 0 ARRIVES at it (comes within {@value #AANKOMST} blocks)
     * while it lies solved and nobody else stands at it ({@value #ANDER} blocks), with a sound and a line, so the newcomer
     * sees it happen and lays the lotus themselves. While a step-0 player stands near, the tiles that lie right sparkle for
     * them and the count shows above the hotbar; the doorway still lets them pass whenever the lotus lies right.
     */
    static final int AANKOMST = 20, ANDER = 12, BIJ = 14;
    /** The step-0 players who are at the puzzle now (to see who arrives), and the count of right tiles each saw last. */
    private static final Set<UUID> BIJ_PUZZEL = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> GETELD = new ConcurrentHashMap<>();

    /** How many of the eight tiles that have a right turn lie right (the round middle one does not count). */
    static int goedeTegels(ServerLevel level) {
        int n = 0;
        for (int i = 1; i <= 9; i++) {
            BlockPos at = Geestenwereld.wereld("tegel_" + i);
            int goed = Lotuspuzzel.GOED[i - 1];
            if (at == null || goed < 0) {
                continue;
            }
            BlockState s = level.getBlockState(at);
            if (s.hasProperty(GeestBlokken.Lotuspuzzeltegel.DRAAI) && s.getValue(GeestBlokken.Lotuspuzzeltegel.DRAAI) == goed) {
                n++;
            }
        }
        return n;
    }

    /** Turns the tiles back (each a quarter or more past its right turn), with a rumble and a puff over the puzzle. */
    static void draaiTerug(ServerLevel level) {
        for (int n = 1; n <= 9; n++) {
            BlockPos at = Geestenwereld.wereld("tegel_" + n);
            int goed = Lotuspuzzel.GOED[n - 1];
            if (at == null || goed < 0) {
                continue;
            }
            BlockState s = level.getBlockState(at);
            if (s.hasProperty(GeestBlokken.Lotuspuzzeltegel.DRAAI)) {
                level.setBlock(at, s.setValue(GeestBlokken.Lotuspuzzeltegel.DRAAI, (goed + 1 + (n % 3)) % 4), Block.UPDATE_ALL);
            }
        }
        BlockPos midden = Geestenwereld.wereld("tegel_5");
        if (midden != null) {
            level.sendParticles(ParticleTypes.CLOUD, midden.getX() + 0.5, midden.getY() + 1.0, midden.getZ() + 0.5, 30, 1.4, 0.2, 1.4, 0.02);
            level.playSound(null, midden, SoundEvents.GRINDSTONE_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.8f, 0.7f);
        }
    }

    /** Every 10 ticks for a player of step 0: arriving at the puzzle, the sparkle of the right tiles, the count. */
    private static void bijPuzzel(ServerPlayer p) {
        ServerLevel level = p.level();
        Vec3 midden = GeestPlek.midden("tegel_5");
        if (midden == null) {
            return;
        }
        double d = p.position().distanceTo(midden);
        UUID u = p.getUUID();
        if (d > AANKOMST + 8) {
            BIJ_PUZZEL.remove(u);
            GETELD.remove(u);
            return;
        }
        if (d <= AANKOMST && BIJ_PUZZEL.add(u) && puzzelGoed(level)) {
            boolean ander = false;
            for (ServerPlayer q : level.players()) {
                if (q != p && q.position().closerThan(midden, ANDER)) {
                    ander = true;
                    break;
                }
            }
            if (!ander) {
                draaiTerug(level);
                GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lotus.tegels_om");
            }
        }
        if (d > BIJ) {
            return;
        }
        for (int i = 1; i <= 9; i++) {
            BlockPos at = Geestenwereld.wereld("tegel_" + i);
            int goed = Lotuspuzzel.GOED[i - 1];
            if (at == null || goed < 0) {
                continue;
            }
            BlockState s = level.getBlockState(at);
            if (s.hasProperty(GeestBlokken.Lotuspuzzeltegel.DRAAI) && s.getValue(GeestBlokken.Lotuspuzzeltegel.DRAAI) == goed) {
                level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, false, false, at.getX() + 0.5, at.getY() + 1.15, at.getZ() + 0.5, 2, 0.25, 0.05, 0.25, 0.0);
            }
        }
        int n = goedeTegels(level);
        Integer was = GETELD.put(u, n);
        if (was == null || was != n) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lotus.puzzel_goed", n, 8);
        }
    }

    // =====================================================================================================================
    // the tick of a player in the Geestenwereld
    // =====================================================================================================================

    static void tick(ServerPlayer p) {
        if (!magLotus(p)) {
            return;
        }
        int stap = lijn().stap(p);
        if (stap == GeestLijnen.L_POORT) {
            if (lijn().begin(p)) {
                GeestHints.lotusBegonnen(p);
            }
            bijPuzzel(p);
            if (GeestPlek.in(p, "poort_doorgang")) {
                if (puzzelGoed(p.level())) {
                    // the lotus lies right (somebody just laid it): whoever stands at the gate now may pass too
                    opgelost(p, p.level());
                } else {
                    duwTerug(p);
                }
            }
        } else if (stap == GeestLijnen.L_HOF && (GeestPlek.in(p, "lotus_hof") || GeestPlek.doos("lotus_hof") == null)) {
            lijn().zet(p, GeestLijnen.L_TEGELS);
            GuhAdvancements.grant(p, "einde2_geest_spel_lotus_hof");
            GeestSpel.geluid(p, "deuntje.pai_sho", 0.8f, 1.0f);
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lotus.hof");
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lotus.hof2");
        } else if (stap == GeestLijnen.L_TEGELS && GeestLijnen.meesterTegels(p) == GeestLijnen.MEESTERS.size()) {
            lijn().zet(p, GeestLijnen.L_LEDEN);
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lotus.vijf");
        } else if (stap == GeestLijnen.L_ARCHIEF && (GeestPlek.in(p, GeestToegang.ARCHIEF) || GeestPlek.bij(p, "archief_ingang", 2.5)
                || GeestPlek.doos(GeestToegang.ARCHIEF) == null && GeestPlek.pos("archief_ingang") == null)) {
            archief(p);
        }
        if (stap >= GeestLijnen.L_VOETSTUKKEN) {
            toonTegels(p);
        }
        // the tile spots a trial is still missing: a soft sparkle (only this player)
        if (stap == GeestLijnen.L_TEGELS || stap == GeestLijnen.L_VOETSTUKKEN) {
            if (p.tickCount % 40 < 10) {
                for (int i = 1; i <= 6; i++) {
                    String m = GeestLijnen.TEGELS.get(i - 1);
                    Vec3 v = GeestPlek.midden("tegel_voetstuk_" + i);
                    if (v != null && GeestLijnen.heeftTegel(p, m) && !lijn().vlag(p, "voetstuk_" + m) && p.position().closerThan(v, 24)) {
                        p.level().sendParticles(p, ParticleTypes.END_ROD, false, false, v.x, v.y + 1.3, v.z, 3, 0.15, 0.2, 0.15, 0.01);
                    }
                }
            }
        }
    }

    private static void duwTerug(ServerPlayer p) {
        BlockPos poort = GeestPlek.pos("poort");
        Vec3 terug = GeestPlek.midden("plek_lotus_ruines");
        if (terug == null) {
            terug = poort == null ? Geestenwereld.aankomst(p.level(), p.getX(), p.getZ()) : Vec3.atBottomCenterOf(poort).add(0, 0, 4);
        }
        p.teleportTo(p.level(), terug.x, terug.y, terug.z, Set.of(), p.getYRot() + 180f, 0f, true);
        p.setDeltaMovement(Vec3.ZERO);
        if (GEZEGD.add(p.getUUID())) {
            GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lotus.poort_dicht");
            p.level().getServer().execute(() -> GEZEGD.remove(p.getUUID()));
        }
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lotus.poort_balk");
    }

    // =====================================================================================================================
    // clicks: the pedestals and the archive door
    // =====================================================================================================================

    static boolean klik(ServerPlayer p, String prop) {
        if (!prop.startsWith("tegel_voetstuk_") && !prop.equals("archief_ingang") && !prop.equals("stenen_lotus")) {
            return false;
        }
        if (!magLotus(p)) {
            return false;
        }
        int stap = lijn().stap(p);
        if (prop.equals("archief_ingang")) {
            if (stap == GeestLijnen.L_ARCHIEF) {
                archief(p);
            } else if (stap < GeestLijnen.L_ARCHIEF) {
                GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lotus.archief_dicht");
            } else {
                GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lotus.archief_weer");
            }
            return true;
        }
        if (prop.equals("stenen_lotus")) {
            GeestSpel.verteller(p, stap > GeestLijnen.L_VOETSTUKKEN ? "gui.guhs.einde2_geest_spel.lotus.lotus_open"
                    : "gui.guhs.einde2_geest_spel.lotus.lotus_dicht");
            return true;
        }
        int n;
        try {
            n = Integer.parseInt(prop.substring("tegel_voetstuk_".length()));
        } catch (NumberFormatException e) {
            return false;
        }
        if (n < 1 || n > 6) {
            return false;
        }
        String m = GeestLijnen.TEGELS.get(n - 1);
        if (lijn().vlag(p, "voetstuk_" + m)) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lotus.ligt_al", Component.translatable("item.guhs.pai_sho_tegel_" + m));
            return true;
        }
        if (stap < GeestLijnen.L_VOETSTUKKEN) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lotus.voetstuk_wacht", Component.translatable("item.guhs.pai_sho_tegel_" + m));
            return true;
        }
        if (!GeestLijnen.heeftTegel(p, m)) {
            GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lotus.voetstuk_wacht", Component.translatable("item.guhs.pai_sho_tegel_" + m));
            return true;
        }
        lijn().vlag(p, "voetstuk_" + m, true);
        GeestSpel.geluid(p, "stinger.tegel", 0.9f, 0.8f + n * 0.08f);
        Vec3 v = GeestPlek.midden(prop);
        if (v != null) {
            p.level().sendParticles(p, ParticleTypes.END_ROD, false, false, v.x, v.y + 1.2, v.z, 20, 0.2, 0.3, 0.2, 0.03);
        }
        int liggen = 0;
        for (String t : GeestLijnen.TEGELS) {
            if (lijn().vlag(p, "voetstuk_" + t)) {
                liggen++;
            }
        }
        GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lotus.gelegd", liggen, GeestLijnen.TEGELS.size());
        nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
        if (liggen == GeestLijnen.TEGELS.size()) {
            ontwaak(p);
        }
        return true;
    }

    /** All six tiles lie: the stone lotus opens and the Oude Reigerguh wakes (the big scene), then the archive opens. */
    static void ontwaak(ServerPlayer p) {
        GeestScenes.speel(p, GeestScenes.REIGER, speler -> {
            if (lijn().stap(speler) == GeestLijnen.L_VOETSTUKKEN) {
                lijn().zet(speler, GeestLijnen.L_ARCHIEF);
                GeestSpel.verteller(speler, "gui.guhs.einde2_geest_spel.lotus.archief_open");
            }
        });
    }

    /** The archive: the book of lore, the line done. */
    static void archief(ServerPlayer p) {
        if (lijn().stap(p) != GeestLijnen.L_ARCHIEF) {
            return;
        }
        lijn().zet(p, GeestLijnen.L_KLAAR);
        GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lotus.archief");
        p.getInventory().add(boek());
        GeestSpel.geef(p, nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get(), 16);
        GeestSpel.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
    }

    /** "Uit het Lotusarchief": a written book whose pages are lang keys (every reader reads it in their own language). */
    public static ItemStack boek() {
        ItemStack boek = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> paginas = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            paginas.add(Filterable.passThrough(Component.translatable("gui.guhs.einde2_geest_spel.boek.pagina." + i)));
        }
        boek.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Uit het Lotusarchief"), "De Oude Reiger", 0, paginas, true));
        boek.set(DataComponents.CUSTOM_NAME, Component.translatable("gui.guhs.einde2_geest_spel.boek.titel"));
        return boek;
    }

    /** The tiles this player laid, on their pedestals (an item display only they see), while they are near. */
    private static void toonTegels(ServerPlayer p) {
        for (int i = 1; i <= 6; i++) {
            String m = GeestLijnen.TEGELS.get(i - 1);
            String naam = "voetstuk_" + m;
            Vec3 v = GeestPlek.midden("tegel_voetstuk_" + i);
            boolean wil = v != null && lijn().vlag(p, naam) && p.position().closerThan(v, 40);
            if (wil && !GeestActeurs.heeft(p, naam)) {
                GeestActeurs.zorg(p, naam, "minecraft:item_display", v.add(0, 1.25, 0), 0f, t -> {
                    net.minecraft.nbt.CompoundTag item = new net.minecraft.nbt.CompoundTag();
                    item.putString("id", "guhs:pai_sho_tegel_" + m);
                    item.putInt("count", 1);
                    t.put("item", item);
                    t.putString("billboard", "vertical");
                });
            } else if (!wil && GeestActeurs.heeft(p, naam)) {
                GeestActeurs.weg(p, naam, false);
            }
        }
    }

    // =====================================================================================================================
    // the old members (spot roles of the echoes)
    // =====================================================================================================================

    /** The founder or a member: tells their part of the history; the founder gives the White Lotus tile. */
    private record LidRol(String marker) implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            if (!magLotus(p)) {
                GeestSpel.zeg(p, npc, "", "gui.guhs.einde2_geest_spel.lid.stil");
                return;
            }
            Verhaallijn l = lijn();
            String naam = marker.equals(STICHTER) ? "stichter" : marker;
            boolean eerder = l.vlag(p, "lid_" + naam);
            l.vlag(p, "lid_" + naam, true);
            if (!marker.equals(STICHTER)) {
                GeestSpel.praat(p, SLEUTEL_LID, List.of(
                        GeestSpel.regel(npc, "", "gui.guhs.einde2_geest_spel.lid." + naam + ".1"),
                        GeestSpel.regel(npc, "", "gui.guhs.einde2_geest_spel.lid." + naam + ".2"),
                        GeestSpel.regel(npc, "", "gui.guhs.einde2_geest_spel.lid." + naam + ".3")),
                        new Praat.Optie(1, "gui.guhs.einde2_geest_spel.optie.dank"));
                if (!eerder) {
                    GeestSpel.balk(p, "gui.guhs.einde2_geest_spel.lid.gehoord", leden(p), LEDEN.size());
                }
                return;
            }
            // the founder
            int stap = l.stap(p);
            if (GeestLijnen.heeftTegel(p, "witte_lotus")) {
                GeestSpel.praat(p, SLEUTEL_LID, List.of(GeestSpel.regel(npc, "", "gui.guhs.einde2_geest_spel.lid.stichter.na")),
                        new Praat.Optie(1, "gui.guhs.einde2_geest_spel.optie.dank"));
                return;
            }
            if (stap >= GeestLijnen.L_LEDEN && leden(p) == LEDEN.size()) {
                GeestSpel.praat(p, SLEUTEL_LID, List.of(
                        GeestSpel.regel(npc, "", "gui.guhs.einde2_geest_spel.lid.stichter.1"),
                        GeestSpel.regel(npc, "", "gui.guhs.einde2_geest_spel.lid.stichter.2"),
                        GeestSpel.regel(npc, "", "gui.guhs.einde2_geest_spel.lid.stichter.3"),
                        GeestSpel.regel(npc, "", "gui.guhs.einde2_geest_spel.lid.stichter.tegel")),
                        new Praat.Optie(1, "gui.guhs.einde2_geest_spel.optie.dank"));
                l.vlag(p, "tegel_witte_lotus", true);
                GeestSpel.geef(p, E2GeestSpelFeature.tegel("witte_lotus"), 1);
                GeestSpel.geluid(p, "stinger.tegel", 1.0f, 1.0f);
                if (l.stap(p) == GeestLijnen.L_LEDEN) {
                    l.zet(p, GeestLijnen.L_VOETSTUKKEN);
                }
                GeestSpel.verteller(p, "gui.guhs.einde2_geest_spel.lotus.naar_voetstukken");
                return;
            }
            String key = stap < GeestLijnen.L_LEDEN ? "gui.guhs.einde2_geest_spel.lid.stichter.eerst_tegels" : "gui.guhs.einde2_geest_spel.lid.stichter.eerst_leden";
            GeestSpel.praat(p, SLEUTEL_LID, List.of(GeestSpel.regel(npc, "", "gui.guhs.einde2_geest_spel.lid.stichter.hallo"),
                    GeestSpel.regel(npc, "", key, leden(p), LEDEN.size())), new Praat.Optie(1, "gui.guhs.einde2_geest_spel.optie.dank"));
        }
    }

    /** How many of the three members this player listened to. */
    static int leden(ServerPlayer p) {
        int n = 0;
        for (String lid : LEDEN) {
            if (lijn().vlag(p, "lid_" + lid)) {
                n++;
            }
        }
        return n;
    }

    @Nullable
    static Entity echo(ServerPlayer p, String meester) {
        return GeestPlek.bewoner(p.level(), "echo_" + meester);
    }
}
