package ast;

import visitor.Visitor;

public class FunctionCall extends Expression {
	
	public final String identifier;
	public final ExpressionList args;
	
	public FunctionCall(String identifier, ExpressionList args) {
		super();
		this.identifier = identifier;
		this.args = args;
	}

	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

}


