package industries.goodteam.gambit.entity

import industries.goodteam.gambit.StatType
import industries.goodteam.gambit.action.*
import industries.goodteam.gambit.effect.AppliedEffect
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.info

open class Entity(
    var name: String,
    var luck: Int = 1,
    var vitality: Int = 30,
    var strength: Int = 5,
    var accuracy: Int = 1,
    var armor: Int = 5,
    var reflexes: Int = 1,
    var concentration: Int = 1,
    vararg var actions: Action
) {

    val log = AnkoLogger(this.javaClass)

    var health = vitality
    var shield = 0

    var gold = 0

    var intent = actions[0]
    var attackMultiplier = 1

    private var stunLeft = -1

    private var effects = mutableListOf<AppliedEffect>()

    open fun update() {
        shield = 0

        for (action in actions) {
            action.update()
        }

        log.info("beforeFilter: ${effects.joinToString(",") { it.value.toString() }}")
        effects.removeAll{
            it.update()
            if (it.done()) modifyStat(it.targetStat, -it.value)
            it.done()
        }
        log.info("afterFilter: ${effects.joinToString(",") { it.left.toString() }}")

        if (stunned()) stunLeft--
    }

    open fun endCombat() {
        for (action in actions) {
            action.refresh()
        }
    }

    open fun intend(action: Action? = null) {
        intent = when {
            stunned() -> Wait()
            action != null -> action
            else -> actions.filter { it.ready() }.random()
        }
    }

    open fun act(action: Action = intent) {
        intent.perform()
    }

    open fun defend(): Int {
        val defenceValue = actionValue(Defend()).random()
        shield += defenceValue
        return defenceValue
    }

    open fun damage(amount: Int): Int {
        var actualAmount = amount - shield
        if (actualAmount < 0) actualAmount = 0
        if (health - actualAmount >= 0) health -= actualAmount else health = 0
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
        intend(Wait())
        return duration
    }

    open fun affect(effect: AppliedEffect) {
        effects.add(effect)
        modifyStat(effect.targetStat, effect.value)

    }

    private fun modifyStat(stat: StatType, value: Int) {
        when(stat) {
            StatType.LUCK -> luck += value
            StatType.VITALITY -> vitality += value
            StatType.HEALTH -> health += value
            StatType.STRENGTH -> strength += value
            StatType.ACCURACY -> accuracy += value
            StatType.ARMOR -> armor += value
            StatType.REFLEXES -> reflexes += value
            StatType.CONCENTRATION -> concentration += value
        }
    }

    open fun alive(): Boolean = health > 0

    open fun stunned(): Boolean = stunLeft > -1

    fun actionValue(action: Action = intent): IntRange {
        return when(action) {
            is Attack -> (if (accuracy < strength) accuracy*attackMultiplier else strength*attackMultiplier)..strength*attackMultiplier
            is Defend -> (if (reflexes < armor) reflexes else armor)..armor
            is Stun -> concentration..concentration
            is Steal -> accuracy*10..reflexes*10
            is Wait -> stunLeft..stunLeft
            is Modify -> action.effect.value..action.effect.value
            else -> 0..0
        }
    }

}