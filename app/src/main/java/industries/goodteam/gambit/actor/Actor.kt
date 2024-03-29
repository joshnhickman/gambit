package industries.goodteam.gambit.actor

import industries.goodteam.gambit.Relic
import industries.goodteam.gambit.Stat
import industries.goodteam.gambit.Stat.*
import industries.goodteam.gambit.Strategy
import industries.goodteam.gambit.action.Action
import industries.goodteam.gambit.action.Attack
import industries.goodteam.gambit.action.Nothing
import industries.goodteam.gambit.effect.AppliedEffect
import industries.goodteam.gambit.event.*
import industries.goodteam.gambit.property.Property
import industries.goodteam.gambit.property.Retaliate

// TODO: make actor final
open class Actor(
    val name: String,
    val strategy: Strategy = Strategy.RANDOM,
    var luck: Int = 1,
    var vitality: Int = 30,
    var strength: Int = 5,
    var accuracy: Int = 1,
    var armor: Int = 5,
    var reflexes: Int = 1,
    var concentration: Int = 1,
    var actions: List<Action>,
    var relics: MutableList<Relic> = mutableListOf()
) {

    val nothing = Nothing()

    // pointer to keep track of current action for sequential strategy
    var actionPointer = -1

    // TODO: figure out a better way to give the action a handle to the actor
    // if an action is created outside of an actor, it will break due to the lateinit property
    init {
        actions.forEach { it.actor = this }
        nothing.actor = this
    }

    var health = vitality
    var shield = 0

    var gold = 0

    var intent = actions[0]
    var attackMultiplier = 1

    var stunLeft = -1

    var effects = mutableListOf<AppliedEffect>()

    var acted = false

    fun activate() {
        EventBus.register(StartRound::class.java, retain = { alive() }) {
            shield = 0
            acted = false
            effects.removeAll { effect ->
                effect.update()
                if (effect.done()) modifyStat(effect.targetStat, -effect.value)
                effect.done()
            }
            if (stunned()) stunLeft--
        }
        actions.forEach { action -> action.activate() }

        relics.forEach { registerProperties(it.properties) }
    }

    open fun act(action: Action = intent) {
        if (!acted) {
            action.perform()
            acted = true
        }
    }

    open fun intend(action: Action? = null) {
        intent = when {
            stunned() -> nothing
            action != null -> action
            else -> {
                when (strategy) {
                    Strategy.RANDOM -> actions.filter { it.ready() }.random()
                    Strategy.SEQUENTIAL -> {
                        actionPointer++
                        if (actionPointer >= actions.size) actionPointer = 0
                        actions[actionPointer]
                    }
                }
            }
        }
    }

    open fun damage(amount: Int): Int {
        var actualAmount = amount - shield
        if (actualAmount < 0) actualAmount = 0
        if (health - actualAmount >= 0) health -= actualAmount else health = 0
        EventBus.post(ActorDamaged(this, actualAmount))
        return actualAmount
    }

    open fun heal(amount: Int = vitality): Int {
        var actualAmount = amount
        if (health + amount > vitality) actualAmount = vitality - health
        health += actualAmount
        return actualAmount
    }

    open fun stun(duration: Int): Int {
        stunLeft = duration
        actions.forEach { it.left = stunLeft }
        intend(nothing)
        return duration
    }

    open fun affect(effect: AppliedEffect) {
        effects.add(effect)
        modifyStat(effect.targetStat, effect.value)

    }

    private fun modifyStat(stat: Stat, value: Int) {
        when (stat) {
            LUCK -> luck += value
            VITALITY -> vitality += value
            HEALTH -> health += value
            STRENGTH -> strength += value
            ACCURACY -> accuracy += value
            ARMOR -> armor += value
            REFLEXES -> reflexes += value
            CONCENTRATION -> concentration += value
        }
    }

    fun stat(stat: Stat): Int = when (stat) {
        LUCK -> luck
        VITALITY -> vitality
        HEALTH -> health
        STRENGTH -> strength
        ACCURACY -> accuracy
        ARMOR -> armor
        REFLEXES -> reflexes
        CONCENTRATION -> concentration
    }

    fun newRelic(relic: Relic) {
        relics.add(relic)
        registerProperties(relic.properties)
    }

    open fun alive(): Boolean = health > 0

    open fun stunned(): Boolean = stunLeft > -1

    private fun registerProperties(properties: List<Property>) {
        for (property in properties) {
            // TODO: move this logic into properties?
            when (property) {
                is Retaliate -> {
                    EventBus.register(ActionPerformed::class.java, retain = { alive() }) {
                        if (it is ActionPerformed && it.action is Attack && it.target == this) {
                            EventBus.post(PropertyTriggered(this, property))
                            it.actor.damage(property.value)
                        }
                    }
                }
            }
        }
    }

    override fun toString(): String = name

}