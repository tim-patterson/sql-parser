package sqlparser

data class Dialect (
        val singleQuote: LiteralType = LiteralType.STRING_LIT,
        val doubleQuote: LiteralType = LiteralType.IDENTIFIER_LIT,
        val backTick: LiteralType = LiteralType.STRING_LIT
) {
    enum class LiteralType { STRING_LIT, IDENTIFIER_LIT }
    companion object {
        val DEFAULT = Dialect()
        val HIVE = Dialect(doubleQuote = LiteralType.STRING_LIT)
    }
}