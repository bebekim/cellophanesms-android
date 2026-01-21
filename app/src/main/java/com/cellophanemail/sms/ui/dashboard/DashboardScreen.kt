package com.cellophanemail.sms.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onSenderClick: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        DashboardContent(
            uiState = uiState,
            onStartScan = viewModel::startInitialScan,
            onSenderClick = onSenderClick
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onStartScan: () -> Unit,
    onSenderClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scan Progress Card (only show if scan not completed)
        if (!uiState.scanState.isInitialScanCompleted) {
            ScanProgressCard(
                scanState = uiState.scanState,
                onStartScan = onStartScan
            )
        }

        // Risk Senders - detailed list with toxic+signal breakdown
        RiskSendersCard(
            riskSenders = uiState.riskSenders,
            onSenderClick = onSenderClick
        )

        // 2x2 Message Matrix
        MessageMatrixCard(
            matrix = uiState.analysisMetrics.messageMatrix
        )

        // Four Horsemen Card (only show if there are detections)
        if (uiState.analysisMetrics.horsemenCounts.total > 0) {
            FourHorsemenCard(horsemenCounts = uiState.analysisMetrics.horsemenCounts)
        }
    }
}
