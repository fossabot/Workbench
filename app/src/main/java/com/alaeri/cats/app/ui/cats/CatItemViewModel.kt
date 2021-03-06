package com.alaeri.cats.app.ui.cats

import android.util.Log
import androidx.lifecycle.*
import com.alaeri.cats.app.DefaultIRootCommandLogger
import com.alaeri.cats.app.cats.Cat
import com.alaeri.command.*
import com.alaeri.command.android.CommandNomenclature
import com.alaeri.command.core.flow.syncInvokeFlow
import com.alaeri.ui.glide.FlowImageLoader
import com.alaeri.ui.glide.ImageLoadingState
import com.alaeri.ui.glide.Size
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CatItemViewModel(private val flowImageLoader: FlowImageLoader, private val defaultSerializer: DefaultIRootCommandLogger): ICommandRootOwner, ViewModel(){

    data class CatLoadingState(val imageLoadingState: ImageLoadingState)
    override val commandRoot: AnyCommandRoot = buildCommandRoot(this, null, CommandNomenclature.Root, defaultSerializer)

    private val mutableLiveDataCat =  MutableLiveData<Triple<Cat, Int, Int>?>(null)
    val catLoadingState : LiveData<CatLoadingState> = mutableLiveDataCat.switchMap { it ->
        invokeRootCommand<LiveData<CatLoadingState>>(
            name = "init CatLoadingState",
            commandNomenclature = CommandNomenclature.Application.Cats.LoadImage) {
            it?.let {
                emit(CommandState.Update(it.first))
                val cat = it.first
                val width = it.second
                val height = it.third
                val flow: Flow<ImageLoadingState> = syncInvokeFlow {
                    flowImageLoader.loadImage(cat.url, Size(width, height))
                }
                flow.map { CatLoadingState(it) }.asLiveData()
            } ?: MutableLiveData<CatLoadingState>()
        }
    }


    fun onItemSet(cat: Cat, width: Int, height: Int) {
        //resetSources()
        mutableLiveDataCat.value = Triple(cat, width, height)
    }

    fun onRetryClicked(width: Int, height: Int){
        mutableLiveDataCat.value = mutableLiveDataCat.value
    }

    public override fun onCleared() {
        super.onCleared()
        Log.d("CATS", "$this onCleared()")
    }




}