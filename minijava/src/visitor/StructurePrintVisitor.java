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
 * This prints the structure of an AST, showing its hierarchical relationships.
 * <p>
 * This version is also cleaned up to actually produce *properly* indented
 * output.
 * 
 * @author norm
 */
public class StructurePrintVisitor implements Visitor<Void> {

	/**
	 * Where to send out.print output.
	 */
	private IndentingWriter out;

	public StructurePrintVisitor(PrintWriter out) {
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
		out.println("Program");
		out.indent();
		n.mainClass.accept(this);
		n.classes.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(MainClass n) {
		out.println("MainClass " + n.name);
		out.indent();
		out.println("VarDecl " + n.argName);
		n.statement.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(ClassDecl n) {
		out.println("ClassDecl " + n.name);
		out.indent();
		if (n.superName != null)
			out.println("ObjectType " + n.superName);
		n.vars.accept(this);
		n.methods.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(VarDecl n) {
		out.println("VarDecl " + n.name);
		out.indent();
		n.type.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(MethodDecl n) {
		out.println("MethodDecl " + n.name);
		out.indent();
		n.returnType.accept(this);
		n.formals.accept(this);
		n.vars.accept(this);
		n.statements.accept(this);
		n.returnExp.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(IntArrayType n) {
		out.println("IntArrayType");
		return null;
	}

	@Override
	public Void visit(BooleanType n) {
		out.println("BooleanType");
		return null;
	}

	@Override
	public Void visit(IntegerType n) {
		out.println("IntegerType");
		return null;
	}

	@Override
	public Void visit(ObjectType n) {
		out.println("ObjectType " + n.name);
		return null;
	}

	@Override
	public Void visit(Block n) {
		out.println("Block");
		out.indent();
		n.statements.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(If n) {
		out.println("If");
		out.indent();
		n.tst.accept(this);
		n.thn.accept(this);
		n.els.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(While n) {
		out.println("While");
		out.indent();
		n.tst.accept(this);
		n.body.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(Print n) {
		out.println("Print");
		out.indent();
		n.exp.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(Assign n) {
		out.println("Assign " + n.name);
		out.indent();
		n.value.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(ArrayAssign n) {
		out.println("ArrayAssign " + n.name);
		out.indent();
		n.index.accept(this);
		n.value.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(And n) {
		out.println("And");
		out.indent();
		n.e1.accept(this);
		n.e2.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(LessThan n) {
		out.println("LessThan");
		out.indent();
		n.e1.accept(this);
		n.e2.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(Plus n) {
		out.println("Plus");
		out.indent();
		n.e1.accept(this);
		n.e2.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(Minus n) {
		out.println("Minus");
		out.indent();
		n.e1.accept(this);
		n.e2.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(Times n) {
		out.println("Times");
		out.indent();
		n.e1.accept(this);
		n.e2.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(ArrayLookup n) {
		out.println("ArrayLookup");
		out.indent();
		n.array.accept(this);
		n.index.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(ArrayLength n) {
		out.println("ArrayLength");
		out.indent();
		n.array.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(Call n) {
		out.println("Call " + n.name);
		out.indent();
		n.receiver.accept(this);
		n.rands.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(IntegerLiteral n) {
		out.println("IntegerLiteral " + n.value);
		return null;
	}

	@Override
	public Void visit(BooleanLiteral n) {
		out.println("BooleanLiteral " + n.value);
		return null;
	}

	@Override
	public Void visit(Identifier n) {
		out.println("IdentifierExp " + n.name);
		return null;
	}

	@Override
	public Void visit(This n) {
		out.println("This");
		return null;
	}

	@Override
	public Void visit(NewArray n) {
		out.println("NewArray");
		out.indent();
		n.size.accept(this);
		out.outdent();
		return null;
	}

	@Override
	public Void visit(NewObject n) {
		out.println("NewObject " + n.typeName);
		return null;
	}

	@Override
	public Void visit(Not n) {
		out.println("Not");
		out.indent();
		n.e.accept(this);
		out.outdent();
		return null;
	}

}
