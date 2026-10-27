-- Point every lesson to a page that contains a playable video, rather than to
-- a series catalogue. The Android app renders these pages inside its WebView.

with video_series(level_code, urls) as (
  values
  ('A0', array[
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-01-they-meet',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-02-toms-party',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-03-what-do-you-like-doing',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-04-where-are-you',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-05-speaking-or-eating',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-06-ill-pay',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-07-a-race',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-08-brown-bread',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-09-family-photos',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-10-sportsman'
  ]::text[]),
  ('A1', array[
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-01-they-meet',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-02-toms-party',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-03-what-do-you-like-doing',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-04-where-are-you',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-05-speaking-or-eating',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-06-ill-pay',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-07-a-race',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-08-brown-bread',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-09-family-photos',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-10-sportsman'
  ]::text[]),
  ('A2', array[
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-06-ill-pay',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-07-a-race',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-08-brown-bread',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-09-family-photos',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-10-sportsman',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-11-what-time-is-it',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-12-an-old-friend',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-13-horoscopes',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-14-can-you-speak-french',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out/episode-15-is-there-a-campsite'
  ]::text[]),
  ('B1', array[
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/animals',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/art',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/bestival',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/big-meal',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/blackpool',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/camden',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/christmas',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/dating',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/education',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/wots/farming'
  ]::text[]),
  ('B2', array[
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/countryside-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/countryside-great-part-2',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/creativity-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/creativity-great-part-2',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/english-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/english-great-part-2',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/entrepreneurs-are-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/entrepreneurs-are-great-part-2',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/green-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/green-great-part-2'
  ]::text[]),
  ('C1', array[
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/heritage-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/heritage-great-part-2',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/innovation-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/innovation-great-part-2',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/knowledge-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/knowledge-great-part-2',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/literature-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/literature-great-part-2',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/music-great-part-1',
    'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great/music-great-part-2'
  ]::text[])
)
update public.lessons as lesson
set video_url = series.urls[lesson.order_index]
from video_series as series
where lesson.level_code = series.level_code;

