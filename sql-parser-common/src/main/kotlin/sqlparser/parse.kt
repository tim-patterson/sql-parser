package sqlparser

import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.misc.ParseCancellationException
import org.antlr.v4.kotlinruntime.tree.TerminalNode

import sqlparser.Ast.*
import sqlparser.Ast.Expression.*
import sqlparser.Ast.Expression.Literal.*


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

private fun parseFile(node: SqlParser.FileContext) : File {
    val pos = SourcePosition(node.position)
    return Ast.File(node.findStmt().map(::parseStatement), pos)
}


private fun parseStatement(node: SqlParser.StmtContext) : Statement {
    return when {
        node.findCreateSchemaStmt() != null -> parseCreateSchemaStmt(node.findCreateSchemaStmt()!!)
        node.findCreateTableStmt() != null -> parseCreateTableStmt(node.findCreateTableStmt()!!)
        else -> TODO()
    }
}

private fun parseCreateSchemaStmt(node: SqlParser.CreateSchemaStmtContext): Statement.CreateSchema {
    val pos = SourcePosition(node.position)
    val ifNotExists = node.findIfNotExists() != null
    val schemaName = parseSimpleIdentifier(node.findSimpleIdentifier() ?: node.findCreateSchemaStmtAuthorizationClause()!!.findSimpleIdentifier()!!)
    return Ast.Statement.CreateSchema(schemaName, ifNotExists, pos)
}


private fun parseCreateTableStmt(node: SqlParser.CreateTableStmtContext): Statement.CreateTable {
    val pos = SourcePosition(node.position)
    //val ifNotExists = node.findIfNotExists() != null
    val tableName = parseQualifiedIdentifier(node.findQualifiedIdentifier()!!)
    val columns = node.findCreateTableStmtColumnList()!!.findCreateTableStmtColumnSpec().map(::parseColumnDefinition)
    return Ast.Statement.CreateTable(tableName, columns, pos)
}

private fun parseColumnDefinition(node: SqlParser.CreateTableStmtColumnSpecContext): ColumnDefinition {
    val pos = SourcePosition(node.position)
    val columnName = parseSimpleIdentifier(node.findSimpleIdentifier()!!)
    val dataTypeNode = node.findDataType()!!
    val dataType = if (dataTypeNode.INTERVAL() != null ){
        dataTypeNode.children!!.joinToString(" ") { it.text }.toUpperCase()
    } else {
        dataTypeNode.text.toUpperCase()
    }
    return Ast.ColumnDefinition(columnName, dataType, pos)
}


private fun parseExpression(node: SqlParser.ExpressionContext): Expression {
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
    val identifier = node.text.toLowerCase()
    return Ast.Identifier(null, identifier, sourcePosition = SourcePosition(node.position))
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