package com.example.lyplayer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 通用顶部工具栏组件，支持动态标题
 * @param title 动态标题的状态
 * @param onSettingsClicked 点击设置菜单项时的回调
 * @param modifier 可选的修饰符
 */
@Composable
fun CustomTopBar(
    title: String, // 工具栏标题
    onSettingsClicked: () -> Unit // 点击设置菜单的回调
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray)
            .padding(16.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(
            onClick = { expanded = !expanded },
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Settings") },
                onClick = onSettingsClicked
            )
        }
    }
}


