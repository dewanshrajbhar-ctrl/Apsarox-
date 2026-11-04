package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "employees")
data class EmployeeEntity(
    @PrimaryKey val id: String, // e.g. "e0", "e1"
    val eid: String,           // e.g. "E001"
    val name: String,
    val dept: String,
    val desig: String,
    val joinDate: String,      // YYYY-MM-DD
    val salary: Double,
    val hrs: Double = 8.0,
    val offDaysJson: String = "[0]", // JSON array of day of week integers: 0=Sun, 1=Mon, etc.
    val active: Boolean = true
)

@Entity(tableName = "attendance", primaryKeys = ["employeeId", "date"])
data class AttendanceEntity(
    val employeeId: String,
    val date: String,          // YYYY-MM-DD
    val punchesJson: String = "[]", // JSON array of "HH:mm" strings
    val breakMinutes: Int = 60,
    val manualStatus: String = "", // empty means auto-calculate, or "Present", "Absent", "Half Day", "Leave", "WFH", "Holiday"
    val notes: String = ""
)

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey val id: String,
    val date: String,          // YYYY-MM-DD
    val name: String,
    val type: String = "Festival", // Festival, National, Company, Other
    val description: String = ""
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String,
    val employeeId: String,
    val amount: Double,
    val emi: Double,
    val startMonth: String,    // YYYY-MM
    val note: String = ""
)

@Entity(tableName = "advances")
data class AdvanceEntity(
    @PrimaryKey val id: String,
    val employeeId: String,
    val date: String,          // YYYY-MM-DD
    val amount: Double,
    val note: String = ""
)

@Entity(tableName = "company_settings")
data class CompanySettingsEntity(
    @PrimaryKey val id: Int = 1,
    val companyName: String = "ApsaRox",
    val standardHours: Double = 8.0,
    val startTime: String = "09:00",
    val endTime: String = "18:00",
    val graceMinutes: Int = 15,
    val lateFine: Double = 0.0,
    val otRate: Double = 0.0, // 0 = auto
    val method: String = "working", // "working", "calendar", "30"
    val paidLeave: Boolean = true,
    val absentPast: Boolean = true,
    val nightShift: Boolean = false,
    val adminPass: String = "admin"
)

// UI and Business models
data class DayInfo(
    val date: String,
    val dayOfWeek: Int, // 0=Sun..6=Sat
    val punches: List<String>,
    val ins: List<String>,
    val outs: List<String>,
    val breakMin: Int,
    val status: String,
    val workMinutes: Int,
    val otMinutes: Int,
    val shortMinutes: Int,
    val lateMinutes: Int,
    val earlyMinutes: Int,
    val holidayName: String?,
    val isOffDay: Boolean,
    val notes: String
)

data class MonthSummary(
    val totalCalendarDays: Int,
    val workingDays: Int,
    val presentDays: Int,
    val wfhDays: Int,
    val absentDays: Int,
    val halfDays: Int,
    val leaveDays: Int,
    val weeklyOffDays: Int,
    val holidayDays: Int,
    val missingPunches: Int,
    val workMinutes: Int,
    val otMinutes: Int,
    val shortMinutes: Int,
    val lateDays: Int,
    val lateMinutesTotal: Int,
    val earlyLeaveDays: Int,
    val attendancePct: Double
)

data class SalarySummary(
    val employee: EmployeeEntity,
    val monthSummary: MonthSummary,
    val perDaySalary: Double,
    val payableDays: Double,
    val paidLeaveDays: Int,
    val unpaidLeaveDays: Int,
    val otRatePerHour: Double,
    val otAmount: Double,
    val lateDeduction: Double,
    val earnedSalary: Double,
    val loanEmi: Double,
    val advanceDeduction: Double,
    val netSalary: Double
)

data class RestoreResult(
    val employeeCount: Int,
    val attendanceCount: Int,
    val holidayCount: Int,
    val loanCount: Int,
    val advanceCount: Int,
    val companyName: String,
    val timestamp: String
)
