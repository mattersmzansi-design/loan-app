package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LoanDao {
    @Query("SELECT * FROM user_profile WHERE id = 'user_profile' LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun deleteUserProfile()

    @Query("SELECT * FROM bank_connection WHERE id = 'bank_connection' LIMIT 1")
    fun getBankConnection(): Flow<BankConnection?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankConnection(connection: BankConnection)

    @Query("DELETE FROM bank_connection")
    suspend fun deleteBankConnection()

    @Query("SELECT * FROM credit_profile WHERE id = 'credit_profile' LIMIT 1")
    fun getCreditProfile(): Flow<CreditProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditProfile(profile: CreditProfile)

    @Query("DELETE FROM credit_profile")
    suspend fun deleteCreditProfile()

    @Query("SELECT * FROM loan_application ORDER BY submittedTimestamp DESC")
    fun getAllLoanApplications(): Flow<List<LoanApplication>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanApplication(app: LoanApplication)

    @Query("DELETE FROM loan_application")
    suspend fun clearLoanApplications()
}
