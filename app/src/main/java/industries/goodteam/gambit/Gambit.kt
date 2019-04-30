package industries.goodteam.gambit

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import industries.goodteam.gambit.action.*
import industries.goodteam.gambit.action.Nothing
import industries.goodteam.gambit.actor.Actor
import industries.goodteam.gambit.actor.Player
import industries.goodteam.gambit.effect.Effect
import kotlinx.android.synthetic.main.combat.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick

@SuppressLint("SetTextI18n")
class Gambit : AppCompatActivity() {

    companion object {

        private val log = AnkoLogger(this.javaClass)

        val attack = Attack(cooldown = 0)
        val defend = Defend(cooldown = 1)
        val stun = Stun(cooldown = 3)
        var steal = Steal(cooldown = 0)

        lateinit var player: Player
        lateinit var enemy: Actor

        lateinit var enemies: List<Actor>

        var events = mutableListOf<String>()
        var level = -1
        var combat = -1
        var round = -1

        var defeated = mutableListOf<Actor>()

        fun addEvent(message: String) {
            log.info("event: $message")
            events.add(message)
        }

    }

    private lateinit var defendCard: Card
    private lateinit var attackCard: Card
    private lateinit var stunCard: Card
    private lateinit var stealCard: Card

//    private lateinit var binding: CombatBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set fullscreen options
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()

        setContentView(R.layout.combat)

        detailsCard.visibility = View.GONE

        defendButton.onClick { act(defend) }
        defendCard = Card(this, defend, defendGuideline, defendButton)
        defendCardLayout.apply {
            setOnLongClickListener {
                detailsName.text = "DEFEND"
                detailsText.text = """
                    |*block ${player.actionValue(defend)} damage
                    |*${defend.cooldown} turn cooldown
                """.trimMargin()
                detailsCard.visibility = View.VISIBLE
                true
            }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP && detailsCard.visibility == View.VISIBLE) {
                    detailsCard.visibility = View.GONE
                    true
                } else false
            }
        }

        attackButton.onClick { act(attack) }
        attackCard = Card(this, attack, attackGuideline, attackButton)
        attackCardLayout.apply {
            setOnLongClickListener {
                detailsName.text = "ATTACK"
                detailsText.text = """
                    |*deal ${player.actionValue(attack)} damage
                    |*damage increases with consecutive attacks
                    |*${attack.cooldown} turn cooldown
                """.trimMargin()
                detailsCard.visibility = View.VISIBLE
                true
            }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP && detailsCard.visibility == View.VISIBLE) {
                    detailsCard.visibility = View.GONE
                    true
                } else false
            }
        }

        stunButton.onClick { act(stun) }
        stunCard = Card(this, stun, stunGuideline, stunButton)
        stunCardLayout.apply {
            setOnLongClickListener {
                detailsName.text = "STUN"
                detailsText.text = """
                    |*stun for ${player.actionValue(stun)} turn(s)
                    |*${stun.cooldown} turn cooldown
                """.trimMargin()
                detailsCard.visibility = View.VISIBLE
                true
            }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP && detailsCard.visibility == View.VISIBLE) {
                    detailsCard.visibility = View.GONE
                    true
                } else false
            }
        }

        stealButton.onClick { act(steal) }
        stealCard = Card(this, steal, stealGuideline, stealButton)
        stealCardLayout.apply {
            setOnLongClickListener {
                detailsName.text = "STEAL"
                detailsText.text = """
                    |*steal ${player.actionValue(steal)} gold
                    |*${steal.cooldown} turn cooldown
                """.trimMargin()
                detailsCard.visibility = View.VISIBLE
                true
            }
            setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP && detailsCard.visibility == View.VISIBLE) {
                    detailsCard.visibility = View.GONE
                    true
                } else false
            }
        }

        waitButton.onClick { act(Nothing()) }

        playerDamageText.alpha = 0f
        enemyDamageText.alpha = 0f

        // debug buttons
        restartButton.onClick {
            log.info("player requested new game")
            startGame()
        }
        editButton.onClick {
            alert("Edit Stats") {
                customView {
                    verticalLayout {
                        var editStats = { stat: String, value: Int, desc: String, function: (Int) -> Unit ->
                            linearLayout {
                                textView(stat)
                                editText {
                                    padding = dip(3)
                                    textSize = 14f
                                    inputType = InputType.TYPE_CLASS_NUMBER
                                    setText("$value")
                                    afterTextChanged {
                                        function(it.getIntWithBlank())
                                        draw()
                                    }
                                }
                                textView(desc)
                            }
                        }
                        padding = dip(10)

                        editStats("pVitality", player.vitality, "max health") { player.vitality = it }
                        editStats("pHealth", player.health, "current health") { player.health = it }
                        editStats("pLuck", player.luck, "not used") { player.luck = it }
                        editStats("pStrength", player.strength, "max damage") { player.strength = it }
                        editStats("pAccuracy", player.accuracy, "min damage (<str)") { player.accuracy = it }
                        editStats("pArmor", player.armor, "max shield") { player.armor = it }
                        editStats("pReflexes", player.reflexes, "min shield (<armor)") { player.reflexes = it }
                        editStats("pConcentration", player.concentration, "stun duration") { player.concentration = it }

                        editStats("pDefendCooldown", defend.cooldown, "") { defend.cooldown = it }
                        editStats("pAttackCooldown", attack.cooldown, "") { attack.cooldown = it }
                        editStats("pStunCooldown", stun.cooldown, "") { stun.cooldown = it }

                        editStats("eVitality", enemy.vitality, "") { enemy.vitality = it }
                        editStats("eHealth", enemy.health, "") { enemy.health = it }
                        editStats("eLuck", enemy.luck, "not used") { enemy.luck = it }
                        editStats("eStrength", enemy.strength, "") { enemy.strength = it }
                        editStats("eAccuracy", enemy.accuracy, "") { enemy.accuracy = it }
                        editStats("eArmor", enemy.armor, "") { enemy.armor = it }
                        editStats("eReflexes", enemy.reflexes, "") { enemy.reflexes = it }
                        editStats("eConcentration", enemy.concentration, "") { enemy.concentration = it }
                    }
                }
                yesButton { toast("saved") }
            }.show()
        }

        startGame()
    }

    private fun startGame() {
        addEvent("start game")
        events.clear()
        defeated.clear()

        player = Player(
            luck = 1,
            vitality = 40,
            strength = 4,
            accuracy = 1,
            armor = 4,
            reflexes = 1,
            concentration = 2,
            attack = attack,
            defend = defend,
            stun = stun,
            steal = steal
        )

        level = -1
        startLevel()
    }

    private fun startLevel() {
        level++
        addEvent("start level $level")

        if (level > 0) {
            val cost = level * 50
            alert("heal ${player.vitality / 2} health for $cost gold?") {
                if (player.gold >= cost) {
                    yesButton {
                        player.gold -= level * 50
                        player.heal(player.vitality / 2)
                        draw()
                    }
                }
                noButton {}
            }.show()
        }

        enemies = mutableListOf(
            Actor(
                name = "generic",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                concentration = 0 + level,
                actions = *arrayOf(Attack(cooldown = 0), Defend(cooldown = 1))
            ),
            Actor(
                name = "defender",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 20 + level,
                reflexes = 5 + level,
                actions = *arrayOf(Attack(cooldown = 1, start = 1), Defend(cooldown = 0))
            ),
            Actor(
                name = "stunner",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                concentration = 0 + level,
                actions = *arrayOf(Attack(cooldown = 0), Stun(cooldown = 4, start = 2))
            ),
            Actor(
                name = "damager",
                luck = 1 + level,
                vitality = 30 + 5 * level,
                strength = 20 + level,
                accuracy = 15 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                actions = *arrayOf(Attack(cooldown = 4, start = 4), Defend(cooldown = 0))
            ),
            Actor(
                name = "weakener",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                concentration = 0 + level,
                actions = *arrayOf(
                    Attack(cooldown = 0),
                    Defend(cooldown = 1),
                    Modify(effect = Effect(StatType.STRENGTH, -2 - level, 2), cooldown = 2)
                )
            )
        ).shuffled()

        combat = -1
        startCombat()
    }

    private fun startCombat() {
        combat++

        player.endCombat()

        animateCards()

        if (combat < enemies.size) {
            enemy = enemies[combat]
            addEvent("encountered enemy ${enemy.name}")
            addEvent("start combat $combat")

            round = -1
            newRound()
        } else startLevel()
    }

    private fun newRound() {
        round++
        addEvent("start round $round")

        enemy.intend()
        addEvent("${enemy.name} intends to ${enemy.intent.name} ${enemy.actionValue()}")

        draw()
    }

    private fun act(action: Action) {
        player.intend(action)

        if (player.intent is Stun) {
            val duration = enemy.stun(player.concentration)
            addEvent("you stunned ${enemy.name} for $duration turns")
        }
        if (enemy.intent is Stun) {
            val duration = player.stun(enemy.concentration)
            addEvent("${enemy.name} stunned you for $duration turns")
        }

        if (player.intent is Modify) { }
        if (enemy.intent is Modify) {
            val effect = (enemy.intent as Modify).effect.apply()
            player.affect(effect)
            addEvent("${enemy.name} modified your ${effect.targetStat} by ${effect.value}")
        }

        if (player.intent is Defend) {
            val amt = player.defend()
            addEvent("you prepare to defend $amt damage")
        }
        if (enemy.intent is Defend) addEvent("${enemy.name} prepares to defend ${enemy.defend()} damage")

        if (player.alive() && player.intent is Attack) {
            val damage = player.actionValue().random()
            val actualDamage = enemy.damage(damage)
            addEvent("you attack for $damage damage")
            addEvent("${enemy.name} takes $actualDamage damage")
            GlobalScope.launch {
                enemyDamageText.text = "$actualDamage"
                enemyDamageText.alpha = 1.0f
                while (enemyDamageText.alpha > 0) {
                    runOnUiThread { enemyDamageText.alpha -= 0.01f }
                    delay(16)
                }
            }
        }
        if (enemy.alive() && enemy.intent is Attack) {
            val damage = enemy.actionValue().random()
            val actualDamage = player.damage(damage)
            addEvent("${enemy.name} attacks for $damage damage")
            addEvent("you take $actualDamage damage")
            GlobalScope.launch {
                playerDamageText.text = "$actualDamage"
                playerDamageText.alpha = 1.0f
                while (playerDamageText.alpha > 0) {
                    runOnUiThread { playerDamageText.alpha -= 0.01f }
                    delay(16)
                }
            }
        }

        if (player.intent is Steal) {
            val stolen = player.actionValue().random()
            player.gold += stolen
            addEvent("you steal $stolen gold")
        }

        if (player.intent is Nothing) addEvent("you do nothing")
        if (enemy.intent is Nothing) addEvent("${enemy.name} does nothing")

        player.act()
        enemy.act()

        animateCards()

        enableButtons(false)
        draw()

        GlobalScope.launch {
            delay(250)
            runOnUiThread { endRound() }
        }
    }

    private fun endRound() {
        player.update()
        enemy.update()

        if (!enemy.alive()) {
            addEvent("${enemy.name} dies")
            addEvent("++ end combat $combat ++")
            defeated.add(enemy)
            alert("defeated ${enemy.name}") { yesButton {} }.show()
            startCombat()
        } else if (!player.alive()) {
            alert("""
                |${enemy.name} defeated you
                |ended on level $level, combat $combat, round $round
                |ended with ${player.gold} gold
                |defeated ${defeated.size} enemies:
                |${defeated.joinToString(", ") { it.name }}
            """.trimMargin()
            ) { yesButton {} }.show()
            startGame()
        } else newRound()

        animateCards()
    }

    private fun enableButtons(enabled: Boolean = true) {
        defendButton.isEnabled = enabled
        attackButton.isEnabled = enabled
        stunButton.isEnabled = enabled
        stealButton.isEnabled = enabled
    }

    private fun animateCards() {
        defendCard.animate()
        attackCard.animate()
        stunCard.animate()
        stealCard.animate()
    }

    private fun draw() {
        playerHealthBar.apply {
            max = player.vitality
            progress = player.health
        }
        enemyHealthBar.apply {
            max = enemy.vitality
            progress = enemy.health
        }

        enemyNameText.text = enemy.name
        enemyActionText.text = "${enemy.intent.name} ${enemy.actionValue()}"

        defendText.text = """
            |*block ${player.actionValue(defend)} damage
            |*${defend.cooldown} turn cooldown
        """.trimMargin()
        attackText.text = """
            |*deal ${player.actionValue(attack)} damage
            |*damage increases with consecutive attacks
            |*${attack.cooldown} turn cooldown
        """.trimMargin()
        stunText.text = """
            |*stun for ${player.actionValue(stun)} turn(s)
            |*${stun.cooldown} turn cooldown
        """.trimMargin()
        stealText.text = """
            |*steal ${player.actionValue(steal)} gold
            |*${steal.cooldown} turn cooldown
        """.trimMargin()

        playerHealthText.text = "${player.health} / ${player.vitality}"
        enemyHealthText.text = "${enemy.health} / ${enemy.vitality}"

        goldText.text = "GOLD: ${player.gold}"

        eventsText.text = events.takeLast(10).joinToString("\n")

        waitButton.visibility = if (player.stunned()) View.VISIBLE else View.GONE
    }

    // helper method to automatically set text as it's edited
    private fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                afterTextChanged.invoke(editable.toString())
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    // helper method to default non-int values to 0 when converting to a string
    private fun String.getIntWithBlank(): Int {
        return try {
            this.toInt()
        } catch (e: NumberFormatException) {
            0
        }
    }
}
