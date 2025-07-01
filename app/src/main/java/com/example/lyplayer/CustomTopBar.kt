/* 
 * 占位符注释 - CustomTopBar.kt
 * 此文件包含自定义顶部栏相关功能
 */
package com.example.lyplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun CustomTopBar(
    title: String,
    onSettingsClicked: () -> Unit,
    onSortOptionSelected: (SortOption) -> Unit,
    selectedSortOption: SortOption // 新增：接收选中的排序方式
) {
    var expanded by remember { mutableStateOf(false) } // 控制主菜单展开/收起
    var sortOptionsExpanded by remember { mutableStateOf(false) } // 控制排序选项的展开/收起
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp
    val screenWidth = configuration.screenWidthDp

    var buttonPosition by remember { mutableStateOf(Offset(0f, 0f)) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(
                start = (screenWidth / 32).dp,
                top = (screenWidth / 64).dp,
                bottom = (screenWidth / 64).dp
            )
            .height((screenHeight / 16).dp)
    ) {
        Text(
            text = title,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(0.8f),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        val iconButtonModifier = Modifier
            .align(Alignment.CenterEnd)
            .onGloballyPositioned { coordinates ->
                // 记录 IconButton 的位置
                buttonPosition = coordinates.positionInRoot()
            }

        IconButton(
            onClick = { expanded = !expanded }, // 切换主菜单展开状态
            modifier = iconButtonModifier,
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = Color.White
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        x = buttonPosition.x.toInt(),
                        y = (buttonPosition.y + 40).toInt()
                    )
                }
        ) {
            // 使用 CustomDropdownMenu 控制主菜单展开/收起
            if (expanded) {
                CustomDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    menuItems = listOf(
                        MenuItem(
                            title = "设置",
                            onClick = onSettingsClicked,
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                        ),
                        MenuItem(
                            title = "排序选项",
                            onClick = {
                                // 切换排序选项的展开状态
                                sortOptionsExpanded = !sortOptionsExpanded
                            },
                            backgroundColor = Color(0xFF333333),
                            textColor = Color.White,
                        )
                    ),
                    backgroundColor = Color(0xFF212121),
                    shadowElevation = 8.dp,
                )
            }
        }

        // 这里是显示排序选项的逻辑，不受主菜单控制
        if (sortOptionsExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            x = buttonPosition.x.toInt(),
                            y = (buttonPosition.y + 90).toInt() // 适当调整排序选项的位置
                        )
                    }
            ) {
                CustomDropdownMenu(
                    expanded = sortOptionsExpanded,
                    onDismissRequest = { sortOptionsExpanded = false },
                    menuItems = listOf(
                        MenuItem(
                            title = "按名称排序",
                            onClick = {
                                onSortOptionSelected(SortOption.NAME)
                            },
                            backgroundColor = if (selectedSortOption == SortOption.NAME) Color(0xFF666666) else Color(0xFF333333),
                            textColor = Color.White,
                        ),
                        MenuItem(
                            title = "按日期排序",
                            onClick = {
                                onSortOptionSelected(SortOption.DATE)
                            },
                            backgroundColor = if (selectedSortOption == SortOption.DATE) Color(0xFF666666) else Color(0xFF333333),
                            textColor = Color.White,
                        ),
                        MenuItem(
                            title = "按大小排序",
                            onClick = {
                                onSortOptionSelected(SortOption.SIZE)
                            },
                            backgroundColor = if (selectedSortOption == SortOption.SIZE) Color(0xFF666666) else Color(0xFF333333),
                            textColor = Color.White,
                        ),
                        MenuItem(
                            title = "按类型排序",
                            onClick = {
                                onSortOptionSelected(SortOption.TYPE)
                            },
                            backgroundColor = if (selectedSortOption == SortOption.TYPE) Color(0xFF666666) else Color(0xFF333333),
                            textColor = Color.White,
                        )
                    ),
                    backgroundColor = Color(0xFF212121),
                    shadowElevation = 8.dp,
                )
            }
        }
    }
}

@Composable
fun CustomDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    menuItems: List<MenuItem>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black.copy(alpha = 0.8f),
    contentPadding: PaddingValues = PaddingValues(8.dp),
    shadowElevation: Dp = 4.dp
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(backgroundColor) // 背景颜色
            .padding(contentPadding) // 调整菜单内容的内边距
            .shadow(shadowElevation) // 阴影效果
    ) {
        menuItems.forEach { item ->
            DropdownMenuItem(
                text = { Text(item.title, color = item.textColor) }, // 自定义文字颜色
                onClick = {
                    item.onClick()
                    onDismissRequest()
                },
                modifier = Modifier
                    .background(item.backgroundColor) // 自定义项的背景色
                    .padding(item.itemPadding) // 自定义项的内边距
                    .clip(RoundedCornerShape(8.dp)) // 圆角效果
            )
        }
    }
}

data class MenuItem(
    val title: String,
    val onClick: () -> Unit,
    val textColor: Color = Color.White, // 默认文字颜色
    val backgroundColor: Color = Color.Transparent, // 默认背景颜色
    val itemPadding: PaddingValues = PaddingValues(2.dp) // 默认项的内边距
)











