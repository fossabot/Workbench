package com.alaeri.cats.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Observer
import androidx.navigation.findNavController
import com.alaeri.cats.app.ui.cats.CatsFragment
import com.alaeri.cats.app.ui.login.LoginFragment
import com.alaeri.cats.app.ui.login.LoginState
import com.alaeri.cats.app.ui.login.LoginViewModel
import org.koin.android.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {

    private val loginViewModel: LoginViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
    }

    override fun onResume() {
        super.onResume()
        loginViewModel.onLogoutClicked()
    }
}
