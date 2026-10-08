package nl.juiced.guhs.dev;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * DEV ONLY: an automatic in-game screenshot round through the real client. Only active when the JVM is started with
 * -Dguhs.autocheck=&lt;script file&gt; (the gradle run 'autocheckClient'); the package is left out of the guhs jar.
 * <p>
 * It creates a fresh singleplayer world 'guhs_autocheck', runs the script (see tools/autocheck/autocheck.txt for the
 * commands), writes screenshots to run/screenshots/autocheck/ and a report (commands that failed, structures not found,
 * missing textures/models from the log...) to run/screenshots/autocheck/report.txt, then quits the game.
 */
public final class AutoCheck {
    private static final Logger LOG = LogManager.getLogger("GuhsAutoCheck");
    private static final String WORLD = "guhs_autocheck";
    private static final String TAG = "autocheck";

    private static File outDir;
    private static List<String> scriptLines;
    private static int scriptIndex;
    private static final Deque<Action> queue = new ArrayDeque<>();
    private static Action current;
    private static State state = State.BOOT;
    private static int stateTicks;
    private static long startMillis;
    private static long timeoutMillis;
    private static final List<String> report = new ArrayList<>();
    private static final List<String> problems = new ArrayList<>();
    private static int shots;
    /** Camera rotation to hold (null = free). */
    private static Float lockYaw, lockPitch;
    /** 2.10.1: hold W (drive on / drive off). */
    private static boolean driveForward;
    /** bbq2: keys held by 'key <name> on' (Super Guhrio's side view is played with the real keys). */
    private static final java.util.Set<net.minecraft.client.KeyMapping> heldKeys = new java.util.LinkedHashSet<>();
    private static boolean opened; // a screen the script opened itself
    /** 2.9 visual QA: the box of the structure the last tplocate found (camrel / relcmd are relative to its min corner). */
    private static BoundingBox lastBox;
    /** 3.0 visual QA: the box of that structure's start piece (= its template; camtpl / looktpl are relative to its min corner). */
    private static BoundingBox lastTpl;
    /** The start piece's placement (a jigsaw piece): its origin and rotation, so camtpl follows a turned template. */
    private static BlockPos lastTplPos;
    private static net.minecraft.world.level.block.Rotation lastTplRot;
    private static ResourceKey<Level> lastDim;
    /** 2.9 visual QA: the NPC the last 'npc' command went to ('interact npc' clicks it). */
    private static int lastNpc = -1;
    private static long seed = 20260926L;
    private static int stopDelay = -1;
    /** Checkpoint (screenshots/autocheck/done.txt): items that were fine are skipped next round (-Dguhs.autocheck.fresh=true: all). */
    private static final java.util.Set<String> done = new java.util.LinkedHashSet<>();
    /** Structure boxes found (this round and earlier rounds), per "id@dimension", to spot structures that overlap. */
    private static final Map<String, BoundingBox> boxes = new LinkedHashMap<>();
    private static int skipped;

    private static boolean isDone(String key) {
        if (done.contains(key)) {
            skipped++;
            return true;
        }
        return false;
    }

    private static void markDone(String key) {
        if (done.add(key)) {
            try {
                Files.writeString(new File(outDir, "done.txt").toPath(), key + System.lineSeparator(), StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                LOG.error("AutoCheck: cannot write done.txt", e);
            }
        }
    }

    private static void loadDone() {
        File f = new File(outDir, "done.txt");
        if (Boolean.getBoolean("guhs.autocheck.fresh")) {
            f.delete();
            return;
        }
        if (!f.exists()) {
            return;
        }
        try {
            for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                done.add(line);
                // structure:<id>@<dim> x0 x1 y0 y1 z0 z1
                String[] p = line.split(" ");
                if (p.length == 7 && p[0].startsWith("structure:")) {
                    boxes.put(p[0].substring("structure:".length()), new BoundingBox(Integer.parseInt(p[1]), Integer.parseInt(p[3]), Integer.parseInt(p[5]),
                            Integer.parseInt(p[2]), Integer.parseInt(p[4]), Integer.parseInt(p[6])));
                }
            }
        } catch (Exception e) {
            LOG.error("AutoCheck: cannot read done.txt", e);
        }
    }

    private static boolean structureDone(Identifier id, String dim) {
        String prefix = "structure:" + id + "@" + dim;
        for (String k : done) {
            if (k.equals(prefix) || k.startsWith(prefix + " ")) {
                return true;
            }
        }
        return false;
    }

    /** Where the last teleport put the camera (waitrender waits until the client is really there). */
    private static Vec3 expectedCam;

    private enum State { BOOT, CREATING, JOINING, RUNNING, QUITTING }

    /**
     * 1.4.0 (two-player check): -Dguhs.autocheck.server=host:port makes this client JOIN that (dedicated, offline-mode) server
     * instead of making its own world. The script then runs "from a distance": a /command is sent as the player's own chat
     * command (the player must be an op there), 'camera' is a /tp, and everything that needs the integrated server (npc,
     * tplocate, use, the example-state commands) is reported as a problem and skipped. Two such clients keep step with
     * 'signal <name>' / 'await <name> [ticks]' (files in -Dguhs.autocheck.sync=<dir>).
     */
    private static final String REMOTE = System.getProperty("guhs.autocheck.server");

    private static boolean remote() {
        return REMOTE != null && !REMOTE.isBlank();
    }

    private static File syncFile(String name) {
        File dir = new File(System.getProperty("guhs.autocheck.sync", "autocheck_sync"));
        dir.mkdirs();
        return new File(dir, name.replaceAll("[^A-Za-z0-9_.\\-]", "_"));
    }

    private AutoCheck() {
    }

    public static void init(IEventBus modBus) {
        String script = System.getProperty("guhs.autocheck");
        if (script == null || script.isBlank()) {
            return;
        }
        try {
            scriptLines = Files.readAllLines(new File(script).toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("AutoCheck: cannot read script {}", script, e);
            return;
        }
        timeoutMillis = Long.getLong("guhs.autocheck.timeoutMinutes", 30L) * 60_000L;
        startMillis = System.currentTimeMillis();
        LogScan.install();
        NeoForge.EVENT_BUS.addListener(AutoCheck::onTick);
        NeoForge.EVENT_BUS.addListener(AutoCheck::onFrame);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, true, AutoCheck::onFog);
        if (!"false".equals(System.getProperty("guhs.autocheck.guardGezin"))) {
            NeoForge.EVENT_BUS.addListener(GezinGuard::onLevelTick);
        }
        LOG.info("AutoCheck active with script {} ({} lines)", script, scriptLines.size());
    }

    // ------------------------------------------------------------------------------------------------ main loop

    /** 'mist off': undo the biome mists (Vadswoud, Kaasmoeras...) so structures can be photographed from afar. */
    private static boolean mistOff;

    private static void onFog(net.neoforged.neoforge.client.event.ViewportEvent.RenderFog event) {
        // (1.1.0: RenderFog has no terrain/sky mode and cannot be cancelled any more; its distances are the terrain fog)
        if (mistOff && event.getType() == net.minecraft.world.level.material.FogType.ATMOSPHERIC) {
            float far = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16f;
            event.setFarPlaneDistance(far);
            event.setNearPlaneDistance(far - Math.max(4, Math.min(64, far / 10)));
        }
    }

    private static int frames;

    private static void onFrame(RenderFrameEvent.Pre event) {
        if (state == State.BOOT && frames++ % 300 == 0) {
            Minecraft mc = Minecraft.getInstance();
            LOG.info("AutoCheck frame {}: loadFinished {}, overlay {}, screen {}", frames, mc.isGameLoadFinished(), mc.getOverlay(), mc.screen);
        }
        applyLock();
    }

    private static void applyLock() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && lockYaw != null) {
            mc.player.setYRot(lockYaw);
            mc.player.yRotO = lockYaw;
            mc.player.setYHeadRot(lockYaw);
            mc.player.yHeadRotO = lockYaw;
            mc.player.setXRot(lockPitch);
            mc.player.xRotO = lockPitch;
        }
    }

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        stateTicks++;
        try {
            tick(mc);
        } catch (Throwable t) {
            problem("AutoCheck crashed in state " + state + " at script line " + scriptIndex + ": " + t);
            LOG.error("AutoCheck error", t);
            current = null;
        }
    }

    private static void tick(Minecraft mc) {
        if (state != State.QUITTING && System.currentTimeMillis() - startMillis > timeoutMillis) {
            problem("TIMEOUT: round took longer than " + timeoutMillis / 60000 + " minutes, stopping at script line " + scriptIndex);
            startQuit(mc);
            return;
        }
        switch (state) {
            case BOOT -> {
                if (stateTicks % 200 == 0) {
                    LOG.info("AutoCheck boot: overlay {}, screen {}", mc.getOverlay(), mc.screen);
                }
                if (mc.getOverlay() == null && mc.screen != null && stateTicks > 40) {
                    setupOptions(mc);
                    outDir = new File(mc.gameDirectory, "screenshots/autocheck");
                    outDir.mkdirs();
                    loadDone();
                    if (done.isEmpty()) {
                        deleteOldShots();
                    }
                    note("AutoCheck start " + new java.util.Date() + ", screen at boot: " + mc.screen.getClass().getSimpleName()
                            + (done.isEmpty() ? " (fresh round)" : " (RESUME: " + done.size()
                            + " items that were OK (done.txt) are skipped; -Dguhs.autocheck.fresh=true checks everything again)"));
                    if (remote()) {
                        note("REMOTE round: joining " + REMOTE + " as " + mc.getUser().getName());
                        var adres = net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(REMOTE);
                        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(new net.minecraft.client.gui.screens.TitleScreen(), mc, adres,
                                new net.minecraft.client.multiplayer.ServerData("autocheck", REMOTE, net.minecraft.client.multiplayer.ServerData.Type.OTHER),
                                false, null);
                    } else {
                        createWorld(mc);
                    }
                    state(State.CREATING);
                }
            }
            case CREATING, JOINING -> {
                if (mc.player != null && mc.level != null && (remote() || mc.getSingleplayerServer() != null)
                        && !(mc.screen instanceof LevelLoadingScreen)) {
                    if (state == State.CREATING) {
                        state(State.JOINING);
                        note("World joined after " + (System.currentTimeMillis() - startMillis) / 1000 + " s");
                    }
                    closeForeignScreen(mc);
                    if (stateTicks > 60) {
                        if (!remote()) {
                            applyRules(mc.getSingleplayerServer());
                        }
                        state(State.RUNNING);
                    }
                } else if (stateTicks > 20 * 180) {
                    problem("World did not load within 3 minutes (screen " + (mc.screen == null ? "none" : mc.screen.getClass().getName()) + ")");
                    startQuit(mc);
                }
            }
            case RUNNING -> {
                if (mc.player == null || !remote() && mc.getSingleplayerServer() == null) {
                    problem("Left the world unexpectedly at script line " + scriptIndex);
                    startQuit(mc);
                    return;
                }
                if (!opened) {
                    closeForeignScreen(mc);
                }
                if (lockYaw != null && mc.screen == null) {
                    mc.mouseHandler.releaseMouse();
                }
                applyLock();
                if (driveForward) {
                    mc.options.keyUp.setDown(true);
                }
                for (net.minecraft.client.KeyMapping k : heldKeys) {
                    k.setDown(true);
                }
                for (int guard = 0; guard < 50; guard++) {
                    if (current == null) {
                        current = next(mc);
                        if (current == null) {
                            return;
                        }
                    }
                    if (current.tick(mc)) {
                        current = null;
                    } else {
                        return;
                    }
                }
            }
            case QUITTING -> {
                if (stopDelay > 0) {
                    stopDelay--;
                } else if (stopDelay == 0) {
                    stopDelay = -1;
                    mc.stop();
                }
            }
        }
    }

    private static void state(State s) {
        state = s;
        stateTicks = 0;
    }

    private static void setupOptions(Minecraft mc) {
        var o = mc.options;
        o.onboardAccessibility = false;
        o.pauseOnLostFocus = false;
        o.skipMultiplayerWarning = true;
        o.hideGui = false;
        o.cloudStatus().set(net.minecraft.client.CloudStatus.OFF);
        o.chunkSectionFadeInTime().set(0.0); // 1.1.0: 26.1 fades new chunk sections in; screenshots want them at once
        o.renderDistance().set(Integer.getInteger("guhs.autocheck.renderDistance", 8));
        o.simulationDistance().set(6);
        o.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
        mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        o.narrator().set(net.minecraft.client.NarratorStatus.OFF);
        o.guiScale().set(Integer.getInteger("guhs.autocheck.guiScale", 2));
        mc.resizeGui();
    }

    /** A screen we didn't open (a mod's welcome screen, the pause menu...) is closed so the round never gets stuck. */
    private static void closeForeignScreen(Minecraft mc) {
        Screen s = mc.screen;
        if (s != null && !(s instanceof LevelLoadingScreen) && !opened) {
            note("closed unexpected screen " + s.getClass().getName() + " (" + s.getTitle().getString() + ")");
            mc.setScreen(null);
        }
    }

    private static void deleteOldShots() {
        File[] old = outDir.listFiles();
        if (old != null) {
            for (File f : old) {
                if (f.getName().endsWith(".png") || f.getName().equals("report.txt")) {
                    f.delete();
                }
            }
        }
    }

    private static void createWorld(Minecraft mc) {
        var source = mc.getLevelSource();
        try {
            if (source.levelExists(WORLD)) {
                try (var access = source.createAccess(WORLD)) {
                    access.deleteLevel();
                }
            }
        } catch (IOException e) {
            problem("Could not delete old world " + WORLD + ": " + e);
        }
        // (1.1.0: LevelSettings has no game rules any more; they are set in applyRules() once the world runs)
        LevelSettings settings = new LevelSettings(WORLD, GameType.CREATIVE, new LevelSettings.DifficultySettings(Difficulty.NORMAL, false, false),
                true, WorldDataConfiguration.DEFAULT);
        WorldOptions options = new WorldOptions(seed, true, false);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, options, WorldPresets::createNormalWorldDimensions,
                new net.minecraft.client.gui.screens.TitleScreen());
    }

    /** The game rules of the check world (1.0.0 put them in LevelSettings). */
    private static void applyRules(net.minecraft.server.MinecraftServer server) {
        server.execute(() -> {
            GameRules rules = server.getGameRules();
            rules.set(GameRules.ADVANCE_TIME, false, server);
            rules.set(GameRules.ADVANCE_WEATHER, false, server);
            rules.set(GameRules.SPAWN_MOBS, false, server);
            rules.set(GameRules.SHOW_ADVANCEMENT_MESSAGES, false, server);
            rules.set(GameRules.SEND_COMMAND_FEEDBACK, false, server);
            rules.set(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0, server);
            rules.set(GameRules.MOB_GRIEFING, false, server);
            rules.set(GameRules.SPAWN_PATROLS, false, server);
            rules.set(GameRules.SPAWN_WANDERING_TRADERS, false, server);
        });
    }

    // ------------------------------------------------------------------------------------------------ script

    /** An action ticks until it returns true (done). */
    @FunctionalInterface
    interface Action {
        boolean tick(Minecraft mc);
    }

    private static Action next(Minecraft mc) {
        if (!queue.isEmpty()) {
            return queue.pollFirst();
        }
        while (scriptIndex < scriptLines.size()) {
            String line = scriptLines.get(scriptIndex++).trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            try {
                Action a = parse(mc, line);
                if (a != null) {
                    return a;
                }
            } catch (Exception e) {
                problem("script line " + scriptIndex + " '" + line + "': " + e);
            }
            if (!queue.isEmpty()) {
                return queue.pollFirst();
            }
        }
        startQuit(mc);
        return null;
    }

    private static Action parse(Minecraft mc, String line) {
        if (line.startsWith("/")) {
            return command(line.substring(1));
        }
        String[] a = line.split("\\s+");
        String rest = line.substring(a[0].length()).trim();
        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "cmd":
                return command(rest.startsWith("/") ? rest.substring(1) : rest);
            case "wait":
                return waitTicks(Integer.parseInt(a[1]));
            case "waitrender":
                return waitRender(a.length > 1 ? Integer.parseInt(a[1]) : 400);
            case "look":
                return mc2 -> {
                    lockYaw = Float.parseFloat(a[1]);
                    lockPitch = Float.parseFloat(a[2]);
                    return true;
                };
            case "unlock":
                return mc2 -> {
                    lockYaw = null;
                    lockPitch = null;
                    return true;
                };
            case "shot":
                return shot(a[1], a.length > 2 && a[2].equalsIgnoreCase("gui"));
            case "log":
                return mc2 -> {
                    note(rest);
                    return true;
                };
            case "section":
                return mc2 -> {
                    note("");
                    note("=== " + rest + " ===  (" + (System.currentTimeMillis() - startMillis) / 1000 + " s)");
                    return true;
                };
            case "gui":
                return gui(a);
            case "tplocate":
                expandTpLocate(mc, a[1], a[2]);
                return null;
            case "tpbiome":
                expandTpBiome(mc, a[1], a[2]);
                return null;
            case "lineup":
                expandLineup(mc, a[1], BlockPos.containing(Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4])));
                return null;
            case "blockwall":
                expandBlockWall(mc, BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])));
                return null;
            case "use":
                return useBlock(BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])));
            case "camera":
                // camera <x> <y> <z> <yaw> <pitch>: teleport (same dimension) and lock the view
                return teleport(null, new Vec3(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])),
                        Float.parseFloat(a[4]), Float.parseFloat(a[5]));
            case "mist":
                return mc2 -> {
                    mistOff = a[1].equalsIgnoreCase("off");
                    note("  mist " + (mistOff ? "off (biome fog overridden)" : "on"));
                    return true;
                };
            case "perspective":
                // perspective first|back|front: the camera view (2.8.1: to see the player's own shoulder)
                return mc2 -> {
                    mc2.options.setCameraType(switch (a[1].toLowerCase(Locale.ROOT)) {
                        case "back" -> net.minecraft.client.CameraType.THIRD_PERSON_BACK;
                        case "front" -> net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
                        default -> net.minecraft.client.CameraType.FIRST_PERSON;
                    });
                    return true;
                };
            case "modelcheck":
                return mc2 -> {
                    modelCheck(mc2);
                    return true;
                };
            // --- 2.9 visual QA: drive screens that the server opens (entity menus) and click/hover inside them ---
            case "opnieuw":
                // opnieuw: (3.0 QA) forget what is OK so far this round, so a structure can be located again (for camtpl after other ones)
                return mc2 -> {
                    done.clear();
                    return true;
                };
            case "camtpl":
            case "looktpl": {
                // camtpl <dx> <dy> <dz> <yaw> <pitch> / looktpl <dx> <dy> <dz> <tx> <ty> <tz>: like camrel / lookrel, but
                // relative to the start piece (template coordinates, also for structures with terrain adaptation)
                boolean kijk = a[0].equalsIgnoreCase("looktpl");
                return mc2 -> {
                    if (lastTpl == null) {
                        problem(a[0] + ": no structure located yet");
                        return true;
                    }
                    java.util.function.Function<Vec3, Vec3> naarWereld = t -> lastTplPos == null
                            ? t.add(lastTpl.minX(), lastTpl.minY(), lastTpl.minZ())
                            : net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate.transform(t, net.minecraft.world.level.block.Mirror.NONE,
                            lastTplRot, BlockPos.ZERO).add(lastTplPos.getX(), lastTplPos.getY(), lastTplPos.getZ());
                    Vec3 cam = naarWereld.apply(new Vec3(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])));
                    float draai = lastTplRot == null ? 0 : switch (lastTplRot) {
                        case CLOCKWISE_90 -> 90;
                        case CLOCKWISE_180 -> 180;
                        case COUNTERCLOCKWISE_90 -> -90;
                        default -> 0;
                    };
                    float[] r = kijk ? lookAt(cam, naarWereld.apply(new Vec3(Double.parseDouble(a[4]), Double.parseDouble(a[5]), Double.parseDouble(a[6]))))
                            : new float[]{Float.parseFloat(a[4]) + draai, Float.parseFloat(a[5])};
                    queue.addFirst(teleport(lastDim, cam, r[0], r[1]));
                    return true;
                };
            }
            case "camrel":
                // camrel <dx> <dy> <dz> <yaw> <pitch>: camera relative to the min corner of the last tplocate'd structure
                return mc2 -> {
                    if (lastBox == null) {
                        problem("camrel: no structure located yet");
                        return true;
                    }
                    queue.addFirst(teleport(lastDim, new Vec3(lastBox.minX() + Double.parseDouble(a[1]), lastBox.minY() + Double.parseDouble(a[2]),
                            lastBox.minZ() + Double.parseDouble(a[3])), Float.parseFloat(a[4]), Float.parseFloat(a[5])));
                    return true;
                };
            case "lookrel":
                // lookrel <dx> <dy> <dz> <tx> <ty> <tz>: camera at a point relative to the last structure, looking at another one
                return mc2 -> {
                    if (lastBox == null) {
                        problem("lookrel: no structure located yet");
                        return true;
                    }
                    Vec3 o = new Vec3(lastBox.minX(), lastBox.minY(), lastBox.minZ());
                    Vec3 cam = o.add(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]));
                    Vec3 tgt = o.add(Double.parseDouble(a[4]), Double.parseDouble(a[5]), Double.parseDouble(a[6]));
                    float[] r = lookAt(cam, tgt);
                    queue.addFirst(teleport(lastDim, cam, r[0], r[1]));
                    return true;
                };
            case "relcmd":
                // relcmd <command with {x:n} {y:n} {z:n}>: a server command with coordinates relative to the last structure's min corner
                return mc2 -> {
                    if (lastBox == null) {
                        problem("relcmd: no structure located yet");
                        return true;
                    }
                    var m = java.util.regex.Pattern.compile("[{]([xyz]):(-?[0-9.]+)[}]").matcher(rest);
                    StringBuilder sb = new StringBuilder();
                    while (m.find()) {
                        double base = switch (m.group(1)) {
                            case "x" -> lastBox.minX();
                            case "y" -> lastBox.minY();
                            default -> lastBox.minZ();
                        };
                        double v = base + Double.parseDouble(m.group(2));
                        m.appendReplacement(sb, v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v));
                    }
                    m.appendTail(sb);
                    String c = sb.toString();
                    String dimId = lastDim.identifier().toString();
                    queue.addFirst(command("execute in " + dimId + " run " + (c.startsWith("/") ? c.substring(1) : c)));
                    return true;
                };
            case "emote":
                // emote <x> <y> <z> <radius> <EMOTE>: every guh in that radius starts that emote in a loop (e.g. SLAPEN: sleeping eyes)
                return server(server -> {
                    ServerPlayer sp = player(server);
                    var c = new Vec3(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]));
                    double r = Double.parseDouble(a[4]);
                    var emote = nl.juiced.guhs.feature.emotes.Emote.valueOf(a[5].toUpperCase(Locale.ROOT));
                    int n = 0;
                    for (var g : sp.level().getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class, new net.minecraft.world.phys.AABB(c, c).inflate(r))) {
                        // (emotes stop on NoAI guhs: give it its AI back, it's standing on the platform)
                        g.setNoAi(false);
                        g.setOnGround(true);
                        if (g.emotes.start(emote, true, nl.juiced.guhs.feature.emotes.GuhEmotes.Source.SELF)) {
                            n++;
                        }
                    }
                    return n;
                }, n -> note("  emote " + a[5] + " started on " + n + " guhs"));
            case "npc":
                // npc <kind> [distance] [height]: go to the nearest guh_npc of that kind (loaded, within 400 blocks) and look at its face
                return goToNpc(a[1].toLowerCase(Locale.ROOT), a.length > 2 ? Double.parseDouble(a[2]) : 2.6, a.length > 3 ? Double.parseDouble(a[3]) : 0.4);
            case "interact":
                if (a.length > 1 && a[1].equals("npc")) {
                    return interactNpc();
                }
                // interact <x> <y> <z> [entity type]: right-click the nearest entity (as the player, empty hand unless /item gave one)
                return interactEntity(BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])),
                        a.length > 4 ? a[4] : null, "interact");
            case "wardrobe":
                // wardrobe <x> <y> <z>: tame the nearest guh to the player and open its wardrobe (like the Kast button)
                return interactEntity(BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])),
                        "guhs:guh", "wardrobe");
            case "guhmenu":
                // guhmenu <x> <y> <z>: tame the nearest guh and open its menu (GuhScreen, like holding right-click)
                return interactEntity(BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])),
                        "guhs:guh", "guhmenu");
            case "aai":
                // aai <x> <y> <z>: (1.2.0) tame the nearest guh and pet it (a short tap: the squish, hearts and "Je aait ...!")
                return interactEntityJob(BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])),
                        "guhs:guh", "aai");
            case "piepmenu":
                // piepmenu <x> <y> <z>: tame the nearest piep-maatje and open its menu
                return interactEntity(BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])),
                        null, "piepmenu");
            case "weerder":
                // weerder <x> <y> <z> [straal]: (1.2.0) a Wilde-guhweerder there (facing south) of the player, its screen open and
                // its blue dome on
                return weerder(BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])),
                        a.length > 4 ? Integer.parseInt(a[4]) : nl.juiced.guhs.feature.weerder.WeerderFeature.STANDAARD);
            case "huisje":
                // huisje <x> <y> <z>: (2.10) a groot Guhhuisje there (door to the south) with three residents (two guhs and a
                // muisje), its screen open and its blue dome on
                return huisje(BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])));
            case "verhaalguh":
                // verhaalguh <baltoguh|mewtwo|stitch626> <x> <y> <z>: (3.0) a tamed story guh of the player there, doing its special
                return verhaalguh(a[1], BlockPos.containing(Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4])));
            case "wolkjes":
                // wolkjes: (3.0) a tamed example guh "Wolkje" goes to the wolkjes; the Guhdex opens on its page in Mijn guhs
                return wolkjes();
            case "huisjevan":
                // huisjevan <naam> <x> <y> <z>: (3.0) a klein Guhhuisje of someone else (owner name <naam>) there, door to the south
                return huisjeVan(a[1], BlockPos.containing(Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4])));
            case "huisjekijk":
                // huisjekijk <x> <y> <z>: (3.0 timmerguh) open the screen of the huisje there as someone who may only look (the
                // AutoCheck player is an op and could change anything, so the screen gets MagBewerken false: grey buttons)
                return huisjeKijk(BlockPos.containing(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3])));
            case "ontgrendel":
                // ontgrendel all | ontgrendel <id> [id...]: unlock clothing pieces for the player (the wardrobe lists unlocks only)
                return unlockClothes(java.util.Arrays.copyOfRange(a, 1, a.length));
            case "mouse":
                // mouse <guiX> <guiY>: move the cursor (gui coordinates) for hover tooltips
                return mc2 -> {
                    double s = mc2.getWindow().getGuiScale();
                    setCursor(mc2, Double.parseDouble(a[1]) * s, Double.parseDouble(a[2]) * s);
                    return true;
                };
            case "click":
                // click <guiX> <guiY> [button]: a mouse click on the open screen
                return mc2 -> {
                    if (mc2.screen == null) {
                        problem("click: no screen open");
                        return true;
                    }
                    double x = Double.parseDouble(a[1]), y = Double.parseDouble(a[2]);
                    int b = a.length > 3 ? Integer.parseInt(a[3]) : 0;
                    var ev = new net.minecraft.client.input.MouseButtonEvent(x, y, new net.minecraft.client.input.MouseButtonInfo(b, 0));
                    mc2.screen.mouseClicked(ev, false);
                    mc2.screen.mouseReleased(ev);
                    return true;
                };
            case "drag":
                // drag <x1> <y1> <x2> <y2>: press, drag and release on the open screen (e.g. turning a 3D preview)
                return mc2 -> {
                    if (mc2.screen != null) {
                        double x1 = Double.parseDouble(a[1]), y1 = Double.parseDouble(a[2]), x2 = Double.parseDouble(a[3]), y2 = Double.parseDouble(a[4]);
                        var down = new net.minecraft.client.input.MouseButtonEvent(x1, y1, new net.minecraft.client.input.MouseButtonInfo(0, 0));
                        var up = new net.minecraft.client.input.MouseButtonEvent(x2, y2, new net.minecraft.client.input.MouseButtonInfo(0, 0));
                        mc2.screen.mouseClicked(down, false);
                        mc2.screen.mouseDragged(up, x2 - x1, y2 - y1);
                        mc2.screen.mouseReleased(up);
                    }
                    return true;
                };
            case "scroll":
                // scroll <guiX> <guiY> <amount>: mouse wheel on the open screen (negative = down)
                return mc2 -> {
                    if (mc2.screen != null) {
                        mc2.screen.mouseScrolled(Double.parseDouble(a[1]), Double.parseDouble(a[2]), 0, Double.parseDouble(a[3]));
                    }
                    return true;
                };
            case "type":
                // type <text>: type into the focused widget of the open screen
                return mc2 -> {
                    if (mc2.screen != null) {
                        for (char ch : rest.toCharArray()) {
                            mc2.screen.charTyped(new net.minecraft.client.input.CharacterEvent(ch));
                        }
                    }
                    return true;
                };
            case "press":
                // press <button label>: press a button of the open screen by its text
                return mc2 -> {
                    pressButton(mc2.screen, rest);
                    return true;
                };
            case "useitem":
                // useitem <ticks>: hold right-click with the held item (eating, consuming a clothing piece...)
                return holdUse(Integer.parseInt(a[1]));
            case "widgets":
                // widgets: list the open screen's widgets (text + bounds) in the report and flag overlapping visible ones
                return mc2 -> {
                    dumpWidgets(mc2);
                    return true;
                };
            // --- 2.10 visual QA ---
            case "samenspel":
                // samenspel <goed|mis|record> <spel>: your own band guhs close by react as if you just played that minigame
                return server(server -> {
                    ServerPlayer sp = player(server);
                    return switch (a[1].toLowerCase(Locale.ROOT)) {
                        case "mis" -> nl.juiced.guhs.feature.samen.SamenSpel.mis(sp, a[2]);
                        case "record" -> nl.juiced.guhs.feature.samen.SamenSpel.record(sp, a[2]);
                        default -> nl.juiced.guhs.feature.samen.SamenSpel.goed(sp, a[2]);
                    };
                }, n -> note("  samenspel " + a[1] + " " + a[2] + ": " + n + " guhs react"));
            case "verhalenstand":
                // verhalenstand: puts the player in the middle of several questlines (for the Guhdex tab Verhalen)
                return server(server -> {
                    ServerPlayer sp = player(server);
                    nl.juiced.guhs.feature.timmerguh.TimmerguhVoortgang.zet(sp, nl.juiced.guhs.feature.timmerguh.TimmerguhVoortgang.MATERIAAL);
                    sp.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS, 9));
                    nl.juiced.guhs.feature.balto.BaltoVerhaal.zet(sp, nl.juiced.guhs.feature.balto.BaltoVerhaal.BIJ_ROSY);
                    nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang.zetStap(sp, nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang.NOTITIES);
                    nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang.vondNotitie(sp, 1);
                    nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang.vondNotitie(sp, 2);
                    nl.juiced.guhs.feature.guhwaii.Ohana.zet(sp, nl.juiced.guhs.feature.guhwaii.Ohana.LIEF);
                    nl.juiced.guhs.quest.GuhQuests.saved(sp).putInt(nl.juiced.guhs.feature.guhwaii.Ohana.GAVEN, nl.juiced.guhs.feature.guhwaii.Ohana.GAVE_KOKOS);
                    nl.juiced.guhs.feature.hemel.HemelQuest.wakker(sp);
                    nl.juiced.guhs.world.GuhWorldData.get(server).player(sp.getUUID()).sledQuest = 1;
                    nl.juiced.guhs.feature.knus.Knusfeest.nieuweRonde(sp, 0, java.util.EnumSet.allOf(nl.juiced.guhs.feature.knus.Feesttaak.class));
                    nl.juiced.guhs.feature.knus.Knusfeest.zet(sp, nl.juiced.guhs.feature.knus.Feesttaak.FEESTTAART, nl.juiced.guhs.feature.knus.Knusfeest.Stap.GEBRACHT);
                    nl.juiced.guhs.feature.beroepen.BeroepenVoortgang.rondAf(sp, nl.juiced.guhs.feature.beroepen.BeroepenVoortgang.Beroep.POLITIE, null);
                    return nl.juiced.guhs.feature.gids.VerhalenVoortgang.alle(sp).size();
                }, n -> note("  verhalenstand: " + n + " questlines"));
            case "titelsstand":
                // titelsstand [kies <id>|geen|wis]: gives the player four titles (Held van Nomguh, Knuffelburgemeester, Vriend
                // van Guhtwo, Opper-vadser), or picks one / none, or takes them all away (for the Guhdex tab Titels)
                return server(server -> {
                    ServerPlayer sp = player(server);
                    var saved = nl.juiced.guhs.quest.GuhQuests.saved(sp);
                    boolean wis = a.length > 1 && a[1].equals("wis");
                    if (a.length > 2 && a[1].equals("kies")) {
                        nl.juiced.guhs.feature.titels.Titels.kies(sp, a[2]);
                    } else if (a.length > 1 && a[1].equals("geen")) {
                        nl.juiced.guhs.feature.titels.Titels.kies(sp, nl.juiced.guhs.feature.titels.Titels.GEEN);
                    } else {
                        saved.putBoolean(nl.juiced.guhs.feature.balto.BaltoVerhaal.HELD, !wis);
                        saved.putBoolean(nl.juiced.guhs.feature.knuffeldal.Feestbuffet.TITEL, !wis);
                        saved.putInt(nl.juiced.guhs.feature.guheinde.GuheindeGevecht.WINS, wis ? 0 : 1);
                        nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang.zetStap(sp, wis ? 0 : nl.juiced.guhs.feature.mewtwo.MewtwoVoortgang.KLAAR);
                        saved.remove(nl.juiced.guhs.feature.titels.Titels.KEUZE);
                        nl.juiced.guhs.feature.titels.Titels.kijk(sp);
                    }
                    nl.juiced.guhs.feature.titels.TitelsPayloads.sync(sp);
                    var t = nl.juiced.guhs.feature.titels.Titels.actief(sp);
                    return nl.juiced.guhs.feature.titels.Titels.behaald(sp).size() + " titles, showing " + (t == null ? "none" : t.id())
                            + ", display name \"" + sp.getDisplayName().getString() + "\"";
                }, n -> note("  titelsstand: " + n));
            case "doolhofstart":
                // doolhofstart <makkelijk|medium|lastig>: start a doolhof game at the nearest doolhofguh (within 400 blocks)
                return server(server -> {
                    ServerPlayer sp = player(server);
                    for (Entity e : sp.level().getEntities((Entity) null, sp.getBoundingBox().inflate(400, 200, 400),
                            e -> e instanceof nl.juiced.guhs.entity.GuhNpcEntity n && n.getKind().id().equals("doolhofguh"))) {
                        var game = nl.juiced.guhs.feature.doolhof.DoolhofGame.start((nl.juiced.guhs.entity.GuhNpcEntity) e, sp,
                                nl.juiced.guhs.feature.spelen.Niveau.valueOf(a[1].toUpperCase(Locale.ROOT)));
                        return game != null;
                    }
                    return false;
                }, ok -> {
                    if (!ok) {
                        problem("doolhofstart: no doolhofguh or the game didn't start");
                    } else {
                        note("  doolhof " + a[1] + " started");
                    }
                });
            case "kart":
                // kart <x> <y> <z> <yaw>: a race guh there, the player steering, the nearest own band guh on the second seat
                return server(server -> {
                    ServerPlayer sp = player(server);
                    ServerLevel level = sp.level();
                    var race = nl.juiced.guhs.feature.race.RaceFeature.RACE_GUH.get().create(level, EntitySpawnReason.TRIGGERED);
                    if (race == null) {
                        return "no race guh";
                    }
                    float yaw = Float.parseFloat(a[4]);
                    race.snapTo(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]), yaw, 0);
                    race.setYBodyRot(yaw);
                    race.setYHeadRot(yaw);
                    level.addFreshEntity(race);
                    sp.startRiding(race, true, true);
                    var guhs = nl.juiced.guhs.feature.band.Band.samenGuhs(sp, 24);
                    if (guhs.isEmpty()) {
                        return "kart: no own guh close by";
                    }
                    guhs.sort(java.util.Comparator.comparingDouble(g -> g.distanceToSqr(sp)));
                    boolean mee = guhs.get(0).startRiding(race, true, true);
                    try {
                        // (as if a race had put it there: otherwise a guh left in a kart without a race hops off by itself)
                        var m = nl.juiced.guhs.feature.samen.SamenMee.class.getDeclaredMethod("testKart", ServerPlayer.class,
                                nl.juiced.guhs.entity.GuhEntity.class);
                        m.setAccessible(true);
                        m.invoke(null, sp, guhs.get(0));
                    } catch (ReflectiveOperationException e) {
                        problem("kart: SamenMee.testKart not reachable: " + e);
                    }
                    return "kart: player " + (sp.getVehicle() == race) + ", guh " + mee + " (" + race.getPassengers().size() + " passengers)";
                }, r -> note("  " + r));
            case "drive":
                // drive on|off: (2.10.1) hold W (the race guh runs) until drive off
                return mc2 -> {
                    driveForward = a[1].equalsIgnoreCase("on");
                    mc2.options.keyUp.setDown(driveForward);
                    return true;
                };
            case "key":
                // key <left|right|up|down|jump|sneak|sprint> on|off: (bbq2) hold a movement key until off;  key all off
                return mc2 -> {
                    var o = mc2.options;
                    if (a[1].equalsIgnoreCase("all")) {
                        heldKeys.forEach(k -> k.setDown(false));
                        heldKeys.clear();
                        return true;
                    }
                    net.minecraft.client.KeyMapping k = switch (a[1].toLowerCase(Locale.ROOT)) {
                        case "left" -> o.keyLeft;
                        case "right" -> o.keyRight;
                        case "up" -> o.keyUp;
                        case "down" -> o.keyDown;
                        case "jump" -> o.keyJump;
                        case "sneak" -> o.keyShift;
                        case "sprint" -> o.keySprint;
                        default -> null;
                    };
                    if (k == null) {
                        problem("key: unknown key " + a[1]);
                        return true;
                    }
                    boolean on = a.length < 3 || a[2].equalsIgnoreCase("on");
                    if (on) {
                        heldKeys.add(k);
                    } else {
                        heldKeys.remove(k);
                    }
                    k.setDown(on);
                    return true;
                };
            case "guhriowacht": {
                // guhriowacht <s> [maxTicks]: (bbq2) wait until the player is at least s blocks along the lane (or, with a
                // minus sign, at most that far); gives up after maxTicks (default 200)
                double doel = Double.parseDouble(a[1]);
                int max = a.length > 2 ? Integer.parseInt(a[2]) : 200;
                int[] t = {0};
                return mc2 -> {
                    double nu = nl.juiced.guhs.feature.guhrio.client.GuhrioClient.devS();
                    boolean daar = doel >= 0 ? nu >= doel : nu <= -doel;
                    if (!daar && ++t[0] > max) {
                        problem("guhriowacht " + a[1] + ": not there after " + max + " ticks (s = " + nu + ")");
                        return true;
                    }
                    return daar;
                };
            }
            case "guhriosprong": {
                // guhriosprong <ticks> [label]: (bbq2) hold space for that many ticks and note how high and far the jump went
                int hold = Integer.parseInt(a[1]);
                int[] t = {0};
                double[] m = new double[4];
                return mc2 -> {
                    var p = mc2.player;
                    if (t[0] == 0) {
                        m[0] = p.getY();
                        m[1] = p.getY();
                        m[2] = p.getX();
                        m[3] = p.getZ();
                    }
                    mc2.options.keyJump.setDown(t[0] < hold);
                    m[1] = Math.max(m[1], p.getY());
                    t[0]++;
                    if (t[0] > 3 && (p.onGround() || t[0] > 80)) {
                        mc2.options.keyJump.setDown(false);
                        note("  guhriosprong " + rest + ": " + String.format(Locale.ROOT, "%.2f blocks high, %.2f blocks far, %d ticks in the air",
                                m[1] - m[0], Math.abs(p.getX() - m[2]) + Math.abs(p.getZ() - m[3]), t[0] - 1));
                        return true;
                    }
                    return false;
                };
            }
            case "guhriopos":
                // guhriopos [label]: (bbq2) where the player and the camera are, and what the player's game knows of the level
                return mc2 -> {
                    var cam = mc2.gameRenderer.getMainCamera();
                    var p = mc2.player;
                    note("  guhriopos " + rest + ": " + String.format(Locale.ROOT, "player %.2f %.2f %.2f yaw %.0f ground %s vel %.3f %.3f %.3f | camera %.2f %.2f %.2f yaw %.1f pitch %.1f | ",
                            p.getX(), p.getY(), p.getZ(), p.getYRot(), p.onGround(), p.getDeltaMovement().x, p.getDeltaMovement().y, p.getDeltaMovement().z,
                            cam.position().x, cam.position().y, cam.position().z, cam.yRot(), cam.xRot())
                            + nl.juiced.guhs.feature.guhrio.client.GuhrioClient.devInfo());
                    return true;
                };
            case "racekart":
                // racekart <x> <y> <z> <yaw> <makkelijk|medium|lastig> [nosprong]: (2.10.1) a race guh set up as in a race on
                // the Regenboogbaan (size, step height, level, the rainbow jump unless nosprong), the player steering
                return server(server -> {
                    ServerPlayer sp = player(server);
                    ServerLevel level = sp.level();
                    var race = nl.juiced.guhs.feature.race.RaceFeature.RACE_GUH.get().create(level, EntitySpawnReason.TRIGGERED);
                    if (race == null) {
                        return "no race guh";
                    }
                    float yaw = Float.parseFloat(a[4]);
                    race.setUpForRace();
                    race.setNiveau(nl.juiced.guhs.feature.spelen.Niveau.valueOf(a[5].toUpperCase(Locale.ROOT)));
                    race.setSprongen(!(a.length > 6 && a[6].equalsIgnoreCase("nosprong")));
                    race.snapTo(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]), yaw, 0);
                    race.setYBodyRot(yaw);
                    race.setYHeadRot(yaw);
                    level.addFreshEntity(race);
                    sp.startRiding(race, true, true);
                    return "racekart " + a[5] + " sprongen " + race.sprongen() + ": riding " + (sp.getVehicle() == race);
                }, r -> note("  " + r));
            case "kartpos":
                // kartpos [label]: (2.10.1) where the player's vehicle is (the rider's game: position, on the ground, gliding)
                return mc2 -> {
                    Entity v = mc2.player.getVehicle();
                    note("  kartpos " + rest + ": " + (v == null ? "not riding" : String.format(Locale.ROOT, "%.2f %.2f %.2f ground %s%s", v.getX(), v.getY(),
                            v.getZ(), v.onGround(), v instanceof nl.juiced.guhs.feature.race.RaceGuhEntity r ? " zweeft " + r.zweeft() + " speed "
                            + String.format(Locale.ROOT, "%.3f", r.getRaceSpeed()) : "")));
                    return true;
                };
            case "renderdistance":
                // renderdistance <chunks>: (2.10) a longer view for overviews of big buildings (the default is 8)
                return mc2 -> {
                    mc2.options.renderDistance().set(Integer.parseInt(a[1]));
                    note("  render distance " + a[1]);
                    return true;
                };
            case "kartinfo":
                // kartinfo: where the riders of the player's race guh are, relative to it (server side)
                return server(server -> {
                    ServerPlayer sp = player(server);
                    Entity v = sp.getVehicle();
                    if (v == null) {
                        return "kartinfo: not riding";
                    }
                    StringBuilder sb = new StringBuilder("kartinfo: vehicle yaw " + v.getYRot() + " body "
                            + (v instanceof LivingEntity l ? l.yBodyRot : 0f));
                    for (Entity e : v.getPassengers()) {
                        Vec3 d = e.position().subtract(v.position());
                        sb.append(String.format(Locale.ROOT, " | %s at %.2f %.2f %.2f yaw %.0f", e.getType().toShortString(), d.x, d.y, d.z, e.getYRot()));
                    }
                    return sb.toString();
                }, r -> note("  " + r));
            case "neartag":
                // neartag <tag> <distance> <height>: camera that far south of (and that high above) the nearest entity with
                // that tag (within 200 blocks), looking at it
                return mc2 -> {
                    queue.addFirst(server(server -> {
                        ServerPlayer sp = player(server);
                        Entity best = null;
                        for (Entity e : sp.level().getEntities((Entity) null, sp.getBoundingBox().inflate(200, 100, 200),
                                e -> e.entityTags().contains(a[1]))) {
                            if (best == null || e.distanceToSqr(sp) < best.distanceToSqr(sp)) {
                                best = e;
                            }
                        }
                        return best == null ? null : best.position();
                    }, at -> {
                        if (at == null) {
                            problem("neartag " + a[1] + ": none within 200 blocks");
                            return;
                        }
                        Vec3 cam = at.add(0, Double.parseDouble(a[3]), Double.parseDouble(a[2]));
                        float[] r = lookAt(cam.add(0, 1.62, 0), at);
                        note(String.format(Locale.ROOT, "  neartag %s at %.1f %.1f %.1f", a[1], at.x, at.y, at.z));
                        queue.addFirst(teleport(null, cam, r[0], r[1]));
                    }));
                    return true;
                };
            case "speelaltijd":
                // speelaltijd <x> <y> <z> <r>: the tamed guhs there look for a toy right away (as in the speelgoed gametests)
                return server(server -> {
                    ServerPlayer sp = player(server);
                    var c = new Vec3(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Double.parseDouble(a[3]));
                    int n = 0;
                    for (var g : sp.level().getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class,
                            new net.minecraft.world.phys.AABB(c, c).inflate(Double.parseDouble(a[4])))) {
                        nl.juiced.guhs.feature.speelgoed.SpeelGoal.TEST_ALTIJD.add(g.getUUID());
                        n++;
                    }
                    return n;
                }, n -> note("  speelaltijd: " + n + " guhs"));
            case "kijknaar":
                // kijknaar <tag>: stay put, but look at the nearest entity with that tag (within 200 blocks)
                return server(server -> {
                    ServerPlayer sp = player(server);
                    Entity best = null;
                    for (Entity e : sp.level().getEntities((Entity) null, sp.getBoundingBox().inflate(200, 100, 200),
                            e -> e.entityTags().contains(a[1]))) {
                        if (best == null || e.distanceToSqr(sp) < best.distanceToSqr(sp)) {
                            best = e;
                        }
                    }
                    return best == null ? null : new Vec3[] {sp.getEyePosition(), best.position().add(0, 0.2, 0)};
                }, r -> {
                    if (r == null) {
                        problem("kijknaar " + a[1] + ": none within 200 blocks");
                        return;
                    }
                    float[] rot = lookAt(r[0], r[1]);
                    lockYaw = rot[0];
                    lockPitch = rot[1];
                    note(String.format(Locale.ROOT, "  kijknaar %s: %.1f blocks away", a[1], r[0].distanceTo(r[1])));
                });
            case "titelnaam":
                // titelnaam: notes the name the CLIENT shows above this player's head (PlayerEvent.NameFormat with the server's
                // guhs:titels_actief list; other players' clients compute the same)
                return mc2 -> {
                    note("  titelnaam (client, above the head): \"" + (mc2.player == null ? "?" : mc2.player.getDisplayName().getString()) + "\"");
                    return true;
                };
            case "clearchat":
                // clearchat: empty the chat (for clean screenshots with the HUD)
                return mc2 -> {
                    mc2.gui.getChat().clearMessages(false);
                    return true;
                };
            case "guhkamer":
                // guhkamer stuur [n] | maag | binnen: send n own guhs close by to the Guhkamer / go to your own Guhmaag /
                // into your own Guhkamer (built first when needed)
                return server(server -> {
                    ServerPlayer sp = player(server);
                    switch (a[1].toLowerCase(Locale.ROOT)) {
                        case "stuur" -> {
                            nl.juiced.guhs.world.MaagManager.ensureMaag(nl.juiced.guhs.world.MaagManager.level(server), sp);
                            int n = a.length > 2 ? Integer.parseInt(a[2]) : 2;
                            StringBuilder sb = new StringBuilder("guhkamer stuur:");
                            for (var g : nl.juiced.guhs.feature.band.Band.samenGuhs(sp, 24)) {
                                if (n-- <= 0) {
                                    break;
                                }
                                sb.append(' ').append(nl.juiced.guhs.feature.guhkamer.Guhkamer.stuur(sp, g));
                            }
                            return sb.toString();
                        }
                        case "maag" -> {
                            nl.juiced.guhs.world.MaagManager.goToOwnMaag(sp);
                            return "guhkamer: in the own Guhmaag";
                        }
                        default -> {
                            return "guhkamer binnen: " + nl.juiced.guhs.feature.guhkamer.Guhkamer.gaNaarBinnen(sp, sp.getUUID());
                        }
                    }
                }, r -> note("  " + r));
            // --- 1.2.8 visual QA: the huisje screen's "Wat kan hier?" dialog ---
            case "huisjeoverzicht":
                // huisjeoverzicht: open the "Wat kan hier?" dialog on the open huisje screen (like its "?" button)
                return mc2 -> {
                    if (mc2.screen instanceof nl.juiced.guhs.feature.huisje.client.HuisjeScreen s) {
                        s.openOverzicht();
                        if (!s.overzichtOpen()) {
                            problem("huisjeoverzicht: the dialog did not open");
                        }
                    } else {
                        problem("huisjeoverzicht: no huisje screen open");
                    }
                    return true;
                };
            case "guhstaal":
                // guhstaal <nl|en|auto>: the Guhs language switch, live (the open screen must follow at once)
                return mc2 -> {
                    nl.juiced.guhs.client.GuhsTaal.set(nl.juiced.guhs.taal.Taal.valueOf(a[1].toUpperCase(Locale.ROOT)));
                    return true;
                };
            case "guiscale":
                // guiscale <n>: the GUI scale (the open screen is laid out again)
                return mc2 -> {
                    mc2.options.guiScale().set(Integer.parseInt(a[1]));
                    mc2.resizeGui();
                    return true;
                };
            case "seed":
                return mc2 -> true; // (handled before world creation: see guhs.autocheck.seed)
            // --- 1.4.0 two-player check: keeping step with another client, a real right-click, what this client was sent ---
            case "signal":
                // signal <name>: tell the other client(s) that this point of the script was reached
                return mc2 -> {
                    try {
                        Files.writeString(syncFile(a[1]).toPath(), mc2.getUser().getName(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        problem("signal " + a[1] + ": " + e);
                    }
                    note("  signal " + a[1]);
                    return true;
                };
            case "await": {
                // await <name> [maxTicks]: wait until another client gave that signal (default: at most 6000 ticks)
                int max = a.length > 2 ? Integer.parseInt(a[2]) : 6000;
                int[] t = {0};
                return mc2 -> {
                    if (syncFile(a[1]).exists()) {
                        note("  await " + a[1] + ": after " + t[0] + " ticks");
                        return true;
                    }
                    if (++t[0] > max) {
                        problem("await " + a[1] + ": no signal after " + max + " ticks");
                        return true;
                    }
                    return false;
                };
            }
            case "klik": {
                // klik <entity type id|*> [radius] [name part]: a real right-click, sent by this client, on the nearest entity
                // of that type the CLIENT knows (default within 8 blocks; also works on a server)
                double r = a.length > 2 ? Double.parseDouble(a[2]) : 8;
                String naam = a.length > 3 ? String.join(" ", java.util.Arrays.copyOfRange(a, 3, a.length)).toLowerCase(Locale.ROOT) : null;
                return mc2 -> {
                    Entity best = null;
                    for (Entity e : mc2.level.entitiesForRendering()) {
                        if (e == mc2.player || e.distanceTo(mc2.player) > r) {
                            continue;
                        }
                        if (!a[1].equals("*") && !BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(a[1].contains(":") ? a[1] : "guhs:" + a[1])) {
                            continue;
                        }
                        if (naam != null && !e.getName().getString().toLowerCase(Locale.ROOT).contains(naam)) {
                            continue;
                        }
                        if (best == null || e.distanceToSqr(mc2.player) < best.distanceToSqr(mc2.player)) {
                            best = e;
                        }
                    }
                    if (best == null) {
                        problem("klik " + rest + ": this client knows no such entity within " + r + " blocks");
                        return true;
                    }
                    opened = true;
                    var hit = new net.minecraft.world.phys.EntityHitResult(best, best.getBoundingBox().getCenter());
                    // (26.1: one call for interact-at and interact, as a real right-click sends them)
                    var uit = mc2.gameMode.interact(mc2.player, best, hit, net.minecraft.world.InteractionHand.MAIN_HAND);
                    note("  klik " + best.getName().getString() + " (" + BuiltInRegistries.ENTITY_TYPE.getKey(best.getType()) + ", "
                            + String.format(Locale.ROOT, "%.1f", best.distanceTo(mc2.player)) + " blocks) -> " + uit);
                    return true;
                };
            }
            case "ziet": {
                // ziet <entity type id|*> [radius]: note which entities of that type THIS client knows (what the server sent it)
                double r = a.length > 2 ? Double.parseDouble(a[2]) : 64;
                return mc2 -> {
                    java.util.Map<String, Integer> tel = new java.util.TreeMap<>();
                    for (Entity e : mc2.level.entitiesForRendering()) {
                        if (e == mc2.player || e.distanceTo(mc2.player) > r) {
                            continue;
                        }
                        String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
                        if (!a[1].equals("*") && !id.equals(a[1].contains(":") ? a[1] : "guhs:" + a[1])) {
                            continue;
                        }
                        boolean naam = e.hasCustomName() || e instanceof net.minecraft.world.entity.player.Player;
                        tel.merge(id + (naam ? " '" + e.getName().getString() + "'" : ""), 1, Integer::sum);
                    }
                    note("  ziet " + a[1] + " within " + r + ": " + (tel.isEmpty() ? "nothing" : tel.toString()));
                    return true;
                };
            }
            case "quit":
                queue.clear();
                scriptIndex = scriptLines.size();
                return mc2 -> {
                    startQuit(mc2);
                    return true;
                };
            default: {
                // (1.2.7) the checks in a real generated world: WereldCheck
                Action w = WereldCheck.parse(a, rest);
                if (w != null) {
                    return w;
                }
                problem("unknown script command: " + line);
                return null;
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ basic actions

    private static Action waitTicks(int ticks) {
        int[] left = {ticks};
        return mc -> --left[0] <= 0;
    }

    /** Wait until the terrain around the camera arrived and all its sections are built (or maxTicks passed). */
    private static Action waitRender(int maxTicks) {
        int[] t = {0};
        int[] stable = {0};
        int[] last = {-1};
        return mc -> {
            t[0]++;
            if (mc.screen instanceof LevelLoadingScreen || mc.level == null || mc.player == null) {
                return t[0] > maxTicks * 2;
            }
            int cx = mc.player.chunkPosition().x(), cz = mc.player.chunkPosition().z();
            int r = Math.max(2, Math.min(7, mc.options.getEffectiveRenderDistance() - 2));
            boolean loaded = true;
            for (int x = -r; x <= r && loaded; x++) {
                for (int z = -r; z <= r && loaded; z++) {
                    loaded = mc.level.hasChunk(cx + x, cz + z);
                }
            }
            // (sections are only compiled when visible, so "built" = the compile queue is empty and stays so)
            boolean built = mc.levelRenderer.hasRenderedAllSections();
            int rendered = mc.levelRenderer.countRenderedSections();
            boolean there = expectedCam == null || mc.player.position().distanceTo(expectedCam) < 3;
            if (there && loaded && built && rendered >= 8 && rendered == last[0]) {
                stable[0]++;
            } else {
                stable[0] = 0;
            }
            last[0] = rendered;
            if (t[0] > 15 && stable[0] >= 15) {
                return true;
            }
            if (t[0] >= maxTicks) {
                note("  (waitrender: gave up after " + maxTicks + " ticks, at camera=" + there + ", chunks loaded=" + loaded + ", built=" + built
                        + ", sections=" + rendered + ")");
                return true;
            }
            return false;
        };
    }

    private static Action shot(String name, boolean withGui) {
        int[] t = {0};
        boolean[] hidBefore = {false};
        return mc -> {
            t[0]++;
            if (t[0] == 1) {
                hidBefore[0] = mc.options.hideGui;
                mc.options.hideGui = !withGui;
                mc.getToastManager().clear();
                return false;
            }
            if (t[0] < 4) {
                return false;
            }
            if (t[0] == 4) {
                String file = "autocheck/" + name.replaceAll("[^A-Za-z0-9_.\\-]", "_") + ".png";
                Screenshot.grab(mc.gameDirectory, file, mc.getMainRenderTarget(), 1, msg -> {
                });
                shots++;
                note("  shot " + file + (mc.screen != null ? " [screen " + mc.screen.getClass().getSimpleName() + "]" : ""));
                return false;
            }
            mc.options.hideGui = hidBefore[0];
            return true;
        };
    }

    /** Run a command on the integrated server as the player (op level 4), recording its output and failure. */
    /** 1.1.0: the 1.21.1 game rule names of the scripts -> the 26.1 ids. */
    private static final java.util.Map<String, String> OLD_GAMERULES = java.util.Map.of(
            "doDaylightCycle", "advance_time", "doWeatherCycle", "advance_weather", "doMobSpawning", "spawn_mobs",
            "doMobLoot", "mob_drops", "doFireTick", "fire_spread_radius_around_player", "randomTickSpeed", "random_tick_speed",
            "mobGriefing", "mob_griefing", "sendCommandFeedback", "send_command_feedback", "keepInventory", "keep_inventory");

    static Action command(String rawCmd) {
        String fixed = rawCmd;
        java.util.regex.Matcher gm = java.util.regex.Pattern.compile("^((?:execute .* run )?gamerule )([A-Za-z]+)(.*)$").matcher(rawCmd);
        if (gm.matches() && OLD_GAMERULES.containsKey(gm.group(2))) {
            String rule = OLD_GAMERULES.get(gm.group(2));
            String rest = gm.group(3);
            if (rule.startsWith("fire_spread")) {
                rest = rest.trim().equals("false") ? " 0" : " 128";
            }
            fixed = gm.group(1) + rule + rest;
        }
        final String cmd = fixed;
        CompletableFuture<?>[] f = {null};
        List<String> out = new ArrayList<>();
        boolean[] failed = {false};
        return mc -> {
            if (remote()) {
                // (the answer comes back as chat: it is in the screenshot and in the client's log, not in the report)
                mc.player.connection.sendCommand(cmd);
                note("  (remote) /" + cmd);
                return true;
            }
            if (f[0] == null) {
                var server = mc.getSingleplayerServer();
                f[0] = server.submit(() -> {
                    ServerPlayer sp = player(server);
                    CommandSource capture = new CommandSource() {
                        @Override
                        public void sendSystemMessage(Component c) {
                            TextColor red = TextColor.fromLegacyFormat(ChatFormatting.RED);
                            if (red.equals(c.getStyle().getColor())) {
                                failed[0] = true;
                            }
                            synchronized (out) {
                                out.add(c.getString());
                            }
                        }

                        @Override
                        public boolean acceptsSuccess() {
                            return true;
                        }

                        @Override
                        public boolean acceptsFailure() {
                            return true;
                        }

                        @Override
                        public boolean shouldInformAdmins() {
                            return false;
                        }
                    };
                    CommandSourceStack src = sp.createCommandSourceStack().withSource(capture).withPermission(net.minecraft.server.permissions.LevelBasedPermissionSet.OWNER)
                            .withCallback((ok, result) -> {
                                if (!ok) {
                                    failed[0] = true;
                                }
                            });
                    server.getCommands().performPrefixedCommand(src, cmd);
                });
                return false;
            }
            if (!f[0].isDone()) {
                return false;
            }
            String msg;
            synchronized (out) {
                msg = String.join(" | ", out);
            }
            if (failed[0]) {
                problem("COMMAND FAILED: /" + cmd + " -> " + msg);
            } else {
                note("  /" + cmd + (msg.isEmpty() ? "" : " -> " + msg));
            }
            return true;
        };
    }

    /** Run something on the server thread and wait for it. */
    static <T> Action server(Function<net.minecraft.server.MinecraftServer, T> job, Consumer<T> then) {
        CompletableFuture<T>[] f = new CompletableFuture[1];
        return mc -> {
            if (remote()) {
                problem("script line " + scriptIndex + " needs the integrated server: skipped in a remote round");
                return true;
            }
            if (f[0] == null) {
                var server = mc.getSingleplayerServer();
                f[0] = server.submit(() -> job.apply(server));
                return false;
            }
            if (!f[0].isDone()) {
                return false;
            }
            try {
                then.accept(f[0].join());
            } catch (Exception e) {
                problem("server job failed: " + e);
                LOG.error("AutoCheck server job", e);
            }
            return true;
        };
    }

    static ServerPlayer player(net.minecraft.server.MinecraftServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    /** Teleport (level null = stay) and hold that rotation. */
    static Action teleport(ResourceKey<Level> dim, Vec3 pos, float yaw, float pitch) {
        if (remote()) {
            return mc -> {
                String tp = String.format(Locale.ROOT, "tp @s %.3f %.3f %.3f %.2f %.2f", pos.x, pos.y, pos.z, yaw, pitch);
                mc.player.connection.sendCommand(dim == null ? tp : "execute in " + dim.identifier() + " run " + tp);
                lockYaw = yaw;
                lockPitch = pitch;
                expectedCam = pos;
                return true;
            };
        }
        return server(server -> {
            ServerPlayer sp = player(server);
            ServerLevel level = dim == null ? sp.level() : server.getLevel(dim);
            sp.teleportTo(level, pos.x, pos.y, pos.z, java.util.Set.of(), yaw, pitch, true);
            return true;
        }, ok -> {
            lockYaw = yaw;
            lockPitch = pitch;
            expectedCam = pos;
        });
    }

    private static Action useBlock(BlockPos pos) {
        return server(server -> {
            ServerPlayer sp = player(server);
            ServerLevel level = sp.level();
            BlockState st = level.getBlockState(pos);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
            return st.useWithoutItem(level, sp, hit).toString();
        }, r -> note("  use " + pos.toShortString() + " -> " + r));
    }

    /** yaw/pitch to look from a camera at a point. */
    private static float[] lookAt(Vec3 cam, Vec3 target) {
        double dx = target.x - cam.x, dy = target.y - cam.y, dz = target.z - cam.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, h)));
        return new float[] {yaw, pitch};
    }

    // ------------------------------------------------------------------------------------------------ GUIs

    private static Action gui(String[] a) {
        String what = a[1].toLowerCase(Locale.ROOT);
        switch (what) {
            case "close":
                return mc -> {
                    mc.setScreen(null);
                    opened = false;
                    return true;
                };
            case "keep":
                // gui keep: (3.0 QA) the next screen a server command opens (/guhs hemel scherm...) is wanted: keep it open until 'gui close'
                return mc -> {
                    opened = true;
                    return true;
                };
            case "guhdex": {
                String prefix = a.length > 2 ? a[2] : "gui_guhdex";
                boolean none = a.length > 3 && a[3].equalsIgnoreCase("none");
                queue.addFirst(mc -> {
                    mc.setScreen(null);
                    opened = false;
                    return true;
                });
                int pages = nl.juiced.guhs.quest.GuhDex.ENTRIES.size();
                for (int p = pages - 1; p >= 0; p--) {
                    int page = p;
                    queue.addFirst(shot(String.format("%s_%02d_%s", prefix, page + 1, nl.juiced.guhs.quest.GuhDex.ENTRIES.get(page).id()), true));
                    queue.addFirst(waitTicks(4));
                    if (page > 0) {
                        queue.addFirst(mc -> {
                            pressButton(mc.screen, ">");
                            return true;
                        });
                    }
                }
                return mc -> {
                    List<String> ids = none ? List.of()
                            : nl.juiced.guhs.quest.GuhDex.ENTRIES.stream().map(nl.juiced.guhs.entity.GuhVariant::id).toList();
                    List<String> tamed = none ? List.of() : ids.subList(0, Math.min(ids.size(), 5));
                    opened = true;
                    parkMouse(mc);
                    var dex = new nl.juiced.guhs.client.screen.GuhDexScreen(new nl.juiced.guhs.network.MaagPayloads.GuhDexData(ids, tamed, List.of()));
                    mc.setScreen(dex);
                    dex.showTab(nl.juiced.guhs.client.screen.GuhDexScreen.Tab.GUHS);   // (the tab is remembered)
                    note("  opened Guhdex (" + pages + " pages, " + (none ? "nothing seen" : "all seen") + ")");
                    return true;
                };
            }
            case "highscores":   // (2.9: the Highscores tab became the Minigames tab)
            case "minigames":
            case "kleding": {
                // gui minigames|kleding [prefix]: the Guhdex's Minigames / Kleding tab (2.9, the gids slice), everything folded
                // open, one shot per screenful (scrolled with the list)
                boolean kleding = what.equals("kleding");
                String prefix = a.length > 2 ? a[2] : kleding ? "gui_gids_kleding" : "gui_gids_minigames";
                var tabOf = kleding ? nl.juiced.guhs.client.screen.GuhDexScreen.Tab.KLEDING : nl.juiced.guhs.client.screen.GuhDexScreen.Tab.MINIGAMES;
                queue.addFirst(mc -> {
                    if (mc.screen instanceof nl.juiced.guhs.client.screen.GuhDexScreen dex) {
                        dex.openAlles(false);
                        dex.lijstPagina(0);
                        dex.showTab(nl.juiced.guhs.client.screen.GuhDexScreen.Tab.GUHS);
                    }
                    mc.setScreen(null);
                    opened = false;
                    return true;
                });
                // (the number of screenfuls is only known once the screen is open: queue the most that can be needed)
                int most = 24;
                for (int p = most - 1; p >= 0; p--) {
                    int page = p;
                    queue.addFirst(mc -> {
                        if (mc.screen instanceof nl.juiced.guhs.client.screen.GuhDexScreen dex && page < dex.lijstPaginas()) {
                            dex.lijstPagina(page);
                            queue.addFirst(shot(String.format("%s_%02d", prefix, page + 1), true));
                            queue.addFirst(waitTicks(3));
                        }
                        return true;
                    });
                }
                return mc -> {
                    List<String> ids = nl.juiced.guhs.quest.GuhDex.ENTRIES.stream().map(nl.juiced.guhs.entity.GuhVariant::id).toList();
                    opened = true;
                    parkMouse(mc);
                    var dex = new nl.juiced.guhs.client.screen.GuhDexScreen(new nl.juiced.guhs.network.MaagPayloads.GuhDexData(ids, List.of(), List.of()));
                    mc.setScreen(dex);
                    dex.showTab(tabOf);
                    dex.openAlles(true);
                    note("  opened the Guhdex " + what + " tab (" + dex.lijstPaginas() + " screenfuls, everything folded open)");
                    return true;
                };
            }
            case "anderevadsjes": {
                // gui anderevadsjes [leeg]: (1.2.10) the Guhdex tab Mijn andere vadsjes with example critters (or none)
                boolean leeg = a.length > 2 && a[2].equals("leeg");
                return mc -> {
                    java.util.List<nl.juiced.guhs.feature.band.client.MijnGuhsCache.Vadsje> vadsjes = new ArrayList<>();
                    if (!leeg) {
                        String[][] vb = {{"guh_konijntje", ""}, {"pieppiepmuisje", "Piepje"}, {"pieppiepmuisje", ""}, {"pieppiepmuisje", "Kaasje"},
                                {"pluiseekhoorntje", "Nootje"}, {"poepschilly", ""}, {"schilly", "Schilly de Tweede"}, {"guhxolotl", "Blub"}};
                        String[] plek = {"wereld", "zit", "schouder", "item_kist", "wereld", "in_guh", "item_speler", "wereld"};
                        for (int i = 0; i < vb.length; i++) {
                            vadsjes.add(new nl.juiced.guhs.feature.band.client.MijnGuhsCache.Vadsje(java.util.UUID.randomUUID(),
                                    Component.literal(vb[i][1]), vb[i][0], Component.translatable("gui.guhs.band.plek." + plek[i], "Coecomber",
                                    Component.translatable("gui.guhs.band.dim.guhs.guhmension"), -10998 + i * 37, 72, 1510 - i * 11), plek[i]));
                        }
                    }
                    opened = true;
                    parkMouse(mc);
                    List<String> ids = nl.juiced.guhs.quest.GuhDex.ENTRIES.stream().map(nl.juiced.guhs.entity.GuhVariant::id).toList();
                    var dex = new nl.juiced.guhs.client.screen.GuhDexScreen(new nl.juiced.guhs.network.MaagPayloads.GuhDexData(ids, List.of(), List.of()));
                    mc.setScreen(dex);
                    dex.showTab(nl.juiced.guhs.client.screen.GuhDexScreen.Tab.ANDERE_VADSJES);
                    // (the tab asks the server for the real ones; its answer lands a few ticks later: put the examples back after it)
                    queue.addFirst(mc3 -> {
                        nl.juiced.guhs.feature.band.client.MijnGuhsCache.zetVadsjes(vadsjes);
                        dex.mijnGuhsVernieuwd();
                        return true;
                    });
                    queue.addFirst(waitTicks(8));
                    note("  opened the Guhdex Mijn andere vadsjes tab (" + vadsjes.size() + " example critters)");
                    return true;
                };
            }
            case "mijnguhs": {
                // gui mijnguhs [list|<n>]: (2.10) the Guhdex tab Mijn guhs with example guhs (one per hearts level, filled
                // dagboekjes) without a server; "list" (default) the list, a number opens that guh's page
                String open = a.length > 2 ? a[2] : "list";
                return mc -> {
                    java.util.List<nl.juiced.guhs.feature.band.client.MijnGuhsCache.Guh> guhs = voorbeeldGuhs();
                    java.util.UUID focus = null;
                    if (!open.equals("list")) {
                        focus = guhs.get(Math.max(0, Math.min(guhs.size() - 1, Integer.parseInt(open)))).id();
                    }
                    nl.juiced.guhs.feature.band.client.MijnGuhsCache.zetVoorbeeld(guhs, 42, focus);
                    nl.juiced.guhs.feature.band.client.MijnGuhsTab.open(focus);
                    opened = true;
                    parkMouse(mc);
                    List<String> ids = nl.juiced.guhs.quest.GuhDex.ENTRIES.stream().map(nl.juiced.guhs.entity.GuhVariant::id).toList();
                    var dex = new nl.juiced.guhs.client.screen.GuhDexScreen(new nl.juiced.guhs.network.MaagPayloads.GuhDexData(ids, List.of(), List.of()));
                    mc.setScreen(dex);
                    dex.showTab(nl.juiced.guhs.client.screen.GuhDexScreen.Tab.MIJN_GUHS);
                    // (the tab asks the server for the real guhs; its answer - none - lands a few ticks later: put the
                    // examples back after it)
                    java.util.UUID focus2 = focus;
                    queue.addFirst(mc3 -> {
                        nl.juiced.guhs.feature.band.client.MijnGuhsCache.zetVoorbeeld(guhs, 42, null);
                        nl.juiced.guhs.feature.band.client.MijnGuhsTab.open(focus2);
                        dex.mijnGuhsVernieuwd();
                        return true;
                    });
                    queue.addFirst(waitTicks(8));
                    note("  opened the Guhdex Mijn guhs tab (" + guhs.size() + " example guhs, " + open + ")");
                    return true;
                };
            }
            case "verhalen": {
                // gui verhalen [list|<id>]: the Guhdex tab Verhalen (the list, or the page of one questline), left open
                String open = a.length > 2 && !a[2].equals("list") ? a[2] : null;
                return mc -> {
                    List<String> ids = nl.juiced.guhs.quest.GuhDex.ENTRIES.stream().map(nl.juiced.guhs.entity.GuhVariant::id).toList();
                    opened = true;
                    parkMouse(mc);
                    nl.juiced.guhs.feature.gids.client.GidsVerhalenTab.open(open);
                    var dex = new nl.juiced.guhs.client.screen.GuhDexScreen(new nl.juiced.guhs.network.MaagPayloads.GuhDexData(ids, List.of(), List.of()));
                    mc.setScreen(dex);
                    dex.showTab(nl.juiced.guhs.client.screen.GuhDexScreen.Tab.VERHALEN);
                    note("  opened the Guhdex Verhalen tab (" + (open == null ? "list" : open) + ")");
                    return true;
                };
            }
            case "verhalenscroll": {
                // gui verhalenscroll <n>: scroll the open Verhalen list/page to screenful n
                int n = Integer.parseInt(a[2]);
                return mc -> {
                    if (mc.screen instanceof nl.juiced.guhs.client.screen.GuhDexScreen dex) {
                        dex.verhalenTab().scrollNaar(n);
                    }
                    return true;
                };
            }
            case "dexopen": {
                // gui dexopen <guhs|knus|minigames|kleding> [open]: open the Guhdex on a tab and leave it open (for mouse/click/shot)
                var tabOf = nl.juiced.guhs.client.screen.GuhDexScreen.Tab.valueOf(a[2].toUpperCase(Locale.ROOT));
                boolean alles = a.length > 3 && a[3].equalsIgnoreCase("open");
                return mc -> {
                    List<String> ids = nl.juiced.guhs.quest.GuhDex.ENTRIES.stream().map(nl.juiced.guhs.entity.GuhVariant::id).toList();
                    opened = true;
                    var dex = new nl.juiced.guhs.client.screen.GuhDexScreen(new nl.juiced.guhs.network.MaagPayloads.GuhDexData(ids, List.of(), List.of()));
                    mc.setScreen(dex);
                    dex.showTab(tabOf);
                    dex.openAlles(alles);
                    dex.lijstPagina(0);
                    return true;
                };
            }
            case "superkompas": {
                // gui superkompas [prefix]: the Superkompas menu (2.9 icon tabs), one shot per tab
                String prefix = a.length > 2 ? a[2] : "gui_gids_superkompas";
                var cats = nl.juiced.guhs.item.SuperkompasItem.CATEGORIES;
                queue.addFirst(mc -> {
                    mc.setScreen(null);
                    opened = false;
                    return true;
                });
                for (int c = cats.size() - 1; c >= 0; c--) {
                    int index = c;
                    queue.addFirst(shot(String.format("%s_%02d_%s", prefix, index + 1, cats.get(index).id()), true));
                    queue.addFirst(waitTicks(3));
                    queue.addFirst(mc -> {
                        if (mc.screen instanceof nl.juiced.guhs.client.screen.SuperkompasScreen sk) {
                            sk.showTab(index);
                        }
                        return true;
                    });
                }
                return mc -> {
                    opened = true;
                    parkMouse(mc);
                    mc.setScreen(new nl.juiced.guhs.client.screen.SuperkompasScreen(net.minecraft.world.InteractionHand.MAIN_HAND, "sjoelhuisje"));
                    note("  opened the Superkompas (" + cats.size() + " tabs)");
                    return true;
                };
            }
            case "taalvraag": {
                // gui taalvraag [name] [keep]: (1.2.0) the first-join language question (AutoCheck runs never get it by themselves);
                // with "keep" it stays open (for mouse/press/shot commands) until 'gui close'
                String name = a.length > 2 ? a[2] : "gui_taalvraag";
                boolean keep = a.length > 3 && a[3].equalsIgnoreCase("keep");
                if (!keep) {
                    queue.addFirst(mc -> {
                        mc.setScreen(null);
                        opened = false;
                        return true;
                    });
                    queue.addFirst(shot(name, true));
                    queue.addFirst(waitTicks(4));
                }
                return mc -> {
                    opened = true;
                    parkMouse(mc);
                    mc.setScreen(new nl.juiced.guhs.client.screen.TaalVraagScreen());
                    note("  opened the language question");
                    return true;
                };
            }
            case "knus": {
                // gui knus [prefix]: the Guhdex's Knus tab (the overview of the sections)
                String prefix = a.length > 2 ? a[2] : "gui_knus";
                queue.addFirst(mc -> {
                    if (mc.screen instanceof nl.juiced.guhs.client.screen.GuhDexScreen dex) {
                        dex.showTab(nl.juiced.guhs.client.screen.GuhDexScreen.Tab.GUHS);
                    }
                    mc.setScreen(null);
                    opened = false;
                    return true;
                });
                queue.addFirst(shot(prefix + "_overzicht", true));
                queue.addFirst(waitTicks(4));
                return mc -> {
                    List<String> ids = nl.juiced.guhs.quest.GuhDex.ENTRIES.stream().map(nl.juiced.guhs.entity.GuhVariant::id).toList();
                    opened = true;
                    parkMouse(mc);
                    var dex = new nl.juiced.guhs.client.screen.GuhDexScreen(new nl.juiced.guhs.network.MaagPayloads.GuhDexData(ids, List.of(), List.of()));
                    mc.setScreen(dex);
                    dex.showTab(nl.juiced.guhs.client.screen.GuhDexScreen.Tab.KNUS);
                    note("  opened the Guhdex Knus tab");
                    return true;
                };
            }
            case "creative": {
                // gui creative <namespace or tab id> <prefix>
                String which = a.length > 2 ? a[2] : "guhs";
                String prefix = a.length > 3 ? a[3] : "gui_creative";
                return mc -> {
                    List<Map.Entry<Identifier, CreativeModeTab>> tabs = new ArrayList<>();
                    for (var e : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
                        Identifier id = e.getKey().identifier();
                        if (id.getNamespace().equals(which) || id.toString().equals(which)) {
                            tabs.add(Map.entry(id, e.getValue()));
                        }
                    }
                    List<Action> acts = new ArrayList<>();
                    for (var tab : tabs) {
                        acts.add(mc2 -> {
                            opened = true;
                            parkMouse(mc2);
                            var screen = new CreativeModeInventoryScreen(mc2.player, mc2.player.connection.enabledFeatures(),
                                    mc2.options.operatorItemsTab().get());
                            mc2.setScreen(screen);
                            try {
                                var m = CreativeModeInventoryScreen.class.getDeclaredMethod("selectTab", CreativeModeTab.class);
                                m.setAccessible(true);
                                m.invoke(screen, tab.getValue());
                            } catch (Exception e) {
                                problem("creative: cannot select tab " + tab.getKey() + ": " + e);
                            }
                            return true;
                        });
                        acts.add(waitTicks(5));
                        int items = tab.getValue().getDisplayItems().size();
                        int rows = (items + 8) / 9;
                        int pages = rows <= 5 ? 1 : (int) Math.ceil((rows - 5) / 5.0) + 1;
                        note("  creative tab " + tab.getKey() + ": " + items + " items, " + pages + " page(s)");
                        for (int p = 0; p < pages; p++) {
                            float scroll = rows <= 5 ? 0 : Math.min(1f, (p * 5f) / (rows - 5));
                            acts.add(mc2 -> {
                                if (mc2.screen instanceof CreativeModeInventoryScreen cs) {
                                    try {
                                        var f = CreativeModeInventoryScreen.class.getDeclaredField("scrollOffs");
                                        f.setAccessible(true);
                                        f.setFloat(cs, scroll);
                                        cs.getMenu().scrollTo(scroll);
                                    } catch (Exception e) {
                                        problem("creative: cannot scroll: " + e);
                                    }
                                } else {
                                    problem("creative: screen is " + (mc2.screen == null ? "none" : mc2.screen.getClass().getName()));
                                }
                                return true;
                            });
                            acts.add(waitTicks(3));
                            acts.add(shot(prefix + "_" + tab.getKey().getPath() + "_" + (p + 1), true));
                        }
                    }
                    acts.add(mc2 -> {
                        mc2.setScreen(null);
                        opened = false;
                        return true;
                    });
                    for (int i = acts.size() - 1; i >= 0; i--) {
                        queue.addFirst(acts.get(i));
                    }
                    return true;
                };
            }
            case "ftbquests": {
                // gui ftbquests [prefix]: open the FTB quest book on every Guhs chapter: an overview (zoomed out), the chapter at
                // normal zoom from top to bottom, and the "Hoe kom je hier?" quest opened. (FTB Quests is only a dev dependency: reflection.)
                String prefix = a.length > 2 ? a[2] : "gui_ftb";
                return mc -> {
                    try {
                        Class<?> cqf = Class.forName("dev.ftb.mods.ftbquests.client.ClientQuestFile");
                        // (1.1.0: FTB Quests 26.1 has ClientQuestFile.getInstance() instead of the INSTANCE field)
                        Object file = (boolean) cqf.getMethod("exists").invoke(null) ? cqf.getMethod("getInstance").invoke(null) : null;
                        if (file == null) {
                            problem("ftbquests: no quest file on the client (is FTB Quests loaded?)");
                            return true;
                        }
                        List<Object> chapters = new ArrayList<>();
                        for (Object ch : (List<?>) file.getClass().getMethod("getAllChapters").invoke(file)) {
                            if (ftbChapterName(ch).startsWith("guhs")) {
                                chapters.add(ch);
                            }
                        }
                        note("  FTB quest book: " + chapters.size() + " Guhs chapters");
                        List<Action> acts = new ArrayList<>();
                        acts.add(mc2 -> {
                            try {
                                opened = true;
                                parkMouse(mc2);
                                cqf.getMethod("openGui").invoke(null);
                            } catch (Exception e) {
                                problem("ftbquests: cannot open the book: " + e);
                            }
                            return true;
                        });
                        acts.add(waitTicks(10));
                        if (!chapters.isEmpty()) {
                            // the chapter list (the Guhs group with the chapter icons): it slides open with the mouse on the left edge
                            acts.add(select(chapters.get(0), 12));
                            acts.add(mc2 -> {
                                try {
                                    Object screen = ftbScreen(mc2);
                                    var f = screen.getClass().getDeclaredField("chapterPanel");
                                    f.setAccessible(true);
                                    Object panel = f.get(screen);
                                    panel.getClass().getMethod("setExpanded", boolean.class).invoke(panel, true);
                                } catch (Exception e) {
                                    problem("ftbquests: cannot open the chapter list: " + e);
                                }
                                setCursor(mc2, 12, mc2.getWindow().getScreenHeight() / 2.0);
                                return true;
                            });
                            acts.add(waitTicks(15));
                            acts.add(shot(prefix + "_00_hoofdstukken", true));
                            acts.add(mc2 -> {
                                parkMouse(mc2);
                                return true;
                            });
                            acts.add(waitTicks(10));
                        }
                        for (Object ch : chapters) {
                            String name = ftbChapterName(ch);
                            List<?> quests = (List<?>) ch.getClass().getMethod("getQuests").invoke(ch);
                            List<?> links = (List<?>) ch.getClass().getMethod("getQuestLinks").invoke(ch);
                            List<?> images = (List<?>) ch.getClass().getMethod("getImages").invoke(ch);
                            double minY = 1e9, maxY = -1e9, minX = 1e9, maxX = -1e9;
                            Object intro = null;
                            for (Object quest : quests) {
                                double x = (double) quest.getClass().getMethod("getX").invoke(quest);
                                double y = (double) quest.getClass().getMethod("getY").invoke(quest);
                                minX = Math.min(minX, x);
                                maxX = Math.max(maxX, x);
                                minY = Math.min(minY, y);
                                maxY = Math.max(maxY, y);
                                String t = ((Component) quest.getClass().getMethod("getTitle").invoke(quest)).getString();
                                if (t.equals("Hoe kom je hier?") || t.equals("How Do You Get Here?")) {   // (1.2.0: NL or EN book)
                                    intro = quest;
                                }
                            }
                            note("  " + name + ": " + quests.size() + " quests, " + links.size() + " links, " + images.size() + " pictures");
                            for (Object link : links) {
                                Object target = ((java.util.Optional<?>) link.getClass().getMethod("getQuest").invoke(link)).orElse(null);
                                Object tch = target == null ? null : target.getClass().getMethod("getChapter").invoke(target);
                                note("    link -> " + (target == null ? "MISSING" : ((Component) target.getClass().getMethod("getTitle").invoke(target)).getString()
                                        + " (in " + ftbChapterName(tch) + ")"));
                                if (target == null) {
                                    problem("ftbquests: a link in " + name + " points nowhere");
                                }
                            }
                            if (intro == null) {
                                problem("ftbquests: " + name + " has no 'Hoe kom je hier?' quest");
                            }
                            double cx = (minX + maxX) / 2, top = minY - 4, bottom = maxY;
                            acts.add(select(ch, 8));
                            acts.add(waitTicks(6));
                            acts.add(shot(prefix + "_" + name + "_0overzicht", true));
                            int part = 1;
                            for (double y = top + 5; ; y += 9) {
                                double yy = Math.min(y, bottom - 3);
                                acts.add(select(ch, 20));
                                acts.add(scroll(cx, yy));
                                acts.add(waitTicks(5));
                                acts.add(shot(prefix + "_" + name + "_" + part++, true));
                                if (yy >= bottom - 3) {
                                    break;
                                }
                            }
                            Object introQuest = intro;
                            if (introQuest != null) {
                                acts.add(mc2 -> {
                                    try {
                                        Object screen = ftbScreen(mc2);
                                        Class<?> qo = Class.forName("dev.ftb.mods.ftbquests.quest.QuestObject");
                                        screen.getClass().getMethod("open", qo, boolean.class).invoke(screen, introQuest, false);
                                    } catch (Exception e) {
                                        problem("ftbquests: cannot open the intro of " + name + ": " + e);
                                    }
                                    return true;
                                });
                                acts.add(waitTicks(6));
                                acts.add(shot(prefix + "_" + name + "_intro", true));
                            }
                        }
                        acts.add(mc2 -> {
                            mc2.setScreen(null);
                            opened = false;
                            return true;
                        });
                        for (int i = acts.size() - 1; i >= 0; i--) {
                            queue.addFirst(acts.get(i));
                        }
                    } catch (Exception e) {
                        problem("ftbquests: " + e);
                    }
                    return true;
                };
            }
            default:
                problem("unknown gui: " + what);
                return null;
        }
    }

    /** FTB Quests: show this chapter in the open quest book, at this zoom (4..28), scrolled to the start. */
    private static Action select(Object chapter, int zoom) {
        return mc -> {
            try {
                Object screen = ftbScreen(mc);
                if (screen == null || !screen.getClass().getName().startsWith("dev.ftb.mods.ftbquests")) {
                    problem("ftbquests: the quest book isn't open (" + (mc.screen == null ? "no screen" : mc.screen.getClass().getName()) + ")");
                    return true;
                }
                Class<?> chClass = Class.forName("dev.ftb.mods.ftbquests.quest.Chapter");
                Object current = screen.getClass().getMethod("getSelectedChapter").invoke(screen);
                if (!(current instanceof java.util.Optional<?> o && o.orElse(null) == chapter)) {
                    screen.getClass().getMethod("selectChapter", chClass).invoke(screen, chapter);
                }
                int now = (int) screen.getClass().getMethod("getZoom").invoke(screen);
                screen.getClass().getMethod("addZoom", double.class).invoke(screen, (zoom - now) / 4.0);
                parkMouse(mc);
            } catch (Exception e) {
                problem("ftbquests: cannot select a chapter: " + e);
            }
            return true;
        };
    }

    /** FTB Quests: a chapter's file name (1.1.0: FTB Quests 26.1 made getFilename private; the chapter's file path is public). */
    private static String ftbChapterName(Object chapter) throws ReflectiveOperationException {
        Object path = ((java.util.Optional<?>) chapter.getClass().getMethod("getPath").invoke(chapter)).orElse(null);
        if (path instanceof java.nio.file.Path p) {
            String n = p.getFileName().toString();
            int dot = n.indexOf('.');
            return dot > 0 ? n.substring(0, dot) : n;
        }
        var m = chapter.getClass().getDeclaredMethod("getFilename");
        m.setAccessible(true);
        return String.valueOf(m.invoke(chapter));
    }

    /** FTB Quests: the quest screen inside FTB Library's screen wrapper (or null). */
    private static Object ftbScreen(Minecraft mc) {
        Object screen = mc.screen;
        try {
            return screen == null ? null : screen.getClass().getMethod("getGui").invoke(screen);
        } catch (Exception e) {
            return screen;
        }
    }

    /** FTB Quests: scroll the quest panel so that this quest position is in the middle. */
    private static Action scroll(double x, double y) {
        return mc -> {
            try {
                Object screen = ftbScreen(mc);
                var f = screen.getClass().getDeclaredField("questPanel");
                f.setAccessible(true);
                Object panel = f.get(screen);
                panel.getClass().getMethod("scrollTo", double.class, double.class).invoke(panel, x, y);
            } catch (Exception e) {
                problem("ftbquests: cannot scroll: " + e);
            }
            return true;
        };
    }

    // ------------------------------------------------------------------------------------------------ 2.10 visual QA helpers

    /** Example guhs for the Mijn guhs tab: one per hearts level, with clothes, favourites, friends and a full dagboekje. */
    private static java.util.List<nl.juiced.guhs.feature.band.client.MijnGuhsCache.Guh> voorbeeldGuhs() {
        java.util.List<nl.juiced.guhs.feature.band.client.MijnGuhsCache.Guh> out = new java.util.ArrayList<>();
        String[][] soorten = {{"normal", "vadsig", "Knabbeltje"}, {"mint", "playful", "Muntje"}, {"golden", "cuddly", "Goudvadsje"},
                {"choco", "lazy", "Snurkie"}};
        int[] hartjes = {42, 180, 820, 2400};
        for (int i = 0; i < soorten.length; i++) {
            net.minecraft.nbt.CompoundTag looks = new net.minecraft.nbt.CompoundTag();
            looks.putString("Variant", soorten[i][0]);
            looks.putString("Personality", soorten[i][1]);
            net.minecraft.nbt.CompoundTag clothes = new net.minecraft.nbt.CompoundTag();
            if (i == 1) {
                clothes.putString("HEAD", "party_hat");
            } else if (i == 2) {
                clothes.putString("EYES", "heart_glasses");
                clothes.putString("NECK", "red_bowtie");
            }
            looks.put("Clothes", clothes);
            looks.putBoolean("Baby", i == 3);
            nl.juiced.guhs.feature.band.BandNiveau niveau = nl.juiced.guhs.feature.band.BandNiveau.van(hartjes[i]);
            nl.juiced.guhs.feature.band.BandNiveau volgende = niveau.volgende();
            java.util.List<nl.juiced.guhs.feature.band.client.MijnGuhsCache.Fav> fav = new java.util.ArrayList<>();
            String[] waarden = {"guhs:guh_cupcake", "guhs:pink_puffs", "mint", "koortje:altijd_vads", "knabbelbal", "dansen", "roze", "?"};
            for (nl.juiced.guhs.feature.band.FavorietSoort s : nl.juiced.guhs.feature.band.FavorietSoort.values()) {
                boolean bekend = s.ordinal() < i * 3;
                fav.add(new nl.juiced.guhs.feature.band.client.MijnGuhsCache.Fav(s.id(),
                        bekend ? nl.juiced.guhs.feature.band.Favorieten.naam(s, waarden[s.ordinal()]) : null));
            }
            java.util.List<nl.juiced.guhs.feature.band.client.MijnGuhsCache.Vriend> vrienden = new java.util.ArrayList<>();
            if (i > 0) {
                vrienden.add(new nl.juiced.guhs.feature.band.client.MijnGuhsCache.Vriend(net.minecraft.network.chat.Component.literal(soorten[(i + 1) % 4][2]), i == 2));
                vrienden.add(new nl.juiced.guhs.feature.band.client.MijnGuhsCache.Vriend(net.minecraft.network.chat.Component.literal(soorten[(i + 2) % 4][2]), false));
            }
            java.util.List<net.minecraft.network.chat.Component> klussen = new java.util.ArrayList<>();
            if (i >= 2) {
                for (String k : new String[] {"farmen", "opgraven", "lampjes"}) {
                    klussen.add(net.minecraft.network.chat.Component.translatable("gui.guhs.klus." + k));
                }
            }
            java.util.Map<String, Long> stats = new java.util.LinkedHashMap<>();
            for (nl.juiced.guhs.feature.band.DagboekStat st : nl.juiced.guhs.feature.band.DagboekStat.values()) {
                stats.put(st.id(), (long) (st.ordinal() + 1) * (i + 1) * 7);
            }
            java.util.List<nl.juiced.guhs.feature.band.client.MijnGuhsCache.Eerste> eerste = new java.util.ArrayList<>();
            String[] ids = {"getemd", "eerste_aai", "eerste_knuffel", "eerste_rit", "eerste_lief", "eerste_huisje", "eerste_mega", "eerste_zielsguh"};
            for (int e = 0; e < Math.min(ids.length, 2 + i * 2); e++) {
                eerste.add(new nl.juiced.guhs.feature.band.client.MijnGuhsCache.Eerste(ids[e], e * 3L));
            }
            java.util.List<nl.juiced.guhs.feature.band.client.MijnGuhsCache.Wist> wist = new java.util.ArrayList<>();
            wist.add(new nl.juiced.guhs.feature.band.client.MijnGuhsCache.Wist(
                    net.minecraft.network.chat.Component.translatable("gui.guhs.wistjedat.band.niveau_lief", "Dev"), 12));
            wist.add(new nl.juiced.guhs.feature.band.client.MijnGuhsCache.Wist(
                    net.minecraft.network.chat.Component.translatable("gui.guhs.wistjedat.band.getemd", "Dev"), 1));
            net.minecraft.network.chat.Component plek = net.minecraft.network.chat.Component.translatable(
                    i == 2 ? "gui.guhs.band.plek.huisje" : i == 3 ? "gui.guhs.band.plek.item_kist" : "gui.guhs.band.plek.wereld", i == 3 ? "Kist" : "Villa Vahoeg",
                    net.minecraft.network.chat.Component.translatable("gui.guhs.band.dim.guhs.guhmension"), 120, 70, -48);
            out.add(new nl.juiced.guhs.feature.band.client.MijnGuhsCache.Guh(java.util.UUID.nameUUIDFromBytes(("voorbeeld" + i).getBytes()),
                    net.minecraft.network.chat.Component.literal(soorten[i][2]), looks, hartjes[i], niveau.ordinal(), volgende == null ? -1 : volgende.drempel(), fav, vrienden,
                    net.minecraft.network.chat.Component.literal(i >= 2 ? "Villa Vahoeg" : ""), klussen, plek, stats, eerste, wist, 0, false,
                    i == 3 ? "item_kist" : "wereld"));
        }
        return out;
    }

    /** A groot Guhhuisje (door to the south) with two guhs and a muisje living in it, its screen open, the dome on. */
    private static Action huisje(BlockPos pos) {
        return mc0 -> {
            opened = true;
            queue.addFirst(server(server -> {
                ServerPlayer sp = player(server);
                ServerLevel level = sp.level();
                nl.juiced.guhs.feature.huisje.Huisje h = nl.juiced.guhs.feature.huisje.HuisjeBlock.bouw(level, pos,
                        net.minecraft.core.Direction.SOUTH, nl.juiced.guhs.feature.huisje.HuisjeMaat.GROOT, sp.getUUID());
                BlockPos d = h.deur();
                for (int i = 0; i < 3; i++) {
                    net.minecraft.world.entity.TamableAnimal t = i < 2 ? nl.juiced.guhs.registry.ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED)
                            : nl.juiced.guhs.feature.piep.PiepFeature.PIEPPIEPMUISJE.get().create(level, EntitySpawnReason.TRIGGERED);
                    if (t == null) {
                        continue;
                    }
                    t.snapTo(d.getX() + 0.5 + (i - 1) * 1.5, d.getY(), d.getZ() + 2.5, 180, 0);
                    t.tame(sp);
                    if (t instanceof nl.juiced.guhs.entity.GuhEntity g && i == 1) {
                        g.setVariant(nl.juiced.guhs.entity.GuhVariant.MINT);
                    }
                    level.addFreshEntity(t);
                    nl.juiced.guhs.feature.huisje.Huisjes.trekIn(h, t);
                }
                nl.juiced.guhs.feature.huisje.HuisjePayloads.open(sp, h);
                // (1.1.0: 26.1 handles the open packet before this task, so the open screen is rebuilt: "Klus-area: aan")
                Minecraft.getInstance().execute(() -> {
                    nl.juiced.guhs.feature.huisje.client.HuisjeKoepel.zet(pos, true);
                    Minecraft m = Minecraft.getInstance();
                    if (m.screen != null) {
                        m.screen.resize(m.screen.width, m.screen.height);
                    }
                });
                return "huisje " + h.naam() + " at " + pos.toShortString() + " with " + h.bewoners().size() + " residents";
            }, r -> note("  " + r)));
            return true;
        };
    }

    /** A Wilde-guhweerder (facing south) of the player with this radius, its screen open, the dome on. */
    private static Action weerder(BlockPos pos, int straal) {
        return mc0 -> {
            opened = true;
            queue.addFirst(server(server -> {
                ServerPlayer sp = player(server);
                ServerLevel level = sp.level();
                level.setBlock(pos, nl.juiced.guhs.feature.weerder.WeerderFeature.WEERDER.get().defaultBlockState()
                        .setValue(nl.juiced.guhs.feature.weerder.WildeGuhweerderBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
                if (!(level.getBlockEntity(pos) instanceof nl.juiced.guhs.feature.weerder.WeerderBlockEntity be)) {
                    return "no weerder at " + pos.toShortString();
                }
                be.zetEigenaar(sp);
                be.zetStraal(straal);
                nl.juiced.guhs.feature.weerder.WeerderPayloads.open(sp, be);
                Minecraft.getInstance().execute(() -> {
                    nl.juiced.guhs.feature.weerder.client.WeerderKoepel.zet(pos, true);
                    Minecraft m = Minecraft.getInstance();
                    if (m.screen != null) {
                        m.screen.resize(m.screen.width, m.screen.height);
                    }
                });
                return "weerder at " + pos.toShortString() + " radius " + be.straal();
            }, r -> note("  " + r)));
            return true;
        };
    }

    // ------------------------------------------------------------------------------------------------ 3.0 (Guhverhalen) helpers

    /** A tamed story guh (Baltoguh, Guhtwo, 626-guh) of the player at pos, doing its special (VariantGedrag.speciaal). */
    private static Action verhaalguh(String id, BlockPos pos) {
        return server(server -> {
            ServerPlayer sp = player(server);
            ServerLevel level = sp.level();
            nl.juiced.guhs.feature.verhaal.VerhaalGuh g = nl.juiced.guhs.feature.verhaal.VerhaalGuh.byId(id);
            if (g == null) {
                return "verhaalguh: unknown " + id;
            }
            nl.juiced.guhs.entity.GuhEntity guh = nl.juiced.guhs.registry.ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
            guh.setVariant(g.variant());
            guh.setGuhScale(nl.juiced.guhs.feature.verhaal.VerhaalGuhs.SCHAAL);
            guh.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 180, 0);
            level.addFreshEntity(guh);
            guh.tame(sp);
            var gedrag = nl.juiced.guhs.feature.verhaal.VariantGedragen.van(guh);
            if (gedrag != null && gedrag.speciaalKnop() != null) {
                gedrag.speciaal(guh, sp);
            }
            return "verhaalguh " + id + " at " + pos.toShortString() + (gedrag == null ? " (no behaviour yet)" : "");
        }, r -> note("  " + r));
    }

    /** A tamed example guh goes to the wolkjes (band.Wolkjes); the Guhdex opens on its page in Mijn guhs. */
    private static Action wolkjes() {
        return mc0 -> {
            opened = true;
            queue.addFirst(server(server -> {
                ServerPlayer sp = player(server);
                ServerLevel level = sp.level();
                nl.juiced.guhs.entity.GuhEntity guh = nl.juiced.guhs.registry.ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
                guh.setCustomName(net.minecraft.network.chat.Component.literal("Wolkje"));
                guh.setVariant(nl.juiced.guhs.entity.GuhVariant.SNOW);
                guh.snapTo(sp.getX() + 2, sp.getY(), sp.getZ(), 0, 0);
                level.addFreshEntity(guh);
                guh.tame(sp);
                guh.kill(level);
                nl.juiced.guhs.feature.band.MijnGuhs.stuur(sp, guh.getUUID());
                nl.juiced.guhs.quest.GuhDex.open(sp);
                return "wolkjes: " + nl.juiced.guhs.feature.band.Wolkjes.dood(server, sp.getUUID()).size() + " in de wolkjes";
            }, r -> note("  " + r)));
            return true;
        };
    }

    /** A klein Guhhuisje of someone else (owner name naam) at pos: the owner line, and no breaking or changing for you. */
    private static Action huisjeVan(String naam, BlockPos pos) {
        return server(server -> {
            ServerLevel level = player(server).level();
            java.util.UUID ander = java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + naam).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            nl.juiced.guhs.feature.huisje.Huisje h = nl.juiced.guhs.feature.huisje.HuisjeBlock.bouw(level, pos, net.minecraft.core.Direction.SOUTH,
                    nl.juiced.guhs.feature.huisje.HuisjeMaat.KLEIN, ander);
            nl.juiced.guhs.feature.huisje.Huisjes.zetEigenaarNaam(server, h, naam);
            return "huisje " + h.naam() + " van " + naam + " at " + pos.toShortString();
        }, r -> note("  " + r));
    }

    /** (3.0 timmerguh) the read-only screen of someone else's huisje, as a non-owner sees it. */
    private static Action huisjeKijk(BlockPos pos) {
        return mc0 -> {
            opened = true;
            queue.addFirst(server(server -> {
                ServerPlayer sp = player(server);
                nl.juiced.guhs.feature.huisje.Huisje h = nl.juiced.guhs.feature.huisje.Huisjes.van(sp.level(), pos);
                if (h == null) {
                    return "no huisje at " + pos.toShortString();
                }
                net.minecraft.nbt.CompoundTag data = nl.juiced.guhs.feature.huisje.HuisjePayloads.data(sp, h);
                data.putBoolean("MagBewerken", false);
                nl.juiced.guhs.network.ModNetworking.sendTo(sp, new nl.juiced.guhs.feature.huisje.HuisjePayloads.Open(data));
                return "read-only screen of " + h.naam() + " (" + h.eigenaarNaam() + ")";
            }, r -> note("  " + r)));
            return true;
        };
    }

    // ------------------------------------------------------------------------------------------------ 2.9 visual QA helpers

    /** Right-click / open the menu of the nearest entity (within 6 blocks of pos) on the server; the screen it opens stays open. */
    private static Action interactEntity(BlockPos pos, @javax.annotation.Nullable String type, String how) {
        // (opened first: the screen can arrive before the server job's result is handled)
        return mc0 -> {
            opened = true;
            queue.addFirst(interactEntityJob(pos, type, how));
            return true;
        };
    }

    private static Action interactEntityJob(BlockPos pos, @javax.annotation.Nullable String type, String how) {
        return server(server -> {
            ServerPlayer sp = player(server);
            ServerLevel level = sp.level();
            var box = new net.minecraft.world.phys.AABB(pos).inflate(6);
            Entity best = null;
            double bestD = Double.MAX_VALUE;
            for (Entity e : level.getEntities((Entity) null, box, e -> !(e instanceof net.minecraft.world.entity.player.Player))) {
                if (type != null && !BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(type)) {
                    continue;
                }
                if (how.equals("piepmenu") && !(e instanceof nl.juiced.guhs.feature.piep.PiepMaatje)) {
                    continue;
                }
                double d = e.distanceToSqr(Vec3.atCenterOf(pos));
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
            if (best == null) {
                return "no entity" + (type == null ? "" : " of type " + type) + " near " + pos.toShortString();
            }
            if (!how.equals("interact") && best instanceof net.minecraft.world.entity.TamableAnimal t && !t.isOwnedBy(sp)) {
                t.tame(sp);
            }
            switch (how) {
                case "wardrobe" -> ((nl.juiced.guhs.entity.GuhEntity) best).openWardrobe(sp);
                case "guhmenu" -> {
                    int id = best.getId();
                    Minecraft.getInstance().execute(() -> {
                        var mc = Minecraft.getInstance();
                        if (mc.level != null && mc.level.getEntity(id) instanceof nl.juiced.guhs.entity.GuhEntity g) {
                            mc.setScreen(new nl.juiced.guhs.client.screen.GuhScreen(g));
                        }
                    });
                }
                case "piepmenu" -> nl.juiced.guhs.feature.piep.PiepMenu.open(sp, (nl.juiced.guhs.feature.piep.PiepMaatje) best);
                case "aai" -> {
                    sp.setShiftKeyDown(false);
                    ((nl.juiced.guhs.entity.GuhEntity) best).onOwnerTap(sp);
                    return "aai " + best.getName().getString() + " -> emote " + ((nl.juiced.guhs.entity.GuhEntity) best).emotes.current();
                }
                default -> {
                    var r = sp.interactOn(best, net.minecraft.world.InteractionHand.MAIN_HAND, best.getBoundingBox().getCenter().subtract(best.position()));
                    return how + " " + best.getName().getString() + " -> " + r;
                }
            }
            return how + " " + best.getName().getString();
        }, r -> {
            if (!how.equals("aai")) {   // (petting opens no screen)
                opened = true;
            }
            note("  " + r);
        });
    }

    private static Action goToNpc(String kind, double dist, double dy) {
        return server(server -> {
            ServerPlayer sp = player(server);
            ServerLevel level = sp.level();
            var box = sp.getBoundingBox().inflate(400, 200, 400);
            Entity best = null;
            double bestD = Double.MAX_VALUE;
            for (Entity e : level.getEntities((Entity) null, box, e -> e instanceof nl.juiced.guhs.entity.GuhNpcEntity n
                    && n.getKind().id().equals(kind))) {
                double d = e.distanceToSqr(sp);
                if (d < bestD) {
                    bestD = d;
                    best = e;
                }
            }
            if (best == null) {
                return null;
            }
            float yaw = best.getYHeadRot();
            Vec3 face = new Vec3(-Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
            Vec3 head = best.position().add(0, best.getBbHeight() * 0.8, 0);
            Vec3 cam = head.add(face.scale(dist)).add(0, dy, 0).subtract(0, sp.getEyeHeight(), 0);
            lastNpc = best.getId();
            return new Object[] {cam, head, best.blockPosition().toShortString()};
        }, r -> {
            if (r == null) {
                problem("npc " + kind + ": none loaded within 400 blocks");
                return;
            }
            Vec3 cam = (Vec3) r[0];
            Vec3 eye = cam.add(0, 1.62, 0);
            float[] rot = lookAt(eye, (Vec3) r[1]);
            note("  npc " + kind + " at " + r[2]);
            queue.addFirst(teleport(null, cam, rot[0], rot[1]));
        });
    }

    private static Action interactNpc() {
        return mc0 -> {
            opened = true;
            queue.addFirst(interactNpcJob());
            return true;
        };
    }

    private static Action interactNpcJob() {
        return server(server -> {
            ServerPlayer sp = player(server);
            Entity e = sp.level().getEntity(lastNpc);
            if (e == null) {
                return "interact npc: no npc";
            }
            return "interact " + e.getName().getString() + " -> " + sp.interactOn(e, net.minecraft.world.InteractionHand.MAIN_HAND, e.getBoundingBox().getCenter().subtract(e.position()));
        }, r -> {
            opened = true;
            note("  " + r);
        });
    }

    private static Action unlockClothes(String[] ids) {
        return server(server -> {
            ServerPlayer sp = player(server);
            int n = 0;
            for (var c : nl.juiced.guhs.entity.GuhClothes.values()) {
                boolean want = ids.length == 0 || ids[0].equals("all") || java.util.Arrays.stream(ids).anyMatch(i -> i.equalsIgnoreCase(c.id()));
                if (want && c.slot != nl.juiced.guhs.entity.GuhClothes.Slot.HAAR && nl.juiced.guhs.feature.kleding.KledingUnlocks.ontgrendel(sp, c)) {
                    n++;
                }
            }
            nl.juiced.guhs.feature.kleding.KledingUnlocks.sync(sp);
            return n;
        }, n -> note("  unlocked " + n + " clothing pieces"));
    }

    private static Action holdUse(int ticks) {
        int[] t = {0};
        return mc -> {
            t[0]++;
            if (t[0] == 1) {
                opened = true;   // (2.10: an item may open its own screen, e.g. the Guhbel: that one is wanted, 'gui close' closes it)
                net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), true);
                net.minecraft.client.KeyMapping.click(mc.options.keyUse.getKey());
            }
            if (t[0] >= ticks) {
                net.minecraft.client.KeyMapping.set(mc.options.keyUse.getKey(), false);
                note("  held right-click " + ticks + " ticks");
                return true;
            }
            return false;
        };
    }

    private static void dumpWidgets(Minecraft mc) {
        if (mc.screen == null) {
            note("  widgets: no screen");
            return;
        }
        note("  widgets of " + mc.screen.getClass().getSimpleName() + " (gui " + mc.screen.width + "x" + mc.screen.height + "):");
        List<net.minecraft.client.gui.components.AbstractWidget> ws = new ArrayList<>();
        for (var child : mc.screen.children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget w) {
                note("    " + w.getClass().getSimpleName() + " '" + w.getMessage().getString() + "' " + w.getX() + "," + w.getY() + " "
                        + w.getWidth() + "x" + w.getHeight() + (w.visible ? "" : " (hidden)") + (w.active ? "" : " (inactive)"));
                if (w.visible) {
                    if (w.getX() < 0 || w.getY() < 0 || w.getRight() > mc.screen.width || w.getBottom() > mc.screen.height) {
                        problem("widget off screen: '" + w.getMessage().getString() + "' on " + mc.screen.getClass().getSimpleName());
                    }
                    int textW = mc.font.width(w.getMessage());
                    if (w instanceof Button && textW > w.getWidth() - 4) {
                        problem("button text wider than the button: '" + w.getMessage().getString() + "' (" + textW + " > " + (w.getWidth() - 4)
                                + ") on " + mc.screen.getClass().getSimpleName());
                    }
                    ws.add(w);
                }
            }
        }
        for (int i = 0; i < ws.size(); i++) {
            for (int j = i + 1; j < ws.size(); j++) {
                var p = ws.get(i);
                var q = ws.get(j);
                if (p.getX() < q.getRight() && q.getX() < p.getRight() && p.getY() < q.getBottom() && q.getY() < p.getBottom()) {
                    problem("widgets overlap on " + mc.screen.getClass().getSimpleName() + ": '" + p.getMessage().getString() + "' and '"
                            + q.getMessage().getString() + "'");
                }
            }
        }
    }

    private static void parkMouse(Minecraft mc) {
        mc.mouseHandler.releaseMouse();
        setCursor(mc, mc.getWindow().getScreenWidth() / 2.0, 3);
    }

    /**
     * Put the mouse at these window pixels. (1.1.0: GLFW ignores glfwSetCursorPos while the window has no focus, e.g. when
     * another window is in front during a long round; the mouse handler is then told directly.)
     */
    private static void setCursor(Minecraft mc, double x, double y) {
        long handle = mc.getWindow().handle();
        org.lwjgl.glfw.GLFW.glfwSetCursorPos(handle, x, y);
        try {
            var m = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
            m.setAccessible(true);
            m.invoke(mc.mouseHandler, handle, x, y);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // GLFW alone then
        }
    }

    private static void pressButton(Screen screen, String label) {
        if (screen == null) {
            problem("no screen open to press '" + label + "'");
            return;
        }
        for (var child : screen.children()) {
            if (child instanceof Button b && b.getMessage().getString().equals(label)) {
                b.onPress(new net.minecraft.client.input.KeyEvent(org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER, 0, 0));
                return;
            }
        }
        problem("button '" + label + "' not found on " + screen.getClass().getSimpleName());
    }

    // ------------------------------------------------------------------------------------------------ structures

    record Found(String dim, BlockPos pos, BoundingBox box, int pieces, String terrain, boolean underground, String error, BoundingBox eerste,
                 BlockPos tplPos, net.minecraft.world.level.block.Rotation tplRot) {
        Found(String dim, BlockPos pos, BoundingBox box, int pieces, String terrain, boolean underground, String error) {
            this(dim, pos, box, pieces, terrain, underground, error, box, null, null);
        }
    }

    private static void expandTpLocate(Minecraft mc, String dimArg, String structArg) {
        var server = mc.getSingleplayerServer();
        var reg = server.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        List<Identifier> ids = new ArrayList<>();
        if (structArg.equals("all")) {
            reg.keySet().stream().filter(id -> id.getNamespace().equals("guhs")).sorted().forEach(ids::add);
        } else {
            ids.add(Identifier.parse(structArg.contains(":") ? structArg : "guhs:" + structArg));
        }
        List<Action> acts = new ArrayList<>();
        acts.add(mc2 -> {
            note("structures to check: " + ids.size());
            return true;
        });
        for (Identifier id : ids) {
            // which dimensions can have it
            acts.add(server(s -> {
                List<String> dims = new ArrayList<>();
                Holder<Structure> holder = s.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, id)).orElse(null);
                if (holder == null) {
                    return dims;
                }
                for (ServerLevel level : s.getAllLevels()) {
                    String d = level.dimension().identifier().toString();
                    if (!dimArg.equals("auto") && !d.equals(dimArg)) {
                        continue;
                    }
                    if (!level.getChunkSource().getGeneratorState().getPlacementsForStructure(holder).isEmpty()) {
                        dims.add(d);
                    }
                }
                return dims;
            }, dims -> {
                if (dims.isEmpty()) {
                    problem("STRUCTURE " + id + ": no placement in any dimension" + (dimArg.equals("auto") ? "" : " (" + dimArg + ")")
                            + " -> not generated naturally (or no structure_set / biome tag)");
                    return;
                }
                List<Action> sub = new ArrayList<>();
                for (String d : dims) {
                    if (structureDone(id, d)) {
                        skipped++;
                        continue;
                    }
                    sub.addAll(structureShots(id, d, dims.size() > 1));
                }
                for (int i = sub.size() - 1; i >= 0; i--) {
                    queue.addFirst(sub.get(i));
                }
            }));
        }
        for (int i = acts.size() - 1; i >= 0; i--) {
            queue.addFirst(acts.get(i));
        }
    }

    private static List<Action> structureShots(Identifier id, String dim, boolean suffixDim) {
        String base = "structure_" + id.getPath() + (suffixDim ? "__" + Identifier.parse(dim).getPath() : "");
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, Identifier.parse(dim));
        Found[] found = {null};
        String[] okKey = {null};
        List<Action> acts = new ArrayList<>();
        long[] t0 = {0};
        acts.add(mc -> {
            t0[0] = System.currentTimeMillis();
            return true;
        });
        acts.add(server(s -> locate(s, id, dimKey), f -> {
            found[0] = f;
            long ms = System.currentTimeMillis() - t0[0];
            if (f.error != null) {
                problem("STRUCTURE " + id + " in " + dim + ": " + f.error + " (" + ms + " ms)");
            } else {
                BoundingBox b = f.box;
                note(String.format("STRUCTURE %s in %s at %s: box %d..%d x %d..%d y %d..%d z (size %dx%dx%d), %d pieces, %s%s (locate %d ms)",
                        id, dim, f.pos.toShortString(), b.minX(), b.maxX(), b.minY(), b.maxY(), b.minZ(), b.maxZ(),
                        b.getXSpan(), b.getYSpan(), b.getZSpan(), f.pieces, f.terrain, f.underground ? " UNDERGROUND" : "", ms));
                // does it overlap a structure found before (same dimension)?
                boolean bad = false;
                for (var e : boxes.entrySet()) {
                    String other = e.getKey();
                    if (!other.endsWith("@" + dim) || other.equals(id + "@" + dim)) {
                        continue;
                    }
                    BoundingBox o = e.getValue();
                    if (o.intersects(b)) {
                        bad = true;
                        int x0 = Math.max(o.minX(), b.minX()), x1 = Math.min(o.maxX(), b.maxX());
                        int y0 = Math.max(o.minY(), b.minY()), y1 = Math.min(o.maxY(), b.maxY());
                        int z0 = Math.max(o.minZ(), b.minZ()), z1 = Math.min(o.maxZ(), b.maxZ());
                        problem(String.format("OVERLAP in %s: %s and %s overlap (bounding boxes) at x %d..%d y %d..%d z %d..%d (%dx%dx%d) -> screenshots %s_*",
                                dim, id, other.substring(0, other.indexOf('@')), x0, x1, y0, y1, z0, z1, x1 - x0 + 1, y1 - y0 + 1, z1 - z0 + 1, base));
                    }
                }
                if (f.tplRot != null) {
                    note("  start piece (template) at " + f.tplPos.toShortString() + ", " + f.tplRot + " (camtpl / looktpl follow it)");
                }
                boxes.put(id + "@" + dim, b);
                lastBox = b;
                lastTpl = f.eerste;
                lastTplPos = f.tplPos;
                lastTplRot = f.tplRot;
                lastDim = dimKey;
                int top = levelTop(dim), bottom = levelBottom(dim);
                if (b.maxY() >= top || b.minY() < bottom) {
                    bad = true;
                    problem(String.format("CUT OFF: %s in %s: box y %d..%d goes outside the build height %d..%d of the dimension -> screenshots %s_*",
                            id, dim, b.minY(), b.maxY(), bottom, top - 1, base));
                }
                okKey[0] = bad ? null : String.format("structure:%s@%s %d %d %d %d %d %d", id, dim, b.minX(), b.maxX(), b.minY(), b.maxY(), b.minZ(), b.maxZ());
            }
        }));
        // the camera actions are decided once the structure is found
        acts.add(mc -> {
            Found f = found[0];
            if (f == null || f.error != null) {
                return true;
            }
            BoundingBox b = f.box;
            Vec3 c = new Vec3((b.minX() + b.maxX() + 1) / 2.0, (b.minY() + b.maxY() + 1) / 2.0, (b.minZ() + b.maxZ() + 1) / 2.0);
            double size = Math.max(Math.max(b.getXSpan(), b.getZSpan()), b.getYSpan());
            double d = Math.max(18, Math.min(110, size * 0.8 + 10));
            List<Action> sub = new ArrayList<>();
            // overview: from the south-east, above
            Vec3 cam = c.add(d * 0.62, d * 0.55, d * 0.62);
            cameraShot(sub, dimKey, cam, c, base + "_1_overview");
            // the other side, lower (to see foundations: floating / buried)
            if (!f.underground) {
                Vec3 cam2 = c.add(-d * 0.75, Math.max(4, d * 0.18), -d * 0.45);
                cameraShot(sub, dimKey, cam2, c, base + "_2_side");
            }
            if (size > 96) {
                Vec3 cam3 = c.add(14, 16, 14);
                cameraShot(sub, dimKey, cam3, c, base + "_3_close");
            }
            if (f.underground) {
                // spectator inside the rock: look into the cavities ("x-ray")
                Vec3 inside = new Vec3(b.minX() + b.getXSpan() * 0.1, c.y + b.getYSpan() * 0.25, b.minZ() + b.getZSpan() * 0.1);
                cameraShot(sub, dimKey, inside, c, base + "_4_inside_xray", false);
            }
            if (okKey[0] != null) {
                sub.add(mc2 -> {
                    markDone(okKey[0]);
                    return true;
                });
            }
            for (int i = sub.size() - 1; i >= 0; i--) {
                queue.addFirst(sub.get(i));
            }
            return true;
        });
        return acts;
    }

    private static ServerLevel levelOf(String dim) {
        var server = Minecraft.getInstance().getSingleplayerServer();
        return server == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(dim)));
    }

    private static int levelTop(String dim) {
        ServerLevel l = levelOf(dim);
        return l == null ? Integer.MAX_VALUE : l.getMaxY() + 1;
    }

    private static int levelBottom(String dim) {
        ServerLevel l = levelOf(dim);
        return l == null ? Integer.MIN_VALUE : l.getMinY();
    }

    private static void cameraShot(List<Action> sub, ResourceKey<Level> dim, Vec3 cam, Vec3 target, String name) {
        cameraShot(sub, dim, cam, target, name, true);
    }

    /** aboveGround: lift the camera out of terrain/fluids (not in dimensions with a roof), so views aren't x-ray by accident. */
    private static void cameraShot(List<Action> sub, ResourceKey<Level> dim, Vec3 cam, Vec3 target, String name, boolean aboveGround) {
        if (aboveGround) {
            sub.add(server(s -> {
                ServerLevel level = s.getLevel(dim);
                if (level == null || level.dimensionType().hasCeiling()) {
                    return cam;
                }
                BlockPos p = BlockPos.containing(cam);
                level.getChunk(p.getX() >> 4, p.getZ() >> 4);
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
                return cam.y < h + 3 ? new Vec3(cam.x, h + 3, cam.z) : cam;
            }, c -> {
                float[] r = lookAt(c, target);
                queue.addFirst(teleport(dim, c, r[0], r[1]));
            }));
        } else {
            float[] r = lookAt(cam, target);
            sub.add(teleport(dim, cam, r[0], r[1]));
        }
        sub.add(waitTicks(10));
        sub.add(waitRender(300));
        sub.add(shot(name, false));
    }

    private static Found locate(net.minecraft.server.MinecraftServer s, Identifier id, ResourceKey<Level> dim) {
        ServerLevel level = s.getLevel(dim);
        Holder<Structure> holder = s.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(ResourceKey.create(Registries.STRUCTURE, id)).orElse(null);
        if (level == null || holder == null) {
            return new Found(dim.identifier().toString(), null, null, 0, "", false, "unknown dimension/structure");
        }
        BlockPos origin = new BlockPos(0, 64, 0);
        String lastErr = "not found within 100 chunks (like /locate)";
        for (int attempt = 0; attempt < 4; attempt++) {
            var pair = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), origin, 100, false);
            if (pair == null) {
                break;
            }
            BlockPos pos = pair.getFirst();
            StructureStart start = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4, ChunkStatus.STRUCTURE_STARTS).getStartForStructure(holder.value());
            if (start == null || !start.isValid()) {
                lastErr = "/locate points to " + pos.toShortString() + " but no valid start there (attempt " + (attempt + 1) + ")";
                origin = pos.offset(800, 0, 800); // look elsewhere
                continue;
            }
            BoundingBox b = start.getBoundingBox();
            // make the chunks real (FULL) around the box so heights are known
            int cx = (b.minX() + b.maxX()) / 2, cz = (b.minZ() + b.maxZ()) / 2;
            StringBuilder terrain = new StringBuilder("terrain around (surface y at 4 sides, 3 blocks outside):");
            int[][] pts = {{cx, b.minZ() - 3}, {cx, b.maxZ() + 3}, {b.minX() - 3, cz}, {b.maxX() + 3, cz}};
            int lowest = Integer.MAX_VALUE, highest = Integer.MIN_VALUE;
            for (int[] p : pts) {
                level.getChunk(p[0] >> 4, p[1] >> 4);
                int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, p[0], p[1]);
                terrain.append(' ').append(h);
                lowest = Math.min(lowest, h);
                highest = Math.max(highest, h);
            }
            level.getChunk(cx >> 4, cz >> 4);
            int centreSurface = level.getHeight(Heightmap.Types.WORLD_SURFACE, cx, cz);
            terrain.append("; surface at centre ").append(centreSurface);
            boolean ceiling = level.dimensionType().hasCeiling();
            if (ceiling) {
                terrain.append(" (dimension has a ceiling: heights are the roof)");
            } else {
                if (b.minY() > highest + 3) {
                    terrain.append(" -> box bottom ").append(b.minY()).append(" is ABOVE all terrain around (floating?)");
                }
                if (b.maxY() < lowest - 2) {
                    terrain.append(" -> box top below terrain (buried / underground)");
                }
            }
            boolean underground = ceiling || b.maxY() < centreSurface - 3;
            // (3.0 QA) the start piece's own box = the template: the structure box grows 12 on every side with terrain adaptation
            BoundingBox eerste = start.getPieces().isEmpty() ? b : start.getPieces().get(0).getBoundingBox();
            BlockPos tplPos = null;
            net.minecraft.world.level.block.Rotation tplRot = null;
            if (!start.getPieces().isEmpty() && start.getPieces().get(0) instanceof net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece pe) {
                tplPos = pe.getPosition();
                tplRot = pe.getRotation();
            }
            return new Found(dim.identifier().toString(), pos, b, start.getPieces().size(), terrain.toString(), underground, null, eerste, tplPos, tplRot);
        }
        return new Found(dim.identifier().toString(), null, null, 0, "", false, lastErr);
    }

    // ------------------------------------------------------------------------------------------------ biomes

    private static void expandTpBiome(Minecraft mc, String dimArg, String biomeArg) {
        var server = mc.getSingleplayerServer();
        List<Action> acts = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            String d = level.dimension().identifier().toString();
            if (!dimArg.equals("auto") && !d.equals(dimArg)) {
                continue;
            }
            var possible = level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
            for (Holder<Biome> h : possible) {
                Identifier id = h.unwrapKey().map(ResourceKey::identifier).orElse(null);
                if (id == null) {
                    continue;
                }
                boolean want = biomeArg.equals("all") ? id.getNamespace().equals("guhs") : id.toString().equals(biomeArg) || id.getPath().equals(biomeArg);
                if (want && !isDone("biome:" + d + ":" + id)) {
                    acts.addAll(biomeShot(level.dimension(), id));
                }
            }
        }
        if (acts.isEmpty()) {
            problem("tpbiome " + dimArg + " " + biomeArg + ": no such biome in that dimension's biome source");
        }
        for (int i = acts.size() - 1; i >= 0; i--) {
            queue.addFirst(acts.get(i));
        }
    }

    private static List<Action> biomeShot(ResourceKey<Level> dim, Identifier biome) {
        List<Action> acts = new ArrayList<>();
        Vec3[] cam = {null};
        float[] rot = {0, 0};
        acts.add(server(s -> {
            ServerLevel level = s.getLevel(dim);
            var key = ResourceKey.create(Registries.BIOME, biome);
            var pair = level.findClosestBiome3d(h -> h.is(key), new BlockPos(0, 64, 0), 6400, 32, 64);
            if (pair == null) {
                // cave biomes: a finer (slower) search
                pair = level.findClosestBiome3d(h -> h.is(key), new BlockPos(0, 64, 0), 3200, 16, 8);
            }
            if (pair == null) {
                return null;
            }
            BlockPos p = pair.getFirst();
            level.getChunk(p.getX() >> 4, p.getZ() >> 4);
            if (level.dimensionType().hasCeiling()) {
                // find the biggest cave space in a few columns around it, then look the most open way
                int minY = level.getMinY() + 1, maxY = level.getMinY() + level.getLogicalHeight() - 2;
                int bestLen = -1, bestX = p.getX(), bestZ = p.getZ(), bestFloor = p.getY();
                for (int dx = -24; dx <= 24; dx += 8) {
                    for (int dz = -24; dz <= 24; dz += 8) {
                        int x = p.getX() + dx, z = p.getZ() + dz;
                        level.getChunk(x >> 4, z >> 4);
                        int run = 0;
                        for (int y = minY; y <= maxY; y++) {
                            if (level.getBlockState(new BlockPos(x, y, z)).isAir()) {
                                run++;
                                if (run > bestLen) {
                                    bestLen = run;
                                    bestX = x;
                                    bestZ = z;
                                    bestFloor = y - run + 1;
                                }
                            } else {
                                run = 0;
                            }
                        }
                    }
                }
                double camY = bestFloor + Math.max(2, Math.min(10, bestLen * 0.5));
                Vec3 camPos = new Vec3(bestX + 0.5, camY, bestZ + 0.5);
                double bestDist = -1;
                Vec3 target = camPos.add(20, -4, 20);
                for (int i = 0; i < 8; i++) {
                    double ang = Math.PI * 2 * i / 8;
                    double ddx = Math.cos(ang), ddz = Math.sin(ang);
                    int dist = 0;
                    while (dist < 48 && level.getBlockState(BlockPos.containing(camPos.x + ddx * dist, camY, camPos.z + ddz * dist)).isAir()) {
                        dist++;
                    }
                    if (dist > bestDist) {
                        bestDist = dist;
                        target = new Vec3(camPos.x + ddx * Math.max(dist, 12), camY - 3, camPos.z + ddz * Math.max(dist, 12));
                    }
                }
                return new Vec3[] {camPos, target, new Vec3(bestLen, bestFloor, bestDist)};
            }
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
            if (h <= level.getMinY() + 1) {
                h = Math.max(p.getY(), 64); // void (guheinde): look around the found height
            } else if (p.getY() < h - 16) {
                // a cave biome: the camera goes into the rock (spectator x-ray) at the found height
                return new Vec3[] {new Vec3(p.getX() + 0.5, p.getY() + 2, p.getZ() + 0.5), new Vec3(p.getX() + 24.5, p.getY() - 4, p.getZ() + 24.5),
                        new Vec3(h, 0, p.getY())};
            }
            return new Vec3[] {new Vec3(p.getX() + 0.5, h + 22, p.getZ() + 0.5), new Vec3(p.getX() + 40.5, h, p.getZ() + 40.5),
                    new Vec3(h, 0, p.getY())};
        }, r -> {
            if (r == null) {
                problem("BIOME " + biome + " in " + dim.identifier() + ": not found within 6400 blocks");
                return;
            }
            cam[0] = r[0];
            float[] lr = lookAt(r[0], r[1]);
            rot[0] = lr[0];
            rot[1] = Math.min(35, lr[1]);
            note(String.format("BIOME %s in %s: camera at %s", biome, dim.identifier(), BlockPos.containing(r[0]).toShortString()));
        }));
        acts.add(mc -> {
            if (cam[0] == null) {
                return true;
            }
            List<Action> sub = new ArrayList<>();
            sub.add(teleport(dim, cam[0], rot[0], rot[1]));
            sub.add(waitTicks(10));
            sub.add(waitRender(300));
            sub.add(shot("biome_" + dim.identifier().getPath() + "_" + biome.getPath(), false));
            sub.add(mc2 -> {
                markDone("biome:" + dim.identifier() + ":" + biome);
                return true;
            });
            for (int i = sub.size() - 1; i >= 0; i--) {
                queue.addFirst(sub.get(i));
            }
            return true;
        });
        return acts;
    }

    // ------------------------------------------------------------------------------------------------ line-ups

    record Spawnable(String label, Function<ServerLevel, Entity> make) {
    }

    private static void expandLineup(Minecraft mc, String group, BlockPos origin) {
        if (isDone("lineup:" + group)) {
            note("lineup " + group + ": already OK (done.txt), skipped");
            return;
        }
        List<Spawnable> list = new ArrayList<>();
        switch (group) {
            case "variants" -> {
                for (var v : nl.juiced.guhs.entity.GuhVariant.values()) {
                    list.add(new Spawnable(v.id() + "\n" + v.displayName().getString(), level -> {
                        CompoundTag t = new CompoundTag();
                        t.putString("Variant", v.id());
                        return fromTag(level, nl.juiced.guhs.registry.ModEntities.GUH.get(), t, false);
                    }));
                }
            }
            case "npcs" -> {
                for (var k : nl.juiced.guhs.entity.GuhNpcEntity.Kind.values()) {
                    list.add(new Spawnable("npc " + k.id(), level -> {
                        CompoundTag t = new CompoundTag();
                        t.putString("Kind", k.id());
                        return fromTag(level, nl.juiced.guhs.registry.ModEntities.GUH_NPC.get(), t, false);
                    }));
                }
            }
            case "entities", "bosses" -> {
                boolean bosses = group.equals("bosses");
                for (var e : BuiltInRegistries.ENTITY_TYPE.entrySet()) {
                    Identifier id = e.getKey().identifier();
                    if (!id.getNamespace().equals("guhs") || id.getPath().equals("guh") || id.getPath().equals("guh_npc")) {
                        continue;
                    }
                    boolean isBoss = id.getPath().contains("opper_mika") || id.getPath().contains("aangebrande_mika") || id.getPath().equals("mika_baas")
                            || id.getPath().equals("moerasheks_mika");
                    if (bosses != isBoss) {
                        continue;
                    }
                    EntityType<?> type = e.getValue();
                    list.add(new Spawnable(id.getPath(), level -> fromTag(level, type, new CompoundTag(), true)));
                }
                list.sort(java.util.Comparator.comparing(Spawnable::label));
            }
            default -> problem("unknown lineup group " + group);
        }
        note("lineup " + group + ": " + list.size() + " entries");
        List<Action> acts = new ArrayList<>();
        int perBatch = group.equals("bosses") ? 2 : 6;
        int n = 0;
        for (int i = 0; i < list.size(); i += perBatch) {
            List<Spawnable> batch = list.subList(i, Math.min(list.size(), i + perBatch));
            int batchNo = ++n;
            double[] geo = new double[3]; // centreX, distance, maxHeight
            List<Object[]> placedOnes = new ArrayList<>(); // {id, x, width, height}
            acts.add(server(s -> {
                ServerLevel level = player(s).level();
                clearTagged(level, origin, 80);
                platform(level, origin, 44, 24);
                List<String> spawned = new ArrayList<>();
                placedOnes.clear();
                double x = origin.getX() + 2;
                double maxH = 1;
                for (Spawnable sp : batch) {
                    Entity e;
                    try {
                        e = sp.make.apply(level);
                    } catch (Exception ex) {
                        spawned.add(sp.label + ": EXCEPTION " + ex);
                        continue;
                    }
                    if (e == null) {
                        spawned.add(sp.label + ": could not be created");
                        continue;
                    }
                    double w = Math.max(1.6, e.getBbWidth() + 1.4);
                    double ex = x + w / 2;
                    e.snapTo(ex, origin.getY() + 1, origin.getZ() + 0.5, 0, 0);
                    e.setYHeadRot(0);
                    if (e instanceof LivingEntity le) {
                        le.yBodyRot = 0;
                        le.yBodyRotO = 0;
                    }
                    e.addTag(TAG);
                    if (!level.addFreshEntity(e)) {
                        spawned.add(sp.label + ": addFreshEntity refused");
                    }
                    label(level, ex, origin.getY() + 1 + e.getBbHeight() + 0.35, origin.getZ() + 0.5, sp.label.replace('\n', ' ').trim(), 0.55f);
                    maxH = Math.max(maxH, e.getBbHeight());
                    placedOnes.add(new Object[] {sp.label.replace("npc ", "").trim().split("\\s+")[0], ex, (double) e.getBbWidth(), (double) e.getBbHeight(), e.getUUID()});
                    spawned.add(sp.label.replace('\n', ' ') + " (" + String.format(Locale.ROOT, "%.1fx%.1f", e.getBbWidth(), e.getBbHeight()) + ")");
                    x += w;
                }
                double span = x - (origin.getX() + 2);
                geo[0] = origin.getX() + 2 + span / 2;
                geo[1] = Math.max(span * 0.5, maxH * 1.7) + 2;
                geo[2] = maxH;
                return spawned;
            }, spawned -> note("  lineup " + group + " #" + batchNo + ": " + String.join(", ", spawned))));
            acts.add(waitTicks(30));
            // which ones removed themselves (discard in their own tick)?
            acts.add(server(s -> {
                ServerLevel level = player(s).level();
                List<String> gone = new ArrayList<>();
                for (Object[] one : placedOnes) {
                    Entity e = level.getEntity((java.util.UUID) one[4]);
                    if (e == null || !e.isAlive()) {
                        gone.add((String) one[0]);
                    }
                }
                return gone;
            }, gone -> {
                if (!gone.isEmpty()) {
                    note("  lineup " + group + " #" + batchNo + ": GONE after 1.5 s (removed themselves, so not in the photo): " + gone);
                }
            }));
            acts.add(mc2 -> {
                List<Action> sub = new ArrayList<>();
                double y = origin.getY() + 1;
                Vec3 target = new Vec3(geo[0], y + geo[2] * 0.45, origin.getZ() + 0.5);
                Vec3 front = new Vec3(geo[0], y + geo[2] * 0.5 + 0.9, origin.getZ() + 0.5 + geo[1]);
                float[] r = lookAt(front, target);
                sub.add(teleport(null, front, r[0], r[1]));
                sub.add(waitTicks(8));
                sub.add(waitRender(200));
                sub.add(shot(String.format("lineup_%s_%02d_front", group, batchNo), false));
                // from behind-right and above: tails, backs
                Vec3 back = new Vec3(geo[0] + geo[1] * 0.35, y + geo[2] + geo[1] * 0.35, origin.getZ() + 0.5 - geo[1] * 0.85);
                float[] r2 = lookAt(back, target);
                sub.add(teleport(null, back, r2[0], r2[1]));
                sub.add(waitTicks(8));
                sub.add(shot(String.format("lineup_%s_%02d_back", group, batchNo), false));
                // side profile from the left end (x-) along the row, close
                Vec3 side = new Vec3(origin.getX() - 3.5, y + geo[2] * 0.7 + 0.5, origin.getZ() + 0.5 + 2.2);
                float[] r3 = lookAt(side, new Vec3(origin.getX() + 3, y + geo[2] * 0.35, origin.getZ() + 0.5));
                sub.add(teleport(null, side, r3[0], r3[1]));
                sub.add(waitTicks(8));
                sub.add(shot(String.format("lineup_%s_%02d_side", group, batchNo), false));
                // a close-up of each one, from behind and above (tails, backs, wings)
                for (Object[] one : placedOnes) {
                    double ex = (double) one[1], w = (double) one[2], h = (double) one[3];
                    double sz = Math.max(1.0, Math.max(w, h)) * 1.5 + 0.6;
                    Vec3 t = new Vec3(ex, y + h * 0.45, origin.getZ() + 0.5);
                    Vec3 c = new Vec3(ex + sz * 0.35, y + h * 0.5 + sz * 0.8, origin.getZ() + 0.5 - sz * 1.05);
                    float[] rc = lookAt(c, t);
                    sub.add(teleport(null, c, rc[0], rc[1]));
                    sub.add(waitTicks(4));
                    sub.add(shot(String.format("lineup_%s_%02d_close_%s", group, batchNo, one[0]), false));
                }
                for (int k = sub.size() - 1; k >= 0; k--) {
                    queue.addFirst(sub.get(k));
                }
                return true;
            });
        }
        acts.add(server(s -> {
            clearTagged(player(s).level(), origin, 80);
            return true;
        }, ok -> markDone("lineup:" + group)));
        for (int i = acts.size() - 1; i >= 0; i--) {
            queue.addFirst(acts.get(i));
        }
    }

    private static Entity fromTag(ServerLevel level, EntityType<?> type, CompoundTag extra, boolean finalize) {
        CompoundTag t = extra.copy();
        t.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        t.putBoolean("NoAI", true);
        t.putBoolean("PersistenceRequired", true);
        t.putBoolean("Silent", true);
        t.putBoolean("Invulnerable", true);
        Entity e = EntityType.loadEntityRecursive(t, level, EntitySpawnReason.LOAD, en -> en);
        if (e == null) {
            return null;
        }
        if (finalize && e instanceof Mob mob) {
            try {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(mob.position())), EntitySpawnReason.COMMAND, null);
            } catch (Exception ex) {
                problem("finalizeSpawn " + type + ": " + ex);
            }
            mob.setNoAi(true);
        }
        if (!(e instanceof LivingEntity)) {
            e.setNoGravity(true);
        }
        return e;
    }

    private static void clearTagged(ServerLevel level, BlockPos origin, int r) {
        for (Entity e : level.getEntities((Entity) null, new AABB(origin).inflate(r), en -> en.entityTags().contains(TAG))) {
            e.discard();
        }
        // also whatever the batch left behind (projectiles, items, xp...)
        for (Entity e : level.getEntities((Entity) null, new AABB(origin).inflate(r), en -> !(en instanceof ServerPlayer))) {
            e.discard();
        }
    }

    private static void platform(ServerLevel level, BlockPos o, int w, int d) {
        BlockState floor = Blocks.SMOOTH_STONE.defaultBlockState();
        for (int x = -6; x < w; x++) {
            for (int z = -6; z < d; z++) {
                level.setBlock(o.offset(x, 0, z), floor, 2);
                for (int y = 1; y < 12; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (!level.getBlockState(p).isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
    }

    private static void label(ServerLevel level, double x, double y, double z, String text, float scale) {
        CompoundTag t = new CompoundTag();
        t.putString("id", "minecraft:text_display");
        // (1.1.0: text components are stored as NBT, not as a JSON string)
        t.store("text", net.minecraft.network.chat.ComponentSerialization.CODEC,
                level.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), Component.literal(text));
        t.putString("billboard", "center");
        t.putString("alignment", "center");
        t.putInt("background", 0x90000000);
        CompoundTag tr = new CompoundTag();
        tr.put("left_rotation", floats(0, 0, 0, 1));
        tr.put("right_rotation", floats(0, 0, 0, 1));
        tr.put("translation", floats(0, 0, 0));
        tr.put("scale", floats(scale, scale, scale));
        t.put("transformation", tr);
        Entity e = EntityType.loadEntityRecursive(t, level, EntitySpawnReason.LOAD, en -> en);
        if (e != null) {
            e.snapTo(x, y, z, 0, 0);
            e.addTag(TAG);
            level.addFreshEntity(e);
        }
    }

    private static ListTag floats(float... v) {
        ListTag l = new ListTag();
        for (float f : v) {
            l.add(FloatTag.valueOf(f));
        }
        return l;
    }

    // ------------------------------------------------------------------------------------------------ block wall

    private static void expandBlockWall(Minecraft mc, BlockPos o) {
        if (isDone("blockwall")) {
            note("blockwall: already OK (done.txt), skipped");
            return;
        }
        queue.addFirst(mc2 -> {
            markDone("blockwall");
            return true;
        });
        final int cols = 8, rowsPerShot = 3, colsPerShot = 8;
        List<Block> blocks = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            if (e.getKey().identifier().getNamespace().equals("guhs")) {
                if (e.getValue() instanceof LiquidBlock) {
                    skipped.add(e.getKey().identifier().getPath() + " (fluid)");
                } else {
                    blocks.add(e.getValue());
                }
            }
        }
        blocks.sort(java.util.Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).getPath()));
        int rows = (blocks.size() + cols - 1) / cols;
        note("blockwall: " + blocks.size() + " blocks in " + rows + " rows of " + cols + (skipped.isEmpty() ? "" : "; skipped " + skipped));
        List<Action> acts = new ArrayList<>();
        // build in parts of 3 rows: the wall is at most 3 rows high each time (so it stays in a small area near daylight)
        for (int r0 = 0; r0 < rows; r0 += rowsPerShot) {
            int first = r0 * cols;
            List<Block> part = blocks.subList(first, Math.min(blocks.size(), first + rowsPerShot * cols));
            int partNo = r0 / rowsPerShot + 1;
            acts.add(server(s -> {
                ServerLevel level = player(s).level();
                clearTagged(level, o, 40);
                // clear space, backing wall (z-1), floor
                for (int x = -3; x < cols * 3 + 3; x++) {
                    for (int y = -1; y < rowsPerShot * 4 + 3; y++) {
                        for (int z = -1; z < 16; z++) {
                            BlockState st = z == -1 ? Blocks.WHITE_CONCRETE.defaultBlockState()
                                    : y == -1 ? Blocks.SMOOTH_STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                            level.setBlock(o.offset(x, y, z), st, 2 | 16);
                        }
                    }
                }
                List<String> placed = new ArrayList<>();
                for (int i = 0; i < part.size(); i++) {
                    Block b = part.get(i);
                    int c = i % cols, row = rowsPerShot - 1 - i / cols;
                    BlockPos p = o.offset(c * 3 + 1, row * 4 + 1, 0);
                    level.setBlock(p.below(), Blocks.SMOOTH_STONE.defaultBlockState(), 2 | 16);
                    BlockState st = b.defaultBlockState();
                    if (st.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                        st = st.setValue(BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.SOUTH);
                    }
                    try {
                        level.setBlock(p, st, 2 | 16);
                        if (st.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                            level.setBlock(p.above(), st.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER), 2 | 16);
                        }
                        if (st.hasProperty(BlockStateProperties.BED_PART)) {
                            level.setBlock(p.south(), st.setValue(BlockStateProperties.BED_PART, BedPart.HEAD), 2 | 16);
                        }
                    } catch (Exception ex) {
                        placed.add(BuiltInRegistries.BLOCK.getKey(b).getPath() + ": EXCEPTION " + ex);
                    }
                    label(level, p.getX() + 0.5, p.getY() - 0.75, p.getZ() + 1.15, BuiltInRegistries.BLOCK.getKey(b).getPath(), 0.62f);
                    placed.add(BuiltInRegistries.BLOCK.getKey(b).getPath());
                }
                return placed;
            }, placed -> note("  blockwall part " + partNo + ": " + String.join(", ", placed))));
            acts.add(waitTicks(10));
            double cy = o.getY() + rowsPerShot * 4 / 2.0;
            for (int half = 0; half < 2; half++) {
                // each half: 4 columns x 3 rows, close enough to read the labels
                double hx = o.getX() + half * cols * 3 / 2.0 + cols * 3 / 4.0 + 0.5;
                Vec3 cam = new Vec3(hx, cy + 0.5, o.getZ() + 8.5);
                float[] r = lookAt(cam, new Vec3(hx, cy - 0.5, o.getZ()));
                acts.add(teleport(null, cam, r[0], r[1]));
                acts.add(waitTicks(4));
                if (half == 0) {
                    acts.add(waitRender(200));
                }
                acts.add(shot(String.format("blocks_%02d%s", partNo, half == 0 ? "a" : "b"), false));
            }
            double cx = o.getX() + cols * 3 / 2.0;
            Vec3 cam2 = new Vec3(cx - 9, cy + 5, o.getZ() + 11);
            float[] r2 = lookAt(cam2, new Vec3(cx - 2, cy - 1, o.getZ()));
            acts.add(teleport(null, cam2, r2[0], r2[1]));
            acts.add(waitTicks(6));
            acts.add(shot(String.format("blocks_%02d_angle", partNo), false));
        }
        for (int i = acts.size() - 1; i >= 0; i--) {
            queue.addFirst(acts.get(i));
        }
    }

    // ------------------------------------------------------------------------------------------------ model check

    /** Every guhs block state and item: missing model (purple/black cube) or quads with the missing texture? */
    private static void modelCheck(Minecraft mc) {
        // (1.1.0: block state models come from ModelManager#getBlockStateModelSet, items from their client item definition)
        var blockModels = mc.getModelManager().getBlockStateModelSet();
        var missingModel = blockModels.missingModel();
        var missingItem = mc.getModelManager().getItemModel(Identifier.fromNamespaceAndPath("guhs", "autocheck_surely_missing"));
        Identifier missingTex = net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation();
        var rnd = net.minecraft.util.RandomSource.create(1);
        int states = 0, items = 0, bad = 0;
        for (var e : BuiltInRegistries.BLOCK.entrySet()) {
            if (!e.getKey().identifier().getNamespace().equals("guhs")) {
                continue;
            }
            java.util.Set<String> seen = new java.util.TreeSet<>();
            for (BlockState st : e.getValue().getStateDefinition().getPossibleStates()) {
                states++;
                if (st.getRenderShape() != net.minecraft.world.level.block.RenderShape.MODEL) {
                    continue;
                }
                var model = blockModels.get(st);
                String what = null;
                if (model == missingModel) {
                    what = "MISSING MODEL";
                } else if (model.particleMaterial().sprite().contents().name().equals(missingTex)) {
                    what = "missing particle texture";
                } else {
                    List<net.minecraft.client.renderer.block.dispatch.BlockStateModelPart> parts = new ArrayList<>();
                    model.collectParts(rnd, parts);
                    List<net.minecraft.client.resources.model.geometry.BakedQuad> quads = new ArrayList<>();
                    for (var part : parts) {
                        quads.addAll(part.getQuads(null));
                        for (var dir : net.minecraft.core.Direction.values()) {
                            quads.addAll(part.getQuads(dir));
                        }
                    }
                    for (var q : quads) {
                        if (q.materialInfo().sprite().contents().name().equals(missingTex)) {
                            what = "MISSING TEXTURE on a face";
                            break;
                        }
                    }
                }
                if (what != null && seen.add(what)) {
                    bad++;
                    problem("BLOCK " + e.getKey().identifier() + " " + what + " (e.g. state " + st + ") -> check assets/guhs/blockstates|models");
                }
            }
        }
        for (var e : BuiltInRegistries.ITEM.entrySet()) {
            if (!e.getKey().identifier().getNamespace().equals("guhs")) {
                continue;
            }
            items++;
            var stack = new net.minecraft.world.item.ItemStack(e.getValue());
            Identifier modelId = stack.get(net.minecraft.core.component.DataComponents.ITEM_MODEL);
            String what = null;
            if (modelId == null || mc.getModelManager().getItemModel(modelId) == missingItem) {
                what = "MISSING MODEL";
            } else {
                var state = new net.minecraft.client.renderer.item.ItemStackRenderState();
                mc.getItemModelResolver().updateForTopItem(state, stack, net.minecraft.world.item.ItemDisplayContext.GUI, mc.level, mc.player, 0);
                var particle = state.pickParticleMaterial(rnd);
                if (particle != null && particle.sprite().contents().name().equals(missingTex)) {
                    what = "MISSING TEXTURE";
                }
            }
            if (what != null) {
                bad++;
                problem("ITEM " + e.getKey().identifier() + " " + what + " -> check assets/guhs/items + models/item");
            }
        }
        note("modelcheck: " + states + " block states, " + items + " items checked, " + bad + " with missing model/texture");
    }

    // ------------------------------------------------------------------------------------------------ report / quit

    static void note(String s) {
        report.add(s);
        LOG.info("[autocheck] {}", s);
    }

    static void problem(String s) {
        problems.add(s);
        report.add("!! " + s);
        LOG.warn("[autocheck] PROBLEM {}", s);
        writeReport(false);
    }

    private static void startQuit(Minecraft mc) {
        if (state == State.QUITTING) {
            return;
        }
        note("");
        note("Round finished after " + (System.currentTimeMillis() - startMillis) / 1000 + " s, " + shots + " screenshots, "
                + skipped + " items skipped (already OK in done.txt).");
        state(State.QUITTING);
        opened = false;
        lockYaw = null;
        writeReport(true);
        try {
            if (mc.level != null) {
                mc.level.disconnect(Component.literal("AutoCheck"));
            }
            mc.disconnect(new net.minecraft.client.gui.screens.GenericMessageScreen(Component.literal("AutoCheck: saving...")), false);
        } catch (Exception e) {
            LOG.error("AutoCheck disconnect", e);
        }
        stopDelay = Boolean.getBoolean("guhs.autocheck.stay") ? -1 : 20;
    }

    private static synchronized void writeReport(boolean finalReport) {
        if (outDir == null) {
            return;
        }
        List<String> out = new ArrayList<>();
        out.add("Guhs AutoCheck report" + (finalReport ? "" : " (in progress)"));
        out.add("");
        out.add("PROBLEMS (" + problems.size() + "):");
        problems.forEach(p -> out.add("  - " + p));
        out.add("");
        File[] crashes = new File(outDir.getParentFile().getParentFile(), "crash-reports").listFiles();
        if (crashes != null) {
            for (File c : crashes) {
                if (c.lastModified() >= startMillis) {
                    out.add("CRASH REPORT during this round: " + c.getAbsolutePath());
                }
            }
        }
        out.add("");
        out.add("LOG FINDINGS (Missing / Unable to load / Exception / Failed to load model / missing texture):");
        out.addAll(LogScan.summary());
        out.add("");
        out.add("RUN LOG:");
        out.addAll(report);
        try {
            Files.write(new File(outDir, "report.txt").toPath(), out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("AutoCheck: cannot write report", e);
        }
    }

    /**
     * WORKAROUND (autocheck only, not a fix): GuhGezin.closeFamilies iterates its FORMING list while spawnBaby ->
     * finalizeMobSpawn -> join adds to that same list, which throws a ConcurrentModificationException and crashes the
     * server (seen in the first round). So the round can go on, the lists are swapped for one whose iterator walks a
     * copy; every time that happens it is reported as a problem.
     */
    static final class GezinGuard {
        private static java.lang.reflect.Field field;
        private static boolean failed;
        private static int reported;

        static void onLevelTick(net.neoforged.neoforge.event.tick.LevelTickEvent.Pre event) {
            if (failed || !(event.getLevel() instanceof ServerLevel level)) {
                return;
            }
            try {
                if (field == null) {
                    field = Class.forName("nl.juiced.guhs.feature.vadswoud.GuhGezin").getDeclaredField("FORMING");
                    field.setAccessible(true);
                    note("GezinGuard active: GuhGezin.FORMING lists get a copy-iterator (see report for CME hits)");
                }
                @SuppressWarnings("unchecked")
                Map<ResourceKey<Level>, List<Object>> map = (Map<ResourceKey<Level>, List<Object>>) field.get(null);
                List<Object> list = map.get(level.dimension());
                if (!(list instanceof SafeList)) {
                    SafeList safe = new SafeList(level.dimension().identifier().toString());
                    if (list != null) {
                        safe.addAll(list);
                    }
                    map.put(level.dimension(), safe);
                }
            } catch (Throwable t) {
                failed = true;
                problem("GezinGuard could not be installed: " + t);
            }
        }

        static final class SafeList extends ArrayList<Object> {
            private final String dim;

            SafeList(String dim) {
                this.dim = dim;
            }

            @Override
            public java.util.Iterator<Object> iterator() {
                Object[] snapshot = toArray();
                int sizeAtStart = size();
                return new java.util.Iterator<>() {
                    int i;
                    Object last;
                    int removed;

                    @Override
                    public boolean hasNext() {
                        boolean more = i < snapshot.length;
                        if (!more && size() != sizeAtStart - removed && reported < 10) {
                            reported++;
                            problem("BUG GuhGezin.closeFamilies (feature/vadswoud/GuhGezin.java:181): the FORMING list of " + dim
                                    + " changed while it was iterated (spawnBaby -> finalizeMobSpawn -> join adds a family) -> "
                                    + "without the autocheck guard this is a ConcurrentModificationException = server crash");
                        }
                        return more;
                    }

                    @Override
                    public Object next() {
                        last = snapshot[i++];
                        return last;
                    }

                    @Override
                    public void remove() {
                        for (int k = 0; k < size(); k++) {
                            if (get(k) == last) {
                                SafeList.this.remove(k);
                                removed++;
                                return;
                            }
                        }
                    }
                };
            }
        }
    }

    /** Collects suspicious log lines (from the start of the game) through a log4j appender. */
    static final class LogScan {
        private static final String[] PATTERNS = {"missing", "unable to load", "exception", "failed to load model", "missing texture",
                "couldn't load", "could not load", "failed to load", "error"};
        private static final Map<String, int[]> lines = new LinkedHashMap<>();

        static void install() {
            try {
                var ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
                var appender = new org.apache.logging.log4j.core.appender.AbstractAppender("GuhsAutoCheck", null, null, true,
                        org.apache.logging.log4j.core.config.Property.EMPTY_ARRAY) {
                    @Override
                    public void append(org.apache.logging.log4j.core.LogEvent event) {
                        if (event.getLoggerName().equals("GuhsAutoCheck")) {
                            return;
                        }
                        if (event.getLevel().isLessSpecificThan(org.apache.logging.log4j.Level.INFO)) {
                            return;
                        }
                        String msg = event.getMessage() == null ? "" : event.getMessage().getFormattedMessage();
                        Throwable t = event.getThrown();
                        String full = msg + (t != null ? " :: " + t : "");
                        String low = full.toLowerCase(Locale.ROOT);
                        boolean hit = t != null;
                        for (String p : PATTERNS) {
                            hit |= low.contains(p);
                        }
                        if (!hit) {
                            return;
                        }
                        String key = "[" + event.getLevel() + "] [" + shortLogger(event.getLoggerName()) + "] "
                                + (full.length() > 600 ? full.substring(0, 600) + "..." : full);
                        synchronized (lines) {
                            lines.computeIfAbsent(key, k -> new int[1])[0]++;
                        }
                    }
                };
                appender.start();
                ctx.getConfiguration().getRootLogger().addAppender(appender, org.apache.logging.log4j.Level.INFO, null);
                ctx.updateLoggers();
            } catch (Throwable t) {
                LOG.error("AutoCheck: could not install the log scanner", t);
            }
        }

        private static String shortLogger(String name) {
            int i = name.lastIndexOf('.');
            return i >= 0 ? name.substring(i + 1) : name;
        }

        static List<String> summary() {
            List<String> guhs = new ArrayList<>(), other = new ArrayList<>();
            synchronized (lines) {
                for (var e : lines.entrySet()) {
                    String s = "  " + (e.getValue()[0] > 1 ? "(" + e.getValue()[0] + "x) " : "") + e.getKey();
                    (s.toLowerCase(Locale.ROOT).contains("guh") ? guhs : other).add(s);
                }
            }
            List<String> out = new ArrayList<>();
            out.add(" guhs-related (" + guhs.size() + "):");
            out.addAll(guhs);
            out.add(" other (" + other.size() + "):");
            out.addAll(other.size() > 250 ? other.subList(0, 250) : other);
            return out;
        }
    }

}
