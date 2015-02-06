package ast;

import java.util.ArrayList;
import java.util.List;

import visitor.Visitor;





public class NodeList<T extends AST> extends AST {
	
	private List<T> nodes;
	
	public NodeList() {
		this.nodes = new ArrayList<T>();
	}

	public NodeList(List<T> nodes) {
		this.nodes = nodes;
	}

	public void add(T t) {
		this.nodes.add(t);
	}
	public int size() {
		return nodes.size();
	}

	public T elementAt(int i) {
		return nodes.get(i);
	}

	@Override
	public <R> R accept(Visitor<R> v) {
		return v.visit(this);
	}

}
