package com.isai.player.ui.nowplaying

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.isai.player.R
import com.isai.player.data.repository.LyricsState
import com.isai.player.data.repository.LyricsTextSize
import com.isai.player.ui.theme.IsaiTheme
import kotlinx.coroutines.delay

@Composable
fun LyricsView(
    lyricsState: LyricsState,
    textSize: LyricsTextSize = LyricsTextSize.MEDIUM,
    onLineClick: (timestampMs: Long) -> Unit,
    onImportClick: () -> Unit,
    onOpenOffsetDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fontSize = when (textSize) {
        LyricsTextSize.SMALL -> 16.sp
        LyricsTextSize.MEDIUM -> 20.sp
        LyricsTextSize.LARGE -> 24.sp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        when (lyricsState) {
            is LyricsState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = IsaiTheme.colors.accent)
                }
            }

            is LyricsState.Synced -> {
                SyncedLyricsContent(
                    lines = lyricsState.lines,
                    activeIndex = lyricsState.activeIndex,
                    fontSize = fontSize,
                    onLineClick = onLineClick,
                    onOpenOffsetDialog = onOpenOffsetDialog
                )
            }

            is LyricsState.Plain -> {
                PlainLyricsContent(
                    lines = lyricsState.lines,
                    fontSize = fontSize,
                    onOpenOffsetDialog = onOpenOffsetDialog
                )
            }

            is LyricsState.Empty -> {
                EmptyLyricsContent(onImportClick = onImportClick)
            }
        }
    }
}

@Composable
private fun SyncedLyricsContent(
    lines: List<com.isai.player.data.lyrics.LyricsLine>,
    activeIndex: Int,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onLineClick: (timestampMs: Long) -> Unit,
    onOpenOffsetDialog: () -> Unit
) {
    val listState = rememberLazyListState()
    var isUserPaused by remember { mutableStateOf(false) }

    // User scroll detector: pause auto-scroll when user manually scrolls
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            isUserPaused = true
        } else if (isUserPaused) {
            // Wait 4 seconds after scroll ends before resuming auto-scroll
            delay(4000L)
            isUserPaused = false
        }
    }

    // Auto-scroll to center active line when not user-paused
    LaunchedEffect(activeIndex, isUserPaused) {
        if (!isUserPaused && activeIndex in lines.indices) {
            val target = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(target)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 80.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(lines, key = { index, line -> "${line.timestampMs}_$index" }) { index, line ->
                val isActive = index == activeIndex
                val lineScale by animateFloatAsState(
                    targetValue = if (isActive) 1.05f else 1.0f,
                    animationSpec = tween(200),
                    label = "LineScale"
                )

                val textColor = when {
                    isActive -> IsaiTheme.colors.accent
                    index < activeIndex -> IsaiTheme.colors.onSurfaceVariant.copy(alpha = 0.5f)
                    else -> IsaiTheme.colors.onSurfaceVariant.copy(alpha = 0.35f)
                }

                Text(
                    text = line.text,
                    fontSize = fontSize,
                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                    color = textColor,
                    textAlign = TextAlign.Center,
                    lineHeight = fontSize * 1.4f,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(lineScale)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onLineClick(line.timestampMs) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .then(
                            if (isActive) {
                                Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                            } else Modifier
                        )
                )
            }
        }

        // Top Controls: Offset button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = CircleShape,
                color = IsaiTheme.colors.surfaceVariant.copy(alpha = 0.8f)
            ) {
                IconButton(onClick = onOpenOffsetDialog, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = stringResource(id = R.string.lyrics_offset_title),
                        tint = IsaiTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Floating "Jump to current" pill when user is scrolled away
        AnimatedVisibility(
            visible = isUserPaused && activeIndex in lines.indices,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Button(
                onClick = {
                    isUserPaused = false
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = IsaiTheme.colors.accent,
                    contentColor = IsaiTheme.colors.onAccent
                ),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.lyrics_jump_to_current),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PlainLyricsContent(
    lines: List<String>,
    fontSize: androidx.compose.ui.unit.TextUnit,
    onOpenOffsetDialog: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = 48.dp, bottom = 48.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            lines.forEach { line ->
                Text(
                    text = line,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Normal,
                    color = IsaiTheme.colors.onBackground.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = fontSize * 1.5f,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyLyricsContent(
    onImportClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = IsaiTheme.colors.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(id = R.string.lyrics_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = IsaiTheme.colors.onBackground
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(id = R.string.lyrics_empty_desc),
            style = MaterialTheme.typography.bodySmall,
            color = IsaiTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onImportClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = IsaiTheme.colors.accent,
                contentColor = IsaiTheme.colors.onAccent
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(id = R.string.lyrics_action_import),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

