package ast;

import visitor.Visitor;

public class Parameter extends AST {

	public final Type type;
	public final String name;
	
	public Parameter(Type type, String name) {
		super();
		this.type = type;
		this.name = name;
	}
	
	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

}
