-- Award XP in the database so an Android client cannot edit total_xp directly.

alter table public.user_progress
add column if not exists xp_awarded boolean not null default false;

update public.user_progress set xp_awarded = true where status = 'completed';

create or replace function private.protect_and_award_progress()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  lesson_xp integer;
begin
  if tg_op = 'UPDATE' and old.xp_awarded then
    new.xp_awarded := true;
  end if;

  if new.status = 'completed' and not new.xp_awarded then
    select xp into lesson_xp from public.lessons where id = new.lesson_id;
    update public.profiles
      set total_xp = total_xp + coalesce(lesson_xp, 0)
      where id = new.user_id;
    new.xp_awarded := true;
  end if;
  return new;
end;
$$;

revoke all on function private.protect_and_award_progress() from public, anon, authenticated;

drop trigger if exists protect_and_award_progress on public.user_progress;
create trigger protect_and_award_progress
before insert or update on public.user_progress
for each row execute function private.protect_and_award_progress();

drop policy if exists progress_delete_own on public.user_progress;
revoke delete on public.user_progress from authenticated;
revoke update on public.profiles from authenticated;
grant update (display_name, level_code) on public.profiles to authenticated;
