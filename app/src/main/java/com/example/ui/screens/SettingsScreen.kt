package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.CompanySettingsEntity
import com.example.ui.theme.StatusRed
import com.example.ui.viewmodel.ApsaRoxViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: ApsaRoxViewModel) {
    val currentSettings by viewModel.settings.collectAsState()

    var companyName by remember(currentSettings) { mutableStateOf(currentSettings.companyName) }
    var standardHours by remember(currentSettings) { mutableStateOf(currentSettings.standardHours.toString()) }
    var startTime by remember(currentSettings) { mutableStateOf(currentSettings.startTime) }
    var endTime by remember(currentSettings) { mutableStateOf(currentSettings.endTime) }
    var graceMinutes by remember(currentSettings) { mutableStateOf(currentSettings.graceMinutes.toString()) }
    var lateFine by remember(currentSettings) { mutableStateOf(currentSettings.lateFine.toInt().toString()) }
    var otRate by remember(currentSettings) { mutableStateOf(currentSettings.otRate.toInt().toString()) }
    var method by remember(currentSettings) { mutableStateOf(currentSettings.method) }
    var paidLeave by remember(currentSettings) { mutableStateOf(currentSettings.paidLeave) }
    var absentPast by remember(currentSettings) { mutableStateOf(currentSettings.absentPast) }
    var nightShift by remember(currentSettings) { mutableStateOf(currentSettings.nightShift) }
    var adminPass by remember(currentSettings) { mutableStateOf(currentSettings.adminPass) }

    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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
                    text = "Company Rules & Calculations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = standardHours,
                        onValueChange = { standardHours = it },
                        label = { Text("Std Daily Hours") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = graceMinutes,
                        onValueChange = { graceMinutes = it },
                        label = { Text("Grace Period (min)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Shift Start Time") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("Shift End Time") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lateFine,
                        onValueChange = { lateFine = it },
                        label = { Text("Late Fine / Day (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = otRate,
                        onValueChange = { otRate = it },
                        label = { Text("OT Rate ₹/h (0=Auto)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Salary Calculation Method
                var methodExpanded by remember { mutableStateOf(false) }
                val methodOptions = listOf(
                    "working" to "Monthly Salary ÷ Working Days",
                    "calendar" to "Monthly Salary ÷ Calendar Days",
                    "30" to "Monthly Salary ÷ 30"
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { methodExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(methodOptions.find { it.first == method }?.second ?: method)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = methodExpanded, onDismissRequest = { methodExpanded = false }) {
                        methodOptions.forEach { (mKey, mLabel) ->
                            DropdownMenuItem(
                                text = { Text(mLabel) },
                                onClick = {
                                    method = mKey
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                HorizontalDivider()

                // Toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Paid Leave", fontWeight = FontWeight.SemiBold)
                        Text("Count approved leaves as paid days", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = paidLeave, onCheckedChange = { paidLeave = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Past Days Absent Rule", fontWeight = FontWeight.SemiBold)
                        Text("Mark days with no record as Absent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = absentPast, onCheckedChange = { absentPast = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Night Shifts", fontWeight = FontWeight.SemiBold)
                        Text("Check-out can be past midnight", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = nightShift, onCheckedChange = { nightShift = it })
                }

                OutlinedTextField(
                    value = adminPass,
                    onValueChange = { adminPass = it },
                    label = { Text("Admin PIN / Password") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        viewModel.saveSettings(
                            CompanySettingsEntity(
                                id = 1,
                                companyName = companyName,
                                standardHours = standardHours.toDoubleOrNull() ?: 8.0,
                                startTime = startTime,
                                endTime = endTime,
                                graceMinutes = graceMinutes.toIntOrNull() ?: 15,
                                lateFine = lateFine.toDoubleOrNull() ?: 0.0,
                                otRate = otRate.toDoubleOrNull() ?: 0.0,
                                method = method,
                                paidLeave = paidLeave,
                                absentPast = absentPast,
                                nightShift = nightShift,
                                adminPass = adminPass
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_settings_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Rules & Settings")
                }
            }
        }

        // End-to-End Encryption & Security Vault
        E2eeSecuritySection(viewModel = viewModel)

        // Demo Data Reset Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Demo Data Management", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    "Reset the database with complete pre-seeded employees (Rahul Mehta, Priya Shah, Amit Patel), September 2026 logs, loans, and holidays.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedButton(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Reset to Demo Seed Data")
                }
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset to Demo Data?") },
            text = { Text("This will restore default employees, holidays, loans, and September 2026 attendance records.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetToDemo()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusRed)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
