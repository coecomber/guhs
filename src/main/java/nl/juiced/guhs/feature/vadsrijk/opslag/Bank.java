package nl.juiced.guhs.feature.vadsrijk.opslag;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;

/**
 * T-OPSLAG: the bank (VOORSTEL_ROADMAP §6.2, R5): ONE account-wide vault (all your characters share it), reachable at a banker
 * in every hub town. Tab 1 (54 slots) is free; more tabs are bought at the banker (click a locked tab, then again).
 * <p><b>Making an NPC a banker is one line</b> (zone 4/5 agents, Lampionhaven/Hoogvads): {@code Bank.bankier("z4_bankier");}
 * from your registry class. That adds the choice "Naar mijn kluis" to the NPC's talk menu (kept with quests). An NPC whose
 * {@code NpcDef.rol} is {@code "bank"} works too (bound here through {@code NpcRollen.koppel("bank", Bank::open)}).
 * Current bankers: see {@link #STANDAARD}.
 */
public final class Bank {
    public static final int VAKKEN = 54, MAX_TABS = 5;
    /** Price of tab 2, 3, 4, 5 (brons): 10 zilver, 50 zilver, 2 goud, 5 goud. */
    public static long[] PRIJZEN = {1000, 5000, 20000, 50000};
    /** How far you may walk from where you opened the vault before it closes. */
    public static final double BEREIK = 8;
    /**
     * The hub bankers (npc ids): Zwamzoet's Bankier, the Knuffelkamp cook (his pantry), De Haven's havenmeester, Kandijkom's
     * veilingmeester (in the bank building), Honingtak's juwelier.
     */
    public static final List<String> STANDAARD = List.of("bankier", "kok", "havenmeester", "z2_veilingmeester", "z3_juwelier");
    private static final Set<String> BANKIERS = ConcurrentHashMap.newKeySet();

    private Bank() {
    }

    /** Makes this NPC a banker: its talk menu gets "Naar mijn kluis". Safe to call twice. */
    public static void bankier(String npcId) {
        if (!BANKIERS.add(npcId)) {
            return;
        }
        QuestMotor.EXTRA.computeIfAbsent(npcId, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(new QuestMotor.ExtraOptie("vadsrijk.opslag.optie.bank", p -> true, Bank::open));
    }

    public static boolean isBankier(String npcId) {
        return BANKIERS.contains(npcId);
    }

    static void init() {
        STANDAARD.forEach(Bank::bankier);
        bankier("z5_gouddraad");   // T-INT: Hoogvads (t-z5verhaal: Raadsheer Gouddraad stands at the bank counter on the market)
        NpcRollen.koppel("bank", Bank::open);
    }

    public static int tabs(ServerPlayer p) {
        return Math.min(MAX_TABS, 1 + Opslag.data(p).bankGekocht);
    }

    public static boolean open(ServerPlayer p) {
        return open(p, 0, p.position());
    }

    static boolean open(ServerPlayer p, int tab, Vec3 waar) {
        if (!Opslag.magOpenen(p)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.opslag.alleen_rijk").withStyle(ChatFormatting.GRAY));
            return false;
        }
        OpslagData d = Opslag.data(p);
        CompoundTag info = info(p);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new OpslagMenu(id, inv, OpslagMenu.BANK, 6, MAX_TABS, d::bankTab,
                () -> tabs(p), sp -> Opslag.magOpenen(sp) && sp.position().distanceTo(waar) <= BEREIK, info, () -> koop(p, waar), tab),
                Component.translatable("vadsrijk.opslag.bank")), buf -> OpslagMenu.schrijf(buf, OpslagMenu.BANK, 6, MAX_TABS, info));
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.5f, 1.2f);
        Tips.toon(p, "vadsrijk.opslag.tip.bank");
        return true;
    }

    static CompoundTag info(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        int g = Opslag.data(p).bankGekocht;
        t.putLong("prijs", g < PRIJZEN.length ? PRIJZEN[g] : 0);
        return t;
    }

    static void koop(ServerPlayer p, Vec3 waar) {
        OpslagData d = Opslag.data(p);
        int tab = p.containerMenu instanceof OpslagMenu m ? m.huidigeTab() : 0;
        if (d.bankGekocht >= PRIJZEN.length) {
            return;
        }
        long prijs = PRIJZEN[d.bankGekocht];
        if (!Spelers.neemMunten(p, prijs)) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.opslag.te_weinig", Spelers.munten(prijs)).withStyle(ChatFormatting.RED));
            return;
        }
        d.bankGekocht++;
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.6f, 1.4f);
        p.sendSystemMessage(Component.translatable("vadsrijk.opslag.bank.gekocht", tabs(p)).withStyle(ChatFormatting.LIGHT_PURPLE));
        p.level().getServer().execute(() -> open(p, tab, waar));
    }
}
