package duress.ultimate;

import android.app.Activity;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class CalculatorActivity extends Activity {

    private static final String PREFS = "prefs";
    private static final String SECRET_CODE_HASH = "secret_code_hash";
    private static final String SECRET_CODE_SALT = "secret_code_salt";

    private TextView display;
    private StringBuilder currentInput;
    private String operator;
    private double firstOperand;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        currentInput = new StringBuilder();
        operator = "";
        firstOperand = 0.0;

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#FAFAFA"));
        rootLayout.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        display = new TextView(this);
        display.setText("0");
        display.setTextSize(TypedValue.COMPLEX_UNIT_SP, 56);
        display.setGravity(Gravity.BOTTOM | Gravity.END);
        display.setPadding(48, 48, 48, 48);
        display.setTextColor(Color.parseColor("#212121"));

        LinearLayout.LayoutParams displayParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1.0f
        );
        rootLayout.addView(display, displayParams);

        GridLayout gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(4);
        gridLayout.setRowCount(4);

        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        gridParams.setMargins(16, 16, 16, 16);
        rootLayout.addView(gridLayout, gridParams);

        String[] buttonLabels = {
                "7", "8", "9", "÷",
                "4", "5", "6", "×",
                "1", "2", "3", "−",
                "C", "0", "=", "+"
        };

        for (int i = 0; i < buttonLabels.length; i++) {
            final String label = buttonLabels[i];
            Button button = new Button(this);
            button.setText(label);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24);
            button.setTextColor(Color.parseColor("#212121"));
            button.setBackgroundColor(Color.parseColor("#E0E0E0"));

            int row = i / 4;
            int col = i % 4;

            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(row, 1f),
                    GridLayout.spec(col, 1f)
            );
            params.width = 0;
            params.height = 0;
            params.setMargins(8, 8, 8, 8);
            button.setLayoutParams(params);

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleButtonClick(label);
                }
            });

            gridLayout.addView(button);
        }

        setContentView(rootLayout);
    }

    private void handleButtonClick(String value) {
        if (value.matches("[0-9]")) {
            if (currentInput.toString().equals("0")) {
                currentInput.setLength(0);
            }
            currentInput.append(value);
            display.setText(currentInput.toString());
        } else if (value.matches("[+−×÷]")) {
            if (currentInput.length() > 0) {
                firstOperand = Double.parseDouble(currentInput.toString());
                operator = value;
                currentInput.setLength(0);
                display.setText(operator);
            }
        } else if (value.equals("C")) {
            currentInput.setLength(0);
            operator = "";
            firstOperand = 0.0;
            display.setText("0");
        } else if (value.equals("=")) {
            if (checkSecretCode(currentInput.toString())) {
                currentInput.setLength(0);
                display.setText("0");
                return;
            }

            if (currentInput.length() > 0 && !operator.isEmpty()) {
                double secondOperand = Double.parseDouble(currentInput.toString());
                double result = 0.0;

                switch (operator) {
                    case "+":
                        result = firstOperand + secondOperand;
                        break;
                    case "−":
                        result = firstOperand - secondOperand;
                        break;
                    case "×":
                        result = firstOperand * secondOperand;
                        break;
                    case "÷":
                        if (secondOperand != 0.0) {
                            result = firstOperand / secondOperand;
                        }
                        break;
                }

                String resultString;
                if (result % 1.0 == 0.0) {
                    resultString = String.valueOf((long) result);
                } else {
                    resultString = String.valueOf(result);
                }

                display.setText(resultString);
                currentInput.setLength(0);
                currentInput.append(resultString);
                operator = "";
            }
        }
    }

    private boolean checkSecretCode(String data) {
        if (data == null) {
            return false;
        }

        if (data.length() < 5) {
            return false;
        }

        data = data.substring(data.length() - 5);

        if (data.length() > 5) {
            return false;
        }

        Context deContext = getApplicationContext().createDeviceProtectedStorageContext();
        SharedPreferences dePrefs = deContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String savedHash = dePrefs.getString(SECRET_CODE_HASH, null);
        String savedSalt = dePrefs.getString(SECRET_CODE_SALT, null);

        if (savedHash == null || savedSalt == null) {
            return false;
        }

        String inputHash = hashPin(data, savedSalt);
        if (!savedHash.equals(inputHash)) {
            return false;
        }

        Intent intent = new Intent(this, EntryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

        return true;
    }

    private String hashPin(String pin, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.decode(salt, Base64.NO_WRAP));
            byte[] hashBytes = digest.digest(pin.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(hashBytes, Base64.NO_WRAP);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
