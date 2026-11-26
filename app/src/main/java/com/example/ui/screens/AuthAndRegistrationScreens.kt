package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.auth.signOutFromVarsityBw
import com.example.model.StudentEmailValidator
import com.example.model.University
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CoralCrimson
import com.example.ui.theme.DeepCyanContainer
import com.example.ui.theme.DeepCyanNavy
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedCarbon
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPula
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.IceCyanText
import com.example.ui.theme.KalahariAmber
import com.example.ui.theme.MintPulaText
import com.example.ui.theme.ObsidianVoid

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_campus_hero),
                        contentDescription = "Botswana university campus skyline",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianVoid.copy(alpha = 0.25f),
                                        ObsidianVoid.copy(alpha = 0.72f),
                                        ObsidianVoid
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .statusBarsPadding()
                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        Surface(
                            color = DeepCyanContainer.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = "Verified campus network",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = ".AC.BW EXCLUSIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IceCyanText
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "VarsityBW",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Botswana's student chat, study groups, marketplace & campus hub.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeatureHighlightRow(
                        icon = Icons.Outlined.Groups,
                        title = "Course Study Groups",
                        subtitle = "Post by university, course code & topic, and approve join requests."
                    )
                    FeatureHighlightRow(
                        icon = Icons.Outlined.Forum,
                        title = "Campus Lounges & DMs",
                        subtitle = "Live chats across UB, BIUST, BAC, Botho, and BUAN."
                    )
                    FeatureHighlightRow(
                        icon = Icons.Outlined.Storefront,
                        title = "Student Market in Pula",
                        subtitle = "Buy & sell textbooks, laptops, and off-campus rooms safely."
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!errorMessage.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, CoralCrimson.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = DeepCyanNavy
                    ),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = DeepCyanNavy,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = "Sign in",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Sign in with Google",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Text(
                    text = "Next step verifies your Botswana university .ac.bw email address.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        color = CarbonSurface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, HairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepCyanContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = ElectricCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StudentRegistrationScreen(
    googleEmail: String,
    googleDisplayName: String,
    isSubmitting: Boolean,
    onRegister: (
        studentEmail: String,
        displayName: String,
        university: University,
        faculty: String,
        yearOfStudy: Int,
        bio: String
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val googleIsAcBw = remember(googleEmail) {
        StudentEmailValidator.isValidAcBwEmail(googleEmail)
    }

    var studentEmail by remember(googleEmail) {
        mutableStateOf(if (googleIsAcBw) googleEmail else "")
    }
    var displayName by remember(googleDisplayName) {
        mutableStateOf(googleDisplayName.take(50))
    }
    var selectedUniversity by remember(googleEmail) {
        mutableStateOf(
            if (googleIsAcBw) University.detectFromDomain(StudentEmailValidator.extractDomain(googleEmail))
            else University.UB
        )
    }
    var faculty by remember { mutableStateOf("") }
    var yearOfStudy by remember { mutableIntStateOf(1) }
    var bio by remember { mutableStateOf("") }

    val isEmailValid = StudentEmailValidator.isValidAcBwEmail(studentEmail)
    val emailDomain = StudentEmailValidator.extractDomain(studentEmail)
    val canSubmit = isEmailValid &&
        displayName.trim().length >= 2 &&
        faculty.trim().length >= 2 &&
        !isSubmitting

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Student Verification",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Exclusive to Botswana university students (.ac.bw)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(
                    onClick = { signOutFromVarsityBw(credentialManager, scope) },
                    modifier = Modifier.testTag("switch_account_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = "Switch Google Account",
                        modifier = Modifier.size(18.dp),
                        tint = ElectricCyan
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Switch", color = ElectricCyan)
                }
            }

            // Domain Verification Status Card
            Surface(
                color = if (isEmailValid) EmeraldContainer.copy(alpha = 0.55f) else CarbonSurface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(
                    1.dp,
                    if (isEmailValid) EmeraldPula.copy(alpha = 0.6f) else KalahariAmber.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isEmailValid) Icons.Filled.CheckCircle else Icons.Filled.VerifiedUser,
                        contentDescription = "Domain status",
                        tint = if (isEmailValid) EmeraldPula else KalahariAmber
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEmailValid) {
                                "Verified .ac.bw domain: $emailDomain"
                            } else {
                                "Enter your .ac.bw student email"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isEmailValid) MintPulaText else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (googleIsAcBw) {
                                "Signed in with institutional Google account ($googleEmail)."
                            } else {
                                "Signed in as ${googleEmail.ifBlank { "Google user" }}. Verify with a valid .ac.bw address (e.g. student@ub.ac.bw, name@biust.ac.bw)."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            OutlinedTextField(
                value = studentEmail,
                onValueChange = { input ->
                    studentEmail = input
                    if (StudentEmailValidator.isValidAcBwEmail(input)) {
                        selectedUniversity = University.detectFromDomain(
                            StudentEmailValidator.extractDomain(input)
                        )
                    }
                },
                label = { Text("University Email (.ac.bw)") },
                placeholder = { Text("202201234@ub.ac.bw or name@biust.ac.bw") },
                leadingIcon = {
                    Icon(Icons.Outlined.AlternateEmail, contentDescription = "University Email")
                },
                isError = studentEmail.isNotBlank() && !isEmailValid,
                supportingText = {
                    if (studentEmail.isNotBlank() && !isEmailValid) {
                        Text(
                            text = "Only Botswana academic emails ending in .ac.bw are accepted.",
                            color = CoralCrimson
                        )
                    } else {
                        Text("Supports UB, BIUST, BAC, Botho, BUAN, and all .ac.bw campuses")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_email_input")
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= 50) displayName = it },
                label = { Text("Display Name") },
                placeholder = { Text("e.g. Kabelo Modise") },
                leadingIcon = {
                    Icon(Icons.Outlined.Badge, contentDescription = "Display Name")
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("display_name_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "University Campus",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    University.entries.forEach { uni ->
                        val selected = selectedUniversity == uni
                        FilterChip(
                            selected = selected,
                            onClick = { selectedUniversity = uni },
                            label = { Text(uni.code) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan,
                                containerColor = CarbonSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = HairlineBorder,
                                selectedBorderColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("reg_uni_${uni.code}")
                        )
                    }
                }
                Text(
                    text = "${selectedUniversity.fullName} • ${selectedUniversity.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ElectricCyan
                )
            }

            OutlinedTextField(
                value = faculty,
                onValueChange = { if (it.length <= 60) faculty = it },
                label = { Text("Faculty / Programme") },
                placeholder = { Text("e.g. Computer Science, Accounting, Agriculture") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("faculty_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Year of Study",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..5).forEach { yr ->
                        val selected = yearOfStudy == yr
                        FilterChip(
                            selected = selected,
                            onClick = { yearOfStudy = yr },
                            label = { Text("Yr $yr") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.2f),
                                selectedLabelColor = ElectricCyan,
                                containerColor = CarbonSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = HairlineBorder,
                                selectedBorderColor = ElectricCyan
                            )
                        )
                    }
                }
            }

            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 200) bio = it },
                label = { Text("Campus Bio (Optional)") },
                placeholder = { Text("Courses you're taking, clubs, or study interests") },
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = CarbonSurface,
                    unfocusedContainerColor = CarbonSurface
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    onRegister(
                        studentEmail,
                        displayName,
                        selectedUniversity,
                        faculty,
                        yearOfStudy,
                        bio
                    )
                },
                enabled = canSubmit,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    contentColor = DeepCyanNavy,
                    disabledContainerColor = ElevatedCarbon,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("complete_registration_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = DeepCyanNavy,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Verify .ac.bw & Enter VarsityBW",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
