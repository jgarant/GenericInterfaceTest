package postilion.realtime.genericinterface.translate.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import postilion.realtime.library.common.model.ConfigAllTransaction;
import postilion.realtime.library.common.util.constants.General;
import postilion.realtime.genericinterface.GenericInterface;
import postilion.realtime.genericinterface.Parameters;

import postilion.realtime.genericinterface.eventrecorder.events.SQLExceptionEvent;
import postilion.realtime.genericinterface.eventrecorder.events.TryCatchException;
import postilion.realtime.genericinterface.translate.ConstructFieldMessage;
import postilion.realtime.genericinterface.translate.MessageTranslator;
import postilion.realtime.genericinterface.translate.bitmap.Base24Ath;
import postilion.realtime.genericinterface.translate.util.udp.Client;
import postilion.realtime.genericinterface.translate.validations.Validation;
import postilion.realtime.genericinterface.translate.validations.Validation.ErrorMessages;
import postilion.realtime.sdk.eventrecorder.EventRecorder;
import postilion.realtime.sdk.ipc.SecurityManager;
import postilion.realtime.sdk.ipc.XEncryptionKeyError;
import postilion.realtime.sdk.message.bitmap.Iso8583;
import postilion.realtime.sdk.message.bitmap.Iso8583.AccountType;
import postilion.realtime.sdk.message.bitmap.Iso8583.RspCode;
import postilion.realtime.sdk.message.bitmap.Iso8583Post;
import postilion.realtime.sdk.message.bitmap.Track2;
import postilion.realtime.sdk.util.TimedHashtable;
import postilion.realtime.sdk.util.XPostilion;

public class Utils {

	private TimedHashtable sourceTranToTmHashtableB24 = null;
	private Map<String, HashMap<String, ConfigAllTransaction>> structureContent = new HashMap<>();
	private Map<String, ConfigAllTransaction> structureMap = new HashMap<>();
	private Client udpClient = null;
	public String nameInterface = "";
	private Parameters params;

	public Utils(Parameters params) {
		this.sourceTranToTmHashtableB24 = params.getSourceTranToTmHashtableB24();
		this.structureContent = params.getStructureContent();
		this.structureMap = params.getStructureMap();
		this.udpClient = params.getUdpClient();
		this.nameInterface = params.getNameInterface();
		this.params = params;
	}

	static long tEnd;
	static long time;

	/** Instancia de SecurityManager **/
	public static SecurityManager securityManager = null;

	static {
		try {
			securityManager = new SecurityManager();
		} catch (Exception e) {
			System.exit(1);
		}
	}

	/**
	 * Retorna el hash del pan para busquedas en el base de datos
	 * 
	 * @param track2 campo 035 en el mensaje
	 * @return hash del pan.
	 * @throws Exception al realizar el hash
	 */
	public static String getHashPan(Track2 track2) throws Exception {
		return securityManager.hashToString(track2.getPan(), SecurityManager.DigestAlgorithm.HMAC_SHA1, true);
	}

	public static String getHashPanCNB(String pan) throws Exception {
		return securityManager.hashToString(pan, SecurityManager.DigestAlgorithm.HMAC_SHA1, true);
	}

	/**
	 * Obtiene el id de cuenta en claro.
	 * 
	 * @param object El id de la cuenta cifrado.
	 * @return Id de cuenta en claro.
	 * @throws XEncryptionKeyError En caso de error.
	 */
	public static String getClearAccount(Object object) throws XEncryptionKeyError {
		String encryptedAccId = (String) object;
		return securityManager.decryptToString(encryptedAccId);
	}

	/**
	 * Valida si la tarjeta ha sido bloqueda en postcard.
	 * 
	 * @param msg mensaje recibido desde Transaction Manager.
	 * @return true si la tarjeta fue bloqueda de lo contrario false.
	 * @throws XPostilion
	 */
	public static boolean isBlockCard(Iso8583Post msg) throws XPostilion {
		boolean rsp = false;
		if (Iso8583Post.MsgType._0210_TRAN_REQ_RSP == msg.getMsgType()
				&& Iso8583Post.RspCode._75_PIN_TRIES_EXCEEDED.equals(msg.getResponseCode())) {
			rsp = true;
		}
		return rsp;
	}

	/**
	 * Construye un mensaje con las descriciones devueltas de la validacion del
	 * mensaje en base 24.
	 * 
	 * @param descriptionsError Hashmap con las descripciones de los errores en el
	 *                          mensaje.
	 * @return mensaje con las descripciones de los errores.
	 */
	public static String constructMsgDescription(Map<Integer, String> descriptionsError) {
		StringBuilder descripcion = new StringBuilder();
		for (Map.Entry<Integer, String> entry : descriptionsError.entrySet()) {
			descripcion.append(entry.getValue());
		}
//		int i = 0;

		return descripcion.toString();
	}

	/**
	 * Valida si un canal es valido
	 * 
	 * @param channelString canal en String
	 * @param channel       canal en entero
	 * @return true si es un canal valio de loc ontrario false.
	 */
	public static boolean isValidChannel(String channelString, int channel) {
		boolean validChannel = true;
		if (channelString.length() == General.UNO) {
			try {
				channel = Integer.parseInt(channelString);
			} catch (NumberFormatException e) {
				validChannel = false;
			}
			switch (channel) {
			case Validation.Channel.ATM:
			case Validation.Channel.INTERNET:
			case Validation.Channel.OFICINAS:
			case Validation.Channel.POS:
			case Validation.Channel.IVR:
			case Validation.Channel.CNB:
				break;
			default:
				validChannel = false;
			}
		} else {
			validChannel = false;
		}
		return validChannel;
	}

	/*
	 * obtinen el c�digo de respuesta seg�n el tipo de cuenta en el processing code.
	 * Ene el caso de que la tarjeta no tenga una cuenta relacionada en postcard
	 *
	 * @param accountType tipo de cuenta obtenido del campo from account del�
	 * processing code
	 * 
	 * @return c�digo de respuesta.
	 */
	public static String getRspCodeToCardWithoutAccount(String accountType) {
		String rspCode;
		switch (accountType.substring(2, 4)) {
		case AccountType._10_SAVINGS:
			rspCode = RspCode._53_NO_SAVINGS_ACCOUNT;
			break;
		case AccountType._20_CHECK:
			rspCode = RspCode._52_NO_CHEQUING_ACCOUNT;
			break;
		case AccountType._30_CREDIT:
			rspCode = RspCode._39_NO_CREDIT_ACCOUNT;
			break;
		default:
			rspCode = RspCode._42_NO_UNIVERSAL_ACCOUNT;
			break;
		}
		return rspCode;
	}

	public static String getStringMessageException(Exception e) {
		StringWriter outError = new StringWriter();
		e.printStackTrace(new PrintWriter(outError));
		return outError.toString();
	}

	/**
	 * Cuenta el n�mero de registro en un resultSet
	 * 
	 * @param rs ResulSet a contar
	 * @return n�mero de registros
	 */
	public int countRows(ResultSet rs) {
		int rows = 0;
		try {
			if (rs.last()) {
				rows = rs.getRow();
				rs.beforeFirst();
			}
		} catch (SQLException e) {
			this.udpClient.sendData(Client.getMsgKeyValue("N/A",
					"Error while counting rows at Method countRows: " + Utils.getStringMessageException(e), "LOG",
					this.nameInterface));
			EventRecorder.recordEvent(
					new SQLExceptionEvent(new String[] { "unknown", "Unknown", Utils.getStringMessageException(e) }));

		}
		return rows;
	}
}