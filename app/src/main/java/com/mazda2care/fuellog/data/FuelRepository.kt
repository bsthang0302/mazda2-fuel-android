package com.mazda2care.fuellog.data

import android.content.Context
import com.mazda2care.fuellog.model.CarProfile
import com.mazda2care.fuellog.model.FormatUtils
import com.mazda2care.fuellog.model.FuelPricePeriodEntity
import com.mazda2care.fuellog.model.FuelProduct
import com.mazda2care.fuellog.model.FuelRecordEntity
import com.mazda2care.fuellog.model.LiveFuelFetchResult
import com.mazda2care.fuellog.model.LiveFuelItemPrice
import com.mazda2care.fuellog.model.PriceZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class FuelRepository(private val context: Context, private val dao: FuelDao) {

    private val prefs = context.getSharedPreferences("mazda2_fuel_prefs", Context.MODE_PRIVATE)

    val fuelRecordsFlow: Flow<List<FuelRecordEntity>> = dao.observeFuelRecords()
    val fuelPricePeriodsFlow: Flow<List<FuelPricePeriodEntity>> = dao.observeFuelPricePeriods()

    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        val isSeeded = prefs.getBoolean("is_seeded_v1", false)
        if (isSeeded) return@withContext

        try {
            val rawJson = context.assets.open("mazda2_car_data.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            importFromJsonString(rawJson, replaceAll = true)
            prefs.edit().putBoolean("is_seeded_v1", true).apply()
        } catch (_: Exception) {
            prefs.edit().putBoolean("is_seeded_v1", true).apply()
        }
    }

    fun getCarProfile(): CarProfile {
        val stationsRaw = prefs.getString(
            "favorite_stations",
            "Petrolimex CHXD 101 Cao Lãnh|Petrolimex CHXD 102 Cao Lãnh|Petrolimex CHXD 111 Cao Lãnh|Cây xăng khác"
        ) ?: ""
        val stations = stationsRaw.split("|").map { it.trim() }.filter { it.isNotEmpty() }

        return CarProfile(
            carName = prefs.getString("car_name", "Mazda 2 Sedan 1.5L AT") ?: "Mazda 2 Sedan 1.5L AT",
            plateNumber = prefs.getString("plate_number", "66A - 344.06") ?: "66A - 344.06",
            origin = prefs.getString("origin", "Nhập khẩu nguyên chiếc Thái Lan") ?: "Nhập khẩu nguyên chiếc Thái Lan",
            engine = prefs.getString("engine", "1.5L SkyActiv-G (110 hp, 144 Nm) + 6AT") ?: "1.5L SkyActiv-G (110 hp, 144 Nm) + 6AT",
            fuelType = prefs.getString("fuel_type", "Xăng RON 95 Mức 3 (Euro 3)") ?: "Xăng RON 95 Mức 3 (Euro 3)",
            selectedFuelCode = prefs.getString("selected_fuel_code", FuelProduct.RON95_III.code) ?: FuelProduct.RON95_III.code,
            priceZoneCode = prefs.getInt("price_zone_code", PriceZone.ZONE_2.code),
            tankCapacityLiters = prefs.getFloat("tank_capacity", 38.0f).toDouble(),
            standardConsumptionL100Km = prefs.getFloat("std_consumption", 5.3f).toDouble(),
            currentFuelPrice = prefs.getFloat("current_fuel_price", 25400f).toDouble(),
            currentOdo = prefs.getInt("current_odo", 1200),
            dailyKm = prefs.getInt("daily_km", 55),
            favoriteStations = if (stations.isEmpty()) listOf("Petrolimex CHXD 101 Cao Lãnh", "Cây xăng khác") else stations
        )
    }

    fun saveCarProfile(profile: CarProfile) {
        prefs.edit()
            .putString("car_name", profile.carName)
            .putString("plate_number", profile.plateNumber)
            .putString("origin", profile.origin)
            .putString("engine", profile.engine)
            .putString("fuel_type", profile.selectedFuelProduct.displayName)
            .putString("selected_fuel_code", profile.selectedFuelCode)
            .putInt("price_zone_code", profile.priceZoneCode)
            .putFloat("tank_capacity", profile.tankCapacityLiters.toFloat())
            .putFloat("std_consumption", profile.standardConsumptionL100Km.toFloat())
            .putFloat("current_fuel_price", profile.currentFuelPrice.toFloat())
            .putInt("current_odo", profile.currentOdo)
            .putInt("daily_km", profile.dailyKm)
            .putString("favorite_stations", profile.favoriteStations.joinToString("|"))
            .apply()
    }

    fun getCachedLiveFuelItems(): List<LiveFuelItemPrice> {
        val raw = prefs.getString("cached_live_fuels_json", null)
        if (!raw.isNullOrBlank()) {
            val decoded = FuelPriceFetcher.decodeItemsFromJson(raw)
            if (decoded.isNotEmpty()) return decoded
        }
        // Mặc định bảng giá Petrolimex khi chưa có mạng lần đầu
        return listOf(
            LiveFuelItemPrice(FuelProduct.RON95_V, "Xăng RON 95-V", 25450.0, 25950.0),
            LiveFuelItemPrice(FuelProduct.RON95_III, "Xăng RON 95-III", 24910.0, 25400.0),
            LiveFuelItemPrice(FuelProduct.E5_RON92, "Xăng E5 RON 92-II", 23800.0, 24270.0),
            LiveFuelItemPrice(FuelProduct.DO_0001S, "Dầu DO 0,001S-V", 22900.0, 23350.0),
            LiveFuelItemPrice(FuelProduct.DO_005S, "Dầu DO 0,05S-II", 21900.0, 22330.0)
        )
    }

    fun getLastSyncNote(): String {
        return prefs.getString("last_sync_note", "Kỳ điều hành gần nhất • Đã lưu offline") ?: ""
    }

    fun getLastSyncTimeMillis(): Long {
        return prefs.getLong("last_sync_epoch", 0L)
    }

    suspend fun syncLiveFuelPrices(): LiveFuelFetchResult = withContext(Dispatchers.IO) {
        val result = FuelPriceFetcher.fetchLatestPetrolimexPrices()
        if (result.success && result.items.isNotEmpty()) {
            val encoded = FuelPriceFetcher.encodeItemsToJson(result.items)
            val noteText = "${result.updatedTitle} (${FormatUtils.formatDateTime(result.effectiveEpochMillis)})"
            prefs.edit()
                .putString("cached_live_fuels_json", encoded)
                .putString("last_sync_note", noteText)
                .putLong("last_sync_epoch", result.effectiveEpochMillis)
                .apply()

            // Cập nhật đơn giá theo loại xăng & vùng người dùng đang chọn
            val profile = getCarProfile()
            val selectedItem = result.items.firstOrNull { it.product == profile.selectedFuelProduct }
                ?: result.items.firstOrNull { it.product == FuelProduct.RON95_III }
                ?: result.items.first()

            val activePrice = if (profile.selectedPriceZone == PriceZone.ZONE_1) {
                selectedItem.priceZone1
            } else {
                selectedItem.priceZone2
            }

            if (activePrice > 0) {
                saveCarProfile(profile.copy(currentFuelPrice = activePrice))
            }

            // Kiểm tra xem kỳ điều hành mới nhất trong DB có khác giá không -> Tự động thêm kỳ giá mới
            val existingPeriods = dao.getAllFuelPricePeriods()
            val latestPeriod = existingPeriods.firstOrNull()
            val ron95_3 = result.items.firstOrNull { it.product == FuelProduct.RON95_III } ?: selectedItem

            val isDifferentPrice = latestPeriod == null ||
                kotlin.math.abs(latestPeriod.priceZone2 - ron95_3.priceZone2) >= 10.0 ||
                kotlin.math.abs(latestPeriod.priceZone1 - ron95_3.priceZone1) >= 10.0

            if (isDifferentPrice) {
                val deltaZone2 = if (latestPeriod != null) {
                    (ron95_3.priceZone2 - latestPeriod.priceZone2).toInt()
                } else 0
                val deltaStr = when {
                    deltaZone2 > 0 -> " (Tăng +${FormatUtils.formatVnd(deltaZone2.toDouble())}/L)"
                    deltaZone2 < 0 -> " (Giảm ${FormatUtils.formatVnd(deltaZone2.toDouble())}/L)"
                    else -> ""
                }

                val newPeriod = FuelPricePeriodEntity(
                    id = UUID.randomUUID().toString().uppercase(),
                    effectiveDateIso = FormatUtils.millisToIso(result.effectiveEpochMillis),
                    epochMillis = result.effectiveEpochMillis,
                    priceZone1 = ron95_3.priceZone1,
                    priceZone2 = ron95_3.priceZone2,
                    note = "Tự động cập nhật ${FormatUtils.formatDateOnly(result.effectiveEpochMillis)}$deltaStr",
                    isAutoFetched = true,
                    allFuelsJson = encoded
                )
                dao.insertFuelPricePeriod(newPeriod)
            }
        }
        result
    }

    suspend fun addOrUpdateFuelRecord(
        id: String? = null,
        epochMillis: Long,
        odo: Int,
        totalCost: Double,
        pricePerLiter: Double,
        stationName: String,
        isFullTank: Boolean
    ) = withContext(Dispatchers.IO) {
        val liters = if (pricePerLiter > 0) totalCost / pricePerLiter else 0.0
        val record = FuelRecordEntity(
            id = id ?: UUID.randomUUID().toString().uppercase(),
            dateIso = FormatUtils.millisToIso(epochMillis),
            epochMillis = epochMillis,
            odo = odo,
            liters = liters,
            totalCost = totalCost,
            stationName = stationName.ifBlank { "Petrolimex CHXD 101 Cao Lãnh" },
            pricePerLiter = pricePerLiter,
            isFullTank = isFullTank
        )
        dao.insertFuelRecord(record)
        recalculateTripsAndEfficiency()

        val profile = getCarProfile()
        if (odo > profile.currentOdo) {
            saveCarProfile(profile.copy(currentOdo = odo))
        }
    }

    suspend fun deleteFuelRecord(record: FuelRecordEntity) = withContext(Dispatchers.IO) {
        dao.deleteFuelRecord(record)
        recalculateTripsAndEfficiency()
    }

    suspend fun addManualPricePeriod(
        priceZone1: Double,
        priceZone2: Double,
        note: String
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val period = FuelPricePeriodEntity(
            id = UUID.randomUUID().toString().uppercase(),
            effectiveDateIso = FormatUtils.millisToIso(now),
            epochMillis = now,
            priceZone1 = priceZone1,
            priceZone2 = priceZone2,
            note = note.ifBlank { "Cập nhật thủ công (${FormatUtils.formatDateOnly(now)})" },
            isAutoFetched = false
        )
        dao.insertFuelPricePeriod(period)

        val profile = getCarProfile()
        val newActive = if (profile.selectedPriceZone == PriceZone.ZONE_1) priceZone1 else priceZone2
        saveCarProfile(profile.copy(currentFuelPrice = newActive))
    }

    private suspend fun recalculateTripsAndEfficiency() {
        val all = dao.getAllFuelRecords()
        if (all.isEmpty()) return

        // Sắp xếp tăng dần theo thời gian & ODO để tính quãng đường di chuyển giữa các lần đổ
        val chronological = all.sortedWith(compareBy<FuelRecordEntity> { it.epochMillis }.thenBy { it.odo })
        val updated = mutableListOf<FuelRecordEntity>()

        var lastDistinctOdo: Int? = null
        var accumulatedLitersAtSameOdo = 0.0

        for (item in chronological) {
            if (lastDistinctOdo == null) {
                lastDistinctOdo = item.odo
                accumulatedLitersAtSameOdo = 0.0
                updated.add(item.copy(tripKm = 0, fuelEfficiency = null))
            } else {
                val deltaKm = item.odo - lastDistinctOdo
                if (deltaKm > 0) {
                    val totalLitersForTrip = item.liters + accumulatedLitersAtSameOdo
                    val eff = (totalLitersForTrip / deltaKm.toDouble()) * 100.0
                    updated.add(
                        item.copy(
                            tripKm = deltaKm,
                            fuelEfficiency = eff
                        )
                    )
                    lastDistinctOdo = item.odo
                    accumulatedLitersAtSameOdo = 0.0
                } else {
                    accumulatedLitersAtSameOdo += item.liters
                    updated.add(item.copy(tripKm = 0, fuelEfficiency = null))
                }
            }
        }
        dao.insertAllFuelRecords(updated)
    }

    /**
     * Xuất toàn bộ dữ liệu ra định dạng JSON tương thích 100% với file `mazda2_car_data.json` trên máy Mac.
     */
    suspend fun exportToJsonString(): String = withContext(Dispatchers.IO) {
        val baseTemplateStr = prefs.getString("raw_mac_template_json", null) ?: try {
            context.assets.open("mazda2_car_data.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            "{}"
        }

        val root = try {
            JSONObject(baseTemplateStr)
        } catch (_: Exception) {
            JSONObject()
        }

        val profile = getCarProfile()
        val profileObj = root.optJSONObject("profile") ?: JSONObject()
        profileObj.put("carName", profile.carName)
        profileObj.put("plateNumber", profile.plateNumber)
        profileObj.put("origin", profile.origin)
        profileObj.put("engine", profile.engine)
        profileObj.put("fuelType", profile.fuelType)
        profileObj.put("currentFuelPrice", profile.currentFuelPrice)
        profileObj.put("currentOdo", profile.currentOdo)
        profileObj.put("dailyKm", profile.dailyKm)
        root.put("profile", profileObj)

        val records = dao.getAllFuelRecords()
        val recordsArr = JSONArray()
        for (r in records) {
            val obj = JSONObject().apply {
                put("id", r.id)
                put("date", r.dateIso)
                put("odo", r.odo)
                put("liters", r.liters)
                put("totalCost", r.totalCost)
                put("stationName", r.stationName)
                put("pricePerLiter", r.pricePerLiter)
                put("tripKm", r.tripKm ?: 0)
                if (r.fuelEfficiency != null && r.fuelEfficiency > 0) {
                    put("fuelEfficiency", r.fuelEfficiency)
                }
            }
            recordsArr.put(obj)
        }
        root.put("fuelRecords", recordsArr)

        val periods = dao.getAllFuelPricePeriods()
        val periodsArr = JSONArray()
        for (p in periods) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("effectiveDate", p.effectiveDateIso)
                put("priceZone1", p.priceZone1)
                put("priceZone2", p.priceZone2)
                put("note", p.note)
            }
            periodsArr.put(obj)
        }
        root.put("fuelPricePeriods", periodsArr)

        root.toString(2)
    }

    /**
     * Nhập dữ liệu từ file JSON (`mazda2_car_data.json` từ Mac hoặc file sao lưu Android).
     */
    suspend fun importFromJsonString(jsonStr: String, replaceAll: Boolean = true): Int = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonStr)
        prefs.edit().putString("raw_mac_template_json", jsonStr).apply()

        val profileObj = root.optJSONObject("profile")
        if (profileObj != null) {
            val current = getCarProfile()
            val updated = current.copy(
                carName = profileObj.optString("carName", current.carName),
                plateNumber = profileObj.optString("plateNumber", current.plateNumber),
                origin = profileObj.optString("origin", current.origin),
                engine = profileObj.optString("engine", current.engine),
                fuelType = profileObj.optString("fuelType", current.fuelType),
                currentFuelPrice = profileObj.optDouble("currentFuelPrice", current.currentFuelPrice),
                currentOdo = profileObj.optInt("currentOdo", current.currentOdo),
                dailyKm = profileObj.optInt("dailyKm", current.dailyKm)
            )
            saveCarProfile(updated)
        }

        val periodsArr = root.optJSONArray("fuelPricePeriods")
        if (periodsArr != null) {
            if (replaceAll) dao.clearAllFuelPricePeriods()
            val periods = mutableListOf<FuelPricePeriodEntity>()
            for (i in 0 until periodsArr.length()) {
                val obj = periodsArr.getJSONObject(i)
                val iso = obj.optString("effectiveDate", FormatUtils.millisToIso(System.currentTimeMillis()))
                periods.add(
                    FuelPricePeriodEntity(
                        id = obj.optString("id", UUID.randomUUID().toString().uppercase()),
                        effectiveDateIso = iso,
                        epochMillis = FormatUtils.parseIsoToMillis(iso),
                        priceZone1 = obj.optDouble("priceZone1", 24910.0),
                        priceZone2 = obj.optDouble("priceZone2", 25400.0),
                        note = obj.optString("note", "Kỳ điều hành"),
                        isAutoFetched = false
                    )
                )
            }
            dao.insertAllFuelPricePeriods(periods)
        }

        var importedCount = 0
        val recordsArr = root.optJSONArray("fuelRecords")
        if (recordsArr != null) {
            if (replaceAll) dao.clearAllFuelRecords()
            val records = mutableListOf<FuelRecordEntity>()
            for (i in 0 until recordsArr.length()) {
                val obj = recordsArr.getJSONObject(i)
                val iso = obj.optString("date", FormatUtils.millisToIso(System.currentTimeMillis()))
                val eff = if (obj.has("fuelEfficiency") && !obj.isNull("fuelEfficiency")) {
                    obj.optDouble("fuelEfficiency")
                } else null
                records.add(
                    FuelRecordEntity(
                        id = obj.optString("id", UUID.randomUUID().toString().uppercase()),
                        dateIso = iso,
                        epochMillis = FormatUtils.parseIsoToMillis(iso),
                        odo = obj.optInt("odo", 0),
                        liters = obj.optDouble("liters", 0.0),
                        totalCost = obj.optDouble("totalCost", 0.0),
                        stationName = obj.optString("stationName", "Cây xăng khác"),
                        pricePerLiter = obj.optDouble("pricePerLiter", 25400.0),
                        tripKm = if (obj.has("tripKm")) obj.optInt("tripKm") else null,
                        fuelEfficiency = eff
                    )
                )
            }
            dao.insertAllFuelRecords(records)
            recalculateTripsAndEfficiency()
            importedCount = records.size
        }

        importedCount
    }
}
