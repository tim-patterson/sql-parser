package sqlparser

data class Dialect (
        val singleQuote: LiteralType = LiteralType.STRING_LIT,
        val doubleQuote: LiteralType = LiteralType.STRING_LIT,
        val backTick: LiteralType = LiteralType.STRING_LIT) {
    enum class LiteralType { STRING_LIT, IDENTIFIER_LIT }
    companion object {
        val DEFAULT = Dialect()
        val POSTGRES = Dialect(doubleQuote = LiteralType.IDENTIFIER_LIT)
    }
}