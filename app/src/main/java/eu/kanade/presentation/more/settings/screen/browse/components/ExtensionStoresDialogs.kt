package eu.kanade.presentation.more.settings.screen.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.network.NetworkPreferences
import kotlinx.coroutines.delay
import tachiyomi.i18n.MR
import tachiyomi.i18n.kmk.KMR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.time.Duration.Companion.seconds

@Composable
fun ExtensionStoreCreateDialog(
    onDismissRequest: () -> Unit,
    onCreate: (String) -> Unit,
    storeIndexUrls: Set<String>,
    processing: Boolean,
    errorMessage: String?,
) {
    val state = rememberTextFieldState()
    val storeAlreadyExists by remember(storeIndexUrls) {
        derivedStateOf {
            val indexUrl = state.text.toString()
            storeIndexUrls.contains(indexUrl)
        }
    }

    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.extensionStoresScreen_addStore_title))
        },
        text = {
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                state = state,
                label = {
                    Text(text = stringResource(MR.strings.extensionStoresScreen_addStoreInput_inputLabel))
                },
                supportingText = {
                    val msgRes = if (storeAlreadyExists) {
                        MR.strings.extensionStoresScreen_addStore_alreadyExists
                    } else {
                        MR.strings.information_required_plain
                    }
                    Text(text = errorMessage ?: stringResource(msgRes))
                },
                isError = errorMessage != null || storeAlreadyExists,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                lineLimits = TextFieldLineLimits.SingleLine,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(state.text.toString()) },
                enabled = !processing && state.text.isNotEmpty() && !storeAlreadyExists,
            ) {
                Text(
                    text = stringResource(
                        resource = if (processing) {
                            MR.strings.extensionStoresScreen_addStore_processing
                        } else {
                            MR.strings.action_add
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )

    LaunchedEffect(focusRequester) {
        // TODO: https://issuetracker.google.com/issues/204502668
        delay(0.1.seconds)
        focusRequester.requestFocus()
    }
}

@Composable
fun ExtensionStoreDeleteDialog(
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
    storeName: String,
    storeIndexUrl: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.extensionStoresScreen_deleteStore_title))
        },
        text = {
            Text(text = stringResource(MR.strings.extensionStoresScreen_deleteStore_body, storeName, storeIndexUrl))
        },
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onDismissRequest()
            }) {
                Text(text = stringResource(MR.strings.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}

// KMK -->
@Composable
fun ExtensionStoreTokenDialog(
    onDismissRequest: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    val networkPreferences = remember { Injekt.get<NetworkPreferences>() }
    val initialToken = remember { networkPreferences.extensionStoreToken().get() }
    var token by remember { mutableStateOf(TextFieldValue(initialToken)) }
    var hideToken by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(KMR.strings.extension_stores_github_token))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(KMR.strings.extension_stores_github_token_dialog_summary))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = token,
                    onValueChange = { token = it },
                    label = {
                        Text(text = stringResource(KMR.strings.extension_stores_github_token_input))
                    },
                    trailingIcon = {
                        IconButton(onClick = { hideToken = !hideToken }) {
                            Icon(
                                imageVector = if (hideToken) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                contentDescription = null,
                            )
                        }
                    },
                    visualTransformation = if (hideToken) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(token.text.toString()) },
                enabled = token.text.isNotEmpty(),
            ) {
                Text(text = stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onClear()
                        onDismissRequest()
                    },
                    enabled = initialToken.isNotEmpty(),
                ) {
                    Text(text = stringResource(KMR.strings.extension_stores_github_token_clear))
                }
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            }
        },
    )
}
// KMK <--

@Composable
fun ExtensionStoreConfirmDialog(
    onDismissRequest: () -> Unit,
    onCreate: () -> Unit,
    storeIndexUrl: String,
    storeAlreadyExists: Boolean,
    processing: Boolean,
    errorMessage: String?,
) {
    val state = rememberTextFieldState(initialText = storeIndexUrl)
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(MR.strings.extensionStoresScreen_addStore_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(MR.strings.extensionStoresScreen_addStoreDeeplink_bodyText))
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    state = state,
                    readOnly = true,
                    supportingText = when {
                        storeAlreadyExists -> {
                            {
                                Text(text = stringResource(MR.strings.extensionStoresScreen_addStore_alreadyExists))
                            }
                        }
                        errorMessage != null -> {
                            {
                                Text(text = errorMessage)
                            }
                        }
                        else -> null
                    },
                    isError = errorMessage != null || storeAlreadyExists,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCreate, enabled = !storeAlreadyExists && !processing) {
                Text(
                    text = stringResource(
                        resource = if (processing) {
                            MR.strings.extensionStoresScreen_addStore_processing
                        } else {
                            MR.strings.action_add
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(MR.strings.action_cancel))
            }
        },
    )
}
