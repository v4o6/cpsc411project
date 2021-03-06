package ast;

import visitor.Visitor;

public class Minus extends Expression {

	public final Expression e1;
	public final Expression e2;

	public Minus(Expression e1, Expression e2) {
		super();
		this.e1 = e1;
		this.e2 = e2;
	}

	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

	@Override
	public boolean isLValue() {
		return false;
	}

}
