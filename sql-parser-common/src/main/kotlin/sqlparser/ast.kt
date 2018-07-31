package sqlparser

import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.misc.ParseCancellationException
import org.antlr.v4.kotlinruntime.tree.TerminalNode

sealed class Ast {
    data class File(val statements: List<Statement>): Ast()

    sealed class Statement: Ast() {
        data class CreateSchema(val schemaName: Identifier, val ifNotExists: Boolean = false): Statement()
        data class CreateTable(val tableName: Identifier, val columns: List<ColumnDefinition>): Statement()
    }

    data class Identifier(val qualifier: String?, val identifier: String): Ast()
    data class ColumnDefinition(val columnName: Identifier, val type: String): Ast()
    sealed class Expression: Ast() {
        sealed class Literal: Expression() {
            data class DateLiteral(val value: String): Literal()
            data class StringLiteral(val value: String): Literal()
            data class IntLiteral(val value: Long): Literal()
            data class FloatLiteral(val value: Double): Literal()
            data class BooleanLiteral(val value: Boolean): Literal()
            object NullLiteral: Literal()
        }
    }
}

// Top level parse functions
fun parseFile(sql: String, strict: Boolean = false): Ast.File {
    return parseFile(sql.parser(strict).file())
}

fun parseStatement(sql: String, strict: Boolean = false): Ast.Statement {
    return parseStatement(sql.parser(strict).singleStmt().findStmt()!!)
}

fun parseExpression(sql: String, strict: Boolean = false): Ast.Expression {
    return parseExpression(sql.parser(strict).singleExpression().findExpression()!!)
}


// parse tree -> Ast functions

private fun parseFile(node: SqlParser.FileContext) : Ast.File {
    return Ast.File(node.findStmt().map(::parseStatement))
}


private fun parseStatement(node: SqlParser.StmtContext) : Ast.Statement {
    return when {
        node.findCreateSchemaStmt() != null -> parseCreateSchemaStmt(node.findCreateSchemaStmt()!!)
        node.findCreateTableStmt() != null -> parseCreateTableStmt(node.findCreateTableStmt()!!)
        else -> TODO()
    }
}

private fun parseCreateSchemaStmt(node: SqlParser.CreateSchemaStmtContext): Ast.Statement.CreateSchema {
    val ifNotExists = node.findIfNotExists() != null
    val schemaName = parseSimpleIdentifier(node.findSimpleIdentifier() ?: node.findCreateSchemaStmtAuthorizationClause()!!.findSimpleIdentifier()!!)
    return Ast.Statement.CreateSchema(schemaName, ifNotExists)
}


private fun parseCreateTableStmt(node: SqlParser.CreateTableStmtContext): Ast.Statement.CreateTable {
    //val ifNotExists = node.findIfNotExists() != null
    val tableName = parseQualifiedIdentifier(node.findQualifiedIdentifier()!!)
    val columns = node.findCreateTableStmtColumnList()!!.findCreateTableStmtColumnSpec().map(::parseColumnDefinition)
    return Ast.Statement.CreateTable(tableName, columns)
}

private fun parseColumnDefinition(node: SqlParser.CreateTableStmtColumnSpecContext): Ast.ColumnDefinition {
    val columnName = parseSimpleIdentifier(node.findSimpleIdentifier()!!)
    val dataTypeNode = node.findDataType()!!
    val dataType = if (dataTypeNode.INTERVAL() != null ){
        dataTypeNode.children!!.joinToString(" ") { it.text }.toUpperCase()
    } else {
        dataTypeNode.text.toUpperCase()
    }
    return Ast.ColumnDefinition(columnName, dataType)
}


private fun parseExpression(node: SqlParser.ExpressionContext): Ast.Expression {
    return when {
        node.findLiteral() != null -> parseLiteral(node.findLiteral()!!)
        else -> TODO("Can't parse expression ${node.text}")
    }
}

private fun parseLiteral(node: SqlParser.LiteralContext): Ast.Expression.Literal {
    return when {
        node.FALSE() != null -> Ast.Expression.Literal.BooleanLiteral(false)
        node.TRUE() != null -> Ast.Expression.Literal.BooleanLiteral(true)
        node.NULL() != null -> Ast.Expression.Literal.NullLiteral
        node.DATE() != null -> Ast.Expression.Literal.DateLiteral(parseStringLit(node.STRING_LITERAL()!!))
        node.STRING_LITERAL() != null -> Ast.Expression.Literal.StringLiteral(parseStringLit(node.STRING_LITERAL()!!))
        node.POSITIVE_FLOAT_LITERAL() != null -> Ast.Expression.Literal.FloatLiteral(node.POSITIVE_FLOAT_LITERAL()!!.text.toDouble())
        node.POSITIVE_INT_LITERAL() != null -> Ast.Expression.Literal.IntLiteral(node.POSITIVE_INT_LITERAL()!!.text.toLong())
        else -> TODO("Can't parse literal ${node.text}")
    }
}


private fun parseQualifiedIdentifier(node: SqlParser.QualifiedIdentifierContext): Ast.Identifier {
    val components = node.findSimpleIdentifier().map(::parseSimpleIdentifier)
    return if (components.size > 1) {
        components[1].copy(qualifier = components[0].identifier)
    } else {
        components[0]
    }
}


private fun parseSimpleIdentifier(node: SqlParser.SimpleIdentifierContext): Ast.Identifier {
    val identifier = node.text.toLowerCase()
    return Ast.Identifier(null, identifier)
}

private fun parseStringLit(node: TerminalNode): String {
    val str = node.text
    return str.substring(1, str.length -1 )
}

private fun String.parser(strict: Boolean = false): SqlParser {
    val input = ANTLRInputStream(this)
    val lexer = SqlLexer(input)
    val tokens = CommonTokenStream(lexer)
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
