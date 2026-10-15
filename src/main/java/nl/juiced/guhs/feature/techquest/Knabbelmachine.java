package nl.juiced.guhs.feature.techquest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.guhpolder.GuhpolderFeature;
import nl.juiced.guhs.feature.spiesburcht.GuhdrankjeItem;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (tech-quests): De Grote Knabbelmachine, the last project of Guh-technologie.
 * <p>
 * A building project in five stages at the Uitvinder-guh (questline {@link TechquestFeature#KNABBELMACHINE}, after his
 * practice hall and after the Aangebrande Mika): every stage asks for big loads of what a factory makes
 * ({@link #FASEN}). The player hands in what they carry, as often as they like; the count is the player's own (counters
 * of the Verhaallijn), so any number of players build at the same time and each sees their own machine grow on the bordes
 * (the client draws it for the stage it gets in {@link TechquestPayloads.Stand}).
 * <p>
 * Once it stands it makes ONE perfect knabbel a day, for each player their own ({@link #klik}): the day is kept in the
 * player's saved guh data ({@link #DAG}).
 */
public final class Knabbelmachine {
    /** The game day (server clock) on which this player last took their perfect knabbel. */
    public static final String DAG = "guhs_techquest_knabbel_dag";
    public static final int FASE_KLAAR = 6;

    /** One thing a stage asks for: its name (the counter and the text key), what counts, the icon in the Guhdex, how many. */
    public record Levering(String naam, Predicate<ItemStack> wat, String icoon, int aantal) {
        public String tekst() {
            return "gui.guhs.techquest.levering." + naam;
        }
    }

    /** The five stages (step 1..5 of the questline): what the Uitvinder-guh wants for each. */
    public static final List<List<Levering>> FASEN = List.of(
            // the foundation: the stone line (a Knabbelaar at a cobblestone generator) and the forest in a tub
            List.of(new Levering("steen", s -> s.is(ItemTags.STONE_CRAFTING_MATERIALS), "minecraft:cobblestone", 256),
                    new Levering("hout", s -> s.is(ItemTags.LOGS), "minecraft:oak_log", 128)),
            // the boiler: the grillkool line and the frituur
            List.of(new Levering("grillkool", s -> s.is(BarbecuetherFeature.GRILLKOOL.get().asItem()), "guhs:grillkool", 96),
                    new Levering("frituur", s -> s.is(ModItems.GEFRITUURDE_KAASKNABBELS.get()), "guhs:gefrituurde_kaasknabbels", 128)),
            // the stomach: the field that harvests itself into a Vadsmolen, and the Knutselmachine
            List.of(new Levering("meel", s -> s.is(GuhpolderFeature.KNABBELMEEL.get()), "guhs:knabbelmeel", 128),
                    new Levering("draad", s -> s.is(ModBlocks.GUH_WIRE.get().asItem()), "guhs:guh_wire", 64)),
            // the snoet: drinks that brew themselves, and the oven that never stops
            List.of(new Levering("drankje", s -> s.getItem() instanceof GuhdrankjeItem, "guhs:guhbrouwketel", 12),
                    new Levering("glas", s -> s.is(net.minecraft.world.item.Items.GLASS), "minecraft:glass", 128)),
            // the heart: a gloeister, and the first bite
            List.of(new Levering("gloeister", s -> s.is(SpiesburchtFeature.GLOEISTER.get()), "guhs:gloeister", 1),
                    new Levering("knabbels", s -> s.is(ModItems.KAAS_KNABBELS.get()), "guhs:kaas_knabbels", 256)));

    private Knabbelmachine() {
    }

    private static Verhaallijn lijn() {
        return TechquestFeature.KNABBELMACHINE;
    }

    private static String teller(int fase, Levering l) {
        return "f" + fase + "_" + l.naam();
    }

    /** How many of this the player has handed in for stage {@code fase} (1..5). */
    public static int geleverd(ServerPlayer p, int fase, Levering l) {
        return Math.min(l.aantal(), lijn().teller(p, teller(fase, l)));
    }

    /** The stage this player's machine is at: 0 (nothing, not begun) .. 6 (it stands and gnaws). */
    public static int fase(ServerPlayer p) {
        return lijn().stap(p);
    }

    public static boolean staat(ServerPlayer p) {
        return lijn().klaar(p);
    }

    /** What handing in did. */
    public record Uitkomst(int genomen, boolean faseKlaar, boolean machineKlaar) {
    }

    /**
     * Takes everything the player carries that the stage they are at still needs. When the stage is complete the machine
     * grows (one step on). Nothing happens outside the stages 1..5.
     */
    public static Uitkomst lever(ServerPlayer p) {
        int fase = fase(p);
        if (fase < 1 || fase > FASEN.size()) {
            return new Uitkomst(0, false, false);
        }
        int genomen = 0;
        boolean compleet = true;
        Inventory inv = p.getInventory();
        for (Levering l : FASEN.get(fase - 1)) {
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
            lijn().teller(p, teller(fase, l), heeft);
            compleet &= heeft >= l.aantal();
        }
        if (genomen > 0) {
            inv.setChanged();
        }
        if (!compleet) {
            return new Uitkomst(genomen, false, false);
        }
        lijn().verder(p, fase);
        return new Uitkomst(genomen, true, staat(p));
    }

    /** What the Guhdex shows as "needed" for a step. */
    static List<VerhaalStand.Nodig> nodig(ServerPlayer p, int stap) {
        if (stap < 1 || stap > FASEN.size()) {
            return List.of();
        }
        List<VerhaalStand.Nodig> out = new ArrayList<>();
        for (Levering l : FASEN.get(stap - 1)) {
            out.add(Verhaallijn.nodig(l.icoon(), l.tekst(), geleverd(p, stap, l), l.aantal()));
        }
        return out;
    }

    // --- one perfect knabbel a day ---------------------------------------------------------------------------------------------

    /** The game day (the Barbecuether's own clock stands still: the server's clock counts). */
    public static long dag(ServerPlayer p) {
        return p.level().getServer().overworld().getGameTime() / 24000L;
    }

    /** Does this player's machine have a knabbel ready (it stands, and they did not take today's yet)? */
    public static boolean knabbelKlaar(ServerPlayer p) {
        return staat(p) && GuhQuests.saved(p).getLongOr(DAG, -1L) != dag(p);
    }

    /** The player clicked the bowl of a Grote Knabbelmachine. */
    public static void klik(ServerPlayer p, BlockPos pos) {
        ServerLevel level = p.level();
        int fase = fase(p);
        if (fase < FASE_KLAAR) {
            String key = !TechquestFeature.TECHNIEK.klaar(p) ? "gui.guhs.techquest.machine.eerst" : fase == 0 ? "gui.guhs.techquest.machine.plan"
                    : "gui.guhs.techquest.machine.bezig";
            p.sendOverlayMessage(Component.translatable(key, fase - 1, FASEN.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        if (!knabbelKlaar(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.techquest.machine.morgen").withStyle(ChatFormatting.LIGHT_PURPLE));
            level.playSound(null, pos, ModSounds.GUH_AMBIENT.get(), SoundSource.BLOCKS, 0.7f, 0.6f);
            return;
        }
        GuhQuests.saved(p).putLong(DAG, dag(p));
        Minigames.give(p, new ItemStack(TechquestFeature.PERFECTE_KNABBEL.get()));
        GuhAdvancements.grant(p, "tech_quests_perfecte_knabbel");
        p.sendOverlayMessage(Component.translatable("gui.guhs.techquest.machine.knabbel").withStyle(ChatFormatting.GOLD));
        level.playSound(null, pos, ModSounds.GUH_EAT.get(), SoundSource.BLOCKS, 1f, 0.7f);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5f, 1.6f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0.02);
        sync(p);
    }

    // --- what the client draws ---------------------------------------------------------------------------------------------------

    private static final Map<ServerPlayer, Integer> GESTUURD = new WeakHashMap<>();

    private static int stand(ServerPlayer p) {
        return fase(p) * 2 + (knabbelKlaar(p) ? 1 : 0);
    }

    /** Sends this player's stage (and whether a knabbel lies ready) to their client. */
    public static void sync(ServerPlayer p) {
        CompoundTag data = new CompoundTag();
        data.putInt("Fase", fase(p));
        data.putBoolean("Knabbel", knabbelKlaar(p));
        GESTUURD.put(p, stand(p));
        TechquestPayloads.send(p, new TechquestPayloads.Stand(data));
    }

    /** The same, only when it changed since the last time (a new day began while the player stood there). */
    public static void syncAlsAnders(ServerPlayer p) {
        Integer vorig = GESTUURD.get(p);
        if (vorig == null || vorig != stand(p)) {
            sync(p);
        }
    }
}
