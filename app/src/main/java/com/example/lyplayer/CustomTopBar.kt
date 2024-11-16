package com.example.lyplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

//主界面顶部工具栏
@Composable
fun CustomTopBar() {
    var expanded by remember { mutableStateOf(false) } // 控制菜单展开状态

    // 使用 Row 来手动创建顶部栏
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray) // 设置顶部栏的背景颜色
            .padding(16.dp) // 设置内边距
    ) {
        // 标题部分，左侧显示应用名称
        Text(
            text = "LYPlayer",  // 确保传递了 text 参数
            modifier = Modifier.weight(1f), // 使用权重分配剩余空间
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )

        // 右侧的更多按钮（省略号）
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }

        // 菜单项展开部分
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false } // 点击其他地方关闭菜单
        ) {
//            DropdownMenuItem(onClick = { /* 菜单项 1 的操作 */ }) {
//                Text("菜单项 1")
//            }
//            DropdownMenuItem(onClick = { /* 菜单项 2 的操作 */ }) {
//                Text("菜单项 2")
//            }
//            DropdownMenuItem(onClick = { /* 菜单项 3 的操作 */ }) {
//                Text("菜单项 3")
//            }
        }
    }
}
