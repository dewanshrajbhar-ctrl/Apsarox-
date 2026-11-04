package com.example.data.db

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY eid ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: String): EmployeeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(employee: EmployeeEntity)

    @Delete
    suspend fun delete(employee: EmployeeEntity)

    @Query("DELETE FROM employees WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM employees ORDER BY eid ASC")
    suspend fun getAllEmployeesList(): List<EmployeeEntity>

    @Query("DELETE FROM employees")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<EmployeeEntity>)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE employeeId = :empId AND date = :date LIMIT 1")
    suspend fun getRecord(empId: String, date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE employeeId = :empId AND date LIKE :monthPrefix || '%'")
    fun getRecordsForMonth(empId: String, monthPrefix: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date LIKE :monthPrefix || '%'")
    fun getAllRecordsForMonth(monthPrefix: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getRecordsForDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance")
    fun getAllRecords(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance")
    suspend fun getAllRecordsList(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<AttendanceEntity>)

    @Query("DELETE FROM attendance WHERE employeeId = :empId AND date = :date")
    suspend fun deleteRecord(empId: String, date: String)

    @Query("DELETE FROM attendance WHERE employeeId = :empId")
    suspend fun deleteForEmployee(empId: String)

    @Query("DELETE FROM attendance")
    suspend fun deleteAll()
}

@Dao
interface HolidayDao {
    @Query("SELECT * FROM holidays ORDER BY date ASC")
    fun getAllHolidays(): Flow<List<HolidayEntity>>

    @Query("SELECT * FROM holidays ORDER BY date ASC")
    suspend fun getAllHolidaysList(): List<HolidayEntity>

    @Query("SELECT * FROM holidays WHERE date LIKE :yearPrefix || '%' ORDER BY date ASC")
    fun getHolidaysForYear(yearPrefix: String): Flow<List<HolidayEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(holiday: HolidayEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(holidays: List<HolidayEntity>)

    @Query("DELETE FROM holidays WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM holidays")
    suspend fun deleteAll()
}

@Dao
interface LoanDao {
    @Query("SELECT * FROM loans")
    fun getAllLoans(): Flow<List<LoanEntity>>

    @Query("SELECT * FROM loans")
    suspend fun getAllLoansList(): List<LoanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(loan: LoanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(loans: List<LoanEntity>)

    @Query("DELETE FROM loans WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM loans")
    suspend fun deleteAll()
}

@Dao
interface AdvanceDao {
    @Query("SELECT * FROM advances ORDER BY date DESC")
    fun getAllAdvances(): Flow<List<AdvanceEntity>>

    @Query("SELECT * FROM advances ORDER BY date DESC")
    suspend fun getAllAdvancesList(): List<AdvanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(advance: AdvanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(advances: List<AdvanceEntity>)

    @Query("DELETE FROM advances WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM advances")
    suspend fun deleteAll()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM company_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<CompanySettingsEntity?>

    @Query("SELECT * FROM company_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): CompanySettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSettings(settings: CompanySettingsEntity)
}
