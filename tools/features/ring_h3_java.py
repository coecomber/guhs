"""
bbq2 (ring-h3) - writes the two Java files of feature/ringh3 that only repeat what the python side decides:

  Plekken.java   where everything is in the template (ring_h3_bouw.PLEKKEN, the doors, the levers, the rune stones, the cast)
  Scenes.java    the two camera scenes (ring_h3_scene) as Cutscene builders of the verhaal engine

  python tools/features/ring_h3_java.py          (from the worktree root) writes both files
  check()                                        (the build, ring_h3.selfcheck) fails when a file on disk is not what this module would
                                                 write: change the python, run this, commit both.
So the numbers exist once, in python, where the template is built and the pictures are drawn.
"""
import os
import sys

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "ringh3")
KOP = "// WRITTEN BY tools/features/ring_h3_java.py - do not edit by hand: change ring_h3_bouw.py / ring_h3_scene.py and run that tool again.\n"


def _f(v):
    """A double literal, short."""
    s = f"{float(v):.3f}".rstrip("0")
    return s + "0" if s.endswith(".") else s


def plekken():
    from features import ring_h3_bouw as B
    r = [KOP, "package nl.juiced.guhs.feature.ringh3;\n", "import java.util.List;\n", "import javax.annotation.Nullable;\n",
         "import net.minecraft.core.BlockPos;", "import net.minecraft.core.Vec3i;\n",
         "/**",
         " * bbq2 (ring-h3): where everything is in the template of De Mijnen van Knabbelmoria (template coordinates: {@link Mijn} turns",
         " * them into the world for a copy). The numbers come from tools/features/ring_h3_bouw.py, which builds the template.",
         " */",
         "public final class Plekken {",
         "    /** A box of template blocks, both corners included. */",
         "    public record Doos(int x0, int y0, int z0, int x1, int y1, int z1) {",
         "        public boolean binnen(Vec3i p) {",
         "            return p.getX() >= x0 && p.getX() <= x1 && p.getY() >= y0 && p.getY() <= y1 && p.getZ() >= z0 && p.getZ() <= z1;",
         "        }\n",
         "        public BlockPos hoek() {",
         "            return new BlockPos(x0, y0, z0);",
         "        }\n",
         "        public BlockPos midden() {",
         "            return new BlockPos((x0 + x1) / 2, y0, (z0 + z1) / 2);",
         "        }",
         "    }\n",
         "    /** A character of the cast in the template: only there for players whose step of ring_h3 is van..tot. */",
         "    public record Rol(String kind, String id, BlockPos plek, float yaw, @Nullable String rol, int van, int tot) {",
         "    }\n",
         f"    /** The template y of the top block of the cave floor, and the three walking levels. */",
         f"    public static final int G = {B.G}, BOVEN = {B.BOVEN}, MIDDEL = {B.MIDDEL}, DIEP = {B.DIEP};"]
    for naam, v in B.PLEKKEN.items():
        if len(v) == 3:
            soort = "Vec3i" if naam == "MAAT" else "BlockPos"
            r.append(f"    public static final {soort} {naam} = new {soort}({v[0]}, {v[1]}, {v[2]});")
        else:
            r.append(f"    public static final Doos {naam} = new Doos({', '.join(str(i) for i in v)});")
    r.append("    // the doors: the blocks that slide away (templates guhs:ringh3_deur_<name>)")
    for naam, v in B.DEUREN.items():
        r.append(f"    public static final Doos DEUR_{naam.upper()} = new Doos({', '.join(str(i) for i in v)});")
    hx = ", ".join(f"new BlockPos({x}, {B.MIDDEL + 1}, {B.HEFBOOMHAL[2] - 1})" for x in B.HENDEL_X)
    r.append("    /** The four levers of the lever hall (lever nr = index), and the order they want to be pulled in. */")
    r.append(f"    public static final List<BlockPos> HENDELS = List.of({hx});")
    r.append(f"    public static final int[] HENDEL_VOLGORDE = {{{', '.join(str(i) for i in B.HENDEL_VOLGORDE)}}};")
    rz = ", ".join(f"new BlockPos({B.PUTKAMER[0] - 1}, {B.MIDDEL + 1}, {z})" for z in B.RUNE_Z)
    r.append("    /** The six rune stones round Gimguh's door (their teken: the block state), the one to knock on and how often. */")
    r.append(f"    public static final List<BlockPos> RUNEN = List.of({rz});")
    r.append(f"    public static final int[] RUNE_TEKENS = {{{', '.join(str(i) for i in B.RUNE_TEKENS)}}};")
    r.append(f"    public static final int RUNE_GOED = {B.RUNE_GOED}, RUNE_KLOPPEN = {B.RUNE_KLOPPEN}, RUNE_NJEG = {B.NJEG};")
    r.append("    public static final List<Rol> CAST = List.of(")
    regels = []
    for kind, id, x, y, z, yaw, plek, van, tot in B.CAST:
        rol = f'"{plek}"' if plek else "null"
        regels.append(f'            new Rol("{kind}", "{id}", new BlockPos({x}, {y}, {z}), {_f(yaw)}f, {rol}, {van}, {tot})')
    r.append(",\n".join(regels) + ");\n")
    r.append("    private Plekken() {\n    }\n}\n")
    return "\n".join(r)


def _vec(scene, p):
    ax, ay, az = scene.anker
    return f"new Vec3({_f(p[0] - ax)}, {_f(p[1] - ay)}, {_f(p[2] - az)})"


VANILLA_GELUID = {"DONDER": "SoundEvents.LIGHTNING_BOLT_THUNDER"}
# particles that are more than a field of ParticleTypes: FLITS = the flash of a firework (one great soft ball of light, gone in
# four ticks), warm white
DEELTJE = {"FLITS": "ColorParticleOption.create(ParticleTypes.FLASH, 0xFFFFF2D0)"}


def _scene(s, naam):
    r = [f'    public static final Cutscene {naam} = Cutscene.maak("{s.id}").duur({s.duur}).bij("ring_h3").kaart("ring_h3").verbergEcht({_f(s.verberg)})']
    for acteur, soort, arg, start, yaw in s.acteurs:
        if soort == "speler":
            r.append(f"            .speler({_vec(s, start)}, {_f(yaw)}f)")
        elif soort == "npc":
            r.append(f'            .npc("{acteur}", GuhNpcEntity.Kind.{arg}, {_vec(s, start)}, {_f(yaw)}f)')
        elif soort == "guh":
            r.append(f'            .guh("{acteur}", GuhVariant.{arg}, {_vec(s, start)}, {_f(yaw)}f)')
        else:
            r.append(f'            .acteur("{acteur}", RingH3Feature.{arg}, {_vec(s, start)}, {_f(yaw)}f)')
    for t, pos, kijk, volgt, knip in sorted(s.camera, key=lambda c: c[0]):
        if volgt:
            r.append(f'            .camera{"Knip" if knip else ""}Volgt({t}, {_vec(s, pos)}, "{volgt}")')
        else:
            r.append(f'            .camera{"Knip" if knip else ""}({t}, {_vec(s, pos)}, {_vec(s, kijk)})')
    for acteur, t0, t1, naar in sorted(s.lopen, key=lambda x: (x[1], x[0])):
        r.append(f'            .loop("{acteur}", {t0}, {t1}, {_vec(s, naar)})')
    for acteur, t, naar in sorted(s.kijken, key=lambda x: (x[1], x[0])):
        r.append(f'            .kijk("{acteur}", {t}, {_vec(s, naar)})')
    for acteur, t, anim in sorted(s.animaties, key=lambda x: (x[1], x[0])):
        r.append(f'            .animatie("{acteur}", {t}, "{anim}")')
    for t, spreker, key, ticks in sorted(s.zinnen):
        r.append(f'            .zeg({t}, "{spreker}", "{key}", {ticks})')
    for t, veld, volume, pitch in sorted(s.geluiden):
        bron = f"() -> {VANILLA_GELUID[veld]}" if veld in VANILLA_GELUID else f"RingH3Feature.{veld}"
        if veld in VANILLA_GELUID:
            r.append(f"            .geluid({t}, {bron}, {_f(volume)}f, {_f(pitch)}f)")
        else:
            r.append(f"            .geluid({t}, {bron}::get, {_f(volume)}f, {_f(pitch)}f)")
    for t, soort, pos, aantal, spreiding in sorted(s.deeltjes, key=lambda x: (x[0], x[1])):
        r.append(f"            .deeltjes({t}, {DEELTJE.get(soort, 'ParticleTypes.' + soort)}, {_vec(s, pos)}, {aantal}, {_f(spreiding)})")
    for t, kracht, ticks in sorted(s.schudden):
        r.append(f"            .schud({t}, {_f(kracht)}f, {ticks})")
    for t0, t1 in sorted(s.zwart):
        r.append(f"            .zwart({t0}, {t1})")
    r.append("            .registreer();")
    return "\n".join(r)


def scenes():
    from features import ring_h3_scene as S
    emmer, brug = S.emmer(), S.brug()
    flitsen = ", ".join(f"{{{t}, {ticks}, {int(round(sterkte * 100))}}}" for t, ticks, sterkte in sorted(brug.flitsen))
    r = [KOP, "package nl.juiced.guhs.feature.ringh3;\n",
         "import net.minecraft.core.particles.ColorParticleOption;", "import net.minecraft.core.particles.ParticleTypes;",
         "import net.minecraft.sounds.SoundEvents;", "import net.minecraft.world.phys.Vec3;",
         "import nl.juiced.guhs.entity.GuhNpcEntity;", "import nl.juiced.guhs.entity.GuhVariant;", "import nl.juiced.guhs.feature.verhaal.Cutscene;\n",
         "/**",
         " * bbq2 (ring-h3): the two camera scenes of the mine, as the verhaal engine wants them. Positions are relative to the scene's",
         " * anchor ({@link Plekken#PUT}, {@link Plekken#BRUG_ANKER}) in template coordinates, so they fit every copy however it is turned.",
         " * The scripts themselves, with what every beat is for, are tools/features/ring_h3_scene.py; their texts: scene.guhs.ringh3_*.",
         " */",
         "public final class Scenes {",
         f"    /** The tick of {{@link #BRUG}} at which the span breaks (the viewer's own game takes the stones away: client.BrugBreuk). */",
         f"    public static final int BRUG_BREEKT = {S.BREEKT};",
         "    /** The white flashes of {@link #BRUG}: {tick, ticks, strength in percent} (drawn by client.RingH3Client). */",
         f"    public static final int[][] FLITSEN = {{{flitsen}}};\n",
         _scene(emmer, "EMMER"), "", _scene(brug, "BRUG"), "",
         "    /** (called from RingH3Feature.register: the fields above register the scenes when this class loads) */",
         "    static void registreer() {\n    }\n",
         "    private Scenes() {\n    }\n}\n"]
    return "\n".join(r)


BESTANDEN = {"Plekken.java": plekken, "Scenes.java": scenes}


def check():
    """The problems: a Java file that is not what this module would write."""
    problems = []
    for naam, maak in BESTANDEN.items():
        pad = os.path.join(JAVA, naam)
        wil = maak()
        heeft = open(pad, encoding="utf-8").read() if os.path.exists(pad) else None
        if heeft != wil:
            problems.append(f"{pad} is stale: run python tools/features/ring_h3_java.py")
    return problems


def schrijf():
    os.makedirs(JAVA, exist_ok=True)
    for naam, maak in BESTANDEN.items():
        with open(os.path.join(JAVA, naam), "w", encoding="utf-8", newline="\n") as f:
            f.write(maak())
        print("wrote", os.path.join(JAVA, naam))


if __name__ == "__main__":
    sys.path.insert(0, "tools")
    schrijf()
