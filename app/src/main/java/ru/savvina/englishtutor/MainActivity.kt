package ru.savvina.englishtutor

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Calendar
import java.util.Locale

private val Green = Color(0xFF58CC02)
private val DarkGreen = Color(0xFF379000)
private val PaleGreen = Color(0xFFE5F9D9)
private val Blue = Color(0xFF1CB0F6)
private val PaleBlue = Color(0xFFE5F5FF)
private val Ink = Color(0xFF192631)
private val Muted = Color(0xFF6B7A88)
private val Border = Color(0xFFDDE6DF)
private val Background = Color(0xFFF8FCF7)
private val Yellow = Color(0xFFFFC800)
private val Dark = Color(0xFF17282F)
private val ErrorRed = Color(0xFFE54D42)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EnglishTutorApp() }
    }
}

@Composable
private fun EnglishTutorApp(viewModel: TutorViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val systemDarkTheme = isSystemInDarkTheme()
    val preferences = remember(context) { context.getSharedPreferences("tutor_preferences", Context.MODE_PRIVATE) }
    var darkTheme by rememberSaveable { mutableStateOf(preferences.getBoolean("dark_theme", systemDarkTheme)) }
    val setDarkTheme: (Boolean) -> Unit = { enabled ->
        darkTheme = enabled
        preferences.edit().putBoolean("dark_theme", enabled).apply()
    }
    TutorTheme(darkTheme) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (val current = uiState.screen) {
                null -> LoadingScreen()
                Screen.Login -> LoginScreen(viewModel::signedIn) { viewModel.navigate(Screen.Register) }
                Screen.Register -> RegisterScreen({ viewModel.navigate(Screen.Login) }, viewModel::signedIn)
                else -> {
                    val loaded = uiState.data
                    if (loaded == null) DataStateScreen(uiState.error, viewModel::refresh)
                    else when (current) {
                        Screen.Home -> HomeScreen(loaded, viewModel::navigate, viewModel::openLesson)
                        Screen.Course -> CourseScreen(loaded, viewModel::navigate, viewModel::openLesson)
                        Screen.Lesson -> {
                            val lesson = uiState.selectedLesson
                            if (lesson == null) DataStateScreen("Для этого уровня пока нет уроков") { viewModel.navigate(Screen.Profile) }
                            else LessonScreen(
                                lesson = lesson,
                                onBack = { viewModel.navigate(Screen.Course) },
                                onVideo = { viewModel.navigate(Screen.Video) },
                                onStart = { viewModel.navigate(Screen.Exercise) },
                            )
                        }
                        Screen.Video -> {
                            val lesson = uiState.selectedLesson
                            if (lesson == null) DataStateScreen("Для этого уровня пока нет уроков") { viewModel.navigate(Screen.Course) }
                            else VideoLessonScreen(lesson) { viewModel.navigate(Screen.Lesson) }
                        }
                        Screen.Exercise -> {
                            val lesson = uiState.selectedLesson
                            val exercises = lesson?.let { item -> loaded.exercises.filter { it.lessonId == item.id } }.orEmpty()
                            if (lesson == null || exercises.isEmpty()) DataStateScreen("Для урока пока нет упражнений") { viewModel.navigate(Screen.Course) }
                            else ExerciseScreen(lesson, exercises, { viewModel.navigate(Screen.Lesson) }) { correct, total -> viewModel.completeLesson(lesson, correct, total) }
                        }
                        Screen.Vocabulary -> VocabularyScreen(loaded, viewModel::navigate, viewModel::setWordSaved)
                        Screen.Profile -> ProfileScreen(
                            data = loaded,
                            isDarkTheme = darkTheme,
                            onThemeChange = setDarkTheme,
                            onNavigate = viewModel::navigate,
                            onLevelChange = viewModel::updateLevel,
                            onBioSave = viewModel::updateBio,
                            onAvatarUpload = viewModel::uploadAvatar,
                        ) { viewModel.loggedOut() }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = Green,
            secondary = Blue,
            background = Color(0xFF0E1714),
            surface = Color(0xFF17231E),
            surfaceVariant = Color(0xFF22312A),
            onSurface = Color(0xFFF1F7F3),
            onSurfaceVariant = Color(0xFFAAB9B0),
            outline = Color(0xFF52645A),
            outlineVariant = Color(0xFF34473D),
        )
    } else {
        lightColorScheme(
            primary = Green,
            secondary = Blue,
            background = Background,
            surface = Color.White,
            surfaceVariant = Color(0xFFF0F5F1),
            onSurface = Ink,
            onSurfaceVariant = Muted,
            outline = Color(0xFF9AA89F),
            outlineVariant = Border,
        )
    }
    MaterialTheme(
        colorScheme = colors,
        typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)),
        content = content,
    )
}

@Composable
private fun LoadingScreen() = Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(Green), contentAlignment = Alignment.Center) {
            Text("A", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(18.dp))
        Text("LINGO", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Загружаем ваш курс", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        CircularProgressIndicator(color = Green, strokeWidth = 4.dp)
    }
}

@Composable
private fun DataStateScreen(error: String?, retry: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (error == null) CircularProgressIndicator(color = Green) else {
            Text("Не удалось загрузить данные", color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp)); Text(error, color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(22.dp)); PrimaryButton("Повторить", onClick = retry)
        }
    }
}

@Composable
private fun LoginScreen(onLogin: () -> Unit, onRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }; var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(310.dp).background(Green).statusBarsPadding()) {
            Text("LINGO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(24.dp))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(132.dp).clip(RoundedCornerShape(38.dp)).background(Color.White), contentAlignment = Alignment.Center) { Text("A", color = Green, fontSize = 62.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(24.dp)); Text("Английский без лишнего", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.padding(24.dp).navigationBarsPadding()) {
            Title("Рады видеть снова"); Spacer(Modifier.height(8.dp)); BodyMuted("Продолжайте обучение с того места, где остановились")
            Spacer(Modifier.height(28.dp)); TutorField(email, { email = it }, "Электронная почта", KeyboardType.Email)
            Spacer(Modifier.height(14.dp)); TutorField(password, { password = it }, "Пароль", password = true)
            errorMessage?.let { AuthMessage(it, true) }; Spacer(Modifier.height(22.dp))
            PrimaryButton(if (isLoading) "Входим…" else "Войти", enabled = !isLoading) {
                val cleanEmail = normalizeEmail(email)
                when {
                    !isValidEmail(cleanEmail) -> errorMessage = "Введите корректный email латинскими буквами, без пробелов"
                    password.isBlank() -> errorMessage = "Введите пароль"
                    else -> scope.launch {
                        isLoading = true; errorMessage = null
                        runCatching { SupabaseProvider.client.auth.signInWith(Email) { this.email = cleanEmail; this.password = password } }
                            .onSuccess { onLogin() }.onFailure { errorMessage = friendlyAuthError(it, false) }
                        isLoading = false
                    }
                }
            }
            TextButton(onClick = onRegister, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp)) { Text("Нет аккаунта? Создать", color = Green, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
private fun RegisterScreen(onBack: () -> Unit, onRegister: () -> Unit) {
    var name by remember { mutableStateOf("") }; var email by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }; var level by remember { mutableStateOf("A0") }; var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }; var isLoading by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        HeaderBack("Создать аккаунт", onBack); BodyMuted("Начните путь от первого слова до свободной речи"); Spacer(Modifier.height(18.dp))
        FieldLabel("Имя"); TutorField(name, { name = it }, "Например, Анна или Олег")
        FieldLabel("Электронная почта"); TutorField(email, { email = it }, "name@example.com", KeyboardType.Email)
        FieldLabel("Пароль"); TutorField(password, { password = it }, "Не менее 8 символов", password = true)
        FieldLabel("Повторите пароль"); TutorField(repeat, { repeat = it }, "Введите пароль ещё раз", password = true)
        Spacer(Modifier.height(20.dp)); SectionTitle("Ваш уровень английского"); BodyMuted("Можно изменить позже в профиле"); LevelSelector(level) { level = it }
        errorMessage?.let { AuthMessage(it, true) }; successMessage?.let { AuthMessage(it, false) }; Spacer(Modifier.height(30.dp))
        PrimaryButton(if (isLoading) "Создаём аккаунт…" else "Зарегистрироваться", enabled = !isLoading) {
            val cleanEmail = normalizeEmail(email); val cleanName = normalizeName(name)
            when {
                !isValidName(cleanName) -> errorMessage = "Введите имя буквами, например: Олег или Анна-Мария"
                !isValidEmail(cleanEmail) -> errorMessage = "Введите корректный email латинскими буквами, без пробелов"
                password.length < 8 -> errorMessage = "Пароль должен содержать не менее 8 символов"
                password != repeat -> errorMessage = "Пароли не совпадают"
                else -> scope.launch {
                    isLoading = true; errorMessage = null; successMessage = null
                    runCatching {
                        SupabaseProvider.client.auth.signUpWith(Email) { this.email = cleanEmail; this.password = password; data = buildJsonObject { put("name", cleanName); put("level", level) } }
                    }.onSuccess {
                        if (SupabaseProvider.client.auth.currentSessionOrNull() != null) onRegister() else successMessage = "Аккаунт создан. Подтвердите email и войдите"
                    }.onFailure { errorMessage = friendlyAuthError(it, true) }
                    isLoading = false
                }
            }
        }
        TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Уже есть аккаунт? Войти", color = Green, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun HomeScreen(data: TutorData, onNavigate: (Screen) -> Unit, onLesson: (LessonDto) -> Unit) {
    val nextLesson = data.lessons.firstOrNull { it.id !in data.completedLessonIds } ?: data.lessons.lastOrNull()
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) { in 5..11 -> "Доброе утро"; in 12..17 -> "Добрый день"; else -> "Добрый вечер" }
    ScreenScaffold(Screen.Home, onNavigate) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(18.dp)); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Title("$greeting, ${data.profile.displayName}", Modifier.weight(1f)); Pill("${data.profile.streakDays} дней", Color(0xFFFFF4CC), Ink)
            }
            Spacer(Modifier.height(20.dp)); StatsCard(data)
            if (nextLesson != null) { Spacer(Modifier.height(26.dp)); ContinueCard(nextLesson, nextLesson.id in data.completedLessonIds) { onLesson(nextLesson) } }
            Spacer(Modifier.height(30.dp)); SectionTitle("Тренируйтесь каждый день")
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PracticeCard("Aa", "Слова", "${data.savedVocabularyIds.size} сохранено", Blue, Modifier.weight(1f)) { onNavigate(Screen.Vocabulary) }
                PracticeCard("▶", "Уроки", "${data.completedLessons} пройдено", Green, Modifier.weight(1f)) { onNavigate(Screen.Course) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun CourseScreen(data: TutorData, onNavigate: (Screen) -> Unit, onLesson: (LessonDto) -> Unit) {
    val done = data.completedLessons
    ScreenScaffold(Screen.Course, onNavigate) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(18.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Title("Курс ${data.profile.levelCode}", Modifier.weight(1f)); Pill("$done из ${data.lessons.size}", PaleGreen, Green) }
            BodyMuted("Учебная программа по уровню CEFR"); Spacer(Modifier.height(18.dp)); Progress(if (data.lessons.isEmpty()) 0f else done.toFloat() / data.lessons.size); Spacer(Modifier.height(28.dp))
            data.lessons.forEachIndexed { index, lesson ->
                val completed = lesson.id in data.completedLessonIds; val unlocked = index == 0 || data.lessons[index - 1].id in data.completedLessonIds
                LessonRow(if (completed) "✓" else lesson.orderIndex.toString(), lesson.title, when { completed -> "Пройдено · ${lesson.xp} XP"; unlocked -> "${lesson.estimatedMinutes} минут · ${lesson.xp} XP"; else -> "Сначала завершите предыдущий урок" }, unlocked && !completed, !unlocked) { if (unlocked) onLesson(lesson) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LessonScreen(lesson: LessonDto, onBack: () -> Unit, onVideo: () -> Unit, onStart: () -> Unit) {
    val speaker = rememberSpeaker()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { HeaderBack("Урок ${lesson.orderIndex}", onBack, Modifier.weight(1f)); Pill("+${lesson.xp} XP", PaleGreen, Green) }
        Spacer(Modifier.height(24.dp))
        Box(Modifier.fillMaxWidth().height(202.dp).clip(RoundedCornerShape(22.dp)).background(Dark), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(68.dp).clip(CircleShape).background(Green).clickable(onClick = onVideo), contentAlignment = Alignment.Center) { Text("▶", color = Color.White, fontSize = 25.sp) }
                Spacer(Modifier.height(18.dp)); Text(lesson.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("Открыть видеоурок VOA Learning English", color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(30.dp)); SectionTitle("Объяснение"); Spacer(Modifier.height(10.dp)); BodyMuted(lesson.theory)
        Spacer(Modifier.height(22.dp)); SectionTitle("Главное правило"); Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(Modifier.padding(18.dp)) { Text(lesson.ruleText, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold); lesson.examples.forEach { Spacer(Modifier.height(10.dp)); Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp) } }
        }
        Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = { speaker(lesson.examples.joinToString(". ")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Прослушать все примеры", color = Blue, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(24.dp)); PrimaryButton("Начать упражнения", onClick = onStart)
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoLessonScreen(lesson: LessonDto, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var isBuffering by remember(lesson.id) { mutableStateOf(true) }
    var playbackError by remember(lesson.id) { mutableStateOf<String?>(null) }
    var isFullscreen by rememberSaveable(lesson.id) { mutableStateOf(false) }
    val player = remember(lesson.id, lesson.videoUrl) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(30_000)
            .setReadTimeoutMs(30_000)
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("EnglishTutor/1.0 Android")
        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory)
            .setLoadErrorHandlingPolicy(DefaultLoadErrorHandlingPolicy(5))
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                }

                override fun onPlayerError(error: PlaybackException) {
                    isBuffering = false
                    playbackError = "Не удалось воспроизвести видео"
                }
            })
            setMediaItem(MediaItem.fromUri(lesson.videoUrl))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    DisposableEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            if (isFullscreen) activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().background(MaterialTheme.colorScheme.background)) {
        HeaderBack("Видеоурок", onBack, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Text(lesson.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Card(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().aspectRatio(16f / 9f).align(Alignment.CenterHorizontally),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(Dark),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            ) {
                VideoPlayer(
                    lesson = lesson,
                    player = player,
                    isBuffering = isBuffering,
                    playbackError = playbackError,
                    onRetry = {
                        playbackError = null
                        isBuffering = true
                        player.prepare()
                        player.play()
                    },
                    onFullscreen = { isFullscreen = true },
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                VideoInfoCard(lesson.levelCode, "Уровень", Modifier.weight(1f))
                VideoInfoCard("${lesson.estimatedMinutes} мин", "Длительность", Modifier.weight(1f))
                VideoInfoCard("+${lesson.xp}", "XP за урок", Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))
            SectionTitle("Об этом уроке")
            Spacer(Modifier.height(10.dp))
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    text = lesson.subtitle.ifBlank { lesson.theory },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 21.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            SectionTitle("Повторите после видео")
            Spacer(Modifier.height(10.dp))
            lesson.examples.take(3).forEachIndexed { index, example ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(PaleGreen),
                        contentAlignment = Alignment.Center,
                    ) { Text("${index + 1}", color = Green, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.width(12.dp))
                    Text(example, color = MaterialTheme.colorScheme.onSurface, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Вернуться к уроку", onClick = onBack)
            Spacer(Modifier.height(12.dp))
        }
    }

    if (isFullscreen) {
        Dialog(
            onDismissRequest = {
                isFullscreen = false
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                VideoPlayer(
                    lesson = lesson,
                    player = player,
                    isBuffering = isBuffering,
                    playbackError = playbackError,
                    onRetry = {
                        playbackError = null
                        isBuffering = true
                        player.prepare()
                        player.play()
                    },
                    onFullscreen = {
                        isFullscreen = false
                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    },
                    isFullscreen = true,
                )
            }
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    lesson: LessonDto,
    player: ExoPlayer,
    isBuffering: Boolean,
    playbackError: String?,
    onRetry: () -> Unit,
    onFullscreen: () -> Unit,
    isFullscreen: Boolean = false,
) {
    Box(Modifier.fillMaxSize().background(Dark)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { playerContext ->
                PlayerView(playerContext).apply {
                    this.player = player
                    useController = true
                    controllerAutoShow = true
                    controllerHideOnTouch = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    keepScreenOn = true
                }
            },
            update = { it.player = player },
        )
        if (isBuffering && playbackError == null) {
            AsyncImage(
                model = lesson.videoPosterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)))
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = Green, strokeWidth = 4.dp)
                Spacer(Modifier.height(10.dp))
                Text("Загружаем видео…", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        playbackError?.let { message ->
            Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(message, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Text("Сервер отвечает медленно. Попробуйте ещё раз", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Повторить") }
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(10.dp).size(42.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.58f)).clickable(onClick = onFullscreen),
            contentAlignment = Alignment.Center,
        ) { Text(if (isFullscreen) "×" else "⛶", color = Color.White, fontSize = if (isFullscreen) 28.sp else 22.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun VideoInfoCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Green, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
private fun ExerciseScreen(lesson: LessonDto, exercises: List<ExerciseDto>, onClose: () -> Unit, onFinish: (Int, Int) -> Unit) {
    var index by remember { mutableIntStateOf(0) }; var selected by remember { mutableIntStateOf(-1) }; var checked by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }; var isFinishing by remember { mutableStateOf(false) }; var retryMessage by remember { mutableStateOf<String?>(null) }; val exercise = exercises[index]; val isCorrect = selected == exercise.correctIndex
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Вопрос ${index + 1} из ${exercises.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text("×", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 32.sp, modifier = Modifier.clickable(onClick = onClose)) }
        Spacer(Modifier.height(18.dp)); Progress((index + 1).toFloat() / exercises.size); Spacer(Modifier.height(32.dp)); Pill("Практика", PaleBlue, Blue)
        retryMessage?.let { message -> Spacer(Modifier.height(14.dp)); Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF4CC)).padding(14.dp)) { Text(message, color = Ink, fontWeight = FontWeight.SemiBold) } }
        Spacer(Modifier.height(30.dp)); Title(exercise.prompt); Spacer(Modifier.height(18.dp)); Text(exercise.question, color = DarkGreen, fontSize = 21.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(26.dp))
        exercise.options.forEachIndexed { optionIndex, answer ->
            Answer(('A'.code + optionIndex).toChar().toString(), answer, selected == optionIndex, when { !checked -> AnswerState.Normal; optionIndex == exercise.correctIndex -> AnswerState.Correct; optionIndex == selected -> AnswerState.Wrong; else -> AnswerState.Normal }) { if (!checked) selected = optionIndex }
        }
        if (checked) { Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isCorrect) PaleGreen else Color(0xFFFFECEA)).padding(16.dp)) {
            Column { Text(if (isCorrect) "Верно!" else "Правильный ответ: ${exercise.options[exercise.correctIndex]}", color = if (isCorrect) DarkGreen else ErrorRed, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(exercise.explanation, color = Ink.copy(alpha = 0.72f), fontSize = 13.sp) }
        } }
        Spacer(Modifier.height(28.dp)); PrimaryButton(when { isFinishing -> "Сохраняем…"; !checked -> "Проверить"; index == exercises.lastIndex -> "Завершить урок"; else -> "Следующий вопрос" }, enabled = selected >= 0 && !isFinishing) {
            if (!checked) { checked = true; if (isCorrect) correctCount++ }
            else if (index == exercises.lastIndex) {
                val score = correctCount * 100 / exercises.size
                if (score >= 70) { isFinishing = true; onFinish(correctCount, exercises.size) }
                else {
                    retryMessage = "Результат: $correctCount из ${exercises.size}. Для завершения нужно не менее 70%. Попробуйте ещё раз."
                    index = 0; selected = -1; checked = false; correctCount = 0; isFinishing = false
                }
            }
            else { index++; selected = -1; checked = false }
        }
    }
}

@Composable
private fun VocabularyScreen(data: TutorData, onNavigate: (Screen) -> Unit, onToggle: (VocabularyDto, Boolean) -> Unit) {
    val speaker = rememberSpeaker()
    ScreenScaffold(Screen.Vocabulary, onNavigate) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(18.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Title("Словарь", Modifier.weight(1f)); Pill("${data.savedVocabularyIds.size} слов", PaleGreen, Green) }
            BodyMuted("Слова уровня ${data.profile.levelCode}. Нажмите +, чтобы сохранить для повторения."); Spacer(Modifier.height(24.dp))
            if (data.vocabulary.isEmpty()) BodyMuted("Для этого уровня слова ещё не добавлены")
            data.vocabulary.forEach { word -> WordRow(word, word.id in data.savedVocabularyIds, { speaker(word.word) }) { onToggle(word, word.id !in data.savedVocabularyIds) } }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ProfileScreen(
    data: TutorData,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onNavigate: (Screen) -> Unit,
    onLevelChange: (String) -> Unit,
    onBioSave: suspend (String) -> Unit,
    onAvatarUpload: suspend (ByteArray, String, String) -> Unit,
    onLogout: () -> Unit,
) {
    val email = SupabaseProvider.client.auth.currentUserOrNull()?.email.orEmpty()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val avatarUrl = remember(data.profile.avatarPath) { TutorRepository.avatarPublicUrl(data.profile.avatarPath) }
    var bio by remember(data.profile.id, data.profile.bio) { mutableStateOf(data.profile.bio) }
    var isEditingBio by remember(data.profile.id) { mutableStateOf(false) }
    var profileMessage by remember { mutableStateOf<String?>(null) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var isSavingProfile by remember { mutableStateOf(false) }
    var logoutError by remember { mutableStateOf<String?>(null) }
    var isSigningOut by remember { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            isSavingProfile = true; profileError = null; profileMessage = null
            runCatching {
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val extension = when (mimeType.lowercase()) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Не удалось прочитать изображение")
                }
                onAvatarUpload(bytes, mimeType, extension)
            }.onSuccess { profileMessage = "Аватар обновлён" }
                .onFailure { profileError = it.message ?: "Не удалось загрузить аватар" }
            isSavingProfile = false
        }
    }
    ScreenScaffold(Screen.Profile, onNavigate) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(18.dp)); Title("Профиль"); Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(84.dp).clip(RoundedCornerShape(28.dp)).background(Green).clickable(enabled = !isSavingProfile) { avatarPicker.launch("image/*") }, contentAlignment = Alignment.Center) {
                    Text(data.profile.displayName.take(1).uppercase(), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    if (avatarUrl != null) AsyncImage(model = avatarUrl, contentDescription = "Аватар профиля", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Box(Modifier.align(Alignment.BottomEnd).size(27.dp).clip(CircleShape).background(Dark).border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) { Text("+", color = Color.White, fontWeight = FontWeight.Bold) }
                }
                Column(Modifier.padding(start = 18.dp).weight(1f)) { Text(data.profile.displayName, fontSize = 22.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); BodyMuted(email) }
            }
            Spacer(Modifier.height(24.dp)); ProfileStats(data)
            Spacer(Modifier.height(30.dp)); Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("О себе", Modifier.weight(1f))
                Box(
                    Modifier.size(38.dp).clip(CircleShape).background(PaleGreen).clickable(enabled = !isSavingProfile) {
                        if (isEditingBio) {
                            bio = data.profile.bio
                            isEditingBio = false
                        } else isEditingBio = true
                    },
                    contentAlignment = Alignment.Center,
                ) { Text(if (isEditingBio) "×" else "+", color = DarkGreen, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(10.dp))
            if (isEditingBio) {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { if (it.length <= 200) bio = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 4,
                    placeholder = { Text("Расскажите о своих целях в английском", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    supportingText = { Text("${bio.length}/200") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface),
                )
                Spacer(Modifier.height(10.dp)); OutlinedButton(
                    onClick = {
                        scope.launch {
                            isSavingProfile = true; profileError = null; profileMessage = null
                            runCatching { onBioSave(bio) }
                                .onSuccess { isEditingBio = false; profileMessage = "Описание сохранено" }
                                .onFailure { profileError = it.message ?: "Не удалось сохранить описание" }
                            isSavingProfile = false
                        }
                    },
                    enabled = !isSavingProfile && bio.trim() != data.profile.bio,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) { Text(if (isSavingProfile) "Сохраняем…" else "Сохранить описание", color = Green, fontWeight = FontWeight.Bold) }
            } else {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Text(
                        text = data.profile.bio.ifBlank { "Описание пока не добавлено" },
                        color = if (data.profile.bio.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            profileMessage?.let { AuthMessage(it, false) }; profileError?.let { AuthMessage(it, true) }
            Spacer(Modifier.height(30.dp)); SectionTitle("Уровень английского"); BodyMuted("Выберите курс, который хотите проходить"); LevelSelector(data.profile.levelCode, onLevelChange)
            Spacer(Modifier.height(30.dp)); SectionTitle("Оформление")
            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Тёмная тема", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Снижает яркость интерфейса вечером", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Switch(checked = isDarkTheme, onCheckedChange = onThemeChange)
                }
            }
            Spacer(Modifier.height(30.dp)); Row(verticalAlignment = Alignment.CenterVertically) { SectionTitle("Личный словарь", Modifier.weight(1f)); Pill("${data.savedVocabularyIds.size} слов", PaleGreen, Green) }
            Spacer(Modifier.height(14.dp)); data.vocabulary.filter { it.id in data.savedVocabularyIds }.take(4).forEach { CompactWordRow(it.word, it.translation) }
            if (data.savedVocabularyIds.isEmpty()) BodyMuted("Сохраните слова во вкладке «Словарь»")
            TextButton(onClick = { onNavigate(Screen.Vocabulary) }) { Text("Открыть весь словарь", color = Green) }; logoutError?.let { AuthMessage(it, true) }; Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { if (!isSigningOut) scope.launch { isSigningOut = true; logoutError = null; runCatching { SupabaseProvider.client.auth.signOut() }.onSuccess { onLogout() }.onFailure { logoutError = "Не удалось выйти. Проверьте интернет" }; isSigningOut = false } }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(if (isSigningOut) "Выходим…" else "Выйти из аккаунта", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable private fun ScreenScaffold(current: Screen, onNavigate: (Screen) -> Unit, content: @Composable (PaddingValues) -> Unit) { Scaffold(containerColor = MaterialTheme.colorScheme.background, bottomBar = { BottomNav(current, onNavigate) }, content = content) }

@Composable
private fun BottomNav(current: Screen, onNavigate: (Screen) -> Unit) {
    val items = listOf(Triple(Screen.Home, "⌂", "Главная"), Triple(Screen.Course, "✓", "Курс"), Triple(Screen.Vocabulary, "Aa", "Словарь"), Triple(Screen.Profile, "●", "Профиль"))
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 8.dp, modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        items.forEach { item ->
            val active = current == item.first
            NavigationBarItem(
                selected = active,
                onClick = { onNavigate(item.first) },
                icon = { Text(item.second, fontSize = if (item.second == "Aa") 15.sp else 20.sp, fontWeight = FontWeight.Bold) },
                label = { Text(item.third, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkGreen,
                    selectedTextColor = DarkGreen,
                    indicatorColor = PaleGreen,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

private enum class AnswerState { Normal, Correct, Wrong }

@Composable
private fun Answer(letter: String, text: String, selected: Boolean, state: AnswerState, onClick: () -> Unit) {
    val color = when (state) { AnswerState.Correct -> Green; AnswerState.Wrong -> ErrorRed; AnswerState.Normal -> if (selected) Green else MaterialTheme.colorScheme.outlineVariant }
    val background = when (state) { AnswerState.Correct -> PaleGreen; AnswerState.Wrong -> Color(0xFFFFECEA); AnswerState.Normal -> if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface }
    val answerTextColor = when (state) {
        AnswerState.Correct, AnswerState.Wrong -> Ink
        AnswerState.Normal -> MaterialTheme.colorScheme.onSurface
    }
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(background).border(if (selected || state != AnswerState.Normal) 2.dp else 1.dp, color, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(letter, color = if (selected || state != AnswerState.Normal) color else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(18.dp)); Text(text, color = answerTextColor, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun LevelSelector(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("A0", "A1", "A2", "B1", "B2", "C1").forEach { level -> Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(14.dp)).background(if (selected == level) Green else MaterialTheme.colorScheme.surface).border(1.dp, if (selected == level) Green else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp)).clickable { onSelect(level) }, contentAlignment = Alignment.Center) { Text(level, color = if (selected == level) Color.White else MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun rememberSpeaker(): (String) -> Unit {
    val context = LocalContext.current; val speaker = remember { TextToSpeech(context) {} }
    DisposableEffect(speaker) { onDispose { speaker.stop(); speaker.shutdown() } }
    return { text -> speaker.language = Locale.US; speaker.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english_tutor") }
}

@Composable private fun Title(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = MaterialTheme.colorScheme.onSurface, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
@Composable private fun SectionTitle(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)
@Composable private fun BodyMuted(text: String) = Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
@Composable private fun FieldLabel(text: String) { Spacer(Modifier.height(14.dp)); Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)) }
@Composable private fun HeaderBack(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) { Row(modifier, verticalAlignment = Alignment.CenterVertically) { Text("‹", fontSize = 36.sp, modifier = Modifier.clickable(onClick = onBack)); Spacer(Modifier.width(10.dp)); Title(title) } }

@Composable
private fun TutorField(value: String, onValueChange: (String) -> Unit, hint: String, keyboardType: KeyboardType = KeyboardType.Text, password: Boolean = false) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth().height(58.dp), placeholder = { Text(hint, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface), keyboardOptions = KeyboardOptions(keyboardType = keyboardType), visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
}

@Composable private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) { Button(onClick, Modifier.fillMaxWidth().height(58.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun Pill(text: String, background: Color, color: Color) { Box(Modifier.clip(RoundedCornerShape(17.dp)).background(background).padding(horizontal = 13.dp, vertical = 8.dp)) { Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun Progress(value: Float) { Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) { Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(Green)) } }

@Composable private fun StatsCard(data: TutorData) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) { Stat("${data.profile.totalXp} XP", "общий опыт", Green); Stat(data.profile.levelCode, "уровень", Blue); Stat(data.completedLessons.toString(), "уроков", MaterialTheme.colorScheme.onSurface) } } }
@Composable private fun Stat(value: String, label: String, color: Color) { Column { Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } }

@Composable
private fun ContinueCard(lesson: LessonDto, completed: Boolean, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Green)) { Column(Modifier.padding(20.dp)) { Pill(if (completed) "Повторение" else "Урок ${lesson.orderIndex}", Color.White, Green); Spacer(Modifier.height(30.dp)); Text(lesson.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); Text(lesson.subtitle, color = Color.White); Spacer(Modifier.height(18.dp)); Text("${lesson.estimatedMinutes} минут · ${lesson.xp} XP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } } }

@Composable
private fun PracticeCard(icon: String, title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) { Card(modifier.height(124.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Column(Modifier.padding(18.dp)) { Text(icon, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } } }

@Composable
private fun LessonRow(number: String, title: String, subtitle: String, active: Boolean, locked: Boolean, onClick: () -> Unit) {
    val complete = number == "✓"
    Card(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(enabled = !locked, onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(if (active) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface), border = BorderStroke(if (active) 2.dp else 1.dp, if (active) Green else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (complete) Green else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Text(if (locked) "🔒" else number, color = if (complete) Color.White else if (locked) MaterialTheme.colorScheme.onSurfaceVariant else Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold) }; Column(Modifier.padding(start = 16.dp)) { Text(title, color = if (locked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } }
    }
}

@Composable private fun ProfileStats(data: TutorData) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) { ProfileStat(data.profile.streakDays.toString(), "дней подряд", Yellow); ProfileStat(data.completedLessons.toString(), "уроков", Green); ProfileStat("${data.accuracy}%", "точность", Blue) } } }
@Composable private fun ProfileStat(value: String, label: String, color: Color) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp) } }

@Composable
private fun WordRow(word: VocabularyDto, saved: Boolean, onSpeak: () -> Unit, onToggle: () -> Unit) { Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(word.word, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); Text(word.transcription, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; Text(word.translation, color = Green, fontSize = 13.sp); Spacer(Modifier.height(5.dp)); Text(word.example, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; Text("▶", color = Blue, fontSize = 18.sp, modifier = Modifier.padding(8.dp).clickable(onClick = onSpeak)); Text(if (saved) "✓" else "+", color = if (saved) Green else MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp).clickable(onClick = onToggle)) } } }
@Composable private fun CompactWordRow(word: String, translation: String) { Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) { Text(word, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(translation, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun AuthMessage(text: String, isError: Boolean) { Text(text, color = if (isError) MaterialTheme.colorScheme.error else DarkGreen, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp)) }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun normalizeEmail(value: String) = value.trim().lowercase().replace(" ", "")
private fun normalizeName(value: String) = value.trim().replace(Regex("\\s+"), " ")
internal fun nextLevelCode(level: String): String? = when (level) { "A0" -> "A1"; "A1" -> "A2"; "A2" -> "B1"; "B1" -> "B2"; "B2" -> "C1"; else -> null }
private fun isValidName(value: String) = value.length in 2..40 && Regex("^[\\p{L}][\\p{L} '\\-]*$").matches(value)
private fun isValidEmail(value: String) = value.isNotBlank() && value.all { it.code in 33..126 } && Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$").matches(value)

private fun friendlyAuthError(error: Throwable, isSignUp: Boolean): String { val message = error.message.orEmpty().lowercase(); return when { "invalid format" in message || "invalid email" in message -> "Email введён неверно. Используйте только латинские буквы"; "already registered" in message || "user already exists" in message -> "Аккаунт с таким email уже существует"; "invalid login credentials" in message -> "Неверная электронная почта или пароль"; "email not confirmed" in message -> "Сначала подтвердите email по ссылке из письма"; "network" in message || "unable to resolve host" in message -> "Нет соединения с интернетом"; isSignUp -> "Не удалось создать аккаунт. Попробуйте ещё раз"; else -> "Не удалось войти. Проверьте данные" } }
internal fun friendlyDataError(error: Throwable): String { val message = error.message.orEmpty().lowercase(); return when { "network" in message || "unable to resolve host" in message -> "Проверьте подключение к интернету"; "jwt" in message || "unauthorized" in message -> "Сессия истекла. Войдите заново"; else -> "Ошибка синхронизации с Supabase" } }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun LoginPreview() { TutorTheme { LoginScreen({}, {}) } }
