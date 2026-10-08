# Guhs 1.2.0 - Dutch -> English glossary

This glossary is **binding** for every translator. If a word is in here, use exactly this English form,
in every key, quest, sign, book and dialogue. If a word is not in here, build it from the parts that are
(knabbel = nibble, vads = chonk, knuffel = snuggle, ...), and keep it consistent with what's already here.

---

## 1. STYLE

**Tone.** Cute, cozy, silly, kid-friendly. Same energy as the Dutch: short sentences, lots of
exclamations, guh-words sprinkled in. Never sarcastic or mean. Mikas are grumpy, never scary.

**How guhs talk.** Guhs are happy little chonks. They say *Guh!*, *Nyeg!* and *WAHOOG!* a lot, use
diminutives ("little", "-ie", "-y": *tummy, pawsies, ears*), and love words like *cozy, snuggly, chonky*.
Keep the guh-word where the Dutch has one; don't add three more. Mikas say *NYEG!* angrily and laugh
*Nyeh-heh-heh!*.

**Spelling.** American English: *color, favorite, behavior, cozy, gray, center, armor, theater,
neighbor, practice*. (Minecraft's own en_us also says *Armor*.) Ukulele, not ukelele.

**Capitalization.**
- Item, block, entity, biome, structure, effect and enchant names: **Title Case**
  (*Wahoog Chonk Pickaxe*, *Bucket of Cheese Sauce*, *Potted Cheese Flower*). Small words stay lowercase
  inside a name (*of, the, in, a, and, on*) unless first.
- Character names: as in the tables (*Mayor Chonkworth*, *Grandpa Guh*).
- In running text, generic nouns stay lowercase: *a guh, two cheese nibbles, your guhbelly*;
  proper places/characters stay capitalized: *Snuggledale, the Guh End, Overlord Mika*.
- Shouts stay in caps exactly where the Dutch shouts: *VAHOEG!* -> *WAHOOG!*, *NJEG!* -> *NYEG!*.

**Placeholders and codes - never touch.** Keep `%s`, `%1$s`, `%2$s`... (you may reorder positional ones
if English word order needs it, but every placeholder must still be there exactly once). Keep `§x`/`&x`
color codes, `&r`, `\\&` (escaped ampersand in FTB Quests), `\n`, `<3`, `♥`, `★`, `♪`, `✔`, `⚠`, arrows,
`{...}`, JSON escapes and `[links]` exactly as-is. Keep keybind letters (W/A/S/D, 1-8, A-K) as they are.

**Punctuation.** Use straight quotes `'` `"` like the Dutch source does. Ellipsis = three dots `...`.
Dashes: the source uses ` - ` (space hyphen space); keep that, no em dashes. Exclamation marks: match the
Dutch count (`!!!` stays `!!!`).

**Puns.** Translate the *joke*, not the words. If the Dutch puns on a Dutch idiom, swap in an English
idiom with the same feel (*"Boer zoekt Guh"* -> *"Farmer Wants a Guh"*). If there is no good English pun,
write something cute and guh-flavored instead; never leave a pun-shaped hole or a literal translation that
makes no sense. Pop-culture parodies keep their parody (Balto, Mewtwo, Lilo & Stitch, Elfstedentocht,
Sherlock, Bob the Builder, Finding Nemo seagulls, Saturday Night Fever...).

**Length.** Keep names short enough for the UI: an item name should not be much longer than the Dutch.
**Signs:** max ~15 characters per line (each sign line is its own key) - rephrase to fit, never let a
line overflow. Scoreboard/HUD/button strings: keep within ~120% of the Dutch length.

**Plurals & possessives.** *Mika* -> plural *Mikas* (no apostrophe; Dutch *Mika's* = plural).
Possessive *Mika's* only where it really is "of Mika" (*Mika's Biome*, *Mika's Grease*).
*guh* -> *guhs*. *Guhdex* has no plural.

**Diminutives.** Dutch *-tje/-je* (guhtje, babyguhtje, muisje) -> *little ...*, *-ie*, or *-ling*:
*guhtje* = "little guh" in prose, *guhling* in names, songs and titles. Don't stack them
("little guhling" is too much).

**Spawn eggs, buckets, borrowed items.** Follow vanilla English: *Guh Spawn Egg*, *Mika Spawn Egg*;
*Bucket of Cheese Sauce*, *Bucket of Guhfish*; "(geleend)" -> "(Borrowed)"; "X in pot" -> *Potted X*.
Stairs/slabs/walls/fences/gates/doors/trapdoors: *Chonkwood Stairs*, *Cheese Rind Brick Slab*, etc.

**Things that NEVER change:** guh, guhs, Guh! (sound), Guhdex, Guhmension (Dutch: Guhmensie), Mika (the
name; titles around it are translated), GeckoLib/mod names, Minecraft vanilla names (use the official
en_us name: Nether, End, Overworld, Ender Dragon, Sniffer...), Experiment 626, Ohana, Aloha.

---

## 2. Core guh words, slang & sounds

| Dutch | English | note |
|---|---|---|
| guh / guhs | guh / guhs | never translated |
| Guh! / Guh guh! / Guhhh... | Guh! / Guh guh! / Guhhh... | the guh sound, unchanged |
| guhtje | little guh / guhling | see Style: diminutives |
| babyguhtje / babyguh | baby guh / guhling | *Babyguhtjes terugbrengen* = *Bringing Back Baby Guhs* |
| njeg / Njeg! / NJEG! | nyeg / Nyeg! / NYEG! | happy guh word; ANGRY when a Mika says it |
| Njeg-njeg / njeh-heh-heh | Nyeg-nyeg / nyeh-heh-heh | Mika laugh = *Nyeh-heh-heh!* |
| vahoeg / VAHOEG! | wahoog / WAHOOG! | the happiest word ("wahoo" + guh) |
| vahoege (adj.) | wahoog | *vahoege vads* = *wahoog chonk*; never "wahooge" |
| supervahoeg | super wahoog | |
| vahoegste | most wahoog / wahoogest | pick what reads best; *Vahoegste van de Zeskamp* = *Wahoogest of the Hexathlon* |
| Vahoegheid | Wahoogness | *Drankje van Vahoegheid* = *Potion of Wahoogness* |
| onvahoeg (effect) | Unwahoog | |
| vads (noun/interj.) | chonk / Chonk! | "lazy and round"; *Vads!* = *Chonk!*; *Slaap lekker vads* = *Sleep chonky-tight* |
| vadsig | chonky | the best compliment for a guh |
| vadsige / vadsiger / vadsigst | chonky / chonkier / chonkiest | |
| vadsigheid | chonkiness | *Vadsigheid-scanner* = *Chonkiness Scanner* |
| vadsen / rondvadsen | to chonk / chonk around | menu *Rondvadsen: %s* = *Chonk around: %s* |
| gevadst / OPPER-GEVADST! | chonked / OVER-CHONKED! | Overlord Mika's defeat cry |
| vadsjes (lieve vadsjes) | chonkies | only in the heart-level phrases |
| lieve vadsige guhs | sweet chonky guhs | the mod's tagline |
| knabbel / knabbels | nibble / nibbles | everywhere, also in compounds |
| kaasknabbel(s) | cheese nibble(s) | generic, lowercase in prose |
| Kaas Knabbels (item) | Cheese Nibbles | the original item name |
| knabbelen | to nibble | |
| knuffel (hug) / knuffelen | cuddle / hug / to cuddle | the act |
| knuffel (plush toy) | plushie | *Guhknuffel* = *Guh Plushie* |
| Knuffel- (in place/proper names) | Snuggle- | *Knuffeldal* = *Snuggledale* |
| knus / Knus (tab, superkompas) | cozy / Cozy | |
| gezellig (effect Gezellig) | snug / Snug | |
| pluis / pluisje / pluizig | fluff / fluffy / fluffy | |
| zielsguh | soulguh | soulmate pun |
| guhwoord (geheim guhwoord) | guh word (secret guh word) | the secret word is NYEG (starts with an N, keep that!) |
| Blub / blub-njeg | Blub / blub-nyeg | water critter sound |
| Piep piep! | Squeak squeak! | |
| Mjam! / Nom nom | Nom! / Nom nom | |
| Hatsjoe! / Hatsjoe-njeg! | Achoo! / Achoo-nyeg! | |
| Gak! (Boris the goose) | Honk! | |
| Auuuhoe! | Awooo! | Baltoguh howl |
| PLOF! / Plons! | FLOMP! / Splash! | |
| Hup hup! | Hup hup! / Go go! | *Hup, Baltoguh!* = *Go, Baltoguh!* |
| Doei! / Doei-doei! | Bye-bye! | |
| Tingeling! | Ding-a-ling! | |
| Hihihi! | Hee hee hee! | |
| Snuf snuf / Snuffel! | Sniff sniff / Sniff! | |
| Oei! | Oopsie! | |
| Wiee! / WIEEEEE! | Wheee! / WHEEEEE! | |
| Brrr | Brrr | |
| Tokkel tokkel | Strum strum | ukulele |
| Kwaak / Kwak | Ribbit / Quack | frog / duck |
| Tjiep | Tweet | |
| Oehoe | Hoo-hoo | |
| Ssst / SSST | Shh / SHHH | |
| zieli / gewoon zieli | sadsie / just sadsie | Piep chapter catch-word for pitiful |
| beef / beef bijgelegd | beef / beef squashed | Shelly drama, keep "beef" |
| besties / bestie-moment | besties / bestie moment | |
| bff 5evr <3 | bff 5evr <3 | keep exactly |

## 3. Heart levels, friendship & guh menu

| Dutch | English | note |
|---|---|---|
| op weg naar lieve vadsjes... | on the way to sweet chonkies... | level 0 |
| lieve vadsjes van elkaar | sweet chonkies together | level 1 |
| mega lieve vadsjes van elkaar | mega sweet chonkies together | level 2 |
| zielsguh bff 5evr <3 | soulguh bff 5evr <3 | level 3 (max) |
| Lieve vadsjes (FTB chapter) | Sweet Chonkies | |
| hartjes | hearts | |
| guhmenu | guh menu | |
| Kledingkast | Wardrobe | |
| Kleertjes / Kleding | Clothes | *Kleertjes uit* = *Clothes Off* |
| pakje (minigame outfit set) | outfit | *het Mika-jagerpakje* = *the Mika Hunter outfit* |
| Rugzak & harnas | Backpack & Harness | |
| Aantrekken! | Dress Up! | |
| Dobbel! | Roll the Dice! | |
| Karakter | Personality | |
| Speels / Lui / Vadsig / Verlegen | Playful / Lazy / Chonky / Shy | |
| Dapper / Kletskous / Knuffelig / Nieuwsgierig | Brave / Chatterbox / Cuddly / Curious | |
| Gedrag: Passief (vluchten) / Passief / Neutraal / Agressief | Behavior: Passive (Flee) / Passive / Neutral / Aggressive | |
| Aanvalsstraal | Attack Radius | |
| Guhgeluidjes | Guh Noises | |
| Zitten / Opstaan | Sit / Stand Up | |
| Zwaartekracht | Gravity | |
| Dagboekje | Diary | |
| Wist-je-datjes | Did-You-Knows | |
| Eerste keren | Firsts | |
| Favorietjes / favorietje | Faves / fave | |
| Lievelings- (hapje/plek/knuffel/liedje/speeltje/emote/kleur/kunstje) | Favorite (Snack/Spot/Plushie/Song/Toy/Emote/Color/Trick) | |
| Beste guh-vriendje / vriendjes | Best Guh Buddy / buddies | |
| Klusjes | Chores | |
| Guhhuisje | Guh House | the guh-head house; *Villa Vahoeg* = *Villa Wahoog* |
| Bewoners | Residents | |
| Speeltje / speelgoed | Toy / toys | |
| Logeren / logeerguhs | Sleepover / sleepover guhs | |
| Guhkamer | Guh Room | guest room in your Guhbelly |
| Guhbel | Guh Bell | |
| Mijn guhs | My Guhs | Guhdex tab |
| Emotes / Lievelingsemote | Emotes / Favorite Emote | |
| Op slot | Locked | |
| Ontgrendeld | Unlocked | |
| Rechtsklik(ken) | Right-click | |
| aaien / aaitje / Je aait %s! | pet / a pet / You pet %s! | 1.2.0: a short tap on your own guh is only petting |
| Welke taal wil je voor Guhs? / Automatisch | Which language for Guhs? / Automatic | 1.2.0 first-join question; language button label *Lang: %s* |

## 4. Emotes

| Dutch | English | note |
|---|---|---|
| Zwaaien | Wave | |
| Dansen | Dance | |
| Slapen | Sleep | |
| VAHOEG-sprong | WAHOOG Jump | |
| Rollen | Roll | |
| Smakken | Munch | |
| Verlegen | Shy | |
| Gapen | Yawn | |
| Zingen | Sing | |
| Knuffelen | Cuddle | |
| Hartjes | Hearts | |
| Knuffeldansje | Snuggle Dance | |
| Bff-knuffel | BFF Hug | |
| Verdrietje | Sniffles | |
| Ukelele | Ukulele | 626-guh only |

## 5. Guhdex & UI terms

| Dutch | English | note |
|---|---|---|
| Guhdex | Guhdex | never translated |
| Zeldzaamheid | Rarity | |
| Gewoon / Ongewoon / Zeldzaam / Heel zeldzaam | Common / Uncommon / Rare / Very Rare | |
| Legendarisch / Uniek | Legendary / Unique | |
| Vaak / Zielig / Stiekem | Common / Pitiful / Sneaky | other rarity words |
| Getemd / Nog niet gezien | Tamed / Not seen yet | |
| Beloningen / Ophalen / Opgehaald | Rewards / Claim / Claimed | |
| Mijlpalen / Verzamelingen | Milestones / Collections | |
| Nog niet ontdekt | Not discovered yet | |
| Highscores / Scorebord / Serverrecord / Wereldrecord | High Scores / Scoreboard / Server Record / World Record | |
| makkelijk / medium / lastig / moeilijk | easy / medium / hard / hard | difficulty levels |
| snelste ronde | fastest lap | |
| Superkompas / Guhmensie-superkompas | Super Compass / Guhmension Super Compass | |
| Avontuur / Wonderen / Wonen / Ondergrond / Einde | Adventure / Wonders / Homes / Underground / End | Super Compass categories |
| Klassiekers | Classics | minigame group |
| De Grote Guhspelen | The Great Guh Games | minigame group |
| Knuffeldal-spelletjes | Snuggledale Games | |
| Guhverhalen / Verhalen | Guh Tales / Tales | stories tab |
| Eerdere avonturen | Earlier Adventures | |
| Bezocht / Nog niet bezocht | Visited / Not visited yet | |
| Kaasregen / Vadsparade / Sterrenregen | Cheese Shower / Chonk Parade / Star Shower | world events |
| Seizoenen: Lente/Zomer/Herfst/Winter | Seasons: Spring/Summer/Fall/Winter | American "Fall" |
| Ochtend / Dag / Middagdutje / Avond / Nacht | Morning / Day / Nap Time / Evening / Night | |
| Bovenwereld | Overworld | vanilla name |
| Maaginstellingen | Belly Settings | |
| Openbaar / Privé / Whitelist | Public / Private / Whitelist | |
| Guhs-instellingen / Clientinstellingen | Guhs Settings / Client Settings | |
| Taal: Automatisch / NL / EN | Language: Auto / NL / EN | the new switch |

## 6. Dimensions & biomes

| Dutch | English | note |
|---|---|---|
| Guhmensie | Guhmension | never translated |
| Guhmaag (dimension/biome) | Guhbelly | your pocket dimension inside a giant guh |
| guhmaag (your own) | guhbelly | lowercase in prose |
| Het Guheinde | The Guh End | endgame dimension |
| De Guhbarbecuether | The Guhbarbecuether | already an English pun, keep |
| Barbecuether | Barbecuether | |
| Guhvelden | Guh Fields | |
| Knabbelkruimels | Nibble Crumbs | |
| Roze pluisjes | Pink Puffs | |
| Kaasvlakte | Cheese Flats | |
| Guhweides | Guh Meadows | |
| Guhpieken | Guh Peaks | |
| Vadskliffen | Chonk Cliffs | |
| Mika's bioom | Mika's Biome | |
| Guhzee / Diepe Guhzee | Guh Sea / Deep Guh Sea | |
| Guhkristalmijn | Guh Crystal Mine | |
| Gatenkaasgrotten | Holey Cheese Caves | holey/holy pun |
| Kaasmoeras | Cheese Swamp | |
| Vadswoud | Chonkwood Forest | wood itself = Chonkwood |
| Houtskoolvlakte | Charcoal Flats | |
| Satébos | Satay Grove | |
| Worstenwoud | Sausage Woods | |
| Asdal | Ash Vale | |
| Rookdelta | Smoke Delta | |
| Knuffeldal | Snuggledale | |
| Guhpolder | Guh Polder | keep "polder" (Dutch-landscape nod for the Elfstedentocht parody) |
| Sneeuwguhtoendra | Snowguh Tundra | |
| Guhwai'i | Guhwai'i | Hawaii pun works in English |
| Nomguhpieken | Nomguh Peaks | |

## 7. Structures & places

| Dutch | English | note |
|---|---|---|
| Guhportaal / kaasknabbelportaal | Guh Portal / cheese nibble portal | |
| Guhgrotten / Uitdagende guhgrotten | Guh Caves / Challenging Guh Caves | |
| hamsterbuizen | hamster tubes | |
| Huis van Boze Mika | Evil Mika's House | |
| Vadsig-heiligdom | Chonky Shrine | Mother Chonky lives here |
| Mika-kamp | Mika Camp | |
| Guh-picknick | Guh Picnic | |
| Sleehut | Sled Hut | |
| Verstopguhhuis | Hide-and-Guh House | |
| Guhkermis / kermis | Guh Fair / fair | |
| Guhkasteel | Guh Castle | |
| Guhland | Guhland | theme park, keep |
| Guhramide | Guhramid | |
| Guhgypter (adv.) | Guhgyptian | "Walk like a Guhgyptian" |
| Guhstandbeeld | Guh Statue | |
| (Grote) Kaasfontein | (Grand) Cheese Fountain | |
| Guhdorp / guhdorpelingen | Guh Village / guh villagers | |
| Hamsterhuis / Groot hamsterhuis / Hamsterstad | Hamster House / Big Hamster House / Hamster Town | |
| Guh Beauty Theater | Guh Beauty Theater | |
| Guhracebaan | Guh Racetrack | |
| Mika-mephal | Whack-a-Mika Hall | |
| Guhdisco | Guh Disco | |
| Guhgolfbaan | Guh Mini Golf | |
| Vadsig eetfestijn | Chonky Food Fest | |
| Guhvisvijver | Guhfish Pond | |
| Zwevende guh-eilandjes | Floating Guh Islets | |
| Kaasmijn | Cheese Mine | |
| Guhbibliotheek / bieb | Guh Library / library | |
| Guhbubbel | The Guh Bubble | underwater dome |
| Knabbelkelder | Nibble Cellar | portal to the Guh End |
| Mika-vesting | Mika Fortress | |
| Terugpoort | Return Gate | |
| Stille Voorraadkelder | Silent Pantry | |
| Verlaten kaasmijnschacht | Old Cheese Mineshaft | (shortened: fits the Super Compass button) |
| Paalhut van de Moerasheks | Swamp Witch's Stilt Hut | |
| Boomhutdorp | Treehouse Village | |
| Barbecueput | Barbecue Pit | |
| Spiesburcht | Skewer Keep | |
| Mika-grillpaleis | Mika Grill Palace | |
| Knuffeldal(-stadje) | Snuggledale (town) | |
| Knabbelbakkerij | Nibble Bakery | |
| Knuffelcreche | Snuggle Daycare | |
| Knabbelthee-huisje / theehuis | Nibble Tea House / tea house | |
| Knip & Vads | Snip & Chonk | hair salon |
| Beroepenstraat | Career Street | |
| brandweerkazerne / ziekenhuisje | fire station / little hospital | |
| Guhboerderij | Guh Farm | |
| Guhtuintjes | Guh Gardens | |
| Guh-Sterrenwacht | Guh Observatory | |
| Ballonfestival | Balloon Festival | |
| Kampeerplekje(s) | Campsite(s) | |
| Knuffelbad | Snuggle Pool | |
| Het Sjoelhuisje | The Shuffle House | |
| Het Guhdoolhof | The Guh Maze | |
| De Knabbelkatapult | The Nibble Catapult | |
| De Knabbelspelen | The Nibble Games | |
| Kaasknabbel-nest | Cheese Nibble Nest | |
| De Elf-Guhjestocht | The Eleven Guhtowns Tour | Elfstedentocht nod |
| Het Guh-Circuit / Pitpaleis | The Guh Circuit / Pit Palace | |
| Nomguh | Nomguh | Nome + nom, keep |
| berghut | mountain hut | Balto story |
| Het kloon-eiland | Clone Island | |
| Het Hemelkapelletje | The Cloud Chapel | |
| Het paalhuisje van Lilo en Nani | Lilo and Nani's Stilt House | |
| guh-asiel | guh shelter | |
| De neergestorte capsule | The Crashed Capsule | |
| Het surfstrand van Guhwai'i | Guhwai'i Surf Beach | |

**Eleven Guhtowns** (Elfstedentocht villages; keep the Frisian-town parody, swap only Dutch slang parts):
Guhwarden -> **Guhwarden** · Snuh -> **Snuh** · IJlguh -> **Ijlguh** · Knabbelsloten -> **Nibblesloten** ·
Vadsvoren -> **Chonkvoren** · Guhdeloopen -> **Guhdeloopen** · Vadskum -> **Chonkum** ·
Knabbelsward -> **Nibblesward** · Guhlingen -> **Guhlingen** · Franeguh -> **Franeguh** · Dokguh -> **Dokguh**.
"It giet oan!" (Frisian) stays as-is: the famous Elfstedentocht phrase.

## 8. Characters & NPCs

| Dutch | English | note |
|---|---|---|
| Mika / Mika's | Mika / Mikas | name kept; plural no apostrophe |
| Grote Mika | Big Mika | |
| Boze Mika | Evil Mika | |
| Mika-baas | Mika Boss | |
| Opper-Mika | Overlord Mika | Guh End boss |
| Nether-Mika | Nether Mika | |
| Moerasheks-Mika | Swamp Witch Mika | |
| Kruimel-Mika | Crumb Mika | |
| Vonk-Mika | Spark Mika | |
| Knekel-Mika | Skelly Mika | |
| Aangebrande Mika | Burnt Mika | |
| Heg-Mika | Hedge Mika | |
| Steele-Mika | Steele Mika | Balto parody, keep "Steele" |
| Voorraadmika | Pantry Mika | |
| Mika-larfje | Mika Grub | |
| Knabbeldief | Nibble Thief | |
| Mika-pikker | Mika Snatcher | |
| Moeder Vadsig | Mother Chonky | |
| Guhbert | Guhbert | her baby, keep |
| Oerguh | Firstguh | the very first guh |
| Hongerige Guh | Hungry Guh | |
| Tandarts-guh | Dentist Guh | |
| Maagenzym-guh | Enzyme Guh | |
| Slee-guh | Sled Guh | |
| Kermis-guh | Fair Guh | |
| Verstopguhtje | Hidey Guh | |
| Tipguh | Hint Guh | |
| Reisguh | Travel Guh | |
| Poortwachter | Gatekeeper | |
| Koningguh / Zijne Vadsigheid | King Guh / His Chonkiness | |
| Showguh | Show Guh | |
| Juf Vadsma / Meneer Glitterguh / Oma Knabbel | Miss Chonksley / Mr. Glitterguh / Granny Nibbles | beauty jury |
| Raceguh | Race Guh | |
| Mepguh | Whack Guh | |
| DJ-guh | DJ Guh | |
| Golfguh | Golf Guh | |
| Smulguh | Munch Guh | |
| Visguh | Fishing Guh | |
| Mijnguh | Miner Guh | |
| Bibliothecaris | The Librarian | |
| Zeemeerguh | Merguh | NPC and variant |
| Boswachterguh | Ranger Guh | |
| Knabbelplukker | Nibble Picker | |
| Grillguh | Grill Guh | |
| Paradeguh / Tamboerguh | Parade Guh / Drummer Guh | |
| Burgemeester Vadsema | Mayor Chonkworth | |
| Knuffelburgemeester (title) | Snuggle Mayor | |
| Cocotje | Coco | always losing things |
| Opa Guh | Grandpa Guh | |
| Pluisje / Knabbeltje / Dikkie | Fluffy / Nibbles / Chubbs | Snuggledale neighbors |
| Mollie / Sproetje / Bolletje | Molly / Freckles / Dumpling | Snuggledale neighbors |
| Bakker Korstje | Baker Crusty | |
| Klantje / Feestklantje | Customer / Party Customer | |
| Juf Knuffel | Miss Snuggles | daycare |
| Mevrouw Theelepel | Mrs. Teaspoon | |
| Kapper Krulletje | Curly the Barber | |
| Boerin Hooibaal | Farmer Haybale | |
| Professor Sterretje | Professor Twinkle | |
| Kapitein Wolkje | Captain Cloudy | |
| Badmeester Bubbel | Lifeguard Bubbles | |
| IJscoguh Tingeling | Jingles the Ice Cream Guh | |
| Opoe Njegschuif | Granny Nyegshuffle | shuffleboard |
| Meneer Vadskronkel | Mr. Chonkwiggle | maze |
| Kapitein Floepguh | Captain Floopguh | catapult |
| Juf Vahoegsakee | Coach Wahoogaroo | Nibble Games host |
| Schaatsmeester Guhglij | Skate Master Guhglide | |
| Stempelguh | Stamp Guh | |
| Coach Vahoegvroem | Coach Wahoog-Vroom | |
| Brandweercommandant Blusguh | Fire Chief Splashguh | |
| Inspecteur Vahoegsma | Inspector Wahoogsby | |
| Dokter Snotneus-guh | Doctor Sniffleguh | |
| Bob de Guhbouwer | Bob the Guhbuilder | "Can we fix it?" nod |
| Klimguhtje / Snotje | Climby / Sniffles | career-street helpers |
| Brandweer-/Politie-/Apothekers-/Bouwguh | Firefighter / Police / Pharmacist / Builder Guh | learned careers |
| Timmerguh | Carpenter Guh | |
| Grillguhs geheime recept | Grill Guh's Secret Recipe | |
| Baltoguh | Baltoguh | |
| Boris (de gans) | Boris (the goose) | |
| Muk / Luk | Muk / Luk | |
| Rosy | Rosy | |
| Zuster Knuffel | Nurse Snuggles | |
| De witte wolf-guh | The White Wolf Guh | |
| Verteller | Narrator | |
| Held van Nomguh | Hero of Nomguh | title |
| Vriend van Guhtwo | Friend of Guhtwo | title (1.2.6) |
| Ohana-guh | Ohana Guh | title (1.2.6) |
| Wolkenvriend | Cloud Friend | title (1.2.6) |
| Huisjesbouwer | House Builder | title (1.2.6) |
| Opper-vadser | Over-Chonker | title (1.2.6) |
| Guhkenner | Guh Expert | title (1.2.6) |
| Titels / Geen titel | Titles / No title | Guhdex tab (1.2.6) |
| Guhtwo | Guhtwo | |
| Mieuwguh | Mewguh | |
| Professor Knabbelkloon | Professor Nibbleclone | |
| De wolkenhoeder | The Cloud Shepherd | |
| 626-guh | 626-guh | |
| Lilo-guh / Nani-guh | Lilo-guh / Nani-guh | |
| Tikiguh | Tiki Guh | |
| Vadstemmer | Chonk Tamer | villager profession |
| Guhkleermaker / kleermaker | Guh Tailor / tailor | villager profession |
| Vadssmid | Chonksmith | villager profession |
| Hamsterbouwer | Hamster Builder | villager profession |
| Mika-jager | Mika Hunter | villager profession |
| Knabbelboer | Nibble Farmer | villager profession |

## 9. Guh variants

| Dutch | English | note |
|---|---|---|
| Regenboogguh | Rainbow Guh | |
| Sterrenhemelguh | Starry Guh | |
| Spookguh | Ghost Guh | |
| Teckelguh | Dachshund Guh | |
| Mummieguh | Mummy Guh | |
| Muntguh | Mint Guh | |
| Chocoguh | Choco Guh | |
| Sneeuwguh | Snow Guh | |
| Truiguh | Sweater Guh | |
| Regenguh | Rain Guh | |
| Feestguh | Party Guh | |
| Chef-guh | Chef Guh | |
| Brontosaurusguh | Brontosaurus Guh | "Guh-saurus!" |
| Gouden Guh | Golden Guh | |
| brococolief | broccocutie | unique, lowercase on purpose |
| Enderguh | Enderguh | |
| Vahoege Enderguh / Hongerige Enderguh | Wahoog Enderguh / Hungry Enderguh | |
| Koningguh | King Guh | |
| Wolkguh | Cloud Guh | |
| Magere guh | Skinny Guh | |
| Kaasmoerasguh | Cheese Swamp Guh | |
| Asguh | Ash Guh | |
| Rookguh | Smoke Guh | |
| Pluisguh | Fluffguh | |
| Pinguh | Pinguh | |
| Glitterguh | Glitter Guh | |
| Kikkerguh | Frogguh | |
| Sterrenguh | Star Guh | |
| Sneeuwpopguh | Snowman Guh | the block you build |

## 10. Critters

| Dutch | English | note |
|---|---|---|
| diertjes / Diertjes van de Guhmensie | critters / Critters of the Guhmension | |
| Pieppiepmuisje(s) / muisje | Squeaksqueak Mouse (Mice) / mousie | |
| Schilly | Shelly | turtle, tiny head, big bestie |
| Poepschilly | Poopshelly | the butt-cleaner of the guh sea |
| Boze Kaasknabbel | Angry Cheese Nibble | |
| Boze Oppernabbel | Angry Overnibble | Opper -> Over |
| Roze Guh Koek | Pink Guh Cookie | |
| Guhvis | Guhfish | |
| Guhbij | Guh Bee | |
| Guhslijm | Guh Slime | |
| Kaasmot | Cheese Moth | |
| Vadswaker | Chonk Warden | Warden parody |
| Pluisvinkje | Fluff Finch | |
| Kaasmeesje | Cheese Chickadee | do NOT use "tit" |
| Guh-uiltje | Guh Owlet | |
| Zeemeeuwtje | Seagull | says "Mine! Mine!" (Nemo nod) |
| Guhxolotl | Guhxolotl | |
| Guh-eendje | Guh Duckling | |
| Knabbelvlindertje | Nibble Butterfly | |
| Glimguhtje | Glowguh | firefly |
| Lieveheersbeestje | Ladybug | |
| Pluisegeltje | Fluffhog | |
| Guh-konijntje | Guh Bunny | |
| Pluiseekhoorntje | Fluff Squirrel | |
| Sjokkel | Shuckly | Shuckle nod |
| Guhschaapje | Guh Lamb | |
| Knabbelkippetje | Nibble Chick | |
| Guhkoe | Guh Cow | |
| Babyguhtje (daycare) | Baby Guh | |
| Guh-sledehondje | Guh Sled Pup | |

## 11. Effects

| Dutch | English | note |
|---|---|---|
| Stil | Hushed | |
| Onvahoeg | Unwahoog | |
| Gezellig | Snug | |
| Uitgerust | Well-Rested | |
| Blosjes | Blushies | |
| Zweverig | Floaty | |
| Besties | Besties | |
| Fris van binnen | Fresh Inside | |
| Lief kijken | Puppy Eyes | |

## 12. Core items & blocks

| Dutch | English | note |
|---|---|---|
| Blok kaasknabbels | Block of Cheese Nibbles | |
| Kaasknabbelsteen / -diepsteen / -aarde / -keien | Cheese Nibble Stone / Deepslate / Dirt / Cobblestone | |
| Gefrituurde kaasknabbels | Fried Cheese Nibbles | |
| Mika's vet | Mika's Grease | used for frying |
| Mika-vetbal | Mika Grease Ball | |
| Guh-koekenpan | Guh Frying Pan | |
| Frituurvads | Fry Chonk | quest title |
| Kaassaus / Borrelende kaassaus | Cheese Sauce / Bubbling Cheese Sauce | |
| Vahoege vads | Wahoog Chonk | the pink ore material |
| Samengeperste super vahoege vads | Compressed Super Wahoog Chonk | |
| Vahoege-vadsstaaf | Wahoog Chonk Ingot | |
| Vahoege-vads(zwaard/houweel/bijl/schep/schoffel/paxel/schaar) | Wahoog Chonk (Sword/Pickaxe/Axe/Shovel/Hoe/Paxel/Shears) | |
| Vahoege-vads(helm/borstplaat/beenstukken/laarzen) | Wahoog Chonk (Helmet/Chestplate/Leggings/Boots) | |
| guhpantser (IJzeren/Diamanten/Netherieten) | Guh Armor (Iron/Diamond/Netherite) | |
| Bankguh / Bank Guh | Bank Guh | storage, 256 of each kind (endless with the Bottomless Nibble Belly, section 24); *het buikje* = *the tummy* |
| Guhrad / Guhdraad | Guh Wheel / Guh Wire | they make and carry *vadskracht* = *chonk power* (section 24), no longer redstone |
| Guhspawner | Guh Spawner | |
| Knabbelbak | Nibble Trough | |
| Guhnaaitafel | Guh Sewing Table | |
| Vadsaambeeld | Chonk Anvil | |
| Buizenwerkbank | Tube Workbench | |
| Mikatrofee / Opper-Mikatrofee | Mika Trophy / Overlord Mika Trophy | |
| Mika-mepper | Mika Whacker | |
| Guhrugzak | Guh Backpack | |
| Guhgrotkompas / Uitdagingskompas / Heiligdomkompas | Guh Cave Compass / Challenge Compass / Shrine Compass | |
| Mika-spoorkompas | Mika Tracker Compass | |
| Guh-buikfluitje | Guh Belly Whistle | |
| Verloren guh-taart / Taartkruimels | Lost Guh Cake / Cake Crumbs | |
| Guh-ballon | Guh Balloon | |
| Guhkristal(-blok/-cluster/-lamp/-steen) | Guh Crystal (Block/Cluster/Lamp/Stone) | |
| Guh-kristalverrekijker | Guh Crystal Spyglass | |
| Guhslee / Sleerail / Sleeglijder | Guh Sled / Sled Rail / Sled Runner | |
| Vahoege sleebouwersboek | Wahoog Sled Builder's Book | |
| Vadsdrop / Guhkurkentrekker / Vahoegschans | Chonk Drop / Guh Corkscrew / Wahoog Ramp | sled rails |
| Maagwand / Maagbodem / Maagzuur / Maagportaal | Belly Wall / Belly Floor / Belly Acid / Belly Portal | |
| Tong / Tand / Verteerde kaasknabbels | Tongue / Tooth / Digested Cheese Nibbles | |
| Guhlelie | Guh Lily Pad | |
| Kaashoning / Knabbelkorf | Cheese Honey / Nibble Hive | |
| Vadszak / Guhkussen | Chonkbag / Guh Cushion | beanbag pun |
| Vadszetel / Vadskast | Chonk Couch / Chonk Cabinet | |
| Guhstoel / Guhtafel | Guh Chair / Guh Table | |
| Lampgion | Languhtern | lampion pun |
| Guhvlaggetjes | Guh Bunting | |
| Kaasbloem / Guhoortjes / Roze guhbloem / Knabbelroos | Cheese Flower / Guh Ears / Pink Guh Flower / Nibble Rose | |
| Guhbloesem(-stam/-boompje) / Vadsplanken | Guh Blossom (Log/Sapling) / Chonk Planks | |
| Guhtaart / Guhcupcake / guhcaron | Guh Cake / Guh Cupcake / Guhcaron | |
| Kaasfondguh | Cheese Fondguh | |
| Vahoege kaasknabbelshake | Wahoog Cheese Nibble Shake | |
| Kaasknabbelzaadjes / Kaasknabbelplant | Cheese Nibble Seeds / Cheese Nibble Plant | |
| Vadszaadbak | Chonk Seed Box | |
| Roze guhgras | Pink Guh Grass | |
| Eenrichtingsvadsglas | One-Way Chonk Glass | |
| Guhlock-pet / Vergroot-vadsglas | Guhlock Cap / Magnifying Chonk Glass | Sherlock nod |
| Koninklijke guhtroon / Gouden guhmedaillon | Royal Guh Throne / Golden Guh Medallion | |
| Wolkenlift / Wolkensuikerspin | Cloud Lift / Cloud Cotton Candy | |
| Kaasader (Diepe/Gouden/Uitgemijnde) | Cheese Vein (Deep/Golden/Mined-Out) | |
| Kaaskluis / Kaasbrok / Goudkaas | Cheese Vault / Cheese Chunk / Gold Cheese | |
| Gouden kaasknabbel | Golden Cheese Nibble | |
| Sterrenstof | Stardust | |
| Kaaskoraal / Parelmoer / Reuzenschelp / Parel | Cheese Coral / Mother-of-Pearl / Giant Clam / Pearl | |
| Kaaskorst(stenen) | Cheese Rind (Bricks) | |
| Mika-steen | Mika Stone | |
| Knabbelportaalframe / Knabbelsokkel / Knabbelslot / Knabbelpoort | Nibble Portal Frame / Nibble Pedestal / Nibble Lock / Nibble Gate | |
| Guheindeportaal | Guh End Portal | |
| Enderguh-ei | Enderguh Egg | |
| Mika-traan | Mika Tear | |
| Oog van Vadsig | Eye of Chonky | Eye of Ender nod |
| Knabbelkristal / Knabbelkroon | Nibble Crystal / Nibble Crown | |
| Guhvleugels | Guh Wings | |
| Gatenkaas(stenen) / Belegen kaas | Holey Cheese (Bricks) / Aged Cheese | |
| Kaasstalactiet / Gloeiend kaasmos | Cheese Stalactite / Glowing Cheese Moss | |
| Kaaskorrel / Kaaskorrelerts | Cheese Crumb / Cheese Crumb Ore | |
| Knabbelsensor / Knabbelschreeuwer / Stille knabbel | Nibble Sensor / Nibble Shrieker / Silent Nibble | sculk parodies |
| Kaasmodder / Kaasriet / Modderig kaasgras | Cheese Mud / Cheese Reeds / Muddy Cheese Grass | |
| Motknabbel / Moeraskaas | Moth Nibble / Swamp Cheese | |
| Vadsverdrijvend drankje | Chonk-Be-Gone Potion | |
| Vadshout(-stam/-planken/...) | Chonkwood (Log/Planks/...) | |
| Guhgezichtje in de schors | Guh Face in the Bark | |
| Vadsmos / Guhnestje / Vadstouw | Chonk Moss / Guh Nest / Chonk Rope | |
| Knabbelbessen(struik) / Knabbelbessentaartje | Nibbleberries (Bush) / Nibbleberry Tart | |
| Houtskoolsteen / Roosterijzer / Gloeikool | Charcoal Stone / Grill Iron / Glowcoal | |
| Satéstam / Worststam / Satévlees | Satay Stem / Sausage Stem / Satay Meat | |
| Pindasaus- / Mosterd-nylium | Peanut Sauce / Mustard Nylium | |
| Uienlicht / Rookgat / Smeulkooltjes | Onionlight / Smoke Vent / Smoldering Embers | |
| Verkoold guhbot / Verkoolde mikakop | Charred Guh Bone / Charred Mika Head | |
| Grillkool / Kaasfrituursaus / Aanmaakblokje | Grill Coal / Cheese Frying Sauce / Fire Starter Cube | |
| Kaasknabbelsaté / Guhbraadworst | Cheese Nibble Satay / Guh Bratwurst | |
| Guhbrouwketel / Knabbelbaken | Guh Brewing Cauldron / Nibble Beacon | |
| Grillspies / Gloeister / Gloeiend kooltje | Grill Skewer / Glowstar / Glowing Ember | |
| Rookloop- / Sluipknabbel- / Guhsprongdrankje | Smokewalk / Sneaky Nibble / Guh Jump Potion | |
| Knuffelgras / Pluisgras | Snuggle Grass / Fluff Grass | |
| Pluizenboom | Fluffy Tree | |
| Knuffelsteen / Pluisdak / Knuffelklinkers | Snugglestone / Fluff Thatch / Snuggle Cobbles | |
| Guhpaddenstoeltje | Guh Mushroom | |
| Feestbuffettafel / Knus-oorkonde | Party Buffet Table / Cozy Certificate | |
| Seizoensbloembak / Seizoensslinger / Bladerhoopje | Season Planter / Season Garland / Leaf Pile | |
| Knusfeestlijstje | Cozyfest List | |
| Knabbeloven | Nibble Oven | |
| Guh-wiegje / Speelkleed | Guh Cradle / Play Mat | |
| Guh-telescoop / Sterrenlantaarn / Wensster | Guh Telescope / Star Lantern / Wishing Star | |
| Guh-luchtballon / Mini-luchtballon | Guh Hot-Air Balloon / Mini Hot-Air Balloon | |
| Guh-slaapzak | Guh Sleeping Bag | |
| Guh-wastobbe / Guhshampoo / Guh-föhn | Guh Washtub / Guh Shampoo / Guh Hair Dryer | |
| Badeendje / Zwembandje | Rubber Ducky / Swim Ring | |
| Guh-xylofoon / Grijpmachine | Guh Xylophone / Claw Machine | |
| Liedjesboekje | Songbook | |
| Marshmallowknabbel | Marshmallow Nibble | |
| Kaasijsje | Cheese Ice Cream | |
| Pluiswol / Knabbelei / Kaasmelk / Knabbelvoer / Guhborstel | Fluff Wool / Nibble Egg / Cheese Milk / Nibble Feed / Guh Brush | |
| Guhgieter / Knabbelgraan / Knabbelmeel / Theekruid | Guh Watering Can / Nibble Grain / Nibble Flour / Tea Herb | |
| Guh-molentje | Guh Windmill | |
| Guhhuisje (klein/medium/groot) | Guh House (Small/Medium/Large) | |
| Knabbelbal / Guh-glijbaantje / Guh-wip / Guh-schommel / Pluizige tunnel | Nibble Ball / Guh Slide / Guh Seesaw / Guh Swing / Fluffy Tunnel | |
| Vadsigheid-scanner | Chonkiness Scanner | |
| Onberekenbaar vahoeg | Incalculably Wahoog | poster/painting |
| Kokosnoot / Kokosmelk / Guh-palm | Coconut / Coconut Milk / Guh Palm | |
| Guh-palm(stam/-planken/-trap/-plaat/-hek/-poort/-deur/-luik/-bord), Gestripte guh-palmstam | Guh Palm (Log/Planks/Stairs/Slab/Fence/Fence Gate/Door/Trapdoor/Sign), Stripped Guh Palm Log | the palm wood set (1.2.8) |
| Schilly-eitjes | Shelly Eggs | |
| Baltoguh-beeldje / Medicijnkist | Baltoguh Statuette / Medicine Chest | |
| Sneeuwguhspar | Snowguh Spruce | |
| Sneeuwslee / Sledebelletje | Snow Sled / Sleigh Bell | |
| Kloontank / Labnotitie / Tankonderdeel | Clone Tank / Lab Note / Tank Part | |
| Het Knuffelhart / Herinnering | The Snuggleheart / Memory | |
| Guhmuziekplaat | Guh Music Disc | |
| Wilde-guhweerder | Wild Guh Shoo-Sign | 1.2.0; "shoo-sign" in running text; its sign says *hier woont al een vadsje* = *a chonky lives here already* |

## 13. Food & bakery

| Dutch | English | note |
|---|---|---|
| Knabbelbroodje | Nibble Bun | |
| Kaaskrakeling | Cheese Pretzel | |
| Vadsvlaai | Chonk Pie | |
| Guhcroissant | Guhcroissant | |
| Knabbelkoekje | Nibble Cookie | |
| Kaasbolletje | Cheese Roll | |
| Pluismuffin | Fluff Muffin | |
| Theetaartje | Tea Tart | |
| Knabbeltompouce | Nibble Tompouce | Dutch pastry nod |
| Vadsdonut | Chonk Donut | |
| Guhwafel | Guh Waffle | |
| Sterrenkoekje | Star Cookie | |
| Feesttaart | Party Cake | |
| Knabbelthee / Kaasmelkthee / Theekruidthee / Guhbloementhee | Nibble Tea / Cheese Milk Tea / Herb Tea / Guh Flower Tea | |
| Warme chocovet | Hot Chocochonk | Elfstedentocht hot cocoa |
| Kommetje snert | Bowl of Pea Soup | |
| Poffertjes | Poffertjes (mini pancakes) | keep the Dutch nod |
| Vadsig gebakken guhvis | Chonky Fried Guhfish | |

## 14. Coins, tickets & currencies

| Dutch | English | note |
|---|---|---|
| munt / -munt | coin / Coin | |
| bon / -bon | ticket | *Kermisbon* = *Fair Ticket*; *Visbon* = *Fishing Ticket* |
| Boekenbon | Book Token | |
| Mepmunt / Discomunt / Smulmunt | Whack Coin / Disco Coin / Munch Coin | |
| Bakmunt / Speenmunt / Krulmunt | Bake Coin / Pacifier Coin / Curl Coin | |
| Ballonmunt / Eendjesmunt / Schelpjesmunt | Balloon Coin / Ducky Coin / Seashell Coin | |
| Showrozet / Raceprijsje | Show Rosette / Race Prize | |
| Katapultster / Spelenlintje / Elfstempel | Catapult Star / Games Ribbon / Town Stamp | |
| Sjoelschijfje / Circuitbeker / Sledesprintbeker | Shuffle Puck / Circuit Cup / Sled Sprint Cup | |
| Stempelkaart | Stamp Card | |
| Elf-Guhjeskruisje | Eleven Guhtowns Cross | the tour medal |

## 15. Minigames

| Dutch | English | note |
|---|---|---|
| Vads-wedstrijd | Chonk Pageant | at the Guh Beauty Theater |
| Miss Vadsig(-sjerp) | Miss Chonky (Sash) | |
| Guhrace / Huur-renguh / Recordgeest | Guh Race / Rental Racing Guh / Record Ghost | |
| Mika meppen | Whack-a-Mika | |
| Guhdisco songs: Vadsige Tango / Mika-Mambo / Njeg-Njeg Boogie | Chonky Tango / Mika Mambo / Nyeg-Nyeg Boogie | |
| Ze hangen aan me vet/veh | They Hang On My Chonk | also the jukebox song: *Guh - They Hang On My Chonk* |
| Guhkoorts op zaterdagavond | Saturday Night Guh Fever | |
| Guhgolf (9 holes) | Guh Golf (9 Holes) | |
| Vadsig eetfestijn | Chonky Food Fest | |
| Guhvis-wedstrijd | Guhfish Contest | |
| Kaasvis / Vadsbaars / Guhpuffer / Roze njegforel / Mika-meerval / Gouden Guhvis | Cheesefish / Chonky Bass / Guhpuffer / Pink Nyeg Trout / Mika Catfish / Golden Guhfish | |
| Verstopguh / verstoppertje | Hide-and-Guh / hide-and-seek | |
| Knabbels bakken | Nibble Baking | |
| Kappersshow | Salon Show | |
| Glijbanen: Roze Trechter / Glimtunnel / Grote Plons | Slides: Pink Funnel / Glow Tunnel / Big Splash | |
| Guh-sjoelen / sjoelbak | Guh Shuffleboard / shuffleboard | Dutch sjoelen nod |
| Guhdoolhof | Guh Maze | |
| Pluisballen / Mika-fort | Fluffballs / Mika Fort | catapult |
| De Grote Zeskamp | The Great Hexathlon | |
| Knabbelhappen | Nibble Bobbing | |
| Zaklopen | Sack Race | |
| Mika-blikgooien | Mika Can Toss | |
| Eierlopen met knabbelei | Egg-and-Spoon Race | |
| Spijkerpoepen | Nail Plopping | the Dutch party game, kid-cheeky |
| Guhguhtje prik | Pin the Tail on the Guh | |
| Elf-Guhjestocht | Eleven Guhtowns Tour | |
| Regenboogbaan / Vadsbaan / Kaasbergbaan | Rainbow Road / Chonk Track / Cheese Mountain Track | Guh Circuit |
| Vadslooping / Rolknabbel | Chonk Loop / Rolling Nibble | |
| Nomguh-sledesprint / medicijntocht | Nomguh Sled Sprint / medicine run | |
| Surfen / Hula / Lilo-guh's surfschool | Surfing / Hula / Lilo-guh's Surf School | |
| Knabbeldraai / njeg-grab / Guh-hop | Nibble Spin / nyeg-grab / Guh Hop | surf tricks |
| Aloha, Njeg / Guhla-Hula Rock / Vahoeg Hula Hop | Aloha, Nyeg / Guhla-Hula Rock / Wahoog Hula Hop | hula songs |
| Guhkwis | Guh Quiz | library |

## 16. Stories (Guh Tales) & questlines

| Dutch | English | note |
|---|---|---|
| De ontvoerde guh | The Guhnapping | Mother Chonky's story |
| De kapotte slee | The Broken Sled | |
| Het Grote Knusfeest / Knusfeest / seizoensfeest | The Great Cozyfest / Cozyfest / season party | |
| feesttaakjes | party chores | |
| Het Guheinde (story) | The Guh End | |
| De Grillguh helpt | The Grill Guh Helps | |
| Samen een huisje bouwen | Building a House Together | |
| Baltoguh en de medicijnkist | Baltoguh and the Medicine Chest | Balto parody |
| Het kloon-eiland | Clone Island | Mewtwo parody |
| Het Knuffelhart | The Snuggleheart | revive story |
| in de wolkjes | up in the clouds | where lost guhs go (gentle, never "dead") |
| Ohana op Guhwai'i | Ohana on Guhwai'i | Lilo & Stitch parody; "Ohana means family" |
| Beroep: brandweer / politie / apotheek / bouw | Career: Firefighter / Police / Pharmacy / Builder | |
| De Knabbeldief-zaak | The Case of the Nibble Thief | |
| De Grote Mika-oorlog | The Great Mika War | |
| steen-papier-schaar-VADS | rock-paper-scissors-CHONK | the VADS gesture = the CHONK gesture |
| Ridder van de Koningguh / van het Guheinde | Knight of the King Guh / of the Guh End | |
| De allereerste kaasknabbel / Het geheim van de kaasknabbel | The Very First Cheese Nibble / The Secret of the Cheese Nibble | |
| VAHOEG! Het guhwoordenboek | WAHOOG! The Guh Dictionary | library book |
| Het Geheime Guhboek | The Secret Guh Book | |

## 17. FTB Quests chapters

| Dutch | English | note |
|---|---|---|
| Guhs (chapter group) | Guhs | |
| Guhs & basis | Guhs & Basics | keep `\\&` escape |
| De Guhmensie | The Guhmension | |
| Minigames & bijzondere plekken | Minigames & Special Places | |
| Onderwater | Underwater | |
| De Guhmaag | The Guhbelly | |
| Het Guheinde | The Guh End | |
| De Guhbarbecuether | The Guhbarbecuether | |
| Grotten, moeras & woud | Caves, Swamp & Woods | |
| Knuffeldal | Snuggledale | |
| Piep! | Squeak! | |
| Lieve vadsjes van elkaar | Sweet Chonkies Together | |
| Guhverhalen | Guh Tales | |
| Diertjes van de Guhmensie | Critters of the Guhmension | |
| Hoe kom je hier? | How Do You Get Here? | first quest of every chapter |

## 18. Snuggledale collections & songs

| Dutch | English | note |
|---|---|---|
| Knuffelvriendjes / Seizoensplakboek | Snuggle Friends / Season Scrapbook | |
| Receptenboek / Theesoorten / Kapselcollectie | Recipe Book / Tea Collection / Hairstyle Collection | |
| Kapsel: Krullen/Kuifje/Knotjes/Strikjes/Pluisbol/Vlechtjes/Hanenkam/Matje | Hairstyle: Curls/Quiff/Buns/Bows/Puffball/Braids/Mohawk/Mullet | |
| Haarverf (roze/mint/citroen/lavendel/hemelsblauw/perzik/zilver/regenboog) | Hair Dye (Pink/Mint/Lemon/Lavender/Sky Blue/Peach/Silver/Rainbow) | |
| Tuinboek / Sterrenatlas / Ballonstempelkaart / Verhalenbundel | Garden Book / Star Atlas / Balloon Stamp Card / Story Collection | |
| Badeendjes / Knuffelkast / Liedjesboek / Kaasijsjes / Piepboek | Rubber Duckies / Plushie Shelf / Songbook / Cheese Ice Creams / Squeak Book | |
| De Grote Knabbel / De Kleine Vads | The Big Nibble / The Little Chonk | constellations (Big/Little Dipper nod) |
| De Vahoege Buik / De Vluchtende Mika / De Kaasschaaf | The Wahoog Belly / The Fleeing Mika / The Cheese Slicer | constellations |
| De VAHOEGE Toonladder | The WAHOOG Scale | |
| Vader Guh | Are You Sleeping, Brother Guh? | Frère Jacques |
| Altijd is Guhtje vads | Twinkle Twinkle Little Guh | same melody |
| Alle guhtjes zwemmen in de kaassaus | All the Guhlings Swim in Cheese Sauce | |
| In de knabbelmaneschijn | By the Light of the Nibble Moon | |
| Ode aan de Knabbel | Ode to the Nibble | Ode to Joy |
| Slaap, guhtje, slaap | Hush, Little Guhling | lullaby |
| Daar is het maantje / Knabbeltje onder je kussen / Wolkje, wolkje | There's the Little Moon / A Nibble Under Your Pillow / Little Cloud, Little Cloud | lullabies |

## 19. Outfits & clothing words

| Dutch | English | note |
|---|---|---|
| hoed / muts / pet / petje | hat / cap / cap / little cap | |
| jas / jasje / vest / trui / sjaal / strik | coat / jacket / vest / sweater / scarf / bow | |
| Guhzonnebril / Hartjesbril / Guhmonocle | Guh Sunglasses / Heart Glasses / Guh Monocle | |
| Sinterklaasmijter / Pietenmuts | Saint Nicholas Miter / Helper's Cap | |
| Koningsdagkroontje / Oranje shirt | King's Day Crown / Orange Shirt | Dutch holiday nod |
| Roze guhonesie / Guhkoksbuis / Guhzuidwester | Pink Guh Onesie / Guh Chef Jacket / Guh Sou'wester | |
| Jockeypetje / Mika-jagershoed / Mepvest met zakjes | Jockey Cap / Mika Hunter Hat / Whacking Vest with Pockets | |
| Glittervadspak / Vahoege afropruik | Glitter Chonk Suit / Wahoog Afro Wig | |
| Ruitjesvadstrui / Vadsige golfpet | Argyle Chonk Sweater / Chonky Golf Cap | |
| Mijnguhhelm / Boekenwurmvest / Geleerdenbaret | Miner Guh Helmet / Bookworm Vest / Scholar's Beret | |
| Hermelijnen koningsmantel / Vahoege guhkoningskroon | Ermine Royal Cape / Wahoog Guh King Crown | |
| Gouden aureooltje / Wolkenvleugeltjes | Golden Halo / Cloud Wings | |
| Rood sjaaltje van Baltoguh / Wolfsoortjes | Baltoguh's Red Scarf / Wolf Ears | |
| Guhtwo-staartje + nekbuisje / Trainerpetje | Guhtwo Tail + Neck Tube / Trainer Cap | |
| 626-oren met antennes / Hula-rokje / Bloemenkrans | 626 Ears with Antennae / Hula Skirt / Flower Lei | |
| Hartjesspeldje / Knuffeltruitje / Zielskroontje / Gouden hartjes-halsbandje | Heart Pin / Snuggle Sweater / Soulguh Crown / Golden Heart Collar | soulguh rewards |
| Oorstrikjes | Ear Bows | |

---

## 20. Quick word-builder (for compounds not listed)

| Dutch part | English part |
|---|---|
| guh- | Guh / guh- (*Guh Sled*, *Guhfish*: join only when it's a creature/proper name, otherwise two words) |
| knabbel- | Nibble |
| kaas- | Cheese |
| vads- / vadsig | Chonk / Chonky |
| vahoeg(e)- | Wahoog |
| knuffel- | Snuggle (places/names), Cuddle (actions), Plushie (toys) |
| knus- | Cozy |
| pluis- | Fluff / Fluffy |
| Mika- | Mika |
| opper- | Over- / Overlord |
| maag- | Belly |
| wolk(en)- / wolkje | Cloud / little cloud |
| sterren- | Star |
| roze | pink |
| zielsguh- | soulguh |
| -guhtje | -guhling / little guh |

## 21. Added during review (cross-chunk names)

| Dutch | English | note |
|---|---|---|
| Smulschaal (geleend) | Munch Bowl (Borrowed) | "munch bowl" in running text |
| Feestslingers | Party Garlands | "party garlands" / "garlands" in running text |
| Kaasmelkdrankje | Cheese Milk Potion | |
| Guh-belletje | Guh Jingle Bell | not the same item as Guh Bell |
| Leenkledingkast | Loaner Wardrobe | |
| Bouwboekje van de Timmerguh | Carpenter Guh's Building Booklet | |
| Showster(-) | Showstar | clothing line, shop |
| Guh-kwismeester | Guh Quizmaster | |
| Knuffeldekentje | Snuggle Blankie | |
| Rompertje | Baby Romper | |
| Guhvoerbak | Guh Feeding Trough | |
| Routelantaarn | Trail Lantern | |
| Mieuwguh-ballonnetje | Little Mewguh Balloon | |
| Feestkapselset | Party Hairdo Kit | |
| De Vallende Knabbel | The Falling Nibble | constellation |
| Pluk (verhaal van Opa Guh) | Pip | |
| Plaatje (bakvorm) | Disc | bakery shape |
| TATUU (sirene) | Nee-naw | fire siren sound |
| Dames en heren-guhs | Ladies and Guhtlemen | |

## 22. The Palewood (1.2.8)

| Dutch | English | note |
|---|---|---|
| Bleekwoud | Palewood | the biome: "the Palewood" in running text |
| Bleekhout(-stam/-planken/-bord/...) | Pale Guhwood (Log/Planks/Sign/...) | |
| Slapend guhgezichtje in de schors | Sleepy Guh Face in the Bark | |
| Bleekmos / Bleekmostapijt / Bleek hangmos | Palewood Moss / Palewood Moss Carpet / Hanging Palewood Moss | not vanilla's "Pale Moss" |
| Krakend Guhhartje / Verzuurd Guhhartje | Creaking Guh Heart / Soured Guh Heart | verzuurd = soured; "zure hartjes" = sour hearts |
| Kraakguh / Kraak-Mika | Creakguh / Creak Mika | |
| Krak! / Krak... krak... | Creak! / Creak... creak... | |
| houten knuffel | wooden hug | |
| Kaashars / Blok kaashars | Cheese Resin / Block of Cheese Resin | |
| Harssteen / Harsstenen (trap/plaat/muur) / Gebeitelde harsstenen | Cheese Resin Brick / Cheese Resin Bricks (Brick Stairs/Slab/Wall) / Chiseled Cheese Resin Bricks | |
| Oogbloempje (dicht/open) | Guh Eyeblossom (Closed/Open) | "guh eyeblossom" in running text |
| Bleke open plek / De Bleke Open Plek | Pale Clearing / The Pale Clearing | |
| Houthakkershutje / Houthakkerguh | Woodcutter's Hut / Woodcutter Guh | |

## 22b. Inside the Guh House

| Dutch | English | Note |
|---|---|---|
| Naar binnen | Go inside | the button in the Guh House screen |
| Kijk eens binnen | Have a Look Inside | the quest and the advancement |
| guhbedje / bedje | Guh Bed / little bed | one per resident |
| logeerbedje | guest bed | the pink bed the player sleeps in |
| prikbord | notice board | who does which chore |
| instoppen | tuck in | once per guh per night |
| briefje | note | on the bed of a guh that is away |
| deurmat | doormat | |
| snoetdeurtje | snoot door | as in the other Guh House texts |
| het baasje | my human | on a guh's note |
| Huisjesraampje | Little House Window | the painted window |

## 23. Guhpixel (lobby, minigames, things for home, Reisbureau, Guh-parkour)

### kern

| Dutch | English | note |
|---|---|---|
| Guhpixel | Guhpixel | never translated |
| Guhpixel-muntjes / muntje / muntjes | Guhpixel Coins / coin / coins | not an item |
| Guh-internetcafé "De Trage Verbinding" | Guh Internet Café "The Slow Connection" | |
| Beheerder-guh / Welkomstguh / Verkoper-guh | Admin Guh / Welcome Guh / Shopkeeper Guh | |
| Netwerkkabeltje / Guhpixel-poort / Guhpixel-portaal | Network Cable / Guhpixel Gate / Guhpixel Portal | |
| Guhpixel-winkel | Guhpixel Shop | |
| [GUH] [VADS] [VADS+] [MVG] [MVG++] | [GUH] [CHONK] [CHONK+] [MVG] [MVG++] | the rank prefixes |
| Guhpixel & uitjes (Guhdex tab) | Guhpixel & Outings | |
| grapspelletjes | joke games | |
| aandenken | keepsake | |
| Skyblok / Bedwars / Vadsnite / Vadsbus | Skyblok / Bedwars / Chonknite / Chonk Bus | |
| Guhmon-gevecht / Gymleider Dutjes | Guhmon Battle / Gym Leader Naps | |
| Boer zoekt Guh / Presentatrice Guhvon / Boer Guhrrit | Farmer Wants a Guh / Host Guhvon / Farmer Guhrrit | |
| Among Guhs / De Vadsvaarder / Kapitein-guh / droomguh | Among Guhs / The Chonkfarer / Captain Guh / dream guh | |
| Guhkade / Flappy Guh / Mika-Pong | Guhcade / Flappy Guh / Mika Pong | |
| Guhkantoor / Prikklok / Bureautje / loonstrookje / kwartaalrapport | Guh Office / Time Clock / Little Desk / payslip / quarterly report | |
| Guhbioscoop / Bioscoopdoek / Bioscoopstoeltje / film | Guh Cinema / Cinema Screen / Cinema Seat / movie | |
| Reisbureau "De Vadsvakantie" / Reisagent-guh / Reisbalie / Reisstempel / reispas / ansichtkaart | Travel Agency "The Chonk Vacation" / Travel Agent Guh / Travel Desk / Travel Stamp / travel pass / postcard | |
| Vadsen bij huize Lingsesdijk 86 / Huize Lingsesdijk 86 | Chonking at Lingsesdijk 86 / The House at Lingsesdijk 86 | keep "Lingsesdijk 86" exactly |
| Guh-parkour / Startpaaltje / Finishpaaltje | Guh Parkour / Start Post / Finish Post | |

### lobby

| Dutch | English | note |
|---|---|---|
| Lobbyguh | Lobby Guh | the six little guhs with gamer names that "chat" |
| Slapende surfer | Sleeping Surfer | a guh asleep behind a computer in the café |
| Oude beige computer | Old Beige Computer | decoration block |
| Gouden lobbyknabbel / gouden knabbel | Golden Lobby Nibble / golden nibble | ten hidden in the lobby |
| Lobby-parkour | lobby parkour | for players; not the Guh Parkour for guhs |
| Parkour-startplaat / -tussenpunt / -finishplaat | Parkour Start Plate / Checkpoint / Finish Plate | |
| tussenpunt | checkpoint | |
| AFK-hoek | AFK Corner | "afk (slaap)" = "afk (sleeping)" |
| De winkel / Guhpixel-winkel | The Shop / Guhpixel Shop | |
| kraampje | stall | the game stalls in the lobby |
| Knabbelspeurder | Nibble Sleuth | title |
| Meest Vadsige Guh | Most Valuable Guh | what MVG stands for (Dutch: Meest Vadsige Guh); no title since 1.3.1 |
| Spelers online: n (en 47 guhs) | Players online: n (and 47 guhs) | |
| TERUG NAAR HUIS | BACK HOME | the exit door |
| BINNENKORT, njeg | COMING SOON, Nyeg | the empty stall |
| xX_Vadsje_Xx / Knabbel2009 / NjegMaster / SlaapKopGuh / GuhGamer_NL / KaasKoning77 | xX_Chonky_Xx / Nibble2009 / NyegMaster / SleepyHeadGuh / GuhGamer_NL / CheeseKing77 | gamer names of the lobby guhs |
| Heb je hem al uit en weer aan gezet, njeg? | Have you tried turning it off and on again, Nyeg? | the Admin Guh |
| daklopers | roof runners | |

### grap1

| Dutch | English | note |
|---|---|---|
| Skyblok-guh / Bedwars-guh / Vadsnite-guh | Skyblok Guh / Bedwars Guh / Chonknite Guh | the three game guhs in the lobby |
| spelguh | game guh | a lobby NPC that starts a game |
| grapspelletje / Grapspelletjes (FTB section) | joke game / Joke Games | |
| hét eiland | THE island | Skyblok; the Dutch stress accent becomes capitals |
| handleiding (Stap 1 van 4.812) | manual (Step 1 of 4,812) | English thousands separator is a comma: 4,812 and 2,406 |
| cobblestone generator | cobblestone generator | kept in both languages (the players' own word) |
| SKYBLOK UITGESPEELD! | SKYBLOK COMPLETED! | |
| aftiteling / Regie / Met dank aan | credits / Director / Special thanks to | |
| decorguhs | set guhs | the guhs who painted the sky |
| ALLEEN PERSONEEL / ZON (40 watt) | STAFF ONLY / SUN (40 watts) | floating labels in the Skyblok box |
| Eilandje-in-een-fles | Island in a Bottle | Skyblok keepsake (decoration block) |
| Team Rood / Blauw / Groen / Geel | Team Red / Blue / Green / Yellow | |
| Teamguh | Team Guh | the stand-in entity |
| Teamslaapmuts Rood (...) | Team Nightcap Red (...) | Bedwars keepsake; slaapmuts = nightcap |
| VERDEDIG JE BED! / BED VERDEDIGD! | DEFEND YOUR BED! / BED DEFENDED! | |
| bedden vernield / dutjes | beds destroyed / naps | end screen |
| bruggen (met wol) | to bridge (with wool) | |
| KUSSEN-GENERATOR | PILLOW GENERATOR | |
| Sloopwars | Breakwars | "Dit is Bedwars, geen Sloopwars" = "This is Bedwars, not Breakwars" |
| luik | hatch | in the floor of the Chonk Bus |
| Nog wakker: %s | Still awake: %s | the counter |
| slaapwolk | sleep cloud | the pink "storm" of Chonknite |
| #1 VADSOVERWINNING / Vadsoverwinning | #1 CHONK ROYALE / Chonk Royale | parody of "#1 Victory Royale" |
| GELIJKSPEL / Iedereen slaapt. Gelijkspel, njeg. | DRAW / Everyone is asleep. It's a draw, Nyeg. | |
| Parachuterugzakje | Parachute Pack | Chonknite keepsake (outfit, back) |
| slaapplekje | sleeping spot | the beds on the Chonknite island |
| Aandenken van Bedwars (Guhpixel) | Keepsake from Bedwars (Guhpixel) | clothing source |

### grap2

| Dutch | English | note |
|---|---|---|
| Guhmon-gevecht / Guhmon | Guhmon Battle / Guhmon | Cobblemon parody; "Guhmon" never translated |
| Gymleider Dutjes / de Dutjesgym | Gym Leader Naps / the Nap Gym | core glossary |
| Snurkel | Snorkel | the gym leader's guh |
| Leenguh | Loaner Guh | the gym's own guh for players without tamed guhs |
| SLAAP (balk) | SLEEP (bar) | instead of HP; caps as in the Dutch |
| Vadsen / Njeg / Knabbel eten / Dutje (zetten) | Chonk / Nyeg / Eat Nibble / Nap (moves) | Title Case, like move names |
| "Het is super vadsig!" / "Het is niet erg effectief..." | "It's super chonky!" / "It's not very effective..." | battle lines |
| vol buikje | full tummy | the after-effect of Eat Nibble |
| Badgedoosje / gymbadge | Badge Case / gym badge | |
| Dutjesbadge / Njegbadge / Knabbelbadge | Nap Badge / Nyeg Badge / Nibble Badge | |
| Guhmon-trainerspet | Guhmon Trainer Cap | outfit; not the Mewtwo "Trainerpetje" |
| "dicht wegens dutje" | "closed for a nap" | the other five gyms |
| Boer zoekt Guh | Farmer Wants a Guh | core glossary |
| Presentatrice Guhvon / Boer Guhrrit | Host Guhvon / Farmer Guhrrit | core glossary |
| Tukkie / Dommelien / Snurkbert | Snoozie / Dozeline / Snorebert | the three candidates |
| logeerweek | stay-over week | |
| instoppen / ingestopt | tuck in / tucked in | |
| slaapvrienden | nap friends | |
| aftiteling | credits | |
| Brievenbus / Ingelijste brief | Mailbox / Framed Letter | |
| Strohoed van Boer Guhrrit / Overall van Boer Guhrrit | Farmer Guhrrit's Straw Hat / Farmer Guhrrit's Overalls | outfit; not the old Straw Hat / Overalls |
| kijkbuisguhtjes | dear viewers | Guhvon's greeting |
| Guhpixel TV | Guhpixel TV | |

### among

| Dutch | English | note |
|---|---|---|
| Among Guhs / De Vadsvaarder | Among Guhs / The Chonkfarer | the game and its ship (kern glossary) |
| Kapitein-guh / Logboek-guh | Captain Guh / Logbook Guh | the queue; the personal numbers |
| Ruimteguh / ruimtepakje | Space Guh / space suit | the guh NPCs of a round |
| crew / Mika / Mika's | crew / Mika / Mikas | roles; never "impostor" |
| in slaap duwen / slaper / droomguh | to push asleep / sleeper / dream guh | nobody is hurt: never "kill", "dead", "body" |
| Mika-kussentje / Saboteerkaart / Stembriefje | Mika Pillow / Sabotage Map / Voting Slip | the game items |
| Taakjes-paneel / Noodknop / Ventilatieluik (luik) | Task Panel / Emergency Button / Vent Hatch (vent) | the ship blocks |
| vergadering / uitspraak / stemmen / overslaan / wegstemmen | meeting / statement / to vote / to skip / to vote out | |
| wachtrij / klaar / leider | queue / ready / leader | |
| Normaal / Lastig | Normal / Hard | difficulty |
| Licht uit / Knabbelalarm / Deuren dicht | Lights Out / Nibble Alarm / Doors Shut | the three sabotages |
| de Slaapzaal / de Kantine / de Navigatie / de Reactor | the Dorm / the Cafeteria / Navigation / the Reactor | room names, with their article inside sentences |
| de Ziekenboeg / Elektra / de Machinekamer | the Sick Bay / Electrical / the Engine Room | |
| de Voorraadkamer / de Schildkamer / de gang | the Storage Room / the Shield Room / the corridor | |
| Worstjes knopen / Pasje door de lezer / Kruimelbak legen | Tying Sausages / Card Through the Reader / Emptying the Crumb Tray | task names |
| Pindasaus tanken / Knabbels sorteren / Dromen downloaden | Fuelling Peanut Sauce / Sorting Nibbles / Downloading Dreams | |
| Wegen ("Resultaat: vads") / Schakelaars goedzetten | Weighing ("Result: chonk") / Setting the Switches | |
| Rood, Blauw, Groen, Geel, Roze, Oranje, Paars, Wit, Bruin, Mint | Red, Blue, Green, Yellow, Pink, Orange, Purple, White, Brown, Mint | a guh NPC is called by its color |
| sus | sus | stays "sus" |
| tellen (10 tellen) | seconds | |
| Sus | Sus | title (the only Among Guhs title since 1.3.1) |
| Onterecht weggestemd | Wrongly Voted Out | a number on the stats board |
| oefenrondje | practice round | the one-time parody round at Captain Guh (joke game "among") |
| SUS-stickerbord | SUS Sticker Board | the keepsake of the practice round |
| (kleur) ruimtepakje / luchttank | (color) Space Suit / air tank | shop clothes: "Red Space Suit" |
| Plantjeshoedje / Eihoedje / Wc-rolhoedje | Little Plant Hat / Fried Egg Hat / Toilet Roll Hat | shop hats |
| Kaaspunthoedje / Sus-briefje / Knabbelhoedje | Cheese Wedge Hat / Sus Note / Nibble Hat | |
| hendel / streep / bak / schakelaar / lampje / briefje | lever / line / bin / switch / little lamp / note | words of the task panels |
| kaasknabbel / knabbel / kruimel (the three bins) | Cheese / Nibble / Crumb | short labels on the bins |
| noodvergadering / agendapunt / besluit | emergency meeting / (agenda) item / decision | the Emergency Button at home |
| gluren (uit het luik) | to peek out (of the vent) | the Vent Hatch at home |
| cijferbord / logboek | stats board / logbook | the Logbook Guh's screen |

### guhkade

| Dutch | English | note |
|---|---|---|
| Guhkade | Guhcade | "arcade" + guh |
| Guhkade-kast / speelkast | Guhcade Cabinet / arcade cabinet | block names: *Guhcade Cabinet: Flappy Guh*, *Guhcade Cabinet: Mika Pong* |
| Flappy Guh | Flappy Guh | never translated |
| Mika-Pong | Mika Pong | no hyphen in English; on the cabinet's screen *MIKA PONG* |
| kaasknabbelpilaren | cheese nibble pillars | |
| fladderen | flap | the one button of Flappy Guh |
| batje | paddle | |
| potje | game | *een potje doen* = *play a game* |
| top 5 van deze kast | this cabinet's top 5 | |
| plafond (van een guh) | ceiling | the best score a kind of guh can ever get |
| sip kijken | look glum | the beaten guh; never "cry" |
| extra oefenen | practice extra | |
| NJEG! / VAHOEG! / TIJD! (op het scherm) | NYEG! / WAHOOG! / TIME! | capitals only: the cabinet's pixel font knows A-Z, digits and - ! ? . : + |

### kantoor

| Dutch | English | note |
|---|---|---|
| Guhkantoor | Guh Office | |
| Guhkantoor-set | Guh Office Set | shop offer: a Time Clock and one Little Desk |
| Guhkantoor B.V. (Besloten Vadsschap) | Guh Office Ltd. (Limited Chonkability) | the company name on every paper |
| Prikklok | Time Clock | |
| Bureautje | Little Desk | *bureau* in the screen = *desk* |
| loonstrookje | payslip | |
| kwartaalrapport | quarterly report | *boekjaar* = *fiscal year* |
| Oorkonde Werknemer van de maand | Employee of the Month Certificate | *oorkonde* = *certificate* |
| Werknemer van de maand | Employee of the Month | Title Case, also in running text |
| inklokken / uitklokken | clock in / clock out | buttons: *Aan het werk* = *Put to Work*, *Naar huis* = *Go Home* |
| dienst | shift | 8 real hours |
| postvakje | pigeonhole | holds 3 payslips per guh |
| bruto / netto / ingehouden / toeslag | gross / net / withheld / bonus | payslip lines |
| de baas / de directie | the boss / management | |
| Vadstoeslag | Chonk bonus | |
| Vakbond Vads Verenigd | Union Chonks United | |

### bioscoop

| Dutch | English | note |
|---|---|---|
| Guhbioscoop | Guh Cinema | core glossary; "theater" for the room (American) |
| film | movie | American; "Skyblok: De Film" = "Skyblok: The Movie" |
| Guhbioscoop-projector | Guh Cinema Projector | block |
| Bioscoopdoek | Cinema Screen | block; "a piece of Cinema Screen" |
| Bioscoopstoeltje | Cinema Seat | block |
| Popcornmachine | Popcorn Machine | block |
| Bakje popcorn | Tub of Popcorn | item |
| Guhbioscoop-set | Guh Cinema Set | shop offer |
| Filmavond / Filmkenner | Movie Night / Movie Buff | FTB quests |
| UITGESPEELD! | COMPLETED! | on the screen in the Skyblok movie |
| #1 VADSOVERWINNING | #1 CHONK ROYALE | the Chonknite win banner |
| slaapwolk | sleep cloud | Chonknite |
| Slaap ze allemaal! | Gotta nap 'em all! | Guhmon tagline |
| Vadsen / Njeg / Knabbel eten / Dutje (Guhmon moves) | Chonk / Nyeg / Eat Nibble / Nap | "Guh used Chonk!" |
| Het is super vadsig! | It's super chonky! | |
| logeerweek | sleepover week | Farmer Wants a Guh |
| Noodvergadering | Emergency meeting | Among Guhs |
| Rood is sus | Red is sus | never translated further |
| ventilatieluik | vent | |
| EINDE | THE END | credits |

### reisbureau

| Dutch | English | note |
|---|---|---|
| Reisbureau "De Vadsvakantie" | Travel Agency "The Chonk Vacation" | structure; on signs: Travel / Agency: The / Chonk Vacation |
| Reisagent-guh | Travel Agent Guh | NPC |
| Reisbalie / balie | Travel Desk / desk | block; the counter in the structure is the same block |
| Reisstempel | Travel Stamp | item, used in the Travel Desk recipe |
| reispas / stempel | travel pass / stamp | per player, not an item |
| ansichtkaart | postcard | item names: *Postcard: <trip>* |
| baas (a guh about its owner) | boss | postcards: *Hi boss!* |
| Veel vadsjes van | Lots of chonks from | the postcard's closing line |
| 1 knabbel (postage stamp) | 1 nibble | |
| Proefreisje om de hoek | Trial Trip Around the Corner | the questline's five-minute trip |
| Gouden koffertje / Koffertje | Golden Suitcase / Little Suitcase | deco blocks |
| Vadsen bij huize Lingsesdijk 86 | Chonking at House Lingsesdijk 86 | the user's inside joke: "Lingsesdijk 86" is never translated |
| Schilderij "Huize Lingsesdijk 86" | Painting "House Lingsesdijk 86" | wall block |
| Dagje Kaasmarkt | Day at the Cheese Market | |
| Middagdutje in het Vadswoud | Afternoon Nap in Chonkwood Forest | |
| Kinderboerderij Knuffeldal | Snuggledale Petting Farm | |
| Strandmiddag op Guhwai'i | Beach Afternoon on Guhwai'i | |
| Wellness in de Barbecuether | Wellness in the Barbecuether | |
| Pretpark de Efteguh | Efteguh Theme Park | Efteling pun, keep "Efteguh" |
| Holle Bolle Guh | Hungry Bulgy Guh | "Papier hier, njeg!" = "Paper here, nyeg!" |
| Guhkenhof | Guhkenhof | Keukenhof pun, keep |
| Wintersport in Nomguh | Winter Sports in Nomguh | |
| Stedentrip Guhrijs / Guhrijs | City Trip to Guhris / Guhris | Paris pun |
| Eiffelknabbeltoren(tje) | (Little) Eiffel Nibble Tower | |
| Camping De Vadsige Tent | Camp Chonky Tent | |
| Guhnetië | Guhnice | Venice pun |
| Reis naar de Kaasmaan / Kaasmaan | Trip to the Cheese Moon / Cheese Moon | |
| Wereldreis in 80 dutjes | Around the World in 80 Naps | |
| Cruise over de Guhzee | Cruise on the Guh Sea | |
| Thuisblijfvakantie "Balkonië" / Balkonië | Staycation "Balconia" / Balconia | |
| Thuis is het ook vads | Home Is Chonk Too | painting title |
| glimguhtjes-lantaarn | Glowguh Lantern | |
| Op vakantie | On vacation | American spelling: vacation, not holiday |

### parkour

| Dutch | English | note |
|---|---|---|
| Guh-parkour / parkour | Guh Parkour / parkour | the course for guhs; not the lobby parkour for players |
| Startpaaltje / Finishpaaltje | Start Post / Finish Post | core glossary |
| Guh-horde | Guh Hurdle | |
| Guh-springplank | Guh Springboard | |
| Kruiptunnel | Crawl Tunnel | not the Fluffy Tunnel (Pluizige tunnel) |
| Slalompaaltjes | Slalom Poles | |
| Evenwichtsbalk | Balance Beam | |
| Knabbeltafeltje | Nibble Table | |
| Parkour-scorebord / scorebord | Parkour Scoreboard / scoreboard | |
| stuk (van een route) / hindernis | piece (of a route) / obstacle | |
| route uitzetten / stukken aanklikken | laying out a route / click pieces | |
| rondje / rondetijd / record | lap / lap time / record | |
| op de route zetten / van de route halen | put on the route / take off the route | |
| bobbel | bump | the guh inside the Crawl Tunnel |
| verplichte pitstop | mandatory pit stop | |
| Hup! / Boing! / Oeps! | Hup! / Boing! / Oops! | |
| vadsje | little chonk | |

## 24. Guh Technology, the Barbecuether buildings, The Lord of the Nibble Ring, Super Guhrio (bbq2)

Chunks `c40` - `c62`. Everything above still holds: njeg = **nyeg**, vads = **chonk**, vahoeg = **wahoog**, knabbel =
**nibble**, guhtje in a name = **guhling**, drankje = **Potion**, kooltje = **Ember**, beeldje = **Statuette**, snoet =
**snoot**, buikje = **tummy**, American spelling (*cozy, neighbor, mustache, apartment, campground, range hood*).

**The one fixed line.** Guhdalf on the bridge (`scene.guhs.ringh3_brug.you_1` .. `you_4`, the advancement and the FTB quest
of the same name) says **"YOU.. SHALL.. NOT.. VADS!"** in BOTH languages, word for word: this one "VADS" is never "CHONK".

**Parodies keep their quote.** The Nibble Ring story is *The Lord of the Rings*, Super Guhrio is *Super Mario*: where the
Dutch bends a famous line, take the famous ENGLISH line and bend it the same way (table "famous lines" below). Names of the
cast keep their guh/Mika pun and follow the English names of the originals (Rivendel -> Rivendell, Merrie -> Merry).

**Nobody gets hurt.** A Mika, the Barbecuerog, the Eye, the Nine, a Flomp Mika or a falling ember only *shove* or *send you
back* to your rest fire / flag: never "kill", "die", "damage", "attack", "lives". The Eye *looks you back*, the Nine *sniff*.

**How they talk.**
- **Smikagol** hisses and says "we" for "I": stretch the s at the END of a word as the Dutch does (*vissss* -> *fishesss*,
  *oogjesss* -> *eyesss*, *Sssst!* -> *Shhhh!*), calls the ring AND the player *vadsje* = **chonkie** (*ons vadsje* = *our
  chonkie*, *Deze kant op, vadsje!* = *This way, chonkie!*). Never "precious".
- **Sam-guh** calls the player *baas* = **boss** (as the guhs on the postcards do) and Guhdalf *meneer Guhdalf* = *Mr. Guhdalf*.
- **Marktmeester-Mika** (a cat-like Mika) says *Mjauw* = **Meow**. **Pad-guh** says *Njam-njeg!* = *Nom-nyeg!*
- **Guhdalf**'s spells are dog Latin and stay: *Vadsus opendus! Knabbelum portalis!* -> *Chonkus opendus! Nibblum portalis!*
- Counted time in screens: *tellen* = *seconds* (as in section 23).

### vadskracht, bank, tech

| Dutch | English | note |
|---|---|---|
| Guh-technologie | Guh Technology | FTB chapter, advancement tab, Tales heading |
| vadskracht | chonk power | lowercase in prose; replaces the old *guhkracht* = *guh power* everywhere; no unit: "%s chonk power" |
| Knuffelkracht | Cuddle Power | advancement title |
| opstelling | setup | everything on one stretch of Guh Wire; *Opstelling 1* = *Setup 1* (practice hall) |
| guhmachine(s) / guh-machine | guh machine(s) | lowercase in prose; the block *Guhmachine* (machine_deel) = *Guh Machine* |
| Telt niet mee | Doesn't count | hover line of a source over the limit |
| Te zwaar / Te groot / staat uit | Too heavy / Too big / is off | hover lines of a setup |
| een machine met een snoet / snoetje | a machine with a snoot / little snoot | every guh machine has a face |
| Proefbron / Proefbatterij | Test Source / Test Battery | dev blocks |
| Proefmachientje / Groot proefmachientje | Little Test Machine / Big Test Machine | dev blocks |
| Knabbelbatterij | Nibble Battery | |
| Knuffelgenerator | Cuddle Generator | knuffel = the act here, so Cuddle |
| Disco-dynamo | Disco Dynamo | no hyphen |
| Blubkacheltje | Blub Stove | "little blub stove" in prose where the Dutch is cute about it |
| Gloeisterkern | Glowstar Core | *Een ster in een kooitje* = *A Star in a Little Cage* |
| draaitafel / bakje | turntable / little tray | parts of the dynamo and the stove |
| Knabbelbuis / Knabbelbuizen | Nibble Tube / Nibble Tubes | as *hamster tubes*, *Tube Workbench* |
| Richtingstuk | Direction Piece | |
| Filterstuk | Filter Piece | |
| Opzuiger | Slurper | *Slurp!* stays |
| Voorraadmeter | Stock Meter | |
| Snuffelsensor | Sniff Sensor | not the Nibble Sensor (Knabbelsensor) |
| Guhklok / Guhteller | Guh Clock / Guh Counter | |
| Rollebollen / Kieskeurig guhtje / De guh ziet alles | Roly-Poly / Picky Little Guh / The Guh Sees All | advancement titles |
| Sauspomp / Sausslang / Sausvat | Sauce Pump / Sauce Hose / Sauce Vat | |
| Grillkoolpers | Grill Coal Press | |
| Brouwautomaat | Auto Brewer | |
| Frituurautomaat | Auto Fryer | |
| Saus uit de grond / Tapje, njeg? | Sauce from the Ground / A Little Tap, Nyeg? | advancement titles |
| Roeren is voor vroeger / Vanzelf vahoeg | Stirring Is So Yesterday / Wahoog All by Itself | advancement titles |
| Onder druk wordt alles grillkool | Under Pressure Everything Turns to Grill Coal | |
| Oogster | Harvester | |
| Knabbelaar | Nibbler | the block breaker |
| Neerzetter | Placer | |
| Knutselmachine | Tinker Machine | *knutselen* (of this machine) = *to tinker*: *Het knutselt vanzelf* = *It Tinkers by Itself* |
| Tekentafel | Drawing Table | |
| Bouwtekening / Lege bouwtekening / Bouwtekening: %s | Blueprint / Empty Blueprint / Blueprint: %s | also *Bouwtekening: Mika-brug* = *Blueprint: Mika Bridge* |
| Plantagebak | Plantation Box | bak = Box (as *Chonk Seed Box*); *Een bos in een bak* = *A Forest in a Box* |
| Vadsmolen | Chonk Mill | |
| Stepstation | Scooter Station | step = kick scooter |
| Haltepaaltje / halte | Stop Post / stop | *aflever-halte* = *drop-off stop* |
| Bezorgguhtje | Delivery Guhling | creature, variant, Guhdex page; *Bezorgguhtje-fluitje* = *Delivery Guhling Whistle* |
| Ophalen / Afleveren | Pick Up / Drop Off | what a stop does |
| Rugzak / Haltes / Naar huis! / Ander station / Leegmaken | Backpack / Stops / Go Home! / Other Station / Clear | Scooter Station screen |
| Tuut tuut! / Bezorgd, njeg! / Op je wenken bediend | Toot Toot! / Delivered, Nyeg! / At Your Beck and Call | advancement titles |
| Hapluikje / luikje | Nom Hatch / hatch | hap = nom: *Hap!* = *Nom!*, *het luikje hapt* = *the hatch noms* |
| Banksleutel | Bank Key | |
| Bodemloos Knabbelmaagje | Bottomless Nibble Belly | the bank upgrade; *Bodemloos buikje* = *Bottomless Tummy* |
| Vol is vol / opgevoerd | Full Is Full / upgraded | bank: 256 of each kind, "%s/%s" |
| Machines bijvullen & leeghalen | Refill & Empty Machines | chore name, in the style of *Tidy Up & Sort* |
| Plantage: planten & hakken | Plantation: Plant & Chop | chore name |
| voordoen / voorgedaan | show it once / shown | what a guh learns to refill |
| klus-area / klusgebied | chore area | one word for both |
| scheutjes | sprouts | as *Peanut Sprouts* |
| Collega's met een snoet / Houthakkertje guh | Colleagues with a Snoot / Little Lumberjack Guh | advancements, diary firsts |
| Hap, opgeruimd! / Pompoenenplukker / Machinist guh | Nom, All Tidy! / Pumpkin Picker / Machinist Guh | |
| Oude Guhrad-centrale | Old Guh Wheel Power Plant | structure; "the power plant" in prose |
| Uitvinder-guh | Inventor Guh | |
| oefenhal | practice hall | *De oefenhal draait weer* = *The Practice Hall Is Running Again* |
| Project 1: Nooit meer zelf bakken | Project 1: Never Bake Yourself Again | FTB sections |
| Project 2: Alles vanzelf in de bank | Project 2: Everything into the Bank by Itself | |
| Project 3: Saus uit de kraan | Project 3: Sauce on Tap | |
| Project 4: Een fabriek die doorwerkt | Project 4: A Factory That Keeps Going | |
| De Grote Knabbelmachine | The Great Nibble Machine | *Stukje Grote Knabbelmachine* = *Piece of the Great Nibble Machine* |
| Knabbelmachine-beeldje | Nibble Machine Statuette | |
| Knabbelmachinist | Nibble Machinist | title |
| Perfecte knabbel | Perfect Nibble | |
| Receptkaart / receptkaarten | Recipe Card / recipe cards | *Receptkaart: saus en slangen* = *Recipe Card: Sauce and Hoses* |
| Receptkaart: knabbelende machines / het Bezorgguhtje / Plantagebak | Recipe Card: Nibbling Machines / The Delivery Guhling / Plantation Box | |
| Oefenknabbel / Propje papier | Practice Nibble / Paper Wad | practice hall items |
| Ome Knabbel | Uncle Nibble | a sign in the practice hall |
| Wordt vervolgd in het Guheinde... | To Be Continued in the Guh End... | the locked FTB slot |
| Bestelguh / Guhterminal / draadloze vadskracht | Order Guh / Guh Terminal / wireless chonk power | named in that slot only |

### gebouwen (the Guhbarbecuether buildings)

| Dutch | English | note |
|---|---|---|
| frituursauszee / sauszee | frying sauce sea / sauce sea | the Barbecuether's lava sea of Cheese Frying Sauce |
| Sausloper | Sauce Strider | vanilla Strider nod |
| Sausblubje / blubje | Sauce Blubby / blubby | plural *Sauce Blubbies*; *Blubje to go* = *Blubby to Go* |
| Blubroom | Blub Cream | "blub cream" in prose |
| Sausblubje in een potje | Sauce Blubby in a Jar | a creature in a jar, not "Potted" |
| Pindasaus aan een stok | Peanut Sauce on a Stick | |
| Stuiterdrankje / Stuiterblub | Bouncy Potion / Bouncy Blub | Guh Potion and its effect |
| Sausloper-stal | Sauce Strider Stable | |
| Verzorger-guh | Caretaker Guh | not the Stable Hand Guh of the Mika Stable |
| sausbak / proefrit / proefrondje | sauce basin / test ride / test lap | |
| Sausloper-vriend / Sausracer | Sauce Strider Friend / Sauce Racer | |
| Lange poten, klein hartje / Kom maar, langpoot! | Long Legs, Little Heart / Come On, Longlegs! | |
| Vriendschap gaat door de maag / Een knuffel te veel / Boing! | Friendship Goes Through the Tummy / One Cuddle Too Many / Boing! | |
| Rookguh-vuurtoren | Smoke Guh Lighthouse | |
| Torenwachter-guh | Lighthouse Keeper Guh | |
| Pepertuin | Pepper Garden | *Pluktuin* = *Picking Garden* |
| Peperteler-guh | Pepper Grower Guh | |
| Vuurtorenlamp / Seinlantaarn / Lampkooltje | Lighthouse Lamp / Signal Lantern / Lamp Ember | |
| Verdwaalde Rookguh | Lost Smoke Guh | |
| Kweekbak / Peperplant / Peperzaadjes | Grow Box / Pepper Plant / Pepper Seeds | |
| Njegpeper / Vahoegpeper / Snoeppeper | Nyeg Pepper / Wahoog Pepper / Candy Pepper | green / red / pink |
| Pepervuurdrankje / Peperzoetdrankje / peperdrankje | Pepperfire Potion / Peppersweet Potion / pepper potion | |
| Peperadem | Pepper Breath | effect |
| Wachtersjas van de vuurtoren / Peperslinger | Lighthouse Keeper's Coat / Pepper Garland | outfit pieces |
| Licht in de rook / Peperbrouwer / Lampaansteker | A Light in the Smoke / Pepper Brewer / Lamplighter | |
| Heet! Heet! Heet! / Ahoi, njeg! / Fiet-fiew! | Hot! Hot! Hot! / Ahoy, Nyeg! / Fweet-fwee! | |
| Mika-woonblokken | Mika Apartments | American: never "flats" |
| flat / westflat / oostflat / hoge flat | apartment block / west block / east block / tall block | *daktuin* = *roof garden*, *galerij* = *walkway* |
| Mika-stal | Mika Stable | |
| Mika-brugpaleis | Mika Bridge Palace | *Door de bek van de Mika* = *Through the Mika's Mouth* |
| Mika-oma | Granny Mika | as *Granny Nibbles* |
| Mopper-Mika / mopperaars | Grumpy Mika / grumps | |
| Brom-Mika / Zeur-Mika / Snurk-Mika | Grumble Mika / Whiny Mika / Snore Mika | no hyphens, as every Mika |
| Buur-Mika / Barbecue-Mika / Zonnebad-Mika | Neighbor Mika / Barbecue Mika / Sunbathing Mika | |
| Worstzwijntje | Sausage Piglet | creature, variant |
| Knorretje | Oinky | the runaway piglet; *Knorretje (in je armen)* = *Oinky (in Your Arms)* |
| Knir / Snuffel / Mosterdje / Truffel | Snort / Snuffles / Mustard / Truffle | name signs in the stable |
| Knor! / Zwijntjesfluisteraar | Oink! / Piglet Whisperer | |
| Stalknecht-guh | Stable Hand Guh | |
| Tolwachter-Mika | Toll Keeper Mika | *HALT! Tol!* = *HALT! Toll!*, *Tolvrij!* = *Toll-Free!* |
| tolbel / tolhuis / tolbrug | toll bell / toll house / toll bridge | *DONG!* stays |
| Raadselkoning / Mika-kwaliteit | Riddle King / Mika Quality | |
| Oma's worstsoep / Soep van oma | Granny's Sausage Soup / Granny's Soup | |
| Breiwerk van Mika-oma / Breimandje van Mika-oma | Granny Mika's Knitting / Granny Mika's Knitting Basket | |
| Gebreide Mika-muts | Knitted Mika Cap | muts = cap |
| Zak zwijnenvoer | Sack of Piglet Feed | |
| Worstzwijntje in een mandje | Sausage Piglet in a Basket | |
| Brugplank van de Tolwachter / Mika-brugplanken / Touwleuning | Toll Keeper's Bridge Plank / Mika Bridge Planks / Rope Railing | |
| Wachter-guh | Guard Guh | at the Skewer Keep; not the Gatekeeper |
| Ere-wachter / wachterspak | Honorary Guard / guard outfit | |
| Wachtershelm / Wachtersmantel | Guard's Helmet / Guard's Cloak | |
| Knuffelmaker-guh | Plushie Maker Guh | |
| Knuffelguh / Knuffel-Mika / Knuffel-Rookguh | Plush Guh / Plush Mika / Plush Smoke Guh | the three plushie BLOCKS; in prose they are "plushies" |
| Knuffelpatroon | Plushie Pattern | |
| De wacht bij de Spiesburcht / De gestolen knuffels | The Watch at the Skewer Keep / The Stolen Plushies | storylines |
| Brugvuurkorf / brugvuren | Bridge Brazier / bridge fires | *Fwoesj!* = *Fwoosh!* |
| Mikakruid | Mika Weed | onkruid pun; *wieden* = *to weed* |
| Zielig lantaarntje | Sad Little Lantern | |
| Aansteekspies van de Wachter-guh / Lantaarnrecept van de Wachter-guh | Guard Guh's Lighting Skewer / Guard Guh's Lantern Recipe | |
| Op kousenvoetjes / Knuffels horen bij guhs | On Tiptoe / Plushies Belong with Guhs | |
| wachthokje / naaihoek | sentry box / sewing corner | |
| Zoutkristal | Salt Crystal | |
| Zoutkristalmijn | Salt Crystal Mine | |
| Zoutkristalader / Zoutkristalerts / Zoutkristalblok / Zoutkristalletjes | Salt Crystal Vein / Salt Crystal Ore / Salt Crystal Block / Little Salt Crystals | *kristalader* = *crystal vein* |
| Zoutkristalhouweel | Salt Crystal Pickaxe | |
| Puin op het spoor | Rubble on the Track | *karrenspoor* = *cart track* |
| Mijnwerker-guh | Mineworker Guh | not the Miner Guh (Mijnguh) of the Cheese Mine |
| Fossiel-opgraving | Fossil Dig | |
| Archeoloog-guh | Archaeologist Guh | |
| Bottenzand / Skeletrek | Bone Sand / Skeleton Rack | |
| Guhkwastje | Guh Dusting Brush | not the Guh Brush (Guhborstel) of the farm; *kwasten* = *to dust* |
| Tyrannoguhrus Njex / Njex | Tyrannoguhrus Nyex / Nyex | *Njex!* = *Nyex!* |
| Tyrannoguhrus-beeldje | Tyrannoguhrus Statuette | |
| Tyrannoguhrus-schedel / -ruggengraat / -ribben / -pootjes / -staartje | Tyrannoguhrus Skull / Spine / Ribs / Legs / Tail | |
| Guhceratops | Guhceratops | stays |
| Een korreltje zout / Zout op de boterham / Njex op de kast | A Grain of Salt / Salt on Your Sandwich / Nyex on the Shelf | |
| Botten in de as / Een berg van zout / De vondst van de eeuw | Bones in the Ash / A Mountain of Salt / The Find of the Century | |
| Grillcamping | Grill Campground | not the Snuggledale Campsite (Kampeerplekje) |
| De Gloeiende Guh | The Glowing Guh | the campground's name |
| Kampbaas-guh | Camp Boss Guh | |
| Houthakker-guh | Lumberjack Guh | not the Woodcutter Guh of the Palewood; *Tjak! Tjak!* = *Chop! Chop!* |
| Nether-Mika-ruilmarkt / ruilmarkt | Nether Mika Barter Market / barter market | piglin-barter nod |
| Marktmeester-Mika / Marktmeester | Market Master Mika / Market Master | says *Mjauw* = *Meow* |
| Kraam-Mika | Stall Mika | |
| Saté-Mika / Vads-Mika / Kolen-Mika | Satay Mika / Chonk Mika / Coal Mika | the three stalls |
| de Waag / Weegschaal van de Waag | the Weigh House / Weigh House Scale | |
| Stapel vads / nepvads | Stack of Chonk / fake chonk | *De nepvads* = *The Fake Chonk*; *NEP!* = *FAKE!* |
| Keurstempel van de Marktmeester | Market Master's Seal of Approval | |
| afdingen / Meester-afdinger | to haggle / Master Haggler | *bod* = *offer* |
| koopje van de dag / extraatje | deal of the day / little extra | |
| Smikkel | Smikkel | the Market Master's nephew, stays |
| tentdoek (Rood/Blauw/Geel/Groen/Crèmekleurig) | Tent Canvas (Red/Blue/Yellow/Green/Cream) | *Red Tent Canvas* |
| tentdoek (schuin) / tentdoekplaat | Tent Canvas (Sloped) / Tent Canvas Slab | *Red Tent Canvas (Sloped)*, *Red Tent Canvas Slab* |
| Tentzak / Tentharing / Kampeerplekbordje | Tent Bag / Tent Stake / Campsite Sign | |
| Hakblok / Groot kampvuur / Bos brandhout / Roosterstok | Chopping Block / Big Campfire / Bundle of Firewood / Roasting Stick | *goudbruin* = *golden brown* |
| Kampeerhoedje / Padvindersdasje / Kampeerrugzak / kampeerpakje | Camping Hat / Scout Neckerchief / Camping Backpack / camping outfit | |
| Kampeerguh / Kampregels | Camper Guh / Camp Rules | |
| Kamperen bij De Gloeiende Guh / De nepvads van de ruilmarkt | Camping at The Glowing Guh / The Fake Chonk of the Barter Market | storylines |

### verhaal (the story engine)

| Dutch | English | note |
|---|---|---|
| Mijn verhaal | My Story | Super Compass entry; in prose a tale you follow is a "story", the tab stays *Tales* |
| Doel op het scherm | Objective on Screen | *aan / uit* = *on / off* |
| Reiskaart | Journey Map | *De reis van de Knabbelring* = *The Journey of the Nibble Ring* |
| Je bent hier / Dit moet je nu doen | You are here / What to do now | on the Journey Map |
| vertelkaart | story card | the narrator's card before a chapter |
| filmpje | scene | a cutscene: *Kijk het filmpje* = *Watch the scene* |
| Opnieuw bekijken | Watch Again | heading |
| Hoofdstuk %s | Chapter %s | |
| Guhdalfs sluier / sluier / rookmuur | Guhdalf's Veil / veil / wall of smoke | the smoke wall around a chapter you have not reached |
| Guhdalf vindt dat je hier nog niet aan toe bent, njeg | Guhdalf thinks you're not ready for this yet, nyeg | |
| Je volgt dit verhaal / vanzelf / vastzetten | You're following this story / automatically / pin | |
| Dit hoort bij het verhaal / bij het gebouw | This is part of the story / of the building | protected ground |
| rustpunt / Rustpunt | rest stop / Rest Stop | structure name *Rest Stop* |
| Rustvuurtje | Rest Fire | where a shove sends you back to |

### Knabbelring (The Lord of the Nibble Ring)

| Dutch | English | note |
|---|---|---|
| In de ban van de Knabbelring | The Lord of the Nibble Ring | the Dutch is the Dutch title of *The Lord of the Rings*: FTB chapter, advancement tab, Tales heading, clothing source |
| De Knabbelring / de ring | The Nibble Ring / the ring | item *The Nibble Ring*; "the Nibble Ring" in prose |
| Ringdrager / ringdrager / ringdragertje | Ring-Bearer / ring-bearer / little ring-bearer | title *Ring-Bearer* |
| Het Reisgenootschap van de Knabbelring / het Reisgenootschap | The Fellowship of the Nibble Ring / the Fellowship | *reisgenoten*, *het gezelschap* = *the companions*, *the company* |
| Guhdalf | Guhdalf | stays; *Guhdalf de Grijze / de Witte* = *Guhdalf the Grey / the White* (the one British "Grey": it is his name) |
| Sam-guh | Sam-guh | stays, with the hyphen |
| Araguh / Leguhlas / Gimguh / Boromika / Pippguh | Araguh / Leguhlas / Gimguh / Boromika / Pippguh | stay |
| Merrie | Merry | |
| Guhrond / Guhladriel | Guhrond / Guhladriel | stay; *Vrouwe Guhladriel* = *Lady Guhladriel* |
| Smikagol | Smikagol | stays; *Smikagol (krokant)* = *Smikagol (Crispy)* |
| Guhstapper | Guhstrider | Araguh's nickname (Stapper = Strider) |
| Gimguh, zoon van Gluhin | Gimguh, son of Gluhin | |
| Durguh | Durguh | stays; *Durguh, Heer van Knabbelmoria* = *Durguh, Lord of Nibblemoria* |
| Sausron | Sauceron | |
| Het Oog van Sausron / het Oog | The Eye of Sauceron / the Eye | *Beeldje van het Oog van Sausron* = *Eye of Sauceron Statuette* |
| Sausuman | Sauceruman | *Sausuman van de Vele Sauzen* = *Sauceruman of Many Sauces* |
| Barbecuerog | Barbecuerog | stays; *Braadworstzweep* = *bratwurst whip* |
| Knekel-Mika-ruiter / de Negen | Skelly Mika Rider / the Nine | Knekel-Mika = Skelly Mika |
| Uruk-Mika | Uruk-Mika | stays |
| dwerg-guh / boomguhs / Gouwguh | dwarf guh / tree guhs / Shire Guh | *dwergendeur* = *dwarf door* |
| Rookguhje | Smoke Guhling | the three caged ones on Mount Fry |
| Knabbelgouw | Nibble Shire | "the Nibble Shire" in prose; *de Gouw* = *the Shire* |
| Knabbel-eind | Nibble End | Bag End |
| heuvelholletje / moestuin / Hovenier | hill hole / vegetable patch / Gardener | |
| Bakkerguh Knabbelings | Baker Guh Nibbins | Baggins |
| Tante Lobelia Guhzak | Aunt Lobelia Guhsackville | Sackville-Baggins |
| Visser Guhpkuil | Fisher Guhpool | |
| Rozie Katoenguh | Rosie Cottonguh | |
| Een langverwacht knabbelfeest | A Long-Expected Nibble Party | chapter 1 |
| afscheidsfeest / Guhdalfs vuurwerkkist / vuurpijl | farewell party / Guhdalf's Fireworks Crate / rocket | *Fwoesh!* = *Fwoosh!* |
| Feesttafel van de Knabbelgouw / Proviandkrat / Heuvelschoorsteentje | Party Table of the Nibble Shire / Provisions Crate / Little Hill Chimney | *proviand* = *provisions* |
| tweede ontbijt / derde ontbijt / elfuurtje / tussendoorknabbel | second breakfast / third breakfast / elevenses / in-between nibble | |
| grillportaal | grill portal | the Barbecuether portal of the big Barbecue Pit |
| Guhvendel | Guhvendell | Rivendell |
| Het Laatste Knusse Huis | The Last Cozy House | Last Homely House |
| De Raad van Guhrond / raadskring / raadsbel | The Council of Guhrond / council ring / council bell | chapter 2 |
| Knabsil / de scherven van Knabsil | Nibsil / the shards of Nibsil | Narsil |
| De Mijnen van Knabbelmoria / Knabbelmoria | The Mines of Nibblemoria / Nibblemoria | chapter 3; knabbel = nibble here too |
| Brug van Knabbel-dûm / Knabbel-dûm | Bridge of Nibble-dûm / Nibble-dûm | Khazad-dûm; keep the û |
| De Poort van Durguh / Diepe Poort / Zuilenhal / Hal van de Hefbomen | The Gate of Durguh / Deep Gate / Hall of Pillars / Hall of Levers | |
| Zeg njeg en treed binnen | Say Nyeg and Enter | "Speak friend and enter"; *Sesam, open u!* = *Open sesame!* |
| Brokkelpad / Brokkelsteen | Crumble Path / Crumble Stone | |
| de Kloof van Knabbelmoria | the Chasm of Nibblemoria | |
| Dwergen-hefboom / hefboom | Dwarf Lever / lever | |
| Runensteen van Knabbelmoria / rune / tombe | Runestone of Nibblemoria / rune / tomb | |
| Trommels in de diepte / Doem... doem... | Drums in the deep / Doom... doom... | |
| De emmer van Pippguh / Dwaas van een Pippguh | Pippguh's Bucket / Fool of a Pippguh | "Fool of a Took" |
| Aan de worst geregen / Even de diepte in / Eronderdoor | Strung on the Sausage / A Quick Trip into the Deep / Underneath | advancement titles |
| De Spiegel van Guhladriel / Spiegel van Guhladriel | The Mirror of Guhladriel / Mirror of Guhladriel | chapter 4 / the block |
| Guhlórien / Caras Guhladhon | Guhlórien / Caras Guhladhon | stay; *boomstad* = *tree city*, *het gouden woud* = *the golden wood* |
| de Grote Spies | the Great Skewer | the mallorn of the tree city |
| gastenvlonder | guest platform | |
| de Guhduin / de Arguhnath / de Guhkoningen | the Guhduin / the Arguhnath / the Guh Kings | *de Pilaren van de Guhkoningen* = *the Pillars of the Guh Kings* |
| de Sausval van Rauguhs | the Sauce Falls of Rauguhs | |
| Elfenbootje / elfenogen | Elven Boat / elf eyes | |
| De gaven van Guhladriel / Guhladriels zegen | The Gifts of Guhladriel / Guhladriel's blessing | |
| Lichtflesje | Light Phial | |
| Elfenmanteltje | Little Elven Cloak | the story item ("be a rock"); the outfit is *Elfenmantel met blaadjesspeld* = *Elven Cloak with Leaf Pin* |
| Elfentouw / Elfentouwhaak | Elven Rope / Elven Rope Hook | |
| De Zwarte Roosterpoort | The Black Grill Gate | chapter 5; rooster = grill, as *Grill Iron* |
| Roosterwachter / Poortrooster / Roosterpoortje | Grill Guard / Gate Grate / Little Grill Door | |
| Blik van het Oog | Gaze of the Eye | |
| het Asveld / de Kale Vlakte / de Schaduwlaan | the Ash Field / the Bare Plain / Shadow Lane | in Ash Vale (Asdal) |
| het Wachthek / de Slakkenhut / de Holte | the Guard Fence / the Slag Hut / the Hollow | |
| de schedelpaal / de uitkijkrug / sluipweg | the skull post / the lookout ridge / sneak path | |
| Kamp van de Reisgenoten / Basiskamp | Camp of the Fellowship / Base Camp | |
| Ik ben een rots, njeg / Achterom | I'm a Rock, Nyeg / Round the Back | |
| De Frituurberg | Mount Fry | chapter 6 and structure; Mount Doom |
| Frituurspleet | the Crack of Fry | Crack of Doom; scene title *The Crack of Fry* |
| Kronkelpad / Eerste, Tweede, Derde Richel / Bakrandje | the Winding Path / First, Second, Third Ledge / the Frying Edge | |
| Kooislot van de Frituurberg / Vallend kooltje | Cage Lock of Mount Fry / Falling Ember | |
| afzuigkap | range hood | *Geen afzuigkap meer* = *No More Range Hood*; signs *AFZUIGKAP 1* = *RANGE HOOD 1* |
| Stukje gefrituurde Knabbelring | Piece of the Fried Nibble Ring | |
| Toren van Sausuman | Tower of Sauceruman | |
| De Ringenbakker | The Ring Baker | his machine |
| Mika-rad | Mika Wheel | the wheel an Uruk-Mika trudges in (a Mika does not run) |
| Sputterpijp / Deegkneder / Sauskraan / Kaaskast | Sputter Pipe / Dough Kneader / Sauce Tap / Cheese Cupboard | |
| Mokhoek / mokken / Ik mok | Sulking Corner / to sulk / I'm Sulking | |
| Pannantír | Pannantír | stays (palantír + pan) |
| Voorraad van Sausuman | Sauceruman's Supplies | |
| Uienring / Ringdeeg | Onion Ring / Ring Dough | |
| Kannetje hete frituursaus / Ui uit de Kaaskast | Jug of Hot Frying Sauce / Onion from the Cheese Cupboard | |
| Stoofpotje van Sam-guh / stoofpotje | Sam-guh's Stew / stew | |
| Feestknabbel | Party Nibble | |
| Smikagols vissenbotje | Smikagol's Fish Bone | |
| lekkere vissss | tasty fishesss | |
| Guhdalfs punthoed met baard | Guhdalf's Pointy Hat with Beard | outfit |
| Harige hobbitvoetjes / De ring aan een kettinkje | Hairy Hobbit Feet / The Ring on a Chain | outfit |
| Een eigen Sam-guh / Een eigen Guhshi | A Sam-guh of Your Own / A Guhshi of Your Own | advancements |
| Mewtwo-guh (in the mirror wink) | Guhtwo | the Dutch line means Guhtwo (section 8) |

**Famous lines (the Nibble Ring).**

| Dutch | English | note |
|---|---|---|
| YOU.. SHALL.. NOT.. VADS! | YOU.. SHALL.. NOT.. VADS! | identical, see the top of this section |
| Een tovenaar komt nooit te laat. Hij komt precies wanneer de knabbels klaar zijn | A wizard is never late. He arrives precisely when the nibbles are ready | title: *A Wizard Is Never Late* |
| Bewaar hem geheim. Bewaar hem heel. En wat je ook doet: NIET opeten. | Keep it secret. Keep it whole. And whatever you do: do NOT eat it. | |
| Kook ze, stamp ze, stop ze in een stoofpotje | Boil 'em, mash 'em, stick 'em in a stew | *Aardappelen!* = *Po-ta-toes!* |
| Verder van huis dan ooit | Farther from Home than Ever | |
| Ik ga niet zonder jou / Ik ga mee! | I'm Not Going Without You / I'm Coming Too! | |
| Ik neem de ring wel mee! Al weet ik de weg niet. | I will take the ring! Though I do not know the way. | |
| Men wandelt niet zomaar naar de Frituurberg | One does not simply walk to Mount Fry | |
| Pootjes vegen, njeg | Wipe your paws, nyeg | the line; on the three SIGNS it is *Wipe paws, nyeg* (a sign line is 90 pixels wide) |
| dingen die nog gebakken moeten worden | things that have yet to be baked | the mirror; title *Wat nog gebakken moet worden* = *What Has Yet to Be Baked* |
| Niet duister, maar ROND en VADS als de dageraad! | Not dark, but ROUND and CHONK as the dawn! | |
| Ik heb alleen maar een beetje trek | I'm only a tiny bit hungry | |
| Tot hier, en geen kruimel verder / Vaarwel, Guhlórien | This Far, and Not a Crumb Further / Farewell, Guhlórien | |
| Wij zweren het op het vadsje / Ons vadsje | We Swears It on the Chonkie / Our Chonkie | |
| Ruik jij ook knabbel? / Wie het licht heeft | Do You Smell Nibble Too? / Whoever Holds the Light | |
| Eén knabbel om ze allemaal te delen | One Nibble to Share Them All | the story's motto (FTB subtitle) |
| Eén ring om ze allemaal op te eten / Eén uienring om ze allemaal op te eten | One Ring to Eat Them All / One Onion Ring to Eat Them All | |
| Ik kan de ring niet dragen... maar wel jou, njeg! / Maar wel jou | I can't carry the ring... but I can carry you, nyeg! / But I Can Carry You | |
| Het prikt in onze oogjesss | It Stings Our Little Eyesss | |
| Daar staat hij dan / Lang leve de koning | There It Stands / Long Live the King | |
| Niet omdoen! / De Negen te snel af / Even uitpuffen | Don't Put It On! / Outrunning the Nine / Catching Your Breath | |
| Iedereen wil een hapje / Er is altijd wel iets te vieren | Everyone Wants a Bite / There's Always Something to Celebrate | |

### Super Guhrio

| Dutch | English | note |
|---|---|---|
| Super Guhrio / Guhrio / Luiguh | Super Guhrio / Guhrio / Luiguh | stay |
| Guhmba / Guhshi | Guhmba / Guhshi | stay; plural *Guhmbas* |
| Kasteel van de Grote Nether-Mika | Big Nether Mika's Castle | "Bowser's Castle" |
| Grote Nether-Mika | Big Nether Mika | as *Big Mika* |
| Prinses Perzikguh | Princess Peachguh | |
| Pad-guh / Pad-guh's kraam | Toad-guh / Toad-guh's Stall | |
| Schild-Mika / Schild-Mika-schild | Shell Mika / Shell Mika Shell | Koopa |
| Plof-Mika | Flomp Mika | Thwomp; PLOF! = FLOMP! |
| Hapbloem | Chomp Flower | Piranha Plant |
| Draaiende grillspies / Naaf van een grillspies | Spinning Grill Skewer / Grill Skewer Hub | fire bar |
| Superknabbel / Gegooide knabbel | Super Nibble / Thrown Nibble | |
| Vuurpeper / Vuurpeperstruik | Fire Pepper / Fire Pepper Bush | |
| Grote vadsmunt / vadsmunten | Big Chonk Coin / chonk coins | *(al gehad)* = *(Already Collected)*; *Achttien keer vads* = *Eighteen Times Chonk* |
| Guhrio-munt / munten | Guhrio Coin / coins | what Toad-guh's stall takes |
| Ei van Guhshi / Een gespikkeld ei | Guhshi's Egg / A Speckled Egg | |
| Vraagtekenblok / Onzichtbaar vraagtekenblok | Question Block / Hidden Question Block | |
| Knabbel-vraagtekenblok | Nibble Question Block | the one for at home |
| Uitroeptekenschakelaar / Schakelblok / klokschakelaar | Exclamation Switch / Switch Block / timer switch | |
| Valblok / Platform | Falling Block / Platform | |
| Groene pijp / Groene pijp (onderstuk) | Green Pipe / Green Pipe (Base) | |
| Warppijp / Warppijp (onderstuk) | Warp Pipe / Warp Pipe (Base) | |
| Groene reispijp | Green Travel Pipe | the one for at home |
| Levelpoort / levelhal | Level Gate / level hall | |
| Guhrio-vlaggetje / Guhrio-vlaggenmast / Guhrio-startvlag | Guhrio Flag / Guhrio Flagpole / Guhrio Start Flag | the flag is the checkpoint a shove sends you back to |
| Kasteelvlaggenmast | Castle Flagpole | the one for at home |
| Guhrio-blok / Guhrio-steen / Guhrio-grond / Guhrio-deur / Guhrio-siersteen | Guhrio Block / Guhrio Brick / Guhrio Ground / Guhrio Door / Guhrio Trim Brick | |
| ...-plekje (Guhmba-, Hapbloem-, Platform-, Plof-Mika-, Schild-Mika-, Valblok-) | ... Spot (*Guhmba Spot*, *Chomp Flower Spot*, ...) | builder marker blocks; *Wachtplekje van Guhshi* = *Guhshi's Waiting Spot*, *Plekje van de Grote Nether-Mika* = *Big Nether Mika's Spot* |
| Guhrio-tip / Guhrio-geheimpje | Guhrio Tip / Guhrio Secret | |
| Geschilderd(e) gras / aarde / heg / wolk / lucht | Painted Grass / Dirt / Hedge / Cloud / Sky | the painted scenery of world 1 |
| Geschilderde heuvel / heuvel met oogjes / Geschilderd tuinplantje | Painted Hill / Painted Hill with Eyes / Painted Garden Plant | |
| Geschilderd loof in de verte / wolkje in de verte / wolkje met een snoet | Painted Distant Leaves / Painted Distant Cloud / Painted Cloud with a Snoot | |
| Wereld 1: de binnentuin / Wereld 2: de kelders / Wereld 3: de burcht | World 1: The Courtyard / World 2: The Cellars / World 3: The Keep | burcht = Keep, as *Skewer Keep* |
| Level 1-1: de binnentuin / Level 1-2: de heggentuin | Level 1-1: The Courtyard / Level 1-2: The Hedge Garden | |
| Level 2-1: De buizenkelder / Level 2-2: Het nest van Guhshi | Level 2-1: The Pipe Cellar / Level 2-2: Guhshi's Nest | |
| Level 3-1: De Grillgang / Level 3-2: De Sauskelder | Level 3-1: The Grill Corridor / Level 3-2: The Sauce Cellar | |
| Het hele kasteel in één keer | The Whole Castle in One Go | high score row |
| het mollenhol / de muntenkas | the Mole Hole / the Coin Greenhouse | the secrets of world 1 |
| Keldergrond / Keldersteen | Cellar Ground / Cellar Brick | |
| Eierslot / Broednest van Guhshi / Broedplekje | Egg Lock / Guhshi's Hatching Nest / Hatching Spot | |
| Roosterbrug / Brughendel | Grate Bridge / Bridge Lever | |
| Guhshi-parkeerpaal | Guhshi Hitching Post | |
| Gloeiend kooltje (the duel) | Glowing Ember | the existing name |
| Taartkarretje van Prinses Perzikguh | Princess Peachguh's Cake Cart | |
| voorplein / torenkamer / Peperkamer / schatkamer | forecourt / tower room / Pepper Room / treasure room | |
| Highscorebord van het kasteel | Castle High Score Board | |
| Rode Guhrio-pet met snor / Groene Luiguh-pet | Red Guhrio Cap with Mustache / Green Luiguh Cap | |
| Kroontje van Prinses Perzikguh / Gouden vadspet | Princess Peachguh's Little Crown / Golden Chonk Cap | |
| Loodguhter | Plumbguh | loodgieter pun |
| Vlaggenmastenverzamelaar / Super vahoege vadsverzamelaar | Flagpole Collector / Super Wahoog Chonk Collector | |
| Njeg! Mama! / Binnendoor / Kelder-vads / Krak... njeg! | Nyeg! Mama! / Shortcut / Cellar Chonk / Crack... nyeg! | |
| Guhshi kruipt uit zijn ei / Een stukje taart / Taart voor iedereen | Guhshi Hatches from His Egg / A Piece of Cake / Cake for Everyone | |

**Famous lines (Super Guhrio).**

| Dutch | English | note |
|---|---|---|
| Bedankt! Maar de prinses is in een ander kasteeldeel, njeg | Thank you! But the princess is in another part of the castle, nyeg | "...in another castle"; title *In een ander kasteeldeel* = *In Another Part of the Castle* |
| Het is-a mij, Guhrio! | It's-a Me, Guhrio! | |
| Hij had gewoon trek in taart | He Just Wanted Some Cake | |
| Geheim gevonden, njeg! | Secret found, nyeg! | |
| Een held! Njeg! / Guhshi is van jou! | A Hero! Nyeg! / Guhshi Is Yours! | |

### Added during the review of c40 - c62 (cross-chunk rules and names)

**Floors.** Dutch *begane grond* = *ground floor*; *eerste / tweede / derde verdieping* = *one floor up / two floors up /
three floors up* (never "first / second / third floor": American and British counting differ by one, and the player has to
find the right stairs). *een trap omhoog* = *one staircase up*.

**A quoted button is the button.** Where a text quotes a screen label, use the label's own words: the Filter Piece's
*alleen deze / alles behalve / laat liggen* = *only these / all except / leave behind* (screen: *Only these*, *All except*,
*Leave behind:*), a Stop Post's *Ophalen / Afleveren* = *Pick Up / Drop Off*.

**The quest book ticks.** *afvinken* = *to tick off* (as in the older chapters), and the closing lines of a "How Do You
Get Here?" quest are the standard ones: *Nothing is locked: every quest ticks itself off as soon as you've done it, even
if you did it earlier. The headings show what logically comes after what. Tick this off and get chonky to work!*

**Steps of a story** (`gui.guhs.verhalen.<story>.stap.N`) are sentences: only the first word and names get a capital
(*The lookout ridge*, *Up the rope*, but *The Ash Field*, *Shadow Lane*). Quest and advancement TITLES stay Title Case.

**Dutch that stays in en_us on purpose.** Guhdalf's *YOU.. SHALL.. NOT.. VADS!*; the word **VRIJ** on the campsite board of
the Grill Campground (it is painted into the texture, green, and *BEZET* in red: the English says *a green VRIJ (vacant)
sign*, never "a VACANT sign"); the dev command `/guhs verhaal demo`; the names *Smikkel*, *Guhdalf*, *Smikagol*...

| Dutch | English | note |
|---|---|---|
| baasje (Smikagol to the player) | master | *Volg ons, vadsje... eh, baasje* = *Follow us, chonkie... er, master*; a guh's note still says *my human* |
| Opa Njeg / Tante Vads / Oma Vahoeg / Neef Guh | Grandpa Nyeg / Auntie Chonk / Granny Wahoog / Cousin Guh | the runners of the Old Guh Wheel Power Plant (with *Uncle Nibble*) |
| Prof. dr. Guh | Prof. Dr. Guh | sign at the Fossil Dig |
| wachtkamer (Knabbelmoria) / westpoort / oostpoort / valhek | guard room / West Gate / East Gate / portcullis | the *Wachtkamer* of the toll bridge is a *Waiting Room* |
| steiger | dock (boats, Sauce Striders) / scaffold (under the toll bridge) / little jetty (the castle) | |
| gang (under the Black Grill Gate) | tunnel | *de gang* of Nibblemoria is *the corridor* |
| wei / box / gangpad / voerbak (Mika-stal) | paddock / pen / aisle / feeding trough | |
| receptie / houtschuur / kampeerplekjes | reception / woodshed / campsites | Grill Campground |
| afdakje | little roof | Fossil Dig, Ash Field |
| kijkbedden / brouwhoek | display beds / brewing corner | Pepper Garden |
| knabbelbak (vuurtoren) | Nibble Trough | |
| pollen Mikakruid | clumps of Mika Weed | |
| brok(ken) puin / hakken (puin, zout) | chunk(s) of rubble / to chip | *Hak maar raak!* = *Chip away!* |
| marktbewijs | market permit | what you haggle over |
| kaasvijver | cheese pond | Super Guhrio world 1 |
| ?-blok | ?-block | in the duel's texts; the block itself is the *Question Block* |
| Floep! (the lighthouse lamp) | Fwoop! | *floept aan* = *fwoops on*; the catapult's *Floep!* is still *Floop!* |
| Hmpf | Hmph | |
| %s uur (a duration) | %s h | never "%s hours" (1 hours) |

## 25. The Guh Path, the old stories' scenes, Sniff Island (verhalenpad)

Chunks `c63` - `c67`. Everything above still holds: njeg = **nyeg**, vads = **chonk**, vahoeg = **wahoog**, knabbel =
**nibble**, pootjes = **pawsies** (a sign says *paws*), American spelling (*harbor, gray, color, favorite, neighbor*).

**Sniff Island is our own story.** It was inspired by the premise of an old dog adventure game and by nothing more: every
name here is ours, and the English must be ours too. Use the names of the tables below and never a term you remember from a
dog game. In particular a *Snuffelmeester* is a **Sniff Maestro** (never "Sniff Master"), the *geneesbloem* is the
**healing flower** (never a "legendary flower") and the *maatje* is a **buddy**, a **forest sprite**.

**Tales and stories.** Titles, the tab and the chapters are *Tales* (*Tales of the Guhmension*, the *Tales* tab); in a
sentence a tale you follow is a *story*: *de grote verhalen* = *the big stories*, *Verhalen gevolgd: %s van %s* = *Stories
followed: %s of %s*, *volg deze verhalen* = *follow these stories* (as *My Story* and *You're following this story* in
section 24).

**Nobody gets hurt, nobody fails.** A dog that swims too far is *washed back ashore*, a dog behind the roadblock is *put
back*. *Zakken bestaat niet* = *There's no such thing as failing*.

**How they talk.**
- The dogs of the island say *njeg* like everybody else. *Woef!* = *Woof!*, *Kef!* = *Yip!*, *Snuf snuf* = *Sniff sniff*.
  A puppy's sad *Piep...* = *Whimper...* (an excited *Piep!* may be *Yip!*; never *Squeak*: that is the mouse of Squeak!).
- **Kapitein Zoutsnoet** calls the player *landrot* = **landlubber**; *Ahoi* = *Ahoy*.
- **Oma Wolletje** calls everybody *lieverd* = **dearie**. The residents call the player *pup* = **pup**.
- **Meester Truffelneus** is a schoolmaster: short, strict, kind. *Zoek!* = *Seek!* His motto *Neus omlaag, staart omhoog*
  = *Nose down, tail up*.
- *papa* = **Papa** (a name: *I'm going after Papa!*), *je vader* = *your father*, *je broertje of zusje* = *your little
  brother or sister* (the game does not know which).

**The four colors of the scent meter are fixed** (the lessons, the exam, the hints and the quest book all use them):
*oranje: iets lekkers* = **orange: something tasty**, *blauw: een ding* = **blue: a thing**, *groen: een dier* = **green:
an animal**, *paars: iets vreemds* = **purple: something strange**. The four kinds stand after *Je ruikt %s* = *You
smell %s*, so they are lowercase and carry their own article.

**The rank line** is *%s (rang %s (laagste) van %s (hoogste))* = *%s (rank %s (lowest) of %s (highest))*, on a sign *(rang 2
van 5)* = *(rank 2 of 5)*: a player must always see which rank they have and how many there are.

**Steps of the story** (`gui.guhs.verhalen.snuffeleiland.stap.N`) are sentences (section 24): *Washed ashore*, *Good
deeds*, *The sniffing exam*. Scene, quest and advancement titles with the same Dutch are Title Case: *Washed Ashore*.

**Signs.** A sign line is its own key and shows 90 pixels (about 15 letters). One key can stand on several signs
(*Snuffeldorp*, *Snuffelschool*, *Truffelneus* do): keep a name alone on its line so every sign still reads well.

### Het Guhpad (c63)

| Dutch | English | note |
|---|---|---|
| Het Guhpad / het Guhpad | The Guh Path / the Guh Path | the big stories open the worlds; FTB chapter group, Guhdex path map |
| padkaart / halte | path map / stop | the map at the top of the Tales tab |
| Verhalen van de Guhmensie | Tales of the Guhmension | FTB chapter (was *Guhverhalen* = *Guh Tales*) |
| Verhalen van de Guhbarbecuether | Tales of the Guhbarbecuether | FTB chapter (holds the Nibble Ring and Super Guhrio) |
| Verhalen van het Guheinde | Tales of the Guh End | FTB chapter (was *Het Guheinde*) |
| Het echte Guheinde / het echte Guheinde | The Real Guh End / the real Guh End | the locked last chapter; mysterious, no spoilers |
| Verhalen gevolgd | Stories Followed | statistic and quest title; the counter is *Stories followed: %s of %s* |
| groot verhaal / de grote verhalen | big story / the big stories | the seven that count for the Guh Path |
| op slot / open | locked / open | *Op slot voor iedereen* = *Locked for everyone* |
| Slotje: eerst de Guhmensie / Slotje: eerst de Guhbarbecuether | Lock: The Guhmension First / Lock: The Guhbarbecuether First | the lock quests |
| Het Guhpad begint hier | The Guh Path Starts Here | first quest of *Tales of the Guhmension* |
| Guh-technologie, laag 6 / Guh-technologie: laag 6 / laag 6 | Guh Technology, tier 6 / Guh Technology: Tier 6 / tier 6 | the placeholders of the real Guh End |
| het knabbelfeest van Guhdalf | Guhdalf's nibble party | chapter 1 of the Nibble Ring (*A Long-Expected Nibble Party*) |
| De Aangebrande Mika verslaan | Beat the Burnt Mika | |
| Baltoguh en Nomguh / Guhtwo en het kloon-eiland | Baltoguh and Nomguh / Guhtwo and Clone Island | the big stories by their path names |
| Het Hemelkapelletje / Ohana op Guhwai'i | The Cloud Chapel / Ohana on Guhwai'i | |
| guhportaal / grillportaal / het portaal in de Knabbelkelder | guh portal / grill portal / the portal in the Nibble Cellar | |

### The old stories' scenes (c64)

The six scenes play in stories that have had English since 1.2.0: every name in them is an older name (sections 6 - 16;
the chunk's source file lists the ones its lines use).

| Dutch | English | note |
|---|---|---|
| De witte wolf-guh | The White Wolf Guh | scene title and speaker (Baltoguh's story) |
| Dag 45: de nacht van de knal | Day 45: The Night of the Bang | Guhtwo's story; the professor's notes say *Day 45. BOOM!* |
| Ohana, bij het kampvuurtje | Ohana, by the Campfire | the ohana line is word for word the English of `gui.guhs.guhwaii.ohana.citaat` |
| Het Knuffelhart klopt | The Snuggleheart Beats | *Bonk... bonk.* = *Thump... thump.* |
| Hij brandt weer! | It's Burning Again! | the line: *WAHOOG, it's burning again!* (as the advancement of that name) |
| Het dak zit erop | The Roof Is On | |
| Boris (in je hoofd) | Boris (in your head) | speaker |
| Het eerste bewonertje | The first little resident | speaker |
| AUUUHOE-NJEG! | AWOOO-NYEG! | the howl |
| Wie ben ik... en waar zijn mijn knabbels? | Who am I... and where are my nibbles? | Guhtwo |
| Dan is het begonnen, njeg | Then it has begun, nyeg | the stranger with the pointy hat (the speaker is shown as ???) |

### Sniff Island: places, things and words (c65 - c67)

| Dutch | English | note |
|---|---|---|
| Het Snuffeleiland / het Snuffeleiland | Sniff Island / Sniff Island | no article in English: *on Sniff Island*, *to Sniff Island*; dimension, biome, story |
| HET SNUFFEL EILAND | SNIFF ISLAND | the title on the Guhstation's little screen |
| Snuffeldorp | Sniffville | the village |
| Steigerhuisje / steigerhuisje | Dock Cottage / dock cottage | the structure in the Guhmension; *steiger* = *dock* (section 24) |
| De Natte Neus | The Wet Nose | the captain's boat |
| Guhstation | Guhstation | stays; *kastje* = *little box*, *spelkastje* = *game console* |
| Geheugenkaart / geheugenkaart | Memory Card / memory card | the item that brings you home |
| Snuffelboekje / snuffelboekje | Sniff Book / sniff book | |
| snuffelen / opsnuffelen / snuffelaar | to sniff / to sniff out / sniffer | |
| snuffeltoets / geurmeter | sniff key / scent meter | the meter *swings* (*slaat uit*); *Slaat hij helemaal uit?* = *Swinging all the way?* |
| geur / Geleerde geuren | scent / Scents learned | |
| aanvalsknop | attack button | you dig with it: *graven* = *to dig*, *Graaf!* = *Dig!* |
| Snuffelpup | Sniff Pup | rank 1 (*rang* = *rank*); lowercase *snuffelpup* (a way to call you) = *sniff pup* |
| Snuffelneus | Sniff Nose | rank 2; the doctor's *een echte snuffelneus* = *a real sniff nose* |
| Snuffelspeurder | Sniff Sleuth | rank 3 (as *Nibble Sleuth*) |
| Snuffelmeester | Sniff Maestro | rank 4; never "Sniff Master" |
| Opper-Snuffelmeester | Grand Sniff Maestro | rank 5 |
| goede daad / goede daden | good deed / good deeds | |
| maatje / Maatje | buddy / Buddy | the companion only you can see |
| bosgeestje / Het bosgeestje | forest sprite / The forest sprite | what a buddy is |
| Zweefzaadje / Mos-eikeltje / Zonnepluisje | Driftseed / Moss Acorn / Sunfluff | the three buddies |
| boompje / Boompje van je maatje | little tree / Your Buddy's Little Tree | |
| kiem / scheutje / struikje / jong boompje | sprout / shoot / little bush / young tree | the tree's four steps; *een kaal plekje* = *a bare patch* |
| stenenkrans | ring of stones | around the little tree |
| Bloesemtakje van je boompje / bloesemtakje | Blossom Twig from Your Little Tree / blossom twig | |
| snuffelles / snuffelschool / Snuffelschool | sniffing lessons / sniff school / Sniff School | |
| snuffelexamen / snuffeldiploma | sniffing exam / sniffing diploma | |
| snuffelkoorts | sniffle fever | what Little Wobble has |
| geneesbloem | healing flower | never "legendary flower" |
| lantaarnfeest / het lantaarnfeest | Lantern Feast / the Lantern Feast | *lampion* = *paper lantern* |
| wegversperring | roadblock | friendly |
| Eilandhond / Snuffelhond / Steigerhond | Island Dog / Sniff Dog / Dock Dog | entity names |
| het strand / strandpoortje / weipoortje | the beach / beach gate / meadow gate | |
| de wei (Snuffeleiland) | the meadow | not the *paddock* of the Mika Stable |
| het plein / het pleintje / de put | the square / the little square / the well | |
| havenkantoor | Harbor Office | *de haven* = *the harbor* |
| dokterspraktijk / praktijk (dokter) | doctor's office / office | the white house with the red cross |
| het schooltje / juttershut / vissershut | the little school / beachcomber's hut / fisherman's hut | |
| moestuin / kippenhok / bijeneik | vegetable patch / chicken coop / bee oak | *moestuin* as in section 24 |
| kluifje / fluitje / deegroller | chew bone / whistle / rolling pin | scents |
| dobber | bobber | *Dobber dobbert, visser vist* = *Bobber bobs, fisher fishes* |
| schoolbel / bol wol / gietertje | school bell / ball of wool / little watering can | wool smells of sheep |
| stuiterbal / reservepet / De sjaal van papa | bouncy ball / spare cap / Papa's scarf | |
| Ras / Vacht | Breed / Coat | the choice screen |
| Shiba / Jack russell / Teckel | Shiba / Jack Russell / Dachshund | |
| Corgi / Golden retriever / Mopshond | Corgi / Golden Retriever / Pug | |
| Speurhond | Sleuth Hound | the trainer's breed |
| Zwart-tan / Chocola-tan / Driekleur | Black and Tan / Chocolate and Tan / Tricolor | coats; the others are plain color words (*Crème* = *Cream*) |

### Sniff Island: who is who

| Dutch | English | note |
|---|---|---|
| Dokter Pleisterpoot | Doctor Plasterpaw | on his sign *Dokter* + *Pleisterpoot* = *Doctor* + *Plasterpaw* |
| Meester Truffelneus | Master Trufflenose | the sniffing teacher; alone on a sign *Truffelneus* = *Trufflenose* |
| Kapitein Zoutsnoet | Captain Saltsnout | on a sign *Kapt. Zoutsnoet* = *Capt. Saltsnout* |
| Jutje Kwispel | Wendy Wagtail | *Strandjutter* = *Beachcomber* |
| Papa Zwerfpoot | Papa Wanderpaw | *die ouwe Zwerfpoot* = *old Wanderpaw* |
| Bakker Kruimelsnuit | Baker Crumbsnout | |
| Visser Natneus | Fisher Wetnose | |
| Juf Blaffetje | Miss Barkley | |
| Oma Wolletje | Granny Woolly | |
| Tuinder Knolletje | Gardener Turnip | *knollen* = *turnips* |
| Kleine Kwijlebal | Little Droolball | the village puppy |
| Kleine Wiebel / Wiebel | Little Wobble / Wobble | your little brother or sister, at the dock |
| Buurvrouw Mandje | Mrs. Basket | the neighbor at the dock (a dog's basket) |

### Sniff Island: lines and titles that stand in more than one chunk

| Dutch | English | note |
|---|---|---|
| Hier mag je pas door als Snuffelneus | You May Only Pass Here as a Sniff Nose | quest title; the line itself: *You may only pass here as a Sniff Nose* |
| Neus omlaag, staart omhoog | Nose Down, Tail Up | quest title; the motto: *Nose down, tail up* |
| Het eiland loopt niet weg | The island isn't going anywhere | the captain |
| Nee ik wil even niet snuffelen, njeg | No, I'd rather not sniff right now, nyeg | the Guhstation's second button |
| Druk op start | Press start | |
| Opslaan en naar huis / Verder spelen | Save and go home / Keep playing | the memory card's menu |
| Ik ga papa achterna! / Hijs de zeilen! | I'm going after Papa! / Hoist the sails! | answer buttons |
| Wordt vervolgd, njeg! | To be continued, nyeg! | |
| Aangespoeld | Washed Ashore | scene and quest title; the step: *Washed ashore* |
| Wie doet dat toch? | Who Keeps Doing That? | scene and quest title |
| Een spoor van papa | A Trace of Papa | scene and quest title; the step: *A trace of Papa* |
| Welke hond ben jij? | Which Dog Are You? | quest title; the screen heading: *Which dog are you?* |
| Snuffelles / Goede daden / Het snuffelexamen | Sniffing Lessons / Good Deeds / The Sniffing Exam | quest titles; the steps: *Sniffing lessons*, *Good deeds*, *The sniffing exam* |
| Er rommelt iets | Something's rattling | step |
| Het lantaarnfeest / De overtocht | The Lantern Feast / The Crossing | scene titles |
| Naar het Snuffeleiland / Weer thuis / Naar huis | To Sniff Island / Home Again / Homeward | scene titles |
| Het boompje groeit | The Little Tree Grows | scene title |
