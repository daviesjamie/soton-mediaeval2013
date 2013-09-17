package org.openimaj.mediaeval.searchhyper2013.util;

/**
 * Methods for moving between the <seconds> and <minutes>.<seconds> formats.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 */
public abstract class Time {

	public static Float MStoS(Float minutesSeconds) {
		Float seconds = 0f;
		
		float minutes = (float) Math.floor(minutesSeconds);
		
		seconds += minutes * 60;
		seconds += (minutesSeconds - minutes) * 100;
		
		return seconds;
	}
	
	public static Float StoMS(Float seconds) {
		Float minutesSeconds = 0f;
		
		float minutes = (float) Math.floor(seconds / 60);
		
		minutesSeconds += Math.round((seconds - (minutes * 60))) / 100;
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

	public static float MStoS(String msString) {
		return MStoS(Float.parseFloat(msString));
	}
}
