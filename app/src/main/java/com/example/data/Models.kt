package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: String = "user_profile",
    val firstName: String,
    val lastName: String,
    val dob: String,
    val ssnMasked: String,
    val rawSsn: String,
    val streetAddress: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val annualIncome: Double,
    val isKycVerified: Boolean = false
)

@Entity(tableName = "bank_connection")
data class BankConnection(
    @PrimaryKey val id: String = "bank_connection",
    val isConnected: Boolean = false,
    val institutionName: String = "",
    val accountNumberMasked: String = "",
    val routingNumber: String = "",
    val balance: Double = 0.0,
    val transactionsJson: String = "" // JSON representation of transaction list
)

@Entity(tableName = "credit_profile")
data class CreditProfile(
    @PrimaryKey val id: String = "credit_profile",
    val creditScore: Int = 0,
    val ratingGroup: String = "", // e.g. Super Prime, Prime, Subprime
    val totalAccounts: Int = 0,
    val openRevolvingLines: Int = 0,
    val publicRecords: Int = 0, // e.g., bankruptcies, judgments
    val recentInquiries: Int = 0,
    val reportDate: String = "",
    val reportJson: String = "" // Full detail bureau JSON
)

@Entity(tableName = "loan_application")
data class LoanApplication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val loanPurpose: String,
    val requestedAmount: Double,
    val approvedAmount: Double,
    val apr: Double,
    val termMonths: Int,
    val monthlyPayment: Double,
    val totalCost: Double,
    val status: String, // PENDING_VERIFICATION, APPROVED, DENIED, ACTIVE
    val adverseReason: String = "", // Regulatory adverse action reasons
    val submittedTimestamp: Long = System.currentTimeMillis()
)

// Simple local class mapping for displaying simulated transactions
data class TransactionItem(
    val id: String,
    val date: String,
    val amount: Double, // Negative for debits, positive for credits
    val category: String,
    val description: String
)
