package analysis.implementation;

import ir.frame.Frame;
import ir.temp.Color;
import ir.temp.Temp;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import util.IndentingWriter;
import util.List;
import codegen.AssemProc;
import codegen.assem.A_MOVE.Status;
import codegen.assem.Instr;
import analysis.FlowGraph;
import analysis.InterferenceGraph;
import analysis.InterferenceGraph.Move;
import analysis.RegAlloc;
import analysis.util.MultiMap;
import analysis.util.graph.Node;
import junit.framework.Assert;

public class SimpleRegAlloc extends RegAlloc {

	private AssemProc proc;
	private String trace = "";
	private FlowGraph<Instr> fg;
	private InterferenceGraph ig;
	private Frame frame;
	private static final boolean generateDotFiles = false;

	private Map<Temp, Color> colorMap = new HashMap<Temp, Color>();
	private List<Temp> registers;
	private List<Color> colors;
	private List<Color> spillColors = List.empty();

	private List<Node<Temp>> simplifyWorklist = List.empty();
	private List<Node<Temp>> freezeWorklist = List.empty();
	private List<Node<Temp>> spillWorklist = List.empty();
	private MultiMap<Temp, Temp> coalesced = new MultiMap<Temp, Temp>();
	
	private List<Move> worklistMoves = List.empty();
//	private List<Move> coalescedMoves = List.empty();
//	private List<Move> constrainedMoves = List.empty();
//	private List<Move> frozenMoves = List.empty();
	private List<Move> waitlistMoves = List.empty();
	private MultiMap<Node<Temp>, Move> moveList = new MultiMap<Node<Temp>, Move>();

	/**
	 * List of *actual* spills.
	 */
	private List<Temp> spilled = List.empty();

	private int iteration;
	static private int incarnation = 0;

	@Override
	public void dump(IndentingWriter out) {
		out.println(trace);
		out.println("Coloring {");
		out.indent();
		for (Temp temp : colorMap.keySet()) {
			out.print(temp);
			out.print(" : ");
			out.println(colorMap.get(temp));
			out.indent();
			for (Node<Temp> interferes : ig.nodeFor(temp).succ()) {
				out.print(interferes);
				out.print(":");
				out.print(getColor(interferes));
				out.print(" ");
			}
			out.println();
			out.outdent();
		}
		out.outdent();
		out.println("}");
		out.print("Spilled");
		out.println(spilled);
	}

	public SimpleRegAlloc(AssemProc proc) {
		this(proc, 1);
	}

	public SimpleRegAlloc(AssemProc proc, int iteration) {
		this.proc = proc;
		this.iteration = iteration;
		this.trace += proc.toString();
		this.frame = proc.getFrame();
		this.registers = frame.registers();

		this.colors = List.empty();
		for (Temp reg : registers) 
			colors.add(reg.getColor());

		build();
		this.trace += "\n" + "Flow graph:\n" + fg.toString();
		this.trace += ig.toString();

		List<Temp> ordering = simplify();

		build(); // must rebuild the graph, since simplify should destroy it.
		color(ordering);
	}

	private void color(List<Temp> toColor) {
		if (toColor.isEmpty()) return;
		Temp t = toColor.head();
		boolean success;

		if (getColor(t) == null) {
			// Try to color using a register
			success = tryToColor(t, colors);

			if (!success) {
				// Try to spill using an existing spill slot.
				spilled.add(t);
				success = tryToColor(t, spillColors);
			}

			if (!success) {
				//Create a new spill slot and use that.
				SpillColor color = new SpillColor(frame);
				spillColors = List.cons(color, spillColors);
				setColor(t, color);
			}
		}
		color(toColor.tail());
	}

	private boolean tryToColor(Temp t, List<Color> colors) {
		for (Color color : colors) {
			if (isColorOK(ig.nodeFor(t), color)) {
				setColor(t, color);
				return true;
			}
		}
		return false;
	}

	private boolean isColorOK(Node<Temp> node, Color color) {
		for (Node<Temp> interferes : node.succ()) 
			if (color.equals(getColor(interferes))) return false;
		return true;
	}

	/**
	 * Start by building the interference graph for the procedure body.
	 */
	private void build() {
		this.fg = FlowGraph.build(proc.getBody());
		this.ig = fg.getInterferenceGraph();
		this.ig.name = proc.getLabel().toString() + " round " + iteration;
	}
		
	/**
	 * Returns a List of Temp's (a stack really) which suggest the order
	 * in which nodes should be assigned colors.
	 */
	private List<Temp> simplify() {
		List<Temp> ordering = List.empty();
		int simplified = 0;

		// Initialize moveLists
		for (Move move : ig.moves()) {			
			moveList.add(move.src, move);
			moveList.add(move.dst, move);
			worklistMoves.add(move);
			move.instr.currentSet = Status.WORKLIST;
		}

		// Construct worklists
		for (Node<Temp> node : ig.nodes()) {
			if (!isColored(node)) {
				if (node.outDegree() >= registers.size())
					spillWorklist.add(node);
				else if (!moveList.get(node).isEmpty())
					freezeWorklist.add(node);
				else
					simplifyWorklist.add(node);
			}
		}
		
		while (!(simplifyWorklist.isEmpty() && worklistMoves.isEmpty() && freezeWorklist.isEmpty() && spillWorklist.isEmpty())) {
			while (!simplifyWorklist.isEmpty()) {
				// remove a node from the graph
				Node<Temp> node = simplifyWorklist.head();
				ordering.add(node.wrappee());
				simplifyWorklist = simplifyWorklist.delete(node);
				for (Node<Temp> adj : node.adj()) {
					// decrement the degree of nodes adjacent to the node just removed
					ig.rmEdge(node, adj);
					ig.rmEdge(adj, node);
					// if the adjacent node can now be simplified, remove it from the spillWorklist
					if (adj.outDegree() == (registers.size() - 1)) {
						if (spillWorklist.contains(adj)) {
							spillWorklist = spillWorklist.delete(adj);
							if (!moveList.get(adj).isEmpty())
								freezeWorklist = List.cons(adj, freezeWorklist);
							else
								simplifyWorklist = List.cons(adj, simplifyWorklist);
							enableMoves(adj);
						}
					}
				}
				if (generateDotFiles) {
					File out = new File("simplify-" + incarnation + "-" + simplified + ".dot");
					try {
						PrintStream outb = new PrintStream(out);
						outb.print(ig.dotString(registers.size(), null));
						outb.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				simplified++;
			}
			while (!worklistMoves.isEmpty()) {
				// coalesce nodes
				Move move = worklistMoves.head();
				Node<Temp> u;
				Node<Temp> v;
				if (isColored(ig.nodeFor(move.src.wrappee()))) {
					u = ig.nodeFor(move.dst.wrappee());
					v = ig.nodeFor(move.src.wrappee());
				} else {
					u = ig.nodeFor(move.src.wrappee());
					v = ig.nodeFor(move.dst.wrappee());
				}
				// if u and v contain at least one predefined register, it will be located in v.

				if (u.equals(v)) {
//					coalescedMoves.add(move);
					move.instr.currentSet = Status.COALESCED;
					addSimplifyWorklist(v);
					moveList.remove(move.src, move);
					moveList.remove(move.dst, move);
				}
				else if (constrained(u, v)) {
//					constrainedMoves.add(move);
					move.instr.currentSet = Status.CONSTRAINED;
					addSimplifyWorklist(u);
					addSimplifyWorklist(v);
					moveList.remove(move.src, move);
					moveList.remove(move.dst, move);
				}
				else if (safeToCoalesce(u, v)) {
//					coalescedMoves.add(move);
					move.instr.currentSet = Status.COALESCED;
					combine(u, v, ordering);
					addSimplifyWorklist(v);
					moveList.remove(move.src, move);
					moveList.remove(move.dst, move);
				}
				else {
					waitlistMoves = List.cons(move, waitlistMoves);
					move.instr.currentSet = Status.WAITING;
				}
				// remove the current move from the worklist
				worklistMoves = worklistMoves.delete(move);
			}

			if (!simplifyWorklist.isEmpty())
				continue;
			
			if (!freezeWorklist.isEmpty()) {
				Node<Temp> toFreeze = null;
				double spillCost = 0;
				for (Node<Temp> n : freezeWorklist) {
					if (ig.spillCost(n) > spillCost) {
						toFreeze = n;
						spillCost = ig.spillCost(n);
					}
				}
				
				freezeWorklist = freezeWorklist.delete(toFreeze);
				simplifyWorklist = List.cons(toFreeze, simplifyWorklist);
				freezeMoves(toFreeze);
				continue;
			}

			if (!spillWorklist.isEmpty()) {
				Node<Temp> toSpill = null;
				double spillCost = Double.MAX_VALUE;
				for (Node<Temp> n : spillWorklist) {
					if (ig.spillCost(n) < spillCost) {
						toSpill = n;
						spillCost = ig.spillCost(n);
					}
				}
				
				spillWorklist = spillWorklist.delete(toSpill);
				simplifyWorklist = List.cons(toSpill, simplifyWorklist);
				freezeMoves(toSpill);
			}	
		}
		incarnation++;
		return ordering;
	}

	private void enableMoves(Node<Temp> n) {
		for (Move move : moveList.get(n)) {
			if (move.instr.currentSet == Status.WAITING) {
				waitlistMoves = waitlistMoves.delete(move);
				move.instr.currentSet = Status.WORKLIST;
				worklistMoves = List.cons(move, worklistMoves);
			}
		}
		for (Node<Temp> adj : n.adj()) {
			for (Move move : moveList.get(adj)) {
				if (move.instr.currentSet == Status.WAITING) {
					waitlistMoves = waitlistMoves.delete(move);
					move.instr.currentSet = Status.WORKLIST;
					worklistMoves = List.cons(move, worklistMoves);
				}
			}	
		}
	}
	
	private void addSimplifyWorklist(Node<Temp> n) {
		if (!isColored(n) && moveList.get(n).isEmpty() && n.outDegree() < registers.size()) {
			freezeWorklist = freezeWorklist.delete(n);
			simplifyWorklist = List.cons(n, simplifyWorklist);
		}
	}

	private boolean constrained(Node<Temp> u, Node<Temp> v) {
		return u.adj(v) || (isColored(u) && isColored(v));
	}

	private boolean safeToCoalesce(Node<Temp> u, Node<Temp> v) {
		boolean test = true;
		// George strategy
		for (Node<Temp> adj : u.adj()) {
			if (adj.outDegree() >= registers.size() && !adj.adj(v)) {
				test = false;
				break;
			}
		}
		if (test) return true;
		
		// Briggs strategy
		int k = 0;
		for (Node<Temp> adj : u.adj().union(v.adj())) {
			if (adj.outDegree() >= registers.size())
				k++;
		}
		return k < registers.size();
	}

	private void combine(Node<Temp> u, Node<Temp> v, List<Temp> ordering) {
		if (freezeWorklist.contains(u))
			freezeWorklist = freezeWorklist.delete(u);
		else if (spillWorklist.contains(u))
			spillWorklist = spillWorklist.delete(u);
		ig.makeAlias(u.wrappee(), v);
		coalesced.add(v.wrappee(), u.wrappee());
		// if u was previously the target of coalescing 
		for (Temp alias : coalesced.get(u.wrappee())) {
			ig.makeAlias(alias, v);
			coalesced.remove(u.wrappee(), alias);
			coalesced.add(v.wrappee(), alias);
		}
			
		for (Move move : moveList.get(u)) {
			moveList.add(v, move);
			if (move.instr.currentSet == Status.WAITING) {
				waitlistMoves = waitlistMoves.delete(move);
				move.instr.currentSet = Status.WORKLIST;
				worklistMoves = List.cons(move, worklistMoves);
			}
		}
		
		for (Node<Temp> adj : u.adj()) {
			ig.rmEdge(u, adj);
			ig.rmEdge(adj, u);
			ig.addEdge(v, adj);
			ig.addEdge(adj, v);
		}
		if (v.outDegree() == (registers.size() - 1) && spillWorklist.contains(v)) {
			spillWorklist = spillWorklist.delete(v);
			freezeWorklist = List.cons(v, freezeWorklist);
		}
		if (v.outDegree() >= registers.size() && freezeWorklist.contains(v)) {
			freezeWorklist = freezeWorklist.delete(v);
			spillWorklist = List.cons(v, spillWorklist);
		}
		
		if (!isColored(v))
			ordering.add(u.wrappee());
		else {
			Color coalesceColor = v.wrappee().getColor();
			setColor(u.wrappee(), coalesceColor);
			for (Temp alias : coalesced.get(u.wrappee()))
				setColor(alias, coalesceColor);
		}
	}

	private void freezeMoves(Node<Temp> u) {
		for (Move move : moveList.get(u)) {
			Node<Temp> v;
			if (ig.nodeFor(move.dst.wrappee()).equals(ig.nodeFor(u.wrappee())))
				v = ig.nodeFor(move.src.wrappee());
			else
				v = ig.nodeFor(move.dst.wrappee());

			// remove move from consideration during coalescing
			if (move.instr.currentSet == Status.WORKLIST)
				worklistMoves = worklistMoves.delete(move);
			else if (move.instr.currentSet == Status.WAITING)
				waitlistMoves = waitlistMoves.delete(move);
			moveList.remove(move.src, move);
			moveList.remove(move.dst, move);
//			frozenMoves.add(move);
			move.instr.currentSet = Status.FROZEN;

			if (freezeWorklist.contains(v) && moveList.get(v).isEmpty()) {
				freezeWorklist = freezeWorklist.delete(v);
				simplifyWorklist = List.cons(v, simplifyWorklist);
			}
		}
	}
	
	private boolean isColored(Node<Temp> node) {
		return getColor(node)!=null;
	}

	private Color getColor(Node<Temp> node) {
		return getColor(node.wrappee());
	}

	private void setColor(Temp t, Color color) {
		Assert.assertNull(getColor(t));
		colorMap.put(t, color);
	}

	/**
	 * Gets the color of a Temp based on the "hypothetical" coloring we are
	 * exploring now.
	 */
	private Color getColor(Temp temp) {
		Color color = temp.getColor();
		if (color != null) // it is precolored!
			return color;
		color = colorMap.get(temp);
		return color;
	}

	public List<Temp> getSpilled() {
		return spilled;
	}

	public Map<Temp, Color> getColorMap() {
		return colorMap;
	}

	public String getTrace() {
		return this.toString();
	}
}
