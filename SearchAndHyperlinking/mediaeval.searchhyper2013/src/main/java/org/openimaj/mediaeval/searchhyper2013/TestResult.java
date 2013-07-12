package org.openimaj.mediaeval.searchhyper2013;

public class TestResult {
	private String queryID;
	private String program;
	private Float  start;
	private Float  end;
	
	public TestResult(String queryID, String program, Float start, Float end) {
		super();
		this.queryID = queryID;
		this.program = program;
		this.start = start;
		this.end = end;
	}
	
	public String getQueryID() {
		return queryID;
	}
	public void setQueryID(String queryID) {
		this.queryID = queryID;
	}
	public String getProgram() {
		return program;
	}
	public void setProgram(String program) {
		this.program = program;
	}
	public Float getStart() {
		return start;
	}
	public void setStart(Float start) {
		this.start = start;
	}
	public Float getEnd() {
		return end;
	}
	public void setEnd(Float end) {
		this.end = end;
	}
	
	
}
