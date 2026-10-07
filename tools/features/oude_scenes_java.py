"""
bbq2 (oude-scenes) - writes the one Java file of feature/oudescenes that only repeats what the python side decides:

  Scenes.java    the six camera scenes (oude_scenes_scene) as Cutscene builders of the verhaal engine, each with what only this
                 slice's own client draws (Effecten: weather, lightning, streams of particles), its anchor in the template, and
                 the moments and spots the server side acts on

  python tools/features/oude_scenes_java.py     (from the worktree root) writes the file
  check()                                       (the build, oude_scenes.selfcheck) fails when the file on disk is not what this
                                                module would write: change the python, run this, commit both.
So the numbers exist once, in python, where they are checked against the templates and drawn.
"""
import os
import sys

JAVA = os.path.join("src", "main", "java", "nl", "juiced", "guhs", "feature", "oudescenes")
KOP = "// WRITTEN BY tools/features/oude_scenes_java.py - do not edit by hand: change oude_scenes_scene.py and run that tool again.\n"

# the sounds a scene may name: sound effects only (the mod's own and vanilla's), never music
GELUID = {
    "WIND": "BaltoFeature.WIND::get", "SNUIF": "BaltoFeature.SNUIF::get", "HUIL": "BaltoFeature.HUIL::get",
    "BELLETJES": "BaltoFeature.BELLETJES::get",
    "TANK_BORREL": "MewtwoFeature.TANK_BORREL::get", "TANK_KLIK": "MewtwoFeature.TANK_KLIK::get", "TANK_HEEL": "MewtwoFeature.TANK_HEEL::get",
    "TELEKINESE": "MewtwoFeature.TELEKINESE::get", "MEW_GIECHEL": "MewtwoFeature.MEW_GIECHEL::get",
    "STER": "HemelFeature.STER::get", "HARTKLOP": "HemelFeature.HARTKLOP::get",
    "HAMER": "BeroepenFeature.HAMER::get",
    "GUH_AMBIENT": "ModSounds.GUH_AMBIENT::get", "GUH_HAPPY": "ModSounds.GUH_HAPPY::get",
    "KLINGEL": "() -> SoundEvents.AMETHYST_BLOCK_CHIME", "DONDER": "() -> SoundEvents.LIGHTNING_BOLT_THUNDER",
    "GLAS": "() -> SoundEvents.GLASS_BREAK", "KAMPVUUR": "() -> SoundEvents.CAMPFIRE_CRACKLE",
    "VUURSTEEN": "() -> SoundEvents.FLINTANDSTEEL_USE", "VUUR": "() -> SoundEvents.FIRE_AMBIENT", "WHOEF": "() -> SoundEvents.FIRECHARGE_USE",
    "MANTEL": "() -> SoundEvents.ARMOR_EQUIP_LEATHER.value()", "VUURWERK": "() -> SoundEvents.FIREWORK_ROCKET_TWINKLE",
    "DEUR_OPEN": "() -> SoundEvents.CHERRY_WOOD_DOOR_OPEN",
}
# the engine's bursts: vanilla particles (they are made when this class loads, long before the mod's own particles exist)
DEELTJE = {"FLASH": "ColorParticleOption.create(ParticleTypes.FLASH, 0xFFF4E8FF)"}
# the streams of this slice's own client: any particle, asked for when it is needed
STROOM = {
    "WOLFGLANS": "BaltoFeature.WOLFGLANS::get", "BUBBEL": "MewtwoFeature.BUBBEL::get", "GLOED": "MewtwoFeature.GLOED::get",
    "HARTJE": "BandFeature.HARTJE::get", "STERRETJE": "HemelFeature.STERRETJE::get",
    "OOG": "() -> new DustParticleOptions(0xB45CFF, 1.25f)", "STRAAL": "() -> ParticleTypes.END_ROD",
}


def _f(v):
    """A double literal, short."""
    s = f"{float(v):.3f}".rstrip("0")
    return s + "0" if s.endswith(".") else s


def _vec(scene, p):
    ax, ay, az = scene.anker
    return f"new Vec3({_f(p[0] - ax)}, {_f(p[1] - ay)}, {_f(p[2] - az)})"


def _richting(v):
    return f"new Vec3({_f(v[0])}, {_f(v[1])}, {_f(v[2])})"


def _scene(s):
    naam = s.kort.upper()
    r = [f"    // ---- {s.id}: {s.titel} ----",
         f"    /** The template block {s.id} is anchored on (template {s.template}). */",
         f"    public static final BlockPos {naam}_ANKER = new BlockPos({s.anker[0]}, {s.anker[1]}, {s.anker[2]});",
         "    /** The structure it stands in, and the jigsaw piece of it (null: the start piece). */",
         f'    public static final String {naam}_STRUCTUUR = "{s.structuur}", {naam}_STUK = ' + (f'"{s.stuk}"' if s.stuk else "null") + ";"]
    for m, t in sorted(s.momenten.items()):
        r.append(f"    public static final int {naam}_{m} = {t};")
    for m, p in sorted(s.plekken.items()):
        r.append(f"    public static final BlockPos {naam}_{m} = new BlockPos({p[0]}, {p[1]}, {p[2]});")
    # what this slice's own client draws
    weer = ", ".join("{" + ", ".join(f"{_f(v)}f" for v in rij) + "}" for rij in s.weer_rijen())
    flitsen = ", ".join(str(t) for t in sorted(s.flitsen))
    r.append(f"    public static final Effecten {naam}_EFFECTEN = new Effecten(new float[][] {{{weer}}}, new int[] {{{flitsen}}}, List.of(")
    stromen = []
    for t0, t1, soort, van, naar, per_tick, spreiding, snelheid in sorted(s.stromen, key=lambda x: (x[0], x[2], x[3])):
        bron = STROOM.get(soort, f"() -> ParticleTypes.{soort}")
        stromen.append(f"            new Effecten.Stroom({t0}, {t1}, {bron}, {_vec(s, van)}, {_vec(s, naar)}, {_f(per_tick)}f, {_f(spreiding)}, {_richting(snelheid)})")
    r.append(",\n".join(stromen) + "));" if stromen else "            ));")
    regel = f'    public static final Cutscene {naam} = Cutscene.maak("{s.id}").duur({s.duur}).bij("{s.lijn}").kaart("{s.id}").verbergEcht({_f(s.verberg)})'
    r.append(regel)
    for a in s.acteurs:
        if a.soort == "speler":
            r.append(f"            .speler({_vec(s, a.start)}, {_f(a.yaw)}f)")
        elif a.soort == "npc":
            r.append(f'            .npc("{a.naam}", GuhNpcEntity.Kind.{a.arg}, {_vec(s, a.start)}, {_f(a.yaw)}f)')
        elif a.soort == "guh" and not a.nbt:
            r.append(f'            .guh("{a.naam}", GuhVariant.{a.arg}, {_vec(s, a.start)}, {_f(a.yaw)}f)')
        elif a.soort == "guh":
            r.append(f'            .acteur("{a.naam}", ModEntities.GUH, {_vec(s, a.start)}, {_f(a.yaw)}f, tag -> Rekwisieten.{a.nbt}(tag, GuhVariant.{a.arg}))')
        else:
            nbt = f", Rekwisieten::{a.nbt}" if a.nbt else ""
            r.append(f'            .acteur("{a.naam}", {a.arg}, {_vec(s, a.start)}, {_f(a.yaw)}f{nbt})')
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
        r.append(f"            .geluid({t}, {GELUID[veld]}, {_f(volume)}f, {_f(pitch)}f)")
    for t, soort, pos, aantal, spreiding in sorted(s.deeltjes, key=lambda x: (x[0], x[1])):
        r.append(f"            .deeltjes({t}, {DEELTJE.get(soort, 'ParticleTypes.' + soort)}, {_vec(s, pos)}, {aantal}, {_f(spreiding)})")
    for t, kracht, ticks in sorted(s.schudden):
        r.append(f"            .schud({t}, {_f(kracht)}f, {ticks})")
    for t0, t1 in sorted(s.zwart):
        r.append(f"            .zwart({t0}, {t1})")
    r.append("            .registreer();")
    return "\n".join(r)


def scenes():
    from features import oude_scenes_scene as S
    r = [KOP, "package nl.juiced.guhs.feature.oudescenes;\n",
         "import java.util.List;\n",
         "import net.minecraft.core.BlockPos;", "import net.minecraft.core.particles.ColorParticleOption;",
         "import net.minecraft.core.particles.DustParticleOptions;", "import net.minecraft.core.particles.ParticleTypes;",
         "import net.minecraft.sounds.SoundEvents;", "import net.minecraft.world.entity.EntityType;", "import net.minecraft.world.phys.Vec3;",
         "import nl.juiced.guhs.entity.GuhNpcEntity;", "import nl.juiced.guhs.entity.GuhVariant;",
         "import nl.juiced.guhs.feature.balto.BaltoFeature;", "import nl.juiced.guhs.feature.band.BandFeature;",
         "import nl.juiced.guhs.feature.beroepen.BeroepenFeature;", "import nl.juiced.guhs.feature.hemel.HemelFeature;",
         "import nl.juiced.guhs.feature.mewtwo.MewtwoFeature;", "import nl.juiced.guhs.feature.verhaal.Cutscene;",
         "import nl.juiced.guhs.registry.ModEntities;", "import nl.juiced.guhs.registry.ModSounds;\n",
         "/**",
         " * bbq2 (oude-scenes): the six camera scenes of the older stories, as the verhaal engine wants them. Positions are relative to",
         " * the scene's anchor (a block of the story's own template), in template coordinates, so they fit every copy however it is",
         " * turned. The scripts themselves, with what every beat is for, are tools/features/oude_scenes_scene.py (which also checks every",
         " * camera and actor against the template); their texts: scene.guhs.oudescenes_*. {@link OudeScenes} says when each plays.",
         " */",
         "public final class Scenes {"]
    for s in S.alle():
        r.append(_scene(s))
        r.append("")
    r += ["    /** (called from OudeScenesFeature.register: the fields above register the scenes when this class loads) */",
          "    static void registreer() {\n    }\n",
          "    private Scenes() {\n    }\n}\n"]
    return "\n".join(r)


BESTANDEN = {"Scenes.java": scenes}


def check():
    """The problems: a Java file that is not what this module would write."""
    problems = []
    for naam, maak in BESTANDEN.items():
        pad = os.path.join(JAVA, naam)
        wil = maak()
        heeft = open(pad, encoding="utf-8").read() if os.path.exists(pad) else None
        if heeft != wil:
            problems.append(f"{pad} is stale: run python tools/features/oude_scenes_java.py")
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
