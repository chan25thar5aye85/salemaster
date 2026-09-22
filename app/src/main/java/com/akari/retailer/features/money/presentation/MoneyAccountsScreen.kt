package com.akari.retailer.features.money.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.akari.retailer.R
import com.akari.retailer.RetailApplication
import com.akari.retailer.core.ui.components.AppScreen
import com.akari.retailer.core.ui.theme.AppTypography
import com.akari.retailer.core.ui.theme.Spacing
import com.akari.retailer.features.money.domain.models.MoneyAccount
import com.akari.retailer.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyAccountsScreen(
    navController: NavController? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as RetailApplication
    
    
    val viewModel: MoneyAccountsViewModel = viewModel(
        factory = MoneyAccountsViewModelFactory(application.container.moneyAccountRepository)
    )
    
    val state by viewModel.state.collectAsState()

    // Delete confirmation dialog
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.handleEvent(MoneyAccountsEvent.DismissDeleteDialog) },
            title = { Text(stringResource(R.string.delete_account)) },
            text = { Text(stringResource(R.string.delete_account_confirmation)) },
            confirmButton = {
                Button(
                    onClick = { viewModel.handleEvent(MoneyAccountsEvent.ConfirmDelete) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.handleEvent(MoneyAccountsEvent.DismissDeleteDialog) }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    AppScreen(
        title = "💰 " + stringResource(R.string.money_accounts),
        showBackButton = true,
        onBackClick = onBack,
        showAddButton = true,
        onAddClick = { viewModel.handleEvent(MoneyAccountsEvent.ShowAddDialog) }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Total Balance Card
            if (state.activeAccounts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.medium),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.large),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.total_balance),
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "${state.totalBalance}",
                            style = AppTypography.header.copy(fontSize = 28.sp),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.activeAccounts.size} ${stringResource(R.string.accounts)}",
                            style = AppTypography.small,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Action Buttons
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.small)
                    .clickable { navController?.navigate(Routes.TRANSFER_MONEY) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💸 " + stringResource(R.string.transfer_money),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.small)
                    .clickable { navController?.navigate(Routes.EXTERNAL_TRANSFER) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🌐 " + stringResource(R.string.external_transfer),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.small)
                    .clickable { navController?.navigate(Routes.MONEY_TRANSACTIONS) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📋 " + stringResource(R.string.transactions),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Spacing.medium)
                    .clickable { navController?.navigate(Routes.MONEY_ANALYTICS) },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.medium),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 " + stringResource(R.string.money_analytics),
                        style = AppTypography.body,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Loading
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // Error
            if (state.error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("❌", fontSize = 40.sp)
                        Text(
                            text = state.error!!,
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        TextButton(
                            onClick = { viewModel.handleEvent(MoneyAccountsEvent.LoadAccounts) }
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
                return@Column
            }

            // Empty
            if (state.accounts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💰", fontSize = 48.sp)
                        Text(
                            text = stringResource(R.string.no_accounts_yet),
                            style = AppTypography.header,
                            modifier = Modifier.padding(top = Spacing.medium)
                        )
                        Text(
                            text = stringResource(R.string.tap_add_account),
                            style = AppTypography.body,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                return@Column
            }

            // Account List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(Spacing.medium),
                contentPadding = PaddingValues(bottom = Spacing.xxlarge)
            ) {
                items(
                    items = state.accounts,
                    key = { it.id }
                ) { account ->
                    MoneyAccountCard(
                        account = account,
                        onEdit = {
                            viewModel.handleEvent(MoneyAccountsEvent.ShowEditDialog(account))
                        },
                        onDelete = {
                            viewModel.handleEvent(MoneyAccountsEvent.ShowDeleteDialog(account.id))
                        }
                    )
                }
            }
        }
    }

    // Add/Edit Dialog
    if (state.showDialog) {
        MoneyAccountDialog(
            isEditing = state.editingAccount != null,
            name = state.dialogName,
            icon = state.dialogIcon,
            color = state.dialogColor,
            openingBalance = state.dialogOpeningBalance,
            accountNumber = state.dialogAccountNumber,
            notes = state.dialogNotes,
            error = state.error,
            isSaving = state.isSaving,
            onNameChange = { viewModel.handleEvent(MoneyAccountsEvent.DialogNameChanged(it)) },
            onIconChange = { viewModel.handleEvent(MoneyAccountsEvent.DialogIconChanged(it)) },
            onColorChange = { viewModel.handleEvent(MoneyAccountsEvent.DialogColorChanged(it)) },
            onOpeningBalanceChange = { viewModel.handleEvent(MoneyAccountsEvent.DialogOpeningBalanceChanged(it)) },
            onAccountNumberChange = { viewModel.handleEvent(MoneyAccountsEvent.DialogAccountNumberChanged(it)) },
            onNotesChange = { viewModel.handleEvent(MoneyAccountsEvent.DialogNotesChanged(it)) },
            onSave = { viewModel.handleEvent(MoneyAccountsEvent.SaveAccount) },
            onDismiss = { viewModel.handleEvent(MoneyAccountsEvent.DismissDialog) }
        )
    }
}

@Composable
fun MoneyAccountCard(
    account: MoneyAccount,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val colorValue = Color(android.graphics.Color.parseColor(account.color))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = colorValue.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(account.icon, fontSize = 24.sp)
                }
            }
            
            Spacer(modifier = Modifier.width(Spacing.medium))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = account.name,
                    style = AppTypography.title
                )
                if (account.accountNumber.isNotEmpty()) {
                    Text(
                        text = account.accountNumber,
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                if (account.isDefault) {
                    Text(
                        text = stringResource(R.string.default_label),
                        style = AppTypography.small,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${account.currentBalance}",
                    style = AppTypography.header,
                    color = if (account.currentBalance >= 0) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.balance),
                    style = AppTypography.small,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            if (!account.isDefault) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit),
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}
