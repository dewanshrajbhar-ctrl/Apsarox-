package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.CompanySettingsEntity
import com.example.data.model.DayInfo
import com.example.data.model.EmployeeEntity
import com.example.data.model.MonthSummary
import com.example.data.model.SalarySummary
import com.example.data.repository.ApsaRoxRepository
import com.example.util.PdfReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("ApsaRox", appName)
  }

  @Test
  fun `test salary calculation logic`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repo = ApsaRoxRepository(db)

    val emp = EmployeeEntity(
      id = "test_emp",
      eid = "E999",
      name = "Test Staff",
      dept = "Engineering",
      desig = "Developer",
      joinDate = "2024-01-01",
      salary = 30000.0,
      hrs = 8.0
    )

    val monthSummary = MonthSummary(
      totalCalendarDays = 30,
      workingDays = 24,
      presentDays = 24,
      wfhDays = 0,
      absentDays = 0,
      halfDays = 0,
      leaveDays = 0,
      weeklyOffDays = 4,
      holidayDays = 2,
      missingPunches = 0,
      workMinutes = 24 * 8 * 60,
      otMinutes = 60, // 1 hour OT
      shortMinutes = 0,
      lateDays = 0,
      lateMinutesTotal = 0,
      earlyLeaveDays = 0,
      attendancePct = 100.0
    )

    val settings = CompanySettingsEntity(
      method = "working",
      otRate = 200.0
    )

    val salary = repo.calculateSalary(emp, 2026, 9, monthSummary, emptyList(), emptyList(), settings)
    assertEquals(30000.0, salary.earnedSalary, 0.01)
    assertEquals(200.0, salary.otAmount, 0.01)
    assertEquals(30200.0, salary.netSalary, 0.01)
  }

  @Test
  fun `test pdf generation for attendance, payroll and custom reports`() {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val emp = EmployeeEntity(
      id = "e0",
      eid = "E001",
      name = "Rahul Mehta",
      dept = "Sales",
      desig = "Sales Executive",
      joinDate = "2024-04-01",
      salary = 30000.0,
      hrs = 8.0
    )

    val monthSummary = MonthSummary(
      totalCalendarDays = 30,
      workingDays = 24,
      presentDays = 23,
      wfhDays = 1,
      absentDays = 0,
      halfDays = 0,
      leaveDays = 0,
      weeklyOffDays = 4,
      holidayDays = 2,
      missingPunches = 0,
      workMinutes = 24 * 8 * 60,
      otMinutes = 120,
      shortMinutes = 0,
      lateDays = 1,
      lateMinutesTotal = 25,
      earlyLeaveDays = 0,
      attendancePct = 100.0
    )

    val dayInfos = listOf(
      DayInfo(
        date = "2026-09-01",
        dayOfWeek = 2,
        punches = listOf("09:00", "18:00"),
        ins = listOf("09:00"),
        outs = listOf("18:00"),
        breakMin = 60,
        status = "Present",
        workMinutes = 480,
        otMinutes = 0,
        shortMinutes = 0,
        lateMinutes = 0,
        earlyMinutes = 0,
        holidayName = null,
        isOffDay = false,
        notes = "On time"
      )
    )

    val salarySummary = SalarySummary(
      employee = emp,
      monthSummary = monthSummary,
      perDaySalary = 1250.0,
      payableDays = 24.0,
      paidLeaveDays = 0,
      unpaidLeaveDays = 0,
      otRatePerHour = 156.25,
      otAmount = 312.5,
      lateDeduction = 0.0,
      earnedSalary = 30000.0,
      loanEmi = 0.0,
      advanceDeduction = 0.0,
      netSalary = 30312.5
    )

    // 1. Attendance PDF
    val attendancePdf = PdfReportGenerator.generateAttendancePdf(
      context = context,
      companyName = "ApsaRox",
      employee = emp,
      monthSummary = monthSummary,
      dayInfos = dayInfos,
      year = 2026,
      month = 9
    )
    assertTrue("Attendance PDF must exist", attendancePdf.exists())
    assertTrue("Attendance PDF must have size > 0", attendancePdf.length() > 0)

    // 2. Payroll Register PDF
    val payrollPdf = PdfReportGenerator.generatePayrollRegisterPdf(
      context = context,
      companyName = "ApsaRox",
      year = 2026,
      month = 9,
      salarySummaries = listOf(salarySummary)
    )
    assertTrue("Payroll PDF must exist", payrollPdf.exists())
    assertTrue("Payroll PDF must have size > 0", payrollPdf.length() > 0)

    // 3. Salary Slip PDF
    val payslipPdf = PdfReportGenerator.generateSalarySlipPdf(
      context = context,
      companyName = "ApsaRox",
      year = 2026,
      month = 9,
      salarySummary = salarySummary
    )
    assertTrue("Payslip PDF must exist", payslipPdf.exists())
    assertTrue("Payslip PDF must have size > 0", payslipPdf.length() > 0)

    // 4. Custom Report PDF
    val customPdf = PdfReportGenerator.generateCustomReportPdf(
      context = context,
      companyName = "ApsaRox",
      reportTitle = "Overtime Report",
      dateRange = "2026-09-01 to 2026-09-30",
      headers = listOf("EID", "Name", "Working Days", "OT Hours", "OT Pay"),
      rows = listOf(listOf("E001", "Rahul Mehta", "24", "2h", "₹313"))
    )
    assertTrue("Custom Report PDF must exist", customPdf.exists())
    assertTrue("Custom Report PDF must have size > 0", customPdf.length() > 0)
  }

  @Test
  fun `test AES-256-GCM local hardware encryption and decryption`() {
    val sampleData = "CONFIDENTIAL_PAYROLL_DATA: salary=55000, bank_account=9876543210"
    val encrypted = com.example.util.crypto.CryptoManager.encryptLocal(sampleData)

    assertNotNull(encrypted.ivBase64)
    assertNotNull(encrypted.cipherTextBase64)
    org.junit.Assert.assertNotEquals(sampleData, encrypted.cipherTextBase64)

    val decrypted = com.example.util.crypto.CryptoManager.decryptLocal(encrypted)
    assertEquals(sampleData, decrypted)
  }

  @Test
  fun `test password-derived AES-256-GCM backup package and integrity verification`() {
    val secretPayload = "{\"company\":\"ApsaRox Enterprise\",\"records\":1500,\"status\":\"active\"}"
    val passphrase = "MasterEncryptionKey#2026"

    val encryptedPackage = com.example.util.crypto.CryptoManager.encryptWithPassword(
      plainText = secretPayload,
      passphrase = passphrase,
      companyName = "ApsaRox"
    )

    assertEquals("AES-256-GCM", encryptedPackage.algorithm)
    assertEquals("PBKDF2WithHmacSHA256", encryptedPackage.kdf)
    assertEquals(65536, encryptedPackage.iterations)
    assertNotNull(encryptedPackage.sha256Checksum)

    // Decrypt with correct password
    val decrypted = com.example.util.crypto.CryptoManager.decryptWithPassword(
      backupPackage = encryptedPackage,
      passphrase = passphrase
    )
    assertEquals(secretPayload, decrypted)

    // Verify SHA-256 hash match
    val hash = com.example.util.crypto.CryptoManager.computeSha256(decrypted)
    assertEquals(encryptedPackage.sha256Checksum, hash)
  }

  @Test(expected = SecurityException::class)
  fun `test incorrect password throws SecurityException`() {
    val secretPayload = "TOP_SECRET_PAYROLL_LEDGER"
    val encryptedPackage = com.example.util.crypto.CryptoManager.encryptWithPassword(
      plainText = secretPayload,
      passphrase = "CorrectPassword123"
    )

    // Decrypt with wrong password must throw SecurityException
    com.example.util.crypto.CryptoManager.decryptWithPassword(
      backupPackage = encryptedPackage,
      passphrase = "WrongPassword999"
    )
  }

  @Test(expected = SecurityException::class)
  fun `test tampered ciphertext is rejected by authentication tag or SHA-256`() {
    val secretPayload = "UNALTERED_FINANCIAL_RECORDS"
    val pkg = com.example.util.crypto.CryptoManager.encryptWithPassword(
      plainText = secretPayload,
      passphrase = "SecurePassword@123"
    )

    // Tamper with cipherText (flip characters)
    val tamperedCipher = if (pkg.cipherTextBase64.startsWith("A")) "B" + pkg.cipherTextBase64.substring(1) else "A" + pkg.cipherTextBase64.substring(1)
    val tamperedPkg = pkg.copy(cipherTextBase64 = tamperedCipher)

    com.example.util.crypto.CryptoManager.decryptWithPassword(
      backupPackage = tamperedPkg,
      passphrase = "SecurePassword@123"
    )
  }

  @Test
  fun `test complete end-to-end repository backup export and restore`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.db.AppDatabase.getInstance(context)
    val repo = ApsaRoxRepository(db)

    // Ensure database is seeded with demo data
    repo.resetToDemoData()

    val passphrase = "EnterpriseVaultPass2026!"
    val backupPackage = repo.exportEncryptedBackup(passphrase)

    assertNotNull(backupPackage.cipherTextBase64)
    assertNotNull(backupPackage.sha256Checksum)
    assertEquals("ApsaRox", backupPackage.company)

    // Restore from encrypted package JSON
    val restoreResult = repo.restoreEncryptedBackup(backupPackage.toJson(), passphrase)
    assertTrue("Restore should succeed", restoreResult.isSuccess)

    val summary = restoreResult.getOrThrow()
    assertTrue("Should have restored employees", summary.employeeCount >= 3)
    assertTrue("Should have restored attendance records", summary.attendanceCount > 0)
    assertTrue("Should have restored holidays", summary.holidayCount >= 3)
    assertEquals("ApsaRox", summary.companyName)
  }
}
