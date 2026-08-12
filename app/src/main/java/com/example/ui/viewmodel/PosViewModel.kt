package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.CartItem
import com.example.data.model.CashExpenseEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.HoldOrderEntity
import com.example.data.model.HppHistoryEntity
import com.example.data.model.KasbonEntity
import com.example.data.model.ProductEntity
import com.example.data.model.RawMaterialEntity
import com.example.data.model.RecipeIngredient
import com.example.data.model.StoreSettings
import com.example.data.model.ToppingItem
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class NavTab { Kasir, Produk, Laporan, Kasbon, Pengaturan, Tentang }
enum class ProductSubTab { DaftarMenu, StokProduk, BahanBaku }
enum class SettingsSubTab { ProfilToko, QrisBank, UserManagement, Panduan, BackupRestore }
enum class PaymentModalType { Tunai, QRIS, Transfer, Kasbon }

data class HppCalcState(
    val namaProduk: String = "",
    val kategori: String = "Makanan",
    val bahanList: List<HppIngredientInput> = emptyList(),
    val tenagaKerja: Double = 0.0,
    val overhead: Double = 0.0,
    val jumlahUnit: Int = 1,
    val targetFC: Double = 35.0,
    val calculatedResult: HppResult? = null
)

data class HppIngredientInput(
    val idBahan: Long? = null,
    val nama: String,
    val hargaPartai: Double,
    val isiPartai: Double,
    val pakai: Double,
    val waste: Double,
    val hppAktual: Double
)

data class HppResult(
    val totalBahan: Double,
    val totalBiayaLain: Double,
    val hppUnit: Double,
    val saranTargetFC: Double,
    val m35: Double,
    val m50: Double,
    val customMarginVal: Double = 40.0,
    val customHarga: Double = 0.0,
    val customUntung: Double = 0.0,
    val customFoodCostPct: Double = 0.0
)

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = PosRepository(database.posDao())
    }

    // DB Flows
    val users: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val products: StateFlow<List<ProductEntity>> = repository.allProducts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val rawMaterials: StateFlow<List<RawMaterialEntity>> = repository.allRawMaterials.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val holdOrders: StateFlow<List<HoldOrderEntity>> = repository.allHoldOrders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val kasbonList: StateFlow<List<KasbonEntity>> = repository.allKasbon.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val cashExpenses: StateFlow<List<CashExpenseEntity>> = repository.allCashExpenses.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val hppHistoryList: StateFlow<List<HppHistoryEntity>> = repository.allHppHistory.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Session & Theme
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", false))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
        prefs.edit().putBoolean("is_dark_mode", isDark).apply()
    }

    private val _showLoginScreen = MutableStateFlow(true)
    val showLoginScreen: StateFlow<Boolean> = _showLoginScreen.asStateFlow()

    fun navigateToLogin() {
        _showLoginScreen.value = true
    }

    fun proceedToLogin() {
        _showLoginScreen.value = true
    }

    fun logout() {
        _currentUser.value = null
        _showLoginScreen.value = true
    }

    fun login(u: String, p: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val matched = repository.getUserByUsername(u)
            if (matched != null && matched.password == p) {
                _currentUser.value = matched
                _showLoginScreen.value = false
                onResult(true, "Berhasil masuk!")
            } else {
                onResult(false, "Username atau Password salah!")
            }
        }
    }

    // Active Navigation
    private val _selectedTab = MutableStateFlow(NavTab.Kasir)
    val selectedTab: StateFlow<NavTab> = _selectedTab.asStateFlow()

    fun selectTab(tab: NavTab) {
        _selectedTab.value = tab
    }

    private val _selectedProductSubTab = MutableStateFlow(ProductSubTab.DaftarMenu)
    val selectedProductSubTab: StateFlow<ProductSubTab> = _selectedProductSubTab.asStateFlow()

    fun selectProductSubTab(subTab: ProductSubTab) {
        _selectedProductSubTab.value = subTab
    }

    private val _selectedSettingsSubTab = MutableStateFlow(SettingsSubTab.QrisBank)
    val selectedSettingsSubTab: StateFlow<SettingsSubTab> = _selectedSettingsSubTab.asStateFlow()

    fun selectSettingsSubTab(subTab: SettingsSubTab) {
        _selectedSettingsSubTab.value = subTab
    }

    // POS Kasir Filter & Cart
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _selectedCategoryFilter = MutableStateFlow("Semua")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    fun selectCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    // Cart State
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _orderType = MutableStateFlow("Dine-In")
    val orderType: StateFlow<String> = _orderType.asStateFlow()
    fun setOrderType(type: String) { _orderType.value = type }

    private val _tableNo = MutableStateFlow("")
    val tableNo: StateFlow<String> = _tableNo.asStateFlow()
    fun setTableNo(no: String) { _tableNo.value = no }

    private val _discountVal = MutableStateFlow(0.0)
    val discountVal: StateFlow<Double> = _discountVal.asStateFlow()
    fun setDiscountVal(v: Double) { _discountVal.value = v }

    private val _discountType = MutableStateFlow("persen") // "persen" or "nominal"
    val discountType: StateFlow<String> = _discountType.asStateFlow()
    fun setDiscountType(t: String) { _discountType.value = t }

    private val _taxPercent = MutableStateFlow(0.0)
    val taxPercent: StateFlow<Double> = _taxPercent.asStateFlow()
    fun setTaxPercent(v: Double) { _taxPercent.value = v }

    private val _customerName = MutableStateFlow("")
    val customerName: StateFlow<String> = _customerName.asStateFlow()
    fun setCustomerName(name: String) { _customerName.value = name }

    private val _cashPaid = MutableStateFlow(0.0)
    val cashPaid: StateFlow<Double> = _cashPaid.asStateFlow()
    fun setCashPaid(amount: Double) { _cashPaid.value = amount }

    // Store Settings
    private val _storeSettings = MutableStateFlow(StoreSettings())
    val storeSettings: StateFlow<StoreSettings> = _storeSettings.asStateFlow()

    fun updateStoreProfile(namaToko: String, alamatToko: String, noTelpToko: String, pesanStruk: String) {
        _storeSettings.value = _storeSettings.value.copy(
            namaToko = namaToko,
            alamatToko = alamatToko,
            noTelpToko = noTelpToko,
            pesanStruk = pesanStruk
        )
    }

    fun updateQrisUrl(url: String) {
        _storeSettings.value = _storeSettings.value.copy(qrisUrl = url)
    }

    fun updateBankInfo(bankNama: String, bankNoRek: String, bankPemilik: String) {
        _storeSettings.value = _storeSettings.value.copy(
            bankNama = bankNama,
            bankNoRek = bankNoRek,
            bankPemilik = bankPemilik
        )
    }

    fun updateModalAwalLaci(modal: Double) {
        _storeSettings.value = _storeSettings.value.copy(modalAwalLaci = modal)
    }

    // Modal / Dialog States
    private val _activeProductForOptions = MutableStateFlow<ProductEntity?>(null)
    val activeProductForOptions: StateFlow<ProductEntity?> = _activeProductForOptions.asStateFlow()

    fun openProductOptions(product: ProductEntity) {
        _activeProductForOptions.value = product
    }

    fun closeProductOptions() {
        _activeProductForOptions.value = null
    }

    private val _activePaymentModal = MutableStateFlow<PaymentModalType?>(null)
    val activePaymentModal: StateFlow<PaymentModalType?> = _activePaymentModal.asStateFlow()

    fun openPaymentModal(type: PaymentModalType) {
        _activePaymentModal.value = type
    }

    fun closePaymentModal() {
        _activePaymentModal.value = null
    }

    private val _activeReceipt = MutableStateFlow<TransactionEntity?>(null)
    val activeReceipt: StateFlow<TransactionEntity?> = _activeReceipt.asStateFlow()

    fun showReceipt(trx: TransactionEntity) {
        _activeReceipt.value = trx
    }

    fun closeReceipt() {
        _activeReceipt.value = null
    }

    private val _showHoldOrdersModal = MutableStateFlow(false)
    val showHoldOrdersModal: StateFlow<Boolean> = _showHoldOrdersModal.asStateFlow()

    fun toggleHoldOrdersModal(show: Boolean) {
        _showHoldOrdersModal.value = show
    }

    private val _showGuideModal = MutableStateFlow<String?>(null) // "owner" or "kasir"
    val showGuideModal: StateFlow<String?> = _showGuideModal.asStateFlow()

    fun openGuideModal(role: String) { _showGuideModal.value = role }
    fun closeGuideModal() { _showGuideModal.value = null }

    // HPP Calculator State
    private val _hppState = MutableStateFlow(HppCalcState())
    val hppState: StateFlow<HppCalcState> = _hppState.asStateFlow()

    fun updateHppNamaProduk(nama: String) { _hppState.value = _hppState.value.copy(namaProduk = nama) }
    fun updateHppKategori(kategori: String) { _hppState.value = _hppState.value.copy(kategori = kategori) }
    fun updateHppTenagaKerja(v: Double) { _hppState.value = _hppState.value.copy(tenagaKerja = v) }
    fun updateHppOverhead(v: Double) { _hppState.value = _hppState.value.copy(overhead = v) }
    fun updateHppJumlahUnit(v: Int) { _hppState.value = _hppState.value.copy(jumlahUnit = maxOf(1, v)) }
    fun updateHppTargetFC(v: Double) { _hppState.value = _hppState.value.copy(targetFC = v) }

    fun addHppIngredient(nama: String, hargaPartai: Double, isiPartai: Double, pakai: Double, waste: Double, idBahan: Long? = null) {
        val hPerUnit = hargaPartai / maxOf(1.0, isiPartai)
        val murni = hPerUnit * pakai
        val denganWaste = murni * (1.0 + (waste / 100.0))
        val item = HppIngredientInput(idBahan, nama, hargaPartai, isiPartai, pakai, waste, denganWaste)
        val newList = _hppState.value.bahanList + item
        _hppState.value = _hppState.value.copy(bahanList = newList)
    }

    fun removeHppIngredient(index: Int) {
        val list = _hppState.value.bahanList.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _hppState.value = _hppState.value.copy(bahanList = list)
        }
    }

    fun resetHppCalculator(defaultCategory: String = "Makanan") {
        _hppState.value = HppCalcState(kategori = defaultCategory)
    }

    fun calculateHppResult() {
        val st = _hppState.value
        val totalBahan = st.bahanList.sumOf { it.hppAktual }
        val totalBiayaLain = st.tenagaKerja + st.overhead
        val totalBiaya = totalBahan + totalBiayaLain
        val unit = maxOf(1, st.jumlahUnit)
        val hppUnit = (totalBiaya / unit)

        val targetFCDecimal = maxOf(0.01, st.targetFC / 100.0)
        // Standard F&B Target Selling Price based on Food Cost Target %
        val saranTargetFC = Math.round(hppUnit / targetFCDecimal).toDouble()
        // Profit Margin 35%: Price = HPP / (1 - 0.35)
        val m35 = Math.round(hppUnit / 0.65).toDouble()
        // Profit Margin 50%: Price = HPP / (1 - 0.50)
        val m50 = Math.round(hppUnit / 0.50).toDouble()

        val defaultHarga = saranTargetFC
        val untung = maxOf(0.0, defaultHarga - hppUnit)
        val fcPct = if (defaultHarga > 0) (hppUnit / defaultHarga) * 100.0 else 0.0

        val result = HppResult(
            totalBahan = totalBahan,
            totalBiayaLain = totalBiayaLain,
            hppUnit = Math.round(hppUnit).toDouble(),
            saranTargetFC = saranTargetFC,
            m35 = m35,
            m50 = m50,
            customHarga = defaultHarga,
            customUntung = Math.round(untung).toDouble(),
            customFoodCostPct = Math.round(fcPct * 10.0) / 10.0
        )
        _hppState.value = _hppState.value.copy(calculatedResult = result)
    }

    fun updateCustomHppPrice(price: Double) {
        val res = _hppState.value.calculatedResult ?: return
        val untung = price - res.hppUnit
        val fcPct = if (price > 0) (res.hppUnit / price) * 100.0 else 0.0
        val marginPct = if (price > 0) (untung / price) * 100.0 else 0.0
        _hppState.value = _hppState.value.copy(
            calculatedResult = res.copy(
                customHarga = price,
                customUntung = Math.round(untung).toDouble(),
                customFoodCostPct = Math.round(fcPct * 10.0) / 10.0,
                customMarginVal = Math.round(marginPct * 10.0) / 10.0
            )
        )
    }

    fun saveHppToArchiveAndMenu(onDone: () -> Unit) {
        val st = _hppState.value
        val res = st.calculatedResult ?: return
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date())

            val bahanJson = JSONArray().apply {
                st.bahanList.forEach { b ->
                    put(JSONObject().apply {
                        put("idBahan", b.idBahan)
                        put("nama", b.nama)
                        put("pakai", b.pakai)
                        put("hppAktual", b.hppAktual)
                    })
                }
            }.toString()

            val hppEntity = HppHistoryEntity(
                waktuStr = dateStr,
                namaProduk = st.namaProduk,
                hppFinal = res.hppUnit,
                saranTargetFC = res.saranTargetFC,
                m35 = res.m35,
                m50 = res.m50,
                jumlahUnitFinal = st.jumlahUnit,
                totalBahan = res.totalBahan,
                totalBiayaLain = res.totalBiayaLain,
                targetFCPersen = st.targetFC,
                bahanListJson = bahanJson
            )
            repository.insertHppHistory(hppEntity)

            // Also sync/update or insert product in Product Menu
            if (st.namaProduk.isNotBlank()) {
                val existingProduct = products.value.find { it.nama.equals(st.namaProduk, ignoreCase = true) }
                if (existingProduct != null) {
                    repository.updateProduct(
                        existingProduct.copy(
                            kategori = st.kategori,
                            modal = res.hppUnit,
                            jual = res.customHarga
                        )
                    )
                } else {
                    val newProduct = ProductEntity(
                        emoji = "🥤",
                        nama = st.namaProduk,
                        kategori = st.kategori,
                        modal = res.hppUnit,
                        jual = res.customHarga,
                        stok = st.jumlahUnit,
                        aktif = true
                    )
                    repository.insertProduct(newProduct)
                }
            }

            onDone()
        }
    }

    // Cart Management Methods
    fun addProductToCart(product: ProductEntity, varian: String = "", toppings: List<ToppingItem> = emptyList()) {
        val currentList = _cart.value.toMutableList()
        val toppingPriceSum = toppings.sumOf { it.harga }
        val toppingNamesSorted = toppings.map { it.nama }.sorted().joinToString(",")
        val cartKey = "${product.id}|$varian|$toppingNamesSorted"

        var namaLengkap = product.nama
        if (varian.isNotBlank()) namaLengkap += " ($varian)"
        if (toppings.isNotEmpty()) namaLengkap += " +[${toppings.joinToString(", ") { it.nama }}]"

        val existingIndex = currentList.indexOfFirst { it.cartKey == cartKey }
        val existingQty = if (existingIndex != -1) currentList[existingIndex].qty else 0

        if (product.stok < (existingQty + 1)) return

        if (existingIndex != -1) {
            val oldItem = currentList[existingIndex]
            val newQty = oldItem.qty + 1
            val effectiveUnit = if (oldItem.grosirMin > 0 && newQty >= oldItem.grosirMin) oldItem.grosirHarga else oldItem.jualDasar
            val newSubtotal = newQty * (effectiveUnit + oldItem.toppingPrice)
            currentList[existingIndex] = oldItem.copy(qty = newQty, subtotal = newSubtotal)
        } else {
            val effectiveUnit = if (product.grosirMin > 0 && 1 >= product.grosirMin) product.grosirHarga else product.jual
            val item = CartItem(
                cartKey = cartKey,
                id = product.id,
                nama = namaLengkap,
                modal = product.modal,
                jualDasar = product.jual,
                grosirMin = product.grosirMin,
                grosirHarga = product.grosirHarga,
                toppingPrice = toppingPriceSum,
                qty = 1,
                subtotal = effectiveUnit + toppingPriceSum
            )
            currentList.add(item)
        }
        _cart.value = currentList
    }

    fun updateCartQty(index: Int, delta: Int) {
        val currentList = _cart.value.toMutableList()
        if (index !in currentList.indices) return
        val item = currentList[index]
        val newQty = item.qty + delta
        if (newQty <= 0) {
            currentList.removeAt(index)
        } else {
            val effectiveUnit = if (item.grosirMin > 0 && newQty >= item.grosirMin) item.grosirHarga else item.jualDasar
            val newSubtotal = newQty * (effectiveUnit + item.toppingPrice)
            currentList[index] = item.copy(qty = newQty, subtotal = newSubtotal)
        }
        _cart.value = currentList
    }

    fun clearCart() {
        _cart.value = emptyList()
        _customerName.value = ""
        _tableNo.value = ""
        _cashPaid.value = 0.0
    }

    // Checkout / Payment Process
    fun checkout(metode: String) {
        val c = _cart.value
        if (c.isEmpty()) return

        val rawSubtotal = c.sumOf { it.subtotal }
        val dType = _discountType.value
        val dVal = _discountVal.value
        val nomDiskon = if (dType == "persen") (rawSubtotal * (dVal / 100.0)) else minOf(rawSubtotal, dVal)
        val setelahDiskon = rawSubtotal - nomDiskon
        val nomPajak = (setelahDiskon * (_taxPercent.value / 100.0))
        val totalAkhir = Math.round(setelahDiskon + nomPajak).toDouble()

        val bayar = if (metode == "tunai") maxOf(totalAkhir, _cashPaid.value) else totalAkhir
        val kembali = if (metode == "tunai") maxOf(0.0, bayar - totalAkhir) else 0.0

        val now = Date()
        val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(now)
        val trxId = "TRX-${System.currentTimeMillis().toString().takeLast(6)}"

        val itemsJsonArray = JSONArray().apply {
            c.forEach { item ->
                put(JSONObject().apply {
                    put("cartKey", item.cartKey)
                    put("id", item.id)
                    put("nama", item.nama)
                    put("modal", item.modal)
                    put("jualDasar", item.jualDasar)
                    put("toppingPrice", item.toppingPrice)
                    put("qty", item.qty)
                    put("subtotal", item.subtotal)
                })
            }
        }.toString()

        val totalModalSum = c.sumOf { it.modal * it.qty }
        val kasirNama = _currentUser.value?.nama ?: "Kasir Utama"

        val trx = TransactionEntity(
            id = trxId,
            waktu = now.time,
            tanggalISO = isoDate,
            tanggalStr = dateStr,
            kasir = kasirNama,
            pelanggan = _customerName.value.ifBlank { "Umum" },
            tipePesanan = _orderType.value,
            nomorMeja = _tableNo.value.ifBlank { "-" },
            metode = metode,
            subtotal = rawSubtotal,
            diskon = nomDiskon,
            pajak = nomPajak,
            totalPemasukan = totalAkhir,
            totalModal = totalModalSum,
            uangBayar = bayar,
            uangKembali = kembali,
            itemsJson = itemsJsonArray
        )

        viewModelScope.launch {
            if (metode == "kasbon") {
                val kasbon = KasbonEntity(
                    trxId = trxId,
                    tanggalISO = isoDate,
                    tanggalStr = dateStr,
                    pelanggan = _customerName.value.ifBlank { "Umum" },
                    tipePesanan = _orderType.value,
                    nomorMeja = _tableNo.value.ifBlank { "-" },
                    total = totalAkhir,
                    status = "Belum Lunas",
                    itemsJson = itemsJsonArray
                )
                repository.insertKasbon(kasbon)
            } else {
                repository.insertTransaction(trx)
            }

            // Deduct product stock & raw material inventory (BOM)
            c.forEach { item ->
                val p = repository.getProductById(item.id)
                if (p != null) {
                    val updatedStock = maxOf(0, p.stok - item.qty)
                    repository.updateProduct(p.copy(stok = updatedStock))

                    // If BOM resep is configured, parse and deduct raw materials
                    if (p.resepJson.isNotBlank() && p.resepJson != "[]") {
                        try {
                            val jsonArray = JSONArray(p.resepJson)
                            for (i in 0 until jsonArray.length()) {
                                val obj = jsonArray.getJSONObject(i)
                                val idBahan = if (obj.has("idBahan")) obj.optLong("idBahan", -1) else -1
                                val pakai = obj.optDouble("pakai", 0.0)
                                if (idBahan > 0) {
                                    val rm = repository.getRawMaterialById(idBahan)
                                    if (rm != null) {
                                        val newRmStok = maxOf(0.0, rm.stok - (pakai * item.qty))
                                        repository.updateRawMaterial(rm.copy(stok = newRmStok))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            _activeReceipt.value = trx
            clearCart()
        }
    }

    // Held Orders Logic
    fun holdCurrentOrder() {
        val c = _cart.value
        if (c.isEmpty()) return
        val nowStr = SimpleDateFormat("HH:mm", Locale("id", "ID")).format(Date())
        val cust = _customerName.value.ifBlank { "Pelanggan #${(holdOrders.value.size + 1)}" }
        val table = _tableNo.value.ifBlank { "Meja #${(holdOrders.value.size + 1)}" }

        val itemsJson = JSONArray().apply {
            c.forEach { item ->
                put(JSONObject().apply {
                    put("cartKey", item.cartKey)
                    put("id", item.id)
                    put("nama", item.nama)
                    put("modal", item.modal)
                    put("jualDasar", item.jualDasar)
                    put("grosirMin", item.grosirMin)
                    put("grosirHarga", item.grosirHarga)
                    put("toppingPrice", item.toppingPrice)
                    put("qty", item.qty)
                    put("subtotal", item.subtotal)
                })
            }
        }.toString()

        val entity = HoldOrderEntity(
            waktuStr = nowStr,
            pelanggan = cust,
            nomorMeja = table,
            tipePesanan = _orderType.value,
            itemsJson = itemsJson
        )

        viewModelScope.launch {
            repository.insertHoldOrder(entity)
            clearCart()
        }
    }

    fun restoreHoldOrder(holdOrder: HoldOrderEntity) {
        viewModelScope.launch {
            val list = mutableListOf<CartItem>()
            try {
                val jsonArr = JSONArray(holdOrder.itemsJson)
                for (i in 0 until jsonArr.length()) {
                    val obj = jsonArr.getJSONObject(i)
                    list.add(
                        CartItem(
                            cartKey = obj.getString("cartKey"),
                            id = obj.getLong("id"),
                            nama = obj.getString("nama"),
                            modal = obj.getDouble("modal"),
                            jualDasar = obj.getDouble("jualDasar"),
                            grosirMin = obj.optInt("grosirMin", 0),
                            grosirHarga = obj.optDouble("grosirHarga", 0.0),
                            toppingPrice = obj.getDouble("toppingPrice"),
                            qty = obj.getInt("qty"),
                            subtotal = obj.getDouble("subtotal")
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            _cart.value = list
            _customerName.value = holdOrder.pelanggan
            _tableNo.value = holdOrder.nomorMeja
            _orderType.value = holdOrder.tipePesanan
            repository.deleteHoldOrder(holdOrder.id)
            _showHoldOrdersModal.value = false
        }
    }

    fun deleteHoldOrder(id: Long) {
        viewModelScope.launch {
            repository.deleteHoldOrder(id)
        }
    }

    // Kasbon Mark as Paid
    fun lunasiKasbon(kasbon: KasbonEntity) {
        viewModelScope.launch {
            val updated = kasbon.copy(status = "Lunas")
            repository.updateKasbon(updated)

            val now = Date()
            val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(now)
            val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)

            val trx = TransactionEntity(
                id = "LUNAS-${kasbon.trxId}",
                waktu = now.time,
                tanggalISO = isoDate,
                tanggalStr = dateStr,
                kasir = _currentUser.value?.nama ?: "Kasir Utama",
                pelanggan = kasbon.pelanggan,
                tipePesanan = kasbon.tipePesanan,
                nomorMeja = kasbon.nomorMeja,
                metode = "tunai",
                totalPemasukan = kasbon.total,
                itemsJson = kasbon.itemsJson
            )
            repository.insertTransaction(trx)
        }
    }

    // Cash Expenses (Kas Keluar)
    fun addCashExpense(ket: String, nominal: Double) {
        if (ket.isBlank() || nominal <= 0) return
        val now = Date()
        val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now)
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(now)
        val entity = CashExpenseEntity(
            tanggalISO = isoDate,
            tanggalStr = dateStr,
            kasir = _currentUser.value?.nama ?: "Kasir Utama",
            keterangan = ket,
            nominal = nominal
        )
        viewModelScope.launch {
            repository.insertCashExpense(entity)
        }
    }

    fun deleteCashExpense(id: Long) {
        viewModelScope.launch {
            repository.deleteCashExpense(id)
        }
    }

    // Product CRUD Operations
    fun saveProduct(
        id: Long = 0,
        emoji: String,
        nama: String,
        kategori: String,
        modal: Double,
        jual: Double,
        grosirMin: Int,
        grosirHarga: Double,
        stok: Int,
        varianList: List<String>,
        toppingList: List<ToppingItem>,
        resepList: List<RecipeIngredient>
    ) {
        if (nama.isBlank() || jual <= 0) return
        val varianJson = JSONArray(varianList).toString()
        val toppingJson = JSONArray().apply {
            toppingList.forEach { t ->
                put(JSONObject().apply {
                    put("nama", t.nama)
                    put("harga", t.harga)
                })
            }
        }.toString()

        val resepJson = JSONArray().apply {
            resepList.forEach { r ->
                put(JSONObject().apply {
                    put("idBahan", r.idBahan)
                    put("nama", r.nama)
                    put("pakai", r.pakai)
                })
            }
        }.toString()

        val entity = ProductEntity(
            id = id,
            emoji = emoji.ifBlank { "📦" },
            nama = nama,
            kategori = kategori,
            modal = modal,
            jual = jual,
            grosirMin = grosirMin,
            grosirHarga = grosirHarga,
            varianJson = varianJson,
            toppingJson = toppingJson,
            stok = stok,
            resepJson = resepJson,
            aktif = true
        )

        viewModelScope.launch {
            if (id > 0) repository.updateProduct(entity) else repository.insertProduct(entity)
        }
    }

    fun toggleProductActive(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product.copy(aktif = !product.aktif))
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            repository.deleteProduct(id)
        }
    }

    fun adjustProductStock(productId: Long, delta: Int) {
        viewModelScope.launch {
            val p = repository.getProductById(productId) ?: return@launch
            val updated = maxOf(0, p.stok + delta)
            repository.updateProduct(p.copy(stok = updated))
        }
    }

    // Raw Material CRUD
    fun saveRawMaterial(id: Long = 0, nama: String, harga: Double, isi: Double, satuan: String, stok: Double? = null) {
        if (nama.isBlank() || harga <= 0 || isi <= 0) return
        viewModelScope.launch {
            if (id > 0) {
                val existing = repository.getRawMaterialById(id)
                val finalStok = stok ?: existing?.stok ?: isi
                val entity = RawMaterialEntity(
                    id = id,
                    nama = nama,
                    harga = harga,
                    isi = isi,
                    stok = finalStok,
                    satuan = satuan.ifBlank { "gram" }
                )
                repository.updateRawMaterial(entity)
            } else {
                val entity = RawMaterialEntity(
                    id = 0,
                    nama = nama,
                    harga = harga,
                    isi = isi,
                    stok = stok ?: isi,
                    satuan = satuan.ifBlank { "gram" }
                )
                repository.insertRawMaterial(entity)
            }
        }
    }

    fun adjustRawMaterialStock(rawMaterialId: Long, delta: Double) {
        viewModelScope.launch {
            val rm = repository.getRawMaterialById(rawMaterialId) ?: return@launch
            val updated = maxOf(0.0, rm.stok + delta)
            repository.updateRawMaterial(rm.copy(stok = updated))
        }
    }

    fun deleteRawMaterial(id: Long) {
        viewModelScope.launch {
            repository.deleteRawMaterial(id)
        }
    }

    // Category CRUD
    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.insertCategory(CategoryEntity(name = name))
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    // User Management CRUD
    fun saveUser(id: Long = 0, nama: String, username: String, pass: String, role: String) {
        if (nama.isBlank() || username.isBlank() || pass.isBlank()) return
        val entity = UserEntity(id = id, nama = nama, username = username, password = pass, role = role)
        viewModelScope.launch {
            if (id > 0) repository.updateUser(entity) else repository.insertUser(entity)
        }
    }

    fun deleteUser(id: Long) {
        viewModelScope.launch {
            repository.deleteUser(id)
        }
    }

    fun resetUserPassword(username: String, newPass: String) {
        viewModelScope.launch {
            val u = repository.getUserByUsername(username)
            if (u != null) {
                repository.updateUser(u.copy(password = newPass))
            }
        }
    }

    fun deleteHppHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteHppHistory(id)
        }
    }

    fun deleteAllTransactions() {
        viewModelScope.launch {
            repository.deleteAllTransactions()
        }
    }

    // Formatting utility
    fun formatRupiah(amount: Double): String {
        return try {
            val formatter = NumberFormat.getInstance(Locale("id", "ID"))
            formatter.format(Math.round(amount))
        } catch (e: Exception) {
            Math.round(amount).toString()
        }
    }
}
