package org.openimaj.mediaeval.searchhyper2013;

/**
 * Represents a time within a file. This exists because we need to move between 
 * the <seconds> and <minutes>.<seconds> formats.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public class Time {
	private Float seconds;
	
	public Time(Float time, Boolean seconds) {
		if (seconds) {
			this.seconds = time;
		} else {
			this.seconds = MStoS(time);
		}
	}
	
	public Time(Float seconds) {
		this(seconds, true);
	}
	
	public Float seconds() {
		return seconds;
	}
	
	public Float minutesSeconds() {
		return StoMS(seconds);
	}
	
	private static Float MStoS(Float minutesSeconds) {
		Float seconds = 0f;
		
		float minutes = (float) Math.floor(minutesSeconds);
		
		seconds += minutes * 60;
		seconds += (minutesSeconds - minutes) * 100;
		
		return seconds;
	}
	
	private static Float StoMS(Float seconds) {
		Float minutesSeconds = 0f;
		
		float minutes = seconds % 60;
		
		minutesSeconds += (seconds - (minutes * 60)) / 100;
		minutesSeconds += minutes;
		
		return minutesSeconds;
	}
	
	public static float HMStoS(String hmsString) {
		String[] parts = hmsString.split(":");
		
		float secs = Float.parseFloat(parts[2]);
		secs += Float.parseFloat(parts[1]) * 60;
		secs += Float.parseFloat(parts[0]) * 60 * 60;
		
		return secs;
	}
}
