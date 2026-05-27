package com.example.project_android.utils

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DashboardExcelExporter {

    fun createBytes(dashboard: JSONObject): ByteArray {
        return ByteArrayOutputStream().use { output ->
            write(output, dashboard)
            output.toByteArray()
        }
    }

    fun write(output: OutputStream, dashboard: JSONObject) {
        ZipOutputStream(output).use { zip ->
            zip.putXml("[Content_Types].xml", contentTypesXml())
            zip.putXml("_rels/.rels", rootRelationshipsXml())
            zip.putXml("xl/workbook.xml", workbookXml())
            zip.putXml("xl/_rels/workbook.xml.rels", workbookRelationshipsXml())
            zip.putXml("xl/styles.xml", stylesXml())
            zip.putXml("xl/worksheets/sheet1.xml", worksheetXml(dashboard))
        }
    }

    private fun worksheetXml(dashboard: JSONObject): String {
        val rows = mutableListOf<String>()
        var rowNumber = 1

        fun row(vararg cells: String) {
            rows += "<row r=\"$rowNumber\">${cells.joinToString("")}</row>"
            rowNumber++
        }

        row(textCell("A1", "B\u00c1O C\u00c1O T\u1ed4NG QUAN HI\u1ec6U SU\u1ea4T", STYLE_TITLE))
        row(textCell("A2", "Ng\u00e0y xu\u1ea5t: ${timestamp()}"))
        row(textCell("A3", "Doanh thu v\u00e0 t\u0103ng tr\u01b0\u1edfng \u0111\u01b0\u1ee3c t\u00ednh theo qu\u00fd hi\u1ec7n t\u1ea1i."))
        row()

        row(textCell("A5", "CH\u1ec8 S\u1ed0 T\u1ed4NG QUAN", STYLE_SECTION))
        row(textCell("A6", "Ch\u1ec9 s\u1ed1", STYLE_HEADER), textCell("B6", "Gi\u00e1 tr\u1ecb", STYLE_HEADER))
        row(textCell("A7", "T\u1ed5ng doanh thu"), numberCell("B7", dashboard.optDouble("revenue"), STYLE_CURRENCY))
        row(textCell("A8", "T\u0103ng tr\u01b0\u1edfng so v\u1edbi qu\u00fd tr\u01b0\u1edbc"), numberCell("B8", dashboard.optDouble("growthPercent") / 100, STYLE_PERCENT))
        row(textCell("A9", "\u0110\u01a1n \u0111ang thu\u00ea"), numberCell("B9", dashboard.optLong("activeOrderCount").toDouble()))
        row(textCell("A10", "T\u1ed5ng \u0111\u01a1n h\u00e0ng"), numberCell("B10", dashboard.optLong("orderCount").toDouble()))
        row(textCell("A11", "S\u1ea3n ph\u1ea9m \u0111ang thu\u00ea"), numberCell("B11", dashboard.optLong("rentedProductCount").toDouble()))
        row()

        row(textCell("A13", "DOANH THU THEO TH\u00c1NG", STYLE_SECTION))
        row(textCell("A14", "Th\u00e1ng", STYLE_HEADER), textCell("B14", "Doanh thu", STYLE_HEADER))
        val series = dashboard.optJSONArray("revenueSeries")
        if (series == null || series.length() == 0) {
            row(textCell("A15", "Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u"), numberCell("B15", 0.0, STYLE_CURRENCY))
        } else {
            for (index in 0 until series.length()) {
                val item = series.optJSONObject(index)
                val line = 15 + index
                row(
                    textCell("A$line", item?.optString("label", "-") ?: "-"),
                    numberCell("B$line", item?.optDouble("amount", 0.0) ?: series.optDouble(index), STYLE_CURRENCY)
                )
            }
        }

        rowNumber = maxOf(rowNumber + 1, 24)
        row(textCell("A$rowNumber", "S\u1ea2N PH\u1ea8M \u0110\u01af\u1ee2C THU\u00ca NHI\u1ec0U NH\u1ea4T", STYLE_SECTION))
        val topHeaderRow = rowNumber
        row(
            textCell("A$topHeaderRow", "M\u00e3 s\u1ea3n ph\u1ea9m", STYLE_HEADER),
            textCell("B$topHeaderRow", "T\u00ean s\u1ea3n ph\u1ea9m", STYLE_HEADER),
            textCell("C$topHeaderRow", "Danh m\u1ee5c / Size", STYLE_HEADER),
            textCell("D$topHeaderRow", "L\u01b0\u1ee3t thu\u00ea", STYLE_HEADER),
            textCell("E$topHeaderRow", "Doanh thu th\u1ef1c thu", STYLE_HEADER)
        )
        val products = dashboard.optJSONArray("topProducts")
        if (products == null || products.length() == 0) {
            row(textCell("A$rowNumber", "Ch\u01b0a c\u00f3 d\u1eef li\u1ec7u"))
        } else {
            for (index in 0 until products.length()) {
                val product = products.optJSONObject(index) ?: continue
                val line = rowNumber
                row(
                    textCell("A$line", product.optString("id", "--")),
                    textCell("B$line", product.optString("name", "--")),
                    textCell("C$line", "${product.optString("category", "--")} / ${product.optString("size", "--")}"),
                    numberCell("D$line", product.optLong("rentalCount").toDouble()),
                    numberCell("E$line", product.optDouble("generatedRevenue"), STYLE_CURRENCY)
                )
            }
        }

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <cols>
                    <col min="1" max="1" width="30" customWidth="1"/>
                    <col min="2" max="2" width="34" customWidth="1"/>
                    <col min="3" max="3" width="28" customWidth="1"/>
                    <col min="4" max="5" width="20" customWidth="1"/>
                </cols>
                <sheetData>${rows.joinToString("")}</sheetData>
                <mergeCells count="3">
                    <mergeCell ref="A1:E1"/>
                    <mergeCell ref="A5:E5"/>
                    <mergeCell ref="A${topHeaderRow - 1}:E${topHeaderRow - 1}"/>
                </mergeCells>
            </worksheet>
        """.trimIndent()
    }

    private fun textCell(reference: String, value: String, style: Int = STYLE_NORMAL): String {
        return "<c r=\"$reference\" s=\"$style\" t=\"inlineStr\"><is><t>${xmlEscape(value)}</t></is></c>"
    }

    private fun numberCell(reference: String, value: Double, style: Int = STYLE_NORMAL): String {
        return "<c r=\"$reference\" s=\"$style\"><v>$value</v></c>"
    }

    private fun timestamp(): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN")).format(Date())
    }

    private fun xmlEscape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun ZipOutputStream.putXml(path: String, content: String) {
        putNextEntry(ZipEntry(path))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
            <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
            <Default Extension="xml" ContentType="application/xml"/>
            <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
            <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
            <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
    """.trimIndent()

    private fun rootRelationshipsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun workbookXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
            <sheets>
                <sheet name="Tong quan" sheetId="1" r:id="rId1"/>
            </sheets>
        </workbook>
    """.trimIndent()

    private fun workbookRelationshipsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
            <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
            <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun stylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
            <numFmts count="1"><numFmt numFmtId="164" formatCode="#,##0 &quot;VND&quot;"/></numFmts>
            <fonts count="4">
                <font><sz val="11"/><name val="Calibri"/></font>
                <font><b/><sz val="16"/><color rgb="FF4430E8"/><name val="Calibri"/></font>
                <font><b/><sz val="11"/><name val="Calibri"/></font>
                <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
            </fonts>
            <fills count="3">
                <fill><patternFill patternType="none"/></fill>
                <fill><patternFill patternType="gray125"/></fill>
                <fill><patternFill patternType="solid"><fgColor rgb="FF4430E8"/><bgColor indexed="64"/></patternFill></fill>
            </fills>
            <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
            <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
            <cellXfs count="6">
                <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                <xf numFmtId="0" fontId="3" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
                <xf numFmtId="164" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
                <xf numFmtId="10" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
            </cellXfs>
        </styleSheet>
    """.trimIndent()

    private const val STYLE_NORMAL = 0
    private const val STYLE_TITLE = 1
    private const val STYLE_SECTION = 2
    private const val STYLE_HEADER = 3
    private const val STYLE_CURRENCY = 4
    private const val STYLE_PERCENT = 5
}
