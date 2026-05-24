package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
          contentAlignment = Alignment.Center
        ) {
          Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
          ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = "VALORAE",
                color = GoldPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Robolectric & Roborazzi Visual Test",
                color = TextSecondary,
                fontSize = 12.sp
              )
            }
          }
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun notifications_dialog_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.components.NotificationsDialog(
          onDismiss = {}
        )
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/notifications_dialog.png")
  }
}
