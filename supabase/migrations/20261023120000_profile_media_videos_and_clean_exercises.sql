-- Add profile media, level-appropriate video lessons and a clean English-only
-- exercise bank. Existing lesson progress is preserved because it references
-- lessons rather than individual exercises.

alter table public.profiles
  add column if not exists bio text not null default '',
  add column if not exists avatar_path text not null default '';

alter table public.profiles
  drop constraint if exists profiles_bio_length;
alter table public.profiles
  add constraint profiles_bio_length check (char_length(bio) <= 200);

alter table public.lessons
  add column if not exists title_en text not null default '',
  add column if not exists video_url text not null default 'https://learnenglish.britishcouncil.org/free-resources/general/video-series';

grant update (bio, avatar_path) on public.profiles to authenticated;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values (
  'avatars',
  'avatars',
  true,
  5242880,
  array['image/jpeg', 'image/png', 'image/webp']
)
on conflict (id) do update set
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists avatars_public_read on storage.objects;
drop policy if exists avatars_insert_own on storage.objects;
drop policy if exists avatars_update_own on storage.objects;
drop policy if exists avatars_delete_own on storage.objects;

create policy avatars_public_read
on storage.objects for select
to anon, authenticated
using (bucket_id = 'avatars');

create policy avatars_insert_own
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'avatars'
  and (storage.foldername(name))[1] = (select auth.uid()::text)
);

create policy avatars_update_own
on storage.objects for update
to authenticated
using (
  bucket_id = 'avatars'
  and (storage.foldername(name))[1] = (select auth.uid()::text)
)
with check (
  bucket_id = 'avatars'
  and (storage.foldername(name))[1] = (select auth.uid()::text)
);

create policy avatars_delete_own
on storage.objects for delete
to authenticated
using (
  bucket_id = 'avatars'
  and (storage.foldername(name))[1] = (select auth.uid()::text)
);

with lesson_content(level_code, order_index, title_en, examples) as (
  values
  ('A0',1,'Greetings',array['Hello! My name is Anna.','Hi! I am Oleg.','Good morning, Ben.','Nice to meet you.','How are you today?','I am fine, thank you.']),
  ('A0',2,'The alphabet and spelling',array['A comes before B.','My name starts with L.','How do you spell your name?','Please spell that word.','There are twenty-six letters.','This is the letter G.']),
  ('A0',3,'Numbers and age',array['I am eighteen years old.','I have two books.','There are ten students.','My number is five.','She is twenty years old.','We need three chairs.']),
  ('A0',4,'In the classroom',array['This is a book.','Open the door, please.','Close your notebook.','The pen is on the desk.','Please sit down.','Listen to the teacher.']),
  ('A0',5,'Colours and shapes',array['This is a red apple.','It is a blue square.','The ball is round.','I have a green bag.','That star is yellow.','The box is brown.']),
  ('A0',6,'My family',array['This is my mother.','This is my brother.','Her name is Nina.','My father is at home.','I have one sister.','We are a happy family.']),
  ('A0',7,'Food and drinks',array['I like apples.','I would like some water.','This bread is fresh.','She drinks tea.','We have two bananas.','The soup is hot.']),
  ('A0',8,'Home and rooms',array['This is a table.','These are two chairs.','My bed is in the bedroom.','The kitchen is small.','There is a lamp here.','The window is open.']),
  ('A0',9,'Days and time',array['English is on Monday.','The lesson is at five.','Today is Friday.','It is three o’clock.','Sunday comes after Saturday.','We study in the morning.']),
  ('A0',10,'Simple questions',array['What is your name?','Where is my book?','How old are you?','Is this your pen?','Who is that person?','What colour is it?']),

  ('A1',1,'The verb be',array['I am a student.','She is at home.','They are ready.','We are good friends.','It is cold today.','You are very kind.']),
  ('A1',2,'Possessive words',array['This is my book.','Her name is Kate.','Their house is large.','His phone is new.','Our teacher is friendly.','Is this your coat?']),
  ('A1',3,'Present simple',array['I work every day.','She reads every evening.','They study English.','He plays football on Saturdays.','We live near the station.','The shop opens at nine.']),
  ('A1',4,'Daily routine',array['I usually wake up at seven.','He goes to work at eight.','We often eat dinner together.','She brushes her teeth before bed.','They sometimes walk to school.','I never drink coffee at night.']),
  ('A1',5,'There is and there are',array['There is a café near my house.','There are two parks here.','There is some milk in the fridge.','There are five chairs in the room.','There is not a bank nearby.','Are there any shops on this street?']),
  ('A1',6,'Can and cannot',array['I can swim.','She cannot drive.','Can you help me?','We can meet after work.','He cannot come today.','Birds can fly.']),
  ('A1',7,'Present continuous',array['I am reading now.','They are waiting outside.','She is cooking dinner.','We are learning English.','He is not sleeping.','Are you listening to me?']),
  ('A1',8,'Countable and uncountable nouns',array['We have many books.','There is not much water.','I need an apple.','She bought some rice.','How many cups do we need?','How much sugar is there?']),
  ('A1',9,'Prepositions of place',array['The keys are on the table.','The bag is under the chair.','The bank is next to the café.','My shoes are behind the door.','The cat is between the boxes.','There is a park opposite the school.']),
  ('A1',10,'Shopping',array['How much is this shirt?','I would like a smaller size.','Can I try this on?','Do you have this in blue?','I will pay by card.','The changing room is over there.']),

  ('A2',1,'Past simple',array['We visited London last year.','She went home early.','I watched a film yesterday.','They did not miss the bus.','Did you enjoy the concert?','He bought a new laptop last week.']),
  ('A2',2,'Future plans',array['I am going to study tonight.','I think it will rain.','We are meeting them tomorrow.','She is going to start a new job.','I will call you later.','Are you going to travel this summer?']),
  ('A2',3,'Comparisons',array['This book is cheaper.','It is the most interesting lesson.','My town is quieter than the city.','That was the best meal today.','A train is faster than a bus.','This exercise is more difficult.']),
  ('A2',4,'Travel and directions',array['Turn left at the bank.','The station is opposite the hotel.','Go straight across the bridge.','The museum is next to the library.','Which platform does the train leave from?','Could you show me the way to the centre?']),
  ('A2',5,'Past continuous',array['I was working at six.','They were walking home.','She was cooking when I arrived.','We were not watching television.','What were you doing last night?','It started raining while he was driving.']),
  ('A2',6,'Present perfect basics',array['I have seen this film.','She has never flown.','We have just finished lunch.','Have you ever visited Rome?','He has already sent the email.','They have not met our teacher yet.']),
  ('A2',7,'Should and must',array['You should call a doctor.','Passengers must show a ticket.','We should leave before eight.','You must not touch that switch.','Should I bring some food?','Students must wear an identity card.']),
  ('A2',8,'First conditional basics',array['If it rains, we will stay inside.','I will call if I am late.','If you study, you will pass.','She will be happy if you visit.','If we miss the bus, we will take a taxi.','What will you do if the shop is closed?']),
  ('A2',9,'Relative clauses',array['The woman who works here is friendly.','This is the book that I bought.','The phone which I ordered has arrived.','I know a man who speaks five languages.','That is the café where we first met.','The film that we watched was funny.']),
  ('A2',10,'Personal emails',array['Hi Sam, thanks for your message.','I hope you are doing well.','It was great to hear from you.','Would you like to visit next weekend?','Please write back when you have time.','See you soon, Anna.']),

  ('B1',1,'Present perfect',array['I have visited Italy twice.','She has already finished the report.','We have lived here since 2020.','Have you ever tried scuba diving?','He has not replied yet.','They have known each other for years.']),
  ('B1',2,'First conditional',array['If it rains, I will stay home.','I will call you when I arrive.','Unless we hurry, we will miss the train.','If she accepts the offer, she will move abroad.','We will go for a walk if the weather improves.','What will happen if nobody volunteers?']),
  ('B1',3,'Modal verbs',array['You should rest.','We must leave now.','She might join us later.','You do not have to pay today.','Could you send me the details?','They may have the information we need.']),
  ('B1',4,'Work and interviews',array['I have worked with customers for three years.','Last year I led a small project.','My main responsibility was training new staff.','I am good at solving practical problems.','I would like to develop my leadership skills.','This role matches my previous experience.']),
  ('B1',5,'Narrative tenses',array['I was walking when it started to rain.','She had left before I arrived.','While we were waiting, the lights went out.','He realised that he had forgotten his keys.','They were driving home when they saw the accident.','By the time I called, the meeting had ended.']),
  ('B1',6,'Gerunds and infinitives',array['I enjoy learning languages.','We decided to leave early.','She avoided answering the question.','He promised to call after work.','They suggested taking the earlier train.','I hope to finish the course this month.']),
  ('B1',7,'Defining relative clauses',array['The colleague who helped me is new.','The app that we use is free.','This is the place where I grew up.','People who exercise regularly often sleep better.','The laptop that I bought is very reliable.','I remember the day when we first met.']),
  ('B1',8,'Phrasal verbs',array['Please turn down the music.','We need to find out the reason.','She looked after her neighbour’s dog.','I ran out of milk this morning.','They put off the meeting until Friday.','He gave up smoking last year.']),
  ('B1',9,'Discussion and opinion',array['In my opinion, the plan is practical.','I see your point, but I disagree.','One reason is that it saves time.','I agree with you to some extent.','For example, public transport reduces traffic.','Overall, I believe the benefits are greater.']),
  ('B1',10,'Formal emails',array['I am writing to request information.','I look forward to your reply.','Could you please confirm the date?','I would be grateful for your assistance.','Please find the requested document attached.','Yours sincerely, Alex Brown.']),

  ('B2',1,'Passive voice',array['The report was sent yesterday.','English is spoken worldwide.','The new bridge will be opened in June.','Several mistakes have been corrected.','The issue is being discussed by the committee.','The package should have been delivered by now.']),
  ('B2',2,'Reported speech',array['She said that she was tired.','He told me he would call.','They explained that the flight had been cancelled.','Anna asked whether I needed help.','He admitted that he had made a mistake.','She warned us not to touch the equipment.']),
  ('B2',3,'Second and third conditionals',array['If I had time, I would travel more.','If we had left earlier, we would have arrived on time.','I would accept the offer if the salary were higher.','If she had seen the message, she would have replied.','What would you do if you lost your passport?','They might have won if they had trained harder.']),
  ('B2',4,'Building arguments',array['In my view, the proposal is worth supporting.','Although the plan is expensive, it offers long-term benefits.','A key advantage is that it reduces waste.','Critics argue that the policy limits choice.','However, this concern can be addressed through clear rules.','On balance, the evidence favours gradual change.']),
  ('B2',5,'Future forms',array['I am meeting the client tomorrow.','The train leaves at nine.','This time next week, we will be travelling.','By Friday, I will have finished the report.','The company is about to announce its decision.','I expect prices will continue to rise.']),
  ('B2',6,'Mixed conditionals',array['If I had studied medicine, I would be a doctor now.','If she were more organised, she would not have missed the deadline.','If we had saved more money, we could travel now.','He would be healthier if he had followed the advice.','If I were not so busy, I would have joined you yesterday.','They might still be together if they had communicated better.']),
  ('B2',7,'Wish and if only',array['I wish I knew the answer.','If only we had booked earlier.','She wishes she could work abroad.','I wish you would stop interrupting.','If only I had listened to your advice.','He wishes the journey were not so long.']),
  ('B2',8,'Linking ideas',array['The plan is costly; however, it is effective.','Demand increased; therefore, prices rose.','Although the task was difficult, we completed it.','In addition, the scheme creates new jobs.','The results were unclear; nevertheless, the team continued.','Whereas the first option is cheaper, the second is more reliable.']),
  ('B2',9,'Collocations',array['We reached an agreement.','The change had a significant impact.','The company faces fierce competition.','She raised an important issue.','The evidence strongly supports this conclusion.','We need to take responsibility for the decision.']),
  ('B2',10,'Argument essays',array['One major advantage is greater flexibility.','In conclusion, the benefits outweigh the risks.','It is often argued that technology reduces social contact.','This essay will examine both sides of the debate.','A further consideration is the cost of implementation.','For these reasons, a balanced approach is preferable.']),

  ('C1',1,'Inversion',array['Never have I seen such a remarkable view.','Rarely do we get an opportunity like this.','Not until midnight did the train finally arrive.','Under no circumstances should the data be shared.','Hardly had I sat down when the phone rang.','Only after the review did they identify the flaw.']),
  ('C1',2,'Cleft sentences',array['It was Anna who solved the problem.','What I need is more time.','The reason why I left was the constant noise.','It is the final chapter that needs revision.','What surprised us most was the speed of the response.','All I wanted was a clear explanation.']),
  ('C1',3,'Advanced modal forms',array['She must have forgotten the appointment.','You could have warned me earlier.','They might have misunderstood the instructions.','He cannot have completed the work alone.','We should have anticipated this difficulty.','The figures may have been reported incorrectly.']),
  ('C1',4,'Academic writing',array['The evidence suggests that the policy was effective.','Nevertheless, this interpretation has several limitations.','The findings are consistent with previous research.','A plausible explanation for this pattern is social pressure.','Further investigation is required to establish causality.','Taken together, these results support the initial hypothesis.']),
  ('C1',5,'Hedging',array['The results may indicate a wider trend.','This approach appears to be effective.','The decline could be partly attributed to seasonal change.','It is reasonable to assume that costs will increase.','The evidence seems to support a cautious conclusion.','This interpretation is likely to remain controversial.']),
  ('C1',6,'Advanced passive structures',array['It is widely believed that demand will rise.','The policy is expected to change next year.','The minister is reported to have rejected the proposal.','Several factors are thought to have influenced the outcome.','The device was found to have a minor defect.','It has been suggested that the trial should be extended.']),
  ('C1',7,'Nominalisation',array['The analysis of the data revealed a clear pattern.','The committee’s rejection of the plan caused concern.','A reduction in costs remains the primary objective.','The rapid expansion of the service created new challenges.','Their failure to disclose the results prompted an inquiry.','Implementation of the policy will require additional funding.']),
  ('C1',8,'Register and idioms',array['Could you clarify your position?','Let us get straight to the point.','The proposal does not meet the required standard.','We need to address the issue without delay.','Her explanation was clear and to the point.','The negotiations appear to have reached a dead end.']),
  ('C1',9,'Critical reading',array['The claim relies on a limited sample.','The evidence supports correlation, not causation.','The author overlooks a plausible alternative explanation.','This conclusion extends beyond the available evidence.','The source provides no data to support its central assumption.','The argument is persuasive, although its scope is narrow.']),
  ('C1',10,'Professional presentations',array['Today I will outline three key findings.','To sum up, the proposed change is both feasible and necessary.','Let me begin by providing some background.','This chart illustrates a steady increase in demand.','I would now like to address the main concern.','I will be happy to take questions at the end.'])
), updated as (
  update public.lessons as lesson
  set title_en = content.title_en,
      examples = to_jsonb(content.examples),
      video_url = case lesson.level_code
        when 'A0' then 'https://learnenglish.britishcouncil.org/free-resources/speaking/a1'
        when 'A1' then 'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out'
        when 'A2' then 'https://learnenglish.britishcouncil.org/free-resources/general/video-series/starting-out'
        when 'B1' then 'https://learnenglish.britishcouncil.org/free-resources/general/video-series/word-street'
        when 'B2' then 'https://learnenglish.britishcouncil.org/free-resources/general/video-series/britain-great'
        else 'https://learnenglish.britishcouncil.org/free-resources/general/video-zone'
      end
  from lesson_content as content
  where lesson.level_code = content.level_code
    and lesson.order_index = content.order_index
  returning lesson.id
)
select count(*) from updated;

delete from public.lesson_exercises;

with source as (
  select
    lesson.id as lesson_id,
    lesson.level_code,
    lesson.title_en,
    example.value as correct_sentence,
    example.ordinality::integer as task_no
  from public.lessons as lesson
  cross join lateral jsonb_array_elements_text(lesson.examples)
    with ordinality as example(value, ordinality)
), choices as (
  select
    source.*,
    case
      when correct_sentence like '% has %' then replace(correct_sentence, ' has ', ' have ')
      when correct_sentence like '% have %' then replace(correct_sentence, ' have ', ' has ')
      when correct_sentence like '% is %' then replace(correct_sentence, ' is ', ' are ')
      when correct_sentence like '% are %' then replace(correct_sentence, ' are ', ' is ')
      when correct_sentence like '% was %' then replace(correct_sentence, ' was ', ' were ')
      when correct_sentence like '% were %' then replace(correct_sentence, ' were ', ' was ')
      when correct_sentence like '% will %' then replace(correct_sentence, ' will ', ' will to ')
      when correct_sentence like '% would %' then replace(correct_sentence, ' would ', ' would to ')
      when correct_sentence like '% can %' then replace(correct_sentence, ' can ', ' can to ')
      else 'Did ' || lower(left(correct_sentence, 1)) || substring(correct_sentence from 2)
    end as distractor_one,
    case
      when correct_sentence like '% do %' then replace(correct_sentence, ' do ', ' do to ')
      when correct_sentence like '% does %' then replace(correct_sentence, ' does ', ' does to ')
      when correct_sentence like '% did %' then replace(correct_sentence, ' did ', ' did to ')
      when correct_sentence like '% to %' then replace(correct_sentence, ' to ', ' to to ')
      when correct_sentence like '% a %' then replace(correct_sentence, ' a ', ' a a ')
      when correct_sentence like '% an %' then replace(correct_sentence, ' an ', ' an an ')
      when correct_sentence like '% the %' then replace(correct_sentence, ' the ', ' the the ')
      when correct_sentence like '% not %' then replace(correct_sentence, ' not ', ' no ')
      else 'Not ' || lower(left(correct_sentence, 1)) || substring(correct_sentence from 2)
    end as distractor_two
  from source
)
insert into public.lesson_exercises
  (lesson_id, order_index, exercise_type, prompt, question, options, correct_index, explanation)
select
  lesson_id,
  task_no,
  'multiple_choice',
  (case level_code
    when 'A0' then array['Choose the correct sentence','Pick clear English','Find the natural phrase','Choose the right example','Read and choose','Select the best sentence']
    when 'A1' then array['Choose the correct sentence','Pick the natural phrase','Check the word order','Find the correct example','Choose clear everyday English','Select the best answer']
    when 'A2' then array['Choose the correct sentence','Read the context and choose','Find the natural option','Check the grammar','Select the accurate sentence','Choose the best answer']
    when 'B1' then array['Choose the accurate sentence','Select the natural construction','Check meaning and grammar','Find the well-formed option','Choose the precise example','Select the best sentence']
    when 'B2' then array['Choose the most accurate sentence','Select the natural formulation','Check structure and meaning','Find the grammatically sound option','Choose the precise expression','Select the strongest answer']
    else array['Choose the most precise sentence','Select the appropriate formal wording','Check nuance and structure','Find the stylistically sound option','Choose the academically accurate sentence','Select the best-formulated answer']
  end)[task_no],
  (array[
    'Which sentence is correct for “' || title_en || '”?',
    'Which option sounds natural in the topic “' || title_en || '”?',
    'Which sentence uses clear English for “' || title_en || '”?',
    'Which option is grammatically correct in “' || title_en || '”?',
    'Which sentence fits the language of “' || title_en || '”?',
    'Which is the best English example for “' || title_en || '”?'
  ])[task_no],
  case task_no % 3
    when 1 then jsonb_build_array(correct_sentence, distractor_one, distractor_two)
    when 2 then jsonb_build_array(distractor_one, correct_sentence, distractor_two)
    else jsonb_build_array(distractor_one, distractor_two, correct_sentence)
  end,
  case task_no % 3 when 1 then 0 when 2 then 1 else 2 end,
  'This is complete, natural English: “' || correct_sentence || '”'
from choices
order by lesson_id, task_no;

grant select on public.lessons, public.lesson_exercises to anon, authenticated;

