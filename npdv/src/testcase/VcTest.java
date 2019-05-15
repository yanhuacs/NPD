package testcase;
public class VcTest {
    public static void main(String[] args) {
        ErrorReporter rep = new ErrorReporter();
        CheckerAssign ca = new CheckerAssign(rep);
        AssignExpr asgn = new AssignExpr();
        ca.check(asgn);
    }
}
class CheckerAssign {
    private ErrorReporter reporter;

    public CheckerAssign(ErrorReporter reporter) {
        this.reporter = reporter;
    }
    public void check(AST ast) {
        AssignExpr e = ((AssignExpr) ast);
        System.out.printf("%d", e.type.isErrorType()); //bug
    }
}
class ErrorReporter {

}
class Type {
    boolean isErrorType() {
        return false;
    }
    int x;
}
class AST {
    Type type;
    AST() {
        type = null;
    }
}
class AssignExpr extends AST {
    public AssignExpr() {
    }
}
