package sqlparser

import org.antlr.v4.kotlinruntime.*
import org.antlr.v4.kotlinruntime.misc.ParseCancellationException

sealed class Ast {
    data class File(val statements: List<Statement>): Ast()

    sealed class Statement: Ast() {
        data class CreateSchema(val schemaName: Identifier, val ifNotExists: Boolean = false): Statement()
        data class CreateTable(val tableName: Identifier, val columns: List<ColumnDefinition>): Statement()
    }

    data class Identifier(val qualifier: String?, val identifier: String): Ast()
    data class ColumnDefinition(val columnName: Identifier, val type: String): Ast()
}

// Top level parse functions
fun parseFile(sql: String, strict: Boolean = false): Ast.File {
    return parseFile(sql.parser(strict).file())
}

fun parseStatement(sql: String, strict: Boolean = false): Ast.Statement {
    return parseStatement(sql.parser(strict).singleStmt().findStmt()!!)
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
