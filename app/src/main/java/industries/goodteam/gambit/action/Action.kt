package industries.goodteam.gambit.action

import industries.goodteam.gambit.effect.Effect

sealed class Action(val name: String, var cooldown: Int, start: Int = -1) {

    var left = start

    open fun update() {
        if (!ready()) left--
    }

    fun refresh() {
        left = -1
    }

    open fun perform() {
        if (!ready()) throw IllegalStateException("cooldown is not finished for $name")
        left = cooldown
    }

    fun ready(): Boolean = left < 0

}

// innate
class Nothing : Action("nothing", -1, -1)

// standard
class Attack(name: String = "attack", cooldown: Int = 0, start: Int = -1) : Action(name, cooldown, start)
class Defend(name: String = "defend", cooldown: Int = 1, start: Int = -1) : Action(name, cooldown, start)

// control
class Stun(name: String = "stun", cooldown: Int = 3, start: Int = -1) : Action(name, cooldown, start)

// other
class Steal(name: String = "steal", cooldown: Int = 0, start: Int = -1) : Action(name, cooldown, start)
class Modify(name: String = "modify", val effect: Effect, cooldown: Int = 3, start: Int = -1) :
    Action(name, cooldown, start)