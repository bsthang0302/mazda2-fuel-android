package com.mazda2care.fuellog.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazda2care.fuellog.model.CarProfile
import com.mazda2care.fuellog.model.FormatUtils
import com.mazda2care.fuellog.model.FuelPricePeriodEntity
import com.mazda2care.fuellog.model.FuelProduct
import com.mazda2care.fuellog.model.FuelRecordEntity
import com.mazda2care.fuellog.model.LiveFuelItemPrice
import com.mazda2care.fuellog.model.PriceZone
import com.mazda2care.fuellog.ui.FuelViewModel
import com.mazda2care.fuellog.ui.theme.CostPurple
import com.mazda2care.fuellog.ui.theme.EcoGreen
import com.mazda2care.fuellog.ui.theme.FuelOrange
import com.mazda2care.fuellog.ui.theme.PetrolBlue
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FuelViewModel,
    onExportJsonClick: () -> Unit,
    onImportJsonClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val profile by viewModel.profile.collectAsState()
    val records by viewModel.fuelRecords.collectAsState()
    val periods by viewModel.pricePeriods.collectAsState()
    val liveItems by viewModel.liveFuelItems.collectAsState()
    val isSyncing by viewModel.isSyncingPrice.collectAsState()
    val syncStatus by viewModel.syncStatusMessage.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddRefillSheet by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<FuelRecordEntity?>(null) }
    var showManualPriceDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = profile.carName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${profile.plateNumber} • ODO ${FormatUtils.formatVndNumberOnly(profile.currentOdo.toDouble())} km",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshLiveFuelPrices() },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Cập nhật giá xăng tự động"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingRecord = null
                        showAddRefillSheet = true
                    },
                    containerColor = FuelOrange,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Ghi Đổ Xăng", fontWeight = FontWeight.Bold) }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.LocalGasStation, contentDescription = null) },
                    label = { Text("Nhật ký xăng") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = null) },
                    label = { Text("Giá & Biểu đồ") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                    label = { Text("Xe & Sao lưu") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> FuelLogTabContent(
                    profile = profile,
                    records = records,
                    periods = periods,
                    isSyncing = isSyncing,
                    syncStatus = syncStatus,
                    onRefreshPrice = { viewModel.refreshLiveFuelPrices() },
                    onOpenManualPrice = { showManualPriceDialog = true },
                    onSwitchZone = { newZone ->
                        viewModel.selectFuelProductAndZone(profile.selectedFuelProduct, newZone)
                    },
                    onAddClick = {
                        editingRecord = null
                        showAddRefillSheet = true
                    },
                    onEditRecord = { rec ->
                        editingRecord = rec
                        showAddRefillSheet = true
                    },
                    onDeleteRecord = { rec ->
                        viewModel.deleteFuelRecord(rec)
                    }
                )

                1 -> PriceAndAnalyticsTabContent(
                    profile = profile,
                    liveItems = liveItems,
                    periods = periods,
                    records = records,
                    isSyncing = isSyncing,
                    syncStatus = syncStatus,
                    onRefreshLive = { viewModel.refreshLiveFuelPrices() },
                    onSelectFuelAndZone = { prod, zone ->
                        viewModel.selectFuelProductAndZone(prod, zone)
                    },
                    onOpenManualPrice = { showManualPriceDialog = true }
                )

                2 -> CarAndBackupTabContent(
                    profile = profile,
                    recordsCount = records.size,
                    onSaveProfile = { updated -> viewModel.updateCarProfile(updated) },
                    onExportJsonClick = onExportJsonClick,
                    onImportJsonClick = onImportJsonClick
                )
            }
        }
    }

    if (showAddRefillSheet) {
        SmartAddFuelBottomSheet(
            profile = profile,
            existingRecords = records,
            editingRecord = editingRecord,
            getPriceForDate = { millis -> viewModel.getPriceForDate(millis) },
            getMatchedPeriod = { millis -> viewModel.getMatchedPeriodForDate(millis) },
            onDismiss = {
                showAddRefillSheet = false
                editingRecord = null
            },
            onSave = { id, epochMillis, odo, cost, unitPrice, station, isFull ->
                viewModel.addOrUpdateFuelRecord(
                    id = id,
                    epochMillis = epochMillis,
                    odo = odo,
                    totalCost = cost,
                    pricePerLiter = unitPrice,
                    stationName = station,
                    isFullTank = isFull
                )
                showAddRefillSheet = false
                editingRecord = null
            }
        )
    }

    if (showManualPriceDialog) {
        ManualUpdatePriceDialog(
            currentPrice = profile.currentFuelPrice,
            onDismiss = { showManualPriceDialog = false },
            onConfirm = { zone1, zone2, note ->
                viewModel.updateManualPrice(zone1, zone2, note)
                showManualPriceDialog = false
            }
        )
    }
}

@Composable
private fun FuelLogTabContent(
    profile: CarProfile,
    records: List<FuelRecordEntity>,
    periods: List<FuelPricePeriodEntity>,
    isSyncing: Boolean,
    syncStatus: String,
    onRefreshPrice: () -> Unit,
    onOpenManualPrice: () -> Unit,
    onSwitchZone: (PriceZone) -> Unit,
    onAddClick: () -> Unit,
    onEditRecord: (FuelRecordEntity) -> Unit,
    onDeleteRecord: (FuelRecordEntity) -> Unit
) {
    // Tính toán thống kê
    val nowZone = Instant.now().atZone(ZoneId.systemDefault())
    val thisMonthRecords = records.filter {
        val dt = Instant.ofEpochMilli(it.epochMillis).atZone(ZoneId.systemDefault())
        dt.month == nowZone.month && dt.year == nowZone.year
    }
    val thisMonthCost = thisMonthRecords.sumOf { it.totalCost }
    val thisMonthLiters = thisMonthRecords.sumOf { it.liters }
    val totalCost = records.sumOf { it.totalCost }
    val totalLiters = records.sumOf { it.liters }

    // Tính mức tiêu hao thực tế L/100km & đ/km theo ODO
    val sortedByOdo = records.sortedWith(compareBy<FuelRecordEntity> { it.odo }.thenBy { it.epochMillis })
    val totalDistance = if (sortedByOdo.size >= 2) {
        sortedByOdo.last().odo - sortedByOdo.first().odo
    } else 0

    val avgL100Km = if (sortedByOdo.size >= 2 && totalDistance > 50) {
        val fuelUsedAfterFirst = sortedByOdo.drop(1).sumOf { it.liters }
        (fuelUsedAfterFirst / totalDistance.toDouble()) * 100.0
    } else {
        profile.standardConsumptionL100Km
    }

    val avgCostPerKm = if (sortedByOdo.size >= 2 && totalDistance > 50) {
        val costAfterFirst = sortedByOdo.drop(1).sumOf { it.totalCost }
        costAfterFirst / totalDistance.toDouble()
    } else {
        (profile.standardConsumptionL100Km / 100.0) * profile.currentFuelPrice
    }

    val priceDeltaVsPrev = if (periods.size >= 2) {
        val p0 = if (profile.selectedPriceZone == PriceZone.ZONE_1) periods[0].priceZone1 else periods[0].priceZone2
        val p1 = if (profile.selectedPriceZone == PriceZone.ZONE_1) periods[1].priceZone1 else periods[1].priceZone2
        (p0 - p1).toInt()
    } else 0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Banner Giá Xăng Tự Động Cập Nhật
        item {
            RealtimePriceBannerCard(
                profile = profile,
                priceDelta = priceDeltaVsPrev,
                isSyncing = isSyncing,
                syncStatus = syncStatus,
                onRefreshPrice = onRefreshPrice,
                onOpenManualPrice = onOpenManualPrice,
                onSwitchZone = onSwitchZone
            )
        }

        // 2. Lưới 4 chỉ số KPI (Tiền xăng tháng này, Tiêu thụ thực tế, Chi phí/km, Tổng tích lũy)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        modifier = Modifier.weight(1f),
                        title = "THÁNG NÀY",
                        value = FormatUtils.formatVnd(thisMonthCost),
                        subtitle = "${thisMonthRecords.size} lần • ${String.format("%.1f", thisMonthLiters)} Lít",
                        accentColor = FuelOrange
                    )
                    KpiStatCard(
                        modifier = Modifier.weight(1f),
                        title = "MỨC TIÊU THỤ",
                        value = FormatUtils.formatL100Km(avgL100Km),
                        subtitle = if (totalDistance > 50) "Qua $totalDistance km ODO" else "Chuẩn SkyActiv 1.5L",
                        accentColor = EcoGreen
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiStatCard(
                        modifier = Modifier.weight(1f),
                        title = "CHI PHÍ / KM",
                        value = "${FormatUtils.formatVndNumberOnly(avgCostPerKm)} đ/km",
                        subtitle = "~${FormatUtils.formatVnd(avgCostPerKm * profile.dailyKm)} / ngày (${profile.dailyKm}km)",
                        accentColor = CostPurple
                    )
                    KpiStatCard(
                        modifier = Modifier.weight(1f),
                        title = "TỔNG ĐÃ CHI",
                        value = FormatUtils.formatVnd(totalCost),
                        subtitle = "${records.size} lần • ${String.format("%.1f", totalLiters)} Lít",
                        accentColor = PetrolBlue
                    )
                }
            }
        }

        // 3. Tiêu đề danh sách lịch sử
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LỊCH SỬ ĐỔ XĂNG (${records.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onAddClick) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Thêm mới", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (records.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = null,
                            tint = FuelOrange,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Chưa có nhật ký đổ xăng", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Chỉ cần nhập số tiền vừa đổ (ví dụ 500.000đ), ứng dụng sẽ tự động quy đổi số lít theo giá Petrolimex hôm nay.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(records, key = { it.id }) { item ->
                FuelRecordItemCard(
                    record = item,
                    onEdit = { onEditRecord(item) },
                    onDelete = { onDeleteRecord(item) }
                )
            }
        }
    }
}

@Composable
private fun RealtimePriceBannerCard(
    profile: CarProfile,
    priceDelta: Int,
    isSyncing: Boolean,
    syncStatus: String,
    onRefreshPrice: () -> Unit,
    onOpenManualPrice: () -> Unit,
    onSwitchZone: (PriceZone) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = PetrolBlue.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "PETROLIMEX",
                            color = PetrolBlue,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "${profile.selectedFuelProduct.shortName} • ${profile.selectedPriceZone.shortLabel.uppercase()}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Nút chuyển nhanh Vùng 1 / Vùng 2
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PriceZone.entries.forEach { zone ->
                        val selected = profile.selectedPriceZone == zone
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSwitchZone(zone) },
                            color = if (selected) FuelOrange.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = zone.shortLabel,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) FuelOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = FormatUtils.formatVnd(profile.currentFuelPrice),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " / Lít",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp)
                        )
                    }

                    if (priceDelta != 0) {
                        val isDown = priceDelta < 0
                        val badgeColor = if (isDown) EcoGreen else Color(0xFFDC2626)
                        val prefix = if (priceDelta > 0) "+" else ""
                        Text(
                            text = "${if (isDown) "▼ Giảm" else "▲ Tăng"} $prefix${FormatUtils.formatVnd(priceDelta.toDouble())} so với kỳ trước",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onOpenManualPrice,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Chỉnh giá", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isSyncing) { onRefreshPrice() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = EcoGreen,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = syncStatus,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "Cập nhật ngay",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PetrolBlue
                )
            }
        }
    }
}

@Composable
private fun KpiStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color
) {
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
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FuelRecordItemCard(
    record: FuelRecordEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() },
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
                        text = FormatUtils.formatDateTime(record.epochMillis),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = record.stationName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FormatUtils.formatVnd(record.totalCost),
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = FuelOrange
                    )
                    Text(
                        text = "${FormatUtils.formatLiters(record.liters)} @ ${FormatUtils.formatVndNumberOnly(record.pricePerLiter)}đ/L",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "ODO: ${FormatUtils.formatVndNumberOnly(record.odo.toDouble())} km",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if ((record.tripKm ?: 0) > 0) {
                        Surface(
                            color = PetrolBlue.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "+${record.tripKm} km",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = PetrolBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if ((record.fuelEfficiency ?: 0.0) > 0.0) {
                        Surface(
                            color = EcoGreen.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = FormatUtils.formatL100Km(record.fuelEfficiency!!),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EcoGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xóa lần đổ xăng này?") },
            text = {
                Text("Bạn có chắc muốn xóa bản ghi ${FormatUtils.formatVnd(record.totalCost)} ngày ${FormatUtils.formatDateTime(record.epochMillis)} không?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    }
                ) {
                    Text("Xóa", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SmartAddFuelBottomSheet(
    profile: CarProfile,
    existingRecords: List<FuelRecordEntity>,
    editingRecord: FuelRecordEntity?,
    getPriceForDate: (Long) -> Double,
    getMatchedPeriod: (Long) -> FuelPricePeriodEntity?,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        epochMillis: Long,
        odo: Int,
        totalCost: Double,
        pricePerLiter: Double,
        stationName: String,
        isFullTank: Boolean
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val nowMillis = remember { System.currentTimeMillis() }

    var selectedEpochMillis by remember {
        mutableLongStateOf(editingRecord?.epochMillis ?: nowMillis)
    }
    var unitPriceText by remember {
        val initPrice = editingRecord?.pricePerLiter ?: getPriceForDate(selectedEpochMillis)
        mutableStateOf(initPrice.toLong().toString())
    }
    var costText by remember {
        val initCost = editingRecord?.totalCost ?: 500_000.0
        mutableStateOf(initCost.toLong().toString())
    }
    var odoText by remember {
        val initOdo = editingRecord?.odo ?: profile.currentOdo
        mutableStateOf(initOdo.toString())
    }
    var stationText by remember {
        mutableStateOf(
            editingRecord?.stationName
                ?: profile.favoriteStations.firstOrNull()
                ?: "Petrolimex CHXD 101 Cao Lãnh"
        )
    }
    var isFullTank by remember {
        mutableStateOf(editingRecord?.isFullTank ?: false)
    }

    val parsedPrice = unitPriceText.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: profile.currentFuelPrice
    val parsedCost = costText.replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0
    val autoLiters = if (parsedPrice > 0) parsedCost / parsedPrice else 0.0
    val tankPercent = if (profile.tankCapacityLiters > 0) {
        ((autoLiters / profile.tankCapacityLiters) * 100.0).roundToInt().coerceAtMost(100)
    } else 0
    val estRangeKm = if (profile.standardConsumptionL100Km > 0) {
        (autoLiters / (profile.standardConsumptionL100Km / 100.0)).roundToInt()
    } else 0

    val prevRecord = existingRecords.firstOrNull { it.id != editingRecord?.id }
    val currentOdoVal = odoText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: profile.currentOdo
    val tripVal = if (prevRecord != null && currentOdoVal > prevRecord.odo) {
        currentOdoVal - prevRecord.odo
    } else 0

    val matchedPeriod = getMatchedPeriod(selectedEpochMillis)
    val quickAmounts = listOf(300_000, 500_000, 700_000, 1_000_000)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (editingRecord == null) "Ghi Nhận Lần Đổ Xăng Mới" else "Chỉnh Sửa Lần Đổ Xăng",
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // 1. Chọn nhanh ngày đổ -> Tự động khớp giá xăng kỳ đó
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Thời điểm đổ: ${FormatUtils.formatDateTime(selectedEpochMillis)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Hôm nay" to 0L,
                        "Hôm qua" to 1L,
                        "2 ngày trước" to 2L
                    ).forEach { (label, daysAgo) ->
                        AssistChip(
                            onClick = {
                                val targetMillis = System.currentTimeMillis() - daysAgo * 86_400_000L
                                selectedEpochMillis = targetMillis
                                unitPriceText = getPriceForDate(targetMillis).toLong().toString()
                            },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
                if (matchedPeriod != null) {
                    Text(
                        text = "⚡ Tự động khớp giá kỳ: ${matchedPeriod.note}",
                        fontSize = 11.sp,
                        color = EcoGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 2. Nhập số tiền & Đơn giá
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = costText,
                    onValueChange = { costText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Số tiền đổ (VNĐ)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                OutlinedTextField(
                    value = unitPriceText,
                    onValueChange = { unitPriceText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Đơn giá (đ/L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Các nút chọn nhanh số tiền (1-chạm)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                quickAmounts.forEach { amt ->
                    FilterChip(
                        selected = parsedCost.toInt() == amt,
                        onClick = {
                            costText = amt.toString()
                            isFullTank = false
                        },
                        label = { Text("${amt / 1000}k", fontWeight = FontWeight.Bold) }
                    )
                }
                FilterChip(
                    selected = isFullTank,
                    onClick = {
                        val fullCost = (profile.tankCapacityLiters * parsedPrice).roundToInt()
                        costText = fullCost.toString()
                        isFullTank = true
                    },
                    label = { Text("Đầy bình (~${profile.tankCapacityLiters.toInt()}L)") }
                )
            }

            // 3. Hộp Kết Quả Tính Tự Động (Số lít, % bình, Quãng đường dự kiến)
            Surface(
                color = PetrolBlue.copy(alpha = 0.08f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PetrolBlue.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LƯỢNG XĂNG NHẬN ĐƯỢC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = FormatUtils.formatLiters(autoLiters),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = PetrolBlue
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TỈ LỆ BÌNH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "~$tankPercent% bình",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DỰ KIẾN ĐI ĐƯỢC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "~$estRangeKm km",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoGreen
                        )
                    }
                }
            }

            // 4. Nhập số ODO hiện tại
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Số ODO trên đồng hồ xe (km):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    if (prevRecord != null) {
                        Text(
                            text = "Lần trước: ${prevRecord.odo} km",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedTextField(
                    value = odoText,
                    onValueChange = { odoText = it.filter { ch -> ch.isDigit() } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    trailingIcon = {
                        if (tripVal > 0) {
                            Text(
                                text = "+$tripVal km",
                                color = PetrolBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Chọn cây xăng
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = stationText,
                    onValueChange = { stationText = it },
                    label = { Text("Cây xăng") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    profile.favoriteStations.forEach { st ->
                        AssistChip(
                            onClick = { stationText = st },
                            label = {
                                Text(
                                    text = st.replace("Petrolimex ", ""),
                                    fontSize = 12.sp
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (stationText == st) {
                                    FuelOrange.copy(alpha = 0.14f)
                                } else {
                                    Color.Transparent
                                }
                            )
                        )
                    }
                }
            }

            // Nút Lưu
            Button(
                onClick = {
                    onSave(
                        editingRecord?.id,
                        selectedEpochMillis,
                        currentOdoVal,
                        parsedCost,
                        parsedPrice,
                        stationText,
                        isFullTank
                    )
                },
                enabled = parsedCost > 0 && parsedPrice > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FuelOrange)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Lưu Lần Đổ Xăng Này",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun PriceAndAnalyticsTabContent(
    profile: CarProfile,
    liveItems: List<LiveFuelItemPrice>,
    periods: List<FuelPricePeriodEntity>,
    records: List<FuelRecordEntity>,
    isSyncing: Boolean,
    syncStatus: String,
    onRefreshLive: () -> Unit,
    onSelectFuelAndZone: (FuelProduct, PriceZone) -> Unit,
    onOpenManualPrice: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Bảng Giá Xăng Dầu Petrolimex Hôm Nay (Vùng 1 & Vùng 2)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BẢNG GIÁ BÁN LẺ PETROLIMEX",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Text(
                                text = syncStatus,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onRefreshLive,
                            enabled = !isSyncing,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Làm mới", fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Header bảng
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mặt hàng (Chạm để chọn)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1.5f)
                        )
                        Text(
                            text = "Vùng 1",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(0.9f)
                        )
                        Text(
                            text = "Vùng 2 (Tỉnh)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    liveItems.forEach { item ->
                        val isSelectedProduct = profile.selectedFuelProduct == item.product
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelectedProduct) FuelOrange.copy(alpha = 0.1f)
                                    else Color.Transparent
                                )
                                .clickable {
                                    onSelectFuelAndZone(item.product, profile.selectedPriceZone)
                                }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(
                                    text = item.product.shortName,
                                    fontWeight = if (isSelectedProduct) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = if (isSelectedProduct) FuelOrange else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelectedProduct) {
                                    Text(
                                        text = "Đang dùng cho xe",
                                        fontSize = 10.sp,
                                        color = FuelOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = FormatUtils.formatVnd(item.priceZone1),
                                fontSize = 13.sp,
                                fontWeight = if (isSelectedProduct && profile.selectedPriceZone == PriceZone.ZONE_1) FontWeight.Black else FontWeight.Medium,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .clickable { onSelectFuelAndZone(item.product, PriceZone.ZONE_1) }
                            )
                            Text(
                                text = FormatUtils.formatVnd(item.priceZone2),
                                fontSize = 13.sp,
                                fontWeight = if (isSelectedProduct && profile.selectedPriceZone == PriceZone.ZONE_2) FontWeight.Black else FontWeight.Bold,
                                color = if (isSelectedProduct && profile.selectedPriceZone == PriceZone.ZONE_2) FuelOrange else PetrolBlue,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onSelectFuelAndZone(item.product, PriceZone.ZONE_2) }
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // 2. Biểu Đồ Biến Động Giá Xăng Qua Các Kỳ Điều Hành
        if (periods.size >= 2) {
            item {
                PriceHistoryChartCard(
                    periods = periods,
                    selectedZone = profile.selectedPriceZone
                )
            }
        }

        // 3. Danh sách lịch sử các kỳ điều chỉnh giá xăng
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LỊCH SỬ KỲ ĐIỀU HÀNH GIÁ (${periods.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpenManualPrice) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Thêm kỳ giá", fontSize = 12.sp)
                }
            }
        }

        items(periods, key = { it.id }) { p ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p.note, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            text = "Áp dụng: ${FormatUtils.formatDateOnly(p.epochMillis)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "V2: ${FormatUtils.formatVnd(p.priceZone2)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = FuelOrange
                        )
                        Text(
                            text = "V1: ${FormatUtils.formatVnd(p.priceZone1)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PriceHistoryChartCard(
    periods: List<FuelPricePeriodEntity>,
    selectedZone: PriceZone
) {
    val chronological = periods.take(10).reversed()
    val prices = chronological.map {
        if (selectedZone == PriceZone.ZONE_1) it.priceZone1 else it.priceZone2
    }
    val minPrice = (prices.minOrNull() ?: 20000.0) - 300.0
    val maxPrice = (prices.maxOrNull() ?: 30000.0) + 300.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "BIẾN ĐỘNG GIÁ XĂNG (${selectedZone.shortLabel.uppercase()})",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                text = "Từ ${FormatUtils.formatVnd(prices.first())} đến ${FormatUtils.formatVnd(prices.last())} qua ${chronological.size} kỳ gần nhất",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                if (prices.size < 2) return@Canvas
                val stepX = size.width / (prices.size - 1).coerceAtLeast(1)
                val rangeY = (maxPrice - minPrice).coerceAtLeast(1.0)

                val points = prices.mapIndexed { idx, price ->
                    val x = idx * stepX
                    val normalizedY = ((price - minPrice) / rangeY).toFloat()
                    val y = size.height - (normalizedY * size.height)
                    Offset(x, y)
                }

                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }

                drawPath(
                    path = path,
                    color = FuelOrange,
                    style = Stroke(width = 6f)
                )

                points.forEach { pt ->
                    drawCircle(
                        color = Color.White,
                        radius = 10f,
                        center = pt
                    )
                    drawCircle(
                        color = FuelOrange,
                        radius = 7f,
                        center = pt
                    )
                }
            }
        }
    }
}

@Composable
private fun CarAndBackupTabContent(
    profile: CarProfile,
    recordsCount: Int,
    onSaveProfile: (CarProfile) -> Unit,
    onExportJsonClick: () -> Unit,
    onImportJsonClick: () -> Unit
) {
    var carName by remember(profile) { mutableStateOf(profile.carName) }
    var plateNumber by remember(profile) { mutableStateOf(profile.plateNumber) }
    var currentOdo by remember(profile) { mutableStateOf(profile.currentOdo.toString()) }
    var dailyKm by remember(profile) { mutableStateOf(profile.dailyKm.toString()) }
    var tankCap by remember(profile) { mutableStateOf(profile.tankCapacityLiters.toInt().toString()) }
    var stationsText by remember(profile) { mutableStateOf(profile.favoriteStations.joinToString("\n")) }
    var savedBanner by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Khối Sao lưu & Đồng bộ với máy Mac (mazda2_car_data.json)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "SAO LƯU OFFLINE & ĐỒNG BỘ MAC (JSON)",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    text = "Dữ liệu được lưu trữ an toàn ngay trên điện thoại (Room SQLite - hiện có $recordsCount bản ghi). Định dạng file JSON tương thích 100% với file mazda2_car_data.json trên ứng dụng Mazda2Care của máy Mac.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportJsonClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PetrolBlue)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Xuất file JSON")
                    }
                    OutlinedButton(
                        onClick = onImportJsonClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nhập file JSON")
                    }
                }
            }
        }

        // 2. Cấu hình thông tin xe & cây xăng quen thuộc
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "THÔNG TIN XE & CÂY XĂNG QUEN THUỘC",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = carName,
                    onValueChange = { carName = it },
                    label = { Text("Tên xe") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = plateNumber,
                        onValueChange = { plateNumber = it },
                        label = { Text("Biển số") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = tankCap,
                        onValueChange = { tankCap = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Bình xăng (Lít)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = currentOdo,
                        onValueChange = { currentOdo = it.filter { ch -> ch.isDigit() } },
                        label = { Text("ODO hiện tại (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dailyKm,
                        onValueChange = { dailyKm = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Quãng đường/ngày (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = stationsText,
                    onValueChange = { stationsText = it },
                    label = { Text("Danh sách cây xăng chọn nhanh (mỗi dòng 1 cây xăng)") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val stations = stationsText.lines().map { it.trim() }.filter { it.isNotEmpty() }
                        onSaveProfile(
                            profile.copy(
                                carName = carName.ifBlank { profile.carName },
                                plateNumber = plateNumber.ifBlank { profile.plateNumber },
                                tankCapacityLiters = tankCap.toDoubleOrNull() ?: 38.0,
                                currentOdo = currentOdo.toIntOrNull() ?: profile.currentOdo,
                                dailyKm = dailyKm.toIntOrNull() ?: profile.dailyKm,
                                favoriteStations = if (stations.isEmpty()) profile.favoriteStations else stations
                            )
                        )
                        savedBanner = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FuelOrange)
                ) {
                    Text("Lưu Cấu Hình Xe", fontWeight = FontWeight.Bold)
                }

                if (savedBanner) {
                    Text(
                        text = "✓ Đã lưu cấu hình xe thành công!",
                        fontSize = 12.sp,
                        color = EcoGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualUpdatePriceDialog(
    currentPrice: Double,
    onDismiss: () -> Unit,
    onConfirm: (zone1: Double, zone2: Double, note: String) -> Unit
) {
    var zone2Input by remember { mutableStateOf(currentPrice.toInt().toString()) }
    var zone1Input by remember {
        val estZ1 = ((currentPrice / 1.02) / 10.0).roundToInt() * 10
        mutableStateOf(estZ1.toString())
    }
    var noteInput by remember { mutableStateOf("Cập nhật theo giá cây xăng thực tế") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cập Nhật Đơn Giá Xăng", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Nhập đơn giá bán lẻ xăng Petrolimex (VNĐ/Lít):",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = zone2Input,
                    onValueChange = {
                        zone2Input = it.filter { ch -> ch.isDigit() }
                        val z2 = zone2Input.toDoubleOrNull()
                        if (z2 != null && z2 > 10000) {
                            zone1Input = (((z2 / 1.02) / 10.0).roundToInt() * 10).toString()
                        }
                    },
                    label = { Text("Giá Vùng 2 - Đồng Tháp (đ/L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = zone1Input,
                    onValueChange = { zone1Input = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Giá Vùng 1 (đ/L)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("Ghi chú kỳ điều chỉnh") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val z2 = zone2Input.toDoubleOrNull() ?: currentPrice
                    val z1 = zone1Input.toDoubleOrNull() ?: z2
                    onConfirm(z1, z2, noteInput)
                }
            ) {
                Text("Lưu giá mới")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
