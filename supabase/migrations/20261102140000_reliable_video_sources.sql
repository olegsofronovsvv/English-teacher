-- British Council video delivery is unavailable in some regions. Use compact,
-- public educational MP4 files from VOA Learning English so Media3 can start
-- playback directly without loading or scripting a third-party web page.
with video_sources(position, video_url, poster_url) as (
  values
    (1, 'https://voa-video.voanews.eu/pangeavideo/2021/02/5/5f/5f00c78a-0f84-41e6-949d-bd808bcadf50_240p.mp4?cb=edc3e5eec', 'https://gdb.voanews.com/5f00c78a-0f84-41e6-949d-bd808bcadf50_tv_w250_r1.jpg'),
    (2, 'https://voa-video.voanews.eu/pangeavideo/2021/02/4/42/4298a2ba-d665-4b1c-aaa1-502bbcb9b88d_240p.mp4?cb=edc3e6218', 'https://gdb.voanews.com/19950733-4857-4462-b2c7-607b2ba10bd1_tv_w250_r1.jpg'),
    (3, 'https://voa-video.voanews.eu/pangeavideo/2016/05/0/02/027f8fdd-e9b0-409c-8618-45be49fcefe8_mobile.mp4?cb=ecfb9e14d', 'https://gdb.voanews.com/027f8fdd-e9b0-409c-8618-45be49fcefe8_tv_w250_r1.jpg'),
    (4, 'https://voa-video.voanews.eu/pangeavideo/2016/05/c/c8/c829a13b-9764-4b89-ad70-1edbdc3d8013_mobile.mp4?cb=ecfb9e17b', 'https://gdb.voanews.com/c829a13b-9764-4b89-ad70-1edbdc3d8013_tv_w250_r1.jpg'),
    (5, 'https://voa-video.voanews.eu/pangeavideo/2016/05/c/ce/ce630bdd-1ffd-4c63-aca7-4bdff50fc7ba_mobile.mp4?cb=ed0a575e6', 'https://gdb.voanews.com/ce630bdd-1ffd-4c63-aca7-4bdff50fc7ba_tv_w250_r1.jpg'),
    (6, 'https://voa-video.voanews.eu/pangeavideo/2016/05/1/17/17b61020-32a1-4021-9575-adbf9ee76979_mobile.mp4?cb=ecfb9e2bf', 'https://gdb.voanews.com/17b61020-32a1-4021-9575-adbf9ee76979_tv_w250_r1.jpg'),
    (7, 'https://voa-video.voanews.eu/pangeavideo/2016/05/5/54/54ebe82e-9578-4c39-9ca9-42dae639be4f_mobile.mp4?cb=ecfb9e2d5', 'https://gdb.voanews.com/54ebe82e-9578-4c39-9ca9-42dae639be4f_tv_w250_r1.jpg'),
    (8, 'https://voa-video.voanews.eu/pangeavideo/2016/05/e/ee/ee277d1c-017b-4b98-9491-8de08a76122e_mobile.mp4?cb=ecfb9e192', 'https://gdb.voanews.com/ee277d1c-017b-4b98-9491-8de08a76122e_tv_w250_r1.jpg'),
    (9, 'https://voa-video.voanews.eu/pangeavideo/2016/05/4/44/44b5b429-02aa-45c0-ac5a-cde704945b4b_mobile.mp4?cb=ecfb9e1a6', 'https://gdb.voanews.com/44b5b429-02aa-45c0-ac5a-cde704945b4b_tv_w250_r1.jpg'),
    (10, 'https://voa-video.voanews.eu/pangeavideo/2016/06/3/3e/3ec2bbef-f064-4545-bbdd-74f2a695e141_mobile.mp4?cb=ecfb9e2ea', 'https://gdb.voanews.com/3ec2bbef-f064-4545-bbdd-74f2a695e141_tv_w250_r1.jpg')
)
update public.lessons as lesson
set video_url = source.video_url,
    video_poster_url = source.poster_url
from video_sources as source
where source.position = ((lesson.order_index - 1) % 10) + 1;
