package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import com.example.ui.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.UserRole
import com.example.data.local.CompanyInfoEntity
import com.example.data.local.QuotationWithItems
import com.example.data.pdf.QuotationPdfGenerator
import com.example.data.repository.AuthenticatedUser
import com.example.ui.components.PremiumLoadingIndicator
import com.example.ui.components.PremiumSecondaryButton
import com.example.ui.theme.DeepNavy
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun QuotationPreviewScreen(
  user: AuthenticatedUser,
  quotationWithItems: QuotationWithItems?,
  companyInfo: CompanyInfoEntity,
  onBack: () -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  val canExport = user.permissions.exportPdf || user.role == UserRole.SUPER_ADMIN

  var pdfFile by remember { mutableStateOf<File?>(null) }
  var pageBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
  var loadFailed by remember { mutableStateOf(false) }
  var reloadTick by remember { mutableIntStateOf(0) }
  var currentPageIndex by remember { mutableIntStateOf(0) }
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  // NOTE: bitmaps are intentionally NOT recycled manually. Recycling them while Compose may still
  // draw the previous frame caused "Canvas: trying to use a recycled bitmap" crashes.
  LaunchedEffect(quotationWithItems, companyInfo, reloadTick) {
    if (quotationWithItems != null) {
      loadFailed = false
      val result =
        withContext(Dispatchers.IO) {
          runCatching {
              QuotationPdfGenerator.buildPreview(context, quotationWithItems, companyInfo)
            }
            .getOrNull()
        }
      if (result == null || result.second.isEmpty()) {
        loadFailed = true
      } else {
        pdfFile = result.first
        pageBitmaps = result.second
        currentPageIndex = 0
        scale = 1f
        offset = Offset.Zero
      }
    }
  }

  val savePdfLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
      val file = pdfFile
      if (uri != null && file != null) {
        scope.launch {
          runCatching {
              withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                  file.inputStream().use { input -> input.copyTo(out) }
                }
              }
            }
            .onSuccess {
              snackbarHostState.showSnackbar(com.example.ui.i18n.Translator.t("تم حفظ ملف PDF بنجاح"))
            }
            .onFailure {
              snackbarHostState.showSnackbar(com.example.ui.i18n.Translator.t("تعذر حفظ ملف PDF"))
            }
        }
      }
    }

  Scaffold(
    containerColor = Color(0xFF1E293B),
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      Surface(
        color = DeepNavy,
        tonalElevation = 4.dp,
      ) {
        Row(
          modifier =
            Modifier.fillMaxWidth()
              .statusBarsPadding()
              .padding(horizontal = 10.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("preview_back_button")) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "رجوع",
                tint = Color.White,
              )
            }
            Spacer(Modifier.width(6.dp))
            Text(
              text = "معاينة عرض السعر",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
          }

          if (canExport && pdfFile != null && quotationWithItems != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  QuotationPdfGenerator.sharePdf(
                    context,
                    pdfFile!!,
                    quotationWithItems.quotation.quotationNumber,
                  )
                },
                modifier = Modifier.testTag("share_pdf_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Share,
                  contentDescription = "مشاركة PDF",
                  tint = Color.White,
                )
              }
              IconButton(
                onClick = {
                  val num = quotationWithItems.quotation.quotationNumber
                  savePdfLauncher.launch("$num.pdf")
                },
                modifier = Modifier.testTag("save_pdf_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.PictureAsPdf,
                  contentDescription = "حفظ PDF",
                  tint = Color.White,
                )
              }
              IconButton(
                onClick = {
                  QuotationPdfGenerator.printPdf(
                    context,
                    pdfFile!!,
                    quotationWithItems.quotation.quotationNumber,
                  )
                },
                modifier = Modifier.testTag("print_pdf_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Print,
                  contentDescription = "طباعة",
                  tint = Color.White,
                )
              }
            }
          }
        }
      }
    },
    bottomBar = {
      if (pageBitmaps.size > 1) {
        Surface(color = DeepNavy) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              enabled = currentPageIndex > 0,
              onClick = {
                currentPageIndex--
                scale = 1f
                offset = Offset.Zero
              },
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "الصفحة السابقة",
                tint = Color.White,
              )
            }
            Text(
              text = "صفحة ${currentPageIndex + 1} من ${pageBitmaps.size}",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White,
            )
            IconButton(
              enabled = currentPageIndex < pageBitmaps.lastIndex,
              onClick = {
                currentPageIndex++
                scale = 1f
                offset = Offset.Zero
              },
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "الصفحة التالية",
                tint = Color.White,
              )
            }
          }
        }
      }
    },
  ) { innerPadding ->
    Box(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .background(Color(0xFF1E293B))
          .padding(14.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (pageBitmaps.isEmpty() && loadFailed) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
          Text(
            text = "تعذر تجهيز المعاينة",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
          )
          PremiumSecondaryButton(text = "إعادة المحاولة", onClick = { reloadTick++ })
        }
      } else if (pageBitmaps.isEmpty()) {
        PremiumLoadingIndicator(message = "جارٍ تجهيز مستند A4...")
      } else {
        val currentBitmap = pageBitmaps[currentPageIndex.coerceIn(0, pageBitmaps.lastIndex)]
        // Pure white A4 paper surface (NO Liquid Glass on the A4 document itself)
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .aspectRatio(QuotationPdfGenerator.A4_WIDTH.toFloat() / QuotationPdfGenerator.A4_HEIGHT.toFloat())
              .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                  val newScale = (scale * zoom).coerceIn(1f, 3.5f)
                  scale = newScale
                  offset =
                    if (newScale > 1f) {
                      Offset(offset.x + pan.x, offset.y + pan.y)
                    } else {
                      Offset.Zero
                    }
                }
              }
              .pointerInput(Unit) {
                detectTapGestures(
                  onDoubleTap = {
                    scale = if (scale > 1.2f) 1f else 2f
                    offset = Offset.Zero
                  }
                )
              }
              .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
              }
              .shadow(18.dp, RoundedCornerShape(6.dp))
              .clip(RoundedCornerShape(6.dp))
              .background(Color.White),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            bitmap = currentBitmap.asImageBitmap(),
            contentDescription = "صفحة عرض السعر A4",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}
