package event

import org.json.JSONArray
import org.json.JSONObject
import width
import java.lang.Math.abs

fun distance(a: Pair<Int, Int>, b: Pair<Int, Int>) = abs(a.first - b.first) + abs(a.second - b.second)

fun parseActiveBonus(source: JSONObject) = ActiveBonus(source.getString("type"), source.getInt("ticks"))

fun parseActiveBonuses(source: JSONArray) = (0 until source.length()).map { parseActiveBonus(source.getJSONObject(it)) }

fun parseBonus(source: JSONObject)
        = Bonus(source.getString("type"),
            parseCoordinate(source.getJSONArray("position")),
            source.getInt("active_ticks"))

fun parseBonuses(source: JSONArray) = (0 until source.length()).map { parseBonus(source.getJSONObject(it)) }

fun parseGameCoordinate(source: JSONArray) = Pair(source[0] as Int, source[1] as Int)

fun parseCoordinate(source: JSONArray): Pair<Int, Int> {
  val res = parseGameCoordinate(source)
  return Pair((res.first - width / 2) / width, (res.second - (width / 2)) / width)
}

fun nextPos(position: Pair<Int, Int>, direction: String?) = when(direction) {
  "right" -> Pair(((position.first - width / 2 + width) / width) * width + width / 2, position.second)
  "left" -> Pair(position.first - width / 2 / width * width + width / 2, position.second)
  "up" -> Pair(position.first, ((position.second - width / 2 + width) / width) * width + width / 2)
  else -> Pair(position.first, position.second - width / 2 / width * width + width / 2)
}

fun parseCoordinateList(source: JSONArray) = (0 until source.length()).map { parseCoordinate(source.getJSONArray(it)) }

fun parsePlayer(source: JSONObject) = Player(
  parseCoordinateList(source.getJSONArray("territory")),
  parseCoordinate(source.getJSONArray("position")),
  if (!source.isNull("direction")) source.getString("direction") else null,
  parseCoordinateList(source.getJSONArray("lines")),
  source.getInt("score"),
  parseActiveBonuses(source.getJSONArray("bonuses")),
  distance(parseGameCoordinate(source.getJSONArray("position")),
    nextPos(
      parseGameCoordinate(source.getJSONArray("position")),
      if (!source.isNull("direction")) source.getString("direction") else null
    ))
)

fun parsePlayers(source: JSONObject) =
  (1..6).map { it.toString() }.filter { source.has(it) }.map { parsePlayer(source.getJSONObject(it)) }

fun parseParams(source: JSONObject) = Params(
  parsePlayer(source.getJSONObject("players").getJSONObject("i")),
  parsePlayers(source.getJSONObject("players")),
  source.getInt("tick_num"),
  parseBonuses(source.getJSONArray("bonuses"))
)

fun parseTick(source: JSONObject) = Tick(
  source.getString("type"),
  parseParams(source.getJSONObject("params"))
)