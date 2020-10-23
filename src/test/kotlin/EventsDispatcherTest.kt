

// class EventsDispatcherTest {
//
//     private sealed class TestEvent {
//         data class First(val first: String = "1") : TestEvent()
//         data class Second(val second: String = "2") : TestEvent()
//     }
//
//     private class FirstHandler : EventHandlerCreator<TestEvent> {
//         private var executing = false
//         override fun create(dispatch: GenericDispatch<TestEvent>): IEventHandler<TestEvent> {
//             return object : IEventHandler<TestEvent> {
//                 override val name = FirstHandler::class.simpleName.toString()
//
//                 override suspend fun handleAsync(event: TestEvent): Boolean {
//                     if (executing)
//                         throw IllegalStateException()
//                     executing = true
//                     return try {
//                         if (event is TestEvent.First) {
//                             dispatch(TestEvent.Second())
//                             true
//                         } else
//                             false
//                     } finally {
//                         executing = false
//                     }
//                 }
//             }
//         }
//     }
//
//     private class SecondHandler : EventHandlerCreator<TestEvent> {
//         override fun create(dispatch: GenericDispatch<TestEvent>): IEventHandler<TestEvent> {
//             return object : IEventHandler<TestEvent> {
//                 override val name = SecondHandler::class.simpleName.toString()
//                 override suspend fun handleAsync(event: TestEvent) = event is TestEvent.Second
//             }
//         }
//     }
//
//     private class TestIEventDispatcher :
//         EventDispatcher<TestEvent>(FirstHandler(), SecondHandler()) {
//         val dispatchCalled = mutableListOf<TestEvent>()
//         override suspend fun dispatch(event: TestEvent) {
//             dispatchCalled.add(event)
//             super.dispatch(event)
//         }
//     }
//
//     @Test
//     fun `Test multiple dispatches in one run`() {
//         val dispatcher = TestIEventDispatcher()
//         runBlocking {
//             dispatcher.dispatch(TestEvent.First())
//         }
//
//         assertThat(dispatcher.dispatchCalled).containsExactly(TestEvent.First(), TestEvent.Second())
//     }
// }
