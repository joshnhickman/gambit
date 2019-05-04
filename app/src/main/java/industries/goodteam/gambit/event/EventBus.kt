package industries.goodteam.gambit.event

import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object EventBus {

    private val log = AnkoLogger(this::class.java)

    private var listeners: MutableMap<Class<out Event>, MutableList<(Event) -> Boolean>> = mutableMapOf()

    var events: MutableList<Event> = mutableListOf()

    /**
     * registers a listener for a given event type
     *
     * @param eventTypes the event types (java classes) to listen for
     * @param function the function to call when a given event is fired
     *        the function should return true if the listener is still needed, false otherwise
     */
    fun register(vararg eventTypes: Class<out Event> = arrayOf(Event::class.java), function: (Event) -> Boolean) {
        eventTypes.forEach { eventType ->
            if (!listeners.containsKey(eventType)) listeners[eventType] = mutableListOf(function)
            else listeners[eventType]?.add(function)
        }
    }

    /**
     * post an event to notify all listeners
     *
     * @param event the event to post
     */
    fun post(event: Event) {
        log.info("${event::class.java.simpleName}: ${event.message}")
        events.add(event)

        listeners[event::class.java]?.retainAll { it(event) }
        listeners[Event::class.java]?.retainAll { it(event) }
    }

    fun eventsFrom(level: Int, combat: Int, round: Int): List<Event> {
        val l = if (level < 0) 0 else level
        val c = if (combat < 0) 0 else combat
        return if (round < 0) listOf() else events.filter { it.level == l && it.combat == l && it.round == round }
    }

}