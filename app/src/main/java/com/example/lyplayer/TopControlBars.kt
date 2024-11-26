package com.example.lyplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun TopControlBar(
    title: String,
    isRightToolbarVisible: Boolean,
    onToggleRightToolbar: () -> Unit,
    onBackClicked: () -> Unit,
    isBackgroundPlayEnabled: Boolean,
    onBackgroundPlayToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    // 顶部工具栏
    if (!isRightToolbarVisible) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f), // 上方颜色
                            Color.Black.copy(alpha = 0.0f)  // 下方颜色
                        )
                    )
                )
                .height(if (isLandscape) (screenHeight / 4).dp else (screenHeight / 12).dp)
                .padding(
                    horizontal = if (isLandscape) (screenWidth / 64).dp else (screenWidth / 32).dp,
                    vertical = if (isLandscape) (screenHeight / 128).dp else (screenHeight / 256).dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(if (isLandscape) (screenHeight / 13).dp else (screenHeight * 0.04f).dp)
                )
            }

            // 视频标题
            Row(
                modifier = Modifier
                    .width(if (isLandscape) (screenWidth * 0.8).dp else (screenWidth * 0.6).dp)
                    .padding(start = (if (isLandscape) (screenWidth / 64).dp else (screenWidth / 32).dp))
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 工具栏按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onToggleRightToolbar,
                    modifier = Modifier
                        .size(if (isLandscape) (screenHeight / 6).dp else (screenHeight * 0.08f).dp)
                        .padding(end = if (isLandscape) (screenWidth / 128).dp else (screenWidth / 64).dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscape) (screenHeight / 13).dp else (screenHeight * 0.04f).dp)
                    )
                }
            }
        }
    }

    // 动态调整右侧工具栏或顶部工具栏
    if (isRightToolbarVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onToggleRightToolbar() }
        ) {
            Box(
                modifier = Modifier
                    .align(if (isLandscape) Alignment.TopEnd else Alignment.TopCenter)
                    .width(if (isLandscape) (screenWidth / 2).dp else screenWidth.dp)
                    .then(
                        if (isLandscape) Modifier.fillMaxHeight()
                        else Modifier.height((screenHeight / 2).dp)
                    )
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(
                        top = if (isLandscape) (screenHeight / 32).dp else 0.dp,
                        end = if (isLandscape) (screenWidth / 32).dp else 0.dp,
                        start = (screenWidth / 32).dp
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    item {
                        Text(
                            text = "工具栏",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                            modifier = Modifier.padding(horizontal = (screenWidth / 64).dp)
                        )
                    }

                    item {
                        Divider(
                            color = Color.Gray,
                            thickness = 1.dp,
                            modifier = Modifier.padding(
                                vertical = (screenHeight / 128).dp,
                                horizontal = (screenWidth / 64).dp
                            )
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp, vertical = 8.dp) // 添加外间距，避免背景裁剪到父布局边界
                                .clip(RoundedCornerShape(4.dp)) // 设置圆角形状
                                .background(Color(0xFF464646).copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = if (isLandscape) (screenWidth / 64).dp else (screenWidth / 32).dp,
                                            vertical = if (isLandscape) (screenHeight / 128).dp else (screenHeight / 256).dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "后台播放",
                                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 8.dp)
                                    )
                                    CustomSwitch(
                                        checked = isBackgroundPlayEnabled,
                                        onCheckedChange = { onBackgroundPlayToggle(it) }
                                    )
                                }

                                Divider(
                                    color = Color.Gray,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(
                                        vertical = (screenHeight / 128).dp,
                                        horizontal = (screenWidth / 64).dp
                                    )
                                )

                                Text(
                                    text = "更多功能即将到来...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                                    modifier = Modifier.padding(
                                        horizontal = if (isLandscape) (screenWidth / 64).dp else (screenWidth / 32).dp,
                                        vertical = if (isLandscape) (screenHeight / 128).dp else (screenHeight / 256).dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

}

@Composable
fun CustomSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp // 判断横竖屏

    // 定义横屏参数，默认是传入的横屏数据
    val currentSwitchWidth = (configuration.screenWidthDp / 16).dp
    val currentSwitchHeight = (configuration.screenHeightDp / 13).dp
    val currentThumbSize = (configuration.screenHeightDp / 19).dp

    val portraitSwitchWidth = (configuration.screenWidthDp / 8).dp  // 竖屏宽度
    val portraitSwitchHeight = (configuration.screenHeightDp / 30).dp  // 竖屏高度
    val portraitThumbSize = (configuration.screenHeightDp / 40).dp  // 竖屏滑块大小

    // 横竖屏动态切换参数
    val switchWidthDp = if (isLandscape) currentSwitchWidth else portraitSwitchWidth
    val switchHeightDp = if (isLandscape) currentSwitchHeight else portraitSwitchHeight
    val thumbSizeDp = if (isLandscape) currentThumbSize else portraitThumbSize

    // 主体样式
    val trackColor = if (checked) Color(0xFFFF0000) else Color(0xFFB3B3B3) // 滑动条颜色
    val thumbColor = Color.White // 滑块颜色

    Box(
        modifier = Modifier
            .size(switchWidthDp, switchHeightDp)
            .background(
                color = trackColor,
                shape = RoundedCornerShape(50) // 圆角矩形
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onCheckedChange(!checked) }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(switchHeightDp / 8) // 动态内边距，基于高度调整
        ) {
            Box(
                modifier = Modifier
                    .size(thumbSizeDp)
                    .background(
                        color = thumbColor,
                        shape = RoundedCornerShape(50)
                    )
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
            )
        }
    }
}


