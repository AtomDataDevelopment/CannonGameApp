package br.com.prog3.cannongameapp;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.CompoundButton;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {

    // Chave para SharedPreferences
    private static final String PREF_NIGHT_MODE = "night_mode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. APLICA O TEMA SALVO ANTES DE CHAMAR super.onCreate()
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isNightMode = prefs.getBoolean(PREF_NIGHT_MODE, false);
        setAppTheme(isNightMode); // Aplica o tema

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 2. CONFIGURA O SWITCH (ASSUMINDO ID nightModeSwitch NO activity_main.xml)
        Switch nightModeSwitch = findViewById(R.id.nightModeSwitch);

        // Verifica se o switch existe no layout antes de configurar
        if (nightModeSwitch != null) {
            nightModeSwitch.setChecked(isNightMode);

            nightModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // 3. SALVA E APLICA O NOVO TEMA
                    saveNightModePreference(isChecked);
                    setAppTheme(isChecked);

                    // 4. RECRIAR A ACTIVITY PARA APLICAR AS NOVAS CORES NA VIEW
                    recreate();
                }
            });
        }
    }

    // Método auxiliar para salvar a preferência
    private void saveNightModePreference(boolean isNightMode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_NIGHT_MODE, isNightMode);
        editor.apply();
    }

    // Método auxiliar para aplicar o tema do aplicativo
    private void setAppTheme(boolean isNightMode) {
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
