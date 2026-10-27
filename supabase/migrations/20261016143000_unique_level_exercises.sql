-- Replace the repeated template exercises with a level-specific bank.
-- User progress references lessons, not exercises, so existing XP and
-- completed lessons remain intact.

delete from public.lesson_exercises;

with exercise_source as (
  select
    lesson.*,
    task_no,
    case lesson.level_code
      when 'A0' then (array[
        'Узнайте простую фразу', 'Выберите понятный пример', 'Найдите базовое правило',
        'Поймите смысл урока', 'Выберите короткое предложение', 'Ответьте в бытовой ситуации',
        'Повторите конструкцию', 'Закрепите первые слова'
      ])[task_no]
      when 'A1' then (array[
        'Выберите правильную форму', 'Соберите повседневную фразу', 'Проверьте порядок слов',
        'Найдите подходящее правило', 'Дополните простой диалог', 'Опишите обычное действие',
        'Исправьте базовую ошибку', 'Примените тему в разговоре'
      ])[task_no]
      when 'A2' then (array[
        'Выберите время по ситуации', 'Завершите бытовой диалог', 'Сравните варианты',
        'Определите значение конструкции', 'Выберите подходящий совет', 'Свяжите два события',
        'Найдите естественную реплику', 'Примените правило в контексте'
      ])[task_no]
      when 'B1' then (array[
        'Проанализируйте контекст', 'Выберите точную грамматику', 'Продолжите связный рассказ',
        'Найдите правильное перефразирование', 'Выразите мнение аргументированно', 'Выберите связующее выражение',
        'Исправьте типичную ошибку', 'Сделайте вывод из ситуации'
      ])[task_no]
      when 'B2' then (array[
        'Различите смысловые оттенки', 'Преобразуйте сложное предложение', 'Выберите точную временную форму',
        'Оцените логику аргумента', 'Найдите уместную связку', 'Сохраните исходный смысл',
        'Выберите естественную коллокацию', 'Завершите развёрнутый ответ'
      ])[task_no]
      else (array[
        'Определите академический регистр', 'Выберите наиболее точную формулировку', 'Проанализируйте скрытый смысл',
        'Найдите стилистически уместный вариант', 'Оцените степень уверенности', 'Преобразуйте формальную конструкцию',
        'Выявите предположение автора', 'Сформулируйте обоснованный вывод'
      ])[task_no]
    end as prompt,
    case lesson.level_code
      when 'A0' then (array[
        'Какая фраза подходит к этой простой теме?',
        'Какой пример можно сказать начинающему?',
        'Какое короткое правило нужно запомнить?',
        'Что объясняет этот урок простыми словами?',
        'Какое предложение построено понятно и правильно?',
        'Что можно сказать в ситуации из урока?',
        'Какую конструкцию полезно повторить вслух?',
        'Какое утверждение верно для этой темы?'
      ])[task_no]
      when 'A1' then (array[
        'Какая форма верно передаёт повседневное действие?',
        'Какая реплика естественно звучит в обычном разговоре?',
        'Какой вариант сохраняет правильный порядок слов?',
        'Какое правило объясняет выбранную грамматику?',
        'Какая фраза правильно продолжает простой диалог?',
        'Как описать действие или ситуацию из урока?',
        'Какой вариант исправляет базовую грамматическую ошибку?',
        'Как применить изученную конструкцию в речи?'
      ])[task_no]
      when 'A2' then (array[
        'Какой вариант подходит к указанной ситуации и времени?',
        'Какая реплика логично завершает бытовой диалог?',
        'Какое предложение точнее сравнивает или описывает предметы?',
        'Какое объяснение передаёт значение конструкции?',
        'Какая фраза выражает уместный совет или обязанность?',
        'Как правильно связать условие, действие или результат?',
        'Какая реплика звучит естественно в знакомой ситуации?',
        'Какой вариант правильно применяет правило в контексте?'
      ])[task_no]
      when 'B1' then (array[
        'Какой вариант точнее соответствует контексту ситуации?',
        'Какая конструкция грамматически и смыслово корректна?',
        'Как логично продолжить связный рассказ по теме?',
        'Какое перефразирование сохраняет исходное значение?',
        'Какая фраза содержит мнение, причину и ясную позицию?',
        'Какое выражение лучше связывает части высказывания?',
        'Какой вариант устраняет типичную ошибку среднего уровня?',
        'Какой вывод обоснован правилом и примерами урока?'
      ])[task_no]
      when 'B2' then (array[
        'Какой вариант передаёт нужный смысловой оттенок?',
        'Какое преобразование сохраняет грамматику и значение?',
        'Какая форма точнее выражает временную связь событий?',
        'Какое утверждение логично поддерживает аргумент?',
        'Какая связка лучше выражает контраст, причину или следствие?',
        'Какой вариант сохраняет исходный смысл без упрощения?',
        'Какое сочетание слов звучит наиболее естественно?',
        'Как завершить ответ последовательно и убедительно?'
      ])[task_no]
      else (array[
        'Какая формулировка соответствует академическому регистру?',
        'Какой вариант наиболее точен по смыслу и структуре?',
        'Какой вывод можно сделать из контекста, не преувеличивая?',
        'Какая конструкция стилистически уместна в формальном тексте?',
        'Какой вариант корректно ограничивает степень уверенности?',
        'Как преобразовать мысль в более плотную формальную конструкцию?',
        'Какое скрытое предположение лежит в основе утверждения?',
        'Какой вывод лучше всего поддержан правилом и примерами?'
      ])[task_no]
    end
      || ' Уровень ' || lesson.level_code || ', урок ' || lesson.order_index ||
         ' «' || lesson.title || '», задание ' || task_no || '.' as question,
    case lesson.level_code
      when 'A0' then (array[
        'Me name is lesson.', 'She are one book.', 'I no have a blue.', 'Yesterday tomorrow is now.',
        'He can to is ready.', 'The book under is table.', 'Do you are my name?', 'I am like apples.'
      ])[task_no]
      when 'A1' then (array[
        'She work every days.', 'There are a shop here.', 'I am go usually home.', 'He can swims well.',
        'How many water is there?', 'They is waiting now.', 'The keys is in the table.', 'I would liking this shirt.'
      ])[task_no]
      when 'A2' then (array[
        'Yesterday I will went there.', 'I have seen him last Monday.', 'She more tall than me.', 'You must to show a ticket.',
        'If it will rain, we stay home.', 'I was cook when he called.', 'The hotel is between from shops.', 'I am write to ask information.'
      ])[task_no]
      when 'B1' then (array[
        'I have finished it yesterday.', 'If it will rain, I would stay.', 'She suggested to leave early.', 'The person which helped me left.',
        'I am agree because it good.', 'Despite it was late, we continued.', 'He explained me the problem.', 'The evidence mean that it definitely true.'
      ])[task_no]
      when 'B2' then (array[
        'The report has sent yesterday.', 'She said that she is tired the day before.', 'If I knew, I would have told you yesterday.', 'Despite of the cost, it is useful.',
        'The result was big important.', 'I wish I would know the answer now.', 'We did a strong impact.', 'In conclusion, but the opposite is however true.'
      ])[task_no]
      else (array[
        'Never I have encountered such evidence.', 'The data absolutely proves every possible case.', 'It is a lot good for the framework.', 'The analysis was done and stuff happened.',
        'This result is certainly true without limitation.', 'The committee decided analysing the issue.', 'Correlation inherently establishes direct causation.', 'The only conclusion is obvious to everyone.'
      ])[task_no]
    end as distractor_one,
    case lesson.level_code
      when 'A0' then (array[
        'I name Anna am.', 'They is hello.', 'This are a book.', 'What your is name?',
        'Two apple red.', 'My mother she are.', 'Water like I.', 'Monday at lesson on.'
      ])[task_no]
      when 'A1' then (array[
        'He do not likes tea.', 'Can you to help me?', 'There is two parks.', 'I reading now.',
        'She never is late usually.', 'How much apples do you need?', 'The bag is next the chair.', 'This one is more small.'
      ])[task_no]
      when 'A2' then (array[
        'She has went home an hour ago.', 'We was travelling when it rained.', 'This is the most cheaper option.', 'You should to call her.',
        'If I am late, I called you.', 'The man which lives here is kind.', 'Go straight in the corner.', 'I look forward to hear from you.'
      ])[task_no]
      when 'B1' then (array[
        'She had left when I have arrived.', 'I enjoy to learn new words.', 'The app who we use is free.', 'Please find the reason out it.',
        'From my opinion, this is useful.', 'Although of the problem, we succeeded.', 'She is responsible of the team.', 'This proves the assumption with no doubt at all.'
      ])[task_no]
      when 'B2' then (array[
        'The policy expects to change soon.', 'He told that he would call me.', 'If she would be organised, she had arrived.', 'Although the risk, we continued.',
        'Moreover the plan is expensive, but however useful.', 'The outcome was completely without expected.', 'We reached to an agreement.', 'The evidence is therefore although limited.'
      ])[task_no]
      else (array[
        'Rarely we do observe a shift of this magnitude.', 'The findings maybe are definitely conclusive.', 'There is many nuanced implication.', 'Could you tell me the thing more or less?',
        'The trend must proves the hypothesis.', 'The nominalisation makes the text more easy.', 'A limited sample guarantees universal validity.', 'Consequently, nevertheless, the premise is however.'
      ])[task_no]
    end as distractor_two
  from public.lessons as lesson
  cross join generate_series(1, 8) as task_no
), prepared as (
  select
    id as lesson_id,
    task_no as order_index,
    prompt,
    question,
    case task_no
      when 1 then jsonb_build_array(examples ->> 0, distractor_one, distractor_two)
      when 2 then jsonb_build_array(distractor_one, examples ->> 1, distractor_two)
      when 3 then jsonb_build_array(distractor_one, distractor_two, rule_text)
      when 4 then jsonb_build_array(theory, distractor_one, distractor_two)
      when 5 then jsonb_build_array(distractor_one, examples ->> 0, distractor_two)
      when 6 then jsonb_build_array(distractor_one, distractor_two, examples ->> 1)
      when 7 then jsonb_build_array(rule_text, distractor_one, distractor_two)
      else jsonb_build_array(distractor_one, theory, distractor_two)
    end as options,
    (array[0, 1, 2, 0, 1, 2, 0, 1])[task_no] as correct_index,
    case task_no
      when 1 then 'Верный вариант взят из первого примера урока: ' || (examples ->> 0)
      when 2 then 'Второй пример показывает ещё одно применение темы: ' || (examples ->> 1)
      when 3 then 'Опирайтесь на главное правило: ' || rule_text
      when 4 then theory
      when 5 then 'Фраза построена по модели урока и звучит естественно.'
      when 6 then 'Этот пример соответствует ситуации и уровню сложности урока.'
      when 7 then 'Перед следующим заданием повторите конструкцию: ' || rule_text
      else 'Ответ следует из объяснения и примеров урока.'
    end as explanation
  from exercise_source
)
insert into public.lesson_exercises
  (lesson_id, order_index, exercise_type, prompt, question, options, correct_index, explanation)
select lesson_id, order_index, 'multiple_choice', prompt, question, options, correct_index, explanation
from prepared
order by lesson_id, order_index;

grant select on public.lesson_exercises to anon, authenticated;
