package ast;

import java.util.ArrayList;
import java.util.List;

import visitor.Visitor;

public class FormalList extends AST {
	
	private List<Parameter> params;
	
	public FormalList() {
		this.params = new ArrayList<Parameter>();
	}
	
	public FormalList(List<Parameter> params) {
		this.params = params;
	}

	public void add(Parameter p) {
		this.params.add(p);
	}
	public int size() {
		return params.size();
	}

	public Parameter elementAt(int i) {
		return params.get(i);
	}
	
	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

}
