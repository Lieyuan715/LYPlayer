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
    // 获取屏幕尺寸
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    // 仅当右侧工具栏不可见时显示顶部工具栏
    if (!isRightToolbarVisible) {
        // 顶部工具栏
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.01f))
                .height((screenHeight / 4).dp)
                .padding(horizontal = (screenWidth / 64).dp, vertical = (screenHeight / 128).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 返回按钮
            IconButton(onClick = onBackClicked) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size((screenHeight / 13).dp) // 图标大小基于屏幕高度
                )
            }

            // 视频标题
            Spacer(modifier = Modifier.width((screenWidth / 64).dp))
            Row(
                modifier = Modifier
                    .width((screenWidth * 0.8).dp) // 限制宽度为屏幕宽度的前 80%
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    maxLines = 2, // 限制标题显示两行
                    overflow = TextOverflow.Ellipsis, // 超出时显示省略号
                    modifier = Modifier.fillMaxWidth() // 让文字占满 Box 的宽度
                )
            }

            // 工具栏按钮
            Row(
                modifier = Modifier.fillMaxWidth(), // 确保行布局填充整个宽度
                horizontalArrangement = Arrangement.End // 水平方向内容右对齐
            ) {
                IconButton(
                    onClick = onToggleRightToolbar,
                    modifier = Modifier.padding(end = (screenWidth / 128).dp) // 右侧的 padding
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = Color.White,
                        modifier = Modifier.size((screenHeight / 13).dp) // 图标大小基于屏幕高度
                    )
                }
            }
        }
    }

    // 右侧工具栏
    if (isRightToolbarVisible) {
        // 拦截层：捕获点击事件
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onToggleRightToolbar() } // 点击关闭右侧工具栏
        ) {
            // 右侧工具栏内容
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width((screenWidth / 2).dp) // 工具栏宽度占屏幕宽度的 1/2
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(top = (screenHeight / 32).dp, end = (screenWidth / 32).dp, start = (screenWidth / 32).dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 工具栏标题
                    item {
                        Text(
                            text = "工具栏",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.Black),
                            modifier = Modifier.padding(horizontal = (screenWidth / 64).dp)
                        )
                    }

                    // 分隔线
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
                                .fillMaxWidth()
                                .padding((screenHeight / 64).dp) // 外层容器的整体间距
                                .background(Color(0xFFEFEFEF)) // 整体背景颜色，可自定义
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // 后台播放选项
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            horizontal = (screenWidth / 64).dp,
                                            vertical = (screenHeight / 128).dp
                                        ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "后台播放",
                                        style = MaterialTheme.typography.bodyLarge.copy(color = Color.Black),
                                        modifier = Modifier.weight(1f)
                                    )
                                    CustomSwitch(
                                        checked = isBackgroundPlayEnabled,
                                        onCheckedChange = { onBackgroundPlayToggle(it) }
                                    )
                                }

                                // 分隔线
                                Divider(
                                    color = Color.Gray,
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(
                                        vertical = (screenHeight / 128).dp,
                                        horizontal = (screenWidth / 64).dp
                                    )
                                )

                                // 添加更多内容
                                Text(
                                    text = "更多功能即将到来...",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray),
                                    modifier = Modifier.padding(
                                        horizontal = (screenWidth / 64).dp,
                                        vertical = (screenHeight / 128).dp
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
    switchWidth: androidx.compose.ui.unit.Dp = (LocalConfiguration.current.screenWidthDp / 16).dp, // 滑轨默认宽度
    switchHeight: androidx.compose.ui.unit.Dp = (LocalConfiguration.current.screenHeightDp / 13).dp, // 滑轨默认高度
    thumbSize: androidx.compose.ui.unit.Dp = (LocalConfiguration.current.screenHeightDp / 19).dp // 滑块默认大小
) {
    val trackColor = if (checked) Color(0xFFFF0000) else Color(0xFFB3B3B3) // 滑轨颜色
    val thumbColor = Color.White // 滑块颜色

    Box(
        modifier = Modifier
            .size(switchWidth, switchHeight)
            .background(
                color = trackColor,
                shape = RoundedCornerShape(50) // 将圆角值设为 50% 宽度，让滑轨完全圆角
            )
            .clickable(
                indication = null, // 关闭波纹效果
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onCheckedChange(!checked) } // 切换状态
            )
    ) {
        // 滑块容器，设置内间距
        Box(
            modifier = Modifier
                .fillMaxSize() // 占满整个滑轨区域
                .padding((LocalConfiguration.current.screenHeightDp / 64).dp) // 内间距
        ) {
            // 滑块本体
            Box(
                modifier = Modifier
                    .size(thumbSize) // 滑块大小
                    .background(
                        color = thumbColor,
                        shape = RoundedCornerShape(50) // 滑块圆角
                    )
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart) // 滑块位置
            )
        }
    }
}


