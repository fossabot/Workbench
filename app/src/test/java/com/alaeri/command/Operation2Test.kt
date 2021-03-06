package com.alaeri.command

import com.alaeri.cats.app.DefaultIRootCommandLogger
import com.alaeri.command.android.CommandNomenclature
import com.alaeri.command.core.*
import com.alaeri.command.entity.Catalog
import com.alaeri.command.core.suspend.suspendingCommand
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Created by Emmanuel Requier on 02/05/2020.
 */
class Operation2Test {

    private lateinit var owner: ICommandRootOwner
    private val testCoroutineScope = TestCoroutineScope()
    lateinit var logger: AnyCommandRoot

    val list = mutableListOf<CommandState<*>>()

    @Before
    fun prepare(){
        logger = buildCommandRoot(this, null, CommandNomenclature.Test, object: DefaultIRootCommandLogger{
            override fun log(context: IInvokationContext<*, *>, state: CommandState<*>) {
                list.add(state)
                println(state)
            }
        })
        owner = object : ICommandRootOwner{
            override val commandRoot: AnyCommandRoot
                get() = logger

        }
//        logger = object : IInvokationContext<Int, Int> {
//            override val command = object : ICommand<Int> {
//                override val owner: Any = this
//                override val nomenclature: CommandNomenclature = CommandNomenclature.Test
//                override val name: String? = "test"
//            }
//            override val invoker: Invoker<Int> = object : Invoker<Int> {
//                override val owner: Any = this
//            }
//
//            override fun emit(opState: CommandState<Int>) {
//
//            }
//        }
    }

    @After
    fun clean(){
        testCoroutineScope.cleanupTestCoroutines()
        list.clear()
    }

    @Test
    fun testBasicSuspendOperationWorks() = runBlocking {
        val value = owner.invokeSuspendingRootCommand<Int>("test", CommandNomenclature.Test){
            1
        }
        assertEquals(1, value)
    }

    @Test
    fun testBasicSyncOperationWorks() = runBlocking {
        val value2 = owner.invokeRootCommand<Int>("test", CommandNomenclature.Test){
            2
        }
        assertEquals(2, value2)
    }



    @Test
    fun testMoreComplexSuspendOperation(){
        runBlocking {
            val value = owner.invokeSuspendingRootCommand<Int>("test", CommandNomenclature.Test){
                val count = suspendInvokeAndFold {
                    suspendingCommand<Int> {
                        val a: Int =  1
                        a
                    }
                }
                emit(CommandState.Update(Progress(1, 2)))
                emit(Step("calling OtherCount"))
                val otherCount = invoke { command<Int> { 0 } }
                count + otherCount
            }
            assertEquals(1, value)
        }
        val value2 = owner.invokeRootCommand<Int>("test", CommandNomenclature.Test){
            val count = invoke { command<Int>{  2  } }
            testCoroutineScope.launch {
                suspendInvokeAndFold {
                    suspendingCommand<Int> {
                        delay(100)
                        0
                    }
                }
            }
            count
        }
        assertEquals(2, value2)
        testCoroutineScope.advanceUntilIdle()

    }

    @Test
    fun testWorksWithClass() = testCoroutineScope.runBlockingTest {
        val catalog = Catalog()
        val count = owner.invokeSuspendingRootCommand<Int>("test", CommandNomenclature.Test){
            invoke {
                catalog.count()
            }
        }
        assertEquals(1, count)
    }

    @Test
    fun testContainsAllEvents() = testCoroutineScope.runBlockingTest {
        val catalog = Catalog()
        val count = owner.invokeSuspendingRootCommand<Int>("test", CommandNomenclature.Test){
            invoke {
                catalog.count()
            }
            1
        }
        assertEquals(1, count)
        assertEquals(4, list.size)
        val thirdEvent = list[2]
        assertTrue(thirdEvent is CommandState.SubCommand<*,*>)
        val operationStateSubOp = thirdEvent as CommandState.SubCommand<*, *>
        val (subOpC, _) = operationStateSubOp.subCommandAndState
        assertEquals(catalog, subOpC.command.owner)
        assertEquals(owner, subOpC.invoker.owner)
    }


}