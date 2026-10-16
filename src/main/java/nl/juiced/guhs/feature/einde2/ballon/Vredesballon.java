package nl.juiced.guhs.feature.einde2.ballon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.einde2.vetbootje.Heteluchtballon;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verhaallijnen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase C, slice trap5_ballon (CONTRACT_E2_B 7.12, DESIGN 6): the Vredesballon, the big project of tech tier 5.
 * <p>
 * Questline {@link #LIJN} ("vredesballon", group techniek, after "werktuigguh" of trap5_machines; all progress per player in
 * {@code GuhQuests.saved}):
 * <ol start="0">
 *   <li>not begun: the board on the building site (a click, {@link BallonBord}) starts it once the Werktuigguh's own line is
 *       done ({@link #magBeginnen});</li>
 *   <li>.. 5: the five stages of {@link #FASEN} (factory output, handed in at the board in portions; counters of the line).
 *       The player's own balloon grows on the building site ({@link BallonLagen}: overlay {@code laag_vredesballon_<n>});</li>
 *   <li value="6">ready: the board asks "Lanceren?"; yes = the scene ({@link BallonScene}, {@link #lanceer});</li>
 *   <li value="7">done (the scene was watched to its end): the statuette, the player's own heteluchtballon (vetbootje's
 *       {@link Heteluchtballon#geef}), the title Vredesbrenger; after that a little bag of knabbels a day ({@link #zakje}).</li>
 * </ol>
 * The Werktuigguh talks through the talking screen ({@link Praat}) and chat; his own NPC (trap5_machines' spot) is only
 * looked up as the face of the screen when he stands near.
 */
public final class Vredesballon {
    public static final String ID = "vredesballon";
    /** The line that comes first (CONTRACT_E2_B 2.1: trap5_machines' line). */
    public static final String VORIGE = "werktuigguh";
    public static final String STRUCTUUR = Einde2Ids.ZUIDELIJKE_LUCHTTEMPEL;
    /** The markers of the building site (bouw_lucht's; CONTRACT_E2 6.7). */
    public static final String ANKER = "vredesballon_anker", TELLER = "vredesballon_teller", BOUWPLAATS = "vredesballon_bouwplaats";
    public static final int AANTAL_FASEN = 5, KLAAR = 6, STAPPEN = 7;
    public static final String TITEL = "vredesbrenger";
    /** Per-player data (CONTRACT_E2_B 2.1: not a step): the game day of the last bag of knabbels. */
    public static final String DAG = "guhs_einde2_trap5_ballon_dag";
    /** (dev run only) the gate is open although the Werktuigguh's line is not registered (a tree without trap5_machines). */
    public static final String DEV_VRIJ = "guhs_einde2_trap5_ballon_dev_vrij";
    /** Hidden advancements (quest/...) and the visible one (guheinde/...). */
    public static final String ADV_BEELDJE = "einde2_trap5_ballon_beeldje", ADV_ZAKJE = "einde2_trap5_ballon_dagelijks",
            ADV_ZICHTBAAR = "guheinde/einde2_trap5_ballon_vredesbrenger";
    static final String T = "gui.guhs.einde2_trap5_ballon.";
    /** The answers of the talking screen come back by these keys. */
    static final String PRAAT_LANCEER = "einde2_trap5_ballon_lanceer", PRAAT_KWIJT = "einde2_trap5_ballon_kwijt", PRAAT_PLAN = "einde2_trap5_ballon_plan";
    /** How many knabbels the bag of the day holds (modest loot). */
    public static final int ZAKJE = 12, ZAKJE_GEFRITUURD = 2;

    // =====================================================================================================================
    // the stages
    // =====================================================================================================================

    /** One thing a stage asks for: its counter name (and text key), the Guhdex icon, how many, what counts. */
    public record Levering(String naam, String icoon, int aantal, Predicate<ItemStack> wat, @Nullable Identifier moetBestaan) {
        public String tekst() {
            return T + "levering." + naam;
        }

        /** False for a thing of another slice that is not in this build (before the merge): the stage leaves it out. */
        public boolean bestaat() {
            return moetBestaan == null || BuiltInRegistries.ITEM.containsKey(moetBestaan);
        }
    }

    /** One item by id (looked up when asked: items of trap5_machines need no compile-time link). */
    private static Levering levering(String naam, String id, int aantal) {
        Identifier rl = Identifier.parse(id);
        return new Levering(naam, id, aantal, s -> BuiltInRegistries.ITEM.getKey(s.getItem()).equals(rl), rl);
    }

    private static boolean isTentdoek(ItemStack s) {
        Identifier id = BuiltInRegistries.ITEM.getKey(s.getItem());
        String p = id.getPath();
        return id.getNamespace().equals("guhs") && p.startsWith("campingmarkt_tentdoek_") && !p.endsWith("_trap") && !p.endsWith("_plaat");
    }

    /**
     * The five stages (steps 1..5): factory output of tiers 1-5 (tools/features/einde2_trap5_ballon.py FASEN says the same and
     * checks it). The burner runs on vadskracht: two Vuurkorven and a Vadsontvanger of trap5_machines.
     */
    public static final List<List<Levering>> FASEN = List.of(
            // the cloth: tent canvas sewn by a Knutselmachine, and Guhdraad for the seams
            List.of(new Levering("tentdoek", "guhs:campingmarkt_tentdoek_geel", 128, Vredesballon::isTentdoek, null),
                    levering("draad", "guhs:guh_wire", 64)),
            // the basket: the forest in a tub, and doosschil for a floor that never breaks
            List.of(new Levering("hout", "minecraft:oak_log", 256, s -> s.is(ItemTags.LOGS), null),
                    levering("doosschil", "guhs:doosschil", 16)),
            // the burner on vadskracht
            List.of(levering("vuurkorf", "guhs:vuurkorf", 2), levering("ontvanger", "guhs:vadsontvanger", 1),
                    levering("grillkool", "guhs:grillkool", 96), levering("kristalkaas", "guhs:kristalkaas", 16)),
            // the stock of knabbels to scatter
            List.of(levering("knabbels", "guhs:kaas_knabbels", 512), levering("frituur", "guhs:gefrituurde_kaasknabbels", 128)),
            // the decoration
            List.of(levering("lichtstaafjes", "guhs:lichtstaafje", 64), levering("flitskruimel", "guhs:flitskruimel", 32)));

    // =====================================================================================================================
    // the line
    // =====================================================================================================================

    public static final Verhaallijn LIJN = Verhaallijn.maak(ID, "techniek").stappen(STAPPEN).icoon("guhs:vredesballon").na(VORIGE).doelregel(true)
            .nodig(Vredesballon::nodig)
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:vredesballon", Vredesballon.LIJN.klaar(p)),
                    Verhaallijn.beloning("guhs:heteluchtballon", Vredesballon.LIJN.klaar(p))))
            .doel((p, stap) -> Doel.structuur(GuheindeFeature.GUHEINDE, STRUCTUUR, Component.translatable("structure.guhs." + STRUCTUUR)))
            .registreer();

    /** Players who are in the air right now (the launch scene): their balloon is not on the building site. */
    static final Set<UUID> OPGESTEGEN = ConcurrentHashMap.newKeySet();

    private Vredesballon() {
    }

    /** May this player begin: the Werktuigguh's line done (or, when nobody registered it, the whole Guhvatar story). */
    public static boolean magBeginnen(ServerPlayer p) {
        Verhaallijn vorige = Verhaallijnen.van(VORIGE);
        if (vorige != null) {
            return vorige.klaar(p);
        }
        return E2Verhaal.finaleKlaar(p) || devVrij(p);
    }

    static boolean devVrij(ServerPlayer p) {
        return !net.neoforged.fml.loading.FMLEnvironment.isProduction() && GuhQuests.saved(p).getBooleanOr(DEV_VRIJ, false);
    }

    /** The stage this player is handing in for (1..5), or 0. */
    public static int fase(ServerPlayer p) {
        int stap = LIJN.stap(p);
        return stap >= 1 && stap <= AANTAL_FASEN ? stap : 0;
    }

    /** Which overlay of the building site this player sees: 0 (none) .. 5 (the whole balloon). */
    public static int laag(ServerPlayer p) {
        int stap = LIJN.stap(p);
        return stap <= 1 ? 0 : Math.min(AANTAL_FASEN, stap - 1);
    }

    /** What stage {@code fase} (1..5) asks of this build (things of a slice that is not here are left out). */
    public static List<Levering> leveringen(int fase) {
        return FASEN.get(fase - 1).stream().filter(Levering::bestaat).toList();
    }

    private static String teller(int fase, Levering l) {
        return "f" + fase + "_" + l.naam();
    }

    public static int geleverd(ServerPlayer p, int fase, Levering l) {
        return Math.min(l.aantal(), LIJN.teller(p, teller(fase, l)));
    }

    /** What handing in did: how many things were taken, and whether the stage is complete now. */
    public record Uitkomst(int genomen, boolean faseKlaar) {
    }

    /** Takes what the player carries that their stage still needs; a complete stage moves the line one step on. */
    public static Uitkomst lever(ServerPlayer p) {
        int fase = fase(p);
        if (fase == 0) {
            return new Uitkomst(0, false);
        }
        int genomen = 0;
        boolean compleet = true;
        Inventory inv = p.getInventory();
        for (Levering l : leveringen(fase)) {
            int heeft = geleverd(p, fase, l);
            for (int i = 0; i < inv.getContainerSize() && heeft < l.aantal(); i++) {
                ItemStack stack = inv.getItem(i);
                if (!stack.isEmpty() && l.wat().test(stack)) {
                    int n = Math.min(stack.getCount(), l.aantal() - heeft);
                    stack.shrink(n);
                    heeft += n;
                    genomen += n;
                }
            }
            LIJN.teller(p, teller(fase, l), heeft);
            compleet &= heeft >= l.aantal();
        }
        if (genomen > 0) {
            inv.setChanged();
        }
        return new Uitkomst(genomen, compleet && LIJN.verder(p, fase));
    }

    /** What the Guhdex shows as "needed" for a step. */
    static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        if (stap < 1 || stap > AANTAL_FASEN) {
            return List.of();
        }
        List<VerhaalStand.Nodig> out = new ArrayList<>();
        for (Levering l : leveringen(stap)) {
            out.add(Verhaallijn.nodig(l.icoon(), l.tekst(), geleverd(p, stap, l), l.aantal()));
        }
        return out;
    }

    // =====================================================================================================================
    // the board: what a click does
    // =====================================================================================================================

    /** The player clicked the progress board of the building site. */
    static void klik(ServerPlayer p) {
        if (Cutscenes.bezig(p) || OPGESTEGEN.contains(p.getUUID())) {
            return;
        }
        int stap = LIJN.stap(p);
        if (stap == 0) {
            if (!magBeginnen(p)) {
                zeg(p, "wg.dicht");
                return;
            }
            Praat.scene(p, PRAAT_PLAN, List.of(regel(p, "wg.plan1"), regel(p, "wg.plan2"), regel(p, "wg.plan3"), regel(p, "wg.plan4")));
            if (LIJN.verder(p, 0)) {
                GuhQuests.hint(p, T + "hint.bord");
                vraag(p, 1);
            }
        } else if (stap <= AANTAL_FASEN) {
            Uitkomst uit = lever(p);
            if (uit.faseKlaar()) {
                zeg(p, "wg.fase" + stap);
                groei(p);
                if (LIJN.stap(p) == KLAAR) {
                    vraagLancering(p);
                } else {
                    vraag(p, stap + 1);
                }
            } else {
                if (uit.genomen() > 0) {
                    zeg(p, "wg.dank", uit.genomen());
                    Schijn.geluid(p, SoundEvents.WOOL_PLACE, p.blockPosition(), 0.8f, 1.2f);
                } else if (p.getRandom().nextInt(3) == 0) {
                    zeg(p, "wg.tip" + p.getRandom().nextInt(3));
                }
                vraag(p, stap);
            }
        } else if (stap == KLAAR) {
            vraagLancering(p);
        } else if (p.isShiftKeyDown()) {
            Praat.scene(p, PRAAT_KWIJT, List.of(regel(p, "wg.kwijt")), new Praat.Optie(1, T + "wg.kwijt_ja"), new Praat.Optie(0, T + "wg.kwijt_nee"));
        } else {
            zakje(p);
        }
        BallonBord.ververs(p);
    }

    /** The answers of the talking screen (registered once: {@link E2BallonFeature}). */
    static void luister() {
        Praat.luister(PRAAT_LANCEER, (p, spreker, optie) -> {
            if (optie == 1) {
                lanceer(p);
            } else if (optie == 0) {
                zeg(p, "wg.wachten");
            }
        });
        Praat.luister(PRAAT_KWIJT, (p, spreker, optie) -> {
            if (optie != 1) {
                return;
            }
            if (Heteluchtballon.opnieuw(p)) {
                zeg(p, "wg.kwijt_nieuw");
            } else {
                zeg(p, "wg.kwijt_nooit");
            }
        });
        Praat.luister(PRAAT_PLAN, (p, spreker, optie) -> {
        });
    }

    private static void vraagLancering(ServerPlayer p) {
        Praat.scene(p, PRAAT_LANCEER, List.of(regel(p, "wg.klaar")), new Praat.Optie(1, T + "wg.lanceren"), new Praat.Optie(0, T + "wg.wacht"));
    }

    /** "Fase n van 5, the doek. I still need:" and one line per thing. */
    static void vraag(ServerPlayer p, int fase) {
        if (fase < 1 || fase > AANTAL_FASEN) {
            return;
        }
        zeg(p, "wg.vraag", fase, Component.translatable(T + "fase." + fase));
        for (Levering l : leveringen(fase)) {
            int heeft = geleverd(p, fase, l);
            boolean binnen = heeft >= l.aantal();
            p.sendSystemMessage(Component.literal("  ").append(Component.translatable(T + "bord.regel", Component.translatable(l.tekst()), heeft, l.aantal()))
                    .withStyle(binnen ? ChatFormatting.GREEN : ChatFormatting.GOLD));
        }
    }

    /** The balloon on the building site grows at once (the next stage of the overlay), with a puff and a sound. */
    private static void groei(ServerPlayer p) {
        Schijn.ververs(p);
        BlockPos anker = plek(p.level(), ANKER);
        if (anker != null) {
            Vec3 midden = Vec3.atBottomCenterOf(anker).add(12, 8, 12);
            Schijn.deeltjes(p, ParticleTypes.CLOUD, midden, 40, 4.0, 0.02);
            Schijn.deeltjes(p, ParticleTypes.HAPPY_VILLAGER, midden, 30, 5.0, 0.02);
            Schijn.geluid(p, SoundEvents.ANVIL_USE, anker, 0.7f, 1.1f);
            Schijn.geluid(p, SoundEvents.PLAYER_LEVELUP, anker, 0.6f, 1.4f);
        }
        p.sendOverlayMessage(Component.translatable(T + "groeit").withStyle(ChatFormatting.GOLD));
    }

    // =====================================================================================================================
    // the launch
    // =====================================================================================================================

    /** Step 6: the scene; when it was watched to its end the balloon is back home and the player is rewarded. */
    public static boolean lanceer(ServerPlayer p) {
        if (LIJN.stap(p) != KLAAR || Cutscenes.bezig(p) || !GuheindeFeature.isGuheinde(p.level())) {
            return false;
        }
        BlockPos anker = plek(p.level(), ANKER);
        if (anker == null) {
            return false;
        }
        OPGESTEGEN.add(p.getUUID());
        Schijn.ververs(p);
        E2Audio.moment(p, "appa");
        boolean gestart = Cutscenes.speel(p, BallonScene.VREDESBALLON, anker, Rotation.NONE, Vredesballon::geland);
        if (!gestart) {
            OPGESTEGEN.remove(p.getUUID());
            E2Audio.stopMoment(p, 0);
            Schijn.ververs(p);
        }
        return gestart;
    }

    /** The scene is over: the balloon is moored again, the line is done. */
    static void geland(ServerPlayer p) {
        OPGESTEGEN.remove(p.getUUID());
        E2Audio.stopMoment(p, 40);
        if (LIJN.verder(p, KLAAR)) {
            beloon(p);
        }
        Schijn.ververs(p);
        BallonBord.ververs(p);
    }

    /** The rewards of the line (once each: the statuette and the balloon keep their own marks). */
    static void beloon(ServerPlayer p) {
        zeg(p, "wg.af1");
        zeg(p, "wg.af2");
        if (LIJN.eenmalig(p, "beeldje")) {
            Minigames.give(p, new ItemStack(E2BallonFeature.VREDESBALLON_ITEM.get()));
        }
        Heteluchtballon.geef(p);
        GidsFeature.grant(p, ADV_ZICHTBAAR);
        p.sendSystemMessage(Component.translatable(T + "titel", Component.translatable("gui.guhs.titel." + TITEL).withStyle(ChatFormatting.GOLD))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** Called when the player falls out of a scene (logged out): nothing is lost, the balloon stands ready again. */
    static void vergeet(UUID speler) {
        OPGESTEGEN.remove(speler);
    }

    // =====================================================================================================================
    // the bag of knabbels a day
    // =====================================================================================================================

    public static long dag(ServerPlayer p) {
        return p.level().getServer().overworld().getGameTime() / 24000L;
    }

    public static boolean zakjeKlaar(ServerPlayer p) {
        return LIJN.klaar(p) && GuhQuests.saved(p).getLongOr(DAG, -1L) != dag(p);
    }

    /** The player is at the board after the story: today's bag, once a day. */
    static boolean zakje(ServerPlayer p) {
        if (!zakjeKlaar(p)) {
            zeg(p, "wg.morgen");
            return false;
        }
        GuhQuests.saved(p).putLong(DAG, dag(p));
        Item knabbels = BuiltInRegistries.ITEM.getValue(Identifier.parse("guhs:kaas_knabbels"));
        Item gefrituurd = BuiltInRegistries.ITEM.getValue(Identifier.parse("guhs:gefrituurde_kaasknabbels"));
        Minigames.give(p, new ItemStack(knabbels, ZAKJE));
        Minigames.give(p, new ItemStack(gefrituurd, ZAKJE_GEFRITUURD));
        GuhAdvancements.grant(p, ADV_ZAKJE);
        zeg(p, "wg.zakje");
        Schijn.geluid(p, SoundEvents.BUNDLE_DROP_CONTENTS, p.blockPosition(), 0.8f, 1.1f);
        Schijn.deeltjes(p, ParticleTypes.HAPPY_VILLAGER, p.position().add(0, 1.2, 0), 12, 0.5, 0.02);
        return true;
    }

    // =====================================================================================================================
    // small helpers
    // =====================================================================================================================

    /** The world position of a marker of the temple (null: no temple known yet, or not the Guheinde). */
    @Nullable
    static BlockPos plek(ServerLevel level, String id) {
        if (!GuheindeFeature.isGuheinde(level) || !Plekken.bekend(level, STRUCTUUR)) {
            return null;
        }
        return Plekken.wereld(level, STRUCTUUR, id);
    }

    /** The Werktuigguh's NPC when he stands near (the face of the talking screen), else null. */
    @Nullable
    static GuhNpcEntity werktuigguh(ServerPlayer p) {
        List<GuhNpcEntity> bij = p.level().getEntitiesOfClass(GuhNpcEntity.class, p.getBoundingBox().inflate(48),
                n -> n.getKind() == GuhNpcEntity.Kind.WERKTUIGGUH && n.isAlive());
        return bij.isEmpty() ? null : bij.get(0);
    }

    private static Praat.Regel regel(ServerPlayer p, String key) {
        return new Praat.Regel(werktuigguh(p), "entity.guhs.guh_npc.werktuigguh", T + key);
    }

    /** A chat line of the Werktuigguh, only for this player. */
    /**
     * (WerktuigguhRol.NA_KLAAR, integratie_b) talked to after his own line: while this player's balloon is not up yet, the
     * Werktuigguh points to the board himself (where to start, the stage that is next, or the launch); true = he answered.
     * After the launch his own tips come back.
     */
    public static boolean wijsNaarBord(ServerPlayer p) {
        if (LIJN.klaar(p) || !magBeginnen(p)) {
            return false;
        }
        int stap = LIJN.stap(p);
        if (stap <= 0) {
            zeg(p, "wg.wijs_begin");
        } else if (stap >= KLAAR) {
            zeg(p, "wg.wijs_klaar");
        } else {
            zeg(p, "wg.wijs_fase", stap, Component.translatable(T + "fase." + stap));
        }
        return true;
    }

    static void zeg(ServerPlayer p, String key, Object... args) {
        MutableComponent naam = Component.translatable("entity.guhs.guh_npc.werktuigguh");
        p.sendSystemMessage(Component.literal("<").append(naam).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.translatable(T + key, args).withStyle(ChatFormatting.WHITE)));
    }
}
