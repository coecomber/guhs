package nl.juiced.guhs.feature.wereld;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2: the role of a quest NPC of a building: the same entity for everyone, a different talk for every player. All
 * progress is in the questline ({@link Stappen}: the story engine's {@code Verhaallijn}, per player), nothing in the NPC,
 * so an unlimited number of players can do the questline at the same NPC at the same time.
 * <pre>
 * static final Verhaallijn LIJN = Verhaallijn.maak("wachter", "barbecue").stappen(3).icoon("guhs:grillspies").registreer();
 *
 * NpcRollen.zet(GuhNpcEntity.Kind.WACHTERGUH, new QuestRol(LIJN) {
 *     protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
 *         switch (stap) {
 *             case 0 -&gt; { zeg(p, npc, "quest.guhs.bestaand.wachter.hallo"); verder(p, 0); }      // 0 -&gt; 1: the quest is given
 *             case 1 -&gt; lever(p, npc, 1, ModItems.GRILLKOOL.get(), 4, "quest.guhs.bestaand.wachter.tekort");   // 1 -&gt; 2
 *             case 2 -&gt; scherm(p, npc, "quest.guhs.bestaand.wachter.klaar", new Praat.Optie(1, "gui.guhs.bestaand.ja"));
 *             default -&gt; zeg(p, npc, "quest.guhs.bestaand.wachter.dank");                          // done: a chat for later
 *         }
 *     }
 *     protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
 *         if (stap == 2 &amp;&amp; optie == 1 &amp;&amp; verder(p, 2)) {                                        // 2 -&gt; 3 = done
 *             geefEenmalig(p, "beloning", new ItemStack(BestaandFeature.RECEPT.get()));           // once per player
 *             zichtbaar(p, "barbecuether/bestaand_wachter");                                       // the visible advancement
 *         }
 *     }
 * });
 * </pre>
 * The {@code Verhaallijn} grants the hidden advancements {@code quest/<lijn>_stap_<i>} (the FTB tasks) and fills the
 * Guhdex tab Verhalen by itself. Rules for every questline (CONTRACT_130 6.3.5): a quest item never comes out of a lootable
 * chest (give it here: {@link #geefEenmalig}, and again when it was lost: {@link #geefAlsKwijt}); what a player changes in
 * the world for the quest is put back with {@link Herstel}.
 */
public abstract class QuestRol implements NpcRole {
    private final Stappen lijn;

    /** Pass the questline (a {@code Verhaallijn}: it implements {@link Stappen}). */
    protected QuestRol(Stappen lijn) {
        this.lijn = lijn;
    }

    /** The player right-clicked the NPC while at this step (0 = not begun; the number of steps = done). */
    protected abstract void praat(GuhNpcEntity npc, ServerPlayer p, int stap);

    /**
     * The answer the player picked in the talking screen this NPC opened ({@link #scherm}), while at this step; optie -1 = the
     * screen was closed. Nothing by default.
     */
    protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
    }

    @Override
    public final void talk(GuhNpcEntity npc, ServerPlayer player) {
        praat(npc, player, lijn.stap(player));
    }

    @Override
    public final void antwoord(GuhNpcEntity npc, ServerPlayer player, int optie) {
        antwoord(npc, player, lijn.stap(player), optie);
    }

    protected final Stappen lijn() {
        return lijn;
    }

    /** The step this player is at. */
    protected final int stap(ServerPlayer p) {
        return lijn.stap(p);
    }

    /** One step further, only when the player is exactly at {@code vanStap} (true = changed): safe to call twice. */
    protected final boolean verder(ServerPlayer p, int vanStap) {
        return lijn.verder(p, vanStap);
    }

    /** A chat line of the NPC, only for this player: "&lt;Name&gt; text". */
    protected void zeg(ServerPlayer p, GuhNpcEntity npc, String key, Object... args) {
        GuhQuests.say(p, npc, key, args);
    }

    /** A grey hint line under it (what to do now), only for this player. */
    protected void hint(ServerPlayer p, String key) {
        GuhQuests.hint(p, key);
    }

    /** The talking screen (portrait, speech balloon, answer buttons); the answer comes back in {@link #antwoord}. */
    protected void scherm(ServerPlayer p, GuhNpcEntity npc, String key, Praat.Optie... opties) {
        Praat.open(p, npc, null, key, new Object[0], opties);
    }

    /** Like {@link #scherm}, with arguments for the text. */
    protected void schermMet(ServerPlayer p, GuhNpcEntity npc, String key, Object[] args, Praat.Optie... opties) {
        Praat.open(p, npc, null, key, args, opties);
    }

    /** Does the player carry {@code n} of this item? */
    protected boolean heeft(ServerPlayer p, Item item, int n) {
        return GuhQuests.count(p, item) >= n;
    }

    /** Takes {@code n} of this item when the player has them (true), else nothing. */
    protected boolean neem(ServerPlayer p, Item item, int n) {
        if (GuhQuests.count(p, item) < n) {
            return false;
        }
        GuhQuests.take(p, item, n);
        return true;
    }

    /**
     * The classic "bring me n of this" step: when the player has them they are taken and the player goes from
     * {@code vanStap} to the next step (true); else the NPC says {@code keyTekort} with (how many the player has, n).
     */
    protected boolean lever(ServerPlayer p, GuhNpcEntity npc, int vanStap, Item item, int n, String keyTekort) {
        if (lijn.stap(p) != vanStap) {
            return false;
        }
        if (!neem(p, item, n)) {
            zeg(p, npc, keyTekort, GuhQuests.count(p, item), n);
            return false;
        }
        return lijn.verder(p, vanStap);
    }

    /** Into the inventory, or dropped at the feet when it is full. */
    protected void geef(ServerPlayer p, ItemStack stack) {
        Minigames.give(p, stack.copy());
    }

    /** Gives these once per player and {@code naam} (the reward of a questline): true the first time, false ever after. */
    protected boolean geefEenmalig(ServerPlayer p, String naam, ItemStack... stacks) {
        if (!lijn.eenmalig(p, naam)) {
            return false;
        }
        for (ItemStack stack : stacks) {
            geef(p, stack);
        }
        return true;
    }

    /** A quest item the player still needs: given (again) when they don't carry one. True when it was given. */
    protected boolean geefAlsKwijt(ServerPlayer p, Item item) {
        if (GuhQuests.count(p, item) > 0) {
            return false;
        }
        geef(p, new ItemStack(item));
        return true;
    }

    /** Grants the hidden advancement guhs:quest/&lt;name&gt; (an extra FTB task next to the questline's own steps). */
    protected void verborgen(ServerPlayer p, String name) {
        GuhAdvancements.grant(p, name);
    }

    /** Grants the visible advancement guhs:&lt;tab&gt;/&lt;name&gt; (pass "tab/name", e.g. "barbecuether/bestaand_wachter"). */
    protected void zichtbaar(ServerPlayer p, String tabEnNaam) {
        GidsFeature.grant(p, tabEnNaam);
    }
}
