"""
einde2 (cast_schurk): what the wiki must say about the creatures of the Guhvatar story. Not a feature module (not in
FEATURES): the docs step reads WIKI (build contract 2.4). The creature pages come from the game data by themselves; the
Mika kinds are characters of the story and get no page of their own here (no spoilers; phase B writes the story's pages).
"""
from features import einde2_cast_schurk_tekst as tekst

WIKI = {
    "entity_home": {
        "draakguh": "bouwwerken/vuurnatie_hoofdstad",
        "bosco": "bouwwerken/ba_sing_guh",
        "wan_shi_guh": "bouwwerken/bibliotheek_wan_shi_guh",
        "vosguh": "bouwwerken/bibliotheek_wan_shi_guh",
        "leeuwschildpad_guh": "bouwwerken/leeuwschildpad_eiland",
        "flopsie": "bouwwerken/omashguh",
        "luchtbizon_guh": "bouwwerken/westelijke_luchttempel",
        "maanvis": "bouwwerken/noordelijke_waterstam",
        "oceaanvis": "bouwwerken/noordelijke_waterstam",
    },
    "tekst": [
        ("vuurnatie_hoofdstad", "De draakguhs",
         f"In hun grot wonen de twee draakguhs, {tekst.DRAKEN[0]} (rood) en {tekst.DRAKEN[1]} (blauw): enorme slangendraken met het "
         "gezicht en de oren van een guh. Ze blazen vuur in alle kleuren, maar dat vuur is lauw: niemand brandt zich."),
        ("ba_sing_guh", "Beer Bosco", "In het paleis woont beer Bosco. Hij heeft een hoedje en een vestje. Verder is het gewoon... een beer."),
        ("bibliotheek_wan_shi_guh", "Wie er wonen",
         "De grote uil Wan Shi Guh, met een wit hartvormig guhgezichtje, en zijn vosguhs: kleine vosjes die boekrollen naar de "
         "goede plank brengen."),
        ("omashguh", "Flopsie", "Koning Bumi-guh heeft een huisdier: Flopsie, een geitgorilla die veel te groot is voor zijn hok en "
         "iedereen likt."),
        ("westelijke_luchttempel", "De kudde", "Rond de tempel zweeft een kudde luchtbizon-guhs, in vijf kleuren, met een kalfje."),
        ("noordelijke_waterstam", "Tui en La", "In de vijver van de geestenoase zwemmen twee vissen rondjes om elkaar: de witte "
         "maanvis Tui en de zwarte oceaanvis La."),
        ("leeuwschildpad_eiland", "Hij leeft", "Het eiland is een Leeuwschildpad-guh. Kijk goed: zijn oogleden knipperen heel "
         "langzaam, en zijn baard wiegt in de wind."),
    ],
}
