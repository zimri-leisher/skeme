/**
 * A variable-value mapping, supporting parent closures
 */
class Closure(
    val memory: MutableMap<String, Node> = mutableMapOf(),
    var parent: Closure? = null
) {

    /**
     * @return the [Node] corresponding to this name. This mapping may exist in this closure or its [parent]. `null` if the mapping
     * doesn't exist
     */
    operator fun get(name: String): Node? = memory[name] ?: parent?.get(name)

    /**
     * Sets the value of [name] to be [node]. This is local, doesn't affect parent closures
     */
    operator fun set(name: String, node: Node) {
        memory[name] = node
    }

    /**
     * Sets the value of [name] to be [node]. This will set it in the highest parent this closure can access
     */
    fun setGlobal(name: String, node: Node) {
        if (parent != null) {
            parent!!.setGlobal(name, node)
        } else {
            memory[name] = node
        }
    }

    /**
     * @return a copy of this closure. The [memory] is a new map instance, but the [parent] references the same parent
     */
    fun copy() = Closure(memory.map { it.key to it.value }.toMap().toMutableMap(), parent)

    private fun join(other: Closure): Closure {
        val memory = mutableMapOf<String, Node>()
        other.memory.forEach { key, value -> memory.put(key, value) }
        this.memory.forEach { key, value -> memory.put(key, value) }
        return Closure(memory)
    }

    /**
     * @return a closure whose memory is the result of flattening this closures memory with all of its parents closures memories
     */
    fun collapse(): Closure {
        var currentClosure = copy()
        var currentParent = parent
        while(currentParent != null) {
            currentClosure = currentClosure.join(currentParent)
            currentParent = currentParent.parent
        }
        return currentClosure
    }

    override fun toString(): String {
        return "${memory.entries.joinToString(separator = "\n")}\n" + parent?.toString()
    }
}