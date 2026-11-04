package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.EmployeeEntity
import com.example.data.model.HolidayEntity
import com.example.data.repository.ApsaRoxRepository
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.StatusGreen
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ApsaRoxViewModel
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeesHolidaysScreen(viewModel: ApsaRoxViewModel) {
    var selectedSubTab by remember { mutableStateOf(0) } // 0 = Employees, 1 = Holidays
    val employees by viewModel.employees.collectAsState()
    val holidays by viewModel.holidays.collectAsState()
    val searchQuery by viewModel.employeeSearchQuery.collectAsState()
    val deptFilter by viewModel.employeeDeptFilter.collectAsState()
    val statusFilter by viewModel.employeeStatusFilter.collectAsState()

    var showEditEmployeeDialog by remember { mutableStateOf(false) }
    var editingEmployee by remember { mutableStateOf<EmployeeEntity?>(null) }

    var showAddHolidayDialog by remember { mutableStateOf(false) }
    var showImportHolidaysDialog by remember { mutableStateOf(false) }

    val departments = employees.map { it.dept }.distinct().filter { it.isNotEmpty() }
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    val filteredEmployees = employees.filter { emp ->
        val matchesQuery = searchQuery.isEmpty() ||
                emp.name.contains(searchQuery, ignoreCase = true) ||
                emp.eid.contains(searchQuery, ignoreCase = true) ||
                emp.dept.contains(searchQuery, ignoreCase = true)

        val matchesDept = deptFilter.isEmpty() || emp.dept == deptFilter
        val matchesStatus = statusFilter.isEmpty() ||
                (if (statusFilter == "Active") emp.active else !emp.active)

        matchesQuery && matchesDept && matchesStatus
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("employees_holidays_screen")
    ) {
        // Tab Header
        TabRow(selectedTabIndex = selectedSubTab) {
            Tab(
                selected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 },
                text = { Text("Staff Directory (${employees.size})") },
                icon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 },
                text = { Text("Holiday Calendar (${holidays.size})") },
                icon = { Icon(Icons.Default.Celebration, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (selectedSubTab == 0) {
            // Employees Tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Search and Filters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.employeeSearchQuery.value = it },
                            placeholder = { Text("Search by name, ID, or department…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.employeeSearchQuery.value = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("employee_search_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = deptFilter.isEmpty(),
                                onClick = { viewModel.employeeDeptFilter.value = "" },
                                label = { Text("All Depts") }
                            )
                            departments.forEach { d ->
                                FilterChip(
                                    selected = deptFilter == d,
                                    onClick = { viewModel.employeeDeptFilter.value = if (deptFilter == d) "" else d },
                                    label = { Text(d) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = statusFilter == "Active",
                                    onClick = { viewModel.employeeStatusFilter.value = if (statusFilter == "Active") "" else "Active" },
                                    label = { Text("Active") }
                                )
                                FilterChip(
                                    selected = statusFilter == "Inactive",
                                    onClick = { viewModel.employeeStatusFilter.value = if (statusFilter == "Inactive") "" else "Inactive" },
                                    label = { Text("Inactive") }
                                )
                            }

                            Button(
                                onClick = {
                                    editingEmployee = null
                                    showEditEmployeeDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("add_employee_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Staff")
                            }
                        }
                    }
                }

                // Employee Cards
                items(filteredEmployees) { emp ->
                    val offDays = ApsaRoxRepository.parseOffDays(emp.offDaysJson)
                    val offDaysText = offDays.map { dayNames.getOrElse(it) { "" } }.joinToString(", ")
                    val formattedSalary = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                        .format(emp.salary).replace(".00", "")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(emp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        Surface(
                                            color = if (emp.active) StatusGreen.copy(alpha = 0.15f) else StatusRed.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (emp.active) "ACTIVE" else "INACTIVE",
                                                color = if (emp.active) StatusGreen else StatusRed,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${emp.eid} · ${emp.dept} · ${emp.desig}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            editingEmployee = emp
                                            showEditEmployeeDialog = true
                                        }
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteEmployee(emp.id) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monthly Salary: $formattedSalary", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Standard: ${emp.hrs}h/day", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("Weekly Off: $offDaysText · Joined: ${emp.joinDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        } else {
            // Holidays Tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Annual Holidays List",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showImportHolidaysDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Import CSV")
                            }
                            Button(
                                onClick = { showAddHolidayDialog = true },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Holiday")
                            }
                        }
                    }
                }

                items(holidays) { hol ->
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
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(hol.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Surface(
                                        color = BrandPrimary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = hol.type,
                                            color = BrandPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text("Date: ${hol.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                if (hol.description.isNotEmpty()) {
                                    Text(hol.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            IconButton(onClick = { viewModel.deleteHoliday(hol.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusRed)
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Employee Dialog
    if (showEditEmployeeDialog) {
        val isEditing = editingEmployee != null
        var eid by remember { mutableStateOf(editingEmployee?.eid ?: "E00${employees.size + 1}") }
        var name by remember { mutableStateOf(editingEmployee?.name ?: "") }
        var dept by remember { mutableStateOf(editingEmployee?.dept ?: "") }
        var desig by remember { mutableStateOf(editingEmployee?.desig ?: "") }
        var joinDate by remember { mutableStateOf(editingEmployee?.joinDate ?: viewModel.getTodayString()) }
        var salaryText by remember { mutableStateOf(editingEmployee?.salary?.toInt()?.toString() ?: "30000") }
        var hoursText by remember { mutableStateOf(editingEmployee?.hrs?.toString() ?: "8.0") }
        val currentOffDays = remember {
            mutableStateListOf<Int>().apply {
                val existing = editingEmployee?.let { ApsaRoxRepository.parseOffDays(it.offDaysJson) } ?: listOf(0)
                addAll(existing)
            }
        }
        var active by remember { mutableStateOf(editingEmployee?.active ?: true) }

        AlertDialog(
            onDismissRequest = { showEditEmployeeDialog = false },
            title = { Text(if (isEditing) "Edit Employee" else "Add New Employee") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    item {
                        OutlinedTextField(value = eid, onValueChange = { eid = it }, label = { Text("Employee ID") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = dept, onValueChange = { dept = it }, label = { Text("Department (e.g. Sales, Accounts)") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = desig, onValueChange = { desig = it }, label = { Text("Designation") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = salaryText, onValueChange = { salaryText = it }, label = { Text("Monthly Salary (₹)") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        OutlinedTextField(value = hoursText, onValueChange = { hoursText = it }, label = { Text("Daily Hours") }, modifier = Modifier.fillMaxWidth())
                    }
                    item {
                        Text("Weekly Off Days", style = MaterialTheme.typography.labelMedium)
                        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            dayNames.forEachIndexed { idx, dName ->
                                FilterChip(
                                    selected = currentOffDays.contains(idx),
                                    onClick = {
                                        if (currentOffDays.contains(idx)) currentOffDays.remove(idx)
                                        else currentOffDays.add(idx)
                                    },
                                    label = { Text(dName) }
                                )
                            }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Active Status")
                            Switch(checked = active, onCheckedChange = { active = it })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sal = salaryText.toDoubleOrNull() ?: 0.0
                        val h = hoursText.toDoubleOrNull() ?: 8.0
                        if (name.isNotEmpty() && eid.isNotEmpty()) {
                            val id = editingEmployee?.id ?: ("e_" + System.currentTimeMillis())
                            val offJson = JSONArray(currentOffDays).toString()
                            viewModel.saveEmployee(
                                EmployeeEntity(
                                    id = id,
                                    eid = eid,
                                    name = name,
                                    dept = dept,
                                    desig = desig,
                                    joinDate = joinDate,
                                    salary = sal,
                                    hrs = h,
                                    offDaysJson = offJson,
                                    active = active
                                )
                            )
                            showEditEmployeeDialog = false
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditEmployeeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Holiday Dialog
    if (showAddHolidayDialog) {
        var hName by remember { mutableStateOf("") }
        var hDate by remember { mutableStateOf(viewModel.getTodayString()) }
        var hType by remember { mutableStateOf("Festival") }
        var hDesc by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddHolidayDialog = false },
            title = { Text("Add Holiday") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = hName, onValueChange = { hName = it }, label = { Text("Holiday Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hDate, onValueChange = { hDate = it }, label = { Text("Date (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hType, onValueChange = { hType = it }, label = { Text("Type (Festival, National, Company)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = hDesc, onValueChange = { hDesc = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (hName.isNotEmpty() && hDate.isNotEmpty()) {
                            viewModel.saveHoliday(
                                HolidayEntity(
                                    id = "h_" + System.currentTimeMillis(),
                                    date = hDate,
                                    name = hName,
                                    type = hType,
                                    description = hDesc
                                )
                            )
                            showAddHolidayDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddHolidayDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Import Holidays Dialog
    if (showImportHolidaysDialog) {
        var csvInput by remember { mutableStateOf("2026-11-01,Diwali,Festival\n2026-12-25,Christmas,Festival") }

        AlertDialog(
            onDismissRequest = { showImportHolidaysDialog = false },
            title = { Text("Import Holidays CSV") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("One per line: YYYY-MM-DD,Holiday Name,Type", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = csvInput,
                        onValueChange = { csvInput = it },
                        modifier = Modifier.fillMaxWidth().height(150.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.importHolidays(csvInput)
                        showImportHolidaysDialog = false
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportHolidaysDialog = false }) { Text("Cancel") }
            }
        )
    }
}
