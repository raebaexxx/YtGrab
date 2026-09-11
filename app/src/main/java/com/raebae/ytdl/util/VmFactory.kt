package com.raebae.ytdl.util

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

object VmFactory {
    inline fun <reified T : ViewModel> of(crossinline create: () -> T): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = create() as VM
        }
}
