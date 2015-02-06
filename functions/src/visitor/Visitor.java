package visitor;

import ast.AST;
import ast.Assign;
import ast.BooleanType;
import ast.Conditional;
import ast.ExpressionList;
import ast.FormalList;
import ast.FunctionCall;
import ast.FunctionDeclaration;
import ast.IdentifierExp;
import ast.IntegerLiteral;
import ast.IntegerType;
import ast.LambdaType;
import ast.LessThan;
import ast.Minus;
import ast.NodeList;
import ast.Not;
import ast.Parameter;
import ast.Plus;
import ast.Print;
import ast.Program;
import ast.Times;
import ast.UnknownType;

/**
 * A modernized version of the Visitor interface, adapted from the textbook's
 * version.
 * <p>
 * Changes: this visitor allows you to return something as a result. 
 * The "something" can be of any particular type, so the Visitor 
 * uses Java generics to express this.
 * 
 * @author kdvolder
 */
public interface Visitor<R> {

	//Lists
	public <T extends AST> R visit(NodeList<T> ns);
	
	//Declarations
	public R visit(Program n);
	
	//Types
	public R visit(IntegerType n);
	public R visit(BooleanType n);
	public R visit(UnknownType n);
	public R visit(LambdaType n);
	
	//Statements
	public R visit(Print n);
	public R visit(Assign n);
	public R visit(FunctionDeclaration functionDeclaration);
	public R visit(FormalList formalList);
	public R visit(Parameter parameter);
	
	//Expressions
	public R visit(LessThan n);
	public R visit(Conditional n);
	public R visit(Plus n);
	public R visit(Minus n);
	public R visit(Times n);
	public R visit(IntegerLiteral n);
	public R visit(IdentifierExp n);
	public R visit(Not not);
	public R visit(FunctionCall functionCall);
	public R visit(ExpressionList expressionList);

}
