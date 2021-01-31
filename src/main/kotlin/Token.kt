class Token(val type: TokenType, val text: String) {
    override fun toString(): String {
        return "Token(type=$type, text=$text)"
    }
}