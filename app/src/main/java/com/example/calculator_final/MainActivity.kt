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

// Data Class
data class Student(val name: String, val score: Int?)
// This application follows the simple rules u gave to use in calls sir at-least two functions
// Function 1: Format info to see if the format given in the CSV file is okay
fun formatStudentInfo(student: Student): String {
    return student.score?.let { "${student.name} - ${student.score} : Grade ${getGrade(it)}" }
        ?: "${student.name} - No Score"
}

//  Function 2: Check if passing this will check if the students in the CSV file have passed
fun isPassing(student: Student): Boolean {
    val score = student.score ?: return false
    return score >= 60
}

//  Higher-order function
fun processStudents(students: List<Student>, action: (Student) -> Unit) {
    students.forEach { action(it) }
}

// Compute grade
fun getGrade(score: Int): Char = when (score) {
    in 90..100 -> 'A'
    in 80..89 -> 'B'
    in 70..79 -> 'C'
    in 60..69 -> 'D'
    else -> 'F'
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                var students by remember { mutableStateOf(listOf<Student>()) }

                // this is the part that import the CSV file
                val importLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    uri?.let {
                        students = readCSV(it)
                        Toast.makeText(
                            this,
                            "Imported ${students.size} students",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                // this is the part that export the CSV file
                val exportLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("text/csv")
                ) { uri: Uri? ->
                    uri?.let { saveCSV(it, students) }
                }


                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {

                    Text(
                        "Student Grade Calculator",
                        fontSize = 28.sp,
                        color = Color(0xFF1A237E)
                    )

                    Spacer(Modifier.height(24.dp))


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = { importLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel")) }) {
                            Text("Import CSV")
                        }

                        Button(onClick = {
                            if (students.isNotEmpty()) {
                                exportLauncher.launch("Grades_Output.csv")
                            } else {
                                Toast.makeText(this@MainActivity, "No data to export", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Export CSV")
                        }
                    }

                    Spacer(Modifier.height(24.dp))


                    val validScores = students.mapNotNull { it.score }
                    val avgScore = if (validScores.isNotEmpty()) validScores.average() else 0.0
                    Text(
                        "Average Score: ${"%.2f".format(avgScore)}",
                        fontSize = 20.sp,
                        color = Color(0xFF1565C0)
                    )

                    Spacer(Modifier.height(24.dp))


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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = student.name,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = student.score?.let { "${it} (${grade})" } ?: "No Score",
                                        fontSize = 18.sp,
                                        color = gradeColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))


                    val passingStudents = students.filter { isPassing(it) }
                    processStudents(passingStudents) { s ->
                        println("Passing student: ${s.name} with grade ${s.score}")
                    }
                }
            }
        }
    }

    // this the code that read the CSV file
    private fun readCSV(uri: Uri): List<Student> {
        val list = mutableListOf<Student>()
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(inputStream.reader()).useLines { lines ->
                    lines.drop(1).forEach { line ->
                        val tokens = line.split(",")
                        val name = tokens.getOrNull(0)?.trim() ?: ""
                        val score = tokens.getOrNull(1)?.trim()?.toIntOrNull()
                        list.add(Student(name, score))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error reading CSV", Toast.LENGTH_SHORT).show()
        }
        return list
    }

    // this the code that saves the new created CSV file
    private fun saveCSV(uri: Uri, students: List<Student>) {
        try {
            contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                writer.write("Name,Score,Grade\n")
                students.forEach { student ->
                    val scoreText = student.score?.toString() ?: ""
                    val gradeText = student.score?.let { getGrade(it) } ?: "No Score"
                    writer.write("${student.name},$scoreText,$gradeText\n")
                }
            }
            Toast.makeText(this, "CSV exported successfully!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error exporting CSV", Toast.LENGTH_SHORT).show()
        }
    }
}