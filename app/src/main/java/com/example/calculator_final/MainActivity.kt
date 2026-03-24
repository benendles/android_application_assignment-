package com.example.calculator_final

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import java.io.BufferedReader

// ── DATA ───────────────────────────

data class Student(val name: String, val score: Int?)

fun getGrade(score: Int): Char = when (score) {
    in 90..100 -> 'A'
    in 80..89  -> 'B'
    in 70..79  -> 'C'
    in 60..69  -> 'D'
    else       -> 'F'
}

fun isPassing(student: Student): Boolean {
    val score = student.score ?: return false
    return score >= 60
}

// ── COLORS ─────────────────────────

private val Navy = Color(0xFF0D1B2A)
private val Accent = Color(0xFF4FC3F7)
private val CardBg = Color(0xFF1E2D42)
private val GradeF = Color(0xFFFF5252)

private fun gradeColor(grade: Char?) = when (grade) {
    'A' -> Color.Green
    'B' -> Color(0xFF69F0AE)
    'C' -> Color.Yellow
    'D' -> Color(0xFFFF9100)
    'F' -> Color.Red
    else -> Color.Gray
}

// ── MAIN ACTIVITY ──────────────────

class MainActivity : ComponentActivity() {

    private var students by mutableStateOf(listOf<Student>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    @Composable
    fun MainScreen() {

        var showDialog by remember { mutableStateOf(false) }

        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                students = readCSV(it)
                Toast.makeText(this, "Imported ${students.size}", Toast.LENGTH_LONG).show()
            }
        }

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv")
        ) { uri ->
            uri?.let { saveCSV(it, students) }
        }

        Box(
            Modifier.fillMaxSize().background(Navy)
        ) {

            Column(Modifier.fillMaxSize().padding(16.dp)) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Accent)
                    Spacer(Modifier.width(10.dp))
                    Text("Grade Book", color = Color.White, fontSize = 24.sp)
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                    Button(
                        onClick = { importLauncher.launch(arrayOf("text/*")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Menu, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Import")
                    }

                    Button(
                        onClick = {
                            if (students.isNotEmpty()) {
                                exportLauncher.launch("Grades.csv")
                            } else {
                                Toast.makeText(this@MainActivity, "No data", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Star, null)
                        Spacer(Modifier.width(5.dp))
                        Text("Export")
                    }
                }

                Spacer(Modifier.height(20.dp))

                LazyColumn {
                    items(students) { student ->
                        StudentCard(student)
                    }
                }
            }

            FloatingActionButton(
                onClick = { showDialog = true },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Add, null)
            }
        }

        if (showDialog) {
            AddStudentDialog(
                onDismiss = { showDialog = false },
                onAdd = { name, score ->
                    students = students + Student(name, score)
                    showDialog = false
                }
            )
        }
    }

    // ── STUDENT CARD ────────────────

    @Composable
    fun StudentCard(student: Student) {
        val grade = student.score?.let { getGrade(it) }

        Card(
            Modifier.fillMaxWidth().padding(6.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg)
        ) {
            Row(Modifier.padding(16.dp)) {

                Text(
                    grade?.toString() ?: "-",
                    color = gradeColor(grade),
                    fontSize = 20.sp
                )

                Spacer(Modifier.width(10.dp))

                Column {
                    Text(student.name, color = Color.White)
                    Text("Score: ${student.score ?: "None"}", color = Color.Gray)
                }
            }
        }
    }

    // ── DIALOG ─────────────────────

    @Composable
    fun AddStudentDialog(onDismiss: () -> Unit, onAdd: (String, Int?) -> Unit) {

        var name by remember { mutableStateOf("") }
        var score by remember { mutableStateOf("") }
        var nameError by remember { mutableStateOf(false) }
        var scoreError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = onDismiss) {
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp)) {

                    Text("Add Student")

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = false },
                        label = { Text("Name") },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text("Name required", color = GradeF) }
                        } else null
                    )

                    OutlinedTextField(
                        value = score,
                        onValueChange = { score = it; scoreError = false },
                        label = { Text("Score") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = scoreError,
                        supportingText = if (scoreError) {
                            { Text("Invalid score", color = GradeF) }
                        } else null
                    )

                    Spacer(Modifier.height(10.dp))

                    Button(onClick = {
                        val parsed = score.toIntOrNull()

                        nameError = name.isBlank()
                        scoreError = score.isNotBlank() && (parsed == null || parsed !in 0..100)

                        if (!nameError && !scoreError) {
                            onAdd(name.trim(), parsed)
                        }
                    }) {
                        Text("Add")
                    }
                }
            }
        }
    }

    // ── CSV ────────────────────────

    private fun readCSV(uri: Uri): List<Student> {
        val list = mutableListOf<Student>()
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(stream.reader()).useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val parts = line.split(",")
                        val name = parts.getOrNull(0)?.trim().orEmpty()
                        val score = parts.getOrNull(1)?.trim()?.toIntOrNull()

                        if (name.isNotBlank()) {
                            list.add(Student(name, score))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error reading CSV", Toast.LENGTH_SHORT).show()
        }
        return list
    }

    private fun saveCSV(uri: Uri, students: List<Student>) {
        try {
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write("Name,Score,Grade\n")
                students.forEach {
                    val score = it.score?.toString() ?: ""
                    val grade = it.score?.let { s -> getGrade(s) } ?: "No Score"
                    writer.write("${it.name},$score,$grade\n")
                }
            }
            Toast.makeText(this, "Exported!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
}