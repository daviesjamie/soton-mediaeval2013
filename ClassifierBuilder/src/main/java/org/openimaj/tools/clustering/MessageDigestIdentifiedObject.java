package org.openimaj.tools.clustering;

import java.io.ByteArrayOutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

import org.openimaj.data.identity.Identifiable;
import org.openimaj.data.identity.IdentifiableObject;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;

/**
 * Automatically identifies objects using a specified MessageDigest.
 * 
 * @author John Preston (jlp1g11@ecs.soton.ac.uk)
 *
 * @param <DIGEST>
 * @param <OBJECT>
 */
public class MessageDigestIdentifiedObject<DIGEST extends MessageDigest, OBJECT> extends IdentifiableObject<OBJECT> {

	String md;
	
	public MessageDigestIdentifiedObject(DIGEST digest, OBJECT object) {
		// Appease the compiler...
		super(null, object);
		
		Kryo kryo = new Kryo();
		
		ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
		DigestOutputStream digestOut = new DigestOutputStream(byteOut, digest);
		Output output = new Output(digestOut);

		kryo.writeObject(output, object);
		
		md = digestOut.getMessageDigest().toString();
	}

	@Override
	public String getID() {
		return md;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Identifiable))
			return false;

		return md.equals(((Identifiable) obj).getID());
	}
}
