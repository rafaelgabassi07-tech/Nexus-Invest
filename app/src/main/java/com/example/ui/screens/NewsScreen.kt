package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.NewsItem
import com.example.ui.theme.*

@Composable
fun NewsScreen(
    news: List<NewsItem>,
    isLoadingNews: Boolean,
    onRefreshNews: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Features and Filters state
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("Tudo") }
    var bookmarkedUrls by remember { mutableStateOf(setOf<String>()) }
    var readUrls by remember { mutableStateOf(setOf<String>()) }

    // Categories list definitions
    val categories = listOf("Tudo", "Ações", "FIIs", "Macroeconomia", "Geral", "Salvos")

    // Intelligent heuristic classification helper
    fun getNewsCategory(item: NewsItem): String {
        val text = (item.title + " " + item.source).lowercase()
        return when {
            text.contains("fii") || text.contains("mxrf") || text.contains("hglg") || text.contains("fundo imobiliário") || text.contains("fundos imobiliários") || text.contains("rendiment") || text.contains("dividendo de fii") -> "FIIs"
            text.contains("petr") || text.contains("vale") || text.contains("ação") || text.contains("ações") || text.contains("divid") || text.contains("jcp") || text.contains("lucr") || text.contains("b3") || text.contains("empresa") -> "Ações"
            text.contains("selic") || text.contains("ipca") || text.contains("inflaç") || text.contains("focus") || text.contains("central") || text.contains("pib") || text.contains("juros") || text.contains("economia") || text.contains("governo") || text.contains("dólar") || text.contains("dolar") -> "Macroeconomia"
            else -> "Geral"
        }
    }

    // Filter and sort news based on active filters (Newest first)
    val filteredNews = remember(news, searchQuery, selectedCategory, bookmarkedUrls) {
        val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
        
        news.filter { item ->
            // Search string match
            val matchesSearch = searchQuery.isEmpty() || 
                    item.title.contains(searchQuery, ignoreCase = true) || 
                    item.source.contains(searchQuery, ignoreCase = true)

            // Category match or Favorites filter
            val matchesCategory = when (selectedCategory) {
                "Tudo" -> true
                "Salvos" -> bookmarkedUrls.contains(item.link)
                else -> getNewsCategory(item) == selectedCategory
            }

            matchesSearch && matchesCategory
        }.sortedByDescending { 
            try {
                dateFormat.parse(it.pubDate)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    // Auto-refresh logic (Check for updates once per session/day equivalent)
    LaunchedEffect(Unit) {
        if (news.isEmpty()) {
            onRefreshNews()
        }
    }

    val listState = rememberLazyListState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "Notícias",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Notícias financeiras em tempo real.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        // 1. Premium Search Bar
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Procurar notícias...", color = TextSecondary.copy(alpha = 0.4f), fontSize = 14.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp)) },
                                trailingIcon = if (searchQuery.isNotEmpty()) {
                                    {
                                        IconButton(onClick = { searchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else null,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = GoldPrimary,
                                    unfocusedBorderColor = BorderColor.copy(alpha = 0.1f),
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.weight(1f).height(50.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            IconButton(
                                onClick = { focusManager.clearFocus() },
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(GoldPrimary, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Outlined.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.background)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (isLoadingNews) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(999.dp)),
                                color = GoldPrimary,
                                trackColor = BorderColor.copy(alpha = 0.12f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 2. Horizontal auto-scroll Category Chips
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(categories) { category ->
                                val isSelected = selectedCategory == category
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) GoldPrimary else MaterialTheme.colorScheme.surface,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) GoldPrimary else BorderColor.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedCategory = category }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = category,
                                        color = if (isSelected) MaterialTheme.colorScheme.background else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.05f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredNews.size} notícias encontradas",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        if (searchQuery.isNotEmpty() || selectedCategory != "Tudo") {
                            Text(
                                text = "Limpar Filtros",
                                color = GoldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.clickable {
                                    searchQuery = ""
                                    selectedCategory = "Tudo"
                                }
                            )
                        }
                    }
                }

                if (filteredNews.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (isLoadingNews) "Sincronizando notícias" else "Nenhuma notícia nesta categoria",
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isLoadingNews) "A página já está pronta; o feed será preenchido automaticamente assim que o Proxy responder." else "Tente outra aba ou remova os filtros de pesquisa.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredNews, key = { it.link }) { item ->
                        val isBookmarked = bookmarkedUrls.contains(item.link)
                        val isRead = readUrls.contains(item.link)

                        NewsCardItem(
                            item = item,
                            isBookmarked = isBookmarked,
                            isRead = isRead,
                            onBookmarkToggle = {
                                bookmarkedUrls = if (isBookmarked) {
                                    bookmarkedUrls - item.link
                                } else {
                                    bookmarkedUrls + item.link
                                }
                            },
                            onClick = {
                                if (item.link.isNotEmpty()) {
                                    readUrls = readUrls + item.link
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore or fallback
                                    }
                                }
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
}

@Composable
fun NewsCardItem(
    item: NewsItem,
    isBookmarked: Boolean,
    isRead: Boolean,
    onBookmarkToggle: () -> Unit,
    onClick: () -> Unit
) {
    // Soft opacity reduction for read articles
    val contentAlpha = if (isRead) 0.65f else 1.0f

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(
            width = 1.dp,
            color = BorderColor.copy(alpha = if (isRead) 0.04f else 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern Category Badge based on classification
                    val cleanSource = remember(item.source) {
                        var cleaned = item.source.lowercase().trim()
                        if (cleaned.startsWith("https://")) cleaned = cleaned.substring(8)
                        else if (cleaned.startsWith("http://")) cleaned = cleaned.substring(7)
                        if (cleaned.startsWith("www.")) cleaned = cleaned.substring(4)
                        val slashIdx = cleaned.indexOf('/')
                        if (slashIdx != -1) cleaned = cleaned.substring(0, slashIdx)
                        cleaned = cleaned.removeSuffix(".br").removeSuffix(".com").removeSuffix(".net").removeSuffix(".org")
                        cleaned.uppercase(java.util.Locale.ROOT)
                    }
                    Box(
                        modifier = Modifier
                            .background(GoldPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp))
                            .border(1.dp, GoldPrimary.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = cleanSource,
                            color = GoldPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isRead) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Date Label
                Text(
                    text = item.pubDate,
                    color = TextSecondary.copy(alpha = contentAlpha),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Headline
            Text(
                text = item.title,
                color = TextPrimary.copy(alpha = contentAlpha),
                fontSize = 13.sp,
                fontWeight = if (isRead) FontWeight.Medium else FontWeight.Bold,
                lineHeight = 17.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Dual interaction bar (Toggles & Action)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Left-side Interactive Bookmark Selector
                    IconButton(
                        onClick = onBookmarkToggle,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remover dos salvos" else "Salvar notícia",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // News Share Button
                    val shareContext = LocalContext.current
                    IconButton(
                        onClick = {
                            try {
                                val sendIntent: Intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "${item.title}\n\nLeia mais em: ${item.link}")
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, null)
                                shareContext.startActivity(shareIntent)
                            } catch (e: Exception) {
                                // Ignore or fallback gracefully
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Compartilhar notícia",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Right-side Read More indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(GoldPrimary.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .clickable { onClick() }
                ) {
                    Text(
                        text = "LER", 
                        color = GoldPrimary, 
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        tint = GoldPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
