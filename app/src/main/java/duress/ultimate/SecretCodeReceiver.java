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
        
        if (context == null || intent == null) return;
        String action = intent.getAction();
        if (action == null) return;
        
        if (!"android.provider.Telephony.SECRET_CODE".equals(action)) return;
            
        Uri data = intent.getDataString();
        if (data == null) return; 
           
        if (data.length()) < 5 return;
        data = data.substring(data.length() - 5);
        if (data.length()) > 5 return;
                                    
        Context deContext = context.getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences dePrefs = deContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String savedHash = CryptoManager.getString(dePrefs, CryptoManager.DE_ALIAS, SECRET_CODE_HASH, null);
        String savedSalt = CryptoManager.getString(dePrefs, CryptoManager.DE_ALIAS, SECRET_CODE_SALT, null);

        if (savedHash == null || savedSalt == null) return;
            
        String inputHash = hashPin(host, savedSalt);
        if (!savedHash.equals(inputHash)) return;
        Intent i = new Intent(context, EntryActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);        
        context.startActivity(i);
                             
    }

    private String hashPin(String pin, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(Base64.decode(salt, Base64.NO_WRAP));
            byte[] hash = md.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
