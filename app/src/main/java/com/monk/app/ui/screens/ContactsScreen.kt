package com.monk.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.monk.app.data.datastore.PreferencesManager
import com.monk.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ContactsScreen - Privacy-First Implementation
 * 
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║  PRIVACY: Only contact IDs are stored, never names or phone numbers.  ║
 * ║  Contact names are fetched from the device at display time only.      ║
 * ║  If a contact is deleted from the device, we gracefully handle it.    ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 */
@Composable
fun ContactsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context) }
    
    var hasContactsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) 
                == PackageManager.PERMISSION_GRANTED
        )
    }
    
    // Privacy: We only store contact IDs
    var whitelistedContactIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showContactPicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasContactsPermission = granted
    }
    
    // Load saved contact IDs
    LaunchedEffect(Unit) {
        whitelistedContactIds = preferencesManager.whitelistedContacts.first()
        isLoading = false
    }
    
    // Save when contacts change
    LaunchedEffect(whitelistedContactIds) {
        if (!isLoading) {
            preferencesManager.setWhitelistedContacts(whitelistedContactIds)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNavigateBack) {
                    Text(
                        "← Back",
                        style = MaterialTheme.typography.labelLarge,
                        color = Primary
                    )
                }
                
                Text(
                    text = "CONTACTS",
                    style = MaterialTheme.typography.labelLarge,
                    letterSpacing = 3.sp,
                    color = TextMuted
                )
                
                Spacer(modifier = Modifier.width(64.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (!hasContactsPermission) {
                // Request permission
                PermissionRequest(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                )
            } else if (whitelistedContactIds.isEmpty()) {
                // Empty state
                EmptyState(
                    onAddContact = { showContactPicker = true }
                )
            } else {
                // Show whitelisted contacts
                WhitelistedContactsList(
                    contactIds = whitelistedContactIds,
                    onRemoveContact = { contactId ->
                        whitelistedContactIds = whitelistedContactIds - contactId
                    },
                    onAddContact = { showContactPicker = true }
                )
            }
        }
        
        // Contact picker dialog
        if (showContactPicker && hasContactsPermission) {
            ContactPickerDialog(
                existingContactIds = whitelistedContactIds,
                onContactSelected = { contactId ->
                    whitelistedContactIds = whitelistedContactIds + contactId
                    showContactPicker = false
                },
                onDismiss = { showContactPicker = false }
            )
        }
    }
}

@Composable
private fun PermissionRequest(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Contact Access",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = Primary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "To whitelist contacts, Monk needs access to your contacts. Names are only shown on your screen — we never store them.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermission,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Text("Grant Access")
        }
    }
}

@Composable
private fun EmptyState(
    onAddContact: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No Whitelisted Contacts",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = Primary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "Add contacts who can always reach you, even during focus mode.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onAddContact,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Contact")
        }
    }
}

@Composable
private fun WhitelistedContactsList(
    contactIds: Set<String>,
    onRemoveContact: (String) -> Unit,
    onAddContact: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "These contacts can always reach you during focus mode.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceElevated,
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.padding(8.dp)
            ) {
                items(contactIds.toList()) { contactId ->
                    // Privacy: Fetch name from device at display time only
                    val contactName = remember(contactId) {
                        getContactNameById(context, contactId) ?: "Unknown Contact"
                    }
                    
                    ContactRow(
                        name = contactName,
                        onRemove = { onRemoveContact(contactId) }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onAddContact,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Primary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Contact")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Privacy reminder
        Text(
            text = "Only contact IDs are saved — names stay on your device.",
            style = MaterialTheme.typography.labelSmall,
            color = TextHint,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ContactRow(
    name: String,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar placeholder
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = Primary
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = Primary,
            modifier = Modifier.weight(1f)
        )
        
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Remove",
                tint = TextMuted
            )
        }
    }
}

@Composable
private fun ContactPickerDialog(
    existingContactIds: Set<String>,
    onContactSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val contacts = remember { getAllContacts(context) }
    var searchQuery by remember { mutableStateOf("") }
    
    val filteredContacts = remember(searchQuery, contacts) {
        contacts
            .filter { it.id !in existingContactIds }
            .filter { searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact") },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contacts") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.height(300.dp)
                ) {
                    items(filteredContacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onContactSelected(contact.id) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = contact.name.firstOrNull()?.uppercase() ?: "?",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Primary
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Text(
                                text = contact.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Divider(color = Divider)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Privacy: Simple data class for contact picker display only.
 * Only the ID is ever stored persistently.
 */
private data class ContactDisplay(
    val id: String,
    val name: String
)

/**
 * Privacy: Fetches contact name from device by ID.
 * This is called at display time only — names are never stored.
 */
private fun getContactNameById(context: android.content.Context, contactId: String): String? {
    return try {
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(0)
            } else {
                null
            }
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Privacy: Fetches all contacts for picker display.
 * Only IDs are stored when a contact is selected.
 */
private fun getAllContacts(context: android.content.Context): List<ContactDisplay> {
    val contacts = mutableListOf<ContactDisplay>()
    
    try {
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY
            ),
            "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
            val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
            
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex)
                val name = cursor.getString(nameIndex)
                
                if (id != null && !name.isNullOrBlank()) {
                    contacts.add(ContactDisplay(id = id, name = name))
                }
            }
        }
    } catch (e: Exception) {
        // Fail silently - empty list is fine
    }
    
    return contacts
}
