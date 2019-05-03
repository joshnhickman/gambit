package industries.goodteam.gambit

import industries.goodteam.gambit.action.Action
import industries.goodteam.gambit.actor.Actor

sealed class Event(val message: String) {
    companion object { }
    override fun toString(): String = message
}

class NewGame : Event("started new game")
data class NewCombat(val number: Int) : Event("started combat $number")
data class NewRound(val number: Int) : Event("started round $number")

data class EncounteredEnemy(val name: String) : Event("encountered enemy $name")

data class ActionIntended(val action: Action, val actor: Actor, val target: Actor) :
    Event("${actor.name} intends to perform ${action.name} on ${target.name}")

data class ActionPerformed(val action: Action, val actor: Actor, val target: Actor) :
    Event("${actor.name} performed ${action.name} on ${target.name}")

data class DamageTaken(val target: Actor, val value: Int) :
        Event("${target.name} took $value damage")