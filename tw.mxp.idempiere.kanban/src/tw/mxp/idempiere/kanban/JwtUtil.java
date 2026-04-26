package tw.mxp.idempiere.kanban;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.compiere.model.MSysConfig;

/**
 * JWT utility — generates and validates tokens using HMAC-SHA512.
 * Secret from AD_SysConfig: KANBAN_TOKEN_SECRET (independent from REST API).
 * Fallback secret if not configured. No external JWT library dependency.
 */
public class JwtUtil {

	private static final String HEADER = "{\"alg\":\"HS512\",\"typ\":\"JWT\",\"kid\":\"idempiere\"}";
	private static final long DEFAULT_EXPIRY_SECONDS = 3600; // 1 hour

	/**
	 * Generate a JWT token.
	 */
	public static String generate(int clientId, int orgId, int userId, int roleId, String userName) {
		try {
			long exp = (System.currentTimeMillis() / 1000) + DEFAULT_EXPIRY_SECONDS;
			StringBuilder payload = new StringBuilder();
			payload.append("{\"sub\":\"").append(esc(userName)).append("\"");
			payload.append(",\"AD_Client_ID\":").append(clientId);
			payload.append(",\"AD_User_ID\":").append(userId);
			payload.append(",\"AD_Role_ID\":").append(roleId);
			payload.append(",\"AD_Org_ID\":").append(orgId);
			payload.append(",\"iss\":\"idempiere.org\"");
			payload.append(",\"exp\":").append(exp);
			payload.append("}");

			return sign(HEADER, payload.toString());
		} catch (Exception e) {
			throw new RuntimeException("JWT generation failed", e);
		}
	}

	/**
	 * Validate a JWT token and return claims.
	 * @return claims map, or null if invalid
	 */
	public static Map<String, Object> validate(String token) {
		if (token == null) return null;
		String[] parts = token.split("\\.");
		if (parts.length != 3) return null;

		try {
			// Verify signature
			String secret = getSecret();
			Mac mac = Mac.getInstance("HmacSHA512");
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
			String expectedSig = Base64.getUrlEncoder().withoutPadding()
					.encodeToString(mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8)));
			if (!expectedSig.equals(parts[2])) return null;

			// Decode payload
			String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

			// Check expiry
			long exp = extractLong(payload, "exp");
			if (exp > 0 && exp < System.currentTimeMillis() / 1000) return null;

			// Extract claims
			Map<String, Object> claims = new HashMap<>();
			claims.put("AD_Client_ID", extractInt(payload, "AD_Client_ID"));
			claims.put("AD_Org_ID", extractInt(payload, "AD_Org_ID"));
			claims.put("AD_User_ID", extractInt(payload, "AD_User_ID"));
			claims.put("AD_Role_ID", extractInt(payload, "AD_Role_ID"));
			return claims;
		} catch (Exception e) {
			return null;
		}
	}

	private static String getSecret() {
		// Use our own SysConfig key, independent from REST API
		String secret = MSysConfig.getValue("KANBAN_TOKEN_SECRET", "");
		if (secret == null || secret.isEmpty()) {
			// Fallback: deterministic secret (same across cluster nodes)
			secret = "iDempiere-Kanban-" + System.getProperty("ADEMPIERE_DB_NAME", "idempiere");
		}
		return secret;
	}

	private static String sign(String header, String payload) throws Exception {
		Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
		String headerB64 = enc.encodeToString(header.getBytes(StandardCharsets.UTF_8));
		String payloadB64 = enc.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		String data = headerB64 + "." + payloadB64;

		String secret = getSecret();
		Mac mac = Mac.getInstance("HmacSHA512");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
		String sig = enc.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));

		return data + "." + sig;
	}

	private static String esc(String s) {
		return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static int extractInt(String json, String key) {
		String search = "\"" + key + "\":";
		int idx = json.indexOf(search);
		if (idx < 0) return -1;
		idx += search.length();
		StringBuilder sb = new StringBuilder();
		for (int i = idx; i < json.length(); i++) {
			char c = json.charAt(i);
			if (Character.isDigit(c) || c == '-') sb.append(c);
			else if (sb.length() > 0) break;
		}
		return sb.length() > 0 ? Integer.parseInt(sb.toString()) : -1;
	}

	private static long extractLong(String json, String key) {
		String search = "\"" + key + "\":";
		int idx = json.indexOf(search);
		if (idx < 0) return -1;
		idx += search.length();
		StringBuilder sb = new StringBuilder();
		for (int i = idx; i < json.length(); i++) {
			char c = json.charAt(i);
			if (Character.isDigit(c)) sb.append(c);
			else if (sb.length() > 0) break;
		}
		return sb.length() > 0 ? Long.parseLong(sb.toString()) : -1;
	}
}
