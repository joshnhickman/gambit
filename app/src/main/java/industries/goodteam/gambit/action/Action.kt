package industries.goodteam.gambit.action

import industries.goodteam.gambit.effect.Effect

sealed class Action(val name: String, val target: Target = Target.OPPONENT, var cooldown: Int, start: Int = -1) {

    var left = start

    open fun update() {
        if (!ready()) left--
    }

    fun refresh() {
        left = -1
    }

    fun ready(): Boolean = left < 0

    open fun perform() {
        if (ready()) left = cooldown
    }

}

// innate
class Nothing : Action(name = "nothing", target = Target.SELF, cooldown = -1)


// standard
class Attack(name: String = "attack", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {

//    val stat = StatType.STRENGTH
//    val rangeState = StatType.ACCURACY

//    override fun perform(actor: Actor, target: Actor) {
//        super.perform(actor, target)
//        var damage = range(actor).random()
//        target.damage(damage)
//    }

//    private fun range(actor: Actor): IntRange = actor.stat(stat)..actor.stat(rangeState)
}

class Defend(name: String = "defend", cooldown: Int = 1, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start)

// control
class Stun(name: String = "stun", cooldown: Int = 3, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start)

// other
class Steal(name: String = "steal", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start)

class Modify(name: String = "modify", val effect: Effect, cooldown: Int = 3, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start)