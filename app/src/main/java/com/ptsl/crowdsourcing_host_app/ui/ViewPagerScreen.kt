package com.ptsl.crowdsourcing_host_app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.ptsl.crowdsourcing_host_app.ui.theme.*

@Composable
fun ViewPagerScreen(
    navController: NavHostController,
    onStartCrowdsourcing: (String, String) -> Unit
) {
    val pageCount = 5
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Custom Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Column {
                Text(
                    text = "Network Journey",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "On swap to a new network data capture",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = { navController.navigate(Screen.RecentTests.route) }) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Recent Tests",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp
        ) { pageIndex ->
            JourneyPage(
                index = pageIndex,
                navController = navController,
                onStartCrowdsourcing = onStartCrowdsourcing
            )
        }

        // Pager Indicators
        Row(
            Modifier
                .height(50.dp)
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pageCount) { iteration ->
                val color = if (pagerState.currentPage == iteration) PrimaryBlue else Color.DarkGray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(if (pagerState.currentPage == iteration) 10.dp else 6.dp)
                        .background(color, RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
fun JourneyPage(
    index: Int,
    navController: NavHostController,
    onStartCrowdsourcing: (String, String) -> Unit
) {
    // Fire and forget crowdsourcing call on page "creation"
    LaunchedEffect(Unit) {
        onStartCrowdsourcing("Page_${index + 1}_Auto_Trigger", "NetworkDataCapture")
    }

    val pageData = getPageData(index)

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = BgDarkCard)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Background Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                pageData.color.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = pageData.color.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = pageData.icon,
                            contentDescription = null,
                            tint = pageData.color,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = pageData.title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = pageData.description,
                    color = TextDim,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                // Show navigation buttons for specific pages
                when (index) {
                    1 -> {
                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = { navController.navigate(Screen.StandardMeasurement.route) },
                            colors = ButtonDefaults.buttonColors(containerColor = pageData.color),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Details", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Learn more about standard capture",
                            color = pageData.color.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    2 -> {
                        Spacer(modifier = Modifier.height(48.dp))
//                        Button(
//                            onClick = { navController.navigate(Screen.FTPMeasurement.route) },
//                            colors = ButtonDefaults.buttonColors(containerColor = pageData.color),
//                            shape = RoundedCornerShape(16.dp),
//                            modifier = Modifier.fillMaxWidth()
//                        ) {
//                            Icon(Icons.Default.Speed, contentDescription = null)
//                            Spacer(modifier = Modifier.width(8.dp))
//                            Text("Run FTP Test", fontWeight = FontWeight.Bold)
//                        }
                        Text(
                            text = "Fire and forget speed testing",
                            color = pageData.color.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    3 -> {
                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = { 
                                onStartCrowdsourcing(
                                    "Page_${index + 1}_Manual_Trigger", 
                                    "FTPNetworkDataCapture"
                                ) 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = pageData.color),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trigger Now", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "Fire and forget",
                            color = pageData.color.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Page Number Badge
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.05f)
            ) {
                Text(
                    text = "${index + 1}",
                    color = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class JourneyPageData(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

fun getPageData(index: Int): JourneyPageData {
    return when (index) {
        0 -> JourneyPageData(
            "Welcome Scout",
            "Starting background network analysis automatically. Explore the app while we work.",
            Icons.Default.Explore,
            PrimaryBlue
        )
        1 -> JourneyPageData(
            "Standard Network Capture",
            "Continuous monitoring of signal strength and connection quality. Tap to learn more.",
            Icons.Default.Analytics,
            AccentPurple
        )
        2 -> JourneyPageData(
            "Speed & Performance",
            "Comprehensive FTP testing for upload and download speeds. See detailed metrics.",
            Icons.Default.Speed,
            SuccessGreen
        )
        3 -> JourneyPageData(
            "Global Coverage",
            "Contributing to crowd-sourced network maps. One-tap speed testing below.",
            Icons.Default.Explore,
            PrimaryBlue
        )
        else -> JourneyPageData(
            "All Set!",
            "Your network profile is being built. Thank you for contributing to better connectivity.",
            Icons.Default.Settings,
            Color.Gray
        )
    }
}
