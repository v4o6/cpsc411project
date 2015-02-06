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


public class NodeCountVisitor implements Visitor<Integer> {
	
	private int nodeCount;
	
	public NodeCountVisitor() {
		this.nodeCount = 0;
	}

	///////////// Visitor methods /////////////////////////////////////////
	
	@Override
	public <T extends AST> Integer visit(NodeList<T> ns) {
		for (int i = 0; i < ns.size(); i++) {
			nodeCount++;
			ns.elementAt(i).accept(this);
		}
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(Program n) {
		nodeCount++;
		n.statements.accept(this);
		n.print.accept(this);
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(IntegerType n) {
		nodeCount++;
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(BooleanType n) {
		nodeCount++;
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(UnknownType n) {
		nodeCount++;
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(Print n) {
		nodeCount++;
		n.exp.accept(this);		
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(Assign n) {
		nodeCount++;
		n.value.accept(this);
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(LessThan n) {
		nodeCount++;
		n.e1.accept(this);
		n.e2.accept(this);
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(Conditional n) {
		nodeCount++;
		n.e1.accept(this);
		n.e2.accept(this);
		n.e3.accept(this);
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(Plus n) {
		nodeCount++;
		n.e1.accept(this);
		n.e2.accept(this);
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(Minus n) {
		nodeCount++;
		n.e1.accept(this);
		n.e2.accept(this);
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(Times n) {
		nodeCount++;
		n.e1.accept(this);
		n.e2.accept(this);
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(IntegerLiteral n) {
		nodeCount++;
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(IdentifierExp n) {
		nodeCount++;
		return new Integer(nodeCount);
	}

	@Override
	public Integer visit(Not n) {
		nodeCount++;
		return new Integer(nodeCount);
	}

}
