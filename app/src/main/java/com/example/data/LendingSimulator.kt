package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

object LendingSimulator {

    // Simulates an API fetch to Experian or Equifax
    fun getCreditBureauReport(firstName: String, lastName: String, ssn: String): CreditProfile {
        // We simulate scoring variations based on the last digit of the SSN
        // This is a neat, predictable simulation mechanism
        val lastDigit = ssn.filter { it.isDigit() }.lastOrNull()?.toString()?.toIntOrNull() ?: 5
        
        val score = when (lastDigit) {
            9 -> 810 // Super Prime
            8 -> 760 // Super Prime
            7 -> 710 // Prime
            6 -> 680 // Prime
            5 -> 640 // Near Prime
            4 -> 610 // Near Prime
            3 -> 580 // Subprime (High cashflow will rescue)
            2 -> 540 // Subprime (Will be denied unless great cashflow)
            1 -> 490 // Denied (Too low)
            else -> 660 // Prime baseline
        }

        val ratingGroup = when {
            score >= 720 -> "Super Prime"
            score >= 660 -> "Prime"
            score >= 600 -> "Near Prime"
            else -> "Subprime"
        }

        val totalAccounts = 4 + (lastDigit * 2)
        val openRevolving = 2 + (lastDigit % 4)
        val publicRecords = if (score < 600 && lastDigit % 2 == 1) 1 else 0 // bankruptcy for certain low scores
        val recentInquiries = lastDigit % 3

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())

        // Create a realistic structured credit report JSON payload
        val reportObj = JSONObject().apply {
            put("provider", "Equifax Consumer Hub API")
            put("requestTimestamp", System.currentTimeMillis())
            put("reportDate", today)
            put("referenceId", "CR-" + UUID.randomUUID().toString().uppercase(Locale.US).take(8))
            
            val subject = JSONObject().apply {
                put("fullName", "$firstName $lastName")
                put("dob", "1988-10-14")
                put("maskedSsn", "XXX-XX-${ssn.takeLast(4)}")
            }
            put("subject", subject)

            val scoreObj = JSONObject().apply {
                put("score", score)
                put("model", "FICO Score 8")
                put("tier", ratingGroup)
            }
            put("creditScore", scoreObj)

            val activeTradelines = JSONArray().apply {
                put(JSONObject().apply {
                    put("creditor", "Apex Credit Cards")
                    put("type", "Revolving")
                    put("limit", 10000.0)
                    put("balance", 1200.0 + (lastDigit * 300))
                    put("status", "Current")
                    put("openedDate", "2019-04-12")
                })
                if (score > 600) {
                    put(JSONObject().apply {
                        put("creditor", "Summit Auto Finance")
                        put("type", "Installment Loan")
                        put("limit", 25000.0)
                        put("balance", 12000.0)
                        put("status", "Current")
                        put("openedDate", "2021-09-01")
                    })
                }
            }
            put("tradelines", activeTradelines)

            val summaryObj = JSONObject().apply {
                put("totalDebt", 1200.0 + (lastDigit * 300) + (if (score > 600) 12000.0 else 0.0))
                put("utilizationRate", (1200.0 + (lastDigit * 300)) / 10000.0)
                put("publicRecords", publicRecords)
                put("inquiries", recentInquiries)
                put("derogatoryAccounts", if (score < 550) 2 else 0)
            }
            put("summary", summaryObj)
        }

        return CreditProfile(
            creditScore = score,
            ratingGroup = ratingGroup,
            totalAccounts = totalAccounts,
            openRevolvingLines = openRevolving,
            publicRecords = publicRecords,
            recentInquiries = recentInquiries,
            reportDate = today,
            reportJson = reportObj.toString(2)
        )
    }

    // Generates simulated Open Banking responses (account info + transaction list)
    fun getSimulatedBankConnection(institution: String, clientIncome: Double): BankConnection {
        val lastDigit = institution.length % 10
        val accountNumber = "•••• •••• •••• " + (3000 + lastDigit * 231).toString()
        val routing = "02100002" + lastDigit

        val balance = 1500.0 + (lastDigit * 850.0)
        
        // Let's create transactions
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        val monthlySalary = clientIncome / 12.0
        val list = mutableListOf<TransactionItem>()

        // Income deposit (recurring every month / 15 days)
        cal.add(Calendar.DAY_OF_YEAR, -5)
        list.add(TransactionItem(
            id = "tx_pay_1",
            date = sdf.format(cal.time),
            amount = monthlySalary / 2.0,
            category = "Income",
            description = "DIRECT DEPOSIT / PAYCHECK COMPANY INC"
        ))

        cal.add(Calendar.DAY_OF_YEAR, -10)
        list.add(TransactionItem(
            id = "tx_util_1",
            date = sdf.format(cal.time),
            amount = -142.50,
            category = "Utilities",
            description = "CITY WATER & ELECTRIC UTILITY"
        ))

        cal.add(Calendar.DAY_OF_YEAR, -5)
        list.add(TransactionItem(
            id = "tx_groceries_1",
            date = sdf.format(cal.time),
            amount = -215.30,
            category = "Food & Groceries",
            description = "WHOLE FOODS SEATTLE WA"
        ))

        cal.add(Calendar.DAY_OF_YEAR, -2)
        list.add(TransactionItem(
            id = "tx_pay_2",
            date = sdf.format(cal.time),
            amount = monthlySalary / 2.0,
            category = "Income",
            description = "DIRECT DEPOSIT / PAYCHECK COMPANY INC"
        ))

        cal.add(Calendar.DAY_OF_YEAR, -3)
        list.add(TransactionItem(
            id = "tx_sub_1",
            date = sdf.format(cal.time),
            amount = -14.99,
            category = "Entertainment",
            description = "NETFLIX MOVIE ENTERTAINMENT"
        ))

        cal.add(Calendar.DAY_OF_YEAR, -4)
        list.add(TransactionItem(
            id = "tx_dining_1",
            date = sdf.format(cal.time),
            amount = -84.20,
            category = "Dining Out",
            description = "THE STEAKHOUSE BAR & GRILL"
        ))

        // Convert transactions list to JSON Array String
        val txArrayJson = JSONArray()
        list.forEach { tx ->
            val robj = JSONObject().apply {
                put("id", tx.id)
                put("date", tx.date)
                put("amount", tx.amount)
                put("category", tx.category)
                put("description", tx.description)
            }
            txArrayJson.put(robj)
        }

        return BankConnection(
            isConnected = true,
            institutionName = institution,
            accountNumberMasked = accountNumber,
            routingNumber = routing,
            balance = balance,
            transactionsJson = txArrayJson.toString(2)
        )
    }

    // Runs internal proprietary credit decision and pricing logic
    // Compiles a Decision state: Approved (with params) or Denied (with adverse reasons)
    fun runCreditDecisionEngine(
        profile: UserProfile,
        bureau: CreditProfile?,
        bank: BankConnection?
    ): DecisionResult {
        val income = profile.annualIncome
        val monthlyIncome = income / 12.0
        
        if (bureau == null) {
            return DecisionResult.Error("Please pull credit bureau information first.")
        }

        val creditScore = bureau.creditScore
        
        // Compute DTI & Cash Flow alternative scoring if open banking is linked
        var monthlyDebtPayment = 300.0 // Default assumed minimum obligations from tradelines
        var openBankingDiscountsApproved = false
        var supplementalIncomeVerified = 0.0

        if (bank != null && bank.isConnected) {
            // Read simulated transaction list and score alternate attributes
            val txArray = try {
                JSONArray(bank.transactionsJson)
            } catch (e: Exception) {
                JSONArray()
            }
            
            var totalVerifiedInflow = 0.0
            var rentPayments = 0.0
            
            for (i in 0 until txArray.length()) {
                val item = txArray.optJSONObject(i) ?: continue
                val amount = item.optDouble("amount", 0.0)
                val cat = item.optString("category", "")
                val desc = item.optString("description", "")
                
                if (amount > 0 && cat == "Income") {
                    totalVerifiedInflow += amount
                }
                if (amount < 0 && (desc.contains("RENT", ignoreCase = true) || desc.contains("MORTGAGE", ignoreCase = true))) {
                    rentPayments += Math.abs(amount)
                }
            }

            // High alternative banking factors can improve scoring
            supplementalIncomeVerified = totalVerifiedInflow
            if (totalVerifiedInflow > 100) {
                openBankingDiscountsApproved = true
            }
            
            // Adjust estimated obligations based on real transactional rent payments
            if (rentPayments > 0) {
                monthlyDebtPayment += rentPayments
            }
        }

        // Compute DTI: monthly debt / monthly income
        val dti = if (monthlyIncome > 0) (monthlyDebtPayment / monthlyIncome) else 1.0
        
        // DECISION TREE:
        // 1. Extreme limits checks:
        if (creditScore < 500) {
            return DecisionResult.Denied(
                "Credit Score below required system minimum (500).",
                adverseReasons = listOf(
                    "Credit Score below acceptable threshold (FICO score: $creditScore).",
                    "Insufficient credit history or high volume of recent delinquent tradelines.",
                    "Lack of alternative bank-verified cash reserves."
                )
            )
        }

        if (dti > 0.55) {
            return DecisionResult.Denied(
                "Debt-to-Income (DTI) ratio is too high ($dti).",
                adverseReasons = listOf(
                    "Debt obligations exceed 55% of verifiable monthly gross income.",
                    "High leverage across active revolving credit lines.",
                    "Inadequate residual cash flow surplus computed from active checking statements."
                )
            )
        }

        if (bureau.publicRecords > 0) {
            // Public Record (Credit Bankruptcy)
            // A super high Open Banking cash flow can salvage but let's be realistic
            if (bank == null || !bank.isConnected || bank.balance < 5000) {
                return DecisionResult.Denied(
                    "Recent bankruptcy public record found without substantial banking reserves.",
                    adverseReasons = listOf(
                        "Public record showing outstanding bankruptcy or tax lien.",
                        "Absence of secondary liquid collateral or offset bank account verification."
                    )
                )
            }
        }

        // Determine Loan Pricing tiers
        val (baseApr, maxAmount, termMonths) = when {
            creditScore >= 750 -> {
                // Super Prime Premium
                Triple(if (openBankingDiscountsApproved) 5.99 else 6.99, 50000.0, 60)
            }
            creditScore >= 680 -> {
                // Prime
                Triple(if (openBankingDiscountsApproved) 9.99 else 11.49, 35000.0, 48)
            }
            creditScore >= 620 -> {
                // Near Prime
                Triple(if (openBankingDiscountsApproved) 14.99 else 16.99, 20000.0, 36)
            }
            else -> {
                // Subprime / Alternative evaluation
                // If Open Banking is connected, let's offer an Alternative Credit Builder Loan!
                if (bank != null && bank.isConnected && bank.balance > 1000) {
                    Triple(24.99, 8000.0, 24)
                } else {
                    return DecisionResult.Denied(
                        "Subprime score and lack of secondary bank transaction data verification.",
                        adverseReasons = listOf(
                            "Credit rating group classified as subprime (FICO score: $creditScore).",
                            "Open Banking statement verification is missing or shows insufficient ledger reserves."
                        )
                    )
                }
            }
        }

        // Custom pricing calculation based on requested amount
        // Approved amount is smaller of requested amount or max allowed tier
        val finalRepayTerm = termMonths
        
        return DecisionResult.Approved(
            tier = bureau.ratingGroup,
            baseApr = baseApr,
            maxSum = maxAmount,
            termMonths = finalRepayTerm,
            dtiEvaluated = dti,
            alternativeDiscountApplied = openBankingDiscountsApproved
        )
    }

    sealed class DecisionResult {
        data class Approved(
            val tier: String,
            val baseApr: Double,
            val maxSum: Double,
            val termMonths: Int,
            val dtiEvaluated: Double,
            val alternativeDiscountApplied: Boolean
        ) : DecisionResult()

        data class Denied(
            val message: String,
            val adverseReasons: List<String>
        ) : DecisionResult()

        data class Error(val message: String) : DecisionResult()
    }
}
