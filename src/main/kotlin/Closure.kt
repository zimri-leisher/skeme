class Closure(
    val memory: MutableMap<String, Node> = mutableMapOf(),
    var parent: Closure? = null
) {

    fun exists(name: String): Boolean = memory.containsKey(name) || parent?.exists(name) == true
    fun isLocal(name: String) = memory.containsKey(name)
    operator fun get(name: String): Node? = memory[name] ?: parent?.get(name)
    operator fun set(name: String, node: Node) {
        memory[name] = node
    }

    fun setGlobal(name: String, node: Node) {
        if (parent != null) {
            parent!!.setGlobal(name, node)
        } else {
            memory[name] = node
        }
    }

    fun copy() = Closure(memory.map { it.key to it.value }.toMap().toMutableMap(), parent)

    fun join(other: Closure): Closure {
        val memory = mutableMapOf<String, Node>()
        other.memory.forEach { key, value -> memory.put(key, value) }
        this.memory.forEach { key, value -> memory.put(key, value) }
        return Closure(memory)
    }

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