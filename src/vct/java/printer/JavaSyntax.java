// -*- tab-width:2 ; indent-tabs-mode:nil -*-

package vct.java.printer;

import hre.ast.TrackingOutput;
import vct.col.ast.ASTNode;
import vct.col.rewrite.Parenthesize;
import hre.HREError;
import vct.util.Syntax;
import vct.util.VerCorsSyntax;
import static vct.col.ast.StandardOperator.*;
import static vct.col.ast.PrimitiveType.Sort.*;
import static vct.col.ast.ASTReserved.*;

/**
 * Create a Syntax object for Java.
 */
public class JavaSyntax extends Syntax {

  public final JavaDialect dialect;
  public JavaSyntax(String language,JavaDialect dialect) {
    super(language);
    this.dialect=dialect;
  }

  private static Syntax JavaSyntax;
  
  
  public synchronized static Syntax getJava(){
    if (JavaSyntax==null){
      Syntax syntax=new JavaSyntax("Java",null);
      setCommon(syntax);
      JavaSyntax=syntax;
    }
    return JavaSyntax;
  }
  
  private static JavaSyntax JavaVerCorsSyntax;
  private static JavaSyntax JavaVeriFastSyntax;
  
  public synchronized static JavaSyntax getJava(JavaDialect dialect){
    switch(dialect){
    case JavaVerCors:
      if (JavaVerCorsSyntax==null){
        JavaSyntax syntax=new JavaSyntax("Java + JML",dialect);
        setCommon(syntax);
        VerCorsSyntax.add(syntax);
        syntax.addLeftFix(Exp,"^^",125);
        syntax.addLeftFix(StructSelect,".",-1);
        syntax.addOperator(HoarePredicate,-1,"/*{","}*/");
        syntax.addOperator(RangeSeq,-1,"[",",",")");

        syntax.addOperator(Member,45,"","in","");
        syntax.addFunction(CurrentPerm,"perm");
        syntax.addFunction(Scale,"scale");
        syntax.addFunction(Drop,"drop");
        syntax.addFunction(Take,"take");
        
        syntax.addOperator(Unfolding,140,"unfolding","in","");
        JavaVerCorsSyntax=syntax;
        
      }
      return JavaVerCorsSyntax;
    case JavaVeriFast:
      if (JavaVeriFastSyntax==null){
        JavaSyntax syntax=new JavaSyntax("Java + VeriFast",dialect);
        setCommon(syntax);
        syntax.addLeftFix(Star,"&*&",40);
        syntax.addPrefix(BindOutput,"?",666);     
        JavaVeriFastSyntax=syntax;  
      }
      return JavaVeriFastSyntax;
    default:
      throw new HREError("Java specification language dialect %s not supported",dialect);
    }
  }
  
  private static  void setCommon(Syntax syntax){
    syntax.addOperator(NewArray,-1,"new ","[","]");
    syntax.addOperator(Subscript,145,"","[","]"); // TODO: check if relative order to Select is OK!
    syntax.addOperator(Cast,145,"((",")",")");
    
    // Java Operators  Precedence
    // 14 postfix  expr++ expr--
    syntax.addPostfix(PostIncr,"++",140);
    syntax.addPostfix(PostDecr,"--",140);
    // 13 unary   ++expr --expr +expr -expr ~ !
    syntax.addPrefix(BitNot, "~", 130);
    syntax.addPrefix(Not, "!", 130);
    syntax.addPrefix(UMinus, "-", 130);
    syntax.addPrefix(UPlus, "+", 130);
    syntax.addPrefix(PreIncr, "++", 130);
    syntax.addPrefix(PreDecr, "--", 130);
    // 12 multiplicative  * / %
    syntax.addLeftFix(Mult,"*",120);
    syntax.addLeftFix(Div,"/",120);
    syntax.addLeftFix(Mod,"%",120);
    // 11 additive  + -
    syntax.addLeftFix(Plus,"+",110);
    syntax.addLeftFix(Minus,"-",110);
    // 10 shift   << >> >>>
    syntax.addInfix(LeftShift,"<<", 100);
    syntax.addInfix(RightShift,">>", 100);
    syntax.addInfix(UnsignedRightShift,">>", 100);
    //  9 relational  < > <= >= instanceof
    syntax.addInfix(LT,"<",90);
    syntax.addInfix(LTE,"<=",90);
    syntax.addInfix(GT,">",90);
    syntax.addInfix(GTE,">=",90);
    syntax.addInfix(Instance," instanceof ",90);
    //  8 equality  == !=
    syntax.addInfix(EQ,"==",80);
    syntax.addInfix(NEQ,"!=",80);
    //  7 bitwise AND   &
    syntax.addInfix(BitAnd,"&",70);
    //  6 bitwise exclusive OR  ^
    syntax.addInfix(BitXor,"^",60);
    //  5 bitwise inclusive OR  |
    syntax.addInfix(BitOr,"|",50);
    //  4 logical AND   &&
    syntax.addLeftFix(And,"&&",40);
    //  3 logical OR  ||
    syntax.addLeftFix(Or,"||",30);
    //Send annotation -- sending permission to verify parallel loops 
    syntax.addOperator(Send,20,"send","to",",",""); //DRB
    //Recv annotation -- receiving permission to verify parallel loops 
    syntax.addOperator(Recv,20,"recv","to",",","");
    
    //  2 ternary   ? :    
    syntax.addOperator(ITE,20,"","?",":","");
    //  1 assignment  = += -= *= /= %= &= ^= |= <<= >>= >>>=
        
    syntax.addRightFix(Assign,"=",10);
    syntax.addRightFix(AddAssign,"+=",10);
    syntax.addRightFix(SubAssign,"-=",10);
    syntax.addRightFix(MulAssign,"*= ",10);
    syntax.addRightFix(DivAssign,"/=",10);
    syntax.addRightFix(RemAssign,"%=",10);
    syntax.addRightFix(AndAssign,"&=",10);
    syntax.addRightFix(XorAssign,"^=",10);
    syntax.addRightFix(OrAssign,"|=",10);
    syntax.addRightFix(ShlAssign,"<<=",10);
    syntax.addRightFix(ShrAssign,">>=",10);
    syntax.addRightFix(SShrAssign,">>>=",10);
    
    syntax.addPrimitiveType(Double,"double");
    syntax.addPrimitiveType(Integer,"int");
    syntax.addPrimitiveType(Long,"long");
    syntax.addPrimitiveType(Void,"void");
    syntax.addPrimitiveType(Boolean,"boolean");
    syntax.addPrimitiveType(Char,"char");
    syntax.addPrimitiveType(Float,"float");
    syntax.addPrimitiveType(UInteger,"/*unsigned*/ int");
    syntax.addPrimitiveType(ULong,"/*unsigned*/ long");
    syntax.addPrimitiveType(UShort,"/*unsigned*/ short");
    syntax.addPrimitiveType(Short,"short");
    syntax.addPrimitiveType(Process,"process");

    syntax.addReserved(Public,"public");
    syntax.addReserved(Private,"private");
    syntax.addReserved(Static,"static");
    syntax.addReserved(This,"this");
    syntax.addReserved(Null,"null");
    syntax.addReserved(Super,"super");
    syntax.addReserved(Final,"final");
  }

  @Override
  public JavaPrinter print(TrackingOutput out, ASTNode n) {
    JavaPrinter p=new JavaPrinter(out,dialect);
    if (n!=null) {
      ASTNode nn=new Parenthesize(this).rewrite(n);
      nn.accept(p);
    }
    return p;
  } 
}


/*
Java Operators 	Precedence
14 postfix 	expr++ expr--
13 unary 	++expr --expr +expr -expr ~ !
12 multiplicative 	* / %
11 additive 	+ -
10 shift 	<< >> >>>
 9 relational 	< > <= >= instanceof
 8 equality 	== !=
 7 bitwise AND 	&
 6 bitwise exclusive OR 	^
 5 bitwise inclusive OR 	|
 4 logical AND 	&&
 3 logical OR 	||
 2 ternary 	? :
 1 assignment 	= += -= *= /= %= &= ^= |= <<= >>= >>>=
*/

