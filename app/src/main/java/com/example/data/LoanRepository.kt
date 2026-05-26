package com.example.data

import kotlinx.coroutines.flow.Flow

class LoanRepository(private val loanDao: LoanDao) {
    val userProfile: Flow<UserProfile?> = loanDao.getUserProfile()
    val bankConnection: Flow<BankConnection?> = loanDao.getBankConnection()
    val creditProfile: Flow<CreditProfile?> = loanDao.getCreditProfile()
    val loanApplications: Flow<List<LoanApplication>> = loanDao.getAllLoanApplications()

    suspend fun saveUserProfile(profile: UserProfile) {
        loanDao.insertUserProfile(profile)
    }

    suspend fun deleteUserProfile() {
        loanDao.deleteUserProfile()
    }

    suspend fun saveBankConnection(connection: BankConnection) {
        loanDao.insertBankConnection(connection)
    }

    suspend fun deleteBankConnection() {
        loanDao.deleteBankConnection()
    }

    suspend fun saveCreditProfile(profile: CreditProfile) {
        loanDao.insertCreditProfile(profile)
    }

    suspend fun deleteCreditProfile() {
        loanDao.deleteCreditProfile()
    }

    suspend fun submitLoanApplication(app: LoanApplication) {
        loanDao.insertLoanApplication(app)
    }

    suspend fun clearAllData() {
        loanDao.deleteUserProfile()
        loanDao.deleteBankConnection()
        loanDao.deleteCreditProfile()
        loanDao.clearLoanApplications()
    }
}
