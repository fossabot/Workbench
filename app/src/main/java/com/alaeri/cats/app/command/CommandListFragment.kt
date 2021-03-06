package com.alaeri.cats.app.command

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.alaeri.cats.app.DefaultIRootCommandLogger
import com.alaeri.cats.app.command.focus.FocusCommandViewModel
import com.alaeri.cats.app.databinding.CatsFragmentBinding
import com.alaeri.command.AnyCommandRoot
import com.alaeri.command.android.CommandNomenclature
import com.alaeri.command.android.LifecycleCommandContext
import com.alaeri.command.android.LifecycleCommandOwner
import com.alaeri.command.android.invokeCommandWithLifecycle
import com.alaeri.command.core.invokeCommand
import com.alaeri.command.history.id.IndexAndUUID
import com.alaeri.command.history.serialization.SerializableCommandStateAndContext
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.android.scope.lifecycleScope
import org.koin.core.KoinComponent
import org.koin.core.parameter.parametersOf

/**
 * Created by Emmanuel Requier on 05/05/2020.
 */
class CommandListViewModel(private val commandRepository: CommandRepository) : ViewModel(){
    private val mutableLiveData = MutableLiveData<List<SerializableCommandStateAndContext<IndexAndUUID>>>()
    val liveData:LiveData<List<SerializableCommandStateAndContext<IndexAndUUID>>>
        get() = mutableLiveData

    init {
        mutableLiveData.value = commandRepository.list.toList()
    }

    fun onRefresh(){
        mutableLiveData.value = commandRepository.list.toList()
    }
}
class CommandListFragment : Fragment(), KoinComponent, LifecycleCommandOwner {

    //private val catsFragment : Fragment by lifecycleScope.inject { parametersOf(this) }
    private lateinit var commandListViewModel: FocusCommandViewModel
    private lateinit var adapter: CommandAdapter
    private val futureLogger =  MutableStateFlow<DefaultIRootCommandLogger?>(null)

    override val commandRoot: AnyCommandRoot = buildLifecycleCommandRoot(futureLogger)
    override val lifecycleCommandContext: LifecycleCommandContext = LifecycleCommandContext(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return invokeCommandWithLifecycle<CatsFragmentBinding> {
            val executionContext = this
            commandListViewModel  = executionContext.invokeCommand<CatsFragmentBinding, FocusCommandViewModel>{ lifecycleScope.get<FocusCommandViewModel>() }
            futureLogger.value = executionContext.invokeCommand<CatsFragmentBinding, DefaultIRootCommandLogger> { lifecycleScope.get<DefaultIRootCommandLogger>() }
            executionContext.invokeCommand<CatsFragmentBinding, Unit> {
                val fragment : Fragment by lifecycleScope.inject { parametersOf(this@CommandListFragment) }
                val innerExecutionContext = this
                adapter = invokeCommand {
                        this@CommandListFragment.lifecycleScope.get<CommandAdapter>()
                    }
                Unit
            }
            val binding = invokeCommand<CatsFragmentBinding, CatsFragmentBinding>(name = "createView") {
                CatsFragmentBinding.inflate(inflater).apply {
                    recyclerView.apply {
                        adapter = this@CommandListFragment.adapter
                        layoutManager = LinearLayoutManager(context)
                    }
                    swipeRefreshLayout.setOnRefreshListener { /*commandListViewModel.onRefresh()*/ }
                }
            }
            invokeCommand<CatsFragmentBinding, Unit>(name = "subscribe"){
                commandListViewModel.liveData.observe(this@CommandListFragment.viewLifecycleOwner, Observer {
                    Log.d("CATS","display items on card: $it")
                    binding.apply {
                        recyclerView.visibility = View.VISIBLE
                        retryButton.visibility = View.GONE
                        catsLoadingTextView.visibility = View.GONE
                        progressCircular.hide()
                        adapter.list.clear()
                        swipeRefreshLayout.isRefreshing = it.isComputing
                        adapter.list.addAll(it.list)
                        adapter.notifyDataSetChanged()
                        recyclerView.visibility = View.VISIBLE
                        retryButton.visibility = View.GONE
                    }
                })
            }
            binding
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
//        commandListViewModel.onRefresh()
    }


}