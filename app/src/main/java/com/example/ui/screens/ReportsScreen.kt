package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.repository.ApsaRoxRepository
import com.example.ui.viewmodel.ApsaRoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ApsaRoxViewModel) {
    val context = LocalContext.current
    val employees by viewModel.employees.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val attendanceRecords by viewModel.currentMonthAttendance.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val reportType by viewModel.reportType.collectAsState()
    val fromDate by viewModel.reportFromDate.collectAsState()
    val toDate by viewModel.reportToDate.collectAsState()
    val deptFilter by viewModel.reportDeptFilter.collectAsState()

    val reportTypes = listOf(
        "monthly" to "Monthly Summary",
        "daily" to "Daily Log",
        "employee" to "Employee Detailed",
        "overtime" to "Overtime Analysis",
        "late" to "Late Attendance",
        "absence" to "Absence Audit",
        "salary" to "Salary Register"
    )

    val activeEmployees = employees.filter { it.active && (deptFilter.isEmpty() || it.dept == deptFilter) }
    val todayDate = viewModel.getTodayString()

    // Build tabular report data
    val (headers, rows) = remember(reportType, fromDate, toDate, activeEmployees, attendanceRecords, holidays, settings) {
        val y = viewModel.selectedYear.value
        val m = viewModel.selectedMonth.value
        val attMap = attendanceRecords.associateBy { "${it.employeeId}|${it.date}" }

        when (reportType) {
            "daily" -> {
                val h = listOf("EID", "Name", "Date", "IN", "OUT", "Work (h)", "OT (h)", "Status", "Notes")
                val r = activeEmployees.map { emp ->
                    val rec = attMap["${emp.id}|$fromDate"]
                    val info = ApsaRoxRepository(com.example.data.db.AppDatabase.getInstance(context)).calculateDayInfo(
                        emp, fromDate, rec, holidays, settings, todayDate
                    )
                    listOf(
                        emp.eid,
                        emp.name,
                        fromDate,
                        info.ins.joinToString(", ").ifEmpty { "—" },
                        info.outs.joinToString(", ").ifEmpty { "—" },
                        ApsaRoxRepository.minutesToTime(info.workMinutes),
                        ApsaRoxRepository.minutesToTime(info.otMinutes),
                        info.status,
                        info.notes
                    )
                }
                Pair(h, r)
            }
            "overtime" -> {
                val h = listOf("EID", "Name", "Working Days", "Total Work", "OT Hours", "OT Rate (₹)", "OT Amount (₹)")
                val r = activeEmployees.map { emp ->
                    val (summary, _) = ApsaRoxRepository(com.example.data.db.AppDatabase.getInstance(context)).calculateMonthSummary(
                        emp, y, m, attendanceRecords.filter { it.employeeId == emp.id }.associateBy { it.date }, holidays, settings, todayDate
                    )
                    val sal = ApsaRoxRepository(com.example.data.db.AppDatabase.getInstance(context)).calculateSalary(
                        emp, y, m, summary, viewModel.loans.value, viewModel.advances.value, settings
                    )
                    listOf(
                        emp.eid,
                        emp.name,
                        "${summary.workingDays}",
                        "${ApsaRoxRepository.minutesToTime(summary.workMinutes)}h",
                        "${ApsaRoxRepository.minutesToTime(summary.otMinutes)}h",
                        String.format("%.1f", sal.otRatePerHour),
                        String.format("%.0f", sal.otAmount)
                    )
                }.filter { it[4] != "00:00h" || reportType == "overtime" }
                Pair(h, r)
            }
            "late" -> {
                val h = listOf("EID", "Name", "Late Days", "Late Minutes", "Fine Rate (₹)", "Deduction (₹)")
                val r = activeEmployees.map { emp ->
                    val (summary, _) = ApsaRoxRepository(com.example.data.db.AppDatabase.getInstance(context)).calculateMonthSummary(
                        emp, y, m, attendanceRecords.filter { it.employeeId == emp.id }.associateBy { it.date }, holidays, settings, todayDate
                    )
                    val fine = summary.lateDays * settings.lateFine
                    listOf(
                        emp.eid,
                        emp.name,
                        "${summary.lateDays}",
                        "${summary.lateMinutesTotal}m",
                        "${settings.lateFine}",
                        String.format("%.0f", fine)
                    )
                }
                Pair(h, r)
            }
            "absence" -> {
                val h = listOf("EID", "Name", "Total Days", "Present", "Absent", "Leave", "Att %")
                val r = activeEmployees.map { emp ->
                    val (summary, _) = ApsaRoxRepository(com.example.data.db.AppDatabase.getInstance(context)).calculateMonthSummary(
                        emp, y, m, attendanceRecords.filter { it.employeeId == emp.id }.associateBy { it.date }, holidays, settings, todayDate
                    )
                    listOf(
                        emp.eid,
                        emp.name,
                        "${summary.workingDays}",
                        "${summary.presentDays + summary.wfhDays}",
                        "${summary.absentDays}",
                        "${summary.leaveDays}",
                        String.format("%.1f%%", summary.attendancePct)
                    )
                }
                Pair(h, r)
            }
            "salary" -> {
                val h = listOf("EID", "Name", "Base Salary", "Payable", "Earned", "OT (+)", "Deduct (-)", "Net Pay")
                val r = activeEmployees.map { emp ->
                    val (summary, _) = ApsaRoxRepository(com.example.data.db.AppDatabase.getInstance(context)).calculateMonthSummary(
                        emp, y, m, attendanceRecords.filter { it.employeeId == emp.id }.associateBy { it.date }, holidays, settings, todayDate
                    )
                    val sal = ApsaRoxRepository(com.example.data.db.AppDatabase.getInstance(context)).calculateSalary(
                        emp, y, m, summary, viewModel.loans.value, viewModel.advances.value, settings
                    )
                    val totalDeduct = sal.lateDeduction + sal.loanEmi + sal.advanceDeduction
                    listOf(
                        emp.eid,
                        emp.name,
                        "₹${emp.salary.toInt()}",
                        "${sal.payableDays}d",
                        "₹${sal.earnedSalary.toInt()}",
                        "₹${sal.otAmount.toInt()}",
                        "₹${totalDeduct.toInt()}",
                        "₹${sal.netSalary.toInt()}"
                    )
                }
                Pair(h, r)
            }
            else -> {
                // monthly summary default
                val h = listOf("EID", "Name", "Working", "Present", "Absent", "Half", "Leave", "Off", "Work (h)", "OT (h)", "Att %")
                val r = activeEmployees.map { emp ->
                    val (summary, _) = ApsaRoxRepository(com.example.data.db.AppDatabase.getInstance(context)).calculateMonthSummary(
                        emp, y, m, attendanceRecords.filter { it.employeeId == emp.id }.associateBy { it.date }, holidays, settings, todayDate
                    )
                    listOf(
                        emp.eid,
                        emp.name,
                        "${summary.workingDays}",
                        "${summary.presentDays + summary.wfhDays}",
                        "${summary.absentDays}",
                        "${summary.halfDays}",
                        "${summary.leaveDays}",
                        "${summary.weeklyOffDays}",
                        "${ApsaRoxRepository.minutesToTime(summary.workMinutes)}",
                        "${ApsaRoxRepository.minutesToTime(summary.otMinutes)}",
                        String.format("%.1f%%", summary.attendancePct)
                    )
                }
                Pair(h, r)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Filter Controls Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Generate Business Reports",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Report Type Dropdown
                    var typeExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { typeExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(reportTypes.find { it.first == reportType }?.second ?: "Select Report")
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                            reportTypes.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.reportType.value = key
                                        typeExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Date Pickers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fromDate,
                            onValueChange = { viewModel.reportFromDate.value = it },
                            label = { Text("From Date") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = toDate,
                            onValueChange = { viewModel.reportToDate.value = it },
                            label = { Text("To Date") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // Export / Share Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val currentTitle = reportTypes.find { it.first == reportType }?.second ?: "Business"
                                val pdf = com.example.util.PdfReportGenerator.generateCustomReportPdf(
                                    context = context,
                                    companyName = settings.companyName,
                                    reportTitle = currentTitle,
                                    dateRange = "$fromDate to $toDate",
                                    headers = headers,
                                    rows = rows
                                )
                                com.example.util.PdfReportGenerator.openOrSharePdf(context, pdf, "Share $currentTitle PDF Report")
                                viewModel.showNotification("Generated $currentTitle PDF Report")
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.weight(1.2f).testTag("generate_pdf_report_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.width(6.dp))
                            Text("Generate PDF", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                val csv = buildString {
                                    append(headers.joinToString(",") { "\"$it\"" } + "\n")
                                    rows.forEach { row ->
                                        append(row.joinToString(",") { "\"$it\"" } + "\n")
                                    }
                                }
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, csv)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Report CSV"))
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("CSV")
                        }
                    }
                }
            }
        }

        // Table Preview
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Report Preview (${rows.size} Records)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    // Horizontal scrolling table
                    Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        Column {
                            // Header row
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                headers.forEach { h ->
                                    Text(
                                        text = h,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.widthIn(min = 80.dp)
                                    )
                                }
                            }
                            HorizontalDivider()

                            // Data rows
                            rows.forEach { row ->
                                Row(
                                    modifier = Modifier
                                        .padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    row.forEach { cell ->
                                        Text(
                                            text = cell,
                                            fontSize = 12.sp,
                                            modifier = Modifier.widthIn(min = 80.dp)
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
