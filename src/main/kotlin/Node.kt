import java.lang.StringBuilder

sealed class Node {
    abstract fun evaluate(closure: Closure, cdr: Node): Node
}

class Cell(val left: Node, val right: Node) : Node() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        println("evaluating $left with $right")
        val function = if(left !is ProcedureNode) left.evaluate(closure, Nil) else left
        if(function !is ProcedureNode) {
            throw Exception("$function is not a procedure")
        }
        return function.evaluate(closure, right)
    }

    fun representationString(): String {
        val builder = StringBuilder()
        builder.append(left.toString())
        if(right !is Nil) {
            if(right is Cell) {
                builder.append(" ${right.representationString()}")
            } else {
                builder.append(" . $right")
            }
        }
        return builder.toString()
    }

    override fun toString(): String {
        return "(${representationString()})"
    }
}

abstract class ProcedureNode : Node()

object CondFunction : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        val conditions = Data.delink(cdr)
        for(condition in conditions) {
            val (bool, result) = Data.delink(condition)
            val boolValue = bool.evaluate(closure, Nil)
            if(boolValue is ElseNode || (boolValue is BooleanLiteral && boolValue.value)) {
                return result.evaluate(closure, Nil)
            }
        }
        return Nil
    }
}

object LetFunction : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        val (variableList, function) = Data.delink(cdr)
        val innerClosure = Closure(parent = closure)
        variableList as Cell
        val variables = Data.delink(variableList)
        for(variable in variables) {
            val (name, value) = Data.delink(variable)
            innerClosure[(name as TextNode).text] = value.evaluate(closure, Nil)
        }
        return function.evaluate(innerClosure, Nil)
    }
}

object ElseNode : Node() {
    override fun evaluate(closure: Closure, cdr: Node) = this
}

class QuoteFunction(val expression: Node) : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node) = expression
    override fun toString(): String {
        return "'$expression"
    }
}

object DefineFunction : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        cdr as Cell
        val name = (cdr.left as TextNode).text
        val value = (cdr.right as Cell).left
        closure.setGlobal(name, value.evaluate(closure, Nil))
        return value
    }
}

object LambdaFunction : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        cdr as Cell
        return LambdaObjectNode(cdr.left, (cdr.right as Cell).left, closure.collapse())
    }
}

open class LambdaObjectNode(val arguments: Node, val function: Node, val capturedClosure: Closure) : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        capturedClosure.parent = closure
        if(arguments !is Cell) {
            if(arguments is Nil) {
                return function.evaluate(Closure(parent = capturedClosure.copy()), Nil)
            }
            val innerClosure = Closure(parent = capturedClosure.copy())
            innerClosure[(arguments as TextNode).text] = cdr
            return function.evaluate(innerClosure, Nil)
        } else {
            println("args of function $function:")
            val innerClosure = Closure(parent = capturedClosure.copy())
            val argumentNames = Data.delink(arguments).map { (it as TextNode).text }
            val arguments = Data.delink(cdr)
            for(i in argumentNames.indices) {
                val evaluatedArgument = arguments[i].evaluate(closure, Nil)
                println("   setting ${argumentNames[i]}=$evaluatedArgument")
                innerClosure[argumentNames[i]] = evaluatedArgument
            }
            return function.evaluate(innerClosure, Nil)
        }
    }
}

object IfFunction : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        cdr as Cell
        if((cdr.left.evaluate(closure, Nil) as BooleanLiteral).value) {
            return (cdr.right as Cell).left.evaluate(closure, Nil)
        }
        return ((cdr.right as Cell).right as Cell).left.evaluate(closure, Nil)
    }
}

object OrFunction : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        val conditions = Data.delink(cdr)
        for(condition in conditions) {
            val evaluated = condition.evaluate(closure, Nil)
            if(evaluated != BooleanLiteral(false)) {
                return evaluated
            }
        }
        return BooleanLiteral(false)
    }
}

object AndFunction : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        val conditions = Data.delink(cdr)
        for(condition in conditions) {
            val evaluated = condition.evaluate(closure, Nil)
            if(evaluated == BooleanLiteral(false)) {
                return evaluated
            }
        }
        return BooleanLiteral(true)
    }
}

class TextNode(val text: String) : Node() {

    override fun evaluate(closure: Closure, cdr: Node): Node {
        return closure[text] ?: throw Exception("Unable to resolve variable $text")
    }
    override fun toString(): String {
        return text
    }
}

class NativeFunction(val function: (Closure, List<Node>) -> Node) : ProcedureNode() {
    override fun evaluate(closure: Closure, cdr: Node): Node {
        val arguments = Data.delink(cdr).map { it.evaluate(closure, Nil) }
        return function(closure, arguments)
    }
}

sealed class Literal<T>(val value: T) : Node() {
    override fun evaluate(closure: Closure, cdr: Node) = this
    override fun toString(): String {
        return "$value"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Literal<*>

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }
}

object Nil : Literal<Any?>(null) {
    override fun toString(): String {
        return "()"
    }
}

class BooleanLiteral(value: Boolean) : Literal<Boolean>(value)
class IntLiteral(value: Int) : Literal<Int>(value)
class StringLiteral(value: String) : Literal<String>(value)
