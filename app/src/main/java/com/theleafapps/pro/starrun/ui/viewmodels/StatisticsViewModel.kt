package com.theleafapps.pro.starrun.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.theleafapps.pro.starrun.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    val mainRepository: MainRepository
) : ViewModel() {
}