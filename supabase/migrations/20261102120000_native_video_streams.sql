-- Native Android playback uses direct HLS streams instead of rendering the
-- complete British Council web page in a WebView.

alter table public.lessons
  add column if not exists video_poster_url text not null default '';

with video_sources(level_code, media_keys) as (
  values
    ('A0', array[
      'le_starting-out_s01e01','le_starting-out_s01e02','le_starting-out_s01e03','le_starting-out_s01e04','le_starting-out_s01e05',
      'le_starting-out_s01e06','le_starting-out_s01e07','le_starting-out_s01e08','le_starting-out_s01e09','le_starting-out_s01e10'
    ]::text[]),
    ('A1', array[
      'le_starting-out_s01e01','le_starting-out_s01e02','le_starting-out_s01e03','le_starting-out_s01e04','le_starting-out_s01e05',
      'le_starting-out_s01e06','le_starting-out_s01e07','le_starting-out_s01e08','le_starting-out_s01e09','le_starting-out_s01e10'
    ]::text[]),
    ('A2', array[
      'le_starting-out_s01e06','le_starting-out_s01e07','le_starting-out_s01e08','le_starting-out_s01e09','le_starting-out_s01e10',
      'le_starting-out_s01e11','le_starting-out_s01e12','le_starting-out_s01e13','le_starting-out_s01e14','le_starting-out_s01e15'
    ]::text[]),
    ('B1', array[
      'le_great_countryside1','le_great_countryside2','le_great_creativity1','le_great_creativity2','le_great_english1',
      'le_great_english2','le_great_entrepreneurs1','le_great_entrepreneurs2','le_great_green1','le_great_green2'
    ]::text[]),
    ('B2', array[
      'le_great_heritage1','le_great_heritage2','le_great_innovation1','le_great_innovation2','le_great_knowledge1',
      'le_great_knowledge2','le_great_literature1','le_great_literature2','le_great_music1','le_great_music2'
    ]::text[]),
    ('C1', array[
      'le_great_countryside1','le_great_creativity2','le_great_english1','le_great_entrepreneurs2','le_great_green1',
      'le_great_heritage2','le_great_innovation1','le_great_knowledge2','le_great_literature1','le_great_music2'
    ]::text[])
), selected as (
  select
    lesson.id,
    sources.media_keys[lesson.order_index] as media_key
  from public.lessons as lesson
  join video_sources as sources on sources.level_code = lesson.level_code
)
update public.lessons as lesson
set
  video_url = 'https://learnenglish.britishcouncil.org/video/' || selected.media_key || '.m3u8',
  video_poster_url = case
    when selected.media_key like 'le_starting-out_%'
      then 'https://learnenglish.britishcouncil.org/video/Starting-Out/' || selected.media_key || '/' || selected.media_key || '.jpg'
    else 'https://learnenglish.britishcouncil.org/video/Great/' || selected.media_key || '/' || selected.media_key || '.jpg'
  end
from selected
where lesson.id = selected.id;
