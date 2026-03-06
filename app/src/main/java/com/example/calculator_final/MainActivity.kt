package com.example.calculator_final

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.BufferedReader

data class Student(val name: String, val score: Int?)

fun formatStudentInfo(student: Student): String {
    return student.score?.let {
        "${student.name} - $it : Grade ${getGrade(it)}"
    } ?: "${student.name} - No Score"
}

fun isPassing(student: Student): Boolean {
    val score = student.score ?: return false
    return score >= 60
}

fun processStudents(students: List<Student>, action: (Student) -> Unit) {
    students.forEach(action)
}

fun getGrade(score: Int): Char = when (score) {
    in 90..100 -> 'A'
    in 80..89 -> 'B'
    in 70..79 -> 'C'
    in 60..69 -> 'D'
    else -> 'F'
}

class MainActivity : ComponentActivity() {

    private var students = mutableStateOf(listOf<Student>())

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

        val importLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                students.value = readCSV(it)

                Toast.makeText(
                    this,
                    "Imported ${students.value.size} students",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        val exportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv")
        ) { uri: Uri? ->
            uri?.let { saveCSV(it, students.value) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            TitleSection()

            Spacer(Modifier.height(24.dp))

            ImportExportButtons(importLauncher, exportLauncher)

            Spacer(Modifier.height(24.dp))

            AverageScoreSection(students.value)

            Spacer(Modifier.height(24.dp))

            StudentList(students.value)

            Spacer(Modifier.height(16.dp))

            val passingStudents = students.value.filter { isPassing(it) }

            processStudents(passingStudents) {
                println("Passing student: ${it.name}")
            }
        }
    }

    @Composable
    fun TitleSection() {
        Text(
            text = "Student Grade Calculator",
            fontSize = 28.sp,
            color = Color(0xFF1A237E)
        )
    }

    @Composable
    fun ImportExportButtons(
        importLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
        exportLauncher: androidx.activity.result.ActivityResultLauncher<String>
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            Button(onClick = {
                importLauncher.launch(arrayOf("text/*"))
            }) {
                Text("Import CSV")
            }

            Button(onClick = {

                if (students.value.isNotEmpty()) {
                    exportLauncher.launch("Grades_Output.csv")
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "No data to export",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }) {
                Text("Export CSV")
            }
        }
    }

    @Composable
    fun AverageScoreSection(students: List<Student>) {

        val scores = students.mapNotNull { it.score }

        val average =
            if (scores.isNotEmpty()) scores.average()
            else 0.0

        Text(
            text = "Average Score: ${"%.2f".format(average)}",
            fontSize = 20.sp,
            color = Color(0xFF1565C0)
        )
    }

    @Composable
    fun StudentList(students: List<Student>) {

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            items(students) { student ->

                val grade = student.score?.let { getGrade(it) }

                val gradeColor = when (grade) {
                    'A' -> Color(0xFF2E7D32)
                    'B' -> Color(0xFF388E3C)
                    'C' -> Color(0xFFF9A825)
                    'D' -> Color(0xFFF57C00)
                    'F' -> Color(0xFFD32F2F)
                    else -> Color.Gray
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(student.name, fontSize = 18.sp)

                        Text(
                            student.score?.let { "$it ($grade)" } ?: "No Score",
                            fontSize = 18.sp,
                            color = gradeColor
                        )
                    }
                }
            }
        }
    }

    private fun readCSV(uri: Uri): List<Student> {

        val studentsList = mutableListOf<Student>()

        try {

            contentResolver.openInputStream(uri)?.use { stream ->

                BufferedReader(stream.reader()).useLines { lines ->

                    lines.drop(1).forEach { line ->

                        val tokens = line.split(",")

                        val name = tokens.getOrNull(0)?.trim().orEmpty()

                        val score =
                            tokens.getOrNull(1)
                                ?.trim()
                                ?.toIntOrNull()

                        studentsList.add(Student(name, score))
                    }
                }
            }

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error reading CSV",
                Toast.LENGTH_SHORT
            ).show()
        }

        return studentsList
    }

    private fun saveCSV(uri: Uri, students: List<Student>) {

        try {

            contentResolver.openOutputStream(uri)
                ?.bufferedWriter()
                ?.use { writer ->

                    writer.write("Name,Score,Grade\n")

                    students.forEach {

                        val score =
                            it.score?.toString() ?: ""

                        val grade =
                            it.score?.let { s -> getGrade(s) }
                                ?: "No Score"

                        writer.write("${it.name},$score,$grade\n")
                    }
                }

            Toast.makeText(
                this,
                "CSV exported successfully!",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {

            e.printStackTrace()

            Toast.makeText(
                this,
                "Error exporting CSV",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}