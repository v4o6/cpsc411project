package ast;

import visitor.Visitor;

public class This extends Expression {

	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

	@Override
	public boolean isLValue() {
		return false;
	}

}
