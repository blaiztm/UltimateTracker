package com.example.ultimatetracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.ultimatetracker.data.model.WatchCategory
import com.example.ultimatetracker.data.model.builtInTypeTitleRes

@Composable
fun mediaTypeLabel(type: String): String = builtInTypeTitleRes(type)?.let { stringResource(it) } ?: type

@Composable
fun categoryLabel(category: WatchCategory): String = stringResource(category.titleRes)
