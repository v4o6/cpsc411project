package visitor;

import ast.AST;
import ast.Assign;
import ast.BooleanType;
import ast.Conditional;
import ast.IdentifierExp;
import ast.IntegerLiteral;
import ast.IntegerType;
import ast.LessThan;
import ast.Minus;
import ast.NodeList;
import ast.Not;
import ast.Plus;
import ast.Print;
import ast.Program;
import ast.Times;
import ast.UnknownType;


public class MaxDepthVisitor implements Visitor<Integer> {

	///////////// Visitor methods /////////////////////////////////////////
	
	@Override
	public <T extends AST> Integer visit(NodeList<T> ns) {
		int h = 0;
		for (int i = 0; i < ns.size(); i++) {
			int tmp = ns.elementAt(i).accept(this);
			if (tmp > h) h = tmp;
		}
		return new Integer(h);
	}

	@Override
	public Integer visit(Program n) {
		int h1 = n.statements.accept(this);
		int h2 = n.print.accept(this);
		return new Integer(Math.max(h1, h2) + 1);
	}

	@Override
	public Integer visit(IntegerType n) {
		return new Integer(1);
	}

	@Override
	public Integer visit(BooleanType n) {
		return new Integer(1);
	}

	@Override
	public Integer visit(UnknownType n) {
		return new Integer(1);
	}

	@Override
	public Integer visit(Print n) {		
		return new Integer(n.exp.accept(this) + 1);
	}

	@Override
	public Integer visit(Assign n) {
		return new Integer(n.value.accept(this) + 1);
	}

	@Override
	public Integer visit(LessThan n) {
		int h1 = n.e1.accept(this);
		int h2 = n.e2.accept(this);
		return new Integer(Math.max(h1, h2) + 1);
	}

	@Override
	public Integer visit(Conditional n) {
		int h1 = n.e1.accept(this);
		int h2 = n.e2.accept(this);
		int h3 = n.e3.accept(this);
		return new Integer(Math.max(h1, Math.max(h2, h3)) + 1);
	}

	@Override
	public Integer visit(Plus n) {
		int h1 = n.e1.accept(this);
		int h2 = n.e2.accept(this);
		return new Integer(Math.max(h1, h2) + 1);
	}

	@Override
	public Integer visit(Minus n) {
		int h1 = n.e1.accept(this);
		int h2 = n.e2.accept(this);
		return new Integer(Math.max(h1, h2) + 1);
	}

	@Override
	public Integer visit(Times n) {
		int h1 = n.e1.accept(this);
		int h2 = n.e2.accept(this);
		return new Integer(Math.max(h1, h2) + 1);
	}

	@Override
	public Integer visit(IntegerLiteral n) {
		return new Integer(1);
	}

	@Override
	public Integer visit(IdentifierExp n) {
		return new Integer(1);
	}

	@Override
	public Integer visit(Not n) {
		return new Integer(n.e.accept(this) + 1);
	}
	
}
