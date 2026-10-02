package com.fushengce.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "tasks")
data class TaskRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val realm: String = "work",
    val title: String,
    val address: String = "",
    val notes: String = "",
    val emergency: Boolean = false,
    val remindAtMillis: Long? = null,
    val completed: Boolean = false,
    val reminderRevision: String = UUID.randomUUID().toString(),
    val notifiedRevision: String? = null,
) {
    fun validated(): TaskRecord {
        require(realm == "work" || realm == "life") { "请选择工作或生活" }
        require(title.isNotBlank() && title.length <= 100) { "事项名称需为1至100字" }
        require(address.length <= 2000 && notes.length <= 4000) { "地址或备注过长，请缩短后保存" }
        require(remindAtMillis == null || remindAtMillis > 0) { "提醒时间无效" }
        return copy(title = title.trim(), address = address.trim(), notes = notes.trim(),
            emergency = emergency && realm == "work")
    }

    fun matches(query: String): Boolean {
        val words = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        return words.all { word ->
            listOf(title, address, notes).any { it.contains(word, ignoreCase = true) }
        }
    }

    fun reminderDue(now: Long, revision: String): Boolean =
        !completed && remindAtMillis != null && remindAtMillis <= now &&
            reminderRevision == revision && notifiedRevision != revision

    fun prepareForSave(previous: TaskRecord?, now: Long): TaskRecord {
        val task = validated()
        val changed = previous == null || previous.remindAtMillis != task.remindAtMillis ||
            previous.completed != task.completed
        require(task.completed || !changed || task.remindAtMillis == null || task.remindAtMillis > now) {
            "请选择将来的提醒时间"
        }
        return task.copy(
            reminderRevision = if (changed) UUID.randomUUID().toString() else previous!!.reminderRevision,
            notifiedRevision = if (changed) null else previous!!.notifiedRevision,
        )
    }
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY completed ASC, emergency DESC, remindAtMillis IS NULL ASC, remindAtMillis ASC, title ASC")
    fun observeAll(): Flow<List<TaskRecord>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun find(id: String): TaskRecord?

    @Query("SELECT * FROM tasks WHERE completed = 0 AND remindAtMillis IS NOT NULL AND (notifiedRevision IS NULL OR notifiedRevision != reminderRevision)")
    suspend fun pendingReminders(): List<TaskRecord>

    @Upsert
    suspend fun upsert(task: TaskRecord)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: String)
}
