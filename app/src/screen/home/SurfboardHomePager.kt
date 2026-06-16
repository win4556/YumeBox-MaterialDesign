/*
 * This file is part of YumeBox.
 *
 * YumeBox is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c)  YumeLira 2025 - Present
 *
 */

package com.github.yumelira.yumebox.screen.home

import com.github.yumelira.yumebox.presentation.theme.UiDp
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.common.util.toast
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.presentation.component.LocalNavigator
import com.github.yumelira.yumebox.presentation.component.ScreenLazyColumn
import com.github.yumelira.yumebox.presentation.component.TopBar
import com.github.yumelira.yumebox.presentation.component.combinePaddingValues
import com.github.yumelira.yumebox.presentation.icon.AppMd3Icons
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.common.util.formatBytesForDisplay
import com.github.yumelira.yumebox.data.gateway.IpMonitoringState
import com.github.yumelira.yumebox.screen.home.displayableExternalIp
import com.ramcosta.composedestinations.generated.destinations.TrafficStatisticsScreenDestination
import dev.oom_wg.purejoy.mlang.MLang
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun SurfboardHomePager(
    mainInnerPadding: PaddingValues,
    isActive: Boolean,
) {
    val homeViewModel = koinViewModel<HomeViewModel>()
    val navigator = LocalNavigator.current

    val controlState by homeViewModel.controlState.collectAsState()
    val uiState by homeViewModel.uiState.collectAsState()
    val trafficNow by homeViewModel.trafficNow.collectAsState()
    val profiles by homeViewModel.profiles.collectAsState()
    val profilesLoaded by homeViewModel.profilesLoaded.collectAsState()
    val ipMonitoringState by homeViewModel.ipMonitoringState.collectAsState()
    val recommendedProfile by homeViewModel.recommendedProfile.collectAsState()
    val hasEnabledProfile by homeViewModel.hasEnabledProfile.collectAsState(initial = false)
    val currentProfile by homeViewModel.currentProfile.collectAsState()
    val selectedServerName by homeViewModel.selectedServerName.collectAsState()
    val selectedServerPing by homeViewModel.selectedServerPing.collectAsState()
    val speedHistory by homeViewModel.speedHistory.collectAsState()
    val testingCurrentNodeDelay by homeViewModel.testingCurrentNodeDelay.collectAsState()
    val proxyMode by homeViewModel.proxyMode.collectAsState()
    val tunnelMode by homeViewModel.tunnelMode.collectAsState()
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        homeViewModel.refreshProxyMode()
    }

    LaunchedEffect(isActive) {
        homeViewModel.setHomeScreenActive(isActive)
    }

    DisposableEffect(homeViewModel) {
        onDispose {
            homeViewModel.setHomeScreenActive(false)
        }
    }

    DisposableEffect(lifecycleOwner, homeViewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                homeViewModel.reconcileRuntimeState()
                homeViewModel.refreshProxyMode()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            context.toast(it, Toast.LENGTH_LONG)
            homeViewModel.consumeError()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            context.toast(it, Toast.LENGTH_SHORT)
            homeViewModel.consumeMessage()
        }
    }

    val isRunning = controlState == HomeProxyControlState.Running
    val isProxyEnabled = profilesLoaded && profiles.isNotEmpty() && controlState.canInteract

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopBar(
                title = "Dashboard",
                actions = {
                    IconButton(
                        enabled = isRunning && !testingCurrentNodeDelay,
                        onClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            homeViewModel.testCurrentNodeDelay()
                        },
                    ) {
                        Icon(
                            imageVector = AppMd3Icons.Action.SpeedTest,
                            contentDescription = MLang.Proxy.Action.Test,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(combinePaddingValues(innerPadding, mainInnerPadding))
        ) {
            ScreenLazyColumn(
                innerPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    SurfboardDashboardContent(
                        trafficNow = if (isRunning) TrafficData.from(trafficNow) else TrafficData.ZERO,
                        profileName = currentProfile?.name?.takeIf { isRunning },
                        tunnelMode = tunnelMode.takeIf { isRunning },
                        controlState = controlState,
                        proxyMode = proxyMode,
                        serverName = selectedServerName.takeIf { isRunning },
                        serverPing = selectedServerPing.takeIf { isRunning },
                        ipMonitoringState = ipMonitoringState,
                        speedHistory = speedHistory,
                        isRunning = isRunning,
                        isActive = isActive,
                        isProxyEnabled = isProxyEnabled,
                        onProxyToggle = {
                            if (!hasEnabledProfile || recommendedProfile == null) {
                                context.toast(MLang.ProfilesVM.Error.ProfileNotExist)
                                return@SurfboardDashboardContent
                            }
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            if (!isRunning) {
                                recommendedProfile?.let { profile ->
                                    homeViewModel.startProxy(
                                        profileId = profile.uuid.toString(),
                                        mode = null
                                    )
                                }
                            } else {
                                coroutineScope.launch { homeViewModel.stopProxy() }
                            }
                        },
                        onNavigateToStats = {
                            navigator.navigate(TrafficStatisticsScreenDestination) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(UiDp.dp32)) }
            }
        }
    }
}

@Composable
private fun SurfboardDashboardContent(
    trafficNow: TrafficData,
    profileName: String?,
    tunnelMode: String?,
    controlState: HomeProxyControlState,
    proxyMode: ProxyMode,
    serverName: String?,
    serverPing: Int?,
    ipMonitoringState: IpMonitoringState,
    speedHistory: List<Long>,
    isRunning: Boolean,
    isActive: Boolean,
    isProxyEnabled: Boolean,
    onProxyToggle: () -> Unit,
    onNavigateToStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = AppConstants.UI.DEFAULT_HORIZONTAL_PADDING),
        verticalArrangement = Arrangement.spacedBy(spacing.space16)
    ) {
        // 网络速度卡片
        SurfboardNetworkSpeedCard(
            trafficNow = trafficNow,
            profileName = profileName,
            tunnelMode = tunnelMode,
            speedHistory = speedHistory,
            isRunning = isRunning,
            isActive = isActive,
            onNavigateToStats = onNavigateToStats
        )

        // 上传/下载数值行
        SurfboardTrafficRow(
            uploadSpeed = trafficNow.upload,
            downloadSpeed = trafficNow.download,
            controlState = controlState,
            proxyMode = proxyMode
        )

        // 功能网格区域
        SurfboardFunctionGrid(
            serverName = serverName,
            serverPing = serverPing,
            ipMonitoringState = ipMonitoringState,
            isRunning = isRunning
        )

        // 底部信息区域
        SurfboardBottomInfo(
            controlState = controlState,
            isRunning = isRunning
        )
    }

    // 浮动操作按钮
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppConstants.UI.DEFAULT_HORIZONTAL_PADDING),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = onProxyToggle,
            enabled = isProxyEnabled,
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = spacing.space16)
        ) {
            Icon(
                imageVector = if (isRunning) {
                    AppMd3Icons.Home.StatusRunning
                } else {
                    AppMd3Icons.Home.StatusIdle
                },
                contentDescription = if (isRunning) "Stop" else "Start",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun SurfboardNetworkSpeedCard(
    trafficNow: TrafficData,
    profileName: String?,
    tunnelMode: String?,
    speedHistory: List<Long>,
    isRunning: Boolean,
    isActive: Boolean,
    onNavigateToStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.space16),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 标题
            Text(
                text = "Network Speed",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 速率图表
            SpeedChart(
                speedHistory = speedHistory,
                isRunning = isRunning,
                animateIdle = isActive,
                onClick = onNavigateToStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            )

            // 底部信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = profileName ?: "None",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column {
                    Text(
                        text = "Mode",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    Text(
                        text = tunnelMode ?: "N/A",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun SurfboardTrafficRow(
    uploadSpeed: Long,
    downloadSpeed: Long,
    controlState: HomeProxyControlState,
    proxyMode: ProxyMode,
    modifier: Modifier = Modifier
) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity

    val (uploadValue, uploadUnit) = formatBytesForDisplay(uploadSpeed)
    val (downloadValue, downloadUnit) = formatBytesForDisplay(downloadSpeed)

    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space12)
    ) {
        // 上传
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(80.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.space12),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = AppMd3Icons.Home.ProxyModeVpn,
                    contentDescription = "Upload",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(spacing.space8))
                Text(
                    text = "Upload",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = "$uploadValue $uploadUnit",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 下载
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(80.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(spacing.space12),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = AppMd3Icons.Home.ProxyModeHttp,
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(spacing.space8))
                Text(
                    text = "Download",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
                Text(
                    text = "$downloadValue $downloadUnit",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SurfboardFunctionGrid(
    serverName: String?,
    serverPing: Int?,
    ipMonitoringState: IpMonitoringState,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = AppTheme.spacing

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.space12)
    ) {
        // 第一行：节点信息 & 延迟
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space12)
        ) {
            SurfboardInfoCard(
                title = "Node",
                value = serverName ?: "Unknown",
                modifier = Modifier.weight(1f)
            )
            SurfboardInfoCard(
                title = "Delay",
                value = if (serverPing != null && serverPing <= 1000) "$serverPing ms" else "--",
                modifier = Modifier.weight(1f)
            )
        }

        // 第二行：IP 信息
        SurfboardInfoCard(
            title = "Exit IP",
            value = ipMonitoringState.displayableExternalIp()?.ip ?: "Unknown",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SurfboardInfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val spacing = AppTheme.spacing

    Surface(
        modifier = modifier.height(70.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.space12),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(spacing.space4))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SurfboardBottomInfo(
    controlState: HomeProxyControlState,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val spacing = AppTheme.spacing

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(spacing.space12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.space8)
        ) {
            Icon(
                imageVector = when (controlState) {
                    HomeProxyControlState.Idle -> AppMd3Icons.Home.StatusIdle
                    HomeProxyControlState.Connecting -> AppMd3Icons.Home.StatusWaiting
                    HomeProxyControlState.Running -> AppMd3Icons.Home.StatusRunning
                    HomeProxyControlState.Disconnecting -> AppMd3Icons.Home.StatusWaiting
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = when (controlState) {
                    HomeProxyControlState.Idle -> "Tap to start"
                    HomeProxyControlState.Connecting -> "Connecting..."
                    HomeProxyControlState.Running -> "Running"
                    HomeProxyControlState.Disconnecting -> "Disconnecting..."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
