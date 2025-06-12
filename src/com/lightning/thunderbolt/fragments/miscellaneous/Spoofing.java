/*
 * Copyright (C) 2019-2024 LightningOS
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lightning.thunderbolt.fragments.miscellaneous;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.preference.Preference;

import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.android.SystemRestartUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;

import org.json.JSONException;
import org.json.JSONObject;

import org.w3c.dom.*;
import javax.xml.parsers.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

@SearchIndexable
public class Spoofing extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "Spoofing";
    private static final String SYS_GMS_SPOOF = "persist.sys.pixelprops.gms";
    private static final String SYS_GPHOTOS_SPOOF = "persist.sys.pixelprops.gphotos";
    private static final String KEY_IMPORT_KEYBOX = "import_keybox";
    private static final String KEY_CLEAR_KEYBOX = "clear_keybox";
    private static final String KEYBOX_PATH = "/data/misc/keybox/keybox.xml";
    private static final String KEY_PIF_JSON_FILE_PREFERENCE = "pif_json_file_preference";
    private static final String KEY_UPDATE_JSON_BUTTON = "update_pif_json";

    private Preference mGphotosSpoof;
    private Preference mImportKeybox;
    private Preference mClearKeybox;
    private Preference mGmsSpoof;
    private Preference mPifJsonFilePreference;
    private Preference mUpdateJsonButton;

    private Handler mHandler;
    
    private static final String[] PIF_KEYS = {
        "ID",
        "BRAND",
        "DEVICE",
        "FINGERPRINT",
        "MANUFACTURER",
        "MODEL",
        "PRODUCT",
        "SECURITY_PATCH",
        "DEVICE_INITIAL_SDK_INT",
        "TYPE",
        "TAGS",
        "RELEASE",
        "DEBUG",
        "SDK_INT"
    };

    private final ActivityResultLauncher<String> mImportKeyboxLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    handleKeyboxImport(uri);
                }
            });

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mHandler = new Handler();

        addPreferencesFromResource(R.xml.spoofing);

        mGphotosSpoof = findPreference(SYS_GPHOTOS_SPOOF);
        mGphotosSpoof.setOnPreferenceChangeListener(this);

        mGmsSpoof = findPreference(SYS_GMS_SPOOF);
        mPifJsonFilePreference = findPreference(KEY_PIF_JSON_FILE_PREFERENCE);
        mUpdateJsonButton = findPreference(KEY_UPDATE_JSON_BUTTON);

        mGmsSpoof.setOnPreferenceChangeListener(this);

        mPifJsonFilePreference.setOnPreferenceClickListener(preference -> {
            openFileSelector(10001);
            return true;
        });

        mUpdateJsonButton.setOnPreferenceClickListener(preference -> {
            updatePropertiesFromUrl("https://raw.githubusercontent.com/AxionAOSP/PlayIntegrityFix/refs/heads/lineage-22.1/pif.json");
            return true;
        });

        Preference showPropertiesPref = findPreference("show_pif_properties");
        if (showPropertiesPref != null) {
            showPropertiesPref.setOnPreferenceClickListener(preference -> {
                showPropertiesDialog();
                return true;
            });
        }

        mClearKeybox = findPreference(KEY_CLEAR_KEYBOX);
        mClearKeybox.setOnPreferenceClickListener(preference -> {
            clearKeybox();
            return true;
        });

        mImportKeybox = findPreference(KEY_IMPORT_KEYBOX);
        mImportKeybox.setOnPreferenceClickListener(preference -> {
            mImportKeyboxLauncher.launch("text/xml");
            return true;
        });
    }

    private void openFileSelector(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/json");
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                if (requestCode == 10001) {
                    loadPifJson(uri);
                }
            }
        }
    }

    private void showPropertiesDialog() {
        StringBuilder properties = new StringBuilder();
        try {
            JSONObject jsonObject = new JSONObject();
            String[] keys = {
                "persist.sys.pihooks_ID",
                "persist.sys.pihooks_BRAND", 
                "persist.sys.pihooks_DEVICE",
                "persist.sys.pihooks_FINGERPRINT",
                "persist.sys.pihooks_MANUFACTURER",
                "persist.sys.pihooks_MODEL",
                "persist.sys.pihooks_PRODUCT",
                "persist.sys.pihooks_SECURITY_PATCH",
                "persist.sys.pihooks_DEVICE_INITIAL_SDK_INT",
                "persist.sys.pihooks_TYPE",
                "persist.sys.pihooks_TAGS",
                "persist.sys.pihooks_RELEASE",
                "persist.sys.pihooks_DEBUG",
                "persist.sys.pihooks_SDK_INT"
            };
            for (String key : keys) {
                String value = SystemProperties.get(key, null);
                if (!TextUtils.isEmpty(value)) {
                    String buildKey = key.replace("persist.sys.pihooks_", "");
                    jsonObject.put(buildKey, value);
                }
            }
            String jsonString = jsonObject.toString(4);
            jsonString = jsonString.replace("\\/", "/");
            properties.append(jsonString);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON from properties", e);
            properties.append(getString(R.string.error_loading_properties));
        }
        new AlertDialog.Builder(getContext())
            .setTitle(R.string.show_pif_properties_title)
            .setMessage(properties.toString())
            .setPositiveButton(android.R.string.ok, null)
            .show();
    }

    private void updatePropertiesFromUrl(String urlString) {
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try (InputStream inputStream = urlConnection.getInputStream()) {
                    String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                    Log.d(TAG, "Downloaded JSON data: " + json);
                    JSONObject jsonObject = new JSONObject(json);
                    String spoofedModel = jsonObject.optString("MODEL", "Unknown model");
                    for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                        String key = it.next();
                        String value = jsonObject.getString(key);
                        Log.d(TAG, "Setting property: persist.sys.pihooks_" + key + " = " + value);
                        SystemProperties.set("persist.sys.pihooks_" + key, value);
                    }
                    for (String pifKey : PIF_KEYS) {
                        if (!jsonObject.has(pifKey)) {
                            Log.d(TAG, "Clearing unspecified property: " + pifKey);
                            SystemProperties.set("persist.sys.pihooks_" + pifKey, "");
                        }
                    }
                    mHandler.post(() -> {
                        String toastMessage = getString(R.string.toast_spoofing_success, spoofedModel);
                        Toast.makeText(getContext(), toastMessage, Toast.LENGTH_LONG).show();
                    });
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error downloading JSON or setting properties", e);
                mHandler.post(() -> {
                    Toast.makeText(getContext(), R.string.toast_spoofing_failure, Toast.LENGTH_LONG).show();
                });
            }
            mHandler.postDelayed(() -> {
                SystemRestartUtils.showSystemRestartDialog(getContext());
            }, 1250);
        }).start();
    }

    private void loadPifJson(Uri uri) {
        Log.d(TAG, "Loading PIF JSON from URI: " + uri.toString());
        try (InputStream inputStream = getActivity().getContentResolver().openInputStream(uri)) {
            if (inputStream != null) {
                String json = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                Log.d(TAG, "PIF JSON data: " + json);
                JSONObject jsonObject = new JSONObject(json);
                for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                    String key = it.next();
                    String value = jsonObject.getString(key);
                    Log.d(TAG, "Setting PIF property: persist.sys.pihooks_" + key + " = " + value);
                    SystemProperties.set("persist.sys.pihooks_" + key, value);
                }
                for (String pifKey : PIF_KEYS) {
                    if (!jsonObject.has(pifKey)) {
                        Log.d(TAG, "Clearing unspecified property: " + pifKey);
                        SystemProperties.set("persist.sys.pihooks_" + pifKey, "");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error reading PIF JSON or setting properties", e);
        }
        mHandler.postDelayed(() -> {
            SystemRestartUtils.showSystemRestartDialog(getContext());
        }, 1250);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mGmsSpoof
            || preference == mGphotosSpoof) {
            SystemRestartUtils.showSystemRestartDialog(getContext());
            return true;
        }
        return false;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.VIEW_UNKNOWN;
    }

    private void handleKeyboxImport(Uri uri) {
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri)) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(in);
            doc.getDocumentElement().normalize();

            Element root = doc.getDocumentElement();
            if (root == null || !"AndroidAttestation".equals(root.getNodeName())) {
                Log.e(TAG, "Invalid root element. Expected <AndroidAttestation>");
                showToast(R.string.import_failed);
                return;
            }

            NodeList keyboxes = doc.getElementsByTagName("Keybox");
            if (keyboxes.getLength() == 0) {
                Log.e(TAG, "No <Keybox> element found in XML.");
                showToast(R.string.import_failed);
                return;
            }

            JSONObject keyboxJson = new JSONObject();

            for (int i = 0; i < keyboxes.getLength(); i++) {
                Element keyboxElement = (Element) keyboxes.item(i);
                NodeList keys = keyboxElement.getElementsByTagName("Key");

                if (keys.getLength() == 0) {
                    Log.w(TAG, "No <Key> entries in <Keybox>. Skipping.");
                    continue;
                }

                for (int j = 0; j < keys.getLength(); j++) {
                    Element keyElement = (Element) keys.item(j);
                    String algorithm = keyElement.getAttribute("algorithm").toUpperCase();
                    if (TextUtils.isEmpty(algorithm)) {
                        Log.w(TAG, "Missing 'algorithm' attribute in <Key>. Skipping.");
                        continue;
                    }

                    if (algorithm.equals("ECDSA")) algorithm = "EC";

                    Element privKeyElem = (Element) keyElement.getElementsByTagName("PrivateKey").item(0);
                    if (privKeyElem == null) {
                        Log.w(TAG, "No <PrivateKey> found for algorithm " + algorithm + ". Skipping.");
                        continue;
                    }

                    String privKeyRaw = getRawText(privKeyElem);
                    String privKey = extractBase64FromPEM(privKeyRaw);
                    if (TextUtils.isEmpty(privKey)) {
                        Log.w(TAG, "Empty private key for " + algorithm + ". Skipping.");
                        continue;
                    }
                    keyboxJson.put(algorithm + ".PRIV", privKey);

                    NodeList certList = keyElement.getElementsByTagName("Certificate");
                    for (int k = 0; k < certList.getLength(); k++) {
                        Element certElem = (Element) certList.item(k);
                        String certRaw = getRawText(certElem);
                        String cert = extractBase64FromPEM(certRaw);
                        if (!TextUtils.isEmpty(cert)) {
                            keyboxJson.put(algorithm + ".CERT_" + (k + 1), cert);
                        } else {
                            Log.w(TAG, "Empty certificate #" + (k + 1) + " for " + algorithm);
                        }
                    }
                }
            }

            if (keyboxJson.length() == 0) {
                Log.e(TAG, "Parsed keybox is empty. Import failed.");
                showToast(R.string.import_failed);
                return;
            }

            Settings.System.putString(requireContext().getContentResolver(),
                    "custom_keybox_data", keyboxJson.toString());

            showToast(R.string.import_success);
            SystemRestartUtils.showSystemRestartDialog(getContext());

        } catch (Exception e) {
            Log.e(TAG, "Keybox import failed", e);
            showToast(R.string.import_failed);
        }
    }

    private String getRawText(Element element) {
        StringBuilder builder = new StringBuilder();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                builder.append(node.getNodeValue());
            }
        }
        return builder.toString().trim();
    }

    private String extractBase64FromPEM(String pem) {
        return pem.replaceAll("-----BEGIN [^-]+-----", "")
                  .replaceAll("-----END [^-]+-----", "")
                  .replaceAll("[\\r\\n\\s]+", "");
    }

    private void showToast(int resId) {
        getActivity().runOnUiThread(() -> 
            Toast.makeText(getContext(), resId, Toast.LENGTH_SHORT).show()
        );
    }

    private void clearKeybox() {
        try {
            Settings.System.putString(requireContext().getContentResolver(), "custom_keybox_data", null);
            showToast(R.string.clear_success);
            SystemRestartUtils.showSystemRestartDialog(getContext());
        } catch (Exception e) {
            Log.e(TAG, "Failed to clear keybox", e);
            showToast(R.string.clear_failed);
        }
    }

    /**
     * For search
     */
    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.spoofing) {

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);

                    return keys;
                }
            };
}
