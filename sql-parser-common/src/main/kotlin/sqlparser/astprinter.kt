package sqlparser

import sqlparser.Ast.*
import sqlparser.Ast.Expression.Literal.*
import sqlparser.Ast.SelectOrUnion.*

open class SqlPrinter {

    protected open val operatorPrecedences = mapOf(
            "*" to 10,
            "/" to 10,
            "%" to 10,
            "+" to 9,
            "-" to 9,
            ">" to 8,
            ">=" to 8,
            "<" to 8,
            "<=" to 8,
            "=" to 8,
            "!=" to 8,
            "IS NULL" to 8,
            "IS NOT NULL" to 8,
            "IN" to 8,
            "NOT IN" to 8,
            "AND" to 7,
            "OR" to 6)

    companion object {
        fun from(ast: Ast): String {
            return SqlPrinter().render(ast)
        }
    }

    fun render(node: Ast): String {
        return when(node) {
            is File -> render(node)
            is Statement.CreateSchema -> render(node)
            is Statement.CreateTable -> render(node)
            is Statement.SelectStmt -> render(node)
            is ColumnDefinition -> render(node)
            is Identifier -> render(node)
            is Expression -> render(node)
            is NamedExpression -> render(node)
            is OrderExpression -> render(node)
            is SelectClause -> render(node)
            is FromClause -> render(node)
            is Union -> render(node)
            is DataSource -> render(node)
        }
    }

    protected open fun render(node: File): String {
        return node.statements.joinToString("\n\n") { render(it) }
    }

    protected open fun render(node: Statement.CreateSchema): String {
        return "CREATE SCHEMA ${render(node.schemaName)};"
    }

    protected open fun render(node: Statement.CreateTable): String {
        return "CREATE TABLE ${render(node.tableName)} (\n" +
              node.columns.joinToString(",\n") { render(it) }.prependIndent("  ") +
                "\n);"
    }

    protected open fun render(node: DataSource): String {
        val renderedSource =  when (node) {
            is DataSource.Table -> render(node.identifier)
            is DataSource.SubQuery -> "(\n${render(node.subQuery).prependIndent("  ")}\n)"
            is DataSource.Join -> render(node)
            is DataSource.TableFunction -> return render(node)
        }
        return if(node.alias != null){
            renderedSource + " " + render(node.alias!!)
        } else {
            renderedSource
        }
    }

    protected open fun render(node: DataSource.TableFunction): String {
        return "${render(node.function)} AS ${render(node.alias!!)} (${node.columnAliases.joinToString { render(it) }})"
    }

    protected open fun render(node: Statement.SelectStmt): String {
        return "${render(node.selectClause)};"
    }

    protected open fun render(node: Union): String {
        val all = if(node.all) " ALL" else ""
        return render(node.top) + "\nUNION" + all + "\n" + render(node.bottom)
    }

    protected open fun render(node: SelectClause): String {
        val ctes = if (node.ctes.isNotEmpty()) {
            "WITH " + node.ctes.joinToString(",\n") {
                "${render(it.alias!!)} AS (\n${render(it.subQuery).prependIndent("  ")}\n)"
            } + "\n\n"
        } else ""
        val selectExpressions = node.selectExpressions.joinToString(",\n") { render(it) }
        val distinct = if(node.distinct) " DISTINCT" else ""
        val fromClause = node.fromClause?.let { "\n" + render(it) } ?: ""
        val whereClause = node.predicate?.let { "\nWHERE\n  " + render(it)} ?: ""
        val groupByClause = if(node.groupByExpressions.isNotEmpty()) {
            "\nGROUP BY\n" + node.groupByExpressions.joinToString(",\n") { render(it) }.prependIndent("  ")
        } else ""
        val havingClause = node.havingPredicate?.let { "\nHAVING\n  " + render(it)} ?: ""
        val orderByClause = if(node.orderByExpressions.isNotEmpty()) {
            "\nORDER BY\n" + node.orderByExpressions.joinToString(",\n") { render(it) }.prependIndent("  ")
        } else ""
        val limitClause = node.limit?.let { "\nLIMIT $it" } ?: ""

        return  ctes +
                "SELECT" + distinct + "\n" +
                selectExpressions.prependIndent("  ") +
                fromClause +
                whereClause +
                groupByClause +
                havingClause +
                orderByClause +
                limitClause
    }

    protected open fun render(node: FromClause): String {
        return "FROM\n" + if(node.source is DataSource.Join) {
            render(node.source)
        } else {
            render(node.source).prependIndent("  ")
        }
    }

    protected open fun render(node: DataSource.Join): String {
        val joinType = when(node.joinType) {
            JoinType.INNER -> "INNER"
            JoinType.CROSS -> "CROSS"
            JoinType.FULL_OUTER -> "FULL OUTER"
            JoinType.LEFT_OUTER -> "LEFT OUTER"
            JoinType.RIGHT_OUTER -> "RIGHT OUTER"
        }

        val onClause = node.onExpression?.let { "\nON " + render(it) } ?: ""

        return if(node.left is DataSource.Join) {
            render(node.left)
        } else {
            render(node.left).prependIndent("  ")
        } + "\n$joinType JOIN\n" +
                render(node.right).prependIndent("  ") +
                onClause
    }

    protected open fun render(node: NamedExpression): String {
        return if (node.name != null) {
            "${render(node.expression)} AS ${node.name}"
        } else {
            render(node.expression)
        }
    }

    protected open fun render(node: OrderExpression): String {
        val ascDesc = if(node.asc) "ASC" else "DESC"
        return "${render(node.expression)} $ascDesc"
    }

    protected open fun render(node: Expression): String {
        return when(node) {
            is Expression.Literal -> render(node)
            is Expression.Reference -> render(node)
            is Expression.FunctionCall -> render(node)
            is Expression.Case -> render(node)
            is Expression.Cast -> render(node)
            is Expression.ScalarSelect -> "(${render(node.subQuery)})"
        }
    }

    protected open fun render(node: Expression.Cast): String {
        val _try = if (node._try) "TRY_" else ""
        return "${_try}CAST(${render(node.expression)} AS ${node.dataType})"
    }

    protected open fun render(node: Expression.Reference): String {
        return render(node.identifier)
    }

    protected open fun render(node: Expression.Case): String {
        val inputExpression = node.inputExpression?.let { " ${render(it)}" } ?: ""
        return "CASE$inputExpression\n" +
                node.matchExpressions.joinToString("") { (a, b) -> "  WHEN ${render(a)} THEN ${render(b)}\n" } +
                (node.elseExpression?.let { "  ELSE ${render(it)}\n" } ?: "") +
                "END"
    }

    protected open fun render(node: Expression.FunctionCall): String {
        val rawArgs = node.args

        return if(node.infix) {
            // If the left expression is an infix function at a lower precedence
            // then we need to wrap it in brackets.
            // If the right expression is an infix function at a lower or the same precedence
            // then we need to wrap it in brackets
            val precedence = operatorPrecedences.getOrElse(node.functionName) { 100 }
            val left = rawArgs[0].let {
                if (it is Expression.FunctionCall && it.infix &&
                        operatorPrecedences.getOrElse(it.functionName) { 0 } < precedence) {
                    "(${render(it)})"
                } else {
                    render(it)
                }
            }
            val right = rawArgs.getOrNull(1)?.let {
                if (it is Expression.FunctionCall && it.infix &&
                        operatorPrecedences.getOrElse(it.functionName) { 0 } <= precedence) {
                    "(${render(it)})"
                } else {
                    render(it)
                }
            }



            if (node.functionName == "IN" || node.functionName == "NOT IN") {
                // special case for IN/NOT IN
                "$left ${node.functionName} (${rawArgs.drop(1).joinToString { render(it) }})"
            } else if (node.functionName == "BETWEEN") {
                "$left ${node.functionName} $right AND ${render(rawArgs[2])}"
            } else if (rawArgs.size == 1 && (node.functionName == "-" || node.functionName == "NOT")) {
                // special case for unitary minus and not
                "${node.functionName} $left"
            } else {
                "$left ${node.functionName}" + (right?.let { " $it" } ?: "")
            }

        } else if (node.functionName == "ARRAY" ) {
            // Special case for array constructor used by presto
            val args = rawArgs.map(::render)
            "ARRAY[${args.joinToString()}]"
        } else {
            val args = rawArgs.map(::render)
            val distinct = if (node.distinct) "DISTINCT " else ""
            "${node.functionName}($distinct${args.joinToString()})"
        }
    }

    protected open fun render(node: Expression.Literal): String {
        return when(node) {
            is DateLiteral -> render(node)
            is IntervalLiteral -> render(node)
            is FloatLiteral -> render(node)
            is IntLiteral -> render(node)
            is NullLiteral -> render(node)
            is BooleanLiteral -> render(node)
            is StringLiteral -> render(node)
        }
    }

    protected open fun render(node: IntervalLiteral): String {
        return "INTERVAL '${node.value}'"
    }

    protected open fun render(node: DateLiteral): String {
        return "DATE '${node.value}'"
    }

    protected open fun render(node: FloatLiteral): String {
        return node.value.toString()
    }

    protected open fun render(node: IntLiteral): String {
        return node.value.toString()
    }

    protected open fun render(node: NullLiteral): String {
        return "NULL"
    }

    protected open fun render(node: BooleanLiteral): String {
        return if (node.value) {
            "TRUE"
        } else {
            "FALSE"
        }
    }

    protected open fun render(node: StringLiteral): String {
        return "\'${node.value.replace("'", "\\'")}\'"
    }

    open fun render(node: Ast.ColumnDefinition): String {
        return "${render(node.columnName)} ${node.type}"
    }

    open fun render(node: Ast.Identifier): String {
        return (node.qualifiers + listOf(node.identifier)).joinToString(".")
    }
}