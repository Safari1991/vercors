package vct.col.rewrite;

import vct.col.ast.*;
import static hre.System.*;

public class ConstructorRewriter extends AbstractRewriter {

  public ConstructorRewriter(ProgramUnit source) {
    super(source);
  }

  public void visit(MethodInvokation e){
    if (e.getDefinition()==null){
      Warning("method invokation (%s) without definition",e.method);
    } else if (e.getDefinition().kind==Method.Kind.Constructor) {
      Fail("%s cannot deal with instantiation that is not an assignment at %s",getClass(),e.getOrigin());
    }
    super.visit(e);
  }
  
  public void visit(AssignmentStatement e){
    if (e.getExpression().isa(StandardOperator.Build) && (((OperatorExpression)e.getExpression()).getArg(0) instanceof ClassType)){
      OperatorExpression i=(OperatorExpression)e.getExpression();
      ASTNode s1=create.assignment(rewrite(e.getLocation()),create.expression(StandardOperator.New,rewrite(i.getType())));
      ASTNode args[]=i.getArguments();
      ASTNode rw_args[]=new ASTNode[args.length-1];
      for(int j=0;j<rw_args.length;j++){
        rw_args[j]=rewrite(args[j+1]);
      }
      MethodInvokation s2=create.invokation(rewrite(e.getLocation()),null,args[0].toString()+"_init",rw_args);
      if (i.get_before().size()>0) {
        s2.set_before(rewrite(i.get_before()));
      }
      if (i.get_after().size()>0) {
        s2.set_after(rewrite(i.get_after()));
      }
      copy_labels(s2,e.getExpression());
      result=create.block(s1,s2);
      return;
    }
    if (e.getExpression() instanceof MethodInvokation){
      MethodInvokation i=(MethodInvokation)e.getExpression();
      if (i.getDefinition().kind==Method.Kind.Constructor) {
        ASTNode s1=create.assignment(rewrite(e.getLocation()),create.expression(StandardOperator.New,rewrite(i.getType())));
        MethodInvokation s2=create.invokation(rewrite(e.getLocation()),null ,i.method+"_init",rewrite(i.getArgs()));
        if (i.get_before().size()>0) {
          s2.set_before(rewrite(i.get_before()));
        }
        if (i.get_after().size()>0) {
          s2.set_after(rewrite(i.get_after()));
        }
        copy_labels(s2,e.getExpression());
        result=create.block(s1,s2);
        return;
      }
    }
    super.visit(e);
  }
  
  public void visit(Method m){
    if (m.kind==Method.Kind.Constructor){
      String name=m.getName()+"_init";
      DeclarationStatement args[]=rewrite(m.getArgs());
      ASTNode body=rewrite(m.getBody());
      ContractBuilder cb=new ContractBuilder();
      Contract c=m.getContract();
      if (c!=null){
        rewrite(c,cb);
      }
      for(DeclarationStatement field:((ASTClass)m.getParent()).dynamicFields()){
        cb.requires(create.expression(
            StandardOperator.Perm,
            create.field_name(field.getName()),
            create.constant(100)
        ));
      }
      result=create.method_decl(create.primitive_type(PrimitiveType.Sort.Void), cb.getContract(), name, args, body);
    } else {
      super.visit(m);
    }
  }
}
