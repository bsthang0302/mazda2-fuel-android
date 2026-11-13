package com.mazda2care.fuellog.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mazda2care.fuellog.model.CarProfile
import com.mazda2care.fuellog.model.FormatUtils
import com.mazda2care.fuellog.ui.theme.CostPurple
import com.mazda2care.fuellog.ui.theme.EcoGreen
import com.mazda2care.fuellog.ui.theme.FuelOrange
import com.mazda2care.fuellog.ui.theme.PetrolBlue
import kotlin.math.ceil

data class ServiceCheckItem(
    val name: String,
    val specAndType: String,
    val costVnd: Int
)

data class ServiceMilestonePlan(
    val stepIndex: Int,
    val relativeKmFromFirst: Int, // 0, 5000, 10000, 15000, 20000...
    val standardOdoKm: Int,       // 1000, 6000, 11000, 16000, 21000... (hoặc mốc chuẩn)
    val badgeTitle: String,
    val title: String,
    val subtitle: String,
    val isFreeLabor: Boolean = false,
    val freeLaborNote: String = "",
    val mustDoItems: List<ServiceCheckItem>,
    val skipItems: List<ServiceCheckItem>,
    val smartSavingTip: String,
    val advisorScript: String,
    val standardTotalVnd: Int,
    val dealerInflatedVnd: Int
)

data class PartSpecReference(
    val name: String,
    val recommendedType: String,
    val replaceCycle: String,
    val smartTip: String
)

@Composable
fun MaintenanceGuideTabContent(
    profile: CarProfile,
    onUpdateProfile: (CarProfile) -> Unit
) {
    val context = LocalContext.current
    val allPlans = remember(profile.lastMaintenanceOdo) {
        buildMazda2MaintenancePlans()
    }

    // Tự động chọn mốc bảo dưỡng tiếp theo dựa trên ODO đã bảo dưỡng gần nhất (mặc định: đã xong 1.000km -> chọn Mốc 2)
    val suggestedPlanIndex = remember(profile.lastMaintenanceOdo, allPlans) {
        val nextOdo = profile.nextMaintenanceOdo
        val idx = allPlans.indexOfFirst { it.standardOdoKm >= nextOdo - 1000 }
        if (idx >= 0) idx else 1
    }

    var selectedPlanIdx by remember(suggestedPlanIndex) { mutableIntStateOf(suggestedPlanIndex) }
    var showUpdateOdoDialog by remember { mutableStateOf(false) }
    var copiedScriptToast by remember { mutableStateOf(false) }

    val activePlan = allPlans.getOrElse(selectedPlanIdx) { allPlans[1] }
    val nextPlan = allPlans.getOrElse(suggestedPlanIndex) { allPlans[1] }

    val kmDrivenSinceLast = (profile.currentOdo - profile.lastMaintenanceOdo).coerceAtLeast(0)
    val progressRatio = (kmDrivenSinceLast.toFloat() / profile.maintenanceIntervalKm.coerceAtLeast(1).toFloat())
        .coerceIn(0f, 1f)
    val kmRemaining = profile.kmUntilNextMaintenance
    val estDaysRemaining = ceil(kmRemaining.toDouble() / profile.dailyKm.coerceAtLeast(1).toDouble()).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Thẻ Đếm Ngược & Nhắc Nhở Mốc Bảo Dưỡng Tiếp Theo (+5.000 km)
        item {
            NextMaintenanceCountdownCard(
                profile = profile,
                nextPlan = nextPlan,
                kmRemaining = kmRemaining,
                estDaysRemaining = estDaysRemaining,
                progressRatio = progressRatio,
                onMarkCompletedClick = { showUpdateOdoDialog = true }
            )
        }

        // 2. Quy tắc vàng "Chuẩn Hãng Nhưng Tiết Kiệm Nhất" cho chu kỳ mỗi 5.000 km
        item {
            GoldenFormulaSummaryCard()
        }

        // 3. Thanh chọn các mốc bảo dưỡng (Mốc 1: 1.000km [Đã xong] -> Mốc 2 (+5k) -> Mốc 3 (+10k)...)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "CHỌN XEM CHI TIẾT TỪNG MỐC (MỖI +5.000 KM)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allPlans.forEachIndexed { idx, plan ->
                        val isDone = plan.standardOdoKm <= profile.lastMaintenanceOdo
                        val isSelected = selectedPlanIdx == idx
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPlanIdx = idx },
                            label = {
                                Text(
                                    text = if (isDone) "✓ ${plan.badgeTitle}" else plan.badgeTitle,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 12.sp
                                )
                            }
                        )
                    }
                }
            }
        }

        // 4. Chi tiết Mốc Đang Chọn: Thay gì, Loại nào, Gạch bỏ gì, Lời thoại cố vấn
        item {
            MilestoneDetailCard(
                plan = activePlan,
                isCompleted = activePlan.standardOdoKm <= profile.lastMaintenanceOdo,
                copiedToast = copiedScriptToast,
                onCopyScript = { script ->
                    val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cb.setPrimaryClip(ClipData.newPlainText("Lời thoại bảo dưỡng", script))
                    copiedScriptToast = true
                }
            )
        }

        // 5. Bảng Tra Cứu Nhanh: Loại Nhớt, Lọc, Phụ Tùng Chuẩn Cho Mazda 2
        item {
            PartsCheatSheetCard()
        }
    }

    if (showUpdateOdoDialog) {
        UpdateMaintenanceOdoDialog(
            profile = profile,
            onDismiss = { showUpdateOdoDialog = false },
            onSave = { newLastOdo, newCurrentOdo ->
                onUpdateProfile(
                    profile.copy(
                        lastMaintenanceOdo = newLastOdo,
                        currentOdo = maxOf(profile.currentOdo, newCurrentOdo)
                    )
                )
                showUpdateOdoDialog = false
            }
        )
    }
}

@Composable
fun HomeMaintenanceReminderBanner(
    profile: CarProfile,
    onOpenMaintenanceTab: () -> Unit
) {
    val kmRemaining = profile.kmUntilNextMaintenance
    val isUrgent = kmRemaining <= 500
    val estDays = ceil(kmRemaining.toDouble() / profile.dailyKm.coerceAtLeast(1).toDouble()).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenMaintenanceTab() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUrgent) Color(0xFFFEF2F2) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = if (isUrgent) Color(0xFFDC2626).copy(alpha = 0.15f) else EcoGreen.copy(alpha = 0.14f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isUrgent) Icons.Default.NotificationsActive else Icons.Default.Build,
                            contentDescription = null,
                            tint = if (isUrgent) Color(0xFFDC2626) else EcoGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isUrgent) "SẮP TỚI MỐC BẢO DƯỠNG!" else "NHẮC BẢO DƯỠNG (+5.000 KM)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isUrgent) Color(0xFFDC2626) else EcoGreen
                        )
                        Text(
                            text = "• Đã xong mốc ${FormatUtils.formatVndNumberOnly(profile.lastMaintenanceOdo.toDouble())}km",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Mốc kế: ${FormatUtils.formatVndNumberOnly(profile.nextMaintenanceOdo.toDouble())} km (Còn ${FormatUtils.formatVndNumberOnly(kmRemaining.toDouble())} km • ~$estDays ngày)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Xem làm gì →",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PetrolBlue
            )
        }
    }
}

@Composable
private fun NextMaintenanceCountdownCard(
    profile: CarProfile,
    nextPlan: ServiceMilestonePlan,
    kmRemaining: Int,
    estDaysRemaining: Int,
    progressRatio: Float,
    onMarkCompletedClick: () -> Unit
) {
    val savedMoney = (nextPlan.dealerInflatedVnd - nextPlan.standardTotalVnd).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = EcoGreen.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "✓ ĐÃ BẢO DƯỠNG MỐC ${FormatUtils.formatVndNumberOnly(profile.lastMaintenanceOdo.toDouble())} KM",
                        color = Color(0xFF4ADE80),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "Chu kỳ +5.000 km",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = "KỲ TIẾP THEO: ODO ${FormatUtils.formatVndNumberOnly(profile.nextMaintenanceOdo.toDouble())} KM",
                color = FuelOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = nextPlan.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "CÒN LẠI ĐẾN KỲ BẢO DƯỠNG",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${FormatUtils.formatVndNumberOnly(kmRemaining.toDouble())} km",
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Khoảng ~$estDaysRemaining ngày nữa (với lộ trình ${profile.dailyKm} km/ngày)",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "CHI PHÍ CHUẨN TIẾT KIỆM",
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = FormatUtils.formatVnd(nextPlan.standardTotalVnd.toDouble()),
                        color = Color(0xFF4ADE80),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Tiết kiệm ${FormatUtils.formatVnd(savedMoney.toDouble())} so với hãng vẽ",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progressRatio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (progressRatio > 0.85f) FuelOrange else Color(0xFF4ADE80),
                trackColor = Color.White.copy(alpha = 0.15f)
            )

            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Mốc trước: ${profile.lastMaintenanceOdo} km",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Text(
                    text = "ODO hiện tại: ${profile.currentOdo} km",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Mốc tới: ${profile.nextMaintenanceOdo} km",
                    color = FuelOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onMarkCompletedClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FuelOrange)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Cập Nhật Mốc Đã Bảo Dưỡng",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GoldenFormulaSummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = PetrolBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "QUY TẮC VÀNG: CHUẨN HÃNG & TIẾT KIỆM NHẤT",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "• Cứ mỗi 5.000 km (Mốc 2, 4, 6, 8...): Chỉ thay Nhớt máy Mazda 0W-20 (3.6 Lít) + Long-đền ốc xả (15k) + Xịt bụi lọc gió (0đ). Chưa thay lọc nhớt! (~795k)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "• Cứ mỗi 10.000 km (Mốc 3, 5, 7...): Thay Nhớt 0W-20 (3.8 Lít) + Thay Lọc nhớt chính hãng (PE01-14-302A - 165k) + Đảo lốp 4 bánh + Tự đổ 1 chai súc béc GDI Techron (~190k).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "• Cứ mỗi 20.000 km (Mốc 5, 9...): Thay mới Lọc gió động cơ & Lọc gió điều hòa + Vệ sinh bảo dưỡng phanh 4 bánh.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "• Tuyệt đối KHÔNG thay sớm: Nước làm mát FL22 và Dầu hộp số 6AT ATF-FZ có tuổi thọ chuẩn 100.000 km (5 năm)!",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = EcoGreen
            )
        }
    }
}

@Composable
private fun MilestoneDetailCard(
    plan: ServiceMilestonePlan,
    isCompleted: Boolean,
    copiedToast: Boolean,
    onCopyScript: (String) -> Unit
) {
    val savedAmount = (plan.dealerInflatedVnd - plan.standardTotalVnd).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                    Text(
                        text = plan.subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isCompleted) {
                    Surface(
                        color = EcoGreen.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "ĐÃ HOÀN THÀNH ✓",
                            color = EcoGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else if (plan.isFreeLabor) {
                    Surface(
                        color = FuelOrange.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "MIỄN PHÍ CÔNG",
                            color = FuelOrange,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (plan.isFreeLabor && plan.freeLaborNote.isNotBlank()) {
                Surface(
                    color = EcoGreen.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎁 ${plan.freeLaborNote}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EcoGreen,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Phần 1: BẮT BUỘC LÀM & LOẠI PHỤ TÙNG CHUẨN NHẤT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EcoGreen.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .border(1.dp, EcoGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = EcoGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "NÊN BẢO DƯỠNG GÌ • THAY LOẠI NÀO CHUẨN NHẤT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = EcoGreen
                    )
                }

                plan.mustDoItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "• ${item.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.specAndType,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (item.costVnd == 0) "0 đ" else FormatUtils.formatVnd(item.costVnd.toDouble()),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EcoGreen
                        )
                    }
                }

                HorizontalDivider(color = EcoGreen.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TỔNG CHI PHÍ CHUẨN:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = FormatUtils.formatVnd(plan.standardTotalVnd.toDouble()),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = EcoGreen
                    )
                }
            }

            // Phần 2: MẸO TIẾT KIỆM THÔNG MINH CHO MỐC NÀY
            Surface(
                color = PetrolBlue.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "💡 MẸO TIẾT KIỆM MỐC NÀY:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = PetrolBlue
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = plan.smartSavingTip,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Phần 3: HẠNG MỤC HÃNG HAY VẼ THÊM — CẦN GẠCH BỎ
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFDC2626).copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFDC2626).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "HÃNG HAY VẼ THÊM — GẠCH BỎ ĐỂ TIẾT KIỆM ${FormatUtils.formatVnd(savedAmount.toDouble())}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFDC2626)
                    )
                }

                plan.skipItems.forEach { skip ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "✕ ${skip.name}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                            Text(
                                text = skip.specAndType,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "-${FormatUtils.formatVnd(skip.costVnd.toDouble())}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // Phần 4: LỜI THOẠI NÓI VỚI CỐ VẤN DỊCH VỤ (1-Chạm Copy)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗣️ CÂU NÓI VỚI CỐ VẤN DỊCH VỤ THACO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = { onCopyScript(plan.advisorScript) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (copiedToast) "Đã chép!" else "Chép lời thoại",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "\"${plan.advisorScript}\"",
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PartsCheatSheetCard() {
    val specs = remember {
        listOf(
            PartSpecReference(
                name = "1. Nhớt động cơ SkyActiv-G 1.5L",
                recommendedType = "Mazda Genuine Oil 0W-20 SN/GF-5 (hoặc Mobil 1 / Castrol 0W-20)",
                replaceCycle = "Mỗi 5.000 km (3.6L nếu không thay lọc • 3.8L nếu thay lọc)",
                smartTip = "Động cơ SkyActiv tỷ số nén 13:1 khe hở hẹp, chạy 0W-20 máy nhẹ, bốc và tiết kiệm xăng nhất."
            ),
            PartSpecReference(
                name = "2. Lọc nhớt (Lọc dầu động cơ)",
                recommendedType = "Chính hãng Mazda mã PE01-14-302A / PE01-14-302B (~165.000 đ)",
                replaceCycle = "Mỗi 10.000 km (2 lần thay nhớt = 1 lần thay lọc nhớt)",
                smartTip = "Lọc nhớt mua chính hãng trong Thaco rất rẻ (165k), không nên dùng lọc trôi nổi ngoài chợ."
            ),
            PartSpecReference(
                name = "3. Dung dịch vệ sinh kim phun GDI",
                recommendedType = "Mazda Deposit Cleaner hoặc Chevron Techron Concentrate Plus (~190.000 đ)",
                replaceCycle = "Mỗi 10.000 km (tự đổ vào bình xăng trước khi đổ đầy)",
                smartTip = "Xe dùng xăng RON 95-III cần giữ sạch đầu béc phun trực tiếp GDI. Tự mua đổ tiết kiệm 70% so với hãng."
            ),
            PartSpecReference(
                name = "4. Lọc gió Động cơ & Lọc gió Điều hòa",
                recommendedType = "Lọc gió máy Mazda P501-13-3A0 • Lọc lạnh than hoạt tính Denso/Masuma",
                replaceCycle = "Mỗi 5.000 km xịt bụi • Mỗi 20.000 km thay mới",
                smartTip = "Lọc gió điều hòa có thể tự mua loại than hoạt tính (~130k) tự mở hộc đồ phụ thay trong 2 phút."
            ),
            PartSpecReference(
                name = "5. Dầu phanh & Lọc xăng trong bình",
                recommendedType = "Dầu phanh DOT 4 (~1 Lít) • Cụm lọc xăng tinh Mazda 2",
                replaceCycle = "Mỗi 40.000 km (hoặc sau 2 năm)",
                smartTip = "Chạy xăng RON 95-III đúng 40.000 km nên thay lọc xăng để bảo vệ bơm cao áp."
            ),
            PartSpecReference(
                name = "6. Bugi đánh lửa & Dầu hộp số & Nước mát",
                recommendedType = "Bugi NGK/Denso Iridium • Dầu số Mazda ATF-FZ • Nước mát FL22",
                replaceCycle = "Bugi: 60.000 - 80.000 km • Dầu số 6AT & Nước mát FL22: 100.000 km",
                smartTip = "Tuyệt đối từ chối nếu hãng gạ thay nước mát FL22 hoặc dầu hộp số ATF-FZ ở 20k - 40k km!"
            )
        )
    }

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
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = CostPurple,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "CẨM NANG LOẠI NHỚT & PHỤ TÙNG CHUẨN MAZDA 2",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            specs.forEachIndexed { idx, sp ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = sp.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Loại chuẩn: ${sp.recommendedType}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PetrolBlue
                    )
                    Text(
                        text = "Chu kỳ: ${sp.replaceCycle}",
                        fontSize = 11.sp,
                        color = EcoGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "💡 ${sp.smartTip}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (idx < specs.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun UpdateMaintenanceOdoDialog(
    profile: CarProfile,
    onDismiss: () -> Unit,
    onSave: (lastMaintOdo: Int, currentOdo: Int) -> Unit
) {
    var lastOdoText by remember { mutableStateOf(profile.nextMaintenanceOdo.toString()) }
    var currentOdoText by remember { mutableStateOf(maxOf(profile.currentOdo, profile.nextMaintenanceOdo).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cập Nhật Mốc Bảo Dưỡng", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Nhập số ODO của mốc bạn vừa bảo dưỡng xong. Ứng dụng sẽ tự động tính mốc bảo dưỡng tiếp theo (+5.000 km):",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1000, 5000, 6000, 10000, 11000, 15000).chunked(3).first().forEach { preset ->
                        FilterChip(
                            selected = lastOdoText == preset.toString(),
                            onClick = { lastOdoText = preset.toString() },
                            label = { Text("${preset / 1000}k km", fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = lastOdoText,
                    onValueChange = { lastOdoText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ODO lúc bảo dưỡng gần nhất (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                OutlinedTextField(
                    value = currentOdoText,
                    onValueChange = { currentOdoText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ODO hiện tại trên xe (km)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                val previewNext = (lastOdoText.toIntOrNull() ?: profile.lastMaintenanceOdo) + 5000
                Text(
                    text = "⚡ Mốc nhắc tiếp theo sẽ là: ${FormatUtils.formatVndNumberOnly(previewNext.toDouble())} km",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EcoGreen
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lastVal = lastOdoText.toIntOrNull() ?: profile.lastMaintenanceOdo
                    val currVal = currentOdoText.toIntOrNull() ?: profile.currentOdo
                    onSave(lastVal, currVal)
                }
            ) {
                Text("Lưu cập nhật")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

private fun buildMazda2MaintenancePlans(): List<ServiceMilestonePlan> {
    return listOf(
        ServiceMilestonePlan(
            stepIndex = 1,
            relativeKmFromFirst = 0,
            standardOdoKm = 1000,
            badgeTitle = "Mốc 1: 1.000 km",
            title = "Mốc 1: 1.000 KM (Kiểm tra đầu tiên)",
            subtitle = "Đã hoàn thành • Kiểm tra tổng quát sau rodai",
            isFreeLabor = true,
            freeLaborNote = "Miễn phí 100% tiền công bảo dưỡng tại Thaco Mazda",
            mustDoItems = listOf(
                ServiceCheckItem(
                    name = "Kiểm tra & siết ốc gầm bệ, hệ thống treo",
                    specAndType = "Nâng cầu kiểm tra độ rơ và lực siết ốc gầm sau 1.000 km đầu",
                    costVnd = 0
                ),
                ServiceCheckItem(
                    name = "Kiểm tra mức dầu máy, dầu phanh, nước làm mát FL22",
                    specAndType = "Chỉ kiểm tra mức dung dịch, KHÔNG thay nhớt ở 1.000 km",
                    costVnd = 0
                ),
                ServiceCheckItem(
                    name = "Quét lỗi điện tử SkyActiv & cân áp suất 4 lốp (2.2 - 2.3 bar)",
                    specAndType = "Miễn phí theo tiêu chuẩn bảo hành",
                    costVnd = 0
                )
            ),
            skipItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu máy sớm ở 1.000 km",
                    specAndType = "Dầu xuất xưởng theo xe Mazda là dầu tổng hợp 0W-20 chuẩn chạy tới 5.000 - 6.000 km mới cần thay!",
                    costVnd = 750_000
                ),
                ServiceCheckItem(
                    name = "Phụ gia súc rửa động cơ mới",
                    specAndType = "Xe mới tinh 1.000 km hoàn toàn không có cặn bẩn để súc!",
                    costVnd = 350_000
                )
            ),
            smartSavingTip = "Ở mốc 1.000 km bạn đã làm đúng chuẩn: chỉ kiểm tra tổng thể miễn phí 0đ, giữ nguyên nhớt zin xuất xưởng để chạy thêm 5.000 km nữa.",
            advisorScript = "Mốc 1.000km này anh chỉ làm đúng quy trình kiểm tra tổng thể miễn phí theo sổ bảo hành thôi nhé. Không thay nhớt và không thêm bất kỳ dung dịch phụ gia nào.",
            standardTotalVnd = 0,
            dealerInflatedVnd = 1_100_000
        ),

        ServiceMilestonePlan(
            stepIndex = 2,
            relativeKmFromFirst = 5000,
            standardOdoKm = 6000,
            badgeTitle = "Mốc 2: +5.000 km (6k)",
            title = "Mốc 2: +5.000 KM Tiếp Theo (ODO ~5.000 - 6.000 KM)",
            subtitle = "Bảo dưỡng Cấp 1 (Nhỏ) • Thay nhớt lần đầu, CHƯA thay lọc nhớt",
            mustDoItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu động cơ Mazda Genuine Oil 0W-20 (3.6 Lít)",
                    specAndType = "Dầu tổng hợp 100% chuẩn SkyActiv SN/GF-5 • Lượng đổ chuẩn 3.6L (do không thay lọc)",
                    costVnd = 550_000
                ),
                ServiceCheckItem(
                    name = "Thay vòng đệm (long-đền) ốc xả nhớt đáy các-te",
                    specAndType = "Mã nhôm chính hãng 9956-41-400 (Bắt buộc thay mới để chống rỉ nhớt)",
                    costVnd = 15_000
                ),
                ServiceCheckItem(
                    name = "Thổi bụi Lọc gió động cơ & Lọc gió điều hòa",
                    specAndType = "Dùng súng hơi xịt sạch bụi, lắp lại dùng tiếp (Tuyệt đối chưa thay mới)",
                    costVnd = 0
                ),
                ServiceCheckItem(
                    name = "Kiểm tra gầm, phanh, châm nước rửa kính & Công Cấp 1",
                    specAndType = "Công bảo dưỡng định kỳ cấp 1 tại hãng",
                    costVnd = 230_000
                )
            ),
            skipItems = listOf(
                ServiceCheckItem(
                    name = "Phụ gia nano bảo vệ nhớt / Súc rửa động cơ (Engine Flush)",
                    specAndType = "Nhớt Mazda 0W-20 đã đủ phụ gia chuẩn Nhật, đổ thêm hóa chất ngoài làm loãng nhớt.",
                    costVnd = 350_000
                ),
                ServiceCheckItem(
                    name = "Dung dịch vệ sinh kim phun / buồng đốt sớm",
                    specAndType = "Xe mới 6.000 km kim phun còn sạch bóng, để mốc 10.000 km mới cần!",
                    costVnd = 280_000
                ),
                ServiceCheckItem(
                    name = "Xịt khử khuẩn / vệ sinh dàn lạnh điều hòa",
                    specAndType = "Dàn lạnh xe mới hoàn toàn sạch, chỉ cần xịt bụi lọc gió cabin là thơm mát.",
                    costVnd = 320_000
                )
            ),
            smartSavingTip = "Mốc 2 này chỉ cần thay đúng 3.6 Lít nhớt Mazda 0W-20 và 1 long-đền ốc xả (15k). Lọc nhớt theo xe còn rất tốt, để mốc sau (+10.000 km) mới thay theo đúng quy tắc 2 lần nhớt = 1 lần lọc!",
            advisorScript = "Anh làm bảo dưỡng cấp 1: chỉ thay dầu máy Mazda 0W-20, thay long-đền ốc xả, xịt bụi 2 lọc gió và kiểm tra gầm. Em gạch bỏ hết các chai phụ gia súc béc, nano nhớt và khử khuẩn dàn lạnh ra khỏi báo giá giúp anh nhé.",
            standardTotalVnd = 795_000,
            dealerInflatedVnd = 1_745_000
        ),

        ServiceMilestonePlan(
            stepIndex = 3,
            relativeKmFromFirst = 10000,
            standardOdoKm = 11000,
            badgeTitle = "Mốc 3: +10.000 km (11k)",
            title = "Mốc 3: +10.000 KM (ODO ~10.000 - 11.000 KM)",
            subtitle = "Bảo dưỡng Cấp 2 (Trung bình nhỏ) • Thay nhớt + Lọc nhớt + Đảo lốp",
            mustDoItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu động cơ Mazda 0W-20 (3.8 Lít)",
                    specAndType = "Có thay lọc nhớt nên lượng đổ chuẩn là 3.8 Lít",
                    costVnd = 580_000
                ),
                ServiceCheckItem(
                    name = "Thay Lọc nhớt động cơ chính hãng Mazda",
                    specAndType = "Mã phụ tùng chuẩn: PE01-14-302A / PE01-14-302B (Kèm long-đền ốc xả 15k)",
                    costVnd = 180_000
                ),
                ServiceCheckItem(
                    name = "Đảo lốp 4 bánh & Cân bằng động kẹp chì",
                    specAndType = "Giúp 4 vỏ xe mòn đều, không bị ồn gợn sóng",
                    costVnd = 150_000
                ),
                ServiceCheckItem(
                    name = "Đổ 1 chai vệ sinh kim phun GDI (Do chạy xăng RON 95-III)",
                    specAndType = "Loại chuẩn: Mazda Deposit Cleaner hoặc Chevron Techron Concentrate Plus (đổ vào bình xăng)",
                    costVnd = 200_000
                ),
                ServiceCheckItem(
                    name = "Vệ sinh thổi bụi 2 lọc gió & Công bảo dưỡng Cấp 2",
                    specAndType = "Kiểm tra phanh, gầm, hệ thống treo",
                    costVnd = 300_000
                )
            ),
            skipItems = listOf(
                ServiceCheckItem(
                    name = "Cân chỉnh góc đặt bánh xe điện tử 3D (Chỉnh độ chụm)",
                    specAndType = "Nếu xe đi đường thẳng buông nhẹ vô lăng vẫn chạy thẳng, lốp không mòn lệch thì tuyệt đối không cần làm!",
                    costVnd = 450_000
                ),
                ServiceCheckItem(
                    name = "Vệ sinh họng hút bướm ga & cảm biến MAF",
                    specAndType = "10.000 km bướm ga điện tử SkyActiv chưa bẩn, tháo sớm dễ báo lỗi tua máy.",
                    costVnd = 380_000
                ),
                ServiceCheckItem(
                    name = "Combo 3 chai phụ gia (Súc các-te + Nano nhớt + Octan)",
                    specAndType = "Chỉ cần duy nhất 1 chai Deposit Cleaner đổ bình xăng là đủ chuẩn Mazda.",
                    costVnd = 650_000
                )
            ),
            smartSavingTip = "Không làm gói 'Cân chỉnh thước lái 3D 450k' nếu xe đang chạy thẳng bình thường. Chai vệ sinh kim phun Mazda Deposit Cleaner / Techron bạn có thể mua về tự đổ vào bình xăng lúc chuẩn bị đổ đầy bình.",
            advisorScript = "Em lên phiếu cho anh: thay nhớt 0W-20, thay lọc nhớt chính hãng, đảo lốp và xịt bụi lọc gió. Không làm cân chỉnh góc đặt bánh xe 3D, không vệ sinh bướm ga và bỏ các chai phụ gia súc máy giúp anh.",
            standardTotalVnd = 1_410_000,
            dealerInflatedVnd = 2_890_000
        ),

        ServiceMilestonePlan(
            stepIndex = 4,
            relativeKmFromFirst = 15000,
            standardOdoKm = 16000,
            badgeTitle = "Mốc 4: +15.000 km (16k)",
            title = "Mốc 4: +15.000 KM (ODO ~15.000 - 16.000 KM)",
            subtitle = "Bảo dưỡng Cấp 1 (Nhỏ) • Chỉ thay nhớt 3.6L, KHÔNG thay lọc nhớt",
            mustDoItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu động cơ Mazda Genuine Oil 0W-20 (3.6 Lít)",
                    specAndType = "Mốc lẻ không thay lọc nhớt -> chỉ tốn 3.6 Lít nhớt",
                    costVnd = 550_000
                ),
                ServiceCheckItem(
                    name = "Thay vòng đệm (long-đền) ốc xả nhớt (9956-41-400)",
                    specAndType = "Long-đền nhôm chính hãng",
                    costVnd = 15_000
                ),
                ServiceCheckItem(
                    name = "Xịt bụi Lọc gió động cơ & Lọc gió điều hòa + Công Cấp 1",
                    specAndType = "Kiểm tra gầm, phanh, nước làm mát, nước rửa kính",
                    costVnd = 230_000
                )
            ),
            skipItems = listOf(
                ServiceCheckItem(
                    name = "Thay lọc nhớt sớm ở mốc 15.000 km",
                    specAndType = "Mới thay lọc nhớt ở mốc trước, chạy thêm 5.000 km nữa mới đến hạn thay!",
                    costVnd = 165_000
                ),
                ServiceCheckItem(
                    name = "Thay lọc gió động cơ & điều hòa sớm",
                    specAndType = "Đợi đúng mốc tiếp theo (+20.000 km) mới thay đồng bộ.",
                    costVnd = 450_000
                ),
                ServiceCheckItem(
                    name = "Vệ sinh dàn lạnh nội soi / Khử mùi",
                    specAndType = "Không cần thiết khi điều hòa vẫn mát sâu và không hôi.",
                    costVnd = 500_000
                )
            ),
            smartSavingTip = "Các mốc lẻ (Mốc 2, Mốc 4, Mốc 6, Mốc 8) cực kỳ nhẹ nhàng: vào hãng chỉ thay đúng nhớt + long-đền ốc xả, tổng hóa đơn dưới 800k và chỉ mất 35 phút!",
            advisorScript = "Hôm nay mốc bảo dưỡng nhỏ anh chỉ thay dầu máy 0W-20 và long-đền ốc xả, xịt bụi lọc gió thôi nhé. Lọc nhớt anh mới thay kỳ trước rồi nên kỳ này không thay, không lấy thêm phụ gia.",
            standardTotalVnd = 795_000,
            dealerInflatedVnd = 1_910_000
        ),

        ServiceMilestonePlan(
            stepIndex = 5,
            relativeKmFromFirst = 20000,
            standardOdoKm = 21000,
            badgeTitle = "Mốc 5: +20.000 km (21k)",
            title = "Mốc 5: +20.000 KM (ODO ~20.000 - 21.000 KM)",
            subtitle = "Bảo dưỡng Cấp 3 (Trung bình lớn - Tròn 1 năm) • Thay 2 Lọc gió & Bảo dưỡng phanh",
            mustDoItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu động cơ Mazda 0W-20 (3.8L) + Lọc nhớt PE01-14-302A",
                    specAndType = "Combo nhớt mới + lọc nhớt chính hãng + long-đền",
                    costVnd = 760_000
                ),
                ServiceCheckItem(
                    name = "Thay mới Lọc gió động cơ & Lọc gió điều hòa (Cabin)",
                    specAndType = "Sau 20.000 km bụi đường miền Tây cần thay mới để động cơ hút gió thoáng và máy lạnh sạch",
                    costVnd = 420_000
                ),
                ServiceCheckItem(
                    name = "Bảo dưỡng hệ thống phanh 4 bánh & Đảo lốp",
                    specAndType = "Tháo vệ sinh bụi bố thắng, tra mỡ chịu nhiệt Silicone vào ắc trượt cùm phanh (Rất quan trọng mùa mưa miền Tây)",
                    costVnd = 350_000
                ),
                ServiceCheckItem(
                    name = "Đổ 1 chai vệ sinh kim phun GDI (Deposit Cleaner / Techron)",
                    specAndType = "Chu kỳ mỗi 10.000 km cho xe chạy xăng RON 95-III",
                    costVnd = 200_000
                ),
                ServiceCheckItem(
                    name = "Tiền công bảo dưỡng Cấp 3",
                    specAndType = "Kiểm tra toàn diện gầm, phanh, thước lái, ắc quy",
                    costVnd = 350_000
                )
            ),
            skipItems = listOf(
                ServiceCheckItem(
                    name = "Thay nước làm mát động cơ sớm ở 20.000 km",
                    specAndType = "Nước mát zin Mazda Super Coolant FL22 có tuổi thọ tới 100.000 km (5 năm)! Tuyệt đối không xả bỏ!",
                    costVnd = 480_000
                ),
                ServiceCheckItem(
                    name = "Vệ sinh buồng đốt bằng khí Hydro / Súc béc treo bình",
                    specAndType = "Không có trong tài liệu bảo dưỡng Mazda Nhật Bản, rất tốn kém.",
                    costVnd = 650_000
                )
            ),
            smartSavingTip = "Mẹo tiết kiệm thêm 200k ở mốc này: Lọc gió điều hòa (Cabin) bạn có thể tự đặt mua loại than hoạt tính Denso/Masuma chuẩn Mazda 2 (~130k) tự mở hộc để đồ bên phụ thay trong 1 phút, không cần mua lọc giấy đắt tiền trong hãng!",
            advisorScript = "Phiếu 20.000km em làm cho anh: Thay nhớt, lọc nhớt, thay lọc gió động cơ, đảo lốp và bảo dưỡng vệ sinh phanh 4 bánh. Nước làm mát FL22 100.000km mới thay nên giữ nguyên, không làm vệ sinh buồng đốt Hydro nhé.",
            standardTotalVnd = 2_080_000,
            dealerInflatedVnd = 3_550_000
        ),

        ServiceMilestonePlan(
            stepIndex = 6,
            relativeKmFromFirst = 40000,
            standardOdoKm = 40000,
            badgeTitle = "Mốc Lớn: 40.000 km",
            title = "Đại Kỳ 40.000 KM (Bảo dưỡng Cấp 4 - Sau 2 năm)",
            subtitle = "Thay dầu phanh DOT 4 + Thay lọc xăng • KHÔNG thay dầu hộp số 6AT sớm!",
            mustDoItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu động cơ 0W-20 (3.8L) + Lọc nhớt + 2 Lọc gió",
                    specAndType = "Combo định kỳ",
                    costVnd = 1_180_000
                ),
                ServiceCheckItem(
                    name = "Thay toàn bộ Dầu phanh (Dầu thắng thủy lực DOT 4)",
                    specAndType = "Sau 2 năm dầu phanh hút ẩm làm giảm nhiệt độ sôi, bắt buộc xả gió thay mới toàn bộ (~1 Lít)",
                    costVnd = 350_000
                ),
                ServiceCheckItem(
                    name = "Thay Lọc nhiên liệu (Lọc xăng tinh đặt trong bình)",
                    specAndType = "Bắt buộc ở 40.000 km khi chạy xăng RON 95-III để bảo vệ bơm cao áp GDI",
                    costVnd = 650_000
                ),
                ServiceCheckItem(
                    name = "Vệ sinh 4 Bugi Iridium, Cảm biến Oxy & Bảo dưỡng phanh 4 bánh",
                    specAndType = "Xịt dung dịch chuyên dụng làm sạch muội carbon bám đầu bugi và cảm biến O2",
                    costVnd = 500_000
                )
            ),
            skipItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu hộp số tự động 6AT sớm ở 40.000 km",
                    specAndType = "Hộp số SkyActiv-Drive 6AT dùng dầu Mazda ATF-FZ siêu bền (chuẩn 80.000 - 100.000 km mới thay). Thay ở 40k km là lãng phí gần 2 triệu!",
                    costVnd = 1_850_000
                ),
                ServiceCheckItem(
                    name = "Thay mới 4 Bugi Iridium sớm ở 40.000 km",
                    specAndType = "Bugi Iridium theo xe rất bền, ở 40k km chỉ cần tháo ra vệ sinh sạch muội than là dùng tốt tới 60k - 80k km!",
                    costVnd = 1_200_000
                )
            ),
            smartSavingTip = "Mốc 40.000 km là mốc hãng hay vẽ nhiều nhất (báo giá lên tới 7 - 8 triệu). Bạn chỉ cần nhớ: Thay Dầu phanh DOT 4 + Lọc xăng, nhưng TỪ CHỐI thay Dầu hộp số 6AT và TỪ CHỐI thay Bugi mới là tiết kiệm ngay hơn 3 triệu đồng!",
            advisorScript = "Mốc 40.000km anh làm đúng sổ bảo hành: Thay nhớt, lọc nhớt, 2 lọc gió, thay dầu phanh DOT 4 và thay lọc xăng. Bugi Iridium chỉ tháo vệ sinh chứ chưa thay mới, và dầu hộp số ATF-FZ để 80k-100k km anh mới thay nhé.",
            standardTotalVnd = 2_680_000,
            dealerInflatedVnd = 6_850_000
        ),

        ServiceMilestonePlan(
            stepIndex = 7,
            relativeKmFromFirst = 50000,
            standardOdoKm = 50000,
            badgeTitle = "Mốc 50.000 km (Free Công)",
            title = "Mốc 50.000 KM (Bảo dưỡng Cấp 2 - TẶNG 100% TIỀN CÔNG)",
            subtitle = "Áp dụng ưu đãi Miễn phí công bảo dưỡng mốc 50.000 km của Thaco",
            isFreeLabor = true,
            freeLaborNote = "MIỄN PHÍ 100% TIỀN CÔNG BẢO DƯỠNG MỐC 50.000 KM!",
            mustDoItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu động cơ Mazda 0W-20 (3.8L) + Lọc nhớt PE01-14-302A",
                    specAndType = "Combo nhớt + lọc nhớt chính hãng",
                    costVnd = 760_000
                ),
                ServiceCheckItem(
                    name = "Đảo lốp 4 bánh & Kiểm tra độ dày má phanh",
                    specAndType = "Nếu má phanh còn dày > 4mm thì chưa cần thay",
                    costVnd = 150_000
                ),
                ServiceCheckItem(
                    name = "Tiền công bảo dưỡng Cấp 2: MIỄN PHÍ 100%",
                    specAndType = "Nhắc cố vấn dịch vụ áp dụng ưu đãi miễn phí công mốc 50.000 km",
                    costVnd = 0
                )
            ),
            skipItems = listOf(
                ServiceCheckItem(
                    name = "Quên trừ tiền công bảo dưỡng + Vẽ combo phụ gia 4 chai",
                    specAndType = "Kiểm tra kỹ hóa đơn mục Tiền công phải bằng 0đ.",
                    costVnd = 1_350_000
                )
            ),
            smartSavingTip = "Nhớ nhắc cố vấn dịch vụ ngay từ lúc mở phiếu là xe bạn có chế độ Miễn phí tiền công ở mốc 50.000 km!",
            advisorScript = "Mốc 50.000km này xe anh có chế độ miễn phí 100% tiền công bảo dưỡng của Thaco, em áp vào phiếu cho anh nhé. Anh chỉ thay nhớt, lọc nhớt và đảo lốp thôi.",
            standardTotalVnd = 910_000,
            dealerInflatedVnd = 2_260_000
        ),

        ServiceMilestonePlan(
            stepIndex = 8,
            relativeKmFromFirst = 60000,
            standardOdoKm = 60000,
            badgeTitle = "Mốc 60.000 km (Bugi)",
            title = "Mốc 60.000 KM (Bảo dưỡng Cấp 3 & Thay Bugi Iridium)",
            subtitle = "Thay bộ 4 Bugi Iridium chính hãng + 2 Lọc gió + Bảo dưỡng phanh",
            mustDoItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu động cơ 0W-20 (3.8L) + Lọc nhớt + 2 Lọc gió",
                    specAndType = "Combo bảo dưỡng động cơ",
                    costVnd = 1_180_000
                ),
                ServiceCheckItem(
                    name = "Thay bộ 4 Bugi đánh lửa Iridium (NGK / Denso PE5R-18-110)",
                    specAndType = "Sau 60.000 km chạy xăng RON 95-III nên thay mới để đánh lửa mạnh và tiết kiệm xăng",
                    costVnd = 1_100_000
                ),
                ServiceCheckItem(
                    name = "Bảo dưỡng cùm phanh 4 bánh, đảo lốp & Công Cấp 3",
                    specAndType = "Kiểm tra dây curoa tổng và điện áp ắc quy",
                    costVnd = 700_000
                )
            ),
            skipItems = listOf(
                ServiceCheckItem(
                    name = "Thay cụm bướm ga / Kim phun khi xe không báo lỗi",
                    specAndType = "Chỉ cần vệ sinh nhẹ, không thay phụ tùng đắt tiền khi xe vận hành êm.",
                    costVnd = 2_200_000
                )
            ),
            smartSavingTip = "Khi thay 4 bugi Iridium ở mốc 60.000 km, bạn hãy giữ lại bộ bugi cũ vẫn còn dùng tốt để làm bộ sơ-cua dự phòng trong cốp xe!",
            advisorScript = "Mốc 60.000km anh thay nhớt, lọc nhớt, 2 lọc gió, thay 4 bugi Iridium và bảo dưỡng phanh 4 bánh. Không làm các gói súc rửa phụ gia nhé.",
            standardTotalVnd = 2_980_000,
            dealerInflatedVnd = 5_180_000
        ),

        ServiceMilestonePlan(
            stepIndex = 9,
            relativeKmFromFirst = 100000,
            standardOdoKm = 100000,
            badgeTitle = "Mốc 100.000 km (Đại tu)",
            title = "Đại Kỳ 100.000 KM (Thay Dầu Hộp Số 6AT & Nước Mát FL22)",
            subtitle = "Mốc lớn nhất 5 năm • Được MIỄN PHÍ 100% TIỀN CÔNG tại Thaco",
            isFreeLabor = true,
            freeLaborNote = "MIỄN PHÍ 100% TIỀN CÔNG BẢO DƯỠNG LỚN MỐC 100.000 KM!",
            mustDoItems = listOf(
                ServiceCheckItem(
                    name = "Thay dầu hộp số tự động 6AT chuyên dụng Mazda ATF-FZ",
                    specAndType = "Đúng chuẩn 100.000 km mới thay để bảo vệ biến mô và hộp số SkyActiv-Drive",
                    costVnd = 1_650_000
                ),
                ServiceCheckItem(
                    name = "Thay nước làm mát động cơ Mazda Super Coolant FL22",
                    specAndType = "Xả toàn bộ nước cũ sau 5 năm / 100.000 km, thay mới dung dịch FL22 chính hãng",
                    costVnd = 480_000
                ),
                ServiceCheckItem(
                    name = "Thay Nhớt 0W-20 + Lọc nhớt + 2 Lọc gió + Dầu phanh DOT 4",
                    specAndType = "Làm mới toàn bộ chất lỏng trên xe",
                    costVnd = 1_530_000
                ),
                ServiceCheckItem(
                    name = "Tiền công Đại kỳ 100.000 KM: MIỄN PHÍ 100%",
                    specAndType = "Áp dụng chế độ miễn phí công mốc 100.000 km của Thaco",
                    costVnd = 0
                )
            ),
            skipItems = listOf(
                ServiceCheckItem(
                    name = "Tính tiền công đại tu (600k - 800k) + Phụ gia súc hộp số",
                    specAndType = "Không dùng hóa chất súc rửa bên trong hộp số tự động 6AT, chỉ xả và châm đúng dầu chuẩn ATF-FZ!",
                    costVnd = 1_800_000
                )
            ),
            smartSavingTip = "Hộp số tự động Mazda 2 bắt buộc phải dùng đúng mã dầu Mazda ATF-FZ (màu xanh), tuyệt đối không thay dầu hộp số đa năng bên ngoài!",
            advisorScript = "Mốc 100.000km anh áp dụng miễn phí 100% tiền công bảo dưỡng. Thay cho anh dầu hộp số chính hãng ATF-FZ, nước làm mát FL22, dầu phanh DOT 4, nhớt máy và các lọc theo chuẩn sổ bảo hành nhé.",
            standardTotalVnd = 3_660_000,
            dealerInflatedVnd = 6_900_000
        )
    )
}
