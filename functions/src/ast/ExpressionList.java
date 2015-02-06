package ast;

import java.util.ArrayList;
import java.util.List;

import visitor.Visitor;

public class ExpressionList extends AST {
	
	private List<Expression> args;
	
	public ExpressionList() {
		this.args = new ArrayList<Expression>();
	}
	
	public ExpressionList(List<Expression> params) {
		this.args = params;
	}

	public void add(Expression p) {
		this.args.add(p);
	}
	public int size() {
		return args.size();
	}

	public Expression elementAt(int i) {
		return args.get(i);
	}
	
	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

}
