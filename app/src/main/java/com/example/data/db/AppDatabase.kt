package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

@Database(
    entities = [
        EmployeeEntity::class,
        AttendanceEntity::class,
        HolidayEntity::class,
        LoanEntity::class,
        AdvanceEntity::class,
        CompanySettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun employeeDao(): EmployeeDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun holidayDao(): HolidayDao
    abstract fun loanDao(): LoanDao
    abstract fun advanceDao(): AdvanceDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "apsarox_database"
                )
                .addCallback(DatabaseCallback())
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    seedDatabase(database)
                }
            }
        }
    }
}

suspend fun seedDatabase(database: AppDatabase) {
    // 1. Settings
    database.settingsDao().setSettings(
        CompanySettingsEntity(
            id = 1,
            companyName = "ApsaRox",
            standardHours = 8.0,
            startTime = "09:00",
            endTime = "18:00",
            graceMinutes = 15,
            lateFine = 0.0,
            otRate = 0.0,
            method = "working",
            paidLeave = true,
            absentPast = true,
            nightShift = false,
            adminPass = "admin"
        )
    )

    // 2. Employees (Rahul Mehta, Priya Shah, Amit Patel)
    val employees = listOf(
        EmployeeEntity(
            id = "e0",
            eid = "E001",
            name = "Rahul Mehta",
            dept = "Sales",
            desig = "Sales Executive",
            joinDate = "2024-04-01",
            salary = 30000.0,
            hrs = 8.0,
            offDaysJson = "[0]", // Sunday
            active = true
        ),
        EmployeeEntity(
            id = "e1",
            eid = "E002",
            name = "Priya Shah",
            dept = "Accounts",
            desig = "Accountant",
            joinDate = "2024-04-01",
            salary = 36000.0,
            hrs = 8.0,
            offDaysJson = "[0]",
            active = true
        ),
        EmployeeEntity(
            id = "e2",
            eid = "E003",
            name = "Amit Patel",
            dept = "Operations",
            desig = "Supervisor",
            joinDate = "2024-04-01",
            salary = 24000.0,
            hrs = 8.0,
            offDaysJson = "[0]",
            active = true
        )
    )
    database.employeeDao().insertAll(employees)

    // 3. Holidays
    val holidays = listOf(
        HolidayEntity("h1", "2026-09-04", "Janmashtami", "Festival", "Krishna Janmashtami"),
        HolidayEntity("h2", "2026-09-14", "Ganesh Chaturthi", "Festival", "Lord Ganesha birthday"),
        HolidayEntity("h3", "2026-10-02", "Gandhi Jayanti", "National", "National Holiday")
    )
    database.holidayDao().insertAll(holidays)

    // 4. Loans & Advances
    database.loanDao().insertOrUpdate(
        LoanEntity("l1", "e1", 12000.0, 2000.0, "2026-08", "Personal loan")
    )
    database.advanceDao().insertOrUpdate(
        AdvanceEntity("a1", "e0", "2026-09-10", 3000.0, "Festival advance")
    )

    // 5. Pre-seed Attendance records for Sept & Oct 2026
    val attList = mutableListOf<AttendanceEntity>()
    for (i in 0..2) {
        val empId = "e$i"
        for (day in 1..30) {
            val date = String.format("2026-09-%02d", day)
            val cal = Calendar.getInstance().apply {
                set(2026, Calendar.SEPTEMBER, day, 0, 0, 0)
            }
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
            if (dayOfWeek == Calendar.SUNDAY || day == 4 || day == 14) continue

            val inMin = 540 + (day * 7 + i * 3) % 20 - 5
            val outMin = 1080 + (day * 5 + i * 11) % 40 - 10
            val inTime = String.format("%02d:%02d", inMin / 60, inMin % 60)
            val outTime = String.format("%02d:%02d", outMin / 60, outMin % 60)

            attList.add(
                AttendanceEntity(
                    employeeId = empId,
                    date = date,
                    punchesJson = "[\"$inTime\", \"$outTime\"]",
                    breakMinutes = 60,
                    manualStatus = "",
                    notes = ""
                )
            )
        }
    }

    // Specific sample edge-cases from HTML:
    // Rahul Mehta (e0):
    val r0_3 = AttendanceEntity("e0", "2026-09-03", "[]", 60, "Leave", "Approved leave")
    val r0_10 = AttendanceEntity("e0", "2026-09-10", "[\"09:05\"]", 60, "", "Forgot check-out")
    val r0_21 = AttendanceEntity("e0", "2026-09-21", "[\"09:40\", \"18:00\"]", 60, "", "Traffic delay")
    val r0_24 = AttendanceEntity("e0", "2026-09-24", "[\"09:00\", \"17:30\"]", 60, "", "Doctor appointment")
    val r0_25 = AttendanceEntity("e0", "2026-09-25", "[\"08:55\", \"20:00\"]", 60, "", "Client visit")

    // Priya Shah (e1):
    val r1_8 = AttendanceEntity("e1", "2026-09-08", "[]", 60, "Leave", "Sick leave")
    val r1_22 = AttendanceEntity("e1", "2026-09-22", "[\"09:00\", \"19:15\"]", 60, "", "Audit work")

    // Amit Patel (e2):
    val r2_12 = AttendanceEntity("e2", "2026-09-12", "[\"09:00\", \"18:00\"]", 60, "WFH", "Work from home")
    val r2_19 = AttendanceEntity("e2", "2026-09-19", "[\"09:00\", \"13:30\"]", 0, "Half Day", "Half day leave")
    val r2_5 = AttendanceEntity(
        "e2",
        "2026-09-05",
        "[\"09:00\", \"13:00\", \"14:00\", \"18:30\"]",
        60,
        "",
        "Lunch out/in (multiple punches)"
    )

    val specificMap = mutableMapOf<String, AttendanceEntity>()
    listOf(r0_3, r0_10, r0_21, r0_24, r0_25, r1_8, r1_22, r2_12, r2_19, r2_5).forEach {
        specificMap["${it.employeeId}|${it.date}"] = it
    }

    val finalAttList = attList.map {
        specificMap["${it.employeeId}|${it.date}"] ?: it
    }
    database.attendanceDao().insertAll(finalAttList)
}
