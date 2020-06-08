package com.alaeri.cats.app.ui.viewpager

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.alaeri.cats.app.R
import com.alaeri.cats.app.databinding.ViewpagerFragmentBinding
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.viewpager_fragment.*
import org.koin.android.scope.lifecycleScope
import org.koin.android.viewmodel.scope.viewModel
import org.koin.core.parameter.parametersOf

enum class PageId{
    Login,
    Cats,
    CommandsList,
    CommandsWebview
}
data class Page(val id: PageId)

class ViewPagerFragment : Fragment(){

    private var binding: ViewpagerFragmentBinding? = null
    private val fragmentsAdapter by lazy { FragmentsAdapter(this) }
    private val viewPagerModel: ViewPagerViewModel by lifecycleScope.viewModel(this)
    private lateinit var tabLayoutMediator : TabLayoutMediator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewPagerFragmentInjected : ViewPagerFragment by lifecycleScope.inject { parametersOf(this) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return ViewpagerFragmentBinding.inflate(inflater, container, false).apply {
            binding = this
            pager.adapter = fragmentsAdapter
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPagerModel.pages.observe(viewLifecycleOwner, Observer {
            fragmentsAdapter.updatePages(it)
            tabLayoutMediator = TabLayoutMediator(tabs, pager) { tab, position ->
                val resId = when(it[position].id){
                    PageId.Cats -> R.string.cats_fragment_title
                    PageId.Login -> R.string.login_fragment_title
                    PageId.CommandsList -> R.string.commands_list_fragment_title
                    PageId.CommandsWebview -> R.string.graph_fragment_title
                }
                tab.text = getString(resId)
            }
            tabLayoutMediator.attach()
        })
    }
}

