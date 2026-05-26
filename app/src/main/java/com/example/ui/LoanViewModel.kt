package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LoanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LoanRepository

    // Database reactive UI streams
    val userProfileFlow: StateFlow<UserProfile?>
    val bankConnectionFlow: StateFlow<BankConnection?>
    val creditProfileFlow: StateFlow<CreditProfile?>
    val loanApplicationsFlow: StateFlow<List<LoanApplication>>

    // Transient UI Screen Flow states
    val activeStep = MutableStateFlow(1) // 1: KYC, 2: Credit, 3: Bank Connect, 4: Underwriting Decision, 5: App History
    val activeDecisionResult = MutableStateFlow<LendingSimulator.DecisionResult?>(null)
    
    // Slider state for approved loan calculations
    val requestedLoanAmount = MutableStateFlow(5000.0)
    val chosenLoanTermMonths = MutableStateFlow(36)

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LoanRepository(database.loanDao())

        userProfileFlow = repository.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        bankConnectionFlow = repository.bankConnection.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        creditProfileFlow = repository.creditProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

        loanApplicationsFlow = repository.loanApplications.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // Process KYC form submission
    fun verifyKycAndSave(
        firstName: String,
        lastName: String,
        dob: String,
        ssn: String,
        address: String,
        city: String,
        state: String,
        zip: String,
        income: Double
    ) {
        viewModelScope.launch {
            val maskedSsn = "XXX-XX-" + ssn.takeLast(4)
            val profile = UserProfile(
                firstName = firstName,
                lastName = lastName,
                dob = dob,
                ssnMasked = maskedSsn,
                rawSsn = ssn,
                streetAddress = address,
                city = city,
                state = state,
                zipCode = zip,
                annualIncome = income,
                isKycVerified = true
            )
            repository.saveUserProfile(profile)
            // Progress to next step in the flow
            activeStep.value = 2
            
            // Re-evaluate decision result if there's pre-existing data
            evaluateDecision()
        }
    }

    // Pull Simulated Bureau Credit Report
    fun pullSimulatedCreditReport() {
        val currentProfile = userProfileFlow.value ?: return
        viewModelScope.launch {
            val report = LendingSimulator.getCreditBureauReport(
                firstName = currentProfile.firstName,
                lastName = currentProfile.lastName,
                ssn = currentProfile.rawSsn
            )
            repository.saveCreditProfile(report)
            activeStep.value = 3
            
            evaluateDecision()
        }
    }

    // Connect Simulated Bank using Open Banking OAuth details
    fun connectSimulatedBank(institutionName: String) {
        val currentProfile = userProfileFlow.value ?: return
        viewModelScope.launch {
            val bankConn = LendingSimulator.getSimulatedBankConnection(
                institution = institutionName,
                clientIncome = currentProfile.annualIncome
            )
            repository.saveBankConnection(bankConn)
            activeStep.value = 4
            
            evaluateDecision()
        }
    }

    // Run Credit Decision Engine manually
    fun evaluateDecision() {
        val profile = userProfileFlow.value ?: return
        val bureau = creditProfileFlow.value
        val bank = bankConnectionFlow.value
        
        if (bureau == null) {
            activeDecisionResult.value = null
            return
        }

        val result = LendingSimulator.runCreditDecisionEngine(profile, bureau, bank)
        activeDecisionResult.value = result

        // Adjust requested loan state based on maximum offer allowed if approved
        if (result is LendingSimulator.DecisionResult.Approved) {
            requestedLoanAmount.value = result.maxSum.coerceAtMost(10000.0) // Default starting request of 10k or max
            chosenLoanTermMonths.value = result.termMonths
        }
    }

    // Action to confirm and submit active approved loan contract
    fun confirmAndOriginateSimulatedLoan(loanPurpose: String) {
        val result = activeDecisionResult.value as? LendingSimulator.DecisionResult.Approved ?: return
        viewModelScope.launch {
            val amount = requestedLoanAmount.value
            val term = chosenLoanTermMonths.value
            val apr = result.baseApr
            
            // Standard compound loan payment calculation
            val monthlyRate = (apr / 100.0) / 12.0
            val monthlyPayment = if (monthlyRate > 0) {
                (amount * monthlyRate) / (1.0 - Math.pow(1.0 + monthlyRate, -term.toDouble()))
            } else {
                amount / term
            }
            val totalCost = (monthlyPayment * term) - amount

            val application = LoanApplication(
                loanPurpose = loanPurpose,
                requestedAmount = amount,
                approvedAmount = amount,
                apr = apr,
                termMonths = term,
                monthlyPayment = monthlyPayment,
                totalCost = totalCost,
                status = "ACTIVE"
            )
            repository.submitLoanApplication(application)
            activeStep.value = 5 // Go to History list
        }
    }

    // Clean restart application wizard
    fun resetWizardFlow() {
        viewModelScope.launch {
            repository.clearAllData()
            activeStep.value = 1
            activeDecisionResult.value = null
            requestedLoanAmount.value = 5000.0
            chosenLoanTermMonths.value = 36
        }
    }

    // Quick skip helper to navigate between headers
    fun navigateToStep(step: Int) {
        activeStep.value = step
    }
}
