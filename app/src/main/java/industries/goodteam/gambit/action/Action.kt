package industries.goodteam.gambit.action

import industries.goodteam.gambit.Gambit
import industries.goodteam.gambit.Stat
import industries.goodteam.gambit.actor.Actor
import industries.goodteam.gambit.actor.Player
import industries.goodteam.gambit.effect.Effect
import industries.goodteam.gambit.event.*

sealed class Action(val name: String, val target: Target = Target.OPPONENT, var cooldown: Int, start: Int = -1) :
    Comparable<Action> {

    abstract val priority: Int

    lateinit var actor: Actor
    var left = start

    fun activate() {
        EventBus.register(StartRound::class.java, retain = { actor.alive() }) {
            if (!ready()) left--
        }
        EventBus.register(StartCombat::class.java, retain = { actor.alive() }) {
            left = -1
        }
    }

    fun ready(): Boolean = left < 0

    fun perform() {
        if (actor.alive() && ready()) {
            left = cooldown
            EventBus.post(ActionPerformed(this, actor, target()))
            act()
        }
    }

    abstract fun act()
    abstract fun describe(): String
    abstract fun describeShort(): String

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

class Nothing : Action(name = "nothing", target = Target.SELF, cooldown = 0) {
    override val priority = 99

    override fun act() {
        EventBus.post(ActorNothing(this, actor))
    }

    override fun describe(): String = "do nothing"
    override fun describeShort(): String = "nothing${if (actor.stunned()) " ${actor.stunLeft}" else ""}"

}

class Stun(name: String = "stun", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {
    override val priority = 1

    private val stat = Stat.CONCENTRATION

    override fun act() {
        val duration = range(stat).random()
        target().stun(duration)
        EventBus.post(
            ActorStunned(
                this,
                target(),
                duration
            )
        )
    }

    override fun describe(): String = "stun for ${actor.stat(stat)} turns"
    override fun describeShort(): String = "stun ${actor.stat(stat)}"
}

class Modify(name: String = "modify", val effect: Effect, cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {
    override val priority = 2

    override fun act() {
        val appliedEffect = effect.apply()
        target().affect(appliedEffect)
        EventBus.post(
            ActorModified(
                this,
                target(),
                appliedEffect.value
            )
        )
    }

    override fun describe(): String = "modify ${effect.targetStat} by ${effect.value}"
    override fun describeShort(): String = "modify ${effect.targetStat} ${effect.value}"
}

class Defend(name: String = "defend", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, target = Target.SELF, cooldown = cooldown, start = start) {

    override val priority = 3

    private val from = Stat.REFLEXES
    private val to = Stat.ARMOR

    override fun act() {
        val amount = range(from, to).random()
        target().shield += amount
        EventBus.post(ActorDefended(this, actor, amount))
    }

    override fun describe(): String = "defend ${range(from, to)} damage"
    override fun describeShort(): String = "defend ${range(from, to)}"
}

// TODO: contains temporary workarounds for player attack multiplier
class Attack(name: String = "attack", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {

    override val priority = 4

    private val from = Stat.ACCURACY
    private val to = Stat.STRENGTH

    override fun act() {
        var amount = range(Stat.ACCURACY, Stat.STRENGTH).random()
        if (actor is Player) amount = range(from, to, actor.attackMultiplier).random()
        EventBus.post(ActorAttacked(this, actor, amount))
        target().damage(amount)
    }

    override fun describe(): String {
        return if (actor is Player) "attack for ${range(from, to, actor.attackMultiplier)}"
        else "attack for ${range(from, to)} damage"
    }

    override fun describeShort(): String {
        return if (actor is Player) "attack ${range(from, to, actor.attackMultiplier)}"
        else "attack ${range(from, to)}"
    }

}

class Steal(name: String = "steal", cooldown: Int = 0, start: Int = -1) :
    Action(name = name, cooldown = cooldown, start = start) {
    override val priority = 5

    private val from = Stat.REFLEXES
    private val to = Stat.ACCURACY

    override fun act() {
        val stolen = range(from, to, 10).random()
        actor.gold += stolen
        EventBus.post(ActorStole(this, actor, stolen))
    }

    override fun describe(): String = "steal ${range(from, to, 10)} gold"
    override fun describeShort(): String = "steal ${range(from, to, 10)}"

}