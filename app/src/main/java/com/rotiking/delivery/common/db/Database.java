package com.rotiking.delivery.common.db;

import android.content.Context;

import com.android.volley.Request;
import com.google.firebase.firestore.FirebaseFirestore;
import com.rotiking.delivery.common.auth.Auth;
import com.rotiking.delivery.common.settings.ApiKey;
import com.rotiking.delivery.utils.Promise;
import com.rotiking.delivery.utils.Server;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Database {
    public static FirebaseFirestore getInstance() {
        return FirebaseFirestore.getInstance();
    }

    public static void emailDeliveryOtp(Context context, String orderId, Promise<String> promise) {
        Map<String, String> headers = new HashMap<>();
        headers.put("RAK", ApiKey.REQUEST_API_KEY);
        headers.put("AT", Auth.AUTH_TOKEN);
        headers.put("UID", Auth.getAuthUserUid());

        JSONObject body = new JSONObject();
        try {
            body.put("order_id", orderId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Server.request(context, Request.Method.POST, ApiKey.REQUEST_API_URL + "delivery/email-d-v-otp/", headers, body, new Promise<JSONObject>() {
                    @Override
                    public void resolving(int progress, String msg) {
                        promise.resolving(progress, msg);
                    }

                    @Override
                    public void resolved(JSONObject data) {
                        try {
                            promise.resolved(data.getString("token"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            promise.reject("Something went wrong.");
                        }
                    }

                    @Override
                    public void reject(String err) {
                        promise.reject(err);
                    }
                }
        );
    }
}
