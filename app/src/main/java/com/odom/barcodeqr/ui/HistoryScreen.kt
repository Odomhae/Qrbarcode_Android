package com.odom.barcodeqr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import com.odom.barcodeqr.history.HistoryViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue

@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val memos by viewModel.listHistory.collectAsState()

    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("메모 입력") },
            modifier = Modifier.fillMaxWidth()
        )

       // Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (input.isNotBlank()) {
                    viewModel.addHistory(input)
                    input = ""
                }
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("저장")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(memos) { qrHistory ->
                Text(
                    text = qrHistory.qrString,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(Color(0xFFEFEFEF))
                        .padding(12.dp)
                )
            }
        }
    }
}
