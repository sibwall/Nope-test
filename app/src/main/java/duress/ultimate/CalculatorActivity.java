package duress.ultimate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CalculatorActivity extends AppCompatActivity {

    private static final String PREFS = "prefs";
    private static final String SECRET_CODE_HASH = "secret_code_hash";
    private static final String SECRET_CODE_SALT = "secret_code_salt";

    private TextView display;
    private String currentInput = "";
    private String operator = "";
    private double firstOperand = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#F5F5F5"));

        display = new TextView(this);
        display.setText("0");
        display.setTextSize(TypedValue.COMPLEX_UNIT_SP, 48);
        display.setGravity(Gravity.BOTTOM | Gravity.END);
        display.setPadding(40, 40, 40, 40);
        display.setTextColor(Color.BLACK);
        
        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        root.addView(display, displayParams);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(4);
        grid.setRowCount(4);
        
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT);
        gridParams.bottomMargin = 20;
        root.addView(grid, gridParams);

        String[] buttons = {
                "7", "8", "9", "/",
                "4", "5", "6", "*",
                "1", "2", "3", "-",
                "C", "0", "=", "+"
        };

        for (String btnText : buttons) {
            Button btn = new Button(this);
            btn.setText(btnText);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(8, 8, 8, 8);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> handleButtonClick(btnText));
            grid.addView(btn);
        }

        setContentView(root);
    }

    private void handleButtonClick(String value) {
        if (value.matches("[0-9]")) {
            if (currentInput.equals("0")) currentInput = "";
            currentInput += value;
            display.setText(currentInput);
            
        } else if (value.matches("[+\\-*/]")) {
            if (!currentInput.isEmpty()) {
                firstOperand = Double.parseDouble(currentInput);
                operator = value;
                currentInput = "";
                display.setText(operator);
            }
            
        } else if (value.equals("C")) {
            currentInput = "";
            operator = "";
            firstOperand = 0;
            display.setText("0");
            
        } else if (value.equals("=")) {
            if (checkSecretCode(currentInput)) {
                currentInput = "";
                display.setText("0");
                return;
            }

            if (!currentInput.isEmpty() && !operator.isEmpty()) {
                double secondOperand = Double.parseDouble(currentInput);
                double result = 0;

                switch (operator) {
                    case "+": result = firstOperand + secondOperand; break;
                    case "-": result = firstOperand - secondOperand; break;
                    case "*": result = firstOperand * secondOperand; break;
                    case "/": 
                        if (secondOperand != 0) result = firstOperand / secondOperand; 
                        break;
                }

                String resultStr = (result % 1 == 0) ? String.valueOf((long) result) : String.valueOf(result);
                display.setText(resultStr);
                
                currentInput = resultStr;
                operator = "";
            }
        }
    }

    private boolean checkSecretCode(String data) {
        if (data == null || data.length() < 5) return false;
        
        data = data.substring(data.length() - 5);

        Context deContext = getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences dePrefs = deContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String savedHash = CryptoManager.getString(dePrefs, CryptoManager.DE_ALIAS, SECRET_CODE_HASH, null);
        String savedSalt = CryptoManager.getString(dePrefs, CryptoManager.DE_ALIAS, SECRET_CODE_SALT, null);

        if (savedHash == null || savedSalt == null) return false;

        String inputHash = hashPin(data, savedSalt);
        if (savedHash.equals(inputHash)) {
            Intent i = new Intent(this, EntryActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(i);
            return true;
        }
        return false;
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
