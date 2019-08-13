import  event.*
import org.json.JSONObject
import java.lang.Math.*
import java.util.*
import kotlin.collections.ArrayList

val scanner = Scanner(System.`in`)
val config = JSONObject(scanner.nextLine()).getJSONObject("params")!!
val N = config.getInt("x_cells_count")
val M = config.getInt("y_cells_count")
val width = config.getInt("width")
val speed = config.getInt("speed")//TODO
val fastSpeed = 2 * config.getInt("speed")//TODO
val commands = arrayOf("left", "up", "right", "down")
const val inf = 1000000
var lastPosition = Pair(0, 0)

fun log(message: String) = 5
//fun log(message: String) = System.err.println(message)

fun nextTick(): Tick {
  val str = scanner.nextLine()
  //log(str)
  return parseTick(JSONObject(str))
}

val bfsStep = listOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))

fun <T> bfs(defPosition: Pair<Int, Int>,
            win: Array<Array<Boolean>>,
            used: Array<Array<Boolean>>,
            defValue: T,
            initValue: T,
            step: (T, Pair<Int, Int>) -> T): T {
  if (win[defPosition.first][defPosition.second]) {
    return initValue
  }
  val act = ArrayDeque<Pair<Pair<Int, Int>, T>>()
  act.addLast(Pair(defPosition, initValue))
  used[defPosition.first][defPosition.second] = false
  used[lastPosition.first][lastPosition.second] = true
  while (!act.isEmpty()) {
    val (position, cur) = act.poll()
    if (position.first < 0 || position.first >= used.size
      || position.second < 0 || position.second >= used[0].size
      || used[position.first][position.second]) {
      continue
    }
    if (win[position.first][position.second]) {
      return step(cur, position)
    }
    used[position.first][position.second] = true
    bfsStep.shuffled().forEach {
      act.addLast(Pair(Pair(position.first + it.first, position.second + it.second), step(cur, position)))
    }
  }
  return defValue
}

fun getMatrix(arr: List<Pair<Int, Int>>,
              n: Int = N,
              m: Int = M): Array<Array<Boolean>> {
  val res = Array(n) { Array(m) { false } }
  arr.forEach {
    res[it.first][it.second] = true
  }
  return res
}

fun getTime(dist: Int, time: Int = 0): Int
        = min((dist + fastSpeed - 1) / fastSpeed, time) + (max(0, dist - fastSpeed * time) + speed - 1) / speed

fun minDist(position: Pair<Int, Int>,
            win: Array<Array<Boolean>>,
            used: Array<Array<Boolean>>): Int
        = bfs(position, win, used, inf, 0) { a, _ -> a + 1 } * width

fun getTurboTime(player: Player): Int {
  if (player.bonuses.any { it.type == "n" }) {
    return player.bonuses.first { it.type == "n" }.ticks
  }
  return 0
}

fun getMinTime(player: Player,
               position: Pair<Int, Int> = player.position,
               win: Array<Array<Boolean>> = getMatrix(player.territory),
               used: Array<Array<Boolean>> = getMatrix(player.lines),
               constDist: Int = 0)
        = getTime(minDist(position, win, used) + constDist, getTurboTime(player))

fun path(position: Pair<Int, Int>,
         win: Array<Array<Boolean>>,
         used: Array<Array<Boolean>>)
  = bfs(position,
  win,
  used,
  ArrayList(),
  ArrayList<Pair<Int, Int>>())
  { a, b ->
    val res = ArrayList<Pair<Int, Int>>()
    for (i in a) {
      res.add(i)
    }
    res.add(b)
    res
  }

fun path(player: Player)
        = path(player.position, getMatrix(player.territory), getMatrix(player.lines))

fun makeMove(position: Pair<Int, Int>, newPosition: Pair<Int, Int>) = when {
  newPosition.first > position.first -> "right"
  newPosition.first < position.first -> "left"
  newPosition.second > position.second -> "up"
  else -> "down"
}

fun makeMove(position: Pair<Int, Int>)
        = println("{ \"command\": \"${makeMove(position, mainOrder.poll())}\" }")

val mainOrder = ArrayDeque<Pair<Int, Int>>()

fun makeMovement(player: Player) {
  var x = player.position.first
  var y = player.position.second
  val step = when (q % 2) {
    0 -> when (Random().nextInt() % 2) {
      0 -> Pair(-1, 0)
      else -> Pair(1, 0)
    }
    else -> when(Random().nextInt() % 2) {
      0 -> Pair(0, -1)
      else -> Pair(0, 1)
    }
  }
  while (true) {
    x += step.first
    y += step.second
    if (x < 0 || x >= N || y < 0 || y >= M || Random().nextInt() % 10 == 0 || player.lines.contains(Pair(x, y))) {
      break
    }
    mainOrder.add(Pair(x, y))
  }
}

fun homePlan(player: Player) {
  mainOrder.clear()
  for (position in path(player)) {
    mainOrder.add(position)
  }
  mainOrder.poll()
  if (mainOrder.isEmpty()) {
    makePlan(player)
  }
}

fun makePlan(player: Player) {
  mainOrder.clear()
  var cnt = 0
  while (cnt < 20 && (mainOrder.isEmpty() || getMinTime(player, mainOrder.peek()) >= inf)) {
    mainOrder.clear()
    makeMovement(player)
    cnt++
  }
  if (mainOrder.isEmpty() || getMinTime(player, mainOrder.peek()) >= inf) {
    homePlan(player)
  }
  q++
}

var q = 0
var prevPath: List<Pair<Int, Int>> = ArrayList()

fun main(args: Array<String>) {
  log(config.toString())
  while (true) {
    val tick: Tick
    try {
      tick = nextTick()
      log("========================")
    } catch (e: Exception) {
      continue
    }
    if (mainOrder.isEmpty()) {
      //здесь может находиться ваша стратегия
      makePlan(tick.params.me)
    }
    else {
      val newPosition = mainOrder.peek()
      val trajectory = getMatrix(tick.params.me.lines)
      for (j in path(newPosition, getMatrix(tick.params.me.territory), getMatrix(tick.params.me.lines)))
        trajectory[j.first][j.second] = true
      val enemyTime = if (tick.params.players.isNotEmpty()) min(tick.params.players.map { getMinTime(it, win = trajectory) }.min()!!, inf) else inf
      val newHomeTime = if (tick.params.me.territory.contains(newPosition)) 0 else getMinTime(tick.params.me, newPosition)

      log("" + getMinTime(tick.params.me, newPosition, constDist = width).toString() + "/" + tick.params.players.map { getMinTime(it, win = trajectory) }.min()!!.toString())

      if (enemyTime <= newHomeTime) {
        log("Enemy alert!!")
        log(tick.params.players.minBy { getMinTime(it, win = trajectory) }!!.position.toString())

        if (!tick.params.me.territory.contains(tick.params.me.position)) {
          mainOrder.clear()
          for (position in path(tick.params.me)) {
            mainOrder.add(position)
          }
          mainOrder.poll()
        }
        else {
          log("lol i'm already home")
          makePlan(tick.params.me)
        }
      }
    }

    val w = ArrayList<Pair<Int, Int>>()
    while (mainOrder.isNotEmpty()) {
      w.add(mainOrder.poll())
    }
    for (i in w) {
      mainOrder.add(i)
    }
    log("me")
    log("base: " + tick.params.me.territory.sortedBy { it.first * M + it.second }.toList().toString())

    if (tick.params.players.isNotEmpty()) {
      val newPosition = mainOrder.peek()
      val trajectory = getMatrix(tick.params.me.lines)
      for (j in path(newPosition, getMatrix(tick.params.me.territory), getMatrix(tick.params.me.lines)))
        trajectory[j.first][j.second] = true
      log(
        "" + getMinTime(
          tick.params.me,
          newPosition,
          constDist = width
        ).toString() + "/" + tick.params.players.map { getMinTime(it, win = trajectory) }.min()!!.toString()
      )
    }

    log("prev path: $prevPath")
    log("lines: " + tick.params.me.lines.sortedBy { it.first * M + it.second }.toList().toString())
    log("home: " + path(tick.params.me).toList().toString())
    log("plan: " + w.toList().toString())
    log("position: " + tick.params.me.position.toString())
    makeMove(tick.params.me.position)
    lastPosition = tick.params.me.position
  }
}