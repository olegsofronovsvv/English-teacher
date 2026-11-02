-- Give every lesson its own compact VOA Learning English video.
with video_sources(position, video_url) as (
  values
    (1, 'https://voa-video.voanews.eu/pangeavideo/2021/02/5/5f/5f00c78a-0f84-41e6-949d-bd808bcadf50_240p.mp4?cb=edc3e5eec'),
    (2, 'https://voa-video.voanews.eu/pangeavideo/2021/02/4/42/4298a2ba-d665-4b1c-aaa1-502bbcb9b88d_240p.mp4?cb=edc3e6218'),
    (3, 'https://voa-video.voanews.eu/pangeavideo/2016/05/0/02/027f8fdd-e9b0-409c-8618-45be49fcefe8_mobile.mp4?cb=ecfb9e14d'),
    (4, 'https://voa-video.voanews.eu/pangeavideo/2016/05/c/c8/c829a13b-9764-4b89-ad70-1edbdc3d8013_mobile.mp4?cb=ecfb9e17b'),
    (5, 'https://voa-video.voanews.eu/pangeavideo/2016/05/c/ce/ce630bdd-1ffd-4c63-aca7-4bdff50fc7ba_mobile.mp4?cb=ed0a575e6'),
    (6, 'https://voa-video.voanews.eu/pangeavideo/2016/05/1/17/17b61020-32a1-4021-9575-adbf9ee76979_mobile.mp4?cb=ecfb9e2bf'),
    (7, 'https://voa-video.voanews.eu/pangeavideo/2016/05/5/54/54ebe82e-9578-4c39-9ca9-42dae639be4f_mobile.mp4?cb=ecfb9e2d5'),
    (8, 'https://voa-video.voanews.eu/pangeavideo/2016/05/e/ee/ee277d1c-017b-4b98-9491-8de08a76122e_mobile.mp4?cb=ecfb9e192'),
    (9, 'https://voa-video.voanews.eu/pangeavideo/2016/05/4/44/44b5b429-02aa-45c0-ac5a-cde704945b4b_mobile.mp4?cb=ecfb9e1a6'),
    (10, 'https://voa-video.voanews.eu/pangeavideo/2016/06/3/3e/3ec2bbef-f064-4545-bbdd-74f2a695e141_mobile.mp4?cb=ecfb9e2ea'),
    (11, 'https://voa-video.voanews.eu/pangeavideo/2016/05/9/93/932ae001-a462-4158-8e25-399289a46ec9_mobile.mp4?cb=ecfb9e1b9'),
    (12, 'https://voa-video.voanews.eu/pangeavideo/2016/05/6/67/671fbaf2-668e-49a3-9828-a0639d1ce3d5_mobile.mp4?cb=ecfb9e304'),
    (13, 'https://voa-video.voanews.eu/pangeavideo/2021/02/7/73/73103ed5-8133-4a1a-9219-cfef86973f25_240p.mp4?cb=edc3fa7ae'),
    (14, 'https://voa-video.voanews.eu/pangeavideo/2021/02/3/3d/3d8d63a8-2984-4469-9737-d34b0d519506_240p.mp4?cb=edc3fa9f5'),
    (15, 'https://voa-video.voanews.eu/pangeavideo/2021/02/b/b0/b07eac5a-cc3a-4f3e-821c-ecae337b5415_240p.mp4?cb=edc3fab67'),
    (16, 'https://voa-video.voanews.eu/pangeavideo/2021/02/d/dd/ddc153c2-e1b2-4d09-b8ba-67172e1f88fe_240p.mp4?cb=edc3faf05'),
    (17, 'https://voa-video.voanews.eu/pangeavideo/2021/02/f/fb/fbb85647-eaa9-4ca4-801e-c4c085593d89_240p.mp4?cb=edc3fb114'),
    (18, 'https://voa-video.voanews.eu/pangeavideo/2021/02/a/aa/aa81937e-fc02-4d62-9f34-e8b432538615_240p.mp4?cb=edc3fb36f'),
    (19, 'https://voa-video.voanews.eu/pangeavideo/2021/02/8/87/87817c7f-1ceb-47ae-9e56-c90081179223_240p.mp4?cb=edc3fb68f'),
    (20, 'https://voa-video.voanews.eu/pangeavideo/2016/06/5/5d/5d078c81-3fa1-4101-9d0f-9cbab2c671ce_mobile.mp4?cb=ecfb9e1dc'),
    (21, 'https://voa-video.voanews.eu/pangeavideo/2021/02/3/38/38946ff6-eee8-4acc-a3fc-759121fd6313_240p.mp4?cb=edc3fbab8'),
    (22, 'https://voa-video.voanews.eu/pangeavideo/2021/02/6/6f/6f09d624-a8a7-4ce8-a972-1a7bfed06679_240p.mp4?cb=edc3fc2cc'),
    (23, 'https://voa-video.voanews.eu/pangeavideo/2021/02/e/e7/e7759765-b3e3-4624-a927-0cc2326eb311_240p.mp4?cb=edc3fc3cf'),
    (24, 'https://voa-video.voanews.eu/pangeavideo/2021/02/2/24/240be8a1-701d-4fd2-b6f0-5fa9dda5d188_240p.mp4?cb=edc3fc52e'),
    (25, 'https://voa-video.voanews.eu/pangeavideo/2021/02/3/35/35095de1-0ff5-48bf-815c-5dab66a55a2f_240p.mp4?cb=edc3fc754'),
    (26, 'https://voa-video.voanews.eu/pangeavideo/2021/02/b/b9/b9ddf673-07ec-4557-b33d-37cf6315e3e3_240p.mp4?cb=edc3fc9d6'),
    (27, 'https://voa-video.voanews.eu/pangeavideo/2021/02/0/00/0025af70-3442-45fb-8417-f12d5592761a_240p.mp4?cb=edc3fcb90'),
    (28, 'https://voa-video.voanews.eu/pangeavideo/2021/02/d/d2/d2a1c2bf-0655-4eab-b4dc-60e15cd64ed9_240p.mp4?cb=edc3fcd85'),
    (29, 'https://voa-video.voanews.eu/pangeavideo/2021/02/4/44/4448c192-ea38-4ddd-a68b-dc1adf515180_240p.mp4?cb=edc3fcffb'),
    (30, 'https://voa-video.voanews.eu/pangeavideo/2021/02/c/cc/cc4d97ac-b23a-4c71-a915-205f1bac86fc_240p.mp4?cb=edc40d53c'),
    (31, 'https://voa-video.voanews.eu/pangeavideo/2021/02/2/2f/2f34a0bc-9aad-45d3-b918-748706bad539_240p.mp4?cb=edc40d617'),
    (32, 'https://voa-video.voanews.eu/pangeavideo/2021/02/8/8e/8e1a9678-e224-4d51-b9ac-fa05d9991d16_240p.mp4?cb=edc40d74a'),
    (33, 'https://voa-video.voanews.eu/pangeavideo/2016/10/5/58/58320812-fc3c-4304-9a00-f2a54f99bc1a_mobile.mp4?cb=edc40e3aa'),
    (34, 'https://voa-video.voanews.eu/pangeavideo/2021/02/9/96/961080a9-4397-411e-a79f-dfa874e4c5ca_240p.mp4?cb=edc43ab4c'),
    (35, 'https://voa-video.voanews.eu/pangeavideo/2021/02/6/6f/6f867a3f-a8f1-4a89-b18c-892f85e60d35_240p.mp4?cb=edc43ad5a'),
    (36, 'https://voa-video.voanews.eu/pangeavideo/2021/02/4/46/4619beb2-18dd-45cb-a55a-42989d3beeec_240p.mp4?cb=edc43aeeb'),
    (37, 'https://voa-video.voanews.eu/pangeavideo/2021/02/1/1d/1d74e0d6-82c0-4782-bebd-e5bbd65a8057_240p.mp4?cb=edc43b190'),
    (38, 'https://voa-video.voanews.eu/pangeavideo/2021/02/0/08/088b68c0-58b8-46dc-9796-d6c542e3d44e_240p.mp4?cb=edc43b603'),
    (39, 'https://voa-video.voanews.eu/pangeavideo/2021/02/a/ac/ac4d5f85-718e-433f-a4e6-989e842db20a_240p.mp4?cb=edc43b8b5'),
    (40, 'https://voa-video.voanews.eu/pangeavideo/2021/02/b/bb/bb252e9e-dc85-4281-9e24-133f65290c88_240p.mp4?cb=edc43bac9'),
    (41, 'https://voa-video.voanews.eu/pangeavideo/2021/02/e/e7/e7e1cf42-37e1-440d-b1fa-de80ec67283d_240p.mp4?cb=edc43be94'),
    (42, 'https://voa-video.voanews.eu/pangeavideo/2021/02/9/9b/9b044a43-6f81-4c9d-9724-f48a5e1a6da7_240p.mp4?cb=edc47a5ce'),
    (43, 'https://voa-video.voanews.eu/pangeavideo/2021/02/1/1b/1b5c7537-8259-49ea-870c-abec2a1b94d1_240p.mp4?cb=edc47a83a'),
    (44, 'https://voa-video.voanews.eu/pangeavideo/2021/02/1/13/13db3058-03d4-44b8-910e-d344aa823b34_240p.mp4?cb=edc47aa9c'),
    (45, 'https://voa-video.voanews.eu/pangeavideo/2021/02/5/5c/5cb0595a-5299-4b58-bdcc-b8541b468cf3_240p.mp4?cb=edc47abcb'),
    (46, 'https://voa-video.voanews.eu/pangeavideo/2021/02/0/0a/0a211e31-97d1-4192-b60d-4de9a153000e_240p.mp4?cb=edc47acee'),
    (47, 'https://voa-video.voanews.eu/pangeavideo/2021/02/a/a4/a49eae09-d90d-4d8e-a62c-0a718b8f32be_240p.mp4?cb=edc47ae38'),
    (48, 'https://voa-video.voanews.eu/pangeavideo/2021/02/b/b7/b76551c9-f6b7-4a36-b8b8-013060087617_240p.mp4?cb=edc47b045'),
    (49, 'https://voa-video.voanews.eu/pangeavideo/2021/02/8/89/89e2abe4-1e50-4a83-b771-b3623fffb590_240p.mp4?cb=edc47b17c'),
    (50, 'https://voa-video.voanews.eu/pangeavideo/2021/02/6/6d/6dfd5f69-746e-45bd-86b1-1dba49c23a48_240p.mp4?cb=edc47b2ba'),
    (51, 'https://voa-video.voanews.eu/pangeavideo/2021/02/7/7a/7a055fd0-0345-41e4-b24d-37df1736f3d1_240p.mp4?cb=edc3e694c'),
    (52, 'https://voa-video.voanews.eu/pangeavideo/2021/02/1/17/1706ec69-afcc-40bb-a79c-4a053b5c77ad_240p.mp4?cb=edc3e78b9'),
    (53, 'https://voa-video.voanews.eu/pangeavideo/2021/02/e/ee/ee3e6fb5-9ea6-4f8b-88b4-c24e14fb2780_240p.mp4?cb=edc3e7884'),
    (54, 'https://voa-video.voanews.eu/pangeavideo/2021/02/8/8e/8e6a25fe-1336-4f85-901d-95d0dcabc112_240p.mp4?cb=edc3e78ea'),
    (55, 'https://voa-video.voanews.eu/pangeavideo/2021/02/3/3f/3f51d054-13af-46ae-a027-e469d65197b9_240p.mp4?cb=edc3e7908'),
    (56, 'https://voa-video.voanews.eu/pangeavideo/2021/02/6/6a/6a264ff8-5d3a-4460-8d09-b56daf6ebe65_240p.mp4?cb=edc3e79b1'),
    (57, 'https://voa-video.voanews.eu/pangeavideo/2021/02/c/cd/cd492745-6461-4501-9f3e-5e59269f5655_240p.mp4?cb=edc3e79dc'),
    (58, 'https://voa-video.voanews.eu/pangeavideo/2021/02/7/78/785c4ee4-6b29-45d8-a060-65bcfbcc1f16_240p.mp4?cb=edc3e7a33'),
    (59, 'https://voa-video.voanews.eu/pangeavideo/2021/02/a/a4/a461d037-2f47-4d6e-b971-86c661a4cb8d_240p.mp4?cb=edc3e7a51'),
    (60, 'https://voa-video.voanews.eu/pangeavideo/2021/02/b/b4/b442094f-e869-4a7f-9c14-d335957eafbc_240p.mp4?cb=edc3e7a9e')
), ordered_lessons as (
  select
    id,
    row_number() over (
      order by
        case level_code
          when 'A0' then 1 when 'A1' then 2 when 'A2' then 3
          when 'B1' then 4 when 'B2' then 5 when 'C1' then 6
          else 99
        end,
        order_index
    )::integer as position
  from public.lessons
)
update public.lessons as lesson
set
  video_url = source.video_url,
  video_poster_url = 'https://gdb.voanews.com/' ||
    substring(source.video_url from '([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})') ||
    '_tv_w250_r1.jpg'
from ordered_lessons
join video_sources as source using (position)
where lesson.id = ordered_lessons.id;
