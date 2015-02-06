package ast;

import visitor.Visitor;

public class Identifier extends Expression {

	public final String name;
	
	public Identifier(String name) {
		super();
		this.name = name;
	}

	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

	@Override
	public boolean isLValue() {
		return true;
	}

}
