package com.example.data.excel

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.example.core.MoneyUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import org.xmlpull.v1.XmlPullParser

data class ParsedExcelRow(
  val rowNumber: Int,
  val name: String,
  val priceUsd: BigDecimal,
)

data class ExcelParseError(
  val rowNumber: Int,
  val reason: String,
)

data class ExcelParseResult(
  val validRows: List<ParsedExcelRow>,
  val errors: List<ExcelParseError>,
  val totalRowsScanned: Int,
)

enum class DuplicateResolution {
  SKIP,
  UPDATE,
}

data class ExcelImportSummary(
  val totalRowsScanned: Int,
  val addedCount: Int,
  val updatedCount: Int,
  val skippedDuplicatesCount: Int,
  val invalidCount: Int,
  val errors: List<ExcelParseError>,
)

object XlsxProductImporter {

  fun parseFromUri(context: Context, uri: Uri): ExcelParseResult {
    val inputStream =
      context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("تعذر فتح ملف Excel المحدد")
    return inputStream.use { parseFromStream(it) }
  }

  fun parseFromStream(stream: InputStream): ExcelParseResult {
    var sharedStringsXml: ByteArray? = null
    var firstSheetXml: ByteArray? = null
    val sheetEntries = mutableMapOf<String, ByteArray>()

    ZipInputStream(stream).use { zis ->
      var entry: ZipEntry? = zis.nextEntry
      while (entry != null) {
        val name = entry.name
        if (!entry.isDirectory) {
          when {
            name.equals("xl/sharedStrings.xml", ignoreCase = true) -> {
              sharedStringsXml = zis.readBytes()
            }
            name.startsWith("xl/worksheets/sheet", ignoreCase = true) &&
              name.endsWith(".xml", ignoreCase = true) -> {
              sheetEntries[name] = zis.readBytes()
            }
          }
        }
        zis.closeEntry()
        entry = zis.nextEntry
      }
    }

    firstSheetXml =
      sheetEntries["xl/worksheets/sheet1.xml"]
        ?: sheetEntries.entries.sortedBy { it.key }.firstOrNull()?.value
        ?: throw IllegalArgumentException("الملف لا يحتوي على ورقة عمل صالحة (sheet1.xml)")

    val sharedStrings =
      sharedStringsXml?.let { parseSharedStrings(ByteArrayInputStream(it)) } ?: emptyList()

    return parseSheetRows(ByteArrayInputStream(firstSheetXml), sharedStrings)
  }

  private fun parseSharedStrings(stream: InputStream): List<String> {
    val result = mutableListOf<String>()
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(stream, "UTF-8")

    var inSi = false
    var inText = false
    val currentText = StringBuilder()

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
      when (event) {
        XmlPullParser.START_TAG -> {
          when (parser.name) {
            "si" -> {
              inSi = true
              currentText.clear()
            }
            "t" -> if (inSi) inText = true
          }
        }
        XmlPullParser.TEXT -> {
          if (inSi && inText) {
            currentText.append(parser.text ?: "")
          }
        }
        XmlPullParser.END_TAG -> {
          when (parser.name) {
            "t" -> inText = false
            "si" -> {
              result.add(currentText.toString())
              inSi = false
            }
          }
        }
      }
      event = parser.next()
    }
    return result
  }

  private fun parseSheetRows(
    stream: InputStream,
    sharedStrings: List<String>,
  ): ExcelParseResult {
    val parser = Xml.newPullParser()
    parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
    parser.setInput(stream, "UTF-8")

    val rawRows = mutableListOf<Pair<Int, Map<Int, String>>>()
    var currentRowNumber = 0
    var currentRowCells = mutableMapOf<Int, String>()
    var currentColIndex = -1
    var currentCellType = ""
    var inValue = false
    var inInlineStr = false
    val valueBuilder = StringBuilder()

    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
      when (event) {
        XmlPullParser.START_TAG -> {
          when (parser.name) {
            "row" -> {
              currentRowNumber = parser.getAttributeValue(null, "r")?.toIntOrNull()
                ?: (rawRows.size + 1)
              currentRowCells = mutableMapOf()
            }
            "c" -> {
              val ref = parser.getAttributeValue(null, "r") ?: ""
              currentColIndex = columnIndexFromCellRef(ref, currentRowCells.size)
              currentCellType = parser.getAttributeValue(null, "t") ?: ""
              valueBuilder.clear()
            }
            "v" -> inValue = true
            "is" -> inInlineStr = true
            "t" -> if (inInlineStr) inValue = true
          }
        }
        XmlPullParser.TEXT -> {
          if (inValue) {
            valueBuilder.append(parser.text ?: "")
          }
        }
        XmlPullParser.END_TAG -> {
          when (parser.name) {
            "v" -> inValue = false
            "t" -> if (inInlineStr) inValue = false
            "is" -> inInlineStr = false
            "c" -> {
              val rawVal = valueBuilder.toString().trim()
              val resolved =
                when (currentCellType) {
                  "s" -> {
                    val idx = rawVal.toIntOrNull()
                    if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                  }
                  else -> rawVal
                }
              if (currentColIndex >= 0 && resolved.isNotEmpty()) {
                currentRowCells[currentColIndex] = resolved.trim()
              }
              currentColIndex = -1
            }
            "row" -> {
              if (currentRowCells.isNotEmpty()) {
                rawRows.add(currentRowNumber to currentRowCells)
              }
            }
          }
        }
      }
      event = parser.next()
    }

    val validRows = mutableListOf<ParsedExcelRow>()
    val errors = mutableListOf<ExcelParseError>()
    var scannedCount = 0

    for ((index, pair) in rawRows.withIndex()) {
      val (rowNum, cells) = pair
      val col1Name = cells[0]?.trim().orEmpty()
      val col2PriceRaw = cells[1]?.trim().orEmpty()

      // Skip header row if first row has non-numeric second column
      if (index == 0 && MoneyUtils.parseUsdOrNull(col2PriceRaw) == null) {
        continue
      }

      scannedCount++
      if (col1Name.isEmpty()) {
        errors.add(ExcelParseError(rowNum, "اسم الصنف فارغ في العمود الأول"))
        continue
      }
      val price = MoneyUtils.parseUsdOrNull(col2PriceRaw)
      if (price == null || price <= BigDecimal.ZERO) {
        errors.add(ExcelParseError(rowNum, "السعر بالدولار غير صالح ($col2PriceRaw)"))
        continue
      }
      validRows.add(ParsedExcelRow(rowNumber = rowNum, name = col1Name, priceUsd = price))
    }

    return ExcelParseResult(
      validRows = validRows,
      errors = errors,
      totalRowsScanned = scannedCount,
    )
  }

  private fun columnIndexFromCellRef(ref: String, fallbackIndex: Int): Int {
    if (ref.isEmpty()) return fallbackIndex
    var index = 0
    var hasLetters = false
    for (ch in ref) {
      if (ch in 'A'..'Z' || ch in 'a'..'z') {
        hasLetters = true
        index = index * 26 + (ch.uppercaseChar() - 'A' + 1)
      } else {
        break
      }
    }
    return if (hasLetters) index - 1 else fallbackIndex
  }

  /**
   * Builds a valid minimal .xlsx workbook containing product columns (Name, Price USD).
   * Useful for generating exportable Excel templates or unit testing.
   */
  fun buildWorkbookBytes(rows: List<Pair<String, String>>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zos ->
      fun writeEntry(path: String, content: String) {
        zos.putNextEntry(ZipEntry(path))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
      }

      writeEntry(
        "[Content_Types].xml",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""",
      )

      writeEntry(
        "_rels/.rels",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""",
      )

      writeEntry(
        "xl/workbook.xml",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Products" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""",
      )

      writeEntry(
        "xl/_rels/workbook.xml.rels",
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""",
      )

      val sheetBuilder = StringBuilder()
      sheetBuilder.append(
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>"""
      )
      sheetBuilder.append(
        """<row r="1"><c r="A1" t="inlineStr"><is><t>اسم الصنف</t></is></c><c r="B1" t="inlineStr"><is><t>السعر بالدولار</t></is></c></row>"""
      )
      rows.forEachIndexed { idx, (name, price) ->
        val r = idx + 2
        val escapedName =
          name.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        sheetBuilder.append(
          """<row r="$r"><c r="A$r" t="inlineStr"><is><t>$escapedName</t></is></c><c r="B$r"><v>$price</v></c></row>"""
        )
      }
      sheetBuilder.append("</sheetData></worksheet>")
      writeEntry("xl/worksheets/sheet1.xml", sheetBuilder.toString())
    }
    return out.toByteArray()
  }
}
