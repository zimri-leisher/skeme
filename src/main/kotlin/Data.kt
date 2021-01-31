/**
 * Just a temporary helper class for linking and delinking groups of [Cell]s
 */
object Data {
    fun link(list: List<Node>): Node {
        if (list.isEmpty()) {
            return Nil
        }
        return Cell(list.first(), link(list.subList(1, list.size)))
    }

    fun delink(cell: Node): List<Node> {
        if (cell is Nil) {
            return emptyList()
        }
        cell as Cell
        return listOf(cell.left) + delink(cell.right)
    }
}