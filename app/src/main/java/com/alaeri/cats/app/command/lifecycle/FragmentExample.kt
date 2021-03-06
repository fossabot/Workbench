package com.alaeri.cats.app.command.lifecycle

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.alaeri.cats.app.DefaultIRootCommandLogger
import com.alaeri.command.CommandState
import com.alaeri.command.android.LifecycleCommandContext
import com.alaeri.command.android.LifecycleCommandOwner
import com.alaeri.command.android.invokeCommandWithLifecycle
import com.alaeri.command.android.invokeSuspendingCommandWithLifecycle
import com.alaeri.command.core.command
import com.alaeri.command.core.invoke
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent

@ExperimentalCoroutinesApi
class FragmentExample: Fragment(), LifecycleCommandOwner, KoinComponent {

    private val mutableStateFlow= MutableStateFlow<DefaultIRootCommandLogger?>(null)
    override val commandRoot = buildLifecycleCommandRoot(mutableStateFlow)
    override val lifecycleCommandContext: LifecycleCommandContext = LifecycleCommandContext(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        invokeCommandWithLifecycle<Unit> {
            emit(CommandState.Update("coucou"))
        }
        lifecycleScope.launch {
            invokeSuspendingCommandWithLifecycle {
                emit(CommandState.Update("coucou suspend"))
                invoke { this@FragmentExample.command<String> { "coucou" } }
            }
        }
        val commandRootContext = getKoin().get<DefaultIRootCommandLogger>()
        mutableStateFlow.value = commandRootContext
    }

    override fun onResume() {
        super.onResume()
        invokeCommandWithLifecycle<Unit> {
            emit(CommandState.Update("coucou"))
            invoke { this@FragmentExample.command<String> { "hola" } }
        }
    }
}