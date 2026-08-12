package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PosViewModel
import com.example.util.PdfReportHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LaporanScreen(viewModel: PosViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()

    if (currentUser?.role == "kasir") {
        KasirShiftReportSection(viewModel)
    } else {
        OwnerStoreReportSection(viewModel)
    }
}

@Composable
private fun KasirShiftReportSection(viewModel: PosViewModel) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val cashExpenses by viewModel.cashExpenses.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var expKet by remember { mutableStateOf("") }
    var expAmount by remember { mutableStateOf("") }

    val todayISO = remember { SimpleDateFormat("yyyy-MM-DD", Locale.US).format(Date()) }
    val shiftTrx = transactions.filter { it.kasir == (currentUser?.nama ?: "") }
    val shiftExpenses = cashExpenses.filter { it.kasir == (currentUser?.nama ?: "") }

    val omsetTunai = shiftTrx.filter { it.metode == "tunai" }.sumOf { it.totalPemasukan }
    val omsetDigital = shiftTrx.filter { it.metode == "qris" || it.metode == "transfer" }.sumOf { it.totalPemasukan }
    val totalExpenses = shiftExpenses.sumOf { it.nominal }

    val modalLaci = storeSettings.modalAwalLaci
    val expectedPhysicalCash = modalLaci + omsetTunai - totalExpenses

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("💵 Shift Uang Laci & Modal Awal", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryIndigo)

                Spacer(modifier = Modifier.height(12.dp))

                var modalInput by remember { mutableStateOf(modalLaci.toInt().toString()) }
                OutlinedTextField(
                    value = modalInput,
                    onValueChange = {
                        modalInput = it
                        viewModel.updateModalAwalLaci(it.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Modal Awal Laci (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Catat Kas Keluar (Pengeluaran Operational Shift):", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = expKet,
                        onValueChange = { expKet = it },
                        placeholder = { Text("Ket (Misal: Es Batu)", fontSize = 11.sp) },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = expAmount,
                        onValueChange = { expAmount = it },
                        placeholder = { Text("Rp", fontSize = 11.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.addCashExpense(expKet, expAmount.toDoubleOrNull() ?: 0.0)
                            expKet = ""
                            expAmount = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("+ Keluar", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                shiftExpenses.forEach { exp ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(exp.keterangan, fontSize = 12.sp)
                        Text("- Rp ${viewModel.formatRupiah(exp.nominal)}", style = PriceTextStyle.copy(fontSize = 12.sp, color = DangerRed))
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📊 Ringkasan Omset Shift Anda", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                Spacer(modifier = Modifier.height(8.dp))

                ReportRow("Total Transaksi:", "${shiftTrx.size} Transaksi")
                ReportRow("Omset Tunai (Cash):", "Rp ${viewModel.formatRupiah(omsetTunai)}", SuccessEmerald)
                ReportRow("Omset Digital (QRIS/Transfer):", "Rp ${viewModel.formatRupiah(omsetDigital)}", PrimaryIndigo)
                ReportRow("Total Kas Keluar:", "- Rp ${viewModel.formatRupiah(totalExpenses)}", DangerRed)

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                ReportRow("Uang Fisik Seharusnya (Laci):", "Rp ${viewModel.formatRupiah(expectedPhysicalCash)}", SuccessEmerald, isBold = true)

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        PdfReportHelper.generateAndPrintShiftReport(
                            context = context,
                            storeName = storeSettings.namaToko,
                            kasirName = currentUser?.nama ?: "Kasir",
                            modalLaci = modalLaci,
                            transactions = shiftTrx,
                            expenses = shiftExpenses
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🖨️ Cetak / Simpan PDF Laporan Shift", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


private enum class ReportPeriod { Harian, Mingguan, Bulanan, EvaluasiKustom, ArsipHpp }

private data class ProductSalesSummary(
    val nama: String,
    val qty: Int,
    val totalOmset: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerStoreReportSection(viewModel: PosViewModel) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val cashExpenses by viewModel.cashExpenses.collectAsState()
    val hppHistoryList by viewModel.hppHistoryList.collectAsState()

    // Calculate time bounds
    val now = remember { System.currentTimeMillis() }
    val oneDayMs = 24 * 60 * 60 * 1000L

    val todayCalendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val startOfTodayMs = todayCalendar.timeInMillis

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.Harian) }
    var customStartDateMs by remember { mutableStateOf(startOfTodayMs - (29 * oneDayMs)) }
    var customEndDateMs by remember { mutableStateOf(now) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var showExportModal by remember { mutableStateOf(false) }

    val filteredTransactions = remember(transactions, selectedPeriod, customStartDateMs, customEndDateMs) {
        when (selectedPeriod) {
            ReportPeriod.Harian -> transactions.filter { it.waktu >= startOfTodayMs }
            ReportPeriod.Mingguan -> transactions.filter { it.waktu >= (startOfTodayMs - (6 * oneDayMs)) }
            ReportPeriod.Bulanan -> transactions.filter { it.waktu >= (startOfTodayMs - (29 * oneDayMs)) }
            ReportPeriod.EvaluasiKustom -> transactions.filter { it.waktu in customStartDateMs..(customEndDateMs + oneDayMs - 1) }
            ReportPeriod.ArsipHpp -> transactions
        }
    }

    val filteredExpenses = remember(cashExpenses, selectedPeriod, customStartDateMs, customEndDateMs) {
        when (selectedPeriod) {
            ReportPeriod.Harian -> cashExpenses.filter {
                val nowStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                it.tanggalISO == nowStr
            }
            ReportPeriod.Mingguan -> cashExpenses.filter {
                val pastMs = startOfTodayMs - (6 * oneDayMs)
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.tanggalISO)
                    date != null && date.time >= pastMs
                } catch (e: Exception) { true }
            }
            ReportPeriod.Bulanan -> cashExpenses.filter {
                val pastMs = startOfTodayMs - (29 * oneDayMs)
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.tanggalISO)
                    date != null && date.time >= pastMs
                } catch (e: Exception) { true }
            }
            ReportPeriod.EvaluasiKustom -> cashExpenses.filter {
                try {
                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it.tanggalISO)
                    date != null && date.time in customStartDateMs..(customEndDateMs + oneDayMs - 1)
                } catch (e: Exception) { true }
            }
            ReportPeriod.ArsipHpp -> cashExpenses
        }
    }

    // Financial calculations
    val totalOmset = filteredTransactions.sumOf { it.totalPemasukan }
    val totalModal = filteredTransactions.sumOf { it.totalModal }
    val totalKasKeluar = filteredExpenses.sumOf { it.nominal }
    val labaKotor = totalOmset - totalModal
    val labaBersih = labaKotor - totalKasKeluar
    val profitMarginPct = if (totalOmset > 0) (labaBersih / totalOmset) * 100.0 else 0.0
    val totalTrxCount = filteredTransactions.size
    val averageBasket = if (totalTrxCount > 0) totalOmset / totalTrxCount else 0.0

    // Payment method breakdown
    val omsetTunai = filteredTransactions.filter { it.metode == "tunai" }.sumOf { it.totalPemasukan }
    val omsetQris = filteredTransactions.filter { it.metode == "qris" }.sumOf { it.totalPemasukan }
    val omsetTransfer = filteredTransactions.filter { it.metode == "transfer" }.sumOf { it.totalPemasukan }
    val omsetKasbon = filteredTransactions.filter { it.metode == "kasbon" }.sumOf { it.totalPemasukan }

    // Order type breakdown
    val countDineIn = filteredTransactions.count { it.tipePesanan == "Dine-In" }
    val countTakeaway = filteredTransactions.count { it.tipePesanan != "Dine-In" }

    // Best seller calculation
    val topProducts = remember(filteredTransactions) {
        val map = mutableMapOf<String, Pair<Int, Double>>()
        filteredTransactions.forEach { trx ->
            try {
                val jsonArray = org.json.JSONArray(trx.itemsJson)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val nama = obj.optString("nama", "Produk")
                    val qty = obj.optInt("qty", 1)
                    val subtotal = obj.optDouble("subtotal", 0.0)
                    val curr = map[nama] ?: Pair(0, 0.0)
                    map[nama] = Pair(curr.first + qty, curr.second + subtotal)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        map.map { ProductSalesSummary(it.key, it.value.first, it.value.second) }
            .sortedByDescending { it.qty }
            .take(5)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Report Period Tabs with swipe hint
        Text(
            "💡 Geser tab ke samping untuk melihat laporan lainnya",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary
        )
        SecondaryScrollableTabRow(selectedTabIndex = selectedPeriod.ordinal) {
            Tab(
                selected = selectedPeriod == ReportPeriod.Harian,
                onClick = { selectedPeriod = ReportPeriod.Harian },
                text = { Text("📅 Hari Ini", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedPeriod == ReportPeriod.Mingguan,
                onClick = { selectedPeriod = ReportPeriod.Mingguan },
                text = { Text("📆 7 Hari", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedPeriod == ReportPeriod.Bulanan,
                onClick = { selectedPeriod = ReportPeriod.Bulanan },
                text = { Text("🗓️ 30 Hari", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedPeriod == ReportPeriod.EvaluasiKustom,
                onClick = { selectedPeriod = ReportPeriod.EvaluasiKustom },
                text = { Text("🔍 Kustom Evaluasi", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedPeriod == ReportPeriod.ArsipHpp,
                onClick = { selectedPeriod = ReportPeriod.ArsipHpp },
                text = { Text("📚 Arsip HPP", fontSize = 11.sp) }
            )
        }

        if (selectedPeriod == ReportPeriod.ArsipHpp) {
            // Arsip HPP Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("📚 Arsip Riwayat HPP & Costing Resep", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SecondaryViolet)

                    Spacer(modifier = Modifier.height(8.dp))

                    if (hppHistoryList.isEmpty()) {
                        Text("Belum ada arsip riwayat HPP.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    } else {
                        hppHistoryList.forEach { hpp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hpp.namaProduk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("HPP: Rp ${viewModel.formatRupiah(hpp.hppFinal)} | Target FC: Rp ${viewModel.formatRupiah(hpp.saranTargetFC)} | ${hpp.waktuStr}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = {
                                        PdfReportHelper.printHppReport(
                                            context = context,
                                            storeName = storeSettings.namaToko,
                                            currentUser = currentUser?.nama ?: "Admin",
                                            namaProduk = hpp.namaProduk,
                                            jumlahUnit = hpp.jumlahUnitFinal,
                                            totalBahan = hpp.totalBahan,
                                            totalBiayaLain = hpp.totalBiayaLain,
                                            tenagaKerja = 0.0,
                                            overhead = hpp.totalBiayaLain,
                                            hppUnit = hpp.hppFinal,
                                            targetFcPct = hpp.targetFCPersen,
                                            saranTargetFc = hpp.saranTargetFC,
                                            m35 = hpp.m35,
                                            m50 = hpp.m50,
                                            customPrice = hpp.saranTargetFC,
                                            bahanListJson = hpp.bahanListJson
                                        )
                                    }) {
                                        Text("Cetak PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                                    }
                                    IconButton(onClick = { viewModel.deleteHppHistory(hpp.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed)
                                    }
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
        } else {
            // Main Analytics View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
                val startStr = dateFormatter.format(Date(customStartDateMs))
                val endStr = dateFormatter.format(Date(customEndDateMs))
                val customDaysCount = (((customEndDateMs - customStartDateMs) / oneDayMs) + 1.0).coerceAtLeast(1.0).toInt()

                if (selectedPeriod == ReportPeriod.EvaluasiKustom) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("📅 Rentang Tanggal Kalender", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryIndigo)
                                    Text("$startStr s.d. $endStr ($customDaysCount Hari)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                                Button(
                                    onClick = { showDateRangeDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("📅 Pilih Kalender", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Quick preset chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "7 Hari" to 7L,
                                    "14 Hari" to 14L,
                                    "30 Hari" to 30L,
                                    "60 Hari" to 60L,
                                    "90 Hari" to 90L
                                ).forEach { (label, days) ->
                                    val isSelected = customDaysCount == days.toInt()
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            customEndDateMs = now
                                            customStartDateMs = startOfTodayMs - ((days - 1) * oneDayMs)
                                        },
                                        label = { Text(label, fontSize = 10.sp) },
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                if (showDateRangeDialog) {
                    val dateRangePickerState = rememberDateRangePickerState(
                        initialSelectedStartDateMillis = customStartDateMs,
                        initialSelectedEndDateMillis = customEndDateMs
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDateRangeDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    dateRangePickerState.selectedStartDateMillis?.let { start ->
                                        customStartDateMs = start
                                    }
                                    dateRangePickerState.selectedEndDateMillis?.let { end ->
                                        customEndDateMs = end
                                    }
                                    showDateRangeDialog = false
                                }
                            ) {
                                Text("Pilih", fontWeight = FontWeight.Bold, color = PrimaryIndigo)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDateRangeDialog = false }) {
                                Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    ) {
                        DateRangePicker(
                            state = dateRangePickerState,
                            title = { Text("Pilih Rentang Tanggal Evaluasi", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Bold) },
                            showModeToggle = false,
                            modifier = Modifier.fillMaxWidth().height(420.dp)
                        )
                    }
                }

                // Summary Header
                val periodTitle = when (selectedPeriod) {
                    ReportPeriod.Harian -> "Penjualan Hari Ini"
                    ReportPeriod.Mingguan -> "Evaluasi 7 Hari Terakhir"
                    ReportPeriod.Bulanan -> "Evaluasi 30 Hari Terakhir"
                    ReportPeriod.EvaluasiKustom -> "Evaluasi Kustom ($startStr - $endStr)"
                    else -> ""
                }

                val periodDaysCount = when (selectedPeriod) {
                    ReportPeriod.Harian -> 1.0
                    ReportPeriod.Mingguan -> 7.0
                    ReportPeriod.Bulanan -> 30.0
                    ReportPeriod.EvaluasiKustom -> customDaysCount.toDouble()
                    else -> 1.0
                }
                val avgDailyOmset = totalOmset / periodDaysCount
                val avgDailyNetProfit = labaBersih / periodDaysCount

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("📊 $periodTitle", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryIndigo)
                            Text("${totalTrxCount} Transaksi", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SecondaryViolet)
                        }

                        Divider()

                        ReportRow("Total Omset Kotor:", "Rp ${viewModel.formatRupiah(totalOmset)}", isBold = true)
                        ReportRow("Total Estimasi Modal (HPP):", "- Rp ${viewModel.formatRupiah(totalModal)}", DangerRed)
                        ReportRow("Total Kas Keluar Operasional:", "- Rp ${viewModel.formatRupiah(totalKasKeluar)}", DangerRed)

                        Divider(modifier = Modifier.padding(vertical = 2.dp))

                        ReportRow("Keuntungan Bersih (Net Profit):", "Rp ${viewModel.formatRupiah(labaBersih)}", SuccessEmerald, isBold = true)
                        ReportRow("Profit Margin %:", "${String.format("%.1f", profitMarginPct)}%", if (profitMarginPct >= 20) SuccessEmerald else DangerRed)
                        ReportRow("Rata-rata Omset / Hari:", "Rp ${viewModel.formatRupiah(avgDailyOmset)}")
                        ReportRow("Rata-rata Laba Bersih / Hari:", "Rp ${viewModel.formatRupiah(avgDailyNetProfit)}", SuccessEmerald)
                        ReportRow("Rata-rata Nilai Keranjang (AOV):", "Rp ${viewModel.formatRupiah(averageBasket)}")
                    }
                }

                // Breakdown Pembayaran
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("💳 Breakdown Metode Pembayaran", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryIndigo)

                        ReportRow("💵 Tunai (Cash):", "Rp ${viewModel.formatRupiah(omsetTunai)}")
                        ReportRow("📱 QRIS Digital:", "Rp ${viewModel.formatRupiah(omsetQris)}", PrimaryIndigo)
                        ReportRow("🏦 Transfer Bank:", "Rp ${viewModel.formatRupiah(omsetTransfer)}")
                        ReportRow("📝 Kasbon Pelanggan:", "Rp ${viewModel.formatRupiah(omsetKasbon)}", DangerRed)

                        Divider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("🍽️ Tipe Pesanan: Dine-In (${countDineIn}) | Takeaway/Bawa Pulang (${countTakeaway})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Top 5 Menu Terlaris
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🔥 Top Menu Terlaris & Kontribusi Omset", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = SecondaryViolet)

                        if (topProducts.isEmpty()) {
                            Text("Belum ada transaksi pada periode ini.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        } else {
                            topProducts.forEachIndexed { index, p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("#${index + 1} ", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SecondaryViolet)
                                        Text(p.nama, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${p.qty} Porsi Terjual", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SuccessEmerald)
                                        Text("Rp ${viewModel.formatRupiah(p.totalOmset)}", style = PriceTextStyle.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)))
                                    }
                                }
                                if (index < topProducts.size - 1) Divider()
                            }
                        }
                    }
                }

                val dateFormatterExport = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
                val startStrExport = dateFormatterExport.format(Date(customStartDateMs))
                val endStrExport = dateFormatterExport.format(Date(customEndDateMs))

                Button(
                    onClick = {
                        val periodTitle = when (selectedPeriod) {
                            ReportPeriod.Harian -> "Penjualan Hari Ini"
                            ReportPeriod.Mingguan -> "Evaluasi 7 Hari Terakhir"
                            ReportPeriod.Bulanan -> "Evaluasi 30 Hari Terakhir"
                            ReportPeriod.EvaluasiKustom -> "Evaluasi Kustom ($startStrExport - $endStrExport)"
                            else -> "Evaluasi Penjualan"
                        }
                        PdfReportHelper.generateAndPrintHtmlReport(
                            context = context,
                            storeName = storeSettings.namaToko,
                            periodName = periodTitle,
                            currentUser = currentUser?.nama ?: "Pemilik",
                            transactions = filteredTransactions,
                            expenses = filteredExpenses,
                            topProducts = topProducts.map { Triple(it.nama, it.qty, it.totalOmset) }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🖨️ Cetak / Ekspor PDF Laporan Evaluasi", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showExportModal) {
        val dateFormatterExport = remember { SimpleDateFormat("dd MMM yyyy", Locale("id", "ID")) }
        val startStrExport = dateFormatterExport.format(Date(customStartDateMs))
        val endStrExport = dateFormatterExport.format(Date(customEndDateMs))

        val periodTitle = when (selectedPeriod) {
            ReportPeriod.Harian -> "Penjualan Hari Ini"
            ReportPeriod.Mingguan -> "Evaluasi 7 Hari Terakhir"
            ReportPeriod.Bulanan -> "Evaluasi 30 Hari Terakhir"
            ReportPeriod.EvaluasiKustom -> "Evaluasi Kustom ($startStrExport - $endStrExport)"
            else -> "Evaluasi Penjualan"
        }

        AlertDialog(
            onDismissRequest = { showExportModal = false },
            title = { Text("📄 Ringkasan Evaluasi Laporan Penjualan", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("==== LAPORAN EVALUASI TOKO ====", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Periode: ${selectedPeriod.name}", fontSize = 12.sp)
                    Text("Total Transaksi: $totalTrxCount", fontSize = 12.sp)
                    Text("Total Omset Kotor: Rp ${viewModel.formatRupiah(totalOmset)}", fontSize = 12.sp)
                    Text("Total Estimasi Modal (HPP): Rp ${viewModel.formatRupiah(totalModal)}", fontSize = 12.sp)
                    Text("Total Kas Keluar: Rp ${viewModel.formatRupiah(totalKasKeluar)}", fontSize = 12.sp)
                    Text("Keuntungan Bersih: Rp ${viewModel.formatRupiah(labaBersih)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuccessEmerald)
                    Text("Profit Margin: ${String.format("%.1f", profitMarginPct)}%", fontSize = 12.sp)
                    Divider()
                    Text("PEMBAYARAN:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("Tunai: Rp ${viewModel.formatRupiah(omsetTunai)}", fontSize = 11.sp)
                    Text("QRIS: Rp ${viewModel.formatRupiah(omsetQris)}", fontSize = 11.sp)
                    Text("Transfer: Rp ${viewModel.formatRupiah(omsetTransfer)}", fontSize = 11.sp)
                    Text("Kasbon: Rp ${viewModel.formatRupiah(omsetKasbon)}", fontSize = 11.sp)
                    Divider()
                    Text("MENU TERLARIS:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    topProducts.forEach {
                        Text("• ${it.nama}: ${it.qty} porsi (Rp ${viewModel.formatRupiah(it.totalOmset)})", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showExportModal = false
                            PdfReportHelper.generateAndPrintHtmlReport(
                                context = context,
                                storeName = storeSettings.namaToko,
                                periodName = periodTitle,
                                currentUser = currentUser?.nama ?: "Pemilik",
                                transactions = filteredTransactions,
                                expenses = filteredExpenses,
                                topProducts = topProducts.map { Triple(it.nama, it.qty, it.totalOmset) }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                    ) {
                        Text("🖨️ Cetak Ke PDF")
                    }
                    TextButton(onClick = { showExportModal = false }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }
}


@Composable
private fun ReportRow(
    label: String,
    value: String,
    color: Color = Color.Unspecified,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = PriceTextStyle.copy(
                fontSize = 13.sp,
                fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = color
            )
        )
    }
}
