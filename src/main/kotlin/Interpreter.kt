/**
 * A class which allows parsing and running of Scheme code. To use, instantiate and then call [run]
 */
class Interpreter {

    /**
     * The tokens of the current parse
     */
    lateinit var tokens: Array<Token>

    /**
     * The current token the interpreter is parsing
     */
    var currentParseIndex = 0

    /**
     * The cumulative closure of all code that has been ran in this [Interpreter] instance
     */
    val closure = Closure()

    init {
        // initialize all native language features that can't be written with other scheme code
        closure["define"] = DefineFunction
        closure["if"] = IfFunction
        closure["lambda"] = LambdaFunction
        closure["+"] = NativeFunction { closure, args ->
            IntLiteral(
                args.sumBy { (it as IntLiteral).value }
            )
        }
        closure["-"] = NativeFunction { closure, args ->
            args.foldIndexed(
                args[0],
                { index, acc, next ->
                    if (index > 0) IntLiteral((acc as IntLiteral).value - (next as IntLiteral).value) else acc
                })
        }
        closure["*"] = NativeFunction { closure, args ->
            args.foldIndexed(
                args[0],
                { index, acc, next ->
                    if (index > 0) IntLiteral((acc as IntLiteral).value * (next as IntLiteral).value) else acc
                })
        }
        closure["/"] = NativeFunction { closure, args ->
            args.foldIndexed(
                args[0],
                { index, acc, next ->
                    if (index > 0) IntLiteral((acc as IntLiteral).value / (next as IntLiteral).value) else acc
                })
        }
        closure["modulo"] = NativeFunction { closure, args ->
            IntLiteral(
                (args[0] as IntLiteral).value % (args[1] as IntLiteral).value
            )
        }
        closure[">"] = NativeFunction { closure, args ->
            BooleanLiteral((args[0] as IntLiteral).value > (args[1] as IntLiteral).value)
        }
        closure["<"] = NativeFunction { closure, args ->
            BooleanLiteral((args[0] as IntLiteral).value < (args[1] as IntLiteral).value)
        }
        closure["list"] = NativeFunction { closure, args ->
            Data.link(args)
        }
        closure["apply"] = NativeFunction { closure, args ->
            val function = args[0]
            val functionArgs = args[1]
            function.evaluate(closure, functionArgs)
        }
        closure["cond"] = CondFunction
        closure["else"] = ElseNode
        closure["cons"] = NativeFunction { closure, args ->
            Cell(args[0], args[1])
        }
        closure["car"] = NativeFunction { closure, args ->
            (args[0] as Cell).left
        }
        closure["cdr"] = NativeFunction { closure, args ->
            (args[0] as Cell).right
        }
        closure["="] = NativeFunction { closure, args ->
            BooleanLiteral(args[0] == args[1])
        }
        closure["null?"] = NativeFunction { closure, args ->
            BooleanLiteral(args[0] is Nil)
        }
        closure["not"] = NativeFunction { closure, args ->
            BooleanLiteral(args[0] == BooleanLiteral(false))
        }
        closure["length"] = NativeFunction { closure, args ->
            IntLiteral(Data.delink(args[0]).size)
        }
        closure["let"] = LetFunction
        closure["and"] = AndFunction
        closure["or"] = OrFunction
        closure["null"] = Nil
    }

    /**
     * Parses and then evaluates the inputted text, returning the result. Side effects made by this code on the global
     * [Closure] will remain for later code ran.
     */
    fun run(text: String): Node {
        tokenize(text)
        var lastResult: Node? = null
        while (currentParseIndex < tokens.lastIndex) {
            val expr = expression()
            lastResult = expr.evaluate(closure, Nil)
        }
        return lastResult ?: Nil
    }

    private fun tokenize(text: String) {
        val tokens = mutableListOf<Token>()
        for (line in text.lines()) {
            val betterText = line.replace("(", " ( ").replace(")", " ) ")
            val words = betterText.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
            for (word in words) {
                val completeMatch = TokenType.values().firstOrNull { it.regex.matchEntire(word) != null }
                if (completeMatch != null) {
                    tokens.add(Token(completeMatch, word))
                } else {
                    tokens.add(Token(TokenType.SYMBOL, word))
                }
            }
            tokens.add(Token(TokenType.NEW_LINE, "\n"))
        }
        this.tokens = tokens.toTypedArray()
        currentParseIndex = 0
    }

    private fun eat(): Token {
        while (currentParseIndex < tokens.lastIndex - 1 && peek().type == TokenType.NEW_LINE) {
            currentParseIndex++
        }
        val token = tokens[currentParseIndex]
        currentParseIndex++
        return token
    }

    private fun eat(tokenType: TokenType): Token {
        if (currentParseIndex > tokens.lastIndex) {
            throw Exception("End of file, expecting $tokenType")
        }
        while (currentParseIndex < tokens.lastIndex - 1 && peek().type == TokenType.NEW_LINE) {
            currentParseIndex++
        }
        val token = tokens[currentParseIndex]
        if (token.type != tokenType) {
            throw Exception("Expected $tokenType, got ${token.type}")
        }
        currentParseIndex++
        return token
    }

    private fun peek() = tokens[currentParseIndex]

    private fun expression(): Node {
        val next = eat()
        when (next.type) {
            TokenType.NEW_LINE -> {
                return Nil
            }
            TokenType.QUOTE -> {
                return QuoteFunction(expression())
            }
            TokenType.OPEN_PAREN -> {
                val sExprs = mutableListOf<Node>()
                while (peek().type != TokenType.CLOSE_PAREN) {
                    sExprs.add(expression())
                }
                eat(TokenType.CLOSE_PAREN)
                return Data.link(sExprs)
            }
            TokenType.LITERAL_BOOLEAN -> {
                return BooleanLiteral(next.text != "#f")
            }
            TokenType.LITERAL_INT -> {
                return IntLiteral(next.text.toInt())
            }
            TokenType.SYMBOL -> {
                return TextNode(next.text)
            }
            TokenType.LITERAL_STRING -> {
                return StringLiteral(next.text.substring(1, next.text.length - 1))
            }
            else -> {
                throw Exception("Unhandled token $next")
            }
        }
    }
}