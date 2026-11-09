# POI_Z6 - coordinates of zone 6 "Het Zijdeland" (u-z6wereld slice)

**GENERATED** by `tools/vadsrijk/zone6/poi6.py` (via `python tools/vadsrijk/maak_kaart.py --zone 6`). Do not edit by hand. Java: `nl.juiced.guhs.feature.vadsrijk.zone6.wereld.Z6WereldPlekken` (`plek("id")`, `GEBIEDEN` / `gebied(x, z)`, `inZone6`, `HUIZEN`, `BEWONERS`, `RIJDIEREN`, `KAMPVUREN`, `UITKIJKEN`). Dev: `/guhs vadsrijk z6wereld plek <id>` teleports there.

**Use ids, never numbers** (`Z6WereldPlekken.plek(id)`): a regen moves the spots with the map.

**Layout.** Zone box x 3280..4180, z -1060..-460 (tiles tx 51..66, tz -18..-7, real game coordinates, the tiles have a coast margin: the land runs into the shared open sea at the north/south/east tile edges). West: the sea cliffs (De Roze Kliffen, coast x ~3292) and the sandstone plateau (y ~120) with the Rozenkloof canyon (the arrival city, floor y ~68-74, line E airport on the north rim) and the Palmenkloven (the river canyon). Middle: the Saffraanrivier through Saffraanpoort (street y 71). South: De Roze Duinen + De Zoutvlakte (the Zingende Zoutgrot below it, floor y 49). North-east: Het Ruinedal (mesas, the dungeon entrance). East: De Moederoase and De Sterrendovende Toren (raid exterior; the gate is sealed, `toren_raid_ingang` = in front of it).

**Notes for the other slices.** The five star beacons are DARK in the tiles: the star of each is centred on `<baken>_ster` (r ~3.6, light blocks replace the grey ones when you light it). `zoutgrot_kristalwand` = the centre of the 3x4 crystal wall (x-1..x+1, y-1..y+2) the cave puzzle opens. `kerker_poort` = in front of the sealed star gate (the instance teleport). All positions are **stand positions** (the air block the feet are in) in `guhs:vadsrijk`, except `*_ster` and `haven_6_5_luchtschip` / `haven_6_5_mast`.

## Areas (`Z6WereldPlekken.GEBIEDEN`, first match wins; biome `guhs:vadsrijk_<biome>`)

| id | x1, z1 .. x2, z2 | biome | name | lv | town |
|---|---|---|---|---|---|
| `z6_rozenkloof` | 3236, -800 .. 3500, -652 | zijdeland_rozenkloof | Rozenkloof | 60-60 | yes |
| `z6_saffraanpoort` | 3634, -950 .. 3890, -742 | zijdeland_saffraanpoort | Saffraanpoort | 62-62 | yes |
| `z6_toren` | 4100, -730 .. 4180, -646 | zijdeland_toren | De Sterrendovende Toren | 70-70 | no |
| `z6_moederoase` | 4000, -900 .. 4180, -600 | zijdeland_moederoase | De Moederoase | 68-70 | no |
| `z6_ruinedal` | 3850, -1060 .. 4180, -900 | zijdeland_ruinedal | Het Ruïnedal | 66-68 | no |
| `z6_zoutvlakte` | 3833, -740 .. 4060, -460 | zijdeland_zoutvlakte | De Zoutvlakte | 64-66 | no |
| `z6_palmenkloven` | 3236, -1060 .. 3850, -800 | zijdeland_palmenkloven | De Palmenkloven | 62-64 | no |
| `z6_roze_duinen` | 3236, -800 .. 3833, -460 | zijdeland_roze_duinen | De Roze Duinen | 60-62 | no |
| `z6_zijdeland` | 3264, -1152 .. 4287, -385 | zijdeland | Het Zijdeland | 60-70 | no |

## All spots

| id | x, y, z | area |
|---|---|---|
| `bazaar_atelier` | 3733, 71, -821 | z6_saffraanpoort |
| `bazaar_atelier_leermeester` | 3735, 71, -821 | z6_saffraanpoort |
| `bazaar_dak` | 3822, 86, -802 | z6_saffraanpoort |
| `bazaar_juwelier` | 3793, 71, -821 | z6_saffraanpoort |
| `bazaar_knoopje` | 3743, 71, -804 | z6_saffraanpoort |
| `bazaar_koopman_1` | 3723, 71, -821 | z6_saffraanpoort |
| `bazaar_koopman_2` | 3783, 71, -804 | z6_saffraanpoort |
| `bazaar_koopman_3` | 3803, 71, -821 | z6_saffraanpoort |
| `bazaar_kruiden` | 3753, 71, -821 | z6_saffraanpoort |
| `bazaar_kruising` | 3759, 71, -810 | z6_saffraanpoort |
| `bazaar_naaiatelier` | 3733, 71, -821 | z6_saffraanpoort |
| `bazaar_oost` | 3824, 71, -818 | z6_saffraanpoort |
| `bazaar_potten` | 3773, 71, -804 | z6_saffraanpoort |
| `bazaar_smidse` | 3713, 71, -804 | z6_saffraanpoort |
| `bazaar_smidse_leermeester` | 3715, 71, -804 | z6_saffraanpoort |
| `bazaar_west` | 3702, 71, -808 | z6_saffraanpoort |
| `bobbeltje_duin` | 3684, 75, -662 | z6_roze_duinen |
| `dak_1` | 3822, 86, -802 | z6_saffraanpoort |
| `dak_2` | 3855, 87, -808 | z6_saffraanpoort |
| `duinbaken` | 3655, 72, -552 | z6_roze_duinen |
| `duinbaken_puzzel` | 3673, 84, -560 | z6_roze_duinen |
| `duinbaken_ster` | 3655, 105, -560 | z6_roze_duinen |
| `duinbaken_voet` | 3655, 72, -546 | z6_roze_duinen |
| `duinen` | 3660, 70, -640 | z6_roze_duinen |
| `duinen_bobbeltje` | 3684, 75, -662 | z6_roze_duinen |
| `duinen_kam` | 3622, 75, -690 | z6_roze_duinen |
| `duinen_karavaanweg` | 3726, 70, -625 | z6_roze_duinen |
| `duinen_rustplek` | 3598, 83, -742 | z6_roze_duinen |
| `duinen_tent` | 3640, 78, -619 | z6_roze_duinen |
| `duinguh_rimpel` | 3646, 71, -540 | z6_roze_duinen |
| `duinput` | 3611, 71, -668 | z6_roze_duinen |
| `examen_1` | 3848, 102, -904 | z6_saffraanpoort |
| `examen_2` | 3766, 71, -841 | z6_saffraanpoort |
| `examen_3` | 3795, 99, -890 | z6_saffraanpoort |
| `geheim_knuffel_1` | 3431, 103, -676 | z6_rozenkloof |
| `geheim_knuffel_2` | 3611, 77, -662 | z6_roze_duinen |
| `geheim_knuffel_3` | 3480, 66, -986 | z6_palmenkloven |
| `geheim_knuffel_4` | 3778, 71, -879 | z6_saffraanpoort |
| `geheim_knuffel_5` | 3950, 69, -540 | z6_zoutvlakte |
| `geheim_knuffel_6` | 3823, 71, -912 | z6_saffraanpoort |
| `geheim_knuffel_7` | 3739, 74, -895 | z6_saffraanpoort |
| `geheim_knuffel_8` | 4090, 60, -800 | z6_moederoase |
| `geheim_schat_1` | 3475, 74, -716 | z6_rozenkloof |
| `geheim_schat_2` | 3797, 99, -890 | z6_saffraanpoort |
| `geheim_schat_3` | 4012, 69, -928 | z6_ruinedal |
| `haven_6_5_bel` | 3398, 142, -772 | z6_rozenkloof |
| `haven_6_5_kapitein` | 3397, 142, -742 | z6_rozenkloof |
| `haven_6_5_landing` | 3398, 142, -758 | z6_rozenkloof |
| `haven_6_5_luchtschip` | 3398, 142, -730 | z6_rozenkloof |
| `haven_6_5_mast` | 3401, 149, -741 | z6_rozenkloof |
| `haven_6_5_vertrekbord` | 3404, 142, -779 | z6_rozenkloof |
| `haven_z6_z5_vertrek` | 3398, 142, -740 | z6_rozenkloof |
| `kampvuur_duinbaken` | 3642, 69, -550 | z6_roze_duinen |
| `kampvuur_duinen` | 3616, 74, -665 | z6_roze_duinen |
| `kampvuur_duinput` | 3616, 74, -665 | z6_roze_duinen |
| `kampvuur_karavaankamp` | 3903, 69, -583 | z6_zoutvlakte |
| `kampvuur_moederoase` | 4057, 66, -830 | z6_moederoase |
| `kampvuur_oasebaken` | 4051, 67, -835 | z6_moederoase |
| `kampvuur_palmbaken` | 3465, 106, -896 | z6_palmenkloven |
| `kampvuur_palmenkloven` | 3497, 64, -964 | z6_palmenkloven |
| `kampvuur_rozenkloof` | 3390, 69, -710 | z6_rozenkloof |
| `kampvuur_rozenkloof_luchthaven` | 3403, 116, -776 | z6_rozenkloof |
| `kampvuur_rozenkloof_plein` | 3390, 69, -710 | z6_rozenkloof |
| `kampvuur_ruinebaken` | 4032, 70, -990 | z6_ruinedal |
| `kampvuur_ruinedal` | 3952, 71, -944 | z6_ruinedal |
| `kampvuur_saffraanpoort` | 3789, 71, -879 | z6_saffraanpoort |
| `kampvuur_saffraanpoort_karavanserai` | 3675, 71, -803 | z6_saffraanpoort |
| `kampvuur_saffraanpoort_paleis` | 3789, 71, -879 | z6_saffraanpoort |
| `kampvuur_zoutbaken` | 3917, 69, -602 | z6_zoutvlakte |
| `kampvuur_zoutvlakte` | 3903, 69, -583 | z6_zoutvlakte |
| `kanaalwijk` | 3729, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_brug` | 3708, 71, -769 | z6_saffraanpoort |
| `kanaalwijk_eend_1` | 3700, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_eend_10` | 3758, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_eend_2` | 3704, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_eend_3` | 3717, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_eend_4` | 3722, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_eend_5` | 3726, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_eend_6` | 3731, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_eend_7` | 3744, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_eend_8` | 3748, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_eend_9` | 3753, 71, -765 | z6_saffraanpoort |
| `kanaalwijk_pluk` | 3731, 71, -765 | z6_saffraanpoort |
| `karavaan_weg_1` | 3566, 118, -794 | z6_roze_duinen |
| `karavaan_weg_2` | 3596, 93, -806 | z6_palmenkloven |
| `karavaan_weg_3` | 3622, 70, -826 | z6_palmenkloven |
| `karavaan_weg_4` | 3640, 71, -846 | z6_saffraanpoort |
| `karavaan_weg_5` | 3700, 71, -530 | z6_roze_duinen |
| `karavaan_weg_6` | 3716, 70, -580 | z6_roze_duinen |
| `karavaan_weg_7` | 3738, 69, -672 | z6_roze_duinen |
| `karavaan_weg_8` | 3758, 71, -746 | z6_saffraanpoort |
| `karavaankamp` | 3905, 69, -581 | z6_zoutvlakte |
| `karavanserai` | 3668, 71, -795 | z6_saffraanpoort |
| `karavanserai_gildebord` | 3669, 71, -803 | z6_saffraanpoort |
| `karavanserai_hof` | 3684, 71, -794 | z6_saffraanpoort |
| `karavanserai_keuken` | 3679, 71, -808 | z6_saffraanpoort |
| `karavanserai_stal` | 3681, 71, -787 | z6_saffraanpoort |
| `kerker_hal` | 3985, 70, -959 | z6_ruinedal |
| `kerker_ingang` | 3985, 70, -950 | z6_ruinedal |
| `kerker_poort` | 3985, 64, -974 | z6_ruinedal |
| `koerier_spetter` | 3532, 64, -958 | z6_palmenkloven |
| `kwastje_1` | 3768, 71, -818 | z6_saffraanpoort |
| `kwastje_2` | 3728, 86, -823 | z6_saffraanpoort |
| `kwastje_3` | 3718, 71, -765 | z6_saffraanpoort |
| `kwastje_4` | 3750, 71, -880 | z6_saffraanpoort |
| `kwastje_5` | 3687, 77, -806 | z6_saffraanpoort |
| `luchthaven_rozenkloof` | 3398, 142, -740 | z6_rozenkloof |
| `mager_mientje` | 4149, 66, -761 | z6_moederoase |
| `moeder_bron` | 4069, 66, -809 | z6_moederoase |
| `moederoase` | 4050, 66, -781 | z6_moederoase |
| `moederpalm` | 4079, 66, -813 | z6_moederoase |
| `moerbei_1` | 3433, 67, -957 | z6_palmenkloven |
| `moerbei_2` | 3462, 67, -998 | z6_palmenkloven |
| `moerbei_3` | 3523, 68, -947 | z6_palmenkloven |
| `noria_1` | 3703, 71, -880 | z6_saffraanpoort |
| `noria_2` | 3745, 71, -833 | z6_saffraanpoort |
| `noria_3` | 3839, 71, -865 | z6_saffraanpoort |
| `noria_brug` | 3739, 71, -835 | z6_saffraanpoort |
| `oase_tent_1` | 4144, 66, -762 | z6_moederoase |
| `oase_tent_2` | 4148, 66, -762 | z6_moederoase |
| `oase_tent_3` | 4152, 66, -762 | z6_moederoase |
| `oase_tenten` | 4149, 66, -761 | z6_moederoase |
| `oasebaken` | 4062, 70, -834 | z6_moederoase |
| `oasebaken_puzzel` | 4080, 68, -842 | z6_moederoase |
| `oasebaken_ster` | 4062, 103, -842 | z6_moederoase |
| `oasebaken_voet` | 4062, 67, -828 | z6_moederoase |
| `oostroute` | 4170, 68, -730 | z6_toren |
| `oostroute_luchtspiegeling` | 4020, 69, -662 | z6_moederoase |
| `paleis_audientie` | 3777, 74, -902 | z6_saffraanpoort |
| `paleis_dak` | 3749, 96, -912 | z6_saffraanpoort |
| `paleis_glans` | 3747, 74, -909 | z6_saffraanpoort |
| `paleis_grootvizier` | 3778, 74, -909 | z6_saffraanpoort |
| `paleis_iwan` | 3765, 73, -888 | z6_saffraanpoort |
| `paleis_minaret` | 3795, 99, -890 | z6_saffraanpoort |
| `paleis_poort` | 3762, 73, -891 | z6_saffraanpoort |
| `paleis_spiegelzaal` | 3752, 74, -901 | z6_saffraanpoort |
| `paleis_troon` | 3782, 76, -905 | z6_saffraanpoort |
| `paleis_tuin` | 3768, 71, -868 | z6_saffraanpoort |
| `palmbaken` | 3478, 108, -898 | z6_palmenkloven |
| `palmbaken_puzzel` | 3496, 106, -906 | z6_palmenkloven |
| `palmbaken_spiegel_1` | 3470, 106, -895 | z6_palmenkloven |
| `palmbaken_spiegel_2` | 3488, 106, -895 | z6_palmenkloven |
| `palmbaken_spiegel_3` | 3478, 107, -917 | z6_palmenkloven |
| `palmbaken_ster` | 3478, 141, -906 | z6_palmenkloven |
| `palmbaken_voet` | 3478, 106, -892 | z6_palmenkloven |
| `palmenkloven` | 3460, 67, -960 | z6_palmenkloven |
| `palmenkloven_moerbei` | 3433, 67, -957 | z6_palmenkloven |
| `palmenkloven_monding` | 3290, 67, -984 | z6_palmenkloven |
| `palmenkloven_palmenbos` | 3443, 67, -965 | z6_palmenkloven |
| `palmenkloven_waterval_onder` | 3538, 64, -957 | z6_palmenkloven |
| `palmguh_dadelpit` | 3486, 106, -896 | z6_palmenkloven |
| `professor_stof` | 3970, 70, -942 | z6_ruinedal |
| `raid_toren_binnen` | 4140, 16, -688 | z6_toren |
| `raid_toren_buiten` | 4119, 85, -688 | z6_toren |
| `raid_toren_poort` | 4123, 85, -688 | z6_toren |
| `rozenkloof_aankomst` | 3394, 142, -779 | z6_rozenkloof |
| `rozenkloof_bank` | 3414, 70, -684 | z6_rozenkloof |
| `rozenkloof_bobbel` | 3414, 69, -717 | z6_rozenkloof |
| `rozenkloof_brug_1` | 3365, 100, -719 | z6_rozenkloof |
| `rozenkloof_brug_2` | 3426, 103, -709 | z6_rozenkloof |
| `rozenkloof_dadels` | 3412, 69, -720 | z6_rozenkloof |
| `rozenkloof_fontein` | 3406, 69, -707 | z6_rozenkloof |
| `rozenkloof_havenmeester` | 3391, 142, -784 | z6_rozenkloof |
| `rozenkloof_herberg` | 3366, 68, -697 | z6_rozenkloof |
| `rozenkloof_herberg_balie` | 3364, 68, -693 | z6_rozenkloof |
| `rozenkloof_karavaan` | 3412, 69, -718 | z6_rozenkloof |
| `rozenkloof_kloofmoeder` | 3513, 76, -697 | z6_roze_duinen |
| `rozenkloof_luchthaven` | 3398, 142, -740 | z6_rozenkloof |
| `rozenkloof_oma_dadel` | 3412, 69, -718 | z6_rozenkloof |
| `rozenkloof_plein` | 3392, 69, -714 | z6_rozenkloof |
| `rozenkloof_post` | 3414, 70, -737 | z6_rozenkloof |
| `rozenkloof_post_1` | 3350, 67, -705 | z6_rozenkloof |
| `rozenkloof_post_2` | 3366, 84, -743 | z6_rozenkloof |
| `rozenkloof_post_3` | 3379, 84, -692 | z6_rozenkloof |
| `rozenkloof_post_4` | 3333, 98, -757 | z6_rozenkloof |
| `rozenkloof_post_5` | 3426, 103, -740 | z6_rozenkloof |
| `rozenkloof_post_6` | 3366, 116, -752 | z6_rozenkloof |
| `rozenkloof_postguh` | 3414, 70, -737 | z6_rozenkloof |
| `rozenkloof_recruitbord` | 3426, 71, -729 | z6_rozenkloof |
| `rozenkloof_richel_1` | 3383, 84, -737 | z6_rozenkloof |
| `rozenkloof_richel_2` | 3383, 100, -744 | z6_rozenkloof |
| `rozenkloof_richel_3` | 3384, 115, -752 | z6_rozenkloof |
| `rozenkloof_schatkamer` | 3474, 74, -699 | z6_rozenkloof |
| `rozenkloof_schatkamer_hal` | 3494, 76, -697 | z6_rozenkloof |
| `rozenkloof_siq_boven` | 3566, 118, -794 | z6_roze_duinen |
| `rozenkloof_siq_onder` | 3448, 72, -713 | z6_rozenkloof |
| `rozenkloof_siq_top` | 3566, 118, -794 | z6_roze_duinen |
| `rozenkloof_stal` | 3442, 72, -728 | z6_rozenkloof |
| `rozenkloof_stalmeester` | 3439, 72, -724 | z6_rozenkloof |
| `rozenkloof_tapijten` | 3376, 68, -702 | z6_rozenkloof |
| `rozenkloof_tapijtwinkel` | 3379, 68, -700 | z6_rozenkloof |
| `rozenkloof_tuin_1` | 3361, 84, -740 | z6_rozenkloof |
| `rozenkloof_tuin_2` | 3414, 102, -739 | z6_rozenkloof |
| `rozenkloof_tuin_2_terras` | 3350, 117, -759 | z6_rozenkloof |
| `rozenkloof_tuin_3` | 3414, 112, -674 | z6_rozenkloof |
| `rozenkloof_tuin_richel1` | 3361, 84, -740 | z6_rozenkloof |
| `rozenkloof_tuinguh` | 3382, 69, -712 | z6_rozenkloof |
| `rozenkloof_zeemond` | 3272, 65, -745 | z6_rozenkloof |
| `ruinebaken` | 4045, 72, -992 | z6_ruinedal |
| `ruinebaken_puzzel` | 4063, 69, -1000 | z6_ruinedal |
| `ruinebaken_ster` | 4045, 105, -1000 | z6_ruinedal |
| `ruinebaken_voet` | 4045, 70, -986 | z6_ruinedal |
| `ruinedal` | 3985, 64, -975 | z6_ruinedal |
| `ruinedal_kamp` | 3960, 71, -940 | z6_ruinedal |
| `ruinedal_tafelmesa` | 4010, 69, -926 | z6_ruinedal |
| `saffraanbrug` | 3764, 71, -851 | z6_saffraanpoort |
| `saffraanbrug_paviljoen` | 3766, 71, -841 | z6_saffraanpoort |
| `saffraanpoort_bank` | 3798, 71, -787 | z6_saffraanpoort |
| `saffraanpoort_bazaar` | 3759, 71, -810 | z6_saffraanpoort |
| `saffraanpoort_herberg` | 3724, 71, -778 | z6_saffraanpoort |
| `saffraanpoort_herberg_balie` | 3728, 71, -778 | z6_saffraanpoort |
| `saffraanpoort_karavanserai` | 3668, 71, -795 | z6_saffraanpoort |
| `saffraanpoort_knars` | 3697, 71, -884 | z6_saffraanpoort |
| `saffraanpoort_oostpoort` | 3881, 71, -884 | z6_saffraanpoort |
| `saffraanpoort_paleis` | 3762, 73, -891 | z6_saffraanpoort |
| `saffraanpoort_paleisplein` | 3756, 71, -863 | z6_saffraanpoort |
| `saffraanpoort_paradijstuin` | 3768, 71, -868 | z6_saffraanpoort |
| `saffraanpoort_plein` | 3756, 71, -863 | z6_saffraanpoort |
| `saffraanpoort_poort_oost` | 3881, 71, -884 | z6_saffraanpoort |
| `saffraanpoort_poort_west` | 3644, 71, -806 | z6_saffraanpoort |
| `saffraanpoort_recruitbord` | 3654, 71, -821 | z6_saffraanpoort |
| `saffraanpoort_saffraanbrug` | 3764, 71, -851 | z6_saffraanpoort |
| `saffraanpoort_spiegelzaal` | 3752, 74, -901 | z6_saffraanpoort |
| `saffraanpoort_sterrenwacht` | 3846, 71, -901 | z6_saffraanpoort |
| `saffraanpoort_sterrenwacht_kijker` | 3848, 102, -904 | z6_saffraanpoort |
| `saffraanpoort_westpoort` | 3644, 71, -806 | z6_saffraanpoort |
| `saffraanpoort_zuidpoort` | 3762, 71, -742 | z6_saffraanpoort |
| `schatkamer_koepeltje` | 3481, 98, -695 | z6_rozenkloof |
| `spiegelzaal_troon` | 3782, 76, -905 | z6_saffraanpoort |
| `sterrenwacht` | 3846, 71, -901 | z6_saffraanpoort |
| `sterrenwacht_brouwerij` | 3831, 71, -907 | z6_saffraanpoort |
| `sterrenwacht_top` | 3848, 102, -904 | z6_saffraanpoort |
| `sterrenwacht_twinkel` | 3846, 71, -906 | z6_saffraanpoort |
| `tablet_1` | 3880, 69, -929 | z6_saffraanpoort |
| `tablet_2` | 3920, 71, -954 | z6_ruinedal |
| `tablet_3` | 3960, 70, -984 | z6_ruinedal |
| `tablet_4` | 4010, 70, -974 | z6_ruinedal |
| `tablet_5` | 4070, 69, -999 | z6_ruinedal |
| `tablet_6` | 4110, 69, -1029 | z6_ruinedal |
| `toren_poort` | 4119, 85, -688 | z6_toren |
| `toren_raid_ingang` | 4123, 85, -688 | z6_toren |
| `toren_top` | 4140, 240, -688 | z6_toren |
| `toren_voet` | 4112, 79, -688 | z6_toren |
| `uitkijk_duinkam` | 3596, 74, -540 | z6_roze_duinen |
| `uitkijk_mesa` | 3920, 71, -1010 | z6_ruinedal |
| `uitkijk_oase` | 4040, 67, -720 | z6_moederoase |
| `uitkijk_palmwaterval` | 3566, 66, -926 | z6_palmenkloven |
| `uitkijk_zeemond` | 3314, 118, -762 | z6_rozenkloof |
| `uitkijk_zoutrots` | 4030, 69, -640 | z6_moederoase |
| `verzonken_sterrenwacht` | 3985, 70, -950 | z6_ruinedal |
| `waterval_1` | 3408, 68, -972 | z6_palmenkloven |
| `waterval_2` | 3470, 64, -967 | z6_palmenkloven |
| `waterval_3` | 3520, 64, -955 | z6_palmenkloven |
| `waterval_4` | 3578, 64, -929 | z6_palmenkloven |
| `waterval_5` | 3627, 67, -897 | z6_palmenkloven |
| `watervallen_top` | 3528, 94, -920 | z6_palmenkloven |
| `watervallen_voet` | 3538, 64, -957 | z6_palmenkloven |
| `wormelien` | 3700, 71, -600 | z6_roze_duinen |
| `wormelien_arena` | 3700, 71, -600 | z6_roze_duinen |
| `z6_burger_1` | 3396, 69, -708 | z6_rozenkloof |
| `z6_burger_2` | 3371, 84, -744 | z6_rozenkloof |
| `z6_burger_3` | 3746, 71, -818 | z6_saffraanpoort |
| `z6_burger_4` | 3780, 69, -858 | z6_saffraanpoort |
| `z6_burger_5` | 3768, 74, -908 | z6_saffraanpoort |
| `z6_burger_6` | 3658, 71, -834 | z6_saffraanpoort |
| `z6_wacht_1` | 3404, 70, -710 | z6_rozenkloof |
| `z6_wacht_2` | 3400, 142, -778 | z6_rozenkloof |
| `z6_wacht_3` | 3641, 71, -840 | z6_saffraanpoort |
| `z6_wacht_4` | 3768, 71, -842 | z6_saffraanpoort |
| `z6_wacht_5` | 3924, 69, -646 | z6_zoutvlakte |
| `z6_wacht_6` | 4051, 66, -767 | z6_moederoase |
| `z6w_leider_brie` | 4123, 66, -804 | z6_moederoase |
| `z6w_leider_korstje` | 3960, 69, -560 | z6_zoutvlakte |
| `z6w_leider_oorschelp` | 3620, 79, -560 | z6_roze_duinen |
| `z6w_leider_oudstewachter` | 4034, 70, -1000 | z6_ruinedal |
| `z6w_leider_pitje` | 3519, 64, -961 | z6_palmenkloven |
| `z6w_mobs_duinen_1` | 3640, 73, -700 | z6_roze_duinen |
| `z6w_mobs_duinen_2` | 3690, 74, -520 | z6_roze_duinen |
| `z6w_mobs_duinen_3` | 3760, 72, -560 | z6_roze_duinen |
| `z6w_mobs_duinen_4` | 3600, 80, -600 | z6_roze_duinen |
| `z6w_mobs_moederoase_1` | 4040, 67, -760 | z6_moederoase |
| `z6w_mobs_moederoase_2` | 4110, 66, -820 | z6_moederoase |
| `z6w_mobs_moederoase_3` | 4060, 67, -720 | z6_moederoase |
| `z6w_mobs_palmenkloven_1` | 3470, 67, -988 | z6_palmenkloven |
| `z6w_mobs_palmenkloven_2` | 3557, 64, -951 | z6_palmenkloven |
| `z6w_mobs_palmenkloven_3` | 3424, 67, -998 | z6_palmenkloven |
| `z6w_mobs_ruinedal_1` | 3920, 71, -960 | z6_ruinedal |
| `z6w_mobs_ruinedal_2` | 4020, 70, -1010 | z6_ruinedal |
| `z6w_mobs_ruinedal_3` | 4090, 69, -1020 | z6_ruinedal |
| `z6w_mobs_zoutvlakte_1` | 3880, 69, -640 | z6_zoutvlakte |
| `z6w_mobs_zoutvlakte_2` | 3990, 69, -610 | z6_zoutvlakte |
| `z6w_mobs_zoutvlakte_3` | 3940, 69, -520 | z6_zoutvlakte |
| `z6w_sterrenwacht_ingang` | 3985, 70, -950 | z6_ruinedal |
| `z6w_test` | 3762, 71, -846 | z6_saffraanpoort |
| `z6w_veilig_karavaankamp` | 3905, 69, -581 | z6_zoutvlakte |
| `z6w_veilig_rozenkloof` | 3400, 70, -714 | z6_rozenkloof |
| `z6w_veilig_saffraanpoort` | 3762, 71, -846 | z6_saffraanpoort |
| `z6w_veilig_toren` | 4100, 68, -690 | z6_toren |
| `z6w_wormelien` | 3640, 81, -610 | z6_roze_duinen |
| `z6w_wormelien_bord` | 3637, 71, -580 | z6_roze_duinen |
| `z6w_wormelien_deur` | 3640, 71, -582 | z6_roze_duinen |
| `z6w_zout_nacht` | 3860, 69, -620 | z6_zoutvlakte |
| `z6w_zoutgrot_elite` | 3917, 50, -542 | z6_zoutvlakte |
| `z6w_zoutkolos` | 3995, 69, -560 | z6_zoutvlakte |
| `z6w_zoutkolos_bord` | 3992, 69, -588 | z6_zoutvlakte |
| `z6w_zoutkolos_deur` | 3995, 69, -586 | z6_zoutvlakte |
| `zoutbaken` | 3930, 71, -604 | z6_zoutvlakte |
| `zoutbaken_puzzel` | 3948, 69, -612 | z6_zoutvlakte |
| `zoutbaken_ster` | 3930, 104, -612 | z6_zoutvlakte |
| `zoutbaken_voet` | 3930, 69, -598 | z6_zoutvlakte |
| `zoutgrot` | 3874, 50, -534 | z6_zoutvlakte |
| `zoutgrot_elite` | 3917, 50, -542 | z6_zoutvlakte |
| `zoutgrot_ingang` | 3874, 69, -499 | z6_zoutvlakte |
| `zoutgrot_ingang_buiten` | 3874, 69, -499 | z6_zoutvlakte |
| `zoutgrot_kristal_1` | 3844, 50, -544 | z6_zoutvlakte |
| `zoutgrot_kristal_2` | 3846, 50, -544 | z6_zoutvlakte |
| `zoutgrot_kristal_3` | 3848, 50, -544 | z6_zoutvlakte |
| `zoutgrot_kristal_4` | 3850, 50, -544 | z6_zoutvlakte |
| `zoutgrot_kristal_5` | 3852, 50, -544 | z6_zoutvlakte |
| `zoutgrot_kristallen` | 3848, 50, -541 | z6_zoutvlakte |
| `zoutgrot_kristalwand` | 3848, 51, -537 | z6_zoutvlakte |
| `zoutgrot_meer` | 3881, 50, -541 | z6_zoutvlakte |
| `zoutgrot_mijn` | 3860, 50, -536 | z6_zoutvlakte |
| `zoutgrot_puzzel` | 3848, 50, -541 | z6_zoutvlakte |
| `zoutgrot_schat` | 3848, 50, -528 | z6_zoutvlakte |
| `zoutgrot_vis` | 3878, 50, -542 | z6_zoutvlakte |
| `zoutkolos_arena` | 3995, 69, -560 | z6_zoutvlakte |
| `zoutvlakte` | 3945, 69, -598 | z6_zoutvlakte |
| `zoutvlakte_kamp` | 3905, 69, -581 | z6_zoutvlakte |
| `zoutvlakte_mikakamp` | 3972, 69, -588 | z6_zoutvlakte |
| `zoutvlakte_ziedershuisje` | 3876, 69, -542 | z6_zoutvlakte |
| `zoutvlakte_ziedershuisje_huis` | 3874, 69, -548 | z6_zoutvlakte |
| `zoutzieder_korrel` | 3876, 69, -542 | z6_zoutvlakte |
| `zoutziedershuisje` | 3874, 69, -548 | z6_zoutvlakte |

## Houses with a role (`HUIZEN`)

| id | role | door | inside |
|---|---|---|---|
| `rozenkloof_schrijn_1` | schrijn | 3333, 82, -754 | 3333, 82, -761 |
| `rozenkloof_woning_1` | woning | 3333, 98, -760 | 3333, 98, -763 |
| `rozenkloof_woning_2` | woning | 3350, 67, -702 | 3350, 67, -699 |
| `rozenkloof_woning_3` | woning | 3366, 84, -746 | 3366, 84, -749 |
| `rozenkloof_woning_4` | woning | 3366, 100, -753 | 3366, 100, -756 |
| `rozenkloof_herberg` | herberg | 3366, 68, -699 | 3366, 68, -696 |
| `rozenkloof_winkel_specerijen` | winkel_specerijen | 3366, 84, -693 | 3366, 84, -691 |
| `rozenkloof_winkel_tapijten` | winkel_tapijten | 3379, 68, -697 | 3379, 68, -695 |
| `rozenkloof_woning_5` | woning | 3379, 84, -689 | 3379, 84, -686 |
| `rozenkloof_woning_6` | woning | 3397, 69, -733 | 3397, 69, -736 |
| `rozenkloof_schrijn_2` | schrijn | 3397, 85, -685 | 3397, 85, -678 |
| `rozenkloof_postkantoor` | postkantoor | 3414, 70, -732 | 3414, 70, -737 |
| `rozenkloof_winkel_dadels` | winkel_dadels | 3414, 86, -737 | 3414, 86, -739 |
| `rozenkloof_bank` | bank | 3414, 70, -690 | 3414, 70, -684 |
| `rozenkloof_woning_7` | woning | 3426, 87, -736 | 3426, 87, -739 |
| `rozenkloof_woning_8` | woning | 3426, 103, -743 | 3426, 103, -746 |
| `rozenkloof_winkel_lampen` | winkel_lampen | 3426, 71, -690 | 3426, 71, -688 |
| `rozenkloof_woning_9` | woning | 3426, 87, -682 | 3426, 87, -679 |
| `rozenkloof_stal` | stal | 3442, 72, -722 | 3442, 72, -728 |
| `rozenkloof_woning_10` | woning | 3442, 104, -738 | 3442, 104, -741 |
| `rozenkloof_schrijn_3` | schrijn | 3442, 88, -678 | 3442, 88, -671 |
| `rozenkloof_schrijn_4` | schrijn | 3459, 73, -682 | 3459, 73, -675 |
| `rozenkloof_woning_11` | woning | 3472, 90, -675 | 3472, 90, -672 |
| `rozenkloof_schatkamer` | hal | 3487, 76, -699 | 3494, 76, -697 |
| `rozenkloof_paviljoen_post` | woning | 3366, 116, -753 | 3366, 116, -756 |
| `rozenkloof_paviljoen` | woning | 3459, 115, -669 | 3459, 115, -666 |
| `huis_sterrenwacht_brouwerij` | brouwerij | 3830, 71, -903 | 3829, 71, -906 |
| `huis_woning_saffraan_1` | woning | 3698, 71, -909 | 3697, 71, -913 |
| `huis_woning_saffraan_3` | woning | 3717, 71, -774 | 3716, 71, -778 |
| `huis_herberg_saffraan` | herberg | 3726, 71, -774 | 3725, 71, -778 |
| `huis_winkel_dadels` | winkel_dadels | 3745, 71, -763 | 3744, 71, -760 |
| `huis_bank_saffraan` | bank | 3799, 71, -790 | 3798, 71, -787 |
| `huis_winkel_kruiden` | winkel_kruiden | 3798, 71, -763 | 3797, 71, -760 |
| `huis_woning_saffraan_2` | woning | 3825, 71, -925 | 3824, 71, -922 |
| `huis_winkel_zijde` | winkel_zijde | 3834, 71, -882 | 3833, 71, -886 |
| `huis_winkel_lampen` | winkel_lampen | 3826, 71, -790 | 3825, 71, -787 |
| `huis_winkel_potten` | winkel_potten | 3853, 71, -801 | 3852, 71, -805 |
| `zoutziedershuisje` | woning | 3874, 69, -545 | 3874, 69, -548 |

Townsfolk figures: 125; mount figures: 69 (bultguh 36, tapijt 20, pluimguh 13).
