package visitor;

import java.io.PrintWriter;

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
import util.IndentingWriter;



/**
 * This is an adaptation of the PrettyPrintVisitor from the textbook
 * online material, but updated to work with the "modernized" 
 * Visitor and our own versions of the AST classes.
 * <p>
 * This version is also cleaned up to actually produce *properly* indented
 * output.
 * 
 * @author kdvolder
 */
public class PrettyPrintVisitor implements Visitor<Void> {

	/**
	 * Where to send out.print output.
	 */
	private IndentingWriter out;
	
	public PrettyPrintVisitor(PrintWriter out) {
		this.out = new IndentingWriter(out);
	}
	
	///////////// Visitor methods /////////////////////////////////////////

	@Override
	public Void visit(Program n) {
		n.statements.accept(this);
		n.print.accept(this);
		return null;
	}

	@Override
	public Void visit(BooleanType n) {
		out.print("boolean ");
		return null;
	}

	@Override
	public Void visit(UnknownType n) {
		out.print("unknown ");
		return null;
	}

	@Override
	public Void visit(IntegerType n) {
		out.print("int ");
		return null;
	}

  	@Override
	public Void visit(Conditional n) {
		out.print("( ");
		n.e1.accept(this);
		out.print(" ? ");
		n.e2.accept(this);
		out.print(" : ");
		n.e3.accept(this);
		out.print(" )");
		return null;
	}

	@Override
	public Void visit(Print n) {
		out.print("print ");
		n.exp.accept(this);
		out.println();
		return null;
	}

	@Override
	public Void visit(Assign n) {
		out.print(n.name + " = ");
		n.value.accept(this);
		out.println(";");
		return null;
	}

	@Override
	public Void visit(LessThan n) {
		out.print("(");
		n.e1.accept(this);
		out.print(" < ");
		n.e2.accept(this);
		out.print(")");
		return null;
	}

	@Override
	public Void visit(Plus n) {
		out.print("(");
		n.e1.accept(this);
		out.print(" + ");
		n.e2.accept(this);
		out.print(")");
		return null;
	}

	@Override
	public Void visit(Minus n) {
		out.print("(");
		n.e1.accept(this);
		out.print(" - ");
		n.e2.accept(this);
		out.print(")");
		return null;
	}

	@Override
	public Void visit(Times n) {
		out.print("(");
		n.e1.accept(this);
		out.print(" * ");
		n.e2.accept(this);
		out.print(")");
		return null;
	}

	@Override
	public Void visit(IntegerLiteral n) {
		out.print(""+n.value);
		return null;
	}

	@Override
	public Void visit(IdentifierExp n) {
		out.print(n.name);
		return null;
	}

	@Override
	public Void visit(Not n) {
		out.print("!");
		n.e.accept(this);
		return null;
	}

	@Override
	public <T extends AST> Void visit(NodeList<T> nodes) {
		for (int i = 0; i < nodes.size(); i++) {
			nodes.elementAt(i).accept(this);
		}
		return null;
	}

	@Override
	public Void visit(FunctionDeclaration n) {
		n.type.accept(this);
		out.print(n.name);
		out.print("(");
		n.params.accept(this);
		out.print(") { ");
		n.statements.accept(this);
		out.print("return ");
		n.value.accept(this);
		out.print("; }\n ");
		return null;
	}

	@Override
	public Void visit(FormalList n) {
		if (n.size() > 0) {
			n.elementAt(0).accept(this);
			for (int i = 1; i < n.size(); i++) {
				out.print(", ");
				n.elementAt(i).accept(this);
			}
		}
		return null;
	}

	@Override
	public Void visit(Parameter n) {
		n.type.accept(this);
		out.print(" " + n.name);
		return null;
	}

	@Override
	public Void visit(FunctionCall n) {
		out.print(n.identifier + "(");
		n.args.accept(this);
		out.print(")");
		return null;
	}

	@Override
	public Void visit(ExpressionList n) {
		if (n.size() > 0) {
			n.elementAt(0).accept(this);
			for (int i = 1; i < n.size(); i++) {
				out.print(", ");
				n.elementAt(i).accept(this);
			}
		}
		return null;
	}

	@Override
	public Void visit(LambdaType n) {
		out.print("lambda ");
		return null;
	}
}
