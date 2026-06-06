package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.Contact
import com.example.ui.ContactsViewModel
import java.util.UUID
import kotlin.math.absoluteValue

// Colors list for name initials avatars matching Clean Minimalism palettes
val avatarPastels = listOf(
    Color(0xFFCCE8E8), // Mint Teal
    Color(0xFFDDE3EA), // Space Gray
    Color(0xFFF4E0D6), // Soft Terracotta
    Color(0xFFE0F2FE), // Sky Blue
    Color(0xFFDCFCE7), // Spring Green
    Color(0xFFF3E8FF)  // Light Purple
)
val avatarTextColors = listOf(
    Color(0xFF051F1F),
    Color(0xFF191C1C),
    Color(0xFF2A180E),
    Color(0xFF0369A1),
    Color(0xFF15803D),
    Color(0xFF6B21A8)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsListScreen(
    navController: NavController,
    viewModel: ContactsViewModel,
    onRequestPermission: () -> Unit,
    hasReadPermission: Boolean
) {
    // Force RTL interface structure
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val searchQuery by viewModel.searchQuery.collectAsState()
        val selectedTab by viewModel.selectedTab.collectAsState()
        val contacts by viewModel.contactsState.collectAsState()

        var showDeleteDialogForContact by remember { mutableStateOf<Contact?>(null) }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "جهات الاتصال",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("add_edit") },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("add_contact_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة مسجل")
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Search field (Filled Style)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("ابحث بالاسم، بريد، أو هاتف...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "مسح")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("search_input"),
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color(0xFFEEF1EF),
                        unfocusedContainerColor = Color(0xFFEEF1EF)
                    ),
                    singleLine = true
                )

                // Tab Selection for All vs Favorites
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        }
                    },
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        text = {
                            Text(
                                "الكل (${if (selectedTab == 0) contacts.size else "..."})",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        },
                        modifier = Modifier.testTag("all_contacts_tab")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedTab == 1) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "المفضلة (${if (selectedTab == 1) contacts.size else "..."})",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        },
                        modifier = Modifier.testTag("favorites_tab")
                    )
                }

                // Call Permission banner if read contacts is missing
                if (!hasReadPermission) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مزامنة جهات اتصال الهاتف",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "اضغط للسماح بعرض جهات الاتصال المخزنة على جهازك تلقائياً.",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onRequestPermission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تفعيل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Contacts list or empty state
                if (contacts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = if (selectedTab == 1) Icons.Outlined.StarBorder else Icons.Outlined.PersonOutline,
                                contentDescription = null,
                                modifier = Modifier.size(72.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (selectedTab == 1) "لا يوجد جهات اتصال مضافة للمفضلة بعد" else "لا يوجد جهات اتصال تطابق البحث أو مسجلة بعد",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (selectedTab == 1) "اضغط على رمز النجمة بجوار أي جهة اتصال لإضافتها لقائمة المفضلة الخاصة بك." else "قم بإضافة جهة اتصال جديدة بالنقر على زر الإضافة (+) أدناه.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                            )
                        }
                    }
                } else {
                    // Clean Minimalism Premium Horizontal Favorites Row
                    val favContacts = remember(contacts) { contacts.filter { it.isFavorite } }
                    if (selectedTab == 0 && searchQuery.isBlank() && favContacts.isNotEmpty()) {
                        Text(
                            text = "المفضلة",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 10.dp)
                        )
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            items(favContacts, key = { "fav_" + it.id }) { fav ->
                                val favColorIndex = fav.name.hashCode().absoluteValue % avatarPastels.size
                                val fBgColor = avatarPastels[favColorIndex]
                                val fTextColor = avatarTextColors[favColorIndex]
                                val fInitial = fav.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "ق"

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .width(72.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { navController.navigate("detail/${fav.id}") }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(fBgColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = fInitial,
                                            color = fTextColor,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = fav.name,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Text(
                            text = "جميع جهات الاتصال",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp)
                        )
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            ContactRowItem(
                                contact = contact,
                                onClick = { navController.navigate("detail/${contact.id}") },
                                onFavoriteToggle = { viewModel.toggleFavorite(contact) },
                                onLongClick = { showDeleteDialogForContact = contact }
                            )
                        }
                    }
                }
            }
        }

        // Quick delete confirmation dialog on Long Click
        showDeleteDialogForContact?.let { contact ->
            AlertDialog(
                onDismissRequest = { showDeleteDialogForContact = null },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteContact(contact)
                            showDeleteDialogForContact = null
                        }
                    ) {
                        Text("حذف جهة الاتصال", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialogForContact = null }) {
                        Text("إلغاء الأمر")
                    }
                },
                title = { Text("حذف مسجل جهة الاتصال؟", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من رغبتك في حذف جهة الاتصال '${contact.name}' من التطبيق نهائياً؟") }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactRowItem(
    contact: Contact,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val colorIndex = contact.name.hashCode().absoluteValue % avatarPastels.size
    val bgColor = avatarPastels[colorIndex]
    val tintColor = avatarTextColors[colorIndex]
    val initial = contact.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "ق"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("contact_item_${contact.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDAE5E3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant initials avatar circle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = tintColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = contact.phoneNumbers.firstOrNull() ?: "لا يوجد رقم مسجل",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Quick Call & Favorite Actions row
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (contact.phoneNumbers.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            try {
                                val tel = contact.phoneNumbers.first()
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "لم نتمكن من تشغيل الاتصال", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "اتصال سريع",
                            tint = Color(0xFF10B981) // emerald accent
                        )
                    }
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.testTag("fav_btn_${contact.id}")
                ) {
                    Icon(
                        imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "تفضيل",
                        tint = if (contact.isFavorite) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    navController: NavController,
    viewModel: ContactsViewModel,
    contactId: String
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val context = LocalContext.current
        val contacts by viewModel.contactsState.collectAsState()
        val contact = contacts.find { it.id == contactId }

        var showDeleteDialog by remember { mutableStateOf(false) }

        if (contact == null) {
            Scaffold { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(inner),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("جهة الاتصال غير موجودة أو تم حذفها.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.popBackStack() }) {
                            Text("الرجوع")
                        }
                    }
                }
            }
            return@CompositionLocalProvider
        }

        val colorIndex = contact.name.hashCode().absoluteValue % avatarPastels.size
        val bgColor = avatarPastels[colorIndex]
        val tintColor = avatarTextColors[colorIndex]
        val initial = contact.name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "ق"

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("تفاصيل جهة الاتصال", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleFavorite(contact) }) {
                            Icon(
                                imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                contentDescription = "تفضيل",
                                tint = if (contact.isFavorite) MaterialTheme.colorScheme.tertiary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { navController.navigate("add_edit?id=${contact.id}") }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "تعديل")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Visual Content Card (Big Avatar, Name, Local tag)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Grand Circle initials
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(bgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                color = tintColor,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Complete Display Name
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Type badge
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = if (contact.isLocal) "نوع جهة الاتصال: محلية مستقلة" else "جهة اتصال بنظام الهاتف",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = if (contact.isLocal) Icons.Default.FolderSpecial else Icons.Default.ContactPhone,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Instant actions bar (Call, Message, Share)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DetailHeaderAction(
                                icon = Icons.Default.Call,
                                label = "اتصال",
                                color = Color(0xFF10B981)
                            ) {
                                val primaryNum = contact.phoneNumbers.firstOrNull()
                                if (primaryNum != null) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$primaryNum"))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "الرقم غير متاح ومسجل", Toast.LENGTH_SHORT).show()
                                }
                            }

                            DetailHeaderAction(
                                icon = Icons.Default.Sms,
                                label = "رسالة",
                                color = Color(0xFF3B82F6)
                            ) {
                                val primaryNum = contact.phoneNumbers.firstOrNull()
                                if (primaryNum != null) {
                                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$primaryNum"))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "الرقم غير متاح ومسجل", Toast.LENGTH_SHORT).show()
                                }
                            }

                            DetailHeaderAction(
                                icon = Icons.Default.Share,
                                label = "مشاركة",
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "جهة اتصال: ${contact.name}")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "جهة اتصال: ${contact.name}\nالهاتف: ${contact.phoneNumbers.joinToString()}"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة بيانات جهة الاتصال"))
                                } catch (e: Exception) {
                                    Toast.makeText(context, "تعذر تشغيل مشاركة البيانات", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                // Numbers Block
                DetailSectionCard(title = "أرقام هواتف جهة الاتصال", icon = Icons.Default.Phone) {
                    if (contact.phoneNumbers.isEmpty()) {
                        Text(
                            "لا يوجد هاتف مسجل حالياً.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        contact.phoneNumbers.forEachIndexed { idx, num ->
                            DetailItemRow(
                                label = if (idx == 0) "الرقم الأساسي" else "رقم إضافي ${idx + 1}",
                                value = num,
                                onActionClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                                    context.startActivity(intent)
                                },
                                actionIcon = Icons.Default.Call,
                                actionDescription = "اتصال"
                            )
                        }
                    }
                }

                // Emails Block
                DetailSectionCard(title = "البريد الإلكتروني", icon = Icons.Default.Email) {
                    if (contact.emails.isEmpty()) {
                        Text(
                            "لا يوجد بريد إلكتروني مسجل.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    } else {
                        contact.emails.forEach { email ->
                            DetailItemRow(
                                label = "عنوان البريد الإلكتروني",
                                value = email,
                                onActionClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "البريد الإلكتروني غير مهيأ بجهازك", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                actionIcon = Icons.Default.Send,
                                actionDescription = "إرسال بريد"
                            )
                        }
                    }
                }

                // Additional Custom Notes Block
                if (!contact.notes.isNullOrBlank()) {
                    DetailSectionCard(title = "ملاحظات وتفاصيل", icon = Icons.Default.Notes) {
                        Text(
                            text = contact.notes,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Deleting Confirmation dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteContact(contact)
                            showDeleteDialog = false
                            navController.popBackStack()
                        }
                    ) {
                        Text("نعم، أحذف", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("إلغاء الأمر")
                    }
                },
                title = { Text("تأكيد الحذف", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد من حذف '${contact.name}' نهائياً؟ لا يمكن التراجع عن هذه الخطوة.") }
            )
        }
    }
}

@Composable
fun DetailHeaderAction(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .width(64.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DetailSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDAE5E3)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

@Composable
fun DetailItemRow(
    label: String,
    value: String,
    onActionClick: () -> Unit,
    actionIcon: ImageVector,
    actionDescription: String
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable {
                // Clicking on the cell copies the number/email to clipboard for standard high utility
                val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText("جهة اتصال", value))
                Toast.makeText(context, "تم نسخ القيمة للحافظة", Toast.LENGTH_SHORT).show()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onActionClick) {
            Icon(
                imageVector = actionIcon,
                contentDescription = actionDescription,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactScreen(
    navController: NavController,
    viewModel: ContactsViewModel,
    contactId: String?
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val contacts by viewModel.contactsState.collectAsState()
        val isEditing = contactId != null
        val existingContact = if (isEditing) contacts.find { it.id == contactId } else null

        var name by remember { mutableStateOf(existingContact?.name ?: "") }
        var phone1 by remember { mutableStateOf(existingContact?.phoneNumbers?.getOrNull(0) ?: "") }
        var phone2 by remember { mutableStateOf(existingContact?.phoneNumbers?.getOrNull(1) ?: "") }
        var email by remember { mutableStateOf(existingContact?.emails?.getOrNull(0) ?: "") }
        var notes by remember { mutableStateOf(existingContact?.notes ?: "") }

        var nameError by remember { mutableStateOf(false) }
        var phoneError by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = if (isEditing) "تعديل جهة الاتصال" else "إضافة جهة اتصال جديدة",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Guide/Instructions
                Text(
                    text = "يرجى ملء الحقول أدناه لحفظ مسجل جهة الاتصال داخلياً بالمستودع واستلام العمليات الفورية السريعة.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Name Input Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("الاسم الكامل (مطلوب)") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("name_input"),
                    shape = RoundedCornerShape(12.dp),
                    isError = nameError,
                    singleLine = true
                )
                if (nameError) {
                    Text(
                        text = "الاسم الكامل مطلوب لحفظ جهة الاتصال.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                }

                // Phone 1 Input Field
                OutlinedTextField(
                    value = phone1,
                    onValueChange = {
                        phone1 = it
                        phoneError = false
                    },
                    label = { Text("رقم الهاتف الأساسي (مطلوب)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(imageVector = Icons.Default.Phone, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("phone_input"),
                    shape = RoundedCornerShape(12.dp),
                    isError = phoneError,
                    singleLine = true
                )
                if (phoneError) {
                    Text(
                        text = "رقم الهاتف الأساسي مطلوب.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                }

                // Phone 2 Input Field
                OutlinedTextField(
                    value = phone2,
                    onValueChange = { phone2 = it },
                    label = { Text("رقم هاتف إضافي (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(imageVector = Icons.Default.PhoneEnabled, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Email Input Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("البريد الإلكتروني (اختياري)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = { Icon(imageVector = Icons.Default.Email, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                // Notes Input Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات إضافية عن جهة الاتصال...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Assignment, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                // Action Save Button
                Button(
                    onClick = {
                        val isNameEmpty = name.isBlank()
                        val isPhoneEmpty = phone1.isBlank()

                        if (isNameEmpty) nameError = true
                        if (isPhoneEmpty) phoneError = true

                        if (!isNameEmpty && !isPhoneEmpty) {
                            val phoneNumbersList = mutableListOf<String>().apply {
                                add(phone1.trim())
                                if (phone2.isNotBlank()) add(phone2.trim())
                            }
                            val emailsList = mutableListOf<String>().apply {
                                if (email.isNotBlank()) add(email.trim())
                            }

                            val saveContact = Contact(
                                id = existingContact?.id ?: UUID.randomUUID().toString(),
                                name = name.trim(),
                                phoneNumbers = phoneNumbersList,
                                emails = emailsList,
                                photoUri = existingContact?.photoUri,
                                isFavorite = existingContact?.isFavorite ?: false,
                                isLocal = existingContact?.isLocal ?: true, // Added contacts are saved locally
                                notes = notes.trim().ifEmpty { null }
                            )

                            viewModel.addOrUpdateContact(saveContact)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("save_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حفظ بيانات جهة الاتصال", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
