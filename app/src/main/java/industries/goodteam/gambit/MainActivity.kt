package industries.goodteam.gambit

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import industries.goodteam.gambit.action.*
import industries.goodteam.gambit.entity.Entity
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    val attack = Attack(0)
    val defend = Defend(1)
    val stun = Stun(3)
    var steal = Steal(0)

    lateinit var player: Entity
    lateinit var enemy: Entity

    lateinit var enemies: List<Entity>

    lateinit var enemyNameText: TextView
    lateinit var enemyActionText: TextView

    lateinit var defendButton: Button
    lateinit var attackButton: Button
    lateinit var stunButton: Button
    lateinit var stealButton: Button
    lateinit var waitButton: Button

    lateinit var attackText: TextView
    lateinit var defendText: TextView
    lateinit var stunText: TextView
    lateinit var stealText: TextView

    lateinit var playerHealthBar: ProgressBar
    lateinit var enemyHealthBar: ProgressBar

    lateinit var playerHealthText: TextView
    lateinit var enemyHealthText: TextView

    lateinit var eventsText: TextView
    lateinit var goldText: TextView

    var events = mutableListOf<String>()
    var combat = -1
    var round = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // set fullscreen options
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        supportActionBar?.hide()

        setContentView(R.layout.combat)

        // get handles to ui
        defendButton = find<Button>(R.id.defendButton).apply { onClick { act(defend) } }
        defendText = find<TextView>(R.id.defendText)

        attackButton = find<Button>(R.id.attackButton).apply { onClick { act(attack) } }
        attackText = find<TextView>(R.id.attackText)

        stunButton = find<Button>(R.id.utilityButton).apply { onClick { act(stun) } }
        stunText = find<TextView>(R.id.stunText)

        stealButton = find<Button>(R.id.stealButton).apply { onClick { act(steal) } }
        stealText = find<TextView>(R.id.stealText)

        waitButton = find<Button>(R.id.waitButton).apply { onClick { act(Wait()) } }

        playerHealthBar = find<ProgressBar>(R.id.healthBar)
        enemyHealthBar = find<ProgressBar>(R.id.enemyHealthBar)

        playerHealthText = find<TextView>(R.id.playerHealthText)
        enemyHealthText = find<TextView>(R.id.enemyHealthText)

        enemyNameText = find<TextView>(R.id.enemyNameText)
        enemyActionText = find<TextView>(R.id.enemyActionText)

        eventsText = find<TextView>(R.id.eventsText)
        goldText = find<TextView>(R.id.goldText)

        // set up buttons
        find<Button>(R.id.restartButton).onClick {
            newGame()
        }

        find<Button>(R.id.editButton).onClick {
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
                                    setText("${value}")
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
                        editStats("eConcentration", enemy.concentration, ">1 will break") { enemy.concentration = it }
                    }
                }
                yesButton { toast("saved") }
            }.show()
        }

        newGame()
    }

    private fun newGame() {
        combat = 0
        events.clear()
        player = Entity(
            name = "player",
            luck = 1,
            vitality = 40,
            strength = 4,
            accuracy = 1,
            armor = 4,
            reflexes = 1,
            concentration = 2,
            actions = *arrayOf(attack, defend, stun, steal)
        )

        enemies = mutableListOf(
            Entity(
                name = "defender",
                luck = 1,
                vitality = 30,
                strength = 5,
                accuracy = 1,
                armor = 20,
                reflexes = 5,
                actions = *arrayOf(Attack(1, 1), Defend(0))),
            Entity(
                name = "stunner",
                luck = 1,
                vitality = 30,
                strength = 5,
                accuracy = 1,
                armor = 5,
                reflexes = 1,
                concentration = 0,
                actions = *arrayOf(Attack(0), Stun(4, 2))),
            Entity(
                name = "damager",
                luck = 1,
                vitality = 30,
                strength = 20,
                accuracy = 15,
                armor = 5,
                reflexes = 1,
                actions = *arrayOf(Attack(4, 4), Defend(0))),
            Entity(
                name = "generic",
                luck = 1,
                vitality = 30,
                strength = 5,
                accuracy = 1,
                armor = 5,
                reflexes = 1,
                actions = *arrayOf(Attack(0), Defend(1)))
        ).shuffled()

        newCombat()
    }

    private fun newCombat() {
        player.endCombat()

        combat++

        if (combat == enemies.size) {
            longToast("game over, restarting")
            newGame()
        } else {
            enemy = enemies[combat]
            events.add("encountered enemy ${enemy.name}")
            events.add("++ start combat ${combat} ++")

            round = 0
            newRound()
        }
    }

    private fun newRound() {
        round++
        events.add("-- start round ${round} --")

        enemy.intend()
        events.add("${enemy.name} intends to ${enemy.intent.name} ${enemy.actionValue()}")

        draw()
    }

    private fun act(action: Action) {
        player.intend(action)

        if (player.intent is Stun) {
            var duration = enemy.stun(player.concentration)
            events.add("you stunned ${enemy.name} for ${duration} turns")
        }
        if (enemy.intent is Stun) {
            var duration = player.stun(enemy.concentration )
            events.add("${enemy.name} stunned you for ${duration} turns")
        }

        if (player.intent is Defend) {
            var amt = player.defend()
            events.add("you prepare to defend ${amt} damage")
        }
        if (enemy.intent is Defend) events.add("${enemy.name} prepares to defend ${enemy.defend()} damage")

        if (player.intent is Attack) {
            var damage = player.actionValue().random()
            var actualDamage = enemy.hit(damage)
            events.add("you attack for ${damage} damage")
            events.add("${enemy.name} takes ${actualDamage} damage")
        }
        if (enemy.intent is Attack) {
            var damage = enemy.actionValue().random()
            var actualDamage = player.hit(damage)
            events.add("${enemy.name} attacks for ${damage} damage")
            events.add("you take ${actualDamage} damage")
        }

        if (player.intent is Steal) {
            var stolen = player.actionValue().random()
            player.gold += stolen
            events.add("you steal ${stolen} gold")
        }

        if (player.intent is Wait) events.add("you do nothing")
        if (enemy.intent is Wait) events.add("${enemy.name} does nothing")

        player.act()
        enemy.act()

        if (!enemy.alive()) {
            events.add("${enemy.name} dies")
            events.add("++ end combat ${combat} ++")
            alert("defeated ${enemy.name}") { yesButton {} }.show()
            newCombat()
        } else if (!player.alive()) {
            alert("${enemy.name} defeated you") { yesButton {} }.show()
            newGame()
        } else {
            player.update()
            enemy.update()
            newRound()
        }
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

        defendText.text = """*block ${player.actionValue(defend)} damage
            |*${defend.cooldown} turn cooldown
        """.trimMargin()
        attackText.text = """*deal ${player.actionValue(attack)} damage
            |*damage increases with consecutive attacks
            |*${attack.cooldown} turn cooldown
        """.trimMargin()
        stunText.text = """*stun for ${player.actionValue(stun)} turn(s)
            |*${stun.cooldown} turn cooldown
        """.trimMargin()
        stealText.text = """*steal ${player.actionValue(steal)} gold
            |*${steal.cooldown} turn cooldown
        """.trimMargin()

        playerHealthText.text = "${player.health} / ${player.vitality}"
        enemyHealthText.text = "${enemy.health} / ${enemy.vitality}"

        goldText.text = "GOLD: ${player.gold}"

        eventsText.text = events.takeLast(10).joinToString("\n")

        if (player.stunned()) {
            defendButton.visibility = View.GONE
            attackButton.visibility = View.GONE
            stunButton.visibility = View.GONE
            waitButton.visibility = View.VISIBLE
        } else {
            defendButton.visibility = if (defend.ready()) View.VISIBLE else View.GONE
            attackButton.visibility = if (attack.ready()) View.VISIBLE else View.GONE
            stunButton.visibility = if (stun.ready()) View.VISIBLE else View.GONE
            waitButton.visibility = View.GONE
        }
    }

    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable) {
                afterTextChanged.invoke(editable.toString())
            }
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
        })
    }

    fun String.getIntWithBlank(): Int {
        try {
            return this.toInt()
        } catch (e: NumberFormatException) {
            return 0
        }
    }
}
