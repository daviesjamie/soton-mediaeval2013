package com.github.wcerfgba.adhocstructures;

public interface IdentifyRequestHandler<OUTER, INNER> {

	public INNER handleIdentifyRequest(OUTER outer);
}
