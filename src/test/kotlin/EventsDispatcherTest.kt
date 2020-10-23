import assertk.assertThat
import assertk.assertions.containsExactly
import com.github.smaugfm.events.EventsDispatcherI
import com.github.smaugfm.events.GenericDispatch
import com.github.smaugfm.events.IGenericEventHandler
import com.github.smaugfm.events.EventHandlerCreator
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class EventsDispatcherTest {

    private sealed class TestEvent {
        data class First(val first: String = "1") : TestEvent()
        data class Second(val second: String = "2") : TestEvent()
    }

    private class FirstHandler : EventHandlerCreator<TestEvent> {
        private var executing = false
        override fun create(dispatch: GenericDispatch<TestEvent>): IGenericEventHandler<TestEvent> {
            return object : IGenericEventHandler<TestEvent> {
                override val name = FirstHandler::class.simpleName.toString()

                override suspend fun handle(event: TestEvent): Boolean {
                    if (executing)
                        throw IllegalStateException()
                    executing = true
                    return try {
                        if (event is TestEvent.First) {
                            dispatch(TestEvent.Second())
                            true
                        } else
                            false
                    } finally {
                        executing = false
                    }
                }
            }
        }
    }

    private class SecondHandler : EventHandlerCreator<TestEvent> {
        override fun create(dispatch: GenericDispatch<TestEvent>): IGenericEventHandler<TestEvent> {
            return object : IGenericEventHandler<TestEvent> {
                override val name = SecondHandler::class.simpleName.toString()
                override suspend fun handle(event: TestEvent) = event is TestEvent.Second
            }
        }
    }

    private class TestIEventDispatcher :
        EventsDispatcherI<TestEvent>(FirstHandler(), SecondHandler()) {
        val dispatchCalled = mutableListOf<TestEvent>()
        override suspend fun dispatch(event: TestEvent) {
            dispatchCalled.add(event)
            super.dispatch(event)
        }
    }

    @Test
    fun `Test multiple dispatches in one run`() {
        val dispatcher = TestIEventDispatcher()
        runBlocking {
            dispatcher.dispatch(TestEvent.First())
        }

        assertThat(dispatcher.dispatchCalled).containsExactly(TestEvent.First(), TestEvent.Second())
    }
}
