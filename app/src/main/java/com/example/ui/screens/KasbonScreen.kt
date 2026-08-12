package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DangerRed
import com.example.ui.theme.PriceTextStyle
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SuccessEmerald
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.PosViewModel

@Composable
fun KasbonScreen(viewModel: PosViewModel) {
    val kasbonList by viewModel.kasbonList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("📒 Daftar Kasbon & Piutang Pelanggan", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryIndigo)
        Spacer(modifier = Modifier.height(12.dp))

        if (kasbonList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada data kasbon/piutang.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(kasbonList) { kasbon ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(kasbon.pelanggan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Tgl: ${kasbon.tanggalStr} | Meja: ${kasbon.nomorMeja}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Total: Rp ${viewModel.formatRupiah(kasbon.total)}", style = PriceTextStyle.copy(fontSize = 13.sp, color = DangerRed))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (kasbon.status == "Lunas") {
                                    Surface(shape = RoundedCornerShape(6.dp), color = SuccessEmerald.copy(alpha = 0.15f)) {
                                        Text("LUNAS", fontSize = 10.sp, color = SuccessEmerald, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                } else {
                                    Surface(shape = RoundedCornerShape(6.dp), color = WarningAmber.copy(alpha = 0.15f)) {
                                        Text("BELUM LUNAS", fontSize = 10.sp, color = WarningAmber, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Button(
                                        onClick = { viewModel.lunasiKasbon(kasbon) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessEmerald)
                                    ) {
                                        Text("Lunasi", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
