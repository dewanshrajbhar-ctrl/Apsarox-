package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdvanceEntity
import com.example.data.model.LoanEntity
import com.example.data.model.SalarySummary
import com.example.data.repository.ApsaRoxRepository
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ApsaRoxViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayrollFinanceScreen(viewModel: ApsaRoxViewModel) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsState()
    val loans by viewModel.loans.collectAsState()
    val advances by viewModel.advances.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val salarySummaries = viewModel.getSalarySummaries()
    val totalNetPayable = salarySummaries.sumOf { it.netSalary }

    var selectedSalarySlip by remember { mutableStateOf<SalarySummary?>(null) }
    var showAddLoanDialog by remember { mutableStateOf(false) }
    var showAddAdvanceDialog by remember { mutableStateOf(false) }

    fun formatInr(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return format.format(amount).replace(".00", "")
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("payroll_finance_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Total Net Payable Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL NET PAYABLE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = formatInr(totalNetPayable),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear · ${salarySummaries.size} Active Employees",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val pdf = com.example.util.PdfReportGenerator.generatePayrollRegisterPdf(
                                    context = context,
                                    companyName = settings.companyName,
                                    year = selectedYear,
                                    month = selectedMonth,
                                    salarySummaries = salarySummaries
                                )
                                com.example.util.PdfReportGenerator.openOrSharePdf(context, pdf, "Share Payroll Register PDF")
                                viewModel.showNotification("Generated Payroll Register PDF")
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1.1f).testTag("export_payroll_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(4.dp))
                            Text("PDF Register", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val csvContent = buildString {
                                    append("EID,Name,Monthly Salary,Working Days,Present,Payable Days,Earned,OT Amount,Late Fine,Loan EMI,Advance,Net Salary\n")
                                    salarySummaries.forEach { s ->
                                        append("${s.employee.eid},\"${s.employee.name}\",${s.employee.salary},${s.monthSummary.workingDays},${s.monthSummary.presentDays},${s.payableDays},${s.earnedSalary},${s.otAmount},${s.lateDeduction},${s.loanEmi},${s.advanceDeduction},${s.netSalary}\n")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, csvContent)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Payroll CSV"))
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CSV", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showAddLoanDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Loan", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Loan", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showAddAdvanceDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Advance", modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(2.dp))
                            Text("Adv", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Section Title: Employee Salary Calculations
        item {
            Text(
                text = "Employee Salary Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(salarySummaries) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = item.employee.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "${item.employee.eid} · ${item.employee.dept} · Base: ${formatInr(item.employee.salary)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { selectedSalarySlip = item },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Payslip", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Payable Days", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${item.payableDays} of ${item.monthSummary.workingDays} days", fontSize = 13.sp)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Earned Base", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatInr(item.earnedSalary), fontSize = 13.sp)
                    }

                    if (item.otAmount > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Overtime (${ApsaRoxRepository.minutesToTime(item.monthSummary.otMinutes)}h)", fontSize = 13.sp, color = StatusGreen)
                            Text("+${formatInr(item.otAmount)}", fontSize = 13.sp, color = StatusGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (item.lateDeduction > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Late Fine (${item.monthSummary.lateDays} days)", fontSize = 13.sp, color = StatusRed)
                            Text("-${formatInr(item.lateDeduction)}", fontSize = 13.sp, color = StatusRed)
                        }
                    }

                    if (item.loanEmi > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Loan EMI", fontSize = 13.sp, color = StatusRed)
                            Text("-${formatInr(item.loanEmi)}", fontSize = 13.sp, color = StatusRed)
                        }
                    }

                    if (item.advanceDeduction > 0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Advance Recovery", fontSize = 13.sp, color = StatusRed)
                            Text("-${formatInr(item.advanceDeduction)}", fontSize = 13.sp, color = StatusRed)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("NET PAYABLE", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(formatInr(item.netSalary), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Section: Active Loans
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Loans & EMIs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddLoanDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Loan")
                }
            }
        }

        if (loans.isEmpty()) {
            item {
                Text("No active loans recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(loans) { loan ->
                val emp = employees.find { it.id == loan.employeeId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(emp?.name ?: "Employee", fontWeight = FontWeight.Bold)
                            Text("Loan: ${formatInr(loan.amount)} · EMI: ${formatInr(loan.emi)}/mo", fontSize = 12.sp)
                            Text("Starts: ${loan.startMonth} · ${loan.note}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.deleteLoan(loan.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Loan", tint = StatusRed)
                        }
                    }
                }
            }
        }

        // Section: Advance Payments
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Advance Payments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddAdvanceDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Advance")
                }
            }
        }

        if (advances.isEmpty()) {
            item {
                Text("No advance payments recorded", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(advances) { adv ->
                val emp = employees.find { it.id == adv.employeeId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(emp?.name ?: "Employee", fontWeight = FontWeight.Bold)
                            Text("Amount: ${formatInr(adv.amount)} · Date: ${adv.date}", fontSize = 12.sp)
                            if (adv.note.isNotEmpty()) {
                                Text("Note: ${adv.note}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteAdvance(adv.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Advance", tint = StatusRed)
                        }
                    }
                }
            }
        }
    }

    // Payslip Modal Dialog
    selectedSalarySlip?.let { slip ->
        AlertDialog(
            onDismissRequest = { selectedSalarySlip = null },
            title = {
                Text("Salary Slip · ${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${slip.employee.name} (${slip.employee.eid})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Department: ${slip.employee.dept} · ${slip.employee.desig}", fontSize = 13.sp)
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Base Monthly Salary:")
                        Text(formatInr(slip.employee.salary), fontWeight = FontWeight.SemiBold)
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Per Day Rate:")
                        Text(formatInr(slip.perDaySalary))
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Payable Days:")
                        Text("${slip.payableDays} days")
                    }
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Earned Salary:")
                        Text(formatInr(slip.earnedSalary))
                    }
                    if (slip.otAmount > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Overtime Pay:")
                            Text("+${formatInr(slip.otAmount)}", color = StatusGreen)
                        }
                    }
                    if (slip.lateDeduction > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Late Deductions:")
                            Text("-${formatInr(slip.lateDeduction)}", color = StatusRed)
                        }
                    }
                    if (slip.loanEmi > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Loan EMI:")
                            Text("-${formatInr(slip.loanEmi)}", color = StatusRed)
                        }
                    }
                    if (slip.advanceDeduction > 0) {
                        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                            Text("Advance Deduction:")
                            Text("-${formatInr(slip.advanceDeduction)}", color = StatusRed)
                        }
                    }
                    HorizontalDivider()
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("FINAL NET PAY:", fontWeight = FontWeight.Bold)
                        Text(formatInr(slip.netSalary), fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, fontSize = 16.sp)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val pdf = com.example.util.PdfReportGenerator.generateSalarySlipPdf(
                                context = context,
                                companyName = settings.companyName,
                                year = selectedYear,
                                month = selectedMonth,
                                salarySummary = slip
                            )
                            com.example.util.PdfReportGenerator.openOrSharePdf(context, pdf, "Share Payslip PDF - ${slip.employee.name}")
                            viewModel.showNotification("Generated PDF Payslip for ${slip.employee.name}")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Text("PDF Slip")
                    }

                    OutlinedButton(onClick = { selectedSalarySlip = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Add Loan Dialog
    if (showAddLoanDialog) {
        var empId by remember { mutableStateOf(employees.firstOrNull()?.id ?: "") }
        var amountText by remember { mutableStateOf("") }
        var emiText by remember { mutableStateOf("") }
        var startMonth by remember { mutableStateOf(String.format("%04d-%02d", selectedYear, selectedMonth)) }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddLoanDialog = false },
            title = { Text("New Employee Loan") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var empExpanded by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { empExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(employees.find { it.id == empId }?.name ?: "Select Employee")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = empExpanded, onDismissRequest = { empExpanded = false }) {
                            employees.forEach { e ->
                                DropdownMenuItem(
                                    text = { Text("${e.eid} · ${e.name}") },
                                    onClick = { empId = e.id; empExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Loan Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = emiText,
                        onValueChange = { emiText = it },
                        label = { Text("Monthly EMI (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = startMonth,
                        onValueChange = { startMonth = it },
                        label = { Text("First EMI Month (YYYY-MM)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        val emi = emiText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && emi > 0 && empId.isNotEmpty()) {
                            viewModel.saveLoan(
                                LoanEntity(
                                    id = "l_" + System.currentTimeMillis(),
                                    employeeId = empId,
                                    amount = amt,
                                    emi = emi,
                                    startMonth = startMonth,
                                    note = noteText
                                )
                            )
                            showAddLoanDialog = false
                        }
                    }
                ) {
                    Text("Save Loan")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddLoanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Advance Dialog
    if (showAddAdvanceDialog) {
        var empId by remember { mutableStateOf(employees.firstOrNull()?.id ?: "") }
        var amountText by remember { mutableStateOf("") }
        var dateText by remember { mutableStateOf(viewModel.getTodayString()) }
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddAdvanceDialog = false },
            title = { Text("New Advance Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var empExpanded by remember { mutableStateOf(false) }
                    Box(Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { empExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(employees.find { it.id == empId }?.name ?: "Select Employee")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = empExpanded, onDismissRequest = { empExpanded = false }) {
                            employees.forEach { e ->
                                DropdownMenuItem(
                                    text = { Text("${e.eid} · ${e.name}") },
                                    onClick = { empId = e.id; empExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Advance Amount (₹)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0 && empId.isNotEmpty()) {
                            viewModel.saveAdvance(
                                AdvanceEntity(
                                    id = "a_" + System.currentTimeMillis(),
                                    employeeId = empId,
                                    date = dateText,
                                    amount = amt,
                                    note = noteText
                                )
                            )
                            showAddAdvanceDialog = false
                        }
                    }
                ) {
                    Text("Save Advance")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddAdvanceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
