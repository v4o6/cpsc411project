package analysis.implementation;

import ir.temp.Temp;

import java.util.Collections;
import java.util.HashMap;

import codegen.assem.Instr;
import util.List;
import analysis.FlowGraph;
import analysis.Liveness;
import analysis.util.ActiveSet;
import analysis.util.MultiMap;
import analysis.util.graph.Node;


public class LivenessImplementation<N> extends Liveness<N> {

	private HashMap<Node<Instr>, ActiveSet<Temp>> liveOuts;
	private HashMap<Node<Instr>, ActiveSet<Temp>> liveIns;
	
	public LivenessImplementation(FlowGraph<N> graph) {
		super(graph);

		liveOuts = new HashMap<Node<Instr>, ActiveSet<Temp>>();
		liveIns = new HashMap<Node<Instr>, ActiveSet<Temp>>();
		for (Node<Instr> n : ((AssemFlowGraph) graph).nodes()) {
			ActiveSet<Temp> out = new ActiveSet<Temp>();
			liveOuts.put(n, out);
			ActiveSet<Temp> use = new ActiveSet<Temp>();
			use.addAll(((Instr) n.wrappee()).use());
			ActiveSet<Temp> tmp = out.remove(((Instr) n.wrappee()).def());
			liveIns.put(n, ActiveSet.union(use, tmp));
		}
		for (Node<Instr> n : ((AssemFlowGraph) graph).nodes()) {
			ActiveSet<Temp> out = liveOuts.get(n);
			for (Node<Instr> s : n.succ()) {
				out.addAll(liveIns.get(s));
			}
		}
	}

	@Override
	public List<Temp> liveOut(Node<N> node) {
		if (liveOuts.containsKey(node))
			return liveOuts.get(node).getElements();
//			return liveIns.get(node).getElements();
		else
			throw new Error("Node not in graph.");
	}

	private List<Temp> liveIn(Node<N> node) {
		if (liveIns.containsKey(node))
			return liveIns.get(node).getElements();
		else
			throw new Error("Node not in graph.");
	}

	private String shortList(List<Temp> l) {
		java.util.List<String> reall = new java.util.ArrayList<String>();
		for (Temp t : l) {
			reall.add(t.toString());
		}
		Collections.sort(reall);
		StringBuffer sb = new StringBuffer();
		sb.append(reall);
		return sb.toString();
	}
	
	private String dotLabel(Node<N> n) {
		StringBuffer sb = new StringBuffer();
		sb.append(shortList(liveIn(n))); sb.append("\\n"); 
		sb.append(n); sb.append(": "); sb.append(n.wrappee()); sb.append("\\n");
		sb.append(shortList(liveOut(n))); 
		return sb.toString();
	}

	private double fontSize() {
		return (Math.max(30, Math.sqrt(Math.sqrt(g.nodes().size() + 1)) * g.nodes().size() * 1.2));
	}

	private double lineWidth() {
		return (Math.max(3.0, Math.sqrt(g.nodes().size() + 1) * 1.4));
	}
	private double arrowSize() {
		return Math.max(2.0, Math.sqrt(Math.sqrt(g.nodes().size() + 1)));
	}
	@Override
	public String dotString(String name) {
		StringBuffer out = new StringBuffer();
		out.append("digraph \"Flow graph\" {\n");
		out.append("labelloc=\"t\";\n");
		out.append("fontsize=" + fontSize() + ";\n");
		out.append("label=\"" + name + "\";\n");

		out.append("  graph [size=\"6.5, 9\", ratio=fill];\n");
		for (Node<N> n : g.nodes()) {
			out.append("  \"" + dotLabel(n) + "\" [fontsize=" + fontSize());
			out.append(", style=\"setlinewidth(" + lineWidth() + ")\", color=" + (g.isMove(n) ? "green" : "blue"));
			out.append("]\n");
		}
		for (Node<N> n : g.nodes()) {
			for (Node<N> o : n.succ()) {
				out.append("  \"" + dotLabel(n) + "\" -> \"" + dotLabel(o) + "\" [arrowhead = normal, arrowsize=" + arrowSize() + ", style=\"setlinewidth(" + lineWidth() + ")\"];\n");
			}
		}

		out.append("}\n");
		return out.toString();
	}

}
