package at.pegelhub.connector.tstp.service;

import java.util.List;

import at.pegelhub.lib.model.Measurement;

public interface TstpBinaryService {

	/**
	 * Decodes binary data as specified in the tstp protocol description.
	 *
	 * @param toDecode the binary that was recieved from the tstp server
	 * @return the decoded binary as  List of Measurements
	 */
	List<Measurement> decode(byte[] toDecode);

	/**
	 * Encodes binary data as specified in the tstp protocol description.
	 *
	 * @param toEncode
	 * @return
	 */
	byte[] encode(List<Measurement> toEncode);
}
