package industries.goodteam.gambit.action

import industries.goodteam.gambit.*
import industries.goodteam.gambit.actor.Actor
import industries.goodteam.gambit.effect.Effect

sealed class Action(val name: String, val target: Target = Target.OPPONENT, var cooldown: Int, start: Int = -1) :
    Comparable<Action> {

    abstract val priority: Int

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
            EventBus.post(ActionPerformed(this, actor, target()))
            act()
            left = cooldown
        }
    }

    open fun act() {}

    abstract fun describe(): String

    fun target(): Actor = if (target == Target.SELF) actor else Gambit.opponent(actor)

    open fun range(from: Stat, to: Stat = from, multiplier: Int = 1): IntRange =
        actor.stat(from) * multiplier..actor.stat(to) * multiplier

    override fun compareTo(other: Action): Int = when {
        this.priority < other.priority -> 1
        this.priority > other.priority -> -1
        this.actor == other.actor -> 0
        this.actor == Gambit.player -> 1
        else -> -1
    }

    override fun toString(): String = name
}

// innate
class Nothing : Action(name = "nothing", target = Target.SELF, cooldown = -1) {
    override val priority = 99

    override fun act() {
        EventBus.post(ActorNothing(this, actor))
    }

    override fun describe(): String = "do nothing"

}

class Stun(name: String = "stun", cooldown: Int = 3, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {
    override val priority = 1

    val stat = Stat.CONCENTRATION

    override fun act() {
        val duration = range(stat).random()
        target().stun(duration)
        EventBus.post(ActorStunned(this, target(), duration))
    }

    override fun describe(): String = "stun for $stat turns"
}

class Modify(name: String = "modify", val effect: Effect, cooldown: Int = 3, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {
    override val priority = 2

    override fun act() {
        val appliedEffect = effect.apply()
        target().affect(appliedEffect)
        EventBus.post(ActorModified(this, actor, appliedEffect.value))
    }

    override fun describe(): String = "modify ${effect.targetStat} by ${effect.value}"
}

class Defend(name: String = "defend", cooldown: Int = 1, start: Int = -1) :
    Action(name = name, target = Target.SELF, cooldown = cooldown, start = start) {

    override val priority = 3

    val from = Stat.REFLEXES
    val to = Stat.ARMOR

    override fun act() {
        val amount = range(from, to).random()
        target().shield += amount
        EventBus.post(ActorDefended(this, actor, amount))
    }

    override fun describe(): String = "defend ${range(from, to)} damage"
}

class Attack(name: String = "attack", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {

    override val priority = 4

    val from = Stat.ACCURACY
    val to = Stat.STRENGTH

    override fun act() {
        val amount = range(Stat.ACCURACY, Stat.STRENGTH).random()
        EventBus.post(ActorAttacked(this, actor, amount))
        target().damage(amount)
    }

    override fun describe(): String = "attack for ${range(from, to)} damage"

}

class Steal(name: String = "steal", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {
    override val priority = 5

    val from = Stat.REFLEXES
    val to = Stat.ACCURACY

    override fun act() {
        val stolen = range(from, to, 10).random()
        actor.gold += stolen
        EventBus.post(ActorStole(this, actor, stolen))
    }

    override fun describe(): String = "steal ${range(from, to, 10)} gold"

}