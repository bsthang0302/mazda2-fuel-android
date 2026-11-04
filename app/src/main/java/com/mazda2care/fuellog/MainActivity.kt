package com.mazda2care.fuellog

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

                // Trình chọn nơi lưu file sao lưu JSON (CreateDocument)
                val exportLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri: Uri? ->
                    if (uri != null) {
                        fuelViewModel.exportBackupJson { jsonContent ->
                            try {
                                contentResolver.openOutputStream(uri)?.use { out ->
                                    out.write(jsonContent.toByteArray(Charsets.UTF_8))
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar("Đã xuất file sao lưu mazda2_car_data.json thành công!")
                                }
                            } catch (e: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Lỗi khi lưu file: ${e.localizedMessage}")
                                }
                            }
                        }
                    }
                }

                // Trình chọn file JSON để nhập dữ liệu (OpenDocument)
                val importLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            val rawJson = contentResolver.openInputStream(uri)
                                ?.bufferedReader(Charsets.UTF_8)
                                ?.use { it.readText() }
                            if (!rawJson.isNullOrBlank()) {
                                fuelViewModel.importBackupJson(rawJson) { res ->
                                    scope.launch {
                                        res.fold(
                                            onSuccess = { count ->
                                                snackbarHostState.showSnackbar("Đã nhập thành công $count bản ghi đổ xăng!")
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
