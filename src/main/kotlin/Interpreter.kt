class Interpreter(val text: String) {

    lateinit var tokens: Array<Token>

    var currentParseIndex = 0

    val closure = Closure()

    init {
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

    fun run(): Node {
        tokenize()
        var lastResult: Node? = null
        while (currentParseIndex < tokens.lastIndex) {
            val expr = expression()
            println(expr)
            lastResult = expr.evaluate(closure, Nil)
        }
        return lastResult ?: Nil
    }

    fun tokenize() {
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
    }

    fun eat(): Token {
        while (currentParseIndex < tokens.lastIndex - 1 && peek().type == TokenType.NEW_LINE) {
            currentParseIndex++
        }
        val token = tokens[currentParseIndex]
        currentParseIndex++
        return token
    }

    fun eat(tokenType: TokenType): Token {
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

    fun peek() = tokens[currentParseIndex]

    fun expression(): Node {
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