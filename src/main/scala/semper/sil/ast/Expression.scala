package semper.sil.ast

import org.kiama.output._
import semper.sil.ast.utility.{Expressions, Consistency, Transformer}

/** Expressions. */
sealed trait Exp extends Node with Typed with Positioned with Infoed with PrettyExpression {

  /**
   * Transforms an expression using the partial function `pre`, recursing on
   * the subexpressions and finally using the partial function `post`.
   *
   * The previous expression is replaced by applying `pre` and `post`,
   * respectively, if and only if these partial functions are defined there.
   * The functions `pre` and `post` must produce expressions that are valid in
   * the given context. For instance, they cannot replace an integer literal by
   * a Boolean literal.
   *
   * @param pre       Partial function used before the recursion.
   *                  Default: partial function with the empty domain.
   * @param recursive Given the original expression, should the children of the
   *                  expression transformed with `pre` be transformed
   *                  recursively? `pre`, `recursive` and `post` are kept the
   *                  same during each recursion.
   *                  Default: recurse if and only if `pre` is not defined there.
   * @param post      Partial function used after the recursion.
   *                  Default: partial function with the empty domain.
   */
  def transform(pre: PartialFunction[Exp, Exp] = PartialFunction.empty)(
    recursive: Exp => Boolean = !pre.isDefinedAt(_),
    post: PartialFunction[Exp, Exp] = PartialFunction.empty): Exp =
    Transformer.transform(this, pre)(recursive, post)

  def isPure = Expressions.isPure(this)

  /** Returns the subexpressions of this expression */
  def subExps = Expressions.subExps(this)

}

// --- Simple integer and boolean expressions (binary and unary operations, literals)

// Arithmetic expressions
case class Add(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(AddOp)
case class Sub(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(SubOp)
case class Mul(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(MulOp)
case class Div(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(DivOp)
case class Mod(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(ModOp)

// Integer comparison expressions
case class LtCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(LtOp)
case class LeCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(LeOp)
case class GtCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(GtOp)
case class GeCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(GeOp)

// Equality and non-equality (defined for all types)
case class EqCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends EqualityCmp("==")
case class NeCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends EqualityCmp("!=")

/** Integer literal. */
case class IntLit(i: BigInt)(val pos: Position = NoPosition, val info: Info = NoInfo) extends Literal {
  lazy val typ = Int
}

/** Integer negation. */
case class Neg(exp: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainUnExp(NegOp)

// Boolean expressions
case class Or(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(OrOp) {
  require(left.isPure && right.isPure)
}
case class And(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(AndOp)
case class Implies(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(ImpliesOp) {
  require(left.isPure)
}

/** Boolean literals. */
sealed abstract class BoolLit(val value: Boolean) extends Literal {
  lazy val typ = Bool
}
object BoolLit {
  def unapply(b: BoolLit) = Some(b.value)
  def apply(b: Boolean)(pos: Position = NoPosition, info: Info = NoInfo) = if (b) TrueLit()(pos, info) else FalseLit()(pos, info)
}
case class TrueLit()(val pos: Position = NoPosition, val info: Info = NoInfo) extends BoolLit(true)
case class FalseLit()(val pos: Position = NoPosition, val info: Info = NoInfo) extends BoolLit(false)

/** Boolean negation. */
case class Not(exp: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainUnExp(NotOp) {
  require(exp.isPure)
}

case class NullLit()(val pos: Position = NoPosition, val info: Info = NoInfo) extends Literal {
  lazy val typ = Ref
}

// --- Accessibility predicates

/** A common trait for accessibility predicates. */
sealed trait AccessPredicate extends Exp {
  require(perm isSubtype Perm)
  def loc: LocationAccess
  def perm: Exp
  lazy val typ = Bool
}
object AccessPredicate {
  def unapply(a: AccessPredicate) = Some((a.loc, a.perm))
}

/** An accessibility predicate for a field location. */
case class FieldAccessPredicate(loc: FieldAccess, perm: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends AccessPredicate

/** An accessibility predicate for a predicate location. */
case class PredicateAccessPredicate(loc: PredicateAccess, perm: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends AccessPredicate

// --- Inhale exhale expressions.

/**
 * This is a special expression that is treated as `inhaleExp` if it is treated as an assumption and as `exhaleExp` if
 * it is treated as a proof obligation.
 */
case class InhaleExhaleExp(inExp: Exp, exExp: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends Exp {
  require(inExp.typ isSubtype Bool)
  require(exExp.typ isSubtype Bool)
  val typ = Bool
}

// --- Permissions

/** A common trait for expressions of type permission. */
sealed trait PermExp extends Exp {
  override lazy val typ = Perm
}

/** A wild card permission. Has an unknown value, but there are no guarantees that it will be the same inside one method. */
case class WildcardPerm()(val pos: Position = NoPosition, val info: Info = NoInfo) extends PermExp

/** The full permission. */
case class FullPerm()(val pos: Position = NoPosition, val info: Info = NoInfo) extends AbstractConcretePerm(1, 1)

/** No permission. */
case class NoPerm()(val pos: Position = NoPosition, val info: Info = NoInfo) extends AbstractConcretePerm(0, 1)

/** An epsilon permission. */
case class EpsilonPerm()(val pos: Position = NoPosition, val info: Info = NoInfo) extends PermExp

/** A concrete fraction as permission amount. */
case class FractionalPerm(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(FracOp)

/** The permission currently held for a given location. */
case class CurrentPerm(loc: LocationAccess)(val pos: Position = NoPosition, val info: Info = NoInfo) extends PermExp

// Arithmetic expressions
case class PermAdd(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(PermAddOp)
case class PermSub(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(PermSubOp)
case class PermMul(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(PermMulOp)
case class IntPermMul(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(IntPermMulOp)

// Comparison expressions
case class PermLtCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(PermLtOp)
case class PermLeCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(PermLeOp)
case class PermGtCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(PermGtOp)
case class PermGeCmp(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends DomainBinExp(PermGeOp)

// --- Function application (domain and normal)

/** Function application. */
case class FuncApp(func: Function, args: Seq[Exp])(val pos: Position = NoPosition, val info: Info = NoInfo) extends FuncLikeApp {
  /**
   * The precondition of this function application (i.e., the precondition of the function with
   * the arguments instantiated correctly).
   */
  lazy val pres = {
    func.pres map (e => Expressions.instantiateVariables(e, func.formalArgs, args))
  }
  /**
   * The postcondition of this function application (i.e., the postcondition of the function with
   * the arguments instantiated correctly).
   */
  lazy val posts = {
    func.posts map (e => Expressions.instantiateVariables(e, func.formalArgs, args))
  }
}

/** User-defined domain function application. */
case class DomainFuncApp(func: DomainFunc, args: Seq[Exp], typVarMap: Map[TypeVar, Type])(val pos: Position = NoPosition, val info: Info = NoInfo) extends AbstractDomainFuncApp {
  override lazy val typ = super.typ.substitute(typVarMap)
  override def formalArgs: Seq[LocalVarDecl] = {
    callee.formalArgs map {
      fa =>
        // substitute parameter types
        LocalVarDecl(fa.name, fa.typ.substitute(typVarMap))(fa.pos)
    }
  }
}

// --- Field and predicate accesses

/** A common trait for expressions accessing a location. */
sealed trait LocationAccess extends Exp {
  def rcv: Exp
  def loc: Location
}

object LocationAccess {
  def unapply(la: LocationAccess) = Some((la.rcv, la.loc))
}

/** A field access expression. */
case class FieldAccess(rcv: Exp, field: Field)(val pos: Position = NoPosition, val info: Info = NoInfo) extends LocationAccess with Lhs {
  lazy val loc = field
  lazy val typ = field.typ
}

/** A predicate access expression. */
case class PredicateAccess(rcv: Exp, predicate: Predicate)(val pos: Position = NoPosition, val info: Info = NoInfo) extends LocationAccess {
  lazy val loc = predicate
  lazy val typ = Pred

  /** The body of the predicate with the receiver instantiated correctly. */
  lazy val predicateBody =
    Expressions.instantiateVariables(predicate.body, Seq(predicate.formalArg), Seq(rcv))
}

// --- Conditional expression

/** A conditional expressions. */
case class CondExp(cond: Exp, thn: Exp, els: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends Exp {
  require(cond isSubtype Bool)
  require(thn.typ == els.typ)
  lazy val typ = thn.typ
}

// --- Unfolding expression

case class Unfolding(acc: PredicateAccessPredicate, exp: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends Exp {
  lazy val typ = exp.typ
}

// --- Old expression

case class Old(exp: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends UnExp {
  lazy val typ = exp.typ
}

// --- Quantifications

/** A common trait for quantified expressions. */
sealed trait QuantifiedExp extends Exp {
  require(exp isSubtype Bool)
  def variables: Seq[LocalVarDecl]
  def exp: Exp
  lazy val typ = Bool
}
object QuantifiedExp {
  def unapply(q: QuantifiedExp) = Some(q.variables, q.exp)
}

/** Universal quantification. */
case class Forall(variables: Seq[LocalVarDecl], triggers: Seq[Trigger], exp: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends QuantifiedExp

/** Existential quantification. */
case class Exists(variables: Seq[LocalVarDecl], exp: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends QuantifiedExp

/** A trigger for a universally quantified formula. */
case class Trigger(exps: Seq[Exp])(val pos: Position = NoPosition, val info: Info = NoInfo) extends Node with Positioned with Infoed {
  require(exps forall Consistency.validTrigger, s"The trigger { ${exps.mkString(", ")} } is not valid.")
}

// --- Variables, this, result

/** A local variable, special or not (used both for declarations and usages). */
sealed trait AbstractLocalVar extends Exp {
  def name: String
  lazy val mutable = true
}
object AbstractLocalVar {
  def unapply(l: AbstractLocalVar) = Some(l.name)
}

/** A normal local variable. */
case class LocalVar(name: String)(val typ: Type, val pos: Position = NoPosition, val info: Info = NoInfo) extends AbstractLocalVar with Lhs {
  require(Consistency.validUserDefinedIdentifier(name))
}

/** A special local variable for the result of a function. */
case class Result()(val typ: Type, val pos: Position = NoPosition, val info: Info = NoInfo) extends AbstractLocalVar {
  lazy val name = "result"
}

// --- Mathematical sequences

/**
 * Marker trait for all sequence-related expressions. Does not imply that the type of the
 * expression is `SeqType`.
 */
sealed trait SeqExp extends Exp

/** The empty sequence of a given element type. */
case class EmptySeq(elemTyp: Type)(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp {
  lazy val typ = SeqType(elemTyp)
}

/** An explicit, non-emtpy sequence. */
case class ExplicitSeq(elems: Seq[Exp])(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp {
  require(elems.length > 0)
  require(elems.tail.forall(e => (e isSubtype elems.head) && (elems.head isSubtype e)))
  lazy val typ = SeqType(elems.head.typ)
}

/** A range of integers from 'low' to 'high', not including 'high', but including 'low'. */
case class RangeSeq(low: Exp, high: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp {
  require((low isSubtype Int) && (high isSubtype Int))
  lazy val typ = SeqType(Int)
}

/** Appending two sequences of the same type. */
case class SeqAppend(left: Exp, right: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp with PrettyBinaryExpression {
  require((left isSubtype right) && (left isSubtype right))
  lazy val priority = 0
  lazy val fixity = Infix(LeftAssoc)
  lazy val op = "++"
  lazy val typ = left.typ
}

/** Access to an element of a sequence at a given index position (starting at 0). */
case class SeqIndex(s: Exp, idx: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp {
  require(s.typ.isInstanceOf[SeqType])
  require(idx isSubtype Int)
  lazy val typ = s.typ.asInstanceOf[SeqType].elementType
}

/** Take the first 'n' elements of the sequence 'seq'. */
case class SeqTake(s: Exp, n: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp {
  require(s.typ.isInstanceOf[SeqType])
  require(n isSubtype Int)
  lazy val typ = s.typ
}

/** Drop the last 'n' elements of the sequence 'seq'. */
case class SeqDrop(s: Exp, n: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp {
  require(s.typ.isInstanceOf[SeqType])
  require(n isSubtype Int)
  lazy val typ = s.typ
}

/** Is the element 'elem' contained in the sequence 'seq'? */
case class SeqContains(elem: Exp, s: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp with PrettyBinaryExpression {
  require(s.typ.isInstanceOf[SeqType])
  require(elem isSubtype s.typ.asInstanceOf[SeqType].elementType)
  lazy val priority = 0
  lazy val fixity = Infix(LeftAssoc)
  lazy val left: PrettyExpression = elem
  lazy val op = "in"
  lazy val right: PrettyExpression = s
  lazy val typ = Bool
}

/** The same sequence as 'seq', but with the element at index 'idx' replaced with 'elem'. */
case class SeqUpdate(s: Exp, idx: Exp, elem: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp {
  require(s.typ.isInstanceOf[SeqType])
  require(idx isSubtype Int)
  require(elem isSubtype s.typ.asInstanceOf[SeqType].elementType)
  lazy val typ = s.typ
}

/** The length of a sequence. */
case class SeqLength(s: Exp)(val pos: Position = NoPosition, val info: Info = NoInfo) extends SeqExp {
  require(s.typ.isInstanceOf[SeqType])
  lazy val typ = Int
}

// --- Common functionality

/** Common super trait for all kinds of literals. */
sealed trait Literal extends Exp

/**
 * A common class for concrete permissions.  The name AbstractConcretePerm is used because it is an abstract superclass for concrete permissions.
 */
sealed abstract class AbstractConcretePerm(val numerator: BigInt, val denominator: BigInt) extends PermExp

/** Common ancestor of Domain Function applications and Function applications. */
sealed trait FuncLikeApp extends Exp with Call with Typed {
  def func: FuncLike
  lazy val callee = func
  def typ = func.typ
}

/** Common superclass for domain functions with arbitrary parameters and return type, binary and unary operations are a special case. */
sealed trait AbstractDomainFuncApp extends FuncLikeApp {
  def func: AbstractDomainFunc
}

/**
 * A common class for equality and inequality comparisons.  Note that equality is defined for
 * all types, and therefore is not a domain function and does not belong to a domain.
 */
sealed abstract class EqualityCmp(val op: String) extends BinExp with PrettyBinaryExpression {
  require(left.typ == right.typ, s"expected the same typ, but got ${left.typ} and ${right.typ}")
  lazy val priority = 13
  lazy val fixity = Infix(NonAssoc)
  lazy val typ = Bool
}

/** Expressions with a unary or binary operator. */
sealed trait DomainOpExp extends AbstractDomainFuncApp {
  def func: Op
  def op = func.op
  def fixity = func.fixity
  def priority = func.priority
}

/** Binary expressions of any kind (whether or not they belong to a domain). */
sealed trait BinExp extends Exp with PrettyBinaryExpression {
  lazy val args = List(left, right)
  def left: Exp
  def right: Exp
}
object BinExp {
  def unapply(binExp: BinExp) = Some((binExp.left, binExp.right))
}

/** Unary expressions of any kind (whether or not they belong to a domain). */
sealed trait UnExp extends Exp {
  lazy val args = List(exp)
  def exp: Exp
}
object UnExp {
  def unapply(unExp: UnExp) = Some(unExp.exp)
}

/** Common superclass for binary expressions that belong to a domain (and thus have a domain operator). */
sealed abstract class DomainBinExp(val func: BinOp) extends BinExp with DomainOpExp
object DomainBinExp {
  def unapply(e: DomainBinExp) = Some((e.left, e.func, e.right))
}

/** Common superclass for unary expressions that belong to a domain (and thus have a domain operator). */
sealed abstract class DomainUnExp(val func: UnOp) extends PrettyUnaryExpression with DomainOpExp with UnExp

/** Expressions which can appear on the left hand side of an assignment */
sealed trait Lhs
