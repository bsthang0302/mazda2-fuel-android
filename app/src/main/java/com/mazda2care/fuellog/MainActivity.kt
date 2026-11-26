package com.mazda2care.fuellog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mazda2care.fuellog.ui.FuelViewModel
import com.mazda2care.fuellog.ui.screens.MainScreen
import com.mazda2care.fuellog.ui.theme.Mazda2FuelTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Mazda2FuelTheme {
                val fuelViewModel: FuelViewModel = viewModel()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Trình chọn nơi lưu file sao lưu JSON (CreateDocument) + Tự động cấp quyền ghi ngầm vĩnh viễn
                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        } catch (_: Exception) {
                        }
                        fuelViewModel.exportBackupJson { jsonContent ->
                            try {
                                contentResolver.openOutputStream(uri, "wt")?.use { out ->
                                    out.write(jsonContent.toByteArray(Charsets.UTF_8))
                                } ?: contentResolver.openOutputStream(uri)?.use { out ->
                                    out.write(jsonContent.toByteArray(Charsets.UTF_8))
                                }
                                fuelViewModel.registerLinkedBackupUri(uri.toString())
                                scope.launch {
                                    snackbarHostState.showSnackbar("✓ Đã lưu & bật Tự Động Sao Lưu Ngầm mỗi khi bạn ghi đổ xăng!")
                                }
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Lỗi khi lưu file: ${e.localizedMessage}")
                                }
                            }
                        }
                    }
                }

                // Trình chọn file JSON để nhập dữ liệu (OpenDocument) + Tự động liên kết làm file sao lưu ngầm
                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            try {
                                contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                )
                                fuelViewModel.registerLinkedBackupUri(uri.toString())
                            } catch (_: Exception) {
                            }
                            val rawJson = contentResolver.openInputStream(uri)
                                ?.bufferedReader(Charsets.UTF_8)
                                ?.use { it.readText() }
                            if (!rawJson.isNullOrBlank()) {
                                fuelViewModel.importBackupJson(rawJson) { res ->
                                    scope.launch {
                                        res.fold(
                                            onSuccess = { count ->
                                                snackbarHostState.showSnackbar("✓ Đã khôi phục $count lần đổ xăng & bật Tự Động Sao Lưu Ngầm!")
                                            },
                                            onFailure = { err ->
                                                snackbarHostState.showSnackbar("File JSON không hợp lệ: ${err.localizedMessage}")
                                            }
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            scope.launch {
                                snackbarHostState.showSnackbar("Không đọc được file: ${e.localizedMessage}")
                            }
                        }
                    }
                }

                MainScreen(
                    viewModel = fuelViewModel,
                    onExportJsonClick = {
                        exportLauncher.launch("mazda2_car_data.json")
                    },
                    onImportJsonClick = {
                        importLauncher.launch(arrayOf("application/json", "*/*"))
                    },
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}
