package sqlparser

import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.misc.ParseCancellationException
import org.antlr.v4.kotlinruntime.tree.TerminalNode

import sqlparser.Ast.*
import sqlparser.Ast.Expression.*
import sqlparser.Ast.Expression.Literal.*
import sqlparser.Ast.SelectOrUnion.*

// Top level parse functions
fun parseFile(sql: String, strict: Boolean = false, dialect: Dialect = Dialect.DEFAULT): Ast.File {
    return Parser().parseFile(sql.parser(dialect, strict).file())
}

fun parseStatement(sql: String, strict: Boolean = false, dialect: Dialect = Dialect.DEFAULT): Ast.Statement {
    return Parser().parseStatement(sql.parser(dialect, strict).singleStmt().findStmt()!!)
}

fun parseExpression(sql: String, strict: Boolean = false, dialect: Dialect = Dialect.DEFAULT): Ast.Expression {
    return Parser().parseExpression(sql.parser(dialect, strict).singleExpression().findExpression()!!)
}

private fun String.parser(dialect: Dialect, strict: Boolean = false): SqlParser {
    val input = ANTLRInputStream(this)
    val lexer = SqlLexer(input)
    val tokens = CommonTokenStream(DialectRewriter(lexer, dialect))

    val parser = SqlParser(tokens)
    if (strict) {
        parser.addErrorListener(ThrowingErrorListener)
    }
    return parser
}

private object ThrowingErrorListener : BaseErrorListener() {
    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
        // println("line $line:$charPositionInLine $msg")
        throw ParseCancellationException("line $line:$charPositionInLine $msg")
    }
}

private class DialectRewriter(val delegate: TokenSource, val dialect: Dialect) : TokenSource by delegate {

    class MasqueradingToken(val token: Token, override val type: Int) : Token by token

    override fun nextToken(): Token {
        val token = delegate.nextToken()
        return when (token.type) {
            SqlLexer.Tokens.SINGLE_QUOTED_LIT.id -> MasqueradingToken(token, tokenTypeForLit(dialect.singleQuote))
            SqlLexer.Tokens.DOUBLE_QUOTED_LIT.id -> MasqueradingToken(token, tokenTypeForLit(dialect.doubleQuote))
            SqlLexer.Tokens.BACKTICKED_LIT.id -> MasqueradingToken(token, tokenTypeForLit(dialect.backTick))
            else -> token
        }
    }

    private fun tokenTypeForLit(literalType: Dialect.LiteralType): Int {
        return when (literalType) {
            Dialect.LiteralType.STRING_LIT -> SqlParser.Tokens.STRING_LITERAL.id
            Dialect.LiteralType.IDENTIFIER_LIT -> SqlParser.Tokens.IDENTIFIER.id
        }
    }
}

private class Parser {

    fun parseFile(node: SqlParser.FileContext): File {
        val pos = SourcePosition(node.position)
        return Ast.File(node.findStmt().map(::parseStatement), pos)
    }


    fun parseStatement(node: SqlParser.StmtContext): Statement {
        return when {
            node.findCreateSchemaStmt() != null -> parseCreateSchemaStmt(node.findCreateSchemaStmt()!!)
            node.findCreateTableStmt() != null -> parseCreateTableStmt(node.findCreateTableStmt()!!)
            node.findSelectStmt() != null -> parseSelectStmt(node.findSelectStmt()!!)
            else -> TODO()
        }
    }

    private fun parseCreateSchemaStmt(node: SqlParser.CreateSchemaStmtContext): Statement.CreateSchema {
        val pos = SourcePosition(node.position)
        val ifNotExists = node.findIfNotExists() != null
        val schemaName = parseSimpleIdentifier(node.findSimpleIdentifier()
                ?: node.findCreateSchemaStmtAuthorizationClause()!!.findSimpleIdentifier()!!)
        return Ast.Statement.CreateSchema(schemaName, ifNotExists, pos)
    }


    private fun parseCreateTableStmt(node: SqlParser.CreateTableStmtContext): Statement.CreateTable {
        val pos = SourcePosition(node.position)
        //val ifNotExists = node.findIfNotExists() != null
        val tableName = parseQualifiedIdentifier(node.findQualifiedIdentifier()!!)
        val columns = node.findCreateTableStmtColumnList()!!.findCreateTableStmtColumnSpec().map(::parseColumnDefinition)
        return Ast.Statement.CreateTable(tableName, columns, pos)
    }

    private fun parseSelectStmt(node: SqlParser.SelectStmtContext): Statement.SelectStmt {
        val pos = SourcePosition(node.position)
        val selectClause = parseSelectOrUnion(node.findSelectOrUnion()!!)
        return Statement.SelectStmt(selectClause, pos)
    }

    private fun parseSelectOrUnion(node: SqlParser.SelectOrUnionContext): SelectOrUnion {
        val pos = SourcePosition(node.position)
        return if(node.UNION() != null) {
            val top = parseSelectOrUnion(node.findSelectOrUnion()!!)
            val bottom = parseSelectClause(node.findSelectClause()!!)
            val all = node.ALL() != null
            Union(top, bottom, all, pos)
        } else {
            parseSelectClause(node.findSelectClause()!!)
        }

    }

    private fun parseSelectClause(node: SqlParser.SelectClauseContext): SelectClause {
        val pos = SourcePosition(node.position)
        val distinct = node.DISTINCT() != null
        val expressions = node.findNamedExpression().map(::parseNamedExpression)
        val fromClause = node.findFromClause()?.let { parseFromClause(it) }
        val predicate = node.findWhereClause()?.findExpression()?.let { parseExpression(it) }
        return SelectClause(expressions, distinct, fromClause, predicate, pos)
    }

    private fun parseFromClause(node: SqlParser.FromClauseContext): FromClause {
        val pos = SourcePosition(node.position)
        val fromItems = node.findFromItem().map(::parseFromItem)

        val source = fromItems.reduce { left, right ->
            DataSource.Join(left, right, joinType = JoinType.CROSS)
        }
        return FromClause(source, pos)
    }

    private fun parseFromItem(node: SqlParser.FromItemContext): DataSource {
        val pos = SourcePosition(node.position)
        val alias = node.findSimpleIdentifier()?.let { parseSimpleIdentifier(it) }

        return when {
            node.findDataSource() != null -> {
                val dataSource = node.findDataSource()!!
                when {
                    dataSource.findQualifiedIdentifier() != null -> {
                        val tblIdentifier = parseQualifiedIdentifier(dataSource.findQualifiedIdentifier()!!)
                        DataSource.Table(tblIdentifier, alias, pos)
                    }

                    dataSource.findSelectOrUnion() != null -> {
                        val subQuery = parseSelectOrUnion(dataSource.findSelectOrUnion()!!)
                        DataSource.SubQuery(subQuery, alias, pos)
                    }

                    else -> TODO()
                }
            }
            else -> {
                val left = parseFromItem(node.findFromItem(0)!!)
                val right = parseFromItem(node.findFromItem(1)!!)
                val joinType = when {
                    node.CROSS() != null -> JoinType.CROSS
                    node.OUTER() != null -> when {
                        node.LEFT() != null -> JoinType.LEFT_OUTER
                        node.RIGHT() != null -> JoinType.RIGHT_OUTER
                        else -> JoinType.FULL_OUTER
                    }
                    else -> JoinType.INNER
                }

                val onExpression = node.findExpression()?.let { parseExpression(it) }
                DataSource.Join(left, right,joinType, onExpression)
            }
        }
    }

    private fun parseNamedExpression(node: SqlParser.NamedExpressionContext): NamedExpression {
        val pos = SourcePosition(node.position)
        val expression = parseExpression(node.findExpression()!!)
        val name = node.findSimpleIdentifier()?.let { parseSimpleIdentifier(it).identifier }
        return NamedExpression(name, expression, pos)
    }

    private fun parseColumnDefinition(node: SqlParser.CreateTableStmtColumnSpecContext): ColumnDefinition {
        val pos = SourcePosition(node.position)
        val columnName = parseSimpleIdentifier(node.findSimpleIdentifier()!!)
        val dataTypeNode = node.findDataType()!!
        val dataType = if (dataTypeNode.INTERVAL() != null) {
            dataTypeNode.children!!.joinToString(" ") { it.text }.toUpperCase()
        } else {
            dataTypeNode.text.toUpperCase()
        }
        return Ast.ColumnDefinition(columnName, dataType, pos)
    }


    fun parseExpression(node: SqlParser.ExpressionContext): Expression {
        val subExpressions = node.findExpression().map(::parseExpression)
        val pos = SourcePosition(node.position)
        return when {
            node.findLiteral() != null -> parseLiteral(node.findLiteral()!!)
            node.AND() != null -> FunctionCall("AND", subExpressions, true, pos)
            node.OR() != null -> FunctionCall("OR", subExpressions, true, pos)
            node.OP_DIV() != null -> FunctionCall("/", subExpressions, true, pos)
            node.OP_MULT() != null -> FunctionCall("*", subExpressions, true, pos)
            node.OP_PLUS() != null -> FunctionCall("+", subExpressions, true, pos)
            node.OP_MINUS() != null -> FunctionCall("-", subExpressions, true, pos)
            node.OP_EQ() != null -> FunctionCall("=", subExpressions, true, pos)
            node.OP_GT() != null -> FunctionCall(">", subExpressions, true, pos)
            node.OP_GTE() != null -> FunctionCall(">=", subExpressions, true, pos)
            node.OP_LT() != null -> FunctionCall("<", subExpressions, true, pos)
            node.OP_LTE() != null -> FunctionCall("<=", subExpressions, true, pos)
            node.OP_NEQ() != null -> FunctionCall("!=", subExpressions, true, pos)
            node.IN() != null -> if (node.NOT() != null) {
                FunctionCall("NOT IN", subExpressions, true, pos)
            } else {
                FunctionCall("IN", subExpressions, true, pos)
            }
            node.NOT() != null -> FunctionCall("IS NOT NULL", subExpressions, true, pos)
            node.NULL() != null -> FunctionCall("IS NULL", subExpressions, true, pos)
            node.findQualifiedIdentifier() != null -> {
                Reference(parseQualifiedIdentifier(node.findQualifiedIdentifier()!!), pos)
            }
            node.findFunctionCall() != null -> {
                val functionCall = node.findFunctionCall()!!
                val args = functionCall.findExpression().map(::parseExpression)
                val functionName = parseSimpleIdentifier(functionCall.findSimpleIdentifier()!!).identifier
                Ast.Expression.FunctionCall(functionName, args, sourcePosition = pos)
            }
            node.OP_OPEN_BRACKET() != null -> subExpressions.single()
            else -> TODO("Can't parse expression ${node.text}")
        }
    }

    private fun parseLiteral(node: SqlParser.LiteralContext): Literal {
        val pos = SourcePosition(node.position)
        return when {
            node.FALSE() != null -> BooleanLiteral(false, pos)
            node.TRUE() != null -> BooleanLiteral(true, pos)
            node.NULL() != null -> NullLiteral(pos)
            node.DATE() != null -> DateLiteral(parseStringLit(node.STRING_LITERAL()!!), pos)
            node.STRING_LITERAL() != null -> StringLiteral(parseStringLit(node.STRING_LITERAL()!!), pos)
            node.POSITIVE_FLOAT_LITERAL() != null -> FloatLiteral(node.POSITIVE_FLOAT_LITERAL()!!.text.toDouble(), pos)
            node.POSITIVE_INT_LITERAL() != null -> IntLiteral(node.POSITIVE_INT_LITERAL()!!.text.toLong(), pos)
            else -> TODO("Can't parse literal ${node.text}")
        }
    }


    private fun parseQualifiedIdentifier(node: SqlParser.QualifiedIdentifierContext): Ast.Identifier {
        val components = node.findSimpleIdentifier().map(::parseSimpleIdentifier)
        return if (components.size > 1) {
            components[1].copy(qualifier = components[0].identifier, sourcePosition = components[0].sourcePosition + components[1].sourcePosition)
        } else {
            components[0]
        }
    }


    private fun parseSimpleIdentifier(node: SqlParser.SimpleIdentifierContext): Ast.Identifier {
        val nodeText = node.text.toLowerCase()
        val identifier = if(node.IDENTIFIER()?.symbol is DialectRewriter.MasqueradingToken) {
            nodeText.substring(1, nodeText.length - 1)
        } else {
            nodeText
        }
        return Ast.Identifier(null, identifier, sourcePosition = SourcePosition(node.position))
    }

    private fun parseStringLit(node: TerminalNode): String {
        val str = node.text
        return str.substring(1, str.length - 1)
    }
}
