package nl.juiced.guhs.client;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.resources.VanillaClientListeners;
import net.neoforged.neoforge.common.NeoForge;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.taal.Taal;
import org.jspecify.annotations.Nullable;

/**
 * 1.2.0: the NL/EN switch of the Guhs texts (per player, client-side; also on servers, because every Guhs text reaches the
 * client as a translatable key). Only the keys of our own lang files (assets/guhs/lang/nl_nl.json and en_us.json) follow the
 * switch; vanilla and other mods keep the Minecraft language.
 * <p>
 * How: after vanilla's LanguageManager (re)loads the Minecraft language, {@link #apply()} wraps that Language in a
 * {@link GuhsLanguage} that answers our keys from the chosen file, and injects it as Language.getInstance() and as I18n's
 * language (FTB Library and I18n.exists read that one; accesstransformer). A new Language object invalidates every
 * TranslatableContents cache (identity check), so chat, tooltips, names, books and screens follow at once. Signs cache their
 * rendered lines: those of the loaded chunks are reset too. FTB Quests: compat/FtbQuestsTaal (+ mixin client.FtbQuestsLocaleMixin).
 */
public final class GuhsTaal {
    public static final Identifier RELOAD_ID = Guhs.id("taal");
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    private static Map<String, String> nlTeksten = Map.of();
    private static Map<String, String> enTeksten = Map.of();
    private static Map<String, Component> nlComponents = Map.of();
    private static Map<String, Component> enComponents = Map.of();

    public static void init(IEventBus modBus) {
        modBus.addListener(GuhsTaal::addReloadListener);
        modBus.addListener(GuhsTaal::onConfigReload);
        NeoForge.EVENT_BUS.addListener(GuhsTaal::onLogin);
    }

    /** The choice of the config (AUTO when it isn't loaded yet). */
    public static Taal choice() {
        return GuhsClientConfig.language();
    }

    /** Minecraft's own language code ("en_us", "nl_nl", ...). */
    public static String minecraftLanguage() {
        Minecraft mc = Minecraft.getInstance();
        return mc == null || mc.getLanguageManager() == null ? "en_us" : mc.getLanguageManager().getSelected();
    }

    /** Are the Guhs texts Dutch right now? */
    public static boolean dutch() {
        return choice().dutch(minecraftLanguage());
    }

    /** "nl_nl" or "en_us": the Guhs lang file in use. */
    public static String code() {
        return choice().code(minecraftLanguage());
    }

    /** The guh menu button: Auto, NL, EN, Auto... (saved in config/guhs-client.toml, applied at once). */
    public static void cycle() {
        set(choice().next());
    }

    public static void set(Taal taal) {
        if (!GuhsClientConfig.SPEC.isLoaded()) {
            return;
        }
        GuhsClientConfig.LANGUAGE.set(taal);
        GuhsClientConfig.SPEC.save();
        apply();
    }

    /** The button label: "Taal: Auto (NL)" / "Language: English". */
    public static Component label() {
        Taal taal = choice();
        Component name = taal == Taal.AUTO
                ? Component.translatable("gui.guhs.taal.auto_is", Component.translatable(dutch() ? "gui.guhs.taal.kort.nl" : "gui.guhs.taal.kort.en"))
                : Component.translatable(taal.key());
        return Component.translatable("gui.guhs.menu.taal", name);
    }

    // ================================================================================================================

    private static void addReloadListener(AddClientReloadListenersEvent event) {
        event.addListener(RELOAD_ID, (ResourceManagerReloadListener) rm -> {
            load(rm);
            apply();
        });
        event.addDependency(VanillaClientListeners.LANGUAGE, RELOAD_ID);
    }

    private static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == GuhsClientConfig.SPEC) {
            Minecraft.getInstance().execute(GuhsTaal::apply);
        }
    }

    /** Joining a server: FTB Quests only gets the tables of the Minecraft language; ask for ours when it differs. */
    private static void onLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        if (ModList.get().isLoaded("ftbquests")) {
            nl.juiced.guhs.compat.FtbQuestsTaal.onLogin();
        }
    }

    private static void load(ResourceManager rm) {
        Map<String, String> en = new HashMap<>(), nl = new HashMap<>();
        Map<String, Component> enC = new HashMap<>(), nlC = new HashMap<>();
        read(rm, "en_us", en, enC);
        nl.putAll(en);   // (a key missing in nl_nl falls back on English, like vanilla's ["en_us", code] stack)
        nlC.putAll(enC);
        read(rm, "nl_nl", nl, nlC);
        enTeksten = Map.copyOf(en);
        nlTeksten = Map.copyOf(nl);
        enComponents = Map.copyOf(enC);
        nlComponents = Map.copyOf(nlC);
    }

    private static void read(ResourceManager rm, String code, Map<String, String> out, Map<String, Component> components) {
        // every pack's guhs:lang/<code>.json in order, so resource packs can still change our texts
        for (Resource resource : rm.getResourceStack(Guhs.id("lang/" + code + ".json"))) {
            try (InputStream in = resource.open()) {
                Language.loadFromJson(in, out::put, components::put);
            } catch (Exception e) {
                LOGGER.warn("Guhs: couldn't read {} from {}", "lang/" + code + ".json", resource.sourcePackId(), e);
            }
        }
    }

    /** Put the chosen Guhs texts on top of the current Minecraft language (again). Main thread. */
    public static void apply() {
        Minecraft mc = Minecraft.getInstance();
        if (enTeksten.isEmpty()) {
            return;   // (before the first resource load)
        }
        Language current = Language.getInstance();
        Language base = current instanceof GuhsLanguage g ? g.base : current;
        boolean dutch = dutch();
        GuhsLanguage lang = new GuhsLanguage(base, dutch ? nlTeksten : enTeksten, dutch ? nlComponents : enComponents);
        Language.inject(lang);
        I18n.setLanguage(lang);
        if (mc.player != null) {
            mc.player.connection.updateSearchTrees();
        }
        resetSigns(mc);
        if (ModList.get().isLoaded("ftbquests")) {
            nl.juiced.guhs.compat.FtbQuestsTaal.onSwitch();
        }
        if (mc.screen != null) {
            mc.screen.resize(mc.screen.width, mc.screen.height);   // (button labels are made in init())
        }
    }

    /** Signs keep their rendered lines (SignText.renderMessages, accesstransformer): forget them in the loaded chunks. */
    private static void resetSigns(Minecraft mc) {
        if (mc.level == null || mc.player == null) {
            return;
        }
        int cx = mc.player.chunkPosition().x(), cz = mc.player.chunkPosition().z();
        int r = mc.options.getEffectiveRenderDistance() + 2;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                LevelChunk chunk = mc.level.getChunkSource().getChunk(x, z, false);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof SignBlockEntity sign) {
                        sign.getFrontText().renderMessages = null;
                        sign.getBackText().renderMessages = null;
                    }
                }
            }
        }
    }

    /** Minecraft's Language with our keys answered from the chosen Guhs file. */
    static final class GuhsLanguage extends Language {
        final Language base;
        private final Map<String, String> ours;
        private final Map<String, Component> ourComponents;
        private @Nullable Map<String, String> merged;

        GuhsLanguage(Language base, Map<String, String> ours, Map<String, Component> ourComponents) {
            this.base = base;
            this.ours = ours;
            this.ourComponents = ourComponents;
        }

        @Override
        public String getOrDefault(String key, String fallback) {
            String v = ours.get(key);
            return v != null ? v : base.getOrDefault(key, fallback);
        }

        @Override
        public boolean has(String key) {
            return ours.containsKey(key) || base.has(key);
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return base.isDefaultRightToLeft();
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText text) {
            return base.getVisualOrder(text);
        }

        @Override
        public List<FormattedCharSequence> getVisualOrder(List<FormattedText> lines) {
            return base.getVisualOrder(lines);
        }

        @Override
        public Map<String, String> getLanguageData() {
            if (merged == null) {
                Map<String, String> m = new HashMap<>(base.getLanguageData());
                m.putAll(ours);
                merged = Map.copyOf(m);
            }
            return merged;
        }

        @Override
        public @Nullable Component getComponent(String key) {
            return ours.containsKey(key) ? ourComponents.get(key) : base.getComponent(key);
        }
    }

    private GuhsTaal() {
    }
}
