package com.alaeri.command

import com.alaeri.cats.app.DefaultIRootCommandLogger
import com.alaeri.command.android.CommandNomenclature
import com.alaeri.command.core.IInvokationContext
import com.alaeri.command.core.invoke
import com.alaeri.command.core.suspendInvokeAndFold
import com.alaeri.command.entity.Catalog
import com.alaeri.command.history.id.DefaultIdStore
import com.alaeri.command.history.serialize
import com.alaeri.command.history.spread
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Created by Emmanuel Requier on 03/05/2020.
 */
@ExperimentalCoroutinesApi
class OperationHistoryTest {

    private val testCoroutineScope = TestCoroutineScope()
    lateinit var logger: IInvokationContext<Int, Int>.(CommandState<Int>) -> Unit
    val list = mutableListOf<CommandState<*>>()

    val iOperationContext = buildCommandRoot(this, "flatten", CommandNomenclature.Test, object : DefaultIRootCommandLogger{
        override fun log(context: IInvokationContext<*, *>, state: CommandState<*>) {
           list.add(state)
        }

    })
    val owner = object : ICommandRootOwner{
        override val commandRoot: AnyCommandRoot
            get() = iOperationContext

    }

    @Before
    fun prepare(){
        logger = { t ->
            list.add(t)
            println(t)
        }
        DefaultIdStore.create()
    }

    @After
    fun clean(){
        testCoroutineScope.cleanupTestCoroutines()
        list.clear()
        DefaultIdStore.reset()
    }

    @Test
    fun testFlatten() = testCoroutineScope.runBlockingTest{
        val catalog = Catalog()
        val count = owner.invokeSuspendingRootCommand<Int>("test", CommandNomenclature.Test){
            invoke{
                catalog.count()
            }
            1
        }
        assertEquals(1, count)
        assertEquals(4, list.size)
        val flatList = list.flatMap { spread(iOperationContext, it, 0, iOperationContext) }
        assertTrue(flatList.none { it.state is CommandState.SubCommand<*,*> })
        assertEquals(4, flatList.size)
        flatList.forEach { println(it) }
        val firstElement = flatList[0]
        val secondElement = flatList[1]
        val thirdElement = flatList[2]
        assertEquals(this, firstElement.operationContext.invoker.owner)
        assertEquals(this, firstElement.operationContext.command.owner)
        assertEquals(catalog, secondElement.operationContext.command.owner)
        assertEquals(this, secondElement.operationContext.invoker.owner)
    }
    object shouldBeEmittedLast
    @ExperimentalCoroutinesApi
    @Test
    fun testSerialize() = testCoroutineScope.runBlockingTest{
        val catalog = Catalog()
        val count = owner.invokeSuspendingRootCommand<Int>("test", CommandNomenclature.Test){

            coroutineScope {
                suspendInvokeAndFold{
                    catalog.downloadAll()
                }
                println("csA: $coroutineContext")
                Unit
            }

            1

        }
        println("csG: ${this}")
        assertEquals(1, count)
        assertEquals(5, list.size)
        delay(300)
        val flatList = list.flatMap { spread(iOperationContext, it, 0, iOperationContext) }
        flatList.map { serialize(it.parentContext, it.operationContext, it.state, it.depth) { DefaultIdStore.instance.keyOf(it) } }.forEach { println(it) }
        delay(300)
        testCoroutineScope.advanceUntilIdle()
    }

//    @Test
//    fun testFocus() = testCoroutineScope.runBlockingTest{
//        val catalog = Catalog()
//        var command: ICommand<*>? = null
//        val count = owner.invokeRootCommand<Int>("test", CommandNomenclature.Test){
//            invoke{
//                catalog.count().apply { command = this }
//            }
//            1
//        }
//        val flatList = list.flatMap { spread(iOperationContext, it, 0, iOperationContext) }
//        val serializedList = flatList.map { serialize(it.parentContext, it.operationContext,it.state, it.depth) { DefaultIdStore.instance.keyOf(it) } }.onEach { println(it) }
//        val focusedCatalog = serializedList.flatMap { it.withFocus(catalog) }.forEach { println(it) }
//        val focusedCommand = serializedList.flatMap { it.withFocus(command!!) }.forEach { println(it) }
//        val focusedCoroutine = serializedList.flatMap { it.withFocus(this) }.forEach { println(it) }
//
//    }

}