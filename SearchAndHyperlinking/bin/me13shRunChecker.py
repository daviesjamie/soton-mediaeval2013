#!/usr/bin/env python
"""
Script to test the validity of a run submission to the Media Eval Search and Hyperlinking Task 2013.
Usage:
python ./me13shRunChecker.py <F> ...
where <F> is either a path to a file or a directory that consists only of run files

me13sh_UT_S_Sh_U_N_LuceneSearch: for a search run using (S) by UT with shot segmentation (Sh) based on LIUM (U) transcripts and no additional features (N) where the system identifier is LuceneSearch.

me13sh_DCU_LA_Sh_U_N_Lucene: for a linking run using only the anchor segment (LA) by DCU with shot segmentation (Sh) based on LIUM (U) transcripts and no additional features (N) where the system identifier is Lucene.

me13sh_DCU_LA_Sh_N_V_Visor: for a linking run using only the anchor segment (LA) by DCU with shot segmentation (Sh) based on no transcript (N) but on visual features (V) where the system identifier is Visor.

me13sh_DCU_LC_Sh_I_MA_Complex: for a linking run using both the anchor and its context (LC) by DCU with shot segmentation (Sh) based on LIMSI transcripts (I) and on visual features (V) as well as metadata (M) where the system identifier is Complex.
"""
import sys, re, os

anchors = [ 'anchor_%d' % i for i in range(1, 99) ]
items = [ 'item_%d' % i for i in range(1, 51) ]
runTypes = [
  'S',   # search run
  'LA',  # linking run using only the anchor segment
  'LC',  # linking run using both the anchor segment and its context
]
segmentations = [
  'Ss', # speech sentence
  'Sp', # speech segment
  'Sh', # shot
  'F',  # fixed length
  'L',  # lexical cohesian
  'O',  # other segmentation
]
asrFeatures = [
  'I', # LIMSI
  'U', # Lium
  'S', # Manual subtitles
  'N'  # No ASR information
]
additionalFeatures = [
  'M', # metadata
  'V', # visual
  'O', # other
  'N'  # No additional features
]

videoFiles = [
  '20080401_002000_bbcthree_pulling', '20080401_003500_bbcone_springwatch_weatherview', '20080401_004000_bbcone_miracle_on_the_estate',
  '20080401_010000_bbcfour_the_book_quiz', '20080401_013000_bbcfour_legends_marty_feldman_six_degrees_of', '20080401_014000_bbcone_the_shogun',
  '20080401_015000_bbcthree_dog_borstal', '20080401_023000_bbcfour_the_book_quiz', '20080401_024000_bbcone_to_buy_or_not_to_buy',
  '20080401_030000_bbctwo_key_stage_3_bitesize_english_1', '20080401_050000_bbctwo_tikkabilla', '20080401_063000_bbctwo_mama_mirabelle_s_home_movies',
  '20080401_064500_bbctwo_tommy_zoom', '20080401_070000_bbctwo_arthur', '20080401_074500_bbctwo_chucklevision', '20080401_080000_bbctwo_tracy_beaker',
  '20080401_090000_bbctwo_stupid', '20080401_100000_bbcone_to_buy_or_not_to_buy', '20080401_110000_bbctwo_the_daily_politics', '20080401_111500_bbcone_bargain_hunt',
  '20080401_113000_bbctwo_working_lunch', '20080401_120000_bbcone_bbc_news', '20080401_124500_bbcone_doctors', '20080401_140500_bbcone_space_pirates',
  '20080401_143500_bbcone_raven', '20080401_144500_bbctwo_flog_it', '20080401_160000_bbcone_newsround', '20080401_161500_bbcone_the_weakest_link',
  '20080401_170000_bbcone_bbc_news', '20080401_170000_bbctwo_eggheads', '20080401_173000_bbcone_bbc_london_news', '20080401_173000_bbctwo_great_british_menu',
  '20080401_180000_bbcfour_world_news_today', '20080401_180000_bbcone_the_one_show', '20080401_180000_bbcthree_dog_borstal',
  '20080401_180000_bbctwo_around_the_world_in_80_gardens', '20080401_183000_bbcfour_pop_goes_the_sixties', '20080401_190000_bbcfour_tv_s_believe_it_or_not',
  '20080401_190000_bbcone_holby_city', '20080401_190000_bbcthree_doctor_who', '20080401_190000_bbctwo_university_challenge_the_professionals',
  '20080401_193000_bbctwo_johnny_s_new_kingdom', '20080401_200000_bbcfour_tv_s_believe_it_or_not', '20080401_200000_bbcone_hotel_babylon',
  '20080401_210000_bbcfour_the_hard_sell', '20080401_210000_bbcone_bbc_ten_o_clock_news', '20080401_210000_bbcthree_eastenders',
  '20080401_210000_bbctwo_later_live_with_jools_holland', '20080401_213000_bbcfour_a_year_in_tibet', '20080401_213000_bbcthree_lily_allen_and_friends',
  '20080401_213000_bbctwo_newsnight', '20080401_213500_bbcone_one_life', '20080401_223000_bbcfour_the_book_quiz', '20080401_230000_bbcfour_proms_on_four',
  '20080401_233500_bbcthree_dog_borstal', '20080402_001000_bbcone_weatherview', '20080402_001500_bbcone_see_hear', '20080402_003000_bbcthree_lily_allen_and_friends',
  '20080402_004500_bbcone_panorama', '20080402_011500_bbcfour_tv_s_believe_it_or_not', '20080402_012000_bbcone_life_in_cold_blood',
  '20080402_021500_bbcfour_tv_s_believe_it_or_not', '20080402_022000_bbcone_to_buy_or_not_to_buy', '20080402_030000_bbctwo_key_stage_3_bitesize_maths_1',
  '20080402_050000_bbcone_breakfast', '20080402_050000_bbctwo_tikkabilla', '20080402_063000_bbctwo_mama_mirabelle_s_home_movies', '20080402_064500_bbctwo_tommy_zoom',
  '20080402_065500_bbctwo_take_a_bow', '20080402_070000_bbctwo_arthur', '20080402_074500_bbctwo_chucklevision', '20080402_080000_bbctwo_tracy_beaker',
  '20080402_090000_bbcone_homes_under_the_hammer', '20080402_090000_bbctwo_stupid', '20080402_093000_bbctwo_what_the_ancients_did_for_us',
  '20080402_100000_bbcone_to_buy_or_not_to_buy', '20080402_103000_bbctwo_the_daily_politics', '20080402_111500_bbcone_bargain_hunt', '20080402_120000_bbcone_bbc_news',
  '20080402_120000_bbctwo_see_hear', '20080402_123000_bbctwo_working_lunch', '20080402_124500_bbcone_doctors', '20080402_140500_bbcone_space_pirates',
  '20080402_144500_bbctwo_flog_it', '20080402_150500_bbcone_young_dracula', '20080402_153500_bbcone_blue_peter', '20080402_160000_bbcone_newsround',
  '20080402_161500_bbcone_the_weakest_link', '20080402_161500_bbctwo_escape_to_the_country', '20080402_170000_bbcone_bbc_news', '20080402_170000_bbctwo_eggheads',
  '20080402_173000_bbcone_bbc_london_news', '20080402_173000_bbctwo_great_british_menu', '20080402_180000_bbcfour_world_news_today', '20080402_180000_bbcone_the_one_show',
  '20080402_180000_bbcthree_dog_borstal', '20080402_180000_bbctwo_coast_the_journey_continues', '20080402_183000_bbcfour_pop_goes_the_sixties',
  '20080402_183000_bbcone_street_doctor', '20080402_190000_bbcfour_around_the_world_in_80_treasures', '20080402_190000_bbcone_traffic_cops',
  '20080402_190000_bbcthree_doctor_who', '20080402_190000_bbctwo_natural_world', '20080402_195000_bbctwo_wild', '20080402_200000_bbcfour_hughie_green_most_sincerely',
  '20080402_200000_bbcone_the_apprentice', '20080402_200000_bbctwo_dan_cruickshank_s_adventures', '20080402_210000_bbcone_bbc_ten_o_clock_news',
  '20080402_210000_bbctwo_the_apprentice_you_re_fired', '20080402_212000_bbcfour_up_pompeii', '20080402_213000_bbctwo_newsnight',
  '20080402_214000_bbcone_one_foot_in_the_grave', '20080402_215000_bbcfour_mark_lawson_talks_to_barry_cryer', '20080402_222000_bbctwo_desi_dna',
  '20080402_225000_bbcfour_hughie_green_most_sincerely', '20080403_000500_bbcone_weatherview', '20080403_001000_bbcfour_a_year_in_tibet',
  '20080403_001000_bbcone_antiques_roadshow', '20080403_005000_bbcthree_dog_borstal', '20080403_011000_bbcfour_play_it_again_the_panel_game',
  '20080403_011000_bbcone_the_choir', '20080403_014000_bbcfour_mark_lawson_talks_to_barry_cryer', '20080403_021000_bbcone_an_island_parish',
  '20080403_024000_bbcone_to_buy_or_not_to_buy', '20080403_030000_bbctwo_key_stage_3_bitesize_science_1', '20080403_050000_bbcone_breakfast',
  '20080403_050000_bbctwo_tikkabilla', '20080403_063000_bbctwo_mama_mirabelle_s_home_movies', '20080403_064500_bbctwo_tommy_zoom', '20080403_065500_bbctwo_take_a_bow',
  '20080403_070000_bbctwo_arthur', '20080403_074500_bbctwo_chucklevision', '20080403_080000_bbctwo_tracy_beaker', '20080403_090000_bbcone_homes_under_the_hammer',
  '20080403_090000_bbctwo_stupid', '20080403_100000_bbcone_to_buy_or_not_to_buy', '20080403_105500_bbctwo_coast', '20080403_110000_bbctwo_the_daily_politics',
  '20080403_111500_bbcone_bargain_hunt', '20080403_113000_bbctwo_working_lunch', '20080403_120000_bbcone_bbc_news', '20080403_124500_bbcone_doctors',
  '20080403_124500_bbctwo_racing_from_aintree', '20080403_140500_bbcone_space_pirates', '20080403_150500_bbcone_stake_out', '20080403_153500_bbcone_beat_the_boss',
  '20080403_154500_bbctwo_flog_it', '20080403_160000_bbcone_newsround', '20080403_161500_bbcone_the_weakest_link', '20080403_170000_bbcone_bbc_news',
  '20080403_170000_bbctwo_eggheads', '20080403_173000_bbcone_bbc_london_news', '20080403_180000_bbcfour_world_news_today', '20080403_180000_bbcone_the_one_show',
  '20080403_180000_bbctwo_torchwood', '20080403_183000_bbcfour_up_pompeii', '20080403_183000_bbcone_eastenders', '20080403_185000_bbctwo_torchwood_declassified',
  '20080403_190000_bbcone_holby_blue', '20080403_190000_bbcthree_doctor_who', '20080403_190000_bbctwo_10_things_you_didn_t_know_about',
  '20080403_200000_bbcfour_a_year_in_tibet', '20080403_200000_bbctwo_the_apprentice_the_worst_decisions_ever',
  '20080403_201500_bbcthree_doctor_who_confidential_kylie_special', '20080403_210000_bbcfour_ashes_to_ashes', '20080403_210000_bbcone_bbc_ten_o_clock_news',
  '20080403_210000_bbcthree_eastenders', '20080403_210000_bbctwo_empty', '20080403_213000_bbctwo_newsnight', '20080403_222000_bbctwo_ideal',
  '20080403_224500_bbcfour_david_ogilvy_the_first_mad_man', '20080403_231500_bbcthree_pulling', '20080403_232000_bbcone_holiday_weather', '20080403_232500_bbcone_panorama',
  '20080403_234500_bbcthree_pulling', '20080403_235500_bbcone_johnny_s_new_kingdom', '20080404_002500_bbcone_the_great_war_in_colour_the_wonderful',
  '20080404_004500_bbcfour_the_lost_world_of_tibet', '20080404_011000_bbcthree_doctor_who_confidential_kylie_special', '20080404_014500_bbcfour_mapping_everest',
  '20080404_015500_bbcone_bill_oddie_s_wild_side', '20080404_021500_bbcfour_a_year_in_tibet', '20080404_022500_bbcone_to_buy_or_not_to_buy',
  '20080404_022500_bbcthree_pulling', '20080404_030000_bbctwo_gcse_bitesize_revision_french_1', '20080404_050000_bbcone_breakfast', '20080404_050000_bbctwo_tikkabilla',
  '20080404_063000_bbctwo_mama_mirabelle_s_home_movies', '20080404_064500_bbctwo_tommy_zoom', '20080404_065500_bbctwo_take_a_bow', '20080404_070000_bbctwo_arthur',
  '20080404_074500_bbctwo_chucklevision', '20080404_080000_bbctwo_tracy_beaker', '20080404_090000_bbctwo_stupid', '20080404_100000_bbcone_to_buy_or_not_to_buy',
  '20080404_110000_bbctwo_the_daily_politics', '20080404_111500_bbcone_bargain_hunt', '20080404_113000_bbctwo_working_lunch', '20080404_120000_bbcone_bbc_news',
  '20080404_123000_bbctwo_coast', '20080404_124500_bbcone_doctors', '20080404_124500_bbctwo_racing_from_aintree', '20080404_140500_bbcone_space_pirates',
  '20080404_153500_bbcone_the_slammer', '20080404_154500_bbctwo_flog_it', '20080404_160000_bbcone_newsround', '20080404_161500_bbcone_the_weakest_link',
  '20080404_170000_bbcone_bbc_news', '20080404_170000_bbctwo_eggheads', '20080404_173000_bbcone_bbc_london_news', '20080404_180000_bbcfour_world_news_today',
  '20080404_180000_bbcone_the_one_show', '20080404_180000_bbcthree_top_gear', '20080404_180000_bbctwo_grand_national_preview',
  '20080404_183000_bbcfour_transatlantic_sessions', '20080404_183000_bbcone_inside_out', '20080404_190000_bbcfour_sacred_music', '20080404_190000_bbcone_eastenders',
  '20080404_190000_bbctwo_gardeners_world', '20080404_193000_bbcone_a_question_of_sport', '20080404_193000_bbcthree_eastenders_ricky_and_bianca',
  '20080404_200000_bbctwo_torchwood', '20080404_203000_bbcthree_lily_allen_and_friends', '20080404_205000_bbctwo_torchwood_declassified',
  '20080404_210000_bbcfour_hughie_green_most_sincerely', '20080404_210000_bbcone_bbc_ten_o_clock_news', '20080404_211500_bbcthree_eastenders',
  '20080404_213000_bbctwo_newsnight', '20080404_214500_bbcthree_eastenders_ricky_and_bianca', '20080404_222000_bbcfour_tv_s_believe_it_or_not',
  '20080404_223500_bbctwo_later_with_jools_holland', '20080404_231500_bbcfour_tv_s_believe_it_or_not', '20080404_233000_bbcthree_gavin_and_stacey',
  '20080405_000000_bbcthree_gavin_and_stacey', '20080405_003000_bbcthree_gavin_and_stacey', '20080405_005000_bbcone_horizon', '20080405_010000_bbcthree_gavin_and_stacey',
  '20080405_012000_bbcfour_sacred_music', '20080405_013000_bbcthree_lily_allen_and_friends', '20080405_014000_bbcone_shroud_of_turin',
  '20080405_022000_bbcfour_transatlantic_sessions', '20080405_024000_bbcone_natural_world', '20080405_031000_bbcthree_gavin_and_stacey', '20080405_050000_bbcone_breakfast',
  '20080405_052000_bbctwo_tikkabilla', '20080405_090000_bbctwo_hedz', '20080405_093000_bbctwo_the_slammer', '20080405_105000_bbcone_bbc_news',
  '20080405_110000_bbcone_match_of_the_day_live_fa_cup_semi_final', '20080405_110000_bbctwo_sound', '20080405_113000_bbctwo_the_surgery', '20080405_115000_bbctwo_them',
  '20080405_120000_bbctwo_the_grand_national', '20080405_133000_bbcone_grand_national', '20080405_153500_bbctwo_final_score', '20080405_160000_bbcone_bbc_news',
  '20080405_161500_bbcone_bbc_london_news', '20080405_161500_bbctwo_wild', '20080405_162000_bbcone_the_weakest_link', '20080405_164500_bbctwo_watching_the_wild',
  '20080405_172000_bbcone_doctor_who', '20080405_180000_bbcfour_sounds_of_the_sixties', '20080405_180000_bbcthree_football_gaffes_galore',
  '20080405_181000_bbcfour_the_naked_civil_servant', '20080405_181000_bbcone_i_d_do_anything', '20080405_181000_bbcthree_doctor_who_confidential',
  '20080405_182500_bbctwo_dad_s_army', '20080405_185500_bbcthree_top_gear', '20080405_190000_bbctwo_china_s_terracotta_army',
  '20080405_193000_bbcfour_doctor_who_the_daleks', '20080405_193000_bbcone_casualty', '20080405_195500_bbcfour_doctor_who_the_daleks',
  '20080405_200000_bbcthree_two_pints_of_lager_outtakes', '20080405_202000_bbcfour_doctor_who_the_daleks', '20080405_202000_bbcone_love_soup',
  '20080405_205000_bbcfour_verity_lambert_drama_queen', '20080405_205000_bbcone_bbc_news', '20080405_211000_bbcone_match_of_the_day',
  '20080405_221500_bbcthree_gavin_and_stacey', '20080405_221500_bbctwo_the_apprentice', '20080405_224000_bbcfour_jonathan_creek',
  '20080405_224000_bbcone_grand_national_highlights', '20080405_224500_bbcthree_gavin_and_stacey', '20080405_231500_bbcthree_gavin_and_stacey',
  '20080405_233000_bbcfour_mark_lawson_talks_to_george_cole', '20080405_234500_bbcthree_gavin_and_stacey', '20080406_005000_bbctwo_space_race',
  '20080406_010000_bbcthree_lily_allen_and_friends', '20080406_012500_bbcfour_verity_lambert_drama_queen', '20080406_014500_bbcthree_eastenders_ricky_and_bianca',
  '20080406_022500_bbcfour_the_hard_sell', '20080406_024500_bbcthree_two_pints_of_lager_outtakes', '20080406_030000_bbcthree_doctor_who_confidential',
  '20080406_050000_bbcone_breakfast', '20080406_052000_bbctwo_tikkabilla', '20080406_062500_bbcone_match_of_the_day', '20080406_070000_bbctwo_trapped',
  '20080406_073000_bbctwo_raven_the_secret_temple', '20080406_080000_bbcone_the_andrew_marr_show', '20080406_080000_bbctwo_hider_in_the_house',
  '20080406_100000_bbcone_countryfile', '20080406_110000_bbcone_bbc_news', '20080406_115000_bbcone_allo_allo', '20080406_122000_bbcone_eastenders',
  '20080406_143000_bbctwo_women_s_european_gymnastics_championship', '20080406_160000_bbcone_songs_of_praise', '20080406_160500_bbctwo_natural_world',
  '20080406_165500_bbctwo_delia', '20080406_170000_bbcone_keeping_up_appearances', '20080406_172500_bbctwo_millions', '20080406_173000_bbcone_seaside_rescue',
  '20080406_180000_bbcfour_sacred_music', '20080406_180000_bbcone_bbc_news', '20080406_180000_bbcthree_eastenders_ricky_and_bianca',
  '20080406_182000_bbcone_bbc_london_news', '20080406_183000_bbcone_i_d_do_anything_results', '20080406_190000_bbcfour_mozart_sacred_music',
  '20080406_190000_bbcone_tiger_spy_in_the_jungle', '20080406_190000_bbcthree_doctor_who', '20080406_190000_bbctwo_top_gear',
  '20080406_195000_bbcthree_doctor_who_confidential', '20080406_200000_bbcfour_dear_television', '20080406_200000_bbcthree_gavin_and_stacey',
  '20080406_200000_bbctwo_louis_theroux_s_african_hunting_holiday', '20080406_201000_bbcfour_washes_whiter', '20080406_203000_bbcthree_pulling',
  '20080406_210000_bbcone_bbc_news', '20080406_210000_bbctwo_match_of_the_day_2', '20080406_214500_bbcfour_hughie_green_most_sincerely',
  '20080406_221000_bbctwo_last_man_standing', '20080406_224500_bbcthree_gavin_and_stacey', '20080406_230500_bbcfour_legends_marty_feldman_six_degrees_of',
  '20080406_231500_bbcone_the_sky_at_night', '20080406_231500_bbcthree_pulling', '20080406_233500_bbcone_weatherview',
  '20080406_234000_bbcone_around_the_world_in_80_gardens', '20080407_000500_bbcfour_mozart_sacred_music', '20080407_004000_bbcone_holby_city',
  '20080407_004000_bbcthree_eastenders_ricky_and_bianca', '20080407_010500_bbcfour_sacred_music', '20080407_014000_bbcone_watchdog',
  '20080407_020500_bbcfour_legends_marty_feldman_six_degrees_of', '20080407_021000_bbcone_to_buy_or_not_to_buy', '20080407_030500_bbcthree_doctor_who_confidential',
  '20080407_034000_bbctwo_inside_sport', '20080407_050000_bbcone_breakfast', '20080407_050000_bbctwo_tikkabilla', '20080407_063000_bbctwo_mama_mirabelle_s_home_movies',
  '20080407_064500_bbctwo_tommy_zoom', '20080407_074500_bbctwo_chucklevision', '20080407_080000_bbctwo_tracy_beaker', '20080407_081500_bbcone_to_buy_or_not_to_buy',
  '20080407_090000_bbctwo_stupid', '20080407_095000_bbctwo_schools_look_and_read', '20080407_101000_bbctwo_schools_razzledazzle', '20080407_111500_bbcone_bargain_hunt',
  '20080407_113000_bbctwo_working_lunch', '20080407_120000_bbcone_bbc_news', '20080407_120000_bbctwo_schools_science_clip_investigates',
  '20080407_121000_bbctwo_schools_science_clip_investigates', '20080407_122000_bbctwo_schools_primary_geography', '20080407_123000_bbcone_bbc_london_news',
  '20080407_124500_bbcone_doctors', '20080407_140500_bbcone_space_pirates', '20080407_143500_bbcone_small_talk_diaries', '20080407_144500_bbctwo_flog_it',
  '20080407_150500_bbcone_the_revenge_files_of_alistair_fury', '20080407_153000_bbctwo_ready_steady_cook', '20080407_153500_bbcone_mi_high',
  '20080407_160000_bbcone_newsround', '20080407_161500_bbcone_the_weakest_link', '20080407_170000_bbcone_bbc_news', '20080407_170000_bbctwo_eggheads',
  '20080407_173000_bbcone_bbc_london_news', '20080407_180000_bbcfour_world_news_today', '20080407_180000_bbcone_the_one_show',
  '20080407_180000_bbcthree_two_pints_of_lager_outtakes', '20080407_180000_bbctwo_the_undercover_diplomat', '20080407_183000_bbcfour_pop_goes_the_sixties',
  '20080407_183000_bbcone_watchdog', '20080407_183500_bbcfour_doctor_who_the_daleks', '20080407_190000_bbcfour_the_sky_at_night', '20080407_190000_bbcone_eastenders',
  '20080407_190000_bbctwo_university_challenge_the_professionals', '20080407_193000_bbcfour_the_book_quiz', '20080407_193000_bbcone_panorama',
  '20080407_193000_bbctwo_delia', '20080407_200000_bbcfour_verity_lambert_drama_queen', '20080407_200000_bbctwo_clowns',
  '20080407_210000_bbcfour_shoulder_to_shoulder_annie_kenney', '20080407_210000_bbcone_bbc_ten_o_clock_news', '20080407_210000_bbcthree_eastenders',
  '20080407_212500_bbcone_bbc_london_news', '20080407_213000_bbcthree_gavin_and_stacey', '20080407_213000_bbctwo_newsnight', '20080407_220000_bbcthree_pulling',
  '20080407_222000_bbcfour_the_cult_of_adam_adamant_lives', '20080407_222000_bbctwo_archaeology_digging_the_past', '20080407_222500_bbcone_inside_sport',
  '20080407_225000_bbcfour_adam_adamant_lives', '20080407_233500_bbcfour_the_sky_at_night', '20080408_000500_bbcfour_the_book_quiz',
  '20080408_001500_bbcthree_gavin_and_stacey', '20080408_002000_bbcone_weatherview', '20080408_002500_bbcone_gardener_s_world',
  '20080408_003500_bbcfour_verity_lambert_drama_queen', '20080408_004500_bbcthree_pulling', '20080408_012500_bbcone_unknown_africa_the_comoros_islands',
  '20080408_013500_bbcfour_the_book_quiz', '20080408_014500_bbcthree_two_pints_of_lager_outtakes', '20080408_015500_bbcone_to_buy_or_not_to_buy',
  '20080408_020500_bbcfour_verity_lambert_drama_queen', '20080408_030000_bbctwo_key_stage_three_bitesize', '20080408_050000_bbcone_breakfast',
  '20080408_050000_bbctwo_tikkabilla', '20080408_063000_bbctwo_mama_mirabelle_s_home_movies', '20080408_064500_bbctwo_tommy_zoom', '20080408_065500_bbctwo_take_a_bow',
  '20080408_070000_bbctwo_arthur', '20080408_074500_bbctwo_chucklevision', '20080408_080000_bbctwo_tracy_beaker', '20080408_081500_bbcone_to_buy_or_not_to_buy',
  '20080408_090000_bbctwo_stupid', '20080408_093000_bbctwo_schools_words_and_pictures', '20080408_094000_bbctwo_schools_words_and_pictures', '20080408_101000_bbctwo_coast',
  '20080408_111500_bbcone_bargain_hunt', '20080408_113000_bbctwo_working_lunch', '20080408_120000_bbcone_bbc_news', '20080408_120000_bbctwo_schools_the_maths_channel',
  '20080408_121000_bbctwo_schools_primary_geography', '20080408_123000_bbcone_bbc_london_news', '20080408_124500_bbcone_doctors', '20080408_140500_bbcone_space_pirates',
  '20080408_143500_bbcone_small_talk_diaries', '20080408_144500_bbctwo_flog_it', '20080408_153000_bbctwo_ready_steady_cook', '20080408_153500_bbcone_blue_peter',
  '20080408_160000_bbcone_newsround', '20080408_161500_bbcone_the_weakest_link', '20080408_170000_bbcone_bbc_news', '20080408_170000_bbctwo_eggheads',
  '20080408_173000_bbcone_bbc_london_news', '20080408_175500_bbcone_party_election_broadcast_by_the', '20080408_180000_bbcfour_world_news_today',
  '20080408_180000_bbcone_the_one_show', '20080408_180000_bbctwo_how_diana_died_a_conspiracy', '20080408_183000_bbcfour_pop_goes_the_sixties',
  '20080408_183000_bbcone_eastenders', '20080408_183000_bbcthree_dog_borstal', '20080408_183500_bbcfour_doctor_who_the_daleks', '20080408_190000_bbcfour_life_in_cold_blood',
  '20080408_190000_bbcone_holby_city', '20080408_190000_bbcthree_dawn_gets_a_baby', '20080408_190000_bbctwo_university_challenge_the_professionals',
  '20080408_193000_bbctwo_johnny_s_new_kingdom', '20080408_200000_bbcfour_chinese_school', '20080408_200000_bbcone_hotel_babylon',
  '20080408_200000_bbctwo_massacre_at_virginia_tech', '20080408_210000_bbcfour_a_year_in_tibet', '20080408_210000_bbcone_bbc_ten_o_clock_news',
  '20080408_210000_bbcthree_eastenders', '20080408_210000_bbctwo_later_live_with_jools_holland', '20080408_212500_bbcone_bbc_london_news',
  '20080408_213000_bbcthree_little_britain_abroad', '20080408_213000_bbctwo_newsnight', '20080408_213500_bbcone_the_killing_of_sally_anne_bowman',
  '20080408_220000_bbcfour_the_book_quiz', '20080408_220000_bbcthree_the_wall', '20080408_223000_bbcfour_chinese_school', '20080408_233000_bbcfour_proms_on_four',
  '20080408_235000_bbcone_weatherview', '20080408_235500_bbcone_see_hear', '20080409_002500_bbcone_unknown_africa_central_african_republic',
  '20080409_005500_bbcone_life_in_cold_blood', '20080409_005500_bbcthree_dawn_gets_a_baby', '20080409_014500_bbcfour_chinese_school',
  '20080409_015500_bbcone_to_buy_or_not_to_buy', '20080409_022500_bbcthree_dog_borstal', '20080409_024500_bbcfour_the_book_quiz',
  '20080409_030000_bbctwo_key_stage_three_bitesize', '20080409_050000_bbcone_breakfast', '20080409_050000_bbctwo_tikkabilla',
  '20080409_063000_bbctwo_mama_mirabelle_s_home_movies', '20080409_064500_bbctwo_tommy_zoom', '20080409_065500_bbctwo_take_a_bow', '20080409_074500_bbctwo_chucklevision',
  '20080409_080000_bbctwo_tracy_beaker', '20080409_081500_bbcone_to_buy_or_not_to_buy', '20080409_090000_bbctwo_stupid', '20080409_104500_bbctwo_coast',
  '20080409_111500_bbcone_bargain_hunt', '20080409_120000_bbcone_bbc_news', '20080409_120000_bbctwo_see_hear', '20080409_123000_bbcone_bbc_london_news',
  '20080409_123000_bbctwo_working_lunch', '20080409_124500_bbcone_doctors', '20080409_130000_bbctwo_world_swimming_championships', '20080409_140500_bbcone_space_pirates',
  '20080409_143500_bbcone_small_talk_diaries', '20080409_144500_bbctwo_flog_it', '20080409_150500_bbcone_young_dracula', '20080409_153000_bbctwo_ready_steady_cook',
  '20080409_153500_bbcone_blue_peter', '20080409_160000_bbcone_newsround', '20080409_161500_bbcone_the_weakest_link', '20080409_170000_bbcone_bbc_news',
  '20080409_170000_bbctwo_eggheads', '20080409_173000_bbcone_bbc_london_news', '20080409_175500_bbcone_party_election_broadcast_by_the',
  '20080409_180000_bbcfour_world_news_today', '20080409_180000_bbcone_the_one_show', '20080409_180000_bbctwo_world_swimming_championships',
  '20080409_183000_bbcfour_doctor_who_the_daleks', '20080409_183000_bbcone_street_doctor', '20080409_183000_bbcthree_dog_borstal',
  '20080409_185500_bbcfour_doctor_who_the_daleks', '20080409_190000_bbcone_traffic_cops', '20080409_190000_bbctwo_natural_world',
  '20080409_192000_bbcfour_live_on_the_night_time_shift', '20080409_195000_bbctwo_badlands_raging_bulls', '20080409_200000_bbcfour_frankie_howerd_rather_you_than_me',
  '20080409_200000_bbcone_the_apprentice', '20080409_200000_bbctwo_dan_cruickshank_s_adventures', '20080409_210000_bbcfour_arena_oooh_er_missus_the_frankie',
  '20080409_210000_bbcone_bbc_ten_o_clock_news', '20080409_210000_bbctwo_the_apprentice_you_re_fired', '20080409_212500_bbcone_bbc_london_news',
  '20080409_213000_bbctwo_newsnight', '20080409_220000_bbcfour_up_pompeii', '20080409_222000_bbctwo_golf_us_masters',
  '20080409_223000_bbcfour_mark_lawson_talks_to_david_renwick', '20080409_231500_bbcthree_the_wall', '20080409_233000_bbcfour_frankie_howerd_rather_you_than_me',
  '20080410_002500_bbcfour_demob_happy_how_tv_conquered', '20080410_003000_bbcone_weatherview', '20080410_003500_bbcone_unknown_africa_angola',
  '20080410_012500_bbcfour_mark_lawson_talks_to_david_renwick', '20080410_022500_bbcfour_frankie_howerd_rather_you_than_me', '20080410_023500_bbcone_to_buy_or_not_to_buy',
  '20080410_025000_bbcthree_dog_borstal', '20080410_030000_bbctwo_key_stage_three_bitesize', '20080410_050000_bbctwo_tikkabilla',
  '20080410_063000_bbctwo_mama_mirabelle_s_home_movies', '20080410_064500_bbctwo_tommy_zoom', '20080410_070000_bbctwo_arthur', '20080410_074500_bbctwo_chucklevision',
  '20080410_080000_bbctwo_tracy_beaker', '20080410_081500_bbcone_to_buy_or_not_to_buy', '20080410_090000_bbctwo_stupid', '20080410_093000_bbctwo_schools_primary_history',
  '20080410_101000_bbctwo_schools_primary_geography', '20080410_103000_bbctwo_schools_science_clips', '20080410_104000_bbctwo_schools_science_clips',
  '20080410_105000_bbctwo_schools_hands_up', '20080410_110000_bbctwo_open_gardens', '20080410_111500_bbcone_bargain_hunt', '20080410_113000_bbctwo_working_lunch',
  '20080410_120000_bbcone_bbc_news', '20080410_120000_bbctwo_world_swimming_championships', '20080410_123000_bbcone_bbc_london_news', '20080410_124500_bbcone_doctors',
  '20080410_140500_bbcone_space_pirates', '20080410_143500_bbcone_small_talk_diaries', '20080410_144500_bbctwo_flog_it', '20080410_150500_bbcone_stake_out',
  '20080410_153500_bbcone_beat_the_boss', '20080410_160000_bbcone_newsround', '20080410_161500_bbcone_the_weakest_link', '20080410_170000_bbcone_bbc_news',
  '20080410_170000_bbctwo_eggheads', '20080410_173000_bbcone_bbc_london_news', '20080410_180000_bbcfour_world_news_today', '20080410_180000_bbcone_the_one_show',
  '20080410_180000_bbctwo_world_swimming_championships', '20080410_183000_bbcfour_in_search_of_medieval_britain', '20080410_183000_bbcone_eastenders',
  '20080410_190000_bbcone_holby_blue', '20080410_190000_bbctwo_coast', '20080410_200000_bbctwo_golf_us_masters', '20080410_210000_bbcfour_chinese_school',
  '20080410_210000_bbcone_bbc_ten_o_clock_news', '20080410_210000_bbcthree_eastenders', '20080410_212500_bbcone_bbc_london_news',
  '20080410_213000_bbcthree_lily_allen_my_favourite_bits', '20080410_213500_bbcone_golf_us_masters', '20080410_222000_bbctwo_ideal',
  '20080410_224500_bbcfour_in_search_of_medieval_britain', '20080410_231000_bbcthree_pulling', '20080410_231500_bbcfour_the_sky_at_night', '20080410_235000_bbcone_panorama',
  '20080411_004000_bbcthree_lily_allen_my_favourite_bits', '20080411_004500_bbcfour_chinese_school', '20080411_005000_bbcone_the_twenties_in_colour_the_wonderful',
  '20080411_012000_bbcone_bill_oddie_s_wild_side', '20080411_014000_bbcthree_pulling', '20080411_014500_bbcfour_in_search_of_medieval_britain',
  '20080411_030000_bbctwo_gcse_bitesize', '20080411_050000_bbcone_breakfast', '20080411_050000_bbctwo_tikkabilla', '20080411_063000_bbctwo_mama_mirabelle_s_home_movies',
  '20080411_064500_bbctwo_tommy_zoom', '20080411_065500_bbctwo_take_a_bow', '20080411_070000_bbctwo_arthur', '20080411_074500_bbctwo_chucklevision',
  '20080411_080000_bbctwo_tracy_beaker', '20080411_081500_bbcone_to_buy_or_not_to_buy', '20080411_090000_bbctwo_stupid',
  '20080411_094500_bbctwo_schools_the_way_things_work', '20080411_100000_bbctwo_schools_the_way_things_work', '20080411_103000_bbctwo_schools_watch',
  '20080411_104500_bbctwo_schools_something_special', '20080411_111500_bbcone_bargain_hunt', '20080411_113000_bbctwo_working_lunch',
  '20080411_123000_bbcone_bbc_london_news', '20080411_123000_bbctwo_world_swimming_championships', '20080411_140500_bbcone_space_pirates',
  '20080411_143500_bbcone_small_talk_diaries', '20080411_144500_bbctwo_flog_it', '20080411_151000_bbcone_basil_brush', '20080411_153000_bbctwo_ready_steady_cook',
  '20080411_153500_bbcone_the_slammer', '20080411_160000_bbcone_newsround', '20080411_161500_bbcone_the_weakest_link', '20080411_170000_bbcone_bbc_news',
  '20080411_170000_bbctwo_eggheads', '20080411_173000_bbcone_bbc_london_news', '20080411_180000_bbcone_the_one_show', '20080411_180000_bbcthree_top_gear',
  '20080411_180000_bbctwo_world_swimming_championships', '20080411_183000_bbcfour_transatlantic_sessions', '20080411_183000_bbcone_inside_out',
  '20080411_190000_bbcfour_sacred_music', '20080411_190000_bbcone_eastenders', '20080411_190000_bbctwo_gardeners_world', '20080411_193000_bbcone_a_question_of_sport',
  '20080411_200000_bbctwo_golf_world_us_masters', '20080411_202000_bbcthree_doctor_who_confidential', '20080411_210000_bbcfour_frankie_howerd_rather_you_than_me',
  '20080411_210000_bbcone_bbc_ten_o_clock_news', '20080411_210000_bbcthree_eastenders', '20080411_212500_bbcone_bbc_london_news', '20080411_213000_bbctwo_newsnight',
  '20080411_224000_bbctwo_later_with_jools_holland', '20080411_231500_bbcthree_gavin_and_stacey', '20080411_234500_bbcthree_lily_allen_my_favourite_bits',
  '20080412_000500_bbcfour_sacred_music', '20080412_010500_bbcfour_frankie_howerd_rather_you_than_me', '20080412_014000_bbcthree_gavin_and_stacey',
  '20080412_015000_bbcone_weatherview', '20080412_015500_bbcone_dan_cruickshank_s_adventures', '20080412_024000_bbcthree_lily_allen_my_favourite_bits',
  '20080412_025500_bbcone_natural_world', '20080412_050000_bbcone_breakfast', '20080412_052000_bbctwo_tikkabilla', '20080412_090000_bbctwo_hedz',
  '20080412_093000_bbctwo_the_slammer', '20080412_104500_bbctwo_sportsround', '20080412_111000_bbcone_football_focus', '20080412_114500_bbctwo_the_surgery',
  '20080412_120000_bbcone_swimming_world_championships', '20080412_120500_bbctwo_sound', '20080412_123500_bbctwo_the_sky_at_night',
  '20080412_131000_bbcone_rugby_union_anglo_welsh_cup_final', '20080412_152500_bbctwo_world_swimming_championships', '20080412_153000_bbcone_final_score',
  '20080412_162000_bbcone_bbc_news', '20080412_162500_bbcone_bbc_london_news', '20080412_163500_bbcone_outtake_tv', '20080412_170500_bbcone_the_kids_are_all_right',
  '20080412_174500_bbcone_doctor_who', '20080412_180000_bbcfour_meetings_with_remarkable_trees', '20080412_180000_bbcthree_dog_borstal', '20080412_180000_bbctwo_dad_s_army',
  '20080412_181000_bbcfour_in_search_of_medieval_britain', '20080412_183000_bbctwo_the_lost_world_of_tibet', '20080412_183500_bbcone_i_d_do_anything',
  '20080412_183500_bbcthree_doctor_who_confidential', '20080412_184000_bbcfour_the_book_quiz', '20080412_192000_bbcthree_top_gear', '20080412_193000_bbctwo_golf_us_masters',
  '20080412_195000_bbcone_casualty', '20080412_201000_bbcfour_pompeii_the_last_day', '20080412_204000_bbcone_love_soup', '20080412_211000_bbcone_bbc_news',
  '20080412_213000_bbcone_match_of_the_day', '20080412_222000_bbcthree_gavin_and_stacey', '20080412_234500_bbctwo_the_apprentice', '20080413_002500_bbcone_weatherview',
  '20080413_003000_bbcthree_lily_allen_my_favourite_bits', '20080413_004500_bbctwo_space_race', '20080413_005500_bbcfour_10_things_you_didn_t_know_about',
  '20080413_013000_bbcthree_gavin_and_stacey', '20080413_015500_bbcfour_the_book_quiz', '20080413_020000_bbcthree_the_wall', '20080413_050000_bbcone_breakfast',
  '20080413_050000_bbctwo_fimbles', '20080413_052000_bbctwo_tikkabilla', '20080413_060500_bbcone_match_of_the_day', '20080413_070000_bbctwo_trapped',
  '20080413_073000_bbcone_london_marathon', '20080413_073000_bbctwo_raven_the_secret_temple', '20080413_113000_bbctwo_moto_gp', '20080413_130000_bbcone_the_politics_show',
  '20080413_130000_bbctwo_premiership_rugby', '20080413_134500_bbctwo_world_swimming_championships', '20080413_140500_bbcone_eastenders',
  '20080413_163000_bbcone_songs_of_praise', '20080413_170500_bbcone_bbc_news', '20080413_172000_bbcone_bbc_london_news', '20080413_174000_bbctwo_london_marathon_highlights',
  '20080413_180000_bbcfour_sacred_music', '20080413_180000_bbcthree_sound', '20080413_183000_bbcone_i_d_do_anything_results', '20080413_183000_bbctwo_golf_us_masters',
  '20080413_190000_bbcfour_proms_on_four', '20080413_190000_bbcone_tiger_spy_in_the_jungle', '20080413_195000_bbcthree_doctor_who_confidential',
  '20080413_200000_bbcfour_dear_television', '20080413_200000_bbcthree_gavin_and_stacey', '20080413_203000_bbcthree_pulling', '20080413_210000_bbcone_bbc_ten_o_clock_news',
  '20080413_210000_bbcthree_lily_allen_my_favourite_bits', '20080413_214500_bbcfour_frankie_howerd_rather_you_than_me', '20080413_224500_bbcthree_gavin_and_stacey',
  '20080413_231500_bbcthree_pulling', '20080413_234500_bbcfour_proms_on_four', '20080413_234500_bbcthree_lily_allen_my_favourite_bits',
  '20080413_234500_bbctwo_last_man_standing', '20080414_000000_bbcone_weatherview', '20080414_000500_bbcone_around_the_world_in_80_gardens',
  '20080414_004000_bbcfour_sacred_music', '20080414_010500_bbcone_holby_city', '20080414_014000_bbcfour_frankie_howerd_rather_you_than_me',
  '20080414_020500_bbcone_watchdog', '20080414_023500_bbcone_to_buy_or_not_to_buy', '20080414_030500_bbcthree_dog_borstal', '20080414_033000_bbctwo_inside_sport',
  '20080414_033500_bbcthree_doctor_who_confidential', '20080414_041000_bbctwo_london_marathon_highlights', '20080414_050000_bbcone_breakfast',
  '20080414_050000_bbctwo_tikkabilla', '20080414_062500_bbctwo_newsround', '20080414_063000_bbctwo_hider_in_the_house', '20080414_073000_bbctwo_jackanory_junior',
  '20080414_080500_bbctwo_boogie_beebies', '20080414_081500_bbcone_to_buy_or_not_to_buy', '20080414_083500_bbctwo_something_special',
  '20080414_095000_bbctwo_schools_look_and_read', '20080414_103000_bbcone_cash_in_the_attic', '20080414_110000_bbctwo_open_gardens', '20080414_111500_bbcone_bargain_hunt',
  '20080414_120000_bbcone_bbc_news', '20080414_120000_bbctwo_schools_science_clip_investigates', '20080414_121000_bbctwo_schools_science_clip_investigates',
  '20080414_122000_bbctwo_schools_primary_geography', '20080414_124500_bbcone_doctors', '20080414_140500_bbcone_space_pirates', '20080414_143500_bbcone_small_talk_diaries',
  '20080414_144500_bbctwo_flog_it', '20080414_151000_bbcone_roar', '20080414_153000_bbctwo_ready_steady_cook', '20080414_153500_bbcone_grange_hill',
  '20080414_160000_bbcone_newsround', '20080414_161500_bbcone_the_weakest_link', '20080414_170000_bbcone_bbc_news', '20080414_170000_bbctwo_eggheads',
  '20080414_173000_bbcone_bbc_london_news', '20080414_173000_bbctwo_great_british_menu', '20080414_180000_bbcone_the_one_show',
  '20080414_180000_bbcthree_young_mums_mansion_friends_and_family', '20080414_183000_bbcone_watchdog', '20080414_183000_bbcthree_dog_borstal',
  '20080414_185000_bbctwo_new_forest_ponies', '20080414_190000_bbcone_eastenders', '20080414_190000_bbctwo_university_challenge_the_professionals',
  '20080414_193000_bbcfour_the_book_quiz', '20080414_193000_bbcone_panorama', '20080414_193000_bbctwo_delia', '20080414_200000_bbcone_waking_the_dead',
  '20080414_210000_bbcone_bbc_ten_o_clock_news', '20080414_210000_bbcthree_eastenders', '20080414_210000_bbctwo_grumpy_guide_to_politics',
  '20080414_213000_bbcthree_gavin_and_stacey', '20080414_213000_bbctwo_newsnight', '20080414_213500_bbcone_meet_the_immigrants', '20080414_220000_bbcthree_pulling',
  '20080414_220500_bbcone_inside_sport', '20080414_222000_bbctwo_court_on_camera', '20080414_230000_bbcfour_arena_saints', '20080415_001500_bbcthree_gavin_and_stacey',
  '20080415_002500_bbcone_weatherview', '20080415_003000_bbcone_louis_theroux_s_african_hunting_holiday', '20080415_004000_bbcthree_pulling',
  '20080415_011500_bbcfour_the_book_quiz', '20080415_013000_bbcone_gardeners_world', '20080415_023000_bbcone_blackpool_medics', '20080415_024500_bbcfour_arena_saints',
  '20080415_030000_bbcone_to_buy_or_not_to_buy', '20080415_030000_bbctwo_history_file_20th_century_world', '20080415_030500_bbcthree_young_mums_mansion_friends_and_family',
  '20080415_050000_bbcone_breakfast', '20080415_050000_bbctwo_tikkabilla', '20080415_062500_bbctwo_newsround', '20080415_063000_bbctwo_hider_in_the_house',
  '20080415_073000_bbctwo_jackanory_junior', '20080415_080500_bbctwo_boogie_beebies', '20080415_081500_bbcone_to_buy_or_not_to_buy',
  '20080415_083500_bbctwo_something_special', '20080415_093000_bbctwo_schools_words_and_pictures', '20080415_094000_bbctwo_schools_words_and_pictures',
  '20080415_101000_bbctwo_timewatch', '20080415_111000_bbctwo_coast', '20080415_111500_bbcone_bargain_hunt', '20080415_113000_bbctwo_working_lunch',
  '20080415_120000_bbctwo_schools_the_maths_channel', '20080415_121000_bbctwo_schools_primary_geography', '20080415_123000_bbctwo_coast', '20080415_124500_bbcone_doctors',
  '20080415_140500_bbcone_space_pirates', '20080415_143500_bbcone_small_talk_diaries', '20080415_144500_bbctwo_flog_it', '20080415_153500_bbcone_blue_peter',
  '20080415_161500_bbcone_the_weakest_link', '20080415_161500_bbctwo_escape_to_the_country', '20080415_170000_bbcone_bbc_news', '20080415_170000_bbctwo_eggheads',
  '20080415_173000_bbcone_bbc_london_news', '20080415_173000_bbctwo_great_british_menu', '20080415_180000_bbcfour_world_news_today', '20080415_180000_bbcone_the_one_show',
  '20080415_180000_bbctwo_torchwood', '20080415_183000_bbcfour_pop_goes_the_sixties', '20080415_183000_bbcone_eastenders', '20080415_183000_bbcthree_dog_borstal',
  '20080415_185000_bbctwo_torchwood_declassified', '20080415_190000_bbcfour_life_in_cold_blood', '20080415_190000_bbcone_holby_city',
  '20080415_190000_bbctwo_university_challenge_the_professionals', '20080415_200000_bbcfour_chinese_school', '20080415_200000_bbcone_waking_the_dead',
  '20080415_200000_bbcthree_young_mums_mansion', '20080415_200000_bbctwo_age_of_terror', '20080415_210000_bbcfour_goodness_gracious_me',
  '20080415_210000_bbcone_bbc_ten_o_clock_news', '20080415_210000_bbcthree_eastenders', '20080415_210000_bbctwo_later_live_with_jools_holland',
  '20080415_213000_bbcthree_little_britain_abroad', '20080415_213000_bbctwo_newsnight', '20080415_220000_bbcthree_the_wall', '20080415_232500_bbcthree_young_mums_mansion',
  '20080415_234500_bbcone_weatherview', '20080415_235000_bbcfour_chinese_school', '20080416_002000_bbcone_blackpool_medics', '20080416_005000_bbcfour_proms_on_four',
  '20080416_005000_bbcone_tiger_spy_in_the_jungle', '20080416_012000_bbcthree_dog_borstal', '20080416_015000_bbcthree_dog_borstal', '20080416_022500_bbcfour_chinese_school',
  '20080416_030000_bbctwo_key_stage_three_bitesize', '20080416_044500_bbctwo_talk_german', '20080416_062500_bbctwo_newsround', '20080416_063000_bbctwo_hider_in_the_house',
  '20080416_073000_bbctwo_jackanory_junior', '20080416_080500_bbctwo_boogie_beebies', '20080416_081500_bbcone_to_buy_or_not_to_buy',
  '20080416_083500_bbctwo_something_special', '20080416_100000_bbcone_open_house', '20080416_111500_bbcone_bargain_hunt', '20080416_113000_bbctwo_open_gardens',
  '20080416_120000_bbcone_bbc_news', '20080416_120000_bbctwo_see_hear', '20080416_123000_bbctwo_working_lunch', '20080416_124500_bbcone_doctors',
  '20080416_140500_bbcone_space_pirates', '20080416_143500_bbcone_small_talk_diaries', '20080416_144500_bbctwo_flog_it', '20080416_151000_bbcone_young_dracula',
  '20080416_153000_bbctwo_ready_steady_cook', '20080416_153500_bbcone_blue_peter', '20080416_160000_bbcone_newsround', '20080416_161500_bbcone_the_weakest_link',
  '20080416_170000_bbcone_bbc_news', '20080416_170000_bbctwo_eggheads', '20080416_173000_bbcone_bbc_london_news', '20080416_180000_bbcfour_world_news_today',
  '20080416_180000_bbcone_the_one_show', '20080416_183000_bbcfour_pop_goes_the_sixties', '20080416_185000_bbctwo_coast',
  '20080416_190000_bbcfour_a_history_of_britain_by_simon_schama', '20080416_190000_bbcone_traffic_cops', '20080416_190000_bbctwo_natural_world',
  '20080416_195000_bbctwo_watching_the_wild', '20080416_200000_bbcfour_the_saint_and_the_hanged_man', '20080416_200000_bbcone_the_apprentice',
  '20080416_200000_bbctwo_dan_cruickshank_s_adventures', '20080416_210000_bbcfour_fantabulosa_kenneth_williams', '20080416_210000_bbcone_bbc_ten_o_clock_news',
  '20080416_210000_bbctwo_the_apprentice_you_re_fired', '20080416_213000_bbctwo_newsnight', '20080416_222000_bbcfour_kenneth_williams_in_his_own_words',
  '20080416_222000_bbctwo_desi_dna', '20080416_231500_bbcthree_the_wall', '20080416_235000_bbcfour_the_saint_and_the_hanged_man', '20080417_001000_bbcone_weatherview',
  '20080417_001500_bbcone_blackpool_medics', '20080417_005000_bbcfour_fantabulosa_kenneth_williams', '20080417_014500_bbcone_an_island_parish',
  '20080417_021000_bbcfour_the_saint_and_the_hanged_man', '20080417_021500_bbcone_to_buy_or_not_to_buy', '20080417_024000_bbcthree_the_wall',
  '20080417_030000_bbctwo_key_stage_three_bitesize', '20080417_050000_bbcone_breakfast', '20080417_050000_bbctwo_tikkabilla', '20080417_063000_bbctwo_hider_in_the_house',
  '20080417_073000_bbctwo_jackanory_junior', '20080417_080500_bbctwo_boogie_beebies', '20080417_081500_bbcone_to_buy_or_not_to_buy',
  '20080417_083500_bbctwo_something_special', '20080417_093000_bbctwo_schools_primary_history', '20080417_095000_bbctwo_schools_megamaths',
  '20080417_101000_bbctwo_schools_landmarks', '20080417_103000_bbctwo_schools_science_clips', '20080417_104000_bbctwo_schools_science_clips',
  '20080417_105000_bbctwo_schools_hands_up', '20080417_110000_bbctwo_open_gardens', '20080417_111500_bbcone_bargain_hunt', '20080417_113000_bbctwo_working_lunch',
  '20080417_120000_bbcone_bbc_news', '20080417_120000_bbctwo_coast', '20080417_124500_bbcone_doctors', '20080417_140500_bbcone_space_pirates',
  '20080417_143500_bbcone_small_talk_diaries', '20080417_144500_bbctwo_flog_it', '20080417_150500_bbcone_stake_out', '20080417_153000_bbctwo_ready_steady_cook',
  '20080417_153500_bbcone_beat_the_boss', '20080417_160000_bbcone_newsround', '20080417_161500_bbcone_the_weakest_link', '20080417_170000_bbcone_bbc_news',
  '20080417_170000_bbctwo_eggheads', '20080417_173000_bbcone_bbc_london_news', '20080417_173000_bbctwo_great_british_menu', '20080417_180000_bbcfour_world_news_today',
  '20080417_180000_bbcone_the_one_show', '20080417_180000_bbctwo_delia', '20080417_183000_bbcfour_in_search_of_medieval_britain', '20080417_183000_bbcone_eastenders',
  '20080417_190000_bbcone_holby_blue', '20080417_190000_bbcthree_dawn_gets_naked', '20080417_190000_bbctwo_coast', '20080417_200000_bbcfour_inside_the_medieval_mind',
  '20080417_204500_bbctwo_heroes_return', '20080417_210000_bbcfour_crusades', '20080417_210000_bbcone_bbc_ten_o_clock_news', '20080417_210000_bbcthree_eastenders',
  '20080417_213000_bbcthree_sex_with_mum_and_dad', '20080417_213000_bbctwo_newsnight', '20080417_222000_bbctwo_ideal', '20080417_223500_bbcfour_chinese_school',
  '20080417_232000_bbcone_holiday_weather', '20080417_232500_bbcone_panorama', '20080417_233500_bbcfour_in_search_of_medieval_britain',
  '20080417_235500_bbcone_the_undercover_diplomat', '20080418_000500_bbcfour_inside_the_medieval_mind', '20080418_004000_bbcthree_dawn_gets_naked',
  '20080418_005500_bbcone_johnny_s_new_kingdom', '20080418_010500_bbcfour_the_book_quiz', '20080418_012500_bbcone_the_twenties_in_colour_the_wonderful',
  '20080418_013500_bbcfour_in_search_of_medieval_britain', '20080418_013500_bbcthree_pulling', '20080418_015500_bbcone_bill_oddie_s_wild_side',
  '20080418_020500_bbcfour_inside_the_medieval_mind', '20080418_020500_bbcthree_sex_with_mum_and_dad', '20080418_022500_bbcone_to_buy_or_not_to_buy',
  '20080418_030000_bbctwo_gcse_bitesize', '20080418_050000_bbcone_breakfast', '20080418_050000_bbctwo_tikkabilla', '20080418_073000_bbctwo_jackanory_junior',
  '20080418_080500_bbctwo_boogie_beebies', '20080418_081500_bbcone_to_buy_or_not_to_buy', '20080418_083500_bbctwo_something_special',
  '20080418_094500_bbctwo_schools_the_way_things_work', '20080418_100000_bbcone_open_house', '20080418_100000_bbctwo_schools_the_way_things_work',
  '20080418_103000_bbctwo_schools_watch', '20080418_104500_bbctwo_schools_something_special', '20080418_110000_bbctwo_open_gardens', '20080418_111500_bbcone_bargain_hunt',
  '20080418_113000_bbctwo_working_lunch', '20080418_120000_bbcone_bbc_news', '20080418_124500_bbcone_doctors', '20080418_140500_bbcone_space_pirates',
  '20080418_143500_bbcone_small_talk_diaries', '20080418_144500_bbctwo_flog_it', '20080418_153000_bbctwo_ready_steady_cook', '20080418_153500_bbcone_the_slammer',
  '20080418_160000_bbcone_newsround', '20080418_161500_bbcone_the_weakest_link', '20080418_170000_bbcone_bbc_news', '20080418_170000_bbctwo_eggheads',
  '20080418_173000_bbcone_bbc_london_news', '20080418_180000_bbcfour_world_news_today', '20080418_180000_bbcone_the_one_show', '20080418_180000_bbcthree_top_gear',
  '20080418_183000_bbcfour_chopin_etudes', '20080418_183000_bbcone_inside_out', '20080418_183000_bbctwo_delia',
  '20080418_183500_bbcfour_the_creation_by_haydn_at_the_barbican', '20080418_190000_bbcone_eastenders', '20080418_190000_bbctwo_gardeners_world',
  '20080418_193000_bbcone_a_question_of_sport', '20080418_200000_bbcthree_doctor_who', '20080418_200000_bbctwo_the_apprentice_motor_mouths',
  '20080418_203000_bbcfour_amazing_journey_the_story_of_the_who', '20080418_205000_bbcthree_doctor_who_confidential', '20080418_210000_bbcone_bbc_ten_o_clock_news',
  '20080418_210000_bbcthree_eastenders', '20080418_213000_bbctwo_newsnight', '20080418_220000_bbctwo_newsnight_review', '20080418_223500_bbctwo_later_with_jools_holland',
  '20080418_231500_bbcthree_gavin_and_stacey', '20080418_232000_bbcfour_the_who_at_the_electric_proms', '20080419_001000_bbcfour_pop_britannia',
  '20080419_001000_bbcone_weatherview', '20080419_001500_bbcone_dan_cruickshank_s_adventures', '20080419_011000_bbcfour_the_creation_by_haydn_at_the_barbican',
  '20080419_011500_bbcone_natural_world', '20080419_014000_bbcthree_gavin_and_stacey', '20080419_020500_bbcone_to_buy_or_not_to_buy',
  '20080419_021000_bbcthree_eastenders_ricky_and_bianca', '20080419_050000_bbcone_breakfast', '20080419_050000_bbctwo_fimbles', '20080419_052000_bbctwo_tikkabilla',
  '20080419_070000_bbctwo_sorcerer_s_apprentice', '20080419_090000_bbctwo_hedz', '20080419_093000_bbctwo_the_slammer', '20080419_104500_bbctwo_sportsround',
  '20080419_110000_bbcone_bbc_news', '20080419_114500_bbctwo_the_surgery', '20080419_120000_bbcone_snooker_world_championship', '20080419_120500_bbctwo_sound',
  '20080419_153000_bbcone_final_score', '20080419_153000_bbctwo_snooker_world_championship', '20080419_161500_bbctwo_rugby_league_challenge_cup',
  '20080419_162000_bbcone_bbc_news', '20080419_163000_bbcone_bbc_london_news', '20080419_164000_bbcone_the_kids_are_all_right', '20080419_172000_bbcone_doctor_who',
  '20080419_180000_bbcthree_gaffes_galore_outtakes', '20080419_180500_bbcone_i_d_do_anything', '20080419_180500_bbcthree_doctor_who_confidential',
  '20080419_183000_bbcfour_in_search_of_medieval_britain', '20080419_183000_bbctwo_snooker_world_championship', '20080419_185000_bbcthree_top_gear',
  '20080419_190500_bbcfour_the_book_quiz', '20080419_193500_bbcfour_mrs_brown', '20080419_193500_bbctwo_dad_s_army', '20080419_195500_bbcone_casualty',
  '20080419_200500_bbctwo_have_i_got_a_bit_more_news_for_you', '20080419_204500_bbcone_love_soup', '20080419_204500_bbctwo_comedy_map_of_britain',
  '20080419_211500_bbcone_bbc_news', '20080419_213000_bbcone_match_of_the_day', '20080419_214500_bbctwo_the_apprentice', '20080419_221000_bbcthree_gavin_and_stacey',
  '20080419_225000_bbcfour_fantabulosa_kenneth_williams', '20080420_000500_bbctwo_snooker_world_championship_highlights',
  '20080420_001000_bbcfour_the_saint_and_the_hanged_man', '20080420_002000_bbcthree_gavin_and_stacey', '20080420_005000_bbcthree_the_wall',
  '20080420_005500_bbctwo_snooker_extra', '20080420_013500_bbcone_weatherview', '20080420_021000_bbcfour_fantabulosa_kenneth_williams',
  '20080420_032000_bbcthree_gaffes_galore_outtakes', '20080420_050000_bbcone_breakfast', '20080420_052000_bbctwo_tikkabilla', '20080420_063500_bbcone_match_of_the_day',
  '20080420_070000_bbctwo_trapped', '20080420_073000_bbctwo_raven_the_secret_temple', '20080420_080000_bbcone_the_andrew_marr_show',
  '20080420_080000_bbctwo_hider_in_the_house', '20080420_100000_bbcone_countryfile', '20080420_110000_bbcone_the_politics_show', '20080420_120000_bbcone_allo_allo',
  '20080420_120000_bbctwo_snooker_world_championship', '20080420_131000_bbcone_eastenders', '20080420_150500_bbcone_tiger_spy_in_the_jungle',
  '20080420_154500_bbctwo_premiership_rugby', '20080420_160500_bbcone_lifeline', '20080420_161500_bbcone_points_of_view',
  '20080420_161500_bbctwo_rugby_league_challenge_cup', '20080420_163000_bbcone_songs_of_praise', '20080420_170500_bbcone_bbc_news', '20080420_172000_bbcone_bbc_london_news',
  '20080420_173000_bbcone_seaside_rescue', '20080420_180000_bbcfour_inside_the_medieval_mind', '20080420_180000_bbcthree_sound',
  '20080420_183000_bbcone_i_d_do_anything_results', '20080420_183000_bbctwo_snooker_world_championship', '20080420_190000_bbcone_the_british_academy_television_awards',
  '20080420_194500_bbcthree_doctor_who_confidential', '20080420_200000_bbcfour_dear_television', '20080420_200000_bbcthree_gavin_and_stacey',
  '20080420_201000_bbcfour_washes_whiter', '20080420_203000_bbcthree_pulling', '20080420_210000_bbcone_bbc_ten_o_clock_news',
  '20080420_210000_bbcthree_amy_my_body_for_bucks', '20080420_214000_bbctwo_graham_norton_uncut', '20080420_215000_bbcfour_amazing_journey_the_story_of_the_who',
  '20080420_222500_bbctwo_last_man_standing', '20080420_224500_bbcthree_gavin_and_stacey', '20080420_231500_bbcthree_pulling',
  '20080420_232000_bbctwo_snooker_world_championship_highlights', '20080420_234000_bbcone_weatherview', '20080420_234500_bbcone_around_the_world_in_80_gardens',
  '20080420_234500_bbcthree_amy_my_body_for_bucks', '20080420_235000_bbcfour_the_who_at_the_electric_prom', '20080421_001000_bbctwo_snooker_extra',
  '20080421_004500_bbcone_holby_city', '20080421_014000_bbcfour_inside_the_medieval_mind', '20080421_014500_bbcone_age_of_terror', '20080421_021000_bbcthree_dog_borstal',
  '20080421_024000_bbcthree_dog_borstal', '20080421_024500_bbcone_watchdog', '20080421_031000_bbcthree_doctor_who_confidential', '20080421_031000_bbctwo_inside_sport',
  '20080421_050000_bbcone_breakfast', '20080421_050000_bbctwo_tikkabilla', '20080421_062500_bbctwo_newsround', '20080421_063000_bbctwo_hider_in_the_house',
  '20080421_073000_bbctwo_jackanory_junior', '20080421_080000_bbctwo_boogie_beebies', '20080421_081500_bbcone_missing_live', '20080421_084000_bbctwo_something_special',
  '20080421_095000_bbctwo_schools_look_and_read', '20080421_100000_bbcone_to_buy_or_not_to_buy', '20080421_101000_bbctwo_schools_razzledazzle',
  '20080421_110000_bbctwo_the_daily_politics', '20080421_111500_bbcone_bargain_hunt', '20080421_113000_bbctwo_working_lunch', '20080421_120000_bbcone_bbc_news',
  '20080421_120000_bbctwo_schools_science_clips_investigates', '20080421_121000_bbctwo_schools_science_clips_investigates',
  '20080421_122000_bbctwo_schools_primary_geography', '20080421_123000_bbctwo_snooker_world_championship', '20080421_124500_bbcone_doctors',
  '20080421_140500_bbcone_space_pirates', '20080421_143500_bbcone_chucklevision', '20080421_151000_bbcone_roar', '20080421_153500_bbcone_grange_hill',
  '20080421_160000_bbcone_newsround', '20080421_161500_bbcone_the_weakest_link', '20080421_170000_bbctwo_eggheads', '20080421_173000_bbcone_bbc_london_news',
  '20080421_180000_bbcfour_world_news_today', '20080421_180000_bbcone_the_one_show', '20080421_180000_bbcthree_dragons_den',
  '20080421_180000_bbctwo_snooker_world_championship', '20080421_183000_bbcfour_abroad_again_in_britain', '20080421_183000_bbcone_watchdog',
  '20080421_190000_bbcone_eastenders', '20080421_190000_bbctwo_university_challenge_the_professionals', '20080421_193000_bbcfour_the_book_quiz',
  '20080421_193000_bbcone_panorama', '20080421_193000_bbcthree_glamour_girls', '20080421_193000_bbctwo_the_hairy_bikers_come_home',
  '20080421_200000_bbcfour_how_to_build_a_cathedral', '20080421_200000_bbcone_waking_the_dead', '20080421_200000_bbcthree_natalie_cassidy_s_diet_secrets',
  '20080421_210000_bbcone_bbc_news_at_ten', '20080421_210000_bbcthree_eastenders', '20080421_213000_bbcthree_gavin_and_stacey', '20080421_213000_bbctwo_newsnight',
  '20080421_213500_bbcone_meet_the_immigrants', '20080421_220000_bbcthree_pulling', '20080421_220500_bbcone_inside_sport', '20080421_222000_bbctwo_a_study_in_sherlock',
  '20080421_225000_bbcfour_britain_s_best_buildings', '20080421_230000_bbctwo_snooker_world_championship_highlights', '20080421_231500_bbcthree_glamour_girls',
  '20080421_234000_bbcfour_how_to_build_a_cathedral', '20080421_234500_bbcthree_natalie_cassidy_s_diet_secrets', '20080421_235000_bbctwo_snooker_extra',
  '20080422_004000_bbcfour_abroad_again_in_britain', '20080422_004000_bbcthree_gavin_and_stacey', '20080422_004500_bbcone_weatherview', '20080422_005000_bbcone_clowns',
  '20080422_011000_bbcthree_pulling', '20080422_014000_bbcfour_the_book_quiz', '20080422_021000_bbcfour_how_to_build_a_cathedral',
  '20080422_025000_bbcone_to_buy_or_not_to_buy', '20080422_030000_bbctwo_gcse_bitesize', '20080422_031000_bbcthree_dog_borstal', '20080422_050000_bbcone_breakfast',
  '20080422_050000_bbctwo_tikkabilla', '20080422_062500_bbctwo_newsround', '20080422_073000_bbctwo_jackanory_junior', '20080422_080000_bbctwo_boogie_beebies',
  '20080422_081500_bbcone_missing', '20080422_084000_bbctwo_something_special', '20080422_090000_bbcone_homes_under_the_hammer',
  '20080422_093000_bbctwo_schools_fun_with_phonics', '20080422_094000_bbctwo_schools_fun_with_phonics', '20080422_100000_bbcone_to_buy_or_not_to_buy',
  '20080422_101000_bbctwo_timewatch', '20080422_110000_bbctwo_the_daily_politics', '20080422_111500_bbcone_bargain_hunt', '20080422_113000_bbctwo_working_lunch',
  '20080422_120000_bbcone_bbc_news_at_one', '20080422_120000_bbctwo_schools_the_maths_channel', '20080422_121000_bbctwo_schools_primary_geography',
  '20080422_123000_bbctwo_snooker_world_championship', '20080422_124500_bbcone_doctors', '20080422_140500_bbcone_space_pirates', '20080422_143500_bbcone_chucklevision',
  '20080422_153500_bbcone_blue_peter', '20080422_160000_bbcone_newsround', '20080422_161500_bbcone_the_weakest_link', '20080422_170000_bbcone_bbc_news_at_six',
  '20080422_170000_bbctwo_eggheads', '20080422_173000_bbcone_bbc_london_news', '20080422_180000_bbcfour_world_news_today',
  '20080422_180000_bbctwo_snooker_world_championship', '20080422_183000_bbcfour_pop_goes_the_sixties', '20080422_183000_bbcone_eastenders',
  '20080422_190000_bbcfour_life_in_cold_blood', '20080422_190000_bbcone_holby_city', '20080422_190000_bbctwo_university_challenge_the_professionals',
  '20080422_193000_bbctwo_the_hairy_bikers_come_home', '20080422_200000_bbcfour_chinese_school', '20080422_200000_bbcone_waking_the_dead',
  '20080422_200000_bbctwo_age_of_terror', '20080422_210000_bbcfour_goodness_gracious_me', '20080422_210000_bbcone_bbc_news_at_ten', '20080422_210000_bbcthree_eastenders',
  '20080422_210000_bbctwo_later_live_with_jools_holland', '20080422_213000_bbcthree_little_britain', '20080422_213000_bbctwo_newsnight',
  '20080422_213500_bbcone_ex_forces_and_homeless', '20080422_220000_bbcthree_the_wall', '20080422_225000_bbcfour_chinese_school',
  '20080422_231000_bbctwo_snooker_world_championship_highlights', '20080422_235000_bbcfour_proms_on_four', '20080422_235000_bbcone_weatherview',
  '20080422_235500_bbcone_see_hear', '20080423_000000_bbctwo_snooker_extra', '20080423_002500_bbcone_tiger_spy_in_the_jungle', '20080423_003000_bbcthree_dawn_gets_naked',
  '20080423_012500_bbcone_to_buy_or_not_to_buy', '20080423_012500_bbcthree_natalie_cassidy_s_diet_secrets', '20080423_014500_bbcfour_chinese_school',
  '20080423_021000_bbcone_to_buy_or_not_to_buy', '20080423_030000_bbctwo_gcse_bitesize_revision', '20080423_050000_bbcone_breakfast', '20080423_050000_bbctwo_tikkabilla',
  '20080423_062500_bbctwo_newsround', '20080423_063000_bbctwo_hider_in_the_house', '20080423_073000_bbctwo_jackanory_junior', '20080423_080000_bbctwo_boogie_beebies',
  '20080423_081500_bbcone_missing_live', '20080423_084000_bbctwo_something_special', '20080423_090000_bbcone_homes_under_the_hammer',
  '20080423_093000_bbctwo_what_the_ancients_did_for_us', '20080423_100000_bbcone_to_buy_or_not_to_buy', '20080423_103000_bbctwo_the_daily_politics',
  '20080423_104500_bbcone_cash_in_the_attic', '20080423_111500_bbcone_bargain_hunt', '20080423_120000_bbcone_bbc_news_at_one', '20080423_120000_bbctwo_stand_by_your_man',
  '20080423_123000_bbctwo_working_lunch', '20080423_124500_bbcone_doctors', '20080423_130000_bbctwo_lifeline', '20080423_131000_bbctwo_snooker_world_championship',
  '20080423_140500_bbcone_space_pirates', '20080423_143500_bbcone_chucklevision', '20080423_151000_bbcone_young_dracula', '20080423_153500_bbcone_blue_peter',
  '20080423_160000_bbcone_newsround', '20080423_161500_bbcone_the_weakest_link', '20080423_170000_bbcone_bbc_news_at_six', '20080423_170000_bbctwo_eggheads',
  '20080423_173000_bbcone_bbc_london_news', '20080423_180000_bbcfour_world_news_today', '20080423_180000_bbcone_the_one_show', '20080423_180000_bbcthree_holby_city',
  '20080423_180000_bbctwo_snooker_world_championship', '20080423_183000_bbcfour_pop_goes_the_sixties', '20080423_183000_bbcone_street_doctor',
  '20080423_190000_bbcfour_a_history_of_britain_by_simon_schama', '20080423_190000_bbcone_traffic_cops', '20080423_190000_bbcthree_glamour_girls',
  '20080423_190000_bbctwo_natural_world', '20080423_195000_bbctwo_watching_the_wild', '20080423_200000_bbcone_the_apprentice',
  '20080423_200000_bbcthree_amy_my_body_for_bucks', '20080423_200000_bbctwo_dan_cruickshank_s_adventures', '20080423_210000_bbcone_bbc_news_at_ten',
  '20080423_210000_bbctwo_the_apprentice_you_re_fired', '20080423_214000_bbcone_made_in_england', '20080423_221000_bbcone_aliyah_the_journey_home',
  '20080423_222000_bbctwo_desi_dna', '20080423_225000_bbctwo_snooker_world_championship_highlights', '20080423_225500_bbcfour_the_last_duel',
  '20080423_232000_bbcthree_the_wall', '20080424_003000_bbcone_weatherview', '20080424_003500_bbcone_seaside_rescue', '20080424_003500_bbcthree_glamour_girls',
  '20080424_010500_bbcfour_how_to_build_a_cathedral', '20080424_010500_bbcone_meet_the_immigrants', '20080424_010500_bbcthree_amy_my_body_for_bucks',
  '20080424_020500_bbcfour_the_last_duel', '20080424_020500_bbcone_to_buy_or_not_to_buy', '20080424_023000_bbcthree_the_wall',
  '20080424_030000_bbctwo_gcse_bitesize_revision', '20080424_031500_bbcthree_dog_borstal', '20080424_050000_bbcone_breakfast', '20080424_050000_bbctwo_tikkabilla',
  '20080424_062500_bbctwo_newsround', '20080424_063000_bbctwo_hider_in_the_house', '20080424_073000_bbctwo_jackanory_junior', '20080424_080000_bbctwo_boogie_beebies',
  '20080424_084000_bbctwo_something_special', '20080424_093000_bbctwo_schools_primary_history', '20080424_095000_bbctwo_schools_megamaths',
  '20080424_100000_bbcone_to_buy_or_not_to_buy', '20080424_100500_bbctwo_schools_last_chance_to_see', '20080424_101000_bbctwo_schools_primary_geography',
  '20080424_103000_bbctwo_schools_key_stage_1_science_clips', '20080424_104000_bbctwo_schools_key_stage_1_science_clips', '20080424_104500_bbcone_cash_in_the_attic',
  '20080424_105000_bbctwo_schools_hands_up', '20080424_110000_bbctwo_the_daily_politics', '20080424_111500_bbcone_bargain_hunt', '20080424_113000_bbctwo_working_lunch',
  '20080424_120000_bbcone_bbc_news_at_one', '20080424_120000_bbctwo_open_gardens', '20080424_123000_bbctwo_snooker_world_championship', '20080424_124500_bbcone_doctors',
  '20080424_140500_bbcone_space_pirates', '20080424_143500_bbcone_chucklevision', '20080424_150500_bbcone_stake_out', '20080424_153500_bbcone_beat_the_boss',
  '20080424_160000_bbcone_newsround', '20080424_161500_bbcone_the_weakest_link', '20080424_170000_bbcone_bbc_news_at_six', '20080424_170000_bbctwo_eggheads',
  '20080424_173000_bbcone_bbc_london_news', '20080424_180000_bbcfour_world_news_today', '20080424_180000_bbcone_the_one_show',
  '20080424_180000_bbctwo_snooker_world_championship', '20080424_183000_bbcone_eastenders', '20080424_190000_bbcfour_the_voices_of_morebath',
  '20080424_190000_bbcone_holby_blue', '20080424_190000_bbctwo_top_gear', '20080424_191000_bbcfour_a_history_of_british_art_dreams_and',
  '20080424_200000_bbcfour_inside_the_medieval_mind', '20080424_200000_bbcthree_child_stars', '20080424_205000_bbctwo_heroes_unmasked', '20080424_210000_bbcfour_crusades',
  '20080424_210000_bbcone_bbc_news_at_ten', '20080424_210000_bbcthree_eastenders', '20080424_213000_bbctwo_newsnight', '20080424_222000_bbctwo_ideal',
  '20080424_223500_bbcfour_chinese_school', '20080424_224500_bbcone_this_week', '20080424_225000_bbctwo_snooker_world_championship_highlights',
  '20080424_230000_bbcthree_pulling', '20080424_233000_bbcone_weatherview', '20080424_233000_bbcthree_child_stars', '20080424_233500_bbcfour_in_search_of_medieval_britain',
  '20080424_233500_bbcone_panorama', '20080424_234000_bbctwo_snooker_extra', '20080425_000500_bbcfour_inside_the_medieval_mind',
  '20080425_000500_bbcone_johnny_s_new_kingdom', '20080425_003500_bbcone_the_twenties_in_colour_the_wonderful', '20080425_010500_bbcfour_the_book_quiz',
  '20080425_010500_bbcone_bill_oddie_s_wild_side', '20080425_013500_bbcfour_in_search_of_medieval_britain', '20080425_013500_bbcone_to_buy_or_not_to_buy',
  '20080425_015500_bbcthree_pulling', '20080425_020500_bbcfour_inside_the_medieval_mind', '20080425_030000_bbctwo_gcse_bitesize_revision',
  '20080425_050000_bbcone_breakfast', '20080425_050000_bbctwo_tikkabilla', '20080425_062500_bbctwo_newsround', '20080425_073000_bbctwo_jackanory_junior',
  '20080425_080000_bbctwo_boogie_beebies', '20080425_081500_bbcone_missing_live', '20080425_084000_bbctwo_something_special',
  '20080425_090000_bbcone_homes_under_the_hammer', '20080425_100000_bbcone_to_buy_or_not_to_buy', '20080425_100000_bbctwo_schools_the_way_things_work',
  '20080425_103000_bbctwo_schools_watch', '20080425_104500_bbcone_cash_in_the_attic', '20080425_104500_bbctwo_schools_something_special',
  '20080425_110000_bbctwo_the_daily_politics', '20080425_111500_bbcone_bargain_hunt', '20080425_113000_bbctwo_working_lunch',
  '20080425_123000_bbctwo_snooker_world_championship', '20080425_124500_bbcone_doctors', '20080425_140500_bbcone_space_pirates', '20080425_143500_bbcone_chucklevision',
  '20080425_153500_bbcone_the_slammer', '20080425_160000_bbcone_newsround', '20080425_161500_bbcone_the_weakest_link', '20080425_170000_bbcone_bbc_news_at_six',
  '20080425_170000_bbctwo_eggheads', '20080425_173000_bbcone_bbc_london_news', '20080425_180000_bbcfour_world_news_today', '20080425_180000_bbcone_the_one_show',
  '20080425_180000_bbcthree_top_gear', '20080425_180000_bbctwo_snooker_world_championship', '20080425_183000_bbcfour_swan_lake', '20080425_190000_bbcone_eastenders',
  '20080425_190000_bbcthree_almost_famous', '20080425_190000_bbctwo_gardeners_world', '20080425_193000_bbcone_a_question_of_sport', '20080425_200000_bbcfour_soul_britannia',
  '20080425_200000_bbcthree_doctor_who', '20080425_210000_bbcfour_bbc_four_sessions_van_morrison', '20080425_210000_bbcone_bbc_news_at_ten',
  '20080425_210000_bbcthree_eastenders', '20080425_213000_bbctwo_newsnight', '20080425_220000_bbcfour_van_morrison_on_later', '20080425_220000_bbctwo_newsnight_review',
  '20080425_223500_bbctwo_later_with_jools_holland', '20080425_230500_bbcthree_gavin_and_stacey', '20080425_233500_bbcthree_almost_famous',
  '20080425_233500_bbctwo_snooker_world_championship_highlights', '20080426_001000_bbcfour_bbc_four_sessions_van_morrison', '20080426_002000_bbcone_weatherview',
  '20080426_002500_bbcone_dan_cruickshank_s_adventures', '20080426_002500_bbctwo_snooker_extra', '20080426_003500_bbcthree_child_stars',
  '20080426_011000_bbcfour_soul_britannia', '20080426_012500_bbcone_natural_world', '20080426_021000_bbcfour_van_morrison_on_later',
  '20080426_023000_bbcthree_gavin_and_stacey', '20080426_052000_bbctwo_tikkabilla', '20080426_070000_bbctwo_sorcerer_s_apprentice',
  '20080426_090000_bbcone_saturday_kitchen', '20080426_090000_bbctwo_hedz', '20080426_093000_bbctwo_the_slammer', '20080426_104500_bbctwo_sportsround',
  '20080426_110000_bbcone_bbc_news', '20080426_111000_bbcone_football_focus', '20080426_120000_bbcone_snooker_world_championship', '20080426_123000_bbctwo_the_surgery',
  '20080426_125000_bbctwo_sound', '20080426_153000_bbcone_final_score', '20080426_154500_bbctwo_snooker_world_championship', '20080426_162000_bbcone_bbc_news',
  '20080426_163000_bbcone_bbc_london_news', '20080426_164000_bbcone_the_kids_are_all_right', '20080426_172000_bbcone_doctor_who', '20080426_173000_bbctwo_dad_s_army',
  '20080426_180000_bbcthree_two_pints_of_lager_outtakes', '20080426_180000_bbctwo_snooker_world_championship', '20080426_180500_bbcone_i_d_do_anything',
  '20080426_180500_bbcthree_doctor_who_confidential', '20080426_181000_bbcfour_in_search_of_medieval_britain', '20080426_184000_bbcfour_the_book_quiz',
  '20080426_185000_bbcthree_top_gear', '20080426_191000_bbcfour_a_perfect_spy', '20080426_195500_bbcone_casualty', '20080426_200500_bbcfour_a_perfect_spy',
  '20080426_200500_bbctwo_have_i_got_a_bit_more_news_for_you', '20080426_204500_bbcone_love_soup', '20080426_204500_bbctwo_comedy_map_of_britain',
  '20080426_211500_bbcone_bbc_news', '20080426_213000_bbcone_match_of_the_day', '20080426_214500_bbctwo_the_apprentice', '20080426_220000_bbcthree_gavin_and_stacey',
  '20080426_224500_bbctwo_last_man_standing', '20080426_231500_bbcthree_glamour_girls', '20080426_234000_bbctwo_snooker_world_championship_highlights',
  '20080426_234500_bbcthree_child_stars', '20080427_001500_bbcfour_selling_power_the_admen_and_no_10', '20080427_003000_bbctwo_snooker_extra',
  '20080427_004500_bbcthree_gavin_and_stacey', '20080427_011500_bbcfour_in_search_of_medieval_britain', '20080427_011500_bbcone_weatherview',
  '20080427_011500_bbcthree_the_wall', '20080427_014500_bbcfour_the_book_quiz', '20080427_020000_bbcthree_doctor_who_confidential',
  '20080427_021500_bbcfour_selling_power_the_admen_and_no_10', '20080427_024500_bbcthree_child_stars', '20080427_050000_bbcone_breakfast',
  '20080427_052000_bbctwo_tikkabilla', '20080427_063500_bbcone_match_of_the_day', '20080427_070000_bbctwo_trapped', '20080427_073000_bbctwo_raven_the_secret_temple',
  '20080427_080000_bbcone_the_andrew_marr_show', '20080427_080000_bbctwo_hider_in_the_house', '20080427_090000_bbcone_sunday_life', '20080427_100000_bbcone_countryfile',
  '20080427_110000_bbcone_the_politics_show', '20080427_110000_bbctwo_boxing', '20080427_120000_bbcone_eastenders', '20080427_120000_bbctwo_snooker_world_championship',
  '20080427_154500_bbcone_keeping_up_appearances', '20080427_161500_bbcone_points_of_view', '20080427_163000_bbcone_songs_of_praise',
  '20080427_163000_bbctwo_lemurs_of_madagascar', '20080427_170500_bbcone_bbc_news', '20080427_171000_bbctwo_natural_world', '20080427_172000_bbcone_bbc_london_news',
  '20080427_173000_bbcone_seaside_rescue', '20080427_180000_bbcfour_inside_the_medieval_mind', '20080427_180000_bbcthree_sound',
  '20080427_180000_bbctwo_snooker_world_championship', '20080427_183000_bbcone_i_d_do_anything_results', '20080427_190000_bbcfour_how_to_build_a_cathedral',
  '20080427_190000_bbcone_miss_austen_regrets', '20080427_190000_bbcthree_doctor_who', '20080427_194500_bbcthree_doctor_who_confidential',
  '20080427_200000_bbcthree_pulling', '20080427_201000_bbcfour_washes_whiter', '20080427_203000_bbcone_the_vicar_of_dibley', '20080427_203000_bbcthree_little_britain',
  '20080427_205000_bbctwo_coast', '20080427_210000_bbcone_bbc_news_at_ten', '20080427_210000_bbctwo_match_of_the_day_2', '20080427_212000_bbcone_panorama',
  '20080427_215000_bbcfour_bbc_four_session_van_morrison', '20080427_215000_bbctwo_graham_norton_uncut', '20080427_222000_bbcone_celine_dion_in_las_vegas_a_new_day',
  '20080427_224500_bbcthree_pulling', '20080427_225000_bbcfour_van_morrison_at_the_bbc', '20080427_231500_bbcthree_page_three_teens',
  '20080427_232500_bbctwo_snooker_world_championship_highlights', '20080427_235000_bbcfour_soul_britannia', '20080428_001500_bbctwo_snooker_extra',
  '20080428_004500_bbcone_weatherview', '20080428_004500_bbcthree_natalie_cassidy_s_diet_secrets', '20080428_005000_bbcfour_how_to_build_a_cathedral',
  '20080428_005000_bbcone_watchdog', '20080428_012000_bbcone_holby_city', '20080428_015000_bbcfour_inside_the_medieval_mind', '20080428_022000_bbcone_age_of_terror',
  '20080428_023000_bbctwo_inside_sport', '20080428_024000_bbcthree_dog_borstal', '20080428_031000_bbcthree_doctor_who_confidential', '20080428_050000_bbcone_breakfast',
  '20080428_050000_bbctwo_tikkabilla', '20080428_062500_bbctwo_newsround', '20080428_063000_bbctwo_hider_in_the_house', '20080428_073000_bbctwo_jackanory_junior',
  '20080428_080000_bbctwo_boogie_beebies', '20080428_081500_bbcone_missing_live', '20080428_084000_bbctwo_something_special', '20080428_093000_bbctwo_schools_focus',
  '20080428_095000_bbctwo_schools_look_and_read', '20080428_100000_bbcone_to_buy_or_not_to_buy', '20080428_101000_bbctwo_schools_razzledazzle',
  '20080428_103000_bbctwo_schools_barnaby_bear', '20080428_110000_bbctwo_the_daily_politics', '20080428_111500_bbcone_bargain_hunt', '20080428_113000_bbctwo_working_lunch',
  '20080428_120000_bbcone_bbc_news_at_one', '20080428_120000_bbctwo_science_clips_investigates_habitats', '20080428_121000_bbctwo_science_clips_investigates_friction',
  '20080428_122000_bbctwo_kenya_shorts_kenya_and_world', '20080428_123000_bbctwo_snooker_world_championship', '20080428_124500_bbcone_doctors',
  '20080428_140500_bbcone_space_pirates', '20080428_143500_bbcone_chucklevision', '20080428_151000_bbcone_roar', '20080428_153500_bbcone_grange_hill',
  '20080428_160000_bbcone_newsround', '20080428_161500_bbcone_the_weakest_link', '20080428_170000_bbcone_bbc_news_at_six', '20080428_170000_bbctwo_eggheads',
  '20080428_173000_bbcone_bbc_london_news', '20080428_180000_bbcfour_world_news_today', '20080428_180000_bbcthree_dragons_den',
  '20080428_180000_bbctwo_snooker_world_championship', '20080428_183000_bbcfour_art_of_eternity', '20080428_183000_bbcone_watchdog', '20080428_190000_bbcone_eastenders',
  '20080428_193000_bbcfour_the_book_quiz', '20080428_193000_bbcone_panorama', '20080428_193000_bbcthree_glamour_girls',
  '20080428_200000_bbcfour_a_history_of_britain_by_simon_schama', '20080428_200000_bbcone_waking_the_dead', '20080428_210000_bbcthree_eastenders',
  '20080428_213000_bbcthree_ideal', '20080428_213000_bbctwo_newsnight', '20080428_213500_bbcone_meet_the_immigrants', '20080428_220000_bbcthree_pulling',
  '20080428_221000_bbcfour_the_hour_of_the_pig', '20080428_222000_bbctwo_snooker_world_championship_highlights', '20080428_231500_bbcthree_glamour_girls',
  '20080429_000000_bbcfour_the_book_quiz', '20080429_003500_bbcone_springwatch_weatherview', '20080429_004500_bbcthree_ideal', '20080429_011500_bbcthree_pulling',
  '20080429_014000_bbcfour_the_book_quiz', '20080429_021000_bbcfour_art_of_eternity', '20080429_021000_bbcone_to_buy_or_not_to_buy',
  '20080429_030000_bbctwo_gcse_bitesize_revision', '20080429_031500_bbcthree_dog_borstal', '20080429_050000_bbcone_breakfast', '20080429_050000_bbctwo_tikkabilla',
  '20080429_063000_bbctwo_hider_in_the_house', '20080429_073000_bbctwo_jackanory_junior', '20080429_080000_bbctwo_boogie_beebies', '20080429_081500_bbcone_missing_live',
  '20080429_084000_bbctwo_something_special', '20080429_093000_bbctwo_schools_let_s_write_poetry', '20080429_100000_bbcone_to_buy_or_not_to_buy',
  '20080429_104500_bbcone_cash_in_the_attic', '20080429_110000_bbctwo_the_daily_politics', '20080429_111500_bbcone_bargain_hunt', '20080429_113000_bbctwo_working_lunch',
  '20080429_120000_bbcone_bbc_news_at_one', '20080429_120000_bbctwo_schools_maths_channel', '20080429_121000_bbctwo_schools_primary_geography',
  '20080429_123000_bbctwo_snooker_world_championship', '20080429_124000_bbcone_doctors', '20080429_143500_bbcone_chucklevision', '20080429_153500_bbcone_blue_peter',
  '20080429_160000_bbcone_newsround', '20080429_161500_bbcone_the_weakest_link', '20080429_170000_bbcone_bbc_news_at_six', '20080429_170000_bbctwo_eggheads',
  '20080429_173000_bbcone_bbc_london_news', '20080429_173000_bbctwo_great_british_menu', '20080429_175500_bbcone_party_election_broadcast',
  '20080429_180000_bbcfour_world_news_today', '20080429_180000_bbcone_the_one_show', '20080429_180000_bbcthree_dog_borstal', '20080429_183000_bbcfour_pop_goes_the_sixties',
  '20080429_183000_bbcone_eastenders', '20080429_190000_bbcfour_life_in_cold_blood', '20080429_190000_bbcone_holby_city',
  '20080429_190000_bbcthree_natalie_cassidy_s_diet_secrets', '20080429_200000_bbcfour_chinese_school', '20080429_200000_bbcone_waking_the_dead',
  '20080429_200000_bbctwo_age_of_terror', '20080429_210000_bbcfour_goodness_gracious_me', '20080429_210000_bbcone_bbc_news_at_ten', '20080429_210000_bbcthree_eastenders',
  '20080429_210000_bbctwo_later_live_with_jools_holland', '20080429_213000_bbcthree_scallywagga', '20080429_213000_bbctwo_newsnight', '20080429_220000_bbcthree_the_wall',
  '20080429_224000_bbcfour_chinese_school', '20080429_231000_bbctwo_snooker_world_championship_highlights', '20080429_232500_bbcone_weatherview',
  '20080429_233000_bbcone_stand_by_your_man', '20080429_234000_bbcfour_proms_on_four', '20080430_000000_bbcone_tiger_spy_in_the_jungle',
  '20080430_000000_bbctwo_snooker_extra', '20080430_003000_bbcthree_scallywagga', '20080430_005500_bbcthree_the_wall', '20080430_013000_bbcone_to_buy_or_not_to_buy',
  '20080430_014000_bbcthree_natalie_cassidy_s_diet_secrets', '20080430_015500_bbcfour_chinese_school', '20080430_021500_bbcone_to_buy_or_not_to_buy',
  '20080430_030000_bbctwo_gcse_bitesize', '20080430_050000_bbcone_breakfast', '20080430_050000_bbctwo_tikkabilla', '20080430_063000_bbctwo_hider_in_the_house',
  '20080430_073000_bbctwo_jackanory_junior', '20080430_080000_bbctwo_boogie_beebies', '20080430_081500_bbcone_missing_live', '20080430_084000_bbctwo_something_special',
  '20080430_093000_bbctwo_a_picture_of_britain', '20080430_100000_bbcone_to_buy_or_not_to_buy', '20080430_103000_bbctwo_the_daily_politics',
  '20080430_104500_bbcone_cash_in_the_attic', '20080430_111500_bbcone_bargain_hunt', '20080430_120000_bbcone_bbc_news_at_one', '20080430_123000_bbcone_bbc_london_news',
  '20080430_123000_bbctwo_working_lunch', '20080430_124000_bbcone_doctors', '20080430_130000_bbctwo_snooker_world_championship', '20080430_140500_bbcone_space_pirates',
  '20080430_143500_bbcone_chucklevision', '20080430_151000_bbcone_young_dracula', '20080430_153500_bbcone_blue_peter', '20080430_160000_bbcone_newsround',
  '20080430_161500_bbcone_the_weakest_link', '20080430_170000_bbcone_bbc_news_at_six', '20080430_170000_bbctwo_eggheads', '20080430_173000_bbcone_bbc_london_news',
  '20080430_180000_bbcfour_world_news_today', '20080430_180000_bbcone_the_one_show', '20080430_180000_bbcthree_holby_city',
  '20080430_180000_bbctwo_snooker_world_championship', '20080430_183000_bbcfour_pop_goes_the_sixties', '20080430_183000_bbcone_street_doctor',
  '20080430_190000_bbcfour_life_in_cold_blood', '20080430_190000_bbcone_traffic_cops', '20080430_190000_bbcthree_glamour_girls',
  '20080430_200000_bbcfour_a_history_of_britain_by_simon_schama', '20080430_200000_bbcone_the_apprentice', '20080430_200000_bbcthree_two_pints_of_lager_and',
  '20080430_203000_bbcthree_ideal', '20080430_210000_bbcone_bbc_news_at_ten', '20080430_210000_bbctwo_the_apprentice_you_re_fired', '20080430_213000_bbctwo_newsnight',
  '20080430_222000_bbctwo_snooker_world_championship_highlights', '20080430_231000_bbctwo_snooker_extra', '20080430_233500_bbcthree_two_pints_of_lager_and',
  '20080430_235000_bbcfour_selling_the_sixties', '20080430_235000_bbcone_weatherview', '20080430_235500_bbcone_seaside_rescue', '20080501_000500_bbcthree_ideal',
  '20080501_002500_bbcone_meet_the_immigrants', '20080501_005000_bbcfour_david_ogilvy_original_mad_man', '20080501_010500_bbcthree_the_wall',
  '20080501_015000_bbcthree_glamour_girls', '20080501_015500_bbcone_to_buy_or_not_to_buy', '20080501_022000_bbcthree_page_three_teens',
  '20080501_030000_bbctwo_gcse_bitesize', '20080501_050000_bbcone_breakfast', '20080501_050000_bbctwo_tikkabilla', '20080501_063000_bbctwo_hider_in_the_house',
  '20080501_073000_bbctwo_jackanory_junior', '20080501_080000_bbctwo_boogie_beebies', '20080501_081500_bbcone_missing_live', '20080501_084000_bbctwo_something_special',
  '20080501_093000_bbctwo_schools_primary_history', '20080501_095000_bbctwo_schools_megamaths', '20080501_100000_bbcone_to_buy_or_not_to_buy',
  '20080501_101000_bbctwo_schools_primary_geography', '20080501_103000_bbctwo_schools_ks1_science_clips', '20080501_104000_bbctwo_schools_ks1_science_clips',
  '20080501_104500_bbcone_cash_in_the_attic', '20080501_105000_bbctwo_schools_hands_up', '20080501_110000_bbctwo_the_daily_politics', '20080501_111500_bbcone_bargain_hunt',
  '20080501_113000_bbctwo_working_lunch', '20080501_120000_bbcone_bbc_news_at_one', '20080501_123000_bbctwo_snooker_world_championship', '20080501_124000_bbcone_doctors',
  '20080501_140500_bbcone_space_pirates', '20080501_143500_bbcone_chucklevision', '20080501_150500_bbcone_stake_out', '20080501_153500_bbcone_beat_the_boss',
  '20080501_160000_bbcone_newsround', '20080501_161500_bbcone_the_weakest_link', '20080501_170000_bbcone_bbc_news_at_six', '20080501_170000_bbctwo_eggheads',
  '20080501_173000_bbcone_bbc_london_news', '20080501_180000_bbcone_the_one_show', '20080501_180000_bbctwo_snooker_world_championship',
  '20080501_183000_bbcfour_in_search_of_medieval_britain', '20080501_190000_bbcfour_sacred_music', '20080501_190000_bbcone_holby_blue',
  '20080501_200000_bbcfour_inside_the_medieval_mind', '20080501_200000_bbcone_the_invisibles', '20080501_200000_bbcthree_should_i_smoke_dope',
  '20080501_204500_bbctwo_heroes_unmasked', '20080501_210000_bbcfour_crusades', '20080501_210000_bbcone_bbc_news_at_ten', '20080501_210000_bbcthree_eastenders',
  '20080501_213000_bbctwo_newsnight', '20080501_222000_bbctwo_snooker_world_championship_highlights', '20080501_223500_bbcone_election_night_2008',
  '20080501_224000_bbcfour_chinese_school', '20080501_225500_bbcthree_pulling', '20080501_231000_bbctwo_panorama', '20080501_232500_bbcthree_should_i_smoke_dope',
  '20080501_234000_bbcfour_in_search_of_medieval_britain', '20080502_001000_bbctwo_the_twenties_in_colour_the_wonderful', '20080502_004000_bbctwo_bill_oddie_s_wild_side',
  '20080502_011000_bbcfour_the_book_quiz', '20080502_012000_bbcthree_pulling', '20080502_014000_bbcfour_in_search_of_medieval_britain',
  '20080502_014000_bbctwo_to_buy_or_not_to_buy', '20080502_021000_bbcfour_inside_the_medieval_mind', '20080502_030000_bbctwo_gcse_bitesize',
  '20080502_050000_bbctwo_tikkabilla', '20080502_062500_bbctwo_newsround', '20080502_063000_bbctwo_hider_in_the_house', '20080502_080000_bbctwo_boogie_beebies',
  '20080502_081500_bbcone_missing_live', '20080502_084000_bbctwo_something_special', '20080502_094500_bbctwo_schools_the_way_things_work',
  '20080502_100000_bbcone_to_buy_or_not_to_buy', '20080502_100000_bbctwo_schools_the_way_things_work', '20080502_103000_bbctwo_schools_watch',
  '20080502_104500_bbcone_cash_in_the_attic', '20080502_104500_bbctwo_schools_something_special', '20080502_110000_bbctwo_the_daily_politics',
  '20080502_111500_bbcone_bargain_hunt', '20080502_113000_bbctwo_working_lunch', '20080502_120000_bbcone_bbc_news_at_one',
  '20080502_123000_bbctwo_snooker_world_championship', '20080502_124000_bbcone_doctors', '20080502_140500_bbcone_space_pirates', '20080502_143500_bbcone_chucklevision',
  '20080502_153500_bbcone_the_slammer', '20080502_160000_bbcone_newsround', '20080502_161500_bbcone_the_weakest_link', '20080502_170000_bbcone_bbc_news_at_six',
  '20080502_170000_bbctwo_eggheads', '20080502_173000_bbcone_bbc_london_news', '20080502_180000_bbcfour_world_news_today', '20080502_180000_bbcthree_top_gear',
  '20080502_180000_bbctwo_snooker_world_championship', '20080502_183000_bbcfour_transatlantic_sessions', '20080502_183000_bbcone_inside_out',
  '20080502_190000_bbcfour_darcey_bussell_s_ten_best_ballet_moments', '20080502_190000_bbcone_eastenders', '20080502_190000_bbcthree_last_man_standing',
  '20080502_193000_bbcone_a_question_of_sport', '20080502_200000_bbcfour_james_taylor_one_man_band', '20080502_200000_bbcthree_doctor_who',
  '20080502_200000_bbctwo_living_the_dream_revisited', '20080502_204500_bbcthree_doctor_who_confidential', '20080502_210000_bbcone_bbc_news_at_ten',
  '20080502_210500_bbcfour_hotel_california_from_the_byrds_to', '20080502_213000_bbctwo_newsnight', '20080502_220000_bbcthree_two_pints_of_lager_and',
  '20080502_221000_bbctwo_newsnight_review', '20080502_224000_bbcfour_in_concert_james_taylor', '20080502_224000_bbctwo_later_with_jools_holland',
  '20080502_234000_bbctwo_snooker_world_championship_highlights', '20080502_234500_bbcthree_ideal', '20080503_001000_bbcfour_james_taylor_one_man_band',
  '20080503_001500_bbcthree_two_pints_of_lager_and', '20080503_002500_bbcone_weatherview', '20080503_003000_bbcone_dan_cruickshank_s_adventures',
  '20080503_004500_bbcthree_two_pints_of_lager_and', '20080503_011000_bbcfour_darcey_bussell_s_ten_best_ballet_moments', '20080503_011500_bbcthree_glamour_girls',
  '20080503_013000_bbcone_natural_world', '20080503_014500_bbcthree_scallywagga', '20080503_021000_bbcfour_transatlantic_sessions', '20080503_021000_bbcthree_ideal',
  '20080503_050000_bbcone_breakfast', '20080503_050000_bbctwo_fimbles', '20080503_052000_bbctwo_tikkabilla', '20080503_070000_bbctwo_sorcerer_s_apprentice',
  '20080503_090000_bbcone_saturday_kitchen', '20080503_090000_bbctwo_hedz', '20080503_093000_bbctwo_the_slammer', '20080503_104500_bbctwo_sportsround',
  '20080503_110000_bbcone_bbc_news', '20080503_111000_bbcone_football_focus', '20080503_120000_bbcone_snooker_world_championship', '20080503_125000_bbctwo_sound',
  '20080503_153000_bbctwo_flog_it', '20080503_162000_bbcone_bbc_news', '20080503_163000_bbcone_bbc_london_news', '20080503_163000_bbctwo_coast',
  '20080503_164000_bbcone_the_kids_are_all_right', '20080503_172000_bbcone_doctor_who', '20080503_173000_bbctwo_dad_s_army',
  '20080503_180000_bbcfour_meetings_with_remarkable_trees', '20080503_180000_bbcthree_three_s_outtakes', '20080503_180000_bbctwo_snooker_world_championship',
  '20080503_180500_bbcone_i_d_do_anything', '20080503_180500_bbcthree_doctor_who_confidential', '20080503_181000_bbcfour_in_search_of_medieval_britain',
  '20080503_190500_bbcthree_top_gear', '20080503_191000_bbcfour_a_perfect_spy', '20080503_195500_bbcone_casualty', '20080503_200500_bbcfour_a_perfect_spy',
  '20080503_204500_bbcone_love_soup', '20080503_210000_bbctwo_have_i_got_a_bit_more_news_for_you', '20080503_211500_bbcone_bbc_news',
  '20080503_213000_bbcone_match_of_the_day', '20080503_214500_bbctwo_comedy_map_of_britain', '20080503_215500_bbcthree_scallywagga',
  '20080503_234500_bbctwo_the_apprentice_you_re_fired', '20080504_001500_bbcthree_glamour_girls', '20080504_004500_bbcthree_page_three_teens',
  '20080504_005500_bbcfour_david_ogilvy_original_mad_man', '20080504_014500_bbcthree_the_wall', '20080504_015500_bbcfour_in_search_of_medieval_britain',
  '20080504_022500_bbcfour_the_book_quiz', '20080504_023000_bbcthree_scallywagga', '20080504_050000_bbcone_breakfast', '20080504_052000_bbctwo_tikkabilla',
  '20080504_064500_bbcone_moto_gp_shanghai', '20080504_070000_bbctwo_trapped', '20080504_073000_bbctwo_hider_in_the_house', '20080504_080000_bbcone_the_andrew_marr_show',
  '20080504_083500_bbctwo_match_of_the_day', '20080504_090000_bbcone_sunday_life', '20080504_100000_bbcone_countryfile', '20080504_110000_bbcone_the_politics_show',
  '20080504_113000_bbctwo_premiership_rugby', '20080504_120000_bbcone_eastenders', '20080504_120000_bbctwo_badminton_horse_trials',
  '20080504_140000_bbctwo_snooker_world_championship', '20080504_152500_bbcone_allo_allo', '20080504_155000_bbcone_keeping_up_appearances',
  '20080504_162000_bbcone_points_of_view', '20080504_163500_bbcone_songs_of_praise', '20080504_170000_bbctwo_bbc_young_musician_of_the_year',
  '20080504_171000_bbcone_bbc_news', '20080504_172500_bbcone_bbc_london_news', '20080504_180000_bbcfour_inside_the_medieval_mind',
  '20080504_180500_bbcone_i_d_do_anything_results', '20080504_190000_bbcthree_doctor_who', '20080504_190000_bbctwo_snooker_world_championship',
  '20080504_200000_bbcfour_dear_television', '20080504_201000_bbcfour_washes_whiter', '20080504_203000_bbcthree_little_britain', '20080504_210000_bbcone_bbc_news_at_ten',
  '20080504_212000_bbcone_match_of_the_day_2', '20080504_214500_bbcfour_james_taylor_one_man_band', '20080504_214500_bbcthree_scallywagga', '20080504_215000_bbctwo_coast',
  '20080504_221500_bbcthree_two_pints_of_lager_and', '20080504_224500_bbcthree_two_pints_of_lager_and', '20080504_225000_bbcfour_hotel_california_from_the_byrds_to',
  '20080504_231500_bbcthree_child_stars', '20080504_234500_bbcone_the_sky_at_night', '20080504_234500_bbctwo_graham_norton_uncut', '20080505_000500_bbcone_weatherview',
  '20080505_001000_bbcone_watchdog', '20080505_002500_bbcfour_in_concert_james_taylor', '20080505_004000_bbcone_holby_city', '20080505_004500_bbcthree_scallywagga',
  '20080505_011000_bbcthree_two_pints_of_lager_and', '20080505_014000_bbcthree_two_pints_of_lager_and', '20080505_021000_bbcfour_inside_the_medieval_mind',
  '20080505_031000_bbctwo_inside_sport', '20080505_050000_bbcone_breakfast', '20080505_050000_bbctwo_tikkabilla', '20080505_062500_bbctwo_newsround',
  '20080505_063000_bbctwo_hider_in_the_house', '20080505_073000_bbctwo_jackanory_junior', '20080505_080000_bbcone_missing_live', '20080505_080000_bbctwo_boogie_beebies',
  '20080505_084000_bbctwo_something_special', '20080505_094500_bbcone_to_buy_or_not_to_buy', '20080505_111500_bbcone_bbc_news',
  '20080505_114000_bbcone_match_of_the_day_live', '20080505_133000_bbctwo_snooker_world_championship', '20080505_170000_bbctwo_eggheads', '20080505_173500_bbcone_bbc_news',
  '20080505_175000_bbcone_bbc_london_news', '20080505_180000_bbcfour_the_book_quiz', '20080505_180000_bbcone_the_one_show', '20080505_180000_bbcthree_dragon_s_den',
  '20080505_180000_bbctwo_val_doonican_rocks', '20080505_183000_bbcfour_the_sky_at_night', '20080505_183000_bbcone_watchdog',
  '20080505_190000_bbcfour_bbc_young_musician_of_the_year_2008', '20080505_190000_bbcone_eastenders', '20080505_190000_bbctwo_snooker_world_championship',
  '20080505_193000_bbcone_panorama', '20080505_193000_bbcthree_glamour_girls', '20080505_200000_bbcfour_christina_a_medieval_life', '20080505_200000_bbcone_waking_the_dead',
  '20080505_210000_bbcone_bbc_news_at_ten', '20080505_210000_bbcthree_eastenders', '20080505_212000_bbcone_meet_the_immigrants', '20080505_213000_bbcthree_ideal',
  '20080505_215000_bbcone_inside_sport', '20080505_220000_bbcthree_placebo', '20080505_225000_bbcfour_christina_a_medieval_life', '20080505_231500_bbcthree_glamour_girls',
  '20080505_235000_bbcfour_bbc_young_musician_of_the_year_2008', '20080506_001500_bbcone_weatherview', '20080506_004500_bbcthree_ideal', '20080506_011500_bbcthree_placebo',
  '20080506_012000_bbcfour_the_book_quiz', '20080506_015000_bbcfour_christina_a_medieval_life', '20080506_025000_bbcfour_the_book_quiz',
  '20080506_030000_bbctwo_shakespeare_the_animated_tales', '20080506_050000_bbcone_breakfast', '20080506_050000_bbctwo_tikkabilla', '20080506_062500_bbctwo_newsround',
  '20080506_063000_bbctwo_hider_in_the_house', '20080506_073000_bbctwo_jackanory_junior', '20080506_081500_bbcone_missing_live', '20080506_084000_bbctwo_something_special',
  '20080506_093000_bbctwo_schools_let_s_write_poetry', '20080506_100000_bbcone_to_buy_or_not_to_buy', '20080506_101000_bbctwo_timewatch',
  '20080506_104500_bbcone_cash_in_the_attic', '20080506_110000_bbctwo_the_daily_politics', '20080506_111500_bbcone_bargain_hunt', '20080506_113000_bbctwo_working_lunch',
  '20080506_120000_bbcone_bbc_news_at_one', '20080506_120000_bbctwo_schools_maths_channel', '20080506_121000_bbctwo_schools_primary_geography',
  '20080506_140500_bbcone_space_pirates', '20080506_141500_bbctwo_through_the_keyhole', '20080506_143500_bbcone_chucklevision', '20080506_144500_bbctwo_flog_it',
  '20080506_153000_bbctwo_ready_steady_cook', '20080506_153500_bbcone_blue_peter', '20080506_160000_bbcone_newsround', '20080506_161500_bbcone_the_weakest_link',
  '20080506_161500_bbctwo_escape_to_the_country', '20080506_170000_bbcone_bbc_news_at_six', '20080506_170000_bbctwo_eggheads', '20080506_173000_bbcone_bbc_london_news',
  '20080506_173000_bbctwo_great_british_menu', '20080506_180000_bbcone_the_one_show', '20080506_180000_bbcthree_dog_borstal', '20080506_183000_bbcfour_pop_goes_the_sixties',
  '20080506_183000_bbcone_eastenders', '20080506_190000_bbcfour_bbc_young_musician_of_the_year_2008', '20080506_190000_bbcone_holby_city',
  '20080506_190000_bbctwo_natural_world', '20080506_195000_bbctwo_watching_the_wild', '20080506_200000_bbcfour_chinese_school', '20080506_200000_bbctwo_age_of_terror',
  '20080506_210000_bbcone_bbc_news_at_ten', '20080506_210000_bbcthree_eastenders', '20080506_210000_bbctwo_later_live_with_jools_holland',
  '20080506_213000_bbcthree_scallywagga', '20080506_213000_bbctwo_newsnight', '20080506_220000_bbcthree_the_wall', '20080506_233000_bbcone_weatherview',
  '20080506_233500_bbcfour_chinese_school', '20080506_233500_bbcone_don_t_leave_me_this_way', '20080507_000500_bbcone_the_primary', '20080507_003000_bbcthree_scallywagga',
  '20080507_003500_bbcfour_bbc_young_musician_of_the_year_2008', '20080507_005500_bbcthree_the_wall', '20080507_013500_bbcfour_the_book_quiz',
  '20080507_020500_bbcfour_chinese_school', '20080507_024000_bbcthree_dog_borstal', '20080507_030000_bbctwo_shakespeare_the_animated_tales',
  '20080507_050000_bbcone_breakfast', '20080507_050000_bbctwo_tikkabilla', '20080507_062500_bbctwo_newsround', '20080507_063000_bbctwo_hider_in_the_house',
  '20080507_073000_bbctwo_jackanory_junior', '20080507_080000_bbctwo_boogie_beebies', '20080507_081500_bbcone_missing_live', '20080507_093000_bbctwo_a_picture_of_britain',
  '20080507_100000_bbcone_to_buy_or_not_to_buy', '20080507_103000_bbctwo_the_daily_politics', '20080507_104500_bbcone_cash_in_the_attic',
  '20080507_111500_bbcone_bargain_hunt', '20080507_120000_bbcone_bbc_news_at_one', '20080507_120000_bbctwo_see_hear', '20080507_123000_bbctwo_working_lunch',
  '20080507_124000_bbcone_doctors', '20080507_130000_bbctwo_animal_park', '20080507_140500_bbcone_space_pirates', '20080507_141500_bbctwo_through_the_keyhole',
  '20080507_143500_bbcone_chucklevision', '20080507_144500_bbctwo_flog_it', '20080507_151000_bbcone_young_dracula', '20080507_153500_bbcone_blue_peter',
  '20080507_160000_bbcone_newsround', '20080507_161500_bbcone_the_weakest_link', '20080507_161500_bbctwo_escape_to_the_country', '20080507_170000_bbcone_bbc_news_at_six',
  '20080507_170000_bbctwo_eggheads', '20080507_173000_bbcone_bbc_london_news', '20080507_173000_bbctwo_great_british_menu', '20080507_180000_bbcfour_world_news_today',
  '20080507_180000_bbcone_the_one_show', '20080507_180000_bbcthree_holby_city', '20080507_183000_bbcfour_pop_goes_the_sixties', '20080507_183000_bbcone_street_doctor',
  '20080507_190000_bbcfour_bbc_young_musician_of_the_year_2008', '20080507_190000_bbcthree_glamour_girls', '20080507_190000_bbctwo_city_salute_with_princes_william_and',
  '20080507_200000_bbcfour_clarissa_and_the_king_s_cookbook', '20080507_200000_bbcone_the_apprentice', '20080507_200000_bbcthree_two_pints_of_lager_and',
  '20080507_200000_bbctwo_dan_cruickshank_s_adventures', '20080507_203000_bbcfour_illuminations_treasures_of_the', '20080507_203000_bbcthree_ideal',
  '20080507_210000_bbcfour_my_secret_agent_auntie_storyville', '20080507_210000_bbcone_bbc_news_at_ten', '20080507_210000_bbctwo_the_apprentice_you_re_fired',
  '20080507_213000_bbctwo_newsnight', '20080507_222000_bbctwo_parallel_worlds_parallel_lives', '20080507_230000_bbcfour_clarissa_and_the_king_s_cookbook',
  '20080507_232000_bbcthree_two_pints_of_lager_and', '20080507_233000_bbcfour_illuminations_treasures_of_the', '20080507_235000_bbcthree_ideal',
  '20080508_000000_bbcfour_bbc_young_musician_of_the_year_2008', '20080508_002000_bbcone_weatherview', '20080508_002500_bbcone_seaside_rescue',
  '20080508_005000_bbcthree_the_wall', '20080508_005500_bbcone_meet_the_immigrants', '20080508_010000_bbcfour_my_secret_agent_auntie_storyville',
  '20080508_013500_bbcthree_glamour_girls', '20080508_020000_bbcfour_clarissa_and_the_king_s_cookbook', '20080508_023000_bbcfour_illuminations_treasures_of_the',
  '20080508_030000_bbctwo_shakespeare_the_animated_tales', '20080508_050000_bbcone_breakfast', '20080508_062500_bbctwo_newsround',
  '20080508_063000_bbctwo_hider_in_the_house', '20080508_073000_bbctwo_jackanory_junior', '20080508_080000_bbctwo_boogie_beebies', '20080508_081500_bbcone_missing_live',
  '20080508_084000_bbctwo_something_special', '20080508_093000_bbctwo_schools_primary_history', '20080508_095000_bbctwo_schools_megamaths',
  '20080508_100000_bbcone_to_buy_or_not_to_buy', '20080508_101000_bbctwo_schools_primary_geography', '20080508_103000_bbctwo_schools_science_clips',
  '20080508_104000_bbctwo_schools_science_clips', '20080508_104500_bbcone_cash_in_the_attic', '20080508_105000_bbctwo_schools_hands_up',
  '20080508_110000_bbctwo_the_daily_politics', '20080508_111500_bbcone_bargain_hunt', '20080508_113000_bbctwo_working_lunch', '20080508_120000_bbcone_bbc_news_at_one',
  '20080508_120000_bbctwo_open_gardens', '20080508_124000_bbcone_doctors', '20080508_140500_bbcone_space_pirates', '20080508_141500_bbctwo_through_the_keyhole',
  '20080508_143500_bbcone_chucklevision', '20080508_144500_bbctwo_flog_it', '20080508_150500_bbcone_stake_out', '20080508_153000_bbctwo_ready_steady_cook',
  '20080508_153500_bbcone_hider_in_the_house', '20080508_160000_bbcone_newsround', '20080508_161500_bbcone_the_weakest_link', '20080508_161500_bbctwo_escape_to_the_country',
  '20080508_170000_bbcone_bbc_news_at_six', '20080508_170000_bbctwo_eggheads', '20080508_173000_bbcone_bbc_london_news', '20080508_173000_bbctwo_great_british_menu',
  '20080508_175500_bbcone_disasters_emergency_committee_myanmar', '20080508_180000_bbcfour_world_news_today', '20080508_180000_bbcone_the_one_show',
  '20080508_182700_bbcfour_disasters_emergency_committee_myanmar', '20080508_183000_bbcfour_in_search_of_medieval_britain', '20080508_183000_bbcone_eastenders',
  '20080508_183000_bbctwo_women_in_black', '20080508_185700_bbcthree_disasters_emergency_committee_myanmar', '20080508_190000_bbcfour_bbc_young_musician_of_the_year_2008',
  '20080508_190000_bbcone_holby_blue', '20080508_190000_bbctwo_living_the_dream_revisited', '20080508_200000_bbcfour_inside_the_medieval_mind',
  '20080508_200000_bbcone_the_invisibles', '20080508_200000_bbcthree_page_three_teens', '20080508_204500_bbctwo_heroes_unmasked',
  '20080508_205700_bbcthree_disasters_emergency_committee_myanmar', '20080508_210000_bbcfour_crusades', '20080508_210000_bbcone_bbc_news_at_ten',
  '20080508_210000_bbcthree_eastenders', '20080508_212700_bbctwo_disasters_emergency_committee_myanmar', '20080508_213000_bbctwo_newsnight',
  '20080508_213500_bbcone_disasters_emergency_committee_myanmar', '20080508_222000_bbctwo_two_pints_of_lager_and', '20080508_224000_bbcfour_chinese_school',
  '20080508_224000_bbcone_this_week', '20080508_232500_bbcone_holiday_weather', '20080508_233000_bbcone_panorama', '20080508_234000_bbcfour_inside_the_medieval_mind',
  '20080508_235500_bbcthree_page_three_teens', '20080509_000000_bbcone_johnny_s_new_kingdom', '20080509_003000_bbcone_the_twenties_in_colour_the_wonderful',
  '20080509_004000_bbcfour_bbc_young_musician_of_the_year_2008', '20080509_013000_bbcone_bill_oddie_s_wild_side', '20080509_014000_bbcfour_in_search_of_medieval_britain',
  '20080509_021000_bbcfour_inside_the_medieval_mind', '20080509_024500_bbcthree_dog_borstal', '20080509_030000_bbctwo_shakespeare_shorts',
  '20080509_050000_bbcone_breakfast', '20080509_050000_bbctwo_tikkabilla', '20080509_062500_bbctwo_newsround', '20080509_063000_bbctwo_hider_in_the_house',
  '20080509_073000_bbctwo_jackanory_junior', '20080509_080000_bbctwo_boogie_beebies', '20080509_081500_bbcone_missing_live', '20080509_084000_bbctwo_something_special',
  '20080509_094500_bbctwo_schools_the_way_things_work', '20080509_100000_bbcone_to_buy_or_not_to_buy', '20080509_100000_bbctwo_schools_the_way_things_work',
  '20080509_104500_bbcone_cash_in_the_attic', '20080509_104500_bbctwo_schools_something_special', '20080509_110000_bbctwo_the_daily_politics',
  '20080509_111500_bbcone_bargain_hunt', '20080509_113000_bbctwo_working_lunch', '20080509_120000_bbcone_bbc_news_at_one', '20080509_124000_bbcone_doctors',
  '20080509_140500_bbcone_space_pirates', '20080509_141500_bbctwo_through_the_keyhole', '20080509_143500_bbcone_chucklevision', '20080509_144500_bbctwo_flog_it',
  '20080509_153000_bbctwo_ready_steady_cook', '20080509_153500_bbcone_the_slammer', '20080509_160000_bbcone_newsround', '20080509_161500_bbcone_the_weakest_link',
  '20080509_161500_bbctwo_escape_to_the_country', '20080509_170000_bbcone_bbc_news_at_six', '20080509_170000_bbctwo_eggheads', '20080509_173000_bbcone_bbc_london_news',
  '20080509_180000_bbcfour_world_news_today', '20080509_180000_bbcone_the_one_show', '20080509_180000_bbcthree_top_gear', '20080509_180000_bbctwo_wildebeest_the_super_herd',
  '20080509_183000_bbcfour_transatlantic_sessions', '20080509_183000_bbcone_after_you_ve_gone', '20080509_183000_bbctwo_the_trees_that_made_britain',
  '20080509_190000_bbcfour_bbc_young_musician_of_the_year_2008', '20080509_190000_bbcone_eastenders', '20080509_190000_bbcthree_last_man_standing',
  '20080509_190000_bbctwo_gardeners_world', '20080509_193000_bbcone_a_question_of_sport', '20080509_200000_bbcthree_doctor_who',
  '20080509_204500_bbcthree_doctor_who_confidential', '20080509_210000_bbcfour_kings_of_cool_crooners', '20080509_210000_bbcone_bbc_news_at_ten',
  '20080509_210000_bbcthree_eastenders', '20080509_213000_bbcthree_two_pints_of_lager_and', '20080509_213000_bbctwo_newsnight',
  '20080509_220000_bbcthree_two_pints_of_lager_and', '20080509_220000_bbctwo_newsnight_review', '20080509_223500_bbcone_national_lottery_euromillions_draw',
  '20080509_223500_bbctwo_later_with_jools_holland', '20080509_232000_bbcthree_scallywagga', '20080509_235000_bbcthree_ideal',
  '20080510_001000_bbcfour_bbc_young_musician_of_the_year_2008', '20080510_001500_bbcone_weatherview', '20080510_002000_bbcone_dan_cruickshank_s_adventures',
  '20080510_002000_bbcthree_two_pints_of_lager_and', '20080510_005000_bbcthree_two_pints_of_lager_and', '20080510_012000_bbcthree_glamour_girls',
  '20080510_024000_bbcfour_transatlantic_sessions', '20080510_025000_bbcthree_scallywagga', '20080510_031500_bbcthree_ideal', '20080510_050000_bbcone_breakfast',
  '20080510_050000_bbctwo_fimbles', '20080510_052000_bbctwo_tikkabilla', '20080510_070000_bbctwo_sorcerer_s_apprentice', '20080510_090000_bbcone_saturday_kitchen',
  '20080510_090000_bbctwo_hedz', '20080510_093000_bbctwo_the_slammer', '20080510_104500_bbctwo_sportsround', '20080510_110000_bbcone_bbc_news',
  '20080510_111000_bbcone_football_focus', '20080510_120000_bbcone_racing_from_ascot_and_haydock', '20080510_123000_bbctwo_the_surgery', '20080510_125000_bbctwo_sound',
  '20080510_132000_bbctwo_the_sky_at_night', '20080510_133500_bbcone_rugby_league_challenge_cup', '20080510_161500_bbcone_bbc_news',
  '20080510_162500_bbcone_bbc_london_news', '20080510_163000_bbcone_outtake_tv', '20080510_170000_bbcone_the_kids_are_all_right', '20080510_174500_bbcone_doctor_who',
  '20080510_180000_bbcfour_in_search_of_medieval_britain', '20080510_180000_bbcthree_radio_1_s_big_weekend', '20080510_180000_bbctwo_dad_s_army',
  '20080510_183000_bbcfour_the_book_quiz', '20080510_183000_bbcone_i_d_do_anything', '20080510_183000_bbcthree_doctor_who_confidential',
  '20080510_183000_bbctwo_secrets_of_the_forbidden_city', '20080510_190000_bbcfour_a_perfect_spy', '20080510_191500_bbcthree_top_gear',
  '20080510_200000_bbcfour_a_perfect_spy', '20080510_200000_bbctwo_have_i_got_a_bit_more_news_for_you', '20080510_202000_bbcone_casualty',
  '20080510_204000_bbctwo_comedy_map_of_britain', '20080510_205500_bbcfour_a_perfect_spy', '20080510_205500_bbcthree_two_pints_of_lager_and',
  '20080510_211000_bbcone_love_soup', '20080510_212500_bbcthree_two_pints_of_lager_and', '20080510_214000_bbcone_bbc_news', '20080510_214000_bbctwo_the_apprentice',
  '20080510_215500_bbcfour_christina_a_medieval_life', '20080510_224000_bbctwo_the_apprentice_you_re_fired', '20080510_225500_bbcfour_how_to_build_a_cathedral',
  '20080510_231500_bbcthree_radio_1_s_big_weekend', '20080510_235500_bbcfour_in_search_of_medieval_britain', '20080511_001500_bbcthree_two_pints_of_lager_and',
  '20080511_002500_bbcfour_the_book_quiz', '20080511_004500_bbcthree_two_pints_of_lager_and', '20080511_005500_bbcfour_christina_a_medieval_life',
  '20080511_011500_bbcthree_glamour_girls', '20080511_014500_bbcone_weatherview', '20080511_014500_bbcthree_should_i_smoke_dope',
  '20080511_015500_bbcfour_how_to_build_a_cathedral', '20080511_024000_bbcthree_scallywagga', '20080511_030500_bbcthree_glamour_girls', '20080511_050000_bbcone_breakfast',
  '20080511_050000_bbctwo_fimbles', '20080511_052000_bbctwo_tikkabilla', '20080511_070000_bbctwo_trapped', '20080511_073000_bbctwo_raven_the_secret_temple',
  '20080511_080000_bbcone_the_andrew_marr_show', '20080511_080000_bbctwo_hider_in_the_house', '20080511_090000_bbcone_in_the_light_of_the_spirit',
  '20080511_100000_bbcone_countryfile', '20080511_110000_bbcone_the_politics_show', '20080511_113000_bbctwo_premiership_rugby',
  '20080511_120000_bbctwo_rugby_league_challenge_cup', '20080511_123000_bbcone_eastenders_omnibus', '20080511_140000_bbctwo_world_cup_rowing',
  '20080511_142500_bbcone_a_heroes_welcome_the_windsor_castle', '20080511_150000_bbctwo_paralympic_world_cup', '20080511_152500_bbcone_final_score',
  '20080511_162000_bbcone_points_of_view', '20080511_163500_bbcone_songs_of_praise', '20080511_165000_bbctwo_storm_geese',
  '20080511_170000_bbctwo_bbc_young_musician_of_the_year_2008', '20080511_171000_bbcone_bbc_news', '20080511_172500_bbcone_bbc_london_news',
  '20080511_173500_bbcone_seaside_rescue', '20080511_180000_bbcfour_inside_the_medieval_mind', '20080511_180000_bbcthree_radio_1_s_big_weekend',
  '20080511_183500_bbcone_i_d_do_anything_results', '20080511_190000_bbcthree_doctor_who', '20080511_190500_bbctwo_wild_china',
  '20080511_193000_bbcfour_terry_jones_medieval_lives', '20080511_194500_bbcthree_doctor_who_confidential', '20080511_200000_bbcfour_christina_a_medieval_life',
  '20080511_200500_bbctwo_coast', '20080511_203000_bbcthree_little_britain', '20080511_210000_bbcone_bbc_news',
  '20080511_210500_bbctwo_russia_a_journey_with_jonathan_dimbleby', '20080511_212000_bbcone_match_of_the_day', '20080511_214500_bbcthree_scallywagga',
  '20080511_220500_bbctwo_graham_norton_uncut', '20080511_221000_bbcthree_radio_1_s_big_weekend', '20080511_224500_bbcfour_kings_of_cool_crooners',
  '20080511_225000_bbctwo_a_game_of_two_eras_1957_v_2007', '20080511_234000_bbcthree_sound',
]

lineno = 0
error = False

def reportError(line, errstr):
  error = True
  return "Error in line " + str(line) + ": " + errstr

def isAnchor(anchor):
  return anchor in anchors
  
def isItem(item):
  return item in items  
  
def isTime(s):
  m = re.match('(\d+).(\d+)', s)
  if m == None: 
    return False
  if int(m.group(1)) < 0 : 
    return False
  if int(m.group(2)) < 0 or int(m.group(2)) > 60: 
    return False
  return True

def isRank(s):
  try:
    i =  int(s)
    return i > 0
  except:
    return False
  
def isScore(s):
  try:
    i =  float(s)
    return True
  except:
    return False

def ToSec(s):
  m = re.match('(\d+).(\d+)', s)
  if m == None: return 0
  return int(m.group(1)) * 60 + int(m.group(2))
  
videoFiles = dict(map(lambda x: (x, True), videoFiles))

def checkRunName(runName):
  import os
  runName = os.path.basename(runName)
  generalPattern = 'me13sh_(?P<team>[^_]+)_(?P<runType>[^_]+)_(?P<segmentation>[^_]+)_(?P<asrFeature>[^_]+)_(?P<additionalFeatures>[^_]+)_(?P<description>[^_]+)(\..+)?$'
  result = {}
  result['filename'] = runName

  m = re.match(generalPattern, runName)
  if m == None:
    print >>sys.stderr, "Error: invalid run name. Run names should follow the pattern:"
    print >>sys.stderr, "me13sh_TEAM_RunType_Segmentation_TranscriptType_AdditionalFeatures[.???], where"
    print >>sys.stderr, "   RunType is one of [" + ','.join(runTypes) + ']'
    print >>sys.stderr, "   Segmentation is a combination of [" + ','.join(segmentations) + ']'
    print >>sys.stderr, "   TranscriptType is one of [" + ','.join(asrFeatures) + ']'
    print >>sys.stderr, "   AdditionalFeatures is a combination of [" + ','.join(additionalFeatures) + ']'    
    sys.exit(1)

  result['team'] = m.group('team')

  runType = m.group('runType')
  if runType not in runTypes:
    return False, "Invalid run type '" + runType + "'. Valid run types are :" + ','.join(runTypes)
  result['runType'] = runType
  
  segmentation = m.group('segmentation')
  pattern = '(' + '|'.join(segmentations) + ')'
  mS = re.match('^'+pattern+'+$', segmentation)
  if mS == None:
    return False, "Invalid segmentation '" + segmentation + "'. Segmentations should be one of: " + ','.join(segmentations)
  segmentation = re.findall(pattern, segmentation)
  result['segmentation'] = segmentation  

  asrFeature = m.group('asrFeature')
  pattern = '(' + '|'.join(asrFeatures) + ')'
  mS = re.match('^'+pattern+'+$', asrFeature)
  if mS == None:
    return False, "Invalid transcript type '" + asrFeature + "'. Transcript type should be one of: " + ','.join(asrFeatures)
  asrFeature = re.findall(pattern, asrFeature)
  result['asrFeature'] = asrFeature  

  additionalFeature = m.group('additionalFeatures')
  if additionalFeature != None:
    pattern = '(' + '|'.join(additionalFeatures) + ')'
    mS = re.match('^'+pattern+'+$', additionalFeature)
    if mS == None:
      return False, "Invalid additionalFeature '" + additionalFeature + "'. Additional features should be one of: " + ','.join(additionalFeatures)

    additionalFeature = re.findall(pattern, additionalFeature)
    result['additionalFeature'] = additionalFeature 
  else:
    result['additionalFeature'] = '' 
  return True, result


# 1. Search sub-task: 
# Workshop participants are required to submit their search results using the following whitespace separated fields in one line for each found result segment:
# Field 
# Explanation 
# queryId   The identifier of the query for which this result was found 
# "Q0"     a legacy constant 
# fileName    The identifier of the video (without extension) of the result segment
# startTime   The starting time of the result segment (mins.secs) 
# endTime   The end time of the result segment (mins.secs) 
# jumpInPoint   The time offset where a users should start playing the video (mins.secs) . If your system considers the beginning of the segment as the jump-in point, please copy the startTime to the jumpInPoint field
# rank    The rank of the result segment for this query  
# confidenceScore   A floating point value describing the confidence of the retrieval system that the result segment is the known-item 
# runName   A identifier for the retrieval system, see also RunSubmission2013 
def checkSearchRun(runName):
  f = open(runName, 'r')
  lineno = 0
  error = False
  errors = []
  for line in f:
    lineno += 1
    line = line.strip()
    field = line.split()
    if len(field) != 9:
      errors.append(reportError(lineno, "Invalid number of fields. See submission format of the readme. " + line))
      continue
    if not isItem(field[0]):
      errors.append(reportError(lineno, "Invalid item id."))
      continue
    if field[2] not in videoFiles:
      errors.append(reportError(lineno, "Invalid video file: " + field[2]))
      continue
    if not isTime(field[3]):
      errors.append(reportError(lineno, "Invalid start time " + field[3]))
      continue  
    if not isTime(field[4]):
      errors.append(reportError(lineno, "Invalid end time " + field[4]))
      continue
    if not isTime(field[5]):
      errors.append(reportError(lineno, "Invalid jumpin time " + field[5]))
      continue
    if ToSec(field[4]) <= ToSec(field[3]):
      errors.append(reportError(lineno, "Segments end time before start time."))
      continue
    if not isRank(field[6]):
      errors.append(reportError(lineno, "Invalid rank " + field[6]))
      continue
    if not isScore(field[7]):
      errors.append(reportError(lineno, "Invalid score " + field[5]))
      continue
  return errors

# 2. Linking sub-task:
# The participants are required to submit their results in the following format: 
# Field 
# Explanation 
# anchorId   The identifier of the anchor from which the links originate
# "Q0"     a legacy constant 
# fileName   The identifier of the video (without extension) of the target segment
# startTime   The starting time of the target segment (mins.secs) 
# endTime   The end time of the target segment (mins.secs) 
# rank    The rank of the target segment within the links for this anchor
# confidenceScore   A floating point value describing the confidence of the retrieval system that the target segment is a suitable link target
# runName   A identifier for the retrieval system used, see also RunSubmission2013  
def checkLinkingRun(runName):
  f = open(runName, 'r')
  lineno = 0
  foundAnchors = set()
  errors = []
  for line in f:
    lineno += 1
    line = line.strip()
    field = line.split()
    if len(field) != 8:
      errors.append(reportError(lineno, "Invalid number of fields. See submission format of the readme. " + line))
      continue
    if not isAnchor(field[0]):
      errors.append(reportError(lineno, "Invalid item id."))
      continue
    foundAnchors.add(field[0])
    if field[2] not in videoFiles:
      errors.append(reportError(lineno, "Invalid video file: " + field[2]))
      continue
    if not isTime(field[3]):
      errors.append(reportError(lineno, "Invalid start time " + field[3]))
      continue  
    if not isTime(field[4]):
      errors.append(reportError(lineno, "Invalid end time " + field[4]))
      continue
    if ToSec(field[4]) <= ToSec(field[3]):
      vals = (field[4], ToSec(field[4]), field[3], ToSec(field[3]))
      errors.append(reportError(lineno, "Segments end time %s (%d) before start time %s (%d)." % vals ))
      continue
    if isRank(field[6]):
      errors.append(reportError(lineno, "Invalid rank " + field[5]))
      continue
    if isScore(field[7]):
      errors.append(reportError(lineno, "Invalid score " + field[6]))
      continue
    start = ToSec(field[3])
    end = ToSec(field[4])
    duration = end - start
    if duration < 10 or duration > 2 * 60:
      errors.append(reportError(lineno, "Link segment from %s to %s is %d seconds long. Must be between 10sec and 2min " % (field[3], field[4], duration)))
      continue
  notseen = set(anchors) - foundAnchors
  if len(notseen) > 0:
    print "Warning: Following anchors weren't mentioned: " + ','.join(notseen)
  return errors

anyErrors = False

def recursiveAdd(f):
  if os.path.isfile(f):
    return [f]
  if os.path.isdir(f):
    runs = []
    for fi in os.listdir(f):
      runs.extend(recursiveAdd(os.path.join(f,fi)))
    return runs
  print >>sys.stderr, "File ", f, " does not exist"

#
runs = []
for file in sys.argv[1:]:
    runs.extend(recursiveAdd(file))
for runName in runs:
  print 'Run', runName
  ok, result = checkRunName(runName)
  error = False
  if ok:
    if result['runType'] == 'S':
      errors = checkSearchRun(runName)
    else:
      errors = checkLinkingRun(runName)
    nerrors = len(errors)
    if nerrors > 0:
      error = True
      if nerrors > 10:
        errors = errors[0:10]
        errors.append('... %d more errors ...' % (nerrors - 10))
      result = '\n'.join(errors)
  else:
    error = True
  
  if error:
    print re.sub('(^|\n)','\\1\t', result)
    anyErrors = True

if anyErrors:
  sys.exit(1)
else:
  sys.exit(0)
  