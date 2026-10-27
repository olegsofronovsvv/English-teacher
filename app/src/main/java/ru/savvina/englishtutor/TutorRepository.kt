package ru.savvina.englishtutor

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant

@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("level_code") val levelCode: String,
    @SerialName("total_xp") val totalXp: Int = 0,
    @SerialName("streak_days") val streakDays: Int = 0,
    val bio: String = "",
    @SerialName("avatar_path") val avatarPath: String = "",
)

@Serializable
data class LessonDto(
    val id: Long,
    @SerialName("level_code") val levelCode: String,
    @SerialName("order_index") val orderIndex: Int,
    val title: String,
    @SerialName("title_en") val titleEn: String = "",
    val subtitle: String,
    val theory: String,
    @SerialName("rule_text") val ruleText: String,
    val examples: List<String>,
    val xp: Int,
    @SerialName("estimated_minutes") val estimatedMinutes: Int,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("video_url") val videoUrl: String = "",
)

@Serializable
data class ExerciseDto(
    val id: Long,
    @SerialName("lesson_id") val lessonId: Long,
    @SerialName("order_index") val orderIndex: Int,
    val prompt: String,
    val question: String,
    val options: List<String>,
    @SerialName("correct_index") val correctIndex: Int,
    val explanation: String,
)

@Serializable
data class ProgressDto(
    @SerialName("user_id") val userId: String,
    @SerialName("lesson_id") val lessonId: Long,
    val status: String,
    @SerialName("correct_answers") val correctAnswers: Int = 0,
    @SerialName("total_answers") val totalAnswers: Int = 0,
)

@Serializable
data class VocabularyDto(
    val id: Long,
    @SerialName("level_code") val levelCode: String,
    val word: String,
    val transcription: String,
    val translation: String,
    val example: String,
)

@Serializable
private data class ProgressWrite(
    @SerialName("user_id") val userId: String,
    @SerialName("lesson_id") val lessonId: Long,
    val status: String,
    @SerialName("correct_answers") val correctAnswers: Int,
    @SerialName("total_answers") val totalAnswers: Int,
    @SerialName("completed_at") val completedAt: String?,
)

@Serializable
private data class SavedWordWrite(
    @SerialName("user_id") val userId: String,
    @SerialName("vocabulary_id") val vocabularyId: Long,
)

@Serializable
private data class ProfileCreate(
    val id: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("level_code") val levelCode: String,
)

data class TutorData(
    val profile: ProfileDto,
    val lessons: List<LessonDto>,
    val exercises: List<ExerciseDto>,
    val progress: List<ProgressDto>,
    val vocabulary: List<VocabularyDto>,
    val savedVocabularyIds: Set<Long>,
) {
    val completedLessonIds: Set<Long> = progress.filter { it.status == "completed" }.map { it.lessonId }.toSet()
    val completedLessons: Int = completedLessonIds.size
    val accuracy: Int = progress.sumOf { it.totalAnswers }.let { total ->
        if (total == 0) 0 else (progress.sumOf { it.correctAnswers } * 100 / total)
    }
}

object TutorRepository {
    private val client get() = SupabaseProvider.client

    private fun requireUserId(): String =
        client.auth.currentUserOrNull()?.id ?: error("Сессия пользователя не найдена")

    suspend fun load(): TutorData {
        val userId = requireUserId()
        val profile = getOrCreateProfile(userId)

        val lessons = client.from("lessons").select {
            filter { eq("level_code", profile.levelCode) }
        }.decodeList<LessonDto>().sortedBy { it.orderIndex }

        val exercises = client.from("lesson_exercises").select()
            .decodeList<ExerciseDto>()
            .filter { exercise -> lessons.any { it.id == exercise.lessonId } }
            .sortedWith(compareBy<ExerciseDto> { it.lessonId }.thenBy { it.orderIndex })

        val progress = client.from("user_progress").select {
            filter { eq("user_id", userId) }
        }.decodeList<ProgressDto>().filter { item -> lessons.any { it.id == item.lessonId } }

        val vocabulary = client.from("vocabulary").select {
            filter { eq("level_code", profile.levelCode) }
        }.decodeList<VocabularyDto>().sortedBy { it.word }

        val savedIds = client.from("user_vocabulary").select {
            filter { eq("user_id", userId) }
        }.decodeList<SavedWordWrite>().map { it.vocabularyId }.toSet()

        return TutorData(profile, lessons, exercises, progress, vocabulary, savedIds)
    }

    private suspend fun getOrCreateProfile(userId: String): ProfileDto {
        suspend fun readProfile() = client.from("profiles").select {
            filter { eq("id", userId) }
        }

        readProfile().decodeList<ProfileDto>().firstOrNull()?.let { return it }

        val user = client.auth.currentUserOrNull() ?: error("Сессия пользователя не найдена")
        val metadata = user.userMetadata
        val rawName = metadata?.get("name")?.jsonPrimitive?.contentOrNull.orEmpty()
        val fallbackName = user.email?.substringBefore('@').orEmpty()
        val displayName = rawName.trim().replace(Regex("\\s+"), " ")
            .takeIf { it.length >= 2 }
            ?.take(60)
            ?: fallbackName.takeIf { it.length >= 2 }?.take(60)
            ?: "Ученик"
        val levelCode = metadata?.get("level")?.jsonPrimitive?.contentOrNull
            ?.uppercase()
            ?.takeIf { it in setOf("A0", "A1", "A2", "B1", "B2", "C1") }
            ?: "A0"

        runCatching {
            client.from("profiles").insert(ProfileCreate(userId, displayName, levelCode))
        }

        return readProfile().decodeList<ProfileDto>().firstOrNull()
            ?: error("Не удалось восстановить профиль")
    }

    suspend fun completeLesson(lesson: LessonDto, correct: Int, total: Int) {
        val userId = requireUserId()
        client.from("user_progress").upsert(
            ProgressWrite(
                userId = userId,
                lessonId = lesson.id,
                status = "completed",
                correctAnswers = correct,
                totalAnswers = total,
                completedAt = Instant.now().toString(),
            ),
        ) { onConflict = "user_id,lesson_id" }
    }

    suspend fun setWordSaved(vocabularyId: Long, saved: Boolean) {
        val userId = requireUserId()
        if (saved) {
            client.from("user_vocabulary").upsert(
                SavedWordWrite(userId, vocabularyId),
            ) { onConflict = "user_id,vocabulary_id" }
        } else {
            client.from("user_vocabulary").delete {
                filter {
                    eq("user_id", userId)
                    eq("vocabulary_id", vocabularyId)
                }
            }
        }
    }

    suspend fun updateLevel(levelCode: String) {
        val userId = requireUserId()
        client.from("profiles").update({ set("level_code", levelCode) }) {
            filter { eq("id", userId) }
        }
    }

    suspend fun updateBio(bio: String) {
        val userId = requireUserId()
        val cleanBio = bio.trim().replace(Regex("\\s+"), " ").take(200)
        client.from("profiles").update({ set("bio", cleanBio) }) {
            filter { eq("id", userId) }
        }
    }

    suspend fun uploadAvatar(bytes: ByteArray, mimeType: String, extension: String) {
        require(bytes.isNotEmpty()) { "Файл изображения пуст" }
        require(bytes.size <= 5 * 1024 * 1024) { "Размер изображения не должен превышать 5 МБ" }

        val userId = requireUserId()
        val safeExtension = extension.lowercase().takeIf { it in setOf("jpg", "jpeg", "png", "webp") } ?: "jpg"
        val path = "$userId/avatar-${System.currentTimeMillis()}.$safeExtension"
        val contentType = when (mimeType.lowercase()) {
            "image/png" -> ContentType.Image.PNG
            "image/webp" -> ContentType("image", "webp")
            else -> ContentType.Image.JPEG
        }

        client.storage["avatars"].upload(path, bytes) {
            upsert = false
            this.contentType = contentType
        }
        client.from("profiles").update({ set("avatar_path", path) }) {
            filter { eq("id", userId) }
        }
    }

    fun avatarPublicUrl(path: String): String? =
        path.takeIf { it.isNotBlank() }?.let { client.storage["avatars"].publicUrl(it) }
}
