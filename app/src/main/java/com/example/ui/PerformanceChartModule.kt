package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InterviewMessage
import com.example.data.CloudInterviewSummary
import kotlin.math.cos
import kotlin.math.sin

enum class ChartType {
    RADAR, BAR
}

/**
 * Parses a conversation's evaluated answers to retrieve real-time performance estimates in three key categories:
 * Communication, Technical Depth, and Role Alignment.
 */
fun calculatePerformanceScores(messages: List<InterviewMessage>): Triple<Float, Float, Float> {
    val userAnswers = messages.filter { it.sender == "USER" && it.evaluationScore != null }
    if (userAnswers.isEmpty()) return Triple(0f, 0f, 0f)

    var commSum = 0f
    var techSum = 0f
    var roleSum = 0f

    userAnswers.forEach { msg ->
        val score = (msg.evaluationScore ?: 7).toFloat()
        val text = msg.messageText.lowercase()
        val feedback = (msg.evaluationFeedback ?: "").lowercase()

        // 1. Communication Score
        val wordCount = text.split("\\s+".toRegex()).size
        var commBonus = 0f
        if (wordCount > 15) commBonus += 0.5f
        if (wordCount > 35) commBonus += 0.5f
        if (text.contains("for example") || text.contains("specifically") || text.contains("furthermore") || text.contains("consequently")) {
            commBonus += 0.8f
        }
        if (text.contains("um") || text.contains("uh") || text.contains("like,") || text.contains("stuff")) {
            commBonus -= 0.6f
        }
        val comm = (score + commBonus).coerceIn(1f, 10f)

        // 2. Technical Depth Score
        var techBonus = 0f
        val techKeywords = listOf(
            "architecture", "database", "query", "thread", "concurrency", "optimize", 
            "asynchronous", "memory", "algorithm", "design pattern", "interface", 
            "scaling", "latency", "bottleneck", "framework", "api", "analytics", 
            "implementation", "system", "performance", "sql", "cache", "efficiency"
        )
        val matchedTech = techKeywords.count { text.contains(it) }
        techBonus += (matchedTech * 0.4f)
        if (feedback.contains("technical") || feedback.contains("depth") || feedback.contains("precise") || feedback.contains("knowledge")) {
            techBonus += 0.5f
        }
        val tech = (score + techBonus).coerceIn(1f, 10f)

        // 3. Role Alignment Score
        var roleBonus = 0f
        val alignmentKeywords = listOf(
            "collaborate", "team", "align", "scrum", "agile", "product", "business", 
            "fit", "client", "user", "requirement", "process", "culture", "leadership"
        )
        val matchedAlignment = alignmentKeywords.count { text.contains(it) }
        roleBonus += (matchedAlignment * 0.5f)
        if (feedback.contains("align") || feedback.contains("role") || feedback.contains("industry") || feedback.contains("strengths")) {
            roleBonus += 0.6f
        }
        val role = (score + roleBonus).coerceIn(1f, 10f)

        commSum += comm
        techSum += tech
        roleSum += role
    }

    val count = userAnswers.size.toFloat()
    return Triple(
        (commSum / count).coerceIn(1f, 10f),
        (techSum / count).coerceIn(1f, 10f),
        (roleSum / count).coerceIn(1f, 10f)
    )
}

/**
 * Fallback score calculations based on summary average text references.
 */
fun calculatePerformanceScoresFromSummary(summary: CloudInterviewSummary): Triple<Float, Float, Float> {
    val score = summary.averageScore.toFloat()
    val text = summary.feedbackHighlights.lowercase()

    var commBonus = 0f
    var techBonus = 0f
    var roleBonus = 0f

    if (text.contains("communication") || text.contains("articulate") || text.contains("express") || text.contains("clear")) {
        commBonus += 0.6f
    }
    if (text.contains("technical") || text.contains("depth") || text.contains("details") || text.contains("architecture")) {
        techBonus += 0.6f
    }
    if (text.contains("alignment") || text.contains("role") || text.contains("culture") || text.contains("fit")) {
        roleBonus += 0.6f
    }

    return Triple(
        (score + commBonus).coerceIn(1f, 10f),
        (score + techBonus).coerceIn(1f, 10f),
        (score + roleBonus).coerceIn(1f, 10f)
    )
}

@Composable
fun PerformanceRadarOrBarChart(
    scores: Triple<Float, Float, Float>,
    modifier: Modifier = Modifier
) {
    var selectedChartType by remember { mutableStateOf(ChartType.BAR) }

    val commValue = scores.first
    val techValue = scores.second
    val roleValue = scores.third

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("performance_analytics_module")
    ) {
        // Module Title and Chart Selector Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Vocal Performance Metrics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Elegant Custom Segmented Selection Tab Row
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                    .padding(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedChartType == ChartType.BAR) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedChartType = ChartType.BAR }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("chart_toggle_bar"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Progress Bars",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedChartType == ChartType.BAR) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (selectedChartType == ChartType.RADAR) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .clickable { selectedChartType = ChartType.RADAR }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("chart_toggle_radar"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Radar Web",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedChartType == ChartType.RADAR) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        AnimatedContent(
            targetState = selectedChartType,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "chart_animation"
        ) { chartType ->
            when (chartType) {
                ChartType.BAR -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PerformanceBarRow(
                            label = "Communication",
                            score = commValue,
                            metricTip = "Clarity, filler words absence, and confidence delivery.",
                            barColor = Color(0xFF3B82F6) // Bright blue
                        )
                        PerformanceBarRow(
                            label = "Technical Depth",
                            score = techValue,
                            metricTip = "Key systems, robust terms, database, scaling detail.",
                            barColor = Color(0xFF10B981) // Teal green
                        )
                        PerformanceBarRow(
                            label = "Role Alignment",
                            score = roleValue,
                            metricTip = "Process suitability, teamwork, and fit highlights.",
                            barColor = Color(0xFFF59E0B) // Amber yellow
                        )
                    }
                }
                ChartType.RADAR -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Drawing Spider-web elements inside Canvas
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            val primaryColor = MaterialTheme.colorScheme.primary
                            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                            val density = LocalDensity.current

                            val animatedComm by animateFloatAsState(targetValue = commValue / 10f, animationSpec = tween(500))
                            val animatedTech by animateFloatAsState(targetValue = techValue / 10f, animationSpec = tween(500))
                            val animatedRole by animateFloatAsState(targetValue = roleValue / 10f, animationSpec = tween(500))

                            Canvas(modifier = Modifier.fillMaxSize().testTag("radar_chart_canvas")) {
                                val centerX = size.width / 2f
                                val centerY = size.height / 2f
                                val maxRadius = (size.width / 2f) * 0.85f

                                // Helper function for calculating x, y coordinate given radius fraction and angle index (0, 1, 2)
                                fun getPointOffset(fraction: Float, index: Int): Offset {
                                    val angle = when (index) {
                                        0 -> -Math.PI / 2.0 // Top
                                        1 -> Math.PI / 6.0  // Bottom-Right
                                        else -> 5 * Math.PI / 6.0 // Bottom-Left
                                    }
                                    val r = maxRadius * fraction
                                    return Offset(
                                        x = centerX + (r * cos(angle)).toFloat(),
                                        y = centerY + (r * sin(angle)).toFloat()
                                    )
                                }

                                // 1. Draw Concentric Levels Outlines (2.5, 5, 7.5, 10)
                                val levels = listOf(0.25f, 0.50f, 0.75f, 1.0f)
                                levels.forEach { lvl ->
                                    val path = Path().apply {
                                        val p0 = getPointOffset(lvl, 0)
                                        moveTo(p0.x, p0.y)
                                        val p1 = getPointOffset(lvl, 1)
                                        lineTo(p1.x, p1.y)
                                        val p2 = getPointOffset(lvl, 2)
                                        lineTo(p2.x, p2.y)
                                        close()
                                    }
                                    drawPath(
                                        path = path,
                                        color = onSurfaceColor.copy(alpha = 0.06f),
                                        style = Stroke(
                                            width = 1.dp.toPx(),
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                                        )
                                    )
                                }

                                // 2. Draw 3 Radial Axes Lines from Center
                                for (i in 0..2) {
                                    val endPt = getPointOffset(1.0f, i)
                                    drawLine(
                                        color = onSurfaceColor.copy(alpha = 0.08f),
                                        start = Offset(centerX, centerY),
                                        end = endPt,
                                        strokeWidth = 1.2.dp.toPx()
                                    )
                                }

                                // 3. Draw Scoring Polygon
                                val cValue = animatedComm
                                val tValue = animatedTech
                                val rValue = animatedRole

                                if (cValue > 0f || tValue > 0f || rValue > 0f) {
                                    val scorePath = Path().apply {
                                        val p0 = getPointOffset(cValue, 0)
                                        moveTo(p0.x, p0.y)
                                        val p1 = getPointOffset(tValue, 1)
                                        lineTo(p1.x, p1.y)
                                        val p2 = getPointOffset(rValue, 2)
                                        lineTo(p2.x, p2.y)
                                        close()
                                    }

                                    // Fill Area
                                    drawPath(
                                        path = scorePath,
                                        color = primaryColor.copy(alpha = 0.16f)
                                    )

                                    // Thick outline
                                    drawPath(
                                        path = scorePath,
                                        color = primaryColor,
                                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                    )

                                    // Vertex Nodes (Dots)
                                    for (i in 0..2) {
                                        val valFrac = when (i) {
                                            0 -> cValue
                                            1 -> tValue
                                            else -> rValue
                                        }
                                        val nodeOffset = getPointOffset(valFrac, i)
                                        // Dynamic Glow surrounding point
                                        drawCircle(
                                            color = primaryColor.copy(alpha = 0.3f),
                                            radius = 5.dp.toPx(),
                                            center = nodeOffset
                                        )
                                        // Central solid point
                                        drawCircle(
                                            color = primaryColor,
                                            radius = 3.dp.toPx(),
                                            center = nodeOffset
                                        )
                                    }
                                }
                            }
                        }

                        // Right Sidebar: Detailed Metric Legend with Badges
                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(start = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadarLegendRow(label = "Communication", score = commValue, badgeColor = Color(0xFF3B82F6))
                            RadarLegendRow(label = "Technical Depth", score = techValue, badgeColor = Color(0xFF10B981))
                            RadarLegendRow(label = "Role Alignment", score = roleValue, badgeColor = Color(0xFFF59E0B))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Subcomponent for single Horizontal Progress bars with animations.
 */
@Composable
fun ColumnScope.PerformanceBarRow(
    label: String,
    score: Float,
    metricTip: String,
    barColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 10f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = metricTip,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    lineHeight = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Score Badge
            Text(
                text = String.format("%.1f/10", score),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = barColor,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(min = 48.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Legend Row for the radar sidebar detailing levels.
 */
@Composable
fun RadarLegendRow(
    label: String,
    score: Float,
    badgeColor: Color
) {
    val appraisal = when {
        score >= 8.5f -> "Exceptional"
        score >= 7.0f -> "Strong"
        score >= 5.0f -> "Satisfactory"
        else -> "Developing"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(badgeColor.copy(alpha = 0.04f))
            .border(1.dp, badgeColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Colored dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(badgeColor)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "$appraisal: " + String.format("%.1f", score),
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
