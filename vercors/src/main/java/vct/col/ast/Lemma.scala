package vct.col.ast

import vct.col.util.VisitorHelper

/**
 * This class represents magic wand proofs, a.k.a. create blocks.
 * 
 * @author sccblom, whmoortwijn
 * @param block The block representing the lemma (for the magic wand proof).
 */
case class Lemma(val block:BlockStatement) extends ASTNode with VisitorHelper {
  override def accept_simple[T,A](m:ASTMapping1[T,A], arg:A) = m.map(this, arg)
  override def accept_simple[T](v:ASTVisitor[T]) = handle_standard(() => v.visit(this))
  override def accept_simple[T](m:ASTMapping[T]) = handle_standard(() => m.map(this))
}