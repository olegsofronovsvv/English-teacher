-- Make every distractor unambiguously incorrect. The previous pass could turn
-- will into would, which sometimes produced a second valid sentence.

with choices as (
  select
    exercise.id,
    exercise.order_index as task_no,
    exercise.options ->> exercise.correct_index as correct_sentence,
    case
      when (exercise.options ->> exercise.correct_index) like '% has %' then replace((exercise.options ->> exercise.correct_index), ' has ', ' have ')
      when (exercise.options ->> exercise.correct_index) like '% have %' then replace((exercise.options ->> exercise.correct_index), ' have ', ' has ')
      when (exercise.options ->> exercise.correct_index) like '% is %' then replace((exercise.options ->> exercise.correct_index), ' is ', ' are ')
      when (exercise.options ->> exercise.correct_index) like '% are %' then replace((exercise.options ->> exercise.correct_index), ' are ', ' is ')
      when (exercise.options ->> exercise.correct_index) like '% was %' then replace((exercise.options ->> exercise.correct_index), ' was ', ' were ')
      when (exercise.options ->> exercise.correct_index) like '% were %' then replace((exercise.options ->> exercise.correct_index), ' were ', ' was ')
      when (exercise.options ->> exercise.correct_index) like '% will %' then replace((exercise.options ->> exercise.correct_index), ' will ', ' will to ')
      when (exercise.options ->> exercise.correct_index) like '% would %' then replace((exercise.options ->> exercise.correct_index), ' would ', ' would to ')
      when (exercise.options ->> exercise.correct_index) like '% can %' then replace((exercise.options ->> exercise.correct_index), ' can ', ' can to ')
      else 'Did ' || lower(left((exercise.options ->> exercise.correct_index), 1)) || substring((exercise.options ->> exercise.correct_index) from 2)
    end as distractor_one,
    case
      when (exercise.options ->> exercise.correct_index) like '% do %' then replace((exercise.options ->> exercise.correct_index), ' do ', ' do to ')
      when (exercise.options ->> exercise.correct_index) like '% does %' then replace((exercise.options ->> exercise.correct_index), ' does ', ' does to ')
      when (exercise.options ->> exercise.correct_index) like '% did %' then replace((exercise.options ->> exercise.correct_index), ' did ', ' did to ')
      when (exercise.options ->> exercise.correct_index) like '% to %' then replace((exercise.options ->> exercise.correct_index), ' to ', ' to to ')
      when (exercise.options ->> exercise.correct_index) like '% a %' then replace((exercise.options ->> exercise.correct_index), ' a ', ' a a ')
      when (exercise.options ->> exercise.correct_index) like '% an %' then replace((exercise.options ->> exercise.correct_index), ' an ', ' an an ')
      when (exercise.options ->> exercise.correct_index) like '% the %' then replace((exercise.options ->> exercise.correct_index), ' the ', ' the the ')
      when (exercise.options ->> exercise.correct_index) like '% not %' then replace((exercise.options ->> exercise.correct_index), ' not ', ' no ')
      else 'Not ' || lower(left((exercise.options ->> exercise.correct_index), 1)) || substring((exercise.options ->> exercise.correct_index) from 2)
    end as distractor_two
  from public.lesson_exercises as exercise
)
update public.lesson_exercises as exercise
set options = case choices.task_no % 3
  when 1 then jsonb_build_array(choices.correct_sentence, choices.distractor_one, choices.distractor_two)
  when 2 then jsonb_build_array(choices.distractor_one, choices.correct_sentence, choices.distractor_two)
  else jsonb_build_array(choices.distractor_one, choices.distractor_two, choices.correct_sentence)
end
from choices
where exercise.id = choices.id;

