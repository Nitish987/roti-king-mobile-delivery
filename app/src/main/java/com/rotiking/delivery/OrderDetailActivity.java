package com.rotiking.delivery;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.rotiking.delivery.adapters.CheckoutCartItemRecyclerAdapter;
import com.rotiking.delivery.common.auth.Auth;
import com.rotiking.delivery.common.db.Database;
import com.rotiking.delivery.common.security.AES128;
import com.rotiking.delivery.models.CartItem;
import com.rotiking.delivery.models.CheckoutCartItem;
import com.rotiking.delivery.models.Order;
import com.rotiking.delivery.models.Topping;
import com.rotiking.delivery.utils.DateParser;
import com.rotiking.delivery.utils.Promise;
import com.rotiking.delivery.utils.Validator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderDetailActivity extends AppCompatActivity implements LocationListener {
    private RecyclerView orderItemRV;
    private ImageButton closeBtn;
    private TextView itemsTxt, orderNumberTxt, totalCartPriceTxt, deliveryCartPriceTxt, totalPayableTxt, nameTxt, phoneTxt, addressTxt, agentNameTxt, agentPhoneTxt, timeTxt, paymentMethodTxt;
    private AppCompatButton confirmDeliveryBtn, trackOnMapBtn;
    private LinearProgressIndicator orderStateIndicator;
    private TextView orderedStateTxt, orderedState, cookingState, dispatchedState, onWayState, deliveredState;
    private SwitchMaterial dispatchedSwitch;
    private LinearLayout deliveryCodeDesk;
    private EditText deliveryCode_eTxt;

    private String orderId, to;
    private GeoPoint geoPoint;
    private LocationManager locationManager;
    private int whatOrderState = -1;

    private static final int LOCATION_PERMISSION_CODE = 102, COARSE_LOCATION_PERMISSION_CODE = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        orderId = getIntent().getStringExtra("ORDER");

        closeBtn = findViewById(R.id.close);
        itemsTxt = findViewById(R.id.items);
        orderNumberTxt = findViewById(R.id.order_number);
        totalCartPriceTxt = findViewById(R.id.total_price);
        deliveryCartPriceTxt = findViewById(R.id.delivery_price);
        totalPayableTxt = findViewById(R.id.payable_price);
        nameTxt = findViewById(R.id.name);
        phoneTxt = findViewById(R.id.phone);
        addressTxt = findViewById(R.id.address);
        agentNameTxt = findViewById(R.id.agent_name);
        agentPhoneTxt = findViewById(R.id.agent_phone);
        timeTxt = findViewById(R.id.time);
        paymentMethodTxt = findViewById(R.id.payment_method);
        orderStateIndicator = findViewById(R.id.order_state_indicator);
        orderedStateTxt = findViewById(R.id.order_state_text);
        orderedState = findViewById(R.id.ordered_state);
        cookingState = findViewById(R.id.cooking_state);
        dispatchedState = findViewById(R.id.dispatched_state);
        onWayState = findViewById(R.id.on_way_state);
        deliveredState = findViewById(R.id.delivered_state);
        confirmDeliveryBtn = findViewById(R.id.confirm_delivery_btn);
        dispatchedSwitch = findViewById(R.id.dispatched_switch);
        trackOnMapBtn = findViewById(R.id.track);
        deliveryCodeDesk = findViewById(R.id.delivery_code_desk);
        deliveryCode_eTxt = findViewById(R.id.delivery_verification_code_e_txt);

        orderItemRV = findViewById(R.id.ordered_item_rv);
        orderItemRV.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        orderItemRV.setHasFixedSize(true);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(OrderDetailActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            checkLocationPermission();
        } else {
            getCurrentLocation();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseFirestore.getInstance().collection("orders").document(orderId).addSnapshotListener((value, error) -> {
            if (value != null && value.exists()) {
                Order order = value.toObject(Order.class);

                assert order != null;
                to = order.getUid();
                geoPoint = order.getLocation();
                whatOrderState = order.getOrderState();

                CheckoutCartItemRecyclerAdapter adapter = new CheckoutCartItemRecyclerAdapter(createOrderItemList(order.getItems()));
                orderItemRV.setAdapter(adapter);

                String tcp = "\u20B9 " + order.getTotalPrice();
                totalCartPriceTxt.setText(tcp);

                String dp_ = "\u20B9 " + order.getDeliveryPrice();
                deliveryCartPriceTxt.setText(dp_);

                String tp_ = "\u20B9 " + order.getPayablePrice();
                totalPayableTxt.setText(tp_);

                String c_ = order.getItems().size() + " Food Items";
                itemsTxt.setText(c_);

                String o_ = "#~order~" + order.getOrderNumber();
                orderNumberTxt.setText(o_);

                String t_ = DateParser.parse(new Date(order.getTime()));
                timeTxt.setText(t_);

                String pm_ = order.getPaymentMethod();
                if (!order.getPaymentOrderID().equals("None")) {
                    pm_ = pm_ +  " (paid)";
                }
                paymentMethodTxt.setText(pm_);
                nameTxt.setText(order.getName());
                addressTxt.setText(order.getAddress());
                phoneTxt.setText(order.getPhone());
                orderedStateTxt.setText(getOrderStateTxt(order.getOrderState()));

                if (order.getAgentName() != null && order.getAgentPhone() != null) {
                    agentNameTxt.setText(order.getAgentName());
                    agentPhoneTxt.setText(order.getAgentPhone());
                }

                switch (order.getOrderState()) {
                    case 0:
                        orderStateIndicator.setProgressCompat(0, true);
                        orderedState.getBackground().setTint(getColor(R.color.red));

                        deliveryCodeDesk.setVisibility(View.GONE);

                        trackOnMapBtn.setVisibility(View.GONE);
                        trackOnMapBtn.setEnabled(false);
                        break;

                    case 1:
                        orderStateIndicator.setProgressCompat(25, true);
                        orderedState.getBackground().setTint(getColor(R.color.red));
                        cookingState.getBackground().setTint(getColor(R.color.red));

                        deliveryCodeDesk.setVisibility(View.GONE);

                        trackOnMapBtn.setVisibility(View.GONE);
                        trackOnMapBtn.setEnabled(false);
                        break;

                    case 2:
                        orderStateIndicator.setProgressCompat(50, true);
                        orderedState.getBackground().setTint(getColor(R.color.red));
                        cookingState.getBackground().setTint(getColor(R.color.red));
                        dispatchedState.getBackground().setTint(getColor(R.color.red));

                        dispatchedSwitch.setOnCheckedChangeListener(null);
                        dispatchedSwitch.setChecked(true);
                        dispatchedSwitch.setEnabled(false);
                        deliveryCodeDesk.setVisibility(View.VISIBLE);

                        trackOnMapBtn.setVisibility(View.VISIBLE);
                        trackOnMapBtn.setEnabled(true);
                        break;

                    case 3:
                        orderStateIndicator.setProgressCompat(75, true);
                        orderedState.getBackground().setTint(getColor(R.color.red));
                        cookingState.getBackground().setTint(getColor(R.color.red));
                        dispatchedState.getBackground().setTint(getColor(R.color.red));
                        onWayState.getBackground().setTint(getColor(R.color.red));

                        dispatchedSwitch.setOnCheckedChangeListener(null);
                        dispatchedSwitch.setChecked(true);
                        dispatchedSwitch.setEnabled(false);
                        deliveryCodeDesk.setVisibility(View.VISIBLE);

                        trackOnMapBtn.setVisibility(View.VISIBLE);
                        trackOnMapBtn.setEnabled(true);
                        break;

                    case 4:
                        orderStateIndicator.setProgressCompat(100, true);
                        orderedState.getBackground().setTint(getColor(R.color.red));
                        cookingState.getBackground().setTint(getColor(R.color.red));
                        dispatchedState.getBackground().setTint(getColor(R.color.red));
                        onWayState.getBackground().setTint(getColor(R.color.red));
                        deliveredState.getBackground().setTint(getColor(R.color.red));

                        dispatchedSwitch.setOnCheckedChangeListener(null);
                        dispatchedSwitch.setEnabled(false);
                        deliveryCodeDesk.setVisibility(View.GONE);

                        trackOnMapBtn.setVisibility(View.GONE);
                        trackOnMapBtn.setEnabled(false);

                        if (order.getSecureNumber() != null) {
                            deliveredState.getBackground().setTint(getColor(R.color.light_red));

                            String notDelivered = "Not Delivered Yet";
                            orderedStateTxt.setText(notDelivered);

                            deliveryCodeDesk.setVisibility(View.VISIBLE);

                            trackOnMapBtn.setVisibility(View.VISIBLE);
                            trackOnMapBtn.setEnabled(true);
                        }
                        break;
                }

                if (!order.isOrderSuccess()) {
                    orderStateIndicator.setProgressCompat(0, true);
                    confirmDeliveryBtn.setEnabled(false);

                    dispatchedSwitch.setEnabled(false);

                    trackOnMapBtn.setVisibility(View.GONE);
                    trackOnMapBtn.setEnabled(false);

                    deliveryCodeDesk.setVisibility(View.GONE);

                    String cancelMsg = "Order was Canceled.";
                    orderedStateTxt.setText(cancelMsg);
                }
            }
        });

        dispatchedSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            int orderState = 1;
            if (b) orderState = 2;
            Map<String, Object> map = new HashMap<>();
            map.put("orderState", orderState);
            FirebaseFirestore.getInstance().collection("orders").document(orderId).update(map).addOnSuccessListener(unused -> {
                Auth.Notify.pushNotification(this, to, "Order Dispatched", "Your Order is dispatched.", new Promise<String>() {
                    @Override
                    public void resolving(int progress, String msg) {}

                    @Override
                    public void resolved(String o) {}

                    @Override
                    public void reject(String err) {}
                });
                Database.emailDeliveryOtp(this, orderId, new Promise<String>() {
                    @Override
                    public void resolving(int progress, String msg) {}

                    @Override
                    public void resolved(String o) {}

                    @Override
                    public void reject(String err) {}
                });
            }).addOnFailureListener(e -> Toast.makeText(this, "Something went wrong.", Toast.LENGTH_SHORT).show());
        });

        trackOnMapBtn.setOnClickListener(view -> {
            Map<String, Object> map = new HashMap<>();
            map.put("orderState", 3);
            FirebaseFirestore.getInstance().collection("orders").document(orderId).update(map).addOnSuccessListener(unused -> {
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse("google.navigation:q=" + geoPoint.getLatitude() + "," + geoPoint.getLongitude()));
                startActivity(intent);
            }).addOnFailureListener(e -> Toast.makeText(this, "Unable to track.", Toast.LENGTH_SHORT).show());
        });

        confirmDeliveryBtn.setOnClickListener(view -> {
            String code = deliveryCode_eTxt.getText().toString();

            if (Validator.isEmpty(code)) {
                deliveryCode_eTxt.setError("Code Required!");
                return;
            }

            DocumentReference reference = FirebaseFirestore.getInstance().collection("orders").document(orderId);
            reference.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    if (AES128.decrypt(Auth.ENCRYPTION_KEY, documentSnapshot.get("secureNumber", String.class)).equals(code)) {
                        Map<String, Object> map = new HashMap<>();
                        map.put("secureNumber", null);
                        map.put("orderState", 4);
                        map.put("agentLocation", new GeoPoint(0,0));
                        reference.update(map).addOnSuccessListener(unused -> {
                            Auth.Notify.pushNotification(this, to, "Order Delivered", "Your Order is delivered.", new Promise<String>() {
                                @Override
                                public void resolving(int progress, String msg) {
                                }

                                @Override
                                public void resolved(String o) {
                                    Toast.makeText(OrderDetailActivity.this, "delivered.", Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void reject(String err) {
                                }
                            });
                        });
                    } else {
                        Toast.makeText(this, "Invalid code.", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        closeBtn.setOnClickListener(view -> finish());
    }

    private List<CheckoutCartItem> createOrderItemList(List<CartItem> items) {
        List<CheckoutCartItem> checkoutCartItems = new ArrayList<>();
        for (CartItem cartItem : items) {
            StringBuilder orderName = new StringBuilder();
            orderName.append(cartItem.getFood_data().getName()).append("(").append(cartItem.getQuantity()).append(")");
            if (!cartItem.getTopping_ids().equals("None")) {
                orderName.append(" + Toppings(");
                for (Topping topping : cartItem.getToppings()) {
                    orderName.append(topping.getName()).append(", ");
                }
                orderName.replace(orderName.length() - 2, orderName.length(), ")");
            }
            CheckoutCartItem item = new CheckoutCartItem(orderName.toString(), cartItem.getTotal_price());
            checkoutCartItems.add(item);
        }
        return checkoutCartItems;
    }

    private String getOrderStateTxt(int state) {
        switch (state) {
            case 0:
                return "Ordered...";
            case 1:
                return "Cooking...";
            case 2:
                return "Dispatched...";
            case 3:
                return "On way...";
            case 4:
                return "Delivered...";
            default:
                return "";
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(OrderDetailActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(OrderDetailActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_CODE);
        else if (ActivityCompat.checkSelfPermission(OrderDetailActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(OrderDetailActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, COARSE_LOCATION_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE || requestCode == COARSE_LOCATION_PERMISSION_CODE) {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (whatOrderState != 4) {
            if (ActivityCompat.checkSelfPermission(OrderDetailActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(OrderDetailActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 10, this);
                else
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 10, this);
            } else {
                Toast.makeText(OrderDetailActivity.this, "Location is required for delivery purpose.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Map<String, Object> map = new HashMap<>();
        GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
        map.put("agentLocation", geoPoint);
        FirebaseFirestore.getInstance().collection("orders").document(orderId).update(map);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(this);
    }
}