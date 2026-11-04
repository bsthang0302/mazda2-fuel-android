package com.mazda2care.fuellog.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mazda2care.fuellog.data.AppDatabase
import com.mazda2care.fuellog.data.FuelRepository
import com.mazda2care.fuellog.model.CarProfile
import com.mazda2care.fuellog.model.FuelPricePeriodEntity
import com.mazda2care.fuellog.model.FuelProduct
import com.mazda2care.fuellog.model.FuelRecordEntity
import com.mazda2care.fuellog.model.LiveFuelItemPrice
import com.mazda2care.fuellog.model.PriceZone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FuelViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repo = FuelRepository(application, db.fuelDao())

    val fuelRecords: StateFlow<List<FuelRecordEntity>> = repo.fuelRecordsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pricePeriods: StateFlow<List<FuelPricePeriodEntity>> = repo.fuelPricePeriodsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _profile = MutableStateFlow(repo.getCarProfile())
    val profile: StateFlow<CarProfile> = _profile.asStateFlow()

    private val _liveFuelItems = MutableStateFlow(repo.getCachedLiveFuelItems())
    val liveFuelItems: StateFlow<List<LiveFuelItemPrice>> = _liveFuelItems.asStateFlow()

    private val _isSyncingPrice = MutableStateFlow(false)
    val isSyncingPrice: StateFlow<Boolean> = _isSyncingPrice.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow(repo.getLastSyncNote())
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repo.ensureSeeded()
            _profile.value = repo.getCarProfile()
            refreshLiveFuelPrices()
        }
    }

    fun refreshLiveFuelPrices() {
        if (_isSyncingPrice.value) return
        viewModelScope.launch {
            _isSyncingPrice.value = true
            _syncStatusMessage.value = "Đang tự động cập nhật giá xăng Petrolimex..."
            val result = repo.syncLiveFuelPrices()
            _liveFuelItems.value = repo.getCachedLiveFuelItems()
            _profile.value = repo.getCarProfile()
            _syncStatusMessage.value = if (result.success) {
                "Đã tự động cập nhật • ${repo.getLastSyncNote()}"
            } else {
                "Đang dùng giá lưu trong máy • (${result.errorMessage ?: "Offline"})"
            }
            _isSyncingPrice.value = false
        }
    }

    fun selectFuelProductAndZone(product: FuelProduct, zone: PriceZone) {
        val current = _profile.value
        val matchedItem = _liveFuelItems.value.firstOrNull { it.product == product }
        val newPrice = if (matchedItem != null) {
            if (zone == PriceZone.ZONE_1) matchedItem.priceZone1 else matchedItem.priceZone2
        } else {
            current.currentFuelPrice
        }

        val updated = current.copy(
            selectedFuelCode = product.code,
            fuelType = product.displayName,
            priceZoneCode = zone.code,
            currentFuelPrice = if (newPrice > 0) newPrice else current.currentFuelPrice
        )
        repo.saveCarProfile(updated)
        _profile.value = updated
    }

    fun updateCarProfile(updated: CarProfile) {
        repo.saveCarProfile(updated)
        _profile.value = updated
    }

    fun addOrUpdateFuelRecord(
        id: String? = null,
        epochMillis: Long,
        odo: Int,
        totalCost: Double,
        pricePerLiter: Double,
        stationName: String,
        isFullTank: Boolean
    ) {
        viewModelScope.launch {
            repo.addOrUpdateFuelRecord(
                id = id,
                epochMillis = epochMillis,
                odo = odo,
                totalCost = totalCost,
                pricePerLiter = pricePerLiter,
                stationName = stationName,
                isFullTank = isFullTank
            )
            _profile.value = repo.getCarProfile()
        }
    }

    fun deleteFuelRecord(record: FuelRecordEntity) {
        viewModelScope.launch {
            repo.deleteFuelRecord(record)
        }
    }

    fun updateManualPrice(priceZone1: Double, priceZone2: Double, note: String) {
        viewModelScope.launch {
            repo.addManualPricePeriod(priceZone1, priceZone2, note)
            _profile.value = repo.getCarProfile()
        }
    }

    fun getMatchedPeriodForDate(epochMillis: Long): FuelPricePeriodEntity? {
        val sorted = pricePeriods.value.sortedByDescending { it.epochMillis }
        return sorted.firstOrNull { it.epochMillis <= epochMillis } ?: sorted.lastOrNull()
    }

    fun getPriceForDate(epochMillis: Long): Double {
        val period = getMatchedPeriodForDate(epochMillis)
        val zone = _profile.value.selectedPriceZone
        if (period != null) {
            return if (zone == PriceZone.ZONE_1) period.priceZone1 else period.priceZone2
        }
        return _profile.value.currentFuelPrice
    }

    fun exportBackupJson(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val json = repo.exportToJsonString()
            onReady(json)
        }
    }

    fun importBackupJson(jsonStr: String, onDone: (Result<Int>) -> Unit) {
        viewModelScope.launch {
            try {
                val count = repo.importFromJsonString(jsonStr, replaceAll = true)
                _profile.value = repo.getCarProfile()
                onDone(Result.success(count))
            } catch (e: Exception) {
                onDone(Result.failure(e))
            }
        }
    }
}
