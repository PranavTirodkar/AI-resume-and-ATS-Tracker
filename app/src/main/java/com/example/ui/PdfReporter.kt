package com.example.ui

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.ResumeProfile
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReporter {

    /**
     * Helper to draw text layout on a specific canvas coordinate and return the final height consumed.
     */
    private fun drawStaticText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        paint: TextPaint,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Int {
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return 0
        
        val builder = StaticLayout.Builder.obtain(cleanText, 0, cleanText.length, paint, width)
            .setAlignment(align)
            .setLineSpacing(0f, 1.15f)
            .setIncludePad(false)
        val layout = builder.build()
        
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return layout.height
    }

    /**
     * Main builder generating the professional two-page ATS reports.
     */
    fun generateAndSharePdf(context: Context, profile: ResumeProfile) {
        val pdfDocument = PdfDocument()
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        val dateString = dateFormat.format(Date(profile.timestamp))

        // Colors Setup (Slate Blue Theme)
        val colorPrimary = Color.parseColor("#1E293B") // Deep slate
        val colorAccentGreen = Color.parseColor("#10B981") // Success Green
        val colorAccentAmber = Color.parseColor("#F59E0B") // Warning Orange
        val colorAccentRed = Color.parseColor("#EF4444") // Severe Red
        val colorMutedText = Color.parseColor("#475569") // Gray subtext
        val colorLightBg = Color.parseColor("#F8FAFC") // Off-white
        val colorDivider = Color.parseColor("#E2E8F0") // Line split grey

        // Determine score color
        val scoreColor = when {
            profile.atsScore >= 75 -> colorAccentGreen
            profile.atsScore >= 60 -> colorAccentAmber
            else -> colorAccentRed
        }

        // --- PAGE 1: EXECUTIVE BRIEF & SKILL INDEXES ---
        val page1Info = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size Specs
        val page1 = pdfDocument.startPage(page1Info)
        val canvas1 = page1.canvas

        // Background Tint
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas1.drawRect(0f, 0f, 595f, 842f, bgPaint)

        // Draw elegant Top Banner Accent Band
        val headerPaint = Paint().apply {
            color = colorPrimary
            style = Paint.Style.FILL
        }
        canvas1.drawRect(0f, 0f, 595f, 90f, headerPaint)

        // Title text inside Banner
        val textPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 21f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas1.drawText("RESUME ATS OPTIMIZATION REPORT", 30f, 44f, textPaint)

        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textPaint.color = Color.parseColor("#94A3B8") // Pale soft gray
        canvas1.drawText("AI COMPATIBILITY MATRIX & SKILLS RECRUITMENT BENCHMARK", 30f, 65f, textPaint)

        // Back to high-contrast styles for normal text
        val labelPaint = TextPaint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = colorPrimary
            isAntiAlias = true
        }
        val valuePaint = TextPaint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            color = Color.BLACK
            isAntiAlias = true
        }

        // Document Details Box
        val detailsBgPaint = Paint().apply {
            color = colorLightBg
            style = Paint.Style.FILL
        }
        canvas1.drawRoundRect(RectF(30f, 105f, 565f, 160f), 8f, 8f, detailsBgPaint)

        canvas1.drawText("Target Job Role:", 45f, 126f, labelPaint)
        canvas1.drawText(profile.targetJobTitle.ifBlank { "N/A" }, 155f, 126f, valuePaint)

        canvas1.drawText("Report Dated:", 45f, 146f, labelPaint)
        canvas1.drawText(dateString, 155f, 146f, valuePaint)

        // Score Circular Ring Gauge (Visual Masterpiece)
        val ringCenterY = 225f
        val ringCenterX = 110f
        val ringRadius = 45f

        val ringTrackPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 9f
            isAntiAlias = true
        }
        canvas1.drawCircle(ringCenterX, ringCenterY, ringRadius, ringTrackPaint)

        val ringFillPaint = Paint().apply {
            color = scoreColor
            style = Paint.Style.STROKE
            strokeWidth = 9f
            strokeCap = Paint.Cap.ROUND
            isAntiAlias = true
        }
        val sweepAngle = (profile.atsScore.toFloat() / 100f) * 360f
        canvas1.drawArc(
            RectF(ringCenterX - ringRadius, ringCenterY - ringRadius, ringCenterX + ringRadius, ringCenterY + ringRadius),
            -90f,
            sweepAngle,
            false,
            ringFillPaint
        )

        // Score Text directly inside Ring
        val ringTextPaint = TextPaint().apply {
            color = colorPrimary
            textSize = 21f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas1.drawText("${profile.atsScore}%", ringCenterX, ringCenterY + 7f, ringTextPaint)

        // Category Assessment Box to the Right of Ring
        val blockTitlePaint = TextPaint().apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            color = colorPrimary
            isAntiAlias = true
        }
        canvas1.drawText("Overall ATS Optimization Index", 180f, 205f, blockTitlePaint)

        val descPaint = TextPaint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            color = colorMutedText
            isAntiAlias = true
        }
        val feedbackSummaryText = when {
            profile.atsScore >= 75 -> "Excellent match. Your resume aligns tightly with target recruiter metrics. It shows outstanding keyword usage and dense functional engineering vocabulary with insignificant compliance barriers."
            profile.atsScore >= 60 -> "Good starter score. Consider closed-loop revisions. Modest keyword mapping improvements and structured phrasing modifications will push you to high-priority recruiter dashboards."
            else -> "Warning index. High risk of immediate automated ATS triage filter exclusion. Critical gaps exist in mandatory technical keywords, active experience mapping, and layout format factors."
        }
        drawStaticText(canvas1, feedbackSummaryText, 180f, 218f, 385, descPaint)

        // Horizontal Line divider
        val linePaint = Paint().apply {
            color = colorDivider
            strokeWidth = 1f
        }
        canvas1.drawLine(30f, 290f, 565f, 290f, linePaint)

        // --- SECTION A: RECONCILED SKILLS DETECTED ---
        canvas1.drawText("Reconciled Skills Detected in Resume", 30f, 317f, blockTitlePaint)
        
        val sectionTextPaint = TextPaint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            color = Color.parseColor("#0F172A")
            isAntiAlias = true
        }

        val skillsText = if (profile.parsedSkills.isBlank()) {
            "No core skills were successfully extracted. Please check format density."
        } else {
            profile.parsedSkills.replace("\n", ", ")
        }
        val skillsHeight = drawStaticText(canvas1, skillsText, 30f, 330f, 535, sectionTextPaint)

        // --- SECTION B: CRITICAL SKILL GAPS (MISSING SKILLS) ---
        val gapsY = 350f + skillsHeight
        canvas1.drawLine(30f, gapsY - 14f, 565f, gapsY - 14f, linePaint)
        canvas1.drawText("Critical Skill Gaps Identified (Missing Keywords)", 30f, gapsY + 12f, blockTitlePaint)

        val warningCardPaint = Paint().apply {
            color = Color.argb(20, Color.red(colorAccentAmber), Color.green(colorAccentAmber), Color.blue(colorAccentAmber))
            style = Paint.Style.FILL
        }
        
        val missingSkillsClean = profile.missingSkills.trim()
        val missingSkillsRenderText = if (missingSkillsClean.isBlank()) {
            "Zero severe keyword matches missing. Highly qualified for the selected parameters!"
        } else {
            missingSkillsClean
        }

        // Draw a light amber alert fill for missing skills
        val textGapHeight = drawStaticText(canvas1, missingSkillsRenderText, 45f, gapsY + 34f, 510, sectionTextPaint)
        canvas1.drawRoundRect(
            RectF(30f, gapsY + 24f, 565f, gapsY + 44f + textGapHeight),
            6f, 6f,
            warningCardPaint
        )

        // Page 1 Footer
        val footerPaint = TextPaint().apply {
            textSize = 9f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
            color = Color.parseColor("#94A3B8")
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas1.drawText("AI ATS Resume Prep Engine • Page 1 of 2", 297f, 815f, footerPaint)

        pdfDocument.finishPage(page1)

        // --- PAGE 2: STRUCTURAL ANALYSIS & ROADMAP STRATEGY ---
        val page2Info = PdfDocument.PageInfo.Builder(595, 842, 2).create()
        val page2 = pdfDocument.startPage(page2Info)
        val canvas2 = page2.canvas

        canvas2.drawRect(0f, 0f, 595f, 842f, bgPaint)
        
        // Header Banner Page 2 (Thin Elegant Slate Style)
        canvas2.drawRect(0f, 0f, 595f, 60f, headerPaint)
        
        textPaint.color = Color.WHITE
        textPaint.textSize = 15f
        textPaint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        canvas2.drawText("STRUCTURAL RECONSTRUCTION & STRATEGIC HIGHLIGHTS", 30f, 37f, textPaint)

        // Section I: Dynamic Structure Issues
        var currentY = 90f
        canvas2.drawText("Resume Structure & Parsing Diagnostics", 30f, currentY, blockTitlePaint)
        currentY += 14f

        val rawStructureIssues = profile.resumeStructureIssues.trim()
        val structureRenderText = if (rawStructureIssues.isBlank()) {
            "✓ Profile structure meets standard ATS industry parsing protocols.\n✓ Header hierarchy, margins, and section orders are clear and legible."
        } else {
            rawStructureIssues
        }
        val structureHeight = drawStaticText(canvas2, structureRenderText, 30f, currentY + 4f, 535, sectionTextPaint)
        currentY += structureHeight + 35f

        canvas2.drawLine(30f, currentY - 18f, 565f, currentY - 18f, linePaint)

        // Section II: Custom Learning Roadmap
        canvas2.drawText("Dynamic Upskilling Roadmap Specs", 30f, currentY, blockTitlePaint)
        currentY += 14f

        val roadmapClean = profile.learningRoadmap.trim()
        val roadmapRenderText = if (roadmapClean.isBlank()) {
            "Select Career & Roadmap Tab inside the main workspace to generate interactive syllabus milestones."
        } else {
            roadmapClean
        }
        val roadmapHeight = drawStaticText(canvas2, roadmapRenderText, 30f, currentY + 4f, 535, sectionTextPaint)
        currentY += roadmapHeight + 35f

        canvas2.drawLine(30f, currentY - 18f, 565f, currentY - 18f, linePaint)

        // Section III: Recommended Projects & Certifications
        canvas2.drawText("Project & Certification Closed-Loop Targets", 30f, currentY, blockTitlePaint)
        currentY += 14f

        val combinedTargetString = StringBuilder().apply {
            if (profile.certificationSuggestions.isNotBlank()) {
                append("RECOMMENDED CERTIFICATIONS:\n")
                append(profile.certificationSuggestions.trim())
                append("\n\n")
            }
            if (profile.projectSuggestions.isNotBlank()) {
                append("RECOMMENDED HANDS-ON PROJECTS:\n")
                append(profile.projectSuggestions.trim())
            }
        }.toString().trim()

        val targetedContent = combinedTargetString.ifBlank {
            "Compile ATS results to view customized learning exercises."
        }
        drawStaticText(canvas2, targetedContent, 30f, currentY + 4f, 535, sectionTextPaint)

        // Page 2 Footer
        canvas2.drawText("AI ATS Resume Prep Engine • Page 2 of 2", 297f, 815f, footerPaint)

        pdfDocument.finishPage(page2)

        // Attempt File Saving & Share Trigger
        try {
            val fileName = "ATS_Score_Report_${System.currentTimeMillis()}.pdf"
            val cacheFile = File(context.cacheDir, fileName)
            val fileOut = FileOutputStream(cacheFile)
            pdfDocument.writeTo(fileOut)
            fileOut.flush()
            fileOut.close()

            // Trigger Share Intent with URI FileProvider authority
            val authorityString = "${context.packageName}.provider"
            val contentUri: Uri = FileProvider.getUriForFile(context, authorityString, cacheFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Resume ATS Optimization Report - ${profile.targetJobTitle}")
                putExtra(Intent.EXTRA_TEXT, "Hello! Enclosed is my parsed ATS score analyzer and skills gap report in PDF format generated via AI Studio Resume Prep Dashboard.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooserIntent = Intent.createChooser(shareIntent, "Save or Share ATS Report PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooserIntent)

        } catch (e: Exception) {
            Toast.makeText(context, "Failed to build report. Reason: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        } finally {
            pdfDocument.close()
        }
    }
}
