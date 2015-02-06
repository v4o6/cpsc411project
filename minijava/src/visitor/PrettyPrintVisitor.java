package visitor;

import java.io.PrintWriter;

import ast.AST;
import ast.And;
import ast.ArrayAssign;
import ast.ArrayLength;
import ast.ArrayLookup;
import ast.Assign;
import ast.Block;
import ast.BooleanLiteral;
import ast.BooleanType;
import ast.Call;
import ast.ClassDecl;
import ast.Identifier;
import ast.If;
import ast.IntArrayType;
import ast.IntegerLiteral;
import ast.IntegerType;
import ast.LessThan;
import ast.MainClass;
import ast.MethodDecl;
import ast.Minus;
import ast.NewArray;
import ast.NewObject;
import ast.NodeList;
import ast.Not;
import ast.ObjectType;
import ast.Plus;
import ast.Print;
import ast.Program;
import ast.This;
import ast.Times;
import ast.VarDecl;
import ast.While;
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
	public <T extends AST> Void visit(NodeList<T> ns) {
		for (int i = 0; i < ns.size(); i++) {
			ns.elementAt(i).accept(this);
		}
		return null;
	}

	@Override
	public Void visit(Program n) {
		n.mainClass.accept(this);
		n.classes.accept(this);
		return null;
	}

	@Override
	public Void visit(MainClass n) {
		out.print("class " + n.name + " {\n");
		out.indent();
		out.print("public static void main (String[] " + n.argName + ") {\n");
		out.indent();
		n.statement.accept(this);
		out.outdent();
		out.print("}\n");
		out.outdent();
		out.print("}\n");
		return null;
	}

	@Override
	public Void visit(ClassDecl n) {
		out.print("class " + n.name);
		if (n.superName != null)
			out.print(" extends " + n.superName);
		out.print(" {\n");
		out.indent();
		n.vars.accept(this);
		n.methods.accept(this);
		out.outdent();
		out.print("}\n");
		return null;
	}

	@Override
	public Void visit(VarDecl n) {
		n.type.accept(this);
		out.print(n.name);
		if (!n.kind.equals(VarDecl.Kind.FORMAL))
			out.print(";\n");
		return null;
	}

	@Override
	public Void visit(MethodDecl n) {
		out.print("public ");
		n.returnType.accept(this);
		out.print(n.name + "(");
		if (n.formals.size() != 0) {
			n.formals.elementAt(0).accept(this);
			for (int i = 1; i < n.formals.size(); i++) {
				out.print(", ");
				n.formals.elementAt(i).accept(this);
			}
		}
		out.print(") {\n");
		out.indent();
		n.vars.accept(this);
		n.statements.accept(this);
		out.print("return ");
		n.returnExp.accept(this);
		out.print(";\n");
		out.outdent();
		out.print("}\n");
		return null;
	}

	@Override
	public Void visit(IntArrayType n) {
		out.print("int [] ");
		return null;
	}
	
	@Override
	public Void visit(BooleanType n) {
		out.print("boolean ");
		return null;
	}

	@Override
	public Void visit(IntegerType n) {
		out.print("int ");
		return null;
	}

	@Override
	public Void visit(ObjectType n) {
		if (n.name != null)
			out.print(n.name + " ");
		else
			out.print("obj ");
		return null;
	}

	@Override
	public Void visit(Block n) {
		out.outdent();
		out.print("{\n");
		out.indent();
		n.statements.accept(this);
		out.outdent();
		out.print("}\n");
		out.indent();
		return null;
	}

	@Override
	public Void visit(If n) {
		out.print("if (");
		n.tst.accept(this);
		out.print(")\n");
		out.indent();
		n.thn.accept(this);
		out.outdent();
		out.print("else\n");
		out.indent();
		n.els.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(While n) {
		out.print("while (");
		n.tst.accept(this);
		out.print(")\n");
		out.indent();
		n.body.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(Print n) {
		out.print("System.out.println(");
		n.exp.accept(this);
		out.print(");\n");
		return null;
	}

	@Override
	public Void visit(Assign n) {
		out.print(n.name + " = ");
		n.value.accept(this);
		out.print(";\n");
		return null;
	}

	@Override
	public Void visit(ArrayAssign n) {
		out.print(n.name + "[");
		n.index.accept(this);
		out.print("] = ");
		n.value.accept(this);
		out.print(";\n");
		return null;
	}
	
	@Override
	public Void visit(And n) {
		out.print("(");
		n.e1.accept(this);
		out.print(" && ");
		n.e2.accept(this);
		out.print(")");
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
	public Void visit(ArrayLookup n) {
		n.array.accept(this);
		out.print("[");
		n.index.accept(this);
		out.print("]");
		return null;
	}

	@Override
	public Void visit(ArrayLength n) {
		n.array.accept(this);
		out.print(".length");
		return null;
	}

	@Override
	public Void visit(Call n) {
		n.receiver.accept(this);
		out.print("." + n.name + "(");
		if (n.rands.size() != 0) {
			n.rands.elementAt(0).accept(this);
			for (int i = 1; i < n.rands.size(); i++) {
				out.print(", ");
				n.rands.elementAt(i).accept(this);
			}
		}
		out.print(")");
		return null;
	}

	@Override
	public Void visit(IntegerLiteral n) {
		out.print("" + n.value);
		return null;
	}

	@Override
	public Void visit(BooleanLiteral n) {
		out.print("" + n.value);
		return null;
	}

	@Override
	public Void visit(Identifier n) {
		out.print(n.name);
		return null;
	}

	@Override
	public Void visit(This n) {
		out.print("this");
		return null;
	}

	@Override
	public Void visit(NewArray n) {
		out.print("new int[");
		n.size.accept(this);
		out.print("]");
		return null;
	}

	@Override
	public Void visit(NewObject n) {
		out.print("new " + n.typeName + "()");
		return null;
	}

	@Override
	public Void visit(Not n) {
		out.print("!");
		n.e.accept(this);
		return null;
	}

}
