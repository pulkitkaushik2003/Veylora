package com.example.veylora.Service;

import com.example.veylora.Model.UserExotelSettings;
import com.example.veylora.Repository.UserExotelSettingsRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The user's own Exotel account. Mirrors the mailbox settings: the token is write-only and encrypted
 * (MailCrypto, key MAIL_ENCRYPTION_KEY). If the user has not saved an account, the server-wide one from .env
 * (EXOTEL_SID, EXOTEL_API_KEY, EXOTEL_API_TOKEN ...) is used when present.
 */
@Service
public class UserExotelSettingsService {

    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{6,15}$");
    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_\\-]{1,100}$");
    private static final Pattern NUMERIC = Pattern.compile("^[0-9]{1,20}$");
    /** The API key and token are sent to this host, so it must really be Exotel. */
    private static final Pattern HOST = Pattern.compile("^[a-z0-9-]+(\\.[a-z0-9-]+)*\\.exotel\\.com$");

    private final UserExotelSettingsRepository repository;
    private final MailCrypto crypto;
    private final ExotelClient client;
    private final ExotelClient.Creds defaults;

    public UserExotelSettingsService(UserExotelSettingsRepository repository, MailCrypto crypto, ExotelClient client,
                                     @Value("${EXOTEL_SID:}") String sid,
                                     @Value("${EXOTEL_API_KEY:}") String key,
                                     @Value("${EXOTEL_API_TOKEN:}") String token,
                                     @Value("${EXOTEL_SUBDOMAIN:api.in.exotel.com}") String subdomain,
                                     @Value("${EXOTEL_CALLER_ID:}") String callerId,
                                     @Value("${EXOTEL_SMS_FROM:}") String smsFrom,
                                     @Value("${EXOTEL_DLT_ENTITY_ID:}") String dltEntityId,
                                     @Value("${EXOTEL_DEFAULT_APP_ID:}") String appId) {
        this.repository = repository;
        this.crypto = crypto;
        this.client = client;
        this.defaults = (blank(sid) || blank(key) || blank(token)) ? null
                : new ExotelClient.Creds(sid.trim(), key.trim(), token.trim(), subdomain.trim(),
                        nul(callerId), nul(smsFrom), nul(dltEntityId), nul(appId));
    }

    public record View(boolean custom, boolean defaultAvailable, String accountSid, String apiKey, String subdomain,
                       String callerId, String smsFrom, String dltEntityId, String defaultAppId) {}

    public record SaveRequest(String accountSid, String apiKey, String apiToken, String subdomain,
                              String callerId, String smsFrom, String dltEntityId, String defaultAppId) {}

    @Transactional(readOnly = true)
    public View get(Long userId) {
        return repository.findByUserId(userId)
                .map(s -> new View(true, defaults != null, s.getAccountSid(), s.getApiKey(), s.getSubdomain(),
                        s.getCallerId(), s.getSmsFrom(), s.getDltEntityId(), s.getDefaultAppId()))
                .orElse(new View(false, defaults != null, null, null, null, null, null, null, null));
    }

    /** The account this user's campaigns run on: their own, else the server one, else null. */
    @Transactional(readOnly = true)
    public ExotelClient.Creds resolve(Long userId) {
        Optional<UserExotelSettings> own = userId == null ? Optional.empty() : repository.findByUserId(userId);
        if (own.isPresent()) {
            UserExotelSettings s = own.get();
            return new ExotelClient.Creds(s.getAccountSid(), s.getApiKey(), crypto.decrypt(s.getApiTokenEnc()),
                    s.getSubdomain(), s.getCallerId(), s.getSmsFrom(), s.getDltEntityId(), s.getDefaultAppId());
        }
        return defaults;
    }

    /** Saves the account. The credentials are checked with Exotel first, so wrong ones are never saved. */
    @Transactional
    public View save(Long userId, SaveRequest r) {
        String sid = clean(r.accountSid());
        String key = clean(r.apiKey());
        if (!ID.matcher(sid).matches()) throw new IllegalArgumentException("Enter your Exotel account SID");
        if (key.isEmpty() || key.length() > 200) throw new IllegalArgumentException("Enter your Exotel API key");
        String host = blank(r.subdomain()) ? "api.in.exotel.com" : r.subdomain().trim().toLowerCase();
        if (!HOST.matcher(host).matches()) {
            throw new IllegalArgumentException("Subdomain must be api.in.exotel.com (Mumbai) or api.exotel.com (Singapore)");
        }
        String callerId = optionalPhone(r.callerId(), "Caller ID (ExoPhone)");
        String smsFrom = optionalPhone(r.smsFrom(), "SMS sender");
        String dlt = clean(r.dltEntityId());
        if (dlt.length() > 60) throw new IllegalArgumentException("DLT entity id is too long");
        String appId = clean(r.defaultAppId());
        if (!appId.isEmpty() && !NUMERIC.matcher(appId).matches()) {
            throw new IllegalArgumentException("Exotel app id is a number, for example 1234567");
        }

        UserExotelSettings s = repository.findByUserId(userId).orElseGet(UserExotelSettings::new);
        String token = r.apiToken() == null ? "" : r.apiToken().trim();
        if (token.isEmpty()) {
            if (s.getId() == null) throw new IllegalArgumentException("Enter your Exotel API token");
            token = crypto.decrypt(s.getApiTokenEnc()); // keep the saved token when only other fields changed
        }
        client.verify(new ExotelClient.Creds(sid, key, token, host, callerId, smsFrom, dlt, appId));

        s.setUserId(userId);
        s.setAccountSid(sid);
        s.setApiKey(key);
        s.setApiTokenEnc(crypto.encrypt(token));
        s.setSubdomain(host);
        s.setCallerId(callerId);
        s.setSmsFrom(smsFrom);
        s.setDltEntityId(nul(dlt));
        s.setDefaultAppId(nul(appId));
        s.setUpdatedAt(LocalDateTime.now());
        repository.save(s);
        return get(userId);
    }

    @Transactional
    public View remove(Long userId) {
        repository.deleteByUserId(userId);
        return get(userId);
    }

    /** Sends a real SMS from the user's account. In India the text must match an approved DLT template. */
    public void testSms(Long userId, String to, String body, String dltTemplateId) {
        ExotelClient.Creds c = requireCreds(userId);
        if (blank(c.smsSender())) throw new IllegalArgumentException("Add an SMS sender (or Caller ID) in the Exotel settings first");
        if (blank(body)) throw new IllegalArgumentException("Enter the test message text");
        client.sendSms(c, phone(to), body, clean(dltTemplateId), "veylora-test");
    }

    /** Places a real call that runs the default flow (or the given one). */
    public void testCall(Long userId, String to, String appId) {
        ExotelClient.Creds c = requireCreds(userId);
        if (blank(c.callerId())) throw new IllegalArgumentException("Add a Caller ID (ExoPhone) in the Exotel settings first");
        String app = blank(appId) ? c.defaultAppId() : appId.trim();
        if (blank(app) || !NUMERIC.matcher(app).matches()) throw new IllegalArgumentException("Enter the Exotel app (flow) id for the test call");
        client.connectCall(c, phone(to), app, "veylora-test");
    }

    private ExotelClient.Creds requireCreds(Long userId) {
        ExotelClient.Creds c = resolve(userId);
        if (c == null) throw new IllegalStateException("Exotel is not set up. Save your Exotel details first.");
        return c;
    }

    private static String phone(String raw) {
        String p = raw == null ? "" : raw.replaceAll("[\\s\\-().]", "");
        if (!PHONE.matcher(p).matches()) throw new IllegalArgumentException("Enter a valid phone number for the test");
        return p;
    }

    private static String optionalPhone(String raw, String label) {
        String p = raw == null ? "" : raw.replaceAll("[\\s\\-().]", "");
        if (p.isEmpty()) return null;
        if (!PHONE.matcher(p).matches()) throw new IllegalArgumentException(label + " must be a phone number");
        return p;
    }

    private static String clean(String s) { return s == null ? "" : s.trim(); }
    private static boolean blank(String s) { return s == null || s.isBlank(); }
    private static String nul(String s) { return blank(s) ? null : s.trim(); }
}
