package industries.goodteam.gambit.event

import industries.goodteam.gambit.Gambit.Companion.level
import industries.goodteam.gambit.action.*
import industries.goodteam.gambit.action.Nothing
import industries.goodteam.gambit.actor.Actor

sealed class Event(val message: String) {
    override fun toString(): String = message
}

class StartGame : Event("started new game")

data class StartLevel(val number: Int) : Event("started level $level")
data class FinishLevel(val number: Int) : Event("finished level $level")

data class EncounteredEnemy(val actor: Actor) : Event("encountered enemy ${actor.name}")

data class StartCombat(val number: Int) : Event("started combat $number")
data class FinishCombat(val number: Int) : Event("finished combated $number")

data class StartRound(val number: Int) : Event("started round $number")
data class FinishRound(val number: Int) : Event("finished round $number")

data class ActionIntended(val action: Action, val actor: Actor, val target: Actor) :
    Event("$actor intends to ${action.describe()}")
//    Event("${actor.name} intends to perform ${action.name} on ${target.name}")

data class ActionPerformed(val action: Action, val actor: Actor, val target: Actor) :
    Event("${actor.name} performs ${action.name} on ${target.name}")

// TODO: does it make sense to have these separate from ActionPerformed ?
data class ActorNothing(val action: Nothing, val actor: Actor) : Event("$actor does nothing")

data class ActorStunned(val action: Stun, val actor: Actor, val duration: Int) :
    Event("$actor stunned for $duration rounds")

data class ActorModified(val action: Modify, val actor: Actor, val change: Int) :
    Event("$actor ${action.effect.targetStat} modified by $change")

data class ActorDefended(val action: Defend, val actor: Actor, val amount: Int) :
    Event("$actor defended from $amount damage")

data class ActorAttacked(val action: Attack, val actor: Actor, val damage: Int) :
    Event("$actor attacks for $damage damage")

data class ActorStole(val action: Steal, val actor: Actor, val amount: Int) : Event("$actor stole $amount gold")

data class ActorDamaged(val target: Actor, val value: Int) :
    Event("${target.name} took $value damage")

data class ActorDied(val actor: Actor) : Event("${actor.name} died")