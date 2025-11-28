package edu.ap.myapplication.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.myapplication.model.Message
import edu.ap.myapplication.model.User

@Composable
fun MessagesScreen() {
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var message by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var showSentMessages by remember { mutableStateOf(false) }
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(currentUser) {
        currentUser?.uid?.let { currentUserId ->
            db.collection("users").get().addOnSuccessListener { result ->
                val userList = result.map { document ->
                    User(document.getString("userId")?.trim() ?: "", document.getString("name") ?: "")
                }
                users = userList.filter { it.userId != currentUserId }
            }

            db.collection("messages")
                .whereEqualTo("to", currentUserId)
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        return@addSnapshotListener
                    }

                    val messageList = mutableListOf<Message>()
                    snapshots?.forEach { doc ->
                        val msg = doc.toObject(Message::class.java)
                        db.collection("users").whereEqualTo("userId", msg.from).get()
                            .addOnSuccessListener { userSnapshot ->
                                if (!userSnapshot.isEmpty) {
                                    msg.fromName = userSnapshot.documents[0].getString("name") ?: "Unknown"
                                }
                                val existingMessage = messages.find { it.timestamp == msg.timestamp && it.from == msg.from }
                                if (existingMessage == null) {
                                    messages = messages + msg
                                }
                            }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row {
            Button(onClick = { showSentMessages = false }) {
                Text("Send Message")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { showSentMessages = true }) {
                Text("Received Messages")
            }
        }

        if (showSentMessages) {
            Text("Received Messages:", style = MaterialTheme.typography.headlineSmall)
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(messages) { msg ->
                    Text("${msg.fromName}: ${msg.message}")
                }
            }
        } else {
            if (selectedUser == null) {
                Text("Select a user to message:", style = MaterialTheme.typography.headlineSmall)
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(users) { user ->
                        Button(onClick = { selectedUser = user }, modifier = Modifier.fillMaxWidth()) {
                            Text(user.name)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text("To: ${selectedUser?.name}", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val messageData = hashMapOf(
                                "from" to currentUser?.uid,
                                "to" to selectedUser?.userId,
                                "message" to message,
                                "timestamp" to System.currentTimeMillis()
                            )
                            db.collection("messages").add(messageData)
                            message = ""
                            selectedUser = null
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Send")
                    }
                    Button(onClick = { selectedUser = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Back")
                    }
                }
            }
        }
    }
}
