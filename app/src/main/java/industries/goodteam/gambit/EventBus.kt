package industries.goodteam.gambit

import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

object EventBus {

    private val log = AnkoLogger(this::class.java)

    private var listeners: MutableMap<Class<out Event>, MutableList<(Event) -> Unit>> = mutableMapOf()

    var events: MutableList<Event> = mutableListOf()

    fun register(vararg eventTypes: Class<out Event> = arrayOf(Event::class.java), test: (Event) -> Unit) {
        eventTypes.forEach { eventType ->
            if (!listeners.containsKey(eventType)) listeners[eventType] = mutableListOf(test)
            else listeners[eventType]?.add(test)
        }
    }

    fun post(event: Event) {
        log.info("${event::class.java.simpleName}: ${event.message}")
        events.add(event)
        listeners[event::class.java]?.forEach { it(event) }
        listeners[Event::class.java]?.forEach { it(event) }
    }

}