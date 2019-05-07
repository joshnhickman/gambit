package industries.goodteam.gambit.event

import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object EventBus {

    private val log = AnkoLogger(this::class.java)

    private var listeners: MutableMap<Class<out Event>, MutableList<Pair<() -> Boolean, (Event) -> Unit>>> =
        mutableMapOf()
    private var uiListeners: MutableMap<Class<out Event>, MutableList<(Event) -> Unit>> = mutableMapOf()

    private var events: MutableList<Event> = mutableListOf()

    /**
     * registers a listener for a given event type
     *
     * @param eventTypes the event types (java classes) to listen for
     * @param retain a lambda that returns true if this listener should be retained
     * @param function the function to call when a given event is fired
     *        the function should return true if the listener is still needed, false otherwise
     */
    fun register(
        vararg eventTypes: Class<out Event> = arrayOf(Event::class.java),
        retain: () -> Boolean = { true },
        function: (Event) -> Unit
    ) {
        eventTypes.forEach { eventType ->
            if (!listeners.containsKey(eventType)) listeners[eventType] = mutableListOf(retain to function)
            else listeners[eventType]?.add(retain to function)
        }
    }

    fun registerUI(
        vararg eventTypes: Class<out Event> = arrayOf(Event::class.java),
        function: (Event) -> Unit
    ) {
        eventTypes.forEach { eventType ->
            if (!uiListeners.containsKey(eventType)) uiListeners[eventType] = mutableListOf(function)
            else uiListeners[eventType]?.add(function)
        }
    }

    fun post(event: Event) {
        log.info("${event::class.java.simpleName}: ${event.message}")
        events.add(event)

        listeners[event::class.java]?.retainAll {
            var retain = it.first()
            if (retain) it.second(event)
            retain
        }
        listeners[Event::class.java]?.retainAll {
            var retain = it.first()
            if (retain) it.second(event)
            retain
        }

        uiListeners[event::class.java]?.forEach { it(event) }
        uiListeners[Event::class.java]?.forEach { it(event) }
    }

    fun clear() {
        events.clear()
        listeners.clear()
    }

    fun eventsFrom(level: Int, combat: Int, round: Int): List<Event> {
        val l = if (level < 0) 0 else level
        val c = if (combat < 0) 0 else combat
        return if (round < 0) listOf() else events.filter { it.level == l && it.combat == c && it.round == round }
    }

}