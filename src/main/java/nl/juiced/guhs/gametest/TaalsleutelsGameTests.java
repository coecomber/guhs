package nl.juiced.guhs.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalEvents;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * 1.1.4: no raw translation keys in game. Players saw "entity.guhs.bewoner.timmertje" above a Knuffeldal resident: the
 * name came from a structure template and nl_nl.json had no text for it. These tests check the keys the mod builds at
 * run time (registry names, item tooltips, names and texts stored in our structure templates, enum-based and numbered
 * keys) against the server's Language (en_us is Dutch too, so it holds every Guhs text).
 */
public class TaalsleutelsGameTests {
    private static final String BATCH = "taalsleutels";
    private static final Pattern TRANSLATE = Pattern.compile("\"translate\"\\s*:\\s*\"([^\"]+)\"");

    private static void check(GameTestHelper helper, List<String> missing, String what) {
        if (!missing.isEmpty()) {
            helper.fail(what + ": " + missing.size() + " missing: " + new TreeSet<>(missing));
        }
    }

    private static void need(List<String> missing, String key) {
        if (!Language.getInstance().has(key)) {
            missing.add(key);
        }
    }

    private static void rij(List<String> missing, String prefix, int count) {
        for (int i = 0; i < count; i++) {
            need(missing, prefix + i);
        }
    }

    /** The name of everything we register: items, blocks, entities, effects, biomes. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void registryNamen(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        BuiltInRegistries.ITEM.forEach(item -> {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Guhs.MODID)) {
                need(missing, item.getDescriptionId());
            }
        });
        BuiltInRegistries.BLOCK.forEach(block -> {
            if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Guhs.MODID)) {
                need(missing, block.getDescriptionId());
            }
        });
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            if (BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(Guhs.MODID)) {
                need(missing, type.getDescriptionId());
            }
        });
        BuiltInRegistries.MOB_EFFECT.forEach(effect -> {
            if (BuiltInRegistries.MOB_EFFECT.getKey(effect).getNamespace().equals(Guhs.MODID)) {
                need(missing, effect.getDescriptionId());
            }
        });
        helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME).listElementIds()
                .filter(k -> k.identifier().getNamespace().equals(Guhs.MODID))
                .forEach(k -> need(missing, "biome." + k.identifier().getNamespace() + "." + k.identifier().getPath()));
        check(helper, missing, "registry names");
        helper.succeed();
    }

    /** Every translatable text in the tooltip of every Guhs item (lore lines and the like). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void tooltips(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        var context = net.minecraft.world.item.Item.TooltipContext.of(helper.getLevel());
        BuiltInRegistries.ITEM.forEach(item -> {
            if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Guhs.MODID)) {
                return;
            }
            List<Component> lines;
            try {
                lines = new ItemStack(item).getTooltipLines(context, null, TooltipFlag.NORMAL);
            } catch (Throwable t) {
                return;     // a client-only tooltip; the static checks cover those
            }
            for (Component line : lines) {
                keys(line, k -> need(missing, k));
            }
        });
        check(helper, missing, "tooltips");
        helper.succeed();
    }

    private static void keys(Component c, java.util.function.Consumer<String> out) {
        if (c.getContents() instanceof TranslatableContents t) {
            out.accept(t.getKey());
            for (Object arg : t.getArgs()) {
                if (arg instanceof Component a) {
                    keys(a, out);
                }
            }
        }
        for (Component sibling : c.getSiblings()) {
            keys(sibling, out);
        }
    }

    /**
     * Our structure templates: the names of the Knuffeldal residents (and their three lines), the NPC kinds and every
     * {"translate": ...} on signs, books and item names.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 400)
    public static void structuurTeksten(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        var manager = helper.getLevel().getStructureManager();
        int[] count = {0};
        manager.listTemplates().filter(id -> id.getNamespace().equals(Guhs.MODID)).toList().forEach(id -> {
            var template = manager.get(id).orElse(null);
            if (template == null) {
                return;
            }
            count[0]++;
            CompoundTag tag = template.save(new CompoundTag());
            walk(tag, id.getPath(), missing);
        });
        helper.assertTrue(count[0] > 50, "found our structure templates: " + count[0]);
        check(helper, missing, "structure texts");
        helper.succeed();
    }

    private static void walk(Tag tag, String where, List<String> missing) {
        if (tag instanceof CompoundTag c) {
            String naam = c.getStringOr(GuhHooks.BEWONER_NAAM, "");
            if (!naam.isEmpty()) {
                int before = missing.size();
                need(missing, "entity.guhs.bewoner." + naam);
                for (int i = 0; i < KnuffeldalEvents.BEWONER_LINES; i++) {
                    need(missing, "quest.guhs.knuffeldal.bewoner." + naam + "." + i);
                }
                tagWhere(missing, before, where);
            }
            if (c.getStringOr("id", "").equals("guhs:guh_npc") && c.contains("Kind")) {
                int before = missing.size();
                need(missing, "entity.guhs.guh_npc." + c.getStringOr("Kind", "").toLowerCase(Locale.ROOT));
                tagWhere(missing, before, where);
            }
            for (String k : c.keySet()) {
                walk(c.get(k), where, missing);
            }
        } else if (tag instanceof ListTag list) {
            for (Tag t : list) {
                walk(t, where, missing);
            }
        } else if (tag != null && tag.getId() == Tag.TAG_STRING) {
            String s = tag.asString().orElse("");
            if (s.contains("translate")) {
                Matcher m = TRANSLATE.matcher(s);
                int before = missing.size();
                while (m.find()) {
                    need(missing, m.group(1));
                }
                tagWhere(missing, before, where);
            }
        }
    }

    private static void tagWhere(List<String> missing, int from, String where) {
        for (int i = from; i < missing.size(); i++) {
            missing.set(i, missing.get(i) + " (" + where + ")");
        }
    }

    /** Keys built from enums and numbered lines. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void enumsEnRijtjes(GameTestHelper helper) {
        List<String> missing = new ArrayList<>();
        for (GuhVariant v : GuhVariant.values()) {
            if (v != GuhVariant.NORMAL && !v.isCharacter()) {
                need(missing, "entity.guhs.guh." + v.id());
            } else if (v.isCharacter()) {
                need(missing, v.npcKind() == null ? "entity.guhs." + v.id() : "entity.guhs.guh_npc." + v.id());
            }
            // the Guhdex page
            need(missing, "gui.guhs.guhdex.rarity." + v.id());
            need(missing, "gui.guhs.guhdex.info." + v.id());
        }
        for (GuhNpcEntity.Kind kind : GuhNpcEntity.Kind.values()) {
            need(missing, "entity.guhs.guh_npc." + kind.id());
        }
        for (GuhPersonality p : GuhPersonality.values()) {
            need(missing, "gui.guhs.personality." + p.id());
            need(missing, "gui.guhs.personality." + p.id() + ".description");
        }
        // the numbered lines: prefix + 0..count-1 (the count is the constant the code rolls with)
        rij(missing, "quest.guhs.grillguh.tip", nl.juiced.guhs.feature.barbecuether.Grillguh.TIPS);
        rij(missing, "quest.guhs.kaasmijn.tip", nl.juiced.guhs.feature.kaasmijn.Mijnguh.TIPS);
        rij(missing, "quest.guhs.kamperen.tip", nl.juiced.guhs.feature.kamperen.OpaGuh.TIPS);
        rij(missing, "quest.guhs.kamperen.marshmallow", nl.juiced.guhs.feature.kamperen.KampvuurMarshmallow.OPA_ZINNEN);
        rij(missing, "quest.guhs.knuffelbad.badmeester.tip", nl.juiced.guhs.feature.knuffelbad.Badmeester.TIPS);
        rij(missing, "quest.guhs.onderwater.tip", nl.juiced.guhs.feature.onderwater.ZeemeerguhRole.TIPS);
        rij(missing, "quest.guhs.sterrenwacht.tip", nl.juiced.guhs.feature.sterrenwacht.SterrenwachtRole.TIPS);
        rij(missing, "quest.guhs.vadswoud.boswachter.tip", nl.juiced.guhs.feature.vadswoud.Boswachterguh.TIPS);
        rij(missing, "quest.guhs.vadswoud.plukker.tip", nl.juiced.guhs.feature.vadswoud.Knabbelplukker.TIPS);
        rij(missing, "item.guhs.klusjes_schelpje.oor.", nl.juiced.guhs.feature.klusjes.SchelpjeItem.OOR_ZINNEN);
        rij(missing, "quest.guhs.cocotje.lief.", nl.juiced.guhs.feature.knuffeldal.Cocotje.LIEVE_ZINNEN);
        rij(missing, "quest.guhs.vadsig.belly.", nl.juiced.guhs.quest.GuhQuests.BELLY_LINES);
        // the residents of the Knuffeldal and its friends book
        for (String naam : nl.juiced.guhs.feature.knuffeldal.KnuffeldalVoortgang.BEWONERS) {
            need(missing, "entity.guhs.bewoner." + naam);
            rij(missing, "quest.guhs.knuffeldal.bewoner." + naam + ".", KnuffeldalEvents.BEWONER_LINES);
        }
        for (String naam : nl.juiced.guhs.feature.knuffeldal.KnuffeldalVoortgang.VRIENDJES_LIJST) {
            need(missing, "gui.guhs.knus.knuffelvriendjes." + naam);
        }
        check(helper, missing, "enum and numbered keys");
        helper.succeed();
    }
}
