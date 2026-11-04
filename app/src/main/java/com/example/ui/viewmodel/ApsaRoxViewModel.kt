package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AudioRecorderManager
import com.example.ai.GeminiService
import com.example.ai.RecordingState
import com.example.data.db.AppDatabase
import com.example.data.model.*
import com.example.data.repository.ApsaRoxRepository
import com.example.util.crypto.CryptoManager
import com.example.util.crypto.EncryptedBackupPackage
import com.example.util.crypto.SecurityAuditInfo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class AppNavTab(val title: String, val iconName: String) {
    DASHBOARD("Attendance", "event_available"),
    PAYROLL("Payroll", "payments"),
    EMPLOYEES("Staff & Off", "group"),
    REPORTS("Reports", "analytics"),
    GEMINI_AI("Gemini AI", "auto_awesome"),
    SETTINGS("Rules", "tune")
}

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class ApsaRoxViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ApsaRoxRepository(AppDatabase.getInstance(application))
    val audioRecorderManager = AudioRecorderManager(application)

    // Current navigation tab
    val currentTab = MutableStateFlow(AppNavTab.DASHBOARD)

    // Filter states
    val selectedYear = MutableStateFlow(2026)
    val selectedMonth = MutableStateFlow(9) // September 2026 default to match sample data
    val selectedEmployeeId = MutableStateFlow<String?>("e0")
    val attendanceViewMode = MutableStateFlow("table") // "table" or "calendar"
    val employeeSearchQuery = MutableStateFlow("")
    val employeeDeptFilter = MutableStateFlow("")
    val employeeStatusFilter = MutableStateFlow("")

    // Reports filter
    val reportType = MutableStateFlow("monthly") // daily, monthly, employee, overtime, late, absence, salary
    val reportFromDate = MutableStateFlow("2026-09-01")
    val reportToDate = MutableStateFlow("2026-09-30")
    val reportDeptFilter = MutableStateFlow("")
    val reportStatusFilter = MutableStateFlow("")

    // Notification toast / snackbar
    val notification = MutableStateFlow<UiNotification?>(null)

    // DB flows
    val employees = repository.employees.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val holidays = repository.holidays.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val loans = repository.loans.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val advances = repository.advances.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val settings = repository.settings.map { it ?: CompanySettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, CompanySettingsEntity())

    private val monthPrefix = combine(selectedYear, selectedMonth) { y, m ->
        String.format("%04d-%02d", y, m)
    }

    val currentMonthAttendance = monthPrefix.flatMapLatest { prefix ->
        repository.getAllMonthAttendance(prefix)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Today's date string YYYY-MM-DD
    fun getTodayString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Audio & Gemini AI State
    val audioRecordingState = audioRecorderManager.recordingState
    val isTranscribing = MutableStateFlow(false)
    val transcriptionResult = MutableStateFlow("")
    val aiPromptInput = MutableStateFlow("")
    val aiResponseText = MutableStateFlow("")
    val isAiGenerating = MutableStateFlow(false)
    val selectedAiModel = MutableStateFlow(GeminiService.MODEL_GENERAL)

    fun showNotification(message: String, isError: Boolean = false) {
        notification.value = UiNotification(message, isError)
    }

    fun clearNotification() {
        notification.value = null
    }

    // Punch IN / OUT
    fun punch(employeeId: String, isCheckIn: Boolean) {
        viewModelScope.launch {
            val result = repository.punch(employeeId, isCheckIn)
            result.onSuccess {
                val empName = employees.value.find { it.id == employeeId }?.name ?: "Employee"
                val action = if (isCheckIn) "Check IN" else "Check OUT"
                showNotification("$action recorded for $empName")
            }.onFailure {
                showNotification(it.message ?: "Punch failed", true)
            }
        }
    }

    // Mark status directly (e.g. Leave, Holiday, Absent)
    fun markStatus(employeeId: String, date: String, status: String) {
        viewModelScope.launch {
            val current = currentMonthAttendance.value.find { it.employeeId == employeeId && it.date == date }
                ?: AttendanceEntity(employeeId = employeeId, date = date)
            val updated = current.copy(manualStatus = status)
            repository.updateAttendanceRecord(updated)
            showNotification("Status marked as $status for $date")
        }
    }

    // Clear attendance day
    fun clearAttendanceDay(employeeId: String, date: String) {
        viewModelScope.launch {
            repository.deleteAttendanceRecord(employeeId, date)
            showNotification("Cleared attendance record for $date")
        }
    }

    // Edit attendance record
    fun saveAttendanceRecord(
        employeeId: String,
        date: String,
        punchesList: List<String>,
        breakMin: Int,
        manualStatus: String,
        notes: String
    ) {
        viewModelScope.launch {
            val punchesJson = JSONArray(punchesList).toString()
            val record = AttendanceEntity(
                employeeId = employeeId,
                date = date,
                punchesJson = punchesJson,
                breakMinutes = breakMin,
                manualStatus = manualStatus,
                notes = notes
            )
            repository.updateAttendanceRecord(record)
            showNotification("Updated record for $date")
        }
    }

    // Employee CRUD
    fun saveEmployee(employee: EmployeeEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateEmployee(employee)
            showNotification("Saved employee ${employee.name}")
        }
    }

    fun deleteEmployee(employeeId: String) {
        viewModelScope.launch {
            repository.deleteEmployee(employeeId)
            showNotification("Employee deleted")
            if (selectedEmployeeId.value == employeeId) {
                selectedEmployeeId.value = employees.value.firstOrNull { it.id != employeeId }?.id
            }
        }
    }

    // Holiday CRUD
    fun saveHoliday(holiday: HolidayEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateHoliday(holiday)
            showNotification("Holiday saved: ${holiday.name}")
        }
    }

    fun deleteHoliday(id: String) {
        viewModelScope.launch {
            repository.deleteHoliday(id)
            showNotification("Holiday removed")
        }
    }

    fun importHolidays(csvLines: String) {
        viewModelScope.launch {
            var count = 0
            val lines = csvLines.split("\n")
            for (line in lines) {
                val parts = line.split(",").map { it.trim() }
                if (parts.size >= 2 && parts[0].matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
                    val date = parts[0]
                    val name = parts[1]
                    val type = if (parts.size >= 3 && parts[2].isNotEmpty()) parts[2] else "Festival"
                    val id = "h_" + System.currentTimeMillis() + "_" + count
                    repository.insertOrUpdateHoliday(
                        HolidayEntity(id = id, date = date, name = name, type = type)
                    )
                    count++
                }
            }
            if (count > 0) {
                showNotification("Imported $count holiday(s)")
            } else {
                showNotification("No valid holiday lines found in format: YYYY-MM-DD,Name,Type", true)
            }
        }
    }

    // Loan & Advance CRUD
    fun saveLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateLoan(loan)
            showNotification("Loan saved for employee")
        }
    }

    fun deleteLoan(id: String) {
        viewModelScope.launch {
            repository.deleteLoan(id)
            showNotification("Loan deleted")
        }
    }

    fun saveAdvance(advance: AdvanceEntity) {
        viewModelScope.launch {
            repository.insertOrUpdateAdvance(advance)
            showNotification("Advance payment recorded")
        }
    }

    fun deleteAdvance(id: String) {
        viewModelScope.launch {
            repository.deleteAdvance(id)
            showNotification("Advance record deleted")
        }
    }

    // Settings
    fun saveSettings(updated: CompanySettingsEntity) {
        viewModelScope.launch {
            repository.saveSettings(updated)
            showNotification("Settings updated")
        }
    }

    fun resetToDemo() {
        viewModelScope.launch {
            repository.resetToDemoData()
            showNotification("Reset all data to demo defaults")
        }
    }

    // End-to-End Encryption (E2EE) State & Backups
    val securityAuditInfo = MutableStateFlow(CryptoManager.getSecurityAuditInfo())
    val isE2eeActive = MutableStateFlow(true)
    val lastBackupFile = MutableStateFlow<File?>(null)
    val lastBackupChecksum = MutableStateFlow<String?>(null)
    val isExportingBackup = MutableStateFlow(false)
    val isRestoringBackup = MutableStateFlow(false)
    val lastRestoreResult = MutableStateFlow<RestoreResult?>(null)
    val availableBackups = MutableStateFlow<List<File>>(emptyList())

    init {
        refreshBackupsList()
    }

    fun refreshBackupsList() {
        val app = getApplication<Application>()
        val dir = app.getExternalFilesDir("backups") ?: app.filesDir
        val files = dir.listFiles { _, name -> name.endsWith(".apsarox.enc") }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()
        availableBackups.value = files
    }

    fun exportEncryptedBackup(passphrase: String, onComplete: (File?, String?, String?) -> Unit) {
        if (passphrase.length < 4) {
            showNotification("Passphrase must be at least 4 characters", true)
            onComplete(null, null, "Passphrase must be at least 4 characters")
            return
        }
        viewModelScope.launch {
            isExportingBackup.value = true
            try {
                val pkg = repository.exportEncryptedBackup(passphrase)
                val app = getApplication<Application>()
                val dir = app.getExternalFilesDir("backups") ?: app.filesDir
                if (!dir.exists()) dir.mkdirs()

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(dir, "apsarox_backup_${timeStamp}.apsarox.enc")
                file.writeText(pkg.toJson(), Charsets.UTF_8)

                lastBackupFile.value = file
                lastBackupChecksum.value = pkg.sha256Checksum
                refreshBackupsList()
                showNotification("E2EE Encrypted backup saved: ${file.name}")
                onComplete(file, pkg.sha256Checksum, null)
            } catch (e: Exception) {
                showNotification("Export failed: ${e.message}", true)
                onComplete(null, null, e.message)
            } finally {
                isExportingBackup.value = false
            }
        }
    }

    fun restoreEncryptedBackup(
        file: File,
        passphrase: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            isRestoringBackup.value = true
            try {
                val content = file.readText(Charsets.UTF_8)
                val result = repository.restoreEncryptedBackup(content, passphrase)
                result.onSuccess { restored ->
                    lastRestoreResult.value = restored
                    showNotification("E2EE Restore Success: ${restored.employeeCount} staff, ${restored.attendanceCount} logs")
                    onComplete(true, "Successfully restored ${restored.employeeCount} staff and ${restored.attendanceCount} records.")
                }.onFailure { err ->
                    showNotification("Restore error: ${err.message}", true)
                    onComplete(false, err.message ?: "Decryption or integrity verification failed.")
                }
            } catch (e: Exception) {
                showNotification("Restore error: ${e.message}", true)
                onComplete(false, e.message ?: "File read error.")
            } finally {
                isRestoringBackup.value = false
            }
        }
    }

    fun restoreEncryptedBackupFromText(
        payloadJson: String,
        passphrase: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            isRestoringBackup.value = true
            try {
                val result = repository.restoreEncryptedBackup(payloadJson.trim(), passphrase)
                result.onSuccess { restored ->
                    lastRestoreResult.value = restored
                    showNotification("E2EE Restore Success: ${restored.employeeCount} staff, ${restored.attendanceCount} logs")
                    onComplete(true, "Successfully restored ${restored.employeeCount} staff and ${restored.attendanceCount} records.")
                }.onFailure { err ->
                    showNotification("Restore error: ${err.message}", true)
                    onComplete(false, err.message ?: "Decryption or integrity verification failed.")
                }
            } catch (e: Exception) {
                showNotification("Restore error: ${e.message}", true)
                onComplete(false, e.message ?: "Invalid backup payload format.")
            } finally {
                isRestoringBackup.value = false
            }
        }
    }

    // Audio Transcription with gemini-3.5-transcribe
    fun startAudioRecording() {
        val result = audioRecorderManager.startRecording()
        result.onFailure {
            showNotification("Microphone recording error: ${it.message}", true)
        }
    }

    fun stopAudioRecordingAndTranscribe() {
        val file = audioRecorderManager.stopRecording()
        if (file == null || !file.exists() || file.length() == 0L) {
            showNotification("No audio recorded.", true)
            return
        }

        viewModelScope.launch {
            isTranscribing.value = true
            val result = GeminiService.transcribeAudio(file)
            isTranscribing.value = false
            result.onSuccess { transcript ->
                transcriptionResult.value = transcript
                aiPromptInput.value = transcript
                showNotification("Audio transcribed with gemini-3.5-transcribe!")
            }.onFailure { err ->
                showNotification("Transcription error: ${err.message}", true)
            }
        }
    }

    fun cancelAudioRecording() {
        audioRecorderManager.reset()
    }

    // Gemini Text Generation
    fun askGemini(prompt: String, model: String = selectedAiModel.value) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            isAiGenerating.value = true
            aiResponseText.value = ""

            val activeEmp = employees.value.filter { it.active }
            val systemContext = buildString {
                append("You are the AI Intelligence system embedded in ApsaRox, an enterprise Attendance and Payroll Management solution. ")
                append("Current company: ${settings.value.companyName}. ")
                append("Active employees count: ${activeEmp.size}. ")
                append("Available employee names: ${activeEmp.joinToString { it.name + " (" + it.eid + ")" }}. ")
                append("Standard working hours: ${settings.value.standardHours}h. Official shift: ${settings.value.startTime} to ${settings.value.endTime}. ")
                append("Format your answers cleanly with bullet points, concise insights, and clear formatting.")
            }

            val result = GeminiService.generateText(
                prompt = prompt,
                model = model,
                systemInstruction = systemContext
            )

            isAiGenerating.value = false
            result.onSuccess {
                aiResponseText.value = it
            }.onFailure {
                aiResponseText.value = "AI Request Failed: ${it.message}"
                showNotification(it.message ?: "AI generation failed", true)
            }
        }
    }



    // Helper calculations
    fun getSalarySummaries(): List<SalarySummary> {
        val y = selectedYear.value
        val m = selectedMonth.value
        val attMap = currentMonthAttendance.value.associateBy { it.date }
        val hols = holidays.value
        val cfg = settings.value
        val today = getTodayString()

        return employees.value.filter { it.active }.map { emp ->
            val (summary, _) = repository.calculateMonthSummary(emp, y, m, attMap, hols, cfg, today)
            repository.calculateSalary(emp, y, m, summary, loans.value, advances.value, cfg)
        }
    }

    fun getDayInfoListForSelectedEmployee(): Pair<MonthSummary?, List<DayInfo>> {
        val empId = selectedEmployeeId.value ?: return Pair(null, emptyList())
        val emp = employees.value.find { it.id == empId } ?: return Pair(null, emptyList())
        val y = selectedYear.value
        val m = selectedMonth.value
        val attMap = currentMonthAttendance.value.filter { it.employeeId == empId }.associateBy { it.date }
        val hols = holidays.value
        val cfg = settings.value
        val today = getTodayString()

        val (summary, days) = repository.calculateMonthSummary(emp, y, m, attMap, hols, cfg, today)
        return Pair(summary, days)
    }
}
