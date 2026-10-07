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
 * If FTB Quests is installed, puts the "Guhs" chapter group (the themed chapters, their texts and the group)
 * into config/ftbquests/quests, so it shows up in the pack's quest book. The chapters are made by
 * tools/make_ftbquests.py; ftbquests/index.txt lists them (and the group id).
 * <p>
 * guhpad: there are two chapter groups now ("Guhs" and, at the end of the sidebar, "Het Guhpad": the big stories). The
 * index lists every group ({@code group <id>}, in sidebar order); a chapter file names its own group. A chapter that we
 * installed and no longer ship (its quests moved to another chapter, with the same quest ids) is taken away again, unless
 * the pack changed it.
 * <p>
 * 1.1.0 (Minecraft 26.1.2): FTB Quests 26.1 reads only JSON5 (1.0.x wrote SNBT, which it ignores now). Everything goes
 * where FTB Quests saves it itself: chapters/&lt;name&gt;.json5, the texts in lang/&lt;locale&gt;/chapters/&lt;name&gt;.json5 (every
 * .json5 under lang/&lt;locale&gt;/ is read and merged), and the group in chapter_groups.json5.
 * <p>
 * 1.2.0: the texts come in two languages (ftbquests/lang/&lt;locale&gt;/&lt;name&gt;.json5 in the jar): English in lang/en_us
 * (also FTB Quests' fallback for every other language) and Dutch in lang/nl_nl (the folder is made when the pack doesn't
 * have it). FTB Quests shows the one that matches the player's Minecraft language.
 * <p>
 * Pack makers' own edits are left alone. FTB Quests re-saves the chapter files (and drops our "guhs_chapter_version"
 * marker when it does), so what we installed is remembered in quests/guhs_chapters.txt: per chapter the version and a
 * fingerprint (all ids and positions in the file). A chapter is only (re)written when:
 * <ul>
 * <li>it isn't there yet;</li>
 * <li>it still has our marker with an older version; or</li>
 * <li>we installed an older version and the file still has the same ids and positions (FTB Quests only re-saved it).</li>
 * </ul>
 * The old single chapter (chapters/guhs.json5, before 2.8) is removed when it is ours: it has the marker, or every id in
 * it is one of ours (they all start with 475548, "GUH" in hex). If a pack added its own quests to it, it stays, and then
 * nothing is installed at all (the new chapters use the same quest ids).
 */
public final class FtbQuestsChapter {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final Pattern VERSION = Pattern.compile("\"?guhs_chapter_version\"?\\s*:\\s*(\\d+)");
    private static final Pattern ID = Pattern.compile("\"([0-9A-F]{16})\"");
    private static final Pattern POSITION = Pattern.compile("(?m)^\\s*\"?(x|y)\"?\\s*:\\s*(-?[0-9.]+(?:[eE][-+]?[0-9]+)?)[dD]?\\s*,?\\s*$");
    static final String INSTALLED = "guhs_chapters.txt";
    static final String EXT = ".json5";
    /** The locales we ship texts for (ftbquests/lang/&lt;locale&gt;/): en_us (English, FTB Quests' fallback) and nl_nl (Dutch). */
    public static final List<String> LOCALES = List.of("en_us", "nl_nl");

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
            if (index == null) {
                return false;
            }
            List<String> groups = new ArrayList<>();   // (guhpad: every chapter group, in sidebar order)
            for (String line : index.split("\\R")) {
                if (line.startsWith("group ")) {
                    groups.add(line.substring(6).trim());
                }
            }
            Path chapters = quests.resolve("chapters");
            Path lang = quests.resolve("lang");
            // the old single chapter (before 2.8)
            Path old = chapters.resolve("guhs" + EXT);
            if (Files.exists(old)) {
                if (!oldChapterIsOurs(Files.readString(old))) {
                    LOGGER.info("config/ftbquests/quests/chapters/guhs.json5 has the pack's own quests: the Guhs chapters are left alone");
                    return false;
                }
                Files.delete(old);
                for (String locale : LOCALES) {
                    Files.deleteIfExists(lang.resolve(locale).resolve("chapters").resolve("guhs" + EXT));
                }
            }
            Map<String, String[]> installed = readInstalled(quests.resolve(INSTALLED));
            boolean wrote = false;
            for (String name : chapters()) {
                String chapter = resource("ftbquests/chapters/" + name + EXT);
                if (chapter == null) {
                    continue;
                }
                int version = version(chapter);
                Path file = chapters.resolve(name + EXT);
                if (Files.exists(file) && !shouldReplace(Files.readString(file), installed.get(name), version)) {
                    continue;
                }
                Files.createDirectories(chapters);
                Files.writeString(file, chapter, StandardCharsets.UTF_8);
                installed.put(name, new String[] {String.valueOf(version), fingerprint(chapter)});
                // the chapter's texts per language, in the file where FTB Quests keeps a chapter's texts itself
                for (String locale : LOCALES) {
                    String chapterLang = resource("ftbquests/lang/" + locale + "/" + name + EXT);
                    if (chapterLang != null) {
                        Path dir = lang.resolve(locale).resolve("chapters");
                        Files.createDirectories(dir);
                        Files.writeString(dir.resolve(name + EXT), chapterLang, StandardCharsets.UTF_8);
                    }
                }
                wrote = true;
            }
            // guhpad: a chapter of ours that is no longer shipped goes (the quests it had are in another chapter now, with
            // the same ids: two copies would be double quests), unless the pack changed it
            List<String> shipped = chapters();
            for (String name : new ArrayList<>(installed.keySet())) {
                if (shipped.contains(name)) {
                    continue;
                }
                Path file = chapters.resolve(name + EXT);
                if (Files.exists(file)) {
                    String onDisk = Files.readString(file);
                    if (version(onDisk) == 0 && !fingerprint(onDisk).equals(installed.get(name)[1])) {
                        continue;                    // (the pack's own edit: leave it alone)
                    }
                    Files.delete(file);
                    for (String locale : LOCALES) {
                        Files.deleteIfExists(lang.resolve(locale).resolve("chapters").resolve(name + EXT));
                    }
                }
                installed.remove(name);
                wrote = true;
            }
            if (!wrote) {
                return false;
            }
            writeInstalled(quests.resolve(INSTALLED), installed);
            for (String group : groups) {
                addGroup(quests.resolve("chapter_groups" + EXT), group);
            }
            LOGGER.info("Installed the Guhs chapters for FTB Quests");
            return true;
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not install the Guhs FTB Quests chapters", e);
            return false;
        }
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
    public static boolean oldChapterIsOurs(String chapter) {
        if (version(chapter) > 0) {
            return true;
        }
        Matcher m = ID.matcher(chapter);
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
    public static String fingerprint(String chapter) {
        List<String> ids = new ArrayList<>();
        Matcher m = ID.matcher(chapter);
        while (m.find()) {
            ids.add(m.group(1));
        }
        List<String> pos = new ArrayList<>();
        Matcher p = POSITION.matcher(chapter);
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

    /** Puts { id: "group" } into chapter_groups.json5 (made when it isn't there), unless it is in there already. */
    static void addGroup(Path file, String group) throws IOException {
        String entry = "{\n      id: \"" + group + "\",\n    },";
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{\n  chapter_groups: [\n    " + entry + "\n  ],\n}\n", StandardCharsets.UTF_8);
            return;
        }
        String text = Files.readString(file);
        if (text.contains(group)) {
            return;
        }
        Matcher m = Pattern.compile("\"?chapter_groups\"?\\s*:\\s*\\[").matcher(text);
        if (m.find()) {
            int close = closingBracket(text, m.end());
            String before = text.substring(0, close).stripTrailing();
            String comma = before.endsWith("[") || before.endsWith(",") ? "" : ",";
            text = before + comma + "\n    " + entry + "\n  " + text.substring(close);
        } else {
            int end = Mth.clamp(text.lastIndexOf('}'), 0, text.length());
            String before = text.substring(0, end).stripTrailing();
            String comma = before.endsWith("{") || before.endsWith(",") ? "" : ",";
            text = before + comma + "\n  chapter_groups: [\n    " + entry + "\n  ],\n}\n";
        }
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    /** The index of the ']' that closes the list opened just before {@code from} (skips nested lists/objects and strings). */
    static int closingBracket(String text, int from) {
        int depth = 0;
        char quote = 0;
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == '\\') {
                    i++;
                } else if (c == quote) {
                    quote = 0;
                }
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '[' || c == '{') {
                depth++;
            } else if (c == ']' || c == '}') {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }
        return text.length();
    }

    public static int version(String chapter) {
        Matcher m = VERSION.matcher(chapter);
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
