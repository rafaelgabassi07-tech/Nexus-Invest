package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.DangerRed
import com.example.network.B3NetworkService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun MarketTicker(modifier: Modifier = Modifier) {
    var tickerItems by remember {
        mutableStateOf(
            listOf(
                Triple("IBOV", "124.810 pts", 1.25),
                Triple("IFIX", "3.394 pts", -0.08),
                Triple("DÓLAR", "R$ 5,14", 0.32)
            )
        )
    }
    
    val listState = rememberLazyListState()
    val infiniteCount = 10000
    
    // Real-time market data background fetching loop
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            while (true) {
                try {
                    val ibovData = B3NetworkService.fetchAssetData("^BVSP")
                    val dolarData = B3NetworkService.fetchAssetData("USDBRL=X")
                    val ifixData = B3NetworkService.fetchAssetData("^IFIX")
                    
                    val newList = mutableListOf<Triple<String, String, Double>>()
                    
                    // Add IBOV
                    if (ibovData != null && ibovData.price > 0.0) {
                        newList.add(Triple("IBOV", String.format(java.util.Locale.US, "%,.0f pts", ibovData.price).replace(",", "."), ibovData.changePercent))
                    } else {
                        newList.add(Triple("IBOV", "124.810 pts", 1.25))
                    }
                    
                    // Add IFIX
                    if (ifixData != null && ifixData.price > 0.0) {
                        newList.add(Triple("IFIX", String.format(java.util.Locale.US, "%,.0f pts", ifixData.price).replace(",", "."), ifixData.changePercent))
                    } else {
                        newList.add(Triple("IFIX", "3.394 pts", -0.08))
                    }
                    
                    // Add Dolar
                    if (dolarData != null && dolarData.price > 0.0) {
                        newList.add(Triple("DÓLAR", String.format(java.util.Locale.getDefault(), "R$ %.2f", dolarData.price), dolarData.changePercent))
                    } else {
                        newList.add(Triple("DÓLAR", "R$ 5,14", 0.32))
                    }
                    
                    withContext(Dispatchers.Main) {
                        tickerItems = newList
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MarketTicker", "Error in background fetch loop", e)
                }
                
                // Refresh indexes every 60 seconds
                delay(60000)
            }
        }
    }
    
    var hasInitializedScroll by remember { mutableStateOf(false) }
    
    // Initial start at the middle to allow seamless left and right scrolls
    LaunchedEffect(tickerItems) {
        if (tickerItems.isNotEmpty() && !hasInitializedScroll) {
            listState.scrollToItem(infiniteCount / 2)
            hasInitializedScroll = true
        }
    }
    
    // Smooth sliding scroll loop that cycles continuously and gently
    LaunchedEffect(tickerItems) {
        if (tickerItems.isNotEmpty()) {
            while (true) {
                try {
                    val currentVisible = listState.firstVisibleItemIndex
                    listState.animateScrollToItem(currentVisible + 1)
                } catch (e: Exception) {
                    // Ignore transient layout/scroll changes
                }
                delay(2500) // gentle frame update
            }
        }
    }
    
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        border = BorderStroke(width = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(infiniteCount) { index ->
                if (tickerItems.isNotEmpty()) {
                    val (name, value, change) = tickerItems[index % tickerItems.size]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp
                        )
                        if (change != 0.0) {
                            val isPos = change > 0
                            Text(
                                text = "${if (isPos) "▲" else "▼"} ${String.format(Locale.getDefault(), "%.2f", Math.abs(change))}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isPos) SuccessGreen else DangerRed
                            )
                        } else {
                            Text(
                                text = "0.00%",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
