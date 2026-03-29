package jp.riverapp.hexlide.presentation.screen.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.hexlide.data.model.AIBattleStats
import jp.riverapp.hexlide.data.model.AIBattleStatsService
import jp.riverapp.hexlide.data.model.AIGameRecord
import jp.riverapp.hexlide.presentation.localization.LocalizedStrings
import jp.riverapp.hexlide.presentation.theme.HexlideColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIStatsScreen(
    strings: LocalizedStrings,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val stats = AIBattleStatsService.load(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = strings.aiStats,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = HexlideColors.Background,
                ),
            )
        },
        containerColor = HexlideColors.Background,
    ) { paddingValues ->
        if (stats.totalGames == 0) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = HexlideColors.TextTertiary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = strings.statsNoGamesYet,
                        fontSize = 16.sp,
                        color = HexlideColors.TextSecondary,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            ) {
                // Summary card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryCard(stats = stats, strings = strings)
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // History header
                item {
                    Text(
                        text = strings.statsGameHistory,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HexlideColors.TextSecondary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                    )
                }

                // History list
                val reversed = stats.records.reversed()
                itemsIndexed(reversed) { index, record ->
                    if (index == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(Color.White),
                        ) {
                            HistoryRow(record = record, strings = strings)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .let {
                                    if (index == reversed.lastIndex) {
                                        it.clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                                    } else it
                                },
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp),
                                color = HexlideColors.TileStroke,
                            )
                            HistoryRow(record = record, strings = strings)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun SummaryCard(stats: AIBattleStats, strings: LocalizedStrings) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Win / Loss / Total row
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                value = "${stats.wins}",
                label = strings.statsWins,
                color = HexlideColors.PieceRed,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(HexlideColors.TileStroke),
            )
            StatItem(
                value = "${stats.losses}",
                label = strings.statsLosses,
                color = HexlideColors.PieceBlue,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(HexlideColors.TileStroke),
            )
            StatItem(
                value = "${stats.totalGames}",
                label = strings.statsTotalGames,
                color = HexlideColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        HorizontalDivider(color = HexlideColors.TileStroke)

        // Win Rate + Avg Turns row
        Row(modifier = Modifier.fillMaxWidth()) {
            StatItem(
                value = "${(stats.winRate * 100).toInt()}%",
                label = strings.statsWinRate,
                color = HexlideColors.ModeAI,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(HexlideColors.TileStroke),
            )
            StatItem(
                value = String.format(Locale.US, "%.1f", stats.averageTurns),
                label = strings.statsAvgTurns,
                color = HexlideColors.TextSecondary,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = HexlideColors.TextSecondary,
        )
    }
}

@Composable
private fun HistoryRow(record: AIGameRecord, strings: LocalizedStrings) {
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.US)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Result icon
        Icon(
            imageVector = if (record.won) Icons.Filled.EmojiEvents else Icons.Filled.Cancel,
            contentDescription = null,
            tint = if (record.won) HexlideColors.ModeAI else HexlideColors.TextTertiary,
            modifier = Modifier.size(24.dp),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Result text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (record.won) strings.youWin else strings.aiWin,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = HexlideColors.TextPrimary,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${strings.statsFirst}: ${if (record.wentFirst) strings.you else strings.ai} / ${strings.statsSecond}: ${if (record.wentFirst) strings.ai else strings.you}",
                    fontSize = 11.sp,
                    color = HexlideColors.TextSecondary,
                )
                Text(
                    text = "·",
                    fontSize = 11.sp,
                    color = HexlideColors.TextSecondary,
                )
                Text(
                    text = String.format(Locale.US, strings.statsTurnsFormat, record.turns),
                    fontSize = 11.sp,
                    color = HexlideColors.TextSecondary,
                )
            }
        }

        // Date
        Text(
            text = dateFormat.format(Date(record.date)),
            fontSize = 11.sp,
            color = HexlideColors.TextTertiary,
        )
    }
}
