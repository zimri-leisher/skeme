class Token(val type: TokenType, val text: String) {
    override fun toString(): String {
        return "Token(type=$type, text=$text)"
    }
}

enum class TokenType(val text: String) {
    OPEN_PAREN("""\("""),
    CLOSE_PAREN("""\)"""),
    QUOTE("""\'"""),
    LITERAL_INT("""\d+"""),
    LITERAL_BOOLEAN("""#f|#t"""),
    LITERAL_STRING("""".+""""),
    NEW_LINE("""$"""),
    SYMBOL("""a^""") // match nothing, this is everything else
    ;

    val regex = Regex(text)
}