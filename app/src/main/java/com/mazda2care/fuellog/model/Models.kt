package com.mazda2care.fuellog.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

enum class FuelProduct(
    val code: String,
    val displayName: String,
    val shortName: String,
    val matchKeywords: List<String>
) {
    RON95_III(
        code = "RON95_III",
        displayName = "Xăng RON 95-III (Mức 3)",
        shortName = "RON 95-III",
        matchKeywords = listOf("95-iii", "95 iii", "95 mức 3", "95-mức 3", "ron 95 mức 3")
    ),
    RON95_V(
        code = "RON95_V",
        displayName = "Xăng RON 95-V (Mức 5)",
        shortName = "RON 95-V",
        matchKeywords = listOf("95-v", "95 v", "95 mức 5", "95-mức 5", "ron 95 mức 5")
    ),
    E5_RON92(
        code = "E5_RON92",
        displayName = "Xăng E5 RON 92-II",
        shortName = "E5 RON 92",
        matchKeywords = listOf("e5 ron 92", "ron 92", "e5")
    ),
    DO_005S(
        code = "DO_005S",
        displayName = "Dầu DO 0,05S-II",
        shortName = "Dầu DO 0,05S",
        matchKeywords = listOf("0,05s", "0.05s", "do 0,05")
    ),
    DO_0001S(
        code = "DO_0001S",
        displayName = "Dầu DO 0,001S-V",
        shortName = "Dầu DO 0,001S",
        matchKeywords = listOf("0,001s", "0.001s", "do 0,001")
    );

    companion object {
        fun fromCode(code: String): FuelProduct =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: RON95_III
    }
}

enum class PriceZone(val code: Int, val label: String, val shortLabel: String) {
    ZONE_1(1, "Vùng 1 (TP lớn / Gần cảng)", "Vùng 1"),
    ZONE_2(2, "Vùng 2 (Đồng Tháp / Tỉnh xa cảng)", "Vùng 2");

    companion object {
        fun fromCode(code: Int): PriceZone = if (code == 1) ZONE_1 else ZONE_2
    }
}

@Entity(tableName = "fuel_records")
data class FuelRecordEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString().uppercase(),
    val dateIso: String,
    val epochMillis: Long,
    val odo: Int,
    val liters: Double,
    val totalCost: Double,
    val stationName: String,
    val pricePerLiter: Double,
    val tripKm: Int? = null,
    val fuelEfficiency: Double? = null,
    val isFullTank: Boolean = false
)

@Entity(tableName = "fuel_price_periods")
data class FuelPricePeriodEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString().uppercase(),
    val effectiveDateIso: String,
    val epochMillis: Long,
    val priceZone1: Double,
    val priceZone2: Double,
    val note: String,
    val isAutoFetched: Boolean = false,
    val allFuelsJson: String = "{}"
)

data class LiveFuelItemPrice(
    val product: FuelProduct,
    val rawName: String,
    val priceZone1: Double,
    val priceZone2: Double
)

data class LiveFuelFetchResult(
    val success: Boolean,
    val updatedTitle: String,
    val effectiveEpochMillis: Long,
    val items: List<LiveFuelItemPrice>,
    val sourceUrl: String,
    val errorMessage: String? = null
)

data class CarProfile(
    val carName: String = "Mazda 2 Sedan 1.5L AT",
    val plateNumber: String = "66A - 344.06",
    val origin: String = "Nhập khẩu nguyên chiếc Thái Lan",
    val engine: String = "1.5L SkyActiv-G (110 hp, 144 Nm) + 6AT",
    val fuelType: String = "Xăng RON 95 Mức 3 (Euro 3)",
    val selectedFuelCode: String = FuelProduct.RON95_III.code,
    val priceZoneCode: Int = PriceZone.ZONE_2.code,
    val tankCapacityLiters: Double = 38.0,
    val standardConsumptionL100Km: Double = 5.3,
    val currentFuelPrice: Double = 25400.0,
    val currentOdo: Int = 1200,
    val dailyKm: Int = 55,
    val favoriteStations: List<String> = listOf(
        "Petrolimex CHXD 101 Cao Lãnh",
        "Petrolimex CHXD 102 Cao Lãnh",
        "Petrolimex CHXD 111 Cao Lãnh",
        "Cây xăng khác"
    )
) {
    val selectedFuelProduct: FuelProduct
        get() = FuelProduct.fromCode(selectedFuelCode)

    val selectedPriceZone: PriceZone
        get() = PriceZone.fromCode(priceZoneCode)
}

object FormatUtils {
    private val vnSymbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    private val currencyFormat = DecimalFormat("#,###", vnSymbols)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm")
    private val dateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    fun formatVnd(amount: Double): String {
        return "${currencyFormat.format(amount.toLong())} đ"
    }

    fun formatVndNumberOnly(amount: Double): String {
        return currencyFormat.format(amount.toLong())
    }

    fun formatLiters(liters: Double): String {
        return String.format(Locale.US, "%.2f Lít", liters)
    }

    fun formatL100Km(value: Double): String {
        return String.format(Locale.US, "%.2f L/100km", value)
    }

    fun formatDateTime(epochMillis: Long): String {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return ldt.format(dateTimeFormatter)
    }

    fun formatDateOnly(epochMillis: Long): String {
        val ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault())
        return ldt.format(dateOnlyFormatter)
    }

    fun parseIsoToMillis(iso: String): Long {
        return try {
            Instant.parse(iso).toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }

    fun millisToIso(epochMillis: Long): String {
        return Instant.ofEpochMilli(epochMillis).toString()
    }
}
