package org.immregistries.iis.kernal.logic.shlink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionUtil {


	/**
	 * Applies raw RFC1951 INFLATE decompression to a byte array.
	 * This method expects a raw DEFLATE-compressed byte array without zlib headers or footers.
	 *
	 * @param input The compressed byte array.
	 * @return The decompressed byte array.
	 * @throws DataFormatException If the input data format is invalid.
	 */
	public static byte[] inflate(byte[] input) throws DataFormatException {
		// Create a new Inflater instance with the "nowrap" parameter set to true.
		// This indicates that the input is a raw DEFLATE stream, not a zlib stream.
		Inflater inflater = new Inflater(true);
		inflater.setInput(input);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length * 2); // Initial capacity for efficiency

		try (outputStream) {
			byte[] buffer = new byte[1024];
			while (!inflater.finished()) {
				int count = inflater.inflate(buffer);
				outputStream.write(buffer, 0, count);
			}
		} catch (IOException e) {
			// Handle the exception appropriately
			e.printStackTrace();
		} finally {
			inflater.end(); // Deallocates native INFLATER resources.
		}

		return outputStream.toByteArray();
	}

	/**
	 * Applies raw RFC1951 DEFLATE compression to a byte array.
	 * This method does not include zlib or gzip headers/footers.
	 *
	 * @param input The uncompressed byte array.
	 * @return The compressed byte array.
	 */
	public static byte[] deflate(byte[] input) {
		// Create a new Deflater instance with the desired compression level.
		// A level of 9 represents the best compression.
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true); // The `true` parameter signifies "nowrap" for raw DEFLATE.
		deflater.setInput(input);
		deflater.finish(); // Indicates that no more input data will be provided.

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream(input.length);
		try (outputStream) {
			byte[] buffer = new byte[1024];

			while (!deflater.finished()) {
				int count = deflater.deflate(buffer);
				outputStream.write(buffer, 0, count);
			}
		} catch (java.io.IOException e) {
			// Handle the exception appropriately
			e.printStackTrace();
		} finally {
			deflater.end(); // Deallocates the native DEFLATER resources.
		}
		return outputStream.toByteArray();
	}


	/**
	 * Removes all useless whitespace from a JSON string.
	 * This method uses the Jackson library to parse and then write the JSON
	 * in a compact format, preserving whitespace within string values.
	 *
	 * @param jsonString The input JSON string.
	 * @return A minified JSON string with no useless whitespace.
	 * @throws IOException If the JSON string is invalid.
	 */
	public static String minifyJson(String jsonString) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(jsonString);
		return objectMapper.writeValueAsString(jsonNode);
	}
}
