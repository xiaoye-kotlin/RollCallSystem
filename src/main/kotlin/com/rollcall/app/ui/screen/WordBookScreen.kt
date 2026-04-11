package com.rollcall.app.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rollcall.app.audio.AudioManager
import com.rollcall.app.data.model.VocabularyBookEntry
import com.rollcall.app.ui.theme.AppTheme
import com.rollcall.app.util.FileHelper
import java.util.Locale

private const val WORD_BOOK_FILE_PATH = "D:/Xiaoye/Learning/WordBook.json"

@Composable
fun WordBookScreen(onClose: () -> Unit) {
    var rawEntries by remember { mutableStateOf(loadWordBookEntries()) }
    var keyword by remember { mutableStateOf("") }
    var showLearned by remember { mutableStateOf(true) }
    val entries = remember(rawEntries, keyword, showLearned) {
        rawEntries.filter { entry ->
            val matchesKeyword = keyword.isBlank() ||
                entry.word.contains(keyword, ignoreCase = true) ||
                entry.meaning.contains(keyword, ignoreCase = true) ||
                entry.root.contains(keyword, ignoreCase = true)
            val matchesLearned = showLearned || !entry.isLearned
            matchesKeyword && matchesLearned
        }
    }

    Window(
        onCloseRequest = onClose,
        title = "生词本",
        undecorated = true,
        transparent = true,
        alwaysOnTop = true,
        resizable = false,
        state = rememberWindowState(
            position = WindowPosition(Alignment.Center),
            size = DpSize(700.dp, 840.dp)
        )
    ) {
        AppTheme {
            val colors = AppTheme.colors

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.verticalGradient(listOf(colors.gradient1, colors.gradient2)))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📘 生词本", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                        Text("共 ${rawEntries.size} 条 · 当前 ${entries.size} 条", fontSize = 13.sp, color = colors.textHint)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { rawEntries = loadWordBookEntries() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "刷新", tint = colors.primary)
                        }
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "关闭", tint = colors.textSecondary)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索单词 / 释义 / 词根") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Spacer(Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        label = if (showLearned) "显示已会" else "只看未会",
                        active = showLearned,
                        accent = colors.primary
                    ) {
                        showLearned = !showLearned
                    }
                }

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.94f))
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(24.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (entries.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                                Text("暂无匹配的生词", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.textHint)
                            }
                        }
                    } else {
                        itemsIndexed(entries, key = { _, item -> "${item.word}_${item.meaning}_${item.category}" }) { index, entry ->
                            WordBookCard(
                                index = index + 1,
                                entry = entry,
                                colors = colors,
                                onSpeak = {
                                    AudioManager.playTextAudio(
                                        text = entry.word,
                                        baseUrl = com.rollcall.app.state.AppState.url,
                                        isInternetAvailable = com.rollcall.app.state.AppState.isInternetAvailable.value
                                    )
                                },
                                onToggleLearned = {
                                    rawEntries = updateWordBookEntry(rawEntries, entry) {
                                        it.copy(isLearned = !it.isLearned)
                                    }
                                },
                                onDelete = {
                                    rawEntries = deleteWordBookEntry(rawEntries, entry)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WordBookCard(
    index: Int,
    entry: VocabularyBookEntry,
    colors: com.rollcall.app.ui.theme.AppColors,
    onSpeak: () -> Unit,
    onToggleLearned: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
            .animateContentSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(index.toString(), fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = colors.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.word, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                Text(entry.type.ifBlank { "未标注词性" }, fontSize = 14.sp, color = colors.textSecondary)
            }
            ActionChip("朗读", colors.primary, onSpeak)
            Spacer(Modifier.width(8.dp))
            ActionChip(if (entry.isLearned) "已会" else "未会", if (entry.isLearned) colors.success else colors.warning, onToggleLearned)
            Spacer(Modifier.width(8.dp))
            ActionChip("删除", colors.error, onDelete)
        }
        Spacer(Modifier.height(12.dp))
        Text(entry.meaning, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        if (entry.root.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text("词根词缀  ${entry.root}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.primary)
        }
        if (entry.example.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(entry.example, fontSize = 14.sp, color = colors.textSecondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
private fun FilterChip(
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.7f))
            .border(1.dp, accent.copy(alpha = if (active) 0.4f else 0.2f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (active) accent else Color(0xFF6B7280))
    }
}

private fun loadWordBookEntries(): List<VocabularyBookEntry> {
    val json = FileHelper.readFromFile(WORD_BOOK_FILE_PATH)
    if (json == "404" || json.isBlank()) return emptyList()

    return runCatching {
        Gson().fromJson<List<VocabularyBookEntry>>(
            json,
            object : TypeToken<List<VocabularyBookEntry>>() {}.type
        ) ?: emptyList()
    }.getOrElse { emptyList() }
}

private fun updateWordBookEntry(
    entries: List<VocabularyBookEntry>,
    target: VocabularyBookEntry,
    transform: (VocabularyBookEntry) -> VocabularyBookEntry
): List<VocabularyBookEntry> {
    val updated = entries.map {
        if (it.word == target.word && it.meaning == target.meaning && it.category == target.category) {
            transform(it)
        } else {
            it
        }
    }
    persistWordBookEntries(updated)
    return updated
}

private fun deleteWordBookEntry(
    entries: List<VocabularyBookEntry>,
    target: VocabularyBookEntry
): List<VocabularyBookEntry> {
    val updated = entries.filterNot {
        it.word == target.word && it.meaning == target.meaning && it.category == target.category
    }
    persistWordBookEntries(updated)
    return updated
}

private fun persistWordBookEntries(entries: List<VocabularyBookEntry>) {
    FileHelper.writeToFile(WORD_BOOK_FILE_PATH, Gson().toJson(entries))
}
