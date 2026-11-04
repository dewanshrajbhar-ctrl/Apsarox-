package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppNavTab
import com.example.ui.viewmodel.ApsaRoxViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        init {
            try {
                android.system.Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                android.system.Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "swrast", true)
                android.system.Os.setenv("MESA_DEBUG", "0", true)
                android.system.Os.setenv("MESA_LOG_LEVEL", "silent", true)
            } catch (e: Throwable) {
                // Ignore
            }
        }
    }

    private val viewModel: ApsaRoxViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: ApsaRoxViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val notification by viewModel.notification.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showSecurityDialog by remember { mutableStateOf(false) }

    // Handle back button: if not on DASHBOARD, return to DASHBOARD
    BackHandler(enabled = currentTab != AppNavTab.DASHBOARD) {
        viewModel.currentTab.value = AppNavTab.DASHBOARD
    }

    // Show snackbar when notification state changes
    LaunchedEffect(notification) {
        notification?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it.message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearNotification()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🧾 ${settings.companyName}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = currentTab.title,
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // E2EE Security Vault status indicator
                    IconButton(
                        onClick = { showSecurityDialog = true },
                        modifier = Modifier.testTag("top_bar_e2ee_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "E2EE Secured",
                            tint = com.example.ui.theme.StatusGreen
                        )
                    }

                    // Quick Voice / Audio Transcribe action button
                    IconButton(
                        onClick = { viewModel.currentTab.value = AppNavTab.GEMINI_AI },
                        modifier = Modifier.testTag("top_bar_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Transcription",
                            tint = if (currentTab == AppNavTab.GEMINI_AI) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Settings quick jump
                    IconButton(
                        onClick = { viewModel.currentTab.value = AppNavTab.SETTINGS },
                        modifier = Modifier.testTag("top_bar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Settings",
                            tint = if (currentTab == AppNavTab.SETTINGS) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    AppNavTab.DASHBOARD to Icons.Default.EventAvailable,
                    AppNavTab.PAYROLL to Icons.Default.Payments,
                    AppNavTab.EMPLOYEES to Icons.Default.Group,
                    AppNavTab.REPORTS to Icons.Default.Analytics,
                    AppNavTab.GEMINI_AI to Icons.Default.AutoAwesome
                )

                tabs.forEach { (tab, icon) ->
                    val isSelected = currentTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { viewModel.currentTab.value = tab },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.outlineVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.outlineVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppNavTab.DASHBOARD -> DashboardAttendanceScreen(viewModel)
                AppNavTab.PAYROLL -> PayrollFinanceScreen(viewModel)
                AppNavTab.EMPLOYEES -> EmployeesHolidaysScreen(viewModel)
                AppNavTab.REPORTS -> ReportsScreen(viewModel)
                AppNavTab.GEMINI_AI -> GeminiCopilotScreen(viewModel)
                AppNavTab.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }

    if (showSecurityDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityDialog = false },
            icon = {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = com.example.ui.theme.StatusGreen,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text("End-to-End Encrypted (E2EE)", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ApsaRox operates on a zero-knowledge, hardware-backed cryptographic architecture.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("🛡️ AES-256-GCM authenticated cipher", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("🔑 Android KeyStore hardware master key", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("🔒 PBKDF2-HMAC-SHA256 encrypted backups (.enc)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Text("✅ SHA-256 cryptographic tamper verification", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        text = "No unencrypted payroll or attendance records ever leave your device.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSecurityDialog = false
                        viewModel.currentTab.value = AppNavTab.SETTINGS
                    }
                ) {
                    Text("Security Vault & Backups")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showSecurityDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
