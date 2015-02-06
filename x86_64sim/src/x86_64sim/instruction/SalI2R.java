package x86_64sim.instruction;

import x86_64sim.State;

public class SalI2R extends Instruction {
	long source;
	String target;
	
	public SalI2R(String source, String target) {
		this.source = Long.parseLong(source);
		this.target = target;
	}
	
	@Override
	public void execute(State state) {
		long value = state.getReg(target) << source;
		if (state.beVerbose)
			System.out.println(target + " <- " + value);
		state.setReg(target, value);
	}

	@Override
	public String toString() {
		return "\tsalq\t$" + source + ", " + target;
	}

}
