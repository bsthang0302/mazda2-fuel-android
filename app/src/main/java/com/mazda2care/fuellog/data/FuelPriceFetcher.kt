package com.mazda2care.fuellog.data

import com.mazda2care.fuellog.model.FuelProduct
import com.mazda2care.fuellog.model.LiveFuelFetchResult
import com.mazda2care.fuellog.model.LiveFuelItemPrice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.math.roundToInt

object FuelPriceFetcher {

    private const val WEBGIA_PETROLIMEX_URL = "https://webgia.com/gia-xang-dau/petrolimex/"

    suspend fun fetchLatestPetrolimexPrices(): LiveFuelFetchResult = withContext(Dispatchers.IO) {
        try {
            val html = downloadHtml(WEBGIA_PETROLIMEX_URL)
            val parsedItems = parseWebGiaPetrolimexHtml(html)
            if (parsedItems.isNotEmpty()) {
                val updatedNote = extractUpdateTimestampNote(html)
                return@withContext LiveFuelFetchResult(
                    success = true,
                    updatedTitle = updatedNote,
                    effectiveEpochMillis = System.currentTimeMillis(),
                    items = parsedItems,
                    sourceUrl = WEBGIA_PETROLIMEX_URL
                )
            }
            LiveFuelFetchResult(
                success = false,
                updatedTitle = "Không tìm thấy bảng giá trong trang nguồn",
                effectiveEpochMillis = System.currentTimeMillis(),
                items = emptyList(),
                sourceUrl = WEBGIA_PETROLIMEX_URL,
                errorMessage = "Cấu trúc bảng HTML thay đổi hoặc rỗng"
            )
        } catch (e: Exception) {
            LiveFuelFetchResult(
                success = false,
                updatedTitle = "Không thể kết nối máy chủ giá xăng",
                effectiveEpochMillis = System.currentTimeMillis(),
                items = emptyList(),
                sourceUrl = WEBGIA_PETROLIMEX_URL,
                errorMessage = e.localizedMessage ?: "Lỗi mạng"
            )
        }
    }

    private fun downloadHtml(urlString: String): String {
        val url = URL(urlString)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
            )
            setRequestProperty("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code")
            }
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line).append('\n')
                }
                return sb.toString()
            }
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseWebGiaPetrolimexHtml(html: String): List<LiveFuelItemPrice> {
        val results = mutableMapOf<FuelProduct, LiveFuelItemPrice>()

        // Tìm tất cả các dòng <tr>...</tr> trong bảng giá
        val trRegex = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val tdRegex = Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

        for (trMatch in trRegex.findAll(html)) {
            val rowHtml = trMatch.groupValues[1]
            val cells = tdRegex.findAll(rowHtml)
                .map { cleanHtmlText(it.groupValues[1]) }
                .filter { it.isNotBlank() }
                .toList()

            if (cells.size >= 3) {
                val productName = cells[0]
                val price1 = parsePriceNumber(cells[1])
                val price2 = parsePriceNumber(cells[2])

                if (price1 != null && price1 > 10_000) {
                    val effectivePrice2 = if (price2 != null && price2 > 10_000) {
                        price2
                    } else {
                        // Quy tắc Vùng 2 Petrolimex (+2% làm tròn đến chục đồng)
                        ((price1 * 1.02) / 10.0).roundToInt() * 10.0
                    }

                    val matchedProduct = matchFuelProduct(productName)
                    if (matchedProduct != null && !results.containsKey(matchedProduct)) {
                        results[matchedProduct] = LiveFuelItemPrice(
                            product = matchedProduct,
                            rawName = productName,
                            priceZone1 = price1,
                            priceZone2 = effectivePrice2
                        )
                    }
                }
            }
        }

        return FuelProduct.entries.mapNotNull { results[it] }
    }

    private fun matchFuelProduct(rawName: String): FuelProduct? {
        val normalized = rawName.lowercase(Locale.ROOT)
            .replace("–", "-")
            .replace("—", "-")
        for (product in FuelProduct.entries) {
            if (product.matchKeywords.any { normalized.contains(it) }) {
                return product
            }
        }
        return null
    }

    private fun parsePriceNumber(raw: String): Double? {
        // Loại bỏ ký tự không phải số, giữ lại chữ số (ví dụ "25.400" hoặc "25,400" -> 25400)
        val digitsOnly = raw.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length in 4..6) {
            return digitsOnly.toDoubleOrNull()
        }
        return null
    }

    private fun extractUpdateTimestampNote(html: String): String {
        val timeRegex = Regex(
            "(Cập nhật[^<]{3,60}|Áp dụng từ[^<]{3,60})",
            RegexOption.IGNORE_CASE
        )
        val match = timeRegex.find(html)
        return if (match != null) {
            cleanHtmlText(match.groupValues[1])
        } else {
            "Bảng giá Petrolimex trực tuyến mới nhất"
        }
    }

    private fun cleanHtmlText(input: String): String {
        return input
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun encodeItemsToJson(items: List<LiveFuelItemPrice>): String {
        val root = JSONObject()
        for (item in items) {
            val obj = JSONObject().apply {
                put("rawName", item.rawName)
                put("zone1", item.priceZone1)
                put("zone2", item.priceZone2)
            }
            root.put(item.product.code, obj)
        }
        return root.toString()
    }

    fun decodeItemsFromJson(jsonStr: String): List<LiveFuelItemPrice> {
        if (jsonStr.isBlank() || jsonStr == "{}") return emptyList()
        return try {
            val root = JSONObject(jsonStr)
            val list = mutableListOf<LiveFuelItemPrice>()
            for (product in FuelProduct.entries) {
                if (root.has(product.code)) {
                    val obj = root.getJSONObject(product.code)
                    list.add(
                        LiveFuelItemPrice(
                            product = product,
                            rawName = obj.optString("rawName", product.displayName),
                            priceZone1 = obj.optDouble("zone1", 0.0),
                            priceZone2 = obj.optDouble("zone2", 0.0)
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
