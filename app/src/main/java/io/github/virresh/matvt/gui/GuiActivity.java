package io.github.virresh.matvt.gui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.virresh.matvt.R;
import io.github.virresh.matvt.helper.Helper;

import static io.github.virresh.matvt.engine.impl.MouseEmulationEngine.bossKey;

public class GuiActivity extends AppCompatActivity {
    CountDownTimer repopulate;
    CheckBox cb_override;
    TextView gui_acc_perm, gui_acc_serv, gui_overlay_perm, gui_overlay_serv;

    EditText et_override;
    Button bt_override;
    LinearLayout boss_override;

    Spinner sp_mouse_icon;
    SeekBar dsbar_mouse_size;

    public static int ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE = 701;
    public static int ACTION_ACCESSIBILITY_PERMISSION_REQUEST_CODE = 702;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gui);
        gui_acc_perm = findViewById(R.id.gui_acc_perm);
        gui_acc_serv = findViewById(R.id.gui_acc_serv);
        gui_overlay_perm = findViewById(R.id.gui_overlay_perm);
        gui_overlay_serv = findViewById(R.id.gui_overlay_serv);
        boss_override = findViewById(R.id.boss_override);
        bt_override = findViewById(R.id.bt_override);
        et_override = findViewById(R.id.et_override);
        cb_override = findViewById(R.id.cb_override);
        sp_mouse_icon = findViewById(R.id.sp_mouse_icon);
        dsbar_mouse_size = findViewById(R.id.dsbar_mouse_size);

        // render icon style dropdown
        IconStyleSpinnerAdapter iconStyleSpinnerAdapter = new IconStyleSpinnerAdapter(this, R.layout.spinner_icon_text_gui, R.id.textView, IconStyleSpinnerAdapter.getResourceList());
        sp_mouse_icon.setAdapter(iconStyleSpinnerAdapter);

        checkValues(iconStyleSpinnerAdapter);
        showBossLayout(cb_override.isChecked());
        cb_override.setOnCheckedChangeListener((compoundButton, b) -> {
            showBossLayout(b);
        });

        bt_override.setOnClickListener(view -> {
            String dat = et_override.getText().toString();
            dat = dat.replaceAll("[^0-9]", "");
            int keyValue; if (dat.isEmpty()) keyValue = KeyEvent.KEYCODE_VOLUME_MUTE;
            else keyValue = Integer.parseInt(dat);
            Helper.setOverrideStatus(this, cb_override.isChecked());
            Helper.setOverrideValue(this, keyValue);
            bossKey = keyValue;
            Toast.makeText(this, "New key is : "+keyValue, Toast.LENGTH_SHORT).show();
        });

        sp_mouse_icon.setOnItemSelectedListener(new OnItemSelectedListener() {
            // the listener is set after setting initial value to avoid echo if any
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                Context ctx = getApplicationContext();
                String style = iconStyleSpinnerAdapter.getItem(pos);
                Helper.setMouseIconPref(ctx, iconStyleSpinnerAdapter.getItem(pos));
//                Toast.makeText(ctx, "Icon style set to "+style+". Changes will take effect from next restart.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        dsbar_mouse_size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // do not do anything if the progress change was done programmatically
                    Context ctx = getApplicationContext();
                    Helper.setMouseSizePref(ctx, progress);
//                    Toast.makeText(ctx, "Mouse size set. Changes will take effect from next restart.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        populateText();
        findViewById(R.id.gui_setup_perm).setOnClickListener(view -> askPermissions());
    }

    private void checkValues(IconStyleSpinnerAdapter adapter) {
        Context ctx = getApplicationContext();
        String val = String.valueOf(Helper.getOverrideValue(ctx));
        if (Helper.isOverriding(ctx)) {
            cb_override.setChecked(true);
            et_override.setText(val);
        }
        String iconStyle = Helper.getMouseIconPref(ctx);
        sp_mouse_icon.setSelection(adapter.getPosition(iconStyle));

        int mouseSize = Helper.getMouseSizePref(ctx);
        dsbar_mouse_size.setProgress(Math.max(Math.min(mouseSize, 4), 0));
    }

    private void showBossLayout(boolean status) {
        if (status) boss_override.setVisibility(View.VISIBLE);
        else boss_override.setVisibility(View.INVISIBLE);
    }


    private void askPermissions() {
        if (Helper.isOverlayDisabled(this)) {
            try {
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION),
                        ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE);
            } catch (Exception unused) {
                Toast.makeText(this, "Overlay Permission Handler not Found", Toast.LENGTH_SHORT).show();
            }
        }
        if (!Helper.isOverlayDisabled(this) && Helper.isAccessibilityDisabled(this)) {
            checkAccPerms();
        }
    }

    private void checkAccPerms() {
        if (Helper.isAccessibilityDisabled(this))
            try {
                startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
                        ACTION_ACCESSIBILITY_PERMISSION_REQUEST_CODE);
            } catch (Exception exception) {
                Toast.makeText(this, "Acessibility Handler not Found", Toast.LENGTH_SHORT).show();
            }
    }

    public void populateText() {
        if (Helper.isOverlayDisabled(this))  gui_overlay_perm.setText(R.string.perm_overlay_denied);
        else gui_overlay_perm.setText(R.string.perm_overlay_allowed);

        if (Helper.isAccessibilityDisabled(this)) {
            gui_acc_perm.setText(R.string.perm_acc_denied);
            gui_acc_serv.setText(R.string.serv_acc_denied);
            gui_overlay_serv.setText(R.string.serv_overlay_denied); }
        else gui_acc_perm.setText(R.string.perm_acc_allowed);

        if (Helper.isAccessibilityDisabled(this) && Helper.isOverlayDisabled(this)) {
            gui_acc_perm.setText(R.string.perm_acc_denied);
            gui_acc_serv.setText(R.string.serv_acc_denied);
            gui_overlay_perm.setText(R.string.perm_overlay_denied);
            gui_overlay_serv.setText(R.string.serv_overlay_denied);
        }

        if (!Helper.isAccessibilityDisabled(this) && !Helper.isOverlayDisabled(this)) {
            gui_acc_perm.setText(R.string.perm_acc_allowed);
            gui_acc_serv.setText(R.string.serv_acc_allowed);
            gui_overlay_perm.setText(R.string.perm_overlay_allowed);
            gui_overlay_serv.setText(R.string.serv_overlay_allowed);
            findViewById(R.id.gui_setup_perm).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTION_MANAGE_OVERLAY_PERMISSION_REQUEST_CODE)
            if (Helper.isOverlayDisabled(this)) {
                Toast.makeText(this, "Overlay Permissions Denied", Toast.LENGTH_SHORT).show();
            } else checkAccPerms();
        if (requestCode == ACTION_ACCESSIBILITY_PERMISSION_REQUEST_CODE)
            if (Helper.isAccessibilityDisabled(this)) {
                Toast.makeText(this, "Accessibility Services not running", Toast.LENGTH_SHORT).show();
            }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Checking services status
        checkServiceStatus();

    }

    private void checkServiceStatus() {
        //checking for changed every 2 sec
        repopulate = new CountDownTimer(2000, 2000) {
            @Override
            public void onTick(long l) { }
            @Override
            public void onFinish() {
                populateText();
                repopulate.start(); //restarting the timer
            }
        };
        repopulate.start();
    }
}