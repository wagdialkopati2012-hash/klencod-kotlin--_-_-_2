package com.example.data.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.core.DateUtils
import com.example.core.MoneyUtils
import com.example.data.local.CompanyInfoEntity
import com.example.data.local.QuotationWithItems
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigDecimal

object QuotationPdfGenerator {

  // Standard A4 in PDF points (72 dpi): 595 x 842
  const val A4_WIDTH = 595
  const val A4_HEIGHT = 842

  private const val ITEMS_FIRST_PAGE = 11
  private const val ITEMS_NEXT_PAGES = 16
  private const val ROW_HEIGHT = 30f

  private const val MARGIN_H = 34f
  private const val MARGIN_TOP = 28f
  private const val MARGIN_BOTTOM = 34f

  // Exact RTL column widths (sum = 527f = 595 - 2*34)
  // Order Right to Left:
  // 1. اسم الصنف (rightmost)
  // 2. الكمية
  // 3. السعر
  // 4. الإجمالي (leftmost)
  private const val COL_TOTAL_W = 100f
  private const val COL_PRICE_W = 90f
  private const val COL_QTY_W = 62f
  private const val COL_NAME_W = 247f
  private const val COL_IDX_W = 28f

  private val navyColor = Color.parseColor("#172B4D")
  private val royalBlueColor = Color.parseColor("#2563EB")
  private val tealColor = Color.parseColor("#13B8A6")
  private val lightHeaderBg = Color.parseColor("#EAF2FF")
  private val zebraBg = Color.parseColor("#F7FAFF")
  private val borderColor = Color.parseColor("#DCE5F2")
  private val mainTextColor = Color.parseColor("#172033")
  private val mutedTextColor = Color.parseColor("#64748B")

  private val renderLock = Any()

  private fun decodeLogo(path: String?): Bitmap? {
    if (path.isNullOrBlank()) return null
    if (!File(path).exists()) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        var sample = 1
        val maxSide = maxOf(bounds.outWidth, bounds.outHeight)
        while (maxSide / sample > 512) sample *= 2
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
      }
      .getOrNull()
  }

  /**
   * Builds the PDF file and the preview bitmaps safely. Never throws: tries lower bitmap scales if
   * memory is short, and falls back to drawing directly if the PDF renderer fails.
   * Returns null only if nothing could be rendered at all.
   */
  fun buildPreview(
    context: Context,
    quotationWithItems: QuotationWithItems,
    companyInfo: CompanyInfoEntity,
  ): Pair<File?, List<Bitmap>>? {
    val file = runCatching { generatePdfFile(context, quotationWithItems, companyInfo) }.getOrNull()
    for (scale in floatArrayOf(2.0f, 1.5f, 1.0f)) {
      var bitmaps: List<Bitmap> =
        if (file != null) {
          runCatching { renderPreviewBitmaps(file, scale) }.getOrNull().orEmpty()
        } else {
          emptyList()
        }
      if (bitmaps.isEmpty()) {
        bitmaps =
          runCatching {
              renderPageBitmapsDirect(context, quotationWithItems, companyInfo, scale)
            }
            .getOrNull()
            .orEmpty()
      }
      if (bitmaps.isNotEmpty()) return file to bitmaps
    }
    return null
  }

  fun generatePdfFile(
    context: Context,
    quotationWithItems: QuotationWithItems,
    companyInfo: CompanyInfoEntity,
  ): File = synchronized(renderLock) { generatePdfFileLocked(context, quotationWithItems, companyInfo) }

  private fun generatePdfFileLocked(
    context: Context,
    quotationWithItems: QuotationWithItems,
    companyInfo: CompanyInfoEntity,
  ): File {
    val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
    val safeNumber =
      quotationWithItems.quotation.quotationNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")
    val outputFile = File(pdfDir, "quotation_${safeNumber}.pdf")

    val baseTypeface =
      ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic) ?: Typeface.DEFAULT
    val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)

    val logoBitmap = decodeLogo(companyInfo.logoPath)

    val items = quotationWithItems.items
    val itemsPerFirstPage = ITEMS_FIRST_PAGE
    val itemsPerFollowingPage = ITEMS_NEXT_PAGES

    val pages = mutableListOf<List<com.example.data.local.QuotationItemEntity>>()
    if (items.isEmpty()) {
      pages.add(emptyList())
    } else {
      val firstChunk = items.take(itemsPerFirstPage)
      pages.add(firstChunk)
      var offset = firstChunk.size
      while (offset < items.size) {
        val nextChunk = items.drop(offset).take(itemsPerFollowingPage)
        pages.add(nextChunk)
        offset += nextChunk.size
      }
    }

    val document = PdfDocument()
    try {
      pages.forEachIndexed { pageIndex, pageItems ->
        val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIndex + 1).create()
        val page = document.startPage(pageInfo)
        drawPage(
          canvas = page.canvas,
          pageIndex = pageIndex,
          totalPages = pages.size,
          quotationWithItems = quotationWithItems,
          pageItems = pageItems,
          companyInfo = companyInfo,
          logoBitmap = logoBitmap,
          regularTypeface = baseTypeface,
          boldTypeface = boldTypeface,
        )
        document.finishPage(page)
      }

      FileOutputStream(outputFile).use { fos -> document.writeTo(fos) }
    } finally {
      document.close()
      logoBitmap?.recycle()
    }
    return outputFile
  }

  fun renderPreviewBitmaps(pdfFile: File, scaleFactor: Float = 2.0f): List<Bitmap> {
    val bitmaps = mutableListOf<Bitmap>()
    val fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
    fd.use { pfd ->
      PdfRenderer(pfd).use { renderer ->
        for (i in 0 until renderer.pageCount) {
          renderer.openPage(i).use { page ->
            val width = (page.width * scaleFactor).toInt().coerceAtLeast(1)
            val height = (page.height * scaleFactor).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmaps.add(bitmap)
          }
        }
      }
    }
    return bitmaps
  }

  fun renderPageBitmapsDirect(
    context: Context,
    quotationWithItems: QuotationWithItems,
    companyInfo: CompanyInfoEntity,
    scaleFactor: Float = 2.0f,
  ): List<Bitmap> {
    val baseTypeface =
      ResourcesCompat.getFont(context, R.font.ibm_plex_sans_arabic) ?: Typeface.DEFAULT
    val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)
    val logoBitmap = decodeLogo(companyInfo.logoPath)

    val items = quotationWithItems.items
    val itemsPerFirstPage = ITEMS_FIRST_PAGE
    val itemsPerFollowingPage = ITEMS_NEXT_PAGES
    val pages = mutableListOf<List<com.example.data.local.QuotationItemEntity>>()
    if (items.isEmpty()) {
      pages.add(emptyList())
    } else {
      val firstChunk = items.take(itemsPerFirstPage)
      pages.add(firstChunk)
      var offset = firstChunk.size
      while (offset < items.size) {
        val nextChunk = items.drop(offset).take(itemsPerFollowingPage)
        pages.add(nextChunk)
        offset += nextChunk.size
      }
    }

    val bitmaps = mutableListOf<Bitmap>()
    try {
      pages.forEachIndexed { pageIndex, pageItems ->
        val w = (A4_WIDTH * scaleFactor).toInt().coerceAtLeast(1)
        val h = (A4_HEIGHT * scaleFactor).toInt().coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.scale(scaleFactor, scaleFactor)
        drawPage(
          canvas = canvas,
          pageIndex = pageIndex,
          totalPages = pages.size,
          quotationWithItems = quotationWithItems,
          pageItems = pageItems,
          companyInfo = companyInfo,
          logoBitmap = logoBitmap,
          regularTypeface = baseTypeface,
          boldTypeface = boldTypeface,
        )
        bitmaps.add(bmp)
      }
    } finally {
      logoBitmap?.recycle()
    }
    return bitmaps
  }

  fun sharePdf(context: Context, pdfFile: File, quotationNumber: String) {
    val uri =
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "عرض سعر رقم $quotationNumber")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    context.startActivity(Intent.createChooser(intent, "مشاركة عرض السعر PDF"))
  }

  fun printPdf(context: Context, pdfFile: File, quotationNumber: String) {
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val adapter =
      object : PrintDocumentAdapter() {
        override fun onLayout(
          oldAttributes: PrintAttributes?,
          newAttributes: PrintAttributes?,
          cancellationSignal: CancellationSignal?,
          callback: LayoutResultCallback?,
          extras: Bundle?,
        ) {
          if (cancellationSignal?.isCanceled == true) {
            callback?.onLayoutCancelled()
            return
          }
          val info =
            PrintDocumentInfo.Builder("quotation_$quotationNumber.pdf")
              .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
              .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
              .build()
          callback?.onLayoutFinished(info, true)
        }

        override fun onWrite(
          pages: Array<out PageRange>?,
          destination: ParcelFileDescriptor?,
          cancellationSignal: CancellationSignal?,
          callback: WriteResultCallback?,
        ) {
          try {
            FileInputStream(pdfFile).use { input ->
              FileOutputStream(destination?.fileDescriptor).use { output ->
                input.copyTo(output)
              }
            }
            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
          } catch (e: Exception) {
            callback?.onWriteFailed(e.message)
          }
        }
      }
    printManager.print("عرض سعر $quotationNumber", adapter, PrintAttributes.Builder().build())
  }

  private fun withAlpha(color: Int, alpha: Int): Int =
    Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))

  private fun fillPaint(c: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = c }

  private fun strokePaint(c: Int, width: Float): Paint =
    Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = c
      style = Paint.Style.STROKE
      strokeWidth = width
    }

  /** Full-width gradient band with soft decorative circles and a teal strip at its bottom. */
  private fun drawHeroBand(canvas: Canvas, height: Float) {
    val w = A4_WIDTH.toFloat()
    val bandPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            0f, 0f, w, height,
            intArrayOf(navyColor, royalBlueColor),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(0f, 0f, w, height, bandPaint)
    canvas.save()
    canvas.clipRect(0f, 0f, w, height)
    canvas.drawCircle(w - 30f, -12f, height * 0.95f, fillPaint(Color.argb(26, 255, 255, 255)))
    canvas.drawCircle(80f, height + 14f, height * 0.72f, fillPaint(withAlpha(tealColor, 40)))
    canvas.restore()
    val strip =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            0f, 0f, w, 0f,
            intArrayOf(tealColor, royalBlueColor, navyColor),
            null,
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(0f, height - 4f, w, height, strip)
  }

  private fun drawPage(
    canvas: Canvas,
    pageIndex: Int,
    totalPages: Int,
    quotationWithItems: QuotationWithItems,
    pageItems: List<com.example.data.local.QuotationItemEntity>,
    companyInfo: CompanyInfoEntity,
    logoBitmap: Bitmap?,
    regularTypeface: Typeface,
    boldTypeface: Typeface,
  ) {
    canvas.drawColor(Color.WHITE)

    val contentLeft = MARGIN_H
    val contentRight = A4_WIDTH - MARGIN_H
    val contentWidth = contentRight - contentLeft
    val q = quotationWithItems.quotation
    val companyName = companyInfo.companyName.ifBlank { "نظام المناقصات" }
    var y: Float

    if (pageIndex == 0) {
      val bandH = 112f
      drawHeroBand(canvas, bandH)

      // Logo (white rounded tile on the left) or a letter badge when there is no logo
      val logoSize = 72f
      val logoRect = RectF(contentLeft, 20f, contentLeft + logoSize, 20f + logoSize)
      canvas.drawRoundRect(logoRect, 16f, 16f, fillPaint(Color.WHITE))
      if (logoBitmap != null) {
        val inner =
          RectF(logoRect.left + 6f, logoRect.top + 6f, logoRect.right - 6f, logoRect.bottom - 6f)
        canvas.save()
        val path =
          android.graphics.Path().apply {
            addRoundRect(inner, 11f, 11f, android.graphics.Path.Direction.CW)
          }
        canvas.clipPath(path)
        canvas.drawBitmap(logoBitmap, null, inner, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
      } else {
        val initial = companyName.trim().firstOrNull()?.toString() ?: "ع"
        drawRtlTextCentered(
          canvas, initial, logoRect.left, logoRect.top, logoSize, logoSize.toInt(),
          28f, navyColor, boldTypeface, Layout.Alignment.ALIGN_CENTER,
        )
      }

      // Company details (right side)
      val textX = contentLeft + logoSize + 16f
      val textW = (contentRight - textX).toInt()
      drawRtlText(
        canvas, companyName, textX, 22f, textW, 19f, Color.WHITE, boldTypeface,
        Layout.Alignment.ALIGN_NORMAL, maxLines = 1,
      )
      if (companyInfo.companySubtitle.isNotBlank()) {
        drawRtlText(
          canvas, companyInfo.companySubtitle, textX, 49f, textW, 10.5f,
          Color.argb(235, 255, 255, 255), regularTypeface, Layout.Alignment.ALIGN_NORMAL,
          maxLines = 1,
        )
      }
      val contactLine =
        listOf(companyInfo.phone, companyInfo.address)
          .filter { it.isNotBlank() }
          .joinToString("  •  ")
      if (contactLine.isNotBlank()) {
        drawRtlText(
          canvas, contactLine, textX, 68f, textW, 9.5f, Color.argb(205, 255, 255, 255),
          regularTypeface, Layout.Alignment.ALIGN_NORMAL, maxLines = 1,
        )
      }
      if (companyInfo.extraInfo.isNotBlank()) {
        drawRtlText(
          canvas, companyInfo.extraInfo, textX, 83f, textW, 8.5f,
          Color.argb(180, 255, 255, 255), regularTypeface, Layout.Alignment.ALIGN_NORMAL,
          maxLines = 1,
        )
      }

      // Title pill overlapping the band edge
      val pillW = 156f
      val pillH = 34f
      val pillRect =
        RectF(
          A4_WIDTH / 2f - pillW / 2f, bandH - pillH / 2f,
          A4_WIDTH / 2f + pillW / 2f, bandH + pillH / 2f,
        )
      canvas.drawRoundRect(
        RectF(pillRect.left, pillRect.top + 3f, pillRect.right, pillRect.bottom + 3f),
        17f, 17f, fillPaint(Color.argb(38, 23, 43, 77)),
      )
      canvas.drawRoundRect(pillRect, 17f, 17f, fillPaint(Color.WHITE))
      canvas.drawRoundRect(pillRect, 17f, 17f, strokePaint(tealColor, 1.6f))
      drawRtlTextCentered(
        canvas, "عرض سعر", pillRect.left, pillRect.top, pillH, pillW.toInt(),
        15f, navyColor, boldTypeface, Layout.Alignment.ALIGN_CENTER,
      )

      // Three info cards (RTL): quotation number | customer | date
      y = bandH + pillH / 2f + 18f
      val gap = 10f
      val cellW = (contentWidth - gap * 2f) / 3f
      val cardH = 52f
      val cardFill = fillPaint(Color.parseColor("#F8FAFD"))
      val cardBorder = strokePaint(borderColor, 1f)
      val metas =
        listOf(
          "رقم العرض" to q.quotationNumber,
          "اسم العميل" to q.customerName,
          "التاريخ" to DateUtils.formatNumericDate(q.quotationDateMillis),
        )
      metas.forEachIndexed { i, (label, value) ->
        val right = contentRight - i * (cellW + gap)
        val rect = RectF(right - cellW, y, right, y + cardH)
        canvas.drawRoundRect(rect, 10f, 10f, cardFill)
        canvas.drawRoundRect(rect, 10f, 10f, cardBorder)
        val bar = RectF(rect.right - 4f, rect.top + 11f, rect.right, rect.bottom - 11f)
        canvas.drawRoundRect(bar, 2f, 2f, fillPaint(if (i == 1) tealColor else royalBlueColor))
        val innerW = (cellW - 26f).toInt()
        drawRtlText(
          canvas, label, rect.left + 10f, rect.top + 9f, innerW, 8.5f, mutedTextColor,
          regularTypeface, Layout.Alignment.ALIGN_NORMAL, maxLines = 1,
        )
        drawRtlText(
          canvas, value, rect.left + 10f, rect.top + 24f, innerW, 11f, mainTextColor,
          boldTypeface, Layout.Alignment.ALIGN_NORMAL, maxLines = 1,
        )
      }
      y += cardH + 18f
    } else {
      val bandH = 54f
      drawHeroBand(canvas, bandH)
      drawRtlTextCentered(
        canvas,
        "عرض سعر رقم ${q.quotationNumber}  —  ${q.customerName}",
        contentLeft, 0f, bandH - 4f, contentWidth.toInt(), 11.5f, Color.WHITE, boldTypeface,
        Layout.Alignment.ALIGN_NORMAL, maxLines = 1,
      )
      y = bandH + 20f
    }

    // ---- Items table: [الإجمالي] [السعر] [الكمية] [اسم الصنف] [#] (left -> right) ----
    val xTotalLeft = contentLeft
    val xPriceLeft = xTotalLeft + COL_TOTAL_W
    val xQtyLeft = xPriceLeft + COL_PRICE_W
    val xNameLeft = xQtyLeft + COL_QTY_W
    val xIdxLeft = xNameLeft + COL_NAME_W

    val tableTop = y
    val tableHeaderHeight = 30f
    val tableHeaderPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            contentLeft, y, contentRight, y,
            navyColor, royalBlueColor,
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRoundRect(
      RectF(contentLeft, y, contentRight, y + tableHeaderHeight), 8f, 8f, tableHeaderPaint,
    )
    fun headerCell(text: String, x: Float, w: Float, align: Layout.Alignment) =
      drawRtlTextCentered(
        canvas, text, x + 4f, y, tableHeaderHeight, (w - 8f).toInt(), 10f, Color.WHITE,
        boldTypeface, align,
      )
    headerCell("#", xIdxLeft, COL_IDX_W, Layout.Alignment.ALIGN_CENTER)
    headerCell("اسم الصنف", xNameLeft + 4f, COL_NAME_W - 8f, Layout.Alignment.ALIGN_NORMAL)
    headerCell("الكمية", xQtyLeft, COL_QTY_W, Layout.Alignment.ALIGN_CENTER)
    headerCell("السعر", xPriceLeft, COL_PRICE_W, Layout.Alignment.ALIGN_CENTER)
    headerCell("الإجمالي", xTotalLeft, COL_TOTAL_W, Layout.Alignment.ALIGN_CENTER)
    y += tableHeaderHeight

    val rowEven = fillPaint(Color.WHITE)
    val rowOdd = fillPaint(zebraBg)
    val hairline = strokePaint(borderColor, 0.8f)
    val idxBadge = fillPaint(Color.parseColor("#E5FAF7"))
    val firstNumber = if (pageIndex == 0) 1 else ITEMS_FIRST_PAGE + (pageIndex - 1) * ITEMS_NEXT_PAGES + 1

    pageItems.forEachIndexed { idx, item ->
      val rowRect = RectF(contentLeft, y, contentRight, y + ROW_HEIGHT)
      canvas.drawRect(rowRect, if (idx % 2 == 0) rowEven else rowOdd)
      canvas.drawLine(contentLeft, y + ROW_HEIGHT, contentRight, y + ROW_HEIGHT, hairline)

      canvas.drawCircle(xIdxLeft + COL_IDX_W / 2f, y + ROW_HEIGHT / 2f, 9.5f, idxBadge)
      drawRtlTextCentered(
        canvas, (firstNumber + idx).toString(), xIdxLeft, y, ROW_HEIGHT, COL_IDX_W.toInt(), 8f,
        tealColor, boldTypeface, Layout.Alignment.ALIGN_CENTER,
      )
      drawRtlTextCentered(
        canvas, item.productNameSnapshot, xNameLeft + 8f, y, ROW_HEIGHT,
        (COL_NAME_W - 16f).toInt(), 9.5f, mainTextColor, regularTypeface,
        Layout.Alignment.ALIGN_NORMAL, maxLines = 2,
      )
      drawRtlTextCentered(
        canvas, String.format(java.util.Locale.US, "%,d", item.quantity), xQtyLeft + 4f, y,
        ROW_HEIGHT, (COL_QTY_W - 8f).toInt(), 9.5f, mainTextColor, boldTypeface,
        Layout.Alignment.ALIGN_CENTER,
      )
      drawRtlTextCentered(
        canvas, MoneyUtils.formatUsdString(item.unitPriceUsdSnapshot), xPriceLeft + 4f, y,
        ROW_HEIGHT, (COL_PRICE_W - 8f).toInt(), 9.5f, mainTextColor, regularTypeface,
        Layout.Alignment.ALIGN_CENTER, maxLines = 1,
      )
      drawRtlTextCentered(
        canvas, MoneyUtils.formatUsdString(item.lineTotalUsd), xTotalLeft + 6f, y, ROW_HEIGHT,
        (COL_TOTAL_W - 12f).toInt(), 9.5f, navyColor, boldTypeface,
        Layout.Alignment.ALIGN_CENTER, maxLines = 1,
      )
      y += ROW_HEIGHT
    }
    canvas.drawRoundRect(
      RectF(contentLeft, tableTop, contentRight, y), 8f, 8f, strokePaint(borderColor, 1f),
    )

    // ---- Summary on the last page ----
    if (pageIndex == totalPages - 1) {
      y += 16f
      val totalBd = q.totalAmountUsd.toBigDecimalOrNull() ?: BigDecimal.ZERO
      val boxH = 84f
      val gap = 10f
      val totalW = 215f
      val wordsRect = RectF(contentLeft + totalW + gap, y, contentRight, y + boxH)
      val totalRect = RectF(contentLeft, y, contentLeft + totalW, y + boxH)

      // Amount in words (right card)
      canvas.drawRoundRect(wordsRect, 12f, 12f, fillPaint(lightHeaderBg))
      canvas.drawRoundRect(wordsRect, 12f, 12f, strokePaint(withAlpha(royalBlueColor, 90), 1f))
      val wInnerX = wordsRect.left + 14f
      val wInnerW = (wordsRect.width() - 28f).toInt()
      drawRtlText(
        canvas, "المبلغ كتابةً", wInnerX, y + 10f, wInnerW, 9f, royalBlueColor, boldTypeface,
        Layout.Alignment.ALIGN_NORMAL, maxLines = 1,
      )
      drawRtlText(
        canvas, MoneyUtils.amountInArabicWords(totalBd), wInnerX, y + 26f, wInnerW, 10.5f,
        mainTextColor, regularTypeface, Layout.Alignment.ALIGN_NORMAL, maxLines = 3,
      )
      val itemsCount = quotationWithItems.items.size
      val totalQty = quotationWithItems.items.sumOf { it.quantity.toLong() }
      drawRtlText(
        canvas,
        "عدد الأصناف: $itemsCount   •   إجمالي الكميات: ${String.format(java.util.Locale.US, "%,d", totalQty)}",
        wInnerX, y + boxH - 20f, wInnerW, 8.5f, mutedTextColor, regularTypeface,
        Layout.Alignment.ALIGN_NORMAL, maxLines = 1,
      )

      // Grand total (left card)
      val totalPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          shader =
            LinearGradient(
              totalRect.left, totalRect.top, totalRect.right, totalRect.bottom,
              navyColor, royalBlueColor,
              Shader.TileMode.CLAMP,
            )
        }
      canvas.drawRoundRect(totalRect, 12f, 12f, totalPaint)
      drawRtlText(
        canvas, "الإجمالي الكلي", totalRect.left + 8f, y + 12f, (totalW - 16f).toInt(), 10f,
        Color.argb(215, 255, 255, 255), regularTypeface, Layout.Alignment.ALIGN_CENTER, maxLines = 1,
      )
      drawRtlText(
        canvas, MoneyUtils.formatUsd(totalBd), totalRect.left + 8f, y + 30f, (totalW - 16f).toInt(),
        19f, Color.WHITE, boldTypeface, Layout.Alignment.ALIGN_CENTER, maxLines = 1,
      )
      canvas.drawRoundRect(
        RectF(totalRect.centerX() - 22f, y + boxH - 17f, totalRect.centerX() + 22f, y + boxH - 14f),
        1.5f, 1.5f, fillPaint(tealColor),
      )
      y += boxH + 14f

      if (q.notes.isNotBlank()) {
        val noteRect = RectF(contentLeft, y, contentRight, y + 52f)
        canvas.drawRoundRect(noteRect, 10f, 10f, fillPaint(Color.parseColor("#FFFBEB")))
        canvas.drawRoundRect(noteRect, 10f, 10f, strokePaint(Color.parseColor("#F3DFA2"), 1f))
        drawRtlText(
          canvas, "ملاحظات", noteRect.left + 14f, y + 8f, (contentWidth - 28f).toInt(), 9f,
          Color.parseColor("#92400E"), boldTypeface, Layout.Alignment.ALIGN_NORMAL, maxLines = 1,
        )
        drawRtlText(
          canvas, q.notes, noteRect.left + 14f, y + 22f, (contentWidth - 28f).toInt(), 9.5f,
          mainTextColor, regularTypeface, Layout.Alignment.ALIGN_NORMAL, maxLines = 2,
        )
        y += 62f
      }

      if (y + 14f <= A4_HEIGHT - 70f) {
        drawRtlText(
          canvas, "نشكركم على ثقتكم ويسعدنا خدمتكم دائماً", contentLeft, y + 14f,
          contentWidth.toInt(), 10.5f, royalBlueColor, boldTypeface,
          Layout.Alignment.ALIGN_CENTER, maxLines = 1,
        )
      }
    }

    // ---- Footer ----
    val footerY = A4_HEIGHT - 38f
    val footerLine =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            contentLeft, footerY, contentRight, footerY,
            intArrayOf(tealColor, royalBlueColor, navyColor),
            null,
            Shader.TileMode.CLAMP,
          )
        strokeWidth = 1.6f
      }
    canvas.drawLine(contentLeft, footerY, contentRight, footerY, footerLine)
    drawRtlText(
      canvas, companyName, contentLeft + 150f, footerY + 8f, (contentWidth - 150f).toInt(), 8.5f,
      mutedTextColor, regularTypeface, Layout.Alignment.ALIGN_NORMAL, maxLines = 1,
    )
    drawRtlText(
      canvas, "صفحة ${pageIndex + 1} من $totalPages", contentLeft, footerY + 8f,
      contentWidth.toInt(), 8.5f, mutedTextColor, boldTypeface, Layout.Alignment.ALIGN_CENTER,
    )
    if (companyInfo.phone.isNotBlank()) {
      drawRtlText(
        canvas, companyInfo.phone, contentLeft, footerY + 8f, 150, 8.5f, mutedTextColor,
        regularTypeface, Layout.Alignment.ALIGN_OPPOSITE, maxLines = 1,
      )
    }
  }

  private fun buildLayout(
    text: String,
    width: Int,
    sizeSp: Float,
    color: Int,
    typeface: Typeface,
    alignment: Layout.Alignment,
    maxLines: Int,
  ): StaticLayout {
    val paint =
      TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = sizeSp
        this.typeface = typeface
      }
    val builder =
      StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(alignment)
        .setTextDirection(TextDirectionHeuristics.RTL)
        .setLineSpacing(0f, 1.15f)
        .setIncludePad(false)
    if (maxLines < Int.MAX_VALUE) {
      builder.setMaxLines(maxLines).setEllipsize(TextUtils.TruncateAt.END)
    }
    return builder.build()
  }

  private fun drawRtlText(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Int,
    sizeSp: Float,
    color: Int,
    typeface: Typeface,
    alignment: Layout.Alignment,
    maxLines: Int = Int.MAX_VALUE,
  ) {
    if (width <= 0 || text.isEmpty()) return
    val layout = buildLayout(text, width, sizeSp, color, typeface, alignment, maxLines)
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
  }

  /** Draws the text vertically centered inside a box that starts at [top] and is [height] tall. */
  private fun drawRtlTextCentered(
    canvas: Canvas,
    text: String,
    x: Float,
    top: Float,
    height: Float,
    width: Int,
    sizeSp: Float,
    color: Int,
    typeface: Typeface,
    alignment: Layout.Alignment,
    maxLines: Int = Int.MAX_VALUE,
  ) {
    if (width <= 0 || text.isEmpty()) return
    val layout = buildLayout(text, width, sizeSp, color, typeface, alignment, maxLines)
    canvas.save()
    canvas.translate(x, top + (height - layout.height) / 2f)
    layout.draw(canvas)
    canvas.restore()
  }
}
