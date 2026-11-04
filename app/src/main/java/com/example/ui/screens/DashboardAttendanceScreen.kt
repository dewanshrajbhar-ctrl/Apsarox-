package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayInfo
import com.example.data.model.EmployeeEntity
import com.example.data.repository.ApsaRoxRepository
import com.example.ui.theme.*
import com.example.ui.viewmodel.ApsaRoxViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAttendanceScreen(viewModel: ApsaRoxViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val employees by viewModel.employees.collectAsState()
    val selectedEmpId by viewModel.selectedEmployeeId.collectAsState()
    val selectedYear by viewModel.selectedYear.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val viewMode by viewModel.attendanceViewMode.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val currentEmployee = employees.find { it.id == selectedEmpId } ?: employees.firstOrNull()
    val (monthSummary, dayInfos) = viewModel.getDayInfoListForSelectedEmployee()
    val todayDate = viewModel.getTodayString()
    val todayInfo = dayInfos.find { it.date == todayDate }

    var editingDayInfo by remember { mutableStateOf<DayInfo?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }

    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_attendance_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Filter Bar: Month, Year & Employee Selector
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
                        text = "Attendance Management",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Dropdown
                        var monthMenuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { monthMenuExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("month_selector_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(monthNames.getOrElse(selectedMonth - 1) { "Month" })
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                            DropdownMenu(
                                expanded = monthMenuExpanded,
                                onDismissRequest = { monthMenuExpanded = false }
                            ) {
                                monthNames.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            viewModel.selectedMonth.value = index + 1
                                            monthMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year input/selector
                        var yearMenuExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.width(110.dp)) {
                            OutlinedButton(
                                onClick = { yearMenuExpanded = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("year_selector_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$selectedYear")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                            DropdownMenu(
                                expanded = yearMenuExpanded,
                                onDismissRequest = { yearMenuExpanded = false }
                            ) {
                                listOf(2025, 2026, 2027).forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text("$yr") },
                                        onClick = {
                                            viewModel.selectedYear.value = yr
                                            yearMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Employee Dropdown
                    var empMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { empMenuExpanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("employee_selector_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Person", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = currentEmployee?.let { "${it.eid} · ${it.name} (${it.dept})" } ?: "Select Employee",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                        DropdownMenu(
                            expanded = empMenuExpanded,
                            onDismissRequest = { empMenuExpanded = false }
                        ) {
                            employees.forEach { emp ->
                                DropdownMenuItem(
                                    text = { Text("${emp.eid} · ${emp.name} (${emp.dept})") },
                                    onClick = {
                                        viewModel.selectedEmployeeId.value = emp.id
                                        empMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Punch & Today Status Hero Card
        if (currentEmployee != null) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = currentEmployee.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentEmployee.eid} · ${currentEmployee.desig} · Today: $todayDate",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            todayInfo?.let { info ->
                                StatusBadge(status = info.status)
                            }
                        }

                        // Punches log & Work time
                        todayInfo?.let { info ->
                            val punchText = if (info.punches.isNotEmpty()) {
                                info.punches.mapIndexed { idx, p ->
                                    (if (idx % 2 == 0) "IN " else "OUT ") + p
                                }.joinToString(" · ")
                            } else "No punches yet today"

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Log: $punchText", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "Work: ${ApsaRoxRepository.minutesToTime(info.workMinutes)}h · OT: ${ApsaRoxRepository.minutesToTime(info.otMinutes)}h" +
                                                    if (info.lateMinutes > 0) " · Late: ${info.lateMinutes}m" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Big Punch Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.punch(currentEmployee.id, true) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("punch_in_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = StatusGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Check In")
                                Spacer(Modifier.width(6.dp))
                                Text("Check IN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.punch(currentEmployee.id, false) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("punch_out_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC2410C)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Check Out")
                                Spacer(Modifier.width(6.dp))
                                Text("Check OUT", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Quick overrides
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editingDayInfo = todayInfo ?: DayInfo(
                                        date = todayDate,
                                        dayOfWeek = 0,
                                        punches = emptyList(),
                                        ins = emptyList(),
                                        outs = emptyList(),
                                        breakMin = 60,
                                        status = "Pending",
                                        workMinutes = 0,
                                        otMinutes = 0,
                                        shortMinutes = 0,
                                        lateMinutes = 0,
                                        earlyMinutes = 0,
                                        holidayName = null,
                                        isOffDay = false,
                                        notes = ""
                                    )
                                    showEditDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Edit Today")
                            }

                            OutlinedButton(
                                onClick = { viewModel.markStatus(currentEmployee.id, todayDate, "Leave") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Mark Leave")
                            }

                            OutlinedButton(
                                onClick = { viewModel.markStatus(currentEmployee.id, todayDate, "Holiday") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Mark Holiday")
                            }

                            OutlinedButton(
                                onClick = { viewModel.markStatus(currentEmployee.id, todayDate, "Absent") },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Mark Absent")
                            }

                            OutlinedButton(
                                onClick = { viewModel.clearAttendanceDay(currentEmployee.id, todayDate) },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed)
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }
        }

        // Monthly Summary KPI Grid
        if (monthSummary != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Monthly Performance (${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val kpis = listOf(
                        Triple("Working Days", "${monthSummary.workingDays}", MaterialTheme.colorScheme.primary),
                        Triple("Present", "${monthSummary.presentDays + monthSummary.wfhDays}", StatusGreen),
                        Triple("Absent", "${monthSummary.absentDays}", StatusRed),
                        Triple("Leave", "${monthSummary.leaveDays}", StatusAmber),
                        Triple("Overtime", "${ApsaRoxRepository.minutesToTime(monthSummary.otMinutes)}h", StatusBlue),
                        Triple("Attendance", "${String.format("%.1f", monthSummary.attendancePct)}%", BrandPrimary)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        kpis.take(3).forEach { (label, value, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        kpis.drop(3).forEach { (label, value, color) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
                                }
                            }
                        }
                    }
                }
            }
        }

        // View Mode Switch: Calendar vs Table & PDF Export
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Records",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            if (currentEmployee != null && monthSummary != null) {
                                val pdf = com.example.util.PdfReportGenerator.generateAttendancePdf(
                                    context = context,
                                    companyName = settings.companyName,
                                    employee = currentEmployee,
                                    monthSummary = monthSummary,
                                    dayInfos = dayInfos,
                                    year = selectedYear,
                                    month = selectedMonth
                                )
                                com.example.util.PdfReportGenerator.openOrSharePdf(context, pdf, "Share Attendance PDF")
                                viewModel.showNotification("Generated Attendance PDF for ${currentEmployee.name}")
                            } else {
                                viewModel.showNotification("Please select an employee first", true)
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("export_attendance_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(4.dp))
                        Text("PDF Sheet", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    FilterChip(
                        selected = viewMode == "table",
                        onClick = { viewModel.attendanceViewMode.value = "table" },
                        label = { Text("Table", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = "Table", modifier = Modifier.size(14.dp)) }
                    )
                    FilterChip(
                        selected = viewMode == "calendar",
                        onClick = { viewModel.attendanceViewMode.value = "calendar" },
                        label = { Text("Calendar", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Calendar", modifier = Modifier.size(14.dp)) }
                    )
                }
            }
        }

        // Content: Calendar or Table
        if (viewMode == "calendar") {
            item {
                AttendanceCalendarView(
                    year = selectedYear,
                    month = selectedMonth,
                    dayInfos = dayInfos,
                    onDayClick = { dayInfo ->
                        editingDayInfo = dayInfo
                        showEditDialog = true
                    }
                )
            }
        } else {
            // Table items
            items(dayInfos) { day ->
                AttendanceTableRowCard(
                    dayInfo = day,
                    onEditClick = {
                        editingDayInfo = day
                        showEditDialog = true
                    }
                )
            }
        }
    }

    // Edit Attendance Dialog
    if (showEditDialog && editingDayInfo != null && currentEmployee != null) {
        AttendanceEditDialog(
            employeeName = currentEmployee.name,
            dayInfo = editingDayInfo!!,
            onDismiss = { showEditDialog = false },
            onSave = { punches, breakMin, status, notes ->
                viewModel.saveAttendanceRecord(
                    employeeId = currentEmployee.id,
                    date = editingDayInfo!!.date,
                    punchesList = punches,
                    breakMin = breakMin,
                    manualStatus = status,
                    notes = notes
                )
                showEditDialog = false
            }
        )
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor, emoji) = when (status) {
        "Present" -> Triple(StatusGreenBg, StatusGreen, "✅")
        "WFH" -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), "🏠")
        "Absent" -> Triple(StatusRedBg, StatusRed, "❌")
        "Half Day" -> Triple(StatusAmberBg, StatusAmber, "◐")
        "Leave" -> Triple(StatusBlueBg, StatusBlue, "🌴")
        "Holiday" -> Triple(Color(0xFFEDE9FE), Color(0xFF6D28D9), "🎉")
        "Sunday", "Weekly Off" -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "🛌")
        "Missing IN", "Missing OUT" -> Triple(StatusAmberBg, StatusAmber, "⚠️")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), "⏳")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(99.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, textColor.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(emoji, fontSize = 12.sp)
            Text(status, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun AttendanceCalendarView(
    year: Int,
    month: Int,
    dayInfos: List<DayInfo>,
    onDayClick: (DayInfo) -> Unit
) {
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val cal = Calendar.getInstance().apply { set(year, month - 1, 1) }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val dayMap = dayInfos.associateBy { it.date.split("-").last().toIntOrNull() ?: 0 }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Day Names Header
            Row(modifier = Modifier.fillMaxWidth()) {
                dayNames.forEach { name ->
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Calendar Grid: rows of 7
            var currentDay = 1
            var totalCells = firstDayOfWeek + daysInMonth
            val rows = Math.ceil(totalCells / 7.0).toInt()

            for (r in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        if (cellIndex < firstDayOfWeek || currentDay > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val d = currentDay
                            val info = dayMap[d]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(68.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (info?.status == "Present" || info?.status == "WFH") StatusGreenBg.copy(alpha = 0.3f)
                                        else if (info?.status == "Absent") StatusRedBg.copy(alpha = 0.3f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .clickable { info?.let { onDayClick(it) } }
                                    .padding(4.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "$d",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                    info?.let {
                                        Text(
                                            text = it.status.take(7),
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (it.workMinutes > 0) {
                                            Text(
                                                text = "${ApsaRoxRepository.minutesToTime(it.workMinutes)}",
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                            currentDay++
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun AttendanceTableRowCard(
    dayInfo: DayInfo,
    onEditClick: () -> Unit
) {
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val dateParts = dayInfo.date.split("-")
    val displayDate = if (dateParts.size == 3) "${dateParts[2]}/${dateParts[1]}" else dayInfo.date

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.2f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(displayDate, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(dayNames.getOrElse(dayInfo.dayOfWeek) { "" }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }

                val inStr = if (dayInfo.ins.isNotEmpty()) dayInfo.ins.joinToString(", ") else "—"
                val outStr = if (dayInfo.outs.isNotEmpty()) dayInfo.outs.joinToString(", ") else "—"
                Text("IN: $inStr · OUT: $outStr", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (dayInfo.notes.isNotEmpty()) {
                    Text("Note: ${dayInfo.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                StatusBadge(status = dayInfo.status)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Work: ${ApsaRoxRepository.minutesToTime(dayInfo.workMinutes)}h" +
                            if (dayInfo.otMinutes > 0) " (+${ApsaRoxRepository.minutesToTime(dayInfo.otMinutes)} OT)" else "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Day", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun AttendanceEditDialog(
    employeeName: String,
    dayInfo: DayInfo,
    onDismiss: () -> Unit,
    onSave: (punches: List<String>, breakMin: Int, status: String, notes: String) -> Unit
) {
    var punchesText by remember { mutableStateOf(dayInfo.punches.joinToString(", ")) }
    var breakText by remember { mutableStateOf(dayInfo.breakMin.toString()) }
    var selectedStatus by remember { mutableStateOf(if (dayInfo.status in listOf("Sunday", "Weekly Off", "Pending", "No Record", "Missing IN", "Missing OUT")) "Auto" else dayInfo.status) }
    var notesText by remember { mutableStateOf(dayInfo.notes) }

    val statusOptions = listOf("Auto", "Present", "Absent", "Half Day", "Leave", "WFH", "Holiday")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$employeeName · ${dayInfo.date}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = punchesText,
                    onValueChange = { punchesText = it },
                    label = { Text("Punches (HH:MM, comma separated)") },
                    placeholder = { Text("09:00, 18:00") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = breakText,
                    onValueChange = { breakText = it },
                    label = { Text("Break (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )

                var statusExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { statusExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Status: $selectedStatus")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statusOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedStatus = opt
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pList = punchesText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    val bMin = breakText.toIntOrNull() ?: 0
                    val finalStatus = if (selectedStatus == "Auto") "" else selectedStatus
                    onSave(pList, bMin, finalStatus, notesText)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
