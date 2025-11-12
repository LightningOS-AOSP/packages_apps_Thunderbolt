/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.lightning.thunderbolt.fragments.about;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.android.settings.R;

public class DonateActivity extends AppCompatActivity {

    private static final String TAG = "DonateActivity";

    private static final String UPI_ID = "zabukazuzu@ybl";
    private static final String UPI_NAME = "Mist OS";
    
    private static final String PAYPAL_EMAIL = "zabukazuzu@gmail.com";
    private static final String PAYPAL_URL = "https://paypal.me/ShukakuZa?country.x=IN&locale.x=en_GB";

    @Override
    protected void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_donate);

        Button donateNow = findViewById(R.id.mistos_donate_button);
        Button dismiss = findViewById(R.id.mistos_later_button);

        donateNow.setOnClickListener(v -> showDonateOptions());
        dismiss.setOnClickListener(v -> onDismissClick());

        TextView msg = findViewById(R.id.mistos_donate_message);
        if (msg != null) {
            msg.setMovementMethod(new ScrollingMovementMethod());
        }

        DonateReceiver.cancelNotification(this);
        setLastChecked();
    }

    private void showDonateOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_donate_options, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        
        Button btnUpi = dialogView.findViewById(R.id.btn_upi);
        Button btnPaypal = dialogView.findViewById(R.id.btn_paypal);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        
        btnUpi.setOnClickListener(v -> {
            dialog.dismiss();
            openUpiPayment();
        });
        
        btnPaypal.setOnClickListener(v -> {
            dialog.dismiss();
            openPayPalPayment();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void openUpiPayment() {
        try {
            Uri uri = Uri.parse("upi://pay").buildUpon()
                    .appendQueryParameter("pa", UPI_ID)
                    .appendQueryParameter("pn", UPI_NAME)
                    .appendQueryParameter("cu", "INR")
                    .build();
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, getString(R.string.mistos_donate_choose_upi_app)));
            } else {
                showUpiCopyDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening UPI payment", e);
            showUpiCopyDialog();
        }
    }

    private void showUpiCopyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.mistos_donate_no_upi_app);
        builder.setMessage(getString(R.string.mistos_donate_copy_upi_id, UPI_ID));
        
        builder.setPositiveButton(R.string.mistos_donate_copy, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("UPI ID", UPI_ID);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.mistos_donate_upi_copied, Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton(android.R.string.lightning_cancel, null);
        builder.show();
    }

    private void openPayPalPayment() {
        try {
            Intent paypalIntent = new Intent(Intent.ACTION_VIEW);
            paypalIntent.setData(Uri.parse(PAYPAL_URL));
            
            paypalIntent.setPackage("com.paypal.android.p2pmobile");
            if (paypalIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(paypalIntent);
                return;
            }
            
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_URL));
            if (browserIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(browserIntent, getString(R.string.mistos_donate_open_browser)));
            } else {
                showPayPalBrowserDialog();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening PayPal", e);
            showPayPalBrowserDialog();
        }
    }

    private void showPayPalBrowserDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.mistos_donate_no_paypal_app);
        builder.setMessage(getString(R.string.mistos_donate_paypal_options, PAYPAL_EMAIL));
        
        builder.setPositiveButton(R.string.mistos_donate_open_browser, (dialog, which) -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_URL));
            browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(browserIntent);
        });
        
        builder.setNeutralButton(R.string.mistos_donate_copy_email, (dialog, which) -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("PayPal Email", PAYPAL_EMAIL);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.mistos_donate_email_copied, Toast.LENGTH_SHORT).show();
        });
        
        builder.setNegativeButton(android.R.string.lightning_cancel, null);
        builder.show();
    }

    private void onDismissClick() {
        finish();
    }

    private void setLastChecked() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putLong(DonateReceiver.DONATE_LAST_CHECKED, System.currentTimeMillis())
                .apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
