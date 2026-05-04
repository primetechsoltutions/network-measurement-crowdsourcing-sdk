package com.ptsl.crowdsourcing_host_app.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.ptsl.crowdsourcing_network_sdk.NetworkCrowdSourcingDataUploader
import com.ptsl.crowdsourcing_host_app.ui.theme.AccentPurple
import com.ptsl.crowdsourcing_host_app.ui.theme.BgDark
import com.ptsl.crowdsourcing_host_app.ui.theme.BgDarkCard
import com.ptsl.crowdsourcing_host_app.ui.theme.FWASDKTheme
import com.ptsl.crowdsourcing_host_app.ui.theme.PrimaryBlue
import com.ptsl.crowdsourcing_host_app.ui.theme.SuccessGreen
import com.ptsl.crowdsourcing_host_app.ui.theme.TextDim
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Fragment-based host app that demonstrates integrating the Network Measurement SDK.
 * This shows fire-and-forget data capture calls from a Fragment context.
 */
class MeasurementHostFragment : Fragment() {

    private val sdk = NetworkCrowdSourcingDataUploader()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        // Initialize SDK with fragment context
        sdk.init(this@MeasurementHostFragment, "FragmentHostApp")

        setContent {
            FWASDKTheme {
                MeasurementFragmentContent(
                    onStartStandardCapture = { triggerStandardCapture() },
                    onStartFTPCapture = { triggerFTPCapture() }
                )
            }
        }
    }

    private fun triggerStandardCapture() {
        val timeStamp = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(System.currentTimeMillis())

        // Fire and forget - no waiting for response
        sdk.startCrowdSourcingUploading(
            msisdn = "8801900000000",
            integratedAppVersion = "1.0.0",
            sdkInitiateTimeStamp = timeStamp,
            integratedAppEventName = "Fragment_Standard_Capture"
        ) { success, status ->
            android.util.Log.d(
                "MeasurementFragment",
                "Standard capture initiated: Success=$success"
            )
        }
    }

    private fun triggerFTPCapture() {
        val timeStamp = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss",
            Locale.getDefault()
        ).format(System.currentTimeMillis())

        // Fire and forget - no waiting for response
        sdk.startCrowdSourcingUploading(
            msisdn = "8801900000000",
            integratedAppVersion = "1.0.0",
            sdkInitiateTimeStamp = timeStamp,
            integratedAppEventName = "Fragment_FTP_Capture"
        ) { success, status ->
            android.util.Log.d(
                "MeasurementFragment",
                "FTP capture initiated: Success=$success"
            )
        }
    }
}

@Composable
private fun MeasurementFragmentContent(
    onStartStandardCapture: () -> Unit,
    onStartFTPCapture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Network Measurement",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Fragment-Based Host Integration",
                color = TextDim,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusIndicator(
                title = "SDK Ready",
                icon = Icons.Default.Check,
                isActive = true,
                modifier = Modifier.weight(1f)
            )
            StatusIndicator(
                title = "Auto-Sync",
                icon = Icons.Default.Cloud,
                isActive = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Measurement options
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MeasurementCard(
                title = "Standard Network Capture",
                description = "Continuous background monitoring of signal strength, latency, and connection quality.",
                icon = Icons.Default.NetworkCell,
                accentColor = PrimaryBlue,
                buttonText = "Start Capture",
                onClick = onStartStandardCapture
            )

            MeasurementCard(
                title = "FTP Speed Test",
                description = "Comprehensive upload and download speed assessment with automatic result submission.",
                icon = Icons.Default.Cloud,
                accentColor = AccentPurple,
                buttonText = "Run Test",
                onClick = onStartFTPCapture
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Features section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Fire-and-Forget Operation",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            FeatureItem(
                icon = Icons.Default.LocationOn,
                title = "Background Processing",
                description = "Measurements run in the background without blocking the UI"
            )

            FeatureItem(
                icon = Icons.Default.Check,
                title = "Automatic Upload",
                description = "Results are automatically uploaded to our servers"
            )

            FeatureItem(
                icon = Icons.Default.Cloud,
                title = "No User Wait",
                description = "User can continue using the app immediately after triggering"
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Info box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = BgDarkCard)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Integration Example",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "This Fragment demonstrates how to integrate the Network Measurement SDK into your app. Initialize the SDK with init(this, \"AppName\") and call startCrowdSourcingUploading() whenever you want to trigger measurements.",
                    color = TextDim,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun MeasurementCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgDarkCard)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.15f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = description,
                        color = TextDim,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Text(buttonText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun StatusIndicator(
    title: String,
    icon: ImageVector,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) SuccessGreen.copy(alpha = 0.15f)
            else Color.Gray.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) SuccessGreen else Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .padding(bottom = 4.dp)
            )
            Text(
                text = title,
                color = if (isActive) SuccessGreen else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = RoundedCornerShape(8.dp),
            color = PrimaryBlue.copy(alpha = 0.15f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = TextDim,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}


