package nl.juiced.guhs.entity;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import nl.juiced.guhs.Guhs;

/**
 * 1.2.6: our structure templates name some of their guhs with a JSON text ("{"translate": "entity.guhs.beroepen_snotje"}").
 * Since 26.1 a name in entity data is a text component and such a string is read as plain text, so the raw JSON floated
 * above their heads. When one of our entities joins a level with such a name, the JSON is read as the component it means
 * (also for the ones that already exist in a world).
 */
public final class JsonNaamFix {
    public static void onJoin(EntityJoinLevelEvent event) {
        Entity e = event.getEntity();
        if (event.getLevel().isClientSide() || !Guhs.MODID.equals(BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace())) {
            return;
        }
        Component fixed = lees(e.getCustomName());
        if (fixed != null) {
            e.setCustomName(fixed);
        }
    }

    /** The component a plain-text name really is ("{...}" JSON with translate/text), or null when it's just a name. */
    public static Component lees(Component naam) {
        if (naam == null || !(naam.getContents() instanceof PlainTextContents plain) || !naam.getSiblings().isEmpty()) {
            return null;
        }
        String s = plain.text().trim();
        if (!s.startsWith("{") || !s.endsWith("}") || !(s.contains("\"translate\"") || s.contains("\"text\""))) {
            return null;
        }
        try {
            JsonElement json = JsonParser.parseString(s);
            return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(null);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private JsonNaamFix() {
    }
}
