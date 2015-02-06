package ast;

import visitor.Visitor;

public class MainClass extends ClassDecl {

	public final String argName;
	public final Statement statement;

	public MainClass(String className, String argName, Statement statement) {
		super(className, null, new NodeList<VarDecl>(null), new NodeList<MethodDecl>(null));
		this.argName = argName;
		this.statement = statement;
	}

	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

}
