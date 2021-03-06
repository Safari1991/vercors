package hre.util;

/**
 * This class contains a test report for a verification run.
 * 
 * @author Stefan Blom
 *
 */
public class TestReport {

  public static enum Verdict { Pass, Fail, Inconclusive, Error };
  
  private Verdict verdict=Verdict.Pass;
  private Exception e;
  
  public void setVerdict(Verdict verdict){
    this.verdict=verdict;
  }
  
  public Verdict getVerdict(){
    return verdict;
  }
  
  public void setException(Exception e){
    this.verdict=Verdict.Error;
    this.e=e;
  }

  public Exception getException(){
    return e;
  }
  
  public void fail(String format, Object ... args) {
    if (verdict!=Verdict.Error){
      verdict=Verdict.Fail;
    }
    System.err.printf("FAIL: "+format+"%n", args);
  }

}
