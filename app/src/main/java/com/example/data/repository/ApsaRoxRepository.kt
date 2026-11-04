package com.example.data.repository

import com.example.data.db.*
import com.example.data.model.*
import com.example.util.crypto.CryptoManager
import com.example.util.crypto.EncryptedBackupPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ApsaRoxRepository(private val database: AppDatabase) {

    val employees: Flow<List<EmployeeEntity>> = database.employeeDao().getAllEmployees()
    val holidays: Flow<List<HolidayEntity>> = database.holidayDao().getAllHolidays()
    val loans: Flow<List<LoanEntity>> = database.loanDao().getAllLoans()
    val advances: Flow<List<AdvanceEntity>> = database.advanceDao().getAllAdvances()
    val settings: Flow<CompanySettingsEntity?> = database.settingsDao().getSettings()

    fun getMonthAttendance(empId: String, monthPrefix: String): Flow<List<AttendanceEntity>> =
        database.attendanceDao().getRecordsForMonth(empId, monthPrefix)

    fun getAllMonthAttendance(monthPrefix: String): Flow<List<AttendanceEntity>> =
        database.attendanceDao().getAllRecordsForMonth(monthPrefix)

    suspend fun getSettings(): CompanySettingsEntity {
        return database.settingsDao().getSettingsSync() ?: CompanySettingsEntity()
    }

    suspend fun saveSettings(settings: CompanySettingsEntity) {
        database.settingsDao().setSettings(settings)
    }

    suspend fun insertOrUpdateEmployee(employee: EmployeeEntity) {
        database.employeeDao().insertOrUpdate(employee)
    }

    suspend fun deleteEmployee(employeeId: String) {
        database.employeeDao().deleteById(employeeId)
        database.attendanceDao().deleteForEmployee(employeeId)
    }

    suspend fun insertOrUpdateHoliday(holiday: HolidayEntity) {
        database.holidayDao().insertOrUpdate(holiday)
    }

    suspend fun deleteHoliday(id: String) {
        database.holidayDao().deleteById(id)
    }

    suspend fun insertOrUpdateLoan(loan: LoanEntity) {
        database.loanDao().insertOrUpdate(loan)
    }

    suspend fun deleteLoan(id: String) {
        database.loanDao().deleteById(id)
    }

    suspend fun insertOrUpdateAdvance(advance: AdvanceEntity) {
        database.advanceDao().insertOrUpdate(advance)
    }

    suspend fun deleteAdvance(id: String) {
        database.advanceDao().deleteById(id)
    }

    suspend fun punch(empId: String, isCheckIn: Boolean): Result<AttendanceEntity> {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val nowTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        val current = database.attendanceDao().getRecord(empId, today)
            ?: AttendanceEntity(employeeId = empId, date = today, punchesJson = "[]")

        val punches = parsePunches(current.punchesJson).toMutableList()
        val isOdd = punches.size % 2 != 0

        if (isCheckIn && isOdd) {
            return Result.failure(Exception("Already checked IN. Please check OUT first."))
        }
        if (!isCheckIn && !isOdd) {
            return Result.failure(Exception("Not checked IN. Please check IN first."))
        }

        punches.add(nowTime)
        val updated = current.copy(
            punchesJson = JSONArray(punches).toString()
        )
        database.attendanceDao().insertOrUpdate(updated)
        return Result.success(updated)
    }

    suspend fun updateAttendanceRecord(record: AttendanceEntity) {
        database.attendanceDao().insertOrUpdate(record)
    }

    suspend fun deleteAttendanceRecord(empId: String, date: String) {
        database.attendanceDao().deleteRecord(empId, date)
    }

    suspend fun resetToDemoData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
        seedDatabase(database)
    }

    suspend fun exportEncryptedBackup(passphrase: String): EncryptedBackupPackage = withContext(Dispatchers.IO) {
        val emps = database.employeeDao().getAllEmployeesList()
        val atts = database.attendanceDao().getAllRecordsList()
        val hols = database.holidayDao().getAllHolidaysList()
        val loans = database.loanDao().getAllLoansList()
        val advs = database.advanceDao().getAllAdvancesList()
        val cfg = database.settingsDao().getSettingsSync() ?: CompanySettingsEntity()

        val root = JSONObject()
        root.put("schemaVersion", 1)
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
        root.put("company", cfg.companyName)

        val cfgObj = JSONObject().apply {
            put("id", cfg.id)
            put("companyName", cfg.companyName)
            put("standardHours", cfg.standardHours)
            put("startTime", cfg.startTime)
            put("endTime", cfg.endTime)
            put("graceMinutes", cfg.graceMinutes)
            put("lateFine", cfg.lateFine)
            put("otRate", cfg.otRate)
            put("method", cfg.method)
            put("paidLeave", cfg.paidLeave)
            put("absentPast", cfg.absentPast)
            put("nightShift", cfg.nightShift)
            put("adminPass", cfg.adminPass)
        }
        root.put("settings", cfgObj)

        val empArr = JSONArray()
        emps.forEach { e ->
            val o = JSONObject().apply {
                put("id", e.id)
                put("eid", e.eid)
                put("name", e.name)
                put("dept", e.dept)
                put("desig", e.desig)
                put("joinDate", e.joinDate)
                put("salary", e.salary)
                put("hrs", e.hrs)
                put("offDaysJson", e.offDaysJson)
                put("active", e.active)
            }
            empArr.put(o)
        }
        root.put("employees", empArr)

        val attArr = JSONArray()
        atts.forEach { a ->
            val o = JSONObject().apply {
                put("employeeId", a.employeeId)
                put("date", a.date)
                put("punchesJson", a.punchesJson)
                put("breakMinutes", a.breakMinutes)
                put("manualStatus", a.manualStatus)
                put("notes", a.notes)
            }
            attArr.put(o)
        }
        root.put("attendance", attArr)

        val holArr = JSONArray()
        hols.forEach { h ->
            val o = JSONObject().apply {
                put("id", h.id)
                put("date", h.date)
                put("name", h.name)
                put("type", h.type)
                put("description", h.description)
            }
            holArr.put(o)
        }
        root.put("holidays", holArr)

        val loanArr = JSONArray()
        loans.forEach { l ->
            val o = JSONObject().apply {
                put("id", l.id)
                put("employeeId", l.employeeId)
                put("amount", l.amount)
                put("emi", l.emi)
                put("startMonth", l.startMonth)
                put("note", l.note)
            }
            loanArr.put(o)
        }
        root.put("loans", loanArr)

        val advArr = JSONArray()
        advs.forEach { v ->
            val o = JSONObject().apply {
                put("id", v.id)
                put("employeeId", v.employeeId)
                put("date", v.date)
                put("amount", v.amount)
                put("note", v.note)
            }
            advArr.put(o)
        }
        root.put("advances", advArr)

        CryptoManager.encryptWithPassword(root.toString(), passphrase, cfg.companyName)
    }

    suspend fun restoreEncryptedBackup(
        encryptedPackageJson: String,
        passphrase: String
    ): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            val pkg = EncryptedBackupPackage.fromJson(encryptedPackageJson)
            val decryptedJson = CryptoManager.decryptWithPassword(pkg, passphrase)
            val root = JSONObject(decryptedJson)

            if (root.has("settings")) {
                val sObj = root.getJSONObject("settings")
                val cfg = CompanySettingsEntity(
                    id = sObj.optInt("id", 1),
                    companyName = sObj.optString("companyName", "ApsaRox"),
                    standardHours = sObj.optDouble("standardHours", 8.0),
                    startTime = sObj.optString("startTime", "09:00"),
                    endTime = sObj.optString("endTime", "18:00"),
                    graceMinutes = sObj.optInt("graceMinutes", 15),
                    lateFine = sObj.optDouble("lateFine", 0.0),
                    otRate = sObj.optDouble("otRate", 0.0),
                    method = sObj.optString("method", "working"),
                    paidLeave = sObj.optBoolean("paidLeave", true),
                    absentPast = sObj.optBoolean("absentPast", true),
                    nightShift = sObj.optBoolean("nightShift", false),
                    adminPass = sObj.optString("adminPass", "admin")
                )
                database.settingsDao().setSettings(cfg)
            }

            val empList = mutableListOf<EmployeeEntity>()
            val empArr = root.optJSONArray("employees") ?: JSONArray()
            for (i in 0 until empArr.length()) {
                val o = empArr.getJSONObject(i)
                empList.add(
                    EmployeeEntity(
                        id = o.getString("id"),
                        eid = o.getString("eid"),
                        name = o.getString("name"),
                        dept = o.getString("dept"),
                        desig = o.getString("desig"),
                        joinDate = o.getString("joinDate"),
                        salary = o.getDouble("salary"),
                        hrs = o.optDouble("hrs", 8.0),
                        offDaysJson = o.optString("offDaysJson", "[0]"),
                        active = o.optBoolean("active", true)
                    )
                )
            }

            val attList = mutableListOf<AttendanceEntity>()
            val attArr = root.optJSONArray("attendance") ?: JSONArray()
            for (i in 0 until attArr.length()) {
                val o = attArr.getJSONObject(i)
                attList.add(
                    AttendanceEntity(
                        employeeId = o.getString("employeeId"),
                        date = o.getString("date"),
                        punchesJson = o.optString("punchesJson", "[]"),
                        breakMinutes = o.optInt("breakMinutes", 60),
                        manualStatus = o.optString("manualStatus", ""),
                        notes = o.optString("notes", "")
                    )
                )
            }

            val holList = mutableListOf<HolidayEntity>()
            val holArr = root.optJSONArray("holidays") ?: JSONArray()
            for (i in 0 until holArr.length()) {
                val o = holArr.getJSONObject(i)
                holList.add(
                    HolidayEntity(
                        id = o.getString("id"),
                        date = o.getString("date"),
                        name = o.getString("name"),
                        type = o.optString("type", "Festival"),
                        description = o.optString("description", "")
                    )
                )
            }

            val loanList = mutableListOf<LoanEntity>()
            val loanArr = root.optJSONArray("loans") ?: JSONArray()
            for (i in 0 until loanArr.length()) {
                val o = loanArr.getJSONObject(i)
                loanList.add(
                    LoanEntity(
                        id = o.getString("id"),
                        employeeId = o.getString("employeeId"),
                        amount = o.getDouble("amount"),
                        emi = o.getDouble("emi"),
                        startMonth = o.getString("startMonth"),
                        note = o.optString("note", "")
                    )
                )
            }

            val advList = mutableListOf<AdvanceEntity>()
            val advArr = root.optJSONArray("advances") ?: JSONArray()
            for (i in 0 until advArr.length()) {
                val o = advArr.getJSONObject(i)
                advList.add(
                    AdvanceEntity(
                        id = o.getString("id"),
                        employeeId = o.getString("employeeId"),
                        date = o.getString("date"),
                        amount = o.getDouble("amount"),
                        note = o.optString("note", "")
                    )
                )
            }

            database.employeeDao().deleteAll()
            database.attendanceDao().deleteAll()
            database.holidayDao().deleteAll()
            database.loanDao().deleteAll()
            database.advanceDao().deleteAll()

            if (empList.isNotEmpty()) database.employeeDao().insertAll(empList)
            if (attList.isNotEmpty()) database.attendanceDao().insertAll(attList)
            if (holList.isNotEmpty()) database.holidayDao().insertAll(holList)
            if (loanList.isNotEmpty()) database.loanDao().insertAll(loanList)
            if (advList.isNotEmpty()) database.advanceDao().insertAll(advList)

            Result.success(
                RestoreResult(
                    employeeCount = empList.size,
                    attendanceCount = attList.size,
                    holidayCount = holList.size,
                    loanCount = loanList.size,
                    advanceCount = advList.size,
                    companyName = pkg.company,
                    timestamp = pkg.timestamp
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Business Logic Calculations matching ApsaRox
    fun calculateDayInfo(
        employee: EmployeeEntity,
        date: String, // YYYY-MM-DD
        attendance: AttendanceEntity?,
        holidays: List<HolidayEntity>,
        settings: CompanySettingsEntity,
        todayDate: String
    ): DayInfo {
        val cal = Calendar.getInstance().apply {
            val parts = date.split("-").map { it.toInt() }
            set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun..6=Sat

        val offDays = parseOffDays(employee.offDaysJson)
        val isOff = offDays.contains(dayOfWeek)
        val holiday = holidays.find { it.date == date }

        val punches = attendance?.let { parsePunches(it.punchesJson) } ?: emptyList()
        val ins = punches.filterIndexed { index, _ -> index % 2 == 0 }
        val outs = punches.filterIndexed { index, _ -> index % 2 == 1 }

        val inMin = if (ins.isNotEmpty()) timeToMinutes(ins.first()) else null
        val outMin = if (outs.isNotEmpty()) timeToMinutes(outs.last()) else null
        val breakMin = attendance?.breakMinutes ?: 60

        var workMinutes = 0
        if (punches.size > 2) {
            var k = 0
            while (k + 1 < punches.size) {
                val t1 = timeToMinutes(punches[k]) ?: 0
                val t2 = timeToMinutes(punches[k + 1]) ?: 0
                var diff = t2 - t1
                if (diff < 0 && settings.nightShift) diff += 1440
                if (diff > 0) workMinutes += diff
                k += 2
            }
        } else if (inMin != null && outMin != null) {
            var diff = outMin - inMin
            if (diff < 0 && settings.nightShift) diff += 1440
            workMinutes = (diff - breakMin).coerceAtLeast(0)
        }

        var status = attendance?.manualStatus?.takeIf { it.isNotEmpty() }
        if (status == null) {
            status = when {
                inMin != null && outMin != null -> "Present"
                inMin != null -> "Missing OUT"
                outMin != null -> "Missing IN"
                isOff -> if (dayOfWeek == 0) "Sunday" else "Weekly Off"
                holiday != null -> "Holiday"
                date < todayDate -> if (settings.absentPast) "Absent" else "No Record"
                else -> "Pending"
            }
        }

        val reqMinutes = (employee.hrs * 60).toInt()
        val isWorkingStatus = status == "Present" || status == "WFH"

        val otMinutes = if (isWorkingStatus && workMinutes > reqMinutes) workMinutes - reqMinutes else 0
        val shortMinutes = if (isWorkingStatus && workMinutes < reqMinutes) reqMinutes - workMinutes else 0

        val startMin = timeToMinutes(settings.startTime) ?: (9 * 60)
        val lateMinutes = if (inMin != null && inMin > startMin + settings.graceMinutes) {
            inMin - startMin
        } else 0

        val endMin = timeToMinutes(settings.endTime) ?: (18 * 60)
        val earlyMinutes = if (outMin != null && inMin != null && outMin < endMin && outMin >= inMin) {
            endMin - outMin
        } else 0

        return DayInfo(
            date = date,
            dayOfWeek = dayOfWeek,
            punches = punches,
            ins = ins,
            outs = outs,
            breakMin = breakMin,
            status = status,
            workMinutes = workMinutes,
            otMinutes = otMinutes,
            shortMinutes = shortMinutes,
            lateMinutes = lateMinutes,
            earlyMinutes = earlyMinutes,
            holidayName = holiday?.name,
            isOffDay = isOff,
            notes = attendance?.notes ?: ""
        )
    }

    fun calculateMonthSummary(
        employee: EmployeeEntity,
        year: Int,
        month: Int, // 1..12
        attendanceMap: Map<String, AttendanceEntity>,
        holidays: List<HolidayEntity>,
        settings: CompanySettingsEntity,
        todayDate: String
    ): Pair<MonthSummary, List<DayInfo>> {
        val daysInMonth = getDaysInMonth(year, month)
        val dayList = mutableListOf<DayInfo>()

        var totalWorkingDays = 0
        var presentDays = 0
        var wfhDays = 0
        var absentDays = 0
        var halfDays = 0
        var leaveDays = 0
        var weeklyOffDays = 0
        var holidayDays = 0
        var missingPunches = 0
        var totalWorkMin = 0
        var totalOtMin = 0
        var totalShortMin = 0
        var lateDays = 0
        var lateMinutesTotal = 0
        var earlyDays = 0

        for (d in 1..daysInMonth) {
            val dateStr = String.format("%04d-%02d-%02d", year, month, d)
            val info = calculateDayInfo(
                employee,
                dateStr,
                attendanceMap[dateStr],
                holidays,
                settings,
                todayDate
            )
            dayList.add(info)

            totalWorkMin += info.workMinutes
            totalOtMin += info.otMinutes
            totalShortMin += info.shortMinutes

            if (info.lateMinutes > 0) {
                lateDays++
                lateMinutesTotal += info.lateMinutes
            }
            if (info.earlyMinutes > 0) {
                earlyDays++
            }

            when (info.status) {
                "Sunday", "Weekly Off" -> weeklyOffDays++
                "Holiday" -> holidayDays++
                else -> totalWorkingDays++
            }

            when (info.status) {
                "Present" -> presentDays++
                "WFH" -> wfhDays++
                "Absent" -> absentDays++
                "Half Day" -> halfDays++
                "Leave" -> leaveDays++
                "Missing IN", "Missing OUT" -> missingPunches++
            }
        }

        val effectivePresent = presentDays + wfhDays + (halfDays * 0.5)
        val attendancePct = if (totalWorkingDays > 0) (effectivePresent / totalWorkingDays) * 100.0 else 0.0

        val summary = MonthSummary(
            totalCalendarDays = daysInMonth,
            workingDays = totalWorkingDays,
            presentDays = presentDays,
            wfhDays = wfhDays,
            absentDays = absentDays,
            halfDays = halfDays,
            leaveDays = leaveDays,
            weeklyOffDays = weeklyOffDays,
            holidayDays = holidayDays,
            missingPunches = missingPunches,
            workMinutes = totalWorkMin,
            otMinutes = totalOtMin,
            shortMinutes = totalShortMin,
            lateDays = lateDays,
            lateMinutesTotal = lateMinutesTotal,
            earlyLeaveDays = earlyDays,
            attendancePct = attendancePct
        )

        return Pair(summary, dayList)
    }

    fun calculateSalary(
        employee: EmployeeEntity,
        year: Int,
        month: Int,
        monthSummary: MonthSummary,
        loans: List<LoanEntity>,
        advances: List<AdvanceEntity>,
        settings: CompanySettingsEntity
    ): SalarySummary {
        val denominator = when (settings.method) {
            "working" -> monthSummary.workingDays.toDouble()
            "calendar" -> monthSummary.totalCalendarDays.toDouble()
            else -> 30.0
        }

        val perDaySalary = if (denominator > 0) employee.salary / denominator else 0.0
        val paidLeaveDays = if (settings.paidLeave) monthSummary.leaveDays else 0
        val unpaidLeaveDays = if (settings.paidLeave) 0 else monthSummary.leaveDays

        val payableDays = monthSummary.presentDays +
                monthSummary.wfhDays +
                (monthSummary.halfDays * 0.5) +
                paidLeaveDays +
                (if (settings.method != "working") monthSummary.weeklyOffDays + monthSummary.holidayDays else 0)

        val earnedSalary = (perDaySalary * payableDays).coerceAtMost(employee.salary * 1.5)

        val otRatePerHour = if (settings.otRate > 0) settings.otRate else {
            if (employee.hrs > 0) perDaySalary / employee.hrs else 0.0
        }
        val otAmount = (monthSummary.otMinutes / 60.0) * otRatePerHour
        val lateDeduction = monthSummary.lateDays * settings.lateFine

        // Loan EMI
        var loanEmi = 0.0
        val targetMonthIndex = (year * 12) + month
        loans.filter { it.employeeId == employee.id }.forEach { loan ->
            val startParts = loan.startMonth.split("-").map { it.toInt() }
            val loanStartMonthIndex = (startParts[0] * 12) + startParts[1]
            val totalEmis = Math.ceil(loan.amount / loan.emi).toInt()
            val monthDiff = targetMonthIndex - loanStartMonthIndex
            if (monthDiff in 0 until totalEmis) {
                val remainingBal = loan.amount - (monthDiff * loan.emi)
                loanEmi += Math.min(loan.emi, remainingBal)
            }
        }

        // Advance in this month
        val monthPrefix = String.format("%04d-%02d", year, month)
        val advanceTotal = advances
            .filter { it.employeeId == employee.id && it.date.startsWith(monthPrefix) }
            .sumOf { it.amount }

        val netSalary = (earnedSalary + otAmount - lateDeduction - loanEmi - advanceTotal).coerceAtLeast(0.0)

        return SalarySummary(
            employee = employee,
            monthSummary = monthSummary,
            perDaySalary = perDaySalary,
            payableDays = payableDays,
            paidLeaveDays = paidLeaveDays,
            unpaidLeaveDays = unpaidLeaveDays,
            otRatePerHour = otRatePerHour,
            otAmount = otAmount,
            lateDeduction = lateDeduction,
            earnedSalary = earnedSalary,
            loanEmi = loanEmi,
            advanceDeduction = advanceTotal,
            netSalary = netSalary
        )
    }

    companion object {
        fun parsePunches(json: String): List<String> {
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        fun parseOffDays(json: String): List<Int> {
            return try {
                val array = JSONArray(json)
                val list = mutableListOf<Int>()
                for (i in 0 until array.length()) {
                    list.add(array.getInt(i))
                }
                list
            } catch (e: Exception) {
                listOf(0) // Sunday default
            }
        }

        fun timeToMinutes(timeStr: String): Int? {
            val parts = timeStr.trim().split(":")
            if (parts.size != 2) return null
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            return h * 60 + m
        }

        fun minutesToTime(minutes: Int): String {
            val h = minutes / 60
            val m = minutes % 60
            return String.format("%02d:%02d", h, m)
        }

        fun getDaysInMonth(year: Int, month: Int): Int {
            val cal = Calendar.getInstance().apply {
                set(year, month - 1, 1)
            }
            return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }
    }
}
