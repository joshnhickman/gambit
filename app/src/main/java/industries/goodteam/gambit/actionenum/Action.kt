package industries.goodteam.gambit.actionenum

import industries.goodteam.gambit.action.Target
import industries.goodteam.gambit.actor.Actor
import industries.goodteam.gambit.effect.Effect

class Action(val name: String, val target: Target = Target.OPPONENT, var cooldown: Int, start: Int = -1, vararg val effects: List<Effect>) {

    var left = start

    fun ready(): Boolean = left < 0

    fun perform(actor: Actor) {
        if (ready()) {
            left = cooldown
            effects.forEach { it.apply(target()) }
        }
    }

    // TODO replace with events
    fun endRound() {
        if (!ready()) left--
    }

    // TODO replace with events
    fun endCombat() {
        left = -1
    }

}