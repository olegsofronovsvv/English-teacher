package ru.savvina.englishtutor

import android.annotation.SuppressLint
import android.os.Bundle
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

private enum class Screen { Login, Register, Home, Course, Lesson, Video, Exercise, Vocabulary, Profile }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { EnglishTutorApp() }
    }
}

@Composable
private fun EnglishTutorApp() {
    var screen by remember { mutableStateOf<Screen?>(null) }
    var data by remember { mutableStateOf<TutorData?>(null) }
    var selectedLesson by remember { mutableStateOf<LessonDto?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val authReady = runCatching {
            SupabaseProvider.client.auth.awaitInitialization()
            if (SupabaseProvider.client.auth.currentSessionOrNull() != null) {
                SupabaseProvider.client.auth.refreshCurrentSession()
            }
        }.isSuccess
        screen = if (authReady && SupabaseProvider.client.auth.currentSessionOrNull() != null) Screen.Home else Screen.Login
    }
    LaunchedEffect(screen, refreshKey) {
        val current = screen
        if (current != null && current !in listOf(Screen.Login, Screen.Register) && SupabaseProvider.client.auth.currentSessionOrNull() != null) {
            loadError = null
            runCatching { TutorRepository.load() }.onSuccess { data = it }.onFailure {
                Log.e("EnglishTutor", "Failed to load learning data", it)
                loadError = friendlyDataError(it)
            }
        }
    }
    fun signedIn() { data = null; refreshKey++; screen = Screen.Home }

    TutorTheme {
        Surface(Modifier.fillMaxSize(), color = Background) {
            when (val current = screen) {
                null -> LoadingScreen()
                Screen.Login -> LoginScreen(::signedIn) { screen = Screen.Register }
                Screen.Register -> RegisterScreen({ screen = Screen.Login }, ::signedIn)
                else -> {
                    val loaded = data
                    if (loaded == null) DataStateScreen(loadError) { refreshKey++ }
                    else when (current) {
                        Screen.Home -> HomeScreen(loaded, { screen = it }) { selectedLesson = it; screen = Screen.Lesson }
                        Screen.Course -> CourseScreen(loaded, { screen = it }) { selectedLesson = it; screen = Screen.Lesson }
                        Screen.Lesson -> {
                            val lesson = selectedLesson ?: loaded.lessons.firstOrNull()
                            if (lesson == null) DataStateScreen("Для этого уровня пока нет уроков") { screen = Screen.Profile }
                            else LessonScreen(
                                lesson = lesson,
                                onBack = { screen = Screen.Course },
                                onVideo = { screen = Screen.Video },
                                onStart = { screen = Screen.Exercise },
                            )
                        }
                        Screen.Video -> {
                            val lesson = selectedLesson ?: loaded.lessons.firstOrNull()
                            if (lesson == null) DataStateScreen("Для этого уровня пока нет уроков") { screen = Screen.Course }
                            else VideoLessonScreen(lesson) { screen = Screen.Lesson }
                        }
                        Screen.Exercise -> {
                            val lesson = selectedLesson ?: loaded.lessons.firstOrNull()
                            val exercises = lesson?.let { item -> loaded.exercises.filter { it.lessonId == item.id } }.orEmpty()
                            if (lesson == null || exercises.isEmpty()) DataStateScreen("Для урока пока нет упражнений") { screen = Screen.Course }
                            else ExerciseScreen(lesson, exercises, { screen = Screen.Lesson }) { correct, total ->
                                scope.launch {
                                    runCatching {
                                        TutorRepository.completeLesson(lesson, correct, total)
                                        val finishedLevel = loaded.lessons.all { item ->
                                            item.id == lesson.id || item.id in loaded.completedLessonIds
                                        }
                                        if (finishedLevel) {
                                            nextLevelCode(loaded.profile.levelCode)?.let { TutorRepository.updateLevel(it) }
                                        }
                                    }.onSuccess { refreshKey++; screen = Screen.Home }
                                        .onFailure { loadError = friendlyDataError(it) }
                                }
                            }
                        }
                        Screen.Vocabulary -> VocabularyScreen(loaded, { screen = it }) { word, save ->
                            scope.launch {
                                runCatching { TutorRepository.setWordSaved(word.id, save) }
                                    .onSuccess { refreshKey++ }.onFailure { loadError = friendlyDataError(it) }
                            }
                        }
                        Screen.Profile -> ProfileScreen(
                            data = loaded,
                            onNavigate = { screen = it },
                            onLevelChange = { newLevel ->
                                scope.launch {
                                    runCatching { TutorRepository.updateLevel(newLevel) }.onSuccess {
                                        selectedLesson = null; data = null; refreshKey++
                                    }.onFailure { loadError = friendlyDataError(it) }
                                }
                            },
                            onBioSave = { bio ->
                                TutorRepository.updateBio(bio)
                                data = null; refreshKey++
                            },
                            onAvatarUpload = { bytes, mimeType, extension ->
                                TutorRepository.uploadAvatar(bytes, mimeType, extension)
                                data = null; refreshKey++
                            },
                        ) { data = null; selectedLesson = null; screen = Screen.Login }
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = lightColorScheme(primary = Green, secondary = Blue, background = Background, surface = Color.White, onSurface = Ink),
    typography = Typography(bodyLarge = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = Ink)), content = content,
)

@Composable private fun LoadingScreen() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Green) }

@Composable
private fun DataStateScreen(error: String?, retry: () -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (error == null) CircularProgressIndicator(color = Green) else {
            Text("Не удалось загрузить данные", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp)); Text(error, color = Muted); Spacer(Modifier.height(22.dp)); PrimaryButton("Повторить", onClick = retry)
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
                Spacer(Modifier.height(18.dp)); Text(lesson.title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("Открыть видеоурок British Council", color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(30.dp)); SectionTitle("Объяснение"); Spacer(Modifier.height(10.dp)); BodyMuted(lesson.theory)
        Spacer(Modifier.height(22.dp)); SectionTitle("Главное правило"); Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column(Modifier.padding(18.dp)) { Text(lesson.ruleText, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold); lesson.examples.forEach { Spacer(Modifier.height(10.dp)); Text("• $it", color = Muted, lineHeight = 20.sp) } }
        }
        Spacer(Modifier.height(12.dp)); OutlinedButton(onClick = { speaker(lesson.examples.joinToString(". ")) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) { Text("Прослушать все примеры", color = Blue, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(24.dp)); PrimaryButton("Начать упражнения", onClick = onStart)
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun VideoLessonScreen(lesson: LessonDto, onBack: () -> Unit) {
    var isLoading by remember(lesson.id) { mutableStateOf(true) }
    var errorMessage by remember(lesson.id) { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.loadUrl("about:blank")
            webView?.clearHistory()
            webView?.destroy()
            webView = null
        }
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().background(Background)) {
        HeaderBack("Видеоурок", onBack, Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        Text(lesson.title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Ink)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        webView = this
                        setBackgroundColor(android.graphics.Color.rgb(15, 32, 39))
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.setSupportMultipleWindows(false)
                        settings.mediaPlaybackRequiresUserGesture = true
                        settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.loadsImagesAutomatically = false
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                if (request?.isForMainFrame != true) return false
                                val host = request.url.host.orEmpty().lowercase()
                                return host != "learnenglish.britishcouncil.org" && !host.endsWith(".britishcouncil.org")
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                view?.settings?.loadsImagesAutomatically = true
                                view?.evaluateJavascript(SIMPLE_VIDEO_SCRIPT, null)
                                view?.postDelayed({ view.evaluateJavascript(SIMPLE_VIDEO_SCRIPT, null) }, 700)
                                isLoading = false
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? =
                                if (request?.url?.toString()?.isHeavyPageResource() == true) emptyWebResource() else null

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    errorMessage = "Не удалось загрузить видео. Проверьте интернет."
                                }
                            }
                        }
                        loadUrl(lesson.videoUrl)
                    }
                },
                update = { view ->
                    if (view.url == null || view.url == "about:blank") view.loadUrl(lesson.videoUrl)
                },
            )
            if (isLoading) CircularProgressIndicator(color = Green, modifier = Modifier.align(Alignment.Center))
            errorMessage?.let { message ->
                Column(Modifier.align(Alignment.Center).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(message, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { errorMessage = null; isLoading = true; webView?.reload() }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Повторить") }
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
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
                colors = CardDefaults.cardColors(Color.White),
                border = BorderStroke(1.dp, Border),
            ) {
                Text(
                    text = lesson.subtitle.ifBlank { lesson.theory },
                    color = Muted,
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
                    Text(example, color = Ink, lineHeight = 21.sp, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Вернуться к уроку", onClick = onBack)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun VideoInfoCard(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, Border),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Green, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(label, color = Muted, fontSize = 10.sp)
        }
    }
}

private fun emptyWebResource() = WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))

private fun String.isHeavyPageResource(): Boolean {
    val host = runCatching { Uri.parse(this).host.orEmpty().lowercase() }.getOrDefault("")
    val isLessonPage = host == "learnenglish.britishcouncil.org"
    val clean = substringBefore('?').lowercase()
    return isLessonPage && (clean.endsWith(".css") || clean.endsWith(".woff") || clean.endsWith(".woff2") ||
        clean.endsWith(".jpg") || clean.endsWith(".jpeg") || clean.endsWith(".png") ||
        clean.endsWith(".gif") || clean.endsWith(".webp") || clean.endsWith(".svg")) ||
        contains("google-analytics") || contains("googletagmanager") || contains("doubleclick")
}

private const val SIMPLE_VIDEO_SCRIPT = """
(() => {
  if (window.__simpleVideoReady) return;
  const media = document.querySelector(
    'video, iframe[src*="youtube"], iframe[src*="vimeo"], iframe[src*="kaltura"], iframe[src*="video"]'
  );
  if (!media) return;
  window.__simpleVideoReady = true;
  const player = media.tagName === 'VIDEO' && media.closest('.video-js') ? media.closest('.video-js') : media;
  const shell = document.createElement('main');
  shell.style.cssText = 'position:fixed;inset:0;display:flex;align-items:center;justify-content:center;background:#0f2027;overflow:hidden';
  player.style.cssText = 'display:block;width:100%;height:100%;max-width:100%;max-height:100%;border:0;background:#0f2027';
  if (player.tagName === 'VIDEO') player.setAttribute('controls', 'controls');
  shell.appendChild(player);
  document.body.replaceChildren(shell);
  document.documentElement.style.cssText = 'margin:0;background:#0f2027;overflow:hidden';
  document.body.style.cssText = 'margin:0;background:#0f2027;overflow:hidden';
})();
"""

@Composable
private fun ExerciseScreen(lesson: LessonDto, exercises: List<ExerciseDto>, onClose: () -> Unit, onFinish: (Int, Int) -> Unit) {
    var index by remember { mutableIntStateOf(0) }; var selected by remember { mutableIntStateOf(-1) }; var checked by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }; var isFinishing by remember { mutableStateOf(false) }; var retryMessage by remember { mutableStateOf<String?>(null) }; val exercise = exercises[index]; val isCorrect = selected == exercise.correctIndex
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Text("Question ${index + 1} of ${exercises.size}", color = Muted, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)); Text("×", color = Muted, fontSize = 32.sp, modifier = Modifier.clickable(onClick = onClose)) }
        Spacer(Modifier.height(18.dp)); Progress((index + 1).toFloat() / exercises.size); Spacer(Modifier.height(32.dp)); Pill("Practice", PaleBlue, Blue)
        retryMessage?.let { message -> Spacer(Modifier.height(14.dp)); Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFF4CC)).padding(14.dp)) { Text(message, color = Ink, fontWeight = FontWeight.SemiBold) } }
        Spacer(Modifier.height(30.dp)); Title(exercise.prompt); Spacer(Modifier.height(18.dp)); Text(exercise.question, color = DarkGreen, fontSize = 21.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(26.dp))
        exercise.options.forEachIndexed { optionIndex, answer ->
            Answer(('A'.code + optionIndex).toChar().toString(), answer, selected == optionIndex, when { !checked -> AnswerState.Normal; optionIndex == exercise.correctIndex -> AnswerState.Correct; optionIndex == selected -> AnswerState.Wrong; else -> AnswerState.Normal }) { if (!checked) selected = optionIndex }
        }
        if (checked) { Spacer(Modifier.height(12.dp)); Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (isCorrect) PaleGreen else Color(0xFFFFECEA)).padding(16.dp)) {
            Column { Text(if (isCorrect) "Correct!" else "Correct answer: ${exercise.options[exercise.correctIndex]}", color = if (isCorrect) DarkGreen else ErrorRed, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(exercise.explanation, color = Muted, fontSize = 13.sp) }
        } }
        Spacer(Modifier.height(28.dp)); PrimaryButton(when { isFinishing -> "Saving…"; !checked -> "Check"; index == exercises.lastIndex -> "Finish lesson"; else -> "Next question" }, enabled = selected >= 0 && !isFinishing) {
            if (!checked) { checked = true; if (isCorrect) correctCount++ }
            else if (index == exercises.lastIndex) {
                val score = correctCount * 100 / exercises.size
                if (score >= 70) { isFinishing = true; onFinish(correctCount, exercises.size) }
                else {
                    retryMessage = "Score: $correctCount of ${exercises.size}. You need at least 70%. Try these questions again."
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
                    placeholder = { Text("Расскажите о своих целях в английском", color = Muted) },
                    supportingText = { Text("${bio.length}/200") },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Border, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White),
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
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
                    Text(
                        text = data.profile.bio.ifBlank { "Описание пока не добавлено" },
                        color = if (data.profile.bio.isBlank()) Muted else Ink,
                        lineHeight = 21.sp,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            profileMessage?.let { AuthMessage(it, false) }; profileError?.let { AuthMessage(it, true) }
            Spacer(Modifier.height(30.dp)); SectionTitle("Уровень английского"); BodyMuted("Выберите курс, который хотите проходить"); LevelSelector(data.profile.levelCode, onLevelChange)
            Spacer(Modifier.height(30.dp)); Row(verticalAlignment = Alignment.CenterVertically) { SectionTitle("Личный словарь", Modifier.weight(1f)); Pill("${data.savedVocabularyIds.size} слов", PaleGreen, Green) }
            Spacer(Modifier.height(14.dp)); data.vocabulary.filter { it.id in data.savedVocabularyIds }.take(4).forEach { CompactWordRow(it.word, it.translation) }
            if (data.savedVocabularyIds.isEmpty()) BodyMuted("Сохраните слова во вкладке «Словарь»")
            TextButton(onClick = { onNavigate(Screen.Vocabulary) }) { Text("Открыть весь словарь", color = Green) }; logoutError?.let { AuthMessage(it, true) }; Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { if (!isSigningOut) scope.launch { isSigningOut = true; logoutError = null; runCatching { SupabaseProvider.client.auth.signOut() }.onSuccess { onLogout() }.onFailure { logoutError = "Не удалось выйти. Проверьте интернет" }; isSigningOut = false } }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(if (isSigningOut) "Выходим…" else "Выйти из аккаунта", fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable private fun ScreenScaffold(current: Screen, onNavigate: (Screen) -> Unit, content: @Composable (PaddingValues) -> Unit) { Scaffold(containerColor = Background, bottomBar = { BottomNav(current, onNavigate) }, content = content) }

@Composable
private fun BottomNav(current: Screen, onNavigate: (Screen) -> Unit) {
    val items = listOf(Triple(Screen.Home, "●", "Главная"), Triple(Screen.Course, "▦", "Курс"), Triple(Screen.Vocabulary, "Aa", "Словарь"), Triple(Screen.Profile, "○", "Профиль"))
    Row(Modifier.fillMaxWidth().height(82.dp).background(Color.White).border(1.dp, Border).navigationBarsPadding(), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { item -> val active = current == item.first; Column(Modifier.weight(1f).clickable { onNavigate(item.first) }, horizontalAlignment = Alignment.CenterHorizontally) { Text(item.second, color = if (active) Green else Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(4.dp)); Text(item.third, color = if (active) Green else Muted, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal, fontSize = 11.sp) } }
    }
}

private enum class AnswerState { Normal, Correct, Wrong }

@Composable
private fun Answer(letter: String, text: String, selected: Boolean, state: AnswerState, onClick: () -> Unit) {
    val color = when (state) { AnswerState.Correct -> Green; AnswerState.Wrong -> ErrorRed; AnswerState.Normal -> if (selected) Green else Border }
    val background = when (state) { AnswerState.Correct -> PaleGreen; AnswerState.Wrong -> Color(0xFFFFECEA); AnswerState.Normal -> if (selected) PaleGreen else Color.White }
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp)).background(background).border(if (selected || state != AnswerState.Normal) 2.dp else 1.dp, color, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(letter, color = if (selected || state != AnswerState.Normal) color else Muted, fontSize = 18.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(18.dp)); Text(text, color = Ink, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun LevelSelector(selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("A0", "A1", "A2", "B1", "B2", "C1").forEach { level -> Box(Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(14.dp)).background(if (selected == level) Green else Color.White).border(1.dp, if (selected == level) Green else Border, RoundedCornerShape(14.dp)).clickable { onSelect(level) }, contentAlignment = Alignment.Center) { Text(level, color = if (selected == level) Color.White else Ink, fontWeight = FontWeight.Bold) } }
    }
}

@Composable
private fun rememberSpeaker(): (String) -> Unit {
    val context = LocalContext.current; val speaker = remember { TextToSpeech(context) {} }
    DisposableEffect(speaker) { onDispose { speaker.stop(); speaker.shutdown() } }
    return { text -> speaker.language = Locale.US; speaker.speak(text, TextToSpeech.QUEUE_FLUSH, null, "english_tutor") }
}

@Composable private fun Title(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
@Composable private fun SectionTitle(text: String, modifier: Modifier = Modifier) = Text(text, modifier, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
@Composable private fun BodyMuted(text: String) = Text(text, color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
@Composable private fun FieldLabel(text: String) { Spacer(Modifier.height(14.dp)); Text(text, color = Ink, fontSize = 12.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)) }
@Composable private fun HeaderBack(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) { Row(modifier, verticalAlignment = Alignment.CenterVertically) { Text("‹", fontSize = 36.sp, modifier = Modifier.clickable(onClick = onBack)); Spacer(Modifier.width(10.dp)); Title(title) } }

@Composable
private fun TutorField(value: String, onValueChange: (String) -> Unit, hint: String, keyboardType: KeyboardType = KeyboardType.Text, password: Boolean = false) {
    OutlinedTextField(value, onValueChange, Modifier.fillMaxWidth().height(58.dp), placeholder = { Text(hint, color = Muted, fontSize = 14.sp) }, singleLine = true, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Border, focusedContainerColor = Color.White, unfocusedContainerColor = Color.White), keyboardOptions = KeyboardOptions(keyboardType = keyboardType), visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
}

@Composable private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) { Button(onClick, Modifier.fillMaxWidth().height(58.dp), enabled = enabled, shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun Pill(text: String, background: Color, color: Color) { Box(Modifier.clip(RoundedCornerShape(17.dp)).background(background).padding(horizontal = 13.dp, vertical = 8.dp)) { Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
@Composable private fun Progress(value: Float) { Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(Color(0xFFE3E9E4))) { Box(Modifier.fillMaxWidth(value.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape).background(Green)) } }

@Composable private fun StatsCard(data: TutorData) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween) { Stat("${data.profile.totalXp} XP", "общий опыт", DarkGreen); Stat(data.profile.levelCode, "уровень", Blue); Stat(data.completedLessons.toString(), "уроков", Ink) } } }
@Composable private fun Stat(value: String, label: String, color: Color) { Column { Text(value, color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(label, color = Muted, fontSize = 12.sp) } }

@Composable
private fun ContinueCard(lesson: LessonDto, completed: Boolean, onClick: () -> Unit) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Green)) { Column(Modifier.padding(20.dp)) { Pill(if (completed) "Повторение" else "Урок ${lesson.orderIndex}", Color.White, Green); Spacer(Modifier.height(30.dp)); Text(lesson.title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(7.dp)); Text(lesson.subtitle, color = Color.White); Spacer(Modifier.height(18.dp)); Text("${lesson.estimatedMinutes} минут · ${lesson.xp} XP", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) } } }

@Composable
private fun PracticeCard(icon: String, title: String, subtitle: String, color: Color, modifier: Modifier, onClick: () -> Unit) { Card(modifier.height(124.dp).clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(18.dp)) { Text(icon, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text(title, color = Ink, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(subtitle, color = Muted, fontSize = 12.sp) } } }

@Composable
private fun LessonRow(number: String, title: String, subtitle: String, active: Boolean, locked: Boolean, onClick: () -> Unit) {
    val complete = number == "✓"
    Card(Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable(enabled = !locked, onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(if (active) PaleGreen else Color.White), border = androidx.compose.foundation.BorderStroke(if (active) 2.dp else 1.dp, if (active) Green else Border)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(if (complete) Green else if (locked) Color(0xFFF0F2F0) else PaleBlue), contentAlignment = Alignment.Center) { Text(if (locked) "🔒" else number, color = if (complete) Color.White else if (locked) Muted else Blue, fontSize = 18.sp, fontWeight = FontWeight.Bold) }; Column(Modifier.padding(start = 16.dp)) { Text(title, color = if (locked) Muted else Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(subtitle, color = Muted, fontSize = 12.sp) } }
    }
}

@Composable private fun ProfileStats(data: TutorData) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.fillMaxWidth().padding(vertical = 20.dp), horizontalArrangement = Arrangement.SpaceEvenly) { ProfileStat(data.profile.streakDays.toString(), "дней подряд", Yellow); ProfileStat(data.completedLessons.toString(), "уроков", Green); ProfileStat("${data.accuracy}%", "точность", Blue) } } }
@Composable private fun ProfileStat(value: String, label: String, color: Color) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(value, color = color, fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(label, color = Muted, fontSize = 11.sp) } }

@Composable
private fun WordRow(word: VocabularyDto, saved: Boolean, onSpeak: () -> Unit, onToggle: () -> Unit) { Card(Modifier.fillMaxWidth().padding(bottom = 12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(word.word, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.width(8.dp)); Text(word.transcription, color = Muted, fontSize = 12.sp) }; Text(word.translation, color = DarkGreen, fontSize = 13.sp); Spacer(Modifier.height(5.dp)); Text(word.example, color = Muted, fontSize = 12.sp) }; Text("▶", color = Blue, fontSize = 18.sp, modifier = Modifier.padding(8.dp).clickable(onClick = onSpeak)); Text(if (saved) "✓" else "+", color = if (saved) Green else Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp).clickable(onClick = onToggle)) } } }
@Composable private fun CompactWordRow(word: String, translation: String) { Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) { Text(word, color = Ink, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(translation, color = Muted) } }
@Composable private fun AuthMessage(text: String, isError: Boolean) { Text(text, color = if (isError) MaterialTheme.colorScheme.error else DarkGreen, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp)) }

private fun normalizeEmail(value: String) = value.trim().lowercase().replace(" ", "")
private fun normalizeName(value: String) = value.trim().replace(Regex("\\s+"), " ")
private fun nextLevelCode(level: String): String? = when (level) { "A0" -> "A1"; "A1" -> "A2"; "A2" -> "B1"; "B1" -> "B2"; "B2" -> "C1"; else -> null }
private fun isValidName(value: String) = value.length in 2..40 && Regex("^[\\p{L}][\\p{L} '\\-]*$").matches(value)
private fun isValidEmail(value: String) = value.isNotBlank() && value.all { it.code in 33..126 } && Regex("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+$").matches(value)

private fun friendlyAuthError(error: Throwable, isSignUp: Boolean): String { val message = error.message.orEmpty().lowercase(); return when { "invalid format" in message || "invalid email" in message -> "Email введён неверно. Используйте только латинские буквы"; "already registered" in message || "user already exists" in message -> "Аккаунт с таким email уже существует"; "invalid login credentials" in message -> "Неверная электронная почта или пароль"; "email not confirmed" in message -> "Сначала подтвердите email по ссылке из письма"; "network" in message || "unable to resolve host" in message -> "Нет соединения с интернетом"; isSignUp -> "Не удалось создать аккаунт. Попробуйте ещё раз"; else -> "Не удалось войти. Проверьте данные" } }
private fun friendlyDataError(error: Throwable): String { val message = error.message.orEmpty().lowercase(); return when { "network" in message || "unable to resolve host" in message -> "Проверьте подключение к интернету"; "jwt" in message || "unauthorized" in message -> "Сессия истекла. Войдите заново"; else -> "Ошибка синхронизации с Supabase" } }

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable private fun LoginPreview() { TutorTheme { LoginScreen({}, {}) } }
