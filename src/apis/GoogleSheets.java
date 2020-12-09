package apis;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;

import sch.discord.Messages;

public class GoogleSheets {

	private static final String APPLICATION_NAME = "Google Sheets API Java Quickstart";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	private static final String CREDENTIALS_FILE_PATH = "credentials.json";
	private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS_READONLY);

	private static Sheets service;

	public static String SpreadSheetID = "1hlUwAw16awEthvwF0OpKWUrF_eUAKR40Z7vc9ievFak";
	public static String range = "Raw Entries!A2:E";

	public static int CHARACTER_NAME_ROW = 1;
	public static int CHARACTER_SHEET_LINK_ROW = 2;
	public static int CHARACTER_PLAYER_ROW=0;

	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
		// Load client secrets.
		InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	public static ValueRange getResponse() throws IOException {
		return service.spreadsheets().values().get(SpreadSheetID, range).execute();
	}
	


	public static String searchForCharacter(String character) throws IOException {
		ValueRange response = getResponse();
		List<List<Object>> values = response.getValues();
		String characterSearch = character.toLowerCase();
		List<String> returns = new LinkedList<>();
		List<String> returnNames = new LinkedList<>();
		for (List<Object> row : values) {
			if (row.size() == 0)
				continue;
			String result = row.get(CHARACTER_NAME_ROW).toString().toLowerCase();
			if (row.isEmpty())
				continue;
			if (result.equals(characterSearch)) {
				return row.get(CHARACTER_SHEET_LINK_ROW).toString();
			} else if (result.contains(characterSearch)) {
				returns.add(row.get(CHARACTER_SHEET_LINK_ROW).toString());
				returnNames.add(row.get(CHARACTER_NAME_ROW).toString());
			}
		}
		if (returns.size() == 1) {
			return returns.get(0);
		} else if (returns.size() > 1) {

			if (returns.size() <= 5) {
				StringBuilder sb = new StringBuilder();
				sb.append(Messages.REGISTRY_CHARACTERSEARCHBYNAME_RETURNED_MULTIPLE_SHEETS);
				Iterator<String> i = returnNames.iterator();
				while (i.hasNext()) {
					sb.append(i.next());
					if (i.hasNext()) {
						sb.append(", ");
					} else {
						sb.append(".");
					}
				}
				return sb.toString();
			}
			return Messages.REGISTRY_CHARACTERSEARCHBYNAME_RETURNED_MULTIPLE_SHEETS_TOO_MANY;
		}
		return Messages.REGISTRY_CHARACTERSEARCHBYNAME_NO_CHARACTER_FOUND;
	}

	public static void connect() throws GeneralSecurityException, IOException {
		final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
		service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
				.setApplicationName(APPLICATION_NAME).build();
	}

}
