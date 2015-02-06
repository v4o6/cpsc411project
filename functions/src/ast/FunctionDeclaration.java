package ast;

import util.ImpTable;
import visitor.Visitor;

public class FunctionDeclaration extends Statement {
	
	public final Type type;
	public final String name;
	public final FormalList params;
	public final NodeList<Statement> statements;
	public final Expression value;

	public ImpTable<Type> variables;
	
	public FunctionDeclaration(Type type, String name, FormalList params, NodeList<Statement> statements, Expression value) {
		super();
		this.type = type;
		this.name = name;
		this.params = params;
		this.statements = statements;
		this.value = value;
	}

	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

}


