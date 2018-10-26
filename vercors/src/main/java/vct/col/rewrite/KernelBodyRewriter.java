package vct.col.rewrite;

import java.util.ArrayList;

import vct.col.ast.ASTNode;
import vct.col.ast.BlockStatement;
import vct.col.ast.Contract;
import vct.col.ast.ContractBuilder;
import vct.col.ast.DeclarationStatement;
import vct.col.ast.Method;
import vct.col.ast.MethodInvokation;
import vct.col.ast.ProgramUnit;
import vct.col.ast.StandardOperator;
import vct.col.ast.Type;
import vct.col.ast.PrimitiveSort;
import vct.col.util.ASTUtils;

class KernelBodyRewriter extends AbstractRewriter {

  public KernelBodyRewriter(ProgramUnit source) {
    super(source);
  }
  
  /*public void visit(NameExpression n) {
	  //Kind kind = n.getKind();
	  String name=n.getName();
	  System.out.println("KernelBodyRewriter-visit(MethodInvokation): "+name);
	  if(name.equals("threadIdx")) {
		  result=plus(mult(create.local_name("opencl_gid"),sub(create.local_name("opencl_gsize"), create.local_name("opencl_gsize"))),
		           create.local_name("opencl_lid")); 
	      return;
	  }else {
	        Fail("bad dimension: %s",name);
	  }
	  
  }*/
  
  @Override
  public void visit(MethodInvokation e){
	  //if (e.method.equals("barrier")|| e.method.equals("func") || e.method.equals("example3")) 
		  System.out.println("KernelBodyRewriter-visit(MethodInvokation): "+e.method);
    ASTNode arg;
    switch(e.method){
    case "get_global_id" :
      arg=e.getArg(0);
      if (arg.isConstant(0)) {
        //result=create.local_name("opencl_tid");
        result=plus(mult(create.local_name("opencl_gid"),create.local_name("opencl_gsize")), 
            create.local_name("opencl_lid"));
        return;
      } else {
        Fail("bad dimension: %s",arg);
      }
      
    case "get_local_id" : // Added by Mohsen
        arg=e.getArg(0);
        if (arg.isConstant(0)) {
        		//result = create.field_decl("opencl_lid",create.primitive_type(PrimitiveSort.Integer));
        	//result = create.identifier("opencl_lid");
        		//result=create.local_name("opencl_lid");
        		result=plus(mult(create.local_name("opencl_gid"),sub(create.local_name("opencl_gsize"), create.local_name("opencl_gsize"))),
        		           create.local_name("opencl_lid")); 
        			//result=plus(create.local_name("opencl_lid"), sub(create.local_name("opencl_gid"),create.local_name("opencl_gid")));
        		return;
        }else {
            Fail("bad dimension: %s",arg);
        }
        
   /* case "barrier" : // Added by Mohsen
    		System.out.println("KernelBodyRewriter-visit(MethodInvokation e)-barrier");
  	    Contract c=null;
  	    ArrayList<String> invs=new ArrayList<String>();
  	    result=create.barrier("barrier",c,invs,null);*/
        
    /*case "barrier":
    		arg=e.getArg(0);
    			if (arg.isName("CLK_GLOBAL_MEM_FENCE")) {
    				System.out.println("barrier");
    				return;
    			}else {
    	            Fail("bad barrier argument: %s",arg);
    	        }*/
        
     /*case "barrier":
    		System.out.println("KernelBodyRewriter-visit(MethodInvokation)-case barrier");
    		System.out.println("KernelBodyRewriter-visit(MethodInvokation)-case barrier-currentContractBuilder: "+currentContractBuilder);
    	   //if (mc!=null){
    		 //  System.out.println("SpecificationCollector-visit(MethodInvokation)-barrier-inside if");
    		  // rewrite(mc,currentContractBuilder);
    	   //}
    	   Contract c = MethodInvokation.getContract("barrier");
    	   System.out.println("KernelBodyRewriter-visit(MethodInvokation)-case barrier-currentContractBuilder.getContract(): "+c);
    	   //currentContractBuilder=null;
    	   ArrayList<String> invs = new ArrayList<String>();
    	   System.out.println("KernelBodyRewriter-visit(MethodInvokation)-invs: "+invs);
    	   result=create.barrier("barrier",c,invs,null);
    	   return;*/
    	   
      default:
        super.visit(e);
    }
  }
  
  @Override
  public void visit(Method m){
	  System.out.println("KernelBodyRewrite-visit(Method): "+m);
    ArrayList<DeclarationStatement> decls=new ArrayList<DeclarationStatement>();
    DeclarationStatement inner_decl=create.field_decl(
        "opencl_lid",create.primitive_type(PrimitiveSort.Integer),
        create.expression(StandardOperator.RangeSeq,
            create.constant(0),create.local_name("opencl_gsize"))); 
    DeclarationStatement outer_decl=create.field_decl(
        "opencl_gid",create.primitive_type(PrimitiveSort.Integer), 
        create.expression(StandardOperator.RangeSeq,
            create.constant(0),create.local_name("opencl_gcount")));
    ContractBuilder icb=new ContractBuilder(); // thread contract 
    ContractBuilder gcb=new ContractBuilder(); // group contract
    gcb.requires(create.constant(true));
    ContractBuilder kcb=new ContractBuilder(); // kernel contract
    kcb.given(create.field_decl("opencl_gcount",create.primitive_type(PrimitiveSort.Integer)));
    kcb.given(create.field_decl("opencl_gsize",create.primitive_type(PrimitiveSort.Integer))); 
    Type returns=rewrite(m.getReturnType());
    for(DeclarationStatement d:m.getArgs()){
      decls.add(rewrite(d));
    }
    Contract c=m.getContract();
    rewrite(c,icb); 
    	gcb.appendInvariant(rewrite(c.invariant));
    	kcb.appendInvariant(rewrite(c.invariant));
    Contract ic=rewrite(m.getContract());
    for(ASTNode clause:ASTUtils.conjuncts(ic.pre_condition, StandardOperator.Star)){
      ASTNode group=create.starall(
          create.expression(StandardOperator.Member,
              create.local_name("opencl_lid"),
              create.expression(StandardOperator.RangeSeq,
                  create.constant(0),create.local_name("opencl_gsize"))
          ),
          clause,
          create.field_decl("opencl_lid",create.primitive_type(PrimitiveSort.Integer))); 
      gcb.requires(group);
      kcb.requires(create.starall(
          create.expression(StandardOperator.Member,
              create.local_name("opencl_gid"),
              create.expression(StandardOperator.RangeSeq,
                  create.constant(0),create.local_name("opencl_gcount"))
          ),
          group,
          create.field_decl("opencl_gid",create.primitive_type(PrimitiveSort.Integer))));
      System.out.println("KernelBodyRewrite-visit(Method)-clause: "+clause);
      System.out.println("KernelBodyRewrite-visit(Method)-group: "+group);
    }
    BlockStatement body=(BlockStatement)rewrite(m.getBody());
    //body.prepend(create.field_decl("opencl_tid",create.primitive_type(Sort.Integer),
    //    plus(mult(create.local_name("opencl_gid"),create.local_name("opencl_gsize")),create.local_name("opencl_lid"))));
    //icb.given(create.field_decl("opencl_tid",create.primitive_type(Sort.Integer)));
    //icb.requires(create.expression(StandardOperator.EQ,
    //    create.local_name("opencl_tid"),plus(mult(create.local_name("opencl_gid"),create.local_name("opencl_gsize")),create.local_name("opencl_lid"))));
    DeclarationStatement[] iters=new DeclarationStatement[]{inner_decl};
    body=create.block(create.region(null,create.parallel_block("group_block", icb.getContract(),iters, body)));
    iters=new DeclarationStatement[]{outer_decl};
    body=create.block(create.region(null,create.parallel_block("kernel_block", gcb.getContract(),iters, body)));
    result=create.method_decl(returns, kcb.getContract(), m.name(), decls, body);
    
    System.out.println("KernelBodyRewrite-visit(Method)-kcb.getContract(): "+kcb.getContract());
    System.out.println("KernelBodyRewrite-visit(Method)-gcb.getContract(): "+gcb.getContract());
    System.out.println("KernelBodyRewrite-visit(Method)-icb.getContract(): "+icb.getContract());
  }
}