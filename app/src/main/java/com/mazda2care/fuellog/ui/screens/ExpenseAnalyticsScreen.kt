package com.mazda2care.fuellog.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazda2care.fuellog.model.CarProfile
import com.mazda2care.fuellog.model.FormatUtils
import com.mazda2care.fuellog.model.FuelRecordEntity
import com.mazda2care.fuellog.ui.theme.CostPurple
import com.mazda2care.fuellog.ui.theme.EcoGreen
import com.mazda2care.fuellog.ui.theme.FuelOrange
import com.mazda2care.fuellog.ui.theme.PetrolBlue
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt

enum class ExpensePeriodMode(val label: String) {
    WEEKLY("Theo Tuần"),
    MONTHLY("Theo Tháng"),
    YEARLY("Theo Năm")
}

data class PeriodExpenseSummary(
    val key: String,
    val title: String,
    val shortChartLabel: String,
    val subtitle: String,
    val totalCost: Double,
    val totalLiters: Double,
    val refillCount: Int,
    val avgPricePerLiter: Double,
    val sortTimestamp: Long
)

data class StationExpenseSummary(
    val stationName: String,
    val totalCost: Double,
    val totalLiters: Double,
    val refillCount: Int,
    val shareRatio: Float
)

@Composable
fun ExpenseAnalyticsTabContent(
    profile: CarProfile,
    records: List<FuelRecordEntity>
) {
    var selectedMode by remember { mutableStateOf(ExpensePeriodMode.MONTHLY) }
    val zoneId = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zoneId) }

    // 1. Tính toán tổng chi tiêu theo Tuần này, Tháng này, Năm nay, và Toàn thời gian
    val startOfThisWeek = remember(today) {
        today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val startOfLastWeek = remember(startOfThisWeek) { startOfThisWeek.minusWeeks(1) }

    val thisWeekRecords = remember(records, startOfThisWeek) {
        records.filter {
            val d = Instant.ofEpochMilli(it.epochMillis).atZone(zoneId).toLocalDate()
            !d.isBefore(startOfThisWeek)
        }
    }
    val lastWeekRecords = remember(records, startOfThisWeek, startOfLastWeek) {
        records.filter {
            val d = Instant.ofEpochMilli(it.epochMillis).atZone(zoneId).toLocalDate()
            !d.isBefore(startOfLastWeek) && d.isBefore(startOfThisWeek)
        }
    }

    val thisMonthYm = remember(today) { YearMonth.from(today) }
    val lastMonthYm = remember(thisMonthYm) { thisMonthYm.minusMonths(1) }

    val thisMonthRecords = remember(records, thisMonthYm) {
        records.filter {
            val d = Instant.ofEpochMilli(it.epochMillis).atZone(zoneId).toLocalDate()
            YearMonth.from(d) == thisMonthYm
        }
    }
    val lastMonthRecords = remember(records, lastMonthYm) {
        records.filter {
            val d = Instant.ofEpochMilli(it.epochMillis).atZone(zoneId).toLocalDate()
            YearMonth.from(d) == lastMonthYm
        }
    }

    val thisYearRecords = remember(records, today) {
        records.filter {
            val d = Instant.ofEpochMilli(it.epochMillis).atZone(zoneId).toLocalDate()
            d.year == today.year
        }
    }

    val totalAllTimeCost = remember(records) { records.sumOf { it.totalCost } }
    val totalAllTimeLiters = remember(records) { records.sumOf { it.liters } }
    val avgCostPerRefill = if (records.isNotEmpty()) totalAllTimeCost / records.size else 0.0
    val avgPricePerLiterAll = if (totalAllTimeLiters > 0) totalAllTimeCost / totalAllTimeLiters else profile.currentFuelPrice

    // 2. Gom nhóm dữ liệu theo chế độ chọn (Tuần / Tháng / Năm)
    val groupedSummaries = remember(records, selectedMode) {
        buildPeriodSummaries(records, selectedMode, zoneId)
    }

    // 3. Thống kê theo Cây xăng
    val stationSummaries = remember(records, totalAllTimeCost) {
        buildStationSummaries(records, totalAllTimeCost)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Thẻ 1: Tổng Chi Tiêu Nhiên Liệu Tích Lũy (Hero Card)
        item {
            LifetimeExpenseHeroCard(
                totalCost = totalAllTimeCost,
                totalLiters = totalAllTimeLiters,
                totalCount = records.size,
                avgCostPerRefill = avgCostPerRefill,
                avgPricePerLiter = avgPricePerLiterAll
            )
        }

        // Thẻ 2: 3 Khung Thời Gian Hiện Tại (Tuần này • Tháng này • Năm nay)
        item {
            CurrentPeriodOverviewRow(
                weekCost = thisWeekRecords.sumOf { it.totalCost },
                weekLiters = thisWeekRecords.sumOf { it.liters },
                lastWeekCost = lastWeekRecords.sumOf { it.totalCost },
                monthCost = thisMonthRecords.sumOf { it.totalCost },
                monthLiters = thisMonthRecords.sumOf { it.liters },
                lastMonthCost = lastMonthRecords.sumOf { it.totalCost },
                yearCost = thisYearRecords.sumOf { it.totalCost },
                yearLiters = thisYearRecords.sumOf { it.liters },
                currentYear = today.year
            )
        }

        // Thẻ 3: Bộ chọn chế độ xem (Theo Tuần | Theo Tháng | Theo Năm) & Biểu đồ cột
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "BIỂU ĐỒ CHI TIÊU NHIÊN LIỆU",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpensePeriodMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = {
                                    Text(
                                        text = mode.label,
                                        fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    if (groupedSummaries.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có dữ liệu đổ xăng",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        ExpenseBarChart(
                            summaries = groupedSummaries.take(8).reversed()
                        )
                    }
                }
            }
        }

        // Tiêu đề bảng chi tiết từng Tuần / Tháng / Năm
        item {
            Text(
                text = "CHI TIẾT CHI TIÊU ${selectedMode.label.uppercase()} (${groupedSummaries.size} KỲ)",
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Danh sách từng kỳ (kèm so sánh kỳ liền trước)
        val maxPeriodCost = groupedSummaries.maxOfOrNull { it.totalCost } ?: 1.0
        itemsIndexed(groupedSummaries, key = { _, item -> item.key }) { index, item ->
            val prevItem = groupedSummaries.getOrNull(index + 1)
            PeriodExpenseDetailCard(
                summary = item,
                previousSummary = prevItem,
                maxCostInList = maxPeriodCost
            )
        }

        // Thẻ 4: Thống kê chi tiêu theo từng Cây Xăng
        if (stationSummaries.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                StationBreakdownCard(stations = stationSummaries)
            }
        }
    }
}

@Composable
private fun LifetimeExpenseHeroCard(
    totalCost: Double,
    totalLiters: Double,
    totalCount: Int,
    avgCostPerRefill: Double,
    avgPricePerLiter: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = FuelOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "TỔNG TIỀN NHIÊN LIỆU ĐÃ CHI",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    color = FuelOrange.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "$totalCount lần đổ",
                        color = FuelOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = FormatUtils.formatVnd(totalCost),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("TỔNG LƯỢNG XĂNG", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = FormatUtils.formatLiters(totalLiters),
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TB MỖI LẦN ĐỔ", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = FormatUtils.formatVnd(avgCostPerRefill),
                        color = Color(0xFF38BDF8),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("GIÁ BÌNH QUÂN", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${FormatUtils.formatVndNumberOnly(avgPricePerLiter)} đ/L",
                        color = Color(0xFF4ADE80),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentPeriodOverviewRow(
    weekCost: Double,
    weekLiters: Double,
    lastWeekCost: Double,
    monthCost: Double,
    monthLiters: Double,
    lastMonthCost: Double,
    yearCost: Double,
    yearLiters: Double,
    currentYear: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PeriodQuickCompareCard(
                modifier = Modifier.weight(1f),
                title = "TUẦN NÀY",
                cost = weekCost,
                liters = weekLiters,
                prevCost = lastWeekCost,
                prevLabel = "tuần trước",
                accentColor = PetrolBlue
            )
            PeriodQuickCompareCard(
                modifier = Modifier.weight(1f),
                title = "THÁNG NÀY",
                cost = monthCost,
                liters = monthLiters,
                prevCost = lastMonthCost,
                prevLabel = "tháng trước",
                accentColor = FuelOrange
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TỔNG CHI TIÊU NĂM $currentYear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", yearLiters)} Lít nhiên liệu trong năm $currentYear",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = FormatUtils.formatVnd(yearCost),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = CostPurple
                )
            }
        }
    }
}

@Composable
private fun PeriodQuickCompareCard(
    modifier: Modifier = Modifier,
    title: String,
    cost: Double,
    liters: Double,
    prevCost: Double,
    prevLabel: String,
    accentColor: Color
) {
    val diff = cost - prevCost
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = FormatUtils.formatVnd(cost),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${String.format(Locale.US, "%.1f", liters)} Lít",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            if (prevCost > 0) {
                val pct = ((diff / prevCost) * 100.0).roundToInt()
                val isMore = diff > 0
                val badgeColor = if (isMore) Color(0xFFDC2626) else EcoGreen
                val sign = if (pct > 0) "+" else ""
                Text(
                    text = "$sign$pct% vs $prevLabel",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    maxLines = 1
                )
            } else {
                Text(
                    text = "Kỳ trước: 0 đ",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpenseBarChart(
    summaries: List<PeriodExpenseSummary>
) {
    val maxCost = (summaries.maxOfOrNull { it.totalCost } ?: 1.0).coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            summaries.forEachIndexed { idx, item ->
                val isLatest = idx == summaries.lastIndex
                val heightFraction = (item.totalCost / maxCost).toFloat().coerceIn(0.08f, 1f)
                val barColor = if (isLatest) FuelOrange else PetrolBlue.copy(alpha = 0.75f)
                val shortMoney = if (item.totalCost >= 1_000_000) {
                    String.format(Locale.US, "%.1ftr", item.totalCost / 1_000_000.0)
                } else {
                    "${(item.totalCost / 1000.0).roundToInt()}k"
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = shortMoney,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLatest) FuelOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.68f)
                            .fillMaxHeight(heightFraction * 0.78f)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(barColor)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = item.shortChartLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isLatest) FontWeight.ExtraBold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodExpenseDetailCard(
    summary: PeriodExpenseSummary,
    previousSummary: PeriodExpenseSummary?,
    maxCostInList: Double
) {
    val progress = (summary.totalCost / maxCostInList.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val diffVsPrev = if (previousSummary != null && previousSummary.totalCost > 0) {
        summary.totalCost - previousSummary.totalCost
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = summary.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = summary.subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatVnd(summary.totalCost),
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = FuelOrange
                    )
                    if (diffVsPrev != null && kotlin.math.abs(diffVsPrev) >= 1000) {
                        val isUp = diffVsPrev > 0
                        val color = if (isUp) Color(0xFFDC2626) else EcoGreen
                        val sign = if (isUp) "+" else ""
                        Text(
                            text = "${if (isUp) "▲" else "▼"} $sign${FormatUtils.formatVnd(diffVsPrev)} so với kỳ trước",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = FuelOrange,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${summary.refillCount} lần đổ • ${FormatUtils.formatLiters(summary.totalLiters)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Giá TB: ${FormatUtils.formatVndNumberOnly(summary.avgPricePerLiter)} đ/L",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StationBreakdownCard(
    stations: List<StationExpenseSummary>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalGasStation,
                    contentDescription = null,
                    tint = PetrolBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "THỐNG KÊ CHI TIÊU THEO HÃNG XĂNG",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            stations.forEach { st ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = st.stationName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${FormatUtils.formatVnd(st.totalCost)} (${(st.shareRatio * 100).roundToInt()}%)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = PetrolBlue
                        )
                    }
                    LinearProgressIndicator(
                        progress = { st.shareRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PetrolBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Text(
                        text = "${st.refillCount} lần đổ • ${FormatUtils.formatLiters(st.totalLiters)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun buildPeriodSummaries(
    records: List<FuelRecordEntity>,
    mode: ExpensePeriodMode,
    zoneId: ZoneId
): List<PeriodExpenseSummary> {
    if (records.isEmpty()) return emptyList()
    val shortDateFmt = DateTimeFormatter.ofPattern("dd/MM")

    return when (mode) {
        ExpensePeriodMode.WEEKLY -> {
            records.groupBy { rec ->
                val date = Instant.ofEpochMilli(rec.epochMillis).atZone(zoneId).toLocalDate()
                date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            }.map { (monday, list) ->
                val sunday = monday.plusDays(6)
                val cost = list.sumOf { it.totalCost }
                val liters = list.sumOf { it.liters }
                val avgPrice = if (liters > 0) cost / liters else 0.0
                val sortTs = monday.atStartOfDay(zoneId).toInstant().toEpochMilli()
                PeriodExpenseSummary(
                    key = "W_${monday}",
                    title = "Tuần ${monday.format(shortDateFmt)} – ${sunday.format(shortDateFmt)}/${sunday.year}",
                    shortChartLabel = monday.format(shortDateFmt),
                    subtitle = "Trung bình ${FormatUtils.formatVnd(cost / 7.0)} / ngày",
                    totalCost = cost,
                    totalLiters = liters,
                    refillCount = list.size,
                    avgPricePerLiter = avgPrice,
                    sortTimestamp = sortTs
                )
            }.sortedByDescending { it.sortTimestamp }
        }

        ExpensePeriodMode.MONTHLY -> {
            records.groupBy { rec ->
                val date = Instant.ofEpochMilli(rec.epochMillis).atZone(zoneId).toLocalDate()
                YearMonth.from(date)
            }.map { (ym, list) ->
                val cost = list.sumOf { it.totalCost }
                val liters = list.sumOf { it.liters }
                val avgPrice = if (liters > 0) cost / liters else 0.0
                val sortTs = ym.atDay(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                PeriodExpenseSummary(
                    key = "M_${ym}",
                    title = "Tháng ${ym.monthValue}/${ym.year}",
                    shortChartLabel = "T${ym.monthValue}/${ym.year % 100}",
                    subtitle = "TB ${FormatUtils.formatVnd(cost / list.size.coerceAtLeast(1))} / lần đổ",
                    totalCost = cost,
                    totalLiters = liters,
                    refillCount = list.size,
                    avgPricePerLiter = avgPrice,
                    sortTimestamp = sortTs
                )
            }.sortedByDescending { it.sortTimestamp }
        }

        ExpensePeriodMode.YEARLY -> {
            records.groupBy { rec ->
                Instant.ofEpochMilli(rec.epochMillis).atZone(zoneId).toLocalDate().year
            }.map { (year, list) ->
                val cost = list.sumOf { it.totalCost }
                val liters = list.sumOf { it.liters }
                val avgPrice = if (liters > 0) cost / liters else 0.0
                val distinctMonths = list.map {
                    Instant.ofEpochMilli(it.epochMillis).atZone(zoneId).toLocalDate().monthValue
                }.distinct().size.coerceAtLeast(1)
                val sortTs = LocalDate.of(year, 1, 1).atStartOfDay(zoneId).toInstant().toEpochMilli()
                PeriodExpenseSummary(
                    key = "Y_$year",
                    title = "Năm $year",
                    shortChartLabel = "$year",
                    subtitle = "Trung bình ~${FormatUtils.formatVnd(cost / distinctMonths)} / tháng",
                    totalCost = cost,
                    totalLiters = liters,
                    refillCount = list.size,
                    avgPricePerLiter = avgPrice,
                    sortTimestamp = sortTs
                )
            }.sortedByDescending { it.sortTimestamp }
        }
    }
}

private fun buildStationSummaries(
    records: List<FuelRecordEntity>,
    totalAllCost: Double
): List<StationExpenseSummary> {
    if (records.isEmpty() || totalAllCost <= 0) return emptyList()
    return records.groupBy { CarProfile.normalizeBrandName(it.stationName) }
        .map { (station, list) ->
            val cost = list.sumOf { it.totalCost }
            val liters = list.sumOf { it.liters }
            StationExpenseSummary(
                stationName = station,
                totalCost = cost,
                totalLiters = liters,
                refillCount = list.size,
                shareRatio = (cost / totalAllCost).toFloat().coerceIn(0f, 1f)
            )
        }
        .sortedByDescending { it.totalCost }
}
