package postilion.realtime.genericinterface.translate.util;

import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.sdk.eventrecorder.EventRecorder;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Esta clase permite hacer registro de mensajes en un archivo de texto, para
 * hacer trazabilidad dentro del c�digo
 * 
 * @author Javier Flores
 *
 */
public class Logger {

	public static final String filePath = "C:\\temp\\logs\\IsoTranslateB24.txt";

	/**
	 * Escribe un mensaje en un archivo de texto.
	 * 
	 * @param msg a escribir en el archivo.
	 */
	public static void logLine(String msg) {

		StringBuilder sb = new StringBuilder();
		sb.append(Constants.OPEN_BRAKET).append(new Date().toString()).append(Constants.CLOSE_BRAKET);
		sb.append(msg);
		sb.append(Constants.LINE_BREAK);
		BufferedWriter bf = null;
		try {
			bf = new BufferedWriter(new FileWriter(filePath, true));
			bf.append(sb.toString());
		} catch (IOException e) {
			EventRecorder.recordEvent(new TryCatchException(new String[] { "Unknown", Logger.class.getName(),
					"Method: [logLine]", Utils.getStringMessageException(e), "N/A" }));
			EventRecorder.recordEvent(e);
		} finally {
			if (bf != null)
				try {
					bf.close();
				} catch (IOException e) {
					EventRecorder.recordEvent(new TryCatchException(new String[] { "Unknown", Logger.class.getName(),
							"Method: [logLine]", Utils.getStringMessageException(e), "N/A" }));
					EventRecorder.recordEvent(e);
				}
		}

	}

	/**
	 * 
	 * Define constantes utiles para la clase
	 * 
	 * @author Cristian Cardozo
	 *
	 */
	public class Constants {

		public final static String OPEN_BRAKET = "[";
		public final static String CLOSE_BRAKET = "]";
		public final static String LINE_BREAK = "\n";

	}
}
