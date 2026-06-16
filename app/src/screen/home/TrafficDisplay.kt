/*
 * This file is part of YumeBox.
 */
package com.github.yumelira.yumebox.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.yumelira.yumebox.common.AppConstants
import com.github.yumelira.yumebox.common.util.formatBytesForDisplay
import com.github.yumelira.yumebox.core.model.TunnelState
import com.github.yumelira.yumebox.data.model.ProxyMode
import com.github.yumelira.yumebox.domain.model.TrafficData
import com.github.yumelira.yumebox.presentation.icon.AppMd3Icons
import com.github.yumelira.yumebox.presentation.theme.AppMotion
import com.github.yumelira.yumebox.presentation.theme.AppTheme
import com.github.yumelira.yumebox.presentation.theme.UiDp
import dev.oom_wg.purejoy.mlang.MLang

@Composable
fun TrafficDisplay(
    trafficNow: TrafficData,
    profileName: String?,
    tunnelMode: TunnelState.Mode?,
    controlState: HomeProxyControlState,
    proxyMode: ProxyMode,
    isEnabled: Boolean,
    onClick: () -> Unit,
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
        // 下载速度卡片
        SpeedCard(
            title = "DOWNLOAD",
            speed = trafficNow.download,
            profileName = profileName,
            tunnelMode = tunnelMode
        )

        // 上传速度
        UploadSection(
            uploadSpeed = trafficNow.upload,
            controlState = controlState,
            proxyMode = proxyMode
        )

        // 模式切换
        OutboundModeSelector(currentMode = tunnelMode)

        // 大按钮
        StartButton(
            controlState = controlState,
            isEnabled = isEnabled,
            onClick = onClick
        )
    }
}

@Composable
private fun SpeedCard(
    title: String,
    speed: Long,
    profileName: String?,
    tunnelMode: TunnelState.Mode?
) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity
    val (value, unit) = formatBytesForDisplay(speed)

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.space20),
            verticalArrangement = Arrangement.spacedBy(spacing.space12)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = AppConstants.UI.TRAFFIC_FONT_SIZE,
                            lineHeight = AppConstants.UI.TRAFFIC_FONT_SIZE
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = AppConstants.UI.TRAFFIC_UNIT_FONT_SIZE
                        ),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = opacity.medium),
                        modifier = Modifier.padding(bottom = spacing.space14, start = spacing.space8)
                    )
                }

                if (profileName != null || tunnelMode != null) {
                    ProfileModeBadge(profileName = profileName, tunnelMode = tunnelMode)
                }
            }
        }
    }
}

@Composable
private fun ProfileModeBadge(
    profileName: String?,
    tunnelMode: TunnelState.Mode?
) {
    if (profileName == null && tunnelMode == null) return

    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity

    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = opacity.subtle),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(componentSizes.statusCapsuleHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(spacing.space8)
        ) {
            Text(
                text = profileName ?: MLang.Home.Traffic.NoProfile,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Box(
                modifier = Modifier
                    .size(spacing.space4)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Text(
                text = tunnelMode.toDisplayName(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun UploadSection(
    uploadSpeed: Long,
    controlState: HomeProxyControlState,
    proxyMode: ProxyMode
) {
    val spacing = AppTheme.spacing
    val (value, unit) = formatBytesForDisplay(uploadSpeed)
    val isRunning = controlState == HomeProxyControlState.Running

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.space12)
        ) {
            Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.space12)
    ) {
        Text(
            text = "UPLOAD",
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "$value $unit",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = MaterialTheme.colorScheme.primary
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.space8),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.animateContentSize(tween(AppMotion.DURATION_FAST, easing = AppMotion.EmphasizedDecelerate))
    ) {
        ProxyStatusCapsule(controlState = controlState)
        AnimatedVisibility(
            visible = isRunning,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(AppMotion.DURATION_FAST, easing = AppMotion.EmphasizedDecelerate)
            ) + fadeIn(tween(AppMotion.DURATION_FAST, easing = AppMotion.EnterEasing)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(AppMotion.DURATION_INSTANT, easing = AppMotion.EmphasizedAccelerate)
            ) + fadeOut(tween(AppMotion.DURATION_INSTANT, easing = AppMotion.ExitEasing))
        ) {
            ProxyTypeCapsule(proxyMode = proxyMode)
        }
            }
        }
    }
}

@Composable
private fun OutboundModeSelector(
    currentMode: TunnelState.Mode?,
    modifier: Modifier = Modifier
) {
    val spacing = AppTheme.spacing
    val opacity = AppTheme.opacity

    val modes = listOf(
        TunnelState.Mode.Rule to "RULE-BASED",
        TunnelState.Mode.Direct to "DIRECT",
        TunnelState.Mode.Global to "GLOBAL"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(spacing.space8)
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = currentMode == mode
            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = opacity.subtle)
            }
            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                color = backgroundColor,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clickable { /* TODO: 切换模式 */ }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun StartButton(
    controlState: HomeProxyControlState,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val spacing = AppTheme.spacing
    val isRunning = controlState == HomeProxyControlState.Running

    val backgroundColor = if (isRunning) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = isEnabled) { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isRunning) AppMd3Icons.Home.StatusRunning else AppMd3Icons.Home.StatusIdle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(spacing.space8))
            Text(
                text = when (controlState) {
                    HomeProxyControlState.Idle -> MLang.Home.Status.TapToStart
                    HomeProxyControlState.Connecting -> MLang.Home.Status.Connecting
                    HomeProxyControlState.Running -> MLang.Home.Status.Running
                    HomeProxyControlState.Disconnecting -> MLang.Home.Status.Disconnecting
                },
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun ProxyTypeCapsule(proxyMode: ProxyMode) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        color = primary.copy(alpha = opacity.subtle),
        shape = RoundedCornerShape(50),
        modifier = Modifier.height(componentSizes.statusCapsuleHeight)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.space12),
            horizontalArrangement = Arrangement.spacedBy(spacing.space6)
        ) {
            Icon(
                imageVector = when (proxyMode) {
                    ProxyMode.Tun -> AppMd3Icons.Home.ProxyModeVpn
                    ProxyMode.RootTun -> AppMd3Icons.Home.ProxyModeTun
                    ProxyMode.Http -> AppMd3Icons.Home.ProxyModeHttp
                },
                contentDescription = null,
                tint = primary,
                modifier = Modifier.size(spacing.space12)
            )
            Text(
                text = when (proxyMode) {
                    ProxyMode.Tun -> MLang.Home.ProxyMode.Vpn
                    ProxyMode.RootTun -> MLang.Home.ProxyMode.Tun
                    ProxyMode.Http -> MLang.Home.ProxyMode.Http
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = primary
            )
        }
    }
}

@Composable
private fun ProxyStatusCapsule(controlState: HomeProxyControlState) {
    val spacing = AppTheme.spacing
    val componentSizes = AppTheme.sizes
    val opacity = AppTheme.opacity
    val primary = MaterialTheme.colorScheme.primary

    Surface(
        color = primary.copy(alpha = opacity.subtle),
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .height(componentSizes.statusCapsuleHeight)
            .animateContentSize(tween(AppMotion.DURATION_FAST, easing = AppMotion.EmphasizedDecelerate))
    ) {
        AnimatedContent(
            targetState = controlState,
            transitionSpec = {
                (slideInHorizontally(
                    initialOffsetX = { it / 2 },
                    animationSpec = tween(AppMotion.DURATION_FAST, easing = AppMotion.EmphasizedDecelerate)
                ) + fadeIn(tween(AppMotion.DURATION_FAST, easing = AppMotion.EnterEasing))
                ).togetherWith(
                    slideOutHorizontally(
                        targetOffsetX = { -it / 2 },
                        animationSpec = tween(AppMotion.DURATION_INSTANT, easing = AppMotion.EmphasizedAccelerate)
                    ) + fadeOut(tween(AppMotion.DURATION_INSTANT, easing = AppMotion.ExitEasing))
                )
            },
            label = "CapsuleStateTransition"
        ) { state ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = spacing.space12),
                horizontalArrangement = Arrangement.spacedBy(spacing.space6)
            ) {
                Icon(
                    imageVector = when (state) {
                        HomeProxyControlState.Idle -> AppMd3Icons.Home.StatusIdle
                        HomeProxyControlState.Connecting,
                        HomeProxyControlState.Disconnecting -> AppMd3Icons.Home.StatusWaiting
                        HomeProxyControlState.Running -> AppMd3Icons.Home.StatusRunning
                    },
                    contentDescription = null,
                    tint = primary,
                    modifier = Modifier.size(spacing.space12)
                )
                Text(
                    text = when (state) {
                        HomeProxyControlState.Idle -> MLang.Home.Status.TapToStart
                        HomeProxyControlState.Connecting -> MLang.Home.Status.Connecting
                        HomeProxyControlState.Running -> MLang.Home.Status.Running
                        HomeProxyControlState.Disconnecting -> MLang.Home.Status.Disconnecting
                    },
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = primary
                )
            }
        }
    }
}

private fun TunnelState.Mode?.toDisplayName(): String = when (this) {
    TunnelState.Mode.Direct -> "Direct"
    TunnelState.Mode.Global -> "Global"
    TunnelState.Mode.Rule -> "Rule"
    else -> "Rule"
}
