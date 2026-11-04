package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.DayInfo
import com.example.data.model.EmployeeEntity
import com.example.data.model.MonthSummary
import com.example.data.model.SalarySummary
import com.example.data.repository.ApsaRoxRepository
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object PdfReportGenerator {

    // PDF Colors adhering to JET BLACK, ROYAL AMETHYST, ELECTRIC CYAN
    private const val COLOR_JET_BLACK = 0xFF09090D.toInt()
    private const val COLOR_JET_ELEVATED = 0xFF14141E.toInt()
    private const val COLOR_ROYAL_AMETHYST = 0xFF7C3AED.toInt()
    private const val COLOR_ROYAL_AMETHYST_DARK = 0xFF4C1D95.toInt()
    private const val COLOR_ELECTRIC_CYAN = 0xFF00E5FF.toInt()
    private const val COLOR_CYAN_ACCENT = 0xFF0891B2.toInt()
    private const val COLOR_TEXT_DARK = 0xFF1E293B.toInt()
    private const val COLOR_TEXT_MUTED = 0xFF64748B.toInt()
    private const val COLOR_BORDER = 0xFFCBD5E1.toInt()
    private const val COLOR_ROW_ALT = 0xFFF8FAFC.toInt()
    private const val COLOR_WHITE = 0xFFFFFFFF.toInt()
    private const val COLOR_STATUS_GREEN = 0xFF059669.toInt()
    private const val COLOR_STATUS_RED = 0xFFDC2626.toInt()

    private const val PAGE_WIDTH = 595 // Standard A4 width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points

    private fun getReportsDir(context: Context): File {
        val dir = File(context.cacheDir, "reports")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun formatInr(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
            .format(amount)
            .replace(".00", "")
    }

    private fun writeFallbackPdf(file: File, title: String, subtitle: String) {
        val streamContent = "BT /F1 16 Tf 50 750 Td ($title) Tj ET BT /F1 12 Tf 50 720 Td ($subtitle) Tj ET"
        val streamLen = streamContent.length
        val pdf = """%PDF-1.4
1 0 obj
<< /Type /Catalog /Pages 2 0 R >>
endobj
2 0 obj
<< /Type /Pages /Kids [3 0 R] /Count 1 >>
endobj
3 0 obj
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>
endobj
4 0 obj
<< /Length $streamLen >>
stream
$streamContent
endstream
endobj
5 0 obj
<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>
endobj
xref
0 6
0000000000 65535 f 
0000000009 00000 n 
0000000058 00000 n 
0000000115 00000 n 
0000000244 00000 n 
0000000305 00000 n 
trailer
<< /Size 6 /Root 1 0 R >>
startxref
375
%%EOF
"""
        file.writeText(pdf)
    }

    private fun getTimestamp(): String {
        return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
    }

    /**
     * Draw standard branded ApsaRox header with Jet Black, Royal Amethyst, and Electric Cyan
     */
    private fun drawDocumentHeader(
        canvas: Canvas,
        companyName: String,
        documentTitle: String,
        subTitle: String,
        paint: Paint
    ) {
        // 1. Top banner in JET BLACK
        paint.color = COLOR_JET_BLACK
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 64f, paint)

        // 2. Cyan accent line
        paint.color = COLOR_ELECTRIC_CYAN
        paint.strokeWidth = 3f
        canvas.drawLine(0f, 64f, PAGE_WIDTH.toFloat(), 64f, paint)

        // 3. Royal Amethyst secondary accent stripe
        paint.color = COLOR_ROYAL_AMETHYST
        paint.strokeWidth = 2f
        canvas.drawLine(0f, 66f, PAGE_WIDTH.toFloat(), 66f, paint)

        // Reset stroke
        paint.style = Paint.Style.FILL

        // Company Name in Electric Cyan
        paint.color = COLOR_ELECTRIC_CYAN
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(companyName.uppercase(), 28f, 26f, paint)

        // Badge: "APSAROX WORKFORCE & PAYROLL"
        paint.color = COLOR_ROYAL_AMETHYST
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("APSAROX INTELLIGENCE SUITE", 28f, 40f, paint)

        // Document Title (Right-aligned in white)
        paint.color = COLOR_WHITE
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val titleWidth = paint.measureText(documentTitle)
        canvas.drawText(documentTitle, PAGE_WIDTH - 28f - titleWidth, 26f, paint)

        // Subtitle (Date / Period)
        paint.color = COLOR_ELECTRIC_CYAN
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val subWidth = paint.measureText(subTitle)
        canvas.drawText(subTitle, PAGE_WIDTH - 28f - subWidth, 42f, paint)
    }

    private fun drawDocumentFooter(canvas: Canvas, pageNumber: Int, totalPages: Int, paint: Paint) {
        paint.color = COLOR_BORDER
        paint.strokeWidth = 1f
        canvas.drawLine(28f, PAGE_HEIGHT - 36f, PAGE_WIDTH - 28f, PAGE_HEIGHT - 36f, paint)

        paint.color = COLOR_TEXT_MUTED
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Generated by ApsaRox Intelligence on ${getTimestamp()}", 28f, PAGE_HEIGHT - 22f, paint)

        val pageStr = "Page $pageNumber of $totalPages"
        val pageWidth = paint.measureText(pageStr)
        canvas.drawText(pageStr, PAGE_WIDTH - 28f - pageWidth, PAGE_HEIGHT - 22f, paint)
    }

    /**
     * 1. GENERATE ATTENDANCE REPORT PDF (Detailed Timesheet / Attendance Grid)
     */
    fun generateAttendancePdf(
        context: Context,
        companyName: String,
        employee: EmployeeEntity,
        monthSummary: MonthSummary,
        dayInfos: List<DayInfo>,
        year: Int,
        month: Int
    ): File {
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val periodStr = "${monthNames.getOrElse(month - 1) { "" }} $year"
        val file = File(getReportsDir(context), "Attendance_${employee.eid}_${year}_${month}.pdf")

        try {
            val document = PdfDocument()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val rowsPerPage = 28
        val totalPages = Math.max(1, Math.ceil(dayInfos.size.toDouble() / rowsPerPage).toInt())

        for (pageIdx in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header
            drawDocumentHeader(
                canvas,
                companyName,
                "ATTENDANCE REPORT",
                "$periodStr · ${employee.name} (${employee.eid})",
                paint
            )

            var y = 84f

            // On Page 1, draw Employee Profile & KPI Summary Cards
            if (pageIdx == 0) {
                // Profile Banner
                paint.color = COLOR_JET_ELEVATED
                canvas.drawRoundRect(28f, y, PAGE_WIDTH - 28f, y + 42f, 8f, 8f, paint)

                paint.color = COLOR_ELECTRIC_CYAN
                paint.textSize = 11f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${employee.name}  [${employee.eid}]", 40f, y + 18f, paint)

                paint.color = COLOR_WHITE
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Dept: ${employee.dept}  |  Role: ${employee.desig}  |  Std: ${employee.hrs}h/day  |  Joined: ${employee.joinDate}", 40f, y + 32f, paint)

                y += 50f

                // Summary Stats in 4 mini-boxes
                val boxWidth = (PAGE_WIDTH - 56f - 24f) / 4f
                val stats = listOf(
                    "Working Days" to "${monthSummary.workingDays}",
                    "Present" to "${monthSummary.presentDays + monthSummary.wfhDays}",
                    "Overtime" to "${ApsaRoxRepository.minutesToTime(monthSummary.otMinutes)}h",
                    "Attendance" to "${String.format("%.1f", monthSummary.attendancePct)}%"
                )

                stats.forEachIndexed { i, (label, value) ->
                    val bx = 28f + (i * (boxWidth + 8f))
                    paint.color = COLOR_ROW_ALT
                    canvas.drawRoundRect(bx, y, bx + boxWidth, y + 36f, 6f, 6f, paint)
                    paint.color = COLOR_BORDER
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRoundRect(bx, y, bx + boxWidth, y + 36f, 6f, 6f, paint)
                    paint.style = Paint.Style.FILL

                    paint.color = COLOR_TEXT_MUTED
                    paint.textSize = 8f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText(label, bx + 8f, y + 14f, paint)

                    paint.color = if (i == 3) COLOR_ROYAL_AMETHYST else if (i == 2) COLOR_CYAN_ACCENT else COLOR_TEXT_DARK
                    paint.textSize = 12f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(value, bx + 8f, y + 30f, paint)
                }

                y += 46f
            }

            // Table Header
            paint.color = COLOR_ROYAL_AMETHYST_DARK
            canvas.drawRect(28f, y, PAGE_WIDTH - 28f, y + 22f, paint)

            paint.color = COLOR_WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("Date", 34f, y + 15f, paint)
            canvas.drawText("Day", 80f, y + 15f, paint)
            canvas.drawText("IN", 115f, y + 15f, paint)
            canvas.drawText("OUT", 155f, y + 15f, paint)
            canvas.drawText("Break", 195f, y + 15f, paint)
            canvas.drawText("Work", 235f, y + 15f, paint)
            canvas.drawText("OT", 275f, y + 15f, paint)
            canvas.drawText("Status", 315f, y + 15f, paint)
            canvas.drawText("Notes & Details", 385f, y + 15f, paint)

            y += 22f

            // Data Rows for this page
            val startIdx = pageIdx * rowsPerPage
            val endIdx = Math.min(dayInfos.size, startIdx + rowsPerPage)
            val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

            for (r in startIdx until endIdx) {
                val d = dayInfos[r]
                val isAlt = (r % 2 == 1)

                if (isAlt) {
                    paint.color = COLOR_ROW_ALT
                    canvas.drawRect(28f, y, PAGE_WIDTH - 28f, y + 18f, paint)
                }

                // Row borders
                paint.color = COLOR_BORDER
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                canvas.drawRect(28f, y, PAGE_WIDTH - 28f, y + 18f, paint)
                paint.style = Paint.Style.FILL

                val inStr = d.ins.firstOrNull() ?: "—"
                val outStr = d.outs.lastOrNull() ?: "—"
                val workStr = ApsaRoxRepository.minutesToTime(d.workMinutes)
                val otStr = if (d.otMinutes > 0) "+${ApsaRoxRepository.minutesToTime(d.otMinutes)}" else "—"

                paint.color = COLOR_TEXT_DARK
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText(d.date.substring(5), 34f, y + 13f, paint)
                canvas.drawText(dayNames.getOrElse(d.dayOfWeek) { "" }, 80f, y + 13f, paint)
                canvas.drawText(inStr, 115f, y + 13f, paint)
                canvas.drawText(outStr, 155f, y + 13f, paint)
                canvas.drawText("${d.breakMin}m", 195f, y + 13f, paint)
                canvas.drawText(workStr, 235f, y + 13f, paint)

                paint.color = if (d.otMinutes > 0) COLOR_STATUS_GREEN else COLOR_TEXT_MUTED
                canvas.drawText(otStr, 275f, y + 13f, paint)

                // Status with color
                paint.color = when (d.status) {
                    "Present", "WFH" -> COLOR_STATUS_GREEN
                    "Absent" -> COLOR_STATUS_RED
                    "Leave" -> COLOR_CYAN_ACCENT
                    else -> COLOR_TEXT_MUTED
                }
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(d.status, 315f, y + 13f, paint)

                // Note
                paint.color = COLOR_TEXT_MUTED
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val noteSnippet = (if (d.notes.isNotEmpty()) d.notes else d.holidayName ?: "").take(32)
                canvas.drawText(noteSnippet, 385f, y + 13f, paint)

                y += 18f
            }

            // Footer
            drawDocumentFooter(canvas, pageIdx + 1, totalPages, paint)
            document.finishPage(page)
        }

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            return file
        } catch (t: Throwable) {
            writeFallbackPdf(file, "ATTENDANCE REPORT - $companyName", "${employee.name} (${employee.eid}) - $periodStr")
            return file
        }
    }

    /**
     * 2. GENERATE FULL PAYROLL REGISTER PDF
     */
    fun generatePayrollRegisterPdf(
        context: Context,
        companyName: String,
        year: Int,
        month: Int,
        salarySummaries: List<SalarySummary>
    ): File {
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val periodStr = "${monthNames.getOrElse(month - 1) { "" }} $year"
        val file = File(getReportsDir(context), "Payroll_Register_${year}_${month}.pdf")

        try {
            val document = PdfDocument()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val rowsPerPage = 20
        val totalPages = Math.max(1, Math.ceil(salarySummaries.size.toDouble() / rowsPerPage).toInt())
        val totalNetPayout = salarySummaries.sumOf { it.netSalary }
        val totalOtAmount = salarySummaries.sumOf { it.otAmount }

        for (pageIdx in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header
            drawDocumentHeader(
                canvas,
                companyName,
                "PAYROLL REGISTER",
                "$periodStr · Official Salary Register",
                paint
            )

            var y = 84f

            // KPI Overview on Page 1
            if (pageIdx == 0) {
                val boxWidth = (PAGE_WIDTH - 56f - 16f) / 3f

                val kpiBoxes = listOf(
                    Triple("Total Net Payable", formatInr(totalNetPayout), COLOR_ROYAL_AMETHYST),
                    Triple("Total Overtime Paid", formatInr(totalOtAmount), COLOR_CYAN_ACCENT),
                    Triple("Active Staff Count", "${salarySummaries.size} Employees", COLOR_TEXT_DARK)
                )

                kpiBoxes.forEachIndexed { i, (label, value, color) ->
                    val bx = 28f + (i * (boxWidth + 8f))
                    paint.color = COLOR_ROW_ALT
                    canvas.drawRoundRect(bx, y, bx + boxWidth, y + 42f, 6f, 6f, paint)
                    paint.color = COLOR_BORDER
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRoundRect(bx, y, bx + boxWidth, y + 42f, 6f, 6f, paint)
                    paint.style = Paint.Style.FILL

                    paint.color = COLOR_TEXT_MUTED
                    paint.textSize = 8.5f
                    canvas.drawText(label, bx + 10f, y + 16f, paint)

                    paint.color = color
                    paint.textSize = 14f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(value, bx + 10f, y + 34f, paint)
                }

                y += 56f
            }

            // Table Header
            paint.color = COLOR_ROYAL_AMETHYST_DARK
            canvas.drawRect(28f, y, PAGE_WIDTH - 28f, y + 22f, paint)

            paint.color = COLOR_WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            canvas.drawText("EID", 34f, y + 15f, paint)
            canvas.drawText("Employee Name", 75f, y + 15f, paint)
            canvas.drawText("Dept", 175f, y + 15f, paint)
            canvas.drawText("Base Salary", 235f, y + 15f, paint)
            canvas.drawText("Pay Days", 305f, y + 15f, paint)
            canvas.drawText("Earned", 360f, y + 15f, paint)
            canvas.drawText("OT (+)", 415f, y + 15f, paint)
            canvas.drawText("Deduct (-)", 465f, y + 15f, paint)
            canvas.drawText("Net Pay", 520f, y + 15f, paint)

            y += 22f

            // Data rows
            val startIdx = pageIdx * rowsPerPage
            val endIdx = Math.min(salarySummaries.size, startIdx + rowsPerPage)

            for (r in startIdx until endIdx) {
                val s = salarySummaries[r]
                val isAlt = (r % 2 == 1)

                if (isAlt) {
                    paint.color = COLOR_ROW_ALT
                    canvas.drawRect(28f, y, PAGE_WIDTH - 28f, y + 20f, paint)
                }

                paint.color = COLOR_BORDER
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                canvas.drawRect(28f, y, PAGE_WIDTH - 28f, y + 20f, paint)
                paint.style = Paint.Style.FILL

                val totalDeduct = s.lateDeduction + s.loanEmi + s.advanceDeduction

                paint.color = COLOR_TEXT_DARK
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawText(s.employee.eid, 34f, y + 14f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(s.employee.name.take(18), 75f, y + 14f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(s.employee.dept, 175f, y + 14f, paint)
                canvas.drawText(formatInr(s.employee.salary), 235f, y + 14f, paint)
                canvas.drawText("${s.payableDays}d", 305f, y + 14f, paint)
                canvas.drawText(formatInr(s.earnedSalary), 360f, y + 14f, paint)

                paint.color = if (s.otAmount > 0) COLOR_STATUS_GREEN else COLOR_TEXT_MUTED
                canvas.drawText(if (s.otAmount > 0) "+${formatInr(s.otAmount)}" else "—", 415f, y + 14f, paint)

                paint.color = if (totalDeduct > 0) COLOR_STATUS_RED else COLOR_TEXT_MUTED
                canvas.drawText(if (totalDeduct > 0) "-${formatInr(totalDeduct)}" else "—", 465f, y + 14f, paint)

                paint.color = COLOR_ROYAL_AMETHYST
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(formatInr(s.netSalary), 520f, y + 14f, paint)

                y += 20f
            }

            // Summary Signature Block on Last Page
            if (pageIdx == totalPages - 1) {
                y += 24f
                paint.color = COLOR_TEXT_MUTED
                paint.textSize = 9f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                canvas.drawLine(40f, y + 40f, 180f, y + 40f, paint)
                canvas.drawText("Prepared by (HR Manager)", 40f, y + 54f, paint)

                canvas.drawLine(PAGE_WIDTH - 180f, y + 40f, PAGE_WIDTH - 40f, y + 40f, paint)
                canvas.drawText("Authorized Signatory (Director)", PAGE_WIDTH - 180f, y + 54f, paint)
            }

            drawDocumentFooter(canvas, pageIdx + 1, totalPages, paint)
            document.finishPage(page)
        }

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            return file
        } catch (t: Throwable) {
            writeFallbackPdf(file, "PAYROLL REGISTER - $companyName", "Period: $periodStr · Total Employees: ${salarySummaries.size}")
            return file
        }
    }

    /**
     * 3. GENERATE FORMAL EMPLOYEE SALARY SLIP PDF
     */
    fun generateSalarySlipPdf(
        context: Context,
        companyName: String,
        year: Int,
        month: Int,
        salarySummary: SalarySummary
    ): File {
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val periodStr = "${monthNames.getOrElse(month - 1) { "" }} $year"
        val emp = salarySummary.employee
        val file = File(getReportsDir(context), "Payslip_${emp.eid}_${year}_${month}.pdf")

        try {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header
        drawDocumentHeader(
            canvas,
            companyName,
            "PAYSLIP / SALARY SLIP",
            "Pay Period: $periodStr",
            paint
        )

        var y = 84f

        // Employee Info Card
        paint.color = COLOR_JET_ELEVATED
        canvas.drawRoundRect(28f, y, PAGE_WIDTH - 28f, y + 64f, 8f, 8f, paint)

        paint.color = COLOR_ELECTRIC_CYAN
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${emp.name}  (${emp.eid})", 42f, y + 22f, paint)

        paint.color = COLOR_WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Designation: ${emp.desig}", 42f, y + 38f, paint)
        canvas.drawText("Department: ${emp.dept}", 42f, y + 52f, paint)

        canvas.drawText("Date of Joining: ${emp.joinDate}", 300f, y + 38f, paint)
        canvas.drawText("Working Days: ${salarySummary.monthSummary.workingDays}  |  Paid Days: ${salarySummary.payableDays}", 300f, y + 52f, paint)

        y += 80f

        // Two Column Breakdown: Earnings vs Deductions
        val colWidth = (PAGE_WIDTH - 56f - 14f) / 2f
        val leftX = 28f
        val rightX = leftX + colWidth + 14f

        // Column Headers
        paint.color = COLOR_ROYAL_AMETHYST_DARK
        canvas.drawRoundRect(leftX, y, leftX + colWidth, y + 24f, 4f, 4f, paint)
        paint.color = COLOR_WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EARNINGS", leftX + 12f, y + 16f, paint)
        canvas.drawText("AMOUNT (₹)", leftX + colWidth - 80f, y + 16f, paint)

        paint.color = COLOR_ROYAL_AMETHYST_DARK
        canvas.drawRoundRect(rightX, y, rightX + colWidth, y + 24f, 4f, 4f, paint)
        paint.color = COLOR_WHITE
        canvas.drawText("DEDUCTIONS", rightX + 12f, y + 16f, paint)
        canvas.drawText("AMOUNT (₹)", rightX + colWidth - 80f, y + 16f, paint)

        y += 30f

        val earnings = listOf(
            "Basic Salary Earned" to formatInr(salarySummary.earnedSalary),
            "Overtime Pay (${ApsaRoxRepository.minutesToTime(salarySummary.monthSummary.otMinutes)}h)" to formatInr(salarySummary.otAmount),
            "Paid Leave Days (${salarySummary.paidLeaveDays}d)" to "Included"
        )

        val deductions = listOf(
            "Late Fine (${salarySummary.monthSummary.lateDays} days)" to formatInr(salarySummary.lateDeduction),
            "Loan Monthly EMI" to formatInr(salarySummary.loanEmi),
            "Advance Payment Recovery" to formatInr(salarySummary.advanceDeduction)
        )

        var rowY = y
        val maxRows = Math.max(earnings.size, deductions.size)
        for (i in 0 until maxRows) {
            val earn = earnings.getOrNull(i)
            val ded = deductions.getOrNull(i)

            // Earnings row
            if (earn != null) {
                paint.color = COLOR_ROW_ALT
                canvas.drawRect(leftX, rowY, leftX + colWidth, rowY + 22f, paint)
                paint.color = COLOR_TEXT_DARK
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(earn.first, leftX + 10f, rowY + 15f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(earn.second, leftX + colWidth - 80f, rowY + 15f, paint)
            }

            // Deductions row
            if (ded != null) {
                paint.color = COLOR_ROW_ALT
                canvas.drawRect(rightX, rowY, rightX + colWidth, rowY + 22f, paint)
                paint.color = COLOR_TEXT_DARK
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText(ded.first, rightX + 10f, rowY + 15f, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(ded.second, rightX + colWidth - 80f, rowY + 15f, paint)
            }

            rowY += 24f
        }

        y = rowY + 12f

        // Subtotals
        val totalEarn = salarySummary.earnedSalary + salarySummary.otAmount
        val totalDed = salarySummary.lateDeduction + salarySummary.loanEmi + salarySummary.advanceDeduction

        paint.color = COLOR_BORDER
        canvas.drawRect(leftX, y, leftX + colWidth, y + 22f, paint)
        paint.color = COLOR_TEXT_DARK
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Total Gross Earnings", leftX + 10f, y + 15f, paint)
        canvas.drawText(formatInr(totalEarn), leftX + colWidth - 80f, y + 15f, paint)

        paint.color = COLOR_BORDER
        canvas.drawRect(rightX, y, rightX + colWidth, y + 22f, paint)
        paint.color = COLOR_TEXT_DARK
        canvas.drawText("Total Deductions", rightX + 10f, y + 15f, paint)
        canvas.drawText(formatInr(totalDed), rightX + colWidth - 80f, y + 15f, paint)

        y += 44f

        // Big NET PAY Banner in JET BLACK with ROYAL AMETHYST & ELECTRIC CYAN
        paint.color = COLOR_JET_BLACK
        canvas.drawRoundRect(28f, y, PAGE_WIDTH - 28f, y + 54f, 8f, 8f, paint)

        paint.color = COLOR_ELECTRIC_CYAN
        paint.strokeWidth = 2f
        canvas.drawLine(28f, y, PAGE_WIDTH - 28f, y, paint)

        paint.color = COLOR_WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NET SALARY PAYABLE", 42f, y + 24f, paint)
        paint.textSize = 9f
        paint.color = COLOR_TEXT_MUTED
        canvas.drawText("Transferred to Employee Bank Account", 42f, y + 42f, paint)

        paint.color = COLOR_ELECTRIC_CYAN
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val netStr = formatInr(salarySummary.netSalary)
        val netWidth = paint.measureText(netStr)
        canvas.drawText(netStr, PAGE_WIDTH - 42f - netWidth, y + 36f, paint)

        // Signatures at bottom
        y += 120f
        paint.color = COLOR_TEXT_MUTED
        paint.strokeWidth = 1f
        canvas.drawLine(50f, y, 190f, y, paint)
        canvas.drawText("Employee Signature", 50f, y + 16f, paint)

        canvas.drawLine(PAGE_WIDTH - 190f, y, PAGE_WIDTH - 50f, y, paint)
        canvas.drawText("Authorized Signatory (HR)", PAGE_WIDTH - 190f, y + 16f, paint)

        drawDocumentFooter(canvas, 1, 1, paint)
        document.finishPage(page)

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            return file
        } catch (t: Throwable) {
            writeFallbackPdf(file, "SALARY SLIP - $companyName", "${emp.name} (${emp.eid}) · $periodStr")
            return file
        }
    }

    /**
     * 4. GENERATE CUSTOM REPORT PDF (Daily, Monthly, OT, Late, Absence, etc.)
     */
    fun generateCustomReportPdf(
        context: Context,
        companyName: String,
        reportTitle: String,
        dateRange: String,
        headers: List<String>,
        rows: List<List<String>>
    ): File {
        val cleanName = reportTitle.replace(" ", "_").replace("/", "_")
        val file = File(getReportsDir(context), "${cleanName}_Report.pdf")

        try {
            val document = PdfDocument()
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val rowsPerPage = 26
        val totalPages = Math.max(1, Math.ceil(rows.size.toDouble() / rowsPerPage).toInt())

        val colCount = Math.max(1, headers.size)
        val colWidth = (PAGE_WIDTH - 56f) / colCount.toFloat()

        for (pageIdx in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIdx + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Header
            drawDocumentHeader(
                canvas,
                companyName,
                reportTitle.uppercase(),
                "Range: $dateRange · Records: ${rows.size}",
                paint
            )

            var y = 84f

            // Table Header Bar
            paint.color = COLOR_ROYAL_AMETHYST_DARK
            canvas.drawRect(28f, y, PAGE_WIDTH - 28f, y + 22f, paint)

            paint.color = COLOR_WHITE
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

            headers.forEachIndexed { i, h ->
                val x = 28f + (i * colWidth) + 4f
                canvas.drawText(h.take(16), x, y + 15f, paint)
            }

            y += 22f

            // Rows
            val startIdx = pageIdx * rowsPerPage
            val endIdx = Math.min(rows.size, startIdx + rowsPerPage)

            for (r in startIdx until endIdx) {
                val row = rows[r]
                val isAlt = (r % 2 == 1)

                if (isAlt) {
                    paint.color = COLOR_ROW_ALT
                    canvas.drawRect(28f, y, PAGE_WIDTH - 28f, y + 20f, paint)
                }

                paint.color = COLOR_BORDER
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 0.5f
                canvas.drawRect(28f, y, PAGE_WIDTH - 28f, y + 20f, paint)
                paint.style = Paint.Style.FILL

                paint.color = COLOR_TEXT_DARK
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                row.forEachIndexed { i, cell ->
                    val x = 28f + (i * colWidth) + 4f
                    val displayCell = cell.take(18)
                    canvas.drawText(displayCell, x, y + 14f, paint)
                }

                y += 20f
            }

            drawDocumentFooter(canvas, pageIdx + 1, totalPages, paint)
            document.finishPage(page)
        }

            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()
            return file
        } catch (t: Throwable) {
            writeFallbackPdf(file, "$reportTitle - $companyName", "Range: $dateRange · Records: ${rows.size}")
            return file
        }
    }

    /**
     * Launch View / Share Intent for the generated PDF
     */
    fun openOrSharePdf(context: Context, file: File, chooserTitle: String = "Open PDF Report") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Fallback: standard share
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(sendIntent, chooserTitle))
        }
    }
}
