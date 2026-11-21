package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes =
  Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
  )

object Dimensions {
  val SpaceXs = 4.dp
  val SpaceSm = 8.dp
  val SpaceMd = 12.dp
  val SpaceLg = 16.dp
  val SpaceXl = 20.dp
  val Space2Xl = 24.dp
  val Space3Xl = 32.dp

  val CardCorner = 22.dp
  val CardCornerLarge = 26.dp
  val ButtonCorner = 18.dp
  val InputCorner = 16.dp
  val NavCapsuleCorner = 30.dp
  val BottomNavClearance = 104.dp
}
