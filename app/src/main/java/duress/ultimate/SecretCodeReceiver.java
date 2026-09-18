package duress.ultimate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class SecretCodeReceiver extends BroadcastReceiver {

    private static final String PREFS = "prefs";
    private static final String SECRET_CODE_HASH = "secret_code_hash";
    private static final String SECRET_CODE_SALT = "secret_code_salt";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context != null && intent != null && "android.provider.Telephony.SECRET_CODE".equals(intent.getAction())) {
            String host = intent.getData() != null ? intent.getData().getHost() : null;
            if (host == null) return;

            Context deContext = context.getApplicationContext().createDeviceProtectedStorageContext();
            SharedPreferences dePrefs = deContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            String savedHash = CryptoManager.getString(dePrefs, CryptoManager.DE_ALIAS, SECRET_CODE_HASH, null);
            String savedSalt = CryptoManager.getString(dePrefs, CryptoManager.DE_ALIAS, SECRET_CODE_SALT, null);

            if (savedHash != null && savedSalt != null) {
                String inputHash = hashPin(host, savedSalt);
                if (savedHash.equals(inputHash)) {
                    Intent i = new Intent(context, EntryActivity.class);
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(i);
                }
            }
        }
    }

    private String hashPin(String pin, String saltBase64) {
        try {
            byte[] salt = Base64.decode(saltBase64, Base64.DEFAULT);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hash = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }
}
