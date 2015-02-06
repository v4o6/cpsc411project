package ir.tree;import util.DefaultIndentable;import util.List;import ir.interp.Word;import ir.interp.X86_64SimFrame;import ir.temp.Temp;abstract public class IRExp extends DefaultIndentable implements IRNode {	private final List<Temp> empty = List.empty();	// The book code provides these methods on each Exp node, these are used to aid	// in implementing the conversion to canonical trees.		/**	 * Retrieve a list of the direct subexpression of this node.	 */	abstract public List<IRExp> kids();		/**	 * Create a new Exp node by copying this node and replacing it's	 * direct subexpressions.	 * <p>	 * It is assumed that the number and ordering of "kids" is the same as the	 * that returned by the kids() method. Any non-expression items are kept as	 * is.	 */	abstract public IRExp build(List<IRExp> kids);		/**	 * To simulate IR execution. This method assumes that the IR is	 * in almost canonical form. In particular, it is assumed that the	 * their are no ISeq expressions in the IRcode (the main reason for	 * this assumption is that it is next to impossible to simulate JUMP's	 * into and out of expressions.	 */	abstract public Word interp(X86_64SimFrame env);	/** 	 * To simulate IR execution. This method is implemented by IR tree's	 * that can be used as target (left hand side) of a move instruction.	 * <p>	 * Only MEM and TEMP nodes (at present) should be used as such so most	 * classes don't need to implement this.	 * <p>	 * It assigns the value to the location represented by the receiver	 * IRExp. The env parameter is provided because the reciever IRExp	 * may contain subtrees that need to be interpreted.	 */	public void set(Word value, X86_64SimFrame env) {		throw new Error("This IR "+this+" is not legal as the LHS of a MOVE.");	}		public boolean isCONST(int i) {		return false;	}	public boolean mentions(Temp t) {		for (IRExp k : kids()) {			if (k.mentions(t)) return true;		}		return false;	}	public boolean mentionsMemOrCall() {		for (IRExp k : kids()) {			if (k.mentionsMemOrCall()) return true;		}		return false;	}	public List<Temp> use() {		List<Temp> answer = empty;		for (IRExp k : kids()) {			List<Temp> kuse = k.use();			for (Temp t : kuse) {				if (!answer.contains(t))					answer = List.cons(t, answer);			}		}		return answer;	}}