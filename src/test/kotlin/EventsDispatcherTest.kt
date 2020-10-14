import assertk.assertThat
import assertk.assertions.containsExactly
import com.github.smaugfm.events.EventsDispatcher
import com.github.smaugfm.events.GenericDispatch
import com.github.smaugfm.events.GenericEventHandler
import com.github.smaugfm.events.IEventHandlerCreator
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class EventsDispatcherTest {

    private sealed class TestEvent {
        data class First(val first: String = "1") : TestEvent()
        data class Second(val second: String = "2") : TestEvent()
    }

    private class FirstHandler : IEventHandlerCreator<TestEvent> {
        private var executing = false
        override fun create(dispatch: GenericDispatch<TestEvent>): GenericEventHandler<TestEvent> {
            return { e ->
                if (executing)
                    throw IllegalStateException()
                executing = true
                try {
                    if (e is TestEvent.First) {
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

    private class SecondHandler : IEventHandlerCreator<TestEvent> {
        override fun create(dispatch: GenericDispatch<TestEvent>): GenericEventHandler<TestEvent> {
            return { e ->
                e is TestEvent.Second
            }
        }
    }

    private class TestEventDispatcher :
        EventsDispatcher<TestEvent>(FirstHandler(), SecondHandler()) {
        val dispatchCalled = mutableListOf<TestEvent>()
        override suspend fun dispatch(event: TestEvent) {
            dispatchCalled.add(event)
            super.dispatch(event)
        }
    }

    @Test
    fun `Test multiple dispatches in one run`() {
        val dispatcher = TestEventDispatcher()
        runBlocking {
            dispatcher.dispatch(TestEvent.First())
        }

        assertThat(dispatcher.dispatchCalled).containsExactly(TestEvent.First(), TestEvent.Second())
    }
}
