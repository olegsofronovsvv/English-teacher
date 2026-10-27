-- Existing Auth users can be missing a public profile when an older signup
-- completed before the profile trigger was installed. Backfill those rows and
-- let an authenticated client create only its own missing profile.

drop policy if exists profiles_insert_own on public.profiles;

create policy profiles_insert_own
on public.profiles
for insert
to authenticated
with check ((select auth.uid()) = id);

grant insert (id, display_name, level_code)
on public.profiles
to authenticated;

insert into public.profiles (id, display_name, level_code)
select
    users.id,
    left(
        case
            when char_length(trim(coalesce(users.raw_user_meta_data ->> 'name', ''))) >= 2
                then trim(users.raw_user_meta_data ->> 'name')
            else split_part(coalesce(users.email, 'Ученик'), '@', 1)
        end,
        60
    ),
    case
        when upper(coalesce(users.raw_user_meta_data ->> 'level', 'A0'))
            in ('A0', 'A1', 'A2', 'B1', 'B2', 'C1')
            then upper(users.raw_user_meta_data ->> 'level')
        else 'A0'
    end
from auth.users as users
on conflict (id) do nothing;
