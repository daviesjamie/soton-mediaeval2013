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
	public float getLength() {
		return endTime - startTime;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(confidenceScore);
		result = prime * result + Float.floatToIntBits(endTime);
		result = prime * result + Float.floatToIntBits(jumpInPoint);
		result = prime * result + ((program == null) ? 0 : program.hashCode());
		result = prime * result + Float.floatToIntBits(startTime);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Result other = (Result) obj;
		if (Float.floatToIntBits(confidenceScore) != Float
				.floatToIntBits(other.confidenceScore))
			return false;
		if (Float.floatToIntBits(endTime) != Float
				.floatToIntBits(other.endTime))
			return false;
		if (Float.floatToIntBits(jumpInPoint) != Float
				.floatToIntBits(other.jumpInPoint))
			return false;
		if (program == null) {
			if (other.program != null)
				return false;
		} else if (!program.equals(other.program))
			return false;
		if (Float.floatToIntBits(startTime) != Float
				.floatToIntBits(other.startTime))
			return false;
		return true;
	}

	public float distanceTo(Result arg0) {
		return (float) Math.sqrt(Math.pow(arg0.getStartTime() - getStartTime(), 2) + 
						 		 Math.pow(arg0.getEndTime() - getEndTime(), 2));
	}
}
