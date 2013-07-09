package org.openimag.mediaeval.searchandhyperlinking;

public class Result {
	private String program;
	private float startTime;
	private float endTime;
	private float jumpInPoint;
	private float confidenceScore;
	
	public Result(String program, float startTime, float endTime, float jumpInPoint,
			float confidenceScore) {
		super();
		this.program = program;
		this.startTime = startTime;
		this.endTime = endTime;
		this.jumpInPoint = jumpInPoint;
		this.confidenceScore = confidenceScore;
	}
	public String getProgram() {
		return program;
	}
	public void setProgram(String program) {
		this.program = program;
	}
	public float getStartTime() {
		return startTime;
	}
	public void setStartTime(float startTime) {
		this.startTime = startTime;
	}
	public float getEndTime() {
		return endTime;
	}
	public void setEndTime(float endTime) {
		this.endTime = endTime;
	}
	public float getJumpInPoint() {
		return jumpInPoint;
	}
	public void setJumpInPoint(float jumpInPoint) {
		this.jumpInPoint = jumpInPoint;
	}
	public float getConfidenceScore() {
		return confidenceScore;
	}
	public void setConfidenceScore(float confidenceScore) {
		this.confidenceScore = confidenceScore;
	}
	
	public String toString() {
		return program + "\n" +
			   startTime + "\n" +
			   endTime + "\n" +
			   jumpInPoint + "\n" +
			   confidenceScore + "\n";
	}
}
