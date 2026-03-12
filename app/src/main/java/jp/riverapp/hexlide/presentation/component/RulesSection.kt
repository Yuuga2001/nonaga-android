package jp.riverapp.hexlide.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import jp.riverapp.hexlide.presentation.localization.LocalizedStrings
import jp.riverapp.hexlide.presentation.theme.HexlideColors

@Composable
fun RulesSection(
    strings: LocalizedStrings,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Goal banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(HexlideColors.GoalBg)
                .border(1.dp, HexlideColors.GoalBorder, RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "\uD83C\uDFAF", // target emoji
                fontSize = 10.sp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = strings.goal,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = HexlideColors.GoalText,
            )
        }

        // Rule steps
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RuleStep(
                number = "1",
                text = strings.slideToEdge,
                modifier = Modifier.weight(1f),
            )
            RuleStep(
                number = "2",
                text = strings.moveEmptyTile,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun RuleStep(
    number: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(HexlideColors.TextPrimary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            fontSize = 10.sp,
            color = HexlideColors.TextSecondary,
        )
    }
}
