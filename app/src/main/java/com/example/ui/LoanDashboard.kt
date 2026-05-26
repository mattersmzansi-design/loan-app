package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LendingSimulator
import com.example.data.LendingSimulator.DecisionResult
import com.example.data.UserProfile
import com.example.data.CreditProfile
import com.example.data.BankConnection
import com.example.data.LoanApplication
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.ui.theme.*
import java.text.DecimalFormat

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoanDashboardScreen(viewModel: LoanViewModel) {
    val activeStep by viewModel.activeStep.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfileFlow.collectAsStateWithLifecycle()
    val bankConnection by viewModel.bankConnectionFlow.collectAsStateWithLifecycle()
    val creditProfile by viewModel.creditProfileFlow.collectAsStateWithLifecycle()
    val loanApplications by viewModel.loanApplicationsFlow.collectAsStateWithLifecycle()
    val decisionResult by viewModel.activeDecisionResult.collectAsStateWithLifecycle()

    val currencyFormat = remember { DecimalFormat("$#,##0.00") }
    val scoreFormat = remember { DecimalFormat("###") }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkSurface, DarkBaseBg)
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "LOAN BUILDER",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.testTag("brand_token")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Lending & Underwriting Hub",
                            color = TextPureWhite,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { viewModel.resetWizardFlow() },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBorder)
                            .testTag("reset_all_state_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset State Data",
                            tint = DarkAccentAlert
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Elegant M3 Navigation Bar aligned for quick tab shifting
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeStep in 1..4,
                    onClick = { if (activeStep > 4) viewModel.navigateToStep(4) },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Active Application") },
                    label = { Text("Application") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = TextMutedGrey,
                        unselectedTextColor = TextMutedGrey
                    )
                )
                NavigationBarItem(
                    selected = activeStep == 5,
                    onClick = { viewModel.navigateToStep(5) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Active Portfolio") },
                    label = { Text("My Portfolio") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary,
                        unselectedIconColor = TextMutedGrey,
                        unselectedTextColor = TextMutedGrey
                    )
                )
            }
        },
        containerColor = DarkBaseBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Step Wizard Bar (Progress Indicator)
            StepIndicatorSection(
                activeStep = activeStep,
                onStepClicked = { targetStep ->
                    // Guard so users can't navigate to steps for which they haven't completed prereqs
                    var ok = false
                    when (targetStep) {
                        1 -> ok = true
                        2 -> if (userProfile != null) ok = true
                        3 -> if (userProfile != null && creditProfile != null) ok = true
                        4 -> if (userProfile != null && creditProfile != null) ok = true
                        5 -> ok = true
                    }
                    if (ok) viewModel.navigateToStep(targetStep)
                }
            )

            HorizontalDivider(color = DarkBorder, modifier = Modifier.padding(horizontal = 20.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Main Animation frame switching based on current state step
            AnimatedContent(
                targetState = activeStep,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "step_animation"
            ) { step ->
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    when (step) {
                        1 -> KycFormSection(
                            userProfile = userProfile,
                            onSubmit = { f, l, d, s, ad, c, st, z, inc ->
                                viewModel.verifyKycAndSave(f, l, d, s, ad, c, st, z, inc)
                            }
                        )
                        2 -> CreditPullSection(
                            userProfile = userProfile,
                            creditProfile = creditProfile,
                            onPull = { viewModel.pullSimulatedCreditReport() }
                        )
                        3 -> BankOAuthSection(
                            bankConnection = bankConnection,
                            onConnect = { school -> viewModel.connectSimulatedBank(school) },
                            userProfile = userProfile
                        )
                        4 -> UnderwritingSection(
                            userProfile = userProfile,
                            creditProfile = creditProfile,
                            bankConnection = bankConnection,
                            decisionResult = decisionResult,
                            requestedAmountFlow = viewModel.requestedLoanAmount,
                            chosenTermFlow = viewModel.chosenLoanTermMonths,
                            onApply = { purpose -> viewModel.confirmAndOriginateSimulatedLoan(purpose) },
                            onCheckAlternative = { viewModel.evaluateDecision() }
                        )
                        5 -> PortfolioSection(
                            loanApplications = loanApplications,
                            userProfile = userProfile,
                            bankConnection = bankConnection,
                            creditProfile = creditProfile,
                            onReset = { viewModel.resetWizardFlow() }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Subcomponent: Animated Step Indicators for our multi-step wizard
@Composable
fun StepIndicatorSection(
    activeStep: Int,
    onStepClicked: (Int) -> Unit
) {
    val steps = listOf("KYC", "Credit", "Bank Link", "Underwrite", "History")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, name ->
            val stepNum = index + 1
            val isActive = activeStep == stepNum
            val isCompleted = activeStep > stepNum

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onStepClicked(stepNum) }
                    .testTag("step_pill_$stepNum")
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                isActive -> EmeraldPrimary
                                isCompleted -> DarkBorder
                                else -> DarkSurface
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) EmeraldPrimary else DarkBorder,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = stepNum.toString(),
                            color = if (isActive) Color.Black else TextMutedGrey,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = name,
                    fontSize = 10.sp,
                    color = if (isActive) EmeraldPrimary else if (isCompleted) TextPureWhite else TextMutedGrey,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }

            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(0.4f)
                        .background(
                            if (activeStep > stepNum) EmeraldPrimary else DarkBorder
                        )
                )
            }
        }
    }
}

// STEP 1: Identity & KYC Form
@Composable
fun KycFormSection(
    userProfile: UserProfile?,
    onSubmit: (String, String, String, String, String, String, String, String, Double) -> Unit
) {
    var firstName by remember { mutableStateOf(userProfile?.firstName ?: "Robert") }
    var lastName by remember { mutableStateOf(userProfile?.lastName ?: "Patterson") }
    var dob by remember { mutableStateOf(userProfile?.dob ?: "1988-10-14") }
    var ssn by remember { mutableStateOf(userProfile?.rawSsn ?: "666459005") } // Default last digit 5 is Near Prime
    var address by remember { mutableStateOf(userProfile?.streetAddress ?: "104 Lighthill Rd") }
    var city by remember { mutableStateOf(userProfile?.city ?: "Seattle") }
    var state by remember { mutableStateOf(userProfile?.state ?: "WA") }
    var zip by remember { mutableStateOf(userProfile?.zipCode ?: "98101") }
    var incomeInput by remember { mutableStateOf(userProfile?.annualIncome?.toString() ?: "72000") }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("kyc_info_form")
    ) {
        Text(
            text = "Step 1: Identity & KYC Verification",
            color = TextPureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Enter verified legal details to enable real-time credit checks and verify your risk profile.",
            color = TextMutedGrey,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = { firstName = it },
                        label = { Text("First Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DarkBorder,
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = TextMutedGrey,
                            unfocusedTextColor = TextPureWhite,
                            focusedTextColor = TextPureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .testTag("first_name_input")
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = { Text("Last Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DarkBorder,
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = TextMutedGrey,
                            unfocusedTextColor = TextPureWhite,
                            focusedTextColor = TextPureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("last_name_input")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Date of Birth (YYYY-MM-DD)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DarkBorder,
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = TextMutedGrey,
                            unfocusedTextColor = TextPureWhite,
                            focusedTextColor = TextPureWhite
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(end = 8.dp)
                            .testTag("dob_input")
                    )
                    OutlinedTextField(
                        value = ssn,
                        onValueChange = { ssn = it },
                        label = { Text("9-Digit SSN / TIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DarkBorder,
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = TextMutedGrey,
                            unfocusedTextColor = TextPureWhite,
                            focusedTextColor = TextPureWhite
                        ),
                        placeholder = { Text("666459005") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ssn_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "*Tip: Change the last digit of SSN to simulate bureau differences (9 = 810 Score, 5 = 640 Score, 3 = 580 Score, 1 = 490 Score).",
                    color = TextMutedTeal,
                    fontSize = 11.sp,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Street Address") },
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = DarkBorder,
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = TextMutedGrey,
                        unfocusedTextColor = TextPureWhite,
                        focusedTextColor = TextPureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("address_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("City") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DarkBorder,
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = TextMutedGrey,
                            unfocusedTextColor = TextPureWhite,
                            focusedTextColor = TextPureWhite
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .padding(end = 6.dp)
                            .testTag("city_input")
                    )
                    OutlinedTextField(
                        value = state,
                        onValueChange = { state = it },
                        label = { Text("State") },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DarkBorder,
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = TextMutedGrey,
                            unfocusedTextColor = TextPureWhite,
                            focusedTextColor = TextPureWhite
                        ),
                        modifier = Modifier
                            .weight(0.6f)
                            .padding(end = 6.dp)
                            .testTag("state_input")
                    )
                    OutlinedTextField(
                        value = zip,
                        onValueChange = { zip = it },
                        label = { Text("ZIP") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = DarkBorder,
                            focusedBorderColor = EmeraldPrimary,
                            focusedLabelColor = EmeraldPrimary,
                            unfocusedLabelColor = TextMutedGrey,
                            unfocusedTextColor = TextPureWhite,
                            focusedTextColor = TextPureWhite
                        ),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("zip_input")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = incomeInput,
                    onValueChange = { incomeInput = it },
                    label = { Text("Verified Annual Income ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = DarkBorder,
                        focusedBorderColor = EmeraldPrimary,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = TextMutedGrey,
                        unfocusedTextColor = TextPureWhite,
                        focusedTextColor = TextPureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("annual_income_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                val isReady = firstName.isNotEmpty() && lastName.isNotEmpty() && ssn.length >= 9 && incomeInput.toDoubleOrNull() != null
                Button(
                    onClick = {
                        keyboardController?.hide()
                        onSubmit(
                            firstName,
                            lastName,
                            dob,
                            ssn,
                            address,
                            city,
                            state,
                            zip,
                            incomeInput.toDoubleOrNull() ?: 50000.0
                        )
                    },
                    enabled = isReady,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_kyc_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        disabledContainerColor = DarkBorder
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Verify Identity & Save Profile",
                        fontWeight = FontWeight.Bold,
                        color = if (isReady) Color.Black else TextMutedGrey
                    )
                }
            }
        }
    }
}

// STEP 2: Credit Bureau Pull
@Composable
fun CreditPullSection(
    userProfile: UserProfile?,
    creditProfile: CreditProfile?,
    onPull: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("credit_bureau_screen")
    ) {
        Text(
            text = "Step 2: Credit Bureau Profile Pull",
            color = TextPureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "A credit bureau report is required under local FCRA laws to evaluate trade lines, active debts, and public records.",
            color = TextMutedGrey,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Consent notice
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Consent Safety",
                            tint = EmeraldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Fair Credit Reporting Act (FCRA) Consent",
                            color = TextPureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Secured using 256-bit Equifax Partner authorization Protocol",
                            color = TextMutedTeal,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "By clicking 'Pull Credit Bureau Record' below, you provide written instructions to Loan Builder under the Federal Fair Credit Reporting Act authorizing us to obtain information from your personal consumer report profile held with Equifax, Experian, or TransUnion.",
                    color = TextMutedGrey,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onPull() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("pull_credit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Record",
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Authorized Call: Pull Bureau Record",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                if (creditProfile != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "CRedential Pull Successful",
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.testTag("credit_success_pill")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bureau Summary Display Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBaseBg)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Simulated Bureau Score", color = TextMutedGrey, fontSize = 11.sp)
                            Text(
                                text = creditProfile.creditScore.toString() + " FICO 8",
                                color = TextPureWhite,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Category: ${creditProfile.ratingGroup}",
                                color = TextMutedTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Circular Progress Representation
                        Box(
                            modifier = Modifier.size(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { (creditProfile.creditScore - 300) / 550f },
                                modifier = Modifier.fillMaxSize(),
                                color = EmeraldPrimary,
                                strokeWidth = 6.dp,
                                trackColor = DarkBorder,
                            )
                            Text(
                                text = creditProfile.creditScore.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPureWhite,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Technical JSON report inspection viewport (simulated integrations look professional here)
                    Text(
                        text = "RAW JSON Response payload (Regulatory Audit):",
                        color = TextMutedGrey,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = creditProfile.reportJson,
                            color = Color(0xFF00FFCC),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}

// STEP 3: Open Banking Bank Linking Card
@Composable
fun BankOAuthSection(
    bankConnection: com.example.data.BankConnection?,
    onConnect: (String) -> Unit,
    userProfile: UserProfile?
) {
    var showOAuthSimulator by remember { mutableStateOf(false) }
    var selectedBankName by remember { mutableStateOf("") }

    val majorBanks = listOf(
        "Chase" to Icons.Default.Home,
        "Wells Fargo" to Icons.Default.Star,
        "Bank of America" to Icons.Default.Check,
        "Citibank" to Icons.Default.Info,
        "Capital One" to Icons.Default.PlayArrow
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bank_link_screen")
    ) {
        Text(
            text = "Step 3: Open Banking Link (Standard §1033 / PSD2)",
            color = TextPureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Securely verify income, cash flow, and monthly balance reserves using real-time institution ledger feeds.",
            color = TextMutedGrey,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Select your Financial Institution",
                    color = TextPureWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                // List of selectable institutions
                majorBanks.forEach { (bankName, icon) ->
                    val isCurrent = bankConnection?.isConnected == true && bankConnection.institutionName == bankName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) DarkBorder else DarkBaseBg)
                            .border(
                                1.dp,
                                if (isCurrent) EmeraldPrimary else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                selectedBankName = bankName
                                showOAuthSimulator = true
                            }
                            .padding(14.dp)
                            .testTag("selector_${bankName.replace(" ", "_").lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = bankName,
                                tint = if (isCurrent) EmeraldPrimary else TextMutedGrey
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = bankName,
                            color = TextPureWhite,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Connect",
                            tint = if (isCurrent) EmeraldPrimary else TextMutedGrey,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (bankConnection?.isConnected == true) {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = DarkBorder)
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Connected Bank Account Details:",
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.testTag("bank_success_header")
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBaseBg)
                            .padding(14.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Institution", color = TextMutedGrey, fontSize = 12.sp)
                            Text(bankConnection.institutionName, color = TextPureWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Account Number", color = TextMutedGrey, fontSize = 12.sp)
                            Text(bankConnection.accountNumberMasked, color = TextPureWhite, fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ledger Balance", color = TextMutedGrey, fontSize = 12.sp)
                            Text("$" + String.format("%.2f", bankConnection.balance), color = EmeraldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Modal Simulated Link Portal OAuth screen representation (extremely realistic)
    if (showOAuthSimulator) {
        AlertDialog(
            onDismissRequest = { showOAuthSimulator = false },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Link",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Secure OAUTH Link Account",
                        color = TextPureWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "You are connecting Loan Builder to $selectedBankName secure open banking API endpoint.",
                        color = TextMutedGrey,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBaseBg)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Permitted Scopes Authorized:",
                                color = TextPureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("• read.account.identity (Verify Owner matching KYC)", color = TextMutedTeal, fontSize = 11.sp)
                            Text("• read.account.balances (Verify Ledger volume)", color = TextMutedTeal, fontSize = 11.sp)
                            Text("• read.account.transactions (Verify Cash Flow consistency)", color = TextMutedTeal, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No login credentials will be stored by Loan Builder. Everything is encrypted via TLS 1.3.",
                        color = TextMutedGrey,
                        fontSize = 11.sp,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConnect(selectedBankName)
                        showOAuthSimulator = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("bank_oauth_connect_button")
                ) {
                    Text("Authorize & Connect Link", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOAuthSimulator = false }) {
                    Text("Cancel", color = TextMutedGrey)
                }
            }
        )
    }
}

// STEP 4: Decision Room & Dynamic Slider or Adverse action report
@Composable
fun UnderwritingSection(
    userProfile: UserProfile?,
    creditProfile: CreditProfile?,
    bankConnection: com.example.data.BankConnection?,
    decisionResult: DecisionResult?,
    requestedAmountFlow: MutableStateFlow<Double>,
    chosenTermFlow: MutableStateFlow<Int>,
    onApply: (String) -> Unit,
    onCheckAlternative: () -> Unit
) {
    val requestedAmount by requestedAmountFlow.collectAsStateWithLifecycle()
    val chosenTerm by chosenTermFlow.collectAsStateWithLifecycle()

    var activePurpose by remember { mutableStateOf("Debt Consolidation") }
    var userAcceptedTermsCheck by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("underwriting_decide_screen")
    ) {
        Text(
            text = "Step 4: Credit Underwriting Decision Room",
            color = TextPureWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "System decision engine processes bureau score and cash flow logs to state credit terms or regulatory adverse rejection letters.",
            color = TextMutedGrey,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Switch states
        when (decisionResult) {
            null -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Pending Evaluation",
                            tint = TextMutedGrey,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Decision Engine Unlinked",
                            color = TextPureWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Text(
                            text = "Please complete the Identity (Step 1) and Bureau (Step 2) profile links before running evaluation.",
                            color = TextMutedGrey,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { onCheckAlternative() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Text("Run Check Underwrite", color = Color.Black)
                        }
                    }
                }
            }

            is DecisionResult.Denied -> {
                // REGULATION B ADVERSE REJECTION CARD
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkAccentAlert),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Decision Denied",
                                tint = DarkAccentAlert,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "REGULATORY ADVERSE ACTION NOTICE",
                                color = DarkAccentAlert,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.testTag("adverse_title_rejection")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Thank you for your application for credit. We regret that we are unable to approve your application at this time. Our credit decision was based, in whole or in part, on the following indicators retrieved:",
                            color = TextPureWhite,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Adverse action points list
                        decisionResult.adverseReasons.forEach { reason ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "•",
                                    color = DarkAccentAlert,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = reason,
                                    color = TextPureWhite,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkBaseBg)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "YOUR RIGHT TO COPIES & DISPUTING RECORDS",
                                color = TextMutedTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Under section 615 of the Fair Credit Reporting Act (FCRA), you have the right to request a free copy of your credit disclosure from our provider within 60 days. You also maintain legal rights to dispute the accuracy or completeness of any information included in consumer profiles directly with the reporting bureaus.",
                                color = TextMutedGrey,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Alternative Tips",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Want to bypass? Restart custom profile with high SSN score (e.g. end SSN with digit 9) or double gross annual incomes to clear lending barriers.",
                                color = TextMutedTeal,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            is DecisionResult.Approved -> {
                // APPROVED OFFER DYNAMIC SLIDERS / CUSTOMIZER CARD
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EmeraldPrimary.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, EmeraldPrimary),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Offer Approved",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "CONGRATULATIONS: APPROVED OFFER",
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    modifier = Modifier.testTag("approval_tag")
                                )
                                Text(
                                    text = "Tier Rating: ${decisionResult.tier}",
                                    color = TextPureWhite,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Customize Your Loan Terms",
                                color = TextPureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            // Loan Amount Slider Setup
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Requested Credit Line", color = TextMutedGrey, fontSize = 13.sp)
                                Text(
                                    text = "$${requestedAmount.toInt()}",
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp,
                                    modifier = Modifier.testTag("loan_amount_display")
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = requestedAmount.toFloat(),
                                onValueChange = { requestedAmountFlow.value = it.toDouble() },
                                valueRange = 1000f..decisionResult.maxSum.toFloat(),
                                steps = 0,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("loan_amount_slider"),
                                colors = SliderDefaults.colors(
                                    thumbColor = EmeraldPrimary,
                                    activeTrackColor = EmeraldPrimary,
                                    inactiveTrackColor = DarkBorder
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("$1,000", color = TextMutedGrey, fontSize = 11.sp)
                                Text("Approved Limit: $${decisionResult.maxSum.toInt()}", color = TextMutedTeal, fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Term selection buttons
                            Text("Repayment Duration Term", color = TextMutedGrey, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val terms = listOf(12, 24, 36, 48, 60).filter { it <= decisionResult.termMonths }
                                terms.forEach { termOpt ->
                                    val isSelected = chosenTerm == termOpt
                                    OutlinedButton(
                                        onClick = { chosenTermFlow.value = termOpt },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                            .testTag("term_button_$termOpt"),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isSelected) EmeraldPrimary else Color.Transparent,
                                            contentColor = if (isSelected) Color.Black else TextPureWhite
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) EmeraldPrimary else DarkBorder
                                        ),
                                        contentPadding = PaddingValues(vertical = 10.dp)
                                    ) {
                                        Text("${termOpt} mo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Real-time compound monthly rates displays
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkBaseBg)
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Approved APR Basis", color = TextMutedGrey, fontSize = 13.sp)
                                    Text("${decisionResult.baseApr}%", color = TextPureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                if (decisionResult.alternativeDiscountApplied) {
                                    Text(
                                        "• 1.50% Open Banking discount automatically applied!",
                                        color = EmeraldPrimary,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = DarkBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Dynamic Compound payment
                                val monthlyRate = (decisionResult.baseApr / 100.0) / 12.0
                                val payment = if (monthlyRate > 0) {
                                    (requestedAmount * monthlyRate) / (1.0 - Math.pow(1.0 + monthlyRate, -chosenTerm.toDouble()))
                                } else {
                                    requestedAmount / chosenTerm
                                }
                                val costOfCredit = (payment * chosenTerm) - requestedAmount

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Estimated Monthly Installment", color = TextPureWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        "$" + String.format("%.2f", payment),
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        modifier = Modifier.testTag("monthly_payment_rate")
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Interest Cost of Credit", color = TextMutedGrey, fontSize = 12.sp)
                                    Text("$" + String.format("%.2f", costOfCredit), color = TextPureWhite, fontSize = 13.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Debt To Income (Evaluated)", color = TextMutedGrey, fontSize = 12.sp)
                                    Text(String.format("%.1f%%", decisionResult.dtiEvaluated * 100), color = TextMutedTeal, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text("Select Credit Purpose", color = TextMutedGrey, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val purposes = listOf("Debt Consolidation", "Home Project", "Business Improvement")
                                purposes.forEach { prp ->
                                    val isCurrent = activePurpose == prp
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) EmeraldPrimary.copy(alpha = 0.15f) else DarkBaseBg)
                                            .border(1.dp, if (isCurrent) EmeraldPrimary else DarkBorder, RoundedCornerShape(8.dp))
                                            .clickable { activePurpose = prp }
                                            .padding(10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = prp,
                                            fontSize = 11.sp,
                                            color = if (isCurrent) EmeraldPrimary else TextMutedGrey,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // Confirm regulatory checkbox
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { userAcceptedTermsCheck = !userAcceptedTermsCheck },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = userAcceptedTermsCheck,
                                    onCheckedChange = { userAcceptedTermsCheck = it },
                                    colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary),
                                    modifier = Modifier.testTag("accept_terms_checkbox")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I state that checking statements provided are true and authorized under state regulations.",
                                    color = TextMutedGrey,
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { onApply(activePurpose) },
                                enabled = userAcceptedTermsCheck,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    disabledContainerColor = DarkBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("originate_loan_button")
                            ) {
                                Icon(Icons.Default.Star, contentDescription = "Draw Loan", tint = if (userAcceptedTermsCheck) Color.Black else TextMutedGrey)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Request Approved Loan & Disburse Fund",
                                    fontWeight = FontWeight.Bold,
                                    color = if (userAcceptedTermsCheck) Color.Black else TextMutedGrey
                                )
                            }
                        }
                    }
                }
            }

            is DecisionResult.Error -> {
                Text(
                    text = decisionResult.message,
                    color = DarkAccentAlert,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

// STEP 5: active loan applications / submission history
@Composable
fun PortfolioSection(
    loanApplications: List<com.example.data.LoanApplication>,
    userProfile: UserProfile?,
    bankConnection: com.example.data.BankConnection?,
    creditProfile: CreditProfile?,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("portfolio_dashboard")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Credit & Loan Portfolio",
                    color = TextPureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Consolidated credit profile and authorized loan disbursements.",
                    color = TextMutedGrey,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (loanApplications.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, DarkBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No Loans",
                        tint = TextMutedGrey,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Portfolio is Empty",
                        color = TextPureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Connect credit profile, link bank account ledger statements and select an approved term to activate loans.",
                        color = TextMutedGrey,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            // Outstanding Loan overview summary
            val totalOutstanding = loanApplications.filter { it.status == "ACTIVE" }.sumOf { it.approvedAmount }
            val monthlyTotal = loanApplications.filter { it.status == "ACTIVE" }.sumOf { it.monthlyPayment }

            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "LOANS OUTSTANDING BALANCE",
                        color = EmeraldPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$" + String.format("%,.2f", totalOutstanding),
                        color = TextPureWhite,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.testTag("lifetime_borrow_total")
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Combined Monthly Installments", color = TextMutedGrey, fontSize = 12.sp)
                        Text("$" + String.format("%.2f", monthlyTotal), color = TextPureWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }

            Text(
                text = "Authorized Loan Disbursements:",
                color = TextPureWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 12.dp, top = 4.dp)
            )

            loanApplications.forEach { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("loan_card_item")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = app.loanPurpose.uppercase(),
                                    color = EmeraldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$" + String.format("%,.2f", app.approvedAmount),
                                    color = TextPureWhite,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = app.status,
                                    color = EmeraldPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DarkBorder)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Monthly payment", color = TextMutedGrey, fontSize = 11.sp)
                                Text("$" + String.format("%.2f", app.monthlyPayment), color = TextPureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("APR", color = TextMutedGrey, fontSize = 11.sp)
                                Text("${app.apr}%", color = EmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Repay Duration", color = TextMutedGrey, fontSize = 11.sp)
                                Text("${app.termMonths} Months", color = TextPureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Credit Profile Connected:",
                    color = TextPureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bureau Score", color = TextMutedGrey, fontSize = 12.sp)
                    Text(
                        (creditProfile?.creditScore?.toString() ?: "N/A") + " FICO",
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bank connection", color = TextMutedGrey, fontSize = 12.sp)
                    Text(
                        bankConnection?.institutionName ?: "Not Connected",
                        color = if (bankConnection?.isConnected == true) EmeraldPrimary else DarkAccentAlert,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onReset() },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkAccentAlert.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Session Data", color = DarkAccentAlert, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
