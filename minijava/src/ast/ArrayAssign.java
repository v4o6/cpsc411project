package ast;

import visitor.Visitor;

public class ArrayAssign extends Statement {

	public final Identifier name;
	public final Expression index;
	public final Expression value;
	
	public ArrayAssign(Identifier name, Expression index, Expression value) {
		super();
		this.name = name;
		this.index = index;
		this.value = value;
	}

	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

}
