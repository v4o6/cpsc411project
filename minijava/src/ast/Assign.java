package ast;

import visitor.Visitor;

public class Assign extends Statement {
	
	public final Identifier name;
	public final Expression value;
	
	public Assign(Identifier name, Expression value) {
		super();
		this.name = name;
		this.value = value;
	}

	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

}
