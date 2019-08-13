package event

data class Bonus(
  val type: String,
  val position: Pair<Int, Int>,
  val ticks: Int
)
