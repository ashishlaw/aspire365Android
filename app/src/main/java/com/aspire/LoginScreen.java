package com.aspire;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aspire.adapters.PopupRecyclerViewAdapter;
import com.aspire.controller.SharedPrefEmail;
import com.aspire.customcontrols.TextViewRegular;
import com.aspire.propertyclasses.CheckLogin;
import com.aspire.propertyclasses.LoginData;
import com.aspire.propertyclasses.LoginProp;
import com.aspire.propertyclasses.UserDetails;
import com.aspire.retrofitclasses.ApiClient;
import com.aspire.retrofitclasses.ApiInterface;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.onesignal.OneSignal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.RECORD_AUDIO;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class LoginScreen extends AppCompatActivity implements PopupRecyclerViewAdapter.Listener {

    public static final int RequestPermissionCode = 1;

    private EditText emailAddressET, passwordET;
    private Button loginBT;
    private String JWTtoken;
    String deviceId = "";
    boolean isClicked = false;
    private CheckBox biometricsCB, rememberMeCB;
    ImageView showPasswordIV;
    private RelativeLayout mainRL;
    private TextViewRegular forgotPasswordTV;
    private PopupWindow popupWindow = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login_screen);


        if (checkPermission()) {

        } else {
            requestPermission();
        }

        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        deviceId = OneSignal.getPermissionSubscriptionState().getSubscriptionStatus().getUserId();

        mainRL = findViewById(R.id.mainRL);

        emailAddressET = findViewById(R.id.emailET);
        passwordET = findViewById(R.id.passwordET);
        loginBT = findViewById(R.id.loginBT);
        showPasswordIV = findViewById(R.id.showPasswordIV);
        biometricsCB = findViewById(R.id.biometricsCB);
        rememberMeCB = findViewById(R.id.rememberMeCB);
        forgotPasswordTV = findViewById(R.id.forgotPasswordTV);

        String isChecked = SharedPrefEmail.getStringEmail(LoginScreen.this, "isChecked");
        rememberMeCB.setChecked(isChecked.equalsIgnoreCase("checked"));

        String userEmail = SharedPrefEmail.getStringEmail(LoginScreen.this, "userEmail");

        if (!userEmail.isEmpty()) {
            emailAddressET.setText(userEmail);
        }

        forgotPasswordTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(LoginScreen.this, ForgotPassword.class);
                startActivity(intent);
            }
        });

        showPasswordIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isClicked) {
                    passwordET.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    passwordET.setSelection(passwordET.getText().length());
                    isClicked = true;
                } else {
                    passwordET.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    passwordET.setSelection(passwordET.getText().length());
                    isClicked = false;
                }
            }
        });

        loginBT.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String emailAddress = emailAddressET.getText().toString().trim();
                String password = passwordET.getText().toString().trim();

                if (emailAddress.isEmpty()) {
                    Snackbar snackbar = Snackbar
                            .make(mainRL, "Enter an Email Address", Snackbar.LENGTH_LONG);
                    snackbar.show();
                } else if (password.isEmpty()) {
                    Snackbar snackbar = Snackbar
                            .make(mainRL, "Enter the Password", Snackbar.LENGTH_LONG);
                    snackbar.show();
                } else {
                    Dialogs.baseShowProgressDialog(LoginScreen.this, "Logging In...");

                    checkLogin(emailAddress, password);
                }

            }
        });
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(LoginScreen.this, new String[]
                {
                        Manifest.permission.CAMERA,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, RequestPermissionCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == RequestPermissionCode) {
            if (grantResults.length > 0) {
                boolean CameraPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean RecordAudio = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean WriteExternalStoragePermission = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                boolean FineLocation = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                boolean CoarseLocation = grantResults[4] == PackageManager.PERMISSION_GRANTED;

                if (CameraPermission && RecordAudio && WriteExternalStoragePermission && FineLocation && CoarseLocation) {
                    Toast.makeText(LoginScreen.this, "Permission Granted", Toast.LENGTH_LONG).show();

                } else {
                    Toast.makeText(LoginScreen.this, "Permission Denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    public boolean checkPermission() {

        int FirstPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), CAMERA);
        int SecondPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), RECORD_AUDIO);
        int ThirdPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        int FourthPermission = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_FINE_LOCATION);
        int FifthPermission = ContextCompat.checkSelfPermission(getApplicationContext(), ACCESS_COARSE_LOCATION);

        return FirstPermissionResult == PackageManager.PERMISSION_GRANTED &&
                SecondPermissionResult == PackageManager.PERMISSION_GRANTED &&
                ThirdPermissionResult == PackageManager.PERMISSION_GRANTED &&
                FourthPermission == PackageManager.PERMISSION_GRANTED &&
                FifthPermission == PackageManager.PERMISSION_GRANTED;
    }

    public void checkLogin(String username, String password) {
        if (!Dialogs.isInternetAvailable(this)) {
            Dialogs.baseHideProgressDialog();
            Dialogs.showToast(this, "Internet Not Available!");
            return;
        }

        ApiInterface apiInterface = ApiClient.getClientWithToken(LoginScreen.this).create(ApiInterface.class);
        apiInterface.checkLoginAPI(username, password).enqueue(new Callback<CheckLogin>() {
            @Override
            public void onResponse(Call<CheckLogin> call, Response<CheckLogin> response) {
                Dialogs.baseHideProgressDialog();
                if (response.isSuccessful()) {
                    if (response.body().getStatus()) {
                        showLoginSelectionPopup(response.body().getData());
                    }
                } else {
                    try {
                        Gson gsonObj = new Gson();
                        LoginProp loginError = gsonObj.fromJson(response.errorBody().string(), LoginProp.class);
                        if (!loginError.getMessage().isEmpty()) {
                            Toast.makeText(getApplicationContext(), loginError.getMessage(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Unable to Login", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), "Unable to Login", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Call<CheckLogin> call, Throwable t) {
                Dialogs.baseHideProgressDialog();
                Log.e("erroroororoororororor", t.getMessage());
                Toast.makeText(getApplicationContext(), "Unable to Login", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void dimBehind(PopupWindow popupWindow) {
        View container;
        if (popupWindow.getBackground() == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                container = (View) popupWindow.getContentView().getParent();
            } else {
                container = popupWindow.getContentView();
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                container = (View) popupWindow.getContentView().getParent().getParent();
            } else {
                container = (View) popupWindow.getContentView().getParent();
            }
        }
        Context context = popupWindow.getContentView().getContext();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) container.getLayoutParams();
        p.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.3f;
        wm.updateViewLayout(container, p);
    }

    private void showLoginSelectionPopup(LoginData data) {

        View popupView = LayoutInflater.from(this).inflate(R.layout.recycler_popup_window, null);
        popupWindow = new PopupWindow(popupView, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);

        //dimBehind(popupWindow);

        RecyclerView recyclerView = popupView.findViewById(R.id.rv_recycler_view);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);
        PopupRecyclerViewAdapter adapter = new PopupRecyclerViewAdapter(this, this, data.getUsers());
        recyclerView.setAdapter(adapter);

        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0);

    }

    public void loginService(String username, String password, String user_id) {
        if (!Dialogs.isInternetAvailable(this)) {
            Dialogs.baseHideProgressDialog();
            Dialogs.showToast(this, "Internet Not Available!");
            return;
        }

        ApiInterface apiInterface = ApiClient.getClientWithToken(LoginScreen.this).create(ApiInterface.class);
        apiInterface.loginAPI(username, password, user_id).enqueue(new Callback<LoginProp>() {
            @Override
            public void onResponse(Call<LoginProp> call, Response<LoginProp> response) {
                Dialogs.baseHideProgressDialog();
                if (response.isSuccessful()) {
                    if (response.body().getStatus()) {
                        JWTtoken = response.body().getData().getJwt().getIdToken();

                        if (rememberMeCB.isChecked()) {
                            SharedPreferences.setString(LoginScreen.this, "loggedIn", "loggedIn");
                            SharedPrefEmail.setStringEmail(LoginScreen.this, "isChecked", "checked");
                            SharedPrefEmail.setStringEmail(LoginScreen.this, "userEmail", emailAddressET.getText().toString().trim());
                        } else {
                            SharedPreferences.setString(LoginScreen.this, "loggedIn", "");
                            SharedPrefEmail.setStringEmail(LoginScreen.this, "isChecked", "");
                            SharedPrefEmail.setStringEmail(LoginScreen.this, "userEmail", "");
                        }

                        SharedPreferences.setString(LoginScreen.this, "token", JWTtoken);
                        SharedPreferences.setString(LoginScreen.this, "deviceID", response.body().getDeviceId());
                        SharedPreferences.setString(LoginScreen.this, "biometricEnabled", "");
                        Intent intent = new Intent(LoginScreen.this, HomeScreen.class);
                        startActivity(intent);
                        finish();
                        if (biometricsCB.isChecked()) {
                            SharedPreferences.setString(LoginScreen.this, "biometricEnabled", "Enabled");
                            Toast.makeText(LoginScreen.this, "Biometrics Authenticated", Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    try {
                        //  Dialogs.baseHideProgressDialog();
                        Gson gsonObj = new Gson();
                        LoginProp loginError = gsonObj.fromJson(response.errorBody().string(), LoginProp.class);
                        Toast.makeText(getApplicationContext(), loginError.getMessage(), Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (popupWindow != null) {
                    popupWindow.dismiss();
                }
            }

            @Override
            public void onFailure(Call<LoginProp> call, Throwable t) {
                Dialogs.baseHideProgressDialog();
                if (popupWindow != null) {
                    popupWindow.dismiss();
                }
                Log.e("erroroororoororororor", t.getMessage());
                Toast.makeText(getApplicationContext(), "Unable to Login", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onItemClicked(UserDetails userDetails) {
        Dialogs.baseShowProgressDialog(LoginScreen.this, "Logging In...");
        loginService(emailAddressET.getText().toString(), passwordET.getText().toString(), userDetails.getId().toString());
    }
}