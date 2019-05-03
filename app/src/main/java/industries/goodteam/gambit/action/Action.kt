package industries.goodteam.gambit.action

import industries.goodteam.gambit.Gambit
import industries.goodteam.gambit.Stat
import industries.goodteam.gambit.actor.Actor
import industries.goodteam.gambit.effect.Effect

sealed class Action(val name: String, val target: Target = Target.OPPONENT, var cooldown: Int, start: Int = -1) :
    Comparable<Action> {

    abstract val priority: Int

    abstract val baseStat: Stat
    abstract val rangeStat: Stat

    lateinit var actor: Actor
    var left = start

    fun endRound() {
        if (!ready()) left--
    }

    fun endCombat() {
        left = -1
    }

    fun ready(): Boolean = left < 0

    fun perform() {
        if (actor.alive() && ready()) {
            act()
            left = cooldown
        }
    }

    abstract fun act()

    fun target(): Actor = if (target == Target.SELF) actor else Gambit.opponent(actor)

    open fun range(): IntRange = actor.stat(rangeStat)..actor.stat(baseStat)

    override fun compareTo(other: Action): Int = when {
        this.priority < other.priority -> 1
        this.priority > other.priority -> -1
        this.actor == other.actor -> 0
        this.actor == Gambit.player -> 1
        else -> -1
    }
}

// innate
class Nothing : Action(name = "nothing", target = Target.SELF, cooldown = -1) {
    override val priority = 99

    override val baseStat = Stat.LUCK
    override val rangeStat = Stat.LUCK

    override fun act() {
        Gambit.addEvent("${actor.name} does nothing")
    }

    override fun range(): IntRange = actor.stunLeft..actor.stunLeft

}

class Stun(name: String = "stun", cooldown: Int = 3, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {
    override val priority = 1

    override val baseStat = Stat.CONCENTRATION
    override val rangeStat = Stat.CONCENTRATION

    override fun act() {
        val duration = range().random()
        target().stun(duration)
        Gambit.addEvent("${actor.name} stunned ${target().name} for $duration turns")
    }
}

class Modify(name: String = "modify", val effect: Effect, cooldown: Int = 3, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {
    override val priority = 2

    override val baseStat = Stat.LUCK
    override val rangeStat = Stat.LUCK

    override fun act() {
        val appliedEffect = effect.apply()
        target().affect(appliedEffect)
        Gambit.addEvent("${actor.name} modifies ${target().name}'s ${effect.targetStat} by ${effect.value}")
    }

    override fun range(): IntRange = effect.value..effect.value
}

class Defend(name: String = "defend", cooldown: Int = 1, start: Int = -1) :
    Action(name = name, target = Target.SELF, cooldown = cooldown, start = start) {

    override val priority = 3

    override val baseStat = Stat.ARMOR
    override val rangeStat = Stat.REFLEXES

    override fun act() {
        val amount = range().random()
        target().shield += amount
        Gambit.addEvent("${actor.name} prepares to defend $amount damage")
    }
}

class Attack(name: String = "attack", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {

    override val priority = 4

    override val baseStat = Stat.STRENGTH
    override val rangeStat = Stat.ACCURACY

    override fun act() {
        val amount = range().random()
        Gambit.addEvent("${actor.name} attacks for $amount damage")
        val damage = target().damage(amount)
        Gambit.addEvent("${target().name} takes $damage damage")
    }

}

class Steal(name: String = "steal", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {
    override val priority = 5

    override val baseStat = Stat.ACCURACY
    override val rangeStat = Stat.REFLEXES

    override fun act() {
        val stolen = range().random()
        actor.gold += stolen
        Gambit.addEvent("${actor.name} steals $stolen gold from ${target().name}")
    }

    override fun range(): IntRange = actor.stat(rangeStat) * 10..actor.stat(baseStat) * 10
}