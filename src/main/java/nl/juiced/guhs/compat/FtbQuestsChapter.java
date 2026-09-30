package nl.juiced.guhs.compat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.Mth;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;

/**
 * If FTB Quests is installed, puts the "Guhs" chapter group (nine themed chapters, their Dutch texts and the group) into
 * config/ftbquests/quests, so it shows up in the pack's quest book. The chapters are made by tools/make_ftbquests.py;
 * ftbquests/index.txt lists them (and the group id).
 * <p>
 * Pack makers' own edits are left alone. FTB Quests re-saves the chapter files (and drops our "guhs_chapter_version"
 * marker when it does), so what we installed is remembered in quests/guhs_chapters.txt: per chapter the version and a
 * fingerprint (all ids and positions in the file). A chapter is only (re)written when:
 * <ul>
 * <li>it isn't there yet;</li>
 * <li>it still has our marker with an older version; or</li>
 * <li>we installed an older version and the file still has the same ids and positions (FTB Quests only re-saved it).</li>
 * </ul>
 * The old single chapter (chapters/guhs.snbt, before 2.8) is removed when it is ours: it has the marker, or every id in
 * it is one of ours (they all start with 475548, "GUH" in hex). If a pack added its own quests to it, it stays, and then
 * nothing is installed at all (the new chapters use the same quest ids).
 */
public final class FtbQuestsChapter {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Pattern VERSION = Pattern.compile("guhs_chapter_version: (\\d+)");
    private static final Pattern ID = Pattern.compile("\"([0-9A-F]{16})\"");
    private static final Pattern POSITION = Pattern.compile("(?m)^\\s*(x|y): (-?[0-9.]+)d?\\s*$");
    /** Lang lines of ours (all our ids start with 475548, "GUH" in hex). */
    private static final Pattern OUR_LANG_ENTRY = Pattern.compile("^\\s*[a-z_]+\\.475548[0-9A-F]{10}\\.[a-z_]+: (.*)$");
    static final String INSTALLED = "guhs_chapters.txt";

    public static void install() {
        if (ModList.get().isLoaded("ftbquests")) {
            installInto(FMLPaths.CONFIGDIR.get().resolve("ftbquests").resolve("quests"));
        }
    }

    /** The chapter files we ship (from ftbquests/index.txt), in reading order. */
    public static List<String> chapters() throws IOException {
        List<String> out = new ArrayList<>();
        String index = resource("ftbquests/index.txt");
        if (index != null) {
            for (String line : index.split("\\R")) {
                if (line.startsWith("chapter ")) {
                    out.add(line.substring(8).trim());
                }
            }
        }
        return out;
    }

    /** Installs into a quests folder (config/ftbquests/quests); returns whether it wrote anything. */
    public static boolean installInto(Path quests) {
        try {
            String index = resource("ftbquests/index.txt");
            String lang = resource("ftbquests/guhs_lang.snbt");
            if (index == null || lang == null) {
                return false;
            }
            String group = null;
            for (String line : index.split("\\R")) {
                if (line.startsWith("group ")) {
                    group = line.substring(6).trim();
                }
            }
            Path chapters = quests.resolve("chapters");
            Path split = quests.resolve("lang").resolve("en_us").resolve("chapters");
            // the old single chapter (before 2.8)
            Path old = chapters.resolve("guhs.snbt");
            if (Files.exists(old)) {
                if (!oldChapterIsOurs(Files.readString(old))) {
                    LOGGER.info("config/ftbquests/quests/chapters/guhs.snbt has the pack's own quests: the Guhs chapters are left alone");
                    return false;
                }
                Files.delete(old);
                Files.deleteIfExists(split.resolve("guhs.snbt"));
            }
            Map<String, String[]> installed = readInstalled(quests.resolve(INSTALLED));
            boolean wrote = false;
            for (String name : chapters()) {
                String chapter = resource("ftbquests/chapters/" + name + ".snbt");
                if (chapter == null) {
                    continue;
                }
                int version = version(chapter);
                Path file = chapters.resolve(name + ".snbt");
                if (Files.exists(file) && !shouldReplace(Files.readString(file), installed.get(name), version)) {
                    continue;
                }
                Files.createDirectories(chapters);
                Files.writeString(file, chapter, StandardCharsets.UTF_8);
                installed.put(name, new String[] {String.valueOf(version), fingerprint(chapter)});
                // packs that split their lang files get the chapter's texts next to their own
                String chapterLang = resource("ftbquests/lang/" + name + ".snbt");
                if (chapterLang != null && Files.isDirectory(quests.resolve("lang").resolve("en_us"))) {
                    Files.createDirectories(split);
                    Files.writeString(split.resolve(name + ".snbt"), chapterLang, StandardCharsets.UTF_8);
                }
                wrote = true;
            }
            if (!wrote) {
                return false;
            }
            writeInstalled(quests.resolve(INSTALLED), installed);
            if (group != null) {
                addGroup(quests.resolve("chapter_groups.snbt"), group);
            }
            // our texts are Dutch in every language: also into the Dutch lang file when the pack has one (else FTB Quests
            // falls back to en_us by itself)
            Path nl = quests.resolve("lang").resolve("nl_nl.snbt");
            if (Files.exists(nl)) {
                String text = stripOurLang(Files.readString(nl));
                int end = text.lastIndexOf('}');
                Files.writeString(nl, text.substring(0, end) + lang.substring(lang.indexOf('{') + 1, lang.lastIndexOf('}')) + "}\n", StandardCharsets.UTF_8);
            }
            if (Files.isDirectory(quests.resolve("lang").resolve("nl_nl"))) {
                for (String name : chapters()) {
                    String chapterLang = resource("ftbquests/lang/" + name + ".snbt");
                    if (chapterLang != null) {
                        Path dir = quests.resolve("lang").resolve("nl_nl").resolve("chapters");
                        Files.createDirectories(dir);
                        Files.writeString(dir.resolve(name + ".snbt"), chapterLang, StandardCharsets.UTF_8);
                    }
                }
            }
            // all texts into the main lang file (our old lines out first)
            Path main = quests.resolve("lang").resolve("en_us.snbt");
            String entries = lang.substring(lang.indexOf('{') + 1, lang.lastIndexOf('}'));
            if (Files.exists(main)) {
                String text = stripOurLang(Files.readString(main));
                int end = text.lastIndexOf('}');
                text = text.substring(0, end) + entries + "}\n";
                Files.writeString(main, text, StandardCharsets.UTF_8);
            } else {
                Files.createDirectories(main.getParent());
                Files.writeString(main, lang, StandardCharsets.UTF_8);
            }
            LOGGER.info("Installed the Guhs chapters for FTB Quests");
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not install the Guhs FTB Quests chapters", e);
            return false;
        }
    }

    /**
     * The lang file without our entries. FTB Quests writes a text list over several lines when it saves
     * (key: [ newline "..." newline ... ]), so a list of ours is left out up to its closing bracket.
     */
    public static String stripOurLang(String text) {
        StringBuilder out = new StringBuilder();
        boolean inList = false;
        for (String line : text.split("\n", -1)) {
            if (inList) {
                inList = !line.trim().equals("]");
                continue;
            }
            Matcher m = OUR_LANG_ENTRY.matcher(line);
            if (m.matches()) {
                String value = m.group(1).trim();
                inList = value.startsWith("[") && !value.endsWith("]");
                continue;
            }
            out.append(line).append('\n');
        }
        return out.substring(0, Math.max(0, out.length() - 1));
    }

    /** Should the chapter file on disk be replaced by ours of this version? (installed: {version, fingerprint} or null) */
    public static boolean shouldReplace(String onDisk, String[] installed, int version) {
        int marked = version(onDisk);
        if (marked > 0) {
            return marked < version;             // still exactly as we wrote it
        }
        if (installed == null) {
            return false;                        // not ours (or we can't tell): leave it alone
        }
        return Integer.parseInt(installed[0]) < version && fingerprint(onDisk).equals(installed[1]);
    }

    /** The old single chapter is ours when it has our marker, or when every id in it is one of ours. */
    public static boolean oldChapterIsOurs(String snbt) {
        if (version(snbt) > 0) {
            return true;
        }
        Matcher m = ID.matcher(snbt);
        boolean any = false;
        while (m.find()) {
            if (!m.group(1).startsWith("475548")) {
                return false;
            }
            any = true;
        }
        return any;
    }

    /** What a pack maker would change: the ids (quests, tasks, rewards, links, pictures, dependencies) and the positions. */
    public static String fingerprint(String snbt) {
        List<String> ids = new ArrayList<>();
        Matcher m = ID.matcher(snbt);
        while (m.find()) {
            ids.add(m.group(1));
        }
        List<String> pos = new ArrayList<>();
        Matcher p = POSITION.matcher(snbt);
        while (p.find()) {
            pos.add(p.group(1) + Math.round(Double.parseDouble(p.group(2)) * 1000));
        }
        ids.sort(null);
        pos.sort(null);
        return Integer.toHexString(ids.hashCode()) + "-" + Integer.toHexString(pos.hashCode()) + "-" + ids.size() + "-" + pos.size();
    }

    private static Map<String, String[]> readInstalled(Path file) throws IOException {
        Map<String, String[]> out = new LinkedHashMap<>();
        if (Files.exists(file)) {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 3 && !line.startsWith("#")) {
                    out.put(parts[0], new String[] {parts[1], parts[2]});
                }
            }
        }
        return out;
    }

    private static void writeInstalled(Path file, Map<String, String[]> installed) throws IOException {
        StringBuilder sb = new StringBuilder("# The Guhs chapters as the Guhs mod installed them: chapter, version, fingerprint (ids and positions).\n"
                + "# A chapter you change in the quest editor is left alone when Guhs updates. Delete a line to let Guhs replace it.\n");
        installed.forEach((name, v) -> sb.append(name).append(' ').append(v[0]).append(' ').append(v[1]).append('\n'));
        Files.createDirectories(file.getParent());
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    /** Puts { id: "group" } into chapter_groups.snbt (made when it isn't there), unless it is in there already. */
    static void addGroup(Path file, String group) throws IOException {
        String entry = "{ id: \"" + group + "\" }";
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{\n\tchapter_groups: [\n\t\t" + entry + "\n\t]\n}\n", StandardCharsets.UTF_8);
            return;
        }
        String text = Files.readString(file);
        if (text.contains(group)) {
            return;
        }
        Matcher m = Pattern.compile("chapter_groups\\s*:\\s*\\[").matcher(text);
        if (m.find()) {
            int close = text.indexOf(']', m.end());
            text = text.substring(0, close).stripTrailing() + "\n\t\t" + entry + "\n\t" + text.substring(close);
        } else {
            int end = text.lastIndexOf('}');
            text = text.substring(0, Mth.clamp(end, 0, text.length())) + "\tchapter_groups: [\n\t\t" + entry + "\n\t]\n}\n";
        }
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    static int version(String snbt) {
        Matcher m = VERSION.matcher(snbt);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    static String resource(String path) throws IOException {
        try (InputStream in = FtbQuestsChapter.class.getClassLoader().getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private FtbQuestsChapter() {
    }
}
