package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppDatabase
import com.example.data.TransactionRepository
import com.example.ui.screens.ChartsScreen

import androidx.compose.material.icons.filled.DateRange
import com.example.ui.screens.AnalysisScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NewsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.PortfolioViewModel
import com.example.viewmodel.PortfolioViewModelFactory

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize local database database context dependencies manually (simple injection)
        val database = AppDatabase.getDatabase(this)
        val repository = TransactionRepository(database.transactionDao())
        val viewModelFactory = PortfolioViewModelFactory(repository)

        setContent {
            MyApplicationTheme {
                // Instantiate our centralized Portfolio ViewModel using factory
                val viewModel: PortfolioViewModel = viewModel(factory = viewModelFactory)
                
                // Collect state reactively with lifecycle awareness
                val portfolioSummary by viewModel.portfolioSummary.collectAsStateWithLifecycle()
                val assetSummaries by viewModel.assetSummaries.collectAsStateWithLifecycle()
                val transactions by viewModel.transactions.collectAsStateWithLifecycle()
                
                val newsFeed by viewModel.newsFeed.collectAsStateWithLifecycle()
                val isLoadingNews by viewModel.isLoadingNews.collectAsStateWithLifecycle()

                val tickerInput by viewModel.searchTickerInput.collectAsStateWithLifecycle()
                val searchResult by viewModel.searchQueryResult.collectAsStateWithLifecycle()
                val chartHistory by viewModel.searchQueryHistory.collectAsStateWithLifecycle()
                val assetNews by viewModel.searchQueryNews.collectAsStateWithLifecycle()
                val isSearchingAsset by viewModel.isSearchingAsset.collectAsStateWithLifecycle()

                val aiReport by viewModel.aiReport.collectAsStateWithLifecycle()
                val isLoadingAiReport by viewModel.isLoadingAiReport.collectAsStateWithLifecycle()

                var activePage by remember { mutableIntStateOf(0) } // 0: Dashboard, 1: Análise, 2: Notícias

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = GoldPrimary,
                                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "INVESTIDOR 10",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        letterSpacing = 1.sp,
                                        color = TextPrimary
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = DarkBackground,
                                titleContentColor = TextPrimary
                            ),
                            actions = {
                                TextButton(onClick = { viewModel.forceRefresh() }) {
                                    Text("Atualizar", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurface,
                            tonalElevation = 8.dp
                        ) {
                            NavigationBarItem(
                                selected = activePage == 0,
                                onClick = { activePage = 0 },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                                label = { Text("Dashboard", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DarkBackground,
                                    selectedTextColor = GoldPrimary,
                                    indicatorColor = GoldPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = activePage == 1,
                                onClick = { activePage = 1 },
                                icon = { Icon(Icons.Default.Search, contentDescription = "Análise de Ativos") },
                                label = { Text("Análise", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DarkBackground,
                                    selectedTextColor = GoldPrimary,
                                    indicatorColor = GoldPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = activePage == 2,
                                onClick = { activePage = 2 },
                                icon = { Icon(Icons.Default.Star, contentDescription = "Gráficos", tint = if (activePage == 2) DarkBackground else TextSecondary) },
                                label = { Text("Estatísticas", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DarkBackground,
                                    selectedTextColor = GoldPrimary,
                                    indicatorColor = GoldPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = activePage == 3,
                                onClick = { activePage = 3 },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Notícias financeiras") },
                                label = { Text("Notícias", style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DarkBackground,
                                    selectedTextColor = GoldPrimary,
                                    indicatorColor = GoldPrimary,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    },
                    containerColor = DarkBackground
                ) { innerPadding ->
                    // Beautiful fade-in navigation switcher based on page index
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (activePage) {
                            0 -> DashboardScreen(
                                summary = portfolioSummary,
                                assets = assetSummaries,
                                transactions = transactions,
                                onAddTransaction = { ticker, qty, prc, type ->
                                    viewModel.insertTransaction(ticker, qty, prc, type)
                                },
                                onDeleteTransaction = { tx ->
                                    viewModel.deleteTransaction(tx)
                                },
                                onAssetClick = { ticker ->
                                    // Set clicked asset as active search in next window and transition tabs smoothly!
                                    viewModel.searchTickerInput.value = ticker
                                    viewModel.searchAndAnalyzeAsset(ticker)
                                    activePage = 1
                                }
                            )
                            1 -> AnalysisScreen(
                                tickerInput = tickerInput,
                                onTickerInputChanges = { viewModel.searchTickerInput.value = it },
                                onSearchClick = { ticker -> viewModel.searchAndAnalyzeAsset(ticker) },
                                searchResult = searchResult,
                                chartHistory = chartHistory,
                                assetNews = assetNews,
                                isSearching = isSearchingAsset,
                                aiReport = aiReport,
                                isLoadingAiReport = isLoadingAiReport,
                                onTriggerAiAnalysis = { viewModel.generateAiAnalysisForSearchedAsset() }
                            )
                            2 -> ChartsScreen(viewModel = viewModel)
                            3 -> NewsScreen(
                                news = newsFeed,
                                isLoadingNews = isLoadingNews,
                                onRefreshNews = { viewModel.fetchGlobalNews() },
                                portfolioSummaryState = aiReport,
                                isLoadingPortfolioReport = isLoadingAiReport,
                                onTriggerPortfolioReport = { viewModel.makeAiAdvisorPortfolioReport() }
                            )
                        }
                    }
                }
            }
        }
    }
}
