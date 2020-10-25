import assertk.assertThat
import assertk.assertions.containsExactly
import com.github.smaugfm.events.EventDispatcher
import com.github.smaugfm.events.HandlersBuilder
import com.github.smaugfm.events.IEvent
import com.github.smaugfm.events.IEventDispatcher
import com.github.smaugfm.events.IEventsHandlerRegistrar
import com.github.smaugfm.events.UnitEvent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class EventsDispatcherTest {

    private sealed class TestEvent {
        data class First(val first: String) : TestEvent(), UnitEvent
        data class Second(val second: String) : TestEvent(), UnitEvent
    }

    private class FirstHandler : IEventsHandlerRegistrar {
        @Volatile
        private var executing = 0
        override fun registerEvents(builder: HandlersBuilder) {
            builder.apply {
                registerUnit { dispatch: IEventDispatcher, _: TestEvent.First ->
                    check(executing <= 2)
                    executing++
                    try {
                        dispatch(TestEvent.Second(executing.toString()))
                    } finally {
                        executing--
                    }
                }
            }
        }
    }

    private class SecondHandler : IEventsHandlerRegistrar {
        @Volatile
        private var dispatched = 0
        override fun registerEvents(builder: HandlersBuilder) {
            builder.apply {
                registerUnit { dispatcher: IEventDispatcher, e: TestEvent.Second ->
                    dispatched++
                    if (dispatched < 2)
                        dispatcher(TestEvent.First(dispatched.toString()))
                }
            }
        }
    }

    private class TestEventDispatcher :
        EventDispatcher(FirstHandler(), SecondHandler()) {
        val dispatchCalled = mutableListOf<TestEvent>()
        override suspend fun <R, E : IEvent<R>> invoke(event: E): R {
            if (event is TestEvent)
                dispatchCalled.add(event)
            return super.invoke(event)
        }
    }

    @Test
    fun `Test multiple dispatches in one run`() {
        val dispatcher = TestEventDispatcher()
        runBlocking {
            dispatcher(TestEvent.First("0"))
        }

        assertThat(dispatcher.dispatchCalled).containsExactly(
            TestEvent.First("0"),
            TestEvent.Second("1"),
            TestEvent.First("1"),
            TestEvent.Second("2")
        )
    }
}
