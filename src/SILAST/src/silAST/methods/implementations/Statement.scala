package silAST.methods.implementations

import silAST.ASTNode
import silAST.types.DataType
import silAST.expressions.Expression
import silAST.expressions.PredicateExpression
import silAST.source.SourceLocation
import silAST.expressions.util.PTermSequence
import silAST.programs.symbols.{ProgramVariableSequence, Field, ProgramVariable}
import silAST.methods.Method
import silAST.expressions.terms.PTerm

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
sealed abstract class Statement private[silAST] extends ASTNode {
  override def toString: String
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
final case class AssignmentStatement private[silAST](

                                                      target: ProgramVariable,
                                                      source: PTerm
                                                      )(override val sourceLocation: SourceLocation)
  extends Statement {
  override def toString: String = target.name + ":=" + source.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
case class FieldAssignmentStatement private[silAST](

                                                     target: ProgramVariable,
                                                     field: Field,
                                                     source: PTerm
                                                     )(override val sourceLocation: SourceLocation)
  extends Statement {
  override def toString: String = target.name + "." + field.name + " := " + source.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
case class NewStatement private[silAST](

                                         target: ProgramVariable,
                                         dataType: DataType
                                         )(override val sourceLocation: SourceLocation)
  extends Statement {
  override def toString: String = target.name + ":= new " + dataType.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//TODO:check signature
final case class CallStatement private[silAST]
(
  targets: ProgramVariableSequence,
  method: Method,
  arguments: PTermSequence
  )(override val sourceLocation: SourceLocation)
  extends Statement
{
  def receiver = arguments.head
  override def toString: String = targets.toString + " := " + arguments.head.toString + "." + method.name + arguments.tail.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
final case class InhaleStatement private[silAST](

                                                  expression: Expression
                                                  )(override val sourceLocation: SourceLocation)
  extends Statement {
  override def toString: String = "inhale " + expression.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
final case class ExhaleStatement private[silAST](

                                                  expression: Expression
                                                  )(override val sourceLocation: SourceLocation)
  extends Statement {
  override def toString: String = "exhale " + expression.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//TODO:FoldStatement/UnfoldStatement arrays?
final case class FoldStatement private[silAST](

                                                predicate: PredicateExpression
                                                )(override val sourceLocation: SourceLocation)
  extends Statement {
  override def toString: String = "fold " + predicate.toString
}

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
final case class UnfoldStatement private[silAST](

                                                  predicate: PredicateExpression
                                                  )(override val sourceLocation: SourceLocation)
  extends Statement {
  override def toString: String = "unfold " + predicate.toString
}
