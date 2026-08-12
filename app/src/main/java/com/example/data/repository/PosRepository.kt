package com.example.data.repository

import com.example.data.dao.PosDao
import com.example.data.model.CashExpenseEntity
import com.example.data.model.CategoryEntity
import com.example.data.model.HoldOrderEntity
import com.example.data.model.HppHistoryEntity
import com.example.data.model.KasbonEntity
import com.example.data.model.ProductEntity
import com.example.data.model.RawMaterialEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow

class PosRepository(private val posDao: PosDao) {

    // Users
    val allUsers: Flow<List<UserEntity>> = posDao.getAllUsers()
    suspend fun getUserByUsername(username: String): UserEntity? = posDao.getUserByUsername(username)
    suspend fun insertUser(user: UserEntity): Long = posDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = posDao.updateUser(user)
    suspend fun deleteUser(id: Long) = posDao.deleteUser(id)

    // Categories
    val allCategories: Flow<List<CategoryEntity>> = posDao.getAllCategories()
    suspend fun insertCategory(category: CategoryEntity): Long = posDao.insertCategory(category)
    suspend fun deleteCategory(id: Long) = posDao.deleteCategory(id)

    // Products
    val allProducts: Flow<List<ProductEntity>> = posDao.getAllProducts()
    suspend fun getProductById(id: Long): ProductEntity? = posDao.getProductById(id)
    suspend fun insertProduct(product: ProductEntity): Long = posDao.insertProduct(product)
    suspend fun updateProduct(product: ProductEntity) = posDao.updateProduct(product)
    suspend fun deleteProduct(id: Long) = posDao.deleteProduct(id)

    // Raw Materials
    val allRawMaterials: Flow<List<RawMaterialEntity>> = posDao.getAllRawMaterials()
    suspend fun getRawMaterialById(id: Long): RawMaterialEntity? = posDao.getRawMaterialById(id)
    suspend fun insertRawMaterial(rawMaterial: RawMaterialEntity): Long = posDao.insertRawMaterial(rawMaterial)
    suspend fun updateRawMaterial(rawMaterial: RawMaterialEntity) = posDao.updateRawMaterial(rawMaterial)
    suspend fun deleteRawMaterial(id: Long) = posDao.deleteRawMaterial(id)

    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = posDao.getAllTransactions()
    fun getTransactionsByDate(isoDate: String): Flow<List<TransactionEntity>> = posDao.getTransactionsByDate(isoDate)
    suspend fun insertTransaction(transaction: TransactionEntity) = posDao.insertTransaction(transaction)
    suspend fun deleteAllTransactions() = posDao.deleteAllTransactions()

    // Hold Orders
    val allHoldOrders: Flow<List<HoldOrderEntity>> = posDao.getAllHoldOrders()
    suspend fun insertHoldOrder(holdOrder: HoldOrderEntity): Long = posDao.insertHoldOrder(holdOrder)
    suspend fun deleteHoldOrder(id: Long) = posDao.deleteHoldOrder(id)

    // Kasbon
    val allKasbon: Flow<List<KasbonEntity>> = posDao.getAllKasbon()
    suspend fun insertKasbon(kasbon: KasbonEntity): Long = posDao.insertKasbon(kasbon)
    suspend fun updateKasbon(kasbon: KasbonEntity) = posDao.updateKasbon(kasbon)

    // Cash Expenses
    val allCashExpenses: Flow<List<CashExpenseEntity>> = posDao.getAllCashExpenses()
    fun getCashExpensesByDate(isoDate: String): Flow<List<CashExpenseEntity>> = posDao.getCashExpensesByDate(isoDate)
    suspend fun insertCashExpense(cashExpense: CashExpenseEntity): Long = posDao.insertCashExpense(cashExpense)
    suspend fun deleteCashExpense(id: Long) = posDao.deleteCashExpense(id)

    // HPP History
    val allHppHistory: Flow<List<HppHistoryEntity>> = posDao.getAllHppHistory()
    suspend fun insertHppHistory(hpp: HppHistoryEntity): Long = posDao.insertHppHistory(hpp)
    suspend fun deleteHppHistory(id: Long) = posDao.deleteHppHistory(id)
}
