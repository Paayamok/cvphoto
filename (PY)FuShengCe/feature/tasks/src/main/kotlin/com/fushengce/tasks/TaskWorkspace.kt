package com.fushengce.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.fushengce.background.TaskReminders
import com.fushengce.database.FuShengCeDatabase
import com.fushengce.database.TaskRecord
import com.fushengce.media.AndroidMediaRepository
import com.fushengce.media.MediaItem
import com.fushengce.metadata.LocalFavorites
import com.fushengce.metadata.LocalCaptions
import com.fushengce.search.SearchScreen
import com.fushengce.search.searchMedia
import com.fushengce.viewer.ViewerScreen
import com.fushengce.viewer.ViewerUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskWorkspace(
    onBack: () -> Unit,
    hasMediaAccess: Boolean,
    limitedMediaAccess: Boolean,
    onRequestMediaAccess: () -> Unit,
    onRequestNotifications: () -> Unit,
    refreshToken: Int,
    incomingText: String?,
    incomingTaskId: String?,
    onConsumeIncoming: () -> Unit,
    initialSearch: Boolean = false,
) {
    val context = LocalContext.current
    val dao = remember { FuShengCeDatabase.get(context).taskDao() }
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val taskFlow = remember(dao, retry) { dao.observeAll().catch { error = "事项读取失败，请重试" } }
    val tasks by taskFlow.collectAsState(initial = null)
    var realm by rememberSaveable { mutableStateOf("work") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var draftAddress by rememberSaveable { mutableStateOf("") }
    var searchOpen by rememberSaveable { mutableStateOf(initialSearch) }
    var query by rememberSaveable { mutableStateOf("") }
    var remindersOnly by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var media by remember { mutableStateOf<List<MediaItem>?>(null) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    var mediaRetry by remember { mutableIntStateOf(0) }
    var viewerId by rememberSaveable { mutableStateOf<Long?>(null) }
    var viewerItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }
    var controlsVisible by remember { mutableStateOf(true) }
    val selected = tasks?.firstOrNull { it.id == selectedId }
    val editing = tasks?.firstOrNull { it.id == editingId }
    val newDraft = remember(editingId) { TaskRecord(id = editingId.orEmpty(), realm = realm, title = "", address = draftAddress) }
    val repository = remember { AndroidMediaRepository(context.contentResolver) }
    val favoriteStore = remember { LocalFavorites(context) }
    val captionStore = remember { LocalCaptions(context) }
    var favorites by remember(refreshToken) { mutableStateOf(favoriteStore.read()) }
    var captions by remember(refreshToken) { mutableStateOf(captionStore.read()) }
    val notificationsAllowed = remember(refreshToken) { TaskReminders.canNotify(context) }
    val precise = remember(refreshToken) { TaskReminders.canSchedulePrecisely(context) }

    LaunchedEffect(incomingTaskId, editingId) {
        if (incomingTaskId != null && editingId == null) {
            selectedId = incomingTaskId
            searchOpen = false
            onConsumeIncoming()
        }
    }
    LaunchedEffect(incomingText, editingId) {
        if (incomingText != null && editingId == null) {
            searchOpen = false
            selectedId = null
        }
    }
    LaunchedEffect(searchOpen, hasMediaAccess, refreshToken, mediaRetry) {
        media = null
        mediaError = null
        viewerId = null
        if (searchOpen && hasMediaAccess) {
            try { media = repository.loadMedia() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mediaError = "照片读取失败，请重试" }
        }
    }
    fun updateTask(task: TaskRecord) {
        scope.launch {
            busy = true
            error = null
            try {
                TaskReminders.save(context, task)
                selectedId = task.id
                editingId = null
                if (task.remindAtMillis != null && !task.completed && !TaskReminders.canNotify(context)) {
                    onRequestNotifications()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "保存失败，请重试" }
            finally { busy = false }
        }
    }
    if (editingId != null) {
        TaskEditor(task = editing ?: newDraft, busy = busy, error = error,
            onBack = { if (!busy) { editingId = null; error = null } }, onSave = ::updateTask)
        return
    }
    BackHandler {
        when {
            viewerId != null -> viewerId = null
            selectedId != null -> selectedId = null
            searchOpen -> searchOpen = false
            else -> onBack()
        }
    }
    val activeMedia = viewerId
    if (activeMedia != null) {
        val content = if (hasMediaAccess && viewerItems.any { it.id == activeMedia }) {
            ViewerUiState.Content(activeMedia, viewerItems, controlsVisible)
        } else ViewerUiState.Unavailable(activeMedia)
        ViewerScreen(state = content, onBack = { viewerId = null }, onRetry = { mediaRetry++ },
            onMediaSelected = { viewerId = it }, onToggleControls = { controlsVisible = !controlsVisible },
            isFavorite = (content as? ViewerUiState.Content)?.currentItem?.uri?.let { it in favorites } == true,
            onToggleFavorite = { favorites = favoriteStore.toggle(it.uri) },
            caption = (content as? ViewerUiState.Content)?.currentItem?.uri?.let(captions::get).orEmpty(),
            onSaveCaption = { item, caption -> captions = captionStore.save(item.uri, caption) },
            backLabel = "返回搜索")
        return
    }
    if (searchOpen && selectedId == null) {
        SearchScreen(query = query, onQueryChange = { query = it }, media = media,
            error = mediaError, onRetry = { mediaRetry++ }, onBack = { searchOpen = false },
            onMediaClick = { item ->
                viewerItems = searchMedia(media.orEmpty(), query, favoriteUris = favorites, captions = captions)
                viewerId = item.id
                controlsVisible = true
            }, limitedAccess = limitedMediaAccess, favoriteUris = favorites, captions = captions,
            taskResults = { TaskSearchResults(tasks.orEmpty(), query) { selectedId = it.id } },
            hasMediaAccess = hasMediaAccess, onRequestMediaAccess = onRequestMediaAccess)
        return
    }
    if (deleteConfirmation && selected != null) {
        AlertDialog(onDismissRequest = { if (!busy) deleteConfirmation = false },
            title = { Text("删除这个事项？") },
            text = { Text("事项及其提醒将被删除，照片原文件不会改变。") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    try {
                        TaskReminders.delete(context, selected.id)
                        deleteConfirmation = false
                        selectedId = null
                    } catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { error = "删除失败，请重试"; deleteConfirmation = false }
                    finally { busy = false }
                }
            }) { Text("删除事项") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleteConfirmation = false }) { Text("取消") } })
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(if (selectedId != null) "事项详情" else "工作与生活") },
            navigationIcon = { TextButton(onClick = {
                if (selectedId != null) selectedId = null else onBack()
            }) { Text("返回") } },
            actions = { TextButton(onClick = { selectedId = null; searchOpen = true }) { Text("语音搜索") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            error?.let { message -> item {
                Text(message, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { error = null; retry++ }) { Text("重试读取") }
            } }
            if (!incomingText.isNullOrBlank()) item {
                Text("收到分享的地点或文字")
                Text(incomingText, maxLines = 4)
                if (incomingText.length > 2000) Text("分享内容超过2000字，请复制其中的完整地址后手动新建事项。")
                Row {
                    Button(enabled = incomingText.length <= 2000, onClick = {
                        realm = "work"; draftAddress = incomingText
                        editingId = UUID.randomUUID().toString(); onConsumeIncoming()
                    }) { Text("填入新工作事项") }
                    TextButton(onClick = onConsumeIncoming) { Text("忽略") }
                }
            }
            if (selectedId != null) {
                if (selected == null) item { Text(if (tasks == null) "正在读取事项…" else "此事项已删除或不存在") }
                else {
                    item {
                        Text(selected.title, style = MaterialTheme.typography.headlineSmall)
                        Text("${if (selected.realm == "work") "工作" else "生活"} · ${if (selected.completed) "已完成" else if (selected.emergency) "应急待办" else "待办"}")
                    }
                    item {
                        Text("地点", style = MaterialTheme.typography.titleMedium)
                        Text(selected.address.ifBlank { "未提供地点信息" })
                        val canNavigate = hasNavigableLocation(selected.address)
                        Button(enabled = canNavigate, onClick = { error = openTaskLocation(context, selected.address) }) {
                            Text(if (canNavigate) "查看地点并导航" else "暂无可用地点，无法导航")
                        }
                        if (canNavigate) Text("请在地图中核对具体目的地后开始导航", style = MaterialTheme.typography.bodySmall)
                    }
                    if (selected.notes.isNotBlank()) item { Text(selected.notes) }
                    item { Text(selected.remindAtMillis?.let { "提醒：${formatTaskTime(it)}" } ?: "未设置提醒") }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(enabled = !busy, onClick = { editingId = selected.id; error = null }) { Text("编辑") }
                            OutlinedButton(enabled = !busy, onClick = {
                                updateTask(selected.copy(completed = !selected.completed,
                                    remindAtMillis = if (selected.completed) null else selected.remindAtMillis))
                            }) { Text(if (selected.completed) "重新开启" else "完成事项") }
                        }
                        TextButton(enabled = !busy, onClick = { deleteConfirmation = true }) { Text("删除事项") }
                    }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = realm == "work", onClick = { realm = "work" }, label = { Text("工作") })
                        FilterChip(selected = realm == "life", onClick = { realm = "life" }, label = { Text("生活") })
                        FilterChip(selected = remindersOnly, onClick = { remindersOnly = !remindersOnly }, label = { Text("有提醒") })
                    }
                    Button(onClick = { draftAddress = ""; editingId = UUID.randomUUID().toString(); error = null }) {
                        Text(if (realm == "work") "新建工作事项" else "新建生活事项")
                    }
                }
                val visible = tasks.orEmpty().filter { it.realm == realm && (!remindersOnly || it.remindAtMillis != null && !it.completed) }
                if (tasks == null) item { Text("正在读取事项…") }
                else if (visible.isEmpty()) item { Text("这里还没有${if (remindersOnly) "待提醒" else ""}事项。可新建事项，粘贴微信地址并设置时间。") }
                items(visible, key = TaskRecord::id) { task -> TaskRow(task) { selectedId = task.id } }
            }
            item {
                HorizontalDivider()
                if (!notificationsAllowed) {
                    Text("通知未开启，事项会保存，但不能弹出提醒。")
                    TextButton(onClick = onRequestNotifications) { Text("开启事项通知") }
                }
                if (!precise) {
                    Text("提醒可能被系统延迟；需要按时提醒时，请允许精确闹钟。")
                    TextButton(onClick = {
                        if (Build.VERSION.SDK_INT >= 31) {
                            try { context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}"))) }
                            catch (_: Exception) { error = "请到系统设置中允许浮生册使用闹钟和提醒" }
                        }
                    }) { Text("设置精确提醒") }
                }
            }
        }
    }
}

@Composable
private fun TaskSearchResults(tasks: List<TaskRecord>, query: String, onTaskClick: (TaskRecord) -> Unit) {
    val matchingTasks = remember(tasks, query) { tasks.filter { it.matches(query) } }
    when {
        query.isBlank() -> Text("说出事项名称、地点或备注中的词，例如“东河”", Modifier.padding(16.dp))
        matchingTasks.isEmpty() -> Text("没有找到匹配的事项，可修改关键词", Modifier.padding(16.dp))
        else -> LazyColumn {
            items(matchingTasks, key = TaskRecord::id) { task ->
                TaskRow(task, onClick = { onTaskClick(task) })
            }
        }
    }
}

@Composable
internal fun TaskRow(task: TaskRecord, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 16.dp)) {
        Text(task.title, style = MaterialTheme.typography.titleMedium)
        Text("${if (task.realm == "work") "工作" else "生活"} · ${if (task.completed) "已完成" else if (task.emergency) "应急" else "待办"}",
            style = MaterialTheme.typography.bodySmall)
        if (task.address.isNotBlank()) Text(task.address, maxLines = 2)
        task.remindAtMillis?.let { Text(formatTaskTime(it), style = MaterialTheme.typography.bodySmall) }
    }
    HorizontalDivider()
}

internal fun formatTaskTime(millis: Long): String = Instant.ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm z"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditor(task: TaskRecord, busy: Boolean, error: String?, onBack: () -> Unit, onSave: (TaskRecord) -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable(task.id) { mutableStateOf(task.title) }
    var realm by rememberSaveable(task.id) { mutableStateOf(task.realm) }
    var address by rememberSaveable(task.id) { mutableStateOf(task.address) }
    var notes by rememberSaveable(task.id) { mutableStateOf(task.notes) }
    var emergency by rememberSaveable(task.id) { mutableStateOf(task.emergency) }
    var reminder by rememberSaveable(task.id) { mutableStateOf(task.remindAtMillis) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var discard by remember { mutableStateOf(false) }
    val changed = title != task.title || realm != task.realm || address != task.address ||
        notes != task.notes || emergency != task.emergency || reminder != task.remindAtMillis
    fun back() { if (!busy) { if (changed) discard = true else onBack() } }
    BackHandler { back() }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("放弃未保存的修改？") },
        confirmButton = { TextButton(onClick = onBack) { Text("放弃修改") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("继续编辑") } })
    Scaffold(topBar = { TopAppBar(title = { Text("编辑事项") }, navigationIcon = {
        TextButton(enabled = !busy, onClick = ::back) { Text("取消") }
    }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = realm == "work", enabled = !busy, onClick = { realm = "work" }, label = { Text("工作") })
                FilterChip(selected = realm == "life", enabled = !busy, onClick = { realm = "life"; emergency = false }, label = { Text("生活") })
            }
            OutlinedTextField(title, { title = it.take(100) }, enabled = !busy, label = { Text("事项名称（必填）") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(address, { address = it.take(2000) }, enabled = !busy, label = { Text("地点或地图分享链接") },
                supportingText = { Text("可从微信复制完整地址后长按粘贴；定位卡片能否转出地址或链接取决于微信和地图版本。") },
                modifier = Modifier.fillMaxWidth(), minLines = 2)
            OutlinedTextField(notes, { notes = it.take(4000) }, enabled = !busy, label = { Text("事项说明与现场记录") },
                modifier = Modifier.fillMaxWidth(), minLines = 3)
            if (realm == "work") {
                Row(Modifier.fillMaxWidth().toggleable(value = emergency, enabled = !busy,
                    role = Role.Checkbox, onValueChange = { emergency = it })) {
                    Checkbox(checked = emergency, enabled = !busy, onCheckedChange = null)
                    Text("应急事项", Modifier.padding(top = 12.dp))
                }
            }
            Text(reminder?.let { "提醒时间：${formatTaskTime(it)}" } ?: "未设置提醒")
            Row {
                OutlinedButton(enabled = !busy, onClick = {
                    val initial = Instant.ofEpochMilli(reminder ?: (System.currentTimeMillis() + 3600000))
                        .atZone(ZoneId.systemDefault())
                    DatePickerDialog(context, { _, year, month, day ->
                        TimePickerDialog(context, { _, hour, minute ->
                            val local = LocalDateTime.of(year, month + 1, day, hour, minute)
                            val zone = ZoneId.systemDefault()
                            if (zone.rules.getValidOffsets(local).size != 1) {
                                dateError = "此时间处于夏令时切换区间，请选择其他时间"
                            } else {
                                val value = local.atZone(zone).toInstant().toEpochMilli()
                                if (value <= System.currentTimeMillis()) dateError = "请选择将来的提醒时间"
                                else { reminder = value; dateError = null }
                            }
                        }, initial.hour, initial.minute, true).show()
                    }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
                }) { Text("选择日期与时间") }
                TextButton(enabled = !busy, onClick = { reminder = null; dateError = null }) { Text("取消提醒") }
            }
            (dateError ?: error)?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = title.isNotBlank() && !busy, onClick = {
                onSave(task.copy(title = title, realm = realm, address = address, notes = notes,
                    emergency = emergency, remindAtMillis = reminder))
            }, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "正在保存…" else "保存事项") }
        }
    }
}
