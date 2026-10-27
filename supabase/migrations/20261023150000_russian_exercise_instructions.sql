-- Keep the English learning material in English, but present all guidance to
-- Russian-speaking learners in Russian.

update public.lesson_exercises as exercise
set
  prompt = case exercise.order_index
    when 1 then 'Выберите правильное предложение'
    when 2 then 'Выберите естественную английскую фразу'
    when 3 then 'Найдите подходящую фразу'
    when 4 then 'Выберите правильный пример'
    when 5 then 'Прочитайте и выберите ответ'
    else 'Выберите лучший вариант'
  end,
  question = case exercise.order_index
    when 1 then 'Какое предложение подходит к теме «' || lesson.title || '»?'
    when 2 then 'Какой вариант звучит естественно в теме «' || lesson.title || '»?'
    when 3 then 'Какое предложение правильно передаёт тему «' || lesson.title || '»?'
    when 4 then 'Какой вариант грамматически правильный?'
    when 5 then 'Какое предложение соответствует теме «' || lesson.title || '»?'
    else 'Какой пример лучше всего подходит к теме «' || lesson.title || '»?'
  end,
  explanation = 'Правильный ответ: «' ||
    coalesce(exercise.options ->> exercise.correct_index, '') ||
    '». Это полное и естественное предложение на английском языке.'
from public.lessons as lesson
where lesson.id = exercise.lesson_id;
