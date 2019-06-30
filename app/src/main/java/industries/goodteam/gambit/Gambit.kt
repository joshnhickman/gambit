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
import industries.goodteam.gambit.event.*
import industries.goodteam.gambit.property.Retaliate
import kotlinx.android.synthetic.main.combat.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick

@SuppressLint("SetTextI18n") // ignore warnings for setting strings outside of resources
class Gambit : AppCompatActivity() {

    private val log = AnkoLogger(this::class.java)

    companion object {
        val attack = Attack(cooldown = 0)
        val defend = Defend(cooldown = 1)
        val stun = Stun(cooldown = 3)
        var steal = Steal(cooldown = 0)

        lateinit var player: Player
        lateinit var enemy: Actor

        lateinit var enemies: List<Actor>

        var level = -1
        var combat = -1
        var round = -1

        var defeated = mutableListOf<Actor>()

        var relics = mutableListOf<Relic>()

        fun opponent(actor: Actor): Actor = if (actor == player) enemy else player
    }

    private lateinit var defendCard: Card
    private lateinit var attackCard: Card
    private lateinit var stunCard: Card
    private lateinit var stealCard: Card

//    private lateinit var binding: CombatBinding

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // force fullscreen portrait
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
                    |*${player.defend.describe()}
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
                    |*${player.attack.describe()}
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
                    |*${player.stun.describe()}
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
                    |*${player.steal.describe()}
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

                        button("give spikes") {
                            onClick {
                                player.newRelic(Relic("spikes", "", listOf(Retaliate(3))))
                            }
                        }
                    }
                }
                yesButton { toast("saved") }
            }.show()
        }

        // animate pieces of the UI when certain events happen
        EventBus.registerUI(ActorDamaged::class.java) {
            if (it is ActorDamaged) {
                val damageText = if (it.target == player) playerDamageText else enemyDamageText
                damageText.text = "${it.value}"
                damageText.alpha = 1.0f
                GlobalScope.launch {
                    while (damageText.alpha > 0) {
                        runOnUiThread { damageText.alpha -= 0.01f }
                        delay(16)
                    }
                }
            }
        }
        EventBus.registerUI(FinishRound::class.java) {
            defendButton.isEnabled = false
            attackButton.isEnabled = false
            stunButton.isEnabled = false
            stealButton.isEnabled = false
            waitButton.visibility = View.GONE
        }
        EventBus.registerUI(StartRound::class.java) {
            defendButton.isEnabled = defend.ready()
            attackButton.isEnabled = attack.ready()
            stunButton.isEnabled = stun.ready()
            stealButton.isEnabled = steal.ready()
            waitButton.visibility = if (player.stunned()) View.VISIBLE else View.GONE
        }
        EventBus.registerUI {
            if (it !is StartGame && it !is StartLevel) draw()
        }

        startGame()
    }

    private fun startGame() {
        EventBus.clear()
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
        player.activate()

        EventBus.post(StartGame())

        level = -1
        startLevel()
    }

    private fun startLevel() {
        level++

        if (level > 0) {
            alert("purchase spikes for 25 gold?") {
                if (player.gold >= 25) {
                    yesButton {
                        player.gold -= 25
                        player.newRelic(Relic("spikes", "", listOf(Retaliate(3))))
                        draw()
                    }
                }
            }
            val cost = level * 50
            alert("heal ${player.vitality / 2} health for $cost gold?") {
                if (player.gold >= cost) {
                    yesButton {
                        player.gold -= cost
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
                actions = listOf(Attack(cooldown = 0), Defend(cooldown = 1))
            ),
            Actor(
                name = "defender",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 20 + level,
                reflexes = 5 + level,
                actions = listOf(Attack(cooldown = 1, start = 1), Defend(cooldown = 0))
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
                actions = listOf(Attack(cooldown = 0), Stun(cooldown = 4, start = 2))
            ),
            Actor(
                name = "damager",
                luck = 1 + level,
                vitality = 30 + 5 * level,
                strength = 20 + level,
                accuracy = 15 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                actions = listOf(Attack(cooldown = 4, start = 4), Defend(cooldown = 0))
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
                actions = listOf(
                    Attack(cooldown = 0),
                    Defend(cooldown = 1),
                    Modify(effect = Effect(Stat.STRENGTH, -2 - level, 2), cooldown = 2)
                )
            ),
            Actor(
                name = "retaliater",
                luck = 1,
                vitality = 30 + 5 * level,
                strength = 5 + level,
                accuracy = 3 + level,
                armor = 5 + level,
                reflexes = 2 + level,
                concentration = 0 + level,
                actions = listOf(Attack(cooldown = 0), Defend(cooldown = 1)),
                relics = mutableListOf(Relic("spikes", "", listOf(Retaliate(1 + 2 * level))))
            )
        ).shuffled()

        relics = mutableListOf(
            Relic("spikes", "ouch", listOf(Retaliate(4)))
        )

        EventBus.post(StartLevel(level))

        combat = -1
        startCombat()
    }

    private fun startCombat() {
        combat++

        if (combat < enemies.size) {
            enemy = enemies[combat]
            enemy.activate()
            EventBus.post(EncounteredEnemy(enemy))
            EventBus.post(StartCombat(combat))

            round = -1
            startRound()
        } else {
            EventBus.post(FinishLevel(level))
            startLevel()
        }
    }

    private fun startRound() {
        round++
        EventBus.post(StartRound(round))

        enemy.intend()
        EventBus.post(ActionIntended(enemy.intent, enemy, player))
    }

    private fun act(action: Action) {
        player.intend(action)

        val order = if (player.intent >= enemy.intent) player to enemy else enemy to player

        order.first.act()
        order.second.act()

        endRound()
    }

    private fun endRound() {
        EventBus.post(FinishRound(round))
        GlobalScope.launch {
            delay(250)
            runOnUiThread {
                if (!enemy.alive()) {
                    defeated.add(enemy)
                    EventBus.post(ActorDied(enemy))
                    EventBus.post(FinishCombat(combat))
                    alert("defeated ${enemy.name}") { yesButton {} }.show()
                    startCombat()
                } else if (!player.alive()) {
                    alert(
                        """
                        |${enemy.name} defeated you
                        |ended on level $level, combat $combat, round $round
                        |ended with ${player.gold} gold
                        |defeated ${defeated.size} enemies:
                        |${defeated.joinToString(", ") { it.name }}
                    """.trimMargin()
                    ) { yesButton {} }.show()
                    startGame()
                } else startRound()
            }
        }
    }

    private fun draw() {
        relicsText.text = player.relics.joinToString(",") { it.name }
        playerHealthBar.apply {
            max = player.vitality
            progress = player.health
        }
        enemyHealthBar.apply {
            max = enemy.vitality
            progress = enemy.health
        }

        enemyNameText.text = enemy.name
        var actionText = enemy.intent.describeShort()
        for (relic in enemy.relics) {
            for (property in relic.properties) {
                actionText += "\n${property.describeShort()}"
            }
        }
        enemyActionText.text = actionText

        defendText.text = """
            |*${player.defend.describe()}
            |*${defend.cooldown} turn cooldown
        """.trimMargin()
        attackText.text = """
            |*${player.attack.describe()}
            |*damage increases with consecutive attacks
            |*${attack.cooldown} turn cooldown
        """.trimMargin()
        stunText.text = """
            |*${player.stun.describe()}
            |*${stun.cooldown} turn cooldown
        """.trimMargin()
        stealText.text = """
            |*${player.steal.describe()}
            |*${steal.cooldown} turn cooldown
        """.trimMargin()

        playerHealthText.text = "${player.health} / ${player.vitality}"
        enemyHealthText.text = "${enemy.health} / ${enemy.vitality}"

        goldText.text = "GOLD: ${player.gold}"

        eventsText.text = "last round:\n" +
                EventBus.eventsFrom(level, combat, round - 1)
                    .joinToString("\n") { it.message }
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
